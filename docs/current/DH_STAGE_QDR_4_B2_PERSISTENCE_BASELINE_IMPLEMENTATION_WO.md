# DH stage-qdr-4 B2 Persistence Baseline Implementation Work Order

> current work order for `DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION`
> work order only; no Java, test, SQL migration, API, provider, HTTP, Agent, LangGraph, NQ or LIVE change in this document task

```text
Task: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION-WO
Task type: WORK_ORDER_ONLY + B2_IMPLEMENTATION_BOUNDARY_DESIGN + MIGRATION_IMPLEMENTATION_WO + REPOSITORY_IMPLEMENTATION_WO + TEST_MATRIX_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / WORK_ORDER_ONLY
Date: 2026-07-08
Repository: F:/project/decision-hub
Branch: dev
Precondition: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-FREEZE-REVIEW / PASS
Next task: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION
```

## 1. 目标与硬边界

B2 implementation 的目标是把 B1 replay / evaluation domain contracts 落到 DH 本仓持久化基线，并补齐 tenant-bound repository、redaction guard、migration/repository 回归测试和 close review 证据。

本 work order 只授权下一轮 implementation，不在本轮实现。

```text
本轮 WO 不修改 Java 生产代码
本轮 WO 不修改 Java 测试代码
本轮 WO 不新增 migration
本轮 WO 不修改 V1-V8 migration
本轮 WO 不新增 V9 migration
本轮 WO 不新增 Repository / JDBC / Service 实现
本轮 WO 不新增 API / Controller
本轮 WO 不新增真实 HTTP client
本轮 WO 不新增真实 provider / Provider SDK
本轮 WO 不接 OpenAI / Anthropic / Gemini / Ollama SDK
本轮 WO 不接 LangGraph / AutoGen / CrewAI
本轮 WO 不启动 Agent runtime
本轮 WO 不修改 NQ
本轮 WO 不读取 credential / token / cookie / apiKey / apiSecret / passphrase
本轮 WO 不持久化 raw prompt
本轮 WO 不持久化 raw provider response
本轮 WO 不触碰交易、订单、撤单、账户、ledger mutation、risk mutation、paper mutation、live mutation
本轮 WO 不把 replay / regression output 写成 trading signal
本轮 WO 不启用 LIVE
```

## 2. Migration implementation boundary

下一轮 B2 implementation 允许新增且仅允许新增以下 migration：

```text
dh-app/src/main/resources/db/migration/V9__qdr_replay_evaluation_baseline.sql
```

V9 必须覆盖 7 张表：

```text
qdr_replay_case
qdr_evaluation_case
qdr_expected_decision_summary
qdr_regression_verdict
qdr_regression_finding
qdr_replay_input_ref
qdr_replay_output_ref
```

V9 DDL 强制要求：

```text
所有主表必须包含 tenant_id。
所有主查询索引必须 tenant-bound。
禁止 UUID-only 查询设计。
禁止 cross-tenant read。
verdict / severity / action_label 必须有 CHECK constraint 或等价强约束。
forbidden action / trading action 必须受约束。
raw prompt / raw provider response / credential 不得出现任何字段名。
JSONB 只能用于结构化 summary/ref，不能作为原文存储桶。
COMMENT ON TABLE / COMMENT ON COLUMN 必须覆盖敏感数据禁止项和 trading-signal 禁止项。
```

FK 策略：

```text
implementation 中必须最终确认 FK 是否会形成循环。
如果 FK 不形成循环，可使用 B2 内部 tenant-bound FK。
如果 FK 会形成循环或过度耦合 V6/V8，则不强加循环 FK。
不使用 FK 时，必须用 tenant-bound unique ref + repository-level consistency 保证引用一致性。
不得对 V6/V8 历史 source 表做未经 review 的强 FK 绑定。
```

## 3. Repository implementation boundary

