# DH-NQ Integration-0 Contract Test Plan

> 任务：NQ-DH-INTEGRATION-0-CONTRACT-FREEZE
> 类型：DOCUMENTATION + CONTRACT DESIGN
> 日期：2026-06-11
> 仓库视角：Decision Hub（DH）
> 配套：`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md`、`DH_NQ_INTEGRATION0_SECURITY_POLICY.md`

本文件**只写测试计划，不写测试代码**。所有用例在 Integration-0 阶段只允许 mock / stub / contract test，**禁止真实 HTTP、禁止真实交易副作用、禁止真实凭证访问、禁止 RealClient**。与 NQ 仓库 `NQ_DH_INTEGRATION0_CONTRACT_TEST_PLAN.md` 口径一致。

---

## 1. 测试范围与原则

- 目标：验证契约面（schema / header / 签名 / replay / tenant / payload / forbidden field / 审计 / 无副作用），不验证实盘收益。
- 实现方式：mock server / stub / contract test（WireMock / MockWebServer 或等价）；DH 侧用既有 Fake / Disabled client，禁止 RealClient、禁止真实 HTTP。
- 数据：deterministic 固定数据；tenant 用 `t-test-*` 前缀；禁止真实账户、真实凭证。
- 每个用例字段：`testName / targetSystem / purpose / input / expectedResult / forbiddenSideEffect / whetherBlocksIntegration0`。

## 2. 必测用例（15 项，冻结）

### T1 禁止能力 contract test
- targetSystem：NQ（DH 侧验证 Disabled/Fake 不触达）
- purpose：DH 无法触达下单/撤单/Paper 启停/策略状态/风控/凭证/DB。
- input：mock 尝试调用禁止能力（契约层占位）。
- expectedResult：契约层 forbidden，403/423；DH DisabledClient 返回 DISABLED 不抛异常。
- forbiddenSideEffect：任何下单/撤单/状态变更/凭证读取/DB 写入。
- whetherBlocksIntegration0：是。

### T2 可开放能力 contract test
- targetSystem：DH / NQ
- purpose：可开放只读/候选能力契约结构正确。
- input：mock 合法候选信号 / 只读摘要。
- expectedResult：schema 校验通过，candidate-only / 只读摘要。
- forbiddenSideEffect：进入交易执行路径。
- whetherBlocksIntegration0：是。

### T3 header 缺失测试
- targetSystem：NQ
- purpose：任一 required header 缺失被拒绝。
- input：分别缺 Source/Tenant-Id/Request-Id/Trace-Id/Timestamp/Nonce/Signature。
- expectedResult：缺认证类 401；来源/租户 403；幂等/追踪 400。
- forbiddenSideEffect：任何写入或执行。
- whetherBlocksIntegration0：是。

### T4 HMAC 签名失败测试
- targetSystem：NQ / DH（DH NQ feedback authenticator 已有覆盖）
- purpose：错误/缺失签名被拒绝。
- input：错误签名、空签名、错 secret。
- expectedResult：401/403；审计不记录签名原材料。
- forbiddenSideEffect：执行；签名原材料落日志。
- whetherBlocksIntegration0：是。

### T5 timestamp 过期测试
- targetSystem：NQ / DH
- purpose：超 ±300 秒窗口拒绝。
- input：早于/晚于窗口的 timestamp。
- expectedResult：401/403。
- forbiddenSideEffect：接受过期请求。
- whetherBlocksIntegration0：是。

### T6 nonce replay 测试
- targetSystem：NQ / DH
- purpose：重复 nonce/requestId 被拒绝。
- input：同 `Source+Nonce+RequestId` 两次。
- expectedResult：首次接受（mock），重放 409。
- forbiddenSideEffect：重复执行/写入。
- whetherBlocksIntegration0：是（注：持久化 nonce 是 Integration-1 前置；Integration-0 用单实例 mock 验证逻辑即可）。

