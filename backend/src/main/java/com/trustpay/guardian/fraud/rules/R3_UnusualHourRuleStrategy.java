package com.trustpay.guardian.fraud.rules;

import com.trustpay.guardian.fraud.FraudRuleConfigService;
import com.trustpay.guardian.fraud.TransactionContext;
import com.trustpay.guardian.fraud.engine.FraudRule;
import com.trustpay.guardian.fraud.engine.RuleHit;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class R3_UnusualHourRuleStrategy implements FraudRule {

    public static final String RULE_CODE = "R3";
    public static final int DEFAULT_POINTS = 15;
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private final FraudRuleConfigService configService;

    public R3_UnusualHourRuleStrategy(FraudRuleConfigService configService) {
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

        int startHour = configService.getIntParameter(code(), "start_hour", 0);
        int endHour = configService.getIntParameter(code(), "end_hour", 4);
        int points = configService.getRulePoints(code(), DEFAULT_POINTS);

        ZonedDateTime zdt = context.getTimestamp().atZone(DEFAULT_ZONE);
        int hour = zdt.getHour();

        // 00:00 to 04:59 Asia/Kolkata
        if (hour >= startHour && hour <= endHour) {
            String explanation = String.format(
                    "Transfer initiated during unusual night hours at %02d:%02d Asia/Kolkata (window: %02d:00 - %02d:59)",
                    hour, zdt.getMinute(), startHour, endHour
            );
            return Optional.of(new RuleHit(code(), points, explanation));
        }

        return Optional.empty();
    }
}
