# Decision Hub Testing

> supporting role: current validation evidence
> primary stage gate source: only for actual command results and tooling risk

## 2026-07-07 DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE validation

```text
Task type: DOCUMENTATION_ONLY + DOCS_GOVERNANCE + FACTSOURCE_CONSOLIDATION + ARCHIVE_CLEANUP + CURRENT_STATE_INDEXING + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
current workspace: E:/Project/decision-hub
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
| current docs archive operation | DONE | QDR stage-qdr-2 / stage-qdr-3 详细工单和计划已移动到 `docs/archive/stage-qdr-2/` 与 `docs/archive/stage-qdr-3/`；pre-close current snapshot 已复制到 `docs/archive/stage-qdr-3/pre-close-current-snapshot-20260707/`。 |
| final `git status --short` | DOCS_ONLY_DIRTY / NO_STAGED | dirty 限于 root README、`docs/current`、`docs/archive` 归档文件；无 Java、test、migration、contracts、golden_cases 或 NQ diff；`git diff --cached --name-only` 为空。 |
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
