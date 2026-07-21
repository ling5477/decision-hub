package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** 结构化 feedback attribution 的纯 use-case 边界。 */
public interface FeedbackAttributionService {

  /** 执行一次 deterministic、auditable、no-side-effect attribution。 */
  FeedbackAttributionResult attribute(FeedbackAttributionCommand command);
}
