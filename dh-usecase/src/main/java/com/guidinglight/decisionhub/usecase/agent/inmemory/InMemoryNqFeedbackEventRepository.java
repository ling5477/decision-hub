package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * NqFeedbackEventRepository 的内存实现（DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2：bounded memory cap）。
 *
 * <p>Why：原实现是无界 {@code ConcurrentHashMap}/{@code ArrayList}，异常或恶意流量可让其无限增长撑爆内存
 * （P1-4 residual: memory cap 缺失）。本实现引入 retention TTL + 全局上限 + 按 tenant 上限，保证内存有界。
 *
 * <p>关键约束：
 *
 * <ol>
 *   <li><b>TTL 清理优先于容量驱逐</b>：每次写入前先按 {@code receivedAt + retention} 清理过期事件，再做容量保护。
 *   <li><b>有界保证</b>：写入后全局条目数 &lt;= {@code maxEvents}，单 tenant 事件数 &lt;= {@code
 *       perTenantMaxEvents}；超限时驱逐最老条目（按 receivedAt 升序），绝不静默无界增长。
 *   <li><b>保留原始 payload</b>：{@link NqFeedbackEnvelope} 按域不变量与项目规范（NQ feedback 必须保存原始 payload）
 *       完整保留 {@code payloadJson}；本批不剥离 payload，也不新增任何 secret / token / credential 存储。
 * </ol>
 *
 * <p>定位：dev/test 与 Stage1 辅助路径；正式持久化（JDBC）留待后续阶段。本实现带上限，dev/test 下同样不会无界增长。
 *
 * <p>线程安全：写入与清理为复合「清理-驱逐-写入」操作，统一用方法级 {@code synchronized} 串行化。
 */
