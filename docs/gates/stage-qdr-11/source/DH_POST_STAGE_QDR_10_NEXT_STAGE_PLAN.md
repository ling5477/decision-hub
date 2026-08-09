# DH Post-Stage-QDR-10 Next-Stage Plan

## Stage-QDR-11 consolidated implementation handoff — 2026-08-09

```text
Stage-QDR-11: IMPLEMENTED / LOCAL_ACCEPTED / FINAL_CLOSE NOT_STARTED
Evidence authority: DecisionFeedbackEvidenceAggregate / SINGLE
Legacy direct acceptance: RETIRED
Internal acceptance model: DecisionEvidenceReplayInternalReport / REUSED AND EVOLVED
Completeness mapping: POLICY_QUALIFIED / FAIL_CLOSED
Bounded policy: PRESERVED END_TO_END
Internal facade: WIRED / READ_ONLY / NO_SIDE_EFFECT
Dual authority: NONE
Remote implementation CI: PENDING
Production capacity: NOT_PROVEN
Next action: DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-FINAL-CLOSE
```

本计划已由 Stage-QDR-11 consolidated implementation 消费。以下 candidate selection、work-order refinement
与 planning validation 仅作为历史设计依据，不再授权新的实现或范围扩张。

## Work-order refinement — 2026-08-09

Stage-QDR-11 的代码现实复核与 implementation work order 已冻结。`DecisionEvidenceReplayInternalReport`、
`DecisionEvidenceReplayReportService`、`InternalAcceptanceStatus` 与 `InternalAcceptanceFinding` 原位复用；
旧 `DecisionEvidenceAggregate` direct input 必须退役，`DecisionFeedbackEvidenceAggregate` 成为唯一 evidence
authority。规划中 `PARTIAL_WITHIN_BOUNDS` “可进入其余 gates”的表述现收紧为最终状态固定
`InternalAcceptanceStatus.INCOMPLETE`：允许生成脱敏 diagnostics，但不得静默或无条件 `ACCEPTED`。

精确 production/test/factsource allowlist、failure taxonomy、report shape、internal facade、Spring wiring、
PostgreSQL/security/regression matrix 与 scope blocker 见：

```text
docs/current/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md
```

本 refinement 不执行 implementation，不修改原候选选择、Stage-QDR-10 immutable close、API/DB/Repository/NQ
边界或 production-capacity 结论。

## 1. Task classification

```text
Task: DH-POST-STAGE-QDR-10-NEXT-STAGE-PLANNING
Classification: PLANNING_ONLY
Repository: E:/Project/decision-hub
Branch: dev
Planning baseline: 86d6dfb8b7933eda1592963414d5b81ea4dc4605
Change type: DOCS_ONLY
Implementation: NOT AUTHORIZED
Push / tag: NOT AUTHORIZED / NOT AUTHORIZED
```

本计划只验证 Stage-QDR-10 关闭状态、从当前代码重建成熟度、比较候选并冻结一个下一阶段。
它不执行技术实现，也不因为 historical roadmap、长期 deferred capability、已有类名或
`production capacity = NOT_PROVEN` 自动恢复任何 workstream。

## 2. Current fact verification

| 项目 | 已验证事实 |
| --- | --- |
| repository / branch | `E:/Project/decision-hub` / `dev` |
| HEAD / `origin/dev` / advertised SHA | `86d6dfb8b7933eda1592963414d5b81ea4dc4605` / exact match |
| ahead / behind | `0 / 0` |
| worktree / staged（写前） | `clean / empty` |
| Stage-QDR-10 | `CLOSED / ACCEPTED / ARCHIVED / TAGGED` |
| implementation / remediation | `756db5bdb541f94713211848f52e2c223c956dae` / `d275b9e30bb381bea8467286786add2c5b43e119` |
| archive close / cleanup | `1b826e8f92cc11d2b6cbe283da550039d7522737` / `86d6dfb8b7933eda1592963414d5b81ea4dc4605` |
| close tag | local + remote annotated tag verified；peeled target `1b826e8f92cc11d2b6cbe283da550039d7522737` |
| archive | `docs/gates/stage-qdr-10/` present / self-contained packet present |
| current process residue | `0` |
| cleanup exact-SHA CI | `31298387436 / PASS / 86d6dfb8b7933eda1592963414d5b81ea4dc4605` |
| starting terminal factsources | `12 / 12 / SYNCHRONIZED / 0 CURRENT CONFLICTS` |
| active P0 / P1 | `0 / 0` |
| formal / production capacity | `NOT_EXECUTED / NOT_PROVEN` |

