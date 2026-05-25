# Decision Hub Worklog

## 2026-05-25 DH-REFIT-1-PLAN

完成 DH 文档结构重构的第一批落地。

### 已完成

```text
建立 docs/current/README.md
建立 docs/current/STATUS.md
建立 docs/current/ROADMAP.md
建立 docs/current/WORKFLOW.md
建立 docs/current/WORK_ORDER.md
更新 docs/README.md
保留 docs/codex 为历史辅助区
确认 DH/NQ 边界文档存在
确认 DH Stage1 重构工单存在
```

### 结论

DH 后续采用与 NQ 一致的工作流：

```text
docs/current = 当前事实源
docs/gates = 冻结快照
docs/codex = 历史计划与辅助执行区
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

### 未做

```text
未写业务代码
未修改 NQ 仓库
未接真实模型
未接交易能力
未建设前端页面
```

---

## 2026-05-25 Stage1（Boundary Freeze + Agent Runtime Skeleton）

详细记录见 `docs/current/DH_REFACTOR_STAGE1_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
dh-domain  新增 ResearchRun / AgentTask / TaskNode / AgentRole / AgentArtifact /
           StrategyCandidate / SignalProposal / RiskReview / JudgeDecision /
           DecisionRecommendation / ExperienceEntry / PheromoneEdge /
           NqFeedbackEvent + 5 个状态枚举
dh-memory  ExperienceStore / PheromoneStore / FailureCaseStore /
           MarketRegimeMemory / StrategyPatternMemory + InMemory 实现
dh-eval    CandidateScorer / RiskHeuristicScorer / EvidenceQualityScorer /
           BacktestResultScorer / JudgeAggregator + 规则实现
dh-connector NqBacktestClient / NqFeedbackClient / NqStrategyCandidateMapper /
             NqContractVerifier + Fake/Default 实现
dh-usecase agent runtime 8 个 service + 6 个 repository 端口 + 默认实现 + InMemory 仓储
dh-api     /api/ai/research-runs/...（POST/GET/start/tasks/candidates/judge-decision）
           /api/ai/feedback/nq
dh-app     AgentRuntimeWiringConfig
db/migration V2__dh_agent_runtime.sql（10 张表，全部带 trace_id / status / payload_json）
test       dh-usecase 新增 ResearchRunStage1ClosedLoopTest，覆盖
           create → start → candidate → judge → NQ feedback → experience 闭环
```

### 验收

```text
mvn -pl dh-domain,dh-memory,dh-eval,dh-connector,dh-usecase -am clean test  BUILD SUCCESS
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
仅 dh-app/PostgresContainerSmokeTest 因当前环境无 Docker 而跳过（与本次改动无关）
```

---

## 2026-05-25 Stage1-CLOSE（旧链路收敛 + 文档单源 + ArchUnit 兜底）

详细记录见 `docs/current/STAGE1_CLOSE_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
旧"多模型平台"链路整体 @Deprecated(since="Stage1-CLOSE", forRemoval=true)：
  - domain.run.{Run,RunStatus,RunStep,StepType}
  - usecase.facade.* + impl + dto
  - usecase.run.* + support.*
  - usecase.gate.* + evaluator.*
  - usecase.contract.*
  - dh-providers.{ModelProvider,MockProvider,ModelOutput,ProviderRegistry}
  - dh-app/AppWiringConfig（旧 bean）

api.run.RunController 迁移到 api.legacy.run 子包，@RequestMapping 从 /runs 改为 /legacy/runs；
同步 CreateRunRequest / RunView 迁移。

contracts/openapi.yaml 中 /runs 路径迁到 /legacy/runs 并标 deprecated。

文档单源：根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
全部更新为 "Stage1 completed / Next: Stage2-PoC"。

docs/codex/plans/_active/STATUS.json 切到 2026-05-25_Stage1_agent_runtime_skeleton；
老 M1 mock-provider 计划归档到 docs/codex/plans/_archive/2026-02-04_M1/。

ArchitectureTest 新增 4 条规则：
  ① ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..
  ② ..connector.nq.. 字段/方法名禁止出现 placeOrder|submitOrder|executeOrder|bypassRisk|forceExecute
     （DefaultNqContractVerifier 自身的黑名单豁免）
  ③ ..usecase.agent.. 不依赖 dh-providers
  ④ ..api.. 控制器 @RequestMapping 不能命中 /orders|/trades|/live

dh-eval/pom.xml parent 从 decision-hub 改回 dh-bom，保留 jackson-databind / slf4j-api 依赖。

dep-tree.txt 重新生成。
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
git grep "/runs" 在 dh-api 控制器里只命中 /legacy/runs 与 deprecation 注释。
README / docs/current/STATUS.md / docs/codex/plans/_active/STATUS.json 三处状态措辞一致。
```

### 下一步

进入 Stage2-PoC：NQ 真实事件回流接通 + Kronos/global-stock-data 工具接口预留。
