# NQ-DH Integration-1 M1 DH Dry-run Contract Entry Mock Work Order（DH）

> Task: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO
> Status: COMPLETED / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NOT IMPLEMENTED
> Date: 2026-07-03
> Repository: Decision Hub
> Source of truth: docs/current

## 1. 目标与边界

本工单只规划 DH 侧 dry-run contract entry mock 的后续实现工作订单。目标是在不新增 runtime endpoint、不修改生产代码、不创建 fixture JSON、不改 `contracts/**` 或 `golden_cases/**` 的前提下，明确后续 DH 侧 test-support / mock-only entry 如何验证 contract validation chain、安全链路、`DecisionOrchestrator` 边界、audit / trace / replay 边界和 fail-closed 行为。

本工单不是 implementation，不授权代码、测试、Controller、API、OpenAPI、migration、schema、fixture、runtime、真实 HTTP、real provider、AI / Agent runtime、LangGraph runtime 或 LIVE。

## 2. 前置边界确认

```text
DH repository: F:\project\decision-hub
DH branch: dev
DH HEAD: a1fe4b50661a08c21b79be8597ac14d90968d11f
DH precheck: clean

NQ dry-run worktree: F:\worktrees\nexus-quant-i1-dryrun
NQ dry-run branch: nq-dh-i1-dryrun
NQ dry-run HEAD: 39b58d7f3a4594c8091765f1faf9b151f0a70d1f
NQ dry-run precheck: clean

NQ dev repository: F:\project\nexus-quant
NQ dev branch: dev
NQ dev HEAD: f69b5cc0e621ed77a50bbf5d5047219ca50888e9
NQ dev precheck: NQ_MAINLINE_DIRTY_ALLOWED
NQ dev dirty scope: non NQ-DH / non Integration-1 mainline changes only
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
```

NQ dev 本轮只读；未修改、未覆盖、未回滚任何 NQ dev 文件。

## 3. M1 总结论

```text
RECOMMENDED_ENTRY_SHAPE: Option C / test-support mock-only / no runtime endpoint
WHY_NO_RUNTIME_ENDPOINT_NOW: 当前 dry-run endpoint shape、NQ_DRYRUN source allowlist、canonical error taxonomy 与 schema alias 仍未通过实现级 review
REQUIRED_REVIEW_IF_API_NEEDED: API / contract / security review, source allowlist review, error taxonomy review, envelope / schema version review, no-side-effect review
PROHIBITED_CAPABILITIES: real HTTP, real provider, RealClient, NQ runtime, NQ DB, NQ mutation, order, cancel, Paper Run, AI / LangGraph runtime, LIVE
M1 close: YES
M2 work order allowed: YES, only as work-order-only
Implementation code allowed by M1: NO
```

M1 推荐后续 DH 侧 entry 形态仍为 test-support / mock-only。后续如果必须新增真实 API、Controller、OpenAPI path、schema header、runtime endpoint 或 HTTP 调用，必须单独进入 API / contract / security review；M1 不默认授权。

## 4. DH dry-run entry 形态规划

### RECOMMENDED_ENTRY_SHAPE

```text
Option C / test-support mock-only / no runtime endpoint
```

推荐理由：

