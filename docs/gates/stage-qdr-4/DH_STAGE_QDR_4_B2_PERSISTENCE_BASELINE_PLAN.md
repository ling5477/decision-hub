# DH stage-qdr-4 B2 Replay / Evaluation Persistence Baseline Plan

> current planning document for `DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN`
> planning only; no code, test, API, repository, provider, HTTP, Agent, LangGraph, NQ or LIVE change

```text
Task: DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
Task type: PLANNING_ONLY + PERSISTENCE_BASELINE_DESIGN + MIGRATION_REVIEW_PREP + QDR_REPLAY_EVALUATION + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / PLAN_ONLY
Date: 2026-07-08
Repository: F:/project/decision-hub
Branch: dev
B1 commit requirement: feat(qdr): add replay evaluation domain contracts
```

## 1. 目标与边界

本轮只规划 stage-qdr-4 B2 persistence baseline。B1 已完成 replay / evaluation domain contracts；B2 implementation 前需要冻结以下持久化边界：

```text
replay case identity
evaluation case identity
expected / actual summary persistence boundary
regression verdict / finding persistence boundary
input / output ref persistence boundary
tenant isolation
redaction policy
migration review/freeze condition
repository method boundary
implementation test matrix
```

本轮明确不做：

```text
不修改 Java 生产代码
不修改 Java 测试代码
不新增 migration
不修改 V1-V8 migration
不新增 V9 migration
不新增 Repository / JDBC / Service 实现
不新增 API / Controller / OpenAPI
不新增真实 HTTP client
不新增真实 provider / Provider SDK
不接 OpenAI / Anthropic / Gemini / Ollama SDK
不接 LangGraph / AutoGen / CrewAI
不启动 Agent runtime
不修改 NQ
不读取 credential / token / cookie / apiKey / apiSecret / passphrase
不持久化 raw prompt
不持久化 raw provider response
不触碰交易、订单、撤单、账户、ledger mutation、risk mutation、paper mutation、live mutation
不把 replay / regression output 写成 trading signal
不启用 LIVE
```

## 2. 现有基线

B1 合同基线：

```text
ReplayCase: tenantId / caseId / sourceDecisionId / sourceRequestId / traceId / inputRef / expectedSummary / policy / createdAt
EvaluationCase: tenantId / evaluationId / caseId / policyVersion / modelVersionRef / modelGatewayVersionRef / expectedSummary / actualSummary / verdict / outputRef
ExpectedDecisionSummary: decisionType / actionLabel / confidenceBand / riskLevel / requiredEvidenceRefs / forbiddenActions
RegressionVerdict: PASS / FAIL / WARN / SKIPPED
RegressionFinding: code / severity / message / evidenceRef
ReplayInputRef: refType / refId / contentHash
ReplayOutputRef: refType / refId / contentHash
```

现有持久化风格：

```text
V6: decision_request / decision_run / quant_signal / quant_decision
V8: qdr_prompt_template / qdr_prompt_version / qdr_model_profile / qdr_model_version / qdr_model_gateway_call
JDBC adapter: tenant-bound query, fail-closed persistence exception, JSONB uses CAST(? AS jsonb)
Security: hash/ref/redacted summary only; raw prompt / raw provider response / credential forbidden
```

## 3. 是否需要 V9 migration

结论：B2 implementation 需要规划 `V9__qdr_replay_evaluation_baseline.sql`，但本轮不得创建该文件。

原因：

```text
B2 目标包含 persistence baseline，不是纯内存 contract。
必须持久化 replay case、evaluation case、expected/actual summary、regression verdict/finding、input/output ref。
如果没有 V9，只能继续停留在 B1 内存合同，无法满足 tenant-bound lookup、golden regression verdict、duplicate case_id deterministic handling 和审计反查。
V9 必须先 review/freeze，再 implementation。
```

## 4. 表结构规划

本轮保留用户要求的 7 张表，不合并表。

