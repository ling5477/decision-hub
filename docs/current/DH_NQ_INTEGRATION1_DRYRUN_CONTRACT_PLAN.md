# DH-NQ Integration-1 Dry-run Contract Plan

> 任务：`NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN`
> 类型：`PLAN_ONLY + CONTRACT_DRYRUN_PLAN + CROSS_REPO_CONTRACT_MAPPING + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE`
> 日期：2026-07-02
> 仓库视角：Decision Hub（DH）
> 状态：`COMPLETED / PLAN ONLY / NOT IMPLEMENTED`

## 1. 结论

本文件只规划 NQ-DH Integration-1 dry-run contract。`COMPLETED` 表示规划文档完成；`PLAN ONLY` 表示只冻结合同边界和后续验证计划；`NOT IMPLEMENTED` 表示本轮没有新增 API、Controller、client、provider、Repository、Service、migration、测试代码、fixture 文件或真实 HTTP。

```text
NQ current main line: GateO.
NQ Integration-1 rebase input: GateN no-real public marketdata / exchange sandbox baseline.
DH baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED.
Legacy DH-GATEK-DECISION-PIPELINE-MVP: SUPERSEDED / NAMING_REPLACED.
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED.
Integration-1 implementation: NOT STARTED.
Integration-1 runtime: NOT STARTED.
Runtime integration: NOT STARTED.
Real HTTP: NOT STARTED.
Real provider: NOT STARTED.
Agent / LangGraph runtime: NOT STARTED.
LIVE: DISABLED.
```

## 2. 事实源读取结果

本轮只读核对了下列事实源：

