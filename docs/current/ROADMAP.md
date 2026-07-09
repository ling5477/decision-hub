# Decision Hub Roadmap

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 1. 当前路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。当前主线为 Quant Decision Review。

```text
stage-qdr-1: CLOSED / ACCEPTED
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
STAGE_QDR_4_B3_PLAN: DONE
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
stage-qdr-4 B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES / CONSUMED
B4 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO / ALREADY_DONE
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES / CONSUMED
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED
STAGE_QDR_5_ARCHIVE: PENDING
STAGE_QDR_5_TAG: NOT_CREATED
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
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
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES
ALLOW_STAGE_QDR_5_TAG_NOW: NO
ALLOW_STAGE_QDR_6_PLAN_NOW: NO
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_NOW: NO / ALREADY_TAGGED
```

## 2. 当前下一步

```text
DH-STAGE-QDR-5-PLAN: DONE
DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER: DONE
DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS: DONE
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO: DONE
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION: DONE
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO: DONE
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION: DONE
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-CLOSE-REVIEW: PASS
DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO: DONE
DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION: DONE
DH-STAGE-QDR-5-FINAL-CLOSE-REVIEW: PASS
Then: DH-STAGE-QDR-5-ARCHIVE-CLOSE
Then: tag close only after archive close commit
```

Stage-QDR-5 plan 已选择推荐主线：

```text
Stage-QDR-5 = Model Gateway Observability / Provider Readiness Hardening
```

推荐顺序：

```text
1. Model Gateway Observability / Provider Readiness Hardening
2. QDR Regression Baseline Hardening
3. Real Provider Dry-run Readiness Plan
4. Agent / LangGraph Preparation
```

Stage-QDR-5 批次规划：

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Current Docs / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
```

B1 已完成为 contracts-only batch；B2 Provider Health / Gateway Call Read Model work order、implementation 与 CI blocker fix 已完成；B3 Provider Readiness Guard / Policy Evaluation work order、implementation 与 close review 已完成，B3 为 `CLOSED / ACCEPTED`。B4 Observability Report / Acceptance Support work order 与 implementation 已完成。Stage-QDR-5 final close review 已 `PASS`，Stage-QDR-5 当前为 `CLOSED / ACCEPTED`。当前唯一下一步是 Stage-QDR-5 archive close。B2 implementation 只做 internal read model，未新增 API、migration 或 production repository/JDBC expansion。B3 是 trust/security boundary 批次，close review 结论为 `PASS`；B4 不新增 API、migration、production repository/JDBC、real provider/HTTP、Provider SDK、Agent、LangGraph 或 LIVE。Archive close 之后才允许单独 tag close；不得在本阶段 review 中直接创建 tag。Stage tag 只能在 archive close commit 存在后，由独立 tag close 任务创建。Real provider dry-run 与 Agent / LangGraph preparation 均后置，不在 Stage-QDR-5 implementation 中启动。

B2 implementation boundary:

```text
target: Provider Health / Gateway Call Read Model
type: internal read model only
source: B1 contracts + existing model gateway call persistence + QDR gateway result / mock provider safe evidence
default: no API / Controller, no migration, no production repository/JDBC expansion
query: tenant-bound only; no UUID-only query, no tenantless list, no cross-tenant read, pageSize max 100
view: safe refs / hashes / redacted summary / enum summaries only
blockers: B2_API_REQUIRED_BLOCKER, B2_SCHEMA_EXTENSION_REQUIRED_BLOCKER, B2_REPOSITORY_EXTENSION_REQUIRED_BLOCKER, security boundary review
```

Stage-QDR-5 batch 测试矩阵至少覆盖：

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

B3 Provider Readiness Guard / Policy Evaluation implementation test matrix 另需覆盖：

```text
valid readiness policy evaluates READY in mock/safe context
missing tenantId returns NOT_READY or fail-closed
missing providerRef returns NOT_READY or fail-closed
missing policyVersion returns SKIPPED or NOT_READY
source denied returns NOT_READY
policy denied returns NOT_READY
timeout classification returns DEGRADED or NOT_READY
budget exceeded returns DEGRADED or NOT_READY
unknown classification returns NOT_READY
credential-like input fails closed
raw prompt input fails closed
raw provider response input fails closed
BUY / SELL / MARKET_ORDER input fails closed
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE input fails closed
READY does not enable real provider
READY does not enable real HTTP
READY does not enable LIVE
READY does not imply trading permission
no Provider SDK / HTTP client / Agent / LangGraph classes introduced
policy evaluation failure fails closed
quality validate passes
```

## 3. 后续阶段边界

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 B1 已 `DONE / DOMAIN_CONTRACTS_ONLY`。B2 plan 已 `DONE / PERSISTENCE_BASELINE_PLAN_ONLY`，B2 freeze review 已 `PASS`，B2 implementation work order 已 `DONE / WORK_ORDER_ONLY`，B2 blocker fix 已 `DONE / TESTCONTAINERS_VERIFIED`，B2 implementation 已 `DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED`，B2 close review 已 `PASS`，B2 已 `CLOSED / ACCEPTED`。B3 plan 已 `DONE / PLAN_ONLY`，B3 WO 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED`，B3 close review 已 `PASS`，B3 已 `CLOSED / ACCEPTED`。B4 plan 已 `DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY`，B4 WO 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED`。Stage-QDR-4 final close review 已 `PASS`，整体 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`，tag close 已 `DONE`。Stage-QDR-5 plan 已 `DONE / PLAN_ONLY`，implementation work order 已 `DONE / WORK_ORDER_ONLY`，B1 已 `DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY`，B2 work order 已 `DONE / WORK_ORDER_ONLY`，B2 implementation 已 `DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED`，B3 work order 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED`，B3 close review 已 `PASS`，B3 为 `CLOSED / ACCEPTED`，B4 work order 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED`，final close review 已 `PASS`，Stage-QDR-5 为 `CLOSED / ACCEPTED`。下一步只能进入 Stage-QDR-5 archive close，不直接 tag，不进入 Stage-QDR-6。real HTTP、real provider、Provider SDK、Agent / LangGraph runtime 和 LIVE 仍然后置且禁止。

## 4. 持续禁止项

```text
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
NQ mutation: NO
NQ DB read/write: NO
trading execution: NO
```

历史路线、旧 work order、旧 review / freeze 记录、blocker fix 过程和 pre-close snapshots 已归入 `docs/gates/**` 或由 `ARCHIVE_INDEX.md` 索引。它们保留复盘价值，但不能作为当前 next action 或 B5 blocker，除非触发 `FACTSOURCE_POLICY.md` 定义的硬错误。`docs/archive/**` 不再作为 QDR 当前归档标准。
