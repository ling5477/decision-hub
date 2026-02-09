> # Git 提交规范（强制）
>
> ## 1. Commit Message 格式（Conventional Commits）
>
> 基本格式：
>
> ```
> <type>(<scope>): <subject>
> ```
>
> 可选（计划 / 运行关联）：
>
> ```
> <type>(<scope>): [plan:<activePlanId>] [run:<runId>] <subject>
> ```
>
> > 说明：
> >
> > - `[plan:<activePlanId>]`：关联执行计划（可选）
> > - `[run:<runId>]`：关联运行实例（可选）
> > - 以上两个标记为**纯文本语义标记**，不是链接，占位符仅用于追踪与审计
>
> ------
>
> ## 2. type 取值（固定集合）
>
> - **feat**：新增功能
> - **fix**：修复 bug
> - **refactor**：重构（不改功能）
> - **perf**：性能优化
> - **test**：新增 / 调整测试（JUnit / Golden）
> - **docs**：文档变更
> - **chore**：杂项（构建脚本、依赖升级、CI 等）
> - **build**：构建系统 / 依赖
> - **ci**：CI 配置
> - **revert**：回滚
>
> ------
>
> ## 3. scope 取值（建议与模块对齐）
>
> - dh-app
> - dh-usecase
> - dh-domain
> - dh-common
> - dh-infra
> - dh-providers
> - dh-connector
> - dh-ledger
> - dh-memory
> - dh-knowledge
> - dh-eval
> - docs
> - scripts
>
> > 建议 scope 与 Maven module 或业务子域保持一致，避免随意扩散。
>
> ------
>
> ## 4. subject 规则（强制）
>
> - 使用 **简体中文**
> - 以 **动词开头**，明确表达“做了什么”
> - 不要使用无语义词（如：update / adjust / misc）
> - 建议 **50 字以内**
>
> ------
>
> ## 5. 示例
>
> ```
> feat(dh-usecase): 新增 RunCreate 门面与最小实现 [plan:2026-02-04_M1_run_gate_mockprovider]
> 
> test(dh-eval): 增加 run_create_001 golden 用例并绑定 verify [plan:2026-02-04_M1_run_gate_mockprovider] [run:run_create_001]
> 
> fix(dh-common): 修复全局异常处理返回码不一致 [plan:2026-02-06_M2_persistence_ledger]
> ```
>
> ------
>
> ## 6. 必须关联计划（强制）
>
> 凡是 **实现 / 修复 / 结构调整类提交**（包括但不限于 `feat / fix / refactor / perf / test / build / ci`），
> **必须在 commit message 或 PR 描述中关联 activePlanId**。
>
> - activePlanId**** 来源：
>
> ```
> docs/codex/plans/<activePlanId>/PLAN.md
> ```
>
> ------
>
> ## 7. 设计说明（为什么要有 plan / run）
>
> 本项目采用 **Plan → Run → Eval（Golden）** 的工程闭环：
>
> - **Plan**：一次明确的设计 / 决策 /执行计划
> - **Run**：某次具体执行或实验实例（可能多次）
> - **Eval / Golden**：结果验证与回归
>
> 通过在 Git 提交中引入 `[plan:]` / `[run:]` 语义标记：
>
> - 可以从代码层面反向追踪决策与执行
> - 支持未来自动化生成：Plan → Commits → Runs → Golden 结果
> - 提供工程级可审计性（而不仅是代码 diff）
>
> ------
>
> > 本规范为 **Decision Hub / 多阶段决策系统** 的工程级约定，
> > 属于“系统语义的一部分”，不是普通格式建议。
