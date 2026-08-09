# Bounded Evidence Semantics

## Policy

- Window：maximum `90D`。
- Page：single page only。
- Maximum items：`100`。
- Order：稳定的 `observedAt DESC, attributionId DESC`。
- Policy metadata：from、to、max、policy id、version、order、overflow 均随 aggregate 携带。

## Completeness

| State | Meaning |
| --- | --- |
| `COMPLETE_WITHIN_BOUNDS` | mandatory decision evidence 完整，feedback 至少一条，scope/identity 一致，且 policy bounds 内无 overflow |
| `PARTIAL_WITHIN_BOUNDS` | mandatory decision evidence 完整，bounded feedback 为空 |
| `INCONSISTENT` | source failure、scope/correlation conflict、ambiguity、unsafe material、incomplete aggregate 或 overflow |
| `NOT_FOUND` | mandatory decision root 不存在 |

100 条且无 next page 可以为 complete-within-bounds；`hasNext=true` 或探测到第 101 条必须 fail closed。`INCONSISTENT` 与 `NOT_FOUND` 的 feedback list 固定为空，禁止泄露已拒绝 evidence。
