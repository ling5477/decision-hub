# decision-hub v1 完整落地包（Full Bundle）

本压缩包为“可直接补进现有仓库”的完整版，已包含：
- X：DecisionRecord v1（schema + example）
- Y：v1 Evaluators（rule-based + yaml 配置）
- Z：v1 Strategy（yaml 配置）
- W：运行时序/落盘点/状态机（文档）+ 最小 DB 表 SQL
- 与你现有 Run / LedgerEventType / RunService 的集成补丁与代码：
  - Run.java / LedgerEventType.java / RunService.java 的 patch（可用 git apply）
  - DecisionRecordV1Factory（decision_id=run_id）
  - gate/evaluator 下 3 个 v1 evaluator（constraint/consistency/completeness）
  - RunService.start() 全接线版（已使用 ModelProvider + ModelOutput）

## 你需要做的事（一次性）
1) 解压并把目录合并进你的仓库根目录（同名目录合并）。
2) 应用 patches/ 下的补丁（或手工合并）：
   - patches/Run.java.patch
   - patches/LedgerEventType.java.patch
   - patches/RunService.java.patch（可忽略：本包已提供“全接线版 RunService.java”，建议直接覆盖）
3) 确认 RunRepository 的持久化能保存/读取 Run.decisionRecord（建议新增 JSON 列 decision_record_json）。
4) providers 的实现若使用了 web/mcp/skills：请在 ModelOutput.meta 里输出 tools_used 或 tool_traces，避免一致性评估误判。

## 重要冻结约定（v1）
- decision_id = run_id
- Gate：constraint + consistency 必须 passed
- Quality：Q = 0.50*completeness + 0.30*consistency + 0.20*constraint
- v1 策略默认 rule_based（pick_best_quality）；后续可切到 dh-config 的 hybrid/weighted_vote


## v2 追加
- 已补齐 dh-domain 模块：Run 增加 decisionRecord 字段 + get/set（见 patches/dh-domain_Run.java.patch）。
