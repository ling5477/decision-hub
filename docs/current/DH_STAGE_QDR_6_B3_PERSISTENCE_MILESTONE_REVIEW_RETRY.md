# DH Stage-QDR-6 B3 Persistence Milestone Review Retry

## 1. Review identity

```text
task: DH-STAGE-QDR-6-B3-PERSISTENCE-MILESTONE-REVIEW-RETRY
classification: REVIEW_ONLY + MIGRATION_REVIEW + JDBC_BOUNDARY_REVIEW + V9_PROJECTION_REVIEW + HASH_SEQUENCING_REVIEW + P3_READINESS_GATE
branch: dev
HEAD: 990c1bb3376cb0edf13a660ffa295b56292da3e9
P1 commit: a3bf8bb3a3b536e87f27c34ce6f8a94c376d08e6
P2 commit: 35eb32cdf6e36a7e47dcd90677ffa0d512527690
blocker-fix commit: 990c1bb3376cb0edf13a660ffa295b56292da3e9
review date: 2026-07-11
```

本轮仅复核首次 milestone review 的四个原始 blocker，并判断重新定义后的 P3 是否可实施。未修改生产代码、测试、migration、API、Repository/JDBC、port 或 runtime wiring；未执行 replay、Provider、Agent、NQ 或 LIVE 能力。

## 2. Preflight and current facts

```text
repository: decision-hub
branch: dev
start worktree: CLEAN
start staged: EMPTY
untracked: EMPTY
V1-V10 blocker-fix diff: EMPTY
Stage-QDR-6 B3-P1: DONE / COMMITTED
Stage-QDR-6 B3-P2: DONE / COMMITTED
first persistence milestone review: BLOCKED
blocker fix: DONE / COMMITTED / POSTGRESQL_VERIFIED
Stage-QDR-6 B3-P3: NOT_STARTED
V11 migration: IMPLEMENTED / METADATA_ONLY
```

## 3. Original blocker closure

### 3.1 Hash sequencing

`CanonicalReplaySnapshotWriteCommand` 与 `CanonicalReplaySnapshotRecord` 已分离。前者不含 `createdAt`，只接受完整 structured material，并要求 `canonicalInputHash` 为 lowercase SHA-256 hex；`null`、blank、`placeholder/default/latest/current/pending/tbd` 与全零 hash 均 fail-closed。`replayInputHash` 还必须等于 structured `ReplayInputRef.contentHash`。

该合同不读取 JDBC、不使用数据库主键、随机值或当前时间，也不生成 hash。`CanonicalReplaySnapshotVersionVector` 固定保存 `QDR6-CJSON-1`、`QDR6-MOCK-REPLAY-1` 与 `SHA-256` compatibility labels。`created_at`、数据库主键与 audit-only metadata 不属于 canonical hash 输入。P3 的唯一合法顺序固定为：

```text
structured snapshot assembler
-> QDR6-CJSON-1 canonicalizer
-> SHA-256 deterministic hash
-> construct CanonicalReplaySnapshotWriteCommand
-> REPEATABLE_READ identity validation
-> immutable persistence
```

结论：`HASH_SEQUENCING_REVIEW: PASS`。该 PASS 不表示 canonicalizer/hash 已实现，只表示 P3 可在 insert 前按冻结合同实现，且 persistence boundary 不再允许伪造或延后 hash。

### 3.2 Database-generated created_at

JDBC `INSERT_COLUMNS` 与 `INSERT` 均不含 `created_at`，V10 的 `created_at timestamptz not null default now()` 因而真实生效。insert 后 repository 通过 tenant-bound composite identity 回读数据库行，并把数据库返回值组合为 persisted record。duplicate-identical 比较只比较 `insertArguments(existing.toWriteCommand())` 与请求 write command，不比较数据库审计时间。

PostgreSQL 测试确认 write command/INSERT 均无 `createdAt`，persisted `createdAt` 与数据库查询值一致，且 duplicate-identical 仍幂等。

结论：`CREATED_AT_BOUNDARY_REVIEW: PASS`。

### 3.3 V9 exact structured projection

`requireV9Projection` 以 tenant、replay row UUID、case business ID、decision/request/trace correlation、policy version 与 gateway version 精确定位唯一 source projection，并 exact compare：

- `ReplayInputRef` 的 `refType`、`refId`、structured JSON 三字段、`content_hash` 与 snapshot `replayInputHash`；
- expected summary role、structured columns、完整 `summary_json`、`requiredEvidenceRefs`、`forbiddenActions`、`summary_hash` 与 replay case `expected_summary_hash`；
- 可选 evaluation/verdict lineage 的 physical/business pair、input/summary physical IDs、hash 与 correlation。

查询不使用 row-ID-only/checksum-only 放行，不使用 latest、时间顺序、provider 名称或字符串相似推断。source 缺失、跨 tenant、冲突或多行均 fail-closed；optional lineage 缺失时保持 `NULL`，不会补造。PostgreSQL 测试真实覆盖 input/hash drift、summary structured/JSON drift、cross-tenant 不 fallback；其余字段由同一 exact predicate/compare 分支静态复核。

结论：`V9_PROJECTION_REVIEW: PASS`、`TENANT_ISOLATION_REVIEW: PASS`。

### 3.4 V11 metadata-only forward migration

