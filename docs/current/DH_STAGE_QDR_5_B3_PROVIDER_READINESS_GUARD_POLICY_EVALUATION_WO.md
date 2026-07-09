# DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-WO

> status: DONE / WORK_ORDER_ONLY
> scope: B3 implementation boundary design
> current factsource: docs/current

## 1. Task Classification

```text
WORK_ORDER_ONLY
B3_SECURITY_BOUNDARY_DESIGN
PROVIDER_READINESS_GUARD_WO
POLICY_EVALUATION_WO
TRUST_DECISION_REVIEW_PREP
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

本轮只编制 Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation implementation work order。不得实现 Java、测试、migration、API、Repository/JDBC/Service，不得接真实 provider、真实 HTTP、Provider SDK、Agent、LangGraph 或 LIVE。

## 2. Preconditions

```text
STAGE_QDR_5_B1: DONE
STAGE_QDR_5_B2: DONE
STAGE_QDR_5_B2_CI_BLOCKER_FIX: DONE
STAGE_QDR_5_B2_COMMIT: feat(qdr): add provider health read model support
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮预检确认当前分支为 `dev`，worktree clean，最近提交包含 `cfe0e9e feat(qdr): add provider health read model support`。

## 3. Provider Readiness Guard Target

后续 B3 implementation 目标：

```text
Provider Readiness Guard / Policy Evaluation
```

边界固定为：

```text
readiness 只表示未来接入条件是否满足
readiness 不等于 provider authorization
readiness 不等于 real HTTP enablement
readiness 不等于 LIVE permission
readiness 不等于 trading permission
readiness 不得驱动 NQ
readiness failure 必须 fail-closed
```

B3 只把 B1 observability contracts 与 B2 provider health / gateway call read model 的安全 evidence 输入到策略评估。它输出内部 readiness decision，用于后续 review evidence，不开启 provider runtime，不发真实 HTTP，不授权 LIVE，不生成交易信号。

## 4. Allowed Structures

后续 B3 implementation 可新增或补齐以下结构，命名可按现有 `qdr.gateway` 风格微调：

```text
ProviderReadinessPolicy
ProviderReadinessGuard
ProviderReadinessEvaluationCommand
ProviderReadinessEvaluationResult
ProviderReadinessDecision
ProviderReadinessDecisionReason
ProviderReadinessFinding
ProviderReadinessGuardService
```

已有 B1/B2 结构必须优先复用：

```text
ModelGatewayObservabilitySummary
ProviderHealthSummary
ProviderFailureClassification
ProviderLatencyBudgetSummary
ProviderTrustDecisionSummary
ProviderReadinessSignal
ProviderHealthReadModelQuery
ProviderHealthReadModelView
ProviderHealthReadModelService
ModelGatewayObservabilityContractService
```

不得重复造与 B1/B2 已有 summary/view 语义相同的新概念。新增结构只能承载 policy evaluation、decision、reason 与 fail-closed guard 语义。

## 5. Policy Input Boundary

B3 policy evaluation 只允许读取或接收：

```text
tenantId
sourceId / sourceRef
providerRef
modelGatewayVersionRef
providerSummaryHash
failureClassification
latencyBudgetSummary
trustDecisionSummary
readinessSignal
provider health / observability read model safe view
policyVersion
traceId
sourceRequestId
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

Policy input 必须保持 tenant-bound 与 source-bound。缺失 tenant/source/policy evidence 时不得降级为允许，必须按 fail-closed decision 处理。

## 6. Decision Boundary

Provider readiness decision 只能输出以下安全枚举或等价命名：

```text
READY
NOT_READY
DEGRADED
SKIPPED
```

语义固定为：

```text
READY = 满足 future readiness criteria
READY != real provider enabled
READY != real HTTP enabled
READY != LIVE enabled
READY != trading allowed
READY != provider credential authorized
```

`READY` 只能用于内部 future-readiness evidence。`NOT_READY`、`DEGRADED`、`SKIPPED` 均保持 non-executable，不触发 provider、HTTP、NQ、trading、risk、ledger、paper 或 live mutation。

## 7. Fail-Closed Boundary

后续 B3 implementation 必须规划并测试：

```text
missing tenantId -> NOT_READY / fail-closed
missing providerRef -> NOT_READY / fail-closed
missing policyVersion -> SKIPPED 或 NOT_READY，按 policy 定义
source denied -> NOT_READY
policy denied -> NOT_READY
timeout / budget exceeded -> DEGRADED 或 NOT_READY
unknown classification -> NOT_READY
credential-like input -> fail-closed
raw provider response input -> fail-closed
trading-term input -> fail-closed
```

异常、null、unknown enum、policy exception、read model exception、contract validation exception 均不得返回 `READY`。失败路径必须输出固定安全 code / reason，不回显原始输入、payload、异常堆栈或敏感文本。

## 8. Security Boundary

B3 是 trust/security boundary 批次。后续 implementation 完成后必须进入独立 review：

```text
SECURITY_BOUNDARY_REVIEW_REQUIRED: YES
```

不得以普通 batch validation 直接进入 B4。B3 review 必须确认 readiness wording、policy deny semantics、fail-closed behavior、source-bound trust policy、tenant boundary、安全扫描和 forbidden-scope diff。

## 9. Implementation Steps For Next Task

后续 `DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION` 只允许按以下顺序执行：

1. 在 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/` 内新增 policy / guard / command / result / decision 结构。
2. 复用 B1 `ModelGatewayObservabilityContractService` 与 B2 `ProviderHealthReadModelService` 的安全 view，不读取 raw material。
3. 实现 tenant-bound、source-bound、policyVersion-bound 的 fail-closed evaluation。
4. 编写 usecase 单元测试，使用内存 safe evidence；默认不启动数据库。
5. 运行 targeted tests、`mvn -ntp -Pquality validate`、forbidden-scope diff 和 safety wording scan。
6. 最小同步 `docs/current/STATUS.md`、`WORK_ORDER.md`、`ROADMAP.md`、`TESTING.md`、`WORKLOG.md`、`CODEX_PROJECT_INSTRUCTIONS.md`。

