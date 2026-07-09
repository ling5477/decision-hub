# DH Stage-QDR-5 Implementation Work Order

## 1. 任务定位

```text
Task: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
Type: WORK_ORDER_ONLY + STAGE_QDR_5_IMPLEMENTATION_PLANNING + MODEL_GATEWAY_OBSERVABILITY_WO + PROVIDER_READINESS_HARDENING_WO + SECURITY_BOUNDARY_DESIGN + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / WORK_ORDER_ONLY
Stage-QDR-4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Stage-QDR-5 plan: DONE / PLAN_ONLY
Stage-QDR-5 implementation: NOT_STARTED
```

本工单只把 Stage-QDR-5 拆成可执行批次，并冻结每个批次的边界、测试矩阵、review 触发条件、禁止事项和提交纪律。本工单不实现 Java 生产代码，不修改测试，不新增 migration，不新增 API / Controller，不新增 Repository / JDBC / Service，不接真实 provider，不发真实 HTTP，不启动 Agent / LangGraph runtime，不开启 LIVE，不修改 NQ。

## 2. 前置状态

```text
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_PLAN: DONE
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

前置 Git 证据必须来自当前工作区命令，不得硬编码本机路径。Stage-QDR-5 implementation batch 开始前仍需重新执行 preflight；本工单完成只授权 B1 作为下一任务，不授权 B2/B3 并行推进。

## 3. Stage-QDR-5 总目标

```text
Stage-QDR-5 = Model Gateway Observability / Provider Readiness Hardening
```

阶段目标是把现有 mock model gateway call、provider trust decision、budget / usage summary、failure code、redacted refs 与 replay/regression evidence 汇总为可审计、tenant-bound、fail-closed 的 provider readiness evidence。该 readiness 仅表示未来接入条件评估，不是 provider authorization、live enablement、trading permission 或 execution approval。

## 4. B1: Model Gateway Observability Contracts

目标：

```text
定义 model gateway observability 与 provider readiness 的 domain/usecase contracts。
```

建议对象：

```text
ModelGatewayObservabilitySummary
ProviderHealthSummary
ProviderFailureClassification
ProviderLatencyBudgetSummary
ProviderTrustDecisionSummary
ProviderReadinessSignal
```

实现边界：

```text
scope: domain/usecase contract only
target package: dh-domain / dh-usecase existing qdr model/gateway boundary
no migration
no API
no Controller
no Repository / JDBC / Service production expansion
no real provider
no real HTTP
no Provider SDK
no credential access
no raw prompt storage
no raw provider response storage
no trading signal
```

B1 必须复用现有安全语义：`ModelGatewayFailureCode`、`ModelGatewayCallStatus`、`ModelGatewayCallTrustDecision`、`ModelGatewayUsage`、`ModelCallBudget`、`QdrPersistenceSafety` 和 safe hash/ref 边界。任何 summary 字段只能保存安全引用、脱敏摘要、状态枚举、计数、延迟桶或 hash，不得保存原始 prompt、原始 provider response、credential-like key 或可执行交易字段。

Review 规则：

```text
ordinary domain/usecase contract batch
standalone review: NO, unless migration / API / security expansion / P0-P1 blocker appears
required close shape: implementation + tests + boundary scan + minimal docs + commit
```

## 5. B2: Provider Health / Gateway Call Read Model

目标：

```text
基于现有 model gateway call / mock provider / QDR gateway result，提供内部只读 health/read model。
```

默认数据来源：

```text
V8 qdr_model_gateway_call
ModelGatewayCallRecord
ModelGatewayCallPersistencePort
JdbcModelGatewayCallRepository if existing methods are sufficient
QdrModelGatewayIntegrationResult safe refs
```

实现边界：

```text
tenant-bound query
internal read model only by default
no raw provider response
no raw prompt storage
no credential access
no real provider
no real HTTP
no API by default
no cross-tenant aggregation
providerSummaryHash / modelGatewayVersionRef are safe refs only
```

Blocker 规则：

```text
If API is required: STAGE_QDR_5_B2_API_REVIEW_REQUIRED
If migration is required: STAGE_QDR_5_B2_SCHEMA_REVIEW_REQUIRED
If production repository expansion is required beyond existing V8 adapter boundary: STAGE_QDR_5_B2_REPOSITORY_EXPANSION_REVIEW_REQUIRED
If cross-tenant read is required: STAGE_QDR_5_B2_TENANT_BOUNDARY_BLOCKED
```

Review 规则：

```text
internal read model only: validation + commit
API / migration / repository production expansion: blocker -> freeze/security review
security boundary expansion: blocker -> security-boundary review
```

## 6. B3: Provider Readiness Guard / Policy Evaluation

目标：

```text
实现 provider readiness decision / trust gate / fail-closed classification。
```

必须满足：

```text
readiness means future integration condition only
readiness is not provider authorization
readiness is not live enablement
readiness is not trading permission
readiness failure always fails closed
```

实现边界：

```text
source-bound trust policy
tenant-bound query
all failures fail-closed
no real provider
no real HTTP
no Provider SDK
no credential lookup
no approval-as-execution
no gateway-result-as-trading-signal
no provider-readiness-as-live-permission
no provider-health-as-trading-signal
```

Failure classification 至少覆盖：

```text
timeout
budget exceeded
policy denied
source denied
unknown
```

Review 规则：

```text
security/trust boundary batch
required review: close review or security-boundary review
commit allowed only after review decision permits close
```

## 7. B4: Observability Report / Acceptance Support

目标：

```text
整合 B1-B3 的 summary / report / current docs acceptance support。
```

实现边界：

```text
internal report only by default
no API unless separately reviewed
no migration unless separately reviewed
no real provider
no real HTTP
no Provider SDK
no Agent / LangGraph
no LIVE
no trading / order / ledger / risk / paper mutation
```

Report 必须表达：

```text
provider health is observability evidence only
provider readiness is future condition evidence only
all denied / degraded / skipped states remain non-executable
failure or missing evidence fails closed
raw material and credential material are absent
```

Review 规则：

```text
no API / migration / security expansion: validation + commit
API / migration / security boundary: blocker -> review
```

## 8. B5: Stage-QDR-5 Final Close Review / Archive / Tag

目标：

```text
Stage-QDR-5 final close review -> archive close -> tag close。
```

固定顺序：

```text
1. final close review PASS
2. archive close docs commit
3. worktree clean
4. annotated tag
5. tag push
6. next stage planning
```

B5 前置条件：

```text
B1-B4 complete
quality validate PASS
boundary scan PASS or false-positive-only with evidence
no forbidden-scope diff
staged empty before close decision
Stage-QDR-5 implementation evidence complete
```

B5 禁止事项：

```text
no tag before archive close commit
no tag in final close review task
no next stage planning before tag close
no real provider dry-run
no Agent / LangGraph preparation implementation
```

## 9. 测试矩阵

| # | 测试点 | 最小预期 | 批次 |
| --- | --- | --- | --- |
| 1 | valid provider health summary can be created | 有效 tenant/provider/call refs 可构造 summary。 | B1 |
| 2 | missing tenantId fails closed | 缺 tenantId 直接拒绝或生成 denied readiness。 | B1/B3 |
| 3 | missing providerRef fails closed | 缺 provider safe ref 直接拒绝或生成 denied readiness。 | B1/B3 |
| 4 | failure classification supports timeout / budget exceeded / policy denied / source denied / unknown | 分类稳定映射，不泄露原始异常。 | B1/B3 |
| 5 | latency budget summary records p50 / p95 / p99 or equivalent safe fields | 延迟/预算字段为安全数值，不代表真实 provider billing。 | B1/B2 |
| 6 | trust decision summary records allowed / denied / degraded / skipped | trust 状态可表达且 denied/degraded/skipped 不可执行。 | B1/B3 |
| 7 | readiness signal cannot enable real provider | readiness 输出不产生 provider enable flag。 | B3 |
| 8 | readiness signal cannot enable real HTTP | readiness 输出不产生 HTTP enable flag。 | B3 |
| 9 | readiness signal cannot enable LIVE | readiness 输出不产生 LIVE enable flag。 | B3 |
| 10 | readiness signal cannot generate trading signal | readiness 输出不包含 BUY / SELL / PLACE_ORDER / CANCEL_ORDER。 | B3 |
| 11 | raw provider response is rejected | raw response 字段或 key 被 redaction boundary 拒绝。 | B1/B2 |
| 12 | credential-like key is rejected | token / key / secret / passphrase 类文本被拒绝。 | B1/B2 |
| 13 | provider health read model is tenant-bound | 查询必须带 tenantId。 | B2 |
| 14 | cross-tenant read returns empty or fail-closed | tenant mismatch 不返回他租户数据。 | B2 |
| 15 | providerSummaryHash / modelGatewayVersionRef are safe refs only | 仅允许 hash/ref，不保存 raw material。 | B1/B2 |
| 16 | no Provider SDK / HTTP client / Agent / LangGraph classes introduced | architecture / rg scan 无新增禁用类或依赖。 | B1-B4 |
| 17 | repository or report failure fails closed | read model/report 失败时输出 denied/skipped 或抛内部可理解异常。 | B2/B4 |
| 18 | quality validate passes | `mvn -ntp -Pquality validate` 通过；`mvnw.cmd` 风险如实记录。 | B1-B5 |

后续 batch 可增加针对类名的精确测试，但不得删除上述测试点。

## 10. Review 触发规则

```text
ordinary batch standalone review: NO
migration trigger: review required
API / Controller trigger: review required
security boundary trigger: review required
stage close trigger: review required
P0 / P1 blocker trigger: review required
ordinary batch close shape: implementation + tests + boundary scan + minimal docs + commit
stage final close PASS -> archive close -> tag close -> next stage planning
```

普通 batch 不做 standalone review。若普通 batch 中发现必须新增 migration、API / Controller、production repository expansion 或安全边界扩大，必须先停止 implementation，输出 blocker，并进入单独 review。

## 11. 安全边界

```text
real HTTP: NO
real provider: NO
Provider SDK: NO
API key: NO
LangGraph runtime: NO
Agent runtime: NO
LIVE: DISABLED
NQ mutation: NO
trading mutation: NO
approval-as-execution: NO
gateway-result-as-trading-signal: NO
provider-readiness-as-live-permission: NO
provider-health-as-trading-signal: NO
raw prompt storage: NO
raw provider response storage: NO
credential storage: NO
failure mode: fail-closed
query boundary: tenant-bound
trust policy: source-bound
```

Stage-QDR-5 不得读取、复制、输出或持久化 credential / token / cookie / apiKey / apiSecret / passphrase。任何需要真实 provider、真实 HTTP、Provider SDK、Agent runtime、LangGraph runtime、LIVE 或 NQ mutation 的需求都必须转入后续独立 planning，不得在 Stage-QDR-5 implementation batch 内实现。

## 12. 验证命令

每个 B1-B4 batch 至少执行：

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only

git diff --name-only -- `
  dh-domain/src/main `
  dh-usecase/src/main `
  dh-app/src/main `
  dh-infra/src/main `
  contracts `
  golden_cases `
  "dh-*/src/main/resources/db/migration"

mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

若 `.\mvnw.cmd -v` 仍输出 wrapper 异常，记录为：

```text
WRAPPER_UNUSABLE / P2 TOOLING RISK
```

不得把 Maven wrapper 写成 PASS。若 `mvn -ntp -Pquality validate` 失败，必须做 RCA、最小修复、再验证；无法修复时输出 blocker。

## 13. 提交纪律

```text
B1 next task: DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
B2 cannot start before B1 commit
B3 cannot start before B2 close/commit or explicit blocker resolution
B4 cannot start before B1-B3 evidence is available
B5 cannot start before B1-B4 are complete
no all-in-one implementation
no automatic tag
no push unless tag close explicitly authorizes it
```

建议提交信息：

```text
docs(qdr): define stage-qdr-5 implementation work order
```

## 14. Readiness Decision

```text
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一任务：

```text
DH-STAGE-QDR-5-B1-MODEL-GATEWAY-OBSERVABILITY-CONTRACTS
```
