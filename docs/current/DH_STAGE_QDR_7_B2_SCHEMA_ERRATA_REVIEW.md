# DH Stage-QDR-7 B2 Schema Errata Review

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-REVIEW`
> mode: `REVIEW_ONLY / SCHEMA_ERRATA_DECISION`
> decision: `PASS / TRANSACTIONAL_CALLBACK_SELECTED`
> implementation: `NOT STARTED`

## 1. 结论

根因已确认：V12 `chk_dh_qdr7_idempotency_state_fields`要求`FAILED.completed_at`非空，V13却在删除旧约束前先执行`failed_at = completed_at, completed_at = null`，PostgreSQL因此以`SQLSTATE 23514`拒绝升级。

唯一获准路线是Option A，但不得沿用现有`beforeMigrate`事件。实现必须将兼容逻辑改为`beforeEachMigrate`，使临时DDL、bounded trim修复与V13处于同一数据库事务。真实Flyway 11.7.2 + PostgreSQL 17.10实验确认：

- `beforeMigrate`在V13失败后会留下已提交的约束删除和数据修改，拒绝继续使用。
- `beforeEachMigrate`在migration失败时会同时回滚临时DDL和数据修改；schema history保持V12且不留下`success=false`行。
- 修复故障条件后可安全重试并按V12→V13→V14完成。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_REVIEW: PASS
SELECTED_REMEDIATION: TRANSACTIONAL_CALLBACK / beforeEachMigrate
ALLOW_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MILESTONE_REVIEW_RETRY_3_NOW: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Flyway与PostgreSQL实况

```text
Flyway: 11.7.2
production location: classpath:db/migration
validateOnMigrate: true
outOfOrder: false
group: false
mixed: false
default executeInTransaction: true
PostgreSQL: 17.10
```

项目只显式配置`enabled=true`和`classpath:db/migration`；其余值由实际`Flyway.getConfiguration()`读取。当前SQL callback被发现为`beforeMigrate - qdr7 v13 compatibility`。PostgreSQL DDL可在事务内回滚，但当前`group=false`时，`beforeMigrate`不与单个V13共用事务；`beforeEachMigrate`与其对应migration共用事务。

## 3. 方案裁定

### Option A：选择，但强制改为`beforeEachMigrate`

`PASS / DESIGN_FROZEN`。未来implementation允许删除旧`beforeMigrate__qdr7_v13_compatibility.sql`并创建加固后的`beforeEachMigrate__qdr7_v13_compatibility.sql`。这属于callback事件迁移，不是版本化migration变更。

精确执行顺序：

1. 先读取`flyway_schema_history`；V13或V14成功时立即return，不访问业务表。
2. V12未成功或目标表不存在时立即return。
3. 校验旧CHECK名称、所属表和`pg_get_constraintdef` fingerprint；不匹配则fail-closed。
4. 检查NULL/blank FAILED error code、V14 overflow和repair ceiling；全部检查在任何写入前完成。
5. 设置transaction-local `lock_timeout`和`statement_timeout`。
6. 删除旧strict CHECK，并立即建立V13可按同名删除的最小临时CHECK；禁止已提交的无约束状态。
7. 按`guard_id`稳定排序，只trim目标FAILED行；固定单批和最大1000行。该值复用既有cleanup hard ceiling，只是migration安全上限，不是容量默认值。
8. V13删除临时同名CHECK、转换typed timestamps并建立最终CHECK。
9. 任一步失败时，callback DDL、trim、V13 DML/DDL和history写入全部rollback；修正原因后安全重试。
10. V14随后执行自身overflow guard与类型收窄。

### Option B：拒绝

真实Flyway 11.7.2、`validateOnMigrate=true`、`outOfOrder=false`实验中，已完成V14数据库新增`V12.1`后，`validate()`与`migrate()`均抛`FlywayValidateException`。启用全局`outOfOrder`或放宽validation违反冻结策略。

### Option C：拒绝

V13已提交并由当前PostgreSQL tests实际执行。修改会改变checksum并破坏V1–V14 immutability；本review无rewrite授权。

### Option D：拒绝

纯DML pre-deploy repair不能解除旧CHECK阻断。若脚本提交临时弱CHECK，脚本提交到Flyway执行之间存在弱约束窗口，并引入漏执行、权限漂移和双轨审计风险；若要求与Flyway共享事务，则实质退化为Option A。

### Option E：拒绝

A已有安全路线，不允许删除Flyway history、重建数据库或建立新schema规避问题。

## 4. Legacy数据、安全与部署规则

- padded FAILED：仅trim `stable_error_code`；不得改变identity、hash、state/version、lease、result reference或lifecycle timestamps。
- NULL/blank FAILED：在任何DDL/DML前失败；禁止默认错误码。
- 超过1000条需修复行：写入前失败，转受控离线数据治理。
- `result_type`超过32字符：V14前失败，不修改原值、不截断。
- V13/V14 completed：history gate必须在业务表访问前return。
- 只允许V12成功且V13未成功窗口获取目标表DDL lock；必须显式设置transaction-local lock/statement timeout。
- 部署前记录schema history、旧CHECK fingerprint、待修复数量和overflow计数；不需要也不得启用`outOfOrder`、`group`或`mixed`。

## 5. Implementation允许范围

```text
删除 beforeMigrate__qdr7_v13_compatibility.sql
新增 beforeEachMigrate__qdr7_v13_compatibility.sql
加固callback history/fingerprint/precheck/timeout/bounded repair
补充Flyway PostgreSQL tests与architecture/source guards
同步本任务current docs
```

禁止修改V1–V14、Java生产代码、API/OpenAPI、HMAC/nonce/source合同或容量默认值。

## 6. 必须测试矩阵

| baseline | 必须证明 |
| --- | --- |
| fresh | V1→V14成功；兼容分支只在V13前命中 |
| V11 | V12→V13→V14顺序不变 |
| V12 clean | 0 repair，V13/V14成功 |
| V12 padded FAILED | 只trim目标行；第二次0 repair |
| V12 blank/NULL FAILED | DDL/DML前失败；CHECK、原值、history不变 |
| repair > ceiling | 写入前失败；无部分修复 |
| V13 deterministic failure | callback与V13全部rollback；history保持V12 |
| failed V13 retry | 修正原因后安全完成V13/V14 |
| V13 completed | 无业务表DDL/DML/scan |
| V14 completed | 重复启动无目标表DDL/DML/scan |
| V14 33-char overflow | 原类型/原值/history保持，修正后重试 |
| constraint fingerprint drift | 写入前fail-closed |

## 7. Review证据与最终裁定

- 仓库focused suite：16 tests，0 failures/errors/skipped，PostgreSQL 17.10，V1→V14成功。
- `beforeMigrate`实验：失败后CHECK缺失且trim已提交。
- `beforeEachMigrate`实验：失败后CHECK存在、原`' PADDED '`保持、history停在前一版本且无failed row。
- selected experiment：第一次V13失败完整rollback；修正后retry至V14；blank行在DDL前失败。
- interstitial experiment：已完成V14环境对新增V12.1的validate/migrate均失败。

```text
ROOT_CAUSE: CONFIRMED
SELECTED_REMEDIATION: TRANSACTIONAL_CALLBACK / beforeEachMigrate
TRANSACTION_ROLLBACK_SAFETY: PASS
MIGRATION_IMMUTABILITY: PASS
LEGACY_V12_UPGRADE_PATH: FROZEN
COMPLETED_V13_V14_SAFETY: PASS / IMPLEMENTATION_TEST_REQUIRED
FAILED_MIGRATION_RETRY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS
```

下一任务仅允许：`DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION`。
