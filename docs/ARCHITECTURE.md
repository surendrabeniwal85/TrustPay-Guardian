# TrustPay Guardian — Technical Architecture & System Design

**Document Version:** 1.0.0  
**Target Release:** `v1.0.0`  
**Classification:** Core System Architecture Specification  
**Last Updated:** 2026-10-26  

---

## 1. System Context & Container Model (C4 Level 2)

```mermaid
C4Container
    title Container Diagram for TrustPay Guardian

    Person(customer, "Customer", "Initiates money transfers, inspects balances, and reviews statements")
    Person(analyst, "Fraud Analyst", "Investigates flagged transactions and executes case decisions")
    Person(admin, "Administrator", "Governs platform users, freezes accounts, and tunes fraud rules")

    Container(frontend, "Single Page App", "React 19, TypeScript, Vite", "Provides customer, analyst, and admin portals with in-memory token security")
    Container(proxy, "Web Server & Proxy", "Nginx 1.27 Alpine", "Terminates HTTPS, enforces CSP headers, serves static assets, and proxies /api to backend")
    Container(backend, "API Application", "Spring Boot 4.1.1, Java 25", "Executes business logic, enforces RBAC, runs fraud heuristics, and coordinates money movement")
    ContainerDb(db, "Relational Database", "MySQL 9.7 LTS", "Stores identity, accounts, immutable ledger, fraud rules, alerts, and audit logs")
    Container(mail, "Mail Service", "MailHog (Dev) / SMTP (Prod)", "Delivers security notices and transfer hold alerts")

    Rel(customer, frontend, "Interacts via browser over HTTPS")
    Rel(analyst, frontend, "Conducts investigations over HTTPS")
    Rel(admin, frontend, "Governs platform over HTTPS")

    Rel(frontend, proxy, "Loads assets and sends API requests")
    Rel(proxy, backend, "Reverse proxies /api/v1/* traffic with cookies", "HTTP/1.1")
    Rel(backend, db, "Reads/writes with pessimistic row locks via HikariCP", "JDBC")
    Rel(backend, mail, "Dispatches alert notifications", "SMTP")
```

---

## 2. Transaction Lifecycle & Finite State Machine (FSM)

A transaction strictly transitions through deterministic states governed by the Fraud Engine, ledger balances, and analyst intervention:

```mermaid
stateDiagram-v2
    [*] --> INITIATED: Customer submits transfer (with UUID Idempotency-Key)
    
    INITIATED --> REJECTED_VALIDATION: Insufficient balance / Frozen account / Self-transfer
    REJECTED_VALIDATION --> [*]

    INITIATED --> SCORING: Idempotency checked & accounts locked (ascending ID order)
    
    SCORING --> APPROVED_SIMULATED: Risk Score 0–39 (Low Risk)
    SCORING --> PENDING_REVIEW: Risk Score 40–69 (Medium Risk)
    SCORING --> BLOCKED: Risk Score 70–100 (High Risk)

    APPROVED_SIMULATED --> LEDGER_POSTED: Atomic debit sender & credit recipient
    LEDGER_POSTED --> [*]

    PENDING_REVIEW --> HELD_RESERVATION: Funds dynamically held (Available Balance decremented)
    HELD_RESERVATION --> ANALYST_REVIEW: Alert created in triage queue

    ANALYST_REVIEW --> RELEASED: Analyst selects RELEASE (notes mandatory)
    RELEASED --> LEDGER_POSTED: Status verified, hold converted to posted ledger debit
    
    ANALYST_REVIEW --> CONFIRM_BLOCKED: Analyst selects CONFIRM BLOCK (notes mandatory)
    CONFIRM_BLOCKED --> BLOCKED: Hold released, funds restored to available balance
    
    BLOCKED --> [*]
```

---

## 3. Fraud Engine Strategy Pipeline & Scoring Architecture

The Fraud Engine implements the GoF Strategy Pattern. Each rule operates as an independent, unit-testable Spring component:

