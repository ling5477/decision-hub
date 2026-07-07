# Decision Hub Archive Index

## 1. 归档原则

归档文件保留历史信息，不删除历史，不重写历史。归档文件不是 current factsource，不得覆盖 `docs/current/STATUS.md` 或 `docs/current/WORK_ORDER.md`。

## 2. stage-qdr-2

```text
docs/archive/stage-qdr-2/DH_STAGE_QDR_2_WORK_ORDER.md
docs/archive/stage-qdr-2/DH_STAGE_QDR_2_DISCIPLINE_CLOSEOUT.md
```

归档原因：

```text
stage-qdr-2 final close 已 CLOSED / ACCEPTED。
上述文件记录已完成阶段的详细 work order 与 discipline closeout 过程。
它们仍有复盘价值，但不应作为 stage-qdr-3 B5 close review 的 current blocker。
```

## 3. stage-qdr-3

```text
docs/archive/stage-qdr-3/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md
docs/archive/stage-qdr-3/DH_STAGE_QDR_3_IMPLEMENTATION_WORK_ORDER.md
```

归档原因：

```text
stage-qdr-3 planning 与 implementation work order 已由 B1-B4 消费。
B1 已 DONE / COMMITTED。
B2/B3/B4 已 DONE / FREEZE ACCEPTED / COMMITTED。
B5 当前为 READY FOR RETRY。
上述计划/工单不再是当前 next action，也不得阻断 B5，除非出现 FACTSOURCE_POLICY.md 定义的硬错误。
```

## 4. Pre-close Current Snapshot

```text
docs/archive/stage-qdr-3/pre-close-current-snapshot-20260707/
```

归档原因：

```text
该目录保存本轮压缩 current docs 前的入口文件快照。
用途是保留旧 WORKLOG / TESTING / STATUS / WORK_ORDER 等长篇历史记录，避免 current factsources 继续承载过期阶段状态。
该目录不是 current factsource。
```

## 5. 当前状态指针

```text
Current status: docs/current/STATUS.md
Current next action: docs/current/WORK_ORDER.md
Factsource policy: docs/current/FACTSOURCE_POLICY.md
Next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```