- 能验证 dry-run contract validation chain，而不提前暴露 runtime HTTP surface。
- 能复用现有 `DecisionRequest` / `DecisionOutput` contract reality，避免把 `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 写成已实现 wire 字段。
- 能保持 `DecisionOrchestrator` 仍在 mock-only / read-only recommendation 范围内运行。
- 能让 NQ M2 只基于 M1 review 结果规划 stub recorder，不提前写 NQ runtime client。

### WHY_NO_RUNTIME_ENDPOINT_NOW

当前不新增 runtime endpoint 的原因：

- `source=NQ_DRYRUN` 仍为 `NEEDS_SECURITY_CONTRACT_CHANGE`，未进入已实现 allowlist。
- canonical error code names 尚未全部实现；M1 只能规划 error normalization。
- `POST /api/ai/feedback/nq` 是 feedback ingest，不是 decision dry-run endpoint，不能复用为 dry-run decision entry。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 仍是 `DOC_ONLY_ALIAS` 或 future envelope planning。
- runtime integration、真实 HTTP、real provider、AI / Agent runtime、LangGraph runtime 和 LIVE 均未开始。

### REQUIRED_REVIEW_IF_API_NEEDED

若后续确实需要真实 API / Controller，必须先完成独立 review：

```text
API path / Controller / OpenAPI review
request / response envelope review
source allowlist security review
canonical error taxonomy review
schema version header review
HMAC signatureMaterial and header binding review
no-outbound / no-side-effect / fail-closed review
rate limit / replay / audit persistence review
NQ consuming side no-trading review
```

### PROHIBITED_CAPABILITIES

```text
real HTTP
real provider
RealClient
NQ runtime connection
NQ DB read/write
NQ mutation
order placement
order cancellation
position / account / ledger mutation
Paper Run start
credential / token / API secret / passphrase access
AI / Agent runtime
LangGraph runtime
LIVE
```

## 5. Contract validation chain 规划

未来 DH 侧 mock/test-support entry 必须按以下顺序验证。任一失败都必须 fail-closed，并输出结构化错误；unknown error 不能升级为 `LONG_BIAS` / `SHORT_BIAS`。

| Step | Gate | M1 规划要求 | Fail-closed 结果 |
| --- | --- | --- | --- |
| 1 | payload size gate | 请求体先过大小上限，不能边解析边扩大内存。 | `PAYLOAD_TOO_LARGE` 或既有等价码。 |
| 2 | canonical header presence | 必须存在 canonical `X-NQ-DH-*` header；legacy header 不接受。 | missing header fail-closed。 |
| 3 | requestId / traceId / tenantId binding | body 权威字段与 header / auth context 绑定；header 不覆盖 body。 | `TENANT_MISMATCH` / `HEADER_BINDING_MISMATCH` 等价 fail-closed。 |
| 4 | source allowlist | `NQ_DRYRUN` 当前不能写成已实现 source；未 review 前只作为 future source plan。 | `SOURCE_DENIED` fail-closed。 |
| 5 | timestamp RFC3339 UTC Z validation | 只接受 RFC3339 / ISO-8601 UTC `Z`，拒绝 epoch seconds、epoch milliseconds、数字时区偏移。 | timestamp invalid / skew fail-closed。 |
| 6 | nonce replay fail-closed | nonce 必须有防重放；重复 nonce 不进入 orchestrator。 | `NONCE_REPLAY` 或既有等价码。 |
| 7 | HMAC value-based signatureMaterial validation | HMAC 只对 value-based signature material 校验；header name 不入签。 | `SIGNATURE_INVALID` fail-closed。 |
| 8 | schema / contract shape validation | 只验证现有 `DecisionRequest` / `DecisionOutput` contract reality，不新增 schema 字段。 | `CONTRACT_INVALID` 或既有等价码。 |
| 9 | forbidden fields validation | 禁止 account、credential、side、price、quantity、leverage、order、private key 等字段。 | `FORBIDDEN_FIELD` fail-closed。 |
| 10 | DecisionOrchestrator mock-only boundary | 仅允许 mock-only `DecisionOrchestrator`，不接 real provider。 | provider disabled / internal fail-closed。 |
| 11 | provider guard mock-only boundary | provider guard 必须继续拒绝 real provider / SUCCESS 假象 / outbound token。 | `PROVIDER_DISABLED` / `PROVIDER_BUDGET_EXCEEDED` / `PROVIDER_TIMEOUT`。 |
| 12 | audit / trace / replay write boundary | 只写 DH-owned 安全摘要；写失败必须 fail-closed，除非未来 review 接受异步可追踪策略。 | `INTERNAL_FAIL_CLOSED` 或既有等价码。 |
| 13 | structured DecisionOutput assembly | 只能组装 `READ_ONLY_RECOMMENDATION`；action 只能在白名单内。 | invalid output fail-closed。 |
| 14 | fail-closed response normalization | 所有失败统一为结构化 response，不暴露内部栈、SQL、credential path、raw secret。 | normalized fail-closed response。 |

补充约束：

- `LONG_BIAS` / `SHORT_BIAS` 仍是只读倾向，不是 `BUY` / `SELL`。
- `BUY` / `SELL` / `PLACE_ORDER` / `CANCEL_ORDER` 不得进入 `DecisionOutput.action`。
- `forbiddenActions` 必须包含 `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。
- NQ 接收任何失败只能记录，不触发交易、风控旁路、Paper Run 或状态修改。
- DH 不得读取 NQ DB，不得修改 NQ 状态。

