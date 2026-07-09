# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO
current task status: DONE / WORK_ORDER_ONLY
next action: DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION
mode: WORK_ORDER_ONLY + B4_IMPLEMENTATION_BOUNDARY_DESIGN + OBSERVABILITY_REPORT_WO + PROVIDER_READINESS_ACCEPTANCE_SUPPORT + CURRENT_DOCS_ACCEPTANCE_SUPPORT + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
```

Stage-QDR-5 B1 已完成，B2 Provider Health / Gateway Call Read Model implementation 与 CI blocker fix 已完成，B3 Provider Readiness Guard / Policy Evaluation implementation 已完成。B3 security boundary / close review 已 `PASS`，B3 为 `CLOSED / ACCEPTED`。B4 Observability Report / Acceptance Support work order 已完成为 `DONE / WORK_ORDER_ONLY`。下一步只允许进入 B4 implementation；B4 implementation 仍为 `NOT_STARTED`，不得 all-in-one implementation，不得直接 final close，不得创建 tag，不得 push。

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
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B2_CI: FIXED / ARCHITECTURE_SOURCE_SCAN_FIXED
STAGE_QDR_5_B2_CI_BLOCKER_FIX: DONE
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: NOT_STARTED
STAGE_QDR_5_FINAL_CLOSE: NOT_STARTED
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_WO_DONE / B4_IMPLEMENTATION_NOT_STARTED
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO / ALL_IN_ONE_FORBIDDEN
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / AFTER_B4_WO
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: YES / AFTER_B4_WO
ALLOW_STAGE_QDR_5_FINAL_CLOSE_NOW: NO
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

Stage-QDR-5 目标是把现有 mock model gateway call、provider trust decision、budget / usage summary、failure code、redacted refs 与 replay/regression evidence 汇总为可审计、tenant-bound、fail-closed 的 readiness evidence。该 readiness 只表示未来接入条件评估，不是 provider authorization、live enablement、trading permission 或 execution approval。

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
work order status: DONE / WORK_ORDER_ONLY
implementation status: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
scope: internal tenant-bound read model
source: existing V8 qdr_model_gateway_call / ModelGatewayCallRecord / safe refs
default: no API, no migration, no raw provider response, no credential, no real provider, no real HTTP
blocker: API / migration / repository production expansion / cross-tenant read
```

### B3

```text
plan/work order allowed now: YES / AFTER_B2_IMPLEMENTATION
work order status: DONE / WORK_ORDER_ONLY
implementation allowed now: YES / AFTER_B3_WO
implementation status: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
scope: provider readiness decision / trust gate / fail-closed classification
required: readiness is future condition evidence only
review: security-boundary / close review required after implementation; review PASS required before B4
close review: PASS / CLOSED / ACCEPTED
```

### B4

```text
work order status: DONE / WORK_ORDER_ONLY
implementation status: NOT_STARTED
implementation allowed now: YES / AFTER_B4_WO
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
B2 implementation 与 B2 CI blocker fix 已完成。
B3 implementation 与 close review 已完成，范围限于 provider readiness guard / policy evaluation internal boundary。
B4 work order 已完成，范围限于 internal report / acceptance support boundary design。
下一步只允许进入 B4 Observability Report / Acceptance Support implementation。
禁止跳过 B4 implementation 直接进入 Stage-QDR-5 final close。
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
DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION
```
