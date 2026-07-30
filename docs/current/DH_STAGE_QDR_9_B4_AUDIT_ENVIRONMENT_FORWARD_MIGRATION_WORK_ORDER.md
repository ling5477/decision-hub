# DH Stage-QDR-9 B4 Audit Environment Forward-Migration Work Order

## 1. 工单状态

~~~text
task:
DH-STAGE-QDR-9-B4-AUDIT-ENVIRONMENT-FORWARD-MIGRATION-IMPLEMENTATION

status:
BLOCKED / NOT AUTHORIZED

design:
DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_STORAGE_DESIGN.md

scope:
DH_STAGE_QDR_9_B4_AUDIT_ENVIRONMENT_SCOPE_ERRATUM.md

candidate migration:
V17__qdr9_audit_environment_storage.sql

required predecessor:
V16__qdr9_reference_liveness_state_model.sql

ALLOW_IMPLEMENTATION:
NO
~~~

本工单是未来实施入口，不是当前授权。V16 未创建、producer signed environment implementation 已撤销、
legacy persistent identity 和 replay blocker 仍开放，因此不得创建 V17 或修改任何实现文件。

## 2. Frozen contracts

~~~text
STORAGE=OPTION A
BACKFILL=B1
SEQUENCE=S2
ENVIRONMENT_SOURCE=FeedbackExecutionScope.environment()
LEGACY_ROW=NULL / UNKNOWN LEGACY
NEW_ROW=NON-NULL DEV OR TEST
COMPATIBILITY_CONSTRUCTOR=REMOVE
READ_ORDER=created_at DESC, id DESC
QUERY_LIMIT=DEFAULT 50 / HARD MAX 100
~~~

`event_status`、`event_json` 和日志都不是 environment source。V17 不修改 V1–V15，不回填历史
environment，不创建 registry/lifecycle/retention，也不加入 API 或 scheduler。

## 3. Future implementation sequence

### P0 — predecessor and cutover proof

1. 证明 V16 已创建并通过独立 migration acceptance；
2. 证明 upstream signed `FeedbackExecutionScope` 已实现并通过 security review；
3. 证明全部 environmentless AUDIT writer 已停止，rolling deployment 不会继续写 null；
4. 重新枚举 migration，确认 V17 未被占用。

任一条件不满足即停止。

### M1 — V17 schema

- 新增 nullable `environment varchar(16)`；
- 新增 value CHECK；
- 新增 `CHECK (environment IS NOT NULL) NOT VALID`；
- 不设置 default、不执行 backfill、不执行 `SET NOT NULL`；
- 新增四个 tenant/environment-leading exact indexes；
- 保留 V1–V16 checksum 与历史 row。

### W1 — trusted write contract

- `AuditEventRecord` canonical constructor 新增 non-null `FeedbackEnvironment`；
- 删除所有 environmentless overload/helper；
- `JdbcDecisionAuditRepository` 显式写 environment；
- 全部 production/test caller 显式传 verified environment；
- 不新增 legacy environmentless record type。

### R1 — internal read contract

- 新增 tenant/environment-bound internal audit query；
- 默认 limit 50、hard max 100、稳定倒序；
- 现有 replay audit projection 只读 `environment IS NULL` legacy row；
- legacy null 保持 absent/UNKNOWN，不映射为 DEV/TEST；
- 不新增 Controller、endpoint 或 OpenAPI。

### T1 — transaction and rate audit

- QDR7 rate identity 与 rate audit 使用同一个 `FeedbackExecutionScope`；
- rate admission、native audit insert 和未来 registry `ACTIVE` row 同一 required transaction；
- 任一失败整体 rollback；
- 无 afterCommit、async event、best-effort 或 retry。

### V — verification

依次执行 migration presence、PostgreSQL clean/upgrade/backfill、repository、read adapter、rate audit、
transaction rollback、concurrency、architecture、full regression 和 quality。任何 P1/P2 未关闭时不得
启动 B4 review retry。

## 4. Exact schema contract

~~~text
table:
dh_decision_audit_event

column:
environment varchar(16) null

value check:
chk_dh_decision_audit_event_environment

new-write check:
chk_dh_decision_audit_event_environment_required / NOT VALID

indexes:
idx_dh_decision_audit_event_tenant_env_type_created_id
idx_dh_decision_audit_event_tenant_env_decision_created_id
idx_dh_decision_audit_event_tenant_env_trace_created_id
idx_dh_decision_audit_event_tenant_env_created_id
~~~

所有索引列顺序以 storage design 为准。不得增加跨环境 unique、environment-only index 或 JSON
expression index。

## 5. Stop conditions

出现以下任一情况立即停止：

- V16 未存在或未通过 acceptance；
- V17 版本已被占用；
- 需要修改 V1–V16；
- 需要默认或推断历史 environment；
- 旧 writer 仍可能写 null；
- 需要保留 environmentless constructor/helper；
- 需要 environment 写入 JSON/status/log；
- 无法保证 rate admission/audit/registry 同事务；
- 需要 scope erratum 之外的 Java/test/POM/config/workflow；
- 需要 registry、retention、B4 review/publication、B5、API、scheduler 或 automatic learning；
- 需要真实 HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE。

## 6. Frozen test matrix

### Migration

~~~text
V1 -> V16 -> V17 clean
V16 -> V17 upgrade
V1-V15 checksums preserved
V16 checksum preserved
V17 applied once
column/check/index comments and definitions exact
historical rows preserved
~~~

### Backfill and constraints

~~~text
legacy null remains null
no DEV/TEST default
no JSON/status/profile inference
new null rejected
DEV/TEST accepted
unknown/PROD/LIVE rejected
~~~

### Writes and atomicity

~~~text
DEV audit persists DEV
TEST audit persists TEST
missing/invalid rejected before JDBC
environment storage failure rolls back native producer action
native audit failure rolls back environment write
rate-limited and accepted events preserve exact environment
concurrent DEV/TEST writes do not leak
~~~

### Reads

~~~text
tenant+environment+event_type
tenant+environment+decision_id
tenant+environment+trace_id
tenant+environment+created_at
stable created_at DESC,id DESC
default 50 / hard max 100
legacy null explicit absent/UNKNOWN
no cross-environment result
~~~

### Architecture

~~~text
production environmentless constructor count = 0
test environmentless helper count = 0
no event_json environment
no event_status repurpose
no async binding
no migration artifact before authorization
no registry/retention/API/scheduler/automatic learning
~~~

## 7. Rollback

V17 为 forward-only migration。发布后不得删除、重写或改名；schema correction 使用后续新 migration。
业务回滚只能停用新 wiring 或普通 `git revert` 代码提交，不能 destructive down migration。由于 historical
null 保留，required constraint 不能直接 validate 或转为 column `NOT NULL`。

## 8. Current boundary

~~~text
code / test / POM change:
NO / NO / NO

migration creation:
NO

V16 / V17:
NOT CREATED / NOT CREATED

registry / retention:
NOT IMPLEMENTED / NOT PRESENT

B4 review retry / publication / B5:
NO / NO / NO

push / tag:
NO / NO
~~~

## 9. Next action

~~~text
DH-STAGE-QDR-9-B4-LEGACY-PERSISTENT-IDENTITY-BLOCKER
~~~
