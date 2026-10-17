# TrustPay Guardian — Phase 0 Architectural Inspection and Master Plan

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Document:** Gate 0 Repository Inspection & Architectural Master Plan  
**Role:** Senior Software Architect & Mentor  
**Date:** 2026-10-17  
**Status:** PROPOSED — Pending User Approval  

---

## 1. Current Root and Exact Folder Structure

- **Repository Root:** `e:\TrustPay Guardian`
- **Current Files:**
  - `Java FS Syllabus.pdf` (227 KB) — Curriculum constraints and learning outcomes.
  - `TrustPay Guardian Fraud-Aware Digital Banking Platform — PRD.pdf` (754 KB) — Reference functional specification.
  - `TrustPay Guardian — Antigravity Step-by-Step Prompt Guide.pdf` (506 KB) — Reference build phase guide.
  - `TrustPay_Guardian_PRD.md` (67 KB) — Authoritative functional specification & reference-alignment addendum.
  - `TrustPay_Guardian_Antigravity_Prompt_Guide.md` (40 KB) — Authoritative phased prompt guide.
  - `TrustPay_Guardian_Claude_Reference_Comparison.md` (7.7 KB) — Reconciliation decisions and schema contracts.
  - `docs/TECH_STACK_AND_VERSION_POLICY.md` — Verified tool versions and policy.
- **Subdirectories:** 0 existing subdirectories (except newly created `docs/`).
- **Git Status:** Git repository is not yet initialized (`.git` absent).

---

## 2. Existing Frontend, Backend, and Database Stack

- **Frontend Stack:** None present. No `package.json`, no build tool, no source code.
- **Backend Stack:** None present. No `pom.xml`, no Maven wrapper, no Java code.
- **Database Stack:** None configured. No schema, no migration scripts, no connection properties.
- **Entry Points:** None exist yet.

---

## 3. Tool Versions Detected (Local Environment Verification)

- **Java JDK:** `25.0.1 LTS` (`Oracle JDK 25.0.1+8-LTS-27`) — Verified via `java -version`.
- **Java Compiler:** `javac 25.0.1` — Verified via `javac -version`.
- **Maven:** `Apache Maven 3.9.16` — Verified via `mvn -version`.
- **Node.js:** `v24.15.0` — Verified via `node -v`.
- **npm:** `11.12.1` — Verified via `npm -v`.
- **MySQL Server:** `MySQL Community Server 8.4.11 LTS` — Service `MySQL84` is **Running**; Port `3306` is open/verified via `Test-NetConnection`.
- **MySQL Client:** `Ver 8.4.11 for Win64 on x86_64` (`C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`) — Verified.
- **Docker:** Not installed / not in PATH.

---

## 4. Existing Application Behaviour and Features

- **Verified Facts:**
  - Only requirements documents, syllabus, and planning guides currently exist in the folder.
  - MySQL 8.4.11 is verified active on localhost:3306.
  - No application code has been generated or altered.
  - The project is at ground zero (Phase 0).
- **Assumptions Distinguished:**
  - We do not assume Docker or Testcontainers can be used locally without a local Docker engine.
  - We do not assume third-party services (e.g. MailHog, cloud services) are available.

---

## 5. Gaps Against the PRD

1. **Repository & Infrastructure:** Missing Git repo, `.gitignore`, `.editorconfig`, CI workflow, environment config templates.
2. **Persistence Layer:** Missing MySQL instance, Flyway migrations `V1__` through `V6__`, JPA entities, repositories, and constraints.
3. **Security & Identity:** Missing Spring Security filter chain, JWT generator/parser, BCrypt 12, refresh token hash table, 5-attempt brute-force lockout, role definitions (`CUSTOMER`, `FRAUD_ANALYST`, `ADMIN`).
4. **Accounts & Beneficiaries:** Missing demo accounts, masked account numbers (`TPG******1234`), beneficiary validation, ownership checks.
5. **Transaction Engine:** Missing atomic transfer execution, ascending-ID row locking, UUID idempotency key, daily limit checks, demo deposits.
6. **Deterministic Fraud Engine:** Missing 7 strategy-pattern rules (`R1` to `R7`), scoring (0–100), thresholds (40 review / 70 block), fraud alerts, and hit logs.
7. **Analyst & Admin Operations:** Missing case resolution workflow (`RELEASE`/`CONFIRM_BLOCK`), system settings management, immutable audit logging.
8. **User Experience:** Missing React 19 + TypeScript + Vite UI, Bootstrap 5.3 dark/light theme (`tpg-theme`), role-based dashboards, local demo video/poster.
9. **Automated Testing:** Missing JUnit 5 unit/integration tests, MockMvc, frontend Vitest/RTL tests, Playwright smoke tests, JaCoCo coverage reports.

