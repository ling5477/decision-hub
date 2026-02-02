package com.guidinglight.decisionhub.memory;

import java.util.List;

public interface VectorStore {
  void upsertVector(String tenantId, String namespace, String id, float[] embedding);
  List<String> search(String tenantId, String namespace, float[] embedding, int k);
}
