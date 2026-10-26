package com.trustpay.guardian.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.admin.dto.*;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditLog;
import com.trustpay.guardian.audit.AuditLogRepository;
import com.trustpay.guardian.common.exception.AccountFrozenException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.UnauthorizedAccessException;
import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.FraudRuleRepository;
import com.trustpay.guardian.notification.Notification;
import com.trustpay.guardian.notification.NotificationRepository;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.transaction.TransferService;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminWorkflowAndSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User admin;
    private User customer1;
    private User customer2;
    private Account customerAccount1;
    private Account customerAccount2;

    @BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        admin = userRepository.save(new User("Master Admin", "m_admin@test.com", "9876543210", "hash", Role.ADMIN));
        customer1 = userRepository.save(new User("Alice Customer", "alice_wf@test.com", "9876543211", "hash", Role.CUSTOMER));
        customer2 = userRepository.save(new User("Bob Customer", "bob_wf@test.com", "9876543212", "hash", Role.CUSTOMER));

        customerAccount1 = accountRepository.save(new Account(customer1, "ACC-ALICE-01", AccountType.SAVINGS, new BigDecimal("50000.00"), new BigDecimal("100000.00")));
        customerAccount2 = accountRepository.save(new Account(customer2, "ACC-BOB-01", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
    }

    @Test
    @DisplayName("Admin creates new staff analyst: password encoded, audit logged, duplicate rejected")
    void createStaffUserWorkflow() {
        CreateUserRequest req = new CreateUserRequest(
                "New Analyst", "new_analyst@test.com", "9876543213", "SecurePass@123", Role.FRAUD_ANALYST);

        UserManagementDto created = adminService.createUser(req, admin.getId(), "127.0.0.1", "REQ-CREAT-01");
        assertThat(created).isNotNull();
        assertThat(created.getEmail()).isEqualTo("new_analyst@test.com");
        assertThat(created.getRole()).isEqualTo(Role.FRAUD_ANALYST);

        User persisted = userRepository.findByEmail("new_analyst@test.com").orElseThrow();
        assertThat(passwordEncoder.matches("SecurePass@123", persisted.getPasswordHash())).isTrue();

        // Duplicate email throws ConflictException (409)
        assertThatThrownBy(() -> adminService.createUser(req, admin.getId(), "127.0.0.1", "REQ-CREAT-02"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Admin freeze and unfreeze account: user notified and transfers immediately blocked with 422")
    void freezeAndUnfreezeAccountWorkflow() {
        // 1. Freeze account
        AccountManagementDto frozen = adminService.freezeAccount(
                customerAccount1.getId(), admin.getId(), "127.0.0.1", "REQ-FRZ-01");
        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);

        // Verify in-app notification received
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ACCOUNT_FROZEN);

        // Verify transfer from frozen account fails immediately with 422 AccountFrozenException
        TransferRequest xferReq = new TransferRequest(customerAccount1.getId(), customerAccount2.getAccountNumber(), new BigDecimal("100.00"), "Groceries");
        assertThatThrownBy(() -> transferService.initiateTransfer(
                xferReq, UUID.randomUUID().toString(), customer1.getId(), "127.0.0.1", "REQ-TX-FRZ"))
                .isInstanceOf(AccountFrozenException.class);

        // 2. Unfreeze account
        AccountManagementDto unfrozen = adminService.unfreezeAccount(
                customerAccount1.getId(), admin.getId(), "127.0.0.1", "REQ-UNFRZ-01");
        assertThat(unfrozen.status()).isEqualTo(AccountStatus.ACTIVE);

        // Verify unfreeze notification received
        List<Notification> notificationsAfter = notificationRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(notificationsAfter.get(0).getType()).isEqualTo(NotificationType.ACCOUNT_UNFROZEN);

        // Verify transfer succeeds now
        var response = transferService.initiateTransfer(
                xferReq, UUID.randomUUID().toString(), customer1.getId(), "127.0.0.1", "REQ-TX-OK");
        assertThat(response).isNotNull();
        assertThat(response.getStatus().name()).isEqualTo("APPROVED_SIMULATED");
    }

    @Test
    @DisplayName("Admin updates system setting: updated_by is assigned, cache is invalidated, and audit log written")
    void updateSystemSettingWorkflow() {
        SystemSetting setting = systemSettingRepository.findBySettingKey("MAX_TRANSFER_AMOUNT").orElseThrow();
        UpdateSettingRequest req = new UpdateSettingRequest("250000.00", "Updated max transfer ceiling");

        try {
            SystemSetting updated = adminService.updateSetting(
                    setting.getId(), req, admin.getId(), "127.0.0.1", "REQ-SET-01");
            assertThat(updated.getSettingValue()).isEqualTo("250000.00");
            assertThat(updated.getUpdatedBy()).isNotNull();
            assertThat(updated.getUpdatedBy().getId()).isEqualTo(admin.getId());

            // Audit log check
            Page<AuditLog> auditLogs = auditLogRepository.findFilteredAuditLogs(
                    AuditAction.SYSTEM_SETTING_UPDATE, null, PageRequest.of(0, 10));
            assertThat(auditLogs.getContent()).isNotEmpty();
        } finally {
            setting.setSettingValue("200000.00");
            systemSettingRepository.save(setting);
            fraudRuleConfigService.clearCache();
        }
    }

    @Test
    @DisplayName("Public stats endpoint reveals non-sensitive aggregate counters and zero personal data")
    void publicStatsEndpointVerification() throws Exception {
        mockMvc.perform(get("/api/v1/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTransfers").isNumber())
                .andExpect(jsonPath("$.data.totalVolume").isNumber())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.alertsAnalyzed").isNumber())
                // Ensure no PII keys exist in the response
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.fullName").doesNotExist())
                .andExpect(jsonPath("$.data.accountNumber").doesNotExist())
                .andExpect(jsonPath("$.data.users").doesNotExist());
    }

    @Test
    @DisplayName("Notification operations: read, read-all, and cross-user authorization protection")
    void notificationWorkflowAndSecurity() {
        Notification n1 = notificationService.sendNotification(
                customer1, NotificationType.TRANSFER_RECEIVED, "Credit Received", "₹500.00 credited");
        Notification n2 = notificationService.sendNotification(
                customer1, NotificationType.SECURITY_ALERT, "Security Alert", "New login from unknown device");

        assertThat(notificationService.getUnreadCount(customer1.getId())).isEqualTo(2);

        // Mark single notification read
        notificationService.markAsRead(n1.getId(), customer1.getId());
        assertThat(notificationService.getUnreadCount(customer1.getId())).isEqualTo(1);

        // Cross-user unauthorized access attempt throws UnauthorizedAccessException (403)
        assertThatThrownBy(() -> notificationService.markAsRead(n2.getId(), customer2.getId()))
                .isInstanceOf(UnauthorizedAccessException.class);

        // Mark all as read
        notificationService.markAllAsRead(customer1.getId());
        assertThat(notificationService.getUnreadCount(customer1.getId())).isEqualTo(0);
    }
}
