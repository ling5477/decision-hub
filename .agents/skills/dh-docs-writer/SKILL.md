---
name: dh-docs-writer
description: Decision Hub documentation governance skill. Use for DH docs/current updates, Gate/Phase/Stage planning, work orders, acceptance/freeze/close review documents, DH-NQ integration documentation sync, Decision Pipeline MVP planning docs, safety boundary docs, API/TESTING/WORKLOG/ROADMAP/STATUS updates, and docs/gates archive tasks. This skill is docs-only and must not write production code, tests, APIs, migrations, runtime clients, providers, AI runtime, LangGraph runtime, NQ runtime integration, or LIVE behavior.
---

# DH Docs Writer

## Overview

Use this skill to keep Decision Hub documentation traceable, reviewable, and aligned with `docs/current` as the current fact source. This skill governs documentation only; it does not authorize implementation, runtime integration, external provider wiring, NQ mutation, AI runtime, LangGraph runtime, or LIVE trading.

Always use `nq-dh-workflow-router` first for task classification, plugin routing, scope narrowing, and boundary checks. Use `dh-docs-writer` as the primary skill only when the requested work is documentation governance or documentation synchronization.

## When To Use

Use this skill for:

- DH `docs/current` updates.
- DH Stage / Phase planning. DH 自身阶段不得使用 Gate 命名。
- DH work order creation and review.
- DH acceptance, freeze, close review, and archive documentation.
- DH and NQ integration documentation synchronization.
- DH Decision Pipeline MVP documentation planning.
- DH security boundary documentation.
- DH `API.md`, `TESTING.md`, `WORKLOG.md`, `ROADMAP.md`, `STATUS.md`, and `README.md` synchronization.
- DH `docs/gates` archive governance.

Do not use this skill to write Java, tests, controllers, services, repositories, clients, providers, migrations, runtime config, LangGraph runtime, model-provider integration, replay APIs, audit tables, snapshot tables, trace tables, or live trading behavior.

## Preflight

Before writing documentation:

1. Read `AGENTS.md`, `README.md`, `docs/current/README.md`, and the target document.
2. For current-state changes, read `docs/current/STATUS.md`, `ROADMAP.md`, `TESTING.md`, and `WORKLOG.md`.
3. For API documentation, read `docs/current/API.md` and `contracts/openapi.yaml`; document only real implemented API.
4. For DH/NQ integration docs, read the current Integration-0 / header / timestamp / security documents first.
5. Check `git status --short` before writing and do not overwrite user changes.
6. Declare allowed files, forbidden files, expected output, and validation commands.

Do not scan generated, secret-bearing, or unrelated areas for docs work unless the user explicitly authorizes a bounded audit:

```text
node_modules
target
build
dist
.git
logs
test-results
coverage
dumps
backups
secrets
credentials
.env
*.key
*.pem
```

## Fact Source Rules

Use this priority order when facts conflict:

1. Current code and actual command results.
2. `docs/current/STATUS.md`, `ROADMAP.md`, `TESTING.md`, and `WORKLOG.md`.
3. Other `docs/current` authority documents.
4. Root `README.md` and `docs/current/README.md` as entry indexes and summaries.
5. `contracts/**` and `golden_cases/**` as read-only contract evidence unless the task explicitly authorizes contract edits.
6. `docs/gates/**` and `docs/archive/**` as historical evidence, not current authority.
7. Attachments and prompts as task inputs that still need reconciliation against current facts.

Rules:

- DH current fact source is `docs/current`.
- Root `README.md` is an entry point, not the final fact source.
- If root `README.md` conflicts with `docs/current`, trust `docs/current` and synchronize `README.md` within the approved docs scope.
- `docs/gates` stores historical frozen snapshots. Do not edit historical gate snapshots unless the task is an explicit freeze/archive task.
- If a historical snapshot needs correction, add an errata or current-doc clarification; do not rewrite frozen history.
- `docs/archive`, if present, is historical reference only and must not become the current state source.

## Stage Naming Rules

DH 与 NQ 使用不同阶段命名体系，文档治理必须先区分项目归属，再写阶段名。

