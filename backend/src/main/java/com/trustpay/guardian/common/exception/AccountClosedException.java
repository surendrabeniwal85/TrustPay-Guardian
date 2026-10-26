package com.trustpay.guardian.common.exception;

public class AccountClosedException extends BusinessRuleException {

    public AccountClosedException(String accountNumber) {
        super(String.format("Account '%s' is CLOSED. No banking operations permitted.", accountNumber));
    }

    public AccountClosedException(String message, Throwable cause) {
        super(message);
    }
}

