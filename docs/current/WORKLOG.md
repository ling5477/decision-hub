# Decision Hub Worklog

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 2026-07-09 DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION

完成 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation implementation。本轮只实现内部 readiness policy evaluation、trust gate、redaction guard、fail-closed rules 和 B3 tests；未新增 API、migration、production repository/JDBC、真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph、NQ 或 LIVE。

### Scope

```text
IMPLEMENTATION
PROVIDER_READINESS_GUARD
POLICY_EVALUATION
TRUST_SECURITY_BOUNDARY
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessPolicy.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuard.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessEvaluationResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessDecision.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessDecisionReason.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuardService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessGuardServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation Result

```text
structures: ProviderReadinessPolicy / ProviderReadinessGuard / EvaluationCommand / EvaluationResult / Decision / DecisionReason / GuardService
policy input boundary: tenantId / sourceRef / providerRef / modelGatewayVersionRef / providerSummaryHash / B1-B2 safe summary-view / policyVersion / trace refs / timestamps only
decision boundary: READY / NOT_READY / DEGRADED / SKIPPED only
fail-closed rules: missing tenant/provider/policy/source denied/policy denied/timeout/budget/unknown/sensitive/raw/trading/NQ mutation/internal exception covered
redaction guard: fixed reason enum retained, safe finding code avoids raw marker echo
authorization/live/trading-signal guard: READY exposes no provider enable flag, HTTP flag, LIVE flag or trading permission
docs sync: B3 DONE, B4 NOT_STARTED, next action close review
```

### Validation Snapshot

```text
targeted B3 test first run: TEST_FIX_REQUIRED / 19 tests / 2 errors
targeted B3 test final: BUILD SUCCESS / 19 tests
dh-domain,dh-usecase scoped regression: BUILD SUCCESS / dh-domain 151 / dh-connector 19 / dh-usecase 405
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
safety wording scan: REVIEWED / GUARD_AND_EXISTING_DOC_HITS
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 NQ
未新增 migration
未新增 API / Controller
未新增 production Repository / JDBC / persistence adapter
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
B4 未启动
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-CLOSE-REVIEW
```

## 2026-07-09 DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO

完成 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation implementation work order。本轮只编制后续 readiness decision、trust gate、fail-closed classification 的实现边界、policy input boundary、decision boundary、fail-closed 规则、security boundary review 触发规则和测试矩阵。未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
B3_SECURITY_BOUNDARY_DESIGN
PROVIDER_READINESS_GUARD_WO
POLICY_EVALUATION_WO
TRUST_DECISION_REVIEW_PREP
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
provider readiness guard target: future-readiness condition evidence only
allowed structures: ProviderReadinessPolicy / Guard / EvaluationCommand / EvaluationResult / Decision / DecisionReason / Finding / GuardService
policy input boundary: tenant/source/provider/gateway/read-model safe view/policyVersion/trace refs only
decision boundary: READY / NOT_READY / DEGRADED / SKIPPED only
fail-closed boundary: missing tenant/provider/policy/source denial/policy denial/timeout/budget/unknown/raw material/trading term fail-closed
security boundary: SECURITY_BOUNDARY_REVIEW_REQUIRED: YES
review triggers: B3 implementation -> security boundary / close review -> B4 only after review PASS
next action: DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION
```

### Validation Snapshot

```text
preflight: PASS / dev / worktree clean / B2 commit present
safety wording scan: REVIEWED / EXISTING_FALSE_POSITIVES_ONLY
forbidden-scope diff: PASS / EMPTY
quality validate: BUILD SUCCESS / Reactor 19/19 / Checkstyle 0 / Spotless passed
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未进入 B3 implementation
未进入 B4
```

## 2026-07-09 DH-STAGE-QDR-5-B2-CI-BLOCKER-FIX

完成 Stage-QDR-5 B2 CI blocker fix。本轮只修复 B1/B2 observability guard 触发既有 `ArchitectureTest` source scan 的问题；未扩展 B2 功能，未修改 CI 配置、migration、API、Controller、production repository/JDBC、NQ、provider、HTTP、Provider SDK、Agent、LangGraph 或 LIVE。

### CI Evidence

```text
CI provider: GitHub Actions
failed run: 28998957967 / CI / dev / docs(qdr): define provider health read model work order
failed log access: GH_CLI_UNAVAILABLE_OR_UNAUTHENTICATED / HTTP 403 API rate limit exceeded / gh auth status token invalid
local equivalent failed command: mvn -gs target/codex-maven-settings.xml -B -ntp test
failed job: build & test (Testcontainers / Docker) equivalent local reproduction
failed step: Build and test equivalent local reproduction
failed command: mvn -B -ntp test
failure excerpt: ArchitectureTest stageQdr3B3_rule34 and stageQdr3B4_rule39 reported ModelGatewayObservabilityContractService.java declaring raw prompt/provider response storage fields
B2-related: YES / B1 guard introduced the forbidden raw-storage marker literal under the B2 read-model workline
```

### Fix

```text
root cause: ModelGatewayObservabilityContractService used underscore-form raw marker literals in production source guard; existing ArchitectureTest forbids those storage-field tokens anywhere in qdr.gateway production sources
minimal fix: normalize removes space, underscore and hyphen, then checks only canonical normalized rawprompt/rawproviderresponse/providerraw/prompttext tokens
security impact: fail-closed raw material rejection preserved; no assertion skipped; no ArchitectureTest relaxation
```

### Validation Snapshot

```text
ModelGatewayObservabilityContractServiceTest + ProviderHealthReadModelServiceTest: BUILD SUCCESS / 36 tests
ArchitectureTest: BUILD SUCCESS / 39 tests
ProviderHealthReadModelServiceTest targeted: BUILD SUCCESS / 17 tests
dh-domain,dh-usecase scoped regression: BUILD SUCCESS / dh-domain 151 / dh-connector 19 / dh-usecase 386
quality validate: BUILD SUCCESS / Checkstyle 0 / Spotless passed
CI equivalent full test with clean settings: BUILD SUCCESS / Reactor 19/19 / Testcontainers executed
B2 safety scan: REVIEWED / ALLOWED_GUARD_AND_DOC_HITS_ONLY
ModelGatewayObservabilityContractService raw marker grep: NO_MATCH
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO
```

## 2026-07-09 DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION

完成 Stage-QDR-5 B2 Provider Health / Gateway Call Read Model implementation。本轮只新增 usecase 内部 read model query/view/service 和单元测试；未新增 API / Controller、migration、production repository/JDBC adapter、真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE 能力，未修改 NQ，未创建 tag，未 push。

