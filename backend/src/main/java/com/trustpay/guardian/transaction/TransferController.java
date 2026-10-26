package com.trustpay.guardian.transaction;

import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.security.CurrentUser;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @CurrentUser UserPrincipal currentUser,
            HttpServletRequest servletRequest) {

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

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(
            @PathVariable Long accountId,
            @CurrentUser UserPrincipal currentUser) {
        List<TransactionResponse> transactions = transferService.getAccountTransactions(accountId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/account/{accountId}/paged")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAccountTransactionsPaged(
            @PathVariable Long accountId,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<TransactionResponse> page = transferService.getAccountTransactionsPaged(accountId, currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