Stage-QDR-10 的 archive、close commit、annotated tag 与 cleanup commit 只作为 immutable baseline；
本计划不修改或重新打开该 stage。

## 3. Scope freeze

### 3.1 Scope definitions

```text
READ_SCOPE:
- repository metadata, local/remote refs and cleanup CI metadata required by preflight
- the 12 terminal current factsources
- Stage-QDR-10 archive existence and current-residue facts
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
- docs/current/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md

VALIDATION_SCOPE:
- WRITE_ALLOWLIST
- forbidden technical and archive paths named by this task
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
- decision pipeline, audit, trace, snapshot, replay, evaluation and regression
- provider trust, readiness, observability and limited runtime guards
- Stage-QDR-10 consolidated evidence, historical feedback and attribution
- reference validation, lifecycle, rate/resource controls and capacity harness
- scheduler, NQ-DH boundary, Agent/checkpoint skeleton, CI and release tooling

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

| 能力面 | 当前代码现实 | 成熟度 | 当前结论 |
| --- | --- | --- | --- |
| decision pipeline | `DecisionDryRunController`、`DecisionOrchestrator`、V5/V6 persistence 与 read Controller 已存在 | R5 | limited runtime 可达；不等于 production-hardened |
| audit / trace / snapshot | audit/trace JDBC、V10 canonical snapshot、PostgreSQL tests 与 read model 已存在 | R4–R5 | 主证据链持久化并可查询 |
| replay / evaluation / regression | deterministic replay、evaluation/regression repositories、read model 与 tests 已存在 | R4 | internal implementation 完整；没有新增外部 runtime 授权 |
| provider trust / readiness / observability | trust policy、readiness guard、observability report 与 model-call persistence 已存在 | R3–R4 | 只支持 mock/internal evidence；real provider/HTTP 未接入 |
| decision-feedback consolidated evidence | `DecisionFeedbackEvidenceService` 已装配，组合 persisted decision/V15 feedback readers，PostgreSQL 与 architecture tests 已存在 | R4 | 只有 bean/test；生产代码无 `aggregate(...)` consumer |
| historical feedback evidence | 90-day、max-100、tenant/environment/decision/trace-bound read service 已装配 | R4 | internal-only，bounded、无公开 API |
| feedback attribution | V15 aggregate、required/repeatable-read atomic persistence、write-time reference validation 已测试 | R4 | 显式结构化 attribution；不连接 learning |
| internal acceptance consumer | Stage-QDR-6 `DecisionEvidenceReplayReportService` 已实现但未装配、无生产 caller，只消费旧 decision evidence | R2 | 单一 acceptance 模型存在，但未消费 Stage-QDR-10 aggregate |
| reference-liveness | V15 write-time exact validator 可证明当前 target；没有 registry/lifecycle runtime | R4 / R1 | 未发现真实 dangling ref；独立 lifecycle capability 继续 deferred |
| retention / cleanup | guard cleanup primitive 有测试；feedback retention 无 service/cutoff；scheduler 只有空壳 `Job` | R1–R2 | 没有合规、规模或增长压力证据 |
| rate / resource safety | PostgreSQL multi-instance rate guard、bounded queue、deadline、memory cap、kill switch 已接 limited runtime | R5 | limited runtime safety 已存在；不能推导 production capacity |
| capacity harness | 15 mandatory scenarios、94 thresholds、PowerShell harness、acceptance IT 与 artifact contract 已存在 | R4 | formal acceptance 未执行；属于独立 readiness gate |
| NQ-DH limited runtime | inbound limited dry-run 与 feedback endpoint 存在；fake/disabled clients，无 active cross-repo authority | R3–R5 | real HTTP、NQ mutation、NQ DB、RealClient 未授权 |
| Agent prerequisites | legacy research/task/artifact/checkpoint skeleton 存在；正式 `AgentTaskSpec` 等合同不存在 | R1–R3 | 无 LLM/LangGraph runtime；须独立 prerequisite planning |
| CI / Testcontainers / release tooling | Maven wrapper、单一 CI workflow、accepted PostgreSQL/Testcontainers/quality evidence存在 | R4 | 普通 CI 通过不等于 production readiness |

### 4.1 Active engineering gap

1. Stage-QDR-10 consolidated aggregate 已成为唯一带显式 `FeedbackExecutionScope`、decision provenance、
   bounded historical feedback 和四态 completeness 的 decision-feedback evidence 合同，但生产代码没有
   internal consumer。
2. Stage-QDR-6 已有唯一 internal acceptance report，能整合旧 decision evidence、replay、regression、
   provider readiness 与 observability；该 service 仅 R2，未装配、无生产 caller，且输入仍是旧
   `DecisionEvidenceAggregate`。
3. 若另建第二套 acceptance model，会制造重复 status/finding authority；若同时传入旧 aggregate 与
   consolidated aggregate，会制造双重 decision evidence source。下一阶段必须演进并复用现有 report，
   让 consolidated aggregate 成为单一 evidence authority。
4. 当前缺少 `COMPLETE_WITHIN_BOUNDS / PARTIAL_WITHIN_BOUNDS / INCONSISTENT / NOT_FOUND` 到既有
   `InternalAcceptanceStatus` 的冻结映射，以及跨 tenant/environment/correlation、overflow、optional feedback
   absent 和 source failure 的 acceptance matrix。

该缺口不证明 public API、自动 learning、capacity、reference registry、retention、NQ 或 Agent 的必要性。

## 5. Candidate assessment

| Candidate | Code evidence | Maturity | Real gap | Dependency | API/DB impact | Necessity | Classification | Rank |
| --- | --- | ---: | --- | --- | --- | --- | --- | ---: |
| A. Production Capacity / Resource Safety | PostgreSQL guard、bounded runtime、15-scenario/94-threshold harness | R4–R5 | formal acceptance 未执行；production profile/threshold 未证明 | 专用环境与独立 capacity authorization | 当前无；测量后才可判断 index/config | 只阻止 production-ready，不阻止当前 internal correctness | `PRODUCTION_READINESS_GATE` | 2 |
| B. Reference-liveness / Evidence Lifecycle | V15 write-time validator；无 registry/cleanup runtime | R4 / R1 | lifecycle registry 不存在，但无当前 dangling error | 真实数据审计与 lifetime contract | 未来可能 migration | 无当前错误，不应 overdesign | `DEFERRED_INDEPENDENT_CAPABILITY` | 5 |
| C. Retention / Cleanup Lifecycle | bounded guard cleanup primitive；feedback retention 与 scheduler runtime 不存在 | R1–R2 | 无 cutoff/规模/合规 pressure | reference-liveness、增长与合规证据 | 未来可能 migration/scheduler | 当前无实际压力 | `DEFERRED_INDEPENDENT_CAPABILITY` | 6 |
| D. Structured Feedback Evolution | attribution persistence/historical evidence R4；mutable learning stores 与 ingress 隔离 | R3–R4 | outcome reconciliation/holdout/promotion proposal 尚无合同 | 独立 offline/approval-first design | 不得接 inbound learning | 有后续价值但不是当前缺口 | `DEFERRED_INDEPENDENT_CAPABILITY` | 3 |
| E. Agent Runtime Contract Baseline | legacy task/artifact/checkpoint skeleton；正式前置合同 exact match 为 0 | R1–R3 | TaskSpec、immutable context、tool auth/budget、pause/resume、sandbox/egress 未冻结 | 独立 Agent planning-first | 未知；本轮不得实现 runtime | 只属于 Agent phase prerequisite | `AGENT_PHASE_PREREQUISITE` | 7 |
| F. NQ-DH Limited Runtime / Contract Formalization | inbound limited endpoint 与 fake/disabled clients；无 RealClient/active cross-repo authority | R3–R5 | joint authority、real HTTP、kill-switch joint tests 未形成 | 独立 NQ-DH workstream/worktree | cross-repo | 不能混入普通 DH stage | `CROSS_REPO_INTEGRATION_WORKSTREAM` | 8 |
| G. Platform / Operational Hardening | CI/tooling、capacity、scheduler skeleton、runbook/alerting问题互不相同 | R1–R4 | 没有单一清晰 engineering objective | 先形成独立 problem statement | 未知 | 不能打包成杂项 stage | `AMBIGUOUS` | 4 |
| H. Decision Evidence Runtime Consumption | consolidated service R4；existing internal acceptance report R2；两者无 main caller | R2–R4 | consolidated evidence 未进入唯一 internal acceptance model | 复用既有 safe structured results 与 wiring | API `NONE` / DB `NONE` | 当前最高价值且边界清晰 | `ACTIVE_NEXT_STAGE_CANDIDATE` | 1 |

## 6. Selected next stage

```text
SELECTED_WORKSTREAM: CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE
NEXT_STAGE_NAME: DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE
NEXT_STAGE_TYPE: DH_OWNED / INTERNAL_ACCEPTANCE_INTEGRATION / SECURITY_BOUNDARY_HARDENING
CORE_OBJECTIVE: MAKE_STAGE_QDR_10_CONSOLIDATED_EVIDENCE_THE_SINGLE_EVIDENCE_AUTHORITY_OF_THE_EXISTING_INTERNAL_ACCEPTANCE_MODEL
REAL_GAPS_CLOSED:
- CONSOLIDATED_EVIDENCE_HAS_NO_INTERNAL_CONSUMER
- EXISTING_INTERNAL_ACCEPTANCE_REPORT_IS_NOT_WIRED
- EXISTING_ACCEPTANCE_INPUT_EXCLUDES_DECISION_FEEDBACK_COMPLETENESS
- CONSOLIDATED_COMPLETENESS_TO_ACCEPTANCE_STATUS_MAPPING_IS_NOT_FROZEN
- DUAL_DECISION_EVIDENCE_AUTHORITY_MUST_BE_PREVENTED
PREREQUISITES:
- STAGE_QDR_10_CLOSED_ACCEPTED_ARCHIVED_TAGGED
- STAGE_QDR_10_AGGREGATE_AND_EXISTING_INTERNAL_REPORT_REUSED
- EXPLICIT_FEEDBACK_EXECUTION_SCOPE
- FEEDBACK_INGRESS_REMAINS_INGEST_ONLY
B1: SINGLE_EVIDENCE_AUTHORITY_AND_STATUS_MAPPING_CONTRACT
B2: EXISTING_INTERNAL_ACCEPTANCE_REPORT_EVOLUTION
B3: INTERNAL_CONSUMER_AND_WIRING_INTEGRATION
B4: POSTGRESQL_SECURITY_ACCEPTANCE_AND_STAGE_CLOSE
B5: NOT_NEEDED
```

### 6.1 Why selected now

- 这是唯一由当前 main-source caller scan 直接证明的 capability gap，而不是从 historical roadmap 恢复。
- 复用现有 `DecisionEvidenceReplayReportService`，不会为 Stage-QDR-10 aggregate 建第二套 acceptance model。
- 通过 usecase/internal wiring 即可从 R2/R4 收敛到 `WIRED_INTERNAL`；无需 Controller、公开 API、
  migration、write Repository、NQ、real HTTP/provider、Agent/LangGraph 或 capacity execution。
- `PARTIAL_WITHIN_BOUNDS` 表示 optional feedback 缺失，仍可进入其余安全门；`INCONSISTENT`、overflow、
  cross-scope 或 source failure 必须 fail-closed，边界可被稳定测试。

### 6.2 Rejected and deferred alternatives

- A 保留为独立 `PRODUCTION_READINESS_GATE`；本 stage 不混入 formal capacity run。
- B/C 没有真实 dangling、合规 cutoff、数据增长或压力证据，继续 deferred。
- D 只能未来以 explicit/offline/human-approved 方式规划，禁止恢复 automatic learning 或 promotion。
- E/F 分别是 Agent prerequisite 与 cross-repo workstream，不能由本 DH stage 自动启动。
- G 没有单一 engineering objective，不能以 platform hardening 名义合并无关问题。

## 7. Frozen implementation batches

### B1 — Single evidence authority and status mapping contract

冻结 consolidated aggregate 到既有 internal acceptance 的唯一 authority 和映射：

- `DecisionFeedbackEvidenceAggregate` 是 decision/feedback evidence 的唯一输入；禁止同时接受第二个
  `DecisionEvidenceAggregate`。
- `COMPLETE_WITHIN_BOUNDS` 可进入其余 replay/regression/readiness/observability gates。
- `PARTIAL_WITHIN_BOUNDS` 只代表 optional feedback absent，可进入其余 gates，但必须保留稳定 finding。
- `INCONSISTENT` 映射 `INVALID`；`NOT_FOUND` 映射 `INCOMPLETE`；source exception 映射 `FAILED`。
- 任一 overflow、tenant/environment/correlation mismatch 或 blocker finding 必须 fail-closed。

成功条件：contract tests 覆盖四态、source failure、稳定 finding 顺序、安全文本与全部 non-authorization
方法；该 batch 属于 security boundary，必须 standalone review。

### B2 — Existing internal acceptance report evolution

演进现有 `DecisionEvidenceReplayInternalReport` / `DecisionEvidenceReplayReportService`，让其消费
consolidated aggregate 内的 decision evidence、completeness 与 bounded feedback summary，并继续组合既有
replay、regression、provider readiness、observability structured results。只允许 safe refs、counts、status 和
sanitized findings；禁止 raw feedback、prompt、provider response、credential 或交易语义。

成功条件：不得新增第二套 acceptance status/report；原有 replay/regression/provider gates 不降级；
`ACCEPTED` 必须要求 consolidated evidence 可用且其余冻结 gate 全部 PASS。

### B3 — Internal consumer and wiring integration

建立 usecase-owned internal consumer/facade，使用显式 `FeedbackExecutionScope` 与 bounded query 调用
`DecisionFeedbackEvidenceService.aggregate(...)`，再复用 B2 report service；在
`DecisionPipelineWiringConfig` 装配 internal beans。调用方必须显式提供已有 structured replay/regression/
readiness/observability results；consumer 不自行触发 replay、Provider 或外部调用。

成功条件：actual Spring wiring 有且只有一套 acceptance bean；main source 存在真实 aggregate consumer；
无 Controller/route、无 scheduler、无 outbound client、无 persistence/write port、无 mutable learning edge。

### B4 — PostgreSQL, security acceptance and stage close

使用真实 PostgreSQL/Testcontainers 验证 decision provenance + bounded feedback + internal acceptance 的
完整、partial、not-found、cross-tenant、cross-environment、correlation mismatch、overflow 和 source failure
矩阵；执行 module/full regression、architecture boundary、quality 与 credential/forbidden-call scan。
通过后按 final-close → archive packet → archive close commit → independent annotated tag → current cleanup
纪律关闭 stage。

成功条件：active P0/P1 `0/0`，全部 mandatory tests 通过，production capacity 仍为 `NOT_PROVEN`，
archive packet self-contained，tag 只能由独立 tag-close task 创建。

```text
BATCH_COUNT: 4
B5: NOT_NEEDED
```

## 8. Impact and boundaries

```text
PRODUCTION_WRITE_BOUNDARY: NONE / READ_ONLY_INTERNAL_ACCEPTANCE
TEST_WRITE_BOUNDARY: EPHEMERAL_TESTCONTAINERS_POSTGRESQL_ONLY

