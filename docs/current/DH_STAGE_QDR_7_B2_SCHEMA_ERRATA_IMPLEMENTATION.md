# DH Stage-QDR-7 B2 Schema Errata Implementation

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION`
> mode: `CODE_CHANGE / TRANSACTIONAL_FLYWAY_CALLBACK / POSTGRESQL_TESTCONTAINERS`
> status: `DONE / REVIEW_PENDING`
> milestone acceptance: `NOT_YET`

## 1. 实施结果

旧`beforeMigrate__qdr7_v13_compatibility.sql`已删除；唯一callback为`beforeEachMigrate__qdr7_v13_compatibility.sql`。callback先读取`flyway_schema_history`：V12未成功、V13成功或V14成功时不访问`dh_qdr7_idempotency_guard`；仅成功V12且无V13记录时进入兼容路径。

兼容路径在history-only no-op gate后立即仅为当前migration事务设置：

```sql
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
```

然后才验证V12表结构和`chk_dh_qdr7_idempotency_state_fields`的schema/table/type/validated/`pg_get_constraintdef`指纹，并检查FAILED error code、trim后长度和repair总数。超过1000行立即fail-closed，不部分修复。repair以`guard_id`排序并限制1000行；只trim符合条件的FAILED `stable_error_code`。随后以同名temporary CHECK衔接V13的FAILED/EXPIRED terminal timestamp转换；V13建立最终CHECK。callback、temporary CHECK、repair、V13 DDL/DML和history成功写入同一PostgreSQL事务，任一失败整体rollback。

V14未改动。它既有的overflow precheck在`result_type`超过32字符时失败，保留原值和`varchar(64)`；修正为32字符后可重试。

## 2. 验证证据

`V13TransactionalCompatibilityCallbackFlywayPostgresTest`在真实Testcontainers PostgreSQL 17.10运行11个测试，0 failures、0 errors、0 skipped，耗时132.912秒。覆盖fresh/V11/V12升级、V12 fingerprint、blank/ceiling fail-closed、V13 injected failure、5秒lock timeout、60秒statement timeout、retry、V13/V14 completed no-op lock trap及V14 no-truncation retry。

Windows Surefire manifest-JAR绝对路径根冲突仅以本次命令的`-Dsurefire.useManifestOnlyJar=false`规避；没有修改POM、Surefire配置或全局JVM设置。

完整命令也已实际通过：`mvn -ntp -pl dh-app -am test`（`dh-app` 146 tests，0 failures/errors/skipped）、`mvn -ntp test`（158 reports / 1075 tests / 0 failures / 0 errors / 0 skipped）和`mvn -ntp -Pquality validate`（reactor 19/19、根项目 Checkstyle 0 violations、Spotless PASS）。

## 3. 边界与下一步

未修改V1–V14、Java生产代码、API/Controller/DTO/OpenAPI、HMAC、nonce、source合同或NQ。未运行capacity benchmark，未进入B3，未接外部HTTP、Provider、Agent/LangGraph或LIVE；未push、未创建tag。

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata implementation: DONE / REVIEW_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY
```
