package com.trustpay.guardian.fraud.engine;

/**
 * Value object representing a triggered fraud rule hit.
 */
public record RuleHit(
        String ruleCode,
        int points,
        String explanation
) {
    public String getRuleCode() {
        return ruleCode;
    }

    public int getPoints() {
        return points;
    }

    public String getExplanation() {
        return explanation;
    }
}
