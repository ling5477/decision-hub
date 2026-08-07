# DH Post-Feedback Ingest Atomicity Next-Stage Plan

## 0. Work-order disposition — 2026-08-08

```text
Plan: CONSUMED BY IMPLEMENTATION WORK ORDER
Work order: docs/current/DH_STAGE_QDR_10_DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION_IMPLEMENTATION_WORK_ORDER.md
Trusted environment source: FOUND / FeedbackExecutionScope
Migration / API / write Repository: NONE / NONE / NONE
Next action: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-CONSOLIDATED-IMPLEMENTATION
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
```

work order 已用真实类、V5/V6/V15 schema、historical query/adapter、认证根与 wiring 复核本计划，并冻结
exact production/test/factsource allowlist、四态 completeness、single-page bounded read 与 B1-B4。后续以
work order 为 implementation authority；本计划保留为选择依据，不得单独扩张实现范围。

## 1. Task classification

```text
Task: DH-POST-FEEDBACK-INGEST-ATOMICITY-NEXT-STAGE-PLANNING
Classification: PLANNING_ONLY
Repository: E:/Project/decision-hub
Branch: dev
Planning baseline: 4601eca969855d461b2cc1a0909a2a51c92269c0
Change type: DOCS_ONLY
Implementation: NOT AUTHORIZED
Push / tag: NOT AUTHORIZED / NOT AUTHORIZED
```

本计划只重建当前 authority、审查代码现实、比较候选并冻结一个下一阶段。它不实现任何技术能力，
也不把历史 roadmap、长期 deferred 项或类名存在本身当作当前能力完成证据。

## 2. Current fact verification

| 项目 | 已验证事实 |
| --- | --- |
| repository / branch | `E:/Project/decision-hub` / `dev` |
| HEAD / `origin/dev` / advertised SHA | `4601eca969855d461b2cc1a0909a2a51c92269c0` / exact match |
| ahead / behind | `0 / 0` |
| worktree / staged（写前） | `clean / empty` |
| side-effect containment | `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| ingest atomicity | `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| atomicity implementation | `1c41a85e94a48ded74ce7a71e65b45f8d239a12b` / published |
| atomicity close / cleanup | `b8e1e07c4721c7e71789bc69bcb83819e9f47e40` / `4601eca969855d461b2cc1a0909a2a51c92269c0` |
| atomicity close tag | local + remote verified；peeled target `b8e1e07c4721c7e71789bc69bcb83819e9f47e40` |
| atomicity archive | `docs/gates/platform-hardening-feedback-ingest-atomicity/` present |
| current process residue | `0` |
| starting terminal factsources | `12 / 12 / SYNCHRONIZED / 0 CURRENT CONFLICTS` |
| active P0 / P1 | `0 / 0` |
| formal / production capacity | `NOT_EXECUTED / NOT_PROVEN` |

两个已关闭 milestone 只作为已验证基线保留；本计划不修改 archive、close commit 或 annotated tag。

## 3. Scope freeze

### 3.1 Scope definitions

```text
READ_SCOPE:
- repository metadata and remote refs required by preflight
- the 12 terminal current factsources
- the two platform-hardening close-tag/archive existence facts
- bounded current source, migration, test, capacity-harness and CI evidence for candidates A-H
- every path in WRITE_ALLOWLIST, including the newly created target plan

WRITE_ALLOWLIST:
- README.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/WORK_ORDER.md
- docs/current/ROADMAP.md
- docs/current/TESTING.md
- docs/current/WORKLOG.md
- docs/current/CODEX_PROJECT_INSTRUCTIONS.md
- docs/current/DH_POST_FEEDBACK_INGEST_ATOMICITY_NEXT_STAGE_PLAN.md

VALIDATION_SCOPE:
- WRITE_ALLOWLIST
- forbidden technical paths named by this task
- root Maven quality reactor

FIXABLE_BLOCKER_SCOPE:
- WRITE_ALLOWLIST only

CURRENT_FACTSOURCE_SCAN_SCOPE:
- README.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/WORK_ORDER.md
- docs/current/ROADMAP.md
- docs/current/TESTING.md
- docs/current/WORKLOG.md
- docs/current/CODEX_PROJECT_INSTRUCTIONS.md

CODE_REALITY_REVIEW_SCOPE:
- decision/audit/trace/snapshot/replay source and migrations
- feedback ingress, structured attribution, historical evidence and atomic persistence
- reference validation, rate/resource guards and capacity harness
- provider/model gateway, Agent skeleton, NQ-DH boundaries and scheduler/tooling

CANDIDATE_WORKSTREAM_SCOPE:
- A through H exactly as required by the planning task
```

