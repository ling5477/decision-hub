# Decision Hub API

## 1. 当前状态

```text
Current stage: Stage2-PoC-B5 IMPLEMENT completed
Next stage:    Stage2-PoC VERIFY
```

OpenAPI 单源：`contracts/openapi.yaml`。

## 2. 已实现端点

```text
GET  /_ping                           健康检查
POST /api/ai/feedback/nq              NQ -> DH 正式回流事件 ingest（Stage2-PoC-B2 落地）
                                      - eventId 幂等去重，重放返回 200 + duplicate=true
                                      - 信封 / payload 校验失败返回 400
POST /legacy/runs                     旧链路（@Deprecated，保留 6 周）
GET  /legacy/runs/{runId}             旧链路（@Deprecated）
```

## 3. Stage2-PoC-VERIFY 计划上线端点

OpenAPI components 已就位，路径以注释占位，待 Stage2-PoC VERIFY 阶段在装好 Docker 的 CI 环境上线：

```text
POST /api/ai/tools/forecast                       -> ForecastArtifact
POST /api/ai/research/snapshots                   -> ExternalMarketSnapshot
GET  /api/ai/research/snapshots/{snapshotId}      -> ExternalMarketSnapshot
GET  /api/ai/research-runs/{runId}/reflections    -> ReflectionEntry[]
GET  /api/ai/research-runs/{runId}/checkpoints    -> CheckpointEntry[]
```

## 4. Stage1 最小 API 草案

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
