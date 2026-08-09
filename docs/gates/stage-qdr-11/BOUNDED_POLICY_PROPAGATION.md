# Bounded Policy Propagation
`BoundedEvidencePolicy` 从 `DecisionFeedbackEvidenceQuery` 进入 `DecisionFeedbackEvidenceAggregate`，再原样进入 `DecisionEvidenceReplayInternalReport`。
保留字段：
- `fromObservedAt`
- `toObservedAt`
- `maxFeedbackItems`
- `policyId`
- `policyVersion`
- `timeWindowSemantics`
- `feedbackOrder`
- `overflowBehavior = FAIL_CLOSED`
Report 同时记录 `evidenceOverflowDetected`、`feedbackEvidenceCount` 和包含 bounds identity 的 stable `evidenceAggregateRef`，因此 report 单独即可解释 evidence bounds。
