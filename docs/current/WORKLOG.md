# Decision Hub Worklog

## Current work — 2026-07-16 capacity harness runtime blocker-2

`DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-RUNTIME-BLOCKER-2`在baseline HEAD `0112d493c38beeec25b3407aa504dbc129e1850c`上执行。任务前branch为`dev`、`HEAD == origin/dev`、staged为空；继承的13个current/acceptance文档修改全部位于附件允许范围，scope设计三项包含关系均为PASS。实现选择JUnit/Testcontainers单一ownership，未修改Java生产代码、application配置、migration/callback、API/contracts、冻结criteria、15个mandatory scenario或NQ。

```text
root cause: CONTAINER_START_AFTER_DYNAMIC_PROPERTY_RESOLUTION / STATIC_INITIALIZATION_ORDER
container ownership: JUNIT / TESTCONTAINERS / SINGLE STATIC POSTGRESQL
container startup order: PASS / BEFORE SPRING PROPERTY RESOLUTION
DynamicPropertySource: FROZEN JDBC URL + USERNAME + PASSWORD + MAPPED PORT ONLY
PowerShell PostgreSQL ownership: NONE
startup failure mapping: PREFLIGHT 10 / CONTEXT OR CONTAINER 20 / ARTIFACT 80
blocked summary required fields: PASS
threshold-comparison blocked artifact: PASS
manifest order / stability: PASS
PowerShell 5.1 + 7 contracts: PASS
targeted harness tests: PASS / 23 / 0 / 0 / 5 PROFILE-DISABLED SKIPPED
effective POM: PASS / ROOT + DH-APP
implementation validation run: 20260716T133710Z
Maven exit: 0
preflight: PASS / 26 OF 26
PostgreSQL container / mapped port: STARTED / 3191
ApplicationContext / Hikari / Flyway V1-V14 / dispatcher: PASS
mandatory scenarios: 0 OF 15 / NOT_RUN
capacity acceptance executed: false
summary: NOT_RUN / INTERNAL EXIT 0 / IMPLEMENTATION_VALIDATION_ONLY
artifact validation: PASS / 0 FINDINGS
summary timestamps: PASS / RFC3339 UTC
threshold comparison: NOT_RUN / FORMAL_SCENARIO_NOT_EXECUTED
manifest: PASS / 27 ENTRIES / 0 MISMATCH
secret scan: PASS / 25 FILES / 0 FINDINGS
teardown: PASS / NO RUN-ID CONTAINER OR VOLUME
full regression: PASS / 172 REPORTS / 1137 / 0 / 0 / 61 CONDITIONAL SKIPS
quality: PASS / CHECKSTYLE 0 / SPOTLESS PASS
formal capacity acceptance: NOT RUN
post-B2 capacity acceptance: BLOCKED / FORMAL RETRY REQUIRED
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY-3
```

static container在类初始化阶段显式`start()`，ready后一次性冻结Spring datasource属性；supplier不再调用未启动container。统一finalizer补齐18个required summary fields、`threshold-comparison.json`、mandatory占位、secret scan、manifest与exact-name teardown。implementation-only run验证真实Spring Context/Hikari/Flyway/Tomcat wiring后在scenario dispatcher安全短路，`0/15`且`capacityAcceptanceExecuted=false`，不形成capacity PASS。

PowerShell 5.1 native command处理同时修复Git CRLF stderr warning污染allowlist的问题；formal模式仍要求clean worktree，implementation-validation模式仅接受附件write allowlist。历史run `20260715T160710Z`的11文件、19 findings、summary缺字段与threshold文件缺失事实保持原样，只证明修复前formal runtime defect。普通full regression中的61项条件skip已披露，不能替代正式scenario；下一步只能独立执行formal retry-3。

## Historical / consumed — 2026-07-15 formal capacity acceptance retry-2 harness blocked

`DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY-2`在exact baseline HEAD `0112d493c38beeec25b3407aa504dbc129e1850c`执行只读前置检查。branch为`dev`，`HEAD == origin/dev`，task前worktree clean且staged empty；8个current factsources全部位于read/validation/scan/write范围，scope设计三项包含关系均为PASS。

```text
criteria SHA-256: d015a48e92be91b9f6b0a5f73c358405924af15044c809e48d3034ed57973cab / PASS
formal profile: qdr7-capacity-acceptance
seed: 7
host available memory at formal preflight: 17290297344 BYTES
frozen minimum: 17179869184 BYTES
environment preflight: PASS / 26 OF 26 CHECKS
formal run id: 20260715T160710Z
formal Maven command: EXECUTED ONCE
Maven exit: 1
internal exit: 80
formal status: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT
Failsafe: 1 / 0 FAILURES / 1 ERROR / 0 SKIPPED
mandatory scenarios: 0 OF 15 / NOT_EXECUTED
formal artifacts: BLOCKED / 11 FILES / 19 FINDINGS
manifest: PASS / 10 ENTRIES / 0 MISMATCH
secret scan: PASS / 6 FILES / 0 FINDINGS
current-run teardown: PASS / CONTAINER AND VOLUME ABSENT
pre-existing historical resources: 3 EXITED CONTAINERS + 3 VOLUMES / UNCHANGED
post-B2 capacity acceptance: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-RUNTIME-BLOCKER-2
```

用户释放内存并恢复Docker后，Java 21.0.9、Maven 3.9.12、Docker 29.6.1、Docker memory `24694083584` bytes、32 logical CPUs、cached `postgres:17`及实际PostgreSQL 17.10、Testcontainers 1.20.4、`MAVEN_OPTS` unset、loopback动态端口、criteria hash和formal内存`17290297344` bytes均通过。唯一formal run随后在ApplicationContext启动阶段因未启动container的mapped port被提前解析而BLOCKED；未执行scenario/regression/quality，不是capacity FAIL。Docker中预先存在3个历史run的exited container与3个volume，本轮未清理；current run container/volume已清除。没有修改Java、测试、POM、harness、criteria/config、application配置、migration/callback、API/contracts或NQ。

## Historical / consumed — 2026-07-15 capacity harness runtime binding blocker

`DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-RUNTIME-BLOCKER`在exact baseline HEAD `1d97e8549fb20d00d26a2890800c730071104c6b`执行。预检保留上一轮15个允许的文档dirty项，staged为空，8个current factsources全部位于read/validation/scan/write范围。最小复现`20260715T144344Z`确认`dh-app`同名profile没有获得root profile内属性，根因分类为`PLUGIN_CONFIGURATION_SCOPE / EXECUTABLE_PROPERTY_UNSET`。

```text
historical formal run: 20260715T140521Z / BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT
binding validation run: 20260715T145836Z
Maven exit: 1 / EXPECTED NON-ZERO
internal exit: 10
preflight: BLOCKED / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED
blockers: git-worktree, available-memory
mandatory scenarios: 0 OF 15 EXECUTED
blocked artifacts: PASS / 26 FILES / MANIFEST 0 / SECRET 0
teardown: PASS / NO RUN-ID CONTAINER OR VOLUME
PowerShell 5.1 + 7: PASS
targeted tests: PASS / 19 / 0 / 0 / 4 EXPECTED PROFILE-DISABLED
full regression: PASS / 172 REPORTS / 1134 / 0 / 0 / 0
quality: PASS / CHECKSTYLE 0 / SPOTLESS PASS
formal capacity acceptance: NOT RUN
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY-2
```

修复将`qdr7.seed`和AUTO executable选择放入可继承顶层属性，Exec使用稳定的`powershell.exe`命令名bootstrap；主入口按显式property、`pwsh.exe`、`powershell.exe`顺序解析并仅记录名称与路径hash。blocked preflight在Maven停止前生成全部mandatory artifact的`NOT_RUN`占位、summary、secret scan、manifest与teardown状态。新增PowerShell双版本contract和Java profile/argument/artifact回归；未修改生产Java、criteria/config数值、application配置、migration/callback、API/contracts或NQ。

## Historical / consumed — 2026-07-15 formal capacity acceptance retry blocked

formal run `20260715T140521Z`的runtime defect、artifact缺失与0/15结论保持历史记录，不被当前binding validation改写。

## Historical / consumed — 2026-07-15 capacity harness implementation

`DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION`已`CLOSED / ACCEPTED`。本轮以`java-backend-regression-tests`为主skill，`dh-docs-writer`负责current docs同步，按冻结work order实现Java/Failsafe + PowerShell + Maven profile混合harness、15个mandatory scenario registry、actual-wiring/database/recovery drivers、连续采样、criteria hash、统计、threshold comparator、artifact/schema/manifest/secret scan、semantic exit code与安全teardown；未执行正式capacity acceptance。

```text
current factsources: PASS / 8 OF 8 / 0 CONFLICTS
targeted harness tests: PASS / 23 / 0 / 0 / 4 EXPECTED PROFILE-DISABLED
full regression: PASS / 172 SUREFIRE SUITES / 1133 / 0 / 0 / 0
quality: BUILD SUCCESS / 19 OF 19 / SCOPED HARNESS CHECKSTYLE 0 / SPOTLESS PASS
forbidden-scope diff: 0
Capacity harness implementation: CLOSED / ACCEPTED
Formal profile: qdr7-capacity-acceptance
Post-B2 capacity acceptance: BLOCKED / EXECUTION NEXT
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY
```

实现新增root/`dh-app` profile、`config/qdr7-capacity/**`、`scripts/qdr7-capacity/**`与`dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/**`，没有修改production Java、application配置、migration/callback、API/contracts、NQ、Provider、Agent/LangGraph或LIVE。Windows PowerShell 5.1和PowerShell 7 contract tests均exit 0；dirty-worktree blocked preflight返回内部exit 10并产出最小blocked artifacts；effective POM包含Exec/Failsafe/formal IT绑定。完整普通回归19/19 Reactor `SUCCESS`，正式profile未激活。

## Historical / consumed — 2026-07-15 capacity criteria freeze retry

- 预检确认repository `E:/Project/decision-hub`、branch `dev`、HEAD `9212047ab473a67f7bbdc8729e99495be6698946`且与`origin/dev`一致；task前worktree clean、staged empty，三个scope包含关系全部PASS。
- 使用`nq-dh-workflow-router`分类为`DOCUMENTATION`，以`dh-docs-writer`执行docs-only freeze；未调用外部connector/plugin。
- 建立evidence ledger：排除`20260713T213431`与`20260714T012507`；`20260714T210052`只使用修复后rate/quota/isolation/nonce/idempotency/tenant cleanup/pressure/resource slice；recovery与restart只使用`20260714T154500Z`。
- Rate按每并发点3轮观测冻结：throughput floor使用min×0.80向下0.1；p50/p95/p99/max ceiling使用max×1.25向上10 ms。未删除离群样本。
- 冻结cold-start quota 3轮10/30、tenant/environment隔离、canonical source fail-closed、nonce 3轮1/23与idempotency lifecycle零容忍合同。
- 冻结tenant-scoped cleanup 10/100/1000、2 workers、1 writer、batch 10，duration ceiling 200/200/1100 ms；跨scope、active/locked/not-expired与duplicate deletion均0容忍。
- 冻结same-pool recovery 3轮：database ready 800 ms、Hikari recovery 5800 ms、protected request recovery 5800 ms；outage false 2xx=0，post-recovery 8并发/100个2xx。
- 冻结normal/pressure/outage/recovery/post-load序列、Hikari/PostgreSQL阈值、restart 3+3、full regression/resources与本地environment baseline。所有数值说明source、sample count、min/median/max、公式、rounding、margin与限制。
- 冻结formal harness合同：profile `qdr7-capacity-acceptance`、seed 7、唯一UTC run-id、`target/qdr7-capacity-acceptance/<run-id>/`、CSV/JSON/percentile/metrics/comparison/manifest/secret scan和fail-closed退出语义；本轮未实现或执行harness。
- 新增`DH_STAGE_QDR_7_B2_CAPACITY_CRITERIA_FREEZE_REVIEW.md`，同步8个current factsources、ROADMAP、TESTING、WORKLOG、implementation work order、threshold evidence与post-implementation acceptance。
- `mvn -ntp -Pquality validate`真实通过：19/19 Reactor SUCCESS、root Checkstyle 0 violations、Spotless PASS。Forbidden-scope diff为0，current fact conflict count=0。

```text
CAPACITY_ACCEPTANCE_CRITERIA_FREEZE: DONE / ACCEPTED
EVIDENCE_COMPATIBILITY: PASS
NUMERIC_THRESHOLDS: FROZEN
ENVIRONMENT_BASELINE: FROZEN
HARNESS_CONTRACT: FROZEN / NOT_IMPLEMENTED
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED / PENDING HARNESS AND EXECUTION
ALLOW_CAPACITY_HARNESS_WORK_ORDER: YES / NEXT_TASK_ONLY
ALLOW_CAPACITY_HARNESS_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_ACCEPTANCE_EXECUTION_NOW: NO
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER
```

> Historical / consumed：从下一节开始保留same-pool recovery及更早worklog；旧criteria状态与next action不得覆盖本节current disposition。

## 2026-07-14 DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER

- 预检确认仓库`E:/Project/decision-hub`、分支`dev`、HEAD `e2cb1ff966eb611f05d3703893fab002ea6142b4`、staged empty；23个inherited dirty files全部保留，未执行reset/checkout/restore/clean。8个current factsources全部纳入read/validation/scan/write scope，`TASK_SCOPE_DESIGN: PASS`、初始`CURRENT_CONFLICT=0`。
- 先修正`FACTSOURCE_POLICY.md`旧active task/next task/1101口径；旧值保留为Historical / Previous attempt / Superseded current state / Consumed evidence。Primary authority保持`STATUS.md`与`WORK_ORDER.md`。
- RCA确认上一轮probe的`127.0.0.1::5432`随机宿主端口在容器stop/start后可能重新分配，应用DataSource仍持有启动时JDBC URL；旧probe只检查容器内`pg_isready`，未比较endpoint或direct JDBC。根因分类为`PROBE_CONTAINER_ENDPOINT_CHANGED / RECOVERY_PROBE_INVALID`。
- 代码现实检查确认persistent guard通过`JdbcTemplate`逐次获取连接，不缓存`Connection`或事务对象；production `application*.yml`没有特殊Hikari recovery参数。本任务未修改production Java或配置。
- 新增`DecisionDryRunSamePoolRecoveryPostgresTest`，使用固定loopback endpoint、同一ApplicationContext/DataSource/Hikari pool、同一PostgreSQL容器和persistent volume。首次编译因AssertJ版本不支持`isNotBetween`失败；第二次因数据库已停止时probe错误查询idempotency失败；均只做test probe最小修正并保留失败事实。
- 最终run `20260714T154500Z`完成same-pool recovery 3/3：database ready `487–512 ms`，protected 2xx恢复`3792–3864 ms`。Outage 9个真实请求全部`5xx / UNKNOWN_ERROR`、false 2xx=0；已提交nonce/idempotency、tenant隔离、PromptVersion canonical row保持，未提交事务未恢复为成功。
- Hikari/PostgreSQL连续序列37 rows，三轮outage各9个采样点，最大间隔1048 ms；恢复后8并发/100请求全部2xx、0个4xx/5xx。Spring Context restart与PostgreSQL same-container persistent-volume restart分别3/3。
- 定向recovery/persistent-guard/actual-wiring集合12项通过；累计PromptVersion/cleanup/rate-limit/persistent-guard/DecisionDryRun组合回归`BUILD SUCCESS`。完整`mvn -ntp test`为19/19 reactor SUCCESS、166 suites、1114/0/0/0。
- 完整回归资源采样捕获Surefire 380 rows/7 PIDs；每个发现fork至少1条样本，覆盖module/start/end/working set/private bytes/CPU，只采集owned Maven后代Java进程。`mvn -ntp -Pquality validate`通过，Checkstyle 0 violations，Spotless PASS。
- Final evidence位于ignored `target/postgresql-same-pool-recovery/20260714T154500Z/`；secret scan 11 files/8 patterns/0 findings，SHA-256 manifest 14 entries/0 mismatch。历史三个capacity evidence目录保持不变。
- Candidate threshold evidence现为`SUFFICIENT`，只开放下一任务`DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`。Criteria尚未冻结，formal harness、capacity acceptance、B3、API、外部HTTP/provider、NQ、Agent/LangGraph与LIVE继续禁止。

```text
POSTGRESQL_SAME_POOL_RECOVERY: CLOSED / ACCEPTED
HIKARI_POSTGRESQL_SERIES: PASS
RESTART_REPRODUCIBILITY: PASS / 3 OF 3 + 3 OF 3
FULL_REGRESSION: PASS / 1114 / 0 / 0 / 0
CANDIDATE_THRESHOLD_EVIDENCE: SUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: YES / NEXT_TASK_ONLY
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY
```

> Historical / consumed：从下一节开始保留capacity scenario retry、threshold evidence retry-2及更早worklog；旧`BLOCKED`、`next action`和旧测试总数不得覆盖本节current disposition。

## 2026-07-14 DH-STAGE-QDR-7-B2-CAPACITY-SCENARIO-EVIDENCE-BLOCKER-RETRY

- 预检确认仓库`E:/Project/decision-hub`、分支`dev`、HEAD `e2cb1ff966eb611f05d3703893fab002ea6142b4`、staged empty；继承14个current/entry文档修改，未发现write allowlist外dirty path。
- `JdbcPromptVersionRepository`改用无目标`ON CONFLICT DO NOTHING`，insert后读取canonical row并比较稳定身份/语义内容；删除`SELECT -> INSERT -> DuplicateKeyException -> same-transaction SELECT`，相同定义幂等返回canonical row，不同定义fail-closed。
- `GuardCleanupCommand`要求明确tenant，拒绝null、blank与`*`；`JdbcGuardCleanupAdapter`的rate/idempotency候选SQL增加`tenant_id = ?`。未新增scheduler、API或global cleanup bypass。
- 增补真实PostgreSQL PromptVersion并发/语义冲突、cleanup tenant isolation与command校验测试；V12 commit-unknown测试fixed-window从60秒调整为3600秒，仅消除跨分钟suite不稳定，不修改production rate contract。
- 定向测试、40项PostgreSQL suite与fresh `dh-app` package通过；actual-wiring preflight 13/13、rate matrix 15/15、cold-start quota 3/3通过。HTTP parser对无`decisionId`非2xx的StrictMode误判已只在`target`临时脚本修正。
- Cleanup临时driver首次因旧构造参数顺序fail-closed；保留失败日志后只修正`target` driver。正式10/100/1000、2 workers、1 writer、batch 10全部PASS，跨tenant/environment、active、locked、not-expired与duplicate deletion均为0。
- PostgreSQL unavailable期间protected请求fail-closed且无内存fallback。恢复后同一ApplicationContext/同一Hikari pool在规定60秒内没有合法protected 2xx；脚本按60次请求而非elapsed deadline继续采样，晚恢复不计PASS。未修改datasource/global config，restart下游证据未完成。
- Full-regression sampler先后暴露空CSV StrictMode、Windows短生命周期parent process和PowerShell runtime-specific SHA-256问题；失败attempt与probe均保留。最终exact-repository Maven + Surefire descendant采样通过，未持久化原始command line。
- `mvn -ntp test`最终为19/19 reactor SUCCESS、1110/0/0/0；PostgreSQL/Testcontainers为8 suites/60 tests/0 skipped；资源baseline PASS。`mvn -ntp -Pquality validate`为19/19 SUCCESS。
- Evidence root `target/capacity-scenario-evidence/20260714T210052/`完成secret scan 117 files/8 patterns/0 findings与SHA-256 manifest 118 entries/0 mismatch；历史两个capacity-threshold目录未修改。
- 本轮未修改V1–V14 migration、Flyway callback、API/Controller/DTO/OpenAPI/contracts、HMAC/timestamp/nonce/source外部合同、datasource/global config、NQ、Provider、Agent/LangGraph或LIVE；未实现formal harness，未commit/push/tag。

```text
PROMPT_VERSION_ATOMIC_BOOTSTRAP: CLOSED / ACCEPTED
CLEANUP_TENANT_SCOPED_CONTRACT: CLOSED / ACCEPTED
COLD_START_QUOTA: PASS / 3 OF 3
CLEANUP_CAPACITY_EVIDENCE: PASS / 10 + 100 + 1000
FULL_REGRESSION_RESOURCE_BASELINE: PASS
POSTGRESQL_SAME_POOL_RECOVERY_BLOCKED
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
next action: wait for a formally assigned same-pool recovery task; do not invent a task ID
```

> Historical / consumed：从下一节开始保留threshold evidence retry-2及更早worklog；旧`next action`不得覆盖本节current disposition。

## 2026-07-14 DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2

