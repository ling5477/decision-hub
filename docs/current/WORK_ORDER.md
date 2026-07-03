# Decision Hub 当前工单

> 当前阶段: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED
> 已关闭: DH-CODEX-WORKFLOW conflict cleanup; Integration-0 safety gate; P1-4 residual; header alignment; timestamp alignment; Stage4 Decision Pipeline MVP; Integration-1 dry-run plan baseline; I1-P0 factsource rebase; I1-P1 contract dry-run plan; I1-P2 contract fixtures plan; I1-P3 dry-run implementation readiness plan; I1-P4 implementation gate review fix; I1 dry-run mock implementation work order
> 下一阶段: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO / NOT STARTED

## 1. 当前目标

`NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE` 已完成 docs/factsource rebase，并 `CLOSED / ACCEPTED / DOCS-ONLY`。`NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN` 已完成 planning-only 合同规划，计划文档为 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md`。`NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN` 已完成 planning-only fixture/schema/golden case 对齐规划，计划文档为 `docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md`。`NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN` 已完成 planning-only readiness 规划，计划文档为 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md`，并合并原 NQ stub / DH entry / joint mock validation 三个计划项。`NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX` 已完成 docs-only gate-fix：双仓 P3 提交状态已复核，schema gap 已归类。`NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO` 已完成 work-order-only 批次拆分，工单为 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md`。下一步唯一允许工作内容是 `NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO / NOT STARTED`。下一步仍只能写 contract gap close work order / review，不是 implementation code、runtime、真实 HTTP、real provider 或 LIVE。`NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN` 已完成 planning-only baseline，计划文档为 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md`。

## 0. NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO（COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
```

本工单修正预检规则并拆分后续 M0-M4：

```text
M0 NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO
M1 NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK
M2 NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER
M3 NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-CONTRACT-TESTS
M4 NQ-DH-I1-M4-DRYRUN-MOCK-CLOSE-REVIEW
```

本工单本身不授权代码、测试、fixture JSON、schema、contracts、golden_cases、API / Controller、migration、runtime、真实 HTTP、real provider、AI / Agent runtime、LangGraph runtime 或 LIVE。唯一下一步是 `NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO / NOT STARTED`。

`DH-STAGE4-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `ACCEPTED / CLOSED`。`DH-STAGE4-DECISION-PIPELINE-MVP-WO` 已产出 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md`，状态为 `ACCEPTED / CLOSED`。K1-K7 已关闭；K8 acceptance / freeze 已 `CLOSED / ACCEPTED`，报告见 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md`，冻结快照见 `docs/gates/dh-stage4-decision-pipeline-mvp/`。Integration-1 dry-run plan 已基于 NQ GateN 完成 rebase；P0 只允许同步两仓事实源和旧 GateK 口径，不允许启动 Integration-1 runtime 或全量 runtime implementation：

```text
READ_ONLY_RECOMMENDATION
候选建议 / 风险解释 / evidence summary
DecisionRequest / DecisionOutput / DecisionTrace contract
Audit event shape contract
Policy denied / audit write failure / provider unavailable fail-closed 规则
ABSTAIN / NO_ACTION 语义
forbiddenActions 固化
验收清单
风险清单
```

语言治理要求：当前工单、后续 review 记录、WORKLOG、TESTING、STATUS、ROADMAP 正文必须中文为主；`DecisionOutput`、`READ_ONLY_RECOMMENDATION`、`ABSTAIN`、`forbiddenActions`、文件路径、命令和固定输出字段等稳定工程标识保留英文原样。

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

## 3. DH-STAGE4-DECISION-PIPELINE-MVP-PLAN（计划产物）

本节记录已关闭的 planning artifact。`docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md` 已 `ACCEPTED / CLOSED`；后续 implementation 必须遵守 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md` 的 K1-K8 批次顺序，不得跳过 review 直接全量 implementation。

允许范围：

```text
输出 Decision Pipeline MVP PLAN 文档
定义 read-only recommendation 边界
定义 evidence / risk / policy / audit trace 字段
定义 DecisionResponse action vocabulary
定义 ABSTAIN / NO_ACTION / BLOCKED / POLICY_DENIED 语义
定义 forbiddenActions: PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
定义 audit write failure fail-closed 规则
定义 provider unavailable / no evidence fail-closed 规则
定义验收清单
定义风险清单
```

计划批次覆盖：

```text
K0 Factsource Sync / Docs Rebase
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton Plan
K3 Audit / Snapshot / Trace / Replay Plan
K4 Mock Provider / Provider Health Plan
K5 Mock NQ Dry-run Contract Test Plan
K6 Golden Cases / Eval Plan
K7 Acceptance / Freeze Plan
```

Readiness 推荐：

```text
ALLOW_STAGE4_PLAN_CLOSE: YES
ALLOW_STAGE4_WO: YES
ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 4. DH-STAGE4-DECISION-PIPELINE-MVP-WO（工单产物）