---

## 6. Version Compatibility Recommendations

- **Java & Spring Boot:** Use Java 25 LTS with `--release 25`. Use the latest stable Spring Boot release compatible with Java 25 (Spring Boot 3.4.x / 4.x GA).
- **MySQL:** Install and configure MySQL 8.4 LTS or MySQL 9.x. Use MySQL Connector/J 9.2.0+.
- **Node.js & Frontend:** Node 24.15.0 with React 19.x, TypeScript 5.7+, Vite 6.x, Bootstrap 5.3.3.
- **Pinning & Safety:** All dependencies strictly pinned via Maven POM dependency management and `package-lock.json`. No snapshot or beta libraries.

---

## 7. Proposed Modular Monolith Folder Structure

```text
trustpay-guardian/
├── .github/workflows/ci.yml
├── docs/
│   ├── TECH_STACK_AND_VERSION_POLICY.md
│   ├── PLANNING_GATE_0.md
│   ├── ARCHITECTURE.md
│   ├── API_DOCUMENTATION.md
│   ├── DATABASE_DESIGN.md
│   ├── THREAT_MODEL.md
│   └── REQUIREMENTS_TRACEABILITY.md
├── backend/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/java/com/trustpay/guardian/
│       │   ├── TrustPayGuardianApplication.java
│       │   ├── config/
│       │   ├── security/
│       │   ├── auth/
│       │   ├── user/
│       │   ├── account/
│       │   ├── beneficiary/
│       │   ├── transaction/
│       │   ├── fraud/
│       │   ├── audit/
│       │   ├── notification/
│       │   ├── dashboard/
│       │   └── common/
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── db/migration/
│       │       ├── V1__create_users_and_accounts.sql
│       │       ├── V2__create_beneficiaries_and_transactions.sql
│       │       ├── V3__create_fraud_tables.sql
│       │       ├── V4__create_auth_notifications_audit.sql
│       │       ├── V5__create_system_settings.sql
│       │       └── V6__seed_fraud_rules_and_settings.sql
│       └── test/java/com/trustpay/guardian/
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── public/
│   │   ├── media/
│   │   │   ├── trustpay-guardian-demo.mp4
│   │   │   └── trustpay-guardian-poster.webp
│   │   └── favicon.svg
│   └── src/
│       ├── main.tsx
│       ├── app/
│       ├── components/
│       ├── layouts/
│       ├── features/
│       │   ├── auth/
│       │   ├── dashboard/
│       │   ├── accounts/
│       │   ├── transactions/
│       │   ├── fraud/
│       │   ├── admin/
│       │   └── settings/
│       ├── services/api/
│       └── styles/
├── compose.yaml
├── README.md
├── .gitignore
└── .env.example
```

---

## 8. Proposed Database Entities & Migration Order

1. **`V1__create_users_and_accounts.sql`**
   - `users`: `id`, `full_name`, `email` (unique), `phone_number`, `password_hash`, `role`, `status`, `failed_login_attempts`, `lockout_until`, `created_at`, `updated_at`, `last_login_at`.
   - `accounts`: `id`, `user_id` (FK), `account_number` (unique), `account_type`, `currency` (INR), `balance` (`DECIMAL(19,2)` CHECK `>= 0`), `status`, `daily_limit`, `created_at`, `updated_at`.
2. **`V2__create_beneficiaries_and_transactions.sql`**
   - `beneficiaries`: `id`, `user_id` (FK), `recipient_account_id` (FK), `nickname`, `created_at`, UNIQUE(`user_id`, `recipient_account_id`).
   - `transactions`: `id`, `reference_id` (unique `TXN-...`), `idempotency_key` (unique), `source_account_id` (FK), `destination_account_id` (FK), `amount` (`DECIMAL(19,2)` CHECK `> 0`), `currency` (INR), `type`, `status`, `risk_score`, `failure_reason`, `created_at`, `completed_at`.
3. **`V3__create_fraud_tables.sql`**
   - `fraud_rules`: `id`, `rule_code` (unique `R1`..`R7`), `name`, `description`, `points`, `is_enabled`, `parameters_json`, `updated_at`.
   - `fraud_alerts`: `id`, `transaction_id` (FK), `risk_score`, `risk_level`, `status`, `assigned_to` (FK), `decision_notes`, `created_at`, `resolved_at`.
   - `fraud_alert_hits`: `id`, `alert_id` (FK), `rule_code`, `score_contribution`, `reason_explanation`, `created_at`.
