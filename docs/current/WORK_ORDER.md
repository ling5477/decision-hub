# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-FIX-REVIEW
current task status: PASS / SOURCE_PRODUCTION_DRIFT_CLOSED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW
mode: SCHEMA_SECURITY_REVIEW_ONLY + SOURCE_CONTRACT_EXACT_WIRE + NO_CAPACITY_BENCHMARK + NO_STAGE_QDR_7_B2_IMPLEMENTATION + NO_EXTERNAL_HTTP + NO_PROVIDER + NO_NQ + NO_AGENT + NO_LIVE
```

Stage-QDR-7 source production-code drift已由`044afba`与独立security fix review关闭：runtime properties不再lowercase request source，配置source与tenant/source pair均保持canonical case并在非法时fail-closed。下一步只允许B2 persistent guards schema/security review；B2 implementation、API、migration、repository expansion和benchmark retry继续未授权。

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
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: COMPLETE / B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE_COMMITTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_B1_IMPLEMENTATION: YES / CONSUMED
STAGE_QDR_6_B2: DONE / EVIDENCE_AGGREGATION_EXISTING_PORTS_ONLY
ALLOW_STAGE_QDR_6_B2_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION_NOW: NO / CONSUMED
SNAPSHOT_PERSISTENCE_GAP_REVIEW: DONE
PERSISTENCE_DESIGN_FROZEN: YES
ALLOW_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: YES
SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: DONE
STAGE_QDR_6_B3_P1: DONE / IMPLEMENTED / POSTGRESQL_VERIFIED
ALLOW_B3_P1_MIGRATION_IMPLEMENTATION: YES / CONSUMED
STAGE_QDR_6_B3_P2: DONE / IMPLEMENTED / POSTGRESQL_VERIFIED
ALLOW_B3_P2_PORT_JDBC_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_B3_PERSISTENCE_MILESTONE_REVIEW: YES / CONSUMED / BLOCKED
ALLOW_B3_PERSISTENCE_MILESTONE_REVIEW_RETRY: YES / CONSUMED / PASS
DH_STAGE_QDR_6_B3_PERSISTENCE_SCHEMA_BLOCKER_FIX: DONE / POSTGRESQL_VERIFIED
STAGE_QDR_6_B3_P3: DONE / IMPLEMENTED / POSTGRESQL_VERIFIED
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_DETERMINISTIC_HASH_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_IMMUTABLE_SNAPSHOT_PERSISTENCE_IN_P3: YES / CONSUMED
DETERMINISTIC_REPLAY_BASELINE_GATE: PASS
STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_BASELINE: DONE / IMPLEMENTED / VERIFIED
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: YES / CONSUMED
ALLOW_REPLAY_COMPARATOR_IMPLEMENTATION: YES / CONSUMED
ALLOW_B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW: YES / CONSUMED / PASS
ALLOW_B3_CLOSE_REVIEW: YES / CONSUMED / PASS
STAGE_QDR_6_B3: CLOSED / ACCEPTED
ALLOW_B4_INTERNAL_REPORT_IMPLEMENTATION: YES / CONSUMED
ALLOW_B3_ADDITIONAL_IMPLEMENTATION_NOW: NO
STAGE_QDR_6_B4_INTERNAL_REPORT: DONE / IMPLEMENTED / VERIFIED
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW: YES / CONSUMED / PREVIOUS_BLOCKED
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
STAGE_QDR_6_FINAL_CLOSE_BLOCKER_FIX: DONE
CURRENT_FACTSOURCE_CONFLICT: CLEARED
CURRENT_FACTSOURCE_CONSISTENCY: PASS
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: YES / CONSUMED
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES
STAGE_QDR_6_ARCHIVE_PATH: docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED
STAGE_QDR_7_PLAN: DONE / PLAN_ONLY
STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_7_MAINLINE: LIMITED_DRY_RUN_RUNTIME_READINESS
ALLOW_STAGE_QDR_7_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B1_CONTRACT_FREEZE: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
ALLOW_RUNTIME_CONTRACT_IMPLEMENTATION_NOW: NO
ALLOW_RATE_LIMIT_IMPLEMENTATION_NOW: NO
ALLOW_IDEMPOTENCY_IMPLEMENTATION_NOW: NO
ALLOW_ADDITIONAL_B4_IMPLEMENTATION_NOW: NO
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_EVIDENCE_CONSOLIDATION_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
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
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
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
implementation status: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
implementation allowed now: NO / CONSUMED
scope: internal report and current docs acceptance support
default: no API, no migration, no real provider / HTTP, no Agent / LangGraph, no LIVE
```

### B5

```text
allowed now: NO / CONSUMED
status: CONSUMED / TAGGED
order: final close review PASS -> archive close docs commit -> annotated tag -> tag push -> post-tag current cleanup -> next stage planning
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

完整矩阵见 `docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md`。

## 7. 当前禁止范围

```text
B2 implementation 与 B2 CI blocker fix 已完成。
B3 implementation 与 close review 已完成，范围限于 provider readiness guard / policy evaluation internal boundary。
B4 work order 已完成，范围限于 internal report / acceptance support boundary design。
下一步只允许进入 Stage-QDR-6 planning-first。
禁止跳过 Stage-QDR-6 planning-first 直接进入 implementation/runtime。
禁止新增 V12+ migration；V11 metadata-only blocker fix 已完成。
禁止修改 V1-V10 migration。
禁止新增 API / Controller / REST endpoint。
禁止新增 Repository / JDBC；milestone retry 只重新授权 P3 assembler/canonicalizer/hash/persistence service 与显式 `REPEATABLE_READ` transaction boundary。
deterministic replay implementation 只能复用现有 snapshot port/record/canonicalizer/hasher，不得重读可变 V5/V6/V8/V9 source。
禁止把现有 `QdrRegressionComparator` 当作 deterministic replay comparator。
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
DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE
```
