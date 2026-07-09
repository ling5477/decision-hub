# Decision Hub Testing

> supporting role: current validation evidence
> primary stage gate source: only for actual command results and tooling risk

## 2026-07-09 DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER validation

```text
Task type: WORK_ORDER_ONLY + STAGE_QDR_5_IMPLEMENTATION_PLANNING + MODEL_GATEWAY_OBSERVABILITY_WO + PROVIDER_READINESS_HARDENING_WO + SECURITY_BOUNDARY_DESIGN + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
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
```

Planned Stage-QDR-5 test matrix:

| # | Test item | Expected result |
| --- | --- | --- |
| 1 | valid provider health summary can be created | summary 仅含 tenant/provider/call safe refs、状态、hash、延迟/预算安全字段。 |
| 2 | missing tenantId fails closed | 缺 tenantId 直接拒绝或输出 denied readiness。 |
| 3 | missing providerRef fails closed | 缺 provider safe ref 直接拒绝或输出 denied readiness。 |
| 4 | failure classification supports timeout / budget exceeded / policy denied / source denied / unknown | 分类稳定映射，不泄露原始异常。 |
| 5 | latency budget summary records p50 / p95 / p99 or equivalent safe fields | 只记录安全数值，不代表真实 provider billing。 |
| 6 | trust decision summary records allowed / denied / degraded / skipped | denied / degraded / skipped 不可执行。 |
| 7 | readiness signal cannot enable real provider | 不产生 provider enable flag。 |
| 8 | readiness signal cannot enable real HTTP | 不产生 HTTP enable flag。 |
| 9 | readiness signal cannot enable LIVE | 不产生 LIVE enable flag。 |
| 10 | readiness signal cannot generate trading signal | 不包含 BUY / SELL / PLACE_ORDER / CANCEL_ORDER。 |
| 11 | raw provider response is rejected | raw response 字段或 key 被 redaction boundary 拒绝。 |
| 12 | credential-like key is rejected | token / key / secret / passphrase 类文本被拒绝。 |
| 13 | provider health read model is tenant-bound | 查询必须带 tenantId。 |
| 14 | cross-tenant read returns empty or fail-closed | tenant mismatch 不返回他租户数据。 |
| 15 | providerSummaryHash / modelGatewayVersionRef are safe refs only | 只保存 hash/ref。 |
| 16 | no Provider SDK / HTTP client / Agent / LangGraph classes introduced | architecture / rg scan 无新增禁用类或依赖。 |
| 17 | repository or report failure fails closed | read model/report 失败时输出 denied/skipped 或抛内部可理解异常。 |
| 18 | quality validate passes | `mvn -ntp -Pquality validate` 通过；`mvnw.cmd` 风险如实记录。 |

Validation record:

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` | PASS | 当前目录为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -30`（开工前） | PASS | 包含 `61b2c49 docs(qdr): plan stage-qdr-5 provider readiness hardening`。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS | 本地 tag 存在。 |
| `git rev-list -n 1 dh-stage-qdr-4-close` | PASS | tag peeled target 为 `62c802064f637ad03d3b0f4a185bd55fa3141af2`。 |
| `git ls-remote --tags origin \| rg "dh-stage-qdr-4-close"` | PASS | remote tag 与 peeled target 均存在。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff、staged diff 或 whitespace error。 |
| `git status --short`（文档修改后） | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于 root README 当前入口、`docs/current` 允许文档和新建 WO；无 staged。 |
| `git diff --check`（文档修改后） | PASS_WITH_EOL_WARNINGS | 无 whitespace error；Git 仍提示 LF -> CRLF warning。 |
| `git diff --stat` / `git diff --name-only`（文档修改后） | DOCS_ONLY_TRACKED_DIFF | tracked diff 限于 root README 当前入口与 `docs/current` 允许文档；新建 WO 由 `git status --short` 记录。 |
| `git diff --cached --name-only` | PASS / EMPTY | staged 为空。 |
| forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` 均无 diff。 |
| safety wording scan | REVIEWED / EXISTING_FALSE_POSITIVE_ONLY | 命中来自 `AGENTS.md` / `.agents` / supporting docs 的否定语义、hard-error phrase 和历史证据；本轮新增 WO 未把 provider/HTTP/SDK/Agent/LangGraph/LIVE 写成开启，也未把 raw material 或 credential material 写成许可。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed；系统 Maven settings 仍有 `Unrecognised tag: 'profiles'` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\` is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

Boundary:

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API
未新增 Controller
未新增 Repository / JDBC / Service implementation
未新增真实 HTTP client
未新增真实 provider
未接 Provider SDK
未新增 OpenAI / Anthropic / Gemini / Ollama SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未修改 NQ
未开启 LIVE
未进入 Stage-QDR-5 implementation
未创建 tag
未 push
```

## 2026-07-09 DH-STAGE-QDR-5-PLAN validation

```text
Task type: PLANNING_ONLY + STAGE_QDR_5_SCOPE_DESIGN + POST_QDR_REPLAY_EVALUATION_PLAN + MODEL_GATEWAY_OBSERVABILITY_REVIEW + PROVIDER_READINESS_BOUNDARY_REVIEW + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
branch: dev
current workspace: resolved by Get-Location for this run
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_TAG_CLOSE: DONE
TAG: dh-stage-qdr-4-close
TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION: NOT_STARTED
```

Validation summary:

| Command | Result | Notes |
| --- | --- | --- |
| `Get-Location` | PASS | 当前工作区确认为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -30` | PASS | HEAD 为 `62c8020 docs(workflow): repair documentation discipline and skill policy`。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS | 本地 tag 存在。 |
| `git rev-list -n 1 dh-stage-qdr-4-close` | PASS | `62c802064f637ad03d3b0f4a185bd55fa3141af2`。 |
| `git ls-remote --tags origin | rg "dh-stage-qdr-4-close"` | PASS | 远端 annotated tag 存在，peeled target 为 `62c802064f637ad03d3b0f4a185bd55fa3141af2`。 |
| `git diff --check` | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` | DOCS_ONLY_TRACKED_DIFF | tracked diff 限于 `README.md` 与 `docs/current` 已允许文档；新增 plan 文件由 `git status --short` 记录。 |
| `git diff --name-only` | DOCS_ONLY_TRACKED_DIFF | tracked diff 为 `README.md`、`docs/current/ARCHIVE_INDEX.md`、`CODEX_PROJECT_INSTRUCTIONS.md`、`README.md`、`ROADMAP.md`、`STATUS.md`、`WORK_ORDER.md`。 |
| `git diff --cached --name-only` | PASS / EMPTY | staged 为空。 |
| forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` 无 diff。 |
| safety wording scan | REVIEWED / HISTORICAL_FALSE_POSITIVE_ONLY | 命中来自 `AGENTS.md` / `.agents` 的 `NOT STARTED` 否定语义、tag-created 规则说明、`FACTSOURCE_POLICY.md` hard-error phrase、旧 TESTING/WORKLOG evidence 和 supporting API/DB_SCHEMA 历史段落；本轮修改文件未把 real HTTP/provider/SDK/Agent/LangGraph/LIVE 写成 enabled/started，也未把 credential/raw prompt/raw provider response 写成许可。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | Reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。系统 Maven settings 仍有 `Unrecognised tag: 'profiles'` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` manifest 问题；不得写成 wrapper PASS。 |

