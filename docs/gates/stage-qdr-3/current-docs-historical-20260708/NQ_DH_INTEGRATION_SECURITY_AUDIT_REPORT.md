# NQ DH INTEGRATION SECURITY AUDIT REPORT

> Audit date: 2026-05-27
> Scope: NexusQuant and Decision Hub future integration security boundary.
> Mode: audit and design only; no integration implementation.

## 1. 结论

- 是否允许 DH 接入 NQ：当前不允许真实接入；仅允许进入 Integration-0 的只读边界与契约冻结设计。
- 是否允许接 NQ RealClient：不允许。DH 当前仍不得接入 `RealNqBacktestClient`、NQ real endpoint 或真实 HTTP 联调。
- 是否允许真实 provider：不允许。真实 LLM provider、OpenAI-compatible relay、第三方中转站均不得接入。
- 是否允许 Paper-only 集成设计：允许做设计与契约审查；不允许启动真实 Paper 联调或自动触发 Paper run。
- 是否允许 LIVE 交易链路：不允许。LIVE 必须单独安全审查、单独 Gate，当前阶段禁止开放。
- 必须先完成前置项：NQ GateJ-FREEZE 必须先单独完成；Integration-0 只能做只读边界、schema、认证、防重放、审计和风险清单冻结。

最终判断：本轮不允许 DH 直接接入 NQ，不允许 RealClient，不允许真实 provider，不允许 LIVE；建议下一步进入 Integration-0，但必须以 NQ GateJ-FREEZE 完成为前置门槛。

## 2. 审查范围

### 2.1 当前状态

- NQ：AUDIT-FIX completed；P1 已关闭；E2E 端口问题已关闭；允许进入 GateJ-FREEZE 判断，但 GateJ-FREEZE 必须单独开工。NQ 当前仍是 GateJ Paper Trading 稳定运行阶段，AI not started。
- DH：DH-AUDIT-FIX completed；P1-1/P1-2/P1-3 已关闭；`mvn test` 当前通过；Stage3-B3 DH Backtest Request Adapter IMPL completed，但仍无真实 HTTP、无 RealClient。

### 2.2 本轮触碰内容

- 读取 NQ 与 DH 当前事实源、API 文档、安全审计文档和边界相关代码。
- 执行只读 git 状态命令与可行验证命令。
- 新增本报告：`docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md`。

### 2.3 本轮未触碰内容

- 未修改 NQ 业务代码。
- 未修改 DH 业务代码。
- 未新增 NQ API / DH API。
- 未新增 migration。
- 未接 NQ RealClient、真实 provider、第三方中转站。
- 未执行真实交易所下单。
- 未读取或输出 `.env`、API Key、Token、模型 Key、交易所凭证明文。

## 3. DH -> NQ 权限边界

### 3.1 必须禁止

DH 不允许直接调用：

- NQ 下单接口：禁止，包括 `/api/trading/orders`、`PlaceOrderRequest`、`PlaceOrderCommand` 等交易执行入口。
- NQ 撤单接口：禁止，包括 `/api/trading/orders/cancel`、`CancelOrderRequest`、`CancelOrderCommand`。
- NQ 策略状态修改接口：禁止，包括策略启停、发布、调度、运行触发等写侧入口。
- NQ Paper Run 启动接口：禁止直接调用 `/api/paper-trading/runs/{paperRunId}/start`、调度 run-once、monitor run-once。
- NQ 交易所凭证读取接口：禁止读取 `/api/exchange-accounts/{accountId}/credentials/**` 或任何 credential repository。
- NQ 数据库写入：禁止直接连接、写入或迁移 NQ 数据库。
- NQ 数据库读取：禁止 DH 直接读 NQ DB；只能走未来受控只读 API 或 feedback 事件。
- NQ 风控、订单状态机、账本、恢复、对账、交易所 adapter：禁止直接调用或绕过。

### 3.2 未来可开放但必须单独审查

仅在 Integration-0 契约冻结、NQ GateJ-FREEZE 完成后，才可以设计以下低权限能力：

- 只读 Research / Backtest / Evaluation 查询：返回脱敏、最小字段、分页、限流、审计。
- 受控 backtest request endpoint：只接收候选回测请求，不执行交易，不启动 Paper/LIVE，不修改策略状态。
- feedback 接收与查询：以 NQ 事实为源，DH 只消费，不反向改写 NQ。
- job 状态查询：只返回异步任务状态、拒绝原因、trace 字段，不暴露订单全量、凭证或账户敏感字段。

