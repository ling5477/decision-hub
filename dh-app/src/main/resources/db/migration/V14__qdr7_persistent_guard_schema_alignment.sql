-- Stage-QDR-7 B2 forward-only schema alignment.
-- V13 result_type曾以varchar(64)创建；本migration在拒绝超长历史值后收窄到冻结的varchar(32)，不修改V1-V13。

do $$
begin
  if exists (
    select 1
    from dh_qdr7_idempotency_guard
    where result_type is not null and length(result_type) > 32
  ) then
    raise exception using
      errcode = '22001',
      message = 'qdr7 V14 rejected result_type longer than frozen varchar(32)';
  end if;
end;
$$;

alter table dh_qdr7_idempotency_guard
  alter column result_type type varchar(32)
  using result_type::varchar(32);

comment on column dh_qdr7_idempotency_guard.result_type is
  'COMPLETED固定为DH_DECISION_OUTPUT；V14与冻结schema保持varchar(32)，用于tenant-bound exact result reference。';
