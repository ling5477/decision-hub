package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.Map;

/**
 * Stage2-PoC-B2：BACKTEST_RESULT_READY handler。
 *
 * <p>positive 判定：verdict=PASS 为 positive；FAIL 为 negative；MARGINAL 默认 positive（不在 handler 里做评分推理）。
 */
public final class BacktestResultReadyHandler extends AbstractNqFeedbackEventHandler {

  public BacktestResultReadyHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    super(experienceFeedbackService, feedbackEventRepository, objectMapper);
  }

  @Override
  public NqFeedbackEventType supportedType() {
    return NqFeedbackEventType.BACKTEST_RESULT_READY;
  }

  @Override
  protected FeedbackSource feedbackSource() {
    return FeedbackSource.BACKTEST;
  }

  @Override
  protected boolean isPositive(final Map<String, Object> payload) {
    final Object verdict = payload.get("verdict");
    if (verdict == null) {
      return true;
    }
    return !"FAIL".equalsIgnoreCase(String.valueOf(verdict));
  }
}
