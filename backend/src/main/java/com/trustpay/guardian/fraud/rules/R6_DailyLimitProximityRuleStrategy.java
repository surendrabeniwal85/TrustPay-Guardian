package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class R6_DailyLimitProximityRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R6";
    public static final int DEFAULT_POINTS = 10;
    public static final BigDecimal DEFAULT_PROXIMITY_RATIO = new BigDecimal("0.90");

    private final FraudRuleConfigService configService;

    public R6_DailyLimitProximityRuleStrategy(FraudRuleConfigService configService) {
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

        BigDecimal proximityRatio = configService.getBigDecimalParameter(code(), "proximity_ratio", DEFAULT_PROXIMITY_RATIO);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        Account src = context.getSourceAccount();
        if (src != null && src.getDailyLimit() != null && src.getDailyLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyLimit = src.getDailyLimit();
            BigDecimal cumulative = context.getTodayOutgoingTotal().add(context.getAmount());
            BigDecimal threshold = dailyLimit.multiply(proximityRatio);

            if (cumulative.compareTo(threshold) >= 0) {
                BigDecimal ratio = cumulative.divide(dailyLimit, 4, RoundingMode.HALF_UP);
                String explanation = String.format(
                        "Cumulative daily outgoing total reaches %s%% of the account's daily limit (Limit: ₹%s, Projected total: ₹%s)",
                        ratio.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString(),
                        dailyLimit.toPlainString(),
                        cumulative.toPlainString()
                );
                return Optional.of(new RuleHit(code(), points, explanation));
            }
        }

        return Optional.empty();
    }
}