本节记录已关闭工单。WO 已授权按 review gate 逐批 implementation；K1 已 `PASS / CLOSED / ACCEPTED`，K2 已 `IMPLEMENTED`，K3 已 `CLOSED / ACCEPTED after M1`，K4 已 `IMPLEMENTED / READY FOR NEXT`。WO 不授权 K5-K8 连续实施，不授权 Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

工单产物：

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
Status: ACCEPTED / CLOSED
K1 status: PASS / CLOSED / ACCEPTED
K2 status: IMPLEMENTED
K3 status: CLOSED / ACCEPTED after M1
M1 status: CLOSED / ACCEPTED
K4 status: CLOSED
K5 status: CLOSED
K6 status: CLOSED
K7 status: CLOSED
K8 status: CLOSED / ACCEPTED
Integration-1 dry-run plan: PLAN BASELINE ACCEPTED
I1-P0 factsource rebase: CLOSED / ACCEPTED / DOCS-ONLY
I1-P1 contract dry-run plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P2 contract fixtures plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P3 dry-run implementation readiness plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P4 implementation gate review fix: COMPLETED / DOCS-ONLY / GATE-FIX
Next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

批次顺序：

```text
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton
K3 Audit / Snapshot / Trace Persistence
K4 Replay Read Model
K5 Mock Provider / Provider Health / Budget / Latency
K6 Mock NQ Dry-run Contract Tests
K7 Golden Cases / Eval 基线
K8 Acceptance / Freeze
```

顺序约束：

```text
K1 after review before K2
K2 after review before K3
K3 after M1 before K4
K4 after implementation before K5
K1-K5 complete before K6
K1-K7 complete before K8
Before Stage4 MVP closed: no Integration-1 runtime, no LangGraph runtime
LangGraph GateL or later
```

DecisionOutput 加固：

```text
decisionType = READ_ONLY_RECOMMENDATION
action only ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
default ABSTAIN
no evidence -> ABSTAIN
provider failure -> ABSTAIN
policy denied -> BLOCKED or ABSTAIN, fail-closed
high risk forbids LONG_BIAS / SHORT_BIAS
forbiddenActions includes PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
structured JSON only
free-text final output forbidden
real trading instruction forbidden
```

Readiness 决策：

```text
ALLOW_WO_CLOSE: YES
ALLOW_K1_IMPLEMENTATION: YES
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

当前状态必须保持：

```text
DH-AUDIT-FIX completed
NQ integration not started
Integration-0 safety gate CLOSED / ACCEPTED
Integration-1 implementation not started
Runtime integration not started
DH integrated NO
AI / Agent runtime not started
RealClient forbidden
real provider forbidden
real HTTP forbidden
LIVE trading forbidden
NQ mutation forbidden
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline: GateN
K2 DecisionOrchestrator Skeleton: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PLAN BASELINE ACCEPTED
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX: COMPLETED / DOCS-ONLY / GATE-FIX
Next concrete action: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

## 5. DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON（已实现）

K2 已在 `dh-usecase` 内完成 mock-only orchestrator skeleton；后续状态由 K3 / M1 readiness 入口接管。

K2 实施范围：

```text
DecisionOrchestrator / DefaultDecisionOrchestrator
DecisionContext / DecisionContextBuilder / DefaultDecisionContextBuilder
DecisionPolicyChecker / DefaultDecisionPolicyChecker
DecisionSignalProvider / DecisionSignalResult / MockDecisionSignalProvider
DecisionRiskReviewer / DefaultDecisionRiskReviewer
DecisionOutputAssembler
DecisionOutput observation / abstainForRisk factory
K2 unit tests
```

K2 明确未做：

```text
K3 audit / snapshot / trace / replay persistence
API path / Controller
Repository / JDBC / migration
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
```

## 6. DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE（CLOSED / ACCEPTED after M1）

K3 已在 DH 仓库内完成 audit / snapshot / trace persistence，并已通过 M1 readiness review 关闭。

K3 实施范围：

```text
Flyway V5: dh_decision_request / dh_decision_context_snapshot /
           dh_decision_trace_step / dh_decision_provider_call_log /
           dh_decision_output / dh_decision_audit_event
DecisionAuditRepository usecase port
DecisionPersistenceRecords
InMemoryDecisionAuditRepository test/default fallback
JdbcDecisionAuditRepository
DecisionPipelineWiringConfig
DefaultDecisionOrchestrator persistence writes + fail-closed handling
K3 usecase / infra / migration tests
docs/current sync
```

K3 明确未做：

```text
K4 replay read model
replay API / Controller / query endpoint
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
NQ DB read/write
```

K3 readiness：

