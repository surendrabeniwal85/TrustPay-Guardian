package com.trustpay.guardian.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertHitRepository extends JpaRepository<FraudAlertHit, Long> {

    List<FraudAlertHit> findByAlertId(Long alertId);

    List<FraudAlertHit> findByRuleCode(String ruleCode);
}
