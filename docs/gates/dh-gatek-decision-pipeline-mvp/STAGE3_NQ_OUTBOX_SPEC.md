# Stage3 NQ Feedback Outbox Spec

> Created: 2026-05-26 (Stage3-B2 NQ Feedback Outbox PLAN)
> Stage: Stage3-B2 PLAN (本文件落盘即视为 B2 PLAN completed 草案，等待 STATUS 同步)
> Parent: `docs/current/STAGE3_PLAN.md`
> Sibling: `docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md`
>          `docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md`
>          `docs/current/STAGE3_CONTRACT_PLAN.md`
>          `docs/current/STAGE3_WORK_ORDER.md`
> Scope: 本文件是 NQ 侧 feedback outbox 的最小可实施规格，仅作 NQ 团队后续工单参考。
>        DH 仓库内不实现任何 NQ outbox 代码；不修改 NQ 仓库；不实现真实联调。
>        本文件不修改任何已落地契约语义；不修改 contracts/openapi.yaml；不新增 Flyway migration；
>        不修改 NqFeedbackEventType（保持 8 种）。

---

## 一、目标与边界

### 1.1 定位

```text
NQ feedback outbox 是 NQ 侧未来向 DH 推送"事实落地后"反馈事件的最小出站机制。
NQ 仍然是唯一交易事实源、唯一风控闸门、唯一订单状态机、唯一正式回测内核。
DH 只接收反馈事件（被动 ingest），不驱动 NQ 执行交易；
DH 不在 ingest 路径触发新的 NQ 请求；DH 不在 ingest 路径写 NQ 数据。
本规格只用于后续 NQ 实施工单参考；本仓库 Stage3-B2 仅落盘 SPEC 文档，零 Java 业务代码改动。
```

### 1.2 关键不变量

```text
NQ 仓库不动                       本仓库（DH）不动 Java 业务代码
NQ 订单 / 风控 / 回测 / 实盘语义不变
DH `/api/ai/feedback/nq` 端点语义不变（Stage2-PoC-B2 已落地，Stage3-B1 已契约对齐）
DH 幂等键 dh_nq_feedback_events.event_id 唯一索引保持
NqFeedbackEventType 保持 8 种（Stage3 全期禁止扩展）
NqFeedbackEnvelope 结构保持（Stage3-B1 仅追加 description / examples）
```

### 1.3 硬禁止（NQ 端 outbox 必须遵守的红线）

```text
不从 DH 直接下单
不让 AI 绕过 NQ 风控
不让 DH 修改 NQ 订单状态
不让 DH 重写 NQ 回测核心
不把 outbox 放进 NQ 交易执行同步路径
不影响 GateJ-FREEZE 稳定运行（NQ 当前生产稳定基线）
不让 outbox 触发新订单 / 修改仓位 / 调拨资金 / 写账本 / 写主行情
不在 outbox payload 内携带交易密钥 / API token / 账号私密凭证
不读取 / 写入实盘账户事实
不允许 outbox 在 NQ 内部事务期间发送（必须事务提交后）
不允许 outbox 触发 in-flight 中间态事件（RUNNING / PROGRESS%）
```

### 1.4 价值边界声明

```text
outbox 是"事实回流通道"，不是"交易控制通道"。
NQ 仍然是交易决定方；DH 是 AI 决策能力层。
DH 不可用时，NQ 必须仍能正常处理订单 / 风控 / 实盘 / 账本；
out 出站失败只影响 AI 反馈链路，不影响 NQ 交易事实。
```

---

## 二、建议 NQ 侧模块（NQ 仓库未来落地，本仓库仅声明）

### 2.1 建议放入的模块

```text
nq-ai-contracts (新增或并入 nq-contracts)
  - DTO：FeedbackEnvelopeDto / 8 种 FeedbackPayloadDto
  - eventType 枚举：与 DH NqFeedbackEventType 一一对应（8 种，禁止扩展）
  - schema reference：以 contracts/json-schema/*.schema.json 为真理源
  - 只放 DTO 与 mapper；不放业务逻辑

nq-infra
  - JDBC outbox repository（nq_ai_feedback_outbox 表的 CRUD）
  - dead-letter repository
  - 不引用 nq-core 订单状态机；不引用 nq-risk 风控核心

nq-scheduler 或 nq-observability (二选一，按 NQ 既有架构落点)
  - outbox dispatcher：周期性 PENDING -> SENDING -> SENT|FAILED|DEAD_LETTER
  - 退避重试调度器
  - audit 对账作业（30 天）

nq-app
  - 装配 outbox dispatcher bean（@ConditionalOnProperty 兜底关闭，避免默认起服跑死信链路）
  - 注入 HTTP 客户端（mTLS / 服务账号 token）
  - dispatcher 仅写 outbox 行，不直接调用 nq-core / nq-risk

nq-api (可选，仅 admin/debug)
  - GET  /admin/ai-outbox/stats              outbox 状态分布
  - POST /admin/ai-outbox/{id}/replay        手动复发（DEAD_LETTER -> PENDING，attempt=0）
  - 必须放在 admin 命名空间；必须有内部鉴权；不可暴露到 Console 业务页面
```

### 2.2 明确不建议放入的模块（NQ 端硬边界清单）

```text
nq-core                         订单状态机；OrderStateMachine
                                outbox 不允许写入 / 不允许触发订单状态切换
nq-risk                         风控放行核心；RiskEngine.evaluate
                                outbox 不允许写入 / 不允许旁路风控
nq-backtest-kernel              正式回测内核；BacktestKernel
                                outbox 不允许触发新回测 / 不允许修改回测结果
nq-paper-engine / nq-live-engine 模拟盘 / 实盘执行器
                                outbox 不允许下单 / 不允许修改仓位
nq-ledger                       账本与审计；Ledger / Audit
                                outbox 不允许补登 / 不允许修改账本
nq-fund-manager                 资金管理；FundManager
                                outbox 不允许调拨资金
nq-marketdata-core              主行情订阅
                                outbox 不允许写入行情
nq-adapter-okx / nq-adapter-binance / nq-adapter-* 实盘适配器
                                outbox 不允许下单 / 不允许同步交易状态
nq-console-frontend             NQ Console 业务页面
                                outbox 不修改前端业务状态
```

### 2.3 模块职责矩阵

| 模块 | 写 outbox 行 | 读 outbox 行 | 发送 HTTP | 调用 nq-core/risk/ledger | 备注 |
| --- | --- | --- | --- | --- | --- |
| 8 个事件源（paper/backtest/risk/alert/recovery/stability/report） | ✅ 事务提交后 | ❌ | ❌ | ❌ | 仅落 outbox 行 |
| nq-infra outbox repository | ✅ | ✅ | ❌ | ❌ | 纯 CRUD |
| nq-scheduler outbox dispatcher | ❌ | ✅ | ✅ | ❌ | 拉 PENDING -> POST DH |
| nq-app | ❌ | ❌ | ❌ | ❌ | 仅装配 bean |
| nq-api admin | ❌ | ✅ | ❌ | ❌ | 仅读 + 手动重放 |

---

## 三、建议 NQ 侧表结构

### 3.1 主表 `nq_ai_feedback_outbox`

