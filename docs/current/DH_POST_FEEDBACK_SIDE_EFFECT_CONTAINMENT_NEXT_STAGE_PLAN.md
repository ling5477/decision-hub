# DH Post-Feedback-Side-Effect-Containment Next Stage Plan

> Task: `DH-POST-FEEDBACK-SIDE-EFFECT-CONTAINMENT-NEXT-STAGE-PLANNING`
> Date: `2026-08-02`
> Type: `PLANNING_ONLY / POST_MILESTONE_TRANSITION_REVIEW / CODE_REALITY_GAP_REVIEW / DOCS_ONLY_CHANGE`
> Implementation: `NOT AUTHORIZED`

## 1. Planning decision

```text
POST_FEEDBACK_CONTAINMENT_NEXT_STAGE_PLAN: DONE
CLOSED_MILESTONE_STATE: CLOSED / ACCEPTED / ARCHIVED / TAGGED
SELECTED_NEXT_WORKSTREAM: FEEDBACK_ENVELOPE_EVENT_ATOMIC_PERSISTENCE
SELECTED_NEXT_STAGE: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
NEXT_STAGE_TYPE: DH_OWNED / PLATFORM_HARDENING / DATA_CONSISTENCY_AND_SECURITY_BOUNDARY
NEXT_STAGE_SCOPE: FROZEN
NEXT_STAGE_BATCH_COUNT: 4
ALLOW_NEXT_STAGE_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_NEXT_STAGE_IMPLEMENTATION_NOW: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

本计划不重新打开 Stage-QDR-9 或 feedback side-effect containment milestone。下一阶段只关闭 legacy
feedback ingress 的 envelope/event 两阶段持久化缺口，不恢复 implicit learning，不实施 capacity、
reference-liveness、retention、NQ runtime、Provider、Agent、LangGraph、Paper 或 LIVE。

## 2. Verified baseline

```text
Repository: E:/Project/decision-hub
Branch: dev
HEAD / origin/dev / advertised SHA: 071bc29ee3c03b4099623c4ade441783cc22091f
Ahead / behind: 0 / 0
Worktree / staged before write: clean / empty
Implementation commit: fddf3558255b5a4f8f6071da42363649942afe46
Archive close commit: 86381c6a47a5d68eb7ab9f57892e42e282ac63ea
Post-tag cleanup commit: 071bc29ee3c03b4099623c4ade441783cc22091f
Close tag: dh-platform-hardening-feedback-side-effect-containment-close
Local / remote peeled tag target: 86381c6a47a5d68eb7ab9f57892e42e282ac63ea / VERIFIED
Archive: docs/gates/platform-hardening-feedback-side-effect-containment/ / PRESENT
Milestone-specific current process residue: 0
Terminal closed-milestone factsources: 12 / 12 / ONE HASH / 0 CONFLICTS
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity: NOT_PROVEN
```

关闭 milestone 的 exact-diff review 仍为 `PASS / 0 REPORTABLE FINDINGS`。本轮识别的原子性缺口是该
milestone 已记录并按 exact-diff reportability 抑制的 supporting candidate；它在 base/head 中行为相同，
因此不改写 milestone 的关闭结论，也不追溯升级为其 P0/P1。

## 3. Task scope design

### READ_SCOPE

- Current authority：任务书列出的 root/current 文档。
- Close evidence：feedback containment archive packet 与 tag/commit evidence。
- Code reality：feedback ingress、persistence、router/handlers、structured attribution、audit/replay、
  reference/retention、capacity、Agent/NQ boundary、V1-V15 migration、相关 tests、root POM 与 CI。

### WRITE_ALLOWLIST

- `docs/current/DH_POST_FEEDBACK_SIDE_EFFECT_CONTAINMENT_NEXT_STAGE_PLAN.md`
- `docs/current/{README,STATUS,WORK_ORDER,ROADMAP,TESTING,WORKLOG,CODEX_PROJECT_INSTRUCTIONS}.md`
- root `README.md`
- `FACTSOURCE_POLICY.md` / `ARCHIVE_INDEX.md` 仅在 authority policy 真实变化时可写；本轮不需要变化。

### VALIDATION_SCOPE

- 全部 write allowlist、禁止技术路径 diff、archive/tag immutability、current authority consistency 与
  `mvn -B -ntp -Pquality validate`。

### FIXABLE_BLOCKER_SCOPE

- 仅 write allowlist。任何代码、测试、migration、API、Repository、archive 或 tag 变更需求均停止。

### CURRENT_FACTSOURCE_SCAN_SCOPE

- 本轮允许同步的 root/current 文档。`AGENTS.md`、`CLAUDE.md` 只读，继续服从
  `STATUS.md + WORK_ORDER.md` 的 primary authority。

### CODE_REALITY_REVIEW_SCOPE

- Legacy feedback envelope/event、transaction/idempotency、structured attribution/historical evidence、
  audit/replay/reference/retention、capacity/resource guard、Agent contract 与 NQ-DH boundary。

### CANDIDATE_WORKSTREAM_SCOPE

- 任务书 A-H 八个候选方向；历史 candidate/deferred 状态均不自动激活。

```text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset WRITE_ALLOWLIST: PASS
TECHNICAL_PATHS_IN_WRITE_ALLOWLIST: 0
```

## 4. Code reality findings

### 4.1 Feedback ingress and persistence

实际调用链为：

```text
NqFeedbackController
  -> DefaultNqFeedbackIngestionService.ingest
  -> NqFeedbackEventRepository.saveEnvelope
  -> NqFeedbackEventTypeRouter.route
  -> AbstractNqFeedbackEventHandler.handle
  -> NqFeedbackEventRepository.append
