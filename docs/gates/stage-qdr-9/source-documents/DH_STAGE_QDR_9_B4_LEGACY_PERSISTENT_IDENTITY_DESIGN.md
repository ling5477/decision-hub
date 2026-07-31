# DH Stage-QDR-9 B4 Legacy Persistent Identity Retirement Design

## 1. 决策状态

~~~text
task:
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-BLOCKER

baseline:
aab84e896595bbd8b3f5e99e8b2880ca28f8e7a4

classification:
DOCUMENTATION / SECURITY_DESIGN / LEGACY_PERSISTENT_IDENTITY_RETIREMENT

current technical tree:
B3 SAFE BASELINE

SELECTED LEGACY PERSISTENT IDENTITY OPTION:
L2 / BOUNDED CONSERVATIVE BLOCKING + PHYSICAL RETIREMENT

NORMAL TRANSITION CONTRACT:
SELECTED / IMPLEMENTATION BLOCKED BY UNPROVEN HARD LIFETIME CEILINGS

UNKNOWN/UNPROVABLE FALLBACK:
BLOCK

SELECTED TOMBSTONE RETIREMENT:
T1 / TRANSACTIONAL PHYSICAL DELETE

LEGACY IDENTITY MIGRATION REQUIRED:
YES

MIGRATION ORDER:
V16 REGISTRY -> V17 AUDIT ENVIRONMENT -> V18 LEGACY IDENTITY CANDIDATE

MIGRATION ARTIFACT:
NOT CREATED / EXACT VERSIONED PATH REQUIRES SEPARATE SEQUENCING ACCEPTANCE

EFFECTIVE_SCOPE_INVARIANTS:
66 / 66 PASS

LEGACY_PERSISTENT_IDENTITY_DESIGN:
BLOCKED

ALLOW_LEGACY_IDENTITY_IMPLEMENTATION:
NO
~~~

本设计完成数据库事实、终态方案、tombstone、cleanup、并发、审计和未来 scope 的冻结，但不把
尚未证明的 lifetime 写成安全值。L2 是唯一允许的正常迁移终态；当前因 old-writer in-flight
transaction bound、commit-unknown reconciliation bound 和 recovery availability bound 均为
`NOT_AVAILABLE`，三个 retirement cutoff 不能接受，implementation 继续 fail-closed。

本文件不修改 Java、测试、POM、migration、配置或 workflow；不创建 V16、V17 或 V18；不实施
persistent identity、replay namespace、registry、retention、B4 review retry/publication 或 B5。

## 2. 权威输入与一致性

按任务顺序审计了 identity/replay design、work order、scope erratum、audit environment storage
design、forward-migration work order、audit environment scope erratum、remote containment 和
Stage-QDR-9 implementation work order。代码现实与这些冻结结论一致：

- 新 persistent identity 必须使用 `QDR9-RATE-IDENTITY-2`、
  `QDR9-IDEMPOTENCY-IDENTITY-2`、`QDR9-RECOVERY-IDENTITY-2` 和 `QDR9-LP1`；
- verified environment 唯一来源为 `command.executionScope().environment()` 或同一 verified
  `FeedbackExecutionScope`；
- 当前 row 的 `environment` 只代表 deployment config，不代表 signed request environment；
- legacy row 不得默认或推断为 DEV/TEST；
- V16 保留给 `qdr_reference_liveness` registry，V17 保留给 audit environment structured storage；
- 当前最高 migration 为 V15，V16/V17/V18 均不存在。

~~~text
TASK_SCOPE_DESIGN_INVALID:
NO

CURRENT_FACTSOURCE_CONFLICTS:
0
~~~

## 3. 数据库事实矩阵

### 3.1 Rate bucket identity

