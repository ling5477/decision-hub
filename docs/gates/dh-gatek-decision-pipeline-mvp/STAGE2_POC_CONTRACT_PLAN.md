# Stage2-PoC Contract Plan

> Status: PLAN
> Created: 2026-05-25

## 1. NQ -> DH 事件契约（Feedback Ingestion）

### 1.1 事件格式

所有 NQ -> DH 事件统一使用以下信封格式：

```json
{
  "eventId": "uuid",
  "eventType": "BACKTEST_COMPLETED | BACKTEST_REJECTED | RISK_REJECTED | PAPER_TRIAL_COMPLETED | RELEASE_APPROVED | RELEASE_REJECTED | TRADE_REVIEW_COMPLETED | POST_MORTEM_CREATED",
  "sourceSystem": "nexus-quant",
  "sourceJobId": "uuid (NQ 侧 job ID)",
  "traceId": "uuid (DH 侧 trace，由 DH 发起请求时传入)",
  "occurredAt": "ISO-8601",
  "schemaVersion": "1.0.0",
  "payload": { ... }
}
```

### 1.2 各事件 payload 定义

#### BACKTEST_COMPLETED

```json
{
  "candidateId": "uuid",
  "strategyName": "string",
  "backtestId": "uuid",
  "metrics": {
    "sharpeRatio": 1.23,
    "maxDrawdown": -0.15,
    "annualReturn": 0.28,
    "winRate": 0.62,
    "profitFactor": 1.85
  },
  "period": {
    "start": "2024-01-01",
    "end": "2025-12-31"
  },
  "verdict": "PASS | FAIL | MARGINAL"
}
```

#### BACKTEST_REJECTED

```json
{
  "candidateId": "uuid",
  "reason": "string",
  "rejectedBy": "SYSTEM | RISK_ENGINE | TIMEOUT"
}
```

#### RISK_REJECTED

```json
{
  "candidateId": "uuid",
  "riskCheckId": "uuid",
  "violations": [
    {
      "rule": "string",
      "severity": "CRITICAL | HIGH | MEDIUM",
      "message": "string"
    }
  ]
}
```

#### PAPER_TRIAL_COMPLETED

```json
{
  "candidateId": "uuid",
  "trialId": "uuid",
  "durationDays": 30,
  "metrics": {
    "realizedPnl": 12500.00,
    "maxDrawdown": -0.08,
    "sharpeRatio": 1.45
  },
  "verdict": "PASS | FAIL"
}
```

#### RELEASE_APPROVED / RELEASE_REJECTED

```json
{
  "candidateId": "uuid",
  "reviewerId": "string",
  "reason": "string (rejected only)",
  "approvedAt": "ISO-8601 (approved only)"
}
```

#### TRADE_REVIEW_COMPLETED

```json
{
  "candidateId": "uuid",
  "reviewId": "uuid",
  "period": { "start": "date", "end": "date" },
  "summary": "string",
  "score": 0.85
}
```

#### POST_MORTEM_CREATED

```json
{
  "candidateId": "uuid",
  "postMortemId": "uuid",
  "rootCause": "string",
  "lessonsLearned": ["string"],
  "severity": "CRITICAL | HIGH | MEDIUM | LOW"
}
```

### 1.3 DH 接收端点

```text
POST /api/ai/feedback/nq
Content-Type: application/json
Body: NqFeedbackEvent envelope (上述格式)
Response: 202 Accepted + { "eventId": "...", "status": "RECEIVED" }
```

校验规则：
- eventType 必须在已知枚举内
- schemaVersion 必须 >= 1.0.0
- traceId 必须对应已知 ResearchRun
- payload 按 eventType 做结构校验

### 1.4 DH 处理流程

```text
接收 -> schema 校验 -> 持久化 raw event -> 路由到对应 handler
  -> 更新 ExperienceEntry (success/failure count)
  -> 更新 PheromoneEdge (score adjustment)
  -> 如果是 failure: 写入 FailureCaseStore
  -> 如果是 POST_MORTEM: 写入 lessons learned
```

