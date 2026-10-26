# TrustPay Guardian — Technical Learning & Interview Viva Guide (35+ Q&As)

**Target Audience:** Software Engineering Candidates, Lead Architects, Technical Evaluators  
**Scope:** Java 25, Spring Boot 4.1, MySQL Concurrency, React 19, Security & Fraud Systems  
**Last Updated:** 2026-10-26  

---

## Category 1: Core Java 25 & Software Engineering Fundamentals

### Q1: Why is `BigDecimal` mandatory for financial amounts, and why is `double` forbidden?
**Answer:** `float` and `double` are IEEE 754 binary floating-point representations that cannot accurately represent decimal fractions like `0.1` or `0.05`. Over multiple operations, accumulated rounding errors cause phantom money creation or destruction. `BigDecimal` represents decimal numbers with arbitrary precision and exact scale, making it the only compliant choice for financial arithmetic.

### Q2: Why must you instantiate `BigDecimal` using strings (e.g., `new BigDecimal("10.50")`) rather than numbers (e.g., `new BigDecimal(10.50)`)?
**Answer:** Passing a `double` literal like `10.50` into the `BigDecimal` constructor immediately passes a value that is already imprecise in binary representation (`10.500000000000000444...`). Using String constructors or `BigDecimal.valueOf(10.50)` preserves exact decimal precision.

### Q3: How are Java 25 Records used in TrustPay Guardian?
**Answer:** Records (e.g., `TransactionContext`, `RuleResult`, `PageResponse<T>`) serve as immutable Data Transfer Objects (DTOs). They eliminate boilerplate getter/setter code, guarantee thread-safety via shallow immutability, and implement transparent `equals()`, `hashCode()`, and `toString()`.

### Q4: What is the difference between shallow immutability in Records and deep immutability?
**Answer:** A record's component fields are implicitly `final`. However, if a record contains a reference to a mutable object (like a `List`), the reference cannot be reassigned, but the underlying list could still be modified unless wrapped with `List.copyOf()`.

### Q5: How does the Strategy Pattern improve upon monolithic `if-else` fraud detection?
**Answer:** A monolithic `if-else` method violates the Open/Closed Principle (OCP) and becomes brittle as rules grow. With the Strategy Pattern, each rule implements `FraudRule` with isolated evaluation logic. Adding, disabling, or tuning a rule requires zero changes to the core `FraudEngine` or other rules, and enables granular unit testing of boundary conditions.

---

## Category 2: MySQL Concurrency, Database Design & Flyway

### Q6: How does TrustPay Guardian prevent deadlocks during concurrent bidirectional transfers?
**Answer:** Deadlocks occur when Thread A locks Account 1 and waits for Account 2, while Thread B locks Account 2 and waits for Account 1 (circular wait). TrustPay Guardian eliminates the circular wait condition by always sorting account IDs in ascending order:
`Long first = Math.min(id1, id2); Long second = Math.max(id1, id2);`
Both threads acquire locks in the exact same numerical sequence.

### Q7: What is the difference between Pessimistic and Optimistic Locking, and where is each used?
**Answer:** 
- **Pessimistic Locking (`SELECT ... FOR UPDATE`):** Blocks other transactions at the database level until the current transaction commits. Used in `TransferService` for money transfers where balance integrity is mission-critical.
- **Optimistic Locking (`@Version`):** Checks version number on update without holding database locks. Used on `Account` for non-transfer administrative updates (like status changes or address updates).

### Q8: Why does TrustPay Guardian calculate "Available Balance" dynamically rather than deducting holds immediately?
**Answer:** If a transfer is flagged for fraud review, deducting the ledger balance prematurely creates accounting drift before approval. If funds are not reserved, the customer could double-spend their balance. Calculating `Available Balance = Current Balance − SUM(Pending Holds)` dynamically reserves the money without creating phantom ledger debits.

### Q9: Why is standard SQL `CURRENT_TIMESTAMP(6)` used instead of MySQL's `UTC_TIMESTAMP(6)`?
**Answer:** `UTC_TIMESTAMP(6)` is a proprietary MySQL extension that breaks in H2 in-memory test databases. Using ANSI SQL `CURRENT_TIMESTAMP(6)` paired with `serverTimezone=UTC` in the JDBC connection URL guarantees universal portability across test and production engines.

### Q10: What happens if a Flyway migration fails in MySQL?
**Answer:** Unlike PostgreSQL, MySQL DDL statements (like `CREATE TABLE` or `ALTER TABLE`) are non-transactional and trigger implicit commits. If a Flyway migration fails halfway, the schema is left in a dirty state. Recovery requires manually reverting the partial changes and cleaning the failed record from `flyway_schema_history`.

---

## Category 3: Spring Boot 4 & Spring Security Architecture

### Q11: Explain the Spring Security Filter Chain execution order in this application.
**Answer:**
1. `RequestIdFilter`: Generates unique correlation ID and populates MDC.
2. `RateLimitFilter`: Evaluates Bucket4j token bucket limits (returns 429 if exceeded).
3. `JwtAuthenticationFilter`: Extracts Bearer token, validates signature/claims, and populates `SecurityContextHolder`.
4. `UsernamePasswordAuthenticationFilter`: Standard Spring security dispatch.
5. `FilterSecurityInterceptor`: Enforces method-level and URL-level `@PreAuthorize` rules.

