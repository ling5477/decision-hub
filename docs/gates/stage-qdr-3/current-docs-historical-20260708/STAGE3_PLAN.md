# Stage3 PLAN

> Created: 2026-05-26
> Stage: Stage3-PLAN (planning only)
> Status: PLAN in progress（本文件落盘即视为 PLAN completed 草案，等待 STATUS 同步）
> Source of truth: docs/current

## 1. Stage3 总目标

把 Stage2-PoC 已经落地的 DH 能力层（领域模型 / 用例服务 / Fake adapter / `/api/ai/feedback/nq` ingest / V3 持久化）与 NQ 真实平台对齐，进入"可联调"前的全套规划。

Stage3 只做规划（PLAN）。它产出 6 份规划文档：

```text
docs/current/STAGE3_PLAN.md                       (本文件，主索引)
docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md     NQ -> DH 出站事件规划
docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md     DH -> NQ 回测请求规划
docs/current/STAGE3_CONTRACT_PLAN.md              端到端契约规则
docs/current/STAGE3_TEST_PLAN.md                  测试策略
docs/current/STAGE3_WORK_ORDER.md                 后续 4 个 IMPLEMENT Batch 工单草案
```

Stage3 IMPLEMENT 由后续工单驱动，本阶段不写业务代码。

## 2. 允许范围

```text
1. 仅 PLAN：在 docs/current/ 下新增 / 维护 6 份 STAGE3_*.md 与状态文件
2. 允许引用并复盘已冻结的 docs/gates/dh-stage1/ 与 docs/gates/dh-stage2-poc/
3. 允许引用现有契约：contracts/openapi.yaml、contracts/json-schema/*.schema.json
4. 允许引用现有领域模型与端口名（仅引用，不修改实现）：
   - dh-domain.feedback.NqFeedbackEvent / FeedbackSource
   - dh-domain.backtest.DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot
   - dh-domain.{forecast, marketdata, reflection, checkpoint} 等
   - dh-usecase.agent.feedback.* / agent.planner.*
   - dh-connector.nq.* / connector.tools.* / connector.research.*
5. 允许在文档中描述未来希望落到 NQ 侧的契约 / 事件 / 字段 / endpoint，但不在本仓库实现
```

## 3. 禁止范围

```text
不修改任何 Java 业务代码
不修改 NQ 仓库
不接真实 NQ API
不接真实 Kronos
不接真实 global-stock-data
不引入 TradingAgents Python
不实现真实下单
不绕过 NQ 风控
不重写 NQ 回测核心
不建设前端
不实现 Stage3 功能
不修改 contracts/openapi.yaml 语义（仅 PLAN 文档引用）
不修改 Flyway migration 语义
```

## 4. NQ -> DH feedback event 出站链路 规划要点

详见 `docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md`。本文件只列要点：

```text
来源：NQ 侧 Paper Trading / Backtest / Risk / Daily Report / Alert / Recovery / Stability 等正式结果
事件信封：复用 Stage2-PoC-B1 已落地 NqFeedbackEnvelope（contracts/json-schema/nq-feedback-envelope.schema.json）
事件类型（8 种）：复用 Stage2-PoC-B1 NqFeedbackEventType（PAPER_RUN_*, BACKTEST_RESULT_READY）
接收路径：复用 Stage2-PoC-B2 已落地 POST /api/ai/feedback/nq
幂等键：复用 Stage2-PoC-B5 V3 ALTER 的 dh_nq_feedback_events.event_id 唯一索引
追踪：traceId / correlationId / requestId / sourceJobId 四字段透传
经验沉淀：feedback ingest 命中后由后续 IMPLEMENT 把 outcome 写入 ExperienceEntry / PheromoneEdge
不做：不在 DH 侧持有 NQ 的订单 / 成交 / 仓位 / 实盘事实表
```

## 5. DH -> NQ backtest request 入站链路 规划要点

详见 `docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md`。本文件只列要点：

```text
DH 侧产物：DhBacktestRequest（contracts/json-schema/dh-backtest-request.schema.json，Stage2-PoC-B1 已落地）
DH 侧出口：dh-connector.nq.NqBacktestClient 端口（Stage1 已落地 Fake；Stage3 IMPLEMENT 阶段才接真实 HTTP）
NQ 侧目标 endpoint（待 NQ 团队确认）：POST /api/ai/research/backtest-requests
NQ 同步响应：DhBacktestRequestAccepted（contracts/json-schema/dh-backtest-request-accepted.schema.json）
NQ 异步结果：以 BACKTEST_RESULT_READY 事件回传，按 §4 链路落地
DH 消费：JudgeDecision 评估流程读取 DhBacktestResultSnapshot，更新 ExperienceEntry
NQ 回测核心：完全由 NQ 自有；DH 不复制、不重写、不旁路、不替代
```

