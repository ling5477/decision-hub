# Decision Hub Roadmap

## 1. 总路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。

路线：

```text
DH-REFIT-1: 文档结构与边界统一
DH-REFIT-2: Agent Runtime Skeleton
DH-REFIT-3: NQ Adapter 最小闭环
DH-REFIT-4: Experience / Pheromone 反馈强化
DH-REFIT-5: NQ Console AI 页面接入
DH-FREEZE: 冻结 DH Agent Decision Layer v1
```

## 2. DH-REFIT-1

目标：重构 DH 文档结构，保持与 NQ 一致。

交付：

```text
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/WORKLOG.md
docs/current/ARCHITECTURE.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/TESTING.md
docs/current/DOCS_STRUCTURE.md
docs/gates/README.md
README.md
AGENTS.md
```

## 3. DH-REFIT-2

目标：建立 Agent Runtime Skeleton。

交付：

```text
ResearchRun
AgentTask
TaskNode
AgentArtifact
StrategyCandidate
JudgeDecision
ExperienceEntry
PheromoneEdge
NqFeedbackEvent
```

验收：`mvn test` 通过，最小运行链路可测试。

## 4. DH-REFIT-3

目标：打通 DH 到 NQ 的 Adapter 最小闭环。

交付：

```text
NqBacktestClient
NqFeedbackClient
NqContractVerifier
FakeNqAdapter
```

验收：DH 能发起 fake NQ backtest request，并接收 fake NQ feedback。

## 5. DH-REFIT-4

目标：轻量蚁群反馈强化。

交付：

```text
ExperienceStore
PheromoneStore
FailureCaseStore
FeedbackScorer
```

验收：NQ feedback 能改变后续候选排序权重。

## 6. DH-REFIT-5

目标：NQ Console 接入 DH AI 页面。

交付页面：

```text
/ai/tasks
/ai/tasks/:id
/ai/candidates
/ai/experiences
/ai/reports
```

验收：NQ Console 统一前端入口，不建设 DH 完整业务前端。

## 7. DH-FREEZE

目标：冻结 DH Agent Decision Layer v1。

验收：

```text
文档快照进入 docs/gates/dh-agent-v1
回归测试通过
边界检查通过
无 AI 直接交易能力
无 NQ 核心污染
```
