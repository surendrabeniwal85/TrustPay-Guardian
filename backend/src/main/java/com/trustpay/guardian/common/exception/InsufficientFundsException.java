package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(BigDecimal availableBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient funds. Available balance: ₹%s, Required amount: ₹%s",
                availableBalance.toPlainString(), requiredAmount.toPlainString()), HttpStatus.BAD_REQUEST);
    }

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
