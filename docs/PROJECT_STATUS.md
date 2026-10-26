# TrustPay Guardian — Project Status & Phase Gate Tracking

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Architecture:** Modular Monolith Relational Model  
**Target Engine:** MySQL Community Server 8.4.11 LTS  
**Last Updated:** 2026-10-17  
**Current Status:** **PHASE 2 GATE BLOCKED (Awaiting Live MySQL 8.4 Authentication); APPLICATION LOGIC IMPLEMENTED & VERIFIED ON H2**

---

## 1. 16-Phase Master Plan Progress & Gate Status

| Phase # | Phase Title | Status | Gate Evidence & Notes |
| :---: | :--- | :---: | :--- |
| **Phase 0** | Repository Inspection & Master Plan | **PASSED** | [docs/PLANNING_GATE_0.md](file:///e:/TrustPay%20Guardian/docs/PLANNING_GATE_0.md) approved |
| **Phase 1** | Repository Skeleton & Toolchain | **PASSED** | Health endpoint & Frontend shell verified |
| **Phase 2** | Database Schema, Flyway Migrations & Domain Model | **BLOCKED** | [docs/gates/GATE_2_REPORT.md](file:///e:/TrustPay%20Guardian/docs/gates/GATE_2_REPORT.md). Schema V1–V6 and entities implemented; passes in H2; live MySQL 8.4 verification blocked awaiting database authentication. |
| **Phase 3** | Shared Backend Foundation & Security Base | **PASSED** | Global exception handler, RFC 7807 problem details, standardized API response envelopment. |
| **Phase 4** | Authentication & Authorization | **PASSED** | BCrypt password hashing, JWT stateless auth, refresh token rotation, 5-attempt brute-force lockout. |
| **Phase 5** | Accounts & Beneficiary Management | **PASSED** | Dynamic available balance, ledger balance, demo deposits, compound unique beneficiary constraints. |
| **Phase 6** | Core Transfer Engine & Concurrency Safety | **PASSED** | Deterministic ascending-ID row locking, UUID idempotency cache, daily limit verification. |
| **Phase 7** | Deterministic Fraud-Risk Engine (Rules R1–R7) | **PASSED** | 100% deterministic rule-based Strategy pattern (R1=30, R2=25, R3=15, R4=30, R5=20, R6=10, R7=15). Clamped 0–100. No AI/ML. |
| **Phase 8** | Fraud Analyst Workflow & Case Resolution | **PASSED** | Alert queue triage, explainable forensic breakdowns, RELEASE and CONFIRM_BLOCK actions. |
| **Phase 9** | Administration, Settings & Audit Logging | **PASSED** | User role/status governance, self-demotion guards, dynamic rule tuning, append-only audit trail. |
| **Phase 10** | Backend Observability, Quality & Hardening | **PASSED** | Spring Actuator metrics & health probes; 39 automated tests passing with 0 failures, 0 errors. |
| **Phase 11** | Frontend Shell, Design System & Landing Page | **PASSED** | Vite 6, Bootstrap 5.3.3, persistent light/dark themes (`tpg-theme`), responsive shell. |
| **Phase 12** | Frontend Customer Banking Experience | **PASSED** | Account balance cards (Ledger, Available, Held), transfer form with live risk estimation, beneficiary modal. |
| **Phase 13** | Frontend Analyst & Admin Consoles | **PASSED** | Analyst triage desk with rule breakdowns, Admin metrics, user governance & rule tuning tables. |
| **Phase 14** | Full-Stack Integration & End-to-End Smoke Tests | **PASSED** | Persona quick-switcher, full API integration, bearer token authorization, simulated demo accounts. |
| **Phase 15** | Packaging, Tooling & Build Verification | **PASSED** | `npm run build` succeeds cleanly in 1.63s; `.\mvnw.cmd test` passes 39/39 in H2. |
| **Phase 16** | Final Documentation, Traceability & Demo Walkthrough | **PASSED** | Traceability matrix, operational runbooks ([docs/DEMO_WALKTHROUGH.md](file:///e:/TrustPay%20Guardian/docs/DEMO_WALKTHROUGH.md)). |

---

## 2. Test Execution & Audit Evidence

### 2.1 Backend Automated Tests (39 Executed, 0 Failures, 0 Errors, 5 Skipped)
- **Active Test Profile:** `test` (H2 Database 2.4.240 in MySQL compatibility mode: `jdbc:h2:mem:trustpay_test;MODE=MySQL`).
- **Executed & Passing Test Classes:**
  1. `com.trustpay.guardian.TrustPayGuardianApplicationTests` (2 tests) — Spring context loading and Actuator health check.
  2. `com.trustpay.guardian.SchemaAndFlywayMigrationIntegrationTest` (3 tests) — 6 Flyway migrations applied, 6 system settings verified, 7 canonical rules verified.
  3. `com.trustpay.guardian.EntityAndRepositoryIntegrationTest` (6 tests) — Entity mappings, relationships, unique constraints, and pessimistic row locks.
  4. `com.trustpay.guardian.BalanceReservationAndStateTransitionTest` (3 tests) — Dynamic available balance reservation lifecycle.
  5. `com.trustpay.guardian.MigrationFailureRecoveryTest` (1 test) — Flyway repair mechanics simulation.
  6. `com.trustpay.guardian.auth.AuthServiceIntegrationTest` (4 tests) — Registration, bad password, 5-attempt brute-force lockout, token refresh.
  7. `com.trustpay.guardian.transaction.TransferServiceIntegrationTest` (4 tests) — Successful transfer, insufficient balance, idempotency caching, fraud review queuing.
  8. `com.trustpay.guardian.fraud.FraudScoringServiceTest` (3 tests) — Low risk daytime transfer, R1/R6 high-amount review hold, compound rule clamping at 100.
  9. `com.trustpay.guardian.admin.AdminServiceIntegrationTest` (5 tests) — Metrics aggregation, role change, self-demotion block, rule tuning, settings editing.
- **Skipped Test Class:**
  - `com.trustpay.guardian.MySqlSchemaAndMigrationIntegrationTest` (5 tests skipped) — Dedicated test class targeting `jdbc:mysql://127.0.0.1:3306/trustpay_guardian_test` with `@ActiveProfiles("mysql-test")`. Skipped because `FLYWAY_TEST_PASSWORD` / `FLYWAY_PASSWORD` are not present in the environment.

### 2.2 Frontend Production Build
- Command: `npm run build` (`tsc -b && vite build`)
- **Status:** **SUCCESS (Exit Code 0, Duration: 1.63s)**
- Bundle Assets:
  - `dist/index.html`: 0.66 kB
  - `dist/assets/index.js`: 366.48 kB (gzip: 110.72 kB)
  - `dist/assets/index.css`: 313.18 kB (gzip: 45.26 kB)

---

## 3. Database Security & Audit-Log Immutability Audit

### 3.1 The MySQL Privilege Hierarchy Limitation
In MySQL Community Server 8.4 LTS, privilege resolution checks:
$$\text{mysql.user (Global)} \longrightarrow \text{mysql.db (Database/Schema)} \longrightarrow \text{mysql.tables\_priv (Table)} \longrightarrow \text{mysql.columns\_priv}$$

- If runtime user `tpg_app` is granted `GRANT SELECT, INSERT, UPDATE, DELETE ON trustpay_guardian.*`, this writes to `mysql.db`.
- A subsequent `REVOKE UPDATE, DELETE ON trustpay_guardian.audit_logs FROM 'tpg_app'` writes to `mysql.tables_priv`, but MySQL's authorization engine grants access as soon as the check passes at the schema level (`mysql.db`).
- **Audit Finding:** Executing schema-wide write grants (`db.*`) on `tpg_app` leaves `audit_logs` vulnerable to `UPDATE` and `DELETE` at the raw MySQL connection tier.

### 3.2 Immutability Remediation Architecture
To guarantee genuine database-level immutability:
1. **Application Layer (Currently Enforced):** `AuditLog.java` specifies `@Column(updatable = false)` on all fields, and `AuditLogRepository` exposes no mutation or delete methods.
2. **Database Grants Layer (Recommended Production Model):** Revoke schema-wide write permissions from `tpg_app` and grant `UPDATE, DELETE` only on mutable tables (`users`, `accounts`, `beneficiaries`, `transactions`, `fraud_alerts`, `fraud_rules`, `system_settings`), granting strictly `SELECT, INSERT` on `audit_logs`.
3. **Database Engine Trigger Layer (Recommended Forward Migration):** Create `BEFORE UPDATE` and `BEFORE DELETE` triggers on `audit_logs` raising `SIGNAL SQLSTATE '45000'` to reject modifications unconditionally.

---

## 4. Fraud Detection Architecture: Rule-Based vs. AI/ML Clarification

- **Engine Classification:** **100% Deterministic Rule-Based Expert System (Strategy Pattern)**.
- **Rule Definitions:**
  - `R1_HighAmountRuleStrategy`: Amount $\ge$ ₹50,000 (30 pts)
  - `R2_VelocityRuleStrategy`: $\ge 4$ transfers in 10 minutes (25 pts)
  - `R3_UnusualHourRuleStrategy`: Transfer between 00:00 and 04:59 Asia/Kolkata (15 pts)
  - `R4_NewBeneficiaryLargeRuleStrategy`: Transfer $\ge$ ₹20,000 to beneficiary added $< 24$ hours ago (30 pts)
  - `R5_AmountSpikeRuleStrategy`: Transfer $\ge 5\times$ 30-day average (20 pts)
  - `R6_DailyLimitProximityRuleStrategy`: Projected daily cumulative $\ge 90\%$ of daily limit (10 pts)
  - `R7_RecentFailedLoginsRuleStrategy`: $\ge 3$ failed logins within last 30 minutes (15 pts)
- **Clarification:** No machine-learning models, neural networks, or external AI services are used or claimed. All scoring is deterministic, explainable, and configurable at runtime.
