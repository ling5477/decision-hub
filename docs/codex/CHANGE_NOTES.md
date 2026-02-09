# CHANGE_NOTES
> 记录每次任务的变更摘要：改了什么、影响哪些模块、验证结果如何。

## 2026-02-08 20:46:17 计划规则优化
- 更新 WORK_ORDER：增加 taskType、activeStepIdx、失败恢复与最小字段约束。
- 更新 TASK_TEMPLATE：显式 taskType 与 SKIPPED 验证口径。
- 更新 VERIFY：按 taskType 区分 PASS/FAIL/SKIPPED 回填。
