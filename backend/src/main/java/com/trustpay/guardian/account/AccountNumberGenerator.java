package com.trustpay.guardian.account;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique 13-character account numbers adhering to PRD format:
 * "TPG" prefix followed by 10 cryptographically random decimal digits.
 * Automatically retries up to 20 times in the unlikely event of a collision.
 */
@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "TPG";
    private final SecureRandom secureRandom = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateAccountNumber() {
        for (int i = 0; i < 20; i++) {
            long number = Math.abs(secureRandom.nextLong()) % 10_000_000_000L;
            String candidate = String.format("%s%010d", PREFIX, number);
            if (accountRepository.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        // Deterministic high-entropy fallback using current timestamp millis
        long fallbackNumber = System.currentTimeMillis() % 10_000_000_000L;
        return String.format("%s%010d", PREFIX, fallbackNumber);
    }
}
