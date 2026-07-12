# DH Stage-QDR-7 B2 Persistent Guards Blocker Fix Retry

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY`
> classification: `CODE_CHANGE + P1_SECURITY_BLOCKER_FIX_RETRY + PRE_V13_COMPATIBILITY_FIX + V14_FORWARD_MIGRATION + JDBC_RESULT_REFERENCE_EVIDENCE + SPRING_TRANSACTION_WIRING_PROOF + CONCURRENT_CLEANUP_PROOF + COMMIT_UNKNOWN_PROOF`
> status: `DONE / REVIEW_PENDING`
> baseline: `e3fd401`
> HEAD before fix: `865257a04bbd0f7030f2e6c8bce090f1480d790b`
> next action: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2`

## 1. 结论与范围

本轮关闭上轮P1-1至P1-5的实现和真实证据缺口，不修改V1-V13、API、Controller、DTO、OpenAPI、HMAC、nonce、source合同或NQ。previous milestone review保持`BLOCKED / HISTORICAL_PRESERVED`；本文件不把B2标为accepted，也不授权capacity acceptance或B3。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_BLOCKER_FIX_RETRY: DONE / REVIEW_PENDING
PRE_V13_COMPATIBILITY: PASS
V14_FORWARD_MIGRATION: PASS
V1_V13_IMMUTABILITY: PASS
RESULT_TYPE_SCHEMA_ALIGNMENT: PASS
DB_CLOCK_ISOLATION_EVIDENCE: PASS
RESULT_REFERENCE_JDBC_EVIDENCE: PASS
PRODUCTION_COMPLETION_ATOMICITY: PASS
IDEMPOTENCY_COMMIT_UNKNOWN_EVIDENCE: PASS
CONCURRENT_CLEANUP_EVIDENCE: PASS
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_2: YES
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Compatibility callback与V14

`beforeMigrate__qdr7_v13_compatibility.sql`由Flyway SQL callback实际发现和执行。它只在`dh_qdr7_idempotency_guard`存在且schema history未成功记录V13时工作：仅对`FAILED.stable_error_code`执行`btrim`，trim后为空即以SQLSTATE `23514`中止，不填默认值、不改变identity、request hash、state或result reference。callback临时替换V12旧CHECK，避免该CHECK在V13迁移`FAILED/EXPIRED.completed_at`前阻断合法行更新；V13随即建立其最终严格CHECK。空库、V1-V11和已完成V13的路径为no-op。

`V14__qdr7_persistent_guard_schema_alignment.sql`先拒绝非空且长度大于32的`result_type`，再将字段收窄至冻结的`varchar(32)`并更新COMMENT。它是forward-only schema alignment：无删表/列、无截断、无V1-V13变更。

真实Flyway测试覆盖fresh V1→V14、padded FAILED V12→V14、V13→V14、blank legacy error拒绝、33字符result type拒绝及V1-V13 checksum map不变。

## 3. Lifecycle、result与事务

- `PersistentGuardLifecycleClockArchitectureTest`禁止persistent guard source使用JVM absolute clock或absolute lifecycle command输入。
- 真实Spring/JDBC probe以caller `+48h/-48h`证明rate window、TTL、retention、lease、heartbeat和cleanup资格仍由PostgreSQL `transaction_timestamp()`决定。
- completed duplicate先验证固定`DH_DECISION_OUTPUT` type；missing、wrong tenant、wrong type、checksum mismatch和unreadable result都返回`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`，无raw prompt、provider response或执行材料。
- production-equivalent wiring证明`JdbcTemplate`、`DataSourceTransactionManager`和`GuardTransactionBoundary`共享同一DataSource/connection transaction；output、audit、checksum validation和terminal CAS任一失败都rollback，不留output/audit/COMPLETED。
- test-only Connection代理在真实delegate commit后抛出异常；idempotency映射为`IDEMPOTENCY_COMMIT_UNKNOWN`，exact reconcile前不宣告成功、不会自动readmit或重放业务。
- 两个独立JDBC cleanup worker验证`FOR UPDATE SKIP LOCKED`、active lease/retention保护、CAS miss不计成功、worker rollback后安全重试，identity tombstone不物理删除。

## 4. 验证

```text
targeted PostgreSQL suite: 23 tests / 0 failures / 0 errors / 0 skipped
owning modules: 135 tests / 0 failures / 0 errors / 0 skipped
full Maven XML: 157 reports / 1064 tests / 0 failures / 0 errors / 0 skipped
PostgreSQL/Testcontainers: PostgreSQL 17.10 / EXECUTED
quality: 19/19 reactor / Checkstyle 0 / Spotless PASS
capacity benchmark: NOT RUN / NOT ALLOWED
```

## 5. 边界与风险

未修改NQ、V1-V13、API/Controller/DTO/OpenAPI、HMAC/nonce/source或交易执行链；未新增第三张业务结果表；未接HTTP、Provider、NQ runtime、Agent或LangGraph；未进入B3，未开启Paper/LIVE，未push/tag。

剩余非P1风险为callback发现/执行顺序、历史数据质量、V14收窄时的锁与超长拒绝、DB clock依赖、tombstone增长、result retention、commit outcome uncertainty、cleanup竞争、PostgreSQL可用性和Maven wrapper。它们必须由下一次独立milestone review复核，不能用本实现自证接受。
