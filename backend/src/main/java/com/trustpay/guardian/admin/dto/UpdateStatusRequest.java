package com.trustpay.guardian.admin.dto;

import com.trustpay.guardian.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
