package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Duration;
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
 * @param expectedLeaseOwner IN_PROGRESS转换的current owner。
 * @param expectedLeaseToken IN_PROGRESS转换的current token。
 * @param targetState 合法目标状态。
 * @param newLeaseOwner 进入/续租IN_PROGRESS的新owner。
 * @param newLeaseToken 进入/续租IN_PROGRESS的新token。
 * @param leaseDuration 新lease时长；绝对截止时间由PostgreSQL生成。
 * @param resultType COMPLETED结果类型，当前仅允许DH_DECISION_OUTPUT。
 * @param resultId COMPLETED safe result id。
 * @param resultChecksum COMPLETED checksum。
 * @param stableErrorCode FAILED stable error。
 */
public record IdempotencyTransitionCommand(
    PersistentGuardIdentity identity,
    String requestId,
    String requestHash,
    IdempotencyState expectedState,
    long expectedVersion,
    String expectedLeaseOwner,
    UUID expectedLeaseToken,
    IdempotencyState targetState,
    String newLeaseOwner,
    UUID newLeaseToken,
    Duration leaseDuration,
    String resultType,
    String resultId,
    String resultChecksum,
    String stableErrorCode) {

  /** 校验基本CAS输入；具体合法边由port实现再次封闭。 */
  public IdempotencyTransitionCommand {
    identity = Objects.requireNonNull(identity, "identity");
    requestId = Objects.requireNonNull(requestId, "requestId");
    requestHash = Objects.requireNonNull(requestHash, "requestHash");
    expectedState = Objects.requireNonNull(expectedState, "expectedState");
    targetState = Objects.requireNonNull(targetState, "targetState");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("expectedVersion must not be negative");
    }
    if (leaseDuration != null
        && (leaseDuration.isZero()
            || leaseDuration.isNegative()
            || leaseDuration.compareTo(Duration.ofHours(1)) > 0)) {
      throw new IllegalArgumentException("leaseDuration outside safety ceiling");
    }
  }
}
