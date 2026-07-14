# DH Stage-QDR-7 B2 Capacity Threshold Evidence

> task: `DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER`
> classification: `CODE_AND_EVIDENCE_FIX`
> baseline: `e2cb1ff966eb611f05d3703893fab002ea6142b4`
> run id: `20260714T154500Z`
> evidence root: `target/postgresql-same-pool-recovery/20260714T154500Z/`
> verdict: `CANDIDATE_THRESHOLD_EVIDENCE: SUFFICIENT`
> next task: `DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`

## Current same-pool recovery结论（2026-07-14）

RCA确认旧probe以`127.0.0.1::5432`请求Docker随机宿主端口，容器stop/start后mapped port可重新分配，而已启动应用仍持有旧JDBC URL；旧probe只检查容器内`pg_isready`，没有比较restart前后endpoint或执行direct JDBC，因此根因分类为`PROBE_CONTAINER_ENDPOINT_CHANGED / RECOVERY_PROBE_INVALID`。Persistent guard每次通过`JdbcTemplate`取得连接，没有缓存`Connection`或事务对象；production `application*.yml`也没有导致不可恢复的特殊Hikari参数。本任务未修改production Java或配置，只新增固定loopback endpoint的真实PostgreSQL回归probe。

run `20260714T154500Z`在同一Spring ApplicationContext、DataSource、Hikari pool、PostgreSQL container、mapped port、JDBC URL hash和persistent volume上完成3/3 same-container restart。每轮数据库ready为487–512 ms，首个合法protected 2xx恢复为3.792–3.864 s；旧60秒只作为previous observation window，不写成正式阈值。数据库不可用期间9个真实protected请求全部`5xx / UNKNOWN_ERROR`，false 2xx为0、未落idempotency记录、未提交事务未恢复为成功。已提交nonce/idempotency、tenant隔离和PromptVersion canonical row均保持。

连续序列共37 rows，三轮outage各9个采样点，最大采样间隔1048 ms；字段覆盖container state、`pg_isready`、direct JDBC、HTTP status/error/latency、Hikari active/idle/pending/total/timeout/creation failure/acquire、PostgreSQL sessions与application state。恢复后8并发/100请求全部2xx，0个4xx或unexpected 5xx；Spring Context restart和PostgreSQL same-container persistent-volume restart分别3/3。定向recovery/persistent-guard/actual-wiring集合12项通过，累计相关回归通过；完整`mvn -ntp test`为166 suites、1114 tests、0 failures/errors/skipped，Surefire采样380 rows/7 PIDs，质量门通过。

```text
ROOT_CAUSE: PROBE_CONTAINER_ENDPOINT_CHANGED
PREVIOUS_PROBE: RECOVERY_PROBE_INVALID
PRODUCTION_CONFIGURATION_CHANGE: NONE
PRODUCTION_CODE_CHANGE: NONE_FOR_RECOVERY
POSTGRESQL_SAME_POOL_RECOVERY: CLOSED / ACCEPTED / 3 OF 3
OUTAGE_FAIL_CLOSED: PASS / 9 REQUESTS / FALSE_2XX 0
HIKARI_POSTGRESQL_SERIES: PASS / 37 ROWS / MAX_GAP_MS 1048
POST_RECOVERY_CONCURRENCY: PASS / 8 CONCURRENT / 100 OF 100 2XX
SPRING_CONTEXT_RESTART: PASS / 3 OF 3
POSTGRESQL_SAME_CONTAINER_RESTART: PASS / 3 OF 3
PERSISTENT_STATE_RECOVERY: PASS
FULL_REGRESSION: PASS / 1114 / 0 / 0 / 0
FULL_REGRESSION_RESOURCE_BASELINE: PASS / 380 SUREFIRE ROWS / 7 PIDS
QUALITY_GATE: PASS
CANDIDATE_THRESHOLD_EVIDENCE: SUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: YES / NEXT_TASK_ONLY
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
```

上一轮run `20260714T210052`的15/15 rate matrix、cold-start quota 3/3和cleanup 10/100/1000证据继续作为consumed evidence复用；本恢复任务未修改PromptVersion repository、rate limiter/rate JDBC adapter、transaction boundary、security chain或cleanup实现。`SUFFICIENT`只表示进入criteria freeze retry的候选证据完整，不等于accepted threshold、production SLO、formal harness或capacity acceptance PASS。

---

## Previous scenario evidence retry结论（historical / consumed，2026-07-14）

本轮按明确write allowlist修复两项生产阻断：`JdbcPromptVersionRepository`改为PostgreSQL原子`INSERT ... ON CONFLICT DO NOTHING -> SELECT canonical row -> stable identity/semantic content compare`，不再使用`SELECT -> INSERT -> DuplicateKeyException -> same-transaction SELECT`；persistent guard cleanup改为明确tenant/environment scope，null、blank和`*` tenant均fail-closed，SQL candidate selection直接包含`tenant_id = ?`。未修改migration、Flyway callback、API/contracts、datasource/global config、NQ、Provider、Agent/LangGraph或LIVE。

