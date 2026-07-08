# NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW

> Status: CLOSED / ACCEPTED / REVIEW_ONLY / DH_ONLY / NO_CODE_CHANGE / NO_NQ_CHANGE / NO_LIVE
> Date: 2026-07-04
> Endpoint: POST /api/ai/decision-dry-runs
> Scope: DH limited dry-run inbound endpoint close review only

## 1. Task classification

```text
REVIEW_ONLY + DH_RUNTIME_ENDPOINT_SECURITY_REVIEW + API_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_NQ_CHANGE + NO_LIVE
```

本轮只审查 DH 侧 limited dry-run inbound endpoint 是否可以关闭；不继续实现功能，不新增或修改 Java 生产代码、测试代码、API、Controller、Client、Service、Repository、migration、contracts、OpenAPI、JSON Schema、golden_cases 或 fixture JSON；不修改 NQ dev 或 NQ dry-run worktree。

## 2. Files inspected

DH：

```text
README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md
docs/current/API.md
docs/current/TESTING.md
docs/current/WORKLOG.md
contracts/openapi.yaml
contracts/json-schema/**
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunController.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunRequest.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunSuccessResponse.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunErrorResponse.java
dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthResult.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderParser.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderValidator.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NormalizedNqDhHeaders.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDhHeaderNames.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/InMemoryRateLimiter.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NonceReplayGuard.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NonceReplayGuardType.java
dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/InMemoryNonceReplayGuard.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNonceReplayGuard.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DefaultDecisionOrchestrator.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/MockDecisionSignalProvider.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/DecisionPersistenceRecords.java
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/InMemoryDecisionAuditRepository.java
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/decision/JdbcDecisionAuditRepository.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/SecurityWiringConfig.java
dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java
dh-app/src/main/resources/application.yml
dh-app/src/main/resources/application-dev.yml
dh-app/src/main/resources/application-test.yml
dh-app/src/main/resources/application-prod.yml
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunServiceTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhDryRunTestSupportEntryTest.java
```

NQ dev read-only：

```text
E:/Project/nexus-quant
git status --short
git diff --stat
git diff --name-only -- docs/current/*NQ_DH* docs/current/*INTEGRATION1*
git diff --name-only --cached -- docs/current/*NQ_DH* docs/current/*INTEGRATION1*
```

NQ dry-run worktree read-only：

```text
E:/Project/nexus-quant-i1-dryrun
git status --short
git branch --show-current
git diff --stat
```

## 3. API boundary

Result: PASS / CLOSED.

