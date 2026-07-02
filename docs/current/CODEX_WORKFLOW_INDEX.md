# Codex Workflow Index

> 状态: active index
> 事实源: docs/current

## 1. 当前 Codex workflow 文档

```text
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
docs/current/DH_WORKFLOW_ROUTER_SKILL.md
docs/current/DH_CODEX_TASK_TEMPLATES.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```

## 2. 开工读取顺序

```text
README.md
AGENTS.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```

涉及 DH/NQ 集成规划时，还必须读取：

```text
docs/current/DH_NQ_INTEGRATION.md
docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md
```

## 3. 强制输出格式

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Findings:
Validation:
Risks:
Next concrete action:
```

## 3.1 阶段命名治理

```text
NQ 自身阶段使用 Gate 体系，例如 GateN。
DH 自身阶段使用 Stage 体系，例如 DH-STAGE4-DECISION-PIPELINE-MVP。
NQ-DH 集成任务可以引用 NQ GateN rebase，但不得把 DH 自身阶段写成 GateK/GateL/GateN。
DH-GATEK-DECISION-PIPELINE-MVP 与 docs/gates/dh-gatek-decision-pipeline-mvp/ 属于历史错误命名残留。
Decision Pipeline MVP 已 ACCEPTED / CLOSED 的事实不变。
下一步命名修复任务: DH-STAGE4-NAMING-REBASE-FIX。
```

## 4. 当前状态锁定

```text
DH-AUDIT-FIX completed.
NQ integration not started.
Integration-0 safety gate CLOSED / ACCEPTED.
Integration-1 NOT STARTED.
Runtime integration NOT STARTED.
DH integrated NO.
AI / Agent runtime NOT STARTED.
RealClient forbidden.
real provider forbidden.
LIVE DISABLED.
NQ mutation forbidden.
GateK Decision Pipeline MVP PLAN: ACCEPTED / CLOSED.
GateK Decision Pipeline MVP WO: ACCEPTED / CLOSED.
Current main line: DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE / IMPLEMENTED / READY FOR REVIEW.
Next concrete action: DH-GATEK-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE-REVIEW / NOT STARTED.
K2 DecisionOrchestrator Skeleton: NOT STARTED.
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED.
NQ current planning baseline: GateN.
```

## 5. 语言治理规则

DH 文档正文、阶段计划、工单、验收、WORKLOG、TESTING、STATUS、ROADMAP 默认中文为主。`Task classification`、`Scope`、`Validation` 等固定输出字段可保留英文，但字段内容必须中文为主。

允许保留英文的范围包括 Java 包名 / 类名 / 方法名 / 字段名、enum 值、JSON Schema 字段、OpenAPI 字段、HTTP header、状态枚举、命令和外部技术名。不得把中文项目文档整体漂移为英文说明；从 NQ skill 或历史文档同步规则时，必须改写为 DH 中文主语言风格。

