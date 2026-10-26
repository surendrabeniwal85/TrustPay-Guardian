# TrustPay Guardian — Architecture Decision Records (ADRs)

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Engine:** MySQL Community Server 8.4.11 LTS  
**Last Updated:** 2026-10-17  

---

## ADR-001: Modular Monolith Relational Architecture
- **Status:** APPROVED
- **Context:** The system requires atomic multi-account transfers, complex deterministic fraud scoring across recent user activity, and explainable audit trails.
- **Decision:** Build a modular monolith in Spring Boot with a single MySQL relational database. Do not introduce microservices, distributed queues (Kafka), or Kubernetes for v1.
- **Consequences:** Eliminates distributed transaction failures (2PC), enables immediate relational consistency, minimizes deployment complexity, and keeps the project testable on a single machine.

---

## ADR-002: Fixed-Precision Financial Arithmetic (`DECIMAL(19,2)` and `BigDecimal`)
- **Status:** APPROVED
- **Context:** Floating-point representations (`float`, `double`) introduce binary rounding errors (e.g. `0.1 + 0.2 != 0.3`) which violate core banking integrity.
- **Decision:** All stored monetary amounts in MySQL strictly use `DECIMAL(19,2)`. In Java, financial amounts strictly use `BigDecimal`. Floating-point types are strictly forbidden.
- **Consequences:** Exact precision to two decimal places; total compliance with financial accounting standards.

---

## ADR-003: Principle of Least Privilege Database Security (Option A Dual-Account Model)
- **Status:** APPROVED
- **Context:** Granting DDL privileges (`DROP`, `CREATE`, `ALTER`) to the normal runtime application account exposes the database to structural destruction in the event of an injection vulnerability.
- **Decision:** Separate schema-migration privileges from runtime application privileges:
  - `tpg_flyway`: Possesses DDL + DML to run Flyway migrations during startup / maintenance.
  - `tpg_app`: Possesses DML only (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) for application runtime queries.
  - Privileges to `UPDATE` and `DELETE` on `audit_logs` are revoked for `tpg_app` to preserve append-only immutability.
- **Consequences:** Defense-in-depth against malicious DDL execution; local-development fallback allows a single administrative user if documented.

---

## ADR-004: Dynamic Available Balance Calculation & Reservation Lifecycle
- **Status:** APPROVED
- **Context:** Naive implementations either prematurely deduct money from the ledger when a transaction is flagged for fraud review, or fail to reserve funds, enabling double-spending while awaiting analyst review.
- **Decision:** Available balance is computed dynamically:
  $$\text{Available Balance} = \text{accounts.balance} - \sum_{\text{PENDING\_REVIEW}} \text{amount}$$
  - Direct approval atomically debits sender and credits recipient.
  - Review holds (`PENDING_REVIEW`) reserve funds without modifying ledger balances.
  - Analyst `RELEASE` converts the reservation to a posted ledger debit.
  - Analyst `CONFIRM_BLOCK` transitions the transaction to `BLOCKED` and releases the reservation exactly once.
- **Consequences:** Zero drift between ledger balances and transactions; no phantom debits; no stranded reservations.

---

## ADR-005: Global Idempotency Key with Mandatory Service-Layer Ownership Verification
- **Status:** APPROVED
- **Context:** Global `UNIQUE (idempotency_key)` protects against duplicate charges, but returning cached transactions across tenants introduces an IDOR information disclosure vulnerability.
- **Decision:** Maintain global `UNIQUE (idempotency_key)` on `transactions`. In the service layer, before returning a cached transaction response, verify `existingTransaction.sourceAccount.userId == currentUser.id`. If an ownership mismatch occurs, immediately reject with HTTP 409 Conflict without revealing details.
- **Consequences:** Prevents duplicate transactions globally while completely shutting down cross-tenant data exposure attacks.

---

## ADR-006: Standard SQL Timestamping (`CURRENT_TIMESTAMP(6)`)
- **Status:** APPROVED
- **Context:** `UTC_TIMESTAMP(6)` is a proprietary MySQL function that fails in standard SQL engines and H2 test environments.
- **Decision:** Use standard ANSI SQL `CURRENT_TIMESTAMP(6)` across migration scripts, paired with a mandatory server UTC timezone configuration (`serverTimezone=UTC` in JDBC and JVM `TimeZone.setDefault("UTC")`).
- **Consequences:** Seamless portability and flawless integration testing without modifying production SQL scripts.

---

