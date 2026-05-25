package com.guidinglight.decisionhub.connector.nq;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;

/**
 * Stage1：NQ 事实回流客户端接口。
 *
 * <p>对应工单 4.5：NqFeedbackClient。
 * 接收来自 NQ 的事实事件（BacktestCompleted、RiskRejected 等）并转交给 DH 经验反馈链路。
 */
public interface NqFeedbackClient {

  /**
   * 接收一个 NQ 回流事件。
   *
   * @param event 结构化事件。
   */
  void receive(NqFeedbackEvent event);
}
