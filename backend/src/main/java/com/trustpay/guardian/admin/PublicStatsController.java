package com.trustpay.guardian.admin;

import com.trustpay.guardian.admin.dto.PublicStatsResponse;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.fraud.FraudAlertRepository;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Public endpoint delivering non-sensitive aggregate operational metrics.
 * Accessible to anonymous users for landing page counters.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Metrics", description = "Anonymous aggregate operational platform counters")
public class PublicStatsController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public PublicStatsController(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            FraudAlertRepository fraudAlertRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get public aggregate stats", description = "Retrieve non-sensitive platform metrics: volume, transfers, users, alerts analyzed")
    public ResponseEntity<ApiResponse<PublicStatsResponse>> getPublicStats() {
        long totalTransfers = transactionRepository.count();
        BigDecimal totalVolume = transactionRepository.sumCompletedVolume();
        long totalUsers = userRepository.count();
        long alertsAnalyzed = fraudAlertRepository.count();

        PublicStatsResponse response = new PublicStatsResponse(
                totalTransfers,
                totalVolume != null ? totalVolume : BigDecimal.ZERO,
                totalUsers,
                alertsAnalyzed
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