### 3.2 Scope invariants

```text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset WRITE_ALLOWLIST: PASS
SCOPE_INVARIANTS: PASS / 3 OF 3
```

禁止写入 `AGENTS.md`、`CLAUDE.md`、`docs/gates/**`、生产/测试代码、API、migration/schema、
Repository/JDBC、contracts、golden cases、POM、workflow 和 NQ。

## 4. Code reality review

成熟度口径：`R0 DOC_ONLY`、`R1 SKELETON`、`R2 INTERNAL_IMPLEMENTATION`、
`R3 WIRED_INTERNAL`、`R4 PERSISTED_AND_TESTED`、`R5 RUNTIME_REACHABLE`、
`R6 PRODUCTION_HARDENED`。

| 能力面 | 代码现实 | 成熟度 | 当前结论 |
| --- | --- | --- | --- |
| decision pipeline | `DecisionOrchestrator`、V5/V6 persistence、dry-run Controller 与内部 read model 已存在 | R5 | 可达但不等于 production-hardened |
| audit / trace / snapshot / replay | V5/V6/V9/V10/V11 结构、JDBC read/write 与 deterministic replay 已测试 | R5 | decision evidence 主链已形成 |
| feedback ingress | Controller → validation → atomic unit of work → envelope/event routing | R5 | `INGEST_ONLY`，不进入 learning |
| feedback atomic persistence | same-datasource required transaction、single-statement state snapshot、exact correlation | R5 | 已关闭 milestone，不重开 |
| structured feedback attribution | domain/service、V15 四表 aggregate、reference validation 与 PostgreSQL tests 已存在 | R4 | internal-only；未接 inbound feedback |
| historical feedback evidence | bounded 90-day/keyset query、tenant/environment filters、internal bean 已装配 | R3 | 没有公开 API；与 decision evidence 是独立 read surface |
| decision evidence aggregate | tenant/trace/request/decision strict correlation，聚合 V5/V6/V8/V9 evidence | R3 | 未包含 V15 outcome/attribution evidence types |
| reference validation | V15 write-time exact tenant-bound lookup；旧 target 表无 environment | R4 | 没有已证明 dangling ref；无 lifecycle registry |
| retention / scheduler | feedback reference status 预留 `ACTIVE/RELEASED/INVALID`；scheduler 仅空壳 `Job` | R1 | 无 feedback retention service、cutoff 或执行压力证据 |
| rate/resource safety | limited dry-run 使用 PostgreSQL rate guard、bounded queue/deadline；inbound memory store 有 TTL/上限 | R5 | 仅 limited runtime；不构成 production capacity 证明 |
| capacity harness | PowerShell harness、94 thresholds、acceptance IT 与 artifact contract 可执行 | R2 | formal run 未执行；只属于 readiness gate |
| provider readiness | internal readiness/observability/model-gateway contract 已装配，runtime provider 为 mock | R3 | real provider/HTTP 未授权且未接入 |
| Agent contract | `AgentTask`、artifact、checkpoint 等旧 skeleton 存在 | R1 | 缺少统一 TaskSpec、immutable context、tool permission/budget、pause/resume、sandbox/egress 合同 |
| NQ-DH state | inbound feedback 与 limited dry-run DH endpoint 存在；无 active cross-repo authority | R3 | real HTTP/provider/NQ mutation 未开始 |
| CI / Testcontainers / tooling | accepted exact-SHA CI、PostgreSQL 17.10、quality gate 有证据；存在非阻断 deprecation warning | R4 | 普通 CI 不等于 capacity acceptance |

### 4.1 Active technical/security gaps

1. `DecisionEvidenceAggregateService` 的固定 evidence 类型不包含 feedback observation、outcome、
   attribution、contribution 或 attribution reference。
2. `HistoricalFeedbackEvidenceReadService` 已能按 `tenant + environment + decisionId + traceId`
   有界读取，但没有与既有 `tenant + traceId + requestId + decisionId` decision evidence 形成一个
   可判定 `COMPLETE / INCOMPLETE / INVALID` 的统一内部合同。
3. V15 feedback identity 显式包含 `environment`，V5/V6 legacy decision tables 不含该列；任何统一读取
   必须由可信调用根显式提供 environment，禁止从数据、profile、header 或唯一命中结果推断。
