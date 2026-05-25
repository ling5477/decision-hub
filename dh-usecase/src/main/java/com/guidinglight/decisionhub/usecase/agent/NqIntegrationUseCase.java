package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;

/**
 * Stage1：NQ 集成入口用例。
 *
 * <p>对应工单 4.2：NqIntegrationUseCase。Stage1 只承担：接收事件 -> 持久化 -> 触发经验反馈。
 *
 * <p>硬约束：DH 不直接下单、不绕过风控；本用例只接收事实回流，不产生交易。
 */
public interface NqIntegrationUseCase {

  /**
   * 接收一条 NQ 反馈事件。
   *
   * @param event 反馈事件。
   */
  void onFeedback(NqFeedbackEvent event);
}
