-- Stage-QDR-7 B2 V13 transactional pre-compatibility callback.
-- 仅在成功V12且尚无V13记录的唯一升级窗口运行。beforeEachMigrate与紧随其后的V13
-- 处于同一Flyway/PostgreSQL事务：任一DDL、DML、V13或history写入失败都会整体rollback。
-- V13/V14已完成、V12尚未成功的环境只读取flyway_schema_history后立即返回，不访问guard业务表。

do $callback$
declare
  v_v12_total bigint;
  v_v12_success bigint;
  v_v13_total bigint;
  v_v13_success bigint;
  v_v14_total bigint;
  v_v14_success bigint;
  v_constraint_count bigint;
  v_constraint_valid boolean;
  v_constraint_def text;
  v_structure_def text;
  v_repair_count bigint;
  v_repaired_count bigint;
  v_expected_constraint_def constant text := $fingerprint$CHECK (((((state)::text = 'RECEIVED'::text) AND (lease_token IS NULL) AND (lease_expires_at IS NULL) AND (result_id IS NULL) AND (result_checksum IS NULL) AND (stable_error_code IS NULL) AND (completed_at IS NULL)) OR (((state)::text = 'IN_PROGRESS'::text) AND (lease_token IS NOT NULL) AND (lease_expires_at IS NOT NULL) AND (lease_expires_at > updated_at) AND (result_id IS NULL) AND (result_checksum IS NULL) AND (stable_error_code IS NULL) AND (completed_at IS NULL)) OR (((state)::text = 'COMPLETED'::text) AND (lease_token IS NULL) AND (lease_expires_at IS NULL) AND (result_id IS NOT NULL) AND (btrim((result_id)::text) <> ''::text) AND (result_checksum ~ '^[0-9a-f]{64}$'::text) AND (stable_error_code IS NULL) AND (completed_at IS NOT NULL)) OR (((state)::text = 'FAILED'::text) AND (lease_token IS NULL) AND (lease_expires_at IS NULL) AND (result_id IS NULL) AND (result_checksum IS NULL) AND (stable_error_code IS NOT NULL) AND (btrim((stable_error_code)::text) <> ''::text) AND (completed_at IS NOT NULL)) OR (((state)::text = 'EXPIRED'::text) AND (lease_token IS NULL) AND (lease_expires_at IS NULL) AND (result_id IS NULL) AND (result_checksum IS NULL) AND (stable_error_code IS NULL) AND (completed_at IS NOT NULL))))$fingerprint$;
  v_expected_structure_def constant text :=
      'guard_id:uuid:true|environment:character varying(16):true|endpoint:character varying(128):true|source:character varying(64):true|tenant_id:character varying(128):true|request_id:character varying(128):true|request_hash:character(64):true|hash_version:character varying(32):true|state:character varying(16):true|state_version:bigint:true|lease_token:uuid:false|lease_expires_at:timestamp with time zone:false|result_id:character varying(128):false|result_checksum:character(64):false|stable_error_code:character varying(128):false|created_at:timestamp with time zone:true|updated_at:timestamp with time zone:true|completed_at:timestamp with time zone:false|expires_at:timestamp with time zone:true|retention_until:timestamp with time zone:true';
