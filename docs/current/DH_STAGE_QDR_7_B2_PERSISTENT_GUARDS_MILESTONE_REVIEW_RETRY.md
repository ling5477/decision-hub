# DH Stage-QDR-7 B2 Persistent Guards Milestone Review Retry

> task: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY`
> classification: `REVIEW_ONLY + P1_FIX_VERIFICATION + FORWARD_MIGRATION_REVIEW + LIFECYCLE_SECURITY_REVIEW + TRANSACTION_ATOMICITY_REVIEW + POSTGRESQL_REGRESSION`
> review baseline: `e3fd401`
> review target: `ee830d4 + 74cfeb9`
> reviewed HEAD: `74cfeb91716371756bd1befa930599d95011085f`
> verdict: `BLOCKED / P1_FIX_RETRY_REQUIRED`
> next action: `DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY`

## 1. Review decision

本轮独立复核确认 EXPIRED tombstone、database-time lifecycle、rate cleanup、lease owner/token、tenant-bound foreign key、admission/completion rollback和rate after-commit unknown方向已有实质修复，且三组规定 Maven 命令全部通过。但是，V12→V13 compatibility、冻结schema一致性、idempotency真实JDBC commit-unknown、production transaction wiring、JVM clock isolation和真实JDBC result-reference证据仍未达到P1关闭门槛。因此previous milestone review的`BLOCKED / 5 P1`历史结论保持不变，B2不得accepted，capacity acceptance和B3继续禁止。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY: BLOCKED
V13_FORWARD_FIX: REJECTED
V12_IMMUTABILITY: PASS
IDEMPOTENCY_EXPIRY_LIFECYCLE: PASS
REQUEST_ID_TOMBSTONE: PASS
DB_CLOCK_UNIFICATION: PASS / CODE_PATH
RATE_CLEANUP_SAFETY: PASS
IDEMPOTENCY_CLEANUP_SAFETY: PASS / CODE_PATH
LEASE_CRASH_RECOVERY: PASS / INTERNAL_PRIMITIVE
RESULT_REFERENCE_SAFETY: BLOCKED / ACTUAL_JDBC_END_TO_END_EVIDENCE_MISSING
ADMISSION_AUDIT_ATOMICITY: PASS / ACTUAL_JDBC_TRANSACTION_TEMPLATE
COMPLETION_ATOMICITY: BLOCKED / PRODUCTION_WIRING_EVIDENCE_MISSING
COMMIT_UNKNOWN_EVIDENCE: BLOCKED / IDEMPOTENCY_REAL_JDBC_MISSING
STORE_FAILURE_FAIL_CLOSED: PASS
PRODUCTION_IN_MEMORY_FALLBACK: ABSENT
API_CONTRACT_UNCHANGED: YES
SECURITY_BOUNDARY: BLOCKED / P1_EVIDENCE_GAPS
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / ZERO_SKIPS
B2_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
```

## 2. Commit and boundary review

- `dev`与HEAD匹配任务输入；worktree和staged在开工时均为空。
- `dev`相对`origin/dev`为behind 0、ahead 4；本轮未fetch、push或创建tag。
- `e3fd401..74cfeb9`共23个文件；V1-V12 diff为空，previous milestone review blob hash与`e3fd401`一致。
- API、Controller、DTO、OpenAPI、HMAC、nonce和source路径diff为空；未修改NQ，未新增第三张结果表。
- 未发现external HTTP、Provider、Agent、LangGraph、Paper、LIVE或交易能力扩张。
- V13是独立forward migration，未回写V1-V12；但其兼容性与冻结schema仍有下述P1，不能accepted。

## 3. Closed findings

### 3.1 EXPIRED and tombstone

`JdbcGuardCleanupAdapter`以bounded、`FOR UPDATE SKIP LOCKED`候选和state/version条件update把符合资格的非EXPIRED记录转为EXPIRED，不物理删除identity。转换清除lease/result/failure细节，保留environment、endpoint、source、tenant、requestId、requestHash和hashVersion。same requestId + same hash返回`IDEMPOTENCY_EXPIRED / 409`，different hash返回`IDEMPOTENCY_CONFLICT / 409`；EXPIRED不能再转换到其他状态。

### 3.2 Database clock and cleanup

Production guard absolute timestamps使用PostgreSQL `transaction_timestamp()`：rate window、created/updated、TTL、retention、lease、completion/failure/expiry、heartbeat、recovery和cleanup eligibility均不接受caller absolute timestamp。Java只传validated `Duration`或数量；rate limiter的caller `Instant`被明确忽略，retry-after使用DB返回时间。Rate cleanup使用DB time、positive bounded safety grace、bounded batch、index candidate与`SKIP LOCKED`，current/future window不会被删除。Idempotency cleanup拒绝active lease，并分别使用retention/expiry资格转tombstone。

### 3.3 Lease and result boundary

V13 CHECK强制IN_PROGRESS绑定`lease_owner + lease_token + lease_expires_at`；heartbeat校验owner/token/state/version，recovery只在DB判定lease过期后按version接管。Owner为进程内随机`qdr7-<UUID>`，未见host/IP/credential内容或日志/response输出。COMPLETED reference使用固定type、tenant-bound FK、exact replay query和safe projection checksum；未保存raw body、nonce、signature、credential、prompt或Provider raw response。

