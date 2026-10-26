# TrustPay Guardian — Phase 0 Verification Report

**Phase:** Phase 0 — Repository Scaffolding and Tooling  
**Branch:** `feature/phase-00-scaffolding`  
**Execution Date:** 2026-10-26  
**Auditor / Engineer:** Principal Java Full Stack Engineer  
**Status:** **PASSED** (With Docker items documented as UNVERIFIED due to host environment constraints)

---

## 1. Checks and Evidence

### Check 1: Backend Clean Verification
- **Command:** `.\mvnw.cmd clean verify` (executed in `backend/`)
- **Result:** `BUILD SUCCESS` (Total time: 23.703 s)
- **Tests Executed:** 39 tests passed, 0 failures, 0 errors, 5 skipped (MySQL server-dependent integration tests skipped cleanly without live credentials).
- **Artifact:** `target/trustpay-guardian-backend-0.1.0-SNAPSHOT.jar` (repackaged with nested dependencies).

### Check 2: Java and Maven Versions
- **Command:** `java -version`, `.\mvnw.cmd -version`
- **Result:**
  - Java version: `25.0.1` 2025-10-21 LTS (Oracle Corporation, build `25.0.1+8-LTS-27`)
  - Maven wrapper version: `3.9.16`

### Check 3: Dependencies & Configuration Alignment
- Added `spring-boot-starter-mail` to `backend/pom.xml`.
- Added `springdoc-openapi-starter-webmvc-ui:2.8.5` to `backend/pom.xml`.
- Added `backend/src/main/resources/application-prod.yml` with strict production defaults (Hikari pool tuning, graceful shutdown, Flyway validate-on-migrate, Hibernate ddl-auto=validate, UTC timezone, locked-down Actuator, disabled public Swagger).

### Check 4: Frontend Build and Scripts
- **Command:** `npm install` (in `frontend/`)
- **Result:** Successfully installed 144 packages including `react-hook-form`, `zod`, `@hookform/resolvers`, `@reduxjs/toolkit`, `react-redux`, `recharts`, `vitest`, and `@testing-library/react`.
- **Command:** `npm run typecheck`
- **Result:** `tsc -b --noEmit` exited code 0 with 0 errors.
- **Command:** `npm run build`
- **Result:** `tsc -b && vite build` completed in 1.53s; production assets compiled into `dist/`.

### Check 5: Compose & Docker Infrastructure
- **File:** `compose.yaml` updated with MySQL 9.7 / 8.4 LTS and MailHog (`mailhog/mailhog:v1.0.1`) with healthchecks.
- **Status:** **UNVERIFIED (Docker not installed on host machine)**
- **Reason:** Docker CLI / daemon is not present in PATH on the host Windows machine (`docker : The term 'docker' is not recognized`).
- **Command for User to Run Later:** `docker compose up -d mysql mailhog` and verify with `docker compose ps`.

### Check 6: Scripts and Cleanliness
- Cleaned and fixed PowerShell syntax in `scripts/verify-mysql.ps1`.
- Verified native MySQL 8.4.11 running on host port 3306.
- Confirmed no `.env`, `target/` or `node_modules/` tracked by git.