不合并原因：

```text
qdr_replay_case 与 qdr_evaluation_case 生命周期不同：case 是回放输入基线，evaluation 是一次策略/模型网关版本下的评估。
qdr_expected_decision_summary 独立保存结构化 expected / actual summary，避免 verdict 或 case 表膨胀。
qdr_regression_verdict 与 qdr_regression_finding 分离，便于一个 verdict 对多个 finding，避免 JSONB 内嵌导致查询困难。
qdr_replay_input_ref 与 qdr_replay_output_ref 分离，便于强制输入/输出都只保存 ref/hash，不保存 raw payload。
分表增加审计可追踪性，并让 tenant_id、trace_id、case_id、evaluation_id、verdict_id 独立建索引。
```

### 4.1 qdr_replay_case

用途：保存可复核 replay case identity 与结构化输入引用，不执行真实 provider，不保存 raw prompt。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, case_id)`, `(tenant_id, created_at)` | 租户隔离字段，所有查询必须带入。 |
| `case_id` | varchar(128) | yes | no | unique with tenant | 租户内 replay case ID。 |
| `evaluation_id` | varchar(128) | no | yes | no | replay case 不直接绑定单次 evaluation。 |
| `verdict_id` | varchar(128) | no | yes | no | replay case 不直接绑定最终 verdict。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | B1 `sourceRequestId` 的查询别名，便于 request 反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | 生成 replay case 时采用的 evaluation policy version。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | 可为空；B1/B2 不代表真实 provider。 |
| `input_ref_id` | uuid | yes | no | fk planned | 指向 `qdr_replay_input_ref.id`。 |
| `output_ref_id` | uuid | no | yes | fk planned | replay case baseline 通常无 output。 |
| `expected_summary_id` | uuid | yes | no | fk planned | 指向 expected summary。 |
| `actual_summary_id` | uuid | no | yes | fk planned | replay case baseline 通常无 actual summary。 |
| `expected_summary_hash` | varchar(64) | yes | no | no | 64 位 SHA-256。 |
| `actual_summary_hash` | varchar(64) | no | yes | no | 评估前为空。 |
| `verdict` | varchar(32) | no | yes | no | replay case 本身不保存最终 verdict。 |
| `severity` | varchar(32) | no | yes | no | replay case 本身不保存 finding severity。 |
| `finding_code` | varchar(128) | no | yes | no | replay case 本身不保存 finding code。 |
| `finding_message` | text | no | yes | no | replay case 本身不保存 finding message。 |
| `case_checksum` | varchar(64) | yes | no | yes with tenant | duplicate `case_id` 判断同内容幂等或冲突。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, case_id)
check(source_decision_id is not null or source_request_id is not null)
check(case_checksum ~ '^[0-9a-f]{64}$')
check(expected_summary_hash ~ '^[0-9a-f]{64}$')
check(actual_summary_hash is null or actual_summary_hash ~ '^[0-9a-f]{64}$')
```

### 4.2 qdr_evaluation_case