## ADR-007: Referential Integrity for Explainable Fraud Rules (`ON DELETE RESTRICT`)
- **Status:** APPROVED
- **Context:** In financial audit compliance, historical fraud alert hit logs must explain exactly which rule triggered and why.
- **Decision:** Foreign key `fraud_alert_hits.rule_code -> fraud_rules.rule_code` enforces `ON DELETE RESTRICT ON UPDATE CASCADE`. Obsolete rules cannot be deleted if historical hits reference them; rules are disabled via `is_enabled = FALSE`.
- **Consequences:** Guarantees historical forensic integrity and explainability.

---

## ADR-008: Idempotent V6 Seeding Without Overwriting Runtime Tuning
- **Status:** APPROVED
- **Context:** Re-running migrations or running integration tests must be repeatable without duplicate key failures or wiping out administrator-tuned thresholds.
- **Decision:** Seed scripts use `INSERT ... ON DUPLICATE KEY UPDATE description = VALUES(description);`. Runtime-managed fields (`setting_value`, `points`, `is_enabled`, `parameters_json`) are omitted from the update clause.
- **Consequences:** 100% idempotent seeding that never destroys historical data or reverts admin customizations.

---

## ADR-009: Dedicated MySQL Integration Testing with Profile Isolation
- **Status:** APPROVED
- **Context:** Integration tests hardcoding `@ActiveProfiles("test")` bind to in-memory H2. Passing override parameters on the command line on Windows failed due to batch script parsing of `&` characters in URLs.
- **Decision:** Establish a dedicated test class `MySqlSchemaAndMigrationIntegrationTest` annotated with `@ActiveProfiles("mysql-test")` and `@EnabledIf("isMySqlConfigured")`. `application-mysql-test.yml` defines clean parameterless JDBC URL defaults (`jdbc:mysql://127.0.0.1:3306/trustpay_guardian_test`) and `clean-disabled: true`.
- **Consequences:** Provides genuine MySQL 8.4 engine verification against the isolated test database without modifying the H2 regression suite or risking Windows batch parsing issues.

---

## ADR-010: Database Privilege Hierarchy & True Immutability for Audit Logs
- **Status:** APPROVED
- **Context:** In MySQL, privilege checking is hierarchical (`mysql.user` -> `mysql.db` -> `mysql.tables_priv` -> `mysql.columns_priv`). A schema-wide grant (`GRANT ... ON db.*`) overrides table-level `REVOKE` statements. Merely revoking `UPDATE/DELETE` on `audit_logs` while `tpg_app` possesses schema-wide write grants does not enforce immutability at the database engine level.
- **Decision:**
  1. Primary Enforcement: Use table-level grants for `tpg_app` on mutable tables and restrict `audit_logs` to `SELECT, INSERT` only (no schema-wide `UPDATE/DELETE`).
  2. Storage Engine Defense-in-Depth: Implement `BEFORE UPDATE` and `BEFORE DELETE` triggers raising `SIGNAL SQLSTATE '45000'` on `audit_logs` to block mutations regardless of user grant escalation.
  3. Domain Layer: Enforce `@Column(updatable = false)` on `AuditLog` entity and omit update/delete methods in `AuditLogRepository`.
- **Consequences:** Guarantees regulatory compliance and tamper-evident audit logging at the storage engine tier.

---

## ADR-011: Production-Grade Reliability and Lifecycle Controls
- **Status:** APPROVED
- **Context:** Spring Boot production reliability requires deterministic shutdown, pooled resource limits, explicit timezones, and no lazy-loading session leaks.
- **Decision:**
  - `spring.jpa.open-in-view=false` to prevent connection leaks during rendering/serialization.
  - Graceful shutdown enabled with a 30s timeout (`server.shutdown=graceful`).
  - HikariCP pool parameters (`maximum-pool-size`, `minimum-idle`, `connection-timeout`, `idle-timeout`) externalized to environment variables.
  - Request size limits (`spring.servlet.multipart.max-file-size=5MB`) and timeouts.
  - Flyway `validate-on-migrate=true` and Hibernate `ddl-auto=validate`.
  - All times standardized to UTC across database, JDBC, JVM, and Jackson serialization.
- **Consequences:** Predictable concurrency, zero uncommitted transaction hold during serialization, and clean cloud container termination.

---

## ADR-012: In-Memory Frontend Token Management & Strict Cookie Transport
- **Status:** APPROVED
- **Context:** Storing JWT access tokens in `localStorage` or `sessionStorage` introduces XSS-vulnerable credential leakage.
- **Decision:**
  - JWT access tokens reside strictly in JavaScript memory (React Context / Axios closure).
  - Refresh tokens are transmitted exclusively via HttpOnly `SameSite=Strict` cookies with dynamic `Secure` flag.
  - Axios interceptor performs silent token refresh on HTTP 401 via `/api/v1/auth/refresh`.
- **Consequences:** Complete defense against token exfiltration via client-side script injection.

---

