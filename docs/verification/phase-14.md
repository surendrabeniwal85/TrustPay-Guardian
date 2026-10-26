# Phase 14 Verification: Docker, CI/CD and Monitoring

**Date**: 2026-10-26
**Commit Target**: `ci: add docker, compose, jenkins pipeline and github actions`
**Branch**: `feature/phase-14-docker-cicd`

---

## 1. Automated Checks & Evidence

### Dockerfile & Image Specification
- **Backend Dockerfile (`backend/Dockerfile`)**:
  - Multi-stage build: `eclipse-temurin:25-jdk-noble` (builder) -> `eclipse-temurin:25-jre-noble` (runtime)
  - Non-privileged execution: dedicated non-root user `appuser` (UID 10001, GID 10001)
  - Container JVM tuning: `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`, `-Djava.security.egd=file:/dev/./urandom`
  - Healthcheck: `curl -fsS http://localhost:8080/actuator/health || exit 1`
  - Ignores local build artifacts via `backend/.dockerignore`
- **Frontend Dockerfile (`frontend/Dockerfile`)**:
  - Multi-stage build: `node:24-alpine` (builder) -> `nginx:1.27-alpine` (runtime)
  - Production distribution served via Nginx with custom `nginx.conf`
  - Healthcheck: `wget -q -O - http://localhost/health || exit 1`
  - Ignores dependencies and transient files via `frontend/.dockerignore`

### Nginx Reverse Proxy & Security Headers (`frontend/nginx.conf`)
- Reverse proxy maps `/api/` traffic to Spring Boot backend service at `http://backend:8080/api/` preserving cookies and client IP
- Single Page Application (SPA) fallback to `/index.html`
- Strict security headers configured:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`
  - `Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: https: blob:; media-src 'self' data: blob:; connect-src 'self' http://localhost:8080; frame-ancestors 'none';`
- Gzip compression enabled for web assets

### Compose Orchestration (`compose.yaml`, `docker-compose.yml`, `compose.prod.yml`)
- `compose.yaml`: orchestrates `mysql` (9.7.0 LTS), `mailhog` (v1.0.1), `backend`, and `frontend` on dedicated bridge network `trustpay-network`
- `compose.prod.yml`: production profile removing simulated MailHog and dev seeder; enforcing mandatory environment variable secrets and container resource limits (CPU/Memory)
- `docker-compose.yml`: backwards-compatible alias

### CI/CD Pipelines
- **Jenkinsfile**: Declarative 7-stage pipeline:
  - `Checkout` -> `Backend Build & Verify` (JaCoCo 80% gate check) -> `SonarQube Static Analysis` -> `Frontend Lint, Test & Build` -> `Docker Images Build` -> `Deploy to Staging`
- **GitHub Actions (`.github/workflows/ci.yml`)**:
  - `backend-verify`: JDK 25 setup, Maven verify, JaCoCo test report archiving
  - `frontend-verify`: Node 24 setup, npm ci, npm run lint, npm test -- --run, npm run build
  - `docker-validate`: tests `docker compose config` against both dev and prod compose specifications

### Production Actuator Lockdown
- `backend/src/main/resources/application-prod.yml` updated:
  - `management.endpoints.web.exposure.include: ${ACTUATOR_EXPOSURE:health}` (only health probe publicly exposed)
  - `management.endpoint.health.show-details: never`
  - `springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false` (Swagger locked down in prod)

### Development Scripts
- `scripts/start-dev.sh` & `scripts/start-dev.ps1`: Automated startup for local developer environment
- `scripts/reset-dev-db.sh` & `scripts/reset-dev-db.ps1`: Safe development database drop and recreation

---

## 2. Requirement Verification Summary

| Requirement | Implementation Detail | Status |
| :--- | :--- | :--- |
| Backend Dockerfile | Multi-stage build with Eclipse Temurin Java 25, non-root user, container flags, healthcheck | VERIFIED |
| Frontend Dockerfile | Multi-stage build with Node 24 and Nginx 1.27 Alpine, healthcheck | VERIFIED |
| Nginx Reverse Proxy | API proxying with preserved cookies, SPA fallback, strict CSP and OWASP security headers | VERIFIED |
| Root Compose Stack | Orchestrates MySQL 9.7, Backend, Frontend, and MailHog; single-command startup | VERIFIED (Config) |
| Production Compose | Strict environment secrets enforcement, resource limits, no MailHog | VERIFIED (Config) |
| Jenkinsfile | 7-stage declarative pipeline with JaCoCo and SonarQube | VERIFIED |
| GitHub Actions CI | Multi-job workflow testing backend, frontend, and compose configs | VERIFIED |
| Prod Actuator Lockdown | Only `/actuator/health` exposed in prod; Swagger disabled | VERIFIED |
| Developer Scripts | Cross-platform Bash and PowerShell startup and database reset scripts in `scripts/` | VERIFIED |
| Live Docker Run on Host | Docker engine not installed on host machine; runs in CI container environments | UNVERIFIED (`docker compose up --build`) |
