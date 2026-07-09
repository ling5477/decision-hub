# DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO

> Historical archive note: This is a historical source document captured before tag close. Current stage state is TAGGED; see README.md / STATUS_SNAPSHOT.md. Historical status wording in this file is not a current factsource.

> status: DONE / WORK_ORDER_ONLY
> scope: B4 implementation boundary design
> current factsource: docs/current

## 1. Task Classification

```text
WORK_ORDER_ONLY
B4_IMPLEMENTATION_BOUNDARY_DESIGN
OBSERVABILITY_REPORT_WO
PROVIDER_READINESS_ACCEPTANCE_SUPPORT
CURRENT_DOCS_ACCEPTANCE_SUPPORT
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

本轮只编制 Stage-QDR-5 B4 Observability Report / Acceptance Support implementation work order。不得实现 Java、测试、migration、API、Controller、Repository/JDBC/Service，不得接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE。

## 2. Preconditions

```text
STAGE_QDR_5_B1: DONE
STAGE_QDR_5_B2: DONE
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮预检确认当前分支为 `dev`，worktree clean，最近提交包含：

```text
feat(qdr): add provider readiness guard policy evaluation
docs(qdr): close provider readiness guard policy evaluation
```

## 3. Observability Report Target

后续 B4 implementation 目标：

```text
Observability Report / Acceptance Support
```

边界固定为：

```text
internal report only by default
no API / Controller by default
no migration by default
no real provider
no real HTTP
no Provider SDK
no credential access
no trading signal
final close acceptance support only
```

B4 只把 B1 observability contracts、B2 provider health / gateway call read model、B3 readiness guard / policy evaluation 的安全结果整理成内部只读 report 与 Stage-QDR-5 final close 验收证据。B4 不改变 gateway call 写入链路，不启动 provider runtime，不扩大 provider authorization、LIVE、trading、NQ mutation 或 execution 边界。

## 4. Allowed Structures

后续 B4 implementation 可新增或补齐以下结构，命名可按现有 `qdr.gateway` 风格微调：

```text
ModelGatewayObservabilityReport
ProviderHealthReportSection
ProviderReadinessReportSection
ProviderReadinessAcceptanceSummary
ProviderFailureClassificationSummary
ProviderLatencyBudgetReport
ProviderTrustDecisionReport
ProviderReadinessEvidenceView
StageQdr5AcceptanceEvidence
ObservabilityReportService
```

如实现中需要更少对象，可以合并 report / section 类型，但必须保留以下语义边界：

```text
provider health summary
gateway observability summary
failure classification summary
latency budget summary
trust decision summary
readiness decision summary
acceptance evidence
security boundary evidence
not authorization
not LIVE permission
not trading signal
```

`ObservabilityReportService` 应优先实现为 usecase 内部 service，组合 B1/B2/B3 已有 safe view / result / summary，不引入 Spring Web、Controller、production JDBC adapter、Provider SDK 或真实 HTTP client。

## 5. Report Input Boundary

B4 report generation 只允许读取或接收：

```text
tenantId
providerRef
sourceRef
modelGatewayVersionRef
providerSummaryHash
failureClassification
latencyBudgetSummary
trustDecisionSummary
readinessSignal
readinessDecision
readinessFinding
readinessPolicyVersion
traceId
sourceRequestId
createdAt / observedAt
B1 observability summary
B2 provider health read model view
B3 readiness evaluation result
```

禁止读取或接收：

```text
credential
token
cookie
apiKey
apiSecret
passphrase
secret
raw prompt
raw provider response
raw request payload
raw response payload
account secret
order instruction
execution instruction
ledger mutation
risk mutation
paper/live mutation
```

缺 `tenantId`、缺 `providerRef`、缺 readiness decision 或输入包含敏感 / 交易 / NQ mutation 语义时，后续实现必须 fail-closed 或输出 `SKIPPED`，不得生成 `PASS`。

## 6. Report Output Boundary

Report 允许输出：

