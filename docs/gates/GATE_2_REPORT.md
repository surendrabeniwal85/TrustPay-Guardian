# TrustPay Guardian — Phase 2 Gate Report

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Phase:** Phase 2 — Database Schema, Flyway Migrations & Domain Persistence Layer  
**Date:** 2026-10-17  
**Evaluator:** Senior Software Architect & Technical Mentor  
**Final Gate Decision:** **`BLOCKED`** (Awaiting Local MySQL 8.4.11 Authentication)  

---

## 1. Executive Summary & Verification Context

During the initial Phase 2 review, conditional approval was granted with the critical observation that integration test logs demonstrated Flyway execution against an in-memory H2 database (`H2 2.4.240` in MySQL compatibility mode) rather than the project's target database engine: **MySQL Community Server 8.4.11 LTS**.

In accordance with explicit verification instructions:
1. The local environment, toolchain, migration scripts, entity models, tests, and Git status were systematically inspected.
2. The local MySQL installation was inspected without printing or soliciting passwords.
3. Live execution against MySQL 8.4.11 was attempted using standard local connection paths.
4. Because the local MySQL instance requires authentication and credentials are not present in the runtime environment (and security rules strictly prohibit requesting or logging credentials), live execution against MySQL could not be autonomously completed.
5. As mandated by gate rule 9, this report declares **`BLOCKED`** and explicitly differentiates between verified static/H2 checks and the pending live MySQL 8.4.11 verification. Under no circumstances are H2 tests presented as MySQL verification.

---

## 2. Local MySQL 8.4.11 Environment Verification

The local host environment was probed to verify the presence and status of MySQL Community Server 8.4.11.

| Check | Command Executed | Exit Code | Observed Output / Evidence | Status |
| :--- | :--- | :---: | :--- | :---: |
| **MySQL Client Binary** | `& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --version` | `0` | `Ver 8.4.11 for Win64 on x86_64 (MySQL Community Server - GPL)` | **VERIFIED** |
| **MySQL Windows Service** | `Get-Service *mysql*` | `0` | `Status: Running`, `Name: MySQL84`, `DisplayName: MySQL84` | **VERIFIED** |
| **Port Connectivity** | `Test-NetConnection -ComputerName 127.0.0.1 -Port 3306` | `0` | `TcpTestSucceeded: True` | **VERIFIED** |

**Conclusion:** MySQL Community Server 8.4.11 is installed, actively running as a Windows Service (`MySQL84`), and listening on port 3306.

---

## 3. Connection & Authentication Audit (The Blocker)

To execute Flyway migrations against an isolated database (`trustpay_guardian_test`) without altering existing data or guessing passwords, connection attempts were tested against standard local accounts.

| Account Attempted | Command Executed | Exit Code | Output / Blocker Evidence |
| :--- | :--- | :---: | :--- |
| `root` (unauthenticated) | `& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -e "SELECT 1;"` | `1` | `ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: NO)` |
| `tpg_flyway` (unauthenticated) | `& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u tpg_flyway -e "SELECT 1;"` | `1` | `ERROR 1045 (28000): Access denied for user 'tpg_flyway'@'localhost' (using password: NO)` |
| `tpg_app` (unauthenticated) | `& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u tpg_app -e "SELECT 1;"` | `1` | `ERROR 1045 (28000): Access denied for user 'tpg_app'@'localhost' (using password: NO)` |

### Environment Variable & Secrets Audit
- Inspected session environment variables for database credentials:
  - `Get-ChildItem Env: | Where-Object { $_.Name -match "MYSQL|FLYWAY|DATASOURCE|DB" }` returned empty (no credentials exposed).
- Inspected local option files:
  - `$env:USERPROFILE\.my.cnf`: **Not found** (`False`)
  - `$env:USERPROFILE\.mylogin.cnf`: **Not found** (`False`)
- Adhered strictly to instruction: *"Do not print passwords or request that I share credentials. If MySQL verification cannot run, report BLOCKED and explain the precise reason. Do not claim a successful MySQL test based on H2."*

---

## 4. Static Migration Scripts DDL Audit (MySQL 8.4.11 Compliance)

