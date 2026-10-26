package com.trustpay.guardian.audit;

/**
 * Entity target types affected by audit actions.
 */
public enum AuditTargetType {
    USER,
    ACCOUNT,
    FRAUD_RULE,
    FRAUD_ALERT,
    SYSTEM_SETTING,
    TRANSACTION
}