### 3.3 DH 输出定位

DH 输出只能是 `Signal` / `Recommendation` / `StrategyCandidate` / `BacktestRequest` / 报告。它们是候选输入，不是 NQ 执行命令。任何最终交易动作必须由 NQ 独立风控、独立状态机和本地审计决定。

## 4. NQ -> DH Feedback 边界

### 4.1 签名与认证

NQ feedback 必须使用服务间认证，最低要求：

- `Authorization: Bearer <service-token>` 或 mTLS，生产建议 mTLS + HMAC 双层。
- `X-DH-NQ-Source` 必须等于 envelope `sourceSystem`，且命中 allowlist。
- `X-DH-NQ-Timestamp` 必须在允许时钟偏移内，建议默认 300 秒。
- `X-DH-NQ-Nonce` 每次唯一。
- `X-DH-NQ-Signature` 使用 HMAC-SHA256，签名材料至少包含 `sourceSystem`、timestamp、nonce、eventId、requestId、traceId、payloadJson。

DH 当前已具备 HMAC、timestamp、nonce/requestId 防重放、source allowlist、payload size gate 的最小框架；未来 NQ 侧必须按同一 canonical 规则生成签名。

### 4.2 防重放

- DH 必须按 `sourceSystem + nonce + requestId` 原子登记 replay key。
- 重放请求返回 409 或等价拒绝结果；已接收的 `eventId` 幂等重放仍可在业务层返回 DUPLICATE，但认证层 nonce 重放必须先拒绝。
- nonce TTL 建议至少为 `2 * maxClockSkew`，生产应持久化或使用集中缓存，不能只依赖单实例内存。

### 4.3 tenant、traceId、requestId 绑定

所有跨系统事件必须包含：

- `tenantId`：必须与认证主体绑定，禁止仅信任请求体。
- `traceId`：端到端追踪主键，必须可在 DH ResearchRun 或未来 Integration-0 trace registry 中命中。
- `requestId`：请求幂等键；DH 发起的请求必须原样回流，NQ 自发事件由 NQ 生成。
- `eventId`：NQ feedback 全局唯一幂等键。
- `schemaVersion`：语义化版本；MAJOR 破坏式升级必须双方同步发版。

### 4.4 payload 大小

- 当前 DH 默认 `decisionhub.security.nq-feedback.max-payload-bytes=65536`，即 64 KiB。
- Integration-0 建议冻结默认最大 64 KiB；超过必须拒绝，不能截断后接受。
- 大型回测结果只传摘要、指标和引用 ID；原始大对象留在 NQ，由受控只读接口按权限查询。

### 4.5 允许发送字段

允许发送给 DH：

- 事件元数据：eventId、eventType、sourceSystem、sourceJobId、occurredAt、schemaVersion。
- 追踪字段：tenantId、traceId、requestId、correlationId。
- Paper/Backtest 摘要：paperRunId、backtestId、status、verdict、指标摘要、拒绝原因码、告警类型、稳定性检查摘要。
- 脱敏 payload：不含凭证、不含账户敏感字段、不含交易所原始密钥。

### 4.6 禁止发送字段

禁止发送给 DH：

- 交易所 API Key、Secret、Passphrase、Token、Cookie、私钥、连接串。
- NQ 数据库 DSN、内部服务凭证、Vault 路径明文。
- 账户余额全量、账户身份敏感字段、原始 credential material。
- LIVE 真实订单全量细节、成交全量、外部订单原始响应全量。
- 未脱敏错误堆栈、内部路径、能反推出密钥或账户的信息。

是否允许发送真实订单全量细节：默认禁止。仅在后续 LIVE 单独审查中，按字段分级、最小化、脱敏、审计和人工批准后，才可开放只读摘要；当前阶段不得发送。

## 5. NQ 对 DH 输入的处理规则

NQ 必须把所有 DH 输入视为不可信输入。

进入 NQ 前必须校验：