Boundary confirmation:

```text
Java production changes: NO
Java test changes: NO
DB migration changes: NO
API / Controller changes: NO
contracts changes: NO
golden_cases changes: NO
NQ changes: NO
tag creation: NO
push: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
Stage-QDR-5 implementation: NOT_STARTED
```

## 2026-07-09 DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION validation

```text
Task type: DOCUMENTATION_CLEANUP_IMPLEMENTATION + WORKFLOW_AUTHORITY_REPAIR + SKILL_POLICY_FIX + ARCHIVE_POLICY_FIX + CURRENT_FACTSOURCE_REPAIR + TOOLING_GUARD_UPDATE + NO_JAVA_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_TAG + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
branch: dev
current workspace: resolved by Get-Location for this run
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
ALLOW_STAGE_QDR_5_PLAN_NOW: NO
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
```

Validation summary:

| Command | Result | Notes |
| --- | --- | --- |
| `Get-Location` | PASS | 当前工作区由命令确认；文档不再把本机盘符路径写成唯一事实。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -30` | PASS | 包含 `3689251 docs(qdr): archive stage-qdr-4 replay evaluation baseline`。 |
| `git diff --check` | PASS_AFTER_FIX | 首次发现 `docs/codex/WORK_ORDER.md` trailing whitespace；已修复后通过。 |
| `git diff --name-only -- dh-domain/src/main dh-usecase/src/main dh-app/src/main dh-infra/src/main contracts golden_cases "dh-*/src/main/resources/db/migration"` | PASS / EMPTY | 未修改 Java production、migration、contracts 或 golden_cases。 |
| safety scan | REVIEWED / CLASSIFIED | `current-valid`: current docs / skills 中的 docs/codex 降权、archive-before-tag、pending tag、review cadence；`historical-only`: docs/gates、docs/codex archive、旧路径与旧阶段快照；`false-positive`: `NOT STARTED`、禁止项、不要写 tag created 的否定语义；`needs-fix`: trailing whitespace，已修复；`actual-risk`: 无。 |
| `mvn -ntp -Pquality validate` | PASS | Reactor 19/19 `BUILD SUCCESS`，0 Checkstyle violations，Spotless check passed；系统 Maven settings 仍有 `Unrecognised tag: 'profiles'` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 shell 报告的 slash command not recognized 与 `.mvn\\wrapper\\maven-wrapper.jar` manifest 问题；不得写成 wrapper PASS。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 本轮未创建 tag。 |
| `git ls-remote --tags origin | rg "dh-stage-qdr-4-close"` | PASS / NO_MATCH | 远端未观察到固定 tag。 |

Boundary confirmation:

```text
Java production changes: NO
Java test changes: NO
DB migration changes: NO
API / Controller changes: NO
contracts changes: NO
golden_cases changes: NO
NQ changes: NO
tag creation: NO
push: NO
Stage-QDR-5: NOT STARTED
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

## 2026-07-09 DH-STAGE-QDR-4-CURRENT-DOCS-CLEANUP validation

```text
Task type: DOCUMENTATION_ONLY + CURRENT_DOCS_CLEANUP + STAGE_QDR_4_ARCHIVED_DOC_REMOVAL + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
reason: Stage-QDR-4 detailed docs are now archived under docs/gates/stage-qdr-4 and should not remain as current docs.
```

Expected current cleanup:

```text
docs/current/DH_STAGE_QDR_4*.md: removed from current
docs/gates/stage-qdr-4/DH_STAGE_QDR_4*.md: retained as archive
tag state: PENDING
```

Actual validation:

| Command | Result | Notes |
| --- | --- | --- |
| `git branch --show-current` | PASS | `dev` |
| `Get-ChildItem docs/current -Filter 'DH_STAGE_QDR_4*.md'` | PASS / EMPTY | `docs/current` 不再保留 Stage-QDR-4 详细 plan / work order 文档。 |
| `Get-ChildItem docs/gates/stage-qdr-4 -Filter 'DH_STAGE_QDR_4*.md'` | PASS / 7 FILES | 7 个 Stage-QDR-4 详细文档保留在归档目录。 |
| `rg -n "docs/current/DH_STAGE_QDR_4\|DH_STAGE_QDR_4_PLAN.md\|DH_STAGE_QDR_4_B[234]_" README.md docs/current/README.md docs/current/STATUS.md docs/current/CODEX_PROJECT_INSTRUCTIONS.md docs/current/FACTSOURCE_POLICY.md docs/current/ARCHIVE_INDEX.md` | PASS / ARCHIVE_POINTERS_ONLY | 未发现 `docs/current/DH_STAGE_QDR_4*.md` 残留；剩余命中均指向 `docs/gates/stage-qdr-4/`。 |
| `git diff --check` | PASS | 无 whitespace error；仅有 Windows LF-to-CRLF 提示。 |
| forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、migration 路径均无 diff。 |
| safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中为 pending tag、`No tag created`、历史 `NOT STARTED`、禁止项、测试守卫或 `FACTSOURCE_POLICY.md` hard-error phrase 清单；未发现 real HTTP/provider/Agent/LangGraph/LIVE enabled/started。 |
| `mvn -ntp -Pquality validate` | PASS | Reactor 19/19 `SUCCESS`；Checkstyle 0 violations；Spotless check passed。 |
| `.\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | 仍输出 `'\` is not recognized` 与 `maven-wrapper.jar` 无主清单属性；不得记录为 PASS。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 本轮未创建 tag。 |
| `git ls-remote --tags origin "refs/tags/dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 远程固定 tag 无匹配输出。 |

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CONTENT-FIX validation

```text
Task type: DOCUMENTATION_ONLY + ARCHIVE_CONTENT_FIX + STAGE_QDR_4_DOCS_GATES_ARCHIVE + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
reason: docs/gates/stage-qdr-4 contained only README.md after archive close; actual Stage-QDR-4 stage docs needed to be present in the archive directory.
```

### Archive content check

```text
docs/gates/stage-qdr-4/README.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

### Validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short` | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于 `docs/current/ARCHIVE_INDEX.md`、`docs/current/TESTING.md`、`docs/current/WORKLOG.md`、`docs/gates/README.md`、`docs/gates/stage-qdr-4/**`。 |
| `Get-ChildItem docs/gates/stage-qdr-4` | PASS | 目录包含 `README.md` 与 7 个 `DH_STAGE_QDR_4*.md` 阶段文档副本。 |
| `git diff --check` | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` / `git diff --name-only` | DOCS_ONLY_DIFF | tracked diff 限于 docs 索引与归档 README；新增 stage docs 由 `git status --short` 记录。 |
| forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` 无 diff。 |
| safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中为 `dh-stage-qdr-4-close` pending tag、`No tag created`、历史 `NOT STARTED`、禁止项或 hard-error phrase 清单；未发现 real HTTP/provider/Agent/LangGraph/LIVE enabled/started。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\' is not recognized` 与 `maven-wrapper.jar` no main manifest attribute；不能写成 wrapper PASS。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 本轮仍未创建 tag。 |

Boundary:

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未打 tag
未 push
```

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CLOSE validation

```text
Task type: DOCUMENTATION_ONLY + STAGE_ARCHIVE_CLOSE + QDR_REPLAY_EVALUATION_REGRESSION_ARCHIVE + TAG_PREP + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
B4 implementation commit: b04408a feat(qdr): add regression report read model support
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_CLOSE: YES
ALLOW_STAGE_QDR_5_PLAN: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Archive close validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -30` | PASS | 包含 `b04408a feat(qdr): add regression report read model support` 与 `b771c13 docs(qdr): close stage-qdr-4 replay evaluation baseline`。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 本地固定 tag 不存在。 |
| `git ls-remote --tags origin | rg "dh-stage-qdr-4-close"` | NO_MATCH_AFTER_RETRY | 首次遇到 Windows `SEC_E_NO_CREDENTIALS`，使用本机 Git 凭据重试后无匹配输出；未发现远程固定 tag。 |
| `git diff --check`（写入后） | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` / `git diff --name-only`（写入后） | DOCS_ONLY_DIFF | tracked diff 限于 README、docs/current 与 docs/gates 归档文档。 |
| `git diff --cached --name-only`（写入后） | PASS / EMPTY | 无 staged 文件。 |
| forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-app/src/main`、`dh-infra/src/main`、`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` 无 diff。 |
| required safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中限定在禁止项、历史 guard/test 说明、`NOT STARTED`、`PENDING`、`No tag created` 和 `next tag`；未把 real HTTP/provider/Agent/LangGraph/LIVE 写成 enabled/started，未把 `dh-stage-qdr-4-close` 写成已创建。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | PASS / PRIOR_FINAL_CLOSE_EVIDENCE | Stage-QDR-4 final close review 已记录 BUILD SUCCESS；`V9QdrReplayEvaluationFlywayPostgresTest` 非 skip，PostgreSQL 17 Testcontainers 启动，Flyway validated 9 migrations 并迁移到 v9。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `V9 PostgreSQL/Testcontainers/Flyway load` | PASS / NOT_SKIPPED | 来自 final close review evidence；V9 PostgreSQL/Flyway load 不再按 skip/blocker 处理。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\' is not recognized` 与 `maven-wrapper.jar` no main manifest attribute；不能写成 wrapper PASS。 |

Boundary:

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

## 2026-07-08 DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW validation

```text
Task type: REVIEW_ONLY + STAGE_FINAL_CLOSE + QDR_REPLAY_EVALUATION_REGRESSION_ACCEPTANCE + TAG_PREP + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
B4 implementation commit: b04408a feat(qdr): add regression report read model support
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_COMMIT: YES
ALLOW_STAGE_QDR_5_PLAN: YES
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Final close validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -30` | PASS | 包含 `b04408a feat(qdr): add regression report read model support`。 |
| `git tag --list "dh-stage-qdr-4-close"` | PASS / NOT_EXISTS | 本地固定 tag 不存在。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 无 whitespace error、tracked diff 或 staged diff。 |
| B1 evidence review | PASS | Replay / Evaluation domain contracts 存在，`ReplayEvaluationContractService` fail-closed，BUY / SELL / MARKET_ORDER 与 PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 均有拒绝测试；无 API / migration / provider / HTTP / Agent / LangGraph / LIVE。 |
| B2 evidence review | PASS | `V9__qdr_replay_evaluation_baseline.sql` 存在，7 张 B2 表、tenant_id、tenant indexes、CHECK、COMMENT、redaction/trading guard 完整；repository ports 与 JDBC adapters tenant-bound；raw prompt / raw provider response / credential 不入库；B2 close review 已 PASS。 |
| B3 evidence review | PASS | deterministic mock gateway regression flow 已实现；comparator 覆盖 decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion；verdict 仅 PASS / WARN / FAIL / SKIPPED；复用 B2 repository；B3 close review 已 PASS。 |
| B4 evidence review | PASS | `RegressionReportQuery`、`RegressionReportView`、`RegressionReportFindingView`、`RegressionDriftSummary`、`RegressionReadModelService` 已存在；tenant-bound query、pagination/pageSize guard、redacted report content 与 drift summary 已实现。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | BUILD SUCCESS | Reactor 15/15 SUCCESS；`V9QdrReplayEvaluationFlywayPostgresTest` 非 skip，PostgreSQL 17 Testcontainers 启动，Flyway validated 9 migrations 并迁移到 v9。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\' is not recognized` 与 `maven-wrapper.jar` no main manifest attribute；不能写成 wrapper PASS。 |
| required safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中限定在禁止项、测试守卫、redaction guard、migration CHECK/COMMENT、文档风险说明和 mock/test fixtures；未发现真实 HTTP/provider/Agent/LangGraph/LIVE 实现、raw material 持久化或 trading signal。 |
| `docs/current/WORKFLOW.md` | NOT_FOUND / NON_BLOCKING | 项目规范仍列为事实源入口，但当前仓库不存在该文件；本轮读取已存在 current factsources。 |

Boundary:

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

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION validation

