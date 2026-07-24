-- Stage-QDR-9 B1: immutable structured feedback persistence schema baseline.
-- Forward-only migration: only adds four tenant/environment-bound tables, constraints and indexes.
-- It does not alter V1-V14, reuse dh_nq_feedback_events, add runtime wiring, or persist raw payloads.

create table qdr_feedback_outcome_observation (
  id uuid not null,
  tenant_id varchar(128) not null,
  environment varchar(16) not null,
  decision_id varchar(128) not null,
  trace_id varchar(128) not null,
  observation_id varchar(128) not null,
  idempotency_key char(64) not null,
  outcome_source varchar(32) not null,
  outcome_status varchar(32) not null,
  observed_at timestamptz not null,
  evaluation_time timestamptz not null,
  canonical_hash char(64) not null,
  created_at timestamptz not null default transaction_timestamp(),
  constraint pk_qdr_feedback_outcome_observation
    primary key (id),
  constraint ux_qdr_feedback_observation_scope
    unique (tenant_id, environment, observation_id),
  constraint ux_qdr_feedback_observation_idempotency
    unique (tenant_id, environment, idempotency_key),
  constraint ux_qdr_feedback_observation_projection
    unique (
      tenant_id,
      environment,
      observation_id,
      decision_id,
      trace_id,
      observed_at
    ),
  constraint chk_qdr_feedback_observation_identity
    check (
      btrim(tenant_id) = tenant_id and tenant_id <> ''
      and btrim(decision_id) = decision_id and decision_id <> ''
      and btrim(trace_id) = trace_id and trace_id <> ''
      and btrim(observation_id) = observation_id and observation_id <> ''
    ),
  constraint chk_qdr_feedback_observation_environment
    check (environment in ('DEV', 'TEST')),
  constraint chk_qdr_feedback_observation_source
    check (
      outcome_source in (
        'DRY_RUN_RESULT',
        'DETERMINISTIC_REPLAY',
        'STRUCTURED_TEST_FIXTURE'
      )
    ),
  constraint chk_qdr_feedback_observation_status
    check (outcome_status in ('SUCCEEDED', 'PARTIALLY_SUCCEEDED', 'FAILED')),
  constraint chk_qdr_feedback_observation_time
    check (observed_at <= evaluation_time),
  constraint chk_qdr_feedback_observation_hashes
    check (
      idempotency_key ~ '^[0-9a-f]{64}$'
      and canonical_hash ~ '^[0-9a-f]{64}$'
    )
);

create table qdr_feedback_attribution (
  id uuid not null,
  tenant_id varchar(128) not null,
  environment varchar(16) not null,
  observation_id varchar(128) not null,
  decision_id varchar(128) not null,
  trace_id varchar(128) not null,
  observed_at timestamptz not null,
  attribution_id char(64) not null,
  policy_id varchar(128) not null,
  policy_version varchar(128) not null,
  attribution_status varchar(32) not null,
  confidence numeric(6,5) not null,
  canonical_hash char(64) not null,
  error_code varchar(64) not null,
  created_at timestamptz not null default transaction_timestamp(),
  constraint pk_qdr_feedback_attribution
    primary key (id),
  constraint ux_qdr_feedback_attribution_scope
    unique (tenant_id, environment, attribution_id),
  constraint fk_qdr_feedback_attribution_observation
    foreign key (
      tenant_id,
      environment,
      observation_id,
      decision_id,
      trace_id,
      observed_at
    )
    references qdr_feedback_outcome_observation (
      tenant_id,
      environment,
      observation_id,
      decision_id,
      trace_id,
      observed_at
    ),
  constraint chk_qdr_feedback_attribution_identity
    check (
      btrim(tenant_id) = tenant_id and tenant_id <> ''
      and btrim(observation_id) = observation_id and observation_id <> ''
      and btrim(decision_id) = decision_id and decision_id <> ''
      and btrim(trace_id) = trace_id and trace_id <> ''
      and btrim(policy_id) = policy_id and policy_id <> ''
      and btrim(policy_version) = policy_version and policy_version <> ''
    ),
  constraint chk_qdr_feedback_attribution_environment
    check (environment in ('DEV', 'TEST')),
  constraint chk_qdr_feedback_attribution_status
    check (attribution_status in ('ATTRIBUTED', 'INCONCLUSIVE', 'REJECTED')),
  constraint chk_qdr_feedback_attribution_confidence
    check (confidence between 0 and 1),
  constraint chk_qdr_feedback_attribution_hashes
    check (
      attribution_id ~ '^[0-9a-f]{64}$'
      and canonical_hash ~ '^[0-9a-f]{64}$'
    ),
  constraint chk_qdr_feedback_attribution_error
    check (error_code ~ '^[A-Z][A-Z0-9_]{0,63}$')
);

