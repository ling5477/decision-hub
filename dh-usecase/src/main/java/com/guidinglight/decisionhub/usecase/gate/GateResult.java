package com.guidinglight.decisionhub.usecase.gate;

import java.util.Map;

public record GateResult(String gateName, GateDecision decision, String message, Map<String, Object> detail) {}
