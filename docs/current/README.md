# Decision Hub Current Docs

## Terminal current authority — 2026-08-03 feedback ingest strict JSON single-root security blocker retry

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-SECURITY-BLOCKER-RETRY
Task result: FIXED / LOCAL_SECURITY_REMEDIATED / NOT PUSHED
Retry baseline / previous remediation HEAD: 77d778cc67ada445c52a74757ce30e9a537b72e3 / PRESERVED
Previous remediation parent: 2cb44f406ff618a531d1ca44e2c1e6848750ed75 / PRESERVED
Origin baseline: 16ecded2f3708d69e05afe5f2a4f621c823d8be3
Feedback ingest atomicity: IMPLEMENTED / LOCAL_SECURITY_REMEDIATED
Final-close attempts: BLOCKED HISTORY PRESERVED / RETRY PENDING
P1 duplicate validation order: FIXED / VALIDATION_AND_STRICT_CANONICALIZATION_BEFORE_LOOKUP
P1 exact event correlation: FIXED / EXACT_ENVELOPE_EVENT_IDENTITY_REQUIRED
P3 strict JSON single root: FIXED / ONE OBJECT ROOT + JSON WHITESPACE + EOF ONLY
Trailing object / array / scalar / boolean / null / malformed token: REJECTED / INVALID_SCHEMA / ZERO WRITES
Trailing whitespace: ACCEPTED
Duplicate JSON object keys: FAIL_CLOSED / INVALID_SCHEMA / ZERO WRITES
Canonical JSON: COMPLETE VALIDATED INPUT / OBJECT KEY REORDER REMAINS DUPLICATE
Current retry diff production / test / factsources / unexpected: 2 / 4 / 9 / 0
API / migration / schema / contracts / POM / workflow / NQ: UNCHANGED / NONE
Learning containment: UNCHANGED / INBOUND MUTABLE-STORE WRITES 0
Codex Security cumulative-diff review: PASS / 0 REPORTABLE FINDINGS / FINAL SNAPSHOT SEALED
Codex Security snapshot: codex-security-snapshot/v1:sha256:edc97872a8dfd71a0366f43c810d7b41010bff8569030fe5230d24828e46ac3a
Codex Security reviewed production worklist: 9 OF 9 / candidate receipts 3 OF 3
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_BLOCKED
Targeted reactor: PASS / 15 OF 15 REACTOR / POSTGRESQL TESTCONTAINERS
Full regression: PASS / 19 OF 19 REACTOR / 1291 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL / Flyway: 17.10 / V1-V15 / REAL TESTCONTAINERS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Implementation/remediation commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY / NOT PUSHED
Implementation publication / close tag: NOT_STARTED / ABSENT
Current factsources: 9 / 9 / SYNCHRONIZED / 0 CURRENT CONFLICTS
Production capacity / production ready: NOT_PROVEN / NO
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE-RETRY-2
ALLOW_FINAL_CLOSE_RETRY: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW / ALLOW_TAG_NOW: NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT / ALLOW_LANGGRAPH: NO / NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE: NO / NO
~~~

本节是 strict JSON single-root security blocker retry 完成后的唯一 terminal authority。此前两次
milestone final-close 的 `BLOCKED` 与 security review 历史均保留在下方，不能改写为已通过。本轮未执行
final close、push、tag、archive、capacity gate 或任何真实外部集成；后续 final-close retry 必须作为独立任务执行。