新run取得13/13 actual-wiring preflight、15/15 measured rate rounds、3/3 cold-start quota与10/100/1000 cleanup calibration PASS；完整回归、真实PostgreSQL/Testcontainers suite、Surefire资源采样和质量门也通过。PostgreSQL临时不可用期间持续fail-closed且没有内存fallback；但数据库恢复后，同一ApplicationContext与同一Hikari pool在规定60秒deadline内没有返回合法protected 2xx。临时脚本错误地把上限实现为60次请求而不是elapsed deadline，deadline之后的观测不得计为PASS；restart下游证据因此未完成。

```text
PROMPT_VERSION_ATOMIC_BOOTSTRAP: CLOSED / ACCEPTED
CLEANUP_TENANT_SCOPED_CONTRACT: CLOSED / ACCEPTED
ACTUAL_WIRING_PROTECTED_2XX: PASS / 13 OF 13
RATE_MATRIX: PASS / 15 OF 15
QUOTA_ATOMICITY: PASS / COLD_START 3 OF 3
CLEANUP_CAPACITY_EVIDENCE: PASS / 10 + 100 + 1000
POSTGRESQL_UNAVAILABLE_FAIL_CLOSED: PASS
POSTGRESQL_SAME_POOL_RECOVERY: BLOCKED / NO PROTECTED 2XX WITHIN 60 SECONDS
HIKARI_POSTGRESQL_SERIES: FAIL / RECOVERY SERIES NOT ACCEPTABLE
RESTART_PERSISTENCE_EVIDENCE: NOT_COMPLETED / BLOCKED_BY_SAME_POOL_RECOVERY
FULL_REGRESSION: PASS / 1110 / 0 / 0 / 0
FULL_REGRESSION_RESOURCE_BASELINE: PASS
QUALITY_GATE: PASS
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
```

### Current run结果摘要

| Evidence | Result |
|---|---|
| actual-wiring preflight | 5 sequential + 8 concurrent，13/13 structured 2xx，0 unexpected 5xx |
| rate matrix | concurrency `1 / 2 / 4 / 8 / 16`，每点3轮、warm-up 20、measured 100；15/15 PASS |
| throughput | observed min/median/max `6.262 / 26.568 / 61.165 req/s` |
| latency | aggregate p50/p95/p99 `152.334 / 305.034 / 413.197 ms`；sample variance `4913.641748` |
| cold-start quota | 3/3；每轮10 accepted、30 `429 / RATE_LIMITED`、0 unexpected 5xx、10 DB winners、oversell 0；无warm-up或bucket reset |
| cleanup | 10/100/1000；2 workers、1 writer、batch 10；duration `115 / 110 / 718 ms`；85 timeline rows |
| cleanup safety | cross-tenant/environment、active、locked、not-expired与duplicate deletion全部为0 |
| unavailable | protected requests fail-closed；日志包含`500/503`与`CannotGetJdbcConnectionException`；未观察内存fallback或partial success |
| same-pool recovery | FAIL；规定60秒内无合法protected 2xx；脚本越过deadline的观测不接受 |
| recovery series | unavailable 20 rows、recovery polling 91 rows；末尾Hikari active/idle/pending=`1/0/0`、timeout count=52；PostgreSQL sessions/active/waiting=`1/1/0` |
| restart | 本run未完成；被same-pool recovery blocker阻断，不复用旧run的restart PASS |
| full regression | 19/19 reactor SUCCESS；1110 tests，0 failures/errors/skipped；fresh PostgreSQL 8 suites/60 tests/0 skipped |
| regression resources | host 188 samples、Maven JVM 186 rows、Surefire 185 rows/5 PIDs、Docker 530 rows；原始command line未持久化 |
| quality | `mvn -ntp -Pquality validate` exit 0，19/19 SUCCESS |

### Current最小剩余阻断集合

1. `POSTGRESQL_SAME_POOL_RECOVERY_BLOCKED`：同一ApplicationContext/同一Hikari pool在规定60秒内未取得合法protected 2xx。
2. Hikari/PostgreSQL recovery series因临时脚本deadline实现错误不能形成accepted recovery evidence。
3. 本run的ApplicationContext与persistent-volume restart 3+3下游证据未执行完成。

以上三项共享同一根阻断。不得通过重启ApplicationContext、修改datasource/global config、晚于deadline的恢复或旧run restart证据绕过。下一formal task ID未获授权，不在current facts中发明。

### Current artifacts与工具RCA

