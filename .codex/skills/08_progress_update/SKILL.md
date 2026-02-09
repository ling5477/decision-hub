---
name: progress-update
description: 每完成一步更新 plans/<activePlanId>/STATUS.json
---
- 完成/推进后必须更新：步骤状态、evidence、activeStepIdx、updatedAt、lastVerify（详见 WORK_ORDER）。
- 计划完成后，将 active 计划从 `PLAN_QUEUE.json:active` 移至 `done`。