create table qdr_feedback_attribution_contribution (
  id uuid not null,
  tenant_id varchar(128) not null,
  environment varchar(16) not null,
  attribution_id char(64) not null,
  dimension varchar(64) not null,
  measurement numeric(6,5) not null,
  contribution numeric(6,5) not null,
  impact varchar(16) not null,
  confidence numeric(6,5) not null,
  reason_code varchar(64) not null,
  evidence_ref varchar(256) not null,
  sort_order smallint not null,
  created_at timestamptz not null default transaction_timestamp(),
  constraint pk_qdr_feedback_attribution_contribution
    primary key (id),
  constraint ux_qdr_feedback_contribution_dimension
    unique (tenant_id, environment, attribution_id, dimension),
  constraint ux_qdr_feedback_contribution_order
    unique (tenant_id, environment, attribution_id, sort_order),
  constraint fk_qdr_feedback_contribution_attribution
    foreign key (tenant_id, environment, attribution_id)
    references qdr_feedback_attribution (tenant_id, environment, attribution_id)
    on delete cascade,
  constraint chk_qdr_feedback_contribution_identity
    check (
      btrim(tenant_id) = tenant_id and tenant_id <> ''
      and btrim(evidence_ref) = evidence_ref and evidence_ref <> ''
    ),
  constraint chk_qdr_feedback_contribution_environment
    check (environment in ('DEV', 'TEST')),
  constraint chk_qdr_feedback_contribution_dimension
    check (
      dimension in (
        'EVIDENCE_QUALITY',
        'DECISION_CONSISTENCY',
        'RISK_DISCIPLINE',
        'OUTCOME_STABILITY'
      )
    ),
  constraint chk_qdr_feedback_contribution_measurement
    check (measurement between -1 and 1),
  constraint chk_qdr_feedback_contribution_value
    check (contribution between -1 and 1),
  constraint chk_qdr_feedback_contribution_confidence
    check (confidence between 0 and 1),
  constraint chk_qdr_feedback_contribution_impact
    check (
      (contribution > 0 and impact = 'POSITIVE')
      or (contribution = 0 and impact = 'NEUTRAL')
      or (contribution < 0 and impact = 'NEGATIVE')
    ),
  constraint chk_qdr_feedback_contribution_reason
    check (reason_code ~ '^[A-Z][A-Z0-9_]{0,63}$'),
  constraint chk_qdr_feedback_contribution_order
    check (sort_order >= 0 and sort_order < 32)
);

create table qdr_feedback_attribution_reference (
  id uuid not null,
  tenant_id varchar(128) not null,
  environment varchar(16) not null,
  attribution_id char(64) not null,
  reference_type varchar(16) not null,
  reference_value varchar(256) not null,
  reference_status varchar(16) not null,
  created_at timestamptz not null default transaction_timestamp(),
  constraint pk_qdr_feedback_attribution_reference
    primary key (id),
  constraint ux_qdr_feedback_reference_identity
    unique (
      tenant_id,
      environment,
      attribution_id,
      reference_type,
      reference_value
    ),
  constraint fk_qdr_feedback_reference_attribution
    foreign key (tenant_id, environment, attribution_id)
    references qdr_feedback_attribution (tenant_id, environment, attribution_id)
    on delete cascade,
  constraint chk_qdr_feedback_reference_identity
    check (
      btrim(tenant_id) = tenant_id and tenant_id <> ''
      and btrim(reference_value) = reference_value and reference_value <> ''
    ),
  constraint chk_qdr_feedback_reference_environment
    check (environment in ('DEV', 'TEST')),
  constraint chk_qdr_feedback_reference_type
    check (reference_type in ('AUDIT', 'REPLAY', 'EVALUATION', 'EVIDENCE')),
  constraint chk_qdr_feedback_reference_status
    check (reference_status in ('ACTIVE', 'RELEASED', 'INVALID')),
  constraint chk_qdr_feedback_reference_scheme
    check (
      (reference_type = 'AUDIT' and reference_value ~ '^audit:[A-Za-z0-9._:-]{1,250}$')
      or (
        reference_type = 'REPLAY'
        and reference_value
          ~ '^(replay-case|canonical-snapshot|replay):[A-Za-z0-9._:-]{1,230}$'
      )
      or (
        reference_type = 'EVALUATION'
        and reference_value ~ '^evaluation:[A-Za-z0-9._:-]{1,245}$'
      )
      or (
        reference_type = 'EVIDENCE'
        and reference_value ~ '^evidence:[A-Za-z0-9._:-]{1,247}$'
      )
    )
);

