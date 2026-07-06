# Decision Hub DB Schema

## 1. 当前状态

```text
Current stage: stage-qdr-2 / Audit Trace Read Model + Human Approval Packet / B4_HUMAN_APPROVAL_API_AUDIT_IMPLEMENTED_BY_VALIDATION / PARTIAL_IMPLEMENTATION
Next stage:    DH-STAGE-QDR-2-B4-REVIEW-FREEZE / READY
```

Flyway 迁移：

```text
V1__init.sql                   Stage1 基线
V2__dh_agent_runtime.sql       Stage1 Agent Runtime + dh_nq_feedback_events
V3__stage2_poc_tools.sql       Stage2-PoC-B5：4 张新表 + 2 张 ALTER
V4__nq_feedback_replay_nonce.sql P1-4 replay nonce persistence
V5__decision_pipeline_audit.sql  Decision audit / snapshot / trace / provider call / output
V6__qdr_decision_core_baseline.sql stage-qdr-1 Decision Core baseline
V7__human_approval_packet.sql      stage-qdr-2 B3 Human Approval Packet
stage-qdr-2 B1 readmodel DTO   DONE / NO DB SCHEMA CHANGE / NO MIGRATION
stage-qdr-2 B2 read repository/API DONE / NO DB SCHEMA CHANGE / NO MIGRATION
stage-qdr-2 B4 approval API/audit DONE / NO DB SCHEMA CHANGE / NO MIGRATION
human_approval_packet            MIGRATION_ADDED / B3 / NOT_TRADING_AUTHORIZATION
```

## 1.0 stage-qdr-2 B1/B2/B3/B4 schema impact

B1 只新增 `dh-usecase` read model DTO / projection 与 tenant-bound query contract；B2 新增只读 JDBC adapter 与 authenticated read-only API。B1/B2 均不新增 DB schema，不新增 Flyway migration，不修改 V5 / V6 migration。B3 新增 `V7__human_approval_packet.sql`，只新增 `human_approval_packet` 表，不修改 V1-V6 历史 migration，不 ALTER 旧表，不 DROP 表。B4 只新增 API / command service / audit integration / tests / wiring，不新增 migration，不修改 V7，不修改 V1-V6。

```text
Read model DTO/query contract: DONE / USECASE_ONLY
Read repository: DONE / JDBC_READONLY / EXISTING_V5_V6_TABLES_ONLY
API implementation: DONE / READ_ONLY / NO_OPENAPI_FORMALIZATION
human_approval_packet: MIGRATION_ADDED / V7
Approval API: IMPLEMENTED_BY_VALIDATION / NO_SCHEMA_CHANGE
Approval write endpoint: IMPLEMENTED_BY_VALIDATION / NO_SCHEMA_CHANGE
Replay execution API: NOT STARTED
```

## 1.1 stage-qdr-1 Decision Core baseline

V6 新增 4 张 Quant Decision Review 主线表。该主线只服务只读审查与审计，不是交易事实源，不承载订单、撤单、账户、账本、风控 mutation 或 LIVE 状态。所有表与字段均在 migration 中补齐 `COMMENT ON TABLE` / `COMMENT ON COLUMN`，字段注释不得写入凭证、账户密钥或真实 provider 信息。

### decision_request

```text
id uuid primary key
request_key varchar not null
request_type varchar not null
source_system varchar not null
source_ref_id varchar null
tenant_id varchar not null
trace_id varchar not null
request_id varchar not null
input_payload_json jsonb not null
context_payload_json jsonb null
status varchar not null
created_at timestamptz not null
updated_at timestamptz not null

unique(tenant_id, request_key)
index(tenant_id, created_at)
index(trace_id)
index(request_id)
```

用途：一条外部或内部审查请求的主线入口。`tenant_id`、`trace_id`、`request_id` 必须写入，便于审计和幂等查询。

### decision_run

```text
id uuid primary key
decision_request_id uuid not null references decision_request(id)
run_no integer not null
status varchar not null
orchestrator_key varchar null
model_provider varchar null
model_name varchar null
started_at timestamptz not null
finished_at timestamptz null
latency_ms bigint null
error_code varchar null
error_message text null
created_at timestamptz not null

unique(decision_request_id, run_no)
index(status)
index(started_at)
```

