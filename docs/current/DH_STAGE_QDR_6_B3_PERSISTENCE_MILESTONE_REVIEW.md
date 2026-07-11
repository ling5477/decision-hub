# DH Stage-QDR-6 B3 Persistence Milestone Review

## 1. Review identity

```text
task: DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW
classification: REVIEW_ONLY + MIGRATION_REVIEW + PORT_JDBC_REVIEW + TENANT_ISOLATION_REVIEW + PERSISTENCE_MILESTONE_GATE
branch: dev
HEAD: 35eb32cdf6e36a7e47dcd90677ffa0d512527690
P1 commit: a3bf8bb3a3b536e87f27c34ce6f8a94c376d08e6
P2 commit: 35eb32cdf6e36a7e47dcd90677ffa0d512527690
review date: 2026-07-11
```

本轮只复核 P1/P2 的 V10、production port/JDBC、tenant isolation、identity mapping、duplicate/immutable 与 transaction evidence；未修改生产代码、测试、migration、API、assembler、runtime wiring 或 replay 实现。

## 2. Current fact verification

```text
repository: decision-hub
branch: dev
HEAD: 35eb32cdf6e36a7e47dcd90677ffa0d512527690
latest commit: 35eb32c feat(qdr): add tenant-bound replay snapshot persistence
start worktree: CLEAN
start staged: EMPTY
Stage-QDR-6 B3-P1: DONE / COMMITTED
Stage-QDR-6 B3-P2: DONE / COMMITTED
Stage-QDR-6 B3-P3: NOT_STARTED
V10 migration: IMPLEMENTED / TESTED
PostgreSQL/Testcontainers: PASS / PostgreSQL 17.10 / 0 skipped
```

P1/P2 commit 与用户给定事实一致。`git diff a3bf8bb^ 35eb32c -- V1-V9` 为空；P1/P2 期间 migration 目录只有 V10 新增，V1-V9 无历史漂移。

## 3. Evidence-backed findings

### 3.1 Migration and schema

通过项：

- V10 为 additive Flyway migration；不修改 V1-V9，不 backfill legacy row，不使用 `IF NOT EXISTS` 或 `CREATE UNIQUE INDEX CONCURRENTLY`。
- V5/V6/V8/V9 source rows 通过 tenant-aware composite unique/FK 与 V6 run/request、V8 call/run/prompt/model tuple 约束绑定。
- required version vector 列均为 `NOT NULL`，固定版本、moving alias、hash、JSON shape、optional lineage 与 payload limits 均有数据库 `CHECK`。
- `context_payload_json <= 131072`、`evidence_refs_json <= 65536`、`expected_decision_summary_json <= 32768`、总量 `<= 262144` 真实由 PostgreSQL 拒绝测试覆盖。
- UPDATE trigger 以 SQLSTATE `55000` fail-closed；snapshot production port 不提供 delete/update。
- table、全部 columns、function 与 trigger 有中文 `COMMENT`；无 raw prompt、raw provider response、credential 或交易指令专用列。
- Flyway failure 测试证明 V10 前置 `ALTER TABLE` 与后续 DDL 可整体回滚。

阻断项：

1. `canonical_input_hash` 在 V10、`CanonicalReplaySnapshotRecord` 与 JDBC insert 中均为调用方必填，但当前 V5/V6/V8/V9 ports 没有 `QDR6-CJSON-1` canonical input hash 来源。P3 当前又明确禁止 canonicalizer 与 deterministic hash。若继续 P3，只能伪造/复用含义不同的 source hash，或扩大 P3 scope，均违反冻结 work order 的 stop condition。
2. `created_at` 虽有数据库 `default now()`，但 persistence record 将其设为必填，JDBC `INSERT` 也显式写入调用方 `createdAt`。这与 P3 冻结的“`created_at` 只由数据库生成”不一致，并把 audit time 责任错误放到 assembler 调用方。
3. 冻结 work order 要求 table/column/index/constraint/trigger `COMMENT`；V10 未为新增 indexes/constraints 写 `COMMENT`。这不影响运行测试，但属于冻结 schema 文档契约偏离。

因此：

```text
B3_PERSISTENCE_SCHEMA_MISMATCH_BLOCKED
SCHEMA_REVIEW: FAIL
```

### 3.2 Port and JDBC boundary

- snapshot port 仅有 `insert`、`findByTenantAndSnapshotId`、`findByTenantAndIdentity`；`tenantId` 均为显式第一参数。
- snapshot port 无 update/delete/upsert overwrite/listAll/latest/findFirst/scan/fallback/tenantless lookup。
- prompt exact lookup 为 `tenantId + promptVersionId`；gateway exact lookup 的 SQL 为 `tenantId + decisionRunId + modelCallRef`。
- 所有新增 SQL 参数化；snapshot SELECT 和 source-validation SELECT 均包含 tenant predicate 或由 V6 已 tenant-bound request join 约束。
- query result 会再次校验 tenant、snapshot/composite identity、prompt ID 或 gateway run/ref；数据库异常映射为结构化 persistence exception，不降级为 empty/success。
- 未发现 trace/provider/time 推断、latest/fallback、模糊匹配或 snapshot 全表扫描。

