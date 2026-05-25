package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;

/**
 * Stage1：默认 NQ 集成入口实现。
 *
 * <p>1. 持久化反馈事件 2. 触发经验反馈强化
 *
 * <p>硬约束：DH 不直接下单、不绕过 NQ 风控；这里只接收事实回流。
 */
public final class DefaultNqIntegrationUseCase implements NqIntegrationUseCase {

  private final NqFeedbackEventRepository feedbackEventRepository;
  private final ExperienceFeedbackService experienceFeedbackService;

  /** 构造。 */
  public DefaultNqIntegrationUseCase(
      final NqFeedbackEventRepository feedbackEventRepository,
      final ExperienceFeedbackService experienceFeedbackService) {
    this.feedbackEventRepository = feedbackEventRepository;
    this.experienceFeedbackService = experienceFeedbackService;
  }

  @Override
  public void onFeedback(final NqFeedbackEvent event) {
    feedbackEventRepository.append(event);
    experienceFeedbackService.apply(event);
  }
}
