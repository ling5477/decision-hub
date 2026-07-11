-- Stage-QDR-6 B3-P1: canonical replay snapshot persistence baseline.
-- 本 migration 仅新增 tenant-bound immutable snapshot schema；不修改 V1-V9 migration，
-- 不回填 legacy row，不新增 API/JDBC/runtime wiring，也不实现 canonicalizer、hash 或 replay。

alter table dh_decision_request
  add constraint ux_dh_decision_request_tenant_decision
  unique (tenant_id, decision_id);

alter table decision_request
  add constraint ux_decision_request_tenant_id
  unique (tenant_id, id);

alter table decision_run
  add constraint ux_decision_run_id_request
  unique (id, decision_request_id);

alter table qdr_prompt_version
  add constraint ux_qdr_prompt_version_tenant_id
  unique (tenant_id, id);

alter table qdr_model_version
  add constraint ux_qdr_model_version_tenant_id
  unique (tenant_id, id);

alter table qdr_model_gateway_call
  add constraint ux_qdr_model_gateway_call_tenant_wide_identity
  unique (tenant_id, id, decision_run_id, prompt_version_id, model_version_id);

create table qdr_canonical_replay_snapshot (
  id uuid primary key,
  tenant_id varchar(128) not null,
  snapshot_id varchar(128) not null,
  decision_id varchar(128) not null,
  decision_request_id uuid not null,
  decision_run_id uuid not null,
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  source varchar(128) not null,
  decision_type varchar(64) not null,
  source_captured_at timestamptz not null,

  model_call_id uuid not null,
  model_call_ref varchar(512) not null,
  prompt_version_id uuid not null,
  model_version_id uuid not null,
  replay_case_row_id uuid not null,
  replay_case_id varchar(128) not null,
  evaluation_case_row_id uuid,
  evaluation_case_id varchar(128),
  regression_verdict_row_id uuid,
  regression_verdict_id varchar(128),

  subject_json jsonb not null,
  context_payload_json jsonb not null,
  evidence_refs_json jsonb not null,
  replay_input_ref_json jsonb not null,
  expected_decision_summary_json jsonb not null,

  snapshot_schema_version varchar(64) not null,
  decision_schema_version varchar(64) not null,
  context_schema_version varchar(64) not null,
  policy_version varchar(128) not null,
  evaluation_policy_version varchar(128) not null,
  prompt_version_ref varchar(256) not null,
  prompt_version_checksum varchar(64) not null,
  model_version_ref varchar(256) not null,
  model_version_checksum varchar(64) not null,
  model_gateway_version_ref varchar(128) not null,
  canonicalization_version varchar(64) not null,
  replay_executor_version varchar(64) not null,
  hash_algorithm_version varchar(32) not null,

  replay_input_hash varchar(64) not null,
  expected_summary_hash varchar(64) not null,
  provider_summary_hash varchar(64),
  canonical_input_hash varchar(64) not null,
  payload_bytes integer not null,
  created_at timestamptz not null default now(),

  constraint ux_qdr_canonical_snapshot_tenant_id
    unique (tenant_id, id),
  constraint ux_qdr_canonical_snapshot_tenant_snapshot
    unique (tenant_id, snapshot_id),
  constraint ux_qdr_canonical_snapshot_aggregate_identity
    unique (tenant_id, decision_run_id, replay_case_row_id, snapshot_schema_version),

  constraint fk_qdr_canonical_snapshot_v5_decision
    foreign key (tenant_id, decision_id)
      references dh_decision_request(tenant_id, decision_id),
  constraint fk_qdr_canonical_snapshot_v6_request
    foreign key (tenant_id, decision_request_id)
      references decision_request(tenant_id, id),
  constraint fk_qdr_canonical_snapshot_v6_run_request
    foreign key (decision_run_id, decision_request_id)
      references decision_run(id, decision_request_id),
  constraint fk_qdr_canonical_snapshot_v8_prompt
    foreign key (tenant_id, prompt_version_id)
      references qdr_prompt_version(tenant_id, id),
  constraint fk_qdr_canonical_snapshot_v8_model
    foreign key (tenant_id, model_version_id)
      references qdr_model_version(tenant_id, id),
  constraint fk_qdr_canonical_snapshot_v8_call
    foreign key (tenant_id, model_call_id, decision_run_id, prompt_version_id, model_version_id)
      references qdr_model_gateway_call(
        tenant_id, id, decision_run_id, prompt_version_id, model_version_id
      ),
  constraint fk_qdr_canonical_snapshot_v9_replay_case
    foreign key (tenant_id, replay_case_row_id)
      references qdr_replay_case(tenant_id, id),
  constraint fk_qdr_canonical_snapshot_v9_evaluation_case
    foreign key (tenant_id, evaluation_case_row_id)
      references qdr_evaluation_case(tenant_id, id),
  constraint fk_qdr_canonical_snapshot_v9_regression_verdict
    foreign key (tenant_id, regression_verdict_row_id)
      references qdr_regression_verdict(tenant_id, id),

  constraint chk_qdr_canonical_snapshot_decision_type
    check (decision_type = 'READ_ONLY_RECOMMENDATION'),
  constraint chk_qdr_canonical_snapshot_fixed_versions
    check (
      snapshot_schema_version = 'QDR6-REPLAY-INPUT-1'
      and context_schema_version = 'QDR6-CONTEXT-1'
      and canonicalization_version = 'QDR6-CJSON-1'
      and replay_executor_version = 'QDR6-MOCK-REPLAY-1'
      and hash_algorithm_version = 'SHA-256'
    ),
  constraint chk_qdr_canonical_snapshot_required_text
    check (
      btrim(tenant_id) <> ''
      and btrim(snapshot_id) <> ''
      and btrim(decision_id) <> ''
      and btrim(trace_id) <> ''
      and btrim(request_id) <> ''
      and btrim(source) <> ''
      and btrim(model_call_ref) <> ''
      and btrim(replay_case_id) <> ''
      and btrim(decision_schema_version) <> ''
      and btrim(policy_version) <> ''
      and btrim(evaluation_policy_version) <> ''
      and btrim(prompt_version_ref) <> ''
      and btrim(model_version_ref) <> ''
      and btrim(model_gateway_version_ref) <> ''
    ),
  constraint chk_qdr_canonical_snapshot_no_version_alias
    check (
      lower(btrim(decision_schema_version)) not in ('latest', 'current', 'default')
      and lower(btrim(policy_version)) not in ('latest', 'current', 'default')
      and lower(btrim(evaluation_policy_version)) not in ('latest', 'current', 'default')
      and lower(btrim(prompt_version_ref)) not in ('latest', 'current', 'default')
      and lower(btrim(model_version_ref)) not in ('latest', 'current', 'default')
      and lower(btrim(model_gateway_version_ref)) not in ('latest', 'current', 'default')
    ),
  constraint chk_qdr_canonical_snapshot_lineage_pairs
    check (
      ((evaluation_case_row_id is null) = (evaluation_case_id is null))
      and ((regression_verdict_row_id is null) = (regression_verdict_id is null))
    ),
  constraint chk_qdr_canonical_snapshot_hashes
    check (
      prompt_version_checksum ~ '^[0-9a-f]{64}$'
      and model_version_checksum ~ '^[0-9a-f]{64}$'
      and replay_input_hash ~ '^[0-9a-f]{64}$'
      and expected_summary_hash ~ '^[0-9a-f]{64}$'
      and canonical_input_hash ~ '^[0-9a-f]{64}$'
      and (provider_summary_hash is null or provider_summary_hash ~ '^[0-9a-f]{64}$')
    ),
  constraint chk_qdr_canonical_snapshot_json_shape
    check (
      jsonb_typeof(subject_json) = 'object'
      and subject_json ?& array['symbol', 'market', 'timeframe']
      and subject_json - array['symbol', 'market', 'timeframe', 'strategyRef', 'researchRef'] = '{}'::jsonb
      and jsonb_typeof(context_payload_json) = 'object'
      and context_payload_json ?& array['snapshotId', 'capturedAt', 'evidenceRefs']
      and context_payload_json - array['snapshotId', 'capturedAt', 'evidenceRefs'] = '{}'::jsonb
      and jsonb_typeof(evidence_refs_json) = 'array'
      and jsonb_array_length(evidence_refs_json) > 0
      and jsonb_typeof(replay_input_ref_json) = 'object'
      and replay_input_ref_json ?& array['refType', 'refId', 'contentHash']
      and replay_input_ref_json - array['refType', 'refId', 'contentHash'] = '{}'::jsonb
      and jsonb_typeof(expected_decision_summary_json) = 'object'
      and expected_decision_summary_json ?& array[
        'decisionType',
        'actionLabel',
        'confidenceBand',
        'riskLevel',
        'requiredEvidenceRefs',
        'forbiddenActions'
      ]
      and expected_decision_summary_json - array[
        'decisionType',
        'actionLabel',
        'confidenceBand',
        'riskLevel',
        'requiredEvidenceRefs',
        'forbiddenActions'
      ] = '{}'::jsonb
    ),
  constraint chk_qdr_canonical_snapshot_no_unsafe_top_level_keys
    check (
      not (subject_json ?| array[
        'rawPrompt', 'raw_prompt', 'promptText', 'rawProviderResponse',
        'raw_provider_response', 'providerRaw', 'credential', 'apiKey',
        'apiSecret', 'passphrase', 'token', 'cookie', 'secret'
      ])
      and not (context_payload_json ?| array[
        'rawPrompt', 'raw_prompt', 'promptText', 'rawProviderResponse',
        'raw_provider_response', 'providerRaw', 'credential', 'apiKey',
        'apiSecret', 'passphrase', 'token', 'cookie', 'secret'
      ])
      and not (replay_input_ref_json ?| array[
        'rawPrompt', 'raw_prompt', 'promptText', 'rawProviderResponse',
        'raw_provider_response', 'providerRaw', 'credential', 'apiKey',
        'apiSecret', 'passphrase', 'token', 'cookie', 'secret'
      ])
      and not (expected_decision_summary_json ?| array[
        'rawPrompt', 'raw_prompt', 'promptText', 'rawProviderResponse',
        'raw_provider_response', 'providerRaw', 'credential', 'apiKey',
        'apiSecret', 'passphrase', 'token', 'cookie', 'secret'
      ])
    ),
  constraint chk_qdr_canonical_snapshot_payload_limits
    check (
      payload_bytes between 1 and 262144
      and octet_length(context_payload_json::text) <= 131072
      and octet_length(evidence_refs_json::text) <= 65536
      and octet_length(expected_decision_summary_json::text) <= 32768
      and octet_length(subject_json::text)
        + octet_length(context_payload_json::text)
        + octet_length(evidence_refs_json::text)
        + octet_length(replay_input_ref_json::text)
        + octet_length(expected_decision_summary_json::text) <= payload_bytes
    )
);

