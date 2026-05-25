package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stage2-PoC-B2：默认 router，按 {@link NqFeedbackEventType} 在 EnumMap 中查 handler。
 *
 * <p>构造时强制覆盖全部 8 个 eventType；缺一即抛 {@link IllegalStateException}， 避免线上漏注册。
 */
public final class DefaultNqFeedbackEventTypeRouter implements NqFeedbackEventTypeRouter {

  private final Map<NqFeedbackEventType, NqFeedbackEventHandler> handlersByType;

  /** 构造：传入全部 8 个 handler，顺序无关。 */
  public DefaultNqFeedbackEventTypeRouter(final List<NqFeedbackEventHandler> handlers) {
    final Map<NqFeedbackEventType, NqFeedbackEventHandler> map = new EnumMap<>(NqFeedbackEventType.class);
    for (NqFeedbackEventHandler h : handlers) {
      final NqFeedbackEventHandler existed = map.put(h.supportedType(), h);
      if (existed != null) {
        throw new IllegalStateException(
            "duplicate handler for eventType: " + h.supportedType()
                + " (" + existed.getClass().getName() + " vs " + h.getClass().getName() + ")");
      }
    }
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      if (!map.containsKey(t)) {
        throw new IllegalStateException("missing handler for eventType: " + t);
      }
    }
    this.handlersByType = Map.copyOf(map);
  }

  @Override
  public void route(final NqFeedbackEnvelope envelope) {
    final NqFeedbackEventHandler handler = handlersByType.get(envelope.getEventType());
    if (handler == null) {
      throw new IllegalStateException("no handler for eventType: " + envelope.getEventType());
    }
    handler.handle(envelope);
  }
}