### T7 tenant mismatch 测试
- targetSystem：NQ / DH
- purpose：header tenant 与认证主体不一致被拒绝。
- input：认证 tenant 与 `X-NQ-DH-Tenant-Id` 不一致。
- expectedResult：403。
- forbiddenSideEffect：跨租户读写。
- whetherBlocksIntegration0：是。

### T8 payload 超 64 KiB 测试
- targetSystem：NQ / DH
- purpose：超限拒绝不截断。
- input：> 65536 bytes body。
- expectedResult：413。
- forbiddenSideEffect：截断后接受。
- whetherBlocksIntegration0：是。

### T9 forbidden field rejection 测试
- targetSystem：DH / NQ
- purpose：契约出现禁止字段被拒绝。
- input：payload 含 apiKey/secret/token/privateKey/mnemonic 等。
- expectedResult：拒绝（FORBIDDEN_FIELD）。
- forbiddenSideEffect：禁止字段落日志/落库/回显。
- whetherBlocksIntegration0：是。

### T10 raw prompt / context rejection 测试
- targetSystem：DH / NQ
- purpose：契约禁止携带 full prompt / full context / raw request / raw response。
- input：payload 含 fullPrompt/fullContext/rawRequest/rawResponse。
- expectedResult：拒绝。
- forbiddenSideEffect：prompt/context 落库或外发。
- whetherBlocksIntegration0：是。

### T11 candidate signal schema validation 测试
- targetSystem：NQ（DH 侧生成契约）
- purpose：DHSignalCandidate schema 校验。
- input：合法/非法（缺字段、错枚举、confidence 越界、candidateOnly≠true）。
- expectedResult：合法接受为候选；非法 400。
- forbiddenSideEffect：非法信号进入执行。
- whetherBlocksIntegration0：是。

### T12 NQ feedback event schema validation 测试
- targetSystem：DH
- purpose：NQFeedbackEvent schema 校验（DH 已有 envelope/validator/router 基线）。
- input：合法/非法（缺 eventId、错 eventType、sourceSystem≠NQ）。
- expectedResult：合法 ingest；非法 400。
- forbiddenSideEffect：DH 反写 NQ。
- whetherBlocksIntegration0：是。

### T13 audit log required test
- targetSystem：DH / NQ
- purpose：接收/拒绝/限流/重放/provider 出站决策必须落审计。
- input：触发各类路径。
- expectedResult：审计存在且脱敏（无密钥/签名原材料/raw payload/raw baseURL）。
- forbiddenSideEffect：审计缺失或记录敏感值。
- whetherBlocksIntegration0：是。

### T14 no trading side-effect test
- targetSystem：NQ / DH
- purpose：任何契约请求不得产生交易副作用。
- input：全部可开放能力请求（mock/Fake/Disabled）。
- expectedResult：无订单/撤单/Paper 启停/策略状态/风控状态变更。
- forbiddenSideEffect：任何交易副作用。
- whetherBlocksIntegration0：是。

### T15 no credential access test
- targetSystem：NQ / DH
- purpose：任何契约请求不得触达凭证/交易所 secret/DB。
- input：尝试触达 credential / exchange secret / DB（契约层占位）。
- expectedResult：无可达路径；契约层拒绝。
- forbiddenSideEffect：读取/输出任何凭证或 secret。
- whetherBlocksIntegration0：是。

## 3. 测试矩阵汇总

| 用例 | 目标 | 阻塞 Integration-0 | 禁止副作用核心 |
| --- | --- | --- | --- |
| T1 | NQ/DH | 是 | 无执行/凭证/DB |
| T2 | DH/NQ | 是 | 不进执行路径 |
| T3 | NQ | 是 | 无写入/执行 |
| T4 | NQ/DH | 是 | 签名材料不落日志 |
| T5 | NQ/DH | 是 | 不接受过期 |
| T6 | NQ/DH | 是 | 不重复执行 |
| T7 | NQ/DH | 是 | 不跨租户 |
| T8 | NQ/DH | 是 | 不截断接受 |
| T9 | DH/NQ | 是 | 禁止字段不落库 |
| T10 | DH/NQ | 是 | prompt/context 不外发 |
| T11 | NQ | 是 | 非法信号不进执行 |
| T12 | DH | 是 | DH 不反写 NQ |
| T13 | DH/NQ | 是 | 审计不漏不泄敏 |
| T14 | NQ/DH | 是 | 无交易副作用 |
| T15 | NQ/DH | 是 | 无凭证访问 |

