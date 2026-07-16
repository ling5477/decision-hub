# DH Stage-QDR-7 B2 Post-Implementation Capacity Acceptance

## Current disposition（2026-07-16）

Capacity acceptance criteria保持`FROZEN / ACCEPTED`，B2保持`CLOSED / ACCEPTED`。历史formal runs `20260715T140521Z`与`20260715T160710Z`继续保留为`BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT`，不被本轮修复覆盖。本轮在exact baseline HEAD `0112d493c38beeec25b3407aa504dbc129e1850c`关闭container启动顺序与blocked artifact合同；implementation-validation run `20260716T133710Z`只证明Testcontainers/Spring wiring和finalizer可执行，不是formal capacity acceptance。当前验收结论为：

```text
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED / FORMAL RETRY REQUIRED
CAPACITY_ACCEPTANCE_CRITERIA: FROZEN / ACCEPTED
CAPACITY_HARNESS_WORK_ORDER: CLOSED / ACCEPTED
CAPACITY_HARNESS_IMPLEMENTATION: CLOSED / ACCEPTED
FORMAL_PROFILE: qdr7-capacity-acceptance
HISTORICAL_FORMAL_RUN_IDS: 20260715T140521Z + 20260715T160710Z
LATEST_FORMAL_RESULT: BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT / INTERNAL EXIT 80 / 0 OF 15
HARNESS_RUNTIME_BINDING: CLOSED / ACCEPTED
HARNESS_TESTCONTAINERS_LIFECYCLE: CLOSED / ACCEPTED
BLOCKED_ARTIFACT_CONTRACT: CLOSED / ACCEPTED
IMPLEMENTATION_VALIDATION_RUN_ID: 20260716T133710Z
IMPLEMENTATION_VALIDATION: PASS / IMPLEMENTATION_VALIDATION_ONLY / MAVEN EXIT 0
IMPLEMENTATION_VALIDATION_PREFLIGHT: PASS / 26 OF 26
IMPLEMENTATION_VALIDATION_WIRING: PASS / CONTAINER + APPLICATION_CONTEXT + HIKARI + FLYWAY + DISPATCHER
IMPLEMENTATION_VALIDATION_MANDATORY_SCENARIOS: 0 OF 15 / NOT_RUN
CAPACITY_ACCEPTANCE_EXECUTED: false
IMPLEMENTATION_VALIDATION_ARTIFACTS: PASS / REQUIRED SUMMARY FIELDS COMPLETE
IMPLEMENTATION_VALIDATION_THRESHOLD_COMPARISON: NOT_RUN / FORMAL_SCENARIO_NOT_EXECUTED
IMPLEMENTATION_VALIDATION_MANIFEST: PASS / 27 ENTRIES / 0 MISMATCH
IMPLEMENTATION_VALIDATION_SECRET_SCAN: PASS / 25 FILES / 0 FINDINGS
IMPLEMENTATION_VALIDATION_TEARDOWN: PASS / CURRENT RUN CONTAINER AND VOLUME ABSENT
B2_IMPLEMENTATION_STATUS: CLOSED / ACCEPTED
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE-RETRY-3
```

本轮选择JUnit/Testcontainers单一container ownership：static PostgreSQL在Spring属性解析前启动并冻结datasource endpoint，PowerShell不创建第二个同用途container。统一finalizer已覆盖preflight、Context/container startup与artifact failure路径。implementation-validation没有执行正确性、数值阈值、恢复、formal regression或formal quality scenario，因此不得写为capacity PASS；完整历史formal结论与本轮follow-up见`DH_STAGE_QDR_7_B2_FORMAL_CAPACITY_ACCEPTANCE_RESULT.md`。

## Historical formal retry-2 disposition（2026-07-15）

formal run `20260715T160710Z`在environment preflight 26/26 PASS后，于ApplicationContext属性解析阶段调用尚未启动container的mapped port而返回`BLOCKED / CAPACITY_HARNESS_RUNTIME_DEFECT / Maven exit 1 / internal exit 80 / 0 OF 15`。其11个artifact、19 findings、缺失10个required summary fields与缺失`threshold-comparison.json`均作为修复前证据保留，不改写为capacity失败或通过。

