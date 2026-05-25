# Archived: 2026-02-04_M1_run_gate_mockprovider

> Archived: 2026-05-25
> Superseded by: Stage1 (Boundary Freeze + Agent Runtime Skeleton) + Stage1-CLOSE

## 为什么归档

本计划跟踪的是 DH 早期"多模型调用平台"路线（DecisionHubFacade + Mock provider + golden runner）。
2026-05-25 完成的 Stage1 已经把 DH 升级为"多 Agent 决策能力层"：
- 旧 `usecase.facade.DecisionHubFacade` / `usecase.run.*` / `usecase.gate.*` / `usecase.contract.*` /
  `dh-providers.*` 整体 `@Deprecated(since="Stage1-CLOSE", forRemoval=true)`。
- 新链路由 `usecase.agent.*`（ResearchRunCommandService / Planner / Generation / Review / Judge /
  ExperienceFeedback / NqIntegration）+ `domain.research/agent/candidate/judge/experience/feedback`
  + `memory.agent.*` + `eval.agent.*` + `connector.nq.*` 取代。

## 本计划未完成的步骤

| idx | 标题 | 处置 |
|---:|---|---|
| 4 | 实现 dh-eval Golden runner + Maven 绑定 verify | OBSOLETE。Stage1 用 dh-eval.agent.rule.* 规则 scorer + 闭环单测取代 |
| 5 | 引入 Checkstyle + verify.ps1 | OBSOLETE。Checkstyle 已在根 pom 强制，verify.ps1 留给 Stage2 |
| 6 | verify 全通过 + 变更记录 | OBSOLETE。验收记录改进入 docs/current/TESTING.md §3 |
| 7 | 修复配置规则文件问题（编码/密钥/Checkstyle 入口） | OBSOLETE。Stage1-CLOSE 已对全部 STATUS 文档做单源同步 |

## 历史快照

详见同目录的 STATUS.json（已设 `state=ARCHIVED`）。

## 后续

如需新计划，请参考 `docs/codex/plans/_active/STATUS.json`（Stage1 + Stage1-CLOSE）与
`docs/current/WORK_ORDER.md`（Stage2-PoC 草稿）。
