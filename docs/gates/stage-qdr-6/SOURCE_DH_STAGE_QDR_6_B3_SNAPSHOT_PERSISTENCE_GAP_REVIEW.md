# DH Stage-QDR-6 B3 Snapshot Persistence Gap Review

## 1. Review identity

```text
task: DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-REVIEW
classification: REVIEW_ONLY + PERSISTENCE_GAP_DESIGN + ADDITIVE_SCHEMA_REVIEW + TENANT_BOUND_PORT_REVIEW + VERSION_IDENTITY_REVIEW
baseline: fc5c0547596924fb9393f4637cfde2e98dd3154b
canonical snapshot contract: FROZEN
SNAPSHOT_PERSISTENCE_GAP_REVIEW: DONE
PERSISTENCE_DESIGN_FROZEN: YES
ADDITIVE_MIGRATION_REQUIRED: YES
PRODUCTION_PORT_EXPANSION_REQUIRED: YES
JDBC_EXPANSION_REQUIRED: YES
ALLOW_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: YES
```

本 review 只冻结最小 additive persistence、tenant-bound port、identity mapping、legacy policy、transaction boundary 与测试计划；不新增或修改 migration、Java、Repository、JDBC、SQL、API、wiring、canonicalizer、hash 或 replay executor。

## 2. Current code reality

- V5 `dh_decision_context_snapshot` 以 `decision_id` 为主键，可按 `tenant_id + decision_id` 读取，但生产 payload 只有 snapshot metadata；没有完整 structured context、`contextSchemaVersion` 或 payload size gate。
- 当前 `DecisionContextSnapshot` 只含 `snapshotId/capturedAt/evidenceRefs`；完整 safe context 必须组合现有 `DecisionRequest` 的 source、decision type、subject 与该 snapshot，而不是保存原始自然语言或 provider material。
- V6 `decision_request` 保存 JSON payload，`decision_run` 通过 `decision_request_id` 关联；现有 read model 已支持 `tenantId + decisionRunId` 校验，但没有直接 tenantless-safe 的 canonical payload 输出。
- V8 gateway call 通过 FK 关联 `decision_run/prompt_version/model_version`，并有 `unique(tenant_id, model_call_ref)`；prompt port 缺少按 `tenantId + promptVersionId` 查询，gateway port 缺少 `tenantId + decisionRunId + modelCallRef` exact identity 查询。
- V9 replay/evaluation/regression 表已使用大量 `(tenant_id, id/business-id)` composite FK 和 tenant-bound ports，可作为新 snapshot 的下游 baseline identity。
- `ReplayPersistenceGuard` 有递归 unsafe key/value 检查，但当前没有 snapshot bytes/payload size 上限。
- 现有 JDBC repository 多为独立语句；app 已有 `TransactionTemplate` precedent，但 canonical snapshot 尚无事务边界。

## 3. Immutable context field gap matrix

