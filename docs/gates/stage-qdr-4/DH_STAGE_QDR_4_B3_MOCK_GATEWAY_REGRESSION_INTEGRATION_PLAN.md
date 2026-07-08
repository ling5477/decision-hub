# DH stage-qdr-4 B3 Mock Gateway Regression Integration Plan

> current planning document for `DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN`
> planning only; no Java, test, migration, API, provider, HTTP, Agent, LangGraph, NQ or LIVE change

```text
Task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
Task type: PLANNING_ONLY + MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN + QDR_REPLAY_EVALUATION_REGRESSION + PIPELINE_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / PLAN_ONLY
Date: 2026-07-08
Repository: F:/project/decision-hub
Branch: dev
Precondition: STAGE_QDR_4_B2 / CLOSED / ACCEPTED
Next task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
```

## 1. 目标与硬边界

B3 的目标是规划如何把已存在的 dry-run / QDR decision artifact、deterministic mock model gateway、安全 prompt/model/provider refs、B2 replay/evaluation persistence baseline 串成可回归的 mock gateway regression flow。

目标链路固定为：

```text
existing dry-run / QDR decision artifact
  -> mock model gateway summary
  -> replay case
  -> evaluation case
  -> expected decision summary
  -> regression comparison
  -> regression verdict
  -> finding list
```

本轮只做规划：

```text
不修改 Java 生产代码
不修改 Java 测试代码
不新增 migration
不修改 V1-V9 migration
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
不直接进入 B3 implementation
```

## 2. 现有基线

已完成基线：

```text
B1: ReplayCase / EvaluationCase / ExpectedDecisionSummary / EvaluationPolicy / RegressionVerdict / RegressionFinding domain contracts
B2: V9 replay/evaluation/regression persistence baseline, tenant-bound repository ports and JDBC adapters
B2 close review: PASS
B2 status: CLOSED / ACCEPTED
```

现有 QDR mock gateway 基线：

```text
DefaultQdrMockModelGatewayBaseline: deterministic prompt/model/provider refs, no env, no secret, no HTTP
MockModelProvider: deterministic local provider, no HTTP, no SDK, no NQ, no trading
DefaultQdrModelGatewayIntegrationService: writes safe gateway call metadata, V5 trace, audit refs; all gateway/persistence/trace/audit failures fail-closed
QdrModelGatewayIntegrationResult: exposes promptVersionId, modelVersionId, providerProfileId, gatewayCallRef, trustDecision, redactionStatus, budgetSummary, redactedSummary, traceRef, auditRef
DecisionDryRunSnapshot: read-only snapshot; action limited to OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
ReplayPersistenceGuard: rejects raw prompt/provider response/credential-like JSON key and executable trading action outside forbiddenActions
```

当前缺口：

```text
缺少 mock gateway safe refs -> replay/evaluation/regression persistence 的 usecase 编排。
缺少 regression comparison policy，把 expected summary 与 actual mock gateway summary 转成 PASS/WARN/FAIL/SKIPPED。
缺少 providerSummaryHash、modelGatewayVersionRef、promptVersionRef、policyVersion drift 的结构化 finding。
缺少 B3 implementation work order 的明确文件范围、测试矩阵和 close review 门禁。
```

## 3. Target Flow

### 3.1 输入来源

B3 implementation 只能从已存在的安全对象或安全 refs 组装 regression case：

```text
tenantId
traceId
requestId
decisionId / decisionRunId
DecisionDryRunSnapshot safe fields
QdrModelGatewayIntegrationResult safe refs
ReplayInputRef / ReplayOutputRef safe refs
ExpectedDecisionSummary expected baseline
EvaluationPolicy / policyVersion
```

禁止输入：

```text
raw prompt
rendered prompt 原文
raw provider response
credential / token / cookie / apiKey / apiSecret / passphrase
未脱敏 header / body / signature
交易订单、账户、账本、风控 mutation payload
真实 provider response
真实 HTTP response
NQ DB row
```

### 3.2 Flow Steps

