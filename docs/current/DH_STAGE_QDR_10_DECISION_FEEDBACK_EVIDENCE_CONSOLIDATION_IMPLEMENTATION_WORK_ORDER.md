# DH Stage-QDR-10 Decision / Feedback Evidence Consolidation Implementation Work Order

## 1. Task classification

```text
Task: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-IMPLEMENTATION-WORK-ORDER
Classification: WORK_ORDER_ONLY / DOCUMENTATION
Repository: E:/Project/decision-hub
Branch: dev
Baseline: 29aaeddefe226fbb3bc3bd3acc8aa032564d76f1
Baseline parent / origin/dev: 4601eca969855d461b2cc1a0909a2a51c92269c0
Change type: DOCS_ONLY
Technical implementation: NOT EXECUTED
Push / tag: NOT AUTHORIZED / NOT AUTHORIZED
```

本工单只冻结 Stage-QDR-10 的 consolidated implementation 范围、合同、文件、批次与验证矩阵。
它不实现代码，不修改 API、migration、schema、Repository、NQ 或任何 runtime 集成。

## 2. Current fact verification

| 项目 | 已验证事实 |
| --- | --- |
| repository / branch | `E:/Project/decision-hub` / `dev` |
| HEAD / parent / origin/dev | `29aaeddefe226fbb3bc3bd3acc8aa032564d76f1` / `4601eca969855d461b2cc1a0909a2a51c92269c0` / `4601eca969855d461b2cc1a0909a2a51c92269c0` |
| ahead / behind | `1 / 0` |
| worktree / staged（写前） | `clean / empty` |
| planning commit | `29aaeddefe226fbb3bc3bd3acc8aa032564d76f1` / local only at task start |
| selected stage | `DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION` |
| closed milestones | side-effect containment + ingest atomicity remain `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| formal / production capacity | `NOT_EXECUTED / NOT_PROVEN` |

允许基线状态 A 成立；未执行 pull、merge、rebase、reset、amend、push 或 tag。

## 3. Scope freeze

### 3.1 Scope definitions

```text
READ_SCOPE:
- repository metadata required by preflight
- current authority and planning fact sources
- bounded decision evidence, V5/V6 persistence, V15 feedback evidence, wiring and tests
- every path in this task's documentation WRITE_ALLOWLIST

WRITE_ALLOWLIST_THIS_TASK:
- docs/current/DH_STAGE_QDR_10_DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION_IMPLEMENTATION_WORK_ORDER.md
- docs/current/DH_POST_FEEDBACK_INGEST_ATOMICITY_NEXT_STAGE_PLAN.md
- README.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/WORK_ORDER.md
- docs/current/ROADMAP.md
- docs/current/TESTING.md
- docs/current/WORKLOG.md
- docs/current/CODEX_PROJECT_INSTRUCTIONS.md

VALIDATION_SCOPE:
- WRITE_ALLOWLIST_THIS_TASK
- forbidden technical and archive paths
- root Maven quality reactor

FIXABLE_BLOCKER_SCOPE:
- WRITE_ALLOWLIST_THIS_TASK only