- 预检确认仓库`E:/Project/decision-hub`、分支`dev`、HEAD `e2cb1ff966eb611f05d3703893fab002ea6142b4`、tracked worktree clean、staged empty；`origin/dev...HEAD = 0/0`，用户输入的ahead 1已发生漂移。
- 使用新run `20260714T012507`、localhost Boot jar、专用PostgreSQL 17.10容器和持久volume；临时token、HMAC secret、数据库密码、nonce、requestId和signature不打印、不持久化，未访问外部HTTP。
- Actual-wiring preflight为5/5 sequential + 8/8 concurrent structured 2xx；完整rate matrix 15/15轮、1500 measured requests全部完成。记录throughput `8.582 / 32.775 / 92.667 req/s`和p50/p95/p99 `107.671 / 212.887 / 275.611 ms`，未将其冻结为threshold。
- Cold-start quota三轮均为3个2xx、30个429、7个503，DB winner严格为10且oversell为0；warmed retry三轮为10 accepted/30 rejected/0 unexpected。数据库rate bucket原子性通过，但用户要求的end-to-end quota scenario失败。
- Tenant/environment isolation与`403 / SOURCE_DENIED`通过；same-nonce race 3/3轮均为1 accepted/23 replay/1 DB winner；idempotency lifecycle 3轮合计75/0/0/0。
- Production `JdbcGuardCleanupAdapter`完成10/100/1000规模、96 timeline rows、2 workers、1 writer、batch 10和duration 59/51/410 ms。Actual cleanup合同按environment/endpoint/source，不含tenant；other-tenant eligible deletion为1/10/100，因此任务要求的cross-tenant safety失败。
- Contention覆盖c=8/16 normal和lock pressure，Hikari pending max 13、PostgreSQL lock waits max 4；temporary DB unavailable请求fail-closed为`500 / UNKNOWN_ERROR`且无partial state，但同一pool未在30次bounded retry内恢复。Sampler在DB unavailable空结果上触发StrictMode错误，recovery series不完整。
- Spring ApplicationContext与PostgreSQL persistent-volume restart各3/3通过，committed nonce/idempotency继续受保护，uncommitted transaction未恢复为success，tenant isolation保持。
- Exact `mvn -ntp test`为19/19 Reactor SUCCESS、1101/0/0/0、PostgreSQL/Testcontainers实际执行、无native-memory OOM；host/Maven JVM/Docker资源已采样，但Surefire JVM rows=0，因此full-regression resource baseline不完整。
- `mvn -ntp -Pquality validate`为19/19 SUCCESS、Checkstyle 0 violations、Spotless PASS。Secret scan为106 files/8 patterns/0 findings；SHA-256 manifest为107 entries、0 mismatch。
- 历史run `20260713T213431`的首个`200`、第二请求`500 / UNKNOWN_ERROR`、provider-profile mismatch根因和0轮rate matrix保持不变；path fix commit `e2cb1ff966eb611f05d3703893fab002ea6142b4`保持`CLOSED`。
- 本轮未修改Java生产代码、Java测试、migration、callback、V1–V14、API/Controller/DTO/OpenAPI/contracts、HMAC/nonce/tenant/source合同或NQ；未实现formal harness，未访问外部Provider，未进入B3，未commit/push/tag。

```text
THRESHOLD_EVIDENCE_COLLECTION: BLOCKED
ACTUAL_WIRING_PROTECTED_2XX: PASS
RATE_MATRIX: PASS / 15 OF 15
QUOTA_ATOMICITY: FAIL
TENANT_ENVIRONMENT_ISOLATION: PASS
CANONICAL_SOURCE_FAIL_CLOSED: PASS
REPLAY_NONCE_RACE_EVIDENCE: PASS
IDEMPOTENCY_LIFECYCLE_EVIDENCE: PASS
CLEANUP_BACKLOG_EVIDENCE: FAIL
CLEANUP_PROTECTED_ROW_SAFETY: FAIL
POSTGRESQL_CONTENTION_EVIDENCE: FAIL
HIKARI_SERIES: FAIL / INCOMPLETE
RESTART_PERSISTENCE_EVIDENCE: PASS
FULL_REGRESSION_RESOURCE_BASELINE: FAIL
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 0 CONFLICTS
QUALITY_GATE: PASS
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-SCENARIO-EVIDENCE-BLOCKER
```

> Historical / consumed：从下一节开始保留calibration path blocker及更早任务的真实记录；其旧`next action`不得覆盖上方retry-2 current worklog。

## 2026-07-13 DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER

- 预检确认`dev`、task前HEAD `962b348761e4837f7aa433f45ba567201d8e67d1`、staged为空；工作区只有用户声明可继承的14个current/entry文档和1个untracked上一轮证据报告，三个scope包含关系成立，范围外技术diff为0。
- 代码审计确认`ProviderProfile.id`是lookup key，tenant/provider/capability/status/trust字段组成immutable config equivalence；`createdAt`为创建审计元数据，但既有registry通过record equality比较完整snapshot。`ProviderProfile`没有model字段，model identity/version由独立`ModelVersion`合同承担。
- 根因为`DefaultQdrMockModelGatewayBaseline.prepare`每次使用请求级`clock.instant()`重建相同stable ID、不同`createdAt`的profile，触发严格registry mismatch。选择最小修复：bean构造时只读取一次`Clock`并保存`final baselineCreatedAt`，后续重复与并发bootstrap复用确定时间；未修改`ProviderProfile`或`InMemoryProviderProfileRegistry`，未放宽冲突检测。
- 新增baseline直接测试，覆盖连续两次、stable ID/provider/model/config/createdAt以及8-worker起跑屏障并发；新增registry直接测试，逐字段验证provider kind/key、capability、status、trust policy与`createdAt`冲突fail-closed且不得覆盖原profile。
- 新增PostgreSQL actual-wiring回归：`@SpringBootTest(RANDOM_PORT)`显式绑定`127.0.0.1`，同一Context完成5次顺序与8-worker同步起跑并发请求；13/13均为结构化`200`，nonce/idempotency/rate数据库证据各覆盖13次请求，`qdr_model_gateway_call`为13条`MOCK/SUCCEEDED`、0条`FAILED`。
- 定向测试首次因`dh-usecase`无Mockito依赖而test compile失败；在不新增依赖的前提下改用advancing `Clock`和纯Java无状态fake。下一次运行进入actual-wiring后又暴露测试错误期待内部`MODEL_GATEWAY_MOCK_CALL`出现在外部response；改为对外`NO_TRADE`/`MOCK_NO_TRADE`合同和gateway-call数据库证据，最终定向命令`BUILD SUCCESS`。
- 指定PostgreSQL persistent guard suite跨Reactor共41 tests（`dh-app` 38 + `dh-infra` 3）、0 failures/errors/skipped，PostgreSQL 17.10与V12–V14/callback compatibility实际执行；`mvn -ntp -pl dh-app -am package`未跳过测试并成功生成Boot jar。
- 第一次临时probe已实际发出5次顺序+8次并发且应用日志均为`200`，但Windows锁定Java stdout导致证据读取脚本exit 1；保留失败run，使用新runId、新container/volume和`FileShare.ReadWrite`最小修复脚本后完整重跑。
- 成功probe在单一jar PID/一次Spring Context下取得13/13结构化2xx，0个5xx、`UNKNOWN_ERROR`或profile mismatch，13个唯一requestId/nonce/HMAC，PostgreSQL rate bucket/idempotency/nonce为1/13/13；未执行正式capacity matrix，target artifacts未纳入Git。
- 完整`mvn -ntp test`为19/19、1101 tests、0 failures/errors/skipped，PostgreSQL/Testcontainers实际执行；`mvn -ntp -Pquality validate`为19/19、Checkstyle 0 violations、Spotless PASS。
- 上一轮证据报告保持原样：首个protected `200`、同一Context第二请求`500 / UNKNOWN_ERROR`、0轮rate matrix、cleanup/contention证据不完整及`commit: NO / EVIDENCE_INSUFFICIENT`均作为历史事实保留。
- 本轮未修改NQ、callback、V1–V14、API/Controller/DTO/OpenAPI/contracts、HMAC/nonce/tenant/source合同；未接外部HTTP/真实Provider、NQ、Agent/LangGraph、Paper或LIVE，未实现正式capacity harness，未进入B3，未push/tag。

```text
CAPACITY_CALIBRATION_PATH_BLOCKER: CLOSED
PROFILE_IDENTITY_CONTRACT: PASS
DETERMINISTIC_BASELINE_PROFILE: PASS
STRICT_REGISTRY_CONFLICT_DETECTION: PASS
REGISTRY_OVERWRITE_PREVENTION: PASS
REPEATABLE_PROTECTED_2XX: PASS
UNKNOWN_ERROR_REGRESSION: PASS
POSTGRESQL_REGRESSION: PASS
FULL_REGRESSION: PASS / 1101 TESTS
QUALITY_GATE: PASS
CAPACITY_THRESHOLD_EVIDENCE: BLOCKED / RETRY REQUIRED
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT / PREVIOUS RUN INVALID FOR RATE MATRIX
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
ALLOW_CAPACITY_THRESHOLD_EVIDENCE_RETRY_2: YES / NEXT_TASK_ONLY
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2
```

> Historical / consumed：从下一节开始保留上一轮threshold evidence retry及更早任务的真实记录；旧`next action`、第二请求失败、0轮matrix和旧测试总数不得覆盖本节current worklog。

## 2026-07-13 DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY

- 预检确认`dev`、HEAD `962b348761e4837f7aa433f45ba567201d8e67d1`、worktree/staged clean；三个scope包含关系成立，task前技术diff为0。
- 使用exact-HEAD Boot jar与专用PostgreSQL 17.10容器/持久volume，仅绑定`127.0.0.1:18137`；临时token、HMAC secret、数据库密码、nonce、requestId和signature只在内存中生成，未持久化。
- 实际`POST /api/ai/decision-dry-runs`取得1个经过完整安全链的`200`；同一Spring Context第二个新nonce/requestId合法请求因`InMemoryProviderProfileRegistry`的provider profile bootstrap mismatch返回`500 / UNKNOWN_ERROR`。
- 因warm-up在第2请求前失败，1/2/4/8/16、每点3轮、warm-up 20、measured 100的rate matrix为0 completed rounds；未把500/rejection latency写成throughput或tail-latency evidence。
- Actual localhost same-nonce race为8线程/24 attempts/1轮：1个`200`、23个`409 NONCE_REPLAY`、0 unexpected errors，PostgreSQL最终1行；非canonical source为`403 SOURCE_DENIED`。
- Exact-HEAD PostgreSQL correctness suites为25 tests、0 failures/errors/skipped，覆盖rate exact winner、tenant/environment隔离、idempotency lifecycle、rollback、commit-unknown、cleanup及hard ceiling；这些结果未提升为capacity threshold。
- Spring Context与PostgreSQL persistent-volume restart后committed replay继续`409 NONCE_REPLAY`；database unavailable请求`500 UNKNOWN_ERROR`且恢复后nonce行0，health恢复`UP`。
- Cleanup production path correctness通过，但缺并发writer、duration和持续backlog timeline；PostgreSQL contention缺持续lock-wait、connection-acquire和Hikari saturation序列。
- 完整`mvn -ntp test`为19/19、1091 tests、0 failures/errors/skipped，Maven Total time `06:01 min`、resource sampler wall `363 s`，Surefire正常退出、无native-memory OOM；PostgreSQL 17.10/Testcontainers实际执行。
- `mvn -ntp -Pquality validate`为19/19 SUCCESS、Checkstyle 0、Spotless PASS。
- 证据目录含全部必需artifact，manifest 65 entries，secret scan 64 files/8 patterns/0 findings；target未stage。
- 新增threshold evidence报告并同步current facts；未修改Java、测试、migration、callback、API/contracts或NQ，未实现正式harness，未进入B3，未commit/push/tag。

```text
THRESHOLD_EVIDENCE_COLLECTION: BLOCKED
ACTUAL_WIRING_PROTECTED_2XX: PASS / SINGLE_SAMPLE
RATE_LIMIT_CALIBRATION: FAIL / WARM_UP_PREREQUISITE
REPLAY_NONCE_RACE_EVIDENCE: PASS
IDEMPOTENCY_LIFECYCLE_EVIDENCE: PASS / CORRECTNESS_ONLY
CLEANUP_BACKLOG_EVIDENCE: FAIL
POSTGRESQL_CONTENTION_EVIDENCE: FAIL / INCOMPLETE
RESTART_PERSISTENCE_EVIDENCE: PASS
FULL_REGRESSION_RESOURCE_BASELINE: PASS
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER
```

> Historical / consumed：从下一节开始均为更早任务当时的真实记录，其旧`next action`、BLOCKED原因和测试总数不得覆盖文件顶部current worklog。

## 2026-07-13 DH-STAGE-QDR-7-B2-GUARD-CONFIGURATION-BYPASS-BLOCKER

- 预检确认`dev`、task前HEAD `7c69ad3b4846eabf0cab04be18feeccda040d3cb`；仅继承上一任务允许的4个Java/测试变更，staged为空，三个scope包含关系成立。
- 新增无状态、无Spring注解的`PersistentGuardHardCeilings`，唯一声明rate window `3600`秒、quota `100000`、idempotency lease `900`秒；`DecisionDryRunGuardProperties`与`RateLimitAdmissionCommand`共同引用，移除两处独立数字。
- 新增`RateLimitAdmissionCommandTest`，覆盖1、冻结最大值、0、max+1、缺失/非法identity以及超限不调用adapter；保留并完成properties、Spring binding/wiring与source合同回归。
- production-equivalent PostgreSQL wiring确认合法最大值到达`JdbcRateLimitAdmissionAdapter`，window/quota max+1在构造阶段失败且不增加bucket行；无in-memory fallback。
- 定向24项、persistent guard PostgreSQL 41项、完整1091项均为0 failures/errors/skipped；PostgreSQL 17.10/Testcontainers与V1–V14实际运行，完整回归无native-memory OOM。
- `mvn -ntp -Pquality validate`通过，19/19 modules、Checkstyle 0 violations、Spotless PASS。
- Hard-ceiling contract与configuration bypass关闭；capacity criteria、post-B2 capacity acceptance继续`BLOCKED`，未执行calibration、未实现harness、未进入B3。

```text
GUARD_CONFIGURATION_BYPASS_BLOCKER: DONE
SINGLE_HARD_CEILING_AUTHORITY: PASS
HARD_CEILING_CONFLICT_COUNT: 0
FULL_REGRESSION: PASS / 1091 TESTS
QUALITY_GATE: PASS
B2_IMPLEMENTATION_STATUS: ACCEPTED
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY
```

> Historical / consumed：从下一节开始均为更早任务当时的真实记录，其旧`next action`、BLOCKED原因和测试总数不得覆盖文件顶部current worklog。

## 2026-07-13 DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE

- 预检确认`dev`、HEAD `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c`；工作区只含上一任务允许继承的12个文档和1个BLOCKED报告，无Java、测试、callback、V1–V14、API/OpenAPI/contracts或NQ diff。
- 使用`nq-dh-workflow-router`分类为`DOCUMENTATION`，并按`dh-docs-writer`只修改附件allowlist内current文档；三个scope包含关系成立。
- 对比current B1合同、生产校验代码、配置、B1 resource evidence、correctness tests、Maven/Surefire和Testcontainers配置；未运行capacity、targeted PostgreSQL suite或full regression。
- 可追溯的correctness常量包括rate 8线程/40 attempts/quota 10、idempotency 8线程/24 attempts/1 winner、cleanup 2 workers/batch 10；这些数值不具备throughput、tail latency、duration或重复轮次证明。
- B1 diagnostic matrix虽尝试1/2/4/8/16并发、20次warm-up和100次measured attempts，但有效2xx样本为0，原报告已排除全部latency/throughput结果。
- 发现hard-ceiling authority冲突：B1 rate window/quota/lease为3600秒/100000/900秒，当前代码允许86400秒/1000000/3600秒；cleanup代码上限1000行比B1文档10000行更严格。本轮未修改代码或既有B1文档。
- 新增`DH_STAGE_QDR_7_B2_CAPACITY_ACCEPTANCE_CRITERIA.md`，记录可追溯数值、mandatory gap、证据分离、OOM规则和blocker要求；最终为`CAPACITY_THRESHOLD_JUSTIFICATION_INSUFFICIENT_BLOCKED`，不是accepted criteria。
- 为上一轮BLOCKED报告添加follow-up disposition，原始38项correctness证据、两次OOM、hash与原始结论保持不变。
- `mvn -ntp -Pquality validate` exit 0，19/19 `SUCCESS`、Checkstyle 0 violations、Spotless通过、总耗时6.149秒。
- Current factsources统一为criteria blocked、harness未实现且被criteria阻断、post-B2 capacity pending criteria/harness、B3 not allowed；下一任务仅为threshold evidence blocker。

```text
CAPACITY_ACCEPTANCE_CRITERIA_FREEZE: BLOCKED
CAPACITY_THRESHOLD_JUSTIFICATION_INSUFFICIENT_BLOCKED
CAPACITY_CRITERIA_AUTHORITY: FAIL
PROJECT_ACCEPTANCE_BASELINE: BLOCKED
HARNESS_CONTRACT: BLOCKED
ENVIRONMENT_PREFLIGHT: BLOCKED
QUALITY_GATE: PASS
B2_IMPLEMENTATION_STATUS: ACCEPTED
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED / PENDING CRITERIA AND HARNESS
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-BLOCKER
```

## 2026-07-13 DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE

- 预检确认`dev`、HEAD `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c`、worktree/staged clean、accepted baseline即当前HEAD；`origin/dev...HEAD=0/0`与任务预期`0/1`不一致，但无技术提交漂移。
- 三个scope包含关系全部成立；task前未发现Java生产/测试、callback、V1–V14、API/OpenAPI/contracts或NQ diff。
- 扫描current work order、B1 capacity resolution/evidence、配置、生产实现和测试；确认只冻结absolute hard ceiling与future harness合同，measured defaults仍`NOT_SELECTED`，没有正式容量命令、测量轮次、吞吐/延迟阈值或metrics-complete actual-wiring 2xx harness。
- 真实执行persistent guard相关PostgreSQL 17.10/Testcontainers suite：38 tests、0 failures、0 errors、0 skipped；rate/idempotency原子竞争、tenant/environment隔离、cleanup、callback timeout/rollback、commit-unknown与nonce restart correctness通过。
- same-nonce concurrent race、callback/cleanup capacity、throughput、p50/p95/p99、pool saturation与正式pressure rounds没有现有harness，未伪造或推导数值。
- 两次完整`mvn -ntp test`均在`dh-app` Surefire fork因本机native-memory OOM中止；诊断日志移入root `target/**`并记录hash。未把旧1076项证据写成本轮PASS。
- `mvn -ntp -Pquality validate` exit 0，19/19 `SUCCESS`、Checkstyle 0 violations、Spotless通过、总耗时11.494秒。
- 最小同步current factsources与本报告；未修改Java、测试、callback、migration、API/contracts、HMAC/nonce/tenant/source合同或NQ，未进入B3，未push、未tag。

```text
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
CAPACITY_CRITERIA_FROZEN: FAIL
CAPACITY_HARNESS_AVAILABLE: FAIL
REAL_POSTGRESQL_EXECUTION: PASS
FULL_REGRESSION: FAIL / JVM_NATIVE_MEMORY_OOM
QUALITY_GATE: PASS
B2_IMPLEMENTATION_STATUS: ACCEPTED
Stage-QDR-7 B3: NOT_ALLOWED
next action: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE
```

## 2026-07-13 DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE

- 预检确认仓库、分支和task前HEAD；staged为空，dirty仅为用户预告的current review变更，未发现技术范围dirty。
- 读取8个current factsources、Stage-QDR-7 implementation work order、consolidated review、STATUS/ROADMAP/WORK_ORDER/TESTING/WORKLOG和archive/factsource入口；未把historical文档升级为current authority。
- 将8个current factsources统一为B1 `FROZEN`、B2 `CLOSED / ACCEPTED`、schema errata `ACCEPTED`、persistent guards `ACCEPTED`、post-B2 capacity acceptance `NOT_STARTED / NEXT`、B3 `NOT_ALLOWED`。
- 在同一consolidated review中保留初始`BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT`及完整技术证据，记录6-file allowlist root cause、同任务scope扩展与最终`PASS / B2 ACCEPTED`。
- 将三个scope包含关系、`TASK_SCOPE_DESIGN_INVALID`和review同任务收口规则同步至AGENTS、CODEX、FACTSOURCE_POLICY与WORK_ORDER。
- 8-file状态扫描分类为`CURRENT_ALIGNED=70`、`HISTORICAL_MARKED=202`、`NEGATIVE_SAFETY_STATEMENT=18`、`CURRENT_CONFLICT=0`。
- 仅修改用户允许的文档；未修改Java、测试、callback、V1–V14、API/OpenAPI、HMAC/nonce/source合同或NQ，未运行capacity benchmark，未进入B3。
- `mvn -ntp -Pquality validate` exit 0，Reactor 19/19 `SUCCESS`、Checkstyle 0 violations、Spotless通过、总耗时6.276秒；full tests与PostgreSQL/Testcontainers均`NOT_RERUN`，仅复用1076项与PostgreSQL 17.10真实执行证据。

```text
STAGE_QDR_7_B2_FACTSOURCE_ALIGNMENT_AND_FINAL_ACCEPTANCE: DONE
TASK_SCOPE_DESIGN: PASS
VALIDATION_WRITE_ALLOWLIST_ALIGNMENT: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 0 CONFLICTS
SCHEMA_ERRATA_IMPLEMENTATION: ACCEPTED
PERSISTENT_GUARDS_IMPLEMENTATION: ACCEPTED
B2_IMPLEMENTATION_STATUS: ACCEPTED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: YES / NEXT_TASK_ONLY
next action: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE
```

## Previous review attempt — Historical / BLOCKED preserved

以下记录是同一consolidated review的初始尝试，不是当前验收结论；其`BLOCKED`事实与当时技术证据保持原样。

## 2026-07-13 DH-STAGE-QDR-7-B2-CONSOLIDATED-FINAL-ACCEPTANCE-REVIEW

