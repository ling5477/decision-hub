# Decision Hub v1 运行时序与落盘点（冻结版）

> 目标：**随时可复盘/可回放/可回归测试**。任何阶段崩溃，都能从数据库中恢复到最近一步。

## 1. 核心对象

- DecisionRecord（一次决策的全量记录）：输入快照、各模型输出、评估结果、最终策略路径、最终输出。
- ModelRun（一次模型调用）：run_id、prompt_snapshot、raw_output、usage、tool_traces、errors。
- EvalResult（一次评估器输出）：evaluator、target、score、passed、reason、metrics。
- FinalDecision（最终决策）：strategy(type/params)、selected(ref)、confidence、output、warnings。

## 2. 状态机（冻结）

### Decision
- INIT → RUNNING → DONE | FAILED

### ModelRun
- PENDING → RUNNING → SUCCEEDED | FAILED | SKIPPED

## 3. 写入点（WAL 思路，冻结）

1) 收到请求：创建 DecisionRecord
- status=INIT
- 写入 input_context + tags + audit(trace_id/session_id/user_id)

2) 开始执行：DecisionRecord.status=RUNNING
- 生成本次执行计划（即便你 v1 不做 Plan 对象，也要能在 record_json 内记录“预期运行的模型列表”）

3) 每个模型调用（并行/串行均可）：
- 写入/更新 model_runs[i]：
  - RUNNING（started_at）→ SUCCEEDED/FAILED（finished_at, latency_ms, errors）
  - raw_output/usage/tool_traces 完整落盘

4) 每个模型 run 完成后，立即跑 Evaluators（最小集：constraint/consistency/completeness）：
- eval_results 逐条追加写入（幂等键：decision_id + evaluator + target.ref）

5) 策略执行：选择候选并写回 final_decision
- 写入：strategy(type/params) + selected + confidence + output + warnings
- DecisionRecord.status=DONE

6) 任何异常：
- DecisionRecord.status=FAILED
- errors[] 追加（建议 record_json 的顶层增加 errors[]，与 model_runs[].errors 区分）
- 注意：FAILED 也必须保留中间产物，便于复盘/重跑对比。

## 4. 幂等与重试（冻结建议）

- 同一次用户请求重试：attempt++，但建议每次重试生成新的 decision_id（便于对比）；如你一定要复用 decision_id，则必须引入 idempotency_key。
- EvalResult 写入幂等：同 evaluator+target 重复写入时，以最后一次为准，同时追加 metrics.attempt。

## 5. 回放（Replay）最小要求

DecisionRecord 必须包含：
- prompt_snapshot（system/developer/user/tools/temperature/top_p）
- raw_output（原始输出，不可再生成）
- evaluator 结果（score/passed/reason）
- strategy.params（当时用的配置）
