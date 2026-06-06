# DH Codex Plugin Workflow

> Status: active workflow rule
> Scope: Decision Hub Codex / Agent tasks
> Integration state: NQ integration not started; Integration-0 not started / plan only

## 1. 目标

本文件固化 DH 仓库的 Codex 插件路由规则。插件只用于工作分类和辅助执行，不改变 DH/NQ 安全边界，不授权真实 provider、RealClient、数据库访问或交易能力。

## 1.1 通用执行边界

开工前必须明确：

```text
repository
module
target files
excluded files
expected output
```

默认排除目录：

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

禁止读取、复制、提交或输出以下敏感材料：

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
除非用户明确要求历史对照，否则当前状态以 STATUS.md、AGENTS.md、
CODEX_PROJECT_INSTRUCTIONS.md、WORK_ORDER.md 的当前段落为准。
历史 Stage 文档只能作为背景，不得自动转化为当前 next task。
```

## 2. 插件路由矩阵

| Task classification | Plugins selected | 使用边界 |
| --- | --- | --- |
| `DOCUMENTATION` | GitHub + Documents + Notion | 仅文档、索引、状态、模板、规则固化。 |
| `CODE_ANALYSIS` | GitHub | 只读分析代码、diff、历史记录和风险点。 |
| `CODE_CHANGE` | GitHub + CodeRabbit | 允许按工单改代码和测试；必须遵守当前 Gate。 |
| `SECURITY_AUDIT` | GitHub + Codex Security + CodeRabbit | 审查风险、攻击面、敏感数据、权限边界和回归。 |
| `AGENT_API` | GitHub + OpenAI Developers + Codex Security | 仅在明确要求 Agent/API 能力时使用；必须先做安全边界检查。 |
| `NQ_INTEGRATION_PLAN` | GitHub + Documents + Codex Security | 只允许规划、契约冻结、安全模型和风险清单；不得真实打通。 |
| `PRODUCT_DESIGN` | Figma + Product Design | 仅 UI / 流程图任务；不改变 API、DB、交易边界。 |
| `PRESENTATION` | Presentations + Documents + Canva | 仅汇报材料、PPT、展示稿和图文资产。 |

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

`Summary` 不是必填字段；所有发现、结论和变更说明都必须进入 `Findings`。

## 4. DH/NQ 安全边界

```text
DH 是多 Agent 决策系统。
DH 当前只允许研究、分析、候选信号、风险解释、审计记录。
DH 不允许下单、撤单、修改策略状态、启动 Paper Run、访问交易所密钥、直接读写 NQ DB。
DH 到 NQ 的任何未来接入都必须从 Integration-0-PLAN 开始。
Integration-0 只能是只读边界、契约冻结、权限模型、审计模型，不允许真实业务打通。
```

当前状态保持：

```text
DH-AUDIT-FIX completed.
NQ integration not started.
Integration-0 not started / plan only.
RealClient forbidden.
real provider forbidden.
LIVE trading forbidden.
NQ mutation forbidden.
```
