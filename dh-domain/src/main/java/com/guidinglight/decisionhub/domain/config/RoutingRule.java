package com.guidinglight.decisionhub.domain.config;

import java.util.Map;

public record RoutingRule(
    String ruleId,
    String tenantId,
    String name,
    Map<String, Object> rule
) {}
