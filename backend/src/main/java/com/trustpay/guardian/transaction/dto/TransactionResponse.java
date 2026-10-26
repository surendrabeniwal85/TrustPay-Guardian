package com.trustpay.guardian.transaction.dto;

import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponse {

    private Long id;
    private String referenceId;
    private String idempotencyKey;
    private Long sourceAccountId;
    private String sourceAccountNumber;
    private String sourceMaskedAccountNumber;
    private Long destinationAccountId;
    private String destinationAccountNumber;
    private String destinationMaskedAccountNumber;
    private String destinationAccountHolderName;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private Integer riskScore;
    private String failureReason;
    private Instant createdAt;
    private Instant completedAt;

    public TransactionResponse() {
    }

    public static TransactionResponse fromEntity(Transaction tx) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(tx.getId());
        dto.setReferenceId(tx.getReferenceId());
        dto.setIdempotencyKey(tx.getIdempotencyKey());

        if (tx.getSourceAccount() != null) {
            dto.setSourceAccountId(tx.getSourceAccount().getId());
            dto.setSourceAccountNumber(tx.getSourceAccount().getAccountNumber());
            dto.setSourceMaskedAccountNumber(tx.getSourceAccount().getMaskedAccountNumber());
        }

        if (tx.getDestinationAccount() != null) {
            dto.setDestinationAccountId(tx.getDestinationAccount().getId());
            dto.setDestinationAccountNumber(tx.getDestinationAccount().getAccountNumber());
            dto.setDestinationMaskedAccountNumber(tx.getDestinationAccount().getMaskedAccountNumber());
            if (tx.getDestinationAccount().getUser() != null) {
                dto.setDestinationAccountHolderName(tx.getDestinationAccount().getUser().getFullName());
            }
        }

        dto.setAmount(tx.getAmount());
        dto.setCurrency(tx.getCurrency());
        dto.setType(tx.getType());
        dto.setStatus(tx.getStatus());
        dto.setRiskScore(tx.getRiskScore());
        dto.setFailureReason(tx.getFailureReason());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setCompletedAt(tx.getCompletedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getSourceMaskedAccountNumber() {
        return sourceMaskedAccountNumber;
    }

    public void setSourceMaskedAccountNumber(String sourceMaskedAccountNumber) {
        this.sourceMaskedAccountNumber = sourceMaskedAccountNumber;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public String getDestinationMaskedAccountNumber() {
        return destinationMaskedAccountNumber;
    }

    public void setDestinationMaskedAccountNumber(String destinationMaskedAccountNumber) {
        this.destinationMaskedAccountNumber = destinationMaskedAccountNumber;
    }

    public String getDestinationAccountHolderName() {
        return destinationAccountHolderName;
    }

    public void setDestinationAccountHolderName(String destinationAccountHolderName) {
        this.destinationAccountHolderName = destinationAccountHolderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
