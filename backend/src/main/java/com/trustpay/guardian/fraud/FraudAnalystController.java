package com.trustpay.guardian.fraud;

import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.fraud.dto.AlertDecisionRequest;
import com.trustpay.guardian.fraud.dto.AlertNotesRequest;
import com.trustpay.guardian.fraud.dto.FraudAlertResponse;
import com.trustpay.guardian.fraud.dto.FraudStatsResponse;
import com.trustpay.guardian.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud")
@PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
public class FraudAnalystController {

    private final FraudAnalystService fraudAnalystService;

    public FraudAnalystController(FraudAnalystService fraudAnalystService) {
        this.fraudAnalystService = fraudAnalystService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Integer minRiskScore,
            @RequestParam(required = false) Integer maxRiskScore,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<FraudAlertResponse> alerts = fraudAnalystService.getAlerts(status, minRiskScore, maxRiskScore, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> getAlertDetails(@PathVariable Long id) {
        FraudAlertResponse alert = fraudAnalystService.getAlertDetails(id);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FraudStatsResponse>> getStats() {
        FraudStatsResponse stats = fraudAnalystService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/alerts/{id}/decision")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolveAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        FraudAlertResponse response = fraudAnalystService.resolveAlert(
                id, request, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Alert investigation decision recorded successfully", response));
    }

    @PostMapping("/alerts/{id}/release")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> releaseAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertNotesRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AlertDecisionRequest decisionRequest = new AlertDecisionRequest("RELEASE", request.decisionNotes());
        FraudAlertResponse response = fraudAnalystService.resolveAlert(
                id, decisionRequest, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Held transfer released successfully", response));
    }

    @PostMapping("/alerts/{id}/confirm-block")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> confirmBlockAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertNotesRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AlertDecisionRequest decisionRequest = new AlertDecisionRequest("CONFIRM_BLOCK", request.decisionNotes());
        FraudAlertResponse response = fraudAnalystService.resolveAlert(
                id, decisionRequest, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Transfer confirmed blocked by fraud analyst", response));
    }
}