- 预检确认`dev`、HEAD `04a98a8a09866bc0dd20a0fbbbdc8b12ca6c175c`、worktree/staged clean、`origin/dev...HEAD=0/0`，关键提交均存在。
- 独立复核callback、V12–V14、persistent rate/idempotency、CAS/tombstone、DB clock、bounded cleanup、result reference、Spring completion transaction、commit unknown与Controller guard顺序；没有发现新的实现、安全、事务、migration或tenant P0/P1。
- 真实PostgreSQL 17.10/Testcontainers回归确认5秒precheck lock timeout、60秒statement timeout、整体rollback、解锁retry、session isolation与完整migration矩阵，0 skipped。
- targeted 15/15、全仓19/19与quality 19/19均`BUILD SUCCESS`；全仓XML为158 reports / 1076 tests / 0 failures / 0 errors / 0 skipped；Checkstyle 0、Spotless通过。
- 8个指定current入口中有6个仍指向旧schema-errata alignment/retry，且不在本轮allowlist；未越界修改。由于`current conflict count`无法达到0，consolidated acceptance必须阻断。
- 仅新增consolidated review并同步允许的STATUS/WORK_ORDER/ROADMAP/TESTING/WORKLOG；未修改生产代码、测试、callback、V1–V14、API/OpenAPI或安全合同。

```text
STAGE_QDR_7_B2_CONSOLIDATED_FINAL_ACCEPTANCE_REVIEW: BLOCKED
SCHEMA_ERRATA_TECHNICAL_REVIEW: PASS
PERSISTENT_GUARDS_TECHNICAL_REVIEW: PASS
CURRENT_FACTSOURCE_CONSISTENCY: FAIL / 6 FILES
B2_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
next action: DH-STAGE-QDR-7-B2-CONSOLIDATED-BLOCKER-FIX
```

## Other historical worklog records — 以下全部内容均为非当前任务

## 2026-07-13 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY

- 基线确认`dev`、HEAD `d827fce`、worktree/staged clean；上一轮review已作为独立commit保留。
- 仅对齐root/current README、CODEX、FACTSOURCE_POLICY、CLAUDE、AGENTS及current authority docs；未修改代码、测试、callback或migration。
- `STATUS.md`与`WORK_ORDER.md`明确为当前状态与下一任务主权威；README、CODEX、CLAUDE、AGENTS只作为入口/执行指导。
- Stage3-B3、Integration-0-PLAN、旧Stage-QDR-4入口、Stage-QDR-7 implementation-not-started与旧work order均删除或移入明确historical/consumed区域。
- 历史`BLOCKED` review原文未修改；B2保持未accepted，capacity acceptance与B3继续禁止。
- 指定stale wording scan共378个命中，`HISTORICAL_MARKED=323`、`NEGATIVE_SAFETY_STATEMENT=55`；严格旧current入口pattern为0。
- forbidden-scope diff、callback diff、V1–V14 diff、Java生产/测试diff与API/contracts diff均为空；历史BLOCKED review零diff。
- `mvn -ntp -Pquality validate`为19/19 `BUILD SUCCESS`、Checkstyle 0、Spotless通过；Maven tests和PostgreSQL/Testcontainers按附件要求未重跑。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_BLOCKER_FIX_RETRY: DONE
schema errata technical implementation: PASS
schema errata independent acceptance: BLOCKED only by factsource drift
callback/migration code changes: NOT_REQUIRED
CURRENT_FACTSOURCE_CONSISTENCY: PASS
ALLOW_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY_2: YES
ALLOW_MILESTONE_REVIEW_RETRY_3_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2
```

下方日期记录保留历史事实；其旧`current task`、`next action`或阶段状态不得覆盖本节。

## 2026-07-13 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY

- 预检确认`dev`、HEAD `479dbcab6b4a29a005cb524afc5a146d224ae2af`、worktree/staged clean、`HEAD == origin/dev`。
- 只读审查`6a1794d..479dbc`：callback仅前移冻结timeout及相邻说明；新增直接相关PostgreSQL锁测试并调整既有60秒测试的JUnit外层watchdog；V1–V14、Java生产、API/OpenAPI及安全合同无diff。
- callback精确顺序通过：history读取/no-op gate后立即`SET LOCAL lock_timeout = '5s'`与`statement_timeout = '60s'`，随后才首次访问guard表、`pg_attribute`、`pg_constraint`和`pg_class`。
- 真实PostgreSQL 17.10 `ACCESS EXCLUSIVE`回归无外部session timeout，约5秒lock timeout后完整保留V12 history、strict CHECK和padded原值；解锁后安全retry至V14，session setting恢复`0`/`0`。
- 目标类12/12、owning dh-app 147/147、全量158 reports / 1076 tests均0 failure/error/skip；quality 19/19、Checkstyle 0、Spotless PASS。
- 发现P1 current factsources/入口规范冲突：CODEX后部仍有未标historical的Stage-QDR-7 planning入口，FACTSOURCE_POLICY仍有旧Stage-QDR-4 current task，仓库CLAUDE仍声明Stage3-B3/Integration-0-PLAN；三者不在本轮allowlist，未越界修复。
- 本轮仅修改允许的review/current文档；未修改callback、Java、测试、V1–V14、API/NQ；未运行capacity、未进入B3、未push/tag。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW_RETRY: BLOCKED
CALLBACK_TIMEOUT_ORDER: PASS
PRECHECK_LOCK_TIMEOUT: PASS
TRANSACTION_ROLLBACK_SAFETY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: FAIL
SCHEMA_ERRATA_IMPLEMENTATION_STATUS: BLOCKED
ALLOW_MILESTONE_REVIEW_RETRY_3: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
```

## 2026-07-13 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX

- 预检确认`dev`、HEAD `6a1794d`、worktree/staged clean、`origin/dev...HEAD=0/0`；实施commit `f144a42`与历史review `6a1794d`均存在。
- 仅将callback的`SET LOCAL lock_timeout = '5s'`和`SET LOCAL statement_timeout = '60s'`前移到history-only no-op gate之后、首次guard precheck之前；V1–V14未改。
- 新增真实PostgreSQL 17.10/Testcontainers `ACCESS EXCLUSIVE`锁回归；约5.195秒lock timeout后保留V12 history、strict CHECK和padded原值，释放锁后retry至V14。
- 同一migration connection验证失败后`lock_timeout`/`statement_timeout`均恢复为`0`，证明`SET LOCAL`未污染session默认值。
- 首轮目标类在慢宿主机上由JUnit 75秒watchdog中止；仅调整测试watchdog为90秒后，冻结的60秒DB timeout和其它生产语义不变。
- 定向timeout用例2/2、目标类12/12、owning/full与quality均通过；完整Surefire为158 reports / 1076 tests / 0 failures / 0 errors / 0 skipped。
- 同步root/current factsources为`B2 schema errata blocker fix: DONE / REVIEW_PENDING`；历史implementation review的`BLOCKED`段落保留，capacity acceptance和B3仍禁止。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_BLOCKER_FIX: DONE / REVIEW_PENDING
B2_MILESTONE_ACCEPTANCE: NOT_YET
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_3_NOW: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY
```

## 2026-07-13 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW

- 只读复核`3ac87a2..f144a42`，确认旧`beforeMigrate`删除、新`beforeEachMigrate`发现，V1–V14、Java生产代码、API/OpenAPI及security contracts无diff。
- 既有PostgreSQL 17.10 suite 11/11通过，覆盖rollback/retry、1000/1001、fingerprint、completed no-op与V14 no-truncation。
- 独立真库实验发现P1：callback在guard constraint/data precheck之后才设置5秒`lock_timeout`；`ACCESS EXCLUSIVE`锁下precheck超过5秒并由外加8秒statement timeout终止。
- 发现current factsources残留：root/current README与CODEX instruction后部仍为旧B2/next-action；不在本轮allowlist，未越界修改。
- owning/full均为158 reports / 1075 tests / 0 failures/errors/skipped；quality 19/19、Checkstyle 0、Spotless PASS。绿色回归不覆盖P1。
- 本轮仅修改review/current文档；未修改callback、Java、测试、V1–V14、API/NQ；未运行capacity、未进入B3、未push/tag。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION_REVIEW: BLOCKED
SCHEMA_ERRATA_IMPLEMENTATION_STATUS: BLOCKED / P1_FIX_REQUIRED
ALLOW_MILESTONE_REVIEW_RETRY_3: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX
```

## 2026-07-13 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION

- 预检确认`dev`、HEAD `3ac87a2`、worktree clean、staged empty；未读取敏感配置，未触碰NQ。
- 使用Git rename将`beforeMigrate__qdr7_v13_compatibility.sql`替换为`beforeEachMigrate__qdr7_v13_compatibility.sql`；V1–V14未修改。
- callback仅在成功V12且V13不存在时访问guard表；对history异常、V12 table/CHECK fingerprint、FAILED blank/length和超过1000行repair均fail-closed。
- 冻结并实现`SET LOCAL lock_timeout = '5s'`和`SET LOCAL statement_timeout = '60s'`；repair按`guard_id`排序、最大1000行，temporary CHECK仅暂时放开V13必需的FAILED/EXPIRED terminal timestamp转换。
- 新增真实PostgreSQL 17/Testcontainers回归：11 tests、0 failures/errors/skipped，覆盖callback发现、事务rollback/retry、两类timeout、ceiling、fingerprint、completed no-op和V14 no-truncation/retry。
- 本机Surefire manifest-JAR绝对路径根冲突仅在首次定向命令以`-Dsurefire.useManifestOnlyJar=false`规避；未修改POM、Surefire或全局JVM配置。随后`mvn -ntp -pl dh-app -am test`、`mvn -ntp test`和`mvn -ntp -Pquality validate`均实际`BUILD SUCCESS`；完整Surefire XML为158 reports / 1075 tests / 0 failures / 0 errors / 0 skipped，根项目Checkstyle 0、Spotless PASS。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION: DONE / REVIEW_PENDING
B2_MILESTONE_ACCEPTANCE: NOT_YET
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_3_NOW: NO
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW
```

## 2026-07-12 DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-REVIEW

- 只读核验V12 CHECK、V13执行顺序、V14 narrowing、Flyway 11.7.2配置和current factsources。
- PostgreSQL 17.10实验拒绝现有`beforeMigrate`事务模型，并证明`beforeEachMigrate`与单个migration共同rollback。
- 拒绝V12.1 interstitial migration：已完成V14环境在当前validation/outOfOrder策略下发生`FlywayValidateException`。
- 选择`beforeEachMigrate TRANSACTIONAL_CALLBACK`；冻结history/constraint fingerprint、pre-write validation、同名临时CHECK、timeout、1000行repair ceiling、retry/no-op和测试矩阵。
- 未修改callback、V1–V14、Java、测试、API/OpenAPI或安全合同；未运行capacity benchmark；未进入B3；未push/tag。

```text
STAGE_QDR_7_B2_SCHEMA_ERRATA_REVIEW: PASS
ALLOW_STAGE_QDR_7_B2_SCHEMA_ERRATA_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_MILESTONE_REVIEW_RETRY_3_NOW: NO
next action: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2

- 独立复核`865257a..8660f4e`：范围仅含P1 callback/V14、直接测试、usecase小修和允许文档；V1-V13、API/Controller/DTO/OpenAPI、HMAC/nonce/source、NQ与previous review未变化。
- 真实Flyway/Testcontainers日志确认callback发现和before-migrate顺序，targeted 23、owning 135、full 1064测试均0 failures/errors/skipped，PostgreSQL 17.10实际运行，quality通过。
- 发现P1 callback安全缺口：V12条件下执行表级CHECK删除/重建和FAILED无界UPDATE，没有锁预算、batch或失败后schema恢复证据。
- 发现V14 fail-closed缺口：显式`result_type::varchar(32)`收窄cast；overflow路径未验证原值保持与修复后retry。
- 发现证据缺口：caller clock matrix未覆盖三类terminal timestamp；completion rollback不是actual service completion路径；cleanup未注入CAS miss。
- 发现`README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`的current-phase块仍指向旧work order/B1 action，而`STATUS.md`/`WORK_ORDER.md`为retry-2，形成当前factsources冲突；本任务allowlist不含前三者，未越界修复。
- 本轮只修改允许review文档；未修改Java、测试、callback、migration、API/NQ或交易边界；未运行capacity benchmark、未进入B3、未push/tag。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY_2: BLOCKED
B2_IMPLEMENTATION_STATUS: BLOCKED / P1_FIX_REQUIRED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY-2
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY

- Preflight确认`dev`、HEAD `865257a`、worktree/staged clean、`origin/dev...HEAD=0/0`、migration max `V13`；未修改NQ。
- 新增only-pre-V13 SQL callback：只在guard表存在且V13尚未成功时规范化FAILED `stable_error_code`；空值以`23514`拒绝，且为V13 typed terminal timestamp迁移临时替换旧CHECK顺序冲突。
- 新增forward-only V14：先拒绝超过32字符的非空`result_type`，再收窄至冻结`varchar(32)`；未修改V1-V13。
- 增加实际Spring/JDBC PostgreSQL证据：caller时钟偏移、result reference 503 taxonomy、同DataSource completion rollback、idempotency真实after-commit unknown、两worker cleanup与CAS miss。
- 修改completed duplicate入口，先校验固定`DH_DECISION_OUTPUT` type；异常reference统一fail-closed为`IDEMPOTENCY_RESULT_UNAVAILABLE / 503`。
- 完成targeted 23、owning 135、full 1064 tests，均0 failures/errors/skipped；PostgreSQL 17.10/Testcontainers实际运行，quality通过。
- 同步root/current factsources为`B1 FROZEN`、`B2 BLOCKER_FIX_RETRY_DONE / REVIEW_PENDING`；previous milestone review保持历史`BLOCKED`，未运行capacity benchmark、未进入B3、未push/tag。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_BLOCKER_FIX_RETRY: DONE / REVIEW_PENDING
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY_2: YES
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY-2
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY

- Preflight确认`dev`、HEAD `74cfeb917...`、initial worktree/staged clean；本地相对`origin/dev` behind 0、ahead 4。
- 复核`e3fd401..74cfeb9`的23-file diff、V12/V13、cleanup/admission/idempotency adapters、usecase transaction coordinator、result projector、production wiring与相关测试；previous milestone review blob未变化。
- 确认EXPIRED tombstone、DB-time lifecycle、rate/current-window保护、active lease predicate、owner/token CAS、tenant-bound FK、rollback和rate real-JDBC commit-unknown方向成立。
- 发现remaining P1：合法V12 FAILED行可能被V13 trim CHECK阻断；`result_type`长度与冻结schema不一致；idempotency commit-unknown仍为fake boundary；production Spring同DataSource wiring、JVM clock offset、actual JDBC result mapping和idempotency concurrent cleanup证据不足。
- 发现root/current README与`CODEX_PROJECT_INSTRUCTIONS.md`仍保留旧planning/B1 next-action；三者不在本任务allowlist，本轮未越界修复并记录为factsource residual。
- 运行targeted、full和quality；PostgreSQL 17.10/Testcontainers实际执行，155 reports / 1055 tests / 0 failures/errors/skips，Checkstyle 0、Spotless PASS。绿色回归不覆盖上述mandatory P1。
- 本轮只修改允许的current review文档；未修改Java、测试、migration、API/OpenAPI、HMAC/nonce/source、Repository/JDBC或NQ；未运行capacity benchmark；未push/tag/commit。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW_RETRY: BLOCKED
B2_IMPLEMENTATION_STATUS: BLOCKED / P1_FIX_RETRY_REQUIRED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX-RETRY
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX

- Preflight确认`dev`、HEAD `e3fd401`、blocker review已提交、worktree/staged clean、V12存在且V13不存在。
- 新增forward-only V13；V12保持不变。补齐`lease_owner/result_type/failed_at/expired_at`、安全回填、状态CHECK与cleanup索引。
- 将admission/lease/TTL/retention/cleanup输入收口为严格`Duration`；所有持久化guard绝对时间与资格判断统一到PostgreSQL `transaction_timestamp()`。
- Idempotency cleanup改为bounded CAS到`EXPIRED`，清理result/failure细节但永久保留identity tombstone；rate cleanup增加DB current-window与safety-grace保护。
- Heartbeat校验owner/token/state/version；recovery仅接管DB判定已过期lease；duplicate客户端不触发接管。
- 补齐真实PostgreSQL/JDBC transaction、result reference、并发cleanup和after-commit connection failure证据。
- 验证：targeted PG 14、full 1055 tests均0 failures/errors/skips；quality Checkstyle 0、Spotless PASS。
- 未修改NQ、V1-V12、API/OpenAPI、Controller/DTO、HMAC/nonce/source；未运行capacity benchmark；未进入B3；未push/tag。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_BLOCKER_FIX: DONE / REVIEW_RETRY_READY
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW_RETRY: YES
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW-RETRY
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW

- 只读复核`19666e5`的51-file commit范围、V12、ports/JDBC、transaction boundary、result projector、cleanup、Controller/filter/wiring、taxonomy与直接测试。
- 确认V1-V11、contracts/OpenAPI、HMAC/nonce无diff；rate conditional upsert、exact key、CAS/version/token、tenant-bound FK、nonce-first guard order和no-in-memory-fallback方向通过。
- 发现P1 blockers：`expires_at`只写不读且terminal EXPIRED不可达；cleanup使用caller-controlled cutoff而非DB now/safety grace；V12缺冻结的`lease_owner/result_type/failed_at`；lease/TTL/retention混用JVM/DB clock；actual completion/result/cleanup transaction matrix不足。
- 运行owning、full和quality；PostgreSQL 17.10/Testcontainers真实执行，875/1045 tests均0 failures/errors/skips。绿色测试不覆盖上述P1，因此milestone保持BLOCKED。
- 未修改Java、测试、V1-V12、API/OpenAPI、HMAC/nonce/source、Repository/JDBC或NQ；未运行capacity benchmark；未push/tag/commit。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_MILESTONE_REVIEW: BLOCKED
B2_IMPLEMENTATION_STATUS: BLOCKED / FIX_REQUIRED
ALLOW_POST_B2_CAPACITY_ACCEPTANCE: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-BLOCKER-FIX
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION

- 开工前确认`dev`、`f2573c4`、clean/unstaged、B2 review已提交、max migration V11且V12不存在。
- 新增additive`V12__qdr7_persistent_runtime_guards.sql`，创建rate bucket与idempotency guard表，并为existing`dh_decision_output`补tenant-bound result reference unique constraint；V1-V11未修改。
- 新增tenant-first capability ports与JDBC adapters，实现DB UTC fixed-window conditional upsert、exact admission/reread、expected state/version/token CAS、heartbeat、expired lease recovery和bounded`SKIP LOCKED` cleanup。
- 新增domain-separated request fingerprint、safe result projector、persistent idempotency wrapper、persistent rate bridge、strict guard properties与required transaction boundary。
- 调整dry-run内部wiring与guard顺序：HMAC/timestamp/nonce后才消费persistent rate；generic key-only idempotency filter精确排除dry-run route；NQ feedback原limiter保持不变。
- 补充Flyway/PostgreSQL并发、CAS/lease/cleanup/rollback、store/commit分类、route exclusion、configuration与architecture regression；修正transaction-start store unavailable被误归类为commit unknown的收口问题。
- owning-module为875 tests，full Maven为1045 tests；均0 failures/errors/skips。PostgreSQL 17.10/Testcontainers与quality validate通过。
- 未修改API/OpenAPI/DTO/HMAC canonical material/nonce identity/NQ；未接外部HTTP、Provider、Agent、LangGraph；未触碰交易/Paper/LIVE；未跑capacity benchmark；未push/tag。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_IMPLEMENTATION: DONE / LOCAL_VALIDATED
ALLOW_STAGE_QDR_7_B2_MILESTONE_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_POST_B2_CAPACITY_ACCEPTANCE_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-MILESTONE-REVIEW
```

## 2026-07-12 DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW

- 只读核验Git、current factsources、Flyway V1-V11、nonce/audit/QDR snapshot表、现有rate/idempotency、JDBC事务/cleanup、ports与PostgreSQL/Testcontainers基础。
- 选择PostgreSQL fixed-window counter，冻结exact key、DB UTC window、原子upsert winner、bounded retry和commit-unknown fail-closed。
- 冻结`dh_qdr7_rate_limit_bucket`与`dh_qdr7_idempotency_guard`候选结构、state machine、lease/crash recovery、duplicate semantics和existing `dh_decision_output` safe result reference。
- 冻结usecase-owned transaction coordinator、capability ports、infra JDBC包边界、production no-fallback wiring、bounded `SKIP LOCKED` cleanup和post-B2 capacity gate。
- 未修改Java、测试、migration、API/OpenAPI、HMAC/nonce/source合同；未实现persistent guards，未运行capacity benchmark，未push/tag。

```text
STAGE_QDR_7_B2_PERSISTENT_GUARDS_SCHEMA_SECURITY_REVIEW: PASS
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION: YES / NEXT_TASK_ONLY
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-IMPLEMENTATION
```

## 2026-07-12 DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-FIX-REVIEW

- 只读审核`044afba fix(qdr): preserve canonical dry-run source semantics`，确认提交未触及Controller/DTO、OpenAPI、migration、Repository/JDBC或HMAC production implementation。
- 以配置输入→Wiring CSV binding→`DecisionDryRunRuntimeProperties` canonical validation→HMAC exact wire comparison→Controller `SOURCE_DENIED / 403`追踪source合同；`NQ_DRYRUN`为唯一canonical值，request不trim/不case-fold/不alias。
- 确认完全空allowlist是production deny-all状态；非空CSV中的blank/trailing-empty、非canonical source、非canonical pair与pair/allowlist矛盾在bean creation时fail-closed。
- 确认HMAC material字段、顺序、编码和raw-body hash不变，source篡改签名失效，nonce replay与tenant/source isolation不变；无新增敏感日志、外部HTTP、Provider、NQ、Agent或LangGraph依赖。
- 运行owning-module Maven、完整Maven和quality validation；PostgreSQL 17 Testcontainers/Flyway V1–V11实际执行，full Surefire为1026 tests、0 failures/errors/skips。

```text
SOURCE_NORMALIZATION_FIX_REVIEW: PASS
SOURCE_PRODUCTION_DRIFT: CLOSED
STAGE_QDR_7_B1_RUNTIME_CONTRACT: FROZEN
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B2-PERSISTENT-GUARDS-SCHEMA-SECURITY-REVIEW
```

## 2026-07-12 DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX

- 复核发现路径为`DecisionDryRunRuntimeWiringConfig` CSV binding到`DecisionDryRunRuntimeProperties`的lowercase normalization；该语义与HMAC exact-wire source冲突。
- 删除request source的case normalization；配置只允许outer trim，`NQ_DRYRUN`之外的lowercase、mixed-case、alias、unknown、blank、trailing-empty和非canonical tenant/source pair在bean创建时fail-closed。
- 保留prod profile空allowlist作为no-source deny state；既有`DecisionContractGapGuardTest`要求prod不得allowlist`NQ_DRYRUN`，未修改该合同或测试。
- 补充properties/wiring/HMAC/WebMvc回归，覆盖canonical成功、exact wire拒绝、source/body篡改签名失效、nonce replay与tenant/source isolation。
- `mvn -ntp test`通过；Docker/Testcontainers PostgreSQL 17和Flyway V1–V11实际执行。未运行capacity benchmark，未改API/OpenAPI、nonce/replay、rate/idempotency、migration、Repository/JDBC、NQ或外部依赖。

```text
STAGE_QDR_7_B1_SOURCE_NORMALIZATION_BLOCKER_FIX: DONE
SOURCE_DRIFT_DISPOSITION: FIXED / REVIEW_PENDING
ALLOW_STAGE_QDR_7_B1_SOURCE_FIX_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW_NOW: NO
next action: DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-FIX-REVIEW
```

## 2026-07-12 DH-STAGE-QDR-7-B1-CAPACITY-BLOCKER-RESOLUTION

- 以 review-only 方式核对 OpenAPI、DTO、Controller、HMAC authenticator、runtime source allowlist/pair、application profiles、fixtures/tests、resource evidence、persistent guard 与 metrics 现实。
- 冻结 canonical source 为 case-sensitive exact `NQ_DRYRUN`：request 不 trim/不改写，config 仅 trim 并保留 case，signature 使用原始 wire value，allowlist/pair 精确比较。
- 发现 `DecisionDryRunRuntimeProperties` lowercases source 而 authenticator 坚持 exact-wire，裁定为 `PRODUCTION_CODE_DRIFT`；OpenAPI coverage ambiguity 与历史 fixture/诊断 harness drift 作为伴随发现记录。
- 将容量门禁拆为 pre-B2 safety contract 与 post-B2 measured defaults；冻结配置类型/单位/合法范围/absolute ceiling/跨字段约束，未选择运行默认值。
- 修正路线为 B1 → B2 schema/security review → persistent guards → actual-wiring 2xx harness → B2 capacity acceptance → B3 → B4；persistent store failure 禁止 in-memory fallback。
- 本轮未运行 benchmark、Docker/Testcontainers 或 Maven tests，未修改代码/测试/migration/API/OpenAPI/wiring/NQ，未 commit/push/tag。
- `mvn -ntp -Pquality validate` 通过：reactor 19/19 `SUCCESS`、root Checkstyle 0 violations、Spotless PASS；未把 validate 写成 Maven tests PASS。

```text
STAGE_QDR_7_B1_CAPACITY_BLOCKER_RESOLUTION: DONE
SOURCE_DRIFT_DISPOSITION: CODE_FIX_REQUIRED
B1_SOURCE_NORMALIZATION_CONTRACT_FIX_REQUIRED
CAPACITY_GATE_SEQUENCE: FROZEN
PRE_B2_SAFETY_LIMITS: FROZEN
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO / SOURCE_FIX_FIRST
next action: DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX
```

## 2026-07-12 DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER

- Preflight确认`dev`/`8543f691` clean、staged empty；B1 contract为`PARTIALLY_FROZEN / RESOURCE_CAPACITY_BLOCKED`。
- 采集Windows/CPU/内存/JDK/Maven/JVM、Tomcat/Hikari/PostgreSQL/Docker/Testcontainers与current rate/idempotency环境事实。
- 启动test-profile loopback应用与唯一隔离PostgreSQL数据库；未使用生产凭证或真实业务数据，结束后已停止应用并删除临时库。
- Existing standalone MockMvc不经过Tomcat/Hikari/PostgreSQL，不能作为capacity harness。
- 正式4 payload × 5 concurrency × warmup/100 samples尝试全部`SOURCE_DENIED / 403`；lowercase wiring诊断随后在mock gateway处`UNKNOWN_ERROR / 500`。有效2xx samples为0，所有耗时/吞吐数据排除。
- Actuator确认Hikari max/min 10，但Tomcat worker/queue未暴露；applicationTaskExecutor呈无界配置，不能作为bounded endpoint queue。
- Docker daemon不可用；全仓1017 tests通过但21个PostgreSQL/Testcontainers tests skipped。
- 不填临时默认数值，维持`RESOURCE_CAPACITY_EVIDENCE: INSUFFICIENT`与B2禁止。

```text
STAGE_QDR_7_B1_RESOURCE_CAPACITY_BLOCKER: BLOCKED
VALID_SUCCESS_SAMPLES: 0
RESOURCE_CAPACITY_EVIDENCE: INSUFFICIENT
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO
next action: DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER-RETRY
```

## 2026-07-12 DH-STAGE-QDR-7-B1-RUNTIME-CONTRACT-SAFETY-POLICY

- Preflight 确认 `dev` / `3ce1cee9`、clean worktree、staged empty；Stage-QDR-7 implementation work order 已提交，B1 未启动 implementation。
- 逐项映射 implementation work order 的 15 步目标 guard 顺序与现有 filter/controller/authenticator/service/JDBC 现实。
- 冻结 runtime truth table、四类 key domain、`QDR7-DRYRUN-CJSON-1` hash 字段、nonce/idempotency 交互、duplicate semantics、`RECEIVED/IN_PROGRESS/COMPLETED/FAILED/EXPIRED` 状态机、error taxonomy 和 audit/redaction policy。
- 记录当前实现差距：feature gate 过晚、rate-limit 早于 HMAC/nonce、idempotency filter 早于 Controller、dynamic kill 缺失、nonce store failure 与 replay 无法区分。
- 65536-byte payload 与 32768-byte context 作为既有兼容上限冻结；deadline、concurrency、queue、persistent rate quota、idempotency lease/TTL、cleanup/retention 与 kill propagation 缺少容量证据。
- 按 fail-closed 规则输出 `B1_RESOURCE_CAPACITY_EVIDENCE_BLOCKED`；B2 schema/security review 与 implementation 均未授权。
- `mvn -ntp -Pquality validate` 通过：reactor 19/19、Checkstyle 0 violations、Spotless PASS；full tests、capacity/load tests 与 Docker/Testcontainers 本轮未运行。

```text
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: BLOCKED
CONTRACT_VERDICT: PARTIALLY_FROZEN / RESOURCE_CAPACITY_BLOCKED
RESOURCE_BUDGETS: BLOCKED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO
next action: DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER
```

## 2026-07-12 DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER

- Preflight 确认 `dev` / `a752e113`、clean worktree、staged empty；最新提交为 Stage-QDR-7 plan。
- 只读核验既有 `POST /api/ai/decision-dry-runs`、guard ordering、HMAC/timestamp/nonce、tenant/source、JVM-local rate limit、key-only in-memory idempotency、JDBC nonce replay、payload/context cap、feature/production flags、kill snapshot、audit/redaction 与 PostgreSQL/Testcontainers 基础。
- 新增 `DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md`，冻结 B1–B5、identity/domain separation、duplicate semantics、truth table、resource/deadline/backpressure、review、测试、回滚和 archive/tag 纪律。
- 明确 same nonce always reject；same requestId + same canonical request 按状态复用冻结结果；same requestId + different hash conflict；unknown/incomplete state fail-closed。
- 明确 B2 不得复用 replay nonce 表，migration + production port/JDBC 必须先做统一 schema/security milestone review；guard store unavailable 禁止 fallback in-memory。
- 明确 outbound HTTP/Provider retry 与 circuit breaker 因没有真实目标继续后置；Stage-QDR-7 implementation 未启动。
- `mvn -ntp -Pquality validate` 通过：reactor 19/19、Checkstyle 0 violations、Spotless PASS；本轮未运行 full Maven tests 或 Docker/Testcontainers。

```text
STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_7_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_7_B1_CONTRACT_FREEZE: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
next action: DH-STAGE-QDR-7-B1-RUNTIME-CONTRACT-SAFETY-POLICY
```

## 2026-07-11 DH-STAGE-QDR-7-PLAN

- Preflight确认`dev`/`363dadf` clean、staged empty；Stage-QDR-6 local/remote annotated tag peeled target仍为`b9b68b3`，current QDR-6 residue为空。
- 基于当前代码现实形成`DH_STAGE_QDR_7_PLAN.md`：既有DH-only limited dry-run inbound endpoint默认关闭，已有HMAC/timestamp/nonce/tenant-source、JDBC replay、payload/memory cap、feature flag、kill switch与audit fail-closed基线。
- 推荐主线为`Runtime Safety and Guard Baseline`；优先关闭persistent multi-instance rate limit、idempotency/duplicate handling、dynamic kill、resource/deadline/backpressure与redaction evidence缺口。
- 规划B1 contract/policy、B2 persistent guards、B3 operational resilience、B4 protected entry acceptance、B5 stage close discipline；API/Controller、migration、production Repository/JDBC和security semantics变化必须独立review。
- NQ-DH runtime alignment、Provider dry-run与Agent/LangGraph/Python runtime继续后置；未启动Stage-QDR-7 implementation。

```text
STAGE_QDR_7: PLANNING / IMPLEMENTATION_NOT_STARTED
STAGE_QDR_7_PLAN: DONE / PLAN_ONLY
STAGE_QDR_7_MAINLINE: LIMITED_DRY_RUN_RUNTIME_READINESS
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER
```

## 2026-07-11 DH-STAGE-QDR-6-POST-TAG-CURRENT-CLEANUP

- Preflight确认`dev`/`b9b68b3` clean，local/remote annotated tag均指向archive commit。
- 按archive `SOURCE_INDEX.md`逐份核验11个current process sources；11/11 archive副本存在且SHA-256一致，packet checksums 23/23通过。
- 使用`git rm`删除11个已归档current process sources；未删除或修改archive packet。
- Current factsources同步为Stage-QDR-6 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`、current sources `PRUNED`、cleanup `DONE`。
- Stage-QDR-7保持`NOT_STARTED`；仅开放`DH-STAGE-QDR-7-PLAN` planning-first，implementation继续禁止。
- `mvn -ntp -Pquality validate`通过，reactor 19/19、Checkstyle 0 violations、Spotless PASS；full tests和PostgreSQL/Testcontainers本轮未重跑。

```text
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
CURRENT_PROCESS_SOURCES: PRUNED / 11_REMOVED
ALLOW_STAGE_QDR_7_PLAN: YES / PLANNING_FIRST_ONLY
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-PLAN
```

## 2026-07-11 DH-STAGE-QDR-6-ARCHIVE-TAG-CLOSE

- Preflight 通过：`dev` / `e09d5b4`、exact 7-file retry diff、staged empty、forbidden/V1-V11 diff empty，本地与远程 tag 均不存在。
- Final close retry 已提交为 `5961164 docs(qdr): accept stage-qdr-6 final close retry`，只包含 7 个预期 current 文档。
- 已在 `docs/gates/stage-qdr-6/` 形成 self-contained packet，复制 11 个 Stage-QDR-6 current source 文档且未删除原件。
- Previous final close `BLOCKED` 与 retry `PASS` 分别保留；archive commit 与 annotated tag 尚未创建。
- `mvn -ntp -Pquality validate` 已通过，reactor 19/19、Checkstyle 0 violations、Spotless PASS；archive SHA-256 在提交前生成并复核。
- Stage-QDR-7 仍 `NOT_STARTED / NOT_ALLOWED_YET`；未修改代码、测试、migration、API、Repository/JDBC、wiring 或 NQ。

```text
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: PENDING / NOT_CREATED
next action: archive commit -> clean worktree -> annotated tag close
```

## 2026-07-11 DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW-RETRY

- 在 `dev` / `e09d5b4`、clean worktree、empty staged baseline 上重新执行 Stage-QDR-6 final close review。
- 五个指定 current factsources 已清除上轮未实现/next B1 冲突；previous `DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md` 的 `BLOCKED` 历史保持不变。
- B1/B2 contracts/aggregation、B3 canonical snapshot/persistence/mock deterministic replay、B4 internal report 与 security boundary 均 `PASS`；未发现实现、安全、tenant、hash、migration 或 authorization blocker。
- 四组 Maven 命令全部通过；全仓 1017 tests、0 failures/errors/skipped；PostgreSQL 17.10/Testcontainers、V1-V11、ArchitectureTest、Checkstyle 与 Spotless 均通过。
- 只新增 retry review 并同步本任务 allowlist 内 current/supporting 文档；未修改 Java、测试、migration、API、port/JDBC、Repository、wiring 或 NQ，未创建 archive packet、commit、tag 或 push。

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: YES
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES
ALLOW_STAGE_QDR_7_PLAN_NOW: NO
next action: DH-STAGE-QDR-6-ARCHIVE-TAG-CLOSE
```

## 2026-07-11 DH-STAGE-QDR-6-FINAL-CLOSE-BLOCKER-FIX

- 仅同步 `README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md` 的 Stage-QDR-6 current facts，并最小更新 `STATUS.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`。
- 清除 `IMPLEMENTATION_NOT_STARTED`、current implementation WO 与 next B1 的错误 current wording；Stage-QDR-6 implementation 统一为 `COMPLETE`。
- Previous final close review 的 `BLOCKED` 历史结论保持不变；当前状态只推进到 `RETRY_PENDING`，不写 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。
- Archive packet、tag、push 与 Stage-QDR-7 planning 均未开始；Java、测试、migration、API、Repository/JDBC、wiring 与 NQ 均未修改。
- `git diff --check`、current facts/safety scans 与 `mvn -ntp -Pquality validate` 已通过；Checkstyle 0 violations，Spotless PASS。本轮未运行 Maven tests 或 Docker/Testcontainers。

```text
STAGE_QDR_6_FINAL_CLOSE_BLOCKER_FIX: DONE
CURRENT_FACTSOURCE_CONFLICT: CLEARED
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET_NOW: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_NOW: NO
ALLOW_STAGE_QDR_7_PLAN_NOW: NO
next action: DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW-RETRY
```

## 2026-07-11 DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW

- 在 `dev` / `964b493`、clean worktree、empty staged 前提下完成 Stage-QDR-6 B1–B4、V10/V11、tenant/identity/hash/transaction、deterministic replay、internal report 与安全授权边界的只读终审。
- B1/B2 contracts/aggregation、B3 canonical snapshot/mock replay、B4 internal report 均 `PASS`；未发现 P0/P1、安全边界、migration、tenant、hash、replay 或 authorization blocker。
- 发现 current factsource blocker：`README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md` 仍写 Stage-QDR-6 未实现/next B1，与代码现实冲突且不在本轮 allowlist；按 `FACTSOURCE_POLICY.md` fail-closed 阻断 final close。
- `mvn -ntp -pl dh-usecase,dh-infra -am test`、`mvn -ntp -pl dh-app -am test`、`mvn -ntp test` 与 `mvn -ntp -Pquality validate` 全部通过；全仓 1017 tests，0 failures/errors/skipped；PostgreSQL 17.10/Testcontainers、V1-V11、Checkstyle、Spotless、ArchitectureTest 均通过。
- 只新增 final close review 并同步 allowlist current docs；未创建 archive packet、commit、tag 或 push。

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW: BLOCKED / CURRENT_FACTSOURCE_CONFLICT
STAGE_QDR_6: IMPLEMENTATION_COMPLETE / FINAL_CLOSE_BLOCKED / NOT_ARCHIVED / NOT_TAGGED
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: NO
ALLOW_STAGE_QDR_7_PLAN_NOW: NO
next action: DH-STAGE-QDR-6-FINAL-CLOSE-BLOCKER-FIX
```

## 2026-07-11 DH-STAGE-QDR-6-B4-EVIDENCE-REPLAY-INTERNAL-REPORT

- 新增 `DecisionEvidenceReplayInternalReport`、`DecisionEvidenceReplayReportService`、`InternalAcceptanceStatus` 与 `InternalAcceptanceFinding`，全部位于 `dh-usecase` 内部 `qdr/report` 包。
- Report 复用现有 evidence aggregate、deterministic replay result、regression report view、provider readiness evaluation 与 observability report，不复制 source model，不新增 input port。
- 状态按固定 fail-closed 优先级映射为 `FAILED / INVALID / UNSUPPORTED / INCOMPLETE / REJECTED / ACCEPTED`；只有 evidence complete、replay reproducible、regression pass、readiness ready 且 observability pass 才允许 `ACCEPTED`。
- tenant/trace/request/decision 与 provider/readiness identity 必须 exact match；source 或转换异常只返回固定脱敏 `FAILED` finding，不透出异常内容。
- Evidence refs、observability refs、replay differences 与统一 findings 均稳定排序；finding constructor 拒绝 raw material、credential-like material 与可执行交易动作。
- Report 强制携带 internal-only safety declaration，所有 acceptance status authorization helper 固定返回 false。
- 新增 16 个 B4 回归；focused、`dh-usecase` 519 tests、全仓 1017 tests 均 0 failures/errors/skipped。真实 PostgreSQL 17.10/Testcontainers、V1→V11、quality、Checkstyle 与 Spotless 全部通过。
- 未修改 migration、V1-V11、production port/JDBC、API/Controller 或 runtime wiring；未持久化 report，未调用 HTTP/Provider/NQ/Agent/LangGraph，未创建 tag，未 push。

```text
STAGE_QDR_6_B4_INTERNAL_REPORT: DONE / IMPLEMENTED / VERIFIED
B4_REPORT_INPUT_BOUNDARY_EXPANSION_REQUIRED: NO
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_ADDITIONAL_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-FINAL-CLOSE-REVIEW
```

## 2026-07-11 DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-CLOSE-REVIEW

- 在 `dev` / `ae4c944`、clean worktree、empty staged 前提下完成 B3-P1/P2/P3、deterministic replay gate 与 baseline 的只读 close review。
- 确认 persisted `CanonicalReplaySnapshotRecord` 是 executor 唯一输入事实源，可无损重建 `ReplayInputSnapshot` 并重新生成 `QDR6-CJSON-1` canonical bytes/hash；executor 不重读 V5/V6/V8/V9 mutable source。
- 确认 `QDR6-MOCK-REPLAY-1` 无 clock/random/environment/HTTP/Provider/NQ/Agent/LangGraph/write dependency，无 API/Controller 或 production runtime wiring。
- 确认 input/output domain separation、lowercase SHA-256、9 类 difference、6 类 status、13 个 failure mapping、stable ordering 与 fail-closed result invariants。
- 确认 result 固定为 internal reproducibility evidence only；不包含 raw material、credential 或交易动作，不形成 Provider/NQ/交易/Paper/LIVE 授权。
- focused 42 tests、`dh-usecase` 503 tests、全仓 1001 tests 均 0 failures/errors/skipped；真实 PostgreSQL 17.10/Testcontainers、V1→V11、quality、Checkstyle 与 Spotless 全部通过。
- 本轮只修改允许的 review/current 文档；未修改 Java、测试、migration、V1-V11、API、port/JDBC、Repository 或 wiring，未持久化 replay result，未创建 tag，未 push。

```text
B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW: PASS
STAGE_QDR_6_B3: CLOSED / ACCEPTED
ALLOW_B4_INTERNAL_REPORT_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_ADDITIONAL_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B4-EVIDENCE-REPLAY-INTERNAL-REPORT
```

## 2026-07-11 DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE

- 新增 `DeterministicReplayCommand`、`DeterministicReplayResult`、`DeterministicReplayExecutor`、冻结 status/difference/failure taxonomy 与 read-only local projection。
- `ReplayInputSnapshot.fromPersistedRecord` 只从数据库回读 record 无损重建 canonical input；不重读 V5/V6/V8/V9 mutable sources，不使用 audit time、随机值或环境字段。
- Executor 只调用现有 snapshot port 的 tenant + full identity exact read，复验 persisted/recomputed/command canonical input hash；不调用 insert、不写数据库、不修改 snapshot。
- Replay output 使用 `QDR6-CJSON-1` 与独立 `DH-QDR6-DETERMINISTIC-REPLAY-OUTPUT` domain SHA-256；input hash 不能冒充 output hash。
- Comparator 仅输出冻结的 9 类 difference，按 type/path 稳定排序；context/summary/evidence 只记录 fingerprints，不保存 raw values。
- 所有异常与不兼容状态映射为冻结 result；`REPRODUCIBLE` 只表示内部 mock reproducibility evidence，不表示真实 Provider/prompt/policy replay 或任何授权。
- 新增 15 个 focused tests；`dh-usecase` 503 tests、全仓 1001 tests 均 0 failures/errors/skipped。真实 PostgreSQL 17.10/Testcontainers 与 V1→V11 regression 通过；quality 19/19、Checkstyle 0、Spotless PASS。
- 未修改 V1-V11、port/JDBC/Repository、wiring、API 或外部 runtime；未调用 HTTP/Provider/NQ/Agent/LangGraph，未持久化 replay result，未创建 tag，未 push。