## 6. 分阶段实施路线

Stage3 不在本轮实施。后续 IMPLEMENT 按 `STAGE3_WORK_ORDER.md` 拆 4 个 Batch：

```text
Batch 1  Contract Alignment           契约对齐与文档收敛（不写业务代码）
Batch 2  NQ Feedback Outbox PLAN/IMPL NQ 侧最小 outbox 规划（IMPL 由 NQ 团队落，本仓库只对齐契约）
Batch 3  DH Backtest Request Adapter  DH 侧生成 + Fake / Contract test 对齐 NQ
Batch 4  End-to-End Contract Test     测试环境联调，不接实盘
```

每个 Batch 都遵守 §3 禁止范围。Batch 4 完成后才进入 Stage3-VERIFY；Stage3-FREEZE 与本仓库当前阶段相同的冻结流程（拷贝 docs/current 到 docs/gates/dh-stage3/）。

## 7. 风险点

```text
NQ 侧 endpoint 未确认：
  - /api/ai/research/backtest-requests 仅在 docs/current/DH_NQ_INTEGRATION.md §4.1 列出，
    需要 Stage3 Batch 1 与 NQ 团队达成正式契约
  - 当前 contracts/openapi.yaml 没有 /api/ai/research/backtest-requests，会在 Batch 1 PLAN 中
    决定是否新增（语义不变，仅声明 DH 侧客户端期望）

NQ 侧 outbox 未规划：
  - NqFeedbackEnvelope 已经 Stage2 落地，但 NQ 端如何按"事件源 -> envelope -> HTTP POST DH"
    实现 outbox + retry + dead-letter，尚未规划

幂等键覆盖：
  - dh_nq_feedback_events.event_id 唯一索引仅在 PoC V3 落地，真实流量 race 取决于 NQ 侧
    eventId 生成策略；需要 Stage3 Batch 1 与 NQ 对齐 eventId 生成规则

经验沉淀路径：
  - Stage2-PoC IMPLEMENT 只完成了 ingest + 持久化，未把 outcome 反向更新到 ExperienceEntry /
    PheromoneEdge。Stage3 IMPLEMENT Batch 1/2 须补此链路（仍只动 DH 仓库）

DhBacktestRequest schema 漂移：
  - DH 侧 schema 与 NQ 侧实际能接受的字段（symbol universe / frequency / 资金口径）可能
    存在差异，需 Batch 1 对齐

PostgresContainerSmokeTest 仍未在 CI 跑过：
  - Stage2 因本地无 Docker 跳过；Stage3 Batch 4 必须在 CI 环境补跑

DH 不得在 PLAN 中悄悄绕过 NQ 风控：
  - DhBacktestRequest 仍是"研究请求"，NQ 自有风控；不允许把 paper / live 触发包裹成
    backtest request 提交给 NQ
```

## 8. 验收标准（Stage3-PLAN 本轮）

```text
6 份 Stage3 PLAN 文档齐全且互相一致：
  - STAGE3_PLAN.md（本文件）
  - STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
  - STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
  - STAGE3_CONTRACT_PLAN.md
  - STAGE3_TEST_PLAN.md
  - STAGE3_WORK_ORDER.md
README.md / AGENTS.md / docs/current/README.md / docs/current/STATUS.md 四处状态对齐到
"Current stage: Stage3-PLAN completed / Next stage: Stage3-WO"
docs/current/WORKLOG.md 追加 Stage3-PLAN 段
docs/current/TESTING.md 记录本轮为文档规划阶段，无 Java 业务代码改动，mvn test 仍 BUILD SUCCESS
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false 全绿
零 Java 业务代码改动；零 NQ 仓库改动；零 Stage3 实现代码；零真实外部接入
```

## 9. Stage3 之后

Stage3 全部 4 个 Batch IMPLEMENT 完成并 VERIFY 通过后，进入 Stage3-FREEZE：

```text
docs/current/* 完整拷贝到 docs/gates/dh-stage3/
README.md / AGENTS.md / docs/current/STATUS.md 切换到 "Stage3 FREEZE completed / Next: DH-FREEZE"
```

DH-FREEZE 之后进入 `Decision Hub Agent Decision Layer v1` 长期维护态。
