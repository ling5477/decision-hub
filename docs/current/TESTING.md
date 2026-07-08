# Decision Hub Testing

> supporting role: current validation evidence
> primary stage gate source: only for actual command results and tooling risk

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-BLOCKER-FIX validation

```text
Task type: BLOCKER_FIX + TESTCONTAINERS_VALIDATION + FLYWAY_POSTGRES_LOAD_VERIFICATION + NO_FEATURE_EXPANSION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

### Blocker fix validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | EXPECTED_DIRTY / B2_ALLOWED_SCOPE | dirty 范围为上一轮 B2 implementation 允许范围和本轮 current docs 同步范围；无 staged 文件。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -15` | PASS | 包含 B1 domain contracts、B2 persistence plan、B2 implementation work order commits。 |
| `git diff --check` | PASS_WITH_CRLF_WARNINGS | exit code 0；仅 Git LF-to-CRLF warning，不是 whitespace error。 |
| `git diff --cached --name-only` | PASS / EMPTY | 无 staged 文件。 |
| `docker ps` | PASS / DOCKER_DAEMON_AVAILABLE | Docker daemon 可连接；初始容器列表为空。 |
| `mvn -ntp -pl dh-app -am "-Dtest=V9QdrReplayEvaluationFlywayPostgresTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | BUILD SUCCESS / POSTGRES_FLYWAY_PASS | Testcontainers 连接 Docker Desktop 29.6.1，启动 PostgreSQL 17.10；Flyway successfully validated 9 migrations，并成功迁移到 version v9；`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | BUILD SUCCESS / POSTGRES_TESTCONTAINERS_PASS | scoped reactor 全绿；`dh-infra` 88 tests，0 skipped；`dh-app` 84 tests，0 skipped；`PostgresContainerSmokeTest` 与 `V9QdrReplayEvaluationFlywayPostgresTest` 均真实执行 PostgreSQL/Testcontainers。 |

### Blocker result

```text
V9 PostgreSQL/Flyway load: PASS
Docker/Testcontainers: VERIFIED
V9QdrReplayEvaluationFlywayPostgresTest: PASS / NOT_SKIPPED
STAGE_QDR_4_B2_IMPLEMENTATION: DONE
ALLOW_STAGE_QDR_4_B2_CLOSE_REVIEW: YES
B3 implementation: NOT_STARTED
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION validation

```text
Task type: IMPLEMENTATION + MIGRATION + REPOSITORY + TESTS + QDR_REPLAY_EVALUATION_PERSISTENCE + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 implementation: BLOCKED / IMPLEMENTED / FLYWAY_POSTGRES_LOAD_UNVERIFIED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

### Implementation validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` / `git branch --show-current` / `git status --short`（开工前） | PASS | 当前目录 `F:\project\decision-hub`，分支 `dev`，起始工作区 clean。 |
| `git log --oneline -15`（开工前） | PASS | 包含 `feat(qdr): add replay evaluation domain contracts`、`docs(qdr): plan replay evaluation persistence baseline`、`docs(qdr): define replay evaluation persistence implementation work order`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 whitespace error、tracked diff 或 staged diff。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | BUILD SUCCESS / WITH_DOCKER_SKIPS | Reactor 测试通过；可见汇总 792 tests，0 failures，0 errors，5 skipped。V9 PostgreSQL/Flyway Testcontainers load test 因 Docker unavailable 被 skip，不能写成 Flyway PASS。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。系统 Maven settings 仍有 `profiles` warning，非本轮阻断。 |
| `.\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| `docker ps` | DOCKER_UNAVAILABLE / BLOCKS_FLYWAY_POSTGRES_LOAD | 无法连接 `npipe:////./pipe/dockerDesktopLinuxEngine`；本机 Docker daemon 不可用，阻断 V9 PostgreSQL/Flyway load 实证。 |
| required safety wording scan | REVIEWED / ALLOWED_HITS_ONLY | 用户指定 `rg` 已执行；全量命中主要为 docs prohibition、legacy safety notes、redaction/trading guard、migration CHECK/COMMENT 和测试守卫。窄范围复核确认本轮新增生产代码未新增 API/Controller/provider/HTTP/Agent/LangGraph/LIVE 实现；敏感词只在 guard / migration 禁止 key / 注释 / 测试中出现。 |
| tenant query guard scan | REVIEWED / NEW_ADAPTERS_PASS | 新增 `JdbcReplayCaseRepository`、`JdbcEvaluationCaseRepository`、`JdbcRegressionVerdictRepository` 查询和 ref consistency SQL 均包含 `tenant_id`；repository ports 未出现 UUID-only `findById(UUID id)` 或不带 tenant 的 case/evaluation/verdict 查询。 |

