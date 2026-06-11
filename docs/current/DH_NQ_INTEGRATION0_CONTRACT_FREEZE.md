# DH-NQ Integration-0 Contract Freeze

> 任务：NQ-DH-INTEGRATION-0-CONTRACT-FREEZE
> 类型：DOCUMENTATION + CONTRACT DESIGN
> 日期：2026-06-11
> 仓库视角：Decision Hub（DH，AI Agent 决策能力层 / 候选建议方）
> 对端：NexusQuant（NQ，交易事实源 / 主权执行方）

本文件是 DH 侧 Integration-0 契约冻结主文档，与 NQ 仓库 `docs/current/NQ_DH_INTEGRATION0_CONTRACT_FREEZE.md` 口径一致，框架视角不同。配套文档：

- `DH_NQ_INTEGRATION0_SECURITY_POLICY.md`：安全策略。
- `DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`：mock / contract test 设计。

---

## 1. 总体结论

- 结论：**有条件通过冻结**。本轮只冻结 Integration-0 契约与边界，不实现集成代码。
- Integration-0 是 **contract / mock / documentation work line, not runtime integration**。
- DH 当前 **无真实 NQ 调用、无真实 Provider、无交易能力**；NQ-DH **not integrated**。
- 真实联调、真实 HTTP、DH RealClient、NQ RealClient、真实 Provider、真实交易所调用、LIVE 全部禁止。
- DH P1-4 残留（rate limit / memory cap / replay nonce 持久化缺失）**不阻塞 Integration-0**，但**阻塞 Integration-1**。本轮不修复 P1-4。

## 2. Integration-0 定义

Integration-0 = DH → NQ 真实接入前的**只读边界、契约冻结、权限模型、审计模型与风险清单**的文档与契约工作线。

允许：只读边界设计、契约冻结、mock/stub/contract test 设计、安全策略文档、signature/tenant/trace/replay/payload 契约设计、禁止能力清单冻结、可开放能力清单冻结。

禁止：真实联调、真实 HTTP、真实交易所调用、DH/NQ RealClient、真实 Provider、下单/撤单/Paper 启停/改策略状态/改风控状态、读取凭证、读写 NQ DB、开启 LIVE、自然语言或 Agent output 直接驱动交易、把本轮写成 implemented、把 Integration-0 写成真实集成。

## 3. 明确禁止事项

1. 不实现任何 Java / frontend / Python 代码；不新增 API / Controller / Service / Repository / DTO / migration。
2. 不新增 DH RealClient（RealNqBacktestClient 等）、NQ RealClient、真实 Provider、真实 NQ 调用、真实交易所调用。
3. 不读取、打印、复制、输出任何真实密钥、token、cookie、私钥、助记词、API secret、passphrase。
4. 不把 DH not integrated 写成 integrated；不把 AI not started 写成 started；不把 LIVE disabled 写成 enabled。
5. 契约冻结只描述未来接入时的稳定约束，不代表已经接入。

## 4. DH → NQ 禁止能力（冻结）

DH **永久禁止默认拥有**以下能力。口径固定：

| 能力 | 允许 Integration-0 | 允许 Integration-1 | 允许 LIVE | 需代码硬闸 | 需审计记录 |
| --- | --- | --- | --- | --- | --- |
| 下单 | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 撤单 | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 修改订单状态 | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 修改策略状态 | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 启动 Paper Run | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 停止 Paper Run | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 修改风控状态 | 否 | 默认否，需单独审计批准 | 否 | 是 | 是 |
| 读取交易所凭证 | 否 | 否 | 否 | 是 | 是 |
| 读取 NQ credential | 否 | 否 | 否 | 是 | 是 |
| 直接读写 NQ DB | 否 | 否 | 否 | 是 | 是 |
| 绕过 NQ API | 否 | 否 | 否 | 是 | 是 |
| 绕过 NQ 风控 | 否 | 否 | 否 | 是 | 是 |
| 绕过 NQ 状态机 | 否 | 否 | 否 | 是 | 是 |
| 绕过 NQ 审计 | 否 | 否 | 否 | 是 | 是 |
| 直接触发 LIVE | 否 | 否 | 否 | 是 | 是 |
| 自然语言直接驱动交易 | 否 | 否 | 否 | 是 | 是 |
| Agent output 直接驱动交易 | 否 | 否 | 否 | 是 | 是 |
| feedback 直接影响交易执行 | 否 | 否 | 否 | 是 | 是 |

冻结原则：