create index idx_qdr_canonical_snapshot_tenant_run
  on qdr_canonical_replay_snapshot(tenant_id, decision_run_id);
create index idx_qdr_canonical_snapshot_tenant_decision
  on qdr_canonical_replay_snapshot(tenant_id, decision_id);
create index idx_qdr_canonical_snapshot_tenant_model_call
  on qdr_canonical_replay_snapshot(tenant_id, model_call_ref);
create index idx_qdr_canonical_snapshot_tenant_replay_case
  on qdr_canonical_replay_snapshot(tenant_id, replay_case_id);

create function reject_qdr_canonical_replay_snapshot_update()
returns trigger
language plpgsql
as $$
begin
  raise exception using
    errcode = '55000',
    message = 'qdr_canonical_replay_snapshot is immutable; UPDATE is forbidden';
end;
$$;

create trigger trg_qdr_canonical_replay_snapshot_reject_update
before update on qdr_canonical_replay_snapshot
for each row execute function reject_qdr_canonical_replay_snapshot_update();

comment on table qdr_canonical_replay_snapshot is
  'Stage-QDR-6 canonical replay snapshot。tenant-bound、immutable、audit-only；只保存结构化安全材料，不保存 raw prompt、raw provider response、凭证或交易指令。';
comment on column qdr_canonical_replay_snapshot.id is '物理 UUID；不得脱离 tenant_id 单独查询。';
comment on column qdr_canonical_replay_snapshot.tenant_id is '第一安全边界；所有写入、读取和引用必须 tenant-bound。';
comment on column qdr_canonical_replay_snapshot.snapshot_id is '租户内 immutable snapshot business ID。';
comment on column qdr_canonical_replay_snapshot.decision_id is 'V5 decision identity；通过 tenant-aware FK 校验。';
comment on column qdr_canonical_replay_snapshot.decision_request_id is 'V6 request 物理 UUID；不替代 request_id。';
comment on column qdr_canonical_replay_snapshot.decision_run_id is 'V6 run 物理 UUID；必须与 decision_request_id 成对匹配。';
comment on column qdr_canonical_replay_snapshot.trace_id is '跨 V5/V6/V8/V9 exact correlation traceId。';
comment on column qdr_canonical_replay_snapshot.request_id is '跨 source exact correlation requestId。';
comment on column qdr_canonical_replay_snapshot.source is '只读 decision source；不表示 NQ runtime 已连接。';
comment on column qdr_canonical_replay_snapshot.decision_type is '固定 READ_ONLY_RECOMMENDATION，不是交易信号。';
comment on column qdr_canonical_replay_snapshot.source_captured_at is '来源快照时间；不是当前时钟或 execution time。';
comment on column qdr_canonical_replay_snapshot.model_call_id is 'V8 gateway call 物理 UUID。';
comment on column qdr_canonical_replay_snapshot.model_call_ref is 'V8 gateway call 安全业务引用，不保存 provider response。';
comment on column qdr_canonical_replay_snapshot.prompt_version_id is 'V8 immutable prompt version UUID，不保存 prompt 正文。';
comment on column qdr_canonical_replay_snapshot.model_version_id is 'V8 immutable model version UUID，不保存 credential。';
comment on column qdr_canonical_replay_snapshot.replay_case_row_id is 'V9 replay case 物理 UUID。';
comment on column qdr_canonical_replay_snapshot.replay_case_id is 'V9 replay case 租户内 business ID。';
comment on column qdr_canonical_replay_snapshot.evaluation_case_row_id is '可选 V9 evaluation 物理 UUID。';
comment on column qdr_canonical_replay_snapshot.evaluation_case_id is '可选 V9 evaluation business ID；与 row UUID 同时为空或同时存在。';
comment on column qdr_canonical_replay_snapshot.regression_verdict_row_id is '可选 V9 regression verdict 物理 UUID。';
comment on column qdr_canonical_replay_snapshot.regression_verdict_id is '可选 V9 verdict business ID；与 row UUID 同时为空或同时存在。';
comment on column qdr_canonical_replay_snapshot.subject_json is '严格 allowlist 的结构化 subject；禁止账户、订单、凭证或执行字段。';
comment on column qdr_canonical_replay_snapshot.context_payload_json is '严格 allowlist 的 immutable context snapshot；禁止 raw material。';
comment on column qdr_canonical_replay_snapshot.evidence_refs_json is 'typed safe refs 数组；只保存 ref/hash，不保存 evidence payload。';
comment on column qdr_canonical_replay_snapshot.replay_input_ref_json is 'V9 structured replay input ref；不保存输入原文。';
comment on column qdr_canonical_replay_snapshot.expected_decision_summary_json is 'V9 structured expected summary；不包含可执行交易动作。';
comment on column qdr_canonical_replay_snapshot.snapshot_schema_version is '固定 QDR6-REPLAY-INPUT-1。';
comment on column qdr_canonical_replay_snapshot.decision_schema_version is 'V5 decision schema version；禁止 latest/current/default。';
comment on column qdr_canonical_replay_snapshot.context_schema_version is '固定 QDR6-CONTEXT-1。';
comment on column qdr_canonical_replay_snapshot.policy_version is 'decision policy immutable version。';
comment on column qdr_canonical_replay_snapshot.evaluation_policy_version is 'evaluation policy immutable version。';
comment on column qdr_canonical_replay_snapshot.prompt_version_ref is 'prompt immutable version ref；不保存 prompt 正文。';
comment on column qdr_canonical_replay_snapshot.prompt_version_checksum is 'prompt version lowercase SHA-256 checksum。';
comment on column qdr_canonical_replay_snapshot.model_version_ref is 'model immutable version ref；不表示 real provider。';
comment on column qdr_canonical_replay_snapshot.model_version_checksum is 'model version lowercase SHA-256 checksum。';
comment on column qdr_canonical_replay_snapshot.model_gateway_version_ref is 'model gateway semantic version ref；不得从 model ID 推断。';
comment on column qdr_canonical_replay_snapshot.canonicalization_version is '未来 canonicalizer compatibility version；P1 不实现算法。';
comment on column qdr_canonical_replay_snapshot.replay_executor_version is '未来 mock replay executor compatibility version；P1 不实现 executor。';
comment on column qdr_canonical_replay_snapshot.hash_algorithm_version is '未来 deterministic hash compatibility label；P1 不实现 hash。';
comment on column qdr_canonical_replay_snapshot.replay_input_hash is 'V9 已存在 safe input hash metadata；不保存原文。';
comment on column qdr_canonical_replay_snapshot.expected_summary_hash is 'V9 expected summary lowercase SHA-256 hash。';
comment on column qdr_canonical_replay_snapshot.provider_summary_hash is '可选 provider summary hash；不保存 provider 原始响应。';
comment on column qdr_canonical_replay_snapshot.canonical_input_hash is '预先校验的 canonical input hash metadata；P1 不负责生成。';
comment on column qdr_canonical_replay_snapshot.payload_bytes is '结构化 snapshot 总字节预算；最大 262144 bytes。';
comment on column qdr_canonical_replay_snapshot.created_at is '数据库 audit-only 创建时间；不得进入 snapshot identity 或 deterministic hash。';
comment on function reject_qdr_canonical_replay_snapshot_update() is
  '拒绝 canonical snapshot UPDATE；历史审计数据只能 append，不允许原地变更。';
comment on trigger trg_qdr_canonical_replay_snapshot_reject_update
  on qdr_canonical_replay_snapshot is
  'immutable guard：任何 UPDATE 均以 SQLSTATE 55000 fail-closed。';