- `POST /api/ai/decision-dry-runs` 是 DH-only inbound endpoint；Controller 只接收 signed / timestamped / nonce / tenant-bound request 并返回 read-only snapshot。
- 未发现 outbound HTTP client、NQ runtime client、RealClient、real provider、Agent / LangGraph runtime、NQ DB 访问或 LIVE 入口。
- `DecisionDryRunSuccessResponse` 与 `DecisionDryRunSnapshot` 只返回 `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；`LONG_BIAS / SHORT_BIAS` 仅为 readonly bias，不是 `BUY / SELL`。
- 内部 `ABSTAIN` 在 `DefaultDecisionDryRunService.toSnapshot` 映射为 `NO_TRADE`，并追加 `INTERNAL_ABSTAIN_MAPPED` reason。
- request 递归扫描 `credential / apiKey / apiSecret / passphrase / executableOrder / quantity / leverage / price / BUY / SELL / PLACE_ORDER / CANCEL_ORDER` 等执行或凭证材料，命中即 `POLICY_DENIED` fail-closed。

## 4. Security gate

Result: PASS / ENFORCED.

| Gate | Review result |
| --- | --- |
| feature flag 默认关闭 | `application.yml` / dev / test 均默认 `enabled: false`。 |
| production disabled | `application-prod.yml` 设置 `enabled: false`、`production-enabled: false`、`kill-switch-enabled: true`、空 allowlist。 |
| kill switch fail-closed | `DecisionDryRunRuntimeProperties.runtimeEnabled()` 中 kill switch 直接拒绝。 |
| HMAC signature verification | `HmacNqDryRunAuthenticator` 校验 method/path/source/tenant/requestId/traceId/timestamp/nonce/schemaVersion/body hash。 |
| RFC3339 / ISO-8601 UTC Z timestamp | `parseTimestamp` 要求 `endsWith("Z") + Instant.parse`。 |
| epoch seconds / epoch milliseconds 拒绝 | `HmacNqDryRunAuthenticatorTest` 与 Controller test 覆盖 numeric epoch。 |
| non-UTC-Z timestamp 拒绝 | `+08:00` 被映射为 `TIMESTAMP_INVALID`。 |
| ±300s timestamp window | 默认 `max-clock-skew-seconds: 300`，超窗返回 `TIMESTAMP_OUT_OF_WINDOW`。 |
| persistent nonce replay guard | 非 dev/test 默认 JDBC；dev/test in-memory 仅测试/单实例且有 TTL + cap。 |
| tenant/source allowlist | source allowlist + tenant/source pair allowlist 双 gate。 |
| `NQ_DRYRUN` 仅 dev/test profile | dev/test 可配置；production profile 清空 allowlist。 |
| `NQ_DRYRUN` 不进 production allowlist | `DecisionContractGapGuardTest` 与 prod config 均覆盖。 |
| requestId / traceId / tenantId binding | header validator 校验可选 canonical header 与认证 tenant/body requestId/body traceId 一致；header 不覆盖权威来源。 |
| payload size cap | Controller 在 JSON 解析前调用 `isPayloadTooLarge`。 |
| rate limit | `RateLimiter.check(source, tenant, route)` 在 HMAC 之前执行，超限 `RATE_LIMITED`。 |
| memory cap | usecase 对 `decisionContext.approxBytes` 执行 cap，超限 `MEMORY_LIMIT_EXCEEDED`。 |
| forbidden capability / forbidden field gate | request recursive scan + mandatory forbidden capability set 检查。 |
| all security failures fail-closed | Controller/security/usecase 均返回 error envelope，不 fallback 成功。 |

## 5. Error taxonomy

Result: PASS / STABLE ENDPOINT-LOCAL TAXONOMY.

`DecisionDryRunErrorCode` 覆盖并稳定映射：

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

- `UNKNOWN_ERROR` 与 audit 写失败均 fail-closed。
- security error 不 fallback 成功。
- provider disabled / timeout / budget exceeded 不转成功，分别映射为 `PROVIDER_DISABLED / PROVIDER_TIMEOUT / BUDGET_EXCEEDED`。
- policy denied 不输出交易建议。
- error envelope 只返回 `error / errorCode / message / requestId / traceId / auditRef`，不回显 secret、signature、raw body、raw prompt、provider raw response、token、cookie 或 credential。

## 6. Audit / trace / replay

Result: PASS / REDACTED / FAIL-CLOSED.

- success request 写入 DH-owned audit/trace/output 记录，并返回 `auditRef`、`replayRef`、`traceSummary`。
- rejected request 经 `DecisionDryRunService.reject` 写入 redacted audit event；audit 写失败返回 `UNKNOWN_ERROR` 且不返回成功 decision。
- `DecisionPersistenceRecords` 与 JDBC adapter 只写 DH-owned tables，不读写 NQ DB。
- `DefaultDecisionOrchestrator` 持久化 subject/context/output/provider summary 时只保存 snapshot id、evidence count、mock provider summary、reason code 与状态，不保存 raw credential、raw prompt、provider raw response 或 raw payload。
- `NormalizedNqDhHeaders.toString()` redacts signature，trace failure 只记录异常类名。

## 7. No-side-effect boundary

Result: PASS / NO OUTBOUND SIDE EFFECT.

- 未触碰 NQ order / execution / ledger / account / trading / live。
- 未新增 real HTTP outbound、RealClient、provider config、NQ runtime client 或 LangGraph/Agent runtime。
- 未修改 migration。
- 未修改 `contracts/openapi.yaml`、`contracts/json-schema/**`、`golden_cases/**` 或 fixture JSON。
- NQ dev 只读检查发现已有非本轮 dirty，但 NQ-DH / Integration-1 scoped unstaged 与 staged diff 为空；NQ dry-run worktree clean。

## 8. Test review

Result: PASS / COVERED FOR CLOSE REVIEW.

覆盖项：

```text
successful valid dry-run
feature flag disabled
invalid / missing signature
invalid timestamp
timestamp out of window
replay nonce
source denied
tenant mismatch
dryRun=false
forbidden BUY / SELL / executableOrder
payload too large
rate limit
memory limit
audit failure fail-closed
provider disabled / timeout / budget exceeded fail-closed
ABSTAIN -> NO_TRADE with reason
contracts/schema/golden_cases guard
no outbound / no provider / no LIVE guard
```

Known validation note:

- `mvn -ntp test` 的 `PostgresContainerSmokeTest` 因 Docker named pipe 权限不可用由测试自身 skip；该项不是 Docker/Testcontainers PASS。
- 本 close review 判断该 skip 不阻断 endpoint close：endpoint 的安全 gate、fail-closed、no-outbound、audit、source allowlist 与 Maven module/full test 均由非 Testcontainers 路径覆盖；但它仍不证明本地 Docker/Testcontainers 可用。

## 9. Validation

Result: PASS / REVIEWED.

```text
git status --short: docs/current-only dirty; new close-review doc
git diff --check: PASS, LF/CRLF warning only
git diff --stat: REVIEWED
forbidden diff for contracts / golden_cases / migration: EMPTY
boundary rg: REVIEWED, no new real outbound / NQ client / provider / LIVE implementation
mvn -ntp -pl dh-api -am test: PASS / BUILD SUCCESS
mvn -ntp -pl dh-usecase -am test: PASS / BUILD SUCCESS
mvn -ntp -Pquality validate: PASS / BUILD SUCCESS
mvn -ntp test: PASS / BUILD SUCCESS, PostgresContainerSmokeTest SKIP due Docker named pipe access denied
NQ dev scoped NQ-DH / Integration-1 diff: EMPTY
NQ worktree status: CLEAN
```

Docker/Testcontainers status: SKIP, not PASS. The skip is caused by local Docker named pipe permission and does not prove Docker/Testcontainers availability.

## 10. Readiness decision

```text
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

`ALLOW_NQ_RUNTIME_CLIENT_WO: YES` 只表示允许进入下一轮 NQ runtime client work order；它不授权 NQ client implementation、真实 HTTP、real provider、contracts/schema/golden_cases 修改、Agent / LangGraph runtime 或 LIVE。

## 11. Risks

未发现阻断 DH limited runtime endpoint close 的问题。进入 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO` 前必须继续固定：

- WO 只能是 work-order-only，不得夹带 implementation。
- NQ client 设计必须先冻结 no-side-effect boundary、timeout、fail-closed、redacted audit、source/tenant binding、payload cap、rate limit、rollback 和 disable switch。
- `NQ_DRYRUN` 不得进入 production allowlist。
- OpenAPI / JSON Schema / golden_cases / fixture JSON formalization 必须另起独立 review。
- Runtime integration 仍不得写成 started；DH 仍不得写成 integrated。

## 12. Next concrete action

```text
NQ-DH-I1-NQ-RUNTIME-CLIENT-WO / NOT STARTED / WORK_ORDER_ONLY / NO_IMPLEMENTATION / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```
