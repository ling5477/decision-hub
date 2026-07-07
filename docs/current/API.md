# Decision Hub API

## 1. 当前状态

```text
当前阶段: DH-STAGE-QDR-3-B3-PERSISTENCE-BASELINE / IMPLEMENTED_BY_VALIDATION / NO_API_CHANGE
下一阶段: DH-STAGE-QDR-3-B3-REVIEW-FREEZE / READY / NO_DIRECT_B4 / NO_API_IMPLEMENTATION
```

OpenAPI 单源：`contracts/openapi.yaml`。

当前状态锁定：

```text
Integration-0:        CLOSED / ACCEPTED
Header alignment:     CLOSED
Timestamp alignment:  CLOSED / ACCEPTED
Code reality audit:   DONE
Stage4-PLAN:          ACCEPTED / CLOSED
Stage4-WO:            ACCEPTED / CLOSED
K1-K8 Stage4 MVP:      CLOSED / ACCEPTED
Decision pipeline API: NOT IMPLEMENTED
Limited runtime plan: CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME
Runtime API contract review: CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME
DH runtime API WO: CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION
DH limited runtime endpoint: IMPLEMENTED / DH_ONLY / DEFAULT_DISABLED / DEV_TEST_ENABLE_ONLY / CLOSE_REVIEW_ACCEPTED / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
Decision Core baseline: CLOSED / ACCEPTED / decision_request + decision_run + quant_signal + quant_decision
stage-qdr-2 read model DTO/query contract: IMPLEMENTED / USECASE_ONLY / NO_ENDPOINT
stage-qdr-2 read model API: IMPLEMENTED / READ_ONLY / TENANT_BOUND / NO_OPENAPI_FORMALIZATION
stage-qdr-2 human approval domain/repository: IMPLEMENTED / INTERNAL_ONLY
stage-qdr-2 approval API: CLOSED / ACCEPTED / COMMITTED / TENANT_BOUND / AUDIT_FAIL_CLOSED / NO_OPENAPI_FORMALIZATION
replay execution API: NOT IMPLEMENTED
stage-qdr-2 B5 close review: YES / ACCEPTED / REVIEW_ONLY / NO_NEW_FEATURE
stage-qdr-3 planning: DONE / PLAN_ONLY
stage-qdr-3 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-3 implementation: B3_IMPLEMENTED_BY_VALIDATION
B1 Prompt/Model Version Domain: IMPLEMENTED_BY_VALIDATION / DOMAIN_USECASE_ONLY / NO_ENDPOINT
B2 Model Gateway Mock Runtime: CLOSED / ACCEPTED / COMMITTED / USECASE_ONLY / NO_ENDPOINT
B3 Persistence Baseline: IMPLEMENTED_BY_VALIDATION / V8_JDBC_BASELINE / NO_ENDPOINT
B4 QDR Pipeline Integration: NOT STARTED
Model Gateway API: NOT STARTED
Provider API: NOT STARTED
Prompt Version API: NOT STARTED
Integration-1:        NOT STARTED
Runtime integration:  NOT STARTED
AI / Agent runtime:   NOT STARTED
LIVE:                 DISABLED
```

OpenAPI 仍为正式契约单源；B4 未修改 `contracts/openapi.yaml`、`contracts/json-schema/**`、`golden_cases/**` 或 fixture JSON。DH Stage4 Decision Pipeline MVP K1-K8 已 `CLOSED / ACCEPTED`；`DecisionRequest` / `DecisionOutput` 已作为 K1 domain contract 与 JSON Schema 落地；audit / snapshot / trace persistence 与 internal replay read model 已在 usecase/infra 内闭环，但 replay execution API 仍未实现。`NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION` 已在 DH 侧实现受限 inbound endpoint `POST /api/ai/decision-dry-runs`，该 endpoint 默认关闭，仅 dev/test profile 可显式启用，production profile disabled / kill switch fail-closed。`stage-qdr-1` 已关闭：成功 dry-run 会创建 `decision_request`、`decision_run`、`quant_signal` 与 `quant_decision`，并继续保留 V5 `dh_decision_*` audit / trace / output 链路。stage-qdr-2 B1/B2/B3/B4 已 `CLOSED / ACCEPTED / COMMITTED`；B4 已新增 tenant-bound approval create / get / decision API 与审计事件写入。approval API 只改变 DH 内部 approval 状态并写审计，不触发 NQ、交易、provider、HTTP 或 replay execution。stage-qdr-3 planning 与 implementation work order 已完成；B1 已提交 domain/usecase/mock registry baseline；B2 已 `CLOSED / ACCEPTED / COMMITTED`；B3 只新增 V8 migration、persistence ports、JDBC repositories 与 tests，不新增 API，不新增 Controller，不修改 OpenAPI，不新增 Provider API、Model Gateway API 或 Prompt Version API。B4/B5 仍为 `NOT STARTED`；任何 future Model Gateway / Provider / Prompt Version API 必须另起 API / contract / security review。`NQ_DRYRUN` 只进入 dev/test allowlist，不进入 production allowlist；实现不包含 NQ runtime client implementation、真实 outbound HTTP、real provider、Agent / LangGraph runtime 或 LIVE。

