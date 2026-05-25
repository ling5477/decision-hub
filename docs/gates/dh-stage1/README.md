# Stage1 Gate — Frozen Snapshot

> Stage1 (Boundary Freeze + Agent Runtime Skeleton): completed
> Stage1-CLOSE (旧链路收敛 + 文档单源 + ArchUnit 兜底): completed
> mvn test: passed (BUILD SUCCESS)
> Next: Stage2-PoC

## 冻结声明

本目录是 Stage1 完成后的 `docs/current/` 快照，冻结于 2026-05-25。

冻结后不得修改本目录内容。后续变更只能发生在 `docs/current/`。

## 验收结果

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS
```

## Stage1 交付物

```text
dh-domain   ResearchRun / AgentTask / TaskNode / AgentRole / AgentArtifact /
            StrategyCandidate / SignalProposal / RiskReview / JudgeDecision /
            DecisionRecommendation / ExperienceEntry / PheromoneEdge /
            NqFeedbackEvent + 5 个状态枚举
dh-memory   ExperienceStore / PheromoneStore / FailureCaseStore /
            MarketRegimeMemory / StrategyPatternMemory + InMemory 实现
dh-eval     CandidateScorer / RiskHeuristicScorer / EvidenceQualityScorer /
            BacktestResultScorer / JudgeAggregator + 规则实现
dh-connector NqBacktestClient / NqFeedbackClient / NqStrategyCandidateMapper /
             NqContractVerifier + Fake/Default 实现
dh-usecase  agent runtime 8 个 service + 6 个 repository 端口 + 默认实现 + InMemory 仓储
dh-api      /api/ai/research-runs/...（POST/GET/start/tasks/candidates/judge-decision）
            /api/ai/feedback/nq
dh-app      AgentRuntimeWiringConfig
db/migration V2__dh_agent_runtime.sql（10 张表）
test        ResearchRunStage1ClosedLoopTest 闭环测试
```

## Stage1-CLOSE 交付物

```text
旧链路整体 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
api.run.RunController 迁移到 api.legacy.run，路径 /legacy/runs
contracts/openapi.yaml 旧路径标 deprecated
ArchitectureTest 新增 4 条规则
文档单源收敛到 docs/current/
docs/codex 计划切换 + 老 M1 归档
```

## 下一阶段

```text
Stage2-PoC：NQ 真实事件回流接通 + Kronos/global-stock-data 工具接口预留
```
