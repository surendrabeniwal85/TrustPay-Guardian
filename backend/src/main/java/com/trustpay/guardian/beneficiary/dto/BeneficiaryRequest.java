package com.trustpay.guardian.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BeneficiaryRequest {

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickname;

    public BeneficiaryRequest() {
    }

    public BeneficiaryRequest(String recipientAccountNumber, String nickname) {
        this.recipientAccountNumber = recipientAccountNumber;
        this.nickname = nickname;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
