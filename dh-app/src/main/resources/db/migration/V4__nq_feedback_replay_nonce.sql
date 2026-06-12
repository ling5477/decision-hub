-- DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1: NQ feedback 防重放 nonce 持久化表。
-- 只新增 DH 自身安全能力层表；不引入任何订单 / 成交 / 仓位 / 实盘执行相关表。
-- 使用 if not exists，单脚本可重复执行不破坏现有数据；不修改历史 V1–V3。
--
-- 设计依据：DH_P1_4_RESIDUAL_FIX_PLAN.md §4.2 候选 A（PostgreSQL-backed NonceReplayGuard）。
-- 列口径与 NonceReplayGuard.markIfAbsent(replayKey, expiresAt) 端口一致：端口只提供
-- replayKey（= source::nonce::requestId）与 expiresAt，故本表只持久化 replay_key + expires_at，
-- 不拆分 source/tenant/request/nonce 列（端口未提供独立 tenant，拆分会引入不可靠解析）。
-- 严禁存储 raw request / raw response / signature 原材料 / authorization / api key / secret /
-- token / cookie / credential / prompt / full context。

create table if not exists dh_nq_replay_nonce (
  replay_key varchar(512) primary key,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

-- 过期清理（惰性 WHERE expires_at < now() 或定时 sweep）依赖 expires_at 范围扫描，建索引避免全表扫描。
create index if not exists idx_dh_nq_replay_nonce_expires on dh_nq_replay_nonce(expires_at);

comment on table dh_nq_replay_nonce is 'NQ feedback 防重放 nonce 持久化存储。markIfAbsent 用 INSERT ON CONFLICT DO NOTHING 原子登记 replay_key；多实例/重启后窗口内重放仍被拒。不存任何凭证/签名原材料/payload 明文。';
comment on column dh_nq_replay_nonce.replay_key is '防重放稳定 key，由 source::nonce::requestId 组合而成（HmacNqFeedbackAuthenticator 口径）；非敏感，但不写日志明文。';
comment on column dh_nq_replay_nonce.expires_at is 'replay key 过期时间（= now + 2×maxClockSkew，由认证层计算）。expires_at < now() 的行可被惰性/定时清理，过期前不得驱逐以保证防重放窗口。';
comment on column dh_nq_replay_nonce.created_at is '行写入时间（now()），仅用于审计与排查，不参与防重放判定。';