```text
1. MockGatewayRegressionCaseBuilder 从 dry-run snapshot + gateway safe refs 构建 ReplayCase。
2. Builder 写入或复用 qdr_replay_input_ref、qdr_expected_decision_summary、qdr_replay_case。
3. QdrRegressionEvaluationService 调用 deterministic mock gateway result 或接收已存在 safe gateway result，不重新接 provider/HTTP。
4. Service 构建 ReplayOutputRef 与 actual ExpectedDecisionSummary(summary_role=ACTUAL)。
5. Service 构建 EvaluationCase，绑定 replay case、expected summary、actual summary、input/output refs。
6. QdrRegressionComparator 按 RegressionBaselinePolicy 比较 expected 与 actual。
7. Comparator 输出 RegressionVerdict 与 RegressionEvidenceRef finding 列表。
8. Service 保存 qdr_regression_verdict 与 qdr_regression_finding。
9. 任何 validation、guard、repository、serialization、persistence 失败均 fail-closed，不 fallback success。
```

不做：

```text
不调用 replay execution API
不新增 Controller
不新增 REST endpoint
不调用真实 provider
不发真实 HTTP
不调用 NQ
不启动 Agent / LangGraph
不生成 trading signal
```

## 4. Usecase Boundaries

以下结构是 B3 implementation 规划对象，本轮不创建代码。

| 用例 / 对象 | 输入 | 输出 | tenantId 要求 | traceId / requestId / decisionId 要求 | fail-closed 行为 | redaction 规则 | provider / HTTP / NQ / trading |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `MockGatewayRegressionCaseBuilder` | `DecisionDryRunSnapshot`、`QdrModelGatewayIntegrationResult`、expected summary、policy | `ReplayCase`、`ReplayInputRef`、expected summary save command | required；所有 nested refs 必须同 tenant | `traceId`、`requestId`、`decisionId` / `decisionRunId` required | 缺字段、跨 tenant、unsafe ref、forbidden action 直接拒绝 | 只保存 hash/ref/redacted summary；拒绝 raw prompt/provider/credential key | provider NO；HTTP NO；NQ NO；trading signal NO |
| `QdrRegressionEvaluationService` | replay case、gateway result safe refs、repositories、policy | saved evaluation case、regression verdict、finding list | 所有 save/find/list 显式 tenant-bound | 每次 evaluation 必须带 traceId/requestId/evaluationId/verdictId | repository/gateway safe-ref/serialization 失败均转 FAIL 或 BLOCKED，不成功返回 | 复用 `ReplayPersistenceGuard.rejectUnsafeJson`；finding message 必须脱敏 | provider NO；HTTP NO；NQ NO；trading signal NO |
| `QdrRegressionComparator` | expected summary、actual summary、baseline policy、evidence refs | `RegressionVerdict` + finding candidates | 不直接查库，但输入必须来自同 tenant service | comparison finding 必须携带 traceId/requestId/evaluationId/verdictId 上下文 | 缺 expected/actual、非法 action、raw material 命中为 FAIL | 不读取 raw provider response；不比较 raw prompt；只比较 structured fields/hash/ref | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionBaselinePolicy` | policyVersion、confidence tolerance、risk tolerance、requiredEvidenceMode、strict refs flags | immutable comparison policy | policy 按 tenant + policyVersion 选择；不得跨 tenant 复用动态状态 | policyVersion required；缺失为 FAIL/BLOCKED | unsupported policyVersion 或 raw provider response mode 直接 BLOCKED | policy 不允许 `RAW_PROVIDER_RESPONSE` mode | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionEvidenceRef` | findingCode、severity、fieldName、expectedRef、actualRef、evidenceRef | safe finding ref / message | tenant 在 service 层绑定；对象不得脱离 evaluation context 持久化 | 必须可回溯到 evaluationId/verdictId | unsafe message、raw material、可执行交易术语命中即拒绝 | 只允许 safe ref、hash、field name、short reason，不允许原始 payload | provider NO；HTTP NO；NQ NO；trading signal NO |

## 5. Regression Comparison Rules

B3 默认使用 strict-safe policy。任何 policy 放宽都必须在 B3 WO 中列明，并在 implementation close review 中逐项验证。

