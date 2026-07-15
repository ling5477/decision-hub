# DH Stage-QDR-7 B2 Capacity Harness Implementation Work Order

> task: `DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER`
> date: `2026-07-15`
> task type: `WORK_ORDER_ONLY`
> authority status: `CLOSED / ACCEPTED`
> B2 status: `CLOSED / ACCEPTED`
> capacity acceptance criteria: `FROZEN / ACCEPTED`
> capacity harness implementation: `NOT_STARTED / NEXT`
> post-B2 capacity acceptance: `BLOCKED / PENDING HARNESS AND EXECUTION`
> Stage-QDR-7 B3: `NOT_ALLOWED`

## 1. 目标、边界与验收

本工单只冻结formal capacity harness的实施合同，不实现harness、不修改代码或测试、不修改POM或脚本，也不执行容量矩阵、故障注入或post-B2 capacity acceptance。

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TASK_SCOPE_DESIGN_INVALID: NO
```

下一实施任务必须交付可重复的本地工程验收入口，但其完成只表示harness已实现和自测通过，不表示capacity acceptance `PASS`。任何capacity结果只能由后续独立任务`DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE`形成；B3在该任务完成前保持`NOT_ALLOWED`。

## 2. 仓库现实与唯一架构选择

### 2.1 已核验现实

- 仓库没有独立`acceptance-test`或`integration-test` module。
- root `pom.xml`只有`quality` profile；没有`maven-failsafe-plugin`、`exec-maven-plugin`或formal acceptance profile。
- `dh-app`已依赖Spring Boot Actuator、Micrometer Prometheus、PostgreSQL、Flyway、Hikari、JUnit和Testcontainers，且是Reactor最后一个module。
- actual-wiring、localhost protected HTTP、PostgreSQL、Hikari MXBean/Micrometer、Docker client、same-pool recovery与restart证据均已在`dh-app/src/test/java/com/guidinglight/decisionhub/qdr7`形成先例。
- `scripts/verify.ps1`是当前唯一版本化PowerShell验证入口；`target`下临时脚本不是正式实现位置。
- `.github/workflows/ci.yml`可用Docker/Testcontainers，并对关键Testcontainers测试执行`0 skipped`断言；当前CI尚无capacity profile入口。
- JSON/CSV evidence已有历史约定，但没有统一JSON Schema、artifact registry、manifest validator或secret-scan实现。

### 2.2 唯一推荐方案：混合模式

```text
Java + JUnit/Failsafe:
  actual Spring wiring、Testcontainers/PostgreSQL生命周期、场景驱动、数据库状态、
  correctness invariant、percentile与threshold比较、结构化artifact写入

PowerShell 5.1-compatible orchestration:
  environment preflight、run-id资源登记、Maven/Surefire/Docker/host连续采样、
  artifact/manifest/schema/secret校验、teardown兜底与最终summary编排

Maven profile:
  qdr7-capacity-acceptance统一入口、只在显式激活时绑定Failsafe与编排脚本、
  维持默认mvn test和mvn -Pquality validate行为不变
