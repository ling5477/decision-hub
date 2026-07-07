# Decision Hub

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层。

它负责 Agent 编排、候选方案生成、多路径探索、历史反馈强化、策略评分、冲突仲裁、报告生成和辅助决策。

它不负责交易核心、订单状态机、风控执行、正式回测内核、模拟盘/实盘执行、账本审计和交易事实沉淀。这些能力由 NexusQuant 承担。

## 当前阶段

```text
当前阶段: DH-STAGE-QDR-2 / ACCEPTED / FINAL_CLOSE_SYNCED / NO_RUNTIME
下一阶段: DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN / READY / PLANNING_ONLY / IMPLEMENTATION_NOT_STARTED
事实源: docs/current
```

## 文档入口

当前事实源：

```text
docs/current/README.md
```

开工前必须读取：

```text
AGENTS.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_STATUS.md
docs/current/STAGE1_CLOSE_WORKLOG.md
```

## 文档结构

```text
docs/current/      当前事实源
docs/gates/        历史阶段快照（Stage1 冻结后归档）
docs/codex/        当前活跃计划（plans/_active/） + 历史归档（plans/_archive/）
contracts/         对外协议、Schema、事件契约
golden_cases/      黄金样例与回归用例
```

## Codex workflow 入口

```text
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
docs/current/DH_WORKFLOW_ROUTER_SKILL.md
docs/current/DH_CODEX_TASK_TEMPLATES.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```

所有 Codex / Agent 任务先使用 `nq-dh-workflow-router` 做任务分类、插件路由、scope 收口和输出格式统一。所有 DH 文档治理、docs/current、Gate/Phase/Stage planning、work order、acceptance/freeze/close review、WORKLOG/TESTING/STATUS/ROADMAP/API 同步和 Decision Pipeline MVP 文档规划任务必须使用 `dh-docs-writer`。标准输出字段固定为 `Task classification`、`Plugins selected`、`Scope`、`Files inspected`、`Files changed`、`Findings`、`Validation`、`Risks`、`Next concrete action`；`Summary` 不是必填字段。

DH 文档正文与代码注释 / Javadoc 默认中文为主；类名、方法名、包名、enum、JSON/OpenAPI 字段、HTTP header、状态枚举、命令和外部技术名保留英文原样。固定输出字段可以保留英文，但字段内容必须中文为主；不得把中文项目文档整体漂移为英文说明。

## 当前工程边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
```

## 标准工作流

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

当前下一步只能进入：

```text
DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN / READY / PLANNING_ONLY / NO_IMPLEMENTATION。
- 当前 DH 实际工作区为 `F:/Project/decision-hub`。
- stage-qdr-2 B1/B2/B3/B4 均已完成、freeze/acceptance 通过并提交；B4 HEAD 为 `feat(qdr): add stage-qdr-2 human approval API`。
- stage-qdr-2 B5 close review 已 `ACCEPTED`；stage-qdr-2 final close 已 `CLOSED`。
- stage-qdr-3 只允许进入 Model Gateway + Prompt Version planning；implementation 仍 `NOT STARTED` 且 `NOT ALLOWED`。
- replay execution API / model gateway / tool registry 均 `NOT STARTED`。
- real HTTP / real provider / Agent runtime / LangGraph runtime 均未启动；LIVE DISABLED。
- `mvnw.cmd` 当前为 `UNUSABLE / P2 TOOLING RISK`；使用系统 Maven `mvn` 作为当前替代验证工具。
- Docker daemon CLI 可用，但本机 Java/Testcontainers 访问 `\\.\pipe\docker_engine` 被拒绝；Docker-gated tests skip 只能记录为环境型 skip，不得写成 PASS。
- 严格禁止：修改 NQ 仓库 / 接实盘 / 自动下单 / 绕风控 / 重写回测核心 / 接真实 provider / 接 LangGraph runtime / 开启 LIVE。
```

冻结快照：`docs/gates/dh-stage3-plan/`（33 个文件含 10 份 STAGE3_*.md；不得直接修改）

## 构建与验证

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

## DH 与 NQ 的关系

```text
NQ Console -> DH API -> Agent Runtime / Judge / Memory / NQ Adapter
NQ Console -> NQ API -> Backtest / Risk / Paper / Live / Audit
NQ -> DH Feedback -> Experience / Pheromone
```

DH 输出结构化建议和报告，NQ 执行正式交易能力。
