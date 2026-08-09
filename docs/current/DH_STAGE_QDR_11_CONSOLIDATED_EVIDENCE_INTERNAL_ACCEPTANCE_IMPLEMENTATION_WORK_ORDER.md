# Stage-QDR-11 Consolidated Evidence Internal Acceptance Implementation Work Order

## 1. Task classification and authority

```text
Task: DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-IMPLEMENTATION-WORK-ORDER
Primary classification: DOCUMENTATION
Work type: WORK_ORDER_ONLY
Stage ownership: DH_OWNED
Repository: E:/Project/decision-hub
Branch: dev
Planning commit: 148e20054b7fc816b696f361bbeb407160bff546
Planning parent / origin/dev: 86d6dfb8b7933eda1592963414d5b81ea4dc4605
Change type: DOCS_ONLY
Implementation: NOT AUTHORIZED IN THIS TASK
Push / tag: NOT AUTHORIZED / NOT AUTHORIZED
Production capacity: NOT_PROVEN
```

本工单只冻结 Stage-QDR-11 的精确实现范围、单一 evidence authority、既有 acceptance 模型演进、
fail-closed 映射、internal consumer/wiring、测试矩阵与停止条件。它不执行 Java 或测试修改，不创建
API、migration、schema、Repository/JDBC、scheduler、外部调用或任何交易/learning 能力。

## 2. Baseline and scope preconditions

本工单只允许以下 baseline：

```text
CASE A:
HEAD = 148e20054b7fc816b696f361bbeb407160bff546
HEAD^ = origin/dev = 86d6dfb8b7933eda1592963414d5b81ea4dc4605
ahead / behind = 1 / 0

CASE B:
HEAD = origin/dev = 148e20054b7fc816b696f361bbeb407160bff546
ahead / behind = 0 / 0

branch = dev
worktree = clean
staged = empty
```

其他状态必须返回：

```text
STAGE_QDR_11_WORK_ORDER_BASELINE_BLOCKED
```

### 2.1 Scope sets

```text
READ_SCOPE:
- 本工单第 11.3 节 DOCS_TASK_WRITE_ALLOWLIST
- docs/current/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md
- 当前 authority 文档的 terminal block
- 第 3 节列出的 production/test code reality 文件
- Git branch/SHA/status/diff path metadata

VALIDATION_SCOPE:
- DOCS_TASK_WRITE_ALLOWLIST 的内容与 diff
- repository diff path metadata，用于证明 technical/archive/unexpected diff 为 0

FIXABLE_BLOCKER_SCOPE:
- DOCS_TASK_WRITE_ALLOWLIST

CURRENT_FACTSOURCE_SCAN_SCOPE:
- README.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/WORK_ORDER.md
- docs/current/ROADMAP.md
- docs/current/TESTING.md
- docs/current/WORKLOG.md
- docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

冻结不变量：

```text
VALIDATION_SCOPE subset_of READ_SCOPE
FIXABLE_BLOCKER_SCOPE subset_of DOCS_TASK_WRITE_ALLOWLIST
CURRENT_FACTSOURCE_SCAN_SCOPE subset_of DOCS_TASK_WRITE_ALLOWLIST
EXPECTED: 3 / 3 PASS
FAILURE: TASK_SCOPE_DESIGN_INVALID
```

## 3. Code reality inspected

### 3.1 Stage-QDR-10 consolidated evidence

| 角色 | 实际类型 | 当前事实 |
| --- | --- | --- |
| query | `DecisionFeedbackEvidenceQuery` | `FeedbackExecutionScope` 是 tenant/environment 唯一 authority；携带 trace/request/decision/run 与显式 bounds |
| aggregate | `DecisionFeedbackEvidenceAggregate` | immutable；组合 decision evidence、persisted environment provenance、bounded feedback、completeness 与 findings |
| service | `DecisionFeedbackEvidenceService` | 已有 Spring bean；只读组合现有 readers；production main source 无 `aggregate(...)` consumer |
| completeness | `EvidenceCompleteness` | `COMPLETE_WITHIN_BOUNDS`、`PARTIAL_WITHIN_BOUNDS`、`INCONSISTENT`、`NOT_FOUND` |
| bounds | `BoundedEvidencePolicy` | closed interval、90 天上限、1..100、固定 policy id/version、稳定 ordering、overflow `FAIL_CLOSED` |

### 3.2 Existing internal acceptance

```text
EXISTING_MODEL_TO_EVOLVE:
com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayInternalReport

EXISTING_SERVICE_TO_EVOLVE:
com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayReportService

EXISTING_STATUS_TO_REUSE:
com.guidinglight.decisionhub.usecase.qdr.report.InternalAcceptanceStatus

EXISTING_FINDING_TO_REUSE:
com.guidinglight.decisionhub.usecase.qdr.report.InternalAcceptanceFinding
```

当前 `DecisionEvidenceReplayReportService.generate(...)` 输入是：

```text
DecisionEvidenceCorrelation
DecisionEvidenceAggregate                     <- OLD_INPUT_TO_RETIRE
DeterministicReplayResult
RegressionReportView
ProviderReadinessEvaluationResult
ModelGatewayObservabilityReport
```

`InternalAcceptanceStatus` 的实际状态为：

```text
ACCEPTED
REJECTED
INCOMPLETE
INVALID
UNSUPPORTED
FAILED
```

当前 report 中由 legacy decision evidence 派生的字段是：

```text
evidenceStatus
evidenceRefs
missingMandatoryEvidence
EVIDENCE source findings
```

当前 report 已有稳定 non-authorization declaration，且 `InternalAcceptanceStatus` 的 Provider、NQ、
trading/execution、Paper/LIVE helper 对所有状态固定返回 `false`。当前 report 不携带 environment、
`EvidenceCompleteness`、`BoundedEvidencePolicy`、overflow state 或 consolidated aggregate ref。

### 3.3 Caller, bean and duplicate-capability result

```text
DecisionEvidenceReplayReportService production caller: 0
DecisionEvidenceReplayReportService Spring bean: 0
DecisionFeedbackEvidenceService Spring bean: 1
DecisionFeedbackEvidenceService production aggregate consumer: 0
Production-wired consolidated acceptance: 0
Second InternalAcceptanceStatus enum: 0
Second DecisionEvidenceReplayInternalReport model: 0
```

`ProviderReadinessAcceptanceStatus` 是既有 observability 子门状态，不是第二套 consolidated internal
acceptance；但其 `PASS` 字样存在被误读为授权的长期语义风险，因此继续只作为 existing acceptance 的
一个输入门，不得提升为 Stage-QDR-11 verdict authority。

结论：

```text
STAGE_QDR_11_DUPLICATE_CAPABILITY_BLOCKED: NO
STAGE_QDR_11_ACCEPTANCE_MODEL_SCOPE_BLOCKED: NO
MIGRATION_REQUIRED: NO
API_CHANGE_REQUIRED: NO
REPOSITORY_EXPANSION_REQUIRED: NO
```

## 4. Selected design

### 4.1 Single evidence authority

```text
NEW_INPUT_AUTHORITY:
DecisionFeedbackEvidenceAggregate

AUTHORITY_COUNT:
1

