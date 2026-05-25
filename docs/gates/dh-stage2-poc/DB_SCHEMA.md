# Decision Hub DB Schema

## 1. 当前状态

```text
Current stage: Stage2-PoC-B5 IMPLEMENT completed
Next stage:    Stage2-PoC VERIFY
```

Flyway 迁移：

```text
V1__init.sql                   Stage1 基线
V2__dh_agent_runtime.sql       Stage1 Agent Runtime + dh_nq_feedback_events
V3__stage2_poc_tools.sql       Stage2-PoC-B5：4 张新表 + 2 张 ALTER
```

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
