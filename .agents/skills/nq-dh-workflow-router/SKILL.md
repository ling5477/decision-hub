---
name: nq-dh-workflow-router
description: Decision Hub and NexusQuant workflow router. Use before every DH/NQ Codex task to detect the repository, read current Authority, classify the task, compose domain/security and repository-specific Java Engineering skills, restrict scope, and preserve safety boundaries.
---

# NQ DH Workflow Router

## Purpose

Use this skill as the first step for Decision Hub and NexusQuant work. It classifies the task, narrows file scope, selects the plugin route, and blocks unsafe integration or trading actions.

## Startup Scope

Before execution, explicitly identify:

```text
repository
module
target files
excluded files
expected output
```

Do not scan these directories unless the user explicitly names a file inside them and the current gate allows it:

```text
node_modules
target
build
dist
.git
logs
test-results
secrets
credentials
```

Do not read, copy, commit, or print these sensitive materials:

```text
token
cookie
API key
API secret
exchange secret
production .env
private key
mnemonic
keystore password
2FA backup code
```

Do not treat archived, historical, or superseded documents as current source of truth. Unless the user explicitly requests historical comparison, use the current sections in `STATUS.md`, `AGENTS.md`, `CODEX_PROJECT_INSTRUCTIONS.md`, and `WORK_ORDER.md`. Historical Stage documents may provide background only and must not become the current next task automatically.

## Required Classification

Choose exactly one primary classification:

```text
DOCUMENTATION
CODE_ANALYSIS
CODE_CHANGE
SECURITY_AUDIT
AGENT_API
NQ_INTEGRATION_PLAN
PRODUCT_DESIGN
PRESENTATION
```

If a task mixes categories, choose the highest-risk category in this order:

```text
SECURITY_AUDIT > AGENT_API > NQ_INTEGRATION_PLAN > CODE_CHANGE > CODE_ANALYSIS > PRODUCT_DESIGN > PRESENTATION > DOCUMENTATION
```

## Plugin Route

```text
DOCUMENTATION        GitHub + Documents + Notion
CODE_ANALYSIS        GitHub
CODE_CHANGE          GitHub + CodeRabbit
SECURITY_AUDIT       GitHub + Codex Security + CodeRabbit
AGENT_API            GitHub + OpenAI Developers + Codex Security
NQ_INTEGRATION_PLAN  GitHub + Documents + Codex Security
PRODUCT_DESIGN       Figma + Product Design
PRESENTATION         Presentations + Documents + Canva
```

Plugin selection does not grant permission to connect external systems, read secrets, access databases, or mutate NQ/DH production state. Use local files first unless the user explicitly authorizes a connector workflow and it is within the current gate.

## Repository Detection And Java Engineering Route

Resolve the repository from `git rev-parse --show-toplevel` and repository-owned Authority files before selecting a Java skill. Do not infer the repository from a task name, historical path, branch name, or model memory.

Fail closed when a Java-scoped task cannot resolve exactly one supported repository:

```text
FAIL_CLOSED / REPOSITORY_UNRESOLVED
```

Load the repository-specific Java Engineering skill when the task adds, modifies, reviews, debugs, refactors, or validates any of these scopes:

```text
Java source
Spring Boot / Spring Framework / Spring MVC / Spring Security / Spring Data
JDBC / transaction / Repository / Service / Controller / Adapter / Port / SPI
concurrency / executor / thread / async / logging / exception handling
Maven Java dependencies
JUnit / Mockito / ArchUnit / Checkstyle / Spotless / PMD / SpotBugs
Java refactoring / Java review
```

Repository routes:

```text
repository = decision-hub AND Java scope exists
  -> dh-java-engineering-standard

repository = nexus-quant AND Java scope exists
  -> nq-java-engineering-standard
```

Do not load a Java Engineering skill for a pure documentation, React/TypeScript-only, Python-only, Authority-only, read-only acceptance, or Git-only task that does not modify or review Java/Maven-Java behavior.

## Skill Composition, Order And Precedence

Execution order is fixed:

```text
1. nq-dh-workflow-router
2. repository detection
3. current Authority / Gate / Stage / task boundary
4. task-specific domain, security, review, migration, API, or documentation skill when applicable
5. repository-specific Java Engineering skill when Java scope exists
6. implementation or review
7. validation
```

Skills compose rather than compete. For example, a Decision Hub Java security review loads the security/review skill first and `dh-java-engineering-standard` second. A pure WORKLOG append or React/TypeScript-only task does not load the Java skill.

Precedence is fixed:

```text
Authority / Gate / Stage
> repository domain invariants
> repository-specific Java Engineering skill
> adapted Huangshan rules
> formatting preference
```

The Java Engineering skill cannot grant permission, override Authority, Gate/Stage, contracts, Schema, state machines, runtime modes, exchange/provider boundaries, or real integration. The Router records only when to load which skill, ordering, precedence, and fail-closed behavior; it does not duplicate Java, Spring, Huangshan, financial, atomicity, or implementation rules.

