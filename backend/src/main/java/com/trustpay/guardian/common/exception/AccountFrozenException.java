package com.trustpay.guardian.common.exception;

public class AccountFrozenException extends BusinessRuleException {

    public AccountFrozenException(String accountNumber) {
        super(String.format("Account '%s' is FROZEN. Outgoing debit and incoming transfers are restricted.", accountNumber));
    }

    public AccountFrozenException(String message, Throwable cause) {
        super(message);
    }
}

