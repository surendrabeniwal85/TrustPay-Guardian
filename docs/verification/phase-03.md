# Phase 3 Verification: Authentication and Role-Based Access Control

## Date: 2026-10-26

## Objective
Verify the authentication, token rotation, brute-force protection, rate limiting, and RBAC requirements from PRD sections 4.1 and 8:
- `SecurityConfig` (stateless, URL rules by role, `@EnableMethodSecurity`)
- `JwtService` (HS256, 15 min expiration, claims `sub`, `role`, `iat`, `exp`, `jti`, `userId`, `fullName`, refuse to start if `JWT_SECRET` is missing or < 32 bytes)
- `RestAuthenticationEntryPoint` (401 RFC 7807 `ProblemDetail`) and `RestAccessDeniedHandler` (403 RFC 7807 `ProblemDetail`)
- `RateLimitFilter` with Bucket4j (10 login/min/IP, 20 transfer/min/user, HTTP 429 with `Retry-After`)
- Passwords with BCrypt strength 12; lockout after 5 consecutive failures for 15 minutes with generic failure message
- Refresh tokens (random 256-bit cryptographically secure values, stored only as SHA-256 hashes in `refresh_tokens`, 7-day validity, rotated on each use, reuse of revoked token terminates all sessions for user, sent as `HttpOnly SameSite=Strict` cookie)
- Registration creates `CUSTOMER` only and default `SAVINGS` account via `AccountService.createDefaultAccount`
- Password change endpoint `POST /api/v1/auth/change-password` with old password verification and token revocation
- `DevDataSeeder` (dev profile only) seeding 1 admin, 1 analyst, 3 customers with passwords from `DEV_SEED_PASSWORD`
- Audit events for register, login success and failure, lockout, logout, password change.

## Verification Checks

### 1. Test Suite Execution
- **Command:** `.\mvnw.cmd test "-Dtest=JwtServiceTest,AuthServiceTest,AuthServiceIntegrationTest,AuthControllerMockMvcTest"`
- **Result:**
  - `JwtServiceTest`: 5/5 PASSED
  - `AuthServiceTest`: 5/5 PASSED
  - `AuthServiceIntegrationTest`: 4/4 PASSED
  - `AuthControllerMockMvcTest`: 2/2 PASSED
- **Total Tests Run:** 16 tests run, 0 failures, 0 errors, 0 skipped. `BUILD SUCCESS`.
- **Full Suite Run:** `.\mvnw.cmd test`
  - Tests run: 61, Failures: 0, Errors: 0, Skipped: 5. `BUILD SUCCESS` in 24.851s.

### 2. Specific Behavioral Checks
- **Startup Protection:** `JwtServiceTest.refusesToStartWithInvalidSecret` confirms that providing a null, empty, or < 32-byte secret throws `IllegalStateException` and refuses startup.
- **Registration & Validation:**
  - Weak password (`"weak"`) rejected with `400 BAD_REQUEST` and `fieldErrors.password`.
  - Valid registration creates `CUSTOMER` role, hashes password with BCrypt (12 rounds), auto-creates default `SAVINGS` account with ₹10,000.00 demo opening balance.
  - Duplicate registration returns `409 CONFLICT` with ProblemDetail.
- **Login & Cookie Policy:**
  - Login returns access token in response body.
  - Refresh token delivered exclusively via `Set-Cookie: refreshToken=...; Path=/api/v1/auth; Max-Age=604800; HttpOnly; SameSite=Strict`.
  - Bad credentials return `401 UNAUTHORIZED` with `"Invalid email or password"`.
- **Brute-Force Lockout:**
  - 5 failed login attempts lock account for 15 minutes; subsequent attempts fail while locked.
- **RBAC Boundaries:**
  - Unauthenticated `GET /api/v1/auth/me` returns `401 UNAUTHORIZED`.
  - Customer token accessing `/api/v1/admin/users` returns `403 FORBIDDEN` with ProblemDetail.
- **Token Rotation & Theft Detection:**
  - Calling `/api/v1/auth/refresh` produces a fresh access token and new rotated refresh token.
  - Presenting the old (revoked) refresh token immediately revokes all active refresh tokens for the user and terminates sessions.
- **Password Change:**
  - `POST /api/v1/auth/change-password` verifies current password, validates new password complexity, enforces mismatch with confirm/current, updates hash, and revokes all active refresh tokens.

### 3. Status
**DONE** - All Phase 3 requirements implemented, tested, and verified.
