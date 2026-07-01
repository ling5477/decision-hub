# DH GateK Decision Pipeline MVP Work Order

> Task: DH-GATEK-DECISION-PIPELINE-MVP-WO
> Status: WORK ORDER / READY FOR REVIEW
> Source plan: `docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`
> Date: 2026-07-01
> Scope: docs-only / work-order-only in this turn

## 1. Gate Lock

This work order is the execution plan for GateK Decision Pipeline MVP. It does
not authorize implementation without batch-level review.

Current accepted facts:

```text
DH-GATEK-DECISION-PIPELINE-MVP-PLAN: ACCEPTED / CLOSED
Integration-0 safety gate: CLOSED / ACCEPTED
NQ integration: not started
Integration-1: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline: GateN
```

This work order allows future implementation only as small reviewed batches:

```text
K1 -> review -> K2 -> review -> K3 -> review -> K4
K1-K5 complete before K6
K1-K7 complete before K8
```

Before GateK MVP is closed, these remain forbidden:

```text
Integration-1 runtime
LangGraph runtime
AI / LLM runtime
real provider
real NQ runtime
real HTTP to NQ
RealClient / RealNqBacktestClient
NQ repository changes
NQ DB read/write
NQ state mutation
Paper Run startup
LIVE trading
```

## 2. Shared DecisionOutput Contract

All GateK batches must preserve this output contract.

```text
decisionType = READ_ONLY_RECOMMENDATION
action vocabulary = ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
default action = ABSTAIN
no evidence -> ABSTAIN
provider failure -> ABSTAIN
policy denied -> BLOCKED or ABSTAIN, fail-closed
high risk -> LONG_BIAS / SHORT_BIAS forbidden
final output = structured JSON only
free-text final output = forbidden
real trading instruction = forbidden
```

`forbiddenActions` must always include:

```text
PLACE_ORDER
CANCEL_ORDER
MUTATE_NQ_STATE
READ_NQ_DB
WRITE_NQ_DB
```

The output must not contain:

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
market order instructions
limit order instructions
exchange credentials
raw provider secrets
direct NQ mutation instructions
```

## 3. Batch Index

| Batch | Name | Purpose | Dependency |
| --- | --- | --- | --- |
| K1 | Decision Contract Freeze | Freeze request/output/action/audit contract | PLAN accepted |
| K2 | DecisionOrchestrator Skeleton | Implement fail-closed orchestration skeleton | K1 reviewed |
| K3 | Audit / Snapshot / Trace Persistence | Persist request, context, trace, output, audit | K2 reviewed |
| K4 | Replay Read Model | Read persisted decision records without rerun | K3 reviewed |
| K5 | Mock Provider / Provider Health / Budget / Latency | Add mock-only provider controls | K4 reviewed |
| K6 | Mock NQ Dry-run Contract Tests | Test boundary against mock NQ fixtures only | K1-K5 complete |
| K7 | Golden Cases / Eval Baseline | Lock deterministic eval baseline | K6 reviewed |
| K8 | Acceptance / Freeze | Close GateK MVP and prepare freeze | K1-K7 complete |

## 4. K1 Decision Contract Freeze

Batch name: `K1 Decision Contract Freeze`

Objective:

```text
Freeze the domain contract for read-only decision input, evidence, policy,
risk, action vocabulary, forbidden actions, output JSON, trace shape and audit
event shape before any orchestrator implementation.
```

Allowed files:

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/**
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/**Decision*ContractTest.java
contracts/json-schema/decision-*.schema.json
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
dh-api/**
dh-app/src/main/resources/db/migration/**
dh-infra/**
dh-connector/** real provider / HTTP client files
NQ repository
production .env or credentials
```

Production changes allowed? YES, domain contract only.

Test changes allowed? YES.

