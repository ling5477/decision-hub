# DH Stage-QDR-9 B4 Reference-Liveness State-Model Design

## 1. 冻结结论

本设计处理 B4 milestone review 的两个 P1：`RELEASED` reference 快照不能证明 target 当前已安全失活，且 AUDIT、REPLAY、EVALUATION target 没有可信的 environment-bound lifecycle。

```text
Stage-QDR-9 B4: IMPLEMENTED LOCALLY / MILESTONE REVIEW BLOCKED
review blocker: STAGE_QDR_9_B4_REFERENCE_STATE_MODEL_UNRESOLVED
P1 findings: 2
selected option: B / UNIFIED_REFERENCE_LIVENESS_REGISTRY
future migration: V16__qdr9_reference_liveness_state_model.sql
future migration state: CANDIDATE / NOT_CREATED
B4 retention: DEFAULT DISABLED
B4 publication: NOT_ALLOWED
B5: NOT_ALLOWED
```

本文件只冻结未来实现合同和范围，不创建 migration、不修改 Java 或测试，也不重新运行 B4 milestone review。

## 2. 代码现实矩阵

| Target type | 当前 owner / source module | 当前 source table | canonical target key | tenant source | environment source | 当前 lifecycle source | 当前状态 | transition owner | reactivation | missing / unknown |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AUDIT | `DecisionAuditRepository` / `JdbcDecisionAuditRepository` | `dh_decision_audit_event` | `audit:<id>` | `tenant_id` | `NOT_AVAILABLE` | `NOT_AVAILABLE` | `event_status=SUCCESS|FAILED`，只是事件处理结果 | `NOT_AVAILABLE` | `NOT_AVAILABLE` | fail-closed |
| REPLAY | `ReplayCaseRepository` / `JdbcReplayCaseRepository` | `qdr_replay_case` | `replay-case:<uuid>`、`replay:<caseId>` | `tenant_id` | `NOT_AVAILABLE` | `NOT_AVAILABLE` | 无 lifecycle 字段 | `NOT_AVAILABLE` | `NOT_AVAILABLE` | fail-closed |
| REPLAY snapshot | `CanonicalReplaySnapshotPersistencePort` / `JdbcCanonicalReplaySnapshotRepository` | `qdr_canonical_replay_snapshot` | `canonical-snapshot:<uuid>` | `tenant_id` | `NOT_AVAILABLE` | `NOT_AVAILABLE` | 无 lifecycle 字段 | `NOT_AVAILABLE` | `NOT_AVAILABLE` | fail-closed |
| EVALUATION | `EvaluationCaseRepository` / `JdbcEvaluationCaseRepository` | `qdr_evaluation_case` | `evaluation:<uuid>` | `tenant_id` | `NOT_AVAILABLE` | `NOT_AVAILABLE` | `verdict=PASS|FAIL|WARN|SKIPPED`，只是评估结论 | `NOT_AVAILABLE` | `NOT_AVAILABLE` | fail-closed |

`JdbcFeedbackRetentionAdapter` 当前只对 RELEASED reference 做 tenant-bound existence lookup；`Target` 和 `AuditTarget` 都没有 environment 或 current lifecycle status。B2 的 `JdbcFeedbackReferenceValidationAdapter` 也只验证 tenant-bound native target existence。B2 aggregate write 使用 `REPEATABLE_READ`；B3 historical evidence query 使用 read-only `READ_COMMITTED`，两者均不是 target lifecycle source-of-truth。

下列推断被永久禁止：target exists、`SUCCESS`、`FAILED`、`PASS`、`FAIL`、`WARN`、`SKIPPED`、reference snapshot `RELEASED`、或仅 tenant match 均不得证明 target 当前可安全删除。

## 3. 方案比较与选择

| 方案 | 结论 | 原因 |
| --- | --- | --- |
| A：在旧 target 表追加 environment/lifecycle 列 | REJECTED | 会把 audit event result 与 lifecycle 混合；AUDIT、REPLAY、EVALUATION 的 producer 和历史数据均没有可证明 environment/lifecycle，backfill 会臆造状态，跨模块升级风险最高。 |
| B：统一 `qdr_reference_liveness` registry | SELECTED | 以独立、typed、environment-bound projection 成为唯一 lifecycle source；保留 native target identity lookup，不重解释旧字段，能以缺失 registry fail-closed 处理历史数据。 |
| C：三个独立 lifecycle projection | REJECTED | 语义可行但会复制 lock、version、backfill 和 retention 查询逻辑；现有三类 target 尚无 lifecycle 模型，无法证明其复杂度收益。 |

## 4. 统一 registry 合同

未来 `qdr_reference_liveness` 的一行是 target 当前 liveness 的唯一状态源，不是 audit event、replay result 或 evaluation verdict 的镜像。

| 字段 | 冻结合同 |
| --- | --- |
| `id` | UUID primary key。 |
| `tenant_id` | 非空、trimmed，第一安全边界。 |
| `environment` | 非空，仅 `DEV` 或 `TEST`；必须由 producer 显式传入，不能从 tenant、ID、JSON 或 reference snapshot 推断。 |
| `reference_type` | 仅 `AUDIT`、`REPLAY`、`EVALUATION`。 |
| `canonical_target_key` | 非空、稳定、typed key；接受的 key scheme 为 `audit:`、`replay-case:`、`replay:`、`canonical-snapshot:`、`evaluation:`。 |
| `lifecycle_state` | 仅 `ACTIVE` 或 `SAFE_INACTIVE`；未知或不支持值不得成为可删除状态。 |
| `native_status` | 可空、有界诊断字段；绝不参与 retention eligibility。 |
| `state_version` | 正整数；每次 lifecycle transition（含 reactivation）递增。 |
| `state_changed_at` | 非空的 transition instant。 |
| `created_at` / `updated_at` | 数据库审计时间。 |