## 4. 实现约束（冻结）

- 本轮**不实现**任何测试代码；仅冻结测试计划。
- 未来实现时必须：全部 mock / stub / Fake / Disabled，禁止真实 HTTP / RealClient / 真实 NQ / 真实交易所；用 `t-test-*` tenant；禁止真实凭证。
- 任一阻塞 Integration-0 的用例不通过，契约冻结视为无效，必须修订契约。
- DH 侧测试必须用 `@EnabledIfEnvironmentVariable` 或等价隔离，默认 profile 不触发真实通道。

## 5. Integration-1 前置（不在本轮）

- DH P1-4 残留（rate limit / memory cap / replay nonce 持久化）必须先修复，才允许 Integration-1 真实只读接入 / 真实通道 contract test。
- 真实通道测试必须隔离 staging / test cluster，Paper-only，LIVE 关闭。

---

# 详细 mock / contract test 设计（NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN）

> 本节为 2026-06-11 增补，与 NQ 仓库 `NQ_DH_INTEGRATION0_CONTRACT_TEST_PLAN.md` 第 6-12 节口径一致，DH 视角。
> 仍然**只做设计，不写测试代码**；`futureCodeLocationSuggestion` 只是建议路径，不得创建代码文件。
> 全部 mock / stub / Fake / Disabled 隔离，禁止真实 HTTP / 真实 NQ / RealClient / 真实 Provider / 真实交易所 / 真实凭证。

## 6. 公共约定

- canonical headers：`X-NQ-DH-Source / X-NQ-DH-Tenant-Id / X-NQ-DH-Request-Id / X-NQ-DH-Trace-Id / X-NQ-DH-Timestamp / X-NQ-DH-Nonce / X-NQ-DH-Signature` + `Content-Type: application/json`。
- 拒绝码：`400 / 401 / 403 / 409 / 413 / 423 / 429`（语义同 NQ 侧）。
- 审计事件命名：`REQUEST_RECEIVED / REQUEST_REJECTED / SIGNATURE_FAILED / REPLAY_REJECTED / TENANT_MISMATCH / PAYLOAD_TOO_LARGE / FORBIDDEN_FIELD_REJECTED / RATE_LIMITED / IDEMPOTENCY_CONFLICT / RISK_RESULT_RECORDED`。
- tenant 固定 `t-test-*`；secret 用测试占位。
- DH 侧测试默认用既有 Fake / Disabled client + `@EnabledIfEnvironmentVariable` 隔离，不触发真实通道。
- `blocksIntegration0=true`：不通过则契约冻结无效。`blocksIntegration1=true`：进入/通过 Integration-1 的强制门禁，真实模式重跑。

## 7. DH Contract Test Matrix（DH 为 feedback 受测方 + 候选产出方）

DH 侧重点：NQ feedback 入站认证/校验（DH 已有 envelope/validator/router + HMAC authenticator 基线）、候选契约产出结构、Disabled/Fake client 不触达真实 NQ、无凭证访问。

