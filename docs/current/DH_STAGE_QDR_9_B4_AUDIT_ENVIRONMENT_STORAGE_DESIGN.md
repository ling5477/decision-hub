# DH Stage-QDR-9 B4 Audit Environment Structured Storage Design

## 1. 决策状态

~~~text
task:
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-SCOPE-DESIGN

baseline:
9249bf78a2eaace4c59aedff788e37e083b72b3b

classification:
DOCUMENTATION / SECURITY_DESIGN / AUDIT_ENVIRONMENT_STRUCTURED_STORAGE

current technical tree:
B3 SAFE BASELINE

SELECTED_AUDIT_ENVIRONMENT_STORAGE:
OPTION A / NULLABLE STRUCTURED COLUMN + STAGED NEW-WRITE ENFORCEMENT

SELECTED_AUDIT_BACKFILL:
B1 / NULL MEANS UNKNOWN LEGACY

SELECTED_MIGRATION_SEQUENCE:
S2 / V16 REFERENCE-LIVENESS REGISTRY, V17 AUDIT ENVIRONMENT

AUDIT_MIGRATION_CANDIDATE:
V17__qdr9_audit_environment_storage.sql / NOT CREATED

EFFECTIVE_SCOPE_INVARIANTS:
60 / 60 PASS

ALLOW_AUDIT_ENVIRONMENT_MIGRATION_IMPLEMENTATION:
NO / BLOCKED BY V16 SEQUENCE AND PRODUCER CUTOVER PREREQUISITES
~~~

本文件只冻结 AUDIT environment 的结构化存储、历史兼容、读写、事务、索引、migration
sequence 和测试合同。它不创建 V16/V17，不修改 Java、测试、POM、配置或 workflow，也不实施
persistent identity、replay namespace、upstream signed environment、registry 或 retention。

## 2. 代码现实矩阵

| 项目 | 当前事实 |
| --- | --- |
| audit table | `dh_decision_audit_event` |
| primary key | `id varchar(192)` |
| tenant | `tenant_id varchar(128) not null` |
| environment | `NOT_AVAILABLE` |
| event type | `event_type varchar(64) not null` |
| event status | `event_status varchar(32) not null`；只允许 `SUCCESS` / `FAILED`，不是 lifecycle |
| payload | `event_json jsonb not null default '{}'::jsonb`；禁止作为 environment source |
| decision ID | `decision_id varchar(128) not null` |
| trace ID | `trace_id varchar(128) not null` |
| request ID | `NOT_AVAILABLE` 作为结构化列；部分 producer 仅在 payload 中保存 |
| source | `NOT_AVAILABLE` 作为结构化列；部分 producer 仅在 payload 中保存 |
| occurred timestamp | `NOT_AVAILABLE` |
| created timestamp | `created_at timestamptz not null default now()` |
| write port | `DecisionAuditRepository.saveAuditEvent` |
| write adapter | `JdbcDecisionAuditRepository` |
| read port | `DecisionReplayQueryRepository` |
| read adapter | `JdbcDecisionReplayQueryRepository` |
| compatibility validator | `JdbcFeedbackReferenceValidationAdapter` 只做 tenant-bound target existence |
| transaction owner | repository 本身不创建事务；由各 producer 的外层 boundary 决定 |
| current indexes | PK `id`；`decision_id`；`(tenant_id, created_at)`；`trace_id` |

`AuditEventRecord` 当前字段为 `id`、`decisionId`、`tenantId`、`traceId`、`eventType`、
`eventStatus`、`eventJson`、`errorCode`、`createdAt`，没有 environment，也没有
environmentless compatibility overload。当前生产构造位置共七处，位于六个文件：

~~~text
dh-app/src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketCommandService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/DefaultQdrModelGatewayIntegrationService.java
~~~

这些当前调用点全部归类为：

~~~text
LEGACY AUDIT PRODUCER / NOT TRUSTED FOR LIFECYCLE
~~~

