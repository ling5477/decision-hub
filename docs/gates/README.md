# Decision Hub Gates

本目录保存 Decision Hub 阶段冻结、阶段归档和 historical records。

当前事实源仍使用：

```text
docs/current
```

QDR 阶段归档使用：

```text
docs/gates/stage-qdr-2/
docs/gates/stage-qdr-3/
docs/gates/stage-qdr-4/
docs/gates/stage-qdr-5/
```

`docs/gates/stage-qdr-4/` 记录 Stage-QDR-4 Replay / Evaluation / Regression Baseline 的 `CLOSED / ACCEPTED / ARCHIVED / TAGGED` 归档入口和实际阶段文档副本，tag 为 `dh-stage-qdr-4-close`。

`docs/gates/stage-qdr-5/` 记录 Stage-QDR-5 Model Gateway Observability / Provider Readiness Hardening 的 `CLOSED / ACCEPTED / ARCHIVED / TAG PENDING` 归档入口，后续 tag 名为 `dh-stage-qdr-5-close`。

Stage archive 必须形成 self-contained archive packet；单个 `README.md` 不满足阶段归档要求。Stage-QDR-4 与 Stage-QDR-5 已补齐 `PLAN.md`、`IMPLEMENTATION_WORK_ORDER.md`、`BATCH_SUMMARY.md`、`VALIDATION_EVIDENCE.md`、`FINAL_CLOSE_REVIEW.md`、`ARCHIVE_CLOSE.md`、`STATUS_SNAPSHOT.md` 等 packet 文件；Stage-QDR-5 tag close 只能在本轮 archive policy fix commit 后作为独立任务执行。

已归档文档只用于 review history、复盘和证据追溯，不覆盖 `docs/current/STATUS.md`、`docs/current/WORK_ORDER.md` 或 `docs/current/FACTSOURCE_POLICY.md` 的当前结论。`docs/archive/**` 不作为 QDR 当前归档标准。
