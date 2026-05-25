-- Stage1: DH Agent Runtime tables. Stage1 默认走内存仓储；本脚本预留 SQL schema 作为后续 dh-infra 持久化的迁移起点。
-- 所有表都包含 id / created_at / updated_at / trace_id / status / payload_json，便于审计与复盘。

create table if not exists dh_research_runs (
  id varchar(64) primary key,
  tenant_id varchar(64) not null,
  trace_id varchar(64) not null,
  topic text not null,
  status varchar(32) not null,
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_research_runs_tenant on dh_research_runs(tenant_id);
create index if not exists idx_dh_research_runs_trace on dh_research_runs(trace_id);

create table if not exists dh_agent_tasks (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  trace_id varchar(64) not null,
  status varchar(32) not null default 'PLANNED',
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_agent_tasks_run on dh_agent_tasks(run_id);

create table if not exists dh_task_nodes (
  id varchar(64) primary key,
  task_id varchar(64) not null,
  run_id varchar(64) not null,
  trace_id varchar(64) not null,
  role varchar(32) not null,
  name varchar(128) not null,
  depends_on jsonb not null default '[]'::jsonb,
  status varchar(32) not null,
  payload_json jsonb not null default '{}'::jsonb,
  output_json jsonb,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_task_nodes_task on dh_task_nodes(task_id);
create index if not exists idx_dh_task_nodes_run on dh_task_nodes(run_id);

create table if not exists dh_agent_artifacts (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  node_id varchar(64),
  trace_id varchar(64) not null,
  role varchar(32) not null,
  type varchar(64) not null,
  status varchar(32) not null default 'CREATED',
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_agent_artifacts_run on dh_agent_artifacts(run_id);

create table if not exists dh_strategy_candidates (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  trace_id varchar(64) not null,
  source_agent varchar(128) not null,
  search_path text,
  evidence_refs jsonb not null default '[]'::jsonb,
  status varchar(32) not null,
  payload_json jsonb not null default '{}'::jsonb,
  score_snapshot jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_strategy_candidates_run on dh_strategy_candidates(run_id);

create table if not exists dh_candidate_scores (
  id varchar(64) primary key,
  candidate_id varchar(64) not null,
  run_id varchar(64) not null,
  trace_id varchar(64) not null,
  scorer varchar(64) not null,
  status varchar(32) not null default 'COMPUTED',
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_candidate_scores_candidate on dh_candidate_scores(candidate_id);

create table if not exists dh_judge_decisions (
  id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  trace_id varchar(64) not null,
  selected_candidate_ids jsonb not null default '[]'::jsonb,
  rejected_candidate_ids jsonb not null default '[]'::jsonb,
  status varchar(32) not null,
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_judge_decisions_run on dh_judge_decisions(run_id);

create table if not exists dh_experience_entries (
  id varchar(64) primary key,
  tenant_id varchar(64) not null,
  trace_id varchar(64) not null,
  experience_key varchar(256) not null,
  strategy_pattern varchar(128),
  market_regime varchar(128),
  data_source varchar(128),
  agent_role varchar(64),
  status varchar(32) not null default 'ACTIVE',
  score double precision not null default 0,
  success_count bigint not null default 0,
  failure_count bigint not null default 0,
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists ux_dh_experience_entries_key
  on dh_experience_entries(tenant_id, experience_key);

create table if not exists dh_pheromone_edges (
  id varchar(64) primary key,
  tenant_id varchar(64) not null,
  trace_id varchar(64),
  from_node varchar(128) not null,
  to_node varchar(128) not null,
  status varchar(32) not null default 'ACTIVE',
  pheromone_score double precision not null default 0,
  payload_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists ux_dh_pheromone_edges
  on dh_pheromone_edges(tenant_id, from_node, to_node);

create table if not exists dh_nq_feedback_events (
  id varchar(64) primary key,
  tenant_id varchar(64) not null,
  run_id varchar(64) not null,
  candidate_id varchar(64),
  trace_id varchar(64) not null,
  source varchar(32) not null,
  event_type varchar(64) not null,
  positive boolean not null,
  status varchar(32) not null default 'RECEIVED',
  payload_json jsonb not null default '{}'::jsonb,
  occurred_at timestamptz not null,
  received_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_dh_nq_feedback_events_run on dh_nq_feedback_events(run_id);
