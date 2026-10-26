package com.trustpay.guardian.auth;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountService;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.auth.dto.AuthResponse;
import com.trustpay.guardian.auth.dto.ChangePasswordRequest;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.auth.dto.UserSummaryDto;
import com.trustpay.guardian.common.exception.AccountLockedException;
import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.common.exception.ResourceNotFoundException;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService tokenProvider;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final long refreshExpirationMs;

    public AuthService(
            UserRepository userRepository,
            AccountService accountService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService tokenProvider,
            AuditService auditService,
            NotificationService notificationService,
            @Value("${app.security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("An account with this email address already exists.");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                request.getPhoneNumber().trim(),
                passwordHash,
                Role.CUSTOMER
        );
        user = userRepository.save(user);

        // Auto-create default SAVINGS account funded with DEMO_OPENING_BALANCE via AccountService
        Account account = accountService.createDefaultAccount(user);

        // Send welcome notification
        notificationService.sendNotification(
                user,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                String.format("Your demo savings account %s has been created with an opening balance of ₹%s.",
                        account.getMaskedAccountNumber(), account.getBalance().toPlainString())
        );

        auditService.logAction(
                user,
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.USER,
                String.valueOf(user.getId()),
                "{\"event\":\"USER_REGISTERED\",\"role\":\"CUSTOMER\"}",
                ipAddress,
                requestId
        );

        return createAuthSession(user);
    }

    @Transactional(noRollbackFor = { BadCredentialsException.class, AccountLockedException.class })
    public AuthResponse login(LoginRequest request, String ipAddress, String requestId) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        Instant now = Instant.now();

        // 1. Check brute force lockout
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now)) {
            long minutesRemaining = Duration.between(now, user.getLockoutUntil()).toMinutes() + 1;
            throw new AccountLockedException(minutesRemaining);
        }

        // 2. Check user status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is " + user.getStatus());
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= 5) {
                user.setLockoutUntil(now.plus(15, ChronoUnit.MINUTES));
                userRepository.save(user);

                auditService.logAction(
                        user,
                        AuditAction.USER_STATUS_CHANGE,
                        AuditTargetType.USER,
                        String.valueOf(user.getId()),
                        "{\"event\":\"ACCOUNT_LOCKED_5_FAILED_ATTEMPTS\"}",
                        ipAddress,
                        requestId
                );

                throw new AccountLockedException(15);
            }

            userRepository.save(user);

            auditService.logAction(
                    user,
                    AuditAction.USER_STATUS_CHANGE,
                    AuditTargetType.USER,
                    String.valueOf(user.getId()),
                    String.format("{\"event\":\"LOGIN_FAILURE\",\"attempt\":%d}", failedAttempts),
                    ipAddress,
                    requestId
            );

            throw new BadCredentialsException("Invalid email or password");
        }

        // Password matches: reset failed login attempts and clear lockout
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        auditService.logAction(
                user,
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.USER,
                String.valueOf(user.getId()),
                "{\"event\":\"LOGIN_SUCCESS\"}",
                ipAddress,
                requestId
        );

        return createAuthSession(user);
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException("Refresh token is missing", HttpStatus.BAD_REQUEST);
        }

        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now();

        // Token reuse detection: if a revoked token is used, someone might have stolen it!
        if (storedToken.getRevokedAt() != null) {
            log.warn("Revoked refresh token presented for user [{}]. Revoking all active tokens!",
                    storedToken.getUser().getEmail());
            // Revoke all tokens for this user as safety measure
            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(storedToken.getUser().getId());
            for (RefreshToken t : activeTokens) {
                t.setRevokedAt(now);
            }
            refreshTokenRepository.saveAll(activeTokens);
            throw new ApiException("Refresh token was revoked or reused. All sessions terminated.", HttpStatus.UNAUTHORIZED);
        }

        if (storedToken.getExpiresAt().isBefore(now)) {
            storedToken.setRevokedAt(now);
            refreshTokenRepository.save(storedToken);
            throw new ApiException("Refresh token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        // Token is valid: rotate it
        storedToken.setRevokedAt(now);

        String newRawToken = tokenProvider.generateSecureRandomToken();
        String newHash = tokenProvider.hashToken(newRawToken);
        storedToken.setReplacedByHash(newHash);
        refreshTokenRepository.save(storedToken);

        // Create new refresh token entry
        RefreshToken newRefreshToken = new RefreshToken(
                storedToken.getUser(),
                newHash,
                now.plusMillis(refreshExpirationMs)
        );
        refreshTokenRepository.save(newRefreshToken);

        UserPrincipal principal = UserPrincipal.create(storedToken.getUser());
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        return new AuthResponse(
                newAccessToken,
                tokenProvider.getExpirationMs() / 1000,
                UserSummaryDto.fromEntity(storedToken.getUser()),
                newRawToken
        );
    }

    @Transactional
    public void logout(String rawRefreshToken, String ipAddress, String requestId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);

                auditService.logAction(
                        token.getUser(),
                        AuditAction.USER_STATUS_CHANGE,
                        AuditTargetType.USER,
                        String.valueOf(token.getUser().getId()),
                        "{\"event\":\"USER_LOGOUT\"}",
                        ipAddress,
                        requestId
                );
            });
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, String ipAddress, String requestId) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("New password and confirmation password do not match");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BusinessRuleException("New password cannot be the same as the current password");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens for the user upon password change
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        Instant now = Instant.now();
        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);

        auditService.logAction(
                user,
                AuditAction.USER_STATUS_CHANGE,
                AuditTargetType.USER,
                String.valueOf(user.getId()),
                "{\"event\":\"PASSWORD_CHANGE\"}",
                ipAddress,
                requestId
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUserSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserSummaryDto.fromEntity(user);
    }

    private AuthResponse createAuthSession(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);

        String rawRefreshToken = tokenProvider.generateSecureRandomToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                Instant.now().plusMillis(refreshExpirationMs)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                tokenProvider.getExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user),
                rawRefreshToken
        );
    }
}