## Terminal current authority — 2026-08-02 feedback ingest atomicity security remediation

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-SECURITY-BLOCKER
Task result: FIXED / LOCAL_SECURITY_REMEDIATED / NOT PUSHED
Remediation baseline / blocked implementation: 2cb44f406ff618a531d1ca44e2c1e6848750ed75 / PRESERVED
Implementation parent / origin/dev: 16ecded2f3708d69e05afe5f2a4f621c823d8be3
Feedback ingest atomicity: IMPLEMENTED / LOCAL_SECURITY_REMEDIATED
Final close: BLOCKED ATTEMPT PRESERVED / RETRY PENDING
P1-1 duplicate validation order: FIXED / VALIDATION_AND_STRICT_CANONICALIZATION_BEFORE_LOOKUP
P1-2 exact event correlation: FIXED / EXACT_ENVELOPE_EVENT_IDENTITY_REQUIRED
Duplicate JSON object keys: FAIL_CLOSED / INVALID_SCHEMA / ZERO WRITES
Orphan / event-only / conflict / ambiguity: FAIL_CLOSED / NO_AUTOMATIC_REPAIR
Active P0 / P1: 0 / 0
Codex Security exact-diff review: PASS / 0 REPORTABLE FINDINGS / FINAL SNAPSHOT SEALED
Codex Security snapshot: codex-security-snapshot/v1:sha256:f4af9a31a77027b6e9b47e4ed241fcb928c64cbf8ee9d65169c21c98a021ddea
Atomic boundary: UNIT_OF_WORK / SAME_DATASOURCE / PROPAGATION_REQUIRED / UNCHANGED
JDBC / in-memory: EXACT_CORRELATION / TRANSACTION_AND_ROLLBACK_PARITY
Rollback / safe retry / response-loss retry: PASS / PASS / PASS
Commit unknown: FAIL_CLOSED / NO_AUTOMATIC_RETRY / UNCHANGED
Concurrent same key: ONE COMPLETE WINNER / CONFLICTS_NOT_DUPLICATES / NO_PARTIAL_STATE
Changed production / test / factsource files: 4 / 6 / 10 / 0 UNEXPECTED
API / migration / schema / contracts / POM / workflow / NQ: UNCHANGED / NONE
Learning containment: UNCHANGED / INBOUND MUTABLE-STORE WRITES 0
Targeted reactor: PASS / 15 OF 15 REACTOR / POSTGRESQL TESTCONTAINERS
Full regression: PASS / 19 OF 19 REACTOR / 1286 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL / Flyway: 17.10 / V1-V15 / REAL TESTCONTAINERS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY
Security remediation commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY / NOT PUSHED
Implementation publication: NOT_STARTED
Close tag: ABSENT
Current factsources: 10 / 10 / SYNCHRONIZED / 0 CURRENT CONFLICTS
Production capacity / production ready: NOT_PROVEN / NO
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE-RETRY
ALLOW_FINAL_CLOSE_RETRY: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW / ALLOW_TAG_NOW: NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT / ALLOW_LANGGRAPH: NO / NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE: NO / NO
~~~

本节是 security blocker remediation 完成后的唯一 terminal authority。原 milestone final-close 的
`BLOCKED` / security review `FAIL` 结论作为历史事实保留，不得改写为已通过；本轮只关闭两条 P1 并允许
后续独立 final-close retry。完整修复、测试与安全复核证据见
`DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_SECURITY_BLOCKER.md`。以下 local implementation、
work-order、planning 与更早区块均为历史时间线，不得覆盖本节或授权 push、tag、capacity、NQ/runtime
integration、真实 provider、Agent/LangGraph、Paper 或 LIVE。



## Terminal current authority — 2026-08-02 feedback ingest atomicity local implementation

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-CONSOLIDATED-IMPLEMENTATION
Task result: IMPLEMENTED / LOCAL_ACCEPTED / NOT PUSHED
Docs baseline: 16ecded2f3708d69e05afe5f2a4f621c823d8be3 / PUBLISHED
Docs baseline exact-SHA CI: 30710328666 / PASS / QUALITY 91396539930 + TESTCONTAINERS 91396539969
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
Feedback ingest atomicity: IMPLEMENTED / LOCAL_ACCEPTED
Atomic boundary: UNIT_OF_WORK / SAME_DATASOURCE / PROPAGATION_REQUIRED
JDBC: TRANSACTION_TEMPLATE / FAIL_FAST_DATASOURCE_IDENTITY / ON_CONFLICT_DO_NOTHING + READBACK
In-memory: ATOMIC_SNAPSHOT_RESTORE / JDBC_SEMANTIC_PARITY
Rollback: ENVELOPE_AND_EVENT_ALL_OR_NOTHING
Safe retry / response-loss retry: SUPPORTED / PASS
Commit unknown: INTERNAL_CLASSIFICATION / NO_AUTOMATIC_RETRY / NO_API_CHANGE
Concurrent same key: ONE COMPLETE WINNER / ENVELOPE 1 + EVENT 1
Scope invariants: PASS / 3 OF 3
Production / test / factsource allowlists: PASS / 7 / 9 / 9 EXACT PATHS / 0 UNEXPECTED
API / migration / schema / contracts / POM / workflow / NQ: UNCHANGED / NONE
Learning containment: UNCHANGED / PASS
Inbound ExperienceFeedbackService.apply callers: 0
Inbound ExperienceStore / PheromoneStore / FailureCaseStore writes: 0 / 0 / 0
Targeted atomicity matrix: PASS / 56 UNIT+API+JDBC + 55 APP/WIRING/ARCHITECTURE/POSTGRESQL
PostgreSQL / Flyway: 17.10 / V1-V15 / REAL TESTCONTAINERS / 0 SKIPPED
Module regression: PASS / 15 OF 15 REACTOR
Full regression: PASS / 19 OF 19 REACTOR / 1274 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY
Implementation commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY / NOT PUSHED
Remote implementation CI: PENDING
Milestone final close: NOT_STARTED
Production capacity / production ready: NOT_PROVEN / NO
Current factsources: 9 / 9 / SYNCHRONIZED / 0 CONFLICTS
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE
ALLOW_MILESTONE_FINAL_CLOSE: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW / ALLOW_TAG_NOW: NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT / ALLOW_LANGGRAPH: NO / NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE: NO / NO
~~~

