package com.guidinglight.decisionhub.usecase.qdr.replay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * QDR replay persistence guard 测试。
 *
 * <p>只验证本地 DTO / JSON / action label 安全边界；不访问数据库、不调用 provider/HTTP/NQ/Agent/LangGraph。
 */
final class ReplayPersistenceGuardTest {

    @Test
    void rejectsMissingTenantId() {
        final IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> ReplayPersistenceGuard.requireTenantId(" "));
        assertFalse(error.getMessage().isBlank());
    }

    @Test
    void rejectsPageSizeOverOneHundred() {
        final IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> new ReplayPageRequest(101, 0));
        assertFalse(error.getMessage().isBlank());
    }

    @Test
    void rejectsSensitivePropertyNamesInTextGuards() {
        for (String field :
                List.of(
                        "rawPrompt",
                        "raw_prompt",
                        "promptText",
                        "rawProviderResponse",
                        "raw_provider_response",
                        "providerRaw",
                        "credential",
                        "apiKey",
                        "apiSecret",
                        "passphrase",
                        "token",
                        "cookie",
                        "secret")) {
            final IllegalArgumentException error =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ReplayPersistenceGuard.requireSafeText("redacted", field));
            assertFalse(error.getMessage().isBlank());
        }
    }

    @Test
    void rejectsSensitiveJsonKeysRecursively() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                ReplayPersistenceGuard.rejectUnsafeJson(
                                        Map.of("nested", Map.of("rawProviderResponse", "redacted")),
                                        "summary"));
        assertFalse(error.getMessage().isBlank());
    }

    @Test
    void rejectsExecutableActionLabelValues() {
        for (String action :
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE")) {
            final IllegalArgumentException error =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ReplayPersistenceGuard.requireActionLabel(action));
            assertFalse(error.getMessage().isBlank());
        }
    }

    @Test
    void allowsForbiddenActionsOnlyAsForbiddenList() {
        final ExpectedDecisionSummary summary =
                new ExpectedDecisionSummary(
                        "READ_ONLY_RECOMMENDATION",
                        "OBSERVE",
                        "MEDIUM",
                        RiskLevel.LOW,
                        List.of("evidence-ref-a"),
                        List.of(
                                "BUY",
                                "SELL",
                                "MARKET_ORDER",
                                "PLACE_ORDER",
                                "CANCEL_ORDER",
                                "MUTATE_NQ_STATE"));

        final ExpectedDecisionSummary checked = ReplayPersistenceGuard.requireSummary(summary);
        ReplayPersistenceGuard.rejectUnsafeJson(
                Map.of("forbiddenActions", checked.forbiddenActions()), "summary.forbiddenActions");

        assertEquals("OBSERVE", checked.actionLabel());
    }

    @Test
    void rejectsExecutableActionOutsideForbiddenActionsJsonPath() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                ReplayPersistenceGuard.rejectUnsafeJson(
                                        Map.of("allowedAction", "PLACE_ORDER"), "summary"));
        assertFalse(error.getMessage().isBlank());
    }

    @Test
    void replayPersistenceDtosDoNotExposeForbiddenStorageProperties() {
        assertRecordComponentsDoNotExposeForbiddenMaterial(ReplayCaseRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(EvaluationCaseRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(RegressionVerdictRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(RegressionFindingRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(SaveReplayCaseCommand.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(SaveEvaluationCaseCommand.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(SaveRegressionVerdictCommand.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(SaveRegressionFindingCommand.class);
    }

    private static void assertRecordComponentsDoNotExposeForbiddenMaterial(final Class<?> recordType) {
        assertDoesNotThrow(
                () ->
                        Arrays.stream(recordType.getRecordComponents())
                                .map(RecordComponent::getName)
                                .forEach(
                                        name -> {
                                            final String lower = name.toLowerCase(Locale.ROOT);
                                            assertFalse(lower.contains("raw"), name);
                                            assertFalse(lower.contains("prompttext"), name);
                                            assertFalse(lower.contains("providerraw"), name);
                                            assertFalse(lower.contains("credential"), name);
                                            assertFalse(lower.contains("apikey"), name);
                                            assertFalse(lower.contains("apisecret"), name);
                                            assertFalse(lower.contains("passphrase"), name);
                                            assertFalse(lower.contains("token"), name);
                                            assertFalse(lower.contains("cookie"), name);
                                            assertFalse(lower.contains("secret"), name);
                                        }));
    }
}