### Q12: Why is `SessionCreationPolicy.STATELESS` configured?
**Answer:** It instructs Spring Security never to create or store an `HttpSession` on the server. Every incoming request must be authenticated independently via Bearer JWT, allowing horizontal scalability across stateless server nodes without sticky sessions.

### Q13: How does the application enforce the principle of Least Privilege between Admins and Analysts?
**Answer:** Segregation of duties is enforced at the controller layer using Spring Security `@PreAuthorize`:
- Case resolution (`/fraud/alerts/{id}/release`) is strictly restricted to `hasRole('FRAUD_ANALYST')`.
- If an `ADMIN` attempts to resolve an alert, Spring Security rejects the request with HTTP `403 Forbidden`. Admins only have read-only visibility into fraud queues.

### Q14: How does the application prevent administrative self-lockout?
**Answer:** In `AdminService`, when updating roles or user statuses, the system checks:
`if (targetUser.getId().equals(currentUser.getId())) throw new BusinessRuleException("Admins cannot modify their own status or role");`
This prevents an administrator from accidentally suspending or demoting themselves.

---

## Category 4: Web Security & OWASP Defense

### Q15: Why are JWT access tokens NOT stored in `localStorage` or `sessionStorage`?
**Answer:** `localStorage` is accessible to any JavaScript executing in the browser origin. If a third-party package or vulnerability introduces Cross-Site Scripting (XSS), the attacker can exfiltrate the token via `localStorage.getItem()`. In TrustPay Guardian, access tokens reside strictly in volatile closure memory in `axiosClient.ts`.

### Q16: How does the application protect against Cross-Site Request Forgery (CSRF)?
**Answer:** The primary REST API uses Bearer authentication, which is immune to CSRF because browsers do not automatically attach `Authorization` headers. For the cookie-based refresh endpoint (`/api/v1/auth/refresh`), the cookie is configured with `SameSite=Strict`, preventing foreign cross-origin sites from sending the cookie.

### Q17: What is an IDOR vulnerability, and how is it prevented in TrustPay Guardian?
**Answer:** Insecure Direct Object Reference occurs when a user accesses another customer's account by manipulating IDs in the URL (e.g. `GET /api/v1/accounts/2`). In TrustPay Guardian, service methods explicitly verify ownership:
`if (!account.getUser().getId().equals(currentUser.getId())) throw new NotFoundException("Account not found");`
Returning HTTP 404 instead of 403 prevents attackers from enumerating valid account IDs.

### Q18: What is the purpose of the UUID `Idempotency-Key` header?
**Answer:** If a network timeout occurs after money is debited but before the client receives the response, the customer might click "Submit" again. By generating a UUID on the frontend and enforcing `UNIQUE(idempotency_key)` on the backend, repeated requests return the cached original transaction response without executing duplicate debits.

---

## Category 5: Frontend Architecture (React 19 & TypeScript)

### Q19: How does Axios handle silent token refresh without logging out the user?
**Answer:** In `axiosClient.ts`, a response interceptor captures HTTP `401 Unauthorized`. It queues pending requests, issues a single background call to `POST /api/v1/auth/refresh`, updates the in-memory access token, and retries the original request seamlessly.

### Q20: How does the application prevent "Theme Flash" on initial page load?
**Answer:** An inline script is placed in the `<head>` of `index.html` before React mounts. It reads the user's preferred theme from `localStorage` (or checks `prefers-color-scheme`) and sets `data-bs-theme` on the `<html>` root immediately, eliminating the jarring white flash on dark mode.

### Q21: What is the difference between Redux Toolkit and React Context in this project?
**Answer:**
- **React Context:** Used for low-frequency global state like Theme (light/dark) and Toast notifications.
- **Redux Toolkit:** Used for high-frequency or polled server state, specifically the 30-second notification unread count polling (`notificationSlice.ts`), preventing unnecessary full-tree re-renders.

---

## Category 6: DevOps, Testing & CI/CD

### Q22: Why use multi-stage Docker builds?
**Answer:** Multi-stage builds separate the build environment (Maven, JDK, npm, source code, compilers) from the final runtime image. The production image contains only the compiled `.jar` and a slim JRE (or Nginx binary), reducing the attack surface and image size by over 70%.

### Q23: Why run containers as non-root users (`appuser`)?
**Answer:** If an application running as `root` inside a container is compromised via remote code execution, the attacker has root privileges on the container and could exploit kernel vulnerabilities to escape onto the host machine. Running as UID `10001` enforces the principle of least privilege.

### Q24: What does the JaCoCo 80% coverage check enforce during `./mvnw verify`?
**Answer:** JaCoCo measures line and branch coverage across compiled bytecode. In `pom.xml`, the `check` goal specifies `<minimum>0.80</minimum>`. If automated tests cover less than 80% of instructions across application classes, the Maven build fails, blocking PR merges.