| Field | Existing source | Persisted now | Tenant-bound readable | Required | Proposed storage | Canonical hash | Missing risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `snapshotSchemaVersion` | frozen contract | NO | NO | YES | dedicated column | YES | snapshot shape ambiguous |
| `tenantId` | V5/V6/V8/V9 | YES | YES | correlation | dedicated column | YES | cross-tenant collision |
| `traceId` | V5/V6/V8/V9 | YES | YES | correlation | dedicated column | NO | source mismatch hidden |
| `requestId` | V5/V6/V8/V9 | YES | YES | correlation | dedicated column | NO | dual request identity drift |
| `decisionId` | V5 | YES | YES | correlation | dedicated column | NO | V5 source not provable |
| `decisionRequestId` | V6 | YES | through join | correlation | dedicated UUID column | NO | run/request mapping drift |
| `decisionRunId` | V6/V8 | YES | YES | correlation | dedicated UUID column | NO | V6/V8 mapping drift |
| `snapshotId` | V5 JSON/domain request | metadata only | via V5 JSON | YES | dedicated business key | YES | snapshot cannot be addressed immutably |
| `source` | V5 request | YES | YES | YES | dedicated column | YES | source semantics drift |
| `decisionType` | V5 request | YES | YES | YES | dedicated column | YES | unsafe decision type |
| `subject` | V5 request/domain request | YES | YES | YES | allowlisted JSON object | YES | analytical context incomplete |
| `contextSnapshot` | current domain request | PARTIAL | PARTIAL | YES | allowlisted JSON object | YES | deterministic input absent |
| `sourceCapturedAt` | context snapshot | YES in V5 JSON | YES | YES | dedicated `timestamptz` | YES | current clock substitution |
| `evidenceRefs` | V5/V6/B2 | refs only | YES | YES | structured JSON array | YES | evidence set drift |
| `decisionSchemaVersion` | V5 request | YES | YES | YES | dedicated column | YES | decision contract ambiguous |
| `contextSchemaVersion` | none | NO | NO | YES | dedicated column | YES | context bytes ambiguous |
| `policyVersion` | V9 | YES | YES | YES | dedicated column | YES | policy drift |
| `evaluationPolicyVersion` | V9 policy semantics | not separate | NO | YES | dedicated column | YES | decision/evaluation policy conflated |
| `promptVersionId/ref` | V8/prompt version | ID YES | ID not directly readable | YES | UUID + immutable ref/checksum columns | YES | prompt version unresolved |
| `modelVersionId/ref` | V8/model version | YES | YES | YES | UUID + immutable ref/checksum columns | YES | model drift |
| `modelGatewayVersionRef` | derived projection | NO independent value | NO | YES | dedicated explicit version column | YES | model version masquerades as gateway version |
| `modelCallId/ref` | V8 | YES | ref readable | strong identity | dedicated UUID/ref columns | ref YES | V6/V8 call mapping unproved |
| `replayCaseId/ref` | V9 | YES | YES | YES | row UUID + business ID | YES | replay baseline unresolved |
| `replayInputRef/hash` | V9 | YES | YES | YES | structured JSON + hash column | YES | source material unproved |
| `expectedDecisionSummary/hash` | V9 | YES | YES | YES | allowlisted JSON + hash column | YES | comparison baseline absent |
| `evaluationCaseId` | V9 | YES | YES | OPTIONAL lineage | nullable row UUID/business ID | NO | evaluation lineage absent |
| `regressionVerdictId` | V9 | YES | YES | OPTIONAL lineage | nullable row UUID/business ID | NO | verdict lineage absent |
| `providerSummaryHash` | V8/Stage-QDR-5 | conditional | conditional | OPTIONAL | nullable hash column | YES when present | provider summary drift hidden |
| `canonicalizationVersion` | frozen contract | NO | NO | YES | dedicated fixed column | YES | byte rules ambiguous |
| `replayExecutorVersion` | frozen contract | NO | NO | YES | dedicated fixed column | YES | executor compatibility ambiguous |
| `hashAlgorithmVersion` | frozen contract | NO | NO | YES | dedicated fixed column | YES | hash interpretation ambiguous |
| `canonicalInputHash` | none | NO | NO | YES for persisted immutable row | dedicated hash column | output | duplicate conflict cannot be detected |
| `createdAt` | database audit | available | YES | audit only | dedicated column | NO | current time contaminates hash |

`context_payload_json` 的冻结 allowlist 仅包含当前 read-only decision contract 能表达的结构化字段：

```text
subject.symbol
subject.market
subject.timeframe
subject.strategyRef optional
subject.researchRef optional
contextSnapshot.snapshotId
contextSnapshot.capturedAt
contextSnapshot.evidenceRefs
```

不得将 V6 raw-like generic payload、自然语言 prompt、provider response、credential 或交易/执行字段复制进新 snapshot。

## 4. Option review

### Option A: 后续 ALTER V5 snapshot 表

`REJECTED`。V5 是既有 audit fact，扩列会把 legacy partial row 与 canonical snapshot 混在同一语义；大量 nullable/default 会诱发补值，并且仍不能解决 V6/V8/V9 identity 与 immutable version vector。

### Option B: 独立 canonical replay snapshot 表

`ACCEPTED AS FOUNDATION`。可以独立定义 tenant isolation、append-only、version vector、hash 和 payload limit，不改变 V5-V9 历史语义。

### Option C: 仅保存 structured context/version，运行时读取其他 refs

`REJECTED AS STANDALONE`。每次重放重新读取 V5/V6/V8/V9 会引入 time-of-read drift、缺失/多行歧义与非原子组合；也无法证明 snapshot 创建时的 exact version identity。

### Option D: 组合方案

`RECOMMENDED`：以 Option B 的独立 materialized snapshot 为权威；创建时通过 Option C 式 tenant-bound safe refs 在单一事务内校验 V5/V6/V8/V9，然后一次性持久化完整 allowlisted snapshot、version vector、source identities 和 canonical hash。后续 replay 只读该 immutable row，不重新以 latest/current 组装。

