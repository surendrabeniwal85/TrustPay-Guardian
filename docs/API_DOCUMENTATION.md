# TrustPay Guardian — Complete REST API Documentation & Contract Reference

**API Version:** `v1`  
**Base Path:** `/api/v1`  
**Specification:** OpenAPI 3.0 / Swagger UI at `/swagger-ui.html`  
**Error Standard:** RFC 7807 Problem Details for HTTP APIs  
**Last Updated:** 2026-10-26  

---

## 1. Global Conventions & Error Response Standard

### Standard Success Envelope
All successful responses return HTTP `200 OK` or `201 Created` with a standardized envelope:
```json
{
  "success": true,
  "data": { ... },
  "message": "Optional human-readable confirmation"
}
```

### Standard Error Response (RFC 7807 Problem Details)
```json
{
  "type": "about:blank",
  "title": "Business Rule Violation",
  "status": 422,
  "detail": "Insufficient available balance. Pending holds: ₹15,000.00",
  "instance": "/api/v1/transactions/transfer",
  "requestId": "req-9f3a1e2b-48cd-41e9",
  "fieldErrors": {
    "amount": "Must be greater than zero"
  }
}
```

---

## 2. Authentication Endpoints (`/api/v1/auth`)

### 2.1 Sign In (`POST /api/v1/auth/login`)
- **Access:** Public (Rate Limited: 10/min/IP)
- **Request Body:**
  ```json
  {
    "email": "john.doe@trustpay.internal",
    "password": "Password123!"
  }
  ```
- **Response `200 OK`:**
  - Headers: `Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=604800`
  - Body:
    ```json
    {
      "success": true,
      "data": {
        "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 900,
        "user": {
          "id": 1,
          "email": "john.doe@trustpay.internal",
          "fullName": "John Doe",
          "role": "CUSTOMER"
        }
      }
    }
    ```

### 2.2 Register Account (`POST /api/v1/auth/register`)
- **Access:** Public
- **Request Body:** `fullName` (>=2 chars), `email` (valid email), `phoneNumber` (10 digits), `password` (>=8 chars, complexity verified).
- **Default Role:** Strictly `CUSTOMER`.

### 2.3 Silent Token Refresh (`POST /api/v1/auth/refresh`)
- **Access:** Public (Requires `refreshToken` cookie)
- **Behavior:** Validates SHA-256 hash against database, rotates token, returns new access token and rotated cookie.

### 2.4 Sign Out (`POST /api/v1/auth/logout`)
- **Access:** Authenticated
- **Behavior:** Invalidates active refresh token in database and returns cleared cookie (`Max-Age=0`).

---

## 3. Account Endpoints (`/api/v1/accounts`)

| Method | Endpoint | Role | Description |
|---|---|:---:|---|
| `GET` | `/api/v1/accounts` | `CUSTOMER` | Returns list of authenticated user's active accounts with masked account numbers and available balances. |
| `POST` | `/api/v1/accounts` | `CUSTOMER` | Opens an additional account (capped at 3 accounts per customer; 4th returns `422 Unprocessable Entity`). |
| `GET` | `/api/v1/accounts/{id}/statement` | `CUSTOMER` | Paginated transaction statement with date range filtering (`startDate`, `endDate`, `page`, `size`). IDOR protected (returns 404 on foreign accounts). |

---

## 4. Beneficiary Endpoints (`/api/v1/beneficiaries`)

| Method | Endpoint | Role | Description |
|---|---|:---:|---|
| `GET` | `/api/v1/beneficiaries` | `CUSTOMER` | Lists saved beneficiaries for the authenticated user. |
| `GET` | `/api/v1/beneficiaries/lookup?accountNumber=...` | `CUSTOMER` | Validates target account and returns masked holder name (e.g. `J*** D**`). |
| `POST` | `/api/v1/beneficiaries` | `CUSTOMER` | Adds a verified beneficiary. Rejects self-addition (422) and duplicate addition (409). |
| `DELETE` | `/api/v1/beneficiaries/{id}` | `CUSTOMER` | Removes saved beneficiary by ID. IDOR protected. |

---

## 5. Transaction & Money Movement Endpoints (`/api/v1/transactions`)

### 5.1 Execute Transfer (`POST /api/v1/transactions/transfer`)
- **Role:** `CUSTOMER` (Rate Limited: 20/min/user)
- **Required Header:** `Idempotency-Key: <UUID>`
- **Request Body:**
  ```json
  {
    "sourceAccountId": 1,
    "destinationAccountNumber": "TPG1000000002",
    "amount": 500.00,
    "note": "Office lunch contribution"
  }
  ```
