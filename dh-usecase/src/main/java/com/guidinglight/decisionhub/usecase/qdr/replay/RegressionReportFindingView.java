package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage-QDR-4 B4 regression report finding 只读视图。
 *
 * <p>finding view 只展示结构化 code、severity、脱敏 message 与 safe evidence ref；构造期再次
 * 运行 redaction / trading-term guard，避免 repository 中的异常数据被当作报告内容透出。
 *
 * @param findingCode    finding code。
 * @param severity       finding severity。
 * @param findingMessage 脱敏 finding message。
 * @param evidenceRef    safe evidence ref，可为空。
 * @param createdAt      创建时间。
 * @param updatedAt      更新时间。
 */
public record RegressionReportFindingView(
        String findingCode,
        RegressionSeverity severity,
        String findingMessage,
        String evidenceRef,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验 finding view 不携带 raw material 或 executable action。
     */
    public RegressionReportFindingView {
        findingCode = ReplayPersistenceGuard.requireSafeText(findingCode, "findingCode");
        severity = Objects.requireNonNull(severity, "severity");
        findingMessage = QdrRegressionSafety.requireFindingMessage(findingMessage);
        evidenceRef = ReplayPersistenceGuard.optionalSafeText(evidenceRef, "evidenceRef");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }

    static RegressionReportFindingView from(final RegressionFindingRecord record) {
        return new RegressionReportFindingView(
                record.findingCode(),
                record.severity(),
                record.findingMessage(),
                record.evidenceRef(),
                record.createdAt(),
                record.updatedAt());
    }
}