MIGRATION_IMPACT: NONE
API_IMPACT: NONE
REPOSITORY_IMPACT: NONE / REUSE_EXISTING_READ_SERVICES_ONLY
NQ_IMPACT: NONE
PROVIDER_IMPACT: NONE / CONSUME_EXISTING_SAFE_STRUCTURED_RESULTS_ONLY
AGENT_IMPACT: NONE
LANGGRAPH_IMPACT: NONE
SECURITY_BOUNDARIES: EXPLICIT_SCOPE + SINGLE_EVIDENCE_AUTHORITY + SAFE_PROJECTION + FAIL_CLOSED + NO_LEARNING
NON_GOALS: CAPACITY + REFERENCE_LIVENESS + RETENTION + LEARNING + NQ + REAL_HTTP + REAL_PROVIDER + AGENT + LANGGRAPH + PAPER + LIVE
```

如 implementation discovery 证明必须新增 migration/schema、公开 API、Repository/JDBC、scheduler、NQ、
Provider 或其他禁止范围，立即停止并返回独立 scope blocker；不得在 work order 内现场扩权。

### 8.1 Security boundaries

- environment 只能来自显式 `FeedbackExecutionScope`；禁止 default、profile inference、repository inference
  或从唯一命中结果反推。
- consolidated aggregate 是单一 decision/feedback evidence authority；不得引入第二份 decision evidence
  input 或接受 correlation 不一致的 precomputed result。
- `PARTIAL_WITHIN_BOUNDS` 只表示 optional feedback absent；不能掩盖 decision evidence、provenance、
  overflow、source 或 scope 错误。
- report/consumer 只读，不持久化 acceptance、不写 feedback、learning stores、case quality、prompt/model/
  strategy version 或 promotion state。
- raw feedback payload、raw prompt、raw provider response、credential、签名材料与完整外部响应不得进入
  report、finding 或日志。
- `PLACE_ORDER`、`CANCEL_ORDER`、NQ mutation/DB access、real HTTP/provider、Paper/LIVE 全部禁止。

### 8.2 Non-goals

```text
NO_NEW_ACCEPTANCE_MODEL
NO_FEEDBACK_INGRESS_CHANGE
NO_IMPLICIT_OR_AUTOMATIC_LEARNING
NO_ACCEPTANCE_PERSISTENCE
NO_PUBLIC_EVIDENCE_OR_ACCEPTANCE_API
NO_MIGRATION_OR_SCHEMA_CHANGE
NO_REPOSITORY_OR_JDBC_CHANGE
NO_REFERENCE_LIVENESS_REGISTRY
NO_RETENTION_OR_CLEANUP
NO_SCHEDULER_OR_BACKGROUND_JOB
NO_CAPACITY_EXECUTION
NO_NQ_RUNTIME_OR_CROSS_REPO_CHANGE
NO_REAL_HTTP_OR_PROVIDER
NO_AGENT_OR_LANGGRAPH_RUNTIME
NO_PAPER_OR_LIVE
```

## 9. Test matrix

| 层级 | 必须验证 |
| --- | --- |
| contract | single evidence authority、四态映射、stable findings、safe projection、non-authorization |
| usecase | complete/partial/not-found/inconsistent/source-failed、existing gate preservation、deterministic order |
| security | cross-tenant、cross-environment、request/trace/decision/run mismatch、overflow、raw material rejection |
| PostgreSQL | persisted provenance + bounded feedback + actual reader integration；无 unbounded/N+1/write query |
| wiring | internal consumer/bean only；no Controller/API/scheduler/outbound client/provider/Agent dependency |
| architecture | one acceptance model；no inbound → mutable learning edge；no NQ/HTTP/Provider/Agent/LangGraph expansion |
| regression | affected modules、19-module full regression、real PostgreSQL/Testcontainers、0 mandatory skips |
| quality | `mvn -B -ntp -Pquality validate`、Checkstyle 0、Spotless PASS |

本 planning task 不重跑完整测试。可复用但不得冒充本轮执行的证据只有：implementation CI
`31297296670`、close CI `31297913196`、cleanup CI `31298387436`、`1326 / 0 / 0 / 0` tests、
PostgreSQL `17.10`。

## 10. Review triggers

- B1 single-authority/status mapping 属于 security boundary，必须 standalone review。
- 任何 migration/schema、API/Controller、Repository/JDBC、scheduler 或 cross-repo runtime 需求立即 block
  并重新 planning。
- 任一 P0/P1 blocker 必须 standalone review；普通 B2/B3 batch 不 standalone review。
- B4 stage close 必须 standalone final security/close review。

## 11. Rollback and final close

### Rollback

每个 implementation batch 使用独立 commit；回滚只撤销 Stage-QDR-11 的 internal contract/report/wiring，
不触碰 V15 data、Stage-QDR-10 code/archive/tag、历史 archive 或 tags。若 B1 无法在单一 evidence authority
且无 migration/API/Repository 变更的条件下冻结，停止 stage，不写技术实现。

### Final close condition

```text
CONSOLIDATED_EVIDENCE_INTERNAL_CONSUMER: PRESENT / WIRED_INTERNAL / TESTED
INTERNAL_ACCEPTANCE_MODEL_COUNT: 1
EVIDENCE_AUTHORITY_COUNT: 1 / DECISION_FEEDBACK_EVIDENCE_AGGREGATE
FOUR_STATE_MAPPING: FROZEN / FAIL_CLOSED
PUBLIC_API / MIGRATION / REPOSITORY: UNCHANGED / NONE / NONE
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
5. tag 验证后清理 completed-stage current process sources并确认 residue `0`。

