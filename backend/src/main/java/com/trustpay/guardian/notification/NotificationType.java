package com.trustpay.guardian.notification;

/**
 * Types of in-app notifications dispatched to users.
 */
public enum NotificationType {
    TRANSFER_RECEIVED,
    TRANSFER_SENT,
    TRANSFER_HELD,
    TRANSFER_BLOCKED,
    TRANSFER_RELEASED,
    ACCOUNT_FROZEN,
    ACCOUNT_UNFROZEN,
    ACCOUNT_CREATED,
    SECURITY_ALERT
}