## 5. Proposed additive migration

规划文件名：

```text
V10__qdr6_canonical_replay_snapshot.sql
```

V10 只做 additive change，不编辑 V1-V9，不 backfill legacy row，不创建 API/wiring。

### 5.1 New table

```text
qdr_canonical_replay_snapshot
```

建议列：

```text
id uuid primary key
tenant_id varchar(128) not null
snapshot_id varchar(128) not null
decision_id varchar(128) not null
decision_request_id uuid not null
decision_run_id uuid not null
trace_id varchar(128) not null
request_id varchar(128) not null
source varchar(128) not null
decision_type varchar(64) not null
source_captured_at timestamptz not null

model_call_id uuid not null
model_call_ref varchar(512) not null
prompt_version_id uuid not null
model_version_id uuid not null
replay_case_row_id uuid not null
replay_case_id varchar(128) not null
evaluation_case_row_id uuid null
evaluation_case_id varchar(128) null
regression_verdict_row_id uuid null
regression_verdict_id varchar(128) null

subject_json jsonb not null
context_payload_json jsonb not null
evidence_refs_json jsonb not null
replay_input_ref_json jsonb not null
expected_decision_summary_json jsonb not null

snapshot_schema_version varchar(64) not null
decision_schema_version varchar(64) not null
context_schema_version varchar(64) not null
policy_version varchar(128) not null
evaluation_policy_version varchar(128) not null
prompt_version_ref varchar(256) not null
prompt_version_checksum varchar(64) not null
model_version_ref varchar(256) not null
model_version_checksum varchar(64) not null
model_gateway_version_ref varchar(128) not null
canonicalization_version varchar(64) not null
replay_executor_version varchar(64) not null
hash_algorithm_version varchar(32) not null

replay_input_hash varchar(64) not null
expected_summary_hash varchar(64) not null
provider_summary_hash varchar(64) null
canonical_input_hash varchar(64) not null
payload_bytes integer not null
created_at timestamptz not null default now()
```

### 5.2 Tenant keys and constraints

```text
unique (tenant_id, id)
unique (tenant_id, snapshot_id)
unique (tenant_id, decision_run_id, replay_case_row_id, snapshot_schema_version)
check decision_type = READ_ONLY_RECOMMENDATION
check fixed versions = QDR6-REPLAY-INPUT-1 / QDR6-CJSON-1 / QDR6-MOCK-REPLAY-1 / SHA-256
check all required version strings are non-empty and not latest/current/default
check all required hashes are lowercase 64-hex
check optional lineage ID pairs are both-null or both-non-null
check JSON columns have required object/array top-level types
check payload_bytes between 1 and 262144
```

V10 还需追加仅用于 tenant-aware FK 的 unique constraints：

```text
dh_decision_request (tenant_id, decision_id)
decision_request (tenant_id, id)
decision_run (id, decision_request_id)
qdr_prompt_version (tenant_id, id)
qdr_model_version (tenant_id, id)
qdr_model_gateway_call
  (tenant_id, id, decision_run_id, prompt_version_id, model_version_id)
```

新表 FK：

- `(tenant_id, decision_id)` -> V5 request。
- `(tenant_id, decision_request_id)` -> V6 request。
- `(decision_run_id, decision_request_id)` -> V6 run/request pair。
- `(tenant_id, model_call_id, decision_run_id, prompt_version_id, model_version_id)` -> V8 physical call/version identity；`model_call_ref` 继续由既有 `(tenant_id, model_call_ref)` unique constraint和应用 exact comparison 保证。
- `(tenant_id, prompt_version_id)` / `(tenant_id, model_version_id)` -> V8 version rows。
- `(tenant_id, replay_case_row_id)` -> V9 replay case。
- optional `(tenant_id, evaluation_case_row_id)` / `(tenant_id, regression_verdict_row_id)` -> V9 rows。

V5 `snapshotId` 仅存在 JSON/domain object，不能建立可靠 FK；新表的 `snapshot_id` 是 canonical snapshot 自身 business identity。V9 duplicated business IDs、version refs 和 hashes仍须由应用在同一事务内比对，数据库 FK 负责 row/tenant identity，不能把跨表语义比较伪装成 FK 已覆盖。

### 5.3 Immutability, retention and payload size

