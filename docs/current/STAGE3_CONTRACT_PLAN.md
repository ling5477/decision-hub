# Stage3 Contract Plan

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md`
> Scope: 规划 Stage3 联调阶段两条链路的契约规则。本文件不修改任何已落地契约语义，仅规划。

## 1. NQ -> DH event contract

```text
传输：    HTTPS POST /api/ai/feedback/nq
content-type: application/json
body：    NqFeedbackEnvelope
schema：  contracts/openapi.yaml#components/schemas/NqFeedbackEnvelope
          contracts/json-schema/nq-feedback-envelope.schema.json
payload schema：8 选 1（contracts/json-schema/nq-feedback-*）
事件类型：NqFeedbackEventType 枚举（8 种）
```

envelope 字段（已在 Stage2-PoC-B1 / B5 落地，本 Stage 不变）：

```text
eventId        必填  NQ 端 UUIDv7，全局唯一，DH 幂等键
eventType      必填  NqFeedbackEventType
occurredAt     必填  ISO-8601，事件在 NQ 端发生时间（不是发送时间）
sourceSystem   必填  "nexus-quant"
sourceJobId    必填  NQ 端业务 Job ID（paperRunId / backtestId / alertId 等）
traceId        必填  与 DH ResearchRun.trace_id 串联
requestId      必填  若由 DH 请求触发，等于原请求 requestId；否则 NQ 端生成
correlationId  必填  跨流程上下文 ID
schemaVersion  必填  semver，例 "1.0.0"
payloadJson    必填  payload 原始 JSON 字符串（rawPayload 一份留底）
receivedAt     选填  DH 端入口时间戳（DH 写）
```

响应（NqFeedbackController，Stage2-PoC-B2 已落地，Stage3 不变）：

```text
202 Accepted + NqFeedbackAcceptedResponse
       { eventId, status, outcome: ACCEPTED|DUPLICATE, traceId, correlationId }
400 Bad Request + NqFeedbackErrorResponse
       { error: "INVALID_REQUEST", errorCode, message, eventId, traceId, correlationId }
```

## 2. DH -> NQ backtest request contract

```text
传输：    HTTPS POST /api/ai/research/backtest-requests   （NQ 侧 endpoint，待 Stage3 Batch 1 与 NQ 团队确认）
content-type: application/json
body：    DhBacktestRequest
schema：  contracts/openapi.yaml#components/schemas/DhBacktestRequest
          contracts/json-schema/dh-backtest-request.schema.json
同步响应：DhBacktestRequestAccepted
异步结果：以 NqFeedbackEnvelope(type=BACKTEST_RESULT_READY) 回传，payload schema:
          contracts/json-schema/nq-feedback-backtest-result-ready.schema.json
```

请求字段（Stage2-PoC-B1 落地，Stage3 复用）：

```text
requestId / traceId                必填
candidateId / strategyName / strategyVersion
strategyParametersJson             必填，完整 JSON 字符串
entryRulesRef / exitRulesRef       可选
startDate / endDate                必填，ISO-8601 date
initialCapital                     exclusiveMinimum 0
symbols                            minItems 1
frequency                          DAILY | HOURLY | MINUTE
requestedBy / requestedAt          必填
status                             DRAFT 初始；进入 outbox 后 QUEUED
```

同步响应：

```text
202 Accepted + DhBacktestRequestAccepted { requestId, jobId, status, acceptedAt }
400 Bad Request + 与 §4 error code 对齐
409 Conflict 同 requestId 重放（NQ 已接收），DH 视为成功且不再重发
5xx              DH 重试由出站 outbox 负责，参考 STAGE3_DH_TO_NQ_BACKTEST_PLAN.md §1
```

## 3. correlationId / traceId / requestId / sourceJobId 规则

```text
traceId
  长度：    32 位 hex 推荐（与 OpenTelemetry trace id 对齐）
  生成方：  DH 端 ResearchRun 创建时生成；DH 出站请求带上
  传递：    DH -> NQ (HTTP header X-Trace-Id + body.traceId 双通道)
            NQ -> DH feedback envelope.traceId 原样回传
  约束：    NQ feedback 中 traceId 必须能在 dh_research_runs.trace_id 找到，否则 errorCode = UNKNOWN_TRACE

requestId
  长度：    UUIDv7 推荐
  生成方：  DH 出站请求方（如 NqBacktestClient）
  传递：    DH -> NQ body.requestId
            NQ -> DH feedback envelope.requestId 必带原值
  约束：    异步 feedback 与原 DhBacktestRequest 通过 requestId 串联