CURRENT_FACTSOURCE_SCAN_SCOPE:
- WRITE_ALLOWLIST_THIS_TASK
```

### 3.2 Scope invariants

```text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST_THIS_TASK: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset WRITE_ALLOWLIST_THIS_TASK: PASS
SCOPE_INVARIANTS: PASS / 3 OF 3
```

## 4. Code reality

### 4.1 Decision evidence surface

- `DecisionEvidenceQuery` 的真实强关联键是 `tenantId + traceId + requestId + decisionId`；
  `decisionRunId` 是 selector，但 `CORE_DECISION` 的 mandatory `RUN` evidence 要求 consolidated query
  将其提升为必填输入。
- `DecisionEvidenceAggregateService` 复用 V5 replay、V6 read model、V9 replay/evaluation 与 provider-safe
  refs，并用 `DecisionEvidenceConsistencyEvaluator` 返回 `COMPLETE / INCOMPLETE / INVALID`。
- `DecisionEvidencePolicy.CORE_DECISION` 的 mandatory 类型为 `REQUEST / RUN / CONTEXT_SNAPSHOT /
  DECISION_OUTPUT / AUDIT_EVENT / TRACE_STEP`；现有类型不包含 feedback observation、outcome、attribution、
  contribution 或 feedback reference。
- V5 `dh_decision_*` 与 V6 `decision_request` / `decision_run` schema 均有 tenant/trace/request 或 decision
  关联，但都没有 `environment` 列。V5 read repository 只按 `tenant_id + decision_id` 读取，再由 aggregate
  对 trace/request/decision 做严格一致性校验。

### 4.2 Feedback evidence surface

- `FeedbackSubject` 与 `OutcomeObservation` 的实际 scope 是
  `tenantId + FeedbackEnvironment + decisionId + traceId`；V15 四张表将同一组 scope 固化到主键、
  unique、复合 foreign key 与 index。
- feedback attribution identity 由 `attributionId` 标识，observation identity 由 `observationId` 标识；
  `AttributionResult` 还强制 audit reference 与 tenant/environment/decision/trace/observation/policy/hash
  全量一致。
- `HistoricalFeedbackEvidenceQuery` 强制 tenant、environment、显式时间范围，时间窗最多 90 天，页大小
  默认 50、最大 100；可按 decisionId 与 traceId 同时精确过滤，cursor 绑定全部 filter fingerprint。
- `JdbcHistoricalFeedbackEvidenceQueryAdapter` 在 read-only `READ_COMMITTED` transaction 中执行
  tenant/environment-bound SELECT；parent 使用 `observed_at desc, attribution_id desc` keyset 排序，
  child 以 attribution ids 批量读取，不逐条查询，不写库。
- `HistoricalFeedbackEvidenceView` 是 immutable safe projection；既有 domain/persistence guard 拒绝
  raw prompt、raw provider material、credential marker 与交易/执行指令。

### 4.3 Trusted environment source

可信来源已找到，且不得替换为默认或推断：

1. `HmacNqDryRunAuthenticator` 先验证 payload cap、authenticated tenant、signed wire environment、
   HMAC、source allowlist、tenant/source allowlist、authenticated environment 与 nonce replay。
2. 仅在上述校验全部通过后创建 `FeedbackExecutionScope(authenticatedTenantId, environment)`。
3. `DecisionDryRunController` 只把 `NqDryRunAuthResult.executionScope()` 附加到 command。
4. `FeedbackEnvironment.fromWire` 只接受精确 `DEV` / `TEST`，缺失或未知值 fail-closed；
   `FeedbackExecutionScope` 不归一化、不默认 tenant/environment。

Stage-QDR-10 只复用 `FeedbackExecutionScope` 作为 trusted caller supplied value contract。consolidated
service 不得依赖 Controller、不得创建 scope、不得读取 Spring profile，也不得从 repository 命中反推环境。

### 4.4 Twelve required answers

| 问题 | 冻结答案 |
| --- | --- |
| decision correlation key | `tenantId + traceId + requestId + decisionId`；consolidated completeness 另要求 `decisionRunId` selector |
| feedback correlation key | `tenantId + environment + decisionId + traceId`，并以 `observationId + attributionId` 区分合法多观察序列 |
| tenant 是否贯穿两侧 | 是；两侧 read surface 均 tenant-bound |
| feedback environment 位置 | `FeedbackExecutionScope`、domain subject/observation/audit ref、V15 四表、historical query/cursor/view 及全部 JDBC parent/child 条件 |
| decision persistence environment | V5 六张 `dh_decision_*` 表和 V6 decision core 表均无 environment |
| 隐式 environment 推断 | consolidated 路径不存在；现有 unrelated Spring profile 检查不得复用为 evidence environment |
| trusted environment 输入 | 已找到：认证根产出的 `FeedbackExecutionScope`；必须由调用方显式传入 |
| historical evidence bounded | 是；90 天、最大 100、keyset cursor、稳定排序 |
| read path side effects | historical adapter 仅 SELECT + read-only transaction；decision replay/read model 同样为只读；consolidated service 禁止写依赖 |
| 可复用 aggregate | decision-only `DecisionEvidenceAggregate` 可复用；不存在 decision+feedback consolidated aggregate |
| 新 Repository 需求 | 无；复用 `DecisionEvidenceAggregateService` 与 `HistoricalFeedbackEvidenceReadService/QueryPort` |
| API/migration/schema 前置 | 无；出现任一需求立即触发 scope-expansion blocker |

## 5. Frozen consolidated design

### 5.1 Query contract

新增 internal-only `DecisionFeedbackEvidenceQuery`，字段固定为：

```text
FeedbackExecutionScope executionScope  // 唯一 tenant/environment authority
String traceId                         // required
String requestId                       // required，decision side only
String decisionId                      // required
String decisionRunId                   // required，保证 CORE_DECISION RUN evidence
Instant fromObservedAt                 // required
Instant toObservedAt                   // required，range <= 90 days
int maxFeedbackItems                   // required，1..100
```

不得重复接受独立 tenant/environment 字符串，避免 query 内出现两个 authority。构造期必须验证所有字段，
并生成既有 `DecisionEvidenceQuery(CORE_DECISION)` 与
`HistoricalFeedbackEvidenceQuery(executionScope.tenantId(), executionScope.environment(), ..., decisionId,
traceId, ...)`。requestId/runId 只校验 decision evidence，不从 feedback 数据推断。

### 5.2 Correlation contract

```text
decision strong correlation:
tenantId + traceId + requestId + decisionId + decisionRunId

