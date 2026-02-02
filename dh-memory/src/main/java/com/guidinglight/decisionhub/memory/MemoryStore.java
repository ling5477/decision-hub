package com.guidinglight.decisionhub.memory;

import java.util.List;

public interface MemoryStore {
  void upsert(MemoryRecord record);
  List<MemoryRecord> query(String tenantId, String namespace, String query, int k);
}
