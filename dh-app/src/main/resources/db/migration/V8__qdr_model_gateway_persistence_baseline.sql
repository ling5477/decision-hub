-- stage-qdr-3 B3: Prompt / Model Version / Model Gateway persistence baseline.
-- 本 migration 只新增 DH 内部 QDR model gateway 持久化基线；不修改 V1-V7，不新增 API，不触发 provider、HTTP、NQ、交易或 LIVE。
-- 持久化层只保存 hash/ref/redacted summary；raw prompt、raw provider response 与 credential material 均禁止入库。

create table if not exists qdr_prompt_template (
  id uuid primary key,
  tenant_id varchar not null,
  template_key varchar not null,
  display_name varchar not null,
  current_version_id uuid,
  status varchar not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_prompt_template_tenant_key unique (tenant_id, template_key),
  constraint chk_qdr_prompt_template_status
    check (status in ('DRAFT', 'ACTIVE', 'DEPRECATED', 'DISABLED'))
);

create table if not exists qdr_prompt_version (
  id uuid primary key,
  tenant_id varchar not null,
  prompt_template_id uuid not null,
  version varchar not null,
  render_policy_key varchar not null,
  template_ref varchar not null,
  template_hash varchar(64) not null,
  redacted_summary text not null,
  status varchar not null,
  checksum varchar(64) not null,
  created_at timestamptz not null,
  created_by varchar not null,
  constraint ux_qdr_prompt_version_tenant_template_version
    unique (tenant_id, prompt_template_id, version),
  constraint fk_qdr_prompt_version_template
    foreign key (prompt_template_id) references qdr_prompt_template(id),
  constraint chk_qdr_prompt_version_status
    check (status in ('DRAFT', 'ACTIVE', 'DEPRECATED', 'DISABLED')),
  constraint chk_qdr_prompt_version_template_hash
    check (template_hash ~ '^[0-9a-f]{64}$'),
  constraint chk_qdr_prompt_version_checksum
    check (checksum ~ '^[0-9a-f]{64}$')
);

create table if not exists qdr_model_profile (
  id uuid primary key,
  tenant_id varchar not null,
  provider_profile_id uuid not null,
  provider_kind varchar not null,
  provider_key varchar not null,
  model_key varchar not null,
  display_name varchar not null,
  capability_summary text not null,
  context_window_tokens integer not null,
  max_output_tokens integer not null,
  profile_status varchar not null,
  trust_policy_ref varchar,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_qdr_model_profile_tenant_model_key unique (tenant_id, model_key),
  constraint chk_qdr_model_profile_provider_kind
    check (provider_kind in ('MOCK', 'LOCAL_PLANNED')),
  constraint chk_qdr_model_profile_status
    check (profile_status in ('ENABLED', 'DISABLED', 'PLANNED')),
  constraint chk_qdr_model_profile_token_bounds
    check (context_window_tokens > 0 and max_output_tokens > 0 and max_output_tokens <= context_window_tokens)
);

create table if not exists qdr_model_version (
  id uuid primary key,
  tenant_id varchar not null,
  model_profile_id uuid not null,
  model_name varchar not null,
  model_version varchar not null,
  capability_summary text not null,
  version_status varchar not null,
  checksum varchar(64) not null,
  created_at timestamptz not null,
  constraint ux_qdr_model_version_tenant_profile_name_version
    unique (tenant_id, model_profile_id, model_name, model_version),
  constraint fk_qdr_model_version_profile
    foreign key (model_profile_id) references qdr_model_profile(id),
  constraint chk_qdr_model_version_status
    check (version_status in ('ACTIVE', 'DEPRECATED', 'DISABLED')),
  constraint chk_qdr_model_version_checksum
    check (checksum ~ '^[0-9a-f]{64}$')
);

