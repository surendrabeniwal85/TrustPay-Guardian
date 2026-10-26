package com.trustpay.guardian.fraud;

import com.trustpay.guardian.transaction.TransactionStatus;

import java.util.List;

public class FraudEvaluationResult {

    private final int totalRiskScore;
    private final RiskLevel riskLevel;
    private final TransactionStatus proposedStatus;
    private final List<FraudRuleHitResult> hits;
    private final String summary;

    public FraudEvaluationResult(int totalRiskScore, RiskLevel riskLevel, TransactionStatus proposedStatus,
                                 List<FraudRuleHitResult> hits, String summary) {
        this.totalRiskScore = totalRiskScore;
        this.riskLevel = riskLevel;
        this.proposedStatus = proposedStatus;
        this.hits = hits;
        this.summary = summary;
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
