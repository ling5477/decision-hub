# DH Stage4 Decision Pipeline MVP 计划

> 任务：DH-STAGE4-DECISION-PIPELINE-MVP-PLAN
> 状态：ACCEPTED / CLOSED
> 范围：docs-only / plan-only
> 日期：2026-07-01

## 0. 范围与边界

本文规划 Decision Hub DH Stage4 Decision Pipeline MVP。本文只定义只读决策管线的合同、审计、回放、mock provider 和验收边界；不实现生产代码、测试代码、API path、migration、runtime client、真实 provider、AI runtime、LangGraph runtime、NQ runtime integration 或 LIVE 行为。

当前允许规划：

```text
DecisionRequest / DecisionOutput 合同规划
DecisionOrchestrator skeleton 规划
DecisionSnapshot / DecisionTrace / Audit / Replay 规划
Mock Provider / Disabled Provider 规划
Mock NQ dry-run contract test 规划
Provider health / budget / latency 规划
Golden Cases / Eval baseline 规划
Acceptance / Freeze 规划
```

当前禁止事项：

```text
Java / Kotlin / Python / TypeScript 生产代码变更
测试代码变更
新增 Controller
新增 API path
新增 migration
新增 Repository / Service / Client 实现
真实 HTTP
真实 NQ 调用
真实 DH runtime integration
交易所调用
RealClient
真实 Provider
OpenAI / Claude / Gemini / local model runtime
LangGraph runtime
MCP write capability
DecisionOrchestrator implementation
replay API implementation
audit / snapshot / trace table implementation
credential / token / cookie / API secret / passphrase access
Integration-1 runtime
把 DH 写成 integrated
把 Runtime integration 写成 started
把 AI / Agent runtime 写成 started
LIVE
AI output as trading instruction
NQ repository changes
继续把旧 NQ GateK 任务线作为当前主线
```

## 1. 阶段位置

当前阶段：

```text
DH Stage4 Decision Pipeline MVP planning
```

当前目标能力：

```text
Decision Execution Engine MVP（只读建议 MVP）
```

当前状态：

```text
Integration-0 safety gate: CLOSED / ACCEPTED
P1-4 residual: CLOSED
replay nonce persistence: CLOSED
bounded memory cap: CLOSED
inbound rate limit / 429 RATE_LIMITED: CLOSED
header alignment: CLOSED
timestamp alignment: CLOSED
code reality audit blockers: fixed
DH security state: FULL
fail-closed state: FULL
Integration-0 contract state: MATCH
Decision pipeline state: PARTIAL
Audit state: PARTIAL
Replay state: PARTIAL
NQ boundary violation: NO
Agent premature introduction: NO
External side effect state: NONE
Integration-1: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LIVE: DISABLED
```

本阶段把 DH 从 safety contract baseline 推进到最小、可审计、可回放、dry-run 的只读决策管线。它仍不是完整 Agent 系统，也不是交易执行系统。

本阶段不是：

```text
Integration-1 runtime
real NQ integration
real provider integration
LangGraph Agent Runtime
multi-agent phase
LIVE trading
```

## 2. 被取代工作线处理

旧任务线不再作为当前路线执行：

```text
NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
```

不得原样执行旧任务线。只允许复用以下安全约束作为输入：

```text
dry-run only
no LIVE
no real provider
no RealClient
no NQ mutation
no NQ DB read/write
no trading side effect
canonical X-NQ-DH-* header family
RFC3339 UTC Z timestamp
tenant / requestId / traceId binding
nonce replay protection
payload size guard
audit trail requirement
```

后续 Integration-1 必须基于 NQ GateN 重新规划，例如：

```text
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN
NQ-DH-GATEN-INTEGRATION1-DRYRUN-PLAN
```

## 3. Stage4 批次计划

### K0: Factsource Sync / Docs Rebase

目标：

```text
确认 docs/current 是当前事实源，并清理 current docs 中过期的 GateK / Integration-0 口径。
```

允许文件：

```text
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/API.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```

规则：