Evidence root为`target/capacity-scenario-evidence/20260714T210052/`，核心文件包括`blocked-evidence-summary.json`、`actual-wiring-preflight.json`、`rate-matrix.csv`、`rate-round-summary.json`、`quota-atomicity-retry.csv`、`cleanup-summary.json`、`cleanup-timeline.csv`、`hikari-series.csv`、`postgres-contention.csv`、`full-regression-summary.json`、`full-regression-jvm.csv`与`quality-gate.log`。该目录被Git忽略，不stage、不commit。

临时HTTP parser曾把缺少`decisionId`的非2xx响应错记为status 0；cleanup driver曾沿用旧构造参数顺序；full-regression sampler曾依赖短生命周期Windows parent process及runtime-specific SHA-256 API。三项均只在`target`临时工具内最小修正，失败attempt/probe证据保留，正式full regression资源summary为PASS。

---

## Previous threshold evidence retry-2（historical / preserved）

### Retry-2结论（2026-07-14）

本轮从新run-id执行了actual-wiring preflight、完整rate matrix、quota/隔离/nonce/idempotency、production cleanup、PostgreSQL/Hikari contention、两类restart、完整Maven回归资源采样、质量门、manifest和secret scan。所有HTTP仅访问`127.0.0.1`，数据库为专用PostgreSQL 17.10容器与持久volume，临时凭证只存在于进程内存。

Rate matrix完成15/15轮，取得localhost候选观测；但mandatory correctness、scenario和资源证据并未全部通过。不得把下列数值称为accepted threshold、production SLO、production capacity、production certification、capacity acceptance PASS或B3 readiness。

```text
THRESHOLD_EVIDENCE_COLLECTION: BLOCKED
ACTUAL_WIRING_PROTECTED_2XX: PASS
RATE_MATRIX: PASS / 15 OF 15
RATE_LIMIT_CORRECTNESS: FAIL / COLD_START_END_TO_END
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
FULL_REGRESSION_RESOURCE_BASELINE: FAIL / SUREFIRE_PROCESS_SAMPLES_0
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
```

### Current retry-2结果摘要

| Evidence | Result |
|---|---|
| actual-wiring preflight | 5/5 sequential + 8/8 concurrent structured 2xx；0 unexpected 5xx、0 `UNKNOWN_ERROR`、0 provider-profile mismatch |
| rate matrix | concurrency `1 / 2 / 4 / 8 / 16`，每点3轮、每轮warm-up 20 + measured 100；15/15完成，1500 measured requests |
| throughput | observed min/median/max `8.582 / 32.775 / 92.667 req/s` |
| latency | p50 `107.671 ms`；p95 `212.887 ms`；p99 `275.611 ms`；max `6532.981 ms`；sample variance `110274.699992` |
| quota cold-start | 三轮均为3个2xx、30个429、7个503；DB accepted=10、oversell=0；end-to-end FAIL |
| quota warmed retry | 三轮均为10个2xx、30个`429 / RATE_LIMITED`、0 unexpected error；只证明warmed path |
| tenant/environment isolation | PASS；A saturation不消费B quota；noncanonical source为`403 / SOURCE_DENIED` |
| same-nonce race | 3/3 PASS；每轮1 accepted、23 `NONCE_REPLAY`、1 DB winner、0 unexpected error |
| idempotency lifecycle | 3轮、75 tests、0 failures/errors/skipped；runtime lifecycle与Flyway migration/schema evidence分开 |
| cleanup | production `JdbcGuardCleanupAdapter`完成10/100/1000规模、96 timeline rows、2 workers、1 writer、batch 10；duration为59/51/410 ms |
| cleanup safety | duplicate=0、cross-environment=0；但other-tenant eligible deletion为1/10/100，违反本任务`cross-tenant deletion = 0`要求 |
| contention | c=8和16 normal/lock-pressure路径按预期fail-closed；Hikari pending max=13，lock waits max=4；DB临时不可用后同一pool未在30次bounded retry内恢复 |
| resource series | Hikari 78、PostgreSQL 77、JVM 78、Docker 77 rows；unavailable/recovery段因sampler StrictMode错误不完整 |
| restart | Spring ApplicationContext 3/3 PASS；PostgreSQL persistent-volume restart 3/3 PASS |
| full regression | `mvn -ntp test`：19/19 Reactor SUCCESS，1101/0/0/0，PostgreSQL/Testcontainers实际执行，无native-memory OOM |
| regression resources | host 102 samples，Maven JVM 100 rows，Docker 168 rows；Surefire JVM 0 rows，因此resource baseline FAIL |
| quality | `mvn -ntp -Pquality validate`：19/19 SUCCESS，Checkstyle 0 violations，Spotless PASS |
| evidence integrity | manifest 107 entries、0 mismatch；secret scan 106 files/8 patterns/0 findings |

