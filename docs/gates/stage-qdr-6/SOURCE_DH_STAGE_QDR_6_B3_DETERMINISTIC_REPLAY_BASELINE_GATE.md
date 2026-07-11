# DH Stage-QDR-6 B3 Deterministic Replay Baseline Gate

## 1. 任务与结论

```text
task: DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE-GATE
classification: REVIEW_ONLY + DETERMINISTIC_REPLAY_CONTRACT_GATE
gate verdict: PASS
REPLAY_INPUT_CONTRACT: FROZEN
EXECUTOR_BOUNDARY: FROZEN
REPRODUCIBILITY_POLICY: FROZEN
DIFFERENCE_TAXONOMY: FROZEN
FAIL_CLOSED_TAXONOMY: FROZEN
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_REPLAY_COMPARATOR_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_B3_CLOSE_REVIEW_NOW: NO
```

本 gate 只冻结纯本地、mock-only deterministic replay baseline。它验证 persisted canonical snapshot 的可重建性、canonical/hash/version compatibility 和结构化差异，不重放真实 prompt/provider/policy runtime，不产生业务决策或任何授权。

## 2. 当前代码事实

- `CanonicalReplaySnapshotPersistencePort` 已提供 tenant-bound `findByTenantAndSnapshotId` 与 full-identity exact read；不需要新增 port。
- `CanonicalReplaySnapshotRecord` 包含重建 `ReplayInputSnapshot` 所需的 identity、subject、context、safe evidence refs、V9 input/expected summary、完整 version vector 与 hashes。
- V10 未单独持久化 canonical byte blob。实现必须从数据库回读 record 重建 `ReplayInputSnapshot`，再使用现有 `Qdr6CanonicalJson` 与 `CanonicalReplaySnapshotHasher` 重新生成 canonical bytes/hash；不得接受 caller-provided canonical bytes。
- `canonicalInputHash` 已持久化且由 P3 在 insert 前生成，可与重算 hash exact compare。
- `CanonicalReplaySnapshotVersionVector` 固定 `QDR6-REPLAY-INPUT-1`、`QDR6-CONTEXT-1`、`QDR6-CJSON-1`、`QDR6-MOCK-REPLAY-1`、`SHA-256`，并拒绝 moving aliases。
- 现有 `QdrRegressionComparator` 是 V9 regression comparator，不是 deterministic replay executor/comparator；不得复用其 PASS/WARN/FAIL 语义冒充 replay reproducibility。
- 当前不存在 `DeterministicReplayCommand`、`DeterministicReplayResult`、`ReplayDifference`、`ReplayReproducibilityStatus` 或 `ReplayFailureCode`。

## 3. 唯一输入事实源

Executor 只能使用一次 tenant-bound exact read 得到的 persisted `CanonicalReplaySnapshotRecord`，并从中重建：

```text
ReplayInputSnapshot
canonical snapshot bytes（本地重新 canonicalize）
canonical input hash（数据库保存值与本地重算值）
complete CanonicalReplaySnapshotVersionVector
safe structured evidence refs
ExpectedDecisionSummary
replay/expected/provider summary hashes
frozen policy/evaluation/model/prompt/gateway version metadata
```

禁止输入：

```text
V5/V6/V8/V9 mutable source re-read
latest/current/default version resolution
current time / random / environment variable / machine path / locale-dependent value
raw prompt / raw provider response / credential
HTTP / Provider / NQ / trading / Agent / LangGraph source
caller-provided canonical bytes or caller-provided replay output
```

Legacy、missing、incomplete 或 hash-invalid record 不得进入 mock transformation。

## 4. Frozen contracts

### 4.1 DeterministicReplayCommand

```text
tenantId
CanonicalReplaySnapshotIdentity identity
snapshotSchemaVersion
expectedCanonicalInputHash
requestedExecutorVersion: QDR6-MOCK-REPLAY-1
```

- `tenantId` 必须与 identity tenant 一致。
- command 只携带 selector 与 expected compatibility metadata，不携带 snapshot payload、raw material、clock 或 dynamic configuration。
- executor 必须调用现有 full-identity exact read；禁止 tenantless、UUID-only、latest 或 scan。

### 4.2 DeterministicReplayResult

```text
tenantId
snapshotId
traceId / requestId / decisionId / decisionRunId
snapshotSchemaVersion
canonicalizationVersion
executorVersion
canonicalInputHash
baselineOutputHash
replayOutputHash
ReplayReproducibilityStatus status
List<ReplayDifference> differences
ReplayFailureCode failureCode optional
sanitizedMessage optional
```

Result 不包含 raw canonical payload、raw values、prompt/provider content、credential、交易字段或 authorization flag。

### 4.3 ReplayDifference

```text
type: ReplayDifferenceType
path
expectedHashOrRef optional
actualHashOrRef optional
sanitizedMessage
```

`path` 与 message 必须经过 safe-text 校验。expected/actual 只允许 hash、version、enum 或 safe ref；禁止保存 raw value。

### 4.4 ReplayReproducibilityStatus

```text
REPRODUCIBLE
DIFFERENT
INCOMPLETE
UNSUPPORTED_VERSION
INVALID_INPUT
EXECUTION_FAILED
```

不得新增 `SUCCESS`、`PASS`、`MOSTLY_REPRODUCIBLE` 等模糊状态。

### 4.5 ReplayFailureCode