### 1.1 stage-qdr-3 B3 API 结论

```text
B3 new endpoint: NO
B3 new Controller: NO
B3 OpenAPI change: NO
B3 Model Gateway API: NOT STARTED
B3 Provider API: NOT STARTED
B3 Prompt Version API: NOT STARTED
B3 replay execution API: NOT STARTED
B4/B5: NOT STARTED
```

B3 persistence baseline 只服务内部 repository 与后续 review/freeze，不改变任何 wire contract。B3 不授权真实 provider、真实 HTTP、Provider SDK、LangGraph / AutoGen / CrewAI、Agent runtime、NQ mutation 或 LIVE。

## 2. 已实现端点

```text
GET  /_ping                                          健康检查
POST /api/ai/research-runs                           创建 ResearchRun
GET  /api/ai/research-runs                           列出当前租户全部 run
GET  /api/ai/research-runs/{runId}                   查询 run 详情
POST /api/ai/research-runs/{runId}/start             启动 run
GET  /api/ai/research-runs/{runId}/tasks             查询 run 的任务图
GET  /api/ai/research-runs/{runId}/candidates        查询 run 下的候选列表
GET  /api/ai/research-runs/{runId}/judge-decision    查询 run 的 JudgeDecision
POST /api/ai/feedback/nq                             NQ -> DH 正式回流事件 ingest（Stage2-PoC-B2 落地）
                                                      - eventId 幂等去重；首次/重放均返回 202
                                                        + NqFeedbackAcceptedResponse（outcome=ACCEPTED|DUPLICATE）
                                                      - 信封 / payload 校验失败返回 400
                                                        + NqFeedbackErrorResponse
                                                      - forbidden field / forbidden capability 命中返回 400
                                                        + errorCode=FORBIDDEN_FIELD|FORBIDDEN_CAPABILITY
POST /api/ai/decision-dry-runs                       DH limited dry-run inbound endpoint（DH-only / default disabled）
                                                      - 仅 signed / timestamped / nonce / tenant-bound request
                                                      - dev/test 可显式启用；production disabled / kill switch fail-closed
                                                      - response 仅 read-only snapshot；不返回 BUY / SELL / 可执行订单字段
                                                      - stage-qdr-1 起内部写入 decision_request / decision_run / quant_signal / quant_decision
                                                      - 主线落库或 V5 audit 落库失败必须 fail-closed
GET  /api/ai/decision-runs/{decisionRunId}           stage-qdr-2 B2 read-only detail API
                                                      - 必须认证；tenant 只从认证上下文读取
                                                      - 只读 V6/V5 现有表；不返回 tenantId 或 raw provider/prompt/material
GET  /api/ai/decision-runs/{decisionRunId}/trace     stage-qdr-2 B2 read-only trace API
                                                      - 只读取已物化 audit trace step；不触发 replay execution
                                                      - response 仅 summary/ref；不触发 HTTP、provider、approval write 或 LIVE
POST /api/ai/decision-runs/{decisionRunId}/approval-packets
                                                     stage-qdr-2 B4 approval packet create API
                                                      - 必须认证；tenant 只从认证上下文读取
                                                      - 基于当前 tenant 下已存在 decision_run 创建 PENDING approval packet
                                                      - 写入 approval packet 与 audit event；audit/repository 失败 fail-closed
GET  /api/ai/approval-packets/{approvalPacketId}     stage-qdr-2 B4 approval packet read API
                                                      - 必须认证；tenant-bound；只返回 redacted API response DTO
POST /api/ai/approval-packets/{approvalPacketId}/decision
                                                     stage-qdr-2 B4 approval decision API
                                                      - 只允许 APPROVED / REJECTED / NEEDS_REVIEW
                                                      - 必须走 approval 状态机；终态重复提交返回冲突
                                                      - 只改变 DH 内部 approval 状态并写 audit event
                                                      - 不修改 quant_decision.action；不触发 NQ、交易、provider、HTTP 或 replay execution
POST /legacy/runs                                    旧链路（@Deprecated，必须认证，不允许匿名）
GET  /legacy/runs/{runId}                            旧链路（@Deprecated，必须认证且 tenant 匹配）
```