```mermaid
flowchart TD
    subgraph TransactionSubmission ["Transaction Submission"]
        TX["Transfer Context (Sender, Recipient, Amount, Velocity, Account History)"]
    end

    subgraph HeuristicPipeline ["7-Rule Strategy Engine"]
        R1["R1: High Amount (Threshold: ₹50,000 | +35 pts)"]
        R2["R2: Rapid Velocity (>3 transfers in 5 mins | +25 pts)"]
        R3["R3: New Beneficiary (First-time recipient | +20 pts)"]
        R4["R4: Dormant Account (No activity >60 days | +20 pts)"]
        R5["R5: Balance Depletion (>80% balance drained | +25 pts)"]
        R6["R6: Round Figure (Multiples of ₹10,000 above ₹25,000 | +15 pts)"]
        R7["R7: Recent Failed Logins (>=3 failures in 15 mins | +20 pts)"]
    end

    TX --> R1 & R2 & R3 & R4 & R5 & R6 & R7

    subgraph ScoreAggregator ["Clamped Score Aggregator"]
        SUM["Sum Triggered Points: Score = Min(100, Sum(Points))"]
    end

    R1 & R2 & R3 & R4 & R5 & R6 & R7 --> SUM

    subgraph DecisionMatrix ["Threshold Decision Matrix"]
        SUM -->|0 <= Score <= 39| D1["APPROVED_SIMULATED\nInstant Transfer"]
        SUM -->|40 <= Score <= 69| D2["PENDING_REVIEW\nFund Reservation & Alert Queue"]
        SUM -->|70 <= Score <= 100| D3["BLOCKED\nInstant Block & Hold Release"]
    end
```

---

## 4. Concurrency & Deadlock-Free Row Locking

To prevent race conditions and eliminate database deadlocks when two parties transfer money to each other simultaneously, `TransferService` sorts account IDs in ascending numerical order before acquiring exclusive pessimistic write locks:

```mermaid
sequenceDiagram
    autonumber
    actor Alice as Customer Alice (Account #10)
    actor Bob as Customer Bob (Account #25)
    participant Service as TransferService
    participant DB as MySQL (InnoDB Engine)

    Note over Alice,Bob: Simultaneous Bidirectional Transfers: Alice -> Bob and Bob -> Alice
    
    par Thread 1 (Alice transfers to Bob)
        Service->>DB: Lock Account #10 (MIN(10, 25))
        DB-->>Service: Lock Acquired
        Service->>DB: Lock Account #25 (MAX(10, 25))
        DB-->>Service: Lock Acquired
    and Thread 2 (Bob transfers to Alice)
        Service->>DB: Lock Account #10 (MIN(10, 25))
        Note over DB: Thread 2 waits for Thread 1 to release #10 (NO CIRCULAR WAIT!)
    end

    Note over Service,DB: Thread 1 verifies balances, debits Alice, credits Bob, commits
    DB-->>Service: Thread 1 Transaction Committed
    
    Note over DB: Lock on #10 released; Thread 2 acquires #10 then #25 sequentially
    DB-->>Service: Thread 2 Lock Acquired
    Note over Service,DB: Thread 2 verifies balances, debits Bob, credits Alice, commits
    DB-->>Service: Thread 2 Transaction Committed
```

---

## 5. Client Token Lifecycle & Silent Refresh Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Browser Client (React)
    participant Axios as axiosClient.ts (In-Memory)
    participant API as Spring Boot Security

    User->>Axios: User logs in with email & password
    Axios->>API: POST /api/v1/auth/login
    API-->>Axios: 200 OK + Body: { accessToken: "jwt..." } + Set-Cookie: refreshToken=...; HttpOnly; SameSite=Strict
    Note over Axios: Access token stored strictly in volatile memory

    User->>Axios: Triggers authenticated request (GET /accounts)
    Axios->>API: GET /accounts (Authorization: Bearer <accessToken>)
    API-->>Axios: 401 Unauthorized (Access Token Expired)

    Note over Axios: Interceptor intercepts 401, pauses queue, initiates silent refresh
    Axios->>API: POST /api/v1/auth/refresh (Cookie: refreshToken automatically attached)
    API-->>Axios: 200 OK + Body: { accessToken: "new_jwt..." } + Rotated Set-Cookie
    Note over Axios: In-memory token updated; retries queued original request

    Axios->>API: GET /accounts (Authorization: Bearer <new_jwt>)
    API-->>Axios: 200 OK + Accounts Data
    Axios-->>User: Seamlessly displays data without user re-login
```
