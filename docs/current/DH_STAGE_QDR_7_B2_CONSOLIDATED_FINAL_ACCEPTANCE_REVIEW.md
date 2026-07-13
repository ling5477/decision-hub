# DH Stage-QDR-7 B2 Consolidated Final Acceptance Review

> task: `DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE`
> classification: `DOC_FIX + CURRENT_FACTSOURCE_ALIGNMENT + TASK_SCOPE_GOVERNANCE_FIX + CONSOLIDATED_FINAL_ACCEPTANCE + B2_MILESTONE_CLOSE`
> baseline/HEAD: `04a98a8a09866bc0dd20a0fbbbdc8b12ca6c175c`
> initial verdict: `BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT`
> final verdict: `PASS / B2 ACCEPTED`
> next action: `DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE`

## 1. 最终验收结论

结论：通过。

```text
Initial review:
BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT

Technical result at initial review:
PASS

Root cause:
validation required 8 current factsources,
but the write allowlist excluded 6 of them

Resolution:
expanded the same task scope to include all validated factsources
and aligned them without changing code, tests, callback or migrations

Technical evidence:
reused because technical artifacts did not change

Final verdict:
PASS / B2 ACCEPTED
```

初始review未发现新的实现、安全、事务、migration、tenant或API P0/P1，技术结果始终为PASS。唯一阻断来自任务scope设计：要求验证8个current factsources，却只允许写其中2个。用户在同一任务中显式扩展write allowlist后，6个冲突入口与2个主权威全部完成对齐；技术artifact未变化，因此复用初始review的技术证据，不重跑full tests或PostgreSQL/Testcontainers。

```text
STAGE_QDR_7_B2_FACTSOURCE_ALIGNMENT_AND_FINAL_ACCEPTANCE: DONE
TASK_SCOPE_DESIGN: PASS
VALIDATION_WRITE_ALLOWLIST_ALIGNMENT: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 0 CONFLICTS
SCHEMA_ERRATA_IMPLEMENTATION: ACCEPTED
PERSISTENT_GUARDS_IMPLEMENTATION: ACCEPTED
B2_IMPLEMENTATION_STATUS: ACCEPTED
MIGRATION_COMPATIBILITY: PASS
CALLBACK_TIMEOUT_SCOPE: PASS
TRANSACTION_ATOMICITY: PASS
CLEANUP_SAFETY: PASS
COMMIT_UNKNOWN_EVIDENCE: PASS
TENANT_SOURCE_ISOLATION: PASS
API_HMAC_NONCE_SOURCE_COMPATIBILITY: PASS
SECURITY_BOUNDARY: PASS
POSTGRESQL_TEST_EVIDENCE: REUSED_PASS
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Scope冲突修复与治理冻结

本任务的`VALIDATION_SCOPE`、`READ_SCOPE`、`FIXABLE_BLOCKER_SCOPE`、`CURRENT_FACTSOURCE_SCAN_SCOPE`与`WRITE_ALLOWLIST`已覆盖同一组8个current factsources：`AGENTS.md`、`CLAUDE.md`、root/current README、`CODEX_PROJECT_INSTRUCTIONS.md`、`FACTSOURCE_POLICY.md`、`STATUS.md`和`WORK_ORDER.md`。

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
```

治理规则已同步到`AGENTS.md`、`CODEX_PROJECT_INSTRUCTIONS.md`、`FACTSOURCE_POLICY.md`和`WORK_ORDER.md`：任一包含关系不成立时必须输出`TASK_SCOPE_DESIGN_INVALID`并停止实施；技术PASS且唯一阻断为current docs漂移时，必须在同一任务内修复并最终验收，禁止形成`docs fix -> review -> docs fix -> review`循环。

## 3. 技术证据复用

```text
Maven tests:
NOT_RERUN
Reused evidence:
1076 tests / 0 failures / 0 errors / 0 skipped

PostgreSQL/Testcontainers:
NOT_RERUN
Reused evidence:
PostgreSQL 17.10 / real execution
```

复用范围覆盖migration compatibility、callback timeout scope、transaction atomicity、cleanup safety、commit-unknown evidence、tenant/source isolation、API/HMAC/nonce/source compatibility与security boundary。由于本任务未修改技术artifact，复用证据有效；本轮只运行文档质量门`mvn -ntp -Pquality validate`。

文档质量门实际结果：exit 0，Reactor 19/19 `SUCCESS`，`BUILD SUCCESS`，Checkstyle 0 violations，Spotless check通过，总耗时6.276秒。系统Maven settings的`profiles`未识别warning为既有非阻断工具风险；`mvnw.cmd`未运行。

