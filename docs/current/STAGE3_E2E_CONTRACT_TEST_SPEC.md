# Stage3 End-to-End Contract Test Spec

> Created: 2026-05-26 (Stage3-B4 End-to-End Contract Test PLAN)
> Stage: Stage3-B4 PLAN (本文件落盘即视为 B4 PLAN completed 草案，等待 STATUS 同步)
> Parent: `docs/current/STAGE3_PLAN.md`
> Sibling: `docs/current/STAGE3_NQ_OUTBOX_SPEC.md`
>          `docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md`
>          `docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md`
>          `docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md`
>          `docs/current/STAGE3_CONTRACT_PLAN.md`
>          `docs/current/STAGE3_TEST_PLAN.md`
>          `docs/current/STAGE3_WORK_ORDER.md`
> Scope: 本文件是 Stage3 DH/NQ 端到端契约测试的最小可实施规格。
>        本轮（Stage3-B4 PLAN）仅产出规划文档；零 Java 业务代码改动；零 NQ 仓库改动；
>        零真实联调；零实盘；零自动下单；零自动发布；零 OpenAPI path 新增；
>        零 Flyway migration 新增；零 contracts/openapi.yaml / contracts/json-schema 语义修改。

---

## 一、目标与边界

### 1.1 目标

```text
本规格规划 DH/NQ 未来端到端契约测试，验证目标限定为"契约面"：
  - JSON Schema 契约一致
  - HTTP 响应矩阵契约一致
  - error code 矩阵契约一致
  - 幂等键契约一致（eventId / requestId）
  - 5 个追踪字段（traceId / requestId / correlationId / sourceJobId / eventId）端到端对齐
  - retry / dead-letter / disabled / 降级行为符合 STAGE3_NQ_OUTBOX_SPEC + STAGE3_DH_BACKTEST_ADAPTER_SPEC
  - 失败隔离（DH 不可用 → NQ 主链路不阻塞；NQ 不可用 → DH ResearchRun 不阻塞）
  - 边界关键词扫描（无下单 / 绕风控 / 实盘路径关键词）
不验证：
  - 实盘收益
  - 实盘交易事实
  - NQ 内部回测核心逻辑（NQ 自有；DH 不重写）
  - LLM 真实推理结果（DH 端默认 Fake / Mock）
```

### 1.2 硬边界

```text
NQ test cluster 必须与 NQ 生产物理隔离（独立 DB / 独立配置 / 独立行情源）
DH staging 必须与 DH 生产物理隔离（独立 DB / 独立配置 / 独立监控）
所有测试使用 fake / test profile / paper / backtest 环境
不接实盘账户                       不自动下单
不自动发布策略                     不绕过 NQ 风控
不修改 NQ 仓库                     不修改 Java 业务代码（本 PLAN 阶段）
不接真实 Kronos                    不接真实 global-stock-data
不引入 TradingAgents Python        不重写 NQ 回测核心
不建设前端                         不修改 contracts / migration / OpenAPI 语义
本 PLAN 不实施任何联调；联调动作在 B4-1 ~ B4-5 IMPLEMENT 阶段，依赖 NQ-1 ~ NQ-5 已完工
```

### 1.3 触发联调的前置条件

```text
在 NQ test cluster + DH staging 真正跑联调 T1-T7 之前，必须满足：
  1. NQ 仓库内 STAGE3_NQ_OUTBOX_SPEC §8 Batch NQ-1 ~ NQ-4 已完工
     - Batch NQ-1 Contract + DB migration
     - Batch NQ-2 Outbox repository + FakeDispatcher
     - Batch NQ-3 8 事件源写入 outbox
     - Batch NQ-4 RealOutboxDispatcher + retry + dead-letter + audit
  2. DH 仓库内 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12 Batch B3-1 ~ B3-5 已完工
     - B3-1 Contract + Service Interface
     - B3-2 Fake / Disabled Client
     - B3-3 Real Client Skeleton（默认 disabled）
     - B3-4 Result Snapshot Consumption
     - B3-5 Tests + Docs
  3. 双方均能在 staging / test cluster 跑通"默认 profile mvn test"
  4. 双方 oncall 评审通过本 SPEC 与 Stage3-B4 IMPLEMENT 工单
  5. 联调期间 prod 环境严格保持 enabled=false（任何 prod 配置变更需 ChangeRequest）

任一前置失败 → 不允许触发联调；回到对应 Batch PLAN，禁止跳跃。
```

---

## 二、测试环境规划

### 2.1 DH staging

```text
profile             stage3-test 或 staging
机房 / 网络         独立 namespace / 独立 VPC；不与 prod 共享 DB / 缓存 / 监控
配置文件            src/main/resources/application-stage3-test.yml（B4-1 落地）

默认 client 装配    FakeNqBacktestClient（兜底）
                    或 DisabledNqBacktestClient（决定关闭 backtest-request 子能力时）
Real client 装配    仅在 B4-1 实施且 NQ test cluster 已就绪时显式开启：
                    decisionhub.stage3.nq.enabled=true
                    decisionhub.stage3.nq.backtest-request.enabled=true
                    decisionhub.stage3.nq.backtest-request.fake-mode=false
                    decisionhub.stage3.nq.backtest-request.base-url=https://<nq-test-cluster>/...

DB                  独立 PostgreSQL 实例（容器或 staging 集群）
                    Flyway V1 / V2 / V3 已落地；Stage3 不新增 migration
                    decisionhub.stage2.jdbc.enabled=true（启用 JDBC 仓储）

监控                独立 Prometheus / Grafana namespace
                    日志独立 ELK index "dh-stage3-test-*"
                    告警通道隔离（不发送到 prod oncall）

数据隔离            tenantId 前缀必须为 "t-test-*"
                    traceId 前缀必须为 "stage3-"
                    correlationId 前缀必须为 "stage3-"
                    symbols 限定测试白名单（不允许 live universe；不允许真实标的）

实盘交易配置        禁止配置任何真实 OKX / Binance / 其他实盘适配器
                    禁止配置真实 API key / secret
                    禁止启用 dh-eval 中任何"自动发布"路径
```

### 2.2 NQ test cluster

```text
profile             local / test / stage3-test（按 NQ 既有 profile 体系决定）
机房 / 网络         NQ 独立 test cluster，与 NQ prod 物理隔离

AI 总开关           nq.ai.enabled=false（默认）
                    联调时显式切 true，否则 backtest-request 与 feedback outbox 都默认关闭
feedback outbox     nq.ai.feedback.enabled=false（默认）
                    联调时显式切 true；目标 endpoint = DH staging /api/ai/feedback/nq
backtest ingress    nq.ai.backtest-request.enabled=false（默认）
                    联调时显式切 true；限速：nq.ai.backtest-request.rate-limit-per-tenant-per-hour=100

paper / backtest    可用（NQ 既有 paper / backtest 引擎）
                    paper run 仅写 NQ test DB；不影响 prod
                    backtest 仅写 NQ test DB；不影响 prod
live trading        必须关闭
                    NQ test cluster 严禁配置任何实盘账户
                    NQ test cluster 严禁连接 OKX / Binance / 其他真实交易所
                    联调期间 oncall 持续监控 live trading 配置项为 false

DB                  独立 PostgreSQL 实例（NQ 自有）
                    nq_ai_feedback_outbox / nq_ai_feedback_dead_letter 已建表（NQ-1 落地）

数据隔离            paper run tenantId 前缀 "t-test-*"
                    backtest job tenantId 前缀 "t-test-*"
                    任何"prod tenantId"渗入 → 立即拒绝并告警

测试数据            测试 universe（symbols）由 NQ 团队定义，不允许真实热门标的
                    backtest 起止日期严禁覆盖 prod 最近 30 天热点（避免数据泄露与冲突）
```

### 2.3 网络与配置

```text
DH staging 环境变量（通过 ConfigMap / Vault 注入；不写入仓库）：
  DH_BASE_URL                       https://dh-stage3-test.example.com
  DH_NQ_BACKTEST_BASE_URL           https://nq-test-cluster.example.com
  DH_NQ_BACKTEST_ENDPOINT_PATH      /api/ai/backtest-requests
  DH_NQ_BACKTEST_TIMEOUT_MS         30000
  DH_NQ_BACKTEST_CONNECT_TIMEOUT_MS 10000
  DH_NQ_BACKTEST_TOKEN              <从 Vault 注入；不入仓库；不入日志>
  DH_NQ_AUTH_MTLS_ENABLED           false（联调首期；mTLS 后续启用）

NQ test cluster 环境变量：
  NQ_AI_FEEDBACK_ENDPOINT           https://dh-stage3-test.example.com/api/ai/feedback/nq
  NQ_AI_FEEDBACK_TOKEN              <从 Vault 注入>
  NQ_AI_BACKTEST_RATE_LIMIT         100  (per-tenant-per-hour)

通用约束：
  timeout                           DH → NQ: connect=10s, read=30s
                                    NQ → DH: connect=10s, read=30s
                                    所有 timeout 不允许设为 0 或 > 60s
  retry                             DH → NQ: max-retries=0（B4-1 起点；视情况提至 3）
                                    NQ → DH: 1s/5s/30s/5min/30min/1h/6h，attempt 上限 8
                                    遵循 STAGE3_NQ_OUTBOX_SPEC §5.3 / STAGE3_DH_BACKTEST_ADAPTER_SPEC §8.2
  auth token                        通过 Vault 注入；24h 自动轮换；不允许写入仓库 / 配置文件 / 日志
  disabled mode                     DH backtest-request.enabled=false 时立即生效（无需重启）
                                    NQ nq.ai.*.enabled=false 时立即生效
  fake mode                         DH backtest-request.fake-mode=true 时即便 enabled=true 仍走 Fake
                                    NQ 端联调期间 RealOutboxDispatcher 装配；fake-mode 仅 DH 端有意义

网络白名单（联调期间）：
  DH staging 出站                   仅允许 DH_NQ_BACKTEST_BASE_URL；其它 NQ 域名拒绝
  NQ test cluster 出站              仅允许 NQ_AI_FEEDBACK_ENDPOINT；其它 DH 域名拒绝
  双方均不允许出站到 prod 域名
```

