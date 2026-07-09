# DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-WO

> status: DONE / WORK_ORDER_ONLY
> scope: B2 implementation boundary design
> current factsource: docs/current

## 1. Task Classification

```text
WORK_ORDER_ONLY
B2_IMPLEMENTATION_BOUNDARY_DESIGN
PROVIDER_HEALTH_READ_MODEL_WO
MODEL_GATEWAY_OBSERVABILITY_READ_MODEL
SECURITY_BOUNDARY_DESIGN
TEST_MATRIX_DESIGN
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

本轮只编制 Stage-QDR-5 B2 Provider Health / Gateway Call Read Model implementation work order。不得实现 Java、测试、migration、API、Controller、Repository/JDBC/Service，不得接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE。

## 2. Preconditions

```text
STAGE_QDR_5_B1: DONE
STAGE_QDR_5_B1_COMMIT: feat(qdr): add model gateway observability contracts
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / BEFORE_WO
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮预检确认当前分支为 `dev`，worktree clean，最近提交包含 `7aed5e8 feat(qdr): add model gateway observability contracts`。

## 3. Read Model Target

后续 B2 implementation 目标：

```text
Provider Health / Gateway Call Read Model
```

边界固定为：

```text
internal read model only
no API / Controller by default
no migration by default
no real provider
no real HTTP
no Provider SDK
no credential access
no trading signal
```

B2 read model 只把 B1 `ModelGatewayObservabilitySummary`、`ProviderHealthSummary`、`ProviderFailureClassification`、`ProviderLatencyBudgetSummary`、`ProviderTrustDecisionSummary`、`ProviderReadinessSignal` 与既有 `ModelGatewayCallRecord` / QDR gateway result / mock provider evidence 投影为内部只读 view。它不改变 gateway call 写入链路，不启动 provider runtime，不扩大 execution 或 trading 边界。

## 4. Allowed Structures

后续 B2 implementation 可新增或补齐以下结构，命名可按现有 `qdr.gateway` / `qdr.readmodel` / `qdr.replay` 风格微调：

```text
ProviderHealthReadModelQuery
ProviderHealthReadModelView
ModelGatewayCallObservabilityView
ProviderFailureClassificationView
ProviderLatencyBudgetView
ProviderTrustDecisionView
ProviderReadinessSignalView
ProviderHealthReadModelService
```

如实现中需要更少对象，可以合并 view 类型，但必须保留以下语义边界：

```text
query boundary
provider health summary
gateway call observability summary
failure classification
latency budget
trust decision
readiness signal / finding
safe refs / redacted summary
```

`ProviderHealthReadModelService` 应优先按现有 `RegressionReadModelService` 模式实现为 usecase 内部 service：组合已有 tenant-bound persistence port / in-memory fixture，输出安全 view；不引入 Spring Web，不新增 Controller，不新增 production JDBC adapter。

## 5. Query Boundary

后续 B2 implementation 查询必须 tenant-bound，支持的 selector 只能是：

```text
tenantId + providerRef
tenantId + modelGatewayVersionRef
tenantId + traceId
tenantId + sourceRequestId
tenantId + failureClassification
tenantId + trustDecision
tenantId + readinessStatus
tenantId + createdAt / lastObservedAt
```

必须禁止：

```text
UUID-only query
tenantless list
cross-tenant read
unbounded list
pageSize > 100 without reject/cap
```

建议 query contract：

```text
tenantId: required
providerRef: optional safe ref
modelGatewayVersionRef: optional safe ref
traceId: optional safe ref
sourceRequestId: optional safe ref
failureClassification: optional enum
trustDecision: optional enum
readinessStatus: optional enum
createdFrom / createdTo: optional time range
lastObservedFrom / lastObservedTo: optional time range
limit: 1..100
offset: >= 0
```

