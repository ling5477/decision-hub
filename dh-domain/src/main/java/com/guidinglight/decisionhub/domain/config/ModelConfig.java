package com.guidinglight.decisionhub.domain.config;

import java.util.Map;

public record ModelConfig(
    String modelKey,
    String tenantId,
    String providerType,
    String endpoint,
    String modelName,
    int timeoutMs,
    Integer maxTokens,
    Long priceInputMicros,
    Long priceOutputMicros,
    Map<String, Object> capabilities
) {}
