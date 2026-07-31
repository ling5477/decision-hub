# DH Stage-QDR-9 Implementation Work Order

## Terminal current authority — 2026-07-31 B4 final close

~~~text
Task:
DH-STAGE-QDR-9-B4-AUTHORITY-PUBLICATION-AND-FINAL-CLOSE

Stage-QDR-9 B4:
CLOSED / ACCEPTED / PUBLISHED

B4 active scope:
COMPLETED

Minimal P1/P2 remediation:
CLOSED / ACCEPTED / PUBLISHED

Implementation commit:
6baabe113a3efa447ddd8036bb1b6c086ce1bcff

Implementation CI:
30625943817 / PASS

Minimal remediation authority:
064774df6eabb75678b7a46cd01b524e870d3ca0

Minimal remediation authority CI:
30626830036 / PASS

B4 remaining-scope rebaseline:
9d472b4642f4d17fc4ee3c0cc7a0d66a0b7d7d83

Rebaseline review:
PASS

B4 final-close review:
PASS / AUTHORITY REBASELINE REVIEW ACCEPTED

Active P0 / P1 / publication-blocking P2:
0 / 0 / 0

Rebaseline exact-SHA CI:
30632879649 / PASS

Reference-liveness:
DEFERRED / NOT IMPLEMENTED
FUTURE INDEPENDENT CAPABILITY

Retention:
DEFERRED / NOT IMPLEMENTED
FUTURE INDEPENDENT CAPABILITY

V16:
HISTORICAL CANDIDATE / NOT AUTHORIZED

V17/V18:
SUPERSEDED / NOT ACTIVE

Formal capacity:
DEFERRED / KNOWN LIMITATION

Required for B4 functional close:
NO

Required for production-ready:
YES

Production capacity:
NOT_PROVEN

B5:
NOT AUTHORIZED

Server deployment:
NOT PERFORMED / NOT AUTHORIZED

Next action:
DH-STAGE-QDR-9-POST-B4-NEXT-GATE-DECISION

ALLOW_B5 / ALLOW_V16_IMPLEMENTATION / ALLOW_RETENTION_IMPLEMENTATION / ALLOW_SERVER_DEPLOYMENT:
NO / NO / NO / NO
~~~

B4 active scope 已完成并正式关闭。Reference-liveness、retention 与历史 V16 候选不属于本次
功能关闭条件，也未因 B4 关闭而自动恢复；B5 仍须等待独立 next-gate decision。

## Terminal current authority — 2026-07-31 B4 readiness authority rebaseline

~~~text
Task:
DH-STAGE-QDR-9-B4-READINESS-AUTHORITY-BLOCKER

Authority baseline:
064774df6eabb75678b7a46cd01b524e870d3ca0

Local documentation commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

Minimal P1/P2 remediation:
CLOSED / ACCEPTED / PUBLISHED

Active P0 / P1:
0 / 0

Implementation exact-SHA CI:
30625943817 / PASS

Authority exact-SHA CI:
30626830036 / PASS

Stage-QDR-9 B4:
REMEDIATION CLOSED / FINAL CLOSE REVIEW PENDING

Stage-QDR-9 B4 active scope:
- signed environment authority
- tenant/source/environment authorization
- rate/idempotency environment isolation
- replay namespace isolation
- recovery/fingerprint isolation
- QDR7 rate-audit environment
- root fail-closed
- milestone and publication evidence

B4 active scope result:
IMPLEMENTED / ACCEPTED / PUBLISHED

Reference-liveness:
DEFERRED / NOT IMPLEMENTED

Reference-liveness state model:
DEFERRED / NOT IMPLEMENTED / FUTURE INDEPENDENT CAPABILITY STAGE

Reference-liveness registry:
NOT IMPLEMENTED / NOT A CURRENT B4 CLOSE GATE

Retention eligibility/deletion/audit:
DEFERRED / NOT IMPLEMENTED / FUTURE INDEPENDENT CAPABILITY STAGE

Retention:
DEFERRED / NOT IMPLEMENTED

V16:
HISTORICAL CANDIDATE / NOT AUTHORIZED

Historical V17/V18 sequence:
SUPERSEDED / NOT ACTIVE

Historical 54/60/66 scopes:
HISTORICAL DESIGN EVIDENCE / NOT ACTIVE IMPLEMENTATION GATES

QDR-7 B2 formal capacity:
DEFERRED / KNOWN LIMITATION

Formal capacity:
DEFERRED / KNOWN LIMITATION

Required for B4 functional close:
NO

Required for production-ready declaration:
YES

Production capacity:
NOT_PROVEN

DH deployment / real persistent data:
NONE / NONE

Primary current status:
docs/current/STATUS.md

Current execution order:
docs/current/WORK_ORDER.md

Stage authority:
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md

Canonical remediation evidence:
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md

Historical designs:
reference-liveness / retention / V16 / identity-replay / audit-storage documents

READINESS_AUTHORITY_BLOCKER:
RESOLVED

CURRENT_AUTHORITY_CONSISTENCY:
PASS / 0 CONFLICTS

B4_FINAL_CLOSE_REVIEW_ELIGIBLE:
YES

Next action:
DH-STAGE-QDR-9-B4-FINAL-CLOSE-REVIEW

ALLOW_V16_IMPLEMENTATION / ALLOW_RETENTION_IMPLEMENTATION / ALLOW_B4_CLOSE_NOW / ALLOW_B5:
NO / NO / NO / NO
~~~

Stage-QDR-9 B4 的当前功能范围只包含已发布的 verified-environment remediation 及其 milestone/
publication evidence。Reference-liveness state model、registry 和 retention eligibility/deletion/audit
已延期到未来独立 capability stage，不是当前 B4 close gate，也不授权 V16。

本节之后的旧 implementation order、scope、`current task` 与 `next action` 只保留历史证据属性；
旧 V16/V17/V18 sequence 与 54/60/66 scope 不再是 active implementation gate。

## Terminal current authority — 2026-07-31 B4 minimal P1/P2 publication and authority close

~~~text
Owner operational attestation:
ACCEPTED

Minimal remediation path:
M1

Minimal P1/P2 remediation:
CLOSED / ACCEPTED / PUBLISHED

Discovery commit:
7fb0907acef477c04431445faf40e16db8dff0fb

Implementation commit:
6baabe113a3efa447ddd8036bb1b6c086ce1bcff

Milestone review:
PASS / P0 0 / P1 0 / PUBLICATION-BLOCKING P2 0

Implementation exact-SHA CI:
30625943817 / PASS

Signed environment:
PASS

Tenant/source/environment authorization:
PASS

Rate environment isolation:
PASS

Idempotency environment isolation:
PASS

Replay namespace isolation:
PASS

Recovery/fingerprint isolation:
PASS

QDR7 rate-audit environment:
PASS

Root fail-closed:
PASS

Legacy compatibility:
NOT REQUIRED / OWNER-CONFIRMED NO REAL DATA

Migration:
NOT REQUIRED

Implementation publication:
PASS / ORDINARY FAST-FORWARD / NO HISTORY REWRITE / NO FORCE PUSH

Server deployment:
NOT PERFORMED / NOT AUTHORIZED

Stage-QDR-9 B4 overall:
NOT CLOSED

V16:
NOT AUTHORIZED

B5:
NOT AUTHORIZED

Formal capacity gate:
NOT EXECUTED

Production capacity:
NOT_PROVEN

Next action:
DH-STAGE-QDR-9-B4-POST-REMEDIATION-READINESS-REASSESSMENT
~~~

该 authority 只关闭并发布本工单的 M1 minimal P1/P2 remediation milestone。完整 B4 仍未
关闭；V16、B5、server deployment、legacy compatibility、registry、retention 和正式 capacity
acceptance 均不在授权范围。

## Minimal P1/P2 Implementation — 2026-07-31

~~~text
task:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-IMPLEMENTATION

implementation baseline:
7fb0907acef477c04431445faf40e16db8dff0fb

local implementation commit:
THIS_DOCUMENT_COMMIT / LOCAL_ONLY / NOT PUBLISHED

selected remediation path:
M1

technical P1 / technical P2:
IMPLEMENTED / LOCAL_ACCEPTED
IMPLEMENTED / LOCAL_ACCEPTED

current technical tree:
MINIMAL ENVIRONMENT-BOUND REMEDIATION IMPLEMENTED LOCALLY

legacy compatibility:
NOT REQUIRED / OWNER-CONFIRMED NO REAL DATA

migration:
NOT REQUIRED

scope:
PASS / 20 PRODUCTION / 16 TEST / 0 MIGRATION / 5 AUTHORITY / 0 UNEXPECTED

PostgreSQL/Testcontainers:
PASS / POSTGRESQL 17.10 / 0 MANDATORY SKIPS

full regression:
PASS / 19 OF 19 / 1243 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED

quality:
PASS / 19 OF 19 / CHECKSTYLE 0 / SPOTLESS PASS

remote publication:
PENDING MILESTONE REVIEW

production capacity:
NOT_PROVEN

next action:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-MILESTONE-REVIEW

ALLOW_MINIMAL_P1_P2_MILESTONE_REVIEW:
YES

ALLOW_PUSH / ALLOW_SERVER_DEPLOYMENT / ALLOW_V16_IMPLEMENTATION / ALLOW_B4_PUBLICATION / ALLOW_B5:
NO / NO / NO / NO / NO
~~~

该 implementation 只消费 owner-attested M1 exact allowlists；未恢复本文件中的历史
V16/V17/V18、registry、retention 或 legacy transition 设计。

## Owner-Attested M1 Rebaseline and Minimal Implementation Discovery — 2026-07-30

