package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.enumValueOrNull;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.instant;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.optionalText;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readInputRef;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readOutputRefOrNull;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readSummary;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.readSummaryOrNull;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.text;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.timestamp;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.uuid;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.uuidOrNull;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.writeJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.DecisionSummaryRole;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPageRequest;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveEvaluationCaseCommand;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * QDR evaluation case JDBC repository。
 *
 * <p>保存 evaluation 前会验证 replay case 同租户存在；所有查询带 tenant_id。该 adapter 不执行 replay，
 * 不调用 provider、HTTP、Agent、LangGraph、NQ 或 LIVE。
 */
public final class JdbcEvaluationCaseRepository implements EvaluationCaseRepository {

    private static final String EVALUATION_COLUMNS =
            "ec.id, ec.tenant_id, ec.case_id, ec.evaluation_id, ec.verdict_id,"
                    + " ec.source_decision_id, ec.source_request_id, ec.trace_id, ec.request_id,"
                    + " ec.policy_version, ec.model_version_ref, ec.model_gateway_version_ref,"
                    + " ec.input_ref_id, ec.output_ref_id, ec.expected_summary_id,"
                    + " ec.actual_summary_id, ec.expected_summary_hash, ec.actual_summary_hash,"
                    + " ec.verdict, ec.severity, ec.finding_code, ec.finding_message,"
                    + " ec.evaluation_checksum, ec.created_at, ec.updated_at,"
                    + " ir.input_ref, orf.output_ref,"
                    + " es.summary_json as expected_summary_json,"
                    + " es.required_evidence_refs_json as expected_evidence_refs_json,"
                    + " es.forbidden_actions_json as expected_forbidden_actions_json,"
                    + " act.summary_json as actual_summary_json,"
                    + " act.required_evidence_refs_json as actual_evidence_refs_json,"
                    + " act.forbidden_actions_json as actual_forbidden_actions_json";

    private static final String EVALUATION_JOIN =
            " from qdr_evaluation_case ec"
                    + " join qdr_replay_input_ref ir"
                    + " on ir.tenant_id = ec.tenant_id and ir.id = ec.input_ref_id"
                    + " left join qdr_replay_output_ref orf"
                    + " on orf.tenant_id = ec.tenant_id and orf.id = ec.output_ref_id"
                    + " join qdr_expected_decision_summary es"
                    + " on es.tenant_id = ec.tenant_id and es.id = ec.expected_summary_id"
                    + " left join qdr_expected_decision_summary act"
                    + " on act.tenant_id = ec.tenant_id and act.id = ec.actual_summary_id";

    private static final String SELECT_REPLAY_CASE_REF =
            "select id from qdr_replay_case where tenant_id = ? and case_id = ? limit 1";

    private static final String SELECT_INPUT_REF =
            "select id from qdr_replay_input_ref where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_OUTPUT_REF =
            "select id from qdr_replay_output_ref where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_SUMMARY =
            "select id from qdr_expected_decision_summary where tenant_id = ? and id = ? limit 1";