- DH 输出永远只是 `Signal` / `Recommendation` / `StrategyCandidate` / `ResearchReport` / `RiskReview` / `BacktestRequest`，是**候选输入**，不是 NQ 执行命令。
- DH 的 JudgeDecision 是 DH 内部最终出口，不是 NQ 的执行授权。
- “需代码硬闸=是”表示未来接入前必须先在 NQ 侧补齐拒绝/隔离硬闸；DH 侧也必须默认 Disabled client。

## 5. DH → NQ 可开放能力（冻结）

统一约束：真实 HTTP=否；需认证=是；需签名=是；需 tenant binding=是；需 requestId/traceId=是；需 timestamp=是；需 nonce=是；需 replay protection=是；需 payload size limit=是（默认 64 KiB）；需 audit log=是；允许进入交易执行路径=否。

| 能力 | 只读 | 允许 mock | 方向 | 说明 |
| --- | --- | --- | --- | --- |
| 1. 读取公开/脱敏系统状态 | 是 | 是 | NQ→DH | DH 消费运行态摘要 |
| 2. 读取策略元数据 | 是 | 是 | NQ→DH | strategyCode/版本/状态枚举 |
| 3. 读取回测摘要 | 是 | 是 | NQ→DH | 指标摘要/verdict |
| 4. 读取 Paper 结果摘要 | 是 | 是 | NQ→DH | paperRunId/状态/指标摘要 |
| 5. 提交候选信号 | 否（写候选） | 是 | DH→NQ | 只落候选，不执行 |
| 6. 提交研究报告 | 否（写候选） | 是 | DH→NQ | 报告引用 |
| 7. 提交风险解释 | 否（写候选） | 是 | DH→NQ | 风险说明 |
| 8. 提交非执行型建议 | 否（写候选） | 是 | DH→NQ | recommendation |
| 9. 接收 NQ 脱敏反馈 | 是 | 是 | NQ→DH | DH 只消费不反写 NQ |
| 10. mock test | 是 | 是 | 双向 | 契约 mock |
| 11. stub test | 是 | 是 | 双向 | stub 隔离 |
| 12. contract test | 是 | 是 | 双向 | schema/header/replay 校验 |

说明：DH 当前 connector 已有 Fake / Disabled client（无真实 HTTP）；Integration-0 阶段所有可开放能力只允许走 Fake / Disabled / mock，**禁止 RealClient、禁止真实 HTTP**。

## 6. Header / Auth / Replay Contract（冻结）

详见 `DH_NQ_INTEGRATION0_SECURITY_POLICY.md`。Required headers：

```text
X-NQ-DH-Source
X-NQ-DH-Tenant-Id
X-NQ-DH-Request-Id
X-NQ-DH-Trace-Id
X-NQ-DH-Timestamp
X-NQ-DH-Nonce
X-NQ-DH-Signature
Content-Type: application/json
```

规则：Timestamp ±300 秒窗口；Nonce 防重放（`Source+Nonce+RequestId` TTL 内唯一）；Signature HMAC-SHA256（候选）；Payload ≤ 64 KiB；Source 必在 allowlist；Tenant 绑定请求/审计/数据作用域；RequestId 幂等+审计；TraceId 跨系统排查；签名原材料/raw request/raw response/prompt/full context 不得落日志、不得落库。

与现有实现的关系（诚实声明，不在本轮修复）：DH 已实现的 NQ feedback authenticator 当前使用 `X-DH-NQ-*` 命名族（见 `DH_AUDIT_FIX_REPORT.md`，P1-1/P1-2/P1-3 已关闭）。Integration-0 冻结的 canonical 跨系统 header 族为 `X-NQ-DH-*`。两者对齐是 **Integration-1 前置项**，不在本轮修复。

## 7. Data Contracts（冻结草案，contract-only / mock-only）

不生成 Java DTO。统一字段：`contractName / version / direction / purpose / allowedFields / forbiddenFields / validationRules / auditRequirements / payloadLimit / idempotencyRules / Integration-0 status`。通用：`version=0.1.0-int0-frozen`，`payloadLimit=64 KiB`，`Integration-0 status=contract-only / mock-only`，`forbiddenFields` 见第 8 节（每个契约继承）。