~~~text
task:
DH-STAGE-QDR-9-B4-OWNER-ATTESTED-M1-REBASELINE-AND-MINIMAL-IMPLEMENTATION-DISCOVERY

owner operational attestation:
ACCEPTED / FIRST_PARTY_OPERATIONAL_ATTESTATION

actual deployment / traffic / operational database / real persistent data:
ABSENT / ABSENT / ABSENT / ABSENT

legacy retention / zero downtime / historical backfill:
NOT REQUIRED / NOT REQUIRED / NOT REQUIRED

selected remediation path:
M1

historical repository-only evidence audit:
M4 / INSUFFICIENT WITHOUT OWNER OR RUNTIME EVIDENCE

historical M4 superseded by:
OWNER OPERATIONAL ATTESTATION

complete discovery:
PASS

minimal implementation scope:
FROZEN / EXACT FILES

migration required:
NO

V16 / V17 / V18:
NOT CREATED / NOT CREATED / NOT CREATED

reference-liveness registry / retention:
NOT REQUIRED / NOT REQUIRED

server deployment:
FORBIDDEN / NQ 168-HOUR ACCEPTANCE SERVER EXCLUDED

minimal implementation:
AUTHORIZED FOR NEXT TASK ONLY / EXACT ALLOWLIST ONLY

review retry / publication / B5:
NOT ALLOWED / NOT ALLOWED / NOT ALLOWED

next action:
DH-STAGE-QDR-9-B4-MINIMAL-P1-P2-IMPLEMENTATION
~~~

### Implementation objective

只实现 verified `FeedbackEnvironment` / `FeedbackExecutionScope`、signed HMAC/bearer environment、
tenant/source/environment authorization、environment-bound rate/idempotency/recovery/fingerprint/
replay，以及既有 QDR7 rate audit JSONB 中的 structured environment。

### Migration decision

~~~text
Can environment isolation be implemented using existing columns/keys?
YES

Is a single forward migration required for new structured audit environment?
NO

Can DEV/TEST databases be rebuilt instead of legacy backfill?
YES

Is any legacy retirement migration required?
NO
~~~

V12 已为 rate/idempotency identity 提供 environment key；V4 replay table 接受 opaque
environment-bound key；V5 audit `event_json` 已是 structured JSONB。因此 minimal migration
allowlist 为空，不创建 V16/V17/V18。

### Frozen exact allowlists

`MINIMAL_PRODUCTION_WRITE_ALLOWLIST`、`MINIMAL_TEST_WRITE_ALLOWLIST`、
`MINIMAL_MIGRATION_ALLOWLIST` 与 `MINIMAL_CURRENT_AUTHORITY_ALLOWLIST` 的唯一完整版本位于：

- `docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md`
- `docs/current/DH_STAGE_QDR_9_B4_MINIMAL_P1_P2_IMPLEMENTATION_DISCOVERY.md`

Implementation 开始后如发现 allowlist 外必需文件：

~~~text
STOP IMPLEMENTATION
RETURN TO COMPLETE DISCOVERY
~~~

不得新增 scope erratum，不得继续累计 42/46/47/48/54/60/66 scope count。

### Explicit exclusions

- legacy tombstone retirement、dual-read/write、legacy blocking、historical backfill；
- V16/V17/V18、reference-liveness registry、retention、scheduler/cleanup subsystem；
- real NQ、real Provider、real HTTP、Agent/LangGraph、Paper、LIVE；
- DH server deployment、NQ acceptance server 使用；
- B4 review publication、B5、push 或 tag。

## Engineering Discipline Reset and Minimal Remediation Rebaseline — 2026-07-30

~~~text
task:
DH-STAGE-QDR-9-B4-ENGINEERING-DISCIPLINE-RESET-AND-MINIMAL-REMEDIATION-PLAN

primary delivery goal:
P1 verified-environment persistent rate/idempotency identity and structured rate audit
P2 verified-environment replay namespace

actual legacy data:
NOT_PROVABLE

rate / idempotency / replay / audit data class:
D / D / D / D

selected remediation path:
M4

selected migration reassessment:
R4 / INSUFFICIENT OPERATIONAL EVIDENCE

reference-liveness registry required for P1/P2:
NO

V16 required before audit environment storage:
NO

V18 required for confirmed current data:
NO / NO CONFIRMED CURRENT DATA

previous V16 -> V17 -> V18 sequence:
HISTORICAL DESIGN EVIDENCE
SUPERSEDED FOR MINIMAL REMEDIATION EXECUTION

previous 54 / 60 / 66 scopes:
HISTORICAL DESIGN EVIDENCE
NOT ACTIVE IMPLEMENTATION GATE

canonical plan:
docs/current/DH_STAGE_QDR_9_B4_ENGINEERING_DISCIPLINE_RESET_AND_MINIMAL_REMEDIATION_PLAN.md

minimal implementation:
NOT AUTHORIZED / EVIDENCE BLOCKER FIRST

V16 / V17 / V18:
NOT CREATED / NOT CREATED / NOT CREATED

registry / retention:
NOT IMPLEMENTED / NOT PRESENT

review retry / publication / B5:
NOT ALLOWED / NOT ALLOWED / NOT ALLOWED

next action:
DH-STAGE-QDR-9-B4-DEPLOYMENT-AND-LEGACY-DATA-EVIDENCE-BLOCKER
~~~

该 rebaseline 只重置当前 delivery path，不删除或改写历史设计。GitHub Actions success、Flyway
schema、Testcontainers fixture、生产类可写路径和 commit publication 均不是真实 deployment/data
证据。当前没有获授权的 environment inventory、running SHA、真实流量、row inventory 或
zero-downtime requirement，因此不得选择 M1/M2/M3，不得继续 lifetime blocker、V18、registry 或
retention 设计。

M4 evidence blocker 解除后，必须先完成一次完整 discovery，并一次性冻结
`MINIMAL_IMPLEMENTATION_WRITE_ALLOWLIST`、`MINIMAL_TEST_ALLOWLIST`、`MIGRATION_ALLOWLIST`
与 `FACTSOURCE_UPDATE_ALLOWLIST`。implementation 中发现 allowlist 外文件时必须停止并返回
discovery，不得新增 scope erratum。

未来 M1/M2 最小边界只允许 verified `FeedbackEnvironment` / `FeedbackExecutionScope`、
signed HMAC/AuthContext environment、tenant/source/environment authorization、environment-bound
rate/idempotency/replay、structured QDR7 rate-audit environment、DEV/TEST/root fail-closed、
PostgreSQL/Testcontainers 和必要 architecture guards。registry、retention、cleanup、scheduler、
historical backfill、V18、generic audit query API、automatic learning 与 B5 明确排除。

## Upstream Environment Contract Remote Containment — 2026-07-30

~~~text
containment decision:
OPTION 1A / ORDINARY REVERT OF IMPLEMENTATION ONLY

design evolution:
OPTION B FROZEN
SCOPE EXPANDED TO 48 / 48

published implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0

published exact-SHA CI:
30283326199 / PASS
REGRESSION PASS ONLY
DOES NOT CLOSE SECURITY REVIEW

publication authorization:
NOT PROVABLE

pre-publication milestone review:
NOT COMPLETED

retrospective review:
BLOCKED

technical P1:
1 OPEN / persistent identity and QDR7 rate-audit environment integrity

technical P2:
1 OPEN / replay key excludes environment

governance P1:
PUBLISHED BEFORE MILESTONE REVIEW

ordinary revert:
df921f275c61d67cebbb95c0924391866a6d09dc

design/scope commits:
PRESERVED

history rewrite:
NONE

current technical tree:
B3 SAFE BASELINE

current upstream implementation:
NOT PRESENT

next action after containment publication and exact-SHA CI:
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-SCOPE-DESIGN
~~~

本工单透明保留完整时序：设计链完成，scope 发展到 `48 / 48`，implementation 创建并远端发布，exact-SHA CI 通过；随后追溯 milestone/security review 识别 technical P1/P2 与发布时序 governance P1；实现通过 ordinary revert 撤销，设计/scope 链继续保留。不得描述为 implementation 从未发布、技术审查通过或 P1/P2 已修复。

当前禁止 P1/P2 implementation、V16、reference-liveness registry、retention、B4 milestone review retry、B4 publication、B5、API、scheduler 与 automatic learning。QDR-7 B2 capacity 继续 `DEFERRED / KNOWN LIMITATION`，production capacity 继续 `NOT_PROVEN`。

## Upstream Persistent Identity and Replay Namespace Blocker — 2026-07-30

~~~text
task:
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-SCOPE-DESIGN

baseline:
b0ff11e4057077ad7e0fe91d691116f069dc744e

published implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0 / PUBLISHED AND REVERTED

current technical tree:
B3 SAFE BASELINE

technical P1 / P2:
1 OPEN / 1 OPEN

persistent identity environment isolation:
DESIGN FROZEN / NOT IMPLEMENTED

QDR7 rate audit environment:
DESIGN FROZEN / NOT IMPLEMENTED

replay namespace environment isolation:
DESIGN FROZEN / NOT IMPLEMENTED

legacy persistent strategy:
OPTION B / CONTRACT BLOCKED

legacy replay strategy:
OPTION B / CONTRACT BLOCKED

audit environment storage:
FORWARD MIGRATION REQUIRED

effective scope:
54 / 54 PASS

V16 / registry / retention:
CANDIDATE-NOT CREATED / NOT IMPLEMENTED / NOT PRESENT

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

