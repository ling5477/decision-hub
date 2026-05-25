# Stage2-PoC Work Order

> Status: WO (可执行工单)
> Created: 2026-05-25
> Depends on: Stage1-FREEZE completed
> Next: Stage2-PoC IMPLEMENT

## 0. 范围与硬约束

### 0.1 目标

把 Stage1 的 Fake adapter 与 InMemory 仓储升级为：

1. NQ feedback event 正式契约（信封 + 8 类 payload + JSON Schema）。
2. DH -> NQ backtest request 契约（仍由 Fake 承载，但格式锁死）。
3. Kronos `ForecastToolPort` / `ForecastArtifact` 接口预留（不接真实推理服务）。
4. global-stock-data `ResearchDataAdapter` / `ExternalMarketSnapshot` / `ResearchSnapshotStore` 接口预留。
5. TradingAgents 风格 reflection / checkpoint / dynamic planner 轻量吸收。
6. dh-usecase 的 6 个 InMemory 仓储替换为 JDBC，并补 3 个 Stage2 新表仓储。
7. 全部走 ArchUnit 边界规则保护。

### 0.2 持续硬约束

```text
不修改 NQ 仓库
不接真实 NQ API（保留 Fake fallback）
不接真实 Kronos 推理服务
不接真实 global-stock-data 拉取
不引入 TradingAgents Python 代码
不实现真实下单
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不建设前端
不引入 BCO/ACO/GWO 重型数学优化器
不删除 Stage1 已落地的领域模型与用例服务
```

### 0.3 全局命名/包根

```text
domain root      com.guidinglight.decisionhub.domain
usecase root     com.guidinglight.decisionhub.usecase
connector root   com.guidinglight.decisionhub.connector
api root         com.guidinglight.decisionhub.api
infra root       com.guidinglight.decisionhub.infra
contracts root   contracts/
migration root   dh-app/src/main/resources/db/migration/
```

### 0.4 实施顺序

```text
Batch 1 -> Batch 2 -> Batch 3 -> Batch 4 -> Batch 5
每个 Batch 完成后必须执行：
  mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
不允许跳过 Batch 顺序，不允许跨 Batch 合并 PR。
```

---

## Batch 1 — Contract + Domain（契约与领域模型固化）

### 1.1 目标

- 固化 NQ feedback event envelope。
- 固化 8 种 NQ feedback event type。
- 定义 DH -> NQ backtest request 契约。
- 定义 `ForecastArtifact` / `ForecastRequest` / `ExternalMarketSnapshot` / `MarketSnapshotRequest` / `ReflectionEntry` / `CheckpointEntry` 领域模型草案。
- 扩展 `ResearchRun` 增加 `regime` / `topic` / `plannerStrategy`。

### 1.2 文件清单

JSON Schema（`contracts/schemas/`）：

```text
contracts/schemas/nq-feedback-event-envelope.v1.schema.json
contracts/schemas/nq-feedback/backtest-completed.v1.schema.json
contracts/schemas/nq-feedback/backtest-rejected.v1.schema.json
contracts/schemas/nq-feedback/risk-rejected.v1.schema.json
contracts/schemas/nq-feedback/paper-trial-completed.v1.schema.json
contracts/schemas/nq-feedback/release-approved.v1.schema.json
contracts/schemas/nq-feedback/release-rejected.v1.schema.json
contracts/schemas/nq-feedback/trade-review-completed.v1.schema.json
contracts/schemas/nq-feedback/post-mortem-created.v1.schema.json
contracts/schemas/dh-backtest-request.v1.schema.json
contracts/schemas/forecast-artifact.v1.schema.json
contracts/schemas/forecast-request.v1.schema.json
contracts/schemas/external-market-snapshot.v1.schema.json
contracts/schemas/market-snapshot-request.v1.schema.json
contracts/schemas/reflection-entry.v1.schema.json
contracts/schemas/checkpoint-entry.v1.schema.json
```

Java 领域模型（`dh-domain/src/main/java/...`）：

```text
domain/feedback/NqFeedbackEventEnvelope.java          (Stage2 正式信封值对象)
domain/feedback/NqFeedbackEventType.java              (enum, 8 值)
domain/feedback/payload/BacktestCompletedPayload.java
domain/feedback/payload/BacktestRejectedPayload.java
domain/feedback/payload/RiskRejectedPayload.java
domain/feedback/payload/PaperTrialCompletedPayload.java
domain/feedback/payload/ReleaseApprovedPayload.java
domain/feedback/payload/ReleaseRejectedPayload.java
domain/feedback/payload/TradeReviewCompletedPayload.java
domain/feedback/payload/PostMortemCreatedPayload.java
domain/feedback/payload/BacktestMetrics.java          (公共值对象)
domain/feedback/payload/PaperTrialMetrics.java
domain/feedback/payload/RiskViolation.java
domain/feedback/payload/BacktestVerdict.java          (enum PASS/FAIL/MARGINAL)
domain/feedback/payload/RejectionSource.java          (enum SYSTEM/RISK_ENGINE/TIMEOUT)
domain/feedback/payload/ViolationSeverity.java        (enum CRITICAL/HIGH/MEDIUM)
domain/feedback/payload/PostMortemSeverity.java       (enum CRITICAL/HIGH/MEDIUM/LOW)
domain/tool/ForecastArtifact.java
domain/tool/ForecastRequest.java
domain/tool/ForecastPoint.java
domain/tool/ForecastHorizon.java                      (enum D1/D5/D20/D60)
domain/tool/ForecastArtifactStatus.java               (enum COMPLETED/PENDING/FAILED/TIMEOUT)
domain/research/ExternalMarketSnapshot.java
domain/research/MarketSnapshotRequest.java
domain/research/MarketDataType.java                   (enum OHLCV/FUNDAMENTALS/NEWS_SENTIMENT)
domain/research/MarketSnapshotStatus.java             (enum COMPLETED/PENDING/FAILED)
domain/research/MarketRegime.java                     (enum BULL/BEAR/VOLATILE/NEUTRAL/UNKNOWN)
domain/agent/ReflectionEntry.java                     (单条 reflection 记录)
domain/agent/CheckpointEntry.java                     (run 级 checkpoint 快照)
domain/agent/ReflectionDecision.java                  (enum CONTINUE/PIVOT/ABORT)
domain/agent/PlannerStrategy.java                     (enum DEFAULT/BULL_FOCUSED/BEAR_FOCUSED/VOLATILE_DIVERSIFIED)
domain/research/ResearchRunPayloadFields.java         (final class 常量字段名)
domain/control/DhBacktestRequest.java                 (DH->NQ 请求值对象)
domain/control/DhBacktestStrategyDefinition.java
domain/control/DhBacktestConfig.java
domain/control/BacktestFrequency.java                 (enum DAILY/HOURLY/MINUTE)
```

