# DH Stage-QDR-9 B4 Persistent Identity and Replay Namespace Design

## 1. 决策状态

~~~text
task:
DH-STAGE-QDR-9-B4-UPSTREAM-CONTRACT-IDENTITY-AND-REPLAY-NAMESPACE-SCOPE-DESIGN

baseline:
b0ff11e4057077ad7e0fe91d691116f069dc744e

classification:
DOCUMENTATION / SECURITY_DESIGN / SCOPE_FREEZE

published and reverted implementation:
549ed5a3224ce3ce375452dcf57629c73e3101d0

current technical tree:
B3 SAFE BASELINE

technical P1:
1 / OPEN

technical P2:
1 / OPEN

governance P1:
RECORDED

PERSISTENT_IDENTITY_CONTRACT:
FROZEN / NOT IMPLEMENTED

LEGACY_PERSISTENT_IDENTITY:
OPTION B / CONTRACT BLOCKED

RATE_AUDIT_ENVIRONMENT:
FROZEN / NOT IMPLEMENTED

AUDIT_ENVIRONMENT_STORAGE:
FORWARD MIGRATION REQUIRED

REPLAY_NAMESPACE:
FROZEN / NOT IMPLEMENTED

LEGACY_REPLAY_NAMESPACE:
OPTION B / CONTRACT BLOCKED

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO
~~~

本设计只冻结 technical P1/P2 的未来合同与精确 scope，不实施修复。`549ed5a…`
曾发布并由 ordinary revert 隔离的历史保持不变；其 exact-SHA CI 只证明回归通过，不关闭安全审查。

## 2. 审计事实

### 2.1 Persistent rate identity

| 项目 | 当前事实 |
| --- | --- |
| entry method | `PersistentDecisionDryRunRateLimiter.check(source, tenantId, route, ignoredNow, requestId, traceId)` |
| current identity fields | `DecisionDryRunGuardProperties.environment`、固定 endpoint、source、tenantId；数据库另以 `window_start` 标识窗口 |
| canonicalization | `PersistentGuardIdentity` 做精确字符串校验；没有 identity version 或 canonical byte codec |
| storage key/columns | `dh_qdr7_rate_limit_bucket` PK = `(environment, endpoint, source, tenant_id, window_start)` |
| tenant binding | YES |
| source binding | YES |
| environment binding | 仅绑定部署配置字符串；未绑定 verified signed `FeedbackEnvironment` |
| TTL/window | DB `transaction_timestamp()` fixed window；1..3600 秒 hard ceiling |
| concurrency | conditional upsert + composite PK + row lock；单 bucket 精确 winner |
| audit | admission 与 `QDR7_RATE_LIMIT_ADMISSION` 在同一 `GuardTransactionBoundary.required` 中 |
| affected tests | `PersistentDecisionDryRunRateLimiterTest`、V12 PostgreSQL migration test、四个 QDR7 PostgreSQL fixture |

当前 `environment` 列存在不等于安全 finding 已解决。它来自
`DecisionDryRunGuardProperties.environment()`，不是请求签名覆盖并由
`command.executionScope().environment()` 提供的强类型环境。

### 2.2 Persistent idempotency and guard identity

| 项目 | 当前事实 |
| --- | --- |
| entry method | `PersistentGuardedDecisionDryRunService.execute` → `executePersistent` |
| current identity fields | deployment environment、endpoint、source、tenantId、requestId；requestHash 作为冲突与 exact-read 条件 |
| canonicalization | `DecisionDryRunRequestFingerprint` 使用 `QDR7-DRYRUN-CJSON-1` + deterministic JSON；不含 environment |
| storage key/columns | unique `(environment, endpoint, source, tenant_id, request_id)` |
| tenant binding | YES |
| source binding | YES |
| environment binding | 仅 deployment environment；不是 verified request environment |
| TTL | duplicate TTL ≤ 7 days；terminal retention ≤ 90 days |
| cleanup | eligible row 转为 `EXPIRED` tombstone；当前不物理删除 tombstone |
| concurrency | insert-on-conflict + exact reread；state/version/lease CAS |
| audit | admission 是独立短事务；首次业务执行、lease、result/audit、terminal transition 在另一个 required transaction |
| affected tests | guard service、fingerprint、V12/V13/V14、四个 QDR7 PostgreSQL fixture |

