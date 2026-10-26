# TrustPay Guardian — Complete Demonstration & Walkthrough Guide

**Platform:** TrustPay Guardian — Fraud-Aware Digital Banking Platform  
**Target Environment:** Windows, Java 25 LTS, Spring Boot 4.1.1, MySQL 8.4.11 Community Server, Node.js 24.15.0  

---

## 1. Quick Start Commands (PowerShell)

### Terminal 1: Backend (Spring Boot 4.1.1)
```powershell
cd "E:\TrustPay Guardian\backend"
.\mvnw.cmd spring-boot:run
```
*Backend will start on `http://localhost:8080/api/v1` with Flyway database migration validation and automated seed data initialization.*

### Terminal 2: Frontend (React 19 + TypeScript + Vite 6)
```powershell
cd "E:\TrustPay Guardian\frontend"
npm run dev
```
*Frontend will launch on `http://localhost:5173`. Open your web browser to this URL.*

### Run All Automated Tests
```powershell
cd "E:\TrustPay Guardian\backend"
.\mvnw.cmd test
```
*Runs all 39 automated integration and unit tests with 0 errors and 0 failures.*

---

## 2. Seeded Personas & Credentials

The application auto-seeds the following personas during startup (with BCrypt-hashed credentials):

| Persona | Email | Password | Role | Default Account Number | Opening Balance |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Customer 1 (John Doe)** | `john.doe@example.com` | `Customer@123` | `CUSTOMER` | `TPG100000000001` | ₹50,000.00 |
| **Customer 2 (Jane Smith)** | `jane.smith@example.com` | `Customer@123` | `CUSTOMER` | `TPG100000000002` | ₹30,000.00 |
| **Fraud Analyst** | `analyst@trustpay.com` | `Analyst@123` | `FRAUD_ANALYST` | N/A (Staff) | N/A |
| **Administrator** | `admin@trustpay.com` | `Admin@123` | `ADMIN` | N/A (Staff) | N/A |

> **Security & Simulation Warning:** These demo credentials and pre-seeded test accounts exist strictly for local simulated demonstration. They must never be deployed or exposed in production environments.

---

## 3. Canonical Fraud Detection Rules (V6 Migration Standard)

The engine evaluates 7 deterministic rule strategies (Strategy Pattern). **This is a 100% deterministic, rule-based expert scoring engine. No machine learning models, statistical neural networks, or external AI services are present or claimed.**

| Code | Canonical Rule Name | Seeded Weight | Rule Condition Checked |
| :---: | :--- | :---: | :--- |
| **R1** | `HIGH_AMOUNT` | **30 pts** | Transfer amount meets or exceeds threshold (`threshold_amount`: ₹50,000.00). |
| **R2** | `VELOCITY` | **25 pts** | Rapid repeated transfers from same source account (`window_minutes`: 10, `max_transfers`: 4). |
| **R3** | `UNUSUAL_HOUR` | **15 pts** | Transfers initiated late-night/early morning (`start_hour`: 0, `end_hour`: 4, `Asia/Kolkata`). |
| **R4** | `NEW_BENEFICIARY_LARGE` | **30 pts** | High-value transfer to recently added beneficiary (`window_hours`: 24, `threshold_amount`: ₹20,000.00). |
| **R5** | `AMOUNT_SPIKE` | **20 pts** | Transfer amount exceeds $5\times$ 30-day historical average (`multiplier`: 5.0, `min_history_count`: 3). |
| **R6** | `DAILY_LIMIT_PROXIMITY` | **10 pts** | Projected cumulative daily total reaches $\ge 90\%$ of daily limit (`proximity_ratio`: 0.90). |
| **R7** | `RECENT_FAILED_LOGINS` | **15 pts** | Sender account had $\ge 3$ failed logins within last 30 minutes (`window_minutes`: 30, `min_failed_attempts`: 3). |

**Risk Thresholds:**
- **Score $< 40$ (LOW RISK):** Auto-clears immediately $\rightarrow$ Status: `APPROVED_SIMULATED`.
- **Score $40$ to $69$ (MEDIUM RISK):** Triggers `PENDING_REVIEW` with balance hold $\rightarrow$ Dispatched to Analyst Queue.
- **Score $\ge 70$ (HIGH RISK):** Immediate block $\rightarrow$ Status: `BLOCKED` (clamped maximum 100).

---

## 4. End-to-End Demonstration Scenarios

### Scenario A: Low-Risk Instant Transfer (Immediate Completion)
1. Quick-switch or login as **Customer 1 (John Doe)**.
2. In the **Transfer Funds** card:
   - Select quick beneficiary **Jane Smith (`TPG100000000002`)** or type `TPG100000000002`.
   - Enter amount: `₹2,500.00`.
   - Add reference note: `Dinner split`.