Migration allowed? NO.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionRequest
DecisionContextSnapshot
DecisionEvidence
DecisionPolicyResult
ProviderDecisionSignal
DecisionRiskReview
DecisionOutput
DecisionAuditEvent
DecisionTraceStep
DecisionAction
DecisionType
DecisionStatus
ForbiddenDecisionAction
```

Main schemas/tables if any:

```text
contracts/json-schema/decision-request.schema.json
contracts/json-schema/decision-output.schema.json
contracts/json-schema/decision-audit-event.schema.json
No database tables in K1.
```

Required tests:

```text
DecisionOutputContractTest
DecisionActionVocabularyContractTest
DecisionForbiddenActionsContractTest
DecisionNoEvidenceAbstainContractTest
DecisionHighRiskBlocksDirectionalBiasTest
DecisionJsonSchemaContractTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
No Controller / REST API.
No migration.
No provider runtime.
No NQ runtime.
No AI / LLM runtime.
No LangGraph runtime.
No LIVE.
```

Exit criteria:

```text
DecisionOutput READ_ONLY_RECOMMENDATION is contract-tested.
ABSTAIN default is contract-tested.
No evidence and provider failure map to ABSTAIN.
Policy denied maps to BLOCKED or ABSTAIN fail-closed.
High risk forbids LONG_BIAS / SHORT_BIAS.
forbiddenActions include all five mandatory forbidden actions.
Schemas and Java contract agree.
mvn test and mvn -Pquality validate pass.
```

Rollback approach:

```text
Revert K1 domain decision package, decision JSON schemas, K1 tests and docs
status entries. No data migration rollback is needed.
```

Next batch:

```text
K2 DecisionOrchestrator Skeleton after K1 review.
```

## 5. K2 DecisionOrchestrator Skeleton

Batch name: `K2 DecisionOrchestrator Skeleton`

Objective:

```text
Create the usecase-layer orchestration skeleton that accepts a frozen
DecisionRequest contract, evaluates evidence/policy/risk/provider signals, and
returns only fail-closed read-only DecisionOutput. The skeleton must not call
real providers or NQ.
```

Allowed files:

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/**
dh-connector/src/main/java/com/guidinglight/decisionhub/connector/decision/**
dh-connector/src/test/java/com/guidinglight/decisionhub/connector/decision/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
dh-api/**
dh-infra/**
dh-app/src/main/resources/db/migration/**
real provider adapters
HTTP clients to NQ
LangGraph / LLM runtime wiring
NQ repository
```

Production changes allowed? YES, usecase skeleton and mock/disabled ports only.

Test changes allowed? YES.

Migration allowed? NO.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionOrchestrator
DefaultDecisionOrchestrator
DecisionEvidenceCollector
DecisionPolicyService
DecisionRiskReviewService
DecisionNormalizer
DecisionAuditPort
DecisionProviderPort
MockDecisionProvider
DisabledDecisionProvider
DecisionProviderResult
```

Main schemas/tables if any:

```text
No new schemas.
No database tables in K2.
```

Required tests:

```text
DecisionOrchestratorDefaultAbstainTest
DecisionOrchestratorProviderFailureTest
DecisionOrchestratorPolicyDeniedTest
DecisionOrchestratorHighRiskTest
DecisionOrchestratorNoEvidenceTest
DisabledDecisionProviderTest
NoRealProviderBoundaryTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
No REST endpoint.
No persistence.
No real provider.
No real HTTP.
No NQ call.
No AI / LLM runtime.
No LangGraph runtime.
No LIVE.
```

Exit criteria:

```text
Orchestrator returns ABSTAIN for empty evidence.
Orchestrator returns ABSTAIN for provider failure.
Policy denied is fail-closed.
High risk blocks LONG_BIAS / SHORT_BIAS.
Mock and disabled providers are deterministic.
No class named RealDecisionProvider or RealClient is introduced.
```

Rollback approach:

```text
Revert K2 usecase decision package, connector decision mock/disabled package,
K2 tests and docs status entries. K1 frozen contracts remain valid unless the
K2 review finds a contract defect.
```

Next batch:

```text
K3 Audit / Snapshot / Trace Persistence after K2 review.
```

## 6. K3 Audit / Snapshot / Trace Persistence

Batch name: `K3 Audit / Snapshot / Trace Persistence`

Objective:

```text
Persist decision request, context snapshot, trace steps, provider call summary,
output and audit events so every recommendation is replayable and auditable.
Audit write failure must fail-closed and must not produce a success output.
```

Allowed files:

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/**
dh-app/src/main/resources/db/migration/V{next}__dh_decision_pipeline_audit.sql
dh-app/src/test/java/com/guidinglight/decisionhub/**/decision/**
docs/current/DB_SCHEMA.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
dh-api/**
real provider adapters
real NQ client
NQ repository
production database access
credentials
```

Production changes allowed? YES.

Test changes allowed? YES.