Quota数据库winner count严格为10且oversell为0，因此`RATE_BUCKET_ATOMICITY: PASS`；但用户要求的actual-wiring end-to-end结果必须为10 accepted/30 rejected/0 unexpected，本轮每轮有7个503，因此`QUOTA_ATOMICITY: FAIL`。RCA指向fresh tenant首次并发时JDBC baseline check-then-insert竞态导致loser transaction进入PostgreSQL aborted state，并非`e2cb1ff`已修复的createdAt mismatch复发。

Cleanup生产合同实际按`environment + endpoint + source`清理，`GuardCleanupCommand`没有tenant字段。生产adapter按现有合同执行成功，不代表满足本任务额外要求的tenant-specific deletion safety；两种结论不得合并。

### Current retry-2最小阻断集合

1. Cold-start quota actual-wiring三轮各出现7个unexpected 503。
2. Production cleanup scope不含tenant，无法满足本任务`cross-tenant deletion = 0`。
3. PostgreSQL临时不可用后同一Hikari pool未恢复，committed replay在恢复尝试后仍为`500 / UNKNOWN_ERROR`。
4. Runtime sampler在DB unavailable阶段因StrictMode空结果处理失败，unavailable/recovery time series不完整。
5. Full regression resource sampler未捕获Surefire JVM进程，resource baseline mandatory field缺失。

### Current retry-2测量限制

- localhost单机样本不代表production部署、网络、数据库拓扑或负载分布。
- 最大延迟受本机JVM、Docker与host contention影响，不可直接外推。
- Cleanup workload只校准当前production contract，并暴露tenant-scope要求与实现合同不一致。
- Idempotency round 1/2的方法级结论来自完整零失败suite log加exact-HEAD静态test inventory；只有round 3保留direct XML。
- 单一canonical source使`CROSS_SOURCE_CAPACITY`为`NOT_APPLICABLE_UNDER_SINGLE_CANONICAL_SOURCE`。

### Current retry-2 artifacts

Evidence root为`target/capacity-threshold-evidence/20260714T012507/`。核心文件包括`environment.json`、`commands.txt`、`actual-wiring-preflight.json`、`rate-matrix.csv`、`rate-round-summary.json`、`quota-atomicity.csv`、`tenant-isolation.csv`、`nonce-race.csv`、`idempotency-lifecycle.json`、`cleanup-timeline.csv`、`cleanup-summary.json`、`postgres-contention.csv`、`hikari-series.csv`、`jvm-series.csv`、`docker-series.csv`、`restart-results.json`、`full-regression.log`、`scenario-summary.json`和`sha256-manifest.txt`。该目录被Git忽略，不stage、不commit。

---

## Previous attempt（historical / preserved）

以下内容原样保留`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY`的首个`200`、第二请求`500 / UNKNOWN_ERROR`、provider-profile mismatch根因、0 completed rate rounds和当时的next task；不得覆盖上方retry-2 current disposition。

> task: `DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY`
> classification: `CALIBRATION_EVIDENCE_COLLECTION`
> baseline: `962b348761e4837f7aa433f45ba567201d8e67d1`
> run id: `20260713T213431`
> evidence root: `target/capacity-threshold-evidence/20260713T213431/`
> verdict: `CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT`
> next task: `DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER`

## 1. 结论

本轮在exact-HEAD Spring Boot jar、localhost、专用PostgreSQL 17.10容器和临时凭证下取得了1个经过完整安全链的`200`，并完成same-nonce并发竞争、canonical source fail-closed、Spring Context/PostgreSQL持久化重启、数据库不可用恢复、真实PostgreSQL correctness回归、完整Maven回归及资源采样。

但是同一Spring Context内第二个使用新nonce、新requestId的合法请求返回`500 / UNKNOWN_ERROR`，因此20次warm-up只完成1次，`1 / 2 / 4 / 8 / 16`并发、每点3轮、每轮100次measured attempts的rate calibration没有任何有效测量轮次。Cleanup证据也缺少并发writer、duration与持续backlog timeline；PostgreSQL contention证据缺少持续lock-wait与connection-acquire序列。不得用correctness fixture、拒绝路径延迟、单次2xx或hard ceiling推导容量阈值。

```text
THRESHOLD_EVIDENCE_COLLECTION: BLOCKED
ACTUAL_WIRING_PROTECTED_2XX: PASS / SINGLE_SAMPLE
ACTUAL_WIRING_REPEATABLE_2XX_CALIBRATION: FAIL
ACTUAL_WIRING_2XX_CALIBRATION_UNAVAILABLE_BLOCKED
RATE_LIMIT_CALIBRATION: FAIL / WARM_UP_PREREQUISITE
CLEANUP_BACKLOG_EVIDENCE: FAIL / MANDATORY_FIELDS_MISSING
POSTGRESQL_CONTENTION_EVIDENCE: FAIL / INCOMPLETE
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
Stage-QDR-7 B3: NOT_ALLOWED
```

