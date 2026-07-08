# NQ-DH Integration-1 M0 Contract Gap Close Work Order（DH）

> Task: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO
> Status: COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED
> Date: 2026-07-03
> Repository: Decision Hub
> Source of truth: docs/current

## 1. 目标与边界

本工单只关闭进入 dry-run mock 前必须明确的 contract gap：`NQ_DRYRUN` source allowlist、canonical error taxonomy、dry-run endpoint shape、schema alias / envelope 字段边界，以及后续 M1 / M2 / M3 的入场条件。

本工单不是 implementation，不授权代码、测试、fixture JSON、schema、contracts、golden cases、API、Controller、migration、runtime、真实 HTTP、real provider、AI / Agent runtime、LangGraph runtime 或 LIVE。

## 2. 前置边界确认

```text
DH repository: F:\project\decision-hub
DH branch: dev
DH HEAD: 6a806a7148712b06b8a4712ed1050da7bebcba0c
DH precheck: clean

NQ dry-run worktree: F:\worktrees\nexus-quant-i1-dryrun
NQ dry-run branch: nq-dh-i1-dryrun
NQ dry-run HEAD: 752f228abe3e4e4a7e6d223211291e10a894d5c7
NQ dry-run precheck: clean

NQ dev repository: F:\project\nexus-quant
NQ dev branch: dev
NQ dev HEAD: 294de92df73668a77b449b7da8318e220f7b8f5c
NQ dev precheck: clean
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
```

NQ dev 本轮只读；未修改、未覆盖、未回滚任何 NQ dev 文件。

## 3. Contract gap close 结论

```text
NQ_DRYRUN source allowlist decision: NEEDS_SECURITY_CONTRACT_CHANGE
Error taxonomy decision: PARTIAL_EXISTS_NOW + DOC_MAPPING_ONLY + REVIEW_GATED_GAPS
Dry-run endpoint shape: RECOMMENDED_SHAPE = Option C / test-support mock-only, no runtime endpoint
Schema alias / envelope fields: DOC_ONLY_ALIAS unless future schema contract review accepts wire change
M0 close: YES
M1 work order allowed: YES
Implementation code allowed by M0: NO
```

## 4. Source allowlist decision

`NQ_DRYRUN` 当前不属于已实现 dry-run source allowlist。现有合同和代码已有 `source` 字段、fail-closed source 校验机制和 Integration-0 的 source allowlist 行为，但没有把 `NQ_DRYRUN` 作为可用 runtime source。

| Item | Decision | 说明 |
| --- | --- | --- |
| `source` 字段 | EXISTS_NOW | `DecisionRequest` schema 已有 `source` required 字段。 |
| 现有 source fail-closed 机制 | EXISTS_NOW | Integration-0 / feedback security 已有 allowlist 和拒绝语义。 |
| `NQ_DRYRUN` source 值 | NEEDS_SECURITY_CONTRACT_CHANGE | 只能在后续安全合同 review 中加入；M0 不改 schema / enum / code。 |
| `dryRun` 语义 | DOC_ONLY_ALIAS | 在 M1 前只能作为 `source=NQ_DRYRUN` 的文档语义，不能作为 wire field。 |
| 未列入 source | FAIL_CLOSED | 后续实现必须拒绝；当前 canonical 映射为 `SOURCE_DENIED`，兼容现有 `SOURCE_NOT_ALLOWED` 语义。 |

未来如果允许 `NQ_DRYRUN`，必须满足：

- 只用于 dry-run / mock / test-support，不得接真实 provider、真实 HTTP、runtime 或 LIVE。
- 必须绑定 `tenantId`、`requestId`、`traceId`，header 不得覆盖 body 权威字段。
- 不得读取 NQ DB，不得访问 credential，不得触发 order / risk / ledger / Paper Run mutation。
- 未授权 source、tenant mismatch、nonce replay、signature invalid、payload oversize 均 fail-closed。

## 5. Error taxonomy decision

| Canonical code | Decision | 当前证据 / 映射 |
| --- | --- | --- |
| `SIGNATURE_INVALID` | EXISTS_NOW | Integration-0 HMAC / signature 校验已有拒绝语义。 |
| `PAYLOAD_TOO_LARGE` | EXISTS_NOW | Integration-0 payload size gate 已有拒绝语义。 |
| `RATE_LIMITED` | EXISTS_NOW | feedback ingress rate limit / audit event 已有语义。 |
| `FORBIDDEN_FIELD` | EXISTS_NOW | forbidden field validation 已有语义。 |
| `TENANT_MISMATCH` | EXISTS_NOW | tenant binding / header binding mismatch 已有语义。 |
| `PROVIDER_DISABLED` | EXISTS_NOW | provider status / reason 已有禁用语义。 |
| `PROVIDER_TIMEOUT` | EXISTS_NOW | provider timeout / budget path 已有语义。 |
| `PROVIDER_BUDGET_EXCEEDED` | EXISTS_NOW | provider budget path 已有语义。 |
| `RISK_BLOCKED` | EXISTS_NOW | decision audit / risk rejection 语义已有。 |
| `TIMESTAMP_SKEW` | DOC_MAPPING_ONLY | 当前实现更接近 `TIMESTAMP_INVALID` / `TIMESTAMP_OUT_OF_WINDOW`；M0 不重命名。 |
| `NONCE_REPLAY` | DOC_MAPPING_ONLY | 当前实现更接近 `REPLAY` / `REPLAY_REJECTED`；M0 不重命名。 |
| `SOURCE_DENIED` | DOC_MAPPING_ONLY | 当前实现更接近 `SOURCE_NOT_ALLOWED`；M0 不重命名。 |
| `AUTH_FAILED` | NEEDS_CONTRACT_REVIEW_BEFORE_CODE | DH/NQ 现有认证失败码不完全一致，不能直接写入 schema / fixture。 |
| `CONTRACT_INVALID` | NEEDS_CONTRACT_REVIEW_BEFORE_CODE | 当前存在 schema validation / invalid schema 语义，canonical 命名需 review。 |
| `INTERNAL_FAIL_CLOSED` | NEEDS_CONTRACT_REVIEW_BEFORE_CODE | 当前存在 decision / persistence failure 语义，canonical 归一需 review。 |