Migration allowed? YES, only after DB review and only for DH-owned audit tables.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionAuditRepository
DecisionSnapshotRepository
DecisionTraceRepository
DecisionReplayRecordRepository
JdbcDecisionAuditRepository
JdbcDecisionSnapshotRepository
JdbcDecisionTraceRepository
DecisionAuditPersistenceService
```

Main schemas/tables if any:

```text
dh_decision_request
dh_decision_context_snapshot
dh_decision_trace_step
dh_provider_call_log
dh_decision_output
dh_decision_audit_event
```

Implementation note:

```text
The actual Flyway version must be checked immediately before K3 starts.
If V4 is still the latest migration, use V5. If a newer migration exists, use
the next available version. Do not reuse an existing migration version.
```

Required tests:

```text
DecisionAuditMigrationContractTest
JdbcDecisionAuditRepositoryTest
JdbcDecisionSnapshotRepositoryTest
JdbcDecisionTraceRepositoryTest
DecisionAuditWriteFailureFailClosedTest
DecisionAuditNoSecretPersistenceTest
DecisionPersistenceTransactionBoundaryTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
DH-owned persistence only.
No NQ DB access.
No provider secret persistence.
No API endpoint.
No Integration-1 runtime.
No LIVE.
```

Exit criteria:

```text
All audit/snapshot/trace tables have primary keys, tenant/request/trace indexes,
created_at timestamps and safe JSON payload boundaries.
Repository tests cover success, missing record and duplicate/idempotent paths.
Audit write failure prevents success DecisionOutput.
No secret/token/key/cookie fields are persisted or logged.
```

Rollback approach:

```text
Revert K3 Java repositories/services/tests/docs. For a local development DB,
drop only K3-created DH audit tables if they were applied locally. Production
database migration is not authorized by this work order.
```

Next batch:

```text
K4 Replay Read Model after K3 review.
```

## 7. K4 Replay Read Model

Batch name: `K4 Replay Read Model`

Objective:

```text
Create a read model that reconstructs persisted decision records for audit and
review without rerunning providers, AI, NQ or policy side effects.
```

Allowed files:

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/replay/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
dh-api/**
provider runtime
NQ runtime
LangGraph runtime
LLM runtime
new migration unless K3 review explicitly requires a schema correction
```

Production changes allowed? YES.

Test changes allowed? YES.