- production port 只允许 `insert/find`，不提供 update/delete。
- V10 建议创建 table-level trigger，拒绝 `UPDATE`；runtime role 不授予 `DELETE`。同 identity + 同 hash 为幂等返回，不同 hash 为 conflict。
- B3 不实现自动 purge。至少保留至 Stage-QDR-6 archive/tag 完成并满足项目 audit retention；删除必须另起 retention/governance review。
- canonical snapshot UTF-8 bytes 总上限冻结为 `262144` bytes；`context_payload_json <= 131072`、`evidence_refs_json <= 65536`、`expected_decision_summary_json <= 32768`，其余部分共享剩余额度。
- application 在 serialization 前按结构化对象估算并在 canonical bytes 生成后再次精确校验；数据库使用 `payload_bytes` 与 `octet_length(jsonb::text)` coarse upper-bound 防御。超限返回 `SNAPSHOT_PAYLOAD_TOO_LARGE`，不得截断。

查询索引至少包括 `(tenant_id, snapshot_id)` unique index、`(tenant_id, decision_run_id)`、`(tenant_id, decision_id)`、`(tenant_id, model_call_ref)` 和 `(tenant_id, replay_case_id)`；不得新增 tenantless lookup index 作为 repository contract。新表、JSON/version/hash/identity/created_at 字段和 immutable trigger 必须有中文 `COMMENT`，明确 no raw material、audit-only time 与 no execution semantics。

V10 是 Flyway versioned migration，不依赖重复执行时的静默 `IF NOT EXISTS` 继续；若目标对象异常预存在应 fail-fast。对 V5/V6/V8 增加 composite unique constraint 会扫描既有表并可能持锁，P1 work order 必须先记录 row count、duplicate/null preflight 和允许的 migration window。若生产规模要求 `CREATE UNIQUE INDEX CONCURRENTLY`，必须拆成明确 `executeInTransaction=false` 的独立 migration review，不能临时塞入事务型 V10。

## 6. Tenant-bound port design

### 6.1 New `CanonicalReplaySnapshotPersistencePort`

```text
insert(CanonicalReplaySnapshotRecord)
findByTenantAndSnapshotId(tenantId, snapshotId)
```

- 输入：完整 tenant-bound immutable record；禁止 tenantless selector。
- 输出：同租户 snapshot record。
- empty：读取返回 `Optional.empty`，上层映射 `SNAPSHOT_MISSING`。
- conflict：相同 identity/hash 幂等；相同 identity/不同 hash 抛 structured checksum conflict。
- exception：统一 persistence exception，事务整体 fail-closed。

不提供 `listAll`、UUID-only、latest/current、时间猜测或 update/delete。

### 6.2 Extend `PromptVersionPersistencePort`

```text
findByTenantAndPromptVersionId(tenantId, promptVersionId)
```

输出现有 safe `PromptVersionRecord`；empty 为 `VERSION_INPUT_MISSING`，多行/tenant conflict 为 `CORRELATION_MISMATCH`，数据库异常 fail-closed。不得选择 current version。

### 6.3 Reuse `ModelVersionPersistencePort`

现有 `findByTenantAndModelVersionId` 已满足 exact identity；不新增 latest lookup。返回 checksum/version 与 gateway record 必须一致。

### 6.4 Extend `ModelGatewayCallPersistencePort`

```text
findByTenantAndDecisionRunIdAndModelCallRef(tenantId, decisionRunId, modelCallRef)
```

输入必须三键齐全；返回 exact V8 safe record。empty 为 `VERSION_INPUT_MISSING/SNAPSHOT_INCOMPLETE`，多行或 run/ref 冲突为 `CORRELATION_MISMATCH`，异常 fail-closed。不得按 createdAt 取最后一条。

### 6.5 Reuse V5/V6/V9 ports

- V5 `DecisionReplayQueryRepository` 已按 `tenantId + decisionId` 查询，可复用作 correlation/subject evidence；不能再作为完整 canonical context source。
- V6 `DecisionReadModelQueryPort.findDecisionRunDetail(tenantId + decisionRunId)` 已能证明 tenant/run/request identity；无需扩大为 raw payload port。
- V9 repositories 已有 tenant + exact ID/business ID 查询；禁止使用 `listByTenant` 猜测 baseline。

## 7. Frozen identity mapping

