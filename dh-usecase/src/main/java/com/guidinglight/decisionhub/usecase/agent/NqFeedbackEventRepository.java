package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import java.util.List;

/** Stage1：NqFeedbackEvent 持久化端口。 */
public interface NqFeedbackEventRepository {

  /** Append-only 写入。 */
  void append(NqFeedbackEvent event);

  /** 按 runId 列出已接收事件（按 receivedAt 升序）。 */
  List<NqFeedbackEvent> listByRun(String tenantId, String runId);
}
