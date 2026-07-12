-- Stage-QDR-7 B2 forward-only安全修复：补齐lease/result/terminal时间语义，保留identity tombstone。
-- 历史V12行按既有状态显式回填；无法满足V12自身状态不变量的行会使migration失败，不静默吞并。

alter table dh_qdr7_idempotency_guard
  add column lease_owner varchar(128),
  add column result_type varchar(64),
  add column failed_at timestamptz,
  add column expired_at timestamptz;

update dh_qdr7_idempotency_guard
set lease_owner = 'legacy-v12'
where state = 'IN_PROGRESS';

update dh_qdr7_idempotency_guard
set result_type = 'DH_DECISION_OUTPUT'
where state = 'COMPLETED';

update dh_qdr7_idempotency_guard
set failed_at = completed_at, completed_at = null
where state = 'FAILED';

update dh_qdr7_idempotency_guard
set expired_at = completed_at, completed_at = null
where state = 'EXPIRED';

alter table dh_qdr7_idempotency_guard
  drop constraint chk_dh_qdr7_idempotency_state_fields;

alter table dh_qdr7_idempotency_guard
  add constraint chk_dh_qdr7_idempotency_lease_owner
    check (lease_owner is null or (btrim(lease_owner) = lease_owner and length(lease_owner) between 1 and 128)),
  add constraint chk_dh_qdr7_idempotency_result_type
    check (result_type is null or result_type = 'DH_DECISION_OUTPUT'),
  add constraint chk_dh_qdr7_idempotency_error_code
    check (stable_error_code is null or (btrim(stable_error_code) = stable_error_code and length(stable_error_code) between 1 and 128)),
  add constraint chk_dh_qdr7_idempotency_state_fields
    check (
      (state = 'RECEIVED'
        and lease_owner is null and lease_token is null and lease_expires_at is null
        and result_type is null and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is null and failed_at is null and expired_at is null)
      or
      (state = 'IN_PROGRESS'
        and lease_owner is not null and lease_token is not null and lease_expires_at is not null
        and lease_expires_at > updated_at
        and result_type is null and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is null and failed_at is null and expired_at is null)
      or
      (state = 'COMPLETED'
        and lease_owner is null and lease_token is null and lease_expires_at is null
        and result_type = 'DH_DECISION_OUTPUT'
        and result_id is not null and btrim(result_id) <> ''
        and result_checksum ~ '^[0-9a-f]{64}$'
        and stable_error_code is null and completed_at is not null and failed_at is null and expired_at is null)
      or
      (state = 'FAILED'
        and lease_owner is null and lease_token is null and lease_expires_at is null
        and result_type is null and result_id is null and result_checksum is null
        and stable_error_code is not null and failed_at is not null
        and completed_at is null and expired_at is null)
      or
      (state = 'EXPIRED'
        and lease_owner is null and lease_token is null and lease_expires_at is null
        and result_type is null and result_id is null and result_checksum is null
        and stable_error_code is null and completed_at is null and failed_at is null and expired_at is not null)
    );

drop index idx_dh_qdr7_idempotency_cleanup;
create index idx_dh_qdr7_idempotency_cleanup
  on dh_qdr7_idempotency_guard(
    environment, endpoint, source, retention_until, expires_at, lease_expires_at, guard_id
  )
  where state <> 'EXPIRED';

comment on column dh_qdr7_idempotency_guard.lease_owner is
  'IN_PROGRESS低敏感内部实例标识；不得包含凭证、IP秘密、用户数据，不得进入日志或外部response。';
comment on column dh_qdr7_idempotency_guard.result_type is
  'COMPLETED固定为DH_DECISION_OUTPUT，用于校验既有tenant-bound exact result reference。';
comment on column dh_qdr7_idempotency_guard.failed_at is
  'FAILED状态的PostgreSQL transaction_timestamp()；不再复用completed_at。';
comment on column dh_qdr7_idempotency_guard.expired_at is
  'EXPIRED identity tombstone的PostgreSQL transaction_timestamp()；本阶段禁止物理删除。';