- 认证主体、tenantId、traceId、requestId、nonce、签名。
- schemaVersion 与 JSON Schema。
- 字段白名单、枚举、日期窗口、symbol 白名单、资金/数量上限。
- 幂等键：同 requestId 不允许换 payload 重放。
- 来源权限：DH 只能提交 candidate signal / recommendation / backtest request，不具备交易执行权限。

NQ 必须独立执行：

- 独立风控：任何 DH signal 不得绕过 `RiskGate` / `PreTradeRiskService`。
- 独立订单状态机：任何订单状态变化只能由 NQ core/application service 完成。
- 本地审计日志：拒绝、接收、限流、重放、风控结果都必须落 NQ 本地审计。
- 本地事实存储：NQ 仍是唯一交易事实源。

DH 信号错误时，NQ 应拒绝：

- 400：schema / 字段 / symbol / 日期 / 参数错误。
- 401 / 403：认证或权限错误。
- 409：幂等冲突或同 requestId payload 不一致。
- 423：AI / integration gate disabled。
- 429：租户或能力限流。

DH 超时或不可用时，NQ 降级：

- NQ 主链路继续运行。
- 不阻塞订单、风控、账本、回测、Paper。
- feedback outbox 保留 PENDING / FAILED / DEAD_LETTER 状态，可人工重放。
- DH 不可用不能触发 NQ 内部策略降级为自动执行。

## 6. Provider / 中转站边界

### 6.1 NQ 数据是否允许发给模型

当前不允许。未来接入真实 provider 前，必须先完成 provider trust policy、数据分级、脱敏、出站审计、baseURL allowlist 和人工审批。

### 6.2 必须脱敏的数据

- tenantId、accountId、strategyCode、paperRunId、backtestId：按需要 hash 或替换为内部别名。
- 订单 / 成交 / 持仓摘要：只保留统计指标和解释所需最小字段。
- 错误信息：去除内部路径、SQL、连接串、服务拓扑。
- 市场数据：只发送必要窗口、必要粒度；禁止批量外发 NQ 原始数据资产。

### 6.3 禁止发给任何第三方 provider 的数据

- 交易所 API Key / Secret / Passphrase / token / cookie / 私钥。
- NQ / DH 服务 token、HMAC secret、JWT、数据库连接串。
- LIVE 账户余额、可识别真实账户身份的字段。
- LIVE 真实订单全量、交易所原始响应、审计原始日志。
- 未脱敏的 NQ 数据库行、原始 credential payload。

### 6.4 trust level 规则

DH 当前 `ProviderTrustLevel` 包含：

- `OFFICIAL_API`：允许使用低敏/中敏上下文；敏感上下文仍需最小化。
- `SELF_HOSTED_GATEWAY`：允许在自控网关内使用敏感上下文，但必须审计、脱敏、限流。
- `CONTROLLED_RELAY`：只允许脱敏低敏上下文；默认不允许敏感上下文。
- `UNTRUSTED_RELAY`：拒绝。
- `UNKNOWN`：拒绝。

OpenAI-compatible relay、new-api、one-api、openrouter、siliconflow、未知 relay/proxy 默认拒绝。

### 6.5 自建网关条件

自建网关必须满足：

- baseURL 固定 allowlist；证书、mTLS 或私网访问控制。
- 不落盘 prompt 原文中的敏感字段，或先脱敏再落审计摘要。
- 出站请求记录 provider、model、baseURL hash、tenantId、traceId、requestId、token/cost 摘要。
- 支持 kill switch、rate limit、tenant quota、数据保留周期。
- 安全评审通过后才能进入 Integration-3 以后阶段。

## 7. 推荐集成架构

推荐架构：候选建议面与执行事实面分离。

```text
DH Research Run
  -> Signal / Recommendation / StrategyCandidate
  -> DH JudgeDecision
  -> signed candidate/backtest request (future, disabled by default)

NQ Integration Gateway
  -> auth / signature / replay / tenant / schema / rate limit
  -> read-only Research / Backtest / Evaluation query
  -> controlled BacktestRequest queue (Paper/LIVE disabled)

NQ Core
  -> independent validation
  -> independent risk
  -> independent state machine
  -> local audit and event log

NQ Feedback Outbox
  -> signed event envelope
  -> retry / dead-letter / audit reconcile
  -> DH /api/ai/feedback/nq
```

架构原则：