LEGACY_DIRECT_EVIDENCE_INPUT:
REMOVED FROM THE EVOLVED PUBLIC GENERATE PATH
```

`DecisionEvidenceReplayReportService` 必须直接消费一个 `DecisionFeedbackEvidenceAggregate`。其中嵌套的
`decisionEvidence()` 只允许作为 consolidated aggregate 内部、已经过 scope/provenance/bounds 校验的组成部分
投影到现有 report；不得再由调用方并列传入第二个 `DecisionEvidenceAggregate`。

禁止 acceptance 直接读取或接受：

```text
legacy DecisionEvidenceAggregate argument
raw feedback evidence argument/list
HistoricalFeedbackEvidenceReadService or QueryPort
independent historical query
second correlation result
repository/JDBC result
```

### 4.2 Compatibility rule

```text
COMPATIBILITY_RULE:
- 删除/替换旧 generate(DecisionEvidenceCorrelation, DecisionEvidenceAggregate, ...) internal Java surface
- 不保留 deprecated overload
- 不提供 fallback-to-old-evidence adapter
- 更新唯一既有 unit test caller
- public HTTP/API/schema compatibility impact = NONE
```

当前旧 surface 没有 production caller 或 Spring bean，因此 source-level internal evolution 不需要 API、schema、
migration 或 Repository 扩张。若实现时发现未知 production caller，立即返回
`STAGE_QDR_11_DUPLICATE_CAPABILITY_BLOCKED`，不得保留双路径兼容。

## 5. B1 — Single Evidence Authority and Acceptance Mapping

B1 是本阶段唯一明确的 security semantics 冻结点。

### 5.1 Exact completeness mapping

| `EvidenceCompleteness` | 既有 `InternalAcceptanceStatus` 结果 | 后续 gates | 说明 |
| --- | --- | --- | --- |
| `COMPLETE_WITHIN_BOUNDS` | 不是预先 verdict；满足其余全部 gate 后才可 `ACCEPTED` | 正常评估 | completeness 只证明 policy bounds 内 evidence 完整，不证明 decision correctness |
| `PARTIAL_WITHIN_BOUNDS` | `INCOMPLETE` | 可生成脱敏 findings/diagnostics，但最终不得 `ACCEPTED` | optional feedback absent 必须保留 `OPTIONAL_FEEDBACK_ABSENT`；不允许 silent/unconditional acceptance |
| `INCONSISTENT` | `INVALID` | fail-closed | scope、identity、provenance、source、ordering、duplicate、unsafe evidence 或 overflow 均不可用 |
| `NOT_FOUND` | `INCOMPLETE` | fail-closed | 强制 REQUEST/RUN root 不存在；不得 fallback |

优先级冻结：

```text
source/evaluation unexpected exception -> FAILED
identity or scope mismatch             -> INVALID
INCONSISTENT / overflow                -> INVALID
NOT_FOUND / PARTIAL_WITHIN_BOUNDS      -> INCOMPLETE
unsupported replay                     -> UNSUPPORTED
remaining gate rejection               -> REJECTED
all gates pass + COMPLETE_WITHIN_BOUNDS -> ACCEPTED
```

### 5.2 Acceptance invariants

```text
evidence completeness != decision correctness
COMPLETE_WITHIN_BOUNDS != ACCEPTED
PARTIAL_WITHIN_BOUNDS != ACCEPTED
ACCEPTED != Provider authorization
ACCEPTED != NQ runtime permission
ACCEPTED != trading/execution permission
ACCEPTED != Paper/LIVE permission
```

`DecisionEvidenceReplayInternalReport` 的 accepted-state constructor invariant 必须增加：

```text
evidenceCompleteness == COMPLETE_WITHIN_BOUNDS
overflowDetected == false
boundedPolicy != null and supported
executionScope tenant == correlation tenant
decision provenance environment == executionScope environment
existing evidence/replay/regression/readiness/observability gates all pass
```

### 5.3 Bounded-policy propagation

Evolved report 必须直接保留 immutable `BoundedEvidencePolicy`，不得只保存 `COMPLETE` 或布尔值。至少保留：

```text
fromObservedAt
toObservedAt
maxFeedbackItems
policyId
policyVersion
timeWindowSemantics
orderingPolicy
overflowBehavior = FAIL_CLOSED
```

不同 bounds 或 policy version 必须在 report equality 与测试中保持可区分。不得把
`COMPLETE_WITHIN_BOUNDS` 压缩成 legacy `DecisionEvidenceStatus.COMPLETE`。

### 5.4 Failure taxonomy

优先复用现有 `DecisionFeedbackEvidenceFinding.Code` 和 `InternalAcceptanceFinding.code` 字符串，不新增第二套
failure enum。映射至少冻结为：

| Work-order semantic | Existing/required stable code | Status |
| --- | --- | --- |
| `EVIDENCE_NOT_FOUND` | `DECISION_ROOT_NOT_FOUND` | `INCOMPLETE` |
| `EVIDENCE_INCONSISTENT` | `DECISION_EVIDENCE_INVALID` 或原始 scope/correlation code | `INVALID` |
| `EVIDENCE_PARTIAL` | `OPTIONAL_FEEDBACK_ABSENT` | `INCOMPLETE` |
| `EVIDENCE_OVERFLOW` | `FEEDBACK_RESULT_LIMIT_EXCEEDED` | `INVALID` |
| `EVIDENCE_SCOPE_MISMATCH` | `TENANT_MISMATCH` / `ENVIRONMENT_MISMATCH` / `TRACE_MISMATCH` / `REQUEST_MISMATCH` / `DECISION_MISMATCH` / `RUN_MISMATCH` | `INVALID` |
| `ACCEPTANCE_POLICY_INVALID` | `ACCEPTANCE_POLICY_INVALID` | `INVALID` |
| `ACCEPTANCE_EVALUATION_FAILED` | `ACCEPTANCE_EVALUATION_FAILED` | `FAILED` |
| `INTERNAL_ACCEPTANCE_FAILURE` | `INTERNAL_ACCEPTANCE_FAILURE` | `FAILED` |

要求：

```text
unknown RuntimeException -> sanitized FAILED or fail-closed exception before report creation
no raw exception message in report/log
no fallback-to-old-evidence
no partial success masquerading as ACCEPTED
no automatic retry
no retry side effect
```

B1 实现后必须在同一 consolidated implementation task 中执行一次 security review，并把证据记录到 implementation
record；不创建独立长期 freeze 文档链。若出现 P0/P1，停止 B2/B3 并进入 blocker task。

## 6. B2 — Evolve Existing Internal Acceptance Model

### 6.1 Report shape

在现有 `DecisionEvidenceReplayInternalReport` record 上原位演进，禁止创建第二个 report。新增或替换字段后，
最终必须能表达：

```text
FeedbackExecutionScope executionScope           -> tenantId + environment
DecisionEvidenceCorrelation correlation         -> tenantId + traceId + requestId + decisionId
EvidenceCompleteness evidenceCompleteness
BoundedEvidencePolicy boundedPolicy
boolean evidenceOverflowDetected
int feedbackEvidenceCount
String evidenceAggregateRef
InternalAcceptanceStatus acceptanceStatus
List<InternalAcceptanceFinding> findings         -> stable reason codes
existing replay/regression/readiness/observability fields
existing safetyDeclaration
```

`evidenceAggregateRef` 是 safe deterministic reference，不是 content digest 或 authorization。固定组成至少包含：

```text
tenantId + environment + traceId + requestId + decisionId
+ policyId + policyVersion + fromObservedAt + toObservedAt + maxFeedbackItems
```

同 identity 但不同 bounds 必须生成不同 ref。不得把 raw feedback、raw prompt、raw provider response、credential、
完整外部响应、签名、nonce 或交易指令写入 report/ref/finding。

本阶段不新增 `evaluatedAt`。现有 replay/regression/readiness/observability 的受控时间仍保持既有语义，但不允许
引入 `Instant.now()`，也不允许时间参与未冻结的 nondeterministic equality/hash。

### 6.2 Existing fields evolution

```text
evidenceStatus:
- 来源改为 consolidatedAggregate.decisionEvidence().status()

