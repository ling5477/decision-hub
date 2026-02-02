create table if not exists dh_run (
  run_id varchar(64) primary key,
  tenant_id varchar(64) not null,
  status varchar(32) not null,
  question text not null,
  config_snapshot jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists dh_run_step (
  step_id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  type varchar(32) not null,
  assigned_provider varchar(128),
  output_ref varchar(256),
  status varchar(32) not null,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists idx_dh_run_step_run on dh_run_step(run_id);

create table if not exists dh_ledger_event (
  event_id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  type varchar(64) not null,
  at timestamptz not null,
  payload jsonb not null default '{}'::jsonb
);

create index if not exists idx_dh_ledger_run on dh_ledger_event(run_id);

create table if not exists dh_artifact (
  artifact_id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  type varchar(64) not null,
  uri text,
  content jsonb,
  created_at timestamptz not null default now()
);

create table if not exists dh_config_snapshot (
  snapshot_id varchar(64) primary key,
  run_id varchar(64) not null,
  tenant_id varchar(64) not null,
  config jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

-- Config & commercialization skeleton
create table if not exists dh_model_config (
  model_key varchar(128) primary key,
  tenant_id varchar(64) not null,
  provider_type varchar(64) not null,
  endpoint text,
  model_name varchar(128),
  timeout_ms int not null default 60000,
  max_tokens int,
  price_input_micros bigint,
  price_output_micros bigint,
  capabilities jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists dh_role_template (
  template_id varchar(64) primary key,
  tenant_id varchar(64) not null,
  name varchar(128) not null,
  description text,
  prompt_template text not null,
  created_at timestamptz not null default now()
);

create table if not exists dh_routing_rule (
  rule_id varchar(64) primary key,
  tenant_id varchar(64) not null,
  name varchar(128) not null,
  rule jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists dh_usage_meter (
  id bigserial primary key,
  tenant_id varchar(64) not null,
  user_id varchar(64),
  run_id varchar(64),
  tokens bigint not null default 0,
  cost_micros bigint not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists dh_api_key (
  key_id varchar(64) primary key,
  tenant_id varchar(64) not null,
  key_hash varchar(256) not null,
  name varchar(128),
  status varchar(32) not null default 'ACTIVE',
  created_at timestamptz not null default now()
);


create table if not exists dh_outbox (
  outbox_id varchar(64) primary key,
  tenant_id varchar(64) not null,
  aggregate_type varchar(64) not null,
  aggregate_id varchar(64) not null,
  event_type varchar(64) not null,
  payload jsonb not null default '{}'::jsonb,
  status varchar(32) not null default 'NEW',
  created_at timestamptz not null default now()
);
create index if not exists idx_dh_outbox_status on dh_outbox(status);
