package com.guidinglight.decisionhub.usecase.qdr.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallTrustDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.gateway.SaveModelGatewayCallCommand;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-3 B3 persistence command safety tests。
 *
 * <p>校验 usecase persistence command/record 不暴露 raw prompt、provider 原始响应或凭证字段，并对危险输入
 * fail-closed。
 */
final class QdrPersistenceCommandSafetyTest {

    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final UUID PROMPT_TEMPLATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID PROMPT_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID MODEL_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000403");
    private static final UUID MODEL_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000404");
    private static final UUID PROVIDER_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000405");
    private static final UUID MODEL_GATEWAY_CALL_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000406");
    private static final UUID DECISION_RUN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000407");
    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");

    @Test
    void promptPersistenceCommandRejectsSecretLikeSummary() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> promptCommand("contains apiKey placeholder", PromptVersionStatus.ACTIVE));

        assertTrue(error.getMessage().contains("redactedSummary rejected"));
    }

    @Test
    void modelPersistenceCommandRejectsCredentialLikeProviderKey() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new SaveModelVersionCommand(
                                        MODEL_PROFILE_ID,
                                        MODEL_VERSION_ID,
                                        PROVIDER_PROFILE_ID,
                                        "tenant-a",
                                        ProviderKind.MOCK,
                                        "credential-provider",
                                        "mock-model",
                                        "mock model",
                                        "deterministic capability",
                                        4096,
                                        512,
                                        ProviderProfileStatus.ENABLED,
                                        "trust-policy-ref-a",
                                        "mock-model",
                                        "2026-07-07",
                                        HASH_A,
                                        NOW));

        assertTrue(error.getMessage().contains("providerKey rejected"));
    }

    @Test
    void gatewayCallCommandRequiresFailureCodeForFailedStatus() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> gatewayCommand(ModelGatewayCallStatus.FAILED, null, 10));

        assertTrue(error.getMessage().contains("failed model gateway call must have failureCode"));
    }

    @Test
    void gatewayCallCommandRejectsNegativeUsage() {
        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> gatewayCommand(ModelGatewayCallStatus.SUCCEEDED, null, -1));

        assertTrue(error.getMessage().contains("inputCharacters must be non-negative"));
    }

    @Test
    void persistenceRecordsDoNotExposeForbiddenFieldNames() {
        assertRecordComponentsDoNotExposeForbiddenMaterial(PromptVersionRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(ModelVersionRecord.class);
        assertRecordComponentsDoNotExposeForbiddenMaterial(ModelGatewayCallRecord.class);
    }

    private static SavePromptVersionCommand promptCommand(
            final String redactedSummary, final PromptVersionStatus status) {
        return new SavePromptVersionCommand(
                PROMPT_TEMPLATE_ID,
                PROMPT_VERSION_ID,
                "tenant-a",
                "qdr-review",
                "qdr review",
                "v1",
                "deterministic-render",
                "prompt-template-ref-a",
                HASH_A,
                redactedSummary,
                status,
                HASH_A,
                NOW,
                "system");
    }

    private static SaveModelGatewayCallCommand gatewayCommand(
            final ModelGatewayCallStatus status,
            final ModelGatewayFailureCode failureCode,
            final int inputCharacters) {
        return new SaveModelGatewayCallCommand(
                MODEL_GATEWAY_CALL_ID,
                "tenant-a",
                "trace-a",
                "request-a",
                DECISION_RUN_ID,
                PROMPT_VERSION_ID,
                MODEL_VERSION_ID,
                PROVIDER_PROFILE_ID,
                ProviderKind.MOCK,
                "mock-provider",
                status,
                failureCode,
                ModelGatewayCallTrustDecision.ALLOWED,
                "provider-trust-ref-a",
                "model-call-a",
                "budget summary",
                inputCharacters,
                120,
                80,
                30,
                2,
                "redacted input summary",
                "redacted output summary",
                HASH_A,
                HASH_B,
                "audit-ref-a",
                "trace-ref-a",
                NOW);
    }

    private static void assertRecordComponentsDoNotExposeForbiddenMaterial(final Class<?> recordType) {
        Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .forEach(
                        name -> {
                            final String lower = name.toLowerCase(Locale.ROOT);
                            assertFalse(lower.contains("raw"));
                            assertFalse(lower.contains("credential"));
                            assertFalse(lower.contains("apikey"));
                            assertFalse(lower.contains("api_secret"));
                            assertFalse(lower.contains("passphrase"));
                        });
    }
}
