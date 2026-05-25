# Stage2-PoC Gate — Frozen Snapshot

> Stage: Stage2-PoC
> Status: FREEZE completed
> Source snapshot: docs/current at Stage2-PoC VERIFY completed
> Verification: mvn test passed, 122 tests / 0 failures / 0 errors
> ArchUnit: 10/10 passed
> Verdict: GO
> Next: Stage3-PLAN

## 冻结声明

本目录是 Stage2-PoC 完成后的 `docs/current/` 快照，冻结于 2026-05-26。

冻结后不得修改本目录内容。后续变更只能发生在 `docs/current/`。

## 验收命令与结果

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS
Total: 122 tests / 0 failures / 0 errors / 0 skipped

dh-domain   35 tests   (JsonSchemaPresenceTest 5)
dh-connector 9 tests   (Fake adapter/store 全绿)
dh-usecase  47 tests   (Stage2ClosedLoopTest 2 / ResearchRunStage1ClosedLoopTest 1)
dh-infra     9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
dh-api       7 tests   (NqFeedbackControllerWebMvcTest 7)
dh-app      15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
```

完整验证报告：`STAGE2_POC_VERIFY_REPORT.md`（同目录快照副本）。

## Stage2-PoC 交付物

```text
Batch 1: 契约 + 领域
  - 8 个 NQ feedback payload + envelope + 3 个 DhBacktest* 契约
  - Forecast / ExternalMarketSnapshot / Reflection / Checkpoint 领域模型
  - 16 份 contracts/json-schema/*.schema.json
  - contracts/openapi.yaml components 全量补齐

Batch 2: NQ -> DH feedback ingestion
  - NqFeedbackController + EnvelopeRequest + AcceptedResponse + ErrorResponse
  - NqFeedbackIngestionService（Validator + Router + 8 个 Handler + 幂等）
  - 端点 POST /api/ai/feedback/nq，202 + Accepted / 400 + Error

Batch 3: dh-connector 工具/研究端口
  - ForecastToolPort + FakeForecastToolAdapter
  - ResearchDataAdapter + FakeResearchDataAdapter
  - ResearchSnapshotStore + InMemoryResearchSnapshotStore

Batch 4: Reflection / Checkpoint / Dynamic Planner
  - PlannerStrategy + Resolver + Registry + 4 个 StrategyHandler
  - DynamicAgentTaskPlanner + DefaultReflectionCheckpointService
  - InMemory Reflection / Checkpoint 仓储
  - JudgeDecision 仍是唯一最终出口

Batch 5: JDBC + Tests + Docs 收口
  - Flyway V3__stage2_poc_tools.sql（4 新表 + 2 ALTER）
  - 5 个 Stage2 JDBC 仓储 + Stage2JdbcWiringConfig + @ConditionalOnMissingBean 兜底
  - ArchUnit 扩到 10 条规则
  - OpenAPI 对齐 + Stage2ClosedLoopTest 全闭环
```

## Stage2-PoC VERIFY 修正项（已应用，本快照已包含）

```text
contracts/openapi.yaml      /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
                            / 400 + NqFeedbackErrorResponse，并补两个 schema
docs/current/DB_SCHEMA.md   修正 V2 文件名为 V2__dh_agent_runtime.sql
docs/current/API.md         把已实现的 7 条 research-runs 端点移入 "已实现端点"
docs/current/STAGE2_POC_VERIFY_REPORT.md  新增（Verdict: GO）
```

## 严格边界（Stage2-PoC 全程坚持，FREEZE 后继续约束）

```text
不修改 NQ 仓库            不接真实 NQ API
不接真实 Kronos          不接真实 global-stock-data
不引入 TradingAgents Python   不实现真实下单
不绕过 NQ 风控            不重写 NQ 回测核心
不建设前端                不引入重型群体智能数学优化器
不把 Kronos / TradingAgents / global-stock-data 直接复制进 DH/NQ
```

## 下一阶段

```text
Stage3-PLAN：仅做 NQ 真实 feedback / backtest request 联调规划。
             不允许直接实现 Stage3 功能；
             不允许修改 NQ 交易核心；
             不允许接实盘自动交易。
```
