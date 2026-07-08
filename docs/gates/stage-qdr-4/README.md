# Stage-QDR-4 Archive

本目录记录 Stage-QDR-4 的归档入口。归档文档只用于 review history、复盘和证据追溯，不覆盖 `docs/current/STATUS.md` 或 `docs/current/WORK_ORDER.md` 的当前结论。

## Archive Entry

```text
Stage: Stage-QDR-4 Replay / Evaluation / Regression Baseline
Status: CLOSED / ACCEPTED / ARCHIVED
Final close review: PASS
Archive close: DONE
Tag: PENDING
Close docs commit: pending until this task is committed
Next tag: dh-stage-qdr-4-close
```

## Closed Scope

```text
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
```

## Boundary

```text
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent runtime: NO
LangGraph runtime: NO
LIVE: DISABLED
NQ mutation: NO
trading execution: NO
```

下一步只允许 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 只能在 tag close 后 planning-first，不得从本归档入口直接进入 implementation、runtime、provider、HTTP、Agent、LangGraph 或 LIVE。
