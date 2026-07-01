# Decision Hub API

## 1. 当前状态

```text
当前阶段: DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE / IMPLEMENTED / READY FOR REVIEW
下一阶段: DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW / NOT STARTED
```

OpenAPI 单源：`contracts/openapi.yaml`。

当前状态锁定：

```text
Integration-0:        CLOSED / ACCEPTED
Header alignment:     CLOSED
Timestamp alignment:  CLOSED / ACCEPTED
Code reality audit:   DONE
GateK-PLAN:           ACCEPTED / CLOSED
GateK-WO:             ACCEPTED / CLOSED
K1 Contract Freeze:   IMPLEMENTED / READY FOR REVIEW
Decision pipeline API: NOT IMPLEMENTED
Integration-1:        NOT STARTED
Runtime integration:  NOT STARTED
AI / Agent runtime:   NOT STARTED
LIVE:                 DISABLED
```

OpenAPI 仍为 API 单源；GateK Decision Pipeline MVP PLAN / WO / K1 均不新增 API path、不新增 migration、不新增 RealClient / provider，不启动 Integration-1 runtime。`DecisionRequest` / `DecisionOutput` 已作为 K1 domain contract 与 JSON Schema 落地，但尚未成为已实现 API；`DecisionTrace` / audit / replay 仍未实现 API。

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
POST /legacy/runs                                    旧链路（@Deprecated，必须认证，不允许匿名）
GET  /legacy/runs/{runId}                            旧链路（@Deprecated，必须认证且 tenant 匹配）
```

## 3. Historical / deferred API 方向（当前 API 未实现）

以下方向是 historical / deferred / gated，不是当前 GateK 的已实现端点，也不得作为当前 implementation 入口：

```text
POST /api/ai/tools/forecast                       -> ForecastArtifact
POST /api/ai/research/snapshots                   -> ExternalMarketSnapshot
GET  /api/ai/research/snapshots/{snapshotId}      -> ExternalMarketSnapshot
GET  /api/ai/research-runs/{runId}/reflections    -> ReflectionEntry[]
GET  /api/ai/research-runs/{runId}/checkpoints    -> CheckpointEntry[]
```

GateK Decision Pipeline MVP 的 API 层仍未实现；只有后续已接受 WO 明确授权后，才允许新增 API / OpenAPI / migration / controller 相关变更。

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
