package com.guidinglight.decisionhub.usecase.agent.backtest;

/**
 * Stage3-B3：{@link DhBacktestRequestService#submit(DhBacktestRequestCommand)} 的 outcome 枚举。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.1。
 *
 * <p>语义：本枚举只描述"DH 端 submit 的本次调用结局"，不描述正式回测结果。
 * {@link com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus#RESULT_READY} 终态只能由
 * NQ 异步 BACKTEST_RESULT_READY feedback 驱动，不能由本枚举值直接产生。
 *
 * <p>硬边界：不允许新增 PLACE_/SUBMIT_/EXECUTE_/LIVE_/RISK_BYPASS_/FORCE_ 前缀的值。
 */
public enum DhBacktestRequestOutcome {
  /** NQ 已接受请求并入队（同步成功）。 */
  ACCEPTED,
  /** NQ 返回 409 DUPLICATE_REQUEST；DH 视为成功。 */
  DUPLICATE,
  /** 24h 内同 (candidateId + paramsHash) 重复，service 内短路。 */
  IDEMPOTENT_SHORT_CIRCUIT,
  /** Fake 模式下的成功（不发 HTTP；deterministic）。 */
  FAKE_ACCEPTED,
  /** DH 或 NQ 端 backtest-request 能力关闭。 */
  DISABLED,
  /** 永久错误（4xx / 重试耗尽 / DH 本地校验失败 / 网络致死）。 */
  FAILED
}