```sql
-- NQ 侧表，DH 仓库不创建、不迁移、不引用。
-- 列命名 snake_case；统一带 COMMENT；不存任何密钥 / token / 账号私密凭证。
CREATE TABLE IF NOT EXISTS nq_ai_feedback_outbox (
  outbox_id           bigserial   PRIMARY KEY,
  event_id            text        NOT NULL UNIQUE,
  event_type          text        NOT NULL,
  schema_version      text        NOT NULL,
  source_system       text        NOT NULL DEFAULT 'nexus-quant',
  source_job_id       text        NOT NULL,
  trace_id            text        NOT NULL,
  request_id          text        NOT NULL,
  correlation_id      text        NOT NULL,
  occurred_at         timestamptz NOT NULL,
  payload_json        jsonb       NOT NULL,
  target_url          text        NOT NULL,
  status              text        NOT NULL DEFAULT 'PENDING',
  retry_count         int         NOT NULL DEFAULT 0,
  next_retry_at       timestamptz,
  last_error_code     text,
  last_error_message  text,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  sent_at             timestamptz,

  CONSTRAINT ck_nq_ai_feedback_outbox_status
    CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'DEAD_LETTER')),
  CONSTRAINT ck_nq_ai_feedback_outbox_event_type
    CHECK (event_type IN (
      'PAPER_RUN_CREATED',
      'PAPER_RUN_STARTED',
      'PAPER_RUN_STOPPED',
      'PAPER_RUN_DAILY_REPORT_GENERATED',
      'PAPER_RUN_ALERT_RAISED',
      'PAPER_RUN_RECOVERY_EVENT_RECORDED',
      'PAPER_RUN_STABILITY_CHECK_COMPLETED',
      'BACKTEST_RESULT_READY'
    )),
  CONSTRAINT ck_nq_ai_feedback_outbox_source_system
    CHECK (source_system = 'nexus-quant'),
  CONSTRAINT ck_nq_ai_feedback_outbox_schema_version
    CHECK (schema_version ~ '^[0-9]+\.[0-9]+\.[0-9]+$')
);

-- COMMENT 必须随建表一同声明，便于 DBA 与 NQ 团队后续治理。
COMMENT ON TABLE  nq_ai_feedback_outbox                    IS 'NQ -> DH AI feedback 出站 outbox。仅承载"事实落地后"事件；不进入交易同步路径。';
COMMENT ON COLUMN nq_ai_feedback_outbox.outbox_id          IS '内部自增主键，仅 NQ 内部使用，不外发。';
COMMENT ON COLUMN nq_ai_feedback_outbox.event_id           IS '全局唯一事件 ID，推荐 UUIDv7；DH 端幂等键；同 event_id 不允许换 payload 重发。';
COMMENT ON COLUMN nq_ai_feedback_outbox.event_type         IS 'NqFeedbackEventType 8 种之一；扩展必须双方先升 MAJOR schema 版本。';
COMMENT ON COLUMN nq_ai_feedback_outbox.schema_version     IS 'envelope schema 语义化版本，例如 "1.0.0"；MAJOR 升级双方同步发版。';
COMMENT ON COLUMN nq_ai_feedback_outbox.source_system      IS '常量 "nexus-quant"；DH 端校验失败 -> 400 INVALID_SCHEMA。';
COMMENT ON COLUMN nq_ai_feedback_outbox.source_job_id      IS 'NQ 端业务 Job ID（paperRunId / backtestId / alertId 等）；DH 仅用于对账，不写入 NQ 数据。';
COMMENT ON COLUMN nq_ai_feedback_outbox.trace_id           IS '与 DH ResearchRun.trace_id 串联；DH 端必须能在 dh_research_runs 命中，否则 400 UNKNOWN_TRACE。';
COMMENT ON COLUMN nq_ai_feedback_outbox.request_id         IS '由 DH 出站请求触发时等于原 requestId；否则 NQ 端生成（UUIDv7）。';
COMMENT ON COLUMN nq_ai_feedback_outbox.correlation_id     IS '业务上下文 ID；同 correlationId 串联多个 requestId / traceId。';
COMMENT ON COLUMN nq_ai_feedback_outbox.occurred_at        IS '业务事件发生时间（NQ 端业务时间，不是发送时间）。';
COMMENT ON COLUMN nq_ai_feedback_outbox.payload_json       IS '事件 payload 原始 JSON；DH 永久留底；不允许携带任何密钥 / token / 账号凭证。';
COMMENT ON COLUMN nq_ai_feedback_outbox.target_url         IS '目标 DH endpoint，默认 https://<dh-host>/api/ai/feedback/nq；通过环境配置注入，不写硬编码。';
COMMENT ON COLUMN nq_ai_feedback_outbox.status             IS 'PENDING / SENDING / SENT / FAILED / DEAD_LETTER。';
COMMENT ON COLUMN nq_ai_feedback_outbox.retry_count        IS '当前重试次数；>= 8 进入 DEAD_LETTER。';
COMMENT ON COLUMN nq_ai_feedback_outbox.next_retry_at      IS '下次允许重试时间；调度器按此字段拉 PENDING 行。';
COMMENT ON COLUMN nq_ai_feedback_outbox.last_error_code    IS '上一次失败的 errorCode（如 UNKNOWN_TRACE / INVALID_SCHEMA / TIMEOUT / HTTP_503 等）。';
COMMENT ON COLUMN nq_ai_feedback_outbox.last_error_message IS '上一次失败的可读消息（脱敏，不含密钥）。';
COMMENT ON COLUMN nq_ai_feedback_outbox.created_at         IS '行创建时间。';
COMMENT ON COLUMN nq_ai_feedback_outbox.updated_at         IS '行最后更新时间。';
COMMENT ON COLUMN nq_ai_feedback_outbox.sent_at            IS 'DH 返回 2xx 的时间；DUPLICATE 也算成功并填充。';

CREATE INDEX IF NOT EXISTS ix_nq_ai_feedback_outbox_status_next_retry
  ON nq_ai_feedback_outbox(status, next_retry_at)
  WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX IF NOT EXISTS ix_nq_ai_feedback_outbox_event_type_created_at
  ON nq_ai_feedback_outbox(event_type, created_at);
CREATE INDEX IF NOT EXISTS ix_nq_ai_feedback_outbox_trace_id
  ON nq_ai_feedback_outbox(trace_id);
CREATE INDEX IF NOT EXISTS ix_nq_ai_feedback_outbox_correlation_id
  ON nq_ai_feedback_outbox(correlation_id);
```

### 3.2 死信表 `nq_ai_feedback_dead_letter`

```sql
CREATE TABLE IF NOT EXISTS nq_ai_feedback_dead_letter (
  dead_letter_id      bigserial   PRIMARY KEY,
  outbox_id           bigint      NOT NULL,
  event_id            text        NOT NULL UNIQUE,
  event_type          text        NOT NULL,
  schema_version      text        NOT NULL,
  source_job_id       text        NOT NULL,
  trace_id            text        NOT NULL,
  request_id          text        NOT NULL,
  correlation_id      text        NOT NULL,
  occurred_at         timestamptz NOT NULL,
  payload_json        jsonb       NOT NULL,
  retry_count         int         NOT NULL,
  last_error_code     text,
  last_error_message  text,
  moved_at            timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE  nq_ai_feedback_dead_letter             IS 'outbox 重试上限耗尽后的死信归档；30 天保留；可手动复发回 PENDING。';
COMMENT ON COLUMN nq_ai_feedback_dead_letter.outbox_id   IS '对应 outbox 行的内部 ID（不是 event_id）。';
COMMENT ON COLUMN nq_ai_feedback_dead_letter.event_id    IS '全局唯一事件 ID，便于人工排错。';
COMMENT ON COLUMN nq_ai_feedback_dead_letter.moved_at    IS '从 outbox 转入死信的时间。';

CREATE INDEX IF NOT EXISTS ix_nq_ai_feedback_dead_letter_event_type_moved_at
  ON nq_ai_feedback_dead_letter(event_type, moved_at);
```

### 3.3 表约束统一规则

