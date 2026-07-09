# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
current task status: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
next action: DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO
mode: IMPLEMENTATION + DOMAIN_USECASE_CONTRACTS_ONLY + MODEL_GATEWAY_OBSERVABILITY + PROVIDER_READINESS_FOUNDATION + TESTS + NO_DB_MIGRATION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
```

Stage-QDR-5 B1 已完成。本工单只授权下一任务 B2 Provider Health / Gateway Call Read Model work order；不得直接进入 B2 implementation、B3 或 all-in-one implementation，不得创建 tag，不得 push。

## 2. 前置状态

```text
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / CONTRACTS_ONLY
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO / ALL_IN_ONE_FORBIDDEN
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 3. Stage-QDR-5 主线

```text
Stage-QDR-5 = Model Gateway Observability / Provider Readiness Hardening
```

Stage-QDR-5 目标是把现有 mock model gateway call、provider trust decision、budget / usage summary、failure code、redacted refs 与 replay/regression evidence 汇总为可审计、tenant-bound、fail-closed 的 provider readiness evidence。该 readiness 只表示未来接入条件评估，不是 provider authorization、live enablement、trading permission 或 execution approval。

## 4. 批次边界

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
```

### B1

```text
next task: DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
status: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
allowed now: YES / CONSUMED
scope: domain/usecase contracts
suggested objects: ModelGatewayObservabilitySummary, ProviderHealthSummary, ProviderFailureClassification, ProviderLatencyBudgetSummary, ProviderTrustDecisionSummary, ProviderReadinessSignal
review: no standalone review unless migration / API / security expansion / P0-P1 blocker appears
```

### B2

```text
work order allowed now: YES
implementation allowed now: NO
scope: internal tenant-bound read model
source: existing V8 qdr_model_gateway_call / ModelGatewayCallRecord / safe refs
default: no API, no migration, no raw provider response, no credential, no real provider, no real HTTP
blocker: API / migration / repository production expansion / cross-tenant read
```

### B3

```text
allowed now: NO
scope: provider readiness decision / trust gate / fail-closed classification
required: readiness is future condition evidence only
review: close review or security-boundary review required
```

### B4

```text
allowed now: NO
scope: internal report and current docs acceptance support
default: no API, no migration, no real provider / HTTP, no Agent / LangGraph, no LIVE
```

### B5

```text
allowed now: NO
order: final close review PASS -> archive close docs commit -> worktree clean -> annotated tag -> tag push -> next stage planning
```

## 5. Review 触发规则

```text
ordinary batch: implementation + tests + boundary scan + minimal docs + commit
standalone review trigger: migration
standalone review trigger: API / Controller
standalone review trigger: security boundary
standalone review trigger: stage close
standalone review trigger: P0 / P1 blocker
stage final close PASS -> archive close -> tag close -> next stage planning
```

普通 batch 不做 standalone review。若 implementation work order 或后续 batch 发现必须新增 migration、API / Controller、production repository expansion 或安全边界变化，必须停止并输出 blocker，进入单独 review 任务。

## 6. 测试矩阵摘要

```text
valid provider health summary can be created
missing tenantId fails closed
missing providerRef fails closed
failure classification supports timeout / budget exceeded / policy denied / source denied / unknown
latency budget summary records p50 / p95 / p99 or equivalent safe fields
trust decision summary records allowed / denied / degraded / skipped
readiness signal cannot enable real provider / real HTTP / LIVE
readiness signal cannot generate trading signal
raw provider response is rejected
credential-like key is rejected
provider health read model is tenant-bound
cross-tenant read returns empty or fail-closed
providerSummaryHash / modelGatewayVersionRef are safe refs only
no Provider SDK / HTTP client / Agent / LangGraph classes introduced
repository or report failure fails closed
quality validate passes
```

完整矩阵见 `docs/current/DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md`。

## 7. 当前禁止范围

```text
禁止进入 B2 implementation，除非后续 B2 work order 或 plan 明确授权。
禁止进入 B3。
禁止新增 migration。
禁止修改 V1-V9 migration。
禁止新增 V10。
禁止新增 API / Controller / REST endpoint。
禁止新增 Repository / JDBC / Service production expansion，除非 review 重新授权。
禁止接真实 HTTP client。
禁止接真实 provider / Provider SDK。
禁止新增 OpenAI / Anthropic / Gemini / Ollama SDK。
禁止读取 credential / token / cookie / apiKey / apiSecret / passphrase。
禁止持久化 raw prompt。
禁止持久化 raw provider response。
禁止把 gateway-result、provider-readiness 或 provider-health 写成 trading signal。
禁止启动 Agent runtime。
禁止接 LangGraph / AutoGen / CrewAI。
禁止修改 NQ。
禁止开启 LIVE。
禁止创建新 tag。
禁止 push。
```

## 8. 下一任务

```text
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO
```
