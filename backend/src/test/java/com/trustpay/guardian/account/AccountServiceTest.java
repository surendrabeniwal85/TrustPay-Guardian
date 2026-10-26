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
}
