-- Stage-QDR-7 B2 V13 pre-compatibility callback.
-- 仅在既有V12 guard表存在且V13尚未成功时修复V12允许、V13新增trim CHECK拒绝的FAILED错误码。
-- 旧V12 CHECK在V13将FAILED/EXPIRED的completed_at迁移到typed timestamp之前会阻断行更新；本callback
-- 只临时替换该约束，V13随后以同名最终CHECK覆盖。除FAILED error code的btrim外不修改业务字段。

do $$
begin
  if to_regclass('public.dh_qdr7_idempotency_guard') is null then
    return;
  end if;

  if exists (
    select 1
    from flyway_schema_history
    where success = true and version = '13'
  ) then
    return;
  end if;

  if exists (
    select 1
    from pg_constraint
    where conrelid = 'public.dh_qdr7_idempotency_guard'::regclass
      and conname = 'chk_dh_qdr7_idempotency_state_fields'
  ) then
    alter table dh_qdr7_idempotency_guard
      drop constraint chk_dh_qdr7_idempotency_state_fields;
    alter table dh_qdr7_idempotency_guard
      add constraint chk_dh_qdr7_idempotency_state_fields
      check (state in ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'EXPIRED'));
  end if;

  if exists (
    select 1
    from dh_qdr7_idempotency_guard
    where state = 'FAILED'
      and nullif(btrim(stable_error_code), '') is null
  ) then
    raise exception using
      errcode = '23514',
      message = 'qdr7 V13 compatibility rejected blank FAILED stable_error_code';
  end if;

  update dh_qdr7_idempotency_guard
  set stable_error_code = btrim(stable_error_code)
  where state = 'FAILED'
    and stable_error_code is distinct from btrim(stable_error_code);
end;
$$;