本 scope-design 冻结 `QDR9-RATE-IDENTITY-2`、`QDR9-IDEMPOTENCY-IDENTITY-2`、
`QDR9-RECOVERY-IDENTITY-2` 与 `QDR9-DRYRUN-REPLAY-2`，统一使用 `QDR9-LP1`
UTF-8 length-prefixed canonical encoding，但保持 rate、idempotency、recovery、replay 独立 namespace。
verified environment 唯一来源是 `command.executionScope().environment()`。

现有 audit storage 无结构化 environment column，禁止复用 `event_json` 或 `event_status`，因此需要
forward migration。现有 idempotency `EXPIRED` tombstone 无物理删除，legacy replay TTL 无 hard ceiling
且当前 port 无 atomic legacy-check + v2 admission；两类 legacy 选择安全保守 Option B，但 implementation
继续 blocked。

下一步仅为：

~~~text
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN
~~~

该任务必须先冻结 migration sequencing，不得占用 candidate V16，不得实施 P1/P2、registry、retention、
B4 review retry/publication 或 B5。

## Audit Environment Structured Storage Blocker — 2026-07-30

~~~text
task:
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN

baseline:
9249bf78a2eaace4c59aedff788e37e083b72b3b

actual audit table:
dh_decision_audit_event

selected audit storage:
OPTION A / NULLABLE STRUCTURED COLUMN + STAGED NEW-WRITE ENFORCEMENT

selected historical backfill:
B1 / NULL UNKNOWN LEGACY

selected migration sequence:
S2 / V16 REFERENCE-LIVENESS REGISTRY THEN V17 AUDIT ENVIRONMENT

candidate audit migration:
V17__qdr9_audit_environment_storage.sql / NOT CREATED

compatibility constructor:
REMOVE / NO LEGACY ENVIRONMENTLESS PRODUCTION RECORD

rate audit environment source:
verified FeedbackExecutionScope.environment()

effective scope:
60 / 60 PASS

technical P1 / P2:
1 OPEN / 1 OPEN

ALLOW_AUDIT_ENVIRONMENT_MIGRATION_IMPLEMENTATION:
NO / BLOCKED BY V16 SEQUENCE AND PRODUCER CUTOVER
~~~

`event_status` 继续只表示 `SUCCESS`/`FAILED`，`event_json` 和日志不得存储或推断 environment。V17
保留历史 row 为 `NULL / UNKNOWN LEGACY`，不默认 DEV/TEST；新受信 row 必须显式写 `DEV`/`TEST`。
QDR7 rate identity 与 rate audit 使用同一 verified `FeedbackExecutionScope`，并保持 admission/audit
同一 required transaction。

V16 仍由 `qdr_reference_liveness` registry candidate 独占且未创建；V17 不能越过 V16 实施。
本设计未修改 Java、测试、POM 或 migration，未实现 identity/replay/upstream environment、registry、
retention、B4 review retry/publication 或 B5。

下一任务：

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-BLOCKER
~~~

## Legacy Persistent Identity Retirement Blocker — 2026-07-30

~~~text
task:
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-BLOCKER

baseline:
aab84e896595bbd8b3f5e99e8b2880ca28f8e7a4

legacy Option B previous state:
CONSERVATIVE BLOCKING / NO TERMINATION CONTRACT

selected normal terminal option:
L2 / BOUNDED CONSERVATIVE BLOCKING + PHYSICAL RETIREMENT

selected tombstone retirement:
T1 / TRANSACTIONAL PHYSICAL DELETE

unknown/unprovable fallback:
BLOCK

EXPIRED tombstone:
UNBOUNDED / UNIQUE AND LOOKUP BLOCKING / NO CURRENT DELETE PATH

rate cutoff:
UNRESOLVED / MAXIMUM IN-FLIGHT GUARD TRANSACTION LIFETIME NOT AVAILABLE

idempotency cutoff:
UNRESOLVED / COMMIT-UNKNOWN RECONCILIATION HARD MAXIMUM NOT AVAILABLE

recovery cutoff:
UNRESOLVED / INDEPENDENT RESULT AVAILABILITY HARD MAXIMUM NOT AVAILABLE

cleanup contract:
FROZEN / INTERNAL-ONLY / DEFAULT-DISABLED / TENANT+FAMILY SCOPED / BOUNDED

retirement audit:
FROZEN / INDEPENDENT STRUCTURED STORAGE / NOT IMPLEMENTED

legacy identity migration:
REQUIRED

migration ordering:
V16 REGISTRY -> V17 AUDIT ENVIRONMENT -> V18 LEGACY IDENTITY CANDIDATE

V18 artifact:
NOT CREATED / EXACT PATH NOT AUTHORIZED

effective scope:
66 / 66 PASS

technical P1 / P2:
1 OPEN / 1 OPEN

ALLOW_LEGACY_IDENTITY_IMPLEMENTATION:
NO
~~~

当前 `dh_qdr7_rate_limit_bucket` 已有 bounded physical cleanup；`dh_qdr7_idempotency_guard` cleanup
只转换为 `EXPIRED` 并清空 result/error/lease，row 继续参与 unique/exact lookup，因此 tombstone
lifetime 无上限。Recovery 没有独立 legacy table，复用同一 guard row 与 tenant-bound
`dh_decision_output` reference。

L2 是唯一允许的正常迁移终态，L1 只保留为 UNKNOWN fallback；L3 会造成 duplicate execution 与
rate bypass；L4 缺少可信历史 environment。T1 必须在同一 required transaction 内完成 candidate
lock、cutoff/state/lease/result/commit-outcome final recheck、independent structured audit 与 exact
DELETE，任何一步失败整批 rollback。

本 task 只完成 documentation/security design，未修改 Java、tests、POM 或 migration；未创建
V16/V17/V18；未实施 persistent identity/replay、registry、retention、review retry/publication 或 B5。

下一任务：

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-LIFETIME-BLOCKER
~~~

## Terminal current authority — 2026-07-27 Stage-QDR-9 B4 upstream Maven dependency scope retry

~~~text
starting HEAD / parent: 1971f3dc29fb690dcbd64cb7b0c62c797d81cbb1 / 75c2449972c4b6f15689144478f31c0b7edf8126
origin/dev: 7624bccba9b865d4b687057f41b96799cb9ba8e3 / FRESHLY VERIFIED
upstream contract implementation: BLOCKED BEFORE CODE COMMIT
current blocker: MAVEN MODULE DEPENDENCY OUTSIDE FROZEN SCOPE
canonical environment type / owner: FeedbackEnvironment / dh-domain
authorized dependency: dh-security -> dh-domain / APPROVED
module dependency scope: dh-security/pom.xml ONLY
effective upstream scope: 47 / 47 PASS
V16 / registry / retention: CANDIDATE-NOT CREATED / NOT STARTED / NOT PRESENT
next action: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-IMPLEMENTATION-RETRY-2
ALLOW_UPSTREAM_CONTRACT_IMPLEMENTATION_RETRY: YES / NEXT TASK ONLY
ALLOW_V16_IMPLEMENTATION / B4 review retry / B4 publication / B5: NO / NO / NO / NO
~~~

## Terminal current authority — 2026-07-26 Stage-QDR-9 B4 producer environment contract scope design

~~~text
Implementation baseline / HEAD / origin/dev / advertised SHA: 7624bccba9b865d4b687057f41b96799cb9ba8e3
Branch / ahead / behind / worktree / staged: dev / 0 / 0 / clean / empty
Containment: CLOSED / ACCEPTED / PUBLISHED
Containment exact-SHA CI: 30203970694 / PASS
Current technical tree: B3 SAFE BASELINE
Stage-QDR-9 B1 / B2 / B3: CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B4: IMPLEMENTATION REVERTED / REVIEW BLOCKED
Reference-liveness design: FROZEN / PUBLISHED
Selected architecture: OPTION B / qdr_reference_liveness registry
V16 implementation: BLOCKED BEFORE CODE WRITE
V16 blocker: PRODUCER ENVIRONMENT CONTRACT MISSING / SOURCES UNRESOLVED
AUDIT / REPLAY / EVALUATION environment contract: FROZEN / LOCAL_ACCEPTED
AUDIT / REPLAY / EVALUATION environment source: UNRESOLVED / FAIL_CLOSED
Producer environment scope: 31 / 31 PASS
V16: CANDIDATE / NOT CREATED
Retention: NOT PRESENT IN CURRENT TREE
B4 milestone review retry / B4 publication / B5: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
API / scheduler / automatic learning: NOT_ALLOWED / NOT_IMPLEMENTED / NOT_ALLOWED
Production capacity: NOT_PROVEN
Terminal current factsources: 12 / 12
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 B4 PRODUCER ENVIRONMENT AUTHORITY HASH / 0 CONFLICTS
current task: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-CONTRACT-SCOPE-DESIGN
current task status: LOCAL_ACCEPTED / SOURCE_BLOCKER_FROZEN
next action: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER
ALLOW_V16_IMPLEMENTATION_RETRY: NO
~~~

## B4 Producer Environment Source Resolution — current blocker

`DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-SOURCE-BLOCKER` 已完成 production root caller、传播链、事务 owner 与 exact scope 审计：

```text
initial producer-environment design: COMPLETED / 31 OF 31
initial source state: UNRESOLVED
V16 attempt: BLOCKED BEFORE CODE WRITE
selected root contract: FeedbackExecutionScope
selected root contract path:
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/FeedbackExecutionScope.java
root strategy: SEPARATE ROOTS / ONE SHARED STRICT TYPE
expanded exact scope: 36 / 36 PASS
V16: CANDIDATE / NOT_CREATED
```

代码现实同时证明：

