# Stage3 Work Order

> Created: 2026-05-26 (Stage3-PLAN)
> Updated: 2026-05-26 (Stage3-WO 细化)
> Parent: `docs/current/STAGE3_PLAN.md`
> Sibling: `docs/current/STAGE3_BATCH_PLAN.md`
> Scope: 把 Stage3 IMPLEMENT 细化到可直接开工的工单。本文件不写业务代码，不修改 NQ 仓库，不做真实联调。

## 0. 通用守则（每个 Batch 都必须遵守）

### 0.1 开工前必读

```text
README.md
AGENTS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORKLOG.md
docs/current/TESTING.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/STAGE3_PLAN.md
docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
docs/current/STAGE3_CONTRACT_PLAN.md
docs/current/STAGE3_TEST_PLAN.md
docs/current/STAGE3_BATCH_PLAN.md
docs/current/STAGE3_WORK_ORDER.md (本文件)
docs/gates/dh-stage1/README.md
docs/gates/dh-stage2-poc/README.md
```

### 0.2 通用验收命令

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

每个 Batch 完成必须 BUILD SUCCESS，且：

```text
- ArchUnit 默认 10 条规则保持全绿（Batch 3 视情况新增不破坏既有）
- Stage1ClosedLoopTest / Stage2ClosedLoopTest 保持全绿
- 122 tests 数量只能上升，不允许下降
- PostgresContainerSmokeTest 在 CI Docker 环境通过；本地无 Docker 跳过
```

### 0.3 通用硬边界（任一 Batch 都不允许违反）

```text
不修改 NQ 仓库
不接真实 NQ API（默认 profile 始终 Fake；Batch 4 联调走 NQ test cluster）
不接真实 Kronos / global-stock-data
不引入 TradingAgents Python / graph scheduler
不实现真实下单 / 不绕过 NQ 风控 / 不重写 NQ 回测核心
不建设前端
不新增 NqFeedbackEventType（保持 8 种）
不修改已落地 OpenAPI 端点的语义；不修改 V1/V2/V3 已落地 migration 语义
```

### 0.4 通用状态推进

```text
开工前      Stage3-WO completed                    Next: Stage3-B1 Contract Alignment
B1 完成后    Stage3-B1 IMPLEMENT completed          Next: Stage3-B2 PLAN
B2 完成后    Stage3-B2 PLAN completed               Next: Stage3-B3 IMPLEMENT
B3 完成后    Stage3-B3 IMPLEMENT completed          Next: Stage3-B4 IMPLEMENT
B4 完成后    Stage3-B4 VERIFY completed             Next: Stage3-FREEZE
```

---

## 1. Batch 1: Contract Alignment

### 1.1 目标

对齐 DH 已有 Stage2 contract 与 NQ 未来接入点；明确 NQ 侧需要新增的最小 contract；明确 DH 侧哪些接口已经具备，哪些只需补契约测试；不写任何真实联调代码。把 STAGE3_NQ_TO_DH_FEEDBACK_PLAN §6 中"经验沉淀（Stage3 IMPLEMENT 待补）"在 DH 仓库内补齐（仍走 InMemory），让 8 个 Handler 命中后能写 ExperienceEntry / PheromoneEdge / FailureCaseStore。

### 1.2 文件清单

允许新增 / 修改：

```text
新增 dh-usecase 测试 (规划，IMPLEMENT 阶段最终路径以代码为准)：
  dh-usecase/src/test/.../agent/feedback/NqFeedbackHandlerExperienceTest.java
    - PAPER_RUN_DAILY_REPORT_GENERATED -> ExperienceEntry 写入
    - BACKTEST_RESULT_READY -> ExperienceEntry + PheromoneEdge 双写
    - PAPER_RUN_ALERT_RAISED -> FailureCaseStore 写入
    - 其他 5 种 -> 时间线写入 / 经验路径 no-op

修改 dh-usecase 已存在的 8 个 Handler（agent/feedback/handler/*Handler.java）：
  - PaperRunCreatedHandler
  - PaperRunStartedHandler
  - PaperRunStoppedHandler
  - PaperRunDailyReportGeneratedHandler   接入 ExperienceEntry 写入
  - PaperRunAlertRaisedHandler            接入 FailureCaseStore 写入
  - PaperRunRecoveryEventRecordedHandler
  - PaperRunStabilityCheckCompletedHandler 接入 StrategyPatternMemory 写入（可选）
  - BacktestResultReadyHandler            接入 ExperienceEntry + PheromoneEdge 双写

修改 dh-memory 已存在的 InMemory 仓储（视需要补查询方法，不破坏 Stage2 行为）：
  - InMemoryExperienceStore.java
  - InMemoryPheromoneStore.java
  - InMemoryFailureCaseStore.java

修改 dh-app 测试（视情况）：
  - ArchitectureTest.java                 视情况新增规则；不放松已落地 10 条

修改文档：
  - docs/current/STATUS.md                Batch1 推进段
  - docs/current/WORKLOG.md               追加 Stage3-B1 IMPLEMENT 段
  - docs/current/TESTING.md               追加 Stage3-B1 验收记录
```

### 1.3 contract 清单

DH 侧已具备（Stage2-PoC 已落地，仅做契约测试）：

```text
NqFeedbackEnvelope                          contracts/json-schema/nq-feedback-envelope.schema.json
PaperRunCreatedPayload                      contracts/json-schema/nq-feedback-paper-run-created.schema.json
PaperRunStartedPayload                      contracts/json-schema/nq-feedback-paper-run-started.schema.json
PaperRunStoppedPayload                      contracts/json-schema/nq-feedback-paper-run-stopped.schema.json
PaperRunDailyReportGeneratedPayload         contracts/json-schema/nq-feedback-paper-run-daily-report-generated.schema.json
PaperRunAlertRaisedPayload                  contracts/json-schema/nq-feedback-paper-run-alert-raised.schema.json
PaperRunRecoveryEventRecordedPayload        contracts/json-schema/nq-feedback-paper-run-recovery-event-recorded.schema.json
PaperRunStabilityCheckCompletedPayload      contracts/json-schema/nq-feedback-paper-run-stability-check-completed.schema.json
BacktestResultReadyPayload                  contracts/json-schema/nq-feedback-backtest-result-ready.schema.json
DhBacktestRequest                           contracts/json-schema/dh-backtest-request.schema.json
DhBacktestRequestAccepted                   contracts/json-schema/dh-backtest-request-accepted.schema.json
DhBacktestResultSnapshot                    contracts/json-schema/dh-backtest-result-snapshot.schema.json
ForecastArtifact                            contracts/json-schema/forecast-artifact.schema.json
ExternalMarketSnapshot                      contracts/json-schema/external-market-snapshot.schema.json
ReflectionEntry                             contracts/json-schema/reflection-entry.schema.json
CheckpointEntry                             contracts/json-schema/checkpoint-entry.schema.json
```

