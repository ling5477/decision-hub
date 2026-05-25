# Decision Hub Docs

Decision Hub 文档采用与 NexusQuant 一致的工作流结构。

## 1. 当前事实源

所有当前阶段的事实、边界、计划、验收与工作记录都以 `docs/current/` 为准。

入口：

```text
docs/current/README.md
```

## 2. 历史冻结快照

历史阶段完成冻结后，复制到：

```text
docs/gates/<stage-id>/
```

冻结目录只读，不能回头修改历史事实。确需修正时，在 `docs/current/WORKLOG.md` 记录勘误，并在下一次冻结中体现。

## 3. Codex 计划区

`docs/codex/` 保留为历史计划与辅助执行区，不再作为唯一当前事实源。

后续 Codex 开工必须优先读取：

```text
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
```

## 4. 当前阶段

当前阶段：DH-REFIT-1-PLAN completed，Next: DH-REFIT-1-WO。

目标：将 DH 文档结构统一到 NQ 工作流，并为 DH Agent Runtime Skeleton 重构提供稳定文档入口。
