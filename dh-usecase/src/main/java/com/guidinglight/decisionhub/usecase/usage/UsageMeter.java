package com.guidinglight.decisionhub.usecase.usage;

public interface UsageMeter {
  void record(String tenantId, String userId, String runId, long tokens, long costMicros);
}
