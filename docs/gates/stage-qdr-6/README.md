# Decision Hub Stage-QDR-6 Archive

## 1. Archive identity

```text
repository: decision-hub
branch: dev
archive date: 2026-07-11
stage: Stage-QDR-6
mainline: Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
status: CLOSED / ACCEPTED / ARCHIVED / TAG_PENDING
tag name: dh-stage-qdr-6-close
tag type: annotated
expected tag target: THIS_ARCHIVE_COMMIT
tag state: NOT_CREATED
Stage-QDR-7: NOT_STARTED / NOT_ALLOWED_YET
```

本目录是 Stage-QDR-6 的 self-contained archive packet。它保存计划、work order、B1-B4 摘要、V10/V11、验证证据、首次 final close `BLOCKED` 历史、factsource repair、retry `PASS`、安全边界、archive close 与 tag 前状态。`docs/current` 是滚动事实源，本目录是完成阶段的不可变历史证据。

## 2. Stage result

```text
B1 evidence correlation/contracts: DONE / COMMITTED / 92cc23a
B2 evidence aggregation: DONE / COMMITTED / 78a6750
B3 canonical snapshot/persistence/deterministic replay: CLOSED / ACCEPTED
B4 internal evidence/replay report: DONE / COMMITTED / 964b493
V10 canonical snapshot schema: DONE / a3bf8bb
V11 persistence metadata fix: DONE / 990c1bb
final close first review: BLOCKED / CURRENT_FACTSOURCE_CONFLICT / HISTORICAL
factsource blocker fix: DONE / e09d5b4
final close retry: PASS / 5961164
archive packet: COMPLETE
tag: NOT_CREATED / PENDING_ARCHIVE_COMMIT
```

## 3. Key commits

| Commit | Result |
| --- | --- |
| `5108f24` | Stage-QDR-6 plan |
| `99cc5ea` | implementation work order |
| `92cc23a` | B1 evidence contracts |
| `78a6750` | B2 evidence aggregation |
| `a3bf8bb` | V10 canonical snapshot schema |
| `35eb32c` | tenant-bound persistence |
| `990c1bb` | persistence boundary fix / V11 |
| `e54e607` | canonical snapshot assembly/hash/persistence |
| `3b0d529` | deterministic replay gate |
| `ae4c944` | deterministic replay baseline |
| `964b493` | internal report |
| `49fdf54` | first final close blocker record |
| `e09d5b4` | current factsource alignment |
| `5961164` | final close retry acceptance |

## 4. Validation snapshot

Final close retry 的真实证据：

```text
mvn -ntp -pl dh-usecase,dh-infra -am test: PASS
mvn -ntp -pl dh-app -am test: PASS
mvn -ntp test: PASS / 1017 tests / 0 failures / 0 errors / 0 skipped
mvn -ntp -Pquality validate: PASS / reactor 19 of 19
Checkstyle: PASS / 0 violations
Spotless: PASS
ArchitectureTest: PASS / 39 tests / 0 skipped
PostgreSQL/Testcontainers: PASS / postgres:17 / PostgreSQL 17.10
Flyway: PASS / V1-V11 validated and applied
V10CanonicalReplaySnapshotFlywayPostgresTest: PASS / 16 tests / 0 skipped
```

Archive/tag close 本轮只要求重新运行 quality；完整 tests 与 PostgreSQL/Testcontainers 不冒充本轮重跑。

## 5. Security boundary

本阶段及其 archive/tag close 均不授权或实现：

```text
real HTTP
real Provider / Provider SDK
NQ runtime integration
Agent / LangGraph / Python runtime
external API
order/trading/execution
Paper / LIVE
raw prompt storage
raw provider response storage
credential storage
```

`REPRODUCIBLE` 只表示 persisted structured snapshot 的本地 mock replay 可复现。`InternalAcceptanceStatus.ACCEPTED` 只表示内部 evidence/replay acceptance。二者均不产生 Provider、NQ、交易、执行、Paper 或 LIVE 授权。

## 6. Archive map

- `MANIFEST.md`：packet 完整性与文件角色。
- `SOURCE_INDEX.md`：current source 到 archive source 的映射。
- `SHA256SUMS.txt`：除自身外 archive 文件的 SHA-256。
- `PLAN.md`：Stage-QDR-6 plan 冻结副本。
- `IMPLEMENTATION_WORK_ORDER.md`：实施工单冻结副本。
- `BATCH_SUMMARY.md`：B1-B4 与关键 commit 摘要。
- `VALIDATION_EVIDENCE.md`：测试、PostgreSQL、quality 与边界证据。
- `FINAL_CLOSE_REVIEW.md`：retry PASS final close review 冻结副本。
- `ARCHIVE_CLOSE.md`：archive close 与 tag 前规则。
- `STATUS_SNAPSHOT.md`：archive commit 时的状态快照。
- `SECURITY_BOUNDARY_REVIEW.md`：外部依赖和授权边界。
- `DISCIPLINE_REPAIR.md`：previous BLOCKED 与 factsource repair 历史。
- `SOURCE_*.md`：Stage-QDR-6 current 过程源文档逐份副本。

## 7. Post-archive rules

1. 必须提交本 archive packet 后才能创建 tag。
2. Tag 必须是 annotated `dh-stage-qdr-6-close` 并指向 archive commit。
3. Tag 未成功 push 并经 `git ls-remote` 验证前，不得写 `TAGGED`。
4. 本任务不删除 `docs/current` Stage-QDR-6 过程文档。
5. Tag close 后下一步只能是 `DH-STAGE-QDR-6-POST-TAG-CURRENT-CLEANUP`。
6. Post-tag cleanup 完成前，Stage-QDR-7 planning 继续 `NOT_ALLOWED_YET`。
