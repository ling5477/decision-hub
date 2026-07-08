package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.enumValue;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.instant;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.optionalText;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readInputRef;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readSummary;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.text;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.timestamp;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.uuid;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.writeJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.DecisionSummaryRole;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPageRequest;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveReplayCaseCommand;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * QDR replay case JDBC repository。
 *
 * <p>所有 SQL 查询都带 `tenant_id`，保存时先写结构化 input ref 与 expected summary，再写 replay case。
 * 本 adapter 不执行 replay、不调用 provider/HTTP/NQ，也不把 replay output 解释为 trading signal。
 */
public final class JdbcReplayCaseRepository implements ReplayCaseRepository {

    private static final String REPLAY_COLUMNS =
            "rc.id, rc.tenant_id, rc.case_id, rc.source_decision_id, rc.source_request_id,"
                    + " rc.trace_id, rc.request_id, rc.policy_version, rc.model_gateway_version_ref,"
                    + " rc.input_ref_id, rc.expected_summary_id, rc.expected_summary_hash,"
                    + " rc.case_checksum, rc.created_at, rc.updated_at,"
                    + " ir.input_ref, es.summary_json as expected_summary_json,"
                    + " es.required_evidence_refs_json, es.forbidden_actions_json";

    private static final String REPLAY_JOIN =
            " from qdr_replay_case rc"
                    + " join qdr_replay_input_ref ir"
                    + " on ir.tenant_id = rc.tenant_id and ir.id = rc.input_ref_id"
                    + " join qdr_expected_decision_summary es"
                    + " on es.tenant_id = rc.tenant_id and es.id = rc.expected_summary_id";