    private static final String INSERT_INPUT_REF =
            "insert into qdr_replay_input_ref"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, ref_type, ref_id, input_ref, content_hash,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)";

    private static final String INSERT_OUTPUT_REF =
            "insert into qdr_replay_output_ref"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, ref_type, ref_id, output_ref, content_hash,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)";

    private static final String INSERT_SUMMARY =
            "insert into qdr_expected_decision_summary"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id, summary_role,"
                    + " decision_type, action_label, confidence_band, risk_level, summary_json,"
                    + " required_evidence_refs_json, forbidden_actions_json, summary_hash,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                    + " CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)";

    private static final String INSERT_EVALUATION_CASE =
            "insert into qdr_evaluation_case"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_version_ref, model_gateway_version_ref, input_ref_id, output_ref_id,"
                    + " expected_summary_id, actual_summary_id, expected_summary_hash,"
                    + " actual_summary_hash, verdict, severity, finding_code, finding_message,"
                    + " evaluation_checksum, created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "select " + EVALUATION_COLUMNS + EVALUATION_JOIN + " where ec.tenant_id = ? and ec.id = ?";

    private static final String SELECT_BY_EVALUATION_ID =
            "select " + EVALUATION_COLUMNS + EVALUATION_JOIN
                    + " where ec.tenant_id = ? and ec.evaluation_id = ?";

    private static final String SELECT_BY_CASE_ID =
            "select " + EVALUATION_COLUMNS + EVALUATION_JOIN
                    + " where ec.tenant_id = ? and ec.case_id = ?"
                    + " order by ec.created_at desc, ec.id asc limit ? offset ?";

    private static final String SELECT_BY_TENANT =
            "select " + EVALUATION_COLUMNS + EVALUATION_JOIN
                    + " where ec.tenant_id = ?"
                    + " order by ec.created_at desc, ec.id asc limit ? offset ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     * @param objectMapper JSON mapper。
     */
    public JdbcEvaluationCaseRepository(
            final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public EvaluationCaseRecord save(final SaveEvaluationCaseCommand command) {
        final SaveEvaluationCaseCommand checked = Objects.requireNonNull(command, "command");
        return findByEvaluationId(checked.tenantId(), checked.evaluationId())
                .map(existing -> idempotentEvaluationCase(existing, checked))
                .orElseGet(() -> insertEvaluationCase(checked));
    }

    @Override
    public Optional<EvaluationCaseRecord> findById(final String tenantId, final UUID id) {
        return findOne(
                "find evaluation case by id",
                SELECT_BY_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireUuid(id, "id"));
    }

    @Override
    public Optional<EvaluationCaseRecord> findByEvaluationId(
            final String tenantId, final String evaluationId) {
        return findOne(
                "find evaluation case by evaluationId",
                SELECT_BY_EVALUATION_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId"));
    }

    @Override
    public List<EvaluationCaseRecord> listByCaseId(
            final String tenantId, final String caseId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findMany(
                "list evaluation case by caseId",
                SELECT_BY_CASE_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(caseId, "caseId"),
                page.limit(),
                page.offset());
    }

    @Override
    public List<EvaluationCaseRecord> listByTenant(
            final String tenantId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findMany(
                "list evaluation case by tenant",
                SELECT_BY_TENANT,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                page.limit(),
                page.offset());
    }

    private EvaluationCaseRecord insertEvaluationCase(final SaveEvaluationCaseCommand command) {
        final EvaluationCase evaluationCase = command.evaluationCase();
        ensureTenantRefExists(
                SELECT_REPLAY_CASE_REF,
                "replay case ref missing",
                evaluationCase.tenantId(),
                evaluationCase.caseId());
        try {
            ensureInputRef(command);
            ensureOutputRef(command);
            ensureSummary(
                    command.expectedSummaryId(),
                    command,
                    evaluationCase.expectedSummary(),
                    DecisionSummaryRole.EXPECTED,
                    command.expectedSummaryHash());
            ensureActualSummary(command);
            jdbcTemplate.update(
                    INSERT_EVALUATION_CASE,
                    command.evaluationCaseId(),
                    evaluationCase.tenantId(),
                    evaluationCase.caseId(),
                    evaluationCase.evaluationId(),
                    verdictId(evaluationCase),
                    command.sourceDecisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    evaluationCase.policyVersion(),
                    evaluationCase.modelVersionRef(),
                    evaluationCase.modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.outputRefId(),
                    command.expectedSummaryId(),
                    command.actualSummaryId(),
                    command.expectedSummaryHash(),
                    command.actualSummaryHash(),
                    verdictStatus(evaluationCase),
                    severity(evaluationCase),
                    findingCode(evaluationCase),
                    findingMessage(evaluationCase),
                    command.evaluationChecksum(),
                    timestamp(command.createdAt()),
                    timestamp(command.updatedAt()));
            return toRecord(command);
        } catch (final DuplicateKeyException error) {
            return findByEvaluationId(command.tenantId(), command.evaluationId())
                    .map(existing -> idempotentEvaluationCase(existing, command))
                    .orElseThrow(() -> new ReplayPersistenceException("duplicate evaluation case rejected", error));
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException("save evaluation case failed", error);
        }
    }

    private void ensureInputRef(final SaveEvaluationCaseCommand command) {
        if (tenantRefExists(SELECT_INPUT_REF, command.tenantId(), command.inputRefId())) {
            return;
        }
        final EvaluationCase evaluationCase = command.evaluationCase();
        final ReplayInputRef inputRef = command.inputRef();
        jdbcTemplate.update(
                INSERT_INPUT_REF,
                command.inputRefId(),
                evaluationCase.tenantId(),
                evaluationCase.caseId(),
                evaluationCase.evaluationId(),
                verdictId(evaluationCase),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                evaluationCase.policyVersion(),
                evaluationCase.modelGatewayVersionRef(),
                inputRef.refType(),
                inputRef.refId(),
                writeJson(objectMapper, inputRef, "inputRef"),
                inputRef.contentHash(),
                timestamp(command.createdAt()),
                timestamp(command.updatedAt()));
    }

    private void ensureOutputRef(final SaveEvaluationCaseCommand command) {
        final EvaluationCase evaluationCase = command.evaluationCase();
        final ReplayOutputRef outputRef = evaluationCase.outputRef();
        if (command.outputRefId() == null || outputRef == null) {
            return;
        }
        if (tenantRefExists(SELECT_OUTPUT_REF, command.tenantId(), command.outputRefId())) {
            return;
        }
        jdbcTemplate.update(
                INSERT_OUTPUT_REF,
                command.outputRefId(),
                evaluationCase.tenantId(),
                evaluationCase.caseId(),
                evaluationCase.evaluationId(),
                verdictId(evaluationCase),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                evaluationCase.policyVersion(),
                evaluationCase.modelGatewayVersionRef(),
                outputRef.refType(),
                outputRef.refId(),
                writeJson(objectMapper, outputRef, "outputRef"),
                outputRef.contentHash(),
                timestamp(command.createdAt()),
                timestamp(command.updatedAt()));
    }

    private void ensureActualSummary(final SaveEvaluationCaseCommand command) {
        if (command.actualSummaryId() == null || command.evaluationCase().actualSummary() == null) {
            return;
        }
        ensureSummary(
                command.actualSummaryId(),
                command,
                command.evaluationCase().actualSummary(),
                DecisionSummaryRole.ACTUAL,
                command.actualSummaryHash());
    }

    private void ensureSummary(
            final UUID summaryId,
            final SaveEvaluationCaseCommand command,
            final ExpectedDecisionSummary summary,
            final DecisionSummaryRole role,
            final String summaryHash) {
        if (tenantRefExists(SELECT_SUMMARY, command.tenantId(), summaryId)) {
            return;
        }
        final EvaluationCase evaluationCase = command.evaluationCase();
        jdbcTemplate.update(
                INSERT_SUMMARY,
                summaryId,
                evaluationCase.tenantId(),
                evaluationCase.caseId(),
                evaluationCase.evaluationId(),
                verdictId(evaluationCase),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                evaluationCase.policyVersion(),
                evaluationCase.modelGatewayVersionRef(),
                command.inputRefId(),
                role == DecisionSummaryRole.ACTUAL ? command.outputRefId() : null,
                role.name(),
                summary.decisionType(),
                summary.actionLabel(),
                summary.confidenceBand(),
                summary.riskLevel().name(),
                writeJson(objectMapper, summary, role.name().toLowerCase() + "Summary"),
                writeJson(objectMapper, summary.requiredEvidenceRefs(), "summary.requiredEvidenceRefs"),
                writeJson(objectMapper, summary.forbiddenActions(), "summary.forbiddenActions"),
                summaryHash,
                timestamp(command.createdAt()),
                timestamp(command.updatedAt()));
    }

    private EvaluationCaseRecord idempotentEvaluationCase(
            final EvaluationCaseRecord existing, final SaveEvaluationCaseCommand command) {
        if (!existing.evaluationChecksum().equals(command.evaluationChecksum())) {
            throw new ReplayChecksumConflictException();
        }
        return existing;
    }

    private Optional<EvaluationCaseRecord> findOne(
            final String operation, final String sql, final Object... args) {
        return findMany(operation, sql, args).stream().findFirst();
    }

    private List<EvaluationCaseRecord> findMany(
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

    private void ensureTenantRefExists(
            final String sql, final String failureMessage, final Object... args) {
        if (!tenantRefExists(sql, args)) {
            throw new ReplayPersistenceException(failureMessage);
        }
    }

    private boolean tenantRefExists(final String sql, final Object... args) {
        try {
            return !jdbcTemplate.queryForList(sql, args).isEmpty();
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException("tenant-bound ref check failed", error);
        }
    }

    private EvaluationCaseRecord mapRecord(final Map<String, Object> row) {
        return new EvaluationCaseRecord(
                uuid(row, "id"),
                text(row, "tenant_id"),
                text(row, "case_id"),
                text(row, "evaluation_id"),
                optionalText(row, "verdict_id"),
                optionalText(row, "source_decision_id"),
                optionalText(row, "source_request_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                text(row, "policy_version"),
                optionalText(row, "model_version_ref"),
                optionalText(row, "model_gateway_version_ref"),
                uuid(row, "input_ref_id"),
                uuidOrNull(row, "output_ref_id"),
                uuid(row, "expected_summary_id"),
                uuidOrNull(row, "actual_summary_id"),
                readInputRef(objectMapper, row.get("input_ref"), "inputRef"),
                readOutputRefOrNull(objectMapper, row.get("output_ref"), "outputRef"),
                readSummary(
                        objectMapper,
                        row.get("expected_summary_json"),
                        row.get("expected_evidence_refs_json"),
                        row.get("expected_forbidden_actions_json"),
                        "expectedSummary"),
                readSummaryOrNull(
                        objectMapper,
                        row.get("actual_summary_json"),
                        row.get("actual_evidence_refs_json"),
                        row.get("actual_forbidden_actions_json"),
                        "actualSummary"),
                text(row, "expected_summary_hash"),
                optionalText(row, "actual_summary_hash"),
                enumValueOrNull(row, "verdict", RegressionVerdict.Status.class),
                enumValueOrNull(row, "severity", RegressionSeverity.class),
                optionalText(row, "finding_code"),
                optionalText(row, "finding_message"),
                text(row, "evaluation_checksum"),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private static EvaluationCaseRecord toRecord(final SaveEvaluationCaseCommand command) {
        final EvaluationCase evaluationCase = command.evaluationCase();
        return new EvaluationCaseRecord(
                command.evaluationCaseId(),
                evaluationCase.tenantId(),
                evaluationCase.caseId(),
                evaluationCase.evaluationId(),
                verdictId(evaluationCase),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                evaluationCase.policyVersion(),
                evaluationCase.modelVersionRef(),
                evaluationCase.modelGatewayVersionRef(),
                command.inputRefId(),
                command.outputRefId(),
                command.expectedSummaryId(),
                command.actualSummaryId(),
                command.inputRef(),
                evaluationCase.outputRef(),
                evaluationCase.expectedSummary(),
                evaluationCase.actualSummary(),
                command.expectedSummaryHash(),
                command.actualSummaryHash(),
                evaluationCase.verdict() == null ? null : evaluationCase.verdict().status(),
                severity(evaluationCase),
                findingCode(evaluationCase),
                findingMessage(evaluationCase),
                command.evaluationChecksum(),
                command.createdAt(),
                command.updatedAt());
    }

    private static String verdictId(final EvaluationCase evaluationCase) {
        return evaluationCase.verdict() == null ? null : evaluationCase.evaluationId() + "-verdict";
    }

    private static String verdictStatus(final EvaluationCase evaluationCase) {
        return evaluationCase.verdict() == null ? null : evaluationCase.verdict().status().name();
    }

    private static RegressionSeverity severity(final EvaluationCase evaluationCase) {
        final RegressionVerdict verdict = evaluationCase.verdict();
        if (verdict == null || verdict.findings().isEmpty()) {
            return null;
        }
        return verdict.findings().stream()
                .map(RegressionFinding::severity)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(null);
    }

    private static String findingCode(final EvaluationCase evaluationCase) {
        return firstFinding(evaluationCase).map(RegressionFinding::code).orElse(null);
    }

    private static String findingMessage(final EvaluationCase evaluationCase) {
        return firstFinding(evaluationCase).map(RegressionFinding::message).orElse(null);
    }

    private static Optional<RegressionFinding> firstFinding(final EvaluationCase evaluationCase) {
        final RegressionVerdict verdict = evaluationCase.verdict();
        if (verdict == null) {
            return Optional.empty();
        }
        return verdict.findings().stream().findFirst();
    }
}
