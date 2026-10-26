package com.trustpay.guardian.security;

import com.trustpay.guardian.user.UserRepository;

/**
 * Backwards-compatible facade for {@link AppUserDetailsService}.
 */
public class CustomUserDetailsService extends AppUserDetailsService {

    public CustomUserDetailsService(UserRepository userRepository) {
        super(userRepository);
    }
}
