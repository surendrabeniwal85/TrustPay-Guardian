package com.trustpay.guardian.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final String CURRENCY_CODE = "INR";
    public static final String CURRENCY_SYMBOL = "₹";

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isValidAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(BigDecimal.ZERO) > 0 && amount.scale() <= 2;
    }

    public static String formatWithSymbol(BigDecimal amount) {
        if (amount == null) {
            return CURRENCY_SYMBOL + "0.00";
        }
        BigDecimal rounded = round(amount);
        String plain = rounded.toPlainString();
        String[] parts = plain.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";

        boolean negative = integerPart.startsWith("-");
        if (negative) {
            integerPart = integerPart.substring(1);
        }

        StringBuilder formattedInt = new StringBuilder();
        int len = integerPart.length();
        if (len <= 3) {
            formattedInt.append(integerPart);
        } else {
            String lastThree = integerPart.substring(len - 3);
            String remaining = integerPart.substring(0, len - 3);
            StringBuilder reversed = new StringBuilder(remaining).reverse();
            StringBuilder grouped = new StringBuilder();
            for (int i = 0; i < reversed.length(); i++) {
                if (i > 0 && i % 2 == 0) {
                    grouped.append(",");
                }
                grouped.append(reversed.charAt(i));
            }
            formattedInt.append(grouped.reverse()).append(",").append(lastThree);
        }

        return (negative ? "-" : "") + CURRENCY_SYMBOL + formattedInt + "." + decimalPart;
    }

    public static String formatPlain(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return round(amount).toPlainString();
    }
}
