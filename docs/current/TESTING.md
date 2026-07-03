# Decision Hub Testing

## 2026-07-03 NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO final validation

```text
Scope:
  - 本轮只关闭 M0 contract gap close work order。
  - 新增 DH M0 工单并同步 DH current docs 状态到 M1 next。
  - 同步 NQ dry-run worktree M0 工单与 current docs 状态。
  - NQ dev 只读；未修改 NQ dev 文件。
  - 不修改 production/test code、contracts、golden_cases、fixture JSON、API、migration 或 runtime wiring。

Result:
  NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO: COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED
  Current next: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED
  WORKSTREAM_MIXED_BLOCKED: NO
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 限于允许的 `docs/current` 文档和新增 M0 WO。 |
| `git branch --show-current` | PASS / `dev` | DH 当前分支为 `dev`。 |
| `git rev-parse HEAD` | PASS / `6a806a7148712b06b8a4712ed1050da7bebcba0c` | 基线为上一轮 dry-run mock implementation WO close commit。 |
| `git diff --check` | PASS | 退出码 0；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档；新增 M0 WO 由 `git status --short` 标识。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| NQ dry-run worktree `git diff --name-only -- backend frontend research scripts deploy .github "backend/**/db/migration"` | PASS / EMPTY | NQ 禁止代码、前端、脚本、workflow、migration 范围无 diff。 |
| NQ dev `git diff --name-only -- "docs/current/*NQ_DH*" "docs/current/*INTEGRATION1*"` | PASS / EMPTY | NQ dev 无 NQ-DH / Integration1 unstaged diff；`WORKSTREAM_MIXED_BLOCKED: NO`。 |
| NQ dev `git diff --name-only --cached -- "docs/current/*NQ_DH*" "docs/current/*INTEGRATION1*"` | PASS / EMPTY | NQ dev 无 staged NQ-DH / Integration1 diff。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 DH reactor module 全部 `SUCCESS`；Docker/Testcontainers 不可用导致 Docker-gated smoke tests skipped，非代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 DH reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未新增 fixture JSON；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO final validation

```text
Scope:
  - 本轮复核上一轮中断后的 WO 收口状态，并补齐缺口。
  - 新增/同步 DH dry-run mock implementation WO 与 current docs 状态。
  - 同步 NQ worktree dry-run mock implementation WO 与 current docs 状态。
  - 不修改 production/test code、contracts、golden_cases、fixture JSON、API、migration 或 runtime wiring。

Result:
  NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO: COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED
  Current next: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO / NOT STARTED
  WORKSTREAM_MIXED_BLOCKED: NO
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 限于允许的 `docs/current` 文档和新增 WO。 |
| `git branch --show-current` | PASS / `dev` | DH 当前分支为 `dev`。 |
| `git rev-parse HEAD` | PASS / `21d87e2654761062e390672f0d121f9b8e168d18` | 基线为 P4 gate-fix close commit。 |
| `git diff --check` | PASS | 退出码 0；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档；新增 WO 由 `git status --short` 标识。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| NQ dev `git diff --name-only -- "docs/current/*NQ_DH*" "docs/current/*INTEGRATION1*"` | PASS / EMPTY | NQ dev 无 NQ-DH / Integration1 dirty diff；`WORKSTREAM_MIXED_BLOCKED: NO`。 |
| NQ dev `git diff --name-only --cached -- "docs/current/*NQ_DH*" "docs/current/*INTEGRATION1*"` | PASS / EMPTY | NQ dev 无 staged NQ-DH / Integration1 diff。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Docker/Testcontainers 不可用导致 4 个 Docker-gated smoke tests skipped，非代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未新增 fixture JSON；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX final validation

```text
Scope:
  - 本轮只做 NQ-DH Integration-1 P4 implementation gate review fix。
  - DH current docs 同步 P4 gate-fix 结论、schema gap 分类和下一步 WO。
  - 不修改 production/test code、contracts、golden_cases、fixture JSON、API、migration 或 runtime wiring。

Result:
  NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX: COMPLETED / DOCS-ONLY / GATE-FIX
  Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 限于允许的 `docs/current` 文档。 |
| `git diff --check` | PASS | 退出码 0；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| stale old-next scan | PASS / EMPTY | current docs 已无旧 P4 not-started next 残留；验证记录不保留完整旧 next 字符串，避免后续自匹配。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；`PostgresContainerSmokeTest` 因本地 Docker/Testcontainers 环境不可用 skip 1，非代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未新增 fixture JSON；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-03 NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN final validation

```text
Scope:
  - 本轮只做 NQ-DH Integration-1 P3 dry-run implementation readiness planning 与验证记录同步。
  - DH canonical plan: docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md。
  - P3 合并原 P3 NQ dry-run stub test plan、P4 DH dry-run entry plan、P5 joint mock validation plan。

Result:
  NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
  P3 next consumed by: NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX / COMPLETED / DOCS-ONLY / GATE-FIX
  Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 仅位于允许的 `docs/current` 文档：P3 readiness plan、current index/status/work order、API planning note、Integration docs 与验证记录。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档；新增 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md` 由 `git status --short` 标识。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| `rg` stale old next scan | PASS / EMPTY | 未发现旧 `NQ-DH-I1-P3-NQ-DRYRUN-STUB-TEST-PLAN / NOT STARTED` 或旧 active next 标识残留在 `docs/current` 当前口径中。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；`PostgresContainerSmokeTest` 因本地 Docker/Testcontainers 环境不可用 skip 1，非代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未新增 fixture JSON；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-02 NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN final validation

```text
Scope:
  - 本轮只做 NQ-DH Integration-1 P2 contract fixtures planning 与验证记录同步。
  - DH canonical plan: docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md。
  - DH API.md 只记录 planned / not implemented schema gap，不宣称 API 已实现。

Result:
  NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
  P2 next consumed by: NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN / COMPLETED / PLAN ONLY / NOT IMPLEMENTED
  P2 next consumed by: NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN and NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX
  Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 仅位于允许的 `docs/current` 文档：P2 fixtures plan、Integration docs、API planning note、current index/status/work order。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档；新增 `docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md` 由 `git status --short` 标识。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；`PostgresContainerSmokeTest` 因 `\\\\.\\pipe\\docker_engine` AccessDenied / Docker environment unavailable 被 Testcontainers skip 1，属于本地 Docker named-pipe 可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed；保留子模块 `unable to find checkstyle:checkstyle outputFile` 信息，聚合结果为 SUCCESS。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未新增 fixture JSON；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-02 NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN final validation

```text
Scope:
  - 本轮只做 NQ-DH Integration-1 P1 contract dry-run planning 与验证记录同步。
  - DH canonical plan: docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md。
  - DH API.md 只记录 planned / not implemented dry-run contract，不宣称 API 已实现。

Result:
  NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
  Next: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  Real HTTP: NOT STARTED
  Real provider: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 dirty 仅位于允许的 `docs/current` 文档：P1 canonical contract plan、Integration docs、API planning note、current index/status/work order。 |
| `git log --oneline -5` / `git rev-parse HEAD` | PASS | HEAD 为 `0ee2768bce6cf856df5c61c3c91c8fed25bdb5c8`，基线为 Stage4 naming 与 P0 factsource close。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Windows LF/CRLF 工作区提示，非阻断。 |
| `git diff --stat` | PASS / DOCS-ONLY | tracked diff 限于 `docs/current` 文档；新增 `docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md` 由 `git status --short` 标识。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；`PostgresContainerSmokeTest` 因 `\\\\.\\pipe\\docker_engine` AccessDenied / Docker environment unavailable 被 Testcontainers skip 1，属于本地 Docker named-pipe 可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed；保留子模块 `unable to find checkstyle:checkstyle outputFile` 信息，聚合结果为 SUCCESS。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-02 NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE

```text
Scope:
  - 本轮只做 NQ / DH Integration-1 dry-run P0 factsource rebase close。
  - 同步当前事实源为 NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED。
  - P1 只允许进入 contract dry-run plan，不是 implementation、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

Result:
  NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
  Next: NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / NOT STARTED
  Integration-1 implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS / CHANGES PRESENT | 当前 worktree 有既有 docs / skill / gate snapshot 改动；本 P0 未新增生产代码、测试代码、contracts 或 golden_cases diff。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Git LF/CRLF 工作区提示。 |
| `git diff --stat` | PASS | diff 面为 docs / skill 文档；无 Java / Kotlin / Python / TypeScript 生产代码或测试代码改动。 |
| `rg -n "DH-GATEK\|dh-gatek\|DH GateK\|GateK\|GateL\|GateN\|GATEN" .agents AGENTS.md README.md docs/current docs/gates` | PASS / CLASSIFIED | 命中已分类：NQ `GateN` 合法；旧 `DH-GATEK-*` / `dh-gatek-*` 为 `SUPERSEDED / NAMING_REPLACED`、历史记录、skill 禁止示例或 `docs/gates/**` 冻结快照内部历史字样；当前前置条件为 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED`。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；`PostgresContainerSmokeTest` 因 `\\.\pipe\docker_engine` AccessDenied / Docker environment unavailable 被 Testcontainers skip 1，属于本地 Docker named-pipe 可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 `SUCCESS`；Checkstyle 0 violations；Spotless check passed；保留子模块 `unable to find checkstyle:checkstyle outputFile` 信息，聚合结果为 SUCCESS。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-02 DH-STAGE4-NAMING-REBASE-FIX

```text
Scope:
  - 本轮只做 DH Stage4 命名 rebase、current factsource 同步、验收报告重命名和冻结目录重命名。
  - 不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration。
  - 不启动 Integration-1 runtime、真实 HTTP、真实 NQ 调用、真实 provider、AI / LangGraph 或 LIVE。

Result:
  DH-STAGE4-NAMING-REBASE-FIX: CLOSED
  DH-STAGE4-DECISION-PIPELINE-MVP: ACCEPTED / CLOSED
  DH-GATEK-DECISION-PIPELINE-MVP: SUPERSEDED / NAMING_REPLACED
  docs/gates/dh-gatek-decision-pipeline-mvp: SUPERSEDED / NAMING_REPLACED
  Next: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS | 变更位于 root/current docs、`.agents/skills/**`、`docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_*` rename 和 `docs/gates/dh-stage4-decision-pipeline-mvp/` rename；无生产代码、测试代码、contracts、golden_cases diff。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Git LF/CRLF 工作区提示。第一次执行曾发现 4 行 Markdown trailing whitespace，已修复并重跑通过。 |
| `git diff --stat` | PASS | 非 rename 内容 diff 为 20 个文件、434 insertions / 332 deletions；目录与文件 rename 由 `git status --short` 单独体现。 |
| `rg -n "DH-GATEK\|dh-gatek\|DH GateK\|GateK\|GateL\|GateN\|GATEN" .agents AGENTS.md README.md docs/current docs/gates` | PASS / CLASSIFIED | 命中已分类：NQ `GateN` 合法；skill 禁止示例合法；旧 `DH-GATEK-*` 与 `dh-gatek-*` 仅作为 `SUPERSEDED / NAMING_REPLACED`、历史 WORKLOG/TESTING 或冻结快照原始内容；`docs/gates/dh-stage4-decision-pipeline-mvp/README.md` 已说明原错误目录名与正确目录名。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；Total time 37.877 s；`PostgresContainerSmokeTest` 因 `\\.\pipe\docker_engine` AccessDenied / Docker environment unavailable 被 Testcontainers skip 1，属于本地 Docker named-pipe 可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；Checkstyle 0 violations；Spotless check passed；Total time 3.656 s。 |

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path / Controller / migration；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential / token / cookie / API secret / passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE；未修改 NQ 仓库。

## 2026-07-02 DH-DOCS-SKILL-STAGE-NAMING-AUDIT-FIX

