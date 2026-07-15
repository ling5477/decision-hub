# CLAUDE（Claude 开发指引 - Decision Hub）

> 目的：让 Claude / 开发者在本仓库内严格遵循当前阶段、模块边界、文档事实源、验证纪律和禁止范围。
> 本文件与 `AGENTS.md` 同源，内容一致；`AGENTS.md` 面向 Codex，本文件面向 Claude。
> 当冲突时，按以下优先级执行：安全/合规/凭证保护 > 用户本轮明确指令 > 当前 Gate / Freeze / Work Order 边界 > 本文件与 `AGENTS.md` > 通用工程最佳实践。

本仓库是 Decision Hub。任何 Agent、Codex、Claude、人工改动都必须按本文件执行。

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
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
```

涉及 DH/NQ 集成时，还必须读取：

```text
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md
docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md
```

`docs/current` 是唯一当前事实源。`STATUS.md`和`WORK_ORDER.md`分别是当前状态与下一任务的主权威。

`CLAUDE.md` is execution guidance, not the primary current-state authority. 本文件不得覆盖`STATUS.md`和`WORK_ORDER.md`的当前结论。

权威层级固定为：

```text
Primary current-state authority:
docs/current/STATUS.md
docs/current/WORK_ORDER.md

Policy authority:
docs/current/FACTSOURCE_POLICY.md

Execution guidance:
AGENTS.md
CLAUDE.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md

Entry/index documents:
README.md
docs/current/README.md
```

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
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED / ACCEPTED
Schema errata implementation: ACCEPTED
Persistent guards implementation: ACCEPTED
Guard hard-ceiling contract: CLOSED / ACCEPTED
Guard configuration bypass: CLOSED
Normative hard ceilings: rate window <= 3600s / rate quota <= 100000 / idempotency lease <= 900s
Capacity acceptance criteria: FROZEN / ACCEPTED
Capacity harness work order: CLOSED / ACCEPTED
Capacity harness implementation: NOT_STARTED / NEXT
Post-B2 capacity acceptance: BLOCKED / PENDING HARNESS AND EXECUTION
Capacity calibration path blocker: CLOSED
Repeatable protected 2xx: PASS
PromptVersion atomic bootstrap: CLOSED / ACCEPTED
Cleanup tenant-scoped contract: CLOSED / ACCEPTED
Rate matrix: PASS / 15 OF 15 MEASURED ROUNDS
Quota atomicity: PASS / COLD_START 3 OF 3
Cleanup protected-row safety: PASS / TENANT_SCOPED
Cleanup capacity evidence: PASS / 10 + 100 + 1000
PostgreSQL same-pool recovery: CLOSED / ACCEPTED / 3 OF 3
PostgreSQL contention evidence: PASS / SAME_POOL_RECOVERY_AND_SERIES_COMPLETE
Restart reproducibility: PASS / SPRING_CONTEXT 3 OF 3 / POSTGRESQL_SAME_CONTAINER 3 OF 3
Capacity threshold evidence: CLOSED / SUFFICIENT
Candidate threshold evidence: SUFFICIENT
Allow capacity criteria freeze retry: NO / CONSUMED_ACCEPTED
Allow capacity harness work order: NO / CONSUMED_ACCEPTED
Allow capacity harness implementation: YES / NEXT_TASK_ONLY
Full regression: PASS / 1114 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Full regression resource baseline: PASS / 380 SUREFIRE ROWS / 7 PIDS
Quality gate: PASS
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION-WORK-ORDER
current task status: CLOSED / ACCEPTED
next task: DH-STAGE-QDR-7-B2-CAPACITY-HARNESS-IMPLEMENTATION
```

Stage3-B3与`Integration-0-PLAN`均为historical/consumed记录，不是当前入口。Integration-0已`CLOSED / ACCEPTED`，但仍只代表contract/mock/documentation work line，不代表runtime integration。

NQ / DH 三轮只读审计（NQ 全仓 / DH 全仓 / NQ-DH 联合边界 + 汇总）已完成，结论同步在 `docs/current/STATUS.md` §1.1。当前口径固定为：

```text
NQ-DH:            not integrated；runtime connection none
Integration-0:    CLOSED / ACCEPTED as contract / mock / documentation work line, not runtime integration
Allowed work:     docs, contract freeze, mock, stub, contract test, security policy
Forbidden work:   real NQ connection, RealClient, real Provider, trading,
                  credential access, NQ DB access, LIVE
Security baseline: FULL
Fail-closed state: FULL
P1-4 residual:     CLOSED
Integration-1:     NOT STARTED
Runtime integration: NOT STARTED
AI / Agent runtime: NOT STARTED
LIVE:              DISABLED
```