## 6. Source allowlist M1 边界

```text
NQ_DRYRUN source allowlist: NOT IMPLEMENTED / NEEDS_SECURITY_CONTRACT_CHANGE
Current M1 handling: future source plan only
SOURCE_DENIED: fail-closed
source bypass in tests: prohibited
```

M1 不把 `NQ_DRYRUN` 写成已实现 allowlist。如果后续 implementation 需要 `source=NQ_DRYRUN`，必须先执行 security contract review，明确：

- source 值、tenant、requestId、traceId 的绑定规则。
- source allowlist 存储位置和测试是否可绕过。
- 未授权 source 的错误码与 response normalization。
- source 是否只限 test-support / mock-only profile。
- 真实 runtime / real HTTP / provider / LIVE 仍保持禁止。

在 review 通过前，`NQ_DRYRUN` 只能作为 future source plan；任何使用它的 mock 设计必须显式标注未实现。

## 7. Error taxonomy M1 边界

M1 只能规划 error normalization，不新增 enum、schema、code 或 fixture。

| Error group | M1 decision |
| --- | --- |
| 已有或可映射语义 | `SIGNATURE_INVALID`、`PAYLOAD_TOO_LARGE`、`RATE_LIMITED`、`FORBIDDEN_FIELD`、`TENANT_MISMATCH`、`PROVIDER_DISABLED`、`PROVIDER_TIMEOUT`、`PROVIDER_BUDGET_EXCEEDED`、`RISK_BLOCKED`。 |
| 文档映射语义 | `TIMESTAMP_SKEW`、`NONCE_REPLAY`、`SOURCE_DENIED` 仍可能映射到现有命名，不在 M1 重命名。 |
| 需独立 review | `AUTH_FAILED`、`CONTRACT_INVALID`、`INTERNAL_FAIL_CLOSED`。 |

约束：

- canonical error code names 尚未全部实现。
- 后续如果需要统一 error enum，必须独立 contract review。
- 所有错误路径必须 fail-closed。
- NQ 不得根据 error response 执行交易或继续策略状态变更。
- 外部原始错误、内部异常栈、SQL、包名、credential path、raw provider response 不得出现在 response 或日志摘要中。

## 8. Audit / trace / replay 边界规划

### ID 关系

```text
requestId: NQ 发起的 dry-run 请求唯一标识；在 DH validation chain 内必须保持不变。
traceId: 跨验证链路的追踪标识；用于日志、audit、trace correlation。
tenantId: 权限与隔离边界；来自 body / auth context / canonical header binding 的一致性校验。
decisionId-like reference: 当前只能作为 DH 内部 persistence / replay 概念或 DOC_ONLY_ALIAS，不是 wire required field。
```

### 当前 alias 状态

```text
decisionId: DOC_ONLY_ALIAS unless future schema review accepts wire field
replayRef: DOC_ONLY_ALIAS unless future schema / replay API review accepts wire field
auditRef: DOC_ONLY_ALIAS unless future audit / envelope review accepts wire field
traceSummary: DOC_ONLY_ALIAS / future envelope planning
X-NQ-DH-Schema-Version: DOC_ONLY_ALIAS / future header review
```

如果当前 schema / domain 不支持 `replayRef` / `auditRef` wire field，不得写入 fixture required field，不得写入 OpenAPI，不得要求 NQ M2 按已实现字段消费。

### 安全摘要要求