create table if not exists qdr_model_gateway_call (
  id uuid primary key,
  tenant_id varchar not null,
  trace_id varchar not null,
  request_id varchar not null,
  decision_run_id uuid not null,
  prompt_version_id uuid not null,
  model_version_id uuid not null,
  provider_profile_id uuid not null,
  provider_kind varchar not null,
  provider_identity_ref varchar not null,
  status varchar not null,
  failure_code varchar,
  trust_decision varchar not null,
  provider_trust_decision_ref varchar not null,
  model_call_ref varchar not null,
  budget_summary text not null,
  input_characters integer not null,
  rendered_prompt_characters integer not null,
  output_characters integer not null,
  estimated_tokens integer not null,
  memory_entries integer not null,
  redacted_input_summary text not null,
  redacted_output_summary text,
  input_hash varchar(64) not null,
  output_hash varchar(64),
  audit_ref varchar,
  trace_ref varchar,
  created_at timestamptz not null,
  constraint ux_qdr_model_gateway_call_tenant_call_ref unique (tenant_id, model_call_ref),
  constraint fk_qdr_model_gateway_call_decision_run
    foreign key (decision_run_id) references decision_run(id),
  constraint fk_qdr_model_gateway_call_prompt_version
    foreign key (prompt_version_id) references qdr_prompt_version(id),
  constraint fk_qdr_model_gateway_call_model_version
    foreign key (model_version_id) references qdr_model_version(id),
  constraint chk_qdr_model_gateway_call_status
    check (status in ('SUCCEEDED', 'FAILED')),
  constraint chk_qdr_model_gateway_call_provider_kind
    check (provider_kind in ('MOCK', 'LOCAL_PLANNED')),
  constraint chk_qdr_model_gateway_call_trust_decision
    check (trust_decision in ('ALLOWED', 'DENIED')),
  constraint chk_qdr_model_gateway_call_failure_code
    check (
      failure_code is null or failure_code in (
        'MISSING_REQUIRED_FIELD',
        'PROMPT_VERSION_NOT_FOUND',
        'MODEL_VERSION_NOT_FOUND',
        'REGISTRY_MISMATCH',
        'PROMPT_DENIED',
        'PROMPT_RENDER_FAILED',
        'POLICY_DENIED',
        'UNKNOWN_PROVIDER',
        'PROVIDER_DISABLED',
        'REAL_PROVIDER_FORBIDDEN',
        'PROVIDER_UNAVAILABLE',
        'PROVIDER_TIMEOUT',
        'BUDGET_EXCEEDED',
        'REDACTION_FAILED',
        'PROVIDER_OUTPUT_INVALID',
        'UNKNOWN_ERROR'
      )
    ),
  constraint chk_qdr_model_gateway_call_failure_shape
    check ((status = 'SUCCEEDED' and failure_code is null) or (status = 'FAILED' and failure_code is not null)),
  constraint chk_qdr_model_gateway_call_hashes
    check (input_hash ~ '^[0-9a-f]{64}$' and (output_hash is null or output_hash ~ '^[0-9a-f]{64}$')),
  constraint chk_qdr_model_gateway_call_budget_non_negative
    check (
      input_characters >= 0
      and rendered_prompt_characters >= 0
      and output_characters >= 0
      and estimated_tokens >= 0
      and memory_entries >= 0
    )
);

create index if not exists idx_qdr_prompt_template_tenant_created_at
  on qdr_prompt_template(tenant_id, created_at);
create index if not exists idx_qdr_prompt_version_tenant_template
  on qdr_prompt_version(tenant_id, prompt_template_id, version);
create index if not exists idx_qdr_prompt_version_checksum
  on qdr_prompt_version(tenant_id, checksum);
create index if not exists idx_qdr_model_profile_tenant_provider
  on qdr_model_profile(tenant_id, provider_profile_id, provider_kind);
create index if not exists idx_qdr_model_version_tenant_profile
  on qdr_model_version(tenant_id, model_profile_id, model_name, model_version);
create index if not exists idx_qdr_model_version_checksum
  on qdr_model_version(tenant_id, checksum);
create index if not exists idx_qdr_model_gateway_call_tenant_decision_run
  on qdr_model_gateway_call(tenant_id, decision_run_id, created_at);
create index if not exists idx_qdr_model_gateway_call_trace_id
  on qdr_model_gateway_call(trace_id);
create index if not exists idx_qdr_model_gateway_call_request_id
  on qdr_model_gateway_call(request_id);
create index if not exists idx_qdr_model_gateway_call_prompt_version
  on qdr_model_gateway_call(tenant_id, prompt_version_id);
create index if not exists idx_qdr_model_gateway_call_model_version
  on qdr_model_gateway_call(tenant_id, model_version_id);
create index if not exists idx_qdr_model_gateway_call_status
  on qdr_model_gateway_call(tenant_id, status, created_at);

comment on table qdr_prompt_template is
  'stage-qdr-3 B3 prompt template metadata 表。只保存 tenant、key、current version ref 与状态，不保存 raw prompt。';
comment on column qdr_prompt_template.id is
  '主键 UUID；prompt template identity。';
comment on column qdr_prompt_template.tenant_id is
  '租户 ID；所有查询和写入必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_prompt_template.template_key is
  '租户内 prompt template key；unique(tenant_id, template_key)。';
