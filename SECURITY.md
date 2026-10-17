# Security Policy

## Scope and Educational Intent

**TrustPay Guardian** is an educational, portfolio-grade banking simulation application designed to demonstrate robust security practices in modern full-stack web applications. It is strictly not intended for production banking or payment processing.

## Reporting a Vulnerability

If you discover a potential security vulnerability within this repository, please follow these guidelines:

1. **Do not create public GitHub issues** for security vulnerabilities.
2. Email the maintainer or project owner with a detailed proof of concept, including:
   - Type of vulnerability (e.g., IDOR, SQL injection, token handling flaw)
   - Step-by-step reproduction instructions
   - Potential impact
3. Security reports will be reviewed within 48 hours.

## Implemented Security Controls

- **Stateless JWT:** Short-lived (15 minutes) access tokens stored strictly in frontend memory.
- **Refresh Token Rotation:** 256-bit cryptographically secure tokens stored as SHA-256 hashes in MySQL, delivered via `HttpOnly`, `SameSite=Strict` cookies. Token reuse immediately invalidates all user sessions.
- **Account Lockout:** 5 consecutive failed login attempts trigger a 15-minute lockout period.
- **Password Hashing:** Passwords encoded with BCrypt (strength 12).
- **Authorization & Ownership:** Backend enforcement using Spring Security RBAC and service-layer IDOR checks.
- **Redaction:** Passwords, tokens, and raw account numbers are redacted from system logs.
