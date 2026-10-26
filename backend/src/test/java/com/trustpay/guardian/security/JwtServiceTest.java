package com.trustpay.guardian.security;

import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = "SuperSecretKeyForTrustPayGuardianPlatformMustBeAtLeast32Bytes!";
    private static final long EXPIRATION_MS = 900000; // 15 min

    private final JwtService jwtService = new JwtService(VALID_SECRET, EXPIRATION_MS);

    @Test
    @DisplayName("Refuses to start when JWT secret is null, empty or shorter than 32 bytes")
    void refusesToStartWithInvalidSecret() {
        assertThatThrownBy(() -> new JwtService(null, EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing or shorter than 32 bytes");

        assertThatThrownBy(() -> new JwtService("", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing or shorter than 32 bytes");

        assertThatThrownBy(() -> new JwtService("short-secret-key", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing or shorter than 32 bytes");
    }

    @Test
    @DisplayName("Generates token with required claims (sub, role, iat, exp, jti, userId, fullName)")
    void generatesTokenWithRequiredClaims() {
        User user = new User("Alice Doe", "alice@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(42L);
        UserPrincipal principal = UserPrincipal.create(user);

        String token = jwtService.generateAccessToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUsernameFromToken(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtService.getRoleFromToken(token)).isEqualTo("CUSTOMER");
        assertThat(jwtService.getJtiFromToken(token)).isNotBlank();
    }

    @Test
    @DisplayName("Rejects tampered and malformed tokens")
    void rejectsTamperedToken() {
        User user = new User("Bob", "bob@example.com", "9876543211", "hash", Role.CUSTOMER);
        user.setId(43L);
        UserPrincipal principal = UserPrincipal.create(user);

        String token = jwtService.generateAccessToken(principal);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThat(jwtService.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Rejects expired tokens")
    void rejectsExpiredToken() {
        // Expiration of 1 millisecond
        JwtService shortLivedJwtService = new JwtService(VALID_SECRET, 1);
        User user = new User("Carol", "carol@example.com", "9876543212", "hash", Role.CUSTOMER);
        user.setId(44L);
        UserPrincipal principal = UserPrincipal.create(user);

        String token = shortLivedJwtService.generateAccessToken(principal);

        // Sleep 15ms so token expires
        try {
            Thread.sleep(15);
        } catch (InterruptedException ignored) {}

        assertThat(shortLivedJwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Generates 256-bit cryptographically secure random token and SHA-256 hash")
    void generatesAndHashesRefreshToken() {
        String rawToken = jwtService.generateSecureRandomToken();

        // 32 bytes in hex = 64 hex characters
        assertThat(rawToken).hasSize(64);
        assertThat(rawToken).matches("^[0-9a-f]{64}$");

        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }
}
