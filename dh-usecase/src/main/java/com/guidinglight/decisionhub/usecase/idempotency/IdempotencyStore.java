package com.guidinglight.decisionhub.usecase.idempotency;

import java.time.Duration;

public interface IdempotencyStore {
  /**
   * @return true if stored successfully; false if key already exists
   */
  boolean tryPut(String tenantId, String key, Duration ttl);
}
