# DH stage-qdr-4 B3 Mock Gateway Regression Integration Work Order

> current work order for `DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO`
> work order only; no Java, test, migration, API, provider, HTTP, Agent, LangGraph, NQ or LIVE change in this round

```text
Task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
Task type: WORK_ORDER_ONLY + B3_IMPLEMENTATION_BOUNDARY_DESIGN + MOCK_GATEWAY_REGRESSION_WO + QDR_REPLAY_EVALUATION_REGRESSION + PIPELINE_INTEGRATION_WO + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / WORK_ORDER_ONLY
Date: 2026-07-08
Repository: F:/project/decision-hub
Branch: dev
Precondition: STAGE_QDR_4_B3_PLAN / DONE
Next task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
```

## 1. Work Order Scope

本工单只冻结 B3 implementation 的边界和验收要求，不实现代码。

固定目标链路：

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

本工单确认：

```text
STAGE_QDR_4_B1: DONE
STAGE_QDR_4_B2: CLOSED / ACCEPTED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3_PLAN: DONE
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
B3 implementation: NOT_STARTED
B4 implementation: NOT_STARTED
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

B3 implementation 只能使用 deterministic mock gateway / local output。不得调用真实 provider，不得发真实 HTTP，不得调用 NQ，不得执行 replay API，不得新增 Controller，不得启动 Agent / LangGraph，不得生成交易信号。

## 2. Preconditions

必须已存在以下提交或等价当前 history 证据：

```text
feat(qdr): add replay evaluation domain contracts
docs(qdr): plan replay evaluation persistence baseline
docs(qdr): define replay evaluation persistence implementation work order
feat(qdr): persist replay evaluation baseline
docs(qdr): close replay evaluation persistence baseline
docs(qdr): plan mock gateway regression integration
```

若下一轮 implementation 开工前任一条件不满足，必须停止并输出对应 blocker：

```text
WORKTREE_NOT_CLEAN_BLOCKED
WRONG_BRANCH_BLOCKED
B3_WO_DOC_MISSING_BLOCKED
B2_CLOSE_REVIEW_NOT_COMMITTED_BLOCKED
```

## 3. Actual Package Reality

附件中的 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/modelgateway/**` 与 `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/provider/**` 在当前仓库中不存在。B3 implementation 必须以当前实际 QDR 路径为准：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/**
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
```

现有可复用对象：

```text
DecisionDryRunSnapshot
QdrModelGatewayIntegrationResult
DefaultQdrMockModelGatewayBaseline
MockModelProvider
DefaultQdrModelGatewayIntegrationService
ReplayEvaluationContractService
ReplayPersistenceGuard
ReplayCaseRepository
EvaluationCaseRepository
RegressionVerdictRepository
```

## 4. Implementation Target Files

下一轮 B3 implementation 允许新增或修改的最小范围必须优先落在：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/test/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
docs/current/**
```

只有当 wiring 或现有 mock gateway safe-ref 适配不可避免时，才允许极小范围读取或修改：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-app/src/test/java/**
```

下一轮 B3 implementation 仍禁止：

```text
dh-app/src/main/resources/db/migration/**
dh-api/**
Controller / REST endpoint
真实 HTTP client
真实 provider client / Provider SDK
OpenAI / Anthropic / Gemini / Ollama SDK
LangGraph / AutoGen / CrewAI
NQ 仓库
contracts / golden_cases，除非用户另行授权
```

## 5. Required Usecase Boundaries

以下结构是 B3 implementation 的允许目标或等价结构。实现时必须保持 usecase-only orchestration，不新增 persistence schema，不新增 REST API。

| 用例 / 对象 | 输入 | 输出 | tenantId 要求 | traceId / requestId / decisionId 要求 | fail-closed 行为 | redaction 规则 | provider / HTTP / NQ / trading |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `MockGatewayRegressionCaseBuilder` | `DecisionDryRunSnapshot`、`QdrModelGatewayIntegrationResult` safe refs、expected summary、`RegressionBaselinePolicy` | `ReplayCase`、`ReplayInputRef`、expected summary save command | required；snapshot、gateway result、expected summary 必须同 tenant | `traceId`、`requestId`、`decisionId` 或 `decisionRunId` required | 缺字段、跨 tenant、unsafe ref、forbidden action 直接拒绝 | 只生成 hash/ref/redacted summary；拒绝 raw prompt、raw provider response、credential-like key | provider NO；HTTP NO；NQ NO；trading signal NO |
| `QdrRegressionEvaluationService` | replay case、gateway safe result、B2 repositories、baseline policy | saved evaluation case、regression verdict、finding list | 所有 repository 调用显式 tenant-bound | evaluation/verdict/finding 必须保留 traceId、requestId、decisionId/evaluationId | repository save failure、serialization failure、policy failure 转 `FAIL` 或 `BLOCKED` finding | 复用 `ReplayPersistenceGuard`；finding message 不含 raw payload | provider NO；HTTP NO；NQ NO；trading signal NO |
| `QdrRegressionComparator` | expected summary、actual summary、baseline policy、safe evidence refs | verdict candidate + finding candidates | 不直接查库；输入必须由 service 校验同 tenant | finding 必须可回溯到 traceId、requestId、evaluationId、verdictId | 缺 expected/actual、非法 action、unsafe evidence 直接 `FAIL` | 不读取 raw prompt/provider response，只比较 structured field/hash/ref | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionBaselinePolicy` | policyVersion、confidence tolerance、risk tolerance、requiredEvidenceMode、strict ref flags | immutable comparison policy | policy 按 tenant + policyVersion 选择或显式传入；不得跨 tenant 复用动态状态 | policyVersion required | unknown policyVersion、unsupported mode、missing policyVersion 转 `FAIL` 或 `BLOCKED` | 不提供 raw response mode；只允许 structured/hash/ref mode | provider NO；HTTP NO；NQ NO；trading signal NO |
| `RegressionEvidenceRef` | findingCode、severity、fieldName、expectedRef、actualRef、evidenceRef | safe finding metadata | tenant 在 service 层绑定；不得脱离 evaluation context 持久化 | 必须绑定 evaluationId/verdictId | unsafe message、raw material、可执行交易术语命中即拒绝 | 只允许 safe ref/hash/field/reason；不保存原始 payload | provider NO；HTTP NO；NQ NO；trading signal NO |

## 6. Regression Comparison Rules

比较结果只允许：

```text
PASS
WARN
FAIL
SKIPPED
```

默认 policy 为 strict-safe。各字段规则如下：

| Comparison item | PASS | WARN | FAIL | SKIPPED |
| --- | --- | --- | --- | --- |
| `decisionType` | expected 与 actual 完全一致，且为 read-only 类型 | policy 明确 alias 且不影响 downstream verdict | actual 缺失、非 read-only、或含 executable/trading 语义 | gateway fail-closed 且 policy 明确不生成 actual summary |
| `actionLabel` | 完全一致，且属于 `OBSERVE` / `NO_TRADE` / `LONG_BIAS` / `SHORT_BIAS` / `NEEDS_REVIEW` / `REJECTED` | `OBSERVE` 与 `NO_TRADE` 仅在 policy 明确允许时 WARN | expected/actual 为 `BUY` / `SELL` / `MARKET_ORDER` / `PLACE_ORDER` / `CANCEL_ORDER` / `MUTATE_NQ_STATE`，或不在允许集合 | actual summary 未生成且 gateway failure 已记录 |
| `confidenceBand` | 完全一致，或 numeric confidence 落在 tolerance 内后归同一 band | drift 超 soft tolerance 但不改变 action/risk | 缺失、不可解析、超 hard tolerance 或导致 action/risk 冲突 | policy 明确跳过 confidence comparison |
| `riskLevel` | 完全一致，或 risk rank drift <= tolerance | soft drift，需要人工复核 | `BLOCKED` / `UNKNOWN` 与 expected 不一致，或 drift 超 hard tolerance | policy 明确跳过 risk comparison |
| `evidenceRefs` | STRICT 下集合一致；ALLOW_SUPERSET 下 actual 为 safe superset | actual 有额外 safe refs | 缺 required evidence ref，出现 unsafe ref 或 raw payload marker | policy 明确用 provider summary hash 替代 required refs |
| `forbiddenActions` | actual 包含所有 expected forbidden actions，且 forbidden list 只表示禁止项 | actual 额外包含新的 forbidden action | missing required forbidden action，或把 forbidden action 写为 allowed/actionLabel | no actual summary and failure recorded |
| `providerSummaryHash` | expected 与 actual hash 完全一致 | hash drift 但 structured summary fields 一致 | hash drift 且 summary fields 不一致，或 hash 缺失/非法 | provider summary not produced because gateway failed closed |
| `modelGatewayVersionRef` | 完全一致 | ref drift 但 prompt/model/policy 都一致，记录 finding | strict policy 下 drift，或 ref 缺失 | no gateway result |
| `promptVersionRef` | 完全一致 | ref drift 但 template hash 与 expected summary 一致 | strict policy 下 drift、缺失或 checksum mismatch | no prompt ref because baseline unavailable |
| `policyVersion` | 完全一致 | 无默认 WARN；drift 必须记录 | 缺失、未知、expected/actual 不一致 | policy 明确标记 case not applicable |

交易术语固定规则：

```text
BUY / SELL / MARKET_ORDER 不能作为 expected actionLabel。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 不能作为 allowed action。
LONG_BIAS / SHORT_BIAS 只能作为 direction label，不得映射为 BUY / SELL。
RegressionVerdict 只表示 replay/evaluation result，不表示交易建议。
```

## 7. Persistence Reuse

B3 implementation 不新增 migration，不新增 V10，不修改 V9。必须复用 B2 的 7 张表：

```text
qdr_replay_case
qdr_evaluation_case
qdr_expected_decision_summary
qdr_regression_verdict
qdr_regression_finding
qdr_replay_input_ref
qdr_replay_output_ref
```

复用要求：

```text
ReplayInputRef: safe ref/hash only
ReplayOutputRef: gateway summary safe ref/hash only
ExpectedDecisionSummary: EXPECTED 与 ACTUAL summary roles
ReplayCase: tenant-bound case + expected summary + input ref
EvaluationCase: tenant-bound evaluation + expected/actual summaries + output ref
RegressionVerdict: PASS/WARN/FAIL/SKIPPED only, not trading signal
RegressionFinding: structured drift/failure finding only
```

如果 implementation 发现 B2 schema 不足：

```text
立即输出 blocker。
不得修改 V9。
不得新增 V10。
不得新增新表。
不得通过 JSONB 塞入 raw payload 或非计划字段绕过 schema。
后续必须另起 migration review/freeze 任务。
```

## 8. Redaction And Safety Rules

B3 implementation 必须复用或等价执行 B2 redaction guard：

```text
no raw prompt storage
no raw provider response storage
no credential storage
no token / cookie / apiKey / apiSecret / passphrase / secret key
no unredacted header/body/signature
no gateway-result-as-trading-signal
```

允许持久化的只有：

```text
safe ref
content hash
providerSummaryHash
modelGatewayVersionRef
promptVersionRef
policyVersion
redacted summary
structured finding code / severity / field
traceId / requestId / decisionId / evaluationId / verdictId
```

所有 failures 必须 fail-closed。

## 9. B3 Implementation Test Matrix

下一轮 implementation 必须覆盖以下测试，不得在本工单实现：

| 序号 | 测试项 | 期望 |
| --- | --- | --- |
| 1 | mock gateway output can create replay case | gateway safe refs 能生成 replay case。 |
| 2 | replay case can create evaluation case | replay case 能生成 evaluation case。 |
| 3 | expected summary can create regression verdict | expected/actual summary 能生成 verdict。 |
| 4 | identical mock output returns PASS | deterministic output 完全一致时 `PASS`。 |
| 5 | confidence drift beyond tolerance returns WARN or FAIL | soft drift `WARN`，hard drift `FAIL`。 |
| 6 | risk level drift returns WARN or FAIL | tolerance 内 `WARN`，越界 `FAIL`。 |
| 7 | missing evidence ref returns FAIL | required evidence 缺失 fail-closed。 |
| 8 | provider summary hash mismatch returns WARN or FAIL | structured fields 一致为 `WARN`，不一致为 `FAIL`。 |
| 9 | modelGatewayVersionRef mismatch is recorded | version drift 必须生成 finding。 |
| 10 | promptVersionRef mismatch is recorded | prompt drift 必须生成 finding。 |
| 11 | policyVersion mismatch is recorded | strict default 下为 `FAIL` 或 `BLOCKED` finding。 |
| 12 | BUY / SELL / MARKET_ORDER expected action fails closed | 不允许作为 expected actionLabel。 |
| 13 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE allowed action fails closed | 不允许作为 allowed action。 |
| 14 | raw prompt is not persisted | raw prompt sample 被 guard 拒绝或仅 hash/ref。 |
| 15 | raw provider response is not persisted | raw provider response sample 被 guard 拒绝或仅 hash/ref。 |
| 16 | credential-like JSON key fails closed | sensitive key 命中 fail-closed。 |
| 17 | cross-tenant regression read returns empty or fail-closed | tenant B 不得读 tenant A output。 |
| 18 | provider/HTTP/Agent/LangGraph classes are not introduced | architecture/safety scan 证明无新增真实 provider/HTTP/Agent/LangGraph。 |
| 19 | repository save failure returns FAIL / BLOCKED | persistence failure 不 fallback success。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |
| 21 | replay/regression output cannot become trading signal | verdict/finding 不生成 NQ command、order、risk、ledger、paper/live mutation。 |
| 22 | B2 schema is reused without migration drift | no V10、no V9 modification、no new table。 |

## 10. Validation Required For Implementation

下一轮 B3 implementation 收口必须执行：

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

mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

如果 `.\mvnw.cmd -v` 仍失败或输出 wrapper 破损症状，只能记录：

```text
WRAPPER_UNUSABLE / P2 TOOLING RISK
```

不得写成 wrapper PASS。

## 11. Safety Scan

下一轮 B3 implementation 和 close review 必须执行：

```powershell
rg -n "raw prompt.*allowed|raw provider response.*allowed|credential.*allowed|BUY|SELL|MARKET_ORDER|PLACE_ORDER|CANCEL_ORDER|MUTATE_NQ_STATE|real HTTP.*YES|real provider.*YES|LangGraph.*STARTED|Agent.*STARTED|LIVE.*ENABLED|B3 implementation.*STARTED" README.md docs/current
```

要求：

```text
不得把 B3 implementation 写成 started。
不得把 real provider / HTTP / Agent / LangGraph / LIVE 写成 started/enabled。
sensitive-data permissive statement 不得出现。
若 FACTSOURCE_POLICY.md 既有 hard-error phrase 命中，按 non-blocking P2 residual 记录。
若 NOT_STARTED 因正则命中 STARTED，按 false positive 解释，不得改成 STARTED。
```

## 12. Review And Freeze Order

B3 固定顺序：

```text
1. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN
2. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
3. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
4. DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW
```

不允许：

```text
B3 WO 后跳过 implementation validation。
B3 implementation 后直接进入 B4。
B3 与 B4 合并。
B3 新增 API。
B3 接真实 provider / HTTP。
B3 接 Agent / LangGraph。
B3 修改 NQ。
```

## 13. Readiness Decision

```text
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next task: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-IMPLEMENTATION
```

## 14. Rollback And Recovery

如果下一轮 implementation 发现边界无法满足：

```text
输出 B3_IMPLEMENTATION_BLOCKED。
保留已通过的 docs/current WO 状态。
不得修改 V9/V10。
不得开启 provider/HTTP/NQ/Agent/LangGraph/LIVE。
不得用 git reset --hard 清理用户改动。
如需回滚本工单文档，只允许通过普通 diff revert 或后续用户授权 commit revert。
```
