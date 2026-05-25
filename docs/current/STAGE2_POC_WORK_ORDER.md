# Stage2-PoC Work Order

> Status: PLAN (待 WO 阶段细化为可执行工单)
> Created: 2026-05-25
> Depends on: Stage1-FREEZE completed

## 1. 目标

实现 Stage2-PoC：NQ 真实事件回流正式契约 + 工具接口预留 + TradingAgents 轻量设计。

## 2. 允许改动范围

```text
dh-domain          新增领域对象（ForecastArtifact, ExternalMarketSnapshot, ReflectionCheckpoint, ForecastRequest, MarketSnapshotRequest）
dh-usecase         AgentTaskPlanner 动态选边；NqFeedbackIngestionService 契约校验增强
dh-connector       tools.ForecastToolPort + research.ResearchDataAdapter + Fake 实现
dh-infra           InMemory -> JDBC 仓储替换 + 新仓储
dh-api             新增 /api/ai/tools/forecast, /api/ai/research/snapshots, /api/ai/research-runs/{id}/reflections
dh-app             WiringConfig 补充新 bean
db/migration       V3__stage2_poc_tools.sql
contracts/         openapi.yaml 新增端点 + JSON Schema
docs/current/      状态更新
golden_cases/      Stage2 闭环用例
```

## 3. 不做事项

```text
不修改 NQ 仓库
不接真实 NQ API
不接真实 Kronos 推理服务
不接真实 global-stock-data 拉取
不引入 TradingAgents Python 代码
不实现真实下单
不绕过 NQ 风控
不重写 NQ 回测核心
不建设前端
不删除 Stage1 已落地代码
```

## 4. 模块清单

### Batch 1: 领域层 + 接口骨架

```text
[1.1] dh-domain/tool/ForecastArtifact.java
[1.2] dh-domain/tool/ForecastRequest.java
[1.3] dh-domain/research/ExternalMarketSnapshot.java
[1.4] dh-domain/research/MarketSnapshotRequest.java
[1.5] dh-domain/agent/ReflectionCheckpoint.java
[1.6] dh-domain/research/ResearchRun 新增 regime/topic/plannerStrategy 字段
[1.7] dh-connector/tools/ForecastToolPort.java（接口）
[1.8] dh-connector/tools/FakeForecastToolAdapter.java
[1.9] dh-connector/research/ResearchDataAdapter.java（接口）
[1.10] dh-connector/research/FakeResearchDataAdapter.java
[1.11] dh-connector/research/ResearchSnapshotStore.java（端口接口）
```

### Batch 2: 用例层增强

```text
[2.1] AgentTaskPlanner 动态选边逻辑（按 topic/regime）
[2.2] NqFeedbackIngestionService 契约校验增强（eventType 枚举、schema 校验、traceId 关联）
[2.3] ReflectionCheckpointService（写入/查询 checkpoint）
[2.4] ForecastToolService（调用 ForecastToolPort，保存 artifact）
[2.5] ResearchDataService（调用 ResearchDataAdapter，保存 snapshot）
```

### Batch 3: 持久化层

```text
[3.1] db/migration/V3__stage2_poc_tools.sql
[3.2] JdbcResearchRunRepository
[3.3] JdbcAgentTaskRepository
[3.4] JdbcStrategyCandidateRepository
[3.5] JdbcJudgeDecisionRepository
[3.6] JdbcExperienceEntryRepository
[3.7] JdbcNqFeedbackEventRepository
[3.8] JdbcForecastArtifactRepository
[3.9] JdbcMarketSnapshotRepository
[3.10] JdbcReflectionCheckpointRepository
```

### Batch 4: API 层

```text
[4.1] ForecastToolController (POST /api/ai/tools/forecast)
[4.2] ResearchSnapshotController (POST/GET /api/ai/research/snapshots)
[4.3] ReflectionController (GET /api/ai/research-runs/{id}/reflections)
[4.4] NqFeedbackController 增强校验逻辑
[4.5] contracts/openapi.yaml 更新
```

### Batch 5: 测试 + 装配

```text
[5.1] Stage2ClosedLoopTest
[5.2] NqFeedbackContractValidationTest
[5.3] ForecastToolPortTest
[5.4] ResearchDataAdapterTest
[5.5] AgentTaskPlannerDynamicTest
[5.6] ReflectionCheckpointTest
[5.7] ArchitectureTest 新增 2-3 条规则
[5.8] dh-app/AgentRuntimeWiringConfig 补充新 bean
```

## 5. API 要求

见 `docs/current/STAGE2_POC_API_PLAN.md`。

## 6. DB 要求

见 `docs/current/STAGE2_POC_DB_PLAN.md`。

## 7. 测试要求

见 `docs/current/STAGE2_POC_TEST_PLAN.md`。

## 8. 验收标准

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
Stage2ClosedLoopTest 通过
NqFeedbackContractValidationTest 通过
ArchUnit 规则全部绿色（旧 5 条 + 新增）
contracts/openapi.yaml 与实现一致
V3 迁移脚本存在且与 DB_SCHEMA 文档一致
docs/current/STATUS.md 更新为 Stage2-PoC IMPLEMENT completed
```

## 9. 风险点

```text
1. JDBC 实现可能暴露 payloadJson 序列化兼容问题
2. AgentTaskPlanner 动态选边如果过度设计会增加测试复杂度
3. V3 ALTER 语句需要幂等（IF NOT EXISTS）
4. Fake adapter 返回的 mock 数据需要足够真实以验证下游逻辑
5. NQ 端契约未正式确认，Stage2 仍用 Fake，但格式必须稳定
```

## 10. Codex 开工提示词

```text
你在 ling5477/decision-hub 仓库 dev 分支上工作。任务名：Stage2-PoC IMPLEMENT。

目标：按 docs/current/STAGE2_POC_WORK_ORDER.md 实现 Stage2-PoC。

开始前必须读取：
- README.md
- AGENTS.md
- docs/current/STATUS.md
- docs/current/STAGE2_POC_PLAN.md
- docs/current/STAGE2_POC_WORK_ORDER.md
- docs/current/STAGE2_POC_API_PLAN.md
- docs/current/STAGE2_POC_CONTRACT_PLAN.md
- docs/current/STAGE2_POC_DB_PLAN.md
- docs/current/STAGE2_POC_TEST_PLAN.md

按 Batch 顺序实现：Batch1 -> Batch2 -> Batch3 -> Batch4 -> Batch5。
每个 Batch 完成后运行 mvn test 确认不破坏现有测试。

严格边界：
- 不修改 NQ 仓库。
- 不接真实 NQ API（保留 Fake fallback）。
- 不接真实 Kronos 推理服务。
- 不接真实 global-stock-data 拉取。
- 不引入 TradingAgents Python 代码。
- 不实现真实下单。
- 不绕过 NQ 风控。
- 不重写 NQ 回测核心。
- 不建设前端。

验收命令：
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

成功标准：
- BUILD SUCCESS
- Stage2ClosedLoopTest 通过
- NqFeedbackContractValidationTest 通过
- ArchUnit 规则保持绿色
- contracts/openapi.yaml 与实现一致
- V3 迁移脚本存在
```
