# DH Stage-QDR-6 B3 Canonical Snapshot Input Contract Review

## 1. Review identity

```text
task: DH-STAGE-QDR-6-B3-CANONICAL-SNAPSHOT-INPUT-CONTRACT-REVIEW
classification: REVIEW_ONLY + CANONICAL_SNAPSHOT_CONTRACT + DETERMINISTIC_REPLAY_SEMANTICS + PERSISTENCE_SUFFICIENCY_REVIEW
baseline: 78a67501701d1f46e47fa73c7d8d3c56870595aa
STAGE_QDR_6_B1: DONE / COMMITTED
STAGE_QDR_6_B2: DONE / COMMITTED
CANONICAL_SNAPSHOT_INPUT_CONTRACT: FROZEN
EXISTING_PERSISTENCE_SUFFICIENT: NO
B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED: YES
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION: NO
```

本 review 只冻结 B3 输入合同、canonicalization、hash material、executor compatibility 与 fail-closed 语义，不实现 snapshot assembler、canonicalizer、hash 或 deterministic replay，不授权 Repository、JDBC、SQL、migration、API、Provider、Agent、NQ 或 LIVE 扩展。

## 2. Code reality and persistence sufficiency

### 2.1 V5 persisted context snapshot

V5 `dh_decision_context_snapshot` 实际持久化 `decision_id`、`tenant_id`、`trace_id`、`context_snapshot_json`、`evidence_refs_json`、`created_at`。`JdbcDecisionReplayQueryRepository` 可以通过 `tenant_id + decision_id` 读取 JSON，并投影为 `DecisionReplayContextView`。

生产写路径 `DefaultDecisionOrchestrator.contextSnapshotJson(...)` 当前只写：

```text
snapshotPresent
snapshotId
capturedAt
evidenceCount
```

因此 V5 保存的是 safe snapshot metadata，不是 deterministic replay 所需的完整 immutable decision context。V5 context 表也没有 `contextSchemaVersion`。`evidenceRefsJson` 可读，但其集合语义和原始顺序没有 canonical contract，不能直接把数据库/serializer 顺序当作 hash 顺序。

V5 request 另有 `schema_version`、`source`、`decision_type`、`subject_json`、`requested_at`，可由现有 tenant-bound replay port 读取；其中 `schema_version` 可作为 `decisionSchemaVersion` 候选，`created_at/requested_at` 不能作为 replay execution input 的替代时间。

### 2.2 V6 correlation and context

V6 持久化 `decision_request.id`、`tenant_id`、`trace_id`、`request_id`、`input_payload_json`、`context_payload_json` 和 `decision_run.id`。现有 `DecisionReadModelQueryPort` 可 tenant-bound 读取 `decisionRunId`、correlation、run summary 与 evidence refs。

但生产 read model 明确不暴露 `input_payload_json/context_payload_json`；`DecisionRunDetailView` 和 `DecisionEvidenceView` 只提供摘要与 safe refs。因此现有 port 不能把 V6 payload 作为 canonical context source，也不能用其补齐 V5 缺失内容。不得新增直读 SQL 绕过此边界。

### 2.3 V8 model, prompt and gateway version inputs

V8 `qdr_model_gateway_call` 持久化 `prompt_version_id`、`model_version_id`、`provider_profile_id`、safe refs、input/output hashes 与脱敏 summaries。`ModelGatewayCallRecord` 可通过 `tenantId + modelCallRef` 读取上述 metadata；`ModelVersionPersistencePort` 可按 `tenantId + modelVersionId` 读取 immutable model version。

仍存在以下读取/语义缺口：

- `PromptVersionPersistencePort` 只有 `tenantId + promptTemplateId + version` 查询，没有按 gateway record 已知的 `promptVersionId` 读取方法。
- `modelGatewayVersionRef` 不是 V8 独立持久化的 gateway executor/version；Stage-QDR-5 projection 当前用 `model-version:<modelVersionId>` 派生该值，不能证明 gateway implementation version。
- V6 production evidence adapter 返回 `provider-call-log:<V5 id>`；B2 将该字符串直接作为 V8 `modelCallRef` 查询。两种 identity 没有已证明的持久化映射，真实路径不能保证定位 V8 record。
- B2 aggregate 不输出 `promptVersionId`、`modelVersionId`、gateway implementation version 或对应 checksums。

