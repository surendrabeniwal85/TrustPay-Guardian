# TrustPay Guardian — Tech Stack and Version Policy

**Project Title:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Architecture:** Modular Monolith (Spring Boot + React + MySQL)  
**Date Checked / Updated:** 2026-10-17  
**Status:** Approved Baseline — Verified Local Environment  

---

## 1. Verified Local Environment Baseline

| Tool / Runtime | Detected Local Version | Verified Location / Detail | Port / Status |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 11 (amd64) | Host development environment | Verified |
| **Java JDK** | 25.0.1 LTS (Oracle JDK 25) | `C:\Program Files\Java\jdk-25` (`build 25.0.1+8-LTS-27`) | Verified |
| **Java Compiler** | javac 25.0.1 | `C:\Program Files\Java\jdk-25\bin\javac.exe` | Compatible with `--release 25` |
| **Maven** | Apache Maven 3.9.16 | `C:\Users\Dell\apache-maven-3.9.16-bin` | Verified running on JDK 25 |
| **Node.js** | v24.15.0 LTS | Node runtime for Vite/npm toolchain | Verified |
| **npm** | 11.12.1 | Package manager for frontend | Verified |
| **MySQL Server** | **8.4.11 LTS Community Server** | Windows Service `MySQL84` (`Running`) | **Port 3306 (TCP Open/Verified)** |
| **MySQL Client** | 8.4.11 Win64 | `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe` | Verified |
| **Docker** | Not installed / Not in PATH | Local native setup; Testcontainers optional | Verified |
| **Git** | Installed (workspace uninitialized) | Ready for initialization in Phase 1 | Verified |

---

## 2. Technology Stack & Version Decisions

### 2.1 Backend (Java / Spring Boot)
- **Language Baseline:** Java 25 LTS (`maven.compiler.release=25`).
- **Framework:** Spring Boot `4.1.1` GA release (supporting Java 25 bytecode).
- **Core Starters:**
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator` (Restricted to `/actuator/health` and `/actuator/info`)
- **Persistence & Migrations:**
  - Spring Data JPA with Hibernate 6.x
  - Flyway Migrations: `flyway-core` and `flyway-mysql` (Versioned SQL migrations `V1` to `V6`)
- **Database Driver:** `com.mysql:mysql-connector-j:9.2.0` (Fully compatible with MySQL 8.4 LTS).
- **Security & Tokens:**
  - Spring Security with BCrypt password hashing (strength 12)
  - Stateless JWT: `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`
  - Access token lifespan: 15 minutes (in-memory on client)
  - Refresh token: 256-bit cryptographically secure token, SHA-256 hashed in database, transmitted via `HttpOnly`, `SameSite=Strict` cookie, rotated on every refresh
  - Account lockout: 5 consecutive failed logins trigger 15-minute lock
- **API Documentation:** `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` (OpenAPI 3 / Swagger UI).
- **Testing:** JUnit 5 (Jupiter), Mockito, MockMvc, AssertJ.

### 2.2 Database (MySQL 8.4 LTS)
- **Database Engine:** MySQL Community Server 8.4.11 LTS.
- **Port:** Local port `3306` (confirmed open and active via `Test-NetConnection`).
- **Monetary Precision:** MySQL `DECIMAL(19,2)` with CHECK `balance >= 0`; Java `BigDecimal` for arithmetic.
- **Timezone Storage:** UTC timestamps (`DATETIME(6)` / `TIMESTAMP`).
- **Schema Management:** Flyway migrations only (`ddl-auto=validate`). No manual table creation; zero hardcoded credentials.

### 2.3 Frontend (React / TypeScript / Vite)
- **Framework & Runtime:** React `19.3.x` with functional components and hooks.
- **Language:** TypeScript `5.7+` / `7.x`.
- **Build Tool:** Vite `6.x` / `8.x` with `@vitejs/plugin-react`.
- **Styling:** Bootstrap `5.3.x` with CSS variables and `data-bs-theme` attribute (persisted as `tpg-theme`).
- **Routing:** React Router DOM `6.28.x` / `7.x` with protected route wrappers.
- **State Management:** React Context API for theme and auth state; feature-scoped custom hooks.
- **Forms & Validation:** React Hook Form `7.54.x` with Zod `3.24.x`.
- **HTTP Client:** Axios `1.7.x` with request/response interceptors for token injection, CSRF, and 401/403 redirects.
- **Visuals & Charts:** Recharts `2.15.x` and Bootstrap Icons `1.11.x`.
- **Testing:** Vitest and React Testing Library; Playwright for end-to-end smoke testing.

### 2.4 Infrastructure & Execution Policy (Native Windows Host vs Docker)
- **Primary Execution Mode:** **100% Native Windows Host**.
  - Backend: Runs directly via `.\mvnw.cmd spring-boot:run` on JDK 25.
  - Frontend: Runs directly via `npm run dev` on Node v24.15.0 LTS.
  - Database: Runs directly on host Windows service `MySQL84` (`localhost:3306`).
- **Docker / Docker Compose Status:** **STRICTLY OPTIONAL**.
  - Docker is **not** required for Phase 1, Phase 2, or any subsequent development/testing phase.
  - In Phase 15, `Dockerfile.backend`, `Dockerfile.frontend`, and `compose.yaml` will be authored for packaging reference and GitHub Actions CI readiness, but running them locally is completely optional.
  - Integration tests will target a local test schema or isolated profiles without requiring Testcontainers or a local Docker engine.

---

## 3. Official Source References & Release Documentation

1. **Oracle Java 25 Release Notes:**  
   https://www.oracle.com/java/technologies/javase/25all-relnotes.html
2. **Spring Boot Releases & System Requirements:**  
   https://docs.spring.io/spring-boot/system-requirements.html
3. **MySQL 8.4 LTS Release Notes & Lifecycle:**  
   https://dev.mysql.com/doc/relnotes/mysql/8.4/en/
4. **React Versions & Release Notes:**  
   https://react.dev/versions
5. **Apache Maven Release History:**  
   https://maven.apache.org/docs/history.html
6. **Springdoc OpenAPI Specification:**  
   https://springdoc.org/
7. **JJWT Library Documentation:**  
   https://github.com/jwtk/jjwt
