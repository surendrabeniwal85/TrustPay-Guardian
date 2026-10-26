package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public class TransactionContext {

    private final Account sourceAccount;
    private final Account destinationAccount;
    private final BigDecimal amount;
    private final User sender;
    private final Instant timestamp;
    private final Optional<Beneficiary> beneficiary;
    private final long recent10MinTransfersCount;
    private final BigDecimal todayOutgoingTotal;
    private final BigDecimal thirtyDayAverageAmount;
    private final long thirtyDayTransfersCount;
    private final int failedLoginAttempts;

    public TransactionContext(
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            User sender,
            Instant timestamp,
            Optional<Beneficiary> beneficiary,
            long recent10MinTransfersCount,
            BigDecimal todayOutgoingTotal,
            BigDecimal thirtyDayAverageAmount,
            long thirtyDayTransfersCount,
            int failedLoginAttempts) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.sender = sender;
        this.timestamp = timestamp;
        this.beneficiary = beneficiary;
        this.recent10MinTransfersCount = recent10MinTransfersCount;
        this.todayOutgoingTotal = todayOutgoingTotal;
        this.thirtyDayAverageAmount = thirtyDayAverageAmount;
        this.thirtyDayTransfersCount = thirtyDayTransfersCount;
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public User getSender() {
        return sender;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Optional<Beneficiary> getBeneficiary() {
        return beneficiary;
    }

    public long getRecent10MinTransfersCount() {
        return recent10MinTransfersCount;
    }

    public BigDecimal getTodayOutgoingTotal() {
        return todayOutgoingTotal;
    }

    public BigDecimal getThirtyDayAverageAmount() {
        return thirtyDayAverageAmount;
    }

    public long getThirtyDayTransfersCount() {
        return thirtyDayTransfersCount;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
}