### 2.4 环境隔离强约束

```text
强制：
  - prod 环境配置始终保持 enabled=false（DH 与 NQ 双方）
  - staging / test 配置不允许包含任何 prod URL / token
  - 联调专用 token 24h 自动轮换；轮换失败立即降级到 enabled=false
  - 任何"prod 数据进入测试环境"立即触发 oncall + 数据销毁流程
  - 任何"测试数据流出到 prod"立即触发 oncall + 回滚

回滚预案（参考 STAGE3_WORK_ORDER §4.7）：
  - 联调失败 1 小时内回滚 DH staging 到 stage2 InMemory 模式（decisionhub.stage3.nq.enabled=false）
  - NQ test cluster 同步切 nq.ai.enabled=false
  - 失败原因 oncall 评审后才允许重试；不允许"硬抹平"或"跳过 Batch"
```

---

## 三、7 个端到端契约测试用例

每个用例在 B4-1 / B4-2 IMPLEMENT 阶段以 `@EnabledIfEnvironmentVariable(named="ENABLED_STAGE3", matches="true")` 隔离，
默认 profile 不跑（不影响日常 mvn test）。联调期间显式：
`ENABLED_STAGE3=true mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'`。

### 3.1 T1：NQ → DH PAPER_RUN_CREATED feedback

```text
目标
  验证 NQ outbox → DH ingest 单事件正向链路；
  验证 5 字段端到端对齐；
  验证 dh_nq_feedback_events 落库；
  验证 DH 主链路（ResearchRun / Stage1ClosedLoopTest 行为）不受影响。

前置
  - NQ test cluster nq.ai.feedback.enabled=true
  - DH staging /api/ai/feedback/nq 可达
  - 存在一个有效的 ResearchRun（traceId 前缀 stage3-，tenantId 前缀 t-test-*）

步骤
  1. DH 侧准备：插入测试 ResearchRun(traceId=stage3-t1-trace, tenantId=t-test-001)
  2. NQ 侧准备：触发 NQ test paper engine 创建 paper run（事件源模块写 nq_ai_feedback_outbox 行）
                envelope.eventId = UUIDv7（NQ 自动生成）
                envelope.eventType = PAPER_RUN_CREATED
                envelope.traceId = stage3-t1-trace
                envelope.requestId = stage3-t1-req
                envelope.correlationId = stage3-t1-corr
                envelope.sourceJobId = nq-paper-001
                envelope.sourceSystem = "nexus-quant"
                envelope.schemaVersion = "1.0.0"
                envelope.occurredAt = now()
                envelope.payloadJson = PaperRunCreatedPayload JSON
  3. NQ outbox dispatcher 拉行 → HTTP POST → DH /api/ai/feedback/nq
  4. DH 返回 202 + NqFeedbackAcceptedResponse{outcome=ACCEPTED}
  5. NQ outbox 行 status 切 SENT

期望结果
  - DH HTTP 202 + body.outcome=ACCEPTED + body.eventId == envelope.eventId
  - DH dh_nq_feedback_events 新增 1 行（event_id 唯一索引）
  - DH dh_nq_feedback_events.trace_id 命中 dh_research_runs.trace_id
  - NQ outbox sent_at 已填充
  - 5 字段对账（traceId / requestId / correlationId / sourceJobId / eventId）
    在 DH 日志 / NQ 日志 / DH 数据库三处完全一致
  - Stage1ClosedLoopTest 在 DH staging 仍能跑通（独立用例验证主链路未阻塞）

失败回退
  - 任何字段失配 → 用例失败；oncall 排查；回 Batch B4-1 起点
```

### 3.2 T2：NQ → DH PAPER_RUN_ALERT_RAISED feedback（含幂等）

```text
目标
  验证 alert 事件契约；
  验证 eventId 幂等（DH dh_nq_feedback_events.event_id 唯一索引保障）；
  验证 NQ outbox 重放后 DH 返回 DUPLICATE；
  验证 NQ 主链路（paper engine / alert engine）不受 DH 重放影响。

前置
  - 同 T1
  - 存在 alert 事件源（NQ test paper run 触发 RISK / SYSTEM 类 alert）

步骤
  1. NQ outbox 第一次发送：envelope.eventType=PAPER_RUN_ALERT_RAISED
                            envelope.eventId = stage3-t2-evt-01
                            payload.alertCode=TEST_ALERT
                            payload.alertLevel=ERROR
                            payload 不携带订单 ID / 账户余额（仅 alertCode / alertLevel /
                            message / paperRunId / 时间戳）
  2. DH 返回 202 outcome=ACCEPTED
  3. NQ outbox dispatcher 模拟"ack 丢失"场景（B4-3 mock；联调由 NQ 端注入测试钩子）
     → outbox 行回 PENDING → 再次发送同 envelope（同 eventId）
  4. DH 第二次收到 → 202 outcome=DUPLICATE
  5. NQ outbox 行最终 status=SENT

期望结果
  - 两次 DH 响应均为 202
  - 第一次 outcome=ACCEPTED；第二次 outcome=DUPLICATE
  - DH dh_nq_feedback_events 仅新增 1 行（第二次幂等命中，不新增）
  - 经验沉淀路径（如启用 B3-4）仅触发 1 次（重放不重新走经验沉淀）
  - NQ paper engine / alert engine 不受任何影响（独立断言：NQ alert_count 表无重复）
  - 5 字段对账一致

失败回退
  - 第二次返回非 202 → 用例失败
  - DH dh_nq_feedback_events 新增 2 行 → 唯一索引失效 → 严重；立即停止联调
```

### 3.3 T3：NQ → DH BACKTEST_RESULT_READY feedback（结果消费）

```text
目标
  验证 BACKTEST_RESULT_READY 事件链路；
  验证 DH 消费 result snapshot 并写 DhBacktestResultSnapshot；
  验证 DH 更新 StrategyCandidateScore / ExperienceEntry / PheromoneEdge；
  验证 DH 不覆盖 NQ 正式回测记录（不反向同步）。

前置
  - 同 T1
  - 存在 DhBacktestRequest 历史记录（DH 出站已发；requestId=stage3-t3-req）
  - StrategyCandidate(candidateId=stage3-t3-cand, frozen=true) 在 DH 端存在

步骤
  1. NQ 端 backtest 引擎完成回测，落 backtest_results 表
                payload.backtestId = nq-bt-001
                payload.requestId = stage3-t3-req
                payload.candidateId = stage3-t3-cand
                payload.sharpeRatio = 1.8
                payload.maxDrawdown = -0.12
                payload.annualReturn = 0.18
                payload.winRate = 0.62
                payload.profitFactor = 1.6
                payload.periodStart = 2026-01-01
                payload.periodEnd = 2026-04-30
                payload.verdict = PASS
                payload.readyAt = now()
                payload.rawPayloadJson = <NQ 原始 JSON 字符串>
  2. NQ outbox 发送 BACKTEST_RESULT_READY envelope
                envelope.sourceJobId = nq-bt-001
                envelope.requestId = stage3-t3-req
                envelope.traceId = stage3-t3-trace
                envelope.correlationId = stage3-t3-corr
                envelope.eventId = stage3-t3-evt-01
  3. DH 接收 → 校验链路：
     - schema 校验通过
     - sourceSystem=nexus-quant 通过
     - eventType in 8 枚举通过
     - schemaVersion 1.0.0 通过
     - eventId 幂等首次 → 通过
     - traceId 命中 dh_research_runs → 通过
     - requestId 命中 DhBacktestRequestRepository → 通过
     - sourceJobId 比对通过
  4. DH 落 DhBacktestResultSnapshot（contracts/json-schema/dh-backtest-result-snapshot.schema.json）
  5. DH 状态机 SUBMITTED → ACCEPTED → RESULT_READY
  6. DH 触发经验沉淀（B3-4 落地）：
     - ExperienceEntry 写入（successKey=(strategyPattern, regime)；score 由 dh-eval BacktestResultScorer 计算）
     - PheromoneEdge 加权（verdict=PASS）
     - dh_checkpoint_entries 写入 type=BACKTEST_RESULT_RECEIVED

期望结果
  - DH HTTP 202 outcome=ACCEPTED
  - DhBacktestResultSnapshot 新增 1 行；verdict=PASS；其它指标完整
  - StrategyCandidateScore / ExperienceEntry / PheromoneEdge 已更新
  - DH 端不反向修改 NQ 数据（独立断言：NQ backtest_results 表无新增 / 无修改）
  - JudgeDecision 仍是唯一最终出口（DH 端不基于 NQ verdict 自动产生 JudgeDecision；
    必须经 DH JudgeDecisionService 决策）
  - 5 字段对账一致

失败回退
  - candidateId 失配 → DH 仅记录 WARN，不沉淀 → 视为预期行为（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §9.3）
  - verdict 非法 → DH 返回 400 INVALID_SCHEMA → NQ outbox 转 DEAD_LETTER
  - 任何 DH 反向修改 NQ 数据 → 严重违规；立即停止联调
```

