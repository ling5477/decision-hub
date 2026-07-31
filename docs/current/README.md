# Decision Hub Current Docs

## 当前状态 — Stage-QDR-9 closed（2026-07-31）

- Stage-QDR-9：`CLOSED / ACCEPTED / ARCHIVED / TAGGED`。
- B1–B4：`CLOSED / ACCEPTED / PUBLISHED`；B5：`FINAL_CLOSE COMPLETE / GOVERNANCE ONLY`。
- Close commit：`88b1d6d8ea68c39eaa74486e5e0bcb6502e00036`；exact-SHA CI：`30640835327 / PASS`。
- Annotated tag：`dh-stage-qdr-9-close`；本地与远端 peeled target 均为 close commit。
- Archive：[Stage-QDR-9 packet](../gates/stage-qdr-9/README.md)；31/31 source copies 与 SHA-256 已验证。
- Current process sources：`31 / 31 PRUNED AS PLANNED`。
- Formal capacity：`NOT_EXECUTED / DEFERRED`；production capacity：`NOT_PROVEN`；production ready：`NO`。
- Reference-liveness、retention、V16/V17/V18、真实 HTTP/Provider/NQ/Agent/LangGraph/Paper/LIVE：未授权。

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

`DH-POST-STAGE-QDR-9-NEXT-STAGE-PLANNING`，仅允许 planning/scope freeze。Cleanup commit 发布并完成
远端对齐前不得启动；该 planning 不自动选择 V16、retention、reference-liveness、capacity 或部署，
也不授权任何 next-stage implementation。

Stage-QDR-9 的原始 plan、work order、batch reviews、designs、errata 与 final-close evidence 已全部移入
[`docs/gates/stage-qdr-9/`](../gates/stage-qdr-9/)，不得再从 `docs/current` 恢复为 active authority。
