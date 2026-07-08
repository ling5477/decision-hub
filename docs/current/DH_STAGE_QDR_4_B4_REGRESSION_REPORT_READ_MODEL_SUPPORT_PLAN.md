# DH stage-qdr-4 B4 Regression Report / Read Model Support Plan

> current planning document for `DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN`
> planning only; no Java, test, migration, API, provider, HTTP, Agent, LangGraph, NQ or LIVE change

```text
Task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN
Task type: PLANNING_ONLY + REGRESSION_REPORT_READ_MODEL_PLAN + QDR_REPLAY_EVALUATION_REPORTING + READ_MODEL_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / PLAN_ONLY
Date: 2026-07-08
Repository: E:/Project/decision-hub
Branch: dev
Precondition: STAGE_QDR_4_B3 / CLOSED / ACCEPTED
Next task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO
```

## 1. 目标与硬边界

B4 只规划 QDR replay / evaluation / regression 的只读报告和 read model 支撑。目标是让 B1-B3 生成的 replay case、evaluation case、expected/actual summary、regression verdict、finding、drift evidence 和版本引用可以被 tenant-bound 查询和审计复核。

本轮只做 planning：

```text
不修改 Java 生产代码
不修改 Java 测试代码
不新增 migration
不修改 V1-V9 migration
不新增 V10
不新增 Repository / JDBC / Service 实现
不新增 API / Controller / REST endpoint
不接真实 HTTP client
不接真实 provider / Provider SDK
不新增 OpenAI / Anthropic / Gemini / Ollama SDK
不接 LangGraph / AutoGen / CrewAI
不启动 Agent runtime
不修改 NQ
不读取 credential / token / cookie / apiKey / apiSecret / passphrase
不持久化 raw prompt
不持久化 raw provider response
不触碰交易、订单、撤单、账户、ledger mutation、risk mutation、paper mutation、live mutation
不把 replay / regression output 写成 trading signal
不启用 LIVE
不直接进入 B4 implementation
不直接进入 B5 final close
不打 tag
```

## 2. 前置状态确认

