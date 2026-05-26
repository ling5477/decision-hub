package com.guidinglight.decisionhub.usecase.agent.backtest;

import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import java.time.Instant;
import java.util.Optional;

/**
 * Stage3-B3：DH 端 backtest request 出站持久化端口。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.1 / §8.1。
 *
 * <p>职责：
 *
 * <ul>
 *   <li>持久化 {@link DhBacktestRequest} + NQ 返回的 jobId 映射；
 *   <li>支持 24h paramsHash 幂等短路查询：
 *       同 (candidateId + paramsHash) 在 24h 内重复 -&gt; 返回原 requestId；
 *   <li>支持 NQ feedback 命中后状态机推进（ACCEPTED -&gt; RESULT_READY / FAILED 等）；
 *   <li>支持按 requestId 反查。
 * </ul>
 *
 * <p>本端口与底层存储（InMemory / JDBC）解耦；Stage3-B3 默认 InMemory。
 * JDBC 替换属 Stage3-B4 / Stage3-FREEZE 后续，本 Batch 不涉及。
 */
public interface DhBacktestRequestRepository {

  /** 首次保存：persist DhBacktestRequest（不允许覆盖已存在的 requestId）。 */
  void save(DhBacktestRequest request);

  /** 状态机推进：NQ 同步响应后或 feedback 命中后更新 status / jobId / acceptedAt / errorCode。 */
  void updateAfterSubmit(
      String requestId,
      DhBacktestRequestStatus newStatus,
      String jobId,
      Instant acceptedAt,
      String errorCode,
      String errorMessage);

  /** 按 requestId 反查（含状态与 jobId 等持久化字段）。 */
  Optional<RequestSnapshot> findByRequestId(String requestId);

  /** 24h 短路查找：同 candidateId + paramsHash 且 sentAt 在 24h 窗口内。 */
  Optional<RequestSnapshot> findByCandidateAndParamsHashWithin24h(
      String candidateId, String paramsHash, Instant now);

  /**
   * 仓储行快照。本类作为端口边界 DTO，不暴露底层存储字段。
   *
   * <p>注意：jobId / acceptedAt / errorCode / errorMessage 在 {@code save} 时可能为 null；
   * 仅在 {@link #updateAfterSubmit} 之后才被填充。
   */
  final class RequestSnapshot {
    private final String requestId;
    private final String candidateId;
    private final String paramsHash;
    private final String traceId;
    private final DhBacktestRequestStatus status;
    private final String jobId;
    private final Instant sentAt;
    private final Instant acceptedAt;
    private final String errorCode;
    private final String errorMessage;

    public RequestSnapshot(
        final String requestId,
        final String candidateId,
        final String paramsHash,
        final String traceId,
        final DhBacktestRequestStatus status,
        final String jobId,
        final Instant sentAt,
        final Instant acceptedAt,
        final String errorCode,
        final String errorMessage) {
      this.requestId = requestId;
      this.candidateId = candidateId;
      this.paramsHash = paramsHash;
      this.traceId = traceId;
      this.status = status;
      this.jobId = jobId;
      this.sentAt = sentAt;
      this.acceptedAt = acceptedAt;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }

    public String getRequestId() {
      return requestId;
    }

    public String getCandidateId() {
      return candidateId;
    }

    public String getParamsHash() {
      return paramsHash;
    }

    public String getTraceId() {
      return traceId;
    }

    public DhBacktestRequestStatus getStatus() {
      return status;
    }

    public String getJobId() {
      return jobId;
    }

    public Instant getSentAt() {
      return sentAt;
    }

    public Instant getAcceptedAt() {
      return acceptedAt;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
