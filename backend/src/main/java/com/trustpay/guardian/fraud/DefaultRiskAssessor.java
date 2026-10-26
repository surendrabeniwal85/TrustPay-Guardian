package com.trustpay.guardian.fraud;

import org.springframework.stereotype.Component;

/**
 * Default implementation of RiskAssessor.
 * TODO: Phase 6 - Replace or wire complete strategy-based fraud engine into this assessor.
 */
@Component
public class DefaultRiskAssessor implements RiskAssessor {

    private final FraudScoringService fraudScoringService;

    public DefaultRiskAssessor(FraudScoringService fraudScoringService) {
        this.fraudScoringService = fraudScoringService;
    }

    @Override
    public RiskDecision assess(TransactionContext ctx) {
        if (fraudScoringService == null) {
            return RiskDecision.approvedZeroScore();
        }

        FraudRuleEvaluationContext evalCtx = new FraudRuleEvaluationContext(
                ctx.getSourceAccount(),
                ctx.getDestinationAccount(),
                ctx.getAmount(),
                ctx.getSender(),
                ctx.getTimestamp(),
                ctx.getBeneficiary(),
                ctx.getRecent10MinTransfersCount(),
                ctx.getTodayOutgoingTotal(),
                ctx.getThirtyDayAverageAmount(),
                ctx.getThirtyDayTransfersCount(),
                ctx.getFailedLoginAttempts()
        );

        FraudEvaluationResult result = fraudScoringService.evaluateTransaction(evalCtx);

        return new RiskDecision(
                result.getTotalRiskScore(),
                result.getRiskLevel(),
                result.getProposedStatus(),
                result.getHits(),
                result.getSummary()
        );
    }
}
