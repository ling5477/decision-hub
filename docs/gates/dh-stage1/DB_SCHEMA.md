# Decision Hub DB Schema

## 1. 当前状态

当前为 Stage1 数据模型规划文档。DH-REFIT-1-PLAN 阶段不新增迁移。

## 2. Stage1 建议表

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

## 3. 通用字段

所有核心表必须包含：

```text
id
trace_id
status
created_at
updated_at
payload_json
```

涉及外部回流的表必须包含：

```text
source_system
source_event_id
source_event_type
raw_payload_json
received_at
```

## 4. 设计原则

```text
结构化字段承载查询
payload_json 承载完整快照
trace_id 串联任务全链路
NQ feedback 原样保存
经验分数可重算
```

## 5. 禁止事项

```text
不保存交易事实替代 NQ
不复制 NQ 订单/成交/仓位/账本表
不把 DH 经验分数作为 NQ 风控事实
```