下一轮 B2 implementation 允许新增或修改的代码范围：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/**
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/**
dh-usecase/src/test/java/**/qdr/**
dh-infra/src/test/java/**/qdr/**
```

允许实现的 repository / JDBC adapter：

```text
ReplayCaseRepository
EvaluationCaseRepository
RegressionVerdictRepository
JdbcReplayCaseRepository
JdbcEvaluationCaseRepository
JdbcRegressionVerdictRepository
```

Repository 方法强制要求：

```text
所有 save / find / list / update 方法显式接收 tenantId。
禁止 findById(id) 这类 UUID-only 方法。
查询方法必须 tenant-bound。
list 方法必须分页，默认 pageSize <= 50，最大 pageSize <= 100。
duplicate case_id 行为必须 deterministic：same checksum 幂等；different checksum fail-closed。
duplicate evaluation_id 行为必须 deterministic：same checksum 幂等；different checksum fail-closed。
cross-tenant read 必须返回 empty 或 fail-closed。
save 失败必须 fail-closed，不允许静默吞异常。
repository 层不得执行 replay。
repository 层不得接 provider。
repository 层不得生成 trading signal。
```

## 4. Domain / mapping boundary

下一轮 implementation 只能映射 B1 已有 domain contracts，不得为了 persistence 重新定义交易语义。

必须保留并映射：

```text
ReplayCase
EvaluationCase
ExpectedDecisionSummary
RegressionVerdict
ReplayInputRef
ReplayOutputRef
EvaluationPolicy
RegressionFinding
RegressionSeverity
```

语义边界：

```text
LONG_BIAS / SHORT_BIAS 只能是 direction label。
LONG_BIAS / SHORT_BIAS 不能映射 BUY / SELL。
RegressionVerdict 只能表示 replay/evaluation verdict。
RegressionVerdict 不能表示交易执行建议。
PASS / FAIL / WARN / SKIPPED 只表示回放评估结果。
```

## 5. Redaction implementation boundary

下一轮 implementation 必须加入敏感字段守卫和回归测试。禁止字段名、JSON key、DTO 属性中出现：

```text
rawPrompt
raw_prompt
promptText
rawProviderResponse
raw_provider_response
providerRaw
credential
apiKey
apiSecret
passphrase
token
cookie
secret
```

安全扫描命中必须分类：

```text
docs prohibition
test guard
legacy unrelated
actual risk
```

处理规则：

```text
docs prohibition / test guard: 可保留，但不得包含真实敏感值。
legacy unrelated: 记录为 residual，不扩大 B2 implementation 范围。
actual risk: 立即停止 implementation，输出 blocker，不继续写代码或 migration。
```

持久化允许内容限于：

```text
tenant_id
trace_id
request_id
source_decision_id / source_request_id
case_id / evaluation_id / verdict_id
policy_version
model_version_ref / model_gateway_version_ref
summary hash
input/output content hash
redacted expected/actual summary
provider kind/status/failure classification
trust decision ref
budget/status summary
finding code / severity / redacted message
```

## 6. Test implementation boundary

下一轮 B2 implementation 至少覆盖：

| 序号 | 测试项 | 验收标准 |
| --- | --- | --- |
| 1 | Flyway V9 migration loads successfully | `V9__qdr_replay_evaluation_baseline.sql` 可由 Flyway 顺序加载。 |
| 2 | repository saves replay case with tenant_id | 保存后可按 tenant + `case_id` / `trace_id` 查询。 |
| 3 | repository rejects missing tenant_id | 缺失 tenant 直接 fail-closed，不写库。 |
| 4 | repository query is tenant-bound | 所有查询 SQL 必须包含 `tenant_id = ?` 或等价 tenant 条件。 |
| 5 | repository does not persist raw prompt | raw prompt 样本被拒绝，或只保存 hash/ref。 |
| 6 | repository does not persist raw provider response | raw provider response 样本被拒绝，或只保存 hash/ref/summary。 |
| 7 | repository does not persist credential | credential-like 样本被拒绝。 |
| 8 | regression verdict can be saved and queried | verdict 可按 tenant + `verdict_id` / `evaluation_id` 查询。 |
| 9 | regression finding list can be saved and queried | 多 finding 可按 tenant + `verdict_id` 分页查询。 |
| 10 | duplicate case_id behavior is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 11 | duplicate evaluation_id behavior is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 12 | cross-tenant replay case read returns empty or fail-closed | tenant B 不得读到 tenant A replay case。 |
| 13 | cross-tenant evaluation case read returns empty or fail-closed | tenant B 不得读到 tenant A evaluation case。 |
| 14 | cross-tenant verdict read returns empty or fail-closed | tenant B 不得读到 tenant A verdict/finding。 |
| 15 | pagination pageSize > 100 is rejected or capped | 超上限分页请求 fail-closed 或显式 capped。 |
| 16 | BUY / SELL / MARKET_ORDER cannot be persisted as executable action | 可执行交易动作不得进入 persisted action。 |
| 17 | PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE cannot be persisted as allowed action | 禁止动作不得保存为允许动作。 |
| 18 | JSONB summary does not contain raw prompt/provider response/credential keys | JSONB 只含结构化 summary/ref，不含 raw/sensitive key。 |
| 19 | repository save failure fails closed | DB / serialization / constraint failure 不被吞掉。 |
| 20 | quality validate passes | `mvn -ntp -Pquality validate` 通过。 |

## 7. Validation command boundary

下一轮 B2 implementation 必须执行：

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

不得把 Maven wrapper 写成 PASS。

## 8. Safety scan boundary

下一轮 B2 implementation 必须执行：

```powershell
rg -n "rawPrompt|raw_prompt|promptText|rawProviderResponse|raw_provider_response|providerRaw|credential|apiKey|apiSecret|passphrase|token|cookie|secret|BUY|SELL|MARKET_ORDER|PLACE_ORDER|CANCEL_ORDER|MUTATE_NQ_STATE|real HTTP|real provider|LangGraph|AutoGen|CrewAI|LIVE" dh-domain dh-usecase dh-infra dh-app docs/current
```

判定要求：

```text
sensitive terms 只能出现在禁止项、测试守卫、redaction guard 或文档风险说明中。
BUY / SELL / MARKET_ORDER 只能出现在禁止项或测试守卫中。
PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 只能出现在禁止项或测试守卫中。
不得出现真实 HTTP/provider/Agent/LangGraph/LIVE 实现。
```

## 9. Review / close rule

后续顺序固定：

```text
1. DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION
2. DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW
3. 通过 close review 后才允许进入 B3 plan / implementation
```

不允许：

```text
B2 implementation 完成后直接进入 B3
B2 与 B3 合并实施
B2 implementation 直接新增 API
B2 implementation 接 provider / HTTP / Agent / LangGraph
B2 implementation 启用 LIVE
```

## 10. Rollback / recovery strategy

```text
Java / repository 改动回滚：使用 Git revert 或按文件级回滚，不允许 reset --hard。
V9 migration 尚未提交前：直接删除本地新增 V9 并恢复测试。
V9 migration 已提交后：不得修改已提交 migration；通过后续补偿 migration 前滚修复。
若 V9 已被任一共享环境应用，不得重写 V9 历史。
若发现敏感字段入库风险，立即停止 implementation，回滚未提交代码，并输出 blocker。
若发现 tenant isolation 缺口，优先修 repository SQL / method signature / tests，不绕过 tenant 校验。
若发现 regression output 被误用为 trading signal，立即阻断 close review。
```

## 11. Readiness decision

```text
STAGE_QDR_4_B2_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B2_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步：

```text
DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-IMPLEMENTATION
```
