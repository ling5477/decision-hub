package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B1 evidence projection 的脱敏与引用边界回归。
 */
class DecisionEvidenceViewTest {

    @Test
    void redactedEvidenceRefsPass() {
        final DecisionEvidenceView view =
                new DecisionEvidenceView(
                        "decision-run-1",
                        "tenant-1",
                        "trace-1",
                        "context-snapshot://snapshot-1",
                        List.of("provider-call-log://call-1"),
                        "decision-output://output-1",
                        "quant-decision://decision-1",
                        RedactionStatus.REDACTED,
                        "{\"refs\":[\"evidence://safe-1\"]}");

        assertEquals(RedactionStatus.REDACTED, view.redactionStatus());
        assertEquals(List.of("provider-call-log://call-1"), view.providerCallLogRefs());
    }

    @Test
    void rawOrUnredactedStatusNamesAreNotPartOfAllowedEnum() {
        assertThrows(IllegalArgumentException.class, () -> RedactionStatus.valueOf("RAW"));
        assertThrows(IllegalArgumentException.class, () -> RedactionStatus.valueOf("UNREDACTED"));
    }

    @Test
    void providerRefsCannotContainRawProviderResponse() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DecisionEvidenceView(
                                "decision-run-1",
                                "tenant-1",
                                "trace-1",
                                "context-snapshot://snapshot-1",
                                List.of("raw provider response: full body"),
                                "decision-output://output-1",
                                "quant-decision://decision-1",
                                RedactionStatus.SUMMARY_ONLY,
                                "{\"refs\":[\"evidence://safe-1\"]}"));
    }

    @Test
    void evidenceRefsJsonCannotContainExecutableAction() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DecisionEvidenceView(
                                "decision-run-1",
                                "tenant-1",
                                "trace-1",
                                "context-snapshot://snapshot-1",
                                List.of("provider-call-log://call-1"),
                                "decision-output://output-1",
                                "quant-decision://decision-1",
                                RedactionStatus.REDACTED,
                                "{\"action\":\"SELL\"}"));
    }
}
