package com.trustpay.guardian.fraud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Optional<FraudAlert> findByTransactionId(Long transactionId);

    List<FraudAlert> findByStatus(AlertStatus status);

    Page<FraudAlert> findByStatus(AlertStatus status, Pageable pageable);

    List<FraudAlert> findByRiskLevel(RiskLevel riskLevel);

    long countByStatus(AlertStatus status);

    @Query("SELECT COALESCE(AVG(a.riskScore), 0.0) FROM FraudAlert a")
    Double findAverageRiskScore();

    @Query(value = """
        SELECT a FROM FraudAlert a
        LEFT JOIN FETCH a.transaction t
        LEFT JOIN FETCH t.sourceAccount sa
        LEFT JOIN FETCH sa.user
        LEFT JOIN FETCH t.destinationAccount da
        LEFT JOIN FETCH da.user
        LEFT JOIN FETCH a.assignedTo
        WHERE (:status IS NULL OR a.status = :status)
          AND (:minScore IS NULL OR a.riskScore >= :minScore)
          AND (:maxScore IS NULL OR a.riskScore <= :maxScore)
        ORDER BY a.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM FraudAlert a
        WHERE (:status IS NULL OR a.status = :status)
          AND (:minScore IS NULL OR a.riskScore >= :minScore)
          AND (:maxScore IS NULL OR a.riskScore <= :maxScore)
        """)
    Page<FraudAlert> findFilteredAlerts(
            @Param("status") AlertStatus status,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            Pageable pageable
    );

    @Query("SELECT a.status, COUNT(a) FROM FraudAlert a GROUP BY a.status")
    List<Object[]> countGroupByStatus();
}
