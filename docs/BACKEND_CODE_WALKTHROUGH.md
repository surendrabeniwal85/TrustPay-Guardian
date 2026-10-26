# TrustPay Guardian — Backend Code Walkthrough & Architectural Reference

**Document Version:** 1.0.0  
**Target Codebase:** `backend/src/main/java/com/trustpay/guardian/`  
**Classification:** Deep Technical Code Walkthrough  
**Last Updated:** 2026-10-26  

---

## 1. Domain Entities & JPA Mapping

### 1.1 `Account.java`
The core ledger account entity represents customer balances:
```java
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status; // ACTIVE, FROZEN, CLOSED

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // Optimistic locking guard for non-transfer operations
}
```
**Key Design Decisions:**
- `balance` strictly uses `BigDecimal` with `precision = 19, scale = 2`.
- `@Version` provides optimistic locking concurrency protection for metadata updates.
- In-memory available balance is dynamically computed using `TransactionRepository.sumPendingHoldsByAccountId(id)`.

### 1.2 `Transaction.java`
Represents an immutable ledger record of money movement:
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", unique = true, nullable = false, length = 36)
    private String reference; // TXN-XXXXXXXXXXXX

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status; // APPROVED_SIMULATED, PENDING_REVIEW, BLOCKED, REJECTED

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore; // 0 to 100

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 64)
    private String idempotencyKey; // UUID from client header
}
```

---

## 2. Security & Token Infrastructure

### 2.1 `JwtService.java`
Handles HMAC-SHA512 cryptographic signing and claims parsing:
- **Startup Secret Validation:** In `@PostConstruct`, verifies secret key is present and >= 64 characters (512 bits). Fails fast if absent.
- **Claims Architecture:** Injects `sub` (email), `userId`, `role`, `fullName`, `iat`, `exp`, and a cryptographically random `jti` (JWT ID).
- **Expiration:** Access tokens are strictly capped at 15 minutes (`900000ms`).

### 2.2 `RateLimitFilter.java` (Bucket4j)
Performs in-memory token bucket rate limiting on high-risk endpoints:
- **Login Endpoint (`/auth/login`):** 10 requests per minute per IP address.
- **Transfer Endpoint (`/transactions/transfer`):** 20 requests per minute per authenticated user ID.
- **Exceeding Limits:** Returns `HTTP 429 Too Many Requests` with RFC 7807 problem details and `Retry-After: 60` header.

---

## 3. Money Movement Implementation (`TransferService.java`)

`TransferService.java` coordinates atomic multi-account transfers under strict concurrency controls:

### Step 1: Idempotency Key Lookup
```java
Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
if (existing.isPresent()) {
    Transaction tx = existing.get();
    // IDOR Protection: verify requesting customer owns the source account
    if (!tx.getSourceAccount().getUser().getId().equals(currentUser.getId())) {
        throw new ConflictException("Idempotency key conflict detected");
    }
    return mapToResponse(tx);
}
```

### Step 2: Deterministic Ascending-ID Pessimistic Row Locking
```java
// Sort IDs to guarantee global lock acquisition order (eliminates deadlocks)
Long firstLockId = Math.min(sourceAccount.getId(), destAccount.getId());
Long secondLockId = Math.max(sourceAccount.getId(), destAccount.getId());

Account lockedFirst = accountRepository.findByIdForUpdate(firstLockId)
    .orElseThrow(() -> new NotFoundException("Account not found"));
Account lockedSecond = accountRepository.findByIdForUpdate(secondLockId)
    .orElseThrow(() -> new NotFoundException("Account not found"));

Account lockedSource = sourceAccount.getId().equals(firstLockId) ? lockedFirst : lockedSecond;
Account lockedDest = destAccount.getId().equals(firstLockId) ? lockedFirst : lockedSecond;
```

### Step 3: Available Balance Verification
```java
BigDecimal pendingHolds = transactionRepository.sumPendingHoldsByAccountId(lockedSource.getId());
BigDecimal availableBalance = lockedSource.getBalance().subtract(pendingHolds);