### 2.1 `POST /api/ai/decision-dry-runs` 内部持久化行为

`stage-qdr-1` 只更新现有 dry-run endpoint 的内部落库行为，不新增 path，不修改 OpenAPI，不改变 response envelope。

```text
decision_request:
  tenant_id / trace_id / request_id 必须写入
  request_key 使用 requestId
  request_type = QUANT_DECISION_REVIEW

decision_run:
  decision_request_id 关联 decision_request
  run_no = 1
  orchestrator_key = DEFAULT_DECISION_ORCHESTRATOR
  provider 字段保留为 null，不代表真实 provider

quant_signal:
  若输入可识别 signal / backtest / report / risk，则记录对应 signal_type
  否则 signal_type = UNKNOWN_REVIEW_INPUT

quant_decision:
  action 仅允许 OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS / NEEDS_REVIEW / REJECTED
  禁止 BUY / SELL / PLACE_ORDER / CANCEL_ORDER
  human_approval_status 默认 NOT_REQUIRED；B3 已新增独立 human_approval_packet migration/domain/repository，但 dry-run endpoint 不自动创建 approval packet
```

安全边界不变：HMAC、timestamp、nonce replay、tenant-source binding、payload cap、memory cap、rate limit、kill switch、forbidden material gate、audit fail-closed 均继续生效。`LONG_BIAS / SHORT_BIAS` 只表示只读方向性审查意见，不是交易指令。

## 2.2 stage-qdr-2 read model 与 approval API（B2/B4 IMPLEMENTED）

以下 read API 已由 B2 实现为 authenticated / tenant-bound / read-only endpoint；approval API 已由 B4 实现为 authenticated / tenant-bound / audit-fail-closed endpoint。它们没有写入 OpenAPI 正式契约，不触发 replay execution、外部 HTTP、provider、NQ mutation、Agent runtime 或 LIVE。

```text
GET  /api/ai/decision-runs/{decisionRunId}                  IMPLEMENTED / READ_ONLY / TENANT_BOUND
GET  /api/ai/decision-runs/{decisionRunId}/trace            IMPLEMENTED / READ_ONLY / TENANT_BOUND / NO_REPLAY_EXECUTION
GET  /api/ai/approval-packets/{approvalPacketId}            IMPLEMENTED / READ_ONLY / TENANT_BOUND
POST /api/ai/decision-runs/{decisionRunId}/approval-packets IMPLEMENTED / CREATE_PENDING_ONLY / TENANT_BOUND
POST /api/ai/approval-packets/{approvalPacketId}/decision   IMPLEMENTED / STATE_MACHINE_ENFORCED / AUDIT_FAIL_CLOSED
```

B1/B2 已实现的 read model / query / service / adapter 名称如下。DTO 仍是 usecase read model contract；API response 由 `DecisionRunReadController` 做安全映射，不把 usecase DTO 当作正式 external API contract：

```text
DecisionRunDetailView
DecisionTraceTimelineView
DecisionTraceStepView
DecisionEvidenceView
RedactionStatus
DecisionRunReadQuery
DecisionTraceReadQuery
DecisionEvidenceReadQuery
DecisionReadModelQueryPort
DecisionReadModelService
JdbcDecisionReadModelQueryAdapter
DecisionRunReadController
```

合同要求：

```text
所有 read API 必须 tenant-bound 且 redacted
所有 approval write API 必须鉴权
read API 不触发 replay execution 或 external call
approval 只改变 DH 内部审批状态，不触发 NQ mutation
response 不返回 credential、raw provider response、raw prompt 或 executable trading instruction
APPROVED 不是 BUY；REJECTED 不是 SELL；LONG_BIAS / SHORT_BIAS 不是交易指令
```

## 2.3 stage-qdr-2 approval internal domain/repository（B3 IMPLEMENTED）

B3 已实现 DH 内部 `HumanApprovalPacket` domain model、approval status machine、repository port / service 与 JDBC adapter，并新增 `V7__human_approval_packet.sql`。B4 在此基础上新增 API command service / controller / audit integration，但未修改 OpenAPI 或 migration。

