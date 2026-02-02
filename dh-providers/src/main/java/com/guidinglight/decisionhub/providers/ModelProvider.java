package com.guidinglight.decisionhub.providers;

import java.time.Duration;
import java.util.Map;

public interface ModelProvider {
  String key();
  Capabilities capabilities();
  ModelOutput invoke(String prompt, Map<String, Object> options, Duration timeout);

  record Capabilities(boolean supportsTools, boolean supportsJsonSchema, int maxTokensHint) {}
}
