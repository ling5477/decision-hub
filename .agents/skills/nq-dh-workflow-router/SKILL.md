---
name: nq-dh-workflow-router
description: Decision Hub and NexusQuant workflow router. Use before every DH/NQ Codex task to classify task type, select allowed plugins, restrict scope, enforce output format, and preserve DH/NQ safety boundaries, especially documentation, code analysis, code change, security audit, agent API, integration planning, product design, and presentation work.
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

## Stage Naming Route

Before choosing files or next actions, classify whether the task is DH-owned, NQ-owned, or NQ-DH integration work:

- DH-owned development, DH-owned documentation, DH-owned freeze, and DH `docs/gates` archive work must use Stage naming.
- NQ-owned development and NQ-owned freeze work may use Gate naming, for example `GateN`.
- NQ-DH integration work may use `Integration-1` and may reference `NQ GateN rebase`, but it must not rewrite the DH-owned stage as `GateK`, `GateL`, or `GateN`.
- If a task or fact source uses `DH GateK`, `DH GateL`, `DH GateN`, `DH-GATEK-*`, `DH-GATEL-*`, or `DH-GATEN-*` for DH-owned work, route first to a naming repair task. Do not continue business implementation, freeze, runtime integration, or Integration-1 progress until the naming residual is classified.
- The canonical DH Decision Pipeline MVP stage is `DH-STAGE4-DECISION-PIPELINE-MVP`; the legacy `DH-GATEK-DECISION-PIPELINE-MVP` wording is a historical error and should be reworked by `DH-STAGE4-NAMING-REBASE-FIX`.
- DH `docs/gates` directories should use DH Stage IDs such as `dh-stage4-decision-pipeline-mvp`; `docs/gates/dh-gatek-*` is not a standard path for new DH freeze work.

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