```text
HumanApprovalPacket: IMPLEMENTED / DOMAIN_ONLY
ApprovalStatusTransitionPolicy: IMPLEMENTED / FAIL_CLOSED
HumanApprovalPacketRepository: IMPLEMENTED / USECASE_PORT
JdbcHumanApprovalPacketRepository: IMPLEMENTED / TENANT_BOUND
Approval API: IMPLEMENTED_BY_VALIDATION / B4
Approval write endpoint: IMPLEMENTED_BY_VALIDATION / B4
Replay execution API: NOT STARTED
```

`APPROVED` 只表示 DH 内部人工审查状态，不是 `BUY`；`REJECTED` 不是 `SELL`；approval status 不触发 NQ mutation、真实 HTTP、provider、order、cancel、risk、ledger、paper 或 LIVE。

## 2.4 stage-qdr-2 approval API 行为（B4 IMPLEMENTED_BY_VALIDATION）

`HumanApprovalPacketController` 暴露三个 B4 endpoint，但不直接暴露 persistence entity。tenantId 只来自 `AuthenticatedRequest` / `DhApiAuthenticationFilter` 写入的认证上下文；body 中不接受 tenantId、approvalStatus、decisionAction、traceId 或 final execution action。

```text
Create request:
  approvalType
  summary
  checklistJson
  evidenceRefsJson
  reviewerId / reviewerNote 为可选输入；reviewerId 优先使用认证上下文 userId

Decision request:
  decision = APPROVED / REJECTED / NEEDS_REVIEW
  reviewerNote
  reviewerId 可选，优先使用认证上下文 userId
```

错误映射：

```text
missing auth: 401
tenant header mismatch: 403
validation error / invalid decision such as BUY or SELL: 400
not found or tenant mismatch lookup: 404
duplicate approval_key: 409
terminal transition denied: 409 / APPROVAL_TRANSITION_DENIED
repository write/read failure: 500 / QDR_APPROVAL_WRITE_FAILED
audit failure: 500 / QDR_APPROVAL_AUDIT_FAILED
```

response 允许字段只包含 approval packet / decision run / approval status / risk / readonly decision action / confidence / redacted checklist/evidence/reviewer summary / timestamps。response 不返回 tenantId，不返回 raw provider response、raw prompt、credential、token、cookie、apiKey、apiSecret、passphrase、quantity、leverage 或 executable order instruction。

## 3. Historical / deferred API 方向（当前 API 未实现）

以下方向是 historical / deferred / gated，不是当前 Stage4 的已实现端点，也不得作为当前 implementation 入口：

```text
POST /api/ai/tools/forecast                       -> ForecastArtifact
POST /api/ai/research/snapshots                   -> ExternalMarketSnapshot
GET  /api/ai/research/snapshots/{snapshotId}      -> ExternalMarketSnapshot
GET  /api/ai/research-runs/{runId}/reflections    -> ReflectionEntry[]
GET  /api/ai/research-runs/{runId}/checkpoints    -> CheckpointEntry[]
```

DH Stage4 Decision Pipeline MVP 的 API 层仍未实现；只有后续已接受 WO 明确授权后，才允许新增 API / OpenAPI / migration / controller 相关变更。

## 3.1 Planned dry-run contract（未实现 API）

`NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN` 仅规划 future dry-run contract，不新增已实现 API。当前不得把下列内容写成已实现 endpoint：

```text
NQ -> DH dry-run request: planned only
DH -> NQ dry-run response: planned only
Dry-run Controller: NOT IMPLEMENTED
Dry-run API path: NOT IMPLEMENTED
OpenAPI extension: NOT IMPLEMENTED
JSON Schema extension: NOT IMPLEMENTED
Runtime HTTP: NOT STARTED
```

P2 已确认计划字段中 `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 仍属于 schema gap / envelope planning；当前 API、OpenAPI 与 JSON Schema 均未实现这些 wire-level 字段。后续若需要字段扩展或 fixture 落地，必须先进入独立 contract review；不得把 P2 planning 写成已实现 API。

P4 gate-fix 结论：上述 gap 字段在下一步 `NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO` 中只能作为 `DOC_ONLY_ALIAS` 或 future envelope planning，不进入 required fixture、OpenAPI、JSON Schema 或 Controller；`X-NQ-DH-Schema-Version` header 也未实现。`source=NQ_DRYRUN`、canonical error code names 与 dry-run endpoint shape 仍需 contract review before code，不得写成已实现 API。

M0 contract gap close 结论：dry-run endpoint shape 的推荐形态为 `Option C / test-support mock-only, no runtime endpoint`；`POST /api/ai/feedback/nq` 是 feedback ingest，不得复用为 decision dry-run endpoint；如未来需要 HTTP endpoint，必须另起 API / contract / security review。`NQ_DRYRUN` source 仍为 `NEEDS_SECURITY_CONTRACT_CHANGE`，`dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 仍为 `DOC_ONLY_ALIAS` 或 future envelope planning，不得作为 OpenAPI / JSON Schema / Controller 已实现字段。