if (availableBalance.compareTo(amount) < 0) {
    throw new BusinessRuleException("Insufficient available balance. Pending holds: ₹" + pendingHolds);
}
```

### Step 4: Fraud Engine Scoring & State Transition
- If `score < 40`: Status = `APPROVED_SIMULATED`. Atomically debits `lockedSource.balance` and credits `lockedDest.balance`.
- If `40 <= score < 70`: Status = `PENDING_REVIEW`. Balances remain unchanged; pending hold is activated. Creates `FraudAlert` in triage queue.
- If `score >= 70`: Status = `BLOCKED`. Balances remain unchanged. Creates resolved blocked alert.

---

## 4. Fraud Strategy Engine (`com.trustpay.guardian.fraud.`)

### 4.1 The Strategy Contract (`FraudRule.java`)
```java
public interface FraudRule {
    String code(); // e.g., "R1_HIGH_AMOUNT"
    RuleResult evaluate(TransactionContext context);
}
```

### 4.2 The 7 Rule Strategies
1. **`R1_HighAmountRuleStrategy`:** Flags amounts exceeding threshold (default: ₹50,000.00). Score: +35.
2. **`R2_RapidVelocityRuleStrategy`:** Counts transfers from source account in rolling 5-minute window. If count > 3, Score: +25.
3. **`R3_NewBeneficiaryRuleStrategy`:** Checks if recipient account exists in user's saved beneficiaries or prior transaction history. If new, Score: +20.
4. **`R4_DormantAccountRuleStrategy`:** Checks elapsed days since last transaction. If > 60 days, Score: +20.
5. **`R5_HighBalanceDepletionRuleStrategy`:** Checks ratio `amount / currentBalance`. If > 80% (0.80), Score: +25.
6. **`R6_RoundFigureRuleStrategy`:** Checks if amount is a round multiple of ₹10,000 above ₹25,000 (e.g. ₹30,000, ₹50,000). Score: +15.
7. **`R7_RecentFailedLoginsRuleStrategy`:** Checks failed login count on sender's user record within past 15 minutes. If >= 3, Score: +20.

### 4.3 In-Memory Dynamic Rule Cache (`FraudRuleConfigService.java`)
- Active rule states, threshold amounts, and point weights are loaded into a thread-safe `ConcurrentHashMap`.
- When an administrator updates a rule via `PATCH /api/v1/admin/fraud-rules/{id}`, the cache is invalidated and refreshed atomically.

---

## 5. Analyst & Admin Governance (`FraudAnalystService.java` & `AdminService.java`)

### 5.1 Case Review Decisions
- `RELEASE`:
  - Re-verifies source account is still `ACTIVE` and has sufficient balance.
  - Converts the pending reservation into a permanent ledger debit and recipient credit.
  - Transitions alert status to `RESOLVED_RELEASED`.
  - Dispatches email notification to customer confirming funds release.
- `CONFIRM_BLOCK`:
  - Transitions transaction status to `BLOCKED`.
  - Automatically releases the pending hold reservation.
  - Transitions alert status to `RESOLVED_BLOCKED`.
- **Idempotency Guard:** Attempting to resolve an already-resolved alert returns `HTTP 409 Conflict`.

### 5.2 Append-Only Compliance Auditing (`AuditService.java`)
Every consequential action logs:
- `userId`: ID of initiating user (or `SYSTEM`).
- `action`: Specific `AuditAction` enum (e.g., `ACCOUNT_FREEZE`, `FRAUD_ALERT_RELEASE`, `RULE_UPDATE`).
- `targetType` & `targetId`: Target entity identifier.
- `details`: Formatted JSON payload containing before/after states.
- `ipAddress`: Client IP address extracted from request headers.
- `requestId`: MDC correlation ID.
