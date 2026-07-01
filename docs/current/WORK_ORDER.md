# Decision Hub 当前工单

> 当前阶段: DH-GATEK-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS / IMPLEMENTED / READY FOR NEXT
> 已关闭: DH-CODEX-WORKFLOW conflict cleanup; Integration-0 safety gate; P1-4 residual; header alignment; timestamp alignment
> 下一阶段: DH-GATEK-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL / NOT STARTED

## 1. 当前目标

下一步唯一允许工作内容是 `DH-GATEK-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL`。

`DH-GATEK-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `ACCEPTED / CLOSED`。`DH-GATEK-DECISION-PIPELINE-MVP-WO` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md`，状态为 `ACCEPTED / CLOSED`。K1 已完成 review 并 `PASS / CLOSED / ACCEPTED`。K2 已完成 mock-only usecase 编排骨架。K3 已经 M1 readiness review 关闭。K4 已完成内部 Replay Read Model，只读取 K3 已持久化数据，不新增 API、Controller、migration 或 replay endpoint。K5 已完成 mock-only provider health / budget / latency controls，不新增 API、Controller、migration、真实 provider 或 NQ runtime。K6 已完成 mock NQ dry-run contract tests 与最小 fixture，不新增 API、Controller、migration、真实 HTTP 或 NQ runtime。下一步只允许进入 K7，不允许连续进入 K8 或全量 implementation：

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

## 3. DH-GATEK-DECISION-PIPELINE-MVP-PLAN（计划产物）

本节记录已关闭的 planning artifact。`docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md` 已 `ACCEPTED / CLOSED`；后续 implementation 必须遵守 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md` 的 K1-K8 批次顺序，不得跳过 review 直接全量 implementation。

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
ALLOW_GATEK_PLAN_CLOSE: YES
ALLOW_GATEK_WO: YES
ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 4. DH-GATEK-DECISION-PIPELINE-MVP-WO（工单产物）

本节记录已关闭工单。WO 已授权按 review gate 逐批 implementation；K1 已 `PASS / CLOSED / ACCEPTED`，K2 已 `IMPLEMENTED`，K3 已 `CLOSED / ACCEPTED after M1`，K4 已 `IMPLEMENTED / READY FOR NEXT`。WO 不授权 K5-K8 连续实施，不授权 Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

工单产物：

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
Status: ACCEPTED / CLOSED
K1 status: PASS / CLOSED / ACCEPTED
K2 status: IMPLEMENTED
K3 status: CLOSED / ACCEPTED after M1
M1 status: CLOSED / ACCEPTED
K4 status: IMPLEMENTED / READY FOR NEXT
K5 status: IMPLEMENTED / READY FOR NEXT
K6 status: IMPLEMENTED / READY FOR NEXT
K7 status: NOT STARTED
Next: DH-GATEK-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL / NOT STARTED
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
Before GateK MVP closed: no Integration-1 runtime, no LangGraph runtime
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
ALLOW_FULL_GATEK_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
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
Integration-1 not started
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
K4 Replay Read Model: IMPLEMENTED / READY FOR NEXT
K5 Provider Health / Budget / Latency: IMPLEMENTED / READY FOR NEXT
K6 Mock NQ Dry-run Contract Tests: IMPLEMENTED / READY FOR NEXT
K7 Golden Cases / Eval: NOT STARTED
```

## 5. DH-GATEK-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON（已实现）

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

## 6. DH-GATEK-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE（CLOSED / ACCEPTED after M1）

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
ALLOW_GATEK_M1_CLOSE_REVIEW: YES
ALLOW_FULL_GATEK_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 7. DH-GATEK-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL（IMPLEMENTED / READY FOR NEXT）

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
ALLOW_GATEK_M2_CLOSE_REVIEW: NO
ALLOW_FULL_GATEK_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 8. DH-GATEK-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY（IMPLEMENTED / READY FOR NEXT）

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
ALLOW_GATEK_M2_CLOSE_REVIEW: NO
ALLOW_FULL_GATEK_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
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
你在 decision-hub 仓库 dev 分支上工作。任务名：DH-GATEK-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS。

目标：只实现 K6 mock NQ dry-run contract tests；使用 mock / fixture 验证 K1-K5 只读边界和 provider guard summary，不接真实 NQ runtime。
判断是否允许 K6 close；不要实现 K7。

禁止：
- 不修改生产代码
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
- mvn test
- mvn -Pquality validate
```