### Scope

```text
IMPLEMENTATION
PROVIDER_HEALTH_READ_MODEL
MODEL_GATEWAY_OBSERVABILITY_READ_MODEL
INTERNAL_READ_MODEL
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayCallObservabilityView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderFailureClassificationView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderLatencyBudgetView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderTrustDecisionView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSignalView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthReadModelServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
read model structures: query、provider health view、gateway call observability view、failure/latency/trust/readiness sub-views、service
query boundary: tenantId 必填；provider/modelGatewayVersion/trace/sourceRequest/failure/trust/readiness selector 或 created/observed window 必填；limit 1..100；offset 非负；无 UUID-only selector
source reuse: 复用 B1 ModelGatewayObservabilitySummary / ProviderHealthSummary / safety contracts；支持从既有 ModelGatewayCallRecord 脱敏 metadata 投影，不扩展 production repository/JDBC
redaction guard: 复用 ModelGatewayObservabilityContractService 与 QdrPersistenceSafety；view 只暴露 safe refs、hash、enum 和 redacted summary
runtime/trading guard: readiness/trust 只作为内部 evidence，不表示 provider authorization、LIVE permission、real provider、real HTTP 或 trading signal
```

### Validation Snapshot

```text
targeted B2 test: mvn -ntp -pl dh-usecase -am "-Dtest=ProviderHealthReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test / BUILD SUCCESS / 17 tests
full validation: see docs/current/TESTING.md after final verification
```

### Next

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO
```

## 2026-07-09 DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO

完成 Stage-QDR-5 B2 Provider Health / Gateway Call Read Model implementation work order。本轮只编制后续 internal read model 的实现边界、query 边界、view 内容边界、persistence/repository blocker、API blocker、review 触发规则、安全门和测试矩阵；未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
B2_IMPLEMENTATION_BOUNDARY_DESIGN
PROVIDER_HEALTH_READ_MODEL_WO
MODEL_GATEWAY_OBSERVABILITY_READ_MODEL
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
read model target: Provider Health / Gateway Call Read Model
allowed structures: ProviderHealthReadModelQuery/View, ModelGatewayCallObservabilityView, ProviderFailureClassificationView, ProviderLatencyBudgetView, ProviderTrustDecisionView, ProviderReadinessSignalView, ProviderHealthReadModelService
query boundary: tenant-bound only; no UUID-only query, tenantless list, cross-tenant read, unbounded list, or pageSize > 100 without reject/cap
view boundary: safe refs, hashes, redacted summary and enum summaries only
persistence boundary: reuse existing model gateway call persistence / QDR gateway result / B1 contracts / QDR safety guards; schema or repository expansion triggers blocker
API boundary: no API / Controller by default; API need triggers blocker
review triggers: API / migration / production repository expansion / security boundary / P0-P1 blocker only
next action: DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未进入 B2 implementation
未进入 B3
```

## 2026-07-09 DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS

完成 Stage-QDR-5 B1 model gateway observability contracts。本轮只新增 usecase 层内存 contract、fail-closed contract service 与 unit test；未新增 migration、API、Controller、Repository/JDBC/persistence、真实 HTTP client、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力，未修改 NQ，未创建 tag，未 push。

### Scope

```text
IMPLEMENTATION
DOMAIN_USECASE_CONTRACTS_ONLY
MODEL_GATEWAY_OBSERVABILITY
PROVIDER_READINESS_FOUNDATION
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilitySummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderHealthSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderFailureClassification.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderLatencyBudgetSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderTrustDecisionSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSignal.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessStatus.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessFinding.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ProviderReadinessSeverity.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractException.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayObservabilityContractServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
contracts: observability summary、provider health summary、failure classification、latency budget、trust decision、readiness status/finding/signal
contract service: ModelGatewayObservabilityContractService validates required refs, safe hash, enum presence, latency/budget bounds, readiness/trust boundaries and fail-closed behavior
failure classification: maps existing ModelGatewayFailureCode to TIMEOUT / BUDGET_EXCEEDED / POLICY_DENIED / SOURCE_DENIED / PROVIDER_UNAVAILABLE / UNKNOWN / PAYLOAD_REJECTED
redaction guard: raw prompt / raw provider response / credential-like text rejected through QdrPersistenceSafety plus B1 raw marker guard
trading guard: BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE rejected as actionable output
runtime guard: readiness cannot enable real provider, real HTTP, LIVE, provider authorization or trading signal
```

### Validation

```text
targeted B1 test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase -am test: BUILD SUCCESS / reactor 9/9
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 / Spotless passed
safety scan: REVIEWED / GUARD_AND_DOC_HITS_ONLY
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
staged: EMPTY
```

### Next

```text
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO
```

## 2026-07-09 DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER

完成 Stage-QDR-5 implementation work order。本轮只编制 B1-B5 批次边界、测试矩阵、review 触发规则、安全门和后续提交纪律；未实现 Java、未修改测试、未新增 migration、未新增 API / Controller、未新增 Repository / JDBC / Service、未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未修改 NQ，未创建 tag，未 push。

### Scope

```text
WORK_ORDER_ONLY
STAGE_QDR_5_IMPLEMENTATION_PLANNING
MODEL_GATEWAY_OBSERVABILITY_WO
PROVIDER_READINESS_HARDENING_WO
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
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
next action: DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
```

### Work Order Result

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
review triggers: migration / API / security boundary / stage close / P0-P1 blocker only
stage order: final close review PASS -> archive close docs commit -> worktree clean -> annotated tag -> tag push -> next stage planning
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Next

```text
DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
```

## 2026-07-09 DH-STAGE-QDR-5-PLAN

完成 Stage-QDR-5 planning。本轮只做规划与 current docs 同步；未修改 Java、测试、migration、API、contracts、golden_cases 或 NQ；未接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE；未创建新 tag，未 push。

### Scope

```text
PLANNING_ONLY
STAGE_QDR_5_SCOPE_DESIGN
POST_QDR_REPLAY_EVALUATION_PLAN
MODEL_GATEWAY_OBSERVABILITY_REVIEW
PROVIDER_READINESS_BOUNDARY_REVIEW
SECURITY_BOUNDARY_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
AGENTS.md
README.md
pom.xml
scripts/verify.ps1
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V8__qdr_model_gateway_persistence_baseline.sql
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_5_PLAN.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/ARCHIVE_INDEX.md
```

