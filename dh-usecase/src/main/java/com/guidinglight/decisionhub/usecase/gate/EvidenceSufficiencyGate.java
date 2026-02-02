package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.domain.run.Run;

import java.util.Map;

public class EvidenceSufficiencyGate implements Gate {

  @Override
  public String name() {
    return "EvidenceSufficiency";
  }

  @Override
  public GateResult evaluate(Run run, RunContext ctx) {
    int n = ctx == null || ctx.evidences() == null ? 0 : ctx.evidences().size();
    if (n < 2) {
      return new GateResult(name(), GateDecision.FAIL, "evidence_count < 2", Map.of("evidenceCount", n));
    }
    return new GateResult(name(), GateDecision.PASS, "ok", Map.of("evidenceCount", n));
  }
}
