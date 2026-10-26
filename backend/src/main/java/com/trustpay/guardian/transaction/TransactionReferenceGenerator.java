package com.trustpay.guardian.transaction;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TransactionReferenceGenerator {

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    public String generateReferenceId() {
        StringBuilder sb = new StringBuilder("TXN-");
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(ALPHANUMERIC.length());
            sb.append(ALPHANUMERIC.charAt(index));
        }
        return sb.toString();
    }
}