### Plan Result

```text
recommended direction: Model Gateway Observability / Provider Readiness Hardening
deferred B: QDR Regression Baseline Hardening
deferred C: Real Provider Dry-run Readiness Plan
deferred D: Agent / LangGraph Preparation
stage-qdr-5 name: Model Gateway Observability / Provider Readiness Hardening
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Current Docs / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
review trigger: migration / API / Controller / security boundary / stage close / P0-P1 blocker only
next action: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
```

### Validation

```text
Get-Location: PASS / F:\project\decision-hub
branch: dev
git status --short before writing: PASS / CLEAN
tag dh-stage-qdr-4-close: PASS / local and remote visible
tag target: PASS / 62c802064f637ad03d3b0f4a185bd55fa3141af2
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --cached --name-only: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety wording scan: REVIEWED / HISTORICAL_ALLOWED_HITS_ONLY
mvn -ntp -Pquality validate: PASS / BUILD SUCCESS / reactor 19/19
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 V10
未新增 API / Controller / REST endpoint
未新增 Repository / JDBC / Service 实现
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未读取 credential / token / cookie / apiKey / apiSecret / passphrase
未持久化 raw prompt
未持久化 raw provider response
未修改 NQ
未启动 Agent runtime
未接 LangGraph runtime
未开启 LIVE
未进入 Stage-QDR-5 implementation
未创建新 tag
未 push
```

## 2026-07-09 DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION

执行 DH 文档纪律修复。本轮只修改文档、workflow guard 和 `.agents` skill policy；未修改 Java、测试、migration、API、contracts、golden_cases 或 NQ；未创建 tag，未 push，未进入 Stage-QDR-5。

### C1 Authority Repair

```text
AGENTS.md: current factsources 修复为 docs/current 实际存在入口；移除不存在的 WORKFLOW / DH_NQ_INTEGRATION / DH_REFACTOR_STAGE1 current 必读入口。
scripts/verify.ps1: current gate 改为 docs/current/STATUS.md、WORK_ORDER.md、CODEX_PROJECT_INSTRUCTIONS.md；docs/codex 仅做 historical JSON sanity check。
docs/codex/WORK_ORDER.md: 降权为 historical / non-authoritative。
docs/codex/plans/_active/STATUS.json: 增加 HISTORICAL_NON_AUTHORITATIVE 元数据。
```

### C2 Skill Policy Repair

```text
.agents/README.md: active skills 同步为 10 个。
.agents/MERGE_MAP.md: 增加 workflow governance skills 与 8 个 implementation/review skills 的关系。
nq-dh-workflow-router: 固化 archive-before-tag、stage tag after archive commit、review cadence、Stage-QDR-5 planning-first、路径/worktree 纪律。
dh-docs-writer: 固化 minimal docs sync、archive/tag 分离、historical docs 不批量重写。
db-schema-migration-review: Gate freeze 扩展为 Gate / DH Stage；明确 migration review 触发条件与 no migration batch 不强制 freeze。
```

### C3 Current Docs Repair

```text
README.md / docs/current/README.md / STATUS.md / WORK_ORDER.md / ROADMAP.md / CODEX_PROJECT_INSTRUCTIONS.md / FACTSOURCE_POLICY.md:
  STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
  STAGE_QDR_4_TAG: PENDING
  ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
  ALLOW_STAGE_QDR_5_PLAN_NOW: NO
  ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO

docs/README.md: 移除旧 DH-REFIT current stage。
docs/current/ARCHIVE_INDEX.md: 修复 stage-qdr-3 摘要中 stage-qdr-4 implementation NOT_STARTED / NO 残留。
```

### C4 Supporting Docs Noise Reduction

```text
docs/current/API.md: 只更新 supporting current summary，不重写历史 API 记录。
docs/current/DB_SCHEMA.md: 只更新 supporting current summary，不重写历史 schema 记录。
docs/gates/dh-stage4-decision-pipeline-mvp/README.md: 增加 legacy GateK wording errata/index；不改冻结历史正文。
.agents/AGENTS.frontend-skill-routing.md: later decision，未 merge/delete。
```

### Validation

```text
git diff --check: PASS_AFTER_FIX
safety scan: REVIEWED / CLASSIFIED / NO_ACTUAL_RISK
forbidden-scope diff: PASS / EMPTY
mvn -ntp -Pquality validate: PASS / BUILD SUCCESS / reactor 19/19
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
local tag dh-stage-qdr-4-close: NOT_EXISTS
remote tag dh-stage-qdr-4-close: NO_MATCH
```

下一步：如果工作区保持 docs-only 且用户进入 tag close 任务，执行 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 仍只能在 tag close 后 planning-first。

## 2026-07-09 DH-STAGE-QDR-4-CURRENT-DOCS-CLEANUP

清理 `docs/current` 下已归档的 Stage-QDR-4 详细阶段文档。`DH_STAGE_QDR_4*.md` 的实际内容已在 `docs/gates/stage-qdr-4/` 存档，本轮从 current 目录移除这些长文档，并把 current factsource 列表改为状态入口、执行纪律、验证记录和归档索引。

### Removed From Current

```text
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

### Remaining Source

```text
docs/gates/stage-qdr-4/
```

### Validation Result

```text
docs/current/DH_STAGE_QDR_4*.md: EMPTY
docs/gates/stage-qdr-4/DH_STAGE_QDR_4*.md: 7 archived files
git diff --check: PASS
forbidden-scope diff: PASS / EMPTY
mvn -ntp -Pquality validate: PASS
mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
tag created: NO
push: NO
```

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CONTENT-FIX

补齐 Stage-QDR-4 归档目录的实际阶段文档。上一轮 archive close commit 已创建 `docs/gates/stage-qdr-4/README.md` 并同步 current factsources，但目录下缺少 `DH_STAGE_QDR_4*.md` 阶段文档副本；本轮将 7 个 Stage-QDR-4 current stage docs 复制到 `docs/gates/stage-qdr-4/`，并同步 `ARCHIVE_INDEX.md` 与 gates README。

### Files Archived

```text
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

### Boundary

```text
No code change
No test change
No migration
No API
No real provider
No real HTTP
No Agent
No LangGraph
No LIVE
No tag created
No push
```

## 2026-07-09 DH-STAGE-QDR-4-ARCHIVE-CLOSE

完成 Stage-QDR-4 archive close docs sync。本轮只同步 README、docs/current 与 docs/gates 归档入口，把 Stage-QDR-4 final close review `PASS` 收口为 `CLOSED / ACCEPTED / ARCHIVED`，并保持 tag 状态为 `PENDING`。