V11 仅包含 `COMMENT ON CONSTRAINT` 与 `COMMENT ON INDEX`，没有 table/column/constraint/index/trigger/function/data mutation。`git diff HEAD^ HEAD` 证明 V1-V10 无修改。真实 PostgreSQL 17.10 证据确认 clean V1→V11、V1→V10 数据再升级 V11 均通过，V10 snapshot row 保留，21 个 snapshot constraint comments 与 4 个 index comments 可查询。V10 failure injection 继续证明事务型 migration failure 不保留前置 DDL；V11 本身只执行 PostgreSQL transactional metadata statements。

结论：`V11_MIGRATION_REVIEW: PASS`、`V1_V11_MIGRATION_INTEGRITY: PASS`。

## 4. P3 readiness decision

现有 tenant-bound read/persistence contracts 已能提供 V5/V6/V8/V9 structured source、physical/business identity、safe refs、version/checksum/hash metadata与 V9 expected projection；snapshot persistence port 已支持 immutable insert、exact read、duplicate-identical/conflict。P3 不需要新增或修改 migration、port、JDBC 或 API。

生产 wiring 已存在 PostgreSQL `PlatformTransactionManager` / `TransactionTemplate` precedent。P3 必须在独立实现中显式设置 `REPEATABLE_READ`，transaction manager 缺失时 fail-fast，并把 source reads、validation、canonicalization/hash、insert/exact compare 放进同一 transaction；当前尚未把该实现或其 integration evidence写成 DONE。

```text
B3_PERSISTENCE_MILESTONE_REVIEW_RETRY: PASS
ALLOW_B3_P3_ASSEMBLER_IMPLEMENTATION: YES
ALLOW_CANONICALIZER_IMPLEMENTATION: YES
ALLOW_DETERMINISTIC_HASH_IMPLEMENTATION: YES
ALLOW_IMMUTABLE_SNAPSHOT_PERSISTENCE_IN_P3: YES
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION_NOW: NO
ALLOW_ADDITIONAL_SCHEMA_CHANGE_NOW: NO
ALLOW_PORT_JDBC_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
```

P3 只允许实现 canonical snapshot assembly/hash/persistence 链路。legacy V5-V9 数据无 canonical snapshot 时继续返回 `LEGACY_NOT_REPLAYABLE`，不 backfill；不得调用 HTTP、Provider、NQ、Agent、LangGraph 或交易能力。

## 5. Validation evidence

| Command / check | Result | Evidence |
| --- | --- | --- |
| `git status --short` | PASS | 开工时 clean；staged/untracked 为空。 |
| `git diff --check` | PASS | 开工时无输出。 |
| `git show --stat --oneline a3bf8bb` | PASS | P1 commit 可读，11 files。 |
| `git show --stat --oneline 35eb32c` | PASS | P2 commit 可读，16 files。 |
| `git show --stat --oneline HEAD` | PASS | blocker fix commit 可读，15 files。 |
| V1-V10 blocker-fix diff | PASS | `HEAD^..HEAD` 无 V1-V10 migration diff。 |
| `mvn -ntp -pl dh-usecase,dh-infra -am test` | BUILD SUCCESS | `dh-infra` 88 tests、0 skipped；Testcontainers 实际启动。 |
| `mvn -ntp -pl dh-app -am test` | BUILD SUCCESS | `dh-app` 102 tests、0 skipped；snapshot PostgreSQL 13/13。 |
| `mvn -ntp test` | BUILD SUCCESS | reactor 19/19；Surefire 合计 965 tests、0 failures/errors/skipped。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | root Checkstyle 0 violations；Spotless check 通过。 |
| PostgreSQL/Testcontainers | PASS | `postgres:17`，实际 PostgreSQL 17.10，0 skipped。 |

## 6. Boundary confirmation

- 未修改 NQ、Java 生产代码、Java 测试代码或 migration。
- 未修改 V1-V11，未新增 API/Controller、Repository/JDBC、production port 或 runtime wiring。
- 未实现 snapshot assembler、canonicalizer、hash 或 deterministic replay。
- 未调用 HTTP、Provider、NQ、Agent 或 LangGraph。
- 未读取或保存凭证、raw prompt 或 raw provider response。
- 未产生交易、执行、Provider、NQ 或 LIVE 授权。
- 未创建 tag，未 commit，未 push。

## 7. Risks

- 外部环境数据兼容性：本轮只验证 disposable PostgreSQL baseline；部署前仍需对目标 V1-V10 数据做 duplicate/orphan/lock-window preflight。
- hash sequencing drift：P3 必须先 canonicalize/hash 再构造 write command，禁止 source hash 复用、placeholder 或 insert-before-hash。
- 数据库时间依赖：`created_at` 只作 audit；不得进入 identity、duplicate comparison 或 canonical bytes。
- V9 projection drift：后续任何字段扩展必须同步 exact structured projection 与回归，不得降级为 checksum-only。
- optional lineage：缺失必须保持缺失；存在时必须成对并 exact tenant-bound。
- transaction manager：P3 必须 fail-fast 获取 production PostgreSQL manager，显式 `REPEATABLE_READ`；当前只是可实施性结论。
- canonical byte accounting：UTF-8/NFC/排序/number/timestamp/optional omission 与 byte limit 必须按冻结合同补齐 deterministic tests。
- legacy data：继续 `LEGACY_NOT_REPLAYABLE`，不 backfill。
- Testcontainers：依赖本机 Docker；本轮稳定通过不等同外部环境持续可用。
- Maven wrapper：仓库无 `mvnw`，本轮使用本机 Maven。

## 8. Next concrete action

```text
DH-STAGE-QDR-6-B3-P3-CANONICAL-SNAPSHOT-ASSEMBLY-HASH-PERSISTENCE
```
