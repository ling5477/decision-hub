# DH Platform Hardening Feedback Ingest Atomicity Implementation Work Order

## Terminal current authority — 2026-08-02 feedback ingest atomicity local implementation

~~~text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-CONSOLIDATED-IMPLEMENTATION
Task result: IMPLEMENTED / LOCAL_ACCEPTED / NOT PUSHED
Docs baseline: 16ecded2f3708d69e05afe5f2a4f621c823d8be3 / PUBLISHED
Docs baseline exact-SHA CI: 30710328666 / PASS / QUALITY 91396539930 + TESTCONTAINERS 91396539969
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
Feedback ingest atomicity: IMPLEMENTED / LOCAL_ACCEPTED
Atomic boundary: UNIT_OF_WORK / SAME_DATASOURCE / PROPAGATION_REQUIRED
JDBC: TRANSACTION_TEMPLATE / FAIL_FAST_DATASOURCE_IDENTITY / ON_CONFLICT_DO_NOTHING + READBACK
In-memory: ATOMIC_SNAPSHOT_RESTORE / JDBC_SEMANTIC_PARITY
Rollback: ENVELOPE_AND_EVENT_ALL_OR_NOTHING
Safe retry / response-loss retry: SUPPORTED / PASS
Commit unknown: INTERNAL_CLASSIFICATION / NO_AUTOMATIC_RETRY / NO_API_CHANGE
Concurrent same key: ONE COMPLETE WINNER / ENVELOPE 1 + EVENT 1
Scope invariants: PASS / 3 OF 3
Production / test / factsource allowlists: PASS / 7 / 9 / 9 EXACT PATHS / 0 UNEXPECTED
API / migration / schema / contracts / POM / workflow / NQ: UNCHANGED / NONE
Learning containment: UNCHANGED / PASS
Inbound ExperienceFeedbackService.apply callers: 0
Inbound ExperienceStore / PheromoneStore / FailureCaseStore writes: 0 / 0 / 0
Targeted atomicity matrix: PASS / 56 UNIT+API+JDBC + 55 APP/WIRING/ARCHITECTURE/POSTGRESQL
PostgreSQL / Flyway: 17.10 / V1-V15 / REAL TESTCONTAINERS / 0 SKIPPED
Module regression: PASS / 15 OF 15 REACTOR
Full regression: PASS / 19 OF 19 REACTOR / 1274 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
CodeRabbit: NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY
Implementation commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY / NOT PUSHED
Remote implementation CI: PENDING
Milestone final close: NOT_STARTED
Production capacity / production ready: NOT_PROVEN / NO
Current factsources: 9 / 9 / SYNCHRONIZED / 0 CONFLICTS
Next action: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-MILESTONE-FINAL-CLOSE
ALLOW_MILESTONE_FINAL_CLOSE: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_PUSH_NOW / ALLOW_TAG_NOW: NO / NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT / ALLOW_LANGGRAPH: NO / NO / NO / NO
ALLOW_PAPER / ALLOW_LIVE: NO / NO
~~~

本节是 consolidated implementation 本地接受后的唯一 terminal authority。implementation commit 只能保留
在本地；push、exact-diff security review、remote exact-SHA CI、archive、tag 与 post-tag cleanup 只能由独立
milestone final close 任务执行。以下 work-order、planning 与已关闭 milestone 区块均为历史时间线，不得覆盖本节。


## 1. Authority and task classification

```text
Task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-IMPLEMENTATION-WORK-ORDER
Primary classification: SECURITY_AUDIT
Delivery: WORK_ORDER_ONLY / DOCS_ONLY_CHANGE
Selected stage: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY
Selected workstream: FEEDBACK_ENVELOPE_EVENT_ATOMIC_PERSISTENCE
Work order result: DONE / SCOPE FROZEN / IMPLEMENTATION NOT STARTED
Selected atomicity design: UNIT_OF_WORK
Atomic boundary / rollback / safe retry / commit unknown: FROZEN / FROZEN / FROZEN / FROZEN
JDBC / in-memory parity: FROZEN
API / migration / schema impact: NONE / NONE / NONE
Production capacity: NOT_PROVEN
Next task: DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-CONSOLIDATED-IMPLEMENTATION
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
ALLOW_PUSH / ALLOW_TAG: NO / NO
```