```text
Task type: DIRTY_WORKTREE_TRIAGE + B4_IMPLEMENTATION_RESUME + REGRESSION_REPORT_READ_MODEL + QDR_REPLAY_EVALUATION_REPORTING + INTERNAL_READ_MODEL + TESTS + NO_DB_MIGRATION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B3: CLOSED / ACCEPTED
STAGE_QDR_4_B4_PLAN: DONE
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
STAGE_QDR_4_B4_DIRTY_SCOPE: ACCEPTED
STAGE_QDR_4_B4_IMPLEMENTATION: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
Stage-QDR-4 final close: NOT_STARTED
Stage-QDR-4 tag: NOT_CREATED
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_4_TAG_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Implementation validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short` / `git ls-files --others --exclude-standard`（dirty triage） | DIRTY_SCOPE_ACCEPTED_FOR_B4_RESUME | dirty 限于 `docs/current/**`、`dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**`、`dh-usecase/src/test/java/**`；无 staged 文件。 |
| `git branch --show-current`（dirty triage） | PASS | `dev`。 |
| `git log --oneline -20`（dirty triage） | PASS | 最近 20 条包含 `b5718e7 docs(qdr): define regression report read model work order`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（dirty triage） | PASS / ALLOWED_DIRTY_ONLY | 无 whitespace error；tracked diff 限于允许的 `docs/current` 文件；staged diff 为空；untracked B4 Java/test 文件由 `git status --short` 记录。 |
| `mvn -ntp -pl dh-usecase -am -Dtest=RegressionReadModelServiceTest test`（目标测试初次） | TOOLING_PATTERN_RETRY | reactor 上游模块无匹配测试导致 surefire fail；使用 `-Dsurefire.failIfNoSpecifiedTests=false` 重新运行。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=RegressionReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（草稿核验） | BUILD SUCCESS | `RegressionReadModelServiceTest` 14 tests，0 failures，0 errors，0 skipped。 |
| B4 pagination fix | DONE | `caseId` 查询路径把 `offset` 传给 `EvaluationCaseRepository.listByCaseId`，并新增同 case 多 evaluation 分页断言。 |
| `mvn -ntp -pl dh-usecase -am "-Dtest=RegressionReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（最终） | BUILD SUCCESS | `RegressionReadModelServiceTest` 15 tests，0 failures，0 errors，0 skipped。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | BUILD SUCCESS | Reactor 15/15 SUCCESS；包含 `RegressionReadModelServiceTest`、ArchUnit、API WebMvc、Testcontainers PostgreSQL 17 和 Flyway V9 load。 |
| `V9QdrReplayEvaluationFlywayPostgresTest` | PASS / POSTGRES_FLYWAY_VERIFIED | PostgreSQL 17 Testcontainer 中 Flyway validated 9 migrations，并成功迁移到 version v9。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\' is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| required safety scan | REVIEWED / ALLOWED_HITS_ONLY | 全量命中限定在禁止项、测试守卫、redaction/trading guard、migration CHECK/COMMENT、文档风险说明和既有 no-runtime 注释；本轮新增文件聚焦扫描只命中 Javadoc 禁止项说明与测试 guard 断言；未发现真实 HTTP/provider/Agent/LangGraph/LIVE 实现。 |
| forbidden-scope dirty check | PASS / EMPTY | `dh-app/src/main/resources/db/migration`、`dh-api`、`dh-infra/src/main/java`、`dh-app/src/main/java`、`contracts`、`golden_cases` 无 diff / untracked。 |

### B4 implementation coverage result

```text
RegressionReportQuery: DONE / tenantId required / no UUID-only selector / limit 1..100
RegressionReportView: DONE / safe refs + redacted summary + hash/version/verdict/finding/drift only
RegressionReportFindingView: DONE / safe finding code/message/evidence ref only
RegressionDriftSummary: DONE / decision/action/confidence/risk/evidence/forbidden/hash/model/prompt/policy drift
RegressionReadModelService: DONE / internal usecase read model / B2 repository port composition
tenant-bound query by caseId: PASS
tenant-bound query by evaluationId: PASS
tenant-bound query by verdictId: PASS
tenantless query: FAIL-CLOSED
UUID-only repository path: ABSENT / NOT CALLED
cross-tenant read: EMPTY / FAIL-CLOSED
list pagination: PASS
pageSize > 100: REJECTED
raw prompt exposure: NOT EXPOSED
raw provider response exposure: NOT EXPOSED
credential-like field exposure: NOT EXPOSED
providerSummaryHash drift: PASS
modelGatewayVersionRef drift: PASS
promptVersionRef drift: PASS
policyVersion drift: PASS
trading executable action exposure: REJECTED / NOT EXPOSED
provider/HTTP/Agent/LangGraph class introduction: NOT INTRODUCED
report generation failure: FAIL-CLOSED
quality validate: PASS
```

Boundary:

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

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO validation

```text
Task type: WORK_ORDER_ONLY + B4_IMPLEMENTATION_BOUNDARY_DESIGN + REGRESSION_REPORT_READ_MODEL_WO + QDR_REPLAY_EVALUATION_REPORTING + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B3: CLOSED / ACCEPTED
STAGE_QDR_4_B4_PLAN: DONE
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
STAGE_QDR_4_B4_IMPLEMENTATION: NOT_STARTED
Stage-QDR-4 final close: NOT_STARTED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_FINAL_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | 最近 20 条包含 B1/B2/B3/B4 plan required commits；HEAD 为 `3c1db84 docs(qdr): plan regression report read model support`。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff 或 staged diff。 |
| B4 implementation WO document | DONE / DOCS_ONLY | 新增 `docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md`；只记录 work order，不写实现。 |
| `git status --short`（写入后） | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于允许的 `docs/current` 文件；新建 B4 WO 文件未 staged。 |
| `git diff --check`（写入后） | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` / `git diff --name-only`（写入后） | DOCS_ONLY_TRACKED_DIFF | tracked diff 限于允许的 `docs/current` 文件；新 WO 文件由 `git status --short` 记录。 |
| `git diff --cached --name-only`（写入后） | PASS / EMPTY | 无 staged 文件。 |
| forbidden-scope diff | PASS / EMPTY | `git diff --name-only -- dh-domain/src/main dh-usecase/src/main dh-app/src/main dh-infra/src/main contracts golden_cases "dh-*/src/main/resources/db/migration"` 无输出。 |
| safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中为 `B4 implementation: NOT_STARTED` 子串假阳性、BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 禁止项、既有 docs guard、既有 FACTSOURCE_POLICY hard-error phrase residual；未发现 real HTTP/provider/Agent/LangGraph/LIVE started/enabled，未发现 hard-error 清单之外的 sensitive-data permissive statement。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\\' is not recognized` 与 `.mvn\\wrapper\\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B4 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | report query by tenantId + caseId succeeds | 返回同租户 replay/evaluation/verdict/report summary。 |
| 2 | report query by tenantId + evaluationId succeeds | 返回同租户 evaluation report。 |
| 3 | report query by tenantId + verdictId succeeds | 返回同租户 verdict report 与 finding list。 |
| 4 | tenantless query fails closed | 缺失 tenantId 拒绝，不查库。 |
| 5 | UUID-only query is absent or rejected | 不提供 UUID-only method；若传入仅 UUID 则拒绝。 |
| 6 | cross-tenant report read returns empty or fail-closed | tenant B 不能读取 tenant A report。 |
| 7 | list query is paginated | list by createdAt/verdict/severity/trace/request/decision 都显式分页。 |
| 8 | pageSize > 100 is rejected or capped | implementation 明确 rejected 或 capped 并测试。 |
| 9 | report does not expose raw prompt | view / JSON / summary 不包含 raw prompt。 |
| 10 | report does not expose raw provider response | view / JSON / summary 不包含 raw provider response。 |
| 11 | report does not expose credential-like fields | credential-like keys 被拒绝或不出现在 view。 |
| 12 | drift summary includes providerSummaryHash mismatch | providerSummaryHash drift 有结构化分类。 |
| 13 | drift summary includes modelGatewayVersionRef mismatch | modelGatewayVersionRef drift 有结构化分类。 |
| 14 | drift summary includes promptVersionRef mismatch | promptVersionRef drift 有结构化分类。 |
| 15 | drift summary includes policyVersion mismatch | policyVersion drift 有结构化分类。 |
| 16 | BUY / SELL / MARKET_ORDER are not exposed as executable action | 它们只可作为 forbidden/prohibited evidence。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE are not exposed as allowed action | 它们不得进入 allowed action。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | safety scan 和 architecture test 证明未新增 runtime。 |
| 19 | report generation failure fails closed | repository/query/drift aggregation failure 不返回 partial unsafe report。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` BUILD SUCCESS。 |

