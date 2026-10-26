package com.trustpay.guardian;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MigrationFailureRecoveryTest {

    @Test
    @DisplayName("Demonstrate and verify Flyway failure recovery runbook (repair + cleanup + retry)")
    void testFlywayFailureAndRepairRunbook() throws Exception {
        // Create an isolated in-memory test database with DB_CLOSE_DELAY=-1 so it persists across connections
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:recovery_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        // Phase 1: Run standard V1-V6 migrations cleanly
        Flyway baseFlyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        var baseResult = baseFlyway.migrate();
        assertThat(baseResult.migrationsExecuted).isEqualTo(6);
        assertThat(baseFlyway.info().current().getVersion().getVersion()).isEqualTo("6");

        // Verify V1-V6 tables exist
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            assertThat(rs.next()).isTrue();
        }

        // Phase 2: Simulate a failed migration attempt on version 7
        // We configure Flyway with db/migration (valid V1-V6) + db/test-broken-migration (broken V7)
        Flyway brokenFlyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration", "classpath:db/test-broken-migration")
                .load();

        assertThatThrownBy(brokenFlyway::migrate)
                .isInstanceOf(FlywayException.class);

        // Verify that flyway_schema_history records a failure for V7
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT success FROM \"flyway_schema_history\" WHERE version = '7'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("success")).isFalse();
        }

        // Verify that broken_table exists due to non-transactional DDL implicit commit
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM broken_table")) {
            assertThat(rs.next()).isTrue();
        }

        // Phase 3: Execute Recovery Runbook:
        // Step 1: Drop partially created objects left by the failed migration (MySQL non-transactional DDL cleanup)
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS broken_table");
        }

        // Step 2: Run flyway repair to remove the failed migration entry from flyway_schema_history
        var repairResult = brokenFlyway.repair();
        assertThat(repairResult.repairActions).isNotEmpty();

        // Verify that failed V7 record has been removed by repair
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE version = '7'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }

        // Step 3: Run the corrected migration using standard production migrations (V1-V6) + fixed V7
        Flyway cleanFlyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration", "classpath:db/test-fixed-migration")
                .load();

        var finalResult = cleanFlyway.migrate();
        assertThat(finalResult.success).isTrue();

        // Verify full schema reaches version 7 and extension table exists
        assertThat(cleanFlyway.info().current().getVersion().getVersion()).isEqualTo("7");

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_extension_table")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
    }
}
