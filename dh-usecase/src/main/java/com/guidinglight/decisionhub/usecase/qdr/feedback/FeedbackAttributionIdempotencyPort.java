package com.guidinglight.decisionhub.usecase.qdr.feedback;

/**
 * Attribution 专用原子幂等端口。
 *
 * <p>端口必须保证同 key 同 hash 只执行一次 {@code firstExecution} 并返回首个稳定结果；同 key 不同 hash
 * 抛出冲突。Stage-QDR-8 只冻结此端口，不提供 production adapter。
 */
public interface FeedbackAttributionIdempotencyPort {

  /**
   * 原子执行或复用首次结果。
   *
   * @param idempotencyKey tenant/environment/decision/observation 的 domain-separated hash
   * @param canonicalHash 全部影响归因输入的 hash
   * @param firstExecution 仅首个合法输入允许执行的无外部系统归因与审计动作
   * @return 首个稳定结果
   */
  FeedbackAttributionResult execute(
      String idempotencyKey,
      String canonicalHash,
      FirstAttributionExecution firstExecution);

  /** 首次归因计算回调；端口不得重试或并发重复调用。 */
  @FunctionalInterface
  interface FirstAttributionExecution {

    /** 执行一次确定性归因与 fail-closed audit。 */
    FeedbackAttributionResult execute();
  }
}
