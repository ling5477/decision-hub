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
stage-qdr-4 implementation: NOT_STARTED / NO
```

## 2. 当前下一步

```text
DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
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

stage-qdr-4 当前只允许进入 implementation work order。它可以把 plan 转化为文件边界、批次、review 触发规则和验证命令，但不得直接实现新功能，不得新增 API / Controller / migration，不得启动真实 HTTP、真实 provider、Provider SDK、Agent / LangGraph runtime 或 LIVE。

## 3. 后续阶段边界

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 implementation 仍需要后续单独授权，当前不得启动。Agent / LangGraph 必须后置，不进入 stage-qdr-4。

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