不得把 Integration-0 写成真实集成；不得把 NQ integration 写成 started；不得把 DH 写成 integrated；不得把 LIVE 写成 enabled。

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

如果只改文档，可以不跑全量测试，但必须在 `docs/current/WORKLOG.md` / `docs/current/TESTING.md` 中写清未跑原因。

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

## 11. Codex / Claude workflow routing（强制）

所有 Codex / Claude / Agent 任务必须先执行 `nq-dh-workflow-router` 前置分类，再决定插件、skill、文件范围和验收命令。

开工前必须明确：

```text
repository
module
target files
excluded files
expected output
```

默认不得扫描：

```text
node_modules
target
build
dist
.git
logs
test-results
secrets
credentials
```

禁止读取、复制、提交或输出：

```text
token
cookie
API key
API secret
exchange secret
production .env
private key
mnemonic
keystore password
2FA backup code
```

文档事实源规则：

```text
不把 archived / historical / superseded 文档当作当前事实源。
除非用户明确要求历史对照，否则当前状态以 STATUS.md、AGENTS.md、CLAUDE.md、
CODEX_PROJECT_INSTRUCTIONS.md、WORK_ORDER.md 的当前段落为准。
历史 Stage 文档只能作为背景，不得自动转化为当前 next task。
```

标准输出格式固定为：

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

禁止把 `Summary` 作为必填输出字段；结论必须落在 `Findings` 或 `Next concrete action`。

### 11.1 任务分类与插件路由

```text
DOCUMENTATION        GitHub + Documents + Notion
CODE_ANALYSIS        GitHub
CODE_CHANGE          GitHub + CodeRabbit
SECURITY_AUDIT       GitHub + Codex Security + CodeRabbit
AGENT_API            GitHub + OpenAI Developers + Codex Security
NQ_INTEGRATION_PLAN  GitHub + Documents + Codex Security
PRODUCT_DESIGN       Figma + Product Design（仅 UI / 流程图任务）
PRESENTATION         Presentations + Documents + Canva
```

插件路由只决定工作方式，不自动授权外部连接、真实 provider、NQ RealClient、数据库访问或交易能力。任何插件、skill、MCP 都不能绕过本文件的 Gate / Freeze / 安全边界。

### 11.2 DH/NQ 安全边界

```text
DH 是多 Agent 决策系统，不是交易执行系统。
DH 当前只允许研究、分析、候选信号、风险解释、审计记录。
DH 不允许下单、撤单、修改策略状态、启动 Paper Run、访问交易所密钥、直接读写 NQ DB。
DH到NQ的任何未来runtime接入都必须在Integration-0 `CLOSED / ACCEPTED`之后另起GateN rebase planning。
Integration-0已`CLOSED / ACCEPTED`，但仍只是contract/mock/documentation work line，不允许真实业务打通。
NQ integration not started.
Integration-1 NOT STARTED.
Runtime integration NOT STARTED.
DH integrated NO.
AI / Agent runtime NOT STARTED.
RealClient forbidden.
real provider forbidden.
LIVE DISABLED.
NQ mutation forbidden.
```

### 11.3 当前 Codex / Claude workflow 文档入口

```text
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
docs/current/FACTSOURCE_POLICY.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```
## 12. Claude 执行纪律

- 默认使用简体中文说明计划、过程和结论。
- 先读 `AGENTS.md`、`CLAUDE.md`、`README.md`、`docs/current/*`，再读目标代码或文档。
- 默认最小变更，避免无关重构。
- 不回退用户已有改动。
- 能用工具验证的结论必须用工具验证。
- 每次交付必须说明修改文件、验证结果、剩余风险和是否触达禁止范围。

---

### 1) 工具分级与用途边界（强制按场景选工具）

#### A. 工程内操作（最高优先级）
**`idea-mcp`**
- 用于：项目结构、检索、阅读、编辑、重构、运行配置、问题检查。
- 约束：
  - 若工具支持 `projectPath`，必须显式传入。
  - 自动修改仅限白名单目录。
  - 编辑流程必须：读文件 → 改 → 格式化 → problems 检查（必要时）。

#### B. 仓库文件直接读写 / 兜底文件操作
**`filesystem`**
- 用于：当 `idea-mcp` 不可用，或需要直接处理普通文本/配置/脚本/说明文件时的兜底读写。
- 禁止：绕过 `idea-mcp` 直接对 Java/TS 核心业务代码做大规模盲改。
- 约束：仅作为工程内文件级兜底工具，不替代 `idea-mcp` 的符号级理解与重构能力。