### B2 implementation coverage result

```text
V9 migration file: CREATED
file-level V9 migration presence tests: PASS
PostgreSQL/Flyway V9 load test: SKIPPED / DOCKER_UNAVAILABLE / NOT_PASS
repository ports: PASS / TENANT_BOUND
JDBC adapters: PASS / TENANT_BOUND
redaction guard: PASS
trading-term guard: PASS
cross-tenant read tests: PASS
duplicate behavior tests: PASS
repository save failure fail-closed test: PASS
quality validate: PASS
readiness: BLOCKED until PostgreSQL/Flyway load is verified
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION-WO validation

```text
Task type: WORK_ORDER_ONLY + B2_IMPLEMENTATION_BOUNDARY_DESIGN + MIGRATION_IMPLEMENTATION_WO + REPOSITORY_IMPLEMENTATION_WO + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` | PASS | 当前目录为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -15`（开工前） | PASS | 最近 15 条包含 `6a43256 docs(qdr): plan replay evaluation persistence baseline` 与 `73b74d2 feat(qdr): add replay evaluation domain contracts`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 whitespace error、tracked diff 或 staged diff。 |
| B2 freeze review conclusion | PASS / USER_PROVIDED_AND_PRIOR_REVIEWED | 上一轮 freeze review 输出 `STAGE_QDR_4_B2_FREEZE_REVIEW: PASS`，本轮用户前置状态也声明 freeze review 已通过；本轮写回 current docs。 |
| safety wording scan | REVIEWED / FALSE_POSITIVE_ONLY | `B2 implementation.*STARTED` 命中 `NOT_STARTED` 行，为 regex 子串假阳性；`BUY` / `SELL` / `MARKET_ORDER` / `PLACE_ORDER` / `CANCEL_ORDER` / `MUTATE_NQ_STATE` 命中均为禁止项、测试守卫或风险说明；`FACTSOURCE_POLICY.md:62` 为 hard-error phrase 清单 residual；未发现 real HTTP / provider / LangGraph / Agent / LIVE 启用语义。 |
| forbidden scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` diff 均为空。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| Maven settings warning | P2 TOOLING RISK / NON_BLOCKING | 系统 Maven 仍输出 `Unrecognised tag: 'profiles'`，来源 `D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml`；不影响本轮 `BUILD SUCCESS`。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B2 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | Flyway V9 migration loads successfully | `V9__qdr_replay_evaluation_baseline.sql` 可由 Flyway 顺序加载。 |
| 2 | repository saves replay case with tenant_id | 保存后可按 tenant + `case_id` / `trace_id` 查询。 |
| 3 | repository rejects missing tenant_id | 缺失 tenant 直接 fail-closed，不写库。 |
| 4 | repository query is tenant-bound | 所有查询 SQL 必须包含 `tenant_id = ?` 或等价 tenant 条件。 |
| 5 | repository does not persist raw prompt | raw prompt 样本被拒绝，或只保存 hash/ref。 |
| 6 | repository does not persist raw provider response | raw provider response 样本被拒绝，或只保存 hash/ref/summary。 |
| 7 | repository does not persist credential | credential-like 样本被拒绝。 |
| 8 | regression verdict can be saved and queried | verdict 可按 tenant + `verdict_id` / `evaluation_id` 查询。 |
| 9 | regression finding list can be saved and queried | 多 finding 可按 tenant + `verdict_id` 分页查询。 |
| 10 | duplicate case_id behavior is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 11 | duplicate evaluation_id behavior is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 12 | cross-tenant replay case read returns empty or fail-closed | tenant B 不得读到 tenant A replay case。 |
| 13 | cross-tenant evaluation case read returns empty or fail-closed | tenant B 不得读到 tenant A evaluation case。 |
| 14 | cross-tenant verdict read returns empty or fail-closed | tenant B 不得读到 tenant A verdict/finding。 |
| 15 | pagination pageSize > 100 is rejected or capped | 超上限分页请求 fail-closed 或显式 capped。 |
| 16 | BUY / SELL / MARKET_ORDER cannot be persisted as executable action | 可执行交易动作不得进入 persisted action。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE cannot be persisted as allowed action | 禁止动作不得保存为允许动作。 |
| 18 | JSONB summary does not contain raw prompt/provider response/credential keys | JSONB 只含结构化 summary/ref，不含 raw/sensitive key。 |
| 19 | repository save failure fails closed | DB / serialization / constraint failure 不被吞掉。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |

### Planned validation commands for implementation

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

`.\mvnw.cmd -v` 若仍失败，必须记录 `WRAPPER_UNUSABLE / P2 TOOLING RISK`，不得写成 PASS。

## 2026-07-08 DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN validation

```text
Task type: PLANNING_ONLY + PERSISTENCE_BASELINE_DESIGN + MIGRATION_REVIEW_PREP + QDR_REPLAY_EVALUATION + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
B1 commit: feat(qdr): add replay evaluation domain contracts
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 implementation: NOT_STARTED / NO
stage-qdr-4 B2 freeze/review: READY
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` | PASS | 当前目录为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -12`（开工前） | PASS | 最近 12 条包含 `73b74d2 feat(qdr): add replay evaluation domain contracts`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 whitespace error、tracked diff 或 staged diff。 |
| `docs/current/WORKFLOW.md` | NOT_FOUND / NON_BLOCKING | 项目规范列为事实源入口，但当前仓库不存在该文件；本轮已读取存在的 current factsources。 |
| safety wording scan | REVIEWED / EXISTING_FALSE_POSITIVE_IN_UNMODIFIABLE_FILE | 用户指定 `rg` 已执行；`BUY` / `SELL` / `MARKET_ORDER` / `PLACE_ORDER` / `CANCEL_ORDER` / `MUTATE_NQ_STATE` 命中均为禁止项或风险说明；未发现 real HTTP / provider / LangGraph / Agent / LIVE 被写成启用状态。既有 `docs/current/FACTSOURCE_POLICY.md:62` 命中 credential-storage permissive phrase，上下文是 hard-error phrase 清单，不是允许凭证持久化；该文件不在本轮允许修改清单内，保留为 safety-scan residual。 |
| `git status --short`（收尾） | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于允许的 `docs/current` 文件；新建 `docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md`；无 staged。 |
| `git diff --check`（收尾） | PASS_WITH_EOL_WARNINGS | 无 whitespace error；仅 Git 的 LF -> CRLF warning。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（收尾） | DOCS_ONLY_TRACKED_DIFF / NO_STAGED | tracked diff 限于 `docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`ROADMAP.md`、`STATUS.md`、`TESTING.md`、`WORKLOG.md`、`WORK_ORDER.md`；untracked plan 文件由 `git status --short` 记录；无 staged。 |
| forbidden scope diff | PASS / EMPTY | `dh-domain`、`dh-usecase`、`dh-memory`、`dh-eval`、`dh-connector`、`dh-api`、`dh-app`、`dh-infra`、`contracts`、`golden_cases` diff 均为空。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| Maven settings warning | P2 TOOLING RISK / NON_BLOCKING | 系统 Maven 仍输出 `Unrecognised tag: 'profiles'`，来源 `D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml`；不影响本轮 `BUILD SUCCESS`。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B2 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | migration loads successfully | `V9__qdr_replay_evaluation_baseline.sql` 可由 Flyway 顺序加载。 |
| 2 | repository saves replay case with tenant_id | 保存后 `tenant_id`、`case_id`、`trace_id` 可按 tenant-bound query 查询。 |
| 3 | repository rejects missing tenant_id | 缺失 tenant 直接 fail-closed，不写库。 |
| 4 | repository query is tenant-bound | 所有查询 SQL 必须包含 `tenant_id = ?`。 |
| 5 | repository does not persist raw prompt | raw prompt 样本被拒绝，或仅保存 hash/ref。 |
| 6 | repository does not persist raw provider response | raw provider response 样本被拒绝，或仅保存 hash/ref/summary。 |
| 7 | repository does not persist credential | credential-like 样本被拒绝。 |
| 8 | regression verdict can be saved and queried | verdict 可按 tenant + verdict_id / evaluation_id 查询。 |
| 9 | finding list can be saved and queried | 多 finding 可按 tenant + verdict_id 分页查询。 |
| 10 | duplicate case_id handling is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 11 | cross-tenant read returns empty or fail-closed | tenant B 不得读到 tenant A 数据。 |
| 12 | Flyway migration test passes | migration test 覆盖 V1-V9 顺序加载。 |

Boundary:

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
未修改 NQ
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

## 2026-07-08 DH-STAGE-QDR-4-B1-REPLAY-EVALUATION-DOMAIN-CONTRACTS validation

```text
Task type: IMPLEMENTATION + DOMAIN_CONTRACTS_ONLY + QDR_REPLAY_EVALUATION_BASELINE + NO_MIGRATION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B1_ONLY / DONE
stage-qdr-4 B2: NOT_STARTED / PLAN_ONLY_NEXT
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -12`（开工前） | PASS | `HEAD` 为 `28aac0b docs(qdr): plan stage-qdr-4 replay evaluation baseline`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 whitespace error、tracked diff 或 staged diff。 |
| `docs/current/WORKFLOW.md` | NOT_FOUND / NON_BLOCKING | 项目规范列为事实源入口，但当前仓库不存在该文件；本轮已读取存在的 current factsources。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" test` | EXPECTED_TARGETING_FAILURE | 上游 `dh-common` 无匹配测试，Surefire 报 `No tests matching pattern`；非代码失败。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（首次） | TEST_FIX_REQUIRED | 13 tests 运行，1 error；测试扫描路径按 root 假设，Surefire 在 `dh-usecase` 模块目录执行导致 `NoSuchFileException`。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（第二次） | TEST_FIX_REQUIRED | 13 tests 运行，1 failure；扫描命中生产 Javadoc 中的 `LangGraph` 边界说明，调整为中性 runtime 表述。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（最终） | BUILD SUCCESS | `ReplayEvaluationContractServiceTest` 13 tests，0 failures，0 errors，0 skipped。 |
| `mvn -ntp -pl dh-domain,dh-usecase -am test` | BUILD SUCCESS | Reactor 9/9 SUCCESS；`dh-domain` 151 tests、`dh-connector` 19 tests、`dh-usecase` 308 tests 均 0 failures / 0 errors。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| Maven settings warning | P2 TOOLING RISK / NON_BLOCKING | 系统 Maven 仍输出 `Unrecognised tag: 'profiles'`，来源 `D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml`；不影响本轮 `BUILD SUCCESS`。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\\` is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| required safety wording scan | REVIEWED / FALSE_POSITIVE_ONLY | 用户指定 `rg` 已执行；全量命中大量既有 docs / tests / safety guards。本轮 replay/evaluation 新增包窄范围复核只命中 fail-closed pattern、policy 禁止 raw provider response 说明、ExpectedDecisionSummary 禁止 BUY / SELL 说明，以及禁止测试；未新增真实 HTTP/client/provider runtime、SDK、credential 读取或 raw prompt/raw provider response 持久化。 |

Boundary:

```text
未修改 NQ
未新增 migration
未修改 V1-V8 migration
未新增 API
未新增 Controller
未新增 Repository / JDBC persistence
未新增真实 HTTP client
未新增真实 provider
未新增 Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