```text
event_id UNIQUE                  全局唯一；同 event_id 不允许换 payload 重发
status CHECK 5 值                PENDING / SENDING / SENT / FAILED / DEAD_LETTER
event_type CHECK 8 值            与 NqFeedbackEventType 一一对应
source_system CHECK 常量          强制 "nexus-quant"
schema_version CHECK semver       正则 ^[0-9]+\.[0-9]+\.[0-9]+$
payload_json JSONB                便于 NQ 内部检索；不存密钥
timestamps TIMESTAMPTZ            occurred_at / created_at / updated_at / sent_at / next_retry_at / moved_at
COMMENT 强制                      表 + 所有列必须有 COMMENT
不存密钥 / token / 账号凭证       SPEC 评审时人工 grep 检查
不存原始市场行情 / 订单明细       不在 outbox 复制交易事实表内容
```

---

## 四、8 种事件触发点详表

每种事件必须由"事实已落地（事务提交后）"的源模块触发，禁止 in-flight 中间态。
8 种事件类型与 `dh-domain.feedback.NqFeedbackEventType` 一一对应（Stage3 全期禁止扩展）。

### 4.1 `PAPER_RUN_CREATED`

```text
NQ 来源模块         nq-paper-engine
触发时机            paper_runs 表事务提交后，run 处于 SCHEDULED 状态
payload schema      contracts/json-schema/nq-feedback-paper-run-created.schema.json
payload 来源        paper_runs 表 + StrategyCandidate 引用
eventId 生成        UUIDv7（NQ 端 outbox 写入前生成）
traceId 填充        = 原 DhBacktestRequest.traceId 或 ResearchRun.traceId（沿用 DH 上游 trace）
requestId 填充      = 触发 paper 的 DH 出站 requestId（若由 DH 触发）
correlationId 填充  = 业务 correlationId（如 candidate paper 周期 ID）
sourceJobId 填充    = paperRunId
是否允许重试        是；按 §5 重试矩阵
是否允许丢弃        否；必须最终 SENT 或 DEAD_LETTER
对交易主链路影响    无；outbox 仅"事后通知"，不阻塞 paper engine
```

### 4.2 `PAPER_RUN_STARTED`

```text
NQ 来源模块         nq-paper-engine
触发时机            run 状态 SCHEDULED -> RUNNING 之后；行情订阅建立完毕
payload schema      contracts/json-schema/nq-feedback-paper-run-started.schema.json
payload 来源        paper_runs.status + startedAt
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否
对交易主链路影响    无；不允许阻塞行情订阅
```

### 4.3 `PAPER_RUN_STOPPED`

```text
NQ 来源模块         nq-paper-engine
触发时机            run 进入 STOPPED 之后（含人工 / 异常 / 自动）
payload schema      contracts/json-schema/nq-feedback-paper-run-stopped.schema.json
payload 来源        paper_runs.status + stoppedAt + stopReason
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否；STOPPED 是关键事实，必须最终 SENT 或 DEAD_LETTER
对交易主链路影响    无；run 已停止，outbox 失败不影响后续 run
```

### 4.4 `PAPER_RUN_DAILY_REPORT_GENERATED`

```text
NQ 来源模块         nq-paper-engine 日报子模块
触发时机            日报落库后（paper_run_daily_reports 表事务提交后）
payload schema      contracts/json-schema/nq-feedback-paper-run-daily-report-generated.schema.json
payload 来源        paper_run_daily_reports 表（realizedPnl / maxDrawdown / winRate 等）
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否；DH ExperienceEntry 经验沉淀依赖此事件
对交易主链路影响    无；日报已落库，outbox 失败不影响下一日 paper run
```

### 4.5 `PAPER_RUN_ALERT_RAISED`

```text
NQ 来源模块         nq-paper-engine / nq-risk-derived-alert / nq-recovery
触发时机            alert 持久化后（含风控派生 alert）
payload schema      contracts/json-schema/nq-feedback-paper-run-alert-raised.schema.json
payload 来源        alerts 表（alertCode / alertLevel / message）
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否；CRITICAL/ERROR 级别 alert 必须送达；DH FailureCaseStore 依赖
对交易主链路影响    无；alert 已落库；outbox 仅 AI 反馈，不影响 NQ 内部告警通道
特殊约束            alert payload 不允许携带订单 ID / 账户余额；只能携带 alertCode / alertLevel /
                    message / 触发的 paperRunId / 时间戳
```

### 4.6 `PAPER_RUN_RECOVERY_EVENT_RECORDED`

```text
NQ 来源模块         nq-recovery
触发时机            恢复事件持久化后（recovery_events 表事务提交后）
payload schema      contracts/json-schema/nq-feedback-paper-run-recovery-event-recorded.schema.json
payload 来源        recovery_events 表（recoveryType / fromState / toState / actor）
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否
对交易主链路影响    无；恢复事件已落库
```

### 4.7 `PAPER_RUN_STABILITY_CHECK_COMPLETED`

```text
NQ 来源模块         nq-observability / nq-stability-check
触发时机            稳定性巡检结果持久化后
payload schema      contracts/json-schema/nq-feedback-paper-run-stability-check-completed.schema.json
payload 来源        stability_check_results 表（result / summary / checkedAt）
eventId / 追踪字段  同 §4.1
是否允许重试        是
是否允许丢弃        否
对交易主链路影响    无；巡检事件不参与订单决策
```

### 4.8 `BACKTEST_RESULT_READY`

```text
NQ 来源模块         nq-backtest-engine
触发时机            backtest result 持久化后（backtest_results 表事务提交后）
payload schema      contracts/json-schema/nq-feedback-backtest-result-ready.schema.json
payload 来源        backtest_results 表（sharpeRatio / maxDrawdown / annualReturn / winRate /
                    profitFactor / verdict / rawPayloadJson）
eventId / 追踪字段  同 §4.1；requestId 必须 = 触发回测的 DhBacktestRequest.requestId；
                    sourceJobId = backtestId（NQ 同步响应里返回过的 jobId）
是否允许重试        是
是否允许丢弃        否；DH JudgeDecision / ExperienceEntry / PheromoneEdge 经验链路依赖
对交易主链路影响    无；回测结果已落库；outbox 失败不影响下一次回测调度
特殊约束            payload.candidateId 必须 = DH 发起的 candidateId；NQ 不可代替 DH 决定 verdict 的语义，
                    DH JudgeDecision 仍是唯一最终出口
```

### 4.9 8 种事件统一约束

```text
所有事件触发点共同满足：
  1. 必须在源表事务提交后触发（不允许事务期间发送）
  2. 必须仅基于"已落地事实"（不允许 in-flight / RUNNING / PROGRESS%）
  3. 必须由 NQ 端独立生成 eventId，且 outbox 行写入前持久化
  4. 必须沿用 DH 上游 traceId / requestId / correlationId（若由 DH 触发）；否则 NQ 端生成
  5. 必须按 schema 严格序列化；payload 不允许携带密钥 / token / 账号凭证
  6. 必须保证幂等：同 event_id 不允许换 payload 重发（DB UNIQUE 约束 + 应用层检查）
  7. 必须事件持久化优先：先写 outbox 行（PENDING），再由 dispatcher 异步发送

禁止扩展事件类型：
  - 不新增 ORDER_* / FILL_* / POSITION_* / LIVE_* 等交易事实事件
  - 不新增 RISK_BYPASS / FORCE_EXECUTE / PLACE_ORDER / SUBMIT_ORDER 等危险语义事件
  - 任何新增必须先在 DH 侧 dh-domain.feedback.NqFeedbackEventType 与 contracts/json-schema
    双源添加，且通过 MAJOR schemaVersion 升级，由双方共同评审
```

---

## 五、retry / dead-letter / audit 规则

### 5.1 状态机