Boundary:

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
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN validation

```text
Task type: PLANNING_ONLY + REGRESSION_REPORT_READ_MODEL_PLAN + QDR_REPLAY_EVALUATION_REPORTING + READ_MODEL_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B3: CLOSED / ACCEPTED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | 最近 20 条包含 B1/B2/B3 required commits；HEAD 为 `e237504 docs(qdr): close mock gateway regression integration`。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff 或 staged diff。 |
| `docs/current/WORKFLOW.md` | NOT_FOUND / NON_BLOCKING | AGENTS 仍列为事实源入口，但当前仓库不存在该文件；本轮读取已存在 current factsources。 |
| B4 plan document | DONE / DOCS_ONLY | 新增 `docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md`；只记录 plan，不写实现。 |
| `git status --short`（写入后） | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于允许的 `docs/current` 文件；新建 B4 plan 文件未 staged。 |
| `git diff --check`（写入后） | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` / `git diff --name-only`（写入后） | DOCS_ONLY_TRACKED_DIFF | tracked diff 限于 `CODEX_PROJECT_INSTRUCTIONS.md`、`ROADMAP.md`、`STATUS.md`、`TESTING.md`、`WORKLOG.md`、`WORK_ORDER.md`；新 plan 文件由 `git status --short` 记录。 |
| `git diff --cached --name-only`（写入后） | PASS / EMPTY | 无 staged 文件。 |
| forbidden-scope diff | PASS / EMPTY | `git diff --name-only -- dh-domain/src/main dh-usecase/src/main dh-app/src/main dh-infra/src/main contracts golden_cases "dh-*/src/main/resources/db/migration"` 无输出。 |
| safety scan | REVIEWED / ALLOWED_HITS_ONLY | 命中为 `B4 implementation: NOT_STARTED` 子串假阳性、BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 禁止项、既有 docs guard、既有 FACTSOURCE_POLICY hard-error phrase residual；未发现 real HTTP/provider/Agent/LangGraph/LIVE started/enabled，未发现 hard-error 清单之外的 sensitive-data permissive statement。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B4 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | report query by tenantId + caseId succeeds | 返回同租户 replay/evaluation/verdict/report summary。 |
| 2 | report query by tenantId + evaluationId succeeds | 返回同租户 evaluation report。 |
| 3 | report query by tenantId + verdictId succeeds | 返回同租户 verdict report 与 finding list。 |
| 4 | tenantless query fails closed | 缺失 tenantId 拒绝，不查库。 |
| 5 | UUID-only query is absent or rejected | 不提供 UUID-only method；仅 UUID 查询被拒绝。 |
| 6 | cross-tenant report read returns empty or fail-closed | tenant B 不能读取 tenant A report。 |
| 7 | list query is paginated | list by createdAt/verdict/severity/trace/request/decision 显式分页。 |
| 8 | pageSize > 100 is rejected or capped | implementation WO 明确 rejected 或 capped 并测试。 |
| 9 | report does not expose raw prompt | view / JSON / summary 不包含 raw prompt。 |
| 10 | report does not expose raw provider response | view / JSON / summary 不包含 raw provider response。 |
| 11 | report does not expose credential-like fields | credential-like keys 被拒绝或不出现在 view。 |
| 12 | drift summary includes providerSummaryHash mismatch | providerSummaryHash drift 有结构化分类。 |
| 13 | drift summary includes modelGatewayVersionRef mismatch | modelGatewayVersionRef drift 有结构化分类。 |
| 14 | drift summary includes promptVersionRef mismatch | promptVersionRef drift 有结构化分类。 |
| 15 | drift summary includes policyVersion mismatch | policyVersion drift 有结构化分类。 |
| 16 | BUY / SELL / MARKET_ORDER are not exposed as executable action | 它们只可作为 forbidden/prohibited evidence。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE are not exposed as allowed action | 它们不得进入 allowed action。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | safety scan 和 architecture test 证明未新增 runtime。 |
| 19 | report generation failure fails closed | repository/query/drift aggregation failure 不返回 partial unsafe report。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` BUILD SUCCESS。 |

Boundary:

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
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW validation

```text
Task type: REVIEW_ONLY + CLOSE_REVIEW + MOCK_GATEWAY_REGRESSION_REVIEW + QDR_PIPELINE_REVIEW + REPOSITORY_REUSE_REVIEW + REDACTION_REVIEW + TRADING_TERM_GUARD_REVIEW + TEST_EVIDENCE_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
B3 implementation commit: 54e5575 feat(qdr): integrate mock gateway regression baseline
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Close review validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -20` | PASS | HEAD 为 `54e5575 feat(qdr): integrate mock gateway regression baseline`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 无 whitespace error、tracked diff 或 staged diff。 |
| B3 implementation commit check | PASS | `git show --name-only --format='%h %s' 54e5575` 只包含 B3 qdr replay Java/test 文件与允许的 `docs/current` 同步文件。 |
| target flow review | PASS | flow 为 dry-run / QDR artifact -> mock gateway safe summary -> replay case -> evaluation case -> expected/actual summary -> comparator -> regression verdict -> findings；未调用真实 provider、HTTP、NQ、replay API、Controller、Agent 或 LangGraph。 |
| usecase/service review | PASS | `tenantId`、`traceId`、`requestId`、`decisionId`、safe hash/ref 均强制校验；repository failure 返回 FAIL / BLOCKER；redaction guard 与 trading-term guard 生效。 |
| comparator review | PASS | 覆盖 decisionType、actionLabel、confidenceBand、riskLevel、evidenceRefs、forbiddenActions、providerSummaryHash、modelGatewayVersionRef、promptVersionRef、policyVersion；结果只允许 PASS / WARN / FAIL / SKIPPED。 |
| persistence reuse review | PASS | 复用 `ReplayCaseRepository`、`EvaluationCaseRepository`、`RegressionVerdictRepository` 与 V9 七张表；无 V10、无 V9 修改、无新表、无 UUID-only query、无 cross-tenant read。 |
| redaction review | PASS | raw prompt、raw provider response、credential/token/cookie/apiKey/apiSecret/passphrase/secret 仅作为禁止项、guard 或测试样本出现；不入库。 |
| trading-term review | PASS | BUY / SELL / MARKET_ORDER 不可作为 expected actionLabel；PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 只允许作为 forbiddenActions 或 guard/test；RegressionVerdict 不是交易建议。 |
| required safety scan | REVIEWED / ALLOWED_HITS_ONLY | 用户指定 `rg` 已执行；命中为禁止项、guard/test、migration CHECK/COMMENT、docs 风险说明或 fixture forbiddenActions；未发现真实 HTTP/provider/Agent/LangGraph/LIVE 实现或 raw material 持久化。 |
| targeted B3 tests | BUILD SUCCESS / PASS | `mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；19 tests，0 failures，0 errors，0 skipped。 |
| broader relevant tests | BUILD SUCCESS / PASS / NOT_SKIPPED | `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test`；reactor 15/15 SUCCESS；Testcontainers PostgreSQL 17 启动，Flyway validated/applied 9 migrations 到 v9。 |
| quality validate | BUILD SUCCESS | `mvn -ntp -Pquality validate`；reactor 19/19 SUCCESS；root Checkstyle 0 violations；Spotless check passed。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### Close review result