NQ 侧未来必须新增的最小 contract（DH 仅声明期望，不在本仓库实施）：

```text
入站方向（NQ -> DH /api/ai/feedback/nq）：
  - NQ outbox 必须按 NqFeedbackEnvelope 结构序列化
  - schemaVersion >= 1.0.0
  - sourceSystem 必须是 "nexus-quant"
  - eventId 全局唯一（推荐 UUIDv7）
  - traceId 必须能在 DH dh_research_runs 中找到（否则 DH 返回 400 UNKNOWN_TRACE）

出站方向（DH -> NQ /api/ai/research/backtest-requests，未来 endpoint）：
  - 接收 DhBacktestRequest 结构
  - 同步响应 DhBacktestRequestAccepted（202 / 400 / 409）
  - 异步以 BACKTEST_RESULT_READY 事件回传 result snapshot
```

### 1.4 JSON Schema / OpenAPI 影响范围

```text
JSON Schema：
  - 不新增任何 schema 文件（保持 16 份）
  - 不修改任何已落地字段类型 / required / additionalProperties
  - 允许补 description / examples（不影响校验语义）
  - JsonSchemaPresenceTest 仍是 16 份；如需，补 example 自检（不强制）

OpenAPI（contracts/openapi.yaml）：
  - 不修改已落地端点（POST /api/ai/feedback/nq、GET/POST /api/ai/research-runs/...）的语义
  - 允许补 description / examples
  - 允许在 components 段为未来 NQ 端 endpoint 增加 description 注释占位
    (POST /api/ai/research/backtest-requests)，但不新增 path（path 由 Batch 3 IMPL 时再决定）
  - info.version 不变（PLAN 阶段保持 0.1.0）
```

### 1.5 DH 侧影响范围

```text
代码层面（IMPLEMENT 阶段允许，本 WO 仅声明范围）：
  - dh-usecase.agent.feedback.handler.* 8 个 Handler：在 apply 阶段加经验沉淀
  - dh-usecase.agent.feedback.AbstractNqFeedbackEventHandler：复用现有 ExperienceFeedbackService
  - dh-memory.InMemory{Experience,Pheromone,FailureCase}Store：补查询方法（如需）
  - 不动 dh-domain（Stage2-PoC-B1 已固化）
  - 不动 dh-api（Controller 不变；仍走 NqFeedbackController）
  - 不动 dh-connector（Batch 3 才动 connector.nq）
  - 不动 dh-infra（不引入 JDBC 切换；不修改 V3 migration）

测试层面：
  - 新增 NqFeedbackHandlerExperienceTest（8 个 eventType 的经验沉淀路径覆盖）
  - 已有 NqFeedbackContractValidationTest / IdempotencyTest / HandlerDispatchTest / WebMvcTest 保持全绿

ArchUnit：
  - 已落地 10 条规则保持
  - 视情况新增（可选；不破坏既有）：
    * usecase.agent.feedback 不得引用 dh-connector.nq
    * usecase.agent.feedback 不得引用真实 HTTP client（RestTemplate / WebClient / OkHttp）
```

### 1.6 NQ 侧未来影响范围（NQ 团队后续工作，本 Batch 不实施）

```text
- NQ outbox 行为按 STAGE3_NQ_TO_DH_FEEDBACK_PLAN §5 实现：8 张事件源 -> 统一 envelope -> HTTP POST DH
- NQ 端 eventId 生成与持久化（推荐 UUIDv7）
- NQ 端 schemaVersion 升级走 semver；MAJOR 必须双方同步
- NQ 端必须保证 traceId 与 DH dh_research_runs.trace_id 一致（DH 不做 fallback）
```

### 1.7 验收标准

```text
代码：
  - mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
  - 122 tests 增加到 130+ 区间（具体以 IMPLEMENT 阶段为准）
  - ArchUnit 全绿（默认 10 条 + 视情况新增不破坏）
  - Stage1ClosedLoopTest / Stage2ClosedLoopTest 仍全绿

经验沉淀链路：
  - PAPER_RUN_DAILY_REPORT_GENERATED 命中后 ExperienceEntry 至少 1 行写入（successKey 由 strategyPattern + regime 派生）
  - BACKTEST_RESULT_READY verdict=PASS -> PheromoneEdge 加权；verdict=FAIL -> 衰减
  - PAPER_RUN_ALERT_RAISED level=ERROR/CRITICAL -> FailureCaseStore 写入
  - 其余 5 种事件 -> 不破坏 Stage2 行为，experience 路径可 no-op

文档：
  - docs/current/STATUS.md                推进 "Stage3-B1 IMPLEMENT completed / Next: Stage3-B2 PLAN"
  - docs/current/WORKLOG.md               追加 Stage3-B1 IMPLEMENT 段
  - docs/current/TESTING.md               追加 Stage3-B1 验收记录（含 mvn 输出摘要）
  - README.md / AGENTS.md / docs/current/README.md 同步当前阶段
```

### 1.8 禁止事项

```text
- 修改 NQ 仓库
- 修改 dh-domain（Stage2-PoC-B1 已固化）
- 新增 NqFeedbackEventType（保持 8 种）
- 引入真实 HTTP client / 真实 NQ 调用
- 修改 contracts/openapi.yaml 已落地端点的语义
- 修改 contracts/json-schema/*.schema.json 已落地字段
- 修改 V1/V2/V3 Flyway migration
- 在 ingest 路径触发新的 NQ 请求（避免回环）
- 在 ingest 路径写 NQ 数据
- 引入 LLM provider / TradingAgents Python / Kronos / global-stock-data
- 建设前端
- 实现 Batch 3 的 outbox / RealNqBacktestClient（不属于本 Batch）
```

### 1.9 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-B1-Contract-Alignment-IMPLEMENT。
当前阶段：Stage3-WO completed / Next stage: Stage3-B1 IMPLEMENT。

目标：把 STAGE3_NQ_TO_DH_FEEDBACK_PLAN §6 中"经验沉淀（Stage3 IMPLEMENT 待补）"在 DH 仓库内补齐：
  - 修改 8 个 NqFeedbackEventType Handler，命中后写 ExperienceEntry / PheromoneEdge / FailureCaseStore（仍走 InMemory）
  - 新增 NqFeedbackHandlerExperienceTest（覆盖 8 个 eventType 的经验沉淀路径）
  - 视情况新增 ArchUnit 规则（不破坏既有 10 条）
  - 不引入真实 HTTP / 真实 NQ 调用 / 新事件类型 / migration / OpenAPI 语义变更

