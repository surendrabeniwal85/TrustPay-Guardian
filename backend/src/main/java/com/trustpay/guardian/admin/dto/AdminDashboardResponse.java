package com.trustpay.guardian.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardResponse {

    private long totalUsers;
    private long activeUsers;
    private long totalAccounts;
    private BigDecimal totalBalance;
    private long totalTransactions;
    private long approvedTransactions;
    private long heldTransactions;
    private long blockedTransactions;
    private long openAlerts;
    private long activeRules;
    private BigDecimal totalVolume;
    private Map<String, Long> alertsByStatus;
    private Map<String, Long> alertsByRule;
    private List<DailyTransactionStat> dailyTransactions;

    public record DailyTransactionStat(String date, long count) {}

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(
            long totalUsers,
            long activeUsers,
            long totalAccounts,
            BigDecimal totalBalance,
            long totalTransactions,
            long approvedTransactions,
            long heldTransactions,
            long blockedTransactions,
            long openAlerts,
            long activeRules,
            BigDecimal totalVolume,
            Map<String, Long> alertsByStatus,
            Map<String, Long> alertsByRule,
            List<DailyTransactionStat> dailyTransactions) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalAccounts = totalAccounts;
        this.totalBalance = totalBalance;
        this.totalTransactions = totalTransactions;
        this.approvedTransactions = approvedTransactions;
        this.heldTransactions = heldTransactions;
        this.blockedTransactions = blockedTransactions;
        this.openAlerts = openAlerts;
        this.activeRules = activeRules;
        this.totalVolume = totalVolume;
        this.alertsByStatus = alertsByStatus;
        this.alertsByRule = alertsByRule;
        this.dailyTransactions = dailyTransactions;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getApprovedTransactions() {
        return approvedTransactions;
    }

    public void setApprovedTransactions(long approvedTransactions) {
        this.approvedTransactions = approvedTransactions;
    }

    public long getHeldTransactions() {
        return heldTransactions;
    }

    public void setHeldTransactions(long heldTransactions) {
        this.heldTransactions = heldTransactions;
    }

    public long getBlockedTransactions() {
        return blockedTransactions;
    }

    public void setBlockedTransactions(long blockedTransactions) {
        this.blockedTransactions = blockedTransactions;
    }

    public long getOpenAlerts() {
        return openAlerts;
    }

    public void setOpenAlerts(long openAlerts) {
        this.openAlerts = openAlerts;
    }

    public long getActiveRules() {
        return activeRules;
    }

    public void setActiveRules(long activeRules) {
        this.activeRules = activeRules;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public Map<String, Long> getAlertsByStatus() {
        return alertsByStatus;
    }

    public void setAlertsByStatus(Map<String, Long> alertsByStatus) {
        this.alertsByStatus = alertsByStatus;
    }

    public Map<String, Long> getAlertsByRule() {
        return alertsByRule;
    }

    public void setAlertsByRule(Map<String, Long> alertsByRule) {
        this.alertsByRule = alertsByRule;
    }

    public List<DailyTransactionStat> getDailyTransactions() {
        return dailyTransactions;
    }

    public void setDailyTransactions(List<DailyTransactionStat> dailyTransactions) {
        this.dailyTransactions = dailyTransactions;
    }
}
