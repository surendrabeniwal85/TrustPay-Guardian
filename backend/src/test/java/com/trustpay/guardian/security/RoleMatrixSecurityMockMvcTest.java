package com.trustpay.guardian.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RoleMatrixSecurityMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> tokens = new HashMap<>();

    @BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        tokens.clear();
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        User customer = userRepository.save(new User("Cust Matrix", "cust_" + uniqueSuffix + "@test.com", "9111111111", "hash", Role.CUSTOMER));
        User analyst = userRepository.save(new User("Analyst Matrix", "analyst_" + uniqueSuffix + "@test.com", "9111111112", "hash", Role.FRAUD_ANALYST));
        User admin = userRepository.save(new User("Admin Matrix", "admin_" + uniqueSuffix + "@test.com", "9111111113", "hash", Role.ADMIN));

        tokens.put("ANONYMOUS", null);
        tokens.put("CUSTOMER", "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(customer)));
        tokens.put("FRAUD_ANALYST", "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(analyst)));
        tokens.put("ADMIN", "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(admin)));
    }

    public static Stream<Arguments> roleMatrixCases() {
        return Stream.of(
                // 1. Public Endpoints (PermitAll for all 4 roles)
                Arguments.of("GET", "/api/v1/public/stats", "ANONYMOUS", 200),
                Arguments.of("GET", "/api/v1/public/stats", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/public/stats", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/public/stats", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/health", "ANONYMOUS", 200),
                Arguments.of("GET", "/api/v1/health", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/health", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/health", "ADMIN", 200),

                Arguments.of("GET", "/actuator/health", "ANONYMOUS", 200),
                Arguments.of("GET", "/actuator/health", "CUSTOMER", 200),
                Arguments.of("GET", "/actuator/health", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/actuator/health", "ADMIN", 200),

                // 2. Customer / Authenticated Endpoints (401 Anon, 200 Auth)
                Arguments.of("GET", "/api/v1/auth/me", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/auth/me", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/auth/me", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/auth/me", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/accounts", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/accounts", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/accounts", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/accounts", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/beneficiaries", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/beneficiaries", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/beneficiaries", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/beneficiaries", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/transactions", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/transactions", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/transactions", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/transactions", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/notifications", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/notifications", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/notifications", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/notifications", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/notifications/unread-count", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/notifications/unread-count", "CUSTOMER", 200),
                Arguments.of("GET", "/api/v1/notifications/unread-count", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/notifications/unread-count", "ADMIN", 200),

                // 3. Fraud Analyst Endpoints
                // Queue & Stats: Analyst and Admin (Read-Only)
                Arguments.of("GET", "/api/v1/fraud/alerts", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/fraud/alerts", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/fraud/alerts", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/fraud/alerts", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/fraud/stats", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/fraud/stats", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/fraud/stats", "FRAUD_ANALYST", 200),
                Arguments.of("GET", "/api/v1/fraud/stats", "ADMIN", 200),

                // Alert Resolutions: Strictly FRAUD_ANALYST only! (Admin returns 403)
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/release", "ANONYMOUS", 401),
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/release", "CUSTOMER", 403),
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/release", "FRAUD_ANALYST", 404), // Auth pass, alert not found
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/release", "ADMIN", 403),          // Admin is strictly read-only

                Arguments.of("POST", "/api/v1/fraud/alerts/99999/confirm-block", "ANONYMOUS", 401),
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/confirm-block", "CUSTOMER", 403),
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/confirm-block", "FRAUD_ANALYST", 404), // Auth pass, alert not found
                Arguments.of("POST", "/api/v1/fraud/alerts/99999/confirm-block", "ADMIN", 403),          // Admin is strictly read-only

                // 4. Admin Management Endpoints (Admin only)
                Arguments.of("GET", "/api/v1/admin/dashboard", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/dashboard", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/dashboard", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/dashboard", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/users", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/users", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/users", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/users", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/accounts", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/accounts", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/accounts", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/accounts", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/transactions", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/transactions", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/transactions", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/transactions", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/fraud-rules", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/fraud-rules", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/fraud-rules", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/fraud-rules", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/settings", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/settings", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/settings", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/settings", "ADMIN", 200),

                Arguments.of("GET", "/api/v1/admin/audit-logs", "ANONYMOUS", 401),
                Arguments.of("GET", "/api/v1/admin/audit-logs", "CUSTOMER", 403),
                Arguments.of("GET", "/api/v1/admin/audit-logs", "FRAUD_ANALYST", 403),
                Arguments.of("GET", "/api/v1/admin/audit-logs", "ADMIN", 200)
        );
    }

    @ParameterizedTest(name = "{0} {1} with role {2} -> expects {3}")
    @MethodSource("roleMatrixCases")
    @DisplayName("Role matrix security policy verification")
    void testRoleMatrix(String method, String url, String role, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), url)
                .contentType(MediaType.APPLICATION_JSON);

        String token = tokens.get(role);
        if (token != null) {
            requestBuilder.header("Authorization", token);
        }

        if ("POST".equals(method)) {
            // Supply valid payload satisfying validation constraints
            requestBuilder.content("{\"decisionNotes\":\"Authorization role check test note\"}");
        }

        mockMvc.perform(requestBuilder)
                .andExpect(status().is(expectedStatus));
    }
}
