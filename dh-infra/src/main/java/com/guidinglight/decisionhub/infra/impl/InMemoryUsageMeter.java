package com.guidinglight.decisionhub.infra.impl;

import com.guidinglight.decisionhub.usecase.usage.UsageMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryUsageMeter implements UsageMeter {

  private static final Logger log = LoggerFactory.getLogger(InMemoryUsageMeter.class);

  @Override
  public void record(String tenantId, String userId, String runId, long tokens, long costMicros) {
    log.info("usage tenant={} user={} run={} tokens={} costMicros={}", tenantId, userId, runId, tokens, costMicros);
  }
}
