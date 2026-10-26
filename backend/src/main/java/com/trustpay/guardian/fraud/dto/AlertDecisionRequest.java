package com.trustpay.guardian.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AlertDecisionRequest {

    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "^(RELEASE|CONFIRM_BLOCK)$", message = "Decision must be either 'RELEASE' or 'CONFIRM_BLOCK'")
    private String decision;

    @NotBlank(message = "Decision notes are required to explain the investigation outcome")
    private String decisionNotes;

    public AlertDecisionRequest() {
    }

    public AlertDecisionRequest(String decision, String decisionNotes) {
        this.decision = decision;
        this.decisionNotes = decisionNotes;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public void setDecisionNotes(String decisionNotes) {
        this.decisionNotes = decisionNotes;
    }
}
