-- stage-qdr-1: Quant Decision Review Core Baseline.
-- 新增 DH 自身 Decision Core 主线表；不新增真实 provider、NQ runtime、交易、订单、账本、Paper 或 LIVE 能力。
-- LONG_BIAS / SHORT_BIAS 仅表示方向性审查意见，不得映射为 BUY / SELL。

create table if not exists decision_request (
  id uuid primary key,
  request_key varchar(128) not null,
  request_type varchar(128) not null,
  source_system varchar(128) not null,
  source_ref_id varchar(256),
  tenant_id varchar(128) not null,
  trace_id varchar(128) not null,
  request_id varchar(128) not null,
  input_payload_json jsonb not null,
  context_payload_json jsonb,
  status varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint ux_decision_request_tenant_key unique (tenant_id, request_key),
  constraint chk_decision_request_status
    check (status in ('ACCEPTED', 'REJECTED'))
);

create index if not exists idx_decision_request_tenant_created
  on decision_request(tenant_id, created_at);
create index if not exists idx_decision_request_trace
  on decision_request(trace_id);
create index if not exists idx_decision_request_request
  on decision_request(request_id);

comment on table decision_request is
  'stage-qdr-1 Quant Decision Review 主线请求表。只保存脱敏只读审查输入，不保存凭证、账户密钥或可执行订单 payload。';
comment on column decision_request.id is
  '主键 UUID；由 DH Decision Core 生成，不来自 NQ 订单或交易系统。';
comment on column decision_request.request_key is
  '租户内幂等 request key；stage-qdr-1 dry-run 使用 requestId。';
comment on column decision_request.request_type is
  '请求类型；stage-qdr-1 固定为 QUANT_DECISION_REVIEW。';
comment on column decision_request.source_system is
  '请求来源系统，例如 NQ_DRYRUN；必须经过 source allowlist 与 tenant/source binding。';
comment on column decision_request.source_ref_id is
  '来源侧引用 ID，可为空；仅用于审计关联，不表示 NQ runtime 已连接。';
comment on column decision_request.tenant_id is
  '租户 ID；所有 Decision Core 主线记录必须携带，用于隔离和查询。';
comment on column decision_request.trace_id is
  '链路 traceId；贯穿 request、run、signal、decision 与 V5 audit 链路。';
comment on column decision_request.request_id is
  '请求 ID；来自入站 dry-run envelope，用于审计对账和响应关联。';
comment on column decision_request.input_payload_json is
  '脱敏后的 request envelope 摘要 JSON，不包含 signature、credential、apiKey、apiSecret、passphrase 或 raw body。';
comment on column decision_request.context_payload_json is
  '脱敏后的 decisionContext 摘要 JSON，不包含账户、数量、价格、杠杆或订单字段。';
comment on column decision_request.status is
  '请求接收状态；仅允许 ACCEPTED / REJECTED，默认 fail-closed 语义。';
comment on column decision_request.created_at is
  '请求主线记录创建时间，使用 timestamptz。';
comment on column decision_request.updated_at is
  '请求主线记录更新时间，stage-qdr-1 创建时与 created_at 一致。';

create table if not exists decision_run (
  id uuid primary key,
  decision_request_id uuid not null references decision_request(id),
  run_no integer not null,
  status varchar(32) not null,
  orchestrator_key varchar(128),
  model_provider varchar(128),
  model_name varchar(128),
  started_at timestamptz not null,
  finished_at timestamptz,
  latency_ms bigint,
  error_code varchar(128),
  error_message text,
  created_at timestamptz not null,
  constraint ux_decision_run_request_run_no unique (decision_request_id, run_no),
  constraint chk_decision_run_status
    check (status in ('RUNNING', 'SUCCEEDED', 'FAILED')),
  constraint chk_decision_run_latency
    check (latency_ms is null or latency_ms >= 0)
);

create index if not exists idx_decision_run_status
  on decision_run(status);
create index if not exists idx_decision_run_started
  on decision_run(started_at);
create index if not exists idx_decision_run_request
  on decision_run(decision_request_id);

comment on table decision_run is
  'stage-qdr-1 Quant Decision Review run 表。当前仅记录 existing deterministic mock orchestrator，不接真实 provider 或 LangGraph runtime。';
comment on column decision_run.id is
  '主键 UUID；标识一次 Quant Decision Review run。';
comment on column decision_run.decision_request_id is
  '关联 decision_request.id；每次 run 必须归属于一个主线请求。';
comment on column decision_run.run_no is
  '同一 decision_request 下的运行序号；stage-qdr-1 固定从 1 开始。';
comment on column decision_run.status is
  'run 状态；仅允许 RUNNING / SUCCEEDED / FAILED，失败必须 fail-closed 记录。';
comment on column decision_run.orchestrator_key is
  '编排器标识；stage-qdr-1 使用 deterministic mock orchestrator。';
comment on column decision_run.model_provider is
  '预留字段；stage-qdr-1 不接 OpenAI / Anthropic / Gemini / Ollama 或任何 real provider。';
comment on column decision_run.model_name is
  '预留字段；stage-qdr-1 不接真实模型，通常为空。';
comment on column decision_run.started_at is
  'run 开始时间，使用 timestamptz。';
comment on column decision_run.finished_at is
  'run 完成或失败时间；RUNNING 状态可为空。';
comment on column decision_run.latency_ms is
  'run 耗时毫秒；必须为空或大于等于 0。';
comment on column decision_run.error_code is
  '失败错误码；成功时为空，失败时记录 fail-closed 原因。';
comment on column decision_run.error_message is
  '脱敏失败摘要；不得写入 raw body、签名材料、凭证或 provider 原始响应。';
comment on column decision_run.created_at is
  'run 记录创建时间，使用 timestamptz。';

