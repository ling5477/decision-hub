# Decision Hub API

## 1. 当前状态

当前为 API 规划文档，DH-REFIT-1-PLAN 阶段不实现业务 API。

## 2. Stage1 最小 API 草案

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

## 3. API 原则

```text
所有输出结构化
所有请求带 traceId 或由服务生成 traceId
所有 Agent 输出保留 artifact 快照
所有 NQ 回流事件保留 raw payload
所有最终建议必须经过 JudgeDecision
```

## 4. 禁止 API

```text
禁止直接下单 API
禁止自动实盘发布 API
禁止绕过 NQ 风控 API
禁止复制 NQ 回测核心 API
```

## 5. NQ 对接方向

DH -> NQ：

```text
BacktestRequest
PaperTrialRequest
ReleaseReviewRequest
```

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
