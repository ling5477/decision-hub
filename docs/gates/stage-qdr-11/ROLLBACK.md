# Stage-QDR-11 Rollback
## Tag 前
使用普通 `git revert <commit>` 回滚，不使用 reset、rebase 或 force push。Implementation 与 archive/authority commit 分开回滚并重新验证 exact-SHA CI。
## Tag 后
`dh-stage-qdr-11-close` 不可移动、覆盖、删除后重建。新问题必须以新 commit 和独立 review 修复。Post-tag cleanup commit 可以普通 revert，但不得修改已冻结 archive 或 tag target。
## 停止条件
任一 security binding、CI、archive hash、authority conflict、local/remote SHA 或 tag peeled target 校验失败，立即停止后续步骤。
