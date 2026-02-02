package com.guidinglight.decisionhub.domain.config;

public record RoleTemplate(
    String templateId,
    String tenantId,
    String name,
    String description,
    String promptTemplate
) {}
