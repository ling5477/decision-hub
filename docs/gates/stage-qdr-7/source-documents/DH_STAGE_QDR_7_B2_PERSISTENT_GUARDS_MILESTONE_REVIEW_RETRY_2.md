# DH Stage-QDR-7 B2 Persistent Guards Milestone Review Retry-2

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2`
> classification: `REVIEW_ONLY + P1_FIX_RETRY_VERIFICATION + FLYWAY_CALLBACK_SECURITY_REVIEW + FORWARD_MIGRATION_REVIEW + JDBC_TRANSACTION_EVIDENCE_REVIEW + CONCURRENT_CLEANUP_REVIEW + POSTGRESQL_REGRESSION`
> baseline: `865257a`
> target: `8660f4e`
> verdict: `BLOCKED / P1_FIX_REQUIRED`
> next action: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY-2`

## 1. Decision

`8660f4e`的范围、Flyway discovery、PostgreSQL 17.10/Testcontainers、真实JDBC duplicate与idempotency after-commit unknown均已独立验证。但下列P1未关闭，因此B2不得接受：

1. `beforeMigrate__qdr7_v13_compatibility.sql`在V12状态执行表级CHECK删除/重建，且对FAILED行执行无界UPDATE；没有batch、锁预算或migration失败后schema状态的真实断言。
2. V14使用`using result_type::varchar(32)`显式收窄cast。虽然preflight试图拒绝超长值，测试没有断言失败后原33字符值未截断或修正后能够安全retry。
3. `+48h/-48h`测试没有覆盖`completed_at`、`failed_at`、`expired_at`的真实JDBC DB-clock结果。
4. completion rollback测试通过actual Spring beans手工组合output/audit/terminal CAS，但未走`PersistentGuardedDecisionDryRunService.executeFirst`实际完成路径。
5. cleanup并发验证`SKIP LOCKED`和rollback retry，但没有制造真实state/version CAS miss并断言其不计成功。
6. `README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`仍在current-phase块指向旧work order/B1 action，与`STATUS.md`、`WORK_ORDER.md`冲突；本任务不允许修改前三者。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY_2: BLOCKED
PRE_V13_COMPATIBILITY: REJECTED
FLYWAY_CALLBACK_SAFETY: FAIL
V14_FORWARD_MIGRATION: REJECTED
V1_V13_IMMUTABILITY: PASS
RESULT_TYPE_SCHEMA_ALIGNMENT: FAIL
DB_CLOCK_ISOLATION_EVIDENCE: FAIL
RESULT_REFERENCE_JDBC_EVIDENCE: PASS
PRODUCTION_COMPLETION_ATOMICITY: FAIL
IDEMPOTENCY_COMMIT_UNKNOWN_EVIDENCE: PASS
CONCURRENT_CLEANUP_EVIDENCE: FAIL
CAS_MISS_SEMANTICS: FAIL
STORE_FAILURE_FAIL_CLOSED: PASS
PRODUCTION_IN_MEMORY_FALLBACK: ABSENT
API_CONTRACT_UNCHANGED: YES
SECURITY_BOUNDARY: FAIL
POSTGRESQL_TEST_EVIDENCE: PASS / INSUFFICIENT_FOR_P1
B2_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Positive evidence

- `git diff 865257a..8660f4e`只包含P1 callback/V14、直接测试、usecase type guard和允许文档；V1-V13、API/Controller/DTO/OpenAPI、HMAC/nonce/source、NQ和previous review未变。
- 本轮targeted Flyway日志明确显示callback在migration前被发现；fresh路径实际迁移V1…V14，PostgreSQL为17.10。
- actual Spring context/JDBC验证completed duplicate的tenant/type/checksum/unreadable result均fail-closed为`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`。
- after-commit connection proxy确实先调用delegate `commit()`再抛`SQLException`；服务返回`IDEMPOTENCY_COMMIT_UNKNOWN`，exact reconcile得到RECEIVED，retry不重放业务。
- full Maven为157 reports / 1064 tests / 0 failures / 0 errors / 0 skipped；quality为19/19、Checkstyle 0、Spotless PASS。

## 3. Boundary

本review只修改允许的current review文档。未修改Java生产/测试、callback、migration、V1-V14、API、HMAC/nonce/source或NQ；未新增Repository/JDBC；未运行capacity benchmark；未进入B3；未接HTTP/Provider/Agent/LangGraph；未触碰交易、Paper或LIVE；未push/tag。

## 4. Required next fix

下一任务必须首先消除callback无界schema/data操作，移除或证明无截断的V14 cast，并补真实PostgreSQL失败后/重试、terminal timestamp offset、actual service completion和CAS-miss矩阵；同时获得明确authority以同步三份入口factsources。完成前，capacity acceptance和B3继续禁止。
