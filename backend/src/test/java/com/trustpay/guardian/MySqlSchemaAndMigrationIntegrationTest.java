package com.trustpay.guardian;

import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.fraud.FraudRuleRepository;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dedicated MySQL 8.4.11 Integration Test.
 * Activates profile "mysql-test" to verify schema migrations and seed assertions
 * against the isolated MySQL database "trustpay_guardian_test".
 *
 * Enabled when MySQL credentials are provided via environment variables or system properties.
 */
@SpringBootTest
@ActiveProfiles("mysql-test")
@EnabledIf(
        value = "isMySqlConfigured",
        disabledReason = "MySQL credentials not provided in environment (FLYWAY_PASSWORD / FLYWAY_TEST_PASSWORD)"
)
class MySqlSchemaAndMigrationIntegrationTest {

    static boolean isMySqlConfigured() {
        String pass = System.getenv("FLYWAY_TEST_PASSWORD");
        if (pass == null || pass.isBlank()) {
            pass = System.getenv("FLYWAY_PASSWORD");
        }
        if (pass == null || pass.isBlank()) {
            pass = System.getenv("SPRING_DATASOURCE_TEST_PASSWORD");
        }
        if (pass == null || pass.isBlank()) {
            pass = System.getenv("SPRING_DATASOURCE_PASSWORD");
        }
        if (pass == null || pass.isBlank()) {
            pass = System.getProperty("spring.flyway.password");
        }
        if (pass == null || pass.isBlank()) {
            pass = System.getProperty("FLYWAY_PASSWORD");
        }
        if (pass == null || pass.isBlank()) {
            pass = System.getProperty("spring.datasource.password");
        }
        return pass != null && !pass.isBlank();
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private FraudRuleRepository fraudRuleRepository;

    @Test
    @DisplayName("Verify actual connection to MySQL engine (not H2) and target isolated schema")
    void verifiesConnectedToActualMySqlServer() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();
            String catalog = conn.getCatalog();

            // Explicitly assert that this is NOT H2
            assertThat(productName).isEqualToIgnoringCase("MySQL");
            assertThat(productName).doesNotContainIgnoringCase("H2");

            // Verify version is MySQL 8.x
            assertThat(productVersion).startsWith("8.");

            // Verify isolated test database catalog
            assertThat(catalog).isEqualTo("trustpay_guardian_test");
        }
    }

    @Test
    @DisplayName("Flyway detects and applies or validates all 6 versioned migrations")
    void flywayAppliesAllSixMigrations() {
        var info = flyway.info();
        assertThat(info.applied()).hasSize(6);
        assertThat(info.current().getVersion().getVersion()).isEqualTo("6");

        // Verify version titles match canonical migrations
        Map<String, String> expectedTitles = Map.of(
                "1", "create users and accounts",
                "2", "create beneficiaries and transactions",
                "3", "create fraud tables",
                "4", "create auth notifications audit",
                "5", "create system settings",
                "6", "seed fraud rules and settings"
        );

        Arrays.stream(info.applied()).forEach(migration -> {
            String version = migration.getVersion().getVersion();
            assertThat(expectedTitles).containsKey(version);
            assertThat(migration.getDescription()).isEqualTo(expectedTitles.get(version));
        });
    }

    @Test
    @DisplayName("Verify all 11 canonical database tables exist in MySQL")
    void allCanonicalTablesExistInMySql() throws Exception {
        Set<String> expectedTables = Set.of(
                "users",
                "accounts",
                "beneficiaries",
                "transactions",
                "fraud_rules",
                "fraud_alerts",
                "fraud_alert_hits",
                "refresh_tokens",
                "notifications",
                "audit_logs",
                "system_settings"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        assertThat(actualTables).containsAll(expectedTables);
    }

    @Test
    @DisplayName("V6 seeded all 6 default system settings with expected values")
    void seededSystemSettingsExistWithExpectedValues() {
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
    @DisplayName("V6 seeded all 7 canonical fraud detection rules (R1-R7) with point weights")
    void seededFraudRulesExistWithExpectedWeightsAndEnabled() {
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
        assertThat(rules).allMatch(FraudRule::isEnabled);
    }
}
