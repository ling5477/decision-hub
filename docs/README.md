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
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
```

## 4. 当前阶段

当前阶段：`STAGE_QDR_4 / CLOSED / ACCEPTED / ARCHIVED`，`STAGE_QDR_4_TAG / PENDING`。

当前任务：`DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION`。本轮只修复文档纪律、workflow authority、skill policy、archive policy 和 current factsource；不得创建 tag，不得进入 Stage-QDR-5，不得修改 Java、测试、migration、API、contracts、golden_cases 或 NQ。

下一步：cleanup 完成并保持 clean 后，才能进入独立 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 只能在 tag close 后 planning-first。
