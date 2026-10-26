package com.trustpay.guardian.fraud;

public class FraudRuleHitResult {

    private final boolean triggered;
    private final String ruleCode;
    private final int points;
    private final String explanation;

    public FraudRuleHitResult(boolean triggered, String ruleCode, int points, String explanation) {
        this.triggered = triggered;
        this.ruleCode = ruleCode;
        this.points = points;
        this.explanation = explanation;
    }

    public static FraudRuleHitResult hit(String ruleCode, int points, String explanation) {
        return new FraudRuleHitResult(true, ruleCode, points, explanation);
    }

    public static FraudRuleHitResult pass(String ruleCode) {
        return new FraudRuleHitResult(false, ruleCode, 0, null);
    }

    public boolean isTriggered() {
        return triggered;
    }

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
