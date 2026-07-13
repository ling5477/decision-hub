# DH Stage-QDR-7 B2 Schema Errata Implementation Review

> task: `DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW`
> mode: `REVIEW_ONLY / POSTGRESQL_REGRESSION / NO_CODE_CHANGE / NO_TEST_CHANGE / NO_MIGRATION_CHANGE`
> review baseline: `3ac87a2`
> review target: `f144a4222619a5ef7fc254a5a8ce37b310687149`
> decision: `BLOCKED / P1_FIX_REQUIRED`

## 1. 审查结论

结论：不通过。

`beforeEachMigrate`事件识别、callback/V13事务回滚、history gate、V12 fingerprint、1000行repair、temporary CHECK、V14 no-truncation与retry的既有PostgreSQL回归均通过；但独立PostgreSQL 17.10实验确认`lock_timeout`在guard表预检之后才设置，无法约束预检阶段的锁等待。另有三个current入口文件保留旧B2状态，current factsources不一致。两项均命中本任务blocker规则。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW: BLOCKED
SCHEMA_ERRATA_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_MILESTONE_REVIEW_RETRY_3: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX
```

## 2. 范围

- 已审查：`3ac87a2..f144a42`、新旧callback、V12/V13/V14、直接PostgreSQL测试、指定current factsources、Maven与quality结果。
- 未审查：capacity benchmark、B3、外部HTTP、Provider、NQ runtime、Agent/LangGraph、Paper/LIVE。
- 明确不涉及：Java生产代码修改、Java测试修改、callback修改、V1–V14修改、API/Controller/DTO/OpenAPI修改。

## 3. Findings

### P0

- 无。

### P1

#### P1-1 `lock_timeout`未覆盖guard表预检阶段

- callback先在第95–105行读取constraint fingerprint，并在第117–135行读取legacy数据；直到第145行才执行`SET LOCAL lock_timeout = '5s'`。
- 独立PostgreSQL 17.10实验在另一事务对`dh_qdr7_idempotency_guard`持有`ACCESS EXCLUSIVE`锁，再执行未修改的callback。为防实验无界等待，仅在调用session外加8秒`statement_timeout`安全终止器。
- callback在constraint fingerprint SQL等待约8436ms后以`canceling statement due to statement timeout`失败，不是5秒`lock timeout`。因此冻结的5秒锁等待合同没有覆盖整个兼容路径。
- 现有测试只持有`ACCESS SHARE`锁，使fingerprint和数据预检可通过，并在后续`ALTER TABLE`等待；该测试不能发现precheck lock gap。

最小修复：在任何可能访问或锁定guard表的catalog/table查询前设置冻结的transaction-local timeout，并增加`ACCESS EXCLUSIVE`预检锁回归；不得改变`5s`/`60s`数值。

#### P1-2 current factsources存在旧状态残留

- `README.md`顶部与`docs/current/README.md`顶部已是`DONE / REVIEW_PENDING`，但两文件后部仍写`B2 BLOCKED / SCHEMA_ERRATA_REVIEWED`并指向旧implementation任务。
- `docs/current/CODEX_PROJECT_INSTRUCTIONS.md`后部同样保留旧B2状态和旧next action。
- `AGENTS.md`已明确自身只是执行指导，权威状态由`STATUS.md`、`WORK_ORDER.md`与`FACTSOURCE_POLICY.md`指定文件定义；但本任务要求指定current入口相互一致，当前不满足。
- 上述三个残留文件不在本轮写入allowlist，本轮未越界修复。

最小修复：blocker-fix完成后，在其明确allowlist内统一所有指定current入口；不得改写历史review结论。

### P2

- 无新增P2。

### P3

- Maven全局settings存在`profiles`标签warning；不影响本轮exit 0，但属于环境治理风险。

## 4. 已通过证据

- Commit scope：旧`beforeMigrate`删除，仅新增`beforeEachMigrate`、直接测试和允许文档；无Java生产代码、API、安全合同或NQ diff。
- Callback discovery：Flyway真实日志为`Executing SQL callback: beforeEachMigrate - qdr7 v13 compatibility`。
- Transaction coupling：V13注入失败、5秒后置DDL锁等待、60秒statement timeout均证明callback repair/temporary CHECK/V13整体rollback；修复故障后retry到V14。
- History/no-op：V13/V14完成环境通过repeatable probe触发callback，在guard表`ACCESS EXCLUSIVE`锁下3秒内返回，证明history gate先退出且不访问业务表。
- Fingerprint/precheck：V12列结构及schema/table/name/type/validated/`pg_get_constraintdef`均验证；missing/drifted CHECK与blank legacy均在兼容写入前拒绝。
- Bounded repair：1000成功；1001在写入前拒绝并保持1001个padded原值；repair按`guard_id`排序、UPDATE限制1000且校验affected count。
- Temporary CHECK：只放开FAILED/EXPIRED从`completed_at`迁移所需的瞬时条件；V13以同名约束替换为正式定义。
- V14：33字符在类型变更前失败，`varchar(64)`与原值保持；修正32字符后retry到`varchar(32)`。
- V1–V14：`3ac87a2..f144a42`对versioned migration diff为空，Flyway验证14个migration通过。

## 5. 验证结果

| Check | Result | Evidence |
|---|---|---|
| targeted callback suite | PASS | 11 tests / 0 failures / 0 errors / 0 skipped；PostgreSQL 17.10 |
| owning reactor | PASS | `mvn -ntp -pl dh-app -am test`；158 reports / 1075 tests / 0 failures / 0 errors / 0 skipped |
| full reactor | PASS | `mvn -ntp test`；158 reports / 1075 tests / 0 failures / 0 errors / 0 skipped |
| quality | PASS | `mvn -ntp -Pquality validate`；19/19、根Checkstyle 0、Spotless PASS |
| frozen 5s lock contract | FAIL | precheck在timeout设置前等待；独立PG实验8436ms由外部8秒statement timeout终止 |
| current factsource consistency | FAIL | root/current README与CODEX instruction存在旧B2/next-action残留 |
| PostgreSQL/Testcontainers | PASS / INSUFFICIENT_FOR_P1 | Docker Desktop、PostgreSQL 17.10真实运行，0 skipped；既有suite未覆盖precheck `ACCESS EXCLUSIVE`锁 |

首次定向Maven命令因PowerShell参数解析把`-Dsurefire.failIfNoSpecifiedTests=false`误传为lifecycle phase而exit 1，未进入测试；使用`mvn --% ...`原样传参后真实执行并通过。该RCA不是产品测试失败。

## 6. 风险、回滚与边界

- 影响面：V12→V13升级若遇并发DDL或其他`ACCESS EXCLUSIVE`持锁者，可在进入冻结的5秒timeout设置前持续等待，造成启动/migration窗口不可预测。
- 触发条件：callback已通过history gate，开始V12 fingerprint或legacy precheck，同时guard表存在冲突锁。
- 最坏结果：Flyway migration长时间阻塞；不会据现有证据造成部分写入，但违反冻结的bounded failure合同。
- 回滚：本轮仅文档变更，可对本轮文档diff做普通revert；禁止reset历史。callback修复必须另起blocker-fix任务。
- 边界：未修改NQ、Java生产/测试、callback、V1–V14、API/OpenAPI、HMAC/nonce/source；未运行capacity benchmark，未进入B3，未连接外部HTTP/Provider/Agent/LangGraph/Paper/LIVE，未push/tag。

## 7. 未验证项

- blocker-fix后的precheck lock timeout与factsources一致性：本轮没有修复授权，必须在下一任务验证。
- branch remote：仅核验本地tracking ref，`origin/dev...HEAD = 0 behind / 1 ahead`；未执行fetch/push。