本节是 consolidated implementation 本地接受后的唯一 terminal authority。implementation commit 只能保留
在本地；push、exact-diff security review、remote exact-SHA CI、archive、tag 与 post-tag cleanup 只能由独立
milestone final close 任务执行。以下 work-order、planning 与已关闭 milestone 区块均为历史时间线，不得覆盖本节。


## Terminal current authority — 2026-08-02 feedback ingest atomicity work order

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-IMPLEMENTATION-WORK-ORDER
Task result: DONE / SCOPE FROZEN / DOCS ONLY / IMPLEMENTATION NOT STARTED
Planning commit: f087f562bdf9802c19ebeebcb8c1523cd390130f / LOCAL ONLY
Closed milestone: CLOSED / ACCEPTED / ARCHIVED / TAGGED / NOT REOPENED
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
Selected design: UNIT_OF_WORK
Transaction participant inventory: PASS / SAME JDBC TEMPLATE / SYNCHRONOUS ROUTE
Atomic boundary / rollback / safe retry / commit unknown: FROZEN / FROZEN / FROZEN / FROZEN
JDBC / in-memory parity: FROZEN
Conflict arbitration: INSERT ON CONFLICT DO NOTHING + SAME-TRANSACTION READ-BACK
Production / test / factsource allowlists: FROZEN / 7 / 9 / 9 EXACT PATHS
Scope invariants: PASS / 3 OF 3
API / migration / schema: NONE / NONE / NONE
Technical implementation/test diff: 0 / 0
Quality validation: PASS / EXIT 0 / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Full tests: NOT_RERUN / WORK_ORDER ONLY
Work-order commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY / NOT PUSHED
Production capacity: NOT_PROVEN
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-CONSOLIDATED-IMPLEMENTATION
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT / ALLOW_LANGGRAPH: NO / NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE / ALLOW_PUSH / ALLOW_TAG: NO / NO / NO / NO
~~~

完整 code reality、transaction ownership、conflict-safe duplicate、failure taxonomy、in-memory snapshot、精确
allowlists 与 PostgreSQL/Testcontainers 矩阵见
`DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_IMPLEMENTATION_WORK_ORDER.md`。本轮未实施代码；以下 planning
与已关闭 milestone 区块均为历史时间线，不得覆盖本工单或授权范围外实现。


## Terminal current authority — 2026-08-02 post-feedback-containment next-stage planning

