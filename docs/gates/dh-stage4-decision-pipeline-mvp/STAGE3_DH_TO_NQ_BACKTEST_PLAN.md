# Stage3 DH -> NQ Backtest Request Plan

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md`
> Scope: 规划 DH 主动向 NQ 提交正式回测请求的全链路。本文件只规划，不实现。

## 1. DH backtest request 生成规则

```text
触发源（DH 侧）：
  ResearchRun 中 JudgeDecision 决定某 StrategyCandidate 进入 "正式回测验证" 节点。
  仅 JudgeDecision 显式 BACKTEST_REQUESTED checkpoint 之后，才允许构造 DhBacktestRequest。

构造数据来源（dh-domain.backtest.DhBacktestRequest，contracts/json-schema/dh-backtest-request.schema.json 已落地）：
  requestId               DH 侧 UUIDv7（一次回测请求 = 一个 requestId）
  traceId                 沿用 ResearchRun.traceId
  candidateId             StrategyCandidate.id
  strategyName            StrategyCandidate.name
  strategyVersion         StrategyCandidate.version
  strategyParametersJson  完整参数快照（StrategyCandidate 已冻结）
  entryRulesRef           可选；引用 StrategyCandidate 的入场规则文本/资源 ID
  exitRulesRef            可选；引用出场规则
  startDate / endDate     研究问题确定的回测区间
  initialCapital          配置（exclusiveMinimum 0）
  symbols                 至少 1 个；不允许全市场扫描
  frequency               DAILY / HOURLY / MINUTE 三选一
  requestedBy             DH 侧调用者标识
  requestedAt             生成时间
  status                  初始为 DRAFT；进入 outbox 后切 QUEUED

幂等：DH 侧 requestId 唯一；同一 candidate + 同一参数组在 24h 窗口内重复触发应短路（细节归 Stage3 IMPLEMENT Batch 3）。
```

## 2. NQ 接收契约草案（待 NQ 团队确认）

```text
endpoint：   POST /api/ai/research/backtest-requests
认证：       由 NQ 侧规定（建议 mTLS / 服务账号 token，本文件不强制）
content-type: application/json
body：       DhBacktestRequest（schema: contracts/json-schema/dh-backtest-request.schema.json）

同步响应：
  202 Accepted + DhBacktestRequestAccepted
                  { requestId, jobId, status: QUEUED|ACCEPTED, acceptedAt }
                  - jobId 由 NQ 生成（即 NqFeedbackEnvelope.sourceJobId 之未来值）
                  - DH 必须把 requestId <-> jobId 持久化映射，便于异步 feedback 对账
  400 Bad Request 契约或字段非法，error code 参考 STAGE3_CONTRACT_PLAN.md §4
  409 Conflict    NQ 侧检测到同 requestId 重放（NQ 已接收过同 requestId）
                  -> DH 视为 idempotent 成功，记录但不再发送

不允许：
  - NQ 在同步响应里直接给回测结果
  - NQ 在同步响应里返回任何与下单 / 实盘有关的字段
```

## 3. NQ 回测执行边界

```text
NQ 自有：
  - 数据源、撮合模型、滑点 / 手续费、风控、复盘
  - 任务调度（队列 + 并发上限 + 限速）
  - 结果落库（NQ 自有 backtest_results / metrics 表）

NQ 不向 DH 暴露：
  - 撮合细节 / 风控规则 / 数据底层
  - 中间态 (RUNNING / PROGRESS%)
  - 任何与订单状态机 / 仓位 / 账本相关的事实
```

## 4. NQ result snapshot 返回规则

回测结果以 **异步 feedback event** 方式返回，不在同步响应里给：

```text
事件类型：BACKTEST_RESULT_READY
传输链路：NQ outbox -> POST /api/ai/feedback/nq -> DH ingest（详见 STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md §6）
envelope.sourceJobId：= NQ 返回的 jobId
payload schema：contracts/json-schema/nq-feedback-backtest-result-ready.schema.json
payload 关键字段：
  backtestId       (= jobId 或 NQ 内部更细 ID)
  requestId        必须等于 DH 发起的 DhBacktestRequest.requestId
  candidateId      必须等于 DH 发起的 candidateId
  sharpeRatio / maxDrawdown / annualReturn / winRate / profitFactor  指标，nullable
  periodStart / periodEnd
  verdict          PASS / FAIL / MARGINAL（NQ 给出，不是 DH 给出）
  readyAt
  rawPayloadJson   NQ 端原始结果 JSON 字符串，DH 原样保存

失败 / 拒绝：
  - NQ 侧若拒绝（参数非法 / 数据缺失）：仍走 feedback envelope，但 verdict = FAIL 或专门
    走 NqFeedbackEventType 中既定路径；不引入新 event type（与 STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md §2 一致）
```

## 5. DH 消费结果规则

```text
ingest 阶段：NqFeedbackIngestionService 命中 BACKTEST_RESULT_READY 后：
  1. 幂等去重（eventId）
  2. 反查 dh_research_runs by traceId；若失配 -> errorCode = UNKNOWN_TRACE
  3. 反查 candidate by candidateId；若失配 -> 仅记录，不沉淀
  4. 将 sharpeRatio / drawdown / verdict 等指标写入 ExperienceEntry
     - successKey = (strategyPattern, regime)
     - score 由 dh-eval BacktestResultScorer 计算（Stage1 已落地）
  5. 更新 PheromoneEdge：verdict=PASS 加权；FAIL 衰减
  6. 写 dh_checkpoint_entries(type=JUDGE_DECISION 之后续) 记录此次回测结果

不做：
  - 不在 ingest 路径自动发布策略 / 提交 paper 请求
  - 不把 NQ verdict 直接当作 JudgeDecision 终态：JudgeDecision 仍是 DH 侧 JudgeDecisionService 出口
  - 不写任何 NQ 数据
```

## 6. 不重写 NQ 回测核心的边界声明

```text
DH 仅生成请求体并提交，永不重写 NQ 回测核心；
DH 永不在自己的仓库里实现：撮合 / 数据加载 / 风控 / 滑点 / 手续费 / 复盘 / 报表生成；
DH 仅消费 NQ 在 feedback envelope 中给的指标，并把它转成经验记号；
DH 不存在 "如果 NQ 回测失败就 DH 自己跑一遍" 的兜底逻辑（Fake adapter 仅服务测试，参见
STAGE3_TEST_PLAN.md §1）。
```

## 7. 端到端流程图（PLAN）

```text
DH ResearchRun -> JudgeDecision (BACKTEST_REQUESTED checkpoint)
              -> dh-connector.nq.NqBacktestClient
              -> POST /api/ai/research/backtest-requests   [Stage3 Batch 3 实现]
              -> NQ accepts (202 + DhBacktestRequestAccepted)
              -> NQ 自行执行回测
              -> NQ outbox 发出 BACKTEST_RESULT_READY 事件
              -> POST /api/ai/feedback/nq                  [Stage2-PoC-B2 已落地]
              -> DH ingest -> ExperienceEntry / PheromoneEdge 更新
              -> ResearchRun 继续推进或终结
```

## 8. 与 Stage3 其他 PLAN 文档的衔接

```text
契约细节        -> docs/current/STAGE3_CONTRACT_PLAN.md
NQ -> DH 入站   -> docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
测试策略        -> docs/current/STAGE3_TEST_PLAN.md
IMPLEMENT 拆批  -> docs/current/STAGE3_WORK_ORDER.md
```
