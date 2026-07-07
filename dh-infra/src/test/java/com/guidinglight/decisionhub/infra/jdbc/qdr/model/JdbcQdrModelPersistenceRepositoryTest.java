package com.guidinglight.decisionhub.infra.jdbc.qdr.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallTrustDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.SaveModelGatewayCallCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.SaveModelVersionCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.SavePromptVersionCommand;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * stage-qdr-3 B3 QDR model persistence JDBC repository tests。
 *
 * <p>使用 fake JdbcTemplate 校验 tenant-bound SQL、duplicate checksum 行为、redaction storage boundary 与
 * fail-closed exception；不连接真实数据库、不调用 provider/HTTP/NQ。
 */
final class JdbcQdrModelPersistenceRepositoryTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String OTHER_TENANT_ID = "tenant-b";
    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final UUID PROMPT_TEMPLATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID PROMPT_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID MODEL_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID MODEL_VERSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID PROVIDER_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000305");
    private static final UUID MODEL_GATEWAY_CALL_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000306");
    private static final UUID DECISION_RUN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000307");
    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");

    private RecordingJdbcTemplate jdbcTemplate;
    private JdbcPromptVersionRepository promptRepository;
    private JdbcModelVersionRepository modelRepository;
    private JdbcModelGatewayCallRepository gatewayRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new RecordingJdbcTemplate();
        promptRepository = new JdbcPromptVersionRepository(jdbcTemplate);
        modelRepository = new JdbcModelVersionRepository(jdbcTemplate);
        gatewayRepository = new JdbcModelGatewayCallRepository(jdbcTemplate);
    }

    @Test
    void savePromptVersionSuccessInsertsTemplateAndVersion() {
        final PromptVersionRecord saved = promptRepository.save(promptCommand(HASH_A));

        assertThat(saved.promptVersionId()).isEqualTo(PROMPT_VERSION_ID);
        assertThat(jdbcTemplate.updates()).hasSize(2);
        assertThat(jdbcTemplate.updates().get(0).sql())
                .contains("insert into qdr_prompt_template")
                .contains("tenant_id");
        assertThat(jdbcTemplate.updates().get(1).sql())
                .contains("insert into qdr_prompt_version")
                .contains("tenant_id");
    }

    @Test
    void lookupPromptVersionSameTenantSuccess() {
        jdbcTemplate.rows = List.of(promptRow(TENANT_ID, HASH_A));

        final var found =
                promptRepository.findByTenantAndTemplateVersion(TENANT_ID, PROMPT_TEMPLATE_ID, "v1");

        assertThat(found).isPresent();
        assertThat(found.get().checksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.firstQuery().sql())
                .contains("pv.tenant_id = ? and pv.prompt_template_id = ? and pv.version = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, PROMPT_TEMPLATE_ID, "v1");
    }

    @Test
    void lookupPromptVersionCrossTenantNotFound() {
        jdbcTemplate.rows = List.of(promptRow(TENANT_ID, HASH_A));

        final var found =
                promptRepository.findByTenantAndTemplateVersion(
                        OTHER_TENANT_ID, PROMPT_TEMPLATE_ID, "v1");

        assertThat(found).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args())
                .containsExactly(OTHER_TENANT_ID, PROMPT_TEMPLATE_ID, "v1");
    }

    @Test
    void duplicatePromptVersionSameChecksumIsIdempotent() {
        jdbcTemplate.rows = List.of(promptRow(TENANT_ID, HASH_A));

        final PromptVersionRecord saved = promptRepository.save(promptCommand(HASH_A));

        assertThat(saved.checksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.updates()).isEmpty();
    }

    @Test
    void duplicatePromptVersionDifferentChecksumFailsClosed() {
        jdbcTemplate.rows = List.of(promptRow(TENANT_ID, HASH_A));

        assertThatThrownBy(() -> promptRepository.save(promptCommand(HASH_B)))
                .isInstanceOf(PromptVersionChecksumConflictException.class)
                .hasMessageContaining("checksum conflict");
    }

    @Test
    void promptVersionRecordDoesNotExposeRawPrompt() {
        assertRecordComponentsDoNotExposeForbiddenMaterial(PromptVersionRecord.class);
    }

    @Test
    void saveModelVersionSuccessInsertsProfileAndVersion() {
        final var saved = modelRepository.save(modelCommand(HASH_A));

        assertThat(saved.modelVersionId()).isEqualTo(MODEL_VERSION_ID);
        assertThat(jdbcTemplate.updates()).hasSize(2);
        assertThat(jdbcTemplate.updates().get(0).sql())
                .contains("insert into qdr_model_profile")
                .contains("tenant_id");
        assertThat(jdbcTemplate.updates().get(1).sql())
                .contains("insert into qdr_model_version")
                .contains("tenant_id");
    }

    @Test
    void lookupModelVersionSameTenantSuccess() {
        jdbcTemplate.rows = List.of(modelRow(TENANT_ID, HASH_A));

        final var found = modelRepository.findByTenantAndModelVersionId(TENANT_ID, MODEL_VERSION_ID);

        assertThat(found).isPresent();
        assertThat(found.get().checksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.firstQuery().sql()).contains("where mv.tenant_id = ? and mv.id = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, MODEL_VERSION_ID);
    }

    @Test
    void lookupModelVersionCrossTenantNotFound() {
        jdbcTemplate.rows = List.of(modelRow(TENANT_ID, HASH_A));

        final var found =
                modelRepository.findByTenantAndModelVersionId(OTHER_TENANT_ID, MODEL_VERSION_ID);

        assertThat(found).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(OTHER_TENANT_ID, MODEL_VERSION_ID);
    }

    @Test
    void duplicateModelVersionSameChecksumIsIdempotent() {
        jdbcTemplate.rows = List.of(modelRow(TENANT_ID, HASH_A));

        final var saved = modelRepository.save(modelCommand(HASH_A));

        assertThat(saved.checksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.updates()).isEmpty();
    }

    @Test
    void duplicateModelVersionDifferentChecksumFailsClosed() {
        jdbcTemplate.rows = List.of(modelRow(TENANT_ID, HASH_A));

        assertThatThrownBy(() -> modelRepository.save(modelCommand(HASH_B)))
                .isInstanceOf(ModelVersionChecksumConflictException.class)
                .hasMessageContaining("checksum conflict");
    }

    @Test
    void modelVersionRecordDoesNotExposeCredential() {
        assertRecordComponentsDoNotExposeForbiddenMaterial(
                com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord.class);
    }

    @Test
    void saveModelGatewayCallSuccess() {
        final var saved = gatewayRepository.save(gatewayCommand());

        assertThat(saved.modelGatewayCallId()).isEqualTo(MODEL_GATEWAY_CALL_ID);
        assertThat(jdbcTemplate.firstUpdate().sql())
                .contains("insert into qdr_model_gateway_call")
                .contains("tenant_id");
    }

    @Test
    void lookupModelGatewayCallSameTenantSuccess() {
        jdbcTemplate.rows = List.of(gatewayRow(TENANT_ID));

        final var found = gatewayRepository.findByTenantAndModelCallRef(TENANT_ID, "model-call-a");

        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(ModelGatewayCallStatus.SUCCEEDED);
        assertThat(jdbcTemplate.firstQuery().sql())
                .contains("where tenant_id = ? and model_call_ref = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, "model-call-a");
    }

    @Test
    void lookupModelGatewayCallCrossTenantNotFound() {
        jdbcTemplate.rows = List.of(gatewayRow(TENANT_ID));

        final var found = gatewayRepository.findByTenantAndModelCallRef(OTHER_TENANT_ID, "model-call-a");

        assertThat(found).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(OTHER_TENANT_ID, "model-call-a");
    }

    @Test
    void gatewayCallRecordDoesNotExposeRawPromptOrProviderResponse() {
        assertRecordComponentsDoNotExposeForbiddenMaterial(
                com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord.class);
    }

    @Test
    void repositorySqlFailureFailsClosed() {
        jdbcTemplate.failure = new DataAccessResourceFailureException("synthetic database failure");

        assertThatThrownBy(
                        () ->
                                promptRepository.findByTenantAndTemplateVersion(
                                        TENANT_ID, PROMPT_TEMPLATE_ID, "v1"))
                .isInstanceOf(PromptVersionPersistenceException.class)
                .hasMessageContaining("find prompt version failed");
        assertThatThrownBy(() -> modelRepository.findByTenantAndModelVersionId(TENANT_ID, MODEL_VERSION_ID))
                .isInstanceOf(ModelVersionPersistenceException.class)
                .hasMessageContaining("find model version failed");
        assertThatThrownBy(() -> gatewayRepository.save(gatewayCommand()))
                .isInstanceOf(ModelGatewayCallPersistenceException.class)
                .hasMessageContaining("save model gateway call failed");
    }

    @Test
    void repositoryNeverUsesUuidOnlyQueryOrUpdate() {
        jdbcTemplate.rows = List.of(promptRow(TENANT_ID, HASH_A));
        promptRepository.findByTenantAndTemplateVersion(TENANT_ID, PROMPT_TEMPLATE_ID, "v1");
        jdbcTemplate.rows = List.of(modelRow(TENANT_ID, HASH_A));
        modelRepository.findByTenantAndModelVersionId(TENANT_ID, MODEL_VERSION_ID);
        jdbcTemplate.rows = List.of(gatewayRow(TENANT_ID));
        gatewayRepository.findByTenantAndModelCallRef(TENANT_ID, "model-call-a");
        gatewayRepository.save(gatewayCommand());

        assertThat(jdbcTemplate.queries()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id = ?"));
        assertThat(jdbcTemplate.updates()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id"));
        assertThat(jdbcTemplate.queries())
                .noneSatisfy(
                        query ->
                                assertThat(query.sql().toLowerCase(Locale.ROOT))
                                        .contains("where id = ?"));
    }

    @Test
    void gatewayCallDuplicateWriteFailsClosed() {
        jdbcTemplate.duplicateOnUpdate = true;

        assertThatThrownBy(() -> gatewayRepository.save(gatewayCommand()))
                .isInstanceOf(ModelGatewayCallPersistenceException.class)
                .hasMessageContaining("duplicate ref");
    }

    private static SavePromptVersionCommand promptCommand(final String checksum) {
        return new SavePromptVersionCommand(
                PROMPT_TEMPLATE_ID,
                PROMPT_VERSION_ID,
                TENANT_ID,
                "qdr-review",
                "qdr review",
                "v1",
                "deterministic-render",
                "prompt-template-ref-a",
                HASH_A,
                "redacted prompt summary",
                PromptVersionStatus.ACTIVE,
                checksum,
                NOW,
                "system");
    }

    private static SaveModelVersionCommand modelCommand(final String checksum) {
        return new SaveModelVersionCommand(
                MODEL_PROFILE_ID,
                MODEL_VERSION_ID,
                PROVIDER_PROFILE_ID,
                TENANT_ID,
                ProviderKind.MOCK,
                "mock-provider",
                "mock-model",
                "mock model",
                "deterministic capability",
                4096,
                512,
                ProviderProfileStatus.ENABLED,
                "trust-policy-ref-a",
                "mock-model",
                "2026-07-07",
                checksum,
                NOW);
    }

    private static SaveModelGatewayCallCommand gatewayCommand() {
        return new SaveModelGatewayCallCommand(
                MODEL_GATEWAY_CALL_ID,
                TENANT_ID,
                "trace-a",
                "request-a",
                DECISION_RUN_ID,
                PROMPT_VERSION_ID,
                MODEL_VERSION_ID,
                PROVIDER_PROFILE_ID,
                ProviderKind.MOCK,
                "mock-provider",
                ModelGatewayCallStatus.SUCCEEDED,
                null,
                ModelGatewayCallTrustDecision.ALLOWED,
                "provider-trust-ref-a",
                "model-call-a",
                "budget summary",
                100,
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

    private static Map<String, Object> promptRow(final String tenantId, final String checksum) {
        return row(
                "prompt_template_id",
                PROMPT_TEMPLATE_ID,
                "prompt_version_id",
                PROMPT_VERSION_ID,
                "tenant_id",
                tenantId,
                "template_key",
                "qdr-review",
                "version",
                "v1",
                "render_policy_key",
                "deterministic-render",
                "template_ref",
                "prompt-template-ref-a",
                "template_hash",
                HASH_A,
                "redacted_summary",
                "redacted prompt summary",
                "status",
                "ACTIVE",
                "checksum",
                checksum,
                "created_at",
                NOW,
                "created_by",
                "system");
    }

    private static Map<String, Object> modelRow(final String tenantId, final String checksum) {
        return row(
                "model_profile_id",
                MODEL_PROFILE_ID,
                "model_version_id",
                MODEL_VERSION_ID,
                "provider_profile_id",
                PROVIDER_PROFILE_ID,
                "tenant_id",
                tenantId,
                "provider_kind",
                "MOCK",
                "provider_key",
                "mock-provider",
                "model_key",
                "mock-model",
                "display_name",
                "mock model",
                "capability_summary",
                "deterministic capability",
                "context_window_tokens",
                4096,
                "max_output_tokens",
                512,
                "profile_status",
                "ENABLED",
                "trust_policy_ref",
                "trust-policy-ref-a",
                "model_name",
                "mock-model",
                "model_version",
                "2026-07-07",
                "checksum",
                checksum,
                "created_at",
                NOW);
    }

    private static Map<String, Object> gatewayRow(final String tenantId) {
        return row(
                "id",
                MODEL_GATEWAY_CALL_ID,
                "tenant_id",
                tenantId,
                "trace_id",
                "trace-a",
                "request_id",
                "request-a",
                "decision_run_id",
                DECISION_RUN_ID,
                "prompt_version_id",
                PROMPT_VERSION_ID,
                "model_version_id",
                MODEL_VERSION_ID,
                "provider_profile_id",
                PROVIDER_PROFILE_ID,
                "provider_kind",
                "MOCK",
                "provider_identity_ref",
                "mock-provider",
                "status",
                "SUCCEEDED",
                "failure_code",
                null,
                "trust_decision",
                "ALLOWED",
                "provider_trust_decision_ref",
                "provider-trust-ref-a",
                "model_call_ref",
                "model-call-a",
                "budget_summary",
                "budget summary",
                "input_characters",
                100,
                "rendered_prompt_characters",
                120,
                "output_characters",
                80,
                "estimated_tokens",
                30,
                "memory_entries",
                2,
                "redacted_input_summary",
                "redacted input summary",
                "redacted_output_summary",
                "redacted output summary",
                "input_hash",
                HASH_A,
                "output_hash",
                HASH_B,
                "audit_ref",
                "audit-ref-a",
                "trace_ref",
                "trace-ref-a",
                "created_at",
                NOW);
    }

    private static Map<String, Object> row(final Object... pairs) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            row.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return row;
    }

    private static void assertRecordComponentsDoNotExposeForbiddenMaterial(final Class<?> recordType) {
        assertThat(Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName))
                .allSatisfy(
                        name -> {
                            final String lower = name.toLowerCase(Locale.ROOT);
                            assertThat(lower).doesNotContain("raw");
                            assertThat(lower).doesNotContain("credential");
                            assertThat(lower).doesNotContain("apikey");
                            assertThat(lower).doesNotContain("api_secret");
                            assertThat(lower).doesNotContain("passphrase");
                        });
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<Query> queries = new ArrayList<>();
        private final List<Query> updates = new ArrayList<>();
        private List<Map<String, Object>> rows = List.of();
        private RuntimeException failure;
        private boolean duplicateOnUpdate;

        @Override
        public List<Map<String, Object>> queryForList(final String sql, final Object... args) {
            queries.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (failure != null) {
                throw failure;
            }
            if (!args[0].equals(TENANT_ID)) {
                return List.of();
            }
            return rows;
        }

        @Override
        public int update(final String sql, final Object... args) {
            updates.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (duplicateOnUpdate) {
                throw new DuplicateKeyException("duplicate model call");
            }
            if (failure != null) {
                throw failure;
            }
            return 1;
        }

        Query firstQuery() {
            return queries.getFirst();
        }

        Query firstUpdate() {
            return updates.getFirst();
        }

        List<Query> queries() {
            return List.copyOf(queries);
        }

        List<Query> updates() {
            return List.copyOf(updates);
        }
    }

    private record Query(String sql, List<Object> args) {
    }
}