comment on column qdr_prompt_template.display_name is
  '脱敏展示名；不得保存 credential material。';
comment on column qdr_prompt_template.current_version_id is
  '当前 prompt version ref；B3 baseline 不通过该字段授权 runtime provider call。';
comment on column qdr_prompt_template.status is
  'template 状态；禁用状态必须 fail-closed。';
comment on column qdr_prompt_template.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_prompt_template.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_prompt_version is
  'stage-qdr-3 B3 immutable prompt version 表。禁止保存 raw prompt，只允许 template_ref、hash 与 redacted summary。';
comment on column qdr_prompt_version.id is
  '主键 UUID；prompt version identity。';
comment on column qdr_prompt_version.tenant_id is
  '租户 ID；所有查询和写入必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_prompt_version.prompt_template_id is
  '关联 qdr_prompt_template.id；prompt_version 必须归属 prompt_template。';
comment on column qdr_prompt_version.version is
  'immutable prompt version 字符串；同 tenant/template/version 不允许不同 checksum 覆盖。';
comment on column qdr_prompt_version.render_policy_key is
  'render policy key；只保存策略引用，不保存 prompt body。';
comment on column qdr_prompt_version.template_ref is
  'template 安全引用；不得保存 raw prompt。';
comment on column qdr_prompt_version.template_hash is
  'template 内容 hash；64 位 SHA-256 hex，不保存原文。';
comment on column qdr_prompt_version.redacted_summary is
  '脱敏 prompt 摘要；不得保存 raw prompt、raw provider response 或 credential material。';
comment on column qdr_prompt_version.status is
  'prompt version 状态；DISABLED 必须 fail-closed。';
comment on column qdr_prompt_version.checksum is
  'immutable checksum；mismatch 必须 fail-closed，不允许更新覆盖。';
comment on column qdr_prompt_version.created_at is
  '创建时间；prompt version immutable，不提供 updated_at。';
comment on column qdr_prompt_version.created_by is
  '创建人或系统标识；不得保存 credential material。';

comment on table qdr_model_profile is
  'stage-qdr-3 B3 model profile 表。只保存 provider identity ref、model capability 与 trust policy ref，不保存 credential。';
comment on column qdr_model_profile.id is
  '主键 UUID；model profile identity。';
comment on column qdr_model_profile.tenant_id is
  '租户 ID；所有查询和写入必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_model_profile.provider_profile_id is
  'provider profile identity ref；不是 credential，也不是真实 provider SDK 配置。';
comment on column qdr_model_profile.provider_kind is
  'provider 类型；B3 仅允许 MOCK / LOCAL_PLANNED，不代表 real provider 已接入。';
comment on column qdr_model_profile.provider_key is
  'provider identity key；不得保存 credential、API key、secret、token 或 passphrase。';
comment on column qdr_model_profile.model_key is
  '租户内 model key；unique(tenant_id, model_key)。';
comment on column qdr_model_profile.display_name is
  '脱敏展示名。';
comment on column qdr_model_profile.capability_summary is
  '脱敏 capability 摘要；不得保存 provider raw response。';
comment on column qdr_model_profile.context_window_tokens is
  '本地预算上限参考；不代表真实 provider billing。';
comment on column qdr_model_profile.max_output_tokens is
  '本地输出上限参考；必须小于等于 context_window_tokens。';
comment on column qdr_model_profile.profile_status is
  'profile 状态；DISABLED / PLANNED 必须 fail-closed。';
comment on column qdr_model_profile.trust_policy_ref is
  'ProviderTrustPolicy 安全引用；不保存策略原始执行上下文或 credential。';
comment on column qdr_model_profile.created_at is
  '创建时间，使用 timestamptz。';
comment on column qdr_model_profile.updated_at is
  '更新时间，使用 timestamptz。';

comment on table qdr_model_version is
  'stage-qdr-3 B3 immutable model version 表。只保存 model metadata、capability summary 与 checksum，不保存 credential。';
comment on column qdr_model_version.id is
  '主键 UUID；model version identity。';
comment on column qdr_model_version.tenant_id is
  '租户 ID；所有查询和写入必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_model_version.model_profile_id is
  '关联 qdr_model_profile.id；model_version 必须归属 model_profile。';
comment on column qdr_model_version.model_name is
  'model name；只保存 identity metadata，不保存 provider endpoint 或 SDK client。';
comment on column qdr_model_version.model_version is
  'provider/model version 字符串；同 tenant/profile/name/version 不允许不同 checksum 覆盖。';
