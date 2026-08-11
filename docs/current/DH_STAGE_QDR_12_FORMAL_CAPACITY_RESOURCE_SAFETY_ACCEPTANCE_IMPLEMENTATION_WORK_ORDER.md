# Stage-QDR-12 Formal Capacity / Resource Safety Acceptance Implementation Work Order

## Terminal current authority — 2026-08-11 Stage-QDR-12 capacity harness remediation

~~~text
Task: DH-STAGE-QDR-12-CAPACITY-HARNESS-BLOCKER
Stage-QDR-12: HARNESS_REMEDIATED / LOCAL_ACCEPTED
Implementation baseline / origin-dev: 8a741e9be400fa238a930767e3a36a565ef9faf6 / SAME
Implementation commit: PENDING / LOCAL_ONLY / NOT_PUSHED
Capacity harness: READY_FOR_FORMAL_EXECUTION
Mandatory scenarios: 15 OF 15 EXECUTABLE / 0 SKIPPED / 0 UNIMPLEMENTED
Threshold coverage: 41 OF 41 CONSUMED / 99 COMPARISONS / 0 ORPHANS
RegressionMax coverage: 5 OF 5 MEASURED_AND_EXECUTED
Resource sampling: JVM + MAVEN + SUREFIRE + DOCKER + HOST_MEMORY / IMPLEMENTED_AND_TESTED
Runtime resource probes: QUEUE_BACKPRESSURE + DEADLINE_TIMEOUT + INPUT_MEMORY_CAP / IMPLEMENTED_AND_TESTED
Environment qualification: IMPLEMENTED_AND_TESTED / POSTGRES_17 TESTCONTAINERS ISOLATED
Formal evidence packet: IMPLEMENTED_AND_TESTED / HASH_INVENTORY_FINALIZER FAIL_CLOSED
Execution mode binding: FORMAL + QUALIFICATION + IMPLEMENTATION_VALIDATION / EXACT
Verdict taxonomy: PASS_WITHIN_FROZEN_PROFILE / FAIL / BLOCKED / INVALID / PRESERVED
Implementation validation: PASS / RUN 20260811T153800Z / FORMAL 0 OF 15 / NOT_EVALUATED
Regression: PASS / 1373 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL / Flyway: 17.10 REAL TESTCONTAINERS / V1-V15 FULL APPLY
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Initial security scans: HISTORICAL / REMEDIATED / NOT_FINAL_AUTHORITY
Security exact-diff: PASS / SEALED SCAN 9b21da1d-541f-4433-9030-a1add08b6272 / 31 OF 31 / 0 REPORTABLE / 0 DEFERRED
Security snapshot: codex-security-snapshot/v1:sha256:8596e9aad082efc20b226878f1620ed747704b987c49d5155873a1365ab88071
CodeRabbit: NOT_EXECUTED / CLI 0.7.2 AUTHENTICATED / 2 ATTEMPTS WEBSOCKET_CLOSED / TOOL_OR_NETWORK_BLOCKED
API / migration / schema / Repository / contracts / POM / workflow / NQ: NONE / NONE / NONE / NONE / NONE / NONE / NONE / NONE
Current factsources: 10 OF 10 / SYNCHRONIZED / 0 CURRENT CONFLICTS
Formal capacity / formal verdict: NOT_EXECUTED / NONE
Production capacity / production ready: NOT_PROVEN / NO
Next action: LOCAL FIX COMMIT -> SEPARATE FORMAL CAPACITY EXECUTION TASK
ALLOW_FORMAL_CAPACITY_EXECUTION / ALLOW_FORMAL_CAPACITY_EXECUTION_NOW: YES_AFTER_LOCAL_COMMIT / NO
ALLOW_LOCAL_FIX_COMMIT / ALLOW_PUSH / ALLOW_TAG: YES / NO / NO
ALLOW_PRODUCTION_READY_DECLARATION: NO
ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO / NO
~~~

本节是 capacity harness blocker 修复完成后的 current authority。harness、完整回归、质量门与唯一最终
安全 exact-diff 已通过并封存，不再重复扫描；formal capacity 未执行且本任务不授权现场执行。

## 1. 任务分类与授权

