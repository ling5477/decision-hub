# DH Refactor Stage1 Work Order

> Status: Ready for Codex
> Scope: Boundary Freeze + Agent Runtime Skeleton
> Created: 2026-05-18

## 1. 目标

将 Decision Hub 从“多模型调用平台”的工程形态，推进到“可接入 NQ 的多 Agent 决策能力层”的第一阶段。

本阶段只做运行时骨架、边界契约、结构化产物、任务审计，不做真实交易闭环。

## 2. 总原则

1. 保持 DH 独立仓库、独立服务。
2. 不迁入 NQ。
3. 不维护完整第二套前端。
4. 不修改 NQ 交易核心。
5. DH 输出建议，NQ 执行正式交易能力。
6. 所有 AI 输出必须结构化落库，不能只停留在聊天文本。

## 3. 当前模块基础

现有 Maven 模块包括：

```text
dh-common
dh-observability
dh-domain
dh-usecase
dh-ledger
dh-providers
dh-memory
dh-knowledge
dh-infra
dh-security
dh-connector
dh-eval
dh-scheduler
dh-config
dh-sdk
dh-api
dh-app
```

第一阶段在现有模块上增量演进，不重建工程。

## 4. 后端改造范围

### 4.1 dh-domain

新增或整理领域对象：

```text
ResearchRun
AgentTask
TaskNode
AgentRole
AgentArtifact
StrategyCandidate
SignalProposal
RiskReview
JudgeDecision
DecisionRecommendation
ExperienceEntry
PheromoneEdge
NqFeedbackEvent
```

状态枚举：

```text
ResearchRunStatus: CREATED, PLANNING, EXPLORING, REVIEWING, JUDGING, WAITING_NQ, COMPLETED, FAILED, CANCELLED
TaskNodeStatus: PENDING, RUNNING, SUCCEEDED, FAILED, SKIPPED
CandidateStatus: GENERATED, FILTERED, SELECTED, REJECTED, SENT_TO_NQ
JudgeDecisionStatus: DRAFT, FINALIZED, REJECTED
FeedbackSource: BACKTEST, RISK, PAPER, RELEASE, LIVE, REVIEW
```

### 4.2 dh-usecase

新增用例服务接口：

```text
ResearchRunCommandService
ResearchRunQueryService
AgentTaskPlanner
CandidateGenerationService
CandidateReviewService
JudgeDecisionService
ExperienceFeedbackService
NqIntegrationUseCase
```

第一阶段用 mock/stub 流程跑通，不接真实模型也可以验收。

### 4.3 dh-memory

新增经验沉淀接口：

```text
ExperienceStore
PheromoneStore
FailureCaseStore
MarketRegimeMemory
StrategyPatternMemory
```

第一阶段做轻量分数，不做重型蚁群优化算法。

### 4.4 dh-eval

新增评分接口：

```text
CandidateScorer
RiskHeuristicScorer
EvidenceQualityScorer
BacktestResultScorer
JudgeAggregator
```

第一阶段采用规则评分 + mock LLM 评审，不做自动实盘决策。

### 4.5 dh-connector

新增 NQ adapter 接口：

```text
NqBacktestClient
NqFeedbackClient
NqStrategyCandidateMapper
NqContractVerifier
```

第一阶段只定义接口和 fake implementation。

### 4.6 dh-api

新增 API：

```text
POST /api/ai/research-runs
GET  /api/ai/research-runs
GET  /api/ai/research-runs/{runId}
POST /api/ai/research-runs/{runId}/start
GET  /api/ai/research-runs/{runId}/tasks
GET  /api/ai/research-runs/{runId}/candidates
GET  /api/ai/research-runs/{runId}/judge-decision
POST /api/ai/feedback/nq
```

### 4.7 dh-infra

新增持久化表的 JDBC/Repository 实现。第一阶段允许先用内存实现，但必须预留 Repository 接口和迁移脚本位置。

建议迁移命名：

```text
V1__dh_agent_runtime.sql
```

## 5. 群体智能工程落点

### 5.1 蜂群机制：轻量版本，立即做

落点：CandidateGenerationService。

实现：

```text
同一 ResearchRun 下并行生成多个 StrategyCandidate
每个 candidate 记录 source_agent、search_path、evidence_refs、score_snapshot
候选进入 filter/rank/select 流程
```

第一阶段不做复杂 Bee Colony Optimization，只做并行候选搜索框架。

### 5.2 蚁群机制：轻量版本，立即做

落点：ExperienceFeedbackService + PheromoneStore。

实现：

```text
strategy_pattern + market_regime + data_source + agent_role 形成经验 key
NQ 回测/风控/模拟盘/复盘结果回流后更新 pheromone_score
成功增强，失败衰减，风控拒绝强惩罚
```

