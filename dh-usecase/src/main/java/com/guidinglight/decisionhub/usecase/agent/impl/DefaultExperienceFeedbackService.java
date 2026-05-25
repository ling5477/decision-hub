package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.experience.ExperienceEntry;
import com.guidinglight.decisionhub.domain.experience.PheromoneEdge;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.memory.agent.ExperienceStore;
import com.guidinglight.decisionhub.memory.agent.FailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.PheromoneStore;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage1：默认经验反馈服务。
 *
 * <p>蚁群轻量实现：根据事件 source/positive 不同，对 ExperienceEntry/PheromoneEdge 做正向增强或负向衰减； 风控拒绝时叠加重罚 +
 * 写入 FailureCaseStore。
 */
public final class DefaultExperienceFeedbackService implements ExperienceFeedbackService {

  private static final double REINFORCE_DELTA = 1.0;
  private static final double DECAY_DELTA = 0.5;
  private static final double RISK_REJECT_PENALTY = 2.0;

  private final ExperienceStore experienceStore;
  private final PheromoneStore pheromoneStore;
  private final FailureCaseStore failureCaseStore;

  /** 构造。 */
  public DefaultExperienceFeedbackService(
      final ExperienceStore experienceStore,
      final PheromoneStore pheromoneStore,
      final FailureCaseStore failureCaseStore) {
    this.experienceStore = experienceStore;
    this.pheromoneStore = pheromoneStore;
    this.failureCaseStore = failureCaseStore;
  }

  @Override
  public void apply(final NqFeedbackEvent event) {
    final String experienceKey = buildExperienceKey(event);
    final ExperienceEntry entry =
        experienceStore
            .findByKey(event.getTenantId(), experienceKey)
            .orElseGet(
                () ->
                    ExperienceEntry.create(
                        event.getTenantId(),
                        event.getTraceId(),
                        experienceKey,
                        derivePattern(event),
                        deriveRegime(event),
                        deriveDataSource(event),
                        deriveAgentRole(event),
                        Map.of("seed", true),
                        TimeProvider.now()));

    if (event.isPositive()) {
      entry.reinforce(REINFORCE_DELTA, TimeProvider.now());
    } else {
      final double penalty =
          event.getSource() == FeedbackSource.RISK ? RISK_REJECT_PENALTY : DECAY_DELTA;
      entry.penalize(penalty, TimeProvider.now());
      final Map<String, Object> failure = new HashMap<>();
      failure.put("eventType", event.getEventType());
      failure.put("source", event.getSource().name());
      failure.put("candidateId", event.getCandidateId());
      failure.put("nqPayload", event.getPayloadJson());
      failureCaseStore.record(
          event.getTenantId(),
          event.getTraceId(),
          event.getRunId(),
          event.getSource().name() + "_REJECTED",
          Map.copyOf(failure),
          TimeProvider.now());
    }
    experienceStore.save(entry);

    updatePheromone(event);
  }

  private void updatePheromone(final NqFeedbackEvent event) {
    final String fromNode = deriveRegime(event);
    final String toNode = derivePattern(event);
    final PheromoneEdge edge =
        pheromoneStore
            .find(event.getTenantId(), fromNode, toNode)
            .orElseGet(
                () ->
                    PheromoneEdge.create(
                        event.getTenantId(), fromNode, toNode, Map.of(), TimeProvider.now()));
    if (event.isPositive()) {
      edge.reinforce(REINFORCE_DELTA, TimeProvider.now());
    } else {
      edge.decay(
          event.getSource() == FeedbackSource.RISK ? RISK_REJECT_PENALTY : DECAY_DELTA,
          TimeProvider.now());
    }
    pheromoneStore.save(edge);
  }

  private static String buildExperienceKey(final NqFeedbackEvent event) {
    return derivePattern(event)
        + "::"
        + deriveRegime(event)
        + "::"
        + deriveDataSource(event)
        + "::"
        + deriveAgentRole(event);
  }

  private static String derivePattern(final NqFeedbackEvent event) {
    final Object pattern = event.getPayloadJson().get("strategyPattern");
    return pattern == null ? "unknown-pattern" : String.valueOf(pattern);
  }

  private static String deriveRegime(final NqFeedbackEvent event) {
    final Object regime = event.getPayloadJson().get("marketRegime");
    return regime == null ? "unknown-regime" : String.valueOf(regime);
  }

  private static String deriveDataSource(final NqFeedbackEvent event) {
    final Object ds = event.getPayloadJson().get("dataSource");
    return ds == null ? "unknown-source" : String.valueOf(ds);
  }

  private static String deriveAgentRole(final NqFeedbackEvent event) {
    final Object role = event.getPayloadJson().get("agentRole");
    return role == null ? "STRATEGY" : String.valueOf(role);
  }
}
