# DH Stage4 Decision Pipeline MVP 工单

> 任务：DH-STAGE4-DECISION-PIPELINE-MVP-WO
> 状态：ACCEPTED / CLOSED
> 来源计划：`docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`
> 日期：2026-07-01
> 范围：docs-only / work-order-only

## 1. 阶段锁定

本工单是 DH Stage4 Decision Pipeline MVP 的批次执行计划。它只授权按批次、按 review 推进；不得据此跳过批次 review 做连续实现。

当前已接受事实：

```text
DH-STAGE4-DECISION-PIPELINE-MVP-PLAN: ACCEPTED / CLOSED
DH-STAGE4-DECISION-PIPELINE-MVP-WO: ACCEPTED / CLOSED
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

当前执行状态：

```text
K1 Decision Contract Freeze: IMPLEMENTED / READY FOR REVIEW
Next concrete action: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
K1-K8: CLOSED / ACCEPTED
```

本工单只允许后续按小批次推进：

```text
K1 -> review -> K2 -> review -> K3 -> review -> K4
K1-K5 complete before K6
K1-K7 complete before K8
```

Stage4 MVP 关闭前，以下能力始终禁止：

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

## 2. 共享 DecisionOutput 合同

所有 Stage4 批次必须保持以下输出合同：

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

`forbiddenActions` 必须始终包含：

```text
PLACE_ORDER
CANCEL_ORDER
MUTATE_NQ_STATE
READ_NQ_DB
WRITE_NQ_DB
```

输出不得包含：

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

## 3. 批次索引

| 批次 | 名称 | 目的 | 依赖 |
| --- | --- | --- | --- |
| K1 | Decision Contract Freeze | 冻结 request/output/action/audit 合同 | PLAN accepted |
| K2 | DecisionOrchestrator Skeleton | 实现 fail-closed orchestration skeleton | K1 reviewed |
| K3 | Audit / Snapshot / Trace Persistence | 持久化 request、context、trace、output、audit | K2 reviewed |
| K4 | Replay Read Model | 只读读取已持久化 decision records，不 rerun | K3 reviewed |
| K5 | Mock Provider / Provider Health / Budget / Latency | 增加 mock-only provider controls | K4 reviewed |
| K6 | Mock NQ Dry-run Contract Tests | 只用 mock NQ fixtures 验证边界 | K1-K5 complete |
| K7 | Golden Cases / Eval 基线 | 锁定 deterministic eval baseline | K6 reviewed |
| K8 | Acceptance / Freeze | 关闭 Stage4 MVP 并准备 freeze | K1-K7 complete |

## 4. K1 Decision Contract Freeze

批次名称：`K1 Decision Contract Freeze`

目标：

```text
在任何 orchestrator implementation 前，冻结 read-only decision input、evidence、policy、risk、action vocabulary、forbidden actions、output JSON、trace shape 和 audit event shape 的 domain contract。
```

允许文件：

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/**
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/**Decision*ContractTest.java
contracts/json-schema/dh-decision-*.schema.json
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

禁止文件：

```text
dh-api/**
dh-app/src/main/resources/db/migration/**
dh-infra/**
dh-connector/** real provider / HTTP client files
NQ repository
production .env or credentials
```

生产代码变更允许：YES，仅 domain contract。

测试代码变更允许：YES。

migration 允许：NO。

API 变更允许：NO。

主要类 / 接口：

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
ForbiddenAction
```

主要 schema / table（如有）：

```text
contracts/json-schema/dh-decision-request.schema.json
contracts/json-schema/dh-decision-output.schema.json
K1 不新增数据库表。
```

必需测试：

```text
DecisionRequestContractTest
DecisionOutputContractTest
DecisionRequestSchemaContractTest
DecisionOutputSchemaContractTest
DecisionEnumContractTest
DecisionNoTradingInstructionContractTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
不新增 Controller / REST API。
不新增 migration。
不启用 provider runtime。
不启用 NQ runtime。
不启用 AI / LLM runtime。
不启用 LangGraph runtime。
不启用 LIVE。
```

退出条件：

```text
DecisionOutput READ_ONLY_RECOMMENDATION 已有 contract test。
ABSTAIN default 已有 contract test。
No evidence 与 provider failure 映射到 ABSTAIN。
Policy denied 映射到 BLOCKED 或 ABSTAIN，并且 fail-closed。
High risk 禁止 LONG_BIAS / SHORT_BIAS。
forbiddenActions 包含五个必需 forbidden actions。
Schemas 与 Java contract 一致。
mvn test 与 mvn -Pquality validate 通过。
```

回滚方式：

```text
回退 K1 domain decision package、decision JSON schemas、K1 tests 和 docs status entries；不需要 data migration rollback。
```

下一批次：

```text
K2 DecisionOrchestrator Skeleton after K1 review.
```

## 5. K2 DecisionOrchestrator Skeleton

批次名称：`K2 DecisionOrchestrator Skeleton`

目标：

```text
创建 usecase-layer orchestration skeleton，接收已冻结的 DecisionRequest contract，评估 evidence / policy / risk / provider signals，并且只返回 fail-closed read-only DecisionOutput。该 skeleton 不得调用真实 provider 或 NQ。
```

允许文件：

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

禁止文件：

```text
dh-api/**
dh-infra/**
dh-app/src/main/resources/db/migration/**
real provider adapters
HTTP clients to NQ
LangGraph / LLM runtime wiring
NQ repository
```

生产代码变更允许：YES，仅 usecase skeleton 与 mock/disabled ports。

测试代码变更允许：YES。

migration 允许：NO。

API 变更允许：NO。

主要类 / 接口：

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

主要 schema / table（如有）：

```text
K2 不新增 schema。
K2 不新增数据库表。
```

必需测试：

```text
DecisionOrchestratorDefaultAbstainTest
DecisionOrchestratorProviderFailureTest
DecisionOrchestratorPolicyDeniedTest
DecisionOrchestratorHighRiskTest
DecisionOrchestratorNoEvidenceTest
DisabledDecisionProviderTest
NoRealProviderBoundaryTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
不新增 REST endpoint。
不新增 persistence。
不接 real provider。
不接 real HTTP。
不调用 NQ。
不启用 AI / LLM runtime。
不启用 LangGraph runtime。
不启用 LIVE。
```

退出条件：

```text
empty evidence 时 Orchestrator 返回 ABSTAIN。
provider failure 时 Orchestrator 返回 ABSTAIN。
Policy denied 必须 fail-closed。
High risk 阻断 LONG_BIAS / SHORT_BIAS。
Mock 和 disabled providers 必须 deterministic。
不得引入名为 RealDecisionProvider 或 RealClient 的类。
```

回滚方式：

```text
回退 K2 usecase decision package、connector decision mock/disabled package、K2 tests 和 docs status entries。除非 K2 review 发现 contract defect，否则 K1 frozen contracts 仍保持有效。
```

下一批次：

```text
K3 Audit / Snapshot / Trace Persistence after K2 review.
```

## 6. K3 Audit / Snapshot / Trace Persistence

批次名称：`K3 Audit / Snapshot / Trace Persistence`

目标：

```text
持久化 decision request、context snapshot、trace steps、provider call summary、output 和 audit events，使每个 recommendation 可 replay、可 audit。Audit write failure 必须 fail-closed，不得产出 success output。
```

允许文件：

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

禁止文件：

```text
dh-api/**
real provider adapters
real NQ client
NQ repository
production database access
credentials
```

生产代码变更允许：YES。

测试代码变更允许：YES。

migration 允许：YES，仅 DB review 之后，且只允许 DH-owned audit tables。

API 变更允许：NO。

主要类 / 接口：

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

主要 schema / table（如有）：

```text
dh_decision_request
dh_decision_context_snapshot
dh_decision_trace_step
dh_provider_call_log
dh_decision_output
dh_decision_audit_event
```

实现说明：

```text
K3 开工前必须重新检查当前最新 Flyway version。
如果 V4 仍是最新 migration，则使用 V5；如果已存在更新 migration，则使用下一个可用版本。不得复用既有 migration version。
```

必需测试：

```text
DecisionAuditMigrationContractTest
JdbcDecisionAuditRepositoryTest
JdbcDecisionSnapshotRepositoryTest
JdbcDecisionTraceRepositoryTest
DecisionAuditWriteFailureFailClosedTest
DecisionAuditNoSecretPersistenceTest
DecisionPersistenceTransactionBoundaryTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
仅允许 DH-owned persistence。
不得访问 NQ DB。
不得持久化 provider secret。
不新增 API endpoint。
不启用 Integration-1 runtime。
不启用 LIVE。
```

退出条件：

```text
所有 audit / snapshot / trace tables 都必须有 primary keys、tenant/request/trace indexes、created_at timestamps 和安全 JSON payload boundaries。
Repository tests 覆盖 success、missing record、duplicate/idempotent paths。
Audit write failure 必须阻止 success DecisionOutput。
不得持久化或记录 secret/token/key/cookie fields。
```

回滚方式：

```text
回退 K3 Java repositories / services / tests / docs。本地开发库如已应用 K3 tables，只能删除 K3 创建的 DH audit tables。本工单不授权 production database migration。
```

下一批次：

```text
K4 Replay Read Model after K3 review.
```

## 7. K4 Replay Read Model

批次名称：`K4 Replay Read Model`

目标：

```text
创建 read model，用于 audit 与 review 时重建 persisted decision records；不得 rerun providers、AI、NQ 或 policy side effects。
```

允许文件：

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

禁止文件：

```text
dh-api/**
provider runtime
NQ runtime
LangGraph runtime
LLM runtime
new migration unless K3 review explicitly requires a schema correction
```

生产代码变更允许：YES。

测试代码变更允许：YES。

migration 允许：默认 NO。

API 变更允许：NO。

主要类 / 接口：

```text
DecisionReplayReadModel
DecisionReplayQuery
DecisionReplayView
DecisionReplayTraceView
JdbcDecisionReplayReadModel
```

主要 schema / table（如有）：

```text
Reuses K3 tables only.
K4 默认不新增 table。
```

必需测试：

```text
DecisionReplayReadModelTest
DecisionReplayDoesNotRerunProviderTest
DecisionReplayTenantBoundaryTest
DecisionReplayMissingRecordTest
DecisionReplayTraceOrderingTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
Replay 必须只读。
Replay 不得调用 provider。
Replay 不得调用 NQ。
Replay 不得重新评估 policy。
Replay 不得产生新的 trading suggestion。
```

退出条件：

```text
Replay 只返回 stored request / snapshot / trace / output / audit views。
Tenant boundary 必须有测试。
Missing records 返回明确 not-found semantics。
replay package 不得触达 Provider 或 NQ ports。
```

回滚方式：

```text
回退 K4 replay package、read model implementation、K4 tests 和 docs entries。除非单独请求 K3 rollback，否则 K3 persisted audit schema 保持不变。
```

下一批次：

```text
K5 Mock Provider / Provider Health / Budget / Latency after K4 review.
```

## 8. K5 Mock Provider / Provider Health / Budget / Latency

批次名称：`K5 Mock Provider / Provider Health / Budget / Latency`

目标：

```text
增加 mock-only provider health、budget、latency controls，使 provider failure、timeout、untrusted state、budget exhaustion 都 fail-closed 到 ABSTAIN 或 BLOCKED，不启用真实 provider runtime。
```

允许文件：

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

禁止文件：

```text
real provider adapters
OpenAI / Claude / Gemini / local model runtime wiring
LangGraph runtime
NQ runtime
dh-api/**
migration files, unless a later reviewed K5-DB sub-batch is opened
```

生产代码变更允许：YES，仅 mock/disabled control logic。

测试代码变更允许：YES。

migration 允许：K5 base batch 中 NO。

API 变更允许：NO。

主要类 / 接口：

```text
DecisionProviderHealth
DecisionProviderHealthStatus
DecisionProviderBudget
DecisionProviderLatencyPolicy
DecisionProviderSafetyGate
MockDecisionProviderHealthStore
DecisionProviderFailureReason
```

主要 schema / table（如有）：

```text
K5 base batch 不新增 table。
如后续单独 DB batch 获批，deferred table names 为：
dh_provider_health
dh_provider_budget_event
dh_model_call_summary
```

必需测试：

```text
DecisionProviderHealthGateTest
DecisionProviderBudgetGateTest
DecisionProviderLatencyPolicyTest
DecisionProviderUntrustedFailClosedTest
DecisionProviderTimeoutAbstainTest
NoRealProviderRuntimeTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
仅 mock-only provider behavior。
不调用 external model。
不接 provider credential。
不启用 runtime agent phase。
不启用 LangGraph runtime。
不启用 Integration-1 runtime。
```

退出条件：

```text
Provider unavailable -> ABSTAIN。
Provider timeout -> ABSTAIN。
Provider untrusted -> BLOCKED or ABSTAIN。
Budget exceeded -> fail-closed。
Latency policy breach -> fail-closed。
不得引入 real provider class 或 dependency。
```

回滚方式：

```text
回退 K5 provider health / budget / latency control classes、mock provider changes、K5 tests 和 docs entries；不需要 migration rollback。
```

下一批次：

```text
K6 Mock NQ Dry-run Contract Tests after K1-K5 complete.
```

## 9. K6 Mock NQ Dry-run Contract Tests

批次名称：`K6 Mock NQ Dry-run Contract Tests`

目标：

```text
证明 Stage4 DecisionOutput 只能 against mock NQ dry-run fixtures 做合同校验，不会调用 NQ runtime、NQ DB、NQ HTTP、Paper Run、orders 或 state mutation。
```

允许文件：

```text
dh-domain/src/test/java/com/guidinglight/decisionhub/gatek/mocknq/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/gatek/mocknq/**
golden_cases/decision-pipeline/mock-nq/**
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

禁止文件：

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

生产代码变更允许：NO。

测试代码变更允许：YES。

migration 允许：NO。

API 变更允许：NO。

主要类 / 接口：

```text
MockNqDecisionRequestFactory
MockNqDecisionContractFixture
GateKDecisionDryRunContractTest
GateKDecisionNoSideEffectTest
GateKDecisionHeaderBoundaryTest
```

主要 schema / table（如有）：

```text
Reuses decision JSON schemas.
K6 不新增数据库表。
```

必需测试：

```text
GateKDecisionDryRunContractTest
GateKDecisionNoSideEffectTest
GateKDecisionForbiddenActionContractTest
GateKDecisionMockNqPayloadShapeTest
GateKDecisionNoNqRuntimeTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
仅允许 Mock NQ。
不启用 NQ runtime。
不访问 NQ DB。
不做 NQ HTTP。
不启动 Paper Run。
不触碰 order state。
不访问 account 或 credential。
```

退出条件：

```text
Dry-run tests 验证 JSON shape 与 forbidden actions。
Side-effect tracker 必须保持 empty。
不得修改 production class。
不得引入 NQ package、URL 或 runtime dependency。
```

回滚方式：

```text
移除 K6 test fixtures、golden mock NQ fixtures 和 docs status entries；不需要 production rollback。
```

下一批次：

```text
K7 Golden Cases / Eval 基线 after K6 review.
```

## 10. K7 Golden Cases / Eval 基线

批次名称：`K7 Golden Cases / Eval 基线`

目标：

```text
创建 deterministic golden cases，锁定 empty evidence、conflicting evidence、high risk、provider failure、policy denied 和 audit failure 路径的 read-only recommendation 行为。
```

允许文件：

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

禁止文件：

```text
production provider runtime
real NQ client
dh-api/**
migration files
NQ repository
LIVE configuration
```

生产代码变更允许：默认 NO。

测试代码变更允许：YES。

migration 允许：NO。

API 变更允许：NO。

主要类 / 接口：

```text
DecisionGoldenCase
DecisionGoldenCaseLoader
DecisionGoldenCaseEvaluatorTest
DecisionGoldenCaseBaselineTest
```

主要 schema / table（如有）：

```text
golden_cases/decision-pipeline/*.json
K7 不新增数据库表。
```

必需测试：

```text
DecisionGoldenCaseBaselineTest
DecisionGoldenCaseEmptyEvidenceTest
DecisionGoldenCaseProviderFailureTest
DecisionGoldenCasePolicyDeniedTest
DecisionGoldenCaseHighRiskTest
DecisionGoldenCaseNoSecretFixtureTest
```

验证命令：

```powershell
git status --short
git diff --check
mvn test
mvn -Pquality validate
```

边界确认：

```text
Golden cases 必须是 deterministic fixtures。
不做 real provider replay。
不做 NQ runtime replay。
不依赖 live market data。
不包含 secret fixture。
```

退出条件：

```text
Golden cases 覆盖 normal、empty、conflicting、high-risk、provider-failure、policy-denied 和 audit-failure paths。
Expected outputs 均为 READ_ONLY_RECOMMENDATION。
所有 forbidden actions 保持 present。
high-risk outputs 不得包含 directional bias。
```

回滚方式：

```text
移除 K7 golden case fixtures、eval tests 和 docs entries。除非 reviewed K7 sub-batch 明确改过 eval production helpers，否则不需要 production rollback。
```

下一批次：

```text
K8 Acceptance / Freeze after K1-K7 complete.
```

## 11. K8 Acceptance / Freeze

批次名称：`K8 Acceptance / Freeze`

目标：

```text
review K1-K7 evidence；只有当 contracts、tests、audit、replay、mock provider controls 和 golden cases 均通过时，才允许 close DH Stage4 Decision Pipeline MVP，并准备 docs/current freeze package。
```

允许文件：

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/gates/dh-stage4-decision-pipeline-mvp/** only after explicit freeze/archive authorization
```

禁止文件：

```text
production Java code
test Java code
contracts/**
golden_cases/**
migration files
API files
NQ repository
```

生产代码变更允许：NO。

测试代码变更允许：NO。

migration 允许：NO。

API 变更允许：NO。

主要类 / 接口：

```text
None in K8.
```

主要 schema / table（如有）：

```text
K8 不修改 schema 或 table。
```

必需测试：

```text
K8 不新增 tests。
K8 必须引用最近一次通过的 K1-K7 validation evidence。
```

验证命令：

```powershell
git status --short
git diff --check
git diff --stat
mvn test
mvn -Pquality validate
```

边界确认：

```text
Acceptance 仅 docs-only。
不新增 implementation。
不启用 Integration-1 runtime。
不启用 Agent phase。
不启用 LangGraph runtime。
不启用 LIVE。
```

退出条件：

```text
K1-K7 均已 reviewed and accepted。
TESTING.md 必须包含 passing mvn test 和 mvn -Pquality validate evidence。
STATUS.md 只能在 evidence 存在后声明 Stage4 MVP closed。
WORKLOG.md 必须记录 exact batch completion evidence。
只有明确授权时才创建 freeze snapshot。
```

回滚方式：

```text
回退 K8 docs changes。只有当 snapshot 是 K8 创建且用户明确授权 removal 时，才移除 freeze snapshot。不得从 K8 回退 K1-K7 implementation。
```

下一批次：

```text
GateL 或后续 planning。LangGraph runtime 属于 GateL 或更晚阶段，不属于 Stage4 MVP。
Integration-1 runtime 在 GateN-rebased planning package reviewed and accepted 前保持 blocked。
```

## 12. 跨批次验收矩阵

| 要求 | K1 | K2 | K3 | K4 | K5 | K6 | K7 | K8 |
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
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

解释：

```text
ALLOW_K1_IMPLEMENTATION: YES 仅表示 WO accepted 后可以启动 K1。
它不授权 K2-K8 无 review 连续实施。
它不授权 Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。
```

## 14. 下一轮 Codex 提示词

当前 K1 已实现并进入 review 阶段。下一轮提示词应为：

```text
你在 F:\project\decision-hub 仓库 dev 分支上工作。

任务名：DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW。

目标：只读 review K1 Decision Contract Freeze 的 domain contract、JSON Schema、contract tests、docs 与验证证据。
判断是否允许 K1 close；不要实现 K2。

禁止：
- 不实现 DecisionOrchestrator
- 不修改生产代码
- 不修改测试代码
- 不新增 API
- 不新增 migration
- 不实现真实 NQ client
- 不实现 RealClient
- 不接真实 HTTP / event 到 NQ
- 不调用 NQ /api/ai/research/backtest-requests
- 不接真实 LLM provider
- 不接 LangGraph runtime
- 不输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 作为 action
- 不启动 Paper Run
- 不修改 NQ 交易状态
- 不访问交易所密钥
- 不触碰 LIVE trading
- 不读取或写入 NQ DB
- 不新增 provider / 交易路径

验收命令：
- git status --short
- git diff --check
- mvn test
- mvn -Pquality validate
```
