# DH Stage-QDR-6 Implementation Work Order

## 1. 工单状态

```text
task: DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER
classification: WORK_ORDER_ONLY
stage: Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
work order status: DONE / WORK_ORDER_ONLY
stage implementation status: B1_DONE / B2_DONE / B3_PERSISTENCE_DESIGN_FROZEN
current implementation authorization: NONE / WORK_ORDER_ONLY_NEXT
next action: DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER
```

本工单只冻结 Stage-QDR-6 B1-B5 的实现边界、对象职责、依赖方向、测试矩阵、review 触发条件、回滚规则和 close 条件。本轮未修改生产代码、测试、migration、API、Controller、Repository/JDBC 或 runtime wiring。

## 2. 开工前事实

```text
repository: decision-hub
branch: dev
worktree at start: CLEAN
HEAD: 5108f24 docs(qdr): plan stage-qdr-6 evidence consolidation baseline
HEAD contains DH_STAGE_QDR_6_PLAN.md: YES
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED_BY_THIS_TASK
```

若后续批次开工时分支、工作区、current factsources 或代码现实变化，必须重新执行 preflight，不得直接沿用本工单中的历史快照。

## 3. 真实代码复核与复用边界

### 3.1 已存在且必须复用的模型

```text
domain.decision.DecisionEvidence
domain.decision.DecisionReplayView
domain.decision.DecisionReplayRequestView
domain.decision.DecisionReplayContextView
domain.decision.DecisionReplayOutputView
domain.decision.DecisionReplayTimelineView
usecase.qdr.readmodel.DecisionEvidenceView
usecase.qdr.readmodel.DecisionRunDetailView
usecase.qdr.readmodel.DecisionTraceTimelineView
usecase.qdr.replay.ReplayCaseRecord
usecase.qdr.replay.EvaluationCaseRecord
usecase.qdr.replay.RegressionVerdictRecord
usecase.qdr.gateway.ModelGatewayCallRecord
usecase.qdr.gateway.ModelGatewayObservabilityReport
```

不得复制或重命名以上对象形成第二套 request/run/replay/provider 模型。Stage-QDR-6 新对象只负责统一 correlation、aggregate、deterministic replay input/result 与 internal report，不替换既有 source model。

### 3.2 已存在且必须优先复用的 ports/read models

```text
DecisionReplayQueryRepository
DecisionReadModelQueryPort
ReplayCaseRepository
EvaluationCaseRepository
RegressionVerdictRepository
ModelGatewayCallPersistencePort
ProviderHealthReadModelService
ProviderReadinessGuardService
ObservabilityReportService
```

当前 `DecisionReplayQueryRepository` 已能读取 V5 request、完整结构化 context snapshot、output、trace、provider call 与 audit event；`DecisionReadModelQueryPort` 已提供 V6 decision run detail、trace 和 evidence refs；V9 repositories 均为 tenant-bound；V8 gateway call record 已保存 safe refs、hash、version identity 与 redacted summary。

### 3.3 当前缺口

```text
unified DecisionEvidenceAggregate: ABSENT
unified correlation policy/evaluator: ABSENT
cross-source aggregation service: ABSENT
canonical ReplayInputSnapshot: ABSENT
deterministic replay executor: ABSENT
evidence/replay internal report: ABSENT
QDR replay/readiness unified production wiring: ABSENT
evidence REST API: ABSENT
```

缺口不授权新增 Repository、SQL、migration 或 API。B2 只能先使用现有 ports；现有 JDBC adapter 的 bean wiring 可在后续 B2 scope 中单独列出，但不得新增 query SQL、JDBC adapter 或 repository contract。

## 4. 全局实现边界

### 4.1 允许

```text
DH internal domain/usecase contracts
tenant-bound read-only aggregation
existing port/read model composition
pure deterministic canonicalization/hash/diff
mock-only replay evidence
internal report and acceptance evidence
unit/integration/architecture regression tests
minimal docs/current synchronization
```

### 4.2 禁止

