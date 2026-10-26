package com.trustpay.guardian.admin.dto;

import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditLog;
import com.trustpay.guardian.audit.AuditTargetType;

import java.time.Instant;

public class AuditLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorName;
    private String actorEmail;
    private AuditAction action;
    private AuditTargetType targetType;
    private String targetId;
    private String detailsJson;
    private String ipAddress;
    private String requestId;
    private Instant createdAt;

    public AuditLogResponse() {
    }

    public static AuditLogResponse fromEntity(AuditLog log) {
        AuditLogResponse res = new AuditLogResponse();
        res.setId(log.getId());
        if (log.getActorUser() != null) {
            res.setActorUserId(log.getActorUser().getId());
            res.setActorName(log.getActorUser().getFullName());
            res.setActorEmail(log.getActorUser().getEmail());
        }
        res.setAction(log.getAction());
        res.setTargetType(log.getTargetType());
        res.setTargetId(log.getTargetId());
        res.setDetailsJson(log.getDetailsJson());
        res.setIpAddress(log.getIpAddress());
        res.setRequestId(log.getRequestId());
        res.setCreatedAt(log.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AuditTargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