用途：保存一次 evaluation case identity，连接 replay case、expected/actual summary、output ref 和 verdict。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, evaluation_id)`, `(tenant_id, case_id, created_at)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | yes | no | unique with tenant | 租户内 evaluation ID。 |
| `verdict_id` | varchar(128) | no | yes | yes with tenant | evaluation 完成后绑定 verdict。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | evaluation policy version。 |
| `model_version_ref` | varchar(128) | no | yes | yes with tenant | 可为空；与 gateway version 至少一个 required。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | 可为空；与 model version 至少一个 required。 |
| `input_ref_id` | uuid | yes | no | fk planned | 本次 evaluation 输入引用。 |
| `output_ref_id` | uuid | no | yes | fk planned | 评估产物输出引用。 |
| `expected_summary_id` | uuid | yes | no | fk planned | expected summary。 |
| `actual_summary_id` | uuid | no | yes | fk planned | actual summary；未执行评估时可为空。 |
| `expected_summary_hash` | varchar(64) | yes | no | no | 64 位 SHA-256。 |
| `actual_summary_hash` | varchar(64) | no | yes | no | actual summary hash。 |
| `verdict` | varchar(32) | no | yes | yes with tenant | PASS / FAIL / WARN / SKIPPED。 |
| `severity` | varchar(32) | no | yes | yes with tenant | 聚合最高 finding severity。 |
| `finding_code` | varchar(128) | no | yes | no | 聚合首要 finding code。 |
| `finding_message` | text | no | yes | no | 聚合脱敏 finding 摘要。 |
| `evaluation_checksum` | varchar(64) | yes | no | yes with tenant | deterministic duplicate handling。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, evaluation_id)
unique(tenant_id, case_id, policy_version, model_gateway_version_ref, evaluation_checksum)
check(source_decision_id is not null or source_request_id is not null)
check(model_version_ref is not null or model_gateway_version_ref is not null)
check(verdict is null or verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED'))
check(severity is null or severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER'))
check(evaluation_checksum ~ '^[0-9a-f]{64}$')
```

### 4.3 qdr_expected_decision_summary

用途：保存 expected / actual structured summary。表名保留为 required object；通过 `summary_role` 区分 `EXPECTED` 与 `ACTUAL`，不新增 `qdr_actual_decision_summary`。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, case_id, summary_role)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | no | yes | yes with tenant | actual summary 通常绑定 evaluation。 |
| `verdict_id` | varchar(128) | no | yes | yes with tenant | verdict 生成后可反查。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | summary 生成策略。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | summary 来源的 gateway 版本引用。 |
| `input_ref_id` | uuid | no | yes | fk planned | summary 来源输入引用。 |
| `output_ref_id` | uuid | no | yes | fk planned | actual summary 来源输出引用。 |
| `summary_role` | varchar(16) | yes | no | yes with tenant | EXPECTED / ACTUAL。 |
| `decision_type` | varchar(128) | yes | no | no | 例如 READ_ONLY_RECOMMENDATION。 |
| `action_label` | varchar(64) | yes | no | no | OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS / NEEDS_REVIEW / REJECTED。 |
| `confidence_band` | varchar(64) | yes | no | no | 脱敏置信区间标签。 |
| `risk_level` | varchar(64) | yes | no | no | LOW / MEDIUM / HIGH / BLOCKED / UNKNOWN。 |
| `expected_summary` | jsonb | conditional | yes | no | `summary_role = EXPECTED` 时 required，结构化 summary，不保存 raw prompt。 |
| `actual_summary` | jsonb | conditional | yes | no | `summary_role = ACTUAL` 时 required，结构化 summary，不保存 raw provider response。 |
| `required_evidence_refs_json` | jsonb | yes | no | no | evidence ref 数组，不保存 raw payload。 |
| `forbidden_actions_json` | jsonb | yes | no | no | 禁止动作数组，必须包含安全约束。 |
| `verdict` | varchar(32) | no | yes | no | summary 自身不作为 verdict source。 |
| `severity` | varchar(32) | no | yes | no | summary 自身不保存 finding severity。 |
| `finding_code` | varchar(128) | no | yes | no | summary 自身不保存 finding code。 |
| `finding_message` | text | no | yes | no | summary 自身不保存 finding message。 |
| `summary_hash` | varchar(64) | yes | no | yes with tenant | 64 位 SHA-256。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, case_id, evaluation_id, summary_role, summary_hash)
check(summary_role in ('EXPECTED', 'ACTUAL'))
check((summary_role = 'EXPECTED' and expected_summary is not null and actual_summary is null)
   or (summary_role = 'ACTUAL' and actual_summary is not null))
check(action_label in ('OBSERVE', 'NO_TRADE', 'LONG_BIAS', 'SHORT_BIAS', 'NEEDS_REVIEW', 'REJECTED'))
check(action_label not in ('BUY', 'SELL', 'PLACE_ORDER', 'CANCEL_ORDER', 'MARKET_ORDER'))
check(summary_hash ~ '^[0-9a-f]{64}$')
```

### 4.4 qdr_regression_verdict

用途：保存 evaluation 的 regression verdict。verdict 只表示回放评估结果，不代表 trading signal。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, verdict_id)`, `(tenant_id, evaluation_id, created_at)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | yes | no | yes with tenant | 关联 evaluation case。 |
| `verdict_id` | varchar(128) | yes | no | unique with tenant | 租户内 verdict ID。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | verdict 对应 policy。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | gateway 版本引用，不代表真实 provider。 |
| `input_ref_id` | uuid | yes | no | fk planned | 输入引用。 |
| `output_ref_id` | uuid | no | yes | fk planned | 输出引用。 |
| `expected_summary_id` | uuid | yes | no | fk planned | expected summary。 |
| `actual_summary_id` | uuid | no | yes | fk planned | actual summary；SKIPPED 时可为空。 |
| `expected_summary_hash` | varchar(64) | yes | no | no | 64 位 SHA-256。 |
| `actual_summary_hash` | varchar(64) | no | yes | no | 64 位 SHA-256。 |
| `verdict` | varchar(32) | yes | no | yes with tenant | PASS / FAIL / WARN / SKIPPED。 |
| `severity` | varchar(32) | yes | no | yes with tenant | 聚合最高 finding severity；PASS 可为 INFO。 |
| `finding_code` | varchar(128) | no | yes | yes with tenant | 聚合首要 finding code。 |
| `finding_message` | text | no | yes | no | 脱敏 finding 摘要。 |
| `failure_reason` | varchar(256) | no | yes | no | FAIL / WARN / SKIPPED 原因。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, verdict_id)
unique(tenant_id, evaluation_id, policy_version, model_gateway_version_ref)
check(verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED'))
check(severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER'))
check(verdict <> 'FAIL' or failure_reason is not null or finding_code is not null)
```

### 4.5 qdr_regression_finding

用途：保存 verdict 下的结构化 finding 列表。finding message 必须脱敏，不保存 raw prompt / raw provider response / credential。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, verdict_id)`, `(tenant_id, severity, created_at)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | yes | no | yes with tenant | 关联 evaluation case。 |
| `verdict_id` | varchar(128) | yes | no | yes with tenant | 关联 regression verdict。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | finding 对应 policy。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | gateway 版本引用。 |
| `input_ref_id` | uuid | no | yes | fk planned | 输入引用。 |
| `output_ref_id` | uuid | no | yes | fk planned | 输出引用。 |
| `expected_summary_id` | uuid | no | yes | fk planned | expected summary。 |
| `actual_summary_id` | uuid | no | yes | fk planned | actual summary。 |
| `expected_summary_hash` | varchar(64) | no | yes | no | 64 位 SHA-256。 |
| `actual_summary_hash` | varchar(64) | no | yes | no | 64 位 SHA-256。 |
| `verdict` | varchar(32) | yes | no | yes with tenant | finding 所属 verdict 状态。 |
| `severity` | varchar(32) | yes | no | yes with tenant | INFO / WARN / ERROR / BLOCKER。 |
| `finding_code` | varchar(128) | yes | no | yes with tenant | 结构化 finding code。 |
| `finding_message` | text | yes | no | no | 脱敏说明。 |
| `evidence_ref` | varchar(256) | no | yes | no | 脱敏 evidence ref。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
check(verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED'))
check(severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER'))
check(finding_code <> '')
check(finding_message <> '')
```

### 4.6 qdr_replay_input_ref

用途：保存 replay input 的结构化引用、hash 和来源反查字段，不保存输入原文。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, ref_type, ref_id)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | no | yes | yes with tenant | 可绑定 evaluation。 |
| `verdict_id` | varchar(128) | no | yes | yes with tenant | 可绑定 verdict。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | no | yes | yes with tenant | input ref 生成时的 policy version。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | gateway 版本引用。 |
| `ref_type` | varchar(64) | yes | no | yes with tenant | decision-run-ref / gateway-call-ref 等。 |
| `ref_id` | varchar(256) | yes | no | yes with tenant | 安全引用 ID。 |
| `input_ref` | jsonb | yes | no | no | 仅结构化 refType/refId/contentHash。 |
| `output_ref` | jsonb | no | yes | no | input ref 表通常为空。 |
| `expected_summary` | jsonb | no | yes | no | 通常不存 summary，仅保留兼容字段规划。 |
| `actual_summary` | jsonb | no | yes | no | input ref 表不得保存 actual raw output。 |
| `verdict` | varchar(32) | no | yes | no | input ref 不产生 verdict。 |
| `severity` | varchar(32) | no | yes | no | input ref 不产生 severity。 |
| `finding_code` | varchar(128) | no | yes | no | input ref 不产生 finding。 |
| `finding_message` | text | no | yes | no | input ref 不产生 finding。 |
| `content_hash` | varchar(64) | yes | no | yes with tenant | 输入内容 hash，不保存原文。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, ref_type, ref_id)
check(source_decision_id is not null or source_request_id is not null)
check(content_hash ~ '^[0-9a-f]{64}$')
```

### 4.7 qdr_replay_output_ref

用途：保存 replay / evaluation output 的结构化引用、hash 和来源反查字段，不保存 provider 原始响应。

| 字段 | 类型 | Required | Nullable | Index | 说明 |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | yes | no | primary key | 主键。 |
| `tenant_id` | varchar(128) | yes | no | yes, `(tenant_id, ref_type, ref_id)` | 租户隔离字段。 |
| `case_id` | varchar(128) | yes | no | yes with tenant | 关联 replay case。 |
| `evaluation_id` | varchar(128) | yes | no | yes with tenant | 关联 evaluation case。 |
| `verdict_id` | varchar(128) | no | yes | yes with tenant | 可绑定 verdict。 |
| `source_decision_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_request_id` 至少一个 required。 |
| `source_request_id` | varchar(128) | conditional | yes | yes with tenant | 与 `source_decision_id` 至少一个 required。 |
| `trace_id` | varchar(128) | yes | no | yes | 审计链路反查。 |
| `request_id` | varchar(128) | yes | no | yes | 请求反查。 |
| `policy_version` | varchar(128) | yes | no | yes with tenant | output ref 对应 policy。 |
| `model_gateway_version_ref` | varchar(128) | no | yes | yes with tenant | gateway 版本引用。 |
| `ref_type` | varchar(64) | yes | no | yes with tenant | gateway-summary-ref / report-ref 等。 |
| `ref_id` | varchar(256) | yes | no | yes with tenant | 安全引用 ID。 |
| `input_ref` | jsonb | no | yes | no | 可保存输入 ref 摘要，不保存原文。 |
| `output_ref` | jsonb | yes | no | no | 仅结构化 refType/refId/contentHash。 |
| `expected_summary` | jsonb | no | yes | no | 通常不存 summary，仅保留兼容字段规划。 |
| `actual_summary` | jsonb | no | yes | no | 结构化 actual summary ref，不保存 provider raw response。 |
| `verdict` | varchar(32) | no | yes | no | output ref 不产生 verdict。 |
| `severity` | varchar(32) | no | yes | no | output ref 不产生 severity。 |
| `finding_code` | varchar(128) | no | yes | no | output ref 不产生 finding。 |
| `finding_message` | text | no | yes | no | output ref 不产生 finding。 |
| `content_hash` | varchar(64) | yes | no | yes with tenant | 输出内容 hash，不保存原文。 |
| `created_at` | timestamptz | yes | no | yes with tenant | 创建时间。 |
| `updated_at` | timestamptz | yes | no | no | 更新时间。 |

约束：

```text
primary key(id)
unique(tenant_id, ref_type, ref_id)
check(source_decision_id is not null or source_request_id is not null)
check(content_hash ~ '^[0-9a-f]{64}$')
```

## 5. Foreign key 策略

计划使用 DH 内部 B2 表之间的 foreign key，但不对 V6/V8 source 表做强 FK。

计划使用的 FK：

```text
qdr_evaluation_case(tenant_id, case_id) -> qdr_replay_case(tenant_id, case_id)
qdr_regression_verdict(tenant_id, evaluation_id) -> qdr_evaluation_case(tenant_id, evaluation_id)
qdr_regression_finding(tenant_id, verdict_id) -> qdr_regression_verdict(tenant_id, verdict_id)
qdr_replay_case.input_ref_id -> qdr_replay_input_ref.id
qdr_evaluation_case.output_ref_id -> qdr_replay_output_ref.id
summary/ref/verdict id FK 需在 implementation review 中确认是否会形成循环；如有循环，改为 repository-level consistency + unique indexed refs。
```

不对 V6/V8 强 FK 的理由：

```text
source_decision_id / source_request_id / trace_id 可能来自历史 QDR run、mock gateway call、外部 dry-run envelope 或未来只读 report ref。
强 FK 到 V6/V8 会让 replay/evaluation baseline 与历史迁移顺序、清理策略和可选 gateway evidence 过度耦合。
审计反查通过 tenant_id + trace_id + request_id + source_decision_id/source_request_id + model_gateway_version_ref 完成。
如果 implementation review 发现必须强一致，可只对稳定 UUID ref 增加 FK，不对外部 string ref 强绑。
```

## 6. Repository 边界规划

本轮只规划 port，不创建接口或实现。

### 6.1 ReplayCaseRepository

| 方法名 | 输入参数 | 输出类型 | tenant 校验 | fail-closed 行为 | 分页 | 跨 tenant |
| --- | --- | --- | --- | --- | --- | --- |
| `saveReplayCase` | `SaveReplayCaseCommand command` | `ReplayCaseRecord` | command 必须包含 `tenantId`；nested input/summary tenant 必须一致 | tenant 缺失、unsafe text、raw prompt/provider/credential、duplicate checksum mismatch、DB error 均抛 persistence exception | no | no |
| `findByTenantAndCaseId` | `String tenantId, String caseId` | `Optional<ReplayCaseRecord>` | `tenantId` required；SQL `where tenant_id = ? and case_id = ?` | tenant 缺失拒绝；DB error 抛 exception | no | no |
| `findByTenantAndTraceId` | `String tenantId, String traceId, PageRequest page` | `List<ReplayCaseRecord>` | tenant required；trace required | pageSize 超过上限拒绝；DB error 抛 exception | yes, max 100 | no |
| `findByTenantAndSourceRequestId` | `String tenantId, String sourceRequestId, PageRequest page` | `List<ReplayCaseRecord>` | tenant required | 缺参 fail-closed | yes, max 100 | no |

### 6.2 EvaluationCaseRepository

| 方法名 | 输入参数 | 输出类型 | tenant 校验 | fail-closed 行为 | 分页 | 跨 tenant |
| --- | --- | --- | --- | --- | --- | --- |
| `saveEvaluationCase` | `SaveEvaluationCaseCommand command` | `EvaluationCaseRecord` | command tenant、case tenant、summary/ref tenant 必须一致 | missing tenant、missing policy/model gateway ref、unsafe summary、duplicate checksum mismatch、DB error 均 fail-closed | no | no |
| `findByTenantAndEvaluationId` | `String tenantId, String evaluationId` | `Optional<EvaluationCaseRecord>` | SQL 必须带 tenant | 缺参拒绝；DB error 抛 exception | no | no |
| `findByTenantAndCaseId` | `String tenantId, String caseId, PageRequest page` | `List<EvaluationCaseRecord>` | SQL 必须带 tenant | pageSize > 100 拒绝 | yes, max 100 | no |
| `findByTenantAndPolicyVersion` | `String tenantId, String policyVersion, PageRequest page` | `List<EvaluationCaseRecord>` | SQL 必须带 tenant | 缺参拒绝 | yes, max 100 | no |

### 6.3 RegressionVerdictRepository

| 方法名 | 输入参数 | 输出类型 | tenant 校验 | fail-closed 行为 | 分页 | 跨 tenant |
| --- | --- | --- | --- | --- | --- | --- |
| `saveVerdict` | `SaveRegressionVerdictCommand command` | `RegressionVerdictRecord` | command tenant、evaluation tenant、finding tenant 必须一致 | FAIL verdict 无 reason/finding、unsafe message、raw material、DB error 均 fail-closed | no | no |
| `saveFindings` | `String tenantId, String verdictId, List<SaveRegressionFindingCommand> commands` | `List<RegressionFindingRecord>` | tenant + verdictId required；每条 finding tenant 一致 | 空 tenant、跨 tenant、unsafe message、DB error 均 fail-closed；建议同事务 | no | no |
| `findByTenantAndVerdictId` | `String tenantId, String verdictId` | `Optional<RegressionVerdictRecord>` | SQL 必须带 tenant | 缺参拒绝 | no | no |
| `findLatestByTenantAndEvaluationId` | `String tenantId, String evaluationId` | `Optional<RegressionVerdictRecord>` | SQL 必须带 tenant | 缺参拒绝 | no | no |
| `findFindingsByTenantAndVerdictId` | `String tenantId, String verdictId, PageRequest page` | `List<RegressionFindingRecord>` | SQL 必须带 tenant | pageSize > 100 拒绝 | yes, max 100 | no |

默认策略：

```text
所有 repository 不提供 UUID-only 方法。
所有读取必须 tenant-bound。
所有列表必须分页，默认 pageSize <= 50，最大 pageSize <= 100。
跨 tenant 查询不允许；未来管理审计如需跨 tenant，必须单独 security review，不得复用普通 repository。
缺失 tenant_id、trace_id、case_id、evaluation_id、verdict_id 等关键字段必须 fail-closed。
Duplicate case_id: same checksum 可返回 existing 或幂等成功；different checksum 必须 duplicate conflict fail-closed。
```

## 7. Redaction 与安全策略

持久化允许内容：

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
finding code / severity /脱敏 message
```