本文件不接受任何threshold，不定义production SLO，不声明production capacity，也不形成B3 readiness。

## 2. Scope与基线

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TASK_SCOPE_DESIGN_INVALID: NO
UNEXPECTED_TECHNICAL_SCOPE_DIRTY_BLOCKED: NO
```

| Item | Result |
|---|---|
| repository | `E:/Project/decision-hub` |
| branch | `dev` |
| baseline HEAD | `962b348761e4837f7aa433f45ba567201d8e67d1` |
| baseline subject | `fix(qdr): enforce persistent guard hard ceilings` |
| accepted B2 commit | `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c` |
| hard-ceiling fix commit | `962b348761e4837f7aa433f45ba567201d8e67d1` |
| worktree before task | clean |
| staged before task | empty |
| technical diff before task | Java、测试、callback、V1–V14、API/OpenAPI/contracts均为0 |
| Stage-QDR-7 B1 | `FROZEN` |
| Stage-QDR-7 B2 | `CLOSED / ACCEPTED` |
| guard hard-ceiling contract | `CLOSED / ACCEPTED` |
| capacity criteria | `BLOCKED / THRESHOLD_EVIDENCE_REQUIRED` |
| formal capacity harness | `NOT_IMPLEMENTED` |
| previous capacity evidence | `0 valid protected 2xx` |
| previous full regression | `1091 / 0 / 0 / 0` |
| Stage-QDR-7 B3 | `NOT_ALLOWED` |

基线命令：

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git log -1 --oneline
git diff --check
git diff --name-only
git diff --cached --name-only
```

## 3. 环境与隔离指纹

| Item | Value |
|---|---|
| captured at | `2026-07-13T14:24:41Z` |
| OS | Windows 11 Pro `10.0.26200` / build `26200` |
| CPU | AMD Ryzen 9 9950X / 16 physical cores / 32 logical processors |
| host memory | `50,524,966,912` bytes total |
| Java | Oracle HotSpot `21.0.9+7-LTS-338` |
| Maven | Apache Maven `3.9.12` |
| `MAVEN_OPTS` | unset |
| Surefire | `3.2.5`; fork/reuseForks使用Maven默认值；parallel未配置 |
| Docker | client/server `29.6.1`; Linux/amd64; 32 CPUs; `24,694,091,776` bytes memory; overlayfs |
| PostgreSQL | `postgres:17`; PostgreSQL `17.10 (Debian 17.10-1.pgdg13+1)` |
| dedicated container | `dh-qdr7-capacity-20260713T213431` |
| dedicated persistent volume | `dh-qdr7-capacity-20260713T213431` |
| dedicated database | `decision_hub_qdr7_capacity` |
| application bind | `127.0.0.1:18137` |
| Spring profile | `test` |
| canonical source | `NQ_DRYRUN` |
| external HTTP | `false` |

临时API token、HMAC secret、数据库密码、nonce、requestId与signature均在内存中生成，未打印、未写入证据文件。所有HTTP目标仅为`127.0.0.1`。未使用生产凭证、生产数据库、外部provider或NQ runtime。

## 4. 实际Spring wiring与安全链

### 4.1 入口与装配

实际入口为：

```text
POST /api/ai/decision-dry-runs
```

运行产物：

```text
dh-app/target/dh-app-1.0.0-SNAPSHOT.jar
SHA-256: c0bc9e6bcbf70bea055770ba1a56bbb892e1b5a35a984925e402cb9b418a91a6
launcher: java -jar
```

从当前代码确认的实际链路：

1. `DecisionDryRunController`在DTO解析前执行payload size gate。
2. `NormalizedNqDhHeaders`与header validator执行tenant/request/trace绑定。
3. `HmacNqDryRunAuthenticator`按`method/path/source/tenant/requestId/traceId/timestamp/nonce/schemaVersion/bodySha256`验签，并校验timestamp、唯一canonical source与tenant/source pair。
4. JDBC `NonceReplayGuard`先执行nonce fail-closed admission。
5. `PersistentDecisionDryRunRateLimiter`通过`JdbcRateLimitAdmissionAdapter`消费persistent rate状态。
6. `PersistentGuardedDecisionDryRunService`通过`JdbcIdempotencyGuardAdapter`和本地PostgreSQL transaction boundary执行idempotency lifecycle。
7. `DefaultDecisionDryRunService`进入既有Controller/service/orchestrator/mock-only wiring；没有绕过Controller或安全链。

### 4.2 Protected 2xx与repeatability blocker

| Attempt | HTTP | Error | Latency | Calibration eligibility |
|---|---:|---|---:|---|
| repeatability probe 1 | `200` | none | `373.734 ms` | 单一protected 2xx；不能形成warm-up |
| repeatability probe 2 | `500` | `UNKNOWN_ERROR` | `153.260 ms` | 排除；warm-up在第2次请求前阻断 |

