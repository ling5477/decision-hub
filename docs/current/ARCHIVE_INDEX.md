# Decision Hub Archive Index
## Terminal current authority — 2026-07-23 Stage-QDR-9 implementation work order local accepted

```text
Planning baseline commit: 30dae01488700f7c1a321fde059a783cd8435b34
Branch: dev
Expected implementation baseline: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
Origin baseline: 2aa5183a81d1733eec38ec2ab85de8d0c90c13a4
Remote exact-SHA CI: 30008506440 / PASS
Remote regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Remote PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
Remote quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-9 plan: DONE / LOCAL_COMMITTED
Stage-QDR-9 implementation work order: FROZEN / LOCAL_ACCEPTED
Stage-QDR-9 implementation: NOT_STARTED
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_PERSISTENCE + HISTORICAL_EVIDENCE_READ_MODEL
Current highest migration: V14
Selected migration: V15 / FUTURE B1 CANDIDATE / NOT CREATED
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Terminal current factsources: 12
Scope invariants: PASS / 8 OF 8
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 BLOCK HASH / 0 CONFLICTS
current task: DH-STAGE-QDR-9-IMPLEMENTATION-WORK-ORDER
current task status: DONE / LOCAL_ACCEPTED
next action: PUBLISH PLAN AND WORK-ORDER COMMITS AND RUN EXACT-SHA CI
ALLOW_PLAN_WO_PUBLICATION: YES / SEPARATE EXPLICIT AUTHORIZATION REQUIRED
ALLOW_STAGE_QDR_9_B1_IMPLEMENTATION: NO / PUBLICATION_AND_EXACT_SHA_CI_REQUIRED
ALLOW_MIGRATION_NOW / ALLOW_REPOSITORY_EXPANSION_NOW / ALLOW_API_CHANGE_NOW: NO / NO / NO
ALLOW_AUTOMATIC_LEARNING: NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

Stage-QDR-9 尚未 implementation/close/archive/tag；current plan 与 implementation work order 已形成。未来 B5 必须先复制二者及各 batch 原始 source documents、形成 self-contained archive packet、完成 archive commit，再由独立授权任务创建 annotated tag。

## Previous terminal authority — 2026-07-23 Stage-QDR-7/QDR-8 archive source recovery and pruning CI fix

```text
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-7 archive: docs/gates/stage-qdr-7/
Stage-QDR-7 tag: dh-stage-qdr-7-close
Stage-QDR-7 tag target: 6acf9c332434cafb45495d58f063a0f3faeaf475
Stage-QDR-7 source documents: RECOVERED / 30 OF 30
Stage-QDR-7 source documents path: docs/gates/stage-qdr-7/source-documents/
Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED / CURRENT_PRUNED
Stage-QDR-8 archive: docs/gates/stage-qdr-8/
Stage-QDR-8 tag: dh-stage-qdr-8-close
Stage-QDR-8 tag target: 7b6066d1062fe49d16d1f093c8b3170354854376
Stage-QDR-8 source documents: RECOVERED / 3 OF 3
Stage-QDR-8 source documents path: docs/gates/stage-qdr-8/source-documents/
Pruning commit: PUBLISHED / 0c0c60c531b6143ad50ba3c62a86d5af3d031aa8
Historical failed CI: 29930851492 / QDR7_CAPACITY_SOURCE_DOCUMENT_PRUNED
Capacity machine contract: MIGRATED_TO_STABLE_CONFIG_PATH
Capacity machine contract path: config/qdr7-capacity/qdr7-capacity-acceptance-criteria.md
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Local targeted tests: PASS / 9 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
Local ArchitectureTest: PASS / 40 OF 40
Local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Terminal current factsources: 12
Scope invariants: PASS / 7 OF 7
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 12 OF 12 / 1 BLOCK HASH / 0 CONFLICTS
current task: DH-STAGE-QDR-7-QDR8-ARCHIVE-SOURCE-RECOVERY-AND-PRUNING-CI-FIX
current task status: DONE / LOCAL_ACCEPTED
next action after local acceptance: OBTAIN FIX COMMIT PUSH AUTHORIZATION AND RUN EXACT-SHA CI
Stage-QDR-9: NOT_STARTED
ALLOW_FIX_COMMIT_PUBLICATION: YES / SEPARATE_EXPLICIT_AUTHORIZATION_REQUIRED
ALLOW_STAGE_QDR_9_PLAN_NOW: NO / FIX_COMMIT_PUBLICATION_AND_EXACT_SHA_CI_REQUIRED
ALLOW_STAGE_QDR_9_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE / ALLOW_MIGRATION / ALLOW_REPOSITORY_EXPANSION: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本任务完成 post-tag archive source completion，并把 QDR-7 capacity machine contract 迁移到稳定 `config` 路径。Stage-QDR-7/8 的 current pruning、archive 和 annotated tag 状态保持不变；B2 capacity 继续为 `DEFERRED / KNOWN_LIMITATION`，production capacity 继续为 `NOT_PROVEN`，Stage-QDR-9 继续为 `NOT_STARTED`。

## Stable machine contract and source archive governance