4. 当前缺少覆盖跨租户、跨环境、decision/trace/request 冲突、多 observation 稳定排序、dangling
   reference 与 source unavailable 的 consolidated acceptance matrix。

上述缺口不影响已关闭 ingest correctness，但直接限制 decision → feedback → outcome → attribution 的
内部可审计性，因此是当前唯一 active next-stage candidate。

## 5. Candidate assessment

| 候选 | 代码证据 | 成熟度 | 当前真实缺口 | 依赖 | API/migration 影响 | 当前必要性 | 分类 | 顺序 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A. Production Capacity / Resource Safety | persistent rate guard、bounded runtime、capacity harness 与 94 thresholds | R2–R5 | formal profile 未执行；production thresholds 未证明；`correlation_id` 无 dedicated index | 专用环境、资源预算、capacity authorization | 本轮无；未来可能按测量结果决定 index | 只阻止 production-ready，未证明普通正确性受阻 | `PRODUCTION_READINESS_GATE` | 2 |
| B. Reference-liveness | V15 write-time validator 与 ACTIVE refs；无 registry/cleanup | R4 | 无 lifecycle registry，但无真实 dangling ref 证据 | 真实数据审计、lifetime contract | 可能 migration；当前不授权 | 非当前 blocker | `DEFERRED_INDEPENDENT_CAPABILITY` | 5 |
| C. Retention / Lifecycle | V15 lifecycle enum；feedback Repository 明确无 delete/retention；scheduler 空壳 | R1 | 无 cutoff、规模压力、删除安全合同 | reference-liveness、合规与规模证据 | 可能 migration/scheduler/API；未确定 | 非当前压力 | `DEFERRED_INDEPENDENT_CAPABILITY` | 6 |
| D. Feedback Evolution / Historical Learning | attribution persistence/historical evidence 已有；mutable learning stores 与 ingress 隔离 | R3–R4 | 缺显式离线 reconciliation、holdout、promotion lineage 与审批合同 | 本计划选定 evidence chain 后的独立设计 | 不得接 inbound；未来可能新 internal contract | 有价值但当前恢复会冲击 containment | `DEFERRED_INDEPENDENT_CAPABILITY` | 3 |
| E. Agent Runtime Contract Baseline | legacy task/artifact/checkpoint skeleton | R1 | TaskSpec、immutable context、permission/tool/budget、approval、pause/resume、sandbox/egress 未冻结 | 独立 Agent planning-first | 未知；不得实现 runtime | 只是 Agent phase 前置 | `AGENT_PHASE_PREREQUISITE` | 7 |
| F. NQ-DH Limited Runtime | DH inbound/limited endpoint 与 fake/disabled connector 存在；无 active cross-repo authority | R3 | NQ worktree/PR authority、real HTTP 与 no-side-effect joint gate 不存在 | 独立 `NQ-DH-*` workstream | cross-repo；本 stage 禁止 | 不能混入 DH stage | `CROSS_REPO_INTEGRATION_WORKSTREAM` | 8 |
| G. Platform / Operational Hardening | CI warning、harness、limited runtime guards、空 scheduler/旧 abstractions 并存 | R1–R4 | 多个互不相干问题，尚无单一工程目标 | 先形成单一 operational problem statement | 未知 | 不能打包成杂项 stage | `AMBIGUOUS` | 4 |
| H. Decision/Feedback Evidence Consolidation | decision evidence aggregate 与 V15 historical feedback evidence 均为真实实现，但彼此分离 | R3–R4 | 缺唯一、显式 environment-bound、fail-closed consolidated chain | 复用既有 read ports；可信 execution scope | API `NONE`；migration `NONE`；read-only Repository composition | 当前最高价值、边界清晰 | `ACTIVE_NEXT_STAGE_CANDIDATE` | 1 |

`correlation_id` dedicated index 缺失只保留为 capacity measurement item，不单独触发 migration；
Reference-liveness、Retention、Agent 与 NQ 也不因长期 deferred 或 skeleton 存在而自动提升。

## 6. Selected next stage

