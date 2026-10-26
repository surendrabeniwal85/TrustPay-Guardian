package com.trustpay.guardian.account;

import com.trustpay.guardian.account.dto.AccountResponse;
import com.trustpay.guardian.account.dto.CreateAccountRequest;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.common.web.PageResponse;
import com.trustpay.guardian.security.CurrentUser;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Simulated bank account management, statements and balance APIs")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "List all accounts owned by the authenticated customer")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(
            @CurrentUser UserPrincipal currentUser) {
        List<AccountResponse> accounts = accountService.getUserAccounts(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    @Operation(summary = "Open an extra account (maximum 3 accounts allowed per customer)")
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @CurrentUser UserPrincipal currentUser,
            @RequestBody(required = false) CreateAccountRequest request) {
        AccountResponse response = accountService.openAccount(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific account owned by the user")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountDetails(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        AccountResponse account = accountService.getAccountDetails(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Get paginated account transaction statement with optional date range")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAccountStatement(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<TransactionResponse> statement = accountService.getAccountStatement(
                id, currentUser.getId(), startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Simulate deposit of funds into an owned account")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @CurrentUser UserPrincipal currentUser,
            HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AccountResponse account = accountService.deposit(request, currentUser.getId(), ipAddress, requestId);
        return ResponseEntity.ok(ApiResponse.success("Demo deposit completed successfully", account));
    }
}
