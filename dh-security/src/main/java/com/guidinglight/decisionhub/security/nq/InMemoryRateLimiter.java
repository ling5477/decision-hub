package com.guidinglight.decisionhub.security.nq;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * 进程内固定窗口限流实现（DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3：bounded in-memory rate limiter）。
 *
 * <p>Why：修复 NQ feedback 入站「完全无限流」缺口。算法采用固定窗口计数器：每个
 * {@code source::tenant::route} key 维护一个窗口起点 + 计数；窗口内计数达到 {@code maxRequests} 即拒绝，
 * 窗口过期后清理并重新计数。
 *
 * <p>关键约束（安全优先，全部 fail-closed）：
 *
 * <ol>
 *   <li><b>有界</b>：key map 受 {@code maxKeys} 限制，绝不无界增长。
 *   <li><b>TTL/窗口清理优先于容量保护</b>：每次 check 先清理已过期窗口（windowEnd &lt;= now），再判定。
 *   <li><b>maxKeys 满且无过期项时 fail-closed</b>：拒绝新 key（返回 RATE_LIMITED 保护性拒绝），<b>绝不</b>为放行新 key
 *       而无界增长，也不驱逐其它租户的活跃窗口。
 *   <li><b>不接触敏感信息</b>：只接收 source / tenant / route / now，不读取 raw body、secret、签名材料。
 *   <li><b>非法配置启动失败</b>：window / maxRequests / maxKeys 非正时构造抛 {@link IllegalArgumentException}。
 * </ol>
 *
 * <p>定位：仅适合 dev/test 或单实例辅助路径；真实多实例通道需集中式（如 Redis）limiter，另起任务。本实现至少
 * 修复「完全无限流」问题，且自身有界、fail-closed。
 *
 * <p>线程安全：{@link #check} 为复合「清理-判定-写入」操作，方法级 {@code synchronized} 串行化，避免并发计数竞态。
 */
public final class InMemoryRateLimiter implements RateLimiter {

  /** 保守默认窗口（秒）。 */
  public static final int DEFAULT_WINDOW_SECONDS = 1;

  /** 保守默认单窗口最大请求数（per source+tenant+route）。 */
  public static final int DEFAULT_MAX_REQUESTS = 20;

  /** 保守默认 key 上限（兜底防无界增长）。 */
  public static final int DEFAULT_MAX_KEYS = 10_000;

  /** source / tenant 缺失时的归一化占位；与真实 tenant 组合后仍是有界 key，不会合并成单一无界公共 key。 */
  private static final String ABSENT = "-";

  private final int windowSeconds;
  private final int maxRequests;
  private final int maxKeys;
  private final Clock clock;

  /** key -> 当前窗口。访问均在 synchronized 方法内。 */
  private final Map<String, Window> buckets = new HashMap<>();

  /** 可变窗口桶：窗口起点 + 计数。仅在持锁路径内修改。 */
  private static final class Window {
    private final Instant windowStart;
    private int count;

    Window(final Instant windowStart) {
      this.windowStart = windowStart;
      this.count = 0;
    }
  }

  /** 默认构造：有界保守默认值 + 系统 UTC 时钟。 */
  public InMemoryRateLimiter() {
    this(DEFAULT_WINDOW_SECONDS, DEFAULT_MAX_REQUESTS, DEFAULT_MAX_KEYS, Clock.systemUTC());
  }

  /**
   * 全参构造。
   *
   * @param windowSeconds 固定窗口长度（秒）；必须 &gt; 0。
   * @param maxRequests 单窗口最大请求数；必须 &gt; 0。
   * @param maxKeys key 上限；必须 &gt; 0（拒绝无界 store）。
   * @param clock 时钟；不可为 null（测试可注入可控时钟）。
   * @throws IllegalArgumentException 任一上限非正（fail-closed，拒绝无界 / 无效配置）。
   */
  public InMemoryRateLimiter(
      final int windowSeconds, final int maxRequests, final int maxKeys, final Clock clock) {
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("InMemoryRateLimiter windowSeconds must be > 0");
    }
    if (maxRequests <= 0) {
      throw new IllegalArgumentException("InMemoryRateLimiter maxRequests must be > 0");
    }
    if (maxKeys <= 0) {
      throw new IllegalArgumentException(
          "InMemoryRateLimiter maxKeys must be > 0 (refusing unbounded rate-limit store)");
    }
    this.windowSeconds = windowSeconds;
    this.maxRequests = maxRequests;
    this.maxKeys = maxKeys;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public synchronized RateLimitResult check(
      final String source, final String tenantId, final String route, final Instant now) {
    final Instant at = now == null ? clock.instant() : now;
    final String key = compositeKey(source, tenantId, route);
    // 1) 窗口/TTL 清理优先：先腾出已过期窗口。
    removeExpired(at);
    final Window existing = buckets.get(key);
    if (existing != null) {
      // 活跃窗口：达到上限 -> 拒绝；否则计数 +1 放行。
      if (existing.count >= maxRequests) {
        return RateLimitResult.limited(retryAfterSeconds(existing.windowStart, at));
      }
      existing.count++;
      return RateLimitResult.pass();
    }
    // 2) 需要新 key：容量保护。清理后仍满 -> fail-closed 保护性拒绝，绝不无界增长 / 不驱逐活跃窗口。
    if (buckets.size() >= maxKeys) {
      return RateLimitResult.limited(windowSeconds);
    }
    final Window fresh = new Window(at);
    fresh.count = 1;
    buckets.put(key, fresh);
    return RateLimitResult.pass();
  }

  /**
   * 清理所有已过期窗口（windowEnd &lt;= now）。供定时 sweep 或测试调用。
   *
   * @return 清理掉的 key 数。
   */
  public synchronized int cleanupExpired() {
    return removeExpired(clock.instant());
  }

  /**
   * 当前 key 数（仅供测试断言有界性）。
   *
   * @return 当前活跃窗口 key 数。
   */
  public synchronized int size() {
    return buckets.size();
  }

  /** 移除 windowEnd &lt;= now 的窗口，返回移除数。调用方需持有锁（均在 synchronized 方法内）。 */
  private int removeExpired(final Instant now) {
    int removed = 0;
    final Iterator<Map.Entry<String, Window>> it = buckets.entrySet().iterator();
    while (it.hasNext()) {
      final Window w = it.next().getValue();
      if (!windowEnd(w.windowStart).isAfter(now)) {
        it.remove();
        removed++;
      }
    }
    return removed;
  }

  private Instant windowEnd(final Instant windowStart) {
    return windowStart.plusSeconds(windowSeconds);
  }

  /** 粗粒度剩余秒数（&gt;=1）；仅供审计 / 日志，响应不暴露精确窗口。 */
  private int retryAfterSeconds(final Instant windowStart, final Instant now) {
    final long secs = windowEnd(windowStart).getEpochSecond() - now.getEpochSecond();
    return (int) Math.max(1L, secs);
  }

  /** key = source::tenant::route；source/tenant 缺失归一化为占位符，仍是按租户隔离的有界 key。 */
  private static String compositeKey(
      final String source, final String tenantId, final String route) {
    return normalize(source) + "::" + normalize(tenantId) + "::" + normalize(route);
  }

  private static String normalize(final String value) {
    return value == null || value.isBlank() ? ABSENT : value.trim();
  }
}
