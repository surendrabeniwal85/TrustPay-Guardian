package com.trustpay.guardian.fraud;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditService;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.ResourceNotFoundException;
import com.trustpay.guardian.fraud.dto.AlertDecisionRequest;
import com.trustpay.guardian.fraud.dto.FraudAlertResponse;
import com.trustpay.guardian.fraud.dto.FraudStatsResponse;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class FraudAnalystService {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalystService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public FraudAnalystService(
            FraudAlertRepository fraudAlertRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getAlerts(AlertStatus status, Integer minScore, Integer maxScore, Pageable pageable) {
        Page<FraudAlert> alerts = fraudAlertRepository.findFilteredAlerts(status, minScore, maxScore, pageable);
        return alerts.map(FraudAlertResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public FraudAlertResponse getAlertDetails(Long alertId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));
        return FraudAlertResponse.fromEntity(alert);
    }

    @Transactional(readOnly = true)
    public FraudStatsResponse getStats() {
        long totalAlerts = fraudAlertRepository.count();
        long openAlerts = fraudAlertRepository.countByStatus(AlertStatus.OPEN);
        long inReviewAlerts = fraudAlertRepository.countByStatus(AlertStatus.IN_REVIEW);
        long resolvedReleased = fraudAlertRepository.countByStatus(AlertStatus.RESOLVED_RELEASED);
        long resolvedBlocked = fraudAlertRepository.countByStatus(AlertStatus.RESOLVED_BLOCKED);
        Double avg = fraudAlertRepository.findAverageRiskScore();
        double averageScore = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        return new FraudStatsResponse(
                totalAlerts,
                openAlerts,
                inReviewAlerts,
                resolvedReleased,
                resolvedBlocked,
                averageScore
        );
    }

    @Transactional
    public FraudAlertResponse resolveAlert(
            Long alertId,
            AlertDecisionRequest request,
            Long analystUserId,
            String ipAddress,
            String requestId) {

        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));

        // PRD: A second resolution attempt returns 409 Conflict.
        if (alert.getStatus() == AlertStatus.RESOLVED_RELEASED || alert.getStatus() == AlertStatus.RESOLVED_BLOCKED) {
            throw new ConflictException("Fraud alert has already been resolved with status: " + alert.getStatus());
        }

        User analyst = userRepository.findById(analystUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", analystUserId));

        Transaction tx = alert.getTransaction();
        Instant now = Instant.now();

        if ("RELEASE".equalsIgnoreCase(request.getDecision())) {
            Account src = tx.getSourceAccount();
            Account dst = tx.getDestinationAccount();

            // Ascending row lock ordering to prevent deadlock
            Long firstId = Math.min(src.getId(), dst.getId());
            Long secondId = Math.max(src.getId(), dst.getId());

            Account firstLocked = accountRepository.findByIdForUpdate(firstId).orElseThrow();
            Account secondLocked = accountRepository.findByIdForUpdate(secondId).orElseThrow();

            Account lockedSrc = src.getId().equals(firstId) ? firstLocked : secondLocked;
            Account lockedDst = dst.getId().equals(firstId) ? firstLocked : secondLocked;

            // Re-check account statuses
            if (lockedSrc.getStatus() == AccountStatus.FROZEN || lockedSrc.getStatus() == AccountStatus.CLOSED
                    || lockedDst.getStatus() == AccountStatus.FROZEN || lockedDst.getStatus() == AccountStatus.CLOSED) {
                log.warn("Account status invalid during release of alert [{}]. Reverting transaction to REJECTED.", alertId);
                tx.setStatus(TransactionStatus.REJECTED);
                tx.setFailureReason("Account status invalid during analyst release execution.");
                tx.setCompletedAt(now);
                transactionRepository.save(tx);

                alert.setStatus(AlertStatus.RESOLVED_BLOCKED);
                alert.setAssignedTo(analyst);
                alert.setDecisionNotes("Auto-blocked: Source or destination account status is invalid: " + request.getDecisionNotes().trim());
                alert.setResolvedAt(now);
                FraudAlert savedAlert = fraudAlertRepository.save(alert);
                return FraudAlertResponse.fromEntity(savedAlert);
            }

            // Re-check balance: if funds are missing, transfer becomes REJECTED and alert RESOLVED_BLOCKED
            if (lockedSrc.getBalance().compareTo(tx.getAmount()) < 0) {
                log.warn("Insufficient funds during release of alert [{}]. Reverting transaction to REJECTED.", alertId);
                tx.setStatus(TransactionStatus.REJECTED);
                tx.setFailureReason("Insufficient funds on release execution.");
                tx.setCompletedAt(now);
                transactionRepository.save(tx);

                alert.setStatus(AlertStatus.RESOLVED_BLOCKED);
                alert.setAssignedTo(analyst);
                alert.setDecisionNotes("Auto-blocked: Insufficient funds upon analyst release attempt.");
                alert.setResolvedAt(now);
                FraudAlert savedAlert = fraudAlertRepository.save(alert);
                return FraudAlertResponse.fromEntity(savedAlert);
            }

            // Funds sufficient: Atomic debit & credit
            lockedSrc.setBalance(lockedSrc.getBalance().subtract(tx.getAmount()));
            lockedDst.setBalance(lockedDst.getBalance().add(tx.getAmount()));

            accountRepository.save(lockedSrc);
            accountRepository.save(lockedDst);

            tx.setStatus(TransactionStatus.APPROVED_SIMULATED);
            tx.setCompletedAt(now);
            transactionRepository.save(tx);

            alert.setStatus(AlertStatus.RESOLVED_RELEASED);
            alert.setAssignedTo(analyst);
            alert.setDecisionNotes(request.getDecisionNotes().trim());
            alert.setResolvedAt(now);
            FraudAlert savedAlert = fraudAlertRepository.save(alert);

            // Notify parties
            notificationService.sendNotification(
                    lockedSrc.getUser(),
                    NotificationType.TRANSFER_RELEASED,
                    "Held Transfer Approved and Released",
                    String.format("Your transfer of ₹%s (Ref: %s) was reviewed and approved by fraud operations. Funds have been posted.",
                            tx.getAmount().toPlainString(), tx.getReferenceId())
            );

            notificationService.sendNotification(
                    lockedDst.getUser(),
                    NotificationType.TRANSFER_RECEIVED,
                    "Funds Received",
                    String.format("Received ₹%s from %s (%s). Reference: %s",
                            tx.getAmount().toPlainString(),
                            lockedSrc.getUser().getFullName(),
                            lockedSrc.getMaskedAccountNumber(),
                            tx.getReferenceId())
            );

            auditService.logAction(
                    analyst,
                    AuditAction.ALERT_DECISION_RELEASE,
                    AuditTargetType.FRAUD_ALERT,
                    String.valueOf(alertId),
                    String.format("{\"decision\":\"RELEASE\",\"referenceId\":\"%s\",\"notes\":\"%s\"}",
                            tx.getReferenceId(), request.getDecisionNotes()),
                    ipAddress,
                    requestId
            );

            return FraudAlertResponse.fromEntity(savedAlert);

        } else if ("CONFIRM_BLOCK".equalsIgnoreCase(request.getDecision())) {
            tx.setStatus(TransactionStatus.BLOCKED);
            tx.setCompletedAt(now);
            tx.setFailureReason("Confirmed fraudulent and blocked by analyst: " + request.getDecisionNotes().trim());
            transactionRepository.save(tx);

            alert.setStatus(AlertStatus.RESOLVED_BLOCKED);
            alert.setAssignedTo(analyst);
            alert.setDecisionNotes(request.getDecisionNotes().trim());
            alert.setResolvedAt(now);
            FraudAlert savedAlert = fraudAlertRepository.save(alert);

            // Notify sender
            notificationService.sendNotification(
                    tx.getSourceAccount().getUser(),
                    NotificationType.TRANSFER_BLOCKED,
                    "Transfer Blocked Following Fraud Review",
                    String.format("Your transfer of ₹%s (Ref: %s) was investigated and confirmed blocked by fraud operations.",
                            tx.getAmount().toPlainString(), tx.getReferenceId())
            );

            auditService.logAction(
                    analyst,
                    AuditAction.ALERT_DECISION_BLOCK,
                    AuditTargetType.FRAUD_ALERT,
                    String.valueOf(alertId),
                    String.format("{\"decision\":\"CONFIRM_BLOCK\",\"referenceId\":\"%s\",\"notes\":\"%s\"}",
                            tx.getReferenceId(), request.getDecisionNotes()),
                    ipAddress,
                    requestId
            );

            return FraudAlertResponse.fromEntity(savedAlert);

        } else {
            throw new IllegalArgumentException("Invalid decision: " + request.getDecision());
        }
    }
}
