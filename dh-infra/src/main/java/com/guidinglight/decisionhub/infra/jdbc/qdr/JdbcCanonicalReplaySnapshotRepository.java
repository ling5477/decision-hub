package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotConflictException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotRecord;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotVersionVector;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotWriteCommand;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V10 canonical replay snapshot 的 tenant-bound JDBC adapter。
 *
 * <p>本 adapter 只访问 DH 本地 V5/V6/V8/V9/V10 表。写入前和读取后都会逐项验证 physical UUID、 business ID、safe ref 与
 * version vector，tenant 始终是第一 SQL predicate。它不生成默认版本、当前业务 时间、canonical bytes 或 hash，也不调用
 * HTTP、Provider、Agent、NQ 或交易链路。
 *
 * <p>线程安全性由不可变依赖 {@link JdbcTemplate} 与 {@link ObjectMapper} 的标准线程安全用法保证；事务由 调用方已有 Spring
 * transaction boundary 控制，本类不会隐式开启跨资源事务。
 */
public final class JdbcCanonicalReplaySnapshotRepository
    implements CanonicalReplaySnapshotPersistencePort {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE =
      new TypeReference<>() {};

  private static final String INSERT_COLUMNS =
      "id, tenant_id, snapshot_id, decision_id, decision_request_id, decision_run_id,"
          + " trace_id, request_id, source, decision_type, source_captured_at,"
          + " model_call_id, model_call_ref, prompt_version_id, model_version_id,"
          + " replay_case_row_id, replay_case_id, evaluation_case_row_id,"
          + " evaluation_case_id, regression_verdict_row_id, regression_verdict_id,"
          + " subject_json, context_payload_json, evidence_refs_json,"
          + " replay_input_ref_json, expected_decision_summary_json,"
          + " snapshot_schema_version, decision_schema_version, context_schema_version,"
          + " policy_version, evaluation_policy_version, prompt_version_ref,"
          + " prompt_version_checksum, model_version_ref, model_version_checksum,"
          + " model_gateway_version_ref, canonicalization_version,"
          + " replay_executor_version, hash_algorithm_version, replay_input_hash,"
          + " expected_summary_hash, provider_summary_hash, canonical_input_hash,"
          + " payload_bytes";

  private static final String SELECT_COLUMNS = INSERT_COLUMNS + ", created_at";

  private static final String INSERT =
      "insert into qdr_canonical_replay_snapshot ("
          + INSERT_COLUMNS
          + ") values ("
          + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
          + " ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb,"
          + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SELECT_BY_SNAPSHOT =
      "select "
          + SELECT_COLUMNS
          + " from qdr_canonical_replay_snapshot"
          + " where tenant_id = ? and snapshot_id = ?";

  private static final String SELECT_BY_IDENTITY =
      "select "
          + SELECT_COLUMNS
          + " from qdr_canonical_replay_snapshot"
          + " where tenant_id = ? and snapshot_id = ? and decision_id = ?"
          + " and decision_request_id = ? and decision_run_id = ?"
          + " and model_call_id = ? and model_call_ref = ?"
          + " and prompt_version_id = ? and model_version_id = ?"
          + " and replay_case_row_id = ? and replay_case_id = ?"
          + " and evaluation_case_row_id is not distinct from ?"
          + " and evaluation_case_id is not distinct from ?"
          + " and regression_verdict_row_id is not distinct from ?"
          + " and regression_verdict_id is not distinct from ?"
          + " and trace_id = ? and request_id = ? and snapshot_schema_version = ?";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 创建可由 repository test 直接实例化的 JDBC adapter；不修改 Spring runtime wiring。
   *
   * @param jdbcTemplate DH datasource JDBC template。
   * @param objectMapper 仅用于 allowlisted structured JSON 的安全序列化/反序列化。
   */
  public JdbcCanonicalReplaySnapshotRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public CanonicalReplaySnapshotRecord insert(
      final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
    final String checkedTenant = requireTenantMatch(tenantId, command);
    validateSourceIdentity(checkedTenant, command);
    final Optional<CanonicalReplaySnapshotRecord> existing = findExistingIdentity(command);
    if (existing.isPresent()) {
      return identicalOrConflict(existing.get(), command);
    }
    try {
      jdbcTemplate.update(INSERT, insertArguments(command));
      return findByTenantAndIdentity(
              checkedTenant, command.identity(), command.versionVector().snapshotSchemaVersion())
          .map(found -> identicalOrConflict(found, command))
          .orElseThrow(
              () ->
                  new CanonicalReplaySnapshotPersistenceException(
                      "inserted canonical replay snapshot was not readable by exact identity"));
    } catch (final DuplicateKeyException error) {
      return findExistingIdentity(command)
          .map(found -> identicalOrConflict(found, command))
          .orElseThrow(() -> new CanonicalReplaySnapshotConflictException());
    } catch (final DataAccessException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "insert canonical replay snapshot failed", error);
    } catch (final RuntimeException error) {
      throw normalize("insert canonical replay snapshot rejected", error);
    }
  }

  @Override
  public Optional<CanonicalReplaySnapshotRecord> findByTenantAndSnapshotId(
      final String tenantId, final String snapshotId) {
    final String checkedTenant = ReplayPersistenceGuard.requireTenantId(tenantId);
    final String checkedSnapshot = ReplayPersistenceGuard.requireSafeText(snapshotId, "snapshotId");
    try {
      final Optional<CanonicalReplaySnapshotRecord> found =
          single(
              jdbcTemplate.queryForList(SELECT_BY_SNAPSHOT, checkedTenant, checkedSnapshot),
              "tenant + snapshotId");
      found.ifPresent(
          record -> {
            if (!checkedTenant.equals(record.identity().tenantId())
                || !checkedSnapshot.equals(record.identity().snapshotId())) {
              throw new CanonicalReplaySnapshotPersistenceException(
                  "snapshot result identity mismatch");
            }
            validateSourceIdentity(checkedTenant, record.toWriteCommand());
          });
      return found;
    } catch (final DataAccessException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "find canonical replay snapshot failed", error);
    } catch (final RuntimeException error) {
      throw normalize("find canonical replay snapshot rejected", error);
    }
  }

  @Override
  public Optional<CanonicalReplaySnapshotRecord> findByTenantAndIdentity(
      final String tenantId,
      final CanonicalReplaySnapshotIdentity identity,
      final String snapshotSchemaVersion) {
    final String checkedTenant = ReplayPersistenceGuard.requireTenantId(tenantId);
    final CanonicalReplaySnapshotIdentity checkedIdentity =
        Objects.requireNonNull(identity, "identity");
    if (!checkedTenant.equals(checkedIdentity.tenantId())) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "tenantId must match canonical snapshot identity");
    }
    final String checkedVersion =
        ReplayPersistenceGuard.requireSafeText(snapshotSchemaVersion, "snapshotSchemaVersion");
    try {
      final Optional<CanonicalReplaySnapshotRecord> found =
          single(
              jdbcTemplate.queryForList(
                  SELECT_BY_IDENTITY,
                  checkedTenant,
                  checkedIdentity.snapshotId(),
                  checkedIdentity.correlation().decisionId(),
                  checkedIdentity.decisionRequestId(),
                  checkedIdentity.decisionRunId(),
                  checkedIdentity.modelCallId(),
                  checkedIdentity.modelCallRef(),
                  checkedIdentity.promptVersionId(),
                  checkedIdentity.modelVersionId(),
                  checkedIdentity.replayCaseRowId(),
                  checkedIdentity.replayCaseId(),
                  checkedIdentity.evaluationCaseRowId(),
                  checkedIdentity.evaluationCaseId(),
                  checkedIdentity.regressionVerdictRowId(),
                  checkedIdentity.regressionVerdictId(),
                  checkedIdentity.correlation().traceId(),
                  checkedIdentity.correlation().requestId(),
                  checkedVersion),
              "composite identity");
      found.ifPresent(
          record -> {
            if (!checkedIdentity.equals(record.identity())
                || !checkedVersion.equals(record.versionVector().snapshotSchemaVersion())) {
              throw new CanonicalReplaySnapshotPersistenceException(
                  "composite snapshot result identity mismatch");
            }
            validateSourceIdentity(checkedTenant, record.toWriteCommand());
          });
      return found;
    } catch (final DataAccessException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "find canonical replay snapshot by identity failed", error);
    } catch (final RuntimeException error) {
      throw normalize("find canonical replay snapshot by identity rejected", error);
    }
  }

  private Optional<CanonicalReplaySnapshotRecord> findExistingIdentity(
      final CanonicalReplaySnapshotWriteCommand command) {
    final Optional<CanonicalReplaySnapshotRecord> bySnapshot =
        findByTenantAndSnapshotId(command.identity().tenantId(), command.identity().snapshotId());
    if (bySnapshot.isPresent()) {
      return bySnapshot;
    }
    return findByTenantAndIdentity(
        command.identity().tenantId(),
        command.identity(),
        command.versionVector().snapshotSchemaVersion());
  }

  private Optional<CanonicalReplaySnapshotRecord> single(
      final List<Map<String, Object>> rows, final String lookup) {
    if (rows.size() > 1) {
      throw new CanonicalReplaySnapshotPersistenceException(
          lookup + " returned multiple canonical snapshot rows");
    }
    return rows.stream().findFirst().map(this::mapRecord);
  }

  private static String requireTenantMatch(
      final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
    final String checkedTenant = ReplayPersistenceGuard.requireTenantId(tenantId);
    final CanonicalReplaySnapshotWriteCommand checked = Objects.requireNonNull(command, "command");
    if (!checkedTenant.equals(checked.identity().tenantId())) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "tenantId must match canonical snapshot record");
    }
    return checkedTenant;
  }

  private CanonicalReplaySnapshotRecord identicalOrConflict(
      final CanonicalReplaySnapshotRecord existing,
      final CanonicalReplaySnapshotWriteCommand requested) {
    // DB-generated created_at 不属于 caller material；其余实际持久化列必须 exact match。
    if (!Arrays.deepEquals(insertArguments(existing.toWriteCommand()), insertArguments(requested))) {
      throw new CanonicalReplaySnapshotConflictException();
    }
    return existing;
  }

  private static CanonicalReplaySnapshotPersistenceException normalize(
      final String message, final RuntimeException error) {
    if (error instanceof CanonicalReplaySnapshotPersistenceException persistenceException) {
      return persistenceException;
    }
    return new CanonicalReplaySnapshotPersistenceException(message, error);
  }

  private Object[] insertArguments(final CanonicalReplaySnapshotWriteCommand command) {
    final CanonicalReplaySnapshotIdentity identity = command.identity();
    final CanonicalReplaySnapshotVersionVector versions = command.versionVector();
    return new Object[] {
      command.id(),
      identity.tenantId(),
      identity.snapshotId(),
      identity.correlation().decisionId(),
      identity.decisionRequestId(),
      identity.decisionRunId(),
      identity.correlation().traceId(),
      identity.correlation().requestId(),
      command.source(),
      command.decisionType(),
      Timestamp.from(command.sourceCapturedAt()),
      identity.modelCallId(),
      identity.modelCallRef(),
      identity.promptVersionId(),
      identity.modelVersionId(),
      identity.replayCaseRowId(),
      identity.replayCaseId(),
      identity.evaluationCaseRowId(),
      identity.evaluationCaseId(),
      identity.regressionVerdictRowId(),
      identity.regressionVerdictId(),
      writeJson(subjectPayload(command.subject()), "subject"),
      writeJson(contextPayload(command.contextSnapshot()), "contextSnapshot"),
      writeJson(evidencePayload(command.evidenceRefs()), "evidenceRefs"),
      writeJson(command.replayInputRef(), "replayInputRef"),
      writeJson(summaryPayload(command.expectedDecisionSummary()), "expectedDecisionSummary"),
      versions.snapshotSchemaVersion(),
      versions.decisionSchemaVersion(),
      versions.contextSchemaVersion(),
      versions.policyVersion(),
      versions.evaluationPolicyVersion(),
      versions.promptVersionRef(),
      versions.promptVersionChecksum(),
      versions.modelVersionRef(),
      versions.modelVersionChecksum(),
      versions.modelGatewayVersionRef(),
      versions.canonicalizationVersion(),
      versions.replayExecutorVersion(),
      versions.hashAlgorithmVersion(),
      command.replayInputHash(),
      command.expectedSummaryHash(),
      command.providerSummaryHash(),
      command.canonicalInputHash(),
      command.payloadBytes()
    };
  }

  private CanonicalReplaySnapshotRecord mapRecord(final Map<String, Object> row) {
    final DecisionEvidenceCorrelation correlation =
        new DecisionEvidenceCorrelation(
            text(row, "tenant_id"),
            text(row, "trace_id"),
            text(row, "request_id"),
            text(row, "decision_id"));
    final CanonicalReplaySnapshotIdentity identity =
        new CanonicalReplaySnapshotIdentity(
            correlation,
            text(row, "snapshot_id"),
            uuid(row, "decision_request_id"),
            uuid(row, "decision_run_id"),
            uuid(row, "model_call_id"),
            text(row, "model_call_ref"),
            uuid(row, "prompt_version_id"),
            uuid(row, "model_version_id"),
            uuid(row, "replay_case_row_id"),
            text(row, "replay_case_id"),
            uuidOrNull(row, "evaluation_case_row_id"),
            optionalText(row, "evaluation_case_id"),
            uuidOrNull(row, "regression_verdict_row_id"),
            optionalText(row, "regression_verdict_id"));
    final Map<String, Object> context = readMap(row.get("context_payload_json"), "contextSnapshot");
    return new CanonicalReplaySnapshotRecord(
        uuid(row, "id"),
        identity,
        text(row, "source"),
        text(row, "decision_type"),
        instant(row, "source_captured_at"),
        readSubject(row.get("subject_json")),
        new DecisionContextSnapshot(
            string(context, "snapshotId"),
            Instant.parse(string(context, "capturedAt")),
            stringList(context.get("evidenceRefs"), "contextSnapshot.evidenceRefs")),
        readEvidenceRefs(row.get("evidence_refs_json"), correlation),
        readInputRef(row.get("replay_input_ref_json")),
        readSummary(row.get("expected_decision_summary_json")),
        new CanonicalReplaySnapshotVersionVector(
            text(row, "snapshot_schema_version"),
            text(row, "decision_schema_version"),
            text(row, "context_schema_version"),
            text(row, "policy_version"),
            text(row, "evaluation_policy_version"),
            text(row, "prompt_version_ref"),
            text(row, "prompt_version_checksum"),
            text(row, "model_version_ref"),
            text(row, "model_version_checksum"),
            text(row, "model_gateway_version_ref"),
            text(row, "canonicalization_version"),
            text(row, "replay_executor_version"),
            text(row, "hash_algorithm_version")),
        text(row, "replay_input_hash"),
        text(row, "expected_summary_hash"),
        optionalText(row, "provider_summary_hash"),
        text(row, "canonical_input_hash"),
        integer(row, "payload_bytes"),
        instant(row, "created_at"));
  }

  /**
   * 对 V5/V6/V8/V9 source rows 执行 exact identity/version 验证。
   *
   * <p>每条 SQL 都以 tenant 开头并携带 physical/business identity；任何缺失、冲突或多行都拒绝， 不使用时间、latest、provider
   * 名称或字符串相似性推断关联。
   */
  private void validateSourceIdentity(
      final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
    final CanonicalReplaySnapshotIdentity id = command.identity();
    final DecisionEvidenceCorrelation correlation = id.correlation();
    final CanonicalReplaySnapshotVersionVector versions = command.versionVector();

    requireSourceRow(
        "select decision_id from dh_decision_request"
            + " where tenant_id=? and decision_id=? and trace_id=? and request_id=?"
            + " and source=? and schema_version=?",
        "V5 decision",
        tenantId,
        correlation.decisionId(),
        correlation.traceId(),
        correlation.requestId(),
        command.source(),
        versions.decisionSchemaVersion());
    requireSourceRow(
        "select dr.id from decision_request dr join decision_run run"
            + " on run.decision_request_id=dr.id"
            + " where dr.tenant_id=? and dr.id=? and run.id=?"
            + " and dr.trace_id=? and dr.request_id=? and dr.source_system=?",
        "V6 request/run",
        tenantId,
        id.decisionRequestId(),
        id.decisionRunId(),
        correlation.traceId(),
        correlation.requestId(),
        command.source());
    requireSourceRow(
        "select id from qdr_prompt_version where tenant_id=? and id=?"
            + " and version=? and checksum=?",
        "V8 prompt version",
        tenantId,
        id.promptVersionId(),
        versions.promptVersionRef(),
        versions.promptVersionChecksum());
    requireSourceRow(
        "select id from qdr_model_version where tenant_id=? and id=?"
            + " and model_version=? and checksum=?",
        "V8 model version",
        tenantId,
        id.modelVersionId(),
        versions.modelVersionRef(),
        versions.modelVersionChecksum());
    requireSourceRow(
        "select id from qdr_model_gateway_call"
            + " where tenant_id=? and id=? and decision_run_id=?"
            + " and prompt_version_id=? and model_version_id=? and model_call_ref=?"
            + " and trace_id=? and request_id=?",
        "V8 gateway call",
        tenantId,
        id.modelCallId(),
        id.decisionRunId(),
        id.promptVersionId(),
        id.modelVersionId(),
        id.modelCallRef(),
        correlation.traceId(),
        correlation.requestId());
    final V9Projection projection = requireV9Projection(
        tenantId,
        command);
    if (id.evaluationCaseRowId() != null) {
      requireSourceRow(
          "select id from qdr_evaluation_case"
              + " where tenant_id=? and id=? and evaluation_id=? and case_id=?"
              + " and source_decision_id=? and source_request_id=?"
              + " and trace_id=? and request_id=? and policy_version=?"
              + " and model_version_ref=? and model_gateway_version_ref=?"
              + " and input_ref_id=? and expected_summary_id=? and expected_summary_hash=?",
          "V9 evaluation case",
          tenantId,
          id.evaluationCaseRowId(),
          id.evaluationCaseId(),
          id.replayCaseId(),
          correlation.decisionId(),
          correlation.requestId(),
          correlation.traceId(),
          correlation.requestId(),
          versions.evaluationPolicyVersion(),
          versions.modelVersionRef(),
          versions.modelGatewayVersionRef(),
          projection.inputRefId(),
          projection.expectedSummaryId(),
          command.expectedSummaryHash());
    }
    if (id.regressionVerdictRowId() != null) {
      requireSourceRow(
          "select id from qdr_regression_verdict"
              + " where tenant_id=? and id=? and verdict_id=? and case_id=?"
              + " and evaluation_id=? and source_decision_id=? and source_request_id=?"
              + " and trace_id=? and request_id=? and policy_version=?"
              + " and model_gateway_version_ref=? and input_ref_id=?"
              + " and expected_summary_id=? and expected_summary_hash=?",
          "V9 regression verdict",
          tenantId,
          id.regressionVerdictRowId(),
          id.regressionVerdictId(),
          id.replayCaseId(),
          id.evaluationCaseId(),
          correlation.decisionId(),
          correlation.requestId(),
          correlation.traceId(),
          correlation.requestId(),
          versions.evaluationPolicyVersion(),
          versions.modelGatewayVersionRef(),
          projection.inputRefId(),
          projection.expectedSummaryId(),
          command.expectedSummaryHash());
    }
  }

  /**
   * 通过 tenant + replay physical/business ID 精确读取 V9 input/summary projection，并逐字段比较。
   *
   * <p>这里不接受 checksum-only、latest、时间顺序或 safe-ref 近似匹配；source 不存在、跨 tenant、JSON/字段漂移均
   * fail-closed。返回的 UUID 仅用于可选 evaluation/verdict lineage 的同源验证。
   */
  private V9Projection requireV9Projection(
      final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
    final CanonicalReplaySnapshotIdentity id = command.identity();
    final DecisionEvidenceCorrelation correlation = id.correlation();
    final CanonicalReplaySnapshotVersionVector versions = command.versionVector();
    final String sql =
        "select replay.input_ref_id, replay.expected_summary_id, replay.expected_summary_hash,"
            + " input.ref_type, input.ref_id, input.input_ref, input.content_hash,"
            + " summary.summary_role, summary.decision_type, summary.action_label,"
            + " summary.confidence_band, summary.risk_level, summary.summary_json,"
            + " summary.required_evidence_refs_json,"
            + " summary.forbidden_actions_json, summary.summary_hash"
            + " from qdr_replay_case replay"
            + " join qdr_replay_input_ref input"
            + " on input.tenant_id=replay.tenant_id and input.id=replay.input_ref_id"
            + " join qdr_expected_decision_summary summary"
            + " on summary.tenant_id=replay.tenant_id and summary.id=replay.expected_summary_id"
            + " where replay.tenant_id=? and replay.id=? and replay.case_id=?"
            + " and replay.source_decision_id=? and replay.source_request_id=?"
            + " and replay.trace_id=? and replay.request_id=? and replay.policy_version=?"
            + " and replay.model_gateway_version_ref=?";
    final Map<String, Object> row =
        requireSourceProjection(
            sql,
            "V9 replay input/summary projection",
            tenantId,
            id.replayCaseRowId(),
            id.replayCaseId(),
            correlation.decisionId(),
            correlation.requestId(),
            correlation.traceId(),
            correlation.requestId(),
            versions.policyVersion(),
            versions.modelGatewayVersionRef());

    final ReplayInputRef expectedInput = command.replayInputRef();
    final Map<String, Object> storedInput = readMap(row.get("input_ref"), "V9 replay input ref");
    if (!expectedInput.refType().equals(text(row, "ref_type"))
        || !expectedInput.refId().equals(text(row, "ref_id"))
        || !expectedInput.contentHash().equals(text(row, "content_hash"))
        || !expectedInput.refType().equals(string(storedInput, "refType"))
        || !expectedInput.refId().equals(string(storedInput, "refId"))
        || !expectedInput.contentHash().equals(string(storedInput, "contentHash"))
        || !command.replayInputHash().equals(text(row, "content_hash"))) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "V9 ReplayInputRef or replay_input_hash exact projection mismatch");
    }

    final ExpectedDecisionSummary expected = command.expectedDecisionSummary();
    if (!"EXPECTED".equals(text(row, "summary_role"))
        || !expected.equals(readSummary(row.get("summary_json")))
        || !expected.decisionType().equals(text(row, "decision_type"))
        || !expected.actionLabel().equals(text(row, "action_label"))
        || !expected.confidenceBand().equals(text(row, "confidence_band"))
        || !expected.riskLevel().name().equals(text(row, "risk_level"))
        || !expected.requiredEvidenceRefs()
            .equals(stringList(row.get("required_evidence_refs_json"), "requiredEvidenceRefs"))
        || !expected.forbiddenActions()
            .equals(stringList(row.get("forbidden_actions_json"), "forbiddenActions"))
        || !command.expectedSummaryHash().equals(text(row, "summary_hash"))
        || !command.expectedSummaryHash().equals(text(row, "expected_summary_hash"))) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "V9 expected decision summary exact projection mismatch");
    }
    return new V9Projection(uuid(row, "input_ref_id"), uuid(row, "expected_summary_id"));
  }

  private Map<String, Object> requireSourceProjection(
      final String sql, final String sourceName, final Object... arguments) {
    final List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, arguments);
    if (rows.size() != 1) {
      throw new CanonicalReplaySnapshotPersistenceException(sourceName + " exact identity mismatch");
    }
    return rows.getFirst();
  }

  /** V9 replay case 已冻结的 input/expected-summary physical lineage。 */
  private record V9Projection(UUID inputRefId, UUID expectedSummaryId) {}

  private void requireSourceRow(
      final String sql, final String sourceName, final Object... arguments) {
    final List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, arguments);
    if (rows.size() != 1) {
      throw new CanonicalReplaySnapshotPersistenceException(
          sourceName + " exact identity mismatch");
    }
  }

  private Map<String, Object> subjectPayload(final DecisionSubject subject) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("symbol", subject.symbol());
    payload.put("market", subject.market());
    payload.put("timeframe", subject.timeframe());
    if (subject.strategyRef() != null) {
      payload.put("strategyRef", subject.strategyRef());
    }
    if (subject.researchRef() != null) {
      payload.put("researchRef", subject.researchRef());
    }
    return payload;
  }

  private Map<String, Object> contextPayload(final DecisionContextSnapshot context) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("snapshotId", context.snapshotId());
    payload.put("capturedAt", context.capturedAt().toString());
    payload.put("evidenceRefs", context.evidenceRefs());
    return payload;
  }

  private List<Map<String, Object>> evidencePayload(final List<DecisionEvidenceRef> refs) {
    return refs.stream()
        .map(
            ref -> {
              final Map<String, Object> payload = new LinkedHashMap<>();
              payload.put("evidenceType", ref.evidenceType().name());
              payload.put("refId", ref.refId());
              if (ref.contentHash() != null) {
                payload.put("contentHash", ref.contentHash());
              }
              payload.put("sourceType", ref.sourceType());
              payload.put("mandatory", ref.mandatory());
              payload.put("redactionStatus", ref.redactionStatus().name());
              return payload;
            })
        .toList();
  }

  private Map<String, Object> summaryPayload(final ExpectedDecisionSummary summary) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("decisionType", summary.decisionType());
    payload.put("actionLabel", summary.actionLabel());
    payload.put("confidenceBand", summary.confidenceBand());
    payload.put("riskLevel", summary.riskLevel().name());
    payload.put("requiredEvidenceRefs", summary.requiredEvidenceRefs());
    payload.put("forbiddenActions", summary.forbiddenActions());
    return payload;
  }

  private DecisionSubject readSubject(final Object value) {
    final Map<String, Object> payload = readMap(value, "subject");
    return new DecisionSubject(
        string(payload, "symbol"),
        string(payload, "market"),
        string(payload, "timeframe"),
        optionalString(payload, "strategyRef"),
        optionalString(payload, "researchRef"));
  }

  private List<DecisionEvidenceRef> readEvidenceRefs(
      final Object value, final DecisionEvidenceCorrelation correlation) {
    final List<Map<String, Object>> payloads = readListOfMaps(value, "evidenceRefs");
    final List<DecisionEvidenceRef> refs = new ArrayList<>();
    for (Map<String, Object> payload : payloads) {
      final String refId = string(payload, "refId");
      final DecisionEvidencePolicy.EvidenceType evidenceType =
          DecisionEvidencePolicy.EvidenceType.valueOf(string(payload, "evidenceType"));
      final String sourceType = string(payload, "sourceType");
      refs.add(
          new DecisionEvidenceRef(
              new DecisionEvidence(refId, evidenceType.name(), sourceType),
              correlation,
              evidenceType,
              refId,
              optionalString(payload, "contentHash"),
              sourceType,
              Boolean.parseBoolean(string(payload, "mandatory")),
              RedactionStatus.valueOf(string(payload, "redactionStatus"))));
    }
    return List.copyOf(refs);
  }

  private ReplayInputRef readInputRef(final Object value) {
    final Map<String, Object> payload = readMap(value, "replayInputRef");
    return ReplayPersistenceGuard.requireInputRef(
        new ReplayInputRef(
            string(payload, "refType"), string(payload, "refId"), string(payload, "contentHash")));
  }

  private ExpectedDecisionSummary readSummary(final Object value) {
    final Map<String, Object> payload = readMap(value, "expectedDecisionSummary");
    return ReplayPersistenceGuard.requireSummary(
        new ExpectedDecisionSummary(
            string(payload, "decisionType"),
            string(payload, "actionLabel"),
            string(payload, "confidenceBand"),
            RiskLevel.valueOf(string(payload, "riskLevel")),
            stringList(payload.get("requiredEvidenceRefs"), "requiredEvidenceRefs"),
            stringList(payload.get("forbiddenActions"), "forbiddenActions")));
  }

  private String writeJson(final Object value, final String fieldName) {
    ReplayPersistenceGuard.rejectUnsafeJson(value, fieldName);
    try {
      return objectMapper.writeValueAsString(value);
    } catch (final JsonProcessingException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "serialize " + fieldName + " failed", error);
    }
  }

  private Map<String, Object> readMap(final Object value, final String fieldName) {
    try {
      final Map<String, Object> payload =
          value instanceof Map<?, ?> map
              ? objectMapper.convertValue(map, MAP_TYPE)
              : objectMapper.readValue(
                  Objects.requireNonNull(value, fieldName).toString(), MAP_TYPE);
      ReplayPersistenceGuard.rejectUnsafeJson(payload, fieldName);
      return payload;
    } catch (final JsonProcessingException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "parse " + fieldName + " failed", error);
    }
  }

  private List<Map<String, Object>> readListOfMaps(final Object value, final String fieldName) {
    try {
      final List<Map<String, Object>> payload =
          value instanceof List<?> list
              ? objectMapper.convertValue(list, LIST_OF_MAPS_TYPE)
              : objectMapper.readValue(
                  Objects.requireNonNull(value, fieldName).toString(), LIST_OF_MAPS_TYPE);
      ReplayPersistenceGuard.rejectUnsafeJson(payload, fieldName);
      return payload;
    } catch (final JsonProcessingException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "parse " + fieldName + " failed", error);
    }
  }

  private List<String> stringList(final Object value, final String fieldName) {
    final Object present = Objects.requireNonNull(value, fieldName);
    final Object safe;
    try {
      // PostgreSQL JSONB 由驱动返回 PGobject；内存 Map/List 路径则保持 convertValue，二者必须同一语义。
      safe =
          present instanceof List<?>
              ? objectMapper.convertValue(present, Object.class)
              : objectMapper.readValue(present.toString(), Object.class);
    } catch (final JsonProcessingException error) {
      throw new CanonicalReplaySnapshotPersistenceException(
          "parse " + fieldName + " failed", error);
    }
    ReplayPersistenceGuard.rejectUnsafeJson(safe, fieldName);
    if (!(safe instanceof List<?> list)) {
      throw new CanonicalReplaySnapshotPersistenceException(fieldName + " must be a JSON array");
    }
    return list.stream()
        .map(
            item -> {
              final String checked = Objects.requireNonNull(item, fieldName).toString().trim();
              if (checked.isEmpty()) {
                throw new CanonicalReplaySnapshotPersistenceException(
                    fieldName + " must not contain blank values");
              }
              return checked;
            })
        .toList();
  }

  private static String string(final Map<String, Object> row, final String key) {
    final String value = optionalText(row, key);
    if (value == null) {
      throw new CanonicalReplaySnapshotPersistenceException(key + " must not be null");
    }
    return value;
  }

  private static String text(final Map<String, Object> row, final String key) {
    return string(row, key);
  }

  private static String optionalString(final Map<String, Object> row, final String key) {
    return optionalText(row, key);
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
    return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
  }

  private static UUID uuidOrNull(final Map<String, Object> row, final String key) {
    final Object value = row.get(key);
    return value == null
        ? null
        : value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
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

  private static Timestamp timestamp(final Instant value) {
    return Timestamp.from(Objects.requireNonNull(value, "value"));
  }

  private static int integer(final Map<String, Object> row, final String key) {
    final Object value = Objects.requireNonNull(row.get(key), key);
    return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
  }
}
