# DH Stage-QDR-6 B3 Snapshot Persistence Gap Work Order

## 1. 工单身份与结论

```text
task: DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER
classification: WORK_ORDER_ONLY + ADDITIVE_MIGRATION_PLAN + TENANT_BOUND_PERSISTENCE_PLAN + IMPLEMENTATION_BATCH_DESIGN + TEST_MATRIX_DESIGN
baseline: 8bd6e0786dda620b9dbf9e6ffb30bbe16d0b0d88
Stage-QDR-6 B1: DONE / COMMITTED / 92cc23a
Stage-QDR-6 B2: DONE / COMMITTED / 78a6750
canonical snapshot contract: FROZEN
persistence gap review: DONE / PERSISTENCE_DESIGN_FROZEN
existing persistence sufficient: NO
additive migration required: YES
production port/JDBC expansion required: YES
Stage-QDR-6 B3 implementation: NOT_STARTED
SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: DONE
```

本工单只把已冻结的 persistence design 拆成 B3-P1、B3-P2、B3-P3 三个可审查批次；不实现 Java、测试、migration、API、runtime wiring、snapshot assembler、canonicalizer、hash 或 deterministic replay。

## 2. Code reality 与冻结基线

### 2.1 Proposed V10 baseline

未来唯一候选 migration：

```text
dh-app/src/main/resources/db/migration/V10__qdr6_canonical_replay_snapshot.sql
table: qdr_canonical_replay_snapshot
mode: additive / insert-find only / no V1-V9 edit / no legacy backfill
```

V10 必须复用 `DH_STAGE_QDR_6_B3_SNAPSHOT_PERSISTENCE_GAP_REVIEW.md` 已冻结的表、字段、tenant key、composite FK、version vector、payload allowlist/size、COMMENT、retention 和 rollback 设计。任何 schema 漂移先停止，不得在实现中临时改写冻结方案。

### 2.2 Existing schema and port paths

| Baseline | Existing path / fact | B3 use |
| --- | --- | --- |
| V5 | `V5__dh_decision_pipeline_audit.sql`；`DecisionReplayQueryRepository` / `JdbcDecisionReplayQueryRepository` | `tenantId + decisionId` correlation 与 structured subject/snapshot metadata；不能冒充完整 canonical context |
| V6 | `V6__qdr_decision_core_baseline.sql`；`DecisionReadModelQueryPort` / `JdbcDecisionReadModelQueryAdapter` | exact tenant + run/request identity；不得开放 generic raw payload |
| V8 | `V8__qdr_model_gateway_persistence_baseline.sql`；prompt/model/gateway ports 与 JDBC repositories | exact prompt/model/call identity、safe refs 与 checksum metadata |
| V9 | `V9__qdr_replay_evaluation_baseline.sql`；replay/evaluation/regression tenant-bound repositories | exact row UUID + business ID + policy/hash lineage |

现有缺口：V5 payload 仅有 metadata；V6 port 不暴露 canonical context；`PromptVersionPersistencePort` 缺少 tenant + prompt version UUID 查询；`ModelGatewayCallPersistencePort` 缺少 tenant + run + safe ref 查询；现有 `TransactionTemplate` precedent 未冻结 `REPEATABLE_READ`，且测试 wiring 可回退 direct mode，B3 production boundary 禁止该回退。

## 3. 全局实施规则与 stop conditions

- `tenantId` 必须是每个 production query 的第一边界；禁止 tenantless、UUID-only、`latest/current`、模糊或时间猜测查询。
- physical UUID、business ID、safe ref 必须分离并逐字段 exact compare；不得以字符串相似或创建时间推断相等。
- V5/V6/V8/V9 identity、完整 version vector、safe structured payload 任一缺失或冲突都 fail-closed。
- persistence 异常不得降级为 in-memory success；production transaction manager 缺失必须 fail-fast。
- 禁止 raw prompt、raw provider response、credential-like material、交易或执行指令。
- 发现 schema 与冻结设计不一致、需要修改 V1-V9、需要 API/runtime endpoint、需要 legacy 自动 backfill、tenant composite key 无法保证或发现 P0/P1 安全问题时立即停止。
- 如必须使用 `CREATE UNIQUE INDEX CONCURRENTLY`，立即停止并输出 `B3_NON_TRANSACTIONAL_MIGRATION_REVIEW_REQUIRED`；不得临时设置 Flyway 非事务执行。