```text
ALLOW_K3_CLOSE: YES
ALLOW_K4_IMPLEMENTATION: YES
ALLOW_STAGE4_M1_CLOSE_REVIEW: YES
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 7. DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL（IMPLEMENTED / READY FOR NEXT）

K4 已在 DH 仓库内完成内部 Replay Read Model。该 read model 只读取 K3 已持久化的六类 DH-owned decision 记录，不重跑 provider、不重跑 orchestrator、不调用 NQ、不修改 audit 数据。

K4 实施范围：

```text
DecisionReplayView / Request / Context / TraceStep / ProviderCall / Output / AuditEvent / Timeline
DecisionReplayStatus
DecisionReplayQuery
DecisionReplayQueryRepository
DecisionReplayQueryService
DefaultDecisionReplayQueryService
JdbcDecisionReplayQueryRepository
DecisionPipelineWiringConfig replay repository / service wiring
K4 usecase / JDBC read / wiring tests
docs/current sync
```

K4 明确未做：

```text
replay API / Controller / query endpoint
new migration
K5 provider health / budget / latency
K6 mock NQ dry-run contract tests
K7 golden cases / eval
K8 acceptance / freeze
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
NQ DB read/write
```

K4 readiness：

```text
ALLOW_K4_CLOSE: YES
ALLOW_K5_IMPLEMENTATION: YES
ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 8. DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY（IMPLEMENTED / READY FOR NEXT）

K5 已在 DH 仓库内完成 mock-only provider health / budget / latency controls。该批次只强化 K2/K3/K4 已有 Decision Pipeline 的 provider guard 与 provider call summary，不新增 API、Controller、migration、真实 provider、HTTP、NQ runtime、LLM、LangGraph 或 LIVE。

K5 实施范围：

```text
DecisionProviderHealth / Budget / Latency / GuardResult / FailureClass value objects
DecisionProviderHealthEvaluator / DefaultDecisionProviderHealthEvaluator
DecisionProviderBudgetGuard / DefaultDecisionProviderBudgetGuard
DecisionProviderLatencyRecorder / DefaultDecisionProviderLatencyRecorder
DecisionProviderGuard / DefaultDecisionProviderGuard
DefaultDecisionOrchestrator provider pre/post guard integration
MockDecisionSignalProvider K5 failure-state test hooks
DecisionPipelineWiringConfig K5 guard wiring
provider call log signal_json / latency_ms / error_code summary
K4 replay provider call view regression
docs/current sync
```

K5 明确未做：

```text
K6 mock NQ dry-run contract tests
K7 golden cases / eval
K8 acceptance / freeze
new API path / Controller / replay API / query endpoint
new migration / provider health table / budget table / latency table
real provider / OpenAI / Claude / Gemini / local model
real HTTP / WebClient / RestTemplate / HttpClient
NQ runtime / RealClient / RealNqBacktestClient
Integration-1 runtime
LangGraph runtime
LIVE / trading / NQ mutation
NQ DB read/write
credential / token / API secret / passphrase access
```

K5 readiness：

```text
ALLOW_K5_CLOSE: YES
ALLOW_K6_IMPLEMENTATION: YES
ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 9. Historical / superseded / deferred 内容

```text
NqFeedbackClient 接通真实 HTTP / event         forbidden / deferred
NqBacktestClient 接通真实 NQ                    forbidden / deferred
NQ /api/ai/research/backtest-requests           forbidden / deferred
RealNqBacktestClient / RealClient               forbidden
real provider                                   forbidden
真实 HTTP / event 到 NQ                         forbidden
启动 Paper Run                                  forbidden
修改 NQ 交易状态                                forbidden
访问交易所密钥                                  forbidden
LIVE trading                                    forbidden
```

以上内容只保留为历史背景，不是当前 next，不允许作为当前实现任务。后续如需恢复，必须先基于 NQ GateN 重新进入 planning-only audit，并通过安全审查、契约冻结和人工确认。

## 10. 不做事项（持续硬约束）

```text
不修改 NQ 仓库交易核心
不实现真实下单
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不建设第二套完整前端
不引入 BCO/ACO/GWO 等重型数学优化器
不把 Kronos / TradingAgents / global-stock-data 整体复制进 DH/NQ
不新增 API / migration / provider / NQ client / RealClient / 交易路径
不读取 token / cookie / exchange secret / production .env / API key / private key / mnemonic / 2FA backup code
```

## 11. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE。

目标：只基于 K1-K7 已完成 evidence 做 DH Stage4 Decision Pipeline MVP acceptance / freeze 审查与文档冻结记录。
判断是否允许 DH Stage4 Decision Pipeline MVP close；不要启动 Integration-1 runtime，不要实现任何 runtime integration。

禁止：
- 不修改生产代码
- 不新增测试代码，除非 K8 acceptance 发现必须用最小回归证明某个验收结论
- 不新增 API
- 不新增 migration
- 不新增 replay API / Controller / query endpoint
- 不实现真实 NQ client
- 不实现 RealClient
- 不接真实 HTTP / event 到 NQ
- 不调用 NQ /api/ai/research/backtest-requests
- 不接真实 LLM provider
- 不接 LangGraph runtime
- 不输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 作为 action
- 不启动 Paper Run
- 不修改 NQ 交易状态
- 不访问交易所密钥
- 不触碰 LIVE trading
- 不读取或写入 NQ DB
- 不新增 provider / 交易路径

验收命令：
- git status --short
- git diff --check
- git diff --stat
- mvn -ntp test
- mvn -ntp -Pquality validate
- rg 边界扫描：RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate /
  HttpClient / placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase / accountId /
  Controller / NqClient / Exchange / Broker / live / LIVE / endpoint / Endpoint /
  PostMapping / GetMapping / RequestMapping
```