public final class InMemoryNqFeedbackEventRepository
    implements NqFeedbackEventRepository, NqFeedbackIngestionUnitOfWork {

  /** 保守默认全局事件上限。 */
  public static final int DEFAULT_MAX_EVENTS = 10_000;

  /** 保守默认单 tenant 事件上限。 */
  public static final int DEFAULT_PER_TENANT_MAX_EVENTS = 10_000;

  /** 保守默认 retention（24h）。 */
  public static final Duration DEFAULT_RETENTION = Duration.ofSeconds(86_400);

  /** 仅用于排序/驱逐时为 null receivedAt 兜底（视为最老，优先驱逐）。 */
  private static final Instant OLDEST = Instant.MIN;

  private final int maxEvents;
  private final int perTenantMaxEvents;
  private final Duration retention;
  private final Clock clock;

  // 保持插入顺序，便于按 (tenant::run) 维度组织；同 run 内事件按 receivedAt 升序对外返回。
  private final Map<String, List<NqFeedbackEvent>> indexByRun = new LinkedHashMap<>();
  // eventId 唯一键幂等；LinkedHashMap 保插入序便于驱逐最老。
  private final Map<String, NqFeedbackEnvelope> envelopesByEventId = new LinkedHashMap<>();

  /** 默认构造：有界默认值。 */
  public InMemoryNqFeedbackEventRepository() {
    this(DEFAULT_MAX_EVENTS, DEFAULT_PER_TENANT_MAX_EVENTS, DEFAULT_RETENTION, Clock.systemUTC());
  }

  /**
   * 全参构造。
   *
   * @param maxEvents 全局事件上限；必须 &gt; 0。
   * @param perTenantMaxEvents 单 tenant 事件上限；必须 &gt; 0。
   * @param retention 事件保留时长；必须为正。
   * @param clock 时钟；不可为 null（测试可注入可控时钟）。
   * @throws IllegalArgumentException 配置非法（上限非正 / retention 非正）。
   */
  public InMemoryNqFeedbackEventRepository(
      final int maxEvents,
      final int perTenantMaxEvents,
      final Duration retention,
      final Clock clock) {
    if (maxEvents <= 0 || perTenantMaxEvents <= 0) {
      throw new IllegalArgumentException(
          "InMemoryNqFeedbackEventRepository caps must be > 0 (refusing unbounded in-memory store)");
    }
    Objects.requireNonNull(retention, "retention");
    if (retention.isZero() || retention.isNegative()) {
      throw new IllegalArgumentException(
          "InMemoryNqFeedbackEventRepository retention must be positive");
    }
    this.maxEvents = maxEvents;
    this.perTenantMaxEvents = perTenantMaxEvents;
    this.retention = retention;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public synchronized void append(final NqFeedbackEvent event) {
    Objects.requireNonNull(event, "event");
    final Instant now = clock.instant();
    // 1) TTL 清理优先。
    removeExpiredEvents(now);
    // 2) 单 tenant 上限：超限驱逐该 tenant 最老事件，保证写入后不超限。
    while (countByTenant(event.getTenantId()) >= perTenantMaxEvents) {
      if (!evictOldestEventOfTenant(event.getTenantId())) {
        break;
      }
    }
    // 3) 全局上限：超限驱逐全局最老事件。
    while (totalEventCount() >= maxEvents) {
      if (!evictOldestEventGlobally()) {
        break;
      }
    }
    indexByRun
        .computeIfAbsent(compositeKey(event.getTenantId(), event.getRunId()), k -> new ArrayList<>())
        .add(event);
  }

  @Override
  public synchronized List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
    final List<NqFeedbackEvent> events = indexByRun.get(compositeKey(tenantId, runId));
    if (events == null) {
      return List.of();
    }
    final List<NqFeedbackEvent> sorted = new ArrayList<>(events);
    sorted.sort(Comparator.comparing(InMemoryNqFeedbackEventRepository::receivedAtOrOldest));
    return sorted;
  }

  @Override
  public synchronized boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
    Objects.requireNonNull(envelope, "envelope");
    removeExpiredEnvelopes(clock.instant());
    // 幂等：已存在直接命中，不占新容量。
    if (envelopesByEventId.containsKey(envelope.getEventId())) {
      return false;
    }
    // 容量保护：满则驱逐最老 envelope（按 receivedAt 升序），保证有界。
    while (envelopesByEventId.size() >= maxEvents) {
      if (!evictOldestEnvelope()) {
        break;
      }
    }
    envelopesByEventId.put(envelope.getEventId(), envelope);
    return true;
  }

  @Override
  public synchronized Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
    return Optional.ofNullable(envelopesByEventId.get(eventId));
  }

  /**
   * 清理所有已过期事件与 envelope（receivedAt + retention &lt;= now）。供定时 sweep 或测试调用。
   *
   * @return 清理掉的条目总数（事件 + envelope）。
   */
  public synchronized int cleanupExpired() {
    final Instant now = clock.instant();
    return removeExpiredEvents(now) + removeExpiredEnvelopes(now);
  }

  /**
   * 当前事件总数（仅供测试断言有界性）。
   *
   * @return indexByRun 内所有 run 的事件数之和。
   */
  public synchronized int size() {
    return totalEventCount();
  }

  /**
   * 当前 envelope 数（仅供测试断言有界性）。
   *
   * @return envelopesByEventId 大小。
   */
  public synchronized int envelopeCount() {
    return envelopesByEventId.size();
  }

  /**
   * 在 repository 自身 monitor 下执行完整工作单元，并在任何 unchecked failure 时恢复全部可变状态。
   *
   * <p>两个 {@link LinkedHashMap} 的插入顺序也属于 cleanup/eviction 语义，因此 snapshot 与 restore 都保持原顺序；
   * event list 逐项复制，禁止浅复制导致 rollback 后残留 append。
   */
  @Override
  public synchronized <T> T required(final Supplier<T> action) {
    final Supplier<T> checked = Objects.requireNonNull(action, "action");
    final Map<String, NqFeedbackEnvelope> envelopeSnapshot =
        new LinkedHashMap<>(envelopesByEventId);
    final Map<String, List<NqFeedbackEvent>> eventSnapshot = snapshotEvents();
    try {
      return checked.get();
    } catch (final RuntimeException | Error failure) {
      envelopesByEventId.clear();
      envelopesByEventId.putAll(envelopeSnapshot);
      indexByRun.clear();
      eventSnapshot.forEach((key, events) -> indexByRun.put(key, new ArrayList<>(events)));
      throw failure;
    }
  }

  // ---- 内部：清理与驱逐（均在 synchronized 方法内调用）----

  private int removeExpiredEvents(final Instant now) {
    int removed = 0;
    final Iterator<Map.Entry<String, List<NqFeedbackEvent>>> runs =
        indexByRun.entrySet().iterator();
    while (runs.hasNext()) {
      final List<NqFeedbackEvent> events = runs.next().getValue();
      final Iterator<NqFeedbackEvent> it = events.iterator();
      while (it.hasNext()) {
        if (isExpired(it.next().getReceivedAt(), now)) {
          it.remove();
          removed++;
        }
      }
      if (events.isEmpty()) {
        runs.remove();
      }
    }
    return removed;
  }

  private Map<String, List<NqFeedbackEvent>> snapshotEvents() {
    final Map<String, List<NqFeedbackEvent>> snapshot = new LinkedHashMap<>();
    indexByRun.forEach((key, events) -> snapshot.put(key, new ArrayList<>(events)));
    return snapshot;
  }

  private int removeExpiredEnvelopes(final Instant now) {
    int removed = 0;
    final Iterator<Map.Entry<String, NqFeedbackEnvelope>> it =
        envelopesByEventId.entrySet().iterator();
    while (it.hasNext()) {
      if (isExpired(it.next().getValue().getReceivedAt(), now)) {
        it.remove();
        removed++;
      }
    }
    return removed;
  }

  /** receivedAt 为 null 时无法计算年龄，视为不按 TTL 过期（仍受容量驱逐约束）。 */
  private boolean isExpired(final Instant receivedAt, final Instant now) {
    return receivedAt != null && !receivedAt.plus(retention).isAfter(now);
  }

  private int totalEventCount() {
    int total = 0;
    for (final List<NqFeedbackEvent> events : indexByRun.values()) {
      total += events.size();
    }
    return total;
  }

  private int countByTenant(final String tenantId) {
    int total = 0;
    for (final List<NqFeedbackEvent> events : indexByRun.values()) {
      for (final NqFeedbackEvent e : events) {
        if (Objects.equals(e.getTenantId(), tenantId)) {
          total++;
        }
      }
    }
    return total;
  }

  /** 驱逐指定 tenant 最老事件（按 receivedAt 升序）；无可驱逐返回 false。 */
  private boolean evictOldestEventOfTenant(final String tenantId) {
    return evictOldest(e -> Objects.equals(e.getTenantId(), tenantId));
  }

  /** 驱逐全局最老事件；无可驱逐返回 false。 */
  private boolean evictOldestEventGlobally() {
    return evictOldest(e -> true);
  }

  private boolean evictOldest(final java.util.function.Predicate<NqFeedbackEvent> filter) {
    String oldestRunKey = null;
    int oldestIdx = -1;
    Instant oldestTs = null;
    for (final Map.Entry<String, List<NqFeedbackEvent>> run : indexByRun.entrySet()) {
      final List<NqFeedbackEvent> events = run.getValue();
      for (int i = 0; i < events.size(); i++) {
        final NqFeedbackEvent e = events.get(i);
        if (!filter.test(e)) {
          continue;
        }
        final Instant ts = receivedAtOrOldest(e);
        if (oldestTs == null || ts.isBefore(oldestTs)) {
          oldestTs = ts;
          oldestRunKey = run.getKey();
          oldestIdx = i;
        }
      }
    }
    if (oldestRunKey == null) {
      return false;
    }
    final List<NqFeedbackEvent> events = indexByRun.get(oldestRunKey);
    events.remove(oldestIdx);
    if (events.isEmpty()) {
      indexByRun.remove(oldestRunKey);
    }
    return true;
  }

  /** 驱逐最老 envelope（按 receivedAt 升序）；无可驱逐返回 false。 */
  private boolean evictOldestEnvelope() {
    String oldestId = null;
    Instant oldestTs = null;
    for (final Map.Entry<String, NqFeedbackEnvelope> en : envelopesByEventId.entrySet()) {
      final Instant ts =
          en.getValue().getReceivedAt() == null ? OLDEST : en.getValue().getReceivedAt();
      if (oldestTs == null || ts.isBefore(oldestTs)) {
        oldestTs = ts;
        oldestId = en.getKey();
      }
    }
    if (oldestId == null) {
      return false;
    }
    envelopesByEventId.remove(oldestId);
    return true;
  }

  private static Instant receivedAtOrOldest(final NqFeedbackEvent event) {
    return event.getReceivedAt() == null ? OLDEST : event.getReceivedAt();
  }

  private static String compositeKey(final String tenantId, final String runId) {
    return tenantId + "::" + runId;
  }
}
