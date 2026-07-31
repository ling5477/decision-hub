# DH Stage-QDR-9 Structured Feedback Attribution Persistence and Historical Evidence Read Model Plan

## Terminal current authority — 2026-07-23 Stage-QDR-9 planning local accepted

```text
Repository baseline: 2aa5183a81d1733eec38ec2ab85de8d0c90c13a4
Branch: dev
Remote exact-SHA CI: 30008506440 / PASS
Remote regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
Remote quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-9 plan: DONE / LOCAL_ACCEPTED
Stage-QDR-9 implementation: NOT_STARTED
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE + HISTORICAL_EVIDENCE_READ_MODEL
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Terminal current factsources: 12
Scope invariants: PASS / 6 OF 6
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 BLOCK HASH / 0 CONFLICTS
current task: DH-STAGE-QDR-9-PLAN
current task status: DONE / LOCAL_ACCEPTED
next action: DH-STAGE-QDR-9-IMPLEMENTATION-WORK-ORDER
ALLOW_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_9_IMPLEMENTATION_NOW: NO
ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW / ALLOW_API_CHANGE_NOW: NO / NO / NO
ALLOW_AUTOMATIC_LEARNING: NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本计划只冻结 Stage-QDR-9 的持久化、内部历史证据查询、安全、测试和阶段门禁。它不创建 migration、Repository、API、表或 runtime wiring，不启动 implementation，也不改变 Stage-QDR-7/8、B2 capacity 或 production capacity 结论。

## 1. 任务分类、目标与边界

```text
Task classification:
PLANNING_ONLY
+ STRUCTURED_FEEDBACK_PERSISTENCE_PLAN
+ HISTORICAL_EVIDENCE_READ_MODEL_PLAN
+ FEEDBACK_QUERY_BOUNDARY_DESIGN
+ SECURITY_BOUNDARY_DESIGN
+ TEST_MATRIX_DESIGN
+ STAGE_GATE_DESIGN
+ NO_CODE_CHANGE
+ NO_TEST_CHANGE
+ NO_MIGRATION_CHANGE
+ NO_API_CHANGE
+ NO_REAL_HTTP
+ NO_REAL_PROVIDER
+ NO_NQ_RUNTIME
+ NO_AGENT
+ NO_LANGGRAPH
+ NO_PAPER
+ NO_LIVE
```

### 1.1 目标

1. 审计 Stage-QDR-8 structured feedback attribution foundation 的真实实现边界。
2. 冻结 observation、attribution、contribution 与 audit/replay/evaluation safe reference 的持久化聚合。
3. 冻结 tenant/environment-bound 的内部 historical evidence read model。
4. 冻结事务、幂等、commit-unknown、retention、cleanup 与完整性边界。
5. 将实施拆成最多四个 batch 加 final close，并定义独立 review 触发条件。
6. 冻结完整测试矩阵、回滚策略、archive/tag/source-document 治理。

### 1.2 不做

```text
不新增或修改 Java 生产代码、测试、migration、Repository、API、Controller、POM 或 workflow
不复用旧 raw feedback event 表作为 QDR-9 attribution persistence
不新增外部 HTTP、真实 Provider、NQ runtime、Agent、LangGraph、Paper 或 LIVE
不自动更新 Experience、Pheromone、Prompt、模型、策略、候选或 JudgeDecision
不形成 automatic learning、online learning 或 decision rerun
不重开 Stage-QDR-7 B2 capacity gate
不声明 production capacity
不修改 Stage-QDR-7/8 archive 或 tag
不 commit、不 push、不创建 tag
```

### 1.3 验收

```text
代码现实问题 10 OF 10 已回答
九类 scope 已冻结
scope subset invariants 6 OF 6 PASS
B1-B5、review trigger、测试矩阵、回滚和 archive/tag discipline 已冻结
12 个 terminal current factsources 使用同一 authority block
current conflict count = 0
仅批准的 13 个 planning docs 有 diff
Maven quality = 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
```

## 2. 当前事实与代码现实

### 2.1 规划基线

```text
repository: E:/Project/decision-hub
branch: dev
HEAD / origin/dev / advertised SHA: 2aa5183a81d1733eec38ec2ab85de8d0c90c13a4
ahead / behind: 0 / 0
worktree / staged at preflight: clean / empty
exact-SHA CI: 30008506440 / PASS
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8 persistence/API/runtime expansion: NONE
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
```

### 2.2 Feedback foundation

已实现的 `com.guidinglight.decisionhub.domain.qdr.feedback` 与 `usecase.qdr.feedback` 提供：

- tenant/environment/decision/trace-bound `FeedbackSubject` 与 `OutcomeObservation`；
- 封闭 `OutcomeSource`、`ObservedDecisionOutcome`、`AttributionDimension`、`FeedbackStatus`；
- 有界 measurement/contribution/confidence、安全 reference 和 immutable collection；
- deterministic canonicalization、SHA-256 canonical hash、结果 identity 与 idempotency key；
- `FeedbackAttributionIdempotencyPort`、`FeedbackAttributionAuditPort`；
- audit failure、idempotency unavailable/conflict 与 scope mismatch 的 fail-closed 用例行为。

未实现：

- attribution persistence port 或 production adapter；
- historical evidence query/read-model port；
- observation、attribution、contribution 或 safe reference 表；
- attribution migration、JDBC Repository、Spring bean/runtime wiring；
- audit/replay target existence validation；
- retention/cleanup implementation。

### 2.3 十个问题的冻结回答

| 问题 | 结论 |
|---|---|
| 当前幂等状态是否仅在进程内 | production 没有幂等状态实现；测试 `BoundedIdempotencyPort` 使用有界 `ConcurrentHashMap`，随进程丢失。 |
| 当前是否存在无界内存状态 | attribution production 无内存状态；测试 fake 有显式 capacity，不是无界。 |
| 当前是否已有 feedback Repository port | 没有 QDR attribution persistence/query Repository port。 |
| 当前是否已有可复用 transaction pattern | 有。`ReplayInputSnapshotAssemblyService` 与 wiring 使用显式 transaction manager、`REPEATABLE_READ`、rollback、无 fallback；JDBC idempotency guard 提供唯一约束/CAS/commit-unknown fail-closed 模式。 |
| 当前 audit/replay 引用如何验证 | 仅验证 reference 字段与 tenant/environment/decision/trace/observation/policy/hash 一致；不验证数据库目标实际存在。 |
| 当前是否存在相同或重复 persistence model | `dh_nq_feedback_events`/`JdbcNqFeedbackEventRepository` 是旧 NQ/Paper raw payload 模型，无 environment，部分查询非 tenant-bound；只能记录为 duplicate concept，不可复用。 |
| 是否需要新增 migration | 后续 implementation 需要；本 planning task 不授权。B1 必须先完成 schema/migration 独立 review/freeze。 |
| 是否需要新增 JDBC Repository | 后续 implementation 需要 tenant/environment-bound JDBC adapter；本 planning task 不授权。 |
| 是否能先完成 persistence 而不新增 API | 可以，且必须如此；B2 仅内部 port/adapter/wiring。 |
| 历史查询是否可以先保持 internal use-case/read model | 可以，且 QDR-9 默认必须保持 internal use-case only。 |

### 2.4 可复用模式与不可复用模型

可复用：

- transaction manager 必须存在；不得 silent fallback；
- `REPEATABLE_READ` 或经 B1 评审证明更严格/等价的隔离级别；
- PostgreSQL unique constraint + `ON CONFLICT`/CAS；
- tenant-bound composite key/FK；
- query adapter 返回安全 DTO、稳定排序、数据库异常 fail-closed；
- commit outcome unknown 时不自动重试、不宣称成功，先用幂等 key 做只读 reconciliation。

不可复用：

- `dh_nq_feedback_events` 的 raw `payload_json`、NQ/Paper source 语义；
- 不含 environment 的 identity；
- 不 tenant-bound 的 eventId 查询；
- 测试 `ConcurrentHashMap` 作为 production persistence；
- 现有不含 environment、分页或 feedback filter 的通用 read model。

## 3. 方向排序与阶段主线

| 顺序 | 方向 | 决策 |
|---:|---|---|
| 1 | A — Feedback Attribution Persistence Baseline | SELECTED / PRIMARY |
| 2 | B — Historical Evidence Read Model | SELECTED / PRIMARY |
| 3 | C — Retention / Cleanup / Integrity | SELECTED / AFTER_PERSISTENCE |
| 4 | D — Feedback API / Controller | DEFERRED / SEPARATE_API_REVIEW_REQUIRED |
| 5 | E — Automatic Learning / Memory Mutation | FORBIDDEN |

Stage 主线冻结为：

```text
STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE
+ HISTORICAL_EVIDENCE_READ_MODEL
```

核心链路：

```text
Decision / Trace
  -> Outcome Observation
  -> Deterministic Attribution
  -> Atomic Immutable Persistence
  -> Tenant/Environment-bound Historical Evidence Read Model
  -> Audit / Replay / Evaluation Safe Reference
