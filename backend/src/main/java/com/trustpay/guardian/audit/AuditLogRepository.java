package com.trustpay.guardian.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(AuditTargetType targetType, String targetId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action IS NULL OR a.action = :action)
          AND (:targetType IS NULL OR a.targetType = :targetType)
        """)
    Page<AuditLog> findFilteredAuditLogs(
            @Param("action") AuditAction action,
            @Param("targetType") AuditTargetType targetType,
            Pageable pageable
    );
}