### Scope

```text
DOCUMENTATION_ONLY
STAGE_ARCHIVE_CLOSE
QDR_REPLAY_EVALUATION_REGRESSION_ARCHIVE
TAG_PREP
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Archive Actions

```text
Stage-QDR-4 archive close docs sync: DONE
README/current factsources: UPDATED
docs/current/ARCHIVE_INDEX.md: UPDATED
docs/gates/stage-qdr-4/README.md: CREATED
No code change
No test change
No migration
No API
No tag created
Next tag pending: dh-stage-qdr-4-close
```

### Boundary

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

### Next

```text
DH-STAGE-QDR-4-TAG-CLOSE
```

## 2026-07-08 DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW

完成 Stage-QDR-4 final close review。审查范围仅限 B1 replay/evaluation domain contracts、B2 persistence baseline、B3 mock gateway regression integration、B4 regression report/read model support 与 current docs 同步；本轮未修改 Java、测试、migration、API、Controller、Repository、Service、contracts、golden_cases 或 NQ。

### Scope

```text
REVIEW_ONLY
STAGE_FINAL_CLOSE
QDR_REPLAY_EVALUATION_REGRESSION_ACCEPTANCE
TAG_PREP
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B1 evidence: PASS
B2 evidence: PASS
B3 evidence: PASS
B4 evidence: PASS
cross-stage boundary: PASS
scoped Maven tests: BUILD SUCCESS
quality validate: BUILD SUCCESS
safety scan: REVIEWED / ALLOWED_HITS_ONLY
mvnw.cmd: WRAPPER_UNUSABLE / P2 TOOLING RISK
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_COMMIT: YES
ALLOW_STAGE_QDR_5_PLAN: YES
```

### Boundary

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

### Next

```text
DH-STAGE-QDR-4-TAG-CLOSE
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION

完成 stage-qdr-4 B4 regression report / read model support implementation。本轮先执行 dirty worktree triage，确认 dirty 范围全部属于 B4 允许文件后继续 resume；只新增 usecase/internal read model 结构与回归测试，复用 B2/V9 repository ports；未新增 migration、未修改 V9、未新增 API / Controller、未新增生产 JDBC 查询、未接 provider / HTTP / Agent / LangGraph / LIVE。

### Scope

```text
IMPLEMENTATION
REGRESSION_REPORT_READ_MODEL
QDR_REPLAY_EVALUATION_REPORTING
INTERNAL_READ_MODEL
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReportFindingView.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionDriftSummary.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReadModelService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionReadModelServiceTest.java
docs/current/STATUS.md
docs/current/README.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
read model target: internal regression report / read model support
structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundary: tenantId required; caseId/evaluationId/verdictId/trace/sourceRequest/sourceDecision/verdict/severity/created range supported; limit/offset required; max pageSize 100
report content: safe refs, IDs, decision summary labels, verdict/severity, providerSummaryHash, modelGatewayVersionRef, policyVersion, redactedSummary, driftSummary, findings
drift summary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository only
redaction: view/finding/safe ref construction reruns redaction guard and rejects raw/sensitive/executable content
trading-term guard: actionLabel remains read-only direction label; executable trading terms are not exposed as allowed action
docs sync: current STATUS / WORK_ORDER / ROADMAP / TESTING / WORKLOG / CODEX_PROJECT_INSTRUCTIONS updated
```

### Validation

```text
dirty scope triage: DIRTY_SCOPE_ACCEPTED_FOR_B4_RESUME / dirty files only in B4 allowed scope
mvn -ntp -pl dh-usecase -am "-Dtest=RegressionReadModelServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 15 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / reactor 15/15
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 / Spotless passed
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
V9QdrReplayEvaluationFlywayPostgresTest: PASS / PostgreSQL 17 / Flyway v9
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
forbidden-scope dirty check: PASS / EMPTY
```

### Boundary

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

### Next

```text
DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO

完成 stage-qdr-4 B4 regression report / read model support implementation work order。B4 WO 只冻结后续 implementation 的 usecase/internal read model 边界、tenant-bound query、report content、drift summary、B2/V9 persistence reuse、redaction / trading-term guard、fail-closed、测试矩阵、validation、安全扫描、review / close 和 tag 后置规则。本轮未修改 Java、测试、migration、API、README、NQ 或 runtime。

### Scope

```text
WORK_ORDER_ONLY
B4_IMPLEMENTATION_BOUNDARY_DESIGN
REGRESSION_REPORT_READ_MODEL_WO
QDR_REPLAY_EVALUATION_REPORTING
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
report/read model target: replay case summary, evaluation case summary, expected vs actual summary, regression verdict summary, finding list, drift summary, version refs, redaction/trading-term guard evidence, tenant-bound query support
allowed structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundary: tenantId + caseId/evaluationId/verdictId/createdAt/verdict/severity/traceId/sourceRequestId/sourceDecisionId; no UUID-only, tenantless, cross-tenant, unbounded list, pageSize > 100 without reject/cap
report content boundary: safe refs/hash/version/redacted summary only; no raw prompt/provider response/credential/trading or mutation payload
drift summary boundary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 V9 seven tables and existing B2 repository only; no V10; no V9 modification; no new table; schema gap must output B4_SCHEMA_GAP_BLOCKER
API/controller decision: default no API / no Controller; if required, output B4_API_REQUIRED_BLOCKER
test matrix: 20 items recorded in TESTING.md and WO
validation commands: git diff/status, scoped Maven test, quality validate, mvnw risk check
safety scan: raw/sensitive/trading/runtime scan required for implementation
review/close rule: usecase/internal read model can proceed to validation + commit; API/migration/security expansion requires review
tag rule: stage tag only after Stage-QDR-4 final close PASS; B4 does not tag
next task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

### Boundary

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
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未生成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety wording scan: REVIEWED / ALLOWED_HITS_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

## 2026-07-08 DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN

完成 stage-qdr-4 B4 regression report / read model support planning。B4 plan 只规划 tenant-bound 只读报告与 read model 支撑，用于 B1-B3 replay/evaluation/regression 结果的 report summary、finding list、drift summary、redaction / trading-term guard evidence 和 safe refs 复核。本轮未修改 Java、测试、migration、API、README、NQ 或 runtime。

### Scope