    private static final String INSERT_INPUT_REF =
            "insert into qdr_replay_input_ref"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, ref_type, ref_id, input_ref, content_hash,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)";

    private static final String INSERT_EXPECTED_SUMMARY =
            "insert into qdr_expected_decision_summary"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id, summary_role,"
                    + " decision_type, action_label, confidence_band, risk_level, summary_json,"
                    + " required_evidence_refs_json, forbidden_actions_json, summary_hash,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                    + " CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)";

    private static final String INSERT_REPLAY_CASE =
            "insert into qdr_replay_case"
                    + " (id, tenant_id, case_id, source_decision_id, source_request_id,"
                    + " trace_id, request_id, policy_version, model_gateway_version_ref,"
                    + " input_ref_id, expected_summary_id, expected_summary_hash, case_checksum,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "select " + REPLAY_COLUMNS + REPLAY_JOIN + " where rc.tenant_id = ? and rc.id = ?";

    private static final String SELECT_BY_CASE_ID =
            "select " + REPLAY_COLUMNS + REPLAY_JOIN + " where rc.tenant_id = ? and rc.case_id = ?";

    private static final String SELECT_BY_TRACE_ID =
            "select " + REPLAY_COLUMNS + REPLAY_JOIN
                    + " where rc.tenant_id = ? and rc.trace_id = ?"
                    + " order by rc.created_at desc, rc.id asc limit ? offset ?";

    private static final String SELECT_BY_SOURCE_REQUEST =
            "select " + REPLAY_COLUMNS + REPLAY_JOIN
                    + " where rc.tenant_id = ? and rc.source_request_id = ?"
                    + " order by rc.created_at desc, rc.id asc limit ? offset ?";

    private static final String SELECT_BY_TENANT =
            "select " + REPLAY_COLUMNS + REPLAY_JOIN
                    + " where rc.tenant_id = ?"
                    + " order by rc.created_at desc, rc.id asc limit ? offset ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     * @param objectMapper JSON mapper。
     */
    public JdbcReplayCaseRepository(
            final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public ReplayCaseRecord save(final SaveReplayCaseCommand command) {
        final SaveReplayCaseCommand checked = Objects.requireNonNull(command, "command");
        return findByCaseId(checked.tenantId(), checked.caseId())
                .map(existing -> idempotentReplayCase(existing, checked))
                .orElseGet(() -> insertReplayCase(checked));
    }

    @Override
    public Optional<ReplayCaseRecord> findById(final String tenantId, final UUID id) {
        return findOne(
                "find replay case by id",
                SELECT_BY_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireUuid(id, "id"));
    }

    @Override
    public Optional<ReplayCaseRecord> findByCaseId(final String tenantId, final String caseId) {
        return findOne(
                "find replay case by caseId",
                SELECT_BY_CASE_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(caseId, "caseId"));
    }

    @Override
    public List<ReplayCaseRecord> listByTraceId(
            final String tenantId, final String traceId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findMany(
                "list replay case by traceId",
                SELECT_BY_TRACE_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(traceId, "traceId"),
                page.limit(),
                page.offset());
    }

    @Override
    public List<ReplayCaseRecord> listBySourceRequestId(
            final String tenantId, final String sourceRequestId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findMany(
                "list replay case by sourceRequestId",
                SELECT_BY_SOURCE_REQUEST,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(sourceRequestId, "sourceRequestId"),
                page.limit(),
                page.offset());
    }

    @Override
    public List<ReplayCaseRecord> listByTenant(
            final String tenantId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findMany(
                "list replay case by tenant",
                SELECT_BY_TENANT,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                page.limit(),
                page.offset());
    }

    private ReplayCaseRecord insertReplayCase(final SaveReplayCaseCommand command) {
        final ReplayCase replayCase = command.replayCase();
        final ReplayInputRef inputRef = replayCase.inputRef();
        final ExpectedDecisionSummary summary = replayCase.expectedSummary();
        try {
            jdbcTemplate.update(
                    INSERT_INPUT_REF,
                    command.inputRefId(),
                    replayCase.tenantId(),
                    replayCase.caseId(),
                    null,
                    null,
                    replayCase.sourceDecisionId(),
                    replayCase.sourceRequestId(),
                    replayCase.traceId(),
                    command.requestId(),
                    replayCase.policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    inputRef.refType(),
                    inputRef.refId(),
                    writeJson(objectMapper, inputRef, "inputRef"),
                    inputRef.contentHash(),
                    timestamp(replayCase.createdAt()),
                    timestamp(command.updatedAt()));
            jdbcTemplate.update(
                    INSERT_EXPECTED_SUMMARY,
                    command.expectedSummaryId(),
                    replayCase.tenantId(),
                    replayCase.caseId(),
                    null,
                    null,
                    replayCase.sourceDecisionId(),
                    replayCase.sourceRequestId(),
                    replayCase.traceId(),
                    command.requestId(),
                    replayCase.policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    command.inputRefId(),
                    null,
                    DecisionSummaryRole.EXPECTED.name(),
                    summary.decisionType(),
                    summary.actionLabel(),
                    summary.confidenceBand(),
                    summary.riskLevel().name(),
                    writeJson(objectMapper, summary, "expectedSummary"),
                    writeJson(objectMapper, summary.requiredEvidenceRefs(), "expectedSummary.requiredEvidenceRefs"),
                    writeJson(objectMapper, summary.forbiddenActions(), "expectedSummary.forbiddenActions"),
                    command.expectedSummaryHash(),
                    timestamp(replayCase.createdAt()),
                    timestamp(command.updatedAt()));
            jdbcTemplate.update(
                    INSERT_REPLAY_CASE,
                    command.replayCaseId(),
                    replayCase.tenantId(),
                    replayCase.caseId(),
                    replayCase.sourceDecisionId(),
                    replayCase.sourceRequestId(),
                    replayCase.traceId(),
                    command.requestId(),
                    replayCase.policy().policyVersion(),
                    command.modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.expectedSummaryId(),
                    command.expectedSummaryHash(),
                    command.caseChecksum(),
                    timestamp(replayCase.createdAt()),
                    timestamp(command.updatedAt()));
            return toRecord(command);
        } catch (final DuplicateKeyException error) {
            return findByCaseId(command.tenantId(), command.caseId())
                    .map(existing -> idempotentReplayCase(existing, command))
                    .orElseThrow(() -> new ReplayPersistenceException("duplicate replay case rejected", error));
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException("save replay case failed", error);
        }
    }

    private ReplayCaseRecord idempotentReplayCase(
            final ReplayCaseRecord existing, final SaveReplayCaseCommand command) {
        if (!existing.caseChecksum().equals(command.caseChecksum())) {
            throw new ReplayChecksumConflictException();
        }
        return existing;
    }

    private Optional<ReplayCaseRecord> findOne(
            final String operation, final String sql, final Object... args) {
        return findMany(operation, sql, args).stream().findFirst();
    }

    private List<ReplayCaseRecord> findMany(
            final String operation, final String sql, final Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args).stream()
                    .map(this::mapRecord)
                    .toList();
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException(operation + " failed", error);
        } catch (final RuntimeException error) {
            if (error instanceof ReplayPersistenceException persistenceException) {
                throw persistenceException;
            }
            throw new ReplayPersistenceException(operation + " rejected", error);
        }
    }

    private ReplayCaseRecord mapRecord(final Map<String, Object> row) {
        return new ReplayCaseRecord(
                uuid(row, "id"),
                text(row, "tenant_id"),
                text(row, "case_id"),
                optionalText(row, "source_decision_id"),
                optionalText(row, "source_request_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                text(row, "policy_version"),
                optionalText(row, "model_gateway_version_ref"),
                uuid(row, "input_ref_id"),
                uuid(row, "expected_summary_id"),
                readInputRef(objectMapper, row.get("input_ref"), "inputRef"),
                readSummary(
                        objectMapper,
                        row.get("expected_summary_json"),
                        row.get("required_evidence_refs_json"),
                        row.get("forbidden_actions_json"),
                        "expectedSummary"),
                text(row, "expected_summary_hash"),
                text(row, "case_checksum"),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private static ReplayCaseRecord toRecord(final SaveReplayCaseCommand command) {
        final ReplayCase replayCase = command.replayCase();
        return new ReplayCaseRecord(
                command.replayCaseId(),
                replayCase.tenantId(),
                replayCase.caseId(),
                replayCase.sourceDecisionId(),
                replayCase.sourceRequestId(),
                replayCase.traceId(),
                command.requestId(),
                replayCase.policy().policyVersion(),
                command.modelGatewayVersionRef(),
                command.inputRefId(),
                command.expectedSummaryId(),
                replayCase.inputRef(),
                replayCase.expectedSummary(),
                command.expectedSummaryHash(),
                command.caseChecksum(),
                replayCase.createdAt(),
                command.updatedAt());
    }
}