```text
safe refs
tenantId
providerRef
sourceRef
modelGatewayVersionRef
traceId
sourceRequestId
providerSummaryHash
failureClassification
latencyBudgetSummary
trustDecisionSummary
readinessStatus
readinessDecision
readinessFinding
readinessPolicyVersion
acceptanceStatus
createdAt / observedAt
redacted summary
```

Report 禁止输出：

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
raw request payload
raw response payload
order instruction
execution instruction
account secret
position mutation
ledger mutation
risk mutation
provider credential status
real provider enablement
real HTTP enablement
LIVE permission
trading permission
```

任何 report rendering、summary、finding、reason code 或 acceptance evidence 都不得回显原始输入、原始异常堆栈、raw payload、credential-like material 或 trading instruction。

## 7. Acceptance Status Boundary

B4 acceptance support 只能输出以下安全枚举或等价命名：

```text
PASS
WARN
FAIL
SKIPPED
```

语义固定为：

```text
PASS = Stage-QDR-5 internal acceptance evidence passed
PASS != provider authorized
PASS != real provider enabled
PASS != real HTTP enabled
PASS != LIVE enabled
PASS != trading allowed
```

`PASS` 只表示 internal acceptance evidence 对 Stage-QDR-5 final close 有支持价值。`WARN`、`FAIL`、`SKIPPED` 均保持 non-executable，不触发 provider、HTTP、NQ、trading、risk、ledger、paper 或 live mutation。

## 8. Persistence / Repository Boundary

B4 默认不新增 migration，不新增 production Repository / JDBC adapter。后续 implementation 必须优先复用：

```text
B1 observability contracts
B2 provider health read model
B3 readiness evaluation result
existing QDR safety guards
docs/current acceptance evidence
```

如果现有结构不足，必须停止并输出：

```text
B4_REPORT_STRUCTURE_BLOCKER
```

如果需要 API / Controller，必须停止并输出：

```text
B4_API_REQUIRED_BLOCKER
```

如果需要 migration / schema，必须停止并输出：

```text
B4_SCHEMA_EXTENSION_REQUIRED_BLOCKER
```

如果需要 production Repository / JDBC adapter，必须停止并输出：

```text
B4_REPOSITORY_EXTENSION_REQUIRED_BLOCKER
```

不得自行新增 V10，不得修改 V1-V9 migration，不得顺手新增 REST endpoint、OpenAPI、Controller、production persistence adapter 或跨租户聚合。

## 9. Implementation Steps For Next Task

后续 `DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION` 只允许按以下顺序执行：

1. 在 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/` 或既有 read model/report 包中新增 report / section / acceptance evidence / service。
2. 复用 B1 `ModelGatewayObservabilityContractService`、B2 `ProviderHealthReadModelService`、B3 `ProviderReadinessGuardService` 的 safe evidence，不读取 raw material。
3. 实现 tenant-bound、source-bound、policyVersion-bound 的 report generation 与 acceptance summary。
4. 编写 usecase 单元测试，使用内存 safe evidence；默认不启动数据库。
5. 运行 targeted tests、`mvn -ntp -Pquality validate`、forbidden-scope diff 和 safety wording scan。
6. 最小同步 `docs/current/STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`、`CODEX_PROJECT_INSTRUCTIONS.md`。

如任一步触发 API、migration、production repository expansion 或 security boundary expansion，立即停止并输出对应 blocker。

## 10. Test Matrix

后续 B4 implementation 至少覆盖以下 20 项测试：

