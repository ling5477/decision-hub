# DH stage-qdr-2 Work Order

```text
Task: DH-STAGE-QDR-2-AUDIT-TRACE-READMODEL-AND-HUMAN-APPROVAL-WO
Stage: stage-qdr-2 = Audit Trace Read Model + Human Approval Packet
Task type: WORK_ORDER_ONLY + STAGE_QDR_2_PLANNING + AUDIT_TRACE_READMODEL_DESIGN + HUMAN_APPROVAL_DESIGN + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE
Status: ACCEPTED / FINAL_CLOSE_CLOSED / STAGE_QDR_3_PLAN_READY
Implementation status: DONE / B1_B2_B3_B4_CLOSED_ACCEPTED_COMMITTED / B5_ACCEPTED
Fact source: docs/current
```

## 1. 目标与边界

stage-qdr-2 的目标是在 stage-qdr-1 已完成的 `decision_request`、`decision_run`、`quant_signal`、`quant_decision` 基础上，补齐只读审计查询与人工审批包，使一条 dry-run / quant review 结果可被查看、解释、审批、拒绝和追踪。

本 Work Order 原始版本只编制后续 implementation 的可执行工单。2026-07-06 已由 B1 消费第一批范围：新增 read model DTO / projection、tenant-bound query contract、unit tests 与 architecture guard；同日 B2 消费第二批范围：新增只读 JDBC read adapter、usecase query service、authenticated detail/trace GET API 与回归测试；同日 B3 消费第三批范围：新增 `human_approval_packet` migration、approval domain / status machine、repository port / service、JDBC adapter 与回归测试；同日 B4 消费第四批范围：新增 tenant-bound approval create / get / decision API、approval command service、audit event 写入、WebMvc/service tests、wiring 与 architecture guard。B1-B4 均已 `CLOSED / ACCEPTED / COMMITTED`。2026-07-07 已完成 B5 close review accepted 结论写回：stage-qdr-2 implementation `DONE`、acceptance `ACCEPTED`、final close `CLOSED`。当前仍未新增 replay execution API、model/provider/runtime 或 LIVE。

## 2. 前置状态

```text
stage-qdr-1 implementation: DONE
stage-qdr-1 freeze: CLOSED / ACCEPTED
V6 decision_request: EXISTS
V6 decision_run: EXISTS
V6 quant_signal: EXISTS
V6 quant_decision: EXISTS
POST /api/ai/decision-dry-runs success path: writes QDR four tables
V5 dh_decision_* audit chain: retained
stage-qdr-2 implementation: DONE
B1 read model DTO/query contract: CLOSED / ACCEPTED / COMMITTED
B2 read repository/API: CLOSED / ACCEPTED / COMMITTED / READ_ONLY
B3 human approval migration/domain/repository: CLOSED / ACCEPTED / COMMITTED / NO_API
B4 human approval API: CLOSED / ACCEPTED / COMMITTED
B5 close review: YES / ACCEPTED / REVIEW_ONLY
human_approval_packet: MIGRATION_ADDED
approval API: IMPLEMENTED_BY_VALIDATION
approval write endpoint: IMPLEMENTED_BY_VALIDATION
replay execution API: NOT STARTED
model_call: NOT STARTED
prompt version: NOT STARTED
tool registry: NOT STARTED
stage-qdr-3 planning: READY
stage-qdr-3 implementation: NOT STARTED
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

如果 stage-qdr-1 的代码和文档变更尚未提交，stage-qdr-2 implementation 前必须先提交或归档 stage-qdr-1，避免 stage-qdr-1 implementation diff 与 stage-qdr-2 docs / code diff 混线。

## 3. 不做事项

stage-qdr-2 不做：

```text
不接真实 provider
不接真实 HTTP
不接 LangGraph
不做复杂多 Agent
不做自动交易
不做自动审批
不修改 NQ
不新增前端
不做 model gateway
不做 prompt version
不做 tool registry
不做 RAG
不新增 model_call
不新增 prompt_template / prompt_version
不新增 tool_definition / tool_invocation
不修改 V7 human_approval_packet migration
不把 LONG_BIAS / SHORT_BIAS 映射为 BUY / SELL
不把 APPROVED 解释为 BUY
不把 REJECTED 解释为 SELL
不把 NO_TRADE 解释为下单
```

## 4. 退出条件

stage-qdr-2 只有在后续小批次全部实现并验证后才允许关闭。退出条件如下：

1. 能通过只读 API 查询 `decision_request` / `decision_run` / trace / output / `quant_decision`。
2. 能生成 `human_approval_packet`。
3. 能记录人工审批状态。
4. 审批只能改变 DH 内部 approval 状态，不能触发 NQ mutation。
5. 所有查询必须 tenant-bound。
6. 所有写入必须 fail-closed。
7. `LONG_BIAS / SHORT_BIAS` 仍不映射 `BUY / SELL`。
8. Maven test 与 quality validate 通过。
9. no-live-order guard 通过。

## 5. Audit Trace Read Model 设计草案

Read model 只读，不触发 replay execution，不触发外部 HTTP，不跨 tenant，不返回 raw credential、raw provider response、raw prompt secret 或 executable trading instruction。

### 5.1 DecisionRunDetailView

```text
decisionRequestId
decisionRunId
tenantId
traceId
requestId
requestKey
requestType
sourceSystem
sourceRefId
status
runNo
startedAt
finishedAt
latencyMs
errorCode
errorMessage
quantSignalSummary
quantDecisionSummary
outputSummary
createdAt
```

最小查询来源：

```text
decision_request
decision_run
quant_signal
quant_decision
dh_decision_output
```

### 5.2 DecisionTraceTimelineView

```text
decisionRunId
traceId
steps[]
  stepId
  stepNo or occurredAt
  stepName
  stepType
  status
  startedAt
  finishedAt
  latencyMs
  errorCode
  errorMessage
  inputSummaryJson
  outputSummaryJson
  providerCallRef
  auditRef
