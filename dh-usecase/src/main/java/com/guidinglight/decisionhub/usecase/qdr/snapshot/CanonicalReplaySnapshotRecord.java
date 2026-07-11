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
 * Canonical replay snapshot 的数据库 persisted record。
 *
 * <p>该 record 复用已有 {@link DecisionSubject}、{@link DecisionContextSnapshot}、
 * {@link DecisionEvidenceRef}、{@link ReplayInputRef} 与 {@link ExpectedDecisionSummary}，只携带
 * structured safe material。它不包含 raw prompt、raw provider response、credential、订单或执行指令，
 * 也不提供 Repository/JDBC、canonicalizer、hash 计算或 replay 行为。与 write command 不同，本 record 的
 * {@code createdAt} 只能来自 insert 后数据库 exact read。
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
     * 校验 persisted record 的写入材料与数据库 audit time。
     *
     * <p>这里不序列化 JSON，也不计算 canonical hash；P3 只能在后续独立授权中负责 assembler。
     */
    public CanonicalReplaySnapshotRecord {
        final CanonicalReplaySnapshotWriteCommand command = new CanonicalReplaySnapshotWriteCommand(
                id, identity, source, decisionType, sourceCapturedAt, subject, contextSnapshot,
                evidenceRefs, replayInputRef, expectedDecisionSummary, versionVector, replayInputHash,
                expectedSummaryHash, providerSummaryHash, canonicalInputHash, payloadBytes);
        evidenceRefs = command.evidenceRefs();
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
    }

    /** 返回不含数据库 {@code createdAt} 的 immutable write material，用于幂等 exact comparison。 */
    public CanonicalReplaySnapshotWriteCommand toWriteCommand() {
        return new CanonicalReplaySnapshotWriteCommand(
                id, identity, source, decisionType, sourceCapturedAt, subject, contextSnapshot,
                evidenceRefs, replayInputRef, expectedDecisionSummary, versionVector, replayInputHash,
                expectedSummaryHash, providerSummaryHash, canonicalInputHash, payloadBytes);
    }

    /**
     * 将已校验 write command 与数据库回读的 audit time 组合为 persisted record。
     *
     * @param command 已完成 canonicalization/hash 的 immutable write material。
     * @param createdAt 数据库实际生成的 {@code created_at}。
     * @return persisted record。
     */
    public static CanonicalReplaySnapshotRecord persisted(
            final CanonicalReplaySnapshotWriteCommand command, final Instant createdAt) {
        final CanonicalReplaySnapshotWriteCommand checked = Objects.requireNonNull(command, "command");
        return new CanonicalReplaySnapshotRecord(
                checked.id(), checked.identity(), checked.source(), checked.decisionType(),
                checked.sourceCapturedAt(), checked.subject(), checked.contextSnapshot(),
                checked.evidenceRefs(), checked.replayInputRef(), checked.expectedDecisionSummary(),
                checked.versionVector(), checked.replayInputHash(), checked.expectedSummaryHash(),
                checked.providerSummaryHash(), checked.canonicalInputHash(), checked.payloadBytes(),
                createdAt);
    }

}
