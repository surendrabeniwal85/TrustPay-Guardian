# TrustPay Guardian — Comprehensive System Audit (Step A)

**Document:** System Audit and Gap Analysis  
**Project:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Environment:** Java 25 LTS, Spring Boot 4.1.x, MySQL 8.4/9.7 LTS, React 19 + TypeScript + Vite  
**Audit Execution Date:** 2026-10-26  
**Auditor:** Principal Java Full Stack Engineer  

---

## 1. Executive Summary of Audit Findings

An exhaustive, non-destructive audit was executed across the entire repository, testing the real toolchain, source code, database migration scripts, configuration, and frontend assets.

- **Backend Verification:** `.\mvnw.cmd clean verify` succeeded in 30.024s with 39 tests passed and 5 skipped (MySQL live server tests skipped due to unconfigured test credentials).
- **Frontend Verification:** `npm run build` (`tsc -b && vite build`) succeeded in 2.84s.
- **Docker Toolchain:** Docker CLI / Podman is not installed on the Windows host machine. Container commands are marked **UNVERIFIED** with instructions for external execution.
- **Native MySQL:** MySQL Server 8.4.11 is running locally on port 3306, but root access requires credentials. H2 in-memory mode is used for reproducible CI/CD unit and slice tests.

Code exists in working trees for early phases, but **zero phases meet the Definition of Done or pass all Phase Prompt checks**. Key security flaws (e.g. token in `localStorage`, broken access control on alert resolution, hardcoded seed passwords) and architectural gaps must be resolved strictly in phase order.

---

## 2. Critical Security Vulnerabilities

### SEC-01: Insecure Token Persistence in `localStorage` (XSS Exposure)
- **Location:** `frontend/src/services/api.ts` (lines 14, 27, 28)
- **Violation:** PRD sections 1.5, 4.1, and 8 mandate that access tokens must remain in memory only, and refresh tokens are transmitted via HttpOnly `SameSite=Strict` cookies. Tokens must never reside in `localStorage` or `sessionStorage`.
- **Remediation:** Remove token retrieval and storage from `localStorage`. Maintain the JWT access token in an in-memory variable (or React context closure). Configure Axios response interceptor for silent token renewal via `/api/v1/auth/refresh`.

### SEC-02: Broken Access Control on Fraud Alert Resolution
- **Location:** `backend/src/main/java/com/trustpay/guardian/fraud/FraudAnalystController.java` (line 20)
- **Violation:** `@PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")` is applied across the entire controller, including `POST /alerts/{id}/decision`. PRD sections 3.3, 3.4, and 5.6 strictly mandate that alert resolution is restricted to `FRAUD_ANALYST` only, while `ADMIN` has read-only visibility.
- **Remediation:** Restrict `resolveAlert` to `hasRole('FRAUD_ANALYST')`. Keep GET endpoints open to both `FRAUD_ANALYST` and `ADMIN`.

### SEC-03: Permissive Cookie Configuration (CSRF Risk)
- **Location:** `backend/src/main/java/com/trustpay/guardian/auth/AuthController.java` (lines 109, 112, 121, 124)
- **Violation:** Refresh token cookie sets `sameSite("Lax")` and hardcodes `secure(false)`. PRD section 4.1 mandates `SameSite=Strict` and property-driven `Secure` (true in prod).
- **Remediation:** Bind `Secure` flag to `app.security.cookie.secure` configuration property and set `SameSite("Strict")`.

### SEC-04: Hardcoded Seed Credentials in Java Source
- **Location:** `backend/src/main/java/com/trustpay/guardian/config/DataInitializer.java` (lines 64, 74, 85, etc.)
- **Violation:** Passwords `Admin@123`, `Analyst@123`, `Customer@123` are hardcoded in Java source. PRD and Prompt Guide rule: "Seed passwords come from environment variables (`DEV_SEED_PASSWORD`)."
- **Remediation:** Inject `@Value("${app.seed.password:${DEV_SEED_PASSWORD:DemoPass123!}}")` or require explicit environment variable in dev profile.

---

## 3. Deviations from the PRD & Functional Gaps

### Backend Foundation & Common
1. **RFC 7807 ProblemDetail:** `GlobalExceptionHandler` returns a custom `ErrorResponse` instead of RFC 7807 `ProblemDetail` enriched with `fieldErrors` and `requestId`.
2. **Missing Request ID Propagation:** No `RequestIdFilter` exists to inject `requestId` into Logback MDC and write `X-Request-Id` response header.
3. **Missing Core Exceptions:** Missing canonical `NotFoundException`, `ConflictException`, and `BusinessRuleException` in `com.trustpay.guardian.common.exception`.
4. **Missing Common Utilities:** `MaskingUtils` (masking account number as `TPG******1234` and email as `j***e@example.com`) and `MoneyUtils` are missing.
5. **Missing PageResponse wrapper:** Pagination endpoints return raw Spring Data `Page<T>` rather than standard `PageResponse<T>` record.
6. **Missing Configuration Properties:** No centralized `@ConfigurationProperties` class `AppProperties` exists for jwt, cors, cookie, and limits.
7. **Missing OpenAPI & Swagger:** `springdoc-openapi-starter-webmvc-ui` is missing from `pom.xml`, and no `OpenApiConfig` is present.
8. **Missing Logback Configuration:** No `logback-spring.xml` configuring structured console logging with MDC `requestId` in dev and JSON in prod.