```

排序规则：优先使用 `stepNo`；如果 V5 trace 仍无 `stepNo`，则使用 `startedAt` / `createdAt` 稳定排序。后续实现不得通过重跑 orchestrator 或 provider 来补 trace。

### 5.3 DecisionEvidenceView

```text
decisionRunId
contextSnapshotRef
providerCallLogRefs
decisionOutputRef
quantDecisionRef
redactionStatus
evidenceRefsJson
```

`redactionStatus` 必须表达 `REDACTED`、`SUMMARY_ONLY`、`NO_SENSITIVE_DATA` 或 `NOT_APPLICABLE`，不得因为证据不可读而返回 raw payload。

## 6. Human Approval Packet 设计草案

`human_approval_packet` 已由 B3 新增为 `V7__human_approval_packet.sql`。该表只记录 DH 内部人工审查证据，不是交易授权表，不触发 NQ mutation、真实 HTTP、provider、order、cancel、risk、ledger、paper 或 LIVE。

### 6.1 表结构草案

```text
human_approval_packet
  id uuid primary key
  decision_run_id uuid not null references decision_run(id)
  tenant_id varchar not null
  trace_id varchar not null
  request_id varchar not null
  approval_key varchar not null
  approval_type varchar not null
  approval_status varchar not null
  risk_level varchar not null
  decision_action varchar not null
  confidence_score numeric(5,4) null
  summary text null
  checklist_json jsonb not null
  evidence_refs_json jsonb null
  reviewer_id varchar null
  reviewer_note text null
  decided_at timestamptz null
  created_at timestamptz not null
  updated_at timestamptz not null
