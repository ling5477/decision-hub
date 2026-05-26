# Stage3 NQ -> DH Feedback Event Plan

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md`
> Scope: 规划 NQ 侧将正式结果以事件方式回流 DH 的全链路。本文件只规划，不实现。

## 1. NQ 侧事件来源

```text
Paper Trading Engine        Paper run 生命周期与日报
Backtest Engine             正式回测完成 / 拒绝
Risk Engine                 风控拒绝
Alert / Recovery 模块       异常报警与恢复
Stability Check 模块        稳定性巡检结果
```

NQ 必须保证每个事件源都通过统一的 outbox 出口发送，不允许任一引擎直接拼装 HTTP 推送 DH。
outbox 实现归 NQ 团队（参考 §5）。

## 2. 事件类型映射（复用 Stage2-PoC-B1 NqFeedbackEventType）

| 事件类型 | NQ 侧触发源 | 必填 payload schema | DH 侧消费目标 |
| --- | --- | --- | --- |
| `PAPER_RUN_CREATED` | Paper run 入队成功 | `nq-feedback-paper-run-created.schema.json` | 任务图：标记 candidate 进入 paper |
| `PAPER_RUN_STARTED` | Paper run 实际开始 | `nq-feedback-paper-run-started.schema.json` | 时间线记录 |
| `PAPER_RUN_STOPPED` | Paper run 终止（含人工 / 异常） | `nq-feedback-paper-run-stopped.schema.json` | 经验沉淀（含 stop reason） |
| `PAPER_RUN_DAILY_REPORT_GENERATED` | 每日报表生成 | `nq-feedback-paper-run-daily-report-generated.schema.json` | ExperienceEntry：realizedPnl / maxDrawdown / winRate |
| `PAPER_RUN_ALERT_RAISED` | 风险或运行告警 | `nq-feedback-paper-run-alert-raised.schema.json` | FailureCaseStore：alertCode / alertLevel |
| `PAPER_RUN_RECOVERY_EVENT_RECORDED` | 恢复事件 | `nq-feedback-paper-run-recovery-event-recorded.schema.json` | 时间线记录 |
| `PAPER_RUN_STABILITY_CHECK_COMPLETED` | 稳定性巡检完成 | `nq-feedback-paper-run-stability-check-completed.schema.json` | StrategyPatternMemory：result + summary |
| `BACKTEST_RESULT_READY` | 正式回测结果落地 | `nq-feedback-backtest-result-ready.schema.json` | JudgeDecision：sharpe / drawdown / verdict 反馈 |

8 个类型与 `contracts/json-schema/*.schema.json` 文件名一一对应。Stage3 不新增事件类型。

## 3. 触发时机

```text
"正式落地" 之后触发，不在中间态触发：
  PAPER_RUN_CREATED                paper_runs 表事务提交后
  PAPER_RUN_STARTED                run 状态由 SCHEDULED 转 RUNNING 之后
  PAPER_RUN_STOPPED                run 状态进入 STOPPED 之后（含手动 / 异常 / 自动）
  PAPER_RUN_DAILY_REPORT_GENERATED 日报落库后
  PAPER_RUN_ALERT_RAISED           alert 持久化后
  PAPER_RUN_RECOVERY_EVENT_RECORDED 恢复事件持久化后
  PAPER_RUN_STABILITY_CHECK_COMPLETED 巡检结果持久化后
  BACKTEST_RESULT_READY            backtest result 持久化后

NQ 端禁止在 in-flight 状态触发事件；DH 仅接受"落地后"事件，避免幻状态 / 重复回滚。
```

## 4. 幂等规则

```text
键：       envelope.eventId（NQ 端生成，全局唯一，UUIDv7 推荐）
唯一索引： Stage2-PoC-B5 V3 已落地 ux_dh_nq_feedback_events_event_id (where event_id is not null)
行为：     重放 -> DH 返回 202 + NqFeedbackAcceptedResponse{outcome: DUPLICATE}
不重算：   重放命中后，DH 不重新走经验沉淀流程，原 outcome 保留
NQ 责任：  - eventId 在 NQ 端生成并持久化在 outbox 行上
           - 同一 eventId 不允许换 payload 重发
           - eventId 与业务实体 ID（paperRunId / backtestId / reportId）解耦
```

## 5. retry / dead-letter / audit 规划

NQ 侧 outbox 行为（NQ 团队实施，DH 仅规划契约）：

```text
outbox 表结构（NQ 侧最小实现）：
  event_id        text primary key
  event_type      text not null            -- 8 种 NqFeedbackEventType 之一
  occurred_at     timestamptz not null
  trace_id        text not null
  request_id      text not null
  correlation_id  text not null
  source_job_id   text not null
  schema_version  text not null
  payload_json    jsonb not null
  attempt         int  not null default 0
  next_retry_at   timestamptz
  status          text not null            -- PENDING / SENT / DEAD
  last_error      text

发送策略：
  - 指数退避：1s / 5s / 30s / 5min / 1h，attempt 上限 8
  - 失败 8 次进入 DEAD 状态，写 dead-letter（NQ 侧），不再重试
  - DH 侧 200/202 视为发送成功（含 DUPLICATE）
  - DH 侧 400 + errorCode 视为契约错误，立即 DEAD，不重试
  - DH 侧 5xx / timeout 视为可重试

审计：
  - NQ outbox 保留 30 天（含 DEAD）
  - DH 侧 dh_nq_feedback_events 保留全部历史（无 TTL，留给后续治理）
  - NQ <-> DH 一致性以 eventId 对账：(NQ outbox sent) ∪ (DH events present) = NQ outbox attempted

DH 侧不实现 retry：DH 是被动 ingest，重试由 NQ outbox 负责。
```

## 6. DH 接收路径

```text
端点：POST /api/ai/feedback/nq                            （Stage2-PoC-B2 已落地）
头：  X-Trace-Id（可选，缺省由 TraceIdFilter 生成）
体：  NqFeedbackEnvelope（contracts/json-schema/nq-feedback-envelope.schema.json）

DH 内部分发链路（Stage2-PoC-B2 已落地）：
  Controller -> NqFeedbackIngestionService(Validator + Router) -> 8 个 Handler ->
  幂等去重（dh_nq_feedback_events.event_id 唯一索引） -> 经验沉淀

经验沉淀（Stage3 IMPLEMENT 待补，本仓库实现）：
  PAPER_RUN_DAILY_REPORT_GENERATED -> ExperienceEntry（pnl / drawdown / winRate）
  PAPER_RUN_ALERT_RAISED           -> FailureCaseStore + PheromoneEdge 衰减
  BACKTEST_RESULT_READY            -> ExperienceEntry + PheromoneEdge 加权
  其余事件                          -> 时间线记录（dh_research_runs.payload_json 切片）
```

## 7. 追踪字段规则

```text
traceId
  生命周期：跨 DH ResearchRun 与 NQ Job
  生成：    DH 侧 ResearchRun 创建时生成；DH -> NQ 请求必带；NQ feedback 必带原 traceId
  传递：    HTTP header X-Trace-Id + envelope.traceId 双通道
  对账：    feedback ingest 收到的 traceId 必须能在 DH 侧 dh_research_runs.trace_id 查到，
            否则 errorCode = UNKNOWN_TRACE

requestId
  生命周期：单次 DH -> NQ 请求（如一次 backtest request）
  生成：    DH 侧出站请求生成；NQ 在异步 feedback 中必须原样回传
  对账：    feedback 与原始 DhBacktestRequest 通过 requestId 串联

correlationId
  生命周期：业务上下文（如一个 candidate 的整个 paper run 周期）
  生成：    DH 侧 candidate / paper 流程统一分配
  传递：    所有相关请求与 feedback 共享同一 correlationId

sourceJobId
  生命周期：NQ 侧 Job 标识（paperRunId / backtestId / alertId 等）
  生成：    NQ 端生成
  传递：    feedback envelope 必填，便于跨系统对账与排错

eventId
  生命周期：单条 feedback event
  生成：    NQ outbox 行 ID（推荐 UUIDv7）
  对账：    DH 侧幂等键，唯一索引保证
```

## 8. 不做事项

```text
DH 不在 ingest 路径下做任何"为 NQ 写交易事实"的动作
DH 不在 ingest 路径下触发新的 NQ 请求（避免回环）
DH 不复制 NQ 订单 / 成交 / 仓位 / 实盘事实表
DH 不实现 retry / outbox / dead-letter（由 NQ 侧负责）
DH 不接受 in-flight 中间态事件
DH 不允许 ingest 路径写 NQ 数据库
DH 不在 Stage3-PLAN 阶段写任何 Java 业务代码（含此链路的经验沉淀实现）
```

## 9. 与 Stage3 其他 PLAN 文档的衔接

```text
契约细节 -> docs/current/STAGE3_CONTRACT_PLAN.md
DH -> NQ 出站 -> docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
测试策略 -> docs/current/STAGE3_TEST_PLAN.md
IMPLEMENT 拆批 -> docs/current/STAGE3_WORK_ORDER.md
```