4. **`V4__create_auth_notifications_audit.sql`**
   - `refresh_tokens`: `id`, `user_id` (FK), `token_hash` (unique SHA-256), `expires_at`, `revoked_at`, `replaced_by_hash`, `created_at`.
   - `notifications`: `id`, `user_id` (FK), `type`, `title`, `message`, `read_at`, `created_at`.
   - `audit_logs`: `id`, `actor_user_id` (FK nullable), `action`, `target_type`, `target_id`, `details_json`, `ip_address`, `request_id`, `created_at`.
5. **`V5__create_system_settings.sql`**
   - `system_settings`: `id`, `setting_key` (unique), `setting_value`, `description`, `updated_at`, `updated_by` (FK).
6. **`V6__seed_fraud_rules_and_settings.sql`**
   - Initial threshold values (`REVIEW_THRESHOLD=40`, `BLOCK_THRESHOLD=70`, etc.) and default weights for rules R1 to R7.

---

## 9. Proposed REST Endpoints and Role Permissions

### 9.1 Role Matrix
- **`PUBLIC`**: Unauthenticated endpoints.
- **`CUSTOMER`**: Standard customer banking operations.
- **`FRAUD_ANALYST`**: Flagged alert review and investigation.
- **`ADMIN`**: User administration, rule parameter updates, settings, audit viewing.

### 9.2 Endpoint Summary
| Method | Endpoint | Allowed Roles | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | `PUBLIC` | Register customer (strictly assigns `CUSTOMER`) |
| `POST` | `/api/v1/auth/login` | `PUBLIC` | Authenticate and issue JWT + HttpOnly refresh cookie |
| `POST` | `/api/v1/auth/refresh` | `PUBLIC` | Rotate refresh token and issue new access JWT |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh token |
| `GET` | `/api/v1/auth/me` | Authenticated | Fetch current authenticated user principal |
| `GET` | `/api/v1/accounts` | `CUSTOMER` | Get current customer's accounts |
| `GET` | `/api/v1/accounts/{id}` | `CUSTOMER` | Get account details (enforces ownership) |
| `GET` | `/api/v1/accounts/{id}/transactions`| `CUSTOMER` | Filterable, paginated transaction history |
| `POST` | `/api/v1/beneficiaries` | `CUSTOMER` | Register a new demo beneficiary |
| `GET` | `/api/v1/beneficiaries` | `CUSTOMER` | List customer's saved beneficiaries |
| `POST` | `/api/v1/transfers` | `CUSTOMER` | Initiate simulated transfer (requires `Idempotency-Key`) |
| `POST` | `/api/v1/transfers/deposit` | `CUSTOMER` | Perform simulated demo deposit (capped at ₹50,000) |
| `GET` | `/api/v1/fraud/alerts` | `FRAUD_ANALYST` | Paginated fraud alert queue |
| `GET` | `/api/v1/fraud/alerts/{id}` | `FRAUD_ANALYST` | Inspect alert details, risk score, and rule hits |
| `POST` | `/api/v1/fraud/alerts/{id}/decision`| `FRAUD_ANALYST` | Resolve alert (`RELEASE` or `CONFIRM_BLOCK`) |
| `GET` | `/api/v1/admin/dashboard` | `ADMIN` | System aggregate metrics and pending counts |
| `GET` | `/api/v1/admin/users` | `ADMIN` | Search and filter demo users |
| `PATCH`| `/api/v1/admin/users/{id}/status` | `ADMIN` | Activate or suspend user account |
| `PATCH`| `/api/v1/admin/users/{id}/roles` | `ADMIN` | Update roles (prevents self-demotion) |
| `PATCH`| `/api/v1/admin/rules/{code}` | `ADMIN` | Tune fraud rule points and parameters (audited) |
| `GET` | `/api/v1/admin/audit-logs` | `ADMIN` | Query immutable system audit history |
| `GET` | `/actuator/health` | `PUBLIC` | Safe health and readiness probe |

---

## 10. Threats and Important Security Decisions

1. **Insecure Direct Object Reference (IDOR):** Service layer strictly enforces user ownership of accounts and transactions. Unauthorized resource access returns HTTP 404/403.
2. **Race Conditions / Double Spending:** Database transfers use pessimistic row locking (`SELECT ... FOR UPDATE`) in deterministic ascending ID order (`min(srcId, dstId)` then `max(srcId, dstId)`), preventing deadlocks and concurrent overdrafts.
3. **Network Retries / Duplicate Charges:** Enforce mandatory UUID `Idempotency-Key` header on transfers. Cached results are returned if a duplicate request arrives.
4. **Token Security & XSS/CSRF:** Access tokens reside only in frontend memory. Refresh tokens are 256-bit secure random strings stored as SHA-256 hashes in MySQL and transmitted exclusively via `HttpOnly`, `SameSite=Strict`, `Secure` cookies. Token reuse immediately revokes all sessions.
5. **Brute Force Protection:** 5 consecutive failed login attempts lock the account for 15 minutes. Rate limits of 10 requests/min per IP on login.
6. **Privilege Escalation:** Public registration endpoint hardcodes role assignment to `ROLE_CUSTOMER`. Role alterations are restricted to admins with self-demotion guards.
7. **PII and Secret Protection:** Passwords, tokens, full account numbers (masked as `TPG******1234`), and JWT secrets are redacted from logs and error payloads.

