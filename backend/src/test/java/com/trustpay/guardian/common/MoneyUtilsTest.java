package com.trustpay.guardian.common;

import com.trustpay.guardian.common.util.MoneyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

    @Test
    @DisplayName("Format with Indian Rupee symbol")
    void testFormatWithSymbol() {
        assertThat(MoneyUtils.formatWithSymbol(new BigDecimal("1000.50"))).isEqualTo("₹1,000.50");
        assertThat(MoneyUtils.formatWithSymbol(new BigDecimal("100000.00"))).isEqualTo("₹1,00,000.00");
        assertThat(MoneyUtils.formatWithSymbol(BigDecimal.ZERO)).isEqualTo("₹0.00");
        assertThat(MoneyUtils.formatWithSymbol(null)).isEqualTo("₹0.00");
    }

    @Test
    @DisplayName("Validate monetary amounts")
    void testIsValidAmount() {
        assertThat(MoneyUtils.isValidAmount(new BigDecimal("100.00"))).isTrue();
        assertThat(MoneyUtils.isValidAmount(new BigDecimal("0.01"))).isTrue();
        assertThat(MoneyUtils.isValidAmount(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtils.isValidAmount(new BigDecimal("-10.00"))).isFalse();
        assertThat(MoneyUtils.isValidAmount(new BigDecimal("10.005"))).isFalse(); // > 2 decimal places
        assertThat(MoneyUtils.isValidAmount(null)).isFalse();
    }

    @Test
    @DisplayName("Round amounts to 2 decimal places using HALF_UP")
    void testRound() {
        assertThat(MoneyUtils.round(new BigDecimal("10.555"))).isEqualTo(new BigDecimal("10.56"));
        assertThat(MoneyUtils.round(new BigDecimal("10.554"))).isEqualTo(new BigDecimal("10.55"));
        assertThat(MoneyUtils.round(null)).isEqualTo(new BigDecimal("0.00"));
    }
}