## 4. B3-P1：Additive Migration + Persistence Contract

### 4.1 Boundary

允许：新增 V10、新 canonical snapshot persistence command/record/contract、migration presence/Flyway/schema tests。禁止：修改 V1-V9、新增 API/runtime wiring、assembler、canonicalizer、deterministic hash 或 replay executor。

### 4.2 Database preflight

P1 改动前必须在目标 PostgreSQL 环境执行并保存脱敏结果；没有真实结果不得写 `PASS`：

1. 记录 `dh_decision_request`、`decision_request`、`decision_run`、`qdr_prompt_version`、`qdr_model_version`、`qdr_model_gateway_call`、`qdr_replay_case`、`qdr_evaluation_case`、`qdr_regression_verdict` row count。
2. 对每个拟新增 composite unique key 做 `GROUP BY ... HAVING count(*) > 1` duplicate 检查。
3. 对拟纳入 composite FK 的列做 nullability 与 orphan candidate 检查。
4. 验证 V5/V6/V8/V9 tenant/physical/business identity candidate 完整性，不做修复或 backfill。
5. 使用目标规模、索引状态和 maintenance window 评估 unique constraint/table scan/lock duration、blocking 与 timeout 风险。
6. 验证 V10 全部 DDL 可由 Flyway 在单一 PostgreSQL transaction 中执行并失败回滚。

任一 duplicate、null、orphan 或 lock window 不可接受时 P1 `BLOCKED`；不得用 cleanup SQL、自动 backfill 或非事务 concurrent index 绕过。

### 4.3 Schema and constraints

V10 必须包含：

- `qdr_canonical_replay_snapshot`，`id` primary key，`unique (tenant_id, id)`、`unique (tenant_id, snapshot_id)` 与冻结的 aggregate identity unique constraint。
- 以 tenant-aware key 关联 V5 request、V6 request/run、V8 prompt/model/gateway call、V9 replay/evaluation/regression rows；V6 run/request pair 与 V8 run/prompt/model/call tuple 必须由 composite identity 约束。
- 全部 required version vector 列 `NOT NULL`、非空且禁止 `latest/current/default`；固定值为 `QDR6-REPLAY-INPUT-1`、`QDR6-CONTEXT-1`、`QDR6-CJSON-1`、`QDR6-MOCK-REPLAY-1`、`SHA-256`。
- structured JSON 顶层类型、required hash/checksum 格式、optional lineage pair、`READ_ONLY_RECOMMENDATION` 与 payload limits 的 `CHECK`。
- canonical payload 总上限 `262144` bytes；`context_payload_json <= 131072`、`evidence_refs_json <= 65536`、`expected_decision_summary_json <= 32768`，超限拒绝且不得截断。
- `(tenant_id, snapshot_id)`、`(tenant_id, decision_run_id)`、`(tenant_id, decision_id)`、`(tenant_id, model_call_ref)`、`(tenant_id, replay_case_id)` 必要索引；不以 tenantless lookup index 形成 repository contract。
- table/column/index/constraint/trigger 中文 `COMMENT`，明确 safe structured material、audit-only `created_at`、no raw material、no execution semantics。

### 4.4 Immutable strategy

