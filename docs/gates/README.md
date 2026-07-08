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
```

`docs/gates/stage-qdr-4/` 记录 Stage-QDR-4 Replay / Evaluation / Regression Baseline 的 `CLOSED / ACCEPTED / ARCHIVED` 归档入口，tag 仍为 `PENDING`，下一步 tag 名为 `dh-stage-qdr-4-close`。

已归档文档只用于 review history、复盘和证据追溯，不覆盖 `docs/current/STATUS.md`、`docs/current/WORK_ORDER.md` 或 `docs/current/FACTSOURCE_POLICY.md` 的当前结论。`docs/archive/**` 不作为 QDR 当前归档标准。
