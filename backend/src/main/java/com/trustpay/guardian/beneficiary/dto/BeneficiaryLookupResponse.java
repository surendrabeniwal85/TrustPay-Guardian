package com.trustpay.guardian.beneficiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lookup response returning masked beneficiary details prior to confirmation")
public record BeneficiaryLookupResponse(
        @Schema(description = "Account number queried", example = "TPG100000000002")
        String accountNumber,

        @Schema(description = "Masked account holder name to protect PII", example = "J*** S****")
        String maskedAccountHolderName
) {
}