`requestHash` 不属于数据库 unique identity；同 key + 不同 hash 保持
`IDEMPOTENCY_CONFLICT`，不得通过把 hash 纳入 row key 来规避冲突。

### 2.3 Recovery identity

| 项目 | 当前事实 |
| --- | --- |
| entry | `JdbcIdempotencyGuardAdapter.recover` |
| locator | persistent identity + requestId + requestHash |
| CAS predicates | `state=IN_PROGRESS`、stateVersion、expired lease |
| environment | 继承当前 deployment-environment identity |
| transaction | 调用方 `GuardTransactionBoundary.required` |
| cleanup/lifetime | 与 idempotency row 相同；`EXPIRED` tombstone 不物理删除 |

state/version/lease 是恢复 CAS 条件，不是可以跨请求复用的 namespace 字段。

### 2.4 Request fingerprint

当前 fingerprint domain 为 `QDR7-DRYRUN-CJSON-1`，包含 tenant、source、schema、dryRun、
forbiddenCapabilities 与 decision context，不含 requestId、traceId、timestamp、nonce、signature、
transport metadata，也不含 environment。它是内容一致性证明，不是 rate、idempotency 或 replay key。

### 2.5 Replay/nonce identity

| 项目 | 当前事实 |
| --- | --- |
| tuple | tenantId、source、path、nonce、requestId |
| method/operation | HMAC material 包含 method；replay key 不包含 method |
| environment | NOT_AVAILABLE |
| canonical version | NOT_AVAILABLE |
| encoding | 未转义 `::` 字符串拼接 |
| backend | `InMemoryNonceReplayGuard` 或共享 `dh_nq_replay_nonce` 的 `JdbcNonceReplayGuard` |
| database key limit | `replay_key varchar(512)` |
| TTL | authenticator 传入 `now + 2 × maxClockSkew`；默认 600 秒 |
| cleanup | in-memory 每次登记前清理；JDBC 可配置为每次登记前惰性清理；无 scheduler |
| maximum legacy lifetime | NOT_AVAILABLE；`maxClockSkew` 没有全局 hard ceiling |
| same-environment replay | 同 key 返回 `NONCE_REPLAY` |
| cross-environment collision | 当前 environment 不在 key 中，故发生碰撞 |

JDBC cleanup 关闭时，过期 row 仍会因主键冲突阻断；当前 port 也没有
“检查未过期 legacy key + 原子登记 v2 key”的能力。

### 2.6 Rate audit

| 项目 | 当前事实 |
| --- | --- |
| event constructor | `PersistentDecisionDryRunRateLimiter.writeAudit` |
| event type | `QDR7_RATE_LIMIT_ADMISSION` |
| current environment source | NOT_AVAILABLE；仅 rate identity 持有 deployment environment |
| persistence | `DecisionPersistenceRecords.AuditEventRecord` → `JdbcDecisionAuditRepository` |
| structured column | `dh_decision_audit_event` 没有 environment column |
| generic JSON | `event_json jsonb`；本设计禁止用它保存 environment |
| compatibility constructor | 当前树不存在；`549ed5a…` 曾新增无 environment overload 并写 null |
| transaction | rate admission 与 rate audit 同一 required transaction |
| fail behavior | audit exception 使 transaction rollback，调用方 fail-closed |

## 3. Canonical persistent identity

### 3.1 版本

~~~text
RATE_IDENTITY_VERSION:
QDR9-RATE-IDENTITY-2

IDEMPOTENCY_IDENTITY_VERSION:
QDR9-IDEMPOTENCY-IDENTITY-2

RECOVERY_IDENTITY_VERSION:
QDR9-RECOVERY-IDENTITY-2
~~~

三个 namespace 共享一个 codec，但不共享 domain/version label。代码事实只证明它们共享
`PersistentGuardIdentity` 的 environment/endpoint/source/tenant 基础字段；rate 另有 window，
idempotency 另有 requestId，recovery 另有 requestHash 与 CAS 条件，因此禁止把三者折叠为同一个 key。

