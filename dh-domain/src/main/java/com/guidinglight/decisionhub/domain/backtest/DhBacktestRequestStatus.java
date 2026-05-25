package com.guidinglight.decisionhub.domain.backtest;

/**
 * Stage2-PoC-B1：DH 发往 NQ 的回测请求的状态机。
 *
 * <p>DH 不执行回测，本枚举只用于跟踪请求的传输与回流状态：
 *
 * <ul>
 *   <li>{@link #DRAFT}：DH 内部构造完毕，尚未发出。
 *   <li>{@link #QUEUED}：已发往 NQ，等待 NQ 排队。
 *   <li>{@link #ACCEPTED}：NQ 已接受并返回 jobId。
 *   <li>{@link #REJECTED}：NQ 拒绝（参数或风控不通过）。
 *   <li>{@link #RESULT_READY}：NQ 已回流 {@code BACKTEST_RESULT_READY} 事件。
 *   <li>{@link #FAILED}：NQ 执行失败或回流超时。
 * </ul>
 *
 * <p>本枚举不允许出现 PLACE/SUBMIT/EXECUTE 类语义；DH 仅“请求 NQ 执行正式能力”，不代理交易。
 */
public enum DhBacktestRequestStatus {
  DRAFT,
  QUEUED,
  ACCEPTED,
  REJECTED,
  RESULT_READY,
  FAILED
}
