create table if not exists dh_bootstrap_check (
  id bigint primary key,
  created_at timestamptz not null default now()
);
