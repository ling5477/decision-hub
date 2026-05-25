package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;

/**
 * Stage1：经验反馈强化用例。
 *
 * <p>对应工单 4.2 + 5.2：ExperienceFeedbackService。蚁群机制的工程落点：
 * 接收 NQ 回流事件后更新 ExperienceEntry 与 PheromoneEdge 的分数。
 */
public interface ExperienceFeedbackService {

  /**
   * 根据一个 NQ 反馈事件更新经验与信息素。
   *
   * @param event 反馈事件。
   */
  void apply(NqFeedbackEvent event);
}
