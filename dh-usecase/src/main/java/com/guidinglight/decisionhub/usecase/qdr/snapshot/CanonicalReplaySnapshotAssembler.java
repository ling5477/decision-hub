package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * V5/V6/V8/V9 与 B2 aggregate 到 {@link ReplayInputSnapshot} 的纯 structured assembler。
 *
 * <p>Assembler 不读取 repository、不生成当前时间、不生成 ID、不计算 hash、不写库。调用方必须在同一
 * PostgreSQL transaction 内通过现有 tenant-bound exact ports 加载 sources，并在 hash 后才能构造 write command。
 */
public final class CanonicalReplaySnapshotAssembler {

  private static final Set<String> SUBJECT_KEYS =
      Set.of("symbol", "market", "timeframe", "strategyRef", "researchRef");
  private static final Set<String> CONTEXT_KEYS = Set.of("snapshotId", "capturedAt", "evidenceRefs");
  private final Qdr6CanonicalJson canonicalJson;

  /** 创建使用指定 canonicalizer 做 payload byte accounting 的 assembler。 */
  public CanonicalReplaySnapshotAssembler(final Qdr6CanonicalJson canonicalJson) {
    this.canonicalJson = Objects.requireNonNull(canonicalJson, "canonicalJson");
  }

  /**
   * 验证 exact source bundle 并组装完整 snapshot；legacy/incomplete/mismatch/unsafe 均结构化拒绝。
   */
  public ReplayInputSnapshot assemble(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final CanonicalReplaySnapshotSources sources) {
    try {
      final CanonicalReplaySnapshotAssemblyRequest checkedRequest =
          Objects.requireNonNull(request, "request");
      final CanonicalReplaySnapshotSources checkedSources =
          Objects.requireNonNull(sources, "sources");
      validateIdentities(checkedRequest, checkedSources);
      validateVersions(checkedRequest, checkedSources);

      final DecisionReplayView replay = checkedSources.v5Replay();
      final DecisionSubject subject = subject(replay.request().subjectJson());
      final DecisionContextSnapshot context = context(replay.context().contextSnapshotJson());
      if (!stableStrings(context.evidenceRefs())
          .equals(stableStrings(replay.context().evidenceRefsJson()))) {
        throw failure(
            CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH,
            "V5 context evidence projection mismatch");
      }

      final ReplayCaseRecord replayCase = checkedSources.v9ReplayCase();
      final ModelGatewayCallRecord gatewayCall = checkedSources.v8GatewayCall();
      return new ReplayInputSnapshot(
          checkedRequest.versionVector().snapshotSchemaVersion(),
          checkedRequest.tenantId(),
          checkedRequest.traceId(),
          checkedRequest.requestId(),
          checkedRequest.decisionId(),
          checkedRequest.decisionRunId(),
          replay.request().source(),
          replay.request().decisionType().name(),
          context.capturedAt(),
          subject,
          context,
          checkedSources.evidenceAggregate().evidenceRefs(),
          checkedRequest.versionVector().policyVersion(),
          checkedRequest.versionVector().evaluationPolicyVersion(),
          checkedRequest.versionVector().modelVersionRef(),
          checkedRequest.versionVector().modelGatewayVersionRef(),
          checkedRequest.versionVector().promptVersionRef(),
          replayCase.inputRef(),
          replayCase.inputRef().contentHash(),
          replayCase.expectedSummary(),
          replayCase.expectedSummaryHash(),
          gatewayCall.outputHash(),
          checkedRequest.versionVector().replayExecutorVersion(),
          checkedRequest.versionVector().canonicalizationVersion(),
          checkedRequest.versionVector().hashAlgorithmVersion());
    } catch (final CanonicalReplaySnapshotAssemblyException error) {
      throw error;
    } catch (final IllegalArgumentException | NullPointerException error) {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.UNSAFE_INPUT,
          "canonical snapshot source rejected",
          error);
    }
  }

  private int payloadBytes(final ReplayInputSnapshot snapshot, final int canonicalSnapshotBytes) {
    long total = 0;
    for (Object value : snapshot.persistedPayloadValues()) {
      total += canonicalJson.canonicalize(value).length;
    }
    total = Math.max(total, canonicalSnapshotBytes);
    if (total <= 0 || total > 262_144) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.UNSAFE_INPUT,
          "canonical snapshot payload exceeds frozen limit");
    }
    return Math.toIntExact(total);
  }

  /**
   * 将 V9 source bundle 的 expected hash 显式带入 write command。
   *
   * <p>该入口不存在 caller-provided hash 参数，防止 source hash 被调用方替换；canonical hash 则只能来自
   * {@link CanonicalReplaySnapshotHasher} 的结果。
   */
  public CanonicalReplaySnapshotWriteCommand toWriteCommand(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final ReplayInputSnapshot snapshot,
      final CanonicalReplaySnapshotHash hash,
      final CanonicalReplaySnapshotSources sources) {
    final CanonicalReplaySnapshotAssemblyRequest checkedRequest =
        Objects.requireNonNull(request, "request");
    final ReplayInputSnapshot checkedSnapshot = Objects.requireNonNull(snapshot, "snapshot");
    final CanonicalReplaySnapshotHash checkedHash = Objects.requireNonNull(hash, "hash");
    final CanonicalReplaySnapshotSources checkedSources = Objects.requireNonNull(sources, "sources");
    if (!checkedSnapshot.expectedSummaryHash().equals(
            checkedSources.v9ReplayCase().expectedSummaryHash())
        || !checkedSnapshot.replayInputHash().equals(
            checkedSources.v9ReplayCase().inputRef().contentHash())) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.SOURCE_CHANGED,
          "V9 source hash changed before persistence");
    }
    return new CanonicalReplaySnapshotWriteCommand(
        checkedRequest.snapshotRecordId(),
        checkedRequest.identity(),
        checkedSnapshot.source(),
        checkedSnapshot.decisionType(),
        checkedSnapshot.sourceCapturedAt(),
        checkedSnapshot.subject(),
        checkedSnapshot.contextSnapshot(),
        checkedSnapshot.evidenceRefs(),
        checkedSnapshot.replayInputRef(),
        checkedSnapshot.expectedDecisionSummary(),
        checkedRequest.versionVector(),
        checkedSnapshot.replayInputHash(),
        ReplayPersistenceGuard.requireSha256Hex(
            checkedSnapshot.expectedSummaryHash(), "expectedSummaryHash"),
        checkedSnapshot.providerSummaryHash(),
        checkedHash.lowercaseHex(),
        payloadBytes(checkedSnapshot, checkedHash.canonicalBytes().length));
  }

  private static void validateIdentities(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final CanonicalReplaySnapshotSources sources) {
    final DecisionReplayView v5 = sources.v5Replay();
    if (v5.replayStatus() != DecisionReplayStatus.FOUND
        || v5.request() == null
        || v5.context() == null
        || v5.output() == null) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.LEGACY_NOT_REPLAYABLE,
          "V5 replay source is incomplete or legacy");
    }
    requireAllEqual("tenant", request.tenantId(), v5.tenantId(), v5.request().tenantId(), v5.context().tenantId());
    requireAllEqual("trace", request.traceId(), v5.traceId(), v5.request().traceId(), v5.context().traceId());
    requireAllEqual("request", request.requestId(), v5.requestId(), v5.request().requestId(), v5.output().requestId());
    requireAllEqual("decision", request.decisionId(), v5.decisionId(), v5.request().decisionId(), v5.context().decisionId());

    final DecisionRunDetailView v6 = sources.v6Run();
    requireAllEqual("V6 tenant", request.tenantId(), v6.tenantId());
    requireAllEqual("V6 trace", request.traceId(), v6.traceId());
    requireAllEqual("V6 request", request.requestId(), v6.requestId());
    requireAllEqual("V6 request UUID", request.decisionRequestId().toString(), v6.decisionRequestId());
    requireAllEqual("V6 run UUID", request.decisionRunId().toString(), v6.decisionRunId());
    requireAllEqual("V5/V6 source", v5.request().source(), v6.sourceSystem());

    final PromptVersionRecord prompt = sources.v8Prompt();
    final ModelVersionRecord model = sources.v8Model();
    final ModelGatewayCallRecord call = sources.v8GatewayCall();
    requireAllEqual("V8 tenant", request.tenantId(), prompt.tenantId(), model.tenantId(), call.tenantId());
    requireUuid("V8 prompt", request.promptVersionId(), prompt.promptVersionId(), call.promptVersionId());
    requireUuid("V8 model", request.modelVersionId(), model.modelVersionId(), call.modelVersionId());
    requireUuid("V8 call", request.modelCallId(), call.modelGatewayCallId());
    requireUuid("V8 run", request.decisionRunId(), call.decisionRunId());
    requireAllEqual("V8 call ref", request.modelCallRef(), call.modelCallRef());
    requireAllEqual("V8 trace", request.traceId(), call.traceId());
    requireAllEqual("V8 request", request.requestId(), call.requestId());
    if (request.providerRef() != null) {
      requireAllEqual("V8 provider ref", request.providerRef(), call.providerIdentityRef());
    }

    final ReplayCaseRecord replay = sources.v9ReplayCase();
    requireUuid("V9 replay row", request.replayCaseRowId(), replay.id());
    requireAllEqual("V9 tenant", request.tenantId(), replay.tenantId());
    requireAllEqual("V9 case", request.replayCaseId(), replay.caseId());
    requireAllEqual("V9 decision", request.decisionId(), replay.sourceDecisionId());
    requireAllEqual("V9 source request", request.requestId(), replay.sourceRequestId());
    requireAllEqual("V9 trace", request.traceId(), replay.traceId());
    requireAllEqual("V9 request", request.requestId(), replay.requestId());

    validateEvaluation(request, replay, sources.v9EvaluationCase());
    validateVerdict(request, replay, sources.v9EvaluationCase(), sources.v9RegressionVerdict());

    final DecisionEvidenceAggregate aggregate = sources.evidenceAggregate();
    if (!aggregate.isComplete()) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          "B2 evidence aggregate is not complete");
    }
    requireAllEqual("aggregate tenant", request.tenantId(), aggregate.correlation().tenantId());
    requireAllEqual("aggregate trace", request.traceId(), aggregate.correlation().traceId());
    requireAllEqual("aggregate request", request.requestId(), aggregate.correlation().requestId());
    requireAllEqual("aggregate decision", request.decisionId(), aggregate.correlation().decisionId());
  }

  private static void validateEvaluation(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final ReplayCaseRecord replay,
      final EvaluationCaseRecord evaluation) {
    if (request.evaluationCaseRowId() == null) {
      if (evaluation != null) {
        throw failure(
            CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH,
            "optional evaluation lineage was not requested");
      }
      return;
    }
    if (evaluation == null) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          "requested evaluation lineage is missing");
    }
    requireUuid("evaluation row", request.evaluationCaseRowId(), evaluation.id());
    requireAllEqual("evaluation ID", request.evaluationCaseId(), evaluation.evaluationId());
    requireAllEqual("evaluation tenant", request.tenantId(), evaluation.tenantId());
    requireAllEqual("evaluation case", replay.caseId(), evaluation.caseId());
    requireAllEqual("evaluation trace", request.traceId(), evaluation.traceId());
    requireAllEqual("evaluation request", request.requestId(), evaluation.requestId());
    requireUuid("evaluation input ref", replay.inputRefId(), evaluation.inputRefId());
    requireUuid("evaluation summary", replay.expectedSummaryId(), evaluation.expectedSummaryId());
    requireAllEqual("evaluation summary hash", replay.expectedSummaryHash(), evaluation.expectedSummaryHash());
  }

  private static void validateVerdict(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final ReplayCaseRecord replay,
      final EvaluationCaseRecord evaluation,
      final RegressionVerdictRecord verdict) {
    if (request.regressionVerdictRowId() == null) {
      if (verdict != null) {
        throw failure(
            CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH,
            "optional verdict lineage was not requested");
      }
      return;
    }
    if (verdict == null || evaluation == null) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          "requested verdict lineage is missing");
    }
    requireUuid("verdict row", request.regressionVerdictRowId(), verdict.id());
    requireAllEqual("verdict ID", request.regressionVerdictId(), verdict.verdictId());
    requireAllEqual("verdict tenant", request.tenantId(), verdict.tenantId());
    requireAllEqual("verdict case", replay.caseId(), verdict.caseId());
    requireAllEqual("verdict evaluation", evaluation.evaluationId(), verdict.evaluationId());
    requireUuid("verdict input ref", replay.inputRefId(), verdict.inputRefId());
    requireUuid("verdict summary", replay.expectedSummaryId(), verdict.expectedSummaryId());
    requireAllEqual("verdict summary hash", replay.expectedSummaryHash(), verdict.expectedSummaryHash());
  }

  private static void validateVersions(
      final CanonicalReplaySnapshotAssemblyRequest request,
      final CanonicalReplaySnapshotSources sources) {
    final CanonicalReplaySnapshotVersionVector versions = request.versionVector();
    requireAllEqual("decision schema", versions.decisionSchemaVersion(), sources.v5Replay().request().schemaVersion());
    requireAllEqual("policy", versions.policyVersion(), sources.v9ReplayCase().policyVersion());
    requireAllEqual("prompt version", versions.promptVersionRef(), sources.v8Prompt().version());
    requireAllEqual("prompt checksum", versions.promptVersionChecksum(), sources.v8Prompt().checksum());
    requireAllEqual("model version", versions.modelVersionRef(), sources.v8Model().modelVersion());
    requireAllEqual("model checksum", versions.modelVersionChecksum(), sources.v8Model().checksum());
    requireAllEqual("gateway version", versions.modelGatewayVersionRef(), sources.v9ReplayCase().modelGatewayVersionRef());
    if (sources.v9EvaluationCase() != null) {
      requireAllEqual("evaluation policy", versions.evaluationPolicyVersion(), sources.v9EvaluationCase().policyVersion());
      requireAllEqual("evaluation model", versions.modelVersionRef(), sources.v9EvaluationCase().modelVersionRef());
      requireAllEqual("evaluation gateway", versions.modelGatewayVersionRef(), sources.v9EvaluationCase().modelGatewayVersionRef());
    }
    if (sources.v9RegressionVerdict() != null) {
      requireAllEqual("verdict policy", versions.evaluationPolicyVersion(), sources.v9RegressionVerdict().policyVersion());
      requireAllEqual("verdict gateway", versions.modelGatewayVersionRef(), sources.v9RegressionVerdict().modelGatewayVersionRef());
    }
  }

  private static DecisionSubject subject(final Map<String, Object> source) {
    final Map<String, Object> checked = exactMap(source, SUBJECT_KEYS, "subject");
    return new DecisionSubject(
        requiredText(checked, "symbol"),
        requiredText(checked, "market"),
        requiredText(checked, "timeframe"),
        optionalText(checked, "strategyRef"),
        optionalText(checked, "researchRef"));
  }

  private static DecisionContextSnapshot context(final Map<String, Object> source) {
    final Map<String, Object> checked = exactMap(source, CONTEXT_KEYS, "contextSnapshot");
    final Object capturedAt = checked.get("capturedAt");
    final Instant instant;
    if (capturedAt instanceof Instant value) {
      instant = value;
    } else if (capturedAt instanceof OffsetDateTime value) {
      instant = value.toInstant();
    } else {
      instant = Instant.parse(Objects.requireNonNull(capturedAt, "capturedAt").toString());
    }
    final Object evidence = Objects.requireNonNull(checked.get("evidenceRefs"), "evidenceRefs");
    if (!(evidence instanceof List<?> list)) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.LEGACY_NOT_REPLAYABLE,
          "V5 context evidence refs are not structured");
    }
    final List<String> refs = new ArrayList<>();
    for (Object value : list) {
      refs.add(ReplayPersistenceGuard.requireSafeText(Objects.requireNonNull(value).toString(), "evidenceRef"));
    }
    return new DecisionContextSnapshot(requiredText(checked, "snapshotId"), instant, refs);
  }

  private static Map<String, Object> exactMap(
      final Map<String, Object> value, final Set<String> allowlist, final String field) {
    final Map<String, Object> checked = Map.copyOf(Objects.requireNonNull(value, field));
    ReplayPersistenceGuard.rejectUnsafeJson(checked, field);
    if (!allowlist.containsAll(checked.keySet())) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.UNSAFE_INPUT,
          field + " contains non-allowlisted fields");
    }
    return checked;
  }

  private static String requiredText(final Map<String, Object> value, final String key) {
    return ReplayPersistenceGuard.requireSafeText(
        Objects.requireNonNull(value.get(key), key).toString(), key);
  }

  private static String optionalText(final Map<String, Object> value, final String key) {
    final Object candidate = value.get(key);
    return candidate == null
        ? null
        : ReplayPersistenceGuard.optionalSafeText(candidate.toString(), key);
  }

  private static List<String> stableStrings(final List<String> values) {
    final Set<String> unique = new HashSet<>();
    for (String value : values) {
      unique.add(ReplayPersistenceGuard.requireSafeText(value, "evidenceRef"));
    }
    return unique.stream().sorted().toList();
  }

  private static void requireUuid(final String field, final UUID expected, final UUID... actual) {
    for (UUID value : actual) {
      if (!Objects.equals(expected, value)) {
        throw failure(
            CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH,
            field + " exact identity mismatch");
      }
    }
  }

  private static void requireAllEqual(
      final String field, final String expected, final String... actual) {
    for (String value : actual) {
      if (!Objects.equals(expected, value)) {
        final CanonicalReplaySnapshotAssemblyException.Code code =
            field.toLowerCase(java.util.Locale.ROOT).contains("tenant")
                ? CanonicalReplaySnapshotAssemblyException.Code.TENANT_MISMATCH
                : CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH;
        throw failure(code, field + " exact identity mismatch");
      }
    }
  }

  private static CanonicalReplaySnapshotAssemblyException failure(
      final CanonicalReplaySnapshotAssemblyException.Code code, final String message) {
    return new CanonicalReplaySnapshotAssemblyException(code, message);
  }

}
