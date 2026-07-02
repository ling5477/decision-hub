# Stage2-PoC DB Plan

> Status: PLAN
> Created: 2026-05-25

## 1. 现有迁移（不修改）

### V1（初始 schema）

```text
基础表（runs, steps 等旧链路表）
```

### V2__dh_agent_runtime.sql（Stage1 已就位）

```text
dh_research_runs
dh_agent_tasks
dh_task_nodes
dh_agent_artifacts
dh_strategy_candidates
dh_candidate_scores
dh_judge_decisions
dh_experience_entries
dh_pheromone_edges
dh_nq_feedback_events
```

## 2. Stage2 新增迁移：V3__stage2_poc_tools.sql

### 2.1 dh_forecast_artifacts

```sql
CREATE TABLE dh_forecast_artifacts (
    id              UUID PRIMARY KEY,
    trace_id        UUID NOT NULL,
    symbol          VARCHAR(32) NOT NULL,
    horizon         VARCHAR(16) NOT NULL,
    predictions_json TEXT NOT NULL,
    model_version   VARCHAR(64),
    status          VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    payload_json    TEXT
);

CREATE INDEX idx_forecast_artifacts_trace ON dh_forecast_artifacts(trace_id);
CREATE INDEX idx_forecast_artifacts_symbol ON dh_forecast_artifacts(symbol);
```

### 2.2 dh_external_market_snapshots

```sql
CREATE TABLE dh_external_market_snapshots (
    id              UUID PRIMARY KEY,
    trace_id        UUID NOT NULL,
    symbols_json    TEXT NOT NULL,
    data_types_json TEXT NOT NULL,
    date_range_start DATE,
    date_range_end   DATE,
    fetched_at      TIMESTAMP NOT NULL,
    data_json       TEXT NOT NULL,
    source_version  VARCHAR(64),
    status          VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_snapshots_trace ON dh_external_market_snapshots(trace_id);
```

### 2.3 dh_reflection_checkpoints

```sql
CREATE TABLE dh_reflection_checkpoints (
    id              UUID PRIMARY KEY,
    run_id          UUID NOT NULL,
    step_index      INT NOT NULL,
    agent_role      VARCHAR(64) NOT NULL,
    reflection      TEXT NOT NULL,
    decision        VARCHAR(32) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    payload_json    TEXT
);

CREATE INDEX idx_reflection_checkpoints_run ON dh_reflection_checkpoints(run_id);
```

## 3. V2 表补充字段（ALTER，包含在 V3 脚本中）

### dh_research_runs 新增字段

```sql
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS regime VARCHAR(64);
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS topic VARCHAR(128);
ALTER TABLE dh_research_runs ADD COLUMN IF NOT EXISTS planner_strategy VARCHAR(64) DEFAULT 'DEFAULT';
```

### dh_nq_feedback_events 新增字段

```sql
ALTER TABLE dh_nq_feedback_events ADD COLUMN IF NOT EXISTS schema_version VARCHAR(16) DEFAULT '1.0.0';
ALTER TABLE dh_nq_feedback_events ADD COLUMN IF NOT EXISTS validation_status VARCHAR(32) DEFAULT 'VALID';
```

## 4. InMemory -> JDBC 替换清单

Stage1 的 6 个 InMemory 仓储需要 JDBC 实现：

```text
InMemoryResearchRunRepository      -> JdbcResearchRunRepository      (V2 dh_research_runs)
InMemoryAgentTaskRepository        -> JdbcAgentTaskRepository        (V2 dh_agent_tasks)
InMemoryStrategyCandidateRepository -> JdbcStrategyCandidateRepository (V2 dh_strategy_candidates)
InMemoryJudgeDecisionRepository    -> JdbcJudgeDecisionRepository    (V2 dh_judge_decisions)
InMemoryExperienceEntryRepository  -> JdbcExperienceEntryRepository  (V2 dh_experience_entries)
InMemoryNqFeedbackEventRepository  -> JdbcNqFeedbackEventRepository  (V2 dh_nq_feedback_events)
```

Stage2 新增仓储：

```text
JdbcForecastArtifactRepository     (V3 dh_forecast_artifacts)
JdbcMarketSnapshotRepository       (V3 dh_external_market_snapshots)
JdbcReflectionCheckpointRepository (V3 dh_reflection_checkpoints)
```

## 5. 设计原则（延续 Stage1）

```text
结构化字段承载查询
payload_json / data_json 承载完整快照
trace_id 串联任务全链路
NQ feedback 原样保存
经验分数可重算
不保存交易事实替代 NQ
```

## 6. 风险

```text
V3 ALTER 语句需要 IF NOT EXISTS 防止重复执行失败
payloadJson 序列化需要 schema_version 字段防止反序列化失败
JDBC 实现需要处理 TEXT 字段的 JSON 序列化/反序列化
PostgresContainerSmokeTest 需要 Docker，CI 环境需确认
```
