package com.guidinglight.decisionhub.security.nq;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 进程内 nonce 防重放实现（DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2：bounded memory cap）。
 *
 * <p>Why：原实现是无界 {@code ConcurrentHashMap}，恶意或异常流量可让其无限增长撑爆内存（P1-4 residual:
 * memory cap 缺失）。本实现引入 TTL 清理 + 全局条目上限，保证内存有界，且超限时 fail-closed（拒绝）而非无限增长。
 *
 * <p>关键约束（安全优先）：
 *
 * <ol>
 *   <li><b>TTL 清理优先于容量驱逐</b>：每次登记前先清理已过期条目；只有 TTL 无法腾出空间时才触发容量保护。
 *   <li><b>满容量 fail-closed，不驱逐未过期条目</b>：容量已满且无过期项时，新 key 直接拒绝（返回 false ->
 *       认证层 409 REPLAY_DETECTED）。绝不为放行新 key 而删除未过期 key，否则会打开防重放窗口让真正的重放通过。
 *   <li><b>TTL 只延长不缩短窗口</b>：条目有效期取 {@code laterOf(认证层 expiresAt, now + ttl)}，保证不会早于认证层
 *       计算的防重放窗口（= 2×maxClockSkew）过期，避免缩短窗口削弱重放防护。
 *   <li><b>不存敏感信息</b>：只存 replayKey -> 有效期 Instant；不存 nonce 明文外的 payload / secret / raw request。
 * </ol>
 *
 * <p>定位：仅用于 dev/test 或单实例辅助路径；多实例 / 生产持久化重放防护使用 {@code JdbcNonceReplayGuard}。 本实现也带上限，
 * 保证 dev/test 下同样不会无界增长。
 *
 * <p>线程安全：{@link #markIfAbsent} 与 {@link #cleanupExpired} 为复合「检查-清理-写入」操作，统一用方法级
 * {@code synchronized} 串行化，避免并发下容量判定与写入竞态。
 */
public final class InMemoryNonceReplayGuard implements NonceReplayGuard {

  /** 保守默认全局上限：足够 dev/test 与单实例辅助路径，又不至于无界。 */
  public static final int DEFAULT_MAX_ENTRIES = 10_000;

  /** 保守默认 TTL：与默认 2×maxClockSkew（300s×2）对齐，作为条目最小保留下限。 */
  public static final Duration DEFAULT_TTL = Duration.ofSeconds(600);

  private final int maxEntries;
  private final Duration ttl;
  private final Clock clock;

  /** replayKey -> 有效期（过期即可清理）。访问均在 synchronized 方法内。 */
  private final Map<String, Instant> seen = new HashMap<>();

  /** 默认构造：有界默认值（{@link #DEFAULT_MAX_ENTRIES} / {@link #DEFAULT_TTL} / 系统 UTC 时钟）。 */
  public InMemoryNonceReplayGuard() {
    this(DEFAULT_MAX_ENTRIES, DEFAULT_TTL, Clock.systemUTC());
  }

  /**
   * 全参构造。
   *
   * @param maxEntries 全局条目上限；必须 &gt; 0，否则启动失败（fail-closed，拒绝无界配置）。
   * @param ttl 条目最小保留时长；必须为正，作为防重放窗口下限。
   * @param clock 时钟；不可为 null（测试可注入可控时钟）。
   * @throws IllegalArgumentException 配置非法（上限非正 / TTL 非正）。
   */
  public InMemoryNonceReplayGuard(final int maxEntries, final Duration ttl, final Clock clock) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException(
          "InMemoryNonceReplayGuard maxEntries must be > 0 (refusing unbounded in-memory nonce store)");
    }
    Objects.requireNonNull(ttl, "ttl");
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("InMemoryNonceReplayGuard ttl must be positive");
    }
    this.maxEntries = maxEntries;
    this.ttl = ttl;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * 原子登记 replay key（有界 + TTL 优先 + 满容量 fail-closed）。
   *
   * @param replayKey source::nonce::requestId 组合 key；不可为 null。
   * @param expiresAt 认证层计算的防重放窗口过期时间；不可为 null。
   * @return true 表示首次出现；false 表示重放或容量已满（fail-closed 拒绝）。
   */
  @Override
  public synchronized boolean markIfAbsent(final String replayKey, final Instant expiresAt) {
    Objects.requireNonNull(replayKey, "replayKey");
    Objects.requireNonNull(expiresAt, "expiresAt");
    // 1) TTL 清理优先：先腾出已过期空间。
    removeExpired(clock.instant());
    // 2) 重放命中：相同 key 仍未过期 -> 拒绝。
    if (seen.containsKey(replayKey)) {
      return false;
    }
    // 3) 容量保护：清理后仍满 -> 拒绝新 key（fail-closed），绝不驱逐未过期 key 以免打开防重放窗口。
    if (seen.size() >= maxEntries) {
      return false;
    }
    // 4) 有效期取 laterOf(认证层 expiresAt, now+ttl)，只延长不缩短防重放窗口。
    final Instant floor = clock.instant().plus(ttl);
    final Instant effectiveExpiry = expiresAt.isAfter(floor) ? expiresAt : floor;
    seen.put(replayKey, effectiveExpiry);
    return true;
  }

  /**
   * 清理所有已过期条目（有效期 &lt;= now）。供定时 sweep 或测试调用。
   *
   * @return 清理掉的条目数。
   */
  public synchronized int cleanupExpired() {
    return removeExpired(clock.instant());
  }

  /**
   * 当前条目数（仅供测试断言有界性）。
   *
   * @return 当前已登记且未清理的条目数。
   */
  public synchronized int size() {
    return seen.size();
  }

  /** 移除有效期 &lt;= now 的条目，返回移除数。调用方需持有锁（均在 synchronized 方法内）。 */
  private int removeExpired(final Instant now) {
    int removed = 0;
    final Iterator<Map.Entry<String, Instant>> it = seen.entrySet().iterator();
    while (it.hasNext()) {
      final Instant exp = it.next().getValue();
      if (!exp.isAfter(now)) {
        it.remove();
        removed++;
      }
    }
    return removed;
  }
}
