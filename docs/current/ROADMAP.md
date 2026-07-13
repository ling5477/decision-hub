# Decision Hub Roadmap

## Current route — 2026-07-13 schema errata factsource alignment

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata implementation: DONE
B2 schema errata review: TECHNICAL_PASS / FACTSOURCE_FIX_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
schema errata technical implementation: PASS
schema errata independent acceptance: BLOCKED only by factsource drift
callback/migration code changes: NOT_REQUIRED
current task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2
```

当前路线只允许完成factsources对齐后进入独立review retry-2。milestone retry-3、capacity acceptance与B3继续禁止。

## Historical routes — 以下全部内容均为非当前路线

下方日期记录及旧`当前路线`/`当前下一步`标题均为historical/consumed快照，不得覆盖上方current route。

## 2026-07-13 Stage-QDR-7 B2 schema errata implementation review retry blocked

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata implementation: BLOCKED / CURRENT_FACTSOURCE_FIX_REQUIRED
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
callback timeout scope: PASS
PostgreSQL rollback/retry/session isolation: PASS
current factsource consistency: FAIL
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
```

技术blocker已由`479dbc`关闭，但current factsources仍存在未标historical的旧阶段入口，因此不能进入milestone review retry-3。下一步仅允许current factsources最小修复与独立复核；capacity acceptance和B3继续禁止。

