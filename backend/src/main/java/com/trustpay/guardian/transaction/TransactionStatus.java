package com.trustpay.guardian.transaction;

/**
 * Execution and fraud review lifecycle status for transactions.
 */
public enum TransactionStatus {
    PENDING_REVIEW,
    APPROVED_SIMULATED,
    BLOCKED,
    REJECTED,
    CANCELLED
}