All 6 Flyway migrations in `backend/src/main/resources/db/migration/` were audited line-by-line for MySQL 8.4.11 LTS compatibility:

### 4.1 Storage Engine, Charset & Collation
- All tables explicitly define `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.
- Fully supported and standard in MySQL 8.4.11.

### 4.2 Monetary & Precision Types
- `accounts.balance`: `DECIMAL(19,2) NOT NULL DEFAULT 0.00`
- `accounts.daily_limit`: `DECIMAL(19,2) NOT NULL DEFAULT 100000.00`
- `transactions.amount`: `DECIMAL(19,2) NOT NULL`
- Fully compatible with MySQL 8.4.11 exact arithmetic; maps to Java `BigDecimal`.

### 4.3 Engine-Level CHECK Constraints (MySQL 8.0.16+ & 8.4 LTS)
MySQL 8.4 natively enforces table and column `CHECK` constraints:
1. `chk_accounts_balance_non_negative`: `CHECK (balance >= 0)` in `V1`
2. `chk_accounts_daily_limit_positive`: `CHECK (daily_limit > 0)` in `V1`
3. `chk_tx_amount_positive`: `CHECK (amount > 0)` in `V2`
4. `chk_tx_distinct_accounts`: `CHECK (source_account_id <> destination_account_id)` in `V2`
5. `chk_tx_type_source`: `CHECK ((tx_type = 'DEPOSIT' AND source_account_id IS NULL) OR (tx_type = 'TRANSFER' AND source_account_id IS NOT NULL))` in `V2`
6. `chk_tx_risk_score`: `CHECK (risk_score >= 0 AND risk_score <= 100)` in `V2`
7. `chk_fraud_rules_weight`: `CHECK (points >= 0 AND points <= 100)` in `V3`
8. `chk_fraud_alerts_score`: `CHECK (risk_score >= 0 AND risk_score <= 100)` in `V3`

### 4.4 ANSI SQL Timestamps & Idempotent Seeding
- `V6__seed_fraud_rules_and_settings.sql` uses ANSI standard `CURRENT_TIMESTAMP(6)`.
- Seeding utilizes `INSERT INTO ... ON DUPLICATE KEY UPDATE description = VALUES(description);` to ensure runtime-configured setting values are never overwritten during restarts.

---

## 5. Supplementary In-Memory Test Suite (H2 Compatibility)

> [!NOTE]
> The tests below were executed against H2 in MySQL compatibility mode (`jdbc:h2:mem:trustpay_test;MODE=MySQL`). They demonstrate code compilation, JPA entity mapping, Spring Data query derivation, and business logic algorithms (e.g., Available Balance calculation and Flyway repair mechanics), but **do NOT constitute MySQL 8.4.11 engine verification**.

| Test Class | Tests Run | Result | Focus Area |
| :--- | :---: | :---: | :--- |
| `TrustPayGuardianApplicationTests` | 2 | PASSED | Context load and actuator health check |
| `SchemaAndFlywayMigrationIntegrationTest` | 3 | PASSED | Migration sequence V1–V6 and V6 seed assertions |
| `EntityAndRepositoryIntegrationTest` | 8 | PASSED | 11 entities, relations, pessimistic locks, custom queries |
| `BalanceReservationAndStateTransitionTest` | 4 | PASSED | Available Balance reservation across hold, block, release, cancel |
| `MigrationFailureRecoveryTest` | 1 | PASSED | MySQL non-transactional DDL failure simulation & `flyway.repair()` |
| **Total** | **18** | **0 Failures** | Execution time: 16.11s |

---

## 6. Detailed Requirements Checklist & Gate Decision

| ID | Verification Requirement | Status | Evidence / Notes |
| :--- | :--- | :---: | :--- |
| **REQ-1** | Inspect configuration, migrations, tests, Git status | **COMPLETED** | Verified clean repository, valid DDL scripts, 18 passing tests, clean Git working directory. |
| **REQ-2** | Verify actual local MySQL version without printing/asking passwords | **COMPLETED** | Verified `MySQL Community Server 8.4.11 LTS` on port 3306 via client binary and service. |
| **REQ-3** | Execute all 6 migrations against actual MySQL 8.4.11 in isolated schema | **BLOCKED** | Blocked by missing database credentials; authentication denied on `localhost`. |
| **REQ-4** | Verify 11 tables, FKs, unique keys, CHECK constraints, 6 settings, 7 rules | **BLOCKED** | Pending execution against live MySQL engine once credentials are provided. |
| **REQ-5** | Verify Option A least-privilege permissions (no UPDATE/DELETE on audit_logs) | **BLOCKED** | Documented in DDL/grants specification; requires MySQL root session to inspect `mysql.user`. |
| **REQ-6** | Run repository and entity checks against actual MySQL | **BLOCKED** | Blocked by lack of live database connection. |
| **REQ-7** | Do not drop, reset, or modify existing user data | **COMPLETED** | No existing databases or user data were altered. All tests targeted memory or isolated schemas. |
| **REQ-8** | Record exact commands, outputs, exit codes in report | **COMPLETED** | All commands, exit codes, and raw outputs documented in Sections 2 and 3. |
| **REQ-9** | If MySQL cannot run, report BLOCKED; do not claim H2 as MySQL | **COMPLETED** | Reported as BLOCKED; H2 results clearly segregated as supplementary only. |
| **REQ-10**| Stop after verification; do not begin Phase 3 | **COMPLETED** | Phase 3 implementation has not started. Awaiting user approval. |

---

## 7. Developer Runbook to Unblock MySQL Verification

To complete live verification against your local MySQL 8.4.11 instance, follow this non-destructive runbook using a dedicated, isolated test schema (`trustpay_guardian_test`):

### Step 1: Provision Isolated Database & Grant Roles (Run once in MySQL Workbench or CLI)
```sql
-- Connect as root or an administrative user:
CREATE DATABASE IF NOT EXISTS trustpay_guardian_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Option A Users (adjust passwords as desired):
CREATE USER IF NOT EXISTS 'tpg_flyway'@'localhost' IDENTIFIED BY 'FlywaySecret123!';
CREATE USER IF NOT EXISTS 'tpg_app'@'localhost' IDENTIFIED BY 'AppSecret123!';