如任一步触发 API、migration、production repository/JDBC expansion 或真实 provider/HTTP 需求，立即停止并输出 blocker，不得顺手实现。

## 10. Test Matrix

后续 B3 implementation 至少覆盖以下 21 项测试：

| # | Test item | Expected result |
| --- | --- | --- |
| 1 | valid readiness policy evaluates READY in mock/safe context | 仅在 tenant/source/provider/policy/safe evidence 全部满足时返回 `READY`。 |
| 2 | missing tenantId returns NOT_READY or fail-closed | 缺 tenantId 不进入允许路径。 |
| 3 | missing providerRef returns NOT_READY or fail-closed | 缺 provider safe ref 不进入允许路径。 |
| 4 | missing policyVersion returns SKIPPED or NOT_READY | 按 policy 定义返回 `SKIPPED` 或 `NOT_READY`，不得返回 `READY`。 |
| 5 | source denied returns NOT_READY | source-bound trust policy 拒绝时 fail-closed。 |
| 6 | policy denied returns NOT_READY | policy deny 不可升级为 degraded allow。 |
| 7 | timeout classification returns DEGRADED or NOT_READY | timeout 不可返回 executable allow。 |
| 8 | budget exceeded returns DEGRADED or NOT_READY | budget exceeded 不可返回 executable allow。 |
| 9 | unknown classification returns NOT_READY | unknown / unmapped 分类 fail-closed。 |
| 10 | credential-like input fails closed | credential、token、cookie、apiKey、apiSecret、passphrase、secret 类字段或文本被拒绝。 |
| 11 | raw prompt input fails closed | raw prompt marker 或字段名被拒绝。 |
| 12 | raw provider response input fails closed | raw provider response marker 或字段名被拒绝。 |
| 13 | BUY / SELL / MARKET_ORDER input fails closed | 交易方向或 market order 词不进入 `READY`。 |
| 14 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE input fails closed | 执行动作或 NQ mutation 词不进入 `READY`。 |
| 15 | READY does not enable real provider | 输出不包含 provider enable flag。 |
| 16 | READY does not enable real HTTP | 输出不包含 HTTP enable flag。 |
| 17 | READY does not enable LIVE | 输出不包含 LIVE enable flag。 |
| 18 | READY does not imply trading permission | 输出不包含 trading permission / execution approval 语义。 |
| 19 | no Provider SDK / HTTP client / Agent / LangGraph classes are introduced | architecture / source scan 无新增禁用类、依赖或 import。 |
| 20 | policy evaluation failure fails closed | policy exception、read model exception 或 contract exception 均返回 fail-closed decision 或内部安全异常。 |
| 21 | quality validate passes | `mvn -ntp -Pquality validate` 通过；`mvnw.cmd` 风险原样记录。 |

不得删除上述测试点。后续 implementation 可增加针对 sourceRef、traceId、sourceRequestId、policyVersion 版本兼容和 reason code rendering 的精确测试。

## 11. Review Triggers

B3 是 trust/security boundary 批次，必须 review。固定顺序：

```text
1. B3 implementation
2. B3 security boundary / close review
3. review PASS 后才允许 B4
```

不允许：

```text
B3 implementation 后直接进入 B4
B3 与 B4 合并
B3 接真实 provider / HTTP
B3 接 Provider SDK
B3 接 Agent / LangGraph
B3 修改 NQ
B3 创建 tag
```

如果后续 B3 implementation 发现必须新增 API / Controller，必须输出：

```text
B3_API_REQUIRED_BLOCKER
```

如果发现必须新增 schema、索引、列或 V10 migration，必须输出：

```text
B3_SCHEMA_EXTENSION_REQUIRED_BLOCKER
```

如果发现必须新增 production repository / JDBC adapter，必须输出：

```text
B3_REPOSITORY_EXTENSION_REQUIRED_BLOCKER
```

## 12. Security Gates

B3 implementation 必须继续保持：

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
no raw prompt storage
no raw provider response storage
no credential storage
all failures fail-closed
tenant-bound query
source-bound trust policy
```

这些 decisions 只能作为内部 policy evidence / readiness reasoning，不得写成 execution signal、execution permission、live permission、provider authorization 或 NQ mutation approval。

## 13. Readiness Decision

```text
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一任务：

```text
DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-IMPLEMENTATION
```
