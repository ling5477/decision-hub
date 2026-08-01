# Rollback

## Before annotated tag

- close/archive commit 如需撤回，只使用普通 `git revert <close-sha>`。
- 禁止 `reset --hard`、rebase、force push 或历史重写。
- CI 未通过、archive/hash 不一致或 tag 目标不精确时停止，不创建 tag。

## After annotated tag

- `dh-platform-hardening-feedback-side-effect-containment-close` 不得移动、覆盖、删除后重建。
- 后续发现问题只能通过新 commit、独立 authority review 与新任务修正。
- cleanup commit 可普通 revert，但 close tag 仍保持原 close archive commit target。

## Technical rollback

技术 implementation 如需回滚，只能在独立授权任务中普通 revert
`fddf3558255b5a4f8f6071da42363649942afe46`；本 governance close 不执行该操作。
