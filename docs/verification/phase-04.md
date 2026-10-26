# Phase 4 Verification: Accounts and Beneficiaries

## Date: 2026-10-26

## Objective
Verify the accounts and beneficiaries requirements from PRD sections 4.2, 4.3, and Phase 4 prompt:
- `AccountService` and `AccountController`
- `AccountNumberGenerator` (`TPG` + 10 digits via `SecureRandom` with collision retry)
- List own accounts (masked: `TPG******1234`)
- Open an extra account (`POST /api/v1/accounts`, max 3 accounts per customer; 4th returns 422)
- Account detail (`GET /api/v1/accounts/{id}`)
- Statement with date range and pagination (`GET /api/v1/accounts/{id}/statement`)
- Available balance dynamic calculation (balance minus outgoing `PENDING_REVIEW` transfers)
- Frozen account handling (cannot send or receive)
- Beneficiaries:
  - Lookup by account number returning only masked account holder name (`GET /api/v1/beneficiaries/lookup?accountNumber=...`)
  - Add beneficiary (`POST /api/v1/beneficiaries`) with checks (account must exist, cannot add own account [422], unique per user [409])
  - List beneficiaries (`GET /api/v1/beneficiaries`)
  - Delete beneficiary (`DELETE /api/v1/beneficiaries/{id}`)
- Strict service-layer IDOR ownership enforcement: another user's account, statement, or beneficiary returns 404 (not 403) to prevent ID enumeration
- DTO records/classes with Bean Validation and OpenAPI annotations with examples.

## Verification Checks

### 1. Test Suite Execution
- **Command:** `.\mvnw.cmd test "-Dtest=AccountServiceTest,BeneficiaryServiceTest,AccountAndBeneficiaryMockMvcTest"`
- **Result:**
  - `AccountServiceTest`: 4/4 PASSED
  - `BeneficiaryServiceTest`: 5/5 PASSED
  - `AccountAndBeneficiaryMockMvcTest`: 1/1 PASSED (comprehensive multi-user flow)
- **Total Tests Run:** 10 tests run, 0 failures, 0 errors, 0 skipped. `BUILD SUCCESS`.
- **Full Suite Run:** `.\mvnw.cmd test`
  - Tests run: 71, Failures: 0, Errors: 0, Skipped: 5. `BUILD SUCCESS` in 28.075s.

### 2. Specific Verification Evidence
- **Account Limit Check:**
  - `AccountAndBeneficiaryMockMvcTest`: User A registers with 1 default account (`TPG******0001`), successfully opens a 2nd account (`AccountType.CURRENT`) and 3rd account (`AccountType.SAVINGS`), reaching 3 accounts.
  - Attempting to open a 4th account is rejected with `422 UNPROCESSABLE_ENTITY` and message: `"Maximum limit of 3 accounts reached for this customer."`.
- **Masking Check:**
  - Masked account number matches pattern `^TPG\*+[0-9]{4}$` (e.g. `TPG******1234`).
- **IDOR Protection Checks:**
  - User A requesting User B's account id returns `404 NOT_FOUND` with RFC 7807 ProblemDetail.
  - User A requesting User B's statement returns `404 NOT_FOUND`.
  - User B attempting to delete User A's beneficiary returns `404 NOT_FOUND`.
- **Beneficiary Address Book Checks:**
  - `GET /api/v1/beneficiaries/lookup?accountNumber=...`: Returns masked recipient name (e.g., `Customer Beta` -> `C******* B***`).
  - Adding own account as beneficiary returns `422 UNPROCESSABLE_ENTITY` (`"Cannot add your own account as a beneficiary."`).
  - Adding a duplicate beneficiary returns `409 CONFLICT` (`"This beneficiary account is already registered in your address book."`).
  - Deleting own beneficiary returns `200 OK`.

### 3. Status
**DONE** - All Phase 4 requirements verified and passing.
