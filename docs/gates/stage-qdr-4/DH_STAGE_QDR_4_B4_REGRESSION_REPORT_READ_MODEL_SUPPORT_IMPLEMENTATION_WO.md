# DH stage-qdr-4 B4 Regression Report / Read Model Support Implementation Work Order

> current work order for `DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION`
> work order only; no Java, test, migration, API, provider, HTTP, Agent, LangGraph, NQ or LIVE change in this round

```text
Task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO
Task type: WORK_ORDER_ONLY + B4_IMPLEMENTATION_BOUNDARY_DESIGN + REGRESSION_REPORT_READ_MODEL_WO + QDR_REPLAY_EVALUATION_REPORTING + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / WORK_ORDER_ONLY
Date: 2026-07-08
Repository: E:/Project/decision-hub
Branch: dev
Precondition: STAGE_QDR_4_B4_PLAN / DONE
Next task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

## 1. Work Order Scope

本工单只冻结 B4 implementation 的边界、测试矩阵、validation 和 close/final review 规则。本轮不实现 Java、测试、migration、API、Repository、JDBC、Service 或 runtime。

本工单确认：

```text
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B3: CLOSED / ACCEPTED
STAGE_QDR_4_B4_PLAN: DONE
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
B4 implementation: NOT_STARTED
Stage-QDR-4 final close: NOT_STARTED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_FINAL_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

B4 implementation 只能建设 tenant-bound internal read model / report service。不得新增 API / Controller，除非 implementation 过程中停止并输出 `B4_API_REQUIRED_BLOCKER`，再转入单独 API freeze review。

## 2. Preconditions

下一轮 implementation 开工前必须确认 worktree clean、分支为 `dev`，且最近 history 包含以下 commit subject 或等价提交：

```text
feat(qdr): add replay evaluation domain contracts
docs(qdr): plan replay evaluation persistence baseline
docs(qdr): define replay evaluation persistence implementation work order
feat(qdr): persist replay evaluation baseline
docs(qdr): close replay evaluation persistence baseline
docs(qdr): define mock gateway regression integration work order
feat(qdr): integrate mock gateway regression baseline
docs(qdr): close mock gateway regression integration
docs(qdr): plan regression report read model support
docs(qdr): define regression report read model work order
```

若任一前置条件不满足，必须停止并输出：

```text
WORKTREE_NOT_CLEAN_BLOCKED
WRONG_BRANCH_BLOCKED
B4_PLAN_NOT_COMMITTED_BLOCKED
B4_IMPLEMENTATION_WO_NOT_COMMITTED_BLOCKED
```

## 3. Report / Read Model Target Boundary

后续 B4 implementation 的目标是 internal read model / report service，不是 API。必须支撑以下只读目标：

```text
1. Replay case read summary
2. Evaluation case read summary
3. Expected vs actual decision summary
4. Regression verdict read summary
5. Finding list
6. Drift summary
7. Provider/model/prompt/policy version refs
8. Redaction guard evidence
9. Trading-term guard evidence
10. Tenant-bound query support
```

报告服务只输出 read-only summary、safe refs、hash、version、verdict、finding 和 drift classification。不得执行 replay，不得调用 provider，不得发 HTTP，不得访问 NQ，不得生成 trading signal，不得触发 order / risk / ledger / paper / live mutation。

## 4. Allowed Structures

后续 implementation 允许新增或补齐以下结构。命名可按现有 `qdr/readmodel` 与 `qdr/replay` 代码风格微调；若需要 usecase port，只能新增 internal usecase port，不得新增 REST endpoint。

| 结构 | 允许职责 | 强制边界 |
| --- | --- | --- |
| `RegressionReportQuery` | 表达 tenant-bound 单条或列表查询条件，封装 selector、pagination、sort。 | `tenantId` 必填；禁止 UUID-only；pageSize 最大 100。 |
| `RegressionReportView` | 聚合 replay case、evaluation case、expected/actual summary、verdict、finding 与 drift summary。 | 只展示 safe fields；任一 cross-tenant mismatch fail-closed。 |
| `RegressionReportFindingView` | 展示脱敏 finding 行。 | `findingMessage` 与 `evidenceRef` 不得包含 raw payload、credential-like key 或 executable trading term。 |
| `RegressionDriftSummary` | 把 B3 comparator 结果转成只读 drift 分类。 | drift 只用于 regression report，不得变成交易建议。 |
| `RegressionReadModelService` | 编排 query validation、B2 repository 读取、view assembly 和 fail-closed 处理。 | usecase/internal only；不新增 API / Controller；repository failure 转 unavailable/fail-closed。 |

