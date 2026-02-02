package com.guidinglight.decisionhub.knowledge;

import java.util.List;

public interface Retriever {
  List<Evidence> retrieve(String tenantId, String query, int k);
}