begin
  -- Fresh schemas may run this callback before Flyway has created its history table.
  if to_regclass('public.flyway_schema_history') is null then
    return;
  end if;

  -- History is the first persistent object inspected. Duplicate or failed V13/V14 rows are
  -- ambiguous upgrade state and must fail closed before any business-table access.
  select
      count(*) filter (where version = '12'),
      count(*) filter (where version = '12' and success),
      count(*) filter (where version = '13'),
      count(*) filter (where version = '13' and success),
      count(*) filter (where version = '14'),
      count(*) filter (where version = '14' and success)
    into v_v12_total, v_v12_success, v_v13_total, v_v13_success, v_v14_total, v_v14_success
    from public.flyway_schema_history;

  if v_v12_total > 1 or v_v13_total > 1 or v_v14_total > 1 then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility rejected duplicate Flyway history version';
  end if;

  if v_v14_total = 1 then
    if v_v14_success = 1 then
      return;
    end if;
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility rejected failed V14 history state';
  end if;

  if v_v13_total = 1 then
    if v_v13_success = 1 then
      return;
    end if;
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility rejected failed V13 history state';
  end if;

  -- V12 has not succeeded on fresh and V1-V11 schemas, so this callback has no business work.
  if v_v12_total = 0 or v_v12_success = 0 then
    return;
  end if;

  -- Freeze both budgets immediately after the history-only no-op gates. From this point onward,
  -- every guard-table catalog lookup, data precheck, repair and DDL is transaction-bounded.
  set local lock_timeout = '5s';
  set local statement_timeout = '60s';

  if to_regclass('public.dh_qdr7_idempotency_guard') is null then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility expected V12 idempotency guard table';
  end if;

  -- Reject a partially changed table before repair or DDL. The ordered definition covers every
  -- V12 column, PostgreSQL type and nullability; V13 columns are therefore not silently accepted.
  select string_agg(
      attname || ':' || format_type(atttypid, atttypmod) || ':' || attnotnull,
      '|' order by attnum)
    into v_structure_def
    from pg_attribute
   where attrelid = 'public.dh_qdr7_idempotency_guard'::regclass
     and attnum > 0
     and not attisdropped;

  if v_structure_def is distinct from v_expected_structure_def then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility rejected non-V12 idempotency guard table structure';
  end if;

  -- A same-name constraint is insufficient: schema, table, CHECK type, validation bit and the
  -- PostgreSQL 17 deparsed V12 definition all form the immutable compatibility fingerprint.
  select
      count(*),
      bool_and(con.contype = 'c' and con.convalidated),
      max(pg_get_constraintdef(con.oid, false))
    into v_constraint_count, v_constraint_valid, v_constraint_def
    from pg_constraint con
    join pg_class rel on rel.oid = con.conrelid
    join pg_namespace nsp on nsp.oid = rel.relnamespace
   where nsp.nspname = 'public'
     and rel.relname = 'dh_qdr7_idempotency_guard'
     and con.conname = 'chk_dh_qdr7_idempotency_state_fields';

  if v_constraint_count <> 1
      or v_constraint_valid is distinct from true
      or v_constraint_def is distinct from v_expected_constraint_def then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility rejected V12 state CHECK fingerprint drift';
  end if;

  -- All legacy-data checks run before the first UPDATE or ALTER TABLE. V13 freezes the trimmed
  -- error code as nonblank and 1..128 characters; no default code is ever invented here.
  if exists (
      select 1
        from public.dh_qdr7_idempotency_guard
       where state = 'FAILED'
         and (
           stable_error_code is null
           or btrim(stable_error_code) = ''
           or char_length(btrim(stable_error_code)) not between 1 and 128
         )) then
    raise exception using
      errcode = '23514',
      message = 'qdr7 compatibility rejected invalid FAILED stable_error_code';
  end if;

  select count(*)
    into v_repair_count
    from public.dh_qdr7_idempotency_guard
   where state = 'FAILED'
     and stable_error_code is distinct from btrim(stable_error_code);

  if v_repair_count > 1000 then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility repair ceiling exceeded; offline governance required';
  end if;

  -- The primary-key ordered CTE confines mutation to the prechecked target set. Its row count
  -- must exactly match the count observed before any DDL, preventing partial repair semantics.
  with repair_target as (
      select guard_id
        from public.dh_qdr7_idempotency_guard
       where state = 'FAILED'
         and stable_error_code is distinct from btrim(stable_error_code)
       order by guard_id
       limit 1000
    ),
    repaired as (
      update public.dh_qdr7_idempotency_guard guard_row
         set stable_error_code = btrim(guard_row.stable_error_code)
        from repair_target target
       where guard_row.guard_id = target.guard_id
      returning guard_row.guard_id
    )
  select count(*) into v_repaired_count from repaired;

  if v_repaired_count <> v_repair_count then
    raise exception using
      errcode = 'P0001',
      message = 'qdr7 compatibility repair count changed during bounded update';
  end if;

  alter table public.dh_qdr7_idempotency_guard
    drop constraint chk_dh_qdr7_idempotency_state_fields;

  -- Keep the V12 state invariants intact while V13 moves terminal timestamps to failed_at and
  -- expired_at. Only the transient completed_at requirement for FAILED/EXPIRED is relaxed.
  alter table public.dh_qdr7_idempotency_guard
    add constraint chk_dh_qdr7_idempotency_state_fields
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
      )
      or (
        state = 'EXPIRED'
        and lease_token is null and lease_expires_at is null
        and result_id is null and result_checksum is null
        and stable_error_code is null
      )
    );
end;
$callback$;