- DH 输出只能作为候选输入，不是执行命令。
- NQ 提供只读 Research / Backtest / Evaluation 查询或受控 request endpoint。
- NQ 独立执行风控和状态机。
- 所有跨系统请求必须签名、限流、防重放、审计。
- 所有跨系统数据必须带 `schemaVersion`。
- 所有跨系统事件必须带 `tenantId`、`traceId`、`requestId`。
- 所有拒绝必须可追踪、可复盘、可对账。

## 8. 分阶段路线

### Integration-0：只读边界与契约冻结

边界：

- 只写文档、schema、OpenAPI 草案、风险清单、runbook。
- 不新增 API，不新增 migration，不接 RealClient。
- 冻结字段分级、签名材料、错误码矩阵、payload 64 KiB 上限、tenant/trace/request 规则。

验收：

- NQ GateJ-FREEZE 完成。
- 双方文档一致。
- 双方安全负责人确认禁止能力清单。

### Integration-1：NQ -> DH feedback 单向通道

边界：

- 仅 NQ 事实落地后发送 feedback。
- outbox 默认关闭，test profile 先 fake dispatcher。
- DH 只 ingest，不反向触发 NQ。

验收：

- HMAC / nonce / source allowlist / payload limit 全绿。
- outbox retry / dead-letter / audit 全绿。
- NQ 主链路不受 DH 不可用影响。

### Integration-2：DH -> NQ candidate signal 单向建议通道

边界：

- DH 只提交 candidate signal / recommendation。
- NQ 只落候选或创建待审查任务，不执行交易，不启动 Paper/LIVE。

验收：

- NQ 把 DH 输入作为 untrusted。
- schema / rate limit / tenant / requestId 幂等全绿。
- 所有拒绝落审计。

### Integration-3：Paper-only 受控联调

边界：

- 仅隔离 test cluster / staging。
- 仅 t-test-* tenant。
- Paper-only；LIVE 配置必须关闭。
- 不使用 prod 数据、prod URL、prod token。

验收：

- 端到端 T1-T7 全绿。
- NQ Paper 主链路可独立运行。
- DH 无 NQ 时仍能 Fake / Disabled 闭环。

### Integration-4：严格风控下的低权限自动建议

边界：

- 仍是建议，不是自动交易。
- 可自动生成待审批建议、待回测请求、风险说明。
- 不允许自动下单、自动撤单、自动发布 LIVE 策略。

验收：

- NQ 独立风控、状态机、审计仍是唯一执行门。
- 人工审批链路明确。
- provider 数据出站审查通过。

LIVE 真实交易必须另行审查；当前阶段不允许开放。

## 9. 风险清单

### P0

- DH 直接调用 NQ 下单/撤单/LIVE 接口，绕过 NQ 风控或状态机。
- DH 或 provider 接触交易所凭证明文、NQ DB 连接串、服务 token。
- NQ 将 DH signal 作为执行命令直接下单。
- 第三方 relay 接收未脱敏 NQ 账户、订单、凭证或 LIVE 上下文。

### P1

- feedback 没有签名、防重放、source allowlist 或 payload size gate。
- tenantId 只信任请求体，未绑定认证主体。
- requestId 幂等冲突时允许换 payload。
- NQ outbox 失败反压交易、风控、账本或回测主链路。
- GateJ-FREEZE 未完成就启动集成实现。

### P2

- 只读接口字段过宽，泄露真实订单、账户、策略源代码或内部拓扑。
- traceId / requestId / correlationId 混用，导致审计无法复盘。
- provider allowlist 未固化，未知 OpenAI-compatible baseURL 被误放行。
- payload 大对象直接外发，导致成本、泄露和可用性风险。

### P3

- 文档状态不一致：DH `WORK_ORDER.md` 仍含旧 Stage2 口径，应后续文档清理。
- NQ / DH 本地验证依赖后端、Docker、端口等环境状态，E2E 容易出现环境性失败。
- Vite chunk > 500 KiB、Mockito dynamic agent、SLF4J provider 警告仍为非阻塞维护项。

## 10. 验证结果

### 10.1 NQ git 状态

