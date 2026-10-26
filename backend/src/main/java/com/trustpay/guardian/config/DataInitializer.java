package com.trustpay.guardian.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Backwards-compatible alias for {@link DevDataSeeder}.
 */
@Component
@Profile("dev")
public class DataInitializer {
    // Delegated entirely to DevDataSeeder
}