```text
PLANNING_ONLY
REGRESSION_REPORT_READ_MODEL_PLAN
QDR_REPLAY_EVALUATION_REPORTING
READ_MODEL_BOUNDARY_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
AGENTS.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/WORKFLOW.md: NOT_FOUND / NON_BLOCKING
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/readmodel/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/readmodel/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayEvaluationPersistenceRepositoryTest.java
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Plan Result

```text
report/read model target: replay case summary, evaluation case summary, expected vs actual summary, regression verdict summary, finding list, drift summary, redaction/trading-term guard evidence
planned structures: RegressionReportQuery, RegressionReportView, RegressionReportFindingView, RegressionDriftSummary, RegressionReadModelService
query boundaries: tenantId + caseId/evaluationId/verdictId/createdAt/verdict/severity/traceId/sourceRequestId/sourceDecisionId; no UUID-only, tenantless, cross-tenant, unbounded list
report content boundary: safe refs/hash/version/redacted summary only; no raw prompt/provider response/credential/trading or mutation payload
drift summary: decisionType/actionLabel/confidenceBand/riskLevel/evidenceRefs/forbiddenActions/providerSummaryHash/modelGatewayVersionRef/promptVersionRef/policyVersion
persistence reuse: B2 V9 seven tables only; no V10; no V9 modification; no new table
API/controller decision: default no API / no Controller; if required, output B4_API_REQUIRED_BLOCKER
test matrix: 20 B4 implementation tests planned
review/freeze rule: internal read model can proceed via implementation validation; API/migration/security expansion requires separate review
tag rule: B4 is not final close; no tag before Stage-QDR-4 final close
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2/B3 required commits and B3 close commit e237504 docs(qdr): close mock gateway regression integration
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
final validation: see TESTING.md same-date B4 plan entry
```

### Boundary

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
未接 LangGraph / AutoGen / CrewAI
未启动 Agent runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B4 implementation
未进入 B5 final close
未打 tag
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW

完成 stage-qdr-4 B3 mock gateway regression integration close review。审查确认 B3 implementation commit 已提交，工作区开工前 clean，target flow、usecase/service、comparator、B2 repository 复用、redaction、trading-term guard、test evidence 与 safety scan 均满足 close 条件。B3 正式关闭为 `CLOSED / ACCEPTED`，仅允许进入 B4 planning，不允许直接进入 B4 implementation。

### Scope

```text
REVIEW_ONLY
CLOSE_REVIEW
MOCK_GATEWAY_REGRESSION_REVIEW
QDR_PIPELINE_REVIEW
REPOSITORY_REUSE_REVIEW
REDACTION_REVIEW
TRADING_TERM_GUARD_REVIEW
TEST_EVIDENCE_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B3 implementation commit: 54e5575 feat(qdr): integrate mock gateway regression baseline
worktree: CLEAN before close docs update
target flow review: PASS
usecase/service review: PASS
comparator review: PASS
persistence reuse review: PASS
redaction review: PASS
trading-term review: PASS
test evidence review: PASS
safety scan review: PASS / ALLOWED_HITS_ONLY
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
```

### Validation

```text
git status --short: PASS / CLEAN before docs update
git branch --show-current: dev
git log --oneline -20: HEAD 54e5575 feat(qdr): integrate mock gateway regression baseline
git diff --check: PASS
git diff --stat: PASS / EMPTY before docs update
git diff --name-only: PASS / EMPTY before docs update
git diff --cached --name-only: PASS / EMPTY
git diff --name-only 54e5575^ 54e5575 -- dh-app/src/main/resources/db/migration dh-api dh-app/src/main/java contracts golden_cases: PASS / EMPTY
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / reactor 15/15 / Testcontainers PostgreSQL 17 / Flyway v9 verified
mvn -ntp -Pquality validate: BUILD SUCCESS / reactor 19/19 / Checkstyle 0 violations / Spotless passed
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

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

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION

完成 stage-qdr-4 B3 mock gateway regression integration implementation。B3 本轮只实现 deterministic mock gateway regression flow，把 existing dry-run / QDR decision artifact、mock model gateway safe summary、B2 replay/evaluation persistence ports、expected/actual decision summary、regression comparator、verdict 和 finding list 串成可测试闭环。本轮未新增 migration，未修改 V1-V9，未新增 API / Controller，未接真实 provider / HTTP / Agent / LangGraph / LIVE，未修改 NQ。

### Scope

```text
IMPLEMENTATION
MOCK_GATEWAY_REGRESSION
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_INTEGRATION
TESTS
NO_DB_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/**
pom.xml
```

### Files Changed

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/RegressionEvidenceRef.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/MockGatewayRegressionCaseBuilder.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionComparator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationResult.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionSafety.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/RegressionBaselinePolicy.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionComparatorTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/QdrRegressionEvaluationServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / QDR decision artifact -> mock model gateway summary -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> regression verdict -> finding list
usecase/service: QdrRegressionEvaluationService orchestrates comparator + case builder + ReplayCaseRepository + EvaluationCaseRepository + RegressionVerdictRepository
case builder: MockGatewayRegressionCaseBuilder creates deterministic tenant-bound IDs, input/output refs, summary hashes, replay/evaluation/verdict/finding commands
comparator: QdrRegressionComparator compares decisionType, actionLabel, confidenceBand, riskLevel, evidenceRefs, forbiddenActions, providerSummaryHash, modelGatewayVersionRef, promptVersionRef, policyVersion and returns PASS / WARN / FAIL / SKIPPED
policy: RegressionBaselinePolicy controls confidence tolerance, provider hash mismatch, risk increase and policy mismatch skip behavior
redaction: RegressionEvidenceRef / QdrRegressionSafety reuse B2 ReplayPersistenceGuard and reject raw prompt/provider response/credential-like fields
trading-term guard: expected actionLabel rejects BUY / SELL / MARKET_ORDER; executable allowed actions reject PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE; verdict/finding remain regression evidence only
persistence reuse: B2/V9 repository ports reused; no V10, no V9 modification, no new table
docs sync: STATUS / WORK_ORDER / ROADMAP / TESTING / WORKLOG / CODEX_PROJECT_INSTRUCTIONS updated for B3 implementation DONE and close review next action
STAGE_QDR_4_B3_IMPLEMENTATION: DONE
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW
```

### Validation

```text
dirty worktree audit: all existing dirty/untracked files are within B3 allowed scope
git branch --show-current: dev
git log --oneline -20: contains B1/B2/B3 WO commits
mvn -ntp -pl dh-usecase -am "-Dtest=QdrRegressionComparatorTest,QdrRegressionEvaluationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 19 tests
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / Testcontainers PostgreSQL 17 / Flyway v9 verified
mvn -ntp -Pquality validate: BUILD SUCCESS / Checkstyle 0 violations / Spotless passed
required safety scan: REVIEWED / ALLOWED_HITS_ONLY
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

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

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO

完成 stage-qdr-4 B3 mock gateway regression integration work order。B3 WO 只冻结 future implementation 的 target flow、usecase/service boundary、comparison rules、B2 persistence reuse、redaction/trading guard、fail-closed 行为、测试矩阵、validation 和 close review 顺序。本轮未修改 Java、测试、migration、API、NQ 或 runtime。

### Scope

```text
WORK_ORDER_ONLY
B3_IMPLEMENTATION_BOUNDARY_DESIGN
MOCK_GATEWAY_REGRESSION_WO
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_INTEGRATION_WO
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/**/qdr/**
dh-infra/src/test/java/**/qdr/**
dh-app/src/test/java/**
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / mock gateway summary -> replay case -> evaluation case -> expected decision summary -> regression comparison -> regression verdict -> finding list
usecase boundaries: MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef
comparison rules: decisionType / actionLabel / confidenceBand / riskLevel / evidenceRefs / forbiddenActions / providerSummaryHash / modelGatewayVersionRef / promptVersionRef / policyVersion
persistence reuse: B2 V9 seven tables only; no V10; no V9 change; no new table
redaction rules: no raw prompt/provider response/credential; safe ref/hash/redacted summary only
trading-term rules: BUY/SELL/MARKET_ORDER not expected action; PLACE_ORDER/CANCEL_ORDER/MUTATE_NQ_STATE not allowed action; LONG_BIAS/SHORT_BIAS direction label only
test matrix: 22 B3 implementation tests planned
review/freeze rules: B3 plan -> B3 WO -> B3 implementation -> B3 close review -> B4 plan
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2 plan/WO/implementation/close review commits
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety scan: REVIEWED / NO_ACTUAL_RISK
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
未进入 B4
```

## 2026-07-08 DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN

完成 stage-qdr-4 B3 mock gateway regression integration planning。B3 plan 只规划 future implementation 如何复用 existing dry-run / mock gateway safe refs / B2 replay-evaluation persistence baseline，串起 replay case、evaluation case、regression comparison、verdict 和 finding list。本轮未修改 Java、测试、migration、API、NQ 或 runtime。

### Scope

```text
PLANNING_ONLY
MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN
QDR_REPLAY_EVALUATION_REGRESSION
PIPELINE_BOUNDARY_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/test/java/**/qdr/**
dh-infra/src/test/java/**/qdr/**
dh-app/src/test/java/**
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
target flow: existing dry-run / mock gateway safe refs -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> verdict -> finding list
usecase boundaries: MockGatewayRegressionCaseBuilder / QdrRegressionEvaluationService / QdrRegressionComparator / RegressionBaselinePolicy / RegressionEvidenceRef planned only
comparison rules: decisionType / actionLabel / confidenceBand / riskLevel / evidenceRefs / forbiddenActions / providerSummaryHash / modelGatewayVersionRef / promptVersionRef / policyVersion planned
persistence reuse: B2 V9 seven tables only; no V10; no V9 change
redaction rules: no raw prompt/provider response/credential; JSONB safe ref/hash/summary only
trading-term rules: BUY/SELL/MARKET_ORDER not expected action; PLACE_ORDER/CANCEL_ORDER/MUTATE_NQ_STATE not allowed action; LONG_BIAS/SHORT_BIAS direction label only
test matrix: 20 B3 implementation tests planned
review/freeze rules: B3 plan -> B3 WO -> B3 implementation -> B3 close review -> B4 plan
STAGE_QDR_4_B3_PLAN: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
```

### Validation

```text
git status --short before writing: PASS / CLEAN
git branch --show-current: dev
git log --oneline -20: contains B1/B2 plan/WO/implementation/close review commits
git diff --check before writing: PASS
git diff --stat/name-only/cached before writing: PASS / EMPTY
forbidden-scope diff: PASS / EMPTY
safety scan: REVIEWED / NO_ACTUAL_RISK
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Boundary

```text
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service 实现
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未修改 NQ
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
未进入 B4
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW

完成 stage-qdr-4 B2 persistence baseline close review。审查确认 B2 implementation commit 已存在且工作区 clean；V9 PostgreSQL/Flyway Testcontainers load、scoped tests、quality validate 与 safety scan 均满足 close 条件。B2 正式关闭为 `CLOSED / ACCEPTED`，仅允许进入 B3 planning，不允许直接进入 B3 implementation。

### Scope

```text
REVIEW_ONLY
CLOSE_REVIEW
MIGRATION_REVIEW
REPOSITORY_REVIEW
TENANT_ISOLATION_REVIEW
REDACTION_REVIEW
TEST_EVIDENCE_REVIEW
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
B2 implementation commit: 3fe1bab feat(qdr): persist replay evaluation baseline
worktree: CLEAN before close docs update
migration review: PASS
repository review: PASS
tenant isolation review: PASS
redaction review: PASS
trading-term review: PASS
test evidence review: PASS
safety scan review: PASS / NO_ACTUAL_RISK
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B3_PLAN: YES
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
```

### Validation

```text
git status --short: PASS / CLEAN before docs update
git diff --check: PASS
git diff --stat: PASS / EMPTY before docs update
git diff --name-only: PASS / EMPTY before docs update
git diff --cached --name-only: PASS / EMPTY
mvn -ntp -pl dh-app -am "-Dtest=V9QdrReplayEvaluationFlywayPostgresTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / PASS / NOT_SKIPPED
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / PASS / NOT_SKIPPED
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
required safety scan: REVIEWED / NO_ACTUAL_RISK
```

### Boundary

```text
未修改 NQ
未修改 Java 生产代码
未修改 Java 测试代码
未新增 migration
未修改 V1-V9 migration
未新增 Repository / JDBC / Service
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 implementation
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-BLOCKER-FIX

完成 B2 implementation 唯一阻断项修复：恢复并使用本机 Docker/Testcontainers，实证 `V9QdrReplayEvaluationFlywayPostgresTest` 在 PostgreSQL 17 Testcontainer 中执行并通过，Flyway validated 9 migrations，并成功迁移到 version v9。本轮未修改 migration 语义、未新增功能、未进入 B3。

### Scope

```text
BLOCKER_FIX
TESTCONTAINERS_VALIDATION
FLYWAY_POSTGRES_LOAD_VERIFICATION
NO_FEATURE_EXPANSION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Result

