package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

public class AccountFrozenException extends ApiException {

    public AccountFrozenException(String accountNumber) {
        super(String.format("Account '%s' is FROZEN. Outgoing debit transfers are restricted.", accountNumber), HttpStatus.FORBIDDEN);
    }

    public AccountFrozenException(String message, HttpStatus status) {
        super(message, status);
    }
}
