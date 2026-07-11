# Stage-QDR-6 Archive Manifest

## Required packet

| File | Role | State |
| --- | --- | --- |
| `README.md` | archive identity、stage result、边界与 post-archive rules | PRESENT |
| `MANIFEST.md` | packet 完整性 | PRESENT |
| `SOURCE_INDEX.md` | source/archive 映射 | PRESENT |
| `SHA256SUMS.txt` | archive content checksums | GENERATED_AND_VERIFIED |
| `PLAN.md` | frozen plan | PRESENT |
| `IMPLEMENTATION_WORK_ORDER.md` | frozen implementation WO | PRESENT |
| `BATCH_SUMMARY.md` | B1-B4 与 commit 摘要 | PRESENT |
| `VALIDATION_EVIDENCE.md` | tests/PostgreSQL/quality evidence | PRESENT |
| `FINAL_CLOSE_REVIEW.md` | retry PASS review | PRESENT |
| `ARCHIVE_CLOSE.md` | archive close 与 tag rule | PRESENT |
| `STATUS_SNAPSHOT.md` | archive-time state | PRESENT |
| `SECURITY_BOUNDARY_REVIEW.md` | no-external-authorization boundary | PRESENT |
| `DISCIPLINE_REPAIR.md` | previous BLOCKED + blocker fix history | PRESENT |

## Source packet

Stage-QDR-6 的 11 个 current 过程文档均以 `SOURCE_` 前缀复制；原文件本轮不删除。完整映射见 `SOURCE_INDEX.md`。

```text
archive completeness: COMPLETE
archive path: docs/gates/stage-qdr-6/
tag name: dh-stage-qdr-6-close
tag state: NOT_CREATED
expected tag target: THIS_ARCHIVE_COMMIT
Stage-QDR-7: NOT_STARTED / NOT_ALLOWED_YET
```
