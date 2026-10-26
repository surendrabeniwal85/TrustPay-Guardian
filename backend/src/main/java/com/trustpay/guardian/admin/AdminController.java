package com.trustpay.guardian.admin;

import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.admin.dto.*;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.fraud.FraudRule;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserManagementDto>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserManagementDto> users = adminService.getUsers(role, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserManagementDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        UserManagementDto created = adminService.createUser(
                request, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff user created successfully", created));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserManagementDto>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        UserManagementDto updated = adminService.updateUserRole(
                id, request.getRole(), currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserManagementDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        UserManagementDto updated = adminService.updateUserStatus(
                id, request.getStatus(), currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updated));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<Page<AccountManagementDto>>> getAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AccountManagementDto> accounts = adminService.getAccounts(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PatchMapping("/accounts/{id}/freeze")
    public ResponseEntity<ApiResponse<AccountManagementDto>> freezeAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AccountManagementDto updated = adminService.freezeAccount(
                id, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", updated));
    }

    @PatchMapping("/accounts/{id}/unfreeze")
    public ResponseEntity<ApiResponse<AccountManagementDto>> unfreezeAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AccountManagementDto updated = adminService.unfreezeAccount(
                id, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Account reactivated successfully", updated));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> transactions = adminService.getTransactions(
                type, status, startDate, endDate, minAmount, maxAmount, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/fraud-rules")
    public ResponseEntity<ApiResponse<List<FraudRule>>> getFraudRules() {
        List<FraudRule> rules = adminService.getFraudRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PutMapping("/fraud-rules/{id}")
    public ResponseEntity<ApiResponse<FraudRule>> updateFraudRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRuleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        FraudRule updated = adminService.updateFraudRule(
                id, request, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Fraud rule updated successfully", updated));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getSettings() {
        List<SystemSetting> settings = adminService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/settings/{id}")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSettingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        SystemSetting updated = adminService.updateSetting(
                id, request, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("System setting updated successfully", updated));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTargetType targetType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> logs = adminService.getAuditLogs(action, targetType, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