修改既有文件：

```text
domain/research/ResearchRun.java   新增字段 regime / topic / plannerStrategy（向后兼容默认值）
domain/feedback/NqFeedbackEvent.java   保留 Stage1 形态，标注 Stage2 envelope 是上层信封 + 引用关系
```

### 1.3 类名清单（公开类，全部公开类必须带注释）

```text
NqFeedbackEventEnvelope            final 值对象
NqFeedbackEventType                enum
BacktestCompletedPayload           final 值对象 (record-style)
BacktestRejectedPayload            final 值对象
RiskRejectedPayload                final 值对象
PaperTrialCompletedPayload         final 值对象
ReleaseApprovedPayload             final 值对象
ReleaseRejectedPayload             final 值对象
TradeReviewCompletedPayload        final 值对象
PostMortemCreatedPayload           final 值对象
BacktestMetrics / PaperTrialMetrics 值对象
RiskViolation                      值对象
BacktestVerdict / RejectionSource / ViolationSeverity / PostMortemSeverity   enum
ForecastArtifact                   final 值对象
ForecastRequest                    final 值对象
ForecastPoint                      final 值对象
ForecastHorizon                    enum
ForecastArtifactStatus             enum
ExternalMarketSnapshot             final 值对象
MarketSnapshotRequest              final 值对象
MarketDataType                     enum
MarketSnapshotStatus               enum
MarketRegime                       enum
ReflectionEntry                    final 值对象
CheckpointEntry                    final 值对象
ReflectionDecision                 enum
PlannerStrategy                    enum
ResearchRunPayloadFields           final 常量类（不可实例化）
DhBacktestRequest                  final 值对象
DhBacktestStrategyDefinition       final 值对象
DhBacktestConfig                   final 值对象
BacktestFrequency                  enum
```

### 1.4 字段清单（最小要求；以 record-like 不可变值对象表达）

`NqFeedbackEventEnvelope`：

```text
String   eventId            UUID, NotNull, 幂等键
NqFeedbackEventType eventType
String   sourceSystem       常量 "nexus-quant"
String   sourceJobId        UUID, NotNull
String   traceId            UUID, NotNull, 关联 DH ResearchRun
Instant  occurredAt         ISO-8601
String   schemaVersion      semver, 默认 "1.0.0"
Object   payload            按 eventType 反序列化后的对应 Payload 值对象
String   rawPayloadJson     完整原始 JSON 留档
```

`BacktestCompletedPayload`：

```text
String  candidateId
String  strategyName
String  backtestId
BacktestMetrics metrics      (sharpeRatio, maxDrawdown, annualReturn, winRate, profitFactor)
LocalDate periodStart
LocalDate periodEnd
BacktestVerdict verdict
```

`BacktestRejectedPayload`：

```text
String candidateId
String reason
RejectionSource rejectedBy
```

`RiskRejectedPayload`：

```text
String candidateId
String riskCheckId
List<RiskViolation> violations    (rule, severity, message)
```

`PaperTrialCompletedPayload`：

```text
String candidateId
String trialId
int    durationDays
PaperTrialMetrics metrics         (realizedPnl, maxDrawdown, sharpeRatio)
BacktestVerdict verdict           (复用 PASS/FAIL/MARGINAL 子集)
```

`ReleaseApprovedPayload`：

```text
String candidateId
String reviewerId
Instant approvedAt
```

`ReleaseRejectedPayload`：

```text
String candidateId
String reviewerId
String reason
```

`TradeReviewCompletedPayload`：

```text
String candidateId
String reviewId
LocalDate periodStart
LocalDate periodEnd
String summary
double score    [0.0, 1.0]
```

`PostMortemCreatedPayload`：

```text
String  candidateId
String  postMortemId
String  rootCause
List<String> lessonsLearned
PostMortemSeverity severity
```

`ForecastArtifact`：

```text
String   artifactId        UUID
String   traceId
String   symbol
ForecastHorizon horizon
List<ForecastPoint> predictions   (date, value, confidence)
String   modelVersion
Instant  generatedAt
ForecastArtifactStatus status
String   rawPayloadJson    外部 adapter 原始返回
```

`ForecastRequest`：

```text
String traceId
String symbol
ForecastHorizon horizon
List<String> features        ["price","volume","volatility"]
String modelHint             可空
```

`ExternalMarketSnapshot`：

```text
String snapshotId
String traceId
List<String> symbols
Instant fetchedAt
String dataJson             多 symbol -> 多 dataType -> 数据
String sourceVersion
MarketSnapshotStatus status
String rawPayloadJson
```

`MarketSnapshotRequest`：

```text
String traceId
List<String> symbols
List<MarketDataType> dataTypes
LocalDate rangeStart
LocalDate rangeEnd
```

`ReflectionEntry`：

```text
String id
String runId
int    stepIndex
AgentRole agentRole
String reflection           text
ReflectionDecision decision
Instant createdAt
String payloadJson          可扩展
```

`CheckpointEntry`：

```text
String id
String runId
int    checkpointIndex
String snapshotJson         冗余整段 ResearchRun snapshot
Instant createdAt
```

`DhBacktestRequest`：

```text
String requestId
String traceId
String candidateId
DhBacktestStrategyDefinition strategyDefinition  (name, version, parametersJson, entryRulesRef, exitRulesRef)
DhBacktestConfig backtestConfig                  (startDate, endDate, initialCapital, symbols, frequency)
```

`ResearchRun` 新增字段（向后兼容）：

```text
MarketRegime    regime            默认 UNKNOWN
String          topic             可空
PlannerStrategy plannerStrategy   默认 DEFAULT
```

### 1.5 Enum 清单

