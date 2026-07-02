# DH-NQ Integration-0 Security Policy

> 任务：NQ-DH-INTEGRATION-0-CONTRACT-FREEZE
> 类型：DOCUMENTATION + CONTRACT DESIGN
> 日期：2026-06-11
> 仓库视角：Decision Hub（DH）
> 配套：`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md`、`DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`

本文件冻结 DH-NQ Integration-0 安全策略：header / 签名 / 防重放 / tenant / trace / payload / 脱敏 / 审计。**只做契约设计，不实现代码。** 与 NQ 仓库 `NQ_DH_INTEGRATION0_SECURITY_POLICY.md` 口径一致。

---

## 1. 安全总原则

- DH → NQ 的所有输出对 NQ 而言是不可信输入；DH 必须按此设计，不得期望 NQ 信任。
- 默认 fail-closed：缺认证/签名/tenant/source 任一要素一律被 NQ 拒绝。
- 默认所有真实通道关闭；Integration-0 只允许 mock / stub / contract test；DH connector 默认 Fake / Disabled。
- 最小权限：DH 默认只有候选提交与只读摘要消费能力，无任何执行能力。
- 凭证零暴露：密钥、签名原材料、raw payload、prompt 不得落日志、不得落库、不得回显。

## 2. Required Headers（冻结）

```text
X-NQ-DH-Source         来源系统标识，必须命中 allowlist
X-NQ-DH-Tenant-Id      租户标识，必须与认证主体绑定
X-NQ-DH-Request-Id     幂等键
X-NQ-DH-Trace-Id       端到端追踪键
X-NQ-DH-Timestamp      请求时间戳（冻结为 RFC3339 / ISO-8601 UTC，必须 Z 结尾，例 2026-06-15T12:34:56Z）
X-NQ-DH-Nonce          一次性随机值
X-NQ-DH-Signature      HMAC-SHA256 签名（候选方案）
Content-Type: application/json
```

任一缺失 → 被拒绝（缺认证类 401；缺来源/租户类 403；缺幂等/追踪类 400）。

## 3. Authentication & Signature（冻结）

- 服务间认证：`Authorization: Bearer <service-token>` 或 mTLS；生产建议 mTLS + HMAC 双层。
- 签名算法：HMAC-SHA256（Integration-0 候选）。
- 签名原材料（canonical string）至少含：`Source / Tenant-Id / Request-Id / Trace-Id / Timestamp / Nonce / sha256(body)`。
- token 哈希存储，禁止明文 token 入库或日志。
- 签名不匹配 → 拒绝；落审计但不记录签名原材料。

## 4. Timestamp & Replay Protection（冻结）

- Timestamp 格式（canonical）：**RFC3339 / ISO-8601 UTC，必须 `Z` 结尾**，例 `2026-06-15T12:34:56Z`；不接受 epoch 秒/毫秒，不接受带数字时区偏移（如 `+08:00`）。
- Timestamp 窗口：默认 ±300 秒，超窗拒绝。
- 实现现状（诚实声明）：DH 生产 `HmacNqFeedbackAuthenticator` 已在 `Instant.parse` 前要求 header 文本以 UTC `Z` 结尾，并以 `Instant.toString()` 归一化为 UTC `Z` 形参与验签；HMAC signatureMaterial 仍 **value-based、不含 header name**（timestamp 以归一化值入签）。DH INT0 validator 与 NQ INT0 companion 均同步拒绝 epoch 秒、epoch 毫秒和数字时区偏移；timestamp alignment overall 已 **CLOSED / ACCEPTED**。CLOSED 仅表示 timestamp 契约收口完成，不授权 Integration-1 runtime。
- Nonce 防重放：`Source + Nonce + Request-Id` 在 TTL 内唯一；重放拒绝（409）。
- Nonce TTL：≥ 2 × maxClockSkew。
- **Integration-1 前置（DH P1-4 残留，本轮不修复）**：nonce 必须持久化或集中缓存，不能只依赖单实例内存。

## 5. Tenant / Trace / Request 绑定（冻结）