```text
docker status: VERIFIED / Docker Desktop 29.6.1 / PostgreSQL 17 Testcontainers runnable
V9 Flyway PostgreSQL load: PASS
Testcontainers result: PASS / NOT_SKIPPED
migration changes: NONE
test changes: NONE
docs sync: DONE
STAGE_QDR_4_B2_IMPLEMENTATION: DONE / POSTGRES_FLYWAY_VERIFIED
ALLOW_STAGE_QDR_4_B2_CLOSE_REVIEW: YES
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW
```

### Boundary

```text
未修改 NQ
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION

完成 stage-qdr-4 B2 persistence baseline 的代码实现与大部分验证：新增 V9 migration、tenant-bound repository ports、JDBC adapters、redaction guard、trading-term guard、repository / migration / tenant isolation / fail-closed tests，并完成 docs/current 最小同步。本轮未新增 API、Controller、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime、NQ mutation 或 LIVE 能力。

### Scope

```text
IMPLEMENTATION
MIGRATION
REPOSITORY
TESTS
QDR_REPLAY_EVALUATION_PERSISTENCE
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcEvaluationCaseRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcRegressionVerdictRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcReplayPersistenceSupport.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-app/src/test/java/com/guidinglight/decisionhub/V9QdrReplayEvaluationBaselineMigrationPresenceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V9QdrReplayEvaluationFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DB_SCHEMA.md
```

### Result

```text
stage-qdr-4 B2 implementation: BLOCKED / IMPLEMENTED / FLYWAY_POSTGRES_LOAD_UNVERIFIED
V9 migration: CREATED / qdr replay evaluation baseline
repository ports: DONE / TENANT_BOUND
JDBC adapters: DONE / TENANT_BOUND
redaction guard: DONE
trading-term guard: DONE
docs sync: DONE
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-BLOCKER-FIX
```

### Boundary

```text
未修改 NQ
未新增 API / Controller / REST endpoint
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
未将 replay / regression output 写成 trading signal
未进入 B3 mock gateway regression integration
```

### Validation

```text
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test: BUILD SUCCESS / WITH_DOCKER_SKIPS
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
docker ps: DOCKER_UNAVAILABLE
required safety wording scan: REVIEWED / ALLOWED_HITS_ONLY
V9 PostgreSQL/Flyway Testcontainers load: SKIPPED / DOCKER_UNAVAILABLE / NOT_PASS
```

## 2026-07-08 DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION-WO

完成 stage-qdr-4 B2 persistence baseline implementation work order。本轮只编制后续实现边界，未实现 Java、测试、migration、repository、API、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
WORK_ORDER_ONLY
B2_IMPLEMENTATION_BOUNDARY_DESIGN
MIGRATION_IMPLEMENTATION_WO
REPOSITORY_IMPLEMENTATION_WO
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Work Order Result

```text
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 implementation: NOT_STARTED / NO
migration boundary: only V9__qdr_replay_evaluation_baseline.sql in next implementation
repository boundary: ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository and JDBC adapters only
tenant isolation: all save/find/list/update methods must be tenant-bound
redaction boundary: raw prompt / raw provider response / credential forbidden
test matrix: 20 required implementation checks
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION
```

### Boundary

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
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden scope diff: PASS / EMPTY
safety wording scan: REVIEWED / FALSE_POSITIVE_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

## 2026-07-08 DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN

完成 stage-qdr-4 B2 replay / evaluation persistence baseline plan。本轮只做规划，未实现 Java、测试、migration、repository、API、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
PLANNING_ONLY
PERSISTENCE_BASELINE_DESIGN
MIGRATION_REVIEW_PREP
QDR_REPLAY_EVALUATION
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Inspected

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DB_SCHEMA.md
docs/current/WORKFLOW.md: NOT_FOUND
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/model/QdrPersistenceSafety.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/ModelGatewayCallPersistencePort.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/SaveModelGatewayCallCommand.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/approval/HumanApprovalPacketRepository.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/DecisionRequestRepository.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractServiceTest.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcDecisionCoreRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/model/JdbcModelGatewayCallRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/JdbcHumanApprovalPacketRepository.java
dh-app/src/main/resources/db/migration/V6__qdr_decision_core_baseline.sql
dh-app/src/main/resources/db/migration/V8__qdr_model_gateway_persistence_baseline.sql
pom.xml
```

### Files Changed

```text
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Plan Result

```text
tables: qdr_replay_case, qdr_evaluation_case, qdr_expected_decision_summary, qdr_regression_verdict, qdr_regression_finding, qdr_replay_input_ref, qdr_replay_output_ref
migration plan: V9__qdr_replay_evaluation_baseline.sql planned only / not created
repository boundaries: ReplayCaseRepository, EvaluationCaseRepository, RegressionVerdictRepository planned only / not implemented
tenant isolation: tenant_id required on all tables and all future query methods
redaction policy: raw prompt / raw provider response / credential forbidden; persisted content limited to summary/hash/version/status/failure classification
review/freeze: B2 implementation 前必须 freeze/review；implementation 后必须 close review
next action: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-FREEZE-REVIEW
```

### Boundary

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
未新增 Agent / LangGraph runtime
未修改 NQ
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
forbidden scope diff: PASS / EMPTY
safety wording scan: REVIEWED / EXISTING_FALSE_POSITIVE_IN_UNMODIFIABLE_FILE
mvn -ntp -Pquality validate: BUILD SUCCESS
.\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

## 2026-07-08 DH-STAGE-QDR-4-B1-REPLAY-EVALUATION-DOMAIN-CONTRACTS

完成 stage-qdr-4 B1 replay / evaluation domain contracts。B1 只实现内存领域合同、usecase fail-closed 校验和单元测试，不新增 migration、API、Controller、Repository、真实 HTTP、真实 provider、Provider SDK、Agent runtime、LangGraph runtime 或 LIVE 能力。

### Scope

```text
IMPLEMENTATION
DOMAIN_CONTRACTS_ONLY
QDR_REPLAY_EVALUATION_BASELINE
NO_MIGRATION
NO_API
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Files Changed

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractService.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/ReplayEvaluationContractServiceTest.java
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### Implementation