## 2026-07-08 DH-STAGE-QDR-4-PLAN validation

```text
Task type: PLANNING_ONLY + STAGE_QDR_4_SCOPE_DESIGN + POST_MODEL_GATEWAY_HARDENING_PLAN + REPLAY_EVAL_PROVIDER_READINESS_REVIEW + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
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

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` | PASS | 当前目录为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | `HEAD` 为 `b3fa637 docs(qdr): record stage-qdr-3 acceptance`。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff，无 staged。 |
| `git status --short`（验证时） | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于 root README 与 `docs/current`；新建 `docs/current/DH_STAGE_QDR_4_PLAN.md`；无 staged。 |
| `git diff --check` | PASS_WITH_EOL_WARNINGS | 无 whitespace error；仅 Git 的 LF -> CRLF warning。 |
| `git diff --stat` | DOCS_ONLY_TRACKED_DIFF | tracked diff 只包含 `README.md` 与 `docs/current` 允许文档；新建未跟踪 plan 文件由 `git status --short` 记录。 |
| `git diff --name-only` | DOCS_ONLY_TRACKED_DIFF | 只列出 root README 与 `docs/current` 已跟踪文档。 |
| `git diff --cached --name-only` | PASS / EMPTY | 无 staged。 |
| required safety wording scan | REVIEWED / FALSE_POSITIVE_ONLY | 仅命中既有 supporting docs：`API.md` line 56 的 Agent 否定状态，以及 `DB_SCHEMA.md` line 138 的跨句 regex 假阳性；二者不在本轮允许修改清单内，且不是实际启用或启动状态。本轮新增/修改允许文档未新增肯定风险表述。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check executed and build passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| Maven settings warning | P2 TOOLING RISK / NON_BLOCKING | 系统 Maven 仍输出 `Unrecognised tag: 'profiles'`，来源 `D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml`；不影响本次 `BUILD SUCCESS`。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\\` is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| Docker/Testcontainers | INHERITED_TOOLING_ENV_RISK / NOT RUN | 本轮 docs-only planning 未运行 Docker/Testcontainers；既有 Docker/Testcontainers 环境型 skip 风险继承，skip 仍不得写成 PASS。 |

Boundary:

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V8 migration
未新增 V9 migration
未新增 API
未新增 Controller
未新增 Repository / Service
未新增真实 HTTP outbound
未新增真实 provider
未接 Provider SDK
未修改 NQ
未接 LangGraph / AutoGen / CrewAI
未开启 LIVE
stage-qdr-4 implementation 未启动
```

