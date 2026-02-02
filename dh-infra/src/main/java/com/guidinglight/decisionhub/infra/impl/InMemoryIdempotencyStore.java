package com.guidinglight.decisionhub.infra.impl;

import com.guidinglight.decisionhub.usecase.idempotency.IdempotencyStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

  private final Map<String, Long> map = new ConcurrentHashMap<>();

  @Override
  public boolean tryPut(String tenantId, String key, Duration ttl) {
    String k = tenantId + ":" + key;
    long now = System.currentTimeMillis();
    long exp = now + ttl.toMillis();
    map.entrySet().removeIf(e -> e.getValue() < now);
    return map.putIfAbsent(k, exp) == null;
  }
}
