package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackPolicy;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackSubject;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeObservation;
import java.util.Objects;

/**
 * 结构化归因命令。
 *
 * @param subject tenant/environment/decision/trace 绑定主体
 * @param observation 已验证的结构化结果观察
 * @param policy 确定性策略与显式评估时间
 */
public record FeedbackAttributionCommand(
    FeedbackSubject subject, OutcomeObservation observation, FeedbackPolicy policy) {

  /** 构造期拒绝缺失输入，scope 关联由服务按稳定错误码校验。 */
  public FeedbackAttributionCommand {
    subject = Objects.requireNonNull(subject, "subject");
    observation = Objects.requireNonNull(observation, "observation");
    policy = Objects.requireNonNull(policy, "policy");
  }
}