- `git status --short`：无输出。
- `git branch --show-current`：`dev`。
- `git log --oneline -10`：
  - `b8cbd1c2 PRE-FREEZE-CODE-AUDIT 已完成`
  - `05e5fd69 docs: complete pre-freeze audit for GateJ`
  - `0cb8406e docs: DOC-CLEAN-2 prune frozen GateH/GateI plans and sync entries`
  - `10ab3ed5 feat: add GateJ-3 paper run recovery, stability checks, and auto alerts`
  - `72bef54d feat: add GateJ-2 paper run reports and alerts`
  - `2878ebdc feat: add GateJ-1 paper run scheduling and heartbeats`
  - `11368007 docs: plan GateJ paper trading stable operation`
  - `40400973 docs: freeze GateI snapshot and sync all documentation`
  - `c11e7a87 feat: complete GateI-4 paper trading monitor and close GateI`
  - `51dd6c77 test: close GateI paper trading monitor validation`
- `git diff --stat`：无输出。

### 10.2 DH git 状态

- `git status --short`：执行报告写入前无输出。
- 报告写入后 `git status --short`：`?? docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md`。
- `git branch --show-current`：`dev`。
- `git log --oneline -10`：
  - `3ed9681 fix(security): 关闭 DH 审计 P1 风险`
  - `67cc869 Stage3-B3 DH Backtest Request Adapter IMPL completed`
  - `fcf3236 docs(stage3): land B2/B3/B4 SPECs, freeze PLAN snapshot, fix Next status`
  - `384c7e0 feat(stage3-b1): align contracts and add 4 contract tests (29 cases)`
  - `3853122 docs(stage3-plan): land 6 Stage3 planning docs for NQ <-> DH integration`
  - `f7b0d05 docs(stage2-poc-verify+freeze): apply VERIFY fixes and freeze docs/current to docs/gates/dh-stage2-poc`
  - `a461c59 feat(stage2-poc-b5): land V3 migration + 5 Stage2 JDBC repos + ArchUnit 10 rules`
  - `0939a51 feat(stage2-poc-b4): add Reflection/Checkpoint service + dynamic planner with 4 strategy handlers`
  - `bd51463 feat(stage2-poc-b3): add Forecast/Research adapter port interfaces and Fake implementations`
  - `6b1615c feat(stage2-poc): land WO + Batch 1 contracts/domain + Batch 2 NQ feedback ingestion`
- `git diff --stat`：执行报告写入前无输出；报告为 untracked 新文件，未纳入 tracked diff stat。

### 10.3 构建与测试

| 仓库 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| NQ | `mvn -f backend/pom.xml test` | 通过 | 首次因 NQ 仓库不在可写根导致 target 写入被拒；提权后 Reactor `BUILD SUCCESS`，23 modules SUCCESS。 |
| NQ | `cd frontend && npm run build` | 通过 | Vite build 成功；保留 chunk > 500 KiB 警告。 |
| NQ | `cd frontend && npm run test:e2e` | 未通过 | Vite 监听 `127.0.0.1:5179` 成功，但本地后端 `127.0.0.1:18888` 未启动，`/api/auth/login` 代理 `ECONNREFUSED`；24 failed / 1 skipped。未修复环境。 |
| DH | `mvn test` | 通过 | Reactor `BUILD SUCCESS`；`PostgresContainerSmokeTest` 因 Docker 环境不可用自动 skipped 1。 |
| DH | `docker compose -f ops/docker-compose.yml config` | 通过 | Compose 配置可渲染；未启动容器。 |

## 11. 最终判断

- 是否进入 Integration-0：建议可以准备进入，但只限只读边界、契约冻结、安全设计和风险清单；不得实现集成功能。
- 是否仍需先完成 NQ GateJ-FREEZE：是。NQ GateJ-FREEZE 必须先单独完成，不能夹带 AI 或集成实现。
- 是否允许 DH 接入 NQ：当前不允许真实接入；Integration-0 后也只能按冻结契约逐步开放低权限能力。
- 是否允许真实 provider：不允许。ProviderTrustPolicy 虽已具备框架，但真实 provider 与 relay 出站必须另行审查。
- 是否允许 LIVE：不允许。LIVE 真实交易必须另行安全审查和 Gate，不得在当前阶段开放。

下一步建议：

1. 先完成 NQ GateJ-FREEZE。
2. 单独开 Integration-0，只冻结 schema、认证、防重放、payload 分级、错误码和审计规则。
3. Integration-0 完成并评审通过后，再决定是否进入 Integration-1 的 NQ -> DH feedback 单向通道。