约束：仅改 dh-usecase / dh-memory / 测试 / docs/current；不改 NQ 仓库；不动 dh-domain。
验收：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS。
状态：Stage3-B1 IMPLEMENT completed -> Next: Stage3-B2 PLAN。
```

---

## 2. Batch 2: NQ Feedback Outbox

### 2.1 目标

规划 NQ 侧最小 feedback outbox，只允许 Paper Trading / Backtest / Risk / Daily Report / Alert / Recovery / Stability 结果事件；不触碰订单执行核心；不接实盘自动交易。本 Batch 在 DH 仓库内只产出 SPEC 文档；NQ 仓库的 outbox 实施由 NQ 团队后续完成，不在本仓库进行。

### 2.2 NQ 侧建议模块（NQ 仓库未来落地，本仓库仅声明）

```text
nq-outbox/
  outbox-core               读写 outbox 表 / 状态机 / 重试调度
  outbox-publisher          按 schedule 拉取 PENDING / 调用 DH HTTP / 写回 SENT or DEAD
  outbox-source-paper       Paper Trading 事件源 -> envelope 序列化 -> 写 outbox
  outbox-source-backtest    Backtest 事件源 -> envelope 序列化
  outbox-source-risk        Risk Engine 事件源（PAPER_RUN_ALERT_RAISED with level=CRITICAL）
  outbox-source-daily       Daily Report 事件源
  outbox-source-alert       Alert 事件源（含 risk-derived alert）
  outbox-source-recovery    Recovery 事件源
  outbox-source-stability   Stability Check 事件源
  outbox-audit              对账作业 / dead-letter 报表

