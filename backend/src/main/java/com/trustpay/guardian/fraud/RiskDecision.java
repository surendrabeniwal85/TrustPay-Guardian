package com.trustpay.guardian.fraud;

import com.trustpay.guardian.fraud.engine.RuleHit;
import com.trustpay.guardian.transaction.TransactionStatus;

import java.util.Collections;
import java.util.List;

public class RiskDecision {

    private final int totalRiskScore;
    private final RiskLevel riskLevel;
    private final TransactionStatus proposedStatus;
    private final List<RuleHit> ruleHits;
    private final String summary;

    public RiskDecision(int totalRiskScore, RiskLevel riskLevel, TransactionStatus proposedStatus,
                        List<RuleHit> ruleHits, String summary) {
        this.totalRiskScore = totalRiskScore;
        this.riskLevel = riskLevel;
        this.proposedStatus = proposedStatus;
        this.ruleHits = ruleHits != null ? ruleHits : Collections.emptyList();
        this.summary = summary;
    }

    public static RiskDecision approvedZeroScore() {
        return new RiskDecision(0, RiskLevel.LOW, TransactionStatus.APPROVED_SIMULATED,
                Collections.emptyList(), "Low Risk - Score: 0/100. Action: APPROVED_SIMULATED");
    }

    public int getTotalRiskScore() {
        return totalRiskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public TransactionStatus getProposedStatus() {
        return proposedStatus;
    }

    public List<RuleHit> getRuleHits() {
        return ruleHits;
    }

    /**
     * Compatibility bridge for existing callers.
     */
    public List<FraudRuleHitResult> getHits() {
        return ruleHits.stream()
                .map(rh -> FraudRuleHitResult.hit(rh.ruleCode(), rh.points(), rh.explanation()))
                .toList();
    }

    public String getSummary() {
        return summary;
    }
}
