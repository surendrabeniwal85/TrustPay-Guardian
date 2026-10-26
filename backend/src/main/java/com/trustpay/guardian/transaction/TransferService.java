package com.trustpay.guardian.transaction;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.beneficiary.BeneficiaryRepository;
import com.trustpay.guardian.common.web.PageResponse;
import com.trustpay.guardian.common.exception.*;
import com.trustpay.guardian.fraud.*;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Kolkata");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final RiskAssessor riskAssessor;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertHitRepository fraudAlertHitRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TransactionReferenceGenerator referenceGenerator;
    private final TransactionTemplate transactionTemplate;

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository,
            RiskAssessor riskAssessor,
            FraudAlertRepository fraudAlertRepository,
            FraudAlertHitRepository fraudAlertHitRepository,
            SystemSettingRepository systemSettingRepository,
            NotificationService notificationService,
            AuditService auditService,
            TransactionReferenceGenerator referenceGenerator,
            TransactionTemplate transactionTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.riskAssessor = riskAssessor;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudAlertHitRepository = fraudAlertHitRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.referenceGenerator = referenceGenerator;
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionResponse initiateTransfer(
            TransferRequest request,
            String idempotencyKey,
            Long userId,
            String ipAddress,
            String requestId) {

        int maxAttempts = 2; // Bounded retry: 1 attempt + 1 retry on deadlock/lock timeout
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return transactionTemplate.execute(status ->
                        doTransfer(request, idempotencyKey, userId, ipAddress, requestId)
                );
            } catch (org.springframework.dao.CannotAcquireLockException |
                     org.hibernate.exception.LockAcquisitionException |
                     org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
                if (attempt < maxAttempts) {
                    log.warn("Lock acquisition timeout/deadlock on attempt {}. Retrying once with backoff...", attempt);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                } else {
                    log.error("Lock acquisition failed after bounded retry.", ex);
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unexpected transfer execution state.");
    }

    private TransactionResponse doTransfer(
            TransferRequest request,
            String idempotencyKey,
            Long userId,
            String ipAddress,
            String requestId) {

        // 1. Idempotency Check (ADR-005)
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(effectiveKey);
        if (existingTx.isPresent()) {
            Transaction tx = existingTx.get();
            // Verify tenant ownership
            if (tx.getSourceAccount() != null && !tx.getSourceAccount().getUser().getId().equals(userId)) {
                log.warn("Idempotency key collision/theft attempt for key [{}]", effectiveKey);
                throw new ConflictException("Transaction with this idempotency key already exists for another account.");
            }
            log.info("Returning cached idempotent response for transaction [{}]", tx.getReferenceId());
            return TransactionResponse.fromEntity(tx);
        }

        // 2. Validate Amount (min 1.00, max MAX_TRANSFER_AMOUNT, max 2 decimals)
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessRuleException("Transfer amount must be at least ₹1.00.");
        }
        if (request.getAmount().scale() > 2) {
            throw new BusinessRuleException("Transfer amount cannot have more than 2 decimal places.");
        }

        BigDecimal maxTransfer = systemSettingRepository.findBySettingKey("MAX_TRANSFER_AMOUNT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("200000.00"));

        if (request.getAmount().compareTo(maxTransfer) > 0) {
            throw new BusinessRuleException(
                    String.format("Transfer amount of ₹%s exceeds maximum allowable single transfer limit of ₹%s",
                            request.getAmount().toPlainString(), maxTransfer.toPlainString())
            );
        }

        // 3. Resolve Destination Account & Beneficiary Ownership
        String destinationAccountNumber;
        Optional<Beneficiary> optBeneficiary = Optional.empty();

        if (request.getBeneficiaryId() != null) {
            Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                    .orElseThrow(() -> new BusinessRuleException("Beneficiary not found with ID: " + request.getBeneficiaryId()));
            if (!beneficiary.getUser().getId().equals(userId)) {
                throw new BusinessRuleException("Beneficiary does not belong to the authenticated user.");
            }
            destinationAccountNumber = beneficiary.getRecipientAccount().getAccountNumber();
            optBeneficiary = Optional.of(beneficiary);
        } else if (request.getDestinationAccountNumber() != null && !request.getDestinationAccountNumber().isBlank()) {
            destinationAccountNumber = request.getDestinationAccountNumber().trim();
        } else {
            throw new BusinessRuleException("Destination account number or beneficiary ID is required.");
        }

        // 4. Resolve IDs without preloading entities into Hibernate session
        Long srcId = request.getSourceAccountId();
        Long dstId;
        if (optBeneficiary.isPresent()) {
            dstId = optBeneficiary.get().getRecipientAccount().getId();
        } else {
            dstId = accountRepository.findIdByAccountNumber(destinationAccountNumber)
                    .orElseThrow(() -> new NotFoundException("Destination account not found with account number: " + destinationAccountNumber));
        }

        if (srcId.equals(dstId)) {
            throw new BusinessRuleException("Source and destination accounts must be distinct.");
        }

        // 5. Deterministic Ascending-ID Row Lock Ordering (Deadlock Guard)
        Long firstId = Math.min(srcId, dstId);
        Long secondId = Math.max(srcId, dstId);

        Account firstLocked = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + firstId));
        Account secondLocked = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + secondId));

        Account lockedSrc = srcId.equals(firstId) ? firstLocked : secondLocked;
        Account lockedDst = dstId.equals(firstId) ? firstLocked : secondLocked;

        // Verify source ownership
        if (!lockedSrc.getUser().getId().equals(userId)) {
            throw new NotFoundException("Source account not found with id: " + srcId);
        }

        User sender = lockedSrc.getUser();

        if (optBeneficiary.isEmpty()) {
            optBeneficiary = beneficiaryRepository.findByUserIdAndRecipientAccountId(userId, lockedDst.getId());
        }

        // 6. Post-Lock Status Checks (Strict 422 BusinessRuleException)
        if (lockedSrc.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(lockedSrc.getMaskedAccountNumber());
        }
        if (lockedSrc.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(lockedSrc.getMaskedAccountNumber());
        }
        if (lockedDst.getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException(lockedDst.getMaskedAccountNumber());
        }
        if (lockedDst.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(lockedDst.getMaskedAccountNumber());
        }

        // 7. Available Balance Check (Balance - Active Pending Holds)
        BigDecimal pendingHolds = transactionRepository.sumPendingOutgoingTransfers(lockedSrc.getId());
        BigDecimal availableBalance = lockedSrc.getBalance().subtract(pendingHolds != null ? pendingHolds : BigDecimal.ZERO);

        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(availableBalance, request.getAmount());
        }

        // 8. Daily Limit Check
        Instant startOfDay = LocalDate.now(TIMEZONE).atStartOfDay(TIMEZONE).toInstant();
        BigDecimal todayOutgoing = transactionRepository.sumOutgoingTransfersSince(lockedSrc.getId(), startOfDay);
        BigDecimal safeTodayOutgoing = todayOutgoing != null ? todayOutgoing : BigDecimal.ZERO;
        BigDecimal projectedDailyTotal = safeTodayOutgoing.add(request.getAmount());

        if (projectedDailyTotal.compareTo(lockedSrc.getDailyLimit()) > 0) {
            BigDecimal remainingLimit = lockedSrc.getDailyLimit().subtract(safeTodayOutgoing).max(BigDecimal.ZERO);
            throw new DailyLimitExceededException(request.getAmount(), remainingLimit);
        }

        // 9. Assemble Context and Assess Risk via RiskAssessor
        long recent10MinCount = transactionRepository.countRecentTransfers(
                lockedSrc.getId(), Instant.now().minus(10, ChronoUnit.MINUTES));

        BigDecimal thirtyDayAvg = transactionRepository.findThirtyDayAverage(
                lockedSrc.getId(), Instant.now().minus(30, ChronoUnit.DAYS));

        long thirtyDayCount = transactionRepository.countThirtyDayTransfers(
                lockedSrc.getId(), Instant.now().minus(30, ChronoUnit.DAYS));

        TransactionContext riskContext = new TransactionContext(
                lockedSrc,
                lockedDst,
                request.getAmount(),
                sender,
                Instant.now(),
                optBeneficiary,
                recent10MinCount,
                safeTodayOutgoing,
                thirtyDayAvg != null ? thirtyDayAvg : BigDecimal.ZERO,
                thirtyDayCount,
                sender.getFailedLoginAttempts()
        );

        RiskDecision evaluation = riskAssessor.assess(riskContext);

        // 10. Persist Transaction and Execute State Transition
        String referenceId = referenceGenerator.generateReferenceId();
        Transaction tx = new Transaction(
                referenceId,
                effectiveKey,
                lockedSrc,
                lockedDst,
                request.getAmount(),
                TransactionType.TRANSFER,
                evaluation.getProposedStatus()
        );
        tx.setRiskScore(evaluation.getTotalRiskScore());

        Instant now = Instant.now();

        try {
            if (evaluation.getProposedStatus() == TransactionStatus.APPROVED_SIMULATED) {
                // Atomic debit & credit
                lockedSrc.setBalance(lockedSrc.getBalance().subtract(request.getAmount()));
                lockedDst.setBalance(lockedDst.getBalance().add(request.getAmount()));

                accountRepository.save(lockedSrc);
                accountRepository.save(lockedDst);

                tx.setCompletedAt(now);
                Transaction savedTx = transactionRepository.save(tx);

                // Notifications
                notificationService.sendNotification(
                        sender,
                        NotificationType.TRANSFER_SENT,
                        "Transfer Completed",
                        String.format("Transferred ₹%s to %s (%s). Reference: %s",
                                request.getAmount().toPlainString(),
                                lockedDst.getUser().getFullName(),
                                lockedDst.getMaskedAccountNumber(),
                                referenceId)
                );

                notificationService.sendNotification(
                        lockedDst.getUser(),
                        NotificationType.TRANSFER_RECEIVED,
                        "Funds Received",
                        String.format("Received ₹%s from %s (%s). Reference: %s",
                                request.getAmount().toPlainString(),
                                sender.getFullName(),
                                lockedSrc.getMaskedAccountNumber(),
                                referenceId)
                );

                auditService.logAction(
                        sender,
                        AuditAction.USER_STATUS_CHANGE,
                        AuditTargetType.TRANSACTION,
                        referenceId,
                        String.format("{\"event\":\"TRANSFER_APPROVED\",\"amount\":%s,\"riskScore\":%d}",
                                request.getAmount(), evaluation.getTotalRiskScore()),
                        ipAddress,
                        requestId
                );

                return TransactionResponse.fromEntity(savedTx);

            } else if (evaluation.getProposedStatus() == TransactionStatus.PENDING_REVIEW) {
                // Balances held dynamically via pendingHolds calculation
                Transaction savedTx = transactionRepository.save(tx);

                // Create open fraud alert
                FraudAlert alert = new FraudAlert(
                        savedTx,
                        evaluation.getTotalRiskScore(),
                        evaluation.getRiskLevel(),
                        AlertStatus.OPEN
                );
                FraudAlert savedAlert = fraudAlertRepository.save(alert);

                for (FraudRuleHitResult hit : evaluation.getHits()) {
                    FraudAlertHit alertHit = new FraudAlertHit(
                            savedAlert,
                            hit.getRuleCode(),
                            hit.getPoints(),
                            hit.getExplanation()
                    );
                    fraudAlertHitRepository.save(alertHit);
                }

                notificationService.sendNotification(
                        sender,
                        NotificationType.TRANSFER_HELD,
                        "Transfer Held for Security Review",
                        String.format("Your transfer of ₹%s (Ref: %s) triggered fraud risk checks (Score: %d/100) and is under review by our fraud analysis team. Funds are temporarily reserved.",
                                request.getAmount().toPlainString(), referenceId, evaluation.getTotalRiskScore())
                );

                auditService.logAction(
                        sender,
                        AuditAction.ALERT_OPENED,
                        AuditTargetType.FRAUD_ALERT,
                        String.valueOf(savedAlert.getId()),
                        String.format("{\"event\":\"ALERT_OPENED\",\"referenceId\":\"%s\",\"riskScore\":%d}",
                                referenceId, evaluation.getTotalRiskScore()),
                        ipAddress,
                        requestId
                );

                return TransactionResponse.fromEntity(savedTx);

            } else {
                // BLOCKED
                tx.setFailureReason(String.format("Automatically blocked by TrustPay Guardian Risk Engine (Score: %d/100, High Risk)", evaluation.getTotalRiskScore()));
                Transaction savedTx = transactionRepository.save(tx);

                // Create blocked fraud alert
                FraudAlert alert = new FraudAlert(
                        savedTx,
                        evaluation.getTotalRiskScore(),
                        evaluation.getRiskLevel(),
                        AlertStatus.RESOLVED_BLOCKED
                );
                alert.setResolvedAt(now);
                alert.setDecisionNotes("Automatically blocked by deterministic rule engine exceeding block threshold.");
                FraudAlert savedAlert = fraudAlertRepository.save(alert);

                for (FraudRuleHitResult hit : evaluation.getHits()) {
                    FraudAlertHit alertHit = new FraudAlertHit(
                            savedAlert,
                            hit.getRuleCode(),
                            hit.getPoints(),
                            hit.getExplanation()
                    );
                    fraudAlertHitRepository.save(alertHit);
                }

                notificationService.sendNotification(
                        sender,
                        NotificationType.TRANSFER_BLOCKED,
                        "Transfer Automatically Blocked",
                        String.format("Your transfer of ₹%s (Ref: %s) was blocked due to critical risk factors (Score: %d/100). No funds were deducted.",
                                request.getAmount().toPlainString(), referenceId, evaluation.getTotalRiskScore())
                );

                auditService.logAction(
                        sender,
                        AuditAction.USER_STATUS_CHANGE,
                        AuditTargetType.TRANSACTION,
                        referenceId,
                        String.format("{\"event\":\"TRANSFER_BLOCKED\",\"riskScore\":%d}", evaluation.getTotalRiskScore()),
                        ipAddress,
                        requestId
                );

                return TransactionResponse.fromEntity(savedTx);
            }
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate idempotency key race condition recovery
            Optional<Transaction> racedTx = transactionRepository.findByIdempotencyKey(effectiveKey);
            if (racedTx.isPresent()) {
                log.info("Handled concurrent idempotency collision; returning saved transaction [{}]", racedTx.get().getReferenceId());
                return TransactionResponse.fromEntity(racedTx.get());
            }
            throw ex;
        }
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request, Long userId, String ipAddress, String requestId) {
        Account account = accountRepository.findByIdForUpdate(request.getDestinationAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + request.getDestinationAccountId()));

        // Ownership enforcement: return 404
        if (!account.getUser().getId().equals(userId)) {
            throw new NotFoundException("Account not found with id: " + request.getDestinationAccountId());
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Cannot deposit into a closed account.");
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new BusinessRuleException("Cannot deposit into a frozen account. A frozen account cannot receive funds.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessRuleException("Deposit amount must be at least ₹1.00.");
        }
        if (request.getAmount().scale() > 2) {
            throw new BusinessRuleException("Deposit amount cannot have more than 2 decimal places.");
        }

        BigDecimal maxDeposit = systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("50000.00"));

        if (request.getAmount().compareTo(maxDeposit) > 0) {
            throw new BusinessRuleException(
                    String.format("Deposit amount exceeds maximum allowable single deposit of ₹%s", maxDeposit.toPlainString())
            );
        }

        // Credit balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        String referenceId = referenceGenerator.generateReferenceId();
        String idempotencyKey = "DEP-" + UUID.randomUUID();

        Transaction tx = new Transaction(
                referenceId,
                idempotencyKey,
                null,
                account,
                request.getAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED_SIMULATED
        );
        tx.setCompletedAt(Instant.now());
        Transaction savedTx = transactionRepository.save(tx);

        notificationService.sendNotification(
                account.getUser(),
                NotificationType.TRANSFER_RECEIVED,
                "Deposit Successful",
                String.format("Demo funds of ₹%s credited to account %s. Reference: %s",
                        request.getAmount().toPlainString(), account.getMaskedAccountNumber(), referenceId)
        );

        auditService.logAction(
                account.getUser(),
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.TRANSACTION,
                referenceId,
                String.format("{\"event\":\"DEMO_DEPOSIT\",\"amount\":%s}", request.getAmount().toPlainString()),
                ipAddress,
                requestId
        );

        return TransactionResponse.fromEntity(savedTx);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getFilteredTransactions(
            Long userId,
            TransactionType type,
            TransactionStatus status,
            Instant startDate,
            Instant endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            Pageable pageable) {

        Page<Transaction> page = transactionRepository.findFilteredTransactionsForUser(
                userId, type, status, startDate, endDate, minAmount, maxAmount, search, pageable);

        return PageResponse.from(page.map(TransactionResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceId, Long userId, boolean isStaff) {
        Transaction tx = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new NotFoundException("Transaction not found with reference: " + referenceId));

        boolean isSourceOwner = tx.getSourceAccount() != null && tx.getSourceAccount().getUser().getId().equals(userId);
        boolean isDestOwner = tx.getDestinationAccount() != null && tx.getDestinationAccount().getUser().getId().equals(userId);

        if (!isSourceOwner && !isDestOwner && !isStaff) {
            // IDOR safe: return 404 to avoid transaction enumeration
            throw new NotFoundException("Transaction not found with reference: " + referenceId);
        }

        return TransactionResponse.fromEntity(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new NotFoundException("Account not found with id: " + accountId);
        }

        return transactionRepository.findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactionsPaged(Long accountId, Long userId, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new NotFoundException("Account not found with id: " + accountId);
        }

        return transactionRepository.findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(accountId, accountId, pageable)
                .map(TransactionResponse::fromEntity);
    }
}
