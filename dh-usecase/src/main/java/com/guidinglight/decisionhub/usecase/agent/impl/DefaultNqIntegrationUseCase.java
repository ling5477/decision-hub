package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;

/**
 * Stage1：默认 NQ 集成入口实现。
 *
 * <p>只追加反馈事件，不触发经验、信息素或失败案例写入。
 *
 * <p>硬约束：DH 不直接下单、不绕过 NQ 风控；这里只接收事实回流。
 */
public final class DefaultNqIntegrationUseCase implements NqIntegrationUseCase {

  private final NqFeedbackEventRepository feedbackEventRepository;

  /** 构造。 */
  public DefaultNqIntegrationUseCase(final NqFeedbackEventRepository feedbackEventRepository) {
    this.feedbackEventRepository = feedbackEventRepository;
  }

  @Override
  public void onFeedback(final NqFeedbackEvent event) {
    feedbackEventRepository.append(event);
  }
}