create index idx_qdr_feedback_observation_decision
  on qdr_feedback_outcome_observation (
    tenant_id,
    environment,
    decision_id,
    observed_at desc,
    observation_id desc
  );

create index idx_qdr_feedback_observation_trace
  on qdr_feedback_outcome_observation (
    tenant_id,
    environment,
    trace_id,
    observed_at desc,
    observation_id desc
  );

create index idx_qdr_feedback_observation_time
  on qdr_feedback_outcome_observation (
    tenant_id,
    environment,
    observed_at desc,
    observation_id desc
  );

create index idx_qdr_feedback_attribution_observation
  on qdr_feedback_attribution (tenant_id, environment, observation_id);

create index idx_qdr_feedback_attribution_policy
  on qdr_feedback_attribution (
    tenant_id,
    environment,
    policy_version,
    observed_at desc,
    attribution_id desc
  );

create index idx_qdr_feedback_attribution_status
  on qdr_feedback_attribution (
    tenant_id,
    environment,
    attribution_status,
    observed_at desc,
    attribution_id desc
  );

create index idx_qdr_feedback_attribution_time
  on qdr_feedback_attribution (
    tenant_id,
    environment,
    observed_at desc,
    attribution_id desc
  );

create index idx_qdr_feedback_reference_hold
  on qdr_feedback_attribution_reference (
    tenant_id,
    environment,
    reference_type,
    reference_status,
    created_at,
    attribution_id
  );

comment on table qdr_feedback_outcome_observation is
  'Stage-QDR-9 immutable structured outcome observation。Tenant/environment bound；不保存JSONB、canonical raw value、prompt、provider response、credential或交易指令。';
comment on column qdr_feedback_outcome_observation.idempotency_key is
  'Tenant/environment业务幂等SHA-256；只保存lowercase hash，不保存canonical input。';
comment on column qdr_feedback_outcome_observation.canonical_hash is
  'Stage-QDR-8完整规范输入的lowercase SHA-256；不保存规范输入原文。';
comment on column qdr_feedback_outcome_observation.observed_at is
  '结构化结果发生时间；必须早于或等于显式evaluation_time。';

comment on table qdr_feedback_attribution is
  'Stage-QDR-9 immutable deterministic attribution projection。通过复合FK固定tenant/environment/decision/trace/observedAt关联。';
comment on column qdr_feedback_attribution.attribution_id is
  '已完成audit的deterministic result identity SHA-256。';
comment on column qdr_feedback_attribution.error_code is
  'Stage-QDR-8稳定错误分类；不包含异常栈、SQL、连接信息或原始payload。';

comment on table qdr_feedback_attribution_contribution is
  'Stage-QDR-9按封闭dimension和sort_order稳定排序的有界贡献；只保存安全evidence引用。';
comment on column qdr_feedback_attribution_contribution.evidence_ref is
  '与Stage-QDR-8 observation/contribution一致的有界安全引用；不保存原始证据payload。';
comment on column qdr_feedback_attribution_contribution.sort_order is
  'Aggregate内连续稳定顺序；范围0至31。';

comment on table qdr_feedback_attribution_reference is
  'Stage-QDR-9 scheme-bound安全引用。B1只创建ACTIVE；B4前不执行release、invalid标记或physical delete。';
comment on column qdr_feedback_attribution_reference.reference_value is
  'Tenant-bound target identity；不保存外部payload、credential或tenantless lookup结果。';
comment on column qdr_feedback_attribution_reference.reference_status is
  'ACTIVE/RELEASED/INVALID生命周期；初次写入只允许ACTIVE由应用合同保证。';