```

## 4. Persistence aggregate 与 schema 要求

### 4.1 聚合边界

不得建立独立 `FeedbackSubject` 表。subject identity 作为 observation 的 tenant/environment/decision/trace scope；attribution 和所有子记录通过 tenant/environment composite FK 关联。

B1 默认冻结四个逻辑关系，最终物理表名和 migration 版本由 implementation work order 在 schema review 中确定：

| 逻辑关系 | 职责 |
|---|---|
| `feedback_outcome_observation` | immutable structured observation、subject scope、source/outcome/time 与受控 metadata。 |
| `feedback_attribution` | idempotency identity、canonical/result hash、policy、status、confidence、transaction outcome。 |
| `feedback_attribution_contribution` | 每 attribution/维度唯一的 contribution、impact、confidence、reason code、安全 evidence ref。 |
| `feedback_attribution_reference` | audit/replay/evaluation safe references、验证状态与 retention hold。 |

### 4.2 字段、类型与长度

| 字段 | 建议 PostgreSQL 类型 | 约束 |
|---|---|---|
| `tenant_id` | `varchar(128)` | `NOT NULL`；所有 PK/FK/query 必带。与现有 domain `MAX_ID_LENGTH=128` 对齐。 |
| `environment` | `varchar(16)` | `NOT NULL`；closed enum/check，当前仅 `DEV`/`TEST`。 |
| `decision_id` | `varchar(128)` | `NOT NULL`。 |
| `trace_id` | `varchar(128)` | `NOT NULL`。 |
| `observation_id` | `varchar(128)` | `NOT NULL`。 |
| `attribution_id` | `char(64)` | deterministic `resultIdentity`；lowercase SHA-256。 |
| `idempotency_key` | `char(64)` | lowercase SHA-256；business unique key 的组成部分。 |
| `outcome_source` | `varchar(32)` | closed enum/check。 |
| `outcome_status` | `varchar(32)` | closed enum/check。 |
| `observed_at` | `timestamptz` | UTC instant，`NOT NULL`。 |
| `evaluation_time` | `timestamptz` | 显式策略时间，`NOT NULL`。 |
| `policy_id` | `varchar(128)` | `NOT NULL`。 |
| `policy_version` | `varchar(128)` | `NOT NULL`。 |
| `canonical_hash` | `char(64)` | lowercase SHA-256，`NOT NULL`。 |
| `attribution_status` | `varchar(32)` | closed enum/check。 |
| `confidence` | `numeric(6,5)` | `0 <= value <= 1`。 |
| `dimension` | `varchar(64)` | closed enum/check；每 attribution 唯一。 |
| `contribution` | `numeric(6,5)` | `-1 <= value <= 1`。 |
| `impact` | `varchar(16)` | `POSITIVE/NEGATIVE/NEUTRAL`，必须与 contribution 符号一致。 |
| `reason_code` | `varchar(64)` | `[A-Z][A-Z0-9_]{0,63}`。 |
| `evidence_ref` | `varchar(256)` | 仅安全引用；应用 guard + DB 长度保护。 |
| `audit_ref` | `varchar(256)` | 仅安全引用；不得承载 payload。 |
| `replay_ref` | `varchar(256)` | 仅安全引用；不得承载 payload。 |
| `reference_kind` | `varchar(16)` | `EVIDENCE/AUDIT/REPLAY/EVALUATION`。 |
| `reference_status` | `varchar(24)` | `VALIDATED/HELD/RELEASED/INVALID`。 |
| `created_at` | `timestamptz` | 数据库 transaction time，immutable。 |

字段长度不得静默截断；非法值必须在 domain/use-case 与 DB constraint 两层 fail-closed。

### 4.3 主键、唯一键与复合约束

```text
observation PK:
  (tenant_id, environment, observation_id)