```text
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B3: CLOSED / ACCEPTED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

最近 20 条提交已包含：

```text
feat(qdr): add replay evaluation domain contracts
docs(qdr): plan replay evaluation persistence baseline
docs(qdr): define replay evaluation persistence implementation work order
feat(qdr): persist replay evaluation baseline
docs(qdr): close replay evaluation persistence baseline
docs(qdr): define mock gateway regression integration work order
feat(qdr): integrate mock gateway regression baseline
docs(qdr): close mock gateway regression integration
```

## 3. Report / Read Model Target

B4 implementation 的目标只能是 usecase/internal read model，不默认新增 REST API。报告支撑以下只读视图：

```text
1. replay case summary
2. evaluation case summary
3. expected vs actual decision summary
4. regression verdict summary
5. finding list
6. providerSummaryHash / modelGatewayVersionRef / promptVersionRef / policyVersion drift
7. redaction guard evidence
8. trading-term guard evidence
9. tenant-bound regression report
```

报告输出必须保持 read-only：

```text
只读 summary / ref / hash / version / verdict / finding
不执行 replay
不调用 provider
不发 HTTP
不访问 NQ
不生成 trading signal
不触发 order / risk / ledger / paper / live mutation
```

## 4. Planned Structures

以下结构是 B4 implementation 规划对象，本轮不创建代码。命名可在 implementation WO 中按现有 `qdr/readmodel` 风格微调。

| 结构 | 输入 | 输出 | tenantId 要求 | case/evaluation/verdict 查询边界 | trace/request/decision 追踪 | 分页要求 | fail-closed 行为 | redaction 规则 | provider / HTTP / NQ / trading |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `RegressionReportQuery` | `tenantId` + one required selector：`caseId` / `evaluationId` / `verdictId` / `traceId` / `sourceRequestId` / `sourceDecisionId` / list filter；`pageSize` / `offset` | validated query object | required；空白 tenant 直接拒绝 | 禁止 UUID-only；禁止 selector 缺失；单条查询必须 tenant + business ID | optional `requesterId` / `traceId` 只能作为审计上下文，不替代 tenant | list query required；max pageSize 100 | 缺 tenant、无 selector、pageSize > 100、unsafe text、多个互斥 selector 冲突时拒绝 | query 不承载 raw prompt/provider response/credential | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionReportView` | tenant-bound replay/evaluation/verdict records + finding list + drift summary | 单个 tenant-bound report view | 所有组成 records 必须同 tenant | 支持 caseId/evaluationId/verdictId 聚合；不存在或跨 tenant 返回 empty | 必须暴露 `traceId`、`sourceRequestId`、`sourceDecisionId`、`requestId`、`caseId`、`evaluationId`、`verdictId` | finding 子列表分页；summary 不允许无限列表 | 任一 required record 缺失、cross-tenant mismatch、repository failure 时 empty 或 unavailable exception | 只展示 safe refs、hash、redacted summary；禁止 raw material | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionReportFindingView` | `RegressionFindingRecord` + optional field/drift classification | 脱敏 finding 行 | record tenant 必须匹配 query tenant | 只允许 tenant + verdictId / evaluationId / caseId 派生读取 | 必须带 `traceId`、`requestId`、`evaluationId`、`verdictId` | list required；max pageSize 100 | finding message 命中敏感词或可执行交易词时拒绝构造 | finding message 和 evidenceRef 必须脱敏；不回显 raw payload | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionDriftSummary` | expected/actual summary hashes、version refs、verdict/finding codes | drift 分类摘要 | 只从同 tenant report input 派生，不单独跨 tenant 查库 | 绑定 caseId/evaluationId/verdictId | 记录 traceId/requestId/decisionId 以支持 audit | 不分页；作为 report 子对象 | 缺 expected 或 actual 时输出 `SKIPPED` 或 blocked reason，不推断通过 | 只展示 drift classification、safe ref/hash/version，不展示原文 | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionReadModelService` | `RegressionReportQuery` + read model query port / existing B2 repositories | Optional single report 或 paginated report summaries | service 入口强制 tenant-bound | 先按 tenant + business ID 查主 record，再聚合附属 records | 所有返回必须保留 trace/request/decision refs | 所有 list 入口显式分页，max 100 | repository exception 转 unavailable/fail-closed；不存在或跨租户返回 empty | 复用 `ReadModelValidation` / `ReplayPersistenceGuard` 等价策略 | provider NO；HTTP NO；NQ NO；trading signal NO |

## 5. Query Boundaries

B4 read model 必须 tenant-bound。允许规划的查询：

```text
1. query by tenantId + caseId
2. query by tenantId + evaluationId
3. query by tenantId + verdictId
4. list by tenantId + createdAt
5. list by tenantId + verdict
6. list by tenantId + severity
7. trace lookup by tenantId + traceId
8. request lookup by tenantId + sourceRequestId
9. decision lookup by tenantId + sourceDecisionId
```

禁止查询：

```text
1. UUID-only query
2. tenantless list
3. cross-tenant read
4. unbounded list
5. report export containing raw prompt
6. report export containing raw provider response
7. report export containing credential-like fields
```

分页规则：

```text
所有 list 查询必须包含 pageSize / offset 或等价 page request。
pageSize 默认建议 50，最大 100。
pageSize > 100 必须 rejected 或 capped；implementation WO 需二选一并写入测试。
排序默认 `createdAt desc, id asc`，避免同时间戳结果不稳定。
```

## 6. Report Content Boundary

报告允许展示：

```text
safe refs
caseId
evaluationId
verdictId
traceId
sourceRequestId
sourceDecisionId
decisionType
actionLabel
confidenceBand
riskLevel
verdict
severity
findingCode
findingMessage
providerSummaryHash
modelGatewayVersionRef
promptVersionRef
policyVersion
createdAt
updatedAt
redacted summary
drift classification
```

报告禁止展示：

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
order instruction
execution instruction
account secret
position mutation
ledger mutation
risk mutation
```