| 项目 | 当前事实 |
| --- | --- |
| table | `dh_qdr7_rate_limit_bucket` |
| primary/unique key | PK `(environment, endpoint, source, tenant_id, window_start)` |
| identity column | deployment `environment`、`endpoint`、`source`、`tenant_id`、DB `window_start` |
| status | `NOT_AVAILABLE`；row 是否 live 由 `window_end` 与 DB time 决定 |
| window start/end | PostgreSQL `transaction_timestamp()` 对齐 UTC epoch；`window_end = window_start + window_seconds` |
| expiry source | `window_end` |
| hard maximum | `window_seconds <= 3600`；quota `<= 100000` |
| cleanup path | `GuardCleanupPort.cleanupExpiredRateBuckets` → `JdbcGuardCleanupAdapter.RATE_SQL` |
| physical delete | YES；`window_end < transaction_timestamp() - safetyGrace` |
| cleanup bounds | tenant/environment/endpoint/source scoped；`safetyGrace <= 1 day`；batch `<= 1000` |
| concurrency lock | stable order + `FOR UPDATE SKIP LOCKED`；admission 使用 composite PK conditional upsert |
| tenant/source binding | YES / YES |
| environment binding | deployment string only；verified signed environment = NO |

Legacy rate row 只阻断同 endpoint/source/tenant 且仍可能属于当前 quota window 的 v2 admission。
禁止使用 legacy `environment` 选择 DEV 或 TEST；legacy check 必须跨全部旧 deployment-environment
值执行。旧 row 已有物理删除能力，但 old writer 是否仍可能提交新 legacy bucket 没有 hard fence。

### 3.2 Idempotency admission identity

| 项目 | 当前事实 |
| --- | --- |
| table | `dh_qdr7_idempotency_guard` |
| primary key | `guard_id uuid` |
| unique identity | `(environment, endpoint, source, tenant_id, request_id)` |
| identity column | deployment `environment`、`endpoint`、`source`、`tenant_id`、`request_id` |
| status values | `RECEIVED`、`IN_PROGRESS`、`COMPLETED`、`FAILED`、`EXPIRED` |
| `RECEIVED` | 已登记，未取得 lease |
| `IN_PROGRESS` | worker 持有 lease；CAS 使用 state/version/owner/token |
| `COMPLETED` | 保存 `DH_DECISION_OUTPUT` typed result reference 与 checksum |
| `FAILED` | 保存 stable error code，不自动重试 |
| `EXPIRED` | 不可重新激活的 blocking tombstone；result/error/lease 已清空 |
| created/updated | `created_at` / `updated_at`，均由 PostgreSQL transaction time 生成 |
| expiry | `expires_at`；admission TTL hard maximum 7 days |
| retention | `retention_until`；terminal retention hard maximum 90 days |
| state timestamps | `completed_at`、`failed_at`、`expired_at` |
| EXPIRED transition owner | `JdbcGuardCleanupAdapter`，或合法 CAS transition 到 `EXPIRED` |
| physical-delete path | `NOT_AVAILABLE` |
| result/recovery payload | `result_type`、`result_id`、`result_checksum`；exact FK 指向 `dh_decision_output` |
| EXPIRED unique/lookup | 仍参与 unique 与 exact lookup；同 hash 返回 `EXPIRED`，不同 hash 返回 conflict |

`JdbcGuardCleanupAdapter` 只把 eligible row 更新为 `EXPIRED`，同时清空 lease、result、checksum、
stable error 和 terminal timestamps，再写 `expired_at`。V13 明确声明“本阶段禁止物理删除”，当前
production tree 没有后续 DELETE，因此 EXPIRED lifetime 无上限，唯一键永久阻断 requestId。

### 3.3 Business result / recovery identity

| 项目 | 当前事实 |
| --- | --- |
| storage table | `dh_qdr7_idempotency_guard`；safe result 位于 `dh_decision_output` |
| separate legacy row | `NOT APPLICABLE`；recovery 与 idempotency 共享同一 guard row |
| identity key | legacy persistent identity + `request_id` + `request_hash`；result exact reference 另含 tenant |
| status | 继承 idempotency state |
| result reuse | `COMPLETED` duplicate 读取 tenant-bound result，重建 safe projection 并校验 checksum |
| expired-lease recovery | adapter 支持 `IN_PROGRESS -> IN_PROGRESS` CAS；当前 production service 无 recovery owner/call path |
| retention period | guard `retention_until <= created_at + 90 days`；独立 business availability ceiling = `NOT_AVAILABLE` |
| physical cleanup | guard row DELETE = `NOT_AVAILABLE`；`dh_decision_output` retention = `NOT_PRESENT` |
| relationship | COMPLETED guard 的 `(tenant_id, result_id)` FK 指向 decision output |