- DH `DecisionRequest` JSON Schema：当前字段为 `requestId / traceId / tenantId / source / decisionType / subject / contextRef / contextSnapshot / requestedAt / schemaVersion`，且 `additionalProperties=false`。
- DH `DecisionOutput` JSON Schema：当前字段为 `requestId / traceId / tenantId / decisionType / action / status / riskLevel / policyStatus / providerStatus / forbiddenActions / reasonCodes / evidenceRefs / createdAt / schemaVersion`，且 `additionalProperties=false`。
- DH domain enum：`DecisionType=READ_ONLY_RECOMMENDATION`；`DecisionAction=ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；`DecisionRiskLevel=LOW / MEDIUM / HIGH / BLOCKED / UNKNOWN`；`DecisionPolicyStatus=ALLOWED / DENIED / REVIEW_REQUIRED / INVALID / BLOCKED`；`ForbiddenAction` 固定五项。
- Integration-0 安全事实：canonical header 为 `X-NQ-DH-*`；timestamp 为 RFC3339 / ISO-8601 UTC `Z`；epoch 秒、epoch 毫秒、数字时区偏移均拒绝；HMAC 为 value-based signature material；payload 上限 64 KiB；nonce replay fail-closed；tenant / requestId / traceId 需要绑定校验。
- NQ 交易边界：order state machine、risk gate、ledger、Paper Run、adapter readiness、no-real/no-outbound guard 均为 NQ 主权边界；DH dry-run 输出不得进入这些 mutation path。

## 3. NQ -> DH dry-run request 规划

本节规划 future wire-level request，不修改当前 schema。进入 implementation 前，必须先在 `I1-P2-CONTRACT-FIXTURES-PLAN` 中决定是否扩展 schema 或用现有字段表达 dry-run。

### 3.1 计划请求体

```json
{
  "schemaVersion": "1.0.0",
  "requestId": "nq-dryrun-req-001",
  "traceId": "trace-i1-dryrun-001",
  "tenantId": "tenant-fixture",
  "source": "NQ_DRYRUN",
  "decisionType": "READ_ONLY_RECOMMENDATION",
  "dryRun": true,
  "requestedAt": "2026-07-02T00:00:00Z",
  "subject": {
    "symbol": "BTC-USDT",
    "market": "SPOT",
    "timeframe": "1m",
    "strategyRef": "strategy-fixture",
    "researchRef": "research-fixture"
  },
  "contextSnapshot": {
    "snapshotId": "snapshot-fixture-001",
    "capturedAt": "2026-07-02T00:00:00Z",
    "evidenceRefs": ["fixture:evidence-001"]
  }
}
```

### 3.2 当前 schema 映射

| 计划字段 | 当前 schema 状态 | P1 结论 |
| --- | --- | --- |
| `schemaVersion` | 已存在，const `1.0.0` | 可直接使用。 |
| `requestId` | 已存在 | 作为幂等键和审计键。 |
| `traceId` | 已存在 | 作为端到端 trace。 |
| `tenantId` | 已存在 | 必须与 header / auth tenant 绑定。 |
| `source` | 已存在 | P1 建议值 `NQ_DRYRUN`，需后续 allowlist review。 |
| `decisionType` | 已存在，且仅允许 `READ_ONLY_RECOMMENDATION` | 必须固定。 |
| `dryRun` | 当前 schema 不存在 | 计划字段；不得在本轮写入 schema。后续二选一：扩展 schema，或用 `source=NQ_DRYRUN` 表达 dry-run。 |
| `subject.symbol / market / timeframe` | 已存在 | 可直接使用。 |
| `strategyRef / researchRef` | 已存在于 `subject` 可选字段 | 只允许只读引用，不得暴露账户、订单或凭证。 |
| `contextSnapshot` | 已存在 | 只传脱敏 evidence refs 和摘要。 |
| `evidenceRefs` | 已存在于 `contextSnapshot` | 若需要顶层 `evidenceRefs`，必须另起 schema review。 |
| `runRef` | 当前 schema 不存在 | 如需表达，只能先映射为脱敏 `researchRef` 或进入 schema review。 |

### 3.3 request 禁止字段

任何 future request 若出现下列字段或等价语义，必须 `FORBIDDEN_FIELD` / fail-closed，且不得落 raw payload：

```text
apiKey
apiSecret
passphrase
token
cookie
accountId
subAccountId
orderId
clientOrderId
positionId
brokerCredential
venueCredential
quantity
price
leverage
side = BUY / SELL
placeOrder
cancelOrder
mutateRisk
mutateLedger
paperRunStart
liveRunStart
```

## 4. DH -> NQ dry-run response 规划

本节规划 DH 返回给 NQ 的只读 DecisionOutput。DH 输出不是交易指令；NQ 只允许记录、展示、审计或进入人工复核。

### 4.1 计划响应体

```json
{
  "decisionId": "decision-fixture-001",
  "requestId": "nq-dryrun-req-001",
  "traceId": "trace-i1-dryrun-001",
  "tenantId": "tenant-fixture",
  "decisionType": "READ_ONLY_RECOMMENDATION",
  "status": "OBSERVATION_ONLY",
  "policyStatus": "ALLOWED",
  "action": "OBSERVE",
  "riskLevel": "LOW",
  "confidence": 0.42,
  "reasonCodes": ["DRYRUN_OBSERVATION_ONLY"],
  "evidenceRefs": ["fixture:evidence-001"],
  "providerStatus": "MOCKED",
  "traceSummary": {
    "traceId": "trace-i1-dryrun-001",
    "stepCount": 3
  },
  "replayRef": "replay-fixture-001",
  "auditRef": "audit-fixture-001",
  "dryRun": true,
  "forbiddenActions": [
    "PLACE_ORDER",
    "CANCEL_ORDER",
    "MUTATE_NQ_STATE",
    "READ_NQ_DB",
    "WRITE_NQ_DB"
  ],
  "createdAt": "2026-07-02T00:00:01Z",
  "schemaVersion": "1.0.0"
}
```

### 4.2 当前 schema 映射

| 计划字段 | 当前 schema 状态 | P1 结论 |
| --- | --- | --- |
| `requestId / traceId / tenantId` | 已存在 | 必须与 request/header 绑定。 |
| `decisionType` | 已存在，且仅允许 `READ_ONLY_RECOMMENDATION` | 必须固定。 |
| `status / policyStatus` | 已存在 | policy denied / invalid / blocked 必须 fail-closed。 |
| `action` | 已存在，只允许五项 | `LONG_BIAS / SHORT_BIAS` 只能表示只读倾向，不是 `BUY / SELL`。 |
| `riskLevel` | 已存在 | `HIGH / BLOCKED / UNKNOWN` 不得输出 directional bias。 |
| `providerStatus` | 已存在 | `TIMEOUT / FAILED / BUDGET_EXCEEDED / DISABLED` 默认 `ABSTAIN`。 |
| `forbiddenActions` | 已存在且必须包含五项 | 缺任一项即 unsafe。 |
| `reasonCodes / evidenceRefs` | 已存在 | 只允许脱敏 reason 和 evidence reference。 |
| `decisionId / confidence / traceSummary / replayRef / auditRef / dryRun` | 当前 schema 不存在 | 计划字段；后续必须通过 envelope、schema extension 或 NQ 本地 metadata 方案审查。 |

### 4.3 response 执行边界

- `ABSTAIN` 是默认 fail-closed action。
- `OBSERVE / NO_TRADE` 是只读观察或不交易建议。
- `LONG_BIAS / SHORT_BIAS` 只能表示分析倾向，不是买卖方向，不是订单 side，不得映射为 order command。
- NQ 不得根据 DH action 直接下单、撤单、修改 risk / ledger / position / strategy / Paper Run / LIVE state。
- NQ 记录 DH decision summary 时只保存脱敏摘要、引用、trace/audit id 和 no-side-effect flag。

## 5. Wire-level header / security 规划

P1 沿用 Integration-0 已接受的 canonical header 和安全语义，不凭空覆盖旧合同。

### 5.1 canonical headers

```text
Content-Type: application/json
X-NQ-DH-Request-Id
X-NQ-DH-Trace-Id
X-NQ-DH-Tenant-Id
X-NQ-DH-Source
X-NQ-DH-Timestamp
X-NQ-DH-Nonce
X-NQ-DH-Signature
```

`X-NQ-DH-Schema-Version` 仅作为 P2 review 候选：若未来加入，必须与 body `schemaVersion` 一致，不一致 `CONTRACT_INVALID` / fail-closed。当前 authoritative schema version 仍在 body。

### 5.2 security rules

- Timestamp 必须为 RFC3339 / ISO-8601 UTC `Z`，例如 `2026-07-02T00:00:00Z`；epoch seconds、epoch milliseconds、数字时区偏移均拒绝。
- 默认 skew window 沿用 ±300 秒；超窗 `TIMESTAMP_SKEW`。
- Nonce replay 以 `source + nonce + requestId` 或等价组合校验；重放 `NONCE_REPLAY`，必须 fail-closed。
- HMAC signatureMaterial 至少包含 source、tenantId、requestId、traceId、timestamp、nonce、`sha256(body)`；保持 value-based，不记录 raw signature material 或 shared secret。
- Source allowlist 初始只规划 `NQ_DRYRUN`、`DH_CONTRACT_TEST`、`JOINT_MOCK_VALIDATION`；正式列表必须后续 review。
- Payload size gate 默认 64 KiB；超限 `PAYLOAD_TOO_LARGE`，不得截断后接受。
- Rate limit 按 tenant + source + route 或等价维度；超限 `RATE_LIMITED`。
- tenant / requestId / traceId 必须在 header、body、auth binding、audit record 中一致；不一致 `TENANT_MISMATCH` 或 `CONTRACT_INVALID`。
- 禁止 credential、token、signature raw material、raw request secret、raw provider payload、full prompt、full context 入日志或持久化。

## 6. Status / error / fail-closed taxonomy

错误码是 P1 规划项，后续 P2/P4 需要与现有 Integration-0 code names 统一。所有失败都 fail-closed，NQ 接到失败只记录和审计，不触发交易。

| Error | 触发条件 | fail-closed 行为 |
| --- | --- | --- |
| `AUTH_FAILED` | 缺少认证、认证主体无效 | 拒绝；不执行业务逻辑。 |
| `SIGNATURE_INVALID` | HMAC 缺失、格式错误或不匹配 | 拒绝；不 fallback 到无签名 dry-run。 |
| `TIMESTAMP_SKEW` | timestamp 格式非 UTC `Z` 或超窗 | 拒绝。 |
| `NONCE_REPLAY` | nonce / requestId 重放 | 拒绝并落 replay audit。 |
| `SOURCE_DENIED` | source 不在 allowlist | 拒绝。 |
| `PAYLOAD_TOO_LARGE` | payload 超 64 KiB 或后续上限 | 拒绝；不得截断接受。 |
| `RATE_LIMITED` | tenant/source/route 超限 | 拒绝或 429；不得进入 provider / decision path。 |
| `CONTRACT_INVALID` | schema、enum、required 字段或 header/body binding 不合法 | 拒绝。 |
| `FORBIDDEN_FIELD` | credential、order、account、quantity、price、side 等禁字段 | 拒绝并高优先级 audit。 |
| `TENANT_MISMATCH` | header/body/auth tenant 不一致 | 拒绝。 |
| `PROVIDER_DISABLED` | provider 被禁用或未授权 | `ABSTAIN` / fail-closed。 |
| `PROVIDER_TIMEOUT` | provider 超时 | `ABSTAIN` / fail-closed。 |
| `PROVIDER_BUDGET_EXCEEDED` | provider 预算或调用上限超出 | `ABSTAIN` / fail-closed。 |
| `RISK_BLOCKED` | 风险策略拒绝 directional bias | `ABSTAIN` 或 `BLOCKED`。 |
| `INTERNAL_FAIL_CLOSED` | 未知异常、audit 写入失败或不可分类错误 | 默认 `ABSTAIN` / 拒绝；不得提升为 LONG / SHORT 信号。 |

任何未知错误都不得提升为 `LONG_BIAS / SHORT_BIAS`，更不得被解释为 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER`。

