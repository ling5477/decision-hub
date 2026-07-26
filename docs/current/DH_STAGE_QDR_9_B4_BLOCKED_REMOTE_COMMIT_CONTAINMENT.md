# DH Stage-QDR-9 B4 Blocked Remote Commit Containment

## 决策

```text
task: DH-STAGE-QDR-9-B4-BLOCKED-REMOTE-COMMIT-CONTAINMENT-DECISION
selected option: OPTION 1 / ORDINARY REVERT + AUTHORITY RECONCILIATION
history strategy: NO HISTORY REWRITE
local commit: AUTHORIZED
push: AUTHORIZED
tag: NOT AUTHORIZED
V16 implementation: NOT PART OF THIS TASK
```

远端 `dev` 曾包含 B4 scope-prewrite `1036171ff7acd85078416b907f0444ab6214b3ca` 与 retention implementation `fa9debb4474eedc4353e1236e227eae8ef23c085`。后续 review 确认两个 P1：旧 reference snapshot 无法证明当前 target inactivity，且 AUDIT / REPLAY / EVALUATION 没有可信的 environment-bound lifecycle source-of-truth。因此采用普通 revert，而非 reset、rebase、amend、force push 或删除历史对象。

## 执行链与保留项

```text
pre-containment origin/dev: fa9debb4474eedc4353e1236e227eae8ef23c085
preserved state-model design: 4ef3991f8379b6653f06a22bcdf5f4b33fcb7585
implementation revert: 4bdcd01f597e822c0e2592bfc368de6eaac56015
scope-prewrite revert: 6fadeb6c6324f6c487175e31b5c80531e30ff9e0
```

保留 Git 历史、首次 B4 milestone review `BLOCKED` 记录、两个 P1、未授权远端发布事实，以及 Option B `qdr_reference_liveness` registry 的冻结设计和 V16 candidate work order。B4 retention 的生产代码、测试、Spring wiring 与 config 均从当前技术树移除；V1–V15、B2 persistence 与 B3 historical evidence read model 保持不变。

## 本地验证

| 检查 | 结果 |
| --- | --- |
| `42697ed..HEAD` 对 `dh-domain dh-usecase dh-infra dh-app` 的净差异 | 0 |
| B4 retention classes、tests、wiring | 不存在 |
| V16 migration | 不存在 |
| `mvn -B -ntp -pl dh-usecase -am test` | PASS / 9 reactor / 596 tests / 0 failures / 0 errors / 0 skipped |
| `mvn -B -ntp -pl dh-infra,dh-app -am test` | PASS / 15 reactor / PostgreSQL/Flyway Testcontainers 真实执行 |
| `mvn -B -ntp test` | PASS / 19 reactor / 1228 tests / 0 failures / 0 errors / 0 skipped / Testcontainers 真实执行 |
| `mvn -B -ntp -Pquality validate` | PASS / 19 reactor / Checkstyle 0 / Spotless PASS |

测试汇总仅统计本轮 `mvn test` 生成的 190 份 Surefire XML；历史 `target/ci-diagnostics` 与已删除 B4 测试留下的旧 XML 被明确排除，避免污染 fresh count。

## 当前边界与下一步

```text
P0 active exposure: 0
P1: 2 / OPEN
B4: IMPLEMENTATION REVERTED / MILESTONE REVIEW BLOCKED
retention runtime: NOT AVAILABLE / NO SCHEDULER / NO API / NO STARTUP INVOCATION
state-model design: FROZEN / PUBLISHED WITH CONTAINMENT
V16: CANDIDATE / NOT CREATED
B4 milestone review retry / B4 retention publication / B5: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
API / scheduler / automatic learning: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
real HTTP / provider / NQ / Agent / LangGraph / Paper / LIVE: NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED / NOT_ALLOWED
next action after containment publication and exact-SHA CI: DH-STAGE-QDR-9-B4-REFERENCE-LIVENESS-FORWARD-MIGRATION-IMPLEMENTATION
```

发布前仍需确认 `origin/dev` 没有变化，fast-forward 推送 containment 提交链，并验证最终 authority SHA 的 remote CI。CI 对该 SHA 通过前，不得创建 V16 或重试 B4 milestone review。
