package com.trustpay.guardian.fraud;

import com.trustpay.guardian.fraud.dto.AlertNotesRequest;
import com.trustpay.guardian.fraud.dto.FraudAlertResponse;
import com.trustpay.guardian.fraud.dto.FraudStatsResponse;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FraudEntityAndControllerUnitTest {

    @Test
    @DisplayName("FraudAlert lifecycle, getters, setters, equals, and hashCode")
    void testFraudAlertEntity() {
        Transaction tx = new Transaction();
        tx.setId(10L);

        FraudAlert alert = new FraudAlert();
        alert.setId(1L);
        alert.setTransaction(tx);
        alert.setRiskScore(75);
        alert.setRiskLevel(RiskLevel.HIGH);
        alert.setStatus(AlertStatus.OPEN);

        User analyst = new User("Analyst", "analyst@example.com", "9876543210", "hash", Role.FRAUD_ANALYST);
        analyst.setId(5L);
        alert.setAssignedTo(analyst);
        alert.setDecisionNotes("Under review");
        alert.setCreatedAt(Instant.now());
        alert.setResolvedAt(Instant.now());

        FraudAlertHit hit = new FraudAlertHit();
        alert.setHits(List.of(hit));

        alert.onCreate(); // prePersist

        assertThat(alert.getId()).isEqualTo(1L);
        assertThat(alert.getTransaction()).isEqualTo(tx);
        assertThat(alert.getRiskScore()).isEqualTo(75);
        assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getAssignedTo()).isEqualTo(analyst);
        assertThat(alert.getDecisionNotes()).isEqualTo("Under review");
        assertThat(alert.getCreatedAt()).isNotNull();
        assertThat(alert.getResolvedAt()).isNotNull();
        assertThat(alert.getHits()).hasSize(1);

        FraudAlert same = new FraudAlert(tx, 75, RiskLevel.HIGH);
        same.setId(1L);
        FraudAlert diff = new FraudAlert(tx, 75, RiskLevel.HIGH, AlertStatus.OPEN);
        diff.setId(2L);

        assertThat(alert).isEqualTo(same);
        assertThat(alert).isNotEqualTo(diff);
        assertThat(alert).isNotEqualTo(null);
        assertThat(alert.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    @DisplayName("FraudAlertHit lifecycle, getters, setters, equals, and hashCode")
    void testFraudAlertHitEntity() {
        FraudAlert alert = new FraudAlert();
        alert.setId(1L);

        FraudAlertHit hit = new FraudAlertHit();
        hit.setId(10L);
        hit.setAlert(alert);
        hit.setRuleCode("RULE_1");
        hit.setScoreContribution(30);
        hit.setReasonExplanation("Amount exceeded threshold");
        hit.setCreatedAt(Instant.now());

        hit.onCreate(); // prePersist

        assertThat(hit.getId()).isEqualTo(10L);
        assertThat(hit.getAlert()).isEqualTo(alert);
        assertThat(hit.getRuleCode()).isEqualTo("RULE_1");
        assertThat(hit.getScoreContribution()).isEqualTo(30);
        assertThat(hit.getReasonExplanation()).isEqualTo("Amount exceeded threshold");
        assertThat(hit.getCreatedAt()).isNotNull();

        FraudAlertHit same = new FraudAlertHit(alert, "RULE_1", 30, "Amount exceeded threshold");
        same.setId(10L);
        FraudAlertHit diff = new FraudAlertHit();
        diff.setId(20L);

        assertThat(hit).isEqualTo(same);
        assertThat(hit).isNotEqualTo(diff);
        assertThat(hit).isNotEqualTo(null);
        assertThat(hit.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    @DisplayName("FraudRule lifecycle, getters, setters, equals, and hashCode")
    void testFraudRuleEntity() {
        FraudRule rule = new FraudRule();
        rule.setId(100L);
        rule.setRuleCode("HIGH_AMOUNT");
        rule.setName("High Amount");
        rule.setDescription("Detects large transactions");
        rule.setPoints(40);
        rule.setScorePoints(45);
        rule.setEnabled(true);
        rule.setParametersJson("{\"threshold\":50000}");
        rule.setUpdatedAt(Instant.now());

        rule.onPersistOrUpdate();

        assertThat(rule.getId()).isEqualTo(100L);
        assertThat(rule.getRuleCode()).isEqualTo("HIGH_AMOUNT");
        assertThat(rule.getName()).isEqualTo("High Amount");
        assertThat(rule.getDescription()).isEqualTo("Detects large transactions");
        assertThat(rule.getPoints()).isEqualTo(45);
        assertThat(rule.getScorePoints()).isEqualTo(45);
        assertThat(rule.isEnabled()).isTrue();
        assertThat(rule.getParametersJson()).isEqualTo("{\"threshold\":50000}");
        assertThat(rule.getUpdatedAt()).isNotNull();

        FraudRule same = new FraudRule("HIGH_AMOUNT", "High Amount", "Desc", 45, true, "{}");
        same.setId(100L);
        FraudRule diff = new FraudRule();
        diff.setId(200L);

        assertThat(rule).isEqualTo(same);
        assertThat(rule).isNotEqualTo(diff);
        assertThat(rule).isNotEqualTo(null);
        assertThat(rule.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    @DisplayName("FraudAnalystController endpoints unit execution")
    void testFraudAnalystController() {
        FraudAnalystService service = mock(FraudAnalystService.class);
        FraudAnalystController controller = new FraudAnalystController(service);

        User analyst = new User("Analyst", "analyst@example.com", "9876543210", "hash", Role.FRAUD_ANALYST);
        analyst.setId(1L);
        UserPrincipal principal = UserPrincipal.create(analyst);

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(servletRequest.getHeader("X-Request-ID")).thenReturn("REQ-123");

        // 1. getAlerts
        Page<FraudAlertResponse> page = new PageImpl<>(Collections.emptyList());
        when(service.getAlerts(any(), any(), any(), any())).thenReturn(page);
        var respAlerts = controller.getAlerts(null, null, null, PageRequest.of(0, 10));
        assertThat(respAlerts.getBody()).isNotNull();

        // 2. getAlertDetails
        FraudAlertResponse detail = mock(FraudAlertResponse.class);
        when(service.getAlertDetails(10L)).thenReturn(detail);
        var respDetail = controller.getAlertDetails(10L);
        assertThat(respDetail.getBody().getData()).isEqualTo(detail);

        // 3. getStats
        FraudStatsResponse stats = mock(FraudStatsResponse.class);
        when(service.getStats()).thenReturn(stats);
        var respStats = controller.getStats();
        assertThat(respStats.getBody().getData()).isEqualTo(stats);

        // 4. releaseAlert
        when(service.resolveAlert(eq(10L), any(), eq(1L), any(), any())).thenReturn(detail);
        var respRel = controller.releaseAlert(10L, new AlertNotesRequest("Clear funds"), principal, servletRequest);
        assertThat(respRel.getBody().getData()).isEqualTo(detail);

        // 5. confirmBlockAlert
        var respBlk = controller.confirmBlockAlert(10L, new AlertNotesRequest("Confirmed fraudulent"), principal, servletRequest);
        assertThat(respBlk.getBody().getData()).isEqualTo(detail);
    }
}