```text
SELECTED_WORKSTREAM: DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION
NEXT_STAGE_NAME: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION
NEXT_STAGE_TYPE: DH_OWNED / INTERNAL_EVIDENCE_CONSOLIDATION / SECURITY_BOUNDARY_HARDENING
CORE_OBJECTIVE: FORM_ONE_TENANT_AND_ENVIRONMENT_BOUND_FAIL_CLOSED_DECISION_TO_FEEDBACK_EVIDENCE_CHAIN
REAL_GAPS_CLOSED:
- DECISION_EVIDENCE_DOES_NOT_SURFACE_V15_FEEDBACK
- FEEDBACK_HISTORY_AND_DECISION_EVIDENCE_HAVE_SEPARATE_COMPLETENESS_MODELS
- ENVIRONMENT_MUST_BE_EXPLICIT_ACROSS_LEGACY_DECISION_AND_V15_FEEDBACK_BOUNDARY
- CONSOLIDATED_CORRELATION_FAILURE_MATRIX_IS_ABSENT
PREREQUISITES:
- BOTH_PLATFORM_HARDENING_MILESTONES_CLOSED
- EXISTING_V5_V6_V15_READ_MODELS_PRESERVED
- FEEDBACK_INGRESS_REMAINS_INGEST_ONLY
- EXPLICIT_DEV_OR_TEST_EXECUTION_SCOPE
B1: CORRELATION_AND_ENVIRONMENT_CONTRACT
B2: BOUNDED_FEEDBACK_EVIDENCE_READ_COMPOSITION
B3: CONSOLIDATED_DECISION_FEEDBACK_EVIDENCE_AGGREGATE
B4: INTERNAL_WIRING_ACCEPTANCE_AND_STAGE_CLOSE
B5: NOT_NEEDED
```

### 6.1 Why selected now

- 两个最近 hardening milestone 已把 inbound learning 副作用与 ingest 原子性风险关闭，安全地读取既有
  outcome/attribution evidence 成为自然的下一层价值，而不是恢复 learning。
- 需要的主要数据与 read ports 已存在，能够通过内部只读 composition 形成清晰的 4-batch stage。
- 该方向不依赖 NQ 变更、real HTTP/provider、Agent runtime、capacity execution、retention 或新 schema。
- 它复用现有 decision evidence 与 feedback evidence，不建设第二套 evidence store。

### 6.2 Rejected and deferred alternatives

- A 保留为 production-readiness gate；必须由后续显式 capacity task 执行正式 profile。
- B/C 没有真实 dangling ref、数据规模或合规 cutoff 证据，继续作为独立 capability deferred。
- D 不得在 containment 刚关闭后恢复自动 learning；未来只能 explicit/offline/approval-first。
- E/F 分别属于 Agent prerequisite 与 cross-repo workstream，不得塞入 DH Stage-QDR-10。
- G 尚未形成单一工程目标，不能用“平台治理”名义合并无关问题。

## 7. Frozen implementation batches

### B1 — Correlation and environment contract

冻结一个 usecase-owned internal query/aggregate contract：必须显式携带 `tenantId`、
`FeedbackEnvironment`、`traceId`、`requestId`、`decisionId` 与有界 observation time range；输出固定
`COMPLETE / INCOMPLETE / INVALID` 与稳定 finding codes。禁止 environment inference、tenantless lookup、
raw payload、credential、prompt/provider raw material 和交易语义。

成功条件：合同测试覆盖缺字段、跨 tenant/environment、主关联键冲突和范围上限；安全边界 review PASS。

### B2 — Bounded feedback evidence read composition

复用 `HistoricalFeedbackEvidenceQueryPort` 与 V15 表，提供 exact decision/trace、显式 environment、
bounded time/keyset 的 read-only source；保留多 observation 的稳定顺序和每个 observation/attribution 的
唯一 identity。不得新增表、索引、delete、retention、repair 或 write repository。

成功条件：PostgreSQL tests 证明无 cross-scope read、无 N+1/unbounded scan、缺 parent/child 或重复 identity
时 fail-closed；普通 batch 不做 standalone review。

### B3 — Consolidated decision-feedback evidence aggregate

以 composition 方式组合既有 `DecisionEvidenceAggregateService` 与 feedback source，增加 feedback
observation、outcome、attribution、contribution、reference 的 typed safe evidence projections；不得复制
持久化模型或修改 inbound ingestion。任一 tenant/environment/correlation 冲突、dangling ref、source
failure 或 ambiguity 都必须返回 `INVALID`/blocker finding，不能降级成“无反馈”。

成功条件：decision → feedback → outcome → attribution 的正常、空反馈、多个时间序列、冲突、dangling
和 unavailable matrix 全部通过；`ExperienceFeedbackService.apply` 与 mutable-store inbound callers 仍为 0。

### B4 — Internal wiring, acceptance and stage close

只装配 internal service/report consumer；不新增 Controller 或公开 API。完成真实 PostgreSQL/Testcontainers、
module/full regression、architecture boundary、quality、credential/forbidden-call scan 与 stage security review。
通过后按 final-close → archive packet → archive close commit → annotated tag → post-tag current cleanup 顺序关闭。

