# TrustPay Guardian — Comprehensive STRIDE Threat Model

**Document Version:** 1.0.0  
**Classification:** Security Architecture & Threat Risk Assessment  
**Methodology:** Microsoft STRIDE Threat Modeling Framework  
**Target Architecture:** Java 25 / Spring Boot 4.1.1 + React 19 / TypeScript + MySQL 9.7 LTS  
**Last Updated:** 2026-10-26  

---

## 1. Executive Summary & Asset Classification

TrustPay Guardian is a digital banking platform simulation designed with defense-in-depth security. The primary assets requiring protection include:
1. **User Identity & Credentials:** Passwords, refresh tokens, and JWT claims.
2. **Financial Ledger & Balances:** Account records, transaction ledger, and pending holds.
3. **Fraud Engine Heuristics & Rules:** Scoring weights, decision thresholds, and investigation queues.
4. **Audit Trail & Governance Logs:** Append-only event history capturing administrative and security actions.

---

## 2. STRIDE Threat Analysis Matrix

### 2.1 Spoofing (Identity Spoofing & Impersonation)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-SP-01** | **JWT Token Forgery / HMAC Tampering**<br>Attacker attempts to forge JWT signatures or substitute `none` algorithm. | **HIGH** | `JwtService` enforces HMAC-SHA512 with strict secret validation; refuses startup if secret length < 64 chars; validates `exp`, `iat`, and `iss`. | `JwtServiceTest` (5/5 tests passed). |
| **T-SP-02** | **Refresh Token Theft & Replay**<br>Attacker intercepts refresh token cookie to generate new access tokens indefinitely. | **HIGH** | Refresh tokens stored as SHA-256 hashes in database; rotated on every use; reuse detection revokes all active tokens for that user immediately. | `AuthServiceTest` & `AuthServiceIntegrationTest`. |
| **T-SP-03** | **Credential Stuffing / Brute Force**<br>Automated bot attacks attempting password guessing. | **HIGH** | BCrypt strength 12; Bucket4j IP rate limiter (10 attempts/min/IP); automatic 15-minute account lockout after 5 consecutive failures. | `AuthServiceIntegrationTest#testAccountLockedAfter5Failures`. |
| **T-SP-04** | **Sender Account Impersonation**<br>Customer attempts to initiate transfers from an account they do not own. | **CRITICAL** | `TransferService` enforces strict ownership check: `sourceAccount.getUser().getId().equals(currentUser.getId())`; returns 404 IDOR guard. | `AccountAndBeneficiaryMockMvcTest`. |

---

### 2.2 Tampering (Data Modification & Integrity Attacks)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-TA-01** | **Double-Debit / Race Condition Exploitation**<br>Attacker fires concurrent transfer requests to spend the same balance twice. | **CRITICAL** | Deterministic ascending-ID pessimistic row locking (`SELECT ... FOR UPDATE`); pre-lock balance checks; mandatory unique `Idempotency-Key` header. | `TransferConcurrencyIntegrationTest` (20 parallel threads, 0 overdraws). |
| **T-TA-02** | **Negative Balance Ledger Corruption**<br>Attacker injects negative transfer amounts to artificially credit their own balance. | **HIGH** | Multi-layer defense: Bean validation (`@DecimalMin("1.00")`), service-layer scale validation (`scale <= 2`), and MySQL engine `CHECK (amount > 0)`. | `EntityAndRepositoryIntegrationTest` & `chk_tx_amount_positive`. |
| **T-TA-03** | **Audit Trail Modification**<br>Malicious insider or attacker attempts to update or delete audit records to hide fraud. | **HIGH** | `audit_logs` table has no `UPDATE` or `DELETE` API endpoints; DDL grants restrict modifications; `AuditService` is strictly append-only. | `AdminServiceIntegrationTest`, `scripts/harden-audit-grants.sql`. |
| **T-TA-04** | **Fraud Rule Score Manipulation**<br>Attacker submits values outside valid 0–100 risk score bands. | **MEDIUM** | Clamped bounds in `FraudEngine`: `Math.max(0, Math.min(100, score))`; database constraint `CHECK (risk_score BETWEEN 0 AND 100)`. | `FraudRuleBoundaryUnitTest` (22/22 passed). |

---