缺 `tenantId` 必须在 query 构造或 service 入口 fail-closed。跨租户读取必须返回 empty 或 fail-closed，不得泄露存在性、数量、trace 或 source request 细节。

## 6. Report / View Content Boundary

允许展示：

```text
tenantId
providerRef
modelGatewayVersionRef
traceId
sourceRequestId
providerSummaryHash
failureClassification
latencyBudgetSummary
trustDecisionSummary
readinessSignal
readinessFinding
lastObservedAt
createdAt
updatedAt
safe refs
redacted summary
```

禁止展示：

```text
raw prompt
raw provider response
credential
token
cookie
apiKey
apiSecret
passphrase
secret
request payload
response payload
order instruction
execution instruction
account secret
position mutation
ledger mutation
risk mutation
```

`ProviderReadinessStatus.READY` 只表示内部 readiness evidence 满足后续评估条件，不表示 provider authorization、live permission、real HTTP enabled、trading permission 或 execution approval。`ProviderTrustDecisionSummary.Decision.ALLOWED` 只表示 mock / policy context 下通过检查，不代表真实 provider 已授权。

## 7. Persistence / Repository Boundary

B2 默认不新增 migration，默认不新增 production Repository / JDBC adapter。后续 implementation 必须优先复用：

```text
model gateway call persistence
QDR gateway result
B1 contracts
existing QDR safety guards
```

现有可复用事实：

```text
V8 table: qdr_model_gateway_call
existing port: ModelGatewayCallPersistencePort
existing query: findByTenantAndModelCallRef(tenantId, modelCallRef)
existing record: ModelGatewayCallRecord
existing safe fields: tenantId, traceId, requestId, providerIdentityRef, status, failureCode, trustDecision,
  providerTrustDecisionRef, modelCallRef, budgetSummary, redacted summaries, input/output hashes, auditRef, traceRef, createdAt
```

如果实现发现只靠现有 `ModelGatewayCallPersistencePort` 与 in-memory / already-loaded evidence 无法支持 B2 查询，必须停止并输出：

```text
B2_REPOSITORY_EXTENSION_REQUIRED_BLOCKER
```

不得自行新增 production repository / JDBC adapter。若实现发现必须新增 schema、索引、列或 V10 migration，必须停止并输出：

```text
B2_SCHEMA_EXTENSION_REQUIRED_BLOCKER
```

不得自行新增 V10，不得修改 V1-V9 migration。

## 8. API / Controller Boundary

B2 默认不新增 API / Controller / REST endpoint。B2 view 只服务内部 usecase read model 和后续 acceptance / report 支撑。

如果后续发现必须对外暴露 API，必须停止并输出：

```text
B2_API_REQUIRED_BLOCKER
```

并转入单独 API freeze review；不得在 B2 implementation 中顺手新增 Controller、route、OpenAPI 或前端接口。

## 9. Implementation Steps For Next Task

后续 `DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION` 只允许按以下顺序执行：

