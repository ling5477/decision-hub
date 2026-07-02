# Decision Hub Docs Structure

## 1. 目标

DH 文档结构统一为与 NQ 一致的工作流结构。

核心规则：

```text
docs/current = 当前事实源
docs/gates = 历史冻结快照
docs/codex = 历史计划与辅助执行区
```

## 2. 根目录文档

```text
README.md = 项目入口、当前阶段、文档入口、构建命令
AGENTS.md = Agent / Codex / 人工执行统一规则
```

## 3. docs/current

`docs/current` 是当前事实源，必须长期维护：

```text
README.md
STATUS.md
ROADMAP.md
WORKFLOW.md
WORK_ORDER.md
WORKLOG.md
ARCHITECTURE.md
API.md
DB_SCHEMA.md
TESTING.md
DOCS_STRUCTURE.md
DH_NQ_INTEGRATION.md
DH_REFACTOR_STAGE1_WORK_ORDER.md
```

## 4. docs/gates

`docs/gates` 保存冻结快照。

目录格式：

```text
docs/gates/<stage-id>/
```

冻结后历史目录只读。

## 5. docs/codex

`docs/codex` 保留旧计划、旧状态、旧 Codex 队列。

从本次重构后，`docs/codex` 不能覆盖 `docs/current` 的当前结论。

## 6. contracts 与 golden_cases

`contracts` 保存 OpenAPI、JSON Schema、事件协议和 SDK 契约。

`golden_cases` 保存 Agent 输出、候选生成、JudgeDecision、NQ feedback、经验分数更新的回归样例。

## 7. 更新规则

每次阶段推进必须更新：

```text
docs/current/STATUS.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

涉及范围变化，更新 `ROADMAP.md` 和 `WORK_ORDER.md`。

涉及架构、API、DB 变化，更新 `ARCHITECTURE.md`、`API.md`、`DB_SCHEMA.md`。
