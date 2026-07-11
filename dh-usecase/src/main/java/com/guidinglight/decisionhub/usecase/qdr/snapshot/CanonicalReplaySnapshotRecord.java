package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical replay snapshot 的 immutable persistence record。
 *
 * <p>该 record 复用已有 {@link DecisionSubject}、{@link DecisionContextSnapshot}、
 * {@link DecisionEvidenceRef}、{@link ReplayInputRef} 与 {@link ExpectedDecisionSummary}，只携带
 * structured safe material。它不包含 raw prompt、raw provider response、credential、订单或执行指令，
 * 也不提供 Repository/JDBC、canonicalizer、hash 计算或 replay 行为。
 *
 * @param id                       snapshot 物理 UUID。
 * @param identity                 tenant-bound physical/business identity。
 * @param source                   V5 read-only decision source。
 * @param decisionType             固定 READ_ONLY_RECOMMENDATION。
 * @param sourceCapturedAt         来源快照时间，不是当前时钟。
 * @param subject                  既有 structured subject。
 * @param contextSnapshot          既有 structured context snapshot。
 * @param evidenceRefs             B1 typed safe refs。
 * @param replayInputRef           V9 structured replay input ref。
 * @param expectedDecisionSummary  V9 structured expected summary。
 * @param versionVector            完整 immutable version vector。
 * @param replayInputHash          V9 replay input hash metadata。
 * @param expectedSummaryHash      V9 expected summary hash metadata。
 * @param providerSummaryHash      可选 provider summary hash metadata。
 * @param canonicalInputHash       预先校验的 canonical input hash metadata；本批次不生成。
 * @param payloadBytes             structured payload 总字节预算。
 * @param createdAt                数据库 audit-only 创建时间。
 */
public record CanonicalReplaySnapshotRecord(
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
        int payloadBytes,
        Instant createdAt) {

    /**
     * 校验 P1 persistence record 的结构、安全边界、correlation 与 payload metadata。
     *
     * <p>这里不序列化 JSON，也不计算 canonical hash；P3 只能在后续独立授权中负责 assembler。
     */
    public CanonicalReplaySnapshotRecord {
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
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        providerSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(providerSummaryHash, "providerSummaryHash");
        canonicalInputHash =
                ReplayPersistenceGuard.requireSha256Hex(canonicalInputHash, "canonicalInputHash");
        if (payloadBytes <= 0 || payloadBytes > 262_144) {
            throw new IllegalArgumentException("payloadBytes must be between 1 and 262144");
        }
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
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
        checked.evidenceRefs()
                .forEach(ref -> ReplayPersistenceGuard.requireSafeText(ref, "contextSnapshot.evidenceRefs"));
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
