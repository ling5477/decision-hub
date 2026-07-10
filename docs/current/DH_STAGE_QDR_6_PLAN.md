# DH Stage-QDR-6 Plan

## 1. 任务定义

```text
task: DH-STAGE-QDR-6-PLAN
classification: PLANNING_ONLY
stage name: Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
中文定义: 决策流水线统一证据链与确定性重放基线
plan status: DONE
implementation status: NOT_STARTED
next action: DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER
```

本计划只冻结 Stage-QDR-6 的范围、批次、验收、测试矩阵、review 触发条件、回滚策略与 close 纪律。本轮未实现 domain/usecase、JDBC、API、migration、测试或 runtime。

## 2. 前置事实与代码现实

### 2.1 Stage 前置事实

```text
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_TAG: dh-stage-qdr-4-close
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_TAG: dh-stage-qdr-5-close
STAGE_QDR_5_CURRENT_CLEANUP: DONE
STAGE_QDR_6_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
```

QDR-5 tag 已在本地和远程验证；QDR-5 archive packet 包含 `PLAN.md`、`IMPLEMENTATION_WORK_ORDER.md`、`BATCH_SUMMARY.md`、`VALIDATION_EVIDENCE.md`、`FINAL_CLOSE_REVIEW.md`、`ARCHIVE_CLOSE.md` 与 `STATUS_SNAPSHOT.md`。`docs/current` 无 `DH_STAGE_QDR_5*.md` 残留。

### 2.2 已存在的事实链

真实代码已经具备以下分散能力：

```text
Decision Pipeline:
  DecisionOrchestrator / DefaultDecisionOrchestrator
  DecisionDryRunController / DefaultDecisionDryRunService
  DecisionPipelineWiringConfig / DecisionDryRunRuntimeWiringConfig

V5 audit chain:
  dh_decision_request
  dh_decision_context_snapshot
  dh_decision_trace_step
  dh_decision_provider_call_log
  dh_decision_output
  dh_decision_audit_event

V6 QDR decision core:
  decision_request -> decision_run
  decision_request -> quant_signal
  decision_run -> quant_decision

V8 model gateway evidence:
  prompt/model version safe metadata
  qdr_model_gateway_call safe refs / hashes / redacted summaries

V9 replay/evaluation/regression:
  qdr_replay_case
  qdr_evaluation_case
  qdr_expected_decision_summary
  qdr_regression_verdict
  qdr_regression_finding
  qdr_replay_input_ref / qdr_replay_output_ref

Stage-QDR-5 internal evidence:
  ProviderHealthReadModelService
  ProviderReadinessGuardService
  ObservabilityReportService
  ModelGatewayObservabilityReport
  StageQdr5AcceptanceEvidence
```

### 2.3 关联关系与缺口

当前关联不是单一 ID 模型：

- V5 `decisionId` 是字符串，当前由外部 `requestId` 派生；V5 request/output/audit/trace 通过 `tenantId + traceId + requestId + decisionId` 对账。
- V6 `decision_request.id` 与 `decision_run.id` 是内部 UUID；dry-run 使用 tenant/request 派生稳定 UUID，并通过外部 `requestId`、`traceId`、`tenantId` 与 V5 对账。
- V8 gateway call 绑定 `tenantId + traceId + requestId + decisionRunId + provider/profile/version refs`。
- V9 replay/evaluation/regression 绑定 `tenantId + caseId + evaluationId`，并保留 `sourceDecisionId + sourceRequestId + traceId + requestId`；V9 未对 V5/V6 source id 建强 FK。
- Stage-QDR-5 provider health/readiness/observability 主要使用 `tenantId + traceId + sourceRequestId + providerRef + modelGatewayVersionRef`，未形成统一 decision evidence aggregate。

因此 Stage-QDR-6 的统一 correlation 必须以 `tenantId` 为强边界，并要求 `traceId + requestId + decisionId` 同时通过一致性校验；`runId`、`caseId`、`evaluationId`、`verdictId`、`providerRef` 是下游引用，不得代替 tenant-bound 主关联。

### 2.4 已确认不存在的能力

```text
DecisionEvidenceAggregate or equivalent unified aggregate: ABSENT
DecisionEvidenceQuery or equivalent unified query: ABSENT
evidence completeness / correlation evaluator: ABSENT
deterministic replay executor from persisted snapshot: ABSENT
replay reproducibility status / execution hash result: ABSENT
evidence-only internal acceptance aggregate: ABSENT
evidence-only REST API: ABSENT
```