observation correlation unique:
  (tenant_id, environment, decision_id, trace_id, observation_id)

attribution PK:
  (tenant_id, environment, attribution_id)

business unique:
  (tenant_id, environment, idempotency_key)

attribution -> observation FK:
  (tenant_id, environment, decision_id, trace_id, observation_id)

contribution PK:
  (tenant_id, environment, attribution_id, dimension)

reference PK:
  (tenant_id, environment, attribution_id, reference_kind, reference_identity)
```

所有新表内部 FK 都必须包含 `tenant_id + environment`。不得创建允许 cross-tenant 或 cross-environment 关联的单列 FK。

对旧 V5/V6/V8/V9 decision/trace/audit/replay 表不默认增加强 FK：B1 必须先审查现有生命周期和 key 结构。初始实现使用 tenant/environment-bound validation port 在事务写入前验证目标存在；只有证明兼容且不产生删除/回填/锁表风险时，才可另行冻结 composite FK。

### 4.4 索引

最小索引集合：

```text
(tenant_id, environment, decision_id, observed_at DESC, attribution_id DESC)
(tenant_id, environment, trace_id, observed_at DESC, attribution_id DESC)
(tenant_id, environment, observation_id)
(tenant_id, environment, policy_id, policy_version, observed_at DESC, attribution_id DESC)
(tenant_id, environment, attribution_status, observed_at DESC, attribution_id DESC)
(tenant_id, environment, observed_at DESC, attribution_id DESC)
(tenant_id, environment, reference_kind, reference_status, created_at)
```

不得为低选择性字段创建不带 tenant/environment 前缀的全局索引。B1 review 必须提供 `EXPLAIN` 假设与索引写放大评估；本计划不声称 production capacity。

### 4.5 JSONB

`JSONB` 只允许用于 observation 的 `measurements` 或 `safe_metadata`，且必须同时满足：

```text
schema/version 固定
top-level key allowlist
最多 32 entries
key <= 64 chars
value/ref <= 256 chars
serialized bytes <= 8192
禁止嵌套对象与数组，除非 B1 单独冻结 schema
应用层 canonical validation + DB size/check guard
不得保存 raw prompt、raw provider response、完整外部 payload 或凭证
```

contribution、audit/replay reference 不得塞入任意 JSONB；应使用可约束关系行。若 B1 不能证明 JSONB 必要，优先规范化子表。

### 4.6 敏感与交易语义禁止

禁止落库：

```text
raw prompt
raw provider response
Authorization header
credential / API key / HMAC secret / database password
完整外部 payload
BUY / SELL / PLACE_ORDER / MARKET_ORDER / CANCEL_ORDER execution instruction
任何可触发 NQ、订单、风控、账务、Paper 或 LIVE 的命令
```

## 5. 事务、幂等与 commit-unknown

### 5.1 首次写入事务

首次写入固定为一个由 use-case application service 管理的事务：

```text
validate tenant/environment/decision/trace references
-> claim (tenant, environment, idempotency_key)
-> insert immutable observation
-> insert attribution
-> insert all contributions
-> persist/validate audit + replay + evaluation references
-> write success audit reference
-> commit
```

observation、attribution、contribution、reference 与成功 audit 状态不可产生半完成提交。audit/reference validation 失败、duplicate conflict、DB error 或 timeout 必须整体 rollback。

transaction manager 缺失必须启动失败或 use-case fail-closed；不得切换到非事务 fallback。事务内不得执行真实 HTTP、Provider、NQ 或其他不受控外部调用。

### 5.2 幂等语义

```text
business identity:
  (tenant_id, environment, idempotency_key)