```

- `NqFeedbackEnvelopeRepository` 独立端口在当前树中不存在；envelope 与 legacy event 共用
  `NqFeedbackEventRepository`。
- JDBC 实现的 `saveEnvelope` 与 `append` 是两个独立 `JdbcTemplate.update`，ingestion service、wiring 和
  repository 均没有覆盖二者的显式统一事务。
- envelope 与 event 共用 `dh_nq_feedback_events`，但写为两行；V3 的 `event_id` unique 只裁决 envelope
  幂等，不代表 legacy event 已 append。
- append 失败会向上 fail-closed，不返回 false success；但此前 envelope 已写入。相同 eventId 重试在
  `findEnvelopeByEventId` 命中后直接返回 `DUPLICATE`，不再 route，因此无法安全恢复缺失 event。
- commit outcome unknown 时，当前合同也没有把“完整提交”和“只存在 envelope”区分为封闭状态。
- In-memory 实现同样把 envelope/event 放在两个集合和两个方法中；单方法 `synchronized` 不形成跨操作
  rollback，虽然其常规 append 路径没有 JDBC 异常面。
- 现有 tests 覆盖 save failure 不 dispatch、handler failure 传播和 duplicate 只 dispatch 一次，但未覆盖
  append failure 后 envelope rollback、重试恢复、并发 duplicate 与 commit-unknown 完整性。

该问题是现实 code-path 数据一致性缺口，但 JDBC wiring 默认关闭，仓库没有生产部署或真实受影响数据证据，
因此本计划把它分类为 `SECURITY_HARDENING_CANDIDATE` 和最高优先级 next stage，而不是当前 P0/P1。

### 4.2 Closed learning boundary

- 八个 handlers 与 `DefaultNqIntegrationUseCase` 只 append event。
- inbound `ExperienceFeedbackService.apply` caller 为 0，三个 mutable learning store inbound write 为 0。
- structured attribution 仍 internal-only，未连接 legacy ingress。
- 下一阶段不得通过原子性修复恢复 Experience/Pheromone/FailureCase mutation。

### 4.3 Structured attribution, audit and replay

- V15 aggregate persistence 已通过 required/repeatable-read transaction 完成四表写入、read-back、
  duplicate reconciliation 和 commit-unknown read-only reconciliation。
- reference validator 在写入时确认 tenant-bound audit/replay/evaluation target；historical evidence read model
  为 internal-only/read-only、keyset pagination、90-day query window、page hard max 100。
- legacy envelope/event atomicity 与 V15 structured attribution 是两条未连接的链；不得借修复前者扩展后者。

### 4.4 Capacity and resource safety

- `config/qdr7-capacity` criteria、thresholds、artifact contracts、PowerShell harness、Maven profile 和 tests 均存在。
- persistent dry-run rate admission 使用 PostgreSQL 原子 bucket，limited runtime 有 deadline、bounded
  concurrency/queue、payload/memory caps 与 fail-closed guard。
- formal capacity 本轮未执行；普通 CI 与历史 1252 tests 不能替代 capacity acceptance。
- 未证明 production capacity 只阻止 `production-ready` 声明，不阻止本次 correctness hardening 规划。

### 4.5 Reference-liveness and retention

- V15 只在写入时验证 target 存在；target 生命周期没有 database FK/registry，理论上可形成 dangling ref。
- 当前代码与仓库证据未显示 unresolved audit/replay/evidence ref，也没有实际 deletion 导致的 blocker。
- V15 repository 明确不提供 delete/retention；没有当前数据规模、合规截止或存储压力证据。
- 因此 reference-liveness 与 retention 保持独立 deferred capability，不能并入原子性阶段。

### 4.6 Feedback evolution, Agent and NQ-DH

- deterministic attribution、historical evidence、approval/replay 基础存在；case quality、outcome reconciliation、
  holdout、promotion、version lineage、bounded learning 和显式 human approval workflow 尚未形成闭环。
- Agent skeleton 有 task/artifact/checkpoint 与 fake tools，但缺统一 tool permission、context budget、immutable
  input snapshot、resume、sandbox/egress 和完整 failure classification 合同；不得直接选择 LangGraph。
- DH 有 default-disabled limited dry-run；本轮没有 NQ cross-repo authority/worktree，真实 HTTP/Provider/NQ
  mutation 均禁止。任何未来工作必须是独立 integration workstream。

### 4.7 Platform and operational health

- Maven wrapper 与单一 CI workflow 存在；当前 quality/close CI 为 green，当前 active P0-P3 为 0。
- `dh-scheduler`、`dh-providers` 等模块较薄，但没有职责冲突或当前主流程 blocker 证据。
- current 文档包含大量历史时间线，治理成本存在，但 primary authority 和 historical precedence 已定义，
  不足以压过当前数据一致性缺口。

## 5. Candidate assessment

| 候选方向 | 代码证据 | 当前风险 | 依赖 | API/migration 影响 | 当前必要性 | 分类 | 推荐顺序 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Feedback envelope/event append atomicity | save envelope 后独立 route/append；append failure 后 retry 直接 duplicate | 孤立 envelope、不可安全重试、commit-unknown 无完整性状态 | DH 内部 persistence contract；无跨仓依赖 | API none；migration none；Repository/transaction contract review required | 最高；有明确失败链 | `SECURITY_HARDENING_CANDIDATE` | 1 |
| Formal capacity/resource safety | criteria/profile/harness 与 bounded guards 已实现；formal 未执行 | 不能声明 production-ready；不构成功能 correctness blocker | 受控 capacity 环境与独立授权 | none | 生产就绪时必须 | `PRODUCTION_READINESS_GATE` | 4 |
| Reference-liveness | write-time target validation；无 lifecycle registry/FK | 理论 dangling ref；当前无实例证据 | 生命周期模型、真实数据/删除证据 | 预计 migration + Repository | 当前不需要 | `DEFERRED_INDEPENDENT_CAPABILITY` | 5 |
| Retention/lifecycle | V15 无 delete/retention；reference status 存在但无 lifecycle executor | 未来规模/合规压力；误删可破坏证据 | reference-liveness、安全 cutoff、规模证据 | Repository；可能 migration | 当前不需要 | `PRODUCTION_READINESS_GATE` | 6 |
| Feedback evolution/case learning | attribution/read model 已有；scoring/reconciliation/holdout/promotion/lineage 未形成 | 过早启用会恢复隐式 learning | 明确 workflow、human approval、holdout | 未来可能 migration/Repository | 当前禁止 | `DEFERRED_INDEPENDENT_CAPABILITY` | 3 |
| Agent Runtime prerequisite | skeleton/fake tool/checkpoint 存在；permission/context/snapshot/resume/sandbox 合同不完整 | 直接 runtime 会越过权限与资源边界 | 独立 contract-first stage | 当前不应触发 | 未来前置 | `AGENT_PHASE_PREREQUISITE` | 7 |
| NQ-DH limited runtime planning | DH inbound dry-run default-disabled；无当前 cross-repo authority | 污染 NQ dev 或误启真实连接 | 独立 NQ worktree/authority/Integration plan | 可能 contract/API review | 当前禁止 | `CROSS_REPO_INTEGRATION_WORKSTREAM` | 8 |
| Platform/operational hardening | CI/wrapper 可用；少数薄模块与文档治理成本；无 active P2 | 可维护性与运维成熟度，不是当前数据风险 | 独立审计与具体故障证据 | 不确定 | 次于已确认原子性 | `AMBIGUOUS` | 2 |

## 6. Selected next stage freeze

### SELECTED_WORKSTREAM

`FEEDBACK_ENVELOPE_EVENT_ATOMIC_PERSISTENCE`

### NEXT_STAGE_NAME

`DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY`

### NEXT_STAGE_TYPE

`DH_OWNED / PLATFORM_HARDENING / DATA_CONSISTENCY_AND_SECURITY_BOUNDARY`

### CORE_OBJECTIVE

保持现有 feedback API、auth、validation、routing、ingest-only 和 idempotency wire semantics，同时保证一次
accepted ingestion 的 envelope 与 legacy event 是一个原子结果：要么都可见，要么都不可见且可安全重试；
duplicate 必须只代表完整的既有结果，不能掩盖 partial state。

### ACTIVE_GAPS_CLOSED

- envelope save 与 event append 跨独立操作，缺少原子边界。
- append failure 后 envelope 残留导致重试短路。
- duplicate/commit-unknown 不能证明 aggregate 完整。
- 缺少真实 PostgreSQL failure、rollback、concurrency 和 recovery 回归。

### PREREQUISITES

- closed milestone、archive 与 tag 保持 immutable。
- implementation work order 必须冻结精确 production/test allowlist 和选择的 transaction/repository contract。
- B1 Repository + security boundary review 通过前不得进入实现。
- 若证明必须 migration 或 API change，停止并进入 scope expansion review，不得在本阶段静默扩大。

### BATCHES_3_TO_5

1. **B1 — Atomic ingestion contract and review**：冻结完整结果、duplicate、rollback、commit-unknown、
   in-memory/JDBC parity、transaction ownership 与 failure taxonomy；执行 Repository/security boundary review。
2. **B2 — Atomic persistence implementation**：在 DH 内部实现统一 atomic boundary，调整 ingestion/router/
   repository/wiring 的最小必要合同；保持两类现有写入和 schema/API 不变，禁止 learning side effect。
3. **B3 — Failure, concurrency and PostgreSQL regression**：覆盖 append failure rollback、safe retry、same-event
   concurrency、duplicate completeness、commit-unknown、in-memory parity、Spring production-equivalent wiring 与
   PostgreSQL/Testcontainers；增加 architecture guards。
4. **B4 — Acceptance and final close**：执行 targeted/full regression、quality 与 exact security diff，形成
   self-contained archive、close commit、exact-SHA CI、annotated tag、远端验证和 post-tag current cleanup。

普通 B2/B3 不做 standalone review；B1 的 Repository/security boundary、任何 P0/P1、意外 migration/API 和
B4 stage close 才触发 review。

### PRODUCTION_WRITE_BOUNDARY

- 仅现有 `dh_nq_feedback_events` 中的 envelope row 与 legacy event row。
- 不新增表、列、索引、outbox、scheduler 或后台补偿任务。
- 不写 V15 structured attribution、Experience/Pheromone/FailureCase、candidate、prompt/model/provider、NQ。
- 不读取或修复未知生产数据；仓库没有真实受影响数据证据。

### TEST_WRITE_BOUNDARY

- 仅与 ingestion、repository、wiring、controller compatibility、architecture 和 PostgreSQL failure injection
  直接相关的 tests/fixtures。
- 不修改 golden cases、外部 contracts 或 NQ tests。

### SECURITY_BOUNDARIES

- `INGEST_ONLY / NO_IMPLICIT_LEARNING / NO_MUTABLE_LEARNING_STORE_ACCESS` 保持不变。
- 不降低 HMAC、timestamp、nonce、tenant/source/header binding、payload cap、rate limit 或 validation。
- partial state、store unavailable 与 commit-unknown 均 fail-closed；不得返回 false success。
- 不以自动 retry 掩盖 commit-unknown；只允许有界、可证明的 read-only reconciliation。
- 不访问 secret、真实 HTTP/Provider、NQ DB/runtime，不启动 Agent/LangGraph/Paper/LIVE。

### NON_GOALS

- 不实施 feedback learning、case promotion、reference-liveness、retention 或 capacity execution。
- 不连接 V15 structured attribution，不改变 historical evidence read model。
- 不修改 endpoint、request/response DTO、OpenAPI/JSON Schema/contracts 或 status code semantics。
- 不修改 Flyway migration，不做历史数据 backfill，不删除 legacy event compatibility row。
- 不重构无关模块，不合并 NQ/integration workstream，不引入 LangGraph。

### IMPACT

```text
MIGRATION_IMPACT: NONE / ANY NEED TRIGGERS SCOPE EXPANSION REVIEW
API_IMPACT: NONE / WIRE CONTRACT MUST REMAIN STABLE
REPOSITORY_IMPACT: YES / MINIMAL ATOMIC CONTRACT OR TRANSACTION BOUNDARY / B1 REVIEW REQUIRED
NQ_IMPACT: NONE
PROVIDER_IMPACT: NONE
AGENT_IMPACT: NONE / CLOSED LEARNING BOUNDARY PRESERVED
```

### TEST_MATRIX

- Unit：accepted/rejected/duplicate；event projection；save/append failure；partial-state rejection。
- Transaction：append failure rolls back envelope；both writes commit together；transaction boundary unavailable
  fail-closed；commit-unknown 不自动重放写。
- Idempotency：same event complete duplicate；partial row 不得 duplicate-success；safe retry；concurrent single winner。
- Persistence：in-memory parity；JDBC/PostgreSQL 17 Testcontainers；Spring wiring 使用同一 transaction resource。
- Compatibility：Controller auth/header/rate/validation/status/response 不变；八个 event types 保持 append-only。
- Architecture：inbound graph 不触达 mutable learning、V15 structured attribution、Provider/NQ/Agent/LangGraph。
- Regression：相关 module tests、`mvn -B -ntp test`、`mvn -B -ntp -Pquality validate`。

### REVIEW_TRIGGERS

- B1 Repository/transaction contract 与 security boundary freeze。
- 任何 P0/P1 或无法区分 partial/complete duplicate 的证据。
- 任何 migration、API/Controller、contract、NQ 或 production-data repair 需求。
- B4 stage final close。

### ROLLBACK

每个 implementation batch 使用独立普通 commit；失败时用 ordinary `git revert` 回退。无 schema/API 变更，
不需要数据库 down migration 或 wire rollback。禁止 reset/rebase/force push；tag 创建后不得移动或重建。

### FINAL_CLOSE_CONDITION

- atomic all-or-nothing、safe retry、complete duplicate 与 commit-unknown acceptance 全部通过。
- ingest-only/no-learning boundary 与 API/schema compatibility 全部通过。
- targeted/full regression、PostgreSQL/Testcontainers、quality、security exact diff 全部通过。
- active P0/P1 与 publication-blocking P2 为 0。
- archive packet、close commit、exact-SHA CI、annotated tag、remote verification、post-tag cleanup 全部完成。

### ARCHIVE_TAG_DISCIPLINE

严格执行 `acceptance -> archive packet -> close commit -> exact-SHA CI -> annotated tag -> remote verification ->
post-tag cleanup`。Archive-before-tag；tag target 必须包含完整 packet；cleanup 不改写历史 snapshot。

### NEXT_TASK

`DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-IMPLEMENTATION-WORK-ORDER`

该 next task 只允许 complete discovery、精确 allowlist、transaction/repository contract 和 batch command freeze；
不得在同一任务开始技术实现。

## 7. Deferred and forbidden capabilities

```text
ALLOW_ENVELOPE_ATOMICITY_FIX_NOW: NO
ALLOW_FEEDBACK_LEARNING_NOW: NO
ALLOW_CASE_PROMOTION_NOW: NO
ALLOW_REFERENCE_LIVENESS_NOW: NO
ALLOW_RETENTION_NOW: NO
ALLOW_CAPACITY_EXECUTION_NOW: NO
ALLOW_NQ_RUNTIME_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 8. Planning-task validation