- production port 只有 insert/find，不提供 update/delete/listAll。
- table-level trigger 必须拒绝任何 `UPDATE`；runtime role 不授予 `DELETE`。
- unique identity 冲突后读取同 tenant exact row，并以所有 immutable persisted fields 做 constant policy exact comparison：完全一致才幂等返回，任一字段不同即 structured conflict。
- P1/P3 不实现 `QDR6-CJSON-1` canonicalizer 或 deterministic SHA-256。冻结字段中的 hash/checksum metadata 仅保存 source 已有 immutable hashes/checksums；不得伪造 `canonicalInputHash`、把 source hash 重新标成 canonical hash，或以 placeholder/default 填充。若 V10 最终要求在 P3 insert 前生成新的 canonical hash，则 P1 停止并进入 canonicalizer sequencing review，不得偷带算法实现。

### 4.5 P1 deliverables and acceptance

- 独立 commit：`V10` + persistence contracts + P1 migration/schema tests + 最小 current docs。
- migration presence/order、clean database migration、existing V1-V9 upgrade、composite FK/unique/check/trigger/comment、duplicate/required version/payload rejection、transaction failure rollback 均有测试证据。
- PostgreSQL/Testcontainers 必须真实运行；Docker 不可用只能记录 `BLOCKED/NOT RUN`，不得将 migration milestone 写成 accepted。
- P1 完成后不授权 P2/P3；等待 P2 独立实现后统一 milestone review。

## 5. B3-P2：Tenant-bound Ports / JDBC / Identity Validation

### 5.1 Boundary

允许：新增 `CanonicalReplaySnapshotPersistencePort` 与 tenant-bound JDBC adapter；为 prompt 补 tenant + UUID exact read；为 gateway call 补 tenant + run + safe ref exact read；复用 V5/V6/V8/V9 model/ports。禁止：schema 改动、API、runtime endpoint、tenantless scan、assembler、canonicalizer、hash 或 replay。

### 5.2 Port contracts

```text
CanonicalReplaySnapshotPersistencePort:
  insert(CanonicalReplaySnapshotRecord)
  findByTenantAndSnapshotId(tenantId, snapshotId)

PromptVersionPersistencePort:
  findByTenantAndPromptVersionId(tenantId, promptVersionId)

ModelGatewayCallPersistencePort:
  findByTenantAndDecisionRunIdAndModelCallRef(tenantId, decisionRunId, modelCallRef)
```

`ModelVersionPersistencePort.findByTenantAndModelVersionId`、V5 `DecisionReplayQueryRepository`、V6 `DecisionReadModelQueryPort`、V9 exact repositories 直接复用。不得增加 UUID-only、list-all、latest/current 或 createdAt ordering 查询。

### 5.3 Identity validation

1. 从 `tenantId` 开始读取 V5 decision、V6 request/run、V8 call/prompt/model、V9 replay/evaluation/regression。
2. 分离并验证 V5 `decisionId`、V6 request/run UUID、V8 call UUID/ref、V9 row UUID/business ID。
3. `tenantId/traceId/requestId/decisionId/decisionRequestId/decisionRunId/modelCallId/modelCallRef/promptVersionId/modelVersionId/replayCaseId` 以及 optional lineage 全部 exact match。
4. prompt/model ref + checksum、gateway version ref、V9 policy/hash 与 record 一致；不得以 display name 或 model ID 推导 gateway version。
5. missing、duplicate、conflict、cross-tenant 一律 structured fail-closed；数据库异常整体失败。

### 5.4 P2 deliverables and acceptance

- 独立 commit：ports/records/JDBC/identity validator + focused JDBC/unit tests + 最小 current docs。
- 测试覆盖 tenant-bound insert/find、cross-tenant invisible、exact prompt/model/call identity、V6/V8 mismatch、tenantless query absent、identical duplicate idempotent、conflicting duplicate rejected、persistence exception no in-memory success。
- P2 不包含 transaction assembler；P1/P2 都完成后统一执行一次 persistence milestone review。

## 6. Persistence milestone review

P1 与 P2 独立 commit 后暂停，统一 review：