原因不是其业务事件无效，而是当前记录没有 verified `FeedbackEnvironment`。未来不新增独立的
environmentless production record；每个仍需保留的 producer 必须先取得同一 verified
`FeedbackExecutionScope`，否则在 V17 cutover 后不得写入 AUDIT。

## 3. Storage options

### 3.1 Option A — existing table structured column

选择。未来 V17 为 `dh_decision_audit_event` 增加：

~~~sql
environment varchar(16)
~~~

历史行保持 `NULL`。未来受信写入显式保存 `DEV` 或 `TEST`。该字段只表达 audit evidence 的
environment scope，不表达 lifecycle；`event_status` 继续只表达事件写入结果。

优点：

- environment 与 native audit row 同一物理 insert，不需要补写或 join；
- `QDR7_RATE_LIMIT_ADMISSION` 的 rate identity、audit environment 和 native audit row 可在同一
  `GuardTransactionBoundary.required` 中原子提交；
- tenant/environment 查询可以使用直接复合索引；
- 历史 `NULL` 可显式保留为 `UNKNOWN LEGACY`，不需要伪造 backfill。

约束：

- V17 应用前必须证明旧 environmentless writer 已全部停止；
- `AuditEventRecord` canonical constructor 必须先在同一发布单元中要求非空
  `FeedbackEnvironment`，且所有 production/test caller 已迁移；
- legacy row 保持 immutable；任何更新都会受到新约束重新检查。

### 3.2 Option B — separate binding table

拒绝。独立 binding table 可以保留 native row，但会增加 native/binding 双写、FK、unique、join 和
partial-write 风险。当前唯一需要的事实是 native audit row 的结构化 environment，而不是独立生命周期；
lifecycle 仍由 V16 `qdr_reference_liveness` 负责。额外 binding table 不能在数据库中反向保证每个受信
native audit row 都有 binding，仍需应用合同，故没有抵消复杂度。

Option B registry 架构与本决策不冲突：V16 registry 保存 lifecycle；V17 native column 保存 audit
evidence scope。二者职责不同。

### 3.3 Option C — event_json / event_status / log

永久拒绝。JSON、status 或日志均不是可约束的结构化 identity：

- `event_status` 已冻结为 `SUCCESS` / `FAILED`，不得改写为 lifecycle 或 environment；
- `event_json` 无法提供 tenant/environment leading index、closed enum 或非空证明；
- 日志不是事务内持久化事实源，无法支持隔离、FK、rollback 或一致读。

## 4. Conservative historical backfill

~~~text
SELECTED_AUDIT_BACKFILL:
B1 / KEEP NULL

legacy representation:
environment = NULL

read meaning:
UNKNOWN LEGACY / ABSENT
~~~

V17 不执行任何 `UPDATE dh_decision_audit_event SET environment = ...`。当前没有已签名、已结构化且可验证
的历史 environment source，因此 B2 的可证明 backfill 结果只能是零行；选择 B1 可以更直接地冻结
“不回填”事实。

永久禁止：

- 默认 `DEV` 或 `TEST`；
- 按 tenant、deployment profile、source、decision ID、trace ID 或 request ID 推断；
- 从 `event_json`、`event_status`、错误码或日志猜测；
- 把 historical null 当作当前 runtime environment；
- 让 legacy row 参与 environment-bound lifecycle 放行或 retention eligibility。

## 5. V17 constraint contract

V17 的 exact staged constraint 为：

~~~text
column:
environment varchar(16) null

value constraint:
chk_dh_decision_audit_event_environment
CHECK (environment IS NULL OR environment IN ('DEV', 'TEST'))

new-write constraint:
chk_dh_decision_audit_event_environment_required
CHECK (environment IS NOT NULL) NOT VALID
~~~