| Identity | Role | Constraint | Application validation |
| --- | --- | --- | --- |
| `tenantId` | root security key | every unique/FK/read starts with tenant where source supports it | all records equal or `TENANT_MISMATCH` |
| `traceId` | strong correlation | indexed/stored, not global unique | exact equality across V5/V6/V8/V9 |
| `requestId` | strong correlation | stored, not global unique | exact equality; no requestKey substitution |
| `decisionId` | V5 strong identity | composite unique/FK with tenant | equals V9 sourceDecisionId when present |
| `decisionRequestId` | V6 physical identity | composite tenant FK | owns decisionRunId |
| `decisionRunId` | V6/V8 strong identity | run/request pair FK + V8 wide FK | exact UUID equality |
| `snapshotId` | canonical business identity | unique with tenant | never derived from time/current row |
| `promptVersionId` | V8 strong version identity | tenant composite FK | ref/checksum exact match |
| `modelVersionId` | V8 strong version identity | tenant composite FK | ref/checksum exact match |
| `modelGatewayVersionRef` | required semantic version | non-empty/fixed policy column | must be explicit; cannot derive from modelVersionId |
| `modelCallRef` | V8 strong call identity | wide unique/FK with tenant/run/versions | exact ref, no V5 provider-log ID substitution |
| `replayCaseId` | V9 strong baseline identity | tenant row FK + duplicated business ID | row/business ID/policy/hash match |
| `evaluationCaseId` | optional lineage | nullable tenant row FK | both-null or exact row/business ID |
| `regressionVerdictId` | optional lineage | nullable tenant row FK | both-null or exact row/business ID |

双 ID 模型风险：V5 `decisionId` 当前常与 requestId 相近、V6 同时有 request UUID/requestId、V9 同时有 row UUID/business ID。任何相等关系都必须由明确字段验证；禁止通过字符串相似、创建时间或当前实现习惯推断。冲突统一拒绝，不选择“更可信”的一侧继续。

## 8. Frozen version vector persistence

以下全部 `NOT NULL` 且进入 canonical hash：

```text
snapshotSchemaVersion = QDR6-REPLAY-INPUT-1
decisionSchemaVersion
contextSchemaVersion = QDR6-CONTEXT-1 for the frozen allowlist
policyVersion
promptVersionRef
modelVersionRef
modelGatewayVersionRef
evaluationPolicyVersion
canonicalizationVersion = QDR6-CJSON-1
replayExecutorVersion = QDR6-MOCK-REPLAY-1
hashAlgorithmVersion = SHA-256
```

`promptVersionRef/modelVersionRef` 必须包含或关联 immutable ID + checksum；不能只保存展示名。任何缺失、`latest/current/default`、与 source checksum 不一致均拒绝 insert。

## 9. Safety and legacy policy

- snapshot 只接受第 3 节 allowlist；unknown key、raw material、credential-like key/value、交易/执行指令立即拒绝。
- `ReplayPersistenceGuard.rejectUnsafeJson` 可复用，但 P1/P3 必须增加 strict allowlist、depth/item count 和 byte-size gate；denylist 不能替代 allowlist。
- legacy V5-V9 row 默认 `LEGACY_NOT_REPLAYABLE`。V10 migration 创建空表，不自动 backfill。
- 只有未来独立 backfill review 能证明每个 required field/version/hash/identity 时才可生成 snapshot；当前已知 V5 partial row 无法证明，禁止 backfill。
- migration success 只证明 schema applied，不等于 snapshot created、identity valid、canonical hash valid 或 replay ready。
- write/read/serialization/constraint/transaction failure全部 fail-closed，不保存 partial row，不回退 latest/default。

## 10. Transaction boundary

未来 snapshot creation 必须在单一 DH PostgreSQL local transaction 中执行：

```text
isolation: REPEATABLE_READ
1. tenant-bound read V5/V6/V8/V9 exact identities
2. validate correlation, required fields, version vector and unsafe material
3. build allowlisted immutable snapshot
4. enforce pre-serialization and canonical-byte size gates
5. generate QDR6-CJSON-1 bytes and SHA-256 hash
6. insert qdr_canonical_replay_snapshot
7. commit
```

任一步失败回滚 insert；不写 V5-V9，不调用外部系统。duplicate identity 同 hash可幂等读取，different hash 抛 conflict 并回滚。transaction boundary 必须使用 production transaction manager；没有 transaction manager 时 production wiring fail-fast，不能回退 direct mode。

## 11. Implementation batches

### P1: additive migration + persistence records