```text
P1 migration/schema: actual PostgreSQL evidence
P2 port/JDBC: tenant isolation and exact identity evidence
V1-V9 checksum compatibility: PASS
non-transactional migration sentinel: NOT_TRIGGERED
P0/P1 security finding: NONE
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION: review decision only
```

review 未通过不得实施 P3。migration、production JDBC/SQL、tenant/correlation 和 security boundary 的差异必须在该节点一次性收口，避免重复 review。

## 7. B3-P3：Snapshot Assembler + Persistence Integration

### 7.1 Boundary

允许：依据冻结合同读取 safe structured sources、构造完整 `ReplayInputSnapshot` persistence record、执行 identity/version/safety/size 校验，并在单一 local PostgreSQL transaction 中 insert/find。禁止：`QDR6-CJSON-1` canonicalizer、deterministic SHA-256、replay executor、API/provider/NQ/Agent/LIVE。

### 7.2 Transaction boundary

```text
transaction manager: production PostgreSQL manager / missing -> fail-fast
isolation: REPEATABLE_READ
1. tenant-bound exact read V5/V6/V8/V9
2. validate correlation and physical/business/safe-ref identity
3. validate complete version vector and source checksum/hash metadata
4. build strict-allowlist structured snapshot
5. enforce depth/item/pre-serialization byte limits
6. insert immutable snapshot or compare exact existing row for idempotency
7. commit
```

任一 read/validation/size/insert/compare failure 回滚；不写 V5-V9，不调用外部系统，不回退 direct/in-memory mode。`created_at` 只由数据库生成并只作 audit，不进入 snapshot identity 或未来 replay hash。

### 7.3 P3 deliverables and acceptance

- 完整 required snapshot fields 与 version vector 均可从 accepted P1/P2 boundary 构造；unknown/raw/credential/trading field 拒绝。
- aggregate-to-snapshot identity mapping 有稳定的 unit + JDBC integration evidence；相同 identity 的全字段相同记录幂等，不同记录拒绝。
- legacy V5-V9 row 无 canonical snapshot 时返回 `LEGACY_NOT_REPLAYABLE`；不 backfill。
- transaction read/validation/insert failure 均无 partial row；source failure fail-closed。
- PostgreSQL/Testcontainers 真实验证通过；architecture guard 证明无 HTTP、Provider SDK、NQ、Agent、LangGraph 或 trading dependency。
- P3 完成仅说明 canonical snapshot persistence inputs 已可用；是否允许 canonicalizer/deterministic replay 必须另起 review，不自动授权。

## 8. Consolidated test matrix

### 8.1 Migration / schema

1. V10 presence 与 V1-V10 顺序。
2. clean database migration。
3. existing V1-V9 database upgrade，既有 checksum 不变。
4. composite FK 与 unique constraints。
5. UPDATE trigger rejection；production delete contract absent。
6. duplicate identity rejection。
7. required version 字段缺失/blank/latest/current/default rejection。
8. total/per-field payload limit rejection，无 truncation。
9. migration transaction 与 failure rollback。
10. table/columns/index/constraints/trigger COMMENT presence。

### 8.2 Port / JDBC

1. tenant-bound insert/find happy path。
2. cross-tenant read invisible/denied。
3. exact prompt/model/call identity。
4. V6/V8 identity mismatch rejection。
5. tenantless/latest/current query 不存在。
6. identical snapshot duplicate idempotent。
7. conflicting snapshot duplicate rejected。
8. persistence exception no in-memory success。

### 8.3 Assembler / integration

1. complete snapshot build。
2. required source missing rejection。
3. unsafe/raw/credential/trading field rejection。
4. legacy data `LEGACY_NOT_REPLAYABLE`。
5. transaction rollback，无 partial row。
6. source read failure fail-closed。
7. PostgreSQL/Testcontainers V1-V10 + JDBC integration。
8. no HTTP/Provider/NQ/Agent/LangGraph/trading dependency。
9. complete version vector and stable aggregate-to-snapshot mapping。
10. no canonicalizer/deterministic hash/replay executor implementation。

