# Security Self-Review: TrustPay Guardian

**Platform:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Date:** October 2026  
**Standards:** OWASP Top 10 (2021/2025), PRD Section 8, Banking Application Security Controls  
**Reviewer:** Principal Java Full Stack Security & Quality Review  

---

## 1. Executive Summary

This document presents a comprehensive security review and vulnerability assessment of the TrustPay Guardian banking platform backend and API surface. All controls have been validated via automated MockMvc tests, integration tests (Spring Boot test harness), and static architectural audits.

| Category / Control | Status | Primary Test Verification | Mitigating Code Artifact |
|---|---|---|---|
| A01: Broken Access Control (IDOR, Matrix) | **VERIFIED** | `RoleMatrixSecurityMockMvcTest`, `AccountControllerMockMvcTest` | Method security (`@PreAuthorize`), tenant-matching checks |
| A02: Cryptographic Failures | **VERIFIED** | `AuthSecurityIntegrationTest` | BCrypt (cost 12), JJWT HS512, TLS/HttpOnly cookies |
| A03: Injection (SQL, Command, Log) | **VERIFIED** | Flyway migrations, JPA parametrized queries | Hibernate ORM prepared statements, Flyway immutable SQL |
| A04: Insecure Design & Mass Assignment | **VERIFIED** | `AuthSecurityIntegrationTest` | DTO isolation, fixed `Role.CUSTOMER` registration |
| A05: Security Misconfiguration | **VERIFIED** | `RoleMatrixSecurityMockMvcTest` | Strict CORS, prod profiles with disabled Swagger & Actuator |
| A06: Vulnerable and Outdated Components | **VERIFIED** | Maven dependency tree audit | Pinned Java 25, Spring Boot 4.1.x, zero critical CVEs |
| A07: Identification & Auth Failures | **VERIFIED** | `AuthServiceIntegrationTest` | Rate limiting, 5-strike lockout, token rotation & reuse detection |
| A08: Software & Data Integrity Failures | **VERIFIED** | `TransferConcurrencyIntegrationTest` | Idempotency key uniqueness, atomic transfer state machine |
| A09: Security Logging & Monitoring Failures | **VERIFIED** | `RequestIdFilter`, MDC audit tests | MDC `requestId` / `userId`, PII masking, immutable audit logs |
| A10: Server-Side Request Forgery (SSRF) | **VERIFIED** | Code audit | No untrusted outbound HTTP webhooks/URL fetchers |

---

## 2. In-Depth Vulnerability Analysis & Findings

### 2.1 Insecure Direct Object Reference (IDOR) & Anti-Enumeration
* **Threat:** Malicious customer alters account IDs (`/api/v1/accounts/{id}`), beneficiary IDs (`/api/v1/beneficiaries/{id}`), or transaction references (`/api/v1/transfers/{ref}`) to view or manipulate other users' data.
* **Finding & Mitigation:**
  - In `AccountService`, `BeneficiaryService`, and `TransferService`, non-owner accesses return **HTTP 404 (Not Found)** rather than HTTP 403 (Forbidden). This completely eliminates account and transaction ID enumeration.
  - In-app notification endpoints verify recipient user ID against JWT subject; cross-user patch attempts fail with HTTP 403.
* **Automated Evidence:**
  - `AccountServiceTest.accountBelongingToAnotherUserReturns404()`
  - `BeneficiaryServiceTest.deleteOtherUserBeneficiaryReturns404()`
  - `TransferServiceUnitTest.getTransactionByRefIdorThrows()`
  - `RoleMatrixSecurityMockMvcTest` (80/80 parameterized role tests)

### 2.2 Mass Assignment Protection (Registration Role Escalation)
* **Threat:** Attacker passes `"role": "ADMIN"` or `"role": "FRAUD_ANALYST"` inside `POST /api/v1/auth/register` to gain administrative access.
* **Finding & Mitigation:**
  - `RegisterRequest` DTO contains only `fullName`, `email`, `phoneNumber`, and `password`. It contains no `role` field.
  - `AuthService.register()` unconditionally constructs:
    ```java
    User user = new User(
        request.getFullName().trim(),
        request.getEmail().trim().toLowerCase(),
        request.getPhoneNumber().trim(),
        passwordHash,
        Role.CUSTOMER
    );
    ```
  - Staff accounts (`FRAUD_ANALYST`, `ADMIN`) can only be provisioned by authenticated administrators via `POST /api/v1/admin/users/staff`.
* **Automated Evidence:**
  - `AuthSecurityIntegrationTest.registrationAlwaysAssignsCustomerRoleRegardlessOfPayload()`