```text
Task: DH-STAGE-QDR-12-FORMAL-CAPACITY-RESOURCE-SAFETY-ACCEPTANCE-IMPLEMENTATION-WORK-ORDER
Primary classification: CODE_ANALYSIS
Delivery classification: WORK_ORDER_ONLY / DOCS_ONLY_CHANGE
Formal capacity execution: NOT_AUTHORIZED / NOT_EXECUTED
Production/test/harness/config change: NOT_AUTHORIZED
API/migration/schema/Repository/contracts/POM/workflow change: NOT_AUTHORIZED
NQ/real HTTP/real provider/feedback learning/Agent/LangGraph/Paper/LIVE: NOT_AUTHORIZED
Local work-order commit: AUTHORIZED
Push/tag: NOT_AUTHORIZED
```

本工单在现有代码上进行只读审计并冻结后续执行合同。它不运行 capacity benchmark，不把历史 qualification 当作 formal verdict，也不把普通回归或 quality PASS 当作 capacity PASS。

## 2. Plugins selected

```text
recommended by nq-dh-workflow-router: GitHub
actually used plugins: NONE / LOCAL-FIRST EVIDENCE SUFFICIENT
GitHub result: NOT_CONNECTED / LOCAL GIT ONLY / NO REMOTE WRITE
Codex Security result: NOT_EXECUTED / DOCS-ONLY WORK ORDER
CodeRabbit result: NOT_EXECUTED / NO CODE CHANGE
Skills: nq-dh-workflow-router + dh-docs-writer
```

## 3. Baseline verification

```text
repository: current decision-hub workspace
branch: dev
HEAD / planning commit: 7023dc345e67a7c5f504911591d760e1f670fda7
HEAD parent / local origin-dev: 6f0097c4a5b0eed354505d59c8a53a9f0f593557
HEAD tree: 210a7e2d7d158df243c63a11bee2b2be650f92d6
ahead / behind: 1 / 0
worktree / staged before write: clean / empty
planning diff: 9 allowlisted docs only / technical diff 0
Stage-QDR-10 / Stage-QDR-11: CLOSED / ACCEPTED / ARCHIVED / TAGGED
formal capacity / production capacity / production ready: NOT_EXECUTED / NOT_PROVEN / NO
```

该状态符合任务书允许的未发布 planning baseline。未执行 fetch；`origin/dev` 表示本地 remote-tracking ref，下一次需要 remote advertised SHA 的任务必须单独执行只读远端核验。

## 4. Scope model

### 4.1 READ_SCOPE

```text
AGENTS.md
CLAUDE.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/current/DH_POST_STAGE_QDR_11_NEXT_STAGE_PLAN.md
config/qdr7-capacity/**
scripts/qdr7-capacity/**
pom.xml
dh-bom/pom.xml
dh-app/pom.xml
dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/**
limited dry-run runtime/config/tests directly needed to verify resource-safety coverage
.github/workflows/ci.yml
```

不扫描 `target`、`build`、`dist`、`.git`、logs、test-results、secret-bearing paths 或 NQ 仓库。

### 4.2 FACTSOURCE_WRITE_ALLOWLIST

```text
docs/current/DH_STAGE_QDR_12_FORMAL_CAPACITY_RESOURCE_SAFETY_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_POST_STAGE_QDR_11_NEXT_STAGE_PLAN.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

`FACTSOURCE_POLICY.md` 与 `ARCHIVE_INDEX.md` 只读；本轮没有修改 authority policy 或 archive policy。

### 4.3 EXECUTION_SCOPE

本轮为空。未来 formal task 只有在本工单列出的 blocker 全部关闭后，才可执行以下冻结入口一次：

```text
mvn -B -ntp -Pqdr7-capacity-acceptance \
  -Dqdr7.runId=<UTC_yyyyMMddTHHmmssZ> \
  -Dqdr7.seed=7 verify
