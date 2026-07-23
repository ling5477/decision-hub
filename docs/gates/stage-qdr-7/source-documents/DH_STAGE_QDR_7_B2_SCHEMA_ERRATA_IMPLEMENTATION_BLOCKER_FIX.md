# DH Stage-QDR-7 B2 Schema Errata Implementation Blocker Fix

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX`
> mode: `P1_SECURITY_BLOCKER_FIX / CALLBACK_TIMEOUT_SCOPE_FIX / POSTGRESQL_LOCK_REGRESSION`
> status: `DONE / REVIEW_PENDING`
> milestone acceptance: `NOT_YET`

## 1. Blocker 与最小修复

历史implementation review确认`beforeEachMigrate` transaction coupling、history gate、bounded repair、rollback/retry和V14 no-truncation通过，但拒绝原callback的timeout顺序：constraint/data precheck可在设置5秒`lock_timeout`前访问guard表及其catalog指纹。该`BLOCKED`结论保留在历史review记录中，本文不改写它。

本轮仅将以下冻结设置前移到history-only no-op gate之后、首次`to_regclass('public.dh_qdr7_idempotency_guard')`和任何guard-related catalog precheck之前：

```sql
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
```

V12未成功、V13已成功或V14已成功时仍在timeout和guard表访问前立即no-op。constraint fingerprint、legacy data precheck、1000行repair ceiling、temporary CHECK、V13/V14与safe retry语义未改变。

## 2. PostgreSQL 锁回归

`V13TransactionalCompatibilityCallbackFlywayPostgresTest` 新增precheck锁场景：connection A以未提交事务持有`ACCESS EXCLUSIVE`；connection B使用独立连接从V12执行Flyway。callback进入兼容路径后在precheck首次表访问等待锁，日志观测约5.195秒后以PostgreSQL lock timeout失败，没有依赖额外session-level statement timeout。

失败后history仍为V12，V12 strict CHECK定义不变，padded `stable_error_code`原值不变，temporary CHECK和V13/V14 success history不存在。同一migration connection的`SHOW lock_timeout`和`SHOW statement_timeout`均恢复为`0`。释放锁后重试成功到V14，padded值只trim一次。

## 3. 验证

- 定向timeout回归：2 tests / 0 failures / 0 errors / 0 skipped，PostgreSQL 17.10/Testcontainers。
- owning：`mvn -ntp -pl dh-app -am test`，15/15 reactor，`BUILD SUCCESS`。
- full：`mvn -ntp test`，158 reports / 1076 tests / 0 failures / 0 errors / 0 skipped，19/19 reactor，`BUILD SUCCESS`。
- quality：`mvn -ntp -Pquality validate`，19/19 reactor，Checkstyle 0 violations，Spotless PASS。

首轮目标类在较慢宿主机上被JUnit外层`@Timeout(75)`中止，数据库冻结值仍为60秒。仅将测试watchdog放宽到90秒后，两条timeout定向用例与后续owning/full回归均通过；未改变生产callback timeout合同。

## 4. 边界与下一步

未修改V1–V14、Java生产代码、API/Controller/DTO/OpenAPI、HMAC、nonce、source合同或NQ。未运行capacity benchmark，未进入B3，未接外部HTTP、Provider、Agent/LangGraph或LIVE。

```text
Stage-QDR-7 B1: FROZEN
B2 schema errata blocker fix: DONE / REVIEW_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY
```
