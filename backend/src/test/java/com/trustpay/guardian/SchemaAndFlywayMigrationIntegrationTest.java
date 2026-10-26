package com.trustpay.guardian;

import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.fraud.FraudRuleRepository;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchemaAndFlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @Test
    @DisplayName("Flyway applied all 6 versioned migrations cleanly")
    void flywayAppliesAllMigrations() {
        var info = flyway.info();
        assertThat(info.applied()).hasSize(6);
        assertThat(info.current().getVersion().getVersion()).isEqualTo("6");
    }

    @Test
    @DisplayName("V6 successfully seeded all default system settings")
    void systemSettingsAreSeeded() {
        Map<String, String> expectedSettings = Map.of(
                "REVIEW_THRESHOLD", "40",
                "BLOCK_THRESHOLD", "70",
                "DEMO_OPENING_BALANCE", "10000.00",
                "MAX_TRANSFER_AMOUNT", "200000.00",
                "MAX_DEPOSIT_AMOUNT", "50000.00",
                "DEFAULT_DAILY_LIMIT", "100000.00"
        );

        for (Map.Entry<String, String> entry : expectedSettings.entrySet()) {
            Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey(entry.getKey());
            assertThat(setting).isPresent();
            assertThat(setting.get().getSettingValue()).isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("V6 successfully seeded all 7 canonical fraud detection rules (R1-R7)")
    void fraudRulesAreSeeded() {
        List<FraudRule> rules = fraudRuleRepository.findAll();
        assertThat(rules).hasSize(7);

        Map<String, Integer> expectedRules = Map.of(
                "R1", 30,
                "R2", 25,
                "R3", 15,
                "R4", 30,
                "R5", 20,
                "R6", 10,
                "R7", 15
        );

        Map<String, Integer> actualRules = rules.stream()
                .collect(Collectors.toMap(FraudRule::getRuleCode, FraudRule::getPoints));

        assertThat(actualRules).isEqualTo(expectedRules);

        // Verify all 7 rules are enabled by default
        assertThat(rules).allMatch(FraudRule::isEnabled);
    }
}