两个请求使用相同Spring Context但不同nonce和requestId。第一个请求证明实际Boot jar、Controller、payload gate、source allowlist、HMAC、timestamp、nonce、tenant binding、persistent rate、persistent idempotency与service wiring可以形成protected 2xx；第二个请求证明该路径不可重复用于calibration。

当前代码现实解释该fail-closed结果：

- `DecisionPipelineWiringConfig`提供singleton `InMemoryProviderProfileRegistry`。
- `DefaultQdrMockModelGatewayBaseline.prepare()`每次以稳定provider profile ID和新的`clock.instant()`创建`ProviderProfile`。
- `InMemoryProviderProfileRegistry.register()`对相同ID但不同record内容抛出`provider profile bootstrap mismatch`。
- 上层将该未分类异常映射为`500 / UNKNOWN_ERROR`。

本任务禁止修改Java生产代码，因此只记录路径blocker，不实施修复。

## 5. Rate-limit calibration

冻结的采样协议为：

```text
concurrency: 1 / 2 / 4 / 8 / 16
warm-up: 20 requests
measured attempts: 100 per round
rounds: 3 per concurrency point
```

实际结果：

| Metric | Result |
|---|---|
| planned matrix rows | `15` |
| warm-up completed per row | `1 / 20` |
| measured attempts executed | `0` |
| completed rounds | `0` |
| requested / accepted / rejected | `0 / 0 / 0` measured attempts |
| unexpected errors in measured rounds | `0`；但warm-up第2请求为`500 / UNKNOWN_ERROR` |
| throughput | `NOT_AVAILABLE` |
| p50 / p95 / p99 / max | `NOT_AVAILABLE` |
| sample variance | `NOT_AVAILABLE` |
| successful concurrency points | none |
| failed concurrency points | `1`仅表示warm-up prerequisite失败，不是容量saturation point |
| not executed concurrency points | `2 / 4 / 8 / 16` |

PostgreSQL correctness suite在test-defined负载下记录：8线程、40 attempts、quota 10，严格10 accepted / 30 rejected，tenant与environment隔离通过，未出现oversell。该结果只证明原子正确性，不是rate capacity threshold。

## 6. Tenant/environment与canonical source

| Check | Result | Evidence class |
|---|---|---|
| tenant隔离 | PASS；一个tenant达到test quota不消费另一个tenant额度 | PostgreSQL correctness only |
| environment隔离 | PASS；相同tenant在不同environment scope独立 | PostgreSQL correctness only |
| cross-tenant query/update/cleanup | 未发现污染 | PostgreSQL correctness only |
| noncanonical source | `403 / SOURCE_DENIED` | actual localhost security chain |
| cross-source capacity | `NOT_APPLICABLE_UNDER_SINGLE_CANONICAL_SOURCE` | contract rule |

没有创建第二个伪造合法source。

## 7. Same-nonce concurrent race

实际localhost安全链使用同一nonce与requestId执行：

```text
threads: 8
attempts: 24
rounds: 1
accepted: 1 x 200
replay rejected: 23 x 409 NONCE_REPLAY
unexpected errors: 0
PostgreSQL final replay record count: 1
```

观测延迟为minimum `25.596 ms`、median `27.018 ms`、maximum `385.482 ms`、p50 `26.871 ms`、p95 `186.437 ms`、p99 `385.482 ms`、sample variance `8837.035`。这些数值包含replay拒绝路径，只能作为race correctness诊断，禁止提升为capacity threshold。

数据库不可用时，请求返回`500 / UNKNOWN_ERROR`，恢复后该attempt的nonce行数为0；不存在in-memory fallback放行。

## 8. Idempotency lifecycle

exact-HEAD PostgreSQL suite结果：`25 tests / 0 failures / 0 errors / 0 skipped`，Testcontainers `1.20.4`，PostgreSQL `17.10`。

| Scenario | Result |
|---|---|
| same key single winner | PASS；8线程/24 attempts/1 `ADMITTED` |
| active lease takeover | PASS；未错误接管 |
| expired lease behavior | PASS；符合冻结合同 |
| rollback | PASS；不留下成功状态 |
| commit-unknown | PASS；不readmit |
| hard-ceiling before JDBC | PASS；超限command不调用adapter |
| store unavailable | PASS；结构化fail-closed，无内存fallback |

这些结果属于lifecycle与transaction correctness，不是运行容量证据。Flyway callback没有被当作runtime lifecycle容量证据。

## 9. Cleanup backlog

实际`JdbcGuardCleanupAdapter`/production Bean correctness路径：