---

## 11. Staged Implementation Phases & Acceptance Gates

- **Phase 0:** Inspection & Master Plan (Current) — Gate: Plan reviewed, no application code changed.
- **Phase 1:** Repository Skeleton & Toolchain — Gate: Spring Boot compiles, React Vite builds, health endpoint responds.
- **Phase 2:** Database Schema & Flyway Migrations — Gate: Clean migration run against MySQL; tables and constraints verified.
- **Phase 3:** Shared Backend Foundation & Security Base — Gate: JWT validation, custom exceptions, error handler, CORS verified.
- **Phase 4:** Authentication & Authorization — Gate: Registration, login, lockout, and token rotation tests pass.
- **Phase 5:** Accounts & Beneficiary Management — Gate: Masked accounts, beneficiary checks, and IDOR prevention tests pass.
- **Phase 6:** Core Transfer Engine & Concurrency Safety — Gate: Atomic transfers, idempotency, and double-spend test scenarios pass.
- **Phase 7:** Deterministic Fraud-Risk Engine — Gate: 7 rules (R1–R7) unit tested across score boundaries; alerts created.
- **Phase 8:** Fraud Analyst Workflow — Gate: Analyst resolution (`RELEASE`/`CONFIRM_BLOCK`) atomic execution verified.
- **Phase 9:** Administration & Audit Logging — Gate: User status/role management, self-demotion prevention, and audit logging verified.
- **Phase 10:** Backend Observability, Quality & Hardening — Gate: JaCoCo coverage >= 80% on core services; OpenAPI spec valid.
- **Phase 11:** Frontend Shell, Theme & Landing Page — Gate: Responsive landing page, exact title, dark/light toggle (`tpg-theme`).
- **Phase 12:** Frontend Customer Experience — Gate: Customer registration, login, dashboard, and transfer execution verified in browser.
- **Phase 13:** Frontend Analyst & Admin Consoles — Gate: Analyst queue inspection, decision modals, and admin settings screens verified.
- **Phase 14:** Full-Stack Integration & Smoke Tests — Gate: End-to-end user journeys pass cleanly; Playwright smoke test green.
- **Phase 15:** Docker Compose & CI Pipeline (Packaging & CI) — Author `Dockerfile.backend`, `Dockerfile.frontend`, and `compose.yaml` for container distribution and GitHub Actions CI; running Docker locally is **strictly optional** (local dev remains 100% native on Windows). Gate: Dockerfiles and CI workflows valid and linted.
- **Phase 16:** Final Documentation, Q&A & Demo Walkthrough — Gate: 100% documentation aligned with actual code; full simulation demo ready.

---

## 12. Execution Commands

### Windows PowerShell (Native Host Execution)
```powershell
# 1. Environment verification
java -version; javac -version; mvn -version; node -v; npm -v

# 2. Backend verification (from Phase 1 onward)
cd backend
.\mvnw.cmd clean verify

# 3. Frontend verification (from Phase 1 onward)
cd frontend
npm install
npm run build
npm test
```

### macOS / Linux Alternatives
```bash
# 1. Environment verification
java -version && javac -version && mvn -version && node -v && npm -v

# 2. Backend verification
cd backend && ./mvnw clean verify

# 3. Frontend verification
cd frontend && npm install && npm run build && npm test
```

---

## 13. Infrastructure Decisions, Blockers, and Required Approvals

1. **MySQL Installation (Resolved):**
   - MySQL 8.4.11 LTS is installed, service `MySQL84` is running, and TCP port 3306 is open and verified.
   - User credentials and secrets will NOT be shared or hardcoded; standard environment variable placeholders (`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) will be utilized in `.env.example`.
2. **Docker / Containerization Policy (Resolved):**
   - **Docker is strictly optional across all phases.**
   - All components (Spring Boot backend, React Vite frontend, MySQL database) will run natively on the Windows host.
   - In Phase 15, container configurations (`Dockerfile.backend`, `Dockerfile.frontend`, `compose.yaml`) will be generated for packaging and GitHub Actions CI, but local Docker installation is never required.
3. **Approval to Proceed:**
   - With MySQL active on port 3306, Docker documented as optional, and all version conflicts reconciled against official sources, we await your approval to implement **Phase 1 (Repository Skeleton & Toolchain)**.