same key + same canonical_hash:
  返回已提交的 immutable attribution；不重复 observation/contribution/audit 写入

same key + different canonical_hash:
  IDEMPOTENCY_CONFLICT；不覆盖、不合并、不创建第二结果

cross-tenant or cross-environment:
  永不复用
```

并发竞争必须由数据库 unique constraint 决胜；loser 只读回查已提交行并比较 hash。不得依赖 JVM lock、全局 static map 或“先查再写”作为唯一保护。

### 5.3 commit-unknown 与 retry

```text
runtime retry: 0
commit outcome unknown: UNKNOWN / FAIL_CLOSED
automatic success claim: forbidden
automatic write retry: forbidden
```

连接在 commit 阶段丢失时，调用方收到稳定 `PERSISTENCE_OUTCOME_UNKNOWN`；后续仅允许使用同 tenant/environment/idempotency key 的只读 reconciliation：

- 已存在且 hash 相同：返回已提交结果；
- 已存在且 hash 不同：`IDEMPOTENCY_CONFLICT`；
- 不存在：保持 unknown，由明确重试入口或人工/任务编排决定，不自动二次写。

B2 必须测试 commit-unknown 模拟，不得把数据库原始错误、SQL 或内部路径暴露给调用方。

## 6. Historical evidence internal read model

### 6.1 查询边界

QDR-9 只新增 internal use-case/read model，不新增 Controller、OpenAPI 或外部 endpoint。所有 query command 必须显式提供：

```text
tenantId
environment
exactly one primary selector or an approved combined filter
time range where applicable
page size
opaque keyset cursor
```

支持：

- by `decisionId`
- by `traceId`
- by `observationId`
- by `attributionId`
- by `policyId + policyVersion`
- by `attributionStatus`
- by `observedAt` range

输出只允许：

```text
structured attribution summary
safe evidence/audit/replay/evaluation reference
policy identity/version
status / confidence / dimension / contribution / reason code
observedAt / evaluationTime / createdAt
```

不得输出 canonical raw input、raw payload、prompt、provider response、credential 或数据库内部异常。

### 6.2 分页、排序与上限

```text
pagination: keyset only
stable order: observed_at DESC, attribution_id DESC
default page size: 50
hard max page size: 100
max time range: 90 days
max returned rows per request: 100
cursor: opaque, versioned, tenant/environment/filter-bound, tamper-rejected
```

不得使用无上限查询；不得用大 offset 扫描。缺失时间范围的 list query 默认最近 30 天，且仍受 90 天 hard max 约束；by exact attributionId/observationId 可免时间范围但只能返回 tenant/environment-bound exact result。

### 6.3 错误行为

| 场景 | 行为 |
|---|---|
| 合法条件无数据 | 返回空集合或 `NOT_FOUND`（exact lookup）；不返回 `null`。 |
| cross-tenant/cross-environment | `NOT_FOUND` 或稳定 access-denied，禁止泄露目标存在性。 |
| 非法 selector/cursor/range/page size | validation error，查询不执行。 |
| reference validation failed | 结果标记为不可用并 fail-closed；不得返回未经验证的 safe ref。 |
| database timeout/error | 稳定 `READ_MODEL_UNAVAILABLE`；不返回 partial result、不 fallback 到跨 scope 查询。 |

## 7. Retention、cleanup 与 integrity

### 7.1 默认策略

```text
default retention: 365 days from observed_at
cleanup scope: one tenant + one environment per invocation
max batch size: 100
transaction timeout: 5 seconds
multi-instance safety: FOR UPDATE SKIP LOCKED or reviewed equivalent
failure behavior: rollback batch + fail-closed
runtime retry: 0
```

365 天只是默认配置，不是 production capacity 结论。配置必须位于后续稳定 `config/**` 机器合同路径，不能让 code/CI 依赖可裁剪的 `docs/current/DH_STAGE_QDR_9_PLAN.md`。

### 7.2 可删除与阻止删除

仅当 observation/attribution 全部超过 retention 且以下引用均不存在 active hold 时，才允许按完整聚合删除：

```text
AUDIT
REPLAY
EVALUATION
legal/compliance hold（若未来定义）
```

`EVIDENCE` 引用必须先经完整性检查证明目标不再需要，或由明确 retention policy 标记 `RELEASED`。不得只删 parent 留下孤立 contribution/reference，也不得只删 child 破坏 immutable aggregate。

删除审计与实际删除必须在同一事务：

```text
select eligible aggregate with tenant/environment scope
-> lock bounded batch
-> revalidate all holds/references
-> write deletion audit identity/hash/count/reason
-> delete child then parent via constrained cascade or explicit ordered delete
-> commit
```

禁止无条件物理删除、跨 tenant batch、未验证引用删除、超时后静默继续。清理失败不改变 eligibility 或 reference 状态。

### 7.3 完整性

B4 至少提供：

- orphan observation/attribution/contribution/reference detection；
- canonical hash 和 deterministic result identity 重算；
- tenant/environment/decision/trace correlation verification；
- invalid/unknown enum 与 size guard detection；
- active reference hold 与 retention eligibility reconciliation；
- 只读 report + bounded repair plan；不得自动猜测或跨租户修复。

## 8. 九类 scope 冻结

### 8.1 当前 planning task

```text
READ_SCOPE:
  README.md
  AGENTS.md
  CLAUDE.md
  docs/current/**
  docs/gates/stage-qdr-8/**
  dh-domain/src/main/**/qdr/feedback/**
  dh-domain/src/test/**/qdr/feedback/**
  dh-usecase/src/main/**/qdr/feedback/**
  dh-usecase/src/test/**/qdr/feedback/**
  bounded transaction/idempotency/read-model JDBC patterns in dh-usecase/dh-infra/dh-app
  dh-app/src/main/resources/db/migration/**
  dh-app/src/main/java/**Config.java
  dh-app/src/test/**
  config/**
  pom.xml and module pom.xml
  .github/workflows/**

WRITE_ALLOWLIST:
  README.md
  AGENTS.md
  CLAUDE.md
  docs/current/DH_STAGE_QDR_9_PLAN.md
  docs/current/README.md
  docs/current/STATUS.md
  docs/current/WORK_ORDER.md
  docs/current/ROADMAP.md
  docs/current/TESTING.md
  docs/current/WORKLOG.md
  docs/current/CODEX_PROJECT_INSTRUCTIONS.md
  docs/current/FACTSOURCE_POLICY.md
  docs/current/ARCHIVE_INDEX.md

VALIDATION_SCOPE:
  WRITE_ALLOWLIST
  forbidden-scope git diffs
  12 terminal factsources
  Maven quality reactor

FIXABLE_BLOCKER_SCOPE:
  WRITE_ALLOWLIST

CURRENT_FACTSOURCE_SCAN_SCOPE:
  the 12 terminal factsources
```

### 8.2 后续 implementation work order 冻结上界

本计划先冻结 implementation 的最大可写上界；下一 Work Order 只能进一步缩小到 exact files，不能扩大。`V15` 是当前 migration 序列的下一版本；若 Work Order 开始前仓库版本发生变化，必须停止并重新评审版本号，不得覆盖或重用已有 migration。

```text
IMPLEMENTATION_WRITE_ALLOWLIST_MAX:
  dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
  dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
  dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
  dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
  dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
  dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
  dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql
  dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
  dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java
  dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/**
  dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
  config/qdr9-feedback/**
  README.md
  AGENTS.md
  CLAUDE.md
  docs/current/DH_STAGE_QDR_9_PLAN.md
  docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
  docs/current/README.md
  docs/current/STATUS.md
  docs/current/WORK_ORDER.md
  docs/current/ROADMAP.md
  docs/current/TESTING.md
  docs/current/WORKLOG.md
  docs/current/CODEX_PROJECT_INSTRUCTIONS.md
  docs/current/FACTSOURCE_POLICY.md
  docs/current/ARCHIVE_INDEX.md

MIGRATION_SCOPE:
  dh-app/src/main/resources/db/migration/V15__qdr9_structured_feedback_persistence.sql

REPOSITORY_SCOPE:
  dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
  dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
  dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
  dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/**
  DecisionPipelineWiringConfig.java and its exact test

READ_MODEL_SCOPE:
  the same bounded usecase/infra feedback packages above
  dh-app/src/test/java/com/guidinglight/decisionhub/qdr9/**
  Controller/OpenAPI are excluded

API_SCOPE:
  EMPTY / NOT AUTHORIZED FOR QDR-9
  any future API requires a separate API/security review and task
```

### 8.3 Scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ IMPLEMENTATION_WRITE_ALLOWLIST_MAX: PASS
REPOSITORY_SCOPE ⊆ IMPLEMENTATION_WRITE_ALLOWLIST_MAX: PASS
READ_MODEL_SCOPE ⊆ IMPLEMENTATION_WRITE_ALLOWLIST_MAX: PASS

implementation work order gate:
6 OF 6 PASS REQUIRED
```

本 planning task 的六个 subset 均已在路径上闭合。下一 Work Order 必须把 glob 上界缩成 exact file list，并再次验证 `6 OF 6 PASS`；任何 scope 扩大、migration 版本漂移或 exact file 未闭合都输出 `TASK_SCOPE_DESIGN_INVALID`，不得开始 implementation。

## 9. 实施批次与 review 规则

### B1 — Persistence Contracts and Schema Freeze

冻结：

- aggregate 与 immutable lifecycle；
- exact tables/columns/types/lengths/checks；
- composite PK/FK/unique/index；
- migration forward/rollback/compatibility/lock risk；
- reference validation port；
- retention hold 与 deletion audit 模型；
- exact implementation `READ_SCOPE/WRITE_ALLOWLIST`，达到 6/6。

`B1` 涉及 schema/migration，必须独立 `db-schema-migration-review`/安全评审并 freeze；评审不授权执行 migration。

### B2 — PostgreSQL / JDBC Persistence Baseline

实现：

- reviewed migration；
- tenant/environment-bound JDBC persistence；
- atomic first write；
- same key/same hash reuse；
- same key/different hash conflict；
- duplicate race 与 commit-unknown fail-closed；
- audit/reference failure rollback；
- PostgreSQL/Testcontainers restart persistence。

B2 是 Repository/事务/幂等 milestone，必须 milestone review。不得使用 global static map，不得新增 API。

### B3 — Historical Evidence Read Model

实现内部 query port/use-case/JDBC adapter、安全 DTO、keyset pagination 与错误转换。正常 validation 收口；若发现 Controller/API 需求必须停止，转入独立 API/security review，不得在本 stage 自动扩 scope。

### B4 — Integrity / Retention / Replay Link Validation

实现 orphan/hash/scope/reference checks 与 bounded cleanup。涉及 delete/retention，必须安全 review；任何 active audit/replay/evaluation hold 都必须阻止删除。

### B5 — Stage-QDR-9 Final Close

完成：

```text
targeted tests
full regression
PostgreSQL/Testcontainers real execution
ArchitectureTest
quality
scope and boundary scans
current factsource sync
self-contained archive packet
source-documents copy
archive close commit
separate annotated tag close
post-tag current pruning
```

B5 必须 final close review。普通 DTO 或内部查询变更不创建额外重复 review。

## 10. 测试矩阵

| 类别 | 必测项 |
|---|---|
| Domain/schema | 合法字段；null/blank；长度边界；非法 environment/source/status；confidence 0/1/越界；contribution -1/0/1/越界；impact 符号；stable canonical hash；immutable collection；敏感/交易语义禁止；DB checks。 |
| Persistence | 首次写入；same key/same hash；same key/different hash；事务回滚；audit/reference failure rollback；duplicate-key race；cross-tenant；cross-environment；并发相同输入；并发冲突输入；commit unknown；PostgreSQL restart persistence。 |
| Correlation | decision/trace/observation 不存在；tenant/environment mismatch；validation port unavailable；旧表不可误关联；内部 composite FK 无越界。 |
| Query/read model | by decision/trace/observation/attribution/policy/status/time；稳定排序；keyset cursor；默认/最大 page size；90 天上限；empty/not-found；cursor tamper；cross-tenant/environment；DB timeout/error fail-closed。 |
| Retention | 无 active ref 可清理；audit/replay/evaluation hold 拒绝；cross-tenant 不清理；batch 100；timeout 5s；并发 cleanup；`SKIP LOCKED`；删除审计回滚；orphan/hash mismatch。 |
| Architecture | no API/Controller/OpenAPI；no external HTTP/Provider/NQ/Agent/LangGraph；no Prompt/Strategy/Experience/Pheromone mutation；no order/risk/ledger/Paper/LIVE mutation；no raw prompt/provider response/payload persistence；machine contract 不依赖 prunable current doc。 |
| Regression/quality | targeted module tests；19-reactor full regression；PostgreSQL/Testcontainers zero mandatory skips；ArchitectureTest；Checkstyle 0；Spotless PASS。 |

性能/规模验证必须明确数据规模、query plan、索引与批次假设，但不得把该验证写成 Stage-QDR-7 B2 production capacity acceptance。

## 11. 错误、安全与可观测性

建议稳定内部错误码：

```text
FEEDBACK_PERSISTENCE_UNAVAILABLE
FEEDBACK_REFERENCE_INVALID
IDEMPOTENCY_CONFLICT
PERSISTENCE_OUTCOME_UNKNOWN
READ_MODEL_VALIDATION_FAILED
READ_MODEL_NOT_FOUND
READ_MODEL_UNAVAILABLE
RETENTION_REFERENCE_HELD
RETENTION_CLEANUP_FAILED
INTEGRITY_CHECK_FAILED
```

日志只允许结构化安全字段：

```text
traceId
tenantId
environment
decisionId
observationId
attributionId
idempotency key hash prefix or correlation token
error code
duration
row count
```

不得记录 canonical raw value、payload、prompt、provider response、credential、SQL 参数全文或个人敏感信息。cross-tenant/cross-environment 请求不得通过错误差异泄露目标存在性。

## 12. 回滚与停止条件

### 12.1 回滚

- Planning docs：revert 本 planning docs commit；不修改 Stage-QDR-7/8 tag/history。
- B1：若仅 contract/schema freeze，revert 对应 docs/contract commit。
- B2 migration：必须在 migration review 中提供 forward-compatible rollback；已应用 migration 不删除历史版本，使用后续 corrective migration；新读写 wiring 必须可 feature-disable。
- B2/B3/B4 code：每 batch 独立 commit，优先 `git revert <commit>`；不得 `reset --hard` 或重写已发布历史。
- Retention：默认 disabled；只有完整性验证和 dry-run report 通过后才可启用。错误删除必须有 deletion audit 和备份/恢复方案，不能依赖日志猜测。

### 12.2 停止条件

出现以下任一情况立即停止，不扩大 allowlist：

- implementation scope 6/6 未通过；
- 需要 API/Controller/OpenAPI；
- 需要修改旧 `dh_nq_feedback_events` 语义或保存 raw payload；
- 无法保证 tenant/environment composite isolation；
- audit/replay/evaluation target 无法 fail-closed 验证；
- 需要非零自动 retry 处理有副作用写入；
- cleanup 不能保证 active reference hold；
- 需要真实 HTTP、Provider、NQ、Agent、LangGraph、Paper 或 LIVE；
- 需要自动修改 Experience、Pheromone、Prompt、模型、策略、候选或 JudgeDecision；
- 新 P0/P1 安全、tenant、事务、migration 或 API blocker。

## 13. Archive、tag 与 pruning discipline

1. 每个 batch 原始 plan/WO/review/validation 文档必须在 pruning 前复制到 `docs/gates/stage-qdr-9/source-documents/`。
2. archive packet 必须 self-contained，不能用 aggregate summary 替代原始 source documents。
3. 长期机器合同必须迁入稳定 `config/**` 或 `contracts/**`；production code/test/CI/scripts/deploy 不得依赖本 plan。
4. final close review PASS 后才允许 final-close docs commit；archive close 与 tag close 是不同任务。
5. annotated tag 必须指向包含完整 archive packet 的 archive commit，且只有 separate explicit authorization 才可创建/push。
6. tag close 后才允许 post-tag `docs/current` pruning；pruning 前必须扫描 machine dependencies。
7. 本 planning task 不 archive、不 tag、不 pruning。

## 14. 完成决策与下一任务

```text
STAGE_QDR_9_PLAN: DONE / LOCAL_ACCEPTED
STAGE_QDR_9_MAINLINE:
  STRUCTURED_FEEDBACK_PERSISTENCE_AND_HISTORICAL_READ_MODEL

ALLOW_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_9_IMPLEMENTATION_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_AUTOMATIC_LEARNING: NO

B2_CAPACITY_GATE: DEFERRED / KNOWN_LIMITATION
PRODUCTION_CAPACITY: NOT_PROVEN
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME: NO
ALLOW_AGENT: NO
ALLOW_LANGGRAPH: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
```

下一精确任务：

```text
DH-STAGE-QDR-9-IMPLEMENTATION-WORK-ORDER
```

该任务只能冻结 exact implementation files、B1 schema review 输入和 6/6 scope；不得在 Work Order 任务中顺带实施。

## 15. 本轮实际验证记录

```text
git diff --check: PASS / EXIT 0
approved planning docs changed: 13 OF 13
unexpected files: 0
terminal factsources: PASS / 12 OF 12
terminal authority hashes: PASS / 1 UNIQUE HASH
current conflicts: 0
scope invariants: PASS / 6 OF 6
production Java diff: 0
test diff: 0
migration diff: 0
API / Controller diff: 0
Repository diff: 0
contracts / golden_cases diff: 0
POM / workflow diff: 0
Stage-QDR-7/8 archive diff: 0
mvn -B -ntp -Pquality validate: PASS / EXIT 0
quality reactor: PASS / 19 OF 19
Checkstyle: 0 VIOLATIONS
Spotless: PASS
full regression: NOT RUN / NOT REQUIRED FOR PLANNING_ONLY
local PostgreSQL/Testcontainers: NOT RUN / NOT REQUIRED FOR PLANNING_ONLY
staged files: 0
commit / push / tag: NOT EXECUTED
```