evidenceRefs:
- 仅从 consolidatedAggregate.decisionEvidence().evidenceRefs() 投影 safe identity

missingMandatoryEvidence:
- 仅从 consolidatedAggregate.decisionEvidence().missingMandatoryEvidence() 投影

EVIDENCE findings:
- 映射 consolidatedAggregate.findings()
- 同时保留 nested decision evidence findings
- stable order + dedup rule 必须有测试
```

`InternalAcceptanceStatus` 不新增第二套 enum，也不增加任何 authorization-true 状态或 helper。

## 7. B3 — Internal Consumer / Facade and Wiring

### 7.1 Minimal facade

新增一个 usecase-owned internal facade：

```text
CLASS:
com.guidinglight.decisionhub.usecase.qdr.report.DecisionFeedbackInternalAcceptanceService

DEPENDENCIES:
DecisionFeedbackEvidenceService
DecisionEvidenceReplayReportService

METHOD:
evaluate(
  DecisionFeedbackEvidenceQuery,
  DeterministicReplayResult,
  RegressionReportView,
  ProviderReadinessEvaluationResult,
  ModelGatewayObservabilityReport
) -> DecisionEvidenceReplayInternalReport
```

固定调用链：

```text
trusted internal caller
-> DecisionFeedbackInternalAcceptanceService
-> DecisionFeedbackEvidenceService.aggregate(query)
-> DecisionEvidenceReplayReportService.generate(consolidatedAggregate, ...)
-> DecisionEvidenceReplayInternalReport
```

Facade 必须：

```text
read-only
tenant-bound
environment-bound
bounded-policy-aware
deterministic
fail-closed
zero persistence/write dependency
zero external I/O
```

Facade 不自行执行 replay/regression/readiness/observability，也不读取其 repository；这些 structured results 仍由
trusted internal caller 显式提供并由 report service 做 identity matching。任何 source/转换异常只能返回脱敏
`FAILED` report（能安全保留 query identity 时）或在 report 创建前抛出固定 fail-closed validation error；不得返回
旧 evidence 结果。

### 7.2 Spring wiring

`DecisionPipelineWiringConfig` 只新增：

```text
1 x DecisionEvidenceReplayReportService bean
1 x DecisionFeedbackInternalAcceptanceService bean
```

保留现有：

```text
1 x DecisionFeedbackEvidenceService bean
```

不得装配旧 direct-evidence acceptance facade，不得同时暴露 legacy/new acceptance path，不得新增 qualifier 形成双注入。
`DecisionPipelineWiringConfigTest` 必须断言三个 bean 各为 exactly one，并断言不存在 Controller/route/scheduler。

## 8. B4 — PostgreSQL, Security, Regression and Final Close Boundary

B4 技术验收必须完成：

```text
real PostgreSQL/Testcontainers acceptance
current Flyway V1-V15 full apply
architecture boundary tests
B1 security review evidence
affected module tests
full regression
quality
zero-write verification
```

通过后只能进入独立：

```text
DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-FINAL-CLOSE
```

Final close 按以下顺序执行，不能在 implementation task 中跳步：

```text
implementation publication
exact-SHA CI
final security/close review
current authority sync
self-contained archive packet
archive close commit
clean worktree
independent annotated tag task
local+remote tag verification
post-tag current cleanup
```

候选 tag：`dh-stage-qdr-11-close`。本工单、implementation 或 ordinary batch 均不得创建 tag。

```text
BATCH_COUNT: 4
B5: NOT_NEEDED
```

## 9. Exact implementation allowlists

### 9.1 PRODUCTION_WRITE_ALLOWLIST

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionEvidenceReplayInternalReport.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionEvidenceReplayReportService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionFeedbackInternalAcceptanceService.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

`InternalAcceptanceStatus.java`、`InternalAcceptanceFinding.java`、Stage-QDR-10 evidence types/services 和所有
Repository/JDBC 均为 read/reuse only，不在 production write allowlist。若实际编译证明必须修改它们，先停止并
返回 `STAGE_QDR_11_ACCEPTANCE_MODEL_SCOPE_BLOCKED`，不得现场扩权。

### 9.2 TEST_WRITE_ALLOWLIST

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionEvidenceReplayReportServiceTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionFeedbackInternalAcceptanceServiceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr11/StageQdr11InternalAcceptanceArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/DecisionFeedbackInternalAcceptanceFlywayPostgresTest.java
```

