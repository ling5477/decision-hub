# Completeness Acceptance Mapping
| Evidence completeness | Internal acceptance | 规则 |
| --- | --- | --- |
| `COMPLETE_WITHIN_BOUNDS` | 继续完整 policy evaluation | 不自动 `ACCEPTED`；仍需 evidence/replay/regression/readiness/observability 全部通过 |
| `PARTIAL_WITHIN_BOUNDS` | `INCOMPLETE` | optional feedback 缺失，fail-closed |
| `INCONSISTENT` | `INVALID` | 包括 scope/provenance/order/identity/source inconsistency |
| `NOT_FOUND` | `INCOMPLETE` | 强制 decision root 不存在，fail-closed |
| overflow | `INVALID` | bounded single-page overflow，fail-closed |
Report constructor 对 `ACCEPTED` 再次强制 `COMPLETE_WITHIN_BOUNDS`、`FAIL_CLOSED` overflow behavior、无 overflow 和所有既有 evidence gates。