comment on column qdr_model_version.capability_summary is
  '脱敏 capability 摘要；不得保存 raw provider response。';
comment on column qdr_model_version.version_status is
  'model version 状态；DISABLED 必须 fail-closed。';
comment on column qdr_model_version.checksum is
  'immutable checksum；mismatch 必须 fail-closed，不允许更新覆盖。';
comment on column qdr_model_version.created_at is
  '创建时间；model version immutable，不提供 updated_at。';

comment on table qdr_model_gateway_call is
  'stage-qdr-3 B3 model gateway call metadata 表。只保存 redacted metadata、hash/ref、budget/usage summary；禁止保存 raw prompt、raw provider response、credential 或真实 provider payload。';
comment on column qdr_model_gateway_call.id is
  '主键 UUID；model gateway call identity。';
comment on column qdr_model_gateway_call.tenant_id is
  '租户 ID；所有查询和写入必须 tenant-bound，不允许 UUID-only 访问。';
comment on column qdr_model_gateway_call.trace_id is
  'traceId；用于串联 decision run、gateway call 与 audit evidence。';
comment on column qdr_model_gateway_call.request_id is
  'requestId；用于审计对账，不代表 NQ runtime 已连接。';
comment on column qdr_model_gateway_call.decision_run_id is
  '关联 decision_run.id；gateway call 必须归属于 QDR run。';
comment on column qdr_model_gateway_call.prompt_version_id is
  '关联 qdr_prompt_version.id；不保存 raw prompt。';
comment on column qdr_model_gateway_call.model_version_id is
  '关联 qdr_model_version.id；不保存 provider credential。';
comment on column qdr_model_gateway_call.provider_profile_id is
  'provider profile identity ref；不是 API key、secret、token 或 passphrase。';
comment on column qdr_model_gateway_call.provider_kind is
  'provider 类型；B3 仅允许 MOCK / LOCAL_PLANNED，不代表 real provider 已接入。';
comment on column qdr_model_gateway_call.provider_identity_ref is
  'provider identity 安全引用；不得保存 credential material。';
comment on column qdr_model_gateway_call.status is
  'gateway call 状态；失败必须携带结构化 failure_code。';
comment on column qdr_model_gateway_call.failure_code is
  '结构化 fail-closed code；不得保存原始异常消息或 provider raw response。';
comment on column qdr_model_gateway_call.trust_decision is
  'ProviderTrustPolicy 判定摘要；ALLOWED / DENIED，不代表真实 provider 已接入。';
comment on column qdr_model_gateway_call.provider_trust_decision_ref is
  'ProviderTrustPolicy 判定安全引用；不保存原始策略上下文。';
comment on column qdr_model_gateway_call.model_call_ref is
  '租户内 model call 幂等/审计引用；unique(tenant_id, model_call_ref)。';
comment on column qdr_model_gateway_call.budget_summary is
  '脱敏预算摘要；不代表真实 provider billing。';
comment on column qdr_model_gateway_call.input_characters is
  '本地估算 input 字符数；非真实 provider usage。';
comment on column qdr_model_gateway_call.rendered_prompt_characters is
  '本地估算 rendered prompt 字符数；不保存 rendered prompt 原文。';
comment on column qdr_model_gateway_call.output_characters is
  '本地估算 output 字符数；不保存 provider raw response。';
comment on column qdr_model_gateway_call.estimated_tokens is
  '本地估算 token；不代表真实 provider billing。';
comment on column qdr_model_gateway_call.memory_entries is
  '本地 memory/context entry 数量；不保存 raw memory payload。';
comment on column qdr_model_gateway_call.redacted_input_summary is
  '脱敏输入摘要；不得保存 raw prompt、credential 或可执行交易指令。';
comment on column qdr_model_gateway_call.redacted_output_summary is
  '脱敏输出摘要；不得保存 raw provider response、credential 或可执行交易指令。';
comment on column qdr_model_gateway_call.input_hash is
  '输入 hash；64 位 SHA-256 hex，不保存原文。';
comment on column qdr_model_gateway_call.output_hash is
  '输出 hash；64 位 SHA-256 hex，不保存原文，可为空。';
comment on column qdr_model_gateway_call.audit_ref is
  'audit 安全引用；不代表 replay execution API 已启动。';
comment on column qdr_model_gateway_call.trace_ref is
  'trace 安全引用；不保存 raw prompt 或 raw provider response。';
comment on column qdr_model_gateway_call.created_at is
  '创建时间，使用 timestamptz。';