```text
PENDING       outbox 行已写入，等待 dispatcher 拉取
SENDING       dispatcher 已拉取并占用，正在发送 HTTP（短暂中间态，dispatcher 崩溃后由扫描器超时回 PENDING）
SENT          DH 返回 2xx（含 202 outcome=DUPLICATE），sent_at 已填充，终态成功
FAILED        DH 返回非永久错误（5xx / timeout / 429），retry_count++，next_retry_at 退避
DEAD_LETTER   retry_count >= 8 或 DH 返回永久错误（400 / 401 / 403），转入死信表，终态失败
```

### 5.2 状态迁移规则

```text
PENDING -> SENDING       dispatcher 拉行成功（UPDATE ... WHERE status='PENDING' AND next_retry_at <= now()）
SENDING -> SENT          DH 2xx，写 sent_at
SENDING -> FAILED        DH 5xx / timeout / 429，retry_count++，写 next_retry_at（见 §5.3）
SENDING -> DEAD_LETTER   DH 400 / 401 / 403（永久错误），插入 nq_ai_feedback_dead_letter，
                         outbox 行 status = DEAD_LETTER（行保留，便于查询）
FAILED  -> SENDING       下次调度循环再次拉取
FAILED  -> DEAD_LETTER   retry_count >= 8，转入死信
SENDING -> PENDING       dispatcher 崩溃 / 超时（SENDING 持续 > 5 分钟），由 sweeper 任务回退
DEAD_LETTER -> PENDING   仅允许通过 admin API 手动复发（retry_count = 0, status = PENDING, next_retry_at = now()）
```

### 5.3 退避策略

```text
重试矩阵（指数退避 + 抖动 ±10%）：
  attempt = 0  立即发送（PENDING 即可被拉取）
  attempt = 1  next_retry_at = now() + 1s
  attempt = 2  + 5s
  attempt = 3  + 30s
  attempt = 4  + 5min
  attempt = 5  + 30min
  attempt = 6  + 1h
  attempt = 7  + 6h
  attempt = 8  -> DEAD_LETTER（停止重试）

抖动：在 next_retry_at 加 ±10% 随机偏移，避免雪崩。

429 Too Many Requests：
  retry_count 不计入死信上限（视为限速，无限退避到 attempt 上限）。
  next_retry_at 按本矩阵计算；若 DH 在 Retry-After header 中给出更长时间则采纳更长者。
```

### 5.4 失败原因记录

```text
last_error_code 取值统一（NQ outbox 内部命名，不外发）：
  - HTTP_400        DH 400 + errorCode 之一（UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE）
  - HTTP_401        认证失败
  - HTTP_403        授权失败
  - HTTP_429        限速
  - HTTP_5xx        DH 服务端错误（按具体 5xx 细分）
  - TIMEOUT         HTTP 客户端超时（默认 10s connect + 30s read）
  - NETWORK         连接拒绝 / DNS 失败 / SSL 失败
  - PAYLOAD_BUILD   outbox dispatcher 序列化失败（理论上发送前已校验，仅兜底）

last_error_message：
  - 限长 1024 字符；脱敏；不允许包含密钥 / token / 账号凭证 / URL 查询参数
  - 仅记录 DH 返回的 errorCode + 截断后的 message，或本地异常类名 + 截断 message
```

### 5.5 dead-letter 与手动复发

```text
进入条件：
  1. retry_count >= 8（暂态错误重试上限）
  2. DH 返回 400 / 401 / 403（永久错误）
  3. 序列化失败（PAYLOAD_BUILD）且重新构造仍失败 3 次

死信归档：
  - 写入 nq_ai_feedback_dead_letter（详见 §3.2）
  - 原 outbox 行 status 切 DEAD_LETTER，retry_count 保留，last_error_* 保留（便于查询）
  - 30 天后归档 / 物理清理；30 天内可手动复发

手动复发流程（NQ admin 操作，本仓库不实现）：
  1. 通过 nq-api admin `POST /admin/ai-outbox/{id}/replay`（仅 admin 鉴权）
  2. UPDATE nq_ai_feedback_outbox SET status='PENDING', retry_count=0, next_retry_at=now(), updated_at=now() WHERE outbox_id = :id
  3. 写入操作审计（actor / reason / 时间戳）
  4. dispatcher 在下次调度循环正常拉取

告警渠道（NQ 自定）：
  - DEAD_LETTER 写入即触发告警（推荐：每 1000 条 / 每小时聚合）
  - 同 traceId 30 天内死信 >= 3 条 -> 触发对账人工介入告警
  - SPEC 不强制告警通道；NQ 团队按既有 oncall 体系实施
```

### 5.6 审计与对账

```text
对账作业（NQ 端每日定时跑）：
  1. 计数 outbox 当日新增行：sum(status='PENDING') + sum(status='SENT') + sum(status='DEAD_LETTER')
  2. 与事件源表（paper_runs / backtest_results / alerts / 等）当日新增行做差
  3. 任一差值 != 0 写入 audit 报表，触发 NQ DBA 排查

DH/NQ 双向对账（每日跑）：
  4. NQ outbox sent.event_id ⊇ DH dh_nq_feedback_events.event_id（NQ 已成功的，DH 必须有记录）
  5. 反向：DH dh_nq_feedback_events.event_id ⊆ NQ outbox sent OR dead_letter（DH 有的，NQ 必须能追溯）
  6. 差异写入双向 audit 报表，触发双方 oncall

保留期：
  - nq_ai_feedback_outbox 30 天（SENT 行）；DEAD_LETTER 行同 30 天，逻辑归档可延长 90 天
  - nq_ai_feedback_dead_letter 30 天主表 + 90 天冷备份
  - DH dh_nq_feedback_events 无 TTL（留 Stage3-FREEZE 后治理）

不允许审计内容：
  - 不写入 NQ 订单 / 成交 / 仓位 / 账本明细
  - 不写入用户身份 / 联系方式
  - audit 报表不外发；仅供 NQ DBA + DH 集成 oncall 内部使用
```

### 5.7 与交易主链路解耦

```text
retry 不阻塞 NQ 交易主链路：
  - dispatcher 跑在独立线程池 / 独立调度器；不抢占 nq-core / nq-risk / nq-paper-engine 线程
  - dispatcher 连接池独立；不复用交易主链路 HTTP / DB 连接池
  - DH 不可用时，NQ 主链路必须仍能下单 / 风控 / 实盘 / 账本写入
  - outbox 表 IO 必须与 nq-core 主表分库或至少分独立 connection；不允许长事务跨链路

DH 不可用时 NQ 行为：
  - PENDING 行堆积；按退避矩阵继续重试（最长 attempt=8 ~ 6h+1h+30min+...）
  - 不影响事件源模块继续写新 outbox 行
  - 不允许"DH 死了就跳过 outbox 写入"；事件源必须始终落 outbox 行
  - 长时间 DH 不可用 -> NQ DBA / oncall 介入；可临时通过 admin API 暂停 dispatcher（不允许删除 outbox 行）
```

---

## 六、幂等与追踪规则

### 6.1 五字段语义