## 2026-07-13 Stage-QDR-7 B2 schema errata implementation blocker fix

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata blocker fix: DONE / REVIEW_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY
```

受控blocker fix已把冻结的5秒lock timeout和60秒statement timeout前移到guard precheck之前，并以PostgreSQL 17 `ACCESS EXCLUSIVE`锁回归闭合整体rollback、session隔离和retry。当前只允许独立schema errata implementation review retry；不得进入milestone retry-3、capacity acceptance或B3。

## 2026-07-12 Stage-QDR-7 B2 milestone review retry-2 blocked

```text
B1 runtime contract: FROZEN
B2 implementation: BLOCKED / P1_FIX_REQUIRED
callback security: BLOCKED / UNBOUNDED_TABLE_SCOPE
V14 narrowing: BLOCKED / EXPLICIT_CAST_AND_RETRY_EVIDENCE_GAP
actual completion / lifecycle terminal timestamp / cleanup CAS-miss evidence: INSUFFICIENT
current factsource consistency: BLOCKED
PostgreSQL regression: PASS / 17.10 / ZERO_SKIPS / INSUFFICIENT_FOR_P1
post-B2 capacity acceptance: NOT_ALLOWED
B3 operational safety: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY-2
```

绿色测试不解除callback作用域、migration fail-closed语义、实际service completion和factsources冲突。修复必须另起受控blocker-fix任务。

## 2026-07-12 Stage-QDR-7 B2 blocker-fix retry done

```text
B1 runtime contract: FROZEN
B2 implementation: BLOCKER_FIX_RETRY_DONE / REVIEW_PENDING
pre-V13 compatibility + V14 schema alignment: PASS
JVM clock / actual JDBC result / production transaction / commit unknown / concurrent cleanup: PASS
PostgreSQL regression: PASS / 17.10 / ZERO_SKIPS
previous milestone review: BLOCKED / HISTORICAL_PRESERVED
post-B2 capacity acceptance: NOT_ALLOWED
B3 operational safety: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2
```

本轮仅关闭P1修复与证据缺口；B2尚未由独立review接受，不得运行capacity harness或进入B3。

## 2026-07-12 Stage-QDR-7 B2 milestone review retry blocked

```text
B1 runtime contract: FROZEN
B2 implementation: BLOCKED / P1_FIX_RETRY_REQUIRED
B2 milestone review retry: BLOCKED
V12→V13 compatibility: BLOCKED
V13 frozen schema alignment: BLOCKED
idempotency real-JDBC commit unknown: MISSING
production transaction wiring evidence: INSUFFICIENT
JVM clock offset / actual JDBC result / idempotency concurrent cleanup: INSUFFICIENT
PostgreSQL regression: PASS / 17.10 / ZERO_SKIPS
post-B2 capacity acceptance: NOT_ALLOWED
B3 operational safety: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY
```

下一轮必须保持V1-V13 immutable并采用新的forward migration，或先做明确schema errata review；不得把现有1055个绿色测试解释为P1证据已完整。capacity harness仍不得运行。

## 2026-07-12 Stage-QDR-7 B2 blocker fix done

```text
B1 runtime contract: FROZEN
B2 implementation: DONE / REVIEW_RETRY_READY
P1 blocker fix: DONE
V13 forward fix: PASS
expiry tombstone / DB clock / cleanup / transaction evidence: PASS
previous milestone review: BLOCKED / HISTORICAL_PRESERVED
post-B2 capacity acceptance: NOT_ALLOWED
B3 operational safety: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY
```

Review retry必须独立复核本轮证据；通过前不得运行capacity harness或进入B3。

## 2026-07-12 Stage-QDR-7 B2 milestone blocked

```text
B1 runtime contract: FROZEN
B2 implementation: DONE / REVIEW_BLOCKED
B2 milestone review: BLOCKED / P1_FIX_REQUIRED
expiry lifecycle: BLOCKED
cleanup DB-time safety: BLOCKED
frozen schema alignment: BLOCKED
completion/result transaction evidence: INSUFFICIENT
post-B2 capacity acceptance: NOT_ALLOWED
B3 operational safety: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX
```

Blocker fix必须保持V12 immutable并采用forward-only additive migration；如决定删除冻结的`lease_owner/result_type/failed_at`要求，必须先单独完成schema/security errata review。capacity harness不得与blocker fix混跑。

## 2026-07-12 Stage-QDR-7 B2 persistent guards implemented

```text
B1 runtime contract: FROZEN
B2 schema/security review: PASS / DESIGN_FROZEN
B2 implementation: DONE / LOCAL_VALIDATED
B2 migration: V12 / POSTGRESQL_17_10_VERIFIED
B2 persistent guards: IMPLEMENTED / NO_IN_MEMORY_FALLBACK
B2 milestone review: NEXT_TASK_ONLY
post-B2 capacity acceptance: REQUIRED / NOT_STARTED
B3 operational safety: BLOCKED_UNTIL_MILESTONE_REVIEW_AND_CAPACITY_ACCEPTANCE
B4 protected-entry acceptance: BLOCKED_UNTIL_CAPACITY_ACCEPTANCE
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW
```

Milestone review必须复核V12兼容性、exact key、atomic winner、CAS/lease/result reference、audit transaction coupling、cleanup和store/commit taxonomy。只有review通过后才可另起capacity acceptance；本轮实现未选择最终window/quota、lease/TTL、retention或cleanup默认值。

## 2026-07-12 Stage-QDR-7 B2 schema/security freeze

```text
B1 runtime contract: FROZEN
B2 schema/security review: PASS / DESIGN_FROZEN
B2 selected rate algorithm: FIXED_WINDOW_COUNTER
B2 migration candidate: V12 / NOT_CREATED
B2 persistent guards: NOT_IMPLEMENTED
B2 implementation: NEXT_TASK_ONLY
B2 actual-wiring 2xx harness: AFTER_IMPLEMENTATION
post-B2 capacity acceptance: REQUIRED
B3 operational safety: BLOCKED_UNTIL_B2_IMPLEMENTATION_AND_CAPACITY_ACCEPTANCE
B4 protected-entry acceptance: BLOCKED_UNTIL_CAPACITY_ACCEPTANCE
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```

Implementation必须保持exact`environment + endpoint + source + tenant`key、PostgreSQL fail-closed、无in-memory fallback、条件状态转换、immutable result reference和bounded cleanup。最终window/quota、lease/TTL、retention与cleanup默认值仍不在本review冻结。

## 2026-07-12 Stage-QDR-7 B1 capacity gate resequencing

```text
B1: safety semantics + config model + legal ranges + absolute hard ceiling / FROZEN
source contract: NQ_DRYRUN exact wire / FROZEN
source production-code drift: CLOSED / 044afba review PASS
B2 schema/security review: NEXT_TASK / FINAL_DEFAULTS_NOT_REQUIRED
B2 implementation: NOT_AUTHORIZED
B2 actual-wiring 2xx harness: AFTER_PERSISTENT_GUARDS
B2 capacity acceptance: REQUIRED / NOT_STARTED
B3 operational safety: AFTER_B2_IMPLEMENTATION
B4 protected-entry acceptance: BLOCKED_UNTIL_CAPACITY_ACCEPTANCE
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW
```

最终 deadline、concurrency、queue、rate、lease/TTL、cleanup 与 retention 默认值不得在 B1 编造。B2 schema/security review 先消费 pre-B2 safety contract；persistent guards 完成后，以 canonical source/HMAC、deterministic mock gateway、loopback、isolated PostgreSQL、Docker/Testcontainers 0 skipped 和完整 Tomcat/Hikari/JVM/HTTP metrics 运行可重复 2xx harness，再冻结 measured defaults。Store failure 禁止回退 in-memory。

## 2026-07-12 Stage-QDR-7 B1 capacity evidence retry

```text
B1 capacity evidence: BLOCKED / INSUFFICIENT
valid success samples: 0
Docker/Testcontainers: UNAVAILABLE / 21_SKIPPED
actual app-wired success harness: NOT_READY
Tomcat worker/queue metrics: NOT_OBSERVABLE
future persistent guard capacity: NOT_IMPLEMENTED / NOT_MEASURABLE
B2 schema/security review: NOT_AUTHORIZED
next action: DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER-RETRY
```

Retry前必须先提供不修改正式合同的2xx mock-only runtime seed/harness、可观测server queue/threads、Docker/PostgreSQL并发环境及future persistent guard容量测量路径。不得用403/500拒绝路径或standalone MockMvc结果冻结生产预算。

## 2026-07-12 Stage-QDR-7 B1 resource capacity blocker

```text
Stage-QDR-7 implementation: NOT_STARTED
B1 guard/truth/domain/duplicate/state/error/redaction contract: FROZEN
B1 resource budgets: BLOCKED / B1_RESOURCE_CAPACITY_EVIDENCE_BLOCKED
B2 schema/security review: NOT_AUTHORIZED
B2 implementation: NOT_AUTHORIZED
required evidence: latency percentiles + target instances + DB contention + growth/recovery/cleanup + kill propagation SLO
next action: DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER
```

当前只能保留 65536-byte raw payload 与 32768-byte decision context 兼容上限。不得把 JVM-local rate limit 的 1 秒/20 请求、通用 idempotency 的 10 分钟 TTL 或其他模块 timeout 直接升级为 protected endpoint 的容量合同。

## 2026-07-12 Stage-QDR-7 implementation route

```text
Stage-QDR-7 plan: DONE / PLAN_ONLY
Stage-QDR-7 implementation work order: DONE / WORK_ORDER_ONLY
Stage-QDR-7 implementation: NOT_STARTED
B1: Runtime Contract / Safety Policy Freeze / NEXT_TASK_ONLY
B2: Persistent Multi-instance Guards / NOT_AUTHORIZED / SCHEMA_SECURITY_REVIEW_FIRST
B3: Operational Safety / Resilience / NOT_AUTHORIZED
B4: Protected Entry Readiness Acceptance / NOT_AUTHORIZED
B5: Final Close -> Archive -> Annotated Tag -> Push -> Cleanup / NOT_STARTED
outbound HTTP retry: DEFERRED / NO_REAL_TARGET
Provider retry/circuit breaker: DEFERRED / NO_REAL_TARGET
next action: DH-STAGE-QDR-7-B1-RUNTIME-CONTRACT-SAFETY-POLICY
```

B1 只冻结合同，不改 Controller/OpenAPI/production code。B2 必须先完成 migration、schema、production port/JDBC 与 security 的统一 milestone review；B3 只实现当前有真实目标的 deadline、bounded concurrency/backpressure、dynamic kill、environment isolation、redaction/audit/metrics 与 invalid configuration fail-closed。B4 只读验收既有 protected endpoint；如需 API/Controller 变化则独立阻断。

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 1. 当前路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。当前主线为 Quant Decision Review。

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED
STAGE_QDR_7_PLAN: DONE / PLAN_ONLY
STAGE_QDR_7_MAINLINE: LIMITED_DRY_RUN_RUNTIME_READINESS
next action: DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: YES / CONSUMED
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES
ALLOW_STAGE_QDR_7_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
```

