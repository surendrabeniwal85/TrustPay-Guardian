package com.trustpay.guardian.transaction;

import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.common.web.PageResponse;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.security.CurrentUser;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Money movement, simulated transfers, deposits and history")
public class TransactionController {

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    @Operation(summary = "Initiate simulated transfer between accounts with idempotency protection")
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @CurrentUser UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException("Idempotency-Key header is required for transfers.");
        }

        try {
            UUID.fromString(idempotencyKey.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Idempotency-Key must be a valid UUID.");
        }

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        TransactionResponse response = transferService.initiateTransfer(
                request, idempotencyKey, currentUser.getId(), ipAddress, requestId);

        HttpStatus status = response.getStatus() == TransactionStatus.APPROVED_SIMULATED
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;

        String message = response.getStatus() == TransactionStatus.APPROVED_SIMULATED
                ? "Transfer completed successfully."
                : response.getStatus() == TransactionStatus.PENDING_REVIEW
                ? "Transfer held for fraud analyst review. Reservation active."
                : "Transfer blocked by fraud security policy.";

        return ResponseEntity.status(status).body(ApiResponse.success(message, response));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit demo funds into an account")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @CurrentUser UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        TransactionResponse response = transferService.deposit(
                request, currentUser.getId(), ipAddress, requestId);

        return ResponseEntity.ok(ApiResponse.success("Deposit completed successfully.", response));
    }

    @GetMapping
    @Operation(summary = "List and filter transactions for current customer")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUser UserPrincipal currentUser) {

        PageResponse<TransactionResponse> result = transferService.getFilteredTransactions(
                currentUser.getId(), type, status, startDate, endDate, minAmount, maxAmount, search, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction details by unique reference ID (owner only)")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByReference(
            @PathVariable String reference,
            @CurrentUser UserPrincipal currentUser) {

        boolean isStaff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.FRAUD_ANALYST;
        TransactionResponse response = transferService.getTransactionByReference(
                reference, currentUser.getId(), isStaff);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
