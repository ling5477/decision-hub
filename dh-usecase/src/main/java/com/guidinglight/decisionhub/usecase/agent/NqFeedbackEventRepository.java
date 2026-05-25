package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
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
}
