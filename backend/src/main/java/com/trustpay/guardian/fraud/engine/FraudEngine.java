package com.trustpay.guardian.fraud.engine;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.RiskAssessor;
import com.trustpay.guardian.fraud.RiskDecision;
import com.trustpay.guardian.fraud.RiskLevel;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.transaction.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core deterministic, explainable fraud-risk engine.
 * Evaluates individual FraudRule strategies, computes capped composite risk scores,
 * and applies decision bands based on dynamic system thresholds.
 */
@Component
@Primary
public class FraudEngine implements RiskAssessor {

    private static final Logger log = LoggerFactory.getLogger(FraudEngine.class);

    private final List<FraudRule> rules;
    private final FraudRuleConfigService configService;

    public FraudEngine(List<FraudRule> rules, FraudRuleConfigService configService) {
        this.rules = rules != null ? rules : List.of();
        this.configService = configService;
    }

    @Override
    public RiskDecision assess(TransactionContext context) {
        long startTime = System.nanoTime();

        int reviewThreshold = configService.getReviewThreshold();
        int blockThreshold = configService.getBlockThreshold();

        int totalScore = 0;
        List<RuleHit> hits = new ArrayList<>();

        for (FraudRule rule : rules) {
            Optional<RuleHit> optHit = rule.evaluate(context);
            if (optHit.isPresent()) {
                RuleHit hit = optHit.get();
                totalScore += hit.points();
                hits.add(hit);
                log.info("Fraud Rule [{}] TRIGGERED with [{}] points: {}", hit.ruleCode(), hit.points(), hit.explanation());
            }
        }

        // Clamp composite score between 0 and 100
        int clampedScore = Math.min(100, Math.max(0, totalScore));

        RiskLevel riskLevel;
        TransactionStatus proposedStatus;

        if (clampedScore < reviewThreshold) {
            riskLevel = RiskLevel.LOW;
            proposedStatus = TransactionStatus.APPROVED_SIMULATED;
        } else if (clampedScore < blockThreshold) {
            riskLevel = RiskLevel.MEDIUM;
            proposedStatus = TransactionStatus.PENDING_REVIEW;
        } else {
            riskLevel = RiskLevel.HIGH;
            proposedStatus = TransactionStatus.BLOCKED;
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        log.info("FraudEngine evaluation completed in {} ms. Score: {}/100, Level: {}, Status: {}, Hits: {}",
                elapsedMs, clampedScore, riskLevel, proposedStatus, hits.size());

        String summary = String.format("Score: %d/100 (%s). Hits: %d rule(s) triggered. Action: %s. Evaluated in %d ms.",
                clampedScore, riskLevel, hits.size(), proposedStatus, elapsedMs);

        return new RiskDecision(clampedScore, riskLevel, proposedStatus, hits, summary);
    }

    public List<FraudRule> getRules() {
        return rules;
    }
}