现有 `MockGatewayRegressionCaseBuilder` 只做 deterministic ID/hash/ref 计算，`QdrRegressionComparator` 只比较调用方提供的 structured summary/version/hash；它们不是从持久化 snapshot 恢复输入并执行的 `DeterministicReplayExecutor`。V9 Repository/JDBC 与 regression/readiness/report services 已存在，但 QDR replay JDBC adapters、regression service、provider health/readiness/report service 未形成统一 production wiring。

## 3. Stage 主线与非目标

Stage-QDR-6 冻结为两个不可分离的方向：

1. 统一决策证据链：将 request、run、snapshot、output、audit、trace、QDR、provider readiness、observability 与内部 acceptance 组合为 tenant-bound、可查询、可验证的内部 aggregate。
2. 确定性重放基线：仅从已经持久化且经过安全校验的 snapshot、safe refs、version refs、hash 与 structured summary 构造 mock-only replay evidence。

本 Stage 不处理：

```text
real HTTP / real provider / Provider SDK
NQ runtime integration / NQ DB access / NQ mutation
Agent / LangGraph / Python Agent runtime
credential access
raw prompt / raw provider response persistence
order / execution / account / ledger / risk mutation
Paper Run / LIVE
BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER output
provider authorization / trading permission / LIVE permission
```

## 4. 统一 evidence correlation 合同

### 4.1 建议对象

```text
DecisionEvidenceQuery
DecisionEvidenceAggregate
DecisionEvidenceRef
DecisionEvidenceStatus
DecisionEvidenceCompleteness
DecisionEvidenceFinding
DecisionEvidenceCorrelation
DecisionEvidencePolicy
```

### 4.2 Correlation 规则

```text
mandatory boundary: tenantId
mandatory primary correlation: traceId + requestId + decisionId
optional decision-core ref: decisionRequestId + runId
downstream refs: caseId + evaluationId + verdictId + providerRef
```

规则：

- `tenantId` 缺失、任一证据 tenant 不一致或 repository 返回跨租户对象时立即 fail-closed。
- `traceId`、`requestId`、`decisionId` 缺失或相互冲突时为 `INVALID_CORRELATION`，不得降级为 complete。
- V5 `decisionId=requestId` 的当前实现只能作为已验证映射，不得升级为永久 schema 假设。
- V6 UUID `decision_request.id` / `decision_run.id` 必须通过 tenant-bound request row 回链外部 `requestId/traceId`。
- V9 `sourceDecisionId/sourceRequestId` 只作为 safe source refs；无强 FK 时必须由 consistency evaluator 校验。
- provider evidence 必须同时匹配 tenant、trace/request 与允许的 provider/version safe refs。

### 4.3 Evidence 类型

```text
REQUEST
RUN
CONTEXT_SNAPSHOT
DECISION_OUTPUT
AUDIT_EVENT
TRACE_STEP
QDR_REPLAY_CASE
QDR_EVALUATION_CASE
REGRESSION_VERDICT
REGRESSION_FINDING
PROVIDER_HEALTH
PROVIDER_READINESS
OBSERVABILITY_REPORT
INTERNAL_ACCEPTANCE_RESULT
```

### 4.4 完整度与 fail-closed

建议状态：

```text
COMPLETE
INCOMPLETE
INVALID
NOT_FOUND
DENIED
FAILED
```

`COMPLETE` 只能在 mandatory evidence、correlation、tenant、safe-ref 与 version 校验全部通过后产生。optional evidence 缺失可以保留 `INCOMPLETE` finding，但不得伪造成 complete。raw/secret-like ref、可执行交易词、跨租户证据或无法证明来源的证据必须使 aggregate 为 `INVALID`、`DENIED` 或 `FAILED`。

## 5. 确定性重放安全基线

### 5.1 建议对象

```text
DeterministicReplayExecutor
DeterministicReplayCommand
DeterministicReplayResult
ReplayInputSnapshot
ReplayExecutionHash
ReplayDifference
ReplayReproducibilityStatus
```

### 5.2 输入边界

允许输入：

```text
persisted structured context snapshot
safe evidence refs and SHA-256 hashes
policyVersion
modelVersionRef / modelGatewayVersionRef
promptVersionRef only as safe version ref
expected/actual structured decision summary
forbidden action set
mock-only algorithm/version identity
```

禁止输入：

```text
raw prompt
raw provider response
credential / token / cookie / api key / secret / passphrase
real provider client or network response
NQ data access or mutation result
executable order/trading payload
```

### 5.3 可重复性规则