- **DHSignalCandidate**（DH_TO_NQ）：候选信号；allowedFields 含 strategyCode/symbol/side/signalType/confidence(0..1)/candidateOnly(const true)；symbol 白名单、枚举、confidence∈[0,1]、candidateOnly=true；同 requestId 幂等。
- **DHResearchReport**（DH_TO_NQ）：研究报告引用与摘要；只允许引用 ID 与摘要，不含原始大对象。
- **DHRiskReview**（DH_TO_NQ）：风险解释；riskLevel 枚举；不含执行指令字段。
- **DHDecisionSummary**（DH_TO_NQ）：JudgeDecision 后建议摘要；recommendation 枚举；linkedCandidateIds 可追溯；candidateOnly=true。
- **NQFeedbackEvent**（NQ_TO_DH）：NQ 脱敏事实事件，DH 只消费；eventId 全局幂等；eventType 枚举；sourceSystem=NQ。
- **NQPaperResultSummary**（NQ_TO_DH）：Paper 结果摘要；status 枚举；只摘要。
- **NQStrategyMetadata**（NQ_TO_DH）：策略元数据；不含源代码。
- **NQBacktestSummary**（NQ_TO_DH）：回测摘要；verdict 枚举；winRate∈[0,1]。
- **NQErrorResponse**（NQ_TO_DH）：标准错误；message 脱敏。
- **NQDhContractError**（NQ_TO_DH）：契约/校验错误；errorCategory ∈ SCHEMA/SIGNATURE/REPLAY/TENANT/PAYLOAD/RATE_LIMIT/FORBIDDEN_FIELD；拒绝必须落审计。

## 8. Forbidden Fields（统一禁止字段，所有契约继承）

```text
API key / API secret / token / cookie / passphrase / private key / mnemonic /
wallet private key / exchange credential / account credential / raw request /
raw response / full prompt / full context / signature raw material /
authorization header / database connection string / production URL with secret /
password / 2FA secret / recovery code
```

出现任一 → 契约校验直接拒绝（FORBIDDEN_FIELD）；禁止字段不得落日志、不得落库、不得回显。

## 9. NQ 对 DH 输入的不可信处理原则（冻结）

DH 必须假定 NQ 把所有 DH 输入当作 **untrusted input**：

- NQ 校验认证主体/tenantId/traceId/requestId/nonce/签名/schema/字段白名单/枚举/幂等/来源权限。
- NQ 独立执行风控、独立订单状态机、本地审计、本地事实源。
- DH 不得期望 NQ 信任 DH 输出；DH 只能提交候选，最终执行权在 NQ。

NQ 拒绝矩阵：`400 schema/字段 / 401 认证 / 403 权限或 tenant / 409 幂等或 replay / 413 payload 超限 / 423 gate disabled / 429 限流`。

DH 侧对应行为：DH 必须能正确处理上述拒绝码（重试/退避/死信/不阻塞 DH 主链路），且 DH 不可用时 NQ 主链路不受影响。

## 10. Mock / Contract Test Plan（冻结摘要）

完整设计见 `DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`。本轮只写计划，不写代码。必须设计 15 项（见 test plan）。每项含 `testName/targetSystem/purpose/input/expectedResult/forbiddenSideEffect/whetherBlocksIntegration0`。

## 11. Integration-0 验收标准（冻结）

1-11 项同 NQ 主文档：三份文档落盘且口径一致；禁止能力、可开放能力、header/auth/replay、数据契约、禁止字段、不可信输入原则、mock/contract test 全部冻结；明确 P1-4 为 Integration-1 前置；明确非实现任务；`git diff --check` 通过，docs-only。

## 12. Integration-1 Blockers（冻结）

进入 Integration-1 前必须修复 DH P1-4 残留（本轮不修复）：

- **rate limit 缺失**：跨系统入口缺租户/能力级限流。
- **memory cap 缺失**：DH InMemory 仓储（dh-memory 5 个 Store、Stage2/Stage3 InMemory 仓储）无上限，真实流量下内存膨胀风险。
- **replay nonce 持久化缺失**：nonce 仅单实例内存，多实例重放防护失效，必须持久化或集中缓存（TTL ≥ 2 × maxClockSkew）。

其它前置：NQ 侧 DH 入站端点 / DH client / feedback outbox 未实现；header `X-DH-NQ-*` 与 `X-NQ-DH-*` 对齐。

## 13. Out-of-Scope（本轮不做）

任何 Java/frontend/Python 代码；任何 API/Controller/Service/Repository/DTO/migration；DH/NQ RealClient、真实 Provider、真实 HTTP、真实交易所调用；真实联调、下单、撤单、Paper 启停、改策略状态、LIVE；读取凭证、读写 NQ DB；修复 P1-4 残留。

## 14. Next Action

- 推进 Integration-0 mock / contract test 设计与安全文档固化（仍是 contract/mock/docs 工作线）。
- 不得直接做真实联调；真实通道必须等 Integration-1 单独开工，先修复 P1-4 残留并通过安全审查。
