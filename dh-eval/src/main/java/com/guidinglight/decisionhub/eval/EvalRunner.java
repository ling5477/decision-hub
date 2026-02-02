package com.guidinglight.decisionhub.eval;

public interface EvalRunner {
  EvalReport run(String suiteId);

  record EvalReport(String suiteId, int passed, int failed) {}
}
