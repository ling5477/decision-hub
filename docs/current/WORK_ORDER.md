# Decision Hub Current Work Order

> Current stage: DH-GATEK-DECISION-PIPELINE-MVP-PLAN / READY FOR REVIEW
> Closed:        DH-CODEX-WORKFLOW conflict cleanup; Integration-0 safety gate; P1-4 residual; header alignment; timestamp alignment
> Next stage:    DH-GATEK-DECISION-PIPELINE-MVP-WO / NOT STARTED

## 1. 当前目标

下一步唯一允许工作内容是 `DH-GATEK-DECISION-PIPELINE-MVP-WO`。

`DH-GATEK-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `PLAN / READY FOR REVIEW`。下一步 WO 只允许把该计划拆成可执行 implementation batches，不允许直接实现真实模型、真实接入或交易能力：

```text
READ_ONLY_RECOMMENDATION
候选建议 / 风险解释 / evidence summary
DecisionRequest / DecisionResponse / DecisionTrace 文档草案
Audit event shape 文档草案
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

本节记录已输出的 planning artifact。下一份工单只能以 `DH-GATEK-DECISION-PIPELINE-MVP-WO` 为主题；启动前必须先 review `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`，不得跳过 WO 直接 implementation。

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

## 4. Historical / superseded / deferred 内容

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

## 5. 不做事项（持续硬约束）

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

## 6. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：DH-GATEK-DECISION-PIPELINE-MVP-WO。

目标：只输出 DH GateK Decision Pipeline MVP 的可执行工单，把
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md 中 K0-K7 拆为小批次 implementation
work order，明确每批允许文件、禁止项、测试、验收和回滚。不要写业务代码。

禁止：
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
- 不新增 API / migration / provider / 交易路径

不要写业务代码。本轮只产出 Decision Pipeline MVP WO 文档草案；implementation 仍 NOT STARTED。
```
