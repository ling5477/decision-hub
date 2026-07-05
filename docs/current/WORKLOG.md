# Decision Hub Worklog

## 2026-07-04 NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW

完成 `NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW`。本轮为 `REVIEW_ONLY + DH_RUNTIME_ENDPOINT_SECURITY_REVIEW + API_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_NQ_CHANGE + NO_LIVE`，只审查 DH limited dry-run inbound endpoint `POST /api/ai/decision-dry-runs` 是否可以关闭，并判断是否允许进入下一步 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO`。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DH_ENDPOINT_CLOSE_REVIEW.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### Review result

```text
NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW: CLOSED / ACCEPTED / REVIEW_ONLY
Endpoint: POST /api/ai/decision-dry-runs
Endpoint boundary: DH_ONLY_INBOUND_LIMITED_DRY_RUN
Runtime integration: NOT STARTED
NQ runtime client: NOT STARTED
Real HTTP outbound: NO
Real provider: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_CLOSE: YES
ALLOW_NQ_RUNTIME_CLIENT_WO: YES
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### Findings

- API boundary：`DecisionDryRunController` 只注册 DH inbound endpoint；未发现 NQ runtime client、RealClient、真实 HTTP outbound、real provider、Agent / LangGraph runtime、LIVE 或 NQ mutation。
- Security gate：feature flag 默认关闭；production disabled；kill switch fail-closed；HMAC、UTC `Z` timestamp、±300s window、nonce replay、tenant/source pair、payload cap、rate limit、memory cap、forbidden field/capability 均有实现与测试覆盖。
- Error taxonomy：`SIGNATURE_INVALID / TIMESTAMP_INVALID / TIMESTAMP_OUT_OF_WINDOW / NONCE_REPLAY / TENANT_MISMATCH / SOURCE_DENIED / PAYLOAD_TOO_LARGE / RATE_LIMITED / MEMORY_LIMIT_EXCEEDED / POLICY_DENIED / PROVIDER_DISABLED / PROVIDER_TIMEOUT / BUDGET_EXCEEDED / UNKNOWN_ERROR` 为 endpoint-local stable taxonomy；security/provider/policy/unknown/audit failure 均 fail-closed。
- Audit / trace / replay：成功与拒绝均写 DH-owned redacted audit；audit 写失败 fail-closed；不保存 raw credential、raw prompt、provider raw response、raw payload、token、cookie 或 API secret。
- Test review：Controller/usecase/security/contract guard 覆盖 valid dry-run、feature disabled、signature、timestamp、nonce replay、source/tenant、dryRun=false、forbidden execution material、payload/rate/memory limit、audit failure、provider disabled/timeout/budget、ABSTAIN mapping 与 no outbound/no provider/no LIVE guard。

### Validation

- `git status --short`：PASS / docs-current only；新增 close-review 文档。
- `git diff --check`：PASS；仅 LF/CRLF warning；无 whitespace error。
- `git diff --name-only -- contracts golden_cases "dh-*/src/main/resources/db/migration"`：PASS / EMPTY。
- boundary `rg`：PASS / REVIEWED；命中分类为 endpoint token、dev/test `NQ_DRYRUN` 配置、测试负向断言、既有 denylist、文档禁令、migration 注释、fixture / golden case 安全样例或 historical contract；未发现本轮新增真实 outbound/NQ client/provider/LIVE。
- `mvn -ntp -pl dh-api -am test`：PASS / BUILD SUCCESS；dh-api 53 tests；`DecisionDryRunControllerWebMvcTest` 11 tests。
- `mvn -ntp -pl dh-usecase -am test`：PASS / BUILD SUCCESS；dh-usecase 179 tests；`DefaultDecisionDryRunServiceTest` 8 tests。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；19 reactor module SUCCESS；Checkstyle 0 violations；Spotless check passed。
- `mvn -ntp test`：PASS / BUILD SUCCESS；`PostgresContainerSmokeTest` 因 Docker named pipe `\\.\pipe\docker_engine` access denied 被测试自身 skip 1 项；该结果不是 Docker/Testcontainers PASS。
- NQ dev 只读：存在非本轮 dirty / untracked，但 NQ-DH / Integration-1 scoped unstaged 与 staged diff 均为空；本轮未写入。
- NQ dry-run worktree 只读：branch=`nq-dh-i1-runtime-api-contract-review`；status clean；diff stat empty。

### 边界确认

未改 Java 生产代码；未改测试代码；未改 NQ dev；未改 NQ dry-run worktree；未改 `contracts/openapi.yaml`、`contracts/json-schema/**`、`golden_cases/**`、fixture JSON 或 migration；未新增真实 outbound HTTP、NQ runtime client、RealClient、real provider、Agent / LangGraph runtime 或 LIVE；未读取 credential、token、cookie、API secret 或 passphrase；未写 order / execution / ledger / account / trading / live；未把 Runtime integration 写成 started；未把 DH 写成 integrated。

### 下一步

进入 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO / NOT STARTED / WORK_ORDER_ONLY / NO_IMPLEMENTATION / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE`。该下一步只允许写工单，不允许直接实现 NQ client、真实 HTTP、real provider、schema/contracts/golden_cases 修改、Agent / LangGraph runtime 或 LIVE。

## 2026-07-04 NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION

完成 `NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION` 的 DH-only limited inbound endpoint 最小闭环。Endpoint 为 `POST /api/ai/decision-dry-runs`，默认关闭，仅 dev/test profile 可显式启用；production profile disabled，kill switch fail-closed。本轮不实现 NQ runtime client、不调用 NQ、不新增真实 outbound HTTP、不接 real provider、不接 Agent / LangGraph runtime、不启用 LIVE。

### 新增文件

```text
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunSuccessResponse.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunErrorResponse.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/main/resources/application-test.yml
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthResult.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/*
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/*
```

### 修改文件

```text
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java
dh-app/src/main/resources/application.yml
dh-app/src/main/resources/application-dev.yml
dh-app/src/main/resources/application-prod.yml
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhDryRunTestSupportEntryTest.java
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 结果

```text
NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION: IMPLEMENTED / PENDING_CLOSE_REVIEW / DH_ONLY
Endpoint: POST /api/ai/decision-dry-runs
Feature flag: default disabled / dev-test explicit enable only / production disabled
NQ_DRYRUN source: dev-test tenant/source allowlist only / not in production allowlist
Runtime integration: NOT STARTED
NQ runtime client: NOT STARTED
Real HTTP outbound: NO
Real provider: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
NEXT_ACTION: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW
```

### Implementation

- `DecisionDryRunController` 新增 DH inbound endpoint，读取 raw body 用于 HMAC body hash，走 canonical `X-NQ-DH-*` header、API tenant context、rate limit、HMAC dry-run authenticator 与 usecase。payload cap 在 JSON 解析前 fail-closed，避免超大 payload 被误映射成普通 policy error。
- `HmacNqDryRunAuthenticator` 校验 method/path/source/tenant/requestId/traceId/timestamp/nonce/schemaVersion/bodySha256 签名材料，强制 secret、tenant/source pair、UTC `Z` timestamp、±300s window、payload cap、signature 和 nonce replay guard。
- `DefaultDecisionDryRunService` 负责 feature flag、production disabled、kill switch、dryRun=true、必填 envelope、forbidden material、forbiddenCapabilities、memory cap、policy gate、audit / trace / replay 写入和 fail-closed normalization。
- response 只返回 read-only snapshot，action 仅允许 `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；内部 `ABSTAIN` 对外映射为 `NO_TRADE`，reason 记录 `INTERNAL_ABSTAIN_MAPPED`。
- `application.yml` 默认 disabled；`application-dev.yml` 与 `application-test.yml` 可显式启用 `NQ_DRYRUN`；`application-prod.yml` 明确 disabled、kill switch true、allowed sources empty。
- `DhApiAuthenticationFilter` 将 `/api/ai/decision-dry-runs` 纳入 DH API bearer 认证保护。

### Tests

- `HmacNqDryRunAuthenticatorTest` 覆盖 valid signature、invalid timestamp、out-of-window、replay nonce、tenant/source denial 和 payload too large。
- `DefaultDecisionDryRunServiceTest` 覆盖 feature flag disabled、valid dry-run、dryRun=false、forbidden execution material、memory limit、audit failure、provider disabled / timeout / budget fail-closed 和 ABSTAIN mapping。
- `DecisionDryRunControllerWebMvcTest` 覆盖 API auth、valid signed request、feature flag disabled、missing/invalid signature、timestamp shape、timestamp window、nonce replay、source denied、tenant mismatch、dryRun=false、forbidden executable material、payload too large、rate limited 和 audit write failure。
- `DecisionContractGapGuardTest` 与 `DhDryRunTestSupportEntryTest` 已更新为允许本轮 limited endpoint，同时继续阻断 contracts/schema/golden_cases、NQ runtime client、real provider、真实 HTTP、LIVE 和 executable trading tokens。

### 验证

- `mvn -ntp -pl dh-api -am test`：PASS / BUILD SUCCESS；`DecisionDryRunControllerWebMvcTest` 11 tests；dh-api 53 tests，0 failures，0 errors。
- `mvn -ntp -pl dh-usecase -am test`：PASS / BUILD SUCCESS；dh-usecase 179 tests，0 failures，0 errors。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；`PostgresContainerSmokeTest` 因 Docker named pipe access denied 被测试自身 skip 1 项，非本轮代码失败。
- `git diff --check`：PASS；仅 Windows LF/CRLF warning；无 whitespace error。
- forbidden diff：`contracts`、`golden_cases`、`dh-*/src/main/resources/db/migration` 均为空。
- boundary `rg`：已用 PowerShell 原生展开 `dh-*` 后重跑；命中分类为 endpoint token、dev/test `NQ_DRYRUN` 配置、测试负向断言、既有安全 denylist、文档禁令、migration 注释或 historical contract；未发现 NQ client、真实 HTTP、real provider、Agent/LangGraph runtime、LIVE 或可执行订单实现。
- NQ dev 只读：存在 unrelated untracked trading preflight 文件；NQ-DH / Integration-1 scoped unstaged 与 staged diff 均为空。
- NQ dry-run worktree 只读：branch=`nq-dh-i1-runtime-api-contract-review`；status clean；diff stat empty。

### 边界确认

未修改 NQ dev；未修改 NQ dry-run worktree；未修改 `contracts/openapi.yaml`、`contracts/json-schema/**`、`golden_cases/**`、fixture JSON 或 migration；未新增 NQ runtime client、RealClient、真实 outbound HTTP client、real provider、Agent / LangGraph runtime 或 LIVE；未读取或输出 credential、token、cookie、API secret、passphrase；未写 order / execution / ledger / account / trading 状态；未输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 或 executable order instruction；未把 DH 写成 integrated；未把 Runtime integration 写成 started。

### Readiness

- `ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_IMPLEMENTATION_CLOSE: YES`
- `ALLOW_DH_ENDPOINT_CLOSE_REVIEW: YES`
- `ALLOW_NQ_RUNTIME_CLIENT_WO: NO`
- `ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO`
- `ALLOW_REAL_HTTP: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_SCHEMA_CHANGE_NOW: NO`
- `ALLOW_CONTRACTS_MODIFICATION_NOW: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 下一步

进入 `NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW / NOT STARTED / REVIEW_ONLY / NO_NQ_RUNTIME_CLIENT / NO_REAL_PROVIDER / NO_LIVE`；不得直接进入 NQ runtime client WO、NQ runtime client implementation、schema/contracts/golden_cases 修改、真实 HTTP、real provider、Agent / LangGraph runtime 或 LIVE。

## 2026-07-04 NQ-DH-I1-DH-RUNTIME-API-WO

完成 `NQ-DH-I1-DH-RUNTIME-API-WO` 的 work-order-only 收口。本轮只写 DH scoped limited dry-run runtime API implementation work order，不实现 runtime，不新增 API / Controller，不修改 production code、test code、contracts、golden_cases、fixture JSON、migration、OpenAPI、Client、Service、Repository 或 runtime wiring。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DH_RUNTIME_API_WO.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-DH-RUNTIME-API-WO: CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION
Future endpoint candidate: POST /api/ai/decision-dry-runs
Future endpoint state now: NOT IMPLEMENTED
NQ_DRYRUN source: REVIEW_GATED / NOT_IN_PRODUCTION_ALLOWLIST
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
NEXT_ACTION: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION
```

### 核心工单结论

- Future endpoint 固定为 `POST /api/ai/decision-dry-runs`，但当前仍为 `NOT IMPLEMENTED`，不得写入已实现 API、OpenAPI path 或 Controller。
- 下一轮 implementation 必须默认 feature flag disabled、dev/test only、production disabled，并强制 HMAC、UTC `Z` timestamp、±300s replay window、persistent nonce replay guard、tenant/source allowlist、requestId / traceId / tenantId binding、payload cap、rate limit、memory cap、fail-closed、redacted audit logging 和 kill switch。
- `NQ_DRYRUN` 仍是 review-gated source，不得直接进入 production allowlist；如下一轮允许，只能先限 dev/test profile，并要求 tenant + source pair allowlist。
- request envelope 只允许 read-only dry-run 字段；禁止 credential、apiKey、apiSecret、passphrase、accountSecret、executableOrder、BUY/SELL order instruction、quantity/leverage/order price executable instruction。
- response envelope 只能是 read-only decision snapshot；`LONG_BIAS / SHORT_BIAS` 只是 bias，不是 `BUY / SELL`；NQ 只能记录，不执行。
- error taxonomy 冻结为 `SIGNATURE_INVALID / TIMESTAMP_INVALID / TIMESTAMP_OUT_OF_WINDOW / NONCE_REPLAY / TENANT_MISMATCH / SOURCE_DENIED / PAYLOAD_TOO_LARGE / RATE_LIMITED / MEMORY_LIMIT_EXCEEDED / POLICY_DENIED / PROVIDER_DISABLED / PROVIDER_TIMEOUT / BUDGET_EXCEEDED / UNKNOWN_ERROR`。
- 后续必须拆成 DH limited endpoint implementation、DH runtime endpoint tests、NQ limited dry-run client work order、NQ client implementation、joint runtime dry-run tests 与 runtime close review，不得把 DH endpoint 和 NQ client 合并为一个大实现任务。

### 验证

- DH `git diff --check`：PASS；仅 LF/CRLF warning；无 whitespace error。
- DH forbidden-scope diff：PASS / EMPTY；未触达 `dh-*/src/main`、`contracts` 或 `golden_cases`。
- DH OpenAPI/schema scan：PASS / NO HIT；`contracts/openapi.yaml` 与 `contracts/json-schema` 未出现 `decision-dry-runs`。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；Checkstyle 0 violations；Spotless check passed。
- DH `mvn test`：NOT RUN；本轮为 work-order-only docs scope，未声称 full test pass。
- NQ dev read-only：存在既有 GateO/current dirty/rename，但 NQ-DH / Integration-1 scoped unstaged 与 staged diff 为空；本轮未修改 NQ dev。
- NQ worktree read-only：branch=`nq-dh-i1-runtime-api-contract-review`；status 与 diff stat 无输出；本轮未修改 NQ worktree。

### 边界确认

未改 DH production code；未改 DH test code；未新增 API / Controller；未新增 Client、Service、Repository 或 migration；未改 `contracts/**`、`golden_cases/**`、fixture JSON 或 OpenAPI；未真实 HTTP；未启动 runtime；未读取或输出 credential、token、cookie、API secret、passphrase；未接 provider；未接 AI / LangGraph；未修改 NQ dev 或 NQ worktree；未开启 LIVE；未把 Runtime integration 写成 started；未把 DH 写成 integrated。

## 2026-07-04 NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW

完成 `NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW` 的 review-only 收口。本轮只审查 PR #12 合并后的 limited dry-run runtime API / contract / security 前置条件，不实现 runtime，不新增 API / Controller，不修改 production code、test code、contracts、golden_cases、fixture JSON、migration、OpenAPI 或 CI workflow。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
E:\Project\nexus-quant-i1-dryrun\docs\current\README.md
E:\Project\nexus-quant-i1-dryrun\docs\current\ROADMAP.md
E:\Project\nexus-quant-i1-dryrun\docs\current\STATUS.md
E:\Project\nexus-quant-i1-dryrun\docs\current\TESTING.md
E:\Project\nexus-quant-i1-dryrun\docs\current\WORKLOG.md
E:\Project\nexus-quant-i1-dryrun\docs\current\WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW: CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME
Recommended option: Option D / freeze API contract, error taxonomy, and envelope before split DH/NQ implementation
Future endpoint candidate: POST /api/ai/decision-dry-runs / NOT IMPLEMENTED
NQ_DRYRUN source: REVIEW_GATED / NOT_IN_PRODUCTION_ALLOWLIST
dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version: FUTURE_ENVELOPE_FIELDS / NOT_SCHEMA_CHANGE_NOW
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
NEXT_ACTION: NQ-DH-I1-DH-RUNTIME-API-WO
```

### 核心评审结论

- DH 可以在后续单独 work order 中规划 limited dry-run API / Controller，但本轮不新增 endpoint；future candidate 为 `POST /api/ai/decision-dry-runs`。
- DH future request 必须 signed / timestamped / nonce / tenant-bound，并且 fail-closed；禁止 provider / Agent / LangGraph / NQ DB / executable trading instruction。
- `NQ_DRYRUN` 仍为 review-gated source，不进入 production allowlist；进入 allowlist 前必须冻结 tenant/source pair、profile isolation、persistent nonce replay、rate limit、payload cap、memory cap、HMAC 和 audit policy。
- canonical error taxonomy、schema envelope、HMAC signature material、timestamp / nonce / replay guard / tenant binding / requestId / traceId 规则必须先冻结，再进入实现。
- NQ future client 不得触发 order / execution / risk mutation / ledger mutation / Paper Run / LIVE；`LONG_BIAS` / `SHORT_BIAS` 不得映射为 `BUY` / `SELL`。
- runtime failure 必须 fail-closed；audit / trace / replay 只能保存 redacted summary，不保存 credential、raw prompt、raw provider response 或敏感 header/body。
- 后续 implementation 必须拆成 DH runtime API contract WO、DH limited runtime endpoint implementation、NQ limited dry-run client implementation、joint runtime dry-run tests 与 runtime close review，不允许合并成一个大实现任务。

### 验证

- DH `git diff --check`：PASS；forbidden-scope diff 为空。
- DH security scan：PASS / REVIEWED；命中均归类为 docs prohibition、test guard、existing unrelated code 或既有模块，未发现 actual risk。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS。
- DH targeted Integration-1 guard / support tests：PASS；`DecisionContractGapGuardTest` 6 tests、`DhDryRunTestSupportEntryTest` 12 tests、`DhIntegration1JointMockContractFixtureTest` 6 tests，均 0 failures / 0 errors。
- DH `mvn -ntp test` 与 README baseline smoke-excluded full test：TIMEOUT / NOT PASSED；均未取得 BUILD SUCCESS/FAILURE，未写成通过。
- NQ worktree `git diff --check`：PASS；forbidden-scope diff 为空。
- NQ security scan：PASS / REVIEWED；未发现 NQ-DH runtime actual risk。
- NQ worktree full backend test：PASS / BUILD SUCCESS；23 modules，`nq-app` 105 tests，0 failures，0 errors，3 skipped。
- NQ worktree Integration0 scoped test：PASS；17 tests，0 failures，0 errors。
- NQ worktree Integration1 scoped test：PASS；18 tests，0 failures，0 errors。
- NQ dev post-PR baseline：PASS / READ-ONLY；当前 `dev` / `origin/dev` 为 `b856cf07155de26f87fad9c21234c1a8a07b964a`，PR #12 merge commit `578eb65e851086d0668bbebef74c319df1e5d63c` 是当前 dev ancestor，scoped NQ-DH / Integration-1 diff 为空。

### 边界确认

未改 DH / NQ production code；未改测试代码；未改 `contracts/**`、`golden_cases/**`、fixture JSON、OpenAPI、Controller、Client、Repository、Service、migration、runtime wiring 或 CI workflow；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient；未新增真实 Provider；未读取或输出 credential、token、cookie、API secret、passphrase；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE；未修改 NQ dev。

## 2026-07-04 NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN

完成 `NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN` 的 docs-only / plan-only 收口。结论为 `CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME`：允许后续单独进入 `NQ-DH-I1-MOCK-BASELINE-PR-PREP` 与 `NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW`，但不允许 runtime implementation。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
E:\Project\nexus-quant-i1-dryrun\docs\current\README.md
E:\Project\nexus-quant-i1-dryrun\docs\current\ROADMAP.md
E:\Project\nexus-quant-i1-dryrun\docs\current\STATUS.md
E:\Project\nexus-quant-i1-dryrun\docs\current\TESTING.md
E:\Project\nexus-quant-i1-dryrun\docs\current\WORKLOG.md
E:\Project\nexus-quant-i1-dryrun\docs\current\WORK_ORDER.md
```

### 结果

```text
ALLOW_LIMITED_DRYRUN_RUNTIME_PLAN_CLOSE: YES
ALLOW_RUNTIME_IMPLEMENTATION: NO
ALLOW_RUNTIME_API_CONTRACT_REVIEW: YES
ALLOW_MOCK_BASELINE_PR_PREP: YES
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
NEXT_ACTION: NQ-DH-I1-MOCK-BASELINE-PR-PREP
```

### 验证

- DH `git diff --check`：PASS；forbidden-scope diff 为空。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS。
- DH `mvn -ntp test`：TIMEOUT，未取得 BUILD SUCCESS/FAILURE；Docker-gated smoke 排除版同样 timeout，残留 Java 进程已清理。
- NQ worktree `git diff --check`：PASS；forbidden-scope diff 为空。
- NQ worktree `mvn -ntp -f backend/pom.xml test`：PASS / BUILD SUCCESS。
- NQ worktree Integration0 scoped test：PASS / BUILD SUCCESS；17 tests，0 failures，0 errors，0 skipped。
- NQ dev pathspec diff：PASS / EMPTY；`WORKSTREAM_MIXED_BLOCKED: NO`。

### 边界确认

未改 DH / NQ production code；未改测试代码；未改 `contracts/**`、`golden_cases/**`、fixture JSON、OpenAPI、Controller、migration 或 CI workflow；未新增 RealClient、real provider、真实 HTTP、AI / LangGraph 或 LIVE。未读取 credential / token / cookie / API secret / passphrase。NQ dev 未修改。

## 2026-07-04 NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS

完成 `NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS` 的 joint mock fixture / contract tests。实现集中在 DH 与 NQ dry-run worktree test scope；本轮不修改 production code、contracts、golden_cases、OpenAPI、Controller、migration 或 runtime wiring。

### 新增文件

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhIntegration1JointMockContractFixtureTest.java
dh-usecase/src/test/resources/nq-dh/integration1/joint_mock_contract_fixtures.json
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1JointMockContractFixtureTest.java
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\resources\nq-dh\integration1\joint_mock_contract_fixtures.json
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS: IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW
Next: NQ-DH-I1-MOCK-CLOSE-REVIEW / NOT STARTED / REVIEW_ONLY / NO_RUNTIME
ALLOW_IMP3_CLOSE: YES
ALLOW_I1_MOCK_CLOSE_REVIEW: YES
ALLOW_PRODUCTION_CODE_CHANGE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 验证

- DH targeted test：`DhIntegration1JointMockContractFixtureTest` PASS；6 tests，0 failures，0 errors，0 skipped。
- NQ targeted test：`NqDhIntegration1JointMockContractFixtureTest` PASS；7 tests，0 failures，0 errors，0 skipped。
- DH `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；Docker/Testcontainers 不可用导致 Docker-gated smoke skipped，非代码失败。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。
- NQ full backend test：PASS / BUILD SUCCESS；23 个 backend reactor module SUCCESS。
- NQ Integration-0 scoped test：PASS；17 tests，0 failures，0 errors，0 skipped。

### 边界确认

未改 DH / NQ `src/main`；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、migration、runtime wiring、RealClient、real provider、真实 HTTP、AI / LangGraph 或 LIVE。未读取 credential / token / cookie / API secret / passphrase。IMP3 只支持 mock close review，不得据此启动 Integration-1 runtime。

## 2026-07-04 NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT

完成 `NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT` 的 DH 侧 current docs 同步与验证记录收口。实现集中在 NQ dry-run worktree test scope；DH 本轮不新增或修改生产 / 测试代码，不修改 contracts、golden_cases、fixture JSON、OpenAPI、Controller、migration 或 runtime wiring。

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT: VERIFY PASS / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_IMP3_JOINT_MOCK_CONTRACT_TESTS
Next consumed: NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS / IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW
ALLOW_IMP2_CLOSE: YES
ALLOW_I1_IMP3_JOINT_MOCK_CONTRACT_TESTS: YES
ALLOW_PRODUCTION_CODE_CHANGE: NO
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

### 验证

- DH `git diff --check`：PASS。
- DH forbidden diff：`dh-domain/src/main` / `dh-usecase/src/main` / `dh-memory/src/main` / `dh-eval/src/main` / `dh-connector/src/main` / `dh-api/src/main` / `dh-app/src/main` / `dh-infra/src/main` / `contracts` / `golden_cases` 均为空。
- DH `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；surefire reports 汇总 451 tests，0 failures，0 errors，4 skipped；Docker/Testcontainers 不可用导致 Docker-gated smoke skipped，非代码失败。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS。
- NQ targeted test：`NqDhIntegration1StubRecorderNoSideEffectTest` PASS；6 tests，0 failures，0 errors，0 skipped。
- NQ full backend test：PASS / BUILD SUCCESS；23 个 backend reactor module SUCCESS；surefire reports 汇总 628 tests，0 failures，0 errors，4 skipped。
- NQ dev pathspec diff：PASS / EMPTY；`WORKSTREAM_MIXED_BLOCKED: NO`。

### 边界确认

未改 DH 代码；未改 DH `contracts/**` 或 `golden_cases/**`；未新增 fixture JSON、API path、Controller、migration、runtime wiring、RealClient、real provider、AI / LangGraph 或 LIVE。未读取 credential / token / cookie / API secret / passphrase。IMP3 仍为下一步单独授权的 joint mock contract tests，不得据此启动 Integration-1 runtime。

## 2026-07-03 NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO

完成 `NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO`。本轮只做 `WORK_ORDER_ONLY`：新增 joint mock fixture family 与 contract test batch 的实现前工单，冻结 M3 为 IMP0 前最后一个规划 WO，并将下一步切换为 `NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED`。NQ dev 仅只读检查，未写入。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO.md
```

### 修改文件

```text
README.md
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
docs/current/DH_NQ_INTEGRATION1_M2_NQ_DRYRUN_STUB_RECORDER_WO.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO: COMPLETED / WORK_ORDER_ONLY / FINAL_WO_BEFORE_IMPLEMENTATION / NOT IMPLEMENTED
Next: NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED
ALLOW_M3_WO_CLOSE: YES
ALLOW_I1_IMP0_CONTRACT_GAP_TEST_SUPPORT_IMPLEMENTATION: YES
ALLOW_MORE_PLANNING_WO: NO
ALLOW_I1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### M3 范围

- 规划 23 类 joint mock fixtures，覆盖 valid dry-run、readonly response、签名缺失/错误、timestamp skew、nonce replay、source denied、payload too large、rate limited、tenant mismatch、credential/order/account/quantity/price/leverage/BUY/SELL forbidden field、provider disabled/timeout/budget exceeded、risk blocked、no evidence fail-closed、internal fail-closed、long/short bias readonly、no real URL、no credential、no outbound。
- 规划 14 组 contract test batches，覆盖 DH contract validator、NQ request builder、NQ recorder no-side-effect、joint fixture parse、forbidden field fail-closed、source denied、UTC `Z` timestamp、HMAC signature material、tenant/requestId/traceId binding、error taxonomy mapping、no-order/no-risk/no-ledger/no-paper/no-live scan、no real HTTP/no outbound、credential logging/persistence、golden_cases compatibility smoke。
- 明确 M3 不创建 fixture JSON、不改 schema/contracts/golden_cases、不新增 API/controller/migration/runtime/provider/AI/LangGraph/LIVE。

### 验证

- `git status --short`：PASS / CHANGES PRESENT；DH dirty 限于允许的 current docs 和新增 M3 WO。
- `git diff --check`：PASS；退出码 0，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY；tracked diff 限于文档。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- NQ dry-run worktree forbidden diff：`backend` / `frontend` / `research` / `scripts` / `deploy` / `.github` / migration 均为空。
- NQ dev pathspec diff：PASS / EMPTY；`docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 unstaged 或 staged diff，`WORKSTREAM_MIXED_BLOCKED: NO`。初始 precheck 曾观察到非 NQ-DH dirty，final spot-check `git status --short` 返回空；本轮未写 NQ dev。
- DH `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 DH reactor module SUCCESS；Docker/Testcontainers 不可用导致 Docker-gated smoke tests skipped，非代码失败。
- DH `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；19 个 DH reactor module SUCCESS；Checkstyle 0 violations；Spotless check passed。
- NQ dry-run worktree `mvn -ntp -f backend/pom.xml test`：PASS / BUILD SUCCESS；23 个 backend reactor module SUCCESS；`nq-app` 86 tests 中 2 skipped。
- NQ dry-run worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS / BUILD SUCCESS；Integration-0 contract/security/no-side-effect 3 个测试类共 17 tests，0 failures / 0 errors / 0 skipped。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 RealClient、real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO final validation

完成 `NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO`。本轮只做 `WORK_ORDER_ONLY`：关闭 `NQ_DRYRUN` source allowlist、canonical error taxonomy、dry-run endpoint shape、schema alias / envelope gap 的 before-code 裁决；同步 DH current docs 与 NQ dry-run worktree current docs；NQ dev 仅只读检查，未写入。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO: COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED
Current next: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED
NQ_DRYRUN source allowlist: NEEDS_SECURITY_CONTRACT_CHANGE
dry-run endpoint shape: Option C / test-support mock-only, no runtime endpoint
DOC_ONLY_ALIAS fields: dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version
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

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档和新增 M0 WO。
- `git diff --check`：PASS；退出码 0，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY；tracked diff 限于 `docs/current` 文档。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- NQ dry-run worktree forbidden diff：`backend` / `frontend` / `research` / `scripts` / `deploy` / `.github` / migration 均为空。
- NQ dev pathspec diff：PASS / EMPTY；`docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 unstaged 或 staged diff，`WORKSTREAM_MIXED_BLOCKED: NO`。最终检查时 NQ dev 存在非 NQ-DH / Integration1 的主线 dirty，本轮未读取敏感内容、未修改、未覆盖。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 DH reactor module SUCCESS；Docker/Testcontainers 不可用导致 Docker-gated smoke tests skipped，非代码失败。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；19 个 DH reactor module SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO final validation

完成上一轮中断后的 `NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO` 复核与补齐。本轮只做 `WORK_ORDER_ONLY`：确认 DH current docs 与 NQ worktree current docs 已同步 dry-run mock implementation WO；修正 NQ dev clean precheck 规则为 NQ-DH / Integration1 相关 pathspec clean，而不是全局 clean；确认 NQ dev 当前无相关 dirty diff，因此 `WORKSTREAM_MIXED_BLOCKED: NO`。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO: COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED
Current next: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO / NOT STARTED
ALLOW_WORK_ORDER_CLOSE: YES
ALLOW_I1_M0_CONTRACT_GAP_CLOSE_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档和新增 WO。
- `git diff --check`：PASS；退出码 0，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY；tracked diff 限于 `docs/current` 文档。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- NQ dev pathspec diff：PASS / EMPTY；`docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 unstaged 或 staged diff，`WORKSTREAM_MIXED_BLOCKED: NO`。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；Docker/Testcontainers 不可用导致 4 个 Docker-gated smoke tests skipped，非代码失败。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX final validation

完成 NQ-DH Integration-1 P4 implementation gate review fix。本轮只做 docs-only / gate-fix：确认双仓 P3 docs 已提交并可继续；把 `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 归为 `DOC_ONLY_ALIAS` 或 future envelope planning；把 `NQ_DRYRUN` source allowlist、canonical error code names、dry-run endpoint shape 归为 `NEEDS_CONTRACT_REVIEW_BEFORE_CODE`；确认 `DecisionAction` whitelist 和 fixed `ForbiddenAction` list 为 `EXISTS_NOW`；确认 trading executable fields 继续 `PROHIBITED`。

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/WORK_ORDER.md
```

### 结果

```text
NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX: COMPLETED / DOCS-ONLY / GATE-FIX
Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW_FIX_CLOSE: YES
ALLOW_I1_P4_RETRY: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档。
- `git diff --check`：PASS；退出码 0，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- stale old next scan：PASS / EMPTY；`docs/current` 无旧 P4 not-started next 残留。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；`PostgresContainerSmokeTest` 因本地 Docker/Testcontainers 环境不可用 skip 1，非代码失败。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-03 NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN final validation

完成 NQ-DH Integration-1 P3 dry-run implementation readiness plan 收口。本轮只做 docs-only / plan-only：新增 DH canonical readiness plan；合并原 `NQ-DH-I1-P3-NQ-DRYRUN-STUB-TEST-PLAN`、`NQ-DH-I1-P4-DH-DRYRUN-ENTRY-PLAN`、`NQ-DH-I1-P5-JOINT-MOCK-VALIDATION-PLAN` 三个后续 planning 任务；该 P3 next 已由 `NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX / COMPLETED / DOCS-ONLY / GATE-FIX` 消费，当前 next 为 `NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED`；不创建 fixture JSON，不修改 contracts、golden_cases、生产代码、测试代码、API、migration、runtime client、provider 或真实 HTTP。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 结果

```text
NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Canonical plan: docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md
P3 next consumed by: NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX / COMPLETED / DOCS-ONLY / GATE-FIX
Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
ALLOW_I1_P3_DRYRUN_IMPLEMENTATION_READINESS_PLAN_CLOSE: YES
ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW: YES
ALLOW_SCHEMA_CHANGE: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档。
- `git diff --check`：PASS；无 whitespace error，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY。
- stale old next scan：PASS / EMPTY；旧 P3/P4/P5 active next 已合并到 P3 readiness plan，当前 next 统一为 P4 implementation gate review。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；`PostgresContainerSmokeTest` 因本地 Docker/Testcontainers 环境不可用 skip 1，非代码失败。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-02 NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN final validation

完成 NQ-DH Integration-1 P2 contract fixtures plan 收口。本轮只做 docs-only / plan-only：新增 DH canonical fixtures plan；原 P2 next 已由 `NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN / COMPLETED / PLAN ONLY / NOT IMPLEMENTED` 与 `NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX / COMPLETED / DOCS-ONLY / GATE-FIX` 消费，当前 next 为 `NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED`；不创建 fixture JSON，不修改 contracts、golden_cases、生产代码、测试代码、API、migration、runtime client、provider 或真实 HTTP。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 结果

```text
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Canonical plan: docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md
P2 next consumed by: NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN / COMPLETED / PLAN ONLY / NOT IMPLEMENTED
P2 next consumed by: NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN and NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX
Current next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN_CLOSE: YES
ALLOW_I1_P3_DRYRUN_IMPLEMENTATION_READINESS_PLAN: YES / COMPLETED / PLAN ONLY
ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW: YES
ALLOW_SCHEMA_CHANGE: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档。
- `git diff --check`：PASS；无 whitespace error，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；`PostgresContainerSmokeTest` 因 Docker named-pipe AccessDenied 被 Testcontainers skip 1，属于本地 Docker 可达性问题，不是代码失败。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture JSON 或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-02 NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN final validation

完成 NQ-DH Integration-1 P1 contract dry-run plan 收口。本轮只做 docs-only / plan-only：新增 DH canonical contract plan，并把 current docs 指向 `NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED`；不实现 runtime、API、Controller、Client、Provider、migration、测试代码或真实 HTTP。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
```

### 修改文件

```text
docs/current/API.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 结果

```text
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Canonical plan: docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
Next: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
ALLOW_I1_P1_CONTRACT_PLAN_CLOSE: YES
ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN: YES
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 规划摘要

- 规划 NQ -> DH dry-run `DecisionRequest`：`schemaVersion`、`requestId`、`traceId`、`tenantId`、`source=NQ_DRYRUN`、`decisionType=READ_ONLY_RECOMMENDATION`、`dryRun=true`、subject、contextSnapshot、evidence refs；`dryRun` 等字段仍是 schema gap，不是已实现字段。
- 规划 DH -> NQ read-only `DecisionOutput`：`ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS` 只读 action、固定 `forbiddenActions`、reason/evidence/provider/trace/audit/replay summary；`decisionId / confidence / replayRef / auditRef / dryRun` 等仍是 schema gap。
- 规划 canonical `X-NQ-DH-*` header、UTC `Z` timestamp、nonce replay、HMAC signatureMaterial、payload size gate、rate limit、error taxonomy、trace / audit / replay、测试矩阵和后续 P2-P6 批次。

### 验证

- `git status --short`：PASS / CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档。
- `git diff --check`：PASS；无 whitespace error，仅 Windows LF/CRLF warning。
- `git diff --stat`：PASS / DOCS-ONLY。
- forbidden diff：`dh-domain` / `dh-usecase` / `dh-memory` / `dh-eval` / `dh-connector` / `dh-api` / `dh-app` / `dh-infra` / `contracts` / `golden_cases` 均为空。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS；`PostgresContainerSmokeTest` 因 Docker named-pipe AccessDenied 被 Testcontainers skip 1，属于本地环境可达性问题。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API path、Controller、Client、Repository、Service、migration、fixture 文件或 CI workflow；未真实 HTTP；未启动 NQ/DH runtime；未读取 credential；未接 real provider、AI 或 LangGraph；未开启 LIVE；未让 DH 输出进入 order、risk mutation、ledger mutation、Paper Run 或 private trading 路径。

## 2026-07-02 NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE

完成 NQ-DH Integration-1 dry-run P0 factsource rebase close。本轮只做 docs-only / factsource-only 收口：把 DH 当前事实源从 “P0 NOT STARTED” 推进为 “P0 CLOSED / ACCEPTED”，并把唯一下一步收口到 `NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / NOT STARTED`。

### 修改内容

- 同步 `AGENTS.md`、`README.md`、`docs/current/README.md`、`docs/current/STATUS.md`、`docs/current/ROADMAP.md`、`docs/current/WORK_ORDER.md`、`docs/current/DH_NQ_INTEGRATION.md`、`docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md` 和 Codex workflow 入口的 current-state wording。
- 固定 Integration-1 前置条件为 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED`。
- 明确 `NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN` 仍是 plan-only，不授权 implementation、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
- 保留历史 `WORKLOG` / `TESTING` 与 `docs/gates/**` 冻结快照内部 GateK 字样，只作为历史记录或 `SUPERSEDED / NAMING_REPLACED` 说明。

### 结果

```text
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
Integration-1 dry-run plan baseline: ACCEPTED
Prerequisite: NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED
Next: NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / NOT STARTED
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

### 验证

- `git diff --check`：PASS，仅 LF/CRLF warning。
- GateK/GateN residual scan：已分类；当前前置条件未写成 `NQ GateN + DH GateK CLOSED`。
- `mvn -ntp test`：PASS / BUILD SUCCESS；19 个 reactor module SUCCESS，`PostgresContainerSmokeTest` 因 Docker named-pipe AccessDenied 被 Testcontainers skip 1。
- `mvn -ntp -Pquality validate`：PASS / BUILD SUCCESS；Checkstyle 0 violations；Spotless check passed。

### 边界确认

未改生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API、Controller、migration、runtime client、provider 或 RealClient；未真实 HTTP；未真实 NQ / DH runtime；未读取 credential；未接 AI / LangGraph；未启动 Integration-1 runtime；未开启 LIVE。

## 2026-07-02 DH-STAGE4-NAMING-REBASE-FIX

完成 DH Stage4 命名 rebase。本轮只做 docs-only / naming-fix-only：修正 DH 当前事实源中的 Stage 命名、验收报告文件名、冻结目录命名和后续 Integration-1 前置条件；不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ 仓库。

### 文件重命名

```text
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md
  -> docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
  -> docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
  -> docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
docs/gates/dh-gatek-decision-pipeline-mvp/
  -> docs/gates/dh-stage4-decision-pipeline-mvp/
```

### 命名结论

```text
DH-STAGE4-DECISION-PIPELINE-MVP: ACCEPTED / CLOSED
DH-GATEK-DECISION-PIPELINE-MVP: SUPERSEDED / NAMING_REPLACED
dh-gatek-decision-pipeline-mvp: SUPERSEDED / NAMING_REPLACED
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
```

后续 Integration-1 前置条件固定为 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED`。旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` 仍只作为 historical reference，保持 `SUPERSEDED / REBASE_REQUIRED`。

### 边界确认

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API / Controller / migration；未启动 Integration-1 runtime；未真实 HTTP；未真实 NQ 调用；未接真实 provider；未接 AI / LangGraph；未开启 LIVE；未修改 NQ 仓库；未读取或输出 credential、token、cookie、API secret 或 passphrase。

## 2026-07-02 DH-DOCS-SKILL-STAGE-NAMING-AUDIT-FIX

完成 DH 文档治理 skill 与 workflow router 的阶段命名规则修复。本轮只改 docs / skill 规则，不改生产代码、测试代码、contracts、golden_cases、API、Controller、migration，不移动或删除 `docs/gates` 冻结目录，不执行 Stage4 命名 rebase。

### 完成内容

- 在 `.agents/skills/dh-docs-writer/SKILL.md` 增加 Stage Naming Rules，明确 DH 自身阶段使用 Stage 体系，NQ 自身阶段使用 Gate 体系。
- 在 `.agents/skills/nq-dh-workflow-router/SKILL.md` 增加 Stage Naming Route，要求发现 `DH GateK/GateL/GateN` 或 `DH-GATEK-*` 时先进入命名修复，而不是继续推进业务任务。
- 在 `AGENTS.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/CODEX_WORKFLOW_INDEX.md`、`docs/current/README.md` 同步简短命名治理规则。
- 在 `docs/current/ROADMAP.md` 记录后续 `DH-STAGE4-NAMING-REBASE-FIX`。

### 命名结论

```text
NQ stage system: Gate，例如 GateN
DH stage system: Stage，例如 DH-STAGE4-DECISION-PIPELINE-MVP
NQ-DH integration wording: 可以引用 NQ GateN rebase，但不得把 DH 自身阶段写成 GateK/GateL/GateN
Current wrong legacy name at that time: DH-GATEK-DECISION-PIPELINE-MVP
Correct future name: DH-STAGE4-DECISION-PIPELINE-MVP
Current wrong legacy dir at that time: docs/gates/dh-gatek-decision-pipeline-mvp/
Future target dir: docs/gates/dh-stage4-decision-pipeline-mvp/
```

Decision Pipeline MVP 已 `ACCEPTED / CLOSED` 的事实不变。当前 docs/current 与 docs/gates 中仍存在 `DH-GATEK` / `dh-gatek` 命名残留，本轮只记录为待修复对象；下一任务 `DH-STAGE4-NAMING-REBASE-FIX` 负责批量命名 rebase 与冻结目录处理方案。

### 边界

未改 Java / Kotlin / Python / TypeScript 生产代码；未改测试代码；未改 `contracts/**` 或 `golden_cases/**`；未新增 API / Controller / migration；未移动或删除 `docs/gates`；未启动 Integration-1 runtime；未真实 HTTP；未真实 NQ 调用；未接真实 provider；未接 AI / LangGraph；未开启 LIVE；未修改 NQ 仓库。

## 2026-07-02 NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN

完成 NQ-DH Integration-1 dry-run 的 GateN rebase planning。本轮只做 docs-only / plan-only 文档规划与 current factsource 同步，不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ runtime。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
```

### 修改文件

```text
docs/current/DH_NQ_INTEGRATION.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### Plan result

```text
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
Next concrete action: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ baseline: GateN
DH baseline: DH Stage4 Decision Pipeline MVP CLOSED / ACCEPTED
Integration-1 dry-run implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

本计划定义 dry-run 目标、NQ / DH 职责边界、NQ -> DH request 规划、DH -> NQ response 规划、`X-NQ-DH-*` header / timestamp / nonce / HMAC / payload size / rate limit / fail-closed 安全协议、I1-P0..P5 批次和合同 / no-outbound / no-live-trade / replay / golden case 测试矩阵。

### 验证

- `git status --short`：PASS；计划文档与 docs/current 索引类文档变更可见。
- `git diff --check`：PASS；无 whitespace error，仅 Windows 行尾转换 warning。
- `git diff --stat`：PASS；tracked diff 限于 docs/current 文档。
- `mvn -ntp test`：BUILD SUCCESS；19 个 reactor module 全部 SUCCESS；`PostgresContainerSmokeTest` 因当前进程访问 `\\.\pipe\docker_engine` 被拒绝而 skip 1 个 Testcontainers smoke，属于 Docker named-pipe 环境可达性问题。
- `mvn -ntp -Pquality validate`：BUILD SUCCESS；19 个 reactor module 全部 SUCCESS；Checkstyle 0 violations；Spotless check passed。

### Readiness decision

```text
ALLOW_INTEGRATION1_DRYRUN_PLAN_CLOSE: YES
ALLOW_NQ_DH_I1_P0_FACTSOURCE_REBASE: YES
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 边界确认

未新增 API path / Controller / migration；未修改 Java 生产代码或测试代码；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未读取或输出 credential、token、cookie、API secret、passphrase；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未把 dry-run 写成真实联调或实盘准备完成。

### 下一步

进入 `NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED`；只允许同步 NQ / DH 当前事实源并清理旧 GateK Integration-1 当前主线残留，不允许启动 implementation、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

## 2026-07-02 DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE

完成 DH Stage4 Decision Pipeline MVP 最终验收与冻结。本轮只做 acceptance / freeze / regression validation / security boundary review / docs sync / no-live-trade confirmation，不修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ 仓库。

### 新增文件

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
docs/gates/dh-stage4-decision-pipeline-mvp/**
```

### 修改文件

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/API.md
```

### 验收结论

```text
DH Stage4 Decision Pipeline MVP: ACCEPTED / CLOSED
K1 Contract Freeze: CLOSED
K2 Orchestrator Skeleton: CLOSED
K3 Audit / Snapshot / Trace Persistence: CLOSED
M1 Readiness Review: CLOSED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED
Next: NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN / NOT STARTED
```

### 验收要点

```text
K1:
  DecisionType 仅 READ_ONLY_RECOMMENDATION；DecisionAction 仅 ABSTAIN / OBSERVE / NO_TRADE /
  LONG_BIAS / SHORT_BIAS；forbiddenActions 固定五项；schema 不含账户、凭证或交易执行字段。

K2:
  DefaultDecisionOrchestrator 使用 deterministic mock provider；无 agent runtime、真实 provider、
  NQ runtime、HTTP、LangGraph 或 LIVE；异常路径返回 structured fail-closed output。

K3:
  Flyway V5 只新增 DH-owned decision audit / snapshot / trace / provider call / output / audit event 表；
  不保存 secret / token / credential / NQ DB 内容；persistence failure fail-closed。

K4:
  internal replay read model 只读取 K3 数据；不新增 API / Controller / replay endpoint；tenant mismatch
  和 corrupted / incomplete data fail-closed。

K5:
  mock-only health / budget / latency guard；disabled / unhealthy / timeout / budget exceeded 均 fail-closed；
  provider call summary 可经 replay 读取。

K6:
  mock NQ dry-run factory / fixture / no-live-trade / persistence-to-replay contract tests 通过；
  无真实 HTTP、NQ mutation 或 NQ 仓库改动。

K7:
  12 个 deterministic golden cases、eval baseline 与 security boundary tests 通过；
  action 白名单和禁止字段扫描稳定。
```

### 验证记录

```text
git status --short
  开工前为空；最终显示 README.md 与 docs/current 状态文档变更，并新增
  docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md 与
  docs/gates/dh-stage4-decision-pipeline-mvp/。

git diff --check
  开工前和最终均 exit code 0；无 whitespace error；仅 Windows LF -> CRLF warning。

git diff --stat
  最终 tracked stat 为 8 个 tracked 文件变更，391 insertions / 87 deletions；
  新增 untracked acceptance report 与 docs/gates freeze snapshot 由 git status --short 展示。

mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  BUILD SUCCESS；reactor 15/15 SUCCESS；Total time 18.980 s；Finished at 2026-07-02T19:39:15+08:00。
  当前 sandbox 下 Testcontainers 无可用 Docker 环境，PostgresContainerSmokeTest skipped 1。

mvn -ntp test
  BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 18.355 s；Finished at 2026-07-02T19:39:53+08:00。
  当前 sandbox 下 Testcontainers 无可用 Docker 环境，PostgresContainerSmokeTest skipped 1。

mvn -ntp -Pquality validate
  BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；
  Finished at 2026-07-02T19:40:04+08:00。

docker info --format '{{.ServerVersion}}'
  FAILURE；npipe:////./pipe/dockerDesktopLinuxEngine 不存在，Docker daemon 当前未运行或未暴露该管道。
```

### 边界确认

```text
未修改生产代码。
未修改测试代码。
未新增 API path。
未新增 Controller。
未新增 migration。
未新增 Repository / Service / Client 实现。
未真实 HTTP。
未真实 NQ 调用。
未真实 DH runtime integration。
未真实交易所调用。
未新增 RealClient。
未新增真实 Provider。
未接 OpenAI / Claude / Gemini / 本地模型。
未接 LangGraph。
未接 MCP 写能力。
未读取或输出 credential、token、cookie、API secret、passphrase 或交易所密钥。
未启动 Integration-1 runtime。
未把 DH 写成 integrated。
未把 Runtime integration 写成 started。
未把 AI / Agent runtime 写成 started。
未开启 LIVE。
未修改 NQ 仓库。
未把 Stage4 完成写成允许真实交易。
未把 mock NQ dry-run 写成真实 NQ runtime。
未把 replay read model 写成 replay API。
未把 golden cases 写成真实交易验证。
```

### Readiness decision

```text
ALLOW_STAGE4_CLOSE: YES
ALLOW_INTEGRATION1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 下一步

```text
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN / NOT STARTED。
该下一步只允许基于 NQ GateN 重新做 Integration-1 dry-run planning；不得启动 Integration-1 runtime、
真实 NQ runtime、真实 provider、LangGraph runtime、AI / Agent runtime 或 LIVE。
```

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL

K7 已完成 deterministic golden cases / eval baseline / security boundary tests。本轮只做 K7 Golden Cases / Eval，不进入 K8 acceptance / freeze，不启动 Integration-1 runtime。

### 新增文件

```text
golden_cases/decision/valid_no_trade.json
golden_cases/decision/policy_blocked.json
golden_cases/decision/provider_timeout_abstain.json
golden_cases/decision/provider_budget_exceeded_abstain.json
golden_cases/decision/high_risk_abstain.json
golden_cases/decision/no_evidence_abstain.json
golden_cases/decision/forbidden_action_rejected.json
golden_cases/decision/replay_found_trace.json
golden_cases/decision/replay_tenant_mismatch_blocked.json
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionGoldenCaseTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionEvalBaselineTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionGoldenCaseSecurityBoundaryTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/DecisionGoldenCaseFixtures.java
```

### 修改文件

```text
golden_cases/decision/mock_nq_valid_dryrun.json
golden_cases/decision/mock_nq_provider_blocked.json
golden_cases/decision/mock_nq_no_live_trade_guard.json
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/MockNqDryRunAssertionSupport.java
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 实现要点

```text
Golden cases:
  - golden_cases/decision 现在共有 12 个 K7 deterministic wrapper。
  - K6 的 3 个 mock NQ dry-run fixture 复用并升级为 K7 wrapper，避免重复创建冲突文件。
  - 每个 wrapper 包含 caseId / description / input / expectedDecision / optional expectedReplay /
    forbiddenAssertions / securityBoundary。
  - expectedDecision 固定 READ_ONLY_RECOMMENDATION；action 只允许 ABSTAIN / OBSERVE / NO_TRADE /
    LONG_BIAS / SHORT_BIAS；forbiddenActions 固定五项。

Eval baseline:
  - DecisionGoldenCaseFixtures 统一读取 golden_cases/decision/*.json，避免每个测试重复处理文件路径。
  - DecisionGoldenCaseTest 覆盖所有 golden case 可解析、K1 request/output schema 形状、
    action 白名单、forbiddenActions 固定五项、关键路径期望。
  - DecisionEvalBaselineTest 固化 deterministic orchestrator 基线，覆盖 valid / blocked / provider failure /
    budget exceeded / high risk / no evidence / forbidden action / replay 等路径。
  - DecisionGoldenCaseSecurityBoundaryTest 扫描 credential / account / order / execution / endpoint /
    provider / output action 禁止项，保证 golden cases 不漂移为真实交易样例。

K6 compatibility:
  - MockNqDryRunAssertionSupport 不再把 expectedDecision.forbiddenActions 内的 PLACE_ORDER /
    CANCEL_ORDER 视为 fixture 越界，因为 K7 wrapper 需要固定 forbiddenActions 列表。
  - K6 仍继续阻断 credential、order/execution 字段和 output action 中的交易动作误用。

Docs:
  - docs/current/README.md / STATUS.md / ROADMAP.md / WORK_ORDER.md 已同步 K7
    IMPLEMENTED / READY FOR ACCEPTANCE。
  - docs/current/TESTING.md 记录本轮实际 git / Maven / quality / boundary scan 结果。
```

### 测试覆盖

```text
DecisionGoldenCaseTest:
  - 12 个 golden case 全部可解析。
  - input / expectedDecision 具备 K1 schema 关键字段。
  - action 白名单、READ_ONLY_RECOMMENDATION、mandatory forbiddenActions 固化。
  - valid_no_trade / policy_blocked / provider timeout / budget exceeded / high risk /
    no evidence / forbidden action / replay tenant mismatch 期望稳定。

DecisionEvalBaselineTest:
  - K1-K6 关键行为经 K7 golden cases 做 deterministic baseline。
  - provider failure / no evidence 默认 ABSTAIN。
  - high risk 不允许 LONG_BIAS / SHORT_BIAS。
  - forbidden action request fail-closed。
  - replay tenant mismatch 不跨 tenant 返回数据。

DecisionGoldenCaseSecurityBoundaryTest:
  - golden cases 中不存在 apiKey / apiSecret / passphrase / privateKey / token。
  - golden cases 中不存在 accountId、quantity、price、side、orderId、venueCredential、brokerCredential。
  - expectedDecision.action 不含 BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER。
  - 不包含真实 HTTP endpoint、真实 provider 配置或 NQ DB 内容。

K6 compatibility:
  - MockNqDecisionDryRunContractTest 与 MockNqDecisionNoLiveTradeContractTest 继续通过。
```

### 验证记录

```text
git status --short
  结果：仅 K7 允许范围内文件变更。

git diff --check
  结果：exit code 0；仅 LF/CRLF warning，无 whitespace error。

git diff --stat
  结果：tracked diff stat 正常输出；未跟踪新增文件由 git status --short 记录。

mvn -ntp -pl dh-usecase -am "-Dtest=DecisionGoldenCaseTest,DecisionEvalBaselineTest,DecisionGoldenCaseSecurityBoundaryTest,MockNqDecisionDryRunContractTest,MockNqDecisionNoLiveTradeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  结果：BUILD SUCCESS；14 tests passed。

mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  结果：BUILD SUCCESS；reactor 15/15 passed；PostgresContainerSmokeTest 在该轮实际使用 Docker / PostgreSQL 17 通过。

mvn -ntp test
  结果：BUILD SUCCESS；reactor 19/19 passed。
  说明：该全量轮次中 PostgresContainerSmokeTest 因 Docker named pipe AccessDeniedException 按 Testcontainers 机制 skip；
        这是本机 Docker 访问差异，不是 K7 代码失败。

mvn -ntp -Pquality validate
  结果：BUILD SUCCESS；reactor 19/19 passed；Checkstyle 0 violations；Spotless passed。

rg boundary scan
  结果：无 K7 生产越界实现；命中均归类为既有 negative tests / guard / docs boundary wording /
        existing API controllers / historical docs / K7 forbidden assertions。
```

### 边界确认

```text
未实现 K8 acceptance / freeze。
未新增 API path。
未新增 Controller。
未新增 migration。
未修改生产代码。
未新增 replay API / query endpoint。
未真实 HTTP。
未真实 NQ 调用。
未真实 provider。
未读取 credential、token、cookie、API secret、passphrase 或交易所密钥。
未接 OpenAI / Claude / Gemini / 本地模型。
未接 LangGraph。
未启动 Integration-1 runtime。
未把 DH 写成 integrated。
未把 Runtime integration 写成 started。
未把 AI / Agent runtime 写成 started。
未开启 LIVE。
未修改 NQ 仓库。
未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放入 DecisionAction 或 expectedDecision.action。
```

### Readiness decision

```text
ALLOW_K7_CLOSE: YES
ALLOW_K8_ACCEPTANCE_FREEZE: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

### 下一步

```text
进入 DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE / NOT STARTED。
K8 只能基于 K1-K7 evidence 做 acceptance / freeze，不得启动 Integration-1 runtime、真实 NQ runtime、
真实 provider、LangGraph runtime、AI / Agent runtime 或 LIVE。
```

## 2026-06-28 DH-CODE-REALITY-AUDIT-FIX-PACK

关闭 code reality audit 中阻断 GateK-PLAN / Integration-1-PLAN 的两个 P1，并在同域处理 P2/P3。

### 修改文件

```text
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/legacy/run/RunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/IdempotencyFilter.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/IngestionErrorCode.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/agent/feedback/impl/DefaultNqFeedbackContractValidator.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/legacy/run/LegacyRunControllerSecurityWebMvcTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/IdempotencyFilterSecurityTest.java
dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackControllerWebMvcTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackContractValidationTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/agent/feedback/NqFeedbackIdempotencyTest.java
docs/current/API.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

### 实现要点

```text
P1-1 /legacy/runs:
  - DhApiAuthenticationFilter 保护 /legacy/runs 与 /legacy/runs/**。
  - RunController.create 从 AuthenticatedRequest.requireTenantId 读取认证 tenant。
  - RunController.get 校验 run tenant 与认证 tenant 一致，不再允许匿名读取。

P1-2 NQ feedback validator:
  - production DefaultNqFeedbackContractValidator 内置 INT0 frozen forbidden fields / capabilities。
  - 递归扫描 payload field name、字符串 capability value，并解析 rawPayloadJson 中的 JSON 字符串继续扫描。
  - 命中后返回 FORBIDDEN_FIELD / FORBIDDEN_CAPABILITY，ingestion service 不保存 envelope、不派发 handler。

P2-1 IdempotencyFilter:
  - filter 顺序从 HIGHEST_PRECEDENCE+5 调整为 +30，位于 DhApiAuthenticationFilter 之后。
  - 幂等 key tenant 只取认证 request attribute；未认证请求不写 synthetic/default tenant key。

P3-1 API.md:
  - Current stage / Next stage 更新为 fix pack / close。
  - 明确 Integration-0 CLOSED / ACCEPTED、header CLOSED、timestamp CLOSED / ACCEPTED、
    code reality audit DONE、GateK-PLAN blocked by this fix pack、Integration-1/runtime NOT STARTED、LIVE DISABLED。
```

### 测试覆盖

```text
LegacyRunControllerSecurityWebMvcTest:
  - anonymous POST /legacy/runs -> 401，未触达 RunService。
  - anonymous GET /legacy/runs/{runId} -> 401，未触达 RunService。
  - authenticated POST 使用 tenant-a，不再是 t-default。
  - authenticated GET 跨 tenant -> 403；同 tenant -> 200。

IdempotencyFilterSecurityTest:
  - IdempotencyFilter order > DhApiAuthenticationFilter order。
  - POST + Idempotency-Key 使用认证 tenant。
  - 未认证请求不写 t-default / synthetic tenant key。

NqFeedbackContractValidationTest / NqFeedbackIdempotencyTest / NqFeedbackControllerWebMvcTest:
  - nested forbidden field reject -> FORBIDDEN_FIELD。
  - nested forbidden capability reject -> FORBIDDEN_CAPABILITY。
  - valid payload still accepted。
  - signed controller request with forbidden payload -> 400 FORBIDDEN_FIELD，未保存 envelope，未路由 handler。
  - INT0 DhNqIntegration0*Test 16/16 回归通过。
```

### 验证记录

```text
Focused:
  mvn "-Dtest=LegacyRunControllerSecurityWebMvcTest,IdempotencyFilterSecurityTest,NqFeedbackControllerWebMvcTest,NqFeedbackContractValidationTest,NqFeedbackIdempotencyTest,DhNqIntegration0*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test
  BUILD SUCCESS；34 tests passed；INT0 16/16 passed。

Full:
  git status --short
    仅本 fix pack 允许范围内文件变更；新增 legacy/idempotency 回归测试文件。
  git diff --check
    exit code 0；仅 LF/CRLF 提示，无 whitespace error。
  git diff --stat
    tracked diff 11 files changed, 558 insertions(+), 28 deletions(-)；另有 2 个新测试文件未计入 tracked stat。
  mvn test
    BUILD SUCCESS；Surefire reports 汇总 307 tests / 0 failures / 0 errors / 4 skipped。
    Skipped 为 Docker/Testcontainers 环境项（JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1）。
  mvn -Pquality validate
    BUILD SUCCESS；checkstyle 0 violations；spotless check passed。
```

### 边界确认

```text
未修改 NQ 仓库；未新增 API；未新增 migration；未做真实 HTTP；未做真实 NQ 调用；
未做真实交易所调用；未新增 RealClient / real provider；未接 AI / LangGraph / LLM；
未启动 Integration-1；未开启 LIVE；未读取或输出真实密钥、token、cookie、API secret、passphrase。
未处理 DecisionOrchestrator；未处理完整 audit/replay 生产模型；未处理多实例集中式 rate limiter。
```

## 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1（replay nonce persistence）

实现 P1-4 三项残留中的第 3 项 **replay nonce persistence**（PostgreSQL-backed），不实现 rate limit、不实现 memory cap、不做 header alignment、不启动 Integration-1、不做真实 NQ 联调。

### 新增文件

```text
dh-app/src/main/resources/db/migration/V4__nq_feedback_replay_nonce.sql      （Flyway V4，新增 dh_nq_replay_nonce 表，不改历史 V1–V3）
dh-infra/src/main/java/.../infra/jdbc/JdbcNonceReplayGuard.java               （JdbcTemplate 实现 NonceReplayGuard，INSERT ON CONFLICT DO NOTHING + fail-closed）
dh-security/src/main/java/.../security/nq/NonceReplayGuardType.java           （guard 选择策略枚举，fail-closed）
dh-security/src/test/java/.../security/nq/NonceReplayGuardTypeTest.java       （选择策略单测：in_memory_guard_only_dev_test 等）
dh-infra/src/test/java/.../infra/jdbc/JdbcNonceReplayGuardTest.java           （Mockito 单测：语义/fail-closed/cleanup）
dh-infra/src/test/java/.../infra/jdbc/JdbcNonceReplayGuardPersistenceTest.java（Testcontainers，Docker-gated：restart simulation / scoping / cleanup）
```

### 修改文件

```text
dh-app/src/main/java/.../config/SecurityWiringConfig.java   （硬编码 InMemoryNonceReplayGuard 改为条件装配：dev/test 允许 in-memory，非 dev/test 默认 jdbc，fail-closed）
dh-app/src/main/resources/application.yml                   （新增 replay.guard-type=jdbc / cleanup-enabled=true 默认）
dh-app/src/main/resources/application-dev.yml               （dev: replay.guard-type=in-memory）
dh-app/src/main/resources/application-prod.yml              （prod: replay.guard-type=jdbc）
dh-infra/pom.xml                                            （新增 dh-security 依赖供实现端口；test 新增 testcontainers junit-jupiter/postgresql + postgresql 驱动）
```

### 验证

```text
命令   mvn test
结果   BUILD SUCCESS；全仓回归全绿；INT0-T01..T15（DhNqIntegration0*Test 16 用例）未被破坏；
       NonceReplayGuardTypeTest 5 / JdbcNonceReplayGuardTest 5 全绿；
       JdbcNonceReplayGuardPersistenceTest 因无 Docker 整类 skip（3）；PostgresContainerSmokeTest 无 Docker skip（既有）
命令   mvn -Pquality validate
结果   FAILURE，来源为既有/环境问题，非本轮改动：
       (1) 聚合模块 checkstyle 读取 config/checkstyle/checkstyle-suppressions.xml 时网络 Connection timed out；
       (2) spotless:check 在多个**未改动**既有文件（AuthContext / TokenVerifier / StaticTokenVerifier /
           HmacNqFeedbackAuthenticator 等）即报 format 违规，说明 spotless 基线本身在本环境不干净。
       已对**本轮自有文件**单独执行 spotless:apply（-DspotlessFiles 限定），未触碰未改动文件。
命令   git diff --check / git status --short / git diff --stat
结果   无 whitespace error；改动仅落在 dh-app/dh-infra/dh-security 允许范围与 docs/current
```

### 严格边界（本轮未违反）

```text
未修改 NQ 仓库；未实现 rate limit；未实现 memory cap；未做 header X-DH-NQ-*/X-NQ-DH-* alignment；
未新增 API / Controller / DTO；未新增 RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所调用；
未下单 / 撤单 / 启停 Paper Run；未改策略 / 风控状态；未读写 NQ DB；未读取或输出真实密钥（测试用固定假值）；
未开启 LIVE；未把 DH 写成 integrated；未把 Integration-1 写成 started；未改历史 migration V1–V3；
未改 contracts/openapi.yaml。
```

### Integration-1 decision

```text
Integration-1 仍 NOT STARTED；Runtime integration NOT STARTED；DH NOT INTEGRATED；AI NOT STARTED；LIVE DISABLED。
P1-4 仍未全部关闭：本轮只关闭 replay nonce persistence，rate limit / memory cap 仍残留。
```

### 下一步

```text
DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2：bounded memory cap（仍不接真实 NQ / 不真实 HTTP / 不 Integration-1）。
```

## 2026-06-12 DH-P1-4-RESIDUAL-FIX-PLAN

输出 DH P1-4 residual（rate limit / memory cap / replay nonce persistence）修复方案，**只做设计不改代码**：不实现限流、不实现 memory cap、不实现 replay nonce persistence、不启动 Integration-1、不做真实 NQ 联调。

### 新增文件

```text
docs/current/DH_P1_4_RESIDUAL_FIX_PLAN.md
```

### 修改文件

```text
docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
```

### 只读核查结论（当前状态）

```text
rate limit：完全缺失。NqFeedbackController -> HmacNqFeedbackAuthenticator 无任何限流；
  NqFeedbackAuthResult 无 429 / RATE_LIMITED。
memory cap：缺失。InMemoryNonceReplayGuard 为无界 ConcurrentHashMap 且无清理（注释已声明生产需替换）；
  InMemoryNqFeedbackEventRepository 及多个 dh-usecase/dh-memory/dh-infra InMemory 仓储均无界。
replay nonce persistence：缺失。InMemoryNonceReplayGuard 单实例内存、无持久化、无 eviction；
  NonceReplayGuard 端口含 expiresAt（TTL 意图）但内存实现未消费。
header：现为 X-DH-NQ-*，Integration-0 冻结 canonical 为 X-NQ-DH-*，对齐属 Integration-1 前置。
```

### 方案要点

```text
rate limit：dh-api 层前置 RateLimiter（端口在 dh-security）；key=source+tenant+route；
  窗口/阈值配置化保守默认；超限 429 RATE_LIMITED（不泄露阈值）；审计 RATE_LIMITED。
memory cap：replay cache 与入站 store 加 TTL + 单 tenant/全局上限 + TTL 优先于容量驱逐；
  超限 fail-closed；审计 MEMORY_CAP_EVICTION / MEMORY_CAP_REJECTED。
replay nonce persistence：候选 A PostgreSQL-backed JdbcNonceReplayGuard（推荐，复用 dh-infra + Flyway）/
  候选 B Redis / 候选 C bounded in-memory（仅 dev/test）；real channel 必须集中式；
  存储不可用 fail-closed；重放 409 REPLAY_DETECTED。
```

### 验证记录

```text
本轮 docs-only：未运行 mvn test；未改代码、测试代码、API、migration、provider、NQ client。
已执行 git status --short / git diff --check / git diff --stat（仅 docs/current）。
```

### 边界确认

```text
未改代码 / 测试代码；未新增 API / migration / Controller / Service / Repository / DTO / RealClient / 真实 Provider。
未做真实 HTTP / 真实 NQ / 真实交易所调用；未启动 Integration-1；未读写 NQ DB；未读取真实密钥；未开启 LIVE。
未把 DH 写成 integrated；未把 Integration-1 写成 started。
只读核查中未输出任何敏感值（仅报告路径、字段名、风险类型）。
```

## 2026-06-12 NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE

输出 DH-NQ Integration-0 safety gate close / acceptance report，正式判定 Integration-0 验收通过并关闭。本轮只做验收文档，不写代码、不改测试代码、不新增 API/migration/RealClient/真实 Provider、不做真实联调。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md
```

### 修改文件

```text
docs/current/STATUS.md / README.md / ROADMAP.md / WORKLOG.md / TESTING.md
```

### 验收结论

```text
Integration-0：PASS / CLOSED / ACCEPTED
Runtime integration NOT STARTED；Integration-1 NOT STARTED；DH NOT INTEGRATED；
AI NOT STARTED；LIVE DISABLED。
已完成链路：三轮审计 + 汇总 → 事实源同步 → 契约冻结 → contract test 矩阵设计 →
  contract test 代码实现（NQ 16 + DH 16）→ implementation review（PASS）→ 本次验收关闭。
契约范围：10 个契约 contract-only / mock-only / test-protected。
测试覆盖：INT0-T01..T15 两侧各 16 用例 passed，含 negative path、audit event shape、
  forbidden side-effect。
Integration-1 前置 blocker：DH P1-4 residual（rate limit / memory cap /
  replay nonce persistence）+ header X-DH-NQ-*/X-NQ-DH-* 对齐 + 真实通道安全前置。
```

### 验证记录

```text
本轮 docs-only：未运行 mvn test；未修改 Java、测试代码、frontend、Python、API、migration、
部署脚本；验收依据引用上一轮已通过的 mvn test BUILD SUCCESS（dh-domain 86 tests）与
implementation review（PASS）。
已执行 git status --short / git diff --check / git diff --stat 核对改动范围（仅 docs/current）。
```

### 边界确认

```text
未修改代码、未修改测试代码、未新增 API、未新增 migration、未新增 DH/NQ RealClient、
未新增真实 Provider。
未做真实 HTTP、未做真实 NQ 调用、未做真实交易所调用、未接 AI、未开启 LIVE、
未读取或输出真实密钥、未读写 NQ DB、无交易副作用。
未把 Integration-0 写成真实集成；未把 Integration-1 写成已开始；未把 DH 写成 integrated；
未把 AI 写成 started；未把 LIVE 写成 enabled。
```

## 2026-06-12 NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL

把已冻结的 Integration-0 contract test matrix（INT0-T01..T15）落成 DH 侧可运行测试代码与脱敏 fixture。本轮只新增 `dh-domain/src/test/**`，不实现真实集成，不修改生产代码，不新增 API/migration/Controller/Service/Repository/DTO，不新增 DH RealClient（RealNqBacktestClient）/真实 Provider，不做真实 HTTP/真实 NQ/真实交易所调用。

### 新增文件（仅 dh-domain/src/test）

```text
dh-domain/src/test/java/.../integration0/support/  9 个 test-only helper
  Int0Contract / Int0Signing / Int0NonceStore / Int0ContractValidator /
  Int0RequestFactory / Int0AuditEvent / Int0ValidationResult /
  Int0SideEffectTracker / Int0CredentialAccessTracker
dh-domain/src/test/java/.../integration0/  3 个测试类（共 16 用例覆盖 INT0-T01..T15）
  DhNqIntegration0ContractValidationTest / DhNqIntegration0SecurityContractTest /
  DhNqIntegration0NoSideEffectTest
dh-domain/src/test/resources/integration0/  10 个脱敏 fixture JSON
```

### 覆盖关系（INT0-T01..T15）

```text
ContractValidation：T02 / T03 / T07 / T08 / T11 / T12
SecurityContract：  T01 / T04 / T05 / T06（test-only 内存 nonce）/ T09 / T10 / T13 / T15
NoSideEffect：      T14（无交易副作用 + 无真实 NQ 调用 + 无凭证访问）
```

### 验证记录

```text
mvn test：BUILD SUCCESS。全仓回归全绿；新增 DhNqIntegration0*Test 16 passed / 0 failed。
ArchitectureTest（ArchUnit 12 条）全绿：integration0 测试包未引入 RealClient / providers /
  RestTemplate / WebClient / OkHttp 等被封堵依赖。
PostgresContainerSmokeTest 因本机无 Docker 自动 skip（既有行为，非本轮引入）。
git diff --check 通过；git status --short 仅命中 dh-domain/src/test/**。
```

### 边界确认

```text
未修改任何 src/main；未新增 API / migration / Controller / Service / Repository / DTO 到 main。
未新增 DH RealClient / NQ RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所调用。
未下单 / 撤单 / 启停 Paper Run / 改策略状态 / 读写 NQ DB / 开启 LIVE；
未读取或输出真实密钥（固定假值 int0-test-secret / t-test-int0 / dh-int0-test）。
未把 Integration-0 写成真实集成；未把 DH 写成 integrated；未把 AI 写成 started；
未把 LIVE 写成 enabled。
```

### Integration-1 前置（不在本轮）

```text
nonce store 为 test-only 内存实现；Integration-1 前必须补持久化 / 集中缓存 nonce。
rate limit、memory cap 仍缺失（DH P1-4 residual），阻塞 Integration-1。
```

## 2026-06-11 NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN

将已冻结的 15 项 mock / contract test plan 拆成 DH 侧详细测试矩阵，定义 mock/stub 行为、DH 侧期望、forbidden side-effect 检查、验收标准与 Integration-0/1 blocker，并产出后续“写测试代码”任务输入材料。本轮只做设计，不写测试代码，不修改 Java/frontend/Python/API/migration/contracts schema，不做真实联调。

### 修改文件

```text
docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md（新增详细矩阵 §6-§12）
docs/current/README.md
docs/current/ROADMAP.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

### 产出内容

```text
DH contract test matrix：INT0-T01..T15，DH 视角；每项含 testId/testName/targetSystem/
  testType/purpose/inputFixture/requiredHeaders/payload/expectedStatus/expectedResult/
  expectedAuditEvent/forbiddenSideEffect/blocksIntegration0/blocksIntegration1/
  implementationOwner/futureCodeLocationSuggestion（建议路径，未创建代码文件）
shared fixture list：18 个 fixtureId（与 NQ 共用，脱敏占位，tenant=t-test-*，无真实密钥）
forbidden side-effect checklist：DH 侧 11 项（无真实 HTTP/RealClient/RealNqBacktestClient/
  下单·撤单·Paper·策略·风控变更/凭证/NQ DB/真实 Provider/LIVE/禁止字段落库/反写 NQ）
Integration-0 acceptance checklist 与 Integration-1 blocker checklist
next implementation task draft：NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL（DH 侧，草案，本轮不执行）
DH P1-4 residual（rate limit / memory cap / replay nonce 持久化）明确仍为 Integration-1 前置
```

### 验证记录

```text
本轮 docs-only，未运行 mvn test；原因：未修改 Java、contracts schema、migration、
测试代码或部署脚本；futureCodeLocationSuggestion 仅为建议路径，未创建任何代码文件。
已执行 git status --short / git diff --check / git diff --stat 核对改动范围。
```

### 边界确认

```text
未修改代码、未新增 API、未新增 migration、未新增 Controller/Service/Repository/DTO、
未修改测试代码、未改部署脚本、未改 contracts/ schema。
未新增 DH RealClient、未新增 NQ RealClient、未新增真实 Provider、未接真实 HTTP、
未接真实交易所、未做真实联调、未开启 LIVE、未读写 NQ DB、未读取凭证。
未把本轮写成 implemented；未把 Integration-0 写成真实集成；未把 DH not integrated
写成 integrated；未把 AI not started 写成 started；未把 LIVE disabled 写成 enabled。
```

## 2026-06-11 NQ-DH-INTEGRATION-0-CONTRACT-FREEZE

冻结 DH-NQ Integration-0 的契约与边界，输出可作为后续 mock / contract test 的稳定依据。本轮只做契约冻结、边界文档、安全策略文档、mock/contract-test 设计，不做真实集成，不修改 Java/API/migration，不新增运行时代码。

### 新增文件

```text
docs/current/DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md
docs/current/DH_NQ_INTEGRATION0_SECURITY_POLICY.md
docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md
```

### 修改文件

```text
docs/current/README.md
docs/current/ROADMAP.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

### 冻结内容

```text
DH → NQ 禁止能力清单（下单/撤单/改订单·策略·风控状态/Paper 启停/读凭证/读写 NQ DB/
  绕过 API·风控·状态机·审计/触发 LIVE/NL·Agent output·feedback 驱动交易）：
  每项 Integration-0=否 / Integration-1=默认否 / LIVE=否 / 需代码硬闸=是 / 需审计=是
DH → NQ 可开放只读/候选能力清单（12 项）：真实 HTTP=否，需认证/签名/tenant/
  requestId/traceId/timestamp/nonce/replay/payload≤64KiB/audit=是，进入执行路径=否
header / auth / replay 契约：X-NQ-DH-Source/Tenant-Id/Request-Id/Trace-Id/Timestamp/
  Nonce/Signature + application/json；±300s 窗口；HMAC-SHA256 候选；64 KiB 上限；
  source allowlist；签名原材料/raw/prompt 不落库
10 个数据契约草案（DHSignalCandidate / DHResearchReport / DHRiskReview /
  DHDecisionSummary / NQFeedbackEvent / NQPaperResultSummary / NQStrategyMetadata /
  NQBacktestSummary / NQErrorResponse / NQDhContractError），contract-only / mock-only
统一禁止字段清单 + NQ 不可信输入处理原则 + 拒绝矩阵（400/401/403/409/413/423/429）
mock / contract test 设计（15 项），只写计划不写代码
Integration-0 验收标准；DH P1-4 残留登记为 Integration-1 前置，不在本轮修复
```

### 验证记录

```text
本轮 docs + contract design only，未运行 mvn test；原因：未修改 Java、契约代码、
migration、测试代码或部署脚本（contracts/ 下 schema 未改，仅新增 docs/current Markdown）。
已执行 git status --short / git diff --check / git diff --stat 核对改动范围。
```

### 边界确认

```text
未修改代码、未新增 API、未新增 migration、未新增 Controller/Service/Repository/DTO。
未新增 DH RealClient、未新增 NQ RealClient、未新增真实 Provider、未做真实联调、
未接真实 HTTP、未接真实交易所、未开启 LIVE。
未读取或输出真实密钥；未读写 NQ DB。
未把本轮写成 implemented；未把 Integration-0 写成真实集成；未把 DH not integrated
写成 integrated；未把 AI not started 写成 started；未把 LIVE disabled 写成 enabled。
```

## 2026-06-11 DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION

把 NQ / DH 三轮只读审计结论与当前阶段事实同步到 DH 事实源文档。本轮只做文档同步，不修改代码，不启动 Integration-0 实现。

### 三轮审计事实

```text
第一轮：NQ 全仓只读审计 completed
第二轮：DH 全仓只读审计 completed
第三轮：NQ-DH 联合边界审计 completed（docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md）
三轮审计汇总 completed
```

### 同步口径

```text
DH Current:       DH audit fixed / ready for Integration-0 planning
DH Next:          Integration-0-PLAN
Provider:         no real provider
NQ client:        no RealClient
Trading ability:  none
Security baseline: P1-1 / P1-2 / P1-3 closed
Remaining issue:  P1-4 residual rate limit / memory cap / replay nonce persistence
                  -> blocks Integration-1, not Integration-0
NQ-DH:            not integrated；runtime connection none；
                  Integration-0 contract freeze allowed；
                  Integration-0 = contract / mock / docs work line, not runtime integration
```

### 修改文件

```text
docs/current/STATUS.md
docs/current/README.md
docs/current/ROADMAP.md
docs/current/WORKLOG.md
docs/current/TESTING.md
AGENTS.md
```

### 验证记录

```text
本轮只改文档，未运行 mvn test；原因：未修改 Java、契约、migration 或部署代码。
已执行 git status --short / git diff --check / git diff --stat 核对改动范围。
```

### 边界确认

```text
未修改任何代码、API、migration、测试或部署脚本。
未新增 RealClient、未新增真实 provider、未做真实联调、未接 NQ 运行时、未开启 LIVE。
未访问 NQ 凭证、未读写 NQ DB。
未把 Integration-0 写成真实集成；未把 NQ integration 写成 started；未把 DH 写成 integrated；未把 LIVE 写成 enabled。
DH 仓库无 CLAUDE.md，未按任务建议新建同名替代文件。
```

## 2026-05-25 DH-REFIT-1-PLAN

完成 DH 文档结构重构的第一批落地。

### 已完成

```text
建立 docs/current/README.md
建立 docs/current/STATUS.md
建立 docs/current/ROADMAP.md
建立 docs/current/WORKFLOW.md
建立 docs/current/WORK_ORDER.md
更新 docs/README.md
保留 docs/codex 为历史辅助区
确认 DH/NQ 边界文档存在
确认 DH Stage1 重构工单存在
```

### 结论

DH 后续采用与 NQ 一致的工作流：

```text
docs/current = 当前事实源
docs/gates = 冻结快照
docs/codex = 历史计划与辅助执行区
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

### 未做

```text
未写业务代码
未修改 NQ 仓库
未接真实模型
未接交易能力
未建设前端页面
```

---

## 2026-05-25 Stage1（Boundary Freeze + Agent Runtime Skeleton）

详细记录见 `docs/current/DH_REFACTOR_STAGE1_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
dh-domain  新增 ResearchRun / AgentTask / TaskNode / AgentRole / AgentArtifact /
           StrategyCandidate / SignalProposal / RiskReview / JudgeDecision /
           DecisionRecommendation / ExperienceEntry / PheromoneEdge /
           NqFeedbackEvent + 5 个状态枚举
dh-memory  ExperienceStore / PheromoneStore / FailureCaseStore /
           MarketRegimeMemory / StrategyPatternMemory + InMemory 实现
dh-eval    CandidateScorer / RiskHeuristicScorer / EvidenceQualityScorer /
           BacktestResultScorer / JudgeAggregator + 规则实现
dh-connector NqBacktestClient / NqFeedbackClient / NqStrategyCandidateMapper /
             NqContractVerifier + Fake/Default 实现
dh-usecase agent runtime 8 个 service + 6 个 repository 端口 + 默认实现 + InMemory 仓储
dh-api     /api/ai/research-runs/...（POST/GET/start/tasks/candidates/judge-decision）
           /api/ai/feedback/nq
dh-app     AgentRuntimeWiringConfig
db/migration V2__dh_agent_runtime.sql（10 张表，全部带 trace_id / status / payload_json）
test       dh-usecase 新增 ResearchRunStage1ClosedLoopTest，覆盖
           create → start → candidate → judge → NQ feedback → experience 闭环
```

### 验收

```text
mvn -pl dh-domain,dh-memory,dh-eval,dh-connector,dh-usecase -am clean test  BUILD SUCCESS
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
仅 dh-app/PostgresContainerSmokeTest 因当前环境无 Docker 而跳过（与本次改动无关）
```

---

## 2026-05-25 Stage1-CLOSE（旧链路收敛 + 文档单源 + ArchUnit 兜底）

详细记录见 `docs/current/STAGE1_CLOSE_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
旧"多模型平台"链路整体 @Deprecated(since="Stage1-CLOSE", forRemoval=true)：
  - domain.run.{Run,RunStatus,RunStep,StepType}
  - usecase.facade.* + impl + dto
  - usecase.run.* + support.*
  - usecase.gate.* + evaluator.*
  - usecase.contract.*
  - dh-providers.{ModelProvider,MockProvider,ModelOutput,ProviderRegistry}
  - dh-app/AppWiringConfig（旧 bean）

api.run.RunController 迁移到 api.legacy.run 子包，@RequestMapping 从 /runs 改为 /legacy/runs；
同步 CreateRunRequest / RunView 迁移。

contracts/openapi.yaml 中 /runs 路径迁到 /legacy/runs 并标 deprecated。

文档单源：根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
全部更新为 "Stage1 completed / Next: Stage2-PoC"。

docs/codex/plans/_active/STATUS.json 切到 2026-05-25_Stage1_agent_runtime_skeleton；
老 M1 mock-provider 计划归档到 docs/codex/plans/_archive/2026-02-04_M1/。

ArchitectureTest 新增 4 条规则：
  ① ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..
  ② ..connector.nq.. 字段/方法名禁止出现 placeOrder|submitOrder|executeOrder|bypassRisk|forceExecute
     （DefaultNqContractVerifier 自身的黑名单豁免）
  ③ ..usecase.agent.. 不依赖 dh-providers
  ④ ..api.. 控制器 @RequestMapping 不能命中 /orders|/trades|/live

dh-eval/pom.xml parent 从 decision-hub 改回 dh-bom，保留 jackson-databind / slf4j-api 依赖。

dep-tree.txt 重新生成。
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
git grep "/runs" 在 dh-api 控制器里只命中 /legacy/runs 与 deprecation 注释。
README / docs/current/STATUS.md / docs/codex/plans/_active/STATUS.json 三处状态措辞一致。
```

### 下一步

进入 Stage2-PoC：NQ 真实事件回流接通 + Kronos/global-stock-data 工具接口预留。

---

## 2026-05-25 Stage1-FREEZE

### 已完成

```text
新建 docs/gates/dh-stage1/ 目录
将 docs/current/ 快照复制到 docs/gates/dh-stage1/
docs/gates/dh-stage1/README.md 声明 Stage1 completed / Stage1-CLOSE completed / mvn test passed / Next: Stage2-PoC
更新 docs/current/STATUS.md：Current stage → Stage1-FREEZE completed
更新 README.md：Current stage → Stage1-FREEZE completed
更新 AGENTS.md：Current stage → Stage1-FREEZE completed
三处状态措辞一致
```

### 未做

```text
未写业务代码
未修改 Stage1 runtime 代码
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 下一步

进入 Stage2-PoC WO：细化工单为可执行步骤。

---

## 2026-05-25 Stage2-PoC PLAN

### 已完成

```text
docs/current/STAGE2_POC_PLAN.md           总体规划（目标、范围、模块设计、风险、验收标准）
docs/current/STAGE2_POC_API_PLAN.md       API 草案（新增端点、请求/响应格式）
docs/current/STAGE2_POC_CONTRACT_PLAN.md  事件契约草案（NQ feedback 信封、Kronos/global-stock-data 接口）
docs/current/STAGE2_POC_DB_PLAN.md        DB 迁移计划（V3 新表 + InMemory->JDBC 替换清单）
docs/current/STAGE2_POC_TEST_PLAN.md      测试计划（6 个新测试 + ArchUnit 新规则）
docs/current/STAGE2_POC_WORK_ORDER.md     工单草案（5 个 Batch + Codex 开工提示词）
更新 docs/current/STATUS.md：Current stage → Stage2-PoC PLAN completed
更新 README.md：Current stage → Stage2-PoC PLAN completed
更新 AGENTS.md：Current stage → Stage2-PoC PLAN completed
```

### 未做

```text
未写业务代码
未修改任何 Java 文件
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 下一步

进入 Stage2-PoC WO：细化工单为可执行步骤，确认每个 Batch 的具体文件清单。

---

## 2026-05-25 Stage2-PoC WO

### 已完成

```text
docs/current/STAGE2_POC_WORK_ORDER.md 重写为可执行版本：
  Batch 1  Contract + Domain
           - 16 个 JSON Schema 清单（NQ envelope + 8 payload + DH backtest + Forecast/Research/Reflection/Checkpoint）
           - 33+ Java 类清单（值对象、枚举、payload）
           - 字段清单（envelope/8 payload/Forecast/Snapshot/Reflection/Checkpoint/DhBacktestRequest）
           - 14 个 enum 清单（NqFeedbackEventType、BacktestVerdict、ForecastHorizon 等）
           - 测试清单（域模型测试 + schema 自检）
           - 不做事项
  Batch 2  NQ Feedback Ingestion
           - Controller/Service/Repository 文件清单（含 Validator/Router/8 Handler）
           - DTO 清单（envelope/accepted/error）
           - 4 步校验规则（envelope schema -> traceId 关联 -> payload 结构 -> 持久化）
           - 幂等规则（eventId 唯一键 + 唯一冲突视为命中）
           - traceId/requestId/correlationId/sourceJobId 规则
           - 测试清单（ContractValidation/Idempotency/HandlerDispatch/WebMvc）
  Batch 3  Forecast / Research Adapter Interfaces
           - interface 清单（ForecastToolPort / ResearchDataAdapter / ResearchSnapshotStore）
           - Fake 实现清单 + 行为约束
           - artifact/snapshot 字段（复用 Batch 1）
           - rawPayloadJson 留档强制规则（6 条）
           - timeout/cache/retry 后置设计（接口不暴露超时参数；Status 枚举留口）
           - 测试清单
  Batch 4  Reflection / Checkpoint / Dynamic Planner
           - 领域模型清单（复用 Batch 1）
           - planner 接口清单（Resolver/Registry/4 个 StrategyHandler/ReflectionCheckpointService）
           - 默认 planner 行为（regime -> strategy 映射表）
           - reflection/checkpoint 字段规范 + 写入规则（JudgeDecision 仍是唯一最终出口）
           - 测试清单（5+4+1 个测试）
           - 禁止引入的 TradingAgents 组件清单（7 项 ❌ + 1 项 ✅）
  Batch 5  JDBC + Tests + Docs
           - V3__stage2_poc_tools.sql 表结构（4 新表 + 2 ALTER）
           - 6 个 InMemory→JDBC 替换 + 5 个新 JDBC 仓储
           - 单元测试矩阵（domain/contracts/connector/usecase/api/infra-jdbc）
           - 集成测试矩阵（Stage2ClosedLoopTest / ApplicationContextLoadsTest / PostgresContainerSmokeTest）
           - ArchUnit 5 条新规则（含旧 5 条共 10 条）
           - docs/current 更新清单 + WiringConfig 装配清单
           - 验收命令

docs/current/STATUS.md：Current stage -> Stage2-PoC WO completed / Next -> Stage2-PoC IMPLEMENT
README.md：Current stage 同步
AGENTS.md：Current stage 同步
```

### 未做

```text
未写业务代码
未修改任何 Java 文件
未修改任何 SQL / OpenAPI / Schema 文件
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
```

### 下一步

进入 Stage2-PoC IMPLEMENT：按 Batch 1 -> 5 顺序实现，每个 Batch 完成后跑一次 mvn test。

---

## 2026-05-25 Stage2-PoC-B1 IMPLEMENT

### 已完成

```text
dh-domain (新增 30 个文件)
  feedback/
    NqFeedbackEventType (8 值 enum)
    NqFeedbackEnvelope (Stage2 信封值对象，含 traceId/requestId/correlationId/sourceJobId/payloadJson)
  feedback/payload/
    AlertLevel (INFO/WARN/ERROR/CRITICAL)
    StabilityCheckResult (STABLE/UNSTABLE/INCONCLUSIVE)
    PaperRunCreatedPayload
    PaperRunStartedPayload
    PaperRunStoppedPayload
    PaperRunDailyReportGeneratedPayload
    PaperRunAlertRaisedPayload
    PaperRunRecoveryEventRecordedPayload
    PaperRunStabilityCheckCompletedPayload
    BacktestResultReadyPayload (持有 requestId 关联 DhBacktestRequest)
  backtest/
    BacktestVerdict (PASS/FAIL/MARGINAL)
    BacktestFrequency (DAILY/HOURLY/MINUTE)
    DhBacktestRequestStatus (DRAFT/QUEUED/ACCEPTED/REJECTED/RESULT_READY/FAILED)
    DhBacktestRequest (含 withStatus 不可变状态前进)
    DhBacktestResultSnapshot
  forecast/
    ForecastTarget (PRICE/VOLATILITY/DIRECTION/VOLUME)
    ForecastHorizon (D1/D5/D20/D60)
    ForecastArtifactStatus (COMPLETED/PENDING/FAILED/TIMEOUT)
    ForecastPoint (confidence [0,1])
    ForecastArtifact (含 rawPayloadJson)
  marketdata/
    MarketDataSource (GLOBAL_STOCK_DATA/INTERNAL_CACHE/FAKE)
    MarketSnapshotStatus (COMPLETED/PENDING/FAILED)
    ExternalMarketSnapshot (含 dataJson + rawPayloadJson)
  reflection/
    ReflectionType (STEP/AGENT/RUN)
    ReflectionEntry
  checkpoint/
    CheckpointType (5 值，含 CANDIDATE_FROZEN/JUDGE_DECISION/PIVOT/ABORT/BACKTEST_REQUESTED)
    CheckpointStatus (DRAFT/RECORDED/DISCARDED)
    CheckpointEntry

contracts/json-schema/ (新增 16 个 JSON Schema 文件)
  nq-feedback-envelope.schema.json
  nq-feedback-paper-run-{created,started,stopped,daily-report-generated,
                        alert-raised,recovery-event-recorded,stability-check-completed}.schema.json
  nq-feedback-backtest-result-ready.schema.json
  dh-backtest-{request,request-accepted,result-snapshot}.schema.json
  forecast-artifact.schema.json
  external-market-snapshot.schema.json
  reflection-entry.schema.json
  checkpoint-entry.schema.json

contracts/openapi.yaml
  追加 components/schemas 23 项（envelope + payload + Dh backtest + 工具产物 + reflection + checkpoint）
  不新增任何 path / endpoint

dh-domain/src/test (新增 8 个测试类，35 个测试用例全绿)
  feedback/NqFeedbackEnvelopeTest             4 cases
  feedback/NqFeedbackPayloadContractTest      8 cases (8 个 payload 各一)
  backtest/DhBacktestRequestContractTest      5 cases
  forecast/ForecastArtifactTest               4 cases
  marketdata/ExternalMarketSnapshotTest       3 cases
  reflection/ReflectionEntryTest              3 cases
  checkpoint/CheckpointEntryTest              3 cases
  contracts/JsonSchemaPresenceTest            5 cases (16 个 schema 存在/可解析/
                                                     additionalProperties=false/
                                                     无 placeOrder/submitOrder/executeOrder/bypassRisk/forceExecute/
                                                     envelope 枚举 8 值)

dh-domain/pom.xml  新增 JUnit Jupiter 测试依赖

docs/current/STATUS.md     Current stage -> Stage2-PoC-B1 IMPLEMENT completed
docs/current/TESTING.md    追加 Stage2-PoC-B1 验收结果
README.md                  同步
AGENTS.md                  同步
docs/current/README.md     同步
```

### 未做（Batch 1 严格边界）

```text
未写 Controller / Service / Repository / JDBC / WiringConfig
未接 NQ / Kronos / global-stock-data
未引入 TradingAgents Python 代码
未做前端
未修改 NQ 仓库
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-domain 新增 35 个测试全部通过
Stage1 闭环测试保持通过
ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B2 IMPLEMENT：NQ feedback ingestion 正式契约（Controller/Service/Validator/Router/8 Handler）。

---

## 2026-05-25 Stage2-PoC-B2 IMPLEMENT

### 已完成

```text
dh-usecase (新增 17 个文件)
  agent/feedback/
    IngestionErrorCode (3 值 enum: UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE)
    IngestionOutcome (3 值 enum: ACCEPTED / DUPLICATE / REJECTED)
    IngestionCommand (HTTP -> 用例层入参值对象，rawEventType 仍为字符串)
    IngestionResult (accepted/duplicate/rejected 工厂)
    ValidationResult (ok/fail 工厂，校验通过时携带 NqFeedbackEnvelope)
    NqFeedbackContractValidator        (接口)
    NqFeedbackEventHandler             (接口)
    NqFeedbackEventTypeRouter          (接口)
    NqFeedbackIngestionService         (接口)
  agent/feedback/impl/
    DefaultNqFeedbackContractValidator (envelope 字段 + sourceSystem + rawEventType -> enum +
                                        schemaVersion >= 1.0.0 + traceId 命中 ResearchRun +
                                        per-eventType 必填字段表)
    DefaultNqFeedbackEventTypeRouter   (EnumMap 全覆盖 8 个 handler，缺一或重复即抛 IllegalStateException)
    DefaultNqFeedbackIngestionService  (幂等优先 -> 校验 -> 保存 -> 派发)
  agent/feedback/handler/
    AbstractNqFeedbackEventHandler     (公共基类：解析 payload + 构造 Stage1 NqFeedbackEvent + append + apply)
    PaperRunCreatedHandler / PaperRunStartedHandler / PaperRunStoppedHandler /
    PaperRunDailyReportGeneratedHandler (PnL>=0 判 positive) /
    PaperRunAlertRaisedHandler (ERROR/CRITICAL 判 negative) /
    PaperRunRecoveryEventRecordedHandler /
    PaperRunStabilityCheckCompletedHandler (UNSTABLE 判 negative) /
    BacktestResultReadyHandler (FAIL 判 negative)

dh-usecase (修改 2 个文件)
  agent/NqFeedbackEventRepository.java                  +saveEnvelope/findEnvelopeByEventId
  agent/inmemory/InMemoryNqFeedbackEventRepository.java +putIfAbsent eventId 唯一键幂等

dh-api (新增 3 个文件 + 修改 2 个)
  feedback/NqFeedbackEnvelopeRequest.java        (Stage2 envelope 请求 DTO，全字段 @NotBlank/@NotNull)
  feedback/NqFeedbackAcceptedResponse.java       (202 响应)
  feedback/NqFeedbackErrorResponse.java          (400 响应)
  feedback/NqFeedbackController.java             升级到 envelope 契约；保留 /api/ai/feedback/nq 路径，无新增 path
  feedback/NqFeedbackRequest.java                @Deprecated(since="Stage2-PoC-B2", forRemoval=true)
  pom.xml                                        加 spring-boot-starter-test (test scope)

dh-app (修改 1 个文件)
  config/AgentRuntimeWiringConfig.java           装配 ObjectMapper / Validator / 8 个 Handler / Router /
                                                 NqFeedbackIngestionService；Stage1 DefaultNqIntegrationUseCase
                                                 与新链路并存（不删除）

dh-usecase 测试 (新增 4 个文件，15 个 cases 全绿)
  agent/feedback/B2TestFixtures.java                          (公共 fixtures)
  agent/feedback/NqFeedbackContractValidationTest.java        7 cases：8 eventType + 6 错误场景
  agent/feedback/NqFeedbackIdempotencyTest.java               3 cases：重放、不同 eventId、REJECTED 不入库
  agent/feedback/NqFeedbackHandlerDispatchTest.java           5 cases：8 全覆盖、独立派发、Stage1 append、raw 保留、重复注册

dh-api 测试 (新增 1 个文件，7 个 cases 全绿)
  feedback/NqFeedbackControllerWebMvcTest.java               202/400 路径 + outcome + 字段分离 + bean 校验

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md / docs/current/README.md
  状态切到 Stage2-PoC-B2 IMPLEMENT completed / Next: Stage2-PoC-B3 IMPLEMENT
```

### 校验链与状态码（与 WO §Batch 2.4 一致）

```text
envelope 必填字段缺失 / sourceSystem != nexus-quant / payload 结构不符  -> 400 INVALID_SCHEMA
rawEventType 不在枚举内                                                  -> 400 UNKNOWN_EVENT_TYPE
schemaVersion < 1.0.0                                                    -> 400 INVALID_SCHEMA
traceId 未命中 ResearchRunRepository                                     -> 400 UNKNOWN_TRACE
eventId 已存在                                                           -> 202 outcome=DUPLICATE, status=RECEIVED
首次合法                                                                 -> 202 outcome=ACCEPTED, status=RECEIVED
```

### B2 follow-up（不在本批回改 Batch 1 领域模型，记录在此）

```text
1. NqFeedbackEnvelope 当前 schemaVersion 字段无 semver 校验；只在 service 层 (DefaultNqFeedbackContractValidator)
   做 >= 1.0.0 比较。若未来允许放宽 schemaVersion 字符串（含 -beta 等后缀），需要域内值对象或专门 SemVer 类型。
2. payload 强类型 value object（PaperRunCreatedPayload 等）当前未由 service 反序列化使用；handler 仅做
   最小 map 解析以触发经验链路。Batch 3+ 可考虑在 service 层增加类型化反序列化以做更细校验。
3. AbstractNqFeedbackEventHandler 用 traceId 作为 runId 走 Stage1 ExperienceFeedbackService 闭环；
   若 Stage2-PoC-B4 / B5 引入 ResearchRun -> runId 单独字段，需把这里改为通过仓库查 runId。
4. NqFeedbackController 仍使用 "t-default" 单 tenant；多租户回到 Stage3+ 再讨论。
```

### 未做（B2 严格边界）

```text
未写 JDBC（Batch 5 处理）
未接真实 NQ HTTP（Fake 保留）
未接 Kronos / global-stock-data
未修改 dh-domain（Batch 1 已固化）
未修改 OpenAPI components/schemas（Batch 1 已落地）
未引入 TradingAgents Python 代码
未做前端
未修改 NQ 仓库
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-usecase B2 新增 15 个测试 + Stage1 闭环测试全部通过
dh-api WebMvc 7 个 cases 全部通过
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B3 IMPLEMENT：dh-connector Forecast / Research Adapter 接口预留 + Fake 实现。

---

## 2026-05-25 Stage2-PoC-B3 IMPLEMENT

### 已完成

```text
dh-connector (新增 8 个文件)
  tools/
    ForecastRequest                  (traceId/symbol/horizon/target；symbol 非空 + horizon/target 非空检查)
    ForecastToolPort                 (Stage2 同步占位端口；rawPayloadJson 非空契约)
  tools/fake/
    FakeForecastToolAdapter          (deterministic mock：固定 generatedAt / FAKE_MODEL_VERSION /
                                      2 个 ForecastPoint；rawPayloadJson 写 mock JSON；
                                      symbol/horizon 非法时 IllegalArgumentException)
  research/
    MarketSnapshotRequest            (traceId/symbols/source/rangeStart/rangeEnd/dataTypes；
                                      symbols 非空 + rangeEnd>=rangeStart 校验)
    ResearchDataAdapter              (Stage2 同步占位端口；rawPayloadJson 非空契约)
    ResearchSnapshotStore            (save / findById / findByTraceId / findBySymbolAndDateRange)
  research/fake/
    FakeResearchDataAdapter          (deterministic mock：固定 fetchedAt / FAKE_SOURCE_VERSION；
                                      dataTypes 空 -> dataJson="{}"；rawPayloadJson 写 mock JSON)
    InMemoryResearchSnapshotStore    (ConcurrentHashMap<snapshotId,…> +
                                      二级索引 traceId -> snapshotId set)

dh-connector (修改 1 个文件)
  pom.xml                            加 junit-jupiter (test scope)

dh-connector 测试 (新增 3 个文件，9 个 cases 全绿)
  tools/fake/FakeForecastToolAdapterTest          3 cases
    ①  happy path -> COMPLETED + 非空 rawPayloadJson + deterministic artifactId
    ②  symbol 为空 / blank -> IllegalArgumentException
    ③  horizon 为空 -> IllegalArgumentException
  research/fake/FakeResearchDataAdapterTest       4 cases
    ①  happy path -> COMPLETED + 非空 rawPayloadJson + deterministic snapshotId
    ②  symbols 为空 -> IllegalArgumentException
    ③  rangeStart > rangeEnd -> IllegalArgumentException
    ④  空 dataTypes -> dataJson="{}" + COMPLETED + 非空 rawPayloadJson
  research/fake/InMemoryResearchSnapshotStoreTest 2 cases
    ①  save -> findById / findByTraceId 命中；null/missing 返回 empty
    ②  findBySymbolAndDateRange 命中/未命中（多 symbol + 日期 overlap）+ start>end 抛错

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md
  状态切到 Stage2-PoC-B3 IMPLEMENT completed / Next: Stage2-PoC-B4 IMPLEMENT
```

### Raw Payload 留档（强制 6 条）

```text
1. FakeForecastToolAdapter 返回的 ForecastArtifact.rawPayloadJson 必填，
   写 {"source":"fake-forecast","symbol":"…","horizon":"…","target":"…"}。
2. FakeResearchDataAdapter 返回的 ExternalMarketSnapshot.rawPayloadJson 必填，
   写 {"source":"fake-research","symbols":[…],"dataTypes":[…],"rangeStart":"…","rangeEnd":"…"}。
3. Fake 不允许空字符串；空 dataTypes 时 dataJson 退化为 "{}"。
4. 数据库列 raw_payload_json TEXT NOT NULL，由 Batch 5 落地。
5. 真实接入失败时，未来适配器 fallback 必须把异常摘要写入 rawPayloadJson。
6. 不允许把敏感凭据写入 rawPayloadJson（Fake 已遵守，无任何 token 字段）。
```

### Timeout / Cache / Retry 后置设计（与 WO §Batch 3.6 一致）

```text
Stage2 不实现 timeout / cache / retry，仅以 javadoc + status 枚举做接口预留：
  ForecastArtifactStatus { COMPLETED, PENDING, FAILED, TIMEOUT }
  MarketSnapshotStatus   { COMPLETED, PENDING, FAILED }

后置接入计划（不在本 WO 内实现）：
  - ForecastToolPort 真实化时，由适配器层通过 Resilience4j 实现 timeout/circuit-breaker/retry。
  - ResearchDataAdapter 真实化时，由适配器层加入磁盘缓存（snapshotId 命中复用）。
  - 接口签名不变，因此 Stage2 的 Fake 与未来真实实现可平滑替换。
```

### 未做（B3 严格边界）

```text
未修改 dh-domain（复用 Batch 1 ForecastArtifact / ExternalMarketSnapshot）
未修改 NQ 仓库
未接真实 Kronos / global-stock-data / NQ API
未引入 Resilience4j / Caffeine / HTTP 客户端
未引入 JDBC（Batch 5 才落地 JDBC ResearchSnapshotStore）
未写 WiringConfig
未引入 TradingAgents Python 代码
未做前端
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-connector Batch 3 新增 9 个 cases 全部通过
Batch 1 / Batch 2 测试保持全绿
Stage1 闭环测试保持通过
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B4 IMPLEMENT：Reflection / Checkpoint / Dynamic Planner。

---

## 2026-05-25 Stage2-PoC-B4 IMPLEMENT

### 已完成

```text
dh-usecase (新增 12 个文件)
  agent/planner/
    PlannerStrategy                       (enum: DEFAULT / BULL_FOCUSED / BEAR_FOCUSED / VOLATILE_DIVERSIFIED；
                                           Stage2-PoC-B4 边界下放在 dh-usecase，避免改动 dh-domain)
    PlannerStrategyResolver               (接口：PlannerStrategy resolve(ResearchRun))
    PlannerStrategyRegistry               (EnumMap-backed；缺 DEFAULT 抛 IAE；重复注册抛 IAE；
                                           handlerFor(null/未知 strategy) 回退 DEFAULT)
    DynamicAgentTaskPlanner               (实现 AgentTaskPlanner：resolver.resolve -> registry.handlerFor ->
                                           handler.buildTask(run, TimeProvider.now()))
  agent/planner/impl/
    DefaultPlannerStrategyResolver        (1. payloadJson.plannerStrategy 显式覆盖（valueOf 失败回退到 regime）；
                                           2. payloadJson.marketRegime 关键字匹配：
                                              volatile / high_volatility 优先于 bull / bear，
                                              使 "bullish but volatile" -> VOLATILE_DIVERSIFIED；
                                           3. 未命中默认 DEFAULT；零 LLM 依赖)
  agent/planner/strategy/
    PlannerStrategyHandler                (接口：strategy() + buildTask(ResearchRun, Instant))
    DefaultPlannerStrategyHandler         (Stage1 6 节点 DAG：SCOUT/ANALYST/STRATEGY/
                                           RISK_REVIEWER/STRATEGY_REVIEWER/JUDGE；
                                           payload tag plannerStrategy=DEFAULT + planSchemaVersion=stage2-b4-v1)
    BullFocusedPlannerStrategyHandler     (SCOUT -> ANALYST -> STRATEGY(primary+secondary) ->
                                           STRATEGY_REVIEWER -> JUDGE；6 节点)
    BearFocusedPlannerStrategyHandler     (SCOUT -> ANALYST -> STRATEGY ->
                                           RISK_REVIEWER(primary+secondary) -> JUDGE；6 节点)
    VolatileDiversifiedPlannerStrategyHandler
                                          (LEADER -> SCOUT -> ANALYST -> STRATEGY ->
                                           (RISK_REVIEWER + STRATEGY_REVIEWER) -> JUDGE；7 节点)
  agent/
    ReflectionEntryRepository             (端口：save / listByRun)
    CheckpointEntryRepository             (端口：save / listByRun)
    ReflectionCheckpointService           (接口：recordReflection / recordCheckpoint /
                                           listReflections / listCheckpoints)
  agent/inmemory/
    InMemoryReflectionEntryRepository     (ConcurrentHashMap<runId, List>；按 stepIndex 排序返回)
    InMemoryCheckpointEntryRepository     (ConcurrentHashMap<runId, List>；按 checkpointIndex 排序返回)
  agent/impl/
    DefaultReflectionCheckpointService    (调用 ReflectionEntry.of / CheckpointEntry.of，
                                           id=IdGenerator.newId()，createdAt=TimeProvider.now()；
                                           ABORT checkpoint + reflection 不替代 JudgeDecision)

dh-usecase 测试 (新增 4 个文件，28 个 cases 全绿)
  agent/planner/PlannerStrategyResolverTest         9 cases
    ①  null run -> DEFAULT
    ②  空 payload -> DEFAULT
    ③  unknown regime 字符串 -> DEFAULT
    ④  bullish/BULL/mid-cap bull cycle -> BULL_FOCUSED
    ⑤  bearish/Bear market -> BEAR_FOCUSED
    ⑥  volatile / high_volatility -> VOLATILE_DIVERSIFIED
    ⑦  "bullish but volatile" -> VOLATILE_DIVERSIFIED（volatile 关键字优先级高于 bull/bear）
    ⑧  显式 plannerStrategy 覆盖 regime
    ⑨  非法显式 plannerStrategy -> 回退到 regime
  agent/planner/PlannerStrategyRegistryTest         5 cases
    ①  注册表缺少 DEFAULT handler -> IllegalArgumentException
    ②  重复 strategy 注册 -> IllegalArgumentException
    ③  handlerFor 返回已注册 handler
    ④  缺失 strategy -> 回退 DEFAULT
    ⑤  4 种 strategy 各自 buildTask 非空 + JUDGE 出现
  agent/planner/DynamicAgentTaskPlannerTest         7 cases
    ①  DEFAULT regime -> DefaultPlannerStrategyHandler 输出 6 节点
    ②  bull regime -> BullFocused 路径 (STRATEGY x2)
    ③  bear regime -> BearFocused 路径 (RISK_REVIEWER x2)
    ④  volatile regime -> VolatileDiversified 7 节点 (LEADER 在前)
    ⑤  显式 plannerStrategy 覆盖 regime
    ⑥  registry 缺失 strategy -> 回退 DEFAULT
    ⑦  每种 strategy 都保留 JUDGE 终点
  agent/reflection/ReflectionCheckpointServiceTest  7 cases
    ①  recordReflection 持久化命中
    ②  listReflections 按 stepIndex 排序
    ③  stepIndex<0 -> IllegalArgumentException
    ④  recordCheckpoint 持久化命中
    ⑤  snapshotJson null -> NullPointerException
    ⑥  ABORT checkpoint + reflection 仍不替代 JudgeDecision (JudgeDecision 仍是唯一终点)
    ⑦  未知 runId -> 空集合

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md / docs/current/README.md
  状态切到 Stage2-PoC-B4 IMPLEMENT completed / Next: Stage2-PoC-B5 IMPLEMENT
```

### Planner 行为表（与 WO §Batch 4.3 一致）

```text
marketRegime 包含 "bullish" / "bull"                  -> BULL_FOCUSED
marketRegime 包含 "bearish" / "bear"                  -> BEAR_FOCUSED
marketRegime 包含 "volatile" / "high_volatility"      -> VOLATILE_DIVERSIFIED（优先级高于 bull/bear）
其它或字段缺失                                          -> DEFAULT
payload.plannerStrategy 显式合法值                     -> 覆盖 regime
payload.plannerStrategy 非法值                         -> 回退到 regime 关键字匹配
registry.handlerFor(未注册 strategy 或 null)            -> DEFAULT
```

### JudgeDecision 仍为唯一终点（与 WO §Batch 4.4 一致）

```text
所有 4 个 StrategyHandler 在任务图末尾必须挂 JUDGE 节点（buildTask 测试覆盖）
ReflectionEntry / CheckpointEntry 只是过程证据，不携带最终决策
ABORT 类型 CheckpointEntry / RUN 级 ReflectionEntry 都不替代 JudgeDecision
```

### 不在 dh-domain 落 PlannerStrategy（B4 边界 trade-off）

```text
原 WO 把 PlannerStrategy 枚举挂在 dh-domain/agent/；
B4 边界要求"不修改 dh-domain"，故 PlannerStrategy 暂放在 dh-usecase/agent/planner/。
Batch 5 评估是否补回 dh-domain 枚举并配套 ArchUnit 规则。
```

### 未做（B4 严格边界）

```text
未引入 LLM client / Python / graph scheduler / 复杂 agent graph runtime
未引入 TradingAgents Python 代码
未修改 dh-domain（复用 Batch 1 ReflectionEntry / CheckpointEntry / AgentTask / TaskNode）
未修改 NQ 仓库
未接真实 NQ API / Kronos / global-stock-data
未引入 JDBC（Batch 5 才落地 JDBC reflection / checkpoint 仓储）
未写 WiringConfig（Stage1 闭环测试仍直连 DefaultAgentTaskPlanner；
                   DynamicAgentTaskPlanner 装配延后到 Batch 5）
未做前端
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-usecase Batch 4 新增 28 个 cases 全部通过
Batch 1 / Batch 2 / Batch 3 测试保持全绿
Stage1 闭环测试保持通过（DefaultAgentTaskPlanner 直连未被破坏）
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B5 IMPLEMENT：JDBC + Tests + Docs（V3 迁移脚本 + 9 个 JDBC 仓储 + ArchUnit 新规则 +
OpenAPI/装配收口）。

---

## 2026-05-25 Stage2-PoC-B5 IMPLEMENT

完成 Stage2-PoC 最后一批收口：V3 migration、JDBC 仓储、WiringConfig、ArchUnit 新规则、OpenAPI 对齐、文档与测试收口。

### 已完成

```text
dh-app/src/main/resources/db/migration/V3__stage2_poc_tools.sql
  4 张新表（CREATE IF NOT EXISTS）：
    dh_forecast_artifacts          (predictions_json jsonb, raw_payload_json jsonb)
    dh_external_market_snapshots   (symbols_json/data_json/raw_payload_json jsonb)
    dh_reflection_entries          (payload_json jsonb，unique(run_id, step_index))
    dh_checkpoint_entries          (snapshot_json jsonb，unique(run_id, checkpoint_index))
  2 张 ALTER（ADD COLUMN IF NOT EXISTS）：
    dh_research_runs               regime, planner_strategy default 'DEFAULT'
    dh_nq_feedback_events          event_id / schema_version / validation_status /
                                   source_job_id / request_id / correlation_id
    + DO 块创建 ux_dh_nq_feedback_events_event_id (event_id is not null)
  comment on table / comment on column 覆盖 trace_id/request_id/correlation_id 等追踪列

dh-infra（新增 5 个 Stage2 JDBC 仓储 + dh-connector 依赖 + jackson + jdbc starter）：
  JdbcNqFeedbackEventRepository      Stage1 append/listByRun + Stage2 saveEnvelope/findEnvelopeByEventId
                                     INSERT 使用 CAST(? AS jsonb)，eventId 幂等：先查询再 catch DuplicateKeyException
  JdbcForecastArtifactRepository     ForecastArtifactStore 实现；predictions_json + raw_payload_json
                                     双 CAST(? AS jsonb)；Jackson ArrayNode 序列化
  JdbcExternalMarketSnapshotRepository ResearchSnapshotStore 实现；symbols_json + data_json +
                                       raw_payload_json 三段 CAST(? AS jsonb)；
                                       findBySymbolAndDateRange 用 symbols_json @> CAST(? AS jsonb)
  JdbcReflectionEntryRepository      payload_json CAST(? AS jsonb)；RowMapper 重建 ReflectionEntry
  JdbcCheckpointEntryRepository      snapshot_json CAST(? AS jsonb)

dh-app config：
  Stage2JdbcWiringConfig             @ConditionalOnProperty(prefix="decisionhub.stage2.jdbc",
                                     name="enabled", havingValue="true", matchIfMissing=false)
                                     装配 5 个 JDBC bean；默认不生效
  AgentRuntimeWiringConfig           5 个 InMemory bean 加 @ConditionalOnMissingBean
                                     允许 JDBC bean 在 enabled=true 时覆盖；
                                     补 DynamicAgentTaskPlanner + Resolver + Registry +
                                     4 个 PlannerStrategyHandler + ReflectionCheckpointService +
                                     ForecastToolPort + ResearchDataAdapter +
                                     InMemoryForecastArtifactStore + InMemoryResearchSnapshotStore

contracts/openapi.yaml：
  POST /api/ai/feedback/nq           对齐 B2 实现（NqFeedbackEnvelope 请求 + {eventId, duplicate} 响应）
  B3/B4 路径以注释占位（forecast/snapshots/reflections/checkpoints controllers 留 VERIFY 上线）

dh-app tests：
  V3MigrationPresenceTest            5 cases（4 张新表 / 2 ALTER / event_id 唯一索引 /
                                     jsonb 列与 comment 保留 / 无 orders|trades|fills|positions|live_）
  ArchitectureTest                   扩到 10 条规则；新增：
                                     - connector.tools !depends ..infra..
                                     - connector.research !depends ..infra..
                                     - domain.{forecast,marketdata,reflection,checkpoint} !depends ..connector..
                                     - usecase.agent.planner !depends providers..
                                     - usecase.agent.feedback !depends providers..

dh-infra tests：
  JdbcNqFeedbackEventRepositoryTest  4 cases：首次写返回 true + CAST(? AS jsonb) /
                                     幂等命中返回 false 且不调 update / unique 竞态返回 false /
                                     null eventId 返回 Optional.empty
  JdbcSqlFragmentsTest               5 cases：reflection/checkpoint/forecast/external_snapshot
                                     insert SQL 命中正确表名 + CAST(? AS jsonb) 次数（>=1/2/3）+
                                     external_snapshot.findById RowMapper 路径

dh-usecase tests：
  Stage2ClosedLoopTest               2 cases：完整 Stage2 闭环——
                                     bullish 走 BULL_FOCUSED / bear 走 BEAR_FOCUSED；
                                     reflections 按 stepIndex 升序、checkpoints 按 checkpointIndex 升序；
                                     JudgeDecision 仍是唯一最终出口

docs/current/STATUS.md / WORKLOG.md / TESTING.md / API.md / DB_SCHEMA.md / README.md / AGENTS.md
  状态切到 Stage2-PoC-B5 IMPLEMENT completed / Next: Stage2-PoC VERIFY
```

### 边界守恒（B5 严格边界）

```text
未修改 NQ 仓库
未接真实 NQ API / Kronos / global-stock-data
未引入 TradingAgents Python 代码
未实现真实下单 / 未绕过 NQ 风控 / 未重写 NQ 回测核心
未建设前端
未改 Stage1 已冻结语义（Stage1ClosedLoopTest 仍直连 DefaultAgentTaskPlanner，绿）
未删除 legacy 旧链路（/legacy/runs 保留）
未引入外部 HTTP 客户端
未把 dh-memory 5 个 Store 替换为 JDBC（留 Stage3）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS

Stage1 闭环 + Batch 1/2/3/4 测试保持全绿
Stage2-PoC-B5 新增 16 cases（V3 5 + JdbcNqFeedback 4 + JdbcSqlFragments 5 + Stage2ClosedLoop 2）全绿
ArchitectureTest 10/10 通过
默认 profile 下 JDBC bean 不装配，InMemory 通路不被破坏
```

### 下一步

进入 Stage2-PoC VERIFY：在装好 Docker 的 CI 环境跑 PostgresContainerSmokeTest，
与 NQ 团队对齐真实 ingest endpoint，灰度切换 decisionhub.stage2.jdbc.enabled=true。

## 2026-05-26 Stage2-PoC VERIFY

对 Stage2-PoC 全量实现做冻结前验证，仅含验证、文档修正与必要小修。详见
`docs/current/STAGE2_POC_VERIFY_REPORT.md`。

### 已完成

```text
mvn test BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
ArchUnit 10/10 通过
硬边界扫描全 PASS（不修改 NQ / 无真实 HTTP / 无下单关键词 / JudgeDecision 唯一出口）
契约一致性：
  - contracts/openapi.yaml /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
    / 400 + NqFeedbackErrorResponse 并补两个 schema
  - docs/current/DB_SCHEMA.md 修正 V2 文件名为 V2__dh_agent_runtime.sql
  - docs/current/API.md 把已实现的 7 条 research-runs 端点移入 "已实现端点"
生成 docs/current/STAGE2_POC_VERIFY_REPORT.md（Verdict: GO，允许进入 FREEZE）
6 份文档状态同步：README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING
```

### 边界与硬约束

```text
不修改 NQ 仓库            不接真实 NQ API
不接真实 Kronos          不接真实 global-stock-data
不引入 TradingAgents Python   不实现真实下单
不绕过 NQ 风控            不重写 NQ 回测核心
不建设前端                不新增 Stage3 功能
```

### 下一步

进入 Stage2-PoC FREEZE：复制 docs/current 至 docs/gates/dh-stage2-poc/ 并锁定，
状态推进至 "Stage2-PoC FREEZE completed / Next: Stage3 PLAN"。

## 2026-05-26 Stage2-PoC FREEZE

把 Stage2-PoC VERIFY 通过的 `docs/current/` 完整快照冻结到 `docs/gates/dh-stage2-poc/`，
状态三处对齐到 "Stage2-PoC FREEZE completed / Next: Stage3-PLAN"。只做文档冻结，不动 Java 业务代码、不动 NQ 仓库、不引入 Stage3 功能。

### 冻结目录

```text
docs/gates/dh-stage2-poc/
  ├── README.md                       冻结声明 + 验收结果 + 交付物 + 边界
  ├── STATUS.md  / WORKLOG.md  / TESTING.md
  ├── API.md     / DB_SCHEMA.md / DH_NQ_INTEGRATION.md
  ├── STAGE2_POC_PLAN.md / STAGE2_POC_WORK_ORDER.md / STAGE2_POC_TEST_PLAN.md
  ├── STAGE2_POC_API_PLAN.md / STAGE2_POC_CONTRACT_PLAN.md / STAGE2_POC_DB_PLAN.md
  ├── STAGE2_POC_VERIFY_REPORT.md
  ├── ARCHITECTURE.md / ROADMAP.md / WORKFLOW.md / WORK_ORDER.md / DOCS_STRUCTURE.md
  └── DH_REFACTOR_STAGE1_*  + STAGE1_CLOSE_WORKLOG.md（沿用上一阶段背景文档）
```

冻结后 `docs/gates/dh-stage2-poc/` 不得修改，后续变更只在 `docs/current/` 进行。

### 验收结果

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
ArchUnit 10/10 PASS
STAGE2_POC_VERIFY_REPORT.md Verdict: GO
```

### 文档同步

```text
README.md                       Current stage: Stage2-PoC FREEZE completed / Next: Stage3-PLAN
AGENTS.md                       同步
docs/current/README.md          同步
docs/current/STATUS.md          同步 + 追加 FREEZE 段
docs/current/WORKLOG.md         追加 2026-05-26 Stage2-PoC FREEZE（本段）
docs/current/TESTING.md         追加 FREEZE 验收记录
```

### 边界与硬约束

```text
不修改 Java 业务代码          不修改 NQ 仓库
不接真实 NQ API              不接真实 Kronos
不接真实 global-stock-data    不引入 TradingAgents Python
不实现真实下单                不绕过 NQ 风控
不重写 NQ 回测核心            不建设前端
不做 Stage3 功能              不修改 migration 语义
不修改 OpenAPI 语义           （仅修文档错字时除外）
```

### 下一步

进入 Stage3-PLAN：仅做 NQ 真实 feedback / backtest request 联调规划；
不允许直接实现 Stage3 功能；不允许修改 NQ 交易核心；不允许接实盘自动交易。

## 2026-05-26 Stage3-PLAN

只做 PLAN 文档，不写 Java 业务代码、不修改 NQ 仓库、不接任何真实外部系统。

### 已完成

```text
新增 6 份 Stage3 规划文档：
  docs/current/STAGE3_PLAN.md                       Stage3 主索引（目标 / 范围 / 风险 / 验收）
  docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md     NQ -> DH feedback 出站事件链路规划
  docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md     DH -> NQ backtest request 入站链路规划
  docs/current/STAGE3_CONTRACT_PLAN.md              端到端契约（status / errorCode / version / 4 字段规则）
  docs/current/STAGE3_TEST_PLAN.md                  测试策略（DH 单测 / NQ 契约 / 联调 / 幂等 / 重试 / 边界）
  docs/current/STAGE3_WORK_ORDER.md                 4 个 IMPLEMENT Batch 工单草案 + Codex 提示词

6 份状态文档同步到 "Stage3-PLAN completed / Next: Stage3-WO"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（122 tests 全绿，作为 PLAN 文档阶段的回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改任何 Java 业务代码           不修改 NQ 仓库
不接真实 NQ API                   不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不实现 Stage3 功能                 不修改 contracts/openapi.yaml 语义
不修改 Flyway migration 语义       不新增 NqFeedbackEventType
```

### 下一步

进入 Stage3-WO：按 docs/current/STAGE3_WORK_ORDER.md 启动 Stage3-Batch1 IMPLEMENT
（Contract Alignment：DH 仓库内补 8 个 Handler 经验沉淀；不联调；不动 NQ）。

---

## 2026-05-26 Stage3-WO

把 Stage3-PLAN 细化为可直接开工的 4 Batch 工单。仅文档，不写 Java 业务代码、不修改 NQ 仓库、不接任何真实外部系统。

### 已完成

```text
重写 docs/current/STAGE3_WORK_ORDER.md：
  §0 通用守则                必读清单 / 验收命令 / 硬边界 / 状态推进
  §1 Batch 1  Contract Alignment
                              文件清单 / contract 清单 / JSON Schema / OpenAPI 影响范围 /
                              DH 侧影响范围 / NQ 侧未来影响范围 / 验收标准 / 禁止事项 /
                              Codex 开工提示词
  §2 Batch 2  NQ Feedback Outbox
                              NQ 侧建议模块 / 建议表（nq_dh_feedback_outbox + nq_dh_feedback_dead_letter）/
                              event outbox 字段映射 / retry 矩阵（1s/5s/30s/5min/1h，attempt 上限 8） /
                              dead-letter / 30 天保留 / audit 对账 / 8 个触发点清单 /
                              不允许触碰的核心模块清单（订单状态机/风控/回测/实盘/账本/资金/主行情/Console） /
                              验收标准 / 禁止事项 / Codex 开工提示词
  §3 Batch 3  DH Backtest Request Adapter
                              DH 侧 adapter 文件清单（DhBacktestRequestService / Repository /
                              RealNqBacktestClient @ConditionalOnProperty / FakeNqBacktestClient
                              @ConditionalOnMissingBean 兜底 / 视情况新增 V4__stage3_dh_outbox.sql） /
                              request DTO -> domain 映射规则 + paramsHash / NQ 接收契约草案 /
                              result snapshot 回传规则 / 24h 幂等规则 + NQ 409 视为成功 /
                              traceId / requestId / correlationId / sourceJobId / eventId 5 字段规则 /
                              ArchUnit 新增规则（非 dh-connector.nq 禁 RestTemplate/WebClient/OkHttp） /
                              验收标准 / 禁止事项 / Codex 开工提示词
  §4 Batch 4  End-to-End Contract Test
                              7 个联调用例（T1-T7：入站正向 / 入站幂等 / 入站契约失败 /
                              出站正向 / 端到端反馈 / 出站幂等 / 4 字段对账） /
                              contract test 清单（DH 仓库内 + 联调 profile + NQ 仓库内）/
                              fake / stub 策略（默认 FakeNqBacktestClient / WireMock / MockWebServer） /
                              失败重试测试（5xx/400/409/429 矩阵） / 幂等测试 / 边界安全测试 /
                              验收命令（mvn 默认 + Postgres + Stage3 联调）/ 出口标准 /
                              禁止事项 / Codex 开工提示词
  §5 Stage3 整体下一步与冻结路径

新增 docs/current/STAGE3_BATCH_PLAN.md：
  §1 总览                    Batch 1-4 范围与依赖关系
  §2 Batch 边界对照表        9 维度 × 4 Batch 矩阵（仓库范围 / 是否写 Java / OpenAPI / Flyway /
                              新增事件类型 / 动 NQ 仓库 / 真实 HTTP / 实盘 / ArchUnit）
  §3 执行顺序与里程碑        M1-M4 + 不变量
  §4 全 Stage3 共同硬边界
  §5 与现有冻结物的关系       docs/gates/dh-stage1 / docs/gates/dh-stage2-poc 保护清单
  §6 Stage3 之后冻结路径      docs/gates/dh-stage3 + DH-FREEZE
  §7 与 Stage3 其他文档的衔接

6 份状态文档同步到 "Stage3-WO completed / Next: Stage3-B1 Contract Alignment"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（122 tests 全绿，作为 WO 文档阶段的回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改任何 Java 业务代码           不修改 NQ 仓库
不接真实 NQ API                   不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不实现 Stage3 功能                 不修改 contracts/openapi.yaml 语义
不修改 Flyway migration 语义       不新增 NqFeedbackEventType
不修改 contracts/json-schema       不联调真实 NQ
```

### 下一步

进入 Stage3-B1 Contract Alignment IMPLEMENT：
按 docs/current/STAGE3_WORK_ORDER.md §1 在 dh-usecase / dh-memory 内补 8 个 Handler 的经验沉淀链路；
仍走 InMemory；不联调；不动 NQ；mvn test 全绿。

---

## 2026-05-26 Stage3-B1 Contract Alignment IMPLEMENT

按用户工单：只在 DH 仓库内对齐契约、schema、OpenAPI、测试与文档，不修改 NQ 仓库、不实现真实联调、
不接真实 HTTP / Kronos / global-stock-data / TradingAgents Python。

### 已完成

```text
contracts/json-schema：
  - nq-feedback-envelope.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 来源说明；
      9 个 envelope 字段 + 1 个 receivedAt 字段全部补 Stage3-B1 字段描述与示例：
        * eventId       UUIDv7 + DH 幂等键说明
        * eventType     8 种枚举映射规则 + 不允许扩展声明
        * occurredAt    业务时间（不是发送时间）
        * sourceSystem  const "nexus-quant"；任何其它值 -> 400 INVALID_SCHEMA
        * sourceJobId   paperRunId / backtestId / alertId 等；DH 只用于对账
        * traceId       与 dh_research_runs.trace_id 关联；失配 -> 400 UNKNOWN_TRACE
        * requestId     由 DH 发起请求 -> 等于原 requestId；否则 NQ 生成
        * correlationId 业务上下文（candidate 的 paper 周期等）
        * schemaVersion semver；MAJOR 升级需双方同步；本地不支持 MAJOR -> 400 INVALID_SCHEMA
        * payloadJson   原始 JSON 字符串；DH 永久留底
        * receivedAt    DH 入口时间戳；NQ 端不写
      required / enum / additionalProperties 等结构性语义不变；
      Stage2 已通过测试 NqFeedbackContractValidationTest / NqFeedbackHandlerDispatchTest /
      NqFeedbackIdempotencyTest / NqFeedbackControllerWebMvcTest 全部保持全绿。

  - dh-backtest-request.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 说明；
      14 个 required 字段 + 2 个 nullable 字段（entryRulesRef / exitRulesRef）全部补 Stage3-B1 描述；
      status 枚举 6 值（DRAFT/QUEUED/ACCEPTED/REJECTED/RESULT_READY/FAILED）注明
      "不含 PLACE/SUBMIT/EXECUTE 等下单语义"；
      frequency 枚举 3 值（DAILY/HOURLY/MINUTE）注明 NQ 不支持 -> 400 UNSUPPORTED_FREQUENCY；
      initialCapital exclusiveMinimum=0 + symbols minItems=1 维持；
      required / enum / additionalProperties 不变。

  - dh-backtest-result-snapshot.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 说明；
      9 个 required 字段 + 5 个 nullable 指标字段（sharpeRatio / maxDrawdown / annualReturn /
      winRate / profitFactor）全部补 Stage3-B1 描述；
      verdict 枚举 3 值（PASS/FAIL/MARGINAL）注明 "DH 不推翻 NQ verdict，
      但 verdict 进入 ExperienceEntry / PheromoneEdge 经验链路"；
      winRate range [0,1] 维持；
      required / enum / additionalProperties 不变。

contracts/openapi.yaml：
  - info.description 新增 Stage3-B1 硬边界声明（不出现 /orders / /trades / /live；
    不出现下单 / 绕风控关键词；不新增事件类型；不修改已落地端点语义；
    JudgeDecision 仍是 DH 唯一最终出口）；
  - components 段保留 NQ 端 endpoint Stage3-B1 planned contract 注释占位
    （注释里描述 POST /api/ai/research/backtest-requests 期望的 request body /
     202 DhBacktestRequestAccepted / 400 6 类 errorCode / 409 / 5xx 矩阵 /
     异步 BACKTEST_RESULT_READY feedback）；
  - paths 段不新增任何路径（Stage3-B1 严格禁止 path 落地，OpenApiContractAlignmentTest
    显式验证 paths 段无 /api/ai/research/backtest-requests / /orders / /trades / /live）；
  - 已落地 /api/ai/feedback/nq 端点语义不变：202 NqFeedbackAcceptedResponse +
    400 NqFeedbackErrorResponse；outcome 枚举（ACCEPTED / DUPLICATE）+
    errorCode 枚举（UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE）保持。

dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/ 新增 4 份契约测试类：
  - NqFeedbackEnvelopeSchemaContractTest               7 cases
      schema 存在 + 可解析 / required 完整 10 字段 / additionalProperties=false /
      eventType.enum 与 NqFeedbackEventType 8 个枚举值一一对应 /
      sourceSystem const "nexus-quant" / schemaVersion semver 正则 /
      黑名单（placeOrder/submitOrder/executeOrder/bypassRisk/forceExecute/
      /orders //trades //live）全无

  - DhBacktestRequestSchemaContractTest                7 cases
      schema 存在 + 可解析 / required 完整 14 字段 / additionalProperties=false /
      status.enum 与 DhBacktestRequestStatus 6 值一一对应 /
      frequency.enum 与 BacktestFrequency 3 值一一对应 /
      initialCapital exclusiveMinimum=0 + symbols minItems=1 / 黑名单全无

  - BacktestResultSnapshotSchemaContractTest           6 cases
      schema 存在 + 可解析 / required 完整 9 字段 / additionalProperties=false /
      verdict.enum 与 BacktestVerdict 3 值一一对应 / winRate range [0,1] / 黑名单全无

  - OpenApiContractAlignmentTest                       9 cases
      openapi.yaml 存在 / /api/ai/feedback/nq 与 NqFeedbackController 一致
      （body=NqFeedbackEnvelope / 202=NqFeedbackAcceptedResponse / 400=NqFeedbackErrorResponse） /
      outcome 枚举含 ACCEPTED + DUPLICATE / errorCode 枚举含
      UNKNOWN_EVENT_TYPE + INVALID_SCHEMA + UNKNOWN_TRACE /
      DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot 组件存在 /
      NqFeedbackEventType 保持 8 种 /
      全文不含 placeOrder | submitOrder | executeOrder | bypassRisk | forceExecute /
      paths 段不含 /orders / /trades / /live /
      Stage3-B1 不允许在 paths 段引入 /api/ai/research/backtest-requests

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  - 总计 151 tests / 0 failures / 0 errors / 0 skipped
    (Stage2 122 + 新增 29；增量分布在 dh-domain：35 -> 64)
  - ArchUnit 10/10 PASS（Stage1-CLOSE 5 + Stage2-PoC-B5 5；本批未新增也未放松）
  - Stage1ClosedLoopTest 1/1 / Stage2ClosedLoopTest 2/2 / NqFeedback* 22/22 全绿

docs 同步：
  - README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md
    全部对齐 "Stage3-B1 Contract Alignment completed / Next: Stage3-B2 NQ Feedback Outbox PLAN"
  - docs/current/STATUS.md §2 新增 Stage3-B1 IMPLEMENT 段
  - docs/current/TESTING.md §1 新增 Stage3-B1 IMPLEMENT 状态行 + §10 验收记录块
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不接真实 NQ API
不接真实 Kronos                    不接真实 global-stock-data
不引入 TradingAgents Python        不实现真实下单
不绕过 NQ 风控                     不重写 NQ 回测核心
不建设前端                         不实现 Stage3-B2/B3/B4 功能
不修改 contracts/openapi.yaml 已落地端点语义
不修改 contracts/json-schema 已落地字段 / required / additionalProperties / 枚举
不新增 NqFeedbackEventType（保持 8 种）
不修改 V1 / V2 / V3 Flyway migration 语义
不新增真实 HTTP client            不修改 NQ 订单 / 风控 / 回测 / 实盘语义
不破坏 ArchUnit 已落地 10 条规则
```

### 下一步

进入 Stage3-B2 NQ Feedback Outbox PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §2 在 DH 仓库内产出 docs/current/STAGE3_NQ_OUTBOX_SPEC.md
（NQ 侧建议模块 + outbox/dead-letter 表 + envelope 映射 + retry 矩阵 +
audit + 8 触发点 + NQ 端硬边界）；零 Java 业务代码改动；NQ 仓库由 NQ 团队后续实施；
DH 不写真实 outbox 客户端；mvn test 全绿。


---

## 2026-05-26 Stage3-B2 NQ Feedback Outbox PLAN

按用户工单：只在 DH 仓库内规划 NQ 侧最小 feedback outbox 规格，落 docs/current/STAGE3_NQ_OUTBOX_SPEC.md。
不修改 Java 业务代码；不修改 NQ 仓库；不接真实联调；不接真实 HTTP / Kronos / global-stock-data /
TradingAgents Python；不新增 Flyway migration；不新增 OpenAPI path。

### 已完成

```text
新增 docs/current/STAGE3_NQ_OUTBOX_SPEC.md（11 段完整规格）：
  §1 目标与边界            定位（NQ outbox 仅"事实回流通道"，不是"交易控制通道"）/ 关键不变量 /
                            硬禁止（不下单 / 不绕风控 / 不修改订单状态 / 不重写回测核心 /
                            不影响 GateJ-FREEZE / outbox 不进入交易同步路径）/ 价值边界声明
  §2 NQ 侧建议模块         建议：nq-ai-contracts / nq-infra / nq-scheduler / nq-app /
                            nq-api admin（admin 命名空间 + 内部鉴权）；
                            不建议放入：nq-core / nq-risk / nq-backtest-kernel /
                            nq-paper-engine / nq-live-engine / nq-ledger / nq-fund-manager /
                            nq-marketdata-core / nq-adapter-* / nq-console-frontend；
                            模块职责矩阵（5 模块 × 5 维度：写 / 读 outbox / 发 HTTP /
                            调用核心 / 备注）
  §3 表结构                主表 nq_ai_feedback_outbox：19 列 + 5 CHECK 约束 + 4 索引 +
                            表/列 COMMENT；status CHECK 5 值；event_type CHECK 8 值；
                            source_system const；schemaVersion semver；payload jsonb；
                            timestamps timestamptz；不存密钥 / token / 账号凭证
                            死信表 nq_ai_feedback_dead_letter：13 列 + UNIQUE event_id + 索引
                            表约束统一规则（event_id 唯一 / 5 状态 CHECK / 8 类型 CHECK /
                            source_system const / schema_version 正则 / JSONB / TIMESTAMPTZ /
                            COMMENT 强制 / 不存密钥）
  §4 8 种事件触发点        每种事件含：NQ 来源模块 / 触发时机（事务提交后）/
                            payload schema 文件 / payload 来源表 / eventId 生成（UUIDv7）/
                            5 字段填充规则 / 是否允许重试与丢弃 / 对交易主链路影响声明
                            统一约束：8 项共同满足条件 + 禁止扩展事件类型清单
  §5 retry / dead-letter / audit
                            5 状态机（PENDING / SENDING / SENT / FAILED / DEAD_LETTER）+
                            状态迁移规则 +
                            退避矩阵（1s / 5s / 30s / 5min / 30min / 1h / 6h，attempt 上限 8，±10% 抖动）+
                            429 退避不计死信上限（遵守 Retry-After）+
                            失败原因分类（HTTP_400 / 401 / 403 / 429 / 5xx / TIMEOUT / NETWORK /
                            PAYLOAD_BUILD；last_error_message 限长 1024 字符且脱敏）+
                            dead-letter 进入条件 + 归档 + 30 天保留 + admin 手动复发 +
                            每日双向对账（NQ outbox sent ⊇ DH events ⊆ NQ outbox sent+dead）+
                            主链路解耦（独立线程池 / 独立连接池 / 不复用交易主链路）
  §6 幂等与追踪规则        eventId / traceId / requestId / correlationId / sourceJobId 五字段语义；
                            不可混用规则；DH 端校验顺序（schema -> sourceSystem -> eventType ->
                            schemaVersion -> eventId 幂等 -> traceId 反查）；时序约束
  §7 HTTP 交互规则         POST /api/ai/feedback/nq 请求结构 + headers + body；
                            期望响应矩阵（202 ACCEPTED|DUPLICATE / 400 + errorCode /
                            401 / 403 / 429 / 5xx / 任何其他状态码）+
                            dispatcher 安全约束白名单 + 黑名单清单 + envelope 冻结要求
  §8 NQ 后续实施 5 个 Batch
                            NQ-1 Contract + DB migration（DTO + Flyway migration，不发 HTTP）
                            NQ-2 Outbox repository + dispatcher fake（JDBC + Fake dispatcher，不发 HTTP）
                            NQ-3 8 事件源写入 outbox（事件源模块 OutboxWriter，仍 Fake dispatcher）
                            NQ-4 Real dispatcher + retry + dead-letter + audit + ArchUnit
                            NQ-5 DH/NQ contract test 联调（NQ test cluster + DH staging，
                            7 个用例 T1-T7，不接实盘）
                            每批含：目标 / 允许改动 / 禁止改动 / 文件清单 / 验收标准
  §9 风险与防护            不影响 GateJ-FREEZE（ArchUnit + 独立 Bean + 独立 ExecutorService）/
                            不进入交易同步链路 / 失败隔离矩阵（outbox 写表 / dispatcher 拉行 /
                            HTTP 发送 / DH 不可用 / outbox 表 IO 慢 5 种失败的影响范围）/
                            DH 不可用降级（PENDING 堆积 + admin pause/resume）/
                            事件重复发送幂等 + 防护手段 /
                            payload schema 演进 semver（PATCH/MINOR 自动；MAJOR 双方协同流程）
  §10 验收标准             本轮（Stage3-B2 PLAN）+ NQ 后续实施 + 硬边界 三段验收
  §11 与 Stage3 其他文档的衔接
                            STAGE3_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN / STAGE3_DH_TO_NQ_BACKTEST_PLAN /
                            STAGE3_CONTRACT_PLAN / STAGE3_TEST_PLAN / STAGE3_WORK_ORDER /
                            STAGE3_BATCH_PLAN

6 份状态文档同步到 "Stage3-B2 NQ Feedback Outbox PLAN completed /
Next: Stage3-B3 DH Backtest Request Adapter PLAN"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §11 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不新增 NqFeedbackEventType（保持 8 种）
本轮只输出 outbox 规格文档
```

### 下一步

进入 Stage3-B3 DH Backtest Request Adapter PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §3 在 DH 仓库内规划 DhBacktestRequestService + RealNqBacktestClient
（默认 disabled；仅 decisionhub.stage3.nq.enabled=true 装配；其余 profile 走 FakeNqBacktestClient）；
本批仍为 PLAN，不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；mvn test 全绿。

---

## 2026-05-26 Stage3-B3 DH Backtest Request Adapter PLAN

按用户工单：只在 DH 仓库内规划 DH -> NQ backtest request adapter，落
docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md。重点是把"DH 请求 NQ 正式回测"设计成
可插拔、默认关闭、可降级、非强依赖的增强能力。
不修改 Java 业务代码；不修改 NQ 仓库；不接真实联调；不接真实 HTTP / Kronos /
global-stock-data / TradingAgents Python；不新增 Flyway migration；不新增 OpenAPI path；
不修改 contracts/openapi.yaml 与 contracts/json-schema/* 语义。

### 已完成

```text
新增 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（14 段完整规格）：
  §1 目标与边界           定位（DH 仅请求，NQ 仍是唯一回测执行方）/
                          关键不变量（DH/NQ 互不强依赖；ResearchRun 主流程无 NQ 时仍能闭环；
                          NQ 主流程无 DH 时仍能完整运行）/
                          硬禁止（不让 DH 执行正式回测 / 不让 DH 直接下单 /
                          不让 DH 绕过 NQ 风控 / 不让 DH 修改 NQ 订单状态 /
                          不让 DH 读写 NQ 交易核心表 / 不让 NQ 强依赖 DH）/
                          价值边界声明（能力 / 失败语义 / 默认安全 / 切换路径）
  §2 可插拔原则           Pluggable Backtest Request Principle 10 条 +
                          三层 gate 模型（stage3.nq.enabled / backtest-request.enabled /
                          fake-mode override，4 状态真值表）+
                          失败降级矩阵（NQ 完全不可用 / NQ 423 / DH 运维关闭）
  §3 建议 DH 侧模块与类   dh-usecase：DhBacktestRequestService / Default / Command /
                                       Result / Outcome / ErrorCode / Repository / InMemory
                          dh-connector：NqBacktestClient 端口 / FakeNqBacktestClient /
                                        DisabledNqBacktestClient / RealNqBacktestClient /
                                        NqBacktestClientProperties / DisabledException
                          dh-infra：（可选）JdbcDhBacktestRequestRepository
                          dh-app：Stage3NqBacktestWiringConfig（三 client 切换）
                          ArchUnit：建议 R11（非 dh-connector.nq 禁 HTTP client）+
                                    R12（usecase.backtest 禁引用 Real client）
  §4 状态模型             9 状态（CREATED / VALIDATED / SUBMITTED / ACCEPTED / RUNNING /
                                  RESULT_READY / FAILED / DISABLED / CANCELLED）+
                          合法迁移表 + 非法迁移拒绝规则 +
                          DH 不允许自行成功的硬规则（RESULT_READY 仅由 NQ 事件驱动）
  §5 DH -> NQ 请求契约    wire-level（contracts/json-schema 14 字段不变）+
                          Stage3-B3 Command 模型字段映射表（Command -> wire 字段映射）+
                          字段语义与限制（requestId / traceId / correlationId / sourceJobId /
                          strategyCandidateId / symbols / market / startTime / endTime /
                          initialCapital / frequency / feeModel / slippageModel /
                          payloadJson）+ 禁止字段清单（凭证 / token / 下单指令）
  §6 NQ 接收契约草案      POST /api/ai/backtest-requests endpoint（NQ 待确认，
                          DH 不落 path）+ auth + headers + 8 种响应矩阵
                          （202 ACCEPTED / 400 INVALID_SCHEMA / 401 / 403 /
                          409 DUPLICATE / 423 AI_DISABLED / 429 / 5xx）+
                          DH 端错误码映射表 + NQ 不允许的行为 + 默认关闭
  §7 三 client 策略       FakeNqBacktestClient（默认；deterministic；不发 HTTP）
                          DisabledNqBacktestClient（DH gate 关闭；返回 DISABLED；不抛异常）
                          RealNqBacktestClient（B3-3 IMPL 阶段；mTLS / token；timeout；
                          仅 dh-connector.nq 内；ArchUnit 守门）
                          + 四种 profile 切换路径（prod 默认 / staging / prod 启用 / 应急关闭）
  §8 幂等与重试规则       requestId 幂等键 + 24h 短路（paramsHash = sha256 of 9 字段）+
                          NQ 409 DUPLICATE 视为成功 +
                          退避矩阵（1s / 5s / 30s / 5min / 30min / 1h / 6h，attempt 上限 8，
                          ±10% 抖动）+
                          429 不计死信上限 + 4xx 永久终态 +
                          重试不阻塞 ResearchRun 主流程 +
                          result snapshot 三字段对齐（requestId / correlationId / sourceJobId）
  §9 消费 result snapshot DhBacktestResultSnapshot 来源唯一（仅 ingest 链路）+
                          经验沉淀路径（traceId 反查 -> requestId 反查 -> candidateId 反查 ->
                          落 snapshot -> ExperienceEntry / PheromoneEdge 更新 ->
                          dh_checkpoint_entries）+
                          缺字段处理 + DH 不覆盖 NQ verdict / 不反向同步
  §10 配置建议            DH application.yml（decisionhub.stage3.nq.*）+
                          NQ application.yml（nq.ai.*）+ prod 默认值（双方全 false）+
                          配置敏感性约束（凭证必须从环境变量 / Vault 注入）
  §11 测试规划            8 个测试类（FakeNqBacktestClientTest /
                          DisabledNqBacktestClientTest /
                          RealNqBacktestClientDisabledByDefaultTest /
                          DhBacktestRequestServiceTest /
                          DhBacktestRequestIdempotencyTest /
                          DhBacktestResultSnapshotConsumptionTest /
                          NoNqDependencyStartupTest /
                          NoDangerousEndpointContractTest）+
                          测试目标矩阵 + ArchUnit R11/R12 配套规则
  §12 后续 IMPL 5 个 Batch B3-1 Contract + Service Interface（先落用例层接口与 DTO）
                          B3-2 Fake / Disabled Client（三 client 切换骨架）
                          B3-3 Optional Real Client Skeleton（默认关闭，mock HTTP 测试）
                          B3-4 Result Snapshot Consumption（ingest 命中后经验沉淀）
                          B3-5 Tests + Docs（8 测试类 + 6 状态文档同步）
                          每批含：目标 / 允许改动 / 禁止改动 / 文件清单 / 验收标准
  §13 验收标准            本轮（PLAN）+ 后续 IMPL + 硬边界三段
  §14 与 Stage3 其他文档的衔接
                          STAGE3_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN / STAGE3_NQ_OUTBOX_SPEC /
                          STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_CONTRACT_PLAN /
                          STAGE3_TEST_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN

6 份状态文档同步到 "Stage3-B3 DH Backtest Request Adapter PLAN completed /
Next: Stage3-B4 End-to-End Contract Test PLAN"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §12 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不新增 NqFeedbackEventType（保持 8 种）
本轮只输出 adapter 规划文档
```

### 下一步

进入 Stage3-B4 End-to-End Contract Test PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §4 规划 NQ test cluster + DH staging 端到端联调用例（T1-T7）；
本批仍为 PLAN，不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；不接实盘；mvn test 全绿。

---

## 2026-05-26 Stage3-B4 End-to-End Contract Test PLAN

按用户工单：只在 DH 仓库内规划 DH/NQ 未来端到端契约测试方案，落
docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md。本轮只写文档，不修改 Java 业务代码，
不修改 NQ 仓库，不真实联调，不接实盘。

### 已完成

```text
新增 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（11 段完整规格）：
  §1 目标与边界           验证目标限定契约面（不验证实盘收益 / NQ 内部回测核心 / LLM 真实推理）+
                          硬边界（NQ test cluster 与生产物理隔离 / DH staging 与生产物理隔离 /
                          所有测试 fake-paper-backtest / 不接实盘 / 不自动下单 / 不自动发布）+
                          触发联调的前置条件（NQ-1..NQ-4 + B3-1..B3-5 + 双方 oncall 评审 +
                          默认 profile mvn test 通过 + prod 严格保持 enabled=false）
  §2 测试环境规划         DH staging（profile stage3-test 或 staging / 独立 namespace + VPC /
                          独立 PostgreSQL / 独立监控 / tenantId 前缀 t-test-* /
                          traceId / correlationId 前缀 stage3- / symbols 测试白名单 /
                          禁止实盘配置）
                          NQ test cluster（profile local/test/stage3-test / AI 默认全关 /
                          paper 与 backtest 可用 / live trading 必须关闭 / tenantId
                          前缀 t-test-* / 联调期间持续监控）
                          网络与配置（DH/NQ 双向 base url + timeout + retry + auth token
                          via Vault 24h 自动轮换 + disabled / fake mode + 出站白名单 +
                          回滚预案：失败 1h 内 enabled=false）
  §3 7 个端到端契约测试用例 每条用例含目标 / 前置 / 步骤 / 期望结果 / 失败回退：
                          T1 PAPER_RUN_CREATED feedback：单事件正向链路 + 5 字段对账
                          T2 PAPER_RUN_ALERT_RAISED 幂等：重放 → 202 DUPLICATE +
                              ack 丢失模拟 + dh_nq_feedback_events 唯一索引保障
                          T3 BACKTEST_RESULT_READY 结果消费：完整 ingest 校验链 +
                              DhBacktestResultSnapshot 落 + ExperienceEntry / PheromoneEdge
                              更新 + DH 不反向修改 NQ + JudgeDecision 仍是唯一最终出口
                          T4 backtest request accepted：DH → NQ 主路径 + 202 + jobId 持久化 +
                              状态机 SUBMITTED → ACCEPTED + NQ 风控正常评估（DH 不绕过）
                          T5 disabled mode：DisabledNqBacktestClient 装配 / 零 HTTP 出站 /
                              ResearchRun 不阻塞 / JudgeDecision 仍可生成
                          T6 outbox retry / dead-letter：DH 临时 5xx → NQ 退避矩阵重试 →
                              最终 SENT 或 DEAD_LETTER（attempt=8）+ 主链路保护断言
                          T7 安全边界扫描：关键词 / 配置 / 双向无依赖启动 / 凭证不泄露 /
                              实盘隔离（A 关键词扫描 / B 配置扫描 / C NQ 无 DH 仍可启动 /
                              D DH 无 NQ 仍可启动 / E 凭证不泄露 / F 实盘隔离）
  §4 10 类 Contract Test  JSON Schema / OpenAPI / HTTP status matrix / Error code matrix /
                          Idempotency / Retry + dead-letter / Disabled startup /
                          No dangerous endpoint / Trace correlation / Regression
                          每类含范围 / 检查项 / 落点 / 基线
  §5 测试数据与追踪规则   5 字段（eventId / requestId / traceId / correlationId / sourceJobId）
                          生成规则 + 联调前缀（stage3-{用例}-{seq}-{type}）+
                          payload / rawPayloadJson 留档（无凭证）+ deterministic 数据
                          （固定 universe / 时间窗口 / capital / paramsHash / sourceJobId 前缀）+
                          tenantId t-test-* 严格遵守 + 30 天保留供排错
  §6 验收命令规划         DH 默认 profile / CI Docker / Stage3 联调（ENABLED_STAGE3=true）三段 +
                          NQ 仓库默认 / 联调两段 + 端到端联调 19 步 checklist
                          （联调启动前 5 步 + 执行 10 步 + 结束后 4 步 + Verdict GO / NO-GO）
  §7 失败处理规则         DH 入站 10 种响应 → NQ outbox 行为映射表
                          DH 出站 10 种响应 → DH 状态机切换映射表
                          联调用例失败处理（6 优先级排查 + 1 小时回滚 + 三轮失败回 Batch PLAN）
                          NQ 主链路保护（订单 / 风控 / 账本 / 实盘 / GateJ-FREEZE 全程不退化）+
                          DH 主链路保护（NQ 不可达时 Fake 闭环 + Stage1/2ClosedLoop 全绿）
  §8 后续 Stage3-B4 IMPL 5 个 Batch
                          B4-1 DH contract test suite（@EnabledIfEnvironmentVariable 隔离）
                          B4-2 NQ contract test fixture plan（NQ 团队执行）
                          B4-3 Stub server / fake server（WireMock / MockWebServer）
                          B4-4 Disabled mode startup test（DH 启动不依赖 NQ）
                          B4-5 End-to-end dry-run checklist（T1-T7 联调 + STAGE3_VERIFY_REPORT.md）
                          每批含：目标 / 允许 / 禁止 / 文件清单 / 验收标准
                          + Batch 依赖与执行顺序
  §9 验收标准             本轮（PLAN）+ 后续 IMPL + 硬边界三段
  §10 与 Stage3 其他文档的衔接
                          STAGE3_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN /
                          STAGE3_CONTRACT_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN /
                          STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_NQ_OUTBOX_SPEC /
                          STAGE3_DH_BACKTEST_ADAPTER_SPEC / STAGE3_E2E_CONTRACT_TEST_SPEC（本文件）/
                          STAGE3_TEST_PLAN
  §11 Stage3-PLAN-FREEZE 衔接
                          10 份 STAGE3_*.md PLAN 文档清单 + FREEZE 路径 +
                          IMPL → FREEZE → DH-FREEZE 路径声明

6 份状态文档同步到 "Stage3-B4 End-to-End Contract Test PLAN completed /
Next: Stage3-PLAN-FREEZE"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §13 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不启动真实联调
不接真实 Kronos                    不接真实 global-stock-data
不引入 TradingAgents Python        不实现真实下单
不绕过 NQ 风控                     不重写 NQ 回测核心
不建设前端                         不自动下单 / 不自动发布策略
NqFeedbackEventType 保持 8 种
本轮只输出端到端契约测试规划文档
```

### 下一步

进入 Stage3-PLAN-FREEZE：
评审 10 份 STAGE3_*.md 文档（STAGE3_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN /
STAGE3_CONTRACT_PLAN / STAGE3_TEST_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN /
STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_NQ_OUTBOX_SPEC / STAGE3_DH_BACKTEST_ADAPTER_SPEC /
STAGE3_E2E_CONTRACT_TEST_SPEC）口径一致性；视需要在 docs/gates/dh-stage3-plan/ 落盘冻结快照；
切到 "Stage3-PLAN-FREEZE completed / Next: Stage3-B1 IMPLEMENT" 体例。

---

## 2026-05-26 Stage3-PLAN-FREEZE

按用户工单：冻结 Stage3 规划成果。
检查 10 份 STAGE3_*.md 文档口径一致性，将当前 docs/current 快照复制到
docs/gates/dh-stage3-plan/，并同步状态文档。
本轮只做文档冻结，不写 Java 业务代码，不修改 NQ 仓库，不实现 Stage3 功能。

### 已完成

```text
一致性核查（10 份 STAGE3_*.md）：
  9 条核心原则全部覆盖、口径一致；无措辞修订需要：

  原则                                                             覆盖
  NQ 与 DH 对接必须可插拔、默认关闭、非强依赖                       ✅
  NQ without DH 必须完整可运行                                    ✅
  DH without NQ 必须可运行 fake / disabled 闭环                   ✅
  NQ -> DH feedback 走旁路 outbox，不阻塞交易主链路                ✅
  DH -> NQ backtest request 是可选增强能力，默认 disabled/fake     ✅
  NQ 仍然是唯一正式回测执行方                                      ✅
  DH 不直接下单、不绕风控、不改订单状态、不重写回测核心             ✅
  Stage3 只允许规划真实联调，不允许接实盘                          ✅
  Stage3 后续 IMPLEMENT 必须逐 Batch 执行                          ✅

落盘冻结快照：
  - mkdir -p docs/gates/dh-stage3-plan/
  - cp -r docs/current/. docs/gates/dh-stage3-plan/
  - 33 个文件已复制（含 10 份 STAGE3_*.md：STAGE3_PLAN / STAGE3_WORK_ORDER /
    STAGE3_BATCH_PLAN / STAGE3_CONTRACT_PLAN / STAGE3_TEST_PLAN /
    STAGE3_NQ_TO_DH_FEEDBACK_PLAN / STAGE3_DH_TO_NQ_BACKTEST_PLAN /
    STAGE3_NQ_OUTBOX_SPEC / STAGE3_DH_BACKTEST_ADAPTER_SPEC /
    STAGE3_E2E_CONTRACT_TEST_SPEC）

冻结声明（docs/gates/dh-stage3-plan/README.md 顶部）：
  - Stage / Status / Source snapshot / Verification / ArchUnit / Scope / Next
  - 8 行 banner + 完整 README.md 主体
  - 明确"本快照是 Stage3 规划冻结，不代表真实联调已完成"
  - 明确"后续 Stage3-B1 IMPLEMENT 仍需单独开工"
  - 明确"NQ 仓库当前未被修改"
  - 明确"实盘 / 自动下单 / 绕风控 / 回测核心改写仍然禁止"
  - 加 Stage3 后续不允许做的事清单（14 条）
  - 加 Stage3 后续允许进入的工单清单（B1 ✅ / B2 / B3 / B4 / Stage3-FREEZE / DH-FREEZE）
  - 加验收命令与结果 + 模块测试分布表
  - 加冻结快照文件清单
  - 加 10 份 STAGE3_*.md 一致性核查表
  - 加 Stage3-PLAN 交付物清单（PLAN / B1 IMPL / B2 PLAN / B3 PLAN / B4 PLAN 五段）
  - 末尾保留原 docs/current/README.md 内容（供历史比对）

6 份状态文档同步到 "Stage3-PLAN-FREEZE completed / Next: Stage3-B1 IMPLEMENT"：
  - README.md
  - AGENTS.md
  - docs/current/README.md（含 docs/codex/plans/_active 引用保持）
  - docs/current/STATUS.md（第 2 节追加 Stage3-PLAN-FREEZE 段；第 4 节"下一阶段"重写）
  - docs/current/WORKLOG.md（本段）
  - docs/current/TESTING.md（§1 状态行 + §14 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  151 tests / 0 failures / 0 errors / 0 skipped / ArchUnit 10/10
  Stage1ClosedLoopTest + Stage2ClosedLoopTest + Stage3-B1 29 contract tests 全部保持回归基线
```

### 严格边界（本阶段未违反）

```text
不修改 Java 业务代码                  不修改 NQ 仓库
不修改 contracts/openapi.yaml         不修改 contracts/json-schema
不新增 Flyway migration               不新增 OpenAPI path
不接真实 NQ                           不接真实 Kronos
不接真实 global-stock-data            不引入 TradingAgents Python
不启动真实联调                        不接实盘
不自动下单                            不自动发布策略
不绕过 NQ 风控                        不重写 NQ 回测核心
不建设前端                            冻结后不得修改 docs/gates/dh-stage3-plan/ 内容
```

### 下一步

进入 **Stage3-B1 IMPLEMENT**（说明：Stage3-B1 Contract Alignment IMPLEMENT 已于 2026-05-26 完成，
本阶段命名沿用 ROADMAP 体例；实际下一步进入 Stage3 后续 IMPLEMENT 工单）：

```text
Stage3-B2 NQ Feedback Outbox IMPL          NQ 团队按 STAGE3_NQ_OUTBOX_SPEC §8（NQ-1..NQ-5）实施
                                            （DH 仓库不动；本仓库仅作为契约参考源）
Stage3-B3 DH Backtest Request Adapter IMPL DH 团队按 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12
                                            （B3-1..B3-5）实施
Stage3-B4 End-to-End Contract Test IMPL    DH+NQ 联调，按 STAGE3_E2E_CONTRACT_TEST_SPEC §8
                                            （B4-1..B4-5）实施
Stage3-VERIFY                               B4-5 联调 GO 后；产出 STAGE3_VERIFY_REPORT.md
Stage3-FREEZE                               VERIFY GO 后；docs/current/* 复制到 docs/gates/dh-stage3/
DH-FREEZE                                   Stage3-FREEZE 后；Decision Hub Agent Decision Layer v1
                                            长期维护态
```

---

## 2026-05-26 Stage3-NEXT-STATUS-FIX

按用户工单：修正 Stage3-PLAN-FREEZE 后的 Next stage 指向。
Stage3-B1 Contract Alignment 已于 2026-05-26 完成，原状态文档把 Next 写成 Stage3-B1 IMPLEMENT
会误导后续接手人重复开工。改为指向 Stage3-B2 NQ Feedback Outbox IMPL，并补充约束。

### 修正内容

```text
目标状态：
  Current stage: Stage3-PLAN-FREEZE completed
  Next stage:    Stage3-B2 NQ Feedback Outbox IMPL

5 份状态文档同步：
  - README.md                 当前阶段段 + 当前下一步段
  - AGENTS.md                 §4 当前阶段段
  - docs/current/README.md    顶部 banner + 下一步只允许进入段
  - docs/current/STATUS.md    顶部 banner + §4 下一阶段段（重写为约束清单）
  - docs/current/WORKLOG.md   本段（追加 FIX 记录）

新增约束清单（落在 docs/current/STATUS.md §4 + 5 份文档执行口径段）：
  - Stage3-B1 Contract Alignment already completed (2026-05-26).
    不要重复开工 Stage3-B1 IMPLEMENT；本批已落 4 份 contract 测试类（29 cases）+
    contracts/openapi.yaml + contracts/json-schema/* 描述对齐。
  - Stage3-B2 touches NQ and must not start until NQ GateJ-FREEZE is complete
    or explicitly approved on an isolated branch.
    B2 是 NQ 仓库工作（按 STAGE3_NQ_OUTBOX_SPEC §8 / NQ-1..NQ-5）；
    NQ GateJ-FREEZE 未完工前 B2 不允许启动；
    即便有隔离分支启动也必须遵守 STAGE3_NQ_OUTBOX_SPEC §1.3 / §9 全部硬边界。
  - DH-side Stage3-B3 can proceed independently with fake/disabled mode
    if NQ work is blocked.
    Stage3-B3 DH Backtest Request Adapter IMPL 可在 DH 仓库独立推进
    （按 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12 / B3-1..B3-5），
    默认 profile FakeNqBacktestClient 兜底，不接真实 NQ。
```

### 严格边界（本阶段未违反）

```text
不修改 Java 业务代码                  不修改 NQ 仓库
不修改 contracts/openapi.yaml         不修改 contracts/json-schema
不新增 Flyway migration               不实现 Stage3 功能
不修改 docs/gates/dh-stage3-plan/     （冻结快照不可改）
不修改 10 份 STAGE3_*.md PLAN 文档    （仅状态文档指向修正）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped / ArchUnit 10/10
（与 Stage3-PLAN-FREEZE 基线完全一致，无回归）
```

### 下一步

执行口径已澄清：
- 推荐路径：Stage3-B3 DH Backtest Request Adapter IMPL（DH 独立推进，fake / disabled 模式）
- NQ 路径：Stage3-B2 NQ Feedback Outbox IMPL（等待 NQ GateJ-FREEZE 或隔离分支批准后启动）
- Stage3-B4 / VERIFY / FREEZE / DH-FREEZE 依次按 STAGE3_WORK_ORDER.md 推进

---

## 2026-05-26 Stage3-B3 DH Backtest Request Adapter IMPL

按用户工单：实现 DH -> NQ Backtest Request Adapter 的 DH 侧可插拔骨架。
本轮只允许 fake / disabled，不允许真实 NQ HTTP 调用。
Stage3-B3 was executed before B2 because B2 touches NQ.

### 已完成

```text
1) dh-connector（新建 2 类 + 修改 2 类）

  com.guidinglight.decisionhub.connector.nq:
    - NqBacktestSubmitStatus.java        枚举 4 值（ACCEPTED / DUPLICATE / DISABLED / FAILED）
    - NqBacktestSubmitResult.java        5 工厂方法 + 6 字段
    - NqBacktestClient.java (修改)        扩 typed submit(DhBacktestRequest) 默认方法；旧 Map 接口保留
  com.guidinglight.decisionhub.connector.nq.fake:
    - FakeNqBacktestClient.java (修改)    扩 typed deterministic submit；
                                          jobId = "fake-job-" + sha256(requestId).take(16)；Clock 可注入
    - DisabledNqBacktestClient.java       新增；submit 返回 outcome=DISABLED + errorCode=DH_DISABLED；
                                          不抛 RuntimeException；不发 HTTP

2) dh-usecase（新建 9 类）

  com.guidinglight.decisionhub.usecase.agent.backtest:
    - DhBacktestRequestService.java                       端口接口
    - DhBacktestRequestCommand.java                       Builder 风格 12 字段
    - DhBacktestRequestResult.java                        5 工厂方法
    - DhBacktestRequestOutcome.java                       6 值枚举
    - DhBacktestRequestErrorCode.java                     17 值枚举 + isRetryable
    - DhBacktestRequestRepository.java                    端口接口 + RequestSnapshot 内部类
  com.guidinglight.decisionhub.usecase.agent.backtest.impl:
    - DefaultDhBacktestRequestService.java                默认实现
      流程：校验入参 -> 计算 paramsHash (sha256) -> 24h 短路 -> 生成 requestId -> 写仓储 ->
            调 NqBacktestClient.submit(typed) -> 映射 status -> 更新仓储 -> 返回 typed Result
      硬约束：不抛 RuntimeException 中断 caller；状态机不允许从 service 内自动生成 RESULT_READY
  com.guidinglight.decisionhub.usecase.agent.backtest.inmemory:
    - InMemoryDhBacktestRequestRepository.java            InMemory 实现（24h 短路）

3) dh-app（新建 2 类 + 修改 1 类）

  com.guidinglight.decisionhub.config:
    - NqBacktestClientProperties.java     @ConfigurationProperties("decisionhub.stage3.nq")
    - Stage3NqBacktestWiringConfig.java   互斥 SpEL 三层 gate 装配
    - AgentRuntimeWiringConfig.java (修改) 移除 nqBacktestClient bean（由 Stage3 装配接管）

4) ArchUnit 扩到 12 条（dh-app ArchitectureTest）

  新增规则 R11 stage3B3_rule11_httpClientOnlyInsideConnectorNqOrAppConfig
    业务模块禁止直接依赖 RestTemplate / WebClient / OkHttp3 / HttpURLConnection；
    仅允许在 ..connector.nq.. 或 ..config.. 内出现
  新增规则 R12 stage3B3_rule12_useCaseBacktestDoesNotDependOnRealClientOrProviders
    usecase.agent.backtest 不依赖 RealNqBacktestClient（占位 + 防御）与 providers

5) 8 个 B3 测试类共 39 cases 全绿

  dh-connector:
    - FakeNqBacktestClientTest                  5 cases
    - DisabledNqBacktestClientTest              5 cases

  dh-usecase:
    - DhBacktestRequestServiceTest              5 cases
    - DhBacktestRequestIdempotencyTest          3 cases
    - DhBacktestResultSnapshotConsumptionTest   4 cases

  dh-app:
    - RealNqBacktestClientDisabledByDefaultTest 4 cases
    - NoNqDependencyStartupTest                 4 cases
    - ArchitectureTest                          扩 R11 / R12（总 12 条）

  dh-domain:
    - NoDangerousEndpointContractTest           6 cases

mvn test BUILD SUCCESS / 190 tests (151 -> 190, +39) / 0 failures / 0 errors / 0 skipped
ArchUnit 12/12 PASS
Stage1ClosedLoopTest + Stage2ClosedLoopTest + Stage3-B1 29 contract tests 全部保持回归基线

6) 文档同步（6 份状态文档）
  - README.md / AGENTS.md / docs/current/README.md：banner + 当前阶段 + 下一步段
  - docs/current/STATUS.md：顶部 banner + §2 新增 Stage3-NEXT-STATUS-FIX + Stage3-B3 IMPL 段 + §4 重写
  - docs/current/WORKLOG.md：本段
  - docs/current/TESTING.md：§1 状态行 + §15 验收记录块
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不接真实 NQ API
不创建真实 HTTP client             不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不改 NQ 订单 / 风控 / 账本 / 实盘 / 回测核心语义
RealNqBacktestClient 未实现（fake-mode=false 仍走 Fake 兜底）
不修改 contracts/openapi.yaml      不修改 contracts/json-schema
不新增 Flyway migration            不新增 OpenAPI path
```

### 下一步

进入 **Stage3-B2 NQ Feedback Outbox IMPL**（blocked until NQ GateJ-FREEZE 或隔离分支批准）：
- B2 是 NQ 仓库工作（按 STAGE3_NQ_OUTBOX_SPEC §8 / NQ-1..NQ-5）；
- NQ GateJ-FREEZE 未完工前 B2 不允许启动；
- 即便有隔离分支启动也必须遵守 STAGE3_NQ_OUTBOX_SPEC §1.3 / §9 全部硬边界。

后续路径：Stage3-B4 联调（按 STAGE3_E2E_CONTRACT_TEST_SPEC §8 / B4-1..B4-5） -> Stage3-VERIFY ->
Stage3-FREEZE -> DH-FREEZE。

---

## 2026-06-06 DH-CODEX-WORKFLOW（Codex workflow routing 规则固化）

按用户工单：把 NQ 已完成的 Codex workflow routing、`nq-dh-workflow-router`、插件路由规则、输出格式和 NQ/DH 安全边界同步到 Decision Hub 仓库。

本轮任务类型为 `DOCUMENTATION`，只修改文档和规则文件，不修改业务代码，不启动 NQ integration。

### 已完成

```text
新增 .agents/skills/nq-dh-workflow-router/SKILL.md
  - 定义 DOCUMENTATION / CODE_ANALYSIS / CODE_CHANGE / SECURITY_AUDIT /
    AGENT_API / NQ_INTEGRATION_PLAN / PRODUCT_DESIGN / PRESENTATION 分类
  - 固化插件路由矩阵
  - 固化 DH/NQ 禁止项
  - 固化标准输出格式，以 Findings 为必填字段，不要求 Summary

新增 docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
  - 固化插件路由规则
  - 明确插件不授权真实 provider / RealClient / DB / 交易能力

新增 docs/current/DH_WORKFLOW_ROUTER_SKILL.md
  - 说明 router skill 触发规则、分类优先级、Integration-0 边界

新增 docs/current/DH_CODEX_TASK_TEMPLATES.md
  - 为 8 类任务提供统一输出模板
  - 所有模板使用 Findings
  - Summary 明确不是必填字段

新增 docs/current/CODEX_PROJECT_INSTRUCTIONS.md
  - 固化 nq-dh-workflow-router 前置分类规则
  - 固化 DH 项目边界和当前状态锁定

新增 docs/current/CODEX_WORKFLOW_INDEX.md
  - 建立 Codex workflow 当前入口索引

更新 AGENTS.md
  - 开工读取清单加入 CODEX_PROJECT_INSTRUCTIONS.md
  - 新增 Codex workflow routing 强制章节
  - active skills 从 8 个调整为 9 个，加入 nq-dh-workflow-router

更新 README.md / docs/current/README.md
  - 增加 Codex workflow 入口
  - 固化标准输出字段

更新 docs/current/STATUS.md / TESTING.md / WORKLOG.md
  - 记录 DH-CODEX-WORKFLOW completed
  - 保持 DH-AUDIT-FIX completed
  - 明确 NQ integration not started
  - 明确 Integration-0 not started / plan only
```

### 严格边界（本阶段未违反）

```text
不修改 backend / frontend / scripts / deploy / migration / API
不新增 NQ client
不新增 RealClient
不新增 real provider
不新增真实交易路径
不连接 NQ
不读取 NQ 数据库
不写 NQ 数据库
不启动 Paper Run
不接 LIVE trading
不读取密钥、凭证、生产配置
不扫描 node_modules / target / build / dist / .git / logs / test-results
不把 DH integration 写成已开始或已完成
```

### 验收

```text
git status --short
git diff --check
定向 rg 检查：
  - AGENTS.md 声明 nq-dh-workflow-router 为 active skill
  - .agents/skills/nq-dh-workflow-router/SKILL.md 存在
  - docs/current/CODEX_PROJECT_INSTRUCTIONS.md 包含前置分类规则
  - 标准输出格式统一使用 Findings
  - Summary 不是必填字段
  - 未把 DH integration 写成 started / completed
  - 未新增 NQ client / RealClient / real provider / LIVE trading
  - 未修改业务代码
```

---

## 2026-06-06 DH-CODEX-WORKFLOW-CLEANUP（DOCUMENTATION 冲突修复）

按用户工单：修复 NQ/DH 横向对照发现的剩余冲突，只允许修改当前 workflow 和状态文档，不修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径。

### 已完成

```text
ROADMAP.md
  - 当前路线 next 从 Stage2-PoC 收口为 Integration-0-PLAN
  - Stage2-PoC 改为 historical / superseded / deferred
  - NqFeedbackClient / NqBacktestClient / 真实 HTTP / event / backtest request /
    RealNqBacktestClient / RealClient / real provider 明确不是当前 next
  - 后续如需恢复，必须先通过 Integration-0-PLAN 安全审查、契约冻结和人工确认

WORK_ORDER.md
  - Next stage 改为 Integration-0-PLAN
  - 下一步唯一允许输出 Integration-0-PLAN 文档
  - 真实 NQ client / NQ /api/ai/research/backtest-requests / RealClient /
    real provider / Paper Run / NQ 交易状态 / LIVE trading 全部标记为 forbidden / deferred / historical

DH workflow 文档
  - AGENTS.md
  - .agents/skills/nq-dh-workflow-router/SKILL.md
  - docs/current/CODEX_PROJECT_INSTRUCTIONS.md
  - docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
  - docs/current/DH_WORKFLOW_ROUTER_SKILL.md

同步 NQ 通用规则：
  - 完整排除目录：node_modules / target / build / dist / .git / logs /
    test-results / secrets / credentials
  - 细凭证禁令：token / cookie / API key / API secret / exchange secret /
    production .env / private key / mnemonic / keystore password / 2FA backup code
  - 开工前范围字段：repository / module / target files / excluded files / expected output
  - archived / historical / superseded 文档不作为当前事实源；
    历史 Stage 文档只能作为背景，不得自动转化为当前 next task

STATUS.md / TESTING.md / WORKLOG.md
  - 记录 ROADMAP / WORK_ORDER 已收口，当前 next 统一为 Integration-0-PLAN
  - 记录 workflow 已补齐 NQ 通用规则
  - 记录本轮只修改 Markdown / Skill 文档，未运行 mvn test
```

### 严格边界（本阶段未违反）

```text
不修改业务代码
不修改 API / migration / provider / NQ client / RealClient / 交易路径
不新增 NQ client
不新增 RealClient
不新增 real provider
不接 NQ
不读取或写入 NQ DB
不启动 Paper Run
不访问交易所密钥
不触碰 LIVE trading
不扫描 node_modules / target / build / dist / .git / logs / test-results / secrets / credentials
不读取 token / cookie / exchange secret / production .env / API key / private key / mnemonic / 2FA backup code
不把 Integration-0 / Stage2-PoC / NQ integration 写成 started / completed
不把 archived / historical / superseded 文档当作当前事实源
```

### 下一步

```text
再次执行最终只读一致性验证，确认 Conflicts 为 None。
```

---

## 2026-06-06 DH-CODEX-WORKFLOW-FINAL-CLEANUP（DOCUMENTATION 入口口径修复）

按用户工单：修复最终只读验证发现的剩余入口文档冲突，只允许修改 AGENTS.md、README.md、
docs/current/README.md、docs/current/DH_NQ_INTEGRATION.md、STATUS.md、TESTING.md、WORKLOG.md。

### 已完成

```text
AGENTS.md
  - Current stage 保持 Stage3-B3 DH Backtest Request Adapter IMPL completed
  - Next stage 改为 Integration-0-PLAN
  - 明确 Integration-0-PLAN 只做只读边界、契约冻结、权限模型、审计模型、风险清单和验收标准
  - Stage3-B2 / NQ Feedback Outbox / 真实 HTTP / event / NQ client / RealClient /
    real provider 标记为 historical / superseded / deferred / gated

README.md
  - Next stage 改为 Integration-0-PLAN
  - 当前下一步从 Stage3-B2 收口为 Integration-0-PLAN
  - 明确 NQ integration not started；Integration-0 not started / plan only；
    RealClient / real provider / LIVE trading / NQ mutation forbidden

docs/current/README.md
  - 顶部 Next stage 改为 Integration-0-PLAN
  - “下一步只允许进入”改为 Integration-0-PLAN
  - 明确 Integration-0-PLAN 不是 implementation；不接 NQ；不新增 NQ client /
    RealClient / real provider；不触碰 LIVE trading；不修改 NQ 状态

docs/current/DH_NQ_INTEGRATION.md
  - 增加当前状态锁定声明
  - REST API 控制面 / POST /api/ai/backtest-requests / 真实 HTTP / event /
    NQ client / RealClient / real provider 方向标记为 historical / superseded /
    deferred / gated
  - 明确这些方向不是当前 next，不是当前 implementation，不允许作为当前开发任务
  - 明确 Integration-0-PLAN 只能做只读边界、契约草案、scope token /
    permission model、audit trail、replay protection、tenant binding、
    request signing、timestamp / nonce、payload size limit、source allowlist、
    risk checklist、acceptance checklist
```

### 严格边界（本阶段未违反）

```text
不修改业务代码
不修改 API / migration / provider / NQ client / RealClient / 交易路径
不新增 NQ client
不新增 RealClient
不新增 real provider
不接 NQ
不读取或写入 NQ DB
不启动 Paper Run
不访问交易所密钥
不触碰 LIVE trading
不扫描 node_modules / target / build / dist / .git / logs / test-results / secrets / credentials
不读取 token / cookie / API key / API secret / exchange secret / production .env /
private key / mnemonic / keystore password / 2FA backup code
不把 Integration-0 / Stage2-PoC / Stage3-B2 / NQ integration 写成 started / completed
不把 archived / historical / superseded 文档当作当前事实源
```

### 下一步

```text
再次执行最终只读一致性验证，确认 Conflicts 为 None。
```

## 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（bounded memory cap）

### 范围
只实现 bounded memory cap；不实现 rate limit；不做 header alignment；不启动 Integration-1；不做真实联调。

### 改动文件（main）
- dh-security/.../security/nq/InMemoryNonceReplayGuard.java：无界 -> 有界（maxEntries + TTL + fail-closed，
  满容量不驱逐未过期 key；有效期取 laterOf 只延长不缩短防重放窗口；非法配置启动失败）
- dh-usecase/.../usecase/agent/inmemory/InMemoryNqFeedbackEventRepository.java：无界 -> 有界
  （maxEvents + perTenantMaxEvents + retention TTL；驱逐最老项；envelope 幂等+有界；保留原始 payload，
  不新增凭证存储；非法配置启动失败）
- dh-app/.../config/SecurityWiringConfig.java：in-memory guard 注入 max-entries/ttl-seconds
- dh-app/.../config/AgentRuntimeWiringConfig.java：feedback repository 注入 max-events/per-tenant-max-events/retention-seconds
- dh-app/src/main/resources/application.yml：新增 replay.in-memory.* 与 feedback-store.* 保守默认（各 profile 经 base 继承）

### 改动文件（test）
- dh-security/.../BoundedInMemoryNonceReplayGuardTest.java（7）
- dh-security/.../NqFeedbackPayloadSizeGateTest.java（2，确认 64KiB 上限未被破坏）
- dh-usecase/.../inmemory/BoundedInMemoryNqFeedbackEventRepositoryTest.java（7）

### 验证
- mvn test：BUILD SUCCESS；7 模块全绿；INT0-T01..T15（16 用例）未被破坏；ArchUnit 12/12
- 本轮 runner 有 Docker，JdbcNonceReplayGuardPersistenceTest 3 用例真实跑通
- git diff --check：无 whitespace error
- mvn -Pquality validate：未强制修复；既有 baseline 问题已分流到独立任务 DH-QUALITY-BASELINE-CLEANUP

### 设计取舍 / 冲突解决
- payload 保留冲突：Batch 2 指引「不得保存 raw payload」与域不变量 + 项目 CLAUDE.md §9「NQ feedback 必须
  保存原始 payload」冲突。按优先级以域不变量/项目规范为准：保留 payloadJson，本批仅确保不新增 secret/token/credential 存储。
- TTL 与认证层 expiresAt 双来源：guard 有效期取 laterOf(认证层 expiresAt, now+ttl)，保证 ttl-seconds 只作为
  最小保留下限，绝不缩短由 2×max-clock-skew-seconds 决定的防重放窗口。

### 边界确认
未修改 NQ；未实现 rate limit；未做 header alignment；未新增 API/migration/RealClient/真实 Provider；
未做真实 HTTP/真实 NQ/真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥。

### 准入
Integration-1 仍 NOT STARTED；P1-4 仍未全部关闭（rate limit 残留）；
下一步 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2-REVIEW，再进入 Batch 3 rate limit。

### 2026-06-13 后续修复：Batch 2 flaky 测试
- 现象：`BoundedInMemoryNqFeedbackEventRepositoryTest.existing_query_semantics_preserved_sorted_by_received_at`
  在 2026-06-13 运行时 `mvn test` 失败（expected:3 but was:1）。
- 根因：该用例使用默认构造（真实 `Clock.systemUTC()`），但事件 receivedAt 固定为 `T0=2026-06-12`，
  默认 retention=24h。墙钟跨过 T0+24h 后，每次 append 的 TTL 清理把先前事件按 retention 过期驱逐，只剩最后一条。
  属时间依赖（flaky）用例，非 bounded memory cap 生产语义缺陷。
- 修复：仅改该测试，注入固定 `MutableClock(T0)`（与同类其余用例一致），事件 age 远小于 retention，三条均保留，
  仅验证排序语义。生产代码与 retention/上限语义未改。
- 复验：`mvn test`（root，全 19 模块）BUILD SUCCESS；`mvn -Pquality validate`（root）BUILD SUCCESS；
  INT0-T01..T15 仍全绿；`git diff --check` 无 whitespace error。
- 改动文件：`dh-usecase/.../inmemory/BoundedInMemoryNqFeedbackEventRepositoryTest.java`（+5/-1，test-only）。

## 2026-06-13 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3（inbound rate limit / 429）

### 范围
只实现 NQ feedback 入站 rate limit / 429 限流路径；不做 header alignment；不启动 Integration-1；不做真实联调 / 真实 HTTP / RealClient。

### 改动文件（main）
- `dh-security/.../security/nq/RateLimiter.java`（新增）：限流端口，key=source+tenant+route；只接收 source/tenant/route/now，不读 body/secret/签名材料。
- `dh-security/.../security/nq/RateLimitResult.java`（新增）：独立结果模型（allowed/reason/retryAfterSeconds/auditCode），超限 reason/auditCode=RATE_LIMITED；不污染 `NqFeedbackAuthResult`。静态工厂命名 `pass()` 以避开 record `allowed()` 访问器同名冲突。
- `dh-security/.../security/nq/InMemoryRateLimiter.java`（新增）：固定窗口计数器；windowSeconds + maxRequests + maxKeys；窗口 TTL 清理优先；maxKeys 满且无过期项 fail-closed 拒绝（不无界、不驱逐活跃窗口）；非法配置构造抛 `IllegalArgumentException` -> 启动失败。
- `dh-api/.../api/feedback/NqFeedbackController.java`：限流前置于 HMAC authenticator；超限 429 + errorCode `RATE_LIMITED`；`log.warn` 审计（auditCode/tenant/source/route/traceId，不含阈值/窗口/计数/密钥）；保留既有 401/403/409/413/202 语义。
- `dh-app/.../config/SecurityWiringConfig.java`：新增 `nqFeedbackRateLimiter` bean，`@Value` 注入保守默认（window=1s / max-requests=20 / max-keys=10000）；非法配置启动失败。
- `dh-app/.../config/AgentRuntimeWiringConfig.java`：feedback-store `per-tenant-max-events` 默认 `@Value` 10000 -> 1000（P2-1 加固）。
- `dh-app/src/main/resources/application.yml`：新增 `rate-limit.{window-seconds,max-requests,max-keys}`；`feedback-store.per-tenant-max-events` 10000 -> 1000。

### 改动文件（test）
- `dh-security/.../security/nq/InMemoryRateLimiterTest.java`（新增，6）。
- `dh-api/.../api/feedback/NqFeedbackRateLimitWebMvcTest.java`（新增，3）。
- `dh-api/.../api/feedback/NqFeedbackControllerWebMvcTest.java`：构造器加注入宽松 limiter（+9/-1），15/15 仍全绿。

### 验证
- `mvn test`：BUILD SUCCESS；InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackControllerWebMvcTest 15/15、NqFeedbackPayloadSizeGateTest 2/2、Batch 2（7+7）未破坏；INT0-T01..T15（16）未破坏；ArchUnit 全绿。
- 无 Docker runner：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1 按 disabledWithoutDocker 跳过（非失败）。
- `mvn -Pquality validate`：BUILD SUCCESS（未引入 quality 违规）。
- `git diff --check`：无 whitespace error（仅 LF→CRLF 提示）。

### 设计取舍
- 限流作用点选在 controller 内、`requireTenantId` 之后、HMAC 之前：tenant 来自已认证上下文（bearer filter 先行 401），source 取 `X-DH-NQ-Source` header；source 缺失归一化为占位符，与真实 tenant 组合仍是按租户隔离的有界 key，不会合并成单一无界公共 key。
- `retryAfterSeconds` 保留在结果模型供审计 / 日志，但 controller 不在响应头暴露，避免泄露精确窗口。
- in-memory limiter 仅 dev/test 或单实例辅助路径；真实多实例集中式（Redis）limiter 另起任务，本轮先修复「完全无限流」。

### 边界确认
未修改 NQ；未做 header alignment；未新增 API 路径；未新增 migration / RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥。

### 准入
Integration-1 仍 NOT STARTED；P1-4 三项残留实现均已落地（replay nonce / memory cap / rate limit），但 **P1-4 未标记全部关闭**——须先 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3-REVIEW，再 DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 单独验收后方可关闭；不得直接进入 Integration-1。

## 2026-06-13 DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE（P1-4 整体回归收口）

### 范围
只做 P1-4 三项修复整体回归收口验收 + 文档关闭口径。不新增功能；不改生产代码 / 测试代码；不启动 Integration-1；不做真实联调 / header alignment。

### 改动文件（仅文档）
- `docs/current/STATUS.md`：§1.1 残留项指向关闭；新增 §1.3「DH P1-4 residual regression close（2026-06-13，CLOSED）」。
- `docs/current/ROADMAP.md`：P1-4 残留 -> CLOSED；下一步只允许 CI 持久化 nonce IT 或 header alignment plan。
- `docs/current/README.md`：security baseline -> P1-4 residual CLOSED（指向 STATUS §1.3）。
- `docs/current/DH_P1_4_RESIDUAL_FIX_PLAN.md`：状态注记 -> CLOSED + 非阻塞后续项。
- `docs/current/TESTING.md`：新增 §27 回归收口验收记录。
- `docs/current/WORKLOG.md`：本条目。

### 回归制品核验（只读，未改代码）
- Batch1 replay nonce：`JdbcNonceReplayGuard`（`INSERT ... ON CONFLICT (replay_key) DO NOTHING`、`DataAccessException` -> false fail-closed -> 409、异常只记类型不记 replay_key/nonce/payload/secret、惰性清理过期行）；`V4__nq_feedback_replay_nonce.sql`（`if not exists`、不动 V1–V3、无凭证列）；`NonceReplayGuardType.select`（缺省非 dev/test -> JDBC，非 dev/test 显式 in-memory -> 启动失败）。
- Batch2 memory cap：`InMemoryNonceReplayGuard`（maxEntries + TTL + 满容量 fail-closed 不驱逐未过期 key）；`InMemoryNqFeedbackEventRepository`（global + per-tenant + retention 上限、TTL 清理优先、非法配置启动失败）；payload 64KiB gate 未受影响。
- Batch3 rate limit：`RateLimiter` / `RateLimitResult` / `InMemoryRateLimiter` 存在；限流前置于 HMAC；key=source+tenant+route；超限 429 RATE_LIMITED 且不泄露阈值/窗口/计数/secret/签名材料；限流短路下游 ingestion；HMAC/timestamp/nonce/replay/payload 语义不变。

### 验证
- `mvn test`：BUILD SUCCESS。JdbcNonceReplayGuardTest 5/5、NonceReplayGuardTypeTest 5/5、HmacNqFeedbackAuthenticatorTest 6/6、BoundedInMemoryNonceReplayGuardTest 7/7、BoundedInMemoryNqFeedbackEventRepositoryTest 7/7、NqFeedbackPayloadSizeGateTest 2/2、InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackControllerWebMvcTest 15/15、DhNqIntegration0*（INT0）16/16、ArchUnit 全绿。
- 本机无 Docker：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1 按 `disabledWithoutDocker` skip（非失败）。**持久化 nonce restart 语义仍需 Docker CI 独立验证，未在本机实跑。**
- `mvn -Pquality validate`：BUILD SUCCESS（仅改文档，未引入违规）。
- `git diff --check`：无 whitespace error；本轮改动仅 `docs/current/*`。

### 三项之间无冲突
- Batch1 JDBC guard 与 Batch2 in-memory bounded guard 是同一 `NonceReplayGuard` 端口的两个实现，由 `NonceReplayGuardType.select` 按 profile 二选一（非 dev/test 默认 JDBC，dev/test 允许 bounded in-memory），互不冲突。
- Batch3 限流前置于 HMAC，超限请求短路、不消耗 nonce / 不写 replay store，对 Batch1/2 是正向交互。
- Batch3 P2-1 加固只下调 feedback-store per-tenant 默认值（config-only），不改 Batch2 主逻辑。

### 关闭口径
DH P1-4 residual: CLOSED；replay nonce persistence: closed；memory cap: closed；rate limit: closed。
Integration-1: NOT STARTED；Runtime integration: NOT STARTED；DH: NOT INTEGRATED；AI: NOT STARTED；LIVE: DISABLED；header alignment: NOT DONE。

### 非阻塞后续项（均未在本轮处理）
1. DH-CI-PERSISTENT-NONCE-IT-ENABLE：Docker CI 跑通 `JdbcNonceReplayGuardPersistenceTest`（restart 语义）。
2. DH-NQ-HEADER-ALIGNMENT-PLAN：header `X-DH-NQ-*` / `X-NQ-DH-*` 对齐。
3. datasource 默认弱口令治理（单独任务）。
4. 多实例真实通道集中式（Redis）rate limiter，Integration-1 前设计审查。
5. rate limit 指标 / counter 可观测性增强。
6. P1-4 后 Integration-1 planning-only audit。

### 边界确认
未修改 NQ；未做 header alignment；未新增 API；未新增 migration；未新增 RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥。

### 准入
P1-4 CLOSED 仅表示 Integration-1 的前置安全缺口关闭，**不等于允许真实联调**。Integration-1 仍 NOT STARTED；下一步只允许 DH-CI-PERSISTENT-NONCE-IT-ENABLE 或 DH-NQ-HEADER-ALIGNMENT-PLAN，不得直接进入 Integration-1 runtime。

## 2026-06-13 DH-CI-PERSISTENT-NONCE-IT-ENABLE（Docker CI 实跑持久化 nonce IT）

### 范围
让带 Docker 的 CI runner 真实运行 Testcontainers 集成测试（JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest），验证 persistent replay nonce 的 restart 语义。不改业务生产代码、不改测试逻辑、不接真实 NQ / 真实 HTTP、不启动 Integration-1。

### 改动文件
- 新增 `.github/workflows/ci.yml`（GitHub Actions）：`ubuntu-latest`（预装并运行 Docker）+ JDK 21（temurin）+ maven 缓存；步骤 `docker info` → `./mvnw -B -ntp test` → 断言两个 IT 在 CI 下 `Skipped: 0`（否则 CI 失败）→ 上传 surefire 报告。
- 文档：`docs/current/TESTING.md`（§28）、`docs/current/WORKLOG.md`（本条）、`docs/current/README.md`。
- **未修改任何测试代码**：`JdbcNonceReplayGuardPersistenceTest` / `PostgresContainerSmokeTest` 保留 `@Testcontainers(disabledWithoutDocker = true)`——本地/无 Docker 优雅 skip，CI 有 Docker 实跑。

### 设计取舍
- 仓库此前无 CI 配置（仅 `.github/PULL_REQUEST_TEMPLATE.md`），故新增 GitHub Actions workflow。
- 不删除 `disabledWithoutDocker`：避免本地/无 Docker 环境硬失败；改为在 CI（确有 Docker）用 assert 步骤强制 IT 非 skip，兼顾"本地可跑"与"CI 必实跑"。
- 仅运行 `test`（含 Testcontainers IT），不在本轮新 CI 引入 quality 门以免首版 CI 被环境性 checkstyle/spotless 波动阻塞；quality 门作为后续 CI 增强项。

### 验证
- 本机（无 Docker）：`mvn -pl dh-infra -am -Dtest=JdbcNonceReplayGuardPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test` → BUILD SUCCESS；该 IT `Tests run: 3, Skipped: 3`（disabledWithoutDocker 优雅 skip，符合预期）。
- `git diff --check`：无 whitespace error。
- YAML：PyYAML 本机不可用，已结构核验（无 Tab 缩进；顶层键 name/on/permissions/concurrency/jobs；6 个 step 结构正确）。
- CI Docker 实跑结果：**待首次 push/PR 触发 GitHub Actions 后确认**；本轮无法在本机触发 CI，故不记为已 executed/passed。

### 边界确认
未修改 NQ；未新增业务 Java 生产代码；未新增 API；未新增 migration；未做 header alignment；未新增 RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未启动 Integration-1；未把 DH 写成 integrated；未读取或输出真实密钥。

### 准入
Integration-1 仍 NOT STARTED；本轮仅启用 CI 实跑能力，不改变 P1-4 CLOSED 口径。下一步：DH-NQ-HEADER-ALIGNMENT-PLAN 或 DH-CONFIG-CREDENTIAL-DEFAULTS-GOVERNANCE，不得直接进入 Integration-1 runtime。

### 2026-06-14 CI 实跑收尾（commit + push + 修复 + 全绿确认）
- 提交并推送 CI 配置后，GitHub Actions（origin/dev）连续暴露并修复两个问题：
  1. **mvnw 在 Linux runner 无法执行**：`./mvnw` -> `Permission denied`（exit 126）。先经 `chmod +x ./mvnw` 仍失败为
     `no main manifest attribute, in .mvn/wrapper/maven-wrapper.jar`——仓库内 wrapper jar 因缺 `.gitattributes` +
     提交端 autocrlf 被 EOL 规范化破坏。**修复（fd522ce）**：CI 改用 ubuntu-latest 预装 `mvn -B -ntp test`，绕开损坏 wrapper。
  2. **PostgresContainerSmokeTest 全上下文加载失败**：`Unsupported Database: PostgreSQL 17.10`——dh-app 仅依赖
     `flyway-core`，Flyway 10+ 已将 PostgreSQL 支持拆为独立模块。**修复（841354d，用户明确授权的生产级修复）**：
     `dh-app/pom.xml` 增加 `flyway-database-postgresql`（版本由 spring-boot-starter-parent 3.5.10 管理）。
- **CI 全绿确认**：GitHub Actions run 27485958120（commit 841354d）结论 success，`[INFO] BUILD SUCCESS`：
  - JdbcNonceReplayGuardPersistenceTest：Tests run 3 / Skipped 0 / 8.317s（restart 语义经真实 PG17 验证）。
  - PostgresContainerSmokeTest：Tests run 1 / Skipped 0 / 6.645s（全上下文加载成功）。
  - assert 步骤 `OK (executed, 0 skipped)` × 2；两个 IT 在 CI Docker 下确认未被 skip。
- 代码改动（本轮，超出原 CI 任务范围但经用户授权）：`dh-app/pom.xml`（+flyway-database-postgresql，8 行）。
  其余为 CI/docs。**说明**：flyway-database-postgresql 是真实 PG 迁移本就必需的模块，属修复既有生产缺陷，非新增功能。
- 边界：未修改 NQ；未新增 API / migration（仅补依赖，未动 V1–V4）；未做 header alignment；未新增 RealClient /
  真实 Provider；未真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥。
- 准入：CI 整体全绿已确认；P1-4 CLOSED 口径不变（CI 实跑是补强证据）；Integration-1 仍 NOT STARTED。
  下一步（用户 step 4，CI 已通过故解阻）：DH-NQ-HEADER-ALIGNMENT-PLAN。
- 后续项：DH-MAVEN-WRAPPER-REPAIR（修复仓库内 .mvn/wrapper/maven-wrapper.jar + 加 .gitattributes，恢复本地/Linux mvnw 可用）。

## 2026-06-14 DH-NQ-HEADER-ALIGNMENT-PLAN（planning-only）

### 范围
只读核查 DH + NQ 两仓 header 用法，输出对齐方案 `docs/current/DH_NQ_HEADER_ALIGNMENT_PLAN.md`。不改运行代码 / 测试；不启动 Integration-1；不做真实联调 / header 改名实施。

### 只读核查结论（current header map）
- NQ（nexus-quant）：canonical `X-NQ-DH-*` 仅在 INT0 fixture（`Int0Contract.java` 7 个）+ docs；**NQ 无生产 header 处理代码**（无 DH 入站端点）；docs 里 `X-DH-NQ-*` 均为对齐说明引用。
- DH：生产 `NqFeedbackController` 用 **legacy `X-DH-NQ-*`**，且仅消费 4 个（Source/Timestamp/Nonce/Signature）；Tenant 来自认证上下文、Request-Id/Trace-Id 来自 body。WebMvc 测试用 legacy；INT0 fixture（`Int0Contract.java`）用 canonical 7（与冻结契约一致，但不经真实 controller，二者解耦故都绿）。
- 关键：`HmacNqFeedbackAuthenticator.signatureMaterial` 为 **value-based（不含 header name）** -> header 改名不会导致签名漂移。

### Findings
- P1-1：DH 生产 legacy 与 Integration-0 冻结 canonical 不一致（已登记 Integration-1 前置）。
- P1-2：契约 7 header vs 实现 4 header 的结构差异（Tenant/Request/Trace 来自 auth-context/body，非 header），对齐需显式决策。
- P2-1：DH 内部命名分裂（生产 legacy / INT0 fixture canonical）。P2-2：source 双绑定校验需在解析层保留。
- P3：header 常量散落、docs/fixtures 术语需统一。

### 推荐策略
canonical 优先 + 短兼容期（双 header 接收、canonical 优先）+ 冲突 fail-closed（同名值不同 -> 拒绝）+ value-based 签名基于 normalized 模型（防混签）+ Tenant/Request/Trace header 不覆盖权威来源（防提权/串租户）+ 审计码 LEGACY_HEADER_USED / HEADER_CONFLICT / MISSING_CANONICAL_HEADER。分 6 批实施（常量/parser -> canonical -> legacy 兼容 -> 冲突 fail-closed -> docs/fixtures -> 移除 legacy 独立 review 后）。

### 边界确认
未修改 Java；未修改测试；未新增 API；未新增 migration；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未启动 Integration-1；未把 DH 写成 integrated；未读取或输出真实密钥。**未跨仓写 NQ**（保持 DH/NQ 仓库边界；NQ companion 文档建议由 NQ-scoped 任务补）。

### 准入
header alignment 仍 NOT STARTED（仅 PLAN）；Integration-1 仍 NOT STARTED；PLAN 通过也不等于允许 runtime integration。下一步 DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW，通过后 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1。

## 2026-06-14 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1（内部结构 skeleton，不改对外行为）

### 范围
实现 header alignment 第一批内部结构：集中 header 常量 + parser + 归一化模型 + validator skeleton。不切换 canonical、不移除 legacy、不做双接收、不改 HMAC 语义、不启动 Integration-1。已定策略：后续 canonical-only（无兼容期）；Tenant/Request/Trace 保权威来源、header 仅一致性校验。

### 新增文件（dh-security/main）
- `security/nq/NqDhHeaderNames.java`：集中 canonical `X-NQ-DH-*`(7) + legacy `X-DH-NQ-*`(7) 常量，消除散落 magic string。
- `security/nq/NormalizedNqDhHeaders.java`：归一化 header 模型（record）；`toString` 对 signature 脱敏（[REDACTED]）。
- `security/nq/NqDhHeaderParser.java`：parser skeleton，Batch 1 仅 `parseLegacy`（按 legacy 读取，封装现有行为）；canonical 读取留待 Batch 2。
- `security/nq/NqDhHeaderValidator.java`：validator skeleton，Batch 1 pass-through（恒 ok，不引入新拒绝）；Batch 2/3 填充 canonical-only + binding 一致性校验。
- `security/nq/NqDhHeaderValidationResult.java`：校验结果模型 + 审计码常量（HEADER_BINDING_MISMATCH / MISSING_CANONICAL_HEADER）。

### 新增文件（dh-security/test）
- `NqDhHeaderNamesTest`(2)、`NqDhHeaderParserTest`(3)、`NqDhHeaderValidatorTest`(2)。

### 修改文件（dh-api/main）
- `api/feedback/NqFeedbackController.java`：去除 4 个 magic string header 常量，改用 `NqDhHeaderParser.parseLegacy(httpRequest::getHeader)` 产出归一化模型；限流 source 与 authenticateNqSource 的 4 个 header 值改取自模型（值与此前逐个 getHeader 等价）。**构造器未变、对外行为未变**：仍读 legacy `X-DH-NQ-*`，HMAC signatureMaterial 语义不变，401/403/409/413/429/202 语义不变，rate limit key=source+tenant+route 不变。validator 本批未接入 controller（Batch 2 再接），保证零行为变更。

### 验证
- `mvn test`：BUILD SUCCESS（exit 0）。新单测 NqDhHeaderNamesTest 2/2、NqDhHeaderParserTest 3/3、NqDhHeaderValidatorTest 2/2；既有 NqFeedbackControllerWebMvcTest 15/15、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2（行为不变）；INT0 DhNqIntegration0* 6+2+8=16/16 未破坏；ArchUnit 全绿。无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：BUILD SUCCESS（未引入 quality 违规）。
- `git diff --check`：无 whitespace error。

### 安全自查
- 不记录 raw signature / signature material / secret / token / full body：parser/validator 不打日志；归一化模型 toString 对 signature 脱敏（单测固化）。

### 边界确认
未修改 NQ；未切换生产 controller 到 canonical-only；未移除 legacy 行为；未实现双接收；未新增 API；未新增 migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未把 header alignment 写成 completed；未读取或输出真实密钥。

### 准入
header alignment 整体仍 NOT COMPLETED（canonical-only 未切换）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1-REVIEW，通过后 Batch 2（canonical 读取）。

## 2026-06-15 DH-NQ-HEADER-ALIGNMENT-DOC-RECONCILE（文档口径收口，纯文档）

### 范围
仅修正 header alignment 文档口径漂移，将计划统一到已评审通过的 **canonical-only** 策略。不改 Java、不改测试、不进入 Batch 2 implementation、不启动 Integration-1。允许改：`DH_NQ_HEADER_ALIGNMENT_PLAN.md` / `README.md` / `TESTING.md` / `WORKLOG.md`。

### 背景
`DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1-REVIEW`（2026-06-14）通过；评审发现文档漂移：PLAN §4–§7 与 README 仍保留 PLAN-REVIEW 改判前的"短兼容期 / 双 header 接收 / 冲突 fail-closed / 移除 legacy 独立批次"措辞，而已 ACCEPTED 的策略是 canonical-only（无兼容期）。本轮收口该漂移。

### 修改文件（仅文档）
- `DH_NQ_HEADER_ALIGNMENT_PLAN.md`：顶部实施进展补 DOC-RECONCILE 说明 + Batch 1 标 DONE/ACCEPTED + legacy 仍当前生产；§4.1 总体方针 兼容期→canonical-only；§4.2 改为"不需要兼容期、不允许双接收"；§4.3 冲突表→canonical-only 接收表（缺/仅 legacy -> `MISSING_CANONICAL_HEADER`）；§4.4 去双解析与"canonical 优先"，normalized 模型对齐实现类 `NormalizedNqDhHeaders`；§4.5 binding 校验明确归 Batch 3；§4.6 审计码去 `LEGACY_HEADER_USED`/`HEADER_CONFLICT`，保留 `MISSING_CANONICAL_HEADER`/`HEADER_BINDING_MISMATCH`/`SIGNATURE_MISMATCH`；§4.7 去 legacy 通道；§5 批次重排（Batch 1 DONE/ACCEPTED；Batch 2 canonical-only 读取；Batch 3 binding 一致性校验；Batch 4 docs/fixtures 收口；legacy 常量退场列为可选后续小批）；§6 测试清单去 legacy_compat/conflict、加 canonical-only/binding 用例；§7 14 问速答 7–11/13 改 canonical-only；§8 next action 改为 Batch 2。
- `README.md`：header alignment 段"推荐 canonical 优先 + 短兼容期 + 冲突 fail-closed"→canonical-only；明确 Batch 2 = canonical-only 读取、Batch 3 = binding 一致性校验、legacy 仍是当前生产行为直到 Batch 2、header alignment overall NOT COMPLETED、Integration-1 NOT STARTED。
- `TESTING.md`：新增第 31 条 DOC-RECONCILE 记录（纯文档，未跑测试 + 原因）。
- `WORKLOG.md`：本条目。

### 口径确认（canonical-only）
1. 采用 canonical-only；2. 无 legacy 兼容期；3. 不实现 legacy/canonical 双接收；4. Batch 2 = canonical-only 读取；5. Batch 3 = Tenant/Request/Trace binding mismatch 一致性校验（`HEADER_BINDING_MISMATCH`）；6. legacy 常量仅作历史引用、不再扩散；7. timestamp 格式分歧另列 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT`，不在本轮。

### 后续项（登记，不在本轮处理）
- **P3-2 未跟踪杂散文件**：工作区存在 4 个未跟踪文件（文件名为任务/计划中文片段，各约 7–15KB，会话开始即存在，疑似历史命令重定向误写）。**不属于任何 commit、不影响构建**（`git diff --check` 干净）。按纪律**未删除**，登记为「待用户单独授权后清理」，本轮不处理。

### 边界确认
未改 Java 生产代码；未改测试；未切 canonical-only 行为（生产仍读 legacy `X-DH-NQ-*`）；未实现双接收；未移除 legacy；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 Maven wrapper / datasource 弱口令 / timestamp 格式分歧；未删除未跟踪杂散文件；未读取或输出真实密钥。

### 准入
header alignment 整体仍 NOT COMPLETED（canonical-only 未切换，生产仍读 legacy `X-DH-NQ-*`）；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（canonical-only 读取）。

## 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（canonical-only 读取）

### 范围
将 DH NQ feedback 入站 header 从 legacy `X-DH-NQ-*` 切到 **canonical-only** `X-NQ-DH-*` 读取。不做 legacy/canonical 双接收、不保留兼容期、不做 Tenant/Request/Trace binding mismatch 强制（留 Batch 3）、不启动 Integration-1、不真实联调。

### 修改文件（main）
- `dh-security/security/nq/NqDhHeaderParser.java`：新增 `parseCanonical(HeaderLookup)`，读 canonical 7 header（Source/Tenant-Id/Request-Id/Trace-Id/Timestamp/Nonce/Signature），family=CANONICAL；`parseLegacy` 保留为历史引用 / 单测（不再被生产 controller 调用）；类 / 方法注释更新为 canonical-only。
- `dh-api/api/feedback/NqFeedbackController.java`：`parseLegacy` -> `parseCanonical`；不再读取 legacy、不做双接收；注释更新。tenant 仍来自认证上下文、requestId/traceId/eventId/sourceSystem/payload 仍来自 body；canonical Tenant/Request/Trace 进入模型但不参与认证、不覆盖权威来源；HMAC signatureMaterial 仍 value-based（不含 header name）。

### 修改文件（test）
- `dh-security/.../NqDhHeaderParserTest.java`：+2（`parse_canonical_reads_x_nq_dh_headers_into_model`、`parse_canonical_does_not_read_legacy_headers`）。
- `dh-api/.../NqFeedbackControllerWebMvcTest.java`：成功路径 header 切 canonical；+5（legacy-only 拒绝→403 SOURCE_NOT_ALLOWED、缺 canonical Source→403、缺 Timestamp→401 TIMESTAMP_EXPIRED、缺 Nonce→401 REPLAY_KEY_MISSING、canonical 成功路径不泄露 raw signature/secret）；签名计算抽出 `canonicalSignature` 复用。
- `dh-api/.../NqFeedbackRateLimitWebMvcTest.java`：header 切 canonical。
- INT0（dh-domain）：无需改（`Int0Contract` 早已用 canonical `X-NQ-DH-*`，且不经真实 controller）。

### 缺失语义（fail-closed，状态码不变）
仅 legacy / 缺必需 canonical header 由现有 `HmacNqFeedbackAuthenticator` 拒绝：canonical Source 缺失/不匹配 -> 403 SOURCE_NOT_ALLOWED；Timestamp -> 401 TIMESTAMP_EXPIRED；Nonce -> 401 REPLAY_KEY_MISSING；Signature -> 401 BAD_SIGNATURE。`NqDhHeaderValidator` 本批仍未接入 controller（显式 MISSING_CANONICAL_HEADER / binding 随 Batch 3 接入）。

### 验证
- `mvn test`：BUILD SUCCESS（exit 0）。NqFeedbackControllerWebMvcTest 20/20、NqFeedbackRateLimitWebMvcTest 3/3、NqDhHeaderParserTest 5/5、NqDhHeaderNamesTest 2/2、NqDhHeaderValidatorTest 2/2、NqFeedbackPayloadSizeGateTest 2/2、INT0 DhNqIntegration0* 6+2+8=16/16；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：BUILD SUCCESS（0 Checkstyle / spotless 通过）。
- `git diff --check`：无 whitespace error。

### 安全自查
不记录 raw signature / signature material / secret / token / full body；归一化模型 toString 对 signature 脱敏；canonical 成功路径单测断言响应不回显 signature / secret。HMAC value-based 不含 header name，改名不漂移。payload 64KiB / nonce replay / rate limit key=source+tenant+route 全保持。

### 边界确认
未改 NQ；未做 legacy/canonical 双接收；未兼容 legacy 入站；未保留 legacy 为生产可接受 header；未移除 legacy 常量；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 wrapper / datasource 弱口令 / timestamp 格式分歧；未删除未跟踪杂散文件；未读取或输出真实密钥。

### 准入
生产入站现为 canonical-only（`X-NQ-DH-*`）；header alignment 整体仍 NOT COMPLETED（Tenant/Request/Trace binding=Batch 3、docs/fixtures 收口=Batch 4 尚待）；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2-REVIEW，通过后 Batch 3。

## 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3（Tenant/Request/Trace binding 一致性校验）

### 范围
正式接入 `NqDhHeaderValidator`，实现 canonical `X-NQ-DH-Tenant-Id/Request-Id/Trace-Id` 与权威来源的 binding 一致性校验。不恢复 legacy 兼容、不做双接收、不启动 Integration-1、不真实联调、不处理 timestamp 格式 / checkstyle DTD / wrapper / datasource 弱口令。

### 修改文件（main）
- `dh-security/security/nq/NqDhHeaderValidator.java`：`validate(NormalizedNqDhHeaders, authTenant, bodyRequestId, bodyTraceId)`；三个 binding header 可选，若提供（非空）则必须等于权威来源，任一不一致 -> invalid / `HEADER_BINDING_MISMATCH`；缺省跳过；`reason` 仅含字段名不含具体值。`AUDIT_MISSING_CANONICAL_HEADER` 保留为预留码（缺必需 canonical 仍由 authenticator 既有码覆盖）。
- `dh-api/api/feedback/NqFeedbackController.java`：新增 `headerValidator` 字段；在 HMAC 认证成功后、入库前调用 `validate(nqHeaders, tenantId, req.getRequestId(), req.getTraceId())`；不一致返回 403 + `NqFeedbackErrorResponse(error=HEADER_BINDING_MISMATCH, errorCode=HEADER_BINDING_MISMATCH, ...)`；审计日志仅记 auditCode + 安全字段。权威来源不变（tenant=认证上下文，requestId/traceId=body）；header 绝不覆盖权威来源。

### 修改文件（test）
- `dh-security/.../NqDhHeaderValidatorTest.java`：重写为 binding 测试（全一致 ok、缺省跳过、tenant/request/trace mismatch 各 1、factory），6 用例；断言 reason 不泄露值。
- `dh-api/.../NqFeedbackControllerWebMvcTest.java`：+5（binding 全一致 202、tenant/request/trace mismatch 各 403 HEADER_BINDING_MISMATCH、mismatch 不泄露 signature/secret/full payload）。

### 安全 / 不变量
- HMAC signatureMaterial 仍 value-based（不含 header name）；canonical Tenant/Request/Trace 不参与签名、不参与认证、不覆盖权威来源。
- 不记录 raw signature / signature material / secret / token / full body；mismatch 响应不回显 header 原值 / signature / secret / payload。
- 保持：rate limit key=source+tenant+route、payload 64KiB（413）、nonce replay（409）、缺 source/timestamp/nonce/signature 由 authenticator fail-closed（403/401）、202/400 语义。

### 验证
- `mvn test`：BUILD SUCCESS。NqDhHeaderValidatorTest 6/6、NqFeedbackControllerWebMvcTest 25/25、NqDhHeaderParserTest 5/5、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2、INT0 DhNqIntegration0* 6+2+8=16/16；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：BUILD SUCCESS（0 Checkstyle / spotless 通过；本轮 checkstyle DTD 未抖动）。
- `git diff --check`：无 whitespace error。

### 边界确认
未改 NQ；未恢复 legacy 兼容；未实现双接收；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 timestamp 格式 / checkstyle DTD / wrapper / datasource 弱口令；未读取或输出真实密钥。

### 后续项（不在本轮）
- Batch 4 docs/fixtures 收口：统一 §4.3 / §4.6 / §6 关于 `MISSING_CANONICAL_HEADER` 的措辞（该码当前为预留，缺必需 canonical 由 authenticator 既有码覆盖）；WebMvc 既有 fixtures 已是 canonical，核对一致。

### 准入
header alignment 整体仍 NOT COMPLETED（仅余 docs/fixtures 收口 Batch 4）；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3-REVIEW，通过后 Batch 4。

## 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4（docs/fixtures 收口）

### 范围
对 header alignment 做 docs/fixtures 收口：统一文档措辞与当前实现事实。**不新增运行代码、不改核心行为、不启动 Integration-1**。

### 修改文件（仅文档）
- `DH_NQ_HEADER_ALIGNMENT_PLAN.md`：§4.3/§4.6/§6 统一 `MISSING_CANONICAL_HEADER` 措辞（预留码，缺 canonical 由 authenticator 既有码 fail-closed、不单独发码）；§5 Batch 2/3 标 ACCEPTED、Batch 4 标 DONE；顶部进展与 §8 标 header alignment 整体 READY FOR CLOSE / PENDING FINAL REVIEW；§7 item 9/13 同步；登记独立后续项。
- `README.md`：同步 `MISSING_CANONICAL_HEADER` 预留码措辞、Batch 3 ACCEPTED、Batch 4 DONE、整体 READY FOR CLOSE、下一步 CLOSE-REVIEW。
- `TESTING.md`：新增第 34 条 Batch 4 收口验收记录。
- `WORKLOG.md`：本条目。
- 无 Java / fixture 文件改动（核对结论见下）。

### 收口核对结论
- **MISSING_CANONICAL_HEADER**：预留审计码；缺 source/timestamp/nonce/signature 仍由 `HmacNqFeedbackAuthenticator` 既有码 fail-closed（SOURCE_NOT_ALLOWED 403 / TIMESTAMP_EXPIRED 401 / REPLAY_KEY_MISSING 401 / BAD_SIGNATURE 401），当前不单独发码。
- **canonical-only 已落地**：生产入站只接受 `X-NQ-DH-*`；legacy `X-DH-NQ-*` 不再接受；无兼容期；无双接收。
- **binding mismatch 已落地**：canonical Tenant-Id/Request-Id/Trace-Id 与权威来源（tenant=认证上下文、requestId/traceId=body）不一致 -> 403 `HEADER_BINDING_MISMATCH`；header 不覆盖权威来源。
- **WebMvc fixtures**：成功路径全用 canonical `X-NQ-DH-*`；唯一 legacy `X-DH-NQ-*` 在 `post_legacyOnlyHeaders_areRejected_canonicalOnly` 负路径（验证 legacy 被拒，刻意保留）。
- **INT0 fixtures**：`Int0Contract` 7 个 canonical `X-NQ-DH-*`（不经真实 controller）。
- README/TESTING/WORKLOG 与当前状态一致。

### 验证
- `mvn test`：BUILD SUCCESS（回归，无运行代码改动）。NqFeedbackControllerWebMvcTest 25/25、NqDhHeaderValidatorTest 6/6、NqDhHeaderParserTest 5/5、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2、INT0 DhNqIntegration0* 6+2+8=16/16；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：本轮 3 次均因 checkstyle SuppressionFilter 联网解析 `checkstyle-suppressions.xml` 外部 DTD 超时（`Connection timed out`）BUILD FAILURE —— 已知环境性问题（`DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE`），**非本批所致**；本批仅改 docs（无 Java/test 改动），checkstyle/spotless 覆盖面与 Batch 3（最近一次 PASS：0 violations / spotless 通过）一致；按规定本轮不修 checkstyle 配置。
- `git diff --check`：无 whitespace error。

### 边界确认
未改 NQ；未改 Java 生产代码；未改 controller 行为；未恢复 legacy 兼容；未实现双接收；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 timestamp 格式 / checkstyle DTD / wrapper / datasource 弱口令 / nonce-burn race 前移；未读取或输出真实密钥。

### 独立后续项（不阻塞 close）
- `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT`：timestamp 格式分歧。
- `DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE`：checkstyle suppressions DTD 离线解析治理（曾偶发联网超时）。
- `DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN`：binding 前移至 HMAC 之前，关闭 nonce-burn race（防御纵深，低危）。

### 准入
header alignment 整体 **READY FOR CLOSE / PENDING FINAL REVIEW（仍未 CLOSED）**；Integration-1 / Runtime integration 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 `DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW`（通过后方可 CLOSED）。

## 2026-06-15 DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE（quality gate 离线 DTD 治理）

### 范围
治理 `mvn -Pquality validate` 因 Checkstyle SuppressionFilter 解析外部 DTD 联网超时导致的质量门禁不稳定。仅改 checkstyle 配置 + 文档；不改 header alignment / `NqFeedbackController` 运行行为，不启动 Integration-1。

### 失败根因
`config/checkstyle/checkstyle-suppressions.xml` 的 DOCTYPE PUBLIC id 为**非标准串** `-//Checkstyle//DTD Suppressions 1.2//EN`，不在 Checkstyle（10.20.0）`SuppressionsLoader` 的 EntityResolver 映射表中，故解析回退到 SYSTEM URL `https://checkstyle.org/dtds/suppressions_1_2.dtd` 联网拉取 -> 弱网/离线下 `Connection timed out: connect` -> checkstyle goal 失败 -> BUILD FAILURE。对照：`config/checkstyle/checkstyle.xml` 用官方 `-//Checkstyle//DTD Checkstyle Configuration 1.3//EN`（在映射表中），故 config 解析本地命中、从不联网（失败只出现在 suppressions，印证根因）。

### 修复方式
把 suppressions 的 PUBLIC id 改为 Checkstyle 官方串 `-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN`（与 checkstyle.org `config_filters` 文档一致），使 EntityResolver 命中并改用 Checkstyle **内置 DTD 资源**解析、不再访问外网；保留 SYSTEM URL 作为标准声明（命中 PUBLIC 后不会被拉取）。**未删除 SuppressionFilter、未删除 suppressions 文件、未降低规则集、未关闭 checkstyle、未跳过 quality profile**；仅改 1 行 PUBLIC id。

### 修改文件
- `config/checkstyle/checkstyle-suppressions.xml`：DOCTYPE PUBLIC id 1 行。
- docs：本 WORKLOG + TESTING（§35）+ README（说明）。

### 验证
- 修复前同会话 `mvn -Pquality validate` 连续 **6 次 BUILD FAILURE**（DTD 联网超时）。
- 修复后（网络仍不可用）`mvn -Pquality validate` **连续 2 次 BUILD SUCCESS / 0 Checkstyle violations / spotless 通过** —— 证明 suppressions DTD 现已离线解析。
- `mvn test`：BUILD SUCCESS；header alignment 类（names 2 / parser 5 / validator 6 / WebMvc 25 / rate limit 3 / payload gate 2）+ INT0 16/16 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `git diff --check`：无 whitespace error。

### 本轮未改变业务行为
仅改 checkstyle 配置元数据（DTD PUBLIC id）；未改任何 Java 生产代码 / 测试逻辑 / header alignment 行为 / controller / 签名 / 限流 / 鉴权 / 鉴别码。

### 残留风险
- 若未来有人把 suppressions PUBLIC id 改回非标准串、或新增引用未知 PUBLIC id 的 XML，将再次触发联网回退。
- 本机/CI runner 的 Checkstyle 版本须保持 10.x（其 `SuppressionsLoader` 含该官方 PUBLIC id 映射）；大版本升级时需复核内置 DTD 映射。
- 本修复消除“正常路径”联网；属深度防御的本地 DTD/catalog（SYSTEM 改本地路径）未引入，保持最小化。

### 准入
quality gate 离线稳定通过，header alignment close review 的环境性阻断已 **UNBLOCKED**；但本轮**不直接标 header alignment CLOSED**。下一步 `DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN`。

## 2026-06-15 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN（header alignment 整体 CLOSED）

### 范围
quality gate 离线 DTD 治理完成后，重跑 header alignment 全链路 close review。只评审 + 记录 close 结论；未改任何 Java/测试，仅更新状态文档。

### 复跑结果（HEAD f260ac7，工作区净）
- `git status --short`：仅本轮状态文档改动；`git diff --check`：无 whitespace error。
- `mvn test`：BUILD SUCCESS。NqDhHeaderNamesTest 2、NqDhHeaderParserTest 5、NqDhHeaderValidatorTest 6、NqFeedbackPayloadSizeGateTest 2、NqFeedbackControllerWebMvcTest 25、NqFeedbackRateLimitWebMvcTest 3、INT0 DhNqIntegration0* 6+2+8=16/16；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：**BUILD SUCCESS / 0 Checkstyle violations / spotless 通过**（离线 DTD 治理后稳定）。

### 逐项确认（17/17）
Batch 1–4 全部仍通过；checkstyle 离线修复未降低 quality gate（仅 1 行 PUBLIC id，SuppressionFilter / 规则集 / profile 不变，0 violations 证明全规则集实跑）；WebMvc 成功路径 canonical-only（唯一 legacy 在 legacy-only 负路径，刻意保留）；INT0 fixtures canonical；legacy-only 仍 403；canonical 成功 202；HEADER_BINDING_MISMATCH 403 正确；HMAC value-based；rate limit key=source+tenant+route；payload 413；nonce replay 409；INT0 16/16。Integration-1 仍禁止。

### Close decision
**header alignment overall：CLOSED。** 全部验收标准满足、quality gate 稳定绿。**CLOSED 仅指 DH 入站 header 对齐完成，不放开 runtime**：Integration-1 / Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED；AI NOT STARTED。

### 修改文件（仅状态文档）
- `DH_NQ_HEADER_ALIGNMENT_PLAN.md`（front-matter 状态 + 顶部进展 + §8 标 CLOSED、next）、`README.md`（标 CLOSED）、`TESTING.md`（§36 close 记录）、`WORKLOG.md`（本条目）。

### 边界确认
未改 NQ / Java 生产代码 / controller 行为 / 测试逻辑；未恢复 legacy；未双接收；未新增 API / migration；未真实 HTTP / NQ / 交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥。

### 后续项（与 close 解耦，均不放开 runtime）
`DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT`、`DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN`（nonce-burn race 防御纵深）、Maven wrapper repair、datasource 默认弱口令治理。

### 准入
header alignment CLOSED。下一步 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT` 或 GateK-PLAN。Integration-1 runtime 仍禁止，须独立 PLAN。

## 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（planning-only）

### 范围
只读核查 DH↔NQ `X-NQ-DH-Timestamp` 线缆格式分歧并输出对齐方案 `docs/current/DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md`。不改运行代码 / 测试；不启动 Integration-1；不跨仓写 NQ。

### 只读核查结论（三方分歧）
- 生产 `HmacNqFeedbackAuthenticator.parseTimestamp` = `Instant.parse` → **RFC3339 / ISO-8601**；签名绑定归一化 `Instant.toString()`（UTC `Z`）；窗口 ±maxClockSkew（默认 300s）。
- `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` §2 = 「冻结为 **epoch 毫秒**」（与生产冲突）。
- DH INT0 fixture（`Int0RequestFactory` / `Int0ContractValidator` / `Int0Contract`）= **epoch 秒**（Long，窗口 ±300s）。
- `CONTRACT_FREEZE` / `CONTRACT_TEST_PLAN` / `AUDIT_REPORT` = 仅 ±300s 窗口，格式中立。
- NQ 仓库本会话不可达（`E:\Project\nexus-quant`）→ NQ 侧格式未当面核对（列 P1-3，可信度中；降级披露见 PLAN §2.5）。

### Findings
- P1-1：生产 RFC3339 与 SECURITY_POLICY epoch 毫秒冲突 → 真实发送方按文档发 epoch 会被 `Instant.parse` 拒绝 → 401，Integration-1 阻断级。
- P1-2：INT0 用 epoch 秒，掩盖 P1-1（不经真实 controller，各自绿）。
- P1-3：NQ 侧未跨仓核对。
- P2：epoch 秒/毫秒单位歧义；RFC3339 子格式（UTC `Z` 规范形）需钉死。

### 推荐决策
canonical = **RFC3339 / ISO-8601 UTC（`Instant.toString()` 规范形，例 `2026-06-15T12:34:56Z`）**；窗口保持 ±300s。依据：生产零改动、跨语言（Java/Py/JS）稳定解析、可读审计、value-based 签名不漂移、规避秒/毫秒歧义。不改 HMAC signatureMaterial；不影响 nonce replay / requestId / traceId / rate limit key / payload gate。不需要运行期兼容期 / 双格式接收。

### 实施分批（本轮不实施）
T1 docs 收口（SECURITY_POLICY epoch 毫秒 -> RFC3339；CONTRACT_FREEZE 钉格式）-> T2 INT0 测试由 epoch 秒改 RFC3339（窗口/语义不变，INT0 16/16 不回归）-> T4 NQ companion 跨仓核对 ->（可选 gated）T3 生产 `parseTimestamp` 收紧 UTC `Z`。

### 边界确认
未改 NQ；未改 Java 生产代码；未改测试；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 wrapper / datasource 弱口令 / nonce-burn race；未读取真实密钥。本轮仅新增 PLAN 文档 + 更新 README/TESTING/WORKLOG。

### 准入
timestamp format alignment PLAN-only / NOT STARTED；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-PLAN-REVIEW`，通过后 IMPL-BATCH-T1。

## 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1（docs 收口，RFC3339 UTC Z）

### 范围
只做 DH 侧 timestamp format 文档收口，将 Integration-0/1 前置契约文档中 `X-NQ-DH-Timestamp` 格式统一为 RFC3339 / ISO-8601 UTC `Z`。不改 Java 生产代码 / 测试 / NQ；不启动 Integration-1。

### 修改文件（仅文档）
- `DH_NQ_INTEGRATION0_SECURITY_POLICY.md`：§2 header 表「epoch 毫秒」→ RFC3339 / ISO-8601 UTC `Z`（例 `2026-06-15T12:34:56Z`）；§4「Timestamp & Replay」加 canonical 格式 + 现状/不变量诚实声明（生产 `Instant.parse`→`Instant.toString()` 归一化、HMAC value-based、header name 不入签、严格 UTC-Z=可选 T3、INT0 epoch 秒待 T2、NQ 侧待 T4 且 Integration-1 前置阻断）；±300s 窗口不变。
- `DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md`：§规则 Timestamp 补 RFC3339 UTC `Z`（拒绝 epoch / 数字偏移），±300s 不变。
- `DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`：T5 / INT0-T05 timestamp 测试 purpose 补 canonical=RFC3339 UTC `Z`；INT0-T05 注明 fixture 当前 epoch 秒待 T2。
- `DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md`：T1 标 DONE、front-matter / §8 状态与 next 更新。
- `README.md` / `TESTING.md`（§38）/ `WORKLOG.md`（本条目）。
- 无 Java / 测试 / NQ 改动。

### 收口要点
- canonical = RFC3339 / ISO-8601 UTC `Z`（例 `2026-06-15T12:34:56Z`）；明确拒绝 epoch 秒/毫秒、数字时区偏移。
- replay 窗口仍 ±300s；HMAC signatureMaterial 不改（value-based、header name 不入签）；生产 `Instant.parse` + `Instant.toString()` 归一化 UTC `Z` 入签。
- 严格 UTC-Z-only 强制 = 可选 T3（未做）；DH INT0 epoch 秒 = T2（未做）；NQ 侧核对 = T4（未做，Integration-1 前置阻断）。
- 未把 timestamp alignment 写成 CLOSED；未把 Integration-1 / Runtime 写成 started；未把 DH 写成 integrated；未把 LIVE 写成 enabled。

### 验证
- `mvn test`：BUILD SUCCESS（回归，无代码改动）。INT0 DhNqIntegration0* 6+2+8=16/16、NqFeedbackControllerWebMvcTest 25、NqDhHeaderValidatorTest 6、NqDhHeaderParserTest 5、NqFeedbackRateLimitWebMvcTest 3、NqFeedbackPayloadSizeGateTest 2；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：BUILD SUCCESS（0 Checkstyle / spotless 通过；离线 DTD 治理后稳定）。
- `git diff --check`：无 whitespace error。

### 边界确认
未改 NQ；未改 Java 生产代码；未改测试；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 INT0 epoch 秒(T2) / NQ companion(T4) / 生产 UTC-Z 收紧(T3) / datasource 弱口令 / wrapper / nonce-burn race；未读取真实密钥。

### 准入
T1 docs 收口 DONE；timestamp alignment 整体 NOT COMPLETED（T2 / T4 待办）；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1-REVIEW`，通过后 T2。

## 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2（INT0 测试对齐 RFC3339 UTC Z）

### 范围
只对齐 DH INT0 contract test/fixture 的 `X-NQ-DH-Timestamp`：epoch 秒 → RFC3339 / ISO-8601 UTC `Z`，使 INT0 与 DH 生产 `Instant.parse` 接受格式一致。不改 Java 生产代码 / NQ；不启动 Integration-1。

### 修改文件（test-only + docs）
- `dh-domain/.../integration0/support/Int0RequestFactory.java`：`H_TIMESTAMP` 由 `Long.toString(nowEpochSeconds)` → `Instant.ofEpochSecond(nowEpochSeconds).toString()`（RFC3339 UTC `Z`）；javadoc 更新；新增 `import java.time.Instant`。
- `dh-domain/.../integration0/support/Int0ContractValidator.java`：timestamp 解析由 `Long.parseLong` → `Instant.parse(...).getEpochSecond()`；非 RFC3339（含 epoch 秒/毫秒）→ `RuntimeException` → `TIMESTAMP_INVALID`（401）；窗口比较仍以秒为单位、保持 ±300s 与 `TIMESTAMP_OUT_OF_WINDOW`；新增 `import java.time.Instant`。
- `dh-domain/.../integration0/DhNqIntegration0SecurityContractTest.java`：`int0T05_timestampWindow` 内补断言（方法数不变）：canonical timestamp 以 `Z` 结尾；epoch 秒字符串 → 401 `TIMESTAMP_INVALID`；epoch 毫秒字符串 → 401 `TIMESTAMP_INVALID`。
- docs：`DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md`（T2 标 DONE、状态/§8 更新）、`README.md`、`TESTING.md`（§39）、`WORKLOG.md`（本条目）。
- 无生产 Java / NQ 改动。

### 不变量
- HMAC（`Int0Signing`）仍 value-based、header name 不入签；timestamp 以其字符串值（现 RFC3339）参与 canonical 签名，签名结构不变。
- header name / requestId / traceId / nonce / tenant / payload / 禁止能力测试 / 禁止字段测试 均未改。
- INT0-T05 语义保持：窗口内通过、过去/未来超窗拒绝（401）；新增格式拒绝为同一 T05 的增量断言。

### 验证
- `mvn test`：BUILD SUCCESS。INT0 DhNqIntegration0* 6+2+8=16/16（SecurityContractTest 8，int0T05 增量断言通过）；NqFeedbackControllerWebMvcTest 25 / NqDhHeaderValidatorTest 6 / NqDhHeaderParserTest 5 / NqFeedbackRateLimitWebMvcTest 3 / NqFeedbackPayloadSizeGateTest 2；ArchUnit 全绿；无 Docker：JDBC 持久化 IT / PostgresContainerSmokeTest 按 disabledWithoutDocker skip。
- `mvn -Pquality validate`：BUILD SUCCESS（0 Checkstyle / spotless 通过）。
- `git diff --check`：无 whitespace error。

### 边界确认
未改 NQ；未改 Java 生产代码；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未处理 T3 生产收紧 / T4 NQ companion / header 命名 stale / datasource 弱口令 / wrapper / nonce-burn race；未读取真实密钥。未把 timestamp alignment 写成 CLOSED、未把 Integration-1 / Runtime 写成 started、未把 DH 写成 integrated、未把 LIVE 写成 enabled。

### 准入
T2 DONE；timestamp alignment 整体 NOT COMPLETED（T4 NQ companion 待办，Integration-1 前置阻断；T3 可选）；Integration-1 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2-REVIEW`。

## 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T3（production / INT0 UTC-Z-only 收紧）

### 范围
将 DH production `X-NQ-DH-Timestamp` 从 `Instant.parse` 可接受任意 RFC3339 offset 收紧为契约要求的 RFC3339 / ISO-8601 UTC `Z`。同步收紧 DH INT0 validator。只做 DH 本地代码、测试与状态文档；不改 NQ、不真实联调、不启动 Integration-1。

### 修改文件
- `dh-security/src/main/java/.../HmacNqFeedbackAuthenticator.java`：`parseTimestamp` 在 `Instant.parse` 前显式要求 `timestampHeader.endsWith("Z")`；非法格式仍返回 `TIMESTAMP_EXPIRED`（401），不新增错误码。
- `dh-security/src/test/java/.../HmacNqFeedbackAuthenticatorTest.java`：补 RFC3339 UTC `Z` accept（既有成功路径）、epoch seconds reject、epoch milliseconds reject、`+08:00` reject、过去/未来超出 ±300s reject。
- `dh-domain/src/test/java/.../integration0/support/Int0ContractValidator.java`：INT0 timestamp 校验同步要求 UTC `Z`，拒绝数字时区偏移；非法格式仍为 `TIMESTAMP_INVALID`（401）。
- `dh-domain/src/test/java/.../integration0/DhNqIntegration0SecurityContractTest.java`：INT0-T05 补 `+08:00` reject，签名按 offset header 重算，证明旧行为会被接受而新行为 fail-closed。
- docs：`DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md`、`DH_NQ_INTEGRATION0_SECURITY_POLICY.md`、`README.md`、`TESTING.md`、`WORKLOG.md`。

### 不变量
HMAC signatureMaterial 仍 value-based、header name 不入签；验签仍使用 `timestamp.toString()` 归一化 UTC `Z` 值；nonce / source / tenant / requestId / traceId / payload 语义不变；±300s replay window 不变。

### 验证
- `mvn -pl dh-domain,dh-security -am "-Dtest=HmacNqFeedbackAuthenticatorTest,DhNqIntegration0SecurityContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；INT0 SecurityContractTest 8/8、HmacNqFeedbackAuthenticatorTest 8/8。
- `git status --short`：仅本轮 8 个文件变更（后续补 `TESTING.md` 后为 9 个）。
- `git diff --check`：exit 0；仅 LF/CRLF warning，无 whitespace error。
- `git diff --stat`：8 files changed, 146 insertions(+), 18 deletions(-)（补 `TESTING.md` 前）。
- `mvn test`：BUILD SUCCESS；INT0 6+2+8=16/16，HmacNqFeedbackAuthenticatorTest 8/8，WebMvc 25/25，parser 5/5，validator 6/6，rate limit 3/3，payload gate 2/2，ArchUnit 12/12；本机无 Docker，既有 PostgresContainerSmokeTest skip 1。
- `mvn -Pquality validate`：BUILD SUCCESS；0 Checkstyle violations，spotless 通过。

### 边界确认
未改 NQ；未新增 API / migration；未真实 HTTP / DH-NQ 调用 / 交易所调用；未新增 RealClient / 真实 Provider；未读取凭证；未启动 Integration-1；未开启 LIVE；未处理 Maven wrapper / datasource 默认弱口令 / nonce-burn race；未改变 HMAC signatureMaterial 字段集合；未引入双格式兼容；未接受 epoch fallback。

### 准入
T3 后续 review 已 ACCEPTED；timestamp alignment 后续由 FINALIZE 收口为 CLOSED / ACCEPTED。Integration-1 仍 NOT STARTED；Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。

## 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-FINALIZE

### 范围
一次性完成 DH-NQ timestamp format alignment 最终收口：更新 DH current docs 中 T3/T4/overall 旧状态，修正 DH Integration-0 contract freeze 里的 legacy `X-DH-NQ-*` stale 声明，并与 NQ companion 当前事实对齐。只改允许的 `docs/current` 文档；不改 DH/NQ Java production code，不改测试代码，不新增 API / migration / RealClient / real provider，不真实 HTTP，不启动 Integration-1。

### Final state
`X-NQ-DH-Timestamp` canonical contract 已 **CLOSED / ACCEPTED**：

- T1 docs：ACCEPTED。
- T2 DH INT0：ACCEPTED。
- T3 DH production / INT0 UTC-Z-only hardening：ACCEPTED。
- T4 NQ companion：ACCEPTED。
- canonical timestamp：RFC3339 / ISO-8601 UTC `Z`，例 `2026-06-15T12:34:56Z`。
- DH production 已强制 `endsWith("Z") + Instant.parse`。
- DH / NQ INT0 均拒绝 epoch seconds、epoch milliseconds、数字时区偏移。
- replay window 保持 ±300s；HMAC signatureMaterial value-based 且 header name 不入签。

### 边界
timestamp CLOSED 只表示 timestamp 契约收口完成，不授权 Integration-1 runtime。Integration-1 / Runtime integration 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。未处理 Maven wrapper / datasource 默认弱口令 / nonce-burn race，未读取或输出凭证。

### 验证
- DH `git status --short`：仅允许的 7 个 `docs/current` 文件 modified。
- DH `git diff --check`：通过；仅 LF/CRLF warning，无 whitespace error。
- DH `git diff --stat`：7 files changed, 141 insertions(+), 134 deletions(-)。
- DH `mvn test`：BUILD SUCCESS；既有 `PostgresContainerSmokeTest` 因本机无 Docker skipped 1。
- DH `mvn -Pquality validate`：BUILD SUCCESS；0 Checkstyle violations；Spotless check 通过。
- NQ `git status --short`：仅允许的 8 个 `docs/current` 文件 modified。
- NQ `git diff --check`：通过；仅 LF/CRLF warning，无 whitespace error。
- NQ `git diff --stat`：8 files changed, 55 insertions(+), 12 deletions(-)。
- NQ `mvn -f backend/pom.xml test`：BUILD SUCCESS。
- NQ `mvn -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；INT0 6+2+9=17/17。
- NQ backend POM quality profile 探测：未检出 `<id>quality</id>` / `spotless` / `checkstyle`，未伪造 quality gate 成功。

## 2026-07-01 DH-DOCS-SKILL-SYNC-FROM-NQ

### 范围
完成 `DH-DOCS-SKILL-SYNC-FROM-NQ` 文档治理同步：以 NQ `nq-docs-writer` 为参考，新增 DH 本地 `dh-docs-writer` skill，并把 DH 当前事实源、workflow index、状态入口和 next-stage 口径同步到 `docs/current`。本轮只做 docs / skill / project rules，不改 DH 生产代码、测试代码、API、migration、runtime 配置、contracts、golden_cases 或 NQ 仓库。

### 修改文件
- 新增 `.agents/skills/dh-docs-writer/SKILL.md`：定义 DH 文档治理规则、事实源优先级、Integration-0 / GateK / GateN / Decision Pipeline 边界、状态词汇、验证要求和禁止跨界项。
- 新增 `.agents/skills/dh-docs-writer/agents/openai.yaml`：由 `skill-creator` 初始化生成的 skill metadata。
- 更新 `AGENTS.md`：将 active skills 从 9 个扩展为 10 个，加入 `dh-docs-writer`；同步 current stage、next stage、GateN rebase、Decision Pipeline MVP PLAN 与 no runtime / no LIVE 边界。
- 更新 `README.md`、`docs/current/README.md`、`docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/CODEX_WORKFLOW_INDEX.md`、`docs/current/DH_CODEX_PLUGIN_WORKFLOW.md`、`docs/current/DH_NQ_INTEGRATION.md`、`docs/current/STATUS.md`、`docs/current/ROADMAP.md`、`docs/current/WORK_ORDER.md`：同步 DH 当前口径、stage 入口、workflow 入口、NQ/DH integration 边界、work order 和旧 Stage4 plan superseded 说明。
- 更新 `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录本轮验证、阻塞项、边界确认和工作日志。

### 不变量
Integration-0 safety gate 仍为 CLOSED / ACCEPTED；P1-4 residual rate limit / memory cap / replay nonce persistence、header alignment、timestamp alignment 均保持 CLOSED。Integration-1 / runtime integration / DH integrated / AI / LangGraph / real provider / RealClient / real HTTP / LIVE 均保持 NOT STARTED 或 DISABLED。Decision Pipeline MVP 当前只允许 PLAN：只读 recommendation / evidence / audit / fail-closed 语义，不允许 BUY / SELL / PLACE_ORDER / CANCEL_ORDER / NQ mutation。

### 验证
- `git status --short`：仅本轮文档与 `.agents/skills/dh-docs-writer/` 变更。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff --stat`：已记录 tracked docs 变更统计；新增 skill 文件由 `git status` 标识为 untracked。
- `git diff -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases`：空 diff。
- 敏感词扫描：仅命中文档禁止清单和安全规则文字；未发现真实凭证值。
- `skill-creator quick_validate.py`：被执行环境阻塞，bundled Python 缺少 `yaml` 模块；手工等价检查通过（frontmatter、无模板 TODO、`agents/openai.yaml` 存在且 prompt 使用 `$dh-docs-writer`）。
- `mvn test`：未进入测试执行；默认 Maven 本地仓库 tracking file 写入报 `FileAlreadyExistsException`，改用临时本地仓库并非沙箱重跑后被 Aliyun Maven 依赖下载 TLS handshake 中断阻塞。
- `mvn -Pquality validate`：未进入 Checkstyle / Spotless；被 Aliyun Maven 下载 `maven-checkstyle-plugin:3.3.1` TLS handshake 中断阻塞。

### 边界确认
未修改 NQ 仓库；未改 DH Java production / test code；未新增 API / migration；未改 contracts / golden_cases；未真实 HTTP；未接 NQ runtime；未接真实 provider；未接 AI / LangGraph；未读取密钥；未开启 LIVE。

### 准入
docs / skill 变更已完成 Git 级验证。Maven 回归受外部依赖下载和本地 Maven 仓库状态阻塞，需在依赖仓库可用或本地 Maven 仓库修复后重跑 `mvn test` 与 `mvn -Pquality validate`。下一步保持 `DH-STAGE4-DECISION-PIPELINE-MVP-PLAN`，不得回退到 superseded 的旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK`。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-PLAN

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-PLAN` docs-only / plan-only 规划：新增 Decision Pipeline MVP 当前计划文档，并同步附件允许范围内的 `docs/current` 入口、状态、路线图、工单、API 说明、验证记录和工作日志。本轮不改 Java 生产代码、测试代码、API、migration、contracts、golden_cases、runtime 配置或 NQ 仓库。

### 修改文件
- 新增 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`：定义 K0-K7 分批计划、Decision contract、audit / snapshot / trace / replay、mock provider、mock NQ dry-run contract test、golden cases、安全矩阵、验收与 readiness gate。
- 更新 `docs/current/README.md`：把当前阶段同步为 `DH-STAGE4-DECISION-PIPELINE-MVP-PLAN / READY FOR REVIEW`，下一阶段同步为 `DH-STAGE4-DECISION-PIPELINE-MVP-WO / NOT STARTED`，并加入新计划文档入口。
- 更新 `docs/current/STATUS.md`、`docs/current/ROADMAP.md`、`docs/current/WORK_ORDER.md`：同步 Stage4 plan ready 状态、K0-K7 范围、next concrete action 和禁止直接实现 runtime 的边界。
- 更新 `docs/current/API.md`：明确 Decision Pipeline API / runtime 仍 NOT IMPLEMENTED，旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` 仍 `SUPERSEDED / REBASE_REQUIRED`。
- 更新 `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录本轮真实验证结果、Maven 配置 RCA、边界确认和工作日志。

### Plan result
计划产物将 Stage4 MVP 拆成 K0-K7：Factsource Sync、Decision Contract Freeze、DecisionOrchestrator Skeleton Plan、Audit / Snapshot / Trace / Replay Plan、Mock Provider / Provider Health Plan、Mock NQ Dry-run Contract Test Plan、Golden Cases / Eval Plan、Acceptance / Freeze Plan。目标 contract 只允许 `READ_ONLY_RECOMMENDATION`，候选动作限定为 `ABSTAIN | OBSERVE | NO_TRADE | LONG_BIAS | SHORT_BIAS`，禁止 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER / NQ mutation`。

### Readiness
- `ALLOW_STAGE4_PLAN_CLOSE: YES`
- `ALLOW_STAGE4_WO: YES`
- `ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO`
- `ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 验证
- `git status --short`：仅 `docs/current` 计划与状态同步文件变更；新增 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases`：空 diff。
- 敏感词扫描：仅命中文档禁止清单和安全边界文字；未发现真实凭证值。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；既有 `PostgresContainerSmokeTest` 因本机无可用 Docker 环境 skipped 1。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS。

### 边界确认
Integration-0 仍 `CLOSED / ACCEPTED`，但只代表 contract / mock / documentation work line；Integration-1 `NOT STARTED`；Runtime integration `NOT STARTED`；DH integrated `NO`；AI / Agent runtime `NOT STARTED`；LIVE `DISABLED`。未接真实 NQ runtime、真实 provider、RealClient、真实 HTTP、交易、NQ DB 或凭证。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-WO / NOT STARTED` 的 work order 评审与冻结；不得跳过 WO 直接实现 Decision Pipeline runtime。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-WO

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-WO` docs-only / work-order-only 工单设计：基于已 `ACCEPTED / CLOSED` 的 Stage4 plan，新增 K1-K8 可执行批次，并同步 `docs/current` 入口、状态、路线图、当前工单、验证记录和工作日志。本轮不改 Java 生产代码、测试代码、API、migration、contracts、golden_cases、runtime 配置或 NQ 仓库。

### 修改文件
- 新增 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md`：定义 K1-K8 批次目标、allowed / forbidden files、生产/测试/API/migration 权限、主要类/接口、schema/table、测试、验证命令、边界、退出条件、回滚方式和下一批次。
- 更新 `docs/current/README.md`：把当前阶段同步为 `DH-STAGE4-DECISION-PIPELINE-MVP-WO / READY FOR REVIEW`，下一阶段同步为 `DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE / NOT STARTED`，并加入 WO 文档入口。
- 更新 `docs/current/STATUS.md`：把 Stage4 plan 标记为 `ACCEPTED / CLOSED`，新增 §1.6 WO ready 记录、K1 next、DecisionOutput hardening 和 readiness decision。
- 更新 `docs/current/ROADMAP.md`：把路线推进到 WO ready / K1 next，并替换 Stage4 WO 验收条件。
- 更新 `docs/current/WORK_ORDER.md`：把当前工单入口从“产出 WO”切换为“K1 Contract Freeze”，补充 WO artifact、批次顺序、DecisionOutput hardening、readiness decision 和 K1 开工提示词。
- 更新 `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录本轮真实验证结果、Maven 本机环境 RCA、边界确认和工作日志。

### Work order result
K1-K8 批次如下：

```text
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton
K3 Audit / Snapshot / Trace Persistence
K4 Replay Read Model
K5 Mock Provider / Provider Health / Budget / Latency
K6 Mock NQ Dry-run Contract Tests
K7 Golden Cases / Eval Baseline
K8 Acceptance / Freeze
```

顺序锁定：

```text
K1 after review before K2
K2 after review before K3
K3 after review before K4
K1-K5 complete before K6
K1-K7 complete before K8
Before Stage4 MVP closed: no Integration-1 runtime, no LangGraph runtime
LangGraph GateL or later
```

### Key decisions
- `DecisionOutput.decisionType` 固定为 `READ_ONLY_RECOMMENDATION`。
- action vocabulary 仅允许 `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`。
- 默认 `ABSTAIN`；no evidence -> `ABSTAIN`；provider failure -> `ABSTAIN`；policy denied -> `BLOCKED` or `ABSTAIN` fail-closed；high risk 禁止 `LONG_BIAS / SHORT_BIAS`。
- `forbiddenActions` 必须包含 `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。
- 最终输出必须为 structured JSON，不允许 free-text final output，不允许真实交易指令。

### Readiness
- `ALLOW_WO_CLOSE: YES`
- `ALLOW_K1_IMPLEMENTATION: YES`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- `git status --short`：仅 docs/current 文档变更；新增 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md`。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff --stat`：执行成功；tracked docs/current 文件存在 diff，新增 WO 文件由 `git status` 标识为 untracked。
- 状态残留扫描：未发现把当前阶段写回 PLAN ready 或 WO not started 的残留。
- readiness forbidden YES 扫描：未发现 `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: YES`、`ALLOW_INTEGRATION_1_RUNTIME: YES`、`ALLOW_AGENT_PHASE: YES`、`ALLOW_LANGGRAPH_RUNTIME: YES` 或 `ALLOW_LIVE: YES`。
- 裸 `mvn test`：被本机全局 Maven settings / repository 阻断，未进入测试执行；关键错误为 `settings.xml` line 227 `Unrecognised tag: profiles` 与 `FileAlreadyExistsException`。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；既有 `PostgresContainerSmokeTest` 因本机无可用 Docker 环境 skipped 1。
- 裸 `mvn -Pquality validate`：被同一本机 Maven 环境问题阻断，未进入 quality 执行。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过；最终重跑完成于 2026-07-01T13:48:23+08:00。

### 边界确认
Integration-0 仍 `CLOSED / ACCEPTED`，但只代表 contract / mock / documentation work line；Integration-1 `NOT STARTED`；Runtime integration `NOT STARTED`；DH integrated `NO`；AI / Agent runtime `NOT STARTED`；LangGraph runtime `NOT STARTED`；LIVE `DISABLED`。未接真实 NQ runtime、真实 provider、RealClient、真实 HTTP、交易、NQ DB 或凭证。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE / NOT STARTED`，只允许单批执行 K1 Decision Contract Freeze；不得跳过 K1 review 或全量 GateK implementation。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE` single-batch implementation。K1 只冻结 Decision Pipeline MVP 的 domain contract、enum、JSON Schema 和 contract tests；不实现 K2-K8，不新增 API、migration、runtime、provider、client、repository、service、controller 或 NQ 集成。

### 修改文件
- 新增 `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/`：
  `DecisionRequest`、`DecisionOutput`、`DecisionSubject`、`DecisionContextSnapshot`、
  `DecisionEvidence`、`DecisionPolicyResult`、`DecisionRiskReview`、`ProviderDecisionSignal`、
  `DecisionTraceStep`、`DecisionAuditEvent`、`DecisionType`、`DecisionAction`、
  `DecisionRiskLevel`、`DecisionPolicyStatus`、`ProviderSignalStatus`、`ForbiddenAction`、
  `DecisionStatus`。
- 新增 `contracts/json-schema/dh-decision-request.schema.json`：冻结 DH read-only recommendation request schema。
- 新增 `contracts/json-schema/dh-decision-output.schema.json`：冻结 DH structured read-only recommendation output schema。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/DecisionRequestContractTest.java`。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/domain/decision/DecisionOutputContractTest.java`。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionRequestSchemaContractTest.java`。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionOutputSchemaContractTest.java`。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionEnumContractTest.java`。
- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionNoTradingInstructionContractTest.java`。
- 更新 `docs/current/README.md`、`docs/current/STATUS.md`、`docs/current/ROADMAP.md`、`docs/current/WORK_ORDER.md`：同步 K1 implemented / ready-for-review，下一步仅 K1 review，K2 still NOT STARTED。
- 更新 `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录验证证据、边界确认和 readiness decision。

### Implementation
- `DecisionType` 仅包含 `READ_ONLY_RECOMMENDATION`。
- `DecisionAction` 仅包含 `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`，不含 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER`。
- `ForbiddenAction` 固定五项：`PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。
- `DecisionRequest` 只表达 read-only recommendation request；字段为 request / trace / tenant / source / subject / context / requestedAt / schemaVersion，不含 account、quantity、price、side、credential 或 execution fields。
- `DecisionOutput` 默认 fail-closed：no evidence -> `ABSTAIN`；provider failure -> `ABSTAIN`；policy denied -> `BLOCKED` + `ABSTAIN`；high / blocked risk 禁止 `LONG_BIAS / SHORT_BIAS`；每个 output 都强制包含 mandatory forbidden actions。
- JSON Schema 使用仓库既有 `draft/2020-12` 风格，root `additionalProperties=false`，enum 与 Java enum 一致。

### Tests
新增 K1 tests 22 cases：

```text
DecisionRequestContractTest: 1
DecisionOutputContractTest: 4
DecisionRequestSchemaContractTest: 5
DecisionOutputSchemaContractTest: 7
DecisionEnumContractTest: 3
DecisionNoTradingInstructionContractTest: 2
```

覆盖项：

```text
schema 文件存在性
required 字段完整
additionalProperties=false
Java enum 与 schema enum 一致
action enum 禁止 BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER
decisionType 仅 READ_ONLY_RECOMMENDATION
forbiddenActions 固定五项
request schema 不含 apiKey / apiSecret / passphrase / account credential / execution intent
response schema 不含 free-text final output / execution command fields
domain no evidence / provider failure / policy denied / high risk fail-closed 行为
```

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- `git status --short`：仅 K1 允许范围内 docs/current、contracts/json-schema、dh-domain main/test 变更。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff --stat`：执行成功；tracked docs/current 文件存在 diff；新增 K1 schema / Java / tests 由 `git status` 标识为 untracked。
- 裸 `mvn test`：被本机全局 Maven settings / repository 阻断，未进入测试执行；关键错误仍为 `settings.xml` line 227 `Unrecognised tag: profiles` 与 `FileAlreadyExistsException`。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-domain -am test`：BUILD SUCCESS；dh-domain 108 tests；0 failures；0 errors；0 skipped。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；既有 `PostgresContainerSmokeTest` 因本机无可用 Docker skipped 1。
- 裸 `mvn -Pquality validate`：被同一本机 Maven 环境问题阻断，未进入 quality 执行。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过。
- `idea-mcp get_file_problems(errorsOnly=true)`：`DecisionOutput.java`、`DecisionRequest.java`、`DecisionOutputSchemaContractTest.java`、`DecisionRequestSchemaContractTest.java` 均无 error 级问题。

### Readiness
- `ALLOW_K1_CLOSE: YES`
- `ALLOW_K2_IMPLEMENTATION: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 边界确认
未实现 `DecisionOrchestrator`；未实现 `DecisionContextBuilder`；未实现 `MockDecisionProvider`；未实现 RiskReview 流水线；未实现 policy evaluator 生产逻辑；未新增 API path / Controller / migration / Repository / Service / Client；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未接 MCP 写能力；未实现 replay API；未实现 audit / snapshot / trace 表；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库。

### 剩余风险
- `DecisionOrchestrator` 尚未实现。
- Audit / snapshot / trace / replay 尚未实现。
- Mock provider 尚未实现。
- Mock NQ dry-run contract tests 尚未实现。
- Integration-1 仍需基于 NQ GateN 重新规划。
- LangGraph 后置。
- Agent phase 后置。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW / NOT STARTED`；不得直接进入 K2。

## 2026-07-01 DH-DOCS-LANGUAGE-GOVERNANCE-FIX

### 范围
完成 `DH-DOCS-LANGUAGE-GOVERNANCE-FIX` 文档治理修复：补齐 DH 文档语言政策，修正 `docs/current` 新增规划 / 工单文档中的明显英文漂移，并同步项目规则入口。本轮只改文档、skill 与项目规则文件；不改 Java 生产代码、测试代码、contracts、golden_cases、API path、migration、runtime、provider、NQ runtime 或 LIVE。

预检时工作区已存在上一轮 K1 的 untracked `contracts/json-schema/**`、`dh-domain/src/main/**` 与 `dh-domain/src/test/**` 文件。本轮未编辑这些文件，也未回退它们。

### 修改文件
- `.agents/skills/dh-docs-writer/SKILL.md`：新增 `Language Rules`，要求 DH 文档正文、架构说明、阶段计划、Worklog、Testing、Roadmap、Status 与代码注释 / Javadoc 默认中文为主。
- `AGENTS.md`：同步当前 K1 review 入口，新增语言治理规则，明确从 NQ skill 同步时必须改写为 DH 中文主语言风格。
- `README.md`、`docs/current/README.md`：同步当前 K1 review 入口和语言治理规则。
- `docs/current/CODEX_PROJECT_INSTRUCTIONS.md`、`docs/current/CODEX_WORKFLOW_INDEX.md`：同步 language policy 与当前主线。
- `docs/current/API.md`：修正 stale Stage4 PLAN 状态，明确 K1 已落 domain/schema 合同但 Decision Pipeline API 仍 `NOT IMPLEMENTED`。
- `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`：把文档主体、章节标题、表格列名和说明段落改为中文，保留稳定工程名词、enum、schema key、状态词和命令。
- `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md`：把工单主体、批次字段、表格列名和说明段落改为中文，并把下一轮提示改为 K1 review。
- `docs/current/STATUS.md`、`docs/current/ROADMAP.md`、`docs/current/WORK_ORDER.md`：追加语言治理状态与规则，不改变 K1 review 主线。
- `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录本轮验证和边界确认。

### 语言规则
DH 文档正文、架构说明、阶段计划、WORKLOG、TESTING、ROADMAP、STATUS 默认中文为主。DH 代码注释 / Javadoc 默认中文为主。

允许保留英文：Java 包名 / 类名 / 方法名 / 字段名、enum 值、JSON Schema 字段、OpenAPI 字段、HTTP header、状态枚举、Maven / Spring / Git / Docker / CI 命令、LangGraph / Spring AI / MCP / PostgreSQL 等外部技术名，以及 `Task classification`、`Scope`、`Validation`、`Risks`、`Next concrete action` 等固定输出字段。

固定输出字段可保留英文，但字段内容必须中文为主。不得新增英文长段落；不得为了“专业感”把中文业务概念翻译成不稳定英文术语后反复使用；如果 Codex 输出英文大段内容，必须在同轮改为中文。

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- `git status --short`：tracked 变更仅为允许的 docs / skill / project rule 文件；预检前既有 K1 untracked contracts / dh-domain / test files 仍存在，本轮未触碰。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff --stat`：执行成功；显示 14 个 tracked docs / skill / project rule 文件存在 diff。
- 指定 `rg` 扫描：仅命中 `.agents/skills/dh-docs-writer/SKILL.md` 的固定输出字段模板 `Boundary confirmation:`，判定允许保留。
- `git diff --name-only -- dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases`：空，tracked diff 未触碰禁止范围。
- `git ls-files --others --exclude-standard dh-domain dh-usecase dh-memory dh-eval dh-connector dh-api dh-app dh-infra contracts golden_cases`：列出预检前已存在的 K1 untracked 文件，作为既有工作区状态记录。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 41.525 s；Finished at 2026-07-01T14:13:48+08:00；既有 `PostgresContainerSmokeTest` 因本机无有效 Docker 环境 skipped 1。
- `mvn -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check 通过；Total time 7.526 s；Finished at 2026-07-01T14:14:06+08:00。

### 边界确认
未修改 Java 生产代码；未修改测试代码；未修改 contracts；未修改 golden_cases；未新增 API；未新增 migration；未实现或启动 K1 新逻辑；未启动 K2；未接 NQ；未真实 HTTP；未接真实 provider；未接 AI / LangGraph；未开启 LIVE；未把 Integration-1 / Runtime integration / Agent phase 写成 started。

### 风险
`docs/current` 历史记录与冻结快照中仍可能存在历史英文术语、命令、状态枚举或 fixed output labels；本轮只修正当前允许文件中的明显漂移，不改历史 gate snapshot。当前附件写明“暂停 K1 / K1 NOT STARTED”，但工作区与 `docs/current/STATUS.md` 在预检时已是 K1 `IMPLEMENTED / READY FOR REVIEW`；本轮保留当前事实源，不回退既有 K1 变更。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW / NOT STARTED`；不得直接进入 K2。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON` single-batch implementation。K2 只在 `dh-usecase` 落 mock-only DecisionOrchestrator skeleton，并补 K2 单元测试与 `docs/current` 状态同步；不实现 K3-K8、不新增 API / Controller / Repository / JDBC / migration、不接真实 provider / NQ / HTTP / LangGraph / LIVE。

### 修改文件
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionOutput.java`：补充 `observation(...)` 与 `abstainForRisk(...)` 工厂方法，供 K2 复用 K1 output contract，不新增第二套 response model。
- 新增 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestrator.java` 与 `DefaultDecisionOrchestrator.java`：固定 policy -> context -> mock signal -> risk -> output assembler 流程，异常统一 fail-closed。
- 新增 `DecisionContext`、`DecisionContextBuilder`、`DefaultDecisionContextBuilder`：只读取 K1 request 内 `contextSnapshot.evidenceRefs`，不读 DB / 文件 / NQ / HTTP。
- 新增 `DecisionPolicyChecker`、`DefaultDecisionPolicyChecker`：阻断 null request、非 read-only、执行意图、账户 / 凭证 / 订单 token。
- 新增 `DecisionSignalProvider`、`DecisionSignalResult`、`MockDecisionSignalProvider`：deterministic mock-only，默认 `MOCKED + NO_TRADE`，拒绝 `SUCCESS`。
- 新增 `DecisionRiskReviewer`、`DefaultDecisionRiskReviewer`：无 evidence / provider failure -> UNKNOWN，`HIGH_RISK` marker -> HIGH，其余 mock-only -> LOW。
- 新增 `DecisionOutputAssembler`：统一组装 structured output，policy denied / no evidence / provider failure / high risk / unexpected failure 均 fail-closed。
- 新增 K2 单元测试：`DecisionOrchestratorTest`、`DecisionOutputAssemblerTest`、`DefaultDecisionPolicyCheckerTest`、`DefaultDecisionRiskReviewerTest`、`MockDecisionSignalProviderTest`。
- 更新 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`：同步 K1 closed、K2 ready-for-review、K3 not started 与验证证据。

### Implementation
- K2 orchestrator 默认构造器使用本地 deterministic 组件，无 Spring wiring、无配置项、无 runtime provider。
- `DefaultDecisionPolicyChecker` 在 provider 前阻断执行意图；测试覆盖 `placeOrder` / `cancelOrder` 负向输入。
- `DefaultDecisionContextBuilder` 只复制 request snapshot evidence，不查询外部资源。
- `MockDecisionSignalProvider` 默认 `NO_TRADE`，并拒绝 `ProviderSignalStatus.SUCCESS`，确保 K2 不模拟真实 provider success。
- `DefaultDecisionRiskReviewer` 对无证据、provider failure 和 high risk 均 fail-closed。
- `DecisionOutputAssembler` 保证输出始终是 K1 `DecisionOutput`，并保留 mandatory `forbiddenActions`。

### Tests
新增 K2 tests 22 cases：

```text
DecisionOrchestratorTest: 7
DecisionOutputAssemblerTest: 4
DefaultDecisionPolicyCheckerTest: 4
DefaultDecisionRiskReviewerTest: 4
MockDecisionSignalProviderTest: 3
```

覆盖项：

```text
正常 mock 输出
无 evidence -> ABSTAIN
forbidden execution intent -> BLOCKED
provider timeout / failure -> ABSTAIN
high risk directional signal -> ABSTAIN
unexpected internal failure -> structured ABSTAIN
null request -> BLOCKED / INVALID
mock provider rejects SUCCESS
mandatory forbiddenActions 保持固定
```

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- 初始 `git status --short`：clean。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- K2 禁止范围 `rg` 扫描：仅命中 policy denylist、负向测试字符串和禁止项注释；未新增 Controller / Repository / JDBC / migration / HTTP client / RealClient / real provider / OpenAI / Claude / Gemini / BUY / SELL / accountId 生产实现。
- 裸 `mvn -pl dh-usecase -am test`：被本机全局 Maven settings / repository 阻断，未进入编译；关键错误为 `settings.xml` line 227 `Unrecognised tag: profiles` 与 `FileAlreadyExistsException`。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-usecase -am test`：BUILD SUCCESS；reactor 9/9 SUCCESS；dh-domain 108 tests、dh-connector 19 tests、dh-usecase 92 tests，全部 0 failures / 0 errors / 0 skipped；Finished at 2026-07-01T15:28:42+08:00。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality -pl dh-usecase -am validate`：BUILD SUCCESS；reactor 9/9 SUCCESS；Finished at 2026-07-01T15:28:37+08:00。
- 直接 `spotless:check`：失败于既有 `dh-common` / `dh-domain` 大量格式漂移，不属于本轮 K2 专属变更；未执行 `spotless:apply`，避免扩大范围。

### Readiness
- `ALLOW_K2_CLOSE: YES`
- `ALLOW_K3_IMPLEMENTATION: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 边界确认
未实现 K3 audit / snapshot / trace / replay persistence；未新增 API path / Controller / Repository / JDBC / migration；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未接 MCP 写能力；未实现 replay API；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库。

### 剩余风险
- K2 尚未 review。
- K3 audit / snapshot / trace / replay persistence 尚未实现。
- Mock provider health / budget / latency 尚未实现。
- Mock NQ dry-run contract tests 尚未实现。
- Direct Spotless check 被既有全仓格式漂移阻断；本轮未做跨范围格式化。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON-REVIEW / NOT STARTED`；不得直接进入 K3。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE` single-batch implementation。K3 只落 DH-owned audit / snapshot / trace persistence，不实现 K4-K8，不新增 API / Controller / replay API，不接真实 provider / NQ / HTTP / LangGraph / LIVE。

### 修改文件
- 新增 `dh-app/src/main/resources/db/migration/V5__dh_decision_pipeline_audit.sql`：六张 K3 表，含 `decision_id` / `trace_id` / `tenant_id`、`jsonb`、`timestamptz`、索引、check constraint 与中文 comment。
- 新增 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionAuditRepository.java`、`DecisionPersistenceRecords.java`、`DecisionPersistenceException.java`、`InMemoryDecisionAuditRepository.java`、`DecisionTraceStepName.java`、`DecisionTraceStepStatus.java`、`DecisionAuditEventType.java`、`DecisionAuditEventStatus.java`。
- 修改 `DefaultDecisionOrchestrator.java`：接入 request / snapshot / trace / provider call / output / audit 持久化，persistence failure 统一 fail-closed。
- 修改 `DecisionOutputAssembler.java`：新增 `persistenceFailure(...)` structured output。
- 新增 `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java`：insert-only JDBC adapter，JSONB 使用 `CAST(? AS jsonb)`，数据库/序列化失败转 `DecisionPersistenceException`。
- 新增 `dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java`：装配 K3 repository 与 `DecisionOrchestrator`，不新增 Controller。
- 新增 `DecisionOrchestratorPersistenceTest`、`JdbcDecisionAuditRepositoryTest`、`V5DecisionPipelineAuditMigrationPresenceTest`。
- 更新 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`。

### Implementation
- `DefaultDecisionOrchestrator` 正常路径写入 request、policy/context/provider/risk/output/audit trace、context snapshot、mock provider call summary、decision output 和 audit event。
- audit / snapshot / trace / output 任一写失败都会返回 `ABSTAIN` + `PERSISTENCE_FAILURE`；如果 fail-closed output / audit 也无法写入，仍返回结构化 fail-closed output。
- `output_json` 不直接序列化 `DecisionOutput` domain object，改为显式安全 Map；`createdAt` 写 ISO 字符串，避免 JavaTime module 依赖和内部模型暴露。
- request persistence 会脱敏 sensitive token 与 `placeOrder` / `cancelOrder` 等 execution-intent token，避免审计表保存可被误读为执行指令或凭证片段的原始字符串。
- `provider_call_log.signal_json` 只保存 deterministic mock summary，不保存真实 provider raw response、credential、NQ DB 内容或敏感 header。
- V5 migration 只创建 DH-owned audit tables，不创建 orders / trades / fills / positions / live / NQ-owned 表。

### Tests
新增 K3 tests 24 cases：

```text
DecisionOrchestratorPersistenceTest: 10
JdbcDecisionAuditRepositoryTest: 9
V5DecisionPipelineAuditMigrationPresenceTest: 5
```

覆盖项：

```text
valid write-through
policy denied
provider timeout
high risk no directional bias
request / context snapshot / output / audit persistence failure fail-closed
missing request unknown IDs
request persistence sensitive / execution-intent redaction
JDBC SQL target table / JSONB cast / exception mapping
V5 migration presence / indexes / comments / no trading runtime tables
```

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- `git status --short`：仅 K3 允许范围内的 `dh-usecase` decision、`dh-infra` JDBC decision、`dh-app` V5 migration/wiring/tests 与 `docs/current` 状态文档变更；未自动 stage。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- `git diff --stat`：tracked diff 8 files changed, 923 insertions(+), 96 deletions(-)；新增 K3 文件由 `git status --short` 标识为 untracked。
- K3 边界关键词扫描：命中项均为禁止说明、migration comment、denylist 或负向测试字符串；未发现真实 runtime provider、HTTP client、Controller、NQ client、Exchange/Broker、BUY/SELL action 实现或 LIVE 启用。
- `docs/current` 当前状态残留扫描：未发现 current stage / next stage 仍指向 K2 review；未发现 K3 current 写成 NOT STARTED；未发现 forbidden readiness 写成 YES。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-usecase,dh-infra -am test`：初始失败一次，RCA 为 `output_json` 直接序列化 `DecisionOutput.createdAt` 依赖 JavaTime module；修复为显式 Map 后 BUILD SUCCESS。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Surefire 汇总 375 tests / 0 failures / 0 errors / 4 skipped。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- 4 skipped 为本机无 Docker 的既有 Testcontainers 环境项：`JdbcNonceReplayGuardPersistenceTest` 3 + `PostgresContainerSmokeTest` 1。

### Readiness
- `ALLOW_K3_CLOSE: YES`
- `ALLOW_K4_IMPLEMENTATION: NO`
- `ALLOW_STAGE4_M1_CLOSE_REVIEW: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 边界确认
未新增 API path；未新增 Controller；未新增 replay API / query endpoint；未实现 K4 Replay Read Model；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未实现 mock NQ dry-run contract tests；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库。

### 剩余风险
- M1 readiness review 尚未开始。
- K4 Replay Read Model 尚未实现。
- K5 provider health / budget / latency 尚未实现。
- K6 mock NQ dry-run contract tests 尚未实现。
- Testcontainers 真实 Postgres 持久化项因本机无 Docker 仍跳过；需要 CI / Docker 环境另行跑。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-M1-READINESS-REVIEW / NOT STARTED`；不得直接进入 K4。

## 2026-07-01 DH-GATEK-K3-CI-OBJECTMAPPER-FIX

### 范围
修复 K3 merge 后 GitHub Actions `mvn -B -ntp test` 在 `PostgresContainerSmokeTest.contextLoads` 的 Spring context 启动失败。本轮只处理 CI failure root cause 和回归测试，不推进 M1 readiness review，不实现 K4 Replay Read Model，不新增 API / Controller / replay API，不接真实 provider / NQ / HTTP / LangGraph / LIVE。

### RCA
CI run `28508807175` / job `84503767630` 失败于 `Build and test`。日志显示 `MappingJackson2HttpMessageConverter` 创建失败，根因是 `ObjectMapper` 注入歧义：

```text
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available:
expected single matching bean but found 2: nqFeedbackObjectMapper,decisionPersistenceObjectMapper
```

K3 `DecisionPipelineWiringConfig` 把 persistence 专用 `ObjectMapper` 注册成了 Spring bean，导致 WebMVC 默认 HTTP message converter 无法选择唯一 mapper。该问题只在真实 `dh-app` Spring context 启动时暴露；本地无 Docker 时 `PostgresContainerSmokeTest` 会跳过，所以需要新增非 Docker 装配回归测试。

### 修改文件
- `dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java`：移除 `decisionPersistenceObjectMapper` Spring bean，改为 repository 装配内部创建 K3 专用 mapper，避免污染全局 `ObjectMapper` bean 集合。
- `dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java`：新增 ApplicationContextRunner 回归测试，断言全局只有一个 `ObjectMapper`、`nqFeedbackObjectMapper` 保持存在、`decisionPersistenceObjectMapper` 不再作为 bean 暴露、`DecisionAuditRepository` 仍可装配。
- `docs/current/TESTING.md`、`docs/current/WORKLOG.md`：记录 CI RCA、修复与验证结果。

### 验证
- `Get-Location`：`F:\project\decision-hub`。
- `git branch --show-current`：`dev`。
- 初始 `git status --short`：clean。
- GitHub MCP 读取 CI job log：确认 run `28508807175` / job `84503767630` 失败于 `PostgresContainerSmokeTest.contextLoads`，核心异常为两个 `ObjectMapper` bean 导致 `NoUniqueBeanDefinitionException`。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -pl dh-app -am "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DecisionPipelineWiringConfigTest" test`：BUILD SUCCESS；新增测试 1/1 passed；reactor 15/15 SUCCESS。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml test`：BUILD SUCCESS；reactor 19/19 SUCCESS；本机无有效 Docker，`PostgresContainerSmokeTest` 按 `disabledWithoutDocker=true` skipped；CI 有 Docker 会实际覆盖该启动路径。
- `mvn -ntp -gs target/codex-maven-settings.xml -s target/codex-maven-settings.xml -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。

### 边界确认
未修改 NQ 仓库；未新增 API path；未新增 Controller；未新增 migration；未新增 replay API / query endpoint；未实现 K4 Replay Read Model；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

### 下一步
当前主线仍为 `DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE / IMPLEMENTED / READY FOR M1`；下一步仍是 `DH-STAGE4-DECISION-PIPELINE-MVP-M1-READINESS-REVIEW / NOT STARTED`，不得直接进入 K4。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL` single-batch implementation。K4 只实现内部 replay read model，不新增 API / Controller / migration / replay endpoint，不重跑 provider，不重跑 orchestrator，不接 NQ / HTTP / LangGraph / LIVE，也不进入 K5-K8。

### 修改文件
- 新增 `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionReplay*.java`：K4 replay request / context / trace step / provider call / output / audit event / timeline / aggregate view 与 `DecisionReplayStatus`。
- 新增 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionReplayQuery.java`、`DecisionReplayQueryRepository.java`、`DecisionReplayQueryService.java`、`DefaultDecisionReplayQueryService.java`。
- 新增 `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionReplayQueryRepository.java`：只读读取 K3 六张 DH-owned decision 表。
- 修改 `dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java`：补充 replay repository / service wiring，复用私有 persistence `ObjectMapper`，不新增全局 `ObjectMapper` bean。
- 新增 `DecisionReplayQueryServiceTest`、`JdbcDecisionReplayQueryRepositoryTest`；更新 `DecisionPipelineWiringConfigTest`。
- 更新 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`。

### Implementation
- `DecisionReplayView` 聚合 K3 已落库的 request、context snapshot、trace steps、provider call summaries、decision output 和 audit events。
- `DefaultDecisionReplayQueryService` 对输入非法、repository 异常、tenant mismatch、trace/request mismatch 统一返回结构化 fail-closed 结果，不向调用方抛裸 RuntimeException。
- `JdbcDecisionReplayQueryRepository` 所有 SQL 均带 `tenant_id = ? and decision_id = ?`；trace / provider / audit 记录按时间和 id 稳定排序。
- JSON 字段仅解析为安全 `Map` / `List`；JSON、枚举或时间字段不可解析返回 `CORRUPTED`；DB 读取失败返回 `BLOCKED`。
- K4 只读，不写库、不调用 provider、不调用 orchestrator、不修改 audit 数据、不访问 NQ DB、不保存或返回 credential / token / raw provider sensitive response。

### Tests
新增/更新 K4 tests 15 cases：

```text
DecisionReplayQueryServiceTest: 8
JdbcDecisionReplayQueryRepositoryTest: 6
DecisionPipelineWiringConfigTest: 1
```

覆盖项：

```text
existing decision + tenant -> FOUND
decisionId missing -> NOT_FOUND
tenant mismatch -> TENANT_MISMATCH / no data
critical row missing -> INCOMPLETE
corrupt JSON -> CORRUPTED
DB read failure -> BLOCKED
trace / provider / audit ordering
tenant-scoped SQL
no JDBC write
ObjectMapper bean remains unique
```

### 验证
- `mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test`（普通 sandbox）：BUILD SUCCESS；reactor 15/15 SUCCESS；`PostgresContainerSmokeTest` 因 Docker pipe 权限跳过 1。
- `docker info --format '{{.ServerVersion}}'`（提权）：成功，Docker Desktop 版本 `29.5.3`。
- `mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test`（提权）：BUILD FAILURE；失败于既有 `JdbcNonceReplayGuardPersistenceTest` 拉取 `postgres:17` 镜像，Docker registry / mirror EOF；K4 replay tests 在失败前已通过。
- `docker pull postgres:17`（提权）：FAILURE；`hub-mirror.c.163.com` 拉取 `postgres:17` 时 EOF。
- `docker pull public.ecr.aws/docker/library/postgres:17`（提权）：FAILURE；备用 public ECR 下载 layer 时 EOF。
- `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；`PostgresContainerSmokeTest` 因普通 sandbox 无 Docker pipe 权限跳过 1。
- `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。
- K4 边界关键词扫描：命中均为既有 Controller、配置、禁止说明、denylist、migration comment、负向安全词或 K4 边界注释；未发现本轮新增 API / Controller / replay endpoint / RealClient / real provider / HTTP client / NQ runtime / LangGraph / LIVE / BUY-SELL action 实现。

### 边界确认
未新增 API path；未新增 Controller；未新增 migration；未新增 replay API / query endpoint；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未实现 K5-K8；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

### 剩余风险
- Provider health / budget / latency 尚未实现。
- Mock NQ dry-run contract tests 尚未实现。
- Golden cases / eval 尚未实现。
- Integration-1 仍需基于 NQ GateN 重新规划。
- LangGraph 后置。
- Agent phase 后置。
- Docker-gated Testcontainers 真实 Postgres 用例当前被 `postgres:17` 镜像拉取 EOF 阻断；需要修复 Docker registry / mirror 后复跑。

### Readiness
- `ALLOW_K4_CLOSE: YES`
- `ALLOW_K5_IMPLEMENTATION: YES`
- `ALLOW_STAGE4_M2_CLOSE_REVIEW: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY / NOT STARTED`；不得直接进入 K6-K8、Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY

### 范围
完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY` single-batch implementation。K5 只强化 K2/K3/K4 既有 Decision Pipeline 的 mock-only provider health、budget、latency 和 fail-closed guard，不新增 API、Controller、migration、provider health/budget/latency 表、真实 provider、HTTP、NQ runtime、LLM、LangGraph 或 LIVE。

### 新增文件
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderHealth.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderHealthStatus.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderBudget.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderBudgetStatus.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderLatency.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderGuardResult.java`
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionProviderFailureClass.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderHealthEvaluator.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionProviderHealthEvaluator.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderBudgetGuard.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionProviderBudgetGuard.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderLatencyRecorder.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionProviderLatencyRecorder.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderGuard.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionProviderGuard.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderHealthEvaluatorTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderBudgetGuardTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderLatencyRecorderTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderGuardTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionOrchestratorProviderGuardTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/DecisionProviderGuardNoOutboundTest.java`

### 修改文件
- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/decision/DecisionOutput.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/MockDecisionSignalProvider.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/MockDecisionSignalProviderTest.java`
- `dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionReplayQueryRepositoryTest.java`
- `dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java`
- `dh-app/src/test/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfigTest.java`
- `docs/current/README.md`
- `docs/current/ROADMAP.md`
- `docs/current/STATUS.md`
- `docs/current/WORK_ORDER.md`
- `docs/current/TESTING.md`
- `docs/current/WORKLOG.md`

### Implementation
- 新增 K5 domain value objects/enums，表达 provider health、budget、latency、guard result 与 failure class；模型只保存本地观测与审计字段，不保存 raw provider response、credential、token、HTTP endpoint 或 NQ 数据。
- 新增 K5 usecase guard 组件：health evaluator、budget guard、latency recorder、provider guard。默认预算使用本地 deterministic unit；默认 timeout 为 1000ms；disabled、unhealthy、timeout、untrusted、budget exceeded、invalid signal 均 fail-closed。
- `DefaultDecisionOrchestrator` 在 mock provider 调用前执行 budget/enabled pre-guard；调用后记录 latency、评估 health，并把 guard summary 写入既有 `dh_decision_provider_call_log` 字段：`latency_ms`、`provider_status`、`error_code`、`signal_json`。
- provider guard 失败时跳过 risk review 和 directional bias，输出 K1 contract 内的 structured `ABSTAIN`；不向调用方抛业务 RuntimeException。
- `DecisionOutput` 增加带 `reasonCodes` 的 `abstainForProviderFailure(...)` overload，不修改 schema、enum 或 action vocabulary。
- `MockDecisionSignalProvider` 保持 deterministic mock-only；`SUCCESS` 继续被视为 forbidden/untrusted，不代表真实 provider 成功。
- `DecisionPipelineWiringConfig` 补充 K5 guard beans，并把 guard/latency recorder 注入 orchestrator；仍不暴露额外全局 `ObjectMapper` bean。
- `JdbcDecisionReplayQueryRepositoryTest` 增加回归，证明 K5 provider call summary 能通过 K4 replay read model 读取。

### Tests
新增/更新 K5 tests，覆盖：

```text
healthy mock provider -> NO_TRADE / OBSERVATION_ONLY
provider disabled -> ABSTAIN
provider unhealthy / failed / untrusted -> ABSTAIN
provider timeout signal -> ABSTAIN
latency timeout -> ABSTAIN
budget exceeded -> ABSTAIN
provider exception -> ABSTAIN 且不泄露 raw message
latency_ms / failureClass / error_code / healthStatus / budgetStatus 写入 provider call summary
K4 replay 可读取 K5 provider summary
K5 guard source 不出现 HTTP / LLM / NQ / exchange / mapping 注解依赖
MockDecisionSignalProvider 继续拒绝 SUCCESS
ObjectMapper bean 仍唯一
```

### 验证
- `mvn -ntp -pl dh-usecase,dh-infra,dh-app -am test`：BUILD SUCCESS；reactor 15/15 SUCCESS；`PostgresContainerSmokeTest` 实际启动 `postgres:17` 并通过。
- `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Surefire XML 汇总 414 tests / 0 failures / 0 errors / 0 skipped。
- `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- `idea-mcp get_file_problems` 对 4 个关键文件均约 300s timeout；已降级到 Maven 编译/测试/quality 与 scoped `rg`，可信度高。
- K5 边界关键词扫描：命中均为既有 Controller、禁止说明、denylist、负向测试断言、migration comment、K5 no-outbound 测试或本轮边界注释；未发现本轮新增 API / Controller / migration / RealClient / real provider / HTTP client / NQ runtime / LangGraph runtime / LIVE / BUY-SELL action 生产实现。
- `DecisionAction.java` 与 `DecisionOutput.java` 交易动作扫描无命中：未引入 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER`。
- `git diff --check`：通过；仅 Windows LF -> CRLF warning，无 whitespace error。

### Readiness
- `ALLOW_K5_CLOSE: YES`
- `ALLOW_K6_IMPLEMENTATION: YES`
- `ALLOW_STAGE4_M2_CLOSE_REVIEW: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 边界确认
未实现 K6-K8；未新增 API path；未新增 Controller；未新增 migration；未新增 provider health / budget / latency 表；未新增 replay API / query endpoint；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

### 剩余风险
- K6 Mock NQ dry-run contract tests 尚未实现。
- K7 Golden Cases / Eval 尚未实现。
- K8 Acceptance / Freeze 尚未实现。
- Integration-1 仍需基于 NQ GateN 重新规划。
- LangGraph 后置。
- Agent phase 后置。

### 下一步
进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS / NOT STARTED`；不得直接进入 K7-K8、M2 close review、Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

## 2026-07-01 DH-STAGE4-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS

### 范围

完成 `DH-STAGE4-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS` single-batch implementation。K6 只新增 mock NQ dry-run contract tests、test-support 和最小 fixture，不新增 API、Controller、migration、replay endpoint、真实 HTTP、NQ runtime、real provider、LLM、LangGraph 或 LIVE；不修改生产代码。

### 新增文件

- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/MockNqDecisionRequestFactory.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/MockNqDryRunFixtures.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/MockNqDryRunAssertionSupport.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/support/RecordingDecisionAuditReplayRepository.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/MockNqDecisionDryRunContractTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/MockNqDecisionNoLiveTradeContractTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/MockNqDecisionPersistenceReplayContractTest.java`
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/MockNqDecisionProviderGuardContractTest.java`
- `golden_cases/decision/mock_nq_valid_dryrun.json`
- `golden_cases/decision/mock_nq_provider_blocked.json`
- `golden_cases/decision/mock_nq_no_live_trade_guard.json`

### 修改文件

- `docs/current/README.md`
- `docs/current/ROADMAP.md`
- `docs/current/STATUS.md`
- `docs/current/WORK_ORDER.md`
- `docs/current/TESTING.md`
- `docs/current/WORKLOG.md`

### Implementation

- `MockNqDecisionRequestFactory` 生成 K1 `DecisionRequest`，source 固定为 `NQ_MOCK`，decisionType 固定为 `READ_ONLY_RECOMMENDATION`，只包含 tenantId / requestId / traceId / subject / schemaVersion / requestedAt / evidenceRefs，不包含 account、order、credential、side、price、quantity 或 execution intent。
- `MockNqDryRunFixtures` 读取 K6 fixture 文本；fixture 只作为 K6 contract test 输入，不进入 K7 eval framework。
- `MockNqDryRunAssertionSupport` 集中断言 mock NQ request 只读语义、structured output、allowed actions 和 mandatory forbiddenActions。
- `RecordingDecisionAuditReplayRepository` 是 test-only 内存 repository，同时实现 K3 audit write port 与 K4 replay read repository，用于证明 orchestrator 写入后能通过现有 replay read model 只读回放，并验证 tenant isolation。
- `MockNqDecisionDryRunContractTest` 覆盖 mock NQ valid request -> structured output、fixture 字段对齐与禁止字段。
- `MockNqDecisionProviderGuardContractTest` 覆盖 provider disabled / budget exceeded / timeout 的 ABSTAIN fail-closed。
- `MockNqDecisionPersistenceReplayContractTest` 覆盖 K3 request / trace / provider / output / audit 写入后 K4 replay 读回，以及跨 tenant 不返回明细。
- `MockNqDecisionNoLiveTradeContractTest` 覆盖 DecisionAction vocabulary、Decision Pipeline 生产源码无 outbound runtime token、fixture 无 execution intent / credential。

### Tests

新增 K6 tests 10 cases：

```text
MockNqDecisionDryRunContractTest: 2
MockNqDecisionNoLiveTradeContractTest: 3
MockNqDecisionPersistenceReplayContractTest: 2
MockNqDecisionProviderGuardContractTest: 3
```

覆盖项：

```text
mock NQ valid request -> structured DecisionOutput
DecisionOutput.decisionType = READ_ONLY_RECOMMENDATION
action only ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
forbiddenActions fixed mandatory five
request / fixture no credential / order / execution fields
provider disabled / budget exceeded / timeout -> ABSTAIN fail-closed
persistence write-through -> K4 replay read model found
replay tenant mismatch -> no details returned
Decision Pipeline production code no HTTP / LLM / LangGraph / Controller token
DecisionAction no BUY / SELL / PLACE_ORDER / CANCEL_ORDER
```

### 验证

- `mvn -ntp -pl dh-usecase -am "-Dtest=MockNqDecisionDryRunContractTest,MockNqDecisionProviderGuardContractTest,MockNqDecisionPersistenceReplayContractTest,MockNqDecisionNoLiveTradeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；K6 10 tests / 0 failures / 0 errors / 0 skipped。
- `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test`：BUILD SUCCESS；reactor 15/15 SUCCESS；`PostgresContainerSmokeTest` 实际启动 `postgres:17` 并通过。
- `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Surefire XML 汇总 424 tests / 0 failures / 0 errors / 0 skipped；`PostgresContainerSmokeTest` 未 skip。
- `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- `git diff --check`：通过；exit 0；无 whitespace error。
- K6 边界关键词扫描：命中均为既有 Controller / mapping、历史或禁止说明、denylist、负向安全断言、K6 no-live-trade 测试、fixture 文件名语义或 docs/current 边界说明；未发现本轮新增 API / Controller / migration / RealClient / real provider / HTTP client / NQ runtime / LangGraph runtime / LIVE / BUY-SELL action 生产实现。

### Readiness

- `ALLOW_K6_CLOSE: YES`
- `ALLOW_K7_IMPLEMENTATION: YES`
- `ALLOW_STAGE4_ACCEPTANCE_REVIEW: NO`
- `ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_ACCEPTANCE_REVIEW: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 边界确认

未实现 K7-K8；未新增 API path；未新增 Controller；未新增 migration；未新增 replay API / query endpoint；未真实 HTTP；未真实 NQ 调用；未真实 DH runtime integration；未真实交易所调用；未新增 RealClient / 真实 Provider；未接 OpenAI / Claude / Gemini / 本地模型；未接 LangGraph；未读取或输出 credential、token、cookie、API secret、passphrase；未启动 Integration-1 runtime；未把 DH 写成 integrated；未把 Runtime integration 写成 started；未把 AI / Agent runtime 写成 started；未开启 LIVE；未修改 NQ 仓库；未把 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 放进 output action。

### 剩余风险

- K7 Golden Cases / Eval 尚未实现。
- K8 Acceptance / Freeze 尚未执行。
- Integration-1 仍需基于 NQ GateN 重新规划。
- LangGraph 后置。
- Agent phase 后置。

### 下一步

进入 `DH-STAGE4-DECISION-PIPELINE-MVP-K7-GOLDEN-CASES-EVAL / NOT STARTED`；不得直接进入 K8、GateK acceptance / freeze、Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

---

## NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO

日期：2026-07-03

### 本轮目标

关闭 M1 work order，规划 DH dry-run contract entry 的 mock-only 入口形态、安全验证链、失败链路、source allowlist/error taxonomy 评审点和 M2 准入判断。本轮为 `WORK_ORDER_ONLY + DH_DRYRUN_ENTRY_PLANNING + CONTRACT_VALIDATION_CHAIN + SECURITY_CHAIN_DESIGN + TEST_SUPPORT_ONLY + NO_RUNTIME + NO_LIVE`。

### 完成内容

- 新增 `docs/current/DH_NQ_INTEGRATION1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO.md`。
- 同步 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`DH_NQ_INTEGRATION.md`、`API.md`、M0 work order 与 dry-run mock implementation placeholder。
- 明确 M1 推荐入口形态为 `Option C / test-support mock-only / no runtime endpoint`。
- 明确 `NQ_DRYRUN` source allowlist 仍未实现，必须在后续安全契约评审后才允许进入代码。
- 明确 `decisionId`、`confidence`、`traceSummary`、`replayRef`、`auditRef`、`X-NQ-DH-Schema-Version` 仍为 `DOC_ONLY_ALIAS`，不得写入当前 schema/contracts/OpenAPI/fixture。
- 规划 14 步 contract validation chain：payload size、canonical header、requestId/traceId/tenantId binding、source allowlist、RFC3339 UTC timestamp、nonce replay、HMAC value-based signature material、schema shape、forbidden fields、mock-only orchestrator boundary、provider guard、audit/trace/replay boundary、structured DecisionOutput、fail-closed response normalization。
- 在 NQ dry-run worktree 同步对应 M1 result consumed work order，并确认下一步只允许 `NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO`。

### 验证

- DH `git status --short` / `git diff --check` / forbidden-scope diff：通过；仅 docs/current 变更；未改代码、contracts、golden_cases。
- DH `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Docker/Testcontainers 不可用导致既有环境相关 4 skips。
- DH `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- NQ worktree `mvn -ntp -f backend/pom.xml test`：BUILD SUCCESS；reactor 23/23 SUCCESS；`nq-app` 86 tests 中 2 skips 为既有环境/guard 条件。
- NQ worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；Integration0 17 tests / 0 failures / 0 errors / 0 skipped。
- NQ dev worktree 仅做 read-only git guard；存在非本任务 mainline dirty 文件，但 `docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 dirty diff，未触发 `WORKSTREAM_MIXED_BLOCKED`。

### 边界

未改生产代码；未改测试代码；未新增或修改 Controller/API、migration、schema、contracts、golden_cases、fixture JSON、runtime、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE；未读取或输出 credential、token、cookie、API secret、passphrase；未触达 NQ DB、NQ mutation、订单、撤单、持仓、账务、Paper Run 或真实交易链路；未把 DH 写成 integrated；未把 Integration-1 runtime 写成 started。

### Readiness

- `ALLOW_M1_WO_CLOSE: YES`
- `ALLOW_I1_M2_NQ_DRYRUN_STUB_RECORDER_WO: YES`
- `ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`
- `ALLOW_SCHEMA_CHANGE: NO`
- `ALLOW_CONTRACTS_MODIFICATION: NO`
- `ALLOW_FIXTURE_IMPLEMENTATION: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION: NO`
- `ALLOW_API_CONTROLLER_CHANGE: NO`
- `ALLOW_REAL_HTTP: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

进入 `NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO / NOT STARTED / WORK_ORDER_ONLY`，执行目录限定为 `F:\worktrees\nexus-quant-i1-dryrun`；不得在 `F:\project\nexus-quant` mainline dev worktree 改 Integration-1 文档或代码。

---

## NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO

日期：2026-07-03

### 本轮目标

关闭 M2 work order，规划 NQ dry-run stub recorder 的 test-support mock-only 形态、请求 builder 字段边界、in-memory recorder 字段边界、no-side-effect 测试矩阵和 M3 准入判断。本轮为 `WORK_ORDER_ONLY + NQ_DRYRUN_STUB_RECORDER_PLANNING + NO_SIDE_EFFECT_TEST_DESIGN + WORKTREE_ONLY + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE`。

### 完成内容

- 新增 `docs/current/DH_NQ_INTEGRATION1_M2_NQ_DRYRUN_STUB_RECORDER_WO.md`。
- 同步 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`DH_NQ_INTEGRATION.md`、`API.md`、M0/M1 work order 与 dry-run mock implementation placeholder。
- 明确 M2 推荐 stub shape 为 `test-support mock-only stub + in-memory recorder plan, no runtime HTTP client`。
- 明确 NQ request builder 只允许 read-only、脱敏、无执行意图字段；禁止 `accountId`、订单、仓位、凭证、BUY/SELL、数量、价格、杠杆、下单/撤单、风控/账务 mutation、Paper/LIVE run start 与 raw prompt/provider response。
- 明确 recorder 只能记录 request/trace/tenant/action whitelist/risk/policy/provider/fail-closed/audit summary，不得调用 execution、risk、ledger、paper/live 或外部 provider。
- 明确 `decisionId`、`confidence`、`traceSummary`、`replayRef`、`auditRef`、`X-NQ-DH-Schema-Version` 仍为 `DOC_ONLY_ALIAS`。
- 在 NQ dry-run worktree 同步对应 M2 work order，并确认下一步只允许 `NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / WORK_ORDER_ONLY`。

### 验证

- DH `git status --short` / `git diff --check` / forbidden-scope diff：通过；仅 docs/current 变更；未改代码、contracts、golden_cases。
- DH `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；Docker/Testcontainers 不可用导致既有环境相关 4 skips。
- DH `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- NQ worktree `mvn -ntp -f backend/pom.xml test`：BUILD SUCCESS；reactor 23/23 SUCCESS；`nq-infra` 1 skip、`nq-app` 2 skips 为既有环境/guard 条件。
- NQ worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；Integration0 17 tests / 0 failures / 0 errors / 0 skipped。
- NQ dev worktree 仅做 read-only git guard；终检存在非本任务 GateO/current dirty diff，但 `docs/current/*NQ_DH*` 与 `docs/current/*INTEGRATION1*` 无 dirty diff，未触发 `WORKSTREAM_MIXED_BLOCKED`。

### 边界

未改生产代码；未改测试代码；未新增或修改 Controller/API、migration、schema、contracts、golden_cases、fixture JSON、runtime、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE；未读取或输出 credential、token、cookie、API secret、passphrase；未触达 NQ DB、NQ mutation、订单、撤单、持仓、账务、Paper Run 或真实交易链路；未把 DH 写成 integrated；未把 Integration-1 runtime 写成 started。

### Readiness

- `ALLOW_M2_WO_CLOSE: YES`
- `ALLOW_I1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO: YES`
- `ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`
- `ALLOW_SCHEMA_CHANGE: NO`
- `ALLOW_CONTRACTS_MODIFICATION: NO`
- `ALLOW_FIXTURE_IMPLEMENTATION: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION: NO`
- `ALLOW_API_CONTROLLER_CHANGE: NO`
- `ALLOW_REAL_HTTP: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

进入 `NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / NOT STARTED / WORK_ORDER_ONLY`；不得提前实现 fixture、contracts、golden_cases、contract tests、runtime、API、真实 HTTP、real provider、AI/LangGraph 或 LIVE。

---

## NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION

日期：2026-07-03

### 本轮目标

落地受控 contract gap test-support guard，覆盖 DH 当前 schema / enum / runtime endpoint 边界、NQ dry-run worktree future request builder / recorder no-side-effect 边界，并保持 mock-only、test-support-only、no runtime、no live。

### 完成内容

- 新增 `dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java`。
- 在 `F:\worktrees\nexus-quant-i1-dryrun` 新增 `backend/nq-app/src/test/java/com/guidinglight/nexusquant/app/integration1/NqDhIntegration1ContractGapGuardTest.java`。
- DH guard 验证 `NQ_DRYRUN`、dry-run endpoint token、DOC_ONLY_ALIAS、canonical error names、BUY/SELL/订单/账户/凭证/交易字段不得提前进入当前 production contract 或 runtime source。
- NQ guard 验证 future `NQ_DRYRUN` 仍为 review-gated test-support source，request builder 拒绝账户/订单/凭证/数量/价格/杠杆/真实 URL/HTTP client，recorder 只保存 readonly summary，LONG/SHORT bias 不映射为 BUY/SELL，且不触发 order/risk/ledger/Paper/LIVE/HTTP side effect。
- 同步 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`DH_NQ_INTEGRATION.md`、`TESTING.md`、`WORKLOG.md`。

### 验证

- DH narrow `mvn -ntp -pl dh-domain -am "-Dtest=DecisionContractGapGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；6 tests / 0 failures / 0 errors。
- NQ narrow `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=NqDhIntegration1ContractGapGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；5 tests / 0 failures / 0 errors。
- DH `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；`PostgresContainerSmokeTest` 因本机 Docker/Testcontainers 不可用跳过 1 项。
- DH `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- NQ worktree `mvn -ntp -f backend/pom.xml test`：BUILD SUCCESS；reactor 23/23 SUCCESS。
- NQ worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；Integration0 17 tests / 0 failures / 0 errors / 0 skipped。

### 边界

未改 DH / NQ production code；未改 schema、contracts、golden_cases、fixture JSON、OpenAPI、Controller、migration、runtime 配置、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE；未读取或输出 credential、token、cookie、API secret、passphrase；未触达 NQ DB、NQ mutation、订单、撤单、持仓、账务、Paper Run 或真实交易链路；未把 DH 写成 integrated；未把 Integration-1 runtime 写成 started。

### Readiness

- `ALLOW_IMP0_CLOSE: YES`
- `ALLOW_I1_IMP1_DH_DRYRUN_TEST_SUPPORT_ENTRY: YES`
- `ALLOW_I1_RUNTIME: NO`
- `ALLOW_REAL_HTTP: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_SCHEMA_CHANGE: NO`
- `ALLOW_CONTRACTS_MODIFICATION: NO`
- `ALLOW_FIXTURE_IMPLEMENTATION: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION: NO`
- `ALLOW_API_CONTROLLER_CHANGE: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

进入 `NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY / NOT STARTED / TEST_SUPPORT_ONLY / MOCK_ONLY`；不得提前实现 runtime endpoint、真实 HTTP、real provider、schema/contracts/golden_cases/fixture JSON、OpenAPI、Controller、migration、AI/LangGraph 或 LIVE。

---

## NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY

日期：2026-07-03

### 本轮目标

落地 DH 侧 dry-run test-support entry / validation harness，用于验证未来 dry-run entry 的合同验证链路、fail-closed 行为、forbidden field 拦截、DecisionOrchestrator mock-only 边界、provider guard 边界和 audit / trace / replay safe-summary 边界。

### 完成内容

- 新增 `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/support/DhDryRunTestSupportEntry.java`。
- 新增 `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhDryRunTestSupportEntryTest.java`。
- validation chain 覆盖 payload size gate、canonical header、requestId / traceId / tenantId binding、source allowlist、UTC `Z` timestamp、nonce replay、value-based HMAC、schema/contract shape、forbidden fields、DecisionOrchestrator mock-only、provider guard、safe summary、structured `DecisionOutput` assembly 和 fail-closed normalization。
- 同步 DH `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`DH_NQ_INTEGRATION.md`、`API.md`、`TESTING.md`、`WORKLOG.md`。
- 在 `E:\Project\nexus-quant-i1-dryrun` 仅同步 NQ worktree `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`。

### 验证

- DH narrow `mvn -ntp -pl dh-usecase -am -Dtest=DhDryRunTestSupportEntryTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；12 tests / 0 failures / 0 errors。
- DH `mvn -ntp test`：BUILD SUCCESS；reactor 19/19 SUCCESS；`PostgresContainerSmokeTest` 因当前 Docker/Testcontainers 环境不可用跳过 1 项。
- DH `mvn -ntp -Pquality validate`：BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。
- NQ worktree `mvn -ntp -f backend/pom.xml test`：BUILD SUCCESS；reactor 23/23 SUCCESS；`nq-app` 2 skipped 为既有环境/guard 条件。
- NQ worktree `mvn -ntp -f backend/pom.xml -pl nq-app -am "-Dtest=*Integration0*" "-Dsurefire.failIfNoSpecifiedTests=false" test`：BUILD SUCCESS；Integration0 17 tests / 0 failures / 0 errors / 0 skipped。

### 边界

未改 DH / NQ production code；未改 schema、contracts、golden_cases、fixture JSON、OpenAPI、Controller、migration、runtime 配置、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE；未读取或输出 credential、token、cookie、API secret、passphrase；未触达 NQ DB、NQ mutation、订单、撤单、持仓、账务、Paper Run 或真实交易链路；未把 DH 写成 integrated；未把 Integration-1 runtime 写成 started。

### Readiness

- `ALLOW_IMP1_CLOSE: YES`
- `ALLOW_I1_IMP2_NQ_STUB_RECORDER_NO_SIDE_EFFECT: YES`
- `ALLOW_PRODUCTION_CODE_CHANGE: NO`
- `ALLOW_SCHEMA_CHANGE: NO`
- `ALLOW_CONTRACTS_MODIFICATION: NO`
- `ALLOW_FIXTURE_IMPLEMENTATION: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION: NO`
- `ALLOW_API_CONTROLLER_CHANGE: NO`
- `ALLOW_REAL_HTTP: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_INTEGRATION_1_RUNTIME: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

进入 `NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT / NOT STARTED / NQ_WORKTREE_ONLY / MOCK_ONLY`；不得提前实现 runtime endpoint、真实 HTTP、real provider、schema/contracts/golden_cases/fixture JSON、OpenAPI、Controller、migration、AI/LangGraph 或 LIVE。

---

## NQ-DH-I1-NQ-RUNTIME-CLIENT-WO

日期：2026-07-04

### 本轮目标

在 NQ integration worktree 编写 limited dry-run runtime client implementation work order，并在 DH dev 仅做 `docs/current` 最小状态同步。本轮为 `WORK_ORDER_ONLY + NQ_SCOPED_RUNTIME_CLIENT_IMPLEMENTATION_PLAN + CROSS_REPO_BOUNDARY_FREEZE + NO_CLIENT_IMPLEMENTATION + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE`。

### 完成内容

- NQ worktree 新增 `docs/current/NQ_DH_INTEGRATION1_NQ_RUNTIME_CLIENT_WO.md`。
- NQ worktree 同步 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`API.md`、`TESTING.md`、`WORKLOG.md`。
- DH dev 最小同步 `docs/current/DH_NQ_INTEGRATION.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md`、`WORKLOG.md`。
- 冻结下一轮 NQ limited client 的 default disabled、dev/test only、production disabled、kill switch fail-closed、request/response envelope、canonical `X-NQ-DH-*` header、HMAC value material、timeout/retry/idempotency、response validation、audit/logging redaction、error taxonomy、测试矩阵、no-side-effect integration point 和回滚要求。
- 明确 DH endpoint `POST /api/ai/decision-dry-runs` 仍是 DH-only inbound limited dry-run；NQ client implementation 未启动，runtime integration 未启动。

### 验证

- DH `git status --short`：PASS；仅 `docs/current` 同步文件有 diff。
- DH `git diff --check`：PASS；仅 LF/CRLF warning，无 whitespace error。
- DH forbidden-scope diff：PASS / EMPTY；未改 DH Java main、contracts 或 golden_cases。
- DH `mvn -ntp -Pquality validate`：BUILD SUCCESS；19/19 reactor SUCCESS；0 Checkstyle violations；Spotless check passed。
- NQ worktree branch：PASS；`nq-dh-i1-nq-runtime-client-wo`。
- NQ worktree forbidden-scope diff：PASS / EMPTY；未改 backend / frontend / research / scripts / deploy / `.github` / migration。
- NQ boundary `rg`：PASS / REVIEWED；命中为既有 docs/backend 业务词、历史/禁止语境或本轮边界说明。
- NQ dev read-only guard：PASS / SCOPED EMPTY；`dev` 分支最终只读 status 显示既有 `research/py` dirty/untracked 变更，但 NQ-DH scoped diff 为空；本轮未修改 NQ dev。
- 未运行 NQ Maven full / targeted Integration0 / targeted Integration1；原因是本轮 docs-only / work-order-only，不声明 NQ Maven PASS。

### 边界

未实现 NQ runtime client；未新增 HTTP client；未真实调用 DH；未真实 outbound HTTP；未接 real provider；未读取或输出 credential、token、cookie、apiKey、apiSecret、passphrase；未修改 NQ dev；未改 DH Java；未改 contracts / OpenAPI / JSON Schema / golden_cases / fixture JSON / migration；未触碰 order / execution / risk / ledger / account / paper / live；未把 `LONG_BIAS / SHORT_BIAS` 映射为 `BUY / SELL`；未接 Agent / LangGraph；未开启 LIVE。

### Readiness

- `ALLOW_NQ_RUNTIME_CLIENT_WO_CLOSE: YES`
- `ALLOW_NQ_LIMITED_RUNTIME_CLIENT_IMPLEMENTATION_WO: YES`
- `ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO`
- `ALLOW_REAL_HTTP_NOW: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_SCHEMA_FORMALIZATION_NOW: NO`
- `ALLOW_CONTRACTS_MODIFICATION_NOW: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO`
- `ALLOW_DH_CODE_CHANGE_NOW: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

`NQ-DH-I1-NQ-LIMITED-RUNTIME-CLIENT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / DEFAULT_DISABLED / DEV_TEST_ONLY / NO_LIVE`。该下一步必须另起实现任务；不得把本 WO close 解释为 runtime integration started、DH integrated、real HTTP allowed、real provider allowed 或 LIVE enabled。

---

## NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-WO

日期：2026-07-05

### 本轮目标

冻结下一轮 joint runtime dry-run test implementation 的测试边界、测试方式、允许文件范围、禁止项、测试矩阵、验收标准和回滚要求。本轮为 `WORK_ORDER_ONLY + JOINT_RUNTIME_DRYRUN_TEST_PLAN + CROSS_REPO_TEST_BOUNDARY_FREEZE + NO_TEST_IMPLEMENTATION + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE`。

### 完成内容

- 新增 `docs/current/DH_NQ_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md`。
- NQ worktree 新增 `docs/current/NQ_DH_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md`。
- DH dev 同步 `docs/current/DH_NQ_INTEGRATION.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`TESTING.md` 与本文件。
- NQ worktree 同步 `docs/current/README.md`、`STATUS.md`、`ROADMAP.md`、`WORK_ORDER.md`、`API.md`、`TESTING.md` 与 `WORKLOG.md`。
- 冻结唯一允许的下一轮验证链路：`NQ limited dry-run client -> fake / in-memory / MockMvc / test-only transport -> DH POST /api/ai/decision-dry-runs -> DH readonly decision envelope -> NQ response validation -> NQ dry-run record-only result`。
- 固定下一轮只能使用 fake transport、in-memory adapter、MockMvc、test-only request/response vector 或 isolated test support module；禁止 real outbound HTTP、真实 DH 服务、localhost 真实运行服务和外网。
- 固定下一轮成功矩阵、DH/NQ fail-closed 矩阵、no-side-effect 矩阵、audit/trace/record 验证、error taxonomy 对齐、验收门槛和回滚要求。
- 明确 `LONG_BIAS / SHORT_BIAS` 只能作为 bias-only，不得映射为 `BUY / SELL`；NQ 只能 record-only，不执行。

### 验证

- DH `git status --short`：PASS / DOCS-ONLY CHANGES PRESENT；dirty 限于允许的 `docs/current` 文档。
- DH `git diff --check`：PASS；仅 LF/CRLF warning，无 whitespace error。
- DH forbidden-scope diff：PASS / EMPTY；未改 contracts、golden_cases 或 migration。
- DH boundary `rg`：PASS / REVIEWED；命中为既有文档禁令、test guard、安全 denylist、endpoint token 或本轮 WO 边界说明。
- NQ worktree branch：PASS；`nq-dh-i1-joint-runtime-dryrun-test-wo`。
- NQ worktree forbidden-scope diff：PASS / EMPTY；未改 backend migration、frontend、research、scripts、deploy、`.github`、contracts 或 golden_cases。
- NQ worktree boundary `rg`：PASS / REVIEWED；命中为既有 docs/backend 业务词、历史/禁止语境或本轮 WO 边界说明。
- NQ dev read-only guard：PASS / SCOPED EMPTY；NQ-DH / Integration-1 scoped unstaged 与 staged diff 均为空，本轮未修改 NQ dev。
- 未运行 DH / NQ Maven full、targeted tests 或 quality profile；原因是本轮 docs-only / work-order-only，未修改 Java、测试、contracts、fixture、golden_cases 或 migration，不声明 Maven PASS。

### 边界

未实现测试；未修改 DH 或 NQ Java 生产代码；未修改测试代码；未新增测试 fixture；未改 DH endpoint；未改 NQ client；未修改 NQ dev；未改 contracts / OpenAPI / JSON Schema / golden_cases / migration；未真实调用 DH；未真实 HTTP；未接 real provider；未读取或输出 credential、token、cookie、apiKey、apiSecret、passphrase；未触碰 order / execution / risk / ledger / account / paper / live；未把 Runtime integration 写成 started；未把 DH 写成 integrated；未接 Agent / LangGraph；未开启 LIVE。

### Readiness

- `ALLOW_JOINT_RUNTIME_DRYRUN_TEST_WO_CLOSE: YES`
- `ALLOW_JOINT_RUNTIME_DRYRUN_TEST_IMPLEMENTATION: NO`
- `ALLOW_REAL_DH_CALL_NOW: NO`
- `ALLOW_REAL_HTTP_NOW: NO`
- `ALLOW_REAL_PROVIDER: NO`
- `ALLOW_SCHEMA_FORMALIZATION_NOW: NO`
- `ALLOW_CONTRACTS_MODIFICATION_NOW: NO`
- `ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO`
- `ALLOW_DH_CODE_CHANGE_NOW: NO`
- `ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: NO`
- `ALLOW_AGENT_PHASE: NO`
- `ALLOW_LANGGRAPH_RUNTIME: NO`
- `ALLOW_LIVE: NO`

### 推荐下一步

进入 `NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-IMPLEMENTATION`；该任务必须由用户单独授权，且只能做 test-only / fake-transport / no-real-http 联合验证，不得真实联调。