本工单承接 `DH_POST_FEEDBACK_SIDE_EFFECT_CONTAINMENT_NEXT_STAGE_PLAN.md`，只冻结 B1–B3 consolidated
implementation 的精确设计、文件边界与验收命令。本轮不修改 Java、测试、API、migration、schema、Repository、
POM、workflow 或 NQ，不实施原子性修复。

## 2. Verified baseline

```text
Repository: E:/Project/decision-hub
Branch: dev
HEAD / planning commit: f087f562bdf9802c19ebeebcb8c1523cd390130f
HEAD parent / origin/dev: 071bc29ee3c03b4099623c4ade441783cc22091f
Semantic ahead / behind: 1 / 0
git left/right output: 0 / 1
Worktree / staged before write: clean / empty
Closed feedback-containment milestone: CLOSED / ACCEPTED / ARCHIVED / TAGGED / NOT REOPENED
Formal capacity: NOT_EXECUTED / DEFERRED
Production capacity: NOT_PROVEN
```

附件中的基线事实与 Git 结果一致。唯一需澄清的运行时事实是：`Stage2JdbcWiringConfig` 仅在
`decisionhub.stage2.jdbc.enabled=true` 时启用；当前仓库 profile 未设置该开关，因此默认 Spring graph 使用
in-memory repository。JDBC adapter 的两次独立 `JdbcTemplate.update` 是真实、可启用的生产实现现实，工单必须
同时修复 JDBC 路径并保证默认 in-memory 路径等价；该澄清不要求配置或 API 变更。

## 3. Scope design

### READ_SCOPE

- Root/current authority 文档、当前 planning document 与本工单。
- Controller、ingestion service、Repository port、JDBC/in-memory adapters、router、8 handlers。
- Feedback/Spring JDBC wiring、datasource/transaction wiring、V2/V3 schema/constraints、异常映射与相关 tests。
- 现有 structured feedback transaction boundary 仅作为项目内模式证据，不接入 legacy ingress。

### WRITE_ALLOWLIST_THIS_TASK

- 本工单与 `DH_POST_FEEDBACK_SIDE_EFFECT_CONTAINMENT_NEXT_STAGE_PLAN.md`。
- root/current 的 `README`、`STATUS`、`WORK_ORDER`、`ROADMAP`、`TESTING`、`WORKLOG`、
  `CODEX_PROJECT_INSTRUCTIONS`。

### VALIDATION_SCOPE / FIXABLE_BLOCKER_SCOPE / CURRENT_FACTSOURCE_SCAN_SCOPE

- Validation 覆盖全部 write allowlist、禁止技术 diff、archive/tag immutability、authority consistency 与
  `mvn -B -ntp -Pquality validate`。
- 两个可写 blocker/scan scope 均只能落在本轮 docs allowlist；任何 Java/test/migration/API/schema/POM/workflow
  修改需求均停止本轮。

```text
VALIDATION_SCOPE subset READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE subset WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE subset WRITE_ALLOWLIST: PASS
TECHNICAL_PATHS_IN THIS-TASK WRITE_ALLOWLIST: 0
```

## 4. Code reality inventory

### 4.1 Ingestion call chain

```text
NqFeedbackController.receive
  -> DefaultNqFeedbackIngestionService.ingest
  -> NqFeedbackEventRepository.findEnvelopeByEventId
  -> NqFeedbackContractValidator.validate
  -> NqFeedbackEventRepository.saveEnvelope
  -> DefaultNqFeedbackEventTypeRouter.route
  -> one of 8 NqFeedbackEventHandler.handle
  -> AbstractNqFeedbackEventHandler.handle
  -> NqFeedbackEventRepository.append
```

