-- Decision Hub v1 最小落库（冻结建议）
-- 说明：v1 先以 JSON 全量落盘为主，少量冗余列便于检索/统计。

CREATE TABLE IF NOT EXISTS decision_record (
  decision_id      VARCHAR(64)  NOT NULL PRIMARY KEY,
  schema_version   VARCHAR(16)  NOT NULL,
  status           VARCHAR(16)  NOT NULL,
  created_at       DATETIME(3)  NOT NULL,
  updated_at       DATETIME(3)  NOT NULL,
  strategy_type    VARCHAR(32)  NULL,
  selected_ref     VARCHAR(128) NULL,
  confidence       DECIMAL(5,4) NULL,
  cost_usd         DECIMAL(12,6) NULL,
  latency_ms       BIGINT       NULL,
  tags_json        JSON         NULL,
  record_json      JSON         NOT NULL
);

CREATE INDEX idx_decision_created_at ON decision_record(created_at);
CREATE INDEX idx_decision_status ON decision_record(status);
CREATE INDEX idx_decision_strategy ON decision_record(strategy_type);

-- 可选：如果你的 MySQL 版本不适合 JSON 索引，可把 tags_json 改成 LONGTEXT 并在应用层解析。
