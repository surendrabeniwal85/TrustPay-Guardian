package com.trustpay.guardian.fraud.dto;

import com.trustpay.guardian.auth.dto.UserSummaryDto;
import com.trustpay.guardian.fraud.AlertStatus;
import com.trustpay.guardian.fraud.FraudAlert;
import com.trustpay.guardian.fraud.RiskLevel;
import com.trustpay.guardian.transaction.dto.TransactionResponse;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class FraudAlertResponse {

    private Long id;
    private TransactionResponse transaction;
    private int riskScore;
    private RiskLevel riskLevel;
    private AlertStatus status;
    private UserSummaryDto assignedTo;
    private String decisionNotes;
    private Instant createdAt;
    private Instant resolvedAt;
    private List<FraudAlertHitResponse> hits;

    public FraudAlertResponse() {
    }

    public static FraudAlertResponse fromEntity(FraudAlert alert) {
        FraudAlertResponse dto = new FraudAlertResponse();
        dto.setId(alert.getId());
        dto.setTransaction(TransactionResponse.fromEntity(alert.getTransaction()));
        dto.setRiskScore(alert.getRiskScore());
        dto.setRiskLevel(alert.getRiskLevel());
        dto.setStatus(alert.getStatus());
        if (alert.getAssignedTo() != null) {
            dto.setAssignedTo(UserSummaryDto.fromEntity(alert.getAssignedTo()));
        }
        dto.setDecisionNotes(alert.getDecisionNotes());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setResolvedAt(alert.getResolvedAt());
        if (alert.getHits() != null) {
            dto.setHits(alert.getHits().stream()
                    .map(FraudAlertHitResponse::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionResponse getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionResponse transaction) {
        this.transaction = transaction;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public UserSummaryDto getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UserSummaryDto assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public void setDecisionNotes(String decisionNotes) {
        this.decisionNotes = decisionNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public List<FraudAlertHitResponse> getHits() {
        return hits;
    }

    public void setHits(List<FraudAlertHitResponse> hits) {
        this.hits = hits;
    }
}