-- Migration User: Full DDL and DML on isolated test schema
GRANT ALL PRIVILEGES ON trustpay_guardian_test.* TO 'tpg_flyway'@'localhost';

-- Application User: DML only on isolated test schema
GRANT SELECT, INSERT, UPDATE, DELETE ON trustpay_guardian_test.* TO 'tpg_app'@'localhost';
REVOKE DROP, CREATE, ALTER, INDEX, REFERENCES ON trustpay_guardian_test.* FROM 'tpg_app'@'localhost';
REVOKE UPDATE, DELETE ON trustpay_guardian_test.audit_logs FROM 'tpg_app'@'localhost';

FLUSH PRIVILEGES;
```

### Step 2: Execute Isolated Verification via Script
In Windows PowerShell from the repository root:
```powershell
# Set environment variables for the test run:
$env:SPRING_DATASOURCE_TEST_PASSWORD = "AppSecret123!"
$env:FLYWAY_TEST_PASSWORD = "FlywaySecret123!"

# Execute the isolated MySQL verification script:
.\scripts\verify-mysql.ps1 -Database "trustpay_guardian_test"
```

### Step 3: Run Spring Boot Integration Tests Against MySQL
```powershell
Push-Location backend
.\mvnw.cmd test "-Dspring.profiles.active=mysql-test"
Pop-Location
```

---

## 8. Final Decision

### **GATE DECISION: `BLOCKED`**

**Reason:**  
MySQL Community Server 8.4.11 is verified as present and running on the local system, but live execution cannot proceed autonomously without database credentials. In accordance with instructions, no passwords were asked for or printed, and H2 tests are not substituted for MySQL verification.

**Next Actions:**  
Once you have executed the isolated runbook above against your local MySQL 8.4.11 instance and confirmed the output, grant approval to transition Phase 2 from `BLOCKED` to `PASS` and initiate Phase 3.
