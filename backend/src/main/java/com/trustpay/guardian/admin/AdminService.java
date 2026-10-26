package com.trustpay.guardian.admin;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountService;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.dto.AccountResponse;
import com.trustpay.guardian.admin.dto.*;
import com.trustpay.guardian.audit.*;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.ResourceNotFoundException;
import com.trustpay.guardian.fraud.*;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertHitRepository fraudAlertHitRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudRuleConfigService fraudRuleConfigService;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            TransactionRepository transactionRepository,
            FraudAlertRepository fraudAlertRepository,
            FraudAlertHitRepository fraudAlertHitRepository,
            FraudRuleRepository fraudRuleRepository,
            FraudRuleConfigService fraudRuleConfigService,
            SystemSettingRepository systemSettingRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudAlertHitRepository = fraudAlertHitRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudRuleConfigService = fraudRuleConfigService;
        this.systemSettingRepository = systemSettingRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalAccounts = accountRepository.count();
        BigDecimal totalBalance = accountRepository.sumTotalBalances();
        long totalTransactions = transactionRepository.count();
        long approvedTransactions = transactionRepository.countByStatus(TransactionStatus.APPROVED_SIMULATED);
        long heldTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING_REVIEW);
        long blockedTransactions = transactionRepository.countByStatus(TransactionStatus.BLOCKED);
        long openAlerts = fraudAlertRepository.countByStatus(AlertStatus.OPEN)
                + fraudAlertRepository.countByStatus(AlertStatus.IN_REVIEW);
        long activeRules = fraudRuleRepository.countByIsEnabledTrue();

        BigDecimal totalVolume = transactionRepository.sumCompletedVolume();
        if (totalVolume == null) {
            totalVolume = BigDecimal.ZERO;
        }

        // Alerts by Status
        Map<String, Long> alertsByStatus = new LinkedHashMap<>();
        for (AlertStatus status : AlertStatus.values()) {
            alertsByStatus.put(status.name(), 0L);
        }
        for (Object[] row : fraudAlertRepository.countGroupByStatus()) {
            if (row[0] != null && row[1] != null) {
                alertsByStatus.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        // Alerts by Rule
        Map<String, Long> alertsByRule = new LinkedHashMap<>();
        for (Object[] row : fraudAlertHitRepository.countGroupByRuleCode()) {
            if (row[0] != null && row[1] != null) {
                alertsByRule.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        // Transactions per day for 14 days
        LocalDate today = LocalDate.now(TIMEZONE);
        Map<String, Long> dailyCounts = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            dailyCounts.put(day.format(DATE_FORMATTER), 0L);
        }

        Instant fourteenDaysAgo = today.minusDays(14).atStartOfDay(TIMEZONE).toInstant();
        List<Instant> recentTxDates = transactionRepository.findCreatedAtSince(fourteenDaysAgo);
        for (Instant txDate : recentTxDates) {
            String dateKey = txDate.atZone(TIMEZONE).toLocalDate().format(DATE_FORMATTER);
            if (dailyCounts.containsKey(dateKey)) {
                dailyCounts.put(dateKey, dailyCounts.get(dateKey) + 1L);
            }
        }

        List<AdminDashboardResponse.DailyTransactionStat> dailyStats = dailyCounts.entrySet().stream()
                .map(e -> new AdminDashboardResponse.DailyTransactionStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new AdminDashboardResponse(
                totalUsers,
                activeUsers,
                totalAccounts,
                totalBalance,
                totalTransactions,
                approvedTransactions,
                heldTransactions,
                blockedTransactions,
                openAlerts,
                activeRules,
                totalVolume,
                alertsByStatus,
                alertsByRule,
                dailyStats
        );
    }

    @Transactional(readOnly = true)
    public Page<UserManagementDto> getUsers(Pageable pageable) {
        return getUsers(null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserManagementDto> getUsers(Role role, UserStatus status, String search, Pageable pageable) {
        Page<User> users = userRepository.findFilteredUsers(role, status, search, pageable);
        return users.map(user -> {
            List<AccountResponse> accounts = accountService.getAccountsByUserId(user.getId());
            return UserManagementDto.fromEntity(user, accounts);
        });
    }

    @Transactional
    public UserManagementDto createUser(CreateUserRequest request, Long adminUserId, String ipAddress, String requestId) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ConflictException("User with email '" + cleanEmail + "' already exists");
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        User user = new User(
                request.getFullName().trim(),
                cleanEmail,
                request.getPhone().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole()
        );

        User savedUser = userRepository.save(user);

        auditService.logAction(
                adminUser,
                AuditAction.USER_ROLE_CHANGE,
                AuditTargetType.USER,
                String.valueOf(savedUser.getId()),
                String.format("{\"action\":\"CREATE_STAFF_USER\",\"role\":\"%s\",\"email\":\"%s\"}",
                        savedUser.getRole(), savedUser.getEmail()),
                ipAddress,
                requestId
        );

        return UserManagementDto.fromEntity(savedUser, List.of());
    }

    @Transactional
    public UserManagementDto updateUserRole(Long targetUserId, Role newRole, Long adminUserId, String ipAddress, String requestId) {
        if (targetUserId.equals(adminUserId)) {
            throw new ApiException("Administrators cannot modify their own role (self-demotion prevented)", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        Role oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);

        auditService.logAction(
                adminUser,
                AuditAction.USER_ROLE_CHANGE,
                AuditTargetType.USER,
                String.valueOf(targetUserId),
                String.format("{\"from\":\"%s\",\"to\":\"%s\",\"targetEmail\":\"%s\"}",
                        oldRole, newRole, targetUser.getEmail()),
                ipAddress,
                requestId
        );

        List<AccountResponse> accounts = accountService.getAccountsByUserId(savedUser.getId());
        return UserManagementDto.fromEntity(savedUser, accounts);
    }

    @Transactional
    public UserManagementDto updateUserStatus(Long targetUserId, UserStatus newStatus, Long adminUserId, String ipAddress, String requestId) {
        if (targetUserId.equals(adminUserId)) {
            throw new ApiException("Administrators cannot suspend or deactivate their own account", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        UserStatus oldStatus = targetUser.getStatus();
        targetUser.setStatus(newStatus);
        if (newStatus == UserStatus.ACTIVE) {
            targetUser.setFailedLoginAttempts(0);
            targetUser.setLockoutUntil(null);
        }
        User savedUser = userRepository.save(targetUser);

        auditService.logAction(
                adminUser,
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.USER,
                String.valueOf(targetUserId),
                String.format("{\"from\":\"%s\",\"to\":\"%s\",\"targetEmail\":\"%s\"}",
                        oldStatus, newStatus, targetUser.getEmail()),
                ipAddress,
                requestId
        );

        List<AccountResponse> accounts = accountService.getAccountsByUserId(savedUser.getId());
        return UserManagementDto.fromEntity(savedUser, accounts);
    }

    @Transactional(readOnly = true)
    public Page<AccountManagementDto> getAccounts(AccountStatus status, String search, Pageable pageable) {
        return accountRepository.findFilteredAccounts(status, search, pageable)
                .map(AccountManagementDto::fromEntity);
    }

    @Transactional
    public AccountManagementDto freezeAccount(Long accountId, Long adminUserId, String ipAddress, String requestId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        account.setStatus(AccountStatus.FROZEN);
        Account savedAccount = accountRepository.save(account);

        // Notify customer
        if (account.getUser() != null) {
            notificationService.sendNotification(
                    account.getUser(),
                    NotificationType.ACCOUNT_FROZEN,
                    "Account Frozen",
                    "Your account " + account.getMaskedAccountNumber() + " has been frozen by administration. Outgoing debit transactions are suspended."
            );
        }

        auditService.logAction(
                adminUser,
                AuditAction.ACCOUNT_FREEZE,
                AuditTargetType.ACCOUNT,
                String.valueOf(accountId),
                String.format("{\"accountNumber\":\"%s\",\"status\":\"FROZEN\"}", account.getAccountNumber()),
                ipAddress,
                requestId
        );

        return AccountManagementDto.fromEntity(savedAccount);
    }

    @Transactional
    public AccountManagementDto unfreezeAccount(Long accountId, Long adminUserId, String ipAddress, String requestId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        account.setStatus(AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        // Notify customer
        if (account.getUser() != null) {
            notificationService.sendNotification(
                    account.getUser(),
                    NotificationType.ACCOUNT_UNFROZEN,
                    "Account Reactivated",
                    "Your account " + account.getMaskedAccountNumber() + " has been unfrozen. Normal operations are restored."
            );
        }

        auditService.logAction(
                adminUser,
                AuditAction.ACCOUNT_UNFREEZE,
                AuditTargetType.ACCOUNT,
                String.valueOf(accountId),
                String.format("{\"accountNumber\":\"%s\",\"status\":\"ACTIVE\"}", account.getAccountNumber()),
                ipAddress,
                requestId
        );

        return AccountManagementDto.fromEntity(savedAccount);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            TransactionType type,
            TransactionStatus status,
            Instant startDate,
            Instant endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            Pageable pageable) {
        return transactionRepository.findAllFilteredTransactions(
                type, status, startDate, endDate, minAmount, maxAmount, search, pageable
        ).map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<FraudRule> getFraudRules() {
        return fraudRuleRepository.findAll();
    }

    @Transactional
    public FraudRule updateFraudRule(Long ruleId, UpdateRuleRequest request, Long adminUserId, String ipAddress, String requestId) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudRule", "id", ruleId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        int oldPoints = rule.getScorePoints();
        boolean oldEnabled = rule.isEnabled();

        rule.setScorePoints(request.getScorePoints());
        rule.setEnabled(request.getIsEnabled());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            rule.setDescription(request.getDescription().trim());
        }

        FraudRule savedRule = fraudRuleRepository.save(rule);

        // Evict in-memory cache to guarantee next evaluation reflects update immediately
        fraudRuleConfigService.clearCache();

        auditService.logAction(
                adminUser,
                AuditAction.FRAUD_RULE_UPDATE,
                AuditTargetType.FRAUD_RULE,
                String.valueOf(ruleId),
                String.format("{\"ruleCode\":\"%s\",\"points\":{\"from\":%d,\"to\":%d},\"enabled\":{\"from\":%b,\"to\":%b}}",
                        rule.getRuleCode(), oldPoints, rule.getScorePoints(), oldEnabled, rule.isEnabled()),
                ipAddress,
                requestId
        );

        return savedRule;
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings() {
        return systemSettingRepository.findAll();
    }

    @Transactional
    public SystemSetting updateSetting(Long settingId, UpdateSettingRequest request, Long adminUserId, String ipAddress, String requestId) {
        SystemSetting setting = systemSettingRepository.findById(settingId)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting", "id", settingId));

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(request.getSettingValue().trim());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            setting.setDescription(request.getDescription().trim());
        }
        setting.setUpdatedBy(adminUser);

        SystemSetting savedSetting = systemSettingRepository.save(setting);

        // Evict in-memory cache to guarantee next evaluation reflects threshold update immediately
        fraudRuleConfigService.clearCache();

        auditService.logAction(
                adminUser,
                AuditAction.SYSTEM_SETTING_UPDATE,
                AuditTargetType.SYSTEM_SETTING,
                String.valueOf(settingId),
                String.format("{\"key\":\"%s\",\"value\":{\"from\":\"%s\",\"to\":\"%s\"}}",
                        setting.getSettingKey(), oldValue, setting.getSettingValue()),
                ipAddress,
                requestId
        );

        return savedSetting;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return getAuditLogs(null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(AuditAction action, AuditTargetType targetType, Pageable pageable) {
        return auditLogRepository.findFilteredAuditLogs(action, targetType, pageable).map(AuditLogResponse::fromEntity);
    }
}
