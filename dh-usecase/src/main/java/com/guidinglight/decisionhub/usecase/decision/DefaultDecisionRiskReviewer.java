package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import java.util.List;
import java.util.Locale;

/**
 * K2 默认 deterministic risk reviewer。
 *
 * <p>规则极小化：无 evidence、无 signal 或 provider 失败均 UNKNOWN；reason / evidence 中出现 HIGH_RISK 时返回 HIGH；
 * 其他 mock-only 场景返回 LOW。该实现不替代 NQ 风控，也不产生交易授权。
 */
public final class DefaultDecisionRiskReviewer implements DecisionRiskReviewer {

  /**
   * 基于本地只读信息执行风险审查。
   *
   * @param context 编排上下文
   * @param signal mock provider signal
   * @return deterministic risk review
   */
  @Override
  public DecisionRiskReview review(final DecisionContext context, final DecisionSignalResult signal) {
    if (context == null || !context.hasEvidence()) {
      return new DecisionRiskReview(DecisionRiskLevel.UNKNOWN, List.of("NO_EVIDENCE"));
    }
    if (signal == null || signal.requiresAbstain()) {
      return new DecisionRiskReview(DecisionRiskLevel.UNKNOWN, List.of("PROVIDER_UNAVAILABLE"));
    }
    if (containsHighRisk(context.evidenceRefs()) || containsHighRisk(signal.reasonCodes())) {
      return new DecisionRiskReview(DecisionRiskLevel.HIGH, List.of("HIGH_RISK"));
    }
    return new DecisionRiskReview(DecisionRiskLevel.LOW, List.of("RISK_OK"));
  }

  private static boolean containsHighRisk(final List<String> values) {
    return values != null
        && values.stream()
            .filter(value -> value != null)
            .map(value -> value.toUpperCase(Locale.ROOT))
            .anyMatch(value -> value.contains("HIGH_RISK"));
  }
}