## 12. Readiness decision

```text
POST_STAGE_QDR_10_NEXT_STAGE_PLAN: DONE
SELECTED_NEXT_WORKSTREAM: CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE
SELECTED_NEXT_STAGE: DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE
NEXT_STAGE_SCOPE: FROZEN
NEXT_STAGE_BATCH_COUNT: 4

TEST_MATRIX: CONTRACT + USECASE + SECURITY + POSTGRESQL + WIRING + ARCHITECTURE + REGRESSION + QUALITY
REVIEW_TRIGGERS: SECURITY_BOUNDARY + MIGRATION_API_REPOSITORY_OR_RUNTIME_SCOPE_CHANGE + P0_OR_P1 + STAGE_CLOSE
ROLLBACK: PER_BATCH_REVERT / NO_STAGE_QDR_10_OR_PERSISTED_DATA_CHANGE
FINAL_CLOSE_CONDITION: SINGLE_INTERNAL_ACCEPTANCE_MODEL + SINGLE_EVIDENCE_AUTHORITY + FULL_VALIDATION_PASS
ARCHIVE_TAG_DISCIPLINE: FINAL_CLOSE_THEN_ARCHIVE_THEN_ARCHIVE_COMMIT_THEN_ANNOTATED_TAG_THEN_CURRENT_CLEANUP
NEXT_TASK: DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-IMPLEMENTATION-WORK-ORDER

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
DH-STAGE-QDR-11-CONSOLIDATED-EVIDENCE-INTERNAL-ACCEPTANCE-IMPLEMENTATION-WORK-ORDER
```

该下一任务仍是 planning/work-order，只冻结 exact implementation allowlist、测试和停止条件；不授权
implementation。

## 14. Planning-task validation

```text
git status --short: PASS / 9 ALLOWLISTED DOCS CHANGED
git diff --check: PASS
unexpected files: 0
forbidden technical diff: 0
archive diff: 0
tag unchanged: PASS / PEELED TARGET 1b826e8f92cc11d2b6cbe283da550039d7522737
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

完整回归、PostgreSQL 和 exact-SHA CI 数字只按第 9 节列出的已接受证据复用，未在本轮重跑。
