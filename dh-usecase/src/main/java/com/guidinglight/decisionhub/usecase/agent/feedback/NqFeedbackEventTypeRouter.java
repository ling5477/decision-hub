package com.guidinglight.decisionhub.usecase.agent.feedback;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;

/**
 * Stage2-PoC-B2：按 {@code NqFeedbackEventType} 路由到 {@link NqFeedbackEventHandler}。
 *
 * <p>Router 必须维护 8 种 eventType 与 handler 的全映射；未注册的 eventType 视为编程错误并抛 {@link
 * IllegalStateException}。
 */
public interface NqFeedbackEventTypeRouter {

  /** 按 envelope.eventType 路由到对应 handler；找不到 handler 抛异常。 */
  void route(NqFeedbackEnvelope envelope);
}