```text
NqFeedbackEventType {
  BACKTEST_COMPLETED, BACKTEST_REJECTED, RISK_REJECTED,
  PAPER_TRIAL_COMPLETED, RELEASE_APPROVED, RELEASE_REJECTED,
  TRADE_REVIEW_COMPLETED, POST_MORTEM_CREATED
}
BacktestVerdict        { PASS, FAIL, MARGINAL }
RejectionSource        { SYSTEM, RISK_ENGINE, TIMEOUT }
ViolationSeverity      { CRITICAL, HIGH, MEDIUM }
PostMortemSeverity     { CRITICAL, HIGH, MEDIUM, LOW }
ForecastHorizon        { D1, D5, D20, D60 }   (JSON 序列化保留 "1D"/"5D"/"20D"/"60D")
ForecastArtifactStatus { COMPLETED, PENDING, FAILED, TIMEOUT }
MarketDataType         { OHLCV, FUNDAMENTALS, NEWS_SENTIMENT }
MarketSnapshotStatus   { COMPLETED, PENDING, FAILED }
MarketRegime           { BULL, BEAR, VOLATILE, NEUTRAL, UNKNOWN }
ReflectionDecision     { CONTINUE, PIVOT, ABORT }
PlannerStrategy        { DEFAULT, BULL_FOCUSED, BEAR_FOCUSED, VOLATILE_DIVERSIFIED }
BacktestFrequency      { DAILY, HOURLY, MINUTE }
```

### 1.6 JSON Schema 清单（draft-2020-12，必须 `$id` + `$schema` + `additionalProperties:false`）

```text
nq-feedback-event-envelope.v1.schema.json     (oneOf 引用 8 个 payload schema)
nq-feedback/backtest-completed.v1.schema.json
nq-feedback/backtest-rejected.v1.schema.json
nq-feedback/risk-rejected.v1.schema.json
nq-feedback/paper-trial-completed.v1.schema.json
nq-feedback/release-approved.v1.schema.json
nq-feedback/release-rejected.v1.schema.json
nq-feedback/trade-review-completed.v1.schema.json
nq-feedback/post-mortem-created.v1.schema.json
dh-backtest-request.v1.schema.json
forecast-artifact.v1.schema.json
forecast-request.v1.schema.json
external-market-snapshot.v1.schema.json
market-snapshot-request.v1.schema.json
reflection-entry.v1.schema.json
checkpoint-entry.v1.schema.json
```

Envelope schema 必备字段：

```text
required: eventId, eventType, sourceSystem, sourceJobId, traceId, occurredAt, schemaVersion, payload
oneOf:    根据 eventType 选 8 个 payload 之一
```

### 1.7 测试清单（Batch 1）

```text
dh-domain/src/test/.../feedback/NqFeedbackEventEnvelopeTest      构造/反序列化/getter
dh-domain/src/test/.../feedback/NqFeedbackEventTypeTest          枚举完整性 8 个
dh-domain/src/test/.../feedback/payload/PayloadShapesTest        8 个 payload 形状
dh-domain/src/test/.../tool/ForecastArtifactTest                 字段不可变 + builder
dh-domain/src/test/.../tool/ForecastHorizonJsonTest              "1D"/"5D"/"20D"/"60D" 序列化
dh-domain/src/test/.../research/ExternalMarketSnapshotTest       数据结构稳定性
dh-domain/src/test/.../research/MarketRegimeTest                 枚举值完整性
dh-domain/src/test/.../agent/ReflectionEntryTest                 字段不可变
dh-domain/src/test/.../agent/CheckpointEntryTest                 字段不可变
dh-domain/src/test/.../control/DhBacktestRequestTest             字段不可变
contracts/src/test/.../JsonSchemaSelfTest                        每个 schema 用 example 自检
```

### 1.8 不做事项（Batch 1）

```text
不写 Controller / Service / Repository
不实现 Fake adapter
不接 NQ HTTP
不接 Kronos
不接 global-stock-data
不写迁移脚本
不修改 dh-usecase / dh-connector / dh-infra / dh-api
不动 dh-app/AgentRuntimeWiringConfig
不动 ArchitectureTest（Batch 1 不引入新包，无需新规则）
```

---

## Batch 2 — NQ Feedback Ingestion（正式契约接入）

### 2.1 目标

- 把现有 `POST /api/ai/feedback/nq` 升级为正式契约入口。
- 校验 envelope + payload + traceId 关联。
- 幂等保证（按 `eventId`）。
- 持久化原始 payload。
- 不直接调用 NQ 实盘执行；DH 只接收事件。

### 2.2 文件清单

Controller / DTO（`dh-api/src/main/java/...`）：

```text
api/feedback/NqFeedbackController.java               (修改：使用新 envelope DTO)
api/feedback/NqFeedbackEnvelopeRequest.java          (新增, 替代旧 NqFeedbackRequest 字段)
api/feedback/NqFeedbackAcceptedResponse.java         (新增, 202 响应体)
api/feedback/NqFeedbackErrorResponse.java            (新增, 400 响应体)
api/feedback/NqFeedbackRequest.java                  (保留 @Deprecated，Stage1 兼容)
```

UseCase / 校验 / 路由（`dh-usecase/src/main/java/...`）：

```text
usecase/agent/NqFeedbackIngestionService.java                (接口)
usecase/agent/impl/DefaultNqFeedbackIngestionService.java    (实现, 编排校验+持久化+handler 派发)
usecase/agent/feedback/NqFeedbackContractValidator.java      (接口)
usecase/agent/feedback/impl/DefaultNqFeedbackContractValidator.java
usecase/agent/feedback/NqFeedbackEventTypeRouter.java        (接口, 按 eventType 路由 handler)
usecase/agent/feedback/impl/DefaultNqFeedbackEventTypeRouter.java
usecase/agent/feedback/handler/NqFeedbackEventHandler.java   (接口)
usecase/agent/feedback/handler/BacktestCompletedHandler.java
usecase/agent/feedback/handler/BacktestRejectedHandler.java
usecase/agent/feedback/handler/RiskRejectedHandler.java
usecase/agent/feedback/handler/PaperTrialCompletedHandler.java
usecase/agent/feedback/handler/ReleaseApprovedHandler.java
usecase/agent/feedback/handler/ReleaseRejectedHandler.java
usecase/agent/feedback/handler/TradeReviewCompletedHandler.java
usecase/agent/feedback/handler/PostMortemCreatedHandler.java
```

Repository 端口与实现：

```text
usecase/agent/NqFeedbackEventRepository.java                 (修改：新增 findByEventId / saveEnvelope)
usecase/agent/inmemory/InMemoryNqFeedbackEventRepository.java (修改：支持 eventId 索引)
```

### 2.3 DTO 清单

```text
NqFeedbackEnvelopeRequest    与 NqFeedbackEventEnvelope 字段一致，payload 收作 JsonNode
NqFeedbackAcceptedResponse   { eventId, status="RECEIVED" }
NqFeedbackErrorResponse      { error, errorCode, traceId? }
```

