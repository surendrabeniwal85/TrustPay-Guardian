package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class R5_AmountSpikeRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R5";
    public static final int DEFAULT_POINTS = 20;
    public static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("5.0");
    public static final int DEFAULT_MIN_HISTORY_COUNT = 3;

    private final FraudRuleConfigService configService;

    public R5_AmountSpikeRuleStrategy(FraudRuleConfigService configService) {
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

        int minHistoryCount = configService.getIntParameter(code(), "min_history_count", DEFAULT_MIN_HISTORY_COUNT);
        BigDecimal multiplier = configService.getBigDecimalParameter(code(), "multiplier", DEFAULT_MULTIPLIER);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        if (context.getThirtyDayTransfersCount() >= minHistoryCount) {
            BigDecimal avg = context.getThirtyDayAverageAmount();
            if (avg != null && avg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = avg.multiply(multiplier);

                if (context.getAmount().compareTo(threshold) > 0) {
                    String explanation = String.format(
                            "Transfer amount of ₹%s is an unusual spike exceeding 5x the 30-day historical average (average: ₹%s, 5x threshold: ₹%s)",
                            context.getAmount().toPlainString(),
                            avg.toPlainString(),
                            threshold.toPlainString()
                    );
                    return Optional.of(new RuleHit(code(), points, explanation));
                }
            }
        }

        return Optional.empty();
    }
}
