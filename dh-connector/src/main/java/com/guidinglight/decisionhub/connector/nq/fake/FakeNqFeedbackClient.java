package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.connector.nq.NqFeedbackClient;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage1：NqFeedbackClient 的 Fake 实现。
 *
 * <p>把收到的事件缓存到内存列表，便于调试与单元测试。生产实现应转发到 ExperienceFeedbackService。
 */
public final class FakeNqFeedbackClient implements NqFeedbackClient {

  private final List<NqFeedbackEvent> events = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void receive(final NqFeedbackEvent event) {
    events.add(event);
  }

  /** Stage1 调试：返回收到事件的不可变快照。 */
  public List<NqFeedbackEvent> snapshot() {
    synchronized (events) {
      return List.copyOf(events);
    }
  }
}