用途：记录一次审查运行。`model_provider` / `model_name` 允许为空，本轮不接真实 provider，不写 provider SDK 事实。

### quant_signal

```text
id uuid primary key
decision_request_id uuid not null references decision_request(id)
source_system varchar not null
symbol varchar null
exchange varchar null
timeframe varchar null
signal_type varchar not null
signal_payload_json jsonb not null
strategy_id varchar null
strategy_version varchar null
dataset_version varchar null
received_at timestamptz not null
created_at timestamptz not null

index(source_system, received_at)
index(symbol, timeframe)
```

用途：保存 Quant Decision Review 的输入信号快照。无法识别输入类型时使用 `UNKNOWN_REVIEW_INPUT`，不得把输入解释成可执行订单。

### quant_decision

```text
id uuid primary key
quant_signal_id uuid null references quant_signal(id)
decision_run_id uuid not null references decision_run(id)
action varchar not null
confidence_score numeric(5,4) null
risk_level varchar not null
rationale text null
constraints_json jsonb null
human_approval_status varchar not null default 'NOT_REQUIRED'
created_at timestamptz not null

index(decision_run_id)
index(quant_signal_id)
index(created_at)
```

允许的 `action`：

```text
OBSERVE
NO_TRADE
LONG_BIAS
SHORT_BIAS
NEEDS_REVIEW
REJECTED
```

禁止的 `action`：

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
```

允许的 `human_approval_status`：

```text
NOT_REQUIRED
REQUIRED
PENDING
APPROVED
REJECTED
```

`LONG_BIAS / SHORT_BIAS` 只表示方向性审查意见，不代表 `BUY / SELL`。`human_approval_status` 当前默认 `NOT_REQUIRED`；B3 已新增独立 `human_approval_packet` 表，B4 approval API 只写该表的内部 approval 状态，不修改 `quant_decision.action`，也不把 `approval_status` 当作 execution signal。

## 1.2 stage-qdr-2 human_approval_packet（B3 MIGRATION_ADDED / NOT TRADING AUTHORIZATION）

B3 已新增 `V7__human_approval_packet.sql`。该 migration 只新增 `human_approval_packet` 表，用于 DH 内部人工审查证据；它不是交易授权表，不触发 NQ mutation，不触发真实 HTTP / provider，不承载 order、cancel、risk、ledger、paper 或 LIVE 状态。

```text
human_approval_packet
  id uuid primary key
  decision_run_id uuid not null references decision_run(id)
  tenant_id varchar not null
  trace_id varchar not null
  request_id varchar not null
  approval_key varchar not null
  approval_type varchar not null
  approval_status varchar not null
  risk_level varchar not null
  decision_action varchar not null
  confidence_score numeric(5,4) null
  summary text null
  checklist_json jsonb not null
  evidence_refs_json jsonb null
  reviewer_id varchar null
  reviewer_note text null
  decided_at timestamptz null
  created_at timestamptz not null
  updated_at timestamptz not null