```

建议约束：

```text
unique(tenant_id, approval_key)
approval_status in (PENDING, APPROVED, REJECTED, NEEDS_REVIEW, EXPIRED)
approval_type in (QUANT_DECISION_REVIEW, RISK_REVIEW, STRATEGY_RELEASE_REVIEW, ANOMALY_REVIEW)
decision_action in (OBSERVE, NO_TRADE, LONG_BIAS, SHORT_BIAS, NEEDS_REVIEW, REJECTED)
decision_action not in (BUY, SELL, PLACE_ORDER, CANCEL_ORDER, MARKET_ORDER, LIMIT_ORDER)
confidence_score is null or confidence_score between 0 and 1
```

索引：

```text
idx_human_approval_packet_tenant_created_at(tenant_id, created_at)
idx_human_approval_packet_decision_run_id(decision_run_id)
idx_human_approval_packet_status(approval_status)
idx_human_approval_packet_trace_id(trace_id)
idx_human_approval_packet_request_id(request_id)
```

### 6.2 审批语义

approval 只改变 DH 内部审批状态，不触发下单、不修改 NQ、不调用外部 HTTP、不升级权限、不允许自动审批。审批包必须 tenant-bound，并保留 `reviewer_id`、`reviewer_note`、`decided_at`。

审批状态修改失败必须 fail-closed；audit 写失败也不能返回成功。

### 6.3 状态机

```text
PENDING -> APPROVED
PENDING -> REJECTED
PENDING -> NEEDS_REVIEW
NEEDS_REVIEW -> APPROVED
NEEDS_REVIEW -> REJECTED
NEEDS_REVIEW -> EXPIRED
PENDING -> EXPIRED
APPROVED / REJECTED / EXPIRED 为终态
禁止 APPROVED -> PENDING
禁止 REJECTED -> APPROVED，除非后续另建 revision；stage-qdr-2 不做 revision
```

## 7. API 合同与实现状态

B2 已实现 read-only decision run detail / trace API；B4 已实现 approval packet create / get / decision API。当前仍未更新 OpenAPI 正式契约，`contracts/openapi.yaml` 仍是 formal contract source，B4 只在代码与 current docs 记录实际 endpoint。所有 approval endpoint 必须鉴权、tenant-bound、redacted，并且不得触发 replay execution、外部 HTTP、provider、NQ mutation、Agent runtime 或 LIVE。

### 7.1 查询 decision run 详情

```http
GET /api/ai/decision-runs/{decisionRunId}
```

要求：

```text
必须 tenant-bound
必须鉴权
返回 DecisionRunDetailView
不返回 credential
不返回 raw provider response
不返回 raw prompt
不返回 executable trading instruction
```

### 7.2 查询 decision run trace

```http
GET /api/ai/decision-runs/{decisionRunId}/trace
```

要求：

```text
必须 tenant-bound
必须按时间或 stepNo 排序
返回 DecisionTraceTimelineView
不触发 replay
不触发 external call
```

### 7.3 查询 approval packet

```http
GET /api/ai/approval-packets/{approvalPacketId}
```

要求：

```text
必须 tenant-bound
必须鉴权
只读
返回审批包详情
不返回 tenantId、credential、raw provider response、raw prompt 或 executable trading instruction
Status: IMPLEMENTED_BY_VALIDATION / B4
```

### 7.4 创建 approval packet

```http
POST /api/ai/decision-runs/{decisionRunId}/approval-packets
```

要求：

```text
只能基于已存在 decision_run 创建
decision_run 必须属于同 tenant
不允许重复创建同 approval_key
不允许自动审批
不触发 NQ mutation
不触发 replay execution、provider、外部 HTTP 或交易链路
初始状态只能是 PENDING
Status: IMPLEMENTED_BY_VALIDATION / B4
```

### 7.5 提交审批决定

```http
POST /api/ai/approval-packets/{approvalPacketId}/decision
```

请求字段：

```text
decision: APPROVED / REJECTED / NEEDS_REVIEW
reviewerNote
reviewerId 或从认证上下文获取
```

要求：

```text
只能从 PENDING 或 NEEDS_REVIEW 转移
终态不可重复提交
不触发下单
不触发外部 HTTP
不修改 quant_decision.action
只写 approval 状态和审计事件
audit write 失败必须 fail-closed
Status: IMPLEMENTED_BY_VALIDATION / B4
```

## 8. 错误码草案

```text
DECISION_RUN_NOT_FOUND
DECISION_RUN_TENANT_MISMATCH
DECISION_TRACE_NOT_FOUND
APPROVAL_PACKET_NOT_FOUND
APPROVAL_PACKET_DUPLICATE
APPROVAL_STATUS_INVALID
APPROVAL_TRANSITION_DENIED
APPROVAL_DECISION_INVALID
APPROVAL_REVIEWER_REQUIRED
AUDIT_TRACE_READ_DENIED
AUDIT_TRACE_REDACTION_REQUIRED
QDR_READMODEL_UNAVAILABLE
QDR_APPROVAL_WRITE_FAILED
QDR_APPROVAL_AUDIT_FAILED
```

规则：

```text
unknown error 必须 fail-closed
tenant mismatch 不能返回跨租户资源存在性细节
approval write failed 不能返回 success
audit write failed 不能返回 success
```

## 9. 安全边界

stage-qdr-2 implementation 必须满足：

1. 所有 read API 必须 tenant-bound。
2. 所有 approval write API 必须鉴权。
3. approval 不触发 NQ。
4. approval 不触发 HTTP。
5. approval 不触发 provider。
6. approval 不触发 order / cancel / risk mutation / ledger mutation / paper / live。
7. 所有 response 必须 redacted。
8. 不返回 credential、token、cookie、apiKey、apiSecret、passphrase。
9. 不返回 raw prompt secret。
10. 不返回 raw provider response。
11. 不返回 executable trading instruction。
12. `LONG_BIAS / SHORT_BIAS` 仍是只读方向性意见。
13. `APPROVED` 不等于 `BUY`。
14. `REJECTED` 不等于 `SELL`。
15. `NO_TRADE` 不等于下单。
16. 审批包只是 human review evidence，不是交易授权。

## 9.1 Discipline closeout 规则

2026-07-06 已完成 `DH-STAGE-QDR-2-DISCIPLINE-CLOSEOUT`。当前 DH 实际工作区为 `E:/Project/decision-hub`；`F:/project/decision-hub` 只能作为历史路径出现，不得作为当前执行路径自动切换。

后续 review 触发条件只保留：

```text
migration / 表结构变化
API / Controller 变化
auth / tenant / HMAC / nonce / source allowlist 变化
audit fail-closed / approval / replay 变化
stage close / acceptance
P0/P1 blocker fix
```

普通 implementation batch 只要求 implementation、tests、boundary scan、minimal docs/current sync 和 commit，不再默认每个普通 batch 单独做长 review。B5 close review 是 stage-qdr-2 的验收节点，允许 review；stage-qdr-3 启动前必须先完成 B5。

## 10. 实现批次

stage-qdr-2 implementation 必须拆成小批次，不得一次大提交。

### Batch 1

```text
DH-STAGE-QDR-2-B1-READMODEL-QUERY-DESIGN-AND-DTO
Status: CLOSED / ACCEPTED / COMMITTED
```

目标：

```text
增加 read model DTO / domain projection
不新增 DB 表
不新增 approval
查询 decision_run / trace / output / quant_decision
只读
```

已交付：

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
unit tests
ArchitectureTest readmodel dependency guard
```

