package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 已完成 {@code QDR6-CJSON-1 + SHA-256} 的 canonical replay snapshot 写入合同。
 *
 * <p>该类型只表示可进入 immutable persistence 的完整 structured material，不表示组装中对象。调用方必须先完成
 * structured assembler、canonicalizer 与 deterministic hash，才能构造本合同并调用 persistence port。它刻意不包含
 * {@code createdAt}，从而禁止调用方控制数据库审计时间；本类型本身不实现 assembler、canonicalizer 或 hash。
 */
public record CanonicalReplaySnapshotWriteCommand(
    UUID id,
    CanonicalReplaySnapshotIdentity identity,
    String source,
    String decisionType,
    Instant sourceCapturedAt,
    DecisionSubject subject,
    DecisionContextSnapshot contextSnapshot,
    List<DecisionEvidenceRef> evidenceRefs,
    ReplayInputRef replayInputRef,
    ExpectedDecisionSummary expectedDecisionSummary,
    CanonicalReplaySnapshotVersionVector versionVector,
    String replayInputHash,
    String expectedSummaryHash,
    String providerSummaryHash,
    String canonicalInputHash,
    int payloadBytes) {

  private static final Set<String> HASH_PLACEHOLDER_LABELS =
      Set.of("placeholder", "default", "latest", "current", "pending", "tbd");

  /**
   * 校验 immutable write material；canonical hash 缺失、moving placeholder 或 V9 input hash 漂移均 fail-closed。
   *
   * <p>该构造器只验证已生成结果的合同，不生成 hash，也不把 {@code createdAt} 纳入 canonical material。
   */
  public CanonicalReplaySnapshotWriteCommand {
    id = ReplayPersistenceGuard.requireUuid(id, "id");
    identity = Objects.requireNonNull(identity, "identity");
    source = ReplayPersistenceGuard.requireSafeText(source, "source");
    decisionType = ReplayPersistenceGuard.requireSafeText(decisionType, "decisionType");
    if (!"READ_ONLY_RECOMMENDATION".equals(decisionType)) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    sourceCapturedAt = ReplayPersistenceGuard.requireInstant(sourceCapturedAt, "sourceCapturedAt");
    subject = requireSubject(subject);
    contextSnapshot = requireContextSnapshot(contextSnapshot, sourceCapturedAt);
    evidenceRefs = requireEvidenceRefs(evidenceRefs, identity);
    replayInputRef = ReplayPersistenceGuard.requireInputRef(replayInputRef);
    expectedDecisionSummary = ReplayPersistenceGuard.requireSummary(expectedDecisionSummary);
    versionVector = Objects.requireNonNull(versionVector, "versionVector");
    replayInputHash = ReplayPersistenceGuard.requireSha256Hex(replayInputHash, "replayInputHash");
    if (!replayInputHash.equals(replayInputRef.contentHash())) {
      throw new IllegalArgumentException("replayInputHash must match replayInputRef.contentHash");
    }
    expectedSummaryHash =
        ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
    providerSummaryHash =
        ReplayPersistenceGuard.optionalSha256Hex(providerSummaryHash, "providerSummaryHash");
    canonicalInputHash = requireCompletedCanonicalHash(canonicalInputHash);
    if (payloadBytes <= 0 || payloadBytes > 262_144) {
      throw new IllegalArgumentException("payloadBytes must be between 1 and 262144");
    }
  }

  private static String requireCompletedCanonicalHash(final String value) {
    if (value != null && HASH_PLACEHOLDER_LABELS.contains(value.trim().toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("canonicalInputHash must not be a placeholder");
    }
    final String checked =
        ReplayPersistenceGuard.requireSha256Hex(value, "canonicalInputHash");
    if (checked.chars().allMatch(character -> character == '0')) {
      throw new IllegalArgumentException("canonicalInputHash must not be a default zero hash");
    }
    return checked;
  }

  private static DecisionSubject requireSubject(final DecisionSubject value) {
    final DecisionSubject checked = Objects.requireNonNull(value, "subject");
    ReplayPersistenceGuard.requireSafeText(checked.symbol(), "subject.symbol");
    ReplayPersistenceGuard.requireSafeText(checked.market(), "subject.market");
    ReplayPersistenceGuard.requireSafeText(checked.timeframe(), "subject.timeframe");
    ReplayPersistenceGuard.optionalSafeText(checked.strategyRef(), "subject.strategyRef");
    ReplayPersistenceGuard.optionalSafeText(checked.researchRef(), "subject.researchRef");
    return checked;
  }

  private static DecisionContextSnapshot requireContextSnapshot(
      final DecisionContextSnapshot value, final Instant sourceCapturedAt) {
    final DecisionContextSnapshot checked = Objects.requireNonNull(value, "contextSnapshot");
    ReplayPersistenceGuard.requireSafeText(checked.snapshotId(), "contextSnapshot.snapshotId");
    if (!sourceCapturedAt.equals(checked.capturedAt())) {
      throw new IllegalArgumentException("sourceCapturedAt must match contextSnapshot.capturedAt");
    }
    if (checked.evidenceRefs().isEmpty()) {
      throw new IllegalArgumentException("contextSnapshot.evidenceRefs must not be empty");
    }
    checked.evidenceRefs().forEach(
        ref -> ReplayPersistenceGuard.requireSafeText(ref, "contextSnapshot.evidenceRefs"));
    return checked;
  }

  private static List<DecisionEvidenceRef> requireEvidenceRefs(
      final List<DecisionEvidenceRef> values, final CanonicalReplaySnapshotIdentity identity) {
    final List<DecisionEvidenceRef> checked = values == null ? List.of() : List.copyOf(values);
    if (checked.isEmpty()) {
      throw new IllegalArgumentException("evidenceRefs must not be empty");
    }
    for (DecisionEvidenceRef ref : checked) {
      final DecisionEvidenceRef present = Objects.requireNonNull(ref, "evidenceRefs item");
      if (!identity.correlation().matches(present.correlation())) {
        throw new IllegalArgumentException("evidenceRefs correlation must match snapshot identity");
      }
    }
    return checked;
  }
}