feedback strong correlation:
tenantId + environment + decisionId + traceId

consolidated authority:
FeedbackExecutionScope(tenantId, environment)
+ exact decision strong correlation
+ every feedback view exact scope match
```

时间接近、symbol、payload hash、自然语言、outcome、strategy 或 latest row 不得作为关联依据。
不同 `observationId/attributionId` 的多条观察是合法的 bounded time series；相同 identity 内容冲突、
parent/child 不完整、scope 不一致或无法唯一还原才属于 ambiguity，必须 fail-closed。

### 5.3 Bounded read contract

- 只调用一次 existing historical read service，`pageSize=maxFeedbackItems`，范围不超过 90 天。
- 结果按 `observedAt DESC, attributionId DESC`；contribution 与 reference 沿用 adapter 的稳定次序。
- `hasNext=false` 才允许完成聚合；`hasNext=true` 表示 correlated evidence 超出调用方上限，返回
  `INCONSISTENT / FEEDBACK_RESULT_LIMIT_EXCEEDED`，不得静默截断或循环翻页。
- empty page 是允许的 optional feedback absence，映射为 `PARTIAL`，不能伪装 `COMPLETE`。
- source failure、cursor/filter failure、incomplete parent/child、duplicate parent identity 均映射为
  blocker finding，不得返回部分成功。

### 5.4 Aggregate contract

新增 immutable `DecisionFeedbackEvidenceAggregate`：

```text
FeedbackExecutionScope executionScope
DecisionEvidenceCorrelation decisionCorrelation
DecisionEvidenceAggregate decisionEvidence
List<HistoricalFeedbackEvidenceView> feedbackEvidence
EvidenceCompleteness completeness
List<DecisionFeedbackEvidenceFinding> findings
```

集合必须 defensive copy，并保持稳定排序。aggregate 只暴露 safe refs/projections；不保存 raw payload、
prompt、provider response、credential、签名、nonce、自由文本交易指令或执行授权。

`EvidenceCompleteness` 固定为：

| 状态 | 精确语义 | 调用方处理 |
| --- | --- | --- |
| `COMPLETE` | decision `CORE_DECISION` mandatory evidence 全部存在，至少一条 feedback evidence，全部 scope/identity 一致且未超限 | 可作为 internal read evidence 使用 |
| `PARTIAL` | decision mandatory evidence 完整，但 optional feedback page 为空 | 明确标记缺少 feedback，不得提升为 complete |
| `INCONSISTENT` | decision `INVALID`、非 root mandatory evidence 缺失、source failure、scope/correlation 冲突、duplicate identity、dangling/incomplete child、unsafe material 或 limit overflow | fail-closed |
| `NOT_FOUND` | decision mandatory root `REQUEST` 或 `RUN` 不存在 | fail-closed |

`INCONSISTENT` 与 `NOT_FOUND` 均不是可降级成功状态。任何 cross-tenant、cross-environment、decision、
trace、request 或 run mismatch 都生成稳定 blocker finding；不得移除冲突记录后返回 `COMPLETE`。

### 5.5 Failure taxonomy

至少冻结以下 stable codes：

```text
EXECUTION_SCOPE_REQUIRED
ENVIRONMENT_INVALID
DECISION_ROOT_NOT_FOUND
DECISION_EVIDENCE_INCOMPLETE
DECISION_EVIDENCE_INVALID
TENANT_MISMATCH
ENVIRONMENT_MISMATCH
DECISION_MISMATCH
TRACE_MISMATCH
REQUEST_MISMATCH
RUN_MISMATCH
FEEDBACK_SOURCE_FAILED
FEEDBACK_RESULT_LIMIT_EXCEEDED
FEEDBACK_IDENTITY_CONFLICT
FEEDBACK_AGGREGATE_INCOMPLETE
UNSAFE_EVIDENCE_REJECTED
```

findings 只能包含稳定 code、severity、safe identity/ref 与安全摘要，不得包含 SQL、stack trace、raw
request/response 或 credential material。

## 6. Consolidated implementation allowlists

### 6.1 PRODUCTION_WRITE_ALLOWLIST

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQuery.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregate.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/EvidenceCompleteness.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceFinding.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceService.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
```