- Controller 只有一个 `NqFeedbackIngestionService` 构造依赖；auth、header binding、rate limit、HMAC、nonce/source/
  timestamp validation 均在 `ingest` 前完成。
- `DefaultNqFeedbackIngestionService` 是 Spring `@Bean` 管理的 final instance，但当前不是 transactional proxy；
  它直接执行 duplicate lookup、save、同步 route/append。
- Router 直接调用 handler；8 handlers 均继承 `AbstractNqFeedbackEventHandler`，没有 executor、listener、scheduler、
  `@Async` 或线程切换。
- 范围内不存在 `@Transactional`、`REQUIRES_NEW`、独立 connection、手工 commit 或自动 retry。

### 4.2 Repository and SQL

- 独立 envelope repository 不存在；envelope 和 legacy event 共用 `NqFeedbackEventRepository` 与
  `dh_nq_feedback_events`，但持久化为两行。
- Envelope SQL 写 `id=eventId`、`event_id=eventId` 及 schema/validation/source/request/correlation 字段。
- Event SQL 写新生成的 `id`、tenant/run/candidate/trace/source/type/payload，`event_id` 保持 `NULL`。
- V2 定义 `id` primary key；V3 定义 `event_id IS NOT NULL` 的 partial unique index
  `ux_dh_nq_feedback_events_event_id`。该约束裁决 envelope 幂等，不证明 event row 已 append。
- JDBC 两个方法使用同一个 repository `JdbcTemplate`，可加入调用方同线程 `PROPAGATION_REQUIRED`
  transaction；当前没有该调用方 transaction。
- `Stage2JdbcWiringConfig` 从同一 Spring context 注入 `JdbcTemplate`；仓库中无自定义第二 DataSource 或第二
  `PlatformTransactionManager` bean。implementation 必须增加 identity/fail-fast guard，不能只依赖推断。

### 4.3 Duplicate, failure and response mapping

- 第一次 `findEnvelopeByEventId` 是优化性 duplicate lookup；并发裁决目前依赖 V3 unique index。
- `saveEnvelope` 当前捕获 `DuplicateKeyException` 返回 `false`。进入显式 PostgreSQL transaction 后不能继续吞
  statement error，因为 transaction 会被中止。
- append/router/unknown exception 向上传播，`GlobalExceptionHandler.handleUnknown` 保持 generic HTTP 500 /
  internal error；不新增 API 字段或错误码。
- ACCEPTED/DUPLICATE 保持 HTTP 202，REJECTED 保持 HTTP 400；HMAC、tenant、rate-limit 与 DTO 不变。
- 当前 partial path：envelope insert 成功 -> router/handler/append 失败 -> envelope 残留；same key retry 在
  duplicate lookup 短路，返回 DUPLICATE 且不补 event。

### 4.4 In-memory and test reality

- In-memory 分别使用 `envelopesByEventId` 与 `indexByRun`。单方法虽 `synchronized`，跨 save+append 没有
  snapshot/rollback；handler failure 会留下 envelope-only。
- `LinkedHashMap` 提供确定性插入顺序；rollback snapshot 必须覆盖 action 中的 TTL cleanup/eviction。
- 已覆盖首次/duplicate、validation reject、save/handler/append failure 传播、8 handler、JDBC envelope SQL 与
  learning containment；缺 append failure rollback、safe retry、response loss、concurrent key、commit unknown、
  snapshot restore、真实 PostgreSQL 两行原子性与 wiring resource identity。

```text
TRANSACTION_PARTICIPANT_INVENTORY: PASS
DIFFERENT_DATABASE_OR_TRANSACTION_MANAGER: NO EVIDENCE
ASYNCHRONOUS_HANDLER_BOUNDARY: NO
COMPLETE_DISCOVERY_BLOCKER: NONE
```

## 5. Selected design — UNIT_OF_WORK

### 5.1 Usecase-owned contract and boundary

