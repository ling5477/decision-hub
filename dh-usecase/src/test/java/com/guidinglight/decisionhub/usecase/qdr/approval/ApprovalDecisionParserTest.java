package com.guidinglight.decisionhub.usecase.qdr.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * `ApprovalDecisionParser` 的安全解析回归测试。
 *
 * <p>测试重点是证明非法 approval decision 会在进入 command service 前 fail-closed，并且异常 message
 * 不携带 raw request value、enum class 名或 `Enum.valueOf` 的原始 cause。
 */
final class ApprovalDecisionParserTest {

    @Test
    void parseSafeAcceptsOnlyApprovalStateMachineDecisions() {
        assertEquals(ApprovalDecision.APPROVED, ApprovalDecisionParser.parseSafe("APPROVED"));
        assertEquals(ApprovalDecision.REJECTED, ApprovalDecisionParser.parseSafe("REJECTED"));
        assertEquals(ApprovalDecision.NEEDS_REVIEW, ApprovalDecisionParser.parseSafe("NEEDS_REVIEW"));
        assertEquals(ApprovalDecision.APPROVED, ApprovalDecisionParser.parseSafe(" APPROVED "));
    }

    @Test
    void parseSafeRejectsTradingVocabularyWithoutLeakingRawValue() {
        for (String rawValue : List.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER")) {
            assertInvalidWithoutRawLeak(rawValue);
        }
    }

    @Test
    void parseSafeRejectsNullBlankAndUnknownWithoutEnumCause() {
        assertInvalidWithoutRawLeak(null);
        assertInvalidWithoutRawLeak("");
        assertInvalidWithoutRawLeak("   ");
        assertInvalidWithoutRawLeak("UNKNOWN_DECISION");
        assertInvalidWithoutRawLeak("approved");
    }

    private static void assertInvalidWithoutRawLeak(final String rawValue) {
        final InvalidApprovalDecisionException error =
                assertThrows(
                        InvalidApprovalDecisionException.class,
                        () -> ApprovalDecisionParser.parseSafe(rawValue));
        assertEquals("Invalid approval decision.", error.getMessage());
        assertFalse(error.getMessage().contains("No enum constant"));
        assertFalse(error.getMessage().contains("ApprovalDecision"));
        for (String forbidden :
                List.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER", "UNKNOWN_DECISION")) {
            assertFalse(error.getMessage().contains(forbidden));
        }
    }
}
