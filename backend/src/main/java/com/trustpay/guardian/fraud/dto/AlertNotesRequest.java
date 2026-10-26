package com.trustpay.guardian.fraud.dto;

import jakarta.validation.constraints.NotBlank;

public record AlertNotesRequest(
        @NotBlank(message = "Decision notes are required to explain the investigation outcome")
        String decisionNotes
) {
}
