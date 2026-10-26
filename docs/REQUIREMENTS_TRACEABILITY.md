# TrustPay Guardian — Requirements Traceability Matrix (RTM)

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Engine:** MySQL Community Server 8.4.11 LTS  
**Last Updated:** 2026-10-17  
**Current Status:** **DOMAIN IMPLEMENTED & H2 VERIFIED; LIVE MYSQL 8.4 VERIFICATION BLOCKED AWAITING AUTHENTICATION**  

---

## 1. Traceability Matrix

| Requirement Ref | Requirement Description | Priority | Implemented In | Verification / Test | Status |
| :--- | :--- | :---: | :--- | :--- | :---: |
| **PRD-PERSIST-01** | 11 canonical entities with explicit PKs, FKs, and indexes | `[M]` | `V1`–`V5` Migrations, JPA Domain Entities | `EntityAndRepositoryIntegrationTest` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-02** | Fixed-precision monetary storage (`DECIMAL(19,2)`, `BigDecimal`) | `[M]` | `accounts`, `transactions` schema and entities | `EntityAndRepositoryIntegrationTest` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-03** | Engine-level balance non-negativity constraint | `[M]` | `V1__create_users_and_accounts.sql` | `chk_accounts_balance_non_negative` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-04** | Engine-level positive transaction amount constraint | `[M]` | `V2__create_beneficiaries_and_transactions.sql` | `chk_tx_amount_positive` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-05** | Transaction type/source account consistency constraint | `[M]` | `V2__create_beneficiaries_and_transactions.sql` | `chk_tx_type_source` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-06** | Distinct source and destination accounts constraint | `[M]` | `V2__create_beneficiaries_and_transactions.sql` | `chk_tx_distinct_accounts` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-07** | Risk score boundaries (0–100) constraint | `[M]` | `V2` and `V3` Migrations | `chk_tx_risk_score`, `chk_fraud_alerts_score` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-08** | Compound unique constraint on beneficiaries `(user_id, recipient)` | `[M]` | `V2__create_beneficiaries_and_transactions.sql` | `uk_beneficiaries_user_recipient` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-09** | Idempotent platform settings and 7 fraud rules seeding | `[M]` | `V6__seed_fraud_rules_and_settings.sql` | `SchemaAndFlywayMigrationIntegrationTest` | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-PERSIST-10** | Dynamic Available Balance fund reservation mechanics | `[M]` | `TransactionRepository`, `Account`, `AccountService` | `BalanceReservationAndStateTransitionTest` | **H2 VERIFIED** |
| **PRD-PERSIST-11** | Least-privilege dual-user database security (Option A) | `[M]` | `application-dev.yml`, `.env.example` | Documented; schema-wide write grant limitation identified | **BLOCKED (MYSQL AUTH)** |
| **PRD-PERSIST-12** | MySQL non-transactional DDL failure recovery runbook | `[M]` | `MigrationFailureRecoveryTest` | Flyway repair simulated & tested | **H2 VERIFIED / PENDING MYSQL** |
| **PRD-AUTH-01** | User registration and role assignment (`CUSTOMER` default) | `[M]` | `AuthService.register()`, `AuthController` | `AuthServiceIntegrationTest#testRegisterSuccess` | **H2 VERIFIED** |
| **PRD-AUTH-02** | 5-failure brute force login lockout (15 mins) | `[M]` | `AuthService.login()`, `User.failedLoginAttempts` | `AuthServiceIntegrationTest#testAccountLockedAfter5Failures` | **H2 VERIFIED** |
| **PRD-AUTH-03** | 15-min JWT access token & 7-day hashed refresh rotation | `[M]` | `JwtTokenProvider`, `AuthService.refreshToken()` | `AuthServiceIntegrationTest#testLoginSuccess` | **H2 VERIFIED** |
| **PRD-TXN-01** | Atomic transfer with ascending row locks & idempotency | `[M]` | `TransferService.executeTransfer()`, `AccountRepository` | `TransferServiceIntegrationTest` (4 tests) | **H2 VERIFIED** |
| **PRD-FRAUD-01** | 7 Strategy-pattern fraud rules (`R1`–`R7`, no AI/ML) | `[M]` | `FraudScoringService.evaluateTransaction()` | `FraudScoringServiceTest` (3 tests) | **H2 VERIFIED** |
| **PRD-ANALYST-01** | Analyst case review (`RELEASE` / `CONFIRM_BLOCK`) | `[M]` | `FraudAnalystService`, `FraudAnalystController` | `TransferServiceIntegrationTest`, UI verification | **H2 VERIFIED** |
| **PRD-AUDIT-01** | Append-only immutable audit logging | `[M]` | `AuditService.logAction()`, `AuditLogRepository` | `AdminServiceIntegrationTest`, schema inspection | **H2 VERIFIED** |
| **PRD-ADMIN-01** | Platform metrics, user governance & rule tuning | `[M]` | `AdminService`, `AdminController` | `AdminServiceIntegrationTest` (5 tests) | **H2 VERIFIED** |
| **PRD-UI-01** | Role-based navigation, persona switcher & responsive UI | `[M]` | React 19, TypeScript, Bootstrap 5.3, Vite 6 | Production build (`npm run build`) | **VERIFIED** |
