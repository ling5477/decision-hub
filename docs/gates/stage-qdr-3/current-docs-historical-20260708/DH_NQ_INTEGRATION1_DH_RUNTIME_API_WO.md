# DH-NQ Integration-1 DH Runtime API Work Order

> 任务：`NQ-DH-I1-DH-RUNTIME-API-WO`
> 类型：`WORK_ORDER_ONLY + DH_SCOPED_RUNTIME_API_IMPLEMENTATION_PLAN + SECURITY_BOUNDARY_FREEZE + NO_RUNTIME_IMPLEMENTATION + NO_LIVE`
> 日期：2026-07-04
> 仓库视角：Decision Hub（DH）
> 状态：`CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION`

## 1. 目标

本 work order 只冻结下一轮 DH limited dry-run runtime endpoint implementation 的边界、文件范围、安全门槛、验收标准、禁止项、测试要求和回滚要求。本轮不实现 runtime，不新增 Controller，不修改 OpenAPI、JSON Schema、contracts、golden_cases 或 fixture JSON。

当前结论：

```text
Future endpoint: POST /api/ai/decision-dry-runs
Endpoint state now: NOT IMPLEMENTED
NQ_DRYRUN source: REVIEW_GATED / NOT_IN_PRODUCTION_ALLOWLIST
Runtime implementation: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NO
Real provider: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
```

## 2. 本轮范围

本轮允许修改：

