# DH Codex Task Templates

> Status: active workflow template
> Output field: `Findings`
> Forbidden required field: `Summary`

## 1. DOCUMENTATION

```text
Task classification: DOCUMENTATION
Plugins selected: GitHub + Documents + Notion
Scope:
- Only documentation, rule, index, status, worklog, testing files approved by the user.
Files inspected:
- <paths>
Files changed:
- <paths>
Findings:
- <documentation changes and state alignment>
Validation:
- git status --short
- git diff --check
Risks:
- No business-code validation unless code changed.
Next concrete action:
- <one concrete next step>
```

## 2. CODE_ANALYSIS

```text
Task classification: CODE_ANALYSIS
Plugins selected: GitHub
Scope:
- Read-only code and diff inspection.
Files inspected:
- <paths>
Files changed:
- None
Findings:
- <bugs, risks, architecture notes, line references>
Validation:
- <read-only commands>
Risks:
- <unknowns and unverified assumptions>
Next concrete action:
- <recommended next step>
```

## 3. CODE_CHANGE

```text
Task classification: CODE_CHANGE
Plugins selected: GitHub + CodeRabbit
Scope:
- Files allowed by current Work Order and user request.
Files inspected:
- <paths>
Files changed:
- <paths>
Findings:
- <implementation notes and behavior changes>
Validation:
- mvn test
- mvn -Pquality validate when relevant
Risks:
- <residual risk and rollback>
Next concrete action:
- <next implementation or review step>
```

## 4. SECURITY_AUDIT

```text
Task classification: SECURITY_AUDIT
Plugins selected: GitHub + Codex Security + CodeRabbit
Scope:
- Security-relevant files, configs, contracts, auth, IO, provider, DB, and integration boundaries.
Files inspected:
- <paths>
Files changed:
- None unless explicitly fixing findings.
Findings:
- <ordered findings by severity with file/line references>
Validation:
- <commands and evidence>
Risks:
- <open risks and false-negative limits>
Next concrete action:
- <highest-priority fix or review gate>
```

## 5. AGENT_API

```text
Task classification: AGENT_API
Plugins selected: GitHub + OpenAI Developers + Codex Security
Scope:
- Agent API, tool contract, auth, logging, and audit model only.
Files inspected:
- <paths>
Files changed:
- <paths>
Findings:
- <API contract and safety model>
Validation:
- mvn test or relevant contract test
Risks:
- real provider remains forbidden in the current DH state; any future provider work requires a separate gate and security review.
Next concrete action:
- <next API/security step>
```

## 6. NQ_INTEGRATION_PLAN

```text
Task classification: NQ_INTEGRATION_PLAN
Plugins selected: GitHub + Documents + Codex Security
Scope:
- Plan, schema, permission model, audit model, read-only boundary, risk list.
Files inspected:
- <paths>
Files changed:
- <docs only unless explicitly approved>
Findings:
- <boundary and contract decisions>
Validation:
- git status --short
- git diff --check
Risks:
- NQ integration not started; RealClient, real provider, LIVE trading, NQ mutation forbidden.
Next concrete action:
- Start or refine Integration-0-PLAN only.
```

## 7. PRODUCT_DESIGN

```text
Task classification: PRODUCT_DESIGN
Plugins selected: Figma + Product Design
Scope:
- UI / workflow diagram / prototype only.
Files inspected:
- <paths or design URLs>
Files changed:
- <design docs or frontend files if explicitly approved>
Findings:
- <flow, states, risks, UX decisions>
Validation:
- visual check / build / E2E when frontend changed
Risks:
- PRODUCT_DESIGN tasks do not change API, DB, trading, provider, or integration scope.
Next concrete action:
- <next design or implementation step>
```

## 8. PRESENTATION

```text
Task classification: PRESENTATION
Plugins selected: Presentations + Documents + Canva
Scope:
- Slides, narrative, speaker notes, diagrams, and export assets.
Files inspected:
- <paths>
Files changed:
- <presentation artifacts>
Findings:
- <deck structure and key messages>
Validation:
- render/export check
Risks:
- Source facts must remain tied to docs/current.
Next concrete action:
- <review or export step>
```
