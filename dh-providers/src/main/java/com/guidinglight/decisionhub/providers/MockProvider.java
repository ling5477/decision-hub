package com.guidinglight.decisionhub.providers;

import java.time.Duration;
import java.util.Map;

/** @deprecated Stage1-CLOSE：旧 MockProvider；不再装配进 AgentRuntimeWiringConfig。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class MockProvider implements ModelProvider {

  @Override
  public String key() {
    return "mock";
  }

  @Override
  public Capabilities capabilities() {
    return new Capabilities(false, false, 2048);
  }

  @Override
  public ModelOutput invoke(String prompt, Map<String, Object> options, Duration timeout) {
    return new ModelOutput(key(), "mock-output: " + prompt, Map.of("timeoutMs", timeout.toMillis()));
  }
}
