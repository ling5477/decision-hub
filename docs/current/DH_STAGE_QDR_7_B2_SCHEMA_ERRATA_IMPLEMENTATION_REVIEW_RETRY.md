# DH Stage-QDR-7 B2 Schema Errata Implementation Review Retry

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY`
> mode: `REVIEW_ONLY / CALLBACK_TIMEOUT_SCOPE_REVIEW / TRANSACTIONAL_CALLBACK_RECHECK / POSTGRESQL_LOCK_REGRESSION / MIGRATION_COMPATIBILITY_RECHECK / CURRENT_FACTSOURCE_RECHECK`
> implementation commit: `f144a4222619a5ef7fc254a5a8ce37b310687149`
> previous review: `6a1794d / BLOCKED / LOCK_TIMEOUT_SCOPE_GAP`
> blocker fix commit: `479dbcab6b4a29a005cb524afc5a146d224ae2af`
> decision: `BLOCKED / CURRENT_FACTSOURCE_FIX_REQUIRED`

## 1. 审查结论

结论：不通过。

`479dbc`已经关闭原review的唯一callback技术blocker：冻结的`SET LOCAL lock_timeout = '5s'`和`SET LOCAL statement_timeout = '60s'`位于history-only no-op gate之后、首次guard表及相关catalog precheck之前。真实PostgreSQL 17.10/Testcontainers的`ACCESS EXCLUSIVE`锁回归证明precheck约5秒失败、callback/V13/history整体回滚、释放锁后安全retry至V14，且migration连接的session setting未被污染。

但current factsources与仓库入口规范仍存在未标为historical的旧阶段入口：`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`后部仍声明`STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED`并指向旧implementation work order；`docs/current/FACTSOURCE_POLICY.md`的“当前状态”仍指向Stage-QDR-4与旧cleanup任务；仓库`CLAUDE.md`仍把当前阶段写成Stage3-B3及`Integration-0-PLAN`。本任务明确规定current factsources存在冲突即必须`BLOCKED`，因此不得接受schema errata implementation，也不得授权milestone review retry-3。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY: BLOCKED
CALLBACK_TIMEOUT_ORDER: PASS
PRECHECK_LOCK_TIMEOUT: PASS
TRANSACTION_ROLLBACK_SAFETY: PASS
FAILED_MIGRATION_RETRY: PASS
COMPLETED_ENVIRONMENT_NO_OP: PASS
SESSION_SETTING_ISOLATION: PASS
V1_V14_IMMUTABILITY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: FAIL
POSTGRESQL_TEST_EVIDENCE: PASS
SCHEMA_ERRATA_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_MILESTONE_REVIEW_RETRY_3: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
```

## 2. 范围

- 已审查：`6a1794d..479dbc`、callback history gate与timeout顺序、V12/V13/V14 compatibility、直接PostgreSQL锁测试、session isolation、指定current factsources、完整Maven与quality结果。
- 未审查：capacity benchmark、B3 implementation、外部HTTP、Provider、NQ runtime、Agent/LangGraph、Paper/LIVE。
- 明确不涉及：Java生产代码修改、Java测试修改、callback修改、V1–V14修改、API/Controller/DTO/OpenAPI修改、HMAC/nonce/source合同修改。

## 3. Findings

### P0

- 无。

### P1

#### P1-1 current factsources仍有未标历史的旧阶段入口

- `docs/current/CODEX_PROJECT_INSTRUCTIONS.md:261`标题仍为`Stage-QDR-4 当前入口`，并在`:344-349`把Stage-QDR-7写成`PLANNING / IMPLEMENTATION_NOT_STARTED`、next action写成`DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER`。该段没有historical/superseded限定，与文件顶部当前authority冲突。
- `docs/current/FACTSOURCE_POLICY.md:66-88`的“当前状态”仍把current task写成`DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION`、next action写成Stage-QDR-4 tag close，与Stage-QDR-7 B2事实冲突。
- 仓库`CLAUDE.md`的“当前阶段”仍声明`Stage3-B3 DH Backtest Request Adapter IMPL completed`、next为`Integration-0-PLAN`；它是仓库入口规范但没有historical限定，与本轮用户指令及`AGENTS.md`的Stage-QDR-7状态冲突。
- root README、current README、STATUS、WORK_ORDER和CODEX顶部块在review前均正确指向本review retry；但同一current事实源内部的未标历史旧入口仍违反“current factsources已消除旧阶段入口”的验收条件。
- `CODEX_PROJECT_INSTRUCTIONS.md`、`FACTSOURCE_POLICY.md`、`CLAUDE.md`不在本轮写入allowlist，本轮未越界修改。

最小修复：另起受控factsources blocker-fix retry，仅清除或明确标记上述旧current入口，并重新核验全部current authority；不得修改callback、测试、migration或实现代码。

### P2

- 无。

### P3

- blocker-fix测试diff除新增`ACCESS EXCLUSIVE`测试与相邻helper/source-order断言外，还把既有60秒statement-timeout测试的JUnit外层watchdog从75秒调为90秒。数据库`SET LOCAL statement_timeout = '60s'`未改变，该变更与慢宿主机回归直接相关，但不属于“只新增一个测试”的字面最小diff。
- Maven全局settings在`D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml:227`报告未识别`profiles`标签；本轮所有命令exit 0，不影响裁定，但属于本机wrapper/tooling风险。
- Testcontainers关闭后偶见Hikari后台连接补充线程的connection-refused warning；所有相关测试均0 failure/error/skip，未观察到数据库状态断言失效。

## 4. Callback与事务证据

精确顺序：