```text
STAGE_QDR_6_B3_DETERMINISTIC_REPLAY_BASELINE: DONE / IMPLEMENTED / VERIFIED
REPLAY_COMMAND_CONTRACT: PASS
REPLAY_RESULT_CONTRACT: PASS
MOCK_EXECUTOR: PASS
INPUT_HASH_VERIFICATION: PASS
OUTPUT_CANONICALIZATION: PASS
OUTPUT_HASH: PASS
STRUCTURED_COMPARATOR: PASS
FAIL_CLOSED_BEHAVIOR: PASS
TENANT_ISOLATION: PASS
NO_EXTERNAL_IO: PASS
ALLOW_B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_B4_INTERNAL_REPORT_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-CLOSE-REVIEW
```

## 2026-07-11 DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE-GATE

- 只读核对 `e54e607` 的 persisted snapshot record、exact read port、V10 columns、`ReplayInputSnapshot`、`QDR6-CJSON-1`、hasher 与 version vector。
- 确认 V10 虽不单独保存 canonical byte blob，但 record 已包含无损重建 snapshot 的全部 structured fields；实现可从数据库回读 record 重新 canonicalize/hash，无需新增 schema、port、Repository/JDBC 或 API。
- 冻结 `DeterministicReplayCommand`、`DeterministicReplayResult`、`ReplayDifference`、`ReplayReproducibilityStatus`、`ReplayFailureCode`、difference taxonomy、fail-closed mapping 与 `QDR6-MOCK-REPLAY-1` 本地投影语义。
- 明确现有 `QdrRegressionComparator` 不是 replay comparator；下一实现不得调用 provider/prompt/policy runtime，不得重读 V5/V6/V8/V9 mutable source，不得把 `REPRODUCIBLE` 当作任何授权。
- 发现 `README.md`、`docs/current/README.md` 与 supporting `ROADMAP.md` 仍有早期 Stage-QDR-6 入口措辞；因不在本轮 allowlist 未修改，当前以 `STATUS.md`/`WORK_ORDER.md` 为准，不将其误判为 gate blocker。
- `mvn -ntp -Pquality validate` 19/19 `BUILD SUCCESS`，Checkstyle 0、Spotless PASS；tests 与 Docker/Testcontainers 本轮 `NOT_RUN`。
- 本轮仅修改允许的 `docs/current` 文档；未修改 Java、测试、V1-V11、API、port/JDBC 或 wiring，未 commit、未 push、未创建 tag。

```text
DETERMINISTIC_REPLAY_BASELINE_GATE: PASS
REPLAY_INPUT_CONTRACT: FROZEN
EXECUTOR_BOUNDARY: FROZEN
REPRODUCIBILITY_POLICY: FROZEN
DIFFERENCE_TAXONOMY: FROZEN
FAIL_CLOSED_TAXONOMY: FROZEN
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_REPLAY_COMPARATOR_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_CLOSE_REVIEW_NOW: NO
next action: DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE
```

## 2026-07-11 DH-STAGE-QDR-6-B3-P3-CANONICAL-SNAPSHOT-ASSEMBLY-HASH-PERSISTENCE

- 新增完整结构化 `ReplayInputSnapshot`、tenant-bound exact source assembler、`QDR6-CJSON-1` canonicalizer 与 domain-separated SHA-256 hasher；hash domain 固定包含 snapshot schema、canonicalization、executor compatibility 与 hash algorithm version。
- 新增内部 `ReplayInputSnapshotAssemblyService`，在显式 PostgreSQL `REPEATABLE_READ` 事务内完成 source 读取/验证、snapshot 组装、canonicalize/hash、二次 source revalidation、immutable insert 与 exact read-back；transaction manager 缺失、source drift 或任一步异常均 fail-closed/rollback。
- `createdAt` 与 snapshot DB ID 不进入 canonical bytes/hash；write command 仅在合法 canonical hash 生成后构造。optional lineage 缺失保持缺失，legacy/incomplete/unsafe source 结构化拒绝，不读取 raw prompt、raw provider response 或凭证。
- 新增 canonicalizer/assembler 17 个单元测试及 PostgreSQL/wiring 回归；targeted、`dh-app -am`、全仓 Maven 和 quality 均 `BUILD SUCCESS`。全仓 Surefire 986 tests、0 failures/errors/skipped；真实 `postgres:17` / PostgreSQL 17.10，root Checkstyle 0、Spotless PASS。
- 未修改 V1-V11、production port/JDBC、API/Controller 或外部 runtime；未实现 deterministic replay executor，未调用 HTTP/Provider/NQ/Agent/LangGraph/LIVE，未创建 tag，未 push。

```text
STAGE_QDR_6_B3_P3: DONE / IMPLEMENTED / POSTGRESQL_VERIFIED
SNAPSHOT_ASSEMBLER: PASS
QDR6_CJSON_1: PASS
DETERMINISTIC_HASH: PASS
REPEATABLE_READ: PASS
IMMUTABLE_PERSISTENCE: PASS
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
ALLOW_B3_CLOSE_REVIEW: YES
next action: DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE
```

## 2026-07-11 DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW-RETRY

- 在 `dev` / `990c1bb`、clean worktree、empty staged、V1-V10 blocker-fix diff 为空的前提下执行只读复审。
- 确认 write/persisted contract 分离、DB-generated `created_at`、V9 tenant-bound exact structured projection 与 V11 metadata-only forward migration 四个原始 blocker 均关闭。
- 真实运行 targeted usecase/infra、dh-app PostgreSQL、full Maven 与 quality；均 `BUILD SUCCESS`。Snapshot PostgreSQL 13/13、PostgreSQL 17.10、Surefire 合计 965 tests、0 skipped，root Checkstyle 0 violations、Spotless 通过。
- P3 readiness 判定为 PASS；下一步只允许 `structured assembler -> QDR6-CJSON-1 -> SHA-256 -> REPEATABLE_READ identity validation -> immutable persistence`，不允许 deterministic replay executor、schema/port/JDBC/API/provider/runtime 扩张。
- 本轮仅修改允许的 `docs/current` 复审结论文档；未修改代码、测试或 migration，未 commit、未 push、未创建 tag。

## 2026-07-11 DH-STAGE-QDR-6-B3-PERSISTENCE-SCHEMA-BLOCKER-FIX

- 从 `CanonicalReplaySnapshotRecord` 拆出 `CanonicalReplaySnapshotWriteCommand`；write command 不含 `createdAt`，只接受完整 `QDR6-CJSON-1 + SHA-256` material，并拒绝缺失、placeholder、moving alias 与 zero hash。
- persistence port/JDBC insert 改为接收 write command；INSERT 排除 `created_at`，成功后按 tenant-bound exact identity 回读数据库实际 persisted record。
- JDBC 对 V9 replay case 联接 `qdr_replay_input_ref` 与 `qdr_expected_decision_summary`，逐字段 exact compare `ReplayInputRef`、`replay_input_hash`、structured expected summary 和 hash；optional evaluation/verdict 仅在明确存在时校验同一 input/summary lineage。
- duplicate-identical comparison 排除 DB-generated audit time，其余 persisted projection 保持 exact；duplicate-conflict、immutable trigger 与 transaction rollback 行为不变。
- 新增 V11 metadata-only forward migration，为 V10 新增 constraints/indexes 补齐中文 `COMMENT`；未修改 V1-V10，也未改变 schema/data 语义。
- focused PostgreSQL 17.10/Testcontainers 最终 15 tests 全部通过、0 skipped；过程中修复 JSONB `PGobject` array exact projection 解析并重跑。
- 工作单将后续 P3 顺序冻结为 `structured assembler -> canonicalizer -> deterministic hash -> REPEATABLE_READ identity validation -> immutable persistence`；本轮未实现或授权 P3/canonicalizer/replay。

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 2026-07-11 DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW

完成 P1/P2 persistence milestone 的只读复核与 current docs 同步。实际读取 V10、V5/V6/V8/V9 migrations、canonical snapshot contracts、snapshot/prompt/gateway ports 与 JDBC、B1/B2 contracts/aggregate、P1/P2 PostgreSQL 与 architecture tests，并核对两个 commit 的实际 diff。

验证全部 green：targeted `dh-usecase,dh-infra`、`dh-app -am`、full Maven 与 quality 均 `BUILD SUCCESS`；V10 PostgreSQL/Testcontainers 9/9、0 skipped，cross-tenant、immutable trigger 与 transaction rollback 均 PASS。Review 仍发现两类未被测试覆盖的 blocker：P3 在禁止 canonicalizer/hash 时无合法 `canonical_input_hash` 来源且 `created_at` 被 caller-supplied；P2 JDBC 未 exact compare V9 `ReplayInputRef`/hash 与 structured expected summary source projection。

首次 targeted Maven 调用因执行工具 timeout 设置过短被外部终止；随后使用足够 timeout 原命令重跑并通过，故不将首次外部终止误记为代码失败。

```text
B3_PERSISTENCE_MILESTONE_REVIEW: BLOCKED
B3_PERSISTENCE_SCHEMA_MISMATCH_BLOCKED
B3_PERSISTENCE_PORT_BOUNDARY_BLOCKED
TENANT_ISOLATION_REVIEW: PASS
TRANSACTION_BOUNDARY_REVIEW: PASS
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-PERSISTENCE-SCHEMA-BLOCKER-FIX
```

本轮只修改允许的 `docs/current` review/status/work order/testing/worklog 文档；未修改生产代码、测试、migration、API、port/JDBC 或 runtime wiring，未实现 assembler/canonicalizer/hash/replay，未创建 tag，未 commit，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B3-P2-TENANT-BOUND-PORT-JDBC

完成 Stage-QDR-6 B3-P2 tenant-bound persistence expansion。新增 `CanonicalReplaySnapshotPersistencePort`、结构化 persistence/conflict exceptions 与 `JdbcCanonicalReplaySnapshotRepository`；snapshot port 只暴露 `insert`、`findByTenantAndSnapshotId`、`findByTenantAndIdentity`，全部显式携带 tenant boundary，不提供 update/delete/latest/list/scan/fallback。

既有 prompt persistence 增加 `tenantId + promptVersionId` exact lookup；gateway call persistence 增加 `tenantId + decisionRunId + modelCallRef` exact lookup。V10 adapter 使用参数化 SQL 插入全部 required 字段与完整 version vector，并在写入前、读取后重新校验 V5/V6/V8/V9 tenant、physical UUID、business ID、safe ref、trace/request 与 version/checksum。重复相同内容幂等返回；冲突、source drift 与 JDBC failure 均 fail-closed。

```text
STAGE_QDR_6_B3_P2: DONE / IMPLEMENTED / VERIFIED
PORT_EXPANSION: IMPLEMENTED
JDBC_EXPANSION: IMPLEMENTED
TENANT_ISOLATION_EVIDENCE: PASS
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / 0_SKIPPED
ALLOW_B3_PERSISTENCE_MILESTONE_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW
```

验证：P2 PostgreSQL targeted 9/9 PASS；`dh-usecase` 463 tests、`dh-infra` 88 tests、`dh-app` 96 tests，均 0 skipped；全仓 Surefire 957 tests、0 failures/errors/skipped；quality 19/19、Checkstyle 0、Spotless PASS。未修改 V1-V10、API/Controller、runtime wiring；未实现 assembler、canonicalizer、hash 或 deterministic replay；未调用 HTTP/provider/NQ/Agent/LangGraph/LIVE；未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B3-P1-CANONICAL-SNAPSHOT-MIGRATION

完成 Stage-QDR-6 B3-P1 additive canonical snapshot persistence baseline。新增 V10 独立 immutable table、tenant-aware composite unique/FK、strict structured JSON shape、完整 version/hash metadata、total/per-field payload limits、UPDATE rejection trigger、必要索引和中文 COMMENT；未修改 V1-V9，未 backfill legacy row。

新增 `CanonicalReplaySnapshotIdentity`、`CanonicalReplaySnapshotVersionVector` 与 `CanonicalReplaySnapshotRecord`，复用 B1 correlation 和既有 subject/context/evidence/replay contracts，只做 immutable local validation，不新增 port、Repository、JDBC、assembler、canonicalizer、hash 计算或 replay。

```text
P1_PREFLIGHT: PASS
V10_MIGRATION: IMPLEMENTED
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / 0_SKIPPED
STAGE_QDR_6_B3_P1: DONE / IMPLEMENTED / VERIFIED
ALLOW_B3_P2_PORT_JDBC_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-P2-TENANT-BOUND-PORT-JDBC
```

验证：P1 targeted 15/15 PASS；PostgreSQL/Flyway 真实验证 clean V1-V10、V1-V9 upgrade、tenant FK、version/duplicate/orphan/cross-tenant rejection、UPDATE SQLSTATE 55000、total/context/evidence/summary payload limits 和 transaction rollback；全仓 Surefire 952 tests、0 failures/errors/skipped；quality 19/19、Checkstyle 0、Spotless PASS。未新增 API/Controller、JDBC/Repository/query SQL、runtime wiring、HTTP/provider/NQ/Agent/LangGraph 或交易能力；未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER

完成 Stage-QDR-6 B3 persistence gap work order。基于 frozen canonical snapshot contract、Option D persistence review 与实际 V5/V6/V8/V9 schema/ports/JDBC/transaction/test patterns，将后续实现拆成 P1 additive migration + persistence contract、P2 tenant-bound ports/JDBC + identity validation、P3 structured snapshot assembler + local transaction persistence。

```text
SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: DONE
ALLOW_B3_P1_MIGRATION_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_P2_PORT_JDBC_IMPLEMENTATION_NOW: NO
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO
ALLOW_MIGRATION_CHANGE_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-P1-CANONICAL-SNAPSHOT-MIGRATION
```

工作单固化了 P1 数据库 row count/duplicate/null/FK/lock/Flyway transaction preflight、V10 tenant/composite constraints、UPDATE rejection、256 KiB payload gate、P2 exact identity reads、P3 `REPEATABLE_READ` transaction、三层测试矩阵、P1/P2 milestone review 和 forward-only rollback。本轮未修改 Java、测试、migration、V1-V9、API、Repository/JDBC、production port 或 wiring；未实现 assembler/canonicalizer/hash/replay，未连接数据库或外部系统，未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-REVIEW

完成 Stage-QDR-6 B3 snapshot persistence gap design review。基于 V5/V6/V8/V9 schema、ports、JDBC、现有 transaction precedent 与 frozen canonical contract，推荐并冻结 Option D：未来 V10 新增独立 append-only canonical snapshot 表，在单一 tenant-bound transaction 内验证 source identities，并物化完整 allowlisted context、version vector、refs 和 canonical hash。

```text
SNAPSHOT_PERSISTENCE_GAP_REVIEW: DONE
PERSISTENCE_DESIGN_FROZEN: YES
ADDITIVE_MIGRATION_REQUIRED: YES
PRODUCTION_PORT_EXPANSION_REQUIRED: YES
JDBC_EXPANSION_REQUIRED: YES
ALLOW_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: YES
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER
```

本轮未修改 Java、测试、migration、V1-V9、Repository/JDBC/SQL、API/Controller、production port 或 wiring；未实现 assembler/canonicalizer/hash/replay，未调用外部系统，未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B3-CANONICAL-SNAPSHOT-INPUT-CONTRACT-REVIEW

完成 Stage-QDR-6 B3 canonical snapshot input contract 与 persistence sufficiency review。基于真实 V5/V6/V8/V9 migration、write/read model、tenant-bound ports、B1/B2 contracts 和 aggregate service，冻结 `ReplayInputSnapshot` 字段分类、完整 version vector、`QDR6-CJSON-1`、SHA-256 domain-separated hash material、`QDR6-MOCK-REPLAY-1` compatibility 与 fail-closed taxonomy。

```text
CANONICAL_SNAPSHOT_INPUT_CONTRACT: FROZEN
EXISTING_PERSISTENCE_SUFFICIENT: NO
B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED: YES
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION: NO
ALLOW_CANONICALIZER_IMPLEMENTATION: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: NO
next action: DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-REVIEW
```

主要阻断：V5 只有 snapshot metadata；V6 persisted payload 不通过现有 port 暴露；V8 prompt/gateway version 读取与 V6/V8 call identity 不完整；V9 无完整 schema/canonicalization/executor version vector；B2 aggregate 只有 refs/findings。本轮未修改生产代码、测试、V1-V9、Repository/JDBC/SQL、API/Controller、production port 或 runtime wiring，未实现 canonicalizer/hash/replay，未调用外部系统，未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-B2-EVIDENCE-AGGREGATION-SERVICE

完成 Stage-QDR-6 B2 internal evidence aggregation service。实现新增 `DecisionEvidenceAggregateService`、`DecisionEvidenceCorrelationResolver` 与 `DecisionEvidenceConsistencyEvaluator`，复用现有 V5/V6/V8/V9 ports/read models 和 Stage-QDR-5 provider health/readiness/observability safe models；未新增或修改 production port 合同、Repository/JDBC/SQL、migration、API、Controller 或 Spring wiring。

```text
STAGE_QDR_6_B1: DONE / COMMITTED
STAGE_QDR_6_B2: DONE / EVIDENCE_AGGREGATION_EXISTING_PORTS_ONLY
mandatory evidence: missing -> INCOMPLETE
optional evidence: missing -> no fabricated ref
cross-tenant/correlation conflict/duplicate contradiction: INVALID
source read or mapping exception: INVALID + structured finding
deterministic order: evidence refs + findings sorted
raw/sensitive/trading material: rejected
B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED: NOT_TRIGGERED
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION: NO
next action: B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED
```

新增 17 个纯单元测试，覆盖用户要求的 15 类验收及 policy mismatch、稳定排序。定向测试 17/17 PASS；`dh-usecase` 461 tests、0 skipped；全仓 Surefire reports 为 937 tests、0 failures/errors、5 skipped。5 个 skipped 均为 Docker/Testcontainers 相关测试，当前 Docker unavailable，未写成 PASS。`mvn -ntp -Pquality validate` 19/19 SUCCESS，Checkstyle 0 violations，Spotless check passed。

B2 aggregate 只输出 B1 safe refs/findings，不携带 V5 canonical snapshot 内容或完整 replay version input；因此 B2 本身完成，但不足以直接授权 B3 deterministic replay。未创建 tag，未 push。

## 2026-07-11 DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER

完成 Stage-QDR-6 implementation work order。开工前确认 `dev`、工作区 clean，HEAD `5108f24` 已包含 Stage-QDR-6 plan。实际复核了既有 decision evidence/replay views、V5/V6 read model、V8 gateway safe metadata、V9 replay/evaluation/regression repositories 与 Stage-QDR-5 provider readiness/observability service。

```text
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: NOT_STARTED
B1: AUTHORIZED_NEXT
B2: NOT_AUTHORIZED_NOW
B3: NOT_AUTHORIZED_NOW
B4: NOT_AUTHORIZED_NOW
B2 repository expansion blocker: NOT_TRIGGERED / RECHECK_AT_B2_START
B3 snapshot sufficiency blocker: NOT_TRIGGERED / RECHECK_AFTER_B2
next action: DH-STAGE-QDR-6-B1-EVIDENCE-CORRELATION-AGGREGATE-CONTRACTS
```

新增 `docs/current/DH_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER.md`，并最小同步根 `README.md`、current index/status/work order/roadmap/testing/worklog/Codex instructions。未修改生产代码、测试、migration、API、Controller、Repository/JDBC、contracts、golden cases、NQ 或 `docs/gates/**`。

`mvn -ntp -Pquality validate` 为 `BUILD SUCCESS`，19/19 reactor modules 成功，root Checkstyle 0 violations，Spotless check 成功。未运行 `mvn test`、Docker/Testcontainers；未创建 tag、未 push、未暂存文件。

## 2026-07-11 DH-STAGE-QDR-6-PLAN

完成 Stage-QDR-6 docs-only planning。开工前确认 `dev`、工作区 clean、暂存区为空，QDR-4/QDR-5 tags 本地与远程存在，QDR-5 archive packet 与 current cleanup 完成，QDR-6 implementation 未开始。实际检查了 decision pipeline、dry-run wiring、V5/V6/V8/V9、QDR replay/evaluation/regression、provider health/readiness/observability、安全层和相关测试。

规划结论：

```text
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_MAINLINE: DECISION_PIPELINE_EVIDENCE_CONSOLIDATION
stage name: Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
existing unified evidence aggregate: ABSENT
existing deterministic replay executor: ABSENT
existing QDR hash/comparator flow: PRESENT / NOT_A_REPLAY_EXECUTOR
deterministic replay input sufficiency: MUST_BE_PROVEN_IN_IMPLEMENTATION_WO
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER
```

文件变更：新增 `docs/current/DH_STAGE_QDR_6_PLAN.md`，同步 `README.md`、`docs/current/README.md`、`STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md` 与 `CODEX_PROJECT_INSTRUCTIONS.md`。未修改 API.md、DB_SCHEMA.md、FACTSOURCE_POLICY.md、ARCHIVE_INDEX.md 或 `docs/gates/**`。