每个 source 模块只允许 "事件落库 -> 序列化为 NqFeedbackEnvelope -> 入 outbox"，
不允许直接走 HTTP，也不允许调用 outbox-publisher 之外的发送通道。
```

### 2.3 NQ 侧建议表（NQ DB，本仓库仅声明）

```sql
-- NQ 侧表，DH 仓库不创建，不迁移
CREATE TABLE IF NOT EXISTS nq_dh_feedback_outbox (
  event_id        text PRIMARY KEY,                   -- envelope.eventId（推荐 UUIDv7）
  event_type      text NOT NULL,                      -- 8 种 NqFeedbackEventType 之一
  occurred_at     timestamptz NOT NULL,
  trace_id        text NOT NULL,
  request_id      text NOT NULL,
  correlation_id  text NOT NULL,
  source_job_id   text NOT NULL,                      -- paperRunId / backtestId / alertId 等
  schema_version  text NOT NULL,                      -- semver, e.g. "1.0.0"
  payload_json    jsonb NOT NULL,                     -- payload 原始 JSON
  attempt         int  NOT NULL DEFAULT 0,
  next_retry_at   timestamptz,
  status          text NOT NULL DEFAULT 'PENDING',    -- PENDING / SENT / DEAD
  last_error      text,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_nq_dh_feedback_outbox_status_next_retry
  ON nq_dh_feedback_outbox(status, next_retry_at)
  WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS ix_nq_dh_feedback_outbox_created_at
  ON nq_dh_feedback_outbox(created_at);

CREATE TABLE IF NOT EXISTS nq_dh_feedback_dead_letter (
  event_id        text PRIMARY KEY,
  event_type      text NOT NULL,
  occurred_at     timestamptz NOT NULL,
  attempt         int NOT NULL,
  last_error      text NOT NULL,
  payload_json    jsonb NOT NULL,
  moved_at        timestamptz NOT NULL DEFAULT now()
);
```

### 2.4 event outbox 字段（与 NqFeedbackEnvelope 映射）

| envelope 字段 | outbox 列 | 来源 | 备注 |
| --- | --- | --- | --- |
| eventId | event_id | NQ outbox 行 ID | 推荐 UUIDv7；DH 幂等键 |
| eventType | event_type | NQ source 模块 | 8 种之一 |
| occurredAt | occurred_at | NQ source 模块 | 业务事件发生时间，不是发送时间 |
| sourceSystem | (常量) | "nexus-quant" | DH 端校验 |
| sourceJobId | source_job_id | NQ source 模块 | paperRunId / backtestId / alertId |
| traceId | trace_id | NQ source 模块（来自 DH 原请求） | DH 端校验必须命中 dh_research_runs |
| requestId | request_id | NQ source 模块（来自 DH 原请求） | 异步 feedback 回链 |
| correlationId | correlation_id | NQ source 模块 | 业务上下文 ID |
| schemaVersion | schema_version | NQ outbox | semver |
| payloadJson | payload_json | NQ source 模块 | payload 原始 JSON |
| (无) | attempt | NQ outbox | 重试次数 |
| (无) | next_retry_at | NQ outbox | 退避调度 |
| (无) | status | NQ outbox | PENDING / SENT / DEAD |
| (无) | last_error | NQ outbox | 上一次失败原因 |

### 2.5 retry / dead-letter / audit 规则

```text
retry 矩阵：
  attempt=0  立即发送
  attempt=1  next_retry_at = sent_at + 1s
  attempt=2  + 5s
  attempt=3  + 30s
  attempt=4  + 5min
  attempt=5  + 1h
  attempt>=8 -> 转 DEAD（停止重试）

DH 响应 -> NQ outbox 行为：
  202 outcome=ACCEPTED   -> SENT
  202 outcome=DUPLICATE  -> SENT（视为成功）
  400 (任何 errorCode)   -> DEAD（契约错误，立即停止重试）
  401 / 403              -> DEAD（认证错误，立即停止）
  429                    -> 退避后重试，attempt 不计死信上限
  5xx / timeout / 网络错误 -> 退避后重试，attempt+1

dead-letter：
  attempt>=8 转 DEAD 并写入 nq_dh_feedback_dead_letter
  报警渠道由 NQ 侧定义（DH 端不参与）
  DEAD 行 30 天后归档；30 天内可手动复发（重置 attempt=0 status=PENDING）

audit：
  outbox 保留 30 天（含 DEAD）
  对账作业每日跑：
    sum(SENT) + sum(DEAD) == sum(attempted)
    DH dh_nq_feedback_events.event_id ⊇ NQ outbox.event_id where status=SENT
    任何不一致写入 audit 报表
  schemaVersion 不一致告警：DH 端如果遇到 schemaVersion 高于本地支持，回 400 INVALID_SCHEMA；
                              NQ outbox 收到后转 DEAD，触发版本对齐流程
```

### 2.6 触发点清单（NQ 端 source 模块允许触发的时机）

只有"正式落地"事件允许触发 outbox 写入：

```text
PAPER_RUN_CREATED                paper_runs 表事务提交后
PAPER_RUN_STARTED                run 状态由 SCHEDULED 转 RUNNING 之后
PAPER_RUN_STOPPED                run 状态进入 STOPPED 之后（含手动 / 异常 / 自动）
PAPER_RUN_DAILY_REPORT_GENERATED 日报落库后
PAPER_RUN_ALERT_RAISED           alert 持久化后（含 risk-derived alert）
PAPER_RUN_RECOVERY_EVENT_RECORDED 恢复事件持久化后
PAPER_RUN_STABILITY_CHECK_COMPLETED 巡检结果持久化后
BACKTEST_RESULT_READY            backtest result 持久化后
```

不允许触发：

```text
- in-flight 中间态（RUNNING / PROGRESS%）
- 任何 NQ 内部撤销 / 回滚 / 清理触发（避免幻状态）
- 任何 in-process 事务期间（必须事务提交后）
- 任何与下单 / 仓位 / 实盘相关的事件（不在 8 种白名单内，禁止扩展）
```

### 2.7 不允许触碰的核心模块清单（NQ 端硬边界）

```text
- 订单状态机（OrderStateMachine）              outbox 不允许写入 / 触发订单状态切换
- 风控核心（RiskEngine.evaluate）              outbox 不允许写入 / 旁路风控
- 正式回测内核（BacktestKernel）               outbox 不允许触发新的回测 / 修改回测结果
- 模拟盘 / 实盘执行器                           outbox 不允许下单 / 修改仓位
- 账本与审计（Ledger / Audit）                 outbox 不允许补登 / 修改账本
- 资金管理（FundManager）                       outbox 不允许调拨资金
- 主行情订阅                                    outbox 不允许写入行情
- NQ Console 业务页面                           outbox 不修改前端状态

source 模块只允许"读取已落库事实，序列化为 envelope，写 outbox"，
不允许任何反向影响业务实体的能力。
```

### 2.8 验收标准（DH 仓库本 Batch 验收，NQ 仓库不在本 Batch 实施）

```text
DH 仓库内：
  - mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
  - 零 Java 业务代码改动（与 Stage3-PLAN 同口径）
  - ArchUnit 10 条全绿
  - Stage1 / Stage2 闭环测试全绿

新增文档：
  - docs/current/STAGE3_NQ_OUTBOX_SPEC.md     按 §2.2-§2.7 完整落地（NQ 团队执行参考）
                                              内含 outbox 表结构、字段映射、重试矩阵、
                                              dead-letter、audit、触发点、硬边界
  - docs/current/STATUS.md                    推进 "Stage3-B2 PLAN completed / Next: Stage3-B3 IMPLEMENT"
  - docs/current/WORKLOG.md                   追加 Stage3-B2 PLAN 段
  - docs/current/TESTING.md                   追加 Stage3-B2 验收记录（mvn 仅作为回归基线）
  - README.md / AGENTS.md / docs/current/README.md 同步阶段
```

### 2.9 禁止事项

```text
- 在 DH 仓库写任何 Java 代码
- 修改 NQ 仓库（实施由 NQ 团队后续完成）
- 新增 NqFeedbackEventType（保持 8 种）
- 修改 contracts/openapi.yaml 端点语义
- 修改 contracts/json-schema/*.schema.json 已落地字段
- 在 DH 仓库实现 outbox 客户端（Batch 3 IMPL 才允许 outbox 出站客户端，且仅在 dh-connector.nq）
- 让 outbox 触碰 §2.7 核心模块
```

### 2.10 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-B2-NQ-Outbox-Spec。
当前阶段：Stage3-B1 IMPLEMENT completed / Next stage: Stage3-B2 PLAN。

目标：在 docs/current/STAGE3_NQ_OUTBOX_SPEC.md 中写下 NQ 侧 feedback outbox 完整规格：
  - NQ 侧建议模块清单（nq-outbox/ + 7 个 source 模块）
  - outbox + dead-letter 表结构（nq_dh_feedback_outbox / nq_dh_feedback_dead_letter）
  - envelope <-> outbox 字段映射表
  - 重试矩阵（1s / 5s / 30s / 5min / 1h，attempt 上限 8）
  - dead-letter / 30 天保留 / 复发流程
  - 对账作业（envelope sent ⊇ DH events）
  - 8 个触发点清单（仅"事实落库后"，禁 in-flight）
  - NQ 端核心模块硬边界清单（订单状态机 / 风控 / 回测 / 实盘 / 账本 / 资金 / 主行情 / Console）
  - schemaVersion 升级流程

约束：不写任何 Java 代码；不修改 NQ 仓库；不修改 contracts/openapi.yaml 或 schema；
      mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS。
状态：Stage3-B2 PLAN completed -> Next: Stage3-B3 IMPLEMENT。
```

---

## 3. Batch 3: DH Backtest Request Adapter

### 3.1 目标

规划并实现 DH 生成 backtest request 并交给 NQ 的最小链路；NQ 仍然执行正式回测；DH 只消费 result snapshot；先 Fake / contract test，不直接真实调用 NQ（默认 profile 仍走 Fake，仅 `decisionhub.stage3.nq.enabled=true` 时才装真实 HTTP bean）。

### 3.2 DH 侧 adapter 文件清单（IMPLEMENT 阶段最终路径以代码为准）

```text
dh-usecase 新增：
  src/main/.../usecase/agent/backtest/DhBacktestRequestService.java
    - 入参：ResearchRun + StrategyCandidate
    - 出参：DhBacktestRequest（domain 对象）
    - 校验：symbols 非空 / startDate < endDate / frequency 合法 / initialCapital > 0
    - 幂等：同 candidate + 同参数 hash 在 24h 内短路（不重复发送）
    - 不直接调 HTTP；委托给 NqBacktestClient 端口

  src/main/.../usecase/agent/backtest/DhBacktestRequestRepository.java （端口）
    - save(requestId, jobId, candidateId, paramsHash, sentAt)
    - findByRequestId(requestId)
    - findByCandidateAndParamsHashWithin24h(candidateId, paramsHash, now)

  src/main/.../usecase/agent/backtest/inmemory/InMemoryDhBacktestRequestRepository.java
    - 默认 InMemory；Stage3 不引入 JDBC

  src/main/.../usecase/agent/backtest/impl/DefaultDhBacktestRequestService.java

dh-connector 新增：
  src/main/.../connector/nq/RealNqBacktestClient.java
    - @ConditionalOnProperty(prefix="decisionhub.stage3.nq", name="enabled", havingValue="true")
    - 默认不装配；仅 stage3.nq.enabled=true 才生效
    - 真实 HTTP 实现（推荐 RestTemplate 或 WebClient，二选一）
    - 仅在 dh-connector.nq 模块内引用 HTTP client
    - 接收 DhBacktestRequest -> POST -> 解析 DhBacktestRequestAccepted

dh-connector 修改（已有）：
  src/main/.../connector/nq/FakeNqBacktestClient.java
    - 添加 @ConditionalOnMissingBean(NqBacktestClient.class)
    - 默认兜底；其他 profile 都走 Fake

dh-app 修改：
  config/AgentRuntimeWiringConfig.java
    - 装配 DhBacktestRequestService + InMemoryDhBacktestRequestRepository
    - RealNqBacktestClient bean 由 @ConditionalOnProperty 自动决定是否装配
    - FakeNqBacktestClient bean @ConditionalOnMissingBean 兜底

dh-infra 视情况新增（可选）：
  src/main/resources/db/migration/V4__stage3_dh_outbox.sql
    - 仅在确实需要持久化 DH 侧 outbox 时创建（默认走 InMemory，Stage3 可不落 V4）
    - 表名建议 dh_backtest_request_outbox / dh_backtest_request_attempts
    - 不修改 V1 / V2 / V3 已有语义

测试：
  dh-usecase/src/test/.../agent/backtest/DhBacktestRequestServiceTest.java
    - build：从 ResearchRun + Candidate 构造合法 DhBacktestRequest
    - validate：symbols 空 / dates 反向 / capital 非正 / frequency 非法 -> IllegalArgumentException
    - idempotent：同 candidate + 同 paramsHash 24h 内重复 -> 短路
    - retryability：5xx 标记 retryable；400 标记 permanent-fail；409 标记 success

  dh-connector/src/test/.../connector/nq/RealNqBacktestClientTest.java
    - 仅 stage3.nq.enabled=true 时启用（@EnabledIfSystemProperty 或 @SpringBootTest profile）
    - mock HTTP 服务器（MockWebServer 或 WireMock）模拟 202 / 400 / 409 / 5xx

  dh-app/src/test/.../config/Stage3WiringConfigTest.java （可选）
    - 默认 profile 验证 FakeNqBacktestClient 装配
    - stage3.nq.enabled=true 验证 RealNqBacktestClient 装配

  dh-app/src/test/.../ArchitectureTest.java
    - 新增规则：除 dh-connector.nq 外其他模块禁止引用 RestTemplate / WebClient / OkHttp / HttpURLConnection
```

### 3.3 request DTO / domain 映射规则

```text
DH 侧 domain：DhBacktestRequest（Stage2-PoC-B1 已落地，contracts/json-schema/dh-backtest-request.schema.json）

ResearchRun + StrategyCandidate -> DhBacktestRequest 字段映射：

  requestId               UUIDv7 by IdGenerator.newId()
  traceId                 ResearchRun.traceId
  candidateId             StrategyCandidate.id
  strategyName            StrategyCandidate.name
  strategyVersion         StrategyCandidate.version
  strategyParametersJson  StrategyCandidate.parametersJson（完整冻结快照）
  entryRulesRef           StrategyCandidate.entryRulesRef（可选）
  exitRulesRef            StrategyCandidate.exitRulesRef（可选）
  startDate               ResearchRun.payloadJson.backtestStart 或 ResearchRun.startedAt - 默认窗口
  endDate                 ResearchRun.payloadJson.backtestEnd 或 ResearchRun.startedAt
  initialCapital          ResearchRun.payloadJson.initialCapital 或租户默认值
  symbols                 StrategyCandidate.symbols（minItems 1，禁止全市场扫描）
  frequency               ResearchRun.payloadJson.backtestFrequency 默认 DAILY
  requestedBy             ResearchRun.requestedBy
  requestedAt             TimeProvider.now()
  status                  DhBacktestRequestStatus.DRAFT 初始；进入 outbox 后 QUEUED

paramsHash（用于幂等短路）：
  hash = sha256(candidateId || strategyVersion || strategyParametersJson || startDate || endDate ||
                initialCapital || symbols.sorted || frequency)
  存 dh_backtest_request_outbox.params_hash（如 V4 启用），InMemory 模式存 ConcurrentHashMap key

不允许的来源：
  - 任何 in-flight 修改后的 candidate（必须 StrategyCandidate.frozen == true）
  - 任何指向 live universe 的 symbols
  - 任何带订单状态机字段的请求（DhBacktestRequest 不含 orderId / fillId / positionId）
```

### 3.4 NQ 接收契约草案（NQ 侧未来 endpoint，本 Batch 仅声明）

```text
endpoint：    POST /api/ai/research/backtest-requests
认证：        由 NQ 侧规定（建议 mTLS / 服务账号 token）
content-type: application/json
body：        DhBacktestRequest（schema: contracts/json-schema/dh-backtest-request.schema.json）

同步响应：
  202 Accepted + DhBacktestRequestAccepted
       { requestId, jobId, status: QUEUED|ACCEPTED, acceptedAt }
       - jobId 由 NQ 生成（即未来 NqFeedbackEnvelope.sourceJobId）
       - DH 侧 DhBacktestRequestRepository 记录 requestId <-> jobId 映射

  400 Bad Request + 错误 body
       errorCode 之一：INVALID_SYMBOLS / INVALID_DATE_RANGE / UNSUPPORTED_FREQUENCY /
                       INVALID_PARAMETERS_JSON / QUOTA_EXCEEDED / RISK_GATED
       DH 侧标记 permanent-fail，不重试

  409 Conflict
       同 requestId 已被 NQ 接收过；DH 视为 idempotent 成功，记录但不再发送

  401 / 403 Unauthorized / Forbidden
       DH 标记 permanent-fail，不重试，告警

  429 Too Many Requests
       DH 退避重试（与 5xx 同矩阵）

  5xx / timeout / 网络错误
       DH 重试（1s / 5s / 30s / 5min / 1h，attempt 上限 8）

不允许：
  - NQ 在同步响应里返回 backtest 结果（必须异步 feedback）
  - 任何与下单 / 实盘 / 仓位 / 账本相关的字段
```

### 3.5 result snapshot 回传规则

回测结果以异步 feedback 事件回传，不在同步响应里给：

```text
事件类型：BACKTEST_RESULT_READY
传输链路：NQ outbox -> POST /api/ai/feedback/nq -> DH ingest（已落地，Stage2-PoC-B2）
envelope.sourceJobId：= NQ 同步响应里的 jobId
payload schema：contracts/json-schema/nq-feedback-backtest-result-ready.schema.json

payload 关键字段：
  backtestId        NQ 内部 ID（= jobId 或更细）
  requestId         必须 = DH 发起的 DhBacktestRequest.requestId
  candidateId       必须 = DH 发起的 candidateId
  sharpeRatio       Double, nullable
  maxDrawdown       Double, nullable
  annualReturn      Double, nullable
  winRate           Double [0,1], nullable
  profitFactor      Double, nullable
  periodStart       ISO date
  periodEnd         ISO date
  verdict           PASS / FAIL / MARGINAL
  readyAt           ISO timestamp
  rawPayloadJson    NQ 端原始 JSON 字符串（DH 原样保存）

DH 侧消费链路（Batch 1 已补，本 Batch 校验联通）：
  1. NqFeedbackController -> NqFeedbackIngestionService -> 幂等去重
  2. BacktestResultReadyHandler 命中
  3. 反查 dh_research_runs by traceId（失配 -> 400 UNKNOWN_TRACE）
  4. 反查 candidate by candidateId（失配 -> 仅记录，不沉淀）
  5. ExperienceEntry 写入（successKey = (strategyPattern, regime)，score 由 BacktestResultScorer 计算）
  6. PheromoneEdge 更新（verdict=PASS 加权；FAIL 衰减）
  7. dh_checkpoint_entries(type=JUDGE_DECISION 之后续) 记录此次回测结果

不做：
  - 自动发布策略 / 自动 paper / 自动下单（JudgeDecision 仍是唯一最终出口）
  - 把 NQ verdict 当作 JudgeDecision 终态
  - 写任何 NQ 数据
```

### 3.6 幂等规则

```text
DH 出站幂等（本 Batch 新增）：
  键：       paramsHash（见 §3.3）
  窗口：     24 小时
  行为：     同 candidate + 同 paramsHash 在 24h 内重复触发：
             1. DhBacktestRequestService 短路，不再生成新 DhBacktestRequest
             2. 返回原 requestId 与 jobId（如已存在）
  存储：     InMemory 模式：ConcurrentHashMap<paramsHash, RequestRecord>，TTL 24h
             JDBC 模式（V4 启用时）：dh_backtest_request_outbox 表唯一约束 (candidate_id, params_hash)

NQ 侧返回 409 Conflict 处理：
  视为 idempotent 成功；
  本地记录 requestId <-> jobId 映射；
  不再重试；
  status 切到 ACCEPTED。

DH 入站幂等（已 Stage2-PoC-B5 落地）：
  键：       envelope.eventId
  存储：     dh_nq_feedback_events.event_id 唯一索引
  行为：     重放 -> 202 outcome=DUPLICATE
```

### 3.7 traceId / requestId / correlationId / sourceJobId 规则

```text
traceId
  长度：    32 位 hex（与 OpenTelemetry trace id 对齐）
  生成方：  DH ResearchRun 创建时
  传递：    DH -> NQ：HTTP header X-Trace-Id + body.traceId 双通道
            NQ -> DH（异步 feedback）：envelope.traceId 原样回传
  约束：    feedback envelope.traceId 必须能在 dh_research_runs.trace_id 找到
            否则 DH 入站返回 400 UNKNOWN_TRACE（已落地）

requestId
  长度：    UUIDv7
  生成方：  DH 出站 DhBacktestRequestService（一次回测请求 = 一个 requestId）
  传递：    DH -> NQ：body.requestId
            NQ -> DH（异步 feedback）：envelope.requestId 原样回传
  约束：    feedback envelope.requestId 必须 = 原 DhBacktestRequest.requestId

correlationId
  长度：    UUIDv7
  生成方：  DH 业务上下文（candidate 进入 paper / backtest 周期时分配）
  传递：    DH/NQ 双向必带；同 correlationId 串联多个 requestId/traceId
  约束：    feedback envelope.correlationId 与 DhBacktestRequest 的 correlationId 必须一致

sourceJobId
  生成方：  NQ 端（同步响应中的 jobId）
  传递：    DhBacktestRequestRepository 持久化 requestId <-> jobId 映射
            异步 feedback envelope.sourceJobId = jobId
  约束：    feedback envelope.sourceJobId 必填；DH 侧用于对账与排错

eventId
  生成方：  NQ outbox 行 ID（推荐 UUIDv7）
  传递：    feedback envelope.eventId
  约束：    DH 入站幂等键，唯一索引保证（已落地）
```

### 3.8 验收标准

```text
代码：
  - mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
  - 默认 profile 不连任何真实 HTTP（FakeNqBacktestClient 兜底）
  - decisionhub.stage3.nq.enabled=true 时 RealNqBacktestClient 装配，且仅 dh-connector.nq 内有 HTTP client
  - ArchUnit 全绿（默认 10 条 + 新增"非 dh-connector.nq 模块禁 HTTP client"规则）
  - Stage1 / Stage2 闭环测试 + Batch 1 经验沉淀测试全绿
  - 测试用例覆盖：
    * DhBacktestRequestService.build / validate / idempotent
    * 5xx 重试标记
    * 400 永久失败标记
    * 409 视为成功
    * @ConditionalOnProperty / @ConditionalOnMissingBean 装配（默认 Fake / 启用时 Real）

文档：
  - docs/current/STATUS.md            推进 "Stage3-B3 IMPLEMENT completed / Next: Stage3-B4 IMPLEMENT"
  - docs/current/WORKLOG.md           追加 Stage3-B3 IMPLEMENT 段
  - docs/current/TESTING.md           追加 Stage3-B3 验收记录
  - docs/current/API.md               补 "Stage3 计划 endpoint" 段说明 RealNqBacktestClient 与 stage3.nq.enabled flag
  - docs/current/DB_SCHEMA.md         若启用 V4，补 V4__stage3_dh_outbox.sql 表结构
  - README.md / AGENTS.md / docs/current/README.md 同步阶段
```

### 3.9 禁止事项

```text
- 修改 NQ 仓库
- 默认 profile 装配真实 HTTP client
- 在 dh-connector.nq 之外的任何模块引用 RestTemplate / WebClient / OkHttp / HttpURLConnection
- 在 DH 仓库实现回测撮合 / 数据加载 / 风控 / 滑点 / 手续费 / 复盘
- 把 NQ verdict 当作 JudgeDecision 终态
- 在 ingest 路径自动发布策略 / 自动 paper / 自动下单
- 修改 V1 / V2 / V3 已落地 migration 语义（V4 仅允许新增）
- 修改 contracts/openapi.yaml 已落地端点的语义
- 新增 NqFeedbackEventType
- 引入 Kronos / global-stock-data / TradingAgents Python
- 建设前端
```

### 3.10 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-B3-DH-Backtest-Adapter-IMPLEMENT。
当前阶段：Stage3-B2 PLAN completed / Next stage: Stage3-B3 IMPLEMENT。

目标：
  - 新增 DhBacktestRequestService（dh-usecase）：build / validate / 24h 幂等短路
  - 新增 DhBacktestRequestRepository 端口 + InMemoryDhBacktestRequestRepository
  - 新增 RealNqBacktestClient（dh-connector.nq）@ConditionalOnProperty(decisionhub.stage3.nq.enabled)
  - 修改 FakeNqBacktestClient 加 @ConditionalOnMissingBean 兜底
  - 修改 AgentRuntimeWiringConfig 装配新 bean
  - （可选）新增 V4__stage3_dh_outbox.sql；不修改 V1/V2/V3
  - 单测覆盖 build / validate / idempotent / 5xx 重试 / 400 永久失败 / 409 视为成功
  - ArchUnit 新增规则：非 dh-connector.nq 禁引用 RestTemplate / WebClient / OkHttp / HttpURLConnection

约束：
  - 默认 profile 不连真实 HTTP；不联调真实 NQ
  - 不修改 NQ 仓库；不修改 dh-domain；不新增事件类型
  - mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS

状态：Stage3-B3 IMPLEMENT completed -> Next: Stage3-B4 IMPLEMENT。
```

---

## 4. Batch 4: End-to-End Contract Test

### 4.1 目标

规划 DH/NQ 测试环境下的完整 contract test；不接实盘；不自动发布策略；不自动下单；联调用例只走 NQ test cluster + DH staging；产出 `docs/current/STAGE3_VERIFY_REPORT.md` 给出 GO/NO-GO 判定。

### 4.2 联调用例清单（7 个，与 STAGE3_TEST_PLAN §3 对齐）

```text
T1  入站正向                    NQ outbox 推 PAPER_RUN_CREATED
                                 -> DH 202 outcome=ACCEPTED
                                 -> dh_nq_feedback_events 有新行（event_id 已落唯一索引）
                                 -> ExperienceEntry 时间线写入

T2  入站幂等                    NQ outbox 重放同 eventId（同 payload）
                                 -> DH 202 outcome=DUPLICATE
                                 -> dh_nq_feedback_events 不新增行
                                 -> 经验沉淀路径不重新触发

T3  入站契约失败                NQ outbox 推 envelope，traceId 不在 dh_research_runs
                                 -> DH 400 + NqFeedbackErrorResponse{ errorCode=UNKNOWN_TRACE }
                                 -> NQ outbox 转 DEAD（不重试）
                                 -> dh_nq_feedback_events 无该行

T4  出站正向                    DH 提交 DhBacktestRequest（test universe 白名单 symbols）
                                 -> NQ test cluster 202 + DhBacktestRequestAccepted{ jobId }
                                 -> DhBacktestRequestRepository 记录 requestId <-> jobId
                                 -> DH 出站 outbox 状态切 SENT

T5  端到端反馈                  接 T4，NQ 完成 backtest
                                 -> NQ outbox 推 BACKTEST_RESULT_READY（envelope.sourceJobId = T4.jobId）
                                 -> DH ingest 命中 BacktestResultReadyHandler
                                 -> ExperienceEntry 写入（含 sharpeRatio / verdict）
                                 -> PheromoneEdge 加权或衰减
                                 -> dh_checkpoint_entries 记录此次回测结果

T6  出站幂等                    DH 在 24h 窗口内重复触发同 candidate + 同参数 backtest
                                 -> DhBacktestRequestService 短路（不再次发送）
                                 -> 返回原 requestId / jobId
                                 -> NQ test cluster 仅看到 1 次 inbound

T7  追踪字段对账                启动 1 个完整链路：DH 创建 ResearchRun -> 出站 backtest -> NQ 入队 -> NQ 回 result
                                 -> 4 字段（traceId / requestId / correlationId / sourceJobId）
                                    在 DH staging 日志、NQ test cluster 日志、DH 数据库（dh_research_runs / dh_nq_feedback_events）
                                    全部能完整对账
```

### 4.3 contract test 清单（在 DH 仓库内可跑的部分）

```text
DH 仓库内（默认 profile，可在 CI 跑）：
  - JsonSchemaPresenceTest                   16 份 schema 完整性 + additionalProperties + 8 枚举
  - NqFeedbackContractValidationTest         envelope / payload / sourceSystem / schemaVersion / traceId
  - NqFeedbackIdempotencyTest                eventId 幂等
  - NqFeedbackHandlerDispatchTest            8 handler 全派发
  - NqFeedbackHandlerExperienceTest（B1）    8 handler 经验沉淀
  - DhBacktestRequestServiceTest（B3）        build / validate / idempotent / 5xx / 400 / 409
  - V3MigrationPresenceTest                   V3 表 / ALTER / 唯一索引
  - JdbcSqlFragmentsTest                      JDBC 仓储 SQL 片段
  - JdbcNqFeedbackEventRepositoryTest         JDBC 幂等 + race
  - ArchitectureTest                          10 + Batch 1/3 视情况新增
  - Stage1ClosedLoopTest / Stage2ClosedLoopTest 全闭环回归
  - PostgresContainerSmokeTest                CI Docker 跑通

DH 仓库内（Stage3 联调 profile，@EnabledIfEnvironmentVariable(named="ENABLED_STAGE3", matches="true")）：
  - Stage3InboundFeedbackEndToEndTest         T1 / T2 / T3
  - Stage3OutboundBacktestEndToEndTest        T4 / T5 / T6
  - Stage3TraceCorrelationTest                T7
  - 默认 profile 这些用例 disabled，不影响日常 mvn test
  - 联调命令：ENABLED_STAGE3=true mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'

NQ 仓库内（NQ 团队执行，本仓库仅声明口径）：
  - NQ outbox 单测：8 种事件类型 envelope 生成
  - NQ outbox 契约测试：以 contracts/json-schema/*.schema.json 为输入
  - NQ outbox retry 单测：1s/5s/30s/5min/1h
  - NQ outbox idempotent 单测：相同 eventId 不重复入队
```

### 4.4 fake / stub 策略

```text
默认 profile：
  - FakeNqBacktestClient（@ConditionalOnMissingBean）兜底，不连任何真实 HTTP
  - InMemoryNqFeedbackEventRepository / DhBacktestRequestRepository 兜底
  - decisionhub.stage2.jdbc.enabled / decisionhub.stage3.nq.enabled 默认 false

联调 profile（application-stage3.yml）：
  - decisionhub.stage3.nq.enabled=true
  - decisionhub.stage3.nq.endpoint=https://<NQ test cluster>/api/ai/research/backtest-requests
  - decisionhub.stage2.jdbc.enabled=true（CI Docker）
  - 配置文件不入 prod profile；CI 通过环境变量注入 endpoint / token

stub 策略：
  - WireMock / MockWebServer 用于 dh-connector RealNqBacktestClient 单测（模拟 202 / 400 / 409 / 5xx）
  - 不引入 real NQ 调用进 DH 仓库 CI；CI 始终 mock NQ 端
  - staging 联调通过环境变量切到真实 NQ test cluster（仅 7 个用例）

数据隔离：
  - tenantId 前缀 "t-test-*"
  - traceId / correlationId 前缀 "stage3-"
  - symbols 限定测试白名单（不允许 live universe）
```

### 4.5 失败重试测试

```text
DH 出站重试（Batch 3 + Batch 4 联调验证）：
  - NQ 返回 502（5xx）-> DH 退避重试 1s / 5s / 30s / 5min / 1h；attempt=8 标记 DEAD
  - NQ 返回 504（timeout）-> 同 5xx 矩阵
  - NQ 返回 429 -> 退避重试，attempt 不计死信上限
  - NQ 返回 400 -> 立即 permanent-fail，不重试
  - NQ 返回 401 / 403 -> 立即 permanent-fail，告警
  - NQ 返回 409 -> 视为 idempotent 成功，停止重试
  - NQ 返回 202 -> SENT

NQ 出站重试（NQ 团队执行；DH 联调侧观察 dh_nq_feedback_events 与 attempts 的关系）：
  - DH 返回 503 -> NQ outbox 退避重试
  - DH 返回 5xx / timeout -> 同矩阵
  - DH 返回 400 -> NQ outbox 立即 DEAD
  - DH 返回 202（含 DUPLICATE）-> SENT

测试覆盖：
  - DhBacktestRequestServiceTest 单测覆盖 5xx / 400 / 409
  - Stage3OutboundBacktestEndToEndTest 联调覆盖 1 次成功 + 1 次 5xx 退避后成功
  - 联调侧不强制 attempt=8 全程；只验证退避逻辑生效（attempt=2 后成功即可）
```

### 4.6 幂等测试

```text
DH 入站幂等（T2 联调 + 单测覆盖）：
  - 同 eventId 二次 ingest -> 202 outcome=DUPLICATE
  - DuplicateKeyException race 用例已在 JdbcNqFeedbackEventRepositoryTest 覆盖
  - 联调验证：dh_nq_feedback_events 第二次不新增行

DH 出站幂等（T6 联调 + 单测覆盖）：
  - 同 candidate + 同 paramsHash 24h 内重复触发 -> DhBacktestRequestService 短路
  - 单测：DhBacktestRequestServiceTest.idempotent
  - 联调：NQ test cluster 入队记录数对比 +1 而不是 +2

NQ outbox 幂等（NQ 团队执行）：
  - 同 eventId 不重新入队
  - DH 端配合：观察 dh_nq_feedback_events 不会因 NQ 内部去重失败而重复

eventId 与 requestId 解耦：
  - eventId 单事件生命周期；requestId 单次回测请求生命周期
  - 同一 requestId 可对应多个事件（QUEUED / RESULT_READY），eventId 各自独立
```

### 4.7 边界安全测试

```text
ArchUnit（必须全绿）：
  - 默认 10 条规则
  - Batch 1 视情况新增（feedback 不引用 connector.nq / HTTP client）
  - Batch 3 视情况新增（非 dh-connector.nq 禁 HTTP client）

JsonSchemaPresenceTest：
  - 16 份 schema 文件存在
  - additionalProperties=false
  - 8 枚举值
  - 无 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute 关键词

V3MigrationPresenceTest（已落地）：
  - V3 表 / ALTER / 唯一索引
  - 无 orders / trades / fills / positions / live_

人工核对清单（FREEZE 前必须扫描）：
  - 不出现 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute 关键词
  - 不出现 /orders /trades /live API 路径
  - 不出现 Python / TradingAgents 引用
  - 不出现真实 HTTP client 引用（除 dh-connector.nq）
  - 不出现 Kronos / global-stock-data 真实接入代码

联调环境隔离：
  - NQ 提供 test cluster，专用 DB / 配置 / 行情源；与生产物理隔离
  - DH staging 配置 endpoint 仅指向 test cluster
  - 测试租户 tenantId 前缀 t-test-*
  - 联调用例只包含 BACKTEST_RESULT_READY / PAPER_RUN_* feedback；
    禁止任何下单 / 实盘 / 资金调用
  - DhBacktestRequest 不指向 live universe；symbols 限定测试白名单

回滚预案：
  - 联调失败 1h 内回滚 staging 配置到 stage2 InMemory 模式
  - decisionhub.stage3.nq.enabled=false 立即生效
  - 数据回滚：dh_nq_feedback_events 联调期间标记 tenantId=t-test-*，非生产数据
```

### 4.8 验收命令

```text
# DH 仓库默认 profile（每个 Batch 完成都跑）
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

# CI Docker 环境（FREEZE 前必须跑通）
mvn test -Dtest='PostgresContainerSmokeTest'

# Stage3 联调（仅 staging 环境跑，默认 profile 不跑）
ENABLED_STAGE3=true \
  mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'

# 全部联调用例 GO 后产出
docs/current/STAGE3_VERIFY_REPORT.md         Verdict: GO / NO-GO
```

### 4.9 出口标准（与 Stage2-PoC VERIFY 同体例）

```text
- 7 个联调用例全绿（T1-T7）
- 默认 profile mvn test 全绿（不含联调用例）
- PostgresContainerSmokeTest 在 CI Docker 通过
- ArchUnit 全绿
- 边界安全人工核对全绿
- STAGE3_VERIFY_REPORT.md Verdict = GO

GO 后进入 Stage3-FREEZE：
  docs/current/* 完整拷贝到 docs/gates/dh-stage3/
  README.md / AGENTS.md / docs/current/STATUS.md 切到
    "Stage3 FREEZE completed / Next: DH-FREEZE"

NO-GO 后回到对应 Batch 的 PLAN，禁止跳跃或硬抹平。
```

### 4.10 禁止事项

```text
- 接实盘 / 自动发布策略 / 自动下单
- DH 仓库实现 NQ outbox / NQ 端逻辑
- 修改 contracts / migration / OpenAPI 语义
- 把 NQ verdict 当作 JudgeDecision 终态
- 在 ingest 路径触发新 NQ 请求（避免回环）
- 把 paper 触发包成 backtest request 提交（绕风控）
- 用 prod tenant 做联调
- 默认 profile 装配 RealNqBacktestClient
- 联调失败后强行硬抹平（必须回 Batch PLAN）
```

### 4.11 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-B4-E2E-Contract-Test。
当前阶段：Stage3-B3 IMPLEMENT completed / Next stage: Stage3-B4 IMPLEMENT。

目标：
  - 在 NQ test cluster + DH staging 跑通 7 个联调用例（T1-T7）
  - 默认 profile 仍走 Fake；联调用例以 @EnabledIfEnvironmentVariable(named="ENABLED_STAGE3") 隔离
  - 在 CI Docker 跑通 PostgresContainerSmokeTest，回填 docs/current/TESTING.md
  - 写 docs/current/STAGE3_VERIFY_REPORT.md，给出 GO / NO-GO 判定

约束：
  - 不接实盘 / 不自动发布 / 不下单 / 不绕风控 / 不修改 NQ 仓库
  - 联调环境物理隔离 + tenantId 前缀 t-test-* + symbols 白名单
  - 默认 profile mvn test 全绿；联调环境跑 Stage3*EndToEnd* 全绿

验收命令：
  mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
  mvn test -Dtest='PostgresContainerSmokeTest'
  ENABLED_STAGE3=true mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'

状态：Stage3-B4 VERIFY completed -> Next: Stage3-FREEZE。
```

---

## 5. Stage3 整体下一步与冻结路径

```text
Stage3-WO completed       -> Stage3-B1 IMPLEMENT
Stage3-B1 IMPLEMENT       -> Stage3-B2 PLAN
Stage3-B2 PLAN            -> Stage3-B3 IMPLEMENT
Stage3-B3 IMPLEMENT       -> Stage3-B4 IMPLEMENT
Stage3-B4 VERIFY (GO)     -> Stage3-FREEZE
Stage3-FREEZE             -> docs/gates/dh-stage3/* 落盘 + 文档状态切 "Stage3 FREEZE completed / Next: DH-FREEZE"
DH-FREEZE                 -> Decision Hub Agent Decision Layer v1（长期维护）
```

任意 Batch 的验收失败必须回到该 Batch 的 PLAN，禁止跳跃或绕过 Batch；
任一 Batch 触碰 §0.3 通用硬边界视为 IMPLEMENT 失败，必须回滚。
