package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.fraud.engine.RuleHit;
import com.trustpay.guardian.fraud.rules.*;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FraudRuleBoundaryUnitTest {

    private FraudRuleConfigService configService;
    private User testUser;
    private Account srcAccount;
    private Account dstAccount;

    @BeforeEach
    void setUp() {
        configService = Mockito.mock(FraudRuleConfigService.class);
        when(configService.isRuleEnabled(any())).thenReturn(true);

        testUser = new User("Test User", "test@example.com", "9876543210", "hashed", Role.CUSTOMER);
        srcAccount = new Account(testUser, "ACC-SRC-01", AccountType.SAVINGS, new BigDecimal("100000.00"), new BigDecimal("100000.00"));
        dstAccount = new Account(testUser, "ACC-DST-01", AccountType.SAVINGS, new BigDecimal("50000.00"), new BigDecimal("100000.00"));
    }

    private TransactionContext createContext(
            BigDecimal amount,
            Instant timestamp,
            Optional<Beneficiary> beneficiary,
            long recentTransfers10Min,
            BigDecimal todayOutgoing,
            BigDecimal thirtyDayAvg,
            long thirtyDayCount,
            int failedLogins) {

        return new TransactionContext(
                srcAccount,
                dstAccount,
                amount,
                testUser,
                timestamp != null ? timestamp : Instant.now(),
                beneficiary != null ? beneficiary : Optional.empty(),
                recentTransfers10Min,
                todayOutgoing != null ? todayOutgoing : BigDecimal.ZERO,
                thirtyDayAvg != null ? thirtyDayAvg : BigDecimal.ZERO,
                thirtyDayCount,
                failedLogins
        );
    }

    @Nested
    @DisplayName("R1 - HIGH_AMOUNT Rule Boundaries (Threshold: ₹50,000.00, 30 points)")
    class R1Boundaries {
        private R1_HighAmountRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R1"), anyInt())).thenReturn(30);
            when(configService.getBigDecimalParameter(eq("R1"), eq("threshold_amount"), any()))
                    .thenReturn(new BigDecimal("50000.00"));
            rule = new R1_HighAmountRuleStrategy(configService);
        }

        @Test
        @DisplayName("Amount ₹49,999.99 (just below) passes")
        void justBelowThresholdPasses() {
            TransactionContext ctx = createContext(new BigDecimal("49999.99"), null, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Amount ₹50,000.00 (exact boundary) triggers")
        void atThresholdTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("50000.00"), null, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(30);
            assertThat(result.get().ruleCode()).isEqualTo("R1");
        }

        @Test
        @DisplayName("Amount ₹50,000.01 (just above) triggers")
        void justAboveThresholdTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("50000.01"), null, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("R2 - VELOCITY Rule Boundaries (Threshold: >= 5 outgoing in 10 mins including current, 25 points)")
    class R2Boundaries {
        private R2_VelocityRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R2"), anyInt())).thenReturn(25);
            when(configService.getIntParameter(eq("R2"), eq("max_transfers"), anyInt())).thenReturn(4);
            rule = new R2_VelocityRuleStrategy(configService);
        }

        @Test
        @DisplayName("3 prior transfers (4 total including current, just below) passes")
        void threePriorTransfersPasses() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 3, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("4 prior transfers (5 total including current, exact boundary) triggers")
        void fourPriorTransfersTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 4, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(25);
            assertThat(result.get().ruleCode()).isEqualTo("R2");
        }

        @Test
        @DisplayName("5 prior transfers (6 total including current, just above) triggers")
        void fivePriorTransfersTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 5, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("R3 - UNUSUAL_HOUR Rule Boundaries (00:00 to 04:59 Asia/Kolkata, 15 points)")
    class R3Boundaries {
        private R3_UnusualHourRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R3"), anyInt())).thenReturn(15);
            when(configService.getIntParameter(eq("R3"), eq("start_hour"), anyInt())).thenReturn(0);
            when(configService.getIntParameter(eq("R3"), eq("end_hour"), anyInt())).thenReturn(4);
            rule = new R3_UnusualHourRuleStrategy(configService);
        }

        @Test
        @DisplayName("23:59:59 Asia/Kolkata (just before window) passes")
        void justBeforeWindowPasses() {
            // 23:59:59 IST = 18:29:59 UTC
            Instant time = Instant.parse("2026-10-25T18:29:59Z");
            TransactionContext ctx = createContext(new BigDecimal("500.00"), time, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("00:00:00 Asia/Kolkata (start of window) triggers")
        void startOfWindowTriggers() {
            // 00:00:00 IST = 18:30:00 UTC previous day
            Instant time = Instant.parse("2026-10-25T18:30:00Z");
            TransactionContext ctx = createContext(new BigDecimal("500.00"), time, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(15);
            assertThat(result.get().ruleCode()).isEqualTo("R3");
        }

        @Test
        @DisplayName("04:59:59 Asia/Kolkata (end of window) triggers")
        void endOfWindowTriggers() {
            // 04:59:59 IST = 23:29:59 UTC previous day
            Instant time = Instant.parse("2026-10-25T23:29:59Z");
            TransactionContext ctx = createContext(new BigDecimal("500.00"), time, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(15);
        }

        @Test
        @DisplayName("05:00:00 Asia/Kolkata (just after window) passes")
        void justAfterWindowPasses() {
            // 05:00:00 IST = 23:30:00 UTC previous day
            Instant time = Instant.parse("2026-10-25T23:30:00Z");
            TransactionContext ctx = createContext(new BigDecimal("500.00"), time, null, 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("R4 - NEW_BENEFICIARY_LARGE Rule Boundaries (< 24 hrs and >= ₹20,000.00, 30 points)")
    class R4Boundaries {
        private R4_NewBeneficiaryLargeRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R4"), anyInt())).thenReturn(30);
            when(configService.getBigDecimalParameter(eq("R4"), eq("threshold_amount"), any()))
                    .thenReturn(new BigDecimal("20000.00"));
            when(configService.getIntParameter(eq("R4"), eq("window_hours"), anyInt())).thenReturn(24);
            rule = new R4_NewBeneficiaryLargeRuleStrategy(configService);
        }

        @Test
        @DisplayName("Beneficiary added 10 hours ago (< 24 hrs), transfer ₹19,999.99 (just below amount) passes")
        void youngBeneficiaryLowAmountPasses() {
            Instant now = Instant.now();
            Beneficiary b = new Beneficiary(testUser, dstAccount, "Friend");
            b.setCreatedAt(now.minus(Duration.ofHours(10)));

            TransactionContext ctx = createContext(new BigDecimal("19999.99"), now, Optional.of(b), 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Beneficiary added 23 hours ago (< 24 hrs), transfer ₹20,000.00 (exact threshold) triggers")
        void youngBeneficiaryThresholdAmountTriggers() {
            Instant now = Instant.now();
            Beneficiary b = new Beneficiary(testUser, dstAccount, "Friend");
            b.setCreatedAt(now.minus(Duration.ofHours(23)));

            TransactionContext ctx = createContext(new BigDecimal("20000.00"), now, Optional.of(b), 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(30);
            assertThat(result.get().ruleCode()).isEqualTo("R4");
        }

        @Test
        @DisplayName("Beneficiary added 25 hours ago (>= 24 hrs), transfer ₹50,000.00 passes")
        void matureBeneficiaryHighAmountPasses() {
            Instant now = Instant.now();
            Beneficiary b = new Beneficiary(testUser, dstAccount, "Friend");
            b.setCreatedAt(now.minus(Duration.ofHours(25)));

            TransactionContext ctx = createContext(new BigDecimal("50000.00"), now, Optional.of(b), 0, null, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("R5 - AMOUNT_SPIKE Rule Boundaries (> 5x 30-day average, >= 3 history count, 20 points)")
    class R5Boundaries {
        private R5_AmountSpikeRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R5"), anyInt())).thenReturn(20);
            when(configService.getBigDecimalParameter(eq("R5"), eq("multiplier"), any()))
                    .thenReturn(new BigDecimal("5.0"));
            when(configService.getIntParameter(eq("R5"), eq("min_history_count"), anyInt())).thenReturn(3);
            rule = new R5_AmountSpikeRuleStrategy(configService);
        }

        @Test
        @DisplayName("History count = 2 (< 3 minimum required), even high spike passes")
        void insufficientHistoryPasses() {
            TransactionContext ctx = createContext(new BigDecimal("50000.00"), null, null, 0, null,
                    new BigDecimal("1000.00"), 2, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("History count = 3, avg ₹1,000 (5x = ₹5,000.00), transfer ₹5,000.00 (not strictly exceeding) passes")
        void exactFiveTimesPasses() {
            TransactionContext ctx = createContext(new BigDecimal("50000.00"), null, null, 0, null,
                    new BigDecimal("10000.00"), 3, 0);
            // 5x 10,000 = 50,000. Amount = 50,000 -> does not exceed 5x
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("History count = 3, avg ₹1,000 (5x = ₹5,000.00), transfer ₹5,000.01 (strictly exceeding) triggers")
        void strictlyExceedingFiveTimesTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("50000.01"), null, null, 0, null,
                    new BigDecimal("10000.00"), 3, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(20);
            assertThat(result.get().ruleCode()).isEqualTo("R5");
        }
    }

    @Nested
    @DisplayName("R6 - DAILY_LIMIT_PROXIMITY Rule Boundaries (>= 90% of daily limit, 10 points)")
    class R6Boundaries {
        private R6_DailyLimitProximityRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R6"), anyInt())).thenReturn(10);
            when(configService.getBigDecimalParameter(eq("R6"), eq("proximity_ratio"), any()))
                    .thenReturn(new BigDecimal("0.90"));
            rule = new R6_DailyLimitProximityRuleStrategy(configService);
        }

        @Test
        @DisplayName("Daily limit ₹100,000, cumulative total ₹89,999.99 (89.9999%, just below) passes")
        void justBelowNinetyPercentPasses() {
            TransactionContext ctx = createContext(new BigDecimal("89999.99"), null, null, 0, BigDecimal.ZERO, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Daily limit ₹100,000, cumulative total ₹90,000.00 (exact 90%) triggers")
        void exactNinetyPercentTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("90000.00"), null, null, 0, BigDecimal.ZERO, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(10);
            assertThat(result.get().ruleCode()).isEqualTo("R6");
        }

        @Test
        @DisplayName("Daily limit ₹100,000, cumulative total ₹90,000.01 (just above 90%) triggers")
        void justAboveNinetyPercentTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("90000.01"), null, null, 0, BigDecimal.ZERO, null, 0, 0);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("R7 - RECENT_FAILED_LOGINS Rule Boundaries (>= 3 failed logins, 15 points)")
    class R7Boundaries {
        private R7_RecentFailedLoginsRuleStrategy rule;

        @BeforeEach
        void init() {
            when(configService.getRulePoints(eq("R7"), anyInt())).thenReturn(15);
            when(configService.getIntParameter(eq("R7"), eq("min_failed_attempts"), anyInt())).thenReturn(3);
            rule = new R7_RecentFailedLoginsRuleStrategy(configService);
        }

        @Test
        @DisplayName("2 failed logins (just below) passes")
        void twoFailedLoginsPasses() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 0, null, null, 0, 2);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("3 failed logins (exact threshold) triggers")
        void threeFailedLoginsTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 0, null, null, 0, 3);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(15);
            assertThat(result.get().ruleCode()).isEqualTo("R7");
        }

        @Test
        @DisplayName("4 failed logins (just above) triggers")
        void fourFailedLoginsTriggers() {
            TransactionContext ctx = createContext(new BigDecimal("500.00"), null, null, 0, null, null, 0, 4);
            Optional<RuleHit> result = rule.evaluate(ctx);
            assertThat(result).isPresent();
            assertThat(result.get().points()).isEqualTo(15);
        }
    }
}
