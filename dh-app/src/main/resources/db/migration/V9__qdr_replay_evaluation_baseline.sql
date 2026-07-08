-- stage-qdr-4 B2: QDR replay / evaluation / regression persistence baseline.
-- 本 migration 只新增 DH 内部 B2 persistence baseline；不新增 API，不触发 provider、HTTP、NQ、Agent、LangGraph、交易或 LIVE。
-- 所有 JSONB 仅保存结构化 summary/ref；raw prompt、raw provider response 与 credential material 均禁止入库。
-- FK 策略：B2 内部引用使用 tenant-bound FK；不对 V6/V8 source 表强 FK，避免 replay baseline 与历史 source 生命周期过度耦合。

create table if not exists qdr_replay_input_ref (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128),
  verdict_id varchar(128),
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128),
  model_gateway_version_ref varchar(128),
  ref_type varchar(64) not null,
  ref_id varchar(256) not null,
  input_ref jsonb not null,
  content_hash varchar(64) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_replay_input_ref_tenant_id unique (tenant_id, id),
  constraint ux_qdr_replay_input_ref_tenant_ref unique (tenant_id, ref_type, ref_id),
  constraint chk_qdr_replay_input_ref_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_replay_input_ref_hash
    check (content_hash ~ '^[0-9a-f]{64}$'),
  constraint chk_qdr_replay_input_ref_json_shape
    check (
      jsonb_typeof(input_ref) = 'object'
      and not (input_ref ?| array[
        'rawPrompt',
        'raw_prompt',
        'promptText',
        'rawProviderResponse',
        'raw_provider_response',
        'providerRaw',
        'credential',
        'apiKey',
        'apiSecret',
        'passphrase',
        'token',
        'cookie',
        'secret'
      ])
    )
);

create table if not exists qdr_replay_output_ref (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128) not null,
  verdict_id varchar(128),
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_gateway_version_ref varchar(128),
  ref_type varchar(64) not null,
  ref_id varchar(256) not null,
  output_ref jsonb not null,
  content_hash varchar(64) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_replay_output_ref_tenant_id unique (tenant_id, id),
  constraint ux_qdr_replay_output_ref_tenant_ref unique (tenant_id, ref_type, ref_id),
  constraint chk_qdr_replay_output_ref_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_replay_output_ref_hash
    check (content_hash ~ '^[0-9a-f]{64}$'),
  constraint chk_qdr_replay_output_ref_json_shape
    check (
      jsonb_typeof(output_ref) = 'object'
      and not (output_ref ?| array[
        'rawPrompt',
        'raw_prompt',
        'promptText',
        'rawProviderResponse',
        'raw_provider_response',
        'providerRaw',
        'credential',
        'apiKey',
        'apiSecret',
        'passphrase',
        'token',
        'cookie',
        'secret'
      ])
    )
);

create table if not exists qdr_expected_decision_summary (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128),
  verdict_id varchar(128),
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_gateway_version_ref varchar(128),
  input_ref_id uuid,
  output_ref_id uuid,
  summary_role varchar(16) not null,
  decision_type varchar(128) not null,
  action_label varchar(64) not null,
  confidence_band varchar(64) not null,
  risk_level varchar(64) not null,
  summary_json jsonb not null,
  required_evidence_refs_json jsonb not null,
  forbidden_actions_json jsonb not null,
  summary_hash varchar(64) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_expected_decision_summary_tenant_id unique (tenant_id, id),
  constraint ux_qdr_expected_decision_summary_role_hash
    unique (tenant_id, case_id, evaluation_id, summary_role, summary_hash),
  constraint fk_qdr_expected_summary_input_ref
    foreign key (tenant_id, input_ref_id) references qdr_replay_input_ref(tenant_id, id),
  constraint fk_qdr_expected_summary_output_ref
    foreign key (tenant_id, output_ref_id) references qdr_replay_output_ref(tenant_id, id),
  constraint chk_qdr_expected_decision_summary_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_expected_decision_summary_role
    check (summary_role in ('EXPECTED', 'ACTUAL')),
  constraint chk_qdr_expected_decision_summary_action_label
    check (action_label in ('OBSERVE', 'NO_TRADE', 'LONG_BIAS', 'SHORT_BIAS', 'NEEDS_REVIEW', 'REJECTED')),
  constraint chk_qdr_expected_decision_summary_no_executable_action
    check (action_label not in (
      'BUY',
      'SELL',
      'MARKET_ORDER',
      'PLACE_ORDER',
      'CANCEL_ORDER',
      'MUTATE_NQ_STATE'
    )),
  constraint chk_qdr_expected_decision_summary_risk
    check (risk_level in ('LOW', 'MEDIUM', 'HIGH', 'BLOCKED', 'UNKNOWN')),
  constraint chk_qdr_expected_decision_summary_hash
    check (summary_hash ~ '^[0-9a-f]{64}$'),
  constraint chk_qdr_expected_decision_summary_json_shape
    check (
      jsonb_typeof(summary_json) = 'object'
      and jsonb_typeof(required_evidence_refs_json) = 'array'
      and jsonb_typeof(forbidden_actions_json) = 'array'
      and not (summary_json ?| array[
        'rawPrompt',
        'raw_prompt',
        'promptText',
        'rawProviderResponse',
        'raw_provider_response',
        'providerRaw',
        'credential',
        'apiKey',
        'apiSecret',
        'passphrase',
        'token',
        'cookie',
        'secret'
      ])
    )
);

