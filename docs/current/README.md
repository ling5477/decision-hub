# Decision Hub Current Docs

## 当前状态 — feedback side-effect containment work order frozen（2026-08-01）

- Stage-QDR-9：`CLOSED / ACCEPTED / ARCHIVED / TAGGED`。
- B1–B4：`CLOSED / ACCEPTED / PUBLISHED`；B5：`FINAL_CLOSE COMPLETE / GOVERNANCE ONLY`。
- Close commit：`88b1d6d8ea68c39eaa74486e5e0bcb6502e00036`；exact-SHA CI：`30640835327 / PASS`。
- Annotated tag：`dh-stage-qdr-9-close`；本地与远端 peeled target 均为 close commit。
- Archive：[Stage-QDR-9 packet](../gates/stage-qdr-9/README.md)；31/31 source copies 与 SHA-256 已验证。
- Current process sources：`31 / 31 PRUNED AS PLANNED`。
- Formal capacity：`NOT_EXECUTED / DEFERRED`；production capacity：`NOT_PROVEN`；production ready：`NO`。
- Reference-liveness、retention、V16/V17/V18、真实 HTTP/Provider/NQ/Agent/LangGraph/Paper/LIVE：未授权。
- Next-stage plan：[DH Post-Stage-QDR-9 Next Stage Plan](DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md)。
- Selected stage：`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT / SCOPE FROZEN`。
- Implementation work order：[Feedback Side-Effect Containment Work Order](DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md)
  已完成实际调用链、`INGEST_ONLY / NO_IMPLICIT_LEARNING`、精确 allowlist 与 test matrix freeze。
- Work-order task implementation：`NOT EXECUTED`；下一 consolidated implementation task：`AUTHORIZED / EXACT SCOPE ONLY`。
- 选择原因：当前 legacy NQ feedback ingress 仍隐式调用 Experience/Pheromone mutation；下一阶段只做
  default-deny containment，不实现 feedback learning。

## Current authority

- [STATUS.md](STATUS.md)：primary current-state authority。
- [WORK_ORDER.md](WORK_ORDER.md)：唯一当前工单与 next action。
- [FACTSOURCE_POLICY.md](FACTSOURCE_POLICY.md)：factsource hierarchy 与 full-sync 规则。
- [ROADMAP.md](ROADMAP.md)：当前路线和 deferred capability 边界。
- [TESTING.md](TESTING.md)：append-only validation evidence。
- [WORKLOG.md](WORKLOG.md)：append-only execution ledger。
- [ARCHIVE_INDEX.md](ARCHIVE_INDEX.md)：历史 archive 与 tag 索引。
- [CODEX_PROJECT_INSTRUCTIONS.md](CODEX_PROJECT_INSTRUCTIONS.md)：执行指导。

## 唯一下一动作

`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-CONSOLIDATED-IMPLEMENTATION`，仅允许按冻结工单
在一个任务内执行 B1–B3、完整回归与一个本地 implementation commit；不得 push、tag 或扩大 scope，不得恢复
V16、retention、reference-liveness、capacity，也不得接 NQ runtime、Agent、LangGraph、Provider 或 LIVE。

Stage-QDR-9 的原始 plan、work order、batch reviews、designs、errata 与 final-close evidence 已全部移入
[`docs/gates/stage-qdr-9/`](../gates/stage-qdr-9/)，不得再从 `docs/current` 恢复为 active authority。