## 2. DH -> NQ 控制面契约

### 2.1 Backtest Request

```text
POST /api/ai/backtest-requests (NQ 侧端点，DH 调用)
```

```json
{
  "requestId": "uuid",
  "traceId": "uuid (DH trace)",
  "candidateId": "uuid",
  "strategyDefinition": {
    "name": "string",
    "version": "string",
    "parameters": { ... },
    "entryRules": "string (DSL or reference)",
    "exitRules": "string (DSL or reference)"
  },
  "backtestConfig": {
    "startDate": "2024-01-01",
    "endDate": "2025-12-31",
    "initialCapital": 1000000,
    "symbols": ["string"],
    "frequency": "DAILY | HOURLY | MINUTE"
  }
}
```

Response: `202 Accepted + { "jobId": "uuid", "status": "QUEUED" }`

### 2.2 Stage2 实现策略

```text
Stage2 不接真实 NQ API。
NqBacktestClient 接口保持不变，FakeNqBacktestClient 按上述契约格式返回 mock 数据。
真实接通留到 NQ 团队确认端点后的 Stage3 或后续迭代。
```

## 3. Kronos ForecastToolPort 契约

### 3.1 接口定义

```java
public interface ForecastToolPort {
    ForecastArtifact requestForecast(ForecastRequest request);
}
```

### 3.2 ForecastRequest

```json
{
  "traceId": "uuid",
  "symbol": "string",
  "horizon": "1D | 5D | 20D | 60D",
  "features": ["price", "volume", "volatility"],
  "modelHint": "kronos-base | kronos-fine-tuned (optional)"
}
```

### 3.3 ForecastArtifact

```json
{
  "artifactId": "uuid",
  "traceId": "uuid",
  "symbol": "string",
  "horizon": "string",
  "predictions": [
    { "date": "2026-06-01", "value": 150.5, "confidence": 0.72 }
  ],
  "modelVersion": "string",
  "generatedAt": "ISO-8601"
}
```

### 3.4 Stage2 实现策略

```text
FakeForecastToolAdapter 返回固定 mock 预测数据。
不接真实 Kronos Python 推理服务。
接口设计支持未来 HTTP/gRPC 适配。
```

## 4. global-stock-data ResearchDataAdapter 契约

### 4.1 接口定义

```java
public interface ResearchDataAdapter {
    ExternalMarketSnapshot fetchSnapshot(MarketSnapshotRequest request);
}
```

### 4.2 MarketSnapshotRequest

```json
{
  "traceId": "uuid",
  "symbols": ["AAPL", "MSFT"],
  "dataTypes": ["OHLCV", "FUNDAMENTALS", "NEWS_SENTIMENT"],
  "dateRange": { "start": "2025-01-01", "end": "2026-05-25" }
}
```

### 4.3 ExternalMarketSnapshot

```json
{
  "snapshotId": "uuid",
  "traceId": "uuid",
  "symbols": ["string"],
  "fetchedAt": "ISO-8601",
  "data": {
    "AAPL": {
      "ohlcv": [ ... ],
      "fundamentals": { ... },
      "newsSentiment": { "score": 0.6, "articles": 42 }
    }
  },
  "sourceVersion": "string"
}
```

### 4.4 ResearchSnapshotStore

```java
public interface ResearchSnapshotStore {
    void save(ExternalMarketSnapshot snapshot);
    Optional<ExternalMarketSnapshot> findByTraceId(String traceId);
    List<ExternalMarketSnapshot> findBySymbolAndDateRange(String symbol, LocalDate start, LocalDate end);
}
```

### 4.5 Stage2 实现策略

```text
FakeResearchDataAdapter 返回固定 mock 市场数据。
不接真实 global-stock-data 拉取服务。
接口设计支持未来 REST/文件系统适配。
```