### 2.4 校验规则（按失败优先级）

```text
1. envelope schema 校验：
   - eventId 非空且为 UUID 格式
   - eventType 必须在 NqFeedbackEventType 枚举内（否则返回 400 UNKNOWN_EVENT_TYPE）
   - schemaVersion 非空且 semver；< 1.0.0 拒绝（返回 400 INVALID_SCHEMA）
   - sourceSystem == "nexus-quant"
   - traceId 非空且为 UUID 格式（否则返回 400 INVALID_SCHEMA）
   - occurredAt 非空，ISO-8601
   - payload 非空
2. traceId 关联校验：
   - 必须能在 ResearchRunRepository 命中（否则返回 400 UNKNOWN_TRACE）
3. payload 结构校验：
   - 按 eventType 反序列化到对应 Payload 类；JSON Schema 校验失败返回 400 INVALID_SCHEMA
4. 通过后：
   - 写入原始 envelope（含 rawPayloadJson）到 NqFeedbackEventRepository
   - 派发 handler，每个 handler 必须显式实现（不允许 default no-op）
```

### 2.5 幂等规则

```text
键：eventId
策略：先 findByEventId(eventId)
  - 命中：直接返回 202 + 原 status，不重复 handler
  - 未命中：先 INSERT，再调用 handler；handler 内部副作用需要自身幂等
存储：dh_nq_feedback_events.event_id UNIQUE
错误：唯一键冲突视为幂等命中，吞掉异常并返回 202
```

### 2.6 traceId / requestId / correlationId 规则

```text
traceId
  来源：envelope.traceId（必须由 DH 发起 backtest-request 时生成，并随 NQ 处理回流）
  用途：关联 ResearchRun，全链路串联
  存储：dh_nq_feedback_events.trace_id

requestId
  来源：HTTP 请求级 ID，由 TraceIdFilter 在 MDC 设置；客户端可传 X-Request-Id
  用途：单次 HTTP 调用排查
  存储：不入业务表，仅 MDC + 访问日志

correlationId
  来源：在 NqFeedbackController 中，若客户端未传则 fallback 到 traceId
  用途：跨服务关联键，对外暴露在响应头 X-Correlation-Id

sourceJobId
  来源：envelope.sourceJobId（NQ 侧 job ID）
  用途：和 NQ 系统对账
  存储：dh_nq_feedback_events.source_job_id
```

### 2.7 测试清单（Batch 2）

```text
dh-usecase/src/test/.../feedback/NqFeedbackContractValidationTest
  ①  8 种 eventType 各 1 条 -> 202 Accepted
  ②  未知 eventType -> 400 UNKNOWN_EVENT_TYPE
  ③  缺少 traceId -> 400 INVALID_SCHEMA
  ④  traceId 不存在 -> 400 UNKNOWN_TRACE
  ⑤  schemaVersion < 1.0.0 -> 400 INVALID_SCHEMA
  ⑥  payload 结构不匹配 eventType -> 400 INVALID_SCHEMA

dh-usecase/src/test/.../feedback/NqFeedbackIdempotencyTest
  ①  同一 eventId 重放 -> 仅入库一次、handler 仅调用一次
  ②  并发重放 -> 唯一键冲突时仍返回 202

dh-usecase/src/test/.../feedback/NqFeedbackHandlerDispatchTest
  8 个 handler 各自被路由到一次

dh-api/src/test/.../feedback/NqFeedbackControllerWebMvcTest
  HTTP 层 happy-path + 4 类错误响应体形状
```

### 2.8 不做事项（Batch 2）

```text
不调用真实 NQ HTTP
不实现 NqBacktestClient 真实化（仍保留 Fake）
不写 JDBC 持久化（保持 InMemory，Batch 5 再切 JDBC）
不实现 Kronos / global-stock-data
不修改 OpenAPI（OpenAPI 在 Batch 4 统一更新）
不引入 reflection / checkpoint 字段（Batch 4 才出现）
不修改 ArchitectureTest
```

---

## Batch 3 — Forecast / Research Adapter Interfaces（端口预留）

### 3.1 目标

- 在 dh-connector 预留 Kronos `ForecastToolPort` 与 Fake 实现。
- 在 dh-connector 预留 global-stock-data `ResearchDataAdapter` + `ResearchSnapshotStore` 与 Fake 实现。
- 不实现真实 HTTP / Python 调用。

### 3.2 Interface 文件清单

```text
dh-connector/src/main/java/.../connector/tools/ForecastToolPort.java
dh-connector/src/main/java/.../connector/research/ResearchDataAdapter.java
dh-connector/src/main/java/.../connector/research/ResearchSnapshotStore.java
```

接口签名：

```java
public interface ForecastToolPort {
    /** Stage2 同步占位；真实接入后再考虑异步包装。 */
    ForecastArtifact requestForecast(ForecastRequest request);
}

public interface ResearchDataAdapter {
    ExternalMarketSnapshot fetchSnapshot(MarketSnapshotRequest request);
}

public interface ResearchSnapshotStore {
    void save(ExternalMarketSnapshot snapshot);
    Optional<ExternalMarketSnapshot> findById(String snapshotId);
    Optional<ExternalMarketSnapshot> findByTraceId(String traceId);
    List<ExternalMarketSnapshot> findBySymbolAndDateRange(String symbol, LocalDate start, LocalDate end);
}
```

### 3.3 Fake Implementation 文件清单

```text
dh-connector/src/main/java/.../connector/tools/fake/FakeForecastToolAdapter.java
dh-connector/src/main/java/.../connector/research/fake/FakeResearchDataAdapter.java
dh-connector/src/main/java/.../connector/research/fake/InMemoryResearchSnapshotStore.java
```

行为：

```text
FakeForecastToolAdapter
  - 输入合法 -> 返回固定 mock ForecastArtifact，status=COMPLETED
  - symbol 为空 -> IllegalArgumentException
  - horizon 为空 -> IllegalArgumentException
  - rawPayloadJson 必须写入 mock JSON 字符串（不能为 null）

FakeResearchDataAdapter
  - 输入合法 -> 返回固定 mock ExternalMarketSnapshot
  - symbols 为空 / dateRange 不合法 -> IllegalArgumentException
  - 空数据请求 -> 返回 status=COMPLETED + 空 data
  - rawPayloadJson 必须写入 mock JSON

InMemoryResearchSnapshotStore
  - ConcurrentHashMap<String, ExternalMarketSnapshot> 按 snapshotId
  - 二级索引：traceId -> snapshotId set
```