## 9. Review, rollback and stage impact

Review sequence：

```text
P1 commit
-> P2 commit
-> one persistence milestone review
-> review PASS
-> P3 commit
-> deterministic replay readiness review
```

Rollback：

- P1/P2/P3 分别独立 commit；未应用 migration 时使用 `git revert <commit>`，不 history rewrite。
- V10 一旦应用不得修改或删除；使用后续 forward fix/cleanup migration。
- 已生成 snapshot 作为 audit evidence 保留，优先停写并 forward-fix，不直接 drop。
- rollback 后重新运行 migration、targeted tests、module/full tests、`mvn -ntp -Pquality validate` 与 forbidden-scope scan。

Stage impact：B3 persistence gap work order 完成，不代表 B3 implementation、canonicalizer、deterministic replay、B4 或 Stage close 已开始。

## 10. Authorization matrix

```text
SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: DONE

ALLOW_B3_P1_MIGRATION_IMPLEMENTATION: YES
ALLOW_B3_P2_PORT_JDBC_IMPLEMENTATION_NOW: NO
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO

ALLOW_MIGRATION_CHANGE_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

`ALLOW_B3_P1_MIGRATION_IMPLEMENTATION: YES` 仅定义下一独立任务的 eligibility；本轮仍为 `NO_MIGRATION_CHANGE`。P1 开工前必须重新检查 branch/worktree/HEAD、执行数据库 preflight 并确认不需要 concurrent index。

## 11. Next concrete action

```text
DH-STAGE-QDR-6-B3-P1-CANONICAL-SNAPSHOT-MIGRATION
```

若 preflight 证明必须使用非事务 concurrent index：

```text
B3_NON_TRANSACTIONAL_MIGRATION_REVIEW_REQUIRED
```

若 schema、identity、hash sequencing 或安全边界无法遵守冻结设计：

```text
DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER-BLOCKER-FIX
```

## 12. Persistence milestone review resolution

2026-07-11 P1/P2 milestone review 结论为 `BLOCKED`。PostgreSQL/Testcontainers、tenant isolation、duplicate/immutable 与 transaction rollback evidence 均通过，但出现 work order stop conditions：

```text
B3_PERSISTENCE_SCHEMA_MISMATCH_BLOCKED
B3_PERSISTENCE_PORT_BOUNDARY_BLOCKED
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-PERSISTENCE-SCHEMA-BLOCKER-FIX
```

阻断原因是必填 `canonical_input_hash` 无 P3 合法 source、`created_at` 未保持 DB-generated，以及 V9 structured input/summary projection 缺少 JDBC exact source comparison。本 resolution 只同步 review 结论，不授权修改 V10、Java、测试、port/JDBC、API 或 runtime wiring。

## 13. Persistence schema blocker fix resolution

2026-07-11 blocker fix 已完成并通过真实 PostgreSQL/Testcontainers focused validation。V1-V10 保持 immutable；V11 只补 V10 constraints/indexes comments。Snapshot 写入合同只接受已完成 canonicalization/hash 的完整 material，且不允许 caller 提供 `created_at`；V9 input/hash/summary/optional lineage 均由 tenant-bound exact projection validation 保护。

后续 P3 原有“assembler 可先于 hash insert”的边界作废，固定顺序重排为：

```text
structured assembler
-> QDR6-CJSON-1 canonicalization
-> deterministic SHA-256 hash
-> REPEATABLE_READ identity validation
-> immutable persistence
```

当前授权保持：

```text
ALLOW_B3_PERSISTENCE_MILESTONE_REVIEW_RETRY: YES / NEXT_TASK_ONLY
ALLOW_B3_P3_IMPLEMENTATION_NOW: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW-RETRY
```
