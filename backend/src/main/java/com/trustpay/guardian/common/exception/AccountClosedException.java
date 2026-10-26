package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

public class AccountClosedException extends ApiException {

    public AccountClosedException(String accountNumber) {
        super(String.format("Account '%s' is CLOSED. No banking operations permitted.", accountNumber), HttpStatus.BAD_REQUEST);
    }
}
