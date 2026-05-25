package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage1：NqBacktestClient 的 Fake 实现。
 *
 * <p>不实际访问 NQ。仅记录 submit 请求，并按提交顺序返回固定快照。
 */
public final class FakeNqBacktestClient implements NqBacktestClient {

  private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();

  @Override
  public Map<String, Object> submit(final Map<String, Object> request) {
    final String jobId = IdGenerator.newId();
    final Map<String, Object> response = new HashMap<>();
    response.put("jobId", jobId);
    response.put("status", "QUEUED");
    response.put("echoRequest", request == null ? Map.of() : Map.copyOf(request));
    final Map<String, Object> frozen = Map.copyOf(response);
    jobs.put(jobId, frozen);
    return frozen;
  }

  @Override
  public Map<String, Object> getJob(final String jobId) {
    final Map<String, Object> job = jobs.get(jobId);
    if (job == null) {
      return Map.of("jobId", jobId, "status", "UNKNOWN");
    }
    return job;
  }
}