### 3.4 Artifact / Snapshot 模型字段（Batch 1 已定）

复用 Batch 1 字段：

```text
ForecastArtifact.{artifactId, traceId, symbol, horizon, predictions, modelVersion, generatedAt, status, rawPayloadJson}
ExternalMarketSnapshot.{snapshotId, traceId, symbols, fetchedAt, dataJson, sourceVersion, status, rawPayloadJson}
```

### 3.5 Raw Payload 留档规则（强制）

```text
1. 任何 ForecastToolPort 实现返回的 ForecastArtifact，必须设置 rawPayloadJson。
2. 任何 ResearchDataAdapter 实现返回的 ExternalMarketSnapshot，必须设置 rawPayloadJson。
3. Fake 也必须填，不允许空字符串（最低要求 "{}"）。
4. 数据库列 raw_payload_json TEXT NOT NULL，由 Batch 5 落地。
5. 如果真实接入失败（超时/异常），fallback 时也要把异常摘要写入 rawPayloadJson。
6. 不允许在 rawPayloadJson 中放敏感凭据（密钥/Token），落库前必须由适配器内部脱敏。
```

### 3.6 Timeout / Cache / Retry 后置设计

```text
Stage2 不实现 timeout / cache / retry，仅以 javadoc + status 枚举做接口预留：
  ForecastArtifactStatus { COMPLETED, PENDING, FAILED, TIMEOUT }
  MarketSnapshotStatus   { COMPLETED, PENDING, FAILED }

后置接入计划（不在本 WO 内实现）：
  - ForecastToolPort 真实化时，由适配器层（dh-connector/tools/http/Kronos*HttpAdapter）
    通过 Resilience4j 实现 timeout/circuit-breaker/retry。
  - ResearchDataAdapter 真实化时，由适配器层加入磁盘缓存（snapshotId 命中复用）。
  - 接口签名不变，因此 Stage2 的 Fake 与未来真实实现可平滑替换。

约束：
  - 不允许在 Port 接口本身暴露超时参数，避免污染领域。
  - 不允许在领域对象上保存重试次数等运行时字段（保留给适配器内部）。
```

### 3.7 测试清单（Batch 3）

```text
dh-connector/src/test/.../tools/FakeForecastToolAdapterTest
  ①  happy path 返回 COMPLETED + 非空 rawPayloadJson
  ②  symbol 为空 -> IllegalArgumentException
  ③  horizon 为空 -> IllegalArgumentException

dh-connector/src/test/.../research/FakeResearchDataAdapterTest
  ①  happy path 返回 COMPLETED + 非空 rawPayloadJson
  ②  symbols 为空 -> IllegalArgumentException
  ③  dateRange start > end -> IllegalArgumentException
  ④  空数据请求 -> 空 data + COMPLETED

dh-connector/src/test/.../research/InMemoryResearchSnapshotStoreTest
  ①  save + findById / findByTraceId
  ②  findBySymbolAndDateRange 命中与未命中
```

### 3.8 不做事项（Batch 3）

```text
不实现真实 HTTP / gRPC / Python 客户端
不引入 Resilience4j / Hystrix / Caffeine 依赖
不修改 dh-domain（字段已在 Batch 1 落地）
不修改 dh-usecase（usecase 层在 Batch 4/5 接入）
不写 JDBC（Batch 5 才落地 JDBC ResearchSnapshotStore）
不引入 Python 进程 / sidecar
不引入新的连接器子包到 ArchUnit 之外（新规则在 Batch 5 加）
```

---

## Batch 4 — Reflection / Checkpoint / Dynamic Planner（轻量吸收 TradingAgents）

### 4.1 目标

- 轻量吸收 TradingAgents 中 reflection + checkpoint + topic/regime 选边的思想。
- `AgentTaskPlanner` 支持基于 `regime` / `topic` 的动态选边。
- 加入 `ReflectionEntry` / `CheckpointEntry` 写入与查询。
- `JudgeDecision` 仍是唯一最终出口（reflection 不允许直接输出最终建议）。

### 4.2 领域模型清单（Batch 1 已定，本 Batch 仅使用）

```text
ReflectionEntry
CheckpointEntry
ReflectionDecision   { CONTINUE, PIVOT, ABORT }
PlannerStrategy      { DEFAULT, BULL_FOCUSED, BEAR_FOCUSED, VOLATILE_DIVERSIFIED }
MarketRegime         { BULL, BEAR, VOLATILE, NEUTRAL, UNKNOWN }
ResearchRunPayloadFields  (常量：REFLECTIONS, CHECKPOINTS, REGIME, TOPIC, PLANNER_STRATEGY)
```

### 4.3 Planner 接口清单

```text
usecase/agent/AgentTaskPlanner.java                                 (修改：加 planByStrategy 默认方法)
usecase/agent/planner/PlannerStrategyResolver.java                  (新增接口)
usecase/agent/planner/impl/DefaultPlannerStrategyResolver.java      (新增)
usecase/agent/planner/strategy/PlannerStrategyHandler.java          (新增接口)
usecase/agent/planner/strategy/DefaultPlannerStrategyHandler.java   (Stage1 行为兼容)
usecase/agent/planner/strategy/BullFocusedPlannerStrategyHandler.java
usecase/agent/planner/strategy/BearFocusedPlannerStrategyHandler.java
usecase/agent/planner/strategy/VolatileDiversifiedPlannerStrategyHandler.java
usecase/agent/planner/PlannerStrategyRegistry.java                  (新增, 持有 4 个 handler)
usecase/agent/planner/impl/DefaultAgentTaskPlanner.java             (修改：委托 Registry)
usecase/agent/ReflectionCheckpointService.java                      (新增接口)
usecase/agent/impl/DefaultReflectionCheckpointService.java          (新增)
usecase/agent/ReflectionEntryRepository.java                        (新增接口)
usecase/agent/inmemory/InMemoryReflectionEntryRepository.java       (新增, Batch 5 切 JDBC)
usecase/agent/CheckpointEntryRepository.java                        (新增接口)
usecase/agent/inmemory/InMemoryCheckpointEntryRepository.java       (新增, Batch 5 切 JDBC)
```

### 4.4 默认 Planner 行为

