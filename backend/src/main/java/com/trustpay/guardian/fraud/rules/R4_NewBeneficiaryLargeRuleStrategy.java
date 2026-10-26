package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
public class R4_NewBeneficiaryLargeRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R4";
    public static final int DEFAULT_POINTS = 30;
    public static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("20000.00");
    public static final int DEFAULT_WINDOW_HOURS = 24;

    private final FraudRuleConfigService configService;

    public R4_NewBeneficiaryLargeRuleStrategy(FraudRuleConfigService configService) {
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

        Optional<Beneficiary> optBeneficiary = context.getBeneficiary();
        if (optBeneficiary.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal threshold = configService.getBigDecimalParameter(code(), "threshold_amount", DEFAULT_THRESHOLD);
        int windowHours = configService.getIntParameter(code(), "window_hours", DEFAULT_WINDOW_HOURS);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        Beneficiary beneficiary = optBeneficiary.get();
        Duration age = Duration.between(beneficiary.getCreatedAt(), context.getTimestamp());
        long hoursSinceCreation = age.toHours();

        if (hoursSinceCreation < windowHours && context.getAmount().compareTo(threshold) >= 0) {
            String explanation = String.format(
                    "High-value transfer of ₹%s to newly registered beneficiary '%s' added only %d hours ago (threshold: < %d hrs, >= ₹%s)",
                    context.getAmount().toPlainString(),
                    beneficiary.getNickname(),
                    hoursSinceCreation,
                    windowHours,
                    threshold.toPlainString()
            );
            return Optional.of(new RuleHit(code(), points, explanation));
        }

        return Optional.empty();
    }
}
