package com.trustpay.guardian.admin.dto;

import java.math.BigDecimal;

public record PublicStatsResponse(
        long totalTransfers,
        BigDecimal totalVolume,
        long totalUsers,
        long alertsAnalyzed,
        long totalSimulatedTransfers,
        BigDecimal totalSimulatedVolume,
        long totalProtectedUsers,
        long fraudAlertsAnalyzed
) {
    public PublicStatsResponse(long totalTransfers, BigDecimal totalVolume, long totalUsers, long alertsAnalyzed) {
        this(totalTransfers, totalVolume, totalUsers, alertsAnalyzed, totalTransfers, totalVolume, totalUsers, alertsAnalyzed);
    }
}
