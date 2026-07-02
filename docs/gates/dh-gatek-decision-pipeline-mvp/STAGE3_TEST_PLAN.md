# Stage3 Test Plan

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md`
> Scope: 规划 Stage3 IMPLEMENT 阶段在 DH 仓库内、以及与 NQ 联调时需要落地的测试。本文件只规划，不实现。

## 1. DH 单测计划（仅在 DH 仓库内运行）

### 1.1 NqFeedbackIngestion 经验沉淀路径（Batch 1/2 新增）

```text
为每个 NqFeedbackEventType 增加一个 unit test：
  PAPER_RUN_DAILY_REPORT_GENERATED -> ExperienceEntry 写入校验
  BACKTEST_RESULT_READY            -> ExperienceEntry + PheromoneEdge 双写校验
  PAPER_RUN_ALERT_RAISED           -> FailureCaseStore 写入校验
  其他 5 种                          -> 时间线写入校验

挂载点：dh-usecase 模块；继续走 InMemory 仓储；不依赖 Docker
依赖：dh-domain.experience.* / dh-memory.* 已落地，仅新增写入路径
覆盖：fresh ingest / duplicate replay / unknown trace 3 个分支
```

### 1.2 DhBacktestRequest 出站生成路径（Batch 3 新增）

```text
DhBacktestRequestBuilder（dh-usecase 内待落，Stage3 IMPLEMENT 实现）：
  - 从 ResearchRun + StrategyCandidate 构造 DhBacktestRequest 字段
  - 校验 symbols 非空 / startDate < endDate / frequency 合法 / initialCapital > 0
  - requestId 唯一性

NqBacktestClient Fake 端到端：
  - 输入 DhBacktestRequest -> 输出 DhBacktestRequestAccepted
  - 模拟 NQ 异步 feedback -> ingest 链路 -> ExperienceEntry 更新

挂载点：dh-usecase / dh-connector；不依赖真实 HTTP
```

### 1.3 ArchUnit 边界守门（Batch 1 调整）

```text
现有 10 条规则保持；Stage3 IMPLEMENT 阶段视情况追加：
  - dh-connector.nq 仍禁字（不得引用 OkHttp/WebClient 之类真实 HTTP，除非 Stage3 Batch 3
    开启对应 conditional bean，且仍仅在 dh-connector 模块内）
  - dh-usecase.agent.feedback 不得直接持有 NQ HTTP 客户端
  - api 层禁路径 /orders /trades /live 保持