Legacy recovery 不新增第三种 legacy table。会阻断 recovery v2 的是同一 legacy idempotency row：
在可证明 recovery window 内不能删除或忽略；又因为 environment 不可信，不能把旧 result 自动复用给
DEV 或 TEST。

### 3.4 Transaction、cleanup 与 wiring

| 项目 | 当前事实 |
| --- | --- |
| guard transaction | `GuardTransactionBoundary.required` → Spring `TransactionTemplate` |
| isolation | datasource/default；显式 isolation = `NOT_AVAILABLE` |
| timeout | 显式 transaction timeout = `NOT_AVAILABLE` |
| cleanup production caller | `NOT_AVAILABLE`；只有 bean wiring 和 tests |
| scheduler/startup/API | `NOT_AVAILABLE` / `NOT_AVAILABLE` / `NOT_AVAILABLE` |
| cleanup audit | structured retirement audit = `NOT_AVAILABLE` |
| cleanup rollback | 外层 required transaction 异常时 rollback，已有 PostgreSQL test |
| concurrent workers | `FOR UPDATE SKIP LOCKED`，已有 two-worker test |

`DecisionPipelineWiringConfig` 不装配 guard cleanup；`DecisionDryRunRuntimeWiringConfig` 只暴露
`GuardCleanupPort` bean。没有 scheduler、startup invocation 或 endpoint。

## 4. Identity 类型边界

| Identity family | 当前 durable namespace | legacy blocking row | 不得混淆的字段 |
| --- | --- | --- | --- |
| rate bucket identity | `dh_qdr7_rate_limit_bucket` composite PK | matching live legacy rate bucket | `request_count` 和 `window_start` 不是 idempotency/replay key |
| idempotency admission identity | `dh_qdr7_idempotency_guard` unique identity | any non-retired matching legacy guard row | `request_hash` 是 conflict proof，不是 unique identity |
| business result/recovery identity | same guard row + decision output FK | same matching legacy guard row；无独立 legacy row | result reference/checksum 与 lease CAS 不是 admission namespace |
| request fingerprint | `QDR7-DRYRUN-CJSON-1` hash | `LEGACY ROW: NOT APPLICABLE` | 不含 environment/requestId；不能充当 namespace |
| nonce/replay identity | `dh_nq_replay_nonce.replay_key` | 本任务不关闭；独立 legacy replay blocker | 不得与 persistent guard identity 合并 |

## 5. Legacy row 状态分类

~~~text
LIVE:
rate window 尚未结束；或 guard 为 RECEIVED/IN_PROGRESS；或 COMPLETED/FAILED 仍在 retention/recovery window；
或存在有效 lease、可恢复结果、未决 commit outcome。

EXPIRED_BUT_BLOCKING:
rate window 已结束但尚未满足 physical cleanup；或 guard 已为 EXPIRED；或业务 TTL 已过但
unique/exact lookup 仍受 row 影响。

RETIREMENT_ELIGIBLE:
old writer 已证明停止；全部 hard lifetime ceiling 已证明；DB-time cutoff 已跨过；当前 row lock 下
再次证明无 active lease、无 valid recovery result、无 unresolved commit outcome。

PHYSICALLY_RETIRED:
同一 required transaction 内完成 candidate lock、最终资格复核、structured audit insert 与 T1 DELETE，
事务提交成功。

UNKNOWN:
缺失/非法 timestamp、未知状态、unbounded lifetime、malformed row、old-writer/in-flight/commit outcome
无法证明。
~~~

`UNKNOWN` 和 `EXPIRED` 都不得自动等价为可删除。只有 committed structured audit 才能证明
`PHYSICALLY_RETIRED`；普通日志或 affected-row count 不足以证明。

## 6. Legacy option 决策

