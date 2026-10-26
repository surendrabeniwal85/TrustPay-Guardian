package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class R7_RecentFailedLoginsRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R7";
    public static final int DEFAULT_POINTS = 15;
    public static final int DEFAULT_MIN_FAILED_ATTEMPTS = 3;

    private final FraudRuleConfigService configService;

    public R7_RecentFailedLoginsRuleStrategy(FraudRuleConfigService configService) {
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

        int minFailedAttempts = configService.getIntParameter(code(), "min_failed_attempts", DEFAULT_MIN_FAILED_ATTEMPTS);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        if (context.getFailedLoginAttempts() >= minFailedAttempts) {
            String explanation = String.format(
                    "Originating user had %d recent failed login attempts prior to initiating this transfer (threshold: >= %d)",
                    context.getFailedLoginAttempts(), minFailedAttempts
            );
            return Optional.of(new RuleHit(code(), points, explanation));
        }

        return Optional.empty();
    }
}
