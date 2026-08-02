package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import java.util.List;
import java.util.Optional;

/**
 * NqFeedback 持久化端口。
 *
 * <p>Stage1：{@link #append(NqFeedbackEvent)} / {@link #listByRun(String, String)} 承载 Stage1 经验事件。
 *
 * <p>Stage2-PoC-B2 新增：{@link #saveEnvelope(NqFeedbackEnvelope)} / {@link
 * #findEnvelopeByEventId(String)} 承载 Stage2 正式 envelope 与 eventId 唯一键幂等。
 */
public interface NqFeedbackEventRepository {

  /** Stage1：Append-only 写入 Stage1 经验事件。 */
  void append(NqFeedbackEvent event);

  /** Stage1：按 runId 列出已接收事件（按 receivedAt 升序）。 */
  List<NqFeedbackEvent> listByRun(String tenantId, String runId);

  /**
   * Stage2-PoC-B2：保存 Stage2 envelope，eventId 唯一。
   *
   * @return {@code true} 表示首次写入；{@code false} 表示已存在（幂等命中）。
   */
  boolean saveEnvelope(NqFeedbackEnvelope envelope);

  /** Stage2-PoC-B2：按 eventId 查找已落库 envelope。 */
  Optional<NqFeedbackEnvelope> findEnvelopeByEventId(String eventId);

  /**
   * 在同线程 router/handler append 前绑定当前 envelope；实现必须在 {@link #endEventCorrelation()} 后清理上下文。
   *
   * <p>Why：legacy envelope 与 routed event 共表但物理行不同，完成态只能依赖持久化的 envelope eventId 精确关联，
   * 不能用 payload、时间戳或 event type 内容元组替代。
   */
  default void beginEventCorrelation(
      final NqFeedbackEnvelope envelope, final String tenantId) {
    throw new UnsupportedOperationException("exact feedback event correlation is not supported");
  }

  /** 清理当前线程的 envelope-event 精确关联上下文。 */
  default void endEventCorrelation() {}

  /**
   * 查询 envelope 与其 routed event 的物理持久化状态。
   *
   * <p>查询必须按 envelope eventId 精确关联，并校验 tenant、source、trace 与 event type；禁止内容元组匹配。
   */
  default FeedbackIngestionPersistence findIngestionPersistence(
      final String eventId,
      final String tenantId,
      final String traceId,
      final NqFeedbackEventType eventType) {
    throw new UnsupportedOperationException("exact feedback event correlation is not supported");
  }

  /** 精确关联的物理状态；ENVELOPE_CONFLICT 由 use-case 在 canonical envelope 比较后判定。 */
  enum FeedbackIngestionPersistenceState {
    ABSENT,
    COMPLETE_MATCH,
    ENVELOPE_ONLY,
    EVENT_ONLY,
    EVENT_CONFLICT,
    AMBIGUOUS_CORRELATION
  }

  /** 物理状态与已存在 envelope；查询结果不可用内容等价替代精确 event 关联。 */
  record FeedbackIngestionPersistence(
      FeedbackIngestionPersistenceState state, Optional<NqFeedbackEnvelope> envelope) {

    public FeedbackIngestionPersistence {
      java.util.Objects.requireNonNull(state, "state");
      java.util.Objects.requireNonNull(envelope, "envelope");
    }
  }
}
