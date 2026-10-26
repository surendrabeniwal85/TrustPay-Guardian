package com.trustpay.guardian.audit;

import com.trustpay.guardian.common.web.RequestIdFilter;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void record(User actor, AuditAction action, AuditTargetType entityType, String entityId, String details) {
        String ipAddress = "127.0.0.1";
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null) {
                HttpServletRequest req = attrs.getRequest();
                String xForwarded = req.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(xForwarded)) {
                    ipAddress = xForwarded.split(",")[0].trim();
                } else {
                    ipAddress = req.getRemoteAddr();
                }
                if (requestId == null) {
                    requestId = (String) req.getAttribute(RequestIdFilter.MDC_KEY);
                }
            }
        } catch (Exception ignored) {
        }
        logAction(actor, action, entityType, entityId, details, ipAddress, requestId != null ? requestId : "SYS");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User actorUser, AuditAction action, AuditTargetType targetType,
                          String targetId, String detailsJson, String ipAddress, String requestId) {
        try {
            User persistentActor = null;
            if (actorUser != null && actorUser.getId() != null) {
                persistentActor = userRepository.findById(actorUser.getId()).orElse(null);
            }
            AuditLog auditLog = new AuditLog(persistentActor, action, targetType, targetId, detailsJson, ipAddress, requestId);
            auditLogRepository.save(auditLog);
            log.info("AUDIT LOG [{}] on [{}:{}] by [User:{}] [req:{}]",
                    action, targetType, targetId, persistentActor != null ? persistentActor.getEmail() : "SYSTEM", requestId);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