不得修改 `HistoricalFeedbackEvidenceQueryPort`、JDBC historical adapter、V5/V6 repositories 或任何
write Repository；这些是只读依赖，不是本 stage 的写入目标。

### 6.2 TEST_WRITE_ALLOWLIST

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceQueryTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceAggregateTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/evidence/DecisionFeedbackEvidenceServiceTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/DecisionFeedbackEvidenceConsolidationFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/qdr10/StageQdr10EvidenceArchitectureTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
```

### 6.3 FACTSOURCE_WRITE_ALLOWLIST

```text
docs/current/DH_STAGE_QDR_10_DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION_CONSOLIDATED_IMPLEMENTATION.md
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
```

### 6.4 FORBIDDEN_PATHS

```text
dh-api/**
dh-app/src/main/resources/db/migration/**
dh-security/**
dh-connector/**
contracts/**
golden_cases/**
docs/gates/**
pom.xml
**/pom.xml
.github/workflows/**

**/*FeedbackAttributionRepository*
**/*FeedbackAttributionPersistenceService*
**/*NqFeedbackIngestion*
**/*ExperienceFeedbackService*
**/*ExperienceStore*
**/*PheromoneStore*
**/*FailureCaseStore*

**/*Provider*
**/*Http*
**/*NqIntegration*
**/*Agent*
**/*LangGraph*
**/*Capacity*
```

若实现需要修改任一 forbidden path、existing schema、migration、API/Controller、write Repository、
feedback ingestion/attribution write、learning store、NQ、Provider/HTTP、Agent/LangGraph 或 capacity harness，
立即停止并返回 `DH-STAGE-QDR-10-SCOPE-EXPANSION-BLOCKER`。

## 7. Frozen batches

### B1 — Correlation and environment contract

- 在 allowlisted usecase package 新增 query/completeness/finding/aggregate contracts。
- 只接受 caller-supplied `FeedbackExecutionScope`；缺失/未知/不匹配 fail-closed。
- 冻结 decision 与 feedback 两侧强关联键、稳定 finding taxonomy 和 immutable aggregate 规则。
- B1 是普通实现批次；在同一 implementation task 内完成 contract tests 与 boundary scan，不单独创建
  review 文档。实际发现 P0/P1 或 scope expansion 时才触发 standalone blocker/review。

### B2 — Bounded feedback evidence composition

- 复用 existing `HistoricalFeedbackEvidenceReadService`，构造 exact tenant/environment/decision/trace query。
- 单页最大 100、最大 90 天、稳定 keyset 排序；`hasNext=true` fail-closed，不翻页、不截断成 complete。
- 不修改 V15 schema、adapter 或 Repository port，不新增 write/read Repository。

### B3 — Consolidated aggregate service

- 复用 `DecisionEvidenceAggregateService(CORE_DECISION)` 与 B2 historical read。
- 先验证 decision root/completeness，再读取 feedback；按本工单映射 completeness 与 findings。
- 不生成 feedback、不 promotion、不调用 `ExperienceFeedbackService.apply`，不写 Experience/Pheromone/
  FailureCase，不重新执行 decision。

### B4 — Internal wiring, PostgreSQL acceptance and implementation close

- 仅在 `DecisionPipelineWiringConfig` 装配 internal bean；不得新增 Controller、route、DTO 或 OpenAPI。
- 完成 usecase、wiring、PostgreSQL 17.x/Testcontainers、architecture、full regression 与 quality。
- B1-B4 在一个 consolidated implementation task 内完成，验证全部通过后只创建一个 implementation commit。
- 技术实现完成后进入独立
  `DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-FINAL-CLOSE`。

## 8. Test matrix

### 8.1 Contract and correlation

- valid tenant/environment/decision/trace/request/run；
- missing/unknown environment；null or mismatched verified scope；
- cross-tenant、cross-environment、decision/trace/request/run mismatch；
- same observation/attribution identity conflict；合法多 observation 稳定排序；
- raw prompt/provider response、credential-like ref 与交易/执行 marker 被拒绝。

### 8.2 Completeness

- mandatory decision + feedback present → `COMPLETE`；
- mandatory decision complete + feedback absent → `PARTIAL`；
- decision root absent → `NOT_FOUND` + fail-closed；
- mandatory non-root evidence absent、source failure 或冲突 → `INCONSISTENT` + fail-closed；
- `COMPLETE` 不允许 blocker finding 或缺失 mandatory evidence。

### 8.3 Bounded read

- `maxFeedbackItems` 的 1、100、0、101 边界；
- 90 天窗口边界与超界拒绝；
- `hasNext=true` → `FEEDBACK_RESULT_LIMIT_EXCEEDED`；
- stable `observedAt DESC, attributionId DESC` 与 child order；
- same input → same aggregate order；不得出现无界循环、N+1 或 latest-row substitution。

### 8.4 PostgreSQL/Testcontainers

- real PostgreSQL 17.x + Flyway V1-V15；
- tenant/environment isolation；decision/trace exact correlation；
- empty/single/multiple/conflicting feedback；bounded overflow；
- parent/child completeness 与 duplicate ambiguity fail-closed；
- query 前后 V15 四表、V5/V6 decision 表与 mutable stores row counts 不变；
- mandatory PostgreSQL tests real execution、0 skipped。

### 8.5 Architecture and regression

- consolidated classes 不依赖 `dh-api`、HTTP/Provider、NQ、Agent/LangGraph、trading/execution 或 mutable
  learning stores；
- 无 Controller/API route、migration、schema、write Repository diff；
- `ExperienceFeedbackService.apply` inbound callers 仍为 0；mutable store inbound writes 仍为 0；
- `mvn -B -ntp test`：19/19 reactor、failures/errors/skipped `0/0/0`；
- `mvn -B -ntp -Pquality validate`：19/19 reactor、Checkstyle 0、Spotless PASS。

## 9. Review triggers and blockers

普通 B1-B3 不增加独立 review。以下条件必须停止 consolidated implementation：

```text
STAGE_QDR_10_TRUSTED_ENVIRONMENT_SOURCE_BLOCKED
- verified FeedbackExecutionScope 无法由 trusted caller 提供