```text
Scope:
  - 本轮只审查并修正 DH 文档治理 skill / workflow router 的阶段命名规则。
  - 不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration。
  - 不移动、不删除、不重命名 docs/gates 冻结目录。
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS | 变更仅位于 `.agents/skills/**`、`AGENTS.md` 和 `docs/current/**` allowlist。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Windows 行尾转换 warning。 |
| `git diff --stat` | PASS | 8 个文件变更，均为 docs / skill 规则文件。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | PASS / EMPTY | 禁止的生产代码、测试代码、contracts、golden_cases 范围无 diff。 |
| `rg -n "DH-GATEK\|dh-gatek\|GateK\|GATEL\|GateL\|GATEN\|GateN\|DH Gate" .agents AGENTS.md README.md docs/current docs/gates` | PASS / CLASSIFIED | 命中已分类：NQ `GateN` 合法；skill 规则示例中的 `GateK/GateL/GateN` 禁止项合法；`DH-GATEK-*`、`dh-gatek-*`、`GateK Decision Pipeline` 属历史错误命名残留或 current docs 待修复对象；根 `README.md` 与部分 `docs/current` 仍有 DH 自身阶段写成 GateK 的残留，下一任务修复。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；`PostgresContainerSmokeTest` 因当前进程访问 `\\.\pipe\docker_engine` 被拒绝而 skip 1 个 Testcontainers smoke，属于 Docker named-pipe 环境可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；Checkstyle 0 violations；Spotless check passed；保留既有子模块 `unable to find checkstyle:checkstyle outputFile` 信息，聚合结果为 SUCCESS。 |

rg 命中分类：

- NQ GateN：允许，用于 NQ 当前阶段和 NQ-DH Integration-1 rebase 前置语境。
- 历史错误命名：`DH-GATEK-DECISION-PIPELINE-MVP`、`docs/gates/dh-gatek-decision-pipeline-mvp/`、`DH GateK Decision Pipeline MVP` 暂时允许保留，但必须指向 `DH-STAGE4-NAMING-REBASE-FIX`。
- skill 规则示例：`.agents/skills/dh-docs-writer/SKILL.md` 与 `.agents/skills/nq-dh-workflow-router/SKILL.md` 中用于说明禁止项的 `GateK/GateL/GateN` 命中允许。
- current docs 待修复对象：`AGENTS.md`、根 `README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/CODEX_WORKFLOW_INDEX.md`、`docs/current/API.md`、`docs/current/ROADMAP.md`、`docs/current/STATUS.md`、`docs/current/WORK_ORDER.md`、`docs/current/DH_GATEK_DECISION_PIPELINE_MVP_*` 仍存在 DH 自身阶段 GateK 命名残留，需由 `DH-STAGE4-NAMING-REBASE-FIX` 统一处理。
- 冻结目录待处理对象：`docs/gates/dh-gatek-decision-pipeline-mvp/**` 本轮未移动、未删除、未重命名，需下一任务制定 rebase / migration / archive 处理方案。

Boundary:

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API / Controller / migration；未移动或删除 `docs/gates`；未启动 Integration-1 runtime；未真实 HTTP；未真实 NQ 调用；未接真实 provider；未接 AI / LangGraph；未开启 LIVE；未修改 NQ 仓库。

## 2026-07-02 NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN

```text
Scope:
  - 本轮只做 NQ-DH Integration-1 dry-run 的 GateN rebase planning 与 docs/current 同步。
  - 不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ runtime。

Plan result:
  NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
  Next: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
  Integration-1 dry-run implementation: NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED
```

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS | 计划文档与 docs/current 索引类文档变更可见；未发现生产代码、测试代码、contracts、golden_cases、API、migration 变更。 |
| `git diff --check` | PASS | 无 whitespace error；仅 Windows 行尾转换 warning。 |
| `git diff --stat` | PASS | tracked diff 限于 `docs/current/DH_NQ_INTEGRATION.md`、`README.md`、`ROADMAP.md`、`STATUS.md`、`WORK_ORDER.md`；新计划文档由 `git status --short` 标识。 |
| `mvn -ntp test` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；`PostgresContainerSmokeTest` 因当前进程访问 `\\.\pipe\docker_engine` 被拒绝而 skip 1 个 Testcontainers smoke，属于 Docker named-pipe 环境可达性问题，不是代码失败。 |
| `mvn -ntp -Pquality validate` | PASS / BUILD SUCCESS | 19 个 reactor module 全部 SUCCESS；Checkstyle 0 violations；Spotless check passed；保留既有子模块 `unable to find checkstyle:checkstyle outputFile` 信息，聚合结果仍为 SUCCESS。 |

Boundary:

未新增 API path / Controller / migration；未修改 Java 生产代码或测试代码；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未读取或输出 credential、token、cookie、API secret、passphrase；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未把 dry-run 写成真实联调或实盘准备完成。

## 2026-07-02 DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE

```text
Scope:
  - DH Stage4 Decision Pipeline MVP 最终验收、冻结、回归验证、安全边界复核和 docs/current 同步。
  - 本轮只改文档与 docs/gates 冻结快照；不修改生产代码、测试代码、contracts、golden_cases、API、Controller 或 migration。

Acceptance:
  DH Stage4 Decision Pipeline MVP: ACCEPTED / CLOSED
  K1-K7: CLOSED
  K8: CLOSED
  Next: NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN / NOT STARTED
  Integration-1 runtime: NOT STARTED
  Runtime integration: NOT STARTED
  DH integrated: NO
  AI / Agent runtime: NOT STARTED
  LangGraph runtime: NOT STARTED
  LIVE: DISABLED

Evidence review:
  - K1 contract: DecisionType 仅 READ_ONLY_RECOMMENDATION；DecisionAction 仅 ABSTAIN / OBSERVE /
    NO_TRADE / LONG_BIAS / SHORT_BIAS；forbiddenActions 固定 PLACE_ORDER / CANCEL_ORDER /
    MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB。
  - K2 orchestrator: DefaultDecisionOrchestrator mock-only、no real provider、no NQ、no HTTP、
    no LangGraph，failure path fail-closed。
  - K3 persistence: V5 只新增 DH-owned decision audit / snapshot / trace / provider call /
    output / audit event 表；不存 secret / token / credential / NQ DB 内容。
  - K4 replay: internal read model；不新增 API / Controller / endpoint；tenant mismatch /
    corrupted / incomplete data fail-closed。
  - K5 provider guard: mock-only health / budget / latency guard；disabled / unhealthy /
    timeout / budget exceeded fail-closed；provider call summary 可 replay。
  - K6 mock NQ dry-run: test-only factory / fixture / no-live-trade / persistence-to-replay
    contract tests；无真实 HTTP / NQ mutation。
  - K7 golden/eval: 12 个 golden cases；action 白名单、forbiddenActions 固定、credential /
    account / order / execution 禁止字段扫描与 eval baseline tests 已覆盖。

Boundary scan:
  Command:
    rg -n "(RealClient|LangGraph|OpenAI|Claude|Gemini|WebClient|RestTemplate|HttpClient|placeOrder|cancelOrder|BUY|SELL|apiSecret|passphrase|accountId|Controller|NqClient|Exchange|Broker|live|LIVE|endpoint|Endpoint|PostMapping|GetMapping|RequestMapping)" dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java dh-app/src/main/resources/db/migration/V5__dh_decision_pipeline_audit.sql
  Result:
    生产范围命中仅为禁止说明、migration comment、脱敏/denylist 和配置注释；未发现生产越界实现。

  Command:
    rg -n "(RealClient|LangGraph|OpenAI|Claude|Gemini|WebClient|RestTemplate|HttpClient|placeOrder|cancelOrder|BUY|SELL|apiSecret|passphrase|accountId|Controller|NqClient|Exchange|Broker|live|LIVE|endpoint|Endpoint|PostMapping|GetMapping|RequestMapping)" dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision dh-domain/src/test/java/com/guidinglight/decisionhub/contracts dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java dh-app/src/main/resources/db/migration/V5__dh_decision_pipeline_audit.sql contracts/json-schema/dh-decision-request.schema.json contracts/json-schema/dh-decision-output.schema.json golden_cases/decision docs/current
  Result:
    完整允许范围命中均为文档说明、negative tests、denylist、migration comment、historical/deferred
    docs 或固定 forbiddenActions；未发现 Stage4 生产越界实现。

  Command:
    rg -n "BUY|SELL|PLACE_ORDER|CANCEL_ORDER|MARKET_ORDER|LIMIT_ORDER"
    dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionAction.java
    dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionOutput.java
  Result:
    exit code 1；无命中；未把交易动作放入 DecisionAction 或 DecisionOutput action。

  Command:
    rg -n "class .*Controller|@PostMapping|@GetMapping|@RequestMapping|@RestController"
    dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision
    dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision
    dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision
    dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
  Result:
    exit code 1；Stage4 decision pipeline 生产范围无新增 Controller 或 mapping annotation。

Validation:
  Command:
    git status --short
  Initial result:
    clean before K8 docs/freeze edits.
  Final result:
    modified README.md and docs/current status docs; untracked
    docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md and
    docs/gates/dh-stage4-decision-pipeline-mvp/.

  Command:
    git diff --check
  Initial/final result:
    exit code 0；无 whitespace error；仅 Windows LF -> CRLF warning。

  Command:
    git diff --stat
  Final tracked result:
    README.md、docs/current/API.md、README.md、ROADMAP.md、STATUS.md、TESTING.md、
    WORKLOG.md、WORK_ORDER.md 共 8 个 tracked 文件变更；391 insertions / 87 deletions。
    新增 untracked acceptance report 与 docs/gates freeze snapshot 不包含在 git diff --stat 输出中，
    以 git status --short 为准。

  Command:
    mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  Result:
    BUILD SUCCESS；reactor 15/15 SUCCESS；Total time 18.980 s；Finished at 2026-07-02T19:39:15+08:00。
  Note:
    当前 sandbox 下 Testcontainers 找不到可用 Docker 环境，PostgresContainerSmokeTest skipped 1；
    这是环境未覆盖项，不是 Stage4 代码失败。

  Command:
    mvn -ntp test
  Result:
    BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 18.355 s；Finished at 2026-07-02T19:39:53+08:00。
  Note:
    当前 sandbox 下 Testcontainers 找不到可用 Docker 环境，PostgresContainerSmokeTest skipped 1。

  Command:
    mvn -ntp -Pquality validate
  Result:
    BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；
    Finished at 2026-07-02T19:40:04+08:00。

  Command:
    docker info --format '{{.ServerVersion}}'
  Result:
    FAILURE；npipe:////./pipe/dockerDesktopLinuxEngine 不存在，Docker daemon 当前未运行或未暴露该管道。

Readiness:
  ALLOW_STAGE4_CLOSE: YES
  ALLOW_INTEGRATION1_DRYRUN_PLAN_REBASE_N: YES
  ALLOW_INTEGRATION_1_RUNTIME: NO
  ALLOW_AGENT_PHASE: NO
  ALLOW_LANGGRAPH_RUNTIME: NO
  ALLOW_LIVE: NO

Boundary:
  未修改生产代码；未修改测试代码；未新增 API path；未新增 Controller；未新增 migration；
  未真实 HTTP；未真实 NQ 调用；未接真实 provider；未读取 credential/token/cookie/API secret/passphrase；
  未接 OpenAI/Claude/Gemini/本地模型；未接 LangGraph；未启动 Integration-1 runtime；
  未开启 LIVE；未修改 NQ 仓库。
```

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL

```text
Scope:
  - 将 golden_cases/decision 扩展到 12 个 deterministic golden cases。
  - 新增 K7 eval baseline / golden case / security boundary 回归测试。
  - 复用 K6 mock NQ dry-run fixtures，并升级为 K7 wrapper，不改 K1-K6 生产合同。
  - 本轮只修改 golden_cases、dh-usecase test code 与 docs/current；生产代码、API、Controller、migration 均无新增。

Golden case inventory:
  Total: 12
  Files:
    golden_cases/decision/valid_no_trade.json
    golden_cases/decision/policy_blocked.json
    golden_cases/decision/provider_timeout_abstain.json
    golden_cases/decision/provider_budget_exceeded_abstain.json
    golden_cases/decision/high_risk_abstain.json
    golden_cases/decision/no_evidence_abstain.json
    golden_cases/decision/forbidden_action_rejected.json
    golden_cases/decision/mock_nq_valid_dryrun.json
    golden_cases/decision/mock_nq_provider_blocked.json
    golden_cases/decision/mock_nq_no_live_trade_guard.json
    golden_cases/decision/replay_found_trace.json
    golden_cases/decision/replay_tenant_mismatch_blocked.json
  Contract:
    decisionType = READ_ONLY_RECOMMENDATION
    action only ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
    forbiddenActions fixed to PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB

Focused regression:
  Command:
    mvn -ntp -pl dh-usecase -am "-Dtest=DecisionGoldenCaseTest,DecisionEvalBaselineTest,DecisionGoldenCaseSecurityBoundaryTest,MockNqDecisionDryRunContractTest,MockNqDecisionNoLiveTradeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  Result:
    BUILD SUCCESS；K7 golden/eval/security + K6 compatibility 共 14 tests passed。

Impact modules:
  Command:
    mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  Result:
    BUILD SUCCESS；reactor 15/15 passed；PostgresContainerSmokeTest 在该轮实际使用 Docker / PostgreSQL 17 通过。

Full validation:
  Command:
    git status --short
  Result:
    仅 K7 允许范围内文件变更：golden_cases/decision、dh-usecase test code、docs/current 状态文档。

  Command:
    git diff --check
  Result:
    exit code 0；仅 LF/CRLF warning，无 whitespace error。

  Command:
    git diff --stat
  Result:
    输出 tracked diff stat；未跟踪的 K7 新 golden cases / tests 需结合 git status --short 查看。

  Command:
    mvn -ntp test
  Result:
    BUILD SUCCESS；reactor 19/19 passed。
    该全量轮次中 PostgresContainerSmokeTest 因 Docker named pipe AccessDeniedException 按 Testcontainers 机制 skip；
    这属于本机 Docker 访问差异，不是 K7 代码失败。影响模块轮次已经实跑并通过该 smoke。

  Command:
    mvn -ntp -Pquality validate
  Result:
    BUILD SUCCESS；reactor 19/19 passed；Checkstyle 0 violations；Spotless passed。
    每模块 checkstyle outputFile lookup 提示为既有非阻断噪音，聚合结果成功。

Boundary scan:
  Command:
    rg -n "(RealClient|LangGraph|OpenAI|Claude|Gemini|WebClient|RestTemplate|HttpClient|placeOrder|cancelOrder|BUY|SELL|apiSecret|passphrase|accountId|Controller|NqClient|Exchange|Broker|live|LIVE|endpoint|Endpoint|PostMapping|GetMapping|RequestMapping)" dh-domain/src/main dh-domain/src/test dh-usecase/src/main dh-usecase/src/test dh-infra/src/main dh-infra/src/test dh-app/src/main dh-app/src/test dh-api/src/main dh-connector/src/main dh-security/src/main contracts golden_cases docs/current
  Result:
    无 K7 生产越界实现。
    命中集中在既有 Integration-0 negative fixtures/tests、no-outbound/ArchUnit guard、docs/current 边界文字、
    既有 API controller、历史 Stage 文档、K7 security tests / forbidden assertions。

Readiness:
  ALLOW_K7_CLOSE: YES
  ALLOW_K8_ACCEPTANCE_FREEZE: YES
  ALLOW_INTEGRATION_1_RUNTIME: NO
  ALLOW_AGENT_PHASE: NO
  ALLOW_LANGGRAPH_RUNTIME: NO
  ALLOW_LIVE: NO

Boundary:
  未新增 API path；未新增 Controller；未新增 migration；未修改生产代码；未真实 HTTP；未真实 NQ 调用；
  未接真实 provider；未读取 credential/token/cookie/API secret/passphrase；未接 OpenAI/Claude/Gemini/本地模型；
  未接 LangGraph；未启动 Integration-1 runtime；未开启 LIVE；未修改 NQ 仓库。
```

## 2026-06-28 DH-CODE-REALITY-AUDIT-FIX-PACK

```text
Scope:
  - /legacy/runs 纳入 DhApiAuthenticationFilter；POST/GET 匿名访问拒绝，认证请求使用认证 tenant。
  - production NQ feedback validator 对齐 INT0 forbidden-field / forbidden-capability 递归校验。
  - IdempotencyFilter 调整到认证之后，只使用认证 tenant，不再写固定 t-default 幂等 key。
  - docs/current/API.md 修正旧阶段口径。

Focused regression:
  Command:
    mvn "-Dtest=LegacyRunControllerSecurityWebMvcTest,IdempotencyFilterSecurityTest,NqFeedbackControllerWebMvcTest,NqFeedbackContractValidationTest,NqFeedbackIdempotencyTest,DhNqIntegration0*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test
  Result:
    BUILD SUCCESS；34 tests passed；INT0 16/16 passed。

Full validation:
  git status --short:
    仅本 fix pack 允许范围内文件变更；新增 legacy/idempotency 回归测试文件。
  git diff --check:
    exit code 0；仅 LF/CRLF 提示，无 whitespace error。
  git diff --stat:
    tracked diff 11 files changed, 558 insertions(+), 28 deletions(-)；另有 2 个新测试文件未计入 tracked stat。
  mvn test:
    BUILD SUCCESS；Surefire reports 汇总 307 tests / 0 failures / 0 errors / 4 skipped。
    Skipped 为 Docker/Testcontainers 环境项（JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1）。
  mvn -Pquality validate:
    BUILD SUCCESS；checkstyle 0 violations；spotless check passed。
```

## 1. 当前状态

```text
Stage1                  代码 + 闭环测试已落地
Stage1-CLOSE            旧链路 @Deprecated + 文档单源 + ArchUnit 4 条新规则
Stage2-PoC-B1 IMPLEMENT 领域模型 + JSON Schema + OpenAPI components 落地
Stage2-PoC-B2 IMPLEMENT NQ feedback ingestion envelope/Validator/Router/8 Handler/幂等/WebMvc
Stage2-PoC-B3 IMPLEMENT dh-connector Forecast / Research Adapter 接口预留 + Fake 实现
Stage2-PoC-B4 IMPLEMENT Reflection / Checkpoint / Dynamic Planner + 4 个 StrategyHandler + 内存仓储 + 4 个测试
Stage2-PoC-B5 IMPLEMENT V3 migration + 5 个 Stage2 JDBC 仓储 + Stage2JdbcWiringConfig +
                        ArchUnit 扩到 10 条 + OpenAPI 对齐 + Stage2ClosedLoopTest 全闭环
Stage2-PoC VERIFY       2026-05-26 BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        硬边界扫描全 PASS；契约/文档不一致项已修正；
                        Verdict: GO，允许进入 Stage2-PoC FREEZE
Stage2-PoC FREEZE       2026-05-26 完成：docs/current 快照冻结到
                        docs/gates/dh-stage2-poc/；FREEZE 前最终验收 mvn test
                        BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        无 Java 业务代码改动；下一阶段进入 Stage3-PLAN
Stage3-PLAN              2026-05-26 完成：仅文档规划，新增 6 份 STAGE3_*.md；
                        无 Java 业务代码改动；mvn test 作为回归基线 BUILD SUCCESS / 122 tests
                        / ArchUnit 10/10；下一阶段进入 Stage3-WO
Stage3-WO                2026-05-26 完成：仅文档工单细化；重写 STAGE3_WORK_ORDER.md
                        + 新增 STAGE3_BATCH_PLAN.md；无 Java 业务代码改动；
                        mvn test 作为回归基线 BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B1 Contract Alignment IMPLEMENT
Stage3-B1 IMPLEMENT      2026-05-26 完成：DH 仓库内对齐 contracts / schema / OpenAPI；
                        新增 4 份 contract 测试类（NqFeedbackEnvelopeSchemaContractTest 7 +
                        DhBacktestRequestSchemaContractTest 7 +
                        BacktestResultSnapshotSchemaContractTest 6 +
                        OpenApiContractAlignmentTest 9 = 29 cases）；
                        mvn test BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        Stage1ClosedLoop / Stage2ClosedLoop / 全部历史用例保持全绿；
                        零 NQ 仓库改动；零真实 HTTP；零 Java 业务代码修改；
                        下一阶段进入 Stage3-B2 NQ Feedback Outbox PLAN
Stage3-B2 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_NQ_OUTBOX_SPEC.md
                        （NQ outbox 11 段完整规格：模块 / 表结构 / 8 触发点 / 5 状态机 +
                        8 attempt 退避矩阵 / audit / 5 字段语义 / HTTP 矩阵 /
                        NQ 后续 5 个 Batch / GateJ-FREEZE 防护 / schema 演进）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B3 DH Backtest Request Adapter PLAN
Stage3-B3 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md
                        （DH backtest request adapter 14 段完整规格：可插拔原则 10 条 + 三层 gate +
                        三 client 策略（Fake / Disabled / Real）+ 9 状态机 + 错误码映射 +
                        24h 幂等 + 8 attempt 退避 + DH/NQ 双方默认关闭 + 8 个测试类规划 +
                        B3-1..B3-5 五批 IMPL 拆解）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B4 End-to-End Contract Test PLAN
Stage3-B4 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md
                        （DH/NQ 端到端契约测试 11 段完整规格：DH staging + NQ test cluster
                        环境规划 / 7 个联调用例 T1-T7 / 10 类 Contract Test / 5 字段对账 /
                        deterministic 测试数据 / 三段验收命令 / 失败处理矩阵 /
                        B4-1..B4-5 五批 IMPL 拆解 / Stage3-PLAN-FREEZE 衔接）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        零真实联调；零实盘；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-PLAN-FREEZE
Stage3-PLAN-FREEZE       2026-05-26 完成：Stage3 规划成果落盘冻结：
                        - 10 份 STAGE3_*.md 一致性核查 9 条核心原则通过（无措辞修订）
                        - docs/current/* 33 个文件复制到 docs/gates/dh-stage3-plan/
                        - 冻结快照 README.md 顶部加冻结声明 + 一致性核查表 +
                          Stage3-PLAN 交付物清单
                        - 6 份状态文档同步到 "Stage3-PLAN-FREEZE completed /
                          Next: Stage3-B1 IMPLEMENT"
                        - mvn test 作为回归基线 BUILD SUCCESS / 151 tests / 0 failures /
                          0 errors / 0 skipped / ArchUnit 10/10
                        - 本轮为文档冻结，零 Java 业务代码改动；零 NQ 仓库改动；
                          零 contracts / migration / OpenAPI 修改；零真实外部接入
                        - 下一阶段进入 Stage3-B1 IMPLEMENT（B1 已完成；B2/B3/B4 单独开工）
DH-CODEX-WORKFLOW       2026-06-06 完成：Codex workflow routing 文档固化；
                        本轮仅文档与规则文件变更，不运行 mvn test；
                        使用 git status --short / git diff --check / 定向文本检查验证
```

最近一次 `mvn test` 见 §3。

## 2. 标准验证命令

最低验证：

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

说明：`PostgresContainerSmokeTest` 依赖 Docker，本机/CI 缺少 Docker 时排除。

质量检查：

```bash
mvn -Pquality validate
```

应用启动验证：

```bash
mvn -pl dh-app -am spring-boot:run
```

## 3. 最近一次验收结果（2026-05-25 Stage2-PoC-B5 IMPLEMENT）

```text
日期：2026-05-25
阶段：Stage2-PoC-B5 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 5 新增（dh-app）
    - V3MigrationPresenceTest                   5/5   通过（4 张新表 / 2 ALTER / event_id 唯一索引 /
                                                              jsonb 列 + comment 保留 / 无 orders|trades|fills|positions|live_）
    - ArchitectureTest                          10/10 通过（Stage1-CLOSE 5 + Stage2-PoC-B5 新增 5：
                                                              connector.tools/research !-> infra；
                                                              domain.{forecast,marketdata,reflection,checkpoint} !-> connector；
                                                              usecase.agent.planner/feedback !-> providers）

  Batch 5 新增（dh-infra）
    - JdbcNqFeedbackEventRepositoryTest         4/4   通过（首次写返回 true + CAST(? AS jsonb) /
                                                              幂等命中返回 false 不调 update /
                                                              unique 竞态 catch DuplicateKeyException /
                                                              null eventId 返回 Optional.empty）
    - JdbcSqlFragmentsTest                      5/5   通过（reflection/checkpoint insert 命中表名 + 1 段 CAST(? AS jsonb)，
                                                              forecast insert 2 段 CAST(? AS jsonb)，
                                                              external_snapshot insert 3 段 CAST(? AS jsonb)，
                                                              external_snapshot.findById 走 RowMapper 返回 empty）

  Batch 5 新增（dh-usecase）
    - Stage2ClosedLoopTest                      2/2   通过（bullish 走 BULL_FOCUSED + reflections 按 stepIndex 升序 +
                                                              checkpoints 按 checkpointIndex 升序 + JudgeDecision 唯一出口；
                                                              bear 走 BEAR_FOCUSED 仍以 JudgeDecision 终结）

  Batch 4 / Batch 3 / Batch 2 / Batch 1 回归保持全绿
    - dh-domain    Batch 1                      35/35
    - dh-connector Batch 3                      9/9
    - dh-usecase   Batch 2 + B4 + B5            47/47
    - dh-api       Batch 2 WebMvc               7/7
    - dh-app                                    15/15（含 ArchUnit 10 + V3MigrationPresence 5）
    - dh-infra     Batch 5                      9/9

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过（DefaultAgentTaskPlanner 仍直连）
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除
                                                Stage2-PoC VERIFY 在装好 Docker 的 CI 上跑

Batch 5 范围（零 NQ 仓库改动 / 零真实外部服务调用 / 零 LLM / 零 TradingAgents Python 代码 / 零前端 /
              零 dh-memory JDBC 替换（留 Stage3）/ 零绕过 NQ 风控）：
  - dh-app 新增   V3__stage2_poc_tools.sql, Stage2JdbcWiringConfig, V3MigrationPresenceTest
                  AgentRuntimeWiringConfig 补 DynamicAgentTaskPlanner + ReflectionCheckpointService 等
                  ArchitectureTest 扩到 10 条规则
  - dh-infra 新增  5 个 JDBC 仓储 + 2 个测试类（9 cases 全绿） + pom.xml 加 dh-connector / jdbc starter
  - dh-usecase 新增 Stage2ClosedLoopTest（2 cases 全绿）
  - dh-connector 新增 ForecastArtifactStore + InMemoryForecastArtifactStore
  - contracts/openapi.yaml  /api/ai/feedback/nq 对齐 + B3/B4 路径占位注释
```

## 5. 历史验收：2026-05-25 Stage2-PoC-B4 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B4 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 4 新增（dh-usecase）
    - PlannerStrategyResolverTest               9/9   通过（DEFAULT 兜底 + bull/bear/volatile 关键字 + volatile 优先 + 显式 plannerStrategy 覆盖 regime + 非法 strategy 回退）
    - PlannerStrategyRegistryTest               5/5   通过（缺失 DEFAULT 拒绝 + 重复注册拒绝 + 注册查 handler + 缺失 strategy 回退 DEFAULT + 各 handler 产出非空任务图）
    - DynamicAgentTaskPlannerTest               7/7   通过（DEFAULT/BULL/BEAR/VOLATILE handler 选择 + 显式 plannerStrategy 覆盖 + registry 缺失回退 DEFAULT + 每种策略保留 JUDGE 终点）
    - ReflectionCheckpointServiceTest           7/7   通过（写入/排序/校验 stepIndex/必填 snapshotJson/ABORT 不替代 JudgeDecision/未知 runId 空集）

  Batch 3 / Batch 2 / Batch 1 回归保持全绿
    - dh-domain    Batch 1 35/35
    - dh-connector Batch 3 9/9
    - dh-usecase   Batch 2 + B4 28/28（feedback 15 + planner/reflection 28 等共 28 个 B4 用例 + 历史用例）
    - dh-api       Batch 2 WebMvc 7/7

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过（DefaultAgentTaskPlanner 仍直连，Stage1 行为不变）
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除

Batch 4 范围（零 NQ 仓库改动 / 零真实外部服务调用 / 零 dh-domain 改动 / 零 JDBC / 零前端 / 零 LLM / 零 TradingAgents Python 代码）：
  - dh-usecase 新增类       12 个
      agent/planner/PlannerStrategy
      agent/planner/PlannerStrategyResolver
      agent/planner/PlannerStrategyRegistry
      agent/planner/DynamicAgentTaskPlanner
      agent/planner/impl/DefaultPlannerStrategyResolver
      agent/planner/strategy/PlannerStrategyHandler
      agent/planner/strategy/DefaultPlannerStrategyHandler
      agent/planner/strategy/BullFocusedPlannerStrategyHandler
      agent/planner/strategy/BearFocusedPlannerStrategyHandler
      agent/planner/strategy/VolatileDiversifiedPlannerStrategyHandler
      agent/ReflectionCheckpointService + impl/DefaultReflectionCheckpointService
      agent/ReflectionEntryRepository + CheckpointEntryRepository
      agent/inmemory/InMemoryReflectionEntryRepository + InMemoryCheckpointEntryRepository
  - dh-usecase 新增测试     4 个（28 cases 全绿）
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B3 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B3 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 3 新增（dh-connector）
    - FakeForecastToolAdapterTest               3/3   通过（happy path + symbol 空 + horizon 空）
    - FakeResearchDataAdapterTest               4/4   通过（happy path + symbols 空 + start>end + 空 dataTypes）
    - InMemoryResearchSnapshotStoreTest         2/2   通过（save+findById/findByTraceId、findBySymbolAndDateRange 命中/未命中）

  Batch 1 / Batch 2 回归保持全绿（与上一轮一致）
    - dh-domain    Batch 1 35/35
    - dh-usecase   Batch 2 15/15
    - dh-api       Batch 2 WebMvc 7/7

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除

Batch 3 范围（零真实外部服务调用 / 零 NQ 仓库改动 / 零 JDBC / 零 dh-domain 改动 / 零 WiringConfig 改动）：
  - dh-connector 新增类     8 个
      tools/ForecastRequest, tools/ForecastToolPort, tools/fake/FakeForecastToolAdapter
      research/MarketSnapshotRequest, research/ResearchDataAdapter, research/ResearchSnapshotStore
      research/fake/FakeResearchDataAdapter, research/fake/InMemoryResearchSnapshotStore
  - dh-connector 新增测试   3 个 (9 cases 全绿)
  - dh-connector pom.xml    加 junit-jupiter (test scope)
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B2 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B2 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 1 回归（dh-domain）
    - NqFeedbackEnvelopeTest                    4/4   通过
    - NqFeedbackPayloadContractTest             8/8   通过
    - DhBacktestRequestContractTest             5/5   通过
    - ForecastArtifactTest                      4/4   通过
    - ExternalMarketSnapshotTest                3/3   通过
    - ReflectionEntryTest                       3/3   通过
    - CheckpointEntryTest                       3/3   通过
    - JsonSchemaPresenceTest                    5/5   通过

  Batch 2 新增（dh-usecase）
    - NqFeedbackContractValidationTest          7/7   通过（8 eventType 合法 + 6 错误场景）
    - NqFeedbackIdempotencyTest                 3/3   通过（重放 / 不同 eventId / REJECTED 不入库）
    - NqFeedbackHandlerDispatchTest             5/5   通过（router 全覆盖 + 重复抛错 + Stage1 append + raw 保留）

  Batch 2 新增（dh-api）
    - NqFeedbackControllerWebMvcTest            7/7   通过（202/400 + outcome + trace/req/corr/job 分离 + bean 校验）

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过（Stage1-CLOSE 5 条规则保持）

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B1 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B1 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-domain  NqFeedbackEnvelopeTest             4/4   通过
  - dh-domain  NqFeedbackPayloadContractTest      8/8   通过 (8 个 payload value object)
  - dh-domain  DhBacktestRequestContractTest      5/5   通过
  - dh-domain  ForecastArtifactTest               4/4   通过
  - dh-domain  ExternalMarketSnapshotTest         3/3   通过
  - dh-domain  ReflectionEntryTest                3/3   通过
  - dh-domain  CheckpointEntryTest                3/3   通过
  - dh-domain  JsonSchemaPresenceTest             5/5   通过 (16 个 schema 存在 + 结构校验)
  - dh-usecase ResearchRunStage1ClosedLoopTest    1/1   通过 (Stage1 回归)
  - dh-usecase DecisionHubFacadeImplTest          1/1   通过 (旧链路冒烟)
  - dh-app     ArchitectureTest                   5/5   通过 (Stage1-CLOSE 5 条规则保持)

跳过：
  - dh-app     PostgresContainerSmokeTest         因当前环境无 Docker，按命令显式排除

Batch 1 范围 (零 Controller/Service/Repository/JDBC/WiringConfig 改动)：
  - 新增 dh-domain 类     30 个 (含 8 payload + 5 enum 在 feedback / payload)
  - 新增 JSON Schema       16 个 (contracts/json-schema/)
  - 新增 OpenAPI schemas   23 项 (contracts/openapi.yaml components only, 无新 path)
  - 新增测试用例           35 个 (dh-domain/src/test, 全绿)
```

## 4. 历史验收：2026-05-25 Stage2-PoC WO

```text
日期：2026-05-25
阶段：Stage2-PoC WO
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-usecase  ResearchRunStage1ClosedLoopTest   1/1 通过
  - dh-usecase  DecisionHubFacadeImplTest         1/1 通过（旧链路冒烟）
  - dh-app      ArchitectureTest                  5/5 通过（旧 1 条 + Stage1-CLOSE 新增 4 条）

跳过：
  - dh-app      PostgresContainerSmokeTest        因当前环境无 Docker，按命令显式排除

说明：本轮 Stage2-PoC WO 只修改文档，未触碰 Java/SQL/Schema，Stage1 测试矩阵保持不变。
```

## 5. 历史验收：2026-05-25 Stage1-CLOSE

```text
日期：2026-05-25
阶段：Stage1-CLOSE
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-usecase  ResearchRunStage1ClosedLoopTest   1/1 通过
  - dh-usecase  DecisionHubFacadeImplTest         1/1 通过（旧链路冒烟）
  - dh-app      ArchitectureTest                  5/5 通过（旧 1 条 + Stage1-CLOSE 新增 4 条）

跳过：
  - dh-app      PostgresContainerSmokeTest        因当前环境无 Docker，按命令显式排除
```

## 4. Stage1 必测闭环

`ResearchRunStage1ClosedLoopTest` 覆盖：

```text
创建 ResearchRun
启动 ResearchRun
生成多个 StrategyCandidate
生成 JudgeDecision
接收 NQ Feedback Event（BACKTEST positive）
更新 ExperienceEntry / PheromoneEdge（success_count +1）
接收 NQ Feedback Event（RISK negative）
更新 ExperienceEntry（failure_count +1）+ FailureCaseStore
```

## 5. 边界测试（ArchUnit，全部由 dh-app/ArchitectureTest 覆盖）

```text
✅ ..domain.. 不依赖 ..infra..
✅ ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..（Stage1-CLOSE）
✅ ..connector.nq.. 类名/方法名禁字（placeOrder/submitOrder/executeOrder/
   bypassRisk/forceExecute），DefaultNqContractVerifier 自身黑名单豁免（Stage1-CLOSE）
✅ ..usecase.agent.. 不依赖 ..providers..（Stage1-CLOSE）
✅ ..api.. 控制器 @RequestMapping 不命中 /orders|/trades|/live（Stage1-CLOSE）
✅ ..connector.tools..  不依赖 ..infra..（Stage2-PoC-B5）
✅ ..connector.research.. 不依赖 ..infra..（Stage2-PoC-B5）
✅ ..domain.{forecast,marketdata,reflection,checkpoint}.. 不依赖 ..connector..（Stage2-PoC-B5）
✅ ..usecase.agent.planner.. 不依赖 ..providers..（Stage2-PoC-B5）
✅ ..usecase.agent.feedback.. 不依赖 ..providers..（Stage2-PoC-B5）
```

## 6. 验收记录格式

每次 VERIFY 后追加：

```text
日期
阶段
命令
结果
失败原因
修复结论
剩余风险
```

## 7. 2026-05-26 Stage2-PoC VERIFY 验收记录

```text
日期       2026-05-26
阶段       Stage2-PoC VERIFY (冻结前验证)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - dh-domain   35 tests   (JsonSchemaPresenceTest 5)
           - dh-connector 9 tests   (Fake adapter/store 全绿)
           - dh-usecase  47 tests   (Stage2ClosedLoopTest 2 / ResearchRunStage1ClosedLoopTest 1)
           - dh-infra     9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
           - dh-api       7 tests   (NqFeedbackControllerWebMvcTest 7)
           - dh-app      15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
           - 总计 122 tests / 0 failures / 0 errors / 0 skipped
失败原因   无
修复结论   契约/文档不一致项已修正：
           1) contracts/openapi.yaml /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
              / 400 + NqFeedbackErrorResponse 并补两个 schema
           2) docs/current/DB_SCHEMA.md 修正 V2 文件名为 V2__dh_agent_runtime.sql
           3) docs/current/API.md 把已实现的 7 条 research-runs 端点移入 "已实现端点"
剩余风险   - PostgresContainerSmokeTest 需 Docker，留给装好 Docker 的 CI 环境
           - 真实 NQ ingest endpoint 对齐留给 FREEZE 后阶段
           - OpenAPI 中 /api/ai/research-runs 端点未落 OpenAPI，列入 Stage3 文档补丁
准入决定   GO，允许进入 Stage2-PoC FREEZE
报告       docs/current/STAGE2_POC_VERIFY_REPORT.md
```

## 8. 2026-05-26 Stage2-PoC FREEZE 验收记录

```text
日期       2026-05-26
阶段       Stage2-PoC FREEZE (冻结前最终验收)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - 总计 122 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
冻结目录   docs/gates/dh-stage2-poc/ （已创建，含冻结声明 README.md）
文档同步   README.md / AGENTS.md / docs/current/README.md / STATUS.md
           / WORKLOG.md / TESTING.md 全部对齐：
           "Current stage: Stage2-PoC FREEZE completed / Next stage: Stage3-PLAN"
本次改动   仅文档；零 Java 业务代码变更；零 NQ 仓库变更；无 Stage3 功能
准入决定   进入 Stage3-PLAN（仅规划 NQ 真实联调，不实现）
```

## 9. 2026-05-26 Stage3-PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-PLAN (文档规划阶段，无 Java 业务代码改动)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 6 份 STAGE3_*.md 规划文档 + 6 份状态文档 bump；零 Java 业务代码改动
本次范围   仅 PLAN：不接真实 NQ / Kronos / global-stock-data；不引入 TradingAgents Python；
           不实现下单 / 风控旁路 / 实盘 / 前端；不修改 NQ 仓库
准入决定   进入 Stage3-WO（按 STAGE3_WORK_ORDER.md 拆批实施）
```

## 10. 2026-05-26 Stage3-B1 Contract Alignment IMPLEMENT 验收记录

```text
日期       2026-05-26
阶段       Stage3-B1 Contract Alignment IMPLEMENT
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - dh-domain   64 tests   (Stage2 35 + 新增 4 份 contract 测试类 29 cases)
           - dh-connector 9 tests   (Fake adapter / store 全绿)
           - dh-usecase  47 tests   (Stage1ClosedLoopTest 1 / Stage2ClosedLoopTest 2 / feedback 15 等)
           - dh-infra     9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
           - dh-api       7 tests   (NqFeedbackControllerWebMvcTest 7)
           - dh-app      15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
           - 总计 151 tests / 0 failures / 0 errors / 0 skipped

新增测试   dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/
           - NqFeedbackEnvelopeSchemaContractTest      7 cases
             (schema 存在 / required 10 字段 / additionalProperties=false /
              eventType 枚举对齐 NqFeedbackEventType 8 值 / sourceSystem const /
              schemaVersion semver / 黑名单关键词为空)
           - DhBacktestRequestSchemaContractTest       7 cases
             (schema 存在 / required 14 字段 / additionalProperties=false /
              status 枚举对齐 DhBacktestRequestStatus 6 值 /
              frequency 枚举对齐 BacktestFrequency 3 值 /
              initialCapital exclusiveMinimum=0 + symbols minItems=1 / 黑名单为空)
           - BacktestResultSnapshotSchemaContractTest  6 cases
             (schema 存在 / required 9 字段 / additionalProperties=false /
              verdict 枚举对齐 BacktestVerdict 3 值 / winRate [0,1] / 黑名单为空)
           - OpenApiContractAlignmentTest              9 cases
             (openapi 存在 / 端点与 NqFeedbackController 一致 /
              outcome 枚举含 ACCEPTED + DUPLICATE /
              errorCode 枚举含 UNKNOWN_EVENT_TYPE + INVALID_SCHEMA + UNKNOWN_TRACE /
              DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot 组件存在 /
              NqFeedbackEventType 保持 8 种 /
              全文不含 placeOrder|submitOrder|executeOrder|bypassRisk|forceExecute /
              paths 段不含 /orders / /trades / /live /
              Stage3-B1 不允许在 paths 落 /api/ai/research/backtest-requests，仅注释占位)

ArchUnit   10/10 PASS（Stage1-CLOSE 5 + Stage2-PoC-B5 5；本批未新增也未放松）
失败原因   无
修复结论   - contracts/json-schema/nq-feedback-envelope.schema.json 补 description / examples，
             eventId / eventType / sourceSystem / traceId / requestId / correlationId /
             schemaVersion / payloadJson 9 字段对齐 STAGE3_CONTRACT_PLAN §1；
             不修改 required / enum / additionalProperties 等结构语义
           - contracts/json-schema/dh-backtest-request.schema.json 补 description；
             不修改 required / enum / additionalProperties
           - contracts/json-schema/dh-backtest-result-snapshot.schema.json 补 description；
             不修改 required / enum / additionalProperties
           - contracts/openapi.yaml info.description 加 Stage3-B1 硬边界声明；
             components 段保留 Stage3-B1 planned contract 注释占位；
             /api/ai/feedback/nq 端点语义不变
剩余风险   - PostgresContainerSmokeTest 需 Docker，留给装好 Docker 的 CI 环境
           - NQ 端 /api/ai/research/backtest-requests 实施由 NQ 团队后续完成（Stage3-B2/B3 规划与对接）
           - Stage3-B1 不修改任何 Handler 行为，经验沉淀路径（ExperienceEntry/PheromoneEdge/
             FailureCaseStore 写入）保留至后续 Batch 在 dh-usecase / dh-memory 实施
准入决定   进入 Stage3-B2 NQ Feedback Outbox PLAN（仅文档；NQ 仓库由 NQ 团队后续实施）
```

## 11. 2026-05-26 Stage3-B2 NQ Feedback Outbox PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B2 NQ Feedback Outbox PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_NQ_OUTBOX_SPEC.md（NQ outbox 11 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增
本次范围   仅 PLAN：声明 NQ 端 outbox SPEC（建议模块 / 表结构 / 8 触发点 / retry 矩阵 /
           audit / 5 字段语义 / HTTP 矩阵 / NQ 后续 5 个 Batch / 风险防护 / 验收）；
           不接真实 NQ / Kronos / global-stock-data；不引入 TradingAgents Python；
           不实现下单 / 风控旁路 / 实盘 / 前端；不修改 NQ 仓库；不写真实 outbox 客户端
准入决定   进入 Stage3-B3 DH Backtest Request Adapter PLAN（仅 PLAN；不写 Java；不联调真实 NQ）
```

## 12. 2026-05-26 Stage3-B3 DH Backtest Request Adapter PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B3 DH Backtest Request Adapter PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（DH backtest adapter 14 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / Kronos / global-stock-data / TradingAgents Python
本次范围   仅 PLAN：声明 DH 端 adapter SPEC（可插拔原则 10 条 + 三层 gate +
           三 client 策略 / 9 状态机 / 24h 幂等 + 8 attempt 退避 / 错误码映射 /
           result snapshot 消费 / 三段配置建议 / 8 个测试类规划 / B3-1..B3-5 五批 IMPL 拆解 /
           风险与防护）；
           不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；不实现下单 / 风控旁路 /
           实盘 / 前端；不修改 NQ 仓库；不写真实 backtest client
准入决定   进入 Stage3-B4 End-to-End Contract Test PLAN（仅 PLAN；不接实盘；不真实联调；
           联调用例 T1-T7 在 Stage3-B4 IMPLEMENT / VERIFY 阶段落地）
```

## 13. 2026-05-26 Stage3-B4 End-to-End Contract Test PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B4 End-to-End Contract Test PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（DH/NQ 端到端契约测试 11 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / 真实联调 / 实盘 / Kronos /
           global-stock-data / TradingAgents Python
本次范围   仅 PLAN：DH staging + NQ test cluster 环境规划 / 7 个联调用例 T1-T7 /
           10 类 Contract Test（JSON Schema / OpenAPI / HTTP status matrix / Error code /
           Idempotency / Retry+dead-letter / Disabled startup / No dangerous endpoint /
           Trace correlation / Regression）/ 5 字段端到端对账 + deterministic 数据 /
           三段验收命令（DH 默认 / CI Docker / Stage3 联调）/ 失败处理矩阵 +
           联调回滚预案 / B4-1..B4-5 五批 IMPL 拆解 / Stage3-PLAN-FREEZE 衔接；
           不写 Java；不联调真实 NQ；不接真实 HTTP；不接实盘；不自动下单 / 发布
准入决定   进入 Stage3-PLAN-FREEZE（评审 10 份 STAGE3_*.md 文档口径一致性；视需要冻结到
           docs/gates/dh-stage3-plan/；6 份状态文档切到 "Stage3-PLAN-FREEZE completed /
           Next: Stage3-B1 IMPLEMENT" 体例）
```

## 14. 2026-05-26 Stage3-PLAN-FREEZE 验收记录

```text
日期       2026-05-26
阶段       Stage3-PLAN-FREEZE（仅文档冻结，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
           - dh-domain 64 / dh-connector 9 / dh-usecase 47 / dh-infra 9 / dh-api 7 / dh-app 15
           - Stage1ClosedLoop / Stage2ClosedLoop / Stage3-B1 29 contract tests 全部保持回归基线
本次改动   仅文档冻结：
           - docs/current/* 33 个文件完整复制到 docs/gates/dh-stage3-plan/（含 10 份 STAGE3_*.md）
           - docs/gates/dh-stage3-plan/README.md 顶部加冻结声明（Status: FREEZE completed /
             Next: Stage3-B1 IMPLEMENT）+ 一致性核查表 + Stage3-PLAN 交付物清单
           - 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / 真实联调 / 实盘 / Kronos /
           global-stock-data / TradingAgents Python 接入
本次范围   文档冻结：10 份 STAGE3_*.md 一致性核查（9 条核心原则口径一致；无措辞修订）；
           落盘 docs/gates/dh-stage3-plan/；状态文档同步；mvn test 回归基线保持
           本轮为文档冻结，无代码实现；冻结后不得修改本目录内容
准入决定   进入 Stage3-B1 IMPLEMENT（Stage3-B1 Contract Alignment IMPLEMENT 已于 2026-05-26 完成；
           Stage3-B2/B3/B4 IMPLEMENT 单独开工，按 STAGE3_WORK_ORDER / NQ_OUTBOX_SPEC §8 /
           DH_BACKTEST_ADAPTER_SPEC §12 / E2E_CONTRACT_TEST_SPEC §8 推进）
```

## 15. 2026-05-26 Stage3-B3 DH Backtest Request Adapter IMPL 验收记录

```text
日期       2026-05-26
阶段       Stage3-B3 DH Backtest Request Adapter IMPL
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 190 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 12/12 PASS（新增 R11 HTTP 客户端隔离 / R12 backtest 端口隔离）
           - 151 → 190（+39 new tests）

本次新增（Java 业务代码）：
  dh-usecase (新建 com.guidinglight.decisionhub.usecase.agent.backtest 包)
    - DhBacktestRequestService 端口
    - impl/DefaultDhBacktestRequestService（校验 + paramsHash sha256 + 24h 短路 +
      不抛 RuntimeException + 状态机映射）
    - DhBacktestRequestCommand（Builder 风格 12 字段）
    - DhBacktestRequestResult（4 工厂方法：accepted/duplicate/idempotentShortCircuit/disabled/failed）
    - DhBacktestRequestOutcome 枚举（6 值）
    - DhBacktestRequestErrorCode 枚举（17 值 + isRetryable）
    - DhBacktestRequestRepository 端口 + inmemory/InMemoryDhBacktestRequestRepository
  dh-connector
    - nq/NqBacktestSubmitStatus 枚举（4 值：ACCEPTED/DUPLICATE/DISABLED/FAILED）
    - nq/NqBacktestSubmitResult（5 工厂方法 + 6 字段）
    - nq/NqBacktestClient.submit(DhBacktestRequest) 默认方法（typed）
    - nq/fake/FakeNqBacktestClient typed deterministic submit
      （jobId = "fake-job-" + sha256(requestId).take(16) + Clock 可注入）
    - nq/fake/DisabledNqBacktestClient（DH gate 关闭；返回 DISABLED 不抛异常）
  dh-app
    - config/NqBacktestClientProperties (@ConfigurationProperties)
    - config/Stage3NqBacktestWiringConfig（互斥 SpEL 三层 gate 装配）
    - config/AgentRuntimeWiringConfig 移除 nqBacktestClient bean（由 Stage3 接管）

本次新增（测试，共 8 个测试类 39 cases）：
  dh-connector
    - FakeNqBacktestClientTest                                              5
    - DisabledNqBacktestClientTest                                          5
  dh-usecase
    - DhBacktestRequestServiceTest                                          5
    - DhBacktestRequestIdempotencyTest                                      3
    - DhBacktestResultSnapshotConsumptionTest                               4
  dh-app
    - RealNqBacktestClientDisabledByDefaultTest                             4
      （4 profile case 全覆盖装配真值表）
    - NoNqDependencyStartupTest                                             4
      （默认 profile / Spring context / 无 HTTP 客户端 bean / 零 Spring 手动构造）
  dh-domain
    - NoDangerousEndpointContractTest                                       6
      （openapi.yaml 无危险关键词 + paths 无危险段 + 16 schemas 无危险关键词 +
       DhBacktestRequestStatus / NqFeedbackEventType 无危险前缀 + 事件类型保持 8 值）
  dh-app ArchitectureTest                                                   2 (新增 R11 / R12)

本次范围：
  - 仅 DH-side IMPL；Fake / Disabled 装配；零真实 HTTP；零 RealNqBacktestClient
  - Stage3-B3 was executed before B2 because B2 touches NQ
  - NQ repository remains unchanged
  - 零 contracts/openapi.yaml 修改；零 contracts/json-schema 修改；
    零 Flyway migration 新增；零 OpenAPI path 新增

准入决定   Stage3-B3 IMPL completed；
           Next: Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE
           or isolated branch approval。
```

## 16. 2026-06-06 DH Codex Workflow Rules 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW（仅文档规则固化）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的文档与规则文件变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向 rg 检查
结果       已执行；检查 nq-dh-workflow-router active skill、router skill 文件存在、
           CODEX_PROJECT_INSTRUCTIONS 前置分类规则、Findings 标准字段、Summary 非必填、
           integration 未写成 started / completed、无新增业务代码路径
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且禁止修改业务代码
准入决定   Codex workflow routing 规则固化完成；NQ integration not started；
           Integration-0 not started / plan only
```

## 17. 2026-06-06 DH Codex Workflow Conflict Cleanup 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW-CLEANUP（仅 Markdown / Skill 文档冲突修复）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的文档与规则文件变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向文本检查
结果       已执行；检查 ROADMAP / WORK_ORDER next 已统一为 Integration-0-PLAN；
           Stage2-PoC / 真实 NQ client / HTTP / event / backtest request 已标记为
           historical / superseded / deferred / forbidden；
           workflow 文档包含完整排除目录、细凭证禁令、开工前范围字段和
           archived / historical / superseded 文档不作为当前事实源规则
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且只修改 Markdown / Skill 文档，
           未修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径
准入决定   下一步只允许 Integration-0-PLAN；NQ integration not started；
           Integration-0 not started / plan only
```

## 18. 2026-06-06 DH Codex Workflow Final Cleanup 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW-FINAL-CLEANUP（仅 Markdown 文档口径修复）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的 Markdown / Skill 文档变更，
           未出现业务代码、API、migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向文本检查
结果       已执行；检查 AGENTS.md / README.md / docs/current/README.md 不再把 Stage3-B2
           写成当前 next；docs/current/DH_NQ_INTEGRATION.md 已把 REST API /
           POST /api/ai/backtest-requests / 真实 HTTP / event / NQ client /
           RealClient / real provider 标记为 historical / superseded / deferred / gated
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且只修改 Markdown，
           未修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径
准入决定   下一步只允许 Integration-0-PLAN；NQ integration not started；
           Integration-0 not started / plan only
```

## 19. 2026-06-11 DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION 验收记录

```text
日期       2026-06-11
阶段       DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION（仅事实源文档同步）
命令       git status --short
结果       已执行；仅命中本轮同步的 docs/current/{STATUS,README,ROADMAP,WORKLOG,TESTING}.md
           与 AGENTS.md，无业务代码、API、migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；用于核对改动集中在事实源 Markdown 文件
全量测试   未执行；任务类型为 DOCUMENTATION，仅改 Markdown，未修改 Java、契约、migration 或部署代码
失败原因   无
修复结论   不适用
剩余风险   阶段口径误写风险已通过禁止项控制：未把 Integration-0 写成真实集成；
           未把 NQ integration 写成 started；未把 DH 写成 integrated；未把 LIVE 写成 enabled
准入决定   DH Next 仍为 Integration-0-PLAN；NQ-DH not integrated；
           Integration-0 = contract / mock / docs work line, not runtime integration；
           P1-4 残留阻塞 Integration-1，不阻塞 Integration-0
```

## 20. 2026-06-11 NQ-DH-INTEGRATION-0-CONTRACT-FREEZE 验收记录

```text
日期       2026-06-11
阶段       NQ-DH-INTEGRATION-0-CONTRACT-FREEZE（DOCUMENTATION + CONTRACT DESIGN）
新增       docs/current/DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md
           docs/current/DH_NQ_INTEGRATION0_SECURITY_POLICY.md
           docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md
修改       docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮新增/修改的 docs/current Markdown，无业务代码、契约代码、
           migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 docs/current/DH_NQ_INTEGRATION0_*.md 与 README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs + contract design only，未修改 Java、contracts/ schema、
           migration、测试代码或部署脚本
失败原因   无
修复结论   不适用
剩余风险   文档契约与未来代码实现可能脱节，后续必须用 contract test 固化；
           本轮未实现集成、未接真实 HTTP / RealClient / 真实 Provider、未开启 LIVE
准入决定   下一步只允许 Integration-0 mock / contract test 设计或安全文档固化，禁止真实联调；
           真实通道必须等 Integration-1 并先修复 DH P1-4 残留
           （rate limit / memory cap / replay nonce 持久化）
```

## 21. 2026-06-11 NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN 验收记录

```text
日期       2026-06-11
阶段       NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN（DOCUMENTATION + CONTRACT TEST DESIGN）
修改       docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md（新增详细矩阵 §6-§12）
           docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮修改的 docs/current Markdown，无业务代码、contracts schema、
           migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md 与 README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs + contract test design only，未写测试代码，未修改 Java、
           contracts schema、migration 或部署脚本
代码文件   未创建；futureCodeLocationSuggestion 仅为建议路径，未创建任何 .java / 测试 / fixture 文件
失败原因   无
修复结论   不适用
剩余风险   文档测试矩阵与未来测试代码可能脱节，后续 NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL
           必须按本矩阵固化；本轮未实现集成、未接真实 HTTP / RealClient / 真实 Provider、未开启 LIVE
准入决定   下一步可进入 contract test 代码实现（草案，只加测试与 fixture、走 Fake/Disabled，
           不接真实通道）；真实通道必须等 Integration-1 并先修复 DH P1-4 残留
```

## 24. 2026-06-12 DH-P1-4-RESIDUAL-FIX-PLAN 验收记录

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-PLAN（DOCUMENTATION + FIX_PLAN）
新增       docs/current/DH_P1_4_RESIDUAL_FIX_PLAN.md
修改       docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short / git diff --check / git diff --stat
结果       已执行；仅命中 docs/current Markdown；无 whitespace error；无业务/测试代码改动
全量测试   未执行；本轮 docs-only fix planning，未改 Java、测试代码、API、migration、provider、NQ client
失败原因   无
修复结论   不适用（本轮仅出方案，未修复任何缺口）
剩余风险   方案与未来实现可能漂移；rate limit 阈值、fail-closed、TTL 优先于驱逐等须在实现阶段用测试固化
准入决定   Integration-1 仍 NOT STARTED；P1-4 未修复前禁止真实只读通道 / 真实 HTTP / RealClient；
           下一步 DH-P1-4-RESIDUAL-FIX-REVIEW 或 DH-P1-4-RESIDUAL-FIX-IMPL，不得直接 Integration-1
```

## 23. 2026-06-12 NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE 验收记录

```text
日期       2026-06-12
阶段       NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE（DOCUMENTATION + ACCEPTANCE_REPORT）
新增       docs/current/DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md
修改       docs/current/STATUS.md / README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮新增/修改的 docs/current Markdown
命令       git diff --check
结果       已执行；无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 acceptance report 与 STATUS/README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs-only，未改业务/测试代码；验收依据引用上一轮 mvn test BUILD SUCCESS
           （dh-domain 86 tests / 0 failures，Integration-0 16 passed，ArchitectureTest 12 条全绿）
失败原因   无
修复结论   不适用
验收结论   Integration-0 PASS / CLOSED / ACCEPTED；Runtime integration / Integration-1 / AI NOT STARTED；
           DH NOT INTEGRATED；LIVE DISABLED
剩余风险   Integration-0 只证明 contract / test-only 安全边界，不证明真实通道安全；
           Integration-1 前必须修复 DH P1-4 residual 并重跑 contract tests
准入决定   下一步只允许 Integration-1 planning-only audit / DH P1-4 residual fix planning /
           NQ GateK-PLAN 文档规划；禁止直接真实联调
```

## 22. 2026-06-12 NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL 验收记录

```text
日期       2026-06-12
阶段       NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL（TEST_CODE_CHANGE）
新增       dh-domain/src/test/java/.../integration0/support/（9 个 test-only helper）
           dh-domain/src/test/java/.../integration0/（3 个测试类，16 用例覆盖 INT0-T01..T15）
           dh-domain/src/test/resources/integration0/（10 个脱敏 fixture JSON）
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿；DhNqIntegration0*Test 16 passed / 0 failed；
           ArchitectureTest（ArchUnit 12 条）全绿；PostgresContainerSmokeTest 因无 Docker skip（既有）
命令       mvn -pl dh-domain -am -Dtest='DhNqIntegration0*Test' -Dsurefire.failIfNoSpecifiedTests=false test
结果       16 tests / 0 failures / 0 errors
命令       git diff --check
结果       已执行；无 whitespace error
命令       git status --short
结果       已执行；仅命中 dh-domain/src/test/**（测试代码与 fixtures）
生产代码   未修改 src/main；未新增 API / migration / Controller / Service / Repository / DTO / RealClient / 真实 Provider
真实通道   未做真实 HTTP / 真实 NQ / 真实交易所；未读取真实密钥（固定假值）；未开启 LIVE
失败原因   无
剩余风险   nonce store 为 test-only 内存实现，不代表真实通道安全；Integration-1 前必须补
           持久化 nonce、rate limit、memory cap（DH P1-4 residual）
准入决定   下一步进入 Integration-0 contract test implementation review / safety gate review，
           不得直接真实联调
```

## 23. 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1 验收记录（replay nonce persistence）

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1（CODE_CHANGE + SECURITY_FIX + DB_MIGRATION）
范围       只实现 replay nonce persistence；不实现 rate limit / memory cap / header alignment
新增测试   dh-security NonceReplayGuardTypeTest（5 用例）
             - explicit_jdbc_is_allowed_under_any_profile
             - in_memory_guard_only_dev_test（非 dev/test 显式 in-memory 启动失败）
             - missing_config_does_not_fallback_to_unsafe_in_memory（缺配置非 dev/test 默认 jdbc）
             - unknown_guard_type_fails_closed
             - isDevOrTest_detects_dev_or_test_profiles_only
           dh-infra JdbcNonceReplayGuardTest（5 用例，Mockito，无需 Docker）
             - markIfAbsent insert 1 -> true 且 SQL 含 on conflict (replay_key) do nothing
             - markIfAbsent insert 0 -> false（replay detected）
             - replay_store_unavailable_fails_closed（DataAccessException -> false，无 secret/payload 拼进 SQL）
             - cleanup_runs_before_mark_when_enabled / cleanup_skipped_when_disabled
           dh-infra JdbcNonceReplayGuardPersistenceTest（3 用例，Testcontainers，需 Docker）
             - persistent_nonce_rejects_replay_after_restart_simulation
             - persistent_nonce_is_source_tenant_request_scoped
             - cleanup_removes_expired_rows_only
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿；INT0-T01..T15（DhNqIntegration0*Test 16 用例）未被破坏；
           NonceReplayGuardTypeTest 5 / JdbcNonceReplayGuardTest 5 全绿；
           JdbcNonceReplayGuardPersistenceTest 因无 Docker 整类 skip（3）；PostgresContainerSmokeTest 无 Docker skip（既有）
说明       CI 需要 Docker 才能验证持久化 nonce 的 restart 语义（JdbcNonceReplayGuardPersistenceTest），
           本机无 Docker 时按 @Testcontainers(disabledWithoutDocker=true) 既有策略跳过。
命令       mvn -Pquality validate
结果       FAILURE，来源为既有/环境问题，非本轮改动：聚合模块 checkstyle 读 suppressions.xml 网络超时；
           spotless:check 在多个**未改动**既有文件即报 format 违规（基线本身不干净）。
           已对本轮自有文件单独 spotless:apply（-DspotlessFiles 限定），未触碰未改动文件。
命令       git diff --check / git status --short
结果       无 whitespace error；改动仅落在 dh-app/dh-infra/dh-security 允许范围与 docs/current
真实通道   未做真实 HTTP / 真实 NQ / 真实交易所；未读取真实密钥（固定假值）；未开启 LIVE；未接 AI
失败原因   无
剩余风险   replay nonce 已持久化，但 rate limit / memory cap 仍残留（P1-4 未全部关闭）；
           DB 不可用时 guard fail-closed（拒绝），属预期保护，需运维保障 DB 可用
准入决定   Integration-1 仍 NOT STARTED；下一步 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（bounded memory cap），
           不得直接真实联调
```

## 25. 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2 验收记录（bounded memory cap）

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（CODE_CHANGE + SECURITY_FIX + TEST_CODE_CHANGE）
范围       只实现 bounded memory cap；不实现 rate limit / header alignment；不启动 Integration-1
本次改动（main）
  - dh-security InMemoryNonceReplayGuard 改为有界：maxEntries 全局上限 + TTL；
    markIfAbsent 先 TTL 清理 -> 命中拒绝 -> 满容量 fail-closed（绝不驱逐未过期 key）；
    有效期取 laterOf(认证层 expiresAt, now+ttl) 只延长不缩短防重放窗口；非法配置构造抛 IllegalArgumentException
  - dh-usecase InMemoryNqFeedbackEventRepository 改为有界：maxEvents 全局 + perTenantMaxEvents +
    retention TTL；append 前先 TTL 清理再按 tenant/global 驱逐最老项；envelope 幂等 + 有界；
    按域不变量与项目规范保留原始 payloadJson，不新增 secret/token 存储；非法配置构造抛 IllegalArgumentException
  - dh-app SecurityWiringConfig / AgentRuntimeWiringConfig 注入保守默认上限/TTL；application.yml 新增
    replay.in-memory.{max-entries,ttl-seconds} 与 feedback-store.{max-events,per-tenant-max-events,retention-seconds}
新增测试
  - dh-security BoundedInMemoryNonceReplayGuardTest（7）：memory_cap_rejects_overflow /
    ttl_cleanup_allows_new_nonce_after_expiry / does_not_evict_unexpired_nonce_window /
    replay_same_key_still_rejected / ttl_floor_never_shortens_replay_window /
    bad_config_fails_closed / default_constructor_is_bounded
  - dh-security NqFeedbackPayloadSizeGateTest（2）：payload_64kib_gate_remains_valid /
    payload_at_64kib_passes_size_gate
  - dh-usecase BoundedInMemoryNqFeedbackEventRepositoryTest（7）：rejects_or_bounds_overflow /
    ttl_cleanup_removes_expired_events / is_per_tenant_bounded /
    existing_query_semantics_preserved_sorted_by_received_at / save_envelope_is_idempotent_and_bounded /
    bad_config_fails_closed / default_constructor_is_bounded
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS；7 模块全 SUCCESS；全仓回归全绿
           - dh-domain 86（INT0-T01..T15 = DhNqIntegration0*Test 16 用例未被破坏）
           - dh-security 26（Bounded 7 + PayloadSizeGate 2 + 既有）
           - dh-usecase 66（Bounded repo 7 + 既有）
           - dh-app 25（ArchitectureTest 12 / NoNqDependencyStartup 4 等，装配通过）
           - 本次 runner 恰好有 Docker：JdbcNonceReplayGuardPersistenceTest 3 用例真实跑通（未 skip）
命令       mvn -Pquality validate
结果       未在本轮重跑/未强制修复；既有 quality-baseline 问题（checkstyle DTD 网络 + spotless 基线）
           已登记为独立任务 DH-QUALITY-BASELINE-CLEANUP；本轮未顺手修未改动文件
命令       git diff --check / git status --short
结果       无 whitespace error（仅 LF→CRLF 提示）；改动仅落在 dh-app/dh-security/dh-usecase 允许范围与 docs/current
边界       未修改 NQ；未实现 rate limit；未做 header alignment；未新增 API / migration / RealClient /
           真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所调用；未接 AI；未开启 LIVE；未读取真实密钥
剩余风险   bounded in-memory 仅适合 dev/test 或单实例辅助路径，真实通道仍应用 JdbcNonceReplayGuard；
           容量满时的保护性拒绝属 fail-closed（拒绝而非放行），需运维监控容量与 TTL
准入决定   Integration-1 仍 NOT STARTED；P1-4 仍未全部关闭（rate limit 残留）；
           下一步 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2-REVIEW，再进入 Batch 3 rate limit，不得直接真实联调
```

### 25.1 2026-06-13 Batch 2 flaky 测试修复复验

```text
日期       2026-06-13
现象       2026-06-13 运行 mvn test 时 dh-usecase 失败 1 例：
           BoundedInMemoryNqFeedbackEventRepositoryTest.existing_query_semantics_preserved_sorted_by_received_at
           AssertionFailedError: expected:<3> but was:<1>（surefire-reports 已确认）
根因       该用例用默认构造（真实 Clock.systemUTC()），事件 receivedAt 固定为 T0=2026-06-12，
           默认 retention=24h。墙钟跨过 T0+24h 后，每次 append 的 TTL 清理把先前事件按 retention 过期驱逐，
           只剩最后一条 -> 时间依赖（flaky）用例，非 bounded memory cap 生产语义缺陷
修复       仅改该测试：注入固定 MutableClock(T0)（与同类其余用例一致），事件 age 远小于 retention，
           三条均保留，仅验证排序语义；生产代码与 retention/上限语义未改（test-only，+5/-1）
命令       mvn test（root，全 19 模块）
结果       BUILD SUCCESS；BoundedInMemoryNqFeedbackEventRepositoryTest 7/7、
           BoundedInMemoryNonceReplayGuardTest 7/7、NqFeedbackPayloadSizeGateTest 2/2、
           DhNqIntegration0*（INT0）6+2+8=16/16 全绿
命令       mvn -Pquality validate（root）
结果       BUILD SUCCESS（本轮未引入 quality 违规）
命令       git diff --check / git diff --stat
结果       无 whitespace error；1 file changed, 5 insertions(+), 1 deletion(-)
边界       未修改 NQ；未实现 rate limit；未做 header alignment；未新增 API / migration / RealClient /
           真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取真实密钥
准入决定   Integration-1 仍 NOT STARTED；P1-4 仍未全部关闭（rate limit 残留）
```

## 26. 2026-06-13 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3 验收记录（inbound rate limit / 429）

```text
日期       2026-06-13
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3（CODE_CHANGE + SECURITY_FIX + TEST_CODE_CHANGE）
范围       只实现 NQ feedback 入站 rate limit / 429；不做 header alignment；不启动 Integration-1；不做真实联调
本次改动（main）
  - dh-security 新增 RateLimiter（端口）/ RateLimitResult（独立结果模型，不污染 HMAC authenticator）/
    InMemoryRateLimiter（固定窗口 + maxKeys 有界 + 窗口 TTL 清理优先 + 满容量 fail-closed + 非法配置启动失败）
  - dh-api NqFeedbackController：限流前置于 HMAC authenticator；key=source+tenant+route；超限 429 + errorCode
    RATE_LIMITED；审计 log.warn(auditCode/tenant/source/route/traceId，不含阈值/窗口/计数/密钥）；
    保留 HMAC/timestamp/nonce/replay 409/payload 413/成功 202 既有语义
  - dh-app SecurityWiringConfig 注入 RateLimiter bean（保守默认 window=1s / max-requests=20 / max-keys=10000；
    非法配置启动失败）；application.yml 新增 rate-limit.{window-seconds,max-requests,max-keys}
  - 配置加固（P2-1）：feedback-store.per-tenant-max-events 默认 10000 -> 1000（< 全局上限），
    仅改默认值（application.yml + AgentRuntimeWiringConfig @Value），不改 bounded memory cap 主逻辑
新增测试
  - dh-security InMemoryRateLimiterTest（6）：returns_limited_after_threshold / tenant_source_isolated /
    store_is_bounded_fail_closed（maxKeys=1）/ ttl_cleanup_allows_new_key_after_window /
    invalid_config_fails_closed / check_requires_no_secret_or_payload（no_credential_access 契约）
  - dh-api NqFeedbackRateLimitWebMvcTest（3）：returns_429_with_rate_limited /
    does_not_leak_internal_thresholds（429 body 无 window/maxRequests/maxKeys/count/threshold/retry-after/secret）/
    rate_limited_request_does_not_invoke_downstream_ingestion（no_trading_side_effect 代理）
  - 既有 NqFeedbackControllerWebMvcTest 更新构造器（注入宽松 limiter），15/15 仍全绿；
    payload 64KiB gate 由 NqFeedbackPayloadSizeGateTest（2）+ 既有 413 用例保持有效
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿
           - InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackControllerWebMvcTest 15/15
           - NqFeedbackPayloadSizeGateTest 2/2、BoundedInMemoryNonceReplayGuardTest 7/7、
             BoundedInMemoryNqFeedbackEventRepositoryTest 7/7（Batch 2 未破坏）
           - INT0-T01..T15 = DhNqIntegration0*（6+2+8）16/16 未破坏；ArchUnit 全绿
           - 无 Docker 的 runner：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1
             按 disabledWithoutDocker 跳过（非失败）
命令       mvn -Pquality validate
结果       BUILD SUCCESS（本轮未引入 quality 违规）
命令       git diff --check / git status --short
结果       无 whitespace error（仅 LF→CRLF 提示）；改动仅落在 dh-api/feedback + dh-api/test + dh-app/config +
           application.yml + dh-security/nq + dh-security/test 允许范围与 docs/current
边界       未修改 NQ；未做 header alignment；未新增 API 路径；未新增 migration / RealClient / 真实 Provider；
           未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥
剩余风险   in-memory limiter 仅适合 dev/test 或单实例辅助路径，真实多实例需集中式（Redis）limiter（另起任务）；
           容量满 / 超阈值的保护性拒绝属 fail-closed，需运维监控阈值与命中率；阈值需按真实流量调优
准入决定   Integration-1 仍 NOT STARTED；P1-4 三项残留实现均已落地（replay nonce / memory cap / rate limit），
           但 P1-4 未标记全部关闭——须先 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3-REVIEW，再
           DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 单独验收后方可关闭；不得直接进入 Integration-1
```

## 27. 2026-06-13 DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 验收记录（P1-4 整体回归收口）

```text
日期       2026-06-13
阶段       DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE（REGRESSION_VALIDATION + SECURITY_REVIEW + DOCUMENTATION）
范围       只做 P1-4 三项修复整体回归收口验收 + 文档关闭口径；不新增功能；不改生产代码 / 测试代码
本轮改动   仅文档：STATUS.md（§1.1 残留项指向关闭 + 新增 §1.3 CLOSED 记录）、ROADMAP.md、README.md、
           DH_P1_4_RESIDUAL_FIX_PLAN.md、TESTING.md（本节）、WORKLOG.md
回归制品核验（只读）
  - Batch1 replay nonce：JdbcNonceReplayGuard 存在；MARK_SQL = INSERT ... ON CONFLICT (replay_key) DO NOTHING；
    DataAccessException -> return false（fail-closed -> 409）；异常只记 ex.getClass().getName()（不记 replay_key/
    nonce/payload/secret）；V4 dh_nq_replay_nonce 表 if not exists、不动 V1–V3、无凭证列；NonceReplayGuardType.select
    缺省非 dev/test -> JDBC，非 dev/test 显式 in-memory -> IllegalStateException（启动失败）
  - Batch2 memory cap：InMemoryNonceReplayGuard maxEntries + TTL、满容量不驱逐未过期 key、fail-closed；
    InMemoryNqFeedbackEventRepository global + per-tenant + retention 上限、TTL 清理优先、非法配置启动失败；
    payload 64KiB gate 不受影响（per-tenant 默认 Batch3 已加固为 1000 < 全局上限）
  - Batch3 rate limit：RateLimiter / RateLimitResult / InMemoryRateLimiter 存在；限流前置于 HMAC；
    key=source+tenant+route（缺失项归一化为占位符，不合并成无界公共 key）；超限 429 RATE_LIMITED；
    429 不泄露阈值/窗口/计数/secret/签名材料；限流短路下游 ingestion；HMAC/timestamp/nonce/replay/payload 语义不变
命令       mvn test
结果       BUILD SUCCESS；关键测试全绿
           - JdbcNonceReplayGuardTest 5/5、NonceReplayGuardTypeTest 5/5、HmacNqFeedbackAuthenticatorTest 6/6
           - BoundedInMemoryNonceReplayGuardTest 7/7、BoundedInMemoryNqFeedbackEventRepositoryTest 7/7
           - NqFeedbackPayloadSizeGateTest 2/2、InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3
           - NqFeedbackControllerWebMvcTest 15/15、DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16、ArchUnit 全绿
           - 本机无 Docker：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1 按
             disabledWithoutDocker skip（非失败）；**持久化 nonce restart 语义仍需 Docker CI 独立验证，未在本机实跑**
命令       mvn -Pquality validate
结果       BUILD SUCCESS（本轮未引入 quality 违规；仅改文档）
命令       git diff --check / git status --short
结果       无 whitespace error；本轮改动仅文档（docs/current/*），无代码 / 测试改动
关闭口径   DH P1-4 residual: CLOSED（replay nonce persistence: closed / memory cap: closed / rate limit: closed）
           Integration-1: NOT STARTED；Runtime integration: NOT STARTED；DH: NOT INTEGRATED；
           AI: NOT STARTED；LIVE: DISABLED；header alignment: NOT DONE
边界       未修改 NQ；未做 header alignment；未新增 API / migration / RealClient / 真实 Provider；
           未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥
剩余风险   in-memory limiter / in-memory guard 单实例局限，真实多实例需集中式（Redis）；fail-closed 保护性拒绝需监控；
           固定窗口边界突发；阈值需按真实流量调优；持久化 nonce restart 语义待 Docker CI 验证
准入决定   P1-4 CLOSED 仅表示 Integration-1 前置安全缺口关闭，不等于允许真实联调；Integration-1 仍 NOT STARTED；
           下一步只允许 DH-CI-PERSISTENT-NONCE-IT-ENABLE 或 DH-NQ-HEADER-ALIGNMENT-PLAN，不得直接 Integration-1 runtime
```

## 28. 2026-06-13 DH-CI-PERSISTENT-NONCE-IT-ENABLE（Docker CI 实跑持久化 nonce IT）

```text
日期       2026-06-13
阶段       DH-CI-PERSISTENT-NONCE-IT-ENABLE（CI_TEST_ENABLEMENT + SECURITY_VALIDATION）
范围       让带 Docker 的 CI runner 真实运行 Testcontainers IT，验证 persistent replay nonce 的 restart 语义；
           不改业务生产代码、不改测试逻辑、不接真实 NQ / 真实 HTTP / 不启动 Integration-1
本轮改动
  - 新增 .github/workflows/ci.yml（GitHub Actions）：ubuntu-latest（预装并运行 Docker）+ JDK 21（temurin）+
    maven cache；步骤：docker info -> ./mvnw -B -ntp test -> 断言 IT 未被 skip -> 上传 surefire 报告
  - 未修改 JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest：保留
    @Testcontainers(disabledWithoutDocker = true)，使本地/无 Docker 仍优雅 skip、CI 有 Docker 实跑

测试门控（关键区分）
  - JdbcNonceReplayGuardPersistenceTest（dh-infra，3 用例）：
      local no Docker  -> skipped（disabledWithoutDocker；本机已确认 Tests run: 3, Skipped: 3）
      CI with Docker   -> 实跑（workflow assert 步骤强制 Skipped: 0，否则 CI 失败）
  - PostgresContainerSmokeTest（dh-app，1 用例）：
      local no Docker  -> skipped
      CI with Docker   -> 实跑（同上 assert 守护）

本机验证（无 Docker）
  命令   mvn -pl dh-infra -am -Dtest=JdbcNonceReplayGuardPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test
  结果   BUILD SUCCESS（exit 0）；JdbcNonceReplayGuardPersistenceTest Tests run: 3, Skipped: 3（无 Docker 优雅 skip，符合预期）
  命令   git diff --check
  结果   无 whitespace error
  YAML   PyYAML 本机不可用，已做结构核验：无 Tab 缩进、顶层键（name/on/permissions/concurrency/jobs）与 6 个 step 结构正确

CI Docker 实跑结果（2026-06-14，已确认；GitHub Actions run 27485958120，结论 success）
  - JdbcNonceReplayGuardPersistenceTest：Tests run: 3, Failures: 0, Errors: 0, **Skipped: 0**, 8.317s
    —— persistent nonce restart 语义（重建 guard 复用同一持久化存储后窗口内重放仍被拒）经真实 PostgreSQL(postgres:17) 验证。
  - PostgresContainerSmokeTest：Tests run: 1, Failures: 0, Errors: 0, **Skipped: 0**, 6.645s（全 app 上下文加载成功）。
  - assert 步骤打印 `OK (executed, 0 skipped)` × 2；`[INFO] BUILD SUCCESS`；job success。
  - 迭代历程：首跑 mvnw 缺执行位（chmod 后仍）wrapper jar 损坏 -> 改用 runner 预装 mvn（fd522ce）；
    再暴露 PostgresContainerSmokeTest 因 Flyway 缺 PostgreSQL 模块报 "Unsupported Database: PostgreSQL 17.10"
    -> 补 flyway-database-postgresql（841354d，用户授权的生产级修复）-> CI 整体全绿。
  - 注意：本条为"配置完成 + 预期"，**首次 push/PR 触发 CI 前不得记为已 executed/passed**

边界       未修改 NQ；未新增业务 Java 生产代码；未新增 API / migration；未做 header alignment；
           未新增 RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；
           未启动 Integration-1；未把 DH 写成 integrated；未读取或输出真实密钥
风险       Testcontainers 依赖 Docker daemon；CI runner 须能拉取 postgres:17（网络 / 镜像源）；
           首次运行有镜像拉取耗时；私有 runner 若无 Docker 需另行启用
准入决定   CI 已确认整体全绿（run 27485958120 success）；persistent nonce restart 语义经真实 PG17 在 CI 实跑通过。
           Integration-1 仍 NOT STARTED；不改变 P1-4 CLOSED 口径（CI 实跑只是补强证据，非改变结论）；
           下一步 DH-NQ-HEADER-ALIGNMENT-PLAN 或 DH-CONFIG-CREDENTIAL-DEFAULTS-GOVERNANCE，不得直接 Integration-1 runtime
```

## 29. 2026-06-14 DH-NQ-HEADER-ALIGNMENT-PLAN（planning-only，未跑测试）

```text
日期       2026-06-14
阶段       DH-NQ-HEADER-ALIGNMENT-PLAN（INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION）
范围       只读核查 DH + NQ header 用法 + 输出对齐方案文档；不改运行代码 / 测试
为何未跑   本轮纯文档（planning-only），未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；
           按 CLAUDE 文档任务纪律记录未跑原因。header alignment 的测试将于 IMPL-BATCH-* 实施时新增并验证。
只读核查   DH 生产 controller 用 legacy X-DH-NQ-*（4：Source/Timestamp/Nonce/Signature）；
           DH INT0 fixture + 两仓 docs + NQ INT0 fixture 用 canonical X-NQ-DH-*；NQ 无生产 header 处理代码；
           HmacNqFeedbackAuthenticator 签名 value-based（不含 header name）-> 改名不漂移。
规划测试   见 DH_NQ_HEADER_ALIGNMENT_PLAN.md §6（accepts_canonical / legacy_compat / conflict_fail_closed /
           signature_after_normalization / keeps_payload_64kib / keeps_nonce_replay / keeps_rate_limit_key /
           keeps_tenant_binding / keeps_INT0_T01_to_T15 / no_real_http / no_credential_access 等）。
边界       未修改 Java；未修改测试；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未接 AI；
           未开启 LIVE；未启动 Integration-1；未跨仓写 NQ；未读取或输出真实密钥。
准入决定   header alignment 仍 NOT STARTED（仅 PLAN）；Integration-1 仍 NOT STARTED；
           下一步 DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW，通过后 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1。
```

## 30. 2026-06-14 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1 验收记录（内部结构 skeleton）

```text
日期       2026-06-14
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1（CODE_CHANGE + CONTRACT_REFACTOR + TEST_REVIEW）
范围       内部结构：集中 header 常量 + parser + 归一化模型 + validator skeleton；不切 canonical、不改对外行为
新增（main）NqDhHeaderNames / NormalizedNqDhHeaders / NqDhHeaderParser（legacy-only）/ NqDhHeaderValidator(skeleton) /
           NqDhHeaderValidationResult（均 dh-security/security/nq）
新增（test）NqDhHeaderNamesTest(2) / NqDhHeaderParserTest(3) / NqDhHeaderValidatorTest(2)
修改（main）NqFeedbackController：去 magic string，经 parser 读取 legacy header 产出归一化模型；构造器不变、行为不变
命令       mvn test
结果       BUILD SUCCESS（exit 0）
           - 新单测：NqDhHeaderNamesTest 2/2、NqDhHeaderParserTest 3/3、NqDhHeaderValidatorTest 2/2
           - 行为不变回归：NqFeedbackControllerWebMvcTest 15/15、NqFeedbackRateLimitWebMvcTest 3/3、
             NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（未引入 quality 违规）
命令       git diff --check
结果       无 whitespace error
安全自查   不记录 raw signature / signature material / secret / token / full body；归一化模型 toString 对 signature
           脱敏（[REDACTED]，单测固化 model_toString_does_not_leak_signature）
边界       未修改 NQ；未切 canonical-only；未移除 legacy；未实现双接收；未新增 API / migration；未真实 HTTP /
           真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   header alignment 整体仍 NOT COMPLETED（canonical-only 未切换）；Integration-1 仍 NOT STARTED；
           下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1-REVIEW，通过后 Batch 2（canonical 读取）
```

## 31. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-DOC-RECONCILE（纯文档，未跑测试）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-DOC-RECONCILE（DOCUMENTATION + INTEGRATION_CONTRACT_RECONCILE）
范围       仅将 header alignment 文档口径统一到 canonical-only；改 DH_NQ_HEADER_ALIGNMENT_PLAN.md / README.md / TESTING.md / WORKLOG.md
为何未跑   本轮纯文档，未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；按 CLAUDE 文档任务纪律记录未跑原因。
           回归基线沿用 BATCH-1 验收（2026-06-14）：mvn test BUILD SUCCESS / mvn -Pquality validate BUILD SUCCESS / INT0 16/16，未受文档改动影响。
验证       git status --short（仅 4 份 docs 改动 + 4 个会话起始即存在的未跟踪杂散文件）；git diff --check 无 whitespace error；git diff --stat 仅 docs/current/*.md。
口径       canonical-only：无兼容期、无双接收；Batch 2=canonical-only 读取；Batch 3=Tenant/Request/Trace binding 一致性校验（HEADER_BINDING_MISMATCH）；
           legacy 仅历史引用、生产在 Batch 2 前仍读 legacy；timestamp 格式分歧另列，不在本轮。
边界       未改 Java；未改测试；未切 canonical-only 行为；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；
           未接 AI；未开启 LIVE；未启动 Integration-1；未处理 wrapper / datasource 弱口令 / timestamp 格式；未删除未跟踪文件；未读取真实密钥。
后续项     P3-2 未跟踪杂散文件（4 个，会话起始即存在）登记为待用户授权后清理，本轮不处理。
准入决定   header alignment 整体仍 NOT COMPLETED（canonical-only 未切换）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（canonical-only 读取）。
```

## 32. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2 验收记录（canonical-only 读取）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（CODE_CHANGE + CONTRACT_ALIGNMENT + SECURITY_FIX + TEST_CODE_CHANGE）
范围       入站 header 切 canonical-only X-NQ-DH-*；不做双接收 / 不兼容 legacy / 不做 binding mismatch（Batch 3）/ 不启动 Integration-1
修改（main）NqDhHeaderParser（+parseCanonical；parseLegacy 保留历史引用）、NqFeedbackController（parseLegacy->parseCanonical）
修改（test）NqDhHeaderParserTest +2；NqFeedbackControllerWebMvcTest 切 canonical +5；NqFeedbackRateLimitWebMvcTest 切 canonical
命令       mvn test
结果       BUILD SUCCESS（exit 0）
           - NqFeedbackControllerWebMvcTest 20/20（canonical 成功 + legacy-only 403 + 缺 source 403 + 缺 timestamp 401 + 缺 nonce 401 + 签名不泄露）
           - NqFeedbackRateLimitWebMvcTest 3/3（429 RATE_LIMITED 保持）
           - NqDhHeaderParserTest 5/5、NqDhHeaderNamesTest 2/2、NqDhHeaderValidatorTest 2/2、NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
缺失语义   canonical Source 缺失/不匹配->403 SOURCE_NOT_ALLOWED；Timestamp->401 TIMESTAMP_EXPIRED；Nonce->401 REPLAY_KEY_MISSING；Signature->401 BAD_SIGNATURE；replay->409；payload->413；rate limit->429；成功->202（全部保持）
安全自查   不记录 raw signature / signature material / secret / token / full body；canonical 成功路径断言响应不回显 signature / secret；HMAC value-based 不含 header name
边界       未改 NQ；未双接收；未兼容 legacy；未保留 legacy 为生产可接受 header；未移除 legacy 常量；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   生产入站现为 canonical-only；header alignment 整体仍 NOT COMPLETED（binding=Batch 3 / docs-fixtures=Batch 4 尚待）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2-REVIEW，通过后 Batch 3
```

## 34. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4 收口记录（docs/fixtures，纯文档 + 回归）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4（DOCUMENTATION + CONTRACT_FIXTURE_REVIEW + REGRESSION_VALIDATION）
范围       docs/fixtures 收口：统一 MISSING_CANONICAL_HEADER 措辞 + 明确 canonical-only / binding 已落地 + 核对 fixtures；无运行代码改动
修改       仅 docs（PLAN / README / TESTING / WORKLOG）；无 Java / fixture 业务改动
fixtures   WebMvc 成功路径全 canonical X-NQ-DH-*；唯一 legacy 在 legacy-only 负路径（刻意保留）；INT0 Int0Contract 7 canonical X-NQ-DH-*
命令       mvn test
结果       BUILD SUCCESS（回归）。WebMvc 25/25、validator 6/6、parser 5/5、rate limit 3/3、payload gate 2/2、INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate（本轮 3 次重跑）
结果       BUILD FAILURE（环境性）：checkstyle SuppressionFilter 联网解析 suppressions DTD 超时（Connection timed out），非本批所致；
           本批仅改 docs（无 Java/test），checkstyle/spotless 覆盖面同 Batch 3 最近 PASS；按规定本轮不修 checkstyle（见 DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE）
命令       git diff --check
结果       无 whitespace error
事实       MISSING_CANONICAL_HEADER=预留码（缺 canonical 由 authenticator 403/401 覆盖）；canonical-only 已落地（不接受 legacy、无兼容期、无双接收）；binding mismatch -> 403 HEADER_BINDING_MISMATCH
后续项     DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT / DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE / DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN（独立，不阻塞 close）
准入决定   header alignment 整体 READY FOR CLOSE / PENDING FINAL REVIEW（仍未 CLOSED）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW
```

## 35. 2026-06-15 DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE 验收记录（quality gate 离线 DTD 治理）

```text
日期       2026-06-15
阶段       DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE（QUALITY_GATE_FIX + BUILD_STABILITY + SECURITY_VALIDATION）
范围       仅治理 checkstyle suppressions 外部 DTD 联网解析；改 config/checkstyle/checkstyle-suppressions.xml 1 行 PUBLIC id + 文档
根因       suppressions DOCTYPE PUBLIC id=非标准 "-//Checkstyle//DTD Suppressions 1.2//EN" 不在 SuppressionsLoader 解析映射 -> 回退 SYSTEM URL 联网 -> 弱网超时 Connection timed out
修复       PUBLIC id 改为官方 "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN" -> EntityResolver 命中内置 DTD、离线解析；未删 filter/文件、未降规则、未关 checkstyle、未跳 profile（仅 1 行）
命令       mvn -Pquality validate（修复后 2 次）
结果       BUILD SUCCESS ×2（0 Checkstyle violations；spotless 通过）；网络仍不可用 -> 证明离线解析成功（修复前同会话连续 6 次 DTD 超时 FAILURE）
命令       mvn test
结果       BUILD SUCCESS。NqDhHeaderNamesTest 2/2、NqDhHeaderParserTest 5/5、NqDhHeaderValidatorTest 6/6、NqFeedbackControllerWebMvcTest 25/25、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2、INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       git diff --check
结果       无 whitespace error
行为       未改任何 Java / header alignment / controller / 签名 / 限流 / 鉴权行为；仅 checkstyle 配置元数据
准入决定   quality gate 离线稳定通过；header alignment close review 环境性阻断 UNBLOCKED；本轮不直接 CLOSED。下一步 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN
```

## 36. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN 验收记录（header alignment CLOSED）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN（REGRESSION_VALIDATION + CONTRACT/SECURITY/DOC REVIEW）
范围       离线 DTD 治理后重跑 close review；只评审 + 记录 CLOSED；未改代码/测试
命令       mvn test
结果       BUILD SUCCESS。names 2 / parser 5 / validator 6 / payload gate 2 / WebMvc 25 / rate limit 3；INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）—— 离线 DTD 治理后稳定，gate 未降低（仅 1 行 PUBLIC id；全规则集实跑）
命令       git status --short / git diff --check
结果       仅状态文档改动；无 whitespace error
确认       17/17：canonical-only 读取、binding mismatch 403 HEADER_BINDING_MISMATCH、HMAC value-based、rate limit key、payload 413、replay 409、legacy-only 403、INT0 16/16
Close      header alignment overall = CLOSED（不放开 runtime；Integration-1 / Runtime / DH integration / AI NOT STARTED；LIVE DISABLED）
后续项     DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT / DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN / Maven wrapper / datasource 弱口令（均与 close 解耦）
准入决定   CLOSED；下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT 或 GateK-PLAN
```

## 37. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（planning-only，未跑测试）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION）
范围       只读核查 X-NQ-DH-Timestamp 线缆格式 + 输出对齐方案；不改运行代码 / 测试
为何未跑   纯文档（planning-only），未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；按文档任务纪律记录未跑原因。
           回归基线沿用 header alignment CLOSE-REVIEW-RERUN（2026-06-15）：mvn test + mvn -Pquality validate BUILD SUCCESS / INT0 16/16。
只读核查   生产 Instant.parse(RFC3339)；SECURITY_POLICY §2=epoch 毫秒（冲突）；INT0 fixture=epoch 秒；CONTRACT_FREEZE/TEST_PLAN=仅 ±300s；NQ 仓库本会话不可达（未当面核对）。
决策       canonical = RFC3339 / ISO-8601 UTC（Instant.toString() 规范形）；窗口 ±300s 不变；HMAC value-based 不改；nonce/replay/binding 不受影响。
实施分批   T1 docs 收口 / T2 INT0 测试对齐 / T4 NQ companion /（可选 gated）T3 生产收紧（见 DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md §6）。
边界       未改 NQ / Java / 测试；未新增 API / migration；未真实 HTTP / NQ / 交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥。
准入决定   timestamp alignment NOT STARTED（仅 PLAN）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-PLAN-REVIEW
```

## 38. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1 验收记录（docs 收口）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1（DOCUMENTATION + INTEGRATION_CONTRACT_ALIGNMENT）
范围       DH 侧 timestamp 契约文档统一为 RFC3339 / ISO-8601 UTC Z；不改 Java / 测试 / NQ
修改       SECURITY_POLICY §2+§4、CONTRACT_FREEZE §规则、CONTRACT_TEST_PLAN T5/INT0-T05、TIMESTAMP_FORMAT_ALIGNMENT_PLAN、README/TESTING/WORKLOG；无 Java/测试/NQ
canonical  RFC3339 / ISO-8601 UTC Z（例 2026-06-15T12:34:56Z）；拒绝 epoch 秒/毫秒/数字偏移；窗口 ±300s 不变；HMAC value-based 不改、header name 不入签
命令       mvn test
结果       BUILD SUCCESS（回归）。INT0 6+2+8=16/16、WebMvc 25、validator 6、parser 5、rate limit 3、payload gate 2；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
未收口     INT0 epoch 秒(T2) / NQ companion(T4，Integration-1 前置阻断) / 生产 UTC-Z 强制(可选 T3)；timestamp alignment 整体 NOT COMPLETED
准入决定   T1 DONE；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1-REVIEW
```

## 39. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2 验收记录（INT0 测试对齐 RFC3339 UTC Z）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2（TEST_CODE_CHANGE + INTEGRATION_CONTRACT_ALIGNMENT + REGRESSION）
范围       DH INT0 contract test/fixture timestamp 由 epoch 秒改 RFC3339 / ISO-8601 UTC Z；不改生产 Java / NQ
修改       Int0RequestFactory（Instant.ofEpochSecond(...).toString()）、Int0ContractValidator（Instant.parse(...).getEpochSecond()，非 RFC3339→TIMESTAMP_INVALID）、DhNqIntegration0SecurityContractTest（int0T05 增量断言）；+docs
命令       mvn test
结果       BUILD SUCCESS。INT0 6+2+8=16/16（SecurityContractTest 8，int0T05 内含 RFC3339-Z accept + epoch 秒/毫秒 reject）；WebMvc 25 / validator 6 / parser 5 / rate limit 3 / payload gate 2；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
不变量     HMAC（Int0Signing）value-based 不改、header name 不入签；±300s 窗口、TIMESTAMP_INVALID/TIMESTAMP_OUT_OF_WINDOW 语义、其它 INT0 测试均不变
未收口     T4 NQ companion（Integration-1 前置阻断）；T3 生产 UTC-Z-only 收紧（可选）；timestamp alignment 整体 NOT COMPLETED
准入决定   T2 DONE；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2-REVIEW
```

## 33. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3 验收记录（Tenant/Request/Trace binding 一致性）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3（CODE_CHANGE + CONTRACT_ALIGNMENT + SECURITY_FIX + TEST_CODE_CHANGE）
范围       接入 NqDhHeaderValidator，canonical Tenant/Request/Trace 与权威来源 binding 一致性；不恢复 legacy / 不双接收 / 不启动 Integration-1
修改（main）NqDhHeaderValidator（4 参 validate + binding 校验）、NqFeedbackController（接入 validator，mismatch -> 403 HEADER_BINDING_MISMATCH）
修改（test）NqDhHeaderValidatorTest 重写为 6 binding 用例；NqFeedbackControllerWebMvcTest +5 binding 用例
命令       mvn test
结果       BUILD SUCCESS
           - NqDhHeaderValidatorTest 6/6、NqFeedbackControllerWebMvcTest 25/25、NqDhHeaderParserTest 5/5、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过；checkstyle DTD 未抖动）
命令       git diff --check
结果       无 whitespace error
binding    canonical Tenant-Id != auth tenant / Request-Id != body requestId / Trace-Id != body traceId（若提供）-> 403 HEADER_BINDING_MISMATCH；header 缺省跳过；header 不覆盖权威来源
保持       HMAC value-based；rate limit key=source+tenant+route；payload 413；nonce replay 409；缺 source/timestamp/nonce/signature 由 authenticator 403/401；成功 202
安全自查   mismatch 响应不回显 header 原值 / signature / secret / full payload；validator reason 不含具体值；不记录 raw signature / material / secret / token / full body
边界       未改 NQ；未恢复 legacy；未双接收；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   header alignment 整体仍 NOT COMPLETED（仅余 docs/fixtures 收口 Batch 4）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3-REVIEW，通过后 Batch 4
```

## 40. 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T3 验收记录（production / INT0 UTC-Z-only 收紧）

```text
日期       2026-06-28
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T3（SECURITY_FIX + CONTRACT_ALIGNMENT + TEST_CODE_CHANGE + REGRESSION_VALIDATION）
范围       DH production parseTimestamp + DH INT0 validator 收紧为 RFC3339 / ISO-8601 UTC `Z`；不改 NQ / 不真实 HTTP / 不启动 Integration-1
修改（main）HmacNqFeedbackAuthenticator：parseTimestamp 在 Instant.parse 前要求 timestampHeader.endsWith("Z")；epoch 秒/毫秒与 +08:00 fail-closed，非法格式沿用 TIMESTAMP_EXPIRED（401）
修改（test）HmacNqFeedbackAuthenticatorTest：覆盖 RFC3339 UTC Z accept、epoch seconds reject、epoch milliseconds reject、+08:00 reject、过去/未来超出 ±300s reject
修改（INT0）Int0ContractValidator：同步要求 UTC Z 后再 Instant.parse；DhNqIntegration0SecurityContractTest INT0-T05 补 +08:00 reject（签名按 offset header 重算）
命令       mvn -pl dh-domain,dh-security -am "-Dtest=HmacNqFeedbackAuthenticatorTest,DhNqIntegration0SecurityContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
结果       BUILD SUCCESS；DhNqIntegration0SecurityContractTest 8/8；HmacNqFeedbackAuthenticatorTest 8/8
命令       git status --short
结果       仅本轮代码/测试/docs 变更；补 TESTING 前为 8 个文件
命令       git diff --check
结果       exit 0；仅 LF/CRLF warning，无 whitespace error
命令       git diff --stat
结果       补 TESTING 前 8 files changed, 146 insertions(+), 18 deletions(-)
命令       mvn test
结果       BUILD SUCCESS；INT0 6+2+8=16/16，Hmac 8/8，WebMvc 25/25，parser 5/5，validator 6/6，rate limit 3/3，payload gate 2/2，ArchUnit 12/12；本机无 Docker，既有 PostgresContainerSmokeTest skip 1
命令       mvn -Pquality validate
结果       BUILD SUCCESS；0 Checkstyle violations；spotless 通过
不变量     HMAC signatureMaterial value-based 不改、header name 不入签；验签仍使用 timestamp.toString() 归一化 UTC Z；±300s replay window、nonce/source/tenant/requestId/traceId/payload 语义不变
边界       未改 NQ；未新增 API / migration；未真实 HTTP / DH-NQ 调用 / 交易所调用；未新增 RealClient / 真实 Provider；未读取凭证；未启动 Integration-1；未开启 LIVE；未处理 Maven wrapper / datasource 默认弱口令 / nonce-burn race；未引入双格式兼容或 epoch fallback
准入决定   T3 已由后续 review ACCEPTED；timestamp alignment 后续由 FINALIZE 收口为 CLOSED / ACCEPTED。Integration-1 / Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。
```

## 41. 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-FINALIZE（timestamp overall CLOSED）

```text
日期       2026-06-28
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-FINALIZE（CONTRACT_FINALIZATION + DOCUMENTATION_FIX + CROSS_REPO_REGRESSION_VALIDATION）
范围       合并 DH T3 review 后旧状态修正、header naming stale docs 小修、DH/NQ 双仓最终回归；不改生产代码 / 测试代码 / API / migration
Final      timestamp alignment overall = CLOSED / ACCEPTED
canonical  RFC3339 / ISO-8601 UTC Z（例 2026-06-15T12:34:56Z）；DH/NQ 均拒绝 epoch seconds / epoch milliseconds / 数字时区偏移；窗口 ±300s 不变
边界       CLOSED 仅表示 timestamp 契约收口完成；Integration-1 / Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED
验证       DH git status --short：仅允许的 7 个 docs/current 文件 modified
           DH git diff --check：通过；仅 LF/CRLF warning，无 whitespace error
           DH git diff --stat：7 files changed, 141 insertions(+), 134 deletions(-)
           DH mvn test：BUILD SUCCESS；既有 PostgresContainerSmokeTest 因本机无 Docker skipped 1
           DH mvn -Pquality validate：BUILD SUCCESS；0 Checkstyle violations；Spotless check 通过
           NQ git status --short：仅允许的 8 个 docs/current 文件 modified
           NQ git diff --check：通过；仅 LF/CRLF warning，无 whitespace error
           NQ git diff --stat：8 files changed, 55 insertions(+), 12 deletions(-)
           NQ mvn -f backend/pom.xml test：BUILD SUCCESS
           NQ INT0 scoped test：BUILD SUCCESS；Integration0 6+2+9=17/17
           NQ backend quality profile 探测：backend POM 未检出 quality profile / Spotless / Checkstyle
```

## 42. 2026-07-01 DH-DOCS-SKILL-SYNC-FROM-NQ 验证记录（docs / skill only）

```text
日期       2026-07-01
阶段       DH-DOCS-SKILL-SYNC-FROM-NQ（DOCS_GOVERNANCE + SKILL_SYNC + PROJECT_RULES_CREATION + WORKFLOW_STANDARDIZATION）
范围       创建 DH 文档治理 skill，并同步 AGENTS / README / docs/current workflow / STATUS / ROADMAP 入口；不改 Java 生产代码、测试代码、API、migration、runtime 配置、contracts 或 golden_cases

命令       git status --short
结果       仅允许的文档与 skill 文件变更；新增 .agents/skills/dh-docs-writer/

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       git diff --stat
结果       输出显示 AGENTS、README 与 docs/current tracked 文档存在 diff；新增 skill 文件为 untracked，另见 git status

命令       git diff -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
结果       空 diff；确认未改 DH 生产代码、测试代码、contracts 或 golden_cases

命令       rg 敏感词扫描（token / cookie / API secret / passphrase / private key / exchange key / database password / real credential material）
结果       仅命中文档禁止清单和安全规则文字；未发现真实凭证值

命令       skill-creator quick_validate.py .agents/skills/dh-docs-writer
结果       未通过执行环境：bundled Python 缺少 yaml 模块，报 ModuleNotFoundError: No module named 'yaml'
补充       手工等价检查通过：SKILL.md frontmatter 含 name/description；无模板 TODO；agents/openai.yaml 存在且 default_prompt 使用 $dh-docs-writer

命令       mvn test
结果       BLOCKED / NOT RUN TO TEST EXECUTION：
           1) 默认本地仓库 D:\Tool\Maven\maven-repository 在 spring-boot-starter-parent/3.5.10 写 tracking file 时 FileAlreadyExistsException；
           2) 改用临时本地仓库并修正 Windows 参数解析后，非沙箱重跑进入 reactor，但 Aliyun Maven 依赖下载反复握手中断；
           3) 最终阻塞在 dh-common 的 maven-resources-plugin 依赖 javax.inject:1 与 org.slf4j:slf4j-api:1.7.36 下载，Remote host terminated the handshake；
           项目测试未启动，未产生测试失败结论。

命令       mvn -Pquality validate
结果       BLOCKED / NOT RUN TO QUALITY EXECUTION：非沙箱运行在 dh-bom 下载 maven-checkstyle-plugin:3.3.1 POM 时 Aliyun Maven handshake 中断；quality 未进入 Checkstyle / Spotless。

边界       未修改 NQ 仓库；未改 DH 生产代码；未改 DH 测试代码；未新增 API；未新增 migration；未真实 HTTP；未接 NQ runtime；未接真实 provider；未接 AI / LangGraph；未读取密钥；未开启 LIVE。
准入决定   docs / skill 变更已完成 Git 级验证；Maven 回归被外部依赖下载阻塞，后续需在依赖仓库可用或本地 Maven 仓库修复后重跑 mvn test / mvn -Pquality validate。
```

## 43. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-PLAN 验证记录（docs-only / plan-only）

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-PLAN（ARCHITECTURE_PLAN + CONTRACT_PLAN + DECISION_PIPELINE_PLAN + AUDIT_REPLAY_PLAN + SECURITY_BOUNDARY + NO_LIVE_TRADE）
范围       新增 Decision Pipeline MVP 计划并同步 docs/current 当前事实源；不改 Java 生产代码、测试代码、API、migration、contracts、golden_cases、runtime 配置或 NQ 仓库

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
结果       仅 docs/current 计划与状态同步文件变更；新增 docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md；未出现代码、测试、contracts、golden_cases 或 migration 变更

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       git diff --stat
结果       tracked docs/current 文件存在 diff；新增计划文件由 git status 标识为 untracked，未自动 stage

命令       git diff -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
结果       空 diff；确认未改 DH 生产代码、测试代码、contracts 或 golden_cases

命令       rg 敏感词扫描（token / cookie / API secret / passphrase / private key / exchange key / database password / real credential material）
结果       仅命中文档禁止清单和安全边界文字；未发现真实凭证值

命令       mvn test
初始结果   默认 Maven 配置未进入测试执行：
           1) 默认本地仓库 D:\Tool\Maven\maven-repository 写 spring-boot-starter-parent/3.5.10 tracking file 时 FileAlreadyExistsException；
           2) 仅设置 -Dmaven.repo.local 仍受全局 settings 影响；
           3) 仅设置 -s target/codex-maven-settings.xml 仍合并全局 Aliyun mirror，依赖下载 TLS handshake 中断。
最终命令   mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
最终结果   BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 06:33；Finished at 2026-07-01T12:08:03+08:00
补充       dh-app 中既有 PostgresContainerSmokeTest 因本机无可用 Docker 环境 skipped 1；其余测试无 failure / error

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 02:14；Finished at 2026-07-01T12:10:43+08:00

边界       未修改 NQ 仓库；未改 DH Java production / test code；未新增 API / migration；未改 contracts / golden_cases；未真实 HTTP；未接 NQ runtime；未接真实 provider；未接 AI / LangGraph runtime；未读取密钥；未开启 LIVE。
准入决定   DH-STAGE4-DECISION-PIPELINE-MVP-PLAN 已完成 docs-only 计划产物并通过 Git / Maven / quality 验证；下一步仅允许进入 DH-STAGE4-DECISION-PIPELINE-MVP-WO，不允许直接实现 runtime。
```

## 44. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-WO 验证记录（docs-only / work-order-only）

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-WO（WORK_ORDER + IMPLEMENTATION_BATCH_DESIGN + CONTRACT_FREEZE_PREP + AUDIT_REPLAY_PLANNING + SECURITY_BOUNDARY + NO_LIVE_TRADE）
范围       新增 DH Stage4 Decision Pipeline MVP K1-K8 可执行工单，并同步 docs/current 当前事实源；不改 Java 生产代码、测试代码、API、migration、contracts、golden_cases、runtime 配置或 NQ 仓库

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
结果       仅 docs/current 文档变更；新增 docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md；未出现代码、测试、contracts、golden_cases 或 migration 变更

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       git diff --stat
结果       执行成功；tracked docs/current 文件存在 diff；新增 WO 文件由 git status 标识为 untracked，未自动 stage

命令       git diff --name-only
结果       仅 docs/current/README.md、docs/current/ROADMAP.md、docs/current/STATUS.md、docs/current/WORK_ORDER.md 出现在 tracked diff；新增 WO 文件由 git status 标识为 untracked

命令       rg current / next 状态残留扫描
结果       未发现把当前阶段写回 PLAN / READY FOR REVIEW 或 WO / NOT STARTED 的残留；历史记录段落未作为当前事实源使用

命令       rg readiness forbidden YES 扫描
结果       未发现 ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: YES、ALLOW_INTEGRATION_1_RUNTIME: YES、ALLOW_AGENT_PHASE: YES、ALLOW_LANGGRAPH_RUNTIME: YES 或 ALLOW_LIVE: YES

命令       mvn test
初始结果   未进入测试执行；本机全局 Maven settings / repository 阻断：
           1) D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml line 227 存在 Unrecognised tag: profiles warning；
           2) D:\Tool\Maven\maven-repository\org\springframework\boot\spring-boot-starter-parent\3.5.10 写 tracking file 时 FileAlreadyExistsException。
最终命令   mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
最终结果   BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 43.903 s；Finished at 2026-07-01T13:21:04+08:00
补充       dh-app 中既有 PostgresContainerSmokeTest 因本机无可用 Docker 环境 skipped 1；其余测试无 failure / error

命令       mvn -Pquality validate
初始结果   未进入 quality 执行；本机全局 Maven settings / repository 阻断，错误与 mvn test 初始结果一致
最终命令   mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
最终结果   BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过；Total time 7.450 s；Finished at 2026-07-01T13:21:33+08:00

Work order result:
           新增 K1-K8 批次工单：K1 Decision Contract Freeze；K2 DecisionOrchestrator Skeleton；
           K3 Audit / Snapshot / Trace Persistence；K4 Replay Read Model；
           K5 Mock Provider / Provider Health / Budget / Latency；
           K6 Mock NQ Dry-run Contract Tests；K7 Golden Cases / Eval Baseline；
           K8 Acceptance / Freeze。

Readiness decision:
           ALLOW_WO_CLOSE: YES
           ALLOW_K1_IMPLEMENTATION: YES
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未修改 NQ 仓库；未改 DH Java production / test code；未新增 API / migration；未改 contracts / golden_cases；未真实 HTTP；未接 NQ runtime；未接真实 provider；未接 AI / LangGraph runtime；未读取密钥；未开启 LIVE。
准入决定   DH-STAGE4-DECISION-PIPELINE-MVP-WO 已完成 docs-only 工单产物并通过 Git / Maven / quality 复验；下一步仅允许进入 K1 Decision Contract Freeze，且必须单批 review，不允许全量 GateK implementation。
```

## 45. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE 验证记录（contract / domain / schema / tests）

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE（CONTRACT_FREEZE + DOMAIN_MODEL + JSON_SCHEMA + CONTRACT_TESTS + SECURITY_BOUNDARY + NO_LIVE_TRADE）
范围       只执行 K1：冻结 Decision Pipeline MVP 的 domain contract、enum、JSON Schema 与 contract tests；不实现 K2-K8，不新增 API / migration / runtime / provider / client

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
结果       仅 K1 允许范围内变更：
           docs/current/README.md
           docs/current/ROADMAP.md
           docs/current/STATUS.md
           docs/current/WORK_ORDER.md
           contracts/json-schema/dh-decision-request.schema.json
           contracts/json-schema/dh-decision-output.schema.json
           dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/**
           dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/**
           dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/Decision*ContractTest.java

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       git diff --stat
结果       执行成功；tracked docs/current 文件存在 diff；新增 K1 schema / Java / tests 由 git status 标识为 untracked，未自动 stage

命令       mvn test
结果       未进入测试执行；本机全局 Maven settings / repository 阻断：
           1) D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml line 227 存在 Unrecognised tag: profiles warning；
           2) D:\Tool\Maven\maven-repository\org\springframework\boot\spring-boot-starter-parent\3.5.10 写 tracking file 时 FileAlreadyExistsException。

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-domain -am test
结果       BUILD SUCCESS；dh-bom / dh-common / dh-domain reactor 3/3 SUCCESS；dh-domain 108 tests；0 failures；0 errors；0 skipped；新增 K1 tests 22 cases

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 50.595 s；Finished at 2026-07-01T13:43:47+08:00
补充       dh-app 中既有 PostgresContainerSmokeTest 因本机无可用 Docker 环境 skipped 1；其余测试无 failure / error

命令       mvn -Pquality validate
结果       未进入 quality 执行；本机全局 Maven settings / repository 阻断，错误与裸 mvn test 一致

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过；Total time 9.294 s；Finished at 2026-07-01T13:48:23+08:00

命令       idea-mcp get_file_problems(errorsOnly=true)
范围       DecisionOutput.java / DecisionRequest.java / DecisionOutputSchemaContractTest.java / DecisionRequestSchemaContractTest.java
结果       4 个文件均无 error 级问题

新增测试   DecisionRequestContractTest 1
           DecisionOutputContractTest 4
           DecisionRequestSchemaContractTest 5
           DecisionOutputSchemaContractTest 7
           DecisionEnumContractTest 3
           DecisionNoTradingInstructionContractTest 2
           合计 22 个 K1 cases

合同覆盖   schema 文件存在性；required 字段完整；additionalProperties=false；
           enum 与 Java enum 一致；action enum 不含 BUY / SELL / PLACE_ORDER / CANCEL_ORDER /
           MARKET_ORDER / LIMIT_ORDER；decisionType 仅 READ_ONLY_RECOMMENDATION；
           forbiddenActions 固定五项；request schema 不含 credential / execution intent 字段；
           output schema 不含 free-text final output / execution command 字段；
           no evidence / provider failure / policy denied / high risk fail-closed domain 行为。

Readiness decision:
           ALLOW_K1_CLOSE: YES
           ALLOW_K2_IMPLEMENTATION: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未实现 DecisionOrchestrator；未实现 DecisionContextBuilder；未实现 MockDecisionProvider；
           未实现 RiskReview 流水线；未实现 policy evaluator 生产逻辑；未新增 API path；
           未新增 Controller；未新增 migration；未新增 Repository / Service / Client 实现；
           未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；
           未新增 RealClient；未新增真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；
           未接 LangGraph；未接 MCP 写能力；未实现 replay API；
           未实现 audit / snapshot / trace 表；未读取或输出 credential / token / cookie /
           API secret / passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；
           未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；
           未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 action enum。
准入决定   K1 Contract Freeze 已完成 implementation 并通过 Git / Maven / quality 验证；下一步只能进入 K1 review，不得直接进入 K2。
```

## 46. 2026-07-01 DH-DOCS-LANGUAGE-GOVERNANCE-FIX 验证记录（docs governance / language policy）

```text
日期       2026-07-01
阶段       DH-DOCS-LANGUAGE-GOVERNANCE-FIX（DOCS_GOVERNANCE + LANGUAGE_POLICY + COMMENT_STYLE_RULES + FACTSOURCE_SYNC）
范围       只修复 DH 文档语言治理规则和明显英文漂移；不改 Java 生产代码、测试代码、contracts、golden_cases、API path、migration、runtime、provider、NQ runtime 或 LIVE

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
结果       本轮 tracked 变更为允许的 docs / skill / project rule 文件：
           .agents/skills/dh-docs-writer/SKILL.md
           AGENTS.md
           README.md
           docs/current/API.md
           docs/current/CODEX_PROJECT_INSTRUCTIONS.md
           docs/current/CODEX_WORKFLOW_INDEX.md
           docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md
           docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
           docs/current/README.md
           docs/current/ROADMAP.md
           docs/current/STATUS.md
           docs/current/TESTING.md
           docs/current/WORKLOG.md
           docs/current/WORK_ORDER.md
           仍存在预检前已在工作区的 K1 untracked contracts / dh-domain / test files；本轮未修改这些文件。

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       git diff --stat
结果       执行成功；显示 14 个 tracked docs / skill / project rule 文件存在 diff

命令       rg -n "Objective|Allowed files|Forbidden files|Exit criteria|Rollback approach|Main classes|Required tests|Validation commands|Boundary confirmation" docs/current .agents/skills/dh-docs-writer AGENTS.md README.md
结果       仅命中 .agents/skills/dh-docs-writer/SKILL.md 中固定输出字段模板 `Boundary confirmation:`；判定允许保留

命令       git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
结果       空；tracked diff 未触碰生产代码、测试代码、contracts 或 golden_cases

命令       git ls-files --others --exclude-standard dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
结果       列出预检前已存在的 K1 untracked contracts / dh-domain / test files；这些文件属于上一轮 K1 工作区状态，不是本轮语言治理写入

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 41.525 s；Finished at 2026-07-01T14:13:48+08:00
补充       dh-app 中既有 PostgresContainerSmokeTest 因本机无有效 Docker 环境 skipped 1；其余测试无 failure / error

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过；Total time 7.526 s；Finished at 2026-07-01T14:14:06+08:00

语言治理   dh-docs-writer 已新增语言规则；AGENTS / README / CODEX_PROJECT_INSTRUCTIONS / CODEX_WORKFLOW_INDEX / docs/current/README 已同步。
           DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md 与 DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md 已改为中文主体；
           保留 DecisionOutput / DecisionOrchestrator / enum / JSON Schema 字段 / HTTP header / 状态词 / 命令等稳定工程标识。

边界       未修改 Java 生产代码；未修改测试代码；未修改 contracts；未修改 golden_cases；未新增 API；未新增 migration；
           未启动 K1 新实现；未启动 K2；未接 NQ；未真实 HTTP；未接真实 provider；未接 AI / LangGraph；
           未开启 LIVE；未把 Integration-1 / Runtime integration / Agent phase 写成 started。
准入决定   本轮语言治理修复已完成并通过 Git / rg / Maven / quality 验证。当前主线仍为 K1 review，不得直接进入 K2。
```

## 47. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON 验证记录（mock-only usecase skeleton）

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON（ORCHESTRATOR_SKELETON + DECISION_PIPELINE_MVP + MOCK_ONLY + SECURITY_BOUNDARY + NO_LIVE_TRADE）
范围       只执行 K2：dh-usecase 内 mock-only DecisionOrchestrator skeleton、context builder、policy checker、mock signal provider、risk reviewer、output assembler 与 K2 unit tests；不实现 K3-K8、不新增 API / Controller / Repository / JDBC / migration、不接真实 provider / NQ / HTTP / LangGraph / LIVE

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
初始结果   工作区 clean；本轮从上一提交后开始
最终范围   仅 K2 允许范围内变更：
           dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionOutput.java
           dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
           dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/**
           docs/current/README.md
           docs/current/STATUS.md
           docs/current/ROADMAP.md
           docs/current/WORK_ORDER.md
           docs/current/TESTING.md
           docs/current/WORKLOG.md

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error

命令       rg K2 禁止范围扫描
范围       dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision
           dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision
           dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision
结果       仅命中：
           - policy denylist 中的 `passphrase`
           - 负向测试字符串 `placeOrder-plan` / `cancelOrder-attempt`
           - 注释中明确禁止 LangGraph / HTTP / NQ / provider runtime 的说明
           未命中新增 Controller / Repository / JDBC / migration / HTTP client / RealClient / OpenAI / Claude / Gemini / BUY / SELL / accountId 等生产实现

命令       mvn -pl dh-usecase -am test
结果       未进入编译；本机全局 Maven settings / repository 阻断：
           1) D:\Tool\Maven\apache-maven-3.9.12\conf\settings.xml line 227 存在 Unrecognised tag: profiles warning；
           2) D:\Tool\Maven\maven-repository\org\springframework\boot\spring-boot-starter-parent\3.5.10 写 tracking file 时 FileAlreadyExistsException。

命令       mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -U -pl dh-usecase -am test
结果       首次绕开全局 settings 后进入依赖解析，但 Maven Central TLS handshake 中断；未进入 K2 编译。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-usecase -am test
结果       BUILD SUCCESS；reactor 9/9 SUCCESS；Total time 10.539 s；Finished at 2026-07-01T15:28:42+08:00
测试摘要   dh-domain 108 tests / 0 failures / 0 errors / 0 skipped
           dh-connector 19 tests / 0 failures / 0 errors / 0 skipped
           dh-usecase 92 tests / 0 failures / 0 errors / 0 skipped
           K2 新增 tests 22 cases：
           DecisionOrchestratorTest 7
           DecisionOutputAssemblerTest 4
           DefaultDecisionPolicyCheckerTest 4
           DefaultDecisionRiskReviewerTest 4
           MockDecisionSignalProviderTest 3

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality -pl dh-usecase -am validate
结果       BUILD SUCCESS；reactor 9/9 SUCCESS；Total time 4.784 s；Finished at 2026-07-01T15:28:37+08:00
补充       Maven 输出 `Unable to perform checkstyle:check, unable to find checkstyle:checkstyle outputFile.`，但 reactor status 为 SUCCESS；未报告 checkstyle violation。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality -pl dh-usecase -am spotless:check
结果       BUILD FAILURE；失败点在 dh-common 既有格式问题（ApiResponse.java、BizException.java、CommonErrorCodes.java、ErrorCode.java），未进入 K2 模块；未做跨范围格式化。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality -pl dh-domain,dh-usecase spotless:check
结果       BUILD FAILURE；失败点在 dh-domain 既有格式漂移（100 个既有文件），不是本轮 K2 变更专属问题；未运行 spotless:apply，避免扩大修改范围。

K2 覆盖     正常 mock 输出 -> READ_ONLY_RECOMMENDATION / OBSERVATION_ONLY / NO_TRADE / Provider MOCKED
           no evidence -> ABSTAIN / UNKNOWN / NOT_CALLED
           forbidden execution intent -> BLOCKED / DENIED / NOT_CALLED
           provider timeout -> ABSTAIN / UNKNOWN / TIMEOUT
           high risk directional signal -> ABSTAIN / HIGH
           internal failure -> structured ABSTAIN / UNEXPECTED_FAILURE
           null request -> BLOCKED / INVALID / unknown-request

Readiness decision:
           ALLOW_K2_CLOSE: YES
           ALLOW_K3_IMPLEMENTATION: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未新增 API path；未新增 Controller；未新增 Repository / JDBC / migration；未新增 audit / snapshot / trace / replay persistence；
           未接真实 HTTP；未接 NQ runtime；未新增 RealClient；未新增真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；
           未接 LangGraph；未接 MCP 写能力；未实现 replay API；未读取或输出 credential / token / cookie / API secret / passphrase；
           未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；
           未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 action enum 或 output action。
准入决定   K2 Orchestrator Skeleton 已完成 implementation 并通过模块 Maven 测试与 quality validate；下一步只能进入 K2 review，不得直接进入 K3。
```

## 48. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE 验证记录（audit / snapshot / trace persistence）

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE（CODE_CHANGE + PERSISTENCE + AUDIT_TRACE + DECISION_SNAPSHOT + FAIL_CLOSED + DOCS_SYNC）
范围       只执行 K3：新增 DH-owned audit / snapshot / trace persistence、usecase port、JDBC adapter、app wiring、K3 tests 和 docs/current sync；不实现 K4-K8、不新增 API / Controller / replay API、不接真实 provider / NQ / HTTP / LangGraph / LIVE

命令       Get-Location
结果       F:\project\decision-hub

命令       git branch --show-current
结果       dev

命令       git status --short
当前范围   K3 允许范围内变更：
           dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
           dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestratorPersistenceTest.java
           dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
           dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepositoryTest.java
           dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
           dh-app/src/main/resources/db/migration/V5__dh_decision_pipeline_audit.sql
           dh-app/src/test/java/com/guidinglight/decisionhub/V5DecisionPipelineAuditMigrationPresenceTest.java
           docs/current/README.md
           docs/current/STATUS.md
           docs/current/ROADMAP.md
           docs/current/WORK_ORDER.md
           docs/current/TESTING.md
           docs/current/WORKLOG.md

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error。

命令       git diff --stat
结果       tracked diff 8 files changed, 923 insertions(+), 96 deletions(-)；新增 K3 Java / SQL / test 文件由 `git status --short` 标识为 untracked，未自动 stage。

命令       K3 边界关键词扫描
范围       K3 changed production/test/docs-adjacent files under dh-usecase decision、dh-infra jdbc decision、dh-app config、V5 migration、V5 migration test
关键词     RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate / HttpClient / placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase / accountId / Controller / NqClient / Exchange / Broker / live / LIVE
结果       命中项均为禁止说明、migration comment、denylist 或负向测试字符串；未发现真实 runtime provider、HTTP client、Controller、NQ client、Exchange/Broker、BUY/SELL action 实现或 LIVE 启用。

命令       docs/current 当前状态残留扫描
结果       未发现 current stage / next stage 仍指向 K2 review；未发现 K3 current 写成 NOT STARTED；未发现 ALLOW_K4_IMPLEMENTATION / ALLOW_STAGE4_M1_CLOSE_REVIEW / ALLOW_INTEGRATION_1_RUNTIME / ALLOW_AGENT_PHASE / ALLOW_LANGGRAPH_RUNTIME / ALLOW_LIVE 被写成 YES。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-usecase,dh-infra -am test
初始结果   FAILURE；`JdbcDecisionAuditRepositoryTest.saveOutput_targetsOutputTableWithJsonbCast` 暴露 `DecisionOutput.createdAt` 直接序列化依赖 JavaTime module。
RCA        output_json 不应直接序列化 domain object；K3 改为 usecase 层显式安全 Map，createdAt 使用 ISO 字符串，不暴露内部模型。
最终结果   BUILD SUCCESS；reactor 11/11 SUCCESS；dh-usecase 102 tests / 0 failures / 0 errors / 0 skipped；dh-infra 26 tests / 0 failures / 0 errors / 3 skipped。
补充       3 skipped 为既有 JdbcNonceReplayGuardPersistenceTest Docker/Testcontainers 环境项。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Surefire 汇总 375 tests / 0 failures / 0 errors / 4 skipped；Total time 41.456 s；Finished at 2026-07-01T17:41:51+08:00。
补充       skipped 4 = dh-infra JdbcNonceReplayGuardPersistenceTest 3 + dh-app PostgresContainerSmokeTest 1；均为本机无 Docker 的既有 Testcontainers 环境项。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；Total time 6.740 s；Finished at 2026-07-01T17:42:06+08:00。

K3 新增测试
           DecisionOrchestratorPersistenceTest 10
           JdbcDecisionAuditRepositoryTest 9
           V5DecisionPipelineAuditMigrationPresenceTest 5

K3 覆盖     valid write-through 写 request / snapshot / trace / provider call / output / audit。
           policy denied 不调用 provider，但仍写 output / audit。
           provider timeout 写 provider call 并返回 ABSTAIN。
           high risk directional signal 不允许 LONG_BIAS / SHORT_BIAS，返回 ABSTAIN。
           request / context snapshot / output / audit persistence failure 均 fail-closed。
           missing request 使用 unknown IDs 并按 policy fail-closed。
           request persistence 会脱敏 sensitive token 与 placeOrder / cancelOrder 等 execution-intent token。
           JDBC SQL 命中六张 K3 表、使用 CAST(? AS jsonb)、DataAccessException / JSON serialization failure 转 DecisionPersistenceException。
           V5 migration 包含六张表、jsonb、timestamptz、索引、中文 comment、安全约束，且不创建 trading / live / NQ-owned 表。

Readiness decision:
           ALLOW_K3_CLOSE: YES
           ALLOW_K4_IMPLEMENTATION: NO
           ALLOW_STAGE4_M1_CLOSE_REVIEW: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未新增 API path；未新增 Controller；未新增 replay API / query endpoint；未实现 K4 Replay Read Model；
           未接真实 HTTP；未接 NQ runtime；未新增 RealClient；未新增真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；
           未接 LangGraph；未实现 mock NQ dry-run contract tests；未读取或输出 credential / token / cookie / API secret / passphrase；
           未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；
           未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。
准入决定   K3 Audit / Snapshot / Trace Persistence 已完成 implementation 并通过模块 Maven、全仓 Maven 与 quality validate；当前状态为 IMPLEMENTED / READY FOR M1。下一步只能进入 M1 readiness review，不得直接进入 K4。
```

## 49. 2026-07-01 DH-GATEK-K3-CI-OBJECTMAPPER-FIX 验证记录（CI failure fix）

```text
日期       2026-07-01
阶段       DH-GATEK-K3-CI-OBJECTMAPPER-FIX（CI_FIX + REGRESSION）
范围       只修复 K3 后 CI 中 dh-app Spring context 启动失败；不推进 M1、不实现 K4、不新增 API / Controller / replay API、不接真实 provider / NQ / HTTP / LangGraph / LIVE

CI 失败    GitHub Actions run 28508807175 / job 84503767630
失败步骤   Build and test
失败测试   PostgresContainerSmokeTest.contextLoads
RCA        K3 新增 `decisionPersistenceObjectMapper` Spring bean 后，容器中同时存在 `nqFeedbackObjectMapper`
           与 `decisionPersistenceObjectMapper` 两个 `ObjectMapper`；Spring WebMVC 创建
           `mappingJackson2HttpMessageConverter` 时需要单个 `ObjectMapper`，因此抛出
           `NoUniqueBeanDefinitionException`。

修复       `DecisionPipelineWiringConfig` 不再把 K3 persistence mapper 注册为全局 Spring bean；
           `DecisionAuditRepository` 内部创建 K3 专用 mapper，避免与 HTTP message converter 竞争。
回归       新增 `DecisionPipelineWiringConfigTest`，用 ApplicationContextRunner 断言：
           - 全局 `ObjectMapper` 仍只有一个；
           - `nqFeedbackObjectMapper` 仍存在；
           - `decisionPersistenceObjectMapper` 不再作为 Spring bean 暴露；
           - `DecisionAuditRepository` 仍能装配。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-app -am "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DecisionPipelineWiringConfigTest" test
结果       BUILD SUCCESS；DecisionPipelineWiringConfigTest 1/1 passed；reactor 15/15 SUCCESS。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；dh-app 32 tests / 0 failures / 0 errors / 1 skipped；
           全仓本地可运行测试通过；Finished at 2026-07-01T18:07:01+08:00。
补充       本机无有效 Docker，`PostgresContainerSmokeTest` 按 `disabledWithoutDocker=true` skipped；
           CI 有 Docker，会实际覆盖该 contextLoads 路径。

命令       mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；
           最终一次执行通过。

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error。

边界       未修改 NQ 仓库；未新增 API path；未新增 Controller；未新增 migration；未新增 replay API；
           未接真实 HTTP；未接 NQ runtime；未新增 RealClient；未新增真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；
           未接 LangGraph；未读取或输出 credential / token / cookie / API secret / passphrase；
           未启动 Integration-1 runtime；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；
           未开启 LIVE；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。
准入决定   CI failure root cause 已修复并有非 Docker 回归测试保护；当前主线仍为 K3 IMPLEMENTED / READY FOR M1，
           下一步仍是 M1 readiness review，不得直接进入 K4。
```

## 50. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL 验证记录

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL（CODE_CHANGE + REPLAY_READ_MODEL + DOCS_SYNC）
范围       只实现内部 replay read model；不新增 API / Controller / migration / replay endpoint；不进入 K5-K8。

K4 新增测试
  - DecisionReplayQueryServiceTest 8/8：
    found / not found / tenant mismatch / incomplete / corrupted / repository failure -> BLOCKED /
    invalid input -> BLOCKED / trace mismatch -> BLOCKED。
  - JdbcDecisionReplayQueryRepositoryTest 6/6：
    found + tenant scoped SQL + no write / not found / incomplete / corrupt JSON -> CORRUPTED /
    DB read failure -> BLOCKED / trace-provider-audit ordering。
  - DecisionPipelineWiringConfigTest 1/1：
    ObjectMapper bean 仍唯一；`decisionPersistenceObjectMapper` 不作为 Spring bean 暴露；
    replay repository / service 可装配。

命令       mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test
结果       BUILD SUCCESS；reactor 15/15 SUCCESS；dh-app `PostgresContainerSmokeTest` 1 skipped。
说明       该次为普通 Codex sandbox 执行，Testcontainers 无法访问 Docker named pipe，按既有
           `disabledWithoutDocker` 语义跳过。K4 usecase / JDBC read / wiring 测试均已通过。

命令       docker info --format '{{.ServerVersion}}'（提权）
结果       29.5.3；本机 Docker Desktop 可访问。

命令       mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test（提权，允许访问 Docker）
结果       BUILD FAILURE；失败点为既有 `JdbcNonceReplayGuardPersistenceTest` 拉取 `postgres:17`
           Docker image 失败，错误为 Docker registry / mirror 下载 EOF；K4 replay tests 在失败前已通过。
补充       `testcontainers/ryuk:0.11.0` 已成功拉取并启动；`postgres:17` 未能拉取成功。

命令       docker pull postgres:17（提权）
结果       FAILURE；Docker daemon 经 `hub-mirror.c.163.com` 拉取 `postgres:17` 时 EOF。

命令       docker pull public.ecr.aws/docker/library/postgres:17（提权）
结果       FAILURE；备用 public ECR 下载 layer 时 EOF。

命令       mvn -ntp test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；dh-app `PostgresContainerSmokeTest` 1 skipped。
说明       普通 sandbox 下 Docker pipe 权限不足，Docker-gated smoke 按既有规则跳过；非 Docker 全仓回归通过。

命令       mvn -ntp -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。

命令       git diff --check
结果       通过；仅 Windows LF -> CRLF warning，无 whitespace error。

命令       K4 边界关键词扫描
范围       dh-domain/src/main、dh-usecase/src/main、dh-infra/src/main、dh-app/src/main、dh-api/src/main、
           dh-connector/src/main、dh-security/src/main、contracts（排除 target）。
结果       命中项均为既有 Controller、已存在配置、禁止说明、denylist、migration comment、负向安全词或本轮 K4
           边界注释；未发现本轮新增 API / Controller / replay endpoint / RealClient / real provider / HTTP client /
           NQ runtime / LangGraph / LIVE / BUY-SELL action 实现。

边界       未新增 API path；未新增 Controller；未新增 migration；未新增 replay API / query endpoint；
           未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；
           未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；
           未实现 K5-K8；未读取或输出 credential、token、cookie、API secret、passphrase；
           未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；
           未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库；
           未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

环境项     Docker Desktop 存在且提权可访问；但当前 Docker registry / mirror 无法拉取 `postgres:17`，
           所以 Docker-gated Testcontainers 真实 Postgres 用例在本轮被镜像拉取阻断。该失败不来自 K4 代码。

Readiness decision
           ALLOW_K4_CLOSE: YES
           ALLOW_K5_IMPLEMENTATION: YES
           ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

下一步     DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY / NOT STARTED。
```

## 51. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY 验证记录

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY
类型       CODE_CHANGE + PROVIDER_GUARD + HEALTH_MODEL + BUDGET_GUARD + LATENCY_TRACKING + SECURITY_BOUNDARY + DOCS_SYNC
范围       只实现 K5 mock-only provider health / budget / latency；不新增 API / Controller / migration；
           不新增 provider health / budget / latency 表；不接真实 provider / HTTP / NQ / LLM / LangGraph / LIVE。

K5 新增/更新测试
  - DecisionProviderHealthEvaluatorTest：health / failure class / SUCCESS forbidden 映射。
  - DecisionProviderBudgetGuardTest：within budget / disabled / budget exceeded。
  - DecisionProviderLatencyRecorderTest：latencyMs / timeout / 负阈值保护。
  - DecisionProviderGuardTest：pre-guard budget gate 与 post-guard health/latency fail-closed。
  - DecisionOrchestratorProviderGuardTest：healthy / disabled / unhealthy / timeout / failure /
    untrusted / budget exceeded / provider exception 的 ABSTAIN fail-closed 与 provider call summary。
  - DecisionProviderGuardNoOutboundTest：K5 guard source 不出现 HTTP / LLM / NQ / exchange / mapping 注解关键依赖。
  - MockDecisionSignalProviderTest：mock-only provider 继续拒绝 SUCCESS。
  - DecisionPipelineWiringConfigTest：K5 guard beans 可装配，ObjectMapper bean 仍唯一。
  - JdbcDecisionReplayQueryRepositoryTest：K4 replay 能读取 K5 provider call summary。

命令       mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test
早期结果   首次 120s 执行超时，未得到完整测试结论；随后延长到 300s 已通过。
最终结果   BUILD SUCCESS；reactor 15/15 SUCCESS；Total time 28.561 s；
           Finished at 2026-07-01T22:15:07+08:00。
补充       本轮 Docker 可访问，dh-app `PostgresContainerSmokeTest` 实际启动 `postgres:17` 并通过。

命令       mvn -ntp test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 29.051 s；
           Finished at 2026-07-01T21:53:11+08:00。
测试摘要   Surefire XML 汇总 414 tests / 0 failures / 0 errors / 0 skipped。
           dh-domain 108；dh-connector 19；dh-usecase 134；dh-security 47；
           dh-infra 32；dh-api 42；dh-app 32。
补充       本轮 Docker 可访问，`PostgresContainerSmokeTest` 未 skip，Flyway V1-V5 均在 PostgreSQL 17.10 上实际迁移通过。

命令       mvn -ntp -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；
           Finished at 2026-07-01T21:53:47+08:00。
补充       各子模块 checkstyle 插件仍输出既有 `unable to find checkstyle:checkstyle outputFile` 信息；
           聚合 checkstyle 最终为 0 violations，reactor status 为 SUCCESS。

命令       idea-mcp get_file_problems（DefaultDecisionOrchestrator / DefaultDecisionProviderGuard /
           DecisionProviderGuardResult / DecisionPipelineWiringConfig）
结果       4 个关键文件 problems 检查均约 300s timeout。
降级       降级为 Maven 编译、模块测试、全仓测试、quality validate 与 scoped rg 扫描；可信度高，
           因为 Java 编译、JUnit、Spring context、Checkstyle 与 Spotless 均已通过。

命令       K5 边界关键词扫描
范围       dh-domain/src/main、dh-usecase/src/main、dh-infra/src/main、dh-app/src/main、dh-api/src/main、
           dh-connector/src/main、dh-security/src/main、相关 test、contracts、docs/current；
           排除 target / build / dist / node_modules / logs / test-results / secrets / credentials。
关键词     RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate / HttpClient /
           placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase / accountId / Controller /
           NqClient / Exchange / Broker / live / LIVE / endpoint / Endpoint / PostMapping /
           GetMapping / RequestMapping。
结果       命中项均为既有 Controller、禁止说明、denylist、负向测试断言、migration comment、
           K5 no-outbound 测试或本轮边界注释；未发现本轮新增 API / Controller / migration /
           RealClient / real provider / HTTP client / NQ runtime / LangGraph runtime / LIVE /
           BUY-SELL action 生产实现。

命令       changed-files 边界关键词扫描
结果       本轮变更命中仅来自：
           - K5 生产注释：明确禁止 LangGraph / LIVE / Controller / endpoint / real provider。
           - `DefaultDecisionOrchestrator` denylist：`passphrase` 等敏感/执行意图词仅用于脱敏。
           - `DecisionProviderGuardNoOutboundTest`：测试断言禁止 HTTP / LLM / NQ / exchange / mapping 注解。
           - docs/current：当前边界、禁止项和 next action 文档说明。

命令       rg -n "BUY|SELL|PLACE_ORDER|CANCEL_ORDER|MARKET_ORDER|LIMIT_ORDER"
           dh-domain/src/main/java/.../DecisionAction.java
           dh-domain/src/main/java/.../DecisionOutput.java
结果       exit 1，无命中；未把交易动作加入 DecisionAction 或 DecisionOutput action。

命令       git diff --check
结果       通过；exit 0；仅 Windows LF -> CRLF warning，无 whitespace error。

命令       git diff --stat
结果       tracked diff 将随 `TESTING.md` / `WORKLOG.md` 本节追加增加；新增 K5 value objects、
           guard components 和 tests 仍由 `git status --short` 标识为 untracked，未自动 stage。

Readiness decision
           ALLOW_K5_CLOSE: YES
           ALLOW_K6_IMPLEMENTATION: YES
           ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未实现 K6-K8；未新增 API path；未新增 Controller；未新增 migration；未新增 provider health /
           budget / latency 表；未接真实 provider；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；
           未真实交易所调用；未读取或输出 credential、token、cookie、API secret、passphrase；未接 OpenAI /
           Claude / Gemini / 本地模型；未接 LangGraph；未启动 Integration-1 runtime；未开启 LIVE；
           未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

准入决定   K5 Provider Health / Budget / Latency 已完成 implementation 并通过模块 Maven、全仓 Maven、
           quality validate、K5 边界关键词扫描与 DecisionAction/DecisionOutput 交易动作扫描。
           下一步允许进入 K6 mock NQ dry-run contract tests；不得跳到 K7-K8、M2 close review、
           Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。
```

## 52. 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS 验证记录

```text
日期       2026-07-01
阶段       DH-STAGE4-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS
类型       CODE_CHANGE + CONTRACT_TESTS + MOCK_NQ_DRYRUN + SECURITY_BOUNDARY + NO_LIVE_TRADE + DOCS_SYNC
范围       只实现 K6 mock NQ dry-run contract tests、test-support 与最小 fixture；不新增 API / Controller / migration；
           不修改生产代码；不真实 HTTP；不接真实 NQ runtime / real provider / LLM / LangGraph / LIVE。

K6 新增测试
  - MockNqDecisionDryRunContractTest 2/2：
    mock NQ valid request -> structured DecisionOutput；fixture 字段与禁止词校验。
  - MockNqDecisionNoLiveTradeContractTest 3/3：
    DecisionAction vocabulary 无 BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER；
    Decision Pipeline 生产代码无 outbound runtime / Controller annotation token；fixture 无 execution intent / credential。
  - MockNqDecisionPersistenceReplayContractTest 2/2：
    mock NQ dry-run 写入 K3 request / trace / provider / output / audit 后，K4 replay read model 可读回；
    replay read model 不跨 tenant 返回明细。
  - MockNqDecisionProviderGuardContractTest 3/3：
    provider disabled / budget exceeded / timeout 均 fail-closed 到 ABSTAIN，并保留 structured output。

命令       git status --short
结果       通过；显示本轮新增 K6 tests / support / golden_cases 与 docs/current 修改，均未 stage。

命令       git diff --check
结果       通过；exit 0；无 whitespace error。

命令       git diff --stat
结果       通过；tracked diff 为 docs/current 六个状态文档同步，267 insertions / 44 deletions；
           新增未跟踪 tests / fixture 由 git status 标识。

命令       mvn -ntp -pl dh-usecase -am "-Dtest=MockNqDecisionDryRunContractTest,MockNqDecisionProviderGuardContractTest,MockNqDecisionPersistenceReplayContractTest,MockNqDecisionNoLiveTradeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
结果       BUILD SUCCESS；K6 新增测试 10 tests / 0 failures / 0 errors / 0 skipped；
           Finished at 2026-07-01T22:58:37+08:00。
说明       首次未加引号的 PowerShell `-Dtest=..., ...` 被逗号解析阻断；随后加引号并补 `-am` 与
           `surefire.failIfNoSpecifiedTests=false` 后通过。前两次失败是命令形态问题，不是代码失败。

命令       mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
结果       BUILD SUCCESS；reactor 15/15 SUCCESS；Finished at 2026-07-01T22:54:40+08:00。
补充       `PostgresContainerSmokeTest` 实际访问 Docker Desktop，启动 `postgres:17`，Flyway V1-V5 在 PostgreSQL 17.10 上迁移通过。

命令       mvn -ntp test
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；Finished at 2026-07-01T22:55:23+08:00。
测试摘要   Surefire XML 汇总 424 tests / 0 failures / 0 errors / 0 skipped。
补充       `PostgresContainerSmokeTest` 未 skip，真实启动 `postgres:17` 并迁移 V1-V5。

命令       mvn -ntp -Pquality validate
结果       BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；
           Finished at 2026-07-01T22:56:01+08:00。
补充       各子模块仍输出既有 `unable to find checkstyle:checkstyle outputFile` 信息；聚合 checkstyle 最终为
           0 violations，reactor status 为 SUCCESS。

命令       K6 边界关键词扫描
范围       dh-domain/src/main、dh-usecase/src/main、dh-infra/src/main、dh-app/src/main、dh-api/src/main、
           dh-connector/src/main、dh-security/src/main、contracts、golden_cases、docs/current、K6 新增测试；
           排除 target / build / dist / node_modules / logs / test-results / secrets / credentials。
关键词     RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate / HttpClient /
           placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase / accountId / Controller /
           NqClient / Exchange / Broker / live / LIVE / endpoint / Endpoint / PostMapping /
           GetMapping / RequestMapping。
结果       命中项均为既有 Controller / mapping、历史或禁止说明、denylist、负向安全断言、K6 no-live-trade 测试、
           fixture 文件名语义或 docs/current 边界说明；未发现本轮新增 API / Controller / migration / RealClient /
           real provider / HTTP client / NQ runtime / LangGraph runtime / LIVE / BUY-SELL action 生产实现。

Readiness decision
           ALLOW_K6_CLOSE: YES
           ALLOW_K7_IMPLEMENTATION: YES
           ALLOW_STAGE4_ACCEPTANCE_REVIEW: NO
           ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_ACCEPTANCE_REVIEW: NO
           ALLOW_INTEGRATION_1_RUNTIME: NO
           ALLOW_AGENT_PHASE: NO
           ALLOW_LANGGRAPH_RUNTIME: NO
           ALLOW_LIVE: NO

边界       未实现 K7-K8；未新增 API path；未新增 Controller；未新增 migration；未新增 replay API /
           query endpoint；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；
           未新增 RealClient / 真实 Provider；未读取或输出 credential、token、cookie、API secret、passphrase；
           未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未启动 Integration-1 runtime；
           未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；
           未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

下一步     DH-STAGE4-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL / NOT STARTED。
```

## 2026-07-03 NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO 验证记录

结论：**PASS / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NO_RUNTIME**。

本轮只验证 M1 work order 与 docs/current 同步结果；未改生产代码、测试代码、Controller/API、migration、schema、contracts、golden_cases、fixture JSON、runtime、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE。

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `git status --short` | **PASS** | DH worktree 仅显示允许的 `docs/current` 修改与新增 M1 work order 文档，未 stage。 |
| `git diff --check` | **PASS** | exit 0；仅 Windows LF/CRLF 转换 warning；无 whitespace error。 |
| `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases` | **PASS / EMPTY** | 禁止范围无 diff；未改代码、契约、golden cases。 |
| `mvn -ntp test` | **BUILD SUCCESS** | reactor 19/19 SUCCESS；Finished at 2026-07-03T17:43:39+08:00；Docker/Testcontainers 不可用导致既有环境相关 4 skips：`JdbcNonceReplayGuardPersistenceTest` 3 skips、`PostgresContainerSmokeTest` 1 skip。 |
| `mvn -ntp -Pquality validate` | **BUILD SUCCESS** | reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；Finished at 2026-07-03T17:46:57+08:00。 |
| NQ worktree `mvn -ntp -f backend/pom.xml test` | **BUILD SUCCESS** | 在 `F:\worktrees\nexus-quant-i1-dryrun` 执行；reactor 23/23 SUCCESS；Finished at 2026-07-03T17:45:27+08:00；`nq-app` 86 tests 中 2 skips 为既有环境/guard 条件。 |
| NQ worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test` | **BUILD SUCCESS** | Integration0 定向验证 17 tests / 0 failures / 0 errors / 0 skipped；Finished at 2026-07-03T17:47:23+08:00。 |
| NQ dev worktree read-only diff guard | **PASS** | `F:\project\nexus-quant` 仅做 git status/branch/log/diff；存在非本任务 mainline dirty 文件，但 `docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 dirty diff；`WORKSTREAM_MIXED_BLOCKED: NO`。 |

M1 readiness：

```text
ALLOW_M1_WO_CLOSE: YES
ALLOW_I1_M2_NQ_DRYRUN_STUB_RECORDER_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

边界确认：未触达生产环境、真实交易、真实交易所私有 API、NQ DB、NQ mutation、credential、token、cookie、API secret、passphrase；未把 DH 写成 integrated；未把 Integration-1 runtime 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE。
