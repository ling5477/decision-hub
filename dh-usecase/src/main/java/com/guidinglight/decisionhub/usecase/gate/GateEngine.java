package com.guidinglight.decisionhub.usecase.gate;

import java.util.ArrayList;
import java.util.List;

public class GateEngine {

  private final List<Gate> gates = new ArrayList<>();

  public GateEngine add(Gate gate) {
    gates.add(gate);
    return this;
  }

  public List<GateResult> evaluateAll(com.guidinglight.decisionhub.domain.run.Run run, RunContext ctx) {
    List<GateResult> out = new ArrayList<>();
    for (Gate g : gates) {
      out.add(g.evaluate(run, ctx));
    }
    return out;
  }
}