```text
new migration or modification of V1-V9
new API / Controller / REST endpoint / OpenAPI path
new production Repository / JDBC adapter / SQL query
real HTTP / real provider / Provider SDK
NQ repository / NQ DB / NQ mutation / NQ runtime integration
Agent / LangGraph / Python Agent runtime
credential access or persistence
raw prompt / raw provider response persistence or replay dependency
order / execution / account / ledger / risk mutation
Paper Run / LIVE
BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER result
provider authorization / trading permission / LIVE permission
```

## 5. 批次依赖关系

```text
B1 Evidence Contracts
  -> B2 Evidence Aggregation Service
    -> B3 Deterministic Replay Baseline
      -> B4 Evidence / Replay Internal Report
        -> B5 Final Close
```

- B1 未完成并验证前，不允许 B2。
- B2 未产生 tenant-bound aggregate 与 replay-ready input evidence 前，不允许 B3。
- B3 未产生 reproducibility result 与 structured differences 前，不允许 B4。
- B1-B4 未全部完成、测试与边界验证前，不允许 B5。
- 每批次必须独立 commit；普通 batch 不增加 standalone review。

## 6. B1：Evidence Correlation / Aggregate Contracts

### 6.1 目标

建立统一但不重复既有模型的 evidence contract，冻结查询键、correlation、safe ref、状态、完整度、finding 与 policy。

### 6.2 包与文件边界