### 3.4 T4：DH → NQ backtest request accepted

```text
目标
  验证 DH 出站 backtest request 主路径；
  验证 NQ 接收并创建异步回测任务（不直接实盘交易）；
  验证 requestId 幂等；
  验证 DH 状态机 SUBMITTED → ACCEPTED 切换；
  验证 jobId 持久化（DhBacktestRequestRepository）。

前置
  - NQ test cluster nq.ai.backtest-request.enabled=true
  - DH staging decisionhub.stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=false
  - DH 已装配 RealNqBacktestClient（B3-3 落地）
  - StrategyCandidate(candidateId=stage3-t4-cand, frozen=true) 存在
  - symbols 在测试白名单（如 ["TEST-SYM-A", "TEST-SYM-B"]）

步骤
  1. DH service 构造 DhBacktestRequestCommand（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §5.2）
                candidateId=stage3-t4-cand
                traceId=stage3-t4-trace
                correlationId=stage3-t4-corr
                strategyName="test-strategy-momentum"
                strategyVersion="v1.0"
                strategyParametersJson="{...frozen JSON...}"
                startDate=2026-01-01
                endDate=2026-04-30
                initialCapital=100000.00
                symbols=["TEST-SYM-A"]
                frequency=DAILY
                requestedBy="stage3-test-runner"
  2. DH service.submit() → 内部 IdGenerator 生成 requestId=stage3-t4-req-01
  3. DH RealNqBacktestClient.submit() → HTTP POST /api/ai/backtest-requests
                headers: X-Trace-Id=stage3-t4-trace
                         X-Correlation-Id=stage3-t4-corr
                         X-Request-Id=stage3-t4-req-01
                         Idempotency-Key=stage3-t4-req-01
                body: DhBacktestRequest（按 wire schema 序列化）
  4. NQ 校验：schema OK / symbols 在白名单 / dates OK / capital > 0 / frequency=DAILY
  5. NQ 写 backtest queue（异步）→ 返回 202 + DhBacktestRequestAccepted{
                requestId=stage3-t4-req-01,
                jobId=nq-bt-002,
                status=QUEUED,
                acceptedAt=now()
     }
  6. DH 持久化 DhBacktestRequestRepository(requestId=stage3-t4-req-01,
                                            jobId=nq-bt-002,
                                            ACCEPTED, acceptedAt)
  7. DH 状态机切 SUBMITTED → ACCEPTED

期望结果
  - DH 收到 HTTP 202
  - DhBacktestRequestRepository 新增 1 行；status=ACCEPTED；jobId=nq-bt-002
  - NQ 端 backtest queue 入队 1 个（不直接执行实盘；不调用 nq-live-engine）
  - NQ 端 RiskEngine 正常评估（NQ 自有风控通过；DH 不绕过）
  - 5 字段对账（traceId / requestId / correlationId / sourceJobId / NQ 端 jobId 与 sourceJobId 一致）
  - DH ResearchRun 状态保持（不阻塞；后续等待 BACKTEST_RESULT_READY，由 T3 / T5 用例覆盖）

失败回退
  - NQ 返回 400 → DH 切 FAILED；不重试；oncall
  - NQ 返回 423 AI_DISABLED → DH 切 DISABLED；视为 T5 用例（disabled mode）
```

### 3.5 T5：DH → NQ disabled mode

```text
目标
  验证 DH 显式关闭 backtest-request 子能力时的降级行为；
  验证 ResearchRun 主流程不失败；
  验证 JudgeDecision 仍可生成（DH 主流程不依赖 NQ）；
  验证不发任何 HTTP（DisabledNqBacktestClient 装配）。

前置
  - DH staging decisionhub.stage3.nq.enabled=true && backtest-request.enabled=false
  - DH 已装配 DisabledNqBacktestClient（B3-2 落地）
  - StrategyCandidate(candidateId=stage3-t5-cand, frozen=true) 存在

步骤
  1. DH 端创建 ResearchRun（traceId=stage3-t5-trace, tenantId=t-test-005）
  2. DH 推进研究流程：候选生成 / Reflection / Planner / Checkpoint（正常路径）
  3. DH 业务节点尝试请求 NQ 回测：service.submit(Command)
  4. DisabledNqBacktestClient.submit() 立即返回：
                DhBacktestRequestResult{
                  outcome=DISABLED,
                  errorCode=DH_DISABLED,
                  retryable=false,
                  jobId=null
                }
  5. DH service 处理 outcome=DISABLED → 状态机切 VALIDATED → DISABLED
                 不再调用 NqBacktestClient.submit() 重试
                 不阻塞 ResearchRun 主流程
                 caller 选择继续走 Fake 兜底或人工决策
  6. DH JudgeDecisionService 在不依赖 NQ 回测结果的情况下，仍能生成 JudgeDecision
                （评分依据可降级为：候选打分 + 历史经验 + Fake 回测；
                  禁止用 NQ verdict 自动决策）

期望结果
  - DH submit() 返回 outcome=DISABLED，不抛 RuntimeException
  - DH 端无 HTTP 出站（mock 验证：HttpClient 实例化次数 = 0；或 outbound 流量 = 0）
  - ResearchRun 状态最终为 JUDGE_DECISION_GENERATED 或类似非终态错误
  - JudgeDecision 生成（虽然没有 NQ 回测结果作为输入）
  - 5 字段中 sourceJobId 为 null（合法；DH 端持有的 4 字段保持）
  - 不存在任何 NQ 端入站日志（NQ test cluster 端 confirm zero inbound for this requestId）

失败回退
  - submit() 抛 RuntimeException → DisabledClient 设计违规；回 B3-2 起点
  - 出现任何 HTTP 出站 → DisabledClient 实现违规；回 B3-2 起点
  - JudgeDecision 生成失败 → DH 主流程对 NQ 强依赖；回 B3-4 起点
```

### 3.6 T6：NQ outbox retry / dead-letter（DH 临时 5xx）

```text
目标
  验证 NQ outbox 退避矩阵；
  验证 DH 临时 5xx 时 NQ outbox 重试；
  验证达到 attempt 上限 8 进入 DEAD_LETTER；
  验证 NQ 交易主链路（订单 / 风控 / 账本 / 实盘）不受影响。

前置
  - 同 T1
  - DH staging 配置可注入临时 5xx 的测试钩子（B4-3 stub 控制器；
    联调期间通过 admin API 或注解开关让 DH 临时返回 503）

步骤
  1. NQ test paper engine 触发 PAPER_RUN_DAILY_REPORT_GENERATED 事件
                envelope.eventId=stage3-t6-evt-01
  2. NQ outbox 拉行 → POST DH /api/ai/feedback/nq
  3. DH 配置钩子返回 503（持续 N 次；N 在测试用例参数化，至少覆盖 attempt=0..2）
  4. NQ outbox 按退避矩阵重试：
     attempt=0 立即；失败 → attempt=1 next_retry_at +1s
     attempt=1 重试；失败 → attempt=2 next_retry_at +5s
     attempt=2 重试；DH 此时恢复 → 返回 202 outcome=ACCEPTED
     NQ outbox 行 status=SENT
  5. （另一个子用例）DH 持续返回 503 至 attempt=8 → NQ outbox 转 DEAD_LETTER
     - 行 status=DEAD_LETTER
     - 写入 nq_ai_feedback_dead_letter
     - last_error_code=HTTP_5xx
     - retry_count=8
  6. 任何时刻，NQ 交易主链路（订单状态机 / 风控 / 账本 / 实盘）保持正常运行
     （独立断言：NQ 测试盘 paper run 状态继续推进；不出现 outbox 重试反压）

期望结果
  - 子用例 a（恢复成功）：NQ outbox 行最终 status=SENT；DH dh_nq_feedback_events 1 行；
                          attempt=2 时成功；总耗时约 1s + 5s = 6s（含 ±10% 抖动）
  - 子用例 b（DEAD_LETTER）：NQ outbox 行 status=DEAD_LETTER；nq_ai_feedback_dead_letter 1 行；
                              DH dh_nq_feedback_events 0 行（从未成功）
  - NQ 交易主链路全程无影响（独立断言：paper run KPI 持续 / 风控放行不阻塞）
  - DH 临时 5xx 不导致 NQ 端任何业务实体异常（独立断言：paper_runs / backtest_results
    / orders / ledger 等表无异常写入或回滚）

失败回退
  - NQ outbox 重试间隔不符合退避矩阵 → 回 NQ Batch NQ-4
  - DEAD_LETTER 进入条件不正确 → 回 NQ Batch NQ-4
  - NQ 主链路反压 → 严重；立即停止联调；回 NQ Batch NQ-2 / NQ-4 重新评估 dispatcher 隔离
```