correlationId
  长度：    UUIDv7 推荐
  生成方：  DH 业务上下文（如 candidate 进入 paper 周期）
  传递：    DH/NQ 双向必带；同一 correlationId 串联多个 requestId/traceId

sourceJobId
  生成方：  NQ 端（paperRunId / backtestId 等）
  作用：    NQ 内部业务实体 ID；DH 仅用于对账与排错
  约束：    feedback envelope.sourceJobId 必填

eventId
  生成方：  NQ outbox
  作用：    DH 幂等键，唯一索引（dh_nq_feedback_events.event_id）
  约束：    与 envelope 中其他字段无关，但生命周期严格单事件
```

## 4. HTTP status 规则

```text
入站（NQ -> DH /api/ai/feedback/nq）
  202 Accepted     首次接收 + NqFeedbackAcceptedResponse(outcome=ACCEPTED)
  202 Accepted     幂等重放 + NqFeedbackAcceptedResponse(outcome=DUPLICATE)
  400 Bad Request  契约错误 + NqFeedbackErrorResponse
  503 Service Unavailable  DH 临时不可用（如 DB 不通）；NQ 必须重试
  ⚠ 不会返回 5xx 表示业务拒绝；业务拒绝走 400 + errorCode

出站（DH -> NQ /api/ai/research/backtest-requests）
  202 Accepted     NQ 已入队
  400 Bad Request  契约错误（DH 自检 + NQ 拒绝两种来源；errorCode 对齐 §5）
  401 Unauthorized 认证错误（NQ 侧规定）
  409 Conflict     重放命中，DH 不再发送
  429 Too Many Requests  限速；DH 退避重试
  5xx              DH 重试 outbox 负责（详见 STAGE3_DH_TO_NQ_BACKTEST_PLAN.md）
```

## 5. error code 规则

入站（NqFeedbackErrorResponse.errorCode）已在 Stage2-PoC-B2 落地：

```text
UNKNOWN_EVENT_TYPE   envelope.eventType 不在 NqFeedbackEventType 枚举内
INVALID_SCHEMA       envelope / payload 不符合 schemaVersion 声明的契约
UNKNOWN_TRACE        envelope.traceId 在 DH dh_research_runs 中找不到
```

出站（DH -> NQ backtest request）规划新增（NQ 团队实施，DH 仅声明期望）：

```text
INVALID_SYMBOLS         symbols 为空 / 含 NQ 不支持的代码
INVALID_DATE_RANGE      startDate >= endDate
UNSUPPORTED_FREQUENCY   frequency 不在 NQ 支持集
INVALID_PARAMETERS_JSON 参数 JSON 解析失败
QUOTA_EXCEEDED          租户回测配额耗尽（NQ 业务规则）
RISK_GATED              NQ 风控拒绝（极端参数）
```

所有 error code 必须出现在双方文档；Stage3 IMPLEMENT 阶段才在代码中体现。

## 6. versioning 规则

```text
envelope.schemaVersion
  当前：    "1.0.0"
  兼容：    semver。MINOR 与 PATCH 向后兼容；MAJOR 升级必须双方同步发版
  破坏：    任何 MAJOR 升级 -> Stage3 IMPLEMENT 阶段才允许，且必须在 contracts/openapi.yaml
            + contracts/json-schema/* 双源同步

OpenAPI 版本
  当前：    contracts/openapi.yaml info.version: 0.1.0
  规则：    Stage3 IMPLEMENT 任一对外契约变更必须升 MINOR；PLAN 阶段不修改 info.version

DhBacktestRequest schema 演化
  规则：    向后兼容字段以 nullable: true 引入；强制字段升级必须配合 NQ 协议 MAJOR

deprecated 规则
  Stage1-CLOSE 已使用 OpenAPI deprecated: true；Stage3 保留同口径
```

## 7. 不修改 / 不语义变更项（本 PLAN 阶段保护）

```text
不修改 contracts/openapi.yaml 已落地端点的语义
不修改 contracts/json-schema/*.schema.json 已落地字段
不新增事件类型（保持 8 种）
不修改 Flyway migration 语义
不在 PLAN 阶段写任何 Java 业务代码
```

## 8. 与 Stage3 其他 PLAN 文档的衔接

```text
入站链路    -> docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
出站链路    -> docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
测试策略    -> docs/current/STAGE3_TEST_PLAN.md
IMPLEMENT 拆批 -> docs/current/STAGE3_WORK_ORDER.md
```