1. 第25行只对`flyway_schema_history`执行`to_regclass`。
2. 第31-39行只读取V12/V13/V14 history。
3. 第47-68行处理V14/V13完成no-op与V12未成功no-op。
4. 第72行设置`SET LOCAL lock_timeout = '5s'`。
5. 第73行设置`SET LOCAL statement_timeout = '60s'`。
6. 第75行才首次对guard表执行`to_regclass`。
7. 第83-110行执行`pg_attribute`、`pg_constraint`、`pg_class` fingerprint。
8. 第122-140行执行legacy data precheck与repair count。
9. 第150-165行执行bounded repair。
10. 第173行起执行DDL，随后由Flyway执行V13。

timeout之前没有guard表`SELECT`、repair count、`UPDATE`或`ALTER TABLE`。completed V13/V14路径在timeout设置和guard访问前返回；在guard表持有`ACCESS EXCLUSIVE`锁时，completed V13/V14 repeatable probe仍在测试门限内no-op。

`ACCESS EXCLUSIVE`测试在migration connection初始确认`SHOW lock_timeout = 0`、`SHOW statement_timeout = 0`，未设置外部session statement timeout。目标执行在4-15秒断言窗口内以包含`lock timeout`的Flyway failure结束。全量日志中callback于`14:23:03.305`进入，失败后的状态核验/重试连接于`14:23:08.443`开始，观测上界约`5.138s`；Maven stdout没有独立打印PostgreSQL抛错瞬间，故不伪造更细时间戳。

锁超时后的真库断言确认：history当前成功版本仍为V12；V13/V14成功记录均为0；V12 strict CHECK定义不变；padded legacy值逐字符保持；`lease_owner`不存在；没有partial repair或半完成schema。释放锁后callback再次运行，padded值只trim一次，V13/V14成功，最终正式约束生效。失败和成功后同一migration connection的两个`SHOW`值均恢复为`0`。

## 5. 完整迁移矩阵

`V13TransactionalCompatibilityCallbackFlywayPostgresTest`共12个测试，全部通过，覆盖：

- fresh V1→V14、V11→V14；
- V12 clean/padded→V14；
- blank legacy fail-closed；
- repair 1000成功、1001写入前失败；
- constraint fingerprint缺失/漂移拒绝；
- 60秒statement timeout整体rollback；
- V13故障整体rollback与安全retry；
- completed V13/V14 no-op；
- precheck `ACCESS EXCLUSIVE` 5秒lock timeout、数据库状态rollback、解锁retry与session isolation；
- V14长度32成功、33失败时类型/原值保持、修正后retry成功。

`f144a42..479dbc`对V1–V14 versioned migrations的diff为空；Flyway在真实PostgreSQL 17.10上反复验证14个migration并到达V14。

## 6. 验证结果

| Check | Result | Evidence |
|---|---|---|
| direct lock method | PASS | 1 test / 0 failures / 0 errors / 0 skipped；PostgreSQL 17.10 |
| callback PostgreSQL suite | PASS | 12 tests / 0 failures / 0 errors / 0 skipped |
| owning reactor | PASS | `mvn -ntp -pl dh-app -am test`；15/15；dh-app 147 / 0 / 0 / 0；总耗时7分17秒 |
| full reactor | PASS | `mvn -ntp test`；19/19；158 reports / 1076 tests / 0 failures / 0 errors / 0 skipped；总耗时11分04秒 |
| quality | PASS | `mvn -ntp -Pquality validate`；19/19；Checkstyle 0 violations；Spotless PASS |
| PostgreSQL/Testcontainers | PASS | Docker Desktop 29.6.1；PostgreSQL 17.10；0 skipped |
| callback timeout order | PASS | 两个`SET LOCAL`位于所有guard/catalog precheck之前 |
| transaction rollback/retry | PASS | 锁、statement timeout、V13故障均整体rollback，修复后retry到V14 |
| completed environment no-op | PASS | V13/V14完成环境不访问被锁guard表 |
| session isolation | PASS | 同一migration connection迁移前后均为`0`/`0` |
| current factsource consistency | FAIL / P1 | CODEX、FACTSOURCE_POLICY与CLAUDE存在未标历史旧current入口 |

正常无锁迁移没有触发60秒门禁，因此不输出`B2_SCHEMA_ERRATA_TIMEOUT_EVIDENCE_REQUIRED`。

## 7. 风险、回滚与边界

- 影响面：本轮仅新增/更新review/current文档；没有改变运行时代码、callback、测试或migration。
- 回滚：删除本review retry文档，并撤销本轮在`STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`新增的顶部段落即可；不得回滚用户或既有提交。
- 剩余风险：current事实源及仓库入口规范冲突会误导后续Agent选择旧work order，是当前唯一P1 blocker。
- 安全边界：未读取凭证或`.env`；未访问生产数据库；未调用外部HTTP、Provider或NQ；未触发交易、Paper或LIVE。

## 8. Readiness decision

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY: BLOCKED
CALLBACK_TIMEOUT_ORDER: PASS
PRECHECK_LOCK_TIMEOUT: PASS
LOCK_TIMEOUT_CONTRACT: PASS
STATEMENT_TIMEOUT_CONTRACT: PASS
TRANSACTION_ROLLBACK_SAFETY: PASS
FAILED_MIGRATION_RETRY: PASS
COMPLETED_ENVIRONMENT_NO_OP: PASS
SESSION_SETTING_ISOLATION: PASS
V1_V14_IMMUTABILITY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: FAIL
SECURITY_BOUNDARY: PASS
POSTGRESQL_TEST_EVIDENCE: PASS
SCHEMA_ERRATA_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_MILESTONE_REVIEW_RETRY_3: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
```