| Comparison item | PASS | WARN | FAIL | SKIPPED |
| --- | --- | --- | --- | --- |
| `decisionType` | expected 与 actual 完全一致，且为 read-only 类型 | 无默认 WARN；如后续 policy 明确 alias 关系，可记录 WARN | actual 缺失、不是 read-only、或含 executable/trading 语义 | gateway fail-closed 且 policy 明确不生成 actual summary |
| `actionLabel` | 完全一致，且属于 `OBSERVE` / `NO_TRADE` / `LONG_BIAS` / `SHORT_BIAS` / `NEEDS_REVIEW` / `REJECTED` | `OBSERVE` 与 `NO_TRADE` 可在 policy 明确允许时 WARN | expected/actual 为 `BUY` / `SELL` / `MARKET_ORDER` / `PLACE_ORDER` / `CANCEL_ORDER` / `MUTATE_NQ_STATE`，或不在允许集合 | actual summary 未生成且 gateway failure 已记录 |
| `confidenceBand` | 完全一致，或 numeric confidence 落在 policy tolerance 内后归同一 band | confidence drift 超出 soft tolerance 但不影响 action/risk | confidence 缺失、不可解析、超 hard tolerance 或导致 action/risk 判定冲突 | policy 不比较 confidence 且记录 reason |
| `riskLevel` | 完全一致，或 risk rank drift <= policy.riskLevelTolerance | risk drift 在 soft tolerance 内但需要人工复核 | actual `BLOCKED`/`UNKNOWN` 与 expected 不一致，或 drift 超 hard tolerance | policy 明确跳过 risk comparison |
| `evidenceRefs` | expected refs 全部存在；STRICT 下集合完全一致，ALLOW_SUPERSET 下 actual 为安全超集 | actual 有额外 safe refs | 缺少 required evidence ref、出现 unsafe ref 或 raw payload marker | policy 明确 provider summary only 且 required refs 由 summary hash 替代 |
| `forbiddenActions` | actual 包含所有 expected forbidden actions，且 forbidden list 只表示禁止项 | actual 额外包含新的 forbidden action | missing required forbidden action；或把 forbidden action 写为 allowed/actionLabel | no actual summary and failure recorded |
| `providerSummaryHash` | expected 与 actual hash 完全一致 | hash 不一致但 structured summary 字段一致，记录 drift | hash 不一致且 summary 字段不一致，或 hash 缺失/非法 | provider summary not produced because gateway failed closed |
| `modelGatewayVersionRef` | 完全一致 | ref drift 但 prompt/model/policy 都一致，记录 version drift | policy 要求 strict gateway ref 而发生 drift，或 ref 缺失 | no gateway result |
| `promptVersionRef` | 完全一致 | ref drift 但 template hash 与 expected summary 一致，记录 prompt drift | strict policy 下 drift、缺失或 checksum mismatch | no prompt ref because baseline unavailable |
| `policyVersion` | 完全一致 | 无默认 WARN；policy drift 必须记录 | policyVersion 缺失、未知、或 expected/actual 不一致 | policy 明确标记该 case 不适用 |

交易术语固定规则：

```text
BUY / SELL / MARKET_ORDER 不能作为 expected actionLabel。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 不能作为 allowed action。
LONG_BIAS / SHORT_BIAS 只能作为 direction label，不得映射为 BUY / SELL。
RegressionVerdict 只表示 replay/evaluation result，不表示交易建议。
```

## 6. Persistence Reuse Rules

B3 不新增 migration，不新增 V10，不修改 V9。必须复用 B2 的 7 张表：

```text
qdr_replay_case
qdr_evaluation_case
qdr_expected_decision_summary
qdr_regression_verdict
qdr_regression_finding
qdr_replay_input_ref
qdr_replay_output_ref
```

映射规则：

| Flow object | Reused table | 说明 |
| --- | --- | --- |
| replay input safe ref | `qdr_replay_input_ref` | 只保存 structured ref/hash，不保存 raw input。 |
| expected summary | `qdr_expected_decision_summary` | `summary_role = EXPECTED`。 |
| replay case | `qdr_replay_case` | 绑定 input ref 与 expected summary。 |
| gateway output safe ref | `qdr_replay_output_ref` | 只保存 gateway summary ref/hash，不保存 provider raw response。 |
| actual summary | `qdr_expected_decision_summary` | `summary_role = ACTUAL`，绑定 output ref。 |
| evaluation case | `qdr_evaluation_case` | 绑定 replay case、expected/actual summary、policy/version refs。 |
| regression verdict | `qdr_regression_verdict` | 保存 PASS/WARN/FAIL/SKIPPED，非交易建议。 |
| finding list | `qdr_regression_finding` | 保存结构化 drift/failure finding。 |

如 B3 implementation 发现 V9 schema 不足：

