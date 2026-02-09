# WORK_ORDER（唯一权威）

本仓库为 **Codex 续跑型工作流**：计划与状态落盘驱动执行。为避免空转，所有规则只在本文件定义，其他文件不得复制口径。

## 启动必读顺序（强制）
1) `docs/codex/WORK_ORDER.md`
2) `docs/codex/PLAN_QUEUE.json`
3) `docs/codex/PLAN_CURRENT_POINTER.json`
4) `docs/codex/plans/<activePlanId>/PLAN.md` 与 `STATUS.json`

路径约定：`activePlanId` 来自 `PLAN_CURRENT_POINTER.json`；计划目录固定为 `docs/codex/plans/<activePlanId>/`。

## 状态机与推进规则（强制）
- `STATUS.json.activeStepIdx` **必须等于** `steps` 中第一个 `status != DONE` 的 `idx`（即“第一个未完成步骤”）。
- `IN_PROGRESS` 只允许出现在 `idx == activeStepIdx` 的步骤上（最多 1 个）。
- `status` 枚举：`TODO | IN_PROGRESS | DONE | BLOCKED | SKIPPED`。
- 若步骤被阻塞：设置 `status=BLOCKED`，并在 `evidence` 中写明阻塞原因与解除条件（可引用 ISSUE 或依赖项）。
- 若确需跳过：使用 `SKIPPED`，并在 `evidence` 中写明原因与风险；禁止通过修改 `activeStepIdx` 实现隐式跳步。

## 计划与落盘（强制）
- 涉及实现/修改时：必须将计划写入 `PLAN.md`（可追加，不要覆盖历史），再开始修改代码/配置。
- 每次推进（至少一个步骤状态变化）后：必须更新 `STATUS.json.updatedAt`，并补充该步骤 `evidence`（文件路径列表）。

## 任务类型与验证门禁（强制）
- `taskType=delivery`：存在代码/配置/契约变更，必须运行 `scripts/verify.ps1`（本质 `mvn verify`），并回填 `STATUS.json.lastVerify`：`time/result/summary/...`。
- `taskType=analysis-only`：无仓库文件变更，可跳过 verify，但必须回填 `lastVerify.result=SKIPPED` 与原因，并保证不改动文件。

## 变更记录（强制）
- 发生 `delivery` 变更后：追加记录到 `docs/codex/CHANGE_NOTES.md`（变更点 + 影响范围 + verify 结论）。

## MCP 使用与降级（强制）
- 若 IntelliJ MCP 可用：优先用于语义重构/批量改引用。
- 若不可用：必须采用保守降级（小步修改 + 搜索/编译/测试验证），禁止盲目全局替换。
