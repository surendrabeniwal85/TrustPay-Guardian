package com.trustpay.guardian.fraud;

/**
 * Strategy contract for assessing risk on transactions.
 */
public interface RiskAssessor {

    RiskDecision assess(TransactionContext ctx);
}
