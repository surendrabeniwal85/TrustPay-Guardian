package com.trustpay.guardian.beneficiary;

import com.trustpay.guardian.beneficiary.dto.BeneficiaryLookupResponse;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryRequest;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryResponse;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.security.CurrentUser;
import com.trustpay.guardian.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "Beneficiaries", description = "Beneficiary address book management and lookup APIs")
@SecurityRequirement(name = "bearerAuth")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup recipient account number and retrieve masked account holder name")
    public ResponseEntity<ApiResponse<BeneficiaryLookupResponse>> lookupBeneficiary(
            @RequestParam String accountNumber) {
        BeneficiaryLookupResponse response = beneficiaryService.lookup(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Add a new beneficiary to the user's address book")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
            @Valid @RequestBody BeneficiaryRequest request,
            @CurrentUser UserPrincipal currentUser) {
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary added successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all beneficiaries registered by the current user")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries(
            @CurrentUser UserPrincipal currentUser) {
        List<BeneficiaryResponse> beneficiaries = beneficiaryService.getUserBeneficiaries(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(beneficiaries));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a beneficiary by ID")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        beneficiaryService.deleteBeneficiary(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("Beneficiary removed successfully"));
    }
}
