# Stage2-PoC API Plan

> Status: PLAN
> Created: 2026-05-25

## 1. 现有端点（Stage1，保持不变）

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

## 2. Stage2 新增端点

### 2.1 Forecast Tool（预留）

```text
POST /api/ai/tools/forecast
```

Request:

```json
{
  "traceId": "uuid",
  "symbol": "string",
  "horizon": "1D | 5D | 20D | 60D",
  "features": ["price", "volume", "volatility"],
  "modelHint": "string (optional)"
}
```

Response (202 Accepted):

```json
{
  "artifactId": "uuid",
  "status": "COMPLETED | PENDING | FAILED",
  "artifact": { ... ForecastArtifact ... }
}
```

### 2.2 Research Snapshots（预留）

```text
POST /api/ai/research/snapshots
GET  /api/ai/research/snapshots/{snapshotId}
GET  /api/ai/research/snapshots?traceId={traceId}
```

POST Request:

```json
{
  "traceId": "uuid",
  "symbols": ["AAPL", "MSFT"],
  "dataTypes": ["OHLCV", "FUNDAMENTALS", "NEWS_SENTIMENT"],
  "dateRange": { "start": "2025-01-01", "end": "2026-05-25" }
}
```

POST Response (202 Accepted):

```json
{
  "snapshotId": "uuid",
  "status": "COMPLETED | PENDING | FAILED",
  "snapshot": { ... ExternalMarketSnapshot ... }
}
```

GET Response:

```json
{
  "snapshotId": "uuid",
  "traceId": "uuid",
  "symbols": ["string"],
  "fetchedAt": "ISO-8601",
  "data": { ... }
}
```

### 2.3 Reflection Checkpoints（查询）

```text
GET /api/ai/research-runs/{runId}/reflections
```

Response:

```json
{
  "runId": "uuid",
  "checkpoints": [
    {
      "stepIndex": 0,
      "agentRole": "ANALYST",
      "reflection": "string",
      "decision": "CONTINUE | PIVOT | ABORT",
      "timestamp": "ISO-8601"
    }
  ]
}
```

## 3. Feedback 端点增强

现有 `POST /api/ai/feedback/nq` 保持不变，但增强校验：

```text
新增：eventType 枚举校验（8 种已知类型）
新增：schemaVersion 兼容性校验
新增：traceId 关联校验（必须对应已知 ResearchRun）
新增：payload 按 eventType 做结构校验
返回：202 Accepted + { "eventId": "...", "status": "RECEIVED" }
错误：400 Bad Request + { "error": "INVALID_SCHEMA | UNKNOWN_TRACE | UNKNOWN_EVENT_TYPE" }
```

## 4. API 原则（延续 Stage1）

```text
所有输出结构化
所有请求带 traceId 或由服务生成 traceId
所有 Agent 输出保留 artifact 快照
所有 NQ 回流事件保留 raw payload
所有最终建议必须经过 JudgeDecision
```

## 5. 禁止 API（延续 Stage1）

```text
禁止直接下单 API
禁止自动实盘发布 API
禁止绕过 NQ 风控 API
禁止复制 NQ 回测核心 API
```

## 6. OpenAPI 更新计划

contracts/openapi.yaml 需新增：

```text
paths:
  /api/ai/tools/forecast          (POST)
  /api/ai/research/snapshots      (POST, GET)
  /api/ai/research/snapshots/{id} (GET)
  /api/ai/research-runs/{id}/reflections (GET)

components/schemas:
  ForecastRequest
  ForecastArtifact
  MarketSnapshotRequest
  ExternalMarketSnapshot
  ReflectionCheckpoint
  NqFeedbackEventEnvelope (正式 schema)
```