仍未交付：

```text
read repository
API / Controller / OpenAPI path
JDBC implementation
approval write
human_approval_packet migration
replay read API
```

### Batch 2

```text
DH-STAGE-QDR-2-B2-READMODEL-REPOSITORY-AND-API
Status: CLOSED / ACCEPTED / COMMITTED / READ_ONLY
```

目标：

```text
增加 tenant-bound read repository：DONE
增加 GET decision run detail：DONE
增加 GET trace：DONE
增加 WebMvc tests：DONE
不新增 approval write
```

### Batch 3

```text
DH-STAGE-QDR-2-B3-HUMAN-APPROVAL-MIGRATION-AND-DOMAIN
Status: CLOSED / ACCEPTED / COMMITTED / NO_API
```

目标：

```text
新增 human_approval_packet migration：DONE
新增 approval domain/status machine：DONE
新增 repository port/service：DONE
新增 JDBC repository adapter：DONE
不新增 API write：DONE / NOT IMPLEMENTED
```

### Batch 4

```text
DH-STAGE-QDR-2-B4-HUMAN-APPROVAL-API-AND-AUDIT
Status: CLOSED / ACCEPTED / COMMITTED
```

目标：

```text
新增 create approval packet API
新增 approval decision API
写审计
状态转移 fail-closed
不触发 NQ / HTTP / provider
```

已交付：

```text
HumanApprovalPacketCommandService
ApprovalWriteBoundary
CreateApprovalPacketCommand
SubmitApprovalDecisionCommand
ApprovalPacketView
HumanApprovalPacketController
DecisionAuditEventType B4 human approval event types
DecisionPipelineWiringConfig approval repository/service/command service/write boundary wiring
DhApiAuthenticationFilter approval endpoint protection
HumanApprovalPacketCommandServiceTest
HumanApprovalPacketControllerWebMvcTest
ArchitectureTest B4 approval API guards
```

仍未交付：

```text
B5 stage-qdr-2 close review / freeze
OpenAPI formalization
replay execution API
model gateway / provider / Agent runtime
```

### Batch 5

```text
DH-STAGE-QDR-2-B5-CLOSE-REVIEW
Status: READY / NOT STARTED / REVIEW_ONLY
```

目标：

```text
review/freeze stage-qdr-2
验证 no-live-order
验证 tenant-bound
验证 approval 不触发交易
验证测试与质量门禁
```

## 11. 测试矩阵

### 11.1 Migration tests

