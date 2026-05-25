# Stage2-PoC Plan

> Status: PLAN
> Created: 2026-05-25
> Depends on: Stage1-FREEZE completed

## 1. 目标

把 Stage1 的 Fake adapter 与 InMemory 仓储升级为：

1. NQ feedback event ingestion 正式契约（HTTP + 事件结构）。
2. Kronos ForecastToolPort / ForecastArtifact 接口预留（不接真实推理服务）。
3. global-stock-data ResearchDataAdapter / ExternalMarketSnapshot / ResearchSnapshotStore 接口预留。
4. TradingAgents 风格 reflection / checkpoint / dynamic planner 轻量设计。

## 2. 允许改动范围

```text
dh-domain          新增 ForecastArtifact / ExternalMarketSnapshot 领域对象
dh-usecase         AgentTaskPlanner 动态选边逻辑；reflection/checkpoint 字段约定
dh-connector       tools.ForecastToolPort + research.ResearchDataAdapter 接口骨架
dh-memory          无新增（Stage1 已就位）
dh-eval            无新增
dh-infra           InMemory -> JDBC 仓储替换（V2 表已就位）
dh-api             新增 /api/ai/tools/forecast（预留）、/api/ai/research/snapshots（预留）
dh-app             WiringConfig 补充新 bean
db/migration       V3__stage2_poc_tools.sql（forecast_artifacts + market_snapshots 表）
contracts/         openapi.yaml 新增 Stage2 端点
docs/current/      本轮 PLAN 文档
golden_cases/      新增 Stage2 闭环用例描述
```

## 3. 禁止改动范围

```text
不修改 NQ 仓库
不接真实 NQ API（保留 Fake fallback）
不接真实 Kronos 推理服务
不接真实 global-stock-data 拉取
不引入 TradingAgents Python 代码
不实现真实下单
不绕过 NQ 风控
不重写 NQ 回测核心
不建设前端
不引入 BCO/ACO/GWO 重型数学优化器
不删除 Stage1 已落地的领域模型与用例服务
```

## 4. 模块设计概览

### 4.1 NQ Feedback Event Ingestion

```text
现状：FakeNqFeedbackClient 硬编码返回
目标：定义正式 NqFeedbackEvent 契约（JSON Schema），
      NqFeedbackIngestionService 接收 HTTP POST，
      校验 schema -> 持久化 raw payload -> 触发经验更新
保留：FakeNqFeedbackClient 作为测试 fallback
```

### 4.2 Kronos ForecastToolPort

```text
新增：dh-connector/tools/ForecastToolPort.java（接口）
新增：dh-connector/tools/FakeForecastToolAdapter.java（Fake 实现）
新增：dh-domain/tool/ForecastArtifact.java（领域对象）
新增：dh-domain/tool/ForecastRequest.java（请求 DTO）
目的：为未来 Kronos 时序预测接入预留标准端口
```

### 4.3 global-stock-data ResearchDataAdapter

```text
新增：dh-connector/research/ResearchDataAdapter.java（接口）
新增：dh-connector/research/FakeResearchDataAdapter.java（Fake 实现）
新增：dh-domain/research/ExternalMarketSnapshot.java（领域对象）
新增：dh-connector/research/ResearchSnapshotStore.java（存储端口）
目的：为未来 global-stock-data 市场数据接入预留标准端口
```

### 4.4 TradingAgents 风格轻量设计

```text
修改：AgentTaskPlanner 支持 topic/regime 动态选边
约定：ResearchRun.payloadJson 增加 reflection / checkpoint 字段命名规范
新增：dh-domain/agent/ReflectionCheckpoint.java（值对象）
目的：吸收 TradingAgents 的 reflection + checkpoint 思想，不引入 Python
```

### 4.5 InMemory -> JDBC 仓储替换

```text
修改：dh-infra 新增 JDBC 实现类对应 Stage1 的 6 个 repository 端口
依赖：V2__dh_agent_runtime.sql 已就位（10 张表）
新增：V3__stage2_poc_tools.sql（forecast_artifacts + external_market_snapshots）
```

## 5. 验收标准

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
ArchUnit 4 条规则保持绿色
新增 Stage2ClosedLoopTest：
  create run -> planner 动态选边 -> forecast tool 调用(fake) -> research data 调用(fake)
  -> candidate -> judge -> NQ feedback(正式契约格式) -> experience 更新
  -> reflection checkpoint 写入 payloadJson
contracts/openapi.yaml 新增端点与 JSON Schema 一致
V3 迁移脚本与 DB_SCHEMA 文档一致
```

## 6. 风险点

```text
1. NQ 端 /api/ai/* 不存在：Stage2 仍用 Fake，但契约格式必须与 NQ 团队对齐后才能去 Fake。
2. Kronos 推理延迟未知：ForecastToolPort 必须支持超时 + fallback。
3. global-stock-data 数据格式未稳定：ResearchDataAdapter 只定义接口，不绑定具体字段。
4. InMemory -> JDBC 切换可能暴露序列化问题：payloadJson 必须有 schema 版本字段。
5. TradingAgents reflection 字段如果过度设计会污染 ResearchRun：只加最小字段约定。
```

## 7. 下一步

本 PLAN 完成后进入 Stage2-PoC WO（Work Order），产出可执行工单。

## 8. IMPLEMENT 开工提示词

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
- ArchUnit 规则保持绿色
- contracts/openapi.yaml 与实现一致
- V3 迁移脚本存在且与 DB_SCHEMA 一致
```
