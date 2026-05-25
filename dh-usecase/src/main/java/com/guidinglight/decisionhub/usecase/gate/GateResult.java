package com.guidinglight.decisionhub.usecase.gate;

import java.util.Map;

/** @deprecated Stage1-CLOSE：旧 Gate 结果记录。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public record GateResult(String gateName, GateDecision decision, String message, Map<String, Object> detail) {}
