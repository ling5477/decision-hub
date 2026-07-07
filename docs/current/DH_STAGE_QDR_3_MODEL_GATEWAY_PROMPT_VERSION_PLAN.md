# DH stage-qdr-3 Model Gateway + Prompt/Model Version Plan

```text
Task: DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN
Task type: PLANNING_ONLY + MODEL_GATEWAY_DESIGN + PROMPT_VERSION_DESIGN + PROVIDER_TRUST_BOUNDARY_DESIGN + AUDIT_REDACTION_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION
Status: DONE / PLAN_ONLY / WORK_ORDER_REQUIRED / IMPLEMENTATION_NOT_STARTED
Fact source: docs/current
Date: 2026-07-07
```

## 1. Stage 定义

```text
stage-qdr-3 = Model Gateway + Prompt/Model Version Baseline
```

stage-qdr-3 的目标是为 QDR 后续可审计的 provider / prompt / model version 链路建立统一边界。它不是 Agent phase，不是 LangGraph runtime，不接真实 provider，不接真实 HTTP，不新增 LIVE 能力，不做 replay execution，不做 NQ integration runtime。

stage-qdr-3 只允许规划和后续 mock / contract / versioning baseline。所有 provider 调用未来都必须经过统一 gateway；业务代码不得直连外部 AI provider，不得绕过 `ProviderTrustPolicy`。

## 2. 当前前置状态