STAGE_QDR_10_MIGRATION_SCOPE_REQUIRED
- environment 必须写入 legacy decision schema
- 任一新表、列、index、constraint 或 Flyway migration 成为必要条件

STAGE_QDR_10_SCOPE_EXPANSION_BLOCKED
- 需要 API/Controller/OpenAPI
- 需要新增或修改 write Repository
- 需要修改 existing historical adapter/port 才能绕过当前 bounds
- 需要 NQ、HTTP/Provider、Agent/LangGraph、learning 或 capacity

STAGE_QDR_10_P0_P1_BLOCKED
- security boundary、tenant isolation、environment isolation 或 data consistency 出现 P0/P1
```

任何 migration、API/Controller、安全边界实际扩张、stage close 或 P0/P1 blocker 才进入 standalone review。

## 10. Rollback and close discipline

### Rollback

implementation 只新增 usecase contracts/service、一个 internal bean 与 allowlisted tests/docs。回滚使用对该
implementation commit 的普通 revert；不修改或删除 V15 数据，不触碰已关闭 milestones、archive 或 tags。
若在 commit 前失败，保留诊断证据并只撤销本 stage allowlisted worktree 变更。

### Final close discipline

```text
1. consolidated implementation + tests + boundary scan + one implementation commit
2. independent FINAL_CLOSE security / exact-diff review
3. publication authorization + exact-SHA CI
4. self-contained archive packet + separate archive-close commit
5. separate tag-close authorization and annotated tag creation
6. remote tag verification
7. post-tag docs/current source pruning and residue check
```

候选 tag 为 `dh-stage-qdr-10-decision-feedback-evidence-consolidation-close`，只能在 archive packet 完整、
archive close commit 已存在且独立 tag-close task 明确授权后创建。final-close review 本身不得提前创建 tag。

## 11. Readiness decision

```text
STAGE_QDR_10_IMPLEMENTATION_WORK_ORDER: DONE
TRUSTED_ENVIRONMENT_SOURCE: FOUND
EXPLICIT_ENVIRONMENT_CONTRACT: FROZEN
CORRELATION_CONTRACT: FROZEN
BOUNDED_FEEDBACK_READ: FROZEN
CONSOLIDATED_AGGREGATE: FROZEN