```text
human_approval_packet migration presence
check constraints
FK
indexes
no destructive DDL
```

### 11.2 Repository tests

```text
tenant-bound decision run detail query
tenant-bound trace query
approval packet create
approval decision transition
duplicate approval_key reject
terminal state transition reject
cross-tenant query denied
```

### 11.3 WebMvc tests

```text
GET decision run detail success
GET decision run detail tenant mismatch denied
GET trace success
GET trace redaction
POST approval packet create success
POST approval decision APPROVED success
POST approval decision REJECTED success
invalid transition fail-closed
missing auth fail-closed
no BUY / SELL / PLACE_ORDER / CANCEL_ORDER in response
```

### 11.4 Architecture tests

```text
domain 不依赖 api/infra/security
usecase 不依赖 provider SDK
no real HTTP client
no LangGraph/AutoGen/CrewAI
no order/risk/ledger/paper/live mutation
```

### 11.5 Security tests

```text
no credential leakage
no raw provider response
no raw prompt secret
no tenant leakage
audit write failure fail-closed
approval write failure fail-closed
```

### 11.6 Quality validation

```powershell
mvn -ntp -pl dh-app -am test
mvn -ntp -Pquality validate
forbidden scan
stage naming scan
```

Docker/Testcontainers skip 只能写成 skip，不能写成 PASS。`mvnw.cmd` 当前已知不可用；后续验证仍必须真实执行并记录输出，不得把 wrapper 写成可用。

## 12. 回滚要求

```text
Batch 1 rollback: 移除 read model DTO / projection 与对应 tests，不影响 V6 四表。
Batch 2 rollback: 移除 read repository、GET API、Controller route 与 WebMvc tests，不触碰 approval。
Batch 3 rollback: 回滚 human_approval_packet migration 草案和 domain/repository；若 migration 已发布，必须走兼容迁移，不得直接 drop 生产表。
Batch 4 rollback: 关闭 approval API feature gate，移除 write route / service wiring；保留审计可追踪记录。
Batch 5 rollback: 仅回滚 close/freeze 文档，不回滚已接受代码；若发现边界失败，状态必须写 BLOCKED。
```

任何 rollback 都不得删除 stage-qdr-1 的 V6 `decision_request`、`decision_run`、`quant_signal`、`quant_decision`，不得修改 NQ 仓库，不得触碰 LIVE。

## 13. 验收命令

```powershell
git status --short
git branch --show-current
git diff --check
git diff --stat
git diff --name-only

rg -n "GateK|GateL|GateM|stage-qdr|stage" docs/current

rg -n "BUY|SELL|PLACE_ORDER|CANCEL_ORDER|MARKET_ORDER|LIMIT_ORDER|placeOrder|cancelOrder|submitOrder|executeOrder|bypassRisk|forceExecute|mutateLedger|mutateRisk|paperRunStart|liveRunStart|apiKey|apiSecret|passphrase|credential|token|cookie|LangGraph|AutoGen|CrewAI|OpenAI|Anthropic|Gemini|Ollama|HttpClient|WebClient|RestTemplate|OkHttp" dh-* docs/current contracts golden_cases

mvn -ntp -pl dh-app -am test
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

## 14. Readiness decision

```text
STAGE_QDR_2_WO: DONE
STAGE_QDR_2_B1: CLOSED / ACCEPTED / COMMITTED
STAGE_QDR_2_B2: CLOSED / ACCEPTED / COMMITTED
STAGE_QDR_2_B3: CLOSED / ACCEPTED / COMMITTED
STAGE_QDR_2_B4: CLOSED / ACCEPTED / COMMITTED
STAGE_QDR_2_B5: YES / ACCEPTED / REVIEW_ONLY
STAGE_QDR_2_IMPLEMENTATION_OVERALL: DONE
ALLOW_STAGE_QDR_2_B4_REVIEW_FREEZE: NO / CLOSED
ALLOW_STAGE_QDR_2_B5_CLOSE_REVIEW: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_3_PLAN: YES
ALLOW_STAGE_QDR_3_IMPLEMENTATION: NO
ALLOW_STAGE_QDR_2_FULL_IMPLEMENTATION_NOW: NO
ALLOW_REPLAY_EXECUTION_NOW: NO
ALLOW_DB_MIGRATION: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步 close review 批次名称：

```text
DH-STAGE-QDR-2-B5-CLOSE-REVIEW
```
