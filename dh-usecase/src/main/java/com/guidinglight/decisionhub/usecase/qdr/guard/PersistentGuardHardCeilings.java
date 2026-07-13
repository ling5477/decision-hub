package com.guidinglight.decisionhub.usecase.qdr.guard;

/**
 * Stage-QDR-7 B1冻结的persistent guard绝对安全上限。
 *
 * <p>本类型只提供无状态常量，供配置快照与底层guard命令共同引用；这些上限不是生产容量默认值。
 */
public final class PersistentGuardHardCeilings {

  /** Persistent rate window绝对安全上限，单位为秒。 */
  public static final int MAX_RATE_WINDOW_SECONDS = 3_600;

  /** 单window/key persistent rate quota绝对安全上限。 */
  public static final int MAX_RATE_QUOTA = 100_000;

  /** Idempotency lease绝对安全上限，单位为秒。 */
  public static final int MAX_IDEMPOTENCY_LEASE_SECONDS = 900;

  private PersistentGuardHardCeilings() {}
}
