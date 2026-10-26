package com.trustpay.guardian.account;

import com.trustpay.guardian.account.dto.AccountResponse;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.common.exception.AccountClosedException;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.common.exception.ResourceNotFoundException;
import com.trustpay.guardian.common.exception.UnauthorizedAccessException;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            SystemSettingRepository systemSettingRepository,
            NotificationService notificationService,
            AuditService auditService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream().map(acc -> {
            BigDecimal available = calculateAvailableBalance(acc);
            return AccountResponse.fromEntity(acc, available);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return getUserAccounts(userId);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to view this account.");
        }

        BigDecimal available = calculateAvailableBalance(account);
        return AccountResponse.fromEntity(account, available);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateAvailableBalance(Account account) {
        BigDecimal pendingOutgoing = transactionRepository.sumPendingOutgoingTransfers(account.getId());
        return account.getBalance().subtract(pendingOutgoing);
    }

    @Transactional
    public AccountResponse deposit(DepositRequest request, Long userId, String ipAddress, String requestId) {
        Account account = accountRepository.findByIdForUpdate(request.getDestinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getDestinationAccountId()));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only deposit funds into your own accounts.");
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException(account.getMaskedAccountNumber());
        }

        BigDecimal maxDeposit = systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("50000.00"));

        if (request.getAmount().compareTo(maxDeposit) > 0) {
            throw new ApiException(
                    String.format("Deposit amount exceeds maximum allowable single deposit of ₹%s", maxDeposit.toPlainString()),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Apply credit
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Record deposit transaction
        String refId = "TXN-DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String idempotencyKey = "DEP-" + UUID.randomUUID();

        Transaction tx = new Transaction(
                refId,
                idempotencyKey,
                null, // Source account is null for deposits
                account,
                request.getAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED_SIMULATED
        );
        tx.setCompletedAt(Instant.now());
        transactionRepository.save(tx);

        notificationService.sendNotification(
                account.getUser(),
                NotificationType.TRANSFER_RECEIVED,
                "Demo Deposit Successful",
                String.format("Successfully deposited ₹%s into account %s. Current balance: ₹%s",
                        request.getAmount().toPlainString(), account.getMaskedAccountNumber(), newBalance.toPlainString())
        );

        auditService.logAction(
                account.getUser(),
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.TRANSACTION,
                refId,
                String.format("{\"event\":\"DEMO_DEPOSIT\",\"amount\":%s,\"account\":\"%s\"}",
                        request.getAmount(), account.getAccountNumber()),
                ipAddress,
                requestId
        );

        BigDecimal available = calculateAvailableBalance(account);
        return AccountResponse.fromEntity(account, available);
    }

    @Transactional
    public Account createDefaultAccount(com.trustpay.guardian.user.User user) {
        BigDecimal openingBalance = systemSettingRepository.findBySettingKey("DEMO_OPENING_BALANCE")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("10000.00"));

        BigDecimal dailyLimit = systemSettingRepository.findBySettingKey("DEFAULT_DAILY_LIMIT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("100000.00"));

        String accountNumber = generateUniqueAccountNumber();
        Account account = new Account(user, accountNumber, AccountType.SAVINGS, openingBalance, dailyLimit);
        return accountRepository.save(account);
    }

    private String generateUniqueAccountNumber() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 10; i++) {
            long number = Math.abs(random.nextLong()) % 1_000_000_000_000L;
            String candidate = String.format("TPG%012d", number);
            if (accountRepository.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "TPG" + System.currentTimeMillis();
    }
}
