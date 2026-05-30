# Decision Hub Agent Guidelines

本仓库是 Decision Hub。任何 Agent、Codex、人工改动都必须按本文件执行。

## 1. 项目定位

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易执行系统。

DH 负责：

```text
Agent 编排
候选方案生成
多路径探索
历史反馈强化
策略评分
冲突仲裁
报告生成
辅助决策
```

NQ 负责：

```text
交易核心
账户与资产
订单状态机
风控链路
正式回测
模拟盘/实盘执行
审计与复盘
```

## 2. 当前事实源

开工前必须优先读取：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
```

涉及 DH/NQ 集成时，还必须读取：

```text
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md
```

`docs/current` 是唯一当前事实源。

`docs/codex` 只保留历史计划与辅助执行区，不得覆盖 `docs/current` 的当前结论。

## 3. 标准工作流

DH 采用与 NQ 一致的阶段化流程：

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

禁止跳过 VERIFY 标记完成。

禁止把临时补丁说明写入根 README。

阶段推进后必须更新：

```text
docs/current/STATUS.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

## 4. 当前阶段

```text
Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
Next stage:    Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE or isolated branch approval
Source of truth: docs/current
```

Stage3-B3 已于 2026-05-26 完成：DH 端 backtest adapter 可插拔骨架（dh-usecase service + DTO + Repository / dh-connector Fake + Disabled client / dh-app Stage3NqBacktestWiringConfig 三层 gate / ArchUnit 扩到 12 条）；190 tests 全绿；无真实 HTTP；无 RealNqBacktestClient。下一步进入 Stage3-B2，但 B2 触及 NQ 仓库，必须等待 NQ GateJ-FREEZE 完工或在隔离分支上获得显式批准后才能启动。每个 Batch 都必须保证：不修改 NQ 仓库（B2 启动前）、不接实盘、不自动下单、不绕过 NQ 风控、不重写 NQ 回测核心、不引入 TradingAgents Python、mvn test 全绿。Stage3 规划冻结快照位于 docs/gates/dh-stage3-plan/。

## 5. 硬边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
DH 不成为交易事实源
```

## 6. 允许改动范围

DH-REFIT-1-WO 允许改：

```text
dh-domain
dh-usecase
dh-memory
dh-eval
dh-connector
dh-api
dh-app
dh-infra
docs/current
contracts
golden_cases
```

当前阶段不允许改：

```text
NQ 仓库
实盘执行链路
订单状态机
风控核心
正式回测核心
NQ Console 正式页面
```

## 7. 构建与验证

最低验证：

```bash
mvn test
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

验证结果必须写入：

```text
docs/current/TESTING.md
```

实现记录必须写入：

```text
docs/current/WORKLOG.md
```

## 8. 代码与命名规范

```text
Java 21
Spring Boot 3.5.x
包名前缀 com.guidinglight.decisionhub
测试类以 *Test 结尾
Flyway 迁移命名 V{版本}__{描述}.sql
```

public/protected 的类、接口、枚举、字段、方法必须有清晰注释。

关键 private 方法如果承载业务规则，也必须注释。

变量、方法、类、枚举命名必须贴近业务原意、可读、可搜索。

## 9. Agent 输出要求

```text
所有 Agent 输出必须结构化
所有关键对象必须有 traceId
所有 NQ feedback 必须保存原始 payload
最终策略建议必须经过 JudgeDecision
```

禁止单个 Agent 直接输出最终交易决策。

## 10. 安全与配置

禁止提交密钥。

敏感值必须通过环境变量或安全配置注入。

所有外部输入必须校验，避免 SQL、路径、命令注入。


## 11) Agent Skills Routing（合并后 skills 规则）

### 11.1 Active skills（唯一默认启用集合）

当前 active skills 仅允许以下 8 个：

1. `frontend-product-ui-design`
2. `ui-visual-system-polish`
3. `frontend-antd-page-builder`
4. `frontend-quality-regression`
5. `java-backend-maintenance`
6. `java-backend-regression-tests`
7. `db-schema-migration-review`
8. `python-ops-tooling`

使用原则：

- 只选择与本轮任务直接相关的 skill，不要一次性激活所有 skills。
- 一个任务最多一个主 skill；其他 skill 只能作为补充，并说明为什么需要。
- 如果 skill 路由与当前 Gate 边界、安全边界、技术栈边界冲突，优先遵守 Gate / Freeze / Work Order / 安全 / 技术栈规则。
- 不得用 skill 名义绕过禁止项：不接 AI/DH、不接真实 provider、不接 NQ RealClient、不触碰 LIVE 交易、不新增未要求的 API / migration / 业务能力。