- canonical serialization 必须固定字段顺序、空值语义、时间表示、数值精度和 UTF-8 编码。
- hash 输入必须包括 tenant-bound correlation、snapshot hash、policy version、model/gateway version refs、algorithm version 与 expected summary hash。
- 相同 snapshot、相同版本、相同 policy、相同 algorithm 必须产生相同 `ReplayExecutionHash`。
- 版本或 summary 变化必须产生结构化 `ReplayDifference`，不得静默覆盖 baseline。
- executor 只生成 replay/evaluation evidence，不能调用 provider、HTTP、NQ、Agent 或交易模块。
- 缺失 snapshot、安全引用不完整、版本不确定、cross-tenant 或 persistence failure 必须返回 structured `FAIL`。

### 5.4 输入充分性 blocker

当前 V5/V6 存在 structured context snapshot/payload，V8/V9 存在 safe refs/hash/version fields，但尚无统一 immutable `ReplayInputSnapshot` 读取合同。B3 implementation 前必须由 implementation work order 明确：

1. 哪个持久化记录是 canonical snapshot source。
2. 如何证明该 snapshot 与 `tenantId + traceId + requestId + decisionId/runId` 一致。
3. 哪些字段参与 canonical hash。
4. 现有 V5/V6/V8/V9 字段是否足够且无需 migration。

若无法只用现有持久化字段证明输入完整性，B3 必须返回 `DETERMINISTIC_REPLAY_INPUT_INSUFFICIENT_BLOCKED`，不得虚构 executor 可实现性；如需 migration，停止普通 batch 并进入 migration/schema review。

## 6. 批次设计

### B1: Evidence Correlation / Aggregate Contracts

目标：冻结 correlation、aggregate、ref、status、completeness、finding 与 fail-closed policy。

```text
scope: domain/usecase contracts only
default repository change: NO
default API change: NO
default migration: NO
review: ordinary batch unless tenant/correlation or security semantics change
```

### B2: Evidence Aggregation Service

目标：组合现有 V5/V6/V8/V9 decision、audit、trace、snapshot、QDR、provider readiness 与 observability read models。

```text
scope: internal tenant-bound aggregation service
preferred source: existing repository/read-model ports
missing mandatory evidence: fail-closed
cross-tenant result: empty or denied
trading/provider authorization side effect: forbidden
```

若只复用现有 port，B2 为普通 batch。若现有 `DecisionReadModelQueryPort`、`DecisionReplayQueryRepository`、V9 repositories 与 `ModelGatewayCallPersistencePort` 无法提供所需读模型，必须停止并输出 `STAGE_QDR_6_REPOSITORY_EXPANSION_REVIEW_REQUIRED`；不得在普通 B2 中直接新增 production repository/JDBC。

### B3: Deterministic Replay Baseline

目标：从 B2 已验证 aggregate/snapshot 构造 mock-only deterministic replay，生成 execution hash、difference 与 reproducibility status。

```text
implementation precondition: DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER accepted
provider call: impossible
HTTP call: impossible
NQ access: impossible
Agent/LangGraph: impossible
output: replay/evaluation evidence only
```

B3 是重要行为边界。implementation 前必须由 stage implementation work order 冻结输入充分性、canonicalization、hash algorithm/version、failure taxonomy 与 architecture guard；若涉及 API 或 migration，分别进入独立 review。

### B4: Evidence / Replay Internal Report

目标：生成内部 read model 或 acceptance report，表达 evidence completeness、reproducibility 与 regression result。

```text
default: internal-only
default API: NO
default migration: NO
acceptance != provider authorization
acceptance != NQ integration permission
acceptance != trading permission
acceptance != LIVE permission
```

如需新增 Controller/REST endpoint，立即停止并进入 `STAGE_QDR_6_API_REVIEW_REQUIRED`。

### B5: Stage-QDR-6 Final Close

```text
B1-B4 accepted
final close review PASS
self-contained archive packet complete
close docs commit
archive close commit
clean worktree
annotated tag
push tag
post-tag current cleanup
then and only then Stage-QDR-7 planning
```

## 7. Review 触发规则

```text
B1: ordinary batch by default
B2 using existing ports only: ordinary batch
B2 requiring production repository/JDBC expansion: standalone review
B3: implementation work order required before implementation
B3 requiring migration/API: separate migration/API review
B4 internal read model/report only: ordinary batch
B4 requiring API: stop and API review
B5: final close review required
```

独立 review 触发条件：

1. migration / table / index / constraint 变化。
2. API / Controller / REST endpoint 变化。
3. production Repository/JDBC expansion。
4. security boundary 变化。
5. tenant/correlation 语义变化。
6. deterministic replay execution boundary 变化。
7. P0/P1 blocker。
8. stage final close。

## 8. 测试矩阵设计