成功条件：active P0/P1 `0/0`，全部强制测试通过，production capacity 仍明确 `NOT_PROVEN`，archive packet
self-contained，tag 只在独立 tag-close task 中创建。

```text
BATCH_COUNT: 4
B5: NOT NEEDED
```

## 8. Impact and boundaries

```text
PRODUCTION_WRITE_BOUNDARY: NONE / READ_ONLY_COMPOSITION
TEST_WRITE_BOUNDARY: EPHEMERAL_TESTCONTAINERS_POSTGRESQL_ONLY

MIGRATION_IMPACT: NONE
API_IMPACT: NONE
REPOSITORY_IMPACT: EXISTING_READ_PORT_COMPOSITION_OR_BOUNDED_READ_EXTENSION_ONLY / NO WRITE PORT
NQ_IMPACT: NONE
PROVIDER_IMPACT: NONE / MOCK_AND_EXISTING_SAFE_REFS_ONLY
AGENT_IMPACT: NONE
LANGGRAPH_IMPACT: NONE
SECURITY_BOUNDARIES: EXPLICIT_ENVIRONMENT + TENANT_BOUND + SAFE_PROJECTION + FAIL_CLOSED + NO_LEARNING
NON_GOALS: CAPACITY + REFERENCE_LIVENESS + RETENTION + LEARNING + NQ + PROVIDER + AGENT + LANGGRAPH + PAPER + LIVE
```

如 implementation discovery 证明必须修改 migration/schema、公开 API、write Repository、NQ 或任何禁止范围，
立即停止并返回独立 scope planning；不得在 work order 中用 scope erratum 扩张。

### 8.1 Security boundaries

- environment 只能来自可信、显式的 DEV/TEST execution scope；禁止默认、猜测或从唯一结果反推。
- 每次读取都必须 tenant-bound；legacy target 缺 environment 时保留当前显式 scope 并做 exact identity check。
- raw feedback payload、raw prompt、raw provider response、credential、签名材料和完整外部响应不得进入 aggregate。
- consolidated evidence 只读，不写 learning stores、不 promotion、不修改 case quality 或策略状态。
- `PLACE_ORDER`、`CANCEL_ORDER`、NQ mutation/DB access、real HTTP/provider、Paper/LIVE 全部禁止。
- source failure、duplicate identity、dangling ref、cross-scope mismatch 与 ambiguity 全部 fail-closed。

### 8.2 Non-goals

```text
NO_FEEDBACK_INGRESS_CHANGE
NO_IMPLICIT_OR_AUTOMATIC_LEARNING
NO_REFERENCE_LIVENESS_REGISTRY
NO_RETENTION_OR_CLEANUP
NO_CAPACITY_EXECUTION
NO_CORRELATION_ID_INDEX
NO_PUBLIC_EVIDENCE_API
NO_MIGRATION_OR_SCHEMA_CHANGE
NO_NQ_RUNTIME_OR_CROSS_REPO_CHANGE
NO_REAL_HTTP_OR_PROVIDER
NO_AGENT_OR_LANGGRAPH_RUNTIME
NO_PAPER_OR_LIVE
```

## 9. Test matrix

| 层级 | 必须验证 |
| --- | --- |
| contract | required scope、explicit environment、bounded range/page、stable findings、safe projection |
| usecase | complete/empty/multiple feedback、missing source、correlation mismatch、deterministic ordering |
| security | cross-tenant、cross-environment、request/trace/decision mismatch、raw material rejection、no learning callers |
| JDBC/PostgreSQL | exact filters、bounded keyset、complete parent/children、duplicate/dangling fail-closed、query-count bound |
| wiring | internal bean only、no Controller/API route、no outbound client/provider/Agent dependency |
| architecture | no inbound → mutable learning edge；no NQ/HTTP/Provider/Agent/LangGraph expansion |
| regression | affected modules、19-module full regression、real PostgreSQL/Testcontainers、0 mandatory skips |
| quality | `mvn -B -ntp -Pquality validate`、Checkstyle 0、Spotless PASS |

本 planning task 不重跑完整测试。可复用但不得冒充本轮执行的证据只有：implementation CI
`31184220520`、close CI `31189681961`、cleanup CI `31190607480`、`1293 / 0 / 0 / 0` tests、
PostgreSQL `17.10`。

## 10. Review triggers

