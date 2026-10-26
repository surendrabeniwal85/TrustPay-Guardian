package com.trustpay.guardian.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.auth.dto.AuthResponse;
import com.trustpay.guardian.auth.dto.ChangePasswordRequest;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.auth.dto.UserSummaryDto;
import com.trustpay.guardian.common.web.RequestIdFilter;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(requestIdFilter)
                .build();
    }

    @Test
    @DisplayName("POST /auth/register - Rejects weak password with 400 and fieldErrors")
    void registerWithWeakPasswordReturns400() throws Exception {
        RegisterRequest weakPasswordRequest = new RegisterRequest(
                "Weak User",
                "weak@example.com",
                "9876543210",
                "weak" // Less than 8 chars, no digit, no uppercase, no special char
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPasswordRequest))
                        .header("X-Request-Id", "REQ-WEAK-PWD"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "REQ-WEAK-PWD"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Register, Login, Me, Change Password and Logout complete flow")
    void completeAuthLifecycleFlow() throws Exception {
        String email = "lifecycle.user@example.com";
        String password = "SecurePassword@123";

        // 1. Register
        RegisterRequest registerReq = new RegisterRequest(
                "Lifecycle User",
                email,
                "9876543219",
                password
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().attribute("refreshToken", "SameSite", "Strict"));

        // 2. Duplicate registration returns 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("An account with this email address already exists."));

        // 3. Login with wrong password returns 401 Unauthorized
        LoginRequest badLogin = new LoginRequest(email, "WrongPassword@999");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        // 4. Login with correct credentials returns 200 OK and HttpOnly SameSite=Strict cookie
        LoginRequest goodLogin = new LoginRequest(email, password);
        String loginResponseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andReturn().getResponse().getContentAsString();

        // Extract token
        String accessToken = objectMapper.readTree(loginResponseJson).path("data").path("accessToken").asText();

        // 5. GET /api/v1/auth/me without token returns 401 Unauthorized ProblemDetail
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 6. GET /api/v1/auth/me with Bearer token returns 200 OK
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        // 7. Customer token on /api/v1/admin/users returns 403 Forbidden ProblemDetail
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 8. Change password
        ChangePasswordRequest changePasswordReq = new ChangePasswordRequest(
                password,
                "NewSecurePassword@2026",
                "NewSecurePassword@2026"
        );
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 9. Logout clears cookie
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "dummyToken")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