## ADR-013: Distributed Concurrency & Idempotency Invariance
- **Status:** APPROVED
- **Context:** Concurrent duplicate transfer submissions or network timeouts can cause double-debiting or duplicate transaction records.
- **Decision:**
  - Mandatory `Idempotency-Key` UUID header on `POST /transactions/transfer`.
  - Global `UNIQUE (idempotency_key)` database constraint on `transactions` table.
  - Pessimistic write locking on accounts in ascending ID order to eliminate database deadlocks.
  - Duplicate key violations (`DataIntegrityViolationException`) caught and mapped to idempotent cached replay.
- **Consequences:** Guarantee of zero overdrawing, zero race condition balance drift, and strict idempotency under concurrent load.

---

## ADR-014: Rate Limiting & Denial-of-Service Defense
- **Status:** APPROVED
- **Context:** Brute-force login attacks and transfer flooding must be mitigated at the application tier.
- **Decision:**
  - Implement Bucket4j token bucket rate limiting: 10 requests/minute per client IP for `/api/v1/auth/login`, 20 requests/minute per authenticated user for `/api/v1/transactions/transfer`.
  - Return HTTP 429 Too Many Requests with `Retry-After` header.
  - Document that the in-memory Bucket4j cache is per-instance, and a distributed store (e.g. Redis) is required for clustered multi-instance deployment.
- **Consequences:** Immediate protection against brute-force and request flooding; clear architectural path to distributed Redis token buckets.

---

## ADR-015: Observability, Metrics & Structured Logging
- **Status:** APPROVED
- **Context:** Production environments require distributed tracing correlation and metrics without exposing secrets or PII.
- **Decision:**
  - `RequestIdFilter` generates unique UUID for every incoming request, stores it in SLF4J MDC, and echoes it in the `X-Request-Id` response header.
  - Logback configured with MDC pattern in development, and structured JSON logging in production.
  - Secrets, passwords, full card/account numbers, and tokens are scrubbed from log output.
  - Actuator exposes `/actuator/health` (liveness and readiness groups) publicly; `/actuator/prometheus` is restricted to internal monitoring.
  - Custom Micrometer metrics: `trustpay.transfers.count` (by status), `trustpay.fraud.score` (distribution summary), `trustpay.fraud.alerts.open` (gauge), `trustpay.auth.failed_logins` (counter).
- **Consequences:** End-to-end request traceability and deep operational visibility into financial workflows.

---

## ADR-016: Code Quality Gates & Vulnerability Scanning
- **Status:** APPROVED
- **Context:** Maintaining portfolio and enterprise quality requires automated enforcement of test coverage, static analysis, and dependency security.
- **Decision:**
  - `jacoco-maven-plugin` configured with a strict build-failure gate: minimum 80% line coverage on `com.trustpay.guardian.service`, `com.trustpay.guardian.fraud`, and `com.trustpay.guardian.security` packages.
  - `sonar-project.properties` provided with pre-configured exclusions and quality profile mappings.
  - ESLint with zero warnings, TypeScript in strict mode, and `npm audit` / dependency scans with zero unresolved high/critical vulnerabilities.
- **Consequences:** Continuous regression prevention and deterministic quality compliance.

---

## ADR-017: Multi-Stage Secure Containerization
- **Status:** APPROVED
- **Context:** Production containers must minimize surface attack area and enforce least privilege runtime security.
- **Decision:**
  - Multi-stage Docker builds for both backend (Maven builder -> Eclipse Temurin 25 JRE slim runtime) and frontend (Node builder -> Nginx Alpine runtime).
  - Pinned non-latest base image tags.
  - Application processes execute under a dedicated non-root user (`appuser` with UID 10001).
  - Native container healthcheck definitions (`HEALTHCHECK`).
  - Production compose file (`docker-compose.prod.yml`) isolates the database on an internal network without publishing port 3306.
- **Consequences:** Minimal container image footprint, hardened container runtime, and compliance with container security best practices.

---

## ADR-018: Explainable Strategy Pattern Fraud Engine
- **Status:** APPROVED
- **Context:** Regulatory review requires each fraud detection rule to be deterministic, isolated, testable, and explainable to compliance officers and analysts.
- **Decision:**
  - Implement each of the seven rules (R1 to R7) as an isolated Spring component implementing `FraudRule` strategy interface (`code()`, `evaluate(TransactionContext)`).
  - Parameters and points dynamically resolved via `FraudRuleConfigService` with in-memory cache evicted upon admin updates.
  - Alert resolution strictly restricted to `FRAUD_ANALYST` role; `ADMIN` granted read-only visibility.
- **Consequences:** Zero black-box obscurity, granular rule unit testability at threshold boundaries, and complete compliance auditability.