### 2.4 V9 replay, evaluation and regression inputs

V9 existing tenant-bound repositories 可读取：

```text
ReplayCaseRecord:
  policyVersion, modelGatewayVersionRef, ReplayInputRef,
  ExpectedDecisionSummary, expectedSummaryHash, caseChecksum

EvaluationCaseRecord:
  policyVersion, modelVersionRef, modelGatewayVersionRef,
  ReplayInputRef/ReplayOutputRef, expected/actual summary and hashes,
  evaluationChecksum

RegressionVerdictRecord:
  policyVersion, modelGatewayVersionRef, expected/actual summary hashes,
  verdict/finding refs
```

这些 record 都以 `tenantId` 为第一查询边界，列表 SQL 也有显式稳定排序。但 V9 的 input ref 仅包含 `refType/refId/contentHash`，不是 canonical snapshot 内容；`modelVersionRef/modelGatewayVersionRef` 在部分表/record 中允许为空；V9 没有 `decisionSchemaVersion`、`contextSchemaVersion`、`promptVersionRef`、`canonicalizationVersion`、`replayExecutorVersion`。已有 checksum/hash 也没有声明使用 `QDR6-CJSON-1` 或本合同 domain separator，不能复用为 B3 execution hash。

### 2.5 B1/B2 aggregate capability

`DecisionEvidenceAggregate` 只有 correlation、typed safe refs、status、findings、missing mandatory evidence。B2 service 读取 V5/V6/V8/V9 后仍只投影 safe refs 与可选 hash；它不携带 V5 snapshot 内容、V8 version vector 或 V9 structured input/summary。

当前不存在：

```text
ReplayInputSnapshot assembler
QDR6-CJSON-1 canonicalizer
QDR6-MOCK-REPLAY-1 executor
ReplayExecutionHash implementation
```

仓库中的其他 SHA-256 helper 只服务各自 checksum/hash，不满足本合同的 canonical bytes 与 domain separation 规则。

## 3. Frozen ReplayInputSnapshot contract

字段分类含义：`REQUIRED` 必须持久化且可 tenant-bound 读取；`OPTIONAL` 缺失时从 canonical JSON 省略，不得写 `null`；`CORRELATION_ONLY` 用于归属校验而非业务输入；`EXCLUDED` 不得进入 snapshot。

