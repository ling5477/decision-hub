package com.guidinglight.decisionhub.api.legacy.run;

import com.guidinglight.decisionhub.domain.run.RunStatus;
import java.time.Instant;
import java.util.Map;

/**
 * @deprecated Stage1-CLOSE：旧多模型平台响应视图；新链路使用
 *     {@link com.guidinglight.decisionhub.api.research.ResearchRunView}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
@SuppressWarnings("deprecation")
public record RunView(
    String runId,
    String tenantId,
    RunStatus status,
    String question,
    Map<String, Object> configSnapshot,
    Instant createdAt,
    Instant updatedAt) {}