- root `README.md` 只在单独授权且 allowed file list 包含它时同步。
- `docs/gates/**` 是历史快照，本计划不得修改。
- `docs/codex/**` 是历史或辅助区，不得覆盖 `docs/current` 当前事实源。

成功标准：

```text
Current docs 一致确认 DH Stage4 Decision Pipeline MVP planning 是当前线。
旧 NQ-DH-GATEK-INTEGRATION1-PLAN-PACK 保持 SUPERSEDED / REBASE_REQUIRED。
NQ GateN 记录为未来 Integration-1 rebase baseline。
```

### K1: Decision Contract Freeze

目标：

```text
冻结第一版只读 recommendation 决策合同。
```

规划合同：

```text
DecisionRequest
DecisionContextSnapshot
DecisionPolicyResult
ProviderDecisionSignal
RiskReview
DecisionOutput
DecisionAuditEvent
DecisionReplayRecord
```

Schema 规则：

```text
每个 public contract 都需要 JSON Schema
required fields 必须显式
additionalProperties=false
enum values 必须显式
tenantId / requestId / traceId / decisionId 可追踪
forbiddenActions 始终存在
action enum 排除 BUY / SELL / PLACE_ORDER / CANCEL_ORDER
```

成功标准：

```text
任何 Java DTO / schema / OpenAPI 变更前，contract plan 可被 review。
DecisionOutput 被限制为 READ_ONLY_RECOMMENDATION。
```

### K2: DecisionOrchestrator Skeleton Plan

目标：

```text
规划线性、最小的 DecisionOrchestrator 主路径。
```

规划流程：

```text
Signed Request
-> API / Contract / Security
-> DecisionOrchestrator
-> normalize
-> policy check
-> mock provider
-> risk review
-> structured decision output
-> audit / snapshot / trace
-> return DecisionOutput
```

Fail-closed 规则：

```text
invalid signature -> reject
invalid contract -> reject
policy denied -> BLOCKED or ABSTAIN
provider unavailable -> ABSTAIN
no evidence -> ABSTAIN
high risk -> ABSTAIN or NO_TRADE
audit write failure -> fail closed
```

成功标准：

```text
后续实现可以拆成 ports、services、repositories 和 tests，且不会创建 runtime NQ integration 或真实 provider access。
```

### K3: Audit / Snapshot / Trace / Replay Plan

目标：

```text
规划 auditability 与 deterministic replay 所需的 persistence 与 read model。
```

规划表或记录：

```text
dh_decision_request
dh_decision_context_snapshot
dh_decision_trace_step
dh_provider_call_log
dh_decision_output
dh_decision_audit_event
replay read model
```

第一版 replay：

```text
read-model replay only
no real provider rerun
no LLM rerun
no NQ call
no external side effect
```

成功标准：

```text
每个 decision 都可以从 stored input、context snapshot、trace、provider signal、risk review、output 和 audit events 重建。
```

### K4: Mock Provider / Provider Health Plan

目标：

```text
规划安全 provider ports 与 health metadata，不接真实 provider。
```

规划类型：

```text
MockDecisionProvider
DisabledDecisionProvider
ProviderTrustPolicy
ProviderBudgetGuard
ProviderTimeoutGuard
ProviderHealth
ProviderLatencySample
ProviderCostSample
ProviderFailureClass
ProviderTrustScore
```

规则：

```text
default provider is DisabledDecisionProvider or deterministic MockDecisionProvider
real provider is forbidden
provider timeout -> ABSTAIN
provider budget denied -> ABSTAIN
provider trust denied -> ABSTAIN
provider spoofing detected -> fail closed
```

成功标准：

```text
provider behavior 可用 mock data 测试，且不能调用真实模型。
```

### K5: Mock NQ Dry-run Contract Test Plan

目标：

```text
规划 DH 侧 mock NQ dry-run contract tests，不修改 NQ，也不真实 HTTP。
```

规划覆盖：

```text
DH repository mock NQ request factory
canonical X-NQ-DH-* header family
RFC3339 UTC Z timestamp
HMAC signature
nonce replay protection
tenant / requestId / traceId / decisionId propagation
no-live-trade guarantee
forbidden side-effect checks
```