- audit / trace / replay 只能记录安全摘要，不记录 credential、raw secret、private key、signature raw string、cookie、token、API secret、passphrase。
- provider raw output 如有，必须按既有脱敏策略处理；M1 不接 real provider。
- M1 不新增 migration，不改 replay API，不新增 replay endpoint。
- audit write failure 在当前规则下必须 fail-closed；如未来要异步化，必须单独 review。

## 9. M1 后续批次准入

```text
ALLOW_M1_WO_CLOSE: YES
ALLOW_I1_M2_NQ_DRYRUN_STUB_RECORDER_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

M2 只能是：

```text
NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO
```

M2 仍必须是 work-order-only，不允许直接 implementation code。如果 source allowlist、endpoint shape 或 error taxonomy 在 M2 前仍阻断 mock/test-support 设计，M2 不得开始，必须回到 M1 fix。

M2 必须在 `F:\worktrees\nexus-quant-i1-dryrun` / `nq-dh-i1-dryrun` 执行；不得在 `F:\project\nexus-quant` dev 主线执行正向改动。

## 10. 后续 M1 implementation review 要求

如果未来用户单独授权进入 M1 implementation review，默认候选范围仅限 test-support / mock-only：

```text
dh-domain/src/test/**
dh-usecase/src/test/**
dh-api/src/test/**
docs/current/**
```

默认仍禁止：

```text
dh-api/src/main/**
dh-app/src/main/**
dh-connector/src/main/**
dh-infra/src/main/**
contracts/**
golden_cases/**
fixture JSON
migration
OpenAPI
production Controller
runtime endpoint
```

后续 M1 implementation review 至少必须覆盖：

- 正常路径：valid mock dry-run request -> structured `DecisionOutput`。
- 失败路径：missing canonical header、bad timestamp、nonce replay、bad HMAC、disallowed source、oversized payload、forbidden field、schema invalid。
- 边界值：payload size 上限、timestamp window 边界、empty evidence、unknown provider status。
- 安全路径：no real HTTP、no RealClient、no provider、no credential access、no NQ DB。
- fail-closed：unknown error 不得输出 directional bias；audit / trace / replay failure 不得被吞。

## 11. 测试要求

本工单本轮验证命令：

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases
mvn -ntp test
mvn -ntp -Pquality validate
```

未来 M1 implementation review 的测试要求：

```powershell
mvn -ntp -pl dh-domain,dh-usecase,dh-api -am "-Dtest=*DryRun*,*Contract*,*Security*" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -ntp test
mvn -ntp -Pquality validate
```

如命令失败，必须记录真实错误，区分环境失败与代码失败，不得伪造成成功。

## 12. 回滚要求

本轮 docs-only 回滚：

```text
revert docs/current/DH_NQ_INTEGRATION1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO.md
revert 本轮同步的 docs/current 状态文档
rerun git diff --check
rerun forbidden-scope diff
```

未来 implementation 回滚必须按实际变更文件回滚，并重新运行对应 Maven / forbidden-scope diff。

## 13. 验收标准

```text
M1 work order created and indexed
RECOMMENDED_ENTRY_SHAPE recorded
WHY_NO_RUNTIME_ENDPOINT_NOW recorded
REQUIRED_REVIEW_IF_API_NEEDED recorded
PROHIBITED_CAPABILITIES recorded
contract validation chain recorded in order
source allowlist M1 boundary recorded
error taxonomy M1 boundary recorded
audit / trace / replay boundary recorded
M2 work-order-only readiness decision recorded
DH current docs synchronized
NQ worktree impact document synchronized
NQ dev remains read-only
forbidden-scope diff remains empty
no production code / test code / contracts / golden_cases / API / migration changes
```

## 14. 下一步

```text
NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO / COMPLETED / WORK_ORDER_ONLY / NQ_DRYRUN_STUB_RECORDER_PLANNED / NOT IMPLEMENTED
```

M2 已完成 NQ dry-run stub / recorder work order planning；未进入 NQ backend implementation。当前下一步只能输出 `NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / NOT STARTED / WORK_ORDER_ONLY`；不得直接创建 fixture JSON、测试代码、schema/contracts/golden_cases、API、Controller、runtime、真实 HTTP、real provider 或 LIVE。