### 6.1 Selected L2

~~~text
SELECTED LEGACY PERSISTENT IDENTITY OPTION:
L2

NORMAL TRANSITION:
LIVE -> EXPIRED_BUT_BLOCKING -> RETIREMENT_ELIGIBLE -> PHYSICALLY_RETIRED

UNKNOWN/UNPROVABLE FALLBACK:
BLOCK RETIREMENT
BLOCK UNSAFE V2 ADMISSION
~~~

L2 保留旧 quota、duplicate 与 recovery 安全语义，同时提供可终止路径。v2 admission 只有在同一
transaction 中锁定并检查 legacy namespace、确认 absent 或 `PHYSICALLY_RETIRED` 后才允许写入。
当前 hard ceiling 未闭合，因此 L2 只完成方向冻结，不能实施。

### 6.2 Rejected options

- L1 永久阻断可作为 UNKNOWN fallback，但会把 EXPIRED tombstone 变成永久 availability failure，
  不能作为正常迁移终态。
- L3 忽略 legacy namespace 会重置 rate quota、允许旧 requestId 重复执行并丢失 recovery 语义。
- L4 缺少 signed structured historical environment；deployment profile、tenant/source 或默认 DEV
  均不是可信 migration source。

## 7. Retirement cutoff

### 7.1 Rate

~~~text
RATE_LEGACY_RETIREMENT_CUTOFF:
UNRESOLVED

required exact rule:
DB transaction time > row.window_end
AND old-writer cutover is committed
AND DB transaction time > old-writer fence time + maximum in-flight guard transaction lifetime
AND locked candidate still belongs to the same legacy identity and is outside every live quota window

DB clock-skew allowance:
0 / lifecycle comparison uses the same PostgreSQL transaction clock

maximum in-flight guard transaction lifetime:
NOT_AVAILABLE
~~~

3600 秒只证明单 bucket window ceiling，不证明旧 transaction 最迟何时提交。不能用 cleanup
`safetyGrace <= 1 day` 代替未定义的 transaction timeout。

### 7.2 Idempotency

~~~text
IDEMPOTENCY_LEGACY_RETIREMENT_CUTOFF:
UNRESOLVED

proven bounds:
admission TTL <= 7 days
terminal retention <= 90 days
production configured lease <= 900 seconds
port transition lease <= 3600 seconds

required exact rule:
DB transaction time > retention_until
AND lease is absent or expired under the locked row
AND old-writer cutover/in-flight bound is crossed
AND commit-unknown reconciliation bound is crossed
AND no valid result/retry/recovery contract remains

commit-unknown reconciliation hard maximum:
NOT_AVAILABLE
~~~

### 7.3 Recovery

~~~text
RECOVERY_LEGACY_RETIREMENT_CUTOFF:
UNRESOLVED

required exact rule:
all idempotency retirement conditions
AND no recoverable COMPLETED result remains
AND no expired-lease recovery may still claim the row
AND business result availability/repeatability window is crossed

guard retention hard maximum:
90 days

independent recovery/business result availability hard maximum:
NOT_AVAILABLE
~~~

禁止统一拍脑袋使用 90 天。`retention_until` 只能证明 guard 配置上限，不能自动证明
commit-unknown reconciliation 和独立 result availability 已结束。

~~~text
LEGACY_PERSISTENT_IDENTITY_RETIREMENT:
BLOCKED
~~~

## 8. EXPIRED tombstone retirement

~~~text
SELECTED TOMBSTONE RETIREMENT:
T1 / PHYSICAL DELETE
~~~

选择 T1，因为：

- T2 会新增 archive copy 和双表原子搬迁，但本需求只需要低敏感结构化 retirement evidence；
- T3 即使新增 `RETIRED` 状态，现有 unique identity 仍覆盖 row，不能解除 namespace 阻断；
- T1 配合独立 structured audit row，可以最小化继续承载业务 namespace 的历史数据。

未来 T1 必须在一个 required transaction 中完成：