```text
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
```

Boundary:

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

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION validation

```text
Task type: IMPLEMENTATION + MOCK_GATEWAY_REGRESSION + QDR_REPLAY_EVALUATION_REGRESSION + PIPELINE_INTEGRATION + TESTS + NO_DB_MIGRATION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
branch: dev
B2 status: CLOSED / ACCEPTED
B3 plan: DONE / PLAN_ONLY
B3 implementation work order: DONE / WORK_ORDER_ONLY
B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
B4 implementation: NOT_STARTED
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Implementation validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工复核） | EXPECTED_DIRTY / B3_ALLOWED_SCOPE | dirty/untracked 范围仅包含 B3 implementation 允许的 qdr replay Java/test 文件与允许同步的 `docs/current` 文件；无 staged 文件。 |
| `git branch --show-current` | PASS | `dev`。 |
| `git log --oneline -20` | PASS | 包含 B1/B2/B3 WO 所需 commits：`feat(qdr): add replay evaluation domain contracts`、B2 plan/WO/implementation/close commits、`docs(qdr): define mock gateway regression integration work order`。 |
| `git diff --check`（开工复核） | PASS_WITH_EOL_WARNINGS | exit code 0；仅 Git LF -> CRLF warning，不是 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工复核） | REVIEWED / NO_STAGED | tracked diff 为允许的 `docs/current` 文件；untracked B3 Java/test 文件由 `git status --short` 记录；无 staged。 |
| B3 code review | PASS | `MockGatewayRegressionCaseBuilder`、`QdrRegressionEvaluationService`、`QdrRegressionComparator`、`RegressionBaselinePolicy`、`RegressionEvidenceRef` 已复核；只做本地 deterministic comparison 与 B2 repository port persistence command 编排。 |
| B2 persistence reuse | PASS | 复用 `ReplayCaseRepository`、`EvaluationCaseRepository`、`RegressionVerdictRepository`；未新增 table/schema，未修改 V9，未绕过 tenant-bound repository port。 |
| targeted B3 tests | BUILD SUCCESS / PASS | `mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；19 tests，0 failures，0 errors，0 skipped。 |
| scoped reactor tests | BUILD SUCCESS / PASS / NOT_SKIPPED | `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test`；reactor 15/15 SUCCESS；`dh-app` 执行 Testcontainers PostgreSQL 17，Flyway successfully validated 9 migrations，并迁移到 version v9。 |
| quality validate | BUILD SUCCESS | `mvn -ntp -Pquality validate`；reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。 |
| required safety scan | REVIEWED / ALLOWED_HITS_ONLY | 用户指定 `rg` 已执行。命中为禁止项说明、docs 风险声明、redaction/trading guard、测试守卫、migration CHECK/COMMENT 或既有 contract guard；未发现真实 HTTP/provider/Agent/LangGraph/LIVE 实现、raw prompt/raw provider response/credential 持久化或 trading signal 输出。 |
| B3 narrow runtime scan | PASS | 新增 B3 Java/test 文件只命中 guard、测试守卫或否定边界注释；未新增 `HttpClient`、`WebClient`、`RestTemplate`、`OkHttp`、Controller、Provider SDK、OpenAI/Anthropic/Gemini/Ollama SDK、LangGraph/AutoGen/CrewAI runtime。 |
| migration/API diff check | PASS / EMPTY | `git diff --name-only -- dh-app/src/main/resources/db/migration dh-api dh-app/src/main/java dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr` 无输出；未新增 migration/API/Controller/JDBC schema drift。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B3 implementation coverage

| 序号 | 测试项 | 实际覆盖 |
| --- | --- | --- |
| 1 | mock gateway output can create replay case | `QdrRegressionEvaluationServiceTest.mockGatewayOutputCreatesReplayEvaluationAndVerdictThroughRepositories`。 |
| 2 | replay case can create evaluation case | 同一 flow 断言 replay/evaluation record 均保存且 caseId 绑定一致。 |
| 3 | expected summary can create regression verdict | `expectedSummaryCanCreateRegressionVerdictAndFinding`。 |
| 4 | identical mock output returns PASS | `QdrRegressionComparatorTest.identicalMockOutputReturnsPass`。 |
| 5 | confidence drift within tolerance returns PASS or WARN | `confidenceDriftWithinToleranceReturnsWarnWhenPolicyRequestsFinding`。 |
| 6 | confidence drift beyond tolerance returns WARN or FAIL | `confidenceDriftBeyondToleranceReturnsFailUnderStrictPolicy`。 |
| 7 | risk level drift returns WARN or FAIL | `riskLevelIncreaseReturnsFailUnderStrictPolicy`。 |
| 8 | missing evidence ref returns FAIL | `missingEvidenceRefReturnsFail`。 |
| 9 | missing forbidden action returns FAIL | `missingForbiddenActionReturnsFail`。 |
| 10 | provider summary hash mismatch returns WARN or FAIL | `providerSummaryHashMismatchReturnsFailUnderStrictPolicy`。 |
| 11 | modelGatewayVersionRef mismatch is recorded | `modelAndPromptVersionMismatchAreRecordedAsFindings`。 |
| 12 | promptVersionRef mismatch is recorded | `modelAndPromptVersionMismatchAreRecordedAsFindings`。 |
| 13 | policyVersion mismatch is recorded or SKIPPED | `policyVersionMismatchCanSkipComparison`。 |
| 14 | BUY / SELL / MARKET_ORDER expected action fails closed | `executableExpectedActionFailsAtBoundary`。 |
| 15 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE allowed action fails closed | `executableAllowedActionFailsAtBoundary`。 |
| 16 | raw prompt is not persisted | `rawPromptAndRawProviderResponseAreRejectedBeforePersistence`。 |
| 17 | raw provider response is not persisted | `rawPromptAndRawProviderResponseAreRejectedBeforePersistence`。 |
| 18 | credential-like JSON key fails closed | `credentialLikeJsonKeyFailsClosedBeforePersistence`。 |
| 19 | cross-tenant regression read returns empty or fail-closed | `crossTenantRegressionReadReturnsEmpty`。 |
| 20 | provider/HTTP/Agent/LangGraph classes are not introduced | `providerHttpAgentAndLangGraphClassesAreNotIntroduced` + narrow `rg` scan。 |
| 21 | repository save failure returns FAIL / BLOCKED | `repositorySaveFailureReturnsFailBlocked`。 |
| 22 | quality validate passes | `mvn -ntp -Pquality validate` BUILD SUCCESS。 |

