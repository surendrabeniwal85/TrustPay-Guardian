package com.trustpay.guardian.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    private String destinationAccountNumber;

    private Long beneficiaryId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Transfer amount must be at least ₹1.00")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {
    }

    public TransferRequest(Long sourceAccountId, String destinationAccountNumber, BigDecimal amount) {
        this(sourceAccountId, destinationAccountNumber, amount, null);
    }

    public TransferRequest(Long sourceAccountId, String destinationAccountNumber, BigDecimal amount, String description) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    public TransferRequest(Long sourceAccountId, Long beneficiaryId, BigDecimal amount, String description) {
        this.sourceAccountId = sourceAccountId;
        this.beneficiaryId = beneficiaryId;
        this.amount = amount;
        this.description = description;
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
