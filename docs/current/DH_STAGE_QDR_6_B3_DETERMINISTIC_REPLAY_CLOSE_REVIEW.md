# DH Stage-QDR-6 B3 Deterministic Replay Close Review

## 1. Review identity

```text
task: DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-CLOSE-REVIEW
classification: REVIEW_ONLY + DETERMINISTIC_REPLAY_CLOSE_GATE + SECURITY_BOUNDARY_REVIEW + REPRODUCIBILITY_EVIDENCE_REVIEW
repository: decision-hub
branch: dev
reviewed HEAD: ae4c94489817bec9223886022b43679d2ae36cbf
baseline commit: ae4c944 feat(qdr): add deterministic replay baseline
worktree at review start: CLEAN
staged at review start: EMPTY
review verdict: PASS
```

本轮只读验收 B3-P1/P2/P3、deterministic replay gate 与 baseline implementation。除本 review 及 current facts 同步外，未修改 Java、测试、migration、API、port/JDBC、Repository 或 runtime wiring；发现问题时也不在本轮顺手修复。

## 2. Current fact verification

```text
B3-P1: DONE / COMMITTED / a3bf8bb
B3-P2: DONE / COMMITTED / 35eb32c
B3-PERSISTENCE-BLOCKER-FIX: DONE / COMMITTED / 990c1bb
B3-P3: DONE / COMMITTED / e54e607
DETERMINISTIC_REPLAY_BASELINE_GATE: PASS / COMMITTED / 3b0d529
DETERMINISTIC_REPLAY_BASELINE: DONE / COMMITTED / ae4c944
HEAD == origin/dev: YES
mismatches: NONE
```

## 3. Snapshot input and persistence review

- Replay execution 只调用 `CanonicalReplaySnapshotPersistencePort.findByTenantAndIdentity`，selector 同时包含 `tenantId`、完整 `CanonicalReplaySnapshotIdentity` 与固定 snapshot schema version。
- `CanonicalReplaySnapshotRecord` 保存重建 `ReplayInputSnapshot` 所需的 structured safe fields、完整 version vector、source hashes 与 persisted canonical input hash；`ReplayInputSnapshot.fromPersistedRecord` 不读取 V5/V6/V8/V9 source。
- executor 从 persisted record 重建 snapshot 后调用 `CanonicalReplaySnapshotHasher` 重新生成 `QDR6-CJSON-1` canonical bytes 与 input hash，并同时 exact compare command expected hash、persisted hash 与 recomputed hash。
- replay 执行阶段不重新查询 V5/V6/V8/V9 mutable source；P3 assembly 时的 source query、二次 revalidation、hash、insert 与 exact read-back 已在 PostgreSQL `REPEATABLE_READ` transaction 内完成。
- snapshot structured payload 不包含 raw prompt、raw provider response 或 credential；V10 采用 allowlist JSON shape、unsafe key guard、payload limits 与 immutable UPDATE trigger。
- `createdAt`、snapshot DB physical `id`、clock、random、environment 与 machine path 不进入 canonical input hash。
- 本轮 worktree 与 staged 对 V1-V11 均无 diff。

结论：`SNAPSHOT_INPUT_BOUNDARY: PASS`，`PERSISTED_SNAPSHOT_INTEGRITY: PASS`，`INPUT_HASH_VERIFICATION: PASS`。

## 4. Executor boundary review

- `DeterministicReplayExecutor` 只接受 `QDR6-MOCK-REPLAY-1`，unsupported snapshot/canonicalization/executor/hash version 均 fail-closed。
- production constructor 的 transformer 固定为 `DeterministicReplayProjection::fromSnapshot`；不存在 clock、random、environment、HTTP、Provider、NQ、Agent 或 LangGraph dependency。
- executor 不执行真实 prompt、model、provider 或 policy runtime；它证明 frozen persisted record 的本地 storage/reproducibility projection 可重复，不证明真实 LLM/provider output 可重复。
- executor 只读 snapshot port；没有 insert/update/delete，未持久化 replay result，也不修改 persisted snapshot。
- deterministic replay package 未被 `dh-api`、`dh-app`、`dh-infra` 或 `dh-connector` production code wiring；未新增 API、Controller 或外部 runtime entry。
- persistence read、reconstruction、hash、baseline projection、mock transformation 与 output canonicalization 的 runtime failure 均映射为 structured non-success result；command constructor 对不合法 selector/version 在执行前 fail-fast。