```

`runId` 同时作为 `attemptId`。不得在本轮或 blocker 修复轮执行该命令。

### 4.4 ARTIFACT_OUTPUT_SCOPE

```text
target/qdr7-capacity-qualification/<attemptId>/
target/qdr7-capacity-acceptance/<attemptId>/
isolated Testcontainers container/volume for the same attempt
ordinary temporary Maven build output
```

这些均为 untracked runtime output；不得写入 tracked source，不得连接 production/shared operational DB。

### 4.5 FORBIDDEN_PATHS

```text
all production Java source changes
all test source changes
config/qdr7-capacity/** changes
scripts/qdr7-capacity/** changes
pom.xml / dh-bom/pom.xml / dh-app/pom.xml changes
.github/workflows/** changes
contracts/** / golden_cases/** changes
migration / API / Repository / schema changes
docs/gates/** / docs/archive/** changes
NQ repository/worktree
secret/credential/.env/key/pem material
```

### 4.6 Scope invariants

```text
VALIDATION_SCOPE subset_of READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset_of WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset_of WRITE_ALLOWLIST: PASS
```

本轮 blocker 只能写成文档结论，不能在本任务内修复技术实现。

## 5. Code reality inspected

### 5.1 Harness entry and lifecycle

- Maven profile：`qdr7-capacity-acceptance`，root `pom.xml` 与 `dh-app/pom.xml`。
- Preflight/finalizer：`scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1`，分别绑定 `pre-integration-test` 与 `post-integration-test`。
- Formal dispatcher：`Qdr7CapacityAcceptanceIT`，仅在显式 profile 与 `qdr7.capacity.profile.active=true` 时由 Failsafe 执行。
- Resource sampler：`Watch-Qdr7CapacityResources.ps1`。
- Contract tests：`Qdr7CapacityContractsTest`、`Qdr7CapacityPowerShellContractTest`、`Qdr7CapacityProfileIsolationTest`、`ArtifactValidatorTest` 与 runtime binding script。
- 普通 CI 只执行 `mvn test` 与 `mvn -Pquality validate`，不会执行 formal profile。

### 5.2 Mandatory scenario registry

实际 mandatory 数量为 `15`。JSON registry 与 Java dispatcher 顺序一致：

| # | Scenario | 当前实际覆盖 |
|---:|---|---|
| 1 | `actual-wiring` | localhost Spring wiring、13 个 structured 2xx、Hikari/Testcontainers/mock provider identity |
| 2 | `rate-matrix` | concurrency 1/2/4/8/16；每点 3 轮；每轮 warm-up 20、measurement 100；吞吐与 p50/p95/p99/max latency |
| 3 | `cold-start-quota` | 3 轮；8 并发、40 attempts、quota 10；DB fixed-window 无 oversell |
| 4 | `tenant-environment-isolation` | 3 轮 tenant/environment identity 隔离与 cross-scope row=0 |
| 5 | `canonical-source-fail-closed` | 非 canonical source 返回 403 / `SOURCE_DENIED` |
| 6 | `nonce-race` | 3 轮；8 线程、24 attempts；1 winner、23 replay、DB winner row=1 |
| 7 | `idempotency-lifecycle` | admission/lease/terminal/result-reference/rollback/cross-scope；部分 recovery 语义依赖 ordinary regression |
| 8 | `tenant-scoped-cleanup` | scale 10/100/1000、2 workers、batch 10、backlog 收敛与 duration |
| 9 | `postgres-hikari-contention` | pressure pool 4、17 workers、Hikari pending/acquire、PostgreSQL wait/lock wait/deadlock |
| 10 | `postgres-same-pool-recovery` | 3 轮同 ApplicationContext/DataSource/Hikari/endpoint/volume 数据库停止与恢复 |
| 11 | `spring-context-restart` | 3 轮 context/dataSource replacement 与 committed/rolled-back state 验证 |
| 12 | `postgres-persistent-volume-restart` | 3 轮 persistent-volume restart 与状态保留 |
| 13 | `post-recovery-concurrency` | 恢复后 8 并发、100 structured 2xx |
| 14 | `full-regression` | 独立 `mvn -B -ntp test`、19 reactor、Testcontainers mandatory reports、0 skipped |
| 15 | `quality-gate` | 独立 `mvn -B -ntp -Pquality -DskipTests validate` |

### 5.3 Threshold reality

`qdr7-capacity-thresholds.json` 的真实结构为：

```text
numeric threshold leaves declared: 41
numeric threshold leaves referenced by addThreshold: 36
expected comparison executions: 94
declared but not compared resource thresholds: 5
correctness invariants: 12
```

`94` 是执行比较次数，不是 94 个独立 threshold definition：

```text
rate: 5 concurrency points * 3 rounds * 5 metrics = 75
cleanup: 3 scales * 1 duration = 3
contention: 4 metrics = 4
recovery: 3 rounds * 4 metrics = 12
total = 94
```

当前参与 hard gate 的数值比较为上述 94 次；任一 `addThreshold` FAIL 会进入 exit 50。12 个 correctness invariant 通过 scenario assertions、ledger、artifact/secret/regression 检查 fail closed。`full-regression` 与 `quality-gate` 分别由 exit 60/70 独立 hard gate。

以下 5 个已声明 resource thresholds 当前没有代码读取或比较，因此不能声称已 hard-gated：

```text
regressionMax.durationMs = 660000
regressionMax.mavenWorkingSetBytes = 603979776
regressionMax.surefireWorkingSetBytes = 2147483648
regressionMax.dockerMemoryBytes = 201326592
regressionMax.minimumFreeMemoryBytes = 13958643712
```

因此 planning 的 `94 thresholds` 必须更正为 `94 comparison executions / 41 declared numeric leaves / 5 unenforced resource leaves`。

### 5.4 Environment qualification reality

当前 preflight 共 26 项：run id、seed、PowerShell、branch、SHA 形状、worktree/staged、criteria version/source hash、5 个 JSON contract 可解析、Java、Maven、`MAVEN_OPTS`、Windows 11 x64、logical CPU、available memory、Docker daemon/version、Docker memory、cached `postgres:17`、container/volume absence、dynamic loopback port。

已明确实现历史 blocker 检查：

```text
docker image inspect postgres:17
missing -> postgres-image BLOCKED
preflight -> exit 10 / ENVIRONMENT_CAPACITY_PREFLIGHT_BLOCKED
scenario ledger -> 15 NOT_STARTED
threshold comparison -> 0 executed / 94 not evaluated
```

历史 `20260719T082658Z` formal attempt 正是该路径：25/26 preflight PASS，`postgres-image` missing，mandatory scenario 0/15，Maven/internal exit 1/10。

当前 preflight 没有完整实现本阶段要求的 Docker storage health、Testcontainers viability 独立 admission、CPU class/model、disk free、filesystem、clock/NTP offset、network policy、credential absence、background load policy；也没有把 remote advertised SHA、Git tree、profile/scenario/threshold hashes写入 environment manifest。因此 qualified environment contract 尚不能关闭。

### 5.5 Artifact/finalizer/verdict reality

现有 harness 会产生 JSON/CSV/log、scenario ledger、threshold comparison、summary、secret scan 与 SHA-256 manifest；它是 machine-readable 且 manifest 可复验。Java summary 先按 ledger/threshold/regression/quality计算，PowerShell finalizer再执行 resource teardown、mandatory artifact/schema/header validation、secret scan 与 manifest validation。

现有 native taxonomy 为 `PASS / FAIL / BLOCKED`，qualification 使用 `NOT_FORMAL`，没有 `INVALID`。当前 artifact common fields 包含 `runId/commitSha/criteriaVersion/timestamps/seed`，但不包含本阶段要求的：

```text
candidateTree
profileId/profileVersion
scenarioSetHash
thresholdSetHash
environmentManifestHash
harnessVersion/harnessHash
attemptId as an explicit field
generatedAt as an explicit field
```

也没有独立 `execution-manifest.json` 或 final artifact inventory JSON。若现在直接执行，packet 无法满足本阶段 exact-SHA/profile/environment binding，B4 必须判为 `INVALID`。

### 5.6 Resource-safety coverage reality

| Dimension | Current evidence | Work-order decision |
|---|---|---|
| rate limit | cold-start quota、tenant/environment identity、DB bucket | 已覆盖 |
| concurrency | rate 1/2/4/8/16、quota/nonce 8、contention 17、post-recovery 8 | 有 load coverage |
| latency | throughput、p50/p95/p99/max | 已 hard-gated |
| DB pressure | Hikari pending/acquire、PostgreSQL wait/lock wait/deadlock | 已 hard-gated |
| failure/recovery | same-pool、context、persistent volume、post-recovery | 已覆盖 |
| cleanup/backlog | 3 scales、2 workers、duration/backlog | 已覆盖 |
| memory | sampler 只可靠记录 Maven working set 与 host available；heap/non-heap/native 缺失，Docker memory 未规范化；5 个 resource thresholds 未比较 | GAP / BLOCKER |
| queue depth | runtime queue capacity=8，但 formal harness 不采样 actual queue depth | GAP / BLOCKER |
| backpressure | concurrency=16 间接施压，但未强制产生和验证 structured queue rejection/queue-wait evidence | GAP / BLOCKER |
| deadline | runtime deadline=5000 ms、max queue wait=100 ms；ordinary unit tests存在，formal harness 无 deadline scenario/metric | GAP / BLOCKER |
| timeout | connection/statement/process/scenario timeouts存在；没有把 runtime deadline/provider timeout 作为 formal resource result | GAP / BLOCKER |
| input memory cap | production path cap=32768 bytes，ordinary controller/usecase tests存在；formal harness未验证 cap rejection | GAP / BLOCKER |

该 gap 触发：

```text
STAGE_QDR_12_CAPACITY_HARNESS_GAP_BLOCKED
```

不得通过文档把缺失指标补写成已支持，也不得直接进入 formal run。

### 5.7 Qualification/formal separation、side effect、phase、retry

1. Qualification 与 formal 使用不同 output root，且 qualification 强制 `formalAcceptanceVerdict=NOT_EVALUATED`、`capacityAcceptanceExecuted=false`，verdict 隔离成立。
2. 但 qualification mode 仍执行 15 scenarios/94 comparisons；它不是纯 environment-only admission。本阶段 B2 需要独立 environment/harness admission artifact，不能把 qualification scenario PASS 等同 capacity PASS。
3. Harness 通过 localhost `HttpClient` 调用本进程 Spring endpoint，provider 为 `MockModelProvider`；无 real outbound HTTP、real provider 或 NQ call。
4. Harness 会写业务形状的表，但只写 dynamic properties 指向的 dedicated `postgres:17` Testcontainers database/volume；不得连接 production/shared DB。
5. Rate matrix 有 warm-up 20 + measurement 100 + 3 rounds；没有独立 cool-down phase。其他 scenarios 使用固定 rounds/attempts；seed 固定为 7。
6. Failsafe 未配置 test rerun；attempt-level retry 不存在。Recovery scenario 内 50ms bounded polling和固定 rounds属于 scenario contract，不是 formal attempt retry。
7. 同一 `attemptId/runId` 已存在时 preflight拒绝；失败后不得在同一 attempt 内重跑。新 attempt 只能由未来独立授权任务启动。

## 6. Frozen exact-SHA/profile identity contract

未来 B1 必须在任何 qualification/formal process 启动前生成 execution-freeze record，至少包含：

```text
candidateSha = git rev-parse HEAD
candidateTree = git rev-parse 'HEAD^{tree}'
advertisedSha = remote refs/heads/dev read-only result
profileId = qdr7-capacity-acceptance
profileVersion = qdr7-capacity-criteria-1
scenarioSetId = qdr7-capacity-scenarios-1
scenarioSetHash = SHA-256(raw bytes of qdr7-capacity-scenario-registry.json)
thresholdSetId = qdr7-capacity-criteria-1
thresholdSetHash = SHA-256(raw bytes of qdr7-capacity-thresholds.json)
criteriaSourceHash = SHA-256(raw bytes of qdr7-capacity-acceptance-criteria.md)
harnessVersion = qdr7-capacity-1 plus exact candidate SHA
harnessHash = SHA-256 manifest over the sorted frozen harness file allowlist
environmentManifestHash = SHA-256(final qualified environment manifest raw bytes)
attemptId = UTC yyyyMMddTHHmmssZ
artifactOutputDirectory = exact normalized path under target/qdr7-capacity-acceptance/attemptId
```

Formal admission 必须同时满足：

```text
HEAD = local origin/dev = advertisedSha
candidateSha = advertisedSha
worktree clean
staged empty
untracked technical config 0
all frozen hashes match
qualified environment manifest is complete and immutable
no prior artifact directory for attemptId
```

run 开始后任一 identity/hash/path 改变，或执行后换 SHA 沿用 artifact，attempt 直接 `INVALID`。

## 7. Qualified environment contract

B2 必须形成独立 machine-readable `capacity-environment-manifest.json`（可由现有 `environment.json` 演进或等价实现），只输出：

```text
QUALIFIED
NOT_QUALIFIED
```

`QUALIFIED != CAPACITY_PASS`。manifest 必须记录并验证：

- Windows 11 x64 与 architecture；Java 21；Maven 3.9.x；resolved PowerShell identity。
- Docker Engine 29.x、daemon availability、storage driver/health、Docker memory、无遗留 attempt container/volume。
- cached exact `postgres:17` image identity/digest；禁止 qualification 后替换 image。
- 实际启动并关闭 dedicated Testcontainers/PostgreSQL 17 smoke probe；`docker ps` 单独成功不构成 qualified。
- logical CPU >=16、CPU model/class；host available memory >=16 GiB、Docker memory >=16 GiB。
- evidence/build volume 的 filesystem type、可写性、disk free bytes；定量最低值尚未被当前代码/criteria定义，blocker task 必须以可审查证据冻结后才可 QUALIFIED。
- clock source、UTC、NTP/synchronization state 与 measured offset；offset ceiling 尚未定义，blocker task 必须冻结。
- required loopback port availability与 localhost-only network policy；外部 egress不可作为运行依赖。
- relevant environment variable name/state allowlist；`MAVEN_OPTS` unset；禁止输出值。
- credential absence检查只验证未配置/未访问，不读取或打印 secret 内容。
- background CPU/memory/disk load observation window与 admission ceiling；当前未定义，blocker task必须冻结。

由于 disk/clock/background-load thresholds、Testcontainers smoke admission与完整 manifest binding 尚未实现，当前结论为：

```text
QUALIFIED_ENVIRONMENT_CONTRACT: BLOCKED
HISTORICAL_POSTGRES_BLOCKER: CHECK PATH EXISTS / CURRENT ENVIRONMENT NOT_EXECUTED
```

## 8. Single formal attempt discipline

- 下一 execution task 最多运行一个 formal attempt；不得 parallel launch。
- attempt identity、SHA/tree、profile/set hashes、environment hash、output directory、start/end time必须先冻结。
- source、test、POM、workflow、Docker image、threshold、scenario不得在 attempt 中改变。
- mandatory scenario无法正常完成为 `BLOCKED`；已执行且 hard threshold/correctness失败为 `FAIL`。
- 不允许同一 attempt 自动 retry。仅现有 scenario 内固定轮次与 bounded recovery polling可执行。
- failure 后不得复用 attemptId，也不得在同一任务现场创建第二 attempt。
- shell/agent timeout 不等于 capacity FAIL。必须先定位 Maven/Java/PowerShell/Docker残留进程、读取 resource registry/ledger/artifacts、确认 attempt仍运行或已终止；只有归类为 `BLOCKED` 或 `INVALID` 并停止残留负载后，未来独立授权才能启动新 attempt。

## 9. Verdict taxonomy and adjudication

B4 使用四态治理 verdict；native harness 状态只是输入：

| Governance verdict | 冻结语义 |
|---|---|
| `PASS_WITHIN_FROZEN_PROFILE` | 15/15 mandatory真实完成并通过；全部 hard comparisons/correctness/regression/quality通过；resource-safety补齐项通过；artifact完整；SHA/tree/profile/scenario/threshold/environment/harness hashes一致；secret/teardown/residual通过 |
| `FAIL` | harness与环境正常完成，但一个或多个 hard threshold/correctness/regression/quality gate失败 |
| `BLOCKED` | qualified environment、Docker/PostgreSQL/Testcontainers、startup、基础设施或 mandatory fixture使 scenario无法正常完成 |
| `INVALID` | dirty/scope/hash/profile/SHA/tree/environment/attempt identity漂移；mandatory artifact缺失；manifest/schema/binding/secret integrity被破坏；duplicate/reused attempt |

映射规则：

```text
native PASS -> 仅在 B4 全部复验后映射 PASS_WITHIN_FROZEN_PROFILE
native FAIL -> FAIL，除非根因属于 artifact/scope integrity，则 INVALID
native BLOCKED -> BLOCKED，除非根因属于 binding/artifact integrity，则 INVALID
missing/skipped mandatory scenario -> BLOCKED
missing mandatory artifact -> INVALID
exit 80 artifact validation BLOCKED -> governance INVALID
qualification PASS/NOT_FORMAL -> no capacity verdict
```

当前 harness只有三态且缺少本阶段绑定字段；因此任何现在产生的 formal packet都不能被 adjudicate 为 PASS。

## 10. Artifact contract

未来 remediation 后的 formal packet 至少包含：

```text
capacity-environment-manifest.json
capacity-execution-manifest.json
scenario-ledger.json and scenario result artifacts
threshold-comparison.json
jvm-series.csv / docker-series.csv and normalized resource-summary.json
capacity-acceptance-summary.json
capacity-final-verdict.json
artifact-inventory.json
sha256-manifest.txt
secret-scan.json
```

每个 machine-readable artifact 必须绑定或可经 execution manifest无歧义继承：

```text
attemptId
candidateSha
candidateTree
profileId/profileVersion
scenarioSetHash
thresholdSetHash
environmentManifestHash
harnessVersion/harnessHash
generatedAt
```

finalizer 顺序固定为：停止 sampler与registered resources → 归一化ledger → 校验scenario/threshold/resource/regression/quality → secret scan → artifact inventory → SHA-256 manifest → summary/verdict最终化 → 再次manifest复验。finalization 后任何字节变化都使 packet `INVALID`。

当前 artifact packet缺少 execution manifest、inventory、resource summary和上述绑定字段，故：

```text
ARTIFACT_CONTRACT: BLOCKED
```

## 11. No-side-effect contract

```text
real provider calls = 0
real outbound HTTP = 0
NQ calls / mutation / DB access = 0
orders / cancellation = 0
Paper / LIVE = 0
feedback learning / promotion = 0
Agent / LangGraph = 0
credential access = 0
production/shared operational DB writes = 0
```

只允许 localhost Spring endpoint、deterministic mock provider、synthetic auth material、dedicated Testcontainers PostgreSQL与temporary artifact/build output。任何越界立即 `INVALID`，并按安全事件保留最小脱敏证据。

## 12. B1-B4 execution order

### B1 — Exact-SHA + formal scope freeze

- 在 blocker remediation通过并独立 review后，核验 local/remote advertised exact SHA、tree、clean/staged、ordinary exact-SHA CI。
- 生成 profile/scenario/threshold/harness hash manifest与 execution-freeze record。
- B1 只冻结合同，不执行 qualification/formal。

### B2 — Qualified environment / harness admission

- 执行完整 environment contract与 cached exact `postgres:17`/Testcontainers smoke admission。
- 只生成 `QUALIFIED` 或 `NOT_QUALIFIED`，不形成 capacity verdict。
- `NOT_QUALIFIED` 转 `DH-STAGE-QDR-12-QUALIFIED-ENVIRONMENT-BLOCKER`；不得启动 B3。

### B3 — Single formal capacity run

- 仅一个 attempt，执行冻结 formal command。
- 15 mandatory identities必须全部运行；修复后的 resource safety checks必须作为现有scenario增强或经独立 review后的scenario-set版本进入 frozen set。
- 产生完整、self-contained、hash-verifiable packet；不做现场修复或第二次运行。

### B4 — Evidence adjudication / final close preparation

- 独立复算 hashes、scenario ledger、94 comparison及补齐后的resource gates、summary与四态 verdict。
- PASS也只声明 `PASS_WITHIN_FROZEN_PROFILE`；生产 readiness单独保持 `NO / NOT_AUTHORIZED`。
- stage final close、archive packet、archive close、annotated tag与post-tag cleanup仍是后续分离任务；candidate tag为 `dh-stage-qdr-12-close`。

```text
B5: NOT_NEEDED
```

## 13. Validation matrix

| Gate | Required evidence | Pass condition |
|---|---|---|
| baseline | Git SHA/tree/local remote-tracking/advertised remote/clean/staged | 全部一致 |
| ordinary CI | exact-SHA test + quality | PASS；不能替代 capacity |
| profile/set | profile/scenario/threshold/harness hashes | frozen且完整 |
| environment | qualified manifest | 所有字段/threshold通过，Testcontainers smoke完成，cached image identity固定 |
| scenarios | ledger | 15/15 completed + passed；0 skipped/not-started/partial |
| numeric thresholds | comparison artifact | 94/94既有比较通过，且5个resource thresholds被真实消费或经reviewed criteria version明确处置 |
| resource safety | normalized resource summary | memory/queue/backpressure/deadline/timeout/latency/DB pressure/recovery均有可判定证据 |
| correctness | invariant results | zero-tolerance全部为0 |
| regression | `mvn -B -ntp test` | 19/19，0 failure/error/skipped，Testcontainers实跑 |
| quality | `mvn -B -ntp -Pquality validate` | 19/19，Checkstyle 0，Spotless PASS |
| artifacts | schema/inventory/hash/secret/teardown | 0 missing/mismatch/finding/residual |
| B4 adjudication | independent recalculation | 四态 verdict唯一、可复验 |

本 work-order 任务只运行 `mvn -B -ntp -Pquality validate`；完整测试 `NOT_RERUN / WORK_ORDER_ONLY`。Stage-QDR-11的1338测试、PostgreSQL 17.10与 CI 31312337732/31313584409/31314059545只作为 historical reused evidence。

## 14. Review triggers

仅以下变化触发独立 review：capacity profile/threshold semantics、scenario set、production/test code、migration/API/security boundary、P0/P1、formal adjudication、stage final close。Qualification本身不单独触发 review；当前 harness blocker必然涉及 test/script/config/POM语义，完成后必须独立 review再允许 B1。

## 15. Rollback

- 本轮仅文档：使用本地 work-order commit 的普通 `git revert <commit>`；禁止 reset/rebase/history rewrite。
- formal execution尚未开始，无 runtime rollback。
- blocker remediation必须独立小步提交；若失败，普通 revert并保持 `FORMAL_CAPACITY=NOT_EXECUTED`。
- future artifact失败可保留为不可变 evidence；不得提交到 Git，不得用清理证据伪装未执行。

## 16. Production-readiness wording

只有未来 B4 PASS 才可写：

```text
CAPACITY_PASS: PASS_WITHIN_FROZEN_PROFILE
PRODUCTION_CAPACITY: PROVEN_WITHIN_FROZEN_PROFILE
PRODUCTION_READY: NO / NOT_AUTHORIZED
```

禁止从 capacity PASS 推导 real HTTP、Provider、NQ runtime、feedback learning、Agent/LangGraph、Paper或LIVE。当前仍为：

```text
FORMAL_CAPACITY: NOT_EXECUTED
PRODUCTION_CAPACITY: NOT_PROVEN
PRODUCTION_READY: NO
```

## 17. Work-order decision

```text
STAGE_QDR_12_IMPLEMENTATION_WORK_ORDER: DONE
CAPACITY_HARNESS: GAP_FOUND
MANDATORY_SCENARIO_IDENTITIES: VERIFIED / 15
MANDATORY_SCENARIO_SET: BLOCKED / RESOURCE_SAFETY_COVERAGE_INCOMPLETE
THRESHOLD_SET: BLOCKED / 41 DECLARED / 36 USED / 94 COMPARISONS / 5 RESOURCE LEAVES UNENFORCED
QUALIFIED_ENVIRONMENT_CONTRACT: BLOCKED / REQUIRED ADMISSION FIELDS AND THRESHOLDS INCOMPLETE
HISTORICAL_POSTGRES_BLOCKER: CHECK PATH VERIFIED / CURRENT ENVIRONMENT NOT_EXECUTED
FORMAL_ATTEMPT_CONTRACT: FROZEN IN WORK ORDER
ARTIFACT_CONTRACT: BLOCKED / REQUIRED BINDINGS AND PACKET FILES MISSING
VERDICT_TAXONOMY: FROZEN IN WORK ORDER / NATIVE HARNESS MAPPING INCOMPLETE
NO_SIDE_EFFECT: FROZEN
ALLOW_FORMAL_CAPACITY_EXECUTION: NO
ALLOW_FORMAL_CAPACITY_EXECUTION_NOW: NO
ALLOW_PRODUCTION_READY_DECLARATION: NO
ALLOW_NQ_RUNTIME / ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER: NO / NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME: NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE: NO / NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 18. Risks

| Risk | Current control |
|---|---|
| environment qualification drift | exact manifest/hash contract；当前 blocker未关闭 |
| Docker/image availability | cached `postgres:17` inspect + future digest/viability admission |
| scenario/threshold drift | raw-byte hashes + candidate tree；当前 resource semantics blocker |
| duplicate formal attempts | unique attemptId/root + single-attempt governance |
| shell timeout/residual process | process/registry/artifact audit before any future attempt |
| artifact integrity | inventory + SHA-256 + B4 independent recalculation；当前 bindings blocker |
| false PASS from skipped scenario | 15/15 ledger hard requirement；missing/skipped => BLOCKED |
| capacity/profile overgeneralization | `PASS_WITHIN_FROZEN_PROFILE` only |
| production-ready wording inflation | production ready stays NO |
| resource false PASS | formal execution blocked until memory/queue/backpressure/deadline/timeout coverage closes |

## 19. Next concrete action

```text
DH-STAGE-QDR-12-CAPACITY-HARNESS-BLOCKER
```

该 blocker 必须最小关闭：5个未消费resource thresholds、resource sampler缺失/不可判定值、queue/backpressure/deadline/timeout/input-memory formal coverage、exact tree/profile/set/environment binding、execution manifest/inventory/final verdict packet、qualified environment缺失字段。该任务会涉及 test/script/config/POM语义，必须另行授权；本工单不授权其实现。

建议 commit message：

```text
docs(qdr): define stage-qdr-12 formal capacity acceptance work order
```

## 20. 本轮文件与验证结果

实际修改：

```text
docs/current/DH_STAGE_QDR_12_FORMAL_CAPACITY_RESOURCE_SAFETY_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_POST_STAGE_QDR_11_NEXT_STAGE_PLAN.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

实际验证：

```text
git status before write: CLEAN / STAGED EMPTY
git diff --check: PASS
allowlisted docs: 10
unexpected files: 0
technical diff: 0
workflow diff: 0
archive diff: 0
scope invariants: PASS / 3 OF 3
current terminal blocks: 8 OF 8 / IDENTICAL SHA-256 / 0 CONFLICTS
quality command: mvn -B -ntp -Pquality validate
quality: PASS / 19 OF 19 REACTOR SUCCESS
Checkstyle / Spotless: 0 / PASS
full tests: NOT_RERUN / WORK_ORDER_ONLY
formal capacity benchmark: NOT_RUN
local commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
push / tag: NO / NO
```