唯一约束为 `(tenant_id, environment, reference_type, canonical_target_key)`。必须另有 `(tenant_id, reference_type, canonical_target_key)` 查询索引，仅用于区分 environment mismatch 与 target missing；它不得替代 environment-bound unique key。

Native target identity 在 retention 内仍须以 tenant-bound lookup 复核。registry 或 native target 任一缺失、key 不可规范化、tenant 不一致、environment 仅能从其他 scope 找到、状态不支持、查询失败或超时，都归类为 UNKNOWN 并阻断删除。

## 5. 生命周期、allowlist 与所有权

三类 allowlist 均只包含新显式 lifecycle contract 的 `SAFE_INACTIVE`，且**不**从 native status 映射：

```text
AUDIT_SAFE_INACTIVE_STATUSES       = { SAFE_INACTIVE }
REPLAY_SAFE_INACTIVE_STATUSES      = { SAFE_INACTIVE }
EVALUATION_SAFE_INACTIVE_STATUSES  = { SAFE_INACTIVE }
```

| 操作 | owner | 约束 |
| --- | --- | --- |
| 创建 target liveness | 对应 AUDIT/REPLAY/EVALUATION producer 与 registry writer | 在同一 DH PostgreSQL transaction 写 native target identity 与 `ACTIVE` registry row；显式携带 tenant/environment/type/key。 |
| `ACTIVE → SAFE_INACTIVE` | target-type internal lifecycle command | 仅显式、受校验的内部 command；不得由 retention、scheduler、API、snapshot status 或 verdict 自动触发。 |
| `SAFE_INACTIVE → ACTIVE` | 同一 target-type internal lifecycle command | 必须递增 `state_version`、更新 `state_changed_at`，并与 retention row lock 兼容。 |
| retention | `JdbcFeedbackRetentionAdapter` | 只读检查；不得创建、release、更新或修复 registry state。 |

当前 producer 没有环境生命周期合同。因此 V16 backfill 不能把任何现有 native target 或 `RELEASED` reference 写成 `SAFE_INACTIVE`。新 producer integration 完成前，缺失 registry row 一律使 retention blocked；B4 默认关闭保持不变。

## 6. Reference compatibility 与保守 backfill

现有 `reference_value` 格式保持；未来 parser 以 aggregate 的 `(tenant, environment, reference_type, reference_value)` 联合解析和查询，禁止在 value 内补猜 environment。

```text
malformed legacy value       -> UNKNOWN / BLOCK DELETE
unresolvable legacy key      -> UNKNOWN / BLOCK DELETE
known native target but no registry row -> UNKNOWN / BLOCK DELETE
registry row in other environment -> REFERENCE_SCOPE_MISMATCH / BLOCK DELETE
native target missing        -> REFERENCE_TARGET_MISSING / BLOCK DELETE
```

V16 只允许为环境和状态都能由新 explicit producer contract 证明的新增数据创建 `ACTIVE` row。历史行的 environment 或 lifecycle 不能证明时，不创建可删除 state；不得默认 `SAFE_INACTIVE`、不得自动 release reference、不得改变 V15 retention default-disabled 状态。

## 7. Retention / transition 并发合同

1. retention 在已有 `REPEATABLE_READ` transaction 中锁定 candidate aggregate：`FOR UPDATE SKIP LOCKED`。
2. 对每个非-EVIDENCE reference，先用 full tenant/environment/type/key lookup 读取 registry，并在同一 transaction 使用 `FOR SHARE` 锁定 registry row；缺失时再以 tenant/type/key 检查是否为 environment mismatch。
3. 在 delete 前紧邻的最终检查中，registry lifecycle 必须显式属于对应 `SAFE_INACTIVE` allowlist，`state_version` 与锁定行一致，native target identity 仍 tenant-bound 存在。
4. producer transition 在同一 datasource transaction 对 registry row `FOR UPDATE`；它与 retention 的 `FOR SHARE` 冲突，因此 reactivation 不能越过已开始的 delete safety boundary。
5. 任意锁超时、query timeout、optimistic version miss、native identity mismatch 或 DataAccessException 均 rollback / fail-closed；不写 success delete audit，不自动重试。

这是一套单一 DH PostgreSQL datasource 的一致性合同。它不声称跨数据库原子性；若未来 producer 不共享该 datasource，retention 必须继续 blocked，直到独立一致性设计获批。

## 8. 实施前置与边界

未来 implementation 必须先执行 V16 clean/upgrade 验证、producer integration、registry lock tests 和 B4 P1 regression；在全部通过前：

```text
B4 milestone review retry: NOT_ALLOWED
B4 publication: NOT_ALLOWED
B5 implementation: NOT_ALLOWED
API / scheduler / automatic learning: NOT_ALLOWED
real HTTP / provider / NQ / Agent / LangGraph / Paper / LIVE: NOT_ALLOWED
```
