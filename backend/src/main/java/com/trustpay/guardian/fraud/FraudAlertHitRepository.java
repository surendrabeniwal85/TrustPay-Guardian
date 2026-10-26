package com.trustpay.guardian.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertHitRepository extends JpaRepository<FraudAlertHit, Long> {

    List<FraudAlertHit> findByAlertId(Long alertId);

    List<FraudAlertHit> findByRuleCode(String ruleCode);

    @org.springframework.data.jpa.repository.Query("SELECT h.ruleCode, COUNT(h) FROM FraudAlertHit h GROUP BY h.ruleCode")
    List<Object[]> countGroupByRuleCode();
}
