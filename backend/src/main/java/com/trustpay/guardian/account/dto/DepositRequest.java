package com.trustpay.guardian.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DepositRequest {

    @NotNull(message = "Destination account ID is required")
    private Long destinationAccountId;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "1.00", message = "Deposit amount must be at least ₹1.00")
    private BigDecimal amount;

    public DepositRequest() {
    }

    public DepositRequest(Long destinationAccountId, BigDecimal amount) {
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
