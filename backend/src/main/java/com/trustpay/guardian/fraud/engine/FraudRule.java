package com.trustpay.guardian.fraud.engine;

import com.trustpay.guardian.fraud.TransactionContext;

import java.util.Optional;

/**
 * Strategy interface for individual fraud detection rules.
 */
public interface FraudRule {

    /**
     * Unique business rule code (e.g., R1, R2, etc.).
     */
    String code();

    /**
     * Evaluates the transaction context against this rule's heuristics.
     *
     * @param context transaction and actor context
     * @return an Optional containing RuleHit if triggered, or empty if passed
     */
    Optional<RuleHit> evaluate(TransactionContext context);
}
