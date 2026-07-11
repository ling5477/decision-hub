-- Stage-QDR-6 B3 persistence blocker fix: metadata-only forward migration。
-- 本 migration 只为 V10 已创建的 constraints/indexes 补充审计 COMMENT；不改变表、列、约束、
-- nullability、FK、unique、trigger、payload、默认值或数据语义。

comment on constraint ux_dh_decision_request_tenant_decision on dh_decision_request is
  'V10 source identity：租户内 decision_id 唯一，用于 canonical snapshot 的 tenant-bound V5 外键。';
comment on constraint ux_decision_request_tenant_id on decision_request is
  'V10 source identity：为 V6 request 提供 tenant_id + id tenant-bound 唯一键。';
comment on constraint ux_decision_run_id_request on decision_run is
  'V10 source identity：固定 decision_run 与 decision_request 的精确归属。';
comment on constraint ux_qdr_prompt_version_tenant_id on qdr_prompt_version is
  'V10 source identity：为 prompt version 提供 tenant_id + id tenant-bound 唯一键。';
comment on constraint ux_qdr_model_version_tenant_id on qdr_model_version is
  'V10 source identity：为 model version 提供 tenant_id + id tenant-bound 唯一键。';
comment on constraint ux_qdr_model_gateway_call_tenant_wide_identity on qdr_model_gateway_call is
  'V10 source identity：固定 tenant、call、run、prompt 与 model 的精确组合。';

comment on constraint qdr_canonical_replay_snapshot_pkey on qdr_canonical_replay_snapshot is
  'Canonical snapshot 物理主键；业务读取仍必须携带 tenant。';
comment on constraint ux_qdr_canonical_snapshot_tenant_id on qdr_canonical_replay_snapshot is
  '为 tenant-bound 外键和物理 identity exact validation 提供唯一键。';
comment on constraint ux_qdr_canonical_snapshot_tenant_snapshot on qdr_canonical_replay_snapshot is
  '租户内 snapshot_id immutable 唯一，支持 duplicate-identical/conflict 判定。';
comment on constraint ux_qdr_canonical_snapshot_aggregate_identity on qdr_canonical_replay_snapshot is
  '租户、run、replay case 与 snapshot schema 的冻结 aggregate identity。';
comment on constraint fk_qdr_canonical_snapshot_v5_decision on qdr_canonical_replay_snapshot is
  '绑定同租户 V5 decision source，不允许 tenantless 或 orphan snapshot。';
comment on constraint fk_qdr_canonical_snapshot_v6_request on qdr_canonical_replay_snapshot is
  '绑定同租户 V6 decision request source。';
comment on constraint fk_qdr_canonical_snapshot_v6_run_request on qdr_canonical_replay_snapshot is
  '绑定 V6 run 与 request 的精确组合，禁止只按 run UUID 推断。';
comment on constraint fk_qdr_canonical_snapshot_v8_prompt on qdr_canonical_replay_snapshot is
  '绑定同租户 V8 immutable prompt version。';
comment on constraint fk_qdr_canonical_snapshot_v8_model on qdr_canonical_replay_snapshot is
  '绑定同租户 V8 immutable model version。';
comment on constraint fk_qdr_canonical_snapshot_v8_call on qdr_canonical_replay_snapshot is
  '绑定同租户 V8 gateway call/run/prompt/model exact identity。';
comment on constraint fk_qdr_canonical_snapshot_v9_replay_case on qdr_canonical_replay_snapshot is
  '绑定同租户 V9 replay case physical identity。';
comment on constraint fk_qdr_canonical_snapshot_v9_evaluation_case on qdr_canonical_replay_snapshot is
  '可选绑定同租户 V9 evaluation lineage；缺失时保持 NULL。';
comment on constraint fk_qdr_canonical_snapshot_v9_regression_verdict on qdr_canonical_replay_snapshot is
  '可选绑定同租户 V9 verdict lineage；缺失时保持 NULL。';
comment on constraint chk_qdr_canonical_snapshot_decision_type on qdr_canonical_replay_snapshot is
  '只允许 READ_ONLY_RECOMMENDATION，禁止 executable trading decision。';
comment on constraint chk_qdr_canonical_snapshot_fixed_versions on qdr_canonical_replay_snapshot is
  '冻结 snapshot/context/canonicalization/replay/hash compatibility labels。';
comment on constraint chk_qdr_canonical_snapshot_required_text on qdr_canonical_replay_snapshot is
  '拒绝关键 identity 与 version metadata 的空白值。';
comment on constraint chk_qdr_canonical_snapshot_no_version_alias on qdr_canonical_replay_snapshot is
  '拒绝 latest/current/default moving aliases，防止 replay source 漂移。';
comment on constraint chk_qdr_canonical_snapshot_lineage_pairs on qdr_canonical_replay_snapshot is
  '可选 evaluation/verdict physical 与 business identity 必须成对存在或同时缺失。';
comment on constraint chk_qdr_canonical_snapshot_hashes on qdr_canonical_replay_snapshot is
  '所有 required hash/checksum 保持 lowercase SHA-256 hex；不允许削弱 canonical_input_hash。';
comment on constraint chk_qdr_canonical_snapshot_json_shape on qdr_canonical_replay_snapshot is
  '冻结 structured safe JSON allowlist 与必需字段，不接受额外顶层字段。';
comment on constraint chk_qdr_canonical_snapshot_no_unsafe_top_level_keys on qdr_canonical_replay_snapshot is
  '拒绝 raw prompt、raw provider response、credential 与 secret 类顶层键。';
comment on constraint chk_qdr_canonical_snapshot_payload_limits on qdr_canonical_replay_snapshot is
  '执行总 payload 与分字段 byte 上限；超限 fail-closed 且不截断。';

comment on index idx_qdr_canonical_snapshot_tenant_run is
  '支持 tenant + decision_run_id 精确定位，不用于 latest 或 tenantless scan。';
comment on index idx_qdr_canonical_snapshot_tenant_decision is
  '支持 tenant + decision_id 精确定位与审计。';
comment on index idx_qdr_canonical_snapshot_tenant_model_call is
  '支持 tenant + model_call_ref 精确定位与审计。';
comment on index idx_qdr_canonical_snapshot_tenant_replay_case is
  '支持 tenant + replay_case_id 精确定位与审计。';
