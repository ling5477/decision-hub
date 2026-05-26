package com.guidinglight.decisionhub.usecase.agent.backtest;

/**
 * Stage3-B3：DH 端 backtest request usecase 端口。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.1 / §4 / §5。
 *
 * <p>职责：
 *
 * <ul>
 *   <li>从 {@link DhBacktestRequestCommand} 构造 wire-level {@code DhBacktestRequest}；
 *   <li>本地校验入参；
 *   <li>24h paramsHash 幂等短路；
 *   <li>调用 {@link com.guidinglight.decisionhub.connector.nq.NqBacktestClient}（默认 Fake 或 Disabled）；
 *   <li>更新 {@link DhBacktestRequestRepository}；
 *   <li>返回 {@link DhBacktestRequestResult}（不抛 RuntimeException 中断 caller）。
 * </ul>
 *
 * <p>硬约束：
 *
 * <ul>
 *   <li>本端口不允许直接生成 BACKTEST_RESULT_READY 终态；DH RESULT_READY 仅由 NQ feedback 事件驱动。
 *   <li>本端口不允许在 ingest 路径自动触发 paper / live / 实盘下单。
 *   <li>本端口不允许在 Disabled 模式下抛异常阻断 ResearchRun 主流程。
 * </ul>
 */
public interface DhBacktestRequestService {

  /** 提交一次 DH -&gt; NQ backtest request。返回 typed 结果（不抛系统异常）。 */
  DhBacktestRequestResult submit(DhBacktestRequestCommand command);
}
