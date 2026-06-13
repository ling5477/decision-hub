package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3：InMemoryRateLimiter bounded / fail-closed 限流测试。
 *
 * <p>覆盖：超阈值拒绝、source/tenant 隔离、maxKeys 有界 fail-closed、窗口 TTL 清理后放行新 key、 非法配置 fail-closed、
 * check 不依赖 secret / payload（no_credential_access 契约）。全部 test-only，固定假 key，不读 .env / secret / token。
 */
class InMemoryRateLimiterTest {

  private static final Instant T0 = Instant.parse("2026-06-13T00:00:00Z");
  private static final String ROUTE = "NQ_FEEDBACK";

  /** 可推进的测试时钟，用于确定性触发窗口过期。 */
  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(final Instant start) {
      this.now = start;
    }

    void advance(final Duration d) {
      this.now = this.now.plus(d);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
      return this;
    }
  }

  @Test
  void rate_limit_returns_limited_after_threshold() {
    final InMemoryRateLimiter limiter = new InMemoryRateLimiter(60, 1, 100, new MutableClock(T0));
    // maxRequests=1：第一次放行，第二次同 key 超限。
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, T0).allowed());
    final RateLimitResult second = limiter.check("nexus-quant", "tenant-a", ROUTE, T0);
    assertFalse(second.allowed());
    assertEquals(RateLimitResult.REASON_RATE_LIMITED, second.reason());
    assertEquals(RateLimitResult.AUDIT_RATE_LIMITED, second.auditCode());
  }

  @Test
  void rate_limit_is_tenant_source_isolated() {
    final InMemoryRateLimiter limiter = new InMemoryRateLimiter(60, 1, 100, new MutableClock(T0));
    // 同 source+tenant+route 才共享桶：tenant-a 已用满。
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, T0).allowed());
    assertFalse(limiter.check("nexus-quant", "tenant-a", ROUTE, T0).allowed());
    // 同 source 不同 tenant 不互相污染。
    assertTrue(limiter.check("nexus-quant", "tenant-b", ROUTE, T0).allowed());
    // 同 tenant 不同 source 不互相污染。
    assertTrue(limiter.check("other-source", "tenant-a", ROUTE, T0).allowed());
  }

  @Test
  void rate_limit_store_is_bounded_fail_closed() {
    // maxKeys=1：第一个 key 占满后，第二个未过期的新 key 被 fail-closed 拒绝，size 不超过上限。
    final InMemoryRateLimiter limiter = new InMemoryRateLimiter(60, 5, 1, new MutableClock(T0));
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, T0).allowed());
    final RateLimitResult overflow = limiter.check("nexus-quant", "tenant-b", ROUTE, T0);
    assertFalse(overflow.allowed());
    assertEquals(RateLimitResult.REASON_RATE_LIMITED, overflow.reason());
    assertEquals(1, limiter.size());
  }

  @Test
  void rate_limit_ttl_cleanup_allows_new_key_after_window() {
    final MutableClock clock = new MutableClock(T0);
    final InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, 1, 100, clock);
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, clock.instant()).allowed());
    // 同窗口内超限。
    assertFalse(limiter.check("nexus-quant", "tenant-a", ROUTE, clock.instant()).allowed());
    // 推进超过窗口：过期窗口被清理，新窗口重新计数后放行。
    clock.advance(Duration.ofSeconds(2));
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, clock.instant()).allowed());
    assertEquals(1, limiter.size());
  }

  @Test
  void rate_limiter_invalid_config_fails_closed() {
    assertThrows(
        IllegalArgumentException.class, () -> new InMemoryRateLimiter(0, 1, 1, Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class, () -> new InMemoryRateLimiter(1, 0, 1, Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class, () -> new InMemoryRateLimiter(1, 1, 0, Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class, () -> new InMemoryRateLimiter(-1, 1, 1, Clock.systemUTC()));
  }

  @Test
  void rate_limit_check_requires_no_secret_or_payload() {
    // 契约（no_credential_access）：check 只接收 source / tenant / route / now，不需要 secret / payload / 签名材料。
    // 这里仅用普通字符串即可完成限流判定，证明限流路径不接触任何凭证。
    final InMemoryRateLimiter limiter = new InMemoryRateLimiter(60, 1, 100, new MutableClock(T0));
    assertTrue(limiter.check("nexus-quant", "tenant-a", ROUTE, T0).allowed());
    // source 缺失也不崩溃，且与真实 tenant 组合仍是有界 key（不合并成单一无界公共 key）。
    assertTrue(limiter.check(null, "tenant-a", ROUTE, T0).allowed());
  }
}