规则：

```text
no NQ repository change
no real HTTP
no NQ DB read/write
no Paper Run start
no order / trade / live endpoint
```

成功标准：

```text
任何 GateN-based Integration-1 planning 之前，contract tests 可证明 signed dry-run request shape 和无 side effects。
```

### K6: Golden Cases / Eval Plan

目标：

```text
规划 deterministic policy、provider、risk 与 contract behavior 的 golden cases。
```

必需 golden cases：

```text
valid_no_trade
policy_blocked
provider_timeout_abstain
high_risk_abstain
no_evidence_abstain
forbidden_action_rejected
invalid_contract_fail_closed
```

成功标准：

```text
每个 golden case 都有 input fixture、expected DecisionOutput、expected audit event 和 forbidden side-effect assertion。
```

### K7: Acceptance / Freeze Plan

目标：

```text
规划 MVP planning 与后续 WO / implementation batches review 后的 acceptance report 和 freeze decision。
```

规划验收产物：

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
```

Acceptance 必须判定：

```text
Stage4 plan 是否可 close
Stage4 WO 是否可 start
Decision Pipeline implementation 是否可 start
Integration-1 dry-run planning 是否可在 GateN rebase 后 start
runtime / agent / LangGraph / LIVE 能力是否仍保持 forbidden
```

成功标准：

```text
WO 接受前，任何 implementation batch 不得开始。
本 Stage4 plan 不得启动 Integration-1 runtime。
```

## 4. 目标合同约束

### 4.1 共享合同规则

每个第一版合同必须包含：

```text
schemaVersion
tenantId
requestId
traceId
createdAt
```

每个 decision-specific record 必须包含：

```text
decisionId
decisionType
policyStatus
auditStatus
forbiddenActions
```

所有 public JSON contract 必须使用：

```text
additionalProperties=false
explicit required fields
explicit enum values
stable error code vocabulary
no database Entity exposure
no raw provider prompt as final output
no credential material
```

### 4.2 DecisionRequest

用途：

```text
承载 signed request，用于只读 decision recommendation。
```

必需字段组：

```text
identity: tenantId, requestId, traceId
intent: decisionGoal, decisionScope, requestedDecisionType
context references: contextSnapshotId or inlineContextRef
policy: policyMode, forbiddenActions
evidence references: evidenceRefs
idempotency: idempotencyKey
timing: createdAt, expiresAt
```

禁止字段或意图：

```text
orderId as mutation target
account secret
exchange credential
LIVE flag
BUY / SELL / PLACE_ORDER / CANCEL_ORDER action request
```

### 4.3 DecisionContextSnapshot

用途：

```text
冻结一次 decision 使用的只读 input context。
```

必需字段组：

```text
snapshotId
tenantId
requestId
traceId
sourceRefs
marketContext
strategyContext
riskContext
evidenceSummary
createdAt
contentHash
```

规则：

```text
snapshot is immutable
snapshot stores original payload references or sanitized payload copies
snapshot does not read NQ DB
snapshot does not include credentials
```

### 4.4 DecisionPolicyResult

用途：

```text
在 provider 或 risk output 被信任前记录 policy evaluation。
```

必需字段组：

```text
policyResultId
decisionId
status: ALLOWED | BLOCKED | REVIEW_REQUIRED
reasonCode
deniedActions
matchedRules
createdAt
```

规则：

```text
policy denied -> BLOCKED or ABSTAIN
policy evaluation error -> fail closed
```

### 4.5 ProviderDecisionSignal

用途：

```text
表达 mock 或 disabled provider signal，但不能让它直接成为 final decision。
```

必需字段组：

```text
providerSignalId
decisionId
providerId
providerMode: MOCK | DISABLED
signalAction
confidence
evidenceRefs
latencyMs
costUnits
failureClass
createdAt
```

规则：

```text
real provider mode is forbidden in Stage4 MVP
provider failure -> ABSTAIN
provider signal never bypasses policy or risk review
```

### 4.6 RiskReview

用途：

```text
在生成 DecisionOutput 前评估风险。
```

必需字段组：

```text
riskReviewId
decisionId
riskLevel: LOW | MEDIUM | HIGH | CRITICAL
riskReasons
blockedActions
reviewerMode: RULE | MOCK
createdAt
```

规则：

```text
HIGH or CRITICAL risk -> no LONG_BIAS / SHORT_BIAS
risk review failure -> ABSTAIN
```

### 4.7 DecisionOutput

第一版必须满足：

```text
decisionType = READ_ONLY_RECOMMENDATION
action in ABSTAIN | OBSERVE | NO_TRADE | LONG_BIAS | SHORT_BIAS
default action = ABSTAIN
no evidence = ABSTAIN
provider failure = ABSTAIN
policy denied = BLOCKED or ABSTAIN
risk high = no LONG_BIAS / SHORT_BIAS
forbiddenActions includes PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
structured JSON only
no free-text final output
no real trading instruction
```

禁止进入 action enum 的值：

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
```

