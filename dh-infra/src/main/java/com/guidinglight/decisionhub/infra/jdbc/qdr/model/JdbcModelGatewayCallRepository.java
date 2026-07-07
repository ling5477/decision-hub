package com.guidinglight.decisionhub.infra.jdbc.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallTrustDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.gateway.SaveModelGatewayCallCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ModelGatewayCall JDBC persistence adapter。
 *
 * <p>本 adapter 只访问 DH 自身 `qdr_model_gateway_call` 表；所有查询都带 `tenant_id`，不提供 UUID-only
 * 访问。写入失败必须 fail-closed，不能静默丢失 gateway call metadata。不调用 HTTP、provider、NQ 或交易路径。
 */
public final class JdbcModelGatewayCallRepository implements ModelGatewayCallPersistencePort {

    private static final String COLUMNS =
            "id, tenant_id, trace_id, request_id, decision_run_id, prompt_version_id,"
                    + " model_version_id, provider_profile_id, provider_kind, provider_identity_ref,"
                    + " status, failure_code, trust_decision, provider_trust_decision_ref,"
                    + " model_call_ref, budget_summary, input_characters, rendered_prompt_characters,"
                    + " output_characters, estimated_tokens, memory_entries, redacted_input_summary,"
                    + " redacted_output_summary, input_hash, output_hash, audit_ref, trace_ref, created_at";

    private static final String INSERT_CALL =
            "insert into qdr_model_gateway_call"
                    + " (id, tenant_id, trace_id, request_id, decision_run_id, prompt_version_id,"
                    + " model_version_id, provider_profile_id, provider_kind, provider_identity_ref,"
                    + " status, failure_code, trust_decision, provider_trust_decision_ref,"
                    + " model_call_ref, budget_summary, input_characters, rendered_prompt_characters,"
                    + " output_characters, estimated_tokens, memory_entries, redacted_input_summary,"
                    + " redacted_output_summary, input_hash, output_hash, audit_ref, trace_ref, created_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_TENANT_CALL_REF =
            "select " + COLUMNS
                    + " from qdr_model_gateway_call"
                    + " where tenant_id = ? and model_call_ref = ?"
                    + " limit 1";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     */
    public JdbcModelGatewayCallRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public ModelGatewayCallRecord save(final SaveModelGatewayCallCommand command) {
        final SaveModelGatewayCallCommand checked = Objects.requireNonNull(command, "command");
        try {
            jdbcTemplate.update(
                    INSERT_CALL,
                    checked.modelGatewayCallId(),
                    checked.tenantId(),
                    checked.traceId(),
                    checked.requestId(),
                    checked.decisionRunId(),
                    checked.promptVersionId(),
                    checked.modelVersionId(),
                    checked.providerProfileId(),
                    checked.providerKind().name(),
                    checked.providerIdentityRef(),
                    checked.status().name(),
                    checked.failureCode() == null ? null : checked.failureCode().name(),
                    checked.trustDecision().name(),
                    checked.providerTrustDecisionRef(),
                    checked.modelCallRef(),
                    checked.budgetSummary(),
                    checked.inputCharacters(),
                    checked.renderedPromptCharacters(),
                    checked.outputCharacters(),
                    checked.estimatedTokens(),
                    checked.memoryEntries(),
                    checked.redactedInputSummary(),
                    checked.redactedOutputSummary(),
                    checked.inputHash(),
                    checked.outputHash(),
                    checked.auditRef(),
                    checked.traceRef(),
                    timestamp(checked.createdAt()));
            return toRecord(checked);
        } catch (final DuplicateKeyException error) {
            throw new ModelGatewayCallPersistenceException("model gateway call duplicate ref", error);
        } catch (final DataAccessException error) {
            throw new ModelGatewayCallPersistenceException("save model gateway call failed", error);
        }
    }

    @Override
    public Optional<ModelGatewayCallRecord> findByTenantAndModelCallRef(
            final String tenantId, final String modelCallRef) {
        try {
            return jdbcTemplate
                    .queryForList(
                            SELECT_BY_TENANT_CALL_REF,
                            QdrPersistenceSafety.requireText(tenantId, "tenantId"),
                            QdrPersistenceSafety.requireSafeText(modelCallRef, "modelCallRef"))
                    .stream()
                    .findFirst()
                    .map(this::mapRecord);
        } catch (final DataAccessException error) {
            throw new ModelGatewayCallPersistenceException("find model gateway call failed", error);
        } catch (final RuntimeException error) {
            if (error instanceof ModelGatewayCallPersistenceException persistenceException) {
                throw persistenceException;
            }
            throw new ModelGatewayCallPersistenceException("find model gateway call rejected", error);
        }
    }

    private static ModelGatewayCallRecord toRecord(final SaveModelGatewayCallCommand command) {
        return new ModelGatewayCallRecord(
                command.modelGatewayCallId(),
                command.tenantId(),
                command.traceId(),
                command.requestId(),
                command.decisionRunId(),
                command.promptVersionId(),
                command.modelVersionId(),
                command.providerProfileId(),
                command.providerKind(),
                command.providerIdentityRef(),
                command.status(),
                command.failureCode(),
                command.trustDecision(),
                command.providerTrustDecisionRef(),
                command.modelCallRef(),
                command.budgetSummary(),
                command.inputCharacters(),
                command.renderedPromptCharacters(),
                command.outputCharacters(),
                command.estimatedTokens(),
                command.memoryEntries(),
                command.redactedInputSummary(),
                command.redactedOutputSummary(),
                command.inputHash(),
                command.outputHash(),
                command.auditRef(),
                command.traceRef(),
                command.createdAt());
    }

    private ModelGatewayCallRecord mapRecord(final Map<String, Object> row) {
        return new ModelGatewayCallRecord(
                uuid(row, "id"),
                text(row, "tenant_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                uuid(row, "decision_run_id"),
                uuid(row, "prompt_version_id"),
                uuid(row, "model_version_id"),
                uuid(row, "provider_profile_id"),
                enumValue(row, "provider_kind", ProviderKind.class),
                text(row, "provider_identity_ref"),
                enumValue(row, "status", ModelGatewayCallStatus.class),
                enumValueOrNull(row, "failure_code", ModelGatewayFailureCode.class),
                enumValue(row, "trust_decision", ModelGatewayCallTrustDecision.class),
                text(row, "provider_trust_decision_ref"),
                text(row, "model_call_ref"),
                text(row, "budget_summary"),
                integer(row, "input_characters"),
                integer(row, "rendered_prompt_characters"),
                integer(row, "output_characters"),
                integer(row, "estimated_tokens"),
                integer(row, "memory_entries"),
                text(row, "redacted_input_summary"),
                optionalText(row, "redacted_output_summary"),
                text(row, "input_hash"),
                optionalText(row, "output_hash"),
                optionalText(row, "audit_ref"),
                optionalText(row, "trace_ref"),
                instant(row, "created_at"));
    }

    private static Timestamp timestamp(final Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }

    private static <E extends Enum<E>> E enumValue(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        return Enum.valueOf(enumType, text(row, key));
    }

    private static <E extends Enum<E>> E enumValueOrNull(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        final String value = optionalText(row, key);
        return value == null ? null : Enum.valueOf(enumType, value);
    }

    private static String text(final Map<String, Object> row, final String key) {
        final String value = optionalText(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static String optionalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        final String checked = value.toString().trim();
        return checked.isEmpty() ? null : checked;
    }

    private static UUID uuid(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static int integer(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static Instant instant(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(value.toString());
    }
}