| # | Test item | Expected result |
| --- | --- | --- |
| 1 | valid observability report can be generated from B1/B2/B3 safe inputs | 有效 tenant/provider/source/readiness evidence 可生成 internal report。 |
| 2 | missing tenantId fails closed | 缺 tenantId 不生成 `PASS`。 |
| 3 | missing providerRef fails closed | 缺 provider safe ref 不生成 `PASS`。 |
| 4 | missing readiness decision fails closed or returns SKIPPED | 缺 readiness decision 输出 `SKIPPED` 或 fail-closed，不能输出 `PASS`。 |
| 5 | report includes failure classification summary | 输出安全 failure classification summary，不包含原始异常。 |
| 6 | report includes latency budget summary | 输出安全 latency / budget summary，不表示真实 provider billing。 |
| 7 | report includes trust decision summary | 输出 trust decision summary，denied/degraded/skipped 不可执行。 |
| 8 | report includes readiness finding summary | 输出 readiness finding summary，不回显原始敏感输入。 |
| 9 | report includes acceptance status | 输出 `PASS / WARN / FAIL / SKIPPED` 之一。 |
| 10 | report does not expose raw prompt | 字段名、summary、rendering 均不暴露 raw prompt。 |
| 11 | report does not expose raw provider response | 字段名、summary、rendering 均不暴露 raw provider response。 |
| 12 | report does not expose credential-like fields | 不暴露 credential、token、cookie、apiKey、apiSecret、passphrase、secret。 |
| 13 | PASS does not imply provider authorization | `PASS` 不生成 provider authorization 语义或 flag。 |
| 14 | PASS does not imply real HTTP / real provider / LIVE | `PASS` 不产生 real HTTP、real provider、LIVE enable flag。 |
| 15 | report does not imply trading permission | report 不输出 trading permission / execution approval。 |
| 16 | BUY / SELL / MARKET_ORDER input fails closed | 交易方向或 market order 词不进入 `PASS`。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE input fails closed | 执行动作或 NQ mutation 词不进入 `PASS`。 |
| 18 | no Provider SDK / HTTP client / Agent / LangGraph classes are introduced | architecture / source scan 无新增禁用类、依赖或 import。 |
| 19 | report generation failure fails closed | report 组装失败转 `FAIL` / `SKIPPED` 或内部安全异常，不返回半成品 `PASS`。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过；`mvnw.cmd` 风险原样记录。 |

不得删除上述测试点。后续 implementation 可增加针对 sourceRef、traceId、sourceRequestId、policyVersion、redacted summary rendering 与 time range 的精确测试。

## 11. Review Triggers

沿用 Stage-QDR-5 工程纪律：

```text
1. 如果 B4 只做 internal report / acceptance support，无 API、无 migration、无 production repository/JDBC、无 security boundary 扩张，可 implementation + tests + boundary scan + minimal docs + commit。
2. 如果需要 API / Controller，必须输出 B4_API_REQUIRED_BLOCKER。
3. 如果需要 migration / V10，必须输出 B4_SCHEMA_EXTENSION_REQUIRED_BLOCKER。
4. 如果需要 production repository / JDBC 扩展，必须输出 B4_REPOSITORY_EXTENSION_REQUIRED_BLOCKER。
5. 如果改动 trust/security boundary，必须转 security boundary review。
6. B4 完成后进入 Stage-QDR-5 final close review，不直接 archive/tag。
```

不允许：

```text
B4 work order 后直接进入 final close
B4 与 final close 合并
B4 接真实 provider / HTTP
B4 接 Provider SDK
B4 接 Agent / LangGraph
B4 修改 NQ
B4 创建 tag
```

## 12. Security Gates

B4 implementation 必须继续保持：

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
no provider-readiness-as-provider-authorization
no provider-health-as-trading-signal
no acceptance-pass-as-provider-authorization
no acceptance-pass-as-live-permission
no acceptance-pass-as-trading-permission
no raw prompt storage
no raw provider response storage
no credential storage
all failures fail-closed
tenant-bound report
source-bound trust policy
```

这些 reports 和 acceptance summaries 只能作为 Stage-QDR-5 internal final close evidence，不得写成 execution signal、execution permission、live permission、provider authorization、trading permission 或 NQ mutation approval。

## 13. Readiness Decision

```text
STAGE_QDR_5_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_FINAL_CLOSE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一任务：

```text
DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-IMPLEMENTATION
```
