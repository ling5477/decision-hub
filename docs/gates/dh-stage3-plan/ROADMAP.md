# Decision Hub Roadmap

## 1. 总路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。

路线：

```text
DH-REFIT-1:   文档结构与边界统一                          [completed]
Stage1:       Boundary Freeze + Agent Runtime Skeleton    [completed]
Stage1-CLOSE: 旧链路 @Deprecated + 文档单源 + ArchUnit    [completed]
Stage2-PoC:   NQ 真实事件回流 + 工具接口预留              [next]
Stage3:       NQ Console AI 页面接入                      [later]
DH-FREEZE:    冻结 DH Agent Decision Layer v1             [later]
```

## 2. Stage1（已完成）

目标：建立 Agent Runtime Skeleton。

交付：

```text
domain.research.ResearchRun (+Status)
domain.agent.AgentTask / TaskNode / AgentRole / AgentArtifact
domain.candidate.StrategyCandidate / SignalProposal
domain.judge.JudgeDecision / RiskReview / DecisionRecommendation
domain.experience.ExperienceEntry / PheromoneEdge
domain.feedback.NqFeedbackEvent / FeedbackSource
usecase.agent.* + impl + InMemory repositories
memory.agent.* + InMemory stores
eval.agent.* + rule scorers
connector.nq.* + fake adapters
api.research.ResearchRunController（POST/GET /api/ai/research-runs/...）
api.feedback.NqFeedbackController（POST /api/ai/feedback/nq）
dh-app/AgentRuntimeWiringConfig + V2__dh_agent_runtime.sql
ResearchRunStage1ClosedLoopTest（create→start→candidate→judge→feedback→experience）
```

验收：`mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false` BUILD SUCCESS。

## 3. Stage1-CLOSE（已完成）

目标：让 Stage1 成为仓库唯一主链路，文档单源，ArchUnit 兜底新边界。

交付：

```text
domain.run.* / api.legacy.run.* / usecase.facade / usecase.run / usecase.gate /
usecase.contract / dh-providers 全部 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
RunController @RequestMapping 从 /runs 改为 /legacy/runs
contracts/openapi.yaml 中 /runs 路径标 deprecated 并迁到 /legacy/runs
docs/current 全套文档同步到 Stage1 完成态
docs/codex/plans/_active/STATUS.json 切到 2026-05-25_Stage1_agent_runtime_skeleton
docs/codex/plans/_archive/2026-02-04_M1/ 归档老 M1 mock-provider 计划
ArchitectureTest 新增 4 条规则（domain 独立 / connector.nq 禁字 / usecase.agent 禁 providers / api 禁 order-trade-live 路径）
dh-eval/pom.xml parent 修回 dh-bom
dep-tree.txt 重新生成
```

## 4. Stage2-PoC（下一阶段）

目标：把 Stage1 的 Fake 接入升级为"NQ 真实事件回流 + 工具接口预留"。

范围：

```text
NqFeedbackClient 接通真实 HTTP/事件（仍保留 Fake 作为测试 fallback）
NqBacktestClient 接通真实 NQ /api/ai/research/backtest-requests
dh-connector.tools.ForecastToolPort + dh-domain ForecastArtifact（Kronos 接口预留，不接真实模型）
dh-connector.research.{ResearchDataAdapter, ExternalMarketSnapshot, ResearchSnapshotStore}（global-stock-data 预留）
AgentTaskPlanner 支持基于 topic/regime 动态选边（TradingAgents 思想吸收）
ResearchRun.payloadJson 增加 reflection / checkpoint 字段命名约定
dh-infra：把 InMemory 仓储替换为 JDBC 实现
```

验收：

```text
端到端：DH 主动发起 fake/真实 NQ backtest request -> NQ 回流 feedback -> DH 经验强化
mvn test 全绿
ArchUnit 4 条规则保持绿色
DB_SCHEMA 与 V2 + V3 迁移脚本一致
```

## 5. Stage3：NQ Console 接入

目标：NQ Console 接入 DH AI 页面。

交付页面（落在 NQ frontend，不在 DH）：

```text
/ai/tasks
/ai/tasks/:id
/ai/candidates
/ai/experiences
/ai/reports
```

验收：NQ Console 统一前端入口，不建设 DH 完整业务前端。

## 6. DH-FREEZE

目标：冻结 DH Agent Decision Layer v1。

验收：

```text
文档快照进入 docs/gates/dh-agent-v1
回归测试通过
边界检查通过
无 AI 直接交易能力
无 NQ 核心污染
```
