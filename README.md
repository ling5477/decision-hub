# Decision Hub

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层。

它负责 Agent 编排、候选方案生成、多路径探索、历史反馈强化、策略评分、冲突仲裁、报告生成和辅助决策。

它不负责交易核心、订单状态机、风控执行、正式回测内核、模拟盘/实盘执行、账本审计和交易事实沉淀。这些能力由 NexusQuant 承担。

## 当前阶段

```text
Current stage: Integration-0 safety gate CLOSED / ACCEPTED
Next stage:    DH-GATEK-DECISION-PIPELINE-MVP-PLAN
Source of truth: docs/current
```

## 文档入口

当前事实源：

```text
docs/current/README.md
```

开工前必须读取：

```text
AGENTS.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_STATUS.md
docs/current/STAGE1_CLOSE_WORKLOG.md
```

## 文档结构

```text
docs/current/      当前事实源
docs/gates/        历史阶段快照（Stage1 冻结后归档）
docs/codex/        当前活跃计划（plans/_active/） + 历史归档（plans/_archive/）
contracts/         对外协议、Schema、事件契约
golden_cases/      黄金样例与回归用例
```

## Codex workflow 入口

```text
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
docs/current/DH_WORKFLOW_ROUTER_SKILL.md
docs/current/DH_CODEX_TASK_TEMPLATES.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```

所有 Codex / Agent 任务先使用 `nq-dh-workflow-router` 做任务分类、插件路由、scope 收口和输出格式统一。所有 DH 文档治理、docs/current、Gate/Phase/Stage planning、work order、acceptance/freeze/close review、WORKLOG/TESTING/STATUS/ROADMAP/API 同步和 Decision Pipeline MVP 文档规划任务必须使用 `dh-docs-writer`。标准输出字段固定为 `Task classification`、`Plugins selected`、`Scope`、`Files inspected`、`Files changed`、`Findings`、`Validation`、`Risks`、`Next concrete action`；`Summary` 不是必填字段。

## 当前工程边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
```

## 标准工作流

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

当前下一步只能进入：

```text
DH-GATEK-DECISION-PIPELINE-MVP-PLAN。
- Stage3-B3 已于 2026-05-26 完成：DH 端 backtest adapter 可插拔骨架落地（Fake / Disabled 三层 gate，
  无真实 HTTP，无 RealNqBacktestClient；190 tests 全绿 / ArchUnit 12/12）。
- Integration-0 safety gate 已 CLOSED / ACCEPTED；P1-4 residual、header alignment、timestamp alignment、
  code reality audit blockers 已关闭或修复。
- Decision Pipeline MVP PLAN 只允许规划 DecisionRequest / DecisionOutput / DecisionOrchestrator /
  Snapshot / Trace / Replay / Audit 的只读建议和 fail-closed 边界。
- Stage3-B2 / NQ Feedback Outbox / 真实 HTTP / event / NQ client / RealClient / real provider
  均为 historical / superseded / deferred / gated，不是当前 next，不允许作为当前实现任务。
- 旧 NQ-DH-GATEK-INTEGRATION1-PLAN-PACK：SUPERSEDED / REBASE_REQUIRED。
- NQ 已进入 GateN；后续 Integration-1 必须基于 GateN rebase 重新规划。
- NQ integration not started；Integration-1 NOT STARTED；Runtime integration NOT STARTED；DH integrated NO。
- AI / Agent runtime NOT STARTED；LangGraph runtime NOT STARTED；LIVE DISABLED。
- RealClient forbidden；real provider forbidden；LIVE trading forbidden；NQ mutation forbidden。
严格禁止：接 NQ runtime / 修改 NQ 仓库 / 接实盘 / 自动下单 / 绕风控 / 重写回测核心 / 引入 TradingAgents Python / 接 LangGraph runtime。
```

冻结快照：`docs/gates/dh-stage3-plan/`（33 个文件含 10 份 STAGE3_*.md；不得直接修改）

## 构建与验证

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

## DH 与 NQ 的关系

```text
NQ Console -> DH API -> Agent Runtime / Judge / Memory / NQ Adapter
NQ Console -> NQ API -> Backtest / Risk / Paper / Live / Audit
NQ -> DH Feedback -> Experience / Pheromone
```

DH 输出结构化建议和报告，NQ 执行正式交易能力。
