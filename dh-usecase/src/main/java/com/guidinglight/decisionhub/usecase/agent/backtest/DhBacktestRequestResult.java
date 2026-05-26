package com.guidinglight.decisionhub.usecase.agent.backtest;

import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Stage3-B3：{@link DhBacktestRequestService#submit(DhBacktestRequestCommand)} 的 typed 返回。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.1。
 *
 * <p>语义：本对象只表达"DH 端 submit 同步阶段结果 + 本地状态机当前位置"。它**不**表达正式回测结果；
 * 正式回测指标只能由 NQ feedback BACKTEST_RESULT_READY 异步携带。
 */
public final class DhBacktestRequestResult {

  private final String requestId;
  private final DhBacktestRequestStatus status;
  private final DhBacktestRequestOutcome outcome;
  private final String jobId;
  private final Instant acceptedAt;
  private final DhBacktestRequestErrorCode errorCode;
  private final String errorMessage;
  private final boolean retryable;

  private DhBacktestRequestResult(
      final String requestId,
      final DhBacktestRequestStatus status,
      final DhBacktestRequestOutcome outcome,
      final String jobId,
      final Instant acceptedAt,
      final DhBacktestRequestErrorCode errorCode,
      final String errorMessage,
      final boolean retryable) {
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.status = Objects.requireNonNull(status, "status");
    this.outcome = Objects.requireNonNull(outcome, "outcome");
    this.jobId = jobId;
    this.acceptedAt = acceptedAt;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.retryable = retryable;
  }

  /** ACCEPTED：NQ 已入队（含 Fake 路径）。 */
  public static DhBacktestRequestResult accepted(
      final String requestId,
      final String jobId,
      final Instant acceptedAt,
      final boolean fakeMode) {
    return new DhBacktestRequestResult(
        requestId,
        DhBacktestRequestStatus.ACCEPTED,
        fakeMode ? DhBacktestRequestOutcome.FAKE_ACCEPTED : DhBacktestRequestOutcome.ACCEPTED,
        Objects.requireNonNull(jobId, "jobId"),
        Objects.requireNonNull(acceptedAt, "acceptedAt"),
        null,
        null,
        false);
  }

  /** DUPLICATE：NQ 409 视为成功；状态机切 ACCEPTED。 */
  public static DhBacktestRequestResult duplicate(
      final String requestId, final String jobId, final Instant acceptedAt) {
    return new DhBacktestRequestResult(
        requestId,
        DhBacktestRequestStatus.ACCEPTED,
        DhBacktestRequestOutcome.DUPLICATE,
        jobId,
        acceptedAt,
        DhBacktestRequestErrorCode.DUPLICATE_REQUEST,
        null,
        false);
  }

  /** 24h 短路：返回原 requestId / jobId（若可获取）；状态机保持原态。 */
  public static DhBacktestRequestResult idempotentShortCircuit(
      final String originalRequestId,
      final DhBacktestRequestStatus originalStatus,
      final String originalJobId,
      final Instant originalAcceptedAt) {
    return new DhBacktestRequestResult(
        originalRequestId,
        Objects.requireNonNull(originalStatus, "originalStatus"),
        DhBacktestRequestOutcome.IDEMPOTENT_SHORT_CIRCUIT,
        originalJobId,
        originalAcceptedAt,
        null,
        null,
        false);
  }

  /** DISABLED：DH gate 关闭或 NQ 423。 */
  public static DhBacktestRequestResult disabled(
      final String requestId, final DhBacktestRequestErrorCode errorCode) {
    return new DhBacktestRequestResult(
        requestId,
        DhBacktestRequestStatus.QUEUED, // 状态机：VALIDATED -> DISABLED 在仓储层标记；service 返回 QUEUED 占位
        DhBacktestRequestOutcome.DISABLED,
        null,
        null,
        Objects.requireNonNull(errorCode, "errorCode"),
        null,
        false);
  }

  /** FAILED：永久错误。 */
  public static DhBacktestRequestResult failed(
      final String requestId,
      final DhBacktestRequestErrorCode errorCode,
      final String errorMessage) {
    return new DhBacktestRequestResult(
        requestId,
        DhBacktestRequestStatus.FAILED,
        DhBacktestRequestOutcome.FAILED,
        null,
        null,
        Objects.requireNonNull(errorCode, "errorCode"),
        errorMessage,
        errorCode.isRetryable());
  }

  public String getRequestId() {
    return requestId;
  }

  public DhBacktestRequestStatus getStatus() {
    return status;
  }

  public DhBacktestRequestOutcome getOutcome() {
    return outcome;
  }

  public String getJobId() {
    return jobId;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public DhBacktestRequestErrorCode getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  /** 仅 outcome=FAILED 且 errorCode.isRetryable() 时为 true。 */
  public boolean isRetryable() {
    return retryable;
  }
}
