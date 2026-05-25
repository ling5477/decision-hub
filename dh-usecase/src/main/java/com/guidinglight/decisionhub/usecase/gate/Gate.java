package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.domain.run.Run;

/** @deprecated Stage1-CLOSE：旧多模型平台 Gate 抽象。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface Gate {
  String name();
  GateResult evaluate(Run run, RunContext ctx);
}