不允许 ArchUnit 规则破坏 Stage1 / Stage2 已落地边界
```

## 2. NQ 契约测试计划（NQ 仓库执行；本仓库通过 schema 同步约束）

```text
1. NQ 端 outbox 单测：覆盖 8 种事件类型 envelope 生成，断言 schemaVersion / sourceSystem
2. NQ 端契约测试：以 contracts/json-schema/*.schema.json 为输入，校验 outbox 产出
3. NQ 端 retry 单测：1s/5s/30s/5m/1h 退避；attempt=8 -> DEAD
4. NQ 端 idempotent 单测：相同 eventId 不应再写 outbox

注意：以上测试在 NQ 仓库内实施。本 plan 仅声明契约口径，DH 不实现，不为 NQ 写测试。
```

## 3. DH/NQ 联调测试计划（IMPLEMENT Batch 4）

测试环境（NQ test cluster + DH staging），不接实盘：

```text
联调用例 T1：NQ outbox 推 PAPER_RUN_CREATED -> DH 202 ACCEPTED -> dh_nq_feedback_events 有行
联调用例 T2：NQ outbox 重放同 eventId -> DH 202 DUPLICATE -> dh_nq_feedback_events 无新行
联调用例 T3：NQ outbox 推 UNKNOWN_TRACE -> DH 400 + errorCode = UNKNOWN_TRACE
联调用例 T4：DH 提交 DhBacktestRequest -> NQ 202 + DhBacktestRequestAccepted
联调用例 T5：NQ 完成 backtest -> outbox 推 BACKTEST_RESULT_READY ->
            DH ingest -> ExperienceEntry / PheromoneEdge 更新
联调用例 T6：DH 在 24h 窗口内重复触发同 candidate + 同参数 backtest -> DH 短路（不再次发送）
联调用例 T7：traceId / requestId / correlationId / sourceJobId 4 字段在双方日志里能完整对账
```

入口：测试环境配置项 `decisionhub.stage3.nq.endpoint`、`decisionhub.stage3.jdbc.enabled=true`。
出口：Batch 4 完成后产出 `docs/current/STAGE3_VERIFY_REPORT.md`（与 Stage2-PoC VERIFY 同体例）。

## 4. 幂等测试

```text
DH 入站（已 Stage2-PoC-B5 覆盖；Stage3 不重复）：
  - 同 eventId 第二次 -> outcome=DUPLICATE
  - JdbcNqFeedbackEventRepositoryTest race 用例已覆盖 DuplicateKeyException

DH 出站（Stage3 Batch 3 新增）：
  - 同 requestId 在 24h 内重复 build -> 业务层短路（不再走 HTTP）
  - NQ 返回 409 Conflict -> DH 视为成功，记录映射

NQ outbox（NQ 团队负责）：
  - 同 eventId 不重新入队
```

## 5. 失败重试测试

```text
NQ outbox 重试矩阵（NQ 团队执行）：
  - DH 返回 5xx / timeout -> 退避重试 1s/5s/30s/5m/1h（最多 8 次）
  - DH 返回 400 -> DEAD，不重试
  - DH 返回 2xx -> SENT，停止重试

DH 出站 outbox 矩阵（Stage3 Batch 3，DH 仓库内）：
  - NQ 返回 5xx -> 重试（与 NQ outbox 镜像）
  - NQ 返回 400 -> DEAD
  - NQ 返回 409 -> 视为成功
  - NQ 返回 2xx -> SENT
```

## 6. 边界安全测试

```text
ArchUnit（Batch 1/3）：
  10 条规则全绿；新增规则不允许破坏现有 10 条

JsonSchemaPresenceTest（已落地）：
  16 份 json-schema 文件完整；Stage3 不新增事件类型 -> 仍为 16

V3MigrationPresenceTest（已落地）：
  V3 表与列声明保持；Stage3 不修改 migration 语义

人工核对：
  - 不出现 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute 关键词
  - 不出现 /orders /trades /live 路径
  - 不出现 Python / TradingAgents 引用
  - 不出现真实 HTTP client（RestTemplate / WebClient / OkHttp / HttpURLConnection 等）
    新引用必须仅在 dh-connector.nq 模块、且伴随 ArchUnit 规则放行声明
```

## 7. 不使用真实实盘的测试策略

```text
联调环境隔离：
  - NQ 提供 test cluster，专用 DB / 专用配置 / 专用市场源；与生产物理隔离
  - DH staging 配置 decisionhub.stage3.nq.endpoint 仅指向 test cluster
  - 测试环境严禁 paper-> live 自动晋升

数据隔离：
  - 测试租户 tenantId 前缀 t-test-*；生产租户禁止参与 Stage3 联调
  - traceId / correlationId 加测试前缀 "stage3-"，便于审计过滤

执行边界：
  - 联调用例只包含 BACKTEST_REQUEST_READY / PAPER_RUN_* feedback；
    禁止任何 "下单 / 实盘 / 资金调用"
  - DhBacktestRequest 不允许指向 live universe；symbols 限定测试白名单

回滚：
  - 任意 Stage3 联调失败必须 1 小时内回滚 staging 配置到 stage2 InMemory 模式
```

## 8. 与 Stage3 其他 PLAN 文档的衔接

```text
契约细节        -> docs/current/STAGE3_CONTRACT_PLAN.md
NQ -> DH 入站   -> docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
DH -> NQ 出站   -> docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
IMPLEMENT 拆批  -> docs/current/STAGE3_WORK_ORDER.md
```