```text
resolveStrategy(researchRun):
  if researchRun.plannerStrategy != DEFAULT:
      return researchRun.plannerStrategy
  switch researchRun.regime:
      BULL          -> BULL_FOCUSED
      BEAR          -> BEAR_FOCUSED
      VOLATILE      -> VOLATILE_DIVERSIFIED
      NEUTRAL / UNKNOWN / null -> DEFAULT
```

各 handler 行为：

```text
DefaultPlannerStrategyHandler             Stage1 既有任务链路（保持向后兼容）
BullFocusedPlannerStrategyHandler         偏进攻型：CANDIDATE_GENERATOR x2 + REVIEWER x1
BearFocusedPlannerStrategyHandler         偏防守型：RISK_REVIEWER x2 + CANDIDATE_GENERATOR x1
VolatileDiversifiedPlannerStrategyHandler 多样化：每种 AgentRole 各 1
```

`topic` 仅影响任务排序（同 strategy 内按 topic 关键词加权），不影响 agent 组合。

### 4.5 Reflection / Checkpoint 字段规范

`ReflectionEntry`：

```text
id            UUID
runId         关联 ResearchRun.id
stepIndex     >= 0；同 runId 内单调递增
agentRole     执行该 step 的 AgentRole
reflection    text（自由文本，建议 <= 4KB）
decision      ReflectionDecision { CONTINUE, PIVOT, ABORT }
createdAt     Instant
payloadJson   可选，扩展用，<= 16KB
```

`CheckpointEntry`：

```text
id                UUID
runId             关联 ResearchRun.id
checkpointIndex   >= 0；同 runId 内单调递增
snapshotJson      冗余 ResearchRun snapshot（含当前 task / candidate 摘要）
createdAt         Instant
```

写入规则：

```text
1. 每个 agent step 完成后，必须写入 1 条 ReflectionEntry。
2. CheckpointEntry 由 ReflectionCheckpointService 在 PIVOT / ABORT 决策或 candidate frozen 时写入。
3. ABORT decision 终止后续 step，但已生成的 candidate 仍允许 JudgeDecision 处理（不能丢失证据）。
4. JudgeDecision 仍是唯一最终出口；reflection 只是过程证据，不允许写入 final recommendation。
5. ResearchRun.payloadJson 内的 reflections[] / checkpoints[] 仅作为冗余镜像，写入失败不阻塞主链路。
```

### 4.6 测试清单（Batch 4）

```text
dh-usecase/src/test/.../planner/AgentTaskPlannerDynamicTest
  ①  regime=BULL -> handler=BULL_FOCUSED
  ②  regime=BEAR -> handler=BEAR_FOCUSED
  ③  regime=VOLATILE -> handler=VOLATILE_DIVERSIFIED
  ④  regime=NEUTRAL/UNKNOWN -> handler=DEFAULT (Stage1 行为)
  ⑤  plannerStrategy 显式覆盖 regime
  ⑥  topic 影响排序但不改 agent 组合

dh-usecase/src/test/.../planner/PlannerStrategyResolverTest
  4 个 strategy 解析

dh-usecase/src/test/.../reflection/ReflectionCheckpointServiceTest
  ①  每个 step 完成写 1 条 ReflectionEntry
  ②  ABORT 决策终止后续 step
  ③  按 runId 查询 reflections / checkpoints
  ④  写入失败不阻塞 JudgeDecision

dh-usecase/src/test/.../planner/JudgeAsSoleExitTest
  reflection 中即使有 ABORT，最终建议仍只来自 JudgeDecision
```

### 4.7 禁止引入的 TradingAgents 组件清单

```text
❌ tradingagents 的 Python 包（任何 .py 文件）
❌ tradingagents/agents 下的整套 prompt 模板原文
❌ tradingagents 内置的 LLM 客户端 / graph runtime / scheduler
❌ tradingagents 的多 agent 协调状态机（不复制；只借鉴）
❌ 任何重型群体智能优化器（BCO/ACO/GWO 等）
❌ tradingagents 的 reflection 主循环（DH 用单步 reflection，不引入循环驱动）
❌ tradingagents 任何与“直接下单”相关的代码路径
✅ 允许借鉴：reflection + checkpoint + topic/regime 动态选边的“思想”而非“代码”
```

### 4.8 不做事项（Batch 4）

```text
不写 JDBC 实现（Reflection/Checkpoint 仍走 InMemory，Batch 5 才切）
不接 NQ 真实事件（Batch 2 已覆盖）
不接 Forecast / Research 真实端口（Batch 3 已覆盖）
不引入 LLM 真实调用
不动 OpenAPI（Batch 5 一次性更新）
不动 ArchitectureTest（Batch 5 一次性加新规则）
```

---

## Batch 5 — JDBC + Tests + Docs（持久化 + 整体测试 + 文档收口）

### 5.1 目标

- 落地 V3 迁移脚本。
- 把 6 个 dh-usecase InMemory 仓储替换为 JDBC，并新增 3 个 Stage2 JDBC 仓储。
- 落地全部 Stage2 测试矩阵。
- ArchUnit 新增规则。
- 更新 docs/current + OpenAPI + WiringConfig。

### 5.2 V3 Migration 文件名与表结构

文件：`dh-app/src/main/resources/db/migration/V3__stage2_poc_tools.sql`