## 7. Trace / audit / replay 规划

### 7.1 lifecycle

```text
requestId: NQ 构造 dry-run request 时生成；用于幂等、audit、重复请求识别。
traceId: NQ 与 DH 贯穿同一 dry-run 链路；用于跨系统排查。
decisionId: DH response 规划项；当前 schema 未实现，后续需决定由 DH 生成还是由 NQ 本地 envelope 生成。
auditRef: DH / NQ 各自审计引用；不是执行凭证。
replayRef: DH replay read model 引用；只读审计入口，不重跑 provider，不驱动交易。
```

### 7.2 audit boundaries

- DH audit event 只记录 event type、tenantId、requestId、traceId、decisionId / auditRef、result、error code、payload size class、timestamp/nonce/signature 校验结果。
- DH replay view 只读 DH-owned decision records；不得读取 NQ DB，不得重放 provider，不得重跑 orchestrator。
- NQ 只记录 DH decision summary、reasonCodes、evidenceRefs、auditRef/replayRef 和 no-side-effect assertion。
- NQ 不保存 DH provider raw secret，不保存 raw prompt，不保存 raw provider response。
- replayRef / auditRef 只是审计引用，不是授权、执行凭证或交易许可。

## 8. 测试矩阵规划

本轮不写测试代码。后续 P2/P3/P4/P5 至少覆盖：