### 3.7 T7：安全边界测试（全文 / 配置 / 启动）

```text
目标
  扫描全文 / 配置 / 路径 / 启动行为，确保 Stage3-B4 完成时没有任何危险关键词 / 路径 /
  实盘配置 / 凭证泄露；
  验证 NQ 无 DH 仍可启动；
  验证 DH 无 NQ 仍可启动。

测试动作（每条独立断言）：

A. 关键词扫描（contracts + docs + 源码）
   ✅ contracts/openapi.yaml         无 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
   ✅ contracts/openapi.yaml paths    无 /orders / /trades / /live
   ✅ contracts/json-schema/*         无上述关键词
   ✅ dh-domain / dh-usecase / dh-connector / dh-api / dh-app 源码    无 placeOrder / submitOrder /
                                                                      executeOrder / bypassRisk /
                                                                      forceExecute（注释中也不允许，
                                                                      除非 NqContractVerifier 自身黑名单豁免）
   ✅ dh-connector.nq 以外的模块      无 RestTemplate / WebClient / OkHttp / HttpURLConnection 引用
                                       （ArchUnit R11 守门）

B. 配置扫描
   ✅ application.yml / application-*.yml      无任何 token / secret 明文（必须 ${ENV} 或 Vault）
   ✅ 默认 profile                              decisionhub.stage3.nq.enabled=false
                                                decisionhub.stage3.nq.backtest-request.enabled=false
                                                decisionhub.stage3.nq.backtest-request.fake-mode=true
   ✅ NQ 默认 profile                           nq.ai.enabled=false
                                                nq.ai.feedback.enabled=false
                                                nq.ai.backtest-request.enabled=false
   ✅ 测试环境 application-stage3-test.yml      DH_NQ_BACKTEST_TOKEN 来自环境变量；不在配置文件
                                                base-url 仅指向 *-test-* 域名；不允许 prod 域名

C. NQ 无 DH 仍可启动（NQ 端独立运行）
   ✅ NQ test cluster 启动时 DH 完全不可达（mock DNS 失败 / 端口拒绝）
   ✅ NQ paper engine / backtest engine / 风控 / 账本 / 实盘 全部正常启动与运行
   ✅ NQ outbox PENDING 堆积；dispatcher 按退避矩阵自然重试；不抛系统级异常
   ✅ NQ 主链路 KPI（订单延迟 / 风控放行率 / 账本一致性）保持正常

D. DH 无 NQ 仍可启动（DH 端独立运行）
   ✅ DH staging 启动时 NQ 完全不可达
   ✅ DH ResearchRun 主流程正常启动
   ✅ DH Stage1ClosedLoopTest 可跑通（Fake 闭环）
   ✅ DH Stage2ClosedLoopTest 可跑通
   ✅ DH 启动后无任何 RestTemplate / WebClient / OkHttp 实例化（默认 Fake / Disabled 兜底）
   ✅ DH /api/ai/feedback/nq 端点可接收请求（mock NQ 端推送）

E. 凭证不泄露
   ✅ DH staging / NQ test cluster 日志中无 token / secret / API key 明文
   ✅ DH error response 不包含任何 token / secret
   ✅ NQ outbox last_error_message 经脱敏（不含 token 子串）
   ✅ payload_json / rawPayloadJson 不包含 token / secret / 实盘账号凭证

F. 实盘隔离
   ✅ NQ test cluster 严禁配置任何实盘账户（OKX / Binance / 其它真实交易所）
   ✅ NQ test cluster paper / backtest 仅写 NQ test DB；不影响 prod
   ✅ DH staging 严禁触发 NQ 端实盘交易（任何 endpoint 都不允许）
   ✅ NQ live trading 配置必须为 false（运行时持续监控）

期望结果
  全部断言 ✅；任一失败 → 联调标记 NO-GO；回 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md
  对应章节排查，禁止跳跃。
```

---

## 四、Contract Test 类型

> 10 类测试覆盖 Stage3 全部契约面与边界面。每类在 B4-1 ~ B4-5 拆批落地。

### 4.1 JSON Schema contract test

```text
范围        contracts/json-schema/*.schema.json 16 份（已 Stage2-PoC + Stage3-B1 落地）
检查项
  - schema 文件存在且可解析
  - required 字段完整
  - additionalProperties=false
  - enum 值与 dh-domain 枚举一一对应（已 Stage3-B1 落地 NqFeedbackEnvelopeSchemaContractTest /
    DhBacktestRequestSchemaContractTest / BacktestResultSnapshotSchemaContractTest）
  - sourceSystem const "nexus-quant"
  - schemaVersion semver
  - 黑名单关键词全无
落点      dh-domain/src/test/.../contracts/（已存在 4 份；B4-1 视需要扩 1~2 份）
基线      Stage3-B1 落地的 29 cases 必须保持全绿（不允许下降）
```

### 4.2 OpenAPI contract test

```text
范围        contracts/openapi.yaml
检查项
  - 已落地端点 /api/ai/feedback/nq 与 NqFeedbackController 一致
  - response 202 引用 NqFeedbackAcceptedResponse；400 引用 NqFeedbackErrorResponse
  - outcome 枚举含 ACCEPTED + DUPLICATE
  - errorCode 枚举含 UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE
  - components 含 DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot
  - NqFeedbackEventType 8 种保持
  - paths 段不含 /orders / /trades / /live
  - 全文不含 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
  - paths 段不含 Stage3-B3 planned endpoint /api/ai/research/backtest-requests /
    /api/ai/backtest-requests（仅注释占位）
落点      Stage3-B1 已有 OpenApiContractAlignmentTest（9 cases，全绿）
基线      Stage3-B1 基线保持
```

### 4.3 HTTP status matrix test

```text
范围        DH /api/ai/feedback/nq 入站 + DH → NQ 出站 backtest request
检查项（DH 入站，B4-1）
  - 202 + ACCEPTED 路径
  - 202 + DUPLICATE 路径
  - 400 + UNKNOWN_EVENT_TYPE
  - 400 + INVALID_SCHEMA
  - 400 + UNKNOWN_TRACE
  - 5xx 临时错误（注入测试钩子）

检查项（DH 出站，B4-1）
  - 202 ACCEPTED 路径
  - 400 INVALID_SYMBOLS / INVALID_DATE_RANGE / UNSUPPORTED_FREQUENCY /
        INVALID_PARAMETERS_JSON / QUOTA_EXCEEDED / RISK_GATED
  - 401 / 403
  - 409 DUPLICATE_REQUEST
  - 423 AI_DISABLED
  - 429 RATE_LIMITED
  - 5xx Server Error
  - timeout
  - network failure

落点        dh-api/src/test/.../NqFeedbackControllerWebMvcTest（入站，已有 7 cases）
            dh-connector/src/test/.../RealNqBacktestClientTest（出站，B3-3 落地 + B4-1 扩展）
            使用 MockWebServer / WireMock；不真发 HTTP
```

### 4.4 Error code matrix test

```text
范围        DH 端错误码与 NQ HTTP 响应映射（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §6.3）
检查项
  - 每个 HTTP 状态 → 唯一 DH errorCode
  - retryable 标记一致
  - 终态切换正确（FAILED / DISABLED / ACCEPTED）
落点      DhBacktestRequestServiceTest（B3-1 / B4-1 扩展）
            NqFeedbackContractValidationTest（入站；Stage2-PoC-B2 已有）
```

### 4.5 Idempotency contract test

```text
范围        DH 入站 eventId 幂等 + DH 出站 requestId 幂等
检查项（入站，已 Stage2-PoC-B5 落地）
  - 同 eventId 重放 → 202 outcome=DUPLICATE
  - dh_nq_feedback_events 唯一索引 race 处理
  - JdbcNqFeedbackEventRepositoryTest（已有 4 cases）

检查项（出站，B3-2 / B4-1 落地）
  - 同 candidateId + paramsHash 24h 内重复 → 短路返回原 requestId
  - NQ 409 DUPLICATE → DH 视为成功 + outcome=DUPLICATE
  - 并发 race 仅一个 SUBMITTED，其它短路

落点      dh-usecase/src/test/.../agent/feedback/NqFeedbackIdempotencyTest（入站；已有）
            dh-usecase/src/test/.../agent/backtest/DhBacktestRequestIdempotencyTest（出站；B3-2）
```

### 4.6 Retry / dead-letter contract test

