package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.knowledge.Evidence;

import java.util.List;

/** @deprecated Stage1-CLOSE：旧 Gate 上下文。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public record RunContext(List<Evidence> evidences, long estimatedCostMicros, long elapsedMs) {}
