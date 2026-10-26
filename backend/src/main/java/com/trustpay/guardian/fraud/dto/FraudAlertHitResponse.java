package com.trustpay.guardian.fraud.dto;

import com.trustpay.guardian.fraud.FraudAlertHit;

public class FraudAlertHitResponse {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private int scoreContribution;
    private String reasonExplanation;

    public FraudAlertHitResponse() {
    }

    public static FraudAlertHitResponse fromEntity(FraudAlertHit hit) {
        FraudAlertHitResponse dto = new FraudAlertHitResponse();
        dto.setId(hit.getId());
        dto.setRuleCode(hit.getRuleCode());
        dto.setRuleName(hit.getRuleCode());
        dto.setScoreContribution(hit.getScoreContribution());
        dto.setReasonExplanation(hit.getReasonExplanation());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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
}
