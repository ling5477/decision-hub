package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.Map;

/**
 * Stage2-PoC-B2：PAPER_RUN_DAILY_REPORT_GENERATED handler。
 *
 * <p>positive 判定：仅依据 payload 中 {@code realizedPnl} 是否 >= 0；不做更复杂的业务推理。
 */
public final class PaperRunDailyReportGeneratedHandler extends AbstractNqFeedbackEventHandler {

  public PaperRunDailyReportGeneratedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    super(experienceFeedbackService, feedbackEventRepository, objectMapper);
  }

  @Override
  public NqFeedbackEventType supportedType() {
    return NqFeedbackEventType.PAPER_RUN_DAILY_REPORT_GENERATED;
  }

  @Override
  protected FeedbackSource feedbackSource() {
    return FeedbackSource.PAPER;
  }

  @Override
  protected boolean isPositive(final Map<String, Object> payload) {
    final Object pnl = payload.get("realizedPnl");
    if (pnl == null) {
      return true;
    }
    if (pnl instanceof Number n) {
      return n.doubleValue() >= 0.0;
    }
    return true;
  }
}
