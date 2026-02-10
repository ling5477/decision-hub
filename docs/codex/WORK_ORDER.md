# WORK_ORDER（唯一权威）

本仓库为 **Codex 续跑型工作流**：计划与状态落盘驱动执行。为避免空转，所有规则只在本文件定义，其他文件不得复制口径。

## 启动必读（强制，最小集合）
1) `docs/codex/WORK_ORDER.md`
2) `docs/codex/plans/_active/STATUS.json`（可选再读：`PLAN.md`）

> 说明：当前激活计划固定映射到 `docs/codex/plans/_active/`。  
> `PLAN_QUEUE.json` 与 `PLAN_CURRENT_POINTER.json` 仅用于**人类排期/追溯**，不再作为 Codex 决策必读与门禁依赖。

## 状态机与推进规则（强制）
- `STATUS.json.activeStepIdx` **必须等于** `steps` 中第一个 `status != DONE` 的 `idx`（即“第一个未完成步骤”）。
- `IN_PROGRESS` 只允许出现在 `idx == activeStepIdx` 的步骤上（最多 1 个）。
- `status` 枚举：`TODO | IN_PROGRESS | DONE | BLOCKED | SKIPPED`。
- 若步骤被阻塞：设置 `status=BLOCKED`，并在 `evidence` 中写明阻塞原因与解除条件（可引用 ISSUE 或依赖项）。
- 若确需跳过：使用 `SKIPPED`，并在 `evidence` 中写明原因与风险；禁止通过修改 `activeStepIdx` 实现隐式跳步。

## 计划与落盘（强制）
- 涉及实现/修改时：必须将计划写入 `docs/codex/plans/_active/PLAN.md`（可追加，不要覆盖历史），再开始修改代码/配置。
- 每次推进（至少一个步骤状态变化）后：必须更新 `STATUS.json.updatedAt`，并补充该步骤 `evidence`。
    - `evidence` 至少包含：
        - `files`：本次涉及的文件路径列表（新增/修改/删除）
        - `notes`：关键说明（做了什么、为什么、风险/回退）
        - 若执行了 verify：`verifySummary`（PASS/FAIL + 关键摘要）
- **禁止只改代码不落盘状态**；否则下一轮会被判定未闭环，易导致重复执行/空转。

## 文件修改方式（强制，防止空转与审计噪音）
- **唯一允许的文件写入方式：使用 apply_patch 工具修改/新增/删除文件。**
- 禁止通过 shell 写文件：包括但不限于 `cat > file`、here-string、重定向、`Out-File`、`Set-Content`、`echo ... >` 等。
- 允许使用 shell **只读**操作：如 `Get-Content -Encoding utf8`、`rg/grep`、`ls`、`git diff`、`mvn` 等。
- 若不慎发生违规写入：**直接立刻用 apply_patch 重新应用等价变更**并继续闭环，**不要在输出里反复说明/道歉**。
- 读取文本文件一律使用 UTF-8：PowerShell 中必须显式 `Get-Content -Encoding utf8`，避免默认编码导致中文乱码。

## 输出纪律（强制，禁止合规复读）
- 输出中**禁止**出现工具合规自检/道歉/过程复盘（如“我误用了…后续我会…”）。
- 输出只允许包含以下四类信息：
    1) **变更清单**：修改/新增/删除的文件路径列表
    2) **验证结果**：`scripts/verify.ps1` 的 PASS/FAIL + 关键摘要（或说明为何 SKIPPED）
    3) **状态落盘**：本次更新了 `STATUS.json` 哪些字段（step 状态/evidence/updatedAt/lastVerify）
    4) **下一步**：明确下一步要做的 step（或 BLOCKED 原因与 nextAction）

## 执行纪律（强制，防止“只回复不执行/确认循环”）
- 禁止在执行中输出“请确认我继续/等待确认”等话术；除非出现硬阻塞（ENV/PERMISSION/MISSING_FILE），否则必须继续推进。
- 每轮响应必须满足二选一：
    - A) **至少执行一个仓库内动作**（命令或 apply_patch），并给出“命令 + 输出摘要”；或
    - B) 输出 `BLOCKED`，并给出：
        - `reason`（枚举：`ENV | PERMISSION | MISSING_FILE | RULE_CONFLICT | UNKNOWN`）
        - `nextAction`（下一步需要人工做什么/提供什么）
- `taskType=delivery` 任务**禁止网络搜索/web browsing**。如确需外部资料：先写入 `PLAN.md`，并将当前 step 标记 `BLOCKED`，等待人工确认。
- 交付类（delivery）步骤的最小执行序列（推荐遵循，避免空转）：
    1) `rg/ls` 定位范围（最多 2 条命令）
    2) `apply_patch` 最小改动（最多 3 个文件）
    3) `pwsh ./scripts/verify.ps1`
    4) 更新 `_active/STATUS.json`（evidence + lastVerify）
    5) 输出 `git diff --stat`

## 任务类型与验证门禁（强制）
- `taskType=delivery`：存在代码/配置/契约变更，必须运行 `scripts/verify.ps1`（本质 `mvn verify`），并回填 `STATUS.json.lastVerify`：
    - `time`：执行时间
    - `result`：`PASS | FAIL`
    - `summary`：关键摘要（模块/测试范围/失败摘要等）
- `taskType=analysis-only`：无仓库文件变更，可跳过 verify，但必须回填 `lastVerify.result=SKIPPED` 与原因，并保证不改动文件。
- `updatedAt` / `lastVerify.time` 必须使用 UTC：yyyy-MM-dd'T'HH:mm:ss'Z'（例：2026-02-10T03:24:08Z）

## 反空转护栏（强制）
- 若在同一 `activeStepIdx` 上连续 2 次执行没有产生任何文件变更或没有更新 `STATUS.json` 的 evidence，则必须：
    - 将该 step 标记为 `BLOCKED`，并写明 `evidence.notes`（阻塞原因 + nextAction + 解除条件）
    - 禁止继续“重复尝试同一动作”而不落盘状态
- 若 verify 连续失败 2 次，必须在 evidence 中记录：
    - 失败类型（lint/compile/test/schema/encoding 等）
    - 最小复现步骤与下一步修复计划（避免反复跑同一 verify 耗时空转）

## 变更记录（强制）
- 发生 `delivery` 变更后：追加记录到 `docs/codex/CHANGE_NOTES.md`（变更点 + 影响范围 + verify 结论）。

## MCP 使用与降级（强制）
- 若 IntelliJ MCP 可用：优先用于语义重构/批量改引用。
- 若不可用：必须采用保守降级（小步修改 + 搜索/编译/测试验证），禁止盲目全局替换。

## PR/提交前（建议）
- `.github/PULL_REQUEST_TEMPLATE.md` 仅用于 GitHub PR 描述模板；**不得**在其中复制/定义 Codex 状态机口径、执行顺序或字段规范（避免形成第二规则源）。
- 当准备开 PR 或合并提交前：按模板填写 `activePlanId`、milestone、verify 摘要与测试勾选，并在 PR 描述中附上关键证据链接/路径。
