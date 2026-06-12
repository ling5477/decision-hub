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
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2：InMemoryNonceReplayGuard bounded memory cap 测试。
 *
 * <p>覆盖：容量上限拒绝溢出、TTL 清理优先、满容量不驱逐未过期 key（防重放窗口优先）、TTL 下限不缩短窗口、 非法配置
 * fail-closed、默认构造亦有界。全部 test-only，固定假 replay key，不读 .env / secret / token。
 */
class BoundedInMemoryNonceReplayGuardTest {

  private static final Instant T0 = Instant.parse("2026-06-12T00:00:00Z");

  /** 可推进的测试时钟，用于确定性触发 TTL 过期。 */
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
  void memory_cap_rejects_overflow_for_nonce_guard() {
    final InMemoryNonceReplayGuard guard =
        new InMemoryNonceReplayGuard(1, Duration.ofSeconds(600), new MutableClock(T0));
    assertTrue(guard.markIfAbsent("k1", T0.plusSeconds(600)));
    // maxEntries=1 已满，第二个未过期 key 必须被拒绝，size 不超过上限（不无界增长）。
    assertFalse(guard.markIfAbsent("k2", T0.plusSeconds(600)));
    assertEquals(1, guard.size());
  }

  @Test
  void memory_cap_ttl_cleanup_allows_new_nonce_after_expiry() {
    final MutableClock clock = new MutableClock(T0);
    final InMemoryNonceReplayGuard guard =
        new InMemoryNonceReplayGuard(1, Duration.ofSeconds(1), clock);
    assertTrue(guard.markIfAbsent("k1", T0.plusSeconds(1)));
    // 推进超过有效期：过期项先被清理，腾出空间后新 key 可登记。
    clock.advance(Duration.ofSeconds(5));
    assertTrue(guard.markIfAbsent("k2", clock.instant().plusSeconds(1)));
    assertEquals(1, guard.size());
  }

  @Test
  void memory_cap_does_not_evict_unexpired_nonce_window() {
    final InMemoryNonceReplayGuard guard =
        new InMemoryNonceReplayGuard(1, Duration.ofSeconds(600), new MutableClock(T0));
    assertTrue(guard.markIfAbsent("k1", T0.plusSeconds(600)));
    // 容量满时新 key 被拒，未过期的 k1 不得被驱逐：k1 仍处于防重放保护中（再次登记仍拒绝）。
    assertFalse(guard.markIfAbsent("k2", T0.plusSeconds(600)));
    assertFalse(guard.markIfAbsent("k1", T0.plusSeconds(600)));
    assertEquals(1, guard.size());
  }

  @Test
  void replay_same_key_still_rejected() {
    final InMemoryNonceReplayGuard guard =
        new InMemoryNonceReplayGuard(10, Duration.ofSeconds(600), new MutableClock(T0));
    assertTrue(guard.markIfAbsent("k1", T0.plusSeconds(600)));
    assertFalse(guard.markIfAbsent("k1", T0.plusSeconds(600)));
  }

  @Test
  void ttl_floor_never_shortens_replay_window() {
    final MutableClock clock = new MutableClock(T0);
    final InMemoryNonceReplayGuard guard =
        new InMemoryNonceReplayGuard(10, Duration.ofSeconds(600), clock);
    // 传入极短 expiresAt（now+1s），但 ttl=600s 提供下限：有效期取 laterOf -> now+600s。
    assertTrue(guard.markIfAbsent("k1", T0.plusSeconds(1)));
    // 超过传入 expiresAt 但远未到 ttl 下限：条目不应被清理，重放仍被拒（窗口未被缩短）。
    clock.advance(Duration.ofSeconds(60));
    guard.cleanupExpired();
    assertFalse(guard.markIfAbsent("k1", T0.plusSeconds(1)));
  }

  @Test
  void bad_config_fails_closed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNonceReplayGuard(0, Duration.ofSeconds(600), Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNonceReplayGuard(-1, Duration.ofSeconds(600), Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNonceReplayGuard(10, Duration.ZERO, Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNonceReplayGuard(10, Duration.ofSeconds(-1), Clock.systemUTC()));
  }

  @Test
  void default_constructor_is_bounded() {
    // 默认构造也必须有界（不再是无界 ConcurrentHashMap）。
    final InMemoryNonceReplayGuard guard = new InMemoryNonceReplayGuard();
    final Instant future = Instant.now().plusSeconds(600);
    assertTrue(guard.markIfAbsent("k1", future));
    assertFalse(guard.markIfAbsent("k1", future));
    assertEquals(1, guard.size());
  }
}