## 2026-07-08 DH-STAGE-QDR-3-FINAL-CLOSE-DOCS-SYNC validation

```text
Task type: DOCUMENTATION_ONLY + STAGE_FINAL_CLOSE_RECORD + ACCEPTANCE_RESULT_SYNC + STAGE_QDR_3_CLOSE_RECORD + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/Project/decision-hub
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

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 当前分支 `dev`，起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | HEAD 包含并位于 `b595cc2 docs: align QDR archives under docs gates`。 |
| B5 close review accepted | ACCEPTED / USER_PROVIDED | 用户已提供 `DH-STAGE-QDR-3-B5-CLOSE-REVIEW` ACCEPTED 结论，本轮只写回 current factsources。 |
| Maven scoped tests | PASS / ACCEPTED_EVIDENCE_FROM_B5_CLOSE_REVIEW | B5 close review accepted 结论包含 scoped Maven tests 通过；本轮 docs-sync 未重新运行 scoped tests。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| Maven settings warning | P2 TOOLING RISK / NON_BLOCKING | 系统 Maven 输出 `Unrecognised tag: 'profiles'`，来源 `D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml`，不影响本次 `BUILD SUCCESS`，但继续作为工具配置风险保留。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\\` is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| Docker/Testcontainers | INHERITED_TOOLING_ENV_RISK / NOT RUN | 本轮 docs-only final close sync 未运行 Docker/Testcontainers；既有 Docker/Testcontainers 环境型 skip 风险继承，skip 仍不得写成 PASS。 |

