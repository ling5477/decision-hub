# Git 提交规范（强制）

## 1. Commit Message 格式（Conventional Commits）
格式：
<type>(<scope>): <subject>

可选：
<type>(<scope>): <subject> [plan:<planId>] [run:<runId>]

### type 取值（固定集合）
- feat：新增功能
- fix：修复 bug
- refactor：重构（不改功能）
- perf：性能优化
- test：新增/调整测试（JUnit/Golden）
- docs：文档变更
- chore：杂项（构建脚本、依赖升级、CI 等）
- build：构建系统/依赖
- ci：CI 配置
- revert：回滚

### scope 取值（建议与模块对齐）
- dh-app / dh-usecase / dh-domain / dh-common / dh-infra / dh-provider / dh-gate / dh-run / dh-ledger / dh-memory / dh-eval / docs / scripts

### subject 规则（强制）
- 简体中文，动词开头，表达“做了什么”
- 不要无语义词（update/adjust/misc）
- 50 字以内

示例：
- feat(dh-usecase): 新增 RunCreate 门面与最小实现 [plan:2026-02-04_M1_run_gate_mockprovider]
- test(dh-eval): 增加 run_create_001 golden 用例并绑定 verify [plan:2026-02-04_M1_run_gate_mockprovider]
- fix(dh-common): 修复全局异常处理返回码不一致 [plan:2026-02-06_M2_persistence_ledger]

## 2. 必须关联计划
凡是实现/修复类提交（feat/fix/refactor/perf/test/build/ci），必须在 commit message 或 PR 描述中关联 planId：
- planId 来自 docs/codex/plans/<planId>/PLAN.md