验证：`mvn -ntp -Pquality validate` 为 `BUILD SUCCESS`，19/19 reactor modules 成功，Checkstyle 0 violations，Spotless 成功。`.\mvnw.cmd -v` 仍输出无效命令与 wrapper jar 无主清单属性，记录为 `WRAPPER_UNUSABLE / P2 TOOLING RISK`。未运行 `mvn test`、Docker 或 Testcontainers，未将其写成 PASS。forbidden-scope diff 与暂存区均为空。

边界：本轮未修改 Java、测试、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ；未接真实 HTTP/provider/SDK，未启动 Agent/LangGraph，未读取凭证，未触碰交易执行链，未创建 tag，未 push。

## 2026-07-09 DH-STAGE-QDR-5-CURRENT-CLEANUP

完成 Stage-QDR-5 tag close 后的 current cleanup。本轮为 documentation-only / post-tag current cleanup，只移动已关闭阶段 source docs、同步 current factsource/index、补充 archive packet source docs 列表，并固化 post-tag current pruning 规则；未修改 Java 生产代码、测试代码、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ。

结论：

```text
STAGE_QDR_5_CURRENT_CLEANUP: DONE
STAGE_QDR_5_SOURCE_DOCS_ARCHIVED: YES
DOCS_CURRENT_QDR5_RESIDUE: NONE
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
ALLOW_STAGE_QDR_6_PLAN: YES / PLANNING_FIRST_ONLY
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-PLAN
```

变更摘要：

```text
docs/current/DH_STAGE_QDR_5*.md: moved to docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5*.md
docs/current: pruned Stage-QDR-5 process source docs and retained current/global factsource entries only
docs/gates/stage-qdr-5: added source docs list and historical archive note
docs/current/ARCHIVE_INDEX.md: synced Stage-QDR-5 TAGGED and source-doc archive pointers
dh-docs-writer / nq-dh-workflow-router: added post-tag current pruning and closed-stage residue blocker rules
```

边界：

```text
no NQ change
no Java production/test change
no migration/API/Controller/repository/JDBC/contract/golden change
no real provider / real HTTP / Provider SDK / Agent / LangGraph / LIVE
no raw prompt / raw provider response / credential persistence
no trading signal
no Stage-QDR-6 planning
no tag
no push
```

## 2026-07-09 DH-DOCS-STAGE-ARCHIVE-POLICY-FIX

完成 stage archive policy 修复与 Stage-QDR-4 / Stage-QDR-5 archive packet backfill。本轮为 documentation policy fix / stage archive packet repair / skill policy fix，只修改 README、`docs/current`、`docs/gates` 与 `.agents` skill policy；未修改 Java 生产代码、测试代码、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ。

结论：

```text
DOCS_STAGE_ARCHIVE_POLICY_FIX: DONE
STAGE_QDR_4_ARCHIVE_PACKET: REPAIRED
STAGE_QDR_5_ARCHIVE_PACKET: REPAIRED
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: NOT_STARTED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
ALLOW_STAGE_QDR_6_PLAN: YES / PLANNING_FIRST_ONLY
ARCHIVE_CLOSE_DIRTY_ACCEPTED_FOR_POLICY_FIX
next action: DH-STAGE-QDR-6-PLAN
```

变更摘要：

```text
dh-docs-writer: 固化 self-contained archive packet、archive-before-tag、tag target commit 与 next-stage planning block。
nq-dh-workflow-router: 补充 archive packet incomplete -> tag blocked、Stage-QDR-6 after Stage-QDR-5 tag close。
stage-qdr-4: 补齐 PLAN / IMPLEMENTATION_WORK_ORDER / BATCH_SUMMARY / VALIDATION_EVIDENCE / FINAL_CLOSE_REVIEW / ARCHIVE_CLOSE / DISCIPLINE_REPAIR / STATUS_SNAPSHOT。
stage-qdr-5: 补齐 PLAN / IMPLEMENTATION_WORK_ORDER / BATCH_SUMMARY / VALIDATION_EVIDENCE / B3_SECURITY_CLOSE_REVIEW / FINAL_CLOSE_REVIEW / ARCHIVE_CLOSE / DISCIPLINE_REPAIR / STATUS_SNAPSHOT。
indexes/current docs: 同步 archive policy repaired、packet required、Stage-QDR-5 tag pending、Stage-QDR-6 not started。
```

验证：

```text
preflight: branch dev, local dh-stage-qdr-5-close tag absent, dirty scope allowed, remote tag check failed with SEC_E_NO_CREDENTIALS
git diff --check: PASS with LF -> CRLF warnings only
forbidden-scope diff: PASS / EMPTY
safety scan: reviewed; hits are forbidden-boundary, historical, negative wording, existing hard-error list phrase, or regex overmatch
quality validate: BUILD SUCCESS, 19/19 reactor success, Checkstyle 0, Spotless passed
mvnw.cmd: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

边界：

```text
no NQ change
no Java production/test change
no migration/API/Controller/repository/JDBC/contract/golden change
no real provider / real HTTP / Provider SDK / Agent / LangGraph / LIVE
no raw prompt / raw provider response / credential persistence
no trading signal
no Stage-QDR-6 planning
no tag
no push
```

## 2026-07-09 DH-STAGE-QDR-5-ARCHIVE-CLOSE

完成 Stage-QDR-5 archive close。本轮为 documentation-only / stage archive close / tag prep，只新增 `docs/gates/stage-qdr-5/README.md` 并同步 README、`docs/current` 与 `docs/gates` 索引；未修改 Java 生产代码、测试代码、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ。

结论：

```text
STAGE_QDR_5_ARCHIVE_CLOSE: DONE
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
ALLOW_STAGE_QDR_6_PLAN: YES / PLANNING_FIRST_ONLY
next action: DH-STAGE-QDR-6-PLAN
```

验证：

```text
preflight: branch dev, worktree clean, final close docs commit c11a0e7 present, dh-stage-qdr-5-close tag absent
git diff --check: PASS with LF -> CRLF warnings only
forbidden-scope diff: PASS / EMPTY
safety scan: reviewed; hits are forbidden-boundary, historical, docs guard, negative wording, or regex overmatch on stage/tag text
quality validate: BUILD SUCCESS, 19/19 reactor success, Checkstyle 0, Spotless passed
mvnw.cmd: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

边界：

```text
no NQ change
no Java production/test change
no migration/API/Controller/repository/JDBC/contract/golden change
no real provider / real HTTP / Provider SDK / Agent / LangGraph / LIVE
no raw prompt / raw provider response / credential persistence
no trading signal
no Stage-QDR-6 planning
no tag
no push
```

## 2026-07-09 DH-STAGE-QDR-5-FINAL-CLOSE-REVIEW

完成 Stage-QDR-5 final close review。本轮为 review-only / docs-current sync，不修改 Java 生产代码、测试代码、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ。审查范围覆盖 B1 Model Gateway Observability Contracts、B2 Provider Health / Gateway Call Read Model、B3 Provider Readiness Guard / Policy Evaluation、B4 Observability Report / Acceptance Support，以及 cross-stage no-real-provider / no-real-HTTP / no-SDK / no-Agent / no-LangGraph / no-LIVE 边界。

结论：

```text
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED
STAGE_QDR_5_ARCHIVE: PENDING
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / PLANNING_FIRST_ONLY
next action: DH-STAGE-QDR-5-ARCHIVE-CLOSE
```

验证：

```text
preflight: branch dev, worktree clean, B4 implementation commit 645cb20 present
targeted tests: 74 tests, 0 failures/errors/skips
scoped tests: dh-domain 151, dh-connector 19, dh-usecase 424 tests, all pass
quality validate: BUILD SUCCESS, 19/19 reactor success, Checkstyle 0, Spotless passed
mvnw.cmd: WRAPPER_UNUSABLE / P2 TOOLING RISK
safety scan: guard/doc/test hits only, no actual runtime/provider/HTTP/SDK/Agent/LangGraph/LIVE implementation found
```

边界：

```text
no NQ change
no Java production/test change
no migration/API/Controller/repository/JDBC/contract/golden change
no real provider / real HTTP / Provider SDK / Agent / LangGraph / LIVE
no raw prompt / raw provider response / credential persistence
no trading signal
no archive close
no tag
no push
```

## 2026-07-09 DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION

完成 Stage-QDR-5 B4 Observability Report / Acceptance Support implementation。本轮新增内部只读 report / acceptance support 结构与 `ObservabilityReportService`，复用 B1 observability summary、B2 provider health read model view 与 B3 readiness evaluation result。实现范围保持在 `dh-usecase` QDR gateway package 与对应单测；未新增 API / Controller、migration、production repository/JDBC、真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph、LIVE 或 NQ 修改。

### Scope

```text
IMPLEMENTATION
OBSERVABILITY_REPORT
PROVIDER_READINESS_ACCEPTANCE_SUPPORT
INTERNAL_REPORT
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReportSection.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessReportSection.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessAcceptanceSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderFailureClassificationSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderLatencyBudgetReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderTrustDecisionReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessEvidenceView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/StageQdr5AcceptanceEvidence.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ObservabilityReportService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ObservabilityReportCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessAcceptanceStatus.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ObservabilityReportSafety.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ObservabilityReportServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation Result

```text
report input boundary: tenant/source safe refs + B1/B2/B3 safe evidence only
report output boundary: safe refs / enum summaries / providerSummaryHash / acceptance status / redacted summary only
acceptance status: PASS / WARN / FAIL / SKIPPED
PASS meaning: Stage-QDR-5 internal acceptance evidence passed only
fail-closed: missing tenant/provider -> FAIL; missing readiness result -> SKIPPED; unsafe/raw/credential/trading/NQ mutation input -> FAIL
security boundary evidence: internal-readonly-report-only
next action: DH-STAGE-QDR-5-FINAL-CLOSE-REVIEW
```

### Boundary

```text
no migration
no V10
no API / Controller / REST endpoint
no production repository / JDBC / persistence adapter
no real provider / HTTP / Provider SDK
no Agent / LangGraph
no LIVE
no NQ change
no raw prompt / raw provider response / credential output
no trading signal
no final close / archive / tag
```

## 2026-07-09 DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO

完成 Stage-QDR-5 B4 Observability Report / Acceptance Support implementation work order。本轮只编制后续 internal report / acceptance support 的实现边界、report input/output boundary、acceptance status boundary、persistence/repository blocker、review trigger、安全门和测试矩阵。未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未进入 B4 implementation，未进入 Stage-QDR-5 final close，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
B4_IMPLEMENTATION_BOUNDARY_DESIGN
OBSERVABILITY_REPORT_WO
PROVIDER_READINESS_ACCEPTANCE_SUPPORT
CURRENT_DOCS_ACCEPTANCE_SUPPORT
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
observability report target: internal report / acceptance support only
allowed structures: ModelGatewayObservabilityReport / ProviderHealthReportSection / ProviderReadinessReportSection / ProviderReadinessAcceptanceSummary / ProviderFailureClassificationSummary / ProviderLatencyBudgetReport / ProviderTrustDecisionReport / ProviderReadinessEvidenceView / StageQdr5AcceptanceEvidence / ObservabilityReportService
report input boundary: B1 observability summary / B2 provider health read model view / B3 readiness evaluation result and safe refs only
report output boundary: safe refs / enum summaries / readiness decision / acceptance status / redacted summary only
acceptance status boundary: PASS / WARN / FAIL / SKIPPED only
persistence boundary: no migration, no production repository/JDBC by default
blockers: B4_REPORT_STRUCTURE_BLOCKER / B4_API_REQUIRED_BLOCKER / B4_SCHEMA_EXTENSION_REQUIRED_BLOCKER / B4_REPOSITORY_EXTENSION_REQUIRED_BLOCKER
test matrix: 20 required implementation checks
next action: DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION
```

### Validation Snapshot

```text
preflight: PASS / dev / worktree clean / B3 implementation commit present / B3 close review commit present
forbidden-scope diff: PASS / EMPTY
safety wording scan: REVIEWED / NEGATIVE_GUARD_AND_EXISTING_DOC_HITS
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
B4 implementation 未启动
Stage-QDR-5 final close 未启动
```

## 2026-07-09 DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-CLOSE-REVIEW

完成 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation security boundary / close review。本轮只做 review 与 `docs/current` 最小同步；未修改 Java 生产代码、测试代码、migration、API、Controller、Repository/JDBC、contracts、golden_cases 或 NQ。

### Scope

```text
REVIEW_ONLY
SECURITY_BOUNDARY_CLOSE_REVIEW
PROVIDER_READINESS_GUARD_REVIEW
POLICY_EVALUATION_REVIEW
TRUST_DECISION_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Close Review Result

```text
structure review: PASS
policy input boundary review: PASS
decision boundary review: PASS
fail-closed review: PASS
security boundary review: PASS
test evidence review: PASS
safety scan review: PASS / GUARD_AND_DOC_HITS_ONLY
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
B3 close review time B4 state: READY_FOR_PLAN_OR_WO / HISTORICAL_RECORD
STAGE_QDR_5_B4_IMPLEMENTATION: NOT_STARTED
```

### Validation Snapshot

```text
preflight: PASS / dev / worktree clean / cfad68a implementation commit present
targeted B3 test: BUILD SUCCESS / ProviderReadinessGuardServiceTest 19 tests
dh-domain,dh-usecase scoped regression: BUILD SUCCESS / dh-domain 151 / dh-connector 19 / dh-usecase 405
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
safety scan: PASS / GUARD_AND_DOC_HITS_ONLY
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未新增 API / Controller
未新增 production Repository / JDBC / persistence adapter
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
B4 implementation 未启动
```

### Next

```text
DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO
```

## 2026-07-09 DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION

完成 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation implementation。本轮只实现内部 readiness policy evaluation、trust gate、redaction guard、fail-closed rules 和 B3 tests；未新增 API、migration、production repository/JDBC、真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph、NQ 或 LIVE。

### Scope

```text
IMPLEMENTATION
PROVIDER_READINESS_GUARD
POLICY_EVALUATION
TRUST_SECURITY_BOUNDARY
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessPolicy.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuard.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessEvaluationResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessDecision.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessDecisionReason.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuardService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuardServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation Result

```text
structures: ProviderReadinessPolicy / ProviderReadinessGuard / EvaluationCommand / EvaluationResult / Decision / DecisionReason / GuardService
policy input boundary: tenantId / sourceRef / providerRef / modelGatewayVersionRef / providerSummaryHash / B1-B2 safe summary-view / policyVersion / trace refs / timestamps only
decision boundary: READY / NOT_READY / DEGRADED / SKIPPED only
fail-closed rules: missing tenant/provider/policy/source denied/policy denied/timeout/budget/unknown/sensitive/raw/trading/NQ mutation/internal exception covered
redaction guard: fixed reason enum retained, safe finding code avoids raw marker echo
authorization/live/trading-signal guard: READY exposes no provider enable flag, HTTP flag, LIVE flag or trading permission
docs sync: B3 DONE, B4 NOT_STARTED, next action close review
```

### Validation Snapshot

```text
targeted B3 test first run: TEST_FIX_REQUIRED / 19 tests / 2 errors
targeted B3 test final: BUILD SUCCESS / 19 tests
dh-domain,dh-usecase scoped regression: BUILD SUCCESS / dh-domain 151 / dh-connector 19 / dh-usecase 405
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
safety wording scan: REVIEWED / GUARD_AND_EXISTING_DOC_HITS
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 NQ
未新增 migration
未新增 API / Controller
未新增 production Repository / JDBC / persistence adapter
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
B4 未启动
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-CLOSE-REVIEW
```

## 2026-07-09 DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO

完成 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation implementation work order。本轮只编制后续 readiness decision、trust gate、fail-closed classification 的实现边界、policy input boundary、decision boundary、fail-closed 规则、security boundary review 触发规则和测试矩阵。未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
B3_SECURITY_BOUNDARY_DESIGN
PROVIDER_READINESS_GUARD_WO
POLICY_EVALUATION_WO
TRUST_DECISION_REVIEW_PREP
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
provider readiness guard target: future-readiness condition evidence only
allowed structures: ProviderReadinessPolicy / Guard / EvaluationCommand / EvaluationResult / Decision / DecisionReason / Finding / GuardService
policy input boundary: tenant/source/provider/gateway/read-model safe view/policyVersion/trace refs only
decision boundary: READY / NOT_READY / DEGRADED / SKIPPED only
fail-closed boundary: missing tenant/provider/policy/source denial/policy denial/timeout/budget/unknown/raw material/trading term fail-closed
security boundary: SECURITY_BOUNDARY_REVIEW_REQUIRED: YES
review triggers: B3 implementation -> security boundary / close review -> B4 only after review PASS
next action: DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION
```

### Validation Snapshot

```text
preflight: PASS / dev / worktree clean / B2 commit present
safety wording scan: REVIEWED / EXISTING_FALSE_POSITIVES_ONLY
forbidden-scope diff: PASS / EMPTY
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未进入 B3 implementation
未进入 B4
```

## 2026-07-09 DH-STAGE-QDR-5-B2-CI-BLOCKER-FIX

完成 Stage-QDR-5 B2 CI blocker fix。本轮只修复 B1/B2 observability guard 触发既有 `ArchitectureTest` source scan 的问题；未扩展 B2 功能，未修改 CI 配置、migration、API、Controller、production repository/JDBC、NQ、provider、HTTP、Provider SDK、Agent、LangGraph 或 LIVE。

### CI Evidence

```text
CI provider: GitHub Actions
failed run: 28998957967 / CI / dev / docs(qdr): define provider health read model work order
failed log access: GH_CLI_UNAVAILABLE_OR_UNAUTHENTICATED / HTTP 403 API rate limit exceeded / gh auth status token invalid
local equivalent failed command: mvn -gs target/codex-maven-settings.xml -B -ntp test
failed job: build & test (Testcontainers / Docker) equivalent local reproduction
failed step: Build and test equivalent local reproduction
failed command: mvn -B -ntp test
failure excerpt: ArchitectureTest stageQdr3B3_rule34 and stageQdr3B4_rule39 reported ModelGatewayObservabilityContractService.java declaring raw prompt/provider response storage fields
B2-related: YES / B1 guard introduced the forbidden raw-storage marker literal under the B2 read-model workline
```

### Fix

```text
root cause: ModelGatewayObservabilityContractService used underscore-form raw marker literals in production source guard; existing ArchitectureTest forbids those storage-field tokens anywhere in qdr.gateway production sources
minimal fix: normalize removes space, underscore and hyphen, then checks only canonical normalized rawprompt/rawproviderresponse/providerraw/prompttext tokens
security impact: fail-closed raw material rejection preserved; no assertion skipped; no ArchitectureTest relaxation
```

### Validation Snapshot

```text
ModelGatewayObservabilityContractServiceTest + ProviderHealthReadModelServiceTest: BUILD SUCCESS / 36 tests
ArchitectureTest: BUILD SUCCESS / 39 tests
ProviderHealthReadModelServiceTest targeted: BUILD SUCCESS / 17 tests
dh-domain,dh-usecase scoped regression: BUILD SUCCESS / dh-domain 151 / dh-connector 19 / dh-usecase 386
quality validate: BUILD SUCCESS / Checkstyle 0 / Spotless passed
CI equivalent full test with clean settings: BUILD SUCCESS / Reactor 19/19 / Testcontainers executed
B2 safety scan: REVIEWED / ALLOWED_GUARD_AND_DOC_HITS_ONLY
ModelGatewayObservabilityContractService raw marker grep: NO_MATCH
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO
```

## 2026-07-09 DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION

完成 Stage-QDR-5 B2 Provider Health / Gateway Call Read Model implementation。本轮只新增 usecase 内部 read model query/view/service 和单元测试；未新增 API / Controller、migration、production repository/JDBC adapter、真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE 能力，未修改 NQ，未创建 tag，未 push。

### Scope

```text
IMPLEMENTATION
PROVIDER_HEALTH_READ_MODEL
MODEL_GATEWAY_OBSERVABILITY_READ_MODEL
INTERNAL_READ_MODEL
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayCallObservabilityView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderFailureClassificationView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderLatencyBudgetView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderTrustDecisionView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSignalView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
read model structures: query、provider health view、gateway call observability view、failure/latency/trust/readiness sub-views、service
query boundary: tenantId 必填；provider/modelGatewayVersion/trace/sourceRequest/failure/trust/readiness selector 或 created/observed window 必填；limit 1..100；offset 非负；无 UUID-only selector
source reuse: 复用 B1 ModelGatewayObservabilitySummary / ProviderHealthSummary / safety contracts；支持从既有 ModelGatewayCallRecord 脱敏 metadata 投影，不扩展 production repository/JDBC
redaction guard: 复用 ModelGatewayObservabilityContractService 与 QdrPersistenceSafety；view 只暴露 safe refs、hash、enum 和 redacted summary
runtime/trading guard: readiness/trust 只作为内部 evidence，不表示 provider authorization、LIVE permission、real provider、real HTTP 或 trading signal
```

### Validation Snapshot

```text
targeted B2 test: mvn -ntp -pl dh-usecase -am "-Dtest=ProviderHealthReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test / BUILD SUCCESS / 17 tests
full validation: see docs/current/TESTING.md after final verification
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO
```

## 2026-07-09 DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO

完成 Stage-QDR-5 B2 Provider Health / Gateway Call Read Model implementation work order。本轮只编制后续 internal read model 的实现边界、query 边界、view 内容边界、persistence/repository blocker、API blocker、review 触发规则、安全门和测试矩阵；未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
B2_IMPLEMENTATION_BOUNDARY_DESIGN
PROVIDER_HEALTH_READ_MODEL_WO
MODEL_GATEWAY_OBSERVABILITY_READ_MODEL
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
read model target: Provider Health / Gateway Call Read Model
allowed structures: ProviderHealthReadModelQuery/View, ModelGatewayCallObservabilityView, ProviderFailureClassificationView, ProviderLatencyBudgetView, ProviderTrustDecisionView, ProviderReadinessSignalView, ProviderHealthReadModelService
query boundary: tenant-bound only; no UUID-only query, tenantless list, cross-tenant read, unbounded list, or pageSize > 100 without reject/cap
view boundary: safe refs, hashes, redacted summary and enum summaries only
persistence boundary: reuse existing model gateway call persistence / QDR gateway result / B1 contracts / QDR safety guards; schema or repository expansion triggers blocker
API boundary: no API / Controller by default; API need triggers blocker
review triggers: API / migration / production repository expansion / security boundary / P0-P1 blocker only
next action: DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未进入 B2 implementation
未进入 B3
```

