package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.Map;

/**
 * Stage2-PoC-B2：PAPER_RUN_ALERT_RAISED handler。
 *
 * <p>positive 判定：ERROR / CRITICAL 视为 negative，其他为 positive（最小闭环，不做复杂分级评估）。
 */
public final class PaperRunAlertRaisedHandler extends AbstractNqFeedbackEventHandler {

  public PaperRunAlertRaisedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    super(experienceFeedbackService, feedbackEventRepository, objectMapper);
  }

  @Override
  public NqFeedbackEventType supportedType() {
    return NqFeedbackEventType.PAPER_RUN_ALERT_RAISED;
  }

  @Override
  protected FeedbackSource feedbackSource() {
    return FeedbackSource.PAPER;
  }

  @Override
  protected boolean isPositive(final Map<String, Object> payload) {
    final Object level = payload.get("alertLevel");
    if (level == null) {
      return true;
    }
    final String s = String.valueOf(level);
    return !"ERROR".equalsIgnoreCase(s) && !"CRITICAL".equalsIgnoreCase(s);
  }
}