1. `docs/current` 只承担当前状态和入口权威，不承担长期机器合同。
2. `config`、`src/main`、`src/test`、CI、`scripts`、`deploy` 不得依赖可被 post-tag pruning 删除的 `docs/current` stage process document。
3. 长期机器可执行合同必须位于 `config/**`、`contracts/**` 或其他明确不可裁剪目录。
4. 阶段原始执行文档必须在 pruning 前完整复制到 `docs/gates/<stage>/source-documents/`。
5. Annotated tag 提供不可变恢复能力，但不能替代工作树中的 searchable archive source packet。
6. Post-tag pruning 前必须扫描 `config`、`src/main`、`src/test`、`.github`、`scripts`、`deploy` 对全部 pruning candidates 的引用。
7. 如果 pruning candidate 仍被机器依赖，必须先迁移依赖，再允许删除 current 文件。
8. Aggregate summary、final review 或 archive manifest 不能替代原始 plan、WO、retry、blocker 和 validation document。
## Stage-QDR-7 / Stage-QDR-8 tagged archive pointers（2026-07-22）

```text
Stage-QDR-7: CLOSED / ACCEPTED / ARCHIVED / TAGGED
archive path: docs/gates/stage-qdr-7/
archive commit: 6acf9c332434cafb45495d58f063a0f3faeaf475
tag: dh-stage-qdr-7-close
tag target: 6acf9c332434cafb45495d58f063a0f3faeaf475
post-tag current pruning: DONE / THIS_DOCUMENT_COMMIT

Stage-QDR-8: CLOSED / ACCEPTED / ARCHIVED / TAGGED
archive path: docs/gates/stage-qdr-8/
close commit: 7b6066d1062fe49d16d1f093c8b3170354854376
tag: dh-stage-qdr-8-close
tag target: 7b6066d1062fe49d16d1f093c8b3170354854376
post-tag current pruning: DONE / THIS_DOCUMENT_COMMIT
```

两个 archive packet 只读且由 tag 固定；本轮没有重写历史。Stage-QDR-7 B2 capacity 继续为 `DEFERRED / KNOWN_LIMITATION`，production capacity 继续为 `NOT_PROVEN`。

## Historical pre-tag Stage-QDR-8 archive state（2026-07-22）

```text
STAGE_QDR_8_FINAL_CLOSE_PREVIOUS_ATTEMPT: BLOCKED / TASK_SCOPE_DESIGN_INVALID / NO WRITES
STAGE_QDR_8_SCOPE_GOVERNANCE_FIX: CLOSED / ACCEPTED
STAGE_QDR_8: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
implementation commit: 1279f1a0a246807e019bd2223c0f7254d50b74d5
exact-SHA CI: 29836489131 / PASS
archive path: docs/gates/stage-qdr-8/
archive packet: COMPLETED
archive commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
tag: NOT_CREATED / EXPLICITLY_AUTHORIZED_AFTER_NEW_HEAD_EXACT_SHA_CI
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
next action: publish new QDR-8 close HEAD; run exact-SHA CI; create QDR-7 then QDR-8 tags
```

归档 packet 包含完整 plan、implementation work order、batch summary、validation evidence、final close review、security boundary review、status snapshot、archive close 与 manifest。上一轮 scope blocker 保留在 final close review 中；本轮把遗漏的 3 个 factsources 纳入 write allowlist，没有缩减 16 文件扫描范围。Current pruning 必须等待 tag close 后作为独立 cleanup 执行。

## Historical pre-tag Stage-QDR-7 retrospective archive state（2026-07-22）

```text
STAGE_QDR_7: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
STAGE_QDR_7_B1: FROZEN
STAGE_QDR_7_B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
STAGE_QDR_7_B3: CLOSED / ACCEPTED
B3 implementation commit: e42d430d6f8d18e32d8a9f02d2197aa68a595d63
B3 exact-SHA CI: 29750432646 / PASS
final stabilization commit: 8906389352d9d92099acdb857fce97aece3e6a20
final stabilization exact-SHA CI: 29823413542 / PASS
archive recovery: RETROSPECTIVE / GOVERNANCE_SEQUENCE_REPAIR
archive path: docs/gates/stage-qdr-7/
archive packet: COMPLETE
archive commit: 6acf9c332434cafb45495d58f063a0f3faeaf475
tag: NOT_CREATED / PENDING_EXACT_SHA_CI
next action: publish new QDR-8 close HEAD; run exact-SHA CI; create ordered tags
```

Stage-QDR-8 planning 与 implementation 已在 Stage-QDR-7 stage-level archive/tag 之前发布。本恢复只修正治理时间顺序，不重写已发布历史，不改变 B1/B2/B3 技术证据，不重开 capacity gate。QDR-7 tag 必须等待新 QDR-8 close HEAD 发布并通过 exact-SHA test + quality CI；post-tag current pruning 不在本任务执行。

## Stage-QDR-6 archive readiness（2026-07-11）

```text
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET: YES / CONSUMED
ALLOW_STAGE_QDR_6_CLOSE_DOCS_COMMIT: YES / CONSUMED / 5961164
ALLOW_STAGE_QDR_6_TAG_CLOSE_AFTER_ARCHIVE: YES
archive path: docs/gates/stage-qdr-6/
archive packet: COMPLETE
archive commit: THIS_ARCHIVE_COMMIT
tag: DONE / dh-stage-qdr-6-close
tag target: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
current process sources: PRUNED / 11_REMOVED
post-tag cleanup: DONE
next action: DH-STAGE-QDR-7-PLAN
```

Previous blocked review与retry PASS已分别保存在`docs/gates/stage-qdr-6/SOURCE_DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW.md`和`SOURCE_DH_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY.md`。完整packet位于`docs/gates/stage-qdr-6/`；annotated tag `dh-stage-qdr-6-close`指向archive commit `b9b68b3`。11个current process sources已核验后清理，下一步只允许Stage-QDR-7 planning-first。

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