```

建议约束：

```text
unique(tenant_id, approval_key)
approval_status in (PENDING, APPROVED, REJECTED, NEEDS_REVIEW, EXPIRED)
approval_type in (QUANT_DECISION_REVIEW, RISK_REVIEW, STRATEGY_RELEASE_REVIEW, ANOMALY_REVIEW)
decision_action in (OBSERVE, NO_TRADE, LONG_BIAS, SHORT_BIAS, NEEDS_REVIEW, REJECTED)
risk_level in (LOW, MEDIUM, HIGH, BLOCKED, UNKNOWN)
decision_action not in (BUY, SELL, PLACE_ORDER, CANCEL_ORDER, MARKET_ORDER, LIMIT_ORDER, MUTATE_NQ_STATE, EXECUTE_ORDER)
confidence_score is null or confidence_score between 0 and 1
```

索引：

```text
idx_human_approval_packet_tenant_created_at(tenant_id, created_at)
idx_human_approval_packet_decision_run_id(decision_run_id)
idx_human_approval_packet_status(approval_status)
idx_human_approval_packet_trace_id(trace_id)
idx_human_approval_packet_request_id(request_id)
```

`checklist_json` / `evidence_refs_json` 不得存储 credential、apiKey、apiSecret、passphrase、token、cookie、raw provider response 或 raw prompt secret。审批包只记录 human review evidence，不是交易授权。审批状态只允许改变 DH 内部 approval 状态；`APPROVED` 不等于 `BUY`，`REJECTED` 不等于 `SELL`，`LONG_BIAS / SHORT_BIAS` 不映射为 `BUY / SELL`。

## 1.3 stage-qdr-2 B4 schema conclusion

```text
B4 new migration: NO
B4 modified V7 migration: NO
B4 modified V1-V6 migrations: NO
B4 new table: NO
B4 schema impact: NONE
B4 audit target: existing dh_decision_audit_event
```

B4 approval create / decision submit 通过既有 `human_approval_packet` 表与既有 `dh_decision_audit_event` 审计表完成，不新增独立 audit subsystem。audit write 失败必须 fail-closed；repository write 失败必须 fail-closed；任何 approval API 都不得写 NQ DB、不得写交易事实、不得写 order / risk / ledger / paper / live mutation。

## 2. Stage2-PoC-B5 新增 4 张表

```text
dh_forecast_artifacts
  id, trace_id, symbol, horizon, target,
  predictions_json jsonb,            -- ForecastPoint[]
  model_version, generated_at timestamptz,
  status, raw_payload_json jsonb

dh_external_market_snapshots
  id, trace_id,
  symbols_json jsonb,                -- string[]
  source, range_start date, range_end date,
  fetched_at timestamptz,
  data_json jsonb,                   -- 外部原始结构
  source_version, status,
  raw_payload_json jsonb

dh_reflection_entries
  id, run_id, trace_id,
  step_index int,                    -- unique(run_id, step_index)
  agent_role, type, content,
  payload_json jsonb, created_at timestamptz

dh_checkpoint_entries
  id, run_id, trace_id,
  checkpoint_index int,              -- unique(run_id, checkpoint_index)
  type, status,
  snapshot_json jsonb,               -- 冗余整段 run snapshot
  created_at timestamptz
```

## 3. Stage2-PoC-B5 ALTER 既有 2 张表

```text
dh_research_runs
  + regime text                                -- IF NOT EXISTS
  + planner_strategy text default 'DEFAULT'    -- IF NOT EXISTS

dh_nq_feedback_events
  + event_id text                              -- IF NOT EXISTS（Stage2 envelope 幂等）
  + schema_version text                        -- IF NOT EXISTS
  + validation_status text                     -- IF NOT EXISTS
  + source_job_id text                         -- IF NOT EXISTS
  + request_id text                            -- IF NOT EXISTS
  + correlation_id text                        -- IF NOT EXISTS
  + 唯一索引 ux_dh_nq_feedback_events_event_id (where event_id is not null)
```

## 4. Stage1 表清单

```text
dh_research_runs
dh_agent_tasks
dh_task_nodes
dh_agent_artifacts
dh_strategy_candidates
dh_candidate_scores
dh_judge_decisions
dh_experience_entries
dh_pheromone_edges
dh_nq_feedback_events
```

## 5. 通用字段

所有核心表必须包含：

```text
id
trace_id
status
created_at
updated_at
payload_json
```

涉及外部回流的表必须包含：

```text
source_system
source_event_id
source_event_type
raw_payload_json
received_at
```

## 6. 设计原则

```text
结构化字段承载查询
payload_json / snapshot_json / data_json 承载完整快照
trace_id / request_id / correlation_id 串联任务全链路
NQ feedback 原样保存 raw_payload_json
经验分数可重算
所有 CREATE / ALTER 走 IF NOT EXISTS，重复 apply 安全
JSONB 写入统一走 CAST(? AS jsonb)（JDBC 仓储）
event_id 落唯一索引，envelope ingest 幂等
```

## 7. 禁止事项

```text
不保存交易事实替代 NQ
不复制 NQ 订单/成交/仓位/账本表（V3 显式不引入 dh_orders/dh_trades/dh_fills/dh_positions/dh_live_*）
不把 DH 经验分数作为 NQ 风控事实
```