```sql
-- 新表 1: forecast artifacts
CREATE TABLE dh_forecast_artifacts (
    id                UUID         PRIMARY KEY,
    trace_id          UUID         NOT NULL,
    symbol            VARCHAR(32)  NOT NULL,
    horizon           VARCHAR(16)  NOT NULL,
    predictions_json  TEXT         NOT NULL,
    model_version     VARCHAR(64),
    status            VARCHAR(32)  NOT NULL DEFAULT 'COMPLETED',
    generated_at      TIMESTAMP    NOT NULL,
    raw_payload_json  TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_forecast_artifacts_trace  ON dh_forecast_artifacts(trace_id);
CREATE INDEX idx_forecast_artifacts_symbol ON dh_forecast_artifacts(symbol);

-- 新表 2: external market snapshots
CREATE TABLE dh_external_market_snapshots (
    id                UUID         PRIMARY KEY,
    trace_id          UUID         NOT NULL,
    symbols_json      TEXT         NOT NULL,
    data_types_json   TEXT         NOT NULL,
    range_start       DATE,
    range_end         DATE,
    fetched_at        TIMESTAMP    NOT NULL,
    data_json         TEXT         NOT NULL,
    source_version    VARCHAR(64),
    status            VARCHAR(32)  NOT NULL DEFAULT 'COMPLETED',
    raw_payload_json  TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_market_snapshots_trace ON dh_external_market_snapshots(trace_id);

-- 新表 3: reflection entries
CREATE TABLE dh_reflection_entries (
    id            UUID         PRIMARY KEY,
    run_id        UUID         NOT NULL,
    step_index    INT          NOT NULL,
    agent_role    VARCHAR(64)  NOT NULL,
    reflection    TEXT         NOT NULL,
    decision      VARCHAR(32)  NOT NULL,
    payload_json  TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reflection_entries_run ON dh_reflection_entries(run_id);
CREATE UNIQUE INDEX uk_reflection_entries_run_step ON dh_reflection_entries(run_id, step_index);

-- 新表 4: checkpoint entries
CREATE TABLE dh_checkpoint_entries (
    id                 UUID         PRIMARY KEY,
    run_id             UUID         NOT NULL,
    checkpoint_index   INT          NOT NULL,
    snapshot_json      TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_checkpoint_entries_run ON dh_checkpoint_entries(run_id);
CREATE UNIQUE INDEX uk_checkpoint_entries_run_idx ON dh_checkpoint_entries(run_id, checkpoint_index);

-- ALTER 1: ResearchRun 动态选边字段
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS regime           VARCHAR(32);
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS topic            VARCHAR(128);
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS planner_strategy VARCHAR(32) DEFAULT 'DEFAULT';

-- ALTER 2: NQ feedback 信封字段
ALTER TABLE dh_nq_feedback_events ADD COLUMN IF NOT EXISTS schema_version    VARCHAR(16) DEFAULT '1.0.0';
ALTER TABLE dh_nq_feedback_events ADD COLUMN IF NOT EXISTS validation_status VARCHAR(32) DEFAULT 'VALID';
ALTER TABLE dh_nq_feedback_events ADD COLUMN IF NOT EXISTS source_job_id     VARCHAR(64);
ALTER TABLE dh_nq_feedback_events ADD CONSTRAINT IF NOT EXISTS uk_nq_feedback_event_id UNIQUE (event_id);
```

> 注：`IF NOT EXISTS` 与 `ADD CONSTRAINT IF NOT EXISTS` 必须按 Postgres 实际语法处理；如果当前 Postgres 版本不支持 constraint IF NOT EXISTS，使用匿名 DO 块 + pg_constraint 判定后再 ADD。

### 5.3 需要新增 / 替换的 JDBC Repository 清单

替换（dh-usecase 现有 InMemory，新实现放 dh-infra）：

```text
InMemoryResearchRunRepository       -> dh-infra/impl/JdbcResearchRunRepository
InMemoryAgentTaskRepository         -> dh-infra/impl/JdbcAgentTaskRepository
InMemoryAgentArtifactRepository     -> dh-infra/impl/JdbcAgentArtifactRepository
InMemoryStrategyCandidateRepository -> dh-infra/impl/JdbcStrategyCandidateRepository
InMemoryJudgeDecisionRepository     -> dh-infra/impl/JdbcJudgeDecisionRepository
InMemoryNqFeedbackEventRepository   -> dh-infra/impl/JdbcNqFeedbackEventRepository
```

新增（Stage2 表）：

```text
dh-infra/impl/JdbcForecastArtifactRepository           (dh_forecast_artifacts)
dh-infra/impl/JdbcExternalMarketSnapshotRepository     (dh_external_market_snapshots)
dh-infra/impl/JdbcReflectionEntryRepository            (dh_reflection_entries)
dh-infra/impl/JdbcCheckpointEntryRepository            (dh_checkpoint_entries)
dh-infra/impl/JdbcResearchSnapshotStore                (满足 connector.ResearchSnapshotStore 端口)
```

保持 InMemory（dh-memory）：

```text
ExperienceStore / PheromoneStore / FailureCaseStore / MarketRegimeMemory / StrategyPatternMemory
  本轮不迁 JDBC；放到下一阶段（Stage3 候选）以控制 Stage2 工作量。
```

### 5.4 API 与 OpenAPI 更新（与 STAGE2_POC_API_PLAN 对齐）

新增 controller：

```text
dh-api/.../tools/ForecastToolController.java          POST /api/ai/tools/forecast
dh-api/.../research/ResearchSnapshotController.java   POST/GET /api/ai/research/snapshots(/{id})
dh-api/.../research/ReflectionController.java         GET /api/ai/research-runs/{runId}/reflections
```

OpenAPI：

```text
contracts/openapi.yaml 新增 paths：
  /api/ai/tools/forecast                       (POST)
  /api/ai/research/snapshots                   (POST, GET)
  /api/ai/research/snapshots/{snapshotId}      (GET)
  /api/ai/research-runs/{runId}/reflections    (GET)
contracts/openapi.yaml 新增 components/schemas:
  NqFeedbackEventEnvelope, NqFeedbackPayload(BACKTEST_COMPLETED 等 8 种)
  ForecastRequest, ForecastArtifact, ForecastPoint
  MarketSnapshotRequest, ExternalMarketSnapshot
  ReflectionEntry, CheckpointEntry
```

### 5.5 单元测试清单

```text
domain                  Batch 1 测试（已列）
contracts JsonSchemaSelfTest   每个 schema 用 example 校验
dh-connector            Fake 适配器 + InMemoryResearchSnapshotStore
dh-usecase
  - NqFeedbackContractValidationTest
  - NqFeedbackIdempotencyTest
  - NqFeedbackHandlerDispatchTest
  - AgentTaskPlannerDynamicTest
  - PlannerStrategyResolverTest
  - ReflectionCheckpointServiceTest
  - JudgeAsSoleExitTest
dh-api                  NqFeedbackControllerWebMvcTest
                        ForecastToolControllerWebMvcTest
                        ResearchSnapshotControllerWebMvcTest
                        ReflectionControllerWebMvcTest
dh-infra (jdbc, H2)
  - JdbcResearchRunRepositoryTest
  - JdbcAgentTaskRepositoryTest
  - JdbcAgentArtifactRepositoryTest
  - JdbcStrategyCandidateRepositoryTest
  - JdbcJudgeDecisionRepositoryTest
  - JdbcNqFeedbackEventRepositoryTest
  - JdbcForecastArtifactRepositoryTest
  - JdbcExternalMarketSnapshotRepositoryTest
  - JdbcReflectionEntryRepositoryTest
  - JdbcCheckpointEntryRepositoryTest
  - JdbcResearchSnapshotStoreTest
```

