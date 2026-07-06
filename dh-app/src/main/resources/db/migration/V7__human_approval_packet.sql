-- stage-qdr-2 B3: Human Approval Packet.
-- 新增 DH 内部人工审查证据表；不新增 approval API、不触发 NQ、不触发交易、不接真实 HTTP / provider / Agent runtime。
-- APPROVED 不是 BUY，REJECTED 不是 SELL，LONG_BIAS / SHORT_BIAS 仍只是只读方向性意见。

create table if not exists human_approval_packet (
  id uuid primary key,
  decision_run_id uuid not null,
  tenant_id varchar not null,
  trace_id varchar not null,
  request_id varchar not null,
  approval_key varchar not null,
  approval_type varchar not null,
  approval_status varchar not null,
  risk_level varchar not null,
  decision_action varchar not null,
  confidence_score numeric(5,4),
  summary text,
  checklist_json jsonb not null,
  evidence_refs_json jsonb,
  reviewer_id varchar,
  reviewer_note text,
  decided_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_human_approval_packet_tenant_key unique (tenant_id, approval_key),
  constraint fk_human_approval_packet_decision_run
    foreign key (decision_run_id) references decision_run(id),
  constraint chk_human_approval_packet_status
    check (approval_status in (
      'PENDING',
      'APPROVED',
      'REJECTED',
      'NEEDS_REVIEW',
      'EXPIRED'
    )),
  constraint chk_human_approval_packet_type
    check (approval_type in (
      'QUANT_DECISION_REVIEW',
      'RISK_REVIEW',
      'STRATEGY_RELEASE_REVIEW',
      'ANOMALY_REVIEW'
    )),
  constraint chk_human_approval_packet_decision_action
    check (decision_action in (
      'OBSERVE',
      'NO_TRADE',
      'LONG_BIAS',
      'SHORT_BIAS',
      'NEEDS_REVIEW',
      'REJECTED'
    )),
  constraint chk_human_approval_packet_no_executable_action
    check (decision_action not in (
      'BUY',
      'SELL',
      'PLACE_ORDER',
      'CANCEL_ORDER',
      'MARKET_ORDER',
      'LIMIT_ORDER',
      'MUTATE_NQ_STATE',
      'EXECUTE_ORDER',
      'APPROVED_AS_BUY',
      'REJECTED_AS_SELL'
    )),
  constraint chk_human_approval_packet_risk_level
    check (risk_level in ('LOW', 'MEDIUM', 'HIGH', 'BLOCKED', 'UNKNOWN')),
  constraint chk_human_approval_packet_confidence
    check (confidence_score is null or (confidence_score >= 0 and confidence_score <= 1))
);

create index if not exists idx_human_approval_packet_tenant_created_at
  on human_approval_packet(tenant_id, created_at);
create index if not exists idx_human_approval_packet_decision_run_id
  on human_approval_packet(decision_run_id);
create index if not exists idx_human_approval_packet_status
  on human_approval_packet(tenant_id, approval_status, updated_at);
create index if not exists idx_human_approval_packet_trace_id
  on human_approval_packet(trace_id);
create index if not exists idx_human_approval_packet_request_id
  on human_approval_packet(request_id);

comment on table human_approval_packet is
  'stage-qdr-2 B3 DH 内部人工审查证据表。该表不是交易授权表，不触发 NQ、订单、风控、账本、Paper 或 LIVE mutation。';
comment on column human_approval_packet.id is
  '主键 UUID；标识一份 DH 内部 human approval packet。';
comment on column human_approval_packet.decision_run_id is
  '关联 decision_run.id；审批包必须归属于已存在的 QDR run。';
comment on column human_approval_packet.tenant_id is
  '租户 ID；所有查询和更新必须 tenant-bound，不允许 UUID-only 访问。';
comment on column human_approval_packet.trace_id is
  '链路 traceId；用于串联 request、run、trace、evidence 与人工审查记录。';
comment on column human_approval_packet.request_id is
  '请求 ID；用于审计对账，不代表 NQ runtime 已连接。';
comment on column human_approval_packet.approval_key is
  '租户内审批幂等 key；unique(tenant_id, approval_key) 防止重复审批包。';
comment on column human_approval_packet.approval_type is
  '审批类型；仅表示 DH 内部人工审查类别，不授权交易。';
comment on column human_approval_packet.approval_status is
  '审批状态；APPROVED 不等于 BUY，REJECTED 不等于 SELL，状态变化不触发外部副作用。';
comment on column human_approval_packet.risk_level is
  '风险等级；对齐 QDR RiskLevel，只用于审查和 fail-closed 判断。';
comment on column human_approval_packet.decision_action is
  '只读 decision action；不允许 BUY、SELL、PLACE_ORDER、CANCEL_ORDER 或任何可执行交易动作。';
comment on column human_approval_packet.confidence_score is
  '置信分数，范围 0 到 1；可为空，不代表交易执行概率。';
comment on column human_approval_packet.summary is
  '脱敏人工审查摘要；不得保存 credential、token、cookie、apiKey、apiSecret、passphrase、raw provider response 或 raw prompt。';
comment on column human_approval_packet.checklist_json is
  '脱敏人工审查 checklist JSON；不得保存 credential、token、cookie、apiKey、apiSecret、passphrase、raw provider response 或 raw prompt。';
comment on column human_approval_packet.evidence_refs_json is
  '脱敏 evidence 引用 JSON；只保存引用或摘要，不保存 raw provider response、raw prompt 或凭证材料。';
comment on column human_approval_packet.reviewer_id is
  '人工审查人标识；可为空，未决状态不得自动通过。';
comment on column human_approval_packet.reviewer_note is
  '人工审查备注；不得保存 credential、token、cookie、apiKey、apiSecret 或 passphrase。';
comment on column human_approval_packet.decided_at is
  '审批决策时间；PENDING 状态为空，终态应由状态机写入。';
comment on column human_approval_packet.created_at is
  '审批包创建时间，使用 timestamptz。';
comment on column human_approval_packet.updated_at is
  '审批包更新时间，使用 timestamptz。';