| Field | Classification | Hash | Frozen semantics |
| --- | --- | --- | --- |
| `snapshotSchemaVersion` | REQUIRED | YES | 固定 `QDR6-REPLAY-INPUT-1`。 |
| `decisionSchemaVersion` | REQUIRED | YES | 来自持久化 decision contract；不得用 current/latest。 |
| `contextSchemaVersion` | REQUIRED | YES | immutable context schema；当前未持久化。 |
| `canonicalizationVersion` | REQUIRED | YES | 固定 `QDR6-CJSON-1`。 |
| `replayExecutorVersion` | REQUIRED | YES | 固定 `QDR6-MOCK-REPLAY-1`。 |
| `hashAlgorithm` | REQUIRED | YES | 固定 `SHA-256`。 |
| `tenantId` | CORRELATION_ONLY | YES | 作为安全域绑定进入 hash，必须与所有 source records 一致。 |
| `traceId` | CORRELATION_ONLY | NO | 必须一致，不影响业务 canonical input。 |
| `requestId` | CORRELATION_ONLY | NO | 必须一致。 |
| `decisionId` | CORRELATION_ONLY | NO | 必须一致。 |
| `decisionRunId` | CORRELATION_ONLY | NO | 必须一致；不得由 request/decision ID 猜测。 |
| `sourceDecisionId` | CORRELATION_ONLY | NO | 有值时必须与 `decisionId` 一致。 |
| `sourceRequestId` | CORRELATION_ONLY | NO | 有值时必须与 `requestId` 一致。 |
| `source` | REQUIRED | YES | 持久化的 decision source safe enum/ref。 |
| `decisionType` | REQUIRED | YES | 仅接受 `READ_ONLY_RECOMMENDATION`。 |
| `subject` | REQUIRED | YES | V5 已持久化的 immutable safe subject object；禁止执行字段。 |
| `contextSnapshot` | REQUIRED | YES | 完整、不可变、schema-bound 的 safe decision context；不能只有 snapshot metadata/ref。 |
| `evidenceRefs` | REQUIRED | YES | typed safe refs；按 set-like 规则 canonicalize。 |
| `policyVersion` | REQUIRED | YES | decision/policy contract version；不得默认 latest。 |
| `evaluationPolicyVersion` | REQUIRED | YES | replay/evaluation policy version；与 `policyVersion` 相同也必须显式保存。 |
| `promptVersionRef` | REQUIRED | YES | immutable prompt version ref/checksum；不得只用当前版本。 |
| `modelVersionRef` | REQUIRED | YES | immutable model version ref/checksum。 |
| `modelGatewayVersionRef` | REQUIRED | YES | gateway implementation/contract version，不能以 model version 冒充。 |
| `providerProfileRef` | OPTIONAL | YES | 仅 safe immutable ref；缺失时省略。 |
| `replayCaseRef` | REQUIRED | YES | tenant-bound V9 case identity。 |
| `replayInputRef` | REQUIRED | YES | structured ref。 |
| `replayInputHash` | REQUIRED | YES | 已持久化 source content hash；不等同 execution hash。 |
| `expectedDecisionSummary` | REQUIRED | YES | V9 safe structured summary。 |
| `expectedSummaryHash` | REQUIRED | YES | 与 summary 一致的 persisted hash。 |
| `evaluationCaseRef` | OPTIONAL | NO | 已存在时用于 lineage，不改变 replay input。 |
| `regressionVerdictRef` | OPTIONAL | NO | 已存在时用于 lineage，不改变 replay input。 |
| `providerSummaryHash` | OPTIONAL | YES | 只有 provider summary 实际存在时纳入；不得合成。 |
| `sourceCapturedAt` | REQUIRED | YES | 业务 snapshot 自身的 persisted capture time；不是查询时间。 |
| `snapshotCreatedAt` | EXCLUDED | NO | assembler/current clock 不得进入 snapshot/hash。 |
| database PK / row order | EXCLUDED | NO | 仅可作为查询实现细节。 |
| latency/observedAt/updatedAt | EXCLUDED | NO | 环境/观察元数据不属于 replay input。 |

以下材料一律 `EXCLUDED`，出现即 `UNSAFE_INPUT_REJECTED`，不得仅做 trim/redact 后继续：

```text
raw prompt
raw provider response
credential
apiKey
apiSecret
token
cookie
passphrase
交易或执行指令
```

交易或执行指令包括 `BUY`、`SELL`、`MARKET_ORDER`、`PLACE_ORDER`、`CANCEL_ORDER`、`MUTATE_NQ_STATE` 及等价 payload；`forbiddenActions` 中作为禁止值出现除外，但必须保留明确的 forbidden-field 语义。

## 4. Frozen version vector

```text
decisionSchemaVersion: REQUIRED
contextSchemaVersion: REQUIRED
policyVersion: REQUIRED
promptVersionRef: REQUIRED
modelVersionRef: REQUIRED
modelGatewayVersionRef: REQUIRED
evaluationPolicyVersion: REQUIRED
canonicalizationVersion: REQUIRED / QDR6-CJSON-1
replayExecutorVersion: REQUIRED / QDR6-MOCK-REPLAY-1
```

任何 required version 缺失、空白、无法 tenant-bound 读取或只可解析为 `latest/current/default` 时，返回 `VERSION_INPUT_MISSING`；不得自动填充、从当前配置推断或调用外部系统补齐。