结论：`EXECUTOR_BOUNDARY_REVIEW: PASS`，`NO_EXTERNAL_IO_REVIEW: PASS`，`MOCK_ONLY_GUARANTEE: PASS`。

## 5. Canonicalization and hash review

- input/output 均使用 `QDR6-CJSON-1`：UTF-8、NFC、normalized key ordering、Set canonical-byte ordering、List/array semantic ordering、explicit null 与 absent 区分、decimal normalization、non-finite rejection、UTC instant normalization。
- input domain 为 `DH-QDR6-CANONICAL-REPLAY-SNAPSHOT`；output domain 为 `DH-QDR6-DETERMINISTIC-REPLAY-OUTPUT`，并包含各自 frozen schema/version labels。
- SHA-256 由 `HexFormat.of().formatHex` 输出 lowercase 64 位 hex；record/result constructors 再执行 lowercase SHA-256 validation。
- caller/source hash 无法替代 canonical input hash；assembler 先 canonicalize/hash，再创建 write command；executor 再从 persisted record 重算。
- output hash 只覆盖 structured replay projection，不包含 `createdAt`、DB ID 或环境字段；executor 额外拒绝 input hash 与 output hash 相等的异常情形。
- unsupported version 返回 `UNSUPPORTED_VERSION`，不 fallback 到 `latest/current/default`。

结论：`CANONICALIZATION_REVIEW: PASS`，`INPUT_OUTPUT_HASH_REVIEW: PASS`。

## 6. Comparator, status and failure review

冻结 difference taxonomy 恰为 9 类：

```text
CONTEXT_DIFFERENCE
POLICY_VERSION_DIFFERENCE
PROMPT_VERSION_DIFFERENCE
MODEL_VERSION_DIFFERENCE
GATEWAY_VERSION_DIFFERENCE
EXPECTED_SUMMARY_DIFFERENCE
EVIDENCE_DIFFERENCE
OUTPUT_HASH_DIFFERENCE
MISSING_REQUIRED_INPUT
```

冻结 status 恰为 6 类：

```text
REPRODUCIBLE
DIFFERENT
INCOMPLETE
UNSUPPORTED_VERSION
INVALID_INPUT
EXECUTION_FAILED
```

- differences 按 `type`、`path` 稳定排序；context/summary/evidence 仅输出 SHA-256 fingerprint，version/ref/hash 字段只输出 frozen safe ref 或 hash，不保存 structured raw payload。
- enum exact-set regression 阻止 taxonomy 静默扩展。
- `REPRODUCIBLE` 要求 differences 为空、failure code 为空、baseline/replay output hash 均存在且 exact equal；`DIFFERENT` 必须至少一个 difference 且无 failure code。
- 13 个 `ReplayFailureCode` 均有唯一、测试锁定的 status mapping；failure result 不携带 differences。

结论：`COMPARATOR_REVIEW: PASS`，`FAIL_CLOSED_REVIEW: PASS`，`STABLE_ORDERING: PASS`。

## 7. Security and authorization semantics

每个 `DeterministicReplayResult` 强制携带：

```text
internal reproducibility evidence only
not provider authorization
not NQ integration permission
not trading or execution permission
not Paper or LIVE permission
```

deterministic replay package 不含 `BUY`、`SELL`、`MARKET_ORDER`、`PLACE_ORDER` 或 `CANCEL_ORDER` action；result 不含 raw canonical payload、raw prompt、raw provider response 或 credential。`REPRODUCIBLE` 不表示业务策略正确、真实 LLM/provider 可重现、NQ 已集成或允许进入 Paper/LIVE/交易链路。

