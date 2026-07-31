# DH Stage-QDR-9 B4 Identity and Replay Namespace Work Order

## 1. 工单状态

~~~text
task:
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-IMPLEMENTATION

status:
BLOCKED / NOT AUTHORIZED

design:
DH_STAGE_QDR_9_B4_IDENTITY_AND_REPLAY_NAMESPACE_DESIGN.md

scope:
DH_STAGE_QDR_9_B4_IDENTITY_AND_REPLAY_NAMESPACE_SCOPE_ERRATUM.md

baseline:
b0ff11e4057077ad7e0fe91d691116f069dc744e

ALLOW_IMPLEMENTATION:
NO
~~~

本工单是未来实施入口的冻结记录，不是实施授权。Audit environment 已完成 scope design，但 implementation
仍被 V16 predecessor 和 producer cutover 阻断。当前仍有三个前置 blocker：

1. audit environment 已冻结为 V17 Option A/B1/S2；persistent identity version storage 需要
   V18 candidate，但 artifact scope 尚未冻结；
2. legacy persistent identity 已选择 L2/T1，但 three-cutoff hard lifetime proof 尚未闭合；
3. legacy replay TTL 无 hard ceiling，且 port 不支持原子 legacy-check + v2 admission。

任何 blocker 未关闭时不得修改本工单所列生产或测试文件。

## 2. Required contracts

~~~text
RATE_IDENTITY_VERSION=QDR9-RATE-IDENTITY-2
IDEMPOTENCY_IDENTITY_VERSION=QDR9-IDEMPOTENCY-IDENTITY-2
RECOVERY_IDENTITY_VERSION=QDR9-RECOVERY-IDENTITY-2
REPLAY_NAMESPACE_VERSION=QDR9-DRYRUN-REPLAY-2
CANONICAL_ENCODING=QDR9-LP1
LEGACY_PERSISTENT_OPTION=L2 / NORMAL PATH SELECTED / LIFETIME BLOCKED
LEGACY_TOMBSTONE_RETIREMENT=T1 / NOT IMPLEMENTED
LEGACY_REPLAY_OPTION=B / BLOCKED
AUDIT_ENVIRONMENT_STORAGE=FORWARD MIGRATION REQUIRED
AUDIT_EVENT_COMPATIBILITY_CONSTRUCTOR=REMOVE
~~~

verified environment 的唯一来源为 `command.executionScope().environment()` 或同一 verified
`FeedbackExecutionScope` 的强类型传播路径。不得恢复 `DecisionDryRunGuardProperties.environment()` 作为
request authority。

## 3. Future implementation sequence

### M1 — migration sequencing scope design

- audit structured environment column：DONE / OPTION A；
- audit legacy backfill：DONE / B1 NULL UNKNOWN LEGACY；
- audit migration sequence：DONE / S2，V16 registry 后为 V17 audit environment；
- candidate：`V17__qdr9_audit_environment_storage.sql` / NOT CREATED；
- persistent identity version/verified-environment storage：仍由 legacy persistent blocker 冻结；
- legacy retirement audit：独立 structured storage / V18 candidate semantics / NOT CREATED；
- clean/upgrade/backfill/constraint/index/Testcontainers matrix：FROZEN / NOT IMPLEMENTED。

### L1 — legacy persistent identity blocker

- selected option：L2 / bounded conservative blocking + physical retirement；
- selected tombstone retirement：T1 / same-transaction audit + DELETE；
- frozen cleanup：internal-only、default-disabled、tenant/family scoped、bounded、stable-order、
  `FOR UPDATE SKIP LOCKED`、no automatic retry；
- unresolved：maximum in-flight transaction、commit-unknown reconciliation、independent recovery
  availability hard ceilings；
- migration：required / `V16 -> V17 -> V18 candidate` / artifact path not authorized；
- next blocker：`DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-LIFETIME-BLOCKER`。

### L2 — legacy replay blocker

- 为 maxClockSkew 冻结正数 hard ceiling；
- 冻结 rolling-upgrade old-writer stop proof；
- 冻结未过期 legacy check + v2 insert 的原子 port；
- 冻结 compatibility window 的 DB-time 结束条件。

### I1 — signed environment propagation retry

只有 M1/L1/L2 全部 accepted 后，才可恢复已冻结的 signed environment、`FeedbackExecutionScope`
与 tenant/source/environment authorization；不得 cherry-pick `549ed5a…`。

### I2 — persistent identity, audit and replay implementation

- rate/idempotency/recovery 使用各自 versioned namespace；
- rate audit 使用同一 verified environment；
- replay 使用 `qdr9-dryrun-replay-v2:<sha256>`；
- legacy Option B fail-closed；
- same-environment duplicate semantics 不变；
- DEV/TEST 并发隔离。

### V — verification

依次执行 targeted unit、PostgreSQL/Testcontainers、architecture、full reactor 与 quality；任何
P1/P2 acceptance 前不得启动 B4 review retry。

## 4. Stop conditions

出现以下任一情况立即停止：

- 需要未列入 exact scope 的 Java/test/POM/config/workflow；
- 需要创建 migration 但 sequencing 尚未 accepted；
- 需要把 legacy row 默认映射 DEV/TEST；
- 需要把 environment 放入 JSON/message/log；
- 需要忽略未过期 legacy replay key；
- 需要物理删除 idempotency tombstone 而无 migration/rollback；
- 需要在 independent structured retirement audit 写入并提交前物理删除；
- 需要把 90 days 或 cleanup safety grace 当作缺失 hard ceiling；
- 需要新 endpoint、scheduler、registry、retention、B5 或 automatic learning；
- 需要真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。

## 5. Acceptance matrix

### Persistent identities

- exact version constants 与 `QDR9-LP1` golden vectors；
- field reorder、missing、null、outer whitespace、unknown environment fail-closed；
- same tuple DEV/TEST key 不同；
- same environment same request reuse/conflict 不变；
- cross-environment concurrent rate/idempotency/recovery 隔离。

### Rate audit

- audit structured column 精确等于 rate identity verified environment；
- accepted/rate-limited/store/audit failure 的 transaction outcome；
- production source 无 environmentless constructor；
- legacy audit row upgrade/read compatibility。

### Replay

- HMAC environment tamper 在 replay admission 前失败；
- method/path/version/environment 全部进入 canonical frame；
- legacy unexpired/expired/absent 三态；
- concurrent same environment 一个 winner；
- concurrent DEV/TEST 两个 winner；
- DB key 长度稳定小于 512；
- store unavailable fail-closed。

### Mandatory fixtures

~~~text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunSamePoolRecoveryPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/DecisionDryRunActualWiringRepeatabilityPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
~~~

## 6. Current boundary

~~~text
code change:
NO

test change:
NO

POM change:
NO

migration creation:
NO

V16 / V17 / registry / retention:
NO / NO / NO / NO

B4 review retry / publication / B5:
NO / NO / NO

push / tag:
NO / NO
~~~

## 7. Next action

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-LIFETIME-BLOCKER
~~~
