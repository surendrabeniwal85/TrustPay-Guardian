package com.trustpay.guardian.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateRuleRequest {

    @NotNull(message = "Score points is required")
    @Min(value = 0, message = "Score points cannot be negative")
    @Max(value = 100, message = "Score points cannot exceed 100")
    private Integer scorePoints;

    @NotNull(message = "isEnabled flag is required")
    private Boolean isEnabled;

    private String description;

    public UpdateRuleRequest() {
    }

    public UpdateRuleRequest(Integer scorePoints, Boolean isEnabled, String description) {
        this.scorePoints = scorePoints;
        this.isEnabled = isEnabled;
        this.description = description;
    }

    public Integer getScorePoints() {
        return scorePoints;
    }

    public void setScorePoints(Integer scorePoints) {
        this.scorePoints = scorePoints;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
