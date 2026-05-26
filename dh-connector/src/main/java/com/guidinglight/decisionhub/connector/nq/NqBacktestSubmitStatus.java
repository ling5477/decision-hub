package com.guidinglight.decisionhub.connector.nq;

/**
 * Stage3-B3：DH -&gt; NQ 回测请求出站调用的同步状态。
 *
 * <p>语义边界（参见 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md §4 / §7）：
 *
 * <ul>
 *   <li>{@link #ACCEPTED}：NQ 已经接受请求并入队（同步阶段成功）；DH 应等待异步 BACKTEST_RESULT_READY feedback。
 *   <li>{@link #DUPLICATE}：NQ 返回 409 DUPLICATE_REQUEST；DH 视为幂等成功，不再重发。
 *   <li>{@link #DISABLED}：DH 端或 NQ 端"backtest-request"能力当前关闭（DH gate=false 或 NQ HTTP 423 AI_DISABLED）；
 *       不抛系统异常，由 caller 决定降级。
 *   <li>{@link #FAILED}：永久错误（4xx 非 409 / 重试耗尽 / 网络致死失败 / 序列化失败）；
 *       不可重试，进入终态。
 * </ul>
 *
 * <p>本枚举只表达"同步阶段语义"；DH 端 ResearchRun 主流程状态机迁移由
 * {@link com.guidinglight.decisionhub.connector.nq.NqBacktestClient} 调用方负责。
 *
 * <p>硬边界：本枚举不允许新增 ORDER_/FILL_/POSITION_/LIVE_/PLACE_/SUBMIT_/EXECUTE_ 前缀的值；
 * 任何新增需先在 STAGE3_CONTRACT_PLAN.md 评审。
 */
public enum NqBacktestSubmitStatus {
  /** NQ 已接收请求并入队（同步阶段成功）。 */
  ACCEPTED,
  /** 同 requestId 已被 NQ 接收过；DH 视为幂等成功。 */
  DUPLICATE,
  /** backtest-request 能力当前关闭（DH 或 NQ 端）；不抛异常。 */
  DISABLED,
  /** 永久错误；不可重试，进入终态。 */
  FAILED
}