```text
eventId         NQ outbox 行 ID 之外的"事件唯一标识"，全局唯一（推荐 UUIDv7）
                生命周期：单条 feedback event
                生成方：NQ outbox 写入前生成
                使用方：DH 幂等键（dh_nq_feedback_events.event_id 唯一索引）
                禁止：换 payload 重发同 eventId；用 eventId 当 requestId / sourceJobId

traceId         32 位 hex（与 OpenTelemetry trace id 对齐）
                生命周期：跨 DH ResearchRun + NQ Job
                生成方：DH ResearchRun 创建时生成
                传递：DH -> NQ HTTP header X-Trace-Id + body.traceId 双通道；
                      NQ feedback envelope.traceId 原样回传
                校验：feedback ingest 收到的 traceId 必须能在 dh_research_runs.trace_id 命中，
                      否则 DH 返回 400 UNKNOWN_TRACE

requestId       UUIDv7
                生命周期：单次 DH -> NQ 请求（如一次 backtest request）
                生成方：DH 出站请求方
                传递：DH -> NQ body.requestId；NQ feedback envelope.requestId 必带原值
                校验：feedback envelope.requestId 必须 = 原 DhBacktestRequest.requestId

correlationId   UUIDv7
                生命周期：业务上下文（如一个 candidate 的整个 paper run / backtest 周期）
                生成方：DH 业务上下文统一分配
                传递：DH/NQ 双向必带；同 correlationId 串联多个 requestId / traceId
                校验：feedback envelope.correlationId 与原 DhBacktestRequest.correlationId 必须一致

sourceJobId     NQ 内部 Job 标识
                生命周期：NQ 端任务（paperRunId / backtestId / alertId 等）
                生成方：NQ 端
                传递：feedback envelope.sourceJobId 必填
                校验：DH 仅用于对账与排错，不参与幂等判定，不参与业务匹配
```

### 6.2 不可混用

```text
五字段语义严格区分，禁止混用：
  - 禁止用 eventId 充当 requestId / sourceJobId / traceId
  - 禁止用 sourceJobId 充当 eventId 或 requestId（sourceJobId 在 NQ 端可能复用）
  - 禁止用 traceId 充当 correlationId（traceId 是 ResearchRun 维度，correlationId 是业务周期维度）
  - 禁止把 5 字段任一塞入 payload_json（必须放 envelope 顶层）
  - 禁止 5 字段任一为空字符串 / null / 自由文本占位符（如 "unknown" / "TBD"）

校验顺序（DH 端 ingest 已落地，Stage2-PoC-B2）：
  1. envelope schema 校验（含 5 字段非空）
  2. sourceSystem == "nexus-quant"
  3. eventType 在 8 种枚举内
  4. schemaVersion semver 且本地支持
  5. eventId 幂等查重
  6. traceId 反查 dh_research_runs

任一失败 -> 400 + 对应 errorCode（NQ outbox 转 DEAD_LETTER，停止重试）
```

### 6.3 时序约束

```text
NQ 端事件源 -> outbox 行写入：
  - 事务提交后立刻写 outbox（同一事务或紧接的独立事务）
  - eventId 生成必须在 outbox 行写入前（不允许 dispatcher 阶段才生成）
  - occurred_at = 业务事件发生时间（不是 outbox 写入时间）

outbox 行 -> DH 接收：
  - dispatcher 拉取间隔建议 1s（PENDING 扫描）
  - HTTP 超时建议 connect=10s read=30s（DH 端 ingest 已知是异步入队，远低于 30s）
  - receivedAt 由 DH 端在 ingest 时回填，NQ 端不必填写

时钟漂移：
  - NQ / DH 双方依赖 NTP；single source of truth = occurred_at（NQ 端）
  - DH 端不基于 receivedAt 做业务判断；只用于审计
```

---

## 七、HTTP 交互规则

### 7.1 端点与请求

```text
目标 endpoint：    POST /api/ai/feedback/nq                (Stage2-PoC-B2 已落地)
authentication：   由 NQ 侧规定（建议 mTLS + 服务账号 token；本仓库不强制）
content-type：     application/json; charset=utf-8
HTTP headers：
  X-Trace-Id      = envelope.traceId（建议双通道）
  X-Request-Id    = envelope.requestId（可选）
  Idempotency-Key = envelope.eventId（可选；DH 端已用 body.eventId 幂等，header 仅备份）
  User-Agent      = "nq-outbox-dispatcher/<version>"

body：             NqFeedbackEnvelope（contracts/json-schema/nq-feedback-envelope.schema.json）
                   - 10 个 required 字段：eventId / eventType / occurredAt / sourceSystem /
                     sourceJobId / traceId / requestId / correlationId / schemaVersion / payloadJson
                   - additionalProperties = false
                   - sourceSystem const "nexus-quant"
                   - schemaVersion semver
```

### 7.2 期望响应

```text
202 Accepted + NqFeedbackAcceptedResponse
  body：{ eventId, status: "ACCEPTED" | "ACCEPTED_DUPLICATE", outcome: "ACCEPTED" | "DUPLICATE",
          traceId, correlationId }
  含义：DH 已接收并入队（首次）或重放命中幂等（重复）
  outbox 行为：UPDATE status = SENT, sent_at = now()

400 Bad Request + NqFeedbackErrorResponse
  body：{ error: "INVALID_REQUEST", errorCode, message, eventId?, traceId?, correlationId? }
  errorCode 之一：
    UNKNOWN_EVENT_TYPE   envelope.eventType 不在 8 种枚举内
    INVALID_SCHEMA       envelope / payload 不符合 schemaVersion 声明的契约
    UNKNOWN_TRACE        envelope.traceId 在 DH dh_research_runs 中找不到
  含义：契约错误（NQ 端发送了 DH 不认识的事件）
  outbox 行为：永久失败 -> DEAD_LETTER（不重试）

401 Unauthorized / 403 Forbidden
  含义：认证 / 授权失败
  outbox 行为：永久失败 -> DEAD_LETTER（不重试）；触发 NQ oncall（可能是 token 过期）

429 Too Many Requests
  含义：DH 限速（当前未实现，预留）
  outbox 行为：按退避矩阵重试；retry_count 不计入死信上限；遵守 Retry-After header

5xx Server Error / 网络超时 / 连接失败
  含义：DH 临时不可用（如 DB 不通、Pod 重启）
  outbox 行为：FAILED -> 按退避矩阵重试

任何其他状态码（1xx / 3xx / 200-201 / 203-208 / 4xx 非上列）：
  含义：协议违规（DH 端 ingest 不应返回这些）
  outbox 行为：当作 5xx 处理（FAILED + 重试）；同时触发告警
```

### 7.3 dispatcher 安全约束

```text
NQ outbox dispatcher 行为白名单：
  ✅ 读取 PENDING / FAILED outbox 行
  ✅ 写 outbox 行的 status / retry_count / next_retry_at / last_error_* / sent_at / updated_at
  ✅ HTTP POST 到配置的 target_url（来自环境变量 / 配置中心，不写死）
  ✅ 写 audit 日志
  ✅ 触发 dead-letter 转储
  ✅ 触发告警（通过既有 NQ 告警通道）

dispatcher 行为黑名单（硬禁止）：
  ❌ 不调用 nq-core 订单状态机
  ❌ 不调用 nq-risk 风控
  ❌ 不调用 nq-backtest-kernel / nq-paper-engine / nq-live-engine
  ❌ 不调用 nq-ledger / nq-fund-manager
  ❌ 不调用 nq-adapter-okx / nq-adapter-binance / nq-adapter-*
  ❌ 不读取 / 写入交易事实表（orders / fills / positions / live_runs / ledger 等）
  ❌ 不读取 token / API key / 账号私密凭证（必须从环境注入，使用即丢，不存内存常驻）
  ❌ 不修改 outbox payload（payload_json 严格只读；NQ 端事件源写入后不允许 dispatcher 重写）
  ❌ 不基于 DH 返回内容反向修改 NQ 业务实体
  ❌ 不允许调用 NQ Console 业务页面 API
```

### 7.4 envelope 冻结要求