新增只依赖 JDK `Supplier<T>` 的 `NqFeedbackIngestionUnitOfWork`：

```java
public interface NqFeedbackIngestionUnitOfWork {
  <T> T required(Supplier<T> action);
}
```

`DefaultNqFeedbackIngestionService` 新增依赖，并以 `unitOfWork.required(() -> ingestAtomic(command))` 包住现有
duplicate-first 编排。边界从第一次 `findEnvelopeByEventId` 开始，在 routed event append 成功后结束。

`DefaultNqFeedbackContractValidator` 包含 `ResearchRunRepository.find`，不是纯函数；为保留 duplicate-first 语义且
避免 validator 重构，它与 duplicate lookup 一起位于 unit-of-work 内。HTTP JSON/Jakarta validation、auth、HMAC/
header/nonce/source/timestamp、rate limit 与 response serialization 在边界外。不得将外部 HTTP、Provider 或
learning mutation 放入 transaction。

### 5.2 JDBC implementation and commit classification

- `JdbcNqFeedbackEventRepository` 同时实现 Repository 与 unit-of-work；usecase 不依赖 Spring transaction API。
- 构造注入现有 `JdbcTemplate`、`ObjectMapper`、`PlatformTransactionManager`，`TransactionTemplate` 精确使用
  `PROPAGATION_REQUIRED`，不配置 `REQUIRES_NEW`，不自动 retry。
- 构造时要求 manager 是 `DataSourceTransactionManager` 兼容类型，并要求其 DataSource 与
  `JdbcTemplate.getDataSource()` 为同一实例；缺失、多个候选或不匹配均 fail-fast。
- action unchecked exception 向外传播并 rollback；transaction creation/rollback failure 映射内部
  `PERSISTENCE_FAILURE`；仅 action 正常返回后 commit phase 抛 `TransactionSystemException` 时映射
  `COMMIT_OUTCOME_UNKNOWN`。用内部 callback wrapper 区分 action 自身同类型异常，保留 cause。
- 不在同一请求自动 retry，不修改 Controller/API error contract。

### 5.3 Conflict-safe envelope insert

`INSERT_ENVELOPE_SQL` 冻结为 `INSERT ... ON CONFLICT DO NOTHING`：

1. update count `1`：首次插入，继续 route/append。
2. count `0`：同 transaction 回读 `event_id`；存在 envelope 才返回 `false`/DUPLICATE。
3. count `0` 且回读为空：fail-closed persistence failure，禁止误报 duplicate。
4. 禁止在 transaction 内捕获 `DuplicateKeyException` 后继续，禁止换 eventId/requestId。

V3 partial unique index 与 primary key 足够，无需 migration/schema change。预查询只作快速路径，数据库
`ON CONFLICT` 结果为并发裁决权威。

### 5.4 In-memory atomicity

- `InMemoryNqFeedbackEventRepository` 同时实现 unit-of-work。
- `required` 为 `synchronized`，action 前 snapshot 两个 map；`indexByRun` 必须复制每个 list，不能浅复制。
- 成功保留状态；任一 `RuntimeException`/`Error` 时恢复两个结构及 action 触发的 cleanup/eviction 后原样重抛。
- 不依赖 `HashMap` 非确定顺序，不 sleep，不提供 no-op transaction。
- monitor 可重入；unit-of-work 与 repository 使用同一 instance，并发 same key 串行化为一对记录。

### 5.5 Wiring

- Default repository bean return type 改为 concrete `InMemoryNqFeedbackEventRepository`，显式
  `@ConditionalOnMissingBean(NqFeedbackEventRepository.class)`；同一 bean 同时提供两个 port。
- JDBC bean return type 改为 concrete `JdbcNqFeedbackEventRepository` 并注入唯一 manager。
- Ingestion service 同时注入 repository 与 unit-of-work；测试断言是同一 bean、两个 profile 都只有一个 ingestion
  root、Controller 无 raw delegate。
- 缺 unit-of-work、manager 多候选或 resource mismatch 时启动失败，禁止 non-atomic 降级。

