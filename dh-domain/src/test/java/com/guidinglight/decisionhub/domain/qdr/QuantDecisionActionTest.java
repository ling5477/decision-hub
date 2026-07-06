package com.guidinglight.decisionhub.domain.qdr;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * QDR action fail-closed 回归。
 *
 * <p>确保 Quant Decision Review 不接受 BUY / SELL / PLACE_ORDER / CANCEL_ORDER，也不接受越界置信度。
 */
class QuantDecisionActionTest {

    @Test
    void qdrActionVocabularyDoesNotContainExecutableTradingActions() {
        assertThrows(IllegalArgumentException.class, () -> QuantDecisionAction.valueOf("BUY"));
        assertThrows(IllegalArgumentException.class, () -> QuantDecisionAction.valueOf("SELL"));
        assertThrows(IllegalArgumentException.class, () -> QuantDecisionAction.valueOf("PLACE_ORDER"));
        assertThrows(IllegalArgumentException.class, () -> QuantDecisionAction.valueOf("CANCEL_ORDER"));
    }

    @Test
    void abstainMapsToNoTradeAndNeverMapsToBuyOrSell() {
        assertEquals(QuantDecisionAction.NO_TRADE,
                QuantDecisionAction.fromDecisionAction(DecisionAction.ABSTAIN));
        assertEquals(QuantDecisionAction.LONG_BIAS,
                QuantDecisionAction.fromDecisionAction(DecisionAction.LONG_BIAS));
    }

    @Test
    void quantDecisionRejectsOutOfRangeConfidence() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new QuantDecision(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                QuantDecisionAction.OBSERVE,
                                new BigDecimal("1.5000"),
                                RiskLevel.LOW,
                                "readonly",
                                Map.of("readOnly", true),
                                HumanApprovalStatus.NOT_REQUIRED,
                                Instant.parse("2026-07-06T00:00:00Z")));
    }
}