| Scenario | Initial | Eligible | Active | Locked | Ineligible | Writers | Workers | Batch | Cleaned | Retained | Timeline | Duration |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| rate bucket concurrent cleanup | 3 | 1 | 1 | 0 | 1 | 0 | 2 | 10 | 1 | 2 | `3 -> 2` | unavailable |
| idempotency concurrent cleanup | 5 | 3 | 1 | 0 | 1 | 0 | 2 | 10 | 3 | 2 | `5 -> 2` | unavailable |
| locked row / SKIP LOCKED | 1 | 1 | 0 | 1 | 0 | 0 | 1 | 10 | 0 | 1 | `1 locked -> 1 retained -> 0 after unlock` | unavailable |
| cleanup rollback | 1 | 1 | 0 | 0 | 0 | 0 | 1 | 10 | 0 | 1 | `1 -> 1 after rollback -> 0 on retry` | unavailable |

所有场景duplicate processing为0、protected rows incorrectly deleted为0。Mandatory capacity fields仍缺失：concurrent writer count为0、没有cleanup duration、没有持续backlog时间序列，也没有真实并发写入下的收敛曲线。因此：

```text
CLEANUP_BACKLOG_EVIDENCE: FAIL
CLEANUP_CALIBRATION_PATH: PRESENT / CORRECTNESS_ONLY
```

## 10. PostgreSQL contention、Hikari与恢复

### 10.1 Correctness与快照

- lock timeout rollback：PASS / correctness test。
- statement timeout rollback：PASS / correctness test。
- commit-unknown no-readmission：PASS / correctness test。
- database unavailable：`500 / UNKNOWN_ERROR`，恢复后nonce行数0。
- recovery health：`UP`。
- 四个PostgreSQL快照均为`lockWaitSessions=0`、`ungrantedLocks=0`、`deadlocks=0`；它们不能替代主动contention曲线。
- Flyway callback timeout值未被用作runtime容量阈值。

### 10.2 Hikari

same-nonce race后的单点快照：

```text
active: 1
idle: 6
pending: 0
maximum: 10
minimum: 2
```

该快照证明metrics可读取，但不是各concurrency point的时间序列，不能证明connection acquire wait或pool saturation。

### 10.3 Verdict

```text
POSTGRESQL_CONTENTION_EVIDENCE: FAIL / MANDATORY_SERIES_INCOMPLETE
NO_IN_MEMORY_FALLBACK: PASS
ORPHAN_OR_PARTIAL_COMMIT_OBSERVED: NO
STRUCTURED_FAIL_CLOSED: PASS
```

缺失项为持续lock-wait、statement timeout压力轮次、connection acquire wait、Hikari saturation与transaction rollback时间序列。

## 11. Restart persistence

| Restart type | Result |
|---|---|
| application object reconstruction | PASS / exact-HEAD PostgreSQL tests |
| Spring ApplicationContext restart | committed replay返回`409 / NONCE_REPLAY` |
| PostgreSQL container restart with persistent volume | committed replay返回`409 / NONCE_REPLAY` |
| committed replay rows after restart | `1` |
| uncommitted/database-unavailable attempt | 恢复后nonce rows `0`，未恢复为成功 |
| recovery state | health `UP`; PostgreSQL `pg_is_in_recovery() = false` |
| cross-tenant contamination | 未观察到 |

## 12. Full regression成功资源基线

命令：

```powershell
mvn -ntp test
```

结果：

```text
Reactor: 19 / 19 SUCCESS
tests: 1091
failures: 0
errors: 0
skipped: 0
Maven Total time: 06:01 min
resource sampler wall duration: 363 seconds
Surefire fork normal exit: true
native-memory OOM: false
Testcontainers actual execution: true
PostgreSQL 17.10 actual execution: true
external Maven download signal: false
```

资源摘要：

| Metric | Observed value |
|---|---|
| host samples | 66 |
| host CPU | min `97%`; median/p95/max `100%` |
| free memory start | `12,411,355,136` bytes |
| free memory minimum | `8,258,981,888` bytes |
| free memory median | `11,217,045,504` bytes |
| free memory end | `11,558,051,840` bytes |
| Maven JVM samples / max working set | `69` / `454,246,400` bytes |
| Maven JVM max private memory | `1,019,482,112` bytes |
| Surefire JVM samples / max working set | `62` / `935,165,952` bytes |
| Surefire JVM max private memory | `1,291,218,944` bytes |
| Docker full-regression samples | `117` |
| Docker max CPU | `64.42%` |
| Docker max memory | `0.35%` |

这是一台localhost测试主机上的成功回归资源基线，不是production capacity或最低生产资源要求。

质量门同轮执行：

```text
mvn -ntp -Pquality validate: 19 / 19 SUCCESS
Checkstyle: 0 violations
Spotless: PASS
```

## 13. Candidate threshold evidence