本轮未发现上述 canonical code 中必须标为 `PROHIBITED` 的项。未知 error code 的未来处理必须 fail-closed，且不得把外部原始错误、内部异常栈、SQL、包名、credential path 或 raw provider response 暴露给前端或 NQ。

## 6. Dry-run endpoint shape decision

| Option | Decision | 说明 |
| --- | --- | --- |
| A. 新增 DH dry-run endpoint | BLOCKED | 需要 API / contract / security review；M0 不授权。 |
| B. 复用现有 secured entry | BLOCKED | `POST /api/ai/feedback/nq` 是 feedback ingest，不是 decision dry-run endpoint；不能直接复用。 |
| C. test-support / mock-only，无 runtime endpoint | RECOMMENDED_SHAPE | M1 / M2 优先走该形态，避免提前引入 runtime / HTTP surface。 |
| D. 延后 API review | FUTURE_IF_NEEDED | 如未来必须有 HTTP endpoint，另起 API / contract / security review。 |

`BLOCKERS`：

- 当前无 dry-run Controller / OpenAPI path / API envelope。
- `NQ_DRYRUN` source allowlist 尚未进入安全合同。
- canonical error taxonomy 尚未完全归一。
- gap fields 不属于现有 wire schema。
- runtime integration、real HTTP、real provider 均未开始。

`REQUIRED_REVIEW_BEFORE_CODE`：

- API path / Controller / OpenAPI review。
- source allowlist security review。
- error taxonomy canonical naming review。
- request / response envelope 和 schema version header review。
- no-side-effect / no-outbound / fail-closed review。

`PROHIBITED_CAPABILITIES`：

```text
real provider
real HTTP
runtime integration
NQ DB read/write
credential access
order / cancel / position / account / ledger mutation
Paper Run mutation
AI / LangGraph runtime
LIVE
```

## 7. Schema alias / envelope gap decision

| Field | Current schema reality | M0 decision | Fixture/API impact |
| --- | --- | --- | --- |
| `dryRun` | request/output schema 均不存在该字段 | DOC_ONLY_ALIAS | 不得作为 required fixture field；不得进入 API。 |
| `decisionId` | output schema 不存在；内部 persistence / replay 有相关 id | DOC_ONLY_ALIAS；wire 需 schema review | 不得要求 NQ fixture 返回该 wire field。 |
| `confidence` | output schema 不存在；内部历史/评估上下文存在相关语义 | DOC_ONLY_ALIAS；wire 需 schema review | 不得作为 output schema 必填。 |
| `traceSummary` | output schema 不存在 | DOC_ONLY_ALIAS；future envelope planning | 不得进入 M1/M2 runtime contract。 |
| `replayRef` | output schema 不存在；replay read model 已有内部能力 | DOC_ONLY_ALIAS；wire 需 schema review | 不得作为跨仓 required field。 |
| `auditRef` | output schema 不存在；audit persistence 已有内部能力 | DOC_ONLY_ALIAS；wire 需 schema review | 不得作为跨仓 required field。 |
| `X-NQ-DH-Schema-Version` | header 未实现；body 已有 `schemaVersion` | DOC_ONLY_ALIAS；header 需 schema/header review | 不得新增 header 校验或签名语义。 |

现有可依赖的 contract reality：

```text
DecisionRequest required fields: requestId, traceId, tenantId, source, decisionType, subject, requestedAt, schemaVersion
DecisionOutput required fields: requestId, traceId, tenantId, decisionType, action, status, riskLevel, policyStatus, providerStatus, forbiddenActions, createdAt, schemaVersion
DecisionAction whitelist: ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
fixed forbiddenActions: PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
additionalProperties: false
```

## 8. M1 / M2 / M3 入场条件

```text
ALLOW_M0_WO_CLOSE: YES
ALLOW_I1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

M1 的唯一允许方向是 `NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED`，且仍必须是 work order / mock-contract planning，不得直接写 implementation code。若 M1 需要改 schema、contracts、fixtures、OpenAPI、Controller、security allowlist 或 error enum，必须先另起 review，不能由 M0 直接授权。

## 9. 禁止项确认

```text
BUY / SELL / quantity / price / leverage: PROHIBITED
order / account / credential / mutation: PROHIBITED
NQ DB read/write: PROHIBITED
real provider / RealClient / real HTTP: PROHIBITED
Paper Run / LIVE / exchange private API: PROHIBITED
AI / Agent runtime / LangGraph runtime: PROHIBITED
```

## 10. 验证要求

本工单收尾必须验证：

```text
git status --short
git diff --check
git diff --stat
git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
mvn -ntp test
mvn -ntp -Pquality validate
```

NQ dry-run worktree 与 NQ dev 必须分别执行对应边界检查；结果写入 `docs/current/TESTING.md` 和 `docs/current/WORKLOG.md`。

## 11. 下一步

```text
NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / COMPLETED / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NOT IMPLEMENTED
```

M1 与 M2 已按 M0 准入完成 work-order-only planning；M1 继续采用 `Option C / test-support mock-only / no runtime endpoint`，M2 继续采用 test-support mock-only stub / recorder planning，不允许直接 implementation。后续唯一允许动作是 `NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / NOT STARTED / WORK_ORDER_ONLY_ALLOWED`。
