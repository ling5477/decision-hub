# Optimized Agent Skills

这是从原 38 个 skills 合并后的精简版。目标是减少触发冲突、降低上下文噪声，并把零散技能合并成“可执行工作流”。当前 active skills 为 10 个，其中 `nq-dh-workflow-router` 与 `dh-docs-writer` 是 DH/NQ workflow governance skills。

## 当前 active skills

| Skill | 用途 |
|---|---|
| `nq-dh-workflow-router` | DH/NQ 任务前置分类、范围收口、插件建议、Stage/Gate 命名与安全边界 |
| `dh-docs-writer` | DH docs/current、work order、archive、freeze/close、事实源和文档治理 |
| `frontend-product-ui-design` | 页面产品化、业务 UX、信息架构、状态/空态/风险提示 |
| `ui-visual-system-polish` | 视觉层级、排版、色彩、响应式、动效、设计系统一致性 |
| `frontend-antd-page-builder` | React + Ant Design 页面、组件、API 接入落地 |
| `frontend-quality-regression` | 前端 bug 修复、审查、Playwright 回归 |
| `java-backend-maintenance` | Java 后端 bug 修复、Service 收口、Spring Boot 模块审查 |
| `java-backend-regression-tests` | JUnit、golden case、集成回归 |
| `db-schema-migration-review` | DDL、Flyway/Liquibase migration、索引/约束/注释审查 |
| `python-ops-tooling` | Python 批处理脚本、数据处理脚本、pytest 回归 |

## 使用原则

1. 所有 DH/NQ 任务先使用：
   - `nq-dh-workflow-router`

2. DH 文档治理、factsources、archive、work order、close/freeze 记录，使用：
   - `dh-docs-writer`

3. 前端页面从需求到上线，优先使用：
   - `frontend-product-ui-design`
   - `frontend-antd-page-builder`
   - `frontend-quality-regression`

4. 只做视觉提升时，使用：
   - `ui-visual-system-polish`

5. Java 后端问题闭环，使用：
   - `java-backend-maintenance`
   - `java-backend-regression-tests`

6. 数据库结构变更，使用：
   - `db-schema-migration-review`

7. Python 工具和批处理，使用：
   - `python-ops-tooling`

8. Stage archive 必须形成 self-contained archive packet，单个 `README.md` 不满足归档要求；Stage tag 只能在完整 archive packet 所在 commit 已存在且 tag close 任务明确授权后处理。archive close 与 tag close 必须分开，packet 不完整时 tag close BLOCKED。

9. 普通 batch 不默认触发 standalone review。只有 migration、API / Controller、安全边界、stage close、P0 / P1 blocker 才触发 review；普通 batch 以 implementation + tests + boundary scan + minimal docs + commit 为闭环。

10. Stage archive close 后、tag close 前不得进入下一阶段 planning；Stage-QDR-6 只能在 Stage-QDR-5 tag close 完成且 current cleanup 通过后另起 planning-first 任务。已关闭 stage 的 `DH_STAGE_*` source docs 必须迁入 `docs/gates/<stage>/SOURCE_*`，`docs/current` residue 未清零时下一阶段 planning BLOCKED。

## 对 NexusQuant / Decision Hub 的默认约束

- 前端默认走专业企业后台 / 金融科技后台风格，不走营销页风格。
- 默认技术栈：React 19 + TypeScript + Vite + React Router + TanStack Query + Axios + Zustand + Ant Design + Playwright。
- 服务端数据放 TanStack Query，不把服务端数据塞进 Zustand。
- 不为了 UI 优化新增后端 API、migration 或业务能力。
- 涉及交易、风控、恢复、停止、发布、撤单等操作时，必须展示影响范围并做二次确认。
- 路径以当前机器 `Get-Location` 返回的真实仓库为准，不硬编码盘符路径。
- NQ dev 与 NQ integration worktree 不得混用。