持久化禁止内容：

```text
raw prompt
rendered prompt 原文
raw provider response
credential / apiKey / apiSecret / token / cookie / passphrase
原始签名串
未脱敏 request header / response body
交易所完整凭证
可执行订单 payload
账户、账本、风控、paper、live mutation payload
```

Provider 相关字段只能保存：

```text
summary
hash
version
status
failure classification
provider identity ref without secret
```

Replay / regression 语义：

```text
replay case 只能复现结构化输入引用，不执行真实 provider。
regression verdict 不得被解释为 trading signal。
LONG_BIAS / SHORT_BIAS 不得映射 BUY / SELL。
PASS / FAIL / WARN / SKIPPED 只表示回放评估结果。
所有未来读取接口必须 tenant-bound。
```

## 8. V9 migration freeze 条件

`V9__qdr_replay_evaluation_baseline.sql` implementation 前必须 freeze/review 以下内容：

```text
7 张表是否全部需要落库，是否仍禁止合并
所有主表是否包含 tenant_id 且 query key 支持 tenant-bound access
trace_id / request_id / source_decision_id / source_request_id 是否可反查
unique constraint 是否支持 deterministic duplicate handling
foreign key 是否会形成循环或过度耦合 V6/V8
jsonb 字段是否只保存结构化 summary/ref，不保存 raw material
CHECK constraint 是否覆盖 verdict / severity / action label / hash / status
COMMENT ON TABLE / COMMENT ON COLUMN 是否覆盖敏感数据禁止项
migration 是否可被 Flyway 顺序加载
repository 是否无 UUID-only / cross-tenant query
是否出现交易语义污染
```