如需要复用 QDR-10 PostgreSQL fixture，允许从既有测试复制最小 fixture helper 到新 QDR-11 test，但禁止修改
`DecisionFeedbackEvidenceConsolidationFlywayPostgresTest.java` 或放宽既有断言。

### 9.3 FACTSOURCE_WRITE_ALLOWLIST

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md
docs/current/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md
```

Implementation task 只能对这些 factsources 做与实际结果一致的最小同步；不得预写 PASS、CI、security review、
publication、archive 或 tag 结果。

### 9.4 FORBIDDEN_PATHS

```text
dh-api/**
dh-infra/**
dh-connector/**
dh-memory/**
contracts/**
golden_cases/**
docs/gates/**
**/src/main/resources/db/migration/**
.github/workflows/**
pom.xml
**/pom.xml
NexusQuant repository/worktree
```

并禁止新增/修改任何：Controller、route、scheduler、background job、Repository/JDBC、HTTP/Provider/NQ client、
Agent/LangGraph runtime、feedback ingestion、feedback attribution write、Experience/Pheromone/FailureCase learning、
capacity harness、trading/execution/Paper/LIVE surface。

如 implementation 需要 API/Controller、migration/schema、Repository/JDBC、cross-repo runtime 或任何 forbidden
path：

```text
STAGE_QDR_11_SCOPE_EXPANSION_BLOCKED
ALLOW_SCOPE_EXPANSION_IN_CURRENT_TASK: NO
```

## 10. Test matrix

### 10.1 Evidence authority and compatibility

- Report service 的唯一 evidence parameter 是 `DecisionFeedbackEvidenceAggregate`。
- 旧 `generate(..., DecisionEvidenceAggregate, ...)` surface 不存在。
- Facade 只调用 `DecisionFeedbackEvidenceService.aggregate(...)` 一次。
- main source 无 direct legacy evidence acceptance path、无 second acceptance model/enum。

### 10.2 Completeness and failure mapping

- `COMPLETE_WITHIN_BOUNDS` + 全部 existing gates pass 才是 `ACCEPTED`。
- `COMPLETE_WITHIN_BOUNDS` + 任一 existing gate fail 不是 `ACCEPTED`。
- `PARTIAL_WITHIN_BOUNDS` 固定 `INCOMPLETE`，保留 optional feedback finding。
- `INCONSISTENT`、overflow、cross-scope、policy invalid 固定 `INVALID`。
- `NOT_FOUND` 固定 `INCOMPLETE`。
- source/evaluation unexpected exception 固定脱敏 `FAILED` 或 fail-closed validation error。
- 任一失败不得 fallback 到 legacy evidence。

### 10.3 Bounded semantics and report shape

- report 精确保留 from/to/max、policy id/version、closed interval、ordering、fail-closed overflow。
- 不同 bounds/policy version 在 report/ref 中可区分。
- executionScope tenant/environment 与 correlation/decision provenance exact match。
- feedback count/ref 不暴露 raw feedback。
- 不使用 `Instant.now()`；report equality/hash deterministic。

### 10.4 Isolation and identity

- cross-tenant denied。
- cross-environment denied。
- trace/request/decision/run mismatch denied。
- replay/regression/readiness/observability identity mismatch denied。
- decision provenance missing/ambiguous/invalid denied。

### 10.5 No side effect

```text
DB writes = 0
feedback mutation = 0
learning-store writes = 0
Provider/HTTP/NQ calls = 0
trading/execution mutation = 0
automatic retry = 0
```

### 10.6 Wiring and architecture

- `DecisionFeedbackEvidenceService`、`DecisionEvidenceReplayReportService`、
  `DecisionFeedbackInternalAcceptanceService` 各 exactly one bean。
- 无 old competing bean/path、Controller/route/scheduler/outbound client。
- qdr report/facade 不依赖 infra/api/Repository/JDBC/Spring/HTTP/Provider client/NQ/Agent/LangGraph/trading。
- 允许消费既有 provider readiness/observability safe structured value types；禁止调用 Provider。

### 10.7 PostgreSQL/Testcontainers

使用当前真实 PostgreSQL 基线并应用全部当前 Flyway migration，至少证明：

```text
persisted decision provenance + bounded V15 feedback
-> DecisionFeedbackEvidenceAggregate
-> DecisionFeedbackInternalAcceptanceService
-> DecisionEvidenceReplayInternalReport

