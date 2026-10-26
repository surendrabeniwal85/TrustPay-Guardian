package com.trustpay.guardian.audit;

/**
 * System audit action types for administrative, security, and fraud operations.
 */
public enum AuditAction {
    USER_STATUS_CHANGE,
    USER_ROLE_CHANGE,
    FRAUD_RULE_UPDATE,
    SYSTEM_SETTING_UPDATE,
    ALERT_DECISION_RELEASE,
    ALERT_DECISION_BLOCK,
    ALERT_OPENED,
    ACCOUNT_FREEZE,
    ACCOUNT_UNFREEZE
}