如果实现发现需要直接扩展 JDBC 查询能力，可新增 internal query port 或补齐现有 B2 repository 方法，但所有方法必须显式接收 `tenantId` 并保持分页上限。

## 5. Query Boundary

后续 implementation 必须支持以下 tenant-bound 查询：

```text
1. tenantId + caseId
2. tenantId + evaluationId
3. tenantId + verdictId
4. tenantId + createdAt
5. tenantId + verdict
6. tenantId + severity
7. tenantId + traceId
8. tenantId + sourceRequestId
9. tenantId + sourceDecisionId
```

必须禁止：

```text
1. UUID-only query
2. tenantless list
3. cross-tenant read
4. unbounded list
5. pageSize > 100 without reject/cap
```

分页规则：

```text
所有 list 查询必须包含 pageSize / offset 或等价 page request。
默认 pageSize 建议 50。
pageSize > 100 必须 rejected 或 capped；implementation 必须明确选择并测试。
排序默认 createdAt desc, id asc，避免同时间戳结果不稳定。
```

## 6. Report Content Boundary

允许展示：

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
order instruction
execution instruction
account secret
position mutation
ledger mutation
risk mutation
```

如果未来需要 report export，必须沿用同一内容边界，并显式拒绝包含 raw prompt、raw provider response、credential-like fields、order instruction 或 NQ mutation payload 的 export。

## 7. Drift Summary Boundary

后续 implementation 必须表达以下 drift：

```text
decisionType drift
actionLabel drift
confidenceBand drift
riskLevel drift
evidenceRefs drift
forbiddenActions drift
providerSummaryHash drift
modelGatewayVersionRef drift
promptVersionRef drift
policyVersion drift
```

建议分类：

| Drift item | classification | report behavior |
| --- | --- | --- |
| `decisionType` | `MATCH` / `MISMATCH` / `SKIPPED` | 只进入 drift/finding，不改变 downstream runtime。 |
| `actionLabel` | `MATCH` / `MISMATCH` / `FORBIDDEN` / `SKIPPED` | executable label 必须 fail-closed。 |
| `confidenceBand` | `MATCH` / `SOFT_DRIFT` / `HARD_DRIFT` / `SKIPPED` | 不推断仓位、数量或订单。 |
| `riskLevel` | `MATCH` / `SOFT_DRIFT` / `HARD_DRIFT` / `SKIPPED` | 不写 NQ risk fact，不触发 risk mutation。 |
| `evidenceRefs` | `MATCH` / `SUPERSET` / `MISSING` / `SKIPPED` | 只展示 safe ref/hash。 |
| `forbiddenActions` | `MATCH` / `EXTENDED` / `MISSING` | forbidden list 只能表示禁止动作。 |
| `providerSummaryHash` | `MATCH` / `DRIFT` / `SKIPPED` | 只展示 hash drift，不展示 provider response。 |
| `modelGatewayVersionRef` | `MATCH` / `DRIFT` / `SKIPPED` | 只展示 version ref。 |
| `promptVersionRef` | `MATCH` / `DRIFT` / `SKIPPED` | 只展示 prompt ref，不展示 prompt text。 |
| `policyVersion` | `MATCH` / `DRIFT` / `SKIPPED` | policy drift 不自动放宽安全边界。 |

固定解释规则：

```text
PASS / WARN / FAIL / SKIPPED 只是 regression verdict。
PASS / WARN / FAIL / SKIPPED 不得作为 trading signal。
LONG_BIAS / SHORT_BIAS 只能作为 direction label。
LONG_BIAS / SHORT_BIAS 不得映射 BUY / SELL。
BUY / SELL / MARKET_ORDER 不得作为 actionLabel。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 不得作为 allowed action。
```

## 8. Persistence Reuse Boundary

后续 implementation 必须复用 B2/V9 表与既有 B2 repository：

```text
qdr_replay_case
qdr_evaluation_case
qdr_expected_decision_summary
qdr_regression_verdict
qdr_regression_finding
qdr_replay_input_ref
qdr_replay_output_ref
```

禁止：

```text
1. 新增 V10。
2. 修改 V9。
3. 新增新表。
4. 绕过 B2 repository。
5. 使用 UUID-only 查询。
6. cross-tenant read。
```

如果发现 B2 schema 不足，必须停止并输出 blocker，不得偷偷改 schema：

```text
B4_SCHEMA_GAP_BLOCKER
```

## 9. API / Controller Decision

B4 implementation 默认只做 usecase/internal read model，不新增 API / Controller。

如果 implementation 过程中发现确实需要 API / Controller，必须停止并输出：

```text
B4_API_REQUIRED_BLOCKER
```

然后转入单独 API freeze review。不得在 B4 implementation 中直接新增 Controller、REST endpoint、OpenAPI path、JSON Schema 或 external contract。

## 10. B4 Implementation Test Matrix

后续 implementation 至少覆盖以下测试：

| 序号 | 测试项 | 验收标准 |
| --- | --- | --- |
| 1 | report query by tenantId + caseId succeeds | 返回同租户 replay/evaluation/verdict/report summary。 |
| 2 | report query by tenantId + evaluationId succeeds | 返回同租户 evaluation report。 |
| 3 | report query by tenantId + verdictId succeeds | 返回同租户 verdict report 与 finding list。 |
| 4 | tenantless query fails closed | 缺失 tenantId 拒绝，不查库。 |
| 5 | UUID-only query is absent or rejected | 不提供 UUID-only method；若传入仅 UUID 则拒绝。 |
| 6 | cross-tenant report read returns empty or fail-closed | tenant B 不能读取 tenant A report。 |
| 7 | list query is paginated | list by createdAt/verdict/severity/trace/request/decision 都显式分页。 |
| 8 | pageSize > 100 is rejected or capped | implementation 明确 rejected 或 capped 并测试。 |
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

## 11. Validation Command Boundary

后续 B4 implementation 必须执行：

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only

mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test

mvn -ntp -Pquality validate

.\mvnw.cmd -v
```

