package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * Limited dry-run 的 bounded concurrency / queue 合同。
 *
 * @param maxConcurrency 最大并发执行数。
 * @param queueCapacity bounded queue 容量；0 表示不排队。
 */
public record RuntimeConcurrencyPolicy(int maxConcurrency, int queueCapacity) {

  /** 最大并发 hard ceiling。 */
  public static final int MAX_CONCURRENCY = 32;

  /** 最大队列 hard ceiling。 */
  public static final int MAX_QUEUE_CAPACITY = 64;

  /** @return 是否满足正并发与 bounded queue hard ceiling。 */
  public boolean isValid() {
    return maxConcurrency >= 1
        && maxConcurrency <= MAX_CONCURRENCY
        && queueCapacity >= 0
        && queueCapacity <= MAX_QUEUE_CAPACITY;
  }
}
