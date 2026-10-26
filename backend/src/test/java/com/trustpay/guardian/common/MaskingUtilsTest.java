package com.trustpay.guardian.common;

import com.trustpay.guardian.common.util.MaskingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilsTest {

    @Test
    @DisplayName("Mask account number preserves prefix and last 4 digits")
    void testMaskAccountNumber() {
        assertThat(MaskingUtils.maskAccountNumber("TPG100000000001")).isEqualTo("TPG********0001");
        assertThat(MaskingUtils.maskAccountNumber("TPG1234567890")).isEqualTo("TPG******7890");
        assertThat(MaskingUtils.maskAccountNumber("1234")).isEqualTo("1234");
        assertThat(MaskingUtils.maskAccountNumber("")).isEqualTo("");
        assertThat(MaskingUtils.maskAccountNumber(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Mask email preserves first and last character of local part and domain")
    void testMaskEmail() {
        assertThat(MaskingUtils.maskEmail("john.doe@example.com")).isEqualTo("j***e@example.com");
        assertThat(MaskingUtils.maskEmail("ab@domain.com")).isEqualTo("a***@domain.com");
        assertThat(MaskingUtils.maskEmail("a@domain.com")).isEqualTo("a***@domain.com");
        assertThat(MaskingUtils.maskEmail("invalid-email")).isEqualTo("***");
        assertThat(MaskingUtils.maskEmail("")).isEqualTo("***");
        assertThat(MaskingUtils.maskEmail(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("Mask phone number preserves last 4 digits")
    void testMaskPhoneNumber() {
        assertThat(MaskingUtils.maskPhoneNumber("9876543210")).isEqualTo("******3210");
        assertThat(MaskingUtils.maskPhoneNumber("1234")).isEqualTo("****");
        assertThat(MaskingUtils.maskPhoneNumber("")).isEqualTo("");
        assertThat(MaskingUtils.maskPhoneNumber(null)).isEqualTo("");
    }
}