```text
范围        NQ outbox 退避矩阵（NQ 侧实现，DH 侧观察）+ DH 出站重试（B3-3）
检查项（NQ 端）
  - 8 attempt 退避矩阵单测（NQ-4）
  - 5xx → 重试；400 → 立即 DEAD_LETTER
  - 429 → 退避不计死信上限
  - 7 失败后 attempt=8 → DEAD_LETTER
  - 抖动 ±10%

检查项（DH 端）
  - max-retries=0 默认（不重试）
  - max-retries > 0 时退避矩阵生效（B4-1 视需要扩展）
  - 429 退避不计死信上限

联调验证（T6）
  - DH 临时 5xx → NQ outbox 重试 → 最终 SENT
  - DH 持续 5xx → NQ outbox DEAD_LETTER
  - NQ 主链路全程不阻塞（独立断言）

落点      NQ 仓库 OutboxStateMachineTest / DeadLetterMoverTest（NQ-2 / NQ-4）
            DH 仓库 RealNqBacktestClientTest（B3-3 mock HTTP）
```

### 4.7 Disabled mode startup test

```text
范围        DH 默认 profile / DH 显式 disabled / NQ 默认 profile
检查项（DH）
  - 默认 profile bean wiring → FakeNqBacktestClient（NoNqDependencyStartupTest）
  - stage3.nq.enabled=true && backtest-request.enabled=false → DisabledNqBacktestClient
  - 启动后无 HTTP 出站（mock HttpClient 实例化次数 = 0）
  - ResearchRun 主流程正常启动

检查项（NQ）
  - nq.ai.enabled=false → 任何 backtest-request 入站返回 423 AI_DISABLED
  - feedback outbox dispatcher 未装配
  - NQ 主链路正常启动

联调验证（T5）
  - DH disabled 模式不发任何 HTTP；ResearchRun 主流程不阻塞；JudgeDecision 仍生成

落点      dh-app/src/test/.../config/RealNqBacktestClientDisabledByDefaultTest（B3-2）
            dh-app/src/test/.../config/NoNqDependencyStartupTest（B3-5）
```

### 4.8 No dangerous endpoint test

```text
范围        全仓库黑名单扫描
检查项
  - contracts/openapi.yaml 全文无 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
  - contracts/openapi.yaml paths 段无 /orders / /trades / /live
  - contracts/json-schema/*.schema.json 全文无上述关键词
  - dh-domain / dh-usecase / dh-connector / dh-api / dh-app 源码无上述关键词
    （NqContractVerifier 自身黑名单豁免）
  - dh-domain.backtest.DhBacktestRequestStatus 枚举值无 PLACE_ / SUBMIT_ / EXECUTE_ 前缀
  - ArchitectureTest（已落地 10 条）保持全绿

落点      dh-domain/src/test/.../contracts/OpenApiContractAlignmentTest（已有 9 cases）
            dh-app/src/test/.../ArchitectureTest（已有 10 cases）
            B4-1 视需要新增 dh-domain/src/test/.../contracts/NoDangerousEndpointContractTest
            （全仓库递归 grep 断言）
```

### 4.9 Trace correlation test

```text
范围        5 字段（traceId / requestId / correlationId / sourceJobId / eventId）端到端对账
检查项（DH 单测，B4-1）
  - DhBacktestRequestService.submit() 后 4 字段（traceId / requestId / correlationId）
    在 DhBacktestRequestRepository 持久化
  - NqFeedbackIngestionService 命中后 5 字段写入 dh_nq_feedback_events
  - 反查 ResearchRun by traceId 命中

检查项（联调验证，T1-T7）
  - 任一用例完成后，5 字段在以下三处完全一致：
    1. DH staging 日志
    2. NQ test cluster 日志
    3. DH 数据库（dh_research_runs / dh_nq_feedback_events / dh_backtest_request_outbox）
  - 任一失配 → 用例失败

落点      dh-usecase/src/test/.../agent/Stage3TraceCorrelationTest（B4-1）
            dh-app/src/test/.../Stage3EndToEndTraceTest（B4-5 联调）
```

### 4.10 Regression test

```text
范围        Stage1 / Stage2 / Stage3-B1 已落地用例（151 tests）全程回归
检查项
  - Stage1ClosedLoopTest 1/1
  - Stage2ClosedLoopTest 2/2
  - NqFeedbackContractValidationTest 7/7
  - NqFeedbackIdempotencyTest 3/3
  - NqFeedbackHandlerDispatchTest 5/5
  - NqFeedbackControllerWebMvcTest 7/7
  - 4 份 Stage3-B1 contract test 共 29/29
  - 全部历史 dh-domain / dh-connector / dh-infra / dh-app 用例
  - ArchUnit 10/10

约束
  - 任一 Batch 完成后必须保持 151 tests 不下降
  - B4-1 ~ B4-5 完成后 tests 总数预期 170+（具体以 IMPLEMENT 阶段为准）

落点      整体 mvn test
            CI 在每次 PR 触发；ENABLED_STAGE3 false 时不跑联调用例
```

---

## 五、测试数据与追踪规则

### 5.1 5 字段生成规则

```text
eventId
  生成方     NQ outbox 写入前生成（NQ 端）
  推荐格式   UUIDv7
  联调前缀   stage3-{用例}-{seq}-evt-NN  例 stage3-t1-evt-01
  生命周期   单条 feedback event；DH 端幂等键
  禁止       DH 端伪造 eventId；NQ 端用 eventId 充当 sourceJobId

requestId
  生成方     DH 出站请求方（DhBacktestRequestService.IdGenerator）
  推荐格式   UUIDv7
  联调前缀   stage3-{用例}-{seq}-req     例 stage3-t4-req-01
  生命周期   单次 DH → NQ 请求；NQ feedback 必须原样回传
  禁止       DH 在 ingest 路径生成新 requestId；NQ 修改 requestId

traceId
  生成方     DH 端 ResearchRun 创建时
  推荐格式   32 位 hex（OpenTelemetry trace id 对齐）；联调前缀 stage3-{用例}-trace
  生命周期   跨 DH ResearchRun + NQ Job 全程
  禁止       NQ 端独立生成 traceId（必须沿用 DH 上游）；DH ingest 路径无 traceId

correlationId
  生成方     DH 业务上下文（candidate 进入研究周期时）
  推荐格式   UUIDv7；联调前缀 stage3-{用例}-corr
  生命周期   业务周期；可串多个 traceId / requestId
  禁止       与 traceId 混用；与 sourceJobId 混用

sourceJobId
  生成方     NQ 端（paperRunId / backtestId / alertId 等）
  推荐格式   NQ 端任意字符串；联调前缀 nq-{业务}-NN  例 nq-paper-001 / nq-bt-002
  生命周期   NQ 端 Job
  禁止       DH 端伪造；DH 用 sourceJobId 当幂等键
```

### 5.2 payload 留档

```text
payloadJson（envelope 顶层）
  内容     payload 原始 JSON 字符串（NQ 端按 contracts/json-schema/nq-feedback-*.schema.json 序列化）
  约束     DH 永久留底；不允许丢失；写入 dh_nq_feedback_events.payload_json (jsonb)

rawPayloadJson（payload 内部，BACKTEST_RESULT_READY 专属）
  内容     NQ 端原始结果 JSON 字符串
  约束     DH 永久留底；写入 dh_backtest_result_snapshot.raw_payload_json

约束（所有 payload）
  - 不允许携带 API key / secret / token / 实盘账户凭证 / cookies
  - 不允许携带订单 ID / fillId / positionId / liveAccountId
  - 不允许携带敏感市场行情（如 prod 流量数据；测试数据可）
  - 长度建议 < 256KB；超过 → 写 NQ 内部 blob，envelope 只带引用 URL（联调超出范围）

测试数据准备
  - DH 端：dh-app/src/test/resources/stage3-fixtures/*.json（B4-1 落地）
  - NQ 端：NQ 仓库 src/test/resources/stage3-fixtures/*.json（NQ-5 落地）
  - 双方 fixtures 字段保持一致；通过 contracts/json-schema 共同约束
```

### 5.3 deterministic 数据

```text
所有测试数据必须 deterministic（可复现）：
  - 测试 universe 由 NQ 团队定义（推荐 ["TEST-SYM-A", "TEST-SYM-B", "TEST-SYM-C"]）
  - 测试时间窗口 fixed（如 2026-01-01 to 2026-04-30）
  - 测试 initialCapital 固定（如 100000.00）
  - paramsHash 计算确定性（sha256，输入排序）
  - sourceJobId 联调期间使用固定前缀 + 序号
  - eventId / requestId UUIDv7（虽含随机，但用例可断言"非空且唯一"，不断言具体值）

禁止
  - 任何真实账户（账号 / 余额 / API key / token）
  - 任何 prod 流量 / 行情数据
  - 任何非测试 universe 标的
  - 任何 datetime "now()" 在断言中（必须固定时间或使用 TimeProvider mock）
  - 任何依赖网络外部服务的随机数（必须本地确定性）

数据隔离
  - tenantId 前缀 "t-test-*"，所有用例严格遵守
  - 任何 prod tenant 渗入 → 立即停止；触发数据销毁
  - 联调结束后 staging DB 数据保留 30 天供排错；之后清理
```

