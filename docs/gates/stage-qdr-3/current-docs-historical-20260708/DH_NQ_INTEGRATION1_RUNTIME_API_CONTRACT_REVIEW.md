# DH-NQ Integration-1 Runtime API Contract Review

> 任务：`NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW`
> 类型：`REVIEW_ONLY + RUNTIME_API_CONTRACT_SECURITY_REVIEW + CROSS_REPO_BOUNDARY_REVIEW + POST_PR_MERGE_BASELINE + NO_RUNTIME_IMPLEMENTATION + NO_LIVE`
> 日期：2026-07-04
> 仓库视角：Decision Hub（DH）
> 状态：`CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME`

## 1. 结论

本轮只做 limited dry-run runtime 的 API / contract / security review，不实现 runtime。结论是：**允许关闭本 review，并允许后续拆出 DH runtime API work order 与 NQ limited dry-run client work order；仍不允许现在实现 runtime、真实 HTTP、real provider、API Controller、schema/contracts/golden_cases 或 LIVE**。

Readiness decision：

```text
ALLOW_RUNTIME_API_CONTRACT_REVIEW_CLOSE: YES
ALLOW_DH_RUNTIME_API_WO: YES
ALLOW_NQ_RUNTIME_CLIENT_WO: YES
ALLOW_RUNTIME_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_API_CONTROLLER_CHANGE_NOW: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮没有新增或修改生产代码、测试代码、Controller、Client、Repository、Service、migration、OpenAPI、JSON Schema、contracts、golden_cases、fixture JSON、runtime wiring、provider 配置或 CI workflow。

## 2. Post-PR baseline

NQ dev 已包含 mock / test-support baseline PR：

```text
NQ dev branch: dev
NQ dev current HEAD: b856cf07155de26f87fad9c21234c1a8a07b964a
NQ PR #12 merge commit: 578eb65e Merge pull request #12 from ling5477/nq-dh-i1-dryrun
PR #12 merge relationship: 578eb65e is ancestor of current dev / origin/dev
Included baseline commits: b997c392 / f658f0ef / 8efbab63 and prior IMP0-IMP3 support commits
NQ dev scoped docs/current NQ-DH / Integration-1 dirty diff: none
NQ dev staged scoped NQ-DH / Integration-1 diff: none
```

NQ worktree 已切在下一阶段 review 分支：

```text
NQ worktree: E:\Project\nexus-quant-i1-dryrun
Branch: nq-dh-i1-runtime-api-contract-review
HEAD: 578eb65e851086d0668bbebef74c319df1e5d63c
Tracking: origin/nq-dh-i1-runtime-api-contract-review
Branch base at creation: PR #12 merge commit 578eb65e
Current origin/dev during final read-only check: b856cf07155de26f87fad9c21234c1a8a07b964a
```

代码与文档扫描结论：

- NQ dev 的 Integration-1 命中只在 `docs/current/**`、`backend/nq-app/src/test/**` 与 `backend/nq-app/src/test/resources/**`。
- NQ dev / NQ worktree `backend/**/src/main/**` 精确扫描未发现 `NQ_DRYRUN`、`NqDhIntegration1`、DH dry-run runtime client、NQ-DH runtime API token 或 dry-run runtime token。
- DH `src/main` 精确扫描未发现 `NQ_DRYRUN`、`DhIntegration1`、NQ-DH dry-run runtime endpoint 或 runtime client token。
- 广域安全扫描命中项均归类为 docs prohibition、test guard、existing unrelated adapter / trading / public-marketdata code 或既有 DH API / provider / security 模块；未发现本轮 actual risk。

## 3. Recommended runtime option

推荐项：**Option D：先冻结 API contract / error taxonomy / envelope，再拆 DH/NQ implementation**。

候选项判断：

| Option | 结论 | 原因 |
| --- | --- | --- |
| Option A：继续 test-support only | 可作为保守回退，不推荐作为下一步 | mock baseline 已合并，继续停留不会关闭 runtime API / contract 安全前置缺口。 |
| Option B：DH 新增 limited dry-run endpoint | 允许进入后续 work order，不允许本轮实现 | endpoint、OpenAPI、schema、feature flag、auth、fail-closed、audit / trace / replay 仍需正式冻结。 |
| Option C：NQ 新增 limited runtime client | 允许进入后续 work order，不允许本轮实现 | NQ client 必须 no-real-http default、feature flag off、no-side-effect tests 先冻结。 |
| Option D：先冻结合同再拆实现 | 推荐 | 能把 PR #12 mock baseline、API 合同、DH endpoint implementation、NQ client implementation 和 joint runtime tests 分开审查，避免把 test-support merge 误读为 runtime authorization。 |

## 4. DH API contract review

DH 是否应该新增 limited dry-run API / Controller：**可以规划，但不能在本轮新增**。

建议的 future endpoint 形态：

```text
Method: POST
Path: /api/ai/decision-dry-runs
Purpose: NQ -> DH limited dry-run readonly DecisionOutput
Implementation state now: NOT IMPLEMENTED
Allowed next step: work-order-only planning
```

请求必须满足：

- 只接受 signed / timestamped / nonce / tenant-bound request。
- header 使用 canonical `X-NQ-DH-*`，最少包含 `Source`、`Timestamp`、`Nonce`、`Signature`、`Tenant-Id`、`Request-Id`、`Trace-Id`、`Schema-Version`。
- `X-NQ-DH-Timestamp` 必须是 RFC3339 / ISO-8601 UTC `Z`，继续拒绝 epoch seconds、epoch milliseconds 和数字时区偏移。
- `source` 当前仍不进入 production allowlist；未来必须显式允许 `NQ_DRYRUN`。
- body 只能携带 read-only `DecisionRequest` 类字段：`requestId`、`traceId`、`tenantId`、`source`、`dryRun=true`、`decisionType=READ_ONLY_RECOMMENDATION`、`subject`、`contextRef`、`contextSnapshot`、`requestedAt`、`schemaVersion`。
- 禁止 `accountId`、`orderId`、position / ledger / credential、raw prompt、raw provider response、`BUY`、`SELL`、`quantity`、`price`、`leverage`、`placeOrder`、`cancelOrder`、`paperRunStart`、`liveRunStart` 等执行意图字段。

响应只能是 readonly `DecisionOutput` envelope：

```text
Required future envelope fields:
  schemaVersion
  dryRun
  decisionId
  requestId
  traceId
  tenantId
  decisionType
  action
  status
  riskLevel
  policyStatus
  providerStatus
  forbiddenActions
  createdAt

Optional future envelope fields:
  confidence
  traceSummary
  replayRef
  auditRef
  reasonCodes
  evidenceRefs
  errors
```

Endpoint 必须 fail-closed：

- provider / Agent / LangGraph 禁止接入。
- DH 不读写 NQ DB。
- DH 不返回 executable trading instruction。
- `action` 只能是 `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`。
- `forbiddenActions` 必须包含 `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。
- audit / trace / replay 只保存安全 summary 和引用，不保存 credential、raw prompt、raw provider response、signature、secret 或完整敏感 header。

## 5. NQ source allowlist review

`NQ_DRYRUN` 当前仍是 review-gated source：**本轮不允许进入 production allowlist**。

未来进入 allowlist 的必要条件：

```text
tenant/source pair allowlist: required
environment profile restriction: dev/test/manual dry-run first; production disabled unless later approved
replay nonce persistence: required, scoped by tenant + source + endpoint + nonce
rate limit: required, tenant/source/route scoped
payload cap: required, recommended max 64 KiB unless contract review changes it
memory cap: required for in-memory guard and audit summary assembly
HMAC: required
timestamp UTC Z: required
requestId / traceId binding: required
schemaVersion: required
kill switch: required
logging redaction: required
no provider / no Agent / no LangGraph / no LIVE: required
```

未满足前，`NQ_DRYRUN` 必须返回 `SOURCE_DENIED` 并 fail-closed；NQ 只能记录拒绝结果，不得执行。

## 6. Error taxonomy review

canonical error taxonomy 必须在实现前冻结为正式 enum / contract。当前不得直接改 Java enum、OpenAPI、JSON Schema、contracts 或 golden_cases。

建议冻结的最小 error code：

```text
SOURCE_DENIED
SIGNATURE_INVALID
TIMESTAMP_SKEW
NONCE_REPLAY
TENANT_MISMATCH
REQUEST_ID_MISMATCH
TRACE_ID_MISMATCH
CONTRACT_INVALID
FORBIDDEN_FIELD
PAYLOAD_TOO_LARGE
RATE_LIMITED
PROVIDER_DISABLED
PROVIDER_TIMEOUT
PROVIDER_BUDGET_EXCEEDED
POLICY_DENIED
RISK_BLOCKED
AUDIT_WRITE_FAILED
REPLAY_WRITE_FAILED
KILL_SWITCH_OPEN
RUNTIME_DISABLED
INTERNAL_FAIL_CLOSED
UNKNOWN
```

映射规则：

| 场景 | error code | HTTP 建议 | DH 输出 | NQ 行为 |
| --- | --- | --- | --- | --- |
| source 未授权 | `SOURCE_DENIED` | 403 | fail-closed | 记录拒绝，不执行 |
| 签名错误 | `SIGNATURE_INVALID` | 401 | fail-closed | 记录拒绝，不执行 |
| timestamp 超窗或非 UTC Z | `TIMESTAMP_SKEW` | 401 | fail-closed | 记录拒绝，不执行 |
| nonce 重放 | `NONCE_REPLAY` | 401 | fail-closed | 记录拒绝，不执行 |
| tenant 不一致 | `TENANT_MISMATCH` | 403 | fail-closed | 记录拒绝，不执行 |
| contract invalid / forbidden field | `CONTRACT_INVALID` / `FORBIDDEN_FIELD` | 400 | fail-closed | 记录拒绝，不执行 |
| provider timeout | `PROVIDER_TIMEOUT` | 200 或 503，随合同冻结 | `ABSTAIN` | 记录 readonly summary |
| budget exceeded | `PROVIDER_BUDGET_EXCEEDED` | 200 或 503，随合同冻结 | `ABSTAIN` | 记录 readonly summary |
| policy denied | `POLICY_DENIED` | 200 或 403，随合同冻结 | `BLOCKED` 或 `ABSTAIN` | 记录 readonly summary |
| unknown | `UNKNOWN` / `INTERNAL_FAIL_CLOSED` | 500 或 503，随合同冻结 | `ABSTAIN` | 记录 readonly summary |

Unknown error 必须 fail-closed，不得 fallback 到 directional bias。

## 7. Schema / envelope review

`dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 当前仍是 `DOC_ONLY_ALIAS` 或 future envelope planning。**本轮 review 允许它们在后续正式合同中转为 envelope 字段，但本轮不修改 schema/contracts/golden_cases**。

转正式字段前必须完成：

- DH JSON Schema / OpenAPI / Java DTO / domain mapper 影响评审。
- NQ request builder / recorder / client DTO 影响评审。
- existing mock fixtures compatibility review。
- golden_cases migration review；默认本轮不允许迁移。
- schema version upgrade policy；建议 future runtime envelope 使用 `X-NQ-DH-Schema-Version` header + body `schemaVersion` 双重一致性校验。

## 8. HMAC / timestamp / replay review

HMAC signature material 必须正式冻结后才能实现。建议 dry-run v1 候选 material：

```text
METHOD + "\n" +
PATH + "\n" +
source + "\n" +
tenantId + "\n" +
requestId + "\n" +
traceId + "\n" +
timestamp + "\n" +
nonce + "\n" +
schemaVersion + "\n" +
sha256(canonicalRequestBody)
```

约束：

- header name 不进入签名材料；签名基于 canonical values。
- method / path 进入 material，避免跨 endpoint replay。
- body hash 必须基于 canonical JSON。
- tenantId、requestId、traceId 的 header 与 body 必须一致；header 不能覆盖 body 权威值。
- nonce replay guard 必须持久化，并按 tenant/source/endpoint namespace 隔离。
- ±300s replay window 可沿用，但 runtime dry-run 要单独记录拒绝原因和 audit summary。

当前 timestamp / nonce / replay guard / tenant binding / requestId / traceId 规则**方向足够**，但还缺 runtime endpoint 专用合同冻结、nonce namespace、rate limit 和 audit 写入策略；这些是 implementation 前 blocker。

## 9. NQ no-side-effect runtime boundary

未来 NQ limited dry-run runtime client 只允许记录 DH readonly output，不得执行：

```text
order placement
order cancellation
risk mutation
ledger mutation
Paper Run start
LIVE
credential read
account secret read
NQ DB mutation based on DH output
```

`LONG_BIAS / SHORT_BIAS` 只能作为分析偏向，不能映射为：

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
order side
quantity
price
leverage
Paper Run input
LIVE execution input
```

Duplicate `requestId` 只能用于审计 / 幂等记录，不得触发交易或重复执行。NQ 不得把 DH output 当作自动交易信号；任何后续人工或策略流程都必须另走 NQ 自身风控、订单状态机和审批链。

## 10. Feature flag / kill switch / environment isolation

后续实现前必须冻结：

```text
DH dry-run API flag: default disabled
NQ dry-run client flag: default disabled
NQ outbound flag: default no-outbound
source allowlist kill switch: required
endpoint kill switch: required
client circuit breaker: required
timeout: short bounded timeout required
retry: default no transparent retry; any retry must preserve idempotency and use fresh nonce
payload size limit: required
response size limit: required
log redaction: required
profile isolation: dev/test/manual first; production disabled unless later approved
rollback: disable flags + remove source allowlist + revert endpoint/client PR independently
```

## 11. Audit / trace / replay review

允许保存：

- `decisionId`、`requestId`、`traceId`、`tenantId`、source、schemaVersion、safe subject summary。
- fail-closed reason、canonical error code、provider/policy/risk status。
- sanitized `traceSummary`、`auditRef`、`replayRef`。
- request / response hash 和安全字段白名单。

禁止保存：

- credential、API secret、passphrase、token、cookie、private key。
- raw prompt。
- raw provider response。
- full signed headers、signature、HMAC secret。
- NQ account secret、order/account private payload。

如果未来需要保存 raw payload，必须另起字段白名单、加密、retention 和 redaction review；本 review 不授权 raw prompt / raw provider response 入库。

## 12. Implementation slicing

后续必须拆成独立任务 / PR，不允许一个大实现任务吞掉所有内容：

```text
1. NQ-DH-I1-DH-RUNTIME-API-WO
   只写 DH work order，冻结 endpoint / request / response / error taxonomy / flags / rollback。

2. NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION
   仅在 WO 接受后实施；feature flag default disabled；mock-only / fail-closed；不得接 provider / Agent / LangGraph。

3. NQ-DH-I1-NQ-LIMITED-DRYRUN-CLIENT-WO
   只写 NQ client work order，冻结 no-side-effect / no-real-http / feature flag / recorder / idempotency。

4. NQ-DH-I1-NQ-LIMITED-DRYRUN-CLIENT-IMPLEMENTATION
   仅在 NQ WO 接受后实施；default no-outbound；不得触发 order / risk / ledger / Paper / LIVE。

5. NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TESTS
   只在两侧实现 review 后进入；覆盖 signed request、source deny、fail-closed、no-side-effect、redaction、kill switch。

6. NQ-DH-I1-RUNTIME-CLOSE-REVIEW
   只做验收和边界确认，不扩大到 real provider 或 LIVE。
```

## 13. Risks / blockers before implementation

- DH endpoint / Controller / OpenAPI path 仍不存在。
- NQ runtime client 仍不存在。
- `NQ_DRYRUN` production allowlist 未冻结。
- canonical error enum / error schema 未冻结。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 尚未进入正式 envelope。
- runtime HMAC material 尚未冻结。
- runtime-specific nonce persistence namespace、rate limit、payload cap、memory cap、kill switch、circuit breaker 尚未实现。
- audit / trace / replay 的 raw payload policy 尚未冻结。

这些 blocker 不阻止本 review 关闭，但阻止 runtime implementation now。

## 14. Boundary confirmation

```text
Production code changed: NO
Test code changed: NO
contracts changed: NO
golden_cases changed: NO
API / Controller added: NO
migration added: NO
runtime started: NO
real HTTP started: NO
real provider connected: NO
NQ DB accessed: NO
credential read/output: NO
Agent / LangGraph runtime started: NO
LIVE enabled: NO
NQ dev modified: NO
```

## 15. Next concrete action

```text
NEXT_ACTION: NQ-DH-I1-DH-RUNTIME-API-WO / NOT STARTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION
```

该下一步只允许写 work order，不允许直接新增 Controller、OpenAPI path、schema、contracts、golden_cases、runtime wiring、real HTTP、NQ client、provider、Agent / LangGraph 或 LIVE。
