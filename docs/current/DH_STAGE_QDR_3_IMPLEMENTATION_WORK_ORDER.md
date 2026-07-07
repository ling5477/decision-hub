# DH stage-qdr-3 Implementation Work Order

```text
Task: DH-STAGE-QDR-3-IMPLEMENTATION-WORK-ORDER
Task type: WORK_ORDER_ONLY + STAGE_QDR_3_IMPLEMENTATION_PLANNING + MODEL_GATEWAY_WORK_ORDER + PROMPT_VERSION_WORK_ORDER + PROVIDER_TRUST_SECURITY_BOUNDARY + AUDIT_REDACTION_TEST_MATRIX + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / WORK_ORDER_ONLY / B3_IMPLEMENTED_BY_VALIDATION
Fact source: docs/current
Date: 2026-07-07
```

## 1. 路径纪律与前置状态

```text
Current execution workspace: F:/project/decision-hub
Former alternate environment path: E:/Project/decision-hub
Path rule: 当前实际执行路径为 F:/project/decision-hub；此前 E:/Project/decision-hub 属于另一环境路径，不得混用。
F:/project/decision-hub exists: YES
E:/Project/decision-hub exists in this environment: NO
Branch: dev
HEAD requirement: docs(qdr): plan stage-qdr-3 model gateway baseline
```

stage-qdr-3 plan 已提交，HEAD 包含：

```text
docs(qdr): plan stage-qdr-3 model gateway baseline
```

当前阶段状态：

```text
stage-qdr-1: CLOSED / ACCEPTED
stage-qdr-2 final close: CLOSED / ACCEPTED
stage-qdr-3 model gateway prompt version plan: DONE
stage-qdr-3 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-3 implementation: B3_IMPLEMENTED_BY_VALIDATION
B1: COMMITTED / MOCK_ONLY / NO_API / NO_MIGRATION
B2: CLOSED / ACCEPTED / COMMITTED / MOCK_ONLY / NO_API / NO_MIGRATION
B3: IMPLEMENTED_BY_VALIDATION / V8_MIGRATION_JDBC_BASELINE / NO_API / REVIEW_FREEZE_REQUIRED
B4: NOT STARTED / DECISION_PIPELINE_REVIEW_REQUIRED
B5: NOT STARTED / CLOSE_REVIEW_ONLY
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

本工单已将 stage-qdr-3 implementation 拆成后续可执行批次。B1 已按 domain/usecase/mock registry 范围完成提交；B2 已按 mock gateway runtime / ProviderTrustPolicy / budget / redaction guard 范围完成提交；B3 已按 V8 migration / persistence ports / JDBC repositories / migration tests / JDBC tests / ArchitectureTest guard 范围完成实现与验证。B4/B5 仍未启动。本线不新增 API，不接真实 provider，不接真实 HTTP，不启动 Agent / LangGraph / LIVE。

## 2. Stage-qdr-3 批次冻结

stage-qdr-3 implementation 必须按以下 B1-B5 顺序推进，不得跳过 B1 直接进入 B2/B3/B4，也不得一次性全量实现。

### B1: Prompt / Model Version Domain + Mock Registry

状态：`IMPLEMENTED_BY_VALIDATION / MOCK_ONLY / NO_API / NO_MIGRATION / NO_REAL_PROVIDER / NO_REAL_HTTP`。

目标：

```text
新增 PromptTemplate / PromptVersion / ModelProfile / ModelVersion / ProviderProfile 等 domain/usecase contract
新增 immutable version / checksum / hash rule
新增 PromptVersionRegistryPort
新增 in-memory/mock registry
新增 PromptRenderPolicy contract
新增 PromptInjectionGuard contract
新增 validation tests 与 architecture tests
```

允许范围：

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/model/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/model/**
对应 tests
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java 的最小 guard
docs/current 最小同步
```

禁止：

```text
real provider
real HTTP
API / Controller / OpenAPI
migration
Provider SDK
LangGraph / AutoGen / CrewAI
NQ mutation
LIVE
raw prompt storage
raw provider response storage
trading mutation
```

Review 规则：B1 若只做 domain/usecase contracts、mock registry、validation tests 和 architecture guard，且不触及 migration、API、provider trust runtime 或 audit fail-closed 实质边界，则 implementation 后执行 validation + minimal docs/current sync + commit，不需要 standalone freeze。

### B2: Model Gateway Mock Runtime + Policy Guard

目标：

```text
新增 ModelGatewayPort / ModelProviderPort / ModelGatewayRequest / ModelGatewayResult
新增 ModelCallContext / ModelCallPolicy / ModelCallBudget / ModelCallRedactionPolicy / ModelCallAuditRef
新增 MockModelProvider
强制 ProviderTrustPolicy
强制 token budget / payload cap / memory cap / redaction guard / prompt injection guard
provider unavailable / policy denied / budget exceeded / redaction failure / prompt denied 全部 fail-closed
```