### INT0-T01 禁止能力 contract test（DH 侧）
- testName：forbidden_capability_disabled_client
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：DH connector 在禁止能力上只能走 DisabledClient，不触达真实 NQ。
- inputFixture：`FX-FORBIDDEN-CALLS`。
- requiredHeaders：N/A（DH 出站默认 disabled）。
- payload：尝试触发下单/撤单/Paper 启停等。
- expectedStatus：DisabledClient 返回 DISABLED（不抛异常、不发 HTTP）。
- expectedResult：无真实 NQ 调用；无副作用。
- expectedAuditEvent：`REQUEST_REJECTED`（DH 本地）。
- forbiddenSideEffect：真实 HTTP / 下单 / 撤单 / 状态变更。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-app/src/test/java/.../integration0/ForbiddenCapabilityDisabledClientTest.java`

### INT0-T02 可开放能力 contract test（DH 侧）
- testName：allowed_capability_contract_dh
- targetSystem：DH
- testType：CONTRACT
- purpose：DH 产出候选契约（`DHSignalCandidate` 等）结构正确，candidateOnly=true。
- inputFixture：`FX-CANDIDATE-VALID`。
- requiredHeaders：契约层定义（出站走 Fake）。
- payload：合法 `DHSignalCandidate`。
- expectedResult：Fake client 返回受控响应；结构通过 schema。
- expectedStatus：Fake 202/200。
- expectedAuditEvent：`REQUEST_RECEIVED`。
- forbiddenSideEffect：进入真实执行路径 / 真实 HTTP。
- blocksIntegration0：true ｜ blocksIntegration1：false
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-usecase/src/test/java/.../integration0/AllowedCapabilityContractTest.java`

### INT0-T03 header 缺失测试（DH feedback 入站）
- testName：missing_header_rejection_dh
- targetSystem：DH
- testType：NEGATIVE / SECURITY
- purpose：DH `/api/ai/feedback/nq` 入站任一 required header 缺失必须拒绝。
- inputFixture：`FX-HEADER-MATRIX`。
- requiredHeaders：逐项缺失。
- payload：合法 `NQFeedbackEvent`。
- expectedStatus：缺认证 401；缺 Source/Tenant 403；缺 Request-Id/Trace-Id 400。
- expectedResult：拒绝；不构造 ingestion command。
- expectedAuditEvent：`REQUEST_REJECTED`。
- forbiddenSideEffect：入库或反写 NQ。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-api/src/test/java/.../integration0/HeaderPresenceContractTest.java`

### INT0-T04 HMAC 签名失败测试（DH 侧，已有基线）
- testName：hmac_signature_failure_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：NQ feedback 错误/缺失签名拒绝；签名原材料不落日志。复用现有 `HmacNqFeedbackAuthenticatorTest` 思路。
- inputFixture：`FX-BAD-SIGNATURE`。
- requiredHeaders：Signature 非法。
- payload：合法 `NQFeedbackEvent`。
- expectedStatus：401/403。
- expectedResult：认证失败不入库；不记录签名原材料。
- expectedAuditEvent：`SIGNATURE_FAILED`。
- forbiddenSideEffect：签名原材料落日志/落库；入库。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-security/src/test/java/.../integration0/HmacSignatureContractTest.java`

### INT0-T05 timestamp 过期测试（DH 侧）
- testName：timestamp_window_rejection_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：feedback timestamp 超 ±300s 窗口拒绝。
- inputFixture：`FX-TIMESTAMP`。
- requiredHeaders：Timestamp 越界。
- payload：合法 `NQFeedbackEvent`。
- expectedStatus：401/403。
- expectedResult：拒绝过期。
- expectedAuditEvent：`REQUEST_REJECTED`。
- forbiddenSideEffect：接受过期。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-security/src/test/java/.../integration0/TimestampWindowContractTest.java`

### INT0-T06 nonce replay 测试（DH 侧）
- testName：nonce_replay_rejection_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：feedback `Source+Nonce+RequestId` 重放拒绝（409）。
- inputFixture：`FX-REPLAY`。
- requiredHeaders：第二次 Nonce/RequestId 复用。
- payload：合法 `NQFeedbackEvent`。
- expectedStatus：首次 202，重放 409。
- expectedResult：重放拒绝；业务层 eventId 幂等可返回 DUPLICATE，但认证层 nonce 先拒。
- expectedAuditEvent：`REPLAY_REJECTED`。
- forbiddenSideEffect：重复入库/重复处理。
- blocksIntegration0：true ｜ blocksIntegration1：true（Int-1 须以**持久化 nonce** 多实例重跑——P1-4 residual）
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-security/src/test/java/.../integration0/NonceReplayContractTest.java`