1. 在 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/` 或既有 read model 包中新增 query / view / service。
2. 复用 B1 contract validator 和 `QdrPersistenceSafety` / existing read model guard 做 safe ref、raw material、credential-like、trading term 校验。
3. 只使用现有 gateway call record / mock provider / QDR gateway result evidence 组装 view。
4. 编写 usecase 单元测试，使用内存 fixture；默认不启动数据库。
5. 运行 targeted tests、`mvn -ntp -Pquality validate`、forbidden-scope diff 和 safety wording scan。
6. 最小同步 `docs/current/STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`、`CODEX_PROJECT_INSTRUCTIONS.md`。

如任一步触发 API、migration、production repository expansion 或 security boundary expansion，立即停止并输出对应 blocker。

## 10. Test Matrix

后续 B2 implementation 至少覆盖以下 19 项测试：

| # | Test item | Expected result |
| --- | --- | --- |
| 1 | provider health query by tenantId + providerRef succeeds | 返回当前 tenant 下 matching provider health view。 |
| 2 | gateway call observability query by tenantId + modelGatewayVersionRef succeeds | 返回 matching gateway call observability view。 |
| 3 | trace lookup by tenantId + traceId succeeds | 只返回当前 tenant 的 trace evidence。 |
| 4 | request lookup by tenantId + sourceRequestId succeeds | 只返回当前 tenant 的 source request evidence。 |
| 5 | tenantless query fails closed | 构造 query 或 service entry 直接拒绝，repository 不被调用。 |
| 6 | UUID-only query is absent or rejected | service 不提供 UUID-only selector；如出现 UUID-only 输入必须拒绝。 |
| 7 | cross-tenant read returns empty or fail-closed | tenant mismatch 不返回他租户数据，也不泄露存在性。 |
| 8 | list query is paginated | 所有 list path 使用 limit / offset。 |
| 9 | pageSize > 100 is rejected or capped | 默认 reject；如 cap 必须记录 cap 规则且测试覆盖。 |
| 10 | report/view does not expose raw prompt | view 字段名和 rendering 均不出现 raw prompt。 |
| 11 | report/view does not expose raw provider response | view 字段名和 rendering 均不出现 raw provider response。 |
| 12 | report/view does not expose credential-like fields | 不暴露 credential、token、cookie、apiKey、apiSecret、passphrase、secret。 |
| 13 | failure classification appears as safe enum only | 只输出 `ProviderFailureClassification` 或安全 view，不输出原始异常。 |
| 14 | trust decision does not imply provider authorization | `ALLOWED` 不产生 authorization / permission / live wording。 |
| 15 | readiness signal does not imply real HTTP / provider / LIVE | `READY` 不产生 real provider、HTTP 或 LIVE enable flag。 |
| 16 | provider-health-as-trading-signal guard is enforced | 不出现 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 或 execution signal。 |
| 17 | provider/HTTP/Provider SDK/Agent/LangGraph classes are not introduced | architecture / source scan 无新增禁用类、依赖或 import。 |
| 18 | report/read failure fails closed | read model 组装或 repository 失败转内部 fail-closed exception / empty，不返回半成品。 |
| 19 | quality validate passes | `mvn -ntp -Pquality validate` 通过；`mvnw.cmd` 风险原样记录。 |

## 11. Review Triggers

沿用 Stage-QDR-5 工程纪律：

```text
1. 如果 B2 只做 internal read model，无 API、无 migration、无 repository production expansion、无 security boundary 扩张，可 implementation + tests + boundary scan + minimal docs + commit，不做 standalone review。
2. 如果需要 API / Controller，必须输出 B2_API_REQUIRED_BLOCKER。
3. 如果需要 migration / V10，必须输出 B2_SCHEMA_EXTENSION_REQUIRED_BLOCKER。
4. 如果需要 production repository / JDBC 扩展，必须输出 B2_REPOSITORY_EXTENSION_REQUIRED_BLOCKER。
5. 如果改动 trust/security boundary，必须转 security boundary review。
```

## 12. Security Gates

B2 implementation 必须继续保持：

```text
no real HTTP
no real provider
no Provider SDK
no API key
no LangGraph
no Agent runtime
no LIVE
no NQ mutation
no trading mutation
no approval-as-execution
no gateway-result-as-trading-signal
no provider-readiness-as-live-permission
no provider-health-as-trading-signal
no raw prompt storage
no raw provider response storage
no credential storage
all failures fail-closed
tenant-bound query
source-bound trust policy
```

这些 view 只能作为内部只读 evidence / reasoning summary，不得写成 execution signal、execution permission、live permission、provider authorization 或 NQ mutation approval。

## 13. Readiness Decision

```text
STAGE_QDR_5_B2_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一任务：

```text
DH-STAGE-QDR-5-B2-PROVIDER-HEALTH-GATEWAY-CALL-READ-MODEL-IMPLEMENTATION
```