COMPLETE_WITHIN_BOUNDS path
PARTIAL_WITHIN_BOUNDS -> INCOMPLETE
INCONSISTENT / NOT_FOUND -> fail-closed
tenant/environment/correlation isolation
overflow fail-closed
DB writes before == DB writes after
```

必须是 mandatory Testcontainers real execution；环境型 skip 不能写成 PASS。

### 10.8 Required commands

Implementation 阶段至少执行：

```powershell
mvn -B -ntp -pl dh-usecase -am "-Dtest=DecisionEvidenceReplayReportServiceTest,DecisionFeedbackInternalAcceptanceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -B -ntp -pl dh-app -am "-Dtest=DecisionPipelineWiringConfigTest,StageQdr11InternalAcceptanceArchitectureTest,DecisionFeedbackInternalAcceptanceFlywayPostgresTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -B -ntp test
mvn -B -ntp -Pquality validate
```

验收必须是：

```text
19 / 19 Reactor SUCCESS
failures / errors / skipped = 0 / 0 / 0
mandatory PostgreSQL/Testcontainers = REAL EXECUTION
Checkstyle = 0
Spotless = PASS
```

## 11. Documentation-task write boundary

### 11.1 Current task result boundary

本轮只允许编制工单和同步 current authority。禁止生产代码、测试、archive、tag 或任何外部系统写入。

### 11.2 Current task validation

```text
only allowlisted docs changed
unexpected files = 0
technical diff = 0
archive diff = 0
scope invariants = 3 / 3
current conflicts = 0
mvn -B -ntp -Pquality validate = REQUIRED
full tests = NOT_RERUN / WORK_ORDER_ONLY
```

Stage-QDR-10 的 `1326 / 0 / 0 / 0`、PostgreSQL `17.10`、implementation CI `31297296670`、close CI
`31297913196`、cleanup CI `31298387436` 只作为已接受历史证据复用，不得写成本轮执行结果。

### 11.3 DOCS_TASK_WRITE_ALLOWLIST

```text
docs/current/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