~~~text
Task: DH-POST-FEEDBACK-SIDE-EFFECT-CONTAINMENT-NEXT-STAGE-PLANNING
Plan result: DONE / SCOPE FROZEN / DOCS ONLY
Closed milestone: CLOSED / ACCEPTED / ARCHIVED / TAGGED / NOT REOPENED
Planning baseline: 071bc29ee3c03b4099623c4ade441783cc22091f
Selected workstream: FEEDBACK_ENVELOPE_EVENT_ATOMIC_PERSISTENCE
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
Selected stage type: DH_OWNED / PLATFORM_HARDENING / DATA_CONSISTENCY_AND_SECURITY_BOUNDARY
Current code gap: ENVELOPE SAVE AND LEGACY EVENT APPEND LACK ONE ATOMIC RESULT
Risk classification: SECURITY_HARDENING_CANDIDATE / NOT CURRENT P0 OR P1
Selected stage scope: FROZEN / 4 BATCHES
Migration / API impact: NONE / NONE
Repository impact: YES / B1 REVIEW REQUIRED
Formal capacity: NOT_EXECUTED / PRODUCTION_READINESS_GATE / DEFERRED
Production capacity: NOT_PROVEN
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-IMPLEMENTATION-WORK-ORDER
ALLOW_NEXT_STAGE_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_NEXT_STAGE_IMPLEMENTATION_NOW: NO
ALLOW_FEEDBACK_LEARNING / ALLOW_REFERENCE_LIVENESS / ALLOW_RETENTION / ALLOW_CAPACITY: NO / NO / NO / NO
ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
~~~

本节是新的 planning authority。完整候选矩阵、transaction/repository review gate、4 个 batches、测试与
close discipline 见 `DH_POST_FEEDBACK_SIDE_EFFECT_CONTAINMENT_NEXT_STAGE_PLAN.md`。本规划只允许进入
implementation work order，不授权技术实现；以下 feedback containment 与 Stage-QDR-9 区块均为已关闭历史。


## Terminal current authority — 2026-08-01 feedback containment milestone final close

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-MILESTONE-FINAL-CLOSE
Milestone: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Implementation: fddf3558255b5a4f8f6071da42363649942afe46 / PUBLISHED
Implementation exact-SHA CI: 30701063741 / PASS / QUALITY + TESTCONTAINERS
Security exact-diff review: PASS / 0 REPORTABLE FINDINGS / ACTIVE P0-P3 0
CodeRabbit: NOT_EXECUTED / TOOL_OR_NETWORK_BLOCKED
Ingress boundary: INGEST_ONLY / NO_IMPLICIT_LEARNING / NO_MUTABLE_LEARNING_STORE_ACCESS
Inbound ExperienceFeedbackService.apply callers: 0
Inbound ExperienceStore / PheromoneStore / FailureCaseStore writes: 0 / 0 / 0
ExperienceFeedbackService and mutable stores: RETAINED / NOT REACHABLE FROM INBOUND FEEDBACK
API / migration / Repository / contracts / POM / workflow / NQ: UNCHANGED / NONE
Legacy environment contract: UNCHANGED / CHARACTERIZED
Structured QDR attribution: INTERNAL-ONLY / UNCHANGED / NOT CONNECTED
Archive: docs/gates/platform-hardening-feedback-side-effect-containment/ / COMPLETE
Archive sources / SHA-256: 2 OF 2 / VERIFIED / 0 MISSING / 0 UNEXPECTED
Archive close commit: 86381c6a47a5d68eb7ab9f57892e42e282ac63ea / PUBLISHED
Archive close exact-SHA CI: 30702114843 / PASS / QUALITY 91374827027 + TESTCONTAINERS 91374826988
Close tag: dh-platform-hardening-feedback-side-effect-containment-close / ANNOTATED / LOCAL+REMOTE VERIFIED
Close tag target: 86381c6a47a5d68eb7ab9f57892e42e282ac63ea
Post-tag cleanup: COMPLETE / 2 CURRENT PROCESS SOURCES PRUNED / CURRENT RESIDUE 0
Terminal factsources: 12 / 12 / SYNCHRONIZED / 0 CURRENT CONFLICTS
Stage-QDR-9: CLOSED / ACCEPTED / ARCHIVED / TAGGED / IMMUTABLE / NOT REOPENED
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity / production ready: NOT_PROVEN / NO
Next action: DH-POST-FEEDBACK-SIDE-EFFECT-CONTAINMENT-NEXT-STAGE-PLANNING / PLANNING ONLY
Next-stage implementation: NOT AUTHORIZED
ALLOW_FEEDBACK_LEARNING / ALLOW_CASE_PROMOTION / ALLOW_REFERENCE_LIVENESS / ALLOW_RETENTION: NO / NO / NO / NO
ALLOW_CAPACITY / ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
~~~

本节是 post-tag cleanup 后唯一 terminal authority。其后的 pre-tag、local-implementation、Stage-QDR-9 及
更早区块均为历史时间线，不得覆盖本节或授权任何技术实现。下一阶段只能另起 planning-first 任务。


