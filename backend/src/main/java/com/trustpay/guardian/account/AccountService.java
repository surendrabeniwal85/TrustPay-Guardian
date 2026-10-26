package com.trustpay.guardian.account;

import com.trustpay.guardian.account.dto.AccountResponse;
import com.trustpay.guardian.account.dto.CreateAccountRequest;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.common.web.PageResponse;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            SystemSettingRepository systemSettingRepository,
            AccountNumberGenerator accountNumberGenerator,
            NotificationService notificationService,
            AuditService auditService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.accountNumberGenerator = accountNumberGenerator;
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
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        // Ownership enforcement: return 404 (not 403) to prevent ID enumeration
        if (!account.getUser().getId().equals(userId)) {
            throw new NotFoundException("Account not found with id: " + accountId);
        }

        BigDecimal available = calculateAvailableBalance(account);
        return AccountResponse.fromEntity(account, available);
    }

    @Transactional
    public AccountResponse openAccount(Long userId, CreateAccountRequest request) {
        long currentCount = accountRepository.countByUserId(userId);
        if (currentCount >= 3) {
            throw new BusinessRuleException("Maximum limit of 3 accounts reached for this customer.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        AccountType type = (request != null && request.getAccountType() != null)
                ? request.getAccountType()
                : AccountType.SAVINGS;

        BigDecimal dailyLimit = systemSettingRepository.findBySettingKey("DEFAULT_DAILY_LIMIT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("100000.00"));

        String accountNumber = accountNumberGenerator.generateAccountNumber();
        Account account = new Account(user, accountNumber, type, new BigDecimal("0.00"), dailyLimit);
        Account saved = accountRepository.save(account);

        auditService.logAction(
                user,
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.ACCOUNT,
                String.valueOf(saved.getId()),
                String.format("{\"event\":\"ACCOUNT_OPENED\",\"type\":\"%s\",\"accountNumber\":\"%s\"}",
                        type, saved.getAccountNumber()),
                "SYSTEM",
                "INTERNAL"
        );

        return AccountResponse.fromEntity(saved, saved.getBalance());
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAccountStatement(
            Long accountId,
            Long userId,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        // IDOR protection: return 404
        if (!account.getUser().getId().equals(userId)) {
            throw new NotFoundException("Account not found with id: " + accountId);
        }

        Page<Transaction> page = transactionRepository.findStatementByAccountIdAndDateRange(
                accountId, startDate, endDate, pageable);

        return PageResponse.from(page.map(TransactionResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateAvailableBalance(Account account) {
        BigDecimal pendingOutgoing = transactionRepository.sumPendingOutgoingTransfers(account.getId());
        return account.getBalance().subtract(pendingOutgoing != null ? pendingOutgoing : BigDecimal.ZERO);
    }

    @Transactional
    public AccountResponse deposit(DepositRequest request, Long userId, String ipAddress, String requestId) {
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

        BigDecimal maxDeposit = systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("50000.00"));

        if (request.getAmount().compareTo(maxDeposit) > 0) {
            throw new BusinessRuleException(
                    String.format("Deposit amount exceeds maximum allowable single deposit of ₹%s", maxDeposit.toPlainString())
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
    public Account createDefaultAccount(User user) {
        BigDecimal openingBalance = systemSettingRepository.findBySettingKey("DEMO_OPENING_BALANCE")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("10000.00"));

        BigDecimal dailyLimit = systemSettingRepository.findBySettingKey("DEFAULT_DAILY_LIMIT")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(new BigDecimal("100000.00"));

        String accountNumber = accountNumberGenerator.generateAccountNumber();
        Account account = new Account(user, accountNumber, AccountType.SAVINGS, openingBalance, dailyLimit);
        return accountRepository.save(account);
    }
}
