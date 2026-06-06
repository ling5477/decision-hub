# Decision Hub Current Work Order

> Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
> Closed:        DH-CODEX-WORKFLOW conflict cleanup
> Next stage:    Integration-0-PLAN

## 1. 当前目标

下一步唯一允许工作内容是 Integration-0-PLAN。

Integration-0-PLAN 只允许输出规划文档，不允许实现真实接入：

```text
只读边界
契约草案
权限模型
审计模型
replay protection
tenant binding
request signing
timestamp / nonce
payload size limit
source allowlist
验收清单
风险清单
```

## 2. Stage1-CLOSE 已完成范围

```text
deprecation：旧 domain.run / api.run / usecase.facade / usecase.run / usecase.gate /
             usecase.contract / dh-providers 全部 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
迁移：     api.run.RunController -> api.legacy.run.RunController，REST 路径 /runs -> /legacy/runs
契约：     contracts/openapi.yaml 中 /runs -> /legacy/runs，并标 deprecated
文档单源： 根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
codex：    docs/codex/plans/_active/STATUS.json 切到 Stage1，老 M1 plan 归档到 _archive/2026-02-04_M1/
ArchUnit： 新增 4 条规则保护新边界
pom：      dh-eval parent 修回 dh-bom
```

## 3. Integration-0-PLAN（唯一下一步）

下一份工单只能以 `Integration-0-PLAN` 为主题；启动前必须先在 `docs/current/` 下输出
Integration-0 规划文档。

允许范围：

```text
输出 Integration-0-PLAN 文档
梳理 DH -> NQ 只读边界
定义契约草案
定义 scope token / permission model
定义 audit trail
定义 replay protection / tenant binding / request signing
定义 timestamp / nonce / payload size limit / source allowlist
定义验收清单
定义风险清单
```

当前状态必须保持：

```text
DH-AUDIT-FIX completed
NQ integration not started
Integration-0 not started / plan only
RealClient forbidden
real provider forbidden
LIVE trading forbidden
NQ mutation forbidden
```

## 4. Historical / superseded / deferred 内容

```text
NqFeedbackClient 接通真实 HTTP / event         forbidden / deferred
NqBacktestClient 接通真实 NQ                    forbidden / deferred
NQ /api/ai/research/backtest-requests           forbidden / deferred
RealNqBacktestClient / RealClient               forbidden
real provider                                   forbidden
真实 HTTP / event 到 NQ                         forbidden
启动 Paper Run                                  forbidden
修改 NQ 交易状态                                forbidden
访问交易所密钥                                  forbidden
LIVE trading                                    forbidden
```

以上内容只保留为历史背景，不是当前 next，不允许作为当前实现任务。后续如需恢复，必须先通过 Integration-0-PLAN 的安全审查、契约冻结和人工确认。

## 5. 不做事项（持续硬约束）

```text
不修改 NQ 仓库交易核心
不实现真实下单
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不建设第二套完整前端
不引入 BCO/ACO/GWO 等重型数学优化器
不把 Kronos / TradingAgents / global-stock-data 整体复制进 DH/NQ
不新增 API / migration / provider / NQ client / RealClient / 交易路径
不读取 token / cookie / exchange secret / production .env / API key / private key / mnemonic / 2FA backup code
```

## 6. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Integration-0-PLAN。

目标：只输出 DH -> NQ 未来接入前的只读边界、契约草案、权限模型、审计模型、
replay protection、tenant binding、request signing、timestamp / nonce、payload size limit、
source allowlist、验收清单和风险清单。

禁止：
- 不实现真实 NQ client
- 不实现 RealClient
- 不接真实 HTTP / event 到 NQ
- 不调用 NQ /api/ai/research/backtest-requests
- 不启动 Paper Run
- 不修改 NQ 交易状态
- 不访问交易所密钥
- 不触碰 LIVE trading
- 不读取或写入 NQ DB
- 不新增 API / migration / provider / 交易路径

不要写业务代码。本轮只产出 Integration-0-PLAN 文档草案。
```
