package com.trustpay.guardian.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.admin.dto.*;
import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.FraudRuleRepository;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminControllerMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User admin;
    private User customer;
    private Account account;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        admin = userRepository.save(new User("Super Admin", "s_admin@mock.com", "9812345670", "hash", Role.ADMIN));
        customer = userRepository.save(new User("Test Customer", "t_cust@mock.com", "9812345671", "hash", Role.CUSTOMER));
        account = accountRepository.save(new Account(customer, "ACC-MOCK-01", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("50000.00")));

        adminToken = "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard returns system KPI metrics")
    void testGetDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalAccounts").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users and POST /api/v1/admin/users")
    void testUserManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("role", "CUSTOMER")
                        .param("search", "Test")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        CreateUserRequest req = new CreateUserRequest(
                "Created Analyst", "created_analyst@mock.com", "9812345672", "SecurePass@123", Role.FRAUD_ANALYST);

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("created_analyst@mock.com"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role and PATCH /api/v1/admin/users/{id}/status")
    void testUserRoleAndStatusUpdates() throws Exception {
        UpdateRoleRequest roleReq = new UpdateRoleRequest(Role.FRAUD_ANALYST);
        mockMvc.perform(patch("/api/v1/admin/users/" + customer.getId() + "/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("FRAUD_ANALYST"));

        UpdateStatusRequest statusReq = new UpdateStatusRequest(UserStatus.SUSPENDED);
        mockMvc.perform(patch("/api/v1/admin/users/" + customer.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/accounts and freeze/unfreeze endpoints")
    void testAccountManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts")
                        .param("status", "ACTIVE")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(patch("/api/v1/admin/accounts/" + account.getId() + "/freeze")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        mockMvc.perform(patch("/api/v1/admin/accounts/" + account.getId() + "/unfreeze")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/fraud-rules and PUT /api/v1/admin/fraud-rules/{id}")
    void testFraudRulesEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fraud-rules")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        FraudRule rule = fraudRuleRepository.findByRuleCode("R1").orElseThrow();
        UpdateRuleRequest req = new UpdateRuleRequest(35, true, "Updated rule R1 for testing");

        mockMvc.perform(put("/api/v1/admin/fraud-rules/" + rule.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").value(35));
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings and PUT /api/v1/admin/settings/{id}")
    void testSettingsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        SystemSetting setting = systemSettingRepository.findBySettingKey("REVIEW_THRESHOLD").orElseThrow();
        UpdateSettingRequest req = new UpdateSettingRequest("42", "Updated review threshold");

        mockMvc.perform(put("/api/v1/admin/settings/" + setting.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("42"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/transactions and GET /api/v1/admin/audit-logs")
    void testTransactionsAndAuditEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/transactions")
                        .param("status", "APPROVED_SIMULATED")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
