# Decision Hub Archive Index

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
stage-qdr-4 planning 已 READY。
stage-qdr-4 implementation 仍为 NOT_STARTED / NO。
```

`pre-close-current-snapshot-20260707/` 保存上一轮压缩 current docs 前的入口文件快照。`current-docs-historical-20260708/` 保存本轮从 `docs/current` 移出的历史阶段文档、旧 integration planning / review / work order、审计报告、旧 workflow support 文档和其他非 current blocker 文件。

这些归档文档均为 historical records。它们不覆盖 current `STATUS.md`、`WORK_ORDER.md`、`CODEX_PROJECT_INSTRUCTIONS.md` 或 `TESTING.md`。

## 4. Current Factsource Pointers

```text
Current status: docs/current/STATUS.md
Current next action: docs/current/WORK_ORDER.md
Factsource policy: docs/current/FACTSOURCE_POLICY.md
Validation evidence: docs/current/TESTING.md
Current docs index: docs/current/README.md
Next action: DH-STAGE-QDR-4-PLAN
```

## 5. Post-close Rule

`DH-STAGE-QDR-3-B5-CLOSE-REVIEW` 已由用户提供 ACCEPTED 结论并写回 current factsources。后续 `DH-STAGE-QDR-4-PLAN` 仍只参考 `FACTSOURCE_POLICY.md` 定义的 current factsources。归档目录 `docs/gates/**` 只作为 historical records。除非归档文档暴露 `FACTSOURCE_POLICY.md` 定义的硬错误，否则不得覆盖 current factsources 或授权 stage-qdr-4 implementation。