### 3.2 Encoding

~~~text
PERSISTENT_IDENTITY_CANONICAL_ENCODING:
QDR9-LP1
~~~

`QDR9-LP1` 的输入和输出合同：

1. 所有字段先按各自强类型合同校验，不 trim、不 lowercase、不使用 enum/hashCode。
2. environment 使用 `FeedbackEnvironment.name()`，仅为 `DEV` 或 `TEST`。
3. 每个字段按 UTF-8 编码为 bytes。
4. frame 以 ASCII domain/version 行开始；随后每个字段按固定顺序写入
   `<decimal-byte-length>:<raw-utf8-bytes>\n`。
5. 字段名和顺序属于 version contract；不使用 JSON、对象序列化或未转义分隔符拼接。
6. 持久化必须结构化保存 identity version 与 verified environment；只在 Java 对象中携带不合格。

### 3.3 字段顺序

~~~text
RATE:
version, environment, endpoint, source, tenantId
storage-window-suffix: windowStart

IDEMPOTENCY:
version, environment, endpoint, source, tenantId, requestId
conflict-proof: requestHash + hashVersion

RECOVERY:
version, environment, endpoint, source, tenantId, requestId, requestHash
CAS-only: state, stateVersion, leaseOwner, leaseToken, leaseExpiresAt
~~~

`requestHash` 继续决定 same-key/same-hash reuse 与 same-key/different-hash conflict；不得把不同 hash
变成两个可并存 row。Recovery 的 CAS-only 字段不进入 durable namespace。

### 3.4 DEV/TEST isolation

~~~text
same tenant/source/request + DEV
!=
same tenant/source/request + TEST

DEV quota does not consume TEST quota
TEST quota does not consume DEV quota

DEV idempotency row does not conflict with TEST row
TEST idempotency row does not reuse DEV result

same-environment duplicate semantics remain unchanged
~~~

verified environment 的唯一来源是 `command.executionScope().environment()`；deployment profile、
guard property、tenant mapping、source mapping 或默认 `DEV` 均不得参与 canonical identity。

## 4. Legacy persistent identity

~~~text
SELECTED LEGACY PERSISTENT IDENTITY OPTION:
B / CONSERVATIVE LEGACY BLOCKING

LEGACY_PERSISTENT_IDENTITY_CONTRACT:
BLOCKED
~~~

当前表虽有 `environment`，但该值只证明部署配置，不证明请求的 signed `FeedbackEnvironment`。因此全部现有 row
均为 legacy namespace，不能按 DEV/TEST backfill。

未来 v2 写入必须使用独立 version + verified environment namespace，并在写入前保守检查：

- active legacy rate bucket 匹配 endpoint/source/tenant/current DB window：fail-closed，直到 window 结束；
- legacy idempotency row 匹配 endpoint/source/tenant/requestId：fail-closed，不复用其 result；
- legacy check 必须忽略旧 deployment environment，禁止借此推断 DEV/TEST；
- legacy 检查与 v2 admission 必须由 PostgreSQL 原子合同保护，不能先查后写。

Option A 被拒绝，因为忽略 legacy idempotency row 会允许同 requestId 的历史结果重复执行，并重置旧的
rate consumption。Option C 被拒绝，因为 current row 没有可信历史字段证明 signed environment。

阻断原因：rate window 有 3600 秒 hard ceiling，但 idempotency cleanup 只转为永久 `EXPIRED`
tombstone，当前不存在安全物理删除。兼容窗口结束条件只能是未来 migration/retirement 合同显式标识
legacy row、证明不再有旧 writer，并经受审查的 bounded cleanup 移除或隔离全部 legacy tombstone；
不得按时间、tenant、profile 或 deployment environment 自动结束。

## 5. Rate audit environment

~~~text
RATE_AUDIT_ENVIRONMENT_SOURCE:
command.executionScope().environment()

AUDIT_EVENT_COMPATIBILITY_CONSTRUCTOR_POLICY:
REMOVE

AUDIT_ENVIRONMENT_STORAGE:
FORWARD MIGRATION REQUIRED
~~~

