package com.trustpay.guardian.security;

/**
 * Backwards-compatible facade for {@link JwtService}.
 */
public class JwtTokenProvider extends JwtService {

    public JwtTokenProvider(String jwtSecret, long jwtExpirationMs) {
        super(jwtSecret, jwtExpirationMs);
    }
}