### 2.3 Repudiation (Disavowing Actions)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-RE-01** | **Transfer Repudiation**<br>Customer claims a transfer was executed without their consent or knowledge. | **MEDIUM** | Generated cryptographically secure transaction reference (`TXN-` + 12 chars); recorded client IP, timestamp, and immutable ledger record. | `TransactionControllerMockMvcTest`. |
| **T-RE-02** | **Analyst Decision Repudiation**<br>Analyst releases a fraudulent transaction and denies making the decision. | **HIGH** | Mandatory investigation decision notes required; analyst user ID, decision timestamp, and rationale recorded in both `fraud_alerts` and `audit_logs`. | `FraudWorkflowAndRoleSecurityIntegrationTest`. |

---

### 2.4 Information Disclosure (Privacy & PII Leakage)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-ID-01** | **Insecure Direct Object Reference (IDOR)**<br>Manipulating account ID or transaction reference to view another user's financial data. | **CRITICAL** | All queries enforce user ownership filters; cross-user requests return HTTP 404 (Not Found) rather than 403 to prevent object existence enumeration. | `AccountServiceTest`, `BeneficiaryServiceTest`. |
| **T-ID-02** | **Token Theft via Cross-Site Scripting (XSS)**<br>Malicious script attempts to extract JWT from browser storage. | **HIGH** | Access token stored strictly in JavaScript runtime memory (`axiosClient.ts`); never written to `localStorage` or `sessionStorage`. | `axiosClient.test.ts` (2/2 passed). |
| **T-ID-03** | **Sensitive PII Logging**<br>Passwords, raw tokens, or full account numbers leaked in server logs. | **MEDIUM** | `MaskingUtils` masks account numbers (`TPG******1234`) and customer names (`J*** D**`); passwords omitted from DTO serialization and log statements. | `MaskingUtilsTest` (7/7 passed). |
| **T-ID-04** | **Detailed Stack Trace Leakage in Error Responses**<br>Server exceptions exposing internal database schema or file paths. | **MEDIUM** | RFC 7807 `ProblemDetail` handles all exceptions in `GlobalExceptionHandler`; stack traces suppressed in non-dev profiles; unique `requestId` returned for tracing. | `GlobalExceptionHandlerMockMvcTest` (6/6 passed). |

---

### 2.5 Denial of Service (Resource Starvation)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-DS-01** | **API Flooding & Transfer Exhaustion**<br>Attacker floods `/transfer` endpoint to overwhelm database connection pool. | **HIGH** | Bucket4j token bucket rate limiting (20 transfer requests/min/user); connection pool limits (`maximum-pool-size: 10`); transaction timeouts. | `RateLimitFilter`, `TransferConcurrencyIntegrationTest`. |
| **T-DS-02** | **HikariCP Connection Pool Starvation**<br>Unbounded queries holding connections indefinitely. | **HIGH** | Query pagination enforced (`Pageable`); N+1 queries eliminated via `JOIN FETCH`; connection timeout set to 30,000ms. | `docs/verification/phase-08.md`. |

---

### 2.6 Elevation of Privilege (Access Control Breaches)

| Threat ID | Threat Description | Severity | Countermeasures Implemented in TrustPay Guardian | Verification & Evidence |
|---|---|:---:|---|---|
| **T-EP-01** | **Customer Accessing Staff / Admin Consoles**<br>Customer attempts to call `/api/v1/fraud/*` or `/api/v1/admin/*`. | **CRITICAL** | Spring Security `@PreAuthorize` and method security enforce `hasRole('ADMIN')` and `hasRole('FRAUD_ANALYST')`; returns HTTP 403 Forbidden. | `RoleMatrixSecurityMockMvcTest` (80/80 passed). |
| **T-EP-02** | **Admin Overriding Analyst Segregation of Duties**<br>Admin attempts to release or block fraud cases directly. | **HIGH** | Alert resolution endpoints (`/decision`, `/release`, `/confirm-block`) strictly restricted to `hasRole('FRAUD_ANALYST')`; Admin is strictly read-only. | `FraudWorkflowAndRoleSecurityIntegrationTest`. |
| **T-EP-03** | **Self-Demotion / Lockout Attacks on Admins**<br>Admin accidentally demotes or suspends their own account. | **MEDIUM** | Service guard rejects self-role modification and self-suspension on current authenticated user. | `AdminWorkflowAndSecurityIntegrationTest`. |

---

## 3. Residual Risk & Ongoing Assurance
- **Simulated Scope:** As an educational simulation platform, TrustPay Guardian uses synthetic demo credentials. In production deployment, external Identity Providers (OAuth2/OIDC, MFA/WebAuthn) and hardware security modules (HSMs) would supersede simple username/password flows.
- **Continuous Security Testing:** Automated via `.github/workflows/ci.yml` and `Jenkinsfile` on every push.