```text
docs/current/DH_NQ_INTEGRATION1_DH_RUNTIME_API_WO.md
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

本轮只读：

```text
dh-api/**
dh-usecase/**
dh-security/**
dh-infra/**
dh-domain/**
contracts/**
golden_cases/**
E:/Project/nexus-quant/docs/current/**
E:/Project/nexus-quant/backend/**
E:/Project/nexus-quant-i1-dryrun/docs/current/**
E:/Project/nexus-quant-i1-dryrun/backend/**
```

本轮禁止：

```text
DH Java production code change
DH Java test code change
Controller / API implementation
Client / Service / Repository implementation
migration / schema change
contracts/json-schema change
golden_cases change
fixture JSON change
real HTTP
real NQ / DH runtime call
real provider
AI / Agent runtime
LangGraph runtime
credential read
NQ dev modification
NQ worktree modification
LIVE
```

## 3. 下一轮 implementation 文件范围

下一轮 `NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION` 只有在本 work order 被关闭、且用户明确开启实现任务后，才允许按最小实现原则触达以下范围：

```text
dh-api/src/main/**        endpoint / DTO / request guard / response envelope
dh-api/src/test/**        WebMvc / security / fail-closed tests
dh-app/src/main/**        feature flag default disabled / kill switch wiring
dh-app/src/test/**        wiring and disabled-by-default tests
dh-usecase/src/main/**    调用既有 DecisionOrchestrator / readonly decision path only
dh-usecase/src/test/**    usecase fail-closed and mapper tests
dh-security/src/main/**   复用或最小封装 HMAC / timestamp / nonce / allowlist / rate-limit guard
dh-security/src/test/**   guard contract tests
dh-infra/src/main/**      仅允许复用既有 audit / nonce persistence wiring；不得新增 migration
docs/current/**           实现后状态同步
```

下一轮仍默认禁止，除非另起 contract/schema/database review 并获得明确授权：

```text
contracts/**
golden_cases/**
src/main/resources/db/migration/**
OpenAPI path change
provider / Agent / LangGraph / real HTTP / NQ client
```

## 4. Endpoint 边界

Future endpoint：

```text
POST /api/ai/decision-dry-runs
```

规则：

- 当前状态是 `NOT IMPLEMENTED`。
- 下一轮 implementation 才允许新增 endpoint。
- endpoint 只服务 limited dry-run。
- endpoint 必须 feature flag disabled by default。
- endpoint 不得触发交易、订单、账户、ledger、NQ 状态变更。
- endpoint 不得调用真实 provider。
- endpoint 不得接 Agent / LangGraph。
- endpoint 不得读取 NQ DB，不得写 NQ DB。
- endpoint 不得返回 `BUY`、`SELL`、`PLACE_ORDER`、`CANCEL_ORDER` 或任何可执行交易指令。

## 5. Security gate

下一轮实现必须强制以下 gate，任一关键 gate 缺失均不得进入 implementation close：

```text
HMAC signature verification
RFC3339 / ISO-8601 UTC Z timestamp
±300s replay window
persistent nonce replay guard
tenant/source allowlist
requestId / traceId / tenantId binding
payload size cap
rate limit
memory cap
fail-closed
redacted audit logging
feature flag disabled by default
kill switch
```

安全规则：

- canonical header 使用 `X-NQ-DH-*`，不得重新引入 legacy header fallback。
- `timestamp` 必须以 `Z` 结尾；epoch seconds、epoch milliseconds、数字时区偏移必须拒绝。
- header 中的 `tenantId / requestId / traceId` 只能做一致性校验，不能覆盖 body 权威值。
- nonce namespace 必须至少绑定 `tenantId + source + endpoint + nonce`。
- rate limit 必须按 `tenant + source + route` 或更严格维度执行。
- payload size cap 默认不得高于既有 64 KiB 策略，除非后续 contract review 明确调整。
- audit 写失败必须 fail-closed。

## 6. Source allowlist

`NQ_DRYRUN` 当前仍为 review-gated source：

```text
NQ_DRYRUN production allowlist: NO
Current allowed state: dev/test review-gated only
Production default: DISABLED
```

下一轮如需允许 `NQ_DRYRUN`，必须满足：

- 仅限 dev/test profile 首批启用。
- 必须要求 tenant + source pair allowlist。
- production disabled unless later approved。
- source allowlist 必须可由 kill switch 立即关闭。
- source denied 必须返回 `SOURCE_DENIED` 或对应 error envelope，并 fail-closed。

## 7. Request envelope

下一轮 implementation 必须冻结 request envelope，至少包含：

```text
requestId
traceId
tenantId
source
timestamp
nonce
schemaVersion
dryRun = true
decisionContext
forbiddenCapabilities
```

不得允许：

```text
credential
apiKey
apiSecret
passphrase
accountSecret
executableOrder
BUY / SELL order instruction
quantity / leverage / order price as executable instruction
```

额外约束：

- `dryRun` 必须为 `true`；缺失或 false 必须拒绝。
- `forbiddenCapabilities` 必须覆盖 `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。
- `decisionContext` 只能是 read-only、脱敏、可审计上下文，不得包含原始 credential、raw prompt 或 raw provider response。
- schema version 必须同时校验 header `X-NQ-DH-Schema-Version` 与 body `schemaVersion` 的一致性；如本轮不实现 header，则不得声称 schema version 已 production-ready。

## 8. Response envelope

下一轮 implementation 必须冻结 response envelope，至少包含：

```text
decisionId
dryRun = true
action = OBSERVE | NO_TRADE | LONG_BIAS | SHORT_BIAS
confidence
riskLevel
reasons
traceSummary
replayRef
auditRef
error envelope
```

响应规则：

- `LONG_BIAS / SHORT_BIAS` 只是 bias，不是 `BUY / SELL`。
- response 只能是 read-only decision snapshot。
- NQ 只能记录，不执行。
- security / policy / audit failure 必须走 fail-closed error envelope，不得输出可执行交易建议。
- 如果复用既有 domain `ABSTAIN`，必须在 implementation 前做 mapper compatibility review；本 work order 不授权本轮 schema/code change。

## 9. Error taxonomy

下一轮 implementation 必须覆盖以下 canonical error taxonomy：

```text
SIGNATURE_INVALID
TIMESTAMP_INVALID
TIMESTAMP_OUT_OF_WINDOW
NONCE_REPLAY
TENANT_MISMATCH
SOURCE_DENIED
PAYLOAD_TOO_LARGE
RATE_LIMITED
MEMORY_LIMIT_EXCEEDED
POLICY_DENIED
PROVIDER_DISABLED
PROVIDER_TIMEOUT
BUDGET_EXCEEDED
UNKNOWN_ERROR
```

规则：

- unknown error 必须 fail-closed。
- security error 不得 fallback 放行。
- provider unavailable 不得转为成功。
- policy denied 不得输出可执行交易建议。
- error response 必须 redacted，不得输出 signature、secret、credential、raw prompt、raw provider response 或敏感 header/body。
- 如果既有 domain/error enum 与本 taxonomy 不一致，必须在 implementation 中显式 mapper，并由测试覆盖；不得隐式混用旧名称。

## 10. Audit / trace / replay

下一轮 implementation 必须要求：

```text
写入 decision snapshot
写入 step-level trace summary
写入 audit event
支持 replayRef / auditRef
不保存 raw credential
不保存 raw prompt
不保存 provider raw response
日志必须 redacted
audit 写失败必须 fail-closed
```

允许保存：

- `decisionId / requestId / traceId / tenantId / source / schemaVersion`。
- request / response hash。
- safe subject summary。
- fail-closed reason。
- canonical error code。
- provider / policy / risk status summary。

禁止保存：

- credential、API secret、passphrase、token、cookie、private key。
- raw prompt。
- raw provider response。
- full signed headers、signature、HMAC secret。
- NQ account secret、order/account private payload。

## 11. Feature flag / kill switch

下一轮 implementation 必须要求：

```text
default disabled
dev/test only
production disabled
emergency kill switch
timeout
retry limit
circuit breaker
no-real-provider guard
no-live guard
```

执行规则：

- 无配置时必须 disabled / fail-closed。
- timeout 必须短且有上限。
- 默认不得透明 retry；如需 retry，必须保留幂等语义并使用新的 nonce 策略或明确禁止 retry。
- circuit breaker 打开时必须返回 fail-closed error envelope。
- rollback 必须能通过 flag / kill switch 独立关闭 DH endpoint，不要求回滚 mock/test-support baseline。

## 12. Implementation slicing

后续必须拆分为独立任务，不得把 DH endpoint 和 NQ client 合并为一个大实现任务：

```text
1. DH limited runtime endpoint implementation
2. DH runtime endpoint tests
3. NQ limited dry-run client work order
4. NQ limited dry-run client implementation
5. joint runtime dry-run tests
6. runtime close review
```

推荐路线：

```text
NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION
NQ-DH-I1-DH-RUNTIME-ENDPOINT-TESTS
NQ-DH-I1-NQ-LIMITED-DRYRUN-CLIENT-WO
NQ-DH-I1-NQ-LIMITED-DRYRUN-CLIENT-IMPLEMENTATION
NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TESTS
NQ-DH-I1-RUNTIME-CLOSE-REVIEW
```

## 13. Validation commands

DH：

```powershell
Set-Location -LiteralPath 'E:\Project\decision-hub'

git status --short
git diff --check
git diff --stat

git diff --name-only -- `
  dh-domain/src/main `
  dh-usecase/src/main `
  dh-memory/src/main `
  dh-eval/src/main `
  dh-connector/src/main `
  dh-api/src/main `
  dh-app/src/main `
  dh-infra/src/main `
  contracts `
  golden_cases

mvn -ntp -Pquality validate
```

NQ dev，只读确认：

```powershell
Set-Location -LiteralPath 'E:\Project\nexus-quant'

git status --short
git diff --stat
git diff --name-only -- docs/current/*NQ_DH* docs/current/*INTEGRATION1*
git diff --name-only --cached -- docs/current/*NQ_DH* docs/current/*INTEGRATION1*
```

NQ worktree，只读确认：

```powershell
Set-Location -LiteralPath 'E:\Project\nexus-quant-i1-dryrun'

git status --short
git branch --show-current
git diff --stat
```

## 14. 回滚要求

本轮 docs-only 回滚：

```powershell
Set-Location -LiteralPath 'E:\Project\decision-hub'
git restore -- docs/current/API.md docs/current/DH_NQ_INTEGRATION.md docs/current/README.md docs/current/ROADMAP.md docs/current/STATUS.md docs/current/WORK_ORDER.md docs/current/TESTING.md docs/current/WORKLOG.md
Remove-Item -LiteralPath 'docs/current/DH_NQ_INTEGRATION1_DH_RUNTIME_API_WO.md' -Force
```

下一轮 implementation 回滚必须独立满足：

- 关闭 endpoint feature flag。
- 关闭 source allowlist / kill switch。
- 回滚 endpoint/controller/DTO/wiring commit。
- 保留 mock/test-support baseline，不得误删历史已接受证据。
- NQ client work order / implementation 必须单独回滚，不得与 DH endpoint 混在一个不可审查变更中。

## 15. Readiness decision

```text
ALLOW_DH_RUNTIME_API_WO_CLOSE: YES
ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_IMPLEMENTATION_WO: YES
ALLOW_DH_RUNTIME_IMPLEMENTATION_NOW: NO
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 16. Boundary confirmation

```text
Production code changed: NO
Test code changed: NO
API / Controller added: NO
Client added: NO
Service / Repository added: NO
Migration added: NO
contracts changed: NO
golden_cases changed: NO
fixture JSON changed: NO
runtime started: NO
real HTTP started: NO
real provider connected: NO
NQ DB accessed: NO
credential read/output: NO
Agent / LangGraph runtime started: NO
LIVE enabled: NO
NQ dev modified: NO
NQ worktree modified: NO
```

## 17. Next concrete action

如果本 work order 关闭，下一步为：

```text
NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / FEATURE_FLAG_DISABLED_BY_DEFAULT / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

如果验证发现阻断项，下一步为：

```text
NQ-DH-I1-DH-RUNTIME-API-WO-BLOCKER-FIX
```
