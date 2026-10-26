package com.trustpay.guardian.fraud;

public interface FraudRuleStrategy {

    String getRuleCode();

    FraudRuleHitResult evaluate(FraudRule rule, FraudRuleEvaluationContext context);
}
