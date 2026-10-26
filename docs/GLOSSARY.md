# TrustPay Guardian — Banking, Fraud & Security Engineering Glossary

**Document Version:** 1.0.0  
**Classification:** Reference Lexicon  
**Last Updated:** 2026-10-26  

---

## 1. Banking & Financial Engineering Terms

- **Available Balance:** The funds immediately spendable by an account holder. Calculated dynamically as `Current Balance − SUM(Pending Holds)`.
- **Current Balance (Ledger Balance):** The total settled funds posted to the account ledger, excluding unreleased holds.
- **Idempotency:** A property of an API operation where making multiple identical requests has the same effect as making a single request. Enforced via client-generated UUID `Idempotency-Key` headers.
- **Pessimistic Row Locking (`SELECT ... FOR UPDATE`):** An exclusive database lock acquired on specific account rows during a transaction to prevent concurrent modifications until commit.
- **Deadlock:** A state where two or more concurrent database transactions are unable to proceed because each holds a lock that the other needs (circular wait).
- **Ascending-ID Lock Acquisition:** An algorithmic technique that sorts account identifiers before acquiring locks, mathematically eliminating circular wait conditions and deadlocks.
- **Pending Hold (Reservation):** Funds earmarked for a transaction awaiting fraud review or clearing. The money cannot be spent by the customer, but has not yet been transferred to the recipient.
- **Settlement:** The final, irreversible posting and crediting of funds between sender and receiver accounts.

---

## 2. Fraud Detection & Heuristic Scoring Terms

- **Heuristic Rule:** A deterministic rule-based condition evaluating transaction parameters (e.g. transfer amount, velocity) to assess risk without machine learning opacity.
- **Risk Score:** A computed integer ranging strictly from `0` (zero risk) to `100` (extreme risk) representing cumulative fraud suspicion.
- **Score Clamping:** A defensive mathematical operation (`Math.min(100, Math.max(0, score))`) ensuring scores never breach expected bounds.
- **Triage Queue:** An operational dashboard listing open fraud alerts sorted primarily by severity (`HIGH` -> `MEDIUM` -> `LOW`) and secondarily by age.
- **Forensic Dossier:** A comprehensive investigative view displaying the transaction context, customer profile, risk score gauge, and individual rule-hit breakdown.
- **False-Positive Rate:** The proportion of legitimate customer transactions flagged as suspicious compared to total resolved alerts.
- **Velocity Window:** A rolling time interval (e.g. 5 minutes) during which the frequency of transactions from an account is measured against a threshold.
- **Dormancy Awakening:** A fraud heuristic that triggers when an account with zero transaction activity for an extended period (e.g. >60 days) suddenly initiates a high-value transfer.
- **Segregation of Duties:** An internal control ensuring that administrators cannot override or execute case decisions reserved exclusively for fraud analysts.

---

## 3. Security, Cryptography & Auth Terms

- **In-Memory JWT Access Token:** An access token stored exclusively in JavaScript runtime closure memory, completely isolated from `localStorage` to defeat XSS token theft.
- **HttpOnly Cookie:** A cookie with the `HttpOnly` flag set, preventing client-side scripts from reading its contents. Used for 7-day refresh tokens.
- **SameSite=Strict:** A cookie attribute instructing the browser never to send the cookie in cross-site requests, providing robust protection against CSRF.
- **Token Bucket Rate Limiting:** An algorithm where tokens are refilled at a constant rate into a bucket of fixed capacity. Each request consumes one token; if empty, the request is rejected with `HTTP 429`.
- **IDOR (Insecure Direct Object Reference):** A vulnerability where an attacker accesses unauthorized data by altering parameter values referencing database keys. Mitigated by returning `HTTP 404` on foreign records.
- **BCrypt:** An adaptive, salt-embedded password hashing function based on the Blowfish cipher. Work factor `12` is enforced in TrustPay Guardian.
- **MDC (Mapped Diagnostic Context):** A logging pattern that attaches contextual metadata (such as `requestId` and `userId`) to every log statement within the execution thread.
- **STRIDE:** A threat modeling framework classifying security risks into Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, and Elevation of Privilege.

---

## 4. Software Architecture & DevOps Terms

- **Flyway:** An open-source database migration tool that manages forward-only SQL schema versioning and checksum verification.
- **JaCoCo:** Java Code Coverage library used during Maven builds to measure line, branch, and instruction coverage against quality gates (>= 80%).
- **Multi-Stage Dockerfile:** A Docker build pattern using separate build and runtime containers to produce minimal, hardened production images without compilers or source code.
- **RFC 7807 (Problem Details):** An IETF standard defining a consistent JSON structure for reporting errors from HTTP APIs.