MIGRATION_REQUIRED: NO
API_CHANGE_REQUIRED: NO
WRITE_REPOSITORY_REQUIRED: NO

ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
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

## 12. Current-task validation record

```text
Only allowlisted docs changed: PASS / 10 OF 10
Technical diff: 0
Archive / tag diff: 0
Scope invariants: PASS / 3 OF 3
Authority consistency: PASS / 10 OF 10 / 0 CURRENT CONFLICTS
mvn -B -ntp -Pquality validate: PASS / 19 OF 19 REACTOR
Checkstyle / Spotless: 0 / PASS
Full tests: NOT_RERUN / WORK_ORDER_ONLY
Local commit: THIS_DOCUMENT_COMMIT
Push / tag: NOT EXECUTED / NOT EXECUTED
```

## 13. Risks

- trusted environment provenance：只能消费 verified `FeedbackExecutionScope`；任何新 caller 必须证明同等级
  authentication/authorization，不得直接 `new` scope 冒充 authority。
- legacy decision environment absence：environment 只约束 feedback read 与 consolidated authority，不能声称
  legacy row 本身已环境隔离。
- correlation ambiguity：合法多 observation 与冲突 identity 必须区分；limit overflow 不得静默截断。
- duplicate evidence model：复用现有 decision aggregate 与 historical view，不复制 persistence model。
- bounded query correctness：单页最大 100；超过上限 fail-closed，可能要求调用方缩小时间窗。
- cross-scope leakage：每个 view 必须逐项复核 tenant/environment/decision/trace。
- scope pressure：任何 migration/API/write Repository 需求都阻断，不允许现场扩权。
- document-over-code drift：implementation 必须按 exact allowlist 与实际类型复核，类名/路径变化需新 planning。

## 14. Next concrete action

```text
DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-CONSOLIDATED-IMPLEMENTATION
```

建议 commit message：

```text
docs(qdr): define stage-qdr-10 evidence consolidation work order
```
