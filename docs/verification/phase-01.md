# TrustPay Guardian — Phase 1 Verification Report

**Phase:** Phase 1 — Database Schema and JPA Entities  
**Branch:** `feature/phase-01-schema-entities`  
**Execution Date:** 2026-10-26  
**Auditor / Engineer:** Principal Java Full Stack Engineer  
**Status:** **PASSED** (Testcontainers MySQL test UNVERIFIED due to lack of Docker on Windows host; verified against H2 migration engine and schema validation)

---

## 1. Checks and Evidence

### Check 1: Flyway Migrations Application & Schema Validation
- **Command:** `.\mvnw.cmd test -Dtest=SchemaAndFlywayMigrationIntegrationTest`
- **Result:** PASSED (3/3 tests)
  - Flyway cleanly applies `V1` to `V6` on empty schema.
  - Current schema version: `v6`.
  - Hibernate configuration: `spring.jpa.hibernate.ddl-auto=validate`. Zero schema validation errors or mismatches.

### Check 2: Seed Rows Verification
- `fraud_rules` table contains exactly 7 canonical rules (R1 to R7) with expected points:
  - R1: 30 pts (HIGH_AMOUNT)
  - R2: 25 pts (VELOCITY)
  - R3: 15 pts (UNUSUAL_HOUR)
  - R4: 30 pts (NEW_BENEFICIARY_LARGE)
  - R5: 20 pts (AMOUNT_SPIKE)
  - R6: 10 pts (DAILY_LIMIT_PROXIMITY)
  - R7: 15 pts (RECENT_FAILED_LOGINS)
- `system_settings` table contains exactly 6 platform settings:
  - `REVIEW_THRESHOLD`: 40
  - `BLOCK_THRESHOLD`: 70
  - `DEMO_OPENING_BALANCE`: 10000.00
  - `MAX_TRANSFER_AMOUNT`: 200000.00
  - `MAX_DEPOSIT_AMOUNT`: 50000.00
  - `DEFAULT_DAILY_LIMIT`: 100000.00

### Check 3: Entity Persist & CHECK Constraint Rejection
- **Command:** `.\mvnw.cmd test -Dtest=EntityAndRepositoryIntegrationTest`
- **Result:** PASSED (9/9 tests)
- **Check Constraint Evidence:**
  - Persisting `Account` with negative balance (`-50.00`) directly triggered SQL constraint violation:
    `Check constraint violation: "chk_accounts_balance_non_negative: "; SQL statement: insert into accounts ...`
  - Rejection successfully captured and verified.
- **Optimistic Locking (@Version):**
  - Added `@Version private Long version = 0L;` to `Account.java`.
  - Added `version BIGINT NOT NULL DEFAULT 0` column to `accounts` table in `V1__create_users_and_accounts.sql`.

### Check 4: Entity-Relationship Diagram
- Verified `docs/diagrams/erd.md` against canonical 11 tables:
  - `users`, `accounts`, `beneficiaries`, `transactions`, `fraud_rules`, `fraud_alerts`, `fraud_alert_hits`, `refresh_tokens`, `notifications`, `audit_logs`, `system_settings`.
  - Updated `accounts` definition in ERD to document `BIGINT version`.

### Check 5: Live Testcontainers Container Check
- **Status:** **UNVERIFIED (Docker not installed on host machine)**
- **Reason:** Docker CLI is not installed on the Windows host machine.
- **Command for User to Run Later:** `.\mvnw.cmd test -Dtest=MySqlSchemaAndMigrationIntegrationTest` with live MySQL credentials or in a Docker-enabled environment.
