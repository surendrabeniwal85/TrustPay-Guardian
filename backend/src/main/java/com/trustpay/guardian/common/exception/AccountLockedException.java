package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {

    public AccountLockedException(long minutesRemaining) {
        super(String.format("Account is temporarily locked due to excessive failed login attempts. Please try again in %d minute(s).",
                minutesRemaining), HttpStatus.LOCKED);
    }

    public AccountLockedException(String message) {
        super(message, HttpStatus.LOCKED);
    }
}
