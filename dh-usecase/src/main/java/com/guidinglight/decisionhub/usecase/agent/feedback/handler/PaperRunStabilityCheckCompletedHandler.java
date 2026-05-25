package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.Map;

/**
 * Stage2-PoC-B2：PAPER_RUN_STABILITY_CHECK_COMPLETED handler。
 *
 * <p>positive 判定：STABLE 为 positive，UNSTABLE 为 negative，INCONCLUSIVE 默认 positive。
 */
public final class PaperRunStabilityCheckCompletedHandler extends AbstractNqFeedbackEventHandler {

  public PaperRunStabilityCheckCompletedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    super(experienceFeedbackService, feedbackEventRepository, objectMapper);
  }

  @Override
  public NqFeedbackEventType supportedType() {
    return NqFeedbackEventType.PAPER_RUN_STABILITY_CHECK_COMPLETED;
  }

  @Override
  protected FeedbackSource feedbackSource() {
    return FeedbackSource.PAPER;
  }

  @Override
  protected boolean isPositive(final Map<String, Object> payload) {
    final Object result = payload.get("result");
    if (result == null) {
      return true;
    }
    return !"UNSTABLE".equalsIgnoreCase(String.valueOf(result));
  }
}