本节只设计测试，本轮不实现。

### 8.1 Evidence aggregate

1. valid complete evidence aggregate。
2. missing tenantId fails closed。
3. missing traceId/requestId/decisionId correlation fails closed。
4. cross-tenant query returns empty or denied。
5. decision and QDR tenant mismatch fails closed。
6. provider readiness belongs to another tenant fails closed。
7. missing mandatory audit evidence marks aggregate incomplete。
8. optional evidence absence does not fabricate complete。
9. raw prompt ref is rejected。
10. raw provider response ref is rejected。
11. credential-like evidence key is rejected。
12. trading instruction evidence is rejected。

### 8.2 Deterministic replay

1. same snapshot + same versions produces same hash。
2. changed modelGatewayVersionRef produces explicit diff。
3. changed policyVersion produces explicit diff。
4. changed expected summary produces explicit diff。
5. missing snapshot fails closed。
6. incomplete safe refs fail closed。
7. cross-tenant replay is denied。
8. raw prompt dependency is rejected。
9. raw provider response dependency is rejected。
10. real Provider call is impossible。
11. real HTTP call is impossible。
12. NQ mutation is impossible。
13. BUY/SELL/MARKET_ORDER result is rejected。
14. timeout/exception returns structured FAIL。
15. persistence/read-model failure returns structured FAIL。

### 8.3 Architecture guards

1. no LangGraph dependency。
2. no Agent runtime。
3. no Provider SDK。
4. no HTTP client。
5. no NQ repository access。
6. no trading/execution dependency。
7. module dependency direction remains valid。

### 8.4 Implementation 验证门槛

```text
targeted unit tests: required
tenant isolation tests: required
repository/read-model integration tests: required if persistence adapter is reused or expanded
architecture source/dependency guards: required
mvn -ntp test: required before batch close
mvn -ntp -Pquality validate: required
Docker/Testcontainers: only PASS when actually executed with Docker
```

## 9. 回滚策略

- 规划阶段：若本计划被后续事实否定，先将 `STAGE_QDR_6_PLAN` 标为 `BLOCKED`，不启动 implementation；回滚仅限本轮 docs diff。
- B1/B2/B3/B4：每批次独立 commit；未提交时仅回滚该批次 allowed files，已提交时使用独立 `git revert <commit>`，不得重写历史。
- 默认无 migration、无 API，因此普通回滚不涉及 schema 或 endpoint compatibility。
- 若独立 review 后允许 migration，必须另写 forward-fix/rollback migration 策略；不得修改 V1-V9。
- 若独立 review 后允许 API，必须另写 compatibility、feature flag、kill switch 与 endpoint removal 方案。
- 任一 batch 回滚不得删除历史 audit/evidence 或把失败状态改写为 PASS。

## 10. Stage close 与归档纪律

Stage close 只有在 B1-B4 实现和验证真实完成后才能进入。归档 packet 至少包括：

```text
README.md
PLAN.md
IMPLEMENTATION_WORK_ORDER.md
BATCH_SUMMARY.md
VALIDATION_EVIDENCE.md
FINAL_CLOSE_REVIEW.md
ARCHIVE_CLOSE.md
STATUS_SNAPSHOT.md
SECURITY_BOUNDARY_REVIEW.md when triggered
```

顺序固定：`final close review PASS -> final close docs commit -> archive packet/close commit -> clean worktree -> annotated tag -> push tag -> post-tag current cleanup`。本计划不创建 tag、不 push，也不预授权 future tag。

## 11. 后续阶段边界

### Stage-QDR-7 候选

`Limited Dry Run Runtime Readiness / NOT_STARTED`：persistent multi-instance rate limit、formal dry-run contract、idempotency、replay guard、kill switch、feature flag、timeout/retry/circuit breaker、protected read-only runtime entry、cross-repo contract tests。

Stage-QDR-6 不实现以上内容。

### Stage-QDR-8 候选

`Agent Runtime Contract Baseline / NOT_STARTED`：Agent input/output schema、tool registry、least-privilege permission、checkpoint/resume、human-in-the-loop、LangGraph mock-only PoC。

Stage-QDR-8 只有在 Stage-QDR-7 完成后才能重新评估。当前禁止创建 Python Agent Runtime 或引入 LangGraph。

## 12. Readiness decision

```text
STAGE_QDR_6_PLAN: DONE
STAGE_QDR_6_MAINLINE: DECISION_PIPELINE_EVIDENCE_CONSOLIDATION
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
ALLOW_EVIDENCE_CONSOLIDATION_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

下一步唯一入口：

```text
DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER
```