- DH 自身开发、DH 自身文档、DH 自身冻结和 DH `docs/gates` 归档必须使用 Stage 体系。
- DH 阶段 ID 使用 `dh-stage<N>-<topic>`，或沿用已存在的历史兼容形式，例如 `dh-stage1`、`dh-stage2-poc`、`dh-stage3-plan`、`dh-stage4-decision-pipeline-mvp`。
- NQ 自身开发、NQ 自身冻结和 NQ 当前阶段可以使用 Gate 体系，例如 `GateN`。
- NQ-DH 集成任务可以写 `NQ GateN rebase`，但不得把 DH 自身阶段写成 `GateK`、`GateL`、`GateN`。
- DH 文档中出现 `DH-GATEK-*` 时，应标记为错误命名或历史错误，并在后续命名 rebase 中改为 `DH-STAGE4-*`。不要在新的 DH 任务名、当前阶段名或冻结目录名中继续扩散 `DH-GATEK-*`。
- DH `docs/gates` 冻结目录应使用 DH Stage ID；`docs/gates/dh-gatek-*` 只能作为历史错误残留或待迁移对象，不得作为新的标准冻结路径。
- 后续 Integration-1 文档前置条件应写 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED`，不得写 `NQ GateN + DH GateK CLOSED`。
- 如果本轮任务不是命名 rebase，不要批量重命名现有 current docs 或移动 `docs/gates`；只记录 residual，并把下一步指向 `DH-STAGE4-NAMING-REBASE-FIX`。

## Current DH Baseline

Baseline captured for the DH Stage4 naming correction line on 2026-07-02. Re-read `docs/current` each turn before repeating these facts.

```text
Integration-0 safety gate: CLOSED / ACCEPTED
P1-4 residual: CLOSED
header alignment: CLOSED
timestamp alignment: CLOSED
code reality audit blockers: fixed
DH security state: FULL
DH fail-closed state: FULL
Integration-0 contract state: MATCH
Decision pipeline state: PARTIAL
Audit state: PARTIAL
Replay state: PARTIAL
Integration-1: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LIVE: DISABLED
DH canonical stage: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED
Legacy erroneous stage name: DH-GATEK-DECISION-PIPELINE-MVP / HISTORICAL_ERROR / NAMING_REPLACED
Legacy erroneous freeze dir: docs/gates/dh-gatek-decision-pipeline-mvp / NAMING_REPLACED
Decision Pipeline MVP PLAN: ACCEPTED / CLOSED
Decision Pipeline MVP WO: ACCEPTED / CLOSED
Decision Pipeline MVP K1-K8: CLOSED / ACCEPTED
Next naming action: DH-STAGE4-NAMING-REBASE-FIX / CLOSED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline: GateN
```

Do not use old `Integration-0 not started / plan only` lines as current authority when newer `docs/current` evidence says Integration-0 is `CLOSED / ACCEPTED`. Do not use old GateK Integration-1 plans as the current route after NQ has moved to GateN.

## Document Update Rules

- `STATUS.md` records current state, allowed scope, forbidden scope, and gate decisions.
- `ROADMAP.md` records phase route, next action, and blocked/deferred lines.
- `WORKLOG.md` records what actually happened in this turn. Prefer append-only entries.
- `TESTING.md` records actual commands, results, scope, warnings, not-run items, and blocking/non-blocking status.
- `API.md` records real implemented API only. Planned API must be clearly marked as not implemented or remain in plan docs.
- `README.md` is an entry and concise status summary; do not make it the full fact source.
- New planning documents must be indexed from `docs/current/README.md`.
- Every documentation change must synchronize relevant indexes. Do not create orphan documents.
- Do not write a command as passed unless it was actually run and passed.
- Do not write a validation as complete when it was skipped, blocked, or failed.

## Language Rules

DH 文档与注释默认使用中文为主，英文只保留在稳定工程标识和外部协议中。

- DH 文档正文、架构说明、阶段计划、Worklog、Testing、Roadmap、Status 默认中文。
- DH 代码注释 / Javadoc 默认中文；类名、方法名、字段名、包名、enum、JSON 字段、OpenAPI 字段、HTTP header、状态枚举和外部技术名保持英文原样。
- Maven / Spring / Git / Docker / CI 命令、LangGraph / Spring AI / MCP / PostgreSQL 等外部技术名可以保留英文。
- 固定输出字段可以保留英文，例如 `Task classification`、`Scope`、`Files inspected`、`Files changed`、`Validation`、`Risks`、`Next concrete action`；字段内容必须中文为主。
- 不允许把中文项目文档整篇漂移为英文说明，不允许新增英文长段落，除非是协议、代码片段、命令、schema 或外部规范引用。
- 不允许为了“专业感”把中文业务概念翻译成不稳定英文术语后反复使用。
- 如果从 NQ skill 或历史文档同步规则，必须改写为 DH 中文主语言风格，不能照搬英文段落。
- 如果 Codex 输出了大段英文说明，必须在同轮改为中文，除非该英文属于上述允许保留范围。

## Gate And Freeze Rules

- A stage may be copied into `docs/gates/<stage-id>/` only after completion and explicit freeze/archive authorization.
- Freeze tasks must include a freeze statement, file list, status alignment, validation record, known residuals, and post-freeze rules.
- Do not edit historical gate snapshots as if they were current docs.
- If historical facts are stale, write current-doc clarification or errata rather than mutating the frozen snapshot.
- Archive governance is documentation-only and must not authorize business code, CI, migration, backend, frontend, Python, LIVE, AI runtime, DH runtime, RealClient, real provider, credential, or exchange changes.

## State Vocabulary

Use precise state words:

```text
NOT STARTED
PLANNING
PLAN ACCEPTED
IMPLEMENTING
DONE
CLOSED
ACCEPTED
BLOCKED
SUPERSEDED
REBASE_REQUIRED
DISABLED
NOT INTEGRATED
```

Do not use vague or unsafe state words:

```text
basically done
almost integrated
kind of started
ready for live
partially live
will be connected
```

## Boundary Rules

DH documentation must never claim these capabilities unless future code, tests, acceptance reports, and an explicit gate all prove and allow them:

- DH can directly place orders.
- DH can cancel orders.
- DH can bypass NQ risk control.
- DH can modify NQ order state.
- DH can read NQ DB.
- DH can write NQ DB.
- DH can connect a real provider.
- DH can start LIVE.
- DH can let AI output trading instructions.
- LangGraph is connected to runtime.
- Integration-1 runtime has started.
- Runtime integration has started.
- DH is integrated with NQ runtime.

Always keep these forbidden unless a later explicit gate changes them:

```text
RealClient forbidden
real provider forbidden
real HTTP forbidden
NQ mutation forbidden
NQ DB access forbidden
LIVE disabled
AI / Agent runtime not started
LangGraph runtime not connected
Integration-1 runtime not started
```

## Decision Pipeline Documentation Rules

Decision Pipeline MVP documents may plan:

- `DecisionRequest`
- `DecisionOutput`
- `DecisionOrchestrator`
- Snapshot
- Trace
- Replay
- Audit

Before implementation and verification, never write these as `DONE` or implemented.

First-version `DecisionOutput` must be:

```text
READ_ONLY_RECOMMENDATION
```

`action` must not contain:

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
```