---

## 六、验收命令规划

### 6.1 DH 仓库

```text
默认 profile（每次 PR 触发；CI 跑）：
  mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

CI Docker 环境（PostgresContainerSmokeTest）：
  mvn test -Dtest='PostgresContainerSmokeTest'

Stage3 联调用例（仅 staging 跑；默认 profile 不跑）：
  ENABLED_STAGE3=true \
  DH_NQ_BACKTEST_BASE_URL=https://nq-test-cluster.example.com \
  DH_NQ_BACKTEST_TOKEN=<from vault> \
    mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'

特定用例：
  ENABLED_STAGE3=true \
    mvn -pl dh-app test -Dtest='Stage3InboundFeedbackEndToEndTest'    # T1 / T2 / T3
  ENABLED_STAGE3=true \
    mvn -pl dh-app test -Dtest='Stage3OutboundBacktestEndToEndTest'   # T4 / T5
  ENABLED_STAGE3=true \
    mvn -pl dh-app test -Dtest='Stage3OutboxRetryEndToEndTest'        # T6
  ENABLED_STAGE3=true \
    mvn -pl dh-app test -Dtest='Stage3SecurityBoundaryTest'           # T7
```

### 6.2 NQ 仓库（NQ 团队执行；本仓库仅声明）

```text
默认 profile：
  mvn -f backend/pom.xml test

联调 profile（仅 NQ test cluster 跑）：
  ENABLED_STAGE3=true \
  NQ_AI_FEEDBACK_ENDPOINT=https://dh-stage3-test.example.com/api/ai/feedback/nq \
  NQ_AI_FEEDBACK_TOKEN=<from vault> \
    mvn -f backend/pom.xml test -Dtest='Stage3*ContractTest*'
```

### 6.3 端到端联调执行 checklist

```text
联调启动前：
  1. NQ 团队确认 NQ-1 ~ NQ-4 完工，NQ test cluster 部署完毕
  2. DH 团队确认 B3-1 ~ B3-5 完工，DH staging 部署完毕
  3. 双方 oncall 评审 STAGE3_E2E_CONTRACT_TEST_SPEC（本文件）
  4. NQ test cluster nq.ai.enabled=true（仅联调期间）
  5. DH staging decisionhub.stage3.nq.enabled=true（仅联调期间）

联调执行：
  6. 默认 profile 跑通 DH mvn test BUILD SUCCESS（151+ tests 全绿）
  7. 默认 profile 跑通 NQ mvn test BUILD SUCCESS
  8. PostgresContainerSmokeTest 在 CI Docker 跑通
  9. 启动 T1（PAPER_RUN_CREATED）→ 全绿
  10. 启动 T2（PAPER_RUN_ALERT_RAISED 幂等）→ 全绿
  11. 启动 T3（BACKTEST_RESULT_READY 结果消费）→ 全绿
  12. 启动 T4（backtest request accepted）→ 全绿
  13. 启动 T5（disabled mode）→ 全绿
  14. 启动 T6（outbox retry / dead-letter）→ 全绿
  15. 启动 T7（安全边界扫描）→ 全绿

联调结束后：
  16. 双方关闭 nq.ai.enabled / decisionhub.stage3.nq.enabled
  17. 产出 docs/current/STAGE3_VERIFY_REPORT.md（Verdict: GO / NO-GO）
  18. GO → Stage3-FREEZE（拷贝 docs/current 到 docs/gates/dh-stage3/）
  19. NO-GO → 回对应 Batch PLAN；禁止跳跃 / 硬抹平
```

---

## 七、失败处理规则

### 7.1 DH 入站（NQ → DH）失败映射

| DH 响应 | 含义 | NQ outbox 行为 | 是否重试 | 是否进 DEAD_LETTER |
| --- | --- | --- | --- | --- |
| 202 ACCEPTED | 首次接收成功 | status=SENT | - | 否 |
| 202 DUPLICATE | 幂等重放命中 | status=SENT（视为成功） | - | 否 |
| 400 UNKNOWN_EVENT_TYPE | envelope.eventType 不在 8 枚举 | status=DEAD_LETTER | 否 | 立即进入 |
| 400 INVALID_SCHEMA | envelope / payload 不符合 schemaVersion | status=DEAD_LETTER | 否 | 立即进入 |
| 400 UNKNOWN_TRACE | envelope.traceId 在 DH 找不到 | status=DEAD_LETTER | 否 | 立即进入 |
| 401 Unauthorized | 认证失败 | status=DEAD_LETTER；告警 | 否 | 立即进入 |
| 403 Forbidden | 授权失败 | status=DEAD_LETTER；告警 | 否 | 立即进入 |
| 429 Rate Limited | 限流 | 退避重试；不计死信上限 | 是 | 否 |
| 5xx Server Error | DH 临时不可用 | 退避重试 | 是 | retry_count >= 8 |
| timeout / network | 网络错误 | 退避重试 | 是 | retry_count >= 8 |

### 7.2 DH 出站（DH → NQ）失败映射

| NQ 响应 | 含义 | DH 状态机切换 | 是否重试 | 是否阻塞 ResearchRun |
| --- | --- | --- | --- | --- |
| 202 ACCEPTED + jobId | NQ 已入队 | SUBMITTED → ACCEPTED | - | 否（继续等待异步结果） |
| 400 INVALID_SCHEMA | 参数错误 | SUBMITTED → FAILED | 否 | 否（永久失败；caller 决定 Fake 兜底） |
| 400 INVALID_SYMBOLS / etc | 字段错误 | SUBMITTED → FAILED | 否 | 否 |
| 401 Unauthorized | 认证错误 | SUBMITTED → FAILED + 告警 | 否 | 否 |
| 403 Forbidden | 授权错误 | SUBMITTED → FAILED + 告警 | 否 | 否 |
| 409 DUPLICATE_REQUEST | NQ 已接收过 | SUBMITTED → ACCEPTED + outcome=DUPLICATE | 否 | 否（视为成功） |
| 423 AI_DISABLED | NQ AI 关闭 | SUBMITTED → DISABLED | 否 | 否（caller 走 Fake） |
| 429 Rate Limited | 限流 | 保持 SUBMITTED；按退避矩阵重试 | 是 | 否（异步重试） |
| 5xx Server Error | NQ 临时错误 | 保持 SUBMITTED；按退避矩阵重试 | 是 | 否 |
| timeout / network | 网络错误 | 保持 SUBMITTED；按退避矩阵重试 | 是 | 否 |

### 7.3 联调用例失败处理

```text
T1-T7 任一用例失败：
  1. 立即停止当前用例；保留 staging / test cluster 状态供排错
  2. 排错优先级：
     a) 5 字段对账失配      → 回 STAGE3_CONTRACT_PLAN §3 / 本 SPEC §5.1 排查
     b) HTTP 响应矩阵失配   → 回 STAGE3_DH_BACKTEST_ADAPTER_SPEC §6 / NQ Batch NQ-4 排查
     c) 幂等失效            → 回 dh_nq_feedback_events 唯一索引 + STAGE3_NQ_OUTBOX_SPEC §6 排查
     d) 重试矩阵异常        → 回 STAGE3_NQ_OUTBOX_SPEC §5.3 排查
     e) 主链路反压          → 严重；回 STAGE3_NQ_OUTBOX_SPEC §5.7 / NQ Batch NQ-2 dispatcher 隔离
     f) 实盘配置渗入        → 立即停止联调；触发数据销毁；回 §2.4 环境隔离强约束
  3. 修复后回归该用例 + 全部前序用例
  4. 三轮失败 → 回该 Batch PLAN；禁止"硬抹平"通过

联调环境回滚：
  - 失败后 1 小时内 staging decisionhub.stage3.nq.enabled=false
  - NQ test cluster nq.ai.enabled=false
  - 5 字段隔离：联调期间数据 tenantId=t-test-* / traceId / correlationId 前缀 stage3-；
    联调结束后保留 30 天供 oncall 排错，之后清理

prod 应急回滚（极端场景；联调期间不应触发）：
  - 任何 prod 流量 / token / tenantId 渗入联调环境 → 立即触发数据销毁 + oncall
  - 任何 prod 配置变更未经过 ChangeRequest → 立即回滚
  - 联调期间 prod 始终保持 enabled=false（双方 oncall 持续监控）
```

### 7.4 NQ 主链路保护（参见 STAGE3_NQ_OUTBOX_SPEC §9）

```text
任何联调失败下，NQ 必须保证：
  - 订单状态机正常运行；下单 / 撤单 / 成交不受 outbox 失败影响
  - 风控放行率持续正常
  - 账本一致性持续保持
  - 实盘 / 模拟盘 / 回测核心正常运行
  - GateJ-FREEZE 不退化

DH 主链路保护：
  - NQ 不可达时 DH ResearchRun 仍能完成 Fake 闭环
  - DH JudgeDecision 仍可基于"候选打分 + 历史经验 + Fake 回测"生成
  - Stage1ClosedLoopTest / Stage2ClosedLoopTest 全程保持全绿
```

