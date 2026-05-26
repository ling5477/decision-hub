package com.guidinglight.decisionhub.connector.nq;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage3-B3：DH -&gt; NQ 回测请求出站调用的结构化返回。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §6.2 同步响应矩阵。
 *
 * <p>语义边界（强制）：
 *
 * <ul>
 *   <li>本对象只表达"同步阶段结果"。
 *   <li>本对象不包含 sharpeRatio / verdict / maxDrawdown 等回测结果指标；
 *       那些字段只能由 NQ 端通过异步 BACKTEST_RESULT_READY feedback 携带，
 *       并由 DH ingest 链路写入 {@code DhBacktestResultSnapshot}（参见 SPEC §9）。
 *   <li>本对象不暴露 NQ 内部回测引擎细节；DH 不重写、不复制 NQ 回测核心。
 * </ul>
 */
public final class NqBacktestSubmitResult {

  private final String requestId;
  private final String jobId;
  private final NqBacktestSubmitStatus status;
  private final Instant acceptedAt;
  private final String errorCode;
  private final String errorMessage;

  private NqBacktestSubmitResult(
      final String requestId,
      final String jobId,
      final NqBacktestSubmitStatus status,
      final Instant acceptedAt,
      final String errorCode,
      final String errorMessage) {
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.jobId = jobId;
    this.status = Objects.requireNonNull(status, "status");
    this.acceptedAt = acceptedAt;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  /** ACCEPTED：携带 jobId 与 acceptedAt。 */
  public static NqBacktestSubmitResult accepted(
      final String requestId, final String jobId, final Instant acceptedAt) {
    return new NqBacktestSubmitResult(
        requestId,
        Objects.requireNonNull(jobId, "jobId"),
        NqBacktestSubmitStatus.ACCEPTED,
        Objects.requireNonNull(acceptedAt, "acceptedAt"),
        null,
        null);
  }

  /** DUPLICATE：NQ 409 DUPLICATE_REQUEST，DH 视为成功并尝试携带原 jobId（可空）。 */
  public static NqBacktestSubmitResult duplicate(
      final String requestId, final String jobId, final Instant acceptedAt) {
    return new NqBacktestSubmitResult(
        requestId, jobId, NqBacktestSubmitStatus.DUPLICATE, acceptedAt, null, null);
  }

  /** DISABLED：DH 端 gate 关闭，或 NQ 返回 423 AI_DISABLED。 */
  public static NqBacktestSubmitResult disabled(final String requestId, final String errorCode) {
    return new NqBacktestSubmitResult(
        requestId,
        null,
        NqBacktestSubmitStatus.DISABLED,
        null,
        Objects.requireNonNull(errorCode, "errorCode"),
        null);
  }

  /** FAILED：永久错误；不可重试。 */
  public static NqBacktestSubmitResult failed(
      final String requestId, final String errorCode, final String errorMessage) {
    return new NqBacktestSubmitResult(
        requestId,
        null,
        NqBacktestSubmitStatus.FAILED,
        null,
        Objects.requireNonNull(errorCode, "errorCode"),
        errorMessage);
  }

  public String getRequestId() {
    return requestId;
  }

  /** 可空：DISABLED / FAILED 情况下无 jobId。 */
  public String getJobId() {
    return jobId;
  }

  public NqBacktestSubmitStatus getStatus() {
    return status;
  }

  /** 可空：仅 ACCEPTED / DUPLICATE 时填充。 */
  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  /** 可空：仅 DISABLED / FAILED 时填充。 */
  public String getErrorCode() {
    return errorCode;
  }

  /** 可空：失败原因可读消息，必须脱敏，不包含密钥 / token / 账号凭证。 */
  public String getErrorMessage() {
    return errorMessage;
  }
}
