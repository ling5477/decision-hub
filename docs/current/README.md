# Decision Hub Current Docs

## 当前状态 — feedback side-effect containment local implementation accepted（2026-08-01）

- Stage-QDR-9：`CLOSED / ACCEPTED / ARCHIVED / TAGGED`。
- B1–B4：`CLOSED / ACCEPTED / PUBLISHED`；B5：`FINAL_CLOSE COMPLETE / GOVERNANCE ONLY`。
- Close commit：`88b1d6d8ea68c39eaa74486e5e0bcb6502e00036`；exact-SHA CI：`30640835327 / PASS`。
- Annotated tag：`dh-stage-qdr-9-close`；本地与远端 peeled target 均为 close commit。
- Archive：[Stage-QDR-9 packet](../gates/stage-qdr-9/README.md)；31/31 source copies 与 SHA-256 已验证。
- Current process sources：`31 / 31 PRUNED AS PLANNED`。
- Formal capacity：`NOT_EXECUTED / DEFERRED`；production capacity：`NOT_PROVEN`；production ready：`NO`。
- Reference-liveness、retention、V16/V17/V18、真实 HTTP/Provider/NQ/Agent/LangGraph/Paper/LIVE：未授权。
- Docs baseline：`49fa8442556bcc971119932421e1f606bc349054 / PUBLISHED / CI 30694264770 PASS`。
- Selected stage：`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT / IMPLEMENTED / LOCAL_ACCEPTED`。
- `FEEDBACK_SIDE_EFFECT_CONTAINMENT: IMPLEMENTED / LOCAL_ACCEPTED`；`REMOTE_IMPLEMENTATION_CI: PENDING`；
  `MILESTONE_FINAL_CLOSE: NOT_STARTED`。
- Implementation work order：[Feedback Side-Effect Containment Work Order](DH_PLATFORM_HARDENING_FEEDBACK_SIDE_EFFECT_CONTAINMENT_IMPLEMENTATION_WORK_ORDER.md)
  已完成 B1–B3：8 handlers 与 compatibility bean append-only，独立 Spring wiring 与 architecture guards 生效。
- Inbound boundary：`INGEST_ONLY / NO_IMPLICIT_LEARNING / NO_MUTABLE_LEARNING_STORE_ACCESS`；四类 inbound
  mutation caller/interactions 均为 0。
- Full regression：`1252 / 0 / 0 / 0`，PostgreSQL 17.10/Testcontainers 实跑；quality 19/19、Checkstyle 0、
  Spotless PASS。
- API、migration、Repository、contracts、POM/workflow、legacy environment 与 structured QDR path 均未改变。
- Implementation：`THIS_IMPLEMENTATION_COMMIT / LOCAL_ONLY / NOT_PUSHED / NO TAG`；remote CI pending。

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

`DH-PLATFORM-HARDENING-FEEDBACK-SIDE-EFFECT-CONTAINMENT-MILESTONE-FINAL-CLOSE`。只有独立授权后才可做
implementation security review、push、exact-SHA CI、archive close 与 tag；不得恢复 V16、retention、
reference-liveness、capacity，也不得接 NQ runtime、Agent、LangGraph、Provider 或 LIVE。

Stage-QDR-9 的原始 plan、work order、batch reviews、designs、errata 与 final-close evidence 已全部移入
[`docs/gates/stage-qdr-9/`](../gates/stage-qdr-9/)，不得再从 `docs/current` 恢复为 active authority。
