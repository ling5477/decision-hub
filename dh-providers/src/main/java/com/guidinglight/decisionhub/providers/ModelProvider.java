package com.guidinglight.decisionhub.providers;

import java.time.Duration;
import java.util.Map;

/** @deprecated Stage1-CLOSE：旧"多模型调用平台"接口；新链路通过 dh-connector / dh-eval 中的适配器与评分器表达模型/工具语义。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface ModelProvider {
  String key();
  Capabilities capabilities();
  ModelOutput invoke(String prompt, Map<String, Object> options, Duration timeout);

  record Capabilities(boolean supportsTools, boolean supportsJsonSchema, int maxTokensHint) {}
}
