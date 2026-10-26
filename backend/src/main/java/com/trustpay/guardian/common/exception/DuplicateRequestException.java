package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends ApiException {

    public DuplicateRequestException(String idempotencyKey) {
        super(String.format("Duplicate request detected for idempotency key '%s'", idempotencyKey), HttpStatus.CONFLICT);
    }

    public DuplicateRequestException(String message, HttpStatus status) {
        super(message, status);
    }
}