---

## 八、后续 Stage3-B4 IMPLEMENT Batch 建议

> 本 PLAN 不实施。B4-1 ~ B4-5 按顺序拆批落地；任一 Batch 失败回到该 Batch 起点。

### 8.1 Batch B4-1：DH contract test suite

```text
目标
  - 在 DH 仓库内落 Stage3 端到端联调用例骨架（@EnabledIfEnvironmentVariable 隔离）
  - 默认 profile 不跑联调用例；CI 仍 BUILD SUCCESS
  - 扩展 4.1 ~ 4.5 contract test 类型在 DH 仓库内的覆盖

允许改动
  - dh-app/src/test/.../Stage3InboundFeedbackEndToEndTest.java                    (T1 / T2 / T3 骨架)
  - dh-app/src/test/.../Stage3OutboundBacktestEndToEndTest.java                   (T4 / T5 骨架)
  - dh-app/src/test/.../Stage3OutboxRetryEndToEndTest.java                        (T6 骨架)
  - dh-app/src/test/.../Stage3SecurityBoundaryTest.java                           (T7 安全扫描)
  - dh-usecase/src/test/.../agent/Stage3TraceCorrelationTest.java                 (5 字段对账 mock)
  - dh-app/src/test/resources/stage3-fixtures/*.json                              (测试 fixture)
  - dh-app/src/main/resources/application-stage3-test.yml                         (联调 profile 配置)

禁止改动
  - 不修改 contracts/openapi.yaml 已落地端点语义
  - 不修改 contracts/json-schema/*.schema.json 已落地字段
  - 不修改 Flyway migration
  - 不引入真实 NQ 调用（B4-3 / B4-5 才允许联调）
  - 不放松 ArchUnit 10 条规则
  - 默认 profile mvn test 必须仍 BUILD SUCCESS

文件清单
  - 上述测试类 + fixtures + profile 配置文件

验收标准
  - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS
  - 默认 profile 联调用例自动 skip（@EnabledIfEnvironmentVariable 检查 ENABLED_STAGE3）
  - ArchUnit 10/10 保持
  - 5 字段对账 mock 测试覆盖 Trace correlation 路径
```

### 8.2 Batch B4-2：NQ contract test fixture plan

```text
目标
  - 在 NQ 仓库内落 Stage3 contract test fixtures（NQ 团队执行；DH 仓库仅声明口径）
  - NQ 端 8 种事件 envelope 序列化 fixture
  - NQ 端 outbox 状态机 fixture
  - NQ 端 backtest accepted fixture

允许改动（NQ 仓库）
  - NQ 仓库 src/test/resources/stage3-fixtures/*.json                              (8 envelope + 8 payload + 2 accepted)
  - NQ 仓库 Stage3ContractTest.java                                                 (NQ 端契约测试)
  - NQ 仓库 application-stage3-test.yml                                             (联调 profile)

禁止改动（NQ 仓库）
  - 不修改 NQ 交易主链路代码
  - 不修改 NQ 风控核心
  - 不修改 NQ 回测内核
  - 不接 DH 真实 endpoint（B4-5 才允许联调）

允许改动（DH 仓库）
  - 不允许任何 DH 仓库改动（本 Batch 不动 DH）

文件清单
  - NQ 仓库测试类 + fixtures

验收标准
  - NQ mvn test BUILD SUCCESS（NQ 团队跑）
  - NQ fixtures 字段与 DH contracts/json-schema/*.schema.json 一致（人工对照 + 单测断言）
  - 不影响 NQ 既有交易主链路
```

### 8.3 Batch B4-3：Stub server / fake server

```text
目标
  - 在 DH 仓库内落 stub server（WireMock / MockWebServer 集成）
  - 模拟 NQ 端 backtest-requests endpoint 各种响应（202 / 400 / 409 / 423 / 429 / 5xx）
  - 模拟 NQ outbox 行为（推送各种 feedback envelope 到 DH /api/ai/feedback/nq）
  - 不依赖真实 NQ test cluster

允许改动
  - dh-connector/src/test/.../nq/StubNqBacktestServer.java                         (基于 WireMock；
                                                                                    复用 RealNqBacktestClientTest)
  - dh-app/src/test/.../Stage3StubServerEndToEndTest.java                          (用 stub server 跑 T1-T6 简化版)
  - 测试依赖：dh-connector pom.xml 添加 wiremock-jre8 (test scope) 或 mockwebserver

禁止改动
  - 不引入生产依赖（仅 test scope）
  - 不修改 dh-connector.nq 主代码
  - 不接真实 NQ test cluster

文件清单
  - 上述测试类 + pom.xml 依赖增量

验收标准
  - mvn test BUILD SUCCESS
  - stub server 覆盖 8 种 HTTP 响应
  - 默认 profile 跑通（stub server 不依赖外部资源）
```

### 8.4 Batch B4-4：Disabled mode startup test

```text
目标
  - 在 DH 仓库内落 disabled / no-NQ 启动测试
  - 验证 DH 默认 profile 启动不依赖 NQ 可达
  - 验证 DisabledNqBacktestClient 装配场景下零 HTTP 出站
  - 验证 NoNqDependencyStartupTest 完整覆盖

允许改动
  - dh-app/src/test/.../config/NoNqDependencyStartupTest.java                      (DH 启动不依赖 NQ)
  - dh-app/src/test/.../config/DisabledClientNoHttpEgressTest.java                 (零 HTTP 出站)
  - dh-app/src/test/.../config/Stage3WiringConfigBeanTypeTest.java                 (三 client 切换断言)

禁止改动
  - 不修改 Stage3NqBacktestWiringConfig 主代码（仅断言）
  - 不修改默认 profile 配置
  - 不引入真实 HTTP 出站

文件清单
  - 上述测试类

验收标准
  - mvn test BUILD SUCCESS
  - 默认 profile bean wiring 验证 FakeNqBacktestClient 装配
  - DisabledClient 装配场景下 HTTP 客户端实例化次数 = 0
  - Spring Boot context 在 NQ 完全不可达情况下成功启动（mock DNS 失败 / 端口拒绝）
```

### 8.5 Batch B4-5：End-to-end dry-run checklist

```text
目标
  - 在 NQ test cluster + DH staging 真正跑 T1-T7 联调用例
  - 产出 docs/current/STAGE3_VERIFY_REPORT.md（Verdict: GO / NO-GO）
  - 完成 Stage3-VERIFY 收口

前置（必须满足才允许进入本 Batch）
  - B4-1 ~ B4-4 完工
  - NQ-1 ~ NQ-5（NQ 仓库）完工
  - 双方 oncall 评审通过

允许改动
  - 仅写文档：docs/current/STAGE3_VERIFY_REPORT.md（Verdict + T1-T7 联调结果 + 失败排查记录）
  - 视情况补 dh-app/src/test/.../Stage3*EndToEnd*Test.java 修复联调期间发现的 mock 缺陷
  - 配置 staging / test cluster 联调 profile（不写入仓库；通过 ConfigMap / Vault 注入）

禁止改动
  - 不接实盘 / 不自动发布策略 / 不自动下单 / 不绕风控
  - 不修改 NQ 交易主链路代码
  - 不修改 DH 主链路代码
  - 不用 prod tenant 做联调（仅 t-test-*）
  - 不修改 contracts / migration / OpenAPI 语义
  - 不放松 ArchUnit 规则
  - 默认 profile mvn test 必须仍 BUILD SUCCESS

文件清单
  - docs/current/STAGE3_VERIFY_REPORT.md
  - 视需要：dh-app/src/test/.../Stage3*EndToEnd*Test.java 微调

验收标准
  - 7 个联调用例（T1-T7）全绿
  - 默认 profile mvn test 仍 BUILD SUCCESS（不含联调用例）
  - PostgresContainerSmokeTest 在 CI Docker 通过
  - ArchUnit 10/10 保持
  - 边界关键词扫描 ✅
  - STAGE3_VERIFY_REPORT.md Verdict = GO
  - GO 后进入 Stage3-FREEZE：
    * docs/current/* 完整拷贝到 docs/gates/dh-stage3/
    * 6 份状态文档切换到 "Stage3 FREEZE completed / Next: DH-FREEZE"
  - NO-GO → 回对应 Batch PLAN，禁止跳跃；禁止"硬抹平"通过
```

### 8.6 Batch 依赖与执行顺序

```text
B4-1 DH contract test suite              先落 DH 端测试骨架（默认 profile）
  |
B4-2 NQ contract test fixture plan       NQ 端 fixture（NQ 团队执行）
  |
B4-3 Stub server / fake server           DH 端 WireMock / MockWebServer
  |
B4-4 Disabled mode startup test          DH 启动不依赖 NQ
  |
B4-5 End-to-end dry-run checklist        真正联调 + STAGE3_VERIFY_REPORT.md

任一 Batch 验收失败 → 回该 Batch 起点，禁止跳跃。
B4-5 GO → Stage3-FREEZE。
B4-5 NO-GO → 回 B4-1 / B4-2 / B4-3 / B4-4 中对应 Batch PLAN。
```