未来 B1 默认只允许：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/**
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
README.md
docs/current/** 的最小状态、测试与 worklog 同步
```

默认不修改 `dh-domain`。如实现发现必须改变现有 domain contract，停止 B1 并按 tenant/correlation/security boundary change 进入 review，不得复制模型规避 review。

### 6.3 对象职责

#### DecisionEvidenceQuery

```text
mandatory: tenantId, traceId, requestId, decisionId
optional selectors: decisionRunId, caseId, evaluationId, verdictId, providerRef
purpose/profile: CORE_DECISION / DETERMINISTIC_REPLAY / INTERNAL_ACCEPTANCE
```

- 所有 mandatory key 必须 non-null、non-blank、safe-text。
- 不接受 tenantless、decisionId-only、UUID-only 查询。
- optional selector 只能缩小当前 tenant 范围，不能替代 primary correlation。

#### DecisionEvidenceCorrelation

```text
primary: tenantId + traceId + requestId + decisionId
decision core refs: decisionRequestId + decisionRunId
QDR refs: caseId + evaluationId + verdictId
provider refs: providerRef + modelVersionRef + modelGatewayVersionRef + promptVersionRef
```

- V5 `decisionId=requestId` 只能作为当前实现映射校验，不得写成永久 schema invariant。
- V6 UUID 必须通过 tenant-bound request/run view 回链外部 `requestId/traceId`。
- V9 `sourceDecisionId/sourceRequestId` 必须与 primary correlation 一致。

#### DecisionEvidenceRef

这是 Stage-QDR-6 aggregate 的 typed safe-ref adapter，不替代既有 `DecisionEvidence` 或 `DecisionEvidenceView`。

```text
fields: evidenceType, refId, contentHash?, sourceType, mandatory, redactionStatus
evidence types:
  REQUEST / RUN / CONTEXT_SNAPSHOT / DECISION_OUTPUT / AUDIT_EVENT / TRACE_STEP
  QDR_REPLAY_CASE / QDR_EVALUATION_CASE / REGRESSION_VERDICT / REGRESSION_FINDING
  PROVIDER_HEALTH / PROVIDER_READINESS / OBSERVABILITY_REPORT / INTERNAL_ACCEPTANCE_RESULT
```

- ref 不承载 raw payload。
- hash 存在时必须是 lowercase SHA-256 hex。
- raw/secret/executable-trading key 或 value 必须拒绝。

#### DecisionEvidenceStatus

```text
COMPLETE
INCOMPLETE
INVALID
NOT_FOUND
DENIED
FAILED
```

#### DecisionEvidenceCompleteness

按 profile 计算 mandatory evidence 覆盖率，不接受调用方随意降低 required set：

```text
CORE_DECISION:
  REQUEST + RUN + CONTEXT_SNAPSHOT + DECISION_OUTPUT + AUDIT_EVENT + TRACE_STEP

DETERMINISTIC_REPLAY:
  CORE_DECISION + QDR_REPLAY_CASE + QDR_EVALUATION_CASE
  + policy/model/gateway version refs + expected summary + safe input hash

INTERNAL_ACCEPTANCE:
  DETERMINISTIC_REPLAY + REGRESSION_VERDICT
  + PROVIDER_HEALTH + PROVIDER_READINESS + OBSERVABILITY_REPORT
  + INTERNAL_ACCEPTANCE_RESULT
```

`REGRESSION_FINDING` 可以为空，但 verdict 为 FAIL/WARN 时 finding 或 failure reason 必须存在。optional evidence 缺失不得伪造为 mandatory evidence present。

#### DecisionEvidenceFinding

```text
fields: code, severity, evidenceType?, safeRef?, message
severity: INFO / WARN / ERROR / BLOCKER
```

固定最小 code：

```text
TENANT_REQUIRED
CORRELATION_KEY_REQUIRED
CORRELATION_MISMATCH
TENANT_MISMATCH
MANDATORY_EVIDENCE_MISSING
EVIDENCE_REF_INVALID
UNSAFE_EVIDENCE_REJECTED
SOURCE_READ_FAILED
```

#### DecisionEvidencePolicy

- 定义三个固定 completeness profiles。
- 负责 safe-ref/redaction/trading-term guard。
- 负责 status/completeness 判定，不读取 Repository，不执行 IO。
- 任一异常、跨 tenant 或 mandatory mismatch 必须 fail-closed。

#### DecisionEvidenceAggregate

```text
fields:
  query/profile
  correlation
  status
  completeness
  evidenceRefs
  findings
```

Aggregate 不携带 raw provider/request/response 内容，不携带 credential，不含 execution authorization。`COMPLETE` 只表示所选内部 profile 的 evidence 完整。

### 6.4 B1 测试矩阵

1. valid CORE_DECISION aggregate contract 可构造。
2. missing tenantId fails closed。
3. missing traceId fails closed。
4. missing requestId fails closed。
5. missing decisionId fails closed。
6. blank/unsafe correlation key fails closed。
7. V5/V6/V9 correlation mismatch 产生 `CORRELATION_MISMATCH`。
8. cross-tenant ref 产生 `TENANT_MISMATCH`。
9. mandatory evidence 缺失不能输出 `COMPLETE`。
10. optional evidence 缺失不伪造 mandatory ref。
11. duplicate refs deterministic 去重或拒绝。
12. invalid SHA-256 ref 拒绝。
13. raw prompt ref 拒绝。
14. raw provider response ref 拒绝。
15. credential-like ref 拒绝。
16. trading/execution ref 拒绝。
17. 现有 `DecisionEvidence`、`DecisionEvidenceView`、`DecisionReplayView` 未被复制或替换。
18. `dh-usecase` 不新增 infra/api/provider/NQ 依赖。

### 6.5 B1 完成条件

```text
contracts implemented
targeted B1 tests PASS
architecture guard PASS
mvn -ntp test PASS
mvn -ntp -Pquality validate PASS
forbidden-scope scan PASS
minimal current docs synced
independent B1 commit created
```

## 7. B2：Evidence Aggregation Service

### 7.1 目标

组合现有 V5/V6/V8/V9 ports/read models，输出 tenant-bound `DecisionEvidenceAggregate`，不引入新 persistence boundary。

### 7.2 计划对象

```text
DecisionEvidenceAggregationService
DecisionEvidenceCorrelationResolver
DecisionEvidenceConsistencyEvaluator
```

不得新增第二套 `DecisionEvidenceReaderPort`，除非后续 repository expansion review 明确允许。Service 默认直接组合现有只读 ports，并将 source record 转换为 B1 safe refs/findings。

### 7.3 Source 映射

```text
V5 DecisionReplayQueryRepository:
  request + context snapshot + output + trace + provider-call + audit event

V6 DecisionReadModelQueryPort:
  request/run correlation + decision detail + trace + evidence refs

V9 ReplayCaseRepository / EvaluationCaseRepository / RegressionVerdictRepository:
  replay/evaluation/verdict/finding records

V8 ModelGatewayCallPersistencePort:
  gateway call safe metadata/hash/version refs

Stage-QDR-5 services:
  provider health + readiness + observability + acceptance evidence
```

### 7.4 聚合顺序

1. 校验 `DecisionEvidenceQuery` 和 profile。
2. 使用 tenant-bound V5/V6 结果建立 primary correlation。
3. correlation 不一致立即返回 `INVALID/DENIED`，不继续下游查询。
4. 按 optional selectors 或 primary correlation 查询 V9 evidence。
5. 仅在已获得 safe `modelCallRef/providerRef` 后查询 V8 gateway metadata。
6. 使用既有 Stage-QDR-5 services 生成内部 provider evidence。
7. consistency evaluator 计算 completeness、findings 与最终 status。
8. source exception 统一映射为 `FAILED + SOURCE_READ_FAILED`。

### 7.5 Repository/JDBC blocker

当前代码现实允许优先复用现有 ports；因此本工单不触发 B2 blocker。B2 开工前仍必须做 scoped sufficiency audit：

```text
new SQL required: B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
new JDBC adapter required: B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
new repository read method required: B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
schema/migration required: B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
```

仅把已经存在的 JDBC adapters 注册为 Spring beans，不等同于新增 reader；但必须限定为 wiring-only，不能夹带 SQL、method 或 schema 扩展，并补 wiring test。

### 7.6 B2 测试矩阵

1. existing-port happy path 输出 CORE `COMPLETE`。
2. cross-tenant V5 result 返回 empty/denied。
3. cross-tenant V6 result 返回 empty/denied。
4. V5/V6 requestId、traceId 或 decision mapping 冲突 fail-closed。
5. V9 sourceDecisionId/sourceRequestId mismatch fail-closed。
6. provider evidence tenant mismatch fail-closed。
7. provider trace/request mismatch fail-closed。
8. mandatory audit event 缺失为 `INCOMPLETE`。
9. mandatory trace 缺失为 `INCOMPLETE`。
10. optional finding 缺失不影响合法 PASS verdict。
11. Repository 返回 null/异常为 structured `FAILED`。
12. unsafe source ref 在投影时拒绝。
13. page size/selector 不能导致 tenantless scan。
14. existing adapter wiring test 通过（仅当添加 wiring）。
15. source scan 证明没有新 Repository/JDBC/SQL/migration/API。

### 7.7 B2 进入条件

```text
B1 implementation and tests: DONE
B1 commit: PRESENT
worktree: CLEAN
repository sufficiency audit: PASS
B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED: NOT_TRIGGERED
```

## 8. B3：Deterministic Replay Baseline

### 8.1 目标

将 B2 已验证的 persisted evidence 转换为 canonical `ReplayInputSnapshot`，使用纯本地、mock-only、无 IO executor 计算 execution hash、reproducibility status 与 structured differences。

### 8.2 包与对象

未来默认放置在：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/deterministic/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/deterministic/**
```

对象：

```text
ReplayInputSnapshot
DeterministicReplayCommand
DeterministicReplayExecutor
DeterministicReplayResult
ReplayExecutionHash
ReplayDifference
ReplayDifferenceType
ReplayReproducibilityStatus
```

### 8.3 Canonical snapshot source（B3 contract review correction）

```text
required primary persisted snapshot:
  complete immutable safe decision context + contextSchemaVersion

current V5 source:
  DecisionReplayContextView.contextSnapshotJson + evidenceRefsJson
  INSUFFICIENT: production content is snapshot metadata only

correlation cross-check:
  DecisionReplayRequestView + DecisionRunDetailView

replay/evaluation baseline:
  ReplayCaseRecord + EvaluationCaseRecord + RegressionVerdictRecord

required version/hash refs:
  decisionSchemaVersion + contextSchemaVersion + policyVersion
  + evaluationPolicyVersion + promptVersionRef + modelVersionRef
  + modelGatewayVersionRef + canonicalizationVersion + replayExecutorVersion
  + providerSummaryHash when actually present
```

V6 `context_payload_json` 当前不通过 existing port 暴露；read-model summary 只能用于一致性佐证，不得在与 V5 snapshot 冲突或 V5 内容缺失时静默替换 canonical source。完整冻结合同以 `DH_STAGE_QDR_6_B3_CANONICAL_SNAPSHOT_CONTRACT_REVIEW.md` 为准。

### 8.4 ReplayInputSnapshot 字段

```text
snapshotSchemaVersion: QDR6-REPLAY-INPUT-1
tenantId
traceId
requestId
decisionId
decisionRunId
contextSnapshot
evidenceRefs
policyVersion
modelVersionRef
modelGatewayVersionRef
promptVersionRef optional
replayInputRef + replayInputHash
expectedDecisionSummary + expectedSummaryHash
providerSummaryHash optional
replayAlgorithmVersion: QDR6-MOCK-REPLAY-1
canonicalizationVersion: QDR6-CJSON-1
hashAlgorithm: SHA-256
```

禁止把 createdAt/observedAt/current clock、随机数、机器路径、locale、数据库 row order 或 unordered map iteration 纳入 execution hash。

### 8.5 Canonical serialization

```text
encoding: UTF-8
Unicode: NFC
object keys: lexicographic ascending
absent optional field: omitted
explicit null: forbidden for hash input
booleans: lowercase true/false
numbers: plain decimal, no exponent, trailing zeros removed, negative zero normalized to zero
timestamps when allowed: UTC ISO_INSTANT
evidenceRefs/forbiddenActions: safe-text normalize + deduplicate + lexicographic sort
ordered business arrays: preserve declared order
line ending: none added to canonical byte sequence
```

### 8.6 Hash 规则

```text
ReplayExecutionHash = lowercaseHex(SHA-256(canonicalReplayInputBytes))
```

hash 输入必须包含 schema/canonicalization/replay algorithm versions；不允许只 hash context JSON。相同 canonical input 必须产生完全相同 hash。

### 8.7 Difference taxonomy

```text
CORRELATION_CHANGED
SNAPSHOT_CHANGED
EVIDENCE_REF_CHANGED
POLICY_VERSION_CHANGED
MODEL_VERSION_CHANGED
MODEL_GATEWAY_VERSION_CHANGED
PROMPT_VERSION_CHANGED
EXPECTED_SUMMARY_CHANGED
PROVIDER_SUMMARY_HASH_CHANGED
ALGORITHM_VERSION_CHANGED
MISSING_INPUT
INVALID_INPUT
TENANT_MISMATCH
UNSAFE_INPUT
HASH_MISMATCH
EXECUTION_FAILED
```

每个 `ReplayDifference` 只能保存 field/path、difference type、expected hash/ref、actual hash/ref 与脱敏 message，不保存 raw value。

### 8.8 Reproducibility status

```text
REPRODUCIBLE
DIFFERENT_INPUT
INCOMPLETE
INVALID
DENIED
FAILED
```

- 相同 input/version/hash：`REPRODUCIBLE`。
- 已明确版本或 snapshot 变化：`DIFFERENT_INPUT` + differences，不伪装为 executor failure。
- missing/incomplete/unsafe/cross-tenant：`INCOMPLETE/INVALID/DENIED`。
- canonicalization/hash 内部异常：`FAILED`。

### 8.9 Snapshot sufficiency gate

2026-07-11 B3 contract review 已完成 B2 actual aggregate 与 V5/V6/V8/V9 persistence/read-port 核验。结论为现有持久化与读取能力不足，blocker 已触发：

```text
canonical V5 snapshot missing
safe evidence refs incomplete
policy/model/gateway version unresolved
expected summary/hash missing
correlation not provable
requires raw prompt/provider response
requires new migration/schema
```

```text
B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED
EXISTING_PERSISTENCE_SUFFICIENT: NO
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION: NO
```

具体缺口包括完整 immutable context、`contextSchemaVersion`、完整 version vector、prompt/gateway version tenant-bound resolution、V6/V8 stable call identity 与 canonical hash version/domain semantics。不得通过临时新增 migration、Repository/JDBC/SQL、HTTP/provider call、默认值、`latest` 或读取 raw material 绕过 blocker。

2026-07-11 persistence gap review 已冻结 Option D：未来通过 `V10__qdr6_canonical_replay_snapshot.sql` 新增独立 immutable snapshot table，并以 tenant-bound source validation、完整 version vector、strict payload allowlist/size gate 和 local transaction 补齐。该结论只允许进入 `DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER`，不授权立即实现 migration、port/JDBC、assembler、canonicalizer 或 replay。

### 8.10 B3 测试矩阵

1. same snapshot + same versions + same algorithm 产生相同 hash。
2. map insertion order 不影响 hash。
3. evidence ref set 顺序不影响 hash。
4. ordered business array 顺序按规则保留。
5. decimal scale/exponent normalization deterministic。
6. Unicode NFC normalization deterministic。
7. policyVersion 变化产生 `POLICY_VERSION_CHANGED`。
8. modelVersionRef 变化产生 `MODEL_VERSION_CHANGED`。
9. modelGatewayVersionRef 变化产生明确 diff。
10. promptVersionRef 变化产生明确 diff。
11. expected summary/hash 变化产生明确 diff。
12. snapshot 变化产生 `SNAPSHOT_CHANGED`。
13. missing snapshot fail-closed。
14. incomplete safe refs fail-closed。
15. cross-tenant input denied。
16. raw prompt dependency rejected。
17. raw provider response dependency rejected。
18. credential-like key/value rejected。
19. BUY/SELL/MARKET_ORDER result rejected。
20. Provider call path impossible。
21. HTTP client path impossible。
22. NQ dependency/mutation path impossible。
23. clock/random/path/locale 不影响 hash。
24. canonicalization exception 返回 structured `FAILED`。
25. source read failure 由 B2 转换后仍保持 fail-closed。

## 9. B4：Evidence / Replay Internal Report

### 9.1 目标

生成内部只读 report，汇总 evidence completeness、correlation、reproducibility、regression、provider readiness、observability 与 internal acceptance result。

### 9.2 计划对象

```text
DecisionEvidenceReplayReport
DecisionEvidenceReportService
DecisionEvidenceAcceptanceResult
```

默认包：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/report/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/report/**
```

### 9.3 Report 最小字段

```text
correlation refs
evidence profile/status/completeness
missing/invalid findings
replay execution hash
replay reproducibility status
replay differences
regression verdict/findings refs
provider health/readiness evidence refs
observability report ref/hash
internal acceptance status
security boundary refs
```

Report 不输出 tenant 以外的跨租户数据，不输出 raw snapshot、raw prompt、raw provider response、credential 或执行内容。是否包含 tenantId 由内部调用边界决定，但所有查询和组装必须 tenant-bound。

### 9.4 Acceptance 语义

```text
PASS: internal evidence profile complete and replay/regression criteria satisfied
WARN: internal non-blocking finding remains
FAIL: mandatory evidence, correlation, replay or regression failed
SKIPPED: required upstream evidence not executed/available
```

任何状态都不代表 provider authorization、NQ integration permission、trading permission、execution approval 或 LIVE permission。

### 9.5 API blocker

默认 internal-only，不新增 Controller、endpoint、OpenAPI 或 schema。若实现需要对外 API：

```text
B4_API_REVIEW_REQUIRED
```

B4 必须停止，不能在普通 implementation 中夹带 API。

### 9.6 B4 测试矩阵

1. complete aggregate + reproducible replay + PASS regression 生成 internal PASS。
2. incomplete evidence 不能生成 PASS。
3. invalid correlation 生成 FAIL。
4. replay `DIFFERENT_INPUT` 生成 WARN/FAIL，按固定 policy 判定。
5. regression FAIL 生成 FAIL。
6. readiness NOT_READY 不能生成 provider authorization。
7. observability evidence 缺失不能伪造 complete。
8. cross-tenant report input denied。
9. raw/credential/trading field rejected。
10. report service exception 返回 structured FAIL。
11. report 不包含 Controller/API dependency。
12. PASS 不映射为 LIVE/trading/NQ permission。

## 10. B5：Final Close

B5 仅在以下条件全部满足后启动：

```text
B1 implementation/tests/commit: DONE
B2 implementation/tests/commit: DONE
B3 implementation/tests/commit: DONE
B4 implementation/tests/commit: DONE
all review triggers: CLOSED or NOT_TRIGGERED
full mvn test: PASS
quality validate: PASS
Docker/Testcontainers evidence: recorded truthfully
forbidden-scope scan: PASS
worktree before close: CLEAN
```

顺序固定：

```text
final close review PASS
-> self-contained archive packet
-> close docs commit
-> archive close commit
-> clean worktree
-> annotated tag
-> push tag
-> post-tag current cleanup
-> Stage-QDR-7 planning eligibility
```

B5 归档 packet 至少包含 `README.md`、`PLAN.md`、`IMPLEMENTATION_WORK_ORDER.md`、`BATCH_SUMMARY.md`、`VALIDATION_EVIDENCE.md`、`FINAL_CLOSE_REVIEW.md`、`ARCHIVE_CLOSE.md`、`STATUS_SNAPSHOT.md`；触发安全 review 时还需 `SECURITY_BOUNDARY_REVIEW.md`。

## 11. Review 触发规则

普通 B1-B4 不做 standalone review。仅以下情况触发：

```text
migration / schema / table / index / constraint change
API / Controller / endpoint / OpenAPI change
production Repository / JDBC / SQL expansion
tenant / correlation semantics change
security/redaction/credential boundary change
deterministic replay execution/canonicalization/hash semantics change
P0 / P1 blocker
stage final close
```

Blocker routing：

```text
B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED
B4_API_REVIEW_REQUIRED
```

## 12. 跨批次 Architecture Guards

后续实现必须扩展或复用 `ArchitectureTest`，至少证明：

1. evidence/replay 包不依赖 `dh-infra` implementation class。
2. evidence/replay 包不依赖 `dh-api`。
3. 不引入 WebClient、RestTemplate、OkHttp、JDK HttpClient 或 outbound URL construction。
4. 不引入 Provider SDK、OpenAI/Anthropic/Gemini/Ollama client。
5. 不引入 Agent、LangGraph、AutoGen、CrewAI。
6. 不依赖 NQ connector/repository/DB。
7. 不依赖 order/execution/account/ledger/risk mutation。
8. 不新增 V10 或修改 V1-V9。
9. 不新增 Controller/endpoint/OpenAPI path。
10. module dependency direction 保持 `domain -> usecase ports -> infra adapters/app wiring`，usecase 不反向依赖 infra。

## 13. Rollback 规则

- 每个 batch 独立 commit，禁止把 B1-B4 混在一个提交。
- 未提交失败 batch：只撤销该 batch allowed files。
- 已提交失败 batch：使用独立 `git revert <batch-commit>`，不使用 history rewrite。
- B2 如触发 repository expansion blocker，在 review 前不得产生半成品 SQL/JDBC diff。
- B3 如触发 snapshot blocker，只记录 structured blocker，不新增 migration 补洞。
- B4 如触发 API blocker，只记录 review 入口，不创建 provisional endpoint。
- rollback 不删除已有 audit/evidence，不把 FAIL/INCOMPLETE 改写为 PASS/COMPLETE。
- 任何 rollback 后必须重跑 targeted tests、`mvn -ntp test`、quality 和 forbidden-scope scan。

## 14. 批次授权矩阵

```text
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: B1_DONE / B2_DONE / B3_BLOCKED

ALLOW_STAGE_QDR_6_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_6_B2_IMPLEMENTATION_NOW: YES / CONSUMED
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: NO
ALLOW_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: YES
ALLOW_STAGE_QDR_6_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_FINAL_CLOSE_NOW: NO

ALLOW_MIGRATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
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
DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER
```
