package com.trustpay.guardian.common.exception;

import java.math.BigDecimal;

public class DailyLimitExceededException extends BusinessRuleException {

    public DailyLimitExceededException(BigDecimal requestedAmount, BigDecimal remainingDailyLimit) {
        super(String.format("Transfer exceeds daily outgoing transfer limit. Requested: ₹%s, Remaining limit: ₹%s",
                requestedAmount.toPlainString(), remainingDailyLimit.toPlainString()));
    }

    public DailyLimitExceededException(String message) {
        super(message);
    }
}

