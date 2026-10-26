package com.trustpay.guardian.fraud.dto;

public record FraudStatsResponse(
        long totalAlerts,
        long openAlerts,
        long inReviewAlerts,
        long resolvedReleased,
        long resolvedBlocked,
        double averageRiskScore
) {
}