Recursive routing is forbidden:

```text
dh-java-engineering-standard -> nq-dh-workflow-router
nq-java-engineering-standard -> nq-dh-workflow-router

RECURSIVE_ROUTING_REJECTED
```

Once the Router has selected a Java Engineering skill for the current task, that skill must not re-enter the Router.

## Stage Naming Route

Before choosing files or next actions, classify whether the task is DH-owned, NQ-owned, or NQ-DH integration work:

- DH-owned development, DH-owned documentation, DH-owned freeze, and DH `docs/gates` archive work must use Stage naming.
- NQ-owned development and NQ-owned freeze work may use Gate naming, for example `GateN`.
- NQ-DH integration work may use `Integration-1` and may reference `NQ GateN rebase`, but it must not rewrite the DH-owned stage as `GateK`, `GateL`, or `GateN`.
- If a task or fact source uses `DH GateK`, `DH GateL`, `DH GateN`, `DH-GATEK-*`, `DH-GATEL-*`, or `DH-GATEN-*` for DH-owned work, route first to a naming repair task. Do not continue business implementation, freeze, runtime integration, or Integration-1 progress until the naming residual is classified.
- The canonical DH Decision Pipeline MVP stage is `DH-STAGE4-DECISION-PIPELINE-MVP`; the legacy `DH-GATEK-DECISION-PIPELINE-MVP` wording is a historical error and must remain marked as `SUPERSEDED / NAMING_REPLACED` after `DH-STAGE4-NAMING-REBASE-FIX`.
- DH `docs/gates` directories should use DH Stage IDs such as `dh-stage4-decision-pipeline-mvp`; `docs/gates/dh-gatek-*` is not a standard path for new DH freeze work.

## Workflow Discipline Route

Archive, tag, review, and path rules:

- `archive-before-tag` is mandatory. A stage tag may be created only after the stage archive commit exists and a separate tag-close task explicitly authorizes tag creation.
- Stage close 后必须先形成 self-contained archive packet，单个 `README.md` 不满足 archive-before-tag 要求。
- Archive packet 必须至少记录计划、work order、batch 摘要、验证证据、final close、archive close、状态快照、边界确认、风险、下一步和 tag state；packet 不完整时 tag close 必须 BLOCKED。
- Archive close and tag close are separate steps. Do not mark a tag as created unless the tag command actually created it and, if required, pushed it.
- Stage-QDR-6 planning 必须等 Stage-QDR-5 archive packet 完整、archive policy repair commit 完成、Stage-QDR-5 tag close 完成后，作为独立 planning-first 任务启动。
- Closed stage current residue must be treated as a cleanup blocker: if `docs/current` still contains completed-stage plan / WO / batch source docs, move them to `docs/gates/<stage>/SOURCE_*` and verify no residue before next-stage planning.
- Stage-QDR-5 must be planning-first. Do not start Stage-QDR-5 implementation, runtime, provider, HTTP, Agent, LangGraph, or LIVE from a tag, cleanup, archive, or close task.
- Only these conditions trigger standalone review: migration, API / Controller, security boundary, stage close, P0 / P1 blocker.
- A normal batch should close as implementation + tests + boundary scan + minimal docs + commit. Do not add standalone review/freeze for every ordinary batch.
- Use the actual repository path from the current environment. Do not hardcode local drive paths as facts.
- NQ dev and NQ integration worktrees must not be mixed. If a task needs NQ, explicitly identify which worktree is allowed; DH documentation tasks must not modify NQ.

## DH Safety Boundary

Treat DH as a multi-agent decision system only:

```text
Allowed:
  research
  analysis
  candidate signals
  risk explanations
  audit records
  structured reports

Forbidden:
  order placement
  order cancellation
  strategy state mutation
  Paper Run start
  LIVE trading
  exchange credential access
  direct NQ DB read/write
  NQ mutation
  NQ RealClient
  real provider
```

Any future DH to NQ connection must start with `Integration-0-PLAN`.

`Integration-0` is plan-only and may cover only:

```text
read-only boundary
contract freeze
permission model
audit model
field classification
error-code matrix
replay protection design
```

It must not implement real business integration.

## Required Output Format

Every response must use these fields:

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Findings:
Validation:
Risks:
Next concrete action:
```

Do not make `Summary` a required field. Put conclusions in `Findings`; put next work in `Next concrete action`.

## Validation Checklist

Before closing a task, verify:

```text
nq-dh-workflow-router was considered before task execution.
Task classification is present.
Plugins selected matches the classification.
Scope does not exceed the user-approved files and current gate.
Files inspected and files changed are explicit.
Findings is used instead of Summary.
NQ integration is not described as started or completed unless a gate explicitly says so.
RealClient, real provider, LIVE trading, NQ mutation, NQ DB access, and secret access remain forbidden.
Business code was not changed for DOCUMENTATION tasks.
DH-owned stages use Stage naming, NQ-owned stages use Gate naming, and NQ-DH integration wording does not convert DH Stage4 into DH GateK/GateL/GateN.
```