- `DecisionDryRunController` 与 `HumanApprovalPacketController` 没有 caller-supplied environment；
- auth context 仅有 user/tenant/roles；
- guard environment 来自 deployment configuration，且支持 staging/prod，禁止转换；
- `QdrRegressionEvaluationService` 没有 production caller 或 Spring wiring。

因此本工单不能转入 V16 retry：

```text
PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION: BLOCKED
AUDIT_ENVIRONMENT_SOURCE: UNRESOLVED
REPLAY_ENVIRONMENT_SOURCE: UNRESOLVED
EVALUATION_ENVIRONMENT_SOURCE: UNRESOLVED
ALLOW_V16_IMPLEMENTATION_RETRY: NO
ALLOW_B4_MILESTONE_REVIEW_RETRY / PUBLICATION / B5: NO / NO / NO
NEXT_ACTION: DH-STAGE-QDR-9-B4-PRODUCER-ENVIRONMENT-UPSTREAM-CONTRACT-BLOCKER
```

权威设计与 future blocked work order：

```text
docs/current/DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_RESOLUTION.md
docs/current/DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SOURCE_IMPLEMENTATION_WORK_ORDER.md
```

## Historical terminal authority — 2026-07-26 Stage-QDR-9 B4 blocked remote commit containment

```text
Implementation baseline: 42697edcba9719a395aecee46830faba2c838948
Branch: dev
Pre-containment origin/dev / blocked B4 implementation: fa9debb4474eedc4353e1236e227eae8ef23c085
Stage-QDR-9 B1 / B2 / B3: CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B4: IMPLEMENTATION REVERTED / MILESTONE REVIEW BLOCKED
Blocked remote publication: CONFIRMED / CONTAINED BY ORDINARY REVERT
Blocked remote scope-prewrite / implementation: 1036171ff7acd85078416b907f0444ab6214b3ca / fa9debb4474eedc4353e1236e227eae8ef23c085
P1 findings: 2 / OPEN
Current B4 retention code / tests / wiring: NOT PRESENT IN CURRENT TREE
Reference-liveness state-model design: FROZEN / PUBLISHED WITH CONTAINMENT
State-model design commit: 4ef3991f8379b6653f06a22bcdf5f4b33fcb7585
Selected architecture: OPTION B / qdr_reference_liveness registry
V16: CANDIDATE / NOT CREATED
Fresh full regression: PASS / 19 OF 19 REACTOR / 1228 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Containment exact-SHA CI: REQUIRED / PENDING PUBLICATION
next action after containment publication and CI: DH-STAGE-QDR-9-B4-REFERENCE-LIVENESS-FORWARD-MIGRATION-IMPLEMENTATION
```

## Historical terminal authority — 2026-07-26 Stage-QDR-9 B3 local acceptance

```text
Implementation baseline: b61f164ea078bf9455fc682adf55ecd12283bbb4
Repository: E:/Project/decision-hub
Branch: dev
Origin baseline: b61f164ea078bf9455fc682adf55ecd12283bbb4
Current highest migration: V15 / PUBLISHED / EXACT_SHA_CI_ACCEPTED
Terminal current factsources: 12
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-9 plan: DONE / PUBLISHED
Stage-QDR-9 implementation work order: FROZEN / ACCEPTED
Stage-QDR-9 B1: CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B2: CLOSED / ACCEPTED / PUBLISHED
Stage-QDR-9 B3: IMPLEMENTED / LOCAL_ACCEPTED
Historical evidence read model: IMPLEMENTED / INTERNAL ONLY
Pagination: KEYSET / observed_at DESC + attribution_id DESC
Page size: DEFAULT 50 / HARD MAX 100
Maximum time range: 90 DAYS
Cursor scope binding / tenant-environment isolation: PASS / PASS
Retention: NOT_STARTED
API / Automatic learning: NOT_ALLOWED / NOT_ALLOWED
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE + HISTORICAL_EVIDENCE_READ_MODEL
Scope invariants: PASS / 17 OF 17
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
```

本文件是 Stage-QDR-9 B1–B4 implementation 的唯一精确 scope contract。它只冻结后续实施文件、schema、事务、幂等、内部查询、retention、安全和测试要求；本工单任务没有创建 migration、Repository、API、Controller、运行时连接或 automatic learning。

## 1. 任务分类、目标与非目标

```text
Task classification:
PLAN_BASELINE_LOCAL_COMMIT
+ WORK_ORDER_ONLY
+ PERSISTENCE_SCHEMA_FREEZE
+ REPOSITORY_TRANSACTION_BOUNDARY_DESIGN
+ HISTORICAL_READ_MODEL_DESIGN
+ RETENTION_SAFETY_DESIGN
+ TEST_MATRIX_FREEZE
+ LOCAL_WORK_ORDER_COMMIT
+ NO_CODE_CHANGE
+ NO_TEST_CHANGE
+ NO_MIGRATION_CHANGE
+ NO_API_CHANGE
+ NO_PUSH
+ NO_TAG
+ NO_AUTOMATIC_LEARNING
+ NO_REAL_HTTP
+ NO_PROVIDER
+ NO_NQ
+ NO_AGENT
+ NO_LANGGRAPH
+ NO_PAPER
+ NO_LIVE
```

### 1.1 目标

1. 将 Stage-QDR-8 deterministic attribution 结果冻结为 tenant/environment-bound 的 immutable persistence aggregate。
2. 冻结 PostgreSQL 数据库幂等、单事务写入、duplicate-key race 和 commit-unknown fail-closed 语义。
3. 冻结仅供内部 use case 使用的 historical evidence read model、keyset cursor 和查询上限。
4. 冻结 reference guard、integrity、bounded retention cleanup、删除审计和多实例竞争控制。
5. 将 B1–B4 精确限制到本工单列出的文件；B5 只冻结验收内容，不授权 archive/tag/pruning 写入。

### 1.2 非目标

```text
不新增 Controller、REST endpoint、OpenAPI 或 external API
不修改 dh_nq_feedback_events，不复用其 raw payload / NQ / Paper 语义
不保存 raw prompt、raw provider response、credential 或完整外部 payload
不新增 automatic learning、online learning 或 decision rerun
不修改 Experience、Pheromone、Prompt、模型、策略、候选或 JudgeDecision
不连接真实 HTTP、Provider、NQ、Agent、LangGraph、Paper 或 LIVE
不重开 Stage-QDR-7 B2 capacity gate
不声明 production capacity
不修改 Stage-QDR-7/8 archive 或 tag
本工单不创建 V15，不创建 Java 文件，不执行 implementation
```

## 2. 代码现实冻结

| 检查项 | 仓库现实 | 工单决策 |
|---|---|---|
| feedback contracts | `domain.qdr.feedback` 与 `usecase.qdr.feedback` 已有 deterministic、tenant/environment-bound contracts。 | 原合同为输入；B1 不重写 Stage-QDR-8 计算语义。 |
| current idempotency | production 没有 attribution 幂等 adapter；测试 fake 使用有界 `ConcurrentHashMap`。 | B2 使用 PostgreSQL unique constraint；禁止 JVM lock/static map。 |
| current in-memory state | attribution production 无状态；测试 fake capacity 显式有界。 | 不新增 production cache 或无界 `Map`。 |
| Repository ports | 没有 QDR feedback aggregate persistence 或 historical read-model port。 | B1 新增专用 ports，不扩大到通用 Repository。 |
| JDBC patterns | 已有 tenant-bound `Jdbc*Repository`、`DuplicateKeyException`、`DataAccessException` 转换。 | 复用编码模式，不复用旧 feedback 表。 |
| transaction patterns | `ReplayInputSnapshotAssemblyService` 使用强制 `PlatformTransactionManager`、`TransactionTemplate`、`REPEATABLE_READ`、无 fallback。 | B2 使用同等显式事务；缺 manager fail-fast。 |
| migration sequence | versioned migration 最高为 `V14`。 | 冻结 `V15__qdr9_structured_feedback_persistence.sql`；开始 B1 前再次检查，漂移即阻断。 |
| pagination | 现有 replay/read model 主要为 bounded offset；没有 keyset/cursor codec。 | B3 新建专用 keyset，不复用 offset API。 |
| retention | `JdbcGuardCleanupAdapter` 已有 tenant scope、bounded batch、`FOR UPDATE SKIP LOCKED`。 | B4 复用安全模式，新增 reference hold 与删除审计。 |
| duplicate concept | `dh_nq_feedback_events` / `JdbcNqFeedbackEventRepository` 无 environment，含 raw `payload_json`，部分 eventId 查询不 tenant-bound。 | 仅作为冲突证据；禁止迁移、关联或复用。 |

## 3. Exact scope contract

### 3.1 READ_SCOPE

当前事实与规划：

```text
README.md
AGENTS.md
CLAUDE.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/current/DH_STAGE_QDR_9_PLAN.md
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
```

