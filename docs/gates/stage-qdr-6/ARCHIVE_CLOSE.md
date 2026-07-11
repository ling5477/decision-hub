# Stage-QDR-6 Archive Close

```text
task: DH-STAGE-QDR-6-ARCHIVE-TAG-CLOSE
archive date: 2026-07-11
archive path: docs/gates/stage-qdr-6/
archive packet: COMPLETE
archive commit: THIS_ARCHIVE_COMMIT
stage status: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
tag name: dh-stage-qdr-6-close
tag type: annotated
tag target: THIS_ARCHIVE_COMMIT
tag state: NOT_CREATED
Stage-QDR-7: NOT_STARTED / NOT_ALLOWED_YET
```

Archive packet 必须先通过 completeness、SHA-256、current facts、quality、forbidden-scope、V1-V11 与 clean-worktree 检查并提交。只有 archive commit 存在且 worktree clean 后，才允许创建 annotated tag。Tag push 与远程 target 验证成功前，不得写 `TAGGED`。

本 archive close 不删除 `docs/current` Stage-QDR-6 过程文档；post-tag current cleanup 是独立下一任务。