```text
stage-qdr-1: CLOSED / ACCEPTED
stage-qdr-2 implementation: DONE
stage-qdr-2 close review: YES
stage-qdr-2 acceptance: ACCEPTED
stage-qdr-2 final close: CLOSED
stage-qdr-2 final docs sync commit: docs(qdr): record stage-qdr-2 acceptance
stage-qdr-3 planning: DONE / PLAN_ONLY
stage-qdr-3 implementation: NOT STARTED
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

本计划不修改 Java 生产代码、测试代码、migration、API、Controller、Repository、Service、contracts、golden_cases 或 NQ 仓库。

## 3. Model Gateway 设计边界

### 3.1 计划接口与模型

以下对象只在本计划中定义边界，当前不实现：

```text
ModelGatewayPort
ModelProviderPort
ModelGatewayRequest
ModelGatewayResult
ModelCallContext
ModelCallPolicy
ModelCallBudget
ModelCallRedactionPolicy
ModelCallAuditRef
ProviderTrustPolicy integration
MockModelProvider
```

### 3.2 Gateway 输入约束

`ModelGatewayRequest` 必须 tenant-bound，并必须携带：

```text
tenantId
traceId
requestId
decisionRunId
promptVersionId
promptVersionChecksum
modelVersionId
providerProfileId
modelCallPolicy
modelCallBudget
redactionPolicy
```

缺少任一必需追踪字段时，gateway 必须 fail-closed，不得调用 provider。`tenantId` 只能来自已认证上下文或已验证的 QDR run，不允许由未认证外部 payload 覆盖。

### 3.3 Gateway 输出约束

`ModelGatewayResult` 必须是结构化结果，不允许直接返回 free-form final text。允许字段只能表达：

```text
status
failureCode
modelCallRef
auditRef
promptVersionRef
modelVersionRef
providerTrustDecisionRef
redactedSummary
structuredDecisionHints
usageSummary
latencyMs
```

`structuredDecisionHints` 只能进入只读审查语义，不能直接改变 approval status，不能触发交易，不能修改 NQ，不能绕过 QDR existing policy / risk / audit guard。`LONG_BIAS / SHORT_BIAS` 仍只表示只读方向性意见，不得映射 `BUY / SELL`。

### 3.4 Raw prompt / raw response 存储

当前默认禁止：

```text
raw prompt storage: NO
raw provider response storage: NO
credential storage: NO
secret-like material storage: NO
```

未来如需保存 raw prompt 或 raw provider response，必须另起安全评审、数据分类评审、migration review 和脱敏策略验收；stage-qdr-3 当前计划只允许保存 hash / ref / redacted summary / redacted excerpt。

### 3.5 ProviderTrustPolicy

所有 provider 调用必须先通过 `ProviderTrustPolicy`。策略至少判断：

```text
providerProfile enabled
tenant/provider allowlist
environment allowlist
mock-only vs real-provider flag
payload cap
token budget
memory cap
prompt injection guard result
redaction result
model capability allowlist
no executable trading instruction
```

策略拒绝时必须返回 `POLICY_DENIED` 并写 redacted audit event。不得 fallback to allow。

### 3.6 Mock provider

`MockModelProvider` 只允许 deterministic / in-memory 行为：

```text
no HttpClient
no WebClient
no RestTemplate
no OkHttp
no provider SDK
no localhost real service
no outbound network
no credential
```

Mock provider 必须可被 source scan / ArchUnit / unit test 证明没有真实 HTTP。mock 结果必须结构化，并且只能用于 QDR read-only decision evidence。

### 3.7 Fail-closed 矩阵

| 场景 | 必须结果 |
| --- | --- |
| provider unavailable | `PROVIDER_UNAVAILABLE`，不产出 allow，不推进 approval，不触发交易 |
| prompt denied | `PROMPT_DENIED`，不调用 provider |
| budget exceeded | `BUDGET_EXCEEDED`，不调用 provider或停止后续 provider step |
| policy denied | `POLICY_DENIED`，不 fallback |
| redaction failure | `REDACTION_FAILED`，不保存 raw，不调用 provider |
| provider timeout | `PROVIDER_TIMEOUT`，只写 redacted summary |
| provider malformed output | `PROVIDER_OUTPUT_INVALID`，structured output fail-closed |
| audit failure | `AUDIT_WRITE_FAILED`，本轮计划默认 fail-closed |
| trace failure | `TRACE_WRITE_FAILED`，本轮计划默认 fail-closed |

## 4. Prompt / Model Version 设计

### 4.1 计划对象

以下对象只规划边界，当前不实现：

```text
PromptTemplate
PromptVersion
PromptRenderContext
PromptRenderResult
PromptInputPolicy
PromptInjectionGuard
PromptVersionRegistry
ModelProfile
ModelVersion
ProviderProfile
```

### 4.2 Prompt template 与 version

`PromptTemplate` 可变的是元数据与当前指针；具体 `PromptVersion` 必须 immutable。每个 version 必须包含 checksum / hash：

```text
promptVersionId
templateKey
version
checksum
createdAt
createdBy
status
redactionPolicyRef
inputPolicyRef
```

任何 prompt change 都必须创建新 `PromptVersion`，不得原地覆盖旧 version。Replay 或 audit 读取到 prompt/model version mismatch 时必须 fail-closed，并标记 `PROMPT_MODEL_VERSION_MISMATCH`。

### 4.3 Render 约束

`PromptRenderResult` 必须先过 `PromptInputPolicy`、`PromptInjectionGuard` 与 redaction，再允许进入 gateway。render 结果禁止包含：

```text
secret-like material
credential
token
cookie
apiKey
apiSecret
passphrase
private key
raw provider response
executable trading instruction
BUY / SELL as action
PLACE_ORDER / CANCEL_ORDER
MUTATE_NQ_STATE
```

`BUY / SELL` 只能作为 denylist、历史禁止项或被引用的已脱敏风险材料出现，不允许作为 prompt 要求或 gateway output action。

### 4.4 Prompt injection guard

`PromptInjectionGuard` 至少定义以下拒绝类型：

```text
IGNORE_SYSTEM_POLICY
EXFILTRATE_SECRET
REQUEST_CREDENTIAL
OVERRIDE_RISK_GATE
EXECUTE_TRADE
MAP_BIAS_TO_ORDER
MUTATE_NQ
RAW_PROMPT_REQUEST
RAW_PROVIDER_RESPONSE_REQUEST
TOOL_OR_AGENT_ESCALATION
```

命中任一拒绝类型时必须 fail-closed，写 redacted audit event，不调用 provider。

### 4.5 Model / provider profile

`ModelProfile` 和 `ModelVersion` 必须记录：

```text
provider
providerProfileId
modelName
modelVersion
capability
contextWindow
maxOutputTokens
defaultTimeout
budgetClass
trustTier
status
checksum or provider version ref
```

`ProviderProfile` 必须记录 provider 类型、环境、是否 mock、是否 enabled、trust policy ref 与 feature flag。future real provider 必须 feature flag disabled by default，并且 production 默认 disabled。

### 4.6 Audit / trace 引用

Prompt version、model version、provider trust decision 必须进入：

```text
model call audit event
prompt render audit event
V5 dh_decision_trace_step summary/ref
V5 dh_decision_provider_call_log safe summary or successor table ref
V6 decision_run model_provider / model_name successor fields or planned refs
quant_decision constraints_json safe ref
read model trace view
```

当前不保存 raw prompt，不保存 raw provider response，只保存 version ref、checksum、hash、redacted summary 与 audit ref。

## 5. Audit / Redaction / Trace 设计

stage-qdr-3 必须规划以下 audit event：

```text
MODEL_CALL_REQUESTED
MODEL_CALL_COMPLETED
MODEL_CALL_FAILED
PROMPT_RENDERED
PROMPT_RENDER_DENIED
PROVIDER_POLICY_DENIED
MODEL_BUDGET_EXCEEDED
MODEL_REDACTION_FAILED
MOCK_PROVIDER_CALLED
PROVIDER_TRUST_DECISION_RECORDED
```

每个 event 至少携带：

```text
tenantId
traceId
requestId
decisionRunId
promptVersionRef
modelVersionRef
providerProfileRef
providerTrustDecisionRef
modelCallRef
redactionStatus
failureCode
redactedSummary
hash/ref
occurredAt
```

明确禁止：

```text
不保存 credential
不保存 raw prompt
不保存 raw provider response
不保存 raw signature material
不保存 full header/body
不保存 executable trading instruction
```

audit failure、trace failure、redaction failure 均 fail-closed。后续若希望 audit 异步化，必须另起设计评审并证明可追踪、可补偿、可拒绝成功返回。

## 6. 数据模型规划

本轮不新增 migration，不修改历史 migration。stage-qdr-3 后续如进入 persistence baseline，必须先出 migration work order，再 review/freeze。

### 6.1 候选表

| 表 | stage-qdr-3 是否必须 | 批次 | 说明 |
| --- | --- | --- | --- |
| `prompt_template` | 必须，持久化基线前必须有 | B3 | 保存 template key、tenant / global scope、状态、当前 version ref，不保存 raw secret |
| `prompt_version` | 必须，持久化基线前必须有 | B3 | immutable version、checksum、render policy ref、redaction policy ref |
| `model_profile` | 必须，持久化基线前必须有 | B3 | provider/model capability 与 trust tier |
| `model_version` | 必须，持久化基线前必须有 | B3 | modelName + modelVersion + checksum/ref + capability |
| `model_gateway_call` | 必须，持久化 model call ref 前必须有 | B3 | model call safe summary、budget、latency、failure code、version refs |
| `model_gateway_audit_ref` | 可后置 | B3 或后续 | 如 V5 `dh_decision_audit_event` 已能表达 ref，可先不建独立表 |

`model_call` 可作为业务概念名称；表名建议使用 `model_gateway_call`，避免与旧 provider call log 语义混淆。既有 `dh_decision_provider_call_log` 可继续作为 V5 audit trace 读取来源，但不应承载 raw provider response。

### 6.2 字段与约束建议

所有 stage-qdr-3 新表都必须 tenant-aware，至少包含：

```text
tenant_id
trace_id where event/call scoped
request_id where event/call scoped
decision_run_id where event/call scoped
created_at
updated_at where mutable metadata exists
status
```

`prompt_version` 必须有 immutable version / checksum：

```text
unique(tenant_id, template_key, version)
unique(tenant_id, prompt_version_id)
checksum not null
status in (DRAFT, ACTIVE, DEPRECATED, DISABLED)
```

`model_version` 建议：

```text
unique(tenant_id, provider_profile_id, model_name, model_version)
index(provider_profile_id, status)
```

`model_gateway_call` 建议：

```text
index(tenant_id, decision_run_id, created_at)
index(trace_id)
index(request_id)
index(prompt_version_id)
index(model_version_id)
raw_prompt column: FORBIDDEN
raw_response column: FORBIDDEN
redacted_summary / prompt_hash / response_hash / request_ref / response_ref: ALLOWED
```

任何 migration batch 都必须 review/freeze。migration batch 之前不得实现 API、真实 provider 或 provider SDK。

## 7. QDR Mock 接入规划

stage-qdr-3 后续 QDR 接入只能接 mock gateway：

```text
DecisionRun -> PromptVersionRegistry mock lookup
PromptRenderPolicy -> redacted PromptRenderResult
ProviderTrustPolicy -> allow mock only
ModelGatewayPort -> MockModelProvider
ModelGatewayResult -> structured hints
DecisionOutput / quant_decision -> read-only evidence only
Audit / Trace -> safe refs
```

Mock gateway 不允许影响 approval status，不允许触发 NQ，不允许触发交易，不允许写 replay execution，不允许改变 `APPROVED / REJECTED` 语义。

## 8. 实现批次规划

stage-qdr-3 后续 implementation 必须先输出独立 work order，不得从本 plan 直接实现。

### B1: Prompt / Model Version Domain + Mock Registry

允许 domain / usecase contracts、in-memory/mock registry、checksum / immutable version rule 与 unit tests。不接真实 provider，不新增 API，不新增 HTTP。如不涉及 migration/API/security boundary，可实现后直接 validation + minimal docs，不单独长 review。

### B2: Model Gateway Mock Runtime + Policy Guard

只接 mock provider。必须执行 `ProviderTrustPolicy`、token budget、payload cap、memory cap、redaction guard、prompt injection guard 与 fail-closed。因为涉及 provider trust / security boundary，完成后需要 close review。

### B3: Persistence Baseline

规划并实现 `prompt_template`、`prompt_version`、`model_profile`、`model_version`、`model_gateway_call` 或等价表。因为涉及 migration，必须先 work order，再 review/freeze。B3 前不得实现 API / provider SDK / real HTTP。

### B4: QDR Decision Pipeline Integration

dry-run/QDR 接入 mock gateway，保持 structured output。不触发 approval / NQ / trading，不改变 read-only action vocabulary。因为涉及 decision pipeline，完成后需要 review。

### B5: Stage-qdr-3 Close Review

只做 final acceptance / close review，不实现新功能。

### Review 触发规则

不再每个普通 batch 单独 review。只在以下情况触发 review：

```text
migration / 表结构变化
API / Controller 变化
auth / tenant / HMAC / nonce / source allowlist 变化
provider trust / prompt injection / redaction / budget / audit fail-closed 变化
P0/P1 blocker fix
stage close / acceptance
```

普通 batch 只做 validation + minimal docs/current sync。

## 9. 安全边界

stage-qdr-3 明确禁止：

```text
real HTTP
real provider
API key
Provider SDK
OpenAI SDK
Anthropic SDK
Gemini SDK
Ollama SDK
LangGraph
AutoGen
CrewAI
NQ mutation
trading mutation
LIVE
raw prompt storage
raw provider response storage
prompt injection bypass
provider trust bypass
fallback to allow
automatic approval
approval status as execution signal
LONG_BIAS / SHORT_BIAS -> BUY / SELL
APPROVED -> BUY
REJECTED -> SELL
```

## 10. 验收标准

stage-qdr-3 plan 通过条件：

```text
stage-qdr-2 final close 已提交
本轮只改 docs
Model Gateway 边界清楚
Prompt / Model Version 边界清楚
ProviderTrustPolicy 统一入口清楚
no real provider / no real HTTP 明确
no Agent / no LangGraph 明确
audit / redaction / trace 要求明确
raw prompt / raw provider response 默认禁止保存
prompt injection guard 明确
fail-closed 明确
implementation batches 不过细
review 触发规则符合 discipline closeout
docs/current 已同步
quality validate 通过
forbidden scan 无 actual risk
```

## 11. Readiness decision

```text
STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN: DONE
ALLOW_STAGE_QDR_3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_3_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作：

```text
DH-STAGE-QDR-3-IMPLEMENTATION-WORK-ORDER
```

