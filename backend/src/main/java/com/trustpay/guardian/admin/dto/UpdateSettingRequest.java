package com.trustpay.guardian.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateSettingRequest {

    @NotBlank(message = "Setting value cannot be blank")
    private String settingValue;

    private String description;

    public UpdateSettingRequest() {
    }

    public UpdateSettingRequest(String settingValue, String description) {
        this.settingValue = settingValue;
        this.description = description;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