```text
NQ outbox dispatcher 只发送"已冻结的 feedback envelope"：
  - envelope 在事件源模块写 outbox 行时即冻结（payload_json 写入即不可变）
  - dispatcher 阶段不允许重新序列化（避免 schema 漂移）
  - 若 schema_version 需要升级，必须通过新写一行（新 eventId）的方式发布，不允许原地改写

例外（仅允许的"非 envelope 内容"修改）：
  - status / retry_count / next_retry_at / last_error_* / sent_at / updated_at 可由 dispatcher 修改
  - target_url 仅允许从 PENDING 状态改为另一 PENDING URL（极少数迁移场景；需 admin 审计）
```

---

## 八、NQ 后续实施 Batch 建议

NQ 仓库后续工单（本仓库 Stage3-B2 不实施；NQ 团队按此 SPEC 拆批落地）：

### 8.1 Batch NQ-1：Contract + DB migration

```text
目标
  - 在 NQ 仓库内落 envelope / payload DTO（nq-ai-contracts）
  - 在 NQ DB 落 nq_ai_feedback_outbox / nq_ai_feedback_dead_letter 表
  - 不发送任何事件；仅契约 + 表结构

允许改动
  - nq-ai-contracts 新建模块（或并入 nq-contracts）
    * FeedbackEnvelopeDto + 8 种 FeedbackPayloadDto
    * eventType / status 枚举与 schema_version 常量
  - nq-infra 新建 Flyway migration
    * V?__ai_feedback_outbox.sql（按 §3 完整建表 + 索引 + COMMENT）
  - nq-app 配置项预留（不装配 dispatcher bean）
    * decision-hub.feedback.endpoint = https://<dh-host>/api/ai/feedback/nq
    * decision-hub.feedback.enabled = false（默认关闭）
    * decision-hub.feedback.token.source = env://NQ_DH_FEEDBACK_TOKEN

禁止改动
  - 不新建 dispatcher
  - 不修改 nq-core / nq-risk / nq-backtest-kernel / nq-paper-engine
  - 不修改 nq-ledger / nq-fund-manager
  - 不修改 NQ Console 业务页面
  - 不接 DH 真实 endpoint
  - 不在 nq-adapter-* 引用本规格

文件清单
  - nq-ai-contracts/src/main/java/...feedback/FeedbackEnvelopeDto.java
  - nq-ai-contracts/src/main/java/...feedback/payload/*PayloadDto.java        (8 个)
  - nq-ai-contracts/src/main/java/...feedback/NqFeedbackEventTypeNq.java       (enum, NQ 内部命名)
  - nq-infra/src/main/resources/db/migration/V?__ai_feedback_outbox.sql
  - nq-app/src/main/resources/application.yml                                  (新增配置占位)

验收标准
  - NQ mvn test 全绿
  - Flyway migration 在 NQ CI 跑通
  - DTO 与 DH contracts/json-schema/*.schema.json 字段一致（人工对照 + 单测断言）
  - 不影响 NQ 既有交易主链路
  - 不发送任何 HTTP 请求
```

### 8.2 Batch NQ-2：Outbox repository + dispatcher fake

```text
目标
  - 落 nq_ai_feedback_outbox / nq_ai_feedback_dead_letter 的 JDBC repository
  - 落 FakeOutboxDispatcher（不发 HTTP，只切状态机）
  - 落单测覆盖 5 状态机迁移

允许改动
  - nq-infra 新增 JDBC repository（OutboxRepository / DeadLetterRepository）
  - nq-scheduler 或 nq-observability 新增 FakeOutboxDispatcher
    * @ConditionalOnProperty(decision-hub.feedback.enabled=false)：装配 Fake；true 时不装配
    * Fake 行为：从 PENDING 拉行，直接切 SENT（不发 HTTP）
  - 新增单测：状态机 5 路径 / 重试矩阵 / 死信转储 / 抖动

禁止改动
  - 不连 DH 真实 endpoint
  - 不发 HTTP
  - 不修改 nq-core / 风控 / 回测核心
  - 不在事件源模块写 outbox 行（Batch NQ-3 才允许）

文件清单
  - nq-infra/src/main/java/...outbox/OutboxRepository.java
  - nq-infra/src/main/java/...outbox/DeadLetterRepository.java
  - nq-infra/src/main/java/...outbox/jdbc/JdbcOutboxRepository.java
  - nq-infra/src/main/java/...outbox/jdbc/JdbcDeadLetterRepository.java
  - nq-scheduler/.../FakeOutboxDispatcher.java
  - nq-scheduler/.../OutboxStateMachine.java                                   (状态迁移与退避)
  - nq-scheduler/src/test/.../FakeOutboxDispatcherTest.java
  - nq-scheduler/src/test/.../OutboxStateMachineTest.java

验收标准
  - NQ mvn test 全绿；新增单测覆盖 5 状态 + 8 attempt 退避矩阵
  - JDBC repository 通过 NQ CI Docker 测试
  - decision-hub.feedback.enabled=false 默认 profile FakeDispatcher 兜底
  - 不影响 NQ 既有交易主链路
  - 不发任何 HTTP
```

### 8.3 Batch NQ-3：Paper Trading / Backtest 事件写入 outbox

```text
目标
  - 在 8 个事件源模块（按 §4 触发点）的"事实落地"事务提交后写 outbox 行
  - 不发 HTTP（仍 FakeOutboxDispatcher 兜底）
  - 验证 8 种事件 envelope 序列化正确

允许改动
  - nq-paper-engine 新增 OutboxWriter 调用点（事务提交后）
    * PAPER_RUN_CREATED / STARTED / STOPPED / DAILY_REPORT / RECOVERY / STABILITY_CHECK
  - nq-backtest-engine 新增 OutboxWriter 调用点
    * BACKTEST_RESULT_READY
  - nq-risk-derived-alert / nq-recovery 新增 OutboxWriter 调用点（若 alert 由这里触发）
    * PAPER_RUN_ALERT_RAISED
  - 新增 EventBuilder 工厂（按 8 种事件类型构造 envelope + payload）
  - 新增集成测试：8 种事件源 -> outbox 行落库

禁止改动
  - 不修改 nq-core 订单状态机
  - 不修改 nq-risk 风控核心
  - 不修改 nq-backtest-kernel 内核
  - 不修改 nq-ledger / nq-fund-manager
  - 不允许在事务期间发送（必须事务提交后）
  - 不允许在 in-flight 状态触发
  - 不允许 OutboxWriter 反向调用业务实体

文件清单
  - nq-paper-engine/.../outbox/PaperRunOutboxWriter.java                       (6 种事件)
  - nq-backtest-engine/.../outbox/BacktestResultOutboxWriter.java              (1 种)
  - nq-risk-derived-alert/.../outbox/AlertOutboxWriter.java                    (1 种)
  - nq-ai-contracts/.../EventBuilder.java                                      (统一构造工厂)
  - 各源模块 src/test/.../*OutboxWriterIT.java

验收标准
  - NQ mvn test 全绿
  - 集成测试覆盖 8 种事件 -> outbox 行存在 + payload 序列化合规
  - 与 DH contracts/json-schema/*.schema.json 字段一致（CI 跑 schema 校验）
  - 不影响 NQ 既有交易主链路
  - 仍走 FakeDispatcher，不发任何 HTTP
```

### 8.4 Batch NQ-4：Retry / dead-letter / audit

