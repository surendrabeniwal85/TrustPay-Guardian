package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.ResourceNotFoundException;
import com.trustpay.guardian.fraud.dto.AlertDecisionRequest;
import com.trustpay.guardian.fraud.dto.FraudAlertResponse;
import com.trustpay.guardian.fraud.dto.FraudStatsResponse;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudAnalystServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private FraudAnalystService fraudAnalystService;

    @BeforeEach
    void setUp() {
        fraudAnalystService = new FraudAnalystService(
                fraudAlertRepository,
                transactionRepository,
                accountRepository,
                userRepository,
                notificationService,
                auditService
        );
    }

    @Test
    @DisplayName("getAlerts returns mapped Page of FraudAlertResponse")
    void getAlertsReturnsFilteredPage() {
        User sender = new User("Sender", "s@test.com", "9000000001", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG1", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("50000.00"));
        Transaction tx = new Transaction("TXN-1", "IDEM-1", src, null, new BigDecimal("500.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW);
        FraudAlert alert = new FraudAlert(tx, 50, RiskLevel.MEDIUM, AlertStatus.OPEN);
        alert.setId(10L);

        when(fraudAlertRepository.findFilteredAlerts(eq(AlertStatus.OPEN), eq(40), eq(70), any()))
                .thenReturn(new PageImpl<>(List.of(alert)));

        Page<FraudAlertResponse> results = fraudAnalystService.getAlerts(AlertStatus.OPEN, 40, 70, PageRequest.of(0, 10));

        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getAlertDetails throws ResourceNotFoundException when alert does not exist")
    void getAlertDetailsNotFoundThrows() {
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraudAnalystService.getAlertDetails(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FraudAlert not found");
    }

    @Test
    @DisplayName("getStats calculates accurate aggregation counts and average risk score")
    void getStatsCalculation() {
        when(fraudAlertRepository.count()).thenReturn(10L);
        when(fraudAlertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(3L);
        when(fraudAlertRepository.countByStatus(AlertStatus.IN_REVIEW)).thenReturn(2L);
        when(fraudAlertRepository.countByStatus(AlertStatus.RESOLVED_RELEASED)).thenReturn(4L);
        when(fraudAlertRepository.countByStatus(AlertStatus.RESOLVED_BLOCKED)).thenReturn(1L);
        when(fraudAlertRepository.findAverageRiskScore()).thenReturn(54.321);

        FraudStatsResponse stats = fraudAnalystService.getStats();

        assertThat(stats.totalAlerts()).isEqualTo(10L);
        assertThat(stats.openAlerts()).isEqualTo(3L);
        assertThat(stats.averageRiskScore()).isEqualTo(54.32);
    }

    @Test
    @DisplayName("resolveAlert on already resolved alert throws ConflictException (409)")
    void resolveAlreadyResolvedAlertThrowsConflict() {
        User sender = new User("Sender", "s@test.com", "9000000001", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG1", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("50000.00"));
        Transaction tx = new Transaction("TXN-1", "IDEM-1", src, null, new BigDecimal("500.00"), TransactionType.TRANSFER, TransactionStatus.APPROVED_SIMULATED);
        FraudAlert alert = new FraudAlert(tx, 60, RiskLevel.MEDIUM, AlertStatus.RESOLVED_RELEASED);
        alert.setId(20L);

        when(fraudAlertRepository.findById(20L)).thenReturn(Optional.of(alert));

        AlertDecisionRequest req = new AlertDecisionRequest("RELEASE", "Attempting second release");

        assertThatThrownBy(() -> fraudAnalystService.resolveAlert(20L, req, 1L, "127.0.0.1", "REQ-RES-01"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been resolved");
    }

    @Test
    @DisplayName("resolveAlert with RELEASE fails when sender balance is insufficient at release time")
    void resolveReleaseWithInsufficientFundsRejects() {
        User sender = new User("Sender", "s@test.com", "9000000001", "hash", Role.CUSTOMER);
        sender.setId(101L);
        User analyst = new User("Analyst", "a@test.com", "9000000002", "hash", Role.FRAUD_ANALYST);
        analyst.setId(102L);

        Account src = new Account(sender, "TPG1", AccountType.SAVINGS, new BigDecimal("100.00"), new BigDecimal("50000.00"));
        src.setId(1L);
        Account dst = new Account(sender, "TPG2", AccountType.SAVINGS, new BigDecimal("500.00"), new BigDecimal("50000.00"));
        dst.setId(2L);

        Transaction tx = new Transaction("TXN-1", "IDEM-1", src, dst, new BigDecimal("500.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW);
        FraudAlert alert = new FraudAlert(tx, 60, RiskLevel.MEDIUM, AlertStatus.OPEN);
        alert.setId(30L);

        when(fraudAlertRepository.findById(30L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(102L)).thenReturn(Optional.of(analyst));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(src));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dst));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(i -> i.getArgument(0));

        AlertDecisionRequest req = new AlertDecisionRequest("RELEASE", "Customer verified by phone call");

        FraudAlertResponse response = fraudAnalystService.resolveAlert(30L, req, 102L, "127.0.0.1", "REQ-RES-02");

        assertThat(response.getStatus()).isEqualTo(AlertStatus.RESOLVED_BLOCKED);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        verify(transactionRepository).save(tx);
    }
}
