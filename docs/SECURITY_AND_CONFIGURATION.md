# TrustPay Guardian — Security Architecture & Configuration Hardening

**Document Version:** 1.0.0  
**Target Environment:** Staging & Production Deployment  
**Classification:** Security Architecture & Hardening Guide  
**Last Updated:** 2026-10-26  

---

## 1. Cryptographic Standards & Password Security

### 1.1 Password Hashing (BCrypt Strength 12)
- Passwords are hashed using standard BCrypt with an adaptive work factor of `12` (`2^12 = 4,096` rounds).
- Provides resilience against GPU/ASIC rainbow table and brute-force cracking.
- Minimum complexity: 8 characters, containing at least one uppercase letter, one digit, and one special symbol.

### 1.2 JWT Signing (HMAC-SHA512)
- Signed using the `HS512` algorithm with a high-entropy 512-bit secret key (`APP_SECURITY_JWT_SECRET`).
- Enforces fail-fast startup: If the secret is missing or less than 64 characters (512 bits), application context initialization immediately terminates.
- Lifespan: Strictly limited to 15 minutes (`900,000ms`).

### 1.3 Refresh Token Hashing (SHA-256)
- Refresh tokens are generated as 256-bit cryptographically secure pseudorandom strings (`SecureRandom`).
- Before database persistence, tokens are hashed with `SHA-256`. Even in the event of an SQL injection or database breach, raw usable refresh tokens are never exposed.

---

## 2. Client-Side Token Architecture & XSS Mitigation

```
Browser Memory Space                          Persistent Browser Storage
+---------------------------------------+     +----------------------------------+
|  JavaScript Runtime Memory            |     |  localStorage / sessionStorage   |
|  - axiosClient.ts                     |     |                                  |
|    let inMemoryAccessToken = "jwt..." |     |  STRICTLY EMPTY!                 |
|                                       |     |  (Zero tokens stored here)       |
+---------------------------------------+     +----------------------------------+
                   |                                           ^
                   v                                           |
      Transmitted via Header:                      Protected against XSS
      Authorization: Bearer <token>
```

- **XSS Defense:** By storing access tokens in closure memory rather than `localStorage`, browser-based script injection attacks cannot read or exfiltrate active session tokens.
- **HttpOnly Cookie Isolation:** Refresh tokens are bound to `HttpOnly; SameSite=Strict; Secure; Path=/api/v1/auth`. Client-side JavaScript cannot access or manipulate the cookie.

---

## 3. Rate Limiting Architecture (Bucket4j)

Token bucket algorithms throttle malicious request rates before reaching database layers:

| Target Route | Key / Dimension | Capacity | Refill Rate | HTTP Status on Exhaustion |
|---|---|:---:|:---:|:---:|
| `/api/v1/auth/login` | Client IP Address | 10 tokens | 10 tokens per 1 minute | `429 Too Many Requests` |
| `/api/v1/transactions/transfer` | User ID | 20 tokens | 20 tokens per 1 minute | `429 Too Many Requests` |

Headers returned on rate-limited responses:
- `Retry-After: 60`
- Content-Type: `application/problem+json` (RFC 7807)

---

## 4. Web Server & Reverse Proxy Hardening (`nginx.conf`)

### 4.1 Content-Security-Policy (CSP)
```text
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: https: blob:; media-src 'self' data: blob:; connect-src 'self' http://localhost:8080; frame-ancestors 'none';
```
- Restricts media, scripts, and fonts strictly to the application's origin.
- Disallows framing by third-party sites (`frame-ancestors 'none'` prevents Clickjacking).

### 4.2 Standard Security Headers
- `X-Content-Type-Options: nosniff` (Prevents MIME-type confusion attacks).
- `X-Frame-Options: DENY` (Enforces clickjacking protection on legacy browsers).
- `Referrer-Policy: strict-origin-when-cross-origin` (Protects query string privacy on outbound links).
- `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()` (Disables unneeded browser APIs).

---

## 5. Production Actuator & Swagger Lockdown

In production (`application-prod.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: ${ACTUATOR_EXPOSURE:health} # Only health probe exposed publicly
  endpoint:
    health:
      show-details: never

springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:false} # Swagger disabled in prod
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:false}
```

---

## 6. Environment Configuration Matrix

| Variable | Description | Default (Dev) | Production Requirement |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `prod` |
| `SPRING_DATASOURCE_URL` | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/trustpay_guardian` | SSL-enforced connection string |
| `SPRING_DATASOURCE_USERNAME` | Database username | `trustpay_user` | Dedicated non-root user |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `trustpay_password` | High-entropy secret from vault |
| `APP_SECURITY_JWT_SECRET` | HMAC-SHA512 signing key | *(Pre-seeded dev key)* | 64+ char random hexadecimal/base64 string |
| `DEV_SEED_PASSWORD` | Synthetic user initial password | `Password123!` | Ignored in `prod` profile |
| `CORS_ALLOWED_ORIGINS` | Permitted web browser origins | `http://localhost:5173,http://localhost:3000` | Exact production domain (e.g. `https://trustpay.internal`) |
