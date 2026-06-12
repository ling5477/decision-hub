package com.guidinglight.decisionhub.usecase.agent.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2：InMemoryNqFeedbackEventRepository bounded memory cap 测试。
 *
 * <p>覆盖：全局上限溢出有界、TTL retention 清理、单 tenant 上限、既有查询语义不破坏、envelope 幂等 + 有界、 非法配置
 * fail-closed、默认构造亦有界。全部 test-only，不读 .env / secret / token，不触发任何外部调用。
 */
class BoundedInMemoryNqFeedbackEventRepositoryTest {

  private static final Instant T0 = Instant.parse("2026-06-12T00:00:00Z");

  /** 可推进的测试时钟，用于确定性触发 retention 过期。 */
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

  private static NqFeedbackEvent event(
      final String tenant, final String run, final Instant receivedAt) {
    return NqFeedbackEvent.create(
        tenant,
        run,
        "cand",
        "trace",
        FeedbackSource.BACKTEST,
        "BacktestCompleted",
        true,
        Map.of(),
        receivedAt,
        receivedAt);
  }

  private static NqFeedbackEnvelope envelope(final String eventId, final Instant receivedAt) {
    return NqFeedbackEnvelope.of(
        eventId,
        NqFeedbackEventType.values()[0],
        T0,
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "job-1",
        "trace",
        "req",
        "corr",
        NqFeedbackEnvelope.DEFAULT_SCHEMA_VERSION,
        "{}",
        receivedAt);
  }

  @Test
  void feedback_repository_rejects_or_bounds_overflow() {
    final InMemoryNqFeedbackEventRepository repo =
        new InMemoryNqFeedbackEventRepository(1, 1, Duration.ofSeconds(86_400), new MutableClock(T0));
    repo.append(event("tenant-a", "run-1", T0));
    repo.append(event("tenant-a", "run-1", T0.plusSeconds(1)));
    // maxEvents=1：写入第二条后仍有界，size 不超过上限。
    assertEquals(1, repo.size());
  }

  @Test
  void feedback_repository_ttl_cleanup_removes_expired_events() {
    final MutableClock clock = new MutableClock(T0);
    final InMemoryNqFeedbackEventRepository repo =
        new InMemoryNqFeedbackEventRepository(10, 10, Duration.ofSeconds(100), clock);
    repo.append(event("tenant-a", "run-1", T0)); // 将过期
    repo.append(event("tenant-a", "run-2", T0.plusSeconds(50))); // 仍有效
    // 推进到 T0+120：run-1 已过期（age 120 > 100），run-2 仍有效（age 70 < 100）。
    clock.advance(Duration.ofSeconds(120));
    final int removed = repo.cleanupExpired();
    assertEquals(1, removed);
    assertEquals(1, repo.size());
    assertTrue(repo.listByRun("tenant-a", "run-1").isEmpty());
    assertEquals(1, repo.listByRun("tenant-a", "run-2").size());
  }

  @Test
  void feedback_repository_is_per_tenant_bounded() {
    final InMemoryNqFeedbackEventRepository repo =
        new InMemoryNqFeedbackEventRepository(
            100, 1, Duration.ofSeconds(86_400), new MutableClock(T0));
    repo.append(event("tenant-a", "run-a", T0));
    repo.append(event("tenant-a", "run-a", T0.plusSeconds(1)));
    // tenant-a 受单 tenant 上限=1 约束，仅保留最新一条。
    assertEquals(1, repo.listByRun("tenant-a", "run-a").size());
    // 不同 tenant 互不影响。
    repo.append(event("tenant-b", "run-b", T0));
    assertEquals(1, repo.listByRun("tenant-b", "run-b").size());
    assertEquals(2, repo.size());
  }

  @Test
  void existing_query_semantics_preserved_sorted_by_received_at() {
    final InMemoryNqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    repo.append(event("tenant-a", "run-1", T0.plusSeconds(30)));
    repo.append(event("tenant-a", "run-1", T0.plusSeconds(10)));
    repo.append(event("tenant-a", "run-1", T0.plusSeconds(20)));
    final List<NqFeedbackEvent> events = repo.listByRun("tenant-a", "run-1");
    assertEquals(3, events.size());
    // 仍按 receivedAt 升序返回（既有语义不破坏）。
    assertEquals(T0.plusSeconds(10), events.get(0).getReceivedAt());
    assertEquals(T0.plusSeconds(20), events.get(1).getReceivedAt());
    assertEquals(T0.plusSeconds(30), events.get(2).getReceivedAt());
  }

  @Test
  void save_envelope_is_idempotent_and_bounded() {
    final InMemoryNqFeedbackEventRepository repo =
        new InMemoryNqFeedbackEventRepository(1, 1, Duration.ofSeconds(86_400), new MutableClock(T0));
    assertTrue(repo.saveEnvelope(envelope("evt-1", T0)));
    // 幂等命中：同 eventId 第二次返回 false。
    assertFalse(repo.saveEnvelope(envelope("evt-1", T0)));
    // 容量有界：写入新 envelope 后总数不超过上限。
    repo.saveEnvelope(envelope("evt-2", T0.plusSeconds(1)));
    assertEquals(1, repo.envelopeCount());
  }

  @Test
  void bad_config_fails_closed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNqFeedbackEventRepository(0, 1, Duration.ofSeconds(1), Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNqFeedbackEventRepository(1, 0, Duration.ofSeconds(1), Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNqFeedbackEventRepository(1, 1, Duration.ZERO, Clock.systemUTC()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new InMemoryNqFeedbackEventRepository(1, 1, Duration.ofSeconds(-1), Clock.systemUTC()));
  }

  @Test
  void default_constructor_is_bounded() {
    // 默认构造也必须有界（不再是无界 List/Map）。
    final InMemoryNqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    repo.append(event("tenant-a", "run-1", T0));
    assertEquals(1, repo.size());
  }
}