#### C. 代码托管与变更协作（仅限 GitHub 场景）
**`github`**
- 用于：PR/Issue 读取、diff/变更审阅、提交记录查询、仓库信息读取。
- 禁止：未经明确指令自动创建/合并 PR、强推、删除分支、改仓库设置。
- 安全：Token 仅允许通过环境变量提供，例如 `GITHUB_MCP_PAT`。

#### D. 外部资料检索（只用于资料/对照，不直接改代码）
**`brave-search`**
- 用于：查外部信息、官方文档、第三方库用法、错误码、兼容性问题。
- 禁止：把搜索结果直接当项目事实；必须回到仓库或运行结果验证可落地性。
- 说明：需要 `BRAVE_API_KEY`。

#### E. 浏览器调试 / 前端运行态排查
**`chrome-devtools`**
- 用于：查看页面 DOM、网络请求、Console、Storage、路由跳转、前端运行时错误。
- 适用场景：
  - 页面白屏
  - 接口已发出但页面没渲染
  - 路由守卫异常
  - 表单交互异常
  - Ant Design 组件行为与预期不一致
- 禁止：执行敏感线上操作。

#### F. 数据库核对 / SQL 验证
**`postgres`**
- 用于：核对表结构、索引、约束、数据分布、执行 SQL 验证 migration/backfill/查询逻辑。
- 适用场景：
  - DDL 审查
  - migration 验证
  - 查询性能初查
  - 闭环联调时校验 DB 状态
- 禁止：未经明确授权直接修改生产数据。

#### G. 容器 / 本地基础设施联调
**`MCP_DOCKER`**
- 用于：查看容器状态、日志、网络、卷、镜像、Compose 相关运行信息。
- 适用场景：
  - 本地 PostgreSQL / 中间件 / 服务容器启动失败
  - 健康检查失败
  - 联调依赖未就绪
- 禁止：未经明确说明删除镜像、清卷、破坏性 prune。

#### H. CSS / 动画 / 视觉效果辅助
**`icss`**
- 用于：复杂 CSS 布局、渐变、遮罩、滤镜、动画、玻璃拟态、纯 CSS 特效实现参考。
- 定位：辅助 MCP，不是主实现工具。
- 禁止：代替业务 skill 负责页面开发主线。

---

### 1.1 `idea-mcp` 常用能力清单（按场景）
- 项目与结构：`get_project_modules`、`get_project_dependencies`、`get_repositories`、`list_directory_tree`
- 检索与阅读：`find_files_by_glob`、`find_files_by_name_keyword`、`search_in_files_by_text`、`search_in_files_by_regex`、`get_file_text_by_path`、`get_all_open_file_paths`
- 代码理解与质量：`get_symbol_info`、`get_file_problems`
- 编辑与重构：`create_new_file`、`replace_text_in_file`、`rename_refactoring`、`reformat_file`、`open_file_in_editor`
- 执行与联调：`get_run_configurations`、`execute_run_configuration`、`execute_terminal_command`

### 2) 不可用降级条件（通用）
当首选工具出现以下任一情况，允许降级到下一优先级工具：
1. 不可访问 / 连接失败
2. 无权限 / 拒绝访问
3. 同一问题连续 2 次超时
4. 返回结果无法覆盖问题（范围不完整 / 关键文件不可读 / 结果与事实矛盾）

---

### 3) 降级顺序（按场景固定）

#### 工程内检索（定位文件 / 符号）
1) `idea-mcp`
2) `filesystem`
3) `rg`
4) `Select-String`
5) `findstr`

#### 前端运行态排查
1) `chrome-devtools`
2) `idea-mcp`
3) `filesystem`

#### 数据库结构 / 查询验证
1) `postgres`
2) `idea-mcp`
3) `filesystem`

#### 本地依赖 / 容器联调
1) `MCP_DOCKER`
2) `idea-mcp`
3) 终端命令

#### 外部资料核对
1) `brave-search`
2) 手动引用（必须带来源且标注可信度）

---

### 4) 降级披露要求（强制）
一旦发生降级，回复必须包含：
- 降级原因
- 使用的工具
- 检索范围
- 结果可信度（高 / 中 / 低 + 原因）

---

### 5) 启动与密钥规则（强制）
- 所有 API Key / Token 仅允许通过环境变量注入：
  - GitHub：`GITHUB_MCP_PAT`
  - Brave：`BRAVE_API_KEY`
  - 其他：按各 MCP server 文档约定