M1 dry-run contract entry mock work order 结论：`RECOMMENDED_ENTRY_SHAPE = Option C / test-support mock-only / no runtime endpoint`；M1 只规划 contract validation chain、source allowlist、error taxonomy、audit / trace / replay 边界和 fail-closed normalization。若未来新增 dry-run HTTP endpoint、Controller、OpenAPI path、schema header 或 envelope field，必须另起 API / contract / security review；当前 API 文档不得把 M1 写成已实现 endpoint。

M2 NQ dry-run stub recorder work order 结论：`RECOMMENDED_STUB_SHAPE = test-support mock-only stub + in-memory recorder plan, no runtime HTTP client`。M2 不新增 DH API，不新增 NQ client，不新增 HTTP endpoint，不修改 OpenAPI 或 JSON Schema；request builder / recorder 仅为后续 test-support planning。若未来需要真实 client 或 endpoint，必须另起 API / contract / security / no-side-effect review。

M3 joint mock fixtures and contract tests work order 结论：M3 只规划 23 类 future fixture family 与 14 个 future contract test batch，不新增 API / endpoint / Controller，不修改 OpenAPI、JSON Schema、`contracts/**` 或 `golden_cases/**`，不创建 fixture JSON，不写测试代码。下一步 IMP0 只允许 test-support / mock-only source handling、canonical error mapping test-support 与 fixture schema support guard；任何 dry-run HTTP endpoint、Controller 或 OpenAPI 变更仍必须另起 API / contract / security review。

IMP1 DH dry-run test-support entry 结论：`DhDryRunTestSupportEntry` 只位于 `dh-usecase/src/test/**`，用于验证未来 dry-run entry 的 mock-only validation chain。它不是 Spring bean，不暴露 Controller，不注册 runtime endpoint，不修改 OpenAPI / JSON Schema / contracts / golden_cases / fixture JSON，也不授权 `NQ_DRYRUN` 进入生产 source allowlist。任何未来 dry-run HTTP endpoint、Controller、OpenAPI path、schema alias 或 runtime wiring 仍必须另起 API / contract / security review。

IMP2 NQ stub recorder no-side-effect 结论：`NqDhIntegration1StubRecorderNoSideEffectTest` 只位于 NQ dry-run worktree `backend/nq-app/src/test/**`，用于验证 future NQ request builder / readonly recorder / no-side-effect guard。它不是 DH API，不是 NQ runtime client，不暴露 Controller，不注册 endpoint，不修改 DH OpenAPI / JSON Schema / contracts / golden_cases / fixture JSON，也不授权真实 HTTP 或 `NQ_DRYRUN` production allowlist。

IMP3 joint mock contract tests 结论：`DhIntegration1JointMockContractFixtureTest` 与 `NqDhIntegration1JointMockContractFixtureTest` 只消费各自 `src/test/resources/nq-dh/integration1/joint_mock_contract_fixtures.json`，用于验证双方对 mock dry-run request / response / forbidden fields / fail-closed / no-side-effect 的理解一致。Fixture 和测试不是 API，不是 Controller，不注册 endpoint，不修改 OpenAPI / JSON Schema / `contracts/**` / `golden_cases/**`，也不授权真实 HTTP、RealClient、real provider、AI / LangGraph runtime 或 `NQ_DRYRUN` production allowlist。

Limited dry-run runtime plan 结论：`NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN` 只评估受限 runtime 前置条件，当前为 `CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME`。本计划允许后续单独进入 `NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW`，但不允许本轮新增 dry-run runtime endpoint、Controller、OpenAPI path、schema alias、`NQ_DRYRUN` production allowlist、NQ runtime client 或真实 HTTP；如未来需要实现，必须先关闭 API / contract / security / source allowlist / error taxonomy / no-side-effect review。