必需字段组：

```text
decisionId
decisionType
action
status: ALLOWED | BLOCKED | ABSTAINED
reasonCode
evidenceSummary
riskReview
policyResult
providerSignals
forbiddenActions
traceId
requestId
tenantId
createdAt
```

### 4.8 DecisionAuditEvent

用途：

```text
记录每个 security、policy、provider、risk、output 和 replay decision。
```

必需字段组：

```text
auditEventId
decisionId
tenantId
requestId
traceId
eventType
actorType
result
reasonCode
payloadHash
createdAt
```

规则：

```text
audit event must not store credential material
audit write failure fails closed for the first MVP unless future accepted design explicitly makes it asynchronous and traceable
```

### 4.9 DecisionReplayRecord

用途：

```text
为已存储 decision 提供 deterministic read-model replay。
```

必需字段组：

```text
replayId
decisionId
sourceSnapshotId
traceStepIds
outputId
auditEventIds
contentHash
createdAt
```

规则：

```text
replay reads stored data only
replay does not rerun provider
replay does not call NQ
replay does not mutate state
```

## 5. 模块规划

Decision Pipeline MVP 仍保持 Java 21 / Spring Boot 3.5.x modular monolith，不默认拆为 microservices 或 Spring Cloud。

| 模块 / 边界 | 职责 | 推荐类型 | 输入 / 输出 | 依赖方向 | 禁止依赖 | 测试重点 |
| --- | --- | --- | --- | --- | --- | --- |
| `dh-api` | 未来 signed request entry 与 response mapping，必须等后续 WO 授权 API | `DecisionController` 仅在后续 WO 授权后出现，request/response mappers | Signed JSON in, `DecisionOutput` out | depends on `dh-usecase`, `dh-security` | NQ client, provider SDK, DB entity exposure | auth, validation, error mapping, no dangerous path |
| `dh-domain` | Decision value objects 与 enums | `DecisionRequest`, `DecisionOutput`, `RiskReview`, `DecisionAction`, `DecisionType` | immutable domain objects | no infra dependency | Spring web, JDBC, provider SDK, NQ runtime | enum constraints, fail-closed factories |
| `dh-usecase` | orchestration ports 与 application service | `DecisionOrchestrator`, `DecisionPolicyService`, `DecisionRiskReviewService` | domain command in, domain result out | depends on domain ports | controller, JDBC implementation, real provider | linear flow, policy denied, provider timeout, audit failure |
| `dh-security` | signing、replay、tenant binding、policy gates | `DecisionRequestAuthenticator`, `DecisionPolicyGuard`, `DecisionReplayGuard` | headers/body in, auth/policy result out | no provider dependency | provider SDK, NQ runtime, secret logging | HMAC, nonce, timestamp, tenant escape, fail-closed |
| `dh-policy` | 逻辑 policy 边界，未来可放在 usecase/security 包下 | `ProviderTrustPolicy`, `ForbiddenActionPolicy`, `AutonomyPolicy` | decision context in, policy result out | domain/security only | connector real clients, DB mutation | forbidden action rejected, unsafe autonomy blocked |
| `dh-provider` / `dh-connector` | mock 与 disabled decision provider ports | `DecisionProvider`, `MockDecisionProvider`, `DisabledDecisionProvider` | normalized context in, provider signal out | connector depends on domain ports | real LLM SDK, HTTP client, NQ client | disabled default, timeout abstain, spoofing fail-closed |
| `dh-audit` | 逻辑 audit 边界，未来在 usecase/infra 下实现 | `DecisionAuditPort`, `DecisionAuditEventWriter` | audit event in, write result out | usecase port, infra adapter | provider or NQ mutation | audit write failure, event shape, no secrets |
| `dh-infra` | migration 获批后的 persistence adapters | `JdbcDecisionSnapshotRepository`, `JdbcDecisionTraceRepository` only after DB WO | repository port in/out | implements usecase ports | domain depending on infra | SQL shape, transaction boundary, no N+1, pagination |
| `dh-observability` | metrics 与 trace naming | `DecisionPipelineMetrics`, `ProviderHealthMetrics` | counters/timers | shared utility | business decisions | latency/cost/failure metrics with no secrets |
| `dh-contract` | 逻辑 contract artifacts，只有未来 WO 授权后才改 `contracts/**` | JSON Schema files | contract fixtures | read by tests | runtime mutation | schema required/additionalProperties/enum checks |
| `dh-app` | future wiring、profiles、保守默认值 | `DecisionPipelineWiringConfig` | Spring beans | depends on implementation modules | real provider enabled by default | disabled startup, conditional beans, no RealClient |

