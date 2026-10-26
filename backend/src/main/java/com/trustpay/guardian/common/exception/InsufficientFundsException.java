package com.trustpay.guardian.common.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessRuleException {

    public InsufficientFundsException(BigDecimal availableBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient funds. Available balance: ₹%s, Required amount: ₹%s",
                availableBalance.toPlainString(), requiredAmount.toPlainString()));
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}

