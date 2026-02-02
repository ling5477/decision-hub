package com.guidinglight.decisionhub.usecase.audit;

import java.util.Map;

public interface AuditLogger {
  void log(String tenantId, String action, Map<String, Object> fields);
}