说明：

- 现有 `dh-providers` 是仓库历史 / legacy module name。Stage4 MVP 不得用它引入真实 provider。
- `dh-policy`、`dh-audit` 和 `dh-contract` 是规划边界。新增 Maven module 必须另起已接受 WO。
- 任何未来 DB table 都必须先经过 migration review task。

## 6. 安全边界矩阵

| 风险 | 等级 | 触发场景 | 影响 | 防御设计 | 验收测试 |
| --- | --- | --- | --- | --- | --- |
| prompt injection | P1 | evidence text 要求 DH 忽略 policy | unsafe recommendation | evidence 始终按 untrusted data 处理，policy 先于 provider output trust | malicious evidence -> ABSTAIN / BLOCKED |
| tool injection | P1 | provider output 请求 tool call 或 NQ mutation | side-effect attempt | Stage4 不执行 tool；始终执行 forbiddenActions | provider signal with PLACE_ORDER -> reject |
| provider spoofing | P1 | fake provider id 声称是 trusted real provider | trust bypass | provider registry allowlist，mode 仅 MOCK/DISABLED | unknown provider -> fail closed |
| replay attack | P1 | duplicate signed request 或 nonce reuse | duplicate decision/audit confusion | nonce replay guard + idempotency key + audit event | same nonce -> reject / duplicate contract behavior |
| timestamp bypass | P1 | old/future timestamp 或非 UTC 格式 | replay window bypass | RFC3339 UTC Z + bounded window | epoch / offset / expired timestamp -> reject |
| tenant escape | P0 | header/body tenant 与 auth context 不一致 | cross-tenant data exposure | authenticated tenant is authority；mismatch fail-closed | tenant mismatch -> reject |
| source forgery | P1 | untrusted source 发送 signed-like request | untrusted input accepted | source allowlist + HMAC + audit | unknown source -> reject |
| model hallucination | P1 | provider 编造 evidence 或 action | unsafe output | evidence refs required；no evidence -> ABSTAIN | missing evidence -> ABSTAIN |
| unsafe autonomous trading | P0 | output action 变成 trading instruction | real trading risk | action enum 排除交易语义；forbiddenActions 固定 | BUY / SELL / PLACE_ORDER / CANCEL_ORDER rejected |
| over-permission tool call | P1 | pipeline 尝试 MCP/write/NQ tool | unauthorized side effect | Stage4 无 tool runtime；静态 forbidden scope | tool-call field -> fail closed |
| unbounded memory/context growth | P2 | 大 evidence/context snapshot | memory exhaustion | payload cap、evidence ref limits、page/read limits | oversize context -> reject |
| cost explosion | P2 | provider loop 或过量调用 | budget overrun | provider budget guard 与 max call count | budget exceeded -> ABSTAIN |
| latency spike | P2 | provider 卡住或下游慢 | request saturation | timeout guard，无 infinite retry | timeout -> ABSTAIN with audit |
| provider fallback failure | P1 | primary/fallback unavailable | false confidence | disabled/mock fallback only produces ABSTAIN | fallback failure -> ABSTAIN |
| audit write failure | P1 | audit sink unavailable | unreplayable decision | first MVP fail-closed on audit write failure | audit writer error -> no DecisionOutput success |
| replay data tampering | P1 | stored trace/snapshot hash mismatch | false replay result | contentHash 与 immutable read model | hash mismatch -> replay invalid |

