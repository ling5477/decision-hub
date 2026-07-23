# DH Stage-QDR-7 B2 Persistent Guards Milestone Review

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW`
> mode: `REVIEW_ONLY + MIGRATION_SECURITY_REVIEW + TRANSACTION_ATOMICITY_REVIEW`
> review target: `19666e5 feat(qdr): add persistent dry-run guards`
> verdict: `BLOCKED / P1_SCHEMA_EXPIRY_CLEANUP_SAFETY_GAPS`

## 1. Review结论

`19666e5`的commit范围、rate-limit conditional upsert、exact identity、CAS/version/token、tenant-bound result FK、nonce-first guard顺序、no-in-memory-fallback和PostgreSQL执行证据均符合B2方向；owning 875 tests与full 1045 tests全部通过且0 skipped。

但milestone不能接受。V12与冻结schema存在字段和状态生命周期漂移；idempotency `expires_at`在production SQL中只写不读，terminal record不会进入`EXPIRED`；cleanup cutoff由调用方JVM时间任意提供，数据库未用`database_now`和safety grace保护当前bucket；关键completion/result/cleanup failure路径缺少真实JDBC事务证据。根据任务stop rule，本轮不修改实现，B2保持`BLOCKED`。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW: BLOCKED
B2_IMPLEMENTATION_STATUS: BLOCKED / FIX_REQUIRED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX
```

## 2. Findings

### P1-1：`expires_at`没有驱动EXPIRED语义

- `JdbcIdempotencyGuardAdapter.INSERT_SQL`写入`expires_at`，但admission、transition、duplicate和cleanup SQL均不读取该字段。
- adapter禁止`COMPLETED/FAILED -> EXPIRED`，与冻结review的terminal tombstone生命周期不一致。
- `JdbcGuardCleanupAdapter`在`retention_until`到期后直接物理删除`COMPLETED/FAILED/EXPIRED`。删除后相同完整identity可再次atomic insert；旧requestId可能重新占用，无法维持冻结的`EXPIRED / 409 / use new requestId`语义。
- 当前`EXPIRED`只有显式`RECEIVED/IN_PROGRESS -> EXPIRED`路径，但production orchestration没有调用该转换。

必须修复：冻结并实现DB-time expiry transition/tombstone路径；在retention保证期内duplicate必须稳定返回EXPIRED；物理清理不得绕过该语义。

### P1-2：cleanup cutoff未由PostgreSQL时间封闭

- `GuardCleanupCommand.cutoffExclusive`接受任意JVM `Instant`。
- rate cleanup只检查`window_end < ?`，未检查`transaction_timestamp()`、current window或冻结的safety grace。
- internal caller若传入未来cutoff，可删除当前甚至未来bucket；数据库层没有active bucket不变量。
- idempotency cleanup同样依赖调用方cutoff，而冻结review要求DB transaction time。

必须修复：expiry predicate必须在SQL内基于PostgreSQL transaction time和安全配置计算；调用方不能提供可前推的绝对删除时间。补当前bucket、future cutoff、clock rollback和并发cleanup真实PostgreSQL回归。

### P1-3：V12与冻结idempotency schema不一致

冻结review要求`lease_owner`、固定`result_type=DH_DECISION_OUTPUT`、`completed_at`仅COMPLETED和独立`failed_at`。V12未包含`lease_owner/result_type/failed_at`，并复用`completed_at`表示FAILED/EXPIRED。

`result_type`可由单一FK目标隐含，但当前没有review errata更新冻结合同；`lease_owner`缺失使crash recovery无法记录bounded worker ownership；timestamp语义与冻结CHECK不一致。必须在blocker-fix前决定：按冻结schemaforward-fix，或先做明确schema-contract errata review，不能静默偏离。

### P1-4：lease/TTL/retention混用JVM与DB clock

- orchestration用`Clock.systemUTC()`生成`expires_at`、`retention_until`和`lease_expires_at`。
- DB用`transaction_timestamp()`写`created_at/updated_at`并判断expired lease。
- JVM/DB clock drift可能导致合法lease被CHECK拒绝、提前recovery或延后recovery；这不是冻结的单一一致时间源。

必须修复：duration由配置提供，absolute expiry由同一PostgreSQL事务时间生成；补clock-skew/rollback测试。

### P1-5：关键原子事务和result-reference测试证据不足

现有真实PostgreSQL测试证明rate rollback、CAS、heartbeat/recovery、FK completion和bounded单批cleanup，但未证明：

- `decision output + success audit + IN_PROGRESS -> COMPLETED`使用actual Spring wiring/JDBC时任一失败整体rollback；
- idempotency admission audit失败时insert rollback；
- completed duplicate target missing/checksum mismatch映射`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`；
- FAILED/EXPIRED duplicate稳定语义；
- 并发cleanup、future cutoff、active/current bucket安全；
- connection loss/commit-unknown不自动重放业务。

这些属于任务明确要求的milestone证据，不可用mock-only store分类测试替代。

## 3. 已通过项目

- Commit仅包含B2 migration、ports/JDBC、内部wiring、taxonomy、直接测试和允许文档；无NQ、contracts/OpenAPI、HMAC/nonce或V1-V11 diff。
- Rate table composite key、counter/window CHECK、conditional upsert和并发winner正确。
- Idempotency exact identity、hash格式、state allowlist、CAS expected state/version/token和terminal overwrite拒绝正确。
- Result FK为`(tenant_id,result_id) -> dh_decision_output(tenant_id,decision_id)`；safe projector执行tenant-first read并重算checksum。
- Controller在HMAC/timestamp/nonce成功后调用persistent rate，再进入persistent idempotency；generic filter精确排除该route。
- PostgreSQL unavailable无in-memory fallback；rate/store/commit/error HTTP分类方向正确。
- 无raw body、signature、nonce、credential、prompt/provider raw response列。

## 4. Validation

| Check | Result |
|---|---|
| owning modules | `BUILD SUCCESS`；875 tests；0 failures/errors/skips |
| full Maven | `BUILD SUCCESS`；155 reports、1045 tests；0 failures/errors/skips |
| quality | `BUILD SUCCESS`；Checkstyle 0；Spotless PASS |
| PostgreSQL/Testcontainers | PostgreSQL 17.10真实运行；V12 suite 7 tests、0 skipped |
| migration history | V1-V11 target diff为空；V1→V12及V11→V12测试通过 |
| milestone sufficiency | `BLOCKED`；绿色测试未覆盖P1缺口 |

## 5. Boundary confirmation

本review未修改Java生产/测试、V1-V12、API/Controller/DTO/OpenAPI、HMAC/nonce/source、Repository/JDBC或NQ；未运行capacity benchmark；未接外部HTTP/Provider/Agent/LangGraph；未触碰交易、Paper或LIVE；未push/tag/commit。

## 6. Next action

`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX`

Blocker fix必须采用forward-only additive schema策略；不得修改已提交V12。如需要改变冻结schema合同，先进入独立schema/security errata review。

