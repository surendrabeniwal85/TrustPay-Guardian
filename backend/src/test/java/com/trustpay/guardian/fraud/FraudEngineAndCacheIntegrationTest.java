package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.fraud.engine.FraudEngine;
import com.trustpay.guardian.transaction.TransactionStatus;
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
class FraudEngineAndCacheIntegrationTest {

    @Autowired
    private FraudEngine fraudEngine;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void resetState() {
        FraudRule r1 = fraudRuleRepository.findByRuleCode("R1").orElse(null);
        if (r1 != null && !r1.isEnabled()) {
            r1.setEnabled(true);
            fraudRuleRepository.saveAndFlush(r1);
        }
        fraudRuleConfigService.clearCache();
    }

    @Test
    @DisplayName("Disabling a rule in the database changes the next score after cache clear")
    void disablingRuleInDatabaseChangesScore() {
        User sender = new User("Alice", "alice_cache@test.com", "9000000010", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG_CACHE_01", AccountType.SAVINGS, new BigDecimal("200000.00"), new BigDecimal("100000.00"));
        Account dst = new Account(sender, "TPG_CACHE_02", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00"));

        // Day transfer of 50,000 (triggers R1 high amount = 30 pts)
        TransactionContext context = new TransactionContext(
                src,
                dst,
                new BigDecimal("50000.00"),
                sender,
                Instant.parse("2026-10-26T12:00:00Z"),
                Optional.empty(),
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                0
        );

        // 1. Initial evaluation with R1 enabled -> score >= 30
        RiskDecision initialDecision = fraudEngine.assess(context);
        assertThat(initialDecision.getTotalRiskScore()).isGreaterThanOrEqualTo(30);
        assertThat(initialDecision.getRuleHits()).anyMatch(hit -> "R1".equals(hit.ruleCode()));

        // 2. Disable R1 in database
        FraudRule r1 = fraudRuleRepository.findByRuleCode("R1").orElseThrow();
        r1.setEnabled(false);
        fraudRuleRepository.save(r1);

        // 3. Invalidate cache
        fraudRuleConfigService.clearCache();

        // 4. Re-evaluate identical transaction -> R1 should not trigger
        RiskDecision updatedDecision = fraudEngine.assess(context);
        assertThat(updatedDecision.getRuleHits()).noneMatch(hit -> "R1".equals(hit.ruleCode()));
        assertThat(updatedDecision.getTotalRiskScore()).isLessThan(initialDecision.getTotalRiskScore());
    }

    @Test
    @DisplayName("Execution time is well below 200 ms and threshold bands apply correctly")
    void scoringRunsUnder200MsAndAppliesBands() {
        User sender = new User("Bob", "bob_perf@test.com", "9000000011", "hash", Role.CUSTOMER);
        Account src = new Account(sender, "TPG_PERF_01", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00"));
        Account dst = new Account(sender, "TPG_PERF_02", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00"));

        TransactionContext context = new TransactionContext(
                src,
                dst,
                new BigDecimal("500.00"),
                sender,
                Instant.parse("2026-10-26T14:00:00Z"),
                Optional.empty(),
                0L,
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                5L,
                0
        );

        long start = System.nanoTime();
        RiskDecision decision = fraudEngine.assess(context);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(200);
        assertThat(decision.getTotalRiskScore()).isEqualTo(0);
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(decision.getProposedStatus()).isEqualTo(TransactionStatus.APPROVED_SIMULATED);
    }
}
