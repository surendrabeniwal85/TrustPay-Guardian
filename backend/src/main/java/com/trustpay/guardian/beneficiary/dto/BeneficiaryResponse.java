package com.trustpay.guardian.beneficiary.dto;

import com.trustpay.guardian.beneficiary.Beneficiary;

import java.time.Instant;

public class BeneficiaryResponse {

    private Long id;
    private Long recipientAccountId;
    private String recipientAccountNumber;
    private String recipientMaskedAccountNumber;
    private String recipientName;
    private String nickname;
    private Instant createdAt;

    public BeneficiaryResponse() {
    }

    public static BeneficiaryResponse fromEntity(Beneficiary beneficiary) {
        BeneficiaryResponse dto = new BeneficiaryResponse();
        dto.setId(beneficiary.getId());
        dto.setRecipientAccountId(beneficiary.getRecipientAccount().getId());
        dto.setRecipientAccountNumber(beneficiary.getRecipientAccount().getAccountNumber());
        dto.setRecipientMaskedAccountNumber(beneficiary.getRecipientAccount().getMaskedAccountNumber());
        dto.setRecipientName(beneficiary.getRecipientAccount().getUser().getFullName());
        dto.setNickname(beneficiary.getNickname());
        dto.setCreatedAt(beneficiary.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(Long recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public String getRecipientMaskedAccountNumber() {
        return recipientMaskedAccountNumber;
    }

    public void setRecipientMaskedAccountNumber(String recipientMaskedAccountNumber) {
        this.recipientMaskedAccountNumber = recipientMaskedAccountNumber;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
