package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.Objects;

/** 幂等端口不可用或 canonical hash 冲突时的稳定 fail-closed 异常。 */
public final class FeedbackAttributionIdempotencyException extends RuntimeException {

  /** 幂等失败种类。 */
  public enum Kind {
    /** 同 key 不同 canonical hash。 */
    CONFLICT,
    /** 幂等能力不可用或容量已满。 */
    UNAVAILABLE
  }

  private final Kind kind;

  /** 创建已分类且不含原始存储错误的幂等异常。 */
  public FeedbackAttributionIdempotencyException(final Kind kind, final String message) {
    super(message);
    this.kind = Objects.requireNonNull(kind, "kind");
  }

  /** 返回稳定失败种类。 */
  public Kind kind() {
    return kind;
  }
}
