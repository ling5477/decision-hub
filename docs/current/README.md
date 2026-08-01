# Decision Hub Current Docs

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