Migration allowed? NO by default.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionReplayReadModel
DecisionReplayQuery
DecisionReplayView
DecisionReplayTraceView
JdbcDecisionReplayReadModel
```

Main schemas/tables if any:

```text
Reuses K3 tables only.
No new table in K4 by default.
```

Required tests:

```text
DecisionReplayReadModelTest
DecisionReplayDoesNotRerunProviderTest
DecisionReplayTenantBoundaryTest
DecisionReplayMissingRecordTest
DecisionReplayTraceOrderingTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
Replay is read-only.
Replay must not call provider.
Replay must not call NQ.
Replay must not evaluate new policy.
Replay must not produce a new trading suggestion.
```

Exit criteria:

```text
Replay returns stored request/snapshot/trace/output/audit views only.
Tenant boundary is tested.
Missing records return explicit not-found semantics.
Provider and NQ ports are not reachable from replay package.
```

Rollback approach:

```text
Revert K4 replay package, read model implementation, K4 tests and docs entries.
K3 persisted audit schema remains unless K3 rollback is separately requested.
```

Next batch:

```text
K5 Mock Provider / Provider Health / Budget / Latency after K4 review.
```

## 8. K5 Mock Provider / Provider Health / Budget / Latency

Batch name: `K5 Mock Provider / Provider Health / Budget / Latency`

Objective:

```text
Add mock-only provider health, budget and latency controls so provider failure,
timeout, untrusted state and budget exhaustion all fail closed into ABSTAIN or
BLOCKED without enabling real provider runtime.
```

Allowed files:

```text
dh-security/src/main/java/com/guidinglight/decisionhub/security/provider/**
dh-security/src/test/java/com/guidinglight/decisionhub/security/provider/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/provider/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/provider/**
dh-connector/src/main/java/com/guidinglight/decisionhub/connector/decision/provider/**
dh-connector/src/test/java/com/guidinglight/decisionhub/connector/decision/provider/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
real provider adapters
OpenAI / Claude / Gemini / local model runtime wiring
LangGraph runtime
NQ runtime
dh-api/**
migration files, unless a later reviewed K5-DB sub-batch is opened
```

Production changes allowed? YES, mock/disabled control logic only.

Test changes allowed? YES.

Migration allowed? NO in K5 base batch.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionProviderHealth
DecisionProviderHealthStatus
DecisionProviderBudget
DecisionProviderLatencyPolicy
DecisionProviderSafetyGate
MockDecisionProviderHealthStore
DecisionProviderFailureReason
```

Main schemas/tables if any:

```text
No table in K5 base batch.
Deferred table names, if later approved in a separate DB batch:
dh_provider_health
dh_provider_budget_event
dh_model_call_summary
```

Required tests:

```text
DecisionProviderHealthGateTest
DecisionProviderBudgetGateTest
DecisionProviderLatencyPolicyTest
DecisionProviderUntrustedFailClosedTest
DecisionProviderTimeoutAbstainTest
NoRealProviderRuntimeTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
Mock-only provider behavior.
No external model call.
No provider credential.
No runtime agent phase.
No LangGraph runtime.
No Integration-1 runtime.
```

Exit criteria:

```text
Provider unavailable -> ABSTAIN.
Provider timeout -> ABSTAIN.
Provider untrusted -> BLOCKED or ABSTAIN.
Budget exceeded -> fail-closed.
Latency policy breach -> fail-closed.
No real provider class or dependency is introduced.
```

Rollback approach:

```text
Revert K5 provider health/budget/latency control classes, mock provider changes,
K5 tests and docs entries. No migration rollback is needed.
```

Next batch:

```text
K6 Mock NQ Dry-run Contract Tests after K1-K5 complete.
```

## 9. K6 Mock NQ Dry-run Contract Tests

Batch name: `K6 Mock NQ Dry-run Contract Tests`

Objective:

```text
Prove that GateK DecisionOutput can be checked against mock NQ dry-run fixtures
without invoking NQ runtime, NQ DB, NQ HTTP, Paper Run, orders or state mutation.
```

Allowed files:

```text
dh-domain/src/test/java/com/guidinglight/decisionhub/gatek/mocknq/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/gatek/mocknq/**
golden_cases/decision-pipeline/mock-nq/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
production Java code
dh-api/**
migration files
real NQ client
NQ repository
NQ DB
real HTTP
Paper Run startup
```

Production changes allowed? NO.

Test changes allowed? YES.

Migration allowed? NO.

API changes allowed? NO.

Main classes/interfaces:

```text
MockNqDecisionRequestFactory
MockNqDecisionContractFixture
GateKDecisionDryRunContractTest
GateKDecisionNoSideEffectTest
GateKDecisionHeaderBoundaryTest
```

Main schemas/tables if any:

```text
Reuses decision JSON schemas.
No database tables in K6.
```

Required tests:

```text
GateKDecisionDryRunContractTest
GateKDecisionNoSideEffectTest
GateKDecisionForbiddenActionContractTest
GateKDecisionMockNqPayloadShapeTest
GateKDecisionNoNqRuntimeTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
Mock NQ only.
No NQ runtime.
No NQ DB.
No NQ HTTP.
No Paper Run.
No order state.
No account or credential access.
```

Exit criteria:

```text
Dry-run tests validate JSON shape and forbidden actions.
Side-effect tracker remains empty.
No production class is changed.
No NQ package, URL or runtime dependency is introduced.
```

Rollback approach:

```text
Remove K6 test fixtures, golden mock NQ fixtures and docs status entries.
No production rollback is needed.
```

Next batch:

```text
K7 Golden Cases / Eval Baseline after K6 review.
```

## 10. K7 Golden Cases / Eval Baseline

Batch name: `K7 Golden Cases / Eval Baseline`

Objective:

```text
Create deterministic golden cases that lock expected read-only recommendation
behavior across empty evidence, conflicting evidence, high risk, provider
failure, policy denied and audit failure paths.
```

Allowed files:

```text
golden_cases/decision-pipeline/**
dh-eval/src/test/java/com/guidinglight/decisionhub/eval/decision/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

Forbidden files:

```text
production provider runtime
real NQ client
dh-api/**
migration files
NQ repository
LIVE configuration
```

Production changes allowed? NO by default.

Test changes allowed? YES.

Migration allowed? NO.

API changes allowed? NO.

Main classes/interfaces:

```text
DecisionGoldenCase
DecisionGoldenCaseLoader
DecisionGoldenCaseEvaluatorTest
DecisionGoldenCaseBaselineTest
```

Main schemas/tables if any:

```text
golden_cases/decision-pipeline/*.json
No database tables in K7.
```

Required tests:

```text
DecisionGoldenCaseBaselineTest
DecisionGoldenCaseEmptyEvidenceTest
DecisionGoldenCaseProviderFailureTest
DecisionGoldenCasePolicyDeniedTest
DecisionGoldenCaseHighRiskTest
DecisionGoldenCaseNoSecretFixtureTest
```

Validation commands:

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
Golden cases are deterministic fixtures.
No real provider replay.
No NQ runtime replay.
No live market data dependency.
No secret fixture.
```

Exit criteria:

```text
Golden cases cover normal, empty, conflicting, high-risk, provider-failure,
policy-denied and audit-failure paths.
Expected outputs are READ_ONLY_RECOMMENDATION.
All forbidden actions remain present.
Directional bias is absent from high-risk outputs.
```

Rollback approach:

```text
Remove K7 golden case fixtures, eval tests and docs entries. No production
rollback is needed unless a reviewed K7 sub-batch explicitly changed eval
production helpers.
```

Next batch:

```text
K8 Acceptance / Freeze after K1-K7 complete.
```

## 11. K8 Acceptance / Freeze

Batch name: `K8 Acceptance / Freeze`

Objective:

```text
Review K1-K7 evidence, close GateK Decision Pipeline MVP only if contracts,
tests, audit, replay, mock provider controls and golden cases all pass, then
prepare the docs/current freeze package.
```

Allowed files:

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/gates/dh-gatek-decision-pipeline-mvp/** only after explicit freeze/archive authorization
```

Forbidden files:

```text
production Java code
test Java code
contracts/**
golden_cases/**
migration files
API files
NQ repository
```

Production changes allowed? NO.

Test changes allowed? NO.

Migration allowed? NO.

API changes allowed? NO.

Main classes/interfaces:

```text
None in K8.
```

Main schemas/tables if any:

```text
No schema or table changes in K8.
```

Required tests:

```text
No new tests in K8.
K8 must cite the last passing K1-K7 validation evidence.
```

Validation commands:

```powershell
git status --short
git diff --check
git diff --stat
mvn test
mvn -Pquality validate
```

Boundary confirmation:

```text
Acceptance is docs-only.
No new implementation.
No Integration-1 runtime.
No Agent phase.
No LangGraph runtime.
No LIVE.
```

Exit criteria:

```text
K1-K7 are reviewed and accepted.
TESTING.md contains passing mvn test and mvn -Pquality validate evidence.
STATUS.md declares GateK MVP closed only after evidence exists.
WORKLOG.md records exact batch completion evidence.
Freeze snapshot is created only if explicitly authorized.
```

Rollback approach:

```text
Revert K8 docs changes and remove the freeze snapshot only if the snapshot was
created during K8 and user explicitly authorizes removal. Do not revert K1-K7
implementation from K8.
```

Next batch:

```text
GateL or later planning. LangGraph runtime is GateL or later, not GateK MVP.
Integration-1 runtime remains blocked until a GateN-rebased planning package is
reviewed and accepted.
```

## 12. Cross-Batch Acceptance Matrix

| Requirement | K1 | K2 | K3 | K4 | K5 | K6 | K7 | K8 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| READ_ONLY_RECOMMENDATION only | required | required | required | required | required | required | required | reviewed |
| ABSTAIN default | required | required | required | required | required | required | required | reviewed |
| No evidence -> ABSTAIN | required | required | persisted | replayed | required | tested | golden | reviewed |
| Provider failure -> ABSTAIN | contract | required | persisted | replayed | required | tested | golden | reviewed |
| Policy denied fail-closed | contract | required | persisted | replayed | required | tested | golden | reviewed |
| Audit write failure fail-closed | contract | required | required | replayed | required | tested | golden | reviewed |
| High risk forbids directional bias | required | required | persisted | replayed | required | tested | golden | reviewed |
| forbiddenActions complete | required | required | persisted | replayed | required | tested | golden | reviewed |
| No real NQ runtime | required | required | required | required | required | required | required | reviewed |
| No LIVE | required | required | required | required | required | required | required | reviewed |

## 13. Readiness Decision

```text
ALLOW_WO_CLOSE: YES
ALLOW_K1_IMPLEMENTATION: YES
ALLOW_FULL_GATEK_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

Interpretation:

```text
ALLOW_K1_IMPLEMENTATION: YES means only K1 can start after this WO is accepted.
It does not authorize K2-K8 without review.
It does not authorize Integration-1 runtime, Agent phase, LangGraph runtime or LIVE.
```

## 14. Next Codex Prompt

```text
你在 F:\project\decision-hub 仓库 dev 分支上工作。

任务名：DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE。

目标：仅执行 K1 Decision Contract Freeze，冻结 DecisionRequest /
DecisionOutput / DecisionAction / forbiddenActions / DecisionAuditEvent /
DecisionTraceStep 的 domain contract 与 JSON schema，并补齐 K1 contract tests。

必须遵守：
- 只允许 K1 文件范围。
- 不实现 DecisionOrchestrator。
- 不新增 API。
- 不新增 migration。
- 不接真实 provider。
- 不接真实 NQ。
- 不接 AI / LLM / LangGraph runtime。
- 不触碰 LIVE。

验收命令：
- git status --short
- git diff --check
- mvn test
- mvn -Pquality validate
```
