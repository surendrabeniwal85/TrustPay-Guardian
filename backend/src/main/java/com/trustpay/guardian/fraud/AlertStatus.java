package com.trustpay.guardian.fraud;

/**
 * Analyst adjudication workflow status for fraud alerts.
 */
public enum AlertStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED_RELEASED,
    RESOLVED_BLOCKED
}
