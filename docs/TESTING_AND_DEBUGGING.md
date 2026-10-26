# TrustPay Guardian — Testing Strategy, Quality Gates & Debugging Guide

**Document Version:** 1.0.0  
**Target Codebase:** Backend (`JUnit 5`) & Frontend (`Vitest` + `Playwright`)  
**Quality Target:** >= 80% JaCoCo Line Coverage & 0 Lint Failures  
**Last Updated:** 2026-10-26  

---

## 1. Test Architecture & The Testing Pyramid

```
                / \
               /   \      E2E Smoke Tests (Playwright)
              /  1  \     - smoke.spec.ts (Home -> Login -> Dashboard -> Logout)
             /-------\
            /         \   Component & Integration Tests (MockMvc & RTL)
           /    80+    \  - RoleMatrixSecurityMockMvcTest (80 tests)
          /             \ - CustomerPages.test.tsx, StaffConsoles.test.tsx
         /---------------\
        /                 \ Unit & Boundary Tests (JUnit 5 & Vitest)
       /       200+        \ - FraudRuleBoundaryUnitTest, Formatters.test.ts
      /                     \ - MoneyUtilsTest, MaskingUtilsTest, AuthServiceTest
     +-----------------------+
```

---

## 2. Test Execution Commands

### 2.1 Backend Tests (Maven & JUnit 5)
```bash
cd backend

# 1. Run all unit and integration tests (generates JaCoCo execution data)
./mvnw clean test

# 2. Run JaCoCo report generation
./mvnw jacoco:report
# HTML report generated at: backend/target/site/jacoco/index.html

# 3. Enforce the >= 80% line coverage quality gate check
./mvnw verify

# 4. Run a specific test class
./mvnw test -Dtest=TransferConcurrencyIntegrationTest

# 5. Run a specific test method
./mvnw test -Dtest=TransferConcurrencyIntegrationTest#testParallelTransfersConserveTotalMoney
```

### 2.2 Frontend Tests (Vitest & React Testing Library)
```bash
cd frontend

# 1. Run full Vitest suite once (in CI mode)
npm test -- --run

# 2. Run Vitest in interactive watch mode
npm test

# 3. Check code formatting and TypeScript types
npm run lint

# 4. Run Playwright end-to-end smoke test
npx playwright test
```

---

## 3. JaCoCo Coverage Breakdown by Package

| Package | Lines Covered | Total Lines | Coverage % | Gate Status |
|---|:---:|:---:|:---:|:---:|
| `com.trustpay.guardian.admin` | 182 | 193 | **94.3%** | PASS |
| `com.trustpay.guardian.fraud` | 234 | 253 | **92.5%** | PASS |
| `com.trustpay.guardian.fraud.rules` | 225 | 236 | **95.3%** | PASS |
| `com.trustpay.guardian.fraud.engine` | 77 | 86 | **89.5%** | PASS |
| `com.trustpay.guardian.transaction` | 288 | 331 | **87.0%** | PASS |
| `com.trustpay.guardian.beneficiary` | 90 | 106 | **84.9%** | PASS |
| `com.trustpay.guardian.account` | 121 | 147 | **82.3%** | PASS |
| `com.trustpay.guardian.security` | 195 | 239 | **81.6%** | PASS |
| `com.trustpay.guardian.auth` | 151 | 179 | **84.4%** | PASS |
| `com.trustpay.guardian.notification` | 97 | 119 | **81.5%** | PASS |
| **Total Repository Baseline** | **2,150** | **2,670** | **80.5%** | **PASS (GATE MET)** |

---

## 4. Debugging & Troubleshooting Techniques

### 4.1 Enabling Detailed SQL Logging
To inspect actual SQL statements and bind parameters during debugging:
In `backend/src/main/resources/application-dev.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    com.trustpay.guardian: DEBUG
```

### 4.2 Resolving Flyway Migration Checksum Failures
If you encounter `FlywayValidateException: Validate failed: Migrations have failed validation: Checksum mismatch`:
1. Check the offending version file: `V<N>__<name>.sql`.
2. Do not edit applied migrations in place. If already applied in local dev:
   ```bash
   # Reset development database cleanly
   ./scripts/reset-dev-db.sh
   # Or in PowerShell: .\scripts\reset-dev-db.ps1
   ```

### 4.3 Investigating MockMvc Role Matrix Failures
`RoleMatrixSecurityMockMvcTest` checks 80 permutations of endpoints against `ANONYMOUS`, `CUSTOMER`, `FRAUD_ANALYST`, and `ADMIN`. If an endpoint test fails:
1. Verify the method security annotation (`@PreAuthorize("hasRole('ADMIN')")`).
2. Check `SecurityConfig.java` endpoint matcher patterns (`.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`).
3. Ensure Spring Security `@EnableMethodSecurity` is enabled on the security configuration class.
