package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.domain.run.Run;

public interface Gate {
  String name();
  GateResult evaluate(Run run, RunContext ctx);
}
