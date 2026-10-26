package com.trustpay.guardian.common.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class DailyLimitExceededException extends ApiException {

    public DailyLimitExceededException(BigDecimal requestedAmount, BigDecimal remainingDailyLimit) {
        super(String.format("Transfer exceeds daily outgoing transfer limit. Requested: ₹%s, Remaining limit: ₹%s",
                requestedAmount.toPlainString(), remainingDailyLimit.toPlainString()), HttpStatus.BAD_REQUEST);
    }
}
