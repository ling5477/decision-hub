# DH Post-Stage-QDR-9 Next Stage Plan

> Task: `DH-POST-STAGE-QDR-9-NEXT-STAGE-PLANNING`
> Date: `2026-08-01`
> Type: `PLANNING_ONLY / POST_STAGE_TRANSITION_REVIEW / DOCS_ONLY_CHANGE`
> Implementation: `NOT AUTHORIZED`

## 1. Planning decision

```text
POST_STAGE_QDR_9_NEXT_STAGE_PLAN: DONE
STAGE_QDR_9_FINAL_STATE: CLOSED / ACCEPTED / ARCHIVED / TAGGED
SELECTED_NEXT_WORKSTREAM: PLATFORM_HARDENING / FEEDBACK_SIDE_EFFECT_CONTAINMENT
SELECTED_NEXT_STAGE: DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT
NEXT_STAGE_TYPE: DH_OWNED / PLATFORM_HARDENING / SECURITY_BOUNDARY_CONTAINMENT
NEXT_STAGE_SCOPE: FROZEN
NEXT_STAGE_BATCH_COUNT: 4
ALLOW_NEXT_STAGE_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_NEXT_STAGE_IMPLEMENTATION_NOW: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

本计划不沿用或自动创建 `Stage-QDR-10`。下一阶段只处理 DH 当前代码中已存在的 legacy feedback
隐式副作用路径，不恢复 Stage-QDR-9，不实施 reference-liveness、retention、capacity、NQ runtime、
Agent、LangGraph、Paper 或 LIVE。

## 2. Verified baseline

```text
Repository: E:/Project/decision-hub
Branch: dev
HEAD / origin/dev / advertised SHA: ddaf7c37e772dcd798d4631eabe0471d26527756
Ahead / behind: 0 / 0
Worktree / staged before write: clean / empty
Stage-QDR-9 close commit: 88b1d6d8ea68c39eaa74486e5e0bcb6502e00036
Stage-QDR-9 cleanup commit: ddaf7c37e772dcd798d4631eabe0471d26527756
Close tag: dh-stage-qdr-9-close / LOCAL+REMOTE PEELED TARGET VERIFIED
Archive direct artifacts: 15 / 15
Archive source copies: 31 / 31
SHA256SUMS: 44 entries / 0 failures
Current Stage-QDR-9 process residue: 0
Technical baseline: c7f940c0c48900a0cfb7eac86aac745c8006629c
Technical diff from baseline through cleanup: 0
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity: NOT_PROVEN
```

`STATUS.md`、`WORK_ORDER.md` 等文件中的 `THIS_CLEANUP_COMMIT / PENDING` 是提交时历史快照；本轮
实测的 cleanup SHA、origin/dev 对齐和既有 exact-SHA CI `30642095807 / PASS` 已证明完成，不构成
current blocker，也不允许 amend、tag rewrite 或历史修订。

## 3. Task scope design

### READ_SCOPE

- Current authority：任务书列出的 root/current 文档。
- Close evidence：`docs/gates/stage-qdr-9/{README.md,FINAL_CLOSE_REVIEW.md,SOURCE_MANIFEST.md}`。
- Code reality：任务书列出的 12 个模块 `src/main`、全部 Flyway migration、相关 tests、CI workflow、
  root `pom.xml`，以及 capacity 稳定机器合同。

### WRITE_ALLOWLIST

- `docs/current/DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md`
- `docs/current/{README,STATUS,WORK_ORDER,ROADMAP,TESTING,WORKLOG,CODEX_PROJECT_INSTRUCTIONS}.md`
- root `README.md`
- `FACTSOURCE_POLICY.md` / `ARCHIVE_INDEX.md` 仅在 policy 必须变化时可写；本轮不需要变化。

### VALIDATION_SCOPE

- 全部 WRITE_ALLOWLIST 文档、技术目录、tests、migration、contracts、golden cases、POM、workflow、
  Stage-QDR-9 archive 和 close tag。

### FIXABLE_BLOCKER_SCOPE

- 仅 WRITE_ALLOWLIST；任何实现、archive、tag 或 policy 外问题均停止并返回 scope review。

### CURRENT_FACTSOURCE_SCAN_SCOPE

- 本轮允许同步的 root/current 文档；`AGENTS.md`、`CLAUDE.md` 只读且继续显式服从
  `STATUS.md + WORK_ORDER.md` 的 primary authority。

### CODE_REALITY_REVIEW_SCOPE

- Decision pipeline、audit/trace/snapshot、replay/evaluation、provider trust/readiness、feedback
  attribution/historical evidence、legacy NQ feedback、capacity、Agent prerequisite、module/tooling/CI。

### CANDIDATE_WORKSTREAM_SCOPE

- Reference-liveness、retention/lifecycle、formal capacity、NQ-DH dry-run readiness、Agent contract、
  feedback/case evolution、platform hardening。

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
TECHNICAL_PATHS_IN_WRITE_ALLOWLIST: 0
```