Port surface 与 tenant isolation 本身通过；但 P2 所称“完整 V9 identity/hash mapping”不成立：

- `validateSourceIdentity` 对 `qdr_replay_case` 只核对 row/business ID、correlation、policy、gateway version 与 `expected_summary_hash`。
- 它没有把 snapshot `ReplayInputRef(refType/refId/contentHash)` / `replay_input_hash` 与 V9 `input_ref_id -> qdr_replay_input_ref` 做 exact comparison。
- 它也没有把 snapshot `ExpectedDecisionSummary` structured projection 与 V9 `expected_summary_id -> qdr_expected_decision_summary` 做 exact comparison。
- `CanonicalReplaySnapshotRecord` 本身也未要求 `replayInputHash == replayInputRef.contentHash`。

因此当前 JDBC 可接受“tenant/correlation 正确但 V9 structured input/summary projection 不属于该 replay case”的安全结构化 record。该问题不会造成 cross-tenant 可见性，但会造成 canonical snapshot source identity drift，且现有 9 个 PostgreSQL tests 未覆盖该场景。

```text
B3_PERSISTENCE_PORT_BOUNDARY_BLOCKED
PORT_JDBC_REVIEW: FAIL
TENANT_ISOLATION_REVIEW: PASS
IDENTITY_MAPPING_REVIEW: FAIL
```

### 3.3 Duplicate, immutable and transaction behavior

- duplicate-identical 使用 `insertArguments` 比较实际持久化 projection；完全一致时返回既有 row。
- duplicate-conflict 对 snapshot unique identity 或 aggregate identity 冲突做 exact read + persisted projection comparison；不一致抛 `CanonicalReplaySnapshotConflictException`。
- UPDATE trigger 真实拒绝；production port/JDBC 无 delete method。
- surrounding Spring transaction rollback test 真实执行 snapshot insert，标记 rollback 后 exact read 为 empty。
- repository 不自行伪造 transaction success；JDBC failure 结构化 fail-closed。
- 当前未提前新增 assembler/runtime wiring。P3 仍必须使用 production PostgreSQL transaction manager，并显式设置 `REPEATABLE_READ`；缺失 transaction manager 必须 fail-fast，不得复用既有 direct fallback precedent。

```text
TRANSACTION_BOUNDARY_REVIEW: PASS
```

该 PASS 只确认 P2 insert 可被真实 Spring transaction 回滚；不表示 P3 的 `REPEATABLE_READ` 已实现。

## 4. P3 readiness

现有 exact ports 能读取 V5/V6/V8/V9 safe structured sources，tenant isolation 与 transaction participation 也足以支撑后续 assembler。但当前不能在“不新增 migration、port、JDBC、API、canonicalizer 或 hash”的边界内完成 P3：

- 必填 `canonical_input_hash` 无合法现有 source。
- DB-generated `created_at` 被当前 record/JDBC 改为 caller-supplied。
- V9 input ref/hash 与 expected summary structured projection 缺少 persistence-boundary exact validation。

因此 milestone 不准入 P3；green tests 不能覆盖上述未测试的 contract mismatch。

## 5. Readiness decision

