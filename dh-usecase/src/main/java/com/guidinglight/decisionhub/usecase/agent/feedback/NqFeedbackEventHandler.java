package com.guidinglight.decisionhub.usecase.agent.feedback;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;

/**
 * Stage2-PoC-B2：单个 eventType 的 handler 端口。
 *
 * <p>每个实现类 SHALL：
 *
 * <ol>
 *   <li>实现 {@link #supportedType()} 表明它处理哪个 {@link NqFeedbackEventType}。
 *   <li>实现 {@link #handle(NqFeedbackEnvelope)} 完成"分类接收 + 触发已有经验反馈服务的最小闭环"； 禁止在 handler 中写复杂业务推理或交易语义。
 * </ol>
 *
 * <p>持久化 envelope 由 {@code NqFeedbackIngestionService} 在派发之前统一处理；handler 不应直接 save envelope。
 */
public interface NqFeedbackEventHandler {

  /** 本 handler 处理的 eventType。 */
  NqFeedbackEventType supportedType();

  /** 处理事件；不要抛业务异常，处理失败应通过日志 + handler 自身降级，不要中断 ingestion。 */
  void handle(NqFeedbackEnvelope envelope);
}
