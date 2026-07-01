-- DH-GATEK-DECISION-PIPELINE-MVP-K3: Decision audit / snapshot / trace persistence.
-- 仅新增 DH 自身只读 decision pipeline 审计表；不新增 API、Controller、NQ runtime、真实 provider 或 LIVE 能力。
-- 严禁存储 secret / token / credential / raw provider sensitive response / NQ DB 内容。

create table if not exists dh_decision_request (
  decision_id varchar(128) primary key,
  request_id varchar(128) not null,
  trace_id varchar(128) not null,
  tenant_id varchar(128) not null,
  source varchar(128) not null,
  decision_type varchar(64) not null,
  subject_json jsonb not null default '{}'::jsonb,
  context_ref varchar(512),
  requested_at timestamptz not null,
  schema_version varchar(32) not null,
  created_at timestamptz not null default now(),
  constraint chk_dh_decision_request_type
    check (decision_type in ('READ_ONLY_RECOMMENDATION'))
);

create index if not exists idx_dh_decision_request_decision_id
  on dh_decision_request(decision_id);
create index if not exists idx_dh_decision_request_tenant_created
  on dh_decision_request(tenant_id, created_at);
create index if not exists idx_dh_decision_request_trace
  on dh_decision_request(trace_id);
create index if not exists idx_dh_decision_request_request
  on dh_decision_request(request_id);

comment on table dh_decision_request is
  'GateK K3 decision request 审计表。仅保存脱敏后的只读 recommendation 请求摘要，不保存凭证、token、secret 或 NQ DB 内容。';
comment on column dh_decision_request.decision_id is '单次 decision run 的链路 ID；K3 MVP 中由 requestId 派生。';
comment on column dh_decision_request.request_id is '请求 ID，用于与调用链、output 和 audit event 对账。';
comment on column dh_decision_request.trace_id is '链路 traceId，贯穿 request、snapshot、trace、provider、output 与 audit。';
comment on column dh_decision_request.tenant_id is '租户 ID；所有 K3 decision 持久化表必须携带。';
comment on column dh_decision_request.subject_json is '脱敏后的 subject 摘要 JSON；不含账户、数量、价格、凭证或执行字段。';
comment on column dh_decision_request.context_ref is '脱敏后的 context 引用；命中敏感词时由 usecase 侧写入 REDACTED。';