### B3 implementation result

```text
STAGE_QDR_4_B3_IMPLEMENTATION: DONE
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW
```

Boundary:

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

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO validation

```text
Task type: WORK_ORDER_ONLY + B3_IMPLEMENTATION_BOUNDARY_DESIGN + MOCK_GATEWAY_REGRESSION_WO + QDR_REPLAY_EVALUATION_REGRESSION + PIPELINE_INTEGRATION_WO + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
B3 plan: DONE / PLAN_ONLY
B3 implementation work order: DONE / WORK_ORDER_ONLY
B3 implementation: NOT_STARTED
B4 implementation: NOT_STARTED
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | 最近提交包含 B1 domain contracts、B2 plan、B2 WO、B2 implementation 与 B2 close review commits。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff 或 staged diff。 |
| B3 plan doc | PASS / EXISTS | `docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md` 存在。 |
| B3 WO document | DONE / DOCS_ONLY | 新增 `docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md`；只记录 work order，不写实现。 |
| forbidden-scope diff | PASS / EMPTY | `git diff --name-only -- dh-domain/src/main dh-usecase/src/main dh-app/src/main dh-infra/src/main contracts golden_cases "dh-*/src/main/resources/db/migration"` 无输出，确认未修改 Java/main、contracts、golden_cases 或 migration。 |
| safety scan | REVIEWED / NO_ACTUAL_RISK | 用户指定 `rg` 已执行。命中为禁止项、trading-term guard、B3 docs boundary、既有 `FACTSOURCE_POLICY.md` hard-error phrase 清单或 `NOT_STARTED` false positive；未发现 real HTTP/provider/Agent/LangGraph/LIVE started/enabled，也未将 sensitive data storage 写成 permissive。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。系统 Maven settings 仍有 `profiles` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B3 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | mock gateway output can create replay case | gateway safe refs 能生成 replay case。 |
| 2 | replay case can create evaluation case | replay case 能生成 evaluation case。 |
| 3 | expected summary can create regression verdict | expected/actual summary 能生成 verdict。 |
| 4 | identical mock output returns PASS | deterministic output 完全一致时 `PASS`。 |
| 5 | confidence drift beyond tolerance returns WARN or FAIL | soft drift `WARN`，hard drift `FAIL`。 |
| 6 | risk level drift returns WARN or FAIL | tolerance 内 `WARN`，越界 `FAIL`。 |
| 7 | missing evidence ref returns FAIL | required evidence 缺失 fail-closed。 |
| 8 | provider summary hash mismatch returns WARN or FAIL | structured fields 一致为 `WARN`，不一致为 `FAIL`。 |
| 9 | modelGatewayVersionRef mismatch is recorded | version drift 必须生成 finding。 |
| 10 | promptVersionRef mismatch is recorded | prompt drift 必须生成 finding。 |
| 11 | policyVersion mismatch is recorded | strict default 下为 `FAIL` 或 `BLOCKED` finding。 |
| 12 | BUY / SELL / MARKET_ORDER expected action fails closed | 不允许作为 expected actionLabel。 |
| 13 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE allowed action fails closed | 不允许作为 allowed action。 |
| 14 | raw prompt is not persisted | raw prompt sample 被 guard 拒绝或仅 hash/ref。 |
| 15 | raw provider response is not persisted | raw provider response sample 被 guard 拒绝或仅 hash/ref。 |
| 16 | credential-like JSON key fails closed | sensitive key 命中 fail-closed。 |
| 17 | cross-tenant regression read returns empty or fail-closed | tenant B 不得读 tenant A output。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | architecture/safety scan 证明无新增真实 provider/HTTP/Agent/LangGraph。 |
| 19 | repository save failure returns FAIL / BLOCKED | persistence failure 不 fallback success。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |
| 21 | replay/regression output cannot become trading signal | verdict/finding 不生成 NQ command、order、risk、ledger、paper/live mutation。 |
| 22 | B2 schema is reused without migration drift | no V10、no V9 modification、no new table。 |

### B3 WO result

```text
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN validation

```text
Task type: PLANNING_ONLY + MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN + QDR_REPLAY_EVALUATION_REGRESSION + PIPELINE_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
B3 plan: DONE / PLAN_ONLY
B3 implementation now: NO
B4 implementation now: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Docs-only validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -20`（开工前） | PASS | 最近提交包含 `a3839b2 docs(qdr): close replay evaluation persistence baseline`、`3fe1bab feat(qdr): persist replay evaluation baseline`、`4dc2944 docs(qdr): define replay evaluation persistence implementation work order`、`6a43256 docs(qdr): plan replay evaluation persistence baseline`、`73b74d2 feat(qdr): add replay evaluation domain contracts`。 |
| `git diff --check`（开工前） | PASS | 无 whitespace error。 |
| `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 tracked diff 或 staged diff。 |
| code path discovery | REVIEWED / EXISTING_PATHS_USED | 附件列出的 `usecase/modelgateway` 与 `usecase/provider` 目录不存在；实际 mock gateway / provider package 为 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**`。 |
| B3 plan document | DONE | 新增 `docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md`；只记录 plan，不写实现。 |
| forbidden-scope diff | PASS / EMPTY | `git diff --name-only -- dh-domain/src/main dh-usecase/src/main dh-app/src/main dh-infra/src/main contracts golden_cases "dh-*/src/main/resources/db/migration"` 无输出，确认未修改 Java/main、contracts、golden_cases 或 migration。 |
| safety scan | REVIEWED / NO_ACTUAL_RISK | 用户指定 `rg` 已执行。命中为禁止项、B3 plan guard、既有 DB/API risk statement 或 `FACTSOURCE_POLICY.md:62` hard-error phrase 清单；未发现 real HTTP/provider/Agent/LangGraph/LIVE started/enabled，也未将 raw prompt/provider response/credential 写成许可。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。系统 Maven settings 仍有 `profiles` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |

