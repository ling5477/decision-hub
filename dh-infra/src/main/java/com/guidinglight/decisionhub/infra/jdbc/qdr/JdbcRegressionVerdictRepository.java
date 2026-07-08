package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.enumValue;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.instant;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.optionalText;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.text;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.timestamp;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.uuid;
import static com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayPersistenceSupport.uuidOrNull;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionFindingRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPageRequest;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionFindingCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionVerdictCommand;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * QDR regression verdict JDBC repository。
 *
 * <p>verdict/finding 只保存 replay/evaluation regression 结果，所有查询和引用检查都绑定 tenant_id。
 * 本 adapter 不生成交易建议、不执行 NQ mutation、不调用 provider 或 HTTP。
 */
public final class JdbcRegressionVerdictRepository implements RegressionVerdictRepository {

    private static final String VERDICT_COLUMNS =
            "id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id,"
                    + " expected_summary_id, actual_summary_id, expected_summary_hash,"
                    + " actual_summary_hash, verdict, severity, finding_code, finding_message,"
                    + " failure_reason, created_at, updated_at";

    private static final String FINDING_COLUMNS =
            "id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id,"
                    + " expected_summary_id, actual_summary_id, expected_summary_hash,"
                    + " actual_summary_hash, verdict, severity, finding_code, finding_message,"
                    + " evidence_ref, created_at, updated_at";

    private static final String SELECT_EVALUATION_REF =
            "select id from qdr_evaluation_case where tenant_id = ? and evaluation_id = ? limit 1";

    private static final String SELECT_INPUT_REF =
            "select id from qdr_replay_input_ref where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_OUTPUT_REF =
            "select id from qdr_replay_output_ref where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_SUMMARY =
            "select id from qdr_expected_decision_summary where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_VERDICT_REF =
            "select id from qdr_regression_verdict where tenant_id = ? and verdict_id = ? limit 1";

    private static final String INSERT_VERDICT =
            "insert into qdr_regression_verdict"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id,"
                    + " expected_summary_id, actual_summary_id, expected_summary_hash,"
                    + " actual_summary_hash, verdict, severity, finding_code, finding_message,"
                    + " failure_reason, created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_FINDING =
            "insert into qdr_regression_finding"
                    + " (id, tenant_id, case_id, evaluation_id, verdict_id, source_decision_id,"
                    + " source_request_id, trace_id, request_id, policy_version,"
                    + " model_gateway_version_ref, input_ref_id, output_ref_id,"
                    + " expected_summary_id, actual_summary_id, expected_summary_hash,"
                    + " actual_summary_hash, verdict, severity, finding_code, finding_message,"
                    + " evidence_ref, created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "select " + VERDICT_COLUMNS + " from qdr_regression_verdict"
                    + " where tenant_id = ? and id = ?";

    private static final String SELECT_BY_VERDICT_ID =
            "select " + VERDICT_COLUMNS + " from qdr_regression_verdict"
                    + " where tenant_id = ? and verdict_id = ?";

    private static final String SELECT_BY_EVALUATION_ID =
            "select " + VERDICT_COLUMNS + " from qdr_regression_verdict"
                    + " where tenant_id = ? and evaluation_id = ?";

    private static final String SELECT_BY_TENANT =
            "select " + VERDICT_COLUMNS + " from qdr_regression_verdict"
                    + " where tenant_id = ? order by created_at desc, id asc limit ? offset ?";

    private static final String SELECT_FINDINGS_BY_VERDICT_ID =
            "select " + FINDING_COLUMNS + " from qdr_regression_finding"
                    + " where tenant_id = ? and verdict_id = ?"
                    + " order by created_at asc, id asc limit ? offset ?";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     */
    public JdbcRegressionVerdictRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public RegressionVerdictRecord save(final SaveRegressionVerdictCommand command) {
        final SaveRegressionVerdictCommand checked = Objects.requireNonNull(command, "command");
        return findByVerdictId(checked.tenantId(), checked.verdictId())
                .map(existing -> idempotentVerdict(existing, checked))
                .orElseGet(() -> insertVerdict(checked));
    }

    @Override
    public List<RegressionFindingRecord> saveFindings(
            final String tenantId,
            final String verdictId,
            final List<SaveRegressionFindingCommand> commands) {
        final String checkedTenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        final String checkedVerdictId = ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId");
        ensureTenantRefExists(SELECT_VERDICT_REF, "verdict ref missing", checkedTenantId, checkedVerdictId);
        final List<SaveRegressionFindingCommand> checkedCommands =
                List.copyOf(Objects.requireNonNull(commands, "commands"));
        try {
            for (SaveRegressionFindingCommand command : checkedCommands) {
                saveFinding(checkedTenantId, checkedVerdictId, command);
            }
            return checkedCommands.stream()
                    .map(this::toFindingRecord)
                    .toList();
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException("save regression findings failed", error);
        }
    }

