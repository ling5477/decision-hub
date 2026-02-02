package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.domain.run.Run;

import java.util.Map;

public class CostBudgetGate implements Gate {

  private final long budgetMicros;

  public CostBudgetGate(long budgetMicros) {
    this.budgetMicros = budgetMicros;
  }

  @Override
  public String name() {
    return "CostBudget";
  }

  @Override
  public GateResult evaluate(Run run, RunContext ctx) {
    long used = ctx == null ? 0 : ctx.estimatedCostMicros();
    if (used > budgetMicros) {
      return new GateResult(name(), GateDecision.FAIL, "cost exceeds budget", Map.of("usedMicros", used, "budgetMicros", budgetMicros));
    }
    return new GateResult(name(), GateDecision.PASS, "ok", Map.of("usedMicros", used, "budgetMicros", budgetMicros));
  }
}