## 当前状态 — feedback side-effect containment local implementation accepted（2026-08-01）

- Stage-QDR-9：`CLOSED / ACCEPTED / ARCHIVED / TAGGED`。
- B1–B4：`CLOSED / ACCEPTED / PUBLISHED`；B5：`FINAL_CLOSE COMPLETE / GOVERNANCE ONLY`。
- Close commit：`88b1d6d8ea68c39eaa74486e5e0bcb6502e00036`；exact-SHA CI：`30640835327 / PASS`。
- Annotated tag：`dh-stage-qdr-9-close`；本地与远端 peeled target 均为 close commit。
- Archive：[Stage-QDR-9 packet](../gates/stage-qdr-9/README.md)；31/31 source copies 与 SHA-256 已验证。
- Current process sources：`31 / 31 PRUNED AS PLANNED`。
- Formal capacity：`NOT_EXECUTED / DEFERRED`；production capacity：`NOT_PROVEN`；production ready：`NO`。
- Reference-liveness、retention、V16/V17/V18、真实 HTTP/Provider/NQ/Agent/LangGraph/Paper/LIVE：未授权。
- Docs baseline：`49fa8442556bcc971119932421e1f606bc349054 / PUBLISHED / CI 30694264770 PASS`。
- Selected stage：`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT / IMPLEMENTED / LOCAL_ACCEPTED`。
- `FEEDBACK_SIDE_EFFECT_CONTAINMENT: IMPLEMENTED / LOCAL_ACCEPTED`；`REMOTE_IMPLEMENTATION_CI: PENDING`；
  `MILESTONE_FINAL_CLOSE: NOT_STARTED`。
- Archived implementation work order：[Feedback Side-Effect Containment Work Order](../gates/platform-hardening-feedback-side-effect-containment/source/DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md)
  已完成 B1–B3：8 handlers 与 compatibility bean append-only，独立 Spring wiring 与 architecture guards 生效。
- Inbound boundary：`INGEST_ONLY / NO_IMPLICIT_LEARNING / NO_MUTABLE_LEARNING_STORE_ACCESS`；四类 inbound
  mutation caller/interactions 均为 0。
- Full regression：`1252 / 0 / 0 / 0`，PostgreSQL 17.10/Testcontainers 实跑；quality 19/19、Checkstyle 0、
  Spotless PASS。
- API、migration、Repository、contracts、POM/workflow、legacy environment 与 structured QDR path 均未改变。
- Implementation：`THIS_IMPLEMENTATION_COMMIT / LOCAL_ONLY / NOT_PUSHED / NO TAG`；remote CI pending。

## Current authority

- [DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_SECURITY_BLOCKER.md](DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_SECURITY_BLOCKER.md)：当前 feedback-ingest atomicity 两条 P1 修复、安全复核与 final-close retry authority。
- [STATUS.md](STATUS.md)：primary current-state authority。
- [WORK_ORDER.md](WORK_ORDER.md)：唯一当前工单与 next action。
- [FACTSOURCE_POLICY.md](FACTSOURCE_POLICY.md)：factsource hierarchy 与 full-sync 规则。
- [ROADMAP.md](ROADMAP.md)：当前路线和 deferred capability 边界。
- [TESTING.md](TESTING.md)：append-only validation evidence。
- [WORKLOG.md](WORKLOG.md)：append-only execution ledger。
- [ARCHIVE_INDEX.md](ARCHIVE_INDEX.md)：历史 archive 与 tag 索引。
- [CODEX_PROJECT_INSTRUCTIONS.md](CODEX_PROJECT_INSTRUCTIONS.md)：执行指导。

## 唯一下一动作

`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-MILESTONE-FINAL-CLOSE`。只有独立授权后才可做
implementation security review、push、exact-SHA CI、archive close 与 tag；不得恢复 V16、retention、
reference-liveness、capacity，也不得接 NQ runtime、Agent、LangGraph、Provider 或 LIVE。

Stage-QDR-9 的原始 plan、work order、batch reviews、designs、errata 与 final-close evidence 已全部移入
[`docs/gates/stage-qdr-9/`](../gates/stage-qdr-9/)，不得再从 `docs/current` 恢复为 active authority。
