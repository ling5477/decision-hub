# DH Platform Hardening Feedback Side-Effect Containment Implementation Work Order

> Task: `DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-IMPLEMENTATION-WORK-ORDER`
> Date: `2026-08-01`
> Type: `WORK_ORDER_ONLY / SECURITY_BOUNDARY_IMPLEMENTATION_DESIGN / DOCS_ONLY_CHANGE`
> Technical implementation in this task: `NOT AUTHORIZED / NOT EXECUTED`

## 1. Work-order decision

```text
FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER: DONE
CALL_CHAIN_INVENTORY: PASS
IMPLICIT_MUTATION_CONFIRMED: YES
INGEST_ONLY_CONTRACT: FROZEN
EXPLICIT_LEARNING_MUTATION_BOUNDARY: FROZEN
PRODUCTION_WRITE_ALLOWLIST: FROZEN
TEST_MATRIX: FROZEN
API_CHANGE_REQUIRED: NO
MIGRATION_REQUIRED: NO
REPOSITORY_EXPANSION_REQUIRED: NO
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

本工单把后续代码执行压缩为同一个 implementation task 内的 B1–B3。B4 仍是独立 milestone
final-close task。本轮只完成 discovery、依赖边界、精确文件和验证矩阵冻结，没有修改任何 Java、测试、
API、migration、Repository、POM、workflow、NQ、Provider、Agent 或 LangGraph runtime。

## 2. Verified baseline

```text
Repository: E:/Project/decision-hub
Branch: dev
HEAD / plan commit: 241663f3ba60cb4d7273bf3b2374ec79509b7cf4
HEAD parent: ddaf7c37e772dcd798d4631eabe0471d26527756
origin/dev: ddaf7c37e772dcd798d4631eabe0471d26527756
Ahead / behind: 1 / 0
Worktree / staged before write: clean / empty
Stage-QDR-9: CLOSED / ACCEPTED / ARCHIVED / TAGGED / NOT REOPENED
Selected workstream: PLATFORM_HARDENING / FEEDBACK_SIDE_EFFECT_CONTAINMENT
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity: NOT_PROVEN
```

该状态符合任务书允许的状态 A。禁止 pull、merge、rebase、reset、amend、stash、revert、push 或 tag。

## 3. Scope design

### 3.1 READ_SCOPE

- 当前权威：本工单、`DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md`、root/current authority docs。
- 生产代码：legacy NQ feedback Controller/request/response、认证、header binding、限流、幂等、
  ingestion、validator、router、8 个 handler、Spring wiring、legacy feedback repository、
  `ExperienceFeedbackService` 与三个 mutable stores。
- structured evidence：QDR feedback attribution、audit reference、V15 internal persistence wiring。
- 测试证据：legacy feedback unit/WebMvc、Spring context、architecture、Stage1 closed-loop 与 QDR feedback tests。
- 边界扫描：所有 `dh-*/src/main/**` 与 `dh-*/src/test/**` 中四个 mutable 类型及其调用点；
  listener、scheduler、transaction callback 和 async callback token。

### 3.2 本轮 WRITE_ALLOWLIST

- `docs/current/DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md`
- `docs/current/DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md`
- `docs/current/STATUS.md`
- `docs/current/WORK_ORDER.md`
- `docs/current/ROADMAP.md`
- `docs/current/TESTING.md`
- `docs/current/WORKLOG.md`
- `docs/current/CODEX_PROJECT_INSTRUCTIONS.md`
- `docs/current/README.md`
- `README.md`

### 3.3 VALIDATION_SCOPE

- 本轮 WRITE_ALLOWLIST。
- `dh-domain/src`、`dh-usecase/src`、`dh-security/src`、`dh-api/src`、`dh-app/src`、`dh-infra/src`、
  `dh-providers/src`、`dh-memory/src`、`dh-knowledge/src`、`dh-eval/src`、`dh-connector/src`、
  `dh-scheduler/src`、`contracts`、`golden_cases`、Flyway migration、root `pom.xml`、`.github`、
  `docs/gates/stage-qdr-9`。

### 3.4 FIXABLE_BLOCKER_SCOPE

- 仅本轮 WRITE_ALLOWLIST。任何代码、test、API、migration、Repository、contract 或 archive 问题均只记录，
  不在本任务修复。

### 3.5 CURRENT_FACTSOURCE_SCAN_SCOPE

- 本轮 WRITE_ALLOWLIST 中的 root/current docs。`AGENTS.md`、`CLAUDE.md`、`FACTSOURCE_POLICY.md` 与
  `ARCHIVE_INDEX.md` 只读，不因扫描范围大于写范围而制造 scope conflict。

### 3.6 专项 discovery scopes

```text
FEEDBACK_INGRESS_CALL_CHAIN_SCOPE:
  protected endpoint -> auth/rate/header/HMAC -> ingestion -> validator/repository/router -> handlers
MUTABLE_LEARNING_CALLSITE_SCOPE:
  all production/test references to ExperienceFeedbackService and three mutable stores
SPRING_WIRING_SCOPE:
  SecurityWiringConfig + AgentRuntimeWiringConfig + Controller component wiring
TEST_MATRIX_SCOPE:
  unit + Spring context + WebMvc/security + architecture + full regression
```

```text
VALIDATION_SCOPE subset of READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset of WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset of WRITE_ALLOWLIST: PASS
TECHNICAL_PATHS_IN_THIS_TASK_WRITE_ALLOWLIST: 0
```

## 4. Current production call chain

### 4.1 HTTP、认证与 admission

```text
POST /api/ai/feedback/nq
-> DhApiAuthenticationFilter#doFilterInternal
   -> TokenVerifier#verify
   -> AuthContext + authenticated tenant request attributes
-> IdempotencyFilter#doFilterInternal (POST + optional Idempotency-Key, after authentication)
-> NqFeedbackController#receive
   -> RateLimiter#check(source, tenant, NQ_FEEDBACK, now)
   -> NqFeedbackAuthenticator#authenticate
      -> HmacNqFeedbackAuthenticator#authenticate
      -> source allowlist + payload cap + timestamp + signature + nonce/requestId replay guard
   -> NqDhHeaderValidator#validate
      -> canonical tenant/requestId/traceId binding
   -> NqFeedbackIngestionService#ingest
```

### 4.2 Ingestion 与隐式 mutation

```text
NqFeedbackController#receive
-> DefaultNqFeedbackIngestionService#ingest
   -> NqFeedbackEventRepository#findEnvelopeByEventId
   -> DefaultNqFeedbackContractValidator#validate
   -> NqFeedbackEventRepository#saveEnvelope
   -> DefaultNqFeedbackEventTypeRouter#route
      -> one of 8 AbstractNqFeedbackEventHandler subclasses
         -> AbstractNqFeedbackEventHandler#handle
            -> NqFeedbackEventRepository#append
            -> ExperienceFeedbackService#apply
               -> DefaultExperienceFeedbackService#apply
                  -> ExperienceStore#findByKey / save
                  -> PheromoneStore#find / save
                  -> FailureCaseStore#record (negative feedback only)
```

8 个真实 handler 为：

1. `PaperRunCreatedHandler`
2. `PaperRunStartedHandler`
3. `PaperRunStoppedHandler`
4. `PaperRunDailyReportGeneratedHandler`
5. `PaperRunAlertRaisedHandler`
6. `PaperRunRecoveryEventRecordedHandler`
7. `PaperRunStabilityCheckCompletedHandler`
8. `BacktestResultReadyHandler`

### 4.3 调用点证据矩阵

| caller | callee / method | Spring wiring | mutation | transaction | tenant/environment | coverage | classification |
|---|---|---|---|---|---|---|---|
| `NqFeedbackController#receive` | `NqFeedbackIngestionService#ingest` | Controller constructor -> `nqFeedbackIngestionService` bean | envelope/event path downstream | 无显式 transaction | tenant 来自 `AuthContext`；legacy command 无 environment 字段 | `NqFeedbackControllerWebMvcTest` | `INBOUND_REQUEST_PATH` |
| `DefaultNqFeedbackIngestionService#ingest` | validator/repository/router | `AgentRuntimeWiringConfig#nqFeedbackIngestionService` | `saveEnvelope` 后派发 | 无显式 transaction；save 与 handler append 非原子 | command tenant 传给 router；environment 不参与 legacy contract | `NqFeedbackIdempotencyTest`、`NqFeedbackContractValidationTest` | `INBOUND_REQUEST_PATH` |
| `AbstractNqFeedbackEventHandler#handle` | repository append + `ExperienceFeedbackService#apply` | 8 个 handler bean 均构造注入 mutable service | 隐式 Experience/Pheromone；负反馈 FailureCase | 无显式 transaction | 用认证 tenant 创建 Stage1 event；无 environment | `NqFeedbackHandlerDispatchTest` | `UNAUTHORIZED_IMPLICIT_MUTATION` |
| `DefaultExperienceFeedbackService#apply` | three mutable stores | `experienceFeedbackService` bean | `ExperienceStore#save`、`PheromoneStore#save`、`FailureCaseStore#record` | 无显式 transaction | store key 含 tenant；无 environment | `ResearchRunStage1ClosedLoopTest` | `EXPLICIT_LEARNING_MUTATION_CAPABILITY / INBOUND-REACHABLE TODAY` |
| `DefaultNqIntegrationUseCase#onFeedback` | repository append + `ExperienceFeedbackService#apply` | `nqIntegrationUseCase` bean | 同上 | 无显式 transaction | event tenant；无 environment | 仅 `ResearchRunStage1ClosedLoopTest` | `DEAD_OR_UNUSED PRODUCTION BEAN / NO PRODUCTION CALLER / AMBIGUOUS MUTATION BYPASS` |

## 5. Complete mutable callsite inventory

### 5.1 Production

- `ExperienceFeedbackService#apply` 的生产调用点共 2 个：
  - `AbstractNqFeedbackEventHandler#handle`：真实 inbound path，必须移除。
  - `DefaultNqIntegrationUseCase#onFeedback`：无生产 caller，但已作为 Spring bean 装配；必须改为 append-only，
    不能保留为可被重新接入的隐式 mutation bypass。
- 三个 store 的生产写点只存在于 `DefaultExperienceFeedbackService`：
  - `experienceStore.save(entry)`
  - `pheromoneStore.save(edge)`
  - `failureCaseStore.record(...)`
- 三个 store 的实现 bean 仅在 `AgentRuntimeWiringConfig` 创建；未发现其他生产写 caller。
- 未发现引用四个 mutable 类型的 `@EventListener`、`@Scheduled`、transaction callback 或 async callback。

### 5.2 Tests

- `NqFeedbackHandlerDispatchTest` 目前显式断言 8 个 handler 调用 `ExperienceFeedbackService`，必须反转为
  ingest-only / zero-learning contract。
- `ResearchRunStage1ClosedLoopTest` 目前通过 `DefaultNqIntegrationUseCase` 固化自动 learning，必须拆分为：
  - feedback ingest/onFeedback 只 append、三个 stores 零变化；
  - 如需保留 mutable service 的既有单元语义，只允许直接调用 `ExperienceFeedbackService#apply`，明确标为
    test-only explicit mutation，不得通过 inbound adapter 触发。
- `NqFeedbackIdempotencyTest` 已覆盖 envelope eventId duplicate 与 handler 单次派发，需继续保持。
- `NqFeedbackControllerWebMvcTest` 与 `NqFeedbackRateLimitWebMvcTest` 已覆盖 auth/HMAC/header/rate/response；
  需新增 successful/rejected request 的 mutable-store zero-interaction 证明。

## 6. Existing security and compatibility reality

### 6.1 Authentication and rate limit

- bearer authentication 由 `DhApiAuthenticationFilter` 在 Controller 前执行。
- tenant 取自已验证 `AuthContext`；`X-DH-Tenant-Id` 不一致时 filter 返回 403。
- NQ canonical headers 由 Controller 解析；rate limit 在 HMAC 前执行。
- HMAC 保持 source allowlist、payload cap、UTC `Z` timestamp、nonce/requestId replay、constant-time signature。
- canonical tenant/requestId/traceId 若提供且与权威来源不一致，Controller 返回 403
  `HEADER_BINDING_MISMATCH`。

### 6.2 Idempotency

- optional HTTP `Idempotency-Key` 由认证后 `IdempotencyFilter` 按 tenant 隔离。
- HMAC nonce/requestId replay 由 `NonceReplayGuard` fail-closed。
- ingestion eventId duplicate 由 `NqFeedbackEventRepository#findEnvelopeByEventId/saveEnvelope` 返回
  `DUPLICATE`，HTTP 仍为 202 且 schema 不变。

### 6.3 Tenant and environment characterization

- legacy Controller 只读取 authenticated tenant；`IngestionCommand` 与 `NqFeedbackEnvelope` 没有 environment
  字段，legacy validator 也没有 environment-mismatch branch。
- `AuthContext` 可以携带 `FeedbackEnvironment`，但 legacy feedback Controller 未读取或推断它。
- 因此本 stage 只能冻结“现有 environment 行为不变”：不新增 wire 字段、不推断环境、不将 structured
  feedback 的 environment 合同强接到 legacy endpoint。相关测试必须是 characterization，不得虚构现有
  environment rejection。
- legacy validator 用 `ResearchRunRepository#find(traceId)` 验证 trace 存在，但不执行 run-tenant compare；
  本 stage 不改变该响应行为。若未来要增强该规则，必须另立 security scope，因为本工单只允许一项行为变化。

## 7. Structured feedback path decision

- `DefaultFeedbackAttributionService` 是 deterministic、tenant/environment-bound、audit fail-closed，且明确不接
  Experience/Prompt/Provider/NQ/Agent/LangGraph；但它没有 production Spring bean。
- production wiring 只装配 internal-only `FeedbackAttributionPersistenceService`，它使用 V15 JDBC Repository 与
  transaction boundary，且没有 Controller/runtime entry。
- 将 legacy endpoint 强接到该路径会引入新的 command mapping、structured persistence 语义和 Repository
  调用，超出 `NO_API_CHANGE / NO_REPOSITORY_CHANGE`。

结论：本 stage 不复用 structured attribution runtime。最小修复只保留现有 envelope + Stage1 event
ingestion/audit-like record，关闭 learning-store mutation；structured QDR path 保持 internal-only、不变。

## 8. Frozen security contracts

### 8.1 `INGEST_ONLY`

```text
INGEST_ONLY:
  authenticate bearer authority
  preserve authenticated tenant
  enforce rate limit before HMAC
  enforce source/timestamp/nonce/signature/payload cap
  preserve canonical header binding
  preserve HTTP Idempotency-Key, HMAC replay and eventId duplicate behavior
  validate legacy envelope and forbidden material
  save the existing envelope
  append the existing legacy feedback event
  return the existing ACCEPTED / DUPLICATE / REJECTED result
  NEVER call mutable learning service or stores
```

目标 no-side-effect application port 沿用现有 `NqFeedbackIngestionService`，不新增同义接口。B2 必须更新其
contract，使 `ingest` 的稳定语义明确为：

```text
INGEST_ONLY / NO_IMPLICIT_LEARNING / NO_MUTABLE_LEARNING_STORE_ACCESS
```

### 8.2 `EXPLICIT_LEARNING_MUTATION`

```text
EXPLICIT_LEARNING_MUTATION:
  capability owner: ExperienceFeedbackService / DefaultExperienceFeedbackService
  possible stores: ExperienceStore / PheromoneStore / FailureCaseStore
  inbound reachability after implementation: ZERO
  production callers after implementation: ZERO
  Spring bean may remain for compatibility, but no feedback ingress bean may depend on it
  enabling or adding an explicit manual/offline workflow: NOT AUTHORIZED IN THIS STAGE
```

### 8.3 Dependency direction

```text
NqFeedbackController
  -> NqFeedbackIngestionService
    -> validator + NqFeedbackEventRepository + router
      -> NqFeedbackEventHandler
        -> NqFeedbackEventRepository only

FORBIDDEN FROM INBOUND GRAPH:
  ExperienceFeedbackService
  DefaultExperienceFeedbackService
  ExperienceStore
  PheromoneStore
  FailureCaseStore
```

### 8.4 Spring boundary

- 新建独立 `FeedbackIngestionWiringConfig`，只装配 repository、validator、ObjectMapper、8 handlers、router、
  `NqFeedbackIngestionService` 与 append-only `NqIntegrationUseCase` compatibility bean。
- 从 `AgentRuntimeWiringConfig` 移走全部 inbound feedback beans，避免一个配置类同时拥有 inbound 与 mutable
  learning dependencies。
- `AgentRuntimeWiringConfig` 可保留三个 store 与 `ExperienceFeedbackService` bean，但它们不得被新的 inbound
  config 注入。
- 不允许以 boolean、profile、环境变量、feature flag、`if`、异常吞噬、event listener、scheduler、
  async callback 或 transaction callback 代替依赖隔离。

## 9. Future consolidated implementation write allowlists

### 9.1 PRODUCTION_WRITE_ALLOWLIST

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/NqIntegrationUseCase.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/impl/DefaultNqIntegrationUseCase.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIngestionService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackEventHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/impl/DefaultNqFeedbackIngestionService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/AbstractNqFeedbackEventHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunCreatedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunStartedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunStoppedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunDailyReportGeneratedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunAlertRaisedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunRecoveryEventRecordedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/PaperRunStabilityCheckCompletedHandler.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/handler/BacktestResultReadyHandler.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/AgentRuntimeWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/FeedbackIngestionWiringConfig.java (new)
```

`DefaultExperienceFeedbackService` 与三个 store 文件不在 allowlist：它们的 mutation 行为不改，只切断全部
feedback ingress caller。`DefaultNqFeedbackEventTypeRouter`、validator、repository 与 Controller 也不需修改。

### 9.2 TEST_WRITE_ALLOWLIST

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackHandlerDispatchTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIdempotencyTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackContractValidationTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/ResearchRunStage1ClosedLoopTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackControllerWebMvcTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackRateLimitWebMvcTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/FeedbackSideEffectContainmentWiringTest.java (new)
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

### 9.3 FACTSOURCE_WRITE_ALLOWLIST

```text
docs/current/DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
README.md
```

### 9.4 FORBIDDEN_PATHS

```text
dh-api/src/main/**
dh-domain/src/main/**
dh-security/src/main/**
dh-memory/src/main/**
dh-infra/src/main/**
dh-providers/**
dh-connector/**
dh-scheduler/**
dh-knowledge/**
dh-eval/**
dh-app/src/main/resources/**
**/db/migration/**
contracts/**
golden_cases/**
pom.xml
**/pom.xml
.github/**
docs/gates/**
AGENTS.md
CLAUDE.md
NQ repository or runtime
```

若实现必须修改任一 forbidden path，立即输出：

```text
FEEDBACK_CONTAINMENT_SCOPE_EXPANSION_REQUIRED
```

不得继续追加文件或修复相邻问题。

## 10. API and behavior compatibility freeze

禁止修改：endpoint path、HTTP method、request/response schema、error taxonomy、HMAC signature material、
timestamp、nonce、source allowlist、tenant/header binding、现有 environment characterization、rate limit、
payload cap、requestId、traceId、correlationId、HTTP `Idempotency-Key`、HMAC replay、eventId duplicate。

唯一允许的行为变化：

```text
successful legacy feedback ingest no longer mutates
ExperienceStore, PheromoneStore, or FailureCaseStore
```

`NqFeedbackEventRepository#saveEnvelope` 与 `append` 继续执行；不新增异步 mutation，不新增 event、scheduler、
callback 或 provider path。现有无显式 transaction 的行为不在本 stage 改造。

## 11. Consolidated implementation batches

### B1 — Contract and Call-Chain Freeze

- 重新验证 baseline 与本工单 callsite inventory。
- 在同一 implementation task 中冻结 `INGEST_ONLY / NO_IMPLICIT_LEARNING`，不创建独立 B1 review task。
- 检查 16 个 production paths、8 个 test paths 与 factsource allowlist 精确存在/新建。
- 重新执行全仓 mutable callsite 与 listener/scheduler/callback 扫描。
- 确认 `API / migration / Repository impact = NONE`；否则停止为 scope blocker。

### B2 — Side-Effect Containment Implementation

- 将 `NqFeedbackIngestionService` 固化为 no-side-effect application port。
- 从 `AbstractNqFeedbackEventHandler` 与 8 个子类移除 `ExperienceFeedbackService` 构造依赖和 `apply`。
- 保留 envelope save、legacy event append、validation、routing 和 response。
- 将 `DefaultNqIntegrationUseCase` 改为 append-only，消除第二个 production `apply` caller。
- 新建独立 `FeedbackIngestionWiringConfig`；从 `AgentRuntimeWiringConfig` 移走 inbound beans。
- 不修改 `DefaultExperienceFeedbackService` 或 stores，不启用任何 explicit learning workflow。

### B3 — Regression and Architecture Guards

- 执行本工单第 12 节 test matrix。
- 全仓证明 production `ExperienceFeedbackService#apply` caller 为 0；三个 store 只由未被 inbound 触达的
  `DefaultExperienceFeedbackService` 引用。
- 证明无 listener/scheduler/callback reintroduces mutation。
- 执行 targeted tests、full Maven regression、quality gate、forbidden diff 和最小 factsource 同步。
- B1–B3 全部通过后只创建一个本地 implementation commit，不 push、不 tag。

### B4 — Milestone Final Close（独立后续任务）

- implementation security review 与 exact diff review。
- 取得独立 push authorization 后才允许 push。
- exact-SHA CI、self-contained archive packet、archive close commit。
- archive-before-tag 后才允许 annotated tag：
  `dh-platform-hardening-feedback-side-effect-containment-close`。
- 验证 remote peeled target，随后执行 post-tag current cleanup。

## 12. Frozen test matrix

### 12.1 Unit

1. valid feedback request 保持 `ACCEPTED/RECEIVED`。
2. invalid feedback 继续 fail-closed 且不保存/不派发。
3. authenticated/canonical tenant mismatch 行为保持 403；不新增 run-tenant 语义。
4. environment characterization 保持：legacy wire 无 environment、不推断、不新增 mismatch branch。
5. duplicate eventId 保持一次 save/dispatch，后续 `DUPLICATE/RECEIVED`。
6. handler 无 `ExperienceFeedbackService` 依赖或调用。
7. handler 无 `ExperienceStore` 依赖或调用。
8. handler 无 `PheromoneStore` 依赖或调用。
9. handler 无 `FailureCaseStore` 依赖或调用。
10. `NqFeedbackIngestionService` 与 append-only `NqIntegrationUseCase` 对三个 stores 零写入。
11. repository/audit-like write failure 继续抛出/失败关闭，不返回成功。
12. unknown runtime exception 不重新启用 mutation，也不得返回 false success。

### 12.2 Spring wiring

1. inbound handler beans 在没有 `ExperienceFeedbackService` 时可解析。
2. `NqFeedbackController` 到 ingestion/handler 的 bean dependency graph 不含三个 stores。
3. context 中无 listener 重新接入 mutation。
4. context 中无 scheduler/transaction/async callback 重新接入 mutation。
5. production wiring context 启动成功。
6. `DhApiAuthenticationFilter`、`IdempotencyFilter`、HMAC、header validator 与 rate limiter 仍在保护链。

### 12.3 WebMvc / endpoint

1. authenticated request path/method/schema/status 不变。
2. HMAC/timestamp/nonce/source/canonical binding 行为不变。
3. rate limit 行为不变且限流请求不进入 ingestion。
4. accepted/error response schema 不变。
5. replay 与 duplicate request 行为不变。
6. successful request 对三个 mutable stores 零 interaction。
7. rejected request 对三个 mutable stores 零 interaction。

### 12.4 Architecture

```text
..usecase.agent.feedback..
must not depend on:
  ExperienceFeedbackService
  DefaultExperienceFeedbackService
  ..memory.agent..

NqFeedbackController + FeedbackIngestionWiringConfig + feedback handlers
must not depend on:
  ExperienceFeedbackService
  ExperienceStore
  PheromoneStore
  FailureCaseStore
```

若 ArchUnit 无法表达 Spring bean graph，使用 source/package rule + context bean-dependency assertions 双重证明。
guard 还必须扫描 listener、scheduler、transaction callback 与 async callback token。

### 12.5 Targeted and full commands

```powershell
mvn -B -ntp -pl dh-usecase -am -Dtest=NqFeedbackHandlerDispatchTest,NqFeedbackIdempotencyTest,NqFeedbackContractValidationTest,ResearchRunStage1ClosedLoopTest test
mvn -B -ntp -pl dh-api -am -Dtest=NqFeedbackControllerWebMvcTest,NqFeedbackRateLimitWebMvcTest test
mvn -B -ntp -pl dh-app -am -Dtest=FeedbackSideEffectContainmentWiringTest,ArchitectureTest test
mvn -B -ntp test
mvn -B -ntp -Pquality validate
```

Full acceptance：19/19 reactor success，failures/errors/skipped 均为 0；mandatory PostgreSQL/Testcontainers
真实执行；Checkstyle 0；Spotless PASS。不得复用历史 CI 冒充 implementation execution。

## 13. Review and stop triggers

立即停止 consolidated implementation：

- 发现新的 inbound production caller 或 hidden listener/scheduler/callback mutation。
- 任何 `API / Controller / request/response / migration / Repository / JDBC / contract / POM / workflow` 变更需求。
- 无法在 production/test allowlist 内使 full regression 通过。
- tenant/environment/idempotency 需要新语义而非 characterization preservation。
- 需要异步处理、event bus、feature flag、provider、NQ runtime、Agent 或 LangGraph。
- 出现 P0/P1、安全数据泄漏或真实外部副作用证据。

对应结果：

```text
API/MIGRATION/REPOSITORY OR OTHER SCOPE EXPANSION:
  DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-SCOPE-BLOCKER

INCOMPLETE CALL CHAIN:
  DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-COMPLETE-DISCOVERY
```

## 14. Commit, publication, rollback and close discipline

- B1–B3 只创建一个本地 implementation commit；禁止 push、tag、merge、rebase、amend、force push。
- implementation 建议 commit：`fix(hardening): contain feedback learning side effects`。
- 本工单提交只包含文档，建议 commit：
  `docs(hardening): define feedback side-effect containment work order`。
- rollback：implementation 未发布时按文件恢复；已普通提交后使用 ordinary `git revert <implementation-sha>`。
  无 schema/API 变更，因此无数据库或 wire rollback。
- B4 必须按 `security review -> accepted implementation -> authorized push -> exact-SHA CI -> archive packet ->
  archive close -> annotated tag -> remote verification -> post-tag cleanup` 执行。

## 15. Boundary confirmation

```text
Stage-QDR-9: CLOSED / ACCEPTED / ARCHIVED / TAGGED / UNCHANGED
Java production/test changes this task: NONE
API / migration / Repository / contracts / POM / workflow: UNCHANGED
NQ / Provider / real HTTP: NOT TOUCHED
Feedback learning / case promotion / memory evolution: NOT IMPLEMENTED
Agent / LangGraph / Paper / LIVE: NOT STARTED / NOT AUTHORIZED
Capacity gate: NOT EXECUTED
Push / tag: NOT EXECUTED
Production capacity: NOT_PROVEN
```

## 16. Next concrete action

```text
DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-CONSOLIDATED-IMPLEMENTATION
```

该任务才允许按本工单 B1–B3 和精确 allowlist 实施；本工单本身不授权立即实现。

## 17. This-task validation record

```text
Git baseline: PASS / ALLOWED STATE A
Scope invariants: PASS / 3 OF 3
Callsite inventory: PASS / 2 PRODUCTION APPLY CALLS / 3 STORE WRITE METHODS
Hidden listener/scheduler/callback scan: 0 FOUND
Only allowlisted docs changed: PASS
Forbidden technical diff: 0
Migration / API / Repository / contracts / POM / workflow diff: 0
Archive diff: 0
mvn -B -ntp -Pquality validate: PASS / EXIT 0 / 19 OF 19
Checkstyle: 0
Spotless: PASS
Full tests: NOT_RERUN / DOCS-ONLY TASK
Technical implementation: NOT EXECUTED
Push / tag: NOT EXECUTED
```
