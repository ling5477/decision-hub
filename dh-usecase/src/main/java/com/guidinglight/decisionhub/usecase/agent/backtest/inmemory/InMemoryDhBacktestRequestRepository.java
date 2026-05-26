package com.guidinglight.decisionhub.usecase.agent.backtest.inmemory;

import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage3-B3：{@link DhBacktestRequestRepository} 的内存实现。
 *
 * <p>默认装配；JDBC 替换属 Stage3-B4 / Stage3-FREEZE 后续，本 Batch 不涉及。
 *
 * <p>特性：
 *
 * <ul>
 *   <li>{@code ConcurrentHashMap} 保证多线程访问安全；
 *   <li>幂等短路键 = (candidateId + "::" + paramsHash)；
 *   <li>{@code save} 拒绝重复 requestId，避免误覆盖；
 *   <li>{@code updateAfterSubmit} 对不存在的 requestId 抛出 IllegalStateException（service 层应保证调用顺序）。
 * </ul>
 */
public final class InMemoryDhBacktestRequestRepository implements DhBacktestRequestRepository {

  private static final Duration IDEMPOTENCY_WINDOW = Duration.ofHours(24);

  /** key=requestId */
  private final Map<String, RequestSnapshot> byRequestId = new ConcurrentHashMap<>();

  /** key=(candidateId + "::" + paramsHash) -&gt; 最新 requestId（用于 24h 短路） */
  private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();

  @Override
  public void save(final DhBacktestRequest request) {
    final String requestId = request.getRequestId();
    final String paramsHash = computeParamsHashStub(request);
    final RequestSnapshot snapshot =
        new RequestSnapshot(
            requestId,
            request.getCandidateId(),
            paramsHash,
            request.getTraceId(),
            request.getStatus(),
            null,
            Instant.now(),
            null,
            null,
            null);
    final RequestSnapshot prior = byRequestId.putIfAbsent(requestId, snapshot);
    if (prior != null) {
      throw new IllegalStateException("duplicate requestId in repository: " + requestId);
    }
    idempotencyIndex.put(idempotencyKey(request.getCandidateId(), paramsHash), requestId);
  }

  @Override
  public void updateAfterSubmit(
      final String requestId,
      final DhBacktestRequestStatus newStatus,
      final String jobId,
      final Instant acceptedAt,
      final String errorCode,
      final String errorMessage) {
    byRequestId.compute(
        requestId,
        (k, existing) -> {
          if (existing == null) {
            throw new IllegalStateException(
                "updateAfterSubmit called for unknown requestId: " + requestId);
          }
          return new RequestSnapshot(
              existing.getRequestId(),
              existing.getCandidateId(),
              existing.getParamsHash(),
              existing.getTraceId(),
              newStatus,
              jobId != null ? jobId : existing.getJobId(),
              existing.getSentAt(),
              acceptedAt != null ? acceptedAt : existing.getAcceptedAt(),
              errorCode != null ? errorCode : existing.getErrorCode(),
              errorMessage != null ? errorMessage : existing.getErrorMessage());
        });
  }

  @Override
  public Optional<RequestSnapshot> findByRequestId(final String requestId) {
    return Optional.ofNullable(byRequestId.get(requestId));
  }

  @Override
  public Optional<RequestSnapshot> findByCandidateAndParamsHashWithin24h(
      final String candidateId, final String paramsHash, final Instant now) {
    final String requestId = idempotencyIndex.get(idempotencyKey(candidateId, paramsHash));
    if (requestId == null) {
      return Optional.empty();
    }
    final RequestSnapshot snapshot = byRequestId.get(requestId);
    if (snapshot == null) {
      return Optional.empty();
    }
    if (snapshot.getSentAt() == null
        || now.isAfter(snapshot.getSentAt().plus(IDEMPOTENCY_WINDOW))) {
      return Optional.empty();
    }
    return Optional.of(snapshot);
  }

  /**
   * Stage3-B3 仅在 service 层计算 paramsHash 并通过 {@link #saveWithParamsHash} 注入；
   * 此 stub 是兜底，service 不应到这里。
   *
   * <p>保持 stub 是为了让 {@link #save(DhBacktestRequest)} 维持端口签名整洁；
   * service 实际走 {@link #saveWithParamsHash} 重载。
   */
  private static String computeParamsHashStub(final DhBacktestRequest request) {
    // Stub: 仅当 service 直接调用 save(request) 时使用（不推荐）；
    // 真正 paramsHash 由 DefaultDhBacktestRequestService.computeParamsHash 计算。
    return Integer.toHexString(java.util.Objects.hash(
        request.getCandidateId(),
        request.getStrategyVersion(),
        request.getStrategyParametersJson(),
        request.getStartDate(),
        request.getEndDate(),
        request.getInitialCapital(),
        request.getSymbols(),
        request.getFrequency()));
  }

  /** Service 端的扩展入口：携带 service 已计算的 paramsHash。 */
  public void saveWithParamsHash(final DhBacktestRequest request, final String paramsHash) {
    java.util.Objects.requireNonNull(paramsHash, "paramsHash");
    final String requestId = request.getRequestId();
    final RequestSnapshot snapshot =
        new RequestSnapshot(
            requestId,
            request.getCandidateId(),
            paramsHash,
            request.getTraceId(),
            request.getStatus(),
            null,
            Instant.now(),
            null,
            null,
            null);
    final RequestSnapshot prior = byRequestId.putIfAbsent(requestId, snapshot);
    if (prior != null) {
      throw new IllegalStateException("duplicate requestId in repository: " + requestId);
    }
    idempotencyIndex.put(idempotencyKey(request.getCandidateId(), paramsHash), requestId);
  }

  private static String idempotencyKey(final String candidateId, final String paramsHash) {
    return candidateId + "::" + paramsHash;
  }
}
