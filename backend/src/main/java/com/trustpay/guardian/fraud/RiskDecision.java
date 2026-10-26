package com.trustpay.guardian.fraud;

import com.trustpay.guardian.transaction.TransactionStatus;

import java.util.Collections;
import java.util.List;

public class RiskDecision {

    private final int totalRiskScore;
    private final RiskLevel riskLevel;
    private final TransactionStatus proposedStatus;
    private final List<FraudRuleHitResult> hits;
    private final String summary;

    public RiskDecision(int totalRiskScore, RiskLevel riskLevel, TransactionStatus proposedStatus,
                        List<FraudRuleHitResult> hits, String summary) {
        this.totalRiskScore = totalRiskScore;
        this.riskLevel = riskLevel;
        this.proposedStatus = proposedStatus;
        this.hits = hits != null ? hits : Collections.emptyList();
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

    public List<FraudRuleHitResult> getHits() {
        return hits;
    }

    public String getSummary() {
        return summary;
    }
}
