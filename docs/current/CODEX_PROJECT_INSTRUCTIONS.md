# Codex Project Instructions

> Project: Decision Hub
> Required pre-skill: `nq-dh-workflow-router`
> Required docs skill: `dh-docs-writer`
> Source of truth: `docs/current`

## 1. 前置分类规则

每个 Codex 任务必须先执行 `nq-dh-workflow-router` 前置分类：

```text
1. 读取用户目标、范围、禁止项和验收标准。
2. 选择一个 Task classification。
3. 按分类选择 Plugins selected。
4. 收口 Scope，明确允许读取和修改的文件。
5. 检查 DH/NQ 安全边界。
6. 按标准输出格式执行和收尾。
```

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
除非用户明确要求历史对照，否则当前状态以 docs/current/STATUS.md、
docs/current/README.md、docs/current/ROADMAP.md、CODEX_PROJECT_INSTRUCTIONS.md
和 AGENTS.md 的当前段落为准。
历史 Stage 文档只能作为背景，不得自动转化为当前 next task。
DH 文档治理任务必须使用 .agents/skills/dh-docs-writer/SKILL.md。
```

分类只能从以下集合选择：

```text
DOCUMENTATION
CODE_ANALYSIS
CODE_CHANGE
SECURITY_AUDIT
AGENT_API
NQ_INTEGRATION_PLAN
PRODUCT_DESIGN
PRESENTATION
```

## 2. 插件选择

```text
DOCUMENTATION        GitHub + Documents + Notion
CODE_ANALYSIS        GitHub
CODE_CHANGE          GitHub + CodeRabbit
SECURITY_AUDIT       GitHub + Codex Security + CodeRabbit
AGENT_API            GitHub + OpenAI Developers + Codex Security
NQ_INTEGRATION_PLAN  GitHub + Documents + Codex Security
PRODUCT_DESIGN       Figma + Product Design，仅在 UI / 流程图任务中使用
PRESENTATION         Presentations + Documents + Canva
```

## 3. 标准输出格式

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

`Summary` 不作为必填字段。发现、结论、变更摘要必须写入 `Findings`。

## 4. DH 项目边界

```text
DH 是多 Agent 决策系统。
DH 当前只允许研究、分析、候选信号、风险解释、审计记录。
DH 不允许下单、撤单、修改策略状态、启动 Paper Run、访问交易所密钥、直接读写 NQ DB。
DH 到 NQ 的任何未来接入都必须从 Integration-0-PLAN 开始。
Integration-0 只能是只读边界、契约冻结、权限模型、审计模型，不允许真实业务打通。
```

## 5. 当前状态锁定

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
Current main line: DH-GATEK-DECISION-PIPELINE-MVP-PLAN.
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED.
NQ current planning baseline: GateN.
```

## 6. DOCUMENTATION 任务默认验证

```powershell
git status --short
git diff --check
git diff --stat
```

如果用户明确禁止业务代码变更，验证时还必须确认 diff 只落在允许的文档和规则文件。