1. tenant + identity-family scoped stable candidate query；
2. `FOR UPDATE SKIP LOCKED`；
3. 最终 cutoff/status/lease/result/commit-outcome 复核；
4. 插入一条 immutable structured retirement audit；
5. DELETE 精确 legacy row；
6. audit 或 delete 任一步失败则整体 rollback；
7. affected row 与 audit row 必须一一对应，不允许 partial delete 或假成功。

## 9. Retirement audit

~~~text
RETIREMENT AUDIT STORAGE:
INDEPENDENT STRUCTURED STORAGE

candidate schema object:
dh_qdr9_legacy_identity_retirement_audit

existing dh_decision_audit_event reuse:
REJECTED
~~~

现有 audit table 的 `decision_id`、event lifecycle 和 V17 new-write environment contract 面向 native
decision producer；legacy retirement 没有可信 legacy environment，也不一定有 decision ID。强行复用会
引入伪造 decision/environment 语义。因此未来 migration 必须建立独立低敏感 audit storage，至少包含：

~~~text
audit_id
tenant_id
identity_family
legacy_identity_hash
previous_status
expiry_at
cutoff_at
retirement_reason
retired_at
batch_run_id
outcome
created_at
~~~

不保存 raw request、canonical bytes、credential、HMAC secret、Authorization、完整 payload、lease token
或 legacy deployment environment 的可信映射。`legacy_identity_hash` 使用独立 versioned canonical
low-sensitivity reference；不能从 hash 反推或日志打印 raw identity。

## 10. Cleanup 与并发合同

~~~text
default:
DISABLED

visibility:
INTERNAL ONLY

tenant scope:
ONE EXACT TENANT PER INVOCATION

family scope:
RATE OR IDEMPOTENCY/RECOVERY / ONE FAMILY PER INVOCATION

batch:
1..1000 / ONE TRANSACTION PER BATCH / NO UNBOUNDED LOOP

ordering:
RATE window_end, tenant_id, window_start
IDEMPOTENCY least(expires_at, retention_until), guard_id

locks:
FOR UPDATE SKIP LOCKED OR PROVEN POSTGRESQL EQUIVALENT

retry:
NO AUTOMATIC RETRY

scheduler / startup / API:
NO / NO / NO
~~~

每个候选必须在锁内重新验证 cutoff、current state、active lease、valid recovery result 和
commit-unknown outcome；任何 unknown 都跳过并记录 blocked outcome，不得 delete。

并发结果冻结如下：

| Race | Required result |
| --- | --- |
| cleanup vs same-environment duplicate | duplicate 在 locked legacy row 可复用期间继续 block；retirement commit 后才允许 v2 unique admission |
| cleanup vs cross-environment v2 | legacy cutoff 前 DEV/TEST 都 block；retirement commit 后两个 verified v2 namespace 可独立 admission |
| cleanup vs legacy recovery read | valid recovery read/lock 先取得时 cleanup skip/block；cleanup 先锁且证明 cutoff 时 recovery 不得返回已退役结果 |
| cleanup vs EXPIRED transition | state/version predicate 只允许一个 winner；另一个重新读取并不得重复 audit |
| two cleanup workers | `SKIP LOCKED` + exact delete + audit unique `(identity_family, legacy_identity_hash)` 保证一次 retirement |

不得使用 global synchronized、static map 或 sleep coordination。新 v2 row 必须通过 identity version
与 verified environment 结构化区分，legacy cleanup 只允许命中 legacy namespace。

## 11. Migration preflight 与判定

~~~text
highest migration:
V15

V16:
ABSENT / RESERVED FOR qdr_reference_liveness

V17:
ABSENT / RESERVED FOR audit environment structured storage

V18:
ABSENT

LEGACY IDENTITY MIGRATION REQUIRED:
YES

selected ordering:
V16 -> V17 -> V18 LEGACY IDENTITY CANDIDATE

V18 artifact path:
NOT_AVAILABLE / SEPARATE SEQUENCING TASK REQUIRED
~~~

Migration 必须提供的 schema 语义已经冻结：