PostgreSQL `NOT VALID` 只跳过对既有行的全表验证，仍约束新增或更新行。因此它可以保留 historical
`NULL`，同时拒绝 cutover 后的新 null row。该合同要求在 migration apply 前停止全部旧 writer；
不得在 rolling deployment 中让旧 binary 继续写入。若无法证明 old-writer stop，V17 implementation
必须停止，而不是删除 required constraint 或临时允许 null。

不得设置 column default，不得执行 `ALTER COLUMN SET NOT NULL`，也不得验证 required constraint，
除非未来独立任务能够证明所有历史 row 都有可信 environment；当前没有该证明。

## 6. Write and transaction contract

未来受信 `AuditEventRecord` canonical constructor 必须包含非空 `FeedbackEnvironment environment`。
不得保留 null overload、default `DEV` overload、deprecated-but-production-reachable overload 或
test-only environmentless helper。

~~~text
environment source:
verified FeedbackExecutionScope.environment()

tenant:
non-blank and equal to executionScope.tenantId()

environment:
DEV or TEST only

native audit write:
same transaction as producer action
~~~

`QDR7_RATE_LIMIT_ADMISSION` 的唯一来源为：

~~~text
command.executionScope().environment()
~~~

创建 rate v2 identity 与 rate audit record 必须使用同一个 `FeedbackExecutionScope` object。禁止从
identity string 反解析、从 `DecisionDryRunGuardProperties`/Spring profile 获取、从 tenant/source
推断或默认。

QDR7 transaction outcome：

~~~text
rate admission success + audit success -> commit
environment missing/invalid -> no rate admission and no audit
audit insert failure -> rate admission rollback
rate admission failure -> no audit commit
commit outcome unknown -> fail-closed / no automatic replay
~~~

V16 registry integration 未来存在时，rate admission、native audit row、environment column 与
`ACTIVE` registry row 必须继续位于同一 DH PostgreSQL transaction。当前任务不创建 registry。

## 7. Read compatibility and isolation

新增 internal-only audit query contract；不新增 Controller、REST endpoint 或 OpenAPI。environment-bound
query 必须显式携带 tenant + `FeedbackEnvironment`，且 SQL 只返回 `environment = ?` 的 row。

至少支持：

~~~text
tenant_id + environment + event_type
tenant_id + environment + decision_id
tenant_id + environment + trace_id
tenant_id + environment + created_at range
~~~

结果排序冻结为：

~~~text
created_at DESC, id DESC
~~~

单次查询必须有界，默认 50、hard max 100；禁止 tenantless、environmentless 或跨环境结果集合。

现有 K4 replay query 不具备 environment selector，故未来只允许作为 legacy compatibility projection：

~~~text
WHERE tenant_id = ? AND decision_id = ? AND environment IS NULL
~~~

它不得读取 V17 新 row，不得把 null 映射成 DEV/TEST，也不得按当前 runtime environment 补值。其
`DecisionReplayAuditEventView` 继续以 environment absent 表达 `UNKNOWN LEGACY`。新 row 由独立
environment-bound internal audit query 返回明确 `FeedbackEnvironment`。

## 8. Exact indexes

V17 必须新增以下非唯一索引，列顺序固定：

~~~text
idx_dh_decision_audit_event_tenant_env_type_created_id
(tenant_id, environment, event_type, created_at DESC, id DESC)

idx_dh_decision_audit_event_tenant_env_decision_created_id
(tenant_id, environment, decision_id, created_at DESC, id DESC)

idx_dh_decision_audit_event_tenant_env_trace_created_id
(tenant_id, environment, trace_id, created_at DESC, id DESC)

idx_dh_decision_audit_event_tenant_env_created_id
(tenant_id, environment, created_at DESC, id DESC)
~~~

现有 PK 与 legacy indexes 保留。不得创建 environment-only 全局索引、tenantless query index、
event-json expression index、跨环境 unique 或改变现有 `id` PK。

## 9. Migration sequencing

~~~text
SELECTED MIGRATION SEQUENCE:
S2

V16:
V16__qdr9_reference_liveness_state_model.sql
CANDIDATE / NOT CREATED / RESERVED