---

> task: `DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE`
> date: `2026-07-13`
> repository: `F:/project/decision-hub`
> branch: `dev`
> baseline SHA: `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c`
> final verdict: `BLOCKED / CAPACITY_ACCEPTANCE_CRITERIA_NOT_FROZEN`
> B2 implementation status: `CLOSED / ACCEPTED`
> B3 readiness: `NOT_ALLOWED`

## Current same-pool recovery disposition（2026-07-14）

`DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER`确认旧probe存在mapped-port漂移，并以固定loopback endpoint完成同一ApplicationContext/DataSource/Hikari pool恢复3/3、outage fail-closed、持久状态、37条连续序列、恢复后8并发/100请求、Spring Context restart 3/3和PostgreSQL same-container restart 3/3。完整回归1114/0/0/0、Surefire 380 rows/7 PIDs与质量门通过，candidate threshold evidence现为`SUFFICIENT`。

因此本报告原始`POST_B2_CAPACITY_ACCEPTANCE: BLOCKED`结论仍不变：criteria尚未在独立retry中冻结，formal harness也未获授权。B2 implementation仍`CLOSED / ACCEPTED`，criteria仍`BLOCKED / NOT_FROZEN`，但`ALLOW_CAPACITY_CRITERIA_FREEZE_RETRY: YES / NEXT_TASK_ONLY`；下一任务只允许`DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`，不得提前声明capacity acceptance PASS或进入B3。

## Previous threshold evidence retry-2 disposition（historical / consumed，2026-07-14）

`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2`完成了13/13 protected preflight、15/15 rate rounds、tenant/environment isolation、same-nonce race、idempotency lifecycle和两类3轮restart；完整`mvn -ntp test`为1101/0/0/0，质量门通过。与此同时，cold-start quota、cleanup tenant-scope safety、PostgreSQL/Hikari unavailable recovery series和Surefire资源采样未满足mandatory条件。

因此本报告原始`POST_B2_CAPACITY_ACCEPTANCE: BLOCKED`结论不变。该轮B2 implementation仍`CLOSED / ACCEPTED`，criteria仍`BLOCKED / NOT_FROZEN`；当时下一任务现已消费，不得覆盖顶部current disposition。

## Previous follow-up disposition（historical / consumed，2026-07-13）

后续criteria freeze仍为`CAPACITY_THRESHOLD_JUSTIFICATION_INSUFFICIENT_BLOCKED`；本报告原始`BLOCKED`结论、38项correctness证据、两次native-memory OOM和原始hash保持不变。上一轮threshold evidence retry仍记录首个actual-wiring protected `200`、同一Context第二请求`500 / UNKNOWN_ERROR`、rate matrix为0轮以及cleanup/contention mandatory evidence不完整。本轮只关闭repeatable 2xx路径阻断：同一Context与packaged jar各自完成5次顺序、8次并发结构化2xx，完整1101项回归通过；这仍不等于capacity acceptance。下一任务为`DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2`，不授权criteria freeze、harness work order/implementation、capacity execution或B3。

> Historical task record：从下一节开始是本验收任务当时的原始结论，其中`next task: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE`已经被本轮消费，不再是current next task。

## 1. 结论

```text
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
CAPACITY_ACCEPTANCE_CRITERIA_NOT_FROZEN_BLOCKED
CAPACITY_ACCEPTANCE_HARNESS_UNAVAILABLE_BLOCKED
Stage-QDR-7 B2: CLOSED / ACCEPTED
Stage-QDR-7 B3: NOT_ALLOWED
next task: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE
```