## 4. Code reality findings

已实现并有代码/测试证据的基础能力：

- `DefaultDecisionOrchestrator` + `DecisionOutput`：只读结构化决策、fail-closed、禁止交易动作。
- `JdbcDecisionAuditRepository`、canonical snapshot 与 deterministic replay：audit/trace/snapshot/replay
  内部闭环，不执行真实 provider/NQ/Agent。
- V9 replay/evaluation、`QdrRegressionEvaluationService`：mock-only regression baseline。
- provider trust/health/readiness：内部 policy/read model，真实 provider 与 HTTP 均不存在。
- V15 + feedback usecase/JDBC：tenant/environment-bound structured attribution、原子持久化、internal-only
  historical evidence keyset read model。
- persistent guard、rate limit、bounded memory、capacity harness：实现存在；formal capacity 尚未执行。

当前最高价值缺口是 legacy feedback side-effect boundary：

1. `NqFeedbackController` 通过 `DefaultNqFeedbackIngestionService` 派发已接受事件。
2. `AbstractNqFeedbackEventHandler` 对每个事件执行 `experienceFeedbackService.apply(event)`。
3. `DefaultExperienceFeedbackService` 自动更新 `ExperienceStore`、`PheromoneStore`，失败时写
   `FailureCaseStore`；`AgentRuntimeWiringConfig` 无独立 enable gate 地装配该链路。
4. 现有回归 `ResearchRunStage1ClosedLoopTest` 与 `NqFeedbackHandlerDispatchTest` 明确要求 feedback
   自动强化 experience/pheromone。
5. Stage-QDR-8/9 新 feedback 包的 architecture guard 只保护新包不接 learning/state mutation，未隔离
   上述 legacy ingress path。current authority 又明确未授权 Agent/autonomous feedback loop。

该差异不证明 NQ、真实交易或 production mutation 已发生；当前 stores 是 DH 内部实现。但它使“反馈接收”
与“隐式状态学习”耦合，属于应先收口的 DH security boundary。Accepted Stage-QDR-9 authority 中 active
P0/P1 仍为 `0/0`；本结论是 post-stage planning 新识别的 security-boundary gap，不改写已关闭 Stage。

## 5. Candidate assessment

| 候选方向 | 当前代码证据 | 当前状态 | 依赖 | 主要价值 | 安全风险 | migration/API 影响 | 是否现在需要 | 推荐顺序 |
|---|---|---|---|---|---|---|---|---|
| Reference-liveness | V15 有 opaque reference 与 write-time validation；无 registry、无 dangling ref 实证 | `DEFERRED_INDEPENDENT_CAPABILITY` | 生命周期与真实规模证据 | 为未来安全清理提供引用存活判断 | 误判可删除审计/回放证据 | 预计需 migration + Repository | 否；无 active blocker | 5 |
| Retention / lifecycle | feedback aggregate 无 delete/retention；仅 guard cleanup 已实现 | `PRODUCTION_READINESS_GATE` | reference-liveness、合规/规模/截止策略 | 控制长期存储 | 误删 immutable evidence | 预计需 Repository；可能需 migration | 否；无规模或合规硬证据 | 6 |
| Formal capacity / resource safety | criteria、profile、scripts、15-scenario harness 已存在；formal run 未执行 | `PRODUCTION_READINESS_GATE` | 明确 production-readiness 目标和受控环境 | 证明本地 operational ceiling | 错把普通 CI 当 capacity | 无 API/migration | 非当前功能 blocker | 4 |
| NQ-DH limited dry-run readiness | DH 有 default-disabled inbound dry-run；无 real client/HTTP，缺 active cross-repo authority | `CROSS_REPO_INTEGRATION_WORKSTREAM` | 独立 NQ authority/worktree/contract planning | 验证跨仓只读边界 | 污染 NQ dev、误启 runtime | 可能触发 API/contract review | 否；必须独立规划 | 7 |
| Agent Runtime Contract Baseline | decision/audit/replay/approval/failure classification 部分具备；tool permission 与 context budget 合同缺失 | `AGENT_PHASE_PREREQUISITE` | side-effect containment、tool/context/checkpoint/human approval freeze | 为 future Agent phase 提供边界 | 过早引入 runtime/LangGraph | 尚不应触发 API/migration | 否 | 3 |
| Feedback learning / historical case evolution | attribution + historical evidence 已实现；holdout/correction/promotion/bounded learning 未实现 | `AGENT_PHASE_PREREQUISITE` | 先隔离 legacy automatic mutation | 建立可审查 case evolution | 形成在线自修改或数据泄漏 | 未来可能需 migration；本轮不授权 | 否；不能越过 containment | 2 |
| Feedback side-effect containment | ingress handler 与 wiring 直接触达 Experience/Pheromone/FailureCase；新旧反馈边界不一致 | `ACTIVE_NEXT_STAGE_CANDIDATE` | 无跨仓依赖 | 让 ingest 与 learning 解耦，恢复 default-deny | 修改既有 legacy closed-loop 行为 | `NO_MIGRATION / NO_API_CHANGE` | 是；先于任何 evolution/Agent | 1 |
| V16/V17/V18 legacy sequence | 仅 archive 中存在 historical/superseded design，当前树最高 V15 | `HISTORICAL_OR_SUPERSEDED` | 无 | 无 current value | 误重开 Stage-QDR-9 | migration | 否 | 不排期 |

