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

本工单是未来实施入口的冻结记录，不是实施授权。当前有三个前置 blocker：

1. audit environment 与 persistent identity version 需要 forward migration；
2. legacy idempotency tombstone 没有安全 retirement；
3. legacy replay TTL 无 hard ceiling，且 port 不支持原子 legacy-check + v2 admission。

任何 blocker 未关闭时不得修改本工单所列生产或测试文件。

## 2. Required contracts

~~~text
RATE_IDENTITY_VERSION=QDR9-RATE-IDENTITY-2
IDEMPOTENCY_IDENTITY_VERSION=QDR9-IDEMPOTENCY-IDENTITY-2
RECOVERY_IDENTITY_VERSION=QDR9-RECOVERY-IDENTITY-2
REPLAY_NAMESPACE_VERSION=QDR9-DRYRUN-REPLAY-2
CANONICAL_ENCODING=QDR9-LP1
LEGACY_PERSISTENT_OPTION=B / BLOCKED
LEGACY_REPLAY_OPTION=B / BLOCKED
AUDIT_ENVIRONMENT_STORAGE=FORWARD MIGRATION REQUIRED
AUDIT_EVENT_COMPATIBILITY_CONSTRUCTOR=REMOVE
~~~

verified environment 的唯一来源为 `command.executionScope().environment()` 或同一 verified
`FeedbackExecutionScope` 的强类型传播路径。不得恢复 `DecisionDryRunGuardProperties.environment()` 作为
request authority。

## 3. Future implementation sequence

### M1 — migration sequencing scope design

- 冻结 audit structured environment column；
- 冻结 persistent identity version/verified-environment storage；
- 冻结 legacy nullable rows、new-write constraints、indexes 与 rollback；
- 不占用或假定 V16；
- 输出 clean migration、V15 upgrade、legacy row 与 constraint Testcontainers matrix。

### L1 — legacy persistent identity blocker

- 证明 legacy rate window 最大 3600 秒；
- 冻结 legacy idempotency tombstone 的 bounded retirement；
- 冻结 atomic legacy check + v2 admission；
- 禁止默认 DEV 或按 deployment environment backfill。

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

V16 / registry / retention:
NO / NO / NO

B4 review retry / publication / B5:
NO / NO / NO

push / tag:
NO / NO
~~~

## 7. Next action

~~~text
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN
~~~
