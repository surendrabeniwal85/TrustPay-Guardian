package com.trustpay.guardian.auth;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.auth.dto.AuthResponse;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.common.exception.AccountLockedException;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Registration creates user with hashed password and auto-created savings account")
    void registrationCreatesUserAndSavingsAccount() {
        RegisterRequest request = new RegisterRequest(
                "Rohan Sharma",
                "rohan.sharma@example.com",
                "9876501234",
                "SecurePass@123"
        );

        AuthResponse response = authService.register(request, "127.0.0.1", "REQ-REG-01");

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("rohan.sharma@example.com");
        assertThat(response.getUser().getRole()).isEqualTo(Role.CUSTOMER);

        User savedUser = userRepository.findByEmail("rohan.sharma@example.com").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePass@123");
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");

        List<Account> accounts = accountRepository.findByUserId(savedUser.getId());
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("Login with valid credentials returns tokens; failed attempts reset to 0")
    void loginWithValidCredentialsSucceeds() {
        RegisterRequest registerReq = new RegisterRequest(
                "Deepa Verma",
                "deepa.verma@example.com",
                "9876501235",
                "MyPassword@123"
        );
        authService.register(registerReq, "127.0.0.1", "REQ-REG-02");

        LoginRequest loginReq = new LoginRequest("deepa.verma@example.com", "MyPassword@123");
        AuthResponse loginResponse = authService.login(loginReq, "127.0.0.1", "REQ-LOGIN-01");

        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();

        User user = userRepository.findByEmail("deepa.verma@example.com").orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Brute-force lockout triggers after 5 failed password attempts")
    void bruteForceLockoutAfterFiveAttempts() {
        RegisterRequest registerReq = new RegisterRequest(
                "Suresh Raina",
                "suresh.raina@example.com",
                "9876501236",
                "ValidPassword@123"
        );
        authService.register(registerReq, "127.0.0.1", "REQ-REG-03");

        LoginRequest badLogin = new LoginRequest("suresh.raina@example.com", "WrongPassword@999");

        // 4 failed attempts should fail with BadCredentialsException (invalid credentials)
        for (int i = 1; i <= 4; i++) {
            final int attempt = i;
            assertThatThrownBy(() -> authService.login(badLogin, "127.0.0.1", "REQ-BAD-" + attempt))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        // 5th attempt locks the account
        assertThatThrownBy(() -> authService.login(badLogin, "127.0.0.1", "REQ-BAD-5"))
                .isInstanceOf(AccountLockedException.class);

        User lockedUser = userRepository.findByEmail("suresh.raina@example.com").orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockoutUntil()).isAfter(Instant.now());

        // Subsequent attempt with even CORRECT password is rejected due to active lock
        LoginRequest correctLogin = new LoginRequest("suresh.raina@example.com", "ValidPassword@123");
        assertThatThrownBy(() -> authService.login(correctLogin, "127.0.0.1", "REQ-LOCKED"))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is temporarily locked");
    }

    @Test
    @DisplayName("Token refresh rotates refresh token and invalidates old token")
    void tokenRefreshRotatesToken() {
        RegisterRequest registerReq = new RegisterRequest(
                "Kavita Sen",
                "kavita.sen@example.com",
                "9876501237",
                "Password@123"
        );
        AuthResponse initial = authService.register(registerReq, "127.0.0.1", "REQ-REG-04");
        String oldRefreshToken = initial.getRefreshToken();

        AuthResponse refreshed = authService.refreshToken(oldRefreshToken);

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(oldRefreshToken);

        // Attempting to reuse old refresh token triggers theft detection and throws ApiException
        assertThatThrownBy(() -> authService.refreshToken(oldRefreshToken))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reused");
    }
}
