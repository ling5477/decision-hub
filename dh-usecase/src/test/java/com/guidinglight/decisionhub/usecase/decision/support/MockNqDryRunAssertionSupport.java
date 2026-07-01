package com.guidinglight.decisionhub.usecase.decision.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;

import java.util.Set;

/**
 * K6 mock NQ dry-run 合同断言。
 *
 * <p>这些断言集中保护请求输入和输出结果的只读边界：mock NQ 请求必须是 `READ_ONLY_RECOMMENDATION`，
 * output 只能使用分析动作，并且 mandatory forbiddenActions 必须完整保留。
 */
public final class MockNqDryRunAssertionSupport {

    /**
     * K1/K6 允许的只读输出 action 集。
     */
    public static final Set<DecisionAction> ALLOWED_ACTIONS =
            Set.of(
                    DecisionAction.ABSTAIN,
                    DecisionAction.OBSERVE,
                    DecisionAction.NO_TRADE,
                    DecisionAction.LONG_BIAS,
                    DecisionAction.SHORT_BIAS);

    private static final Set<String> FORBIDDEN_REQUEST_TOKENS =
            Set.of(
                    "orderId",
                    "accountId",
                    "apiKey",
                    "apiSecret",
                    "passphrase",
                    "leverage",
                    "quantity",
                    "price",
                    "side",
                    "venueCredential",
                    "brokerCredential",
                    "placeOrder",
                    "cancelOrder");

    private static final Set<String> FORBIDDEN_OUTPUT_TOKENS =
            Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER", "MARKET_ORDER", "LIMIT_ORDER");

    private MockNqDryRunAssertionSupport() {
    }

    /**
     * 断言 mock NQ 请求只表达只读推荐意图。
     *
     * @param request K6 mock NQ request。
     */
    public static void assertMockNqRequestIsReadOnly(final DecisionRequest request) {
        assertNotNull(request);
        assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, request.getDecisionType());
        assertTrue(
                request.getSource().equals("NQ_MOCK") || request.getSource().equals("NQ"),
                "source must be NQ or NQ_MOCK");
        assertNotNull(request.getSubject());
        assertNotNull(request.getContextSnapshot());
        assertEquals(DecisionRequest.DEFAULT_SCHEMA_VERSION, request.getSchemaVersion());
        assertNoForbiddenRequestTokens(request.getRequestId());
        assertNoForbiddenRequestTokens(request.getTraceId());
        assertNoForbiddenRequestTokens(request.getTenantId());
        assertNoForbiddenRequestTokens(request.getSource());
        assertNoForbiddenRequestTokens(request.getSubject().symbol());
        assertNoForbiddenRequestTokens(request.getSubject().market());
        assertNoForbiddenRequestTokens(request.getSubject().timeframe());
        assertNoForbiddenRequestTokens(request.getSubject().strategyRef());
        assertNoForbiddenRequestTokens(request.getSubject().researchRef());
        assertNoForbiddenRequestTokens(request.getContextRef());
        assertFalse(request.getContextSnapshot().evidenceRefs().isEmpty());
    }

    /**
     * 断言输出仍是 K1 structured read-only contract。
     *
     * @param output orchestrator 输出。
     */
    public static void assertStructuredReadOnlyOutput(final DecisionOutput output) {
        assertNotNull(output);
        assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, output.getDecisionType());
        assertTrue(ALLOWED_ACTIONS.contains(output.getAction()), "unexpected output action");
        assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
        assertNoForbiddenOutputTokens(output.getAction().name());
        for (String reasonCode : output.getReasonCodes()) {
            assertNoForbiddenOutputTokens(reasonCode);
        }
    }

    /**
     * 断言 fixture 文本不包含 credential、order 或 execution intent 字段。
     *
     * @param fixtureBody fixture JSON 文本。
     */
    public static void assertFixtureContainsNoCredentialOrExecutionIntent(
            final String fixtureBody) {
        assertNotNull(fixtureBody);
        for (String token : FORBIDDEN_REQUEST_TOKENS) {
            assertFalse(
                    containsIgnoreCase(fixtureBody, token), "fixture must not contain token: " + token);
        }
        for (String token : FORBIDDEN_OUTPUT_TOKENS) {
            assertFalse(fixtureBody.contains(token), "fixture must not contain output token: " + token);
        }
    }

    private static void assertNoForbiddenRequestTokens(final String value) {
        if (value == null) {
            return;
        }
        for (String token : FORBIDDEN_REQUEST_TOKENS) {
            assertFalse(containsIgnoreCase(value, token), "request must not contain token: " + token);
        }
    }

    private static void assertNoForbiddenOutputTokens(final String value) {
        if (value == null) {
            return;
        }
        for (String token : FORBIDDEN_OUTPUT_TOKENS) {
            assertFalse(value.contains(token), "output must not contain token: " + token);
        }
    }

    private static boolean containsIgnoreCase(final String value, final String token) {
        return value.toLowerCase(java.util.Locale.ROOT).contains(token.toLowerCase(java.util.Locale.ROOT));
    }
}
