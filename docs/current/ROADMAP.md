# Decision Hub Roadmap

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 1. 当前路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。当前主线为 Quant Decision Review。

```text
stage-qdr-1: CLOSED / ACCEPTED
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B1_ONLY / DONE
stage-qdr-4 B2: NOT_STARTED / PLAN_ONLY_NEXT
```

## 2. 当前下一步

```text
DH-STAGE-QDR-4-B2-REPLAY-EVALUATION-PERSISTENCE-BASELINE-PLAN
```

stage-qdr-4 plan 已选择唯一主线：

```text
stage-qdr-4 = QDR Replay / Evaluation / Regression Baseline
```

推荐顺序：

```text
1. QDR replay / evaluation / regression baseline
2. Model gateway observability / provider readiness hardening
3. real provider dry-run readiness plan
4. Agent / LangGraph preparation
```

stage-qdr-4 B1 已完成 domain/usecase contracts。当前只允许进入 B2 persistence baseline plan；B2 可能涉及 migration、repository 或 persistence 边界，因此必须先 review/freeze，不得直接 implementation，不得跳到 B3/B4，不得新增 API / Controller，不得启动真实 HTTP、真实 provider、Provider SDK、Agent / LangGraph runtime 或 LIVE。

## 3. 后续阶段边界

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 B1 已 `DONE / DOMAIN_CONTRACTS_ONLY`。B2 仅允许先做 persistence baseline plan；implementation 仍需后续单独授权。Agent / LangGraph 必须后置，不进入 stage-qdr-4。

## 4. 持续禁止项

```text
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
NQ mutation: NO
NQ DB read/write: NO
trading execution: NO
```

历史路线、旧 work order、旧 review / freeze 记录、blocker fix 过程和 pre-close snapshots 已归入 `docs/gates/**` 或由 `ARCHIVE_INDEX.md` 索引。它们保留复盘价值，但不能作为当前 next action 或 B5 blocker，除非触发 `FACTSOURCE_POLICY.md` 定义的硬错误。`docs/archive/**` 不再作为 QDR 当前归档标准。