本轮不能形成容量PASS。现有current work order与B1 capacity blocker resolution冻结了配置类型、单位、合法范围、绝对hard ceiling、fail-closed语义和未来harness结构要求，但明确没有选择production/measured defaults，也没有冻结可执行的并发矩阵、warm-up、测量轮次、throughput/latency阈值或正式benchmark命令。仓库中存在真实PostgreSQL并发正确性测试，但不存在满足actual Spring wiring、2xx、Tomcat/Hikari/JVM/HTTP metrics完整要求的正式capacity/stress harness。

真实PostgreSQL 17.10/Testcontainers正确性回归共38项通过、0 failures、0 errors、0 skipped；这只能证明测试定义负载下的原子性、隔离、cleanup、commit-unknown与重启模拟，不得替代容量验收。两次完整`mvn -ntp test`均在`dh-app` Surefire fork因本机native memory不足中止，因此full regression也未满足PASS条件。B2既有acceptance不回退，但B3继续禁止。

## 2. Scope与预检

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TASK_SCOPE_DESIGN_INVALID: NO
UNEXPECTED_TECHNICAL_SCOPE_DIRTY_BLOCKED: NO
```

| Check | Result |
|---|---|
| branch | `dev` |
| task前HEAD | `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c` |
| accepted baseline | 与task前HEAD相同 |
| worktree | clean |
| staged | empty |
| `origin/dev...HEAD` | `0/0`；与任务预期`0/1`不同，但不影响技术基线且不作为容量结论 |
| technical diff | Java、测试、callback、V1–V14、API/OpenAPI/contracts均为空 |
| NQ scope | 未读取、未修改 |

## 3. Criteria来源审查

| Scenario / criterion | Source | 已冻结内容 | 缺失内容 | 判定 |
|---|---|---|---|---|
| 配置安全边界 | `DH_STAGE_QDR_7_B1_CAPACITY_BLOCKER_RESOLUTION.md` §3 | deadline、concurrency、queue、rate、lease、TTL、cleanup、retention的类型、范围与absolute hard ceiling | measured default、容量阈值 | 只能用于配置校验，不能用于容量PASS |
| measured defaults | 同文件 §4 | 全部标记`NOT_SELECTED / POST_B2_MEASUREMENT_REQUIRED` | 全部运行默认值 | 未冻结 |
| harness合同 | 同文件 §6；`DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md` §5.4 | actual wiring、canonical source/HMAC、loopback、isolated PostgreSQL、0 skipped与指标类别 | 固定seed/profile/payload corpus、warm-up、样本数、正式并发矩阵、命令、阈值 | 未冻结且无可执行harness |
| previous capacity evidence | `DH_STAGE_QDR_7_B1_RESOURCE_CAPACITY_EVIDENCE.md` §5–§6 | 旧诊断尝试过1/2/4/8/16并发、20 warm-up、100 measured attempts | 有效2xx样本、p95/p99、throughput、saturation point | 0个有效2xx样本，明确排除，不得复用 |
| persistent rate/idempotency correctness | `V12PersistentRuntimeGuardsFlywayPostgresTest` | 8线程下40次rate竞争严格接受10次；8线程下24次idempotency竞争只接受1次 | sustained duration、吞吐/延迟阈值、多轮测量 | correctness test only |
| cleanup correctness | `V12PersistentRuntimeGuardsFlywayPostgresTest`、`PersistentGuardProductionWiringPostgresTest` | 2 worker并发cleanup、SKIP LOCKED、active/locked/ineligible记录保留 | cleanup throughput、backlog、duration threshold、under-load轮次 | correctness test only |
| nonce persistence | `JdbcNonceReplayGuardPersistenceTest` | guard实例重建后相同nonce拒绝；tenant/source/request scope隔离；只清理过期行 | same-nonce concurrent race、吞吐/延迟阈值 | restart correctness only；并发race未覆盖 |
| callback / commit-unknown | `V13TransactionalCompatibilityCallbackFlywayPostgresTest`、`PersistentGuardProductionWiringPostgresTest` | lock/statement timeout rollback、retry、actual JDBC commit-unknown不readmit | callback claim/lease容量、pressure threshold与测量轮次 | correctness test only |

冻结criteria缺少用户要求的至少以下要素：

- 可执行的正式capacity/benchmark命令；
- 可追溯的正式并发矩阵与持续时间/iteration count；
- warm-up与measured rounds；
- throughput或latency acceptance threshold；
- 完整scenario到阈值的映射；
- metrics-complete actual-wiring 2xx harness。

因此不得从absolute hard ceiling、旧的0-success diagnostic matrix或单元/集成测试常量推导临时容量阈值。

## 4. 环境指纹

| Item | Value |
|---|---|
| OS | Microsoft Windows 11 Pro `10.0.26200`，build `26200` |
| CPU | 12th Gen Intel(R) Core(TM) i7-12700H；14 physical cores；20 logical processors |
| memory | 15.70 GiB total；采样free memory约0.89 GiB；完整回归前后曾观测约0.77–1.96 GiB |
| Java | Oracle Java `21.0.8`；HotSpot 64-Bit Server VM `21.0.8+12-LTS-250` |
| JVM | default Maven/Surefire参数；未为通过验收临时修改heap或fork参数 |
| Maven | Apache Maven `3.9.12` |
| Docker | client/server `29.6.1`；Docker Desktop Linux/amd64；20 CPUs；8,165,437,440 bytes memory；overlayfs |
| PostgreSQL | image `postgres:17`；Flyway runtime确认`PostgreSQL 17.10` |
| Testcontainers | `1.20.4` |
| commit | `1f75373cbe17b44ae99bb76cb73f0fdb2d52275c` |
| test profile | 目标suite为JUnit/Testcontainers + isolated PostgreSQL；未启用production profile |
| connection pool | 目标adapter测试使用测试DataSource；Spring smoke包含Hikari，但本轮无可冻结的capacity pool参数或pool metrics |
| capacity parameters | `NOT_FROZEN / NOT_AVAILABLE` |
| execution date | `2026-07-13`，Asia/Shanghai |

本环境仅代表当前测试主机与当前配置，不构成production capacity certification。

## 5. Exact commands与结果

### 5.1 Git preflight

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git log --oneline -8
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only
git status --branch --short
git rev-list --left-right --count origin/dev...HEAD
```