如未来需要 report export，必须继续沿用同一内容边界，并显式拒绝包含 raw prompt、raw provider response、credential-like fields、order instruction 或 NQ mutation payload 的 export。

## 7. Drift Summary Rules

`RegressionDriftSummary` 需要把 B3 comparator 的结构化 drift 转成只读分类，不改变 verdict 语义：

| Drift item | planned classification | report behavior |
| --- | --- | --- |
| `decisionType` drift | `DECISION_TYPE_MATCH` / `DECISION_TYPE_MISMATCH` / `DECISION_TYPE_SKIPPED` | mismatch 只进入 finding/drift，不变成交易动作。 |
| `actionLabel` drift | `ACTION_LABEL_MATCH` / `ACTION_LABEL_MISMATCH` / `ACTION_LABEL_FORBIDDEN` / `ACTION_LABEL_SKIPPED` | `BUY`、`SELL`、`MARKET_ORDER` 禁止作为可执行 action。 |
| `confidenceBand` drift | `CONFIDENCE_MATCH` / `CONFIDENCE_SOFT_DRIFT` / `CONFIDENCE_HARD_DRIFT` / `CONFIDENCE_SKIPPED` | 只展示 band drift，不推断仓位或订单。 |
| `riskLevel` drift | `RISK_MATCH` / `RISK_SOFT_DRIFT` / `RISK_HARD_DRIFT` / `RISK_SKIPPED` | 不写 NQ risk fact，不触发 risk mutation。 |
| `evidenceRefs` drift | `EVIDENCE_MATCH` / `EVIDENCE_SUPERSET` / `EVIDENCE_MISSING` / `EVIDENCE_SKIPPED` | evidenceRef 只展示 safe ref/hash。 |
| `forbiddenActions` drift | `FORBIDDEN_ACTIONS_MATCH` / `FORBIDDEN_ACTIONS_EXTENDED` / `FORBIDDEN_ACTIONS_MISSING` | `PLACE_ORDER`、`CANCEL_ORDER`、`MUTATE_NQ_STATE` 只能作为 forbidden action。 |
| `providerSummaryHash` drift | `PROVIDER_SUMMARY_HASH_MATCH` / `PROVIDER_SUMMARY_HASH_DRIFT` / `PROVIDER_SUMMARY_HASH_SKIPPED` | 只展示 hash drift，不展示 provider response。 |
| `modelGatewayVersionRef` drift | `MODEL_GATEWAY_VERSION_MATCH` / `MODEL_GATEWAY_VERSION_DRIFT` / `MODEL_GATEWAY_VERSION_SKIPPED` | 只展示 version ref。 |
| `promptVersionRef` drift | `PROMPT_VERSION_MATCH` / `PROMPT_VERSION_DRIFT` / `PROMPT_VERSION_SKIPPED` | 只展示 prompt ref，不展示 prompt text。 |
| `policyVersion` drift | `POLICY_VERSION_MATCH` / `POLICY_VERSION_DRIFT` / `POLICY_VERSION_SKIPPED` | policy drift 不自动放宽安全边界。 |

固定解释规则：

```text
PASS / WARN / FAIL / SKIPPED 只是 regression verdict。
PASS / WARN / FAIL / SKIPPED 不得作为 trading signal。
LONG_BIAS / SHORT_BIAS 只能作为 direction label。
LONG_BIAS / SHORT_BIAS 不得映射 BUY / SELL。
BUY / SELL / MARKET_ORDER 不得作为 actionLabel。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 不得作为 allowed action。
```

## 8. Persistence Reuse

B4 默认不新增 migration。B4 implementation 必须复用 B2/V9 表：

```text
qdr_replay_case
qdr_evaluation_case
qdr_expected_decision_summary
qdr_regression_verdict
qdr_regression_finding
qdr_replay_input_ref
qdr_replay_output_ref
```

复用结论：

```text
不新增 V10。
不修改 V9。
不新增新表。
不把 report/read model 的临时需求塞进 JSONB raw payload。
如发现 B2 schema 不足，输出 blocker，不得偷偷改 schema。
```

