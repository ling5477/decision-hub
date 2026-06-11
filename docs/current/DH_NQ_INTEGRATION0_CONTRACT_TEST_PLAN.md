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