| Required report field | Evidence |
|---|---|
| observed minimum / median / maximum | rate calibration不可用；只有nonce race correctness诊断值，不可用于threshold |
| p50 / p95 / p99 | rate calibration不可用；nonce race拒绝路径值仅诊断 |
| sample variance | rate calibration不可用；nonce race为`8837.035`，仅diagnostic |
| successful concurrency points | none |
| failed concurrency points | concurrency 1 warm-up prerequisite失败；不是saturation point |
| not executed points | `2 / 4 / 8 / 16` |
| resource profile | full regression成功基线已记录；per-capacity-point资源曲线缺失 |
| throughput observations | none；0 valid measured rounds |
| latency observations | 1个protected 2xx与失败/拒绝路径；不得作为tail-latency threshold |

```text
CANDIDATE_THRESHOLD_EVIDENCE: INSUFFICIENT
ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: NO
```

最小阻断集合：

1. `ACTUAL_WIRING_2XX_CALIBRATION_UNAVAILABLE_BLOCKED`：同一Context第二个合法请求在warm-up前`500 / UNKNOWN_ERROR`。
2. `CLEANUP_BACKLOG_EVIDENCE`：缺并发writer、duration和持续backlog timeline。
3. `POSTGRESQL_CONTENTION_EVIDENCE`：缺持续lock-wait、connection-acquire与Hikari saturation序列。

## 14. Measurement limitations

- localhost单主机不代表production环境。
- 同一Context第二个合法请求失败，未形成rate matrix。
- 没有有效的1/2/4/8/16 measured rounds、throughput或tail latency。
- Cleanup workload没有并发writer、duration或持续backlog收敛数据。
- PostgreSQL没有持续contention与connection acquire wait曲线。
- Hikari只有post-scenario快照。
- Nonce race latency混合accepted与rejection path，不能升级为容量阈值。
- Full regression资源样本证明本机一次成功回归，不证明生产SLO、容量或最小资源。
- 单一canonical source使cross-source capacity不适用；不得伪造第二合法source。

## 15. Evidence manifest

Evidence root：`target/capacity-threshold-evidence/20260713T213431/`。该目录被Git忽略，不stage、不commit。

| File | SHA-256 |
|---|---|
| `environment.json` | `b84be61686a00eb3844702f78a1b42531b2b66a3250b87b247c7edfdf3148e4e` |
| `commands.txt` | `302ec82d242f9e21ca6d8f55fe33a09a309e3c449c9459cfd4d723c2f63c3504` |
| `http-samples.csv` | `d828e017eaaaf569bd2326e0e20bc9b152e41c81457c6ba1c8392ff523bda333` |
| `scenario-summary.json` | `f852304b436f8c54c43835cdb1cf274575400c1999474c5088f59fe5acf64ab7` |
| `rate-limit-samples.csv` | `57beaa181c00fa464cd7efbd3bb4baabb4ea70e38452d66d426d586c08ea8fa7` |
| `nonce-race-samples.csv` | `4167dc5f6e2ee87d5ba578506a78b06aad3ce579694ca1ee0fa0c89a1ada7702` |
| `cleanup-samples.csv` | `ad13502d3f979b31427bbf19c34a1a3ab50dd405ace8a54afe00b1a0b396f009` |
| `postgres-samples.csv` | `2910c451cf58778808dc564c808cdc8640f75d01bfc6f2821859c66a5a682259` |
| `hikari-samples.csv` | `4bcc794bed95ca7c3ce960e7ca3c61ffb88e14d58986b40f8b32fe2668c2fe3d` |
| `jvm-samples.csv` | `38ed7487ee5fd19ed1e514fb19bc4fcaf4b7845f3da1350ea7495b2a72d4db32` |
| `docker-stats.csv` | `d8d3fa129f0e769562699aed9f96d4df26277a9fb8e8a16e5697aecb6dea97a1` |
| `full-regression.log` | `88e844060f54939f0524677fec4dee1301750f1b32b561e5ef2c7ecf2bbb92b6` |
| `sha256-manifest.txt` | `d41da1ca38cedb60559547fedbc42ba2257865347d2f462d78e372728379a5cc` |

完整manifest含65个条目；secret scan为64 files、8 patterns、0 findings，未持久化match values。

## 16. Boundary与下一步

```text
Java production diff: 0
Java test diff: 0
callback diff: 0
V1-V14 diff: 0
API/Controller/DTO/OpenAPI/contracts diff: 0
NQ diff: 0
formal capacity harness implementation: NO
external HTTP: NO
real provider: NO
Agent/LangGraph runtime: NO
LIVE: NO
commit: NO / EVIDENCE_INSUFFICIENT
push: NO
tag: NO
```

下一任务只允许：

```text
DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER
```

在repeatable actual-wiring protected 2xx、完整rate matrix、cleanup并发写入/backlog/duration与PostgreSQL/Hikari contention序列可复现前，不允许重试capacity criteria freeze，不允许实现正式capacity harness，不允许执行post-B2 capacity acceptance，也不允许进入Stage-QDR-7 B3。
