package com.trustpay.guardian.auth;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountService;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.auth.dto.AuthResponse;
import com.trustpay.guardian.auth.dto.ChangePasswordRequest;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.common.exception.AccountLockedException;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                accountService,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                auditService,
                notificationService,
                604800000L
        );
    }

    @Test
    @DisplayName("Registration rejects duplicate email with ConflictException (409)")
    void registrationRejectsDuplicateEmail() {
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        RegisterRequest request = new RegisterRequest("Existing User", "existing@example.com", "9876543210", "Password@123");

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1", "REQ-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Password failure increments count; 5th failure triggers 15-minute lockout")
    void lockoutAfterFiveFailures() {
        User user = new User("John", "john@example.com", "9876543210", "hashed_pwd", Role.CUSTOMER);
        user.setFailedLoginAttempts(4);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass@123", "hashed_pwd")).thenReturn(false);

        LoginRequest badLogin = new LoginRequest("john@example.com", "WrongPass@123");

        assertThatThrownBy(() -> authService.login(badLogin, "127.0.0.1", "REQ-5"))
                .isInstanceOf(AccountLockedException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockoutUntil()).isNotNull();
        assertThat(user.getLockoutUntil()).isAfter(Instant.now().plus(14, ChronoUnit.MINUTES));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Password change succeeds with valid old password and revokes all active refresh tokens")
    void changePasswordSucceedsAndRevokesTokens() {
        User user = new User("John", "john@example.com", "9876543210", "old_hash", Role.CUSTOMER);
        user.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "old_hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@2026")).thenReturn("new_hash");

        RefreshToken token1 = new RefreshToken(user, "hash1", Instant.now().plus(7, ChronoUnit.DAYS));
        RefreshToken token2 = new RefreshToken(user, "hash2", Instant.now().plus(7, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(10L)).thenReturn(List.of(token1, token2));

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "NewPassword@2026", "NewPassword@2026");
        authService.changePassword(10L, request, "127.0.0.1", "REQ-CHG");

        assertThat(user.getPasswordHash()).isEqualTo("new_hash");
        assertThat(token1.getRevokedAt()).isNotNull();
        assertThat(token2.getRevokedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    @DisplayName("Password change rejects mismatched confirmation or identical new password")
    void changePasswordValidations() {
        ChangePasswordRequest mismatch = new ChangePasswordRequest("Old@123", "New@123", "Different@123");
        assertThatThrownBy(() -> authService.changePassword(1L, mismatch, "127.0.0.1", "REQ-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not match");

        ChangePasswordRequest same = new ChangePasswordRequest("Same@123", "Same@123", "Same@123");
        assertThatThrownBy(() -> authService.changePassword(1L, same, "127.0.0.1", "REQ-2"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("same as the current password");
    }

    @Test
    @DisplayName("Refresh token reuse detection revokes all tokens for that user")
    void tokenReuseDetectionRevokesAllTokens() {
        User user = new User("Victim", "victim@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(20L);

        RefreshToken revokedToken = new RefreshToken(user, "revoked_hash", Instant.now().plus(5, ChronoUnit.DAYS));
        revokedToken.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(jwtService.hashToken("raw_reused_token")).thenReturn("revoked_hash");
        when(refreshTokenRepository.findByTokenHash("revoked_hash")).thenReturn(Optional.of(revokedToken));

        RefreshToken activeToken1 = new RefreshToken(user, "active_hash_1", Instant.now().plus(5, ChronoUnit.DAYS));
        RefreshToken activeToken2 = new RefreshToken(user, "active_hash_2", Instant.now().plus(5, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(20L)).thenReturn(List.of(activeToken1, activeToken2));

        assertThatThrownBy(() -> authService.refreshToken("raw_reused_token"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("revoked or reused");

        assertThat(activeToken1.getRevokedAt()).isNotNull();
        assertThat(activeToken2.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).saveAll(any());
    }
}
