package com.guidinglight.decisionhub.usecase.agent.backtest;

/**
 * Stage3-B3：DH 端 backtest request 错误码。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §6.3 错误码映射表（节选可重试 / 永久失败相关码）。
 *
 * <p>每个枚举值附带 {@link #isRetryable()} 标记，caller 可据此决定后续是否重试 / 切 Fake / 触发告警。
 *
 * <p>硬边界：不允许包含 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute /
 * placeLive / submitLive 等任何下单 / 实盘 / 绕风控 / 重写回测核心相关语义。
 */
public enum DhBacktestRequestErrorCode {
  /** DH 端本地校验失败（symbols / dates / capital / frequency / paramsHash 等）。 */
  DH_VALIDATION_FAILED(false),
  /** DH gate 关闭：backtest-request.enabled=false。 */
  DH_DISABLED(false),
  /** NQ 400：symbols 字段非法。 */
  INVALID_SYMBOLS(false),
  /** NQ 400：日期区间非法。 */
  INVALID_DATE_RANGE(false),
  /** NQ 400：不支持的频率。 */
  UNSUPPORTED_FREQUENCY(false),
  /** NQ 400：strategyParametersJson 非法。 */
  INVALID_PARAMETERS_JSON(false),
  /** NQ 400：超配额。 */
  QUOTA_EXCEEDED(false),
  /** NQ 400：风控拒绝（DH 不绕过；视为永久失败）。 */
  RISK_GATED(false),
  /** NQ 401：认证失败。 */
  HTTP_401(false),
  /** NQ 403：授权失败。 */
  HTTP_403(false),
  /** NQ 409：重复请求；DH 视为成功，但保留错误码用于审计。 */
  DUPLICATE_REQUEST(false),
  /** NQ 423 AI_DISABLED。 */
  NQ_AI_DISABLED(false),
  /** NQ 429：限速；可重试，但不计入死信上限。 */
  RATE_LIMITED(true),
  /** NQ 5xx：临时错误。 */
  HTTP_5XX(true),
  /** HTTP 客户端超时。 */
  TIMEOUT(true),
  /** 网络 / DNS / SSL 失败。 */
  NETWORK(true),
  /** 协议违规：未知 HTTP 状态码或不可解析响应。 */
  PROTOCOL_VIOLATION(true);

  private final boolean retryable;

  DhBacktestRequestErrorCode(final boolean retryable) {
    this.retryable = retryable;
  }

  /** 是否允许重试。{@code true} 表示按退避矩阵重试；{@code false} 表示终态失败。 */
  public boolean isRetryable() {
    return retryable;
  }
}