```text
SNAPSHOT_NOT_FOUND
TENANT_IDENTITY_MISMATCH
CORRELATION_MISMATCH
CANONICAL_HASH_MISMATCH
VERSION_VECTOR_INCOMPLETE
CANONICALIZATION_VERSION_UNSUPPORTED
EXECUTOR_VERSION_UNSUPPORTED
LEGACY_NOT_REPLAYABLE
UNSAFE_INPUT
BASELINE_INCOMPLETE
PERSISTENCE_READ_FAILED
MOCK_TRANSFORMATION_FAILED
OUTPUT_CANONICALIZATION_FAILED
```

## 5. QDR6-MOCK-REPLAY-1

`QDR6-MOCK-REPLAY-1` 是 deterministic storage/reproducibility baseline，不是 provider 或 business-policy re-execution。其本地 transformation 只允许把已验证 snapshot 投影为：

```text
expected decision summary structured fields
stable safe evidence refs
policy/evaluation/prompt/model/gateway version fingerprint
replay/expected/provider summary hashes
fixed executor/canonicalization/hash versions
```

输出使用 `QDR6-CJSON-1` canonicalize，并使用独立 domain：

```text
SHA-256(
  "DH-QDR6-DETERMINISTIC-REPLAY-OUTPUT\0"
  + snapshot schema version
  + canonicalization version
  + executor version
  + hash algorithm version
  + canonical replay output bytes
)
```

不得调用真实 model、provider、prompt template、policy engine、NQ 或任何外部 IO。该 baseline 证明相同 frozen record 在相同版本下产生相同本地投影，不证明真实 AI/provider 输出可重现。

## 6. Execution sequence

```text
validate DeterministicReplayCommand
-> load persisted record by tenant + full identity
-> verify tenant/correlation/identity
-> reconstruct ReplayInputSnapshot from record only
-> canonicalize snapshot and re-hash
-> exact compare canonicalInputHash and version compatibility
-> execute QDR6-MOCK-REPLAY-1 local projection
-> canonicalize/hash replay output
-> compare frozen baseline projection and replay projection
-> emit structured DeterministicReplayResult
```

任何 read、reconstruction、canonicalization、hash、transformation 或 comparison exception 必须映射为结构化非成功结果；不得 fallback 到 mutable source 或默认值。

## 7. Difference taxonomy

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

- input hash 不匹配属于 `INVALID_INPUT / CANONICAL_HASH_MISMATCH`，不得降级为普通 difference。
- unsupported canonicalization/executor/schema version 属于 `UNSUPPORTED_VERSION`。
- `DIFFERENT` 必须至少包含一个结构化 difference；`REPRODUCIBLE` 必须 differences 为空且 baseline/replay output hash exact match。

## 8. Fail-closed mapping

| Condition | Status | Failure code |
| --- | --- | --- |
| snapshot missing | `INCOMPLETE` | `SNAPSHOT_NOT_FOUND` |
| tenant/identity/correlation mismatch | `INVALID_INPUT` | 对应 mismatch code |
| canonical hash mismatch | `INVALID_INPUT` | `CANONICAL_HASH_MISMATCH` |
| incomplete version/baseline | `INCOMPLETE` | `VERSION_VECTOR_INCOMPLETE` / `BASELINE_INCOMPLETE` |
| unsupported schema/canonicalizer/executor | `UNSUPPORTED_VERSION` | 对应 unsupported code |
| legacy/unsafe input | `INVALID_INPUT` | `LEGACY_NOT_REPLAYABLE` / `UNSAFE_INPUT` |
| persistence failure | `EXECUTION_FAILED` | `PERSISTENCE_READ_FAILED` |
| transformation/canonicalization failure | `EXECUTION_FAILED` | 对应 execution code |

## 9. Implementation boundary

下一任务只允许新增：

```text
dh-usecase/.../qdr/replay/deterministic/** contracts/service/comparator
dh-usecase/.../qdr/replay/deterministic/** pure unit tests
最小 internal wiring（只有实际需要且不新增外部入口时）
docs/current 最小事实同步
```

必须复用现有 snapshot port、record、canonicalizer 与 hasher。禁止 schema/migration、port、Repository/JDBC、API/Controller、HTTP/provider/NQ/Agent/LangGraph/LIVE 扩展。若实现发现 record 无法无损重建 canonical snapshot，立即停止并转入 `DH-STAGE-QDR-6-B3-REPLAY-INPUT-GAP-REVIEW`。

## 10. Required implementation tests

至少覆盖：same record reproducible、record reconstruction/hash exact、map/set/locale/timezone stability、tenant/correlation mismatch、missing/legacy/unsafe input、hash mismatch、incomplete/unsupported versions、各 difference type、`DIFFERENT` 必须携带 differences、mock transformation/canonicalization/read failure、no clock/random/environment、no HTTP/provider/NQ/Agent/trading dependency、result 无 authorization/raw material。

## 11. Authorization boundary

任何 `REPRODUCIBLE` 或其他 result 都只表示内部 mock reproducibility evidence，不代表 Provider authorization、NQ integration permission、trading/execution permission、Paper/LIVE permission、Agent 准入或业务策略正确性。

## 12. Known documentation residual

`README.md`、`docs/current/README.md` 与 supporting `ROADMAP.md` 仍含早期 Stage-QDR-6 planning/B1 入口措辞。本轮 allowlist 不包含这些文件，因此未修改；当前 gate 事实以优先级更高且已同步的 `STATUS.md` 与 `WORK_ORDER.md` 为准。该 residual 不改变代码 sufficiency 或 gate verdict，但后续文档治理应收口。

## 13. Next action

```text
DH-STAGE-QDR-6-B3-DETERMINISTIC-REPLAY-BASELINE
```
