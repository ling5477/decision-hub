# DH Stage-QDR-9 B4 Legacy Persistent Identity Retirement Work Order

## 1. 工单状态

~~~text
task:
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-RETIREMENT-IMPLEMENTATION

status:
BLOCKED / NOT AUTHORIZED

design:
DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_DESIGN.md

scope:
DH_STAGE_QDR_9_B4_LEGACY_PERSISTENT_IDENTITY_SCOPE_ERRATUM.md

baseline:
aab84e896595bbd8b3f5e99e8b2880ca28f8e7a4

selected option:
L2

selected tombstone retirement:
T1

migration:
REQUIRED / V18 CANDIDATE AFTER V16 AND V17 / NOT CREATED

ALLOW_IMPLEMENTATION:
NO
~~~

本工单冻结未来顺序和 stop conditions，不授权任何技术文件写入。当前 three-cutoff lifetime proof
未闭合，migration artifact path 未进入 allowlist，retirement audit schema 未创建，legacy replay
仍有独立 blocker。

## 2. Required contracts

~~~text
LEGACY_OPTION=L2
NORMAL_PATH=LIVE -> EXPIRED_BUT_BLOCKING -> RETIREMENT_ELIGIBLE -> PHYSICALLY_RETIRED
UNKNOWN_FALLBACK=BLOCK
TOMBSTONE_RETIREMENT=T1
CLEANUP=INTERNAL_ONLY / DEFAULT_DISABLED / TENANT_AND_FAMILY_SCOPED
LOCKING=STABLE_ORDER / FOR_UPDATE_SKIP_LOCKED
TRANSACTION=ONE_REQUIRED_TRANSACTION_PER_BATCH
AUDIT=INDEPENDENT_STRUCTURED_IMMUTABLE_STORAGE
RETRY=NO_AUTOMATIC_RETRY
SCHEDULER_API=NO / NO
~~~

Verified environment 只能来自同一 verified `FeedbackExecutionScope`。Legacy deployment
`environment` 永远不得 backfill、默认或推断为 DEV/TEST。

## 3. Blocking prerequisites

### L0 — lifetime proof

必须由独立 blocker 冻结：

- old writer stop/fence proof；
- maximum in-flight guard transaction lifetime；
- commit-unknown reconciliation hard maximum；
- independent recovery/business-result availability hard maximum；
- 三类 identity 的 exact DB-time cutoff。

任一为 `NOT_AVAILABLE` 时：`ALLOW_IMPLEMENTATION=NO`。

### M0 — migration sequencing

只有 L0 accepted 后才允许独立 sequencing task：

- 证明 V16 registry 与 V17 audit environment 已按冻结职责 accepted；
- 复核 V18 版本未被占用；
- 冻结 v2 structured namespace schema、retirement audit table、indexes、constraints、comments；
- 将唯一 V18 artifact 与 migration tests 加入 exact allowlist；
- 不修改 V1–V17。

### C0 — producer cutover

- old binary 不再写 legacy rate/idempotency row；
- rolling deployment fence 使用 DB-visible evidence；
- verified environment producer 全量生效；
- 未知 cutover 状态 fail-closed。

## 4. Future implementation sequence

### C1 — contracts

- 实现 legacy classification 与 retirement eligibility typed contract；
- rate、idempotency/recovery 保持独立 family；
- request fingerprint、replay identity 不进入 retirement key；
- unknown/malformed/unbounded 状态统一 block。

### M1 — forward migration

- 只创建 sequencing task 最终确认的 V18；
- 建立 versioned verified-environment namespace；
- 建立独立 immutable retirement audit storage；
- legacy environment 保持 unknown；
- clean、V15→V16→V17→V18 upgrade、checksum 与 rollback-safety tests。

### R1 — retirement primitives

- `GuardCleanupPort` 不暴露任意 delete；
- candidate 返回 typed low-sensitivity reference，不只返回 count；
- T1 delete 与 structured audit 一一对应；
- audit failure/delete failure 整批 rollback；
- EXPIRED 不能通过状态更新替代 physical retirement。

### S1 — internal service

- default disabled；
- one tenant + one family；
- one bounded transaction per invocation；
- no scheduler、startup、endpoint；
- no automatic retry 或 unbounded loop。

### A1 — atomic legacy check + v2 admission

- legacy absent/retired proof 与 v2 insert 在同一 PostgreSQL contract；
- same-environment unique semantics 不变；
- DEV/TEST 独立；
- old writer 未 fence 时 fail-closed；
- legacy cleanup 永不命中 v2 row。

### V — verification

依次执行 unit、PostgreSQL/Testcontainers、concurrency、architecture、module/full regression 与 quality。
P1/P2 未关闭前不得启动 B4 review retry。

## 5. Stop conditions

出现以下任一情况立即停止：

- 任一 cutoff 仍为 `NOT_AVAILABLE`；
- V16/V17 未 accepted 或 V18 已被占用；
- 需要默认、推断或复制 legacy environment；
- 需要在 structured audit commit 前物理删除；
- 需要复用 `dh_decision_audit_event` 并伪造 decision/environment；
- 需要 scope erratum 之外的 Java/test/POM/migration/config/workflow；
- 需要 scheduler、startup cleanup、API、跨 tenant delete 或 automatic retry；
- 需要 registry、retention、B4 review/publication、B5 或 automatic learning；
- 需要真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。

## 6. Acceptance matrix

### Rate

- active/expired-within-cutoff/eligible/retired 四态；
- old-writer fence + maximum transaction lifetime；
- DEV/TEST independent bucket；
- same environment fixed-window semantics unchanged；
- cleanup/admission race 无 quota bypass。

### Idempotency

- active/completed/failed/EXPIRED/unknown；
- TTL、retention、lease、commit-unknown proof；
- same hash reuse 与 different hash conflict 不变；
- retired tombstone 解除阻断且不允许重复业务执行；
- no duplicate retirement audit。

### Recovery

- valid result 保持到 accepted cutoff；
- result/checksum missing fail-closed；
- expired-lease recovery 与 cleanup race；
- no cross-environment result reuse；
- guard row 与 decision output relationship 不被静默破坏。

### Cleanup and audit

- default disabled、batch 1..1000、tenant/family scoped；
- stable order + `FOR UPDATE SKIP LOCKED`；
- two workers；
- audit failure rolls back delete；
- delete failure rolls back audit；
- malformed/unknown row remains blocking。

### Mandatory PostgreSQL/architecture fixtures

~~~text
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/StageQdr9FeedbackArchitectureTest.java
~~~

## 7. Current boundary

~~~text
code / test / POM change:
NO / NO / NO

migration creation:
NO

V16 / V17 / V18:
NOT CREATED / NOT CREATED / NOT CREATED

registry / retention:
NOT IMPLEMENTED / NOT PRESENT

legacy identity / replay implementation:
NO / NO

B4 review retry / publication / B5:
NO / NO / NO

push / tag:
NO / NO
~~~

## 8. Next action

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-LIFETIME-BLOCKER
~~~