```text
目标
  - 落真实 OutboxDispatcher（@ConditionalOnProperty(decision-hub.feedback.enabled=true)）
  - 真实 HTTP 客户端（OkHttp / WebClient，二选一；带 mTLS / token）
  - 退避矩阵实测；死信归档；audit 对账作业

允许改动
  - nq-scheduler 新增 RealOutboxDispatcher
  - nq-scheduler 新增 RetryBackoffPolicy / DeadLetterMover / AuditReconcileJob
  - nq-app 配置真实 HTTP 客户端 bean（仅在 enabled=true 时装配）
  - 新增 ArchUnit 规则：outbox 模块禁引用 nq-core / nq-risk / nq-ledger 等核心包

禁止改动
  - 不修改 nq-core / nq-risk / nq-backtest-kernel / 实盘适配器
  - 不允许 dispatcher 调用业务实体
  - 不允许在生产 profile 默认开启（enabled=false 保持默认）
  - 不允许把 token 写入数据库 / 日志

文件清单
  - nq-scheduler/.../RealOutboxDispatcher.java
  - nq-scheduler/.../RetryBackoffPolicy.java
  - nq-scheduler/.../DeadLetterMover.java
  - nq-scheduler/.../AuditReconcileJob.java
  - nq-scheduler/src/test/.../RetryBackoffPolicyTest.java
  - nq-scheduler/src/test/.../DeadLetterMoverTest.java
  - nq-scheduler/src/test/.../OutboxArchitectureTest.java                      (ArchUnit)

验收标准
  - NQ mvn test 全绿
  - 退避矩阵单测覆盖 attempt=0..8
  - dead-letter 转储路径单测覆盖
  - audit 对账作业单测覆盖（mock outbox + dh 端 event 集合差异）
  - ArchUnit 规则全绿
  - 默认 profile（enabled=false）仍走 FakeDispatcher
  - 不影响 NQ 既有交易主链路
```

### 8.5 Batch NQ-5：DH/NQ contract test

```text
目标
  - 联调测试环境：NQ test cluster + DH staging
  - 7 个端到端联调用例（T1-T7，详见 docs/current/STAGE3_TEST_PLAN.md §3 与
    docs/current/STAGE3_WORK_ORDER.md §4.2）
  - 不接实盘；不真实下单；联调用例只走 NQ test cluster + DH staging

允许改动
  - NQ 仓库新增 contract test profile（与 prod profile 隔离）
  - DH 仓库新增 @EnabledIfEnvironmentVariable(named="ENABLED_STAGE3") 联调用例
  - NQ test cluster 配置 decision-hub.feedback.enabled=true 指向 DH staging
  - DH staging 配置 decisionhub.stage2.jdbc.enabled=true（CI Docker 跑通）

禁止改动
  - 不接实盘
  - 不真实下单
  - 不绕过 NQ 风控
  - 不修改 NQ 回测核心
  - 不用 prod tenant 做联调
  - 不修改 contracts / migration / OpenAPI 语义

文件清单（NQ 仓库内）
  - nq-contract-test/.../Stage3InboundFeedbackEndToEndTest.java
  - nq-contract-test/.../Stage3OutboundBacktestEndToEndTest.java
  - nq-contract-test/.../Stage3TraceCorrelationTest.java

文件清单（DH 仓库内，本 Batch 由 DH Stage3-B4 实施）
  - dh-app/src/test/.../Stage3InboundFeedbackEndToEndTest.java
  - dh-app/src/test/.../Stage3OutboundBacktestEndToEndTest.java
  - dh-app/src/test/.../Stage3TraceCorrelationTest.java

验收标准
  - 7 个联调用例全绿（T1-T7）
  - 默认 profile mvn test 仍全绿（不含联调用例）
  - PostgresContainerSmokeTest 在 CI Docker 通过
  - ArchUnit 全绿
  - 联调失败 -> 回到对应 Batch PLAN（禁止跳跃 / 硬抹平）
```

---

## 九、风险与防护

### 9.1 不影响 GateJ-FREEZE

```text
GateJ-FREEZE 是 NQ 当前生产稳定基线；outbox 不允许破坏：
  - outbox 模块不引用 nq-core / nq-risk / nq-backtest-kernel / nq-ledger 内部类
  - outbox 写表 / dispatcher 跑独立线程池 / 独立连接池
  - outbox 失败不抛异常到事件源模块（异步落库；事件源 commit 后不感知 outbox 后续）
  - outbox 重试不占用 nq-core 调度槽 / 不占用 nq-risk 时间窗
  - 任何 outbox 故障 -> nq-core 主链路日志只见"AI feedback delivery deferred"级别 INFO，
    不出现 ERROR / 告警 / 重试堆积反压到主链路

防护手段：
  - ArchUnit 规则（Batch NQ-4）守门
  - 独立 Spring Bean / @Async / 独立 ExecutorService
  - 独立 DataSource 或至少独立 connection pool（避免长事务跨链路）
```

### 9.2 不进入交易同步链路

```text
事件源模块 -> outbox 行写入：
  - 事务提交后立即写（同事务或紧接独立事务）
  - 不允许 OutboxWriter 反向影响业务实体
  - 不允许 OutboxWriter 调用 nq-risk / nq-core / nq-backtest-kernel
  - 不允许 OutboxWriter 触发新订单 / 新回测 / 新风控决策

dispatcher 行为：
  - 见 §7.3 黑名单清单
  - dispatcher 异步运行；事件源同步路径不依赖 dispatcher 完成
```

### 9.3 不阻塞订单、风控、账本、回测

```text
失败隔离矩阵：

  outbox 写表失败            -> 事件源模块仅 WARN 日志，不阻塞主链路提交
                                （事件已落业务表；outbox 缺失由对账作业兜底人工补登）
  dispatcher 拉行失败        -> 仅影响 outbox 自身；事件源继续正常写新行
  HTTP 发送失败              -> 仅影响该行 status；不影响其他 outbox 行
  DH 长时间不可用            -> outbox PENDING 堆积；NQ 主链路完全不受影响；
                                NQ DBA / oncall 介入；可临时停 dispatcher（不删 outbox 行）
  outbox 表 IO 慢            -> 独立 connection pool 防止反压；事件源使用专用短事务

主链路保护：
  - nq-core / nq-risk / nq-backtest-kernel / nq-paper-engine / nq-live-engine / nq-ledger /
    nq-fund-manager / nq-adapter-* 任一模块完全不依赖 outbox 行存在与否
  - 移除 outbox 模块（极端情况下）应当不影响主链路功能（仅丢失 AI 反馈）
```

### 9.4 DH 不可用时 NQ 降级策略

```text
DH 完全不可用（持续 > 1 小时）：
  1. PENDING 堆积按退避矩阵自然累积（不会无限重试，attempt=8 即转 DEAD_LETTER）
  2. NQ oncall 通过既有告警通道感知（dispatcher 失败率 > 阈值）
  3. 临时降级：NQ admin API `POST /admin/ai-outbox/pause` 暂停 dispatcher
     * outbox 行仍正常写入（不丢事件源事实）
     * dispatcher 拉行循环停止；不再产生 HTTP 请求
  4. 恢复后 `POST /admin/ai-outbox/resume`；积压行按退避矩阵继续发送

DH 半可用（间歇 5xx / 高延迟）：
  - dispatcher 按退避矩阵自然退让
  - 抖动避免雪崩
  - 不切到 NQ 端内部缓存（DH 失败 != NQ 数据丢失）

NQ 端必须保证：
  - 任何 outbox 状态下，NQ 主链路下单 / 风控 / 实盘 / 账本写入持续可用
  - 任何 outbox 状态下，新事件源仍能写新 outbox 行（不阻塞业务表 commit）
```

### 9.5 事件重复发送时 DH 幂等处理

