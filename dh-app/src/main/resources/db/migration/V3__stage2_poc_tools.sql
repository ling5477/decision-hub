-- Stage2-PoC-B5: Tool / Research / Reflection / Checkpoint 持久化 schema 与 NQ Feedback 幂等扩展。
-- 仅扩 DH 自身的能力层表；不引入任何订单 / 成交 / 仓位 / 实盘执行相关表。
-- 所有表使用 if not exists，单脚本可重复执行不破坏现有数据。

-- ============================================================================
-- 1. Forecast Artifacts —— Kronos / 外部预测工具产物
-- ============================================================================
create table if not exists dh_forecast_artifacts (
  id varchar(64) primary key,
  trace_id varchar(64) not null,
  symbol varchar(64) not null,
  horizon varchar(32) not null,
  target varchar(32) not null,
  predictions_json jsonb not null default '[]'::jsonb,
  model_version varchar(64),
  generated_at timestamptz not null,
  status varchar(32) not null default 'GENERATED',
  raw_payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_forecast_artifacts_trace on dh_forecast_artifacts(trace_id);
create index if not exists idx_dh_forecast_artifacts_symbol on dh_forecast_artifacts(symbol);

comment on table dh_forecast_artifacts is 'Stage2 预测工具产物（Kronos 等）。raw_payload_json 保留外部原始 payload，可复盘。';
comment on column dh_forecast_artifacts.trace_id is '链路追踪 ID，关联 ResearchRun / AgentRun。';
comment on column dh_forecast_artifacts.predictions_json is '预测点序列 JSON：[{ts, value, confidence?}]';
comment on column dh_forecast_artifacts.raw_payload_json is '外部模型原始响应，供审计回放。';

-- ============================================================================
-- 2. External Market Snapshots —— global-stock-data 等行情快照
-- ============================================================================
create table if not exists dh_external_market_snapshots (
  id varchar(64) primary key,
  trace_id varchar(64) not null,
  symbols_json jsonb not null default '[]'::jsonb,
  source varchar(64) not null,
  range_start date not null,
  range_end date not null,
  fetched_at timestamptz not null,
  data_json jsonb not null default '{}'::jsonb,
  source_version varchar(64),
  status varchar(32) not null default 'FETCHED',
  raw_payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_external_market_snapshots_trace on dh_external_market_snapshots(trace_id);
create index if not exists idx_dh_external_market_snapshots_source on dh_external_market_snapshots(source);

comment on table dh_external_market_snapshots is 'Stage2 外部行情快照（global-stock-data 等）。range_start/end 标识快照区间。';
comment on column dh_external_market_snapshots.trace_id is '链路追踪 ID。';
comment on column dh_external_market_snapshots.symbols_json is '快照覆盖的 symbol 列表 JSON。';
comment on column dh_external_market_snapshots.data_json is '快照数据正文，按 source 约定结构。';
comment on column dh_external_market_snapshots.raw_payload_json is '外部数据源原始响应，供审计回放。';

-- ============================================================================
-- 3. Reflection Entries —— DynamicPlanner / Reflection 步骤记录
-- ============================================================================
create table if not exists dh_reflection_entries (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  trace_id varchar(64) not null,
  step_index integer not null,
  agent_role varchar(64) not null,
  type varchar(32) not null,
  content text not null,
  payload_json jsonb,
  created_at timestamptz not null default now()
);
create index if not exists idx_dh_reflection_entries_run on dh_reflection_entries(run_id);
create unique index if not exists ux_dh_reflection_entries_step on dh_reflection_entries(run_id, step_index);

comment on table dh_reflection_entries is 'Stage2 反思条目：DynamicPlanner / Reflection 在 run 内逐步骤产生的反思与决策依据。';
comment on column dh_reflection_entries.run_id is '所属 ResearchRun ID。';
comment on column dh_reflection_entries.trace_id is '链路追踪 ID。';
comment on column dh_reflection_entries.step_index is 'Run 内步骤序号，自 0 起，单调递增。';
comment on column dh_reflection_entries.payload_json is '可选结构化补充信息，用于细分场景。';

-- ============================================================================
-- 4. Checkpoint Entries —— DynamicPlanner / Checkpoint 阶段切换记录
-- ============================================================================
create table if not exists dh_checkpoint_entries (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  trace_id varchar(64) not null,
  checkpoint_index integer not null,
  type varchar(32) not null,
  status varchar(32) not null,
  snapshot_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists idx_dh_checkpoint_entries_run on dh_checkpoint_entries(run_id);
create unique index if not exists ux_dh_checkpoint_entries_index on dh_checkpoint_entries(run_id, checkpoint_index);

comment on table dh_checkpoint_entries is 'Stage2 Checkpoint：DynamicPlanner 在阶段切换时落地的可回放快照。';
comment on column dh_checkpoint_entries.run_id is '所属 ResearchRun ID。';
comment on column dh_checkpoint_entries.trace_id is '链路追踪 ID。';
comment on column dh_checkpoint_entries.checkpoint_index is 'Run 内 checkpoint 序号，自 0 起。';
comment on column dh_checkpoint_entries.snapshot_json is '阶段快照 JSON，可用于审计与潜在回放。';

-- ============================================================================
-- 5. ALTER dh_research_runs —— 增补 Stage2 Planner / Regime 字段
-- ============================================================================
alter table dh_research_runs add column if not exists regime varchar(64);
alter table dh_research_runs add column if not exists planner_strategy varchar(64) not null default 'DEFAULT';

comment on column dh_research_runs.regime is 'Stage2 市场 regime 标签（如 BULL / BEAR / RANGE / VOLATILE / UNKNOWN）。';
comment on column dh_research_runs.planner_strategy is 'Stage2 命中的 PlannerStrategy 名称，默认 DEFAULT。';

-- ============================================================================
-- 6. ALTER dh_nq_feedback_events —— 幂等 / 追踪 / 校验字段
-- ============================================================================
alter table dh_nq_feedback_events add column if not exists event_id varchar(64);
alter table dh_nq_feedback_events add column if not exists schema_version varchar(16) not null default '1.0.0';
alter table dh_nq_feedback_events add column if not exists validation_status varchar(32) not null default 'VALID';
alter table dh_nq_feedback_events add column if not exists source_job_id varchar(64);
alter table dh_nq_feedback_events add column if not exists request_id varchar(64);
alter table dh_nq_feedback_events add column if not exists correlation_id varchar(64);

comment on column dh_nq_feedback_events.event_id is 'NQ 端事件唯一 ID，DH 侧用于幂等去重。';
comment on column dh_nq_feedback_events.schema_version is 'NQ feedback envelope schema 版本。';
comment on column dh_nq_feedback_events.validation_status is '入口校验状态：VALID / INVALID / REJECTED。';
comment on column dh_nq_feedback_events.source_job_id is 'NQ 端任务/作业 ID，便于跨系统对账。';
comment on column dh_nq_feedback_events.request_id is '链路 request_id，与 NQ 一致。';
comment on column dh_nq_feedback_events.correlation_id is '链路 correlation_id，与 NQ 一致。';

-- event_id 唯一约束（保证幂等）。使用 DO 块兼容多次执行。
do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'ux_dh_nq_feedback_events_event_id'
  ) then
    create unique index if not exists ux_dh_nq_feedback_events_event_id
      on dh_nq_feedback_events(event_id)
      where event_id is not null;
  end if;
end$$;