`QDR7_RATE_LIMIT_ADMISSION` 必须使用创建 rate v2 identity 的同一个 verified environment object。
Controller 完成 signature、timestamp、nonce、source、tenant/source/environment authorization 后创建
`FeedbackExecutionScope`，再把 scope 同时传给 rate admission、rate audit 和 dry-run command。

禁止从 identity string 反解析、从 `DecisionDryRunGuardProperties`/Spring profile 推断、从 tenant/source
映射或使用 compatibility constructor 默认环境。

`549ed5a…` 的无 environment overload 会向 environment 参数传 null，并把 environment 塞入
`event_json`。未来不得恢复该设计。所有 production `AuditEventRecord` 构造点必须显式传入强类型 environment；
旧 overload 删除。确需无 environment 的 test-only fixture 必须改为显式 fail-fast helper，不能存在于
production source。

当前 `dh_decision_audit_event` 只有通用 `event_json`，没有可信结构化 environment column，故必须新增
forward migration。迁移版本与 exact migration path 本轮不选择；V16 已被 reference-liveness 设计占用为
candidate 且未创建，下一任务必须先冻结 migration sequencing。

## 6. Versioned replay namespace

~~~text
REPLAY_NAMESPACE_VERSION:
QDR9-DRYRUN-REPLAY-2

REPLAY_CANONICAL_ENCODING:
QDR9-LP1

REPLAY_STORAGE_KEY:
qdr9-dryrun-replay-v2:<lowercase-sha256-hex(QDR9-LP1-frame)>
~~~

canonical frame 固定字段顺序：

~~~text
version
environment
tenantId
source
method
path
nonce
requestId
~~~

method 取 HMAC material 的 uppercase canonical method，path 使用 canonical endpoint。环境来自同一个
verified `FeedbackExecutionScope`。现有 tenant/source/path/nonce/requestId 字段全部保留，并新增 version、
environment、method。hash key 总长固定且小于现有 `varchar(512)`；canonical bytes 不持久化、不记录日志。

安全顺序：

1. missing/invalid environment 在 root fail-closed；
2. environment 属于 HMAC canonical material；
3. environment tamper 先得到 `SIGNATURE_INVALID`；
4. signature 与 tenant/source/environment authorization 通过后才进入 replay admission；
5. 同 environment 同 tuple 第二次返回 `NONCE_REPLAY`；
6. DEV 与 TEST 使用不同 v2 key。

## 7. Legacy replay

~~~text
SELECTED LEGACY REPLAY OPTION:
B / CONSERVATIVE LEGACY BLOCKING

LEGACY_REPLAY_NAMESPACE_CONTRACT:
BLOCKED
~~~

未来 admission 必须对当前 legacy tuple 计算旧 key；未过期 legacy key 存在时返回
`NONCE_REPLAY`，缺失或已过期时才原子登记 v2 key。不得忽略 legacy key，不得默认 DEV，也不得把同一 key
复制到两个环境后声明完成迁移。

Option A 被拒绝，因为 rollout 后忽略未过期 legacy key 会缩短旧防重放窗口。Option C 被拒绝，因为 legacy row
只有 opaque `replay_key` 和 expiry，没有可信 environment。

当前实际：

~~~text
TTL:
2 × maxClockSkew / DEFAULT 600 SECONDS

database expiry:
dh_nq_replay_nonce.expires_at

cleanup frequency:
REQUEST-DRIVEN LAZY DELETE WHEN ENABLED / NO SCHEDULER

maximum legacy lifetime:
NOT_AVAILABLE
~~~

阻断原因：

- `maxClockSkew` 没有全局 hard ceiling；
- JDBC cleanup 可配置关闭；
- 当前 `NonceReplayGuard` 只有 `markIfAbsent`，不能原子执行未过期 legacy check + v2 insert；
- rolling deployment 期间旧 binary 仍可能写 legacy key。

兼容窗口结束必须同时证明：旧 writer 已全部停止；以 DB transaction time 捕获的全部 legacy
`expires_at` 已跨过；v2 admission 全量生效；过期判断不依赖 row 物理删除。上述 proof 与 max-clock-skew
ceiling 未在独立 blocker 中冻结前，不允许 replay implementation。

## 8. Transaction and concurrency contract

当前事务事实：

