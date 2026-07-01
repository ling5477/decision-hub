# Decision Hub Roadmap

## 1. 总路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。

路线：

```text
DH-REFIT-1:   文档结构与边界统一                          [completed]
Stage1:       Boundary Freeze + Agent Runtime Skeleton    [completed]
Stage1-CLOSE: 旧链路 @Deprecated + 文档单源 + ArchUnit    [completed]
Integration-0: 只读边界、契约冻结、权限模型、审计模型        [closed / accepted]
DH-GATEK-DECISION-PIPELINE-MVP-PLAN:
              只读 Decision Pipeline MVP 规划               [accepted / closed]
DH-GATEK-DECISION-PIPELINE-MVP-WO:
              Decision Pipeline MVP 可执行工单               [accepted / closed]
DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE:
              Decision Contract Freeze                     [pass / closed / accepted]
DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW:
              K1 contract review                          [completed / closed]
DH-GATEK-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON:
              DecisionOrchestrator Skeleton                [implemented / ready for review]
DH-GATEK-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON-REVIEW:
              K2 orchestrator skeleton review              [next / not started]
Stage2-PoC:   NQ 真实事件回流 + 工具接口预留              [historical / superseded / deferred]
Stage3:       NQ Console AI 页面接入                      [later / gated]
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

## 4. Integration-0 / Decision Pipeline MVP 当前路线

Integration-0 safety gate 已 `CLOSED / ACCEPTED`。当前下一步不再是旧 `Integration-0-PLAN`；`DH-GATEK-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `ACCEPTED / CLOSED`。`DH-GATEK-DECISION-PIPELINE-MVP-WO` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md`，状态为 `ACCEPTED / CLOSED`。K1 Contract Freeze review 已 `PASS / CLOSED / ACCEPTED`。K2 DecisionOrchestrator Skeleton 已实现 mock-only usecase 编排骨架，状态为 `IMPLEMENTED / READY FOR REVIEW`。下一步是 `DH-GATEK-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON-REVIEW`（NOT STARTED），不得跳过 K2 review 或批次 review 直接进入 K3。

语言治理补充：后续 DH roadmap、plan、work order、testing、worklog、status 文档正文必须中文为主；工程对象名、enum、JSON/OpenAPI 字段、HTTP header、状态枚举、命令和外部技术名保留英文原样。固定输出字段可以保留英文，但字段内容必须中文为主。

当前状态：

```text
NQ integration not started
Integration-0 safety gate CLOSED / ACCEPTED
Integration-1 NOT STARTED
Runtime integration NOT STARTED
DH integrated NO
AI / Agent runtime NOT STARTED
RealClient forbidden
real provider forbidden
LIVE DISABLED
NQ mutation forbidden
GateK plan artifact docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md
GateK plan status ACCEPTED / CLOSED
Current GateK work order artifact docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
Current GateK work order status ACCEPTED / CLOSED
K1 contract freeze status PASS / CLOSED / ACCEPTED
K2 DecisionOrchestrator Skeleton IMPLEMENTED / READY FOR REVIEW
Next concrete action DH-GATEK-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON-REVIEW / NOT STARTED
K3 Audit / Snapshot / Trace Persistence NOT STARTED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline GateN
```

NQ / DH 三轮只读审计（NQ 全仓 / DH 全仓 / NQ-DH 联合边界 + 汇总）已完成。Integration-0 是 contract / mock / documentation 工作线，不是 runtime integration。DH P1-4 residual 已 CLOSED；header alignment CLOSED；timestamp alignment CLOSED；code reality audit blockers fixed。上述 CLOSED 不授权 Integration-1 runtime。

DH-NQ Integration-0 契约冻结已完成（contract / mock / docs，未实现集成），见
`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` /
`DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md` / `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`。当前不允许直接 Integration-1 实现、真实只读通道、真实 HTTP、RealClient、Provider、LIVE、AI 自动交易或 LangGraph runtime。

范围：

```text
DecisionRequest / DecisionOutput / DecisionOrchestrator 规划
Snapshot / Trace / Replay / Audit 边界规划
READ_ONLY_RECOMMENDATION 输出模型
ABSTAIN 默认策略
forbiddenActions: PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
policy denied fail-closed
audit 写失败 fail-closed
验收清单与风险清单
```

Decision Pipeline MVP PLAN 必须明确禁止：

```text
不实现真实 NQ client
不实现 RealClient
不接真实 HTTP / event 到 NQ
不调用 NQ runtime 或 NQ /api/ai/research/backtest-requests
不启动 Paper Run
不修改 NQ 交易状态
不访问交易所密钥
不触碰 LIVE trading
不读取 NQ DB
不写 NQ DB
不接 LangGraph runtime
不接 LLM / OpenAI / Claude / Gemini / 本地模型 runtime
不输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER
```

GateK WO 验收：

```text
已输出 DH-GATEK-DECISION-PIPELINE-MVP-WO 文档
已拆分 K1-K8 implementation batches
已明确每批 allowed / forbidden files、生产/测试/API/migration 权限、测试、验收和回滚
已固化 DecisionOutput READ_ONLY_RECOMMENDATION 与 ABSTAIN / fail-closed 规则
已固化 forbiddenActions 与批次顺序约束
git diff --check 通过
本轮按用户要求尝试 mvn test 与 mvn -Pquality validate，并在 TESTING.md 记录真实结果
```

## 5. Stage2-PoC（historical / superseded / deferred）

以下内容是历史规划背景，不是当前 next，不允许作为当前实现任务：

```text
NqFeedbackClient 接通真实 HTTP/事件（deferred / forbidden until Integration-0 approved）
NqBacktestClient 接通真实 NQ /api/ai/research/backtest-requests（deferred / forbidden until Integration-0 approved）
RealNqBacktestClient / RealClient（forbidden）
real provider（forbidden）
真实 HTTP / event 到 NQ（forbidden）
DH 主动发起真实 NQ backtest request（forbidden）
```

后续如需恢复这些方向，必须先通过 Integration-0-PLAN 的安全审查、契约冻结和人工确认。

## 6. Stage3：NQ Console 接入

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