create table if not exists quant_signal (
  id uuid primary key,
  decision_request_id uuid not null references decision_request(id),
  source_system varchar(128) not null,
  symbol varchar(128),
  exchange varchar(128),
  timeframe varchar(64),
  signal_type varchar(128) not null,
  signal_payload_json jsonb not null,
  strategy_id varchar(128),
  strategy_version varchar(128),
  dataset_version varchar(128),
  received_at timestamptz not null,
  created_at timestamptz not null
);

create index if not exists idx_quant_signal_source_received
  on quant_signal(source_system, received_at);
create index if not exists idx_quant_signal_symbol_timeframe
  on quant_signal(symbol, timeframe);
create index if not exists idx_quant_signal_request
  on quant_signal(decision_request_id);

comment on table quant_signal is
  'stage-qdr-1 Quant Decision Review 输入信号摘要表。signal_type 无法识别时写 UNKNOWN_REVIEW_INPUT。';
comment on column quant_signal.id is
  '主键 UUID；标识一次 QDR 输入信号摘要。';
comment on column quant_signal.decision_request_id is
  '关联 decision_request.id；信号摘要必须归属于一个主线请求。';
comment on column quant_signal.source_system is
  '输入来源系统，例如 NQ_DRYRUN；不代表真实 NQ runtime 已连接。';
comment on column quant_signal.symbol is
  '标的符号，可为空；仅作为审查上下文，不是下单标的授权。';
comment on column quant_signal.exchange is
  '市场或交易所上下文，可为空；不得保存交易所凭证。';
comment on column quant_signal.timeframe is
  '信号周期或时间框架，可为空。';
comment on column quant_signal.signal_type is
  '信号类型；无法识别时写 UNKNOWN_REVIEW_INPUT。';
comment on column quant_signal.signal_payload_json is
  '脱敏 signal/report/backtest/risk 摘要 JSON；不保存 credential、订单、数量、价格、杠杆或 raw provider response。';
comment on column quant_signal.strategy_id is
  '策略引用 ID，可为空；仅用于审查追踪，不表示策略状态 mutation。';
comment on column quant_signal.strategy_version is
  '策略版本，可为空；stage-qdr-1 仅预留，不驱动执行。';
comment on column quant_signal.dataset_version is
  '数据集版本，可为空；用于未来只读回放和审计。';
comment on column quant_signal.received_at is
  '输入信号接收或捕获时间，使用 timestamptz。';
comment on column quant_signal.created_at is
  '信号摘要记录创建时间，使用 timestamptz。';

create table if not exists quant_decision (
  id uuid primary key,
  quant_signal_id uuid references quant_signal(id),
  decision_run_id uuid not null references decision_run(id),
  action varchar(64) not null,
  confidence_score numeric(5,4),
  risk_level varchar(64) not null,
  rationale text,
  constraints_json jsonb,
  human_approval_status varchar(32) not null default 'NOT_REQUIRED',
  created_at timestamptz not null,
  constraint chk_quant_decision_action
    check (action in (
      'OBSERVE',
      'NO_TRADE',
      'LONG_BIAS',
      'SHORT_BIAS',
      'NEEDS_REVIEW',
      'REJECTED'
    )),
  constraint chk_quant_decision_no_executable_action
    check (action not in ('BUY', 'SELL', 'PLACE_ORDER', 'CANCEL_ORDER')),
  constraint chk_quant_decision_confidence
    check (confidence_score is null or (confidence_score >= 0 and confidence_score <= 1)),
  constraint chk_quant_decision_risk
    check (risk_level in ('LOW', 'MEDIUM', 'HIGH', 'BLOCKED', 'UNKNOWN')),
  constraint chk_quant_decision_human_approval_status
    check (human_approval_status in (
      'NOT_REQUIRED',
      'REQUIRED',
      'PENDING',
      'APPROVED',
      'REJECTED'
    ))
);

create index if not exists idx_quant_decision_run
  on quant_decision(decision_run_id);
create index if not exists idx_quant_decision_signal
  on quant_decision(quant_signal_id);
create index if not exists idx_quant_decision_created
  on quant_decision(created_at);

comment on table quant_decision is
  'stage-qdr-1 Quant Decision Review 只读决策表。不代表交易授权，不触发 BUY / SELL / PLACE_ORDER / CANCEL_ORDER。';
comment on column quant_decision.id is
  '主键 UUID；标识一次只读 Quant Decision Review 输出。';
comment on column quant_decision.quant_signal_id is
  '关联 quant_signal.id，可为空；表示该 decision 基于哪条输入信号摘要。';
comment on column quant_decision.decision_run_id is
  '关联 decision_run.id；每条 quant_decision 必须归属于一次 run。';
comment on column quant_decision.action is
  '只允许 OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS / NEEDS_REVIEW / REJECTED；LONG_BIAS 与 SHORT_BIAS 只是审查意见。';
comment on column quant_decision.confidence_score is
  '置信分数，范围 0 到 1；可为空，不代表交易执行概率。';
comment on column quant_decision.risk_level is
  '风险等级；仅允许 LOW / MEDIUM / HIGH / BLOCKED / UNKNOWN。';
comment on column quant_decision.rationale is
  '脱敏决策理由摘要；不得包含 raw prompt、raw provider response、凭证或可执行订单。';
comment on column quant_decision.constraints_json is
  '只读约束摘要 JSON；用于记录 forbiddenActions、risk/policy 状态等安全约束。';
comment on column quant_decision.human_approval_status is
  'stage-qdr-1 默认 NOT_REQUIRED；stage-qdr-2 才能引入 human_approval_packet 与审批 API。';
comment on column quant_decision.created_at is
  '只读决策记录创建时间，使用 timestamptz。';