```text
ReplayCase / EvaluationCase: tenant-bound replay/evaluation case contracts
ExpectedDecisionSummary: structured summary only; LONG_BIAS / SHORT_BIAS remain bias labels
EvaluationPolicy: structured comparison policy; raw provider response comparison is rejected
RegressionVerdict: PASS / FAIL / WARN / SKIPPED with failure reason and findings
ReplayEvaluationContractService: validates required fields, trading-mutation terms, policy safety, and B1 initial SKIPPED verdict
```

### Validation

```text
mvn -ntp -pl dh-usecase -am "-Dtest=ReplayEvaluationContractServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test: BUILD SUCCESS / 13 tests
mvn -ntp -pl dh-domain,dh-usecase -am test: BUILD SUCCESS
```

### Boundary

```text
未修改 NQ
未新增 migration
未修改 V1-V8 migration
未新增 API / Controller
未新增 Repository / JDBC persistence
未新增真实 HTTP client
未新增真实 provider / Provider SDK
未新增 Agent / LangGraph runtime
未开启 LIVE
未保存 raw prompt / raw provider response / credential
```

### Next

```text
DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
```

## 2026-07-08 DH-STAGE-QDR-4-PLAN

完成 stage-qdr-4 planning。推荐唯一主线为 `QDR Replay / Evaluation / Regression Baseline`，并把后续动作限定为 implementation work order，不直接启动 implementation。

### Scope

```text
PLANNING_ONLY
STAGE_QDR_4_SCOPE_DESIGN
POST_MODEL_GATEWAY_HARDENING_PLAN
REPLAY_EVAL_PROVIDER_READINESS_REVIEW
SECURITY_BOUNDARY_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
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

### Plan Result

```text
recommended direction: A / QDR replay, evaluation and regression baseline
deferred B: Model gateway observability / provider readiness hardening
deferred C: real provider dry-run readiness plan
deferred D: Agent / LangGraph preparation
batches: B1 replay/eval contracts; B2 persistence baseline; B3 mock gateway regression integration; B4 report/read model/docs support; B5 close review
next task: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

### Boundary

```text
本轮不修改 Java 生产代码
本轮不修改 Java 测试代码
本轮不新增 migration
本轮不修改 V1-V8 migration
本轮不新增 V9 migration
本轮不新增 API / Controller / REST endpoint
本轮不新增真实 HTTP outbound
本轮不新增真实 provider client
本轮不新增 Provider SDK
本轮不启动 Agent / LangGraph runtime
本轮不修改 NQ
本轮不启用 LIVE
stage-qdr-4 implementation 未启动
```

### Files Changed

```text
README.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
```

### Validation

```text
git status --short: DOCS_ONLY_DIRTY / NO_STAGED
git diff --check: PASS_WITH_EOL_WARNINGS
git diff --stat: DOCS_ONLY_TRACKED_DIFF
git diff --name-only: DOCS_ONLY_TRACKED_DIFF
git diff --cached --name-only: PASS / EMPTY
safety wording scan: REVIEWED / FALSE_POSITIVE_ONLY
mvn -ntp -Pquality validate: BUILD SUCCESS
.\\mvnw.cmd -v: WRAPPER_UNUSABLE / P2 TOOLING RISK
```

### Next

```text
DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

## 2026-07-08 DH-STAGE-QDR-3-FINAL-CLOSE-DOCS-SYNC

将用户提供的 `DH-STAGE-QDR-3-B5-CLOSE-REVIEW` ACCEPTED 结论写回 current factsources，并把 stage-qdr-3 final close 收口为 `CLOSED / ACCEPTED`。

### Scope

```text
DOCUMENTATION_ONLY
STAGE_FINAL_CLOSE_RECORD
ACCEPTANCE_RESULT_SYNC
STAGE_QDR_3_CLOSE_RECORD
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
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

### Boundary

```text
本轮不修改 Java 生产代码
本轮不修改 Java 测试代码
本轮不新增 migration
本轮不修改 V1-V8 migration
本轮不新增 V9 migration
本轮不新增 API / Controller / REST endpoint
本轮不新增真实 HTTP outbound
本轮不新增真实 provider client
本轮不新增 Provider SDK
本轮不启动 Agent / LangGraph runtime
本轮不修改 NQ
本轮不启用 LIVE
stage-qdr-4 implementation 未启动
```

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

### Next

```text
DH-STAGE-QDR-4-PLAN
```

## 2026-07-07 DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE

执行 docs-only governance pre-close 收口。本轮目标是把 stage-qdr-3 B5 retry 前的当前事实源、归档索引、supporting docs 和 blocker 规则拆开，避免旧阶段文档继续阻断 close review。

### Scope

```text
DOCUMENTATION_ONLY
DOCS_GOVERNANCE
FACTSOURCE_CONSOLIDATION
ARCHIVE_CLEANUP
CURRENT_STATE_INDEXING
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Current State

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/Project/decision-hub
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

### Archive Actions

```text
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_WORK_ORDER.md
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_DISCIPLINE_CLOSEOUT.md
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-3/pre-close-current-snapshot-20260707/
```

上述文件均为 historical record，不是 current factsource。current docs 只保留 `README / STATUS / WORK_ORDER / CODEX_PROJECT_INSTRUCTIONS / TESTING / FACTSOURCE_POLICY / ARCHIVE_INDEX` 作为 B5 retry 前的治理入口。

### Files Changed

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/gates/**
```

### Next

```text
DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

## 2026-07-08 DH-DOCS-GOVERNANCE-GATES-ARCHIVE-FIX

修正上一轮归档路径口径：项目既有阶段归档目录是 `docs/gates`，本轮不再引入 `docs/archive` 作为第二套 QDR 归档体系。

### Scope

```text
DOCUMENTATION_ONLY
DOCS_GOVERNANCE_CORRECTION
GATES_ARCHIVE_ALIGNMENT
CURRENT_DOCS_CLEANUP
FACTSOURCE_CONSOLIDATION
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

### Archive Actions

```text
docs/archive/stage-qdr-2/* -> docs/gates/stage-qdr-2/
docs/archive/stage-qdr-3/* -> docs/gates/stage-qdr-3/
docs/current historical residuals -> docs/gates/stage-qdr-3/current-docs-historical-20260708/
docs/archive empty directory cleanup: DONE
```

### Current Docs Retained

```text
README.md
STATUS.md
WORK_ORDER.md
CODEX_PROJECT_INSTRUCTIONS.md
TESTING.md
FACTSOURCE_POLICY.md
ARCHIVE_INDEX.md
WORKLOG.md
ROADMAP.md
API.md
DB_SCHEMA.md
```

### Current State

```text
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```
