# Decision Hub Archive Index

## Stage-QDR-6 archive readiness（2026-07-11）

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW: BLOCKED / CURRENT_FACTSOURCE_CONFLICT
STAGE_QDR_6: IMPLEMENTATION_COMPLETE / FINAL_CLOSE_BLOCKED / NOT_ARCHIVED / NOT_TAGGED
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: NO
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: NO
archive packet: NOT_CREATED
archive commit: NOT_CREATED
tag: NOT_CREATED
next action: DH-STAGE-QDR-6-FINAL-CLOSE-BLOCKER-FIX
```

Final close review 来源为 `docs/current/DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md`。Stage-QDR-6 因 current factsource 冲突尚未通过 final close，不得创建 archive packet、archive commit 或 tag。必须先完成 blocker fix 并让 final close review PASS，之后才可进入 archive-before-tag 顺序。

## 1. 归档原则

归档文件保留历史信息，不删除历史，不重写历史。归档文件不是 current factsource，不得覆盖 `docs/current/STATUS.md` 或 `docs/current/WORK_ORDER.md`。

当前 QDR 阶段归档目录统一为：

```text
docs/gates/**
```

`docs/archive/**` 不再作为本项目 QDR 阶段的新归档口径。若历史遗留目录未来重新出现，只能作为 historical reference，不能作为 current factsource 或 B5 close review blocker。

## 2. stage-qdr-2

归档目录：

```text
docs/gates/stage-qdr-2/
```

当前内容：

```text
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_WORK_ORDER.md
docs/gates/stage-qdr-2/DH_STAGE_QDR_2_DISCIPLINE_CLOSEOUT.md
```

内容摘要：

```text
stage-qdr-2 final close 已 FINAL CLOSE CLOSED / ACCEPTED。
上述文件记录已完成阶段的详细 work order 与 discipline closeout 过程。
它们仍有复盘价值，但不应作为 stage-qdr-3 B5 close review 的 current blocker。
```

## 3. stage-qdr-3

归档目录：

```text
docs/gates/stage-qdr-3/
```

当前内容：

```text
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md
docs/gates/stage-qdr-3/DH_STAGE_QDR_3_IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-3/pre-close-current-snapshot-20260707/
docs/gates/stage-qdr-3/current-docs-historical-20260708/
```

内容摘要：

```text
stage-qdr-3 planning 与 implementation work order 已由 B1-B4 消费。
B1 已 DONE / COMMITTED。
B2/B3/B4 已 DONE / FREEZE ACCEPTED / COMMITTED。
B5 close review 已 YES / ACCEPTED。
stage-qdr-3 acceptance 已 ACCEPTED。
stage-qdr-3 final close 已 CLOSED / ACCEPTED。
stage-qdr-4 planning 已 DONE / PLAN_ACCEPTED。
stage-qdr-4 已 CLOSED / ACCEPTED / ARCHIVED。
stage-qdr-4 tag close 已 DONE。
```

`pre-close-current-snapshot-20260707/` 保存上一轮压缩 current docs 前的入口文件快照。`current-docs-historical-20260708/` 保存本轮从 `docs/current` 移出的历史阶段文档、旧 integration planning / review / work order、审计报告、旧 workflow support 文档和其他非 current blocker 文件。

这些归档文档均为 historical records。它们不覆盖 current `STATUS.md`、`WORK_ORDER.md`、`CODEX_PROJECT_INSTRUCTIONS.md` 或 `TESTING.md`。

## 4. stage-qdr-4

归档目录：

```text
docs/gates/stage-qdr-4/
```

当前内容：

```text
docs/gates/stage-qdr-4/README.md
docs/gates/stage-qdr-4/PLAN.md
docs/gates/stage-qdr-4/IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-4/BATCH_SUMMARY.md
docs/gates/stage-qdr-4/VALIDATION_EVIDENCE.md
docs/gates/stage-qdr-4/FINAL_CLOSE_REVIEW.md
docs/gates/stage-qdr-4/ARCHIVE_CLOSE.md
docs/gates/stage-qdr-4/DISCIPLINE_REPAIR.md
docs/gates/stage-qdr-4/STATUS_SNAPSHOT.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

内容摘要：

```text
Stage-QDR-4 Replay / Evaluation / Regression Baseline
Status: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Final close review: PASS
Archive close: DONE
Tag close: DONE
Tag: DONE / dh-stage-qdr-4-close
Tag target: 62c8020 docs(workflow): repair documentation discipline and skill policy
Archive close docs commit: 3689251 docs(qdr): archive stage-qdr-4 replay evaluation baseline
Archive content fix: Stage-QDR-4 current stage documents copied into docs/gates/stage-qdr-4
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
```

Stage-QDR-4 归档记录只作为 historical record 和 tag close 证据，不授权 real HTTP、real provider、Provider SDK、Agent runtime、LangGraph runtime、LIVE、NQ mutation 或 trading execution。

## 5. stage-qdr-5

归档目录：

```text
docs/gates/stage-qdr-5/
```

当前内容：

```text
docs/gates/stage-qdr-5/README.md
docs/gates/stage-qdr-5/PLAN.md
docs/gates/stage-qdr-5/IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-5/BATCH_SUMMARY.md
docs/gates/stage-qdr-5/VALIDATION_EVIDENCE.md
docs/gates/stage-qdr-5/B3_SECURITY_CLOSE_REVIEW.md
docs/gates/stage-qdr-5/FINAL_CLOSE_REVIEW.md
docs/gates/stage-qdr-5/ARCHIVE_CLOSE.md
docs/gates/stage-qdr-5/DISCIPLINE_REPAIR.md
docs/gates/stage-qdr-5/STATUS_SNAPSHOT.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_PLAN.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md
docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO.md
```

内容摘要：

```text
Stage-QDR-5 Model Gateway Observability / Provider Readiness Hardening
Status: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Final close review: PASS
Archive close: DONE
Tag state: DONE / dh-stage-qdr-5-close
Tag: dh-stage-qdr-5-close
B1 Model Gateway Observability Contracts: DONE
B2 Provider Health / Gateway Call Read Model: DONE
B3 Provider Readiness Guard / Policy Evaluation: CLOSED / ACCEPTED
B3 close review: PASS
B4 Observability Report / Acceptance Support: DONE
Archive policy: REPAIRED
Archive packet policy: REQUIRED_FOR_ALL_FUTURE_STAGES
Stage-QDR-6: NOT_STARTED
Source docs: ARCHIVED_UNDER_DOCS_GATES
```

Stage-QDR-5 归档记录只作为 historical record 和 tag close 前审计证据，不授权 real HTTP、real provider、Provider SDK、Agent runtime、LangGraph runtime、LIVE、NQ mutation、trading signal、raw prompt / raw provider response / credential storage。Stage-QDR-5 tag close 已完成；后续 Stage-QDR-6 只能作为独立 planning-first 任务启动。

## 6. Current Factsource Pointers

```text
Current status: docs/current/STATUS.md
Current next action: docs/current/WORK_ORDER.md
Factsource policy: docs/current/FACTSOURCE_POLICY.md
Validation evidence: docs/current/TESTING.md
Current docs index: docs/current/README.md
Stage-QDR-5 source docs: docs/gates/stage-qdr-5/SOURCE_DH_STAGE_QDR_5_*.md
Stage-QDR-5 status: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Stage-QDR-5 final close review: PASS
Stage-QDR-5 archive: DONE
Stage-QDR-5 tag: DONE / dh-stage-qdr-5-close
Stage-QDR-4 archive directory: docs/gates/stage-qdr-4/
Stage-QDR-5 archive directory: docs/gates/stage-qdr-5/
Archive policy: REPAIRED
Archive packet policy: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
Next action: DH-STAGE-QDR-6-PLAN
```

## 7. Post-close Rule

`DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW` 已 `PASS`，Stage-QDR-4 已归档并打 tag，为 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。`DH-STAGE-QDR-5-PLAN`、implementation work order、B1、B2、B3、B4、final close review、archive close 与 tag close 均已完成；本轮补齐 post-tag current cleanup。Stage-QDR-5 当前为 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`，tag 为 `dh-stage-qdr-5-close`。后续只允许先执行 `DH-STAGE-QDR-6-PLAN`；不得直接进入 Stage-QDR-6 implementation/runtime。归档目录 `docs/gates/**` 只作为 historical records。除非归档文档暴露 `FACTSOURCE_POLICY.md` 定义的硬错误，否则不得覆盖 current factsources 或授权越过当前 work order。