### 11.2 Optional skills（默认不启用）

- `.agents/optional-skills/` 下的 skill 不默认启用。
- `shadcn` / 其他非 Ant Design UI skill 只有在用户明确要求、或目标项目本身已使用对应框架时才允许使用。
- NexusQuant / Decision Hub 当前前端默认使用 React + TypeScript + Ant Design 企业后台栈，不得私自切换 UI 框架或引入新的 UI 体系。

### 11.3 前端任务路由

- 页面产品化、业务 UX、信息架构、核心状态模型、空态 / 错误态 / 禁用态 / 风险态、前端中文文案：使用 `frontend-product-ui-design`。
- 视觉层级、排版、色彩、专业金融后台质感、响应式、设计系统一致性、页面 polish：使用 `ui-visual-system-polish`。
- Ant Design 页面开发、组件组合、API 接入、类型定义、TanStack Query hooks、Axios client 接线、页面落地：使用 `frontend-antd-page-builder`。
- 前端 bug、路由 / 表单 / Ant Design 行为异常、E2E、Playwright、构建回归、UI 行为回归、提交前前端质量收口：使用 `frontend-quality-regression`。

做前端页面时，默认按以下顺序思考，但只激活本轮需要的 skill：

```text
frontend-product-ui-design
  -> frontend-antd-page-builder
  -> ui-visual-system-polish
  -> frontend-quality-regression
```

### 11.4 后端 / DB / Python 任务路由

- Java / Spring Boot / 模块边界 / Service 修复 / 异常链 / 事务 / 并发幂等 / 状态流转：使用 `java-backend-maintenance`。
- JUnit、golden cases、Controller / Service / Repository 集成回归、bug 修复后回归测试：使用 `java-backend-regression-tests`。
- Flyway / Liquibase migration、DDL、索引、约束、默认值、COMMENT、schema 审查、回填脚本审查：使用 `db-schema-migration-review`。
- Python 运维脚本、批处理、数据清洗、导入导出、迁移辅助、pytest、ruff、mypy：使用 `python-ops-tooling`。

做后端 DB 相关改动时，默认按以下顺序思考，但只激活本轮需要的 skill：

```text
db-schema-migration-review
  -> java-backend-maintenance
  -> java-backend-regression-tests
```

### 11.5 NexusQuant / Decision Hub 前端风格

- 默认是专业金融科技后台，不是营销页。
- 高信息密度但不拥挤，弱装饰、强层级。
- 强状态表达：运行、停止、失败、风控拒绝、恢复中、重试中、过期、未配置、无权限必须清晰可见。
- 强风控和异常可见性：不得为了页面好看隐藏风险、失败、拒绝、停用、审计和追踪信息。
- 使用 Ant Design 企业后台风格与既有组件模式。
- 禁止营销页式大标题、大渐变、大插画、无意义动效、过度动效和隐藏风险状态。

### 11.6 前端页面验收标准

新增或调整前端页面时，默认检查：

- 有明确业务目标说明。
- 有核心状态摘要。
- 有清晰筛选区、主数据区、详情区、操作区。
- loading / empty / error / disabled / risky operation 状态完整。
- 危险操作有二次确认。
- REAL / LIVE / 风控失败 / 恢复 / 重试 / 停止类操作必须有明确风险提示和影响范围说明。
- 服务端数据使用 TanStack Query；Zustand 只放 auth、account-context 等轻量全局状态。
- 不新增 API，不改后端契约，不新增 migration，除非用户明确要求。

### 11.7 MCP 辅助规则

以下 MCP 只作为辅助，不改变主 skill：

- 前端运行态问题：`chrome-devtools`
- 复杂 CSS / 动画参考：`icss`
- 查询 DB 结构 / 数据：`postgres`
- 本地依赖与容器联调：`MCP_DOCKER`
- 读写普通文件或兜底检索：`filesystem`

### 11.8 输出要求

完成后必须输出：

1. 主 skill 是什么，为什么命中；如未使用 skill，说明原因。
2. 辅助 skill / MCP 是什么，为什么需要；如未使用，说明未使用。
3. 新增文件。
4. 修改文件。
5. 验证步骤。
6. 风险与未覆盖项。
7. 若发现与现有规则冲突，必须说明冲突点，并以现有 Gate 边界、安全边界、技术栈边界优先。