V17:
V17__qdr9_audit_environment_storage.sql
CANDIDATE / NOT CREATED
~~~

S1 被拒绝：registry lifecycle 与 native audit environment 是两个独立职责，没有必须同一 migration
原子上线的证据；合并会扩大已冻结 V16、耦合 rollback 和测试。

S3 被拒绝：它会把已冻结的 V16 registry 语义顺延，且没有不可规避的安全依赖支持重排。

V17 implementation 必须以 V16 已存在并通过 clean/upgrade/checksum/index tests 为前置。V16 仍未获
实施授权，因此：

~~~text
AUDIT MIGRATION IMPLEMENTATION:
BLOCKED BY SEQUENCE
~~~

persistent identity version storage 和 legacy tombstone retirement 仍由后续独立 blocker 冻结；
不得把它们静默并入 V17。

## 10. Frozen test matrix

### Migration

- empty DB 依次应用 V1→V16→V17；
- V16 predecessor 升级到 V17；
- V1–V15 checksum 不变，V16/V17 各应用一次；
- column、CHECK、NOT VALID required constraint 与四个 exact indexes 存在；
- historical row 数量和内容不变；
- V17 在 V16 不存在时不得作为获授权的 implementation 路径。

### Backfill

- 不可证明的历史 row 继续为 null；
- 无 default DEV/TEST；
- 无 event-json、status、tenant、profile 或 source inference；
- zero-row deterministic backfill 为预期。

### New writes and rate audit

- DEV/TEST audit 分别持久化 DEV/TEST；
- missing、invalid、PROD、LIVE environment 在 JDBC 前拒绝；
- accepted 与 rate-limited `QDR7_RATE_LIMIT_ADMISSION` 保存 exact environment；
- audit failure rollback rate admission；rate/native failure 不留下 partial audit；
- canonical constructor 与全部 production/test caller 均无 environmentless invocation。

### Reads and concurrency

- new DEV/TEST row 只在 matching tenant/environment query 中可见；
- legacy row 只在 explicit legacy projection 中以 absent/UNKNOWN 读取；
- null 不映射为 DEV/TEST；
- concurrent DEV/TEST writes 不交叉；
- transaction failure 不留下 partial row；
- 同一 business identity 的 DEV/TEST evidence 保持独立，物理 `id` PK 仍全局唯一。

### Architecture

- environment 不进入 `event_json`、`event_status` 或日志；
- 无 default DEV、async binding、afterCommit 补写或 catch-and-log；
- 当前任务无 V16/V17、registry、retention、API、scheduler 或 automatic learning artifact。

## 11. Readiness

~~~text
AUDIT_ENVIRONMENT_STORAGE_DESIGN:
DONE

AUDIT_STORAGE_OPTION:
A

AUDIT_BACKFILL:
B1

MIGRATION_SEQUENCE:
S2

AUDIT_MIGRATION_VERSION:
V17 FROZEN / NOT CREATED

AUDIT_WRITE_CONTRACT:
FROZEN

AUDIT_READ_CONTRACT:
FROZEN

COMPATIBILITY_CONSTRUCTOR:
REMOVE

TRANSACTION_ATOMICITY:
FROZEN

ALLOW_AUDIT_ENVIRONMENT_MIGRATION_IMPLEMENTATION:
NO

ALLOW_IDENTITY_REPLAY_IMPLEMENTATION:
NO

ALLOW_V16_IMPLEMENTATION:
NO

ALLOW_B4_MILESTONE_REVIEW_RETRY / ALLOW_B4_PUBLICATION / ALLOW_B5_IMPLEMENTATION:
NO / NO / NO
~~~

## 12. Next action

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-BLOCKER
~~~

该下一任务仍不得创建 V16/V17；它必须先关闭 legacy persistent idempotency tombstone 与 atomic
legacy-check + v2 admission blocker。legacy replay blocker 继续 OPEN。
