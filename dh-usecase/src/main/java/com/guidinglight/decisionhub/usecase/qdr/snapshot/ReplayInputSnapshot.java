package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Stage-QDR-6 的完整 structured replay input snapshot。
 *
 * <p>该对象只保存 canonical hash 所需的安全结构化语义；不包含 snapshot 数据库主键、数据库
 * {@code created_at}、当前时钟、随机值、机器路径、raw prompt、provider response、凭证或交易指令。
 * physical identity 由外层 assembly request/persistence identity 管理，避免与 business/safe refs 混用。
 */
public record ReplayInputSnapshot(
    String snapshotSchemaVersion,
    String tenantId,
    String traceId,
    String requestId,
    String decisionId,
    UUID decisionRunId,
    String source,
    String decisionType,
    Instant sourceCapturedAt,
    DecisionSubject subject,
    DecisionContextSnapshot contextSnapshot,
    List<DecisionEvidenceRef> evidenceRefs,
    String policyVersion,
    String evaluationPolicyVersion,
    String modelVersionRef,
    String modelGatewayVersionRef,
    String promptVersionRef,
    ReplayInputRef replayInputRef,
    String replayInputHash,
    ExpectedDecisionSummary expectedDecisionSummary,
    String expectedSummaryHash,
    String providerSummaryHash,
    String replayAlgorithmVersion,
    String canonicalizationVersion,
    String hashAlgorithmVersion) {

  /** 校验 canonical snapshot required fields，并对 evidence refs 做稳定去重排序。 */
  public ReplayInputSnapshot {
    snapshotSchemaVersion = ReplayPersistenceGuard.requireSafeText(snapshotSchemaVersion, "snapshotSchemaVersion");
    tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
    traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
    requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
    decisionId = ReplayPersistenceGuard.requireSafeText(decisionId, "decisionId");
    decisionRunId = ReplayPersistenceGuard.requireUuid(decisionRunId, "decisionRunId");
    source = ReplayPersistenceGuard.requireSafeText(source, "source");
    decisionType = ReplayPersistenceGuard.requireSafeText(decisionType, "decisionType");
    if (!"READ_ONLY_RECOMMENDATION".equals(decisionType)) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    sourceCapturedAt = ReplayPersistenceGuard.requireInstant(sourceCapturedAt, "sourceCapturedAt");
    subject = Objects.requireNonNull(subject, "subject");
    contextSnapshot = Objects.requireNonNull(contextSnapshot, "contextSnapshot");
    if (!sourceCapturedAt.equals(contextSnapshot.capturedAt())) {
      throw new IllegalArgumentException("sourceCapturedAt must match context snapshot capturedAt");
    }
    evidenceRefs = stableEvidenceRefs(evidenceRefs, tenantId, traceId, requestId, decisionId);
    policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
    evaluationPolicyVersion =
        ReplayPersistenceGuard.requireSafeText(evaluationPolicyVersion, "evaluationPolicyVersion");
    modelVersionRef = ReplayPersistenceGuard.requireSafeText(modelVersionRef, "modelVersionRef");
    modelGatewayVersionRef =
        ReplayPersistenceGuard.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
    promptVersionRef = ReplayPersistenceGuard.requireSafeText(promptVersionRef, "promptVersionRef");
    replayInputRef = ReplayPersistenceGuard.requireInputRef(replayInputRef);
    replayInputHash = ReplayPersistenceGuard.requireSha256Hex(replayInputHash, "replayInputHash");
    if (!replayInputHash.equals(replayInputRef.contentHash())) {
      throw new IllegalArgumentException("replayInputHash must match structured replayInputRef");
    }
    expectedDecisionSummary = ReplayPersistenceGuard.requireSummary(expectedDecisionSummary);
    expectedSummaryHash =
        ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
    providerSummaryHash =
        ReplayPersistenceGuard.optionalSha256Hex(providerSummaryHash, "providerSummaryHash");
    replayAlgorithmVersion =
        ReplayPersistenceGuard.requireSafeText(replayAlgorithmVersion, "replayAlgorithmVersion");
    canonicalizationVersion =
        ReplayPersistenceGuard.requireSafeText(canonicalizationVersion, "canonicalizationVersion");
    hashAlgorithmVersion =
        ReplayPersistenceGuard.requireSafeText(hashAlgorithmVersion, "hashAlgorithmVersion");
  }

  /**
   * 仅使用数据库回读的 immutable record 重建 canonical replay input。
   *
   * <p>该方法不读取 V5/V6/V8/V9 source，不使用当前时间、随机值或环境配置；数据库主键与
   * {@code createdAt} 也不会进入 snapshot。
   *
   * @param record tenant-bound persisted snapshot record。
   * @return 与 P3 写入时 canonical input 等价的结构化 snapshot。
   */
  public static ReplayInputSnapshot fromPersistedRecord(
      final CanonicalReplaySnapshotRecord record) {
    final CanonicalReplaySnapshotRecord checked = Objects.requireNonNull(record, "record");
    final CanonicalReplaySnapshotVersionVector versions = checked.versionVector();
    return new ReplayInputSnapshot(
        versions.snapshotSchemaVersion(),
        checked.identity().tenantId(),
        checked.identity().correlation().traceId(),
        checked.identity().correlation().requestId(),
        checked.identity().correlation().decisionId(),
        checked.identity().decisionRunId(),
        checked.source(),
        checked.decisionType(),
        checked.sourceCapturedAt(),
        checked.subject(),
        checked.contextSnapshot(),
        checked.evidenceRefs(),
        versions.policyVersion(),
        versions.evaluationPolicyVersion(),
        versions.modelVersionRef(),
        versions.modelGatewayVersionRef(),
        versions.promptVersionRef(),
        checked.replayInputRef(),
        checked.replayInputHash(),
        checked.expectedDecisionSummary(),
        checked.expectedSummaryHash(),
        checked.providerSummaryHash(),
        versions.replayExecutorVersion(),
        versions.canonicalizationVersion(),
        versions.hashAlgorithmVersion());
  }

  /**
   * 返回唯一 canonical hash input projection。
   *
   * <p>Map 顺序不参与语义；canonicalizer 会排序 keys。optional provider hash 缺失时省略，显式 null
   * 不会被伪装成 absent。evidence refs 与 summary set-like fields 已稳定排序；业务 List 仍保持声明顺序。
   */
  public Map<String, Object> canonicalValue() {
    final Map<String, Object> value = new LinkedHashMap<>();
    value.put("snapshotSchemaVersion", snapshotSchemaVersion);
    value.put("tenantId", tenantId);
    value.put("traceId", traceId);
    value.put("requestId", requestId);
    value.put("decisionId", decisionId);
    value.put("decisionRunId", decisionRunId);
    value.put("source", source);
    value.put("decisionType", decisionType);
    value.put("sourceCapturedAt", sourceCapturedAt);
    value.put("subject", subjectValue());
    value.put("contextSnapshot", contextValue());
    value.put("evidenceRefs", evidenceValues());
    value.put("policyVersion", policyVersion);
    value.put("evaluationPolicyVersion", evaluationPolicyVersion);
    value.put("modelVersionRef", modelVersionRef);
    value.put("modelGatewayVersionRef", modelGatewayVersionRef);
    value.put("promptVersionRef", promptVersionRef);
    value.put("replayInputRef", replayInputValue());
    value.put("replayInputHash", replayInputHash);
    value.put("expectedDecisionSummary", expectedSummaryValue());
    value.put("expectedSummaryHash", expectedSummaryHash);
    if (providerSummaryHash != null) {
      value.put("providerSummaryHash", providerSummaryHash);
    }
    value.put("replayAlgorithmVersion", replayAlgorithmVersion);
    value.put("canonicalizationVersion", canonicalizationVersion);
    value.put("hashAlgorithmVersion", hashAlgorithmVersion);
    return Map.copyOf(value);
  }

  /** 返回 V10 JSON payload byte accounting 使用的五个 structured projections。 */
  public List<Object> persistedPayloadValues() {
    return List.of(subjectValue(), contextValue(), evidenceValues(), replayInputValue(), expectedSummaryValue());
  }

  private Map<String, Object> subjectValue() {
    final Map<String, Object> value = new LinkedHashMap<>();
    value.put("symbol", subject.symbol());
    value.put("market", subject.market());
    value.put("timeframe", subject.timeframe());
    if (subject.strategyRef() != null) {
      value.put("strategyRef", subject.strategyRef());
    }
    if (subject.researchRef() != null) {
      value.put("researchRef", subject.researchRef());
    }
    return Map.copyOf(value);
  }

  private Map<String, Object> contextValue() {
    return Map.of(
        "snapshotId", contextSnapshot.snapshotId(),
        "capturedAt", contextSnapshot.capturedAt(),
        "evidenceRefs", stableStrings(contextSnapshot.evidenceRefs()));
  }

  private List<Map<String, Object>> evidenceValues() {
    final List<Map<String, Object>> values = new ArrayList<>();
    for (DecisionEvidenceRef ref : evidenceRefs) {
      final Map<String, Object> value = new LinkedHashMap<>();
      value.put("evidenceType", ref.evidenceType().name());
      value.put("refId", ref.refId());
      if (ref.contentHash() != null) {
        value.put("contentHash", ref.contentHash());
      }
      value.put("sourceType", ref.sourceType());
      value.put("mandatory", ref.mandatory());
      value.put("redactionStatus", ref.redactionStatus().name());
      values.add(Map.copyOf(value));
    }
    return List.copyOf(values);
  }

  private Map<String, Object> replayInputValue() {
    return Map.of(
        "refType", replayInputRef.refType(),
        "refId", replayInputRef.refId(),
        "contentHash", replayInputRef.contentHash());
  }

  private Map<String, Object> expectedSummaryValue() {
    return Map.of(
        "decisionType", expectedDecisionSummary.decisionType(),
        "actionLabel", expectedDecisionSummary.actionLabel(),
        "confidenceBand", expectedDecisionSummary.confidenceBand(),
        "riskLevel", expectedDecisionSummary.riskLevel().name(),
        "requiredEvidenceRefs", stableStrings(expectedDecisionSummary.requiredEvidenceRefs()),
        "forbiddenActions", stableForbiddenActions(expectedDecisionSummary.forbiddenActions()));
  }

  private static List<DecisionEvidenceRef> stableEvidenceRefs(
      final List<DecisionEvidenceRef> values,
      final String tenantId,
      final String traceId,
      final String requestId,
      final String decisionId) {
    final List<DecisionEvidenceRef> checked = List.copyOf(Objects.requireNonNull(values, "evidenceRefs"));
    if (checked.isEmpty()) {
      throw new IllegalArgumentException("evidenceRefs must not be empty");
    }
    final Map<String, DecisionEvidenceRef> unique = new java.util.TreeMap<>();
    for (DecisionEvidenceRef ref : checked) {
      final DecisionEvidenceRef present = Objects.requireNonNull(ref, "evidenceRef");
      if (!tenantId.equals(present.correlation().tenantId())
          || !traceId.equals(present.correlation().traceId())
          || !requestId.equals(present.correlation().requestId())
          || !decisionId.equals(present.correlation().decisionId())) {
        throw new IllegalArgumentException("evidence correlation must match snapshot");
      }
      final DecisionEvidenceRef previous = unique.putIfAbsent(present.identity(), present);
      if (previous != null && !previous.equals(present)) {
        throw new IllegalArgumentException("conflicting evidence ref identity");
      }
    }
    return List.copyOf(unique.values());
  }

  private static List<String> stableStrings(final List<String> values) {
    final TreeSet<String> sorted = new TreeSet<>();
    for (String value : Objects.requireNonNull(values, "values")) {
      sorted.add(ReplayPersistenceGuard.requireSafeText(value, "collection value"));
    }
    return List.copyOf(sorted);
  }

  private static List<String> stableForbiddenActions(final List<String> values) {
    final TreeSet<String> sorted = new TreeSet<>();
    for (String value : Objects.requireNonNull(values, "values")) {
      sorted.add(ReplayPersistenceGuard.requireForbiddenActionLabel(value));
    }
    return List.copyOf(sorted);
  }
}