## 4. B2 milestone close

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED / ACCEPTED
Schema errata implementation: ACCEPTED
Persistent guards implementation: ACCEPTED
Post-B2 capacity acceptance: NOT_STARTED / NEXT
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE
```

本close不运行capacity benchmark，不进入B3，不修改API/OpenAPI，不接外部HTTP/provider/NQ/Agent/LangGraph，不启用Paper/LIVE。

## 5. Initial review — Previous review attempt / Historical preserved

以下为初始consolidated review的完整审计记录。其`BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT`、P1判断、证据和当时next action均按原始事实保留；它不覆盖上方final verdict。

```text
task: DH-STAGE-QDR-7-B2-CONSOLIDATED-FINAL-ACCEPTANCE-REVIEW
classification: REVIEW_ONLY + SCHEMA_ERRATA_ACCEPTANCE + PERSISTENT_GUARD_MILESTONE_ACCEPTANCE + CURRENT_FACTSOURCE_RECHECK + POSTGRESQL_REGRESSION
baseline/HEAD: 04a98a8a09866bc0dd20a0fbbbdc8b12ca6c175c
verdict: BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT
next action: DH-STAGE-QDR-7-B2-CONSOLIDATED-BLOCKER-FIX
```

### 5.1 初始审查结论

结论：不通过。

schema errata、persistent guard实现、真实PostgreSQL 17.10/Testcontainers回归、全仓测试与质量门禁均通过本轮独立复核，没有发现新的实现、安全、事务、migration或tenant P0/P1。最终acceptance仍被current factsource硬验收阻断：任务要求检查的8个入口中，有6个仍把旧schema-errata alignment/retry写成current authority；这6个文件均不在本轮允许修改范围，不能越界修复。因此`current conflict count`不是0，不能宣告B2 accepted或授权post-B2 capacity acceptance。

```text
STAGE_QDR_7_B2_CONSOLIDATED_FINAL_ACCEPTANCE_REVIEW: BLOCKED
SCHEMA_ERRATA_TECHNICAL_REVIEW: PASS
PERSISTENT_GUARDS_TECHNICAL_REVIEW: PASS
CURRENT_FACTSOURCE_CONSISTENCY: FAIL / 6 FILES
SCHEMA_ERRATA_IMPLEMENTATION: BLOCKED / ACCEPTANCE_NOT_COMPLETED
PERSISTENT_GUARDS_IMPLEMENTATION: BLOCKED / ACCEPTANCE_NOT_COMPLETED
B2_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

### 5.2 范围

- 已审查：`19666e5`、`ee830d4`、`74cfeb9`、`8660f4e`、`3ac87a2`、`f144a42`、`479dbc`、`04a98a8`；callback、V12–V14、persistent rate/idempotency、cleanup、Spring事务边界、result reference、Controller guard顺序、测试及current factsources。
- 未审查：capacity benchmark、B3、外部HTTP/provider、NQ runtime、Agent/LangGraph、Paper/LIVE。
- 明确不涉及：生产代码、测试、callback、V1–V14、API/OpenAPI、HMAC/nonce/source合同的修改。

### 5.3 Findings

#### P0

- 无。

#### P1

- `README.md`、`AGENTS.md`、`CLAUDE.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/FACTSOURCE_POLICY.md`仍以未标historical的旧schema-errata alignment/retry作为current入口，没有统一到本任务要求的`B2: FINAL_ACCEPTANCE_REVIEW / capacity acceptance: NOT_STARTED`。这些文件不在本轮allowlist，无法在不越权的情况下把冲突数降为0。

#### P2

- Maven全局settings的`profiles`未识别warning与Mockito future-JDK warning为既有非阻断风险；本轮不修改本机配置或测试启动参数。

#### P3

- 无。

### 5.4 Schema errata复核

- 唯一callback为`beforeEachMigrate__qdr7_v13_compatibility.sql`；本轮工作树的V1–V14 diff为空，Flyway成功校验14个migration。
- callback只先读`flyway_schema_history`；V12未成功、V13已成功或V14已成功时在访问guard表前no-op。
- `SET LOCAL lock_timeout = '5s'`与`SET LOCAL statement_timeout = '60s'`位于首次guard表、`pg_attribute`、`pg_constraint`、`pg_class`和数据precheck之前。
- PostgreSQL 17.10 `ACCESS EXCLUSIVE`回归从callback进入`15:33:56.949`到失败路径返回`15:34:02.050`，日志观测约5.101秒；未依赖外部session statement timeout，未等待接近60秒。
- 锁超时后V12 history、strict CHECK、padded legacy原值与pre-V13 schema保持；V13/V14无成功记录和partial state。释放锁后安全retry到V14，session `lock_timeout`/`statement_timeout`保持默认值。
- repair 1000成功、1001写入前失败、constraint fingerprint拒绝、statement timeout整体rollback、V13故障rollback/retry、completed V13/V14 no-op及V14 33字符no-truncation/retry均由12项真实PostgreSQL测试覆盖。

### 5.5 Persistent guards复核