结果：task前worktree clean、staged empty、branch `dev`、HEAD等于accepted baseline、技术范围diff为空；`origin/dev...HEAD=0/0`。

### 5.2 Criteria与harness扫描

```powershell
rg -n -i "capacity|benchmark|stress|load|throughput|latency|p95|p99|concurr|rate.?limit|nonce|replay|callback|cleanup|restart|commit.?unknown|Testcontainers|PostgreSQL" pom.xml docs/current dh-security dh-api dh-app dh-infra .github -g "!**/target/**"
```

结果：发现B1诊断记录和并发正确性测试；未发现正式capacity profile、benchmark/stress plugin、script、runner或同时冻结scenario、command、measurement与threshold的current acceptance定义。

### 5.3 Persistent guard PostgreSQL/Testcontainers correctness suite

```powershell
mvn -ntp -pl dh-infra,dh-app -am "-Dtest=V12PersistentRuntimeGuardsFlywayPostgresTest,V13TransactionalCompatibilityCallbackFlywayPostgresTest,PersistentGuardProductionWiringPostgresTest,PersistentGuardLifecycleClockArchitectureTest,JdbcNonceReplayGuardPersistenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`BUILD SUCCESS`；38 tests、0 failures、0 errors、0 skipped；真实`postgres:17`容器，Flyway确认PostgreSQL 17.10。该命令是correctness regression，不是capacity harness。

### 5.4 Full Maven regression

```powershell
mvn -ntp test
```

结果：连续两次exit 1，均在`dh-app` Surefire fork因JVM native-memory OOM中止；第一次`malloc`申请1,345,136 bytes失败，第二次申请1,808,496 bytes失败。已完成的测试报告未出现assertion failure，但reactor未完成，因此不得报告full regression PASS或沿用旧1076项数字。

### 5.5 Quality gate

```powershell
mvn -ntp -Pquality validate
```

结果：exit 0，19/19 modules `SUCCESS`，`BUILD SUCCESS`，总耗时11.494秒；root Checkstyle 0 violations；Spotless通过。各子模块仍提示没有独立`checkstyle:checkstyle` outputFile，最终root聚合门通过。

## 6. Scenario matrix与measured results

| Scenario | Actual execution | Result | Capacity verdict |
|---|---|---|---|
| same-key rate competition | 8线程、40 attempts、test-defined quota 10；accepted严格为10 | correctness PASS | 无正式轮次/阈值，capacity未验收 |
| tenant/environment isolation | saturated key后其他tenant与environment仍可接受 | correctness PASS | 无noisy-neighbor pressure threshold |
| idempotency race | 8线程、24 attempts；只1个`ADMITTED` | correctness PASS | 无持续负载或latency threshold |
| replay nonce race | 实例重建后replay拒绝、scope隔离通过；未发现same-nonce concurrent race harness | partial correctness | mandatory concurrent race未执行 |
| callback lifecycle | timeout/rollback/retry、result reference、commit-unknown测试通过 | correctness PASS | 无callback capacity harness |
| cleanup under load | 2 worker；3个eligible transition；active/future/locked行保留 | correctness PASS | 无under-load duration/throughput/backlog threshold |
| PostgreSQL contention | lock timeout、statement timeout、rollback/retry与SKIP LOCKED路径通过 | correctness PASS | 无pressure轮次与lock-wait指标阈值 |
| restart persistence | 新guard实例复用同一PostgreSQL状态后replay仍拒绝 | correctness PASS | 未形成完整application/container restart capacity round |
| fail-closed | DB异常、audit/completion rollback与commit-unknown不readmit测试通过 | correctness PASS | 未在正式pressure harness中测量 |

| Metric | Value |
|---|---|
| requested operations | capacity harness未执行；仅记录上述测试内固定attempts |
| accepted / rejected | rate test 10 accepted / 30 rejected；idempotency test 1 admitted / 23 non-winners |
| unexpected errors | targeted suite 0；full regression有JVM OOM环境错误 |
| throughput | `NOT_AVAILABLE` |
| p50 / p95 / p99 latency | `NOT_AVAILABLE` |
| max latency | `NOT_AVAILABLE` |
| database lock wait / timeout | correctness timeout路径通过；无capacity metric |
| connection pool usage | `NOT_AVAILABLE` |
| rows created / retained / cleaned | 仅test fixture断言；无capacity measurement |
| cleanup duration | `NOT_AVAILABLE` |
| restart recovery | app-level guard instance recreation PASS；formal capacity round unavailable |
| warm-up | `NOT_AVAILABLE` |
| measured rounds | `0 / FORMAL_CAPACITY_HARNESS_UNAVAILABLE` |

## 7. Correctness与threshold verdict

```text
NO_OVERSUBSCRIPTION_AT_TEST_LOAD: PASS
IDEMPOTENCY_SINGLE_WINNER_AT_TEST_LOAD: PASS
TENANT_ENVIRONMENT_ISOLATION_AT_TEST_LOAD: PASS
NONCE_RESTART_PERSISTENCE: PASS
SAME_NONCE_CONCURRENT_RACE: NOT_EXECUTED
CALLBACK_CORRECTNESS: PASS
CLEANUP_CORRECTNESS: PASS
COMMIT_UNKNOWN_FAIL_CLOSED: PASS
CAPACITY_THRESHOLD_COMPARISON: NOT_POSSIBLE
FORMAL_CAPACITY_ROUNDS: NOT_EXECUTED
```

正确性测试没有发现超额成功、跨tenant/environment污染、active/locked行误清理、commit-unknown静默成功或transaction rollback破坏。由于没有正式capacity harness，无法对callback orphan/terminal state、cleanup backlog、pool saturation、lock wait、throughput或latency进行容量级证明。

## 8. 原始证据位置与hash

Surefire XML与dumpstream保存在各模块`target/surefire-reports/**`；以下JVM诊断日志已移动到root `target/**`，均为gitignored/untracked证据，不stage、不commit：

| File | SHA-256 |
|---|---|
| `target/capacity-full-regression-hs_err_pid24700.log` | `81D379D85F00037015609B8A4D835D4A5EF0178031F0968352B2F6A51C3209A7` |
| `target/capacity-full-regression-replay_pid24700.log` | `93CC01D3E58A30C68388E1E6DB609B8DCAB12D6B3E81AD20FA3D4354771CD829` |
| `target/capacity-full-regression-retry-hs_err_pid36940.log` | `69D4CCD9D7AF32300CA3FF32813A9A309A21E7BF750346615D3646A611A29703` |
| `target/capacity-full-regression-retry-hs_err_pid37632.log` | `D2C68902F69F148FF3BD8FD4710B9D17E3F4CB8E0C2EE1F0E17630B5428786F0` |
| `target/capacity-full-regression-retry-hs_err_pid4252.log` | `3280DAD0A1B54B9D2B8900B3B669BDE182BB7757F018DA645FEABE2FBE97B0B1` |
| `target/capacity-full-regression-retry-replay_pid36940.log` | `56EB9F1303641BEA9A64D5C6CA499522088D598DD7C9A3A1B00494133A78594A` |

## 9. Readiness decision

```text
POST_B2_CAPACITY_ACCEPTANCE: BLOCKED
CAPACITY_CRITERIA_FROZEN: FAIL
CAPACITY_HARNESS_AVAILABLE: FAIL
REAL_POSTGRESQL_EXECUTION: PASS
CONCURRENCY_ATOMICITY: PASS / CORRECTNESS_TEST_LOAD_ONLY
TENANT_SOURCE_ISOLATION: PASS / CORRECTNESS_TEST_LOAD_ONLY
REPLAY_NONCE_RACE: FAIL / CONCURRENT_RACE_NOT_EXECUTED
CALLBACK_CAPACITY: FAIL / CAPACITY_HARNESS_UNAVAILABLE
CLEANUP_CAPACITY: FAIL / CAPACITY_HARNESS_UNAVAILABLE
RESTART_PERSISTENCE: PASS / APP_INSTANCE_RECONSTRUCTION_ONLY
FAIL_CLOSED_UNDER_PRESSURE: FAIL / FORMAL_PRESSURE_NOT_EXECUTED
FULL_REGRESSION: FAIL / JVM_NATIVE_MEMORY_OOM
QUALITY_GATE: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 0 CONFLICTS
B2_IMPLEMENTATION_STATUS: ACCEPTED
ALLOW_STAGE_QDR_7_B3_ENTRY: NO
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 10. 风险与下一步

- benchmark environment representativeness：当前主机可用内存低且完整回归发生native-memory OOM，不适合作为production capacity基准。
- frozen threshold adequacy：threshold尚未冻结，无法评价adequacy。
- database lock contention：正确性路径通过，但无持续负载lock-wait曲线。
- connection pool saturation：无capacity pool metrics。
- cleanup backlog：无backlog增长/清理吞吐证据。
- cross-tenant noisy-neighbor：功能隔离通过，容量级noisy-neighbor未测。
- callback recovery：正确性路径通过，容量级claim/lease/recovery未测。
- Maven wrapper risk：既有`UNUSABLE / P2 TOOLING RISK`不变；本轮使用系统Maven。
- Docker/Testcontainers stability：目标suite真实运行且0 skipped；完整回归阻断来自JVM native memory，不是Docker不可用。
- premature B3 entry：若用hard ceiling、旧0-success matrix或correctness test常量伪装capacity threshold，会越过冻结边界。

下一步只允许：

```text
DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE
```

该任务必须先冻结可追溯的scenario、command、seed/profile/payload、warm-up、measured rounds、concurrency/duration/iterations、correctness invariants、throughput/latency thresholds与环境约束，再另行判断是否需要独立harness实现授权。本报告不授权修改Java、测试、migration、callback、API/contracts或进入B3。