### INT0-T07 tenant mismatch 测试（DH 侧）
- testName：tenant_binding_mismatch_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：header tenant 与认证主体不一致拒绝（DH 已移除硬编码默认 tenant）。
- inputFixture：`FX-TENANT-MISMATCH`。
- requiredHeaders：tenant 不一致。
- payload：合法 `NQFeedbackEvent`。
- expectedStatus：403。
- expectedResult：拒绝；按认证 tenant 入库，禁止跨租户。
- expectedAuditEvent：`TENANT_MISMATCH`。
- forbiddenSideEffect：跨租户入库。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-api/src/test/java/.../integration0/TenantBindingContractTest.java`

### INT0-T08 payload 超 64 KiB 测试（DH 侧）
- testName：payload_size_limit_dh
- targetSystem：DH
- testType：NEGATIVE / SECURITY
- purpose：feedback payload 超 65536 bytes 拒绝不截断。
- inputFixture：`FX-OVERSIZE-PAYLOAD`。
- requiredHeaders：全 7。
- payload：超限 body。
- expectedStatus：413。
- expectedResult：拒绝不截断。
- expectedAuditEvent：`PAYLOAD_TOO_LARGE`。
- forbiddenSideEffect：截断后接受。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-security/src/test/java/.../integration0/PayloadSizeContractTest.java`

### INT0-T09 forbidden field rejection 测试（BOTH）
- testName：forbidden_field_rejection_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：契约/payload 含禁止字段拒绝；不落库。
- inputFixture：`FX-FORBIDDEN-FIELDS`。
- requiredHeaders：全 7。
- payload：含禁止字段。
- expectedStatus：400（FORBIDDEN_FIELD）。
- expectedResult：拒绝；禁止字段不落库/不回显。
- expectedAuditEvent：`FORBIDDEN_FIELD_REJECTED`。
- forbiddenSideEffect：禁止字段落库/落日志。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：BOTH
- futureCodeLocationSuggestion：`dh-domain/src/test/java/.../integration0/ForbiddenFieldContractTest.java`

### INT0-T10 raw prompt / context rejection 测试（BOTH）
- testName：raw_prompt_context_rejection_dh
- targetSystem：DH
- testType：SECURITY / NEGATIVE
- purpose：禁止携带 fullPrompt/fullContext/rawRequest/rawResponse。
- inputFixture：`FX-RAW-PROMPT`。
- requiredHeaders：全 7。
- payload：含 raw prompt/context。
- expectedStatus：400（FORBIDDEN_FIELD）。
- expectedResult：拒绝；prompt/context 不落库不外发。
- expectedAuditEvent：`FORBIDDEN_FIELD_REJECTED`。
- forbiddenSideEffect：prompt/context 落库或外发 provider。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：BOTH
- futureCodeLocationSuggestion：`dh-domain/src/test/java/.../integration0/RawPromptRejectionContractTest.java`

