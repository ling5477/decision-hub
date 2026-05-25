package com.guidinglight.decisionhub.eval.agent.rule;

import com.guidinglight.decisionhub.eval.agent.BacktestResultScorer;
import java.util.HashMap;
import java.util.Map;

/** Stage1：默认回测结果评分器。规则：snapshot.sharpe 直接归一化到 [0,1]（除以 3 截断）。 */
public final class DefaultBacktestResultScorer implements BacktestResultScorer {

  @Override
  public Map<String, Object> score(final Map<String, Object> backtestSnapshot) {
    double backtestScore = 0.0;
    if (backtestSnapshot != null) {
      final Object sharpe = backtestSnapshot.get("sharpe");
      if (sharpe instanceof Number n) {
        backtestScore = n.doubleValue() / 3.0;
      }
    }
    if (backtestScore < 0.0) {
      backtestScore = 0.0;
    }
    if (backtestScore > 1.0) {
      backtestScore = 1.0;
    }

    final Map<String, Object> result = new HashMap<>();
    result.put("backtestScore", backtestScore);
    result.put("source", "DefaultBacktestResultScorer");
    return Map.copyOf(result);
  }
}