```

选择理由：真实场景已由`dh-app`的JUnit/Testcontainers actual-wiring承载，正确性继续留在Java最稳妥；进程树、host和Docker连续采样更适合PowerShell。该方案不新增production module、不把容量语义放入`src/main`，也不依赖`target`临时脚本。脚本必须兼容Windows PowerShell 5.1，并通过可覆盖的`qdr7.powershell.executable`为后续CI的`pwsh`保留入口。

拒绝方案A：纯Java难以稳定覆盖Windows host、Maven/Surefire process tree和Docker连续采样。拒绝方案B：纯PowerShell会重复实现正确性、数据库状态和threshold逻辑，并延续临时脚本脆弱性。

## 3. Maven profile与正式命令

### 3.1 唯一入口

```powershell
mvn -ntp -Pqdr7-capacity-acceptance -Dqdr7.runId=<UTC_yyyyMMddTHHmmssZ> -Dqdr7.seed=7 verify
```

调用者必须显式提供唯一、path-safe的UTC run-id；seed固定为整数`7`。禁止profile自动生成含本地时区或随机字符的run-id，禁止并行启动两个acceptance run。

### 3.2 生命周期绑定

| Phase | Owner | Required action |
|---|---|---|
| `validate` | root/`dh-bom` profile | 执行现有Checkstyle与Spotless合同；不修改默认profile |
| `pre-integration-test` | `dh-app` + Exec | preflight、run root、resource registry、sampler启动；失败分类为BLOCKED |
| `integration-test` | `dh-app` + Failsafe | 只发现`Qdr7CapacityAcceptanceIT`，执行全部mandatory scenarios |
| `post-integration-test` | `dh-app` + Exec | 停止sampler、teardown兜底、schema/manifest/secret scan、summary finalize |
| `verify` | Failsafe | 根据IT结果使Maven整体PASS或non-zero；禁止Testcontainers skip |

外层`verify`天然覆盖Reactor unit tests；finalizer必须扫描全部Surefire/Failsafe报告并证明19/19 Reactor模块均有可解释状态、0 failures/errors/skipped。不得再启动递归`mvn test`，避免双重Reactor与资源样本污染。

### 3.3 Maven退出现实与canonical exit code

Maven通常会把plugin/test子进程的任意失败归一为外层process non-zero，不能可靠保留子进程的`10/20/.../100`。因此合同分两层：

- `mvn ... verify`：`0`仅表示PASS；任意BLOCKED或FAIL必须non-zero。
- `capacity-acceptance-summary.json.exitCode`与`harness-exit-code.txt`：保存canonical semantic exit code，作为BLOCKED/FAIL精确分类；两处必须一致。

| Code | Final status | Meaning |
|---:|---|---|
| 0 | PASS | 全部mandatory scenario与threshold通过 |
| 10 | BLOCKED | environment preflight blocked |
| 20 | BLOCKED | actual-wiring或启动失败 |
| 30 | BLOCKED | mandatory scenario缺失或未发现 |
| 40 | FAIL | correctness invariant失败 |
| 50 | FAIL | numeric threshold失败 |
| 60 | FAIL | full regression失败或Testcontainers skipped |
| 70 | FAIL | quality gate失败 |
| 80 | BLOCKED | artifact、manifest或schema不完整/不一致 |
| 90 | FAIL | secret scan finding大于0 |
| 100 | BLOCKED | unexpected harness failure，无法形成有效判定 |

优先级固定为`90 > 80 > 70 > 60 > 40 > 50 > 30 > 20 > 10 > 0`；多项失败时summary保留全部findings，但`exitCode`取最高优先级。`BLOCKED`与`FAIL`不得仅凭Maven外层非零值推断，必须读取primary summary。

## 4. Run isolation与生命周期

每次执行只允许：

```text
host: 127.0.0.1 / localhost
tenant: qdr7-capacity-<run-id>
environment: QDR7_CAPACITY_<run-id>
source: NQ_DRYRUN
HMAC: 进程内临时值
PostgreSQL container: dh-qdr7-capacity-<run-id>
Docker volume: dh-qdr7-capacity-<run-id>
evidence root: target/qdr7-capacity-acceptance/<run-id>/
```

所有container、volume、PID、port、tenant、environment、temporary file和artifact必须进入`resource-registry.json`并绑定同一run-id。Teardown顺序固定为：停止采样器 -> 关闭Spring/Failsafe context -> rollback未提交事务 -> 停止并删除run-id container -> 删除run-id volume -> 删除临时凭证与stop marker -> 验证无run-id残留。失败保留只允许保留evidence，不允许保留credential；container/volume默认总是删除。禁止按prefix批量删除，禁止影响其他Docker资源，禁止`docker system prune`、`docker volume prune`或工作区reset。

## 5. Environment preflight合同

任何capacity scenario前必须生成`environment.json`与`preflight.json`，检查：

- Windows 11 x64；至少16 logical CPUs；启动时host available memory至少16 GiB。
- Docker Engine 29.x可用，allocation至少16 GiB；cached `postgres:17`可用，禁止隐式pull。
- Java 21、Maven 3.9.x、Testcontainers 1.20.4；`MAVEN_OPTS`必须unset。
- Hikari normal pool `10/2`、pressure pool `4/2`；HTTP最大并发16；单一Maven/harness进程。
- 指定loopback ports未占用；run-id container/volume/PID不存在。
- branch、HEAD、worktree、staged state全部记录；HEAD必须在整轮不变，dirty/staged默认BLOCKED。
- 读取`DH_STAGE_QDR_7_B2_CAPACITY_ACCEPTANCE_CRITERIA.md`冻结environment baseline并记录其SHA-256。

任一不满足时停止在preflight，写`ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED`、semantic exit `10`与BLOCKED summary；禁止降低并发、轮数、payload或threshold适配机器。

## 6. Mandatory scenario driver合同

公共时钟使用monotonic elapsed time；artifact timestamp统一UTC ISO-8601毫秒精度`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`。除表中特例外，单scenario timeout为15分钟，setup/teardown各2分钟；全部随机选择只从seed `7`派生并记录。

| Driver | Input / setup | Warm-up / measured | Correctness invariant | Numeric threshold | Artifact / failure |
|---|---|---|---|---|---|
| actual-wiring preflight | run scope、canonical source、临时HMAC、PostgreSQL 17、RANDOM_PORT loopback | 5 sequential + 8 concurrent protected requests | 13/13 structured 2xx；Spring/DataSource/Hikari identity存在 | 不参与rate阈值 | `actual-wiring.json`；启动失败20，错误响应40 |
| rate matrix | C=1/2/4/8/16 | 每点warm-up 20、measured 100、3 rounds | 每轮100个structured 2xx，unexpected 4xx/5xx/transport=0 | criteria §5每C的throughput/p50/p95/p99/max | `rate-matrix.csv`、`rate-summary.json`；40/50 |
| cold-start quota | window=3600、quota=10、C=8、attempts=40 | 无warm-up，3 rounds，新bucket | 10 accepted、30 `RATE_LIMITED`、DB winners=10、canonical row=1 | 精确计数，oversell=0 | `quota-atomicity.json`；40 |
| tenant/environment isolation | 至少A/B两个scope；A先耗尽quota | 无额外warm-up，3 rounds | B仍接受；cross-tenant/environment=0；noncanonical source为403 | 污染计数=0 | `tenant-isolation.json`；40 |
| same-nonce race | threads=8、attempts=24 | 无warm-up，3 rounds | 1 accepted、23 `NONCE_REPLAY`、DB winner=1 | unexpected=0 | `nonce-race.json`；40 |
| idempotency lifecycle | unique requestId/hash + active/expired/terminal fixtures | 6 lifecycle paths，每路径至少3轮 | single winner、lease不可偷、rollback无success、commit-unknown不readmit、terminal stable | partial/orphan/overwrite/readmission=0 | `idempotency-lifecycle.json`；40 |
| tenant-scoped cleanup | scale=10/100/1000，workers=2、writer=1、batch=10 | 无warm-up，每scale 1 measured run | protected/locked/not-expired/other-scope不删；backlog按合同收敛 | 200/200/1100 ms | `cleanup-timeline.csv`、`cleanup-summary.json`；40/50 |
| PostgreSQL/Hikari contention | normal->pressure->lock->statement timeout | 1 warm-up sequence + 6 measured samples | controlled rollback；deadlock/partial commit/unexpected 5xx=0 | pending<=17、acquire<=3800ms、wait<=7、lock-wait<=4 | `postgres-hikari-series.csv`；40/50 |
| same-pool recovery | 同一Context/DataSource/Hikari/container endpoint/JDBC URL/volume | 3 rounds；outage每轮至少3请求；recovery后C=8/100 | outage false 2xx=0；state/canonical row保持；post 100/100 2xx | DB<=800ms、Hikari<=5800ms、request<=5800ms、sampling gap<=1400ms | `recovery-timeline.csv`；40/50 |
| Spring Context restart | 同一run scope与persistent DB | 3 rounds | committed protected、uncommitted非success、isolation/canonical row/cleanup保持 | 3/3 PASS | `restart-results.json`；40 |
| PostgreSQL persistent-volume restart | same-container + run-id volume | 3 rounds | 与上一行相同 | 3/3 PASS | `restart-results.json`；40 |
| post-recovery concurrency | recovery后C=8、requests=100 | 3 rounds随recovery执行 | 100/100 2xx、4xx/5xx=0 | criteria §8 | `recovery-timeline.csv`、summary；40/50 |
| full Maven regression | 当前outer Reactor `verify`的Surefire/Failsafe报告 | 无warm-up | 19/19、0 failures/errors/skipped、Testcontainers executed | <=660s；Maven<=576MiB；Surefire<=2GiB；Docker<=192MiB；free memory>=13GiB | `full-regression.log`、summary；60 |
| quality gate | qdr profile复用现有Checkstyle/Spotless合同 | 无 | 两项均PASS | finding=0 | `quality-summary.json`；70 |

场景执行顺序固定为：preflight -> actual wiring -> rate -> quota -> isolation/source -> nonce -> idempotency -> cleanup -> normal/pressure/lock/timeout/outage/recovery -> two restart suites -> post-recovery load -> regression/report scan -> quality/report scan -> integrity/secret -> summary。场景缺失或顺序断裂为semantic exit `30`。

## 7. 连续指标采集

采样间隔固定1秒；outage/recovery阶段额外使用100毫秒Java内采样。时钟以`System.nanoTime()`/PowerShell `Stopwatch`计算elapsed，wall clock只用于UTC timestamp。

- HTTP：status、安全errorCode、latency、throughput、p50/p95/p99/max、transport error。
- Hikari：active、idle、pending、total、acquire latency、timeout counter。
- PostgreSQL：sessions、active/waiting、wait event、locks、deadlocks、statement timeout、connection failure；只保存聚合计数和安全分类。
- JVM：PID、CPU、heap used/committed/max、non-heap、thread count、working set；native memory若不可用写`null`和reason。
- Maven/Surefire：PID/parent PID、command hash、CPU、working set；禁止保存完整command line中的敏感参数。
- Docker/host：run-id container CPU/memory、host available memory、logical CPU。
- cleanup/recovery：eligible backlog、protected count、phase、elapsed、recovery milestone。

不得只使用单点Actuator快照；Actuator/Micrometer、MXBean、SQL聚合与OS采样必须以同一run-id/timestamp/elapsed字段关联。

## 8. Artifact与schema合同

### 8.1 公共字段

所有JSON必须包含`schemaVersion`=`qdr7-capacity-1`、`runId`、`commitSha`、`scenario`、`status`=`PASS|FAIL|BLOCKED|NOT_RUN`、`startedAtUtc`、`finishedAtUtc`、`durationMs`、`seed`、`unitSystem`、`missingValues`。CSV首列固定为`schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs`；缺失数值为空字段且在对应summary的`missingValues`说明，禁止使用0伪装缺失。时间统一毫秒，大小统一bytes，内存展示值可另带MiB/GiB派生字段。

### 8.2 必需文件与专属字段

| Artifact | Required content |
|---|---|
| `environment.json` | OS/arch/CPU/memory、Java/Maven/Docker/PostgreSQL/Testcontainers版本、Docker allocation、criteria hash |
| `preflight.json` | 每项check、expected、actual safe summary、status、blockerCode |
| `commands.txt` | 序号、UTC、cwd相对路径、脱敏命令、exit status；UTF-8 |
| `resource-registry.json` | run-id绑定的PID、port、container、volume、tenant/environment hash与teardown状态 |
| `actual-wiring.json` | Spring/DataSource/Hikari/container identity hash、13个request结果计数 |
| `rate-matrix.csv` | C、round、sample、status、latencyMs、started/completed monotonic offset |
| `rate-summary.json` | 每C/round的throughput、p50/p95/p99/max、structured2xx和error counts |
| `quota-atomicity.json` | round、attempt/accepted/rejected/DB winner/canonical row/oversell counts |
| `tenant-isolation.json` | scope hashes、A/B结果、cross-scope rows、noncanonical source result |
| `nonce-race.json` | round、attempt/accepted/replay/DB winner/unexpected counts |
| `idempotency-lifecycle.json` | lifecycle case、state transitions、winner、rollback/commit-unknown/terminal结果 |
| `cleanup-timeline.csv` | scale、phase、eligible/protected/locked/backlog/insert/delete counts |
| `cleanup-summary.json` | 每scale duration/outcome/threshold、最终收敛与保护计数 |
| `postgres-hikari-series.csv` | phase、Hikari四计数/acquire/timeout、PG session/wait/lock/deadlock聚合 |
| `recovery-timeline.csv` | round、phase、HTTP class、DB/Hikari/request ready milestone、sampling gap |
| `restart-results.json` | restart type/round、container/volume/context identity hash、invariant results |
| `jvm-series.csv` | PID/parent hash、CPU、heap/nonheap/native/workingSet/threadCount |
| `docker-series.csv` | container hash、CPU、memoryBytes、hostAvailableBytes |
| `full-regression.log` | report scan与Reactor模块结果的脱敏文本记录，不伪造未捕获console |
| `full-regression-summary.json` | module/test/failure/error/skip counts、duration、process coverage、resource maxima |
| `quality-summary.json` | Checkstyle/Spotless execution/status/finding counts |
| `threshold-comparison.json` | criterionId、source artifact/field、observed、operator、threshold、unit、status |
| `capacity-acceptance-summary.json` | finalStatus、exitCode、all scenario statuses、first blocker、all findings、artifact counts |
| `harness-exit-code.txt` | 单行十进制canonical semantic exit code |
| `sha256-manifest.txt` | 相对POSIX路径、两个空格、lowercase SHA-256；按ordinal path排序；不包含自身 |
| `secret-scan.json` | scannedFiles、patternsVersion、findingCount=0、只保存patternId/path/line，不保存match |

JSON Schema统一放入`config/qdr7-capacity/qdr7-capacity-artifacts.schema.json`，CSV header与必需artifact登记在`qdr7-capacity-artifact-registry.json`，threshold值来自`qdr7-capacity-thresholds.json`。任何run-id/commit不一致、JSON不可解析、CSV header不匹配、mandatory artifact缺失、scenario count不一致、threshold项缺失、manifest mismatch或secret finding都不得PASS。必需文件缺失固定为`BLOCKED / ARTIFACT_INCOMPLETE`。

## 9. Secret与日志治理

禁止artifact/log保存HMAC secret、完整signature、数据库密码、token/cookie、完整credential、raw provider response、raw prompt、production payload、未脱敏header或完整command line。允许字段仅为SHA-256、截断identifier、结构化errorCode、status、聚合计数与安全摘要。

secret scan必须覆盖case-insensitive credential key、Bearer、cookie、private key header、HMAC/signature raw value、JDBC password、常见token前缀以及临时测试secret的exact hash marker。finding只保存pattern ID和位置，不回显匹配内容。`commands.txt`与Maven/Surefire process采样必须先redact再落盘。

## 10. Harness自测矩阵

实施任务必须以短时、无真实capacity run的fixtures覆盖：

1. 参数解析：缺run-id、非法UTC/path、seed非7、重复run-id。
2. preflight：版本/CPU/memory/Docker/port/Git dirty/staged各BLOCKED路径。
3. threshold comparator：`< <= = >= >`、单位不匹配、缺字段、边界相等。
4. nearest-rank percentile：空集、1项、偶数、p50/p95/p99、overflow-safe duration。
5. JSON Schema与CSV header：合法、缺公共字段、错误unit、未知status。
6. manifest：稳定排序、内容变化、缺文件、self exclusion、path traversal。
7. secret scan：每个pattern positive fixture、redaction、false-positive allowlist。
8. semantic exit code：0/10/.../100与优先级、BLOCKED/FAIL/PASS分类。
9. 场景发现：mandatory count与固定顺序；缺场景返回30。
10. 部分场景失败：仍写summary并完成run-id teardown。
11. Testcontainers skipped：不得PASS，返回60并写明确finding。
12. run-id隔离：只允许删除registry中exact resource，不匹配资源保持不变。
13. sampler：PID退出、Docker暂不可读、native memory unavailable均写missing reason。
14. Maven profile isolation：未激活profile时不运行`*IT`和脚本；默认`mvn test`/quality合同不变。

## 11. Consolidated implementation batches

### H1 — Runner/Profile/Artifact基线

实现profile、run context、preflight、Failsafe发现、PowerShell lifecycle、schema/registry/threshold、manifest、secret scan、semantic exit code与对应自测。

### H2 — Mandatory场景与指标采集

实现actual-wiring、rate/quota/isolation/nonce/idempotency/cleanup/contention/recovery/restart/post-recovery驱动，以及HTTP/Hikari/PostgreSQL/JVM/Maven/Surefire/Docker/host连续采样。

### H3 — Threshold、完整回归与实施验收

实现threshold comparison、Surefire/Failsafe全量扫描、quality结果汇总、summary、teardown兜底、文档同步与边界scan。

H1-H3必须在同一个consolidated implementation任务连续完成，不创建普通review循环。只有migration/API-contract/security外部语义必须变化、冻结criteria无法实现或出现新P0/P1时中止并进入blocker任务。

## 12. 下一实施任务精确write allowlist

### Maven

```text
pom.xml
dh-bom/pom.xml
dh-app/pom.xml
```

### Java test infrastructure（不得进入`src/main`）

```text
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityHarnessContractTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityRunContext.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityScenarioDriver.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityArtifactContract.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityThresholdComparator.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityPercentiles.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityExitCode.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityTeardown.java
```

### PowerShell与schema

```text
scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1
config/qdr7-capacity/qdr7-capacity-artifacts.schema.json
config/qdr7-capacity/qdr7-capacity-artifact-registry.json
config/qdr7-capacity/qdr7-capacity-thresholds.json
config/qdr7-capacity/qdr7-capacity-secret-patterns.txt
```

### Current factsources与实施记录

```text
AGENTS.md
CLAUDE.md
README.md
docs/current/README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_7_B2_CAPACITY_ACCEPTANCE_CRITERIA.md
docs/current/DH_STAGE_QDR_7_B2_CAPACITY_THRESHOLD_EVIDENCE.md
docs/current/DH_STAGE_QDR_7_B2_POST_IMPLEMENTATION_CAPACITY_ACCEPTANCE.md
docs/current/DH_STAGE_QDR_7_B2_CAPACITY_HARNESS_IMPLEMENTATION_WORK_ORDER.md
```

上述为完整write allowlist，不含`.github/workflows/ci.yml`、production `src/main`、existing qdr7 tests、migration、callback、application配置、API/contracts或NQ。若实现必须修改未列文件，先停止并输出`TASK_SCOPE_DESIGN_INVALID`，不得自行扩表。

## 13. Implementation acceptance

```text
profile executable: REQUIRED
default Maven lifecycle unchanged: REQUIRED
harness self-tests: PASS
mandatory scenarios discoverable: ALL
all artifacts generatable: REQUIRED
semantic exit classification: PASS
outer Maven status: 0 only on PASS; non-zero otherwise
teardown: PASS / exact run-id only
target staged: 0
external HTTP/provider/NQ/Agent/LIVE: 0
full regression: PASS / 0 skipped
quality: PASS
current fact conflicts: 0
```

实施验收不得宣称`capacity acceptance PASS`。长时间mandatory capacity execution留给独立post-B2任务；实施任务只允许短时smoke/self-test证明runner、driver discovery、artifact和teardown合同可工作。

## 14. 回滚

- `pom.xml`、`dh-bom/pom.xml`、`dh-app/pom.xml`的profile/Failsafe/Exec变更可作为一个Maven slice单独`git revert`。
- 新增Java test infrastructure可作为一个test slice单独revert，不触及production。
- `scripts/qdr7-capacity`与`config/qdr7-capacity`可作为artifact tooling slice单独revert。
- current docs只回滚本任务顶部active块和追加记录，不重写历史。
- 不修改migration或production data；teardown只删除registry中exact run-id资源。
- 禁止`git reset --hard`、`git clean`、批量restore或全局Docker prune。

## 15. 冻结结论与下一步

```text
CAPACITY_HARNESS_IMPLEMENTATION_WORK_ORDER: DONE / ACCEPTED
HARNESS_ARCHITECTURE: FROZEN / HYBRID
MAVEN_PROFILE_CONTRACT: FROZEN
SCENARIO_DRIVER_CONTRACT: FROZEN
ARTIFACT_SCHEMA: FROZEN
EXIT_CODE_CONTRACT: FROZEN / SEMANTIC_CODE_PLUS_MAVEN_NONZERO
ENVIRONMENT_PREFLIGHT_CONTRACT: FROZEN
SECURITY_BOUNDARY: PASS
IMPLEMENTATION_WRITE_ALLOWLIST: PASS
HARNESS_TEST_MATRIX: PASS
Capacity harness implementation: NOT_STARTED / NEXT
Post-B2 capacity acceptance: BLOCKED / PENDING HARNESS AND EXECUTION
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER
current task status: CLOSED / ACCEPTED
next task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION
ALLOW_CAPACITY_HARNESS_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_CAPACITY_ACCEPTANCE_EXECUTION_NOW: NO
ALLOW_STAGE_QDR_7_B3_ENTRY: NO
```

本工单不授权外部HTTP、real Provider、NQ runtime integration、Agent/LangGraph、Paper或LIVE。