- 禁止把密钥写入仓库文件、Markdown、脚本、截图、日志输出。

---

### 6) 编辑与运行纪律（强制）
- 编辑：先读 → 再改 → 再格式化 → 必要时 problems 检查
- 运行：先列出 run configs → 再执行 → 汇报退出状态与关键输出

#### 编辑类任务（强制流程）
1. 先读取目标文件，确认上下文
2. 再执行修改 / 重构 / 新建文件
3. 修改后必须格式化
4. 最后确认无明显错误 / 警告激增
5. 回复中列出：修改文件清单 + 变更摘要

#### 运行 / 联调类任务（强制流程）
1. 先确认可用目标（run config / 容器 / DB / 页面）
2. 再执行联调
3. 回复中汇报：退出状态 + 关键输出摘要 + 关键异常

---


## 13 Agent Skills Routing（合并后 skills 规则）

### 13.1 Active skills（唯一默认启用集合）

当前 active skills 仅允许以下 9 个：

1. `nq-dh-workflow-router`
2. `frontend-product-ui-design`
3. `ui-visual-system-polish`
4. `frontend-antd-page-builder`
5. `frontend-quality-regression`
6. `java-backend-maintenance`
7. `java-backend-regression-tests`
8. `db-schema-migration-review`
9. `python-ops-tooling`

使用原则：

- `nq-dh-workflow-router` 是所有任务的前置分类 skill；它只做分类、范围收口、插件建议和输出格式统一，不授权业务能力。
- 只选择与本轮任务直接相关的 skill，不要一次性激活所有 skills。
- 一个任务最多一个主 skill；其他 skill 只能作为补充，并说明为什么需要。
- 如果 skill 路由与当前 Gate 边界、安全边界、技术栈边界冲突，优先遵守 Gate / Freeze / Work Order / 安全 / 技术栈规则。
- 不得用 skill 名义绕过禁止项：不接 AI/DH、不接真实 provider、不接 NQ RealClient、不触碰 LIVE 交易、不新增未要求的 API / migration / 业务能力。

### 13.2 Optional skills（默认不启用）

- `.agents/optional-skills/` 下的 skill 不默认启用。
- `shadcn` / 其他非 Ant Design UI skill 只有在用户明确要求、或目标项目本身已使用对应框架时才允许使用。
- NexusQuant / Decision Hub 当前前端默认使用 React + TypeScript + Ant Design 企业后台栈，不得私自切换 UI 框架或引入新的 UI 体系。

### 13.3 前端任务路由

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

### 13.4 后端 / DB / Python 任务路由

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

### 13.5 NexusQuant / Decision Hub 前端风格

- 默认是专业金融科技后台，不是营销页。
- 高信息密度但不拥挤，弱装饰、强层级。
- 强状态表达：运行、停止、失败、风控拒绝、恢复中、重试中、过期、未配置、无权限必须清晰可见。
- 强风控和异常可见性：不得为了页面好看隐藏风险、失败、拒绝、停用、审计和追踪信息。
- 使用 Ant Design 企业后台风格与既有组件模式。
- 禁止营销页式大标题、大渐变、大插画、无意义动效、过度动效和隐藏风险状态。

### 13.6 前端页面验收标准

新增或调整前端页面时，默认检查：

- 有明确业务目标说明。
- 有核心状态摘要。
- 有清晰筛选区、主数据区、详情区、操作区。
- loading / empty / error / disabled / risky operation 状态完整。
- 危险操作有二次确认。
- REAL / LIVE / 风控失败 / 恢复 / 重试 / 停止类操作必须有明确风险提示和影响范围说明。
- 服务端数据使用 TanStack Query；Zustand 只放 auth、account-context 等轻量全局状态。
- 不新增 API，不改后端契约，不新增 migration，除非用户明确要求。

### 13.7 MCP 辅助规则

以下 MCP 只作为辅助，不改变主 skill：

- 前端运行态问题：`chrome-devtools`
- 复杂 CSS / 动画参考：`icss`
- 查询 DB 结构 / 数据：`postgres`
- 本地依赖与容器联调：`MCP_DOCKER`
- 读写普通文件或兜底检索：`filesystem`

### 13.8 输出要求

完成后必须输出：

1. 主 skill 是什么，为什么命中；如未使用 skill，说明原因。
2. 辅助 skill / MCP 是什么，为什么需要；如未使用，说明未使用。
3. 新增文件。
4. 修改文件。
5. 验证步骤。
6. 风险与未覆盖项。
7. 若发现与现有规则冲突，必须说明冲突点，并以现有 Gate 边界、安全边界、技术栈边界优先。
