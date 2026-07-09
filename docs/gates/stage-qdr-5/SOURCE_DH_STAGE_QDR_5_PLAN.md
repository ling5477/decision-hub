# DH Stage-QDR-5 Plan

> Historical archive note: This is a historical source document captured before tag close. Current stage state is TAGGED; see README.md / STATUS_SNAPSHOT.md. Historical status wording in this file is not a current factsource.

## 1. 任务定位

```text
Task: DH-STAGE-QDR-5-PLAN
Type: PLANNING_ONLY
Status: DONE / PLAN_ONLY
Stage-QDR-4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Stage-QDR-4 tag close: DONE
Tag: dh-stage-qdr-4-close
Tag target: 62c8020 docs(workflow): repair documentation discipline and skill policy
Stage-QDR-5 implementation: NOT_STARTED
```

本轮只规划 Stage-QDR-5，不实现 Java 生产代码，不修改测试，不新增 migration，不新增 API / Controller，不接真实 provider，不发真实 HTTP，不启动 Agent / LangGraph runtime，不开启 LIVE，不修改 NQ。

## 2. 前置证据

本轮已用实际命令确认：

```text
repository: Decision Hub
branch: dev
worktree: CLEAN at planning start
local tag: dh-stage-qdr-4-close exists
local tag target: 62c802064f637ad03d3b0f4a185bd55fa3141af2
remote tag: dh-stage-qdr-4-close exists
remote peeled tag target: 62c802064f637ad03d3b0f4a185bd55fa3141af2
```

Stage-QDR-4 已完成：

```text
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
Final close review: PASS
Archive close: DONE
Tag close: DONE
```

代码现状支撑：

```text
Model Gateway 现有代码路径: dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
Regression 现有代码路径: dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
Gateway call persistence: qdr_model_gateway_call / V8
Replay regression persistence: V9
Actual absent path: dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/modelgateway
```

## 3. 候选方向评估

| 方向 | 结论 | 依据 | 边界 |
| --- | --- | --- | --- |
| A. Model Gateway Observability / Provider Readiness Hardening | 推荐主线 | Stage-QDR-3/4 已有 mock gateway、gateway call metadata、failure code、budget summary、trust decision、replay/regression report；缺口是 provider health summary、readiness gate、failure classification 聚合、latency/budget/retry policy summary 和 trust decision report。 | 仍只使用 mock provider；不接真实 HTTP，不接 Provider SDK，不读取 credential，不做 real provider dry-run。 |
| B. QDR Regression Baseline Hardening | 延后到 A 后或作为后续 Stage-QDR-6 候选 | Stage-QDR-4 已覆盖 comparator、tolerance、golden regression lifecycle 的初版能力；继续硬化有价值，但 provider readiness 是进入任何 real provider dry-run 前的更高优先级安全门。 | 不新增 API，不接 provider，不接 NQ，不进入 Agent。 |
| C. Real Provider Dry-run Readiness Plan | 后置规划，不在 Stage-QDR-5 实现 | feature flag、kill switch、credential boundary、network isolation、timeout/retry/circuit breaker 需要先依赖 A 的 readiness evidence 和 fail-closed reporting。 | 只允许未来单独 planning；不实现，不接 SDK，不发 HTTP。 |
| D. Agent / LangGraph Preparation | 明确后置 | 当前 audit / replay / regression / provider readiness / security gates 尚未证明 agent-ready；Agent runtime 会放大 provider、HTTP、prompt、credential 和 LIVE 误用风险。 | 只保留后置 checklist；不接 LangGraph runtime，不启动 Agent。 |

推荐顺序：

```text
1. A: Model Gateway Observability / Provider Readiness Hardening
2. B: QDR Regression Baseline Hardening
3. C: Real Provider Dry-run Readiness Plan
4. D: Agent / LangGraph Preparation
```

## 4. 推荐阶段名称

```text
Stage-QDR-5 = Model Gateway Observability / Provider Readiness Hardening
```

阶段目标是把现有 mock gateway call、provider trust、budget / usage、failure code 和 regression report 汇总为可审计、tenant-bound、fail-closed 的 provider readiness evidence。它不是 provider 接入阶段，也不是 Agent 阶段。

## 5. Stage-QDR-5 批次

### B1: Model Gateway Observability Contracts

目标：

```text
provider health summary contract
gateway call failure classification contract
latency / budget summary contract
retry policy summary contract
trust decision summary contract
provider readiness summary contract
```

约束：

```text
no migration if possible
no API
no real provider
no real HTTP
no Provider SDK
no raw prompt storage
no raw provider response storage
no credential storage
```

### B2: Provider Health / Gateway Call Read Model

目标：

```text
internal read model only
tenant-bound query
model gateway call status aggregation
failure classification aggregation
latency / budget / usage projection
trust decision report projection
```

约束：

```text
prefer existing V8 qdr_model_gateway_call
no raw provider response
no credential
no API unless separately reviewed
schema gap must stop as STAGE_QDR_5_SCHEMA_GAP_BLOCKER
```

### B3: Provider Readiness Guard / Policy Evaluation

目标：

```text
readiness decision
fail-closed classification
provider readiness gate
mock-only provider eligibility
source-bound trust policy
no-approval-as-execution guard
no-gateway-result-as-trading-signal guard
```

约束：

```text
no real provider
no HTTP
no Provider SDK
no credential lookup
all failures fail-closed
```

### B4: Observability Report / Current Docs / Acceptance Support

目标：

```text
observability report support
readiness report support
current docs sync
acceptance evidence support
security boundary evidence
```

约束：

```text
no API unless separately reviewed
no migration unless separately reviewed
no runtime provider
no Agent / LangGraph
no LIVE
```

### B5: Stage-QDR-5 Final Close Review

目标：

```text
Stage-QDR-5 final close review
archive close
tag close
```

规则：

```text
B1/B2/B3/B4 只是 Stage-QDR-5 batch，不创建 tag。
Stage tag 只能在 B5 final close PASS 且 archive close commit 存在后创建。
Tag close remains a separate explicit task after archive close.
```

## 6. Review 触发规则

沿用文档纪律修复后的规则：

```text
ordinary batch: implementation + tests + boundary scan + minimal docs + commit
standalone review trigger: migration
standalone review trigger: API / Controller
standalone review trigger: security boundary
standalone review trigger: stage close
standalone review trigger: P0 / P1 blocker
stage final close PASS -> archive close -> tag close -> next stage planning
```

普通 B1-B4 batch 不做 standalone review。若 B1-B4 中发现 migration、API、security boundary 或 P0/P1 blocker，则停止当前 batch，输出对应 blocker，并进入单独 review 任务。

## 7. 安全门

Stage-QDR-5 必须保持：

```text
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
ALLOW_NQ_MUTATION: NO
ALLOW_TRADING_MUTATION: NO
no approval-as-execution
no gateway-result-as-trading-signal
no raw prompt storage
no raw provider response storage
no credential storage
all failures fail-closed
tenant-bound query
source-bound trust policy
```

任何 future real provider dry-run 只能在 Stage-QDR-5 close 后另起 planning，并先完成 feature flag、kill switch、credential boundary、network isolation、timeout / retry / circuit breaker、sandbox provider 和 no-live policy 审查。

## 8. 下一步

```text
STAGE_QDR_5_PLAN: DONE
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
Next task: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
```