3. Notice the live risk estimate shows: **Score 0 (Low Risk)** because amount is below ₹50,000, recipient is an established beneficiary, velocity is normal, and it is daytime.
4. Click **Transfer Now**.
5. **Observed Result:**
   - Transaction status immediately shows `COMPLETED` with risk score `< 40`.
   - John's Available and Ledger balances decrease by `₹2,500.00`.
   - Switch to **Customer 2 (Jane)**: Jane's balance has increased by `₹2,500.00`.

---

### Scenario B: High-Risk Transfer & Fraud Alert Queue (Review Workflow)
1. As **Customer 1 (John Doe)**:
   - Enter an unknown recipient account number (or newly added beneficiary within 24h).
   - Enter an amount exceeding ₹50,000 (e.g., `₹55,000.00`).
   - Notice the live risk warning: **High amount triggers rule R1 (+30 pts)**. If daily limit proximity (R6: +10 pts) or new beneficiary (R4: +30 pts) also fires, the composite score reaches $\ge 40$.
2. Click **Transfer Now**.
3. **Observed Result:**
   - Transaction status shows `PENDING_REVIEW`.
   - Dynamic balance hold is applied: John's **Ledger Balance** remains intact, but **Available Balance** is decremented by the transfer amount, and **Held in Review** shows the reserved funds.
   - Concurrency protection ensures John cannot double-spend those held funds on another transfer!

---

### Scenario C: Fraud Analyst Investigation & Decision
1. Quick-switch to **Fraud Analyst (`analyst@trustpay.com`)**.
2. Navigate to **Fraud Alert Queue** (`/analyst`).
3. You will see the pending alert in the queue with its composite risk score and triggered rules badge.
4. Click **Inspect Alert**:
   - The modal displays the forensic breakdown: rule contributions (e.g. `R1 High Amount: +30 pts`, `R6 Daily Limit Proximity: +10 pts`).
   - Transaction metadata, source account, and recipient account are detailed.
5. Choose one of two actions:
   - **Option 1: Release Transaction**  
     Enter notes: `Verified customer via out-of-band call`. Click **Release Transaction**. Funds are permanently debited from the sender and credited to the recipient; status moves to `COMPLETED`.
   - **Option 2: Confirm Fraud (Block)**  
     Enter notes: `Confirmed phishing attack`. Click **Confirm Fraud (Block)**. Transaction status transitions to `BLOCKED`; sender's reserved balance is released back to available balance; customer receives an alert notification.

---

### Scenario D: Administrator Governance & Rule Tuning
1. Quick-switch to **Administrator (`admin@trustpay.com`)**.
2. Navigate to **Admin Console** (`/admin`):
   - **Tab 1 (System Metrics):** Real-time count of total users, accounts, transaction volume, pending reviews, and blocked fraud cases.
   - **Tab 2 (User Management):** View all platform users. Test role modification or account suspension. Notice that administrators cannot self-demote or suspend their own accounts (system-protected).
   - **Tab 3 (Fraud Rules Tuning):** View all 7 active fraud prevention rules (R1 to R7). Click **Edit** on rule `R1`, change points from `30` to `40`, and click **Save**. Changes take effect instantly in the live scoring engine without restarting the server!
   - **Tab 4 (System Settings):** Manage global thresholds (`DAILY_TRANSFER_LIMIT`, `HIGH_AMOUNT_THRESHOLD`, `REVIEW_THRESHOLD`).
   - **Tab 5 (Immutable Audit Logs):** Chronological, append-only log of every administrative and security event (logins, role changes, rule tuning, fraud decisions) with actor, IP address, and timestamp.

---

### Scenario E: Security & Brute-Force Lockout
1. Log out.
2. Attempt to sign in as `john.doe@example.com` with an incorrect password 5 consecutive times.
3. On the 5th failed attempt:
   - The system locks the account for 15 minutes.
   - Response explicitly returns: `Account is temporarily locked due to multiple failed login attempts. Please try again later or contact support.`
   - Switch to Administrator console: John's status displays as `LOCKED`, and an audit event `USER_LOCKED` is permanently recorded in the immutable audit trail!

---

## 5. Architectural Summary

- **Layered Architecture:** Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ MySQL 8.4 Relational Model.
- **Engine Classification:** 100% Deterministic Rule-Based Expert System (Strategy Pattern). No AI/ML components are used.
- **Concurrency Safety:** Deterministic ascending-ID row locking (`min(src, dst)` then `max(src, dst)`) completely prevents deadlocks under concurrent cross-transfers.
- **Idempotency Protection:** UUID idempotency keys cached in database prevent accidental double-billing or duplicate transfers.
- **Dynamic Balance Formula:**
  $$\text{Available Balance} = \text{Ledger Balance} - \sum (\text{Pending Outgoing Transfers})$$