- rate admission 与 rate audit：同一个 required transaction；audit 失败回滚 rate counter；
- idempotency admission 与 admission audit：同一个独立短 required transaction；
- first business execution：lease、delegate 内部 DB write、terminal transition 和 guard audit 在第二个
  required transaction；
- completed duplicate result read：单独 required transaction；
- isolation level：`TransactionTemplate` 未显式设置，使用 transaction manager/default datasource isolation，
  具体级别为 NOT_AVAILABLE；
- external IO：`GuardTransactionBoundary` 合同禁止。

未来必须保持以上原子性并新增：

- rate v2 identity creation 与 rate audit 使用同一个 `FeedbackExecutionScope`；
- legacy check + v2 rate/idempotency/replay admission 必须原子；
- store/audit/commit 不确定继续 fail-closed，不自动重放业务执行；
- 不使用 global synchronized、static Map 或 sleep 做 PostgreSQL 并发协调。

## 9. Frozen test matrix

### 9.1 Rate and idempotency

- DEV 消耗 quota 后 TEST quota 不变；反向亦同；
- same environment 保持 existing fixed-window semantics；
- same tenant/source/requestId 的 DEV/TEST 不产生 `IDEMPOTENCY_CONFLICT`；
- same environment same request 保持 reuse/conflict semantics；
- 16 DEV + 16 TEST 同 business identity 得到两个独立 namespace；
- 16 same-environment duplicate requests 保持一个 deterministic winner/result。

### 9.2 Audit

- DEV/TEST admission audit 分别持久化结构化 DEV/TEST；
- accepted 与 rate-limited audit 均使用同一 verified environment；
- compatibility constructor 在 production 不可达且最终删除；
- audit write failure 回滚 rate decision；
- audit count 与 environment 精确匹配并发结果。

### 9.3 Replay

- 同 tuple DEV 第一次 accepted；
- 同 tuple TEST 第一次按冻结 legacy 策略决定，legacy window 结束后 accepted；
- same environment 第二次 `NONCE_REPLAY`；
- DEV signature 改为 TEST、TEST signature 改为 DEV 均先 `SIGNATURE_INVALID`；
- unexpired legacy key 按 Option B fail-closed；
- expired legacy key 进入 v2；
- 无 default DEV migration。

### 9.4 Mandatory PostgreSQL fixtures

必须保持原断言强度并增加 environment-bound 场景：

~~~text
PersistentGuardProductionWiringPostgresTest
DecisionDryRunSamePoolRecoveryPostgresTest
DecisionDryRunActualWiringRepeatabilityPostgresTest
Qdr7CapacityAcceptanceIT
~~~

### 9.5 Architecture guards

禁止 environmentless production identity、environmentless production `AuditEventRecord`、
default DEV、无 version/environment 的 replay key、V16、registry、retention、新 endpoint、
scheduler 与 automatic learning。

## 10. Migration preflight and readiness

~~~text
highest migration:
V15

V16 present:
NO

FORWARD_MIGRATION_REQUIRED:
YES
~~~

Forward migration 至少需要解决：

- audit structured environment storage；
- persistent identity version 与 verified environment 的 legacy/v2 可区分存储；
- legacy tombstone 的安全识别与 retirement 合同。

本任务不选择 migration version 或 path，不创建 migration，也不复用 candidate V16。下一任务必须先冻结
migration sequencing、upgrade path、nullable legacy rows、new-write constraints、rollback 与 PostgreSQL tests。

~~~text
IDENTITY_REPLAY_NAMESPACE_DESIGN:
DONE / BLOCKERS RECORDED

DEV_TEST_ISOLATION:
PASS / DESIGN

EFFECTIVE_SCOPE_INVARIANTS:
54 / 54 PASS

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_B4_MILESTONE_REVIEW_RETRY:
NO

ALLOW_B4_PUBLICATION:
NO

ALLOW_B5_IMPLEMENTATION:
NO
~~~

## 11. Next action

~~~text
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN
~~~

该任务还必须携带 persistent identity version storage sequencing，并保留 legacy replay blocker。migration
scope 接受后仍不得直接 implementation；legacy replay TTL/atomic admission 合同须在独立 blocker 中关闭。
