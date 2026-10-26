package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public class FraudRuleEvaluationContext {

    private final Account sourceAccount;
    private final Account destinationAccount;
    private final BigDecimal transferAmount;
    private final User senderUser;
    private final Instant transferTime;
    private final Optional<Beneficiary> beneficiary;
    private final long recentTransfersCountIn10Min;
    private final BigDecimal todayTotalOutgoing;
    private final BigDecimal thirtyDayAverageAmount;
    private final long thirtyDayCount;
    private final int recentFailedLoginAttempts;

    public FraudRuleEvaluationContext(
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal transferAmount,
            User senderUser,
            Instant transferTime,
            Optional<Beneficiary> beneficiary,
            long recentTransfersCountIn10Min,
            BigDecimal todayTotalOutgoing,
            BigDecimal thirtyDayAverageAmount,
            long thirtyDayCount,
            int recentFailedLoginAttempts) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.transferAmount = transferAmount;
        this.senderUser = senderUser;
        this.transferTime = transferTime;
        this.beneficiary = beneficiary;
        this.recentTransfersCountIn10Min = recentTransfersCountIn10Min;
        this.todayTotalOutgoing = todayTotalOutgoing != null ? todayTotalOutgoing : BigDecimal.ZERO;
        this.thirtyDayAverageAmount = thirtyDayAverageAmount != null ? thirtyDayAverageAmount : BigDecimal.ZERO;
        this.thirtyDayCount = thirtyDayCount;
        this.recentFailedLoginAttempts = recentFailedLoginAttempts;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public User getSenderUser() {
        return senderUser;
    }

    public Instant getTransferTime() {
        return transferTime;
    }

    public Optional<Beneficiary> getBeneficiary() {
        return beneficiary;
    }

    public long getRecentTransfersCountIn10Min() {
        return recentTransfersCountIn10Min;
    }

    public BigDecimal getTodayTotalOutgoing() {
        return todayTotalOutgoing;
    }

    public BigDecimal getThirtyDayAverageAmount() {
        return thirtyDayAverageAmount;
    }

    public long getThirtyDayCount() {
        return thirtyDayCount;
    }

    public int getRecentFailedLoginAttempts() {
        return recentFailedLoginAttempts;
    }
}