### INT0-T11 candidate signal schema validation 测试（DH 产出）
- testName：candidate_signal_schema_dh
- targetSystem：DH
- testType：CONTRACT / NEGATIVE
- purpose：DH 产出 `DHSignalCandidate` schema 合法/非法校验。
- inputFixture：`FX-CANDIDATE-VALID` / `FX-CANDIDATE-INVALID`。
- requiredHeaders：契约层。
- payload：合法/非法候选。
- expectedStatus：合法通过；非法 400。
- expectedResult：非法不产出/不提交。
- expectedAuditEvent：合法 `REQUEST_RECEIVED`；非法 `REQUEST_REJECTED`。
- forbiddenSideEffect：非法候选进入真实提交。
- blocksIntegration0：true ｜ blocksIntegration1：false
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-domain/src/test/java/.../integration0/CandidateSignalSchemaContractTest.java`

### INT0-T12 NQ feedback event schema validation 测试（DH 主负责）
- testName：nq_feedback_event_schema_dh
- targetSystem：DH
- testType：CONTRACT / NEGATIVE
- purpose：`NQFeedbackEvent` schema 校验（DH 已有 envelope/validator/router 基线，可扩展）。
- inputFixture：`FX-FEEDBACK-VALID` / `FX-FEEDBACK-INVALID`。
- requiredHeaders：全 7。
- payload：合法/非法 feedback。
- expectedStatus：合法 202；非法 400。
- expectedResult：合法 ingest；非法拒绝；DH 不反写 NQ。
- expectedAuditEvent：合法 `REQUEST_RECEIVED`；非法 `REQUEST_REJECTED`。
- forbiddenSideEffect：DH 反写 NQ。
- blocksIntegration0：true ｜ blocksIntegration1：false
- implementationOwner：DH
- futureCodeLocationSuggestion：`dh-domain/src/test/java/.../integration0/NqFeedbackEventSchemaContractTest.java`

### INT0-T13 audit log required test（BOTH）
- testName：audit_log_required_dh
- targetSystem：DH
- testType：SECURITY / CONTRACT
- purpose：接收/拒绝/replay/provider 出站决策必须落审计且脱敏。
- inputFixture：`FX-AUDIT-PATHS`。
- requiredHeaders：按子用例。
- payload：按子用例。
- expectedStatus：随路径。
- expectedResult：审计存在且脱敏（无密钥/签名原材料/raw payload/raw baseURL）。
- expectedAuditEvent：对应事件均生成。
- forbiddenSideEffect：审计缺失或记录敏感值。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：BOTH
- futureCodeLocationSuggestion：`dh-observability/src/test/java/.../integration0/AuditLogRequiredContractTest.java`

### INT0-T14 no trading side-effect test（BOTH）
- testName：no_trading_side_effect_dh
- targetSystem：DH
- testType：SECURITY
- purpose：DH 任何契约请求不得产生真实交易副作用（Disabled/Fake 闭环）。
- inputFixture：`FX-ALL-ALLOWED`。
- requiredHeaders：契约层。
- payload：各可开放能力合法 body。
- expectedStatus：Fake/Disabled 受控返回。
- expectedResult：无真实 NQ 调用、无下单/撤单/Paper/策略/风控变更。
- expectedAuditEvent：`REQUEST_RECEIVED`（无执行类事件）。
- forbiddenSideEffect：任何真实交易副作用 / 真实 HTTP。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：BOTH
- futureCodeLocationSuggestion：`dh-app/src/test/java/.../integration0/NoTradingSideEffectContractTest.java`

### INT0-T15 no credential access test（BOTH）
- testName：no_credential_access_dh
- targetSystem：DH
- testType：SECURITY
- purpose：DH 任何契约请求不得触达交易所 secret / NQ credential / NQ DB。
- inputFixture：`FX-CRED-PROBE`。
- requiredHeaders：契约层。
- payload：探测性 body。
- expectedStatus：拒绝 / 无可达路径。
- expectedResult：无凭证读取；无 NQ DB 访问。
- expectedAuditEvent：`REQUEST_REJECTED`。
- forbiddenSideEffect：读取/输出任何凭证或 secret；访问 NQ DB。
- blocksIntegration0：true ｜ blocksIntegration1：true
- implementationOwner：BOTH
- futureCodeLocationSuggestion：`dh-app/src/test/java/.../integration0/NoCredentialAccessContractTest.java`

## 8. Shared Fixture List

与 NQ 仓库 `NQ_DH_INTEGRATION0_CONTRACT_TEST_PLAN.md` §8 共用同一组 fixture（fixtureId 一致）：

```text
FX-HEADERS-VALID / FX-CANDIDATE-VALID / FX-CANDIDATE-INVALID / FX-READONLY-QUERY /
FX-FEEDBACK-VALID / FX-FEEDBACK-INVALID / FX-HEADER-MATRIX / FX-BAD-SIGNATURE /
FX-TIMESTAMP / FX-REPLAY / FX-TENANT-MISMATCH / FX-OVERSIZE-PAYLOAD /
FX-FORBIDDEN-FIELDS / FX-RAW-PROMPT / FX-FORBIDDEN-CALLS / FX-CRED-PROBE /
FX-ALL-ALLOWED / FX-AUDIT-PATHS
```

约束：全部占位/脱敏；禁止真实 API key/secret/token/凭证；tenant=`t-test-*`；签名 secret 为测试占位。DH 侧 fixture 建议放 `dh-domain/src/test/resources/integration0/`（实现阶段创建，本轮不创建）。

## 9. Forbidden Side-Effect Checklist

每个 DH 用例运行后必须断言以下为 0 / 未发生：

```text
[ ] 无真实 HTTP 出站（RealClient / RestTemplate / WebClient / OkHttp 计数为 0）
[ ] 无真实 NQ 调用
[ ] 无 RealNqBacktestClient 装配
[ ] 无下单 / 撤单 / Paper 启停 / 策略状态 / 风控状态变更
[ ] 无交易所凭证 / NQ credential 读取
[ ] 无 NQ DB 读/写
[ ] 无真实 Provider / 真实 LLM 调用
[ ] 无 LIVE 触发
[ ] 无禁止字段 / 签名原材料 / raw payload / prompt / context 落库
[ ] 无跨租户数据访问
[ ] 无 DH 反写 NQ
```

## 10. Integration-0 Acceptance Checklist

```text
[ ] T01..T15 DH 侧设计完整（16 字段齐全）
[ ] 所有 blocksIntegration0=true 用例未来实现后必须全绿
[ ] 全部 mock / stub / Fake / Disabled，无真实 HTTP / NQ / 交易所 / 凭证
[ ] @EnabledIfEnvironmentVariable 或等价隔离，默认 profile 不触发真实通道
[ ] shared fixture 脱敏、tenant=t-test-*、无真实密钥
[ ] forbidden side-effect checklist 纳入每个用例断言
[ ] 本轮 docs-only，未写测试代码 / 未改业务代码 / 未改 contracts schema / 未改 migration
```

## 11. Integration-1 Blocker Checklist

进入 Integration-1 前必须额外满足（DH P1-4 residual，本轮不修复）：

```text
[ ] rate limit：新增 429 限流 contract test；DH InMemory 无限流，须补租户/能力级限流
[ ] memory cap：dh-memory 5 Store 与 Stage2/Stage3 InMemory 仓储上限/外部存储就绪后补容量测试
[ ] replay nonce persistence：T06 须以持久化/集中缓存 nonce 多实例重跑
[ ] header X-DH-NQ-* 与 X-NQ-DH-* 对齐验证
[ ] 真实通道隔离 staging / test cluster，Paper-only，LIVE 关闭
```

## 12. Next Implementation Task Draft（草案，本轮不执行）

> 仅作为后续“写测试代码”任务的输入材料，本轮不创建任何代码文件。

```text
任务名（草案）：NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL（DH 侧）
类型：CODE_CHANGE（测试代码）+ CONTRACT TEST
前置：本设计文档冻结
范围（DH 仓库）：
  - dh-domain / dh-api / dh-security / dh-observability / dh-app src/test 下实现
    T01..T15 的 DH 侧 contract/security test，使用既有 Fake / Disabled client、
    MockMvc / mock 仓储；扩展现有 NqFeedback*Test、HmacNqFeedbackAuthenticatorTest。
  - 新增 dh-domain/src/test/resources/integration0/ fixture（脱敏占位）。
硬约束：
  - 不新增 API / Controller / Service / Repository / DTO / migration（只加测试与 fixture）。
  - 不接真实 HTTP / RealClient / RealNqBacktestClient / 真实 Provider / 真实交易所 / LIVE。
  - 默认 profile 不触发真实通道；@EnabledIfEnvironmentVariable 隔离真实联调用例。
  - 全部 blocksIntegration0=true 用例必须通过。
验收：
  - DH mvn test -Dtest='!PostgresContainerSmokeTest' 全绿（含新增 contract test）。
  - forbidden side-effect checklist 全部断言为 0。
  - ArchUnit 规则保持全绿，无新增对 RealClient / providers 的依赖。
不包含：
  - rate limit / memory cap / persistent replay nonce 的真实实现（属 Integration-1）。
```