```text
DH 幂等机制（Stage2-PoC-B5 已落地，本 SPEC 不变）：
  - dh_nq_feedback_events.event_id 唯一索引
  - 重放命中 -> 202 outcome=DUPLICATE
  - 重放命中不重新走经验沉淀流程

NQ outbox 重复发送可能场景：
  1. dispatcher 拉行后 HTTP 已发送但 ack 未收到 -> outbox 行仍是 SENDING -> 超时回 PENDING -> 再次发送
  2. 手动复发死信
  3. 调度器实例重启 / SENDING 行扫描器回退

防护：
  - 全部走 eventId 幂等（不依赖请求超时窗口）
  - dispatcher SENDING 行超时建议 5 分钟（与 DH ingest 异步入队时间数量级一致）
  - admin 手动复发记审计（actor / reason / 时间戳）
  - SPEC 评审时确认：DH 幂等行为对"NQ 视为成功 vs DH 视为重复"无歧义
    （outcome=DUPLICATE 对 NQ 视为 SENT，对 DH 视为重复，双方语义一致）
```

### 9.6 payload schema 演进策略

```text
schema_version semver 规则：
  PATCH（1.0.X）         向后兼容；无须双方同步发版；DH 自动接受
  MINOR（1.X.0）         向后兼容（仅新增 optional 字段）；无须双方同步发版
  MAJOR（X.0.0）         破坏式升级；必须双方同步发版

MAJOR 升级流程（NQ + DH 双方协同）：
  1. DH 侧先升级（兼容新旧 MAJOR）
     * dh-domain / contracts/json-schema / contracts/openapi.yaml 同步升 MAJOR
     * DH ingest 支持新旧 MAJOR；旧 MAJOR 标 deprecated
  2. NQ 侧再升级（outbox 切到新 MAJOR）
     * 新事件按新 MAJOR 写 outbox
     * 历史 PENDING 行仍按原 MAJOR 发送（不允许重写 payload）
  3. 全量切完后 DH 侧拆除旧 MAJOR 支持（提前在 contracts deprecated 标记 6 周以上）

DH 端遇到本地未支持的 MAJOR：
  - 返回 400 + INVALID_SCHEMA
  - outbox 转 DEAD_LETTER
  - 触发双方对账 + 版本协调

不允许：
  - NQ 单边升 MAJOR 而 DH 未跟进
  - 在同一 envelope 内混用多个 MAJOR
  - 通过 PATCH/MINOR 偷偷做破坏式变更
```

---

## 十、验收标准

### 10.1 本轮（Stage3-B2 NQ Feedback Outbox PLAN）完成标准

```text
本仓库 Stage3-B2 仅落 SPEC 文档；零 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration 修改。

文档完成度：
  ✅ docs/current/STAGE3_NQ_OUTBOX_SPEC.md 存在（本文件）
  ✅ 8 种事件触发点完整（PAPER_RUN_* 7 种 + BACKTEST_RESULT_READY 1 种，与 NqFeedbackEventType 一一对应）
  ✅ outbox 主表 + 死信表表结构完整（建表 SQL + COMMENT + 索引）
  ✅ retry / dead-letter / audit 规则完整（5 状态机 + 8 attempt 退避矩阵 + 30 天保留 + 双向对账）
  ✅ 幂等与追踪规则完整（5 字段语义 + 不可混用 + 时序约束）
  ✅ HTTP 交互规则完整（202 / 400 / 401 / 403 / 429 / 5xx 全覆盖）
  ✅ NQ 后续 5 个 Batch（NQ-1 ~ NQ-5）目标 / 允许 / 禁止 / 文件清单 / 验收标准完整
  ✅ 风险与防护完整（GateJ-FREEZE / 主链路解耦 / 降级 / 幂等 / schema 演进）
  ✅ 硬边界明确（不下单 / 不绕风控 / 不重写回测 / 不修改订单状态 / 不影响 NQ 主链路）

文档同步：
  ✅ README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md / TESTING.md
     同步到 "Stage3-B2 NQ Feedback Outbox PLAN completed / Next: Stage3-B3 DH Backtest Request Adapter PLAN"

代码与构建：
  ✅ 无 Java 业务代码修改
  ✅ 无 contracts/openapi.yaml 修改
  ✅ 无 contracts/json-schema/*.schema.json 修改
  ✅ 无 Flyway migration 新增 / 修改
  ✅ 无 NQ 仓库改动
  ✅ mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  ✅ ArchUnit 10/10 保持
  ✅ Stage1ClosedLoopTest / Stage2ClosedLoopTest 保持全绿
  ✅ 151 tests（Stage3-B1 基线）保持，未下降
```

### 10.2 NQ 侧后续实施验收口径（NQ 团队后续按 §8 Batch 落地）

```text
NQ 仓库整体验收口径（NQ 团队跑）：
  - NQ-1 ~ NQ-5 各 Batch 独立通过 NQ mvn test
  - 不影响 NQ 既有交易主链路（GateJ-FREEZE 通过）
  - 默认 profile（decision-hub.feedback.enabled=false）走 FakeDispatcher
  - prod 启用前必须先在 staging 走 §8.5 Batch NQ-5 联调全绿
  - ArchUnit 规则全绿（outbox 模块禁引用核心包）

DH/NQ 双向验收口径（NQ-5 联调后跑）：
  - 7 个端到端用例 T1-T7 全绿（详见 docs/current/STAGE3_TEST_PLAN.md §3）
  - 4 字段（traceId / requestId / correlationId / sourceJobId）端到端对账无差异
  - DH dh_nq_feedback_events.event_id ⊆ NQ outbox sent + dead_letter

任何 Batch 验收失败 -> 回该 Batch PLAN，禁止跳跃或硬抹平。
```

### 10.3 硬边界（本 SPEC 与 NQ 后续实施都不允许违反）

```text
不修改 NQ 仓库（本仓库 Stage3-B2 永远不动 NQ）
不修改 Java 业务代码
不修改 contracts/openapi.yaml 语义
不修改 contracts/json-schema 已落地字段
不新增 Flyway migration
不新增 OpenAPI path
不接真实 NQ API（本 SPEC 仅声明期望）
不接真实 Kronos
不接真实 global-stock-data
不引入 TradingAgents Python
不实现真实下单
不绕过 NQ 风控
不重写 NQ 回测核心
不建设前端
不新增 NqFeedbackEventType（保持 8 种）
不影响 NQ GateJ-FREEZE 生产基线
不让 outbox 进入交易执行同步路径
不让 outbox 修改订单 / 风控 / 账本 / 资金 / 行情
不在 outbox payload 携带密钥 / token / 账号凭证
```

---

## 十一、与 Stage3 其他文档的衔接

```text
Stage3 主索引           docs/current/STAGE3_PLAN.md
NQ -> DH 出站事件链路   docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md       (8 事件 + ingest 路径 + 幂等)
DH -> NQ 入站请求链路   docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md       (DhBacktestRequest 出站 + result snapshot)
端到端契约规则          docs/current/STAGE3_CONTRACT_PLAN.md                (envelope / 5 字段 / errorCode / version)
NQ outbox 规格（本文件）docs/current/STAGE3_NQ_OUTBOX_SPEC.md                (NQ 端 outbox 表 / 模块 / 触发点 / retry)
测试策略                docs/current/STAGE3_TEST_PLAN.md                    (单测 / 联调 / 幂等 / 重试 / 边界)
4 批 IMPLEMENT 工单     docs/current/STAGE3_WORK_ORDER.md                   (B1 / B2 / B3 / B4 工单)
Batch 边界对照表        docs/current/STAGE3_BATCH_PLAN.md                   (执行顺序 + 依赖)
```

本 SPEC 是 NQ 端 outbox 实施的事实源；DH 仓库后续不再为 NQ outbox 写代码。
NQ 团队按 §8 Batch 落地时如发现 SPEC 与 DH 已落地契约 / endpoint 不一致，
应回到 docs/current/STAGE3_CONTRACT_PLAN.md / contracts/* 确认；必要时回 Stage3-B2 PLAN 修订本 SPEC。