## 2026-07-09 DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS

完成 Stage-QDR-5 B1 model gateway observability contracts。本轮只新增 usecase 层内存 contract、fail-closed contract service 与 unit test；未新增 migration、API、Controller、Repository/JDBC/persistence、真实 HTTP client、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力，未修改 NQ，未创建 tag，未 push。

### Scope

```text
IMPLEMENTATION
DOMAIN_USECASE_CONTRACTS_ONLY
MODEL_GATEWAY_OBSERVABILITY
PROVIDER_READINESS_FOUNDATION
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilitySummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderFailureClassification.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderLatencyBudgetSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderTrustDecisionSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSignal.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessStatus.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessFinding.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSeverity.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractException.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
contracts: observability summary、provider health summary、failure classification、latency budget、trust decision、readiness status/finding/signal
contract service: ModelGatewayObservabilityContractService validates required refs, safe hash, enum presence, latency/budget bounds, readiness/trust boundaries and fail-closed behavior
failure classification: maps existing ModelGatewayFailureCode to TIMEOUT / BUDGET_EXCEEDED / POLICY_DENIED / SOURCE_DENIED / PROVIDER_UNAVAILABLE / UNKNOWN / PAYLOAD_REJECTED
redaction guard: raw prompt / raw provider response / credential-like text rejected through QdrPersistenceSafety plus B1 raw marker guard
trading guard: BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE rejected as actionable output
runtime guard: readiness cannot enable real provider, real HTTP, LIVE, provider authorization or trading signal
```

### Validation

```text
targeted B1 test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase -am test: BUILD SUCCESS / reactor 9/9
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 / Spotless passed
safety scan: REVIEWED / GUARD_AND_DOC_HITS_ONLY
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
staged: EMPTY
```

### Next

```text
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO
```

## 2026-07-09 DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER

完成 Stage-QDR-5 implementation work order。本轮只编制 B1-B5 批次边界、测试矩阵、review 触发规则、安全门和后续提交纪律；未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
STAGE_QDR_5_IMPLEMENTATION_PLANNING
MODEL_GATEWAY_OBSERVABILITY_WO
PROVIDER_READINESS_HARDENING_WO
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
Stage-QDR-5 implementation: NOT_STARTED
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
```

### Work Order Result

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
review triggers: migration / API / security boundary / stage close / P0-P1 blocker only
stage order: final close review PASS -> archive close docs commit -> worktree clean -> annotated tag -> tag push -> next stage planning
```

### Files Changed

```text
README.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Next

```text
DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
```

## 2026-07-09 DH-STAGE-QDR-5-PLAN

完成 Stage-QDR-5 planning。本轮只做规划与 current docs 同步；未修改 Java、测试、migration、API、contracts、golden_cases 或 NQ；未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未创建新 tag，未 push。

### Scope

```text
PLANNING_ONLY
STAGE_QDR_5_SCOPE_DESIGN
POST_QDR_REPLAY_EVALUATION_PLAN
MODEL_GATEWAY_OBSERVABILITY_REVIEW
PROVIDER_READINESS_BOUNDARY_REVIEW
SECURITY_BOUNDARY_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
AGENTS.md
README.md
pom.xml
scripts/verify.ps1
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V8__qdr_model_gateway_persistence_baseline.sql
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
```

### Files Changed

```text
README.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_PLAN.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/ARCHIVE_INDEX.md
```

### Plan Result

```text
recommended direction: Model Gateway Observability / Provider Readiness Hardening
deferred B: QDR Regression Baseline Hardening
deferred C: Real Provider Dry-run Readiness Plan
deferred D: Agent / LangGraph Preparation
stage-qdr-5 name: Model Gateway Observability / Provider Readiness Hardening
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Current Docs / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
review trigger: migration / API / Controller / security boundary / stage close / P0-P1 blocker only
next action: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
```

### Validation

```text
Get-Location: PASS / F:\project\decision-hub
branch: dev
git status --short before writing: PASS / CLEAN
tag dh-stage-qdr-4-close: PASS / local and remote visible
tag target: PASS / 62c802064f637ad03d3b0f4a185bd55fa3141af2
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --cached --name-only: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety wording scan: REVIEWED / HISTORICAL_ALLOWED_HITS_ONLY
mvn -ntp -Pquality validate: PASS / BUILD SUCCESS / reactor 19/19
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未读取 credential / token / cookie / apiKey / apiSecret / passphrase
未持久化 raw prompt
未持久化 raw provider response
未修改 NQ
未启动 Agent runtime
未接 LangGraph runtime
未开启 LIVE
未进入 Stage-QDR-5 implementation
未创建新 tag
未 push
```

## 2026-07-09 DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION

执行 DH 文档纪律修复。本轮只修改文档、workflow guard 和 `.agents` skill policy；未修改 Java、测试、migration、API、contracts、golden_cases 或 NQ；未创建 tag，未 push，未进入 Stage-QDR-5。

### C1 Authority Repair

```text
AGENTS.md: current factsources 修复为 docs/current 实际存在入口；移除不存在的 WORKFLOW / DH_NQ_INTEGRATION / DH_REFACTOR_STAGE1 current 必读入口。
scripts/verify.ps1: current gate 改为 docs/current/STATUS.md、WORK_ORDER.md、CODEX_PROJECT_INSTRUCTIONS.md；docs/codex 仅做 historical JSON sanity check。
docs/codex/WORK_ORDER.md: 降权为 historical / non-authoritative。
docs/codex/plans/_active/STATUS.json: 增加 HISTORICAL_NON_AUTHORITATIVE 元数据。
```

### C2 Skill Policy Repair

```text
.agents/README.md: active skills 同步为 10 个。
.agents/MERGE_MAP.md: 增加 workflow governance skills 与 8 个 implementation/review skills 的关系。
nq-dh-workflow-router: 固化 archive-before-tag、stage tag after archive commit、review cadence、Stage-QDR-5 planning-first、路径/worktree 纪律。
dh-docs-writer: 固化 minimal docs sync、archive/tag 分离、historical docs 不批量重写。
db-schema-migration-review: Gate freeze 扩展为 Gate / DH Stage；明确 migration review 触发条件与 no migration batch 不强制 freeze。
```

### C3 Current Docs Repair

```text
README.md / docs/current/README.md / STATUS.md / WORK_ORDER.md / ROADMAP.md / CODEX_PROJECT_INSTRUCTIONS.md / FACTSOURCE_POLICY.md:
  STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
  STAGE_QDR_4_TAG: PENDING
  ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
  ALLOW_STAGE_QDR_5_PLAN_NOW: NO
  ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO

docs/README.md: 移除旧 DH-REFIT current stage。
docs/current/ARCHIVE_INDEX.md: 修复 stage-qdr-3 摘要中 stage-qdr-4 implementation NOT_STARTED / NO 残留。
```

### C4 Supporting Docs Noise Reduction

```text
docs/current/API.md: 只更新 supporting current summary，不重写历史 API 记录。
docs/current/DB_SCHEMA.md: 只更新 supporting current summary，不重写历史 schema 记录。
docs/gates/dh-stage4-decision-pipeline-mvp/README.md: 增加 legacy GateK wording errata/index；不改冻结历史正文。
.agents/AGENTS.frontend-skill-routing.md: later decision，未 merge/delete。
```

### Validation

```text
git diff --check: PASS_AFTER_FIX
safety scan: REVIEWED / CLASSIFIED / NO_ACTUAL_RISK
forbidden-scope diff: PASS / EMPTY
mvn -ntp -Pquality validate: PASS / BUILD SUCCESS / reactor 19/19
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
local tag dh-stage-qdr-4-close: NOT_EXISTS
remote tag dh-stage-qdr-4-close: NO_MATCH
```

下一步：如果工作区保持 docs-only 且用户进入 tag close 任务，执行 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 仍只能在 tag close 后 planning-first。

## 2026-07-09 DH-STAGE-QDR-4-CURRENT-DOCS-CLEANUP

清理 `docs/current` 下已归档的 Stage-QDR-4 详细阶段文档。`DH_STAGE_QDR_4*.md` 的实际内容已在 `docs/gates/stage-qdr-4/` 存档，本轮从 current 目录移除这些长文档，并把 current factsource 列表改为状态入口、执行纪律、验证记录和归档索引。

### Removed From Current

```text
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

### Remaining Source

```text
docs/gates/stage-qdr-4/
```

### Validation Result

```text
docs/current/DH_STAGE_QDR_4*.md: EMPTY
docs/gates/stage-qdr-4/DH_STAGE_QDR_4*.md: 7 archived files
git diff --check: PASS
forbidden-scope diff: PASS / EMPTY
mvn -ntp -Pquality validate: PASS
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
tag created: NO
push: NO
```

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CONTENT-FIX

补齐 Stage-QDR-4 归档目录的实际阶段文档。上一轮 archive close commit 已创建 `docs/gates/stage-qdr-4/README.md` 并同步 current factsources，但目录下缺少 `DH_STAGE_QDR_4*.md` 阶段文档副本；本轮将 7 个 Stage-QDR-4 current stage docs 复制到 `docs/gates/stage-qdr-4/`，并同步 `ARCHIVE_INDEX.md` 与 gates README。

### Files Archived

```text
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

### Boundary

```text
No code change
No test change
No migration
No API
No real provider
No real HTTP
No Agent
No LangGraph
No LIVE
No tag created
No push
```

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CLOSE

完成 Stage-QDR-4 archive close docs sync。本轮只同步 README、docs/current 与 docs/gates 归档入口，把 Stage-QDR-4 final close review `PASS` 收口为 `CLOSED / ACCEPTED / ARCHIVED`，并保持 tag 状态为 `PENDING`。

### Scope

```text
DOCUMENTATION_ONLY
STAGE_ARCHIVE_CLOSE
QDR_REPLAY_EVALUATION_REGRESSION_ARCHIVE
TAG_PREP
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Archive Actions

```text
Stage-QDR-4 archive close docs sync: DONE
README/current factsources: UPDATED
docs/current/ARCHIVE_INDEX.md: UPDATED
docs/gates/stage-qdr-4/README.md: CREATED
No code change
No test change
No migration
No API
No tag created
Next tag pending: dh-stage-qdr-4-close
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 Stage-QDR-5 implementation
未打 tag
未 push
```

### Next

```text
DH-STAGE-QDR-4-TAG-CLOSE
```

## 2026-07-08 DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW

完成 Stage-QDR-4 final close review。审查范围仅限 B1 replay/evaluation domain contracts、B2 persistence baseline、B3 mock gateway regression integration、B4 regression report/read model support 与 current docs 同步；本轮未修改 Java、测试、migration、API、Controller、Repository、Service、contracts、golden_cases 或 NQ。

### Scope

```text
REVIEW_ONLY
STAGE_FINAL_CLOSE
QDR_REPLAY_EVALUATION_REGRESSION_ACCEPTANCE
TAG_PREP
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B1 evidence: PASS
B2 evidence: PASS
B3 evidence: PASS
B4 evidence: PASS
cross-stage boundary: PASS
scoped Maven tests: BUILD SUCCESS
quality validate: BUILD SUCCESS
safety scan: REVIEWED / ALLOWED_HITS_ONLY
mvnw.cmd: WRAPPER_UNUSABLE / P2 TOOLING RISK
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_COMMIT: YES
ALLOW_STAGE_QDR_5_PLAN: YES
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 Stage-QDR-5
未打 tag
```

### Next

```text
DH-STAGE-QDR-4-TAG-CLOSE
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION

完成 stage-qdr-4 B4 regression report / read model support implementation。本轮先执行 dirty worktree triage，确认 dirty 范围全部属于 B4 允许文件后继续 resume；只新增 usecase/internal read model 结构与回归测试，复用 B2/V9 repository ports；未新增 migration、未修改 V9、未新增 API / Controller、未新增生产 JDBC 查询、未接 provider / HTTP / Agent / LangGraph / LIVE。

### Scope

```text
IMPLEMENTATION
REGRESSION_REPORT_READ_MODEL
QDR_REPLAY_EVALUATION_REPORTING
INTERNAL_READ_MODEL
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportFindingView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionDriftSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReadModelService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReadModelServiceTest.java
docs/current/STATUS.md
docs/current/README.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
read model target: internal regression report / read model support
structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundary: tenantId required; caseId/evaluationId/verdictId/trace/sourceRequest/sourceDecision/verdict/severity/created range supported; limit/offset required; max pageSize 100
report content: safe refs, IDs, decision summary labels, verdict/severity, providerSummaryHash, modelGatewayVersionRef, policyVersion, redactedSummary, driftSummary, findings
drift summary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository only
redaction: view/finding/safe ref construction reruns redaction guard and rejects raw/sensitive/executable content
trading-term guard: actionLabel remains read-only direction label; executable trading terms are not exposed as allowed action
docs sync: current STATUS / WORK_ORDER / ROADMAP / TESTING / WORKLOG / CODEX_PROJECT_INSTRUCTIONS updated
```

### Validation

```text
dirty scope triage: DIRTY_SCOPE_ACCEPTED_FOR_B4_RESUME / dirty files only in B4 allowed scope
mvn -ntp -pl dh-usecase -am "-Dtest=RegressionReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 15 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / reactor 15/15
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 / Spotless passed
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
V9QdrReplayEvaluationFlywayPostgresTest: PASS / PostgreSQL 17 / Flyway v9
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
forbidden-scope dirty check: PASS / EMPTY
```

### Boundary

```text
未修改 NQ
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未访问 credential / token / cookie / apiKey / apiSecret / passphrase
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 Stage-QDR-4 final close
未打 tag
```

### Next

```text
DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO

完成 stage-qdr-4 B4 regression report / read model support implementation work order。B4 WO 只冻结后续 implementation 的 usecase/internal read model 边界、tenant-bound query、report content、drift summary、B2/V9 persistence reuse、redaction / trading-term guard、fail-closed、测试矩阵、validation、安全扫描、review / close 和 tag 后置规则。本轮未修改 Java、测试、migration、API、README、NQ 或 runtime。

### Scope

```text
WORK_ORDER_ONLY
B4_IMPLEMENTATION_BOUNDARY_DESIGN
REGRESSION_REPORT_READ_MODEL_WO
QDR_REPLAY_EVALUATION_REPORTING
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
report/read model target: replay case summary, evaluation case summary, expected vs actual summary, regression verdict summary, finding list, drift summary, version refs, redaction/trading-term guard evidence, tenant-bound query support
allowed structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundary: tenantId + caseId/evaluationId/verdictId/createdAt/verdict/severity/traceId/sourceRequestId/sourceDecisionId; no UUID-only, tenantless, cross-tenant, unbounded list, pageSize > 100 without reject/cap
report content boundary: safe refs/hash/version/redacted summary only; no raw prompt/provider response/credential/trading or mutation payload
drift summary boundary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 V9 seven tables and existing B2 repository only; no V10; no V9 modification; no new table; schema gap must output B4_SCHEMA_GAP_BLOCKER
API/controller decision: default no API / no Controller; if required, output B4_API_REQUIRED_BLOCKER
test matrix: 20 items recorded in TESTING.md and WO
validation commands: git diff/status, scoped Maven test, quality validate, mvnw risk check
safety scan: raw/sensitive/trading/runtime scan required for implementation
review/close rule: usecase/internal read model can proceed to validation + commit; API/migration/security expansion requires review
tag rule: stage tag only after Stage-QDR-4 final close PASS; B4 does not tag
next task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety wording scan: REVIEWED / ALLOWED_HITS_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN

完成 stage-qdr-4 B4 regression report / read model support planning。B4 plan 只规划 tenant-bound 只读报告与 read model 支撑，用于 B1-B3 replay/evaluation/regression 结果的 report summary、finding list、drift summary、redaction / trading-term guard evidence 和 safe refs 复核。本轮未修改 Java、测试、migration、API、README、NQ 或 runtime。

### Scope

```text
PLANNING_ONLY
REGRESSION_REPORT_READ_MODEL_PLAN
QDR_REPLAY_EVALUATION_REPORTING
READ_MODEL_BOUNDARY_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
AGENTS.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/WORKFLOW.md: NOT_FOUND / NON_BLOCKING
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/readmodel/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/readmodel/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayEvaluationPersistenceRepositoryTest.java
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Plan Result

```text
report/read model target: replay case summary, evaluation case summary, expected vs actual summary, regression verdict summary, finding list, drift summary, redaction/trading-term guard evidence
planned structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundaries: tenantId + caseId/evaluationId/verdictId/createdAt/verdict/severity/traceId/sourceRequestId/sourceDecisionId; no UUID-only, tenantless, cross-tenant, unbounded list
report content boundary: safe refs/hash/version/redacted summary only; no raw prompt/provider response/credential/trading or mutation payload
drift summary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 V9 seven tables only; no V10; no V9 modification; no new table
API/controller decision: default no API / no Controller; if required, output B4_API_REQUIRED_BLOCKER
test matrix: 20 B4 implementation tests planned
review/freeze rule: internal read model can proceed via implementation validation; API/migration/security expansion requires separate review
tag rule: B4 is not final close; no tag before Stage-QDR-4 final close
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2/B3 required commits and B3 close commit e237504 docs(qdr): close mock gateway regression integration
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
final validation: see TESTING.md same-date B4 plan entry
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW

完成 stage-qdr-4 B3 mock gateway regression integration close review。审查确认 B3 implementation commit 已提交，工作区开工前 clean，target flow、usecase/service、comparator、B2 repository 复用、redaction、trading-term guard、test evidence 与 safety scan 均满足 close 条件。B3 正式关闭为 `CLOSED / ACCEPTED`，仅允许进入 B4 planning，不允许直接进入 B4 implementation。

### Scope

```text
REVIEW_ONLY
CLOSE_REVIEW
MOCK_GATEWAY_REGRESSION_REVIEW
QDR_PIPELINE_REVIEW
REPOSITORY_REUSE_REVIEW
REDACTION_REVIEW
TRADING_TERM_GUARD_REVIEW
TEST_EVIDENCE_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B3 implementation commit: 54e5575 feat(qdr): integrate mock gateway regression baseline
worktree: CLEAN before close docs update
target flow review: PASS
usecase/service review: PASS
comparator review: PASS
persistence reuse review: PASS
redaction review: PASS
trading-term review: PASS
test evidence review: PASS
safety scan review: PASS / ALLOWED_HITS_ONLY
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
```

### Validation

```text
git status --short: PASS / CLEAN before docs update
git branch --show-current: dev
git log --oneline -20: HEAD 54e5575 feat(qdr): integrate mock gateway regression baseline
git diff --check: PASS
git diff --stat: PASS / EMPTY before docs update
git diff --name-only: PASS / EMPTY before docs update
git diff --cached --name-only: PASS / EMPTY
git diff --name-only 54e5575^ 54e5575 -- dh-app/src/main/resources/db/migration dh-api dh-app/src/main/java contracts golden_cases: PASS / EMPTY
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / reactor 15/15 / Testcontainers PostgreSQL 17 / Flyway v9 verified
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 violations / Spotless passed
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4 implementation
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION

完成 stage-qdr-4 B3 mock gateway regression integration implementation。B3 本轮只实现 deterministic mock gateway regression flow，把 existing dry-run / QDR decision artifact、mock model gateway safe summary、B2 replay/evaluation persistence ports、expected/actual decision summary、regression comparator、verdict 和 finding list 串成可测试闭环。本轮未新增 migration，未修改 V1-V9，未新增 API / Controller，未接真实 provider / HTTP / Agent / LangGraph / LIVE，未修改 NQ。

### Scope

```text
IMPLEMENTATION
MOCK_GATEWAY_REGRESSION
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_INTEGRATION
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/**
pom.xml
```