create table if not exists qdr_replay_case (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_gateway_version_ref varchar(128),
  input_ref_id uuid not null,
  expected_summary_id uuid not null,
  expected_summary_hash varchar(64) not null,
  case_checksum varchar(64) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_replay_case_tenant_id unique (tenant_id, id),
  constraint ux_qdr_replay_case_tenant_case unique (tenant_id, case_id),
  constraint fk_qdr_replay_case_input_ref
    foreign key (tenant_id, input_ref_id) references qdr_replay_input_ref(tenant_id, id),
  constraint fk_qdr_replay_case_expected_summary
    foreign key (tenant_id, expected_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint chk_qdr_replay_case_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_replay_case_hashes
    check (
      expected_summary_hash ~ '^[0-9a-f]{64}$'
      and case_checksum ~ '^[0-9a-f]{64}$'
    )
);

create table if not exists qdr_evaluation_case (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128) not null,
  verdict_id varchar(128),
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_version_ref varchar(128),
  model_gateway_version_ref varchar(128),
  input_ref_id uuid not null,
  output_ref_id uuid,
  expected_summary_id uuid not null,
  actual_summary_id uuid,
  expected_summary_hash varchar(64) not null,
  actual_summary_hash varchar(64),
  verdict varchar(32),
  severity varchar(32),
  finding_code varchar(128),
  finding_message text,
  evaluation_checksum varchar(64) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_evaluation_case_tenant_id unique (tenant_id, id),
  constraint ux_qdr_evaluation_case_tenant_evaluation unique (tenant_id, evaluation_id),
  constraint ux_qdr_evaluation_case_identity_checksum
    unique (tenant_id, case_id, policy_version, model_gateway_version_ref, evaluation_checksum),
  constraint fk_qdr_evaluation_case_replay_case
    foreign key (tenant_id, case_id) references qdr_replay_case(tenant_id, case_id),
  constraint fk_qdr_evaluation_case_input_ref
    foreign key (tenant_id, input_ref_id) references qdr_replay_input_ref(tenant_id, id),
  constraint fk_qdr_evaluation_case_output_ref
    foreign key (tenant_id, output_ref_id) references qdr_replay_output_ref(tenant_id, id),
  constraint fk_qdr_evaluation_case_expected_summary
    foreign key (tenant_id, expected_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint fk_qdr_evaluation_case_actual_summary
    foreign key (tenant_id, actual_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint chk_qdr_evaluation_case_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_evaluation_case_model_ref
    check (model_version_ref is not null or model_gateway_version_ref is not null),
  constraint chk_qdr_evaluation_case_verdict
    check (verdict is null or verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED')),
  constraint chk_qdr_evaluation_case_severity
    check (severity is null or severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER')),
  constraint chk_qdr_evaluation_case_hashes
    check (
      expected_summary_hash ~ '^[0-9a-f]{64}$'
      and (actual_summary_hash is null or actual_summary_hash ~ '^[0-9a-f]{64}$')
      and evaluation_checksum ~ '^[0-9a-f]{64}$'
    )
);

create table if not exists qdr_regression_verdict (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128) not null,
  verdict_id varchar(128) not null,
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_gateway_version_ref varchar(128),
  input_ref_id uuid not null,
  output_ref_id uuid,
  expected_summary_id uuid not null,
  actual_summary_id uuid,
  expected_summary_hash varchar(64) not null,
  actual_summary_hash varchar(64),
  verdict varchar(32) not null,
  severity varchar(32) not null,
  finding_code varchar(128),
  finding_message text,
  failure_reason varchar(256),
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_regression_verdict_tenant_id unique (tenant_id, id),
  constraint ux_qdr_regression_verdict_tenant_verdict unique (tenant_id, verdict_id),
  constraint ux_qdr_regression_verdict_identity
    unique (tenant_id, evaluation_id, policy_version, model_gateway_version_ref),
  constraint fk_qdr_regression_verdict_evaluation_case
    foreign key (tenant_id, evaluation_id) references qdr_evaluation_case(tenant_id, evaluation_id),
  constraint fk_qdr_regression_verdict_input_ref
    foreign key (tenant_id, input_ref_id) references qdr_replay_input_ref(tenant_id, id),
  constraint fk_qdr_regression_verdict_output_ref
    foreign key (tenant_id, output_ref_id) references qdr_replay_output_ref(tenant_id, id),
  constraint fk_qdr_regression_verdict_expected_summary
    foreign key (tenant_id, expected_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint fk_qdr_regression_verdict_actual_summary
    foreign key (tenant_id, actual_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint chk_qdr_regression_verdict_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_regression_verdict_status
    check (verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED')),
  constraint chk_qdr_regression_verdict_severity
    check (severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER')),
  constraint chk_qdr_regression_verdict_failure_shape
    check (verdict <> 'FAIL' or failure_reason is not null or finding_code is not null),
  constraint chk_qdr_regression_verdict_hashes
    check (
      expected_summary_hash ~ '^[0-9a-f]{64}$'
      and (actual_summary_hash is null or actual_summary_hash ~ '^[0-9a-f]{64}$')
    )
);

create table if not exists qdr_regression_finding (
  id uuid primary key,
  tenant_id varchar(128) not null,
  case_id varchar(128) not null,
  evaluation_id varchar(128) not null,
  verdict_id varchar(128) not null,
  source_decision_id varchar(128),
  source_request_id varchar(128),
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  policy_version varchar(128) not null,
  model_gateway_version_ref varchar(128),
  input_ref_id uuid,
  output_ref_id uuid,
  expected_summary_id uuid,
  actual_summary_id uuid,
  expected_summary_hash varchar(64),
  actual_summary_hash varchar(64),
  verdict varchar(32) not null,
  severity varchar(32) not null,
  finding_code varchar(128) not null,
  finding_message text not null,
  evidence_ref varchar(256),
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_regression_finding_tenant_id unique (tenant_id, id),
  constraint fk_qdr_regression_finding_verdict
    foreign key (tenant_id, verdict_id) references qdr_regression_verdict(tenant_id, verdict_id),
  constraint fk_qdr_regression_finding_input_ref
    foreign key (tenant_id, input_ref_id) references qdr_replay_input_ref(tenant_id, id),
  constraint fk_qdr_regression_finding_output_ref
    foreign key (tenant_id, output_ref_id) references qdr_replay_output_ref(tenant_id, id),
  constraint fk_qdr_regression_finding_expected_summary
    foreign key (tenant_id, expected_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint fk_qdr_regression_finding_actual_summary
    foreign key (tenant_id, actual_summary_id) references qdr_expected_decision_summary(tenant_id, id),
  constraint chk_qdr_regression_finding_source
    check (source_decision_id is not null or source_request_id is not null),
  constraint chk_qdr_regression_finding_verdict
    check (verdict in ('PASS', 'FAIL', 'WARN', 'SKIPPED')),
  constraint chk_qdr_regression_finding_severity
    check (severity in ('INFO', 'WARN', 'ERROR', 'BLOCKER')),
  constraint chk_qdr_regression_finding_message
    check (finding_code <> '' and finding_message <> ''),
  constraint chk_qdr_regression_finding_hashes
    check (
      (expected_summary_hash is null or expected_summary_hash ~ '^[0-9a-f]{64}$')
      and (actual_summary_hash is null or actual_summary_hash ~ '^[0-9a-f]{64}$')
    )
);

create index if not exists idx_qdr_replay_input_ref_tenant_case
  on qdr_replay_input_ref(tenant_id, case_id);
create index if not exists idx_qdr_replay_input_ref_tenant_evaluation
  on qdr_replay_input_ref(tenant_id, evaluation_id);
create index if not exists idx_qdr_replay_input_ref_tenant_verdict
  on qdr_replay_input_ref(tenant_id, verdict_id);
create index if not exists idx_qdr_replay_input_ref_tenant_trace
  on qdr_replay_input_ref(tenant_id, trace_id);
create index if not exists idx_qdr_replay_input_ref_tenant_source_request
  on qdr_replay_input_ref(tenant_id, source_request_id);
create index if not exists idx_qdr_replay_input_ref_tenant_source_decision
  on qdr_replay_input_ref(tenant_id, source_decision_id);
create index if not exists idx_qdr_replay_input_ref_tenant_created
  on qdr_replay_input_ref(tenant_id, created_at);

create index if not exists idx_qdr_replay_output_ref_tenant_case
  on qdr_replay_output_ref(tenant_id, case_id);
create index if not exists idx_qdr_replay_output_ref_tenant_evaluation
  on qdr_replay_output_ref(tenant_id, evaluation_id);
create index if not exists idx_qdr_replay_output_ref_tenant_verdict
  on qdr_replay_output_ref(tenant_id, verdict_id);
create index if not exists idx_qdr_replay_output_ref_tenant_trace
  on qdr_replay_output_ref(tenant_id, trace_id);
create index if not exists idx_qdr_replay_output_ref_tenant_source_request
  on qdr_replay_output_ref(tenant_id, source_request_id);
create index if not exists idx_qdr_replay_output_ref_tenant_source_decision
  on qdr_replay_output_ref(tenant_id, source_decision_id);
create index if not exists idx_qdr_replay_output_ref_tenant_created
  on qdr_replay_output_ref(tenant_id, created_at);

create index if not exists idx_qdr_expected_decision_summary_tenant_case
  on qdr_expected_decision_summary(tenant_id, case_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_evaluation
  on qdr_expected_decision_summary(tenant_id, evaluation_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_verdict
  on qdr_expected_decision_summary(tenant_id, verdict_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_trace
  on qdr_expected_decision_summary(tenant_id, trace_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_source_request
  on qdr_expected_decision_summary(tenant_id, source_request_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_source_decision
  on qdr_expected_decision_summary(tenant_id, source_decision_id);
create index if not exists idx_qdr_expected_decision_summary_tenant_created
  on qdr_expected_decision_summary(tenant_id, created_at);

create index if not exists idx_qdr_replay_case_tenant_trace
  on qdr_replay_case(tenant_id, trace_id);
create index if not exists idx_qdr_replay_case_tenant_source_request
  on qdr_replay_case(tenant_id, source_request_id);
create index if not exists idx_qdr_replay_case_tenant_source_decision
  on qdr_replay_case(tenant_id, source_decision_id);
create index if not exists idx_qdr_replay_case_tenant_created
  on qdr_replay_case(tenant_id, created_at);
create index if not exists idx_qdr_replay_case_tenant_checksum
  on qdr_replay_case(tenant_id, case_checksum);

create index if not exists idx_qdr_evaluation_case_tenant_case
  on qdr_evaluation_case(tenant_id, case_id, created_at);
create index if not exists idx_qdr_evaluation_case_tenant_verdict
  on qdr_evaluation_case(tenant_id, verdict_id);
create index if not exists idx_qdr_evaluation_case_tenant_trace
  on qdr_evaluation_case(tenant_id, trace_id);
create index if not exists idx_qdr_evaluation_case_tenant_source_request
  on qdr_evaluation_case(tenant_id, source_request_id);
create index if not exists idx_qdr_evaluation_case_tenant_source_decision
  on qdr_evaluation_case(tenant_id, source_decision_id);
create index if not exists idx_qdr_evaluation_case_tenant_created
  on qdr_evaluation_case(tenant_id, created_at);
create index if not exists idx_qdr_evaluation_case_tenant_verdict_status
  on qdr_evaluation_case(tenant_id, verdict, severity, created_at);

create index if not exists idx_qdr_regression_verdict_tenant_case
  on qdr_regression_verdict(tenant_id, case_id, created_at);
create index if not exists idx_qdr_regression_verdict_tenant_evaluation
  on qdr_regression_verdict(tenant_id, evaluation_id, created_at);
create index if not exists idx_qdr_regression_verdict_tenant_trace
  on qdr_regression_verdict(tenant_id, trace_id);
create index if not exists idx_qdr_regression_verdict_tenant_source_request
  on qdr_regression_verdict(tenant_id, source_request_id);
create index if not exists idx_qdr_regression_verdict_tenant_source_decision
  on qdr_regression_verdict(tenant_id, source_decision_id);
create index if not exists idx_qdr_regression_verdict_tenant_created
  on qdr_regression_verdict(tenant_id, created_at);
create index if not exists idx_qdr_regression_verdict_tenant_status
  on qdr_regression_verdict(tenant_id, verdict, severity, created_at);

create index if not exists idx_qdr_regression_finding_tenant_case
  on qdr_regression_finding(tenant_id, case_id, created_at);
create index if not exists idx_qdr_regression_finding_tenant_evaluation
  on qdr_regression_finding(tenant_id, evaluation_id, created_at);
create index if not exists idx_qdr_regression_finding_tenant_verdict
  on qdr_regression_finding(tenant_id, verdict_id, created_at);
create index if not exists idx_qdr_regression_finding_tenant_trace
  on qdr_regression_finding(tenant_id, trace_id);
create index if not exists idx_qdr_regression_finding_tenant_source_request
  on qdr_regression_finding(tenant_id, source_request_id);
create index if not exists idx_qdr_regression_finding_tenant_source_decision
  on qdr_regression_finding(tenant_id, source_decision_id);
create index if not exists idx_qdr_regression_finding_tenant_created
  on qdr_regression_finding(tenant_id, created_at);
create index if not exists idx_qdr_regression_finding_tenant_severity
  on qdr_regression_finding(tenant_id, severity, created_at);

comment on table qdr_replay_input_ref is
  'stage-qdr-4 B2 replay input ref 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_replay_input_ref.id is
  '主键 UUID；input ref identity。';
comment on column qdr_replay_input_ref.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_replay_input_ref.case_id is
  '租户内 replay case ID；只用于回放基线反查。';
comment on column qdr_replay_input_ref.evaluation_id is
  '可选 evaluation ID；不代表 replay execution API 已启动。';
comment on column qdr_replay_input_ref.verdict_id is
  '可选 verdict ID；verdict 只表示 replay/evaluation 结果。';
comment on column qdr_replay_input_ref.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_replay_input_ref.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_replay_input_ref.trace_id is
  'traceId；用于串联 replay/evaluation/regression evidence。';
comment on column qdr_replay_input_ref.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_replay_input_ref.policy_version is
  '生成 input ref 时使用的 evaluation policy version。';
comment on column qdr_replay_input_ref.model_gateway_version_ref is
  'model gateway 版本安全引用；不代表 real provider 已接入。';
comment on column qdr_replay_input_ref.ref_type is
  '输入引用类型；只保存结构化 ref，不保存原文。';
comment on column qdr_replay_input_ref.ref_id is
  '输入引用 ID；不得保存 credential material。';
comment on column qdr_replay_input_ref.input_ref is
  '结构化 input ref JSON；不得保存 raw prompt、raw provider response、credential、token、cookie、apiKey、apiSecret 或 passphrase。';
comment on column qdr_replay_input_ref.content_hash is
  '输入内容 SHA-256 hash；64 位 hex，不保存输入原文。';
comment on column qdr_replay_input_ref.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_replay_input_ref.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_replay_output_ref is
  'stage-qdr-4 B2 replay/evaluation output ref 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_replay_output_ref.id is
  '主键 UUID；output ref identity。';
comment on column qdr_replay_output_ref.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_replay_output_ref.case_id is
  '租户内 replay case ID；只用于回放基线反查。';
comment on column qdr_replay_output_ref.evaluation_id is
  '租户内 evaluation ID；output ref 必须绑定 evaluation。';
comment on column qdr_replay_output_ref.verdict_id is
  '可选 verdict ID；verdict 只表示 replay/evaluation 结果。';
comment on column qdr_replay_output_ref.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_replay_output_ref.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_replay_output_ref.trace_id is
  'traceId；用于串联 replay/evaluation/regression evidence。';
comment on column qdr_replay_output_ref.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_replay_output_ref.policy_version is
  '生成 output ref 时使用的 evaluation policy version。';
comment on column qdr_replay_output_ref.model_gateway_version_ref is
  'model gateway 版本安全引用；不代表 real provider 已接入。';
comment on column qdr_replay_output_ref.ref_type is
  '输出引用类型；只保存结构化 ref，不保存 provider 原始响应。';
comment on column qdr_replay_output_ref.ref_id is
  '输出引用 ID；不得保存 credential material。';
comment on column qdr_replay_output_ref.output_ref is
  '结构化 output ref JSON；不得保存 raw prompt、raw provider response、credential、token、cookie、apiKey、apiSecret 或 passphrase。';
comment on column qdr_replay_output_ref.content_hash is
  '输出内容 SHA-256 hash；64 位 hex，不保存 provider 原始响应。';
comment on column qdr_replay_output_ref.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_replay_output_ref.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_expected_decision_summary is
  'stage-qdr-4 B2 expected/actual decision summary 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_expected_decision_summary.id is
  '主键 UUID；summary identity。';
comment on column qdr_expected_decision_summary.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_expected_decision_summary.case_id is
  '租户内 replay case ID。';
comment on column qdr_expected_decision_summary.evaluation_id is
  '可选 evaluation ID；actual summary 通常绑定 evaluation。';
comment on column qdr_expected_decision_summary.verdict_id is
  '可选 verdict ID；verdict 只表示 replay/evaluation 结果。';
comment on column qdr_expected_decision_summary.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_expected_decision_summary.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_expected_decision_summary.trace_id is
  'traceId；用于串联 replay/evaluation/regression evidence。';
comment on column qdr_expected_decision_summary.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_expected_decision_summary.policy_version is
  'summary 对应的 evaluation policy version。';
comment on column qdr_expected_decision_summary.model_gateway_version_ref is
  'model gateway 版本安全引用；不代表 real provider 已接入。';
comment on column qdr_expected_decision_summary.input_ref_id is
  'tenant-bound input ref FK；不保存输入原文。';
comment on column qdr_expected_decision_summary.output_ref_id is
  'tenant-bound output ref FK；不保存 provider 原始响应。';
comment on column qdr_expected_decision_summary.summary_role is
  'summary 角色；EXPECTED / ACTUAL。';
comment on column qdr_expected_decision_summary.decision_type is
  'decision 类型；例如 READ_ONLY_RECOMMENDATION，不是 trading signal。';
comment on column qdr_expected_decision_summary.action_label is
  '只读 action label；不得保存 BUY、SELL、MARKET_ORDER、PLACE_ORDER、CANCEL_ORDER 或 MUTATE_NQ_STATE。';
comment on column qdr_expected_decision_summary.confidence_band is
  '脱敏 confidence 区间标签，不代表交易执行概率。';
comment on column qdr_expected_decision_summary.risk_level is
  'QDR 风险等级；只用于回放评估，不写 NQ 风控事实。';
comment on column qdr_expected_decision_summary.summary_json is
  '结构化 summary JSON；不得保存 raw prompt、raw provider response、credential 或可执行订单 payload。';
comment on column qdr_expected_decision_summary.required_evidence_refs_json is
  '结构化 evidence ref 数组；只保存 ref/hash，不保存原始 payload。';
comment on column qdr_expected_decision_summary.forbidden_actions_json is
  '禁止动作数组；记录 BUY/SELL/PLACE_ORDER 等禁止项时只能表示 forbiddenActions，不是 allowed action。';
comment on column qdr_expected_decision_summary.summary_hash is
  'summary SHA-256 hash；64 位 hex，不保存原始 summary source。';
comment on column qdr_expected_decision_summary.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_expected_decision_summary.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_replay_case is
  'stage-qdr-4 B2 replay case 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_replay_case.id is
  '主键 UUID；replay case identity。';
comment on column qdr_replay_case.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_replay_case.case_id is
  '租户内 replay case ID；unique(tenant_id, case_id)。';
comment on column qdr_replay_case.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_replay_case.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_replay_case.trace_id is
  'traceId；用于 replay case 审计反查。';
comment on column qdr_replay_case.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_replay_case.policy_version is
  '生成 replay case 时采用的 evaluation policy version。';
comment on column qdr_replay_case.model_gateway_version_ref is
  'model gateway 版本安全引用；不代表 real provider 已接入。';
comment on column qdr_replay_case.input_ref_id is
  'tenant-bound input ref FK；repository 保存时必须先写 input ref。';
comment on column qdr_replay_case.expected_summary_id is
  'tenant-bound expected summary FK；repository 保存时必须先写 summary。';
comment on column qdr_replay_case.expected_summary_hash is
  'expected summary SHA-256 hash；64 位 hex。';
comment on column qdr_replay_case.case_checksum is
  'case checksum；duplicate same checksum 可幂等，different checksum 必须 fail-closed。';
comment on column qdr_replay_case.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_replay_case.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_evaluation_case is
  'stage-qdr-4 B2 evaluation case 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_evaluation_case.id is
  '主键 UUID；evaluation case identity。';
comment on column qdr_evaluation_case.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_evaluation_case.case_id is
  '租户内 replay case ID；通过 tenant-bound FK 关联 qdr_replay_case。';
comment on column qdr_evaluation_case.evaluation_id is
  '租户内 evaluation ID；unique(tenant_id, evaluation_id)。';
comment on column qdr_evaluation_case.verdict_id is
  '可选 verdict ID；不反向 FK 到 verdict，避免 FK 循环。';
comment on column qdr_evaluation_case.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_evaluation_case.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_evaluation_case.trace_id is
  'traceId；用于 evaluation 审计反查。';
comment on column qdr_evaluation_case.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_evaluation_case.policy_version is
  'evaluation policy version。';
comment on column qdr_evaluation_case.model_version_ref is
  'model version 安全引用；不代表 real provider 已接入。';
comment on column qdr_evaluation_case.model_gateway_version_ref is
  'model gateway version 安全引用；不代表 real HTTP 或 provider 已接入。';
comment on column qdr_evaluation_case.input_ref_id is
  'tenant-bound input ref FK。';
comment on column qdr_evaluation_case.output_ref_id is
  'tenant-bound output ref FK；可为空，不保存 provider 原始响应。';
comment on column qdr_evaluation_case.expected_summary_id is
  'tenant-bound expected summary FK。';
comment on column qdr_evaluation_case.actual_summary_id is
  'tenant-bound actual summary FK；可为空。';
comment on column qdr_evaluation_case.expected_summary_hash is
  'expected summary SHA-256 hash；64 位 hex。';
comment on column qdr_evaluation_case.actual_summary_hash is
  'actual summary SHA-256 hash；64 位 hex，可为空。';
comment on column qdr_evaluation_case.verdict is
  'evaluation 当前 verdict 摘要；PASS/FAIL/WARN/SKIPPED 只表示 replay/evaluation 结果。';
comment on column qdr_evaluation_case.severity is
  'finding 最高严重度；沿用 B1 INFO/WARN/ERROR/BLOCKER。';
comment on column qdr_evaluation_case.finding_code is
  '聚合 finding code；不得保存敏感材料。';
comment on column qdr_evaluation_case.finding_message is
  '脱敏 finding 摘要；不得保存 raw prompt、raw provider response 或 credential。';
comment on column qdr_evaluation_case.evaluation_checksum is
  'evaluation checksum；duplicate same checksum 可幂等，different checksum 必须 fail-closed。';
comment on column qdr_evaluation_case.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_evaluation_case.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_regression_verdict is
  'stage-qdr-4 B2 regression verdict 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_regression_verdict.id is
  '主键 UUID；regression verdict identity。';
comment on column qdr_regression_verdict.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_regression_verdict.case_id is
  '租户内 replay case ID。';
comment on column qdr_regression_verdict.evaluation_id is
  '租户内 evaluation ID；通过 tenant-bound FK 关联 qdr_evaluation_case。';
comment on column qdr_regression_verdict.verdict_id is
  '租户内 verdict ID；unique(tenant_id, verdict_id)。';
comment on column qdr_regression_verdict.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_regression_verdict.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_regression_verdict.trace_id is
  'traceId；用于 regression verdict 审计反查。';
comment on column qdr_regression_verdict.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_regression_verdict.policy_version is
  'verdict 对应 policy version。';
comment on column qdr_regression_verdict.model_gateway_version_ref is
  'model gateway version 安全引用；不代表 real HTTP 或 provider 已接入。';
comment on column qdr_regression_verdict.input_ref_id is
  'tenant-bound input ref FK。';
comment on column qdr_regression_verdict.output_ref_id is
  'tenant-bound output ref FK；可为空。';
comment on column qdr_regression_verdict.expected_summary_id is
  'tenant-bound expected summary FK。';
comment on column qdr_regression_verdict.actual_summary_id is
  'tenant-bound actual summary FK；可为空。';
comment on column qdr_regression_verdict.expected_summary_hash is
  'expected summary SHA-256 hash；64 位 hex。';
comment on column qdr_regression_verdict.actual_summary_hash is
  'actual summary SHA-256 hash；64 位 hex，可为空。';
comment on column qdr_regression_verdict.verdict is
  'PASS/FAIL/WARN/SKIPPED；只表示 replay/evaluation 结果，不是交易建议。';
comment on column qdr_regression_verdict.severity is
  'finding 最高严重度；沿用 B1 INFO/WARN/ERROR/BLOCKER。';
comment on column qdr_regression_verdict.finding_code is
  '聚合 finding code；不得保存敏感材料。';
comment on column qdr_regression_verdict.finding_message is
  '脱敏 finding 摘要；不得保存 raw prompt、raw provider response 或 credential。';
comment on column qdr_regression_verdict.failure_reason is
  '脱敏失败原因；FAIL 必须有 reason 或 finding code。';
comment on column qdr_regression_verdict.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_regression_verdict.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_regression_finding is
  'stage-qdr-4 B2 regression finding 表。tenant-bound；not executable trading signal；no raw prompt；no raw provider response；no credential。';
comment on column qdr_regression_finding.id is
  '主键 UUID；regression finding identity。';
comment on column qdr_regression_finding.tenant_id is
  '租户 ID；所有保存、查询和引用必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_regression_finding.case_id is
  '租户内 replay case ID。';
comment on column qdr_regression_finding.evaluation_id is
  '租户内 evaluation ID。';
comment on column qdr_regression_finding.verdict_id is
  '租户内 verdict ID；通过 tenant-bound FK 关联 qdr_regression_verdict。';
comment on column qdr_regression_finding.source_decision_id is
  '来源 decision 引用；不对 V6/V8 source 表强 FK。';
comment on column qdr_regression_finding.source_request_id is
  '来源 request 引用；用于 tenant-bound 审计反查。';
comment on column qdr_regression_finding.trace_id is
  'traceId；用于 regression finding 审计反查。';
comment on column qdr_regression_finding.request_id is
  'requestId；审计对账字段，不表示 NQ runtime 已连接。';
comment on column qdr_regression_finding.policy_version is
  'finding 对应 policy version。';
comment on column qdr_regression_finding.model_gateway_version_ref is
  'model gateway version 安全引用；不代表 real HTTP 或 provider 已接入。';
comment on column qdr_regression_finding.input_ref_id is
  'tenant-bound input ref FK，可为空。';
comment on column qdr_regression_finding.output_ref_id is
  'tenant-bound output ref FK，可为空，不保存 provider 原始响应。';
comment on column qdr_regression_finding.expected_summary_id is
  'tenant-bound expected summary FK，可为空。';
comment on column qdr_regression_finding.actual_summary_id is
  'tenant-bound actual summary FK，可为空。';
comment on column qdr_regression_finding.expected_summary_hash is
  'expected summary SHA-256 hash；64 位 hex，可为空。';
comment on column qdr_regression_finding.actual_summary_hash is
  'actual summary SHA-256 hash；64 位 hex，可为空。';
comment on column qdr_regression_finding.verdict is
  'finding 所属 verdict；只表示 replay/evaluation 结果。';
comment on column qdr_regression_finding.severity is
  'finding 严重度；沿用 B1 INFO/WARN/ERROR/BLOCKER。';
comment on column qdr_regression_finding.finding_code is
  '结构化 finding code；不得保存敏感材料。';
comment on column qdr_regression_finding.finding_message is
  '脱敏 finding 说明；不得保存 raw prompt、raw provider response 或 credential。';
comment on column qdr_regression_finding.evidence_ref is
  '脱敏 evidence ref；只保存引用，不保存原始 payload。';
comment on column qdr_regression_finding.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_regression_finding.updated_at is
  '更新时间，使用 timestamptz。';
