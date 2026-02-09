# _active

该目录为 **当前激活计划** 的固定映射点，Codex 仅需读取本目录下的 `STATUS.json`（可选 `PLAN.md`）即可继续执行。

- 不再要求 Codex 读取 `PLAN_QUEUE.json` / `PLAN_CURRENT_POINTER.json`。
- 若要切换计划：将目标计划目录中的 `PLAN.md` 与 `STATUS.json` 覆盖到本目录（或用脚本/复制操作）。
