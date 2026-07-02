# DH Refactor Stage1 Worklog

> Author: ling5477 (执行 docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md)
> Date: 2026-05-25
> Branch: dev

## 概览

按照 DH_REFACTOR_STAGE1_WORK_ORDER.md 的“Codex 开工提示词”，完成 Stage1 Boundary Freeze + Agent Runtime Skeleton。

边界硬约束（已严格遵守）：

- 不迁移 DH 到 NQ。
- 不修改 NQ 仓库。
- 不实现真实下单。
- 不绕过风控（NqContractVerifier 显式禁用 placeOrder/forceExecute/bypassRisk 字段）。
- 不重写 NQ 回测核心。
- 不建设完整第二套前端（无新增前端页面）。

## 改动清单

### dh-domain（领域骨架）

- `domain.research.ResearchRun` / `ResearchRunStatus`
- `domain.agent.AgentRole` / `AgentTask` / `TaskNode` / `TaskNodeStatus` / `AgentArtifact`
- `domain.candidate.StrategyCandidate` / `CandidateStatus` / `SignalProposal`
- `domain.judge.JudgeDecision` / `JudgeDecisionStatus` / `RiskReview` / `DecisionRecommendation`
- `domain.experience.ExperienceEntry` / `PheromoneEdge`
- `domain.feedback.NqFeedbackEvent` / `FeedbackSource`

所有对象自带 `traceId` + `payloadJson`，便于审计/复盘。

### dh-memory（经验沉淀）

- `memory.agent.ExperienceStore` + `InMemoryExperienceStore`
- `memory.agent.PheromoneStore` + `InMemoryPheromoneStore`
- `memory.agent.FailureCaseStore` + `InMemoryFailureCaseStore`
- `memory.agent.MarketRegimeMemory` + `InMemoryMarketRegimeMemory`
- `memory.agent.StrategyPatternMemory` + `InMemoryStrategyPatternMemory`

pom：增加 `dh-domain` 依赖。

### dh-eval（规则评分骨架）

- `eval.agent.CandidateScorer` + `rule.DefaultCandidateScorer`
- `eval.agent.RiskHeuristicScorer` + `rule.DefaultRiskHeuristicScorer`
- `eval.agent.EvidenceQualityScorer` + `rule.DefaultEvidenceQualityScorer`
- `eval.agent.BacktestResultScorer` + `rule.DefaultBacktestResultScorer`
- `eval.agent.JudgeAggregator` + `rule.DefaultJudgeAggregator`

### dh-connector（NQ 适配器骨架）

- `connector.nq.NqBacktestClient` + `fake.FakeNqBacktestClient`
- `connector.nq.NqFeedbackClient` + `fake.FakeNqFeedbackClient`
- `connector.nq.NqStrategyCandidateMapper` + `fake.DefaultNqStrategyCandidateMapper`
- `connector.nq.NqContractVerifier` + `fake.DefaultNqContractVerifier`

pom：增加 `dh-domain` 依赖。

### dh-usecase（运行时用例）

接口（agent 包）：

- `ResearchRunCommandService` / `ResearchRunQueryService`
- `AgentTaskPlanner`
- `CandidateGenerationService` / `CandidateReviewService`
- `JudgeDecisionService`
- `ExperienceFeedbackService`
- `NqIntegrationUseCase`
- 仓储端口：`ResearchRunRepository` / `AgentTaskRepository` / `StrategyCandidateRepository`
  / `JudgeDecisionRepository` / `NqFeedbackEventRepository` / `AgentArtifactRepository`

默认实现（impl 包）：

- `DefaultAgentTaskPlanner`（狼群骨架：SCOUT → ANALYST → STRATEGY → RISK_REVIEWER + STRATEGY_REVIEWER → JUDGE）
- `DefaultCandidateGenerationService`（蜂群轻量：每个 run 并行生成 3 个候选 + 独立 searchPath）
- `DefaultCandidateReviewService`
- `DefaultJudgeDecisionService`（阈值挑选，所有最终输出经过 Judge）
- `DefaultExperienceFeedbackService`（蚁群轻量：score 增强/衰减 + 风控强惩罚 + 写入 FailureCaseStore）
- `DefaultNqIntegrationUseCase`
- `DefaultResearchRunCommandService` / `DefaultResearchRunQueryService`

内存仓储（inmemory 包）：6 个 In-Memory 实现，Stage1 默认绑定，Stage2 起可替换为 dh-infra JDBC 实现。

pom：增加 `dh-memory`、`dh-connector` 依赖。

### dh-api（REST 入口）

- `api.research.ResearchRunController`：
  - `POST /api/ai/research-runs`
  - `GET  /api/ai/research-runs`
  - `GET  /api/ai/research-runs/{runId}`
  - `POST /api/ai/research-runs/{runId}/start`
  - `GET  /api/ai/research-runs/{runId}/tasks`
  - `GET  /api/ai/research-runs/{runId}/candidates`
  - `GET  /api/ai/research-runs/{runId}/judge-decision`
- `api.feedback.NqFeedbackController`：
  - `POST /api/ai/feedback/nq`

### dh-app（装配 + 迁移）

- 新增 `config.AgentRuntimeWiringConfig`：Stage1 默认绑定内存仓储 + 规则评分器 + Fake NQ adapter。
- 新增 Flyway 脚本 `db/migration/V2__dh_agent_runtime.sql`：覆盖工单要求的 10 张表，所有表都带
  `id / created_at / updated_at / trace_id / status / payload_json`。
- pom：增加 `dh-eval` 依赖，以便 AppWiringConfig 可见 scorer Bean。

### 测试

`dh-usecase`：新增 `ResearchRunStage1ClosedLoopTest`，覆盖：

```
create → start → 生成多个 StrategyCandidate → 仲裁（JudgeDecision FINALIZED 或 REJECTED）
→ 接收一条 NQ Feedback Event (BACKTEST positive)
→ ExperienceEntry / PheromoneEdge 增强
→ 接收一条 NQ Feedback Event (RISK negative)
→ ExperienceEntry 失败计数 +1 + FailureCaseStore 写入
```

## 验证记录

- `mvn -pl dh-domain,dh-memory,dh-eval,dh-connector,dh-usecase -am clean test`：BUILD SUCCESS（Stage1 闭环测试通过）。
- `mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false`：BUILD SUCCESS。
- 仅 `dh-app` 的 `PostgresContainerSmokeTest` 因当前环境无 Docker 而失败；与本次改动无关，属于本仓库
  既有的 Testcontainers 集成测试，需要 Docker daemon。

## 群体智能落点

- 蜂群：`CandidateGenerationService` 在同一 ResearchRun 下并行生成多个 StrategyCandidate，每个候选独立
  记录 `sourceAgent / searchPath / evidenceRefs / scoreSnapshot`。
- 蚁群：`ExperienceFeedbackService` + `PheromoneStore` 维护 `ExperienceEntry.score` 与
  `PheromoneEdge.pheromoneScore`；RISK 拒绝触发强惩罚。
- 狼群：`AgentTaskPlanner` 输出固定 6 节点骨架；`JudgeDecisionService` 是 ResearchRun 唯一的“决策出口”。

## 后续（不在 Stage1 范围）

- dh-infra：把 In-Memory 仓储替换为 Postgres 实现（V2 迁移脚本已就位）。
- dh-providers：把当前 mock provider 与 Stage1 agent 串联，引入真实 LLM 调用。
- 安全：把 `t-default` 占位 tenant 替换为 dh-security 注入。