| Case | 目标 |
| --- | --- |
| valid dry-run request | request/response happy path 可互认。 |
| invalid signature | HMAC mismatch fail-closed。 |
| timestamp skew | 超窗或非 UTC `Z` fail-closed。 |
| nonce replay | 重放 fail-closed。 |
| source denied | 非 allowlist source fail-closed。 |
| payload too large | 64 KiB gate fail-closed。 |
| rate limited | tenant/source/route 超限 fail-closed。 |
| tenant mismatch | header/body/auth tenant 不一致 fail-closed。 |
| forbidden field: credential | apiKey/apiSecret/passphrase/token/cookie 等拒绝。 |
| forbidden field: order/account/quantity/side | orderId/accountId/quantity/price/leverage/BUY/SELL 等拒绝。 |
| DH provider disabled | response `ABSTAIN` / fail-closed。 |
| DH provider timeout | response `ABSTAIN` / fail-closed。 |
| DH budget exceeded | response `ABSTAIN` / fail-closed。 |
| high risk blocked | `HIGH/BLOCKED` 不允许 directional bias。 |
| no evidence fail-closed | 默认 `ABSTAIN`。 |
| NQ no-order guarantee | 不触发 order state machine / place / cancel。 |
| NQ no-risk-mutation guarantee | 不修改 NQ risk facts。 |
| NQ no-paper-run-start guarantee | 不启动 Paper Run。 |
| NQ no-live guarantee | LIVE 保持 disabled，不进入 real trading。 |
| replayRef / auditRef shape | 只读引用可追踪，不含 secret/raw payload。 |
| golden case compatibility | deterministic golden baseline 可复现。 |
| idempotency / duplicate requestId | 同 requestId 同 payload 可幂等，换 payload fail-closed。 |
| no real outbound scan | mock/stub/harness 不触达真实 NQ/DH/交易所 host。 |