```text
Baseline branch / HEAD / origin/dev: dev / 071bc29ee3c03b4099623c4ade441783cc22091f / SAME
Ahead / behind before write: 0 / 0
Worktree / staged before write: clean / empty
Closed-milestone terminal factsources: PASS / 12 OF 12 / ONE HASH / 0 CONFLICTS
Planning-authority write-scope sources: PASS / 8 OF 8 / ONE HASH
Scope invariants: PASS / 3 OF 3
Unexpected files: 0
Forbidden technical diff: 0
Archive diff: 0
Tag verification: LOCAL + REMOTE PEELED TARGET VERIFIED / UNCHANGED
Quality validate: PASS / EXIT 0 / 19 OF 19 REACTOR SUCCESS
Checkstyle: 0 VIOLATIONS
Spotless: PASS
Full tests this planning task: NOT_RERUN
Reused CI evidence: 30701063741 / 30702114843 / 30702569171
Reused regression evidence: 1252 / 0 / 0 / 0 / POSTGRESQL 17.10
Evidence disposition: HISTORICAL REUSE / NOT THIS-TASK EXECUTION / NOT CAPACITY ACCEPTANCE
Staged before commit: EMPTY
Local commit: THIS_PLANNING_COMMIT / LOCAL ONLY
Push / tag: NOT EXECUTED / NOT EXECUTED
```
