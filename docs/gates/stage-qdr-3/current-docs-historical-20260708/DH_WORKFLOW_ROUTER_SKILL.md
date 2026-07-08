# DH Workflow Router Skill

> Skill: `.agents/skills/nq-dh-workflow-router/SKILL.md`
> Status: active
> Purpose: task pre-classification, plugin routing, scope control, output format, DH/NQ boundary enforcement

## 1. 触发规则

所有 DH / NQ 相关 Codex 任务开工前必须先使用 `nq-dh-workflow-router` 做前置分类。

该 skill 只做：

```text
任务分类
插件建议
scope 收口
风险边界检查
标准输出格式统一
验收清单提示
```

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

敏感材料禁令：

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

该 skill 不授权：

```text
真实 provider
NQ RealClient
NQ DB 读写
NQ API mutation
Paper Run 启动
LIVE trading
密钥读取
业务代码越权修改
```

## 2. 分类优先级

```text
SECURITY_AUDIT
AGENT_API
NQ_INTEGRATION_PLAN
CODE_CHANGE
CODE_ANALYSIS
PRODUCT_DESIGN
PRESENTATION
DOCUMENTATION
```

任务含多个属性时，选择风险更高的分类。

## 3. 输出格式

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

禁止把 `Summary` 作为必填字段。需要总结时写入 `Findings`。

## 4. Integration-0 边界

未来任何 DH 到 NQ 的接入都必须先开 `Integration-0-PLAN`。

Integration-0 只能冻结：

```text
只读边界
契约 schema
认证与签名材料
防重放规则
权限模型
审计模型
错误码矩阵
payload 分级
风险清单
```

Integration-0 不允许：

```text
新增真实 API path
新增 migration
新增 RealClient
连接 NQ
读取 NQ DB
写入 NQ DB
启动 Paper Run
接 LIVE trading
```