Stage-QDR-6 已完成archive、annotated tag、remote verification与post-tag current pruning；previous BLOCKED与retry PASS继续保存在archive。Stage-QDR-7 planning 已完成，推荐先关闭 persistent multi-instance rate limit、idempotency、replay interaction、resource caps、kill switch与fail-closed policy，再做 operational resilience 和既有 protected entry acceptance。下一步只允许 implementation work order；Stage-QDR-7 implementation仍未启动。

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
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: IMPLEMENTING / WORK_ORDER_DONE / B1_DONE / B2_DONE / B3_WORK_ORDER_DONE
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: B1_DONE / B2_DONE / B3_NOT_STARTED
STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED
STAGE_QDR_8: NOT_STARTED
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
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
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
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
DH-STAGE-QDR-5-ARCHIVE-CLOSE: DONE
DH-STAGE-QDR-6-PLAN: DONE / PLAN_ONLY
DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER: DONE / WORK_ORDER_ONLY
DH-STAGE-QDR-6-B1-EVIDENCE-CORRELATION-AGGREGATE-CONTRACTS: DONE / COMMITTED
DH-STAGE-QDR-6-B2-EVIDENCE-AGGREGATION-SERVICE: DONE / COMMITTED
DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER: DONE / WORK_ORDER_ONLY
Then: DH-STAGE-QDR-6-B3-P1-CANONICAL-SNAPSHOT-MIGRATION
```

Stage-QDR-6 主线已冻结：

```text
Stage-QDR-6 = Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
Stage-QDR-7 = Limited Dry Run Runtime Readiness / PLANNING / IMPLEMENTATION_NOT_STARTED
Stage-QDR-8 = Agent Runtime Contract Baseline / NOT_STARTED
```

Stage-QDR-6 只收敛统一 evidence correlation/aggregate、tenant-bound aggregation、mock-only deterministic replay baseline 与 internal report/acceptance evidence。Stage-QDR-7 的 multi-instance rate limit、formal dry-run runtime contract、idempotency、kill switch、timeout/retry/circuit breaker 和 cross-repo contract tests 全部后置；Stage-QDR-8 的 Agent/LangGraph contract 只能在 Stage-QDR-7 完成后重新评估。当前禁止真实 HTTP/provider、Provider SDK、NQ runtime integration、Agent/LangGraph 与 LIVE。

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

B1 已完成为 contracts-only batch；B2 Provider Health / Gateway Call Read Model work order、implementation 与 CI blocker fix 已完成；B3 Provider Readiness Guard / Policy Evaluation work order、implementation 与 close review 已完成，B3 为 `CLOSED / ACCEPTED`。B4 Observability Report / Acceptance Support work order 与 implementation 已完成。Stage-QDR-5 final close review、archive close 与 tag close 均已完成，本轮补齐 post-tag current cleanup，Stage-QDR-5 当前为 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。当前唯一下一步是 Stage-QDR-6 planning-first。B2 implementation 只做 internal read model，未新增 API、migration 或 production repository/JDBC expansion。B3 是 trust/security boundary 批次，close review 结论为 `PASS`；B4 不新增 API、migration、production repository/JDBC、real provider/HTTP、Provider SDK、Agent、LangGraph 或 LIVE。Stage-QDR-5 tag 已完成为 `dh-stage-qdr-5-close`；后续不得直接进入 Stage-QDR-6 implementation/runtime。Real provider dry-run 与 Agent / LangGraph preparation 均后置，不在 Stage-QDR-5 implementation 中启动。

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

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 B1 已 `DONE / DOMAIN_CONTRACTS_ONLY`。B2 plan 已 `DONE / PERSISTENCE_BASELINE_PLAN_ONLY`，B2 freeze review 已 `PASS`，B2 implementation work order 已 `DONE / WORK_ORDER_ONLY`，B2 blocker fix 已 `DONE / TESTCONTAINERS_VERIFIED`，B2 implementation 已 `DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED`，B2 close review 已 `PASS`，B2 已 `CLOSED / ACCEPTED`。B3 plan 已 `DONE / PLAN_ONLY`，B3 WO 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED`，B3 close review 已 `PASS`，B3 已 `CLOSED / ACCEPTED`。B4 plan 已 `DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY`，B4 WO 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED`。Stage-QDR-4 final close review 已 `PASS`，整体 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`，tag close 已 `DONE`。Stage-QDR-5 plan 已 `DONE / PLAN_ONLY`，implementation work order 已 `DONE / WORK_ORDER_ONLY`，B1 已 `DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY`，B2 work order 已 `DONE / WORK_ORDER_ONLY`，B2 implementation 已 `DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED`，B3 work order 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED`，B3 close review 已 `PASS`，B3 为 `CLOSED / ACCEPTED`，B4 work order 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED`，final close review、archive close、tag close 与 current cleanup 均已完成，Stage-QDR-5 为 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。下一步只能进入 Stage-QDR-6 planning-first，不进入 Stage-QDR-6 implementation/runtime。real HTTP、real provider、Provider SDK、Agent / LangGraph runtime 和 LIVE 仍然后置且禁止。

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