### Accounts & Beneficiaries (Phase 4)
1. **Account Number Generation:** `AccountNumberGenerator` (generating `TPG` + 10 digits via `SecureRandom` with collision retry) is missing.
2. **Account Limit Enforcement:** Limit of maximum 3 accounts per customer (4th returning HTTP 422) is not implemented.
3. **Account Statements:** Endpoint `GET /api/v1/accounts/{id}/statement` with date range and pagination is missing.
4. **Beneficiary Masked Lookup:** `GET /api/v1/beneficiaries/lookup?accountNumber=...` returning only masked holder name is missing.
5. **Beneficiary Deletion:** `DELETE /api/v1/beneficiaries/{id}` is missing.
6. **Data Transfer Objects:** DTOs are defined as mutable classes instead of Java records.

### Transactions & Money Movement (Phase 5)
1. **Idempotency-Key Header Optional:** `TransferController` accepts `Idempotency-Key` as optional rather than mandatory UUID header.
2. **Deposit Endpoint Route:** Deposit is currently exposed at `/api/v1/accounts/deposit` instead of PRD-mandated `POST /api/v1/transactions/deposit`.
3. **Transaction History Multi-Criteria Filter:** `GET /api/v1/transactions` lacks multi-filter specification (type, status, date range, min/max amount, reference search).
4. **Transaction Reference Lookup:** `GET /api/v1/transactions/{reference}` (owner-only) is missing.
5. **Missing 20-Thread Concurrency Test:** Concurrency test proving zero overdrawing and system money invariance under parallel load is missing.

### Fraud Engine & Analyst Workflow (Phase 6)
1. **Fraud Rule Caching:** In-memory caching with eviction on admin rule update (`FraudRuleConfigService`) is missing.
2. **Fraud Stats Endpoint:** `GET /api/v1/fraud/stats` is missing.
3. **Rule Boundary Tests:** Boundary tests for all 7 rules (R1 to R7 at, below, above threshold) are incomplete.
4. **PRD Section 16 Demo Script Test:** End-to-end integration test verifying the exact PRD demo flow is missing.

### Admin & Notifications (Phase 7)
1. **Account Freeze/Unfreeze:** Admin endpoints to freeze and unfreeze accounts with notifications are missing.
2. **Admin All Transactions:** Admin query endpoint for all platform transactions is missing.
3. **Admin User Creation:** Admin endpoint to create analysts or administrators is missing.
4. **Public Statistics:** `GET /api/v1/public/stats` providing aggregate, non-sensitive counts is missing.
5. **Notification Mark All Read:** Endpoint to mark all notifications read is missing.
6. **MailHog Email Delivery:** Email notification integration for held/blocked transfers is missing.
7. **Role Matrix Security Test:** Comprehensive 4-identity parameterised MockMvc test across all platform routes is missing.

---

## 4. Quality, Tooling & DevOps Gaps

1. **JaCoCo Quality Gate (Phase 8):** `jacoco-maven-plugin` is missing from `pom.xml`. No 80% coverage quality gate exists.
2. **SonarQube Properties:** `sonar-project.properties` is missing.
3. **Frontend Packages & Scripts (Phase 0 & 9):**
   - Missing scripts in `package.json`: `lint`, `typecheck`, `test`, `test:coverage`, `e2e`.
   - Missing runtime/dev dependencies: `react-hook-form`, `zod`, `@hookform/resolvers`, `@reduxjs/toolkit`, `react-redux`, `recharts`, `vitest`, `@testing-library/react`.
   - `react-router-dom@6.28.0` is installed; PRD/Prompt Guide targets modern routing patterns.
4. **Docker Containerization (Phase 14):**
   - Missing `backend/Dockerfile`, `frontend/Dockerfile`, `nginx.conf`, `.dockerignore`.
   - `compose.yaml` lacks backend, frontend, and MailHog definitions.
   - `docker-compose.prod.yml` is missing.
5. **CI/CD Pipelines:**
   - `Jenkinsfile` is missing.
   - GitHub Actions workflow `.github/workflows/ci.yml` is missing frontend test/lint steps and dependency scans.
6. **Documentation & Release Artifacts:**
   - 14 comprehensive documents specified in Phase 16 are missing.
   - `docs/RUNBOOK.md`, `docs/THREAT_MODEL.md`, `docs/SECURITY_REVIEW.md`, and `docs/LOAD_TEST.md` are missing.
   - Git release tags `v0.1.0` and `v1.0.0` have not been established.

---

## 5. Dead Code & Defective Scripts

1. **`scripts/verify-mysql.ps1` Parser Errors:**
   - Syntax errors in PowerShell due to unescaped quotes and redirect symbols (`>`) on lines 74, 75, and 100.
2. **`compose.yaml`:**
   - Mentions MySQL 8.4.11; PRD target is MySQL 9.7 LTS or 8.4 LTS, with MailHog missing.

---

## 6. Execution Roadmap & Strategy

All work will proceed strictly phase by phase (Phase 0 to Phase 16) on dedicated `feature/phase-XX-name` branches created from `develop`. No code will be deleted unless it violates security, correctness, or the PRD.
Every phase will execute all verification checks, record evidence in `docs/verification/phase-XX.md`, update `docs/PROGRESS.md`, and merge into `develop` with `--no-ff`.