## 6. Frozen rollback, retry and concurrency semantics

### Pre-commit failure

```text
Envelope rows: 0
Event rows: 0
Response: no success / existing generic 500 for internal failure
Retry: same eventId/requestId executes the complete unit again
```

### Complete commit / duplicate / response loss

```text
First complete result: ACCEPTED / rows 1 + 1
Same-key retry: DUPLICATE / HTTP 202 / rows remain 1 + 1
Additional event append: 0
Response loss simulation: commit first, discard result, retry same key -> DUPLICATE / 1 + 1
```

### Concurrent same key

- latch/barrier 同步起跑，不使用 sleep。
- JDBC 由 `ON CONFLICT DO NOTHING` + commit 裁决；in-memory 由同一 monitor 串行化。
- 最多一个 ACCEPTED，其余进入现有 DUPLICATE 或 fail-closed；最终 `1 + 1`，不得 partial。

### Commit unknown

```text
Classification: INTERNAL COMMIT_OUTCOME_UNKNOWN
API field/status addition: NONE
Automatic retry in same request: FORBIDDEN
Assume rollback / assume commit: FORBIDDEN / FORBIDDEN
Original cause: PRESERVED
Caller convergence: retry same eventId/requestId
Committed outcome -> DUPLICATE / 1 + 1
Rolled-back outcome -> complete re-execution / 1 + 1
```

## 7. Consolidated implementation allowlists