### Files Changed

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/RegressionEvidenceRef.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionComparator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionSafety.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionBaselinePolicy.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionComparatorTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / QDR decision artifact -> mock model gateway summary -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> regression verdict -> finding list
usecase/service: QdrRegressionEvaluationService orchestrates comparator + case builder + ReplayCaseRepository + EvaluationCaseRepository + RegressionVerdictRepository
case builder: MockGatewayRegressionCaseBuilder creates deterministic tenant-bound IDs, input/output refs, summary hashes, replay/evaluation/verdict/finding commands
comparator: QdrRegressionComparator compares decisionType, actionLabel, confidenceBand, riskLevel, evidenceRefs, forbiddenActions, providerSummaryHash, modelGatewayVersionRef, promptVersionRef, policyVersion and returns PASS / WARN / FAIL / SKIPPED
policy: RegressionBaselinePolicy controls confidence tolerance, provider hash mismatch, risk increase and policy mismatch skip behavior
redaction: RegressionEvidenceRef / QdrRegressionSafety reuse B2 ReplayPersistenceGuard and reject raw prompt/provider response/credential-like fields
trading-term guard: expected actionLabel rejects BUY / SELL / MARKET_ORDER; executable allowed actions reject PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE; verdict/finding remain regression evidence only
persistence reuse: B2/V9 repository ports reused; no V10, no V9 modification, no new table
docs sync: STATUS / WORK_ORDER / ROADMAP / TESTING / WORKLOG / CODEX_PROJECT_INSTRUCTIONS updated for B3 implementation DONE and close review next action
STAGE_QDR_4_B3_IMPLEMENTATION: DONE
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW
```

### Validation

```text
dirty worktree audit: all existing dirty/untracked files are within B3 allowed scope
git branch --show-current: dev
git log --oneline -20: contains B1/B2/B3 WO commits
mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / Testcontainers PostgreSQL 17 / Flyway v9 verified
mvn -ntp -Pquality validate: BUILD SUCCESS / Checkstyle 0 violations / Spotless passed
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 NQ
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO

完成 stage-qdr-4 B3 mock gateway regression integration work order。B3 WO 只冻结 future implementation 的 target flow、usecase/service boundary、comparison rules、B2 persistence reuse、redaction/trading guard、fail-closed 行为、测试矩阵、validation 和 close review 顺序。本轮未修改 Java、测试、migration、API、NQ 或 runtime。

### Scope

```text
WORK_ORDER_ONLY
B3_IMPLEMENTATION_BOUNDARY_DESIGN
MOCK_GATEWAY_REGRESSION_WO
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_INTEGRATION_WO
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/**/qdr/**
dh-infra/src/test/java/**/qdr/**
dh-app/src/test/java/**
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / mock gateway summary -> replay case -> evaluation case -> expected decision summary -> regression comparison -> regression verdict -> finding list
usecase boundaries: MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef
comparison rules: decisionType / actionLabel / confidenceBand / riskLevel / evidenceRefs / forbiddenActions / providerSummaryHash / modelGatewayVersionRef / promptVersionRef / policyVersion
persistence reuse: B2 V9 seven tables only; no V10; no V9 change; no new table
redaction rules: no raw prompt/provider response/credential; safe ref/hash/redacted summary only
trading-term rules: BUY/SELL/MARKET_ORDER not expected action; PLACE_ORDER/CANCEL_ORDER/MUTATE_NQ_STATE not allowed action; LONG_BIAS/SHORT_BIAS direction label only
test matrix: 22 B3 implementation tests planned
review/freeze rules: B3 plan -> B3 WO -> B3 implementation -> B3 close review -> B4 plan
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2 plan/WO/implementation/close review commits
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety scan: REVIEWED / NO_ACTUAL_RISK
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
未进入 B4
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN

完成 stage-qdr-4 B3 mock gateway regression integration planning。B3 plan 只规划 future implementation 如何复用 existing dry-run / mock gateway safe refs / B2 replay-evaluation persistence baseline，串起 replay case、evaluation case、regression comparison、verdict 和 finding list。本轮未修改 Java、测试、migration、API、NQ 或 runtime。

### Scope

```text
PLANNING_ONLY
MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_BOUNDARY_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/**/qdr/**
dh-infra/src/test/java/**/qdr/**
dh-app/src/test/java/**
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / mock gateway safe refs -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> verdict -> finding list
usecase boundaries: MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef planned only
comparison rules: decisionType / actionLabel / confidenceBand / riskLevel / evidenceRefs / forbiddenActions / providerSummaryHash / modelGatewayVersionRef / promptVersionRef / policyVersion planned
persistence reuse: B2 V9 seven tables only; no V10; no V9 change
redaction rules: no raw prompt/provider response/credential; JSONB safe ref/hash/summary only
trading-term rules: BUY/SELL/MARKET_ORDER not expected action; PLACE_ORDER/CANCEL_ORDER/MUTATE_NQ_STATE not allowed action; LONG_BIAS/SHORT_BIAS direction label only
test matrix: 20 B3 implementation tests planned
review/freeze rules: B3 plan -> B3 WO -> B3 implementation -> B3 close review -> B4 plan
STAGE_QDR_4_B3_PLAN: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2 plan/WO/implementation/close review commits
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety scan: REVIEWED / NO_ACTUAL_RISK
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
未进入 B4
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW

完成 stage-qdr-4 B2 persistence baseline close review。审查确认 B2 implementation commit 已存在且工作区 clean；V9 PostgreSQL/Flyway Testcontainers load、scoped tests、quality validate 与 safety scan 均满足 close 条件。B2 正式关闭为 `CLOSED / ACCEPTED`，仅允许进入 B3 planning，不允许直接进入 B3 implementation。

### Scope

```text
REVIEW_ONLY
CLOSE_REVIEW
MIGRATION_REVIEW
REPOSITORY_REVIEW
TENANT_ISOLATION_REVIEW
REDACTION_REVIEW
TEST_EVIDENCE_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B2 implementation commit: 3fe1bab feat(qdr): persist replay evaluation baseline
worktree: CLEAN before close docs update
migration review: PASS
repository review: PASS
tenant isolation review: PASS
redaction review: PASS
trading-term review: PASS
test evidence review: PASS
safety scan review: PASS / NO_ACTUAL_RISK
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B3_PLAN: YES
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
```

### Validation

```text
git status --short: PASS / CLEAN before docs update
git diff --check: PASS
git diff --stat: PASS / EMPTY before docs update
git diff --name-only: PASS / EMPTY before docs update
git diff --cached --name-only: PASS / EMPTY
mvn -ntp -pl dh-app -am "-Dtest=V9QdrReplayEvaluationFlywayPostgresTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / PASS / NOT_SKIPPED
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / PASS / NOT_SKIPPED
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
required safety scan: REVIEWED / NO_ACTUAL_RISK
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-BLOCKER-FIX

完成 B2 implementation 唯一阻断项修复：恢复并使用本机 Docker/Testcontainers，实证 `V9QdrReplayEvaluationFlywayPostgresTest` 在 PostgreSQL 17 Testcontainer 中执行并通过，Flyway validated 9 migrations，并成功迁移到 version v9。本轮未修改 migration 语义、未新增功能、未进入 B3。

### Scope

```text
BLOCKER_FIX
TESTCONTAINERS_VALIDATION
FLYWAY_POSTGRES_LOAD_VERIFICATION
NO_FEATURE_EXPANSION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
docker status: VERIFIED / Docker Desktop 29.6.1 / PostgreSQL 17 Testcontainers runnable
V9 Flyway PostgreSQL load: PASS
Testcontainers result: PASS / NOT_SKIPPED
migration changes: NONE
test changes: NONE
docs sync: DONE
STAGE_QDR_4_B2_IMPLEMENTATION: DONE / POSTGRES_FLYWAY_VERIFIED
ALLOW_STAGE_QDR_4_B2_CLOSE_REVIEW: YES
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW
```

### Boundary

```text
未修改 NQ
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION

完成 stage-qdr-4 B2 persistence baseline 的代码实现与大部分验证：新增 V9 migration、tenant-bound repository ports、JDBC adapters、redaction guard、trading-term guard、repository / migration / tenant isolation / fail-closed tests，并完成 docs/current 最小同步。本轮未新增 API、Controller、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime、NQ mutation 或 LIVE 能力。

### Scope

```text
IMPLEMENTATION
MIGRATION
REPOSITORY
TESTS
QDR_REPLAY_EVALUATION_PERSISTENCE
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcRegressionVerdictRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayPersistenceSupport.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/test/java/com/guidinglight/decisionhub/V9QdrReplayEvaluationBaselineMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V9QdrReplayEvaluationFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DB_SCHEMA.md
```

### Result

```text
stage-qdr-4 B2 implementation: BLOCKED / IMPLEMENTED / FLYWAY_POSTGRES_LOAD_UNVERIFIED
V9 migration: CREATED / qdr replay evaluation baseline
repository ports: DONE / TENANT_BOUND
JDBC adapters: DONE / TENANT_BOUND
redaction guard: DONE
trading-term guard: DONE
docs sync: DONE
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-BLOCKER-FIX
```

### Boundary

```text
未修改 NQ
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 mock gateway regression integration
```

### Validation

```text
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / WITH_DOCKER_SKIPS
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
docker ps: DOCKER_UNAVAILABLE
required safety wording scan: REVIEWED / ALLOWED_HITS_ONLY
V9 PostgreSQL/Flyway Testcontainers load: SKIPPED / DOCKER_UNAVAILABLE / NOT_PASS
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION-WO

完成 stage-qdr-4 B2 persistence baseline implementation work order。本轮只编制后续实现边界，未实现 Java、测试、migration、repository、API、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
WORK_ORDER_ONLY
B2_IMPLEMENTATION_BOUNDARY_DESIGN
MIGRATION_IMPLEMENTATION_WO
REPOSITORY_IMPLEMENTATION_WO
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 implementation: NOT_STARTED / NO
migration boundary: only V9__qdr_replay_evaluation_baseline.sql in next implementation
repository boundary: ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository and JDBC adapters only
tenant isolation: all save/find/list/update methods must be tenant-bound
redaction boundary: raw prompt / raw provider response / credential forbidden
test matrix: 20 required implementation checks
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V8 migration
未新增 V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden scope diff: PASS / EMPTY
safety wording scan: REVIEWED / FALSE_POSITIVE_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

## 2026-07-08 DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN

完成 stage-qdr-4 B2 replay / evaluation persistence baseline plan。本轮只做规划，未实现 Java、测试、migration、repository、API、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
PLANNING_ONLY
PERSISTENCE_BASELINE_DESIGN
MIGRATION_REVIEW_PREP
QDR_REPLAY_EVALUATION
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DB_SCHEMA.md
docs/current/WORKFLOW.md: NOT_FOUND
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/model/QdrPersistenceSafety.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayCallPersistencePort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/SaveModelGatewayCallCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/DecisionRequestRepository.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractServiceTest.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcDecisionCoreRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/model/JdbcModelGatewayCallRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcHumanApprovalPacketRepository.java
dh-app/src/main/resources/db/migration/V6__qdr_decision_core_baseline.sql
dh-app/src/main/resources/db/migration/V8__qdr_model_gateway_persistence_baseline.sql
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Plan Result

```text
tables: qdr_replay_case, qdr_evaluation_case, qdr_expected_decision_summary, qdr_regression_verdict, qdr_regression_finding, qdr_replay_input_ref, qdr_replay_output_ref
migration plan: V9__qdr_replay_evaluation_baseline.sql planned only / not created
repository boundaries: ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository planned only / not implemented
tenant isolation: tenant_id required on all tables and all future query methods
redaction policy: raw prompt / raw provider response / credential forbidden; persisted content limited to summary/hash/version/status/failure classification
review/freeze: B2 implementation 前必须 freeze/review；implementation 后必须 close review
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-FREEZE-REVIEW
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V8 migration
未新增 V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden scope diff: PASS / EMPTY
safety wording scan: REVIEWED / EXISTING_FALSE_POSITIVE_IN_UNMODIFIABLE_FILE
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

## 2026-07-08 DH-STAGE-QDR-4-B1-REPLAY-EVALUATION-DOMAIN-CONTRACTS

完成 stage-qdr-4 B1 replay / evaluation domain contracts。B1 只实现内存领域合同、usecase fail-closed 校验和单元测试，不新增 migration、API、Controller、Repository、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
IMPLEMENTATION
DOMAIN_CONTRACTS_ONLY
QDR_REPLAY_EVALUATION_BASELINE
NO_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
ReplayCase / EvaluationCase: tenant-bound replay/evaluation case contracts
ExpectedDecisionSummary: structured summary only; LONG_BIAS / SHORT_BIAS remain bias labels
EvaluationPolicy: structured comparison policy; raw provider response comparison is rejected
RegressionVerdict: PASS / FAIL / WARN / SKIPPED with failure reason and findings
ReplayEvaluationContractService: validates required fields, trading-mutation terms, policy safety, and B1 initial SKIPPED verdict
```

### Validation

```text
mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 13 tests
mvn -ntp -pl dh-domain,dh-usecase -am test: BUILD SUCCESS
```

### Boundary

```text
未修改 NQ
未新增 migration
未修改 V1-V8 migration
未新增 API / Controller
未新增 Repository / JDBC persistence
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Next

```text
DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
```

## 2026-07-08 DH-STAGE-QDR-4-PLAN

完成 stage-qdr-4 planning。推荐唯一主线为 `QDR Replay / Evaluation / Regression Baseline`，并把后续动作限定为 implementation work order，不直接启动 implementation。

### Scope

```text
PLANNING_ONLY
STAGE_QDR_4_SCOPE_DESIGN
POST_MODEL_GATEWAY_HARDENING_PLAN
REPLAY_EVAL_PROVIDER_READINESS_REVIEW
SECURITY_BOUNDARY_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 implementation: NOT_STARTED / NO
recommended direction: QDR Replay / Evaluation / Regression Baseline
next action: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

### Plan Result

```text
recommended direction: A / QDR replay, evaluation and regression baseline
deferred B: Model gateway observability / provider readiness hardening
deferred C: real provider dry-run readiness plan
deferred D: Agent / LangGraph preparation
batches: B1 replay/eval contracts; B2 persistence baseline; B3 mock gateway regression integration; B4 report/read model/docs support; B5 close review
next task: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

### Boundary

```text
本轮不修改 Java 生产代码
本轮不修改 Java 测试代码
本轮不新增 migration
本轮不修改 V1-V8 migration
本轮不新增 V9 migration
本轮不新增 API / Controller / REST endpoint
本轮不新增真实 HTTP outbound
本轮不新增真实 provider client
本轮不新增 Provider SDK
本轮不启动 Agent / LangGraph runtime
本轮不修改 NQ
本轮不启用 LIVE
stage-qdr-4 implementation 未启动
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
safety wording scan: REVIEWED / FALSE_POSITIVE_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

## 2026-07-08 DH-STAGE-QDR-3-FINAL-CLOSE-DOCS-SYNC

将用户提供的 `DH-STAGE-QDR-3-B5-CLOSE-REVIEW` ACCEPTED 结论写回 current factsources，并把 stage-qdr-3 final close 收口为 `CLOSED / ACCEPTED`。

### Scope

```text
DOCUMENTATION_ONLY
STAGE_FINAL_CLOSE_RECORD
ACCEPTANCE_RESULT_SYNC
STAGE_QDR_3_CLOSE_RECORD
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
stage-qdr-3 implementation: DONE
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: READY
stage-qdr-4 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-4-PLAN
```

### Boundary

```text
本轮不修改 Java 生产代码
本轮不修改 Java 测试代码
本轮不新增 migration
本轮不修改 V1-V8 migration
本轮不新增 V9 migration
本轮不新增 API / Controller / REST endpoint
本轮不新增真实 HTTP outbound
本轮不新增真实 provider client
本轮不新增 Provider SDK
本轮不启动 Agent / LangGraph runtime
本轮不修改 NQ
本轮不启用 LIVE
stage-qdr-4 implementation 未启动
```

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

### Next

```text
DH-STAGE-QDR-4-PLAN
```

## 2026-07-07 DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE

执行 docs-only governance pre-close 收口。本轮目标是把 stage-qdr-3 B5 retry 前的当前事实源、归档索引、supporting docs 和 blocker 规则拆开，避免旧阶段文档继续阻断 close review。

### Scope

```text
DOCUMENTATION_ONLY
DOCS_GOVERNANCE
FACTSOURCE_CONSOLIDATION
ARCHIVE_CLEANUP
CURRENT_STATE_INDEXING
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/Project/decision-hub
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

## 2026-07-15 DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER

### Task classification

```text
WORK_ORDER_ONLY
CAPACITY_HARNESS_IMPLEMENTATION_DESIGN
ACCEPTANCE_RUNNER_CONTRACT
MAVEN_PROFILE_DESIGN
SCENARIO_DRIVER_DESIGN
EVIDENCE_ARTIFACT_SCHEMA
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
CURRENT_FACTSOURCE_SYNC
NO_CODE_CHANGE / NO_TEST_CHANGE / NO_MIGRATION_CHANGE
NO_CAPACITY_EXECUTION / NO_B3 / NO_EXTERNAL_HTTP / NO_PROVIDER / NO_NQ / NO_AGENT / NO_LIVE
```

### Completed

- 使用`nq-dh-workflow-router`完成DOCUMENTATION前置分类，并以`dh-docs-writer`执行docs-only治理。
- 核验root/module POM、`dh-app` qdr7 actual-wiring/Testcontainers tests、`scripts/verify.ps1`、CI Docker能力与现有artifact惯例。
- 选择Java/JUnit/Failsafe + PowerShell + Maven profile混合模式；没有新增production module或`src/main`容量组件。
- 冻结`qdr7-capacity-acceptance`唯一命令、Maven lifecycle、semantic exit code、run-id隔离、environment preflight、14类mandatory driver、连续指标、artifact/schema/manifest/secret scan、teardown与14项harness自测。
- 冻结下一implementation的三个POM、9个Java test infrastructure文件、1个PowerShell入口、4个config/schema文件和current docs精确write allowlist。
- 新增`DH_STAGE_QDR_7_B2_CAPACITY_HARNESS_IMPLEMENTATION_WORK_ORDER.md`并同步8个current factsources、ROADMAP、criteria/evidence/acceptance supporting docs。

### Validation

```text
scope containment: PASS
current factsources: PASS / 8 OF 8 / 0 CONFLICTS
mandatory contract markers: PASS / 40 OF 40
unexpected files: 0
forbidden-scope tracked diff: 0
forbidden-scope untracked diff: 0
git diff --check: PASS / EOL WARNINGS ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS / 19 OF 19
full Maven regression: NOT RERUN / ACCEPTED 1114-TEST FACT REUSED
capacity execution: NOT RUN
```

### Current state

```text
Stage-QDR-7 B2: CLOSED / ACCEPTED
Capacity acceptance criteria: FROZEN / ACCEPTED
Capacity harness work order: CLOSED / ACCEPTED
Capacity harness implementation: NOT_STARTED / NEXT
Post-B2 capacity acceptance: BLOCKED / PENDING HARNESS AND EXECUTION
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION
```

本任务未实现或执行harness，未修改Java、测试、POM、脚本、application、migration/callback、API/contracts或NQ，未接外部HTTP/provider/Agent/LangGraph，未开启Paper/LIVE。Local commit仅在最终diff、scope与staged复核通过后创建；禁止push与tag。

## 2026-07-11 DH-STAGE-QDR-6-B1-EVIDENCE-CORRELATION-AGGREGATE-CONTRACTS

- 完成 `DecisionEvidenceQuery`、`DecisionEvidenceCorrelation`、`DecisionEvidenceAggregate`、`DecisionEvidenceRef`、`DecisionEvidenceStatus`、`DecisionEvidenceFinding` 与 `DecisionEvidencePolicy` contracts。
- 复用 `DecisionEvidence`、`DecisionEvidenceView`、`DecisionReplayView`、`QdrPersistenceSafety` 与 `PromptModelSafetyRules`；四键 correlation、跨租户、冲突 ref 和不安全 ref 均 fail-closed。
- 新增 20 项纯单元测试，覆盖完整度、四键冲突、跨租户、重复不一致引用、敏感材料/交易词拒绝和禁止依赖 guard。
- 未修改 `dh-domain`，未新增 migration、API、Controller、Repository、SQL、JDBC 或 production aggregation service；未实现 deterministic replay，未调用 Provider、HTTP、NQ、Agent、LangGraph 或 LIVE。
- 验证：目标 B1 test、`mvn -ntp -pl dh-usecase -am test` 与 `mvn -ntp -Pquality validate` 均为 `BUILD SUCCESS`。
- 下一步：`DH-STAGE-QDR-6-B2-EVIDENCE-AGGREGATION-SERVICE`。

### Archive Actions

```text
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_WORK_ORDER.md
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_DISCIPLINE_CLOSEOUT.md
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-3/pre-close-current-snapshot-20260707/
```

上述文件均为 historical record，不是 current factsource。current docs 只保留 `README / STATUS / WORK_ORDER / CODEX_PROJECT_INSTRUCTIONS / TESTING / FACTSOURCE_POLICY / ARCHIVE_INDEX` 作为 B5 retry 前的治理入口。

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/gates/**
```

### Next

```text
DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

## 2026-07-08 DH-DOCS-GOVERNANCE-GATES-ARCHIVE-FIX

修正上一轮归档路径口径：项目既有阶段归档目录是 `docs/gates`，本轮不再引入 `docs/archive` 作为第二套 QDR 归档体系。

### Scope

```text
DOCUMENTATION_ONLY
DOCS_GOVERNANCE_CORRECTION
GATES_ARCHIVE_ALIGNMENT
CURRENT_DOCS_CLEANUP
FACTSOURCE_CONSOLIDATION
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Archive Actions

```text
docs/archive/stage-qdr-2/* -> docs/gates/stage-qdr-2/
docs/archive/stage-qdr-3/* -> docs/gates/stage-qdr-3/
docs/current historical residuals -> docs/gates/stage-qdr-3/current-docs-historical-20260708/
docs/archive empty directory cleanup: DONE
```

### Current Docs Retained

```text
README.md
STATUS.md
WORK_ORDER.md
CODEX_PROJECT_INSTRUCTIONS.md
TESTING.md
FACTSOURCE_POLICY.md
ARCHIVE_INDEX.md
WORKLOG.md
ROADMAP.md
API.md
DB_SCHEMA.md
```

### Current State

```text
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```
