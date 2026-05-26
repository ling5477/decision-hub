package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage1 落地 + Stage3-B3 扩展：{@link NqBacktestClient} 的 Fake 实现。
 *
 * <p>Stage3-B3 行为契约（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §7.1）：
 *
 * <ul>
 *   <li>不发任何 HTTP；不持有 HTTP 客户端；不消耗外部资源；
 *   <li>typed submit 返回 deterministic jobId = "fake-job-" + sha256(requestId).take(16)；
 *   <li>同 requestId 重复 submit 返回相同 jobId（DH 端可观察到一致性，便于幂等测试）；
 *   <li>acceptedAt 由可注入 {@link Clock} 决定，默认 system UTC；
 *   <li>不模拟 BACKTEST_RESULT_READY；Fake 不能伪造正式回测结果。
 * </ul>
 */
public final class FakeNqBacktestClient implements NqBacktestClient {

  private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();
  private final Clock clock;

  /** 默认构造：使用 UTC system clock。 */
  public FakeNqBacktestClient() {
    this(Clock.systemUTC());
  }

  /** 测试可注入 Clock 以确保 acceptedAt deterministic。 */
  public FakeNqBacktestClient(final Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

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

  /**
   * Stage3-B3 typed submit：deterministic 实现。
   *
   * <p>不发 HTTP；不依赖外部资源；同 requestId 返回相同 jobId。
   */
  @Override
  public NqBacktestSubmitResult submit(final DhBacktestRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }
    final String jobId = deterministicJobId(request.getRequestId());
    final Instant acceptedAt = clock.instant();
    final Map<String, Object> snapshot = new HashMap<>();
    snapshot.put("jobId", jobId);
    snapshot.put("status", "QUEUED");
    snapshot.put("requestId", request.getRequestId());
    snapshot.put("candidateId", request.getCandidateId());
    snapshot.put("acceptedAt", acceptedAt.toString());
    jobs.put(jobId, Map.copyOf(snapshot));
    return NqBacktestSubmitResult.accepted(request.getRequestId(), jobId, acceptedAt);
  }

  private static String deterministicJobId(final String requestId) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] hash = md.digest(requestId.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(16);
      for (int i = 0; i < 8; i++) {
        sb.append(String.format("%02x", hash[i]));
      }
      return "fake-job-" + sb;
    } catch (final NoSuchAlgorithmException e) {
      // SHA-256 在 JRE 中必定存在；理论上不会触发。仍兜底回退到 requestId 后 16 位。
      final String safe = requestId.length() <= 16 ? requestId : requestId.substring(0, 16);
      return "fake-job-" + safe;
    }
  }
}