## 5. QDR6-CJSON-1 byte-level rules

1. 顶层必须是 JSON object；输出为无 BOM、无尾随换行的 UTF-8 bytes。
2. object key 在 key 经过 Unicode NFC 后，按 Unicode code point lexicographic ascending 排序；重复 key 拒绝。
3. JSON 字符串值做 Unicode NFC，不做 trim、不改变大小写；若合同字段不允许首尾空白则拒绝输入，而不是静默改写。非 ASCII 字符直接编码为 UTF-8；双引号和反斜线使用 JSON 短转义，控制字符使用标准短转义或 `\u00XX` uppercase hex；不转义 `/`。
4. enum 必须使用合同声明的 uppercase ASCII token；未知值或大小写漂移拒绝。
5. boolean 只能输出 `true` 或 `false`。
6. integer 使用 base-10，零为 `0`，禁止 `+`、前导零和 `-0`。
7. decimal 必须以精确 `BigDecimal`/等价十进制输入；输出 plain decimal、禁止 exponent、移除无意义 trailing zeros、`-0` 归一为 `0`。二进制 float/double、NaN、Infinity 一律拒绝。
8. timestamp 必须先严格解析为 `Instant`，输出 UTC `Z` 的 `Instant.toString()` canonical form；保留实际纳秒精度，不接受 local time、offset 保留、locale format 或数据库 session timezone。
9. UUID 输出 lowercase 8-4-4-4-12 hyphenated form；其他 ID 保持 NFC 后原值，不做大小写/trim 推断。
10. required 字段的 absent 或 `null` 均拒绝；optional 字段 absent 时省略，explicit `null` 拒绝。`null` 与 absent 不得等价。
11. `evidenceRefs`、`forbiddenActions` 等明确声明为 set-like 的数组：先按元素 canonical bytes 去重，再按 canonical bytes unsigned lexicographic ascending 排序。
12. 明确声明为 ordered business sequence 的数组保持原顺序；未声明数组语义时拒绝，不猜测排序方式。
13. map 使用 object key 规则；set 必须先转换为合同声明的 set-like array。不得依赖 `HashMap/HashSet` iteration order。
14. 数据库列表读取必须有稳定 SQL order；即便有稳定 SQL order，set-like field 仍按本合同重排。不得让 row order、分页时钟或并列排序影响 bytes。
15. 禁止 current time、random、host/path、locale、timezone、process/thread ID、database PK、query latency 等环境字段。

## 6. Hash material and executor compatibility

```text
algorithm: SHA-256
canonicalization: QDR6-CJSON-1
executor compatibility: QDR6-MOCK-REPLAY-1
domain separator ASCII:
DH|QDR6|REPLAY_INPUT|QDR6-REPLAY-INPUT-1|QDR6-CJSON-1|QDR6-MOCK-REPLAY-1|SHA-256\n
canonical bytes:
domainSeparatorBytes || qdr6CanonicalSnapshotBytes
execution hash:
lowercaseHex(SHA-256(canonical bytes))
```

Hash 覆盖表格中 `Hash = YES` 的字段。`tenantId` 是唯一进入 hash 的 correlation-only 字段，用于 tenant domain binding；`traceId/requestId/decisionId/decisionRunId/source*Id` 只做 fail-closed correlation validation，不进入 hash。`createdAt/updatedAt/observedAt`、数据库主键、row order 和 assembler clock 均排除。

未来 B3 只能返回结构化 `ReplayExecutionHash`：

```text
algorithm
canonicalizationVersion
replayExecutorVersion
snapshotSchemaVersion
hashHex
```

本轮不持久化 hash。V9 现有 hash/checksum 缺少本合同 version/domain 声明，不能冒充 execution hash。若未来要求持久化 execution hash/version tuple，必须另行做 schema/repository review，不能复用含义不同的现有列。

