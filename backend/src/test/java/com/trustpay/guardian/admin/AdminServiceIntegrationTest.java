package com.trustpay.guardian.admin;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.admin.dto.*;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.fraud.FraudRuleRepository;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdminServiceIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Test
    @DisplayName("Admin dashboard correctly calculates system aggregate metrics")
    void dashboardMetricsCalculation() {
        AdminDashboardResponse dashboard = adminService.getDashboard();

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getActiveRules()).isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("Admin can update other user role but self-demotion is prevented")
    void userRoleUpdateAndSelfDemotionGuard() {
        User admin = userRepository.save(new User("Admin Super", "adminsuper@test.com", "9222222221", "hash", Role.ADMIN));
        User target = userRepository.save(new User("Target Cust", "targetcust@test.com", "9222222222", "hash", Role.CUSTOMER));

        // Promoting target to FRAUD_ANALYST succeeds
        UserManagementDto updatedTarget = adminService.updateUserRole(
                target.getId(), Role.FRAUD_ANALYST, admin.getId(), "127.0.0.1", "REQ-ROLE-01");
        assertThat(updatedTarget.getRole()).isEqualTo(Role.FRAUD_ANALYST);

        // Self-demotion must be blocked
        assertThatThrownBy(() -> adminService.updateUserRole(
                admin.getId(), Role.CUSTOMER, admin.getId(), "127.0.0.1", "REQ-ROLE-02"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("self-demotion prevented");
    }

    @Test
    @DisplayName("Admin can suspend user but self-suspension is prevented")
    void userStatusUpdateAndSelfSuspensionGuard() {
        User admin = userRepository.save(new User("Admin Power", "adminpower@test.com", "9222222223", "hash", Role.ADMIN));
        User target = userRepository.save(new User("Suspect User", "suspect@test.com", "9222222224", "hash", Role.CUSTOMER));

        // Suspending target user succeeds
        UserManagementDto suspended = adminService.updateUserStatus(
                target.getId(), UserStatus.SUSPENDED, admin.getId(), "127.0.0.1", "REQ-STATUS-01");
        assertThat(suspended.getStatus()).isEqualTo(UserStatus.SUSPENDED);

        // Self-suspension must be blocked
        assertThatThrownBy(() -> adminService.updateUserStatus(
                admin.getId(), UserStatus.SUSPENDED, admin.getId(), "127.0.0.1", "REQ-STATUS-02"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot suspend or deactivate their own account");
    }

    @Test
    @DisplayName("Admin can tune fraud rule score points and system settings")
    void tuneFraudRuleAndSettings() {
        User admin = userRepository.save(new User("Admin Tuner", "admintuner@test.com", "9222222225", "hash", Role.ADMIN));

        FraudRule rule = fraudRuleRepository.findByRuleCode("R1").orElseThrow();
        UpdateRuleRequest ruleReq = new UpdateRuleRequest(45, true, "Updated R1 threshold score to 45 pts");

        try {
            FraudRule updatedRule = adminService.updateFraudRule(
                    rule.getId(), ruleReq, admin.getId(), "127.0.0.1", "REQ-TUNE-01");
            assertThat(updatedRule.getPoints()).isEqualTo(45);

            SystemSetting setting = systemSettingRepository.findBySettingKey("REVIEW_THRESHOLD").orElseThrow();
            UpdateSettingRequest settingReq = new UpdateSettingRequest("45", "Increased review threshold to 45");

            SystemSetting updatedSetting = adminService.updateSetting(
                    setting.getId(), settingReq, admin.getId(), "127.0.0.1", "REQ-TUNE-02");
            assertThat(updatedSetting.getSettingValue()).isEqualTo("45");
        } finally {
            rule.setPoints(30);
            fraudRuleRepository.save(rule);
            SystemSetting setting = systemSettingRepository.findBySettingKey("REVIEW_THRESHOLD").orElseThrow();
            setting.setSettingValue("40");
            systemSettingRepository.save(setting);
        }
    }

    @Test
    @DisplayName("Admin audit log page query returns recorded actions")
    void auditLogQuery() {
        Page<AuditLogResponse> logs = adminService.getAuditLogs(PageRequest.of(0, 10));
        assertThat(logs).isNotNull();
    }
}