```text
立即输出 blocker。
不得修改 V9。
不得新增 V10。
不得通过 JSONB 塞入 raw 或非计划字段绕过 schema。
后续需另起 migration review/freeze 任务。
```

## 7. Redaction Rules

B3 implementation 必须继续复用或等价覆盖以下规则：

```text
raw prompt 不可入库
rendered prompt 原文不可入库
raw provider response 不可入库
credential / token / cookie / apiKey / apiSecret / passphrase 不可入库
JSONB key guard 覆盖 top-level 和 nested sensitive key
finding message 不得回显原始 payload
error message 使用固定安全摘要
providerSummaryHash 只保存 hash，不保存 provider response
```

允许保存：

```text
safe ref
hash
redacted summary
policyVersion
modelGatewayVersionRef
promptVersionRef
modelVersionRef
providerProfileId / provider identity ref without secret
trust decision ref
budget summary
finding code / severity / redacted message
```

## 8. Test Matrix For B3 Implementation

B3 implementation 必须规划并覆盖以下测试，本轮不实现：

| 序号 | 测试项 | 计划验收 |
| --- | --- | --- |
| 1 | mock gateway output can create replay case | gateway safe refs 能生成并保存 replay case。 |
| 2 | replay case can create evaluation case | replay case + actual summary/output ref 能生成 evaluation case。 |
| 3 | expected summary can create regression verdict | expected/actual summary comparison 能保存 verdict。 |
| 4 | identical mock output returns PASS | deterministic output 与 baseline 完全一致时 `PASS`。 |
| 5 | confidence drift beyond tolerance returns WARN or FAIL | soft drift `WARN`，hard drift `FAIL`。 |
| 6 | risk level drift returns WARN or FAIL | policy tolerance 内 `WARN`，越界 `FAIL`。 |
| 7 | missing evidence ref returns FAIL | required evidence 缺失 fail-closed。 |
| 8 | provider summary hash mismatch returns WARN or FAIL | 结构化字段一致为 `WARN`，语义不一致为 `FAIL`。 |
| 9 | modelGatewayVersionRef mismatch is recorded | drift 必须生成 finding，不静默忽略。 |
| 10 | promptVersionRef mismatch is recorded | drift 必须生成 finding，不静默忽略。 |
| 11 | policyVersion mismatch is recorded | strict default 下为 `FAIL` 或 `BLOCKED` finding。 |
| 12 | BUY / SELL / MARKET_ORDER expected action fails closed | 不允许作为 expected actionLabel。 |
| 13 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE allowed action fails closed | 不允许作为 allowed action。 |
| 14 | raw prompt is not persisted | raw prompt sample 被 guard 拒绝或仅 hash/ref。 |
| 15 | raw provider response is not persisted | raw provider response sample 被 guard 拒绝或仅 hash/ref。 |
| 16 | credential-like JSON key fails closed | sensitive key 命中 fail-closed。 |
| 17 | cross-tenant regression read returns empty or fail-closed | tenant B 不得读 tenant A case/evaluation/verdict/finding。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | architecture/safety scan 证明无新增真实 provider/HTTP/Agent/LangGraph。 |
| 19 | repository save failure returns FAIL / BLOCKED | persistence failure 不吞异常，不 fallback success。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |

## 9. Review / Freeze Rules

B3 顺序固定：

```text
1. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
2. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
3. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
4. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW
```

不允许：

```text
B3 plan 后直接 implementation
B3 与 B4 合并
B3 新增 API
B3 接真实 provider / HTTP
B3 接 Agent / LangGraph
B3 修改 NQ
B3 新增 migration 或修改 V9
```

B3 WO 必须再次明确：

```text
target files
excluded files
expected implementation output
usecase/service/repository wiring boundary
test matrix
validation commands
rollback / blocker strategy
no real provider / no HTTP / no Agent / no LangGraph / no LIVE
```

## 10. B4 后置规则

B4 read model / report 仍后置。B3 不输出 reporting API，不新增 Controller，不新增 OpenAPI，不把 regression verdict 暴露为 trading signal。若 B4 需要 read model/report/API，必须另起 plan / WO / implementation / close review，并重新审查 tenant、auth、redaction、pagination、read-only boundary。

## 11. Readiness Decision

```text
STAGE_QDR_4_B3_PLAN: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步：

```text
DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
```
