# DH GateK Decision Pipeline MVP Plan

> Task: DH-GATEK-DECISION-PIPELINE-MVP-PLAN
> Status: PLAN / READY FOR REVIEW
> Scope: docs-only / plan-only
> Date: 2026-07-01

## 0. Scope And Boundary

This document plans the Decision Hub GateK Decision Pipeline MVP. It does not
implement production code, test code, API paths, migrations, runtime clients,
real providers, AI runtime, LangGraph runtime, NQ runtime integration, or LIVE
behavior.

Current allowed work:

```text
DecisionRequest / DecisionOutput contract planning
DecisionOrchestrator skeleton planning
DecisionSnapshot / DecisionTrace / Audit / Replay planning
Mock Provider / Disabled Provider planning
Mock NQ dry-run contract test planning
Provider health / budget / latency planning
Golden Cases / Eval baseline planning
Acceptance / Freeze planning
```

Current forbidden work:

```text
Java / Kotlin / Python / TypeScript production code changes
test code changes
new Controller
new API path
new migration
new Repository / Service / Client implementation
real HTTP
real NQ call
real DH runtime integration
exchange call
RealClient
real Provider
OpenAI / Claude / Gemini / local model runtime
LangGraph runtime
MCP write capability
DecisionOrchestrator implementation
replay API implementation
audit / snapshot / trace table implementation
credential / token / cookie / API secret / passphrase access
Integration-1 runtime
DH integrated wording
Runtime integration started wording
AI / Agent runtime started wording
LIVE
AI output as trading instruction
NQ repository changes
continuing old NQ GateK task as current main line
```

## 1. Stage Position

Current stage:

```text
DH GateK Decision Pipeline MVP planning
```

Current target:

```text
Decision Execution Engine MVP
```

Current state:

```text
Integration-0 safety gate: CLOSED / ACCEPTED
P1-4 residual: CLOSED
replay nonce persistence: CLOSED
bounded memory cap: CLOSED
inbound rate limit / 429 RATE_LIMITED: CLOSED
header alignment: CLOSED
timestamp alignment: CLOSED
code reality audit blockers: fixed
DH security state: FULL
fail-closed state: FULL
Integration-0 contract state: MATCH
Decision pipeline state: PARTIAL
Audit state: PARTIAL
Replay state: PARTIAL
NQ boundary violation: NO
Agent premature introduction: NO
External side effect state: NONE
Integration-1: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LIVE: DISABLED
```

This stage moves DH from a safety contract baseline toward a minimal auditable,
replayable, dry-run decision pipeline. It is still not a complete Agent system.

This stage is not:

```text
Integration-1 runtime
real NQ integration
real provider integration
LangGraph Agent Runtime
multi-agent phase
LIVE trading
```

## 2. Superseded Work Handling

The old task line is closed as a current route:

```text
NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
```

It must not be executed as-is. Only these constraints may be reused as safety
inputs:

```text
dry-run only
no LIVE
no real provider
no RealClient
no NQ mutation
no NQ DB read/write
no trading side effect
canonical X-NQ-DH-* header family
RFC3339 UTC Z timestamp
tenant / requestId / traceId binding
nonce replay protection
payload size guard
audit trail requirement
```

Future Integration-1 work must be planned against the NQ GateN baseline under a
new planning line, for example:

```text
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN
NQ-DH-GATEN-INTEGRATION1-DRYRUN-PLAN
```

## 3. GateK Batch Plan

### K0: Factsource Sync / Docs Rebase

Goal:

```text
Confirm docs/current as the current fact source and remove stale GateK /
Integration-0 wording from current docs.
```

Allowed files:

```text
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/API.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

Rules:

- Root `README.md` should be synchronized only in a separately authorized docs
  sync if the active allowed file list includes it.
- `docs/gates/**` is historical and must not be edited for this plan.
- `docs/codex/**` is historical or auxiliary and must not override
  `docs/current`.

Success:

```text
Current docs agree that GateK Decision Pipeline MVP planning is current.
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK remains SUPERSEDED / REBASE_REQUIRED.
NQ GateN is recorded as the future Integration-1 rebase baseline.
```

### K1: Decision Contract Freeze

Goal:

```text
Freeze first-version decision contracts as read-only recommendation contracts.
```

Planned contracts:

```text
DecisionRequest
DecisionContextSnapshot
DecisionPolicyResult
ProviderDecisionSignal
RiskReview
DecisionOutput
DecisionAuditEvent
DecisionReplayRecord
```

Schema rules:

```text
JSON Schema required for each public contract
required fields explicit
additionalProperties=false
enum values explicit
tenantId / requestId / traceId / decisionId traceable
forbiddenActions always present
action enum excludes BUY / SELL / PLACE_ORDER / CANCEL_ORDER
```

Success:

```text
Contract plan is reviewable before any Java DTO / schema / OpenAPI change.
DecisionOutput is constrained to READ_ONLY_RECOMMENDATION.
```

### K2: DecisionOrchestrator Skeleton Plan

Goal:

```text
Plan a linear minimal DecisionOrchestrator main path.
```

Planned flow:

```text
Signed Request
-> API / Contract / Security
-> DecisionOrchestrator
-> normalize
-> policy check
-> mock provider
-> risk review
-> structured decision output
-> audit / snapshot / trace
-> return DecisionOutput
```

Fail-closed rules:

```text
invalid signature -> reject
invalid contract -> reject
policy denied -> BLOCKED or ABSTAIN
provider unavailable -> ABSTAIN
no evidence -> ABSTAIN
high risk -> ABSTAIN or NO_TRADE
audit write failure -> fail closed
```

Success:

```text
Future implementation can be split into ports, services, repositories, and
tests without creating runtime NQ integration or real provider access.
```

### K3: Audit / Snapshot / Trace / Replay Plan

Goal:

```text
Plan persistence and read models for auditability and deterministic replay.
```

Planned tables or records:

```text
dh_decision_request
dh_decision_context_snapshot
dh_decision_trace_step
dh_provider_call_log
dh_decision_output
dh_decision_audit_event
replay read model
```

First replay version:

```text
read-model replay only
no real provider rerun
no LLM rerun
no NQ call
no external side effect
```

Success:

```text
Each decision can be reconstructed from stored input, context snapshot, trace,
provider signal, risk review, output, and audit events.
```

### K4: Mock Provider / Provider Health Plan

Goal:

```text
Plan safe provider ports and health metadata without real provider integration.
```

Planned types:

```text
MockDecisionProvider
DisabledDecisionProvider
ProviderTrustPolicy
ProviderBudgetGuard
ProviderTimeoutGuard
ProviderHealth
ProviderLatencySample
ProviderCostSample
ProviderFailureClass
ProviderTrustScore
```

Rules:

```text
default provider is DisabledDecisionProvider or deterministic MockDecisionProvider
real provider is forbidden
provider timeout -> ABSTAIN
provider budget denied -> ABSTAIN
provider trust denied -> ABSTAIN
provider spoofing detected -> fail closed
```

Success:

```text
Provider behavior is testable with mock data and cannot call a real model.
```

### K5: Mock NQ Dry-run Contract Test Plan

Goal:

```text
Plan DH-side mock NQ dry-run contract tests without changing NQ and without
real HTTP.
```

Planned coverage:

```text
DH repository mock NQ request factory
canonical X-NQ-DH-* header family
RFC3339 UTC Z timestamp
HMAC signature
nonce replay protection
tenant / requestId / traceId / decisionId propagation
no-live-trade guarantee
forbidden side-effect checks
```

Rules:

```text
no NQ repository change
no real HTTP
no NQ DB read/write
no Paper Run start
no order / trade / live endpoint
```

Success:

```text
Contract tests can prove signed dry-run request shape and no side effects before
any GateN-based Integration-1 planning.
```

### K6: Golden Cases / Eval Plan

Goal:

```text
Plan golden cases for deterministic policy, provider, risk, and contract
behavior.
```

Required golden cases:

```text
valid_no_trade
policy_blocked
provider_timeout_abstain
high_risk_abstain
no_evidence_abstain
forbidden_action_rejected
invalid_contract_fail_closed
```

Success:

```text
Each golden case has input fixture, expected DecisionOutput, expected audit
event, and forbidden side-effect assertion.
```

### K7: Acceptance / Freeze Plan

Goal:

```text
Plan the acceptance report and freeze decision after the MVP planning and future
WO/implementation batches are reviewed.
```

Planned acceptance artifact:

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
```

Acceptance must decide:

```text
whether GateK plan can close
whether GateK WO can start
whether Decision Pipeline implementation can start
whether Integration-1 dry-run planning can start after GateN rebase
whether any runtime / agent / LangGraph / LIVE capability remains forbidden
```

Success:

```text
No implementation batch can start before WO is accepted.
No Integration-1 runtime can start from this GateK plan.
```

## 4. Target Contract Constraints

### 4.1 Shared Contract Rules

Every first-version contract must include:

```text
schemaVersion
tenantId
requestId
traceId
createdAt
```

Every decision-specific record must include:

```text
decisionId
decisionType
policyStatus
auditStatus
forbiddenActions
```

All public JSON contracts must use:

```text
additionalProperties=false
explicit required fields
explicit enum values
stable error code vocabulary
no database Entity exposure
no raw provider prompt as final output
no credential material
```

### 4.2 DecisionRequest

Purpose:

```text
Capture a signed request for a read-only decision recommendation.
```

Required field groups:

```text
identity: tenantId, requestId, traceId
intent: decisionGoal, decisionScope, requestedDecisionType
context references: contextSnapshotId or inlineContextRef
policy: policyMode, forbiddenActions
evidence references: evidenceRefs
idempotency: idempotencyKey
timing: createdAt, expiresAt
```

Forbidden:

```text
orderId as mutation target
account secret
exchange credential
LIVE flag
BUY / SELL / PLACE_ORDER / CANCEL_ORDER action request
```

### 4.3 DecisionContextSnapshot

Purpose:

```text
Freeze the read-only input context used by a decision.
```

Required field groups:

```text
snapshotId
tenantId
requestId
traceId
sourceRefs
marketContext
strategyContext
riskContext
evidenceSummary
createdAt
contentHash
```

Rules:

```text
snapshot is immutable
snapshot stores original payload references or sanitized payload copies
snapshot does not read NQ DB
snapshot does not include credentials
```

### 4.4 DecisionPolicyResult

Purpose:

```text
Record policy evaluation before provider or risk output is trusted.
```

Required field groups:

```text
policyResultId
decisionId
status: ALLOWED | BLOCKED | REVIEW_REQUIRED
reasonCode
deniedActions
matchedRules
createdAt
```

Rules:

```text
policy denied -> BLOCKED or ABSTAIN
policy evaluation error -> fail closed
```

### 4.5 ProviderDecisionSignal

Purpose:

```text
Represent a mock or disabled provider signal without letting it become a final
decision.
```

Required field groups:

```text
providerSignalId
decisionId
providerId
providerMode: MOCK | DISABLED
signalAction
confidence
evidenceRefs
latencyMs
costUnits
failureClass
createdAt
```

Rules:

```text
real provider mode is forbidden in GateK MVP
provider failure -> ABSTAIN
provider signal never bypasses policy or risk review
```

### 4.6 RiskReview

Purpose:

```text
Assess risk before producing DecisionOutput.
```

Required field groups:

```text
riskReviewId
decisionId
riskLevel: LOW | MEDIUM | HIGH | CRITICAL
riskReasons
blockedActions
reviewerMode: RULE | MOCK
createdAt
```

Rules:

```text
HIGH or CRITICAL risk -> no LONG_BIAS / SHORT_BIAS
risk review failure -> ABSTAIN
```

### 4.7 DecisionOutput

First version must satisfy:

```text
decisionType = READ_ONLY_RECOMMENDATION
action in ABSTAIN | OBSERVE | NO_TRADE | LONG_BIAS | SHORT_BIAS
default action = ABSTAIN
no evidence = ABSTAIN
provider failure = ABSTAIN
policy denied = BLOCKED or ABSTAIN
risk high = no LONG_BIAS / SHORT_BIAS
forbiddenActions includes PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
structured JSON only
no free-text final output
no real trading instruction
```

Forbidden action enum values:

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
```

Required field groups:

```text
decisionId
decisionType
action
status: ALLOWED | BLOCKED | ABSTAINED
reasonCode
evidenceSummary
riskReview
policyResult
providerSignals
forbiddenActions
traceId
requestId
tenantId
createdAt
```

### 4.8 DecisionAuditEvent

Purpose:

```text
Record every security, policy, provider, risk, output, and replay decision.
```

Required field groups:

```text
auditEventId
decisionId
tenantId
requestId
traceId
eventType
actorType
result
reasonCode
payloadHash
createdAt
```

Rules:

```text
audit event must not store credential material
audit write failure fails closed for the first MVP unless future accepted design
explicitly makes it asynchronous and traceable
```

### 4.9 DecisionReplayRecord

Purpose:

```text
Provide deterministic read-model replay for the stored decision.
```

Required field groups:

```text
replayId
decisionId
sourceSnapshotId
traceStepIds
outputId
auditEventIds
contentHash
createdAt
```

Rules:

```text
replay reads stored data only
replay does not rerun provider
replay does not call NQ
replay does not mutate state
```

## 5. Module Planning

Decision Pipeline MVP stays a Java 21 / Spring Boot 3.5.x modular monolith. It
does not default to microservices or Spring Cloud.

| Module / boundary | Responsibility | Recommended types | Input / output | Dependency direction | Forbidden dependency | Test focus |
| --- | --- | --- | --- | --- | --- | --- |
| `dh-api` | Future signed request entry and response mapping, after WO approval | `DecisionController` only if future WO authorizes API, request/response mappers | Signed JSON in, `DecisionOutput` out | depends on `dh-usecase`, `dh-security` | NQ client, provider SDK, DB entity exposure | auth, validation, error mapping, no dangerous path |
| `dh-domain` | Decision value objects and enums | `DecisionRequest`, `DecisionOutput`, `RiskReview`, `DecisionAction`, `DecisionType` | immutable domain objects | no infra dependency | Spring web, JDBC, provider SDK, NQ runtime | enum constraints, fail-closed factories |
| `dh-usecase` | Orchestration ports and application service | `DecisionOrchestrator`, `DecisionPolicyService`, `DecisionRiskReviewService` | domain command in, domain result out | depends on domain ports | controller, JDBC implementation, real provider | linear flow, policy denied, provider timeout, audit failure |
| `dh-security` | Signing, replay, tenant binding, policy gates | `DecisionRequestAuthenticator`, `DecisionPolicyGuard`, `DecisionReplayGuard` | headers/body in, auth/policy result out | no provider dependency | provider SDK, NQ runtime, secret logging | HMAC, nonce, timestamp, tenant escape, fail-closed |
| `dh-policy` | Logical policy boundary, package under usecase/security unless future module accepted | `ProviderTrustPolicy`, `ForbiddenActionPolicy`, `AutonomyPolicy` | decision context in, policy result out | domain/security only | connector real clients, DB mutation | forbidden action rejected, unsafe autonomy blocked |
| `dh-provider` / `dh-connector` | Mock and disabled decision provider ports | `DecisionProvider`, `MockDecisionProvider`, `DisabledDecisionProvider` | normalized context in, provider signal out | connector depends on domain ports | real LLM SDK, HTTP client, NQ client | disabled default, timeout abstain, spoofing fail-closed |
| `dh-audit` | Logical audit boundary, package under usecase/infra until module accepted | `DecisionAuditPort`, `DecisionAuditEventWriter` | audit event in, write result out | usecase port, infra adapter | provider or NQ mutation | audit write failure, event shape, no secrets |
| `dh-infra` | Future persistence adapters after migration approval | `JdbcDecisionSnapshotRepository`, `JdbcDecisionTraceRepository` only after DB WO | repository port in/out | implements usecase ports | domain depending on infra | SQL shape, transaction boundary, no N+1, pagination |
| `dh-observability` | Metrics and trace naming | `DecisionPipelineMetrics`, `ProviderHealthMetrics` | counters/timers | shared utility | business decisions | latency/cost/failure metrics with no secrets |
| `dh-contract` | Logical contract artifacts under `contracts/**` only if future WO authorizes schema changes | JSON Schema files | contract fixtures | read by tests | runtime mutation | schema required/additionalProperties/enum checks |
| `dh-app` | Future wiring, profiles, conservative defaults | `DecisionPipelineWiringConfig` | Spring beans | depends on all implementation modules | real provider enabled by default | disabled startup, conditional beans, no RealClient |

Notes:

- Existing `dh-providers` is a historical / legacy module name in this repository. GateK MVP should not use it to introduce a real provider.
- `dh-policy`, `dh-audit`, and `dh-contract` are planning boundaries. Creating new Maven modules requires a later accepted WO.
- Any future DB table requires a migration review task before implementation.

## 6. Security Boundary Matrix

| Risk | Level | Trigger scenario | Impact | Defense design | Acceptance test |
| --- | --- | --- | --- | --- | --- |
| prompt injection | P1 | evidence text asks DH to ignore policy | unsafe recommendation | evidence treated as untrusted data, policy before provider output trust | malicious evidence -> ABSTAIN / BLOCKED |
| tool injection | P1 | provider output requests tool call or NQ mutation | side-effect attempt | no tool execution in GateK; forbiddenActions always enforced | provider signal with PLACE_ORDER -> reject |
| provider spoofing | P1 | fake provider id claims trusted real provider | trust bypass | provider registry allowlist, mode MOCK/DISABLED only | unknown provider -> fail closed |
| replay attack | P1 | duplicate signed request or nonce reuse | duplicate decision/audit confusion | nonce replay guard + idempotency key + audit event | same nonce -> reject / duplicate contract behavior |
| timestamp bypass | P1 | old/future timestamp or non-UTC format | replay window bypass | RFC3339 UTC Z + bounded window | epoch / offset / expired timestamp -> reject |
| tenant escape | P0 | tenant in header/body mismatches auth context | cross-tenant data exposure | authenticated tenant is authority; mismatch fail-closed | tenant mismatch -> reject |
| source forgery | P1 | untrusted source sends signed-like request | untrusted input accepted | source allowlist + HMAC + audit | unknown source -> reject |
| model hallucination | P1 | provider fabricates evidence or action | unsafe output | evidence refs required, no evidence -> ABSTAIN | missing evidence -> ABSTAIN |
| unsafe autonomous trading | P0 | output action becomes trading instruction | real trading risk | action enum excludes trading; forbiddenActions fixed | BUY / SELL / PLACE_ORDER / CANCEL_ORDER rejected |
| over-permission tool call | P1 | pipeline tries MCP/write/NQ tool | unauthorized side effect | no tool runtime in GateK; static forbidden scope | tool-call field -> fail closed |
| unbounded memory/context growth | P2 | large evidence/context snapshot | memory exhaustion | payload cap, evidence ref limits, page/read limits | oversize context -> reject |
| cost explosion | P2 | provider loop or excessive calls | budget overrun | provider budget guard and max call count | budget exceeded -> ABSTAIN |
| latency spike | P2 | provider stalls or downstream slow | request saturation | timeout guard, no infinite retry | timeout -> ABSTAIN with audit |
| provider fallback failure | P1 | primary and fallback unavailable | false confidence | disabled/mock fallback produces ABSTAIN only | fallback failure -> ABSTAIN |
| audit write failure | P1 | audit sink unavailable | unreplayable decision | first MVP fail-closed on audit write failure | audit writer error -> no DecisionOutput success |
| replay data tampering | P1 | stored trace/snapshot hash mismatch | false replay result | contentHash and immutable read model | hash mismatch -> replay invalid |

## 7. Golden Cases / Eval Baseline

| Case | Input | Expected output | Expected audit | Forbidden side effect |
| --- | --- | --- | --- | --- |
| `valid_no_trade` | valid request, sufficient evidence, low risk, no trade need | `NO_TRADE` | policy allowed + risk low + output created | no NQ call, no order |
| `policy_blocked` | request asks forbidden action | `BLOCKED` or `ABSTAIN` | policy denied | no provider call required, no order |
| `provider_timeout_abstain` | provider exceeds timeout | `ABSTAIN` | provider timeout + output abstain | no retry loop |
| `high_risk_abstain` | risk level HIGH | `ABSTAIN` or `NO_TRADE` | risk high | no LONG_BIAS / SHORT_BIAS |
| `no_evidence_abstain` | evidenceRefs empty or invalid | `ABSTAIN` | evidence insufficient | no provider trust |
| `forbidden_action_rejected` | action BUY / SELL / PLACE_ORDER / CANCEL_ORDER | contract reject | contract violation | no audit success event as allowed |
| `invalid_contract_fail_closed` | missing required fields or additionalProperties | reject | validation failure | no provider call |

## 8. Acceptance / Freeze Plan

Future acceptance report:

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
```

Future acceptance checks:

```text
contracts frozen and reviewed
WO accepted before implementation
no Java/test/API/migration changes in plan-only stage
no real provider
no real HTTP
no NQ runtime integration
no NQ mutation
no LIVE
golden cases defined
audit/replay model reviewed
security matrix covered
readiness decisions recorded
```

Future freeze may copy `docs/current` to a gate snapshot only after explicit
freeze/archive authorization.

## 9. Readiness Decision

```text
ALLOW_GATEK_PLAN_CLOSE: YES
ALLOW_GATEK_WO: YES
ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

Interpretation:

- `ALLOW_GATEK_PLAN_CLOSE: YES` means this planning document can be reviewed as
  the GateK Plan close candidate.
- `ALLOW_GATEK_WO: YES` means the next documentation task may produce
  `DH-GATEK-DECISION-PIPELINE-MVP-WO`.
- `ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO` means no implementation may start
  until the WO is written, reviewed, and accepted.
- `ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES` means a separate GateN-based
  planning-only dry-run Integration-1 document may be prepared later.
- All runtime, agent, LangGraph, and LIVE decisions remain `NO`.

## 10. Validation Commands

This docs-only plan should be validated with:

```powershell
git status --short
git diff --check
git diff --stat
mvn test
mvn -Pquality validate
```

If Maven is blocked by local repository or network dependency download issues,
record the exact failure and do not claim test success.

## 11. Rollback

Rollback is file-level:

```powershell
git restore --worktree -- docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md docs/current/README.md docs/current/STATUS.md docs/current/ROADMAP.md docs/current/WORK_ORDER.md docs/current/API.md docs/current/TESTING.md docs/current/WORKLOG.md
```

This command must only be run after confirming no user changes were added to the
same files after this plan update.