## 6. Selected stage freeze

### NEXT_STAGE_NAME

`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT`

### CORE_OBJECTIVE

保持 NQ feedback ingest 的现有认证、校验、幂等和响应合同，同时让 inbound feedback 默认只能记录/审计，
不能隐式更新 Experience/Pheromone/FailureCase、candidate、prompt/model/provider 或任何 Agent 状态。

### ACTIVE_GAPS_CLOSED

- legacy ingress 对 `ExperienceFeedbackService` 的无条件调用。
- `AgentRuntimeWiringConfig` 中 ingress 与 mutable learning service 的直接装配。
- 新 structured feedback “无 learning mutation”边界未覆盖 legacy ingress 的 architecture gap。
- 以旧 closed-loop 测试继续固化隐式学习行为的回归合同。

### PREREQUISITES

- Stage-QDR-9 保持 closed/tagged 且技术 baseline 不变。
- implementation work order 必须完成精确 write allowlist 与 compatibility impact 列表。
- 安全边界 review 必须在 B1 完成；未通过前不得进入 B2。

### BATCHES_3_TO_5

1. **B1 — Feedback disposition contract 与 security boundary review**：冻结 `INGEST_ONLY / NO_IMPLICIT_LEARNING`
   default-deny 语义、兼容范围、错误处置和 architecture rules；这是 security-boundary review trigger。
2. **B2 — Runtime side-effect decoupling**：移除 NQ feedback handler / integration wiring 对 mutable learning
   service 的隐式调用；保持 API、auth、rate limit、idempotency、event persistence 与 response 不变。
3. **B3 — Regression and architecture guards**：重写 legacy closed-loop tests 为 ingest-only 合同，增加依赖扫描、
   application-context 与 WebMvc regressions，证明 feedback path 不触达 mutable learning/Agent/Provider/NQ。
4. **B4 — Acceptance and stage final close**：执行 targeted/full regression、quality、boundary scan，形成 archive
   packet、close commit、exact-SHA CI、annotated tag、remote verification 与 post-tag current cleanup。

普通 B2/B3 不做 standalone review；只有 B1 security boundary、P0/P1、意外 API/migration 和 B4 stage close
触发 review。

### WRITE_BOUNDARIES

Implementation work order 只能在以下边界内细化精确文件：

- `dh-usecase/.../usecase/agent/feedback/**`
- `dh-usecase/.../usecase/agent/impl/DefaultNqIntegrationUseCase.java`（仅兼容隔离所需）
- `dh-app/.../config/AgentRuntimeWiringConfig.java`
- 上述路径的相关 tests、`dh-app/.../ArchitectureTest.java`
- current planning/validation/close docs 与未来本阶段 archive packet

禁止修改 `dh-api`、contracts、OpenAPI、Flyway、Repository/JDBC、QDR8/9 structured feedback domain/persistence、
provider、connector、NQ 仓库或其他 Agent 功能。若实施发现必须扩大上述边界，停止并进入 scope review。

### SECURITY_BOUNDARIES