- **Responses:**
  - `200 OK` / `APPROVED_SIMULATED`: Instant approval, funds debited and credited.
  - `200 OK` / `PENDING_REVIEW`: Risk score 40–69. Transfer held for analyst review. Dynamic available balance hold created.
  - `200 OK` / `BLOCKED`: Risk score >= 70. Transfer blocked immediately.
  - `409 Conflict`: Reused `Idempotency-Key` with differing parameters or ownership mismatch.
  - `422 Unprocessable Entity`: Insufficient available balance, frozen account, or self-transfer.

### 5.2 Simulate Deposit (`POST /api/v1/transactions/deposit`)
- **Role:** `CUSTOMER`
- **Request Body:** `accountId`, `amount` (₹100 to ₹100,000). Simulates ATM / external bank credit.

### 5.3 Filter Transactions (`GET /api/v1/transactions`)
- **Role:** `CUSTOMER`
- **Query Params:** `type`, `status`, `startDate`, `endDate`, `search`, `page`, `size`.
- **Scope:** Strictly returns transactions where user is sender or recipient.

---

## 6. Fraud Analyst Endpoints (`/api/v1/fraud`)

| Method | Endpoint | Role | Description |
|---|---|:---:|---|
| `GET` | `/api/v1/fraud/stats` | `FRAUD_ANALYST`, `ADMIN` | Returns queue KPIs: open alerts, average risk score, resolved count, false-positive rate. |
| `GET` | `/api/v1/fraud/alerts` | `FRAUD_ANALYST`, `ADMIN` | Alert triage queue sorted by severity (`HIGH` -> `MEDIUM` -> `LOW`) then age. |
| `GET` | `/api/v1/fraud/alerts/{id}` | `FRAUD_ANALYST`, `ADMIN` | Forensic alert dossier: score meter, rule-hit breakdown, sender history. |
| `POST` | `/api/v1/fraud/alerts/{id}/release` | `FRAUD_ANALYST` | Releases held transfer. Converts hold to posted ledger debit. (Admin gets 403). |
| `POST` | `/api/v1/fraud/alerts/{id}/confirm-block` | `FRAUD_ANALYST` | Confirms transfer block. Releases held funds reservation. (Admin gets 403). |

---

## 7. Administrator Governance Endpoints (`/api/v1/admin`)

| Method | Endpoint | Role | Description |
|---|---|:---:|---|
| `GET` | `/api/v1/admin/dashboard/stats` | `ADMIN` | 14-day daily transaction volume trend and rule hit distribution metrics. |
| `GET` | `/api/v1/admin/users` | `ADMIN` | Filterable user listing across roles and statuses. |
| `POST` | `/api/v1/admin/users/staff` | `ADMIN` | Provisions new `FRAUD_ANALYST` or `ADMIN` user with hashed credentials. |
| `PATCH` | `/api/v1/admin/users/{id}/role` | `ADMIN` | Updates user role. Self-demotion guard rejects changes on admin's own ID (400). |
| `PATCH` | `/api/v1/admin/users/{id}/status` | `ADMIN` | Suspends or enables user account. Self-suspension guard rejects changes on admin (400). |
| `GET` | `/api/v1/admin/accounts` | `ADMIN` | Bank-wide account listing with status and balance filters. |
| `PATCH` | `/api/v1/admin/accounts/{id}/freeze` | `ADMIN` | Immediately freezes account, halts all debits/credits, issues user notification. |
| `PATCH` | `/api/v1/admin/accounts/{id}/unfreeze` | `ADMIN` | Restores account to `ACTIVE` status. |
| `GET` | `/api/v1/admin/fraud-rules` | `ADMIN` | Lists all 7 fraud strategy rules with enabled flags, thresholds, and points. |
| `PATCH` | `/api/v1/admin/fraud-rules/{id}` | `ADMIN` | Tunes rule parameters (0–100 points validation) and invalidates in-memory cache. |
| `GET` | `/api/v1/admin/audit-logs` | `ADMIN` | Append-only compliance log viewer with action and date filters. |

---

## 8. Public Unauthenticated Counters (`/api/v1/public/stats`)

- **Method:** `GET /api/v1/public/stats`
- **Access:** Public (`permitAll`)
- **Response `200 OK`:**
  ```json
  {
    "success": true,
    "data": {
      "totalTransfers": 1420,
      "totalVolume": 18450250.00,
      "totalUsers": 580,
      "alertsAnalyzed": 145
    }
  }
  ```
- **Privacy Assurance:** Contains zero user PII, account numbers, or IP addresses.
