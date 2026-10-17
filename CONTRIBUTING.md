# Contributing to TrustPay Guardian

Thank you for contributing to TrustPay Guardian! To maintain high code quality, security posture, and educational clarity, please adhere to these guidelines.

## Development Principles

1. **Modular Monolith Integrity:** Keep feature domain boundaries clean (controller, service, repository, entity, dto).
2. **Never Commit Secrets:** Do not commit `.env`, passwords, JWT secrets, or local database credentials.
3. **Backend Is the Source of Truth:** Never rely solely on frontend validation for business invariants, permissions, or ownership.
4. **Conventional Commits:** Use structured commit prefixes:
   - `feat(scope): ...`
   - `fix(scope): ...`
   - `test(scope): ...`
   - `docs(scope): ...`
   - `refactor(scope): ...`
   - `chore(scope): ...`

## Branching Strategy

- `main`: Demonstrable, verified releases.
- `feature/<name>`: Focused feature branches.

## Pre-Commit Verification

Before submitting changes, ensure all tests and builds pass:
```powershell
# Backend verification
cd backend
.\mvnw.cmd clean verify

# Frontend verification
cd frontend
npm run lint
npm run build
npm test
```