- B1 correlation/environment contract 属于 security boundary，必须 standalone review。
- 任何 migration、schema、API/Controller 或 write Repository 需求立即 block 并重新 planning。
- 任一 P0/P1 blocker 必须 standalone review；普通 B2/B3 batch 不 standalone review。
- B4 stage close 必须 standalone final security/close review。

## 11. Rollback and final close

### Rollback

每个 implementation batch 使用独立 commit；回滚只撤销该 stage 的 internal contract/composition/wiring，
不触碰 V15 data、两个已关闭 platform-hardening milestone、历史 archive 或 tags。若 B1 无法在无 migration
条件下证明 environment boundary，停止 stage，不写生产代码。

### Final close condition

```text
CONSOLIDATED_INTERNAL_CHAIN: COMPLETE / TENANT_AND_ENVIRONMENT_BOUND / FAIL_CLOSED
DECISION_TO_FEEDBACK_TO_OUTCOME_TO_ATTRIBUTION: AUDITABLE
PUBLIC_API / MIGRATION / WRITE_REPOSITORY: UNCHANGED / NONE / NONE
INBOUND_MUTABLE_LEARNING_CALLERS: 0
ACTIVE_P0_P1: 0 / 0
FULL_REGRESSION / POSTGRESQL / QUALITY: PASS / PASS / PASS
PRODUCTION_CAPACITY: NOT_PROVEN
```

### Archive/tag/current-cleanup discipline

1. final close review PASS；
2. self-contained `docs/gates/<stage-id>/` archive packet；
3. 独立 archive close commit；
4. clean worktree 后由独立 tag-close task 创建 annotated tag；
5. tag 验证后清理 completed-stage current process sources 并确认 residue `0`。

## 12. Readiness decision

```text
POST_FEEDBACK_INGEST_ATOMICITY_NEXT_STAGE_PLAN: DONE
SELECTED_NEXT_WORKSTREAM: DECISION_FEEDBACK_EVIDENCE_CONSOLIDATION
SELECTED_NEXT_STAGE: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION
NEXT_STAGE_SCOPE: FROZEN
NEXT_STAGE_BATCH_COUNT: 4

TEST_MATRIX: CONTRACT + USECASE + SECURITY + POSTGRESQL + WIRING + ARCHITECTURE + REGRESSION + QUALITY
REVIEW_TRIGGERS: SECURITY_BOUNDARY + MIGRATION_OR_API_SCOPE_CHANGE + P0_OR_P1 + STAGE_CLOSE
ROLLBACK: PER_BATCH_REVERT / NO_V15_DATA_OR_CLOSED_MILESTONE_CHANGE
FINAL_CLOSE_CONDITION: CONSOLIDATED_CHAIN_ACCEPTED + ACTIVE_P0_P1_0_0 + FULL_VALIDATION_PASS
ARCHIVE_TAG_DISCIPLINE: FINAL_CLOSE_THEN_ARCHIVE_THEN_ARCHIVE_COMMIT_THEN_ANNOTATED_TAG_THEN_CURRENT_CLEANUP
NEXT_TASK: DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-IMPLEMENTATION-WORK-ORDER

ALLOW_NEXT_STAGE_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_NEXT_STAGE_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_EXECUTION_NOW: NO
ALLOW_REFERENCE_LIVENESS_NOW: NO
ALLOW_RETENTION_NOW: NO
ALLOW_FEEDBACK_LEARNING_NOW: NO
ALLOW_NQ_RUNTIME_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 13. Next concrete action

```text
DH-STAGE-QDR-10-DECISION-FEEDBACK-EVIDENCE-CONSOLIDATION-IMPLEMENTATION-WORK-ORDER
```

该下一任务仍是 planning/work-order，不授权 implementation。

## 14. Planning-task validation

```text
git status --short: PASS / 9 ALLOWLISTED DOCS CHANGED
git diff --check: PASS
unexpected files: 0
forbidden technical diff: 0
archive diff: 0
staged before commit: 0
current authority update set: PASS / 8 OF 8
scope invariants: PASS / 3 OF 3
mvn -B -ntp -Pquality validate: PASS / 19 OF 19 REACTOR
Checkstyle: 0 VIOLATIONS
Spotless: PASS
full tests: NOT_RERUN / PLANNING_ONLY
capacity: NOT_EXECUTED
local commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
push / tag: NOT_EXECUTED / NOT_EXECUTED
```

上述 full regression、PostgreSQL 和 exact-SHA CI 数字仅按第 9 节列出的已接受证据复用，未在本轮重跑。