```text
B3_PERSISTENCE_MILESTONE_REVIEW: BLOCKED

SCHEMA_REVIEW: FAIL
PORT_JDBC_REVIEW: FAIL
TENANT_ISOLATION_REVIEW: PASS
IDENTITY_MAPPING_REVIEW: FAIL
TRANSACTION_BOUNDARY_REVIEW: PASS
POSTGRESQL_TEST_EVIDENCE: PASS

B3_PERSISTENCE_SCHEMA_MISMATCH_BLOCKED
B3_PERSISTENCE_PORT_BOUNDARY_BLOCKED

ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION: NO
ALLOW_CANONICALIZER_IMPLEMENTATION_NOW: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
ALLOW_ADDITIONAL_SCHEMA_CHANGE_NOW: NO
ALLOW_PORT_JDBC_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

## 6. Validation

| Command / check | Result | Evidence |
| --- | --- | --- |
| `git status --short` before docs | PASS | clean；staged empty。 |
| `git diff --check` before docs | PASS | no output。 |
| `git show --stat --oneline a3bf8bb` | PASS | P1 11 files，1561 insertions，7 deletions。 |
| `git show --stat --oneline 35eb32c` | PASS | P2 16 files，1459 insertions，75 deletions。 |
| V1-V9 diff | PASS | `a3bf8bb^..35eb32c` 无 V1-V9 migration diff；仅新增 V10。 |
| `mvn -ntp -pl dh-usecase,dh-infra -am test` | BUILD SUCCESS | `dh-usecase` 与 `dh-infra` success；`dh-infra` 88 tests，0 skipped；PostgreSQL Testcontainers 实际启动。 |
| `mvn -ntp -pl dh-app -am test` | BUILD SUCCESS | `dh-app` 96 tests，0 skipped；V10 PostgreSQL 9/9。 |
| `mvn -ntp test` | BUILD SUCCESS | reactor 19/19 success；V10 PostgreSQL 9/9，0 skipped。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | reactor 19/19；root Checkstyle 0 violations；Spotless check success。 |
| cross-tenant | PASS | snapshot/prompt/gateway exact cross-tenant reads empty。 |
| immutable trigger | PASS | SQLSTATE `55000`。 |
| transaction rollback | PASS | insert 后 transaction rollback，row 不可见。 |
| architecture guards | PASS / INCOMPLETE COVERAGE | method-surface guards 通过；未覆盖 V9 projection mismatch 与 canonical-hash sequencing。 |
| skipped tests | PASS | V10 PostgreSQL/Testcontainers 0 skipped。 |

首次 targeted Maven 调用因执行工具 timeout 设置过短，在约 5 秒时被外部终止，未形成测试结论；使用 120 秒 timeout 原命令重跑后 `BUILD SUCCESS`。该事件为 review harness timeout，不是代码或测试失败。

## 7. Risks

- external environment data compatibility：P1 preflight 仅证明当时 empty baseline；外部已存在 V1-V9 数据仍需部署前 duplicate/orphan/lock window 复核。
- schema lock：V10 composite unique constraints 会扫描/锁定 source tables；本轮未对生产规模做实时测量。
- tenant isolation：当前三层 isolation 通过；后续 P3 必须继续只使用 exact tenant-bound methods。
- identity mapping drift：V9 structured input/summary exact comparison 缺失，是当前 blocker。
- duplicate comparison scope：persisted projection comparison 安全，但不能替代 source projection validation。
- transaction manager dependency：P3 缺少 production manager 必须 fail-fast。
- `REPEATABLE_READ` enforcement：尚未实现，P3 必须显式设置并补 integration evidence。
- payload growth：DB 上限真实有效；P3 仍需 pre-serialization 与 exact byte accounting。
- legacy data：无 auto-backfill；legacy row 保持 `LEGACY_NOT_REPLAYABLE`。
- Testcontainers reliability：本轮 Docker/PostgreSQL 17.10 稳定运行，但仍依赖本机 Docker。
- migration rollback：测试通过；已提交 V10 不得原地编辑，blocker 修复必须另起授权并采用 forward-only 策略。
- Maven wrapper risk：仓库未使用 `mvnw`，本轮依赖本机 Maven。

## 8. Boundary confirmation

- 未修改 NQ、Java 生产代码、Java 测试代码或 V1-V10。
- 未新增 migration、API/Controller、Repository/JDBC、production port 或 runtime wiring。
- 未实现 snapshot assembler、canonicalizer、hash 或 deterministic replay。
- 未调用 HTTP、Provider、NQ、Agent 或 LangGraph。
- 未读取或保存凭证、raw prompt 或 raw provider response。
- 未产生交易、执行、Provider、NQ 或 LIVE 授权。
- 未创建 tag，未 commit，未 push。

## 9. Next concrete action

```text
DH-STAGE-QDR-6-B3-PERSISTENCE-SCHEMA-BLOCKER-FIX
```

该后续任务必须先重新授权 forward-only schema/contract/JDBC 修复范围，并同时收口 canonical hash sequencing、DB-generated `created_at` 与 V9 exact projection validation；不得在本 review 中顺手修复。

## 10. Blocker fix follow-up resolution

后续任务 `DH-STAGE-QDR-6-B3-PERSISTENCE-SCHEMA-BLOCKER-FIX` 已在独立授权范围内完成：write/persisted contract 已拆分，JDBC 恢复 DB-generated `created_at`，V9 structured projection exact validation 已补齐，V11 仅增加 constraints/indexes comments。该结果不回写或掩盖本 review 在当时基线上的 `BLOCKED` 历史结论；只将当前下一步推进为独立 review retry。

```text
HASH_SEQUENCING_FIX: PASS
CREATED_AT_BOUNDARY_FIX: PASS
V9_PROJECTION_VALIDATION: PASS
V11_METADATA_FIX: PASS
POSTGRESQL_TEST_EVIDENCE: PASS / POSTGRESQL_17_10 / 0_SKIPPED
ALLOW_B3_PERSISTENCE_MILESTONE_REVIEW_RETRY: YES / NEXT_TASK_ONLY
ALLOW_B3_P3_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW-RETRY
```
