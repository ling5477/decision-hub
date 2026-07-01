# Decision Hub Current Work Order

> Current stage: DH-GATEK-DECISION-PIPELINE-MVP-WO / READY FOR REVIEW
> Closed:        DH-CODEX-WORKFLOW conflict cleanup; Integration-0 safety gate; P1-4 residual; header alignment; timestamp alignment
> Next stage:    DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE / NOT STARTED

## 1. 当前目标

下一步唯一允许工作内容是 `DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE`。

`DH-GATEK-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `ACCEPTED / CLOSED`。`DH-GATEK-DECISION-PIPELINE-MVP-WO` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md`，状态为 `WORK ORDER / READY FOR REVIEW`。下一步只允许按 K1 单批次执行 Decision Contract Freeze，不允许跳过 batch review 直接全量 implementation，也不允许实现真实模型、真实接入或交易能力：

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

## 3. DH-GATEK-DECISION-PIPELINE-MVP-PLAN（PLAN artifact）

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

Plan batch coverage:

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

Readiness recommendation:

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

## 4. DH-GATEK-DECISION-PIPELINE-MVP-WO（WORK ORDER artifact）

本节记录当前可审查工单。WO 只授权下一步进入 K1，不授权 K2-K8 连续实施，不授权 Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

Work order artifact:

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
```

Batch order:

```text
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton
K3 Audit / Snapshot / Trace Persistence
K4 Replay Read Model
K5 Mock Provider / Provider Health / Budget / Latency
K6 Mock NQ Dry-run Contract Tests
K7 Golden Cases / Eval Baseline
K8 Acceptance / Freeze
```

Ordering constraints:

```text
K1 after review before K2
K2 after review before K3
K3 after review before K4
K1-K5 complete before K6
K1-K7 complete before K8
Before GateK MVP closed: no Integration-1 runtime, no LangGraph runtime
LangGraph GateL or later
```

DecisionOutput hardening:

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

Readiness decision:

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
```

## 5. Historical / superseded / deferred 内容

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

## 6. 不做事项（持续硬约束）

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

## 7. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE。

目标：只执行 K1 Decision Contract Freeze。冻结 DecisionRequest / DecisionOutput /
DecisionAction / forbiddenActions / DecisionAuditEvent / DecisionTraceStep 的 domain contract
与 JSON schema，并补齐 K1 contract tests。不要实现 K2-K8。

禁止：
- 不实现 DecisionOrchestrator
- 不新增 API
- 不新增 migration
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
