package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.time.Duration;
import java.util.Objects;

/**
 * Limited dry-run 的总 deadline 与最大排队等待合同。
 *
 * @param deadline 从 bounded admission 开始计算的总 deadline。
 * @param maxQueueWait 有队列时允许等待任务开始的最长时间。
 */
public record RuntimeDeadlinePolicy(Duration deadline, Duration maxQueueWait) {

  /** 总 deadline 安全上限。 */
  public static final Duration MAX_DEADLINE = Duration.ofSeconds(30);

  /** Queue wait 安全上限。 */
  public static final Duration MAX_QUEUE_WAIT = Duration.ofSeconds(1);

  /** 拒绝 null duration；数值合法性由 policy 统一 fail-closed 判定。 */
  public RuntimeDeadlinePolicy {
    deadline = Objects.requireNonNull(deadline, "deadline");
    maxQueueWait = Objects.requireNonNull(maxQueueWait, "maxQueueWait");
  }

  /**
   * 校验 deadline 与 queue wait hard ceiling。
   *
   * @param queueCapacity bounded queue 容量。
   * @return 是否可用于 runtime admission。
   */
  public boolean isValid(final int queueCapacity) {
    if (deadline.isZero() || deadline.isNegative() || deadline.compareTo(MAX_DEADLINE) > 0) {
      return false;
    }
    if (maxQueueWait.isNegative() || maxQueueWait.compareTo(MAX_QUEUE_WAIT) > 0) {
      return false;
    }
    if (queueCapacity > 0 && maxQueueWait.compareTo(deadline) >= 0) {
      return false;
    }
    return queueCapacity != 0 || maxQueueWait.isZero();
  }
}