---

## 九、验收标准

### 9.1 本轮（Stage3-B4 End-to-End Contract Test PLAN）完成标准

```text
本仓库 Stage3-B4 仅落 SPEC 文档；零 Java 业务代码改动；零 NQ 仓库改动；零 contracts /
migration / OpenAPI path 修改；零真实联调；零实盘；零自动下单。

文档完成度：
  ✅ docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md 存在（本文件）
  ✅ T1-T7 用例完整（7 用例 × 目标 / 前置 / 步骤 / 期望 / 失败回退 5 维度）
  ✅ disabled / fake / real 三种模式测试策略完整（参见 §3.5 / §4.7）
  ✅ NQ 无 DH 可启动、DH 无 NQ 可启动的测试策略明确（参见 §3.7 / §4.7）
  ✅ 10 类 Contract Test 类型完整（JSON Schema / OpenAPI / HTTP status / Error code /
     Idempotency / Retry+dead-letter / Disabled startup / No dangerous endpoint /
     Trace correlation / Regression）
  ✅ 测试数据与追踪规则完整（5 字段生成 + payload 留档 + deterministic 数据）
  ✅ 验收命令规划完整（DH / NQ / 联调三段）
  ✅ 失败处理规则完整（DH 入站 / DH 出站 / 联调用例 / NQ 主链路保护）
  ✅ 后续 Stage3-B4 IMPLEMENT 5 个 Batch 完整（B4-1 ~ B4-5）
  ✅ 硬边界明确（不接实盘 / 不自动下单 / 不自动发布 / 不绕风控 / 不重写回测核心）

文档同步：
  ✅ README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md / TESTING.md
     同步到 "Stage3-B4 End-to-End Contract Test PLAN completed / Next: Stage3-PLAN-FREEZE"

代码与构建：
  ✅ 无 Java 业务代码修改
  ✅ 无 contracts/openapi.yaml 修改
  ✅ 无 contracts/json-schema/*.schema.json 修改
  ✅ 无 Flyway migration 新增 / 修改
  ✅ 无 NQ 仓库改动
  ✅ mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  ✅ ArchUnit 10/10 保持
  ✅ Stage1ClosedLoopTest / Stage2ClosedLoopTest 保持全绿
  ✅ 151 tests（Stage3-B1 + B2 + B3 基线）保持，未下降
```

### 9.2 后续 Stage3-B4 IMPL 验收口径（B4-1 ~ B4-5 实施后）

```text
代码完成度：
  - DH 仓库 Stage3*EndToEnd*Test 骨架（@EnabledIfEnvironmentVariable 隔离）
  - DH 仓库 NoNqDependencyStartupTest / DisabledClientNoHttpEgressTest /
    Stage3WiringConfigBeanTypeTest
  - DH 仓库 StubNqBacktestServer（WireMock 集成）
  - NQ 仓库 Stage3ContractTest + fixtures
  - docs/current/STAGE3_VERIFY_REPORT.md（联调结果）

行为完成度：
  - 默认 profile mvn test BUILD SUCCESS（联调用例自动 skip）
  - 联调 profile（ENABLED_STAGE3=true）T1-T7 全绿
  - DH 默认 profile 启动不依赖 NQ
  - NQ 默认 profile 启动不依赖 DH
  - 5 字段在双方日志 + DH 数据库三处端到端对账一致
  - DH 临时 5xx 时 NQ outbox 退避矩阵生效；最终 SENT 或 DEAD_LETTER
  - NQ 主链路全程不受联调影响

边界完成度：
  - 全文不含 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
  - paths 段不含 /orders / /trades / /live
  - dh-connector.nq 以外不引用 RestTemplate / WebClient / OkHttp
  - 不接实盘 / 不自动下单 / 不自动发布
  - 联调环境 prod 完全隔离；tenantId 前缀 t-test-* 严格遵守
  - 凭证不泄露（日志 / error response / payload 均无 token / secret 明文）
  - STAGE3_VERIFY_REPORT.md Verdict = GO
```

### 9.3 硬边界（本 SPEC 与 NQ 后续实施都不允许违反）

```text
不修改 NQ 仓库                          不修改 Java 业务代码（本 PLAN 阶段）
不修改 contracts/openapi.yaml 语义       不修改 contracts/json-schema 已落地字段
不新增 Flyway migration                  不新增 OpenAPI path
不接真实 NQ API（本 PLAN）              不启动真实联调（本 PLAN）
不接真实 Kronos                          不接真实 global-stock-data
不引入 TradingAgents Python              不实现真实下单
不绕过 NQ 风控                          不重写 NQ 回测核心
不建设前端                              不自动发布策略
不自动下单                              不让 DH 标记正式回测成功
不让 DH 覆盖 NQ verdict                  不让 NQ 强依赖 DH 才能启动
不让 DH 强依赖 NQ 才能完成 ResearchRun   不在联调使用 prod tenant
不在联调使用 prod URL                    不在联调使用真实账户 / 真实凭证
默认 profile 不创建真实 NQ client 实例    联调失败禁止"硬抹平"通过
```

---

## 十、与 Stage3 其他文档的衔接

```text
Stage3 主索引           docs/current/STAGE3_PLAN.md
4 批 IMPLEMENT 工单     docs/current/STAGE3_WORK_ORDER.md                    (B1 ~ B4 工单)
Batch 边界对照表        docs/current/STAGE3_BATCH_PLAN.md                    (执行顺序 + 依赖)
端到端契约规则          docs/current/STAGE3_CONTRACT_PLAN.md                 (envelope / 5 字段 / errorCode / version)
NQ → DH 出站事件链路    docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md       (8 事件 + ingest 路径 + 幂等)
DH → NQ 入站请求链路    docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md       (DhBacktestRequest 出站 + result snapshot)
NQ outbox 规格          docs/current/STAGE3_NQ_OUTBOX_SPEC.md                (NQ 端表 / 模块 / 触发点 / retry)
DH backtest adapter 规格 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md     (DH 端 adapter / 三 client / 状态机)
E2E contract test 规格（本文件）
                        docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md        (联调用例 T1-T7 + 10 类测试)
测试策略                docs/current/STAGE3_TEST_PLAN.md                     (单测 / 联调 / 幂等 / 重试 / 边界)
```

本 SPEC 是 Stage3-B4 IMPLEMENT 阶段的事实源；B4-1 ~ B4-5 实施时若发现 SPEC 与已落地契约 /
adapter / outbox 不一致，应回 docs/current/STAGE3_CONTRACT_PLAN.md /
docs/current/STAGE3_NQ_OUTBOX_SPEC.md / docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md
确认；必要时回 Stage3-B4 PLAN 修订本 SPEC。

---

## 十一、Stage3-PLAN-FREEZE 衔接

```text
Stage3-B4 PLAN completed（本文件落盘）之后：

下一阶段：Stage3-PLAN-FREEZE
  - Stage3 全部 PLAN 文档已完工：
    1. STAGE3_PLAN.md
    2. STAGE3_BATCH_PLAN.md
    3. STAGE3_WORK_ORDER.md
    4. STAGE3_CONTRACT_PLAN.md
    5. STAGE3_TEST_PLAN.md
    6. STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
    7. STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
    8. STAGE3_NQ_OUTBOX_SPEC.md         (Stage3-B2 PLAN)
    9. STAGE3_DH_BACKTEST_ADAPTER_SPEC.md (Stage3-B3 PLAN)
    10. STAGE3_E2E_CONTRACT_TEST_SPEC.md  (Stage3-B4 PLAN，本文件)

  - Stage3-PLAN-FREEZE 动作：
    * 评审 10 份 STAGE3_*.md 文档之间的口径一致性
    * 视需要在 docs/gates/dh-stage3-plan/ 落盘冻结快照（与 Stage1-FREEZE / Stage2-PoC-FREEZE 同体例）
    * README / AGENTS / docs/current/README / STATUS 切到 "Stage3-PLAN-FREEZE completed /
      Next: Stage3-B1 IMPLEMENT"
    * 重新审视 Stage3-B1 ~ B4 IMPL 路径

  - Stage3-PLAN-FREEZE 之后进入 IMPLEMENT 阶段：
    Batch 1 Contract Alignment IMPLEMENT       (DH 仓库内补 8 Handler 经验沉淀；已 Stage3-B1 IMPL 完成)
    Batch 2 NQ Feedback Outbox IMPL            (NQ 仓库后续实施，按 STAGE3_NQ_OUTBOX_SPEC §8)
    Batch 3 DH Backtest Request Adapter IMPL   (DH 仓库后续实施，按 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12)
    Batch 4 End-to-End Contract Test IMPL/VERIFY (DH+NQ 联调，按本 SPEC §8)

  - Stage3-FREEZE 路径（B4 VERIFY GO 之后）：
    docs/current/* 完整拷贝到 docs/gates/dh-stage3/
    6 份状态文档切到 "Stage3 FREEZE completed / Next: DH-FREEZE"

  - DH-FREEZE：Decision Hub Agent Decision Layer v1 长期维护态
```
