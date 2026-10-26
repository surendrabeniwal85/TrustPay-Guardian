package com.trustpay.guardian.transaction;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.beneficiary.BeneficiaryRepository;
import com.trustpay.guardian.common.exception.*;
import com.trustpay.guardian.fraud.*;
import com.trustpay.guardian.notification.EmailService;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceUnitTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private RiskAssessor riskAssessor;
    @Mock private FraudAlertRepository fraudAlertRepository;
    @Mock private FraudAlertHitRepository fraudAlertHitRepository;
    @Mock private SystemSettingRepository systemSettingRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private TransactionReferenceGenerator referenceGenerator;
    @Mock private TransactionTemplate transactionTemplate;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(
                accountRepository,
                transactionRepository,
                beneficiaryRepository,
                userRepository,
                riskAssessor,
                fraudAlertRepository,
                fraudAlertHitRepository,
                systemSettingRepository,
                notificationService,
                emailService,
                auditService,
                referenceGenerator,
                transactionTemplate
        );

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private User createUser(Long id, String name, String email) {
        User u = new User(name, email, "9876543210", "hash", Role.CUSTOMER);
        u.setId(id);
        return u;
    }

    private Account createAccount(Long id, User user, String accNo, BigDecimal balance, BigDecimal limit) {
        Account a = new Account(user, accNo, AccountType.SAVINGS, balance, limit);
        a.setId(id);
        return a;
    }

    @Test
    @DisplayName("Idempotency returns existing transaction for same user")
    void idempotencyReturnsExisting() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        Transaction existing = new Transaction("TX-EXISTING", "IDEM-KEY", src, null, BigDecimal.valueOf(100), TransactionType.TRANSFER, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);

        when(transactionRepository.findByIdempotencyKey("IDEM-KEY")).thenReturn(Optional.of(existing));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", BigDecimal.valueOf(100), "Rent");
        var resp = transferService.initiateTransfer(req, "IDEM-KEY", 1L, "127.0.0.1", "REQ-1");

        assertThat(resp.getReferenceId()).isEqualTo("TX-EXISTING");
    }

    @Test
    @DisplayName("Idempotency collision from another user throws ConflictException (409)")
    void idempotencyCollisionThrowsConflict() {
        User userOther = createUser(2L, "Bob", "bob@example.com");
        Account srcOther = createAccount(20L, userOther, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        Transaction existing = new Transaction("TX-EXISTING", "IDEM-KEY", srcOther, null, BigDecimal.valueOf(100), TransactionType.TRANSFER, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);

        when(transactionRepository.findByIdempotencyKey("IDEM-KEY")).thenReturn(Optional.of(existing));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", BigDecimal.valueOf(100), "Rent");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "IDEM-KEY", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Invalid amount scale > 2 throws BusinessRuleException")
    void invalidAmountScaleThrows() {
        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("100.123"), "Rent");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("decimal places");
    }

    @Test
    @DisplayName("Amount exceeding maximum transfer limit throws BusinessRuleException")
    void amountExceedsMaxTransferThrows() {
        when(systemSettingRepository.findBySettingKey("MAX_TRANSFER_AMOUNT"))
                .thenReturn(Optional.of(new SystemSetting("MAX_TRANSFER_AMOUNT", "1000.00", "desc")));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("2000.00"), "Rent");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds maximum allowable single transfer limit");
    }

    @Test
    @DisplayName("Missing destination account and beneficiary throws BusinessRuleException")
    void missingDestinationThrows() {
        TransferRequest req = new TransferRequest(10L, "   ", new BigDecimal("50.00"), "Rent");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Destination account number or beneficiary ID is required");
    }

    @Test
    @DisplayName("Beneficiary not belonging to user throws BusinessRuleException")
    void foreignBeneficiaryThrows() {
        User userOther = createUser(2L, "Bob", "bob@example.com");
        Account otherAcc = createAccount(20L, userOther, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        Beneficiary b = new Beneficiary(userOther, otherAcc, "Other");
        b.setId(99L);

        when(beneficiaryRepository.findById(99L)).thenReturn(Optional.of(b));

        TransferRequest req = new TransferRequest(10L, 99L, new BigDecimal("50.00"), "Rent");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Beneficiary does not belong to the authenticated user");
    }

    @Test
    @DisplayName("Source and destination accounts must be distinct")
    void sameSourceAndDestinationThrows() {
        when(accountRepository.findIdByAccountNumber("TPG1000000001")).thenReturn(Optional.of(10L));

        TransferRequest req = new TransferRequest(10L, "TPG1000000001", new BigDecimal("50.00"), "Self");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Source and destination accounts must be distinct");
    }

    @Test
    @DisplayName("Frozen destination account throws AccountFrozenException")
    void frozenDestinationThrows() {
        User userA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, userA, "TPG1000000001", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        User userB = createUser(2L, "Bob", "bob@example.com");
        Account dst = createAccount(20L, userB, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        dst.setStatus(AccountStatus.FROZEN);

        when(accountRepository.findIdByAccountNumber("TPG1000000002")).thenReturn(Optional.of(20L));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(dst));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("50.00"), "Gift");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(AccountFrozenException.class);
    }

    @Test
    @DisplayName("Closed source account throws AccountClosedException")
    void closedSourceThrows() {
        User userA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, userA, "TPG1000000001", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        src.setStatus(AccountStatus.CLOSED);

        User userB = createUser(2L, "Bob", "bob@example.com");
        Account dst = createAccount(20L, userB, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        when(accountRepository.findIdByAccountNumber("TPG1000000002")).thenReturn(Optional.of(20L));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(dst));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("50.00"), "Gift");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("Closed destination account throws AccountClosedException")
    void closedDestinationThrows() {
        User userA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, userA, "TPG1000000001", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        User userB = createUser(2L, "Bob", "bob@example.com");
        Account dst = createAccount(20L, userB, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));
        dst.setStatus(AccountStatus.CLOSED);

        when(accountRepository.findIdByAccountNumber("TPG1000000002")).thenReturn(Optional.of(20L));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(dst));

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("50.00"), "Gift");
        assertThatThrownBy(() -> transferService.initiateTransfer(req, "KEY-1", 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(AccountClosedException.class);
    }

    @Test
    @DisplayName("Transfer blocked by automated security controls creates blocked transaction and alert")
    void transferBlockedBySecurity() {
        User userA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, userA, "TPG1000000001", BigDecimal.valueOf(50000), BigDecimal.valueOf(100000));

        User userB = createUser(2L, "Bob", "bob@example.com");
        Account dst = createAccount(20L, userB, "TPG1000000002", BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        when(accountRepository.findIdByAccountNumber("TPG1000000002")).thenReturn(Optional.of(20L));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(dst));
        when(referenceGenerator.generateReferenceId()).thenReturn("TX-BLOCKED-999");

        RiskDecision decision = new RiskDecision(
                85,
                RiskLevel.HIGH,
                com.trustpay.guardian.transaction.TransactionStatus.BLOCKED,
                List.of(new com.trustpay.guardian.fraud.engine.RuleHit("VELOCITY_SPIKE", 85, "Extreme burst")),
                "High risk blocked"
        );
        when(riskAssessor.assess(any())).thenReturn(decision);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        FraudAlert alert = new FraudAlert();
        alert.setId(77L);
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        TransferRequest req = new TransferRequest(10L, "TPG1000000002", new BigDecimal("5000.00"), "Pay");
        var resp = transferService.initiateTransfer(req, "KEY-BLOCKED", 1L, "127.0.0.1", "REQ-1");

        assertThat(resp.getStatus()).isEqualTo(com.trustpay.guardian.transaction.TransactionStatus.BLOCKED);
        verify(emailService, times(1)).sendEmail(eq("alice@example.com"), contains("Transfer Blocked"), anyString());
    }

    @Test
    @DisplayName("Deposit into frozen account throws BusinessRuleException")
    void depositIntoFrozenThrows() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account account = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));
        account.setStatus(AccountStatus.FROZEN);

        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        DepositRequest req = new DepositRequest(10L, BigDecimal.valueOf(100));
        assertThatThrownBy(() -> transferService.deposit(req, 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot deposit into a frozen account");
    }

    @Test
    @DisplayName("Deposit into closed account throws BusinessRuleException")
    void depositIntoClosedThrows() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account account = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));
        account.setStatus(AccountStatus.CLOSED);

        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        DepositRequest req = new DepositRequest(10L, BigDecimal.valueOf(100));
        assertThatThrownBy(() -> transferService.deposit(req, 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot deposit into a closed account");
    }

    @Test
    @DisplayName("Deposit exceeding maximum allowable amount throws BusinessRuleException")
    void depositExceedingMaxThrows() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account account = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(systemSettingRepository.findBySettingKey("MAX_DEPOSIT_AMOUNT"))
                .thenReturn(Optional.of(new SystemSetting("MAX_DEPOSIT_AMOUNT", "1000.00", "desc")));

        DepositRequest req = new DepositRequest(10L, BigDecimal.valueOf(5000));
        assertThatThrownBy(() -> transferService.deposit(req, 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Deposit amount exceeds maximum allowable single deposit");
    }

    @Test
    @DisplayName("Deposit amount with scale > 2 throws BusinessRuleException")
    void depositScaleInvalidThrows() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account account = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        DepositRequest req = new DepositRequest(10L, new BigDecimal("100.999"));
        assertThatThrownBy(() -> transferService.deposit(req, 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Deposit amount cannot have more than 2 decimal places");
    }

    @Test
    @DisplayName("Deposit amount less than 1.00 throws BusinessRuleException")
    void depositAmountTooSmallThrows() {
        User user = createUser(1L, "Alice", "alice@example.com");
        Account account = createAccount(10L, user, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        DepositRequest req = new DepositRequest(10L, new BigDecimal("0.50"));
        assertThatThrownBy(() -> transferService.deposit(req, 1L, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Deposit amount must be at least ₹1.00");
    }

    @Test
    @DisplayName("Get transaction by reference as non-owner non-staff throws NotFoundException (IDOR)")
    void getTransactionByRefIdorThrows() {
        User ownerA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, ownerA, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        Transaction tx = new Transaction("TX-REF-100", "KEY-1", src, null, BigDecimal.TEN, TransactionType.DEPOSIT, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);
        when(transactionRepository.findByReferenceId("TX-REF-100")).thenReturn(Optional.of(tx));

        // User 99 (neither source, nor destination, not staff)
        assertThatThrownBy(() -> transferService.getTransactionByReference("TX-REF-100", 99L, false))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found with reference: TX-REF-100");
    }

    @Test
    @DisplayName("Get transaction by reference as staff succeeds")
    void getTransactionByRefStaffSucceeds() {
        User ownerA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, ownerA, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        Transaction tx = new Transaction("TX-REF-100", "KEY-1", src, null, BigDecimal.TEN, TransactionType.DEPOSIT, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);
        when(transactionRepository.findByReferenceId("TX-REF-100")).thenReturn(Optional.of(tx));

        var resp = transferService.getTransactionByReference("TX-REF-100", 99L, true);
        assertThat(resp.getReferenceId()).isEqualTo("TX-REF-100");
    }

    @Test
    @DisplayName("Get account transactions for another user's account throws NotFoundException")
    void getAccountTransactionsForeignAccountThrows() {
        User ownerA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, ownerA, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        when(accountRepository.findById(10L)).thenReturn(Optional.of(src));

        // User 2 requests User 1's account transactions
        assertThatThrownBy(() -> transferService.getAccountTransactions(10L, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Get account transactions paged returns paged transactions")
    void getAccountTransactionsPagedSuccess() {
        User ownerA = createUser(1L, "Alice", "alice@example.com");
        Account src = createAccount(10L, ownerA, "TPG1000000001", BigDecimal.valueOf(500), BigDecimal.valueOf(1000));

        when(accountRepository.findById(10L)).thenReturn(Optional.of(src));
        Transaction tx = new Transaction("TX-PAGED-1", "KEY-1", src, null, BigDecimal.TEN, TransactionType.DEPOSIT, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);
        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(eq(10L), eq(10L), any()))
                .thenReturn(page);

        var result = transferService.getAccountTransactionsPaged(10L, 1L, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReferenceId()).isEqualTo("TX-PAGED-1");
    }

    @Test
    @DisplayName("Get filtered transactions returns PageResponse")
    void getFilteredTransactionsReturnsPage() {
        Transaction tx = new Transaction("TX-FILTER-1", "KEY-1", null, null, BigDecimal.TEN, TransactionType.DEPOSIT, com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);
        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findFilteredTransactionsForUser(eq(1L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        var result = transferService.getFilteredTransactions(1L, null, null, null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getReferenceId()).isEqualTo("TX-FILTER-1");
    }
}