## 9. 后续批次

| Batch | 状态 | 目标 | 禁止 |
| --- | --- | --- | --- |
| `I1-P2-CONTRACT-FIXTURES-PLAN` | `COMPLETED / PLAN ONLY / NOT IMPLEMENTED` | 已规划双仓 fixture / schema / golden case 对齐，不写 runtime。 | 不写 runtime、不新增真实 endpoint、不真实 HTTP。 |
| `I1-P3-NQ-DRYRUN-STUB-TEST-PLAN` | `MERGED_INTO_NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN` | 已并入 NQ stub readiness。 | 不接 DH runtime、不触发 order/risk/ledger/Paper/LIVE。 |
| `I1-P4-DH-DRYRUN-ENTRY-PLAN` | `MERGED_INTO_NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN` | 已并入 DH dry-run entry readiness。 | 不新增 API path、Controller、migration。 |
| `I1-P5-JOINT-MOCK-VALIDATION-PLAN` | `MERGED_INTO_NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN` | 已并入 joint mock validation readiness。 | 不启动 NQ/DH runtime。 |
| `I1-P4-IMPLEMENTATION-GATE-REVIEW` | `NOT STARTED` | 由旧 P6 重新编号，判断 P0-P3 是否足以允许后续 implementation。 | 即使进入 implementation，也仍禁止 LIVE、real provider、自动下单。 |

## 10. Readiness decision

```text
ALLOW_I1_P1_CONTRACT_PLAN_CLOSE: YES
ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN: YES
ALLOW_I1_P3_DRYRUN_IMPLEMENTATION_READINESS_PLAN: YES / COMPLETED / PLAN ONLY
ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW: YES
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 11. 边界确认

本 P1 不修改生产代码；不修改测试代码；不修改 `contracts/**`；不修改 `golden_cases/**`；不新增 API；不新增 migration；不新增 Controller / Client / Repository / Service；不真实 HTTP；不启动 runtime；不读取 credential；不接 provider；不接 AI / LangGraph；不进入 Paper Run；不进入 LIVE；不让 DH 输出进入 order、risk mutation、paper/live trading 或 private trading 路径。

## 12. P2/P3 planning closeout

`NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN` 已完成 planning-only 收口，文档见 `DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md`。P2 只规划 future fixtures、schema gap、golden case alignment、error taxonomy alignment 和 P3-P6 后续批次；不创建 fixture JSON，不改 JSON Schema，不改 `contracts/**` 或 `golden_cases/**`，不写测试代码或生产代码，不启动 runtime。

`NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN` 已完成 planning-only readiness 收口，文档见 `DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md`。原 P3/P4/P5 已合并进该 P3；旧 P6 implementation gate review 重新编号为 P4。

当前下一步只允许：

```text
NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW / NOT STARTED
```