- feedback ingest 默认 `INGEST_ONLY`，不存在隐式 learning/promotion。
- 不降低 HMAC、timestamp、nonce、tenant binding、payload cap、rate limit 或 idempotency。
- 不将 raw payload、credential、provider material 或交易字段送入 mutable memory。
- 不调用真实 HTTP/provider/NQ，不启动 Agent/LangGraph，不生成交易动作。
- 不以 feature flag 偷渡 enabled learning；未来 learning 必须独立 stage + human approval + holdout/promotion policy。

### NON_GOALS

- 不实现 case retrieval、quality scoring、outcome reconciliation、human correction、holdout、promotion 或 lineage。
- 不实施 reference-liveness、retention 或 formal capacity。
- 不修改 API、migration、Repository、contracts、NQ 或 provider。
- 不删除整个 legacy Agent skeleton，不做模块合并、wrapper repair 或无关重构。

### IMPACT

```text
MIGRATION_IMPACT: NONE
API_IMPACT: NONE / WIRE CONTRACT MUST REMAIN STABLE
NQ_IMPACT: NONE / NO NQ REPOSITORY OR RUNTIME
PROVIDER_IMPACT: NONE / NO REAL PROVIDER OR HTTP
AGENT_IMPACT: CONTAINMENT ONLY / NO AGENT PHASE / NO LANGGRAPH
```

### TEST_MATRIX

- Unit：accepted/duplicate/rejected feedback；handler 记录事件但不调用 mutable learning service。
- Compatibility：现有 API response、auth/header/rate/idempotency 语义不变。
- Context：Spring wiring 中 feedback ingress 不依赖 `ExperienceFeedbackService`、`ExperienceStore`、
  `PheromoneStore` 或 `FailureCaseStore`。
- Architecture：legacy + structured feedback path 均禁止到 candidate/prompt/provider/Agent/LangGraph/NQ mutation。
- Safety：forbidden material、tenant isolation、fail-closed 失败路径。
- Regression：相关 module tests、全 reactor `mvn -B -ntp test`、`mvn -B -ntp -Pquality validate`。
- Database：复用现有 schema；不得新增或修改 migration。

### REVIEW_TRIGGERS

- B1 security boundary freeze。
- 任何 P0/P1 blocker。
- 任何 migration、API/Controller、contract 或 Repository/JDBC 需求（立即 scope expansion review）。
- B4 stage final close。

### ROLLBACK

每个 batch 使用独立普通 commit；失败时用 ordinary `git revert` 回退对应 implementation commit。无 schema/API
变更，因此无数据回填或 wire rollback。不得 reset/rebase/force push；stage tag 创建后不得移动或改写。

### FINAL_CLOSE_CONDITION

- ingress-only 与 no-implicit-learning acceptance 全部通过。
- API/migration/Repository/technical out-of-scope diff 为 0。
- targeted + full regression、quality、security boundary scan 全部通过。
- self-contained archive packet、close commit、exact-SHA CI、annotated tag、remote verification 和 post-tag
  cleanup 全部完成。

### ARCHIVE_TAG_DISCIPLINE

严格执行 `acceptance -> archive packet -> close commit -> exact-SHA CI -> annotated tag -> remote verification ->
post-tag cleanup`；archive-before-tag，tag target 必须包含完整 packet，cleanup 不改写历史 snapshot。

### NEXT_TASK

`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-IMPLEMENTATION-WORK-ORDER`

该 next task 只允许继续做 work-order/discovery freeze，不授权 implementation。

### WORK_ORDER_HANDOFF_RESULT

```text
WORK_ORDER: DONE
DOCUMENT: docs/current/DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md
CALL_CHAIN_INVENTORY: PASS
SECURITY_CONTRACT: INGEST_ONLY / NO_IMPLICIT_LEARNING / FROZEN
PRODUCTION_AND_TEST_ALLOWLISTS: FROZEN
API / MIGRATION / REPOSITORY IMPACT: NONE / NONE / NONE
NEXT_TASK: DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-CONSOLIDATED-IMPLEMENTATION
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_IN_WORK_ORDER_TASK: NO
```

该 handoff 不改写本计划的候选评估或 Stage-QDR-9 closed authority；它只记录 work-order task 已消费完成。

## 7. Deferred classification

```text
ALLOW_REFERENCE_LIVENESS_IMPLEMENTATION_NOW: NO
ALLOW_RETENTION_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_EXECUTION_NOW: NO
ALLOW_NQ_RUNTIME_INTEGRATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```
