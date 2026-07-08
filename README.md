# Decision Hub

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易执行系统。DH 负责候选方案、风险解释、审计记录、结构化报告和辅助决策；交易核心、账户资产、订单状态机、风控执行、正式回测、模拟盘/实盘执行和交易事实源仍由 NexusQuant 承担。

## 当前阶段

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/project/decision-hub
next action: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

## 当前事实源

stage-qdr-3 final close 与 stage-qdr-4 planning 入口只应以以下文件作为当前事实源：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
```

事实源规则见 `docs/current/FACTSOURCE_POLICY.md`；归档索引见 `docs/current/ARCHIVE_INDEX.md`。

## 文档入口

```text
docs/current/README.md                  当前文档索引
docs/current/STATUS.md                  唯一当前状态表
docs/current/WORK_ORDER.md              唯一下一步入口
docs/current/CODEX_PROJECT_INSTRUCTIONS.md 当前 Codex / Claude 执行纪律
docs/current/TESTING.md                 当前验证证据与工具风险
docs/current/DH_STAGE_QDR_4_PLAN.md     stage-qdr-4 planning 结论
docs/current/FACTSOURCE_POLICY.md       当前事实源与 blocker 规则
docs/current/ARCHIVE_INDEX.md           QDR 历史归档索引
```

`docs/current/WORKLOG.md`、`ROADMAP.md`、`API.md`、`DB_SCHEMA.md` 是 supporting documents，不是 primary stage gate source。旧阶段工单、旧 review / freeze 记录、blocker fix 过程和中间产物已经移动或索引到 `docs/gates/**`，不得覆盖 `STATUS.md` 与 `WORK_ORDER.md` 的当前结论。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档口径；若未来重新出现，只能作为历史遗留引用。

## 硬边界

```text
不修改 NQ 仓库
不直接下单
不绕过 NQ 风控
不替代 NQ 订单状态机
不重写 NQ 回测核心
不新增真实 HTTP outbound
不新增真实 provider
不新增 Provider SDK
不启动 Agent / LangGraph runtime
不启用 LIVE
```

## 验证入口

当前 docs-only 治理的最低验证：

```powershell
git status --short
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍按 `UNUSABLE / P2 TOOLING RISK` 处理；可用验证工具是系统 Maven `mvn`。Docker/Testcontainers skip 只能记录为环境型 skip，不得写成 PASS。