`forbiddenActions` must contain:

```text
PLACE_ORDER
CANCEL_ORDER
MUTATE_NQ_STATE
READ_NQ_DB
WRITE_NQ_DB
```

Fail-closed defaults:

- If evidence is insufficient, default to `ABSTAIN`.
- If a provider fails, default to `ABSTAIN`.
- If policy denies a request, fail closed.
- If audit writing fails, fail closed unless a later accepted design explicitly makes it asynchronous and traceable.

## NQ / DH Integration Documentation Rules

- NQ has moved to GateN; do not create new current plans that continue to use NQ GateK as the active phase name.
- Old GateK Integration-1 plans are historical references only.
- Mark old `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` as `SUPERSEDED / REBASE_REQUIRED`.
- DH Decision Pipeline MVP must be referenced as Stage4 in new current wording: `DH-STAGE4-DECISION-PIPELINE-MVP` / `dh-stage4-decision-pipeline-mvp`.
- Use `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED` as the Integration-1 prerequisite wording.
- New Integration-1 plans must use GateN rebase wording, for example:
  - `NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN`
  - `NQ-DH-GATEN-INTEGRATION1-DRYRUN-PLAN`
- Integration-1 dry-run does not equal runtime.
- Dry-run does not equal real trading.
- NQ records but does not execute.
- DH returns only structured decision, risk opinion, or recommendation.

## Output Format

For DH documentation tasks, final output must include:

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Implementation / Plan result:
Validation:
Boundary confirmation:
Risks:
Next concrete action:
```

For review tasks, include:

```text
Findings:
Decision:
Allow / Block:
```

Do not make `Summary` a required field. Put conclusions in `Findings` or `Implementation / Plan result`.

## Validation Rules

For every DH docs-only task, run at least:

```powershell
git status --short
git diff --check
git diff --stat
```

When scope boundaries matter, also run forbidden-scope diffs:

```powershell
git diff -- dh-domain
git diff -- dh-usecase
git diff -- dh-memory
git diff -- dh-eval
git diff -- dh-connector
git diff -- dh-api
git diff -- dh-app
git diff -- dh-infra
git diff -- contracts
git diff -- golden_cases
```

If environment and time allow, run:

```powershell
mvn test
mvn -Pquality validate
```

If Maven or quality validation is not run, state the reason. If a command fails, record the real failure reason and do not write success.

## Security And Credential Rules

Documentation must not contain or print:

```text
token
cookie
API secret
passphrase
private key
exchange key
database password
real credential material
```

If suspected credential material is encountered, redact it and do not output the original value.

## Relationship To Other Skills

- `dh-docs-writer` governs DH documentation.
- `nq-dh-workflow-router` classifies tasks, selects plugin route, narrows scope, and enforces the standard output format.
- If a task requires code implementation, route it to the relevant implementation skill; do not execute it under `dh-docs-writer`.
- If a task requires security audit, use security-audit rules; do not treat it as wording polish.
- If a task touches the NQ repository, use NQ-scoped rules and skills. Do not modify NQ from a DH documentation skill.