- `tenantId` 必须与认证主体绑定，禁止仅信任请求体；约束数据与审计作用域。
- `traceId` 端到端追踪主键，必须可在 DH ResearchRun / 审计记录中命中。
- `requestId` 幂等键；DH 发起的请求必须原样回流；同 requestId 换 payload → 409。
- `correlationId`（可选）关联一组事件，不与 traceId / requestId 混用。

## 6. Payload 策略（冻结）

- payload ≤ 64 KiB（65536 bytes），超限拒绝（413），不截断。
- 大对象禁止外发，只传摘要、指标、引用 ID。
- Content-Type 必须 `application/json`。

## 7. 脱敏与禁止外发数据（冻结）

禁止 DH 向任何第三方 provider 外发，或在契约中携带：

```text
交易所 API Key / Secret / Passphrase / token / cookie / 私钥 / 连接串
NQ / DH 服务 token、HMAC secret、JWT、数据库 DSN
账户余额全量、可识别真实账户身份字段、原始 credential payload
LIVE 真实订单全量、成交全量、交易所原始响应全量
未脱敏错误堆栈、内部路径、SQL、服务拓扑
full prompt / full context / raw request / raw response
```

必须脱敏后才用于摘要：tenantId / accountId / strategyCode / paperRunId / backtestId 按需 hash 或内部别名；订单/成交/持仓只保留统计指标；错误信息去内部路径/SQL/连接串；市场数据只必要窗口与粒度。

## 8. Provider / 中转站边界（冻结）

- Integration-0 不接任何真实 provider，不发起真实 LLM 调用。
- 真实 provider 接入前必须完成 provider trust policy、数据分级、脱敏、出站审计、baseURL allowlist、人工审批。
- trust level（沿用 DH `ProviderTrustLevel`）：`OFFICIAL_API` / `SELF_HOSTED_GATEWAY` / `CONTROLLED_RELAY` 分级允许；`UNTRUSTED_RELAY` / `UNKNOWN` 拒绝。
- OpenAI-compatible relay、new-api、one-api、openrouter、siliconflow、未知 relay/proxy 默认拒绝（P1-3 已落策略框架）。

## 9. 审计要求（冻结）

必须落审计：接收 / 拒绝 / 限流 / 重放命中 / 签名失败 / tenant 不一致 / payload 超限 / forbidden field 命中 / 幂等冲突 / provider 出站决策。

审计字段（按存在选择）：`traceId / requestId / tenantId / source / eventType / errorCode / 耗时 / 结果 / provider / baseUrlHash / trustLevel`。

审计禁止记录：`token / API key / secret / passphrase / cookie / 私钥 / 助记词 / 签名原材料 / raw request / raw response / full prompt / full context / 未脱敏堆栈 / raw baseURL`。

## 10. Gate 与开关（冻结）

- 跨系统集成默认 gate disabled；命中 disabled 返回/视为 423。
- DH connector 三层 gate（stage3.nq.enabled / backtest-request.enabled / fake-mode）默认全部 false；Integration-0 只走 Fake / Disabled。
- LIVE 永远独立开关、独立审查、独立 Gate；Integration-0..N 不得开启 LIVE。
- 真实通道开关只能在 Integration-1 及以后、P1-4 残留修复并通过安全审查后，由人工显式启用。

## 11. 与现有实现的关系（诚实声明）

- DH-NQ header alignment 已 **CLOSED**；DH production NQ feedback 入站已使用 canonical-only `X-NQ-DH-*` header 族，不再接受 legacy `X-DH-NQ-*` 作为成功路径。
- 本策略冻结的 canonical 跨系统 header 族仍为 `X-NQ-DH-*`；HMAC/timestamp/nonce/source allowlist/payload gate 语义保持。
- Header alignment CLOSED 与 timestamp alignment CLOSED 均不授权 Integration-1 runtime；真实通道仍必须另起独立 PLAN 与安全审查。

## 12. Integration-1 安全前置（冻结）

进入 Integration-1 前必须修复（DH P1-4 残留，本轮不修复）：

- rate limit（租户/能力级限流）。
- memory cap（InMemory 仓储上限或外部存储）。
- replay nonce 持久化（持久化或集中缓存）。
