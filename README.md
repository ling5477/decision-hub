# Decision Hub

## Current authority — Stage-QDR-7 B2 milestone close

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED / ACCEPTED
Schema errata implementation: ACCEPTED
Persistent guards implementation: ACCEPTED
Post-B2 capacity acceptance: NOT_STARTED / NEXT
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE
current task status: DONE / B2_MILESTONE_CLOSED
next task: DH-STAGE-QDR-7-B2-POST-IMPLEMENTATION-CAPACITY-ACCEPTANCE
```

schema errata与persistent guards已基于既有技术证据正式`ACCEPTED`，B2 milestone已`CLOSED / ACCEPTED`。下一步只允许独立执行post-B2 capacity acceptance；本结论不授权capacity benchmark在本任务运行，也不授权B3、API、外部HTTP/provider、NQ、Agent/LangGraph或LIVE。

权威层级固定为：

```text
Primary current-state authority:
docs/current/STATUS.md
docs/current/WORK_ORDER.md

Policy authority:
docs/current/FACTSOURCE_POLICY.md

Execution guidance:
AGENTS.md
CLAUDE.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md

Entry/index documents:
README.md
docs/current/README.md
```

执行指导和入口不得覆盖主权威。

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易执行系统。DH 负责候选方案、风险解释、审计记录、结构化报告和辅助决策；交易核心、账户资产、订单状态机、风控执行、正式回测、模拟盘/实盘执行和交易事实源仍由 NexusQuant 承担。

## 已关闭阶段历史摘要（非当前）

本节只保留Stage-QDR-2至Stage-QDR-6及已消费入口的historical/consumed记录。当前任务和下一任务只以上方`Current authority`、`docs/current/STATUS.md`和`docs/current/WORK_ORDER.md`为准。

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
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 B2: CLOSED / ACCEPTED
stage-qdr-4 B3: CLOSED / ACCEPTED
stage-qdr-4 B4: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B2_CI_BLOCKER_FIX: DONE
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: COMPLETE / B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE_COMMITTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS / 5961164
CURRENT_FACTSOURCE_CONFLICT: CLEARED
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET_NOW: YES / CONSUMED
ALLOW_STAGE_QDR_6_TAG_CLOSE_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_7_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
historical workspace note: use Get-Location per run
```

## Stage-QDR-4 归档状态

```text
Stage-QDR-4 Replay / Evaluation / Regression Baseline: CLOSED / ACCEPTED / ARCHIVED
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
```

Stage-QDR-4、Stage-QDR-5 与 Stage-QDR-6 均已 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。本节不是Stage-QDR-7当前入口；当前B2状态只以上方authority与`STATUS.md`/`WORK_ORDER.md`为准。

## 当前事实源

Stage-QDR-5 已归档并完成 tag close。当前事实源只保留全局状态、下一步入口、验证证据和归档索引；Stage-QDR-5 过程源文件已迁入 `docs/gates/stage-qdr-5/`。

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/DH_STAGE_QDR_7_B2_CONSOLIDATED_FINAL_ACCEPTANCE_REVIEW.md B2 milestone close / includes previous BLOCKED review
docs/current/DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md Historical / consumed work order with B2 close addendum
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/ARCHIVE_INDEX.md historical archive index / supporting only
docs/gates/stage-qdr-4/                 Stage-QDR-4 归档目录与阶段文档
docs/gates/stage-qdr-5/                 Stage-QDR-5 归档目录与阶段文档
docs/gates/stage-qdr-6/                 Stage-QDR-6 self-contained archive packet
```

Stage-QDR-5 historical source docs 已归档到 `docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_*.md`；这些文件只作为 historical archive evidence，不是 current factsource。

## 文档入口

```text
docs/current/README.md                  当前文档索引
docs/current/DH_STAGE_QDR_7_PLAN.md     Historical / consumed Stage-QDR-7 planning baseline
docs/current/DH_STAGE_QDR_7_B2_CONSOLIDATED_FINAL_ACCEPTANCE_REVIEW.md B2 milestone close；保留Previous review attempt / BLOCKED
docs/current/DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md Historical / consumed；顶部含B2 close addendum
docs/current/STATUS.md                  唯一当前状态表
docs/current/WORK_ORDER.md              唯一下一步入口
docs/current/CODEX_PROJECT_INSTRUCTIONS.md 当前 Codex / Claude 执行纪律
docs/current/TESTING.md                 当前验证证据与工具风险
docs/current/FACTSOURCE_POLICY.md       当前事实源与 blocker 规则
docs/current/ARCHIVE_INDEX.md           QDR 历史归档索引
docs/gates/stage-qdr-4/                 Stage-QDR-4 归档目录与阶段文档
docs/gates/stage-qdr-5/                 Stage-QDR-5 归档目录与阶段文档
docs/gates/stage-qdr-6/                 Stage-QDR-6 self-contained archive packet
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