允许范围：

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/model/**
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway/**
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/model/**
app wiring config，仅 mock provider
tests
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
docs/current 最小同步
```

禁止：

```text
real HTTP
real provider
Provider SDK
OpenAI / Anthropic / Gemini / Ollama SDK
LangGraph / AutoGen / CrewAI
API / Controller / OpenAPI
migration
NQ mutation
LIVE
fallback to allow
```

Review 规则：B2 必须 review/freeze，因为涉及 `ProviderTrustPolicy`、gateway fail-closed、安全边界与 mock provider no-outbound 证明。

B2 当前结果：`CLOSED / ACCEPTED / COMMITTED`。B2 review/freeze 已关闭；当前下一步只允许 `DH-STAGE-QDR-3-B3-REVIEW-FREEZE`，不得直接进入 B4 pipeline integration 或 B5 close review。

### B3: Persistence Baseline

目标：

```text
规划并实现 prompt_template / prompt_version / model_profile / model_version / model_gateway_call 或等价表
raw prompt / raw provider response 默认禁止保存
保存 hash / ref / summary / redacted excerpt
新增 migration presence tests 与 JDBC repository tests
```

允许范围：

```text
dh-app/src/main/resources/db/migration/V8__*.sql
dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/model/**
repository ports
migration tests
JDBC tests
docs/current DB schema sync
```

禁止：

```text
修改 V1-V7 历史 migration
raw_prompt column
raw_response column
credential / token / cookie / apiKey / apiSecret / passphrase storage
API / Controller
real provider
real HTTP
Provider SDK
NQ mutation
LIVE
```

Review 规则：B3 必须 review/freeze，因为涉及 Flyway migration、表结构、索引、约束、字段注释和持久化安全边界。

B3 当前结果：`IMPLEMENTED_BY_VALIDATION / REVIEW_FREEZE_REQUIRED`。下一步只允许 `DH-STAGE-QDR-3-B3-REVIEW-FREEZE`；不得直接进入 B4 decision pipeline integration 或 B5 close review。

### B4: QDR Decision Pipeline Integration

目标：

```text
将 QDR decision pipeline 接入 mock gateway
保持 structured output
将 promptVersionRef / modelVersionRef / providerTrustDecisionRef / modelCallRef 写入 audit trace 或安全 ref
不触发 approval
不触发 NQ
不触发交易
不触发 real provider
```

允许范围：

```text
QDR dry-run / decision pipeline integration files
model gateway mock integration files
audit trace mapping files
tests
docs/current minimal sync
```

禁止：

```text
approval status as execution signal
automatic approval
replay execution
real HTTP
real provider
Provider SDK
NQ mutation
trading mutation
LIVE
BUY / SELL / PLACE_ORDER / CANCEL_ORDER as output action
```

Review 规则：B4 必须 review/freeze，因为涉及 decision pipeline integration、audit trace mapping、QDR output boundary 和 fail-closed 行为。

### B5: Stage-qdr-3 Close Review

目标：

```text
最终验收
冻结 B1-B4 evidence
确认 no real provider / no real HTTP / no Agent / no LangGraph / no LIVE
确认 raw prompt / raw response 禁止项
确认 ProviderTrustPolicy / fail-closed / audit / redaction / prompt injection guard 全部满足
```

禁止：

```text
实现新功能
补新 API
补 migration
补 provider client
补 HTTP client
启动 Agent / LangGraph / LIVE
```

Review 规则：B5 是 stage close review，必须独立完成，不得与 B4 implementation 混合。

## 3. Review 触发规则

```text
B1: 若只做 domain/usecase contracts，无 migration/API/provider trust runtime，则不需要 standalone freeze；implementation 后 validation + commit。
B2: 必须 review/freeze，因为涉及 ProviderTrustPolicy / gateway fail-closed。
B3: 必须 review/freeze，因为涉及 migration。
B4: 必须 review/freeze，因为涉及 decision pipeline integration。
B5: 必须 stage close review。
P0/P1 blocker fix: 必须复核。
普通 docs sync: 不再长 review。
```

普通 batch 不再默认 standalone 长 review。只有以下情况触发 review：

```text
migration / 表结构变化
API / Controller / OpenAPI 变化
auth / tenant / HMAC / nonce / source allowlist 变化
ProviderTrustPolicy / prompt injection / redaction / budget / audit fail-closed 变化
decision pipeline integration 变化
P0/P1 blocker fix
stage close / acceptance
```

## 4. B1 下一步任务草案

```text
Task: DH-STAGE-QDR-3-B1-PROMPT-MODEL-VERSION-DOMAIN-MOCK-REGISTRY
Task type: CONTROLLED_IMPLEMENTATION + PROMPT_MODEL_VERSION_DOMAIN + MOCK_REGISTRY + NO_DB_MIGRATION + NO_API + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
```

B1 目标：

```text
PromptTemplate
PromptVersion immutable
checksum / hash
ModelProfile
ModelVersion
ProviderProfile
PromptVersionRegistryPort
in-memory/mock registry
PromptRenderPolicy contract
PromptInjectionGuard contract
validation tests
architecture tests
```

B1 必须禁止：

```text
real provider
real HTTP
API
migration
Provider SDK
LangGraph
NQ
LIVE
raw prompt/raw provider response storage
trading mutation
```

B1 验收：

```text
PromptVersion 不可变，prompt change 必须新建 version
checksum/hash 必填且可验证
mock registry deterministic / in-memory / no outbound
PromptRenderPolicy contract 禁止 secret-like material 和 executable trading instruction
PromptInjectionGuard contract 覆盖 override / exfiltration / trading intent / raw prompt request / tool escalation
architecture tests 禁止 qdr model package 引入 HTTP client / provider SDK / LangGraph / NQ client
no-real-http scan 通过
no-provider-SDK scan 通过
no-live-order scan 通过
```

## 5. 安全门禁

stage-qdr-3 implementation 必须遵守：

```text
所有 provider 调用未来必须经过 ModelGateway
ModelGateway 必须经过 ProviderTrustPolicy
业务代码不得直连 provider
real provider disabled by default
no API key / credential in docs/examples
no raw prompt/raw response storage
prompt injection guard fail-closed
redaction failure fail-closed
budget exceeded fail-closed
provider unavailable fail-closed
audit failure fail-closed
LONG_BIAS / SHORT_BIAS 不映射 BUY / SELL
APPROVED 不映射 BUY
REJECTED 不映射 SELL
approval status 不是 execution signal
Agent / LangGraph 后置
```

任何失败路径不得 fallback to allow；不得因为 mock provider 成功而放宽真实 provider、真实 HTTP、NQ mutation 或 LIVE 边界。

## 6. 测试矩阵

stage-qdr-3 后续 implementation 必须覆盖：

```text
domain validation tests
checksum immutability tests
prompt render policy tests
injection guard tests
mock registry tests
provider trust enforcement tests
budget exceeded tests
redaction failure tests
architecture boundary tests
migration presence tests for B3
JDBC repository tests for B3
QDR integration tests for B4
no-real-http scan
no-provider-SDK scan
no-live-order scan
```

B1 最低测试：

```text
PromptVersion immutable / checksum tests
PromptTemplate version pointer validation
ModelProfile / ModelVersion validation
ProviderProfile mock-only validation
PromptVersionRegistryPort in-memory behavior tests
PromptRenderPolicy deny secret-like material tests
PromptInjectionGuard deny override/trade/raw prompt/tool escalation tests
ArchitectureTest qdr model no outbound/no provider SDK guard
```

B2 最低测试：

```text
ProviderTrustPolicy enforced before provider call
mock provider deterministic output
provider unavailable fail-closed
budget exceeded fail-closed
redaction failure fail-closed
prompt denied fail-closed
no real HTTP / no provider SDK scan
```

B3 最低测试：

```text
V8 migration presence tests
COMMENT ON TABLE / COMMENT ON COLUMN presence tests
tenant_id / trace_id / request_id / decision_run_id presence
raw_prompt / raw_response absence tests
unique/index/check constraints tests
JDBC repository redacted summary / hash / ref persistence tests
```

B4 最低测试：

```text
QDR mock gateway integration tests
structured output only tests
audit trace ref mapping tests
prompt/model version trace ref tests
no approval trigger tests
no NQ mutation tests
no trading action tests
```

## 7. 文件边界总表

| Batch | 允许修改 | 必须禁止 | Review |
| --- | --- | --- | --- |
| B1 | `dh-domain/**/qdr/model/**`, `dh-usecase/**/qdr/model/**`, tests, `ArchitectureTest`, docs/current | API, migration, HTTP, provider SDK, NQ, LIVE | 不需要 standalone freeze，除非触发安全边界变化 |
| B2 | `dh-usecase/**/qdr/model/**`, `dh-usecase/**/qdr/gateway/**`, `dh-domain/**/qdr/model/**`, mock-only app wiring, tests, `ArchitectureTest`, docs/current | real HTTP, real provider, SDK, API, migration, NQ, LIVE | 必须 review/freeze |
| B3 | `V8__*.sql`, `dh-infra/**/jdbc/qdr/model/**`, repository ports, migration/JDBC tests, DB schema docs | 修改 V1-V7, raw columns, API, real provider, HTTP | 必须 review/freeze |
| B4 | QDR dry-run / decision pipeline integration, mock gateway integration, audit trace mapping, tests, docs/current | approval trigger, replay execution, NQ/trading, real provider/HTTP | 必须 review/freeze |
| B5 | docs/current close review / evidence | 新功能、代码、migration、API、provider | stage close review |

## 8. Readiness decision

```text
STAGE_QDR_3_IMPLEMENTATION_WORK_ORDER: DONE
ALLOW_STAGE_QDR_3_B1_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_3_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_3_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_3_B4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作：

```text
DH-STAGE-QDR-3-B1-PROMPT-MODEL-VERSION-DOMAIN-MOCK-REGISTRY
```