create table if not exists dh_decision_context_snapshot (
  decision_id varchar(128) primary key,
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  context_snapshot_json jsonb not null default '{}'::jsonb,
  evidence_refs_json jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_dh_decision_context_snapshot_decision_id
  on dh_decision_context_snapshot(decision_id);
create index if not exists idx_dh_decision_context_snapshot_tenant_created
  on dh_decision_context_snapshot(tenant_id, created_at);
create index if not exists idx_dh_decision_context_snapshot_trace
  on dh_decision_context_snapshot(trace_id);

comment on table dh_decision_context_snapshot is
  'GateK K3 decision context snapshot 表。只保存只读 evidence refs 和脱敏快照摘要，不读取或复制 NQ DB 内容。';
comment on column dh_decision_context_snapshot.context_snapshot_json is '脱敏后的 context snapshot 摘要 JSON。';
comment on column dh_decision_context_snapshot.evidence_refs_json is '本次 decision 使用的 evidence 引用列表 JSON。';

create table if not exists dh_decision_trace_step (
  id varchar(192) primary key,
  decision_id varchar(128) not null,
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  step_name varchar(64) not null,
  step_status varchar(32) not null,
  started_at timestamptz not null,
  ended_at timestamptz,
  error_code varchar(128),
  error_message varchar(256),
  created_at timestamptz not null default now(),
  constraint chk_dh_decision_trace_step_status
    check (step_status in ('STARTED', 'COMPLETED', 'FAILED'))
);

create index if not exists idx_dh_decision_trace_step_decision_id
  on dh_decision_trace_step(decision_id);
create index if not exists idx_dh_decision_trace_step_tenant_created
  on dh_decision_trace_step(tenant_id, created_at);
create index if not exists idx_dh_decision_trace_step_trace
  on dh_decision_trace_step(trace_id);

comment on table dh_decision_trace_step is
  'GateK K3 decision trace step 表。记录只读编排步骤开始、完成和失败，不表示真实 provider 或 NQ runtime 启动。';
comment on column dh_decision_trace_step.step_name is '步骤名，如 POLICY_CHECK / CONTEXT_BUILD / MOCK_PROVIDER_SIGNAL / RISK_REVIEW。';
comment on column dh_decision_trace_step.error_message is '脱敏错误摘要；仅允许异常类型或安全摘要，不写原始请求、响应或凭证。';

create table if not exists dh_decision_provider_call_log (
  id varchar(192) primary key,
  decision_id varchar(128) not null,
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  provider_name varchar(128) not null,
  provider_status varchar(64) not null,
  latency_ms bigint not null default 0,
  signal_json jsonb not null default '{}'::jsonb,
  error_code varchar(128),
  created_at timestamptz not null default now(),
  constraint chk_dh_decision_provider_latency
    check (latency_ms >= 0)
);

create index if not exists idx_dh_decision_provider_call_decision_id
  on dh_decision_provider_call_log(decision_id);
create index if not exists idx_dh_decision_provider_call_tenant_created
  on dh_decision_provider_call_log(tenant_id, created_at);
create index if not exists idx_dh_decision_provider_call_trace
  on dh_decision_provider_call_log(trace_id);

comment on table dh_decision_provider_call_log is
  'GateK K3 provider call summary 表。K3 仅记录 deterministic mock provider 摘要，不保存真实 provider 原始响应或凭证。';
comment on column dh_decision_provider_call_log.signal_json is
  '脱敏 provider signal 摘要 JSON，仅含 providerMode/status/action/reasonCodes 等安全字段。';

create table if not exists dh_decision_output (
  decision_id varchar(128) primary key,
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  decision_type varchar(64) not null,
  action varchar(64) not null,
  risk_level varchar(64) not null,
  policy_status varchar(64) not null,
  confidence numeric(8, 6) not null default 0,
  output_json jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  constraint chk_dh_decision_output_type
    check (decision_type in ('READ_ONLY_RECOMMENDATION')),
  constraint chk_dh_decision_output_action
    check (action in ('ABSTAIN', 'OBSERVE', 'NO_TRADE', 'LONG_BIAS', 'SHORT_BIAS')),
  constraint chk_dh_decision_output_confidence
    check (confidence >= 0 and confidence <= 1)
);

create index if not exists idx_dh_decision_output_decision_id
  on dh_decision_output(decision_id);
create index if not exists idx_dh_decision_output_tenant_created
  on dh_decision_output(tenant_id, created_at);
create index if not exists idx_dh_decision_output_trace
  on dh_decision_output(trace_id);
create index if not exists idx_dh_decision_output_request
  on dh_decision_output(request_id);

comment on table dh_decision_output is
  'GateK K3 decision output 表。保存 K1 structured read-only recommendation 输出，不代表交易授权。';
comment on column dh_decision_output.action is
  '只读 recommendation action；禁止 BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER。';
comment on column dh_decision_output.output_json is
  '完整结构化 DecisionOutput JSON；不含 free-text final trading instruction、凭证或 NQ mutation 指令。';

create table if not exists dh_decision_audit_event (
  id varchar(192) primary key,
  decision_id varchar(128) not null,
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  event_type varchar(64) not null,
  event_status varchar(32) not null,
  event_json jsonb not null default '{}'::jsonb,
  error_code varchar(128),
  created_at timestamptz not null default now(),
  constraint chk_dh_decision_audit_event_status
    check (event_status in ('SUCCESS', 'FAILED'))
);

create index if not exists idx_dh_decision_audit_event_decision_id
  on dh_decision_audit_event(decision_id);
create index if not exists idx_dh_decision_audit_event_tenant_created
  on dh_decision_audit_event(tenant_id, created_at);
create index if not exists idx_dh_decision_audit_event_trace
  on dh_decision_audit_event(trace_id);

comment on table dh_decision_audit_event is
  'GateK K3 decision audit event 表。记录 policy/provider/risk/output/persistence 生命周期事件，写失败必须 fail-closed。';
comment on column dh_decision_audit_event.event_json is
  '脱敏事件摘要 JSON；只含 requestId/action/status/risk/policy/provider 等安全字段。';