- 编制并单独 review `V10__qdr6_canonical_replay_snapshot.sql`。
- 新增 immutable persistence command/record/safety contracts。
- 不写 assembler、canonicalizer、replay executor。

### P2: tenant-bound ports/JDBC adapters + identity validation

- 新 snapshot port/JDBC adapter。
- prompt-by-ID 与 exact gateway identity 最小扩展。
- V5/V6/V8/V9 identity resolver，仍不生成 replay result。

### P3: canonical snapshot assembler + persistence integration tests

- 仅在 P1/P2 accepted 后实现 allowlisted assembler、`QDR6-CJSON-1` materialization/hash 和 transaction boundary。
- 不实现 deterministic replay executor，不新增 API/provider/NQ/Agent/LIVE。

每批独立 commit；P1 migration、P2 production port/JDBC、P3 canonicalization/security boundary 均需要对应 review evidence，不得合并成 all-in-one batch。

## 12. Required test matrix

1. V10 migration presence、顺序、V1-V9 checksum compatibility。
2. PostgreSQL/Testcontainers 从 V1 迁移至 V10。
3. tenant-bound insert/read happy path。
4. cross-tenant read empty/denied。
5. direct update rejection；runtime delete unavailable。
6. duplicate identity + same hash idempotent。
7. duplicate identity + different hash conflict。
8. 每个 required version 缺失、blank、latest/current/default 拒绝。
9. prompt/model/gateway checksum/ref mismatch 拒绝。
10. unknown/raw/credential/trading key/value 递归拒绝。
11. pre-serialization oversize 与 canonical bytes oversize 拒绝且无 truncation。
12. V5/V6/V8/V9 tenant/trace/request/decision/run mismatch 拒绝。
13. V6 request UUID/run UUID mapping mismatch 拒绝。
14. V8 run/modelCall/prompt/model tuple mismatch 拒绝。
15. V9 replay row/business ID/policy/hash mismatch 拒绝。
16. legacy row 无 V10 snapshot 时返回 `LEGACY_NOT_REPLAYABLE`。
17. transaction 中任一 read/validation/hash/insert failure 无 partial snapshot。
18. database constraint/duplicate exception映射为 structured fail-closed result。
19. DB `created_at` 不进入 canonical hash。
20. no HTTP/Provider SDK/NQ/order/execution/Agent/LangGraph dependency architecture guard。
21. Maven quality、focused tests、module tests、full tests。

Docker/Testcontainers 未真实运行不得写 PASS；migration applied 不得写 replay readiness PASS。

## 13. Rollback strategy

- Flyway migration 已发布后不修改/删除 V10，不使用 history rewrite。
- implementation 未发布：撤销 P1/P2/P3 各自 commit。
- V10 已应用但尚无 production rows：先回滚 app wiring，再通过独立 forward cleanup migration 移除新 table/constraints；不编辑 V10。
- 已有 snapshot rows：优先停用新写路径并保留审计数据，使用 forward-fix migration；不得 drop table 丢失 evidence。
- V5-V9 不变，因此 rollback 不需要恢复历史表内容。

## 14. Review triggers and readiness

以下任一变化必须重新 review：table/column/FK/trigger/size/retention 变化、production port/JDBC 扩大、transaction isolation 降低、allowlist/unsafe policy 变化、canonical/hash version 变化、legacy backfill、API/wiring/provider/NQ dependency。

Schema review 结论为 `CONDITIONAL PASS / DESIGN_ONLY`：表名、字段、tenant keys、FK、CHECK、索引、COMMENT、legacy/rollback 路线已冻结；实际 V10 implementation 前仍必须用 row-count/duplicate preflight 证明 composite unique constraints 的兼容性和锁风险，并用 PostgreSQL/Testcontainers 证明从 V1-V9 到 V10 的 replayability。该条件不阻止进入 work order，但阻止直接实施 migration。

```text
SNAPSHOT_PERSISTENCE_GAP_REVIEW: DONE
PERSISTENCE_DESIGN_FROZEN: YES
ADDITIVE_MIGRATION_REQUIRED: YES
PRODUCTION_PORT_EXPANSION_REQUIRED: YES
JDBC_EXPANSION_REQUIRED: YES

ALLOW_SNAPSHOT_PERSISTENCE_GAP_WORK_ORDER: YES
ALLOW_MIGRATION_IMPLEMENTATION_NOW: NO
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

## 15. Next concrete action

```text
DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-WORK-ORDER
```
