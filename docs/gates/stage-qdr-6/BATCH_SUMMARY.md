# Stage-QDR-6 Batch Summary

## Mainline

```text
Decision Pipeline Evidence Consolidation / Deterministic Replay Baseline
```

## B1 Evidence correlation/contracts

- Commit：`92cc23a feat(qdr): add decision evidence correlation contracts`
- 状态：`DONE / COMMITTED`
- 结果：tenant/trace/request/decision exact correlation；`COMPLETE / INCOMPLETE / INVALID` fail-closed taxonomy；20 contract tests。

## B2 Evidence aggregation

- Commit：`78a6750 feat(qdr): add decision evidence aggregation service`
- 状态：`DONE / COMMITTED`
- 结果：只组合既有 V5/V6/V8/V9 tenant-bound ports/read models；无 repository/schema expansion；17 aggregation tests。

## B3 Canonical snapshot/persistence/deterministic replay

- `a3bf8bb`：V10 canonical snapshot schema。
- `35eb32c`：tenant-bound persistence。
- `990c1bb`：persistence boundary fix 与 V11 metadata-only migration。
- `e54e607`：`REPEATABLE_READ` assembly/hash/immutable persistence。
- `3b0d529`：deterministic replay gate。
- `ae4c944`：`QDR6-MOCK-REPLAY-1` deterministic replay baseline。
- 状态：`CLOSED / ACCEPTED`。
- 边界：persisted snapshot exact read + local mock transform；无 external IO、provider、NQ、Agent、交易或 write-back。

## B4 Internal evidence/replay report

- Commit：`964b493 feat(qdr): add evidence replay internal report`
- 状态：`DONE / COMMITTED`
- 结果：内部 evidence/replay/regression/readiness/observability consolidation；16 tests；所有外部 authorization helper 固定为 `false`。

## Close sequence

```text
49fdf54 first final close: BLOCKED / CURRENT_FACTSOURCE_CONFLICT
e09d5b4 factsource blocker fix: DONE
5961164 final close retry: PASS / CLOSE DOCS COMMITTED
archive commit: THIS_ARCHIVE_COMMIT
tag: dh-stage-qdr-6-close / NOT_CREATED
```
