package com.guidinglight.decisionhub.usecase.gate;

import com.guidinglight.decisionhub.knowledge.Evidence;

import java.util.List;

public record RunContext(List<Evidence> evidences, long estimatedCostMicros, long elapsedMs) {}
