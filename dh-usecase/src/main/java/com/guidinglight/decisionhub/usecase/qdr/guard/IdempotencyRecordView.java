package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Instant;
import java.util.UUID;

/**
 * Exact idempotency记录安全投影；不包含raw/canonical request或敏感材料。
 *
 * @param guardId stable guard id。
 * @param identity 完整隔离身份。
 * @param requestId exact request id。
 * @param requestHash SHA-256。
 * @param state 当前状态。
 * @param stateVersion CAS版本。
 * @param leaseOwner 低敏感内部实例标识；不得进入响应或日志。
 * @param leaseToken opaque内部lease；不得进入响应或日志。
 * @param leaseExpiresAt lease截止时间。
 * @param resultType safe result类型。
 * @param resultId safe result reference。
 * @param resultChecksum safe projection checksum。
 * @param stableErrorCode 冻结失败码。
 * @param expiresAt duplicate语义过期时间。
 * @param retentionUntil 最早清理时间。
 */
public record IdempotencyRecordView(
    UUID guardId,
    PersistentGuardIdentity identity,
    String requestId,
    String requestHash,
    IdempotencyState state,
    long stateVersion,
    String leaseOwner,
    UUID leaseToken,
    Instant leaseExpiresAt,
    String resultType,
    String resultId,
    String resultChecksum,
    String stableErrorCode,
    Instant expiresAt,
    Instant retentionUntil) {}