如果 `.\mvnw.cmd -v` 仍失败，必须记录：

```text
WRAPPER_UNUSABLE / P2 TOOLING RISK
```

不得写成 PASS。

## 12. Safety Scan Boundary

后续 B4 implementation 必须执行：

```powershell
rg -n "rawPrompt|raw_prompt|promptText|rawProviderResponse|raw_provider_response|providerRaw|credential|apiKey|apiSecret|passphrase|token|cookie|secret|BUY|SELL|MARKET_ORDER|PLACE_ORDER|CANCEL_ORDER|MUTATE_NQ_STATE|real HTTP|real provider|LangGraph|AutoGen|CrewAI|LIVE" dh-domain dh-usecase dh-infra dh-app docs/current
```

判定要求：

```text
sensitive terms 只能出现在禁止项、测试守卫、redaction guard、migration CHECK/COMMENT 或文档风险说明中。
BUY / SELL / MARKET_ORDER 只能出现在禁止项、forbiddenActions、CHECK、测试守卫或风险说明中。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 只能出现在禁止项、forbiddenActions、CHECK、测试守卫或风险说明中。
不得出现真实 HTTP/provider/Agent/LangGraph/LIVE 实现。
```

## 13. Review / Close Rule

B4 implementation review 规则：

```text
1. 如果只实现 usecase/internal read model，无 API、无 migration、无 security expansion，可 implementation 后 validation + commit，不做 standalone freeze。
2. 如果新增 API / Controller，必须先 API freeze review。
3. 如果新增 migration，必须先 migration freeze review。
4. 如果改动 tenant/security/redaction boundary，必须 close review。
5. B4 完成后进入 Stage-QDR-4 final close review。
```

禁止：

```text
1. B4 implementation 完成后直接打 tag。
2. B4 与 B5 合并。
3. B4 新增 API。
4. B4 接真实 provider / HTTP。
5. B4 接 Agent / LangGraph。
6. B4 修改 NQ。
```

Tag rule：

```text
stage-qdr-4 tag 只允许在 Stage-QDR-4 final close PASS 后创建。
B4 不是 final close，不允许打 tag。
```

## 14. Readiness Decision

```text
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_FINAL_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步：

```text
DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```
