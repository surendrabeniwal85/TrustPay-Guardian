package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class R2_VelocityRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R2";
    public static final int DEFAULT_POINTS = 25;
    public static final int DEFAULT_MAX_PRIOR_TRANSFERS = 4; // >= 5 outgoing transfers including current transfer

    private final FraudRuleConfigService configService;

    public R2_VelocityRuleStrategy(FraudRuleConfigService configService) {
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

        int maxPrior = configService.getIntParameter(code(), "max_transfers", DEFAULT_MAX_PRIOR_TRANSFERS);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        long recentCount = context.getRecent10MinTransfersCount();
        long totalIncludingCurrent = recentCount + 1;

        if (recentCount >= maxPrior) {
            String explanation = String.format(
                    "High transaction velocity: %d transfers initiated within the last 10 minutes (threshold: >= %d)",
                    totalIncludingCurrent, maxPrior + 1
            );
            return Optional.of(new RuleHit(code(), points, explanation));
        }

        return Optional.empty();
    }
}