### 3.4 Rollback and regression baseline

真实PostgreSQL/JDBC测试证明rate admission + audit、idempotency admission + audit在audit失败时rollback；手工`TransactionTemplate`测试证明output + audit + terminal CAS可同事务提交，FK/audit失败时无孤立output且guard不进入COMPLETED。三组规定Maven命令均通过，PostgreSQL 17.10/Testcontainers实际运行，Surefire为155 reports / 1055 tests / 0 skipped；Architecture、B1 source/HMAC/nonce和Stage-QDR-6 replay regression随full suite通过。

## 4. Remaining P1 blockers

### P1-1 V12→V13 compatibility is not complete

V12允许`stable_error_code`为任意非blank `varchar(128)`，包括带首尾空格的值；V13新增`btrim(stable_error_code) = stable_error_code`，却没有在加CHECK前拒绝性preflight、显式归一化或兼容回填。因此合法V12 FAILED历史行可能在V13 migration失败。现有升级测试只seed COMPLETED，未覆盖V12 RECEIVED、IN_PROGRESS、FAILED、EXPIRED及边界合法值。

### P1-2 Frozen schema mismatch remains

冻结schema将`result_type`定义为`varchar(32)`；V13实际新增`varchar(64)`。虽然CHECK只允许`DH_DECISION_OUTPUT`，但本任务明确规定任何V13与冻结schema不一致均BLOCKED。必须通过forward migration修正，或先完成明确的schema errata review；不得静默解释为一致。

### P1-3 Idempotency commit-unknown is still mock-only

真实test-only `DataSource/Connection`代理执行“delegate commit成功后抛`SQLException`”只覆盖rate并映射`RATE_LIMIT_COMMIT_UNKNOWN`。`74cfeb9`新增的idempotency测试使用fake `GuardTransactionBoundary`在`action.get()`后直接抛`IllegalStateException`，没有真实JDBC insert/audit/commit，也没有对已提交idempotency identity执行exact DB reconcile。因此`IDEMPOTENCY_COMMIT_UNKNOWN`仍是分类单测，不满足mandatory actual-JDBC evidence。

### P1-4 Production transaction wiring is not proven

Production使用`TransactionTemplate(PlatformTransactionManager)`，所有JDBC组件注入`JdbcTemplate`；代码方向可参与同一Spring transaction。但回归测试手工创建`DriverManagerDataSource + DataSourceTransactionManager + TransactionTemplate`并直接实例化repositories/adapters，没有启动实际Spring context、没有解析production bean、没有断言transaction manager与JdbcTemplate绑定同一DataSource，也没有排除多transaction manager选择漂移。故不存在self-invocation，但“真实production Spring wiring与同一DataSource连接”证据仍不足。

### P1-5 Mandatory lifecycle/result/concurrency evidence remains incomplete

- 未找到JVM clock大幅前后偏移测试，无法用测试证明caller/JVM clock不改变window、lease、TTL、retention、heartbeat、recovery或cleanup判断。
- 真实JDBC result测试只证明FK拒绝missing/wrong-tenant；missing/checksum mismatch到`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`由fake projector单测证明，未形成actual JDBC end-to-end duplicate路径。
- concurrent cleanup测试只覆盖rate bucket；未覆盖两个idempotency cleanup worker的`SKIP LOCKED`、CAS miss不计成功和安全重试。

### Current factsource residual

`README.md`、`docs/current/README.md`和`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`仍保留Stage-QDR-7 planning / B1 next-action口径，与当前`STATUS.md`和`WORK_ORDER.md`冲突。这三个文件不在本任务允许修改范围，本轮未越界修复；该漂移必须在后续获得明确docs allowlist后同步，且旧入口不得覆盖本review retry的`BLOCKED`结论。

## 5. Validation evidence

```text
mvn -ntp -pl dh-usecase,dh-infra,dh-security,dh-api,dh-app -am test: BUILD SUCCESS
mvn -ntp test: BUILD SUCCESS
mvn -ntp -Pquality validate: BUILD SUCCESS
Surefire: 155 reports / 1055 tests / 0 failures / 0 errors / 0 skipped
V12PersistentRuntimeGuardsFlywayPostgresTest: 14 / 0 / 0 / 0
PostgreSQL: 17.10 via Testcontainers / EXECUTED
Flyway: clean V1→V13 / executed
V12→V13: COMPLETED sample only / insufficient compatibility matrix
Checkstyle: 0 violations
Spotless: PASS
capacity benchmark: NOT RUN / NOT ALLOWED
```

## 6. Boundary confirmation

本轮只修改允许的review/current文档。未修改NQ、Java生产代码、Java测试、migration、V1-V13、API/Controller/DTO/OpenAPI、HMAC/nonce/source、Repository/JDBC；未运行capacity benchmark；未进入B3；未接external HTTP、Provider、NQ runtime、Agent或LangGraph；未触碰交易、Paper或LIVE；未push、commit或创建tag。

## 7. Next action

下一步仅允许`DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY`：以新的forward migration或先行schema errata关闭V12兼容性与`result_type`冻结漂移，并补真实JDBC idempotency after-commit unknown、production Spring wiring同DataSource、JVM clock offset、actual JDBC result mapping和idempotency concurrent cleanup证据。若同步入口factsources，必须先显式扩展docs allowlist。完成前：

```text
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_OPENAPI_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```
