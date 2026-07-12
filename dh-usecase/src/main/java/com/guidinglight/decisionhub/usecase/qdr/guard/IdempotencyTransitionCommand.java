package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Expected-state/version/token条件转换命令；adapter不得提供任意update入口。
 *
 * @param identity 完整identity。
 * @param requestId exact requestId。
 * @param requestHash immutable request hash。
 * @param expectedState expected source state。
 * @param expectedVersion expected CAS version。
 * @param expectedLeaseToken IN_PROGRESS转换的current token。
 * @param targetState 合法目标状态。
 * @param newLeaseToken 进入/续租IN_PROGRESS的新token。
 * @param leaseExpiresAt 新lease截止时间。
 * @param resultId COMPLETED safe result id。
 * @param resultChecksum COMPLETED checksum。
 * @param stableErrorCode FAILED stable error。
 * @param transitionedAt DB-facing业务时间；adapter仍使用DB transaction time写updated_at。
 */
public record IdempotencyTransitionCommand(
    PersistentGuardIdentity identity,
    String requestId,
    String requestHash,
    IdempotencyState expectedState,
    long expectedVersion,
    UUID expectedLeaseToken,
    IdempotencyState targetState,
    UUID newLeaseToken,
    Instant leaseExpiresAt,
    String resultId,
    String resultChecksum,
    String stableErrorCode,
    Instant transitionedAt) {

  /** 校验基本CAS输入；具体合法边由port实现再次封闭。 */
  public IdempotencyTransitionCommand {
    identity = Objects.requireNonNull(identity, "identity");
    requestId = Objects.requireNonNull(requestId, "requestId");
    requestHash = Objects.requireNonNull(requestHash, "requestHash");
    expectedState = Objects.requireNonNull(expectedState, "expectedState");
    targetState = Objects.requireNonNull(targetState, "targetState");
    transitionedAt = Objects.requireNonNull(transitionedAt, "transitionedAt");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("expectedVersion must not be negative");
    }
  }
}