### 5.6 集成测试清单

```text
dh-usecase Stage2ClosedLoopTest          (完整 Stage2 闭环, 见 STAGE2_POC_TEST_PLAN §2.1)
dh-app     ApplicationContextLoadsTest   (Stage2 bean 装配)
dh-app     PostgresContainerSmokeTest    (Stage2 V3 迁移可执行；本地无 Docker 时 CI 跑)
```

### 5.7 ArchUnit 规则清单（保持 + 新增）

保持（Stage1-CLOSE 已落地的 5 条）：

```text
✅ ..domain.. 不依赖 ..infra..
✅ ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..
✅ ..connector.nq.. 字段/方法名禁字（DefaultNqContractVerifier 豁免）
✅ ..usecase.agent.. 不依赖 ..providers..
✅ ..api.. 控制器 @RequestMapping 不命中 /orders|/trades|/live
```

新增（Stage2 落到 ArchitectureTest 共 5 条）：

```text
⑥ ..connector.tools.. 不依赖 ..infra..
⑦ ..connector.research.. 不依赖 ..infra..
⑧ ..domain.tool.. 与 ..domain.research.. 不依赖 ..connector..
⑨ ..usecase.agent.planner.. 不依赖 ..providers..
⑩ ..usecase.agent.feedback.. 不依赖 ..providers..
```

### 5.8 docs/current 需要更新的文件清单

```text
docs/current/STATUS.md           Current stage -> Stage2-PoC IMPLEMENT completed（VERIFY 后再切）
docs/current/WORKLOG.md          追加 Stage2-PoC IMPLEMENT 记录
docs/current/TESTING.md          追加 Stage2 验收结果块
docs/current/ROADMAP.md          标注 Stage2-PoC completed
docs/current/WORK_ORDER.md       指向下一份 Stage3 WO 草案
docs/current/STAGE2_POC_PLAN.md  保持，不改
docs/current/STAGE2_POC_WORK_ORDER.md  保持，不改（本 WO）
docs/current/STAGE2_POC_API_PLAN.md / CONTRACT_PLAN / DB_PLAN / TEST_PLAN  保持
README.md                        Current stage 同步
AGENTS.md                        Current stage 同步
```

### 5.9 dh-app 装配

```text
dh-app/.../config/AgentRuntimeWiringConfig.java   修改：
  - 注入 PlannerStrategyRegistry + 4 个 handler
  - 注入 ReflectionCheckpointService
  - 注入 ForecastToolPort (Fake)
  - 注入 ResearchDataAdapter (Fake)
  - 注入 ResearchSnapshotStore (InMemory or JDBC)
  - 注入 NqFeedbackIngestionService + Validator + Router + 8 个 Handler
  - InMemory*Repository bean 替换为 Jdbc*Repository bean
```

### 5.10 验收命令

```bash
# 主验收
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

# 质量
mvn -Pquality validate

# 应用装配冒烟（可选）
mvn -pl dh-app -am spring-boot:run
```

成功标准：

```text
BUILD SUCCESS
Stage1 闭环测试保持通过
Stage2ClosedLoopTest 通过
NqFeedbackContractValidationTest 通过
ArchUnit 10 条规则全绿
contracts/openapi.yaml 与实现一致
V3 迁移脚本与 §5.2 一致
docs/current 三处状态一致
```

### 5.11 不做事项（Batch 5）

```text
不替换 dh-memory 的 5 个 Store 为 JDBC（留给 Stage3）
不接真实 NQ HTTP
不接真实 Kronos / global-stock-data
不引入 Resilience4j / Caffeine
不建设前端
不修改 Stage1 已落地的领域语义
不做数据迁移脚本（dh_xxx 是 Stage1-CLOSE 之后的新表，不存在历史数据）
```

---

## 9. 全局验收标准

```text
1. 5 个 Batch 全部完成。
2. 每个 Batch 完成后 mvn test 通过；最终一次合并验收必须再跑一次。
3. ArchUnit 10 条规则全绿。
4. contracts/ 下 schema 与 dh-domain 模型字段一致。
5. V3 迁移脚本可重复执行（含 IF NOT EXISTS）。
6. docs/current/STATUS.md、README.md、AGENTS.md 状态一致。
7. 不存在任何对 NQ 真实 API 的硬编码 URL。
```

---

## 10. Codex 开工提示词（Stage2-PoC IMPLEMENT）

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage2-PoC IMPLEMENT。

目标：按 docs/current/STAGE2_POC_WORK_ORDER.md 实现 Stage2-PoC。

开始前必须读取：
- README.md
- AGENTS.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/ROADMAP.md
- docs/current/WORKFLOW.md
- docs/current/WORK_ORDER.md
- docs/current/WORKLOG.md
- docs/current/TESTING.md
- docs/current/STAGE2_POC_PLAN.md
- docs/current/STAGE2_POC_WORK_ORDER.md
- docs/current/STAGE2_POC_API_PLAN.md
- docs/current/STAGE2_POC_CONTRACT_PLAN.md
- docs/current/STAGE2_POC_DB_PLAN.md
- docs/current/STAGE2_POC_TEST_PLAN.md
- docs/current/DH_NQ_INTEGRATION.md
- docs/gates/dh-stage1/README.md

严格边界：
- 不修改 NQ 仓库。
- 不接真实 NQ API（保留 Fake fallback）。
- 不接真实 Kronos 推理服务。
- 不接真实 global-stock-data 拉取。
- 不引入 TradingAgents Python 代码。
- 不实现真实下单。
- 不绕过 NQ 风控。
- 不重写 NQ 回测核心。
- 不建设前端。

实施顺序：Batch 1 -> Batch 2 -> Batch 3 -> Batch 4 -> Batch 5。
每个 Batch 完成后必须运行：
  mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

成功标准：
- BUILD SUCCESS
- Stage1 闭环测试保持通过
- Stage2ClosedLoopTest 通过
- NqFeedbackContractValidationTest 通过
- ArchUnit 10 条规则全绿
- contracts/openapi.yaml 与实现一致
- V3 迁移脚本存在且与 STAGE2_POC_WORK_ORDER.md §5.2 一致

完成后必须：
- 更新 docs/current/STATUS.md 为 "Current stage: Stage2-PoC IMPLEMENT completed / Next: Stage2-PoC VERIFY"
- 追加 docs/current/WORKLOG.md 实现记录
- 追加 docs/current/TESTING.md 验收结果
- 同步 README.md 与 AGENTS.md 当前阶段
```
