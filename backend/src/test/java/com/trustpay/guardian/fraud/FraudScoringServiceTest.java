package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FraudScoringServiceTest {

    @Autowired
    private FraudScoringService fraudScoringService;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();
    }

    @Test
    @DisplayName("Normal daytime low-amount transfer produces LOW risk and APPROVED_SIMULATED")
    void normalTransferProducesLowRisk() {
        User sender = new User("Alice", "alice@test.com", "9000000001", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG_TEST_01", AccountType.SAVINGS, new BigDecimal("20000.00"), new BigDecimal("100000.00"));
        Account dst = new Account(sender, "TPG_TEST_02", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00"));

        FraudRuleEvaluationContext context = new FraudRuleEvaluationContext(
                src,
                dst,
                new BigDecimal("500.00"),
                sender,
                Instant.parse("2026-10-17T14:30:00Z"),
                Optional.empty(),
                0L,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                5L,
                0
        );

        FraudEvaluationResult result = fraudScoringService.evaluateTransaction(context);

        assertThat(result.getTotalRiskScore()).isLessThan(40);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.getProposedStatus()).isEqualTo(com.trustpay.guardian.transaction.TransactionStatus.APPROVED_SIMULATED);
    }

    @Test
    @DisplayName("Very high amount (R1) triggers review threshold (Score >= 40)")
    void highAmountTriggersReview() {
        User sender = new User("Bob", "bob@test.com", "9000000002", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG_TEST_03", AccountType.SAVINGS, new BigDecimal("150000.00"), new BigDecimal("60000.00"));
        Account dst = new Account(sender, "TPG_TEST_04", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00"));

        FraudRuleEvaluationContext context = new FraudRuleEvaluationContext(
                src,
                dst,
                new BigDecimal("60000.00"), // Exceeds 50,000 R1 threshold (30 pts) + R6 proximity (10 pts) = 40 pts
                sender,
                Instant.parse("2026-10-17T11:00:00Z"),
                Optional.empty(),
                0L,
                new BigDecimal("60000.00"),
                new BigDecimal("50000.00"),
                10L,
                0
        );

        FraudEvaluationResult result = fraudScoringService.evaluateTransaction(context);

        assertThat(result.getTotalRiskScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.getHits()).anyMatch(hit -> "R1".equals(hit.getRuleCode()));
        assertThat(result.getProposedStatus()).isIn(
                com.trustpay.guardian.transaction.TransactionStatus.PENDING_REVIEW,
                com.trustpay.guardian.transaction.TransactionStatus.BLOCKED
        );
    }

    @Test
    @DisplayName("Multiple fraud rule hits accumulate and clamp at 100 with BLOCKED status")
    void compoundRiskTriggersBlock() {
        User sender = new User("Charlie", "charlie@test.com", "9000000003", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG_TEST_05", AccountType.SAVINGS, new BigDecimal("200000.00"), new BigDecimal("200000.00"));
        Account dst = new Account(sender, "TPG_TEST_06", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00"));

        FraudRuleEvaluationContext context = new FraudRuleEvaluationContext(
                src,
                dst,
                new BigDecimal("80000.00"), // R1 hit (40 pts)
                sender,
                Instant.parse("2026-10-17T03:15:00Z"), // Unusual hours (R3)
                Optional.empty(),
                5L, // Velocity (R2)
                new BigDecimal("185000.00"), // Proximity (R6)
                new BigDecimal("5000.00"), // Spike (R5)
                10L,
                4 // Failed logins (R7)
        );

        FraudEvaluationResult result = fraudScoringService.evaluateTransaction(context);

        // Clamped at 100
        assertThat(result.getTotalRiskScore()).isEqualTo(100);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.getProposedStatus()).isEqualTo(com.trustpay.guardian.transaction.TransactionStatus.BLOCKED);
        assertThat(result.getHits().size()).isGreaterThanOrEqualTo(3);
    }
}