`QDR6-MOCK-REPLAY-1` 只接受完整 `QDR6-REPLAY-INPUT-1` + `QDR6-CJSON-1` bytes；不允许 Provider/HTTP/NQ/Agent/LangGraph/clock/random/IO，不允许兼容 unknown executor/canonicalization version。

## 7. Completeness and fail-closed taxonomy

| Code | Trigger | Result |
| --- | --- | --- |
| `SNAPSHOT_MISSING` | canonical context source 不存在 | `INCOMPLETE` |
| `SNAPSHOT_INCOMPLETE` | required context/refs 缺失 | `INCOMPLETE` |
| `CORRELATION_MISMATCH` | trace/request/decision/run/source identity 冲突 | `INVALID` |
| `TENANT_MISMATCH` | 任一 source tenant 不一致 | `DENIED` |
| `VERSION_INPUT_MISSING` | version vector 缺失或使用 latest/default | `INCOMPLETE` |
| `UNSAFE_INPUT_REJECTED` | raw/credential/trading/execution material | `DENIED` |
| `CANONICALIZATION_FAILED` | 合法 version 下无法生成 canonical bytes | `FAILED` |
| `UNSUPPORTED_CANONICALIZATION_VERSION` | 非 `QDR6-CJSON-1` | `INVALID` |
| `UNSUPPORTED_REPLAY_EXECUTOR_VERSION` | 非 `QDR6-MOCK-REPLAY-1` | `INVALID` |
| `HASH_GENERATION_FAILED` | SHA-256/encoding 内部失败 | `FAILED` |

同一持久化 source 多条记录互相冲突、explicit null/absent 违规、unknown array semantics、float 输入、timestamp 无法严格解析，也必须归入上述最具体的 fail-closed code，不能跳过字段继续执行。

## 8. Readiness decision

```text
CANONICAL_SNAPSHOT_INPUT_CONTRACT: FROZEN
EXISTING_PERSISTENCE_SUFFICIENT: NO
B3_SNAPSHOT_INPUT_INSUFFICIENT_BLOCKED: YES

ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION: NO
ALLOW_CANONICALIZER_IMPLEMENTATION: NO
ALLOW_DETERMINISTIC_REPLAY_IMPLEMENTATION: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_LIVE: NO
```

阻断字段/能力：

1. 完整 immutable `contextSnapshot` 未持久化；V5 只有 metadata。
2. `contextSchemaVersion` 未持久化。
3. `evaluationPolicyVersion` 与 decision `policyVersion` 未形成完整、显式 version vector。
4. `promptVersionRef` 不能从 gateway record 通过现有 tenant-bound port 按 ID 解析。
5. 独立 `modelGatewayVersionRef` 未被 V8 持久化；当前值由 model version 派生。
6. V6 provider-call-log ref 与 V8 modelCallRef 的生产关联未证明。
7. B2 aggregate 不携带 canonical content、structured V9 input/summary 或 version vector。
8. 现有 hash/checksum 没有 `QDR6-CJSON-1` + domain separator + executor version 语义。

最小扩展方向仅供下一轮 review，不构成本轮授权：

- 为 safe immutable context 内容及 `contextSchemaVersion` 建立明确持久化来源；优先 additive migration，不修改 V1-V9。
- 提供 tenant-bound snapshot read port，禁止 tenantless scan 和 raw material。
- 补齐按 `tenantId + promptVersionId` 的安全读取能力，以及持久化的 gateway contract/executor version。
- 建立 V6/V8 可证明的 stable model call identity，或由新的 tenant-bound assembler source 明确返回。
- 让 snapshot source 返回完整 version vector 与 structured V9 baseline，而不是把 B2 safe-ref aggregate 扩成 raw carrier。

## 9. Next concrete action

```text
DH-STAGE-QDR-6-B3-SNAPSHOT-PERSISTENCE-GAP-REVIEW
```

该 review 必须先决定 additive persistence/port 的最小边界；在独立授权前不得创建 migration、Repository/JDBC/SQL、API 或 provisional B3 implementation。
