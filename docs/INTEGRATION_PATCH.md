# v1 与现有 Run / LedgerEventType 的对齐方案（可直接落地）

你现有：
- Run 聚合：RunStatus（DRAFT/QUEUED/RUNNING/...） + steps（RunStep/StepType）
- LedgerEventType：RUN_CREATED/RUN_ENQUEUED/RUN_STARTED/STEP_STARTED/STEP_COMPLETED/GATE_EVALUATED/DECISION_FINALIZED

## 1. 冻结约定（v1）

- decision_id = run_id（强烈建议）
- DecisionRecord 全量档案挂在 Run.decisionRecord（Map 或 JSON）上
- Gate：constraint + consistency 必须 passed 才能进入策略
- 质量分：Q = 0.50*completeness + 0.30*consistency + 0.20*constraint

## 2. 事件映射（推荐）

- RUN_STARTED：Run.start()
- STEP_STARTED/STEP_COMPLETED：把“模型调用”当作 RunStep（StepType.MODEL_CALL*）
- GATE_EVALUATED：每个候选 runId 的 gate 完成后发一次（payload 带 runId + passed）
- EVALUATION_RECORDED（新增）：每个 evaluator 输出写入 DecisionRecord.eval_results 时发
- DECISION_FINALIZED：final_decision 写回完成时发
- RUN_FAILED（新增）：异常时发

> 如果你暂时不想新增 EventType：EVALUATION_RECORDED 可以先复用 STEP_COMPLETED 并在 payload 标注 kind=evaluation。

## 3. 最小代码改动清单

- 应用 Run.java.patch：新增 decisionRecord 字段 + rehydrate 参数
- 应用 LedgerEventType.java.patch：新增 RUN_FAILED / EVALUATION_RECORDED
- 应用 RunService.java.patch：create() 初始化 DecisionRecord；新增 start(runId) 骨架
- 引入 DecisionRecordV1Factory：用 Run 生成初始 record

## 4. evaluator 代码放置

本包提供 rule-based 的 v1 Evaluators：
- usecase/gate/evaluator/ConstraintEvaluatorV1
- usecase/gate/evaluator/ConsistencyEvaluatorV1
- usecase/gate/evaluator/CompletenessEvaluatorV1

它们只依赖 decisionRecord(Map) + runId（model_run.run_id）即可工作。

## 5. 下一步你要接线的 3 个方法（不再拆问）

在 RunService.start()（或你单独的 Executor）里按顺序做：
1) 往 decisionRecord.model_runs 追加每个模型的 run_id/raw_output/tool_traces/usage
2) 对每个模型 run_id 调 3 个 Evaluator，把结果追加到 decisionRecord.eval_results
3) 复用你已有 DecisionEngineV1（或自己实现）从 eval_results 计算 gateCandidates + quality，写回 final_decision，并发 DECISION_FINALIZED
