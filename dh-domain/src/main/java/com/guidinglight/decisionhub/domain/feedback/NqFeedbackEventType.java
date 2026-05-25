package com.guidinglight.decisionhub.domain.feedback;

/**
 * Stage2-PoC-B1：NQ -> DH 回流事件的 8 种正式类型。
 *
 * <p>命名与 contracts/json-schema/ 下的 schema $id 一一对应；
 * 任何 Controller / Service / Repository 接收外部 envelope 时，必须先把字符串字段映射到本枚举， 不允许直接传递自由文本 eventType。
 *
 * <p>本枚举与 Stage1 {@link FeedbackSource} 不是同一维度：FeedbackSource 表示回流链路来源（BACKTEST / RISK / PAPER /
 * RELEASE / LIVE / REVIEW），用于经验权重；本枚举表示具体事件类型（创建/启动/停止/日报/告警/恢复/稳定性检查/回测结果）。
 */
public enum NqFeedbackEventType {
  /** NQ 侧 paper run 已创建。 */
  PAPER_RUN_CREATED,
  /** NQ 侧 paper run 已启动并开始接收行情。 */
  PAPER_RUN_STARTED,
  /** NQ 侧 paper run 已停止。 */
  PAPER_RUN_STOPPED,
  /** NQ 侧 paper run 生成了某一天的日报。 */
  PAPER_RUN_DAILY_REPORT_GENERATED,
  /** NQ 侧 paper run 触发了告警。 */
  PAPER_RUN_ALERT_RAISED,
  /** NQ 侧 paper run 记录了一次恢复事件（例如重启/对账成功）。 */
  PAPER_RUN_RECOVERY_EVENT_RECORDED,
  /** NQ 侧 paper run 完成了一次稳定性检查。 */
  PAPER_RUN_STABILITY_CHECK_COMPLETED,
  /** NQ 侧某次 backtest 的结果已经准备就绪，可供 DH 拉取或在 payload 中提供。 */
  BACKTEST_RESULT_READY
}