## 7. Golden Cases / Eval 基线

| Case | 输入 | 期望输出 | 期望 audit | 禁止副作用 |
| --- | --- | --- | --- | --- |
| `valid_no_trade` | valid request, sufficient evidence, low risk, no trade need | `NO_TRADE` | policy allowed + risk low + output created | no NQ call, no order |
| `policy_blocked` | request asks forbidden action | `BLOCKED` or `ABSTAIN` | policy denied | no provider call required, no order |
| `provider_timeout_abstain` | provider exceeds timeout | `ABSTAIN` | provider timeout + output abstain | no retry loop |
| `high_risk_abstain` | risk level HIGH | `ABSTAIN` or `NO_TRADE` | risk high | no LONG_BIAS / SHORT_BIAS |
| `no_evidence_abstain` | evidenceRefs empty or invalid | `ABSTAIN` | evidence insufficient | no provider trust |
| `forbidden_action_rejected` | action BUY / SELL / PLACE_ORDER / CANCEL_ORDER | contract reject | contract violation | no audit success event as allowed |
| `invalid_contract_fail_closed` | missing required fields or additionalProperties | reject | validation failure | no provider call |

## 8. Acceptance / Freeze 计划

未来 acceptance report：

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
```

未来 acceptance checks：

```text
contracts frozen and reviewed
WO accepted before implementation
no Java/test/API/migration changes in plan-only stage
no real provider
no real HTTP
no NQ runtime integration
no NQ mutation
no LIVE
golden cases defined
audit/replay model reviewed
security matrix covered
readiness decisions recorded
```

只有在用户明确授权 freeze/archive 后，future freeze 才能把 `docs/current` 复制到 gate snapshot。

## 9. Readiness Decision

```text
ALLOW_STAGE4_PLAN_CLOSE: YES
ALLOW_STAGE4_WO: YES
ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

解释：

- `ALLOW_STAGE4_PLAN_CLOSE: YES` 表示本 planning document 可作为 Stage4 Plan close candidate 接受 review。
- `ALLOW_STAGE4_WO: YES` 表示下一轮文档任务可以产出 `DH-STAGE4-DECISION-PIPELINE-MVP-WO`。
- `ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO` 表示 WO 写完、review、accepted 之前不得开工实现。
- `ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES` 表示之后可单独准备基于 GateN 的 planning-only dry-run Integration-1 文档。
- 所有 runtime、agent、LangGraph、LIVE 决策仍保持 `NO`。

## 10. 验证命令

本 docs-only plan 至少用以下命令验证：

```powershell
git status --short
git diff --check
git diff --stat
mvn test
mvn -Pquality validate
```

如果 Maven 被本地仓库或网络依赖下载阻塞，必须记录真实失败原因，不得声称测试成功。

## 11. 回滚方式

回滚按文件级执行：

```powershell
git restore --worktree -- docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md docs/current/README.md docs/current/STATUS.md docs/current/ROADMAP.md docs/current/WORK_ORDER.md docs/current/API.md docs/current/TESTING.md docs/current/WORKLOG.md
```

执行前必须确认同一文件没有用户在本计划更新之后追加的新改动。
