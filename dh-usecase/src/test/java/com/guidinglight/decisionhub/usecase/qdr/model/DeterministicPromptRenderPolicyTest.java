package com.guidinglight.decisionhub.usecase.qdr.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.model.PromptTemplateId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-3 B1 PromptRenderPolicy 回归。
 *
 * <p>render policy 只做本地 deterministic validation/render；不需要 provider 或 HTTP hook。
 */
final class DeterministicPromptRenderPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");

    @Test
    void promptRenderPolicyAllowsSafeInputInMemory() {
        final DeterministicPromptRenderPolicy policy =
                new DeterministicPromptRenderPolicy(new DeterministicPromptInjectionGuard());

        final PromptRenderResult result =
                policy.render(
                        prompt(),
                        new PromptRenderContext(
                                "tenant-a",
                                "trace-a",
                                "request-a",
                                Map.of("symbol", "BTCUSDT", "risk", "LOW")));

        assertTrue(result.allowed());
        assertEquals("Review QDR evidence for BTCUSDT with risk LOW.", result.renderedText());
    }

    @Test
    void promptRenderPolicyDeniesUnsafeInput() {
        final DeterministicPromptRenderPolicy policy =
                new DeterministicPromptRenderPolicy(new DeterministicPromptInjectionGuard());

        final PromptRenderResult result =
                policy.render(
                        prompt(),
                        new PromptRenderContext(
                                "tenant-a",
                                "trace-a",
                                "request-a",
                                Map.of("symbol", "BTCUSDT", "risk", "place order now")));

        assertFalse(result.allowed());
        assertNull(result.renderedText());
        assertEquals("PROMPT_INPUT_DENIED", result.failureCode());
    }

    @Test
    void promptRenderPolicyFailsClosedOnGuardFailure() {
        final DeterministicPromptRenderPolicy policy =
                new DeterministicPromptRenderPolicy(
                        (tenantId, input) -> {
                            throw new IllegalStateException("guard down");
                        });

        final PromptRenderResult result =
                policy.render(
                        prompt(),
                        new PromptRenderContext(
                                "tenant-a",
                                "trace-a",
                                "request-a",
                                Map.of("symbol", "BTCUSDT")));

        assertFalse(result.allowed());
        assertNull(result.renderedText());
        assertEquals("PROMPT_GUARD_FAILURE", result.failureCode());
    }

    @Test
    void promptRenderPolicyDeniesTenantMismatch() {
        final DeterministicPromptRenderPolicy policy =
                new DeterministicPromptRenderPolicy(new DeterministicPromptInjectionGuard());

        final PromptRenderResult result =
                policy.render(
                        prompt(),
                        new PromptRenderContext(
                                "tenant-b",
                                "trace-a",
                                "request-a",
                                Map.of("symbol", "BTCUSDT")));

        assertFalse(result.allowed());
        assertEquals("PROMPT_TENANT_MISMATCH", result.failureCode());
    }

    @Test
    void promptRenderPolicyDoesNotNeedProviderOrHttpInvocation() {
        final RecordingGuard guard = new RecordingGuard();
        final DeterministicPromptRenderPolicy policy = new DeterministicPromptRenderPolicy(guard);

        final PromptRenderResult result =
                policy.render(
                        prompt(),
                        new PromptRenderContext(
                                "tenant-a",
                                "trace-a",
                                "request-a",
                                Map.of("symbol", "BTCUSDT")));

        assertTrue(result.allowed());
        assertEquals(2, guard.invocations);
    }

    private static PromptVersion prompt() {
        return PromptVersion.create(
                new PromptVersionId("prompt-v1"),
                new PromptTemplateId("qdr-review-template"),
                "tenant-a",
                "v1",
                "Review QDR evidence for {{symbol}} with risk {{risk}}.",
                "default-render",
                PromptVersionStatus.ACTIVE,
                NOW,
                "system");
    }

    private static final class RecordingGuard implements PromptInjectionGuard {
        private int invocations;

        @Override
        public PromptInjectionDecision evaluate(final String tenantId, final String input) {
            invocations++;
            return PromptInjectionDecision.allow();
        }
    }
}