第一阶段不做全局路径优化，只做经验分数与检索优先级。

### 5.3 狼群机制：立即做骨架

落点：AgentTaskPlanner + JudgeDecisionService。

角色：

```text
LeaderAgent
ScoutAgent
AnalystAgent
StrategyAgent
RiskReviewerAgent
StrategyReviewerAgent
JudgeAgent
```

实现：

```text
Leader 负责规划任务图
Scout/Analyst/Strategy 生成候选和证据
RiskReviewer/StrategyReviewer 负责审查
Judge 负责统一评分和最终结构化输出
```

所有最终输出必须经过 JudgeDecision，不允许单 Agent 直接给最终结论。

## 6. 数据库表建议

```text
dh_research_runs
dh_agent_tasks
dh_task_nodes
dh_agent_artifacts
dh_strategy_candidates
dh_candidate_scores
dh_judge_decisions
dh_experience_entries
dh_pheromone_edges
dh_nq_feedback_events
```

所有表必须有：

```text
id
created_at
updated_at
trace_id
status
payload_json
```

所有 JSON 字段保留原始输入输出快照，方便复盘。

## 7. 前端范围

本阶段 DH 不做完整业务前端。

只提供 API。正式页面后续放到 NQ Console。

如果必须验证，可以保留最小 debug 页面：

```text
/admin/debug/research-runs
/admin/debug/providers
/admin/debug/workflow-trace
```

## 8. 验收标准

### 8.1 编译验收

```bash
mvn test
```

必须通过。

### 8.2 API 验收

能完成最小闭环：

```text
创建 ResearchRun
启动 ResearchRun
生成多个 StrategyCandidate
生成 JudgeDecision
接收一条 NQ Feedback Event
更新 ExperienceEntry / PheromoneEdge
查询 run detail 可看到完整任务轨迹
```

### 8.3 边界验收

代码中不得出现：

```text
直接下单实现
绕过风控的交易执行
NQ 订单状态机复制实现
NQ 回测核心复制实现
实盘发布自动执行
```

### 8.4 审计验收

每个 ResearchRun 必须能追踪：

```text
输入任务
Agent 角色
任务节点
候选方案
评分过程
仲裁结果
NQ 回流事件
经验更新结果
```

## 9. 风险点

### 9.1 概念堆砌

规避：第一阶段只做轻量蜂群/蚁群/狼群工程骨架，不做复杂数学优化器。

### 9.2 DH/NQ 边界混乱

规避：NQ 执行、DH 建议；所有交易事实以 NQ 为准。

### 9.3 AI 输出不可复盘

规避：所有 Agent 输出必须结构化落库，保存 payload_json。

### 9.4 成本失控

规避：第一阶段使用 mock provider / rule scorer，模型调用统一经过 provider gateway，预留 token/cost 字段。

## 10. Codex 开工提示词

```text
你在 ling5477/decision-hub 仓库工作。

目标：完成 DH Refactor Stage1：Boundary Freeze + Agent Runtime Skeleton。

严格边界：
- 不迁移 DH 到 NQ。
- 不修改 NQ 仓库。
- 不实现真实下单。
- 不绕过风控。
- 不重写 NQ 回测核心。
- 不建设完整第二套前端。

请按 docs/current/DH_NQ_INTEGRATION.md 和 docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md 执行。

优先级：
1. 在 dh-domain 定义 ResearchRun、AgentTask、TaskNode、StrategyCandidate、JudgeDecision、ExperienceEntry、PheromoneEdge、NqFeedbackEvent 等领域模型和枚举。
2. 在 dh-usecase 定义 ResearchRunCommandService、CandidateGenerationService、JudgeDecisionService、ExperienceFeedbackService、NqIntegrationUseCase。
3. 在 dh-connector 定义 NQ adapter 接口和 fake implementation。
4. 在 dh-api 增加最小 REST API，支持创建/启动/查询 ResearchRun，查询 candidates/judge decision，接收 NQ feedback。
5. 增加测试，验证创建 run -> 启动 -> 生成候选 -> 仲裁 -> 接收 feedback -> 更新经验 的最小闭环。
6. mvn test 必须通过。

实现要求：
- 第一阶段允许 fake provider / mock scorer。
- 所有输出必须结构化。
- 所有关键对象必须有 traceId。
- 不引入复杂群体智能数学优化器。
- 蜂群只做并行候选生成框架。
- 蚁群只做经验分数更新。
- 狼群只做角色化任务编排和 Judge 仲裁。

完成后更新：
- docs/current/DH_REFACTOR_STAGE1_WORKLOG.md
- docs/current/DH_REFACTOR_STAGE1_STATUS.md
```