`docs/current/FACTSOURCE_POLICY.md`、`docs/current/ARCHIVE_INDEX.md` 不修改：本工单没有改变 authority policy 或
archive state。所有 production/test/archive/tag 路径禁止修改。

## 12. Review triggers and blocker routes

仅以下情况触发 standalone review/blocker：

```text
B1 security semantics review
migration/schema need
API/Controller need
Repository/JDBC expansion
P0/P1
cross-repo runtime
stage final close
```

B2/B3 普通实现不 standalone review，除非触发上述条件。

```text
existing acceptance cannot safely evolve:
-> DH-STAGE-QDR-11-ACCEPTANCE-MODEL-BLOCKER

API/migration/Repository expansion required:
-> DH-STAGE-QDR-11-SCOPE-EXPANSION-BLOCKER

duplicate production-wired consolidated acceptance discovered:
-> STAGE_QDR_11_DUPLICATE_CAPABILITY_BLOCKED
```

## 13. Rollback

Implementation 按 B1、B2/B3、validation/close evidence 的可审查 commit 边界执行。回滚只撤销
Stage-QDR-11 的 report/service/facade/wiring/test/current-doc changes；不修改或删除 Stage-QDR-10 aggregate、
V15 data、migration、archive、close tag 或历史 evidence。无 DB/schema 变更，因此不需要数据回滚。

停止条件：

```text
dual evidence authority cannot be removed
existing report cannot carry bounded semantics without second model
unexpected production caller requires legacy overload
API/migration/Repository/JDBC becomes necessary
cross-tenant/environment identity cannot be proven
zero-write property cannot be proven
P0/P1 remains open
```

## 14. Readiness decision

```text
STAGE_QDR_11_IMPLEMENTATION_WORK_ORDER: DONE
SINGLE_EVIDENCE_AUTHORITY: FROZEN
EXISTING_ACCEPTANCE_MODEL_REUSE: YES
COMPLETENESS_MAPPING: FROZEN
BOUNDED_POLICY_PROPAGATION: FROZEN
DUAL_AUTHORITY_RISK: CLOSED_BY_DESIGN / IMPLEMENTATION_TEST_PENDING
MIGRATION_REQUIRED: NO
API_CHANGE_REQUIRED: NO
REPOSITORY_EXPANSION_REQUIRED: NO

ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO / THIS TASK IS DOCS ONLY
ALLOW_CAPACITY_EXECUTION: NO
ALLOW_REFERENCE_LIVENESS: NO
ALLOW_RETENTION: NO
ALLOW_FEEDBACK_LEARNING: NO
ALLOW_NQ_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 15. Next concrete action

工单 validation 与本地提交通过后，下一任务唯一为：

```text
DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-CONSOLIDATED-IMPLEMENTATION
```

该授权不在本轮生效；必须从 clean worktree、精确 work-order commit 与本工单 allowlist 重新预检。

建议 implementation commit message：

```text
feat(qdr): integrate consolidated evidence into internal acceptance
```

本工单建议 commit message：

```text
docs(qdr): define stage-qdr-11 internal acceptance work order
```

## 16. Work-order validation record

```text
git status --short (pre-commit): PASS / 10 ALLOWLISTED DOCS CHANGED
git diff --check: PASS
unexpected files: 0
technical diff: 0
archive diff: 0
scope invariants: PASS / 3 OF 3
current factsources / conflicts: PASS / 8 OF 8 / 1 BLOCK HASH / 0 CONFLICTS
mvn -B -ntp -Pquality validate: PASS / 19 OF 19 REACTOR SUCCESS
Checkstyle: 0 VIOLATIONS
Spotless: PASS
full tests: NOT_RERUN / WORK_ORDER_ONLY
capacity: NOT_EXECUTED
local commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
push / tag: NOT_EXECUTED / NOT_EXECUTED
```
