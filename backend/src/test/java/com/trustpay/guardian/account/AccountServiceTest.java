package com.trustpay.guardian.account;

import com.trustpay.guardian.account.dto.AccountResponse;
import com.trustpay.guardian.account.dto.CreateAccountRequest;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                userRepository,
                transactionRepository,
                systemSettingRepository,
                accountNumberGenerator,
                notificationService,
                auditService
        );
    }

    @Test
    @DisplayName("Opening an extra account succeeds when customer has fewer than 3 accounts")
    void openAccountSucceedsUnderLimit() {
        User user = new User("John", "john@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);

        when(accountRepository.countByUserId(1L)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("TPG1234567890");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            acc.setId(101L);
            return acc;
        });

        AccountResponse response = accountService.openAccount(1L, new CreateAccountRequest(AccountType.CURRENT));

        assertThat(response).isNotNull();
        assertThat(response.getAccountType()).isEqualTo(AccountType.CURRENT);
        assertThat(response.getMaskedAccountNumber()).isEqualTo("TPG******7890");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Opening a 4th account throws BusinessRuleException (HTTP 422)")
    void openFourthAccountThrowsBusinessRuleException() {
        when(accountRepository.countByUserId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> accountService.openAccount(1L, new CreateAccountRequest(AccountType.SAVINGS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Maximum limit of 3 accounts reached");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requesting another user's account returns 404 NotFoundException (IDOR Protection)")
    void otherUserAccountReturns404() {
        User userA = new User("User A", "a@example.com", "9876543210", "hash", Role.CUSTOMER);
        userA.setId(1L);

        User userB = new User("User B", "b@example.com", "9876543211", "hash", Role.CUSTOMER);
        userB.setId(2L);

        Account accountB = new Account(userB, "TPG9999999999", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("50000.00"));
        accountB.setId(200L);

        when(accountRepository.findById(200L)).thenReturn(Optional.of(accountB));

        assertThatThrownBy(() -> accountService.getAccountDetails(200L, 1L)) // User A requesting Account B
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found with id: 200");
    }

    @Test
    @DisplayName("Available balance accurately subtracts outgoing pending review transfers")
    void availableBalanceSubtractsPendingTransfers() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        Account account = new Account(user, "TPG1000000000", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("50000.00"));
        account.setId(50L);

        when(transactionRepository.sumPendingOutgoingTransfers(50L)).thenReturn(new BigDecimal("3500.00"));

        BigDecimal available = accountService.calculateAvailableBalance(account);

        assertThat(available).isEqualByComparingTo("6500.00");
    }

    @Test
    @DisplayName("Deposit succeeds when account is active and within limit")
    void depositSucceeds() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);
        Account account = new Account(user, "TPG1000000000", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("50000.00"));
        account.setId(50L);

        when(accountRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(account));
        when(systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT"))
                .thenReturn(Optional.of(new SystemSetting("MAX_DEPOSIT_AMOUNT", "50000.00", "Max deposit")));

        com.trustpay.guardian.account.dto.DepositRequest req =
                new com.trustpay.guardian.account.dto.DepositRequest(50L, new BigDecimal("10000.00"));

        AccountResponse resp = accountService.deposit(req, 1L, "127.0.0.1", "REQ-DEP-01");

        assertThat(resp).isNotNull();
        assertThat(resp.getBalance()).isEqualByComparingTo("15000.00");
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any());
        verify(notificationService).sendNotification(eq(user), eq(com.trustpay.guardian.notification.NotificationType.TRANSFER_RECEIVED), anyString(), anyString());
    }

    @Test
    @DisplayName("Deposit fails if amount exceeds MAX_DEPOSIT_AMOUNT")
    void depositFailsWhenExceedsLimit() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);
        Account account = new Account(user, "TPG1000000000", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("50000.00"));
        account.setId(50L);

        when(accountRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(account));
        when(systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT"))
                .thenReturn(Optional.of(new SystemSetting("MAX_DEPOSIT_AMOUNT", "50000.00", "Max deposit")));

        com.trustpay.guardian.account.dto.DepositRequest req =
                new com.trustpay.guardian.account.dto.DepositRequest(50L, new BigDecimal("50001.00"));

        assertThatThrownBy(() -> accountService.deposit(req, 1L, "127.0.0.1", "REQ-DEP-02"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds maximum allowable single deposit");
    }

    @Test
    @DisplayName("Deposit fails if account is FROZEN or CLOSED")
    void depositFailsOnRestrictedAccount() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);
        Account frozenAccount = new Account(user, "TPG1000000000", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("50000.00"));
        frozenAccount.setId(50L);
        frozenAccount.setStatus(AccountStatus.FROZEN);

        when(accountRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(frozenAccount));

        com.trustpay.guardian.account.dto.DepositRequest req =
                new com.trustpay.guardian.account.dto.DepositRequest(50L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> accountService.deposit(req, 1L, "127.0.0.1", "REQ-DEP-03"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("frozen account");
    }

    @Test
    @DisplayName("Statement query for account succeeds with ownership check")
    void getAccountStatementSucceeds() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);
        Account account = new Account(user, "TPG1000000000", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("50000.00"));
        account.setId(50L);

        when(accountRepository.findById(50L)).thenReturn(Optional.of(account));
        when(transactionRepository.findStatementByAccountIdAndDateRange(eq(50L), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        var statement = accountService.getAccountStatement(50L, 1L, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(statement).isNotNull();
    }

    @Test
    @DisplayName("createDefaultAccount provisions savings account with settings parameters")
    void createDefaultAccountProvisionsAccount() {
        User user = new User("User", "u@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);

        when(systemSettingRepository.findBySettingKey("DEMO_OPENING_BALANCE"))
                .thenReturn(Optional.of(new SystemSetting("DEMO_OPENING_BALANCE", "15000.00", "Opening balance")));
        when(systemSettingRepository.findBySettingKey("DEFAULT_DAILY_LIMIT"))
                .thenReturn(Optional.of(new SystemSetting("DEFAULT_DAILY_LIMIT", "75000.00", "Daily limit")));
        when(accountNumberGenerator.generateAccountNumber()).thenReturn("TPG9988776655");
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account created = accountService.createDefaultAccount(user);

        assertThat(created).isNotNull();
        assertThat(created.getBalance()).isEqualByComparingTo("15000.00");
        assertThat(created.getDailyLimit()).isEqualByComparingTo("75000.00");
        assertThat(created.getAccountNumber()).isEqualTo("TPG9988776655");
    }
}
