package com.trustpay.guardian.fraud;

import com.trustpay.guardian.fraud.engine.FraudEngine;
import org.springframework.stereotype.Service;

/**
 * Service bridging high-level transaction evaluation to the strategy-based FraudEngine.
 */
@Service
public class FraudScoringService {

    private final FraudEngine fraudEngine;

    public FraudScoringService(FraudEngine fraudEngine) {
        this.fraudEngine = fraudEngine;
    }

    public FraudEvaluationResult evaluateTransaction(FraudRuleEvaluationContext context) {
        TransactionContext txContext = new TransactionContext(
                context.getSourceAccount(),
                context.getDestinationAccount(),
                context.getTransferAmount(),
                context.getSenderUser(),
                context.getTransferTime(),
                context.getBeneficiary(),
                context.getRecentTransfersCountIn10Min(),
                context.getTodayTotalOutgoing(),
                context.getThirtyDayAverageAmount(),
                context.getThirtyDayCount(),
                context.getRecentFailedLoginAttempts()
        );

        RiskDecision decision = fraudEngine.assess(txContext);

        return new FraudEvaluationResult(
                decision.getTotalRiskScore(),
                decision.getRiskLevel(),
                decision.getProposedStatus(),
                decision.getHits(),
                decision.getSummary()
        );
    }
}
