package com.trustpay.guardian.fraud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "fraud_alert_hits")
public class FraudAlertHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private FraudAlert alert;

    @Column(name = "rule_code", nullable = false, length = 30)
    private String ruleCode;

    @Column(name = "score_contribution", nullable = false)
    private int scoreContribution;

    @Column(name = "reason_explanation", nullable = false, length = 255)
    private String reasonExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FraudAlertHit() {
    }

    public FraudAlertHit(FraudAlert alert, String ruleCode, int scoreContribution, String reasonExplanation) {
        this.alert = alert;
        this.ruleCode = ruleCode;
        this.scoreContribution = scoreContribution;
        this.reasonExplanation = reasonExplanation;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FraudAlert getAlert() {
        return alert;
    }

    public void setAlert(FraudAlert alert) {
        this.alert = alert;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public int getScoreContribution() {
        return scoreContribution;
    }

    public void setScoreContribution(int scoreContribution) {
        this.scoreContribution = scoreContribution;
    }

    public String getReasonExplanation() {
        return reasonExplanation;
    }

    public void setReasonExplanation(String reasonExplanation) {
        this.reasonExplanation = reasonExplanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FraudAlertHit that = (FraudAlertHit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
