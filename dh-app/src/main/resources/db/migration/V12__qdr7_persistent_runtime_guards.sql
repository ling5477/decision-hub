-- Stage-QDR-7 B2: limited dry-run persistent multi-instance guards.
-- 本 migration 只新增 DH 自身安全门禁表和 result exact-reference 约束；不修改 V1-V11，
-- 不新增 API、外部 HTTP、Provider、NQ、Agent/LangGraph、交易、Paper 或 LIVE 能力。

alter table dh_decision_output
  add constraint ux_dh_decision_output_tenant_decision
  unique (tenant_id, decision_id);

create table dh_qdr7_rate_limit_bucket (
  environment varchar(16) not null,
  endpoint varchar(128) not null,
  source varchar(64) not null,
  tenant_id varchar(128) not null,
  window_start timestamptz not null,
  window_end timestamptz not null,
  window_seconds integer not null,
  limit_value integer not null,
  request_count bigint not null,
  created_at timestamptz not null default transaction_timestamp(),
  updated_at timestamptz not null default transaction_timestamp(),
  constraint pk_dh_qdr7_rate_limit_bucket
    primary key (environment, endpoint, source, tenant_id, window_start),
  constraint chk_dh_qdr7_rate_identity
    check (
      environment in ('dev', 'test', 'staging', 'prod')
      and endpoint = '/api/ai/decision-dry-runs'
      and source = 'NQ_DRYRUN'
      and btrim(tenant_id) = tenant_id
      and tenant_id <> ''
    ),
  constraint chk_dh_qdr7_rate_window
    check (
      window_seconds > 0
      and window_end > window_start
      and window_end = window_start + make_interval(secs => window_seconds)
    ),
  constraint chk_dh_qdr7_rate_counter
    check (
      limit_value > 0
      and request_count >= 1
      and request_count <= limit_value
      and request_count <= 2147483647
    )
);

create index idx_dh_qdr7_rate_cleanup
  on dh_qdr7_rate_limit_bucket(
    window_end, environment, endpoint, source, tenant_id, window_start
  );

comment on table dh_qdr7_rate_limit_bucket is
  'Stage-QDR-7 fixed-window persistent rate-limit bucket。完整 key 包含 environment/endpoint/source/tenant/window；只保存计数和时间，不保存 request payload、签名、nonce、credential 或交易材料。';
comment on column dh_qdr7_rate_limit_bucket.window_start is
  '由 PostgreSQL transaction_timestamp() 按 UTC epoch 对齐得出的窗口身份；application clock 不参与。';
comment on column dh_qdr7_rate_limit_bucket.request_count is
  '当前窗口已原子接纳的请求数；达到 limit_value 后 conditional upsert 不再递增。';

create table dh_qdr7_idempotency_guard (
  guard_id uuid primary key,
  environment varchar(16) not null,
  endpoint varchar(128) not null,
  source varchar(64) not null,
  tenant_id varchar(128) not null,
  request_id varchar(128) not null,
  request_hash char(64) not null,
  hash_version varchar(32) not null,
  state varchar(16) not null,
  state_version bigint not null default 0,
  lease_token uuid,
  lease_expires_at timestamptz,
  result_id varchar(128),
  result_checksum char(64),
  stable_error_code varchar(128),
  created_at timestamptz not null default transaction_timestamp(),
  updated_at timestamptz not null default transaction_timestamp(),
  completed_at timestamptz,
  expires_at timestamptz not null,
  retention_until timestamptz not null,
  constraint ux_dh_qdr7_idempotency_identity
    unique (environment, endpoint, source, tenant_id, request_id),
  constraint ux_dh_qdr7_idempotency_tenant_guard
    unique (tenant_id, guard_id),
  constraint fk_dh_qdr7_idempotency_result
    foreign key (tenant_id, result_id)
      references dh_decision_output(tenant_id, decision_id),
  constraint chk_dh_qdr7_idempotency_identity
    check (
      environment in ('dev', 'test', 'staging', 'prod')
      and endpoint = '/api/ai/decision-dry-runs'
      and source = 'NQ_DRYRUN'
      and btrim(tenant_id) = tenant_id
      and tenant_id <> ''
      and btrim(request_id) = request_id
      and request_id <> ''
    ),
  constraint chk_dh_qdr7_idempotency_hash
    check (
      request_hash ~ '^[0-9a-f]{64}$'
      and hash_version = 'QDR7-DRYRUN-CJSON-1'
    ),
  constraint chk_dh_qdr7_idempotency_state
    check (state in ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'EXPIRED')),
  constraint chk_dh_qdr7_idempotency_version
    check (state_version >= 0),
  constraint chk_dh_qdr7_idempotency_retention
    check (expires_at >= created_at and retention_until >= expires_at),
  constraint chk_dh_qdr7_idempotency_state_fields
    check (
      (
        state = 'RECEIVED'
        and lease_token is null and lease_expires_at is null
        and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is null
      )
      or (
        state = 'IN_PROGRESS'
        and lease_token is not null and lease_expires_at is not null
        and lease_expires_at > updated_at
        and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is null
      )
      or (
        state = 'COMPLETED'
        and lease_token is null and lease_expires_at is null
        and result_id is not null and btrim(result_id) <> ''
        and result_checksum ~ '^[0-9a-f]{64}$'
        and stable_error_code is null and completed_at is not null
      )
      or (
        state = 'FAILED'
        and lease_token is null and lease_expires_at is null
        and result_id is null and result_checksum is null
        and stable_error_code is not null and btrim(stable_error_code) <> ''
        and completed_at is not null
      )
      or (
        state = 'EXPIRED'
        and lease_token is null and lease_expires_at is null
        and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is not null
      )
    )
);

create index idx_dh_qdr7_idempotency_cleanup
  on dh_qdr7_idempotency_guard(
    retention_until, environment, endpoint, source, tenant_id, request_id
  )
  where state in ('COMPLETED', 'FAILED', 'EXPIRED');

create index idx_dh_qdr7_idempotency_expired_lease
  on dh_qdr7_idempotency_guard(
    lease_expires_at, environment, endpoint, source, tenant_id, request_id
  )
  where state = 'IN_PROGRESS';

comment on table dh_qdr7_idempotency_guard is
  'Stage-QDR-7 persistent idempotency state machine。只保存完整 identity、SHA-256、state/lease、safe result reference或stable error；禁止 raw body、canonical request、prompt、provider response、signature、nonce、authorization、credential或交易执行材料。';
comment on column dh_qdr7_idempotency_guard.request_hash is
  'SHA-256("QDR7-DRYRUN-CJSON-1\\n" + canonicalRequestBytes) lowercase hex；不保存canonical bytes。';
comment on column dh_qdr7_idempotency_guard.lease_token is
  'IN_PROGRESS内部opaque CAS token；不得返回客户端或写入日志。';
comment on column dh_qdr7_idempotency_guard.result_id is
  'COMPLETED时tenant-bound exact reference到既有insert-only dh_decision_output.decision_id；不新增第三张结果表。';