现有代码证据：

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/readmodel/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/ReplayInputSnapshotAssemblyService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcDecisionReadModelQueryAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcCanonicalReplaySnapshotRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcIdempotencyGuardAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcGuardCleanupAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
dh-app/src/main/resources/db/migration/**
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
pom.xml
dh-domain/pom.xml
dh-usecase/pom.xml
dh-infra/pom.xml
dh-app/pom.xml
.github/workflows/ci.yml
```

READ_SCOPE 同时包含下列 WRITE_ALLOWLIST 全部 exact files。

### 3.2 WRITE_ALLOWLIST

Domain / use-case contracts：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceException.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceValidationPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceTransactionBoundary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidencePage.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQueryPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackIntegrityReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackIntegrityPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionService.java
```

JDBC adapters：

```text
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackPersistenceTransactionBoundary.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodec.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackIntegrityAdapter.java
```

Migration、wiring 与 stable machine contract：

```text
dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
config/qdr9-feedback/qdr9-feedback-contract.yml
```

Tests：

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecordsTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodecTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackIntegrityAdapterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

Current docs：

```text
README.md
AGENTS.md
CLAUDE.md
docs/current/DH_STAGE_QDR_9_PLAN.md
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
```

### 3.3 VALIDATION_SCOPE

```text
WRITE_ALLOWLIST 全部 exact files
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
12 terminal current factsources
19-module Maven reactor
```

### 3.4 FIXABLE_BLOCKER_SCOPE

```text
WRITE_ALLOWLIST
```

验证发现 WRITE_ALLOWLIST 外的 blocker 时必须停止，不能顺手修复。

### 3.5 CURRENT_FACTSOURCE_SCAN_SCOPE

```text
README.md
AGENTS.md
CLAUDE.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
```

### 3.6 MIGRATION_SCOPE

```text
dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceFlywayPostgresTest.java
```

### 3.7 REPOSITORY_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceException.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceValidationPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceTransactionBoundary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackPersistenceTransactionBoundary.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecordsTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapterTest.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
```

### 3.8 READ_MODEL_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidencePage.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQueryPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodec.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapter.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodecTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapterTest.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
Controller / OpenAPI / dh-api: EMPTY / EXCLUDED
```

### 3.9 RETENTION_SCOPE

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackIntegrityReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackIntegrityPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapter.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackIntegrityAdapter.java
config/qdr9-feedback/qdr9-feedback-contract.yml
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackRetentionServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackRetentionAdapterTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackIntegrityAdapterTest.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

### 3.10 ARCHITECTURE_GUARD_SCOPE

```text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

### 3.11 Original scope invariants — ORIGINAL / SUPERSEDED FOR B1 FINAL ACCEPTANCE

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
REPOSITORY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
READ_MODEL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
RETENTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
ARCHITECTURE_GUARD_SCOPE ⊆ VALIDATION_SCOPE: PASS

ORIGINAL TOTAL: 8 OF 8 PASS
```

每个 batch 开始前必须将本工单总 allowlist 缩小为该 batch 的 exact subset，不得扩大。任一 invariant 失败时输出 `TASK_SCOPE_DESIGN_INVALID`。

### 3.12 B1 Scope Erratum — Post-implementation governance repair

本节为 `80b21f31bdb5dc7a15f327aa9617b0640fd1f22b` 完成后的透明治理修复。它不改写上方原始冻结的 `8 / 8 PASS`，该原始记录继续保留并且仅对 B1 最终验收标记为 `ORIGINAL / SUPERSEDED FOR B1 FINAL ACCEPTANCE`。

首次 B1 milestone review 发现：原始 published `WRITE_ALLOWLIST` 未列出两个实际已在 B1 implementation commit 中修改的 legacy Flyway compatibility tests。因此，原始 `8 / 8 PASS` 不能单独覆盖实际提交范围，首次 review 的结论固定为：

```text
INITIAL MILESTONE REVIEW: BLOCKED
BLOCKER: STAGE_QDR_9_B1_REVIEW_SCOPE_VIOLATION
TASK_SCOPE_DESIGN_INVALID
```

原始 B1 技术 implementation subset 保持为 13 个文件：

```text
config/qdr9-feedback/qdr9-feedback-contract.yml
dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V15Qdr9FeedbackPersistenceMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceException.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceTransactionBoundary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackReferenceValidationPort.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceRecordsTest.java
```

批准的精确扩展仅为下列两个路径；它们同时加入 B1 effective `READ_SCOPE`、effective `WRITE_ALLOWLIST`、`VALIDATION_SCOPE` 与新 `LEGACY_MIGRATION_TEST_FIX_SCOPE`：

```text
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V13TransactionalCompatibilityCallbackFlywayPostgresTest.java
```

两处已提交修改的唯一语义是：当 historical target 参数为 `null` 时，显式使用 migration target `14`。禁止以下扩展：

```text
弱化或删除断言
将 expected version 改为 15
production callback 修改
V1–V15 migration 修改
任何其他 legacy test 修改
```

#### Effective B1 `WRITE_ALLOWLIST`

effective B1 技术 `WRITE_ALLOWLIST` 是上列原始 13 文件加上两个 exact legacy test 文件，共 15 个技术文件。原 12 个 terminal current factsources 仍属于 B1 current-fact alignment write scope；本 erratum 与 milestone review 文档是 post-implementation governance evidence，不追溯加入原始 implementation commit。

#### Effective scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
REPOSITORY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
READ_MODEL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
RETENTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
ARCHITECTURE_GUARD_SCOPE ⊆ VALIDATION_SCOPE: PASS
LEGACY_MIGRATION_TEST_FIX_SCOPE ⊆ WRITE_ALLOWLIST: PASS

EFFECTIVE TOTAL: 9 OF 9 PASS
```

`LEGACY_MIGRATION_TEST_FIX_SCOPE` 仅包含上述两份 V12/V13 test；不得据此扩大到其他历史 migration、callback、schema 或 implementation 路径。若该 effective contract 之外出现 blocker，必须输出 `STAGE_QDR_9_B1_TECHNICAL_BLOCKER_REQUIRES_NEW_TASK`，不得在本治理修复任务中修改技术文件。

### 3.13 B2 Effective Scope Serialization — post-implementation governance repair

本节是对已经发布的 B2 技术提交 `5363c1930684c7c1baf0ea8a72f36d5ede870b4e` 的范围序列化。它不修改该提交，也不改写上方原始 `8 / 8 PASS` 或 B1 effective `9 / 9 PASS` 历史；四个 B2 exact scope sets 仅补齐 B2 实际技术文件的可独立重建记录。

`B2_JDBC_IMPLEMENTATION_SCOPE`：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackPersistenceErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackPersistenceTransactionBoundary.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapter.java
```

`B2_TRANSACTION_TEST_SCOPE`：

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/FeedbackAttributionPersistenceServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepositoryTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackReferenceValidationAdapterTest.java
```

`B2_CONCURRENCY_TEST_SCOPE`：

```text
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcFeedbackAttributionRepositoryTest.java
```

`B2_WIRING_SCOPE`：

```text
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
```

四个集合都是冻结 `WRITE_ALLOWLIST` 的 exact subsets；B2 technical subset 与 12 个 terminal current factsources 合并后覆盖 B2 implementation commit 的全部 23 个变更文件。它们不授权 V1–V15 migration、historical read model、retention/delete、Controller/API/OpenAPI、POM/workflow、QDR-7/QDR-8 archive、真实 HTTP/Provider/NQ、Agent/LangGraph 或 automatic learning。

#### Effective B2 scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
REPOSITORY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
READ_MODEL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
RETENTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
ARCHITECTURE_GUARD_SCOPE ⊆ VALIDATION_SCOPE: PASS
LEGACY_MIGRATION_TEST_FIX_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_JDBC_IMPLEMENTATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_TRANSACTION_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_CONCURRENCY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_WIRING_SCOPE ⊆ WRITE_ALLOWLIST: PASS

EFFECTIVE B2 TOTAL: 13 OF 13 PASS
```

B2 的 publication、exact-SHA CI 与 milestone review 记录在 current authority close 中完成；本节不授权 B3 implementation。

### 3.14 B3 Exact Scope Serialization — implementation prewrite

`B3_READ_MODEL_IMPLEMENTATION_SCOPE`：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidencePage.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQueryPort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadService.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapter.java
```

`B3_KEYSET_CURSOR_SCOPE`：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidencePage.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodec.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapter.java
```

`B3_QUERY_TEST_SCOPE`：

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/HistoricalFeedbackEvidenceReadServiceTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/FeedbackEvidenceCursorCodecTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/JdbcHistoricalFeedbackEvidenceQueryAdapterTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

`B3_WIRING_SCOPE`：

```text
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
```

上述四个集合均是已冻结 `WRITE_ALLOWLIST` 的 exact subset。B3 implementation 只允许修改此处列出的 14 个技术文件与 12 个 terminal current factsources；不授权 V1–V15 migration、B2 aggregate write/transaction/idempotency、retention/delete、Controller/API/OpenAPI、POM/workflow、archive/config、external HTTP/Provider/NQ、Agent/LangGraph、automatic learning 或交易副作用。

#### Effective B3 scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
REPOSITORY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
READ_MODEL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
RETENTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
ARCHITECTURE_GUARD_SCOPE ⊆ VALIDATION_SCOPE: PASS
LEGACY_MIGRATION_TEST_FIX_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_JDBC_IMPLEMENTATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_TRANSACTION_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_CONCURRENCY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B2_WIRING_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B3_READ_MODEL_IMPLEMENTATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B3_KEYSET_CURSOR_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B3_QUERY_TEST_SCOPE ⊆ WRITE_ALLOWLIST: PASS
B3_WIRING_SCOPE ⊆ WRITE_ALLOWLIST: PASS

EFFECTIVE B3 TOTAL: 17 OF 17 PASS
```

## 4. V15 schema freeze

### 4.1 Migration identity

```text
current highest version: V14
selected version: V15
exact future file:
  dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
state in this task: NOT CREATED
```

B1 开始前必须再次枚举 migration。若最高版本不再是 V14，输出 `DH-STAGE-QDR-9-SCHEMA-DESIGN-BLOCKER`，重新选择实际下一版本；禁止覆盖、改名或复用既有 migration。

### 4.2 Physical tables

仓库的 QDR persistence 主表使用 `qdr_*`，因此冻结以下四关系 aggregate：

```text
qdr_feedback_outcome_observation
qdr_feedback_attribution
qdr_feedback_attribution_contribution
qdr_feedback_attribution_reference
```

`dh_nq_feedback_events` 不属于该 aggregate，不建立 FK、不回填、不搬迁数据。

### 4.3 `qdr_feedback_outcome_observation`

```text
id                 uuid primary key
tenant_id          varchar(128) not null
environment        varchar(16) not null
decision_id        varchar(128) not null
trace_id           varchar(128) not null
observation_id     varchar(128) not null
idempotency_key    char(64) not null
outcome_source     varchar(32) not null
outcome_status     varchar(32) not null
observed_at        timestamptz not null
evaluation_time    timestamptz not null
canonical_hash     char(64) not null
created_at         timestamptz not null default transaction_timestamp()
```

约束：

```text
environment in ('DEV','TEST')
outcome_source in ('DRY_RUN_RESULT','DETERMINISTIC_REPLAY','STRUCTURED_TEST_FIXTURE')
outcome_status in ('SUCCEEDED','PARTIALLY_SUCCEEDED','FAILED')
observed_at <= evaluation_time
idempotency_key / canonical_hash = lowercase SHA-256
unique (tenant_id, environment, observation_id)
unique (tenant_id, environment, idempotency_key)
unique (tenant_id, environment, observation_id, decision_id, trace_id, observed_at)
```

### 4.4 `qdr_feedback_attribution`

```text
id                    uuid primary key
tenant_id             varchar(128) not null
environment           varchar(16) not null
observation_id        varchar(128) not null
decision_id           varchar(128) not null
trace_id              varchar(128) not null
observed_at           timestamptz not null
attribution_id        char(64) not null
policy_id             varchar(128) not null
policy_version        varchar(128) not null
attribution_status    varchar(32) not null
confidence            numeric(6,5) not null
canonical_hash        char(64) not null
error_code            varchar(64) not null
created_at            timestamptz not null default transaction_timestamp()
```

约束：

```text
environment in ('DEV','TEST')
attribution_status in ('ATTRIBUTED','INCONCLUSIVE','REJECTED')
0 <= confidence <= 1
attribution_id / canonical_hash = lowercase SHA-256
unique (tenant_id, environment, attribution_id)
foreign key
  (tenant_id, environment, observation_id, decision_id, trace_id, observed_at)
references qdr_feedback_outcome_observation
  (tenant_id, environment, observation_id, decision_id, trace_id, observed_at)
```

`decision_id`、`trace_id`、`observed_at` 是 immutable query projection；复合 FK 防止它们与 observation 漂移。

### 4.5 `qdr_feedback_attribution_contribution`

```text
id                 uuid primary key
tenant_id          varchar(128) not null
environment        varchar(16) not null
attribution_id     char(64) not null
dimension          varchar(64) not null
measurement        numeric(6,5) not null
contribution       numeric(6,5) not null
impact             varchar(16) not null
confidence         numeric(6,5) not null
reason_code        varchar(64) not null
evidence_ref       varchar(256) not null
sort_order         smallint not null
created_at         timestamptz not null default transaction_timestamp()
```

约束：

```text
dimension 为 AttributionDimension 封闭集合
-1 <= measurement <= 1
-1 <= contribution <= 1
0 <= confidence <= 1
impact in ('POSITIVE','NEUTRAL','NEGATIVE') 且与 contribution 符号一致
reason_code ~ '^[A-Z][A-Z0-9_]{0,63}$'
0 <= sort_order < 32
unique (tenant_id, environment, attribution_id, dimension)
unique (tenant_id, environment, attribution_id, sort_order)
foreign key (tenant_id, environment, attribution_id)
  references qdr_feedback_attribution (tenant_id, environment, attribution_id)
  on delete cascade
```

### 4.6 `qdr_feedback_attribution_reference`

```text
id                 uuid primary key
tenant_id          varchar(128) not null
environment        varchar(16) not null
attribution_id     char(64) not null
reference_type     varchar(16) not null
reference_value    varchar(256) not null
reference_status   varchar(16) not null
created_at         timestamptz not null default transaction_timestamp()
```

约束：

```text
reference_type in ('AUDIT','REPLAY','EVALUATION','EVIDENCE')
reference_status in ('ACTIVE','RELEASED','INVALID')
unique (tenant_id, environment, attribution_id, reference_type, reference_value)
foreign key (tenant_id, environment, attribution_id)
  references qdr_feedback_attribution (tenant_id, environment, attribution_id)
  on delete cascade
```

初次写入只允许 `ACTIVE`。`AUDIT`、`REPLAY`、`EVALUATION` 的 `ACTIVE` 行阻止 retention；`EVIDENCE` 必须通过完整性检查，但不单独形成永久 retention hold。

### 4.7 Index freeze

```text
qdr_feedback_outcome_observation:
  (tenant_id, environment, decision_id, observed_at DESC, observation_id DESC)
  (tenant_id, environment, trace_id, observed_at DESC, observation_id DESC)
  (tenant_id, environment, observed_at DESC, observation_id DESC)

qdr_feedback_attribution:
  unique (tenant_id, environment, attribution_id)
  (tenant_id, environment, observation_id)
  (tenant_id, environment, policy_version, observed_at DESC, attribution_id DESC)
  (tenant_id, environment, attribution_status, observed_at DESC, attribution_id DESC)
  (tenant_id, environment, observed_at DESC, attribution_id DESC)

qdr_feedback_attribution_reference:
  (tenant_id, environment, reference_type, reference_status, created_at, attribution_id)
```

所有业务 unique/index 均以 `tenant_id + environment` 开头；禁止 cross-tenant 或 cross-environment global business unique。

### 4.8 JSONB and data safety

四张新表不使用 JSONB。`safeMetadata` 不在 QDR-9 落库；`canonical_hash` 只证明完整规范输入，不保存 canonical raw value。

唯一 JSONB 例外是 B4 复用既有 `dh_decision_audit_event.event_json` 写删除审计，原因是删除后审计必须留在 aggregate 外且不能增加第五张 aggregate 表。固定 schema：

```text
schemaVersion
environment
cutoff
reasonCode
aggregateCount
observationCount
attributionCount
aggregateHash
```

要求：

```text
top-level allowlist only
无嵌套对象或数组
serialized bytes <= 1024
禁止 raw prompt/provider response/payload/credential/交易参数
应用校验 + repository size check
与删除同一 transaction；audit 写失败整体 rollback
```

禁止持久化：

```text
raw prompt
raw provider response
Authorization
HMAC secret
API key
database password
完整外部 payload
BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER
订单、风控、账务、Paper 或 LIVE 执行参数
```

## 5. Repository、transaction 与 idempotency

### 5.1 Persistence input

`FeedbackPersistenceRecords` 只接收：

```text
原始 FeedbackAttributionCommand
FeedbackCanonicalHash（不得向 read model 暴露 canonicalValue）
已完成 audit 的 FeedbackAttributionResult
由应用生成的 UUID row identities
```

构造时必须重新校验 tenant、environment、decision、trace、observation、policy、canonical hash、result identity、contribution 顺序和 reference 一致性。任何 mismatch 在 SQL 前拒绝。

### 5.2 Single transaction

`FeedbackAttributionPersistenceService` 通过强制 `FeedbackPersistenceTransactionBoundary` 执行：

```text
find tenant/environment/idempotency_key
-> absent: confirm AUDIT / REPLAY / EVALUATION / EVIDENCE references
-> insert observation
-> insert attribution
-> insert ordered contributions
-> insert references
-> exact read-back
-> commit
```

要求：

```text
REPEATABLE_READ
PROPAGATION_REQUIRED
transaction manager missing: fail-fast
runtime retry: 0
全部成功或全部 rollback
不得存在半完成 attribution
reference confirmation 失败不得缓存或返回成功
事务内不调用 HTTP、Provider、NQ 或其他外部系统
```

### 5.3 Database idempotency

```text
business key:
  tenant_id + environment + idempotency_key

same key + same canonical_hash:
  exact reload observation + attribution + contributions + references
  返回已有完整 aggregate
  不重复写 audit、observation、children

same key + different canonical_hash:
  IDEMPOTENCY_CONFLICT
  不覆盖、不合并、不创建第二结果

cross-tenant / cross-environment:
  永不复用
```

不得使用“先查再写”作为唯一保护。并发 winner 由 `unique (tenant_id, environment, idempotency_key)` 决定；loser 的 insert transaction 回滚后，只能在新的只读 transaction 按完整 identity reload 并比较 hash。

### 5.4 Duplicate-key and exception classification

```text
DuplicateKeyException:
  transaction rollback
  exact scoped reload
  same hash -> existing aggregate
  different hash -> IDEMPOTENCY_CONFLICT
  missing row -> PERSISTENCE_FAILURE

CannotCreateTransactionException / pre-commit DataAccessException:
  PERSISTENCE_FAILURE

reference target absent or mismatch:
  REFERENCE_INVALID

commit phase connection loss / heuristic / outcome cannot prove:
  COMMIT_OUTCOME_UNKNOWN
  不自动 retry
  不声称成功
```

commit unknown 后只允许 tenant/environment/idempotency key 的 read-only reconciliation：

```text
row exists + same hash -> committed result
row exists + different hash -> IDEMPOTENCY_CONFLICT
row absent -> COMMIT_OUTCOME_UNKNOWN remains
```

稳定失败分类至少为：

```text
IDEMPOTENCY_CONFLICT
TENANT_SCOPE_MISMATCH
ENVIRONMENT_SCOPE_MISMATCH
REFERENCE_INVALID
PERSISTENCE_FAILURE
COMMIT_OUTCOME_UNKNOWN
QUERY_VALIDATION_FAILED
QUERY_FAILURE
RETENTION_BLOCKED
RETENTION_FAILURE
```

数据库原始 SQL、异常栈、连接信息和参数不得进入 use-case result。

## 6. Reference confirmation

Reference value 使用 scheme-bound safe identity；长度上限 256：

```text
AUDIT:
  audit:<audit-event-id>
  按 tenant_id + decision_id + trace_id + id 验证 dh_decision_audit_event

REPLAY:
  replay-case:<uuid> 或 canonical-snapshot:<uuid>
  按 tenant_id + target id 验证 qdr_replay_case / qdr_canonical_replay_snapshot

EVALUATION:
  evaluation:<uuid>
  按 tenant_id + target id 验证 qdr_evaluation_case

EVIDENCE:
  evidence:<safe-id>
  与 command/contribution 的 allowlisted evidence ref 精确一致并通过 safe-reference guard
```

现有 audit/replay/evaluation 表没有统一 environment 列，因此不建立旧表 FK。新 reference row 始终保存当前 `tenant_id + environment`，validator 先绑定该 scope，再执行 tenant-bound target lookup；不得退化为 tenantless lookup。无法证明 target 时 fail-closed。

## 7. Historical Evidence Read Model

### 7.1 Internal-only boundary

只新增 `HistoricalFeedbackEvidenceReadService` 与 query port/JDBC adapter。禁止：

```text
Controller
REST endpoint
OpenAPI
dh-api change
external API
```

如 implementation 需要 API，立即输出：

```text
STAGE_QDR_9_API_SCOPE_REVIEW_REQUIRED
```

### 7.2 Supported selectors

每次查询必须携带 `tenantId + environment`，并使用以下一个 primary selector：

```text
decisionId
traceId
observationId
attributionId
policyVersion
attributionStatus
observedAt range
```

exact `observationId` / `attributionId` 只返回同 tenant/environment 结果；不存在或 cross-scope 均不泄露目标存在性。

### 7.3 Time and page bounds

```text
pagination: keyset only
default limit: 50
hard max: 100
maximum time range: 90 days
default list range: most recent 30 days
sort: observed_at DESC, attribution_id DESC
SQL fetch: requested limit + 1, result never exceeds requested limit
offset: forbidden
unbounded list: forbidden
database exception: QUERY_FAILURE / no partial result
```

Keyset predicate：

```text
observed_at < cursor.observedAt
OR (observed_at = cursor.observedAt AND attribution_id < cursor.attributionId)
```

### 7.4 Cursor contract

cursor 内容：

```text
version = 1
tenantId
environment
selectorType
normalizedFilterHash
observedAt
attributionId
checksum
```

编码：

```text
length-prefixed canonical UTF-8 fields
Base64URL without padding
lowercase SHA-256 checksum over versioned canonical bytes
```

这是 internal opaque cursor 的完整性校验，不是外部 authentication token。tenant/environment/filter 必须与当前 query 重新计算并精确匹配。版本未知、Base64 非法、字段缺失、checksum 错误、scope/filter mismatch、时间或 attributionId 非法时：

```text
QUERY_VALIDATION_FAILED
SQL not executed
no fallback
no partial data
```

未来若新增 external API，必须在独立 API/security review 中改为带 secret lifecycle 的签名 cursor；本 stage 不引入 secret。

## 8. Retention、cleanup 与 integrity

### 8.1 Frozen defaults

```text
retention enabled default: false
default retention: 365 days from observed_at
batch size: 100
transaction timeout: 5 seconds
scope: exactly one tenant + one environment
runtime retry: 0
```

`config/qdr9-feedback/qdr9-feedback-contract.yml` 是稳定 machine contract，只含非敏感默认值、上限和 closed enums。wiring 使用 startup configuration snapshot；不提供动态 production control、调度器或外部 endpoint。

### 8.2 Eligibility and holds

只有超过 retention 且不存在以下 active reference 的完整 aggregate 才可删除：

```text
ACTIVE AUDIT
ACTIVE REPLAY
ACTIVE EVALUATION
```

`EVIDENCE` 必须先通过完整性检查；`INVALID` reference 阻断 cleanup 并返回 `RETENTION_BLOCKED`，不得通过忽略错误推进删除。

### 8.3 Cleanup transaction

```text
select candidate attribution ids for one tenant/environment
-> observed_at cutoff
-> deterministic order
-> limit 100
-> FOR UPDATE SKIP LOCKED
-> recheck ACTIVE holds
-> write bounded deletion audit to existing dh_decision_audit_event
-> delete children through constrained cascade
-> delete attribution
-> delete observation only when no attribution remains
-> verify affected counts <= batch
-> commit
```

删除审计和物理删除必须同事务。超时、audit failure、count mismatch、reference race、DataAccessException 或 orphan detection 均整体 rollback 并返回稳定失败；禁止静默继续。

多实例并发通过 `FOR UPDATE SKIP LOCKED` 分配候选；同 aggregate 最多一个 cleaner 持锁。不得使用跨 tenant batch、全表 delete、无上限循环、定时自动重试或 JVM 全局锁。

### 8.4 Integrity

`FeedbackIntegrityPort` 至少提供 tenant/environment-bound 只读检查：

```text
orphan attribution
orphan contribution
orphan reference
observation/attribution scope drift
canonical hash format
attribution id format
contribution sort gap / duplicate dimension
invalid reference type/status
ACTIVE hold / retention eligibility conflict
forbidden raw-payload-shaped column or value
```

检查结果有界；发现问题只返回 report 并阻断 cleanup，不自动猜测或修复数据。

## 9. Batch freeze

### B1 — Persistence Contracts and Schema Baseline

允许：

```text
FeedbackPersistenceErrorCode / Exception / Records
Repository / reference validation / transaction ports
V15 migration
stable qdr9 feedback machine contract
migration presence and PostgreSQL/Testcontainers schema tests
schema/constraint/index contract tests
```

禁止：

```text
JDBC aggregate write implementation
read-model adapter
retention delete
API / Controller
automatic learning
```

B1 涉及 migration，implementation 后必须执行独立 milestone review。下一任务：

```text
DH-STAGE-QDR-9-B1-PERSISTENCE-CONTRACTS-AND-SCHEMA-IMPLEMENTATION
```

开始条件：

```text
plan + work-order commits 已发布
published exact SHA CI test + quality PASS
migration highest version 仍为 V14
B1 exact subset invariants PASS
```

### B2 — JDBC Persistence and Transactional Idempotency

允许：

```text
JdbcFeedbackAttributionRepository
JdbcFeedbackReferenceValidationAdapter
JdbcFeedbackPersistenceTransactionBoundary
FeedbackAttributionPersistenceService
internal wiring
database idempotency / duplicate-key / commit-unknown
PostgreSQL restart persistence
```

必须 milestone review。禁止 API、runtime retry、in-memory production state、automatic learning。

### B3 — Historical Evidence Internal Read Model

允许：

```text
internal query models/service/port
cursor codec
JDBC keyset adapter
stable ordering
bounded result set
```

正常 validation 收口；发现 API 需求转 `STAGE_QDR_9_API_SCOPE_REVIEW_REQUIRED`。

### B4 — Integrity and Retention

允许：

```text
integrity report
reference guard
bounded cleanup
deletion audit
startup-disabled retention wiring
```

涉及 physical delete，必须安全 review；默认保持 disabled。

### B5 — Final Close

必须包含：

```text
targeted tests
full 19-module regression
PostgreSQL/Testcontainers real execution / zero mandatory skips
ArchitectureTest
quality
12/12 current factsources / 0 conflicts
self-contained archive packet
source-documents copy
archive close commit
separate annotated tag task
post-tag current pruning
machine dependency scan
```

B5 不是当前 WRITE_ALLOWLIST 授权。archive、tag、pruning 必须在 final-close/archive/tag 的独立 exact scope 中重新冻结。

## 10. Test matrix

### 10.1 Migration / schema

```text
clean migration
upgrade V14 -> V15
four tables exist
all columns/types/defaults/comments
all PK/unique/FK/check constraints
all indexes
tenant/environment leading constraints
field lengths
forbidden nullable fields
no JSONB in four aggregate tables
dh_nq_feedback_events unchanged
V15 rerun/version immutability contract
```

### 10.2 Persistence

```text
first write
same key / same hash exact aggregate reload
same key / different hash IDEMPOTENCY_CONFLICT
observation insert rollback
attribution insert rollback
contribution insert rollback
reference confirmation/write rollback
duplicate-key concurrency
cross-tenant isolation
cross-environment isolation
stable contribution sort
PostgreSQL restart persistence
commit-unknown classification
unknown outcome no auto retry
exact read-back mismatch fail-closed
```

### 10.3 Query

```text
decision query
trace query
observation query
attribution query
policyVersion filter
attributionStatus filter
observedAt range
stable observed_at/attribution_id ordering
cursor next page without duplicate/gap
invalid version/base64/checksum/scope/filter cursor
default limit 50
hard max 100
90-day range cap
default 30-day range
cross-tenant rejection
cross-environment rejection
database failure / no partial result
offset query absent
```

### 10.4 Retention

```text
expired unreferenced delete
ACTIVE AUDIT block
ACTIVE REPLAY block
ACTIVE EVALUATION block
INVALID reference block
tenant isolation
environment isolation
batch cap 100
timeout 5 seconds
concurrent cleaners / SKIP LOCKED
partial failure rollback
deletion audit same transaction
audit failure rollback
orphan contribution/reference detection
count mismatch fail-closed
retention disabled by default
```

### 10.5 Architecture guards

```text
no API / Controller / OpenAPI
no HTTP
no Provider
no NQ
no Agent / LangGraph
no automatic learning
no Experience / Pheromone / Prompt / Strategy mutation
no order / risk / ledger / Paper / LIVE mutation
no raw prompt / provider response / full payload persistence
no dependency on prunable docs/current process documents
no unbounded Map/cache/query/delete/retry
```

## 11. Validation commands

每 batch 执行其最小 targeted tests；B5 执行完整集合。至少包括：

```powershell
git status --short
git diff --check
git diff --name-only
mvn -B -ntp -pl dh-usecase,dh-infra,dh-app -am test
mvn -B -ntp test
mvn -B -ntp -Pquality validate
```

PostgreSQL/Testcontainers 必须真实执行并确认 mandatory tests `Skipped: 0`。所有 current factsources 必须扫描为 12/12、一个 authority block hash、0 conflicts。

## 12. Rollback and publication discipline

```text
已发布 migration 不删除、不重写、不改名
migration 保持 forward-compatible
schema correction 使用新的 forward migration
代码/文档回滚使用普通 git revert
新表可保留但停用 wiring
禁止 destructive down migration
retention 默认 disabled，独立 startup switch
禁止 reset --hard / history rewrite
annotated tag 不移动
```

每个 batch 独立 commit。plan/work-order 发布与 exact-SHA CI 通过前，不允许开始 B1；本任务不 push、不 tag。

## 13. Stop conditions and readiness

出现以下任一情况立即停止：

```text
scope invariant != 8/8
migration highest version != V14 before B1
需要 WRITE_ALLOWLIST 外文件
需要 API / Controller / OpenAPI
需要复用 dh_nq_feedback_events 或保存 raw payload
无法证明 tenant/environment isolation
reference target 无法 fail-closed confirmation
duplicate race 只能依赖 JVM state
commit unknown 需要自动 retry
retention 无法保证 active hold
需要真实 HTTP / Provider / NQ / Agent / LangGraph / Paper / LIVE
需要 automatic learning 或状态自动变更
出现新 P0/P1 security/tenant/transaction/migration/API blocker
```

当前 readiness：

```text
STAGE_QDR_9_PLAN_BASELINE: COMMITTED
STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER: DONE / LOCAL_ACCEPTED
SCOPE_INVARIANTS: PASS / 8 OF 8
ALLOW_PLAN_WO_PUBLICATION: YES / EXPLICIT AUTHORIZATION REQUIRED
ALLOW_STAGE_QDR_9_B1_IMPLEMENTATION: NO / PUBLICATION_AND_EXACT_SHA_CI_REQUIRED
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_AUTOMATIC_LEARNING: NO
B2_CAPACITY_GATE: DEFERRED / KNOWN_LIMITATION
PRODUCTION_CAPACITY: NOT_PROVEN
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME: NO
ALLOW_AGENT: NO
ALLOW_LANGGRAPH: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
```

下一精确任务：

```text
DH-STAGE-QDR-9-PLAN-WO-PUBLICATION-AND-EXACT-SHA-CI
```

## B4 Reference-Liveness State-Model Blocker

本节在既有 B4 implementation 已存在后追加，绝不重写原 B4 exact scope 或声称其已经包含下列跨模块变更。

```text
Stage-QDR-9 B4: IMPLEMENTED LOCALLY / MILESTONE REVIEW BLOCKED
B4 scope-prewrite commit: 1036171ff7acd85078416b907f0444ab6214b3ca
B4 implementation commit: fa9debb4474eedc4353e1236e227eae8ef23c085
review blocker: STAGE_QDR_9_B4_REFERENCE_STATE_MODEL_UNRESOLVED
P1 findings: 2
P1-1: RELEASED reference snapshot cannot prove current target inactivity
P1-2: AUDIT / REPLAY / EVALUATION have no trustworthy environment-bound lifecycle state
selected remediation: unified qdr_reference_liveness registry
forward migration: V16__qdr9_reference_liveness_state_model.sql / CANDIDATE / NOT_CREATED
reference-liveness design: FROZEN / LOCAL_ACCEPTED
B4 publication: NOT_ALLOWED
B4 milestone review retry: NOT_ALLOWED UNTIL FORWARD MIGRATION AND P1 FIX PASS
B5: NOT_ALLOWED
```

`event_status`、evaluation `verdict`、native row existence 和 reference snapshot `RELEASED` 均不是 lifecycle。未来 implementation 仅可依据
`DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_STATE_MODEL_DESIGN.md` 与
`DH_STAGE_QDR_9_B4_REFERENCE_LIVENESS_FORWARD_MIGRATION_WORK_ORDER.md` 的 27/27 exact scope 执行；retention 保持 default-disabled，
并且不得创建 API、scheduler、automatic learning、real HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE 行为。

## B4 Blocked Remote Publication Containment

```text
B4 scope-prewrite created: 1036171ff7acd85078416b907f0444ab6214b3ca
B4 implementation created: fa9debb4474eedc4353e1236e227eae8ef23c085
Initial milestone review: BLOCKED / REFERENCE_STATE_MODEL_UNRESOLVED / P1 2 OPEN
Remote publication: CONFIRMED / NOT AUTHORIZED BY THE LOCAL REVIEW STATE
Authority divergence: CONFIRMED
Containment decision: OPTION 1 / ORDINARY REVERT
Implementation containment commit: 4bdcd01f597e822c0e2592bfc368de6eaac56015
Scope withdrawal containment commit: 6fadeb6c6324f6c487175e31b5c80531e30ff9e0
History rewrite: NONE
Current B4 retention implementation: REVERTED / NOT PRESENT
Historical audit: RETAINED
Reference-liveness design: RETAINED / 4ef3991f8379b6653f06a22bcdf5f4b33fcb7585
V16: CANDIDATE / NOT CREATED
B4 milestone review retry / B4 retention publication / B5: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
```

本节不删除已发布提交对象，也不把远端 publication、P1 修复或 B4 acceptance 改写为未发生或已完成。普通 revert 只使当前技术树回到 B3 authority 的等价状态；后续 V16 前向迁移还必须先解决 producer environment source blocker。
+
## B4 Producer Environment Contract Blocker

V16 implementation attempt is BLOCKED BEFORE CODE WRITE. The code audit found that AUDIT, REPLAY and EVALUATION producers do not carry an explicit FeedbackEnvironment from a trusted entry point through their native records to the future registry transition.

~~~text
BLOCKER: STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_CONTRACT_BLOCKER
NO_DEFAULT_OR_INFERENCE: PASS
NEW_CROSS_MODULE_SCOPE: DH_STAGE_QDR_9_B4_PRODUCER_ENVIRONMENT_SCOPE_WORK_ORDER.md
AUDIT / REPLAY / EVALUATION CONTRACT: FROZEN / LOCAL_ACCEPTED
AUDIT / REPLAY / EVALUATION SOURCE: UNRESOLVED / FAIL_CLOSED
ORIGINAL_REFERENCE_LIVENESS_SCOPE: 27 / 27 PASS
EFFECTIVE_SCOPE: 31 / 31 PASS
V16: NOT CREATED
B4 RETENTION: NOT PRESENT IN CURRENT TREE
ALLOW_V16_IMPLEMENTATION_RETRY: NO
B4 MILESTONE REVIEW RETRY / B4 PUBLICATION / B5: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
~~~

禁止把 tenant、ID、trace、event_status、verdict、Spring profile、guard property default 或 existing target 解释为 environment；缺失时不得写 native target 或 registry transition。这个 blocker 不改变 Option B，不修改 V1-V15，不恢复 retention，也不授权 API、scheduler、automatic learning 或任何外部 runtime。

上一 source-resolution task 保持 BLOCKED 的历史事实；其 design input 已由 trusted upstream authority decision 吸收。新的实施顺序为：

~~~text
AUDIT authority: OPTION B / NEW EXPLICIT SIGNED CONTRACT REQUIRED
REPLAY/EVALUATION: DORMANT / NO PRODUCTION ENTRY
FeedbackExecutionScope: FROZEN / NOT IMPLEMENTED
ORIGINAL_TRUSTED_UPSTREAM_SCOPE: 42 / 42 PASS / BLOCKED DURING IMPLEMENTATION / SUPERSEDED FOR IMPLEMENTATION ACCEPTANCE
SCOPE_ERRATUM: DONE / 6 EXACT FILES ADDED
EFFECTIVE_UPSTREAM_SCOPE: 46 / 46 PASS
V16: CANDIDATE / NOT CREATED
V16 implementation: NOT ALLOWED
NEXT_TASK: DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-SCOPE-BLOCKER-RETRY-2
~~~

scope erratum 精确加入 `DecisionDryRunErrorCode`、`DecisionOrchestrator`、`DecisionDryRunRequestFingerprint` 与三个既有 PostgreSQL compatibility tests；它不修改原 42-file 历史事实，也不实施代码。下一 retry 的 Part A 已将唯一遗留 PostgreSQL fixture `PersistentGuardProductionWiringPostgresTest` 加入 `B4_UPSTREAM_PERSISTENT_GUARD_COMPATIBILITY_TEST_SCOPE`；effective upstream scope 因此为 `48 / 48 PASS`。Part B 只能升级该 fixture 的显式 signed DEV/TEST environment 与 verified `FeedbackExecutionScope`，并保留 persistent guard 成功、基础设施错误和恢复语义；不得创建 V16、registry/retention integration、REPLAY/EVALUATION controller 或 runtime wiring、API、scheduler 或 automatic learning。
