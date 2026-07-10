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
STAGE_QDR_6: PLANNING / WORK_ORDER_DONE / IMPLEMENTATION_NOT_STARTED
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_B1_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_6_B2_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_6_B4_IMPLEMENTATION_NOW: NO
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
current workspace: use Get-Location per run
current task: DH-STAGE-QDR-6-IMPLEMENTATION-WORK-ORDER
current task status: DONE / WORK_ORDER_ONLY
next action: DH-STAGE-QDR-6-B1-EVIDENCE-CORRELATION-AGGREGATE-CONTRACTS
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

Stage-QDR-4 归档和 tag close 不授权 real HTTP、real provider、Provider SDK、Agent runtime、LangGraph runtime、LIVE、NQ mutation 或 trading execution。Stage-QDR-5 已 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`，tag 为 `dh-stage-qdr-5-close`。`DH-STAGE-QDR-6-PLAN` 与 implementation work order 已完成；当前只允许进入 B1 Evidence Correlation / Aggregate Contracts，不允许 B2-B4、migration、API、Repository expansion、provider、Agent 或 LIVE。

## 当前事实源

Stage-QDR-5 已归档并完成 tag close。当前事实源只保留全局状态、下一步入口、验证证据和归档索引；Stage-QDR-5 过程源文件已迁入 `docs/gates/stage-qdr-5/`。

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
docs/gates/stage-qdr-4/                 Stage-QDR-4 归档目录与阶段文档
docs/gates/stage-qdr-5/                 Stage-QDR-5 归档目录与阶段文档
```

Stage-QDR-5 historical source docs 已归档到 `docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_*.md`；这些文件只作为 historical archive evidence，不是 current factsource。

## 文档入口

```text
docs/current/README.md                  当前文档索引
docs/current/DH_STAGE_QDR_6_PLAN.md     Stage-QDR-6 当前规划与范围冻结
docs/current/DH_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER.md Stage-QDR-6 实施工单与批次边界
docs/current/STATUS.md                  唯一当前状态表
docs/current/WORK_ORDER.md              唯一下一步入口
docs/current/CODEX_PROJECT_INSTRUCTIONS.md 当前 Codex / Claude 执行纪律
docs/current/TESTING.md                 当前验证证据与工具风险
docs/current/FACTSOURCE_POLICY.md       当前事实源与 blocker 规则
docs/current/ARCHIVE_INDEX.md           QDR 历史归档索引
docs/gates/stage-qdr-4/                 Stage-QDR-4 归档目录与阶段文档
docs/gates/stage-qdr-5/                 Stage-QDR-5 归档目录与阶段文档
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