### B3 implementation test matrix

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | mock gateway output can create replay case | gateway safe refs 能生成并保存 replay case。 |
| 2 | replay case can create evaluation case | replay case + actual summary/output ref 能生成 evaluation case。 |
| 3 | expected summary can create regression verdict | expected/actual summary comparison 能保存 verdict。 |
| 4 | identical mock output returns PASS | deterministic output 与 baseline 完全一致时 `PASS`。 |
| 5 | confidence drift beyond tolerance returns WARN or FAIL | soft drift `WARN`，hard drift `FAIL`。 |
| 6 | risk level drift returns WARN or FAIL | policy tolerance 内 `WARN`，越界 `FAIL`。 |
| 7 | missing evidence ref returns FAIL | required evidence 缺失 fail-closed。 |
| 8 | provider summary hash mismatch returns WARN or FAIL | 结构化字段一致为 `WARN`，语义不一致为 `FAIL`。 |
| 9 | modelGatewayVersionRef mismatch is recorded | drift 必须生成 finding，不静默忽略。 |
| 10 | promptVersionRef mismatch is recorded | drift 必须生成 finding，不静默忽略。 |
| 11 | policyVersion mismatch is recorded | strict default 下为 `FAIL` 或 `BLOCKED` finding。 |
| 12 | BUY / SELL / MARKET_ORDER expected action fails closed | 不允许作为 expected actionLabel。 |
| 13 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE allowed action fails closed | 不允许作为 allowed action。 |
| 14 | raw prompt is not persisted | raw prompt sample 被 guard 拒绝或仅 hash/ref。 |
| 15 | raw provider response is not persisted | raw provider response sample 被 guard 拒绝或仅 hash/ref。 |
| 16 | credential-like JSON key fails closed | sensitive key 命中 fail-closed。 |
| 17 | cross-tenant regression read returns empty or fail-closed | tenant B 不得读 tenant A case/evaluation/verdict/finding。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | architecture/safety scan 证明无新增真实 provider/HTTP/Agent/LangGraph。 |
| 19 | repository save failure returns FAIL / BLOCKED | persistence failure 不吞异常，不 fallback success。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |

### B3 plan result

```text
STAGE_QDR_4_B3_PLAN: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW validation

```text
Task type: REVIEW_ONLY + CLOSE_REVIEW + MIGRATION_REVIEW + REPOSITORY_REVIEW + TENANT_ISOLATION_REVIEW + REDACTION_REVIEW + TEST_EVIDENCE_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: F:/project/decision-hub
branch: dev
B2 implementation commit: 3fe1bab feat(qdr): persist replay evaluation baseline
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B3_PLAN: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
real HTTP / provider / Agent / LangGraph / LIVE: NO / DISABLED
```

### Close review validation

| 命令 / 证据 | 结果 | 说明 |
| --- | --- | --- |
| `Get-Location` | PASS | 当前目录为 `F:\project\decision-hub`。 |
| `git status --short`（开工前） | PASS / CLEAN | 起始无 dirty / staged。 |
| `git branch --show-current`（开工前） | PASS | `dev`。 |
| `git log --oneline -15`（开工前） | PASS | 最近提交包含 `3fe1bab feat(qdr): persist replay evaluation baseline`。 |
| `git diff --check` / `git diff --stat` / `git diff --name-only` / `git diff --cached --name-only`（开工前） | PASS / EMPTY | 起始无 whitespace error、tracked diff 或 staged diff。 |
| `git show --name-only --format='%h %s' HEAD` | PASS | HEAD 为 `3fe1bab feat(qdr): persist replay evaluation baseline`；B2 commit 文件清单未包含 `dh-api`、Controller、NQ 仓库、Provider SDK 或真实 HTTP client。 |
| `git diff --name-only HEAD~1..HEAD -- dh-app/src/main/resources/db/migration` | PASS | 仅新增 `dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql`；未修改 V1-V8 migration。 |
| migration review | PASS | V9 创建 7 张表；所有 B2 表包含 `tenant_id`；主查询路径具备 tenant-bound index；verdict / severity / action_label / hash CHECK 存在；COMMENT 明确 no raw prompt / no raw provider response / no credential / not executable trading signal；JSONB 限定 structured summary/ref。 |
| repository review | PASS | `ReplayCaseRepository`、`EvaluationCaseRepository`、`RegressionVerdictRepository` 与 JDBC adapters 均为 tenant-bound；未发现 UUID-only 查询；list 使用 `ReplayPageRequest`，`limit` 最大 100；duplicate checksum / verdict mismatch fail-closed。 |
| redaction review | PASS | DB CHECK 覆盖 top-level sensitive key；repository guard 递归覆盖 JSON key/value；测试覆盖 raw prompt / raw provider response / credential-like key。 |
| trading-term review | PASS | `BUY` / `SELL` / `MARKET_ORDER` / `PLACE_ORDER` / `CANCEL_ORDER` / `MUTATE_NQ_STATE` 只能出现在 forbiddenActions、CHECK、guard 或测试守卫；`LONG_BIAS` / `SHORT_BIAS` 只作为 direction label；`RegressionVerdict` 只表示 replay/evaluation verdict。 |
| `mvn -ntp -pl dh-app -am "-Dtest=V9QdrReplayEvaluationFlywayPostgresTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | BUILD SUCCESS / PASS / NOT_SKIPPED | Docker Desktop 29.6.1 + PostgreSQL 17.10 Testcontainer；Flyway successfully validated 9 migrations and applied to version v9；`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。 |
| `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test` | BUILD SUCCESS / PASS / NOT_SKIPPED | scoped reactor 15/15 SUCCESS；`dh-domain` 151 tests、`dh-usecase` 316 tests、`dh-infra` 88 tests、`dh-app` 84 tests 均 0 skipped；`PostgresContainerSmokeTest` 与 `V9QdrReplayEvaluationFlywayPostgresTest` 均真实执行 PostgreSQL/Testcontainers。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | Reactor 19/19 SUCCESS；Checkstyle 0 violations；Spotless check passed。系统 Maven settings 仍有 `profiles` warning，非本轮阻断。 |
| `.\\mvnw.cmd -v` | WRAPPER_UNUSABLE / P2 TOOLING RISK | exit code 0，但输出仍包含 `'\' is not recognized` 与 `.mvn\wrapper\maven-wrapper.jar` no main manifest attribute；不能写成 Maven wrapper PASS。 |
| required safety scan | REVIEWED / NO_ACTUAL_RISK | 用户指定 `rg` 已执行。全量命中为 docs 禁止项、历史风险说明、migration CHECK/COMMENT、redaction/trading guard、测试守卫或 fixture；B2 commit 窄范围复核未发现真实 HTTP/provider/Agent/LangGraph/LIVE 实现、raw prompt/raw provider response/credential 持久化或 executable trading signal。 |

### Close review result

```text
STAGE_QDR_4_B2_BLOCKER_FIX: DONE
STAGE_QDR_4_B2_IMPLEMENTATION: DONE
V9 PostgreSQL/Flyway load: PASS
Testcontainers result: PASS / NOT_SKIPPED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B3_PLAN: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
```

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