- rate admission为数据库时间驱动的fixed-window conditional upsert；并发quota winner与after-commit unknown均fail-closed。
- idempotency使用完整`environment + endpoint + source + tenant + requestId`身份、request hash与expected state/version/lease token CAS；EXPIRED为不可复用tombstone。
- lifecycle、lease、heartbeat、recovery、terminal timestamp、retention与cleanup eligibility使用PostgreSQL `transaction_timestamp()`；caller `+48h/-48h`不改变判定。
- cleanup为bounded batch与`FOR UPDATE SKIP LOCKED`；保护current rate window、active lease及future retention，更新计数来自实际成功transition，不把0-row CAS结果计为成功。
- completed duplicate先校验固定result type，再以tenant-bound read model重建snapshot并校验checksum；missing/cross-tenant/wrong-type/checksum mismatch/unreadable均fail-closed。
- `PersistentGuardedDecisionDryRunService.executeFirst`在同一`GuardTransactionBoundary.required`内执行lease、delegate output/audit、result projection/checksum与terminal CAS。真实Spring beans验证`DataSourceTransactionManager`与`JdbcTemplate`共享同一DataSource/connection，output、audit、checksum与COMPLETED CAS失败均整体rollback。
- rate/idempotency均以真实delegate commit后抛异常验证commit unknown；production wiring无generic in-memory idempotency fallback。
- Controller顺序保持HMAC/timestamp/nonce先于persistent rate与idempotency completed-result读取；API/OpenAPI、HMAC、nonce、source合同diff为空，无raw material持久化或交易授权。

### 5.6 证据

- targeted：`mvn -ntp -pl dh-usecase,dh-infra,dh-security,dh-api,dh-app -am test`，15/15 reactor，`BUILD SUCCESS`；dh-app 147项，0 failure/error/skip；总耗时7分43秒。
- full：`mvn -ntp test`，19/19 reactor，158 reports / 1076 tests / 0 failures / 0 errors / 0 skipped；总耗时7分05秒。
- quality：`mvn -ntp -Pquality validate`，19/19 reactor，Checkstyle 0 violations，Spotless PASS。
- PostgreSQL/Testcontainers：Docker Desktop 29.6.1，Testcontainers 1.20.4，`postgres:17`，Flyway报告PostgreSQL 17.10，真实执行且0 skipped。
- Git：`dev`，HEAD `04a98a8a09866bc0dd20a0fbbbdc8b12ca6c175c`，开工时clean/staged empty，`origin/dev...HEAD=0/0`。

### 5.7 风险与修复建议

- 影响面：current入口会把后续执行路由回已消费的schema-errata retry，且无法满足本轮显式`current conflict count=0`验收。
- 最坏结果：B2在factsources不一致时被错误接受，或后续任务继续从旧入口启动。
- 最小修复：在单独授权的docs-only blocker fix中，仅同步上述6个入口与本轮主权威；保留所有previous `BLOCKED` review为historical。
- 验证方式：重新扫描8个指定factsources，要求文件级冲突数0；执行forbidden-scope diff与`git diff --check`。
- 回滚方式：本轮仅为review文档，可按文件级diff撤销；未修改实现、测试或migration。

### 5.8 实际读取文件

- `AGENTS.md`、`CLAUDE.md`、`README.md`
- `docs/current/README.md`、`CODEX_PROJECT_INSTRUCTIONS.md`、`FACTSOURCE_POLICY.md`、`STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`
- `docs/current/DH_STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY.md`
- `docs/current/DH_STAGE_QDR_7_B2_PERSISTENT_GUARDS_SCHEMA_SECURITY_REVIEW.md`
- `docs/current/DH_STAGE_QDR_7_B2_PERSISTENT_GUARDS_IMPLEMENTATION.md`
- `docs/current/DH_STAGE_QDR_7_B2_PERSISTENT_GUARDS_BLOCKER_FIX_RETRY.md`
- `docs/current/DH_STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY.md`
- `docs/current/DH_STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY_2.md`
- `docs/current/DH_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION.md`
- `docs/current/DH_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY.md`
- `dh-app/src/main/resources/db/migration/beforeEachMigrate__qdr7_v13_compatibility.sql`
- `dh-app/src/main/resources/db/migration/V12__qdr7_persistent_runtime_guards.sql`
- `dh-app/src/main/resources/db/migration/V13__qdr7_persistent_guard_safety_fix.sql`
- `dh-app/src/main/resources/db/migration/V14__qdr7_persistent_guard_schema_alignment.sql`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/PersistentGuardedDecisionDryRunService.java`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcRateLimitAdmissionAdapter.java`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcIdempotencyGuardAdapter.java`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/JdbcGuardCleanupAdapter.java`
- `dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/PersistentGuardProductionWiringPostgresTest.java`
- `dh-app/src/test/java/com/guidinglight/decisionhub/V13TransactionalCompatibilityCallbackFlywayPostgresTest.java`

### 5.9 未验证项

- post-B2 capacity benchmark未运行且不允许运行。
- B3、外部HTTP/provider、NQ、Agent/LangGraph、Paper/LIVE未进入。
- 6个白名单外current factsources未修改，因此本轮无法把冲突数降为0。