结论：`AUTHORIZATION_GUARD_REVIEW: PASS`，`RAW_MATERIAL_PROTECTION: PASS`，`FALSE_REPRODUCIBILITY_PROTECTION: PASS`。

## 8. Validation evidence

| Command / evidence | Result | Evidence |
| --- | --- | --- |
| `git status --short` | PASS | review 开始时 clean；staged/untracked 为空。 |
| `git diff --check` | PASS | 无 whitespace error。 |
| `git show --stat --oneline ae4c944` | PASS | 17 files，1721 insertions，8 deletions；仅 deterministic replay usecase/tests 与 4 个 current docs。 |
| focused replay/snapshot tests | PASS | 42 tests，0 failures/errors/skipped。 |
| `mvn -ntp -pl dh-usecase -am test` | BUILD SUCCESS | `dh-usecase` 503 tests；deterministic executor 11、comparator 4、assembler 7、record 10、canonical JSON 10；全部 0 skipped。 |
| `mvn -ntp test` | BUILD SUCCESS | 19/19 reactor；Surefire XML 汇总 1001 tests，0 failures/errors/skipped。 |
| PostgreSQL/Testcontainers | PASS | `postgres:17` / PostgreSQL 17.10 实际启动；V1→V11 clean migration；V10 snapshot PostgreSQL 16/16、V9 PostgreSQL 1/1，0 skipped。 |
| `mvn -ntp -Pquality validate` | BUILD SUCCESS | 19/19；root Checkstyle 0 violations；Spotless check PASS。 |
| architecture/source guards | PASS | 无 HTTP/Provider/NQ/Agent/clock/random/environment/write dependency；无 deterministic replay API/wiring。 |
| V1-V11 review diff | PASS | worktree/staged migration diff 为空。 |

Quality 仍输出既有子模块 `checkstyle outputFile` non-blocking 提示；root aggregate Checkstyle 与 Spotless 实际通过。Full tests 仍输出 Mockito/Byte Buddy dynamic agent 的 future-JDK warning，不影响本轮结果。

## 9. Readiness decision

```text
B3_DETERMINISTIC_REPLAY_CLOSE_REVIEW: PASS
SNAPSHOT_INPUT_BOUNDARY: PASS
CANONICALIZATION_REVIEW: PASS
INPUT_OUTPUT_HASH_REVIEW: PASS
EXECUTOR_BOUNDARY_REVIEW: PASS
COMPARATOR_REVIEW: PASS
FAIL_CLOSED_REVIEW: PASS
TENANT_ISOLATION_REVIEW: PASS
NO_EXTERNAL_IO_REVIEW: PASS
AUTHORIZATION_GUARD_REVIEW: PASS
POSTGRESQL_REGRESSION_EVIDENCE: PASS

STAGE_QDR_6_B3: CLOSED / ACCEPTED
ALLOW_B4_INTERNAL_REPORT_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_ADDITIONAL_IMPLEMENTATION_NOW: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
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

## 10. Risks and next action

- legacy snapshots：V10 canonical snapshot 之前的 legacy row 不具备完整 immutable input，不得伪装为 replayable。
- version drift：任何 snapshot/canonicalization/executor/hash version 漂移必须继续 fail-closed，不得 fallback。
- false reproducibility：B4 必须保留 internal-only 文案，不得将 `REPRODUCIBLE` 转译为业务正确、provider authorization 或交易授权。
- Maven wrapper：本轮使用系统 `mvn`；未把 `mvnw.cmd` 状态写成可用。
- Mockito/Byte Buddy：未来 JDK 禁止动态 agent loading 前需单独治理，当前 non-blocking。

```text
next action: DH-STAGE-QDR-6-B4-EVIDENCE-REPLAY-INTERNAL-REPORT
```

B4 只允许 internal report implementation；不得新增 API、schema、port/JDBC、HTTP、Provider、NQ、Agent、LangGraph、Paper/LIVE 或交易能力。
