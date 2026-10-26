# TrustPay Guardian: Fraud-Aware Digital Banking Platform

[![Java 25](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1%20GA-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-blue.svg)](https://dev.mysql.com/downloads/)
[![React](https://img.shields.io/badge/React-19.3-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7%2B-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> [!IMPORTANT]
> **Educational Simulation & Regulatory Disclaimer:**  
> TrustPay Guardian is an educational, portfolio-grade digital banking simulation designed to showcase secure modern Java full-stack engineering and explainable fraud-risk management workflows.  
> **It is NOT a real bank, payment processor, or financial institution, and is NOT certified against PCI-DSS, ISO 27001, RBI Digital Payment Security Controls, or any banking regulation.** It does NOT connect to real bank accounts, move real money, process real credit cards/UPI, or collect sensitive personal identity documents (such as real Aadhaar, PAN, or CVVs). All account numbers, balances, transactions, and users are strictly synthetic.

---

## Backend Reference & Developer Guide

### 1. Prerequisites
- **Java JDK 25 LTS** (Oracle OpenJDK 25 or Eclipse Temurin 25)
- **Apache Maven 3.9+** (or use included `./mvnw` / `mvnw.cmd`)
- **MySQL Server 8.4 LTS / 9.7 LTS** (or Docker container; H2 in-memory mode active by default in test profile)

### 2. Environment Variables & Configuration
The backend reads all sensitive credentials and runtime thresholds from environment variables (with safe local development defaults provided in `.env.example` and `application-dev.yml`):

| Environment Variable | Description | Dev Default | Production Requirement |
|---|---|---|---|
| `JWT_SECRET` | HMAC-SHA512 signing secret key | *(Pre-configured dev secret)* | Minimum 64-character high-entropy secret; startup fails if missing |
| `JWT_EXPIRATION_MS` | Access token lifespan | `900000` (15 minutes) | `900000` |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifespan | `604800000` (7 days) | `604800000` |
| `SPRING_DATASOURCE_URL` | JDBC Connection string | `jdbc:mysql://localhost:3306/trustpay_guardian` | SSL-enforced MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username | `trustpay_user` | Non-root production user |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `trustpay_secret` | Strong secret from vault/env |
| `HIKARI_MAX_POOL_SIZE` | HikariCP maximum connections | `10` | Sized according to server capacity |
| `CORS_ALLOWED_ORIGINS` | Permitted browser origins | `http://localhost:5173,http://localhost:3000` | Exact production frontend domain |

### 3. How to Run Locally
```bash
# Navigate to backend
cd backend

# Run with dev profile (starts on port 8080)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- API Base URL: `http://localhost:8080/api/v1`
- Swagger OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

### 4. How to Test & Verify Quality
```bash
# Run unit & mock tests with JaCoCo report
./mvnw clean test jacoco:report

# Run full verification including JaCoCo quality gate (>= 80% line coverage)
./mvnw verify

# Verify JaCoCo coverage threshold directly
./mvnw jacoco:check
```

### 5. Simulated Demonstration Accounts
*Default accounts seeded automatically during development for interactive testing:*

| Persona | Email | Dev Password | Initial Account | Opening Balance |
|---|---|---|---|---|
| **Customer 1** | `john.doe@example.com` | `Customer@123` | `TPG100000000001` | ₹50,000.00 |
| **Customer 2** | `jane.smith@example.com` | `Customer@123` | `TPG100000000002` | ₹30,000.00 |
| **Fraud Analyst** | `analyst@trustpay.com` | `Analyst@123` | *N/A (Analyst Persona)* | *N/A* |
| **Administrator** | `admin@trustpay.com` | `Admin@123` | *N/A (Admin Persona)* | *N/A* |

### 6. Interactive End-to-End Demo Script (PRD Section 16)
1. **Low-Risk Transfer (Instant Approval):**
   - Log in as Customer 1 (`john.doe@example.com`).
   - Transfer ₹500 to Customer 2 (`TPG100000000002`).
   - Risk score: 0 (Low). Status immediately transitions to `APPROVED_SIMULATED`.
   - Sender balance debited to ₹49,500; recipient balance credited to ₹30,500. Notifications delivered to both users.
2. **Medium/High-Risk Transfer (Analyst Review):**
   - Transfer ₹60,000 to a new recipient without prior transaction history.
   - Triggers `HIGH_AMOUNT` (+35 points) and `NEW_BENEFICIARY` (+25 points) -> Total risk score: 60 (Medium/High).
   - Status transitions to `PENDING_REVIEW`. Funds reserved under dynamic pending holds.
3. **Analyst Forensics & Triage:**
   - Log in as Fraud Analyst (`analyst@trustpay.com`).
   - Navigate to `/analyst/alerts`, open the pending alert, and inspect the forensic rule-hit breakdown.
   - Click **Release** (completes transfer) or **Confirm Block** (cancels transfer and releases hold) with mandatory decision notes.
4. **Admin Governance:**
   - Log in as Administrator (`admin@trustpay.com`).
   - Navigate to `/admin/fraud-rules` to tune rule thresholds or toggle active rules.
   - Review `/admin/audit-logs` to inspect immutable action logs.

---

## Documentation Links
- [Security Review & OWASP Assessment](docs/SECURITY_REVIEW.md)
- [Requirements Traceability Matrix](docs/REQUIREMENTS_TRACEABILITY.md)
- [Database Schema Design & Security Architecture](docs/DATABASE_DESIGN.md)
- [Architecture Decision Records (ADRs)](docs/DECISIONS.md)
- [PRD Specification](docs/PRD.md)
- [Verification Records](docs/verification/)