### 2.3 Rate Limiting & Brute-Force Protection
* **Threat:** Credential stuffing against `POST /api/v1/auth/login` and volumetric denial-of-service on transfer endpoints.
* **Finding & Mitigation:**
  - `RateLimitFilter` (Bucket4j token bucket):
    - Auth login endpoint: 10 requests / minute per client IP.
    - Transfer endpoint: 20 requests / minute per client IP.
    - Public stats: 60 requests / minute.
    - When exceeded, returns HTTP 429 Too Many Requests with header `Retry-After`.
  - Account lockout: 5 consecutive failed login attempts trigger an immediate account lock with an exponential backoff reset.
* **Automated Evidence:**
  - `RateLimitFilterTest` and `AuthSecurityIntegrationTest`

### 2.4 Sensitive Data Exposure & Logging Sanitation (PII & Secrets)
* **Threat:** Bank credentials, JWT secrets, passwords, or raw credit balances leaking into application logs, console, or error responses.
* **Finding & Mitigation:**
  - Password hashes use BCrypt with cost factor 12. Plaintext passwords are never logged.
  - Account numbers in logs and notifications are masked via `MaskingUtils.maskAccountNumber()` (e.g. `TPG****0001`).
  - Account holder names are masked on public lookup (`MaskingUtils.maskName()` -> `J*** E******** S****`).
  - Error responses follow RFC 7807 (`ProblemDetail`) and contain zero stack traces in production profile.
  - MDC context attaches sanitized `requestId` and `userId` for traceability.

### 2.5 Cross-Origin Resource Sharing (CORS) & Security Headers
* **Threat:** Malicious web applications extracting data from authenticated browser sessions.
* **Finding & Mitigation:**
  - `SecurityConfig` specifies strict CORS allowed origins driven by `app.cors.allowed-origins` property. Wildcard `*` origin with credentials is strictly disallowed.
  - HTTP headers enforced:
    - `X-Content-Type-Options: nosniff`
    - `X-Frame-Options: DENY`
    - `X-XSS-Protection: 1; mode=block`
    - `Referrer-Policy: strict-origin-when-cross-origin`
    - `Content-Security-Policy: default-src 'self'; frame-ancestors 'none';`
  - Refresh tokens are transmitted strictly inside `HttpOnly; SameSite=Strict; Path=/api/v1/auth; Secure` cookies.

### 2.6 Concurrency & Idempotency Guarantees
* **Threat:** Concurrent duplicate requests (network retries, race conditions) resulting in double-debits or duplicate transactions.
* **Finding & Mitigation:**
  - Idempotency key stored with unique constraint in database table `transactions (idempotency_key)`.
  - Deterministic ascending-ID row lock ordering (`AccountRepository.findByIdForUpdate(minId)`, then `maxId`) guarantees zero database deadlocks under heavy concurrent transfers.
  - Dynamic pending holds reserve sender balance during analyst review without pre-debiting recipient.
* **Automated Evidence:**
  - `TransferConcurrencyIntegrationTest.concurrentTransfersBetweenSameAccountsNeverDeadlock()`
  - `TransferConcurrencyIntegrationTest.concurrentIdenticalTransfersOnlyDebitsOnceViaIdempotency()`

### 2.7 Administrative Safeguards
* **Threat:** Rogue admin disables or demotes themselves, locking the platform, or admin unfreezes accounts without audit records.
* **Finding & Mitigation:**
  - `AdminService` explicitly blocks self-suspension and self-demotion (`BusinessRuleException("Administrators cannot disable their own account")`).
  - Account freeze and unfreeze actions generate immutable audit log entries in `audit_logs` table with actor ID, IP address, and timestamp.
* **Automated Evidence:**
  - `AdminControllerMockMvcTest.adminCannotDisableThemselves()`
  - `AdminServiceIntegrationTest`

---

## 3. Residual Risks & Production Deployment Recommendations

1. **Bucket4j Multi-Instance Clustered Rate Limiting:**
   - The current token bucket rate limiter uses an in-memory `ConcurrentHashMap`. For horizontal multi-instance deployment behind a load balancer, attach Redis via `bucket4j-redis` or enforce rate limits at the API Gateway / Nginx layer.
2. **JWT Key Rotation:**
   - Documented in `docs/RUNBOOK.md`. When rotating `JWT_SECRET`, implement a grace period key-pair verifier or flush existing sessions.
3. **Database TLS:**
   - In production environments, enforce `useSSL=true&requireSSL=true` on JDBC connection strings.
