package com.trustpay.guardian.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(
            @Value("${app.security.jwt.secret:}") String jwtSecret,
            @Value("${app.security.jwt.expiration-ms:900000}") long jwtExpirationMs) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing or shorter than 32 bytes (256 bits). Refusing to start application.");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generates an access token with claims: sub, role, iat, exp, jti, userId, fullName.
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .id(jti)
                .claim("userId", userPrincipal.getId())
                .claim("role", userPrincipal.getRole().name())
                .claim("fullName", userPrincipal.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    public String getJtiFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getId();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Generates a cryptographically secure 256-bit random refresh token (64 hex characters).
     */
    public String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    /**
     * Computes the SHA-256 hash of a string for secure database storage.
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