    @Override
    public Optional<RegressionVerdictRecord> findById(final String tenantId, final UUID id) {
        return findOne(
                "find regression verdict by id",
                SELECT_BY_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireUuid(id, "id"));
    }

    @Override
    public Optional<RegressionVerdictRecord> findByVerdictId(
            final String tenantId, final String verdictId) {
        return findOne(
                "find regression verdict by verdictId",
                SELECT_BY_VERDICT_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId"));
    }

    @Override
    public Optional<RegressionVerdictRecord> findByEvaluationId(
            final String tenantId, final String evaluationId) {
        return findOne(
                "find regression verdict by evaluationId",
                SELECT_BY_EVALUATION_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId"));
    }

    @Override
    public List<RegressionFindingRecord> listFindingsByVerdictId(
            final String tenantId, final String verdictId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findManyFindings(
                "list regression findings by verdictId",
                SELECT_FINDINGS_BY_VERDICT_ID,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId"),
                page.limit(),
                page.offset());
    }

    @Override
    public List<RegressionVerdictRecord> listByTenant(
            final String tenantId, final int limit, final int offset) {
        final ReplayPageRequest page = new ReplayPageRequest(limit, offset);
        return findManyVerdicts(
                "list regression verdict by tenant",
                SELECT_BY_TENANT,
                ReplayPersistenceGuard.requireTenantId(tenantId),
                page.limit(),
                page.offset());
    }

    private RegressionVerdictRecord insertVerdict(final SaveRegressionVerdictCommand command) {
        ensureTenantRefExists(
                SELECT_EVALUATION_REF,
                "evaluation ref missing",
                command.tenantId(),
                command.evaluationId());
        ensureTenantRefExists(SELECT_INPUT_REF, "input ref missing", command.tenantId(), command.inputRefId());
        ensureTenantRefExists(
                SELECT_SUMMARY, "expected summary ref missing", command.tenantId(), command.expectedSummaryId());
        ensureOptionalRef(SELECT_OUTPUT_REF, command.tenantId(), command.outputRefId(), "output ref missing");
        ensureOptionalRef(SELECT_SUMMARY, command.tenantId(), command.actualSummaryId(), "actual summary ref missing");
        try {
            jdbcTemplate.update(
                    INSERT_VERDICT,
                    command.verdictRecordId(),
                    command.tenantId(),
                    command.caseId(),
                    command.evaluationId(),
                    command.verdictId(),
                    command.sourceDecisionId(),
                    command.sourceRequestId(),
                    command.traceId(),
                    command.requestId(),
                    command.policyVersion(),
                    command.modelGatewayVersionRef(),
                    command.inputRefId(),
                    command.outputRefId(),
                    command.expectedSummaryId(),
                    command.actualSummaryId(),
                    command.expectedSummaryHash(),
                    command.actualSummaryHash(),
                    command.verdict().status().name(),
                    command.severity().name(),
                    command.findingCode(),
                    command.findingMessage(),
                    command.verdict().failureReason(),
                    timestamp(command.createdAt()),
                    timestamp(command.updatedAt()));
            return toVerdictRecord(command);
        } catch (final DuplicateKeyException error) {
            return findByVerdictId(command.tenantId(), command.verdictId())
                    .map(existing -> idempotentVerdict(existing, command))
                    .orElseThrow(() -> new ReplayPersistenceException("duplicate regression verdict rejected", error));
        } catch (final DataAccessException error) {
            throw new ReplayPersistenceException("save regression verdict failed", error);
        }
    }

    private void saveFinding(
            final String tenantId, final String verdictId, final SaveRegressionFindingCommand command) {
        if (!command.tenantId().equals(tenantId) || !command.verdictId().equals(verdictId)) {
            throw new ReplayPersistenceException("finding tenant or verdict mismatch");
        }
        jdbcTemplate.update(
                INSERT_FINDING,
                command.findingId(),
                command.tenantId(),
                command.caseId(),
                command.evaluationId(),
                command.verdictId(),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                command.policyVersion(),
                command.modelGatewayVersionRef(),
                command.inputRefId(),
                command.outputRefId(),
                command.expectedSummaryId(),
                command.actualSummaryId(),
                command.expectedSummaryHash(),
                command.actualSummaryHash(),
                command.verdict().name(),
                command.severity().name(),
                command.findingCode(),
                command.findingMessage(),
                command.evidenceRef(),
                timestamp(command.createdAt()),
                timestamp(command.updatedAt()));
    }

    private RegressionVerdictRecord idempotentVerdict(
            final RegressionVerdictRecord existing, final SaveRegressionVerdictCommand command) {
        if (existing.verdict() != command.verdict().status()
                || existing.severity() != command.severity()
                || !existing.evaluationId().equals(command.evaluationId())) {
            throw new ReplayPersistenceException("regression verdict duplicate mismatch");
        }
        return existing;
    }

    private Optional<RegressionVerdictRecord> findOne(
            final String operation, final String sql, final Object... args) {
        return findManyVerdicts(operation, sql, args).stream().findFirst();
    }

    private List<RegressionVerdictRecord> findManyVerdicts(
            final String operation, final String sql, final Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args).stream()
                    .map(this::mapVerdict)
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

    private List<RegressionFindingRecord> findManyFindings(
            final String operation, final String sql, final Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args).stream()
                    .map(this::mapFinding)
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

    private void ensureOptionalRef(
            final String sql, final String tenantId, final UUID id, final String failureMessage) {
        if (id != null && !tenantRefExists(sql, tenantId, id)) {
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

    private RegressionVerdictRecord mapVerdict(final Map<String, Object> row) {
        return new RegressionVerdictRecord(
                uuid(row, "id"),
                text(row, "tenant_id"),
                text(row, "case_id"),
                text(row, "evaluation_id"),
                text(row, "verdict_id"),
                optionalText(row, "source_decision_id"),
                optionalText(row, "source_request_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                text(row, "policy_version"),
                optionalText(row, "model_gateway_version_ref"),
                uuid(row, "input_ref_id"),
                uuidOrNull(row, "output_ref_id"),
                uuid(row, "expected_summary_id"),
                uuidOrNull(row, "actual_summary_id"),
                text(row, "expected_summary_hash"),
                optionalText(row, "actual_summary_hash"),
                enumValue(row, "verdict", RegressionVerdict.Status.class),
                enumValue(row, "severity", RegressionSeverity.class),
                optionalText(row, "finding_code"),
                optionalText(row, "finding_message"),
                optionalText(row, "failure_reason"),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private RegressionFindingRecord mapFinding(final Map<String, Object> row) {
        return new RegressionFindingRecord(
                uuid(row, "id"),
                text(row, "tenant_id"),
                text(row, "case_id"),
                text(row, "evaluation_id"),
                text(row, "verdict_id"),
                optionalText(row, "source_decision_id"),
                optionalText(row, "source_request_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                text(row, "policy_version"),
                optionalText(row, "model_gateway_version_ref"),
                uuidOrNull(row, "input_ref_id"),
                uuidOrNull(row, "output_ref_id"),
                uuidOrNull(row, "expected_summary_id"),
                uuidOrNull(row, "actual_summary_id"),
                optionalText(row, "expected_summary_hash"),
                optionalText(row, "actual_summary_hash"),
                enumValue(row, "verdict", RegressionVerdict.Status.class),
                enumValue(row, "severity", RegressionSeverity.class),
                text(row, "finding_code"),
                text(row, "finding_message"),
                optionalText(row, "evidence_ref"),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private RegressionVerdictRecord toVerdictRecord(final SaveRegressionVerdictCommand command) {
        return new RegressionVerdictRecord(
                command.verdictRecordId(),
                command.tenantId(),
                command.caseId(),
                command.evaluationId(),
                command.verdictId(),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                command.policyVersion(),
                command.modelGatewayVersionRef(),
                command.inputRefId(),
                command.outputRefId(),
                command.expectedSummaryId(),
                command.actualSummaryId(),
                command.expectedSummaryHash(),
                command.actualSummaryHash(),
                command.verdict().status(),
                command.severity(),
                command.findingCode(),
                command.findingMessage(),
                command.verdict().failureReason(),
                command.createdAt(),
                command.updatedAt());
    }

    private RegressionFindingRecord toFindingRecord(final SaveRegressionFindingCommand command) {
        return new RegressionFindingRecord(
                command.findingId(),
                command.tenantId(),
                command.caseId(),
                command.evaluationId(),
                command.verdictId(),
                command.sourceDecisionId(),
                command.sourceRequestId(),
                command.traceId(),
                command.requestId(),
                command.policyVersion(),
                command.modelGatewayVersionRef(),
                command.inputRefId(),
                command.outputRefId(),
                command.expectedSummaryId(),
                command.actualSummaryId(),
                command.expectedSummaryHash(),
                command.actualSummaryHash(),
                command.verdict(),
                command.severity(),
                command.findingCode(),
                command.findingMessage(),
                command.evidenceRef(),
                command.createdAt(),
                command.updatedAt());
    }
}
