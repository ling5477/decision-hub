package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;

/** Stage2-PoC-B2：PAPER_RUN_CREATED handler，最小闭环只做经验登记，不做交易推理。 */
public final class PaperRunCreatedHandler extends AbstractNqFeedbackEventHandler {

  public PaperRunCreatedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    super(experienceFeedbackService, feedbackEventRepository, objectMapper);
  }

  @Override
  public NqFeedbackEventType supportedType() {
    return NqFeedbackEventType.PAPER_RUN_CREATED;
  }

  @Override
  protected FeedbackSource feedbackSource() {
    return FeedbackSource.PAPER;
  }
}