Boundary:

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V8 migration
未新增 V9 migration
未新增 API
未新增 Controller
未新增 Repository / Service
未新增真实 HTTP outbound
未新增真实 provider
未接 Provider SDK
未修改 NQ
未接 LangGraph / AutoGen / CrewAI
未开启 LIVE
stage-qdr-4 implementation 未启动
```

## 2026-07-08 DH-DOCS-GOVERNANCE-GATES-ARCHIVE-FIX validation

```text
Task type: DOCUMENTATION_ONLY + DOCS_GOVERNANCE_CORRECTION + GATES_ARCHIVE_ALIGNMENT + CURRENT_DOCS_CLEANUP + FACTSOURCE_CONSOLIDATION + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/Project/decision-hub
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| workspace path check | PASS_WITH_PATH_CORRECTION | 用户命令块中的 `E:\Project\decision-hub` 不存在；实际工作区为 `F:\project\decision-hub`，本轮未触碰 NQ。 |
| `git status --short`（开工前） | PASS / CLEAN | 当前分支 `dev`，起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -15`（开工前） | PASS | HEAD 为 `767018b feat(gates): add paper shadow consistency drilldown read model`。 |
| `Test-Path docs\gates` / `Test-Path docs\archive`（开工前） | PASS | 两者均存在；`docs/archive` 仅包含上一轮 QDR 误放归档文件。 |
| current docs cleanup | DONE | `docs/current` 已收口为 `README / STATUS / WORK_ORDER / CODEX_PROJECT_INSTRUCTIONS / TESTING / FACTSOURCE_POLICY / ARCHIVE_INDEX / WORKLOG / ROADMAP / API / DB_SCHEMA`。 |
| archive migration | DONE | `docs/archive/stage-qdr-2/*` 已迁移到 `docs/gates/stage-qdr-2/`；`docs/archive/stage-qdr-3/*` 已迁移到 `docs/gates/stage-qdr-3/`；空 `docs/archive` 已清理。 |
| historical residual archive | DONE | `docs/current` 其他历史阶段文档已迁移到 `docs/gates/stage-qdr-3/current-docs-historical-20260708/`。 |
| current residual scan | PASS_WITH_HISTORICAL_MENTIONS | current factsources / supporting docs 仅保留当前状态、archive index 和 historical pointers；`docs/gates` 命中旧状态均为 historical records。 |
| stale facts scan | PASS_WITH_CANONICAL_FALSE_POSITIVES | README 与 `docs/current` 未命中旧 pending-close、旧 QDR3 not-started、旧 model-gateway not-started、旧 QDR2 B5 close-review、旧 B4 review-freeze-required 或任何真实 HTTP / provider / LIVE 肯定状态；canonical `stage-qdr-4: NOT_STARTED` 命中符合预期。 |
| forbidden scope diff | PASS / EMPTY | `dh-domain`、`dh-usecase`、`dh-memory`、`dh-eval`、`dh-connector`、`dh-api`、`dh-app`、`dh-infra`、`contracts`、`golden_cases`、`**/db/migration/**`、`**/*.java` diff 均为空。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| `.\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 命令 exit code 0，但输出仍包含 `'\` is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| Docker/Testcontainers | INHERITED_TOOLING_ENV_RISK / NOT RUN | 本轮 docs-only governance 未运行 Docker/Testcontainers；既有 Docker/Testcontainers skip 风险继承，skip 仍不得写成 PASS。 |

## 2026-07-07 DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE validation

```text
Task type: DOCUMENTATION_ONLY + DOCS_GOVERNANCE + FACTSOURCE_CONSOLIDATION + ARCHIVE_CLEANUP + CURRENT_STATE_INDEXING + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/Project/decision-hub
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 当前分支 `dev`，起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -15`（开工前） | PASS | HEAD 为 `75792f5 docs(qdr): sync stage-qdr-3 codex instructions`。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff，无 staged。 |
| current docs archive operation | SUPERSEDED_BY_GATES_ARCHIVE_FIX | 上一轮曾放入 `docs/archive/**`；本轮已修正为 `docs/gates/**`，详见 2026-07-08 记录。 |
| final `git status --short` | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于 root README、`docs/current`、`docs/gates` 归档文件；无 Java、test、migration、contracts、golden_cases 或 NQ diff；`git diff --cached --name-only` 为空。 |
| final `git diff --check` | PASS_WITH_EOL_WARNINGS | 无 whitespace error；仅 Windows LF -> CRLF warning。 |
| final `git diff --stat` | DOCS_ONLY_TRACKED_DIFF | tracked diff 为 current docs 压缩与 QDR 旧文档从 `docs/current` 移出；untracked archive snapshot 与新 index/policy 由 `git status --short` 记录。 |
| stale facts scan | REVIEWED / ONLY_CANONICAL_FALSE_POSITIVE | 仅命中用户要求保留的 `stage-qdr-4: NOT_STARTED` canonical 状态；未命中旧 pending-close、旧 QDR3 not-started、旧 model-gateway not-started、旧 QDR2 B5 close-review、旧 B4 review-freeze-required、LIVE enabled、real provider enabled 或 real HTTP enabled 语义。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；未使用 `-DskipTests` 或 `-DskipITs`。 |
| `.\mvnw.cmd -v` | WRAPPER_UNUSABLE / NOT_VALID_MAVEN_WRAPPER | 命令 exit code 0，但输出仍包含 `'\` is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| Docker/Testcontainers | INHERITED_TOOLING_ENV_RISK / NOT RUN | 本轮 docs-only governance 未运行 Docker/Testcontainers；既有 Docker/Testcontainers skip 风险继承，skip 仍不得写成 PASS。 |

Boundary:

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V8 migration
未新增 V9 migration
未新增 API
未新增 Controller
未新增 Repository / Service
未新增真实 HTTP outbound
未新增真实 provider
未接 Provider SDK
未修改 NQ
未接 LangGraph / AutoGen / CrewAI
未开启 LIVE
stage-qdr-4 未启动
```