已存在 V9 支撑点：

```text
tenantId + caseId / evaluationId / verdictId unique/indexed refs
tenantId + traceId indexes
tenantId + sourceRequestId indexes
tenantId + sourceDecisionId indexes
tenantId + createdAt indexes
tenantId + verdict / severity / createdAt indexes
safe ref/hash/version/policy fields
redaction and trading-term CHECK / COMMENT guard
```

## 9. API / Controller Decision

B4 plan 结论：

```text
默认不新增 API / Controller。
B4 implementation 推荐只做 usecase/internal read model。
不新增 REST endpoint。
不修改 OpenAPI。
不修改 auth / tenant / security boundary。
```

如果后续 implementation 发现必须新增 API / Controller，必须停止并输出：

```text
B4_API_REQUIRED_BLOCKER
```

然后另起 API freeze review；不得在 B4 implementation 中直接实现 API。

## 10. B4 Implementation Test Matrix

B4 implementation 必须规划并覆盖以下测试，本轮不得实现：

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | report query by tenantId + caseId succeeds | 返回同租户 replay/evaluation/verdict/report summary。 |
| 2 | report query by tenantId + evaluationId succeeds | 返回同租户 evaluation report。 |
| 3 | report query by tenantId + verdictId succeeds | 返回同租户 verdict report 与 finding list。 |
| 4 | tenantless query fails closed | 缺失 tenantId 拒绝，不查库。 |
| 5 | UUID-only query is absent or rejected | 不提供 UUID-only method；若传入仅 UUID 则拒绝。 |
| 6 | cross-tenant report read returns empty or fail-closed | tenant B 不能读取 tenant A report。 |
| 7 | list query is paginated | list by createdAt/verdict/severity/trace/request/decision 都显式分页。 |
| 8 | pageSize > 100 is rejected or capped | implementation WO 明确 rejected 或 capped 并测试。 |
| 9 | report does not expose raw prompt | view / JSON / summary 不包含 raw prompt。 |
| 10 | report does not expose raw provider response | view / JSON / summary 不包含 raw provider response。 |
| 11 | report does not expose credential-like fields | credential-like keys 被拒绝或不出现在 view。 |
| 12 | drift summary includes providerSummaryHash mismatch | providerSummaryHash drift 有结构化分类。 |
| 13 | drift summary includes modelGatewayVersionRef mismatch | modelGatewayVersionRef drift 有结构化分类。 |
| 14 | drift summary includes promptVersionRef mismatch | promptVersionRef drift 有结构化分类。 |
| 15 | drift summary includes policyVersion mismatch | policyVersion drift 有结构化分类。 |
| 16 | BUY / SELL / MARKET_ORDER are not exposed as executable action | 它们只可作为 forbidden/prohibited evidence。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE are not exposed as allowed action | 它们不得进入 allowed action。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | safety scan 和 architecture test 证明未新增 runtime。 |
| 19 | report generation failure fails closed | repository/query/drift aggregation failure 不返回 partial unsafe report。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` BUILD SUCCESS。 |

## 11. Review / Freeze Rules

B4 implementation 的 review 规则：

```text
如果 B4 只实现 usecase/internal read model，无 API、无 migration、无 security expansion，可 implementation 后 validation + commit，不做 standalone freeze。
如果 B4 新增 API / Controller，必须先 API freeze review。
如果 B4 新增 migration，必须先 migration freeze review。
如果 B4 改动 tenant/security/redaction boundary，必须 close review。
Stage-QDR-4 final close 必须单独 review。
```

不允许：

```text
B4 plan 后直接实现 API。
B4 与 B5 合并。
B4 接真实 provider / HTTP。
B4 接 Agent / LangGraph。
B4 修改 NQ。
B4 打 tag。
```

Stage tag 规则：

```text
stage-qdr-4 tag 只允许在 Stage-QDR-4 final close 后处理。
B4 不是 final close，不允许打 tag。
```

## 12. Readiness Decision

```text
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步：

```text
DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO
```
