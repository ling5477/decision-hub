package com.guidinglight.decisionhub.infra.impl;

import com.guidinglight.decisionhub.usecase.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Slf4jAuditLogger implements AuditLogger {

  private static final Logger log = LoggerFactory.getLogger(Slf4jAuditLogger.class);

  @Override
  public void log(String tenantId, String action, Map<String, Object> fields) {
    log.info("audit tenant={} action={} fields={}", tenantId, action, fields);
  }
}
