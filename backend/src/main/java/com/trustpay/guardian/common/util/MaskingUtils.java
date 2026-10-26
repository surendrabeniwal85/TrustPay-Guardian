package com.trustpay.guardian.common.util;

import org.springframework.util.StringUtils;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskAccountNumber(String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            return "";
        }
        String clean = accountNumber.trim();
        if (clean.length() <= 7) {
            return clean;
        }

        String prefix = clean.startsWith("TPG") ? "TPG" : clean.substring(0, 3);
        String suffix = clean.substring(clean.length() - 4);
        int maskLength = Math.max(3, clean.length() - prefix.length() - suffix.length());
        return prefix + "*".repeat(maskLength) + suffix;
    }

    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.trim().split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }

        char first = local.charAt(0);
        char last = local.charAt(local.length() - 1);
        return first + "***" + last + "@" + domain;
    }

    public static String maskPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        String suffix = clean.substring(clean.length() - 4);
        return "*".repeat(clean.length() - 4) + suffix;
    }

    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return "***";
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() <= 1) {
                sb.append(word);
            } else {
                sb.append(word.charAt(0)).append("*".repeat(word.length() - 1));
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