### PRODUCTION_WRITE_ALLOWLIST

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIngestionUnitOfWork.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIngestionTransactionException.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/impl/DefaultNqFeedbackIngestionService.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/inmemory/InMemoryNqFeedbackEventRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepository.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/FeedbackIngestionWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/Stage2JdbcWiringConfig.java
```

内部 exception code 只允许 `PERSISTENCE_FAILURE` 与 `COMMIT_OUTCOME_UNKNOWN`；不得增加 API enum/DTO。

### TEST_WRITE_ALLOWLIST

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIdempotencyTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIngestionAtomicityTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/inmemory/BoundedInMemoryNqFeedbackEventRepositoryTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackControllerWebMvcTest.java
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepositoryTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/FeedbackIngestAtomicityFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/FeedbackIngestAtomicityWiringTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/config/FeedbackSideEffectContainmentWiringTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

### FACTSOURCE_WRITE_ALLOWLIST

```text
docs/current/DH_PLATFORM_HARDENING_FEEDBACK_INGEST_ATOMICITY_IMPLEMENTATION_WORK_ORDER.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
README.md
```

Planning document 在 implementation task 中只读；B4 archive task 才复制 source documents。

### FORBIDDEN_PATHS

```text
AGENTS.md
CLAUDE.md
docs/gates/**
dh-api/src/main/**
dh-domain/**
dh-security/**
dh-memory/**
dh-connector/**
dh-eval/**
dh-scheduler/**
dh-providers/**
contracts/**
golden_cases/**
dh-app/src/main/resources/db/migration/**
pom.xml
dh-*/pom.xml
.github/**
NQ repository/worktree/**
```

还禁止其他 business Repository、V15 attribution、learning service/stores、API/Controller/DTO/OpenAPI、config
properties、capacity、HTTP/Provider、Agent/LangGraph。任何 allowlist 外必要变更均停止：

```text
FEEDBACK_INGEST_ATOMICITY_SCOPE_EXPANSION_REQUIRED
ALLOW_CONSOLIDATED_IMPLEMENTATION: NO
```

## 8. Consolidated batches

### B1 — Repository / transaction / security review

- 复核 7 production、9 test、9 factsource exact paths；再次证明单 JdbcTemplate/manager/DataSource 与同步 handlers。
- 冻结 `UNIT_OF_WORK`、`PROPAGATION_REQUIRED`、conflict SQL 与 failure taxonomy。
- 若需 API/migration/schema/第二数据库/第二 manager，停止；B1 不做独立 review commit。

### B2 — Atomic persistence implementation

- 完成 usecase contract、JDBC transaction、conflict-safe insert、in-memory snapshot rollback 与 wiring。
- 保持 ingress、8 types、payload projection、response、tenant/HMAC/rate-limit/idempotency/no-learning 不变。
- 不重构其他 Repository，不引入 outbox/scheduler/MQ/compensation/feature flag。

### B3 — Failure, retry and concurrency regression

- 补齐 unit/in-memory/JDBC/PostgreSQL/wiring/architecture 矩阵。
- 执行 targeted/full regression、quality 与 boundary diff。
- 全部通过后只创建一个本地 implementation commit；不 push、不 tag。

### B4 — Milestone final close

独立任务执行 exact-diff security review、push、exact-SHA CI、authority sync、self-contained archive、archive close
commit、annotated tag、remote verification 与 post-tag cleanup。候选 tag：
`dh-platform-hardening-feedback-ingest-atomicity-close`；严格 archive-before-tag。

## 9. Frozen test matrix

### Unit / in-memory

1. envelope + event success；save failure 时 event 不执行。
2. router/unsupported/handler/append/unknown failure 时两个状态均 rollback。
3. committed duplicate 不追加 event；rollback 后 same key 完整成功。
4. snapshot 恢复 envelope、event、cleanup/eviction；并发 same key 仅一对；不 sleep。
5. learning store interaction 保持 0。

### JDBC unit

1. `PROPAGATION_REQUIRED` 与同 DataSource guard。
2. conflict SQL count 1/0；count 0 回读存在 -> duplicate，回读空 -> failure。
3. callback failure 原样传播并 rollback。
4. transaction create/rollback failure -> `PERSISTENCE_FAILURE`。
5. commit phase unknown -> `COMMIT_OUTCOME_UNKNOWN`，cause 保留且无 retry。

### PostgreSQL 17 / Testcontainers

1. real PostgreSQL 17.x，clean Flyway V1–V15。
2. envelope insert 后 append 前 fault、append SQL fault、router/handler exception 均 counts `0 + 0`。
3. rollback 后 retry -> `1 + 1`；concurrent same key -> one complete winner / `1 + 1`。
4. successful commit 后丢弃 response，再 retry -> duplicate / `1 + 1`。
5. 分别断言 envelope rows 与 matching legacy event rows；不得 envelope-only/event-only。

### Wiring / architecture / compatibility

1. Controller 只有一个 protected root；repository/UoW 同一 bean；JDBC 使用同一 manager/DataSource。
2. 禁止 `REQUIRES_NEW`、async/listener/scheduler append、内部 retry/sleep。
3. 8 handlers、HMAC/header/tenant/rate-limit/idempotency、HTTP 202/400/500 不变。
4. no-learning guard 继续通过；V15/API/migration/contracts/NQ/Provider/Agent/LangGraph diff 为 0。

### Implementation validation commands

```powershell
mvn -B -ntp -pl dh-usecase -am -Dtest=NqFeedbackIdempotencyTest,NqFeedbackIngestionAtomicityTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -ntp -pl dh-infra -am -Dtest=JdbcNqFeedbackEventRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -ntp -pl dh-api -am -Dtest=NqFeedbackControllerWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -ntp -pl dh-app -am -Dtest=FeedbackIngestAtomicityFlywayPostgresTest,FeedbackIngestAtomicityWiringTest,FeedbackSideEffectContainmentWiringTest,ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -ntp test
mvn -B -ntp -Pquality validate
```

Mandatory Testcontainers 不得因 Docker 缺失被宣称 PASS；并发测试只用 latch/barrier/future timeout，不用 sleep。

## 10. Review, rollback and publication

- Review triggers：migration/schema/API/Controller/wire contract、第二 DataSource/manager、无法证明 complete duplicate
  或 commit phase、P0/P1、learning/NQ/Provider/Agent/LangGraph dependency、B4 close。
- Rollback：B1–B3 一个普通 commit；失败用 ordinary `git revert`。无 down migration/backfill/wire rollback；禁止
  reset/rebase/force push/手工 delete compensation。
- Consolidated implementation 只 local commit；B4 exact-diff security PASS 后才 push，并等待 exact-SHA CI。
- Archive packet 必须在 tag 前完成；tag target 包含 archive close commit，tag 不得移动或重建。

## 11. Files inspected

- Root/current authority、planning document。
- Controller/service/result/validator/router/handler/Repository port、JDBC/in-memory adapters。
- Feedback/Stage2 JDBC wiring、application profiles、POM transaction dependencies、V2/V3、Global exception handler。
- 8 concrete handlers、相关 usecase/infra/api/app/wiring/architecture/PostgreSQL tests inventory。
- V15 transaction boundary 仅作 pattern 证据。

## 12. This-task validation record

```text
Git baseline: PASS
Only allowlisted docs changed: PASS / 10 OF 10 / 0 UNEXPECTED
Forbidden technical diff: PASS / 0
Archive/tag diff: PASS / 0
Scope invariants: PASS / 3 OF 3
Transaction participant inventory: PASS
Authority consistency: PASS / 8 OF 8 WORK-ORDER ENTRY SOURCES
Quality validate: PASS / EXIT 0 / 19 OF 19 REACTOR SUCCESS
Checkstyle / Spotless: 0 VIOLATIONS / PASS
Full tests: NOT_RERUN / WORK_ORDER ONLY
Staged before commit: EMPTY
Local commit: THIS_DOCUMENT_COMMIT / LOCAL ONLY
Push / tag: NOT EXECUTED / NOT EXECUTED
```

## 13. Readiness decision

```text
FEEDBACK_INGEST_ATOMICITY_IMPLEMENTATION_WORK_ORDER: DONE
TRANSACTION_PARTICIPANT_INVENTORY: PASS
SELECTED_ATOMICITY_DESIGN: UNIT_OF_WORK
ATOMIC_BOUNDARY: FROZEN
ROLLBACK_SEMANTICS: FROZEN
SAFE_RETRY_SEMANTICS: FROZEN
COMMIT_UNKNOWN_SEMANTICS: FROZEN
JDBC_IN_MEMORY_PARITY: FROZEN
PRODUCTION_WRITE_ALLOWLIST: FROZEN
TEST_MATRIX: FROZEN
API_CHANGE_REQUIRED / MIGRATION_REQUIRED / SCHEMA_CHANGE_REQUIRED: NO / NO / NO
ALLOW_CONSOLIDATED_IMPLEMENTATION: YES / NEXT TASK ONLY
ALLOW_IMPLEMENTATION_NOW: NO
ALLOW_FEEDBACK_LEARNING / ALLOW_CAPACITY_EXECUTION / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_AGENT_PHASE: NO / NO / NO
ALLOW_LANGGRAPH_RUNTIME / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO
PRODUCTION_CAPACITY: NOT_PROVEN
```

## 14. Risks and next action

- Manager mismatch：implementation 用 DataSource identity guard 把当前推断变为 fail-fast contract。
- Duplicate race：事务内吞 unique exception 会污染 transaction；必须采用冻结的 conflict SQL + 回读。
- Commit unknown：只按 phase 分类，禁止内部 retry/伪造成功。
- In-memory：snapshot 必须恢复 cleanup/eviction，浅复制会残留副作用。
- Default wiring：profiles 默认 in-memory；JDBC acceptance 必须显式启用并跑真实 PostgreSQL。
- Exception/authority drift：generic 500、closed milestone、archive/tag 与 no-learning boundary 不变。

Next concrete action：
`DH-PLATFORM-HARDENING-FEEDBACK-INGEST-ATOMICITY-CONSOLIDATED-IMPLEMENTATION`。
该任务只按本工单 B1–B3 exact allowlists 完成 implementation + tests + boundary scan + minimal docs + one local
commit，不 push/tag。建议 commit message：

```text
fix(feedback): make ingest persistence atomic
```