V9 计划最小 DDL 单元：

```text
create table qdr_replay_input_ref
create table qdr_replay_output_ref
create table qdr_expected_decision_summary
create table qdr_replay_case
create table qdr_evaluation_case
create table qdr_regression_verdict
create table qdr_regression_finding
primary key on id
unique(tenant_id, case_id)
unique(tenant_id, evaluation_id)
unique(tenant_id, verdict_id)
tenant_id indexes
trace_id indexes
request_id indexes
case_id indexes
evaluation_id indexes
verdict_id indexes
created_at indexes
jsonb fields limited to structured refs/summaries
check constraints for enum/status/hash/trading-term denial
COMMENT on every table and security-critical column
```

## 9. B2 implementation 测试矩阵

B2 implementation 必须补充或更新以下测试；本轮不得实现：

| 序号 | 测试项 | 验收标准 |
| --- | --- | --- |
| 1 | migration loads successfully | `V9__qdr_replay_evaluation_baseline.sql` 可由 Flyway 加载。 |
| 2 | repository saves replay case with tenant_id | 保存后 `tenant_id`、`case_id`、`trace_id` 可查。 |
| 3 | repository rejects missing tenant_id | 缺失 tenant 直接 fail-closed，不写库。 |
| 4 | repository query is tenant-bound | 所有查询 SQL 带 `tenant_id = ?`。 |
| 5 | repository does not persist raw prompt | raw prompt 样本被拒绝或只保存 hash/ref。 |
| 6 | repository does not persist raw provider response | raw provider response 样本被拒绝或只保存 hash/ref/summary。 |
| 7 | repository does not persist credential | credential-like 样本被拒绝。 |
| 8 | regression verdict can be saved and queried | verdict 可按 tenant + verdict_id / evaluation_id 查询。 |
| 9 | finding list can be saved and queried | 多 finding 可按 tenant + verdict_id 分页查询。 |
| 10 | duplicate case_id handling is deterministic | same checksum 幂等；different checksum fail-closed。 |
| 11 | cross-tenant read returns empty or fail-closed | tenant B 不得读到 tenant A 数据。 |
| 12 | Flyway migration test passes | migration test 覆盖 V1-V9 顺序加载。 |

补充建议：

```text
V9 COMMENT scan: table/column comments include raw prompt/provider/credential prohibition.
jsonb scope test: expected_summary / actual_summary 仅结构化字段，不包含 raw payload keys.
trading term guard: BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER 不得进入 summary action_label。
repository pagination test: list query pageSize 超上限 fail-closed。
```

## 10. Review / Freeze 规则

B2 涉及 migration、repository 和 persistence boundary。必须执行：

```text
B2 implementation 前: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-FREEZE-REVIEW
B2 implementation 后: DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW
```

Freeze/review 重点：

```text
schema 是否泄露敏感数据
tenant isolation 是否完整
jsonb 是否过度存储
索引是否支持 replay/eval 查询
migration 是否可回滚或可通过补偿 migration 前滚修复
repository 是否 fail-closed
是否出现交易语义污染
是否出现 real HTTP / provider / SDK / Agent / LangGraph / LIVE 误启用
```

## 11. Readiness decision

```text
STAGE_QDR_4_B2_PLAN: DONE
ALLOW_STAGE_QDR_4_B2_FREEZE_REVIEW: YES
ALLOW_STAGE_QDR_4_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步：

```text
DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-FREEZE-REVIEW
```
