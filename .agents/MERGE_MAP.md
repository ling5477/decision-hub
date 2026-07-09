# Skills Merge Map

原始包里共有 38 个 skills。本次合并为 10 个 active skills：2 个 DH/NQ workflow governance skills，8 个 implementation / review skills。

## 0. workflow governance skills

### nq-dh-workflow-router

负责：所有 DH/NQ 任务的前置分类、仓库 / 模块 / 文件范围收口、插件建议、Stage/Gate 命名、NQ/DH 集成边界、禁止真实 provider / HTTP / Agent / LangGraph / LIVE 越界。

### dh-docs-writer

负责：DH `docs/current`、work order、archive、freeze / close、TESTING、WORKLOG、STATUS、ROADMAP、README、factsources 与文档治理。该 skill 不授权代码、测试、migration、API、runtime、provider 或 LIVE。

## 1. implementation / review skills

### 1. frontend-product-ui-design

合并来源：

- `shape`
- `onboard`
- `clarify`
- `distill`
- `critique` 的 UX / 信息架构部分
- `harden` 的状态、错误、边界情况部分
- 原建议中的 product-page-ux、trading-business-ux、state-and-empty-design、frontend-copywriting 思路

负责：产品目标、页面结构、业务流程、状态语义、空态/错误态、风险操作提示、业务文案。

### 2. ui-visual-system-polish

合并来源：

- `impeccable`
- `anthropic-frontend-design`
- `arrange`
- `typeset`
- `colorize`
- `bolder`
- `quieter`
- `delight`
- `animate`
- `adapt`
- `normalize`
- `polish`
- `extract` 的设计系统部分
- `audit` 的视觉/可访问性部分
- `optimize` 的 UI 性能部分
- `overdrive` 降级为“仅在明确要求 wow/动效时启用”
- `ui-ux-pro-max` 的设计参考能力

负责：视觉层级、布局、排版、色彩、动效、响应式、设计系统一致性、精修审查。

### 3. frontend-antd-page-builder

合并来源：

- `build-page-from-api`
- `wire-api-module`
- `scaffold-component`
- `extract` 的组件抽取部分

负责：React + TypeScript + Ant Design 页面实现、API 接入、组件骨架、详情抽屉、表格、筛选、操作区。

### 4. frontend-quality-regression

合并来源：

- `fix-ui-bug`
- `frontend-review`
- `e2e-regression`
- `audit` 的提交前质量检查部分
- `harden` 的生产边界检查部分
- `optimize` 的性能回归检查部分

负责：前端 bug 闭环、类型检查、构建、Playwright、提交前审查。

### 5. java-backend-maintenance

合并来源：

- `fix-prod-bug-java`
- `refactor-service-layer-java`
- `spring-boot-module-review`

负责：Java 后端 bug 修复、Service 层职责收口、Spring Boot 模块边界审查。

### 6. java-backend-regression-tests

合并来源：

- `write-junit-and-golden-tests`
- `integration-regression-java`

负责：JUnit、golden case、Controller/Service/Repository 集成回归。

### 7. db-schema-migration-review

合并来源：

- `review-ddl-and-migration`

负责：DDL、migration、索引、约束、默认值、注释、回填脚本审查。

### 8. python-ops-tooling

合并来源：

- `build-batch-script-python`
- `write-pytest-regression`

负责：Python 批处理、数据清洗、导入导出、运维辅助、pytest 回归。

## 不建议默认启用

`shadcn` 不建议放入 active skills，因为 NexusQuant / Decision Hub 当前主栈是 Ant Design。需要做 shadcn 项目时，再单独加入。

## Workflow policy merge

- Stage archive 必须形成 self-contained archive packet，不能只保留单个 `README.md`；packet 不完整时 tag close 必须 BLOCKED。
- Stage tag 只能在完整 archive packet 所在 commit 已存在且 tag close 任务明确授权后处理；archive close 与 tag close 必须分开。
- Stage archive close 后、tag close 前不得进入下一阶段 planning；Stage-QDR-6 只能在 Stage-QDR-5 tag close 完成后另起 planning-first 任务。
- 只有 migration、API / Controller、安全边界、stage close、P0 / P1 blocker 触发 review。
- 普通 batch 不默认触发 standalone review；闭环形态为 implementation + tests + boundary scan + minimal docs + commit。
- 不硬编码本机盘符路径，执行时以当前机器真实仓库路径为准。
- NQ dev 与 NQ integration worktree 不得混用。