- versioned persistent rate/idempotency/recovery namespace 与 verified DEV/TEST environment；
- legacy row 保持 unknown environment，不 backfill、不默认、不复制到两个环境；
- v2 unique key 与 legacy unique key 可区分，T1 retirement 后旧 row 不再阻断；
- `dh_qdr9_legacy_identity_retirement_audit` 独立 immutable structured evidence；
- retirement candidate、cutoff 与 audit 所需 bounded indexes/constraints/comments；
- V1–V17 checksum 不变，clean/upgrade/rollback safety 与 PostgreSQL tests。

具体使用新 v2 表还是兼容列/partial indexes，以及最终 migration 文件名，必须在 lifetime blocker
关闭后的 migration sequencing task 中一次性冻结。当前不得伪造 V18 文件路径，也不得静默占用版本。

## 12. Frozen test matrix

### Rate

- legacy active rate row blocks DEV/TEST v2；
- legacy expired but within cutoff blocks v2；
- retirement-eligible row T1 + audit 原子提交；
- DEV/TEST 使用独立 v2 buckets；
- same-environment fixed-window semantics 不变。

### Idempotency

- legacy active/completed/EXPIRED row 在各自 cutoff 前 block；
- retired tombstone 不再 block v2；
- DEV/TEST 同 requestId 产生独立 v2 row；
- same-environment duplicate reuse/conflict 不变；
- different hash 不能通过新 namespace 绕过 conflict。

### Recovery

- legacy recoverable result 保持到 proven cutoff；
- cutoff 前 retirement rejected；
- cutoff 后 result-unavailable/commit-known proof + T1 成功；
- 不跨 DEV/TEST reuse；
- 无独立 legacy recovery row 的 fixture 不创建额外 table。

### Unknown

missing expiry、invalid status、unbounded TTL、malformed row、commit outcome unknown：

~~~text
BLOCK RETIREMENT
BLOCK UNSAFE V2 ADMISSION
~~~

### Cleanup、concurrency 与 rollback

- default disabled、tenant/family scoped、batch bounded、stable order；
- 16 DEV + 16 TEST requests；
- cleanup racing duplicate/recovery/EXPIRED transition；
- two workers 不重复 audit；
- delete failure rollback audit；audit failure rollback delete；
- no rate/idempotency bypass；no recovery loss。

### Architecture

- no default/inferred environment；
- no legacy row mapped DEV/TEST；
- no physical delete without cutoff proof and structured audit；
- no V16/V17/V18 artifact in this task；
- no registry/retention/endpoint/scheduler/automatic learning。

## 13. Readiness

~~~text
LEGACY_PERSISTENT_IDENTITY_DESIGN:
BLOCKED

LEGACY_IDENTITY_OPTION:
L2

RATE_RETIREMENT_CUTOFF:
UNRESOLVED

IDEMPOTENCY_RETIREMENT_CUTOFF:
UNRESOLVED

RECOVERY_RETIREMENT_CUTOFF:
UNRESOLVED

TOMBSTONE_RETIREMENT:
T1

CLEANUP_CONTRACT:
FROZEN

RETIREMENT_AUDIT:
FROZEN / INDEPENDENT STRUCTURED STORAGE / NOT IMPLEMENTED

MIGRATION_REQUIRED:
YES

MIGRATION_SEQUENCE:
ORDER FROZEN / ARTIFACT SCOPE BLOCKED

CROSS_MODULE_SCOPE:
PASS

EFFECTIVE_SCOPE_INVARIANTS:
66 / 66 PASS

ALLOW_LEGACY_IDENTITY_IMPLEMENTATION:
NO

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_B4_MILESTONE_REVIEW_RETRY / ALLOW_B4_PUBLICATION / ALLOW_B5_IMPLEMENTATION:
NO / NO / NO

PRODUCTION_CAPACITY:
NOT_PROVEN
~~~

## 14. Next action

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-LIFETIME-BLOCKER
~~~

该任务只允许证明并冻结 old-writer/in-flight transaction、commit-unknown reconciliation 与 independent
recovery availability hard ceilings。三个 cutoff 全部 accepted 前，不得启动 migration sequencing 或
legacy identity implementation。Legacy replay blocker 继续 OPEN。
