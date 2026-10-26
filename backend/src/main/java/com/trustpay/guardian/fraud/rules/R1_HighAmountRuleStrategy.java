package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class R1_HighAmountRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R1";
    public static final int DEFAULT_POINTS = 30;
    public static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("50000.00");

    private final FraudRuleConfigService configService;

    public R1_HighAmountRuleStrategy(FraudRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String code() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        if (!configService.isRuleEnabled(code())) {
            return Optional.empty();
        }

        BigDecimal threshold = configService.getBigDecimalParameter(code(), "threshold_amount", DEFAULT_THRESHOLD);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        BigDecimal amount = context.getAmount();
        if (amount != null && amount.compareTo(threshold) >= 0) {
            String explanation = String.format(
                    "Transfer amount of ₹%s meets or exceeds high-amount threshold of ₹%s",
                    amount.toPlainString(), threshold.toPlainString()
            );
            return Optional.of(new RuleHit(code(), points, explanation));
        }

        return Optional.empty();
    }
}
