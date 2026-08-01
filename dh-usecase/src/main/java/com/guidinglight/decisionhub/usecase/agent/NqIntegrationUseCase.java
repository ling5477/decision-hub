package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;

/**
 * Stage1：NQ 集成入口用例。
 *
 * <p>兼容性入口只承担接收并追加反馈事件。经验学习必须由未来独立授权的显式 workflow 触发，不能从该入口隐式发生。
 *
 * <p>硬约束：DH 不直接下单、不绕过风控；本用例只接收事实回流，不产生交易。
 */
public interface NqIntegrationUseCase {

  /**
   * 接收并追加一条 NQ 反馈事件，不触发 mutable learning store。
   *
   * @param event 反馈事件。
   */
  void onFeedback(NqFeedbackEvent event);
}
