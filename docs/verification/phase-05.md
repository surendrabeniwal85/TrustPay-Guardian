# Phase 5 Verification: Transfers, Deposits & Money Movement

**Date:** 2026-10-26  
**Environment:** Java 25.0.1 LTS, Spring Boot 4.1.1, H2 In-Memory (MySQL Mode)  
**Branch:** `feature/phase-05-transfers`  

---

## 1. Scope & Execution Plan

- **Risk Assessment Abstraction:** Introduced `RiskAssessor` interface and `DefaultRiskAssessor` returning low-risk decision (`TODO: Phase 6 - Wire full strategy-based fraud engine`).
- **Transfer Endpoint (`POST /api/v1/transactions/transfer`):**
  - Mandatory `Idempotency-Key` (UUID) validation header; duplicate requests return original reference with no extra debit.
  - Validations: Amount scale <= 2, min ₹1.00, max `MAX_TRANSFER_AMOUNT`, beneficiary ownership check.
  - Deterministic ascending-ID row lock ordering (`Math.min(srcId, dstId)` and `Math.max(srcId, dstId)`) via `findByIdForUpdate` with no entity pre-loading to prevent version conflicts.
  - Post-lock checks: FROZEN/CLOSED accounts reject with HTTP 422 (`AccountFrozenException` / `AccountClosedException`).
  - Available balance check (balance minus pending holds) and daily limit check (HTTP 422 `InsufficientFundsException` / `DailyLimitExceededException`).
  - Reference ID generator: `TXN-` + 12 random uppercase alphanumerics (`TransactionReferenceGenerator`).
- **Deposit Endpoint (`POST /api/v1/transactions/deposit`):**
  - Validates `MAX_DEPOSIT_AMOUNT` (₹50,000.00), min ₹1.00, rejects frozen/closed accounts.
  - Updates balance, creates `DEPOSIT` transaction record (`APPROVED_SIMULATED`), sends in-app notification, and logs audit.
- **Transactions Query & History (`GET /api/v1/transactions`):**
  - Filterable by type, status, date range, min/max amount, reference search, with pagination.
- **Transaction Reference Detail (`GET /api/v1/transactions/{reference}`):**
  - Scoped to transaction parties (sender/receiver or staff), returning HTTP 404 to non-parties for IDOR protection.
- **Resilience:**
  - Bounded retry (1 retry with backoff) on `CannotAcquireLockException` / `LockAcquisitionException` / `ObjectOptimisticLockingFailureException`.
  - Concurrent duplicate idempotency collision recovery via database unique constraint.

---

## 2. Automated Test Evidence

### A. Full Test Suite Run (`./mvnw test`)
- **Command:** `./mvnw test`
- **Result:** `BUILD SUCCESS` in 21.469 s
- **Total Tests Run:** 84
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 5 (Testcontainers MySQL integration tests awaiting host Docker daemon)

### B. 20-Thread Concurrency Test (`TransferConcurrencyIntegrationTest`)
- **Command:** `./mvnw test -Dtest=TransferConcurrencyIntegrationTest`
- **Tests Executed:**
  - `twentyParallelTransfersConserveMoneyAndPreventOverdrawing`: 20 threads simultaneously transferring ₹1,000 from ₹10,000 balance.
  - `repeatedConcurrencyStressTest`: 5 repeated runs of the 20-thread scenario.
- **Verification Criteria:**
  - Zero accounts overdrawn (`finalSrc.getBalance() == 0.00`).
  - Exactly 10 successful transfers and 10 rejected with `InsufficientFundsException`.
  - Recipient final balance = ₹15,000.00 (₹5,000 initial + 10 * ₹1,000).
  - Total system money invariant preserved: `finalSrc + finalDst == ₹15,000.00`.
  - Zero deadlocks or race conditions.

### C. Controller & MockMvc Test Suite (`TransactionControllerMockMvcTest`)
- **Command:** `./mvnw test -Dtest=TransactionControllerMockMvcTest`
- **Tests Executed:**
  - `transferLifecycleAndIdempotencyReplay`: Transfer execution and idempotent replay returning identical reference without double debit.
  - `transferRejectsMissingOrInvalidIdempotencyKey`: Rejection with HTTP 422 if `Idempotency-Key` is missing or not a valid UUID.
  - `transferToFrozenAccountFailsWith422`: Immediate 422 response with zero balance changes.
  - `transferExceedingBalanceFailsWith422`: HTTP 422 ProblemDetail on insufficient funds.
  - `transferWithInvalidScaleFailsWith422`: HTTP 422 ProblemDetail on amount scale > 2.
  - `depositLifecycleSucceeds`: Deposit credits balance and returns type `DEPOSIT`.
  - `listAndReferenceLookupIdorEnforcement`: Query pagination works for owner; unauthorized party receives 404 Not Found.

---

## 3. Phase Status

- **Status:** **DONE**