Runtime API contract review 结论：`NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW` 已 `CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME`。该 review 当时推荐 future endpoint candidate 为 `POST /api/ai/decision-dry-runs`，只接受 signed / timestamped / nonce / tenant-bound `NQ_DRYRUN` request，并只返回 readonly `DecisionOutput` envelope；review 本身不新增 Controller、OpenAPI path、schema/contracts/golden_cases、runtime wiring、real HTTP、real provider、Agent / LangGraph 或 LIVE。`dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 仍未进入当前正式 API schema；如后续转正式字段，必须另起 schema/envelope compatibility review。

DH runtime API work order 结论：`NQ-DH-I1-DH-RUNTIME-API-WO` 已 `CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION`。该 work order 只冻结 future `POST /api/ai/decision-dry-runs` 的 endpoint 边界、security gate、`NQ_DRYRUN` source allowlist 策略、request/response envelope、error taxonomy、audit/trace/replay、feature flag、kill switch、validation 和 rollback；WO 关闭时 endpoint 尚未实现，后续实现状态以下一段为准。implementation 仍不得接 real HTTP、real provider、NQ runtime client、Agent / LangGraph 或 LIVE，且 `LONG_BIAS / SHORT_BIAS` 不得映射为 `BUY / SELL`。

DH limited runtime endpoint implementation 结论：`NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION` 已完成 DH-only 最小实现；交付时状态为 `IMPLEMENTED / PENDING_CLOSE_REVIEW / FEATURE_FLAG_DISABLED_BY_DEFAULT / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE`，后续 close review 状态以下一段为准。实现新增 `DecisionDryRunController`、dry-run DTO、HMAC dry-run authenticator、dry-run usecase、dev/test/prod feature flag wiring 与回归测试；request 必须通过 HMAC、UTC `Z` timestamp、±300s window、nonce replay、tenant/source pair、payload cap、rate limit、memory cap、policy gate 和 audit/trace/replay gate。成功响应只包含 read-only snapshot，action 仅允许 `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；内部 `ABSTAIN` 对外映射为 `NO_TRADE` 并记录 `INTERNAL_ABSTAIN_MAPPED`。本轮未修改 OpenAPI、JSON Schema、contracts、golden_cases、fixture JSON 或 migration；未新增 NQ client、RealClient、真实 HTTP、real provider、Agent / LangGraph runtime 或 LIVE。

DH limited runtime endpoint close review 结论：`NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW` 已 `CLOSED / ACCEPTED / REVIEW_ONLY`。close review 确认 endpoint 仍是 DH-only inbound limited dry-run，安全 gate、feature flag、production disabled、kill switch、HMAC、UTC `Z` timestamp、nonce replay、tenant/source allowlist、payload cap、rate limit、memory cap、forbidden material gate、audit fail-closed、error taxonomy 与 no-side-effect boundary 均可接受。下一步只允许 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO / NOT STARTED / WORK_ORDER_ONLY`；不得直接实现 NQ client、不得真实 HTTP、不得接 real provider、不得修改 OpenAPI / JSON Schema / contracts / golden_cases、不得接 Agent / LangGraph runtime 或 LIVE。

## 4. Stage1 最小 API 集合（已实现，留作历史记录）

```text
POST /api/ai/research-runs
GET  /api/ai/research-runs
GET  /api/ai/research-runs/{runId}
POST /api/ai/research-runs/{runId}/start
GET  /api/ai/research-runs/{runId}/tasks
GET  /api/ai/research-runs/{runId}/candidates
GET  /api/ai/research-runs/{runId}/judge-decision
POST /api/ai/feedback/nq
```

## 5. API 原则

```text
所有输出结构化
所有请求带 traceId 或由服务生成 traceId
所有 Agent 输出保留 artifact 快照
所有 NQ 回流事件保留 raw payload
所有最终建议必须经过 JudgeDecision
```

## 6. 禁止 API

```text
禁止直接下单 API
禁止自动实盘发布 API
禁止绕过 NQ 风控 API
禁止复制 NQ 回测核心 API
```

## 7. NQ 对接方向

DH -> NQ：

```text
BacktestRequest
PaperTrialRequest
ReleaseReviewRequest
```

以上仅为 historical / deferred 方向，不代表当前已启动 runtime integration；当前仍禁止真实 HTTP、真实 NQ
调用、NQ mutation、RealClient、real provider 和 LIVE。

NQ -> DH：

```text
BacktestCompleted
BacktestRejected
RiskRejected
PaperTrialCompleted
ReleaseApproved
ReleaseRejected
TradeReviewCompleted
PostMortemCreated
```
