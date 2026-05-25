# Decision Hub Current Work Order

> Current stage: Stage1 (Boundary Freeze + Agent Runtime Skeleton) completed
> Closed:        Stage1-CLOSE（旧链路 @Deprecated + 文档单源 + ArchUnit 兜底）
> Next stage:    Stage2-PoC (NQ 真实事件回流 + 工具接口预留)

## 1. 当前目标

Stage1-CLOSE 已完成，等待开 Stage2-PoC 的工单。

## 2. Stage1-CLOSE 已完成范围

```text
deprecation：旧 domain.run / api.run / usecase.facade / usecase.run / usecase.gate /
             usecase.contract / dh-providers 全部 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
迁移：     api.run.RunController -> api.legacy.run.RunController，REST 路径 /runs -> /legacy/runs
契约：     contracts/openapi.yaml 中 /runs -> /legacy/runs，并标 deprecated
文档单源： 根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
codex：    docs/codex/plans/_active/STATUS.json 切到 Stage1，老 M1 plan 归档到 _archive/2026-02-04_M1/
ArchUnit： 新增 4 条规则保护新边界
pom：      dh-eval parent 修回 dh-bom
```

## 3. Stage2-PoC（下一轮）

下一份工单将以"NQ 真实事件回流 + 工具接口预留"为主题；启动前必须先在 docs/current/ 下出
`STAGE2_POC_WORK_ORDER.md` 由 Plan 阶段交付。

下一轮工单候选范围（待 Plan 收敛）：

```text
NqFeedbackClient / NqBacktestClient 接通真实 HTTP/事件（保留 Fake fallback）
ForecastToolPort（Kronos 接口骨架，不接真实模型）
ResearchDataAdapter / ExternalMarketSnapshot / ResearchSnapshotStore（global-stock-data 预留）
AgentTaskPlanner 动态选边（按 topic/regime）
ResearchRun.payloadJson 引入 reflection / checkpoint 字段命名约定
dh-infra：把 InMemory 仓储替换为 JDBC 实现（V2 已就位）
```

## 4. 不做事项（持续硬约束）

```text
不修改 NQ 仓库交易核心
不实现真实下单
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不建设第二套完整前端
不引入 BCO/ACO/GWO 等重型数学优化器
不把 Kronos / TradingAgents / global-stock-data 整体复制进 DH/NQ
```

## 5. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage2-PoC-PLAN。

目标：把 Stage1 的 Fake NQ adapter 与内存仓储升级为"NQ 真实事件回流 + 工具接口预留"；
不动 Stage1 已落地的领域模型与用例服务边界，不修改 NQ 仓库，不实现真实下单。

请先在 docs/current/STAGE2_POC_WORK_ORDER.md 中产出：
1. 与 NQ 团队需要协调的 NqFeedbackEvent / NqBacktestRequest 契约清单（OpenAPI 草稿）。
2. dh-connector.tools.ForecastToolPort 与 ForecastArtifact 的接口签名（不接 Python）。
3. dh-connector.research.{ResearchDataAdapter, ExternalMarketSnapshot, ResearchSnapshotStore} 接口签名。
4. AgentTaskPlanner 动态选边的最小决策表。
5. dh-infra：InMemory 仓储替换为 JDBC 实现的迁移清单（V3 候选 + 既有 V2 表对齐）。

不要写业务代码。本轮只产出 WORK_ORDER 草稿与 PLAN_QUEUE。
```
