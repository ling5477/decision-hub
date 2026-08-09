# Single Evidence Authority
唯一 production acceptance evidence authority 为 `DecisionFeedbackEvidenceAggregate`。
证据：
- `DecisionEvidenceReplayReportService` 只有一个 public `generate`。
- 该方法首参唯一为 `DecisionFeedbackEvidenceAggregate`。
- legacy `DecisionEvidenceAggregate` direct acceptance overload 为 0。
- production legacy fallback/caller 为 0。
- `DecisionFeedbackInternalAcceptanceService` 只执行一次 consolidated aggregate 与一次 report evaluation。
- Spring wiring 各有一个 evidence/report/facade bean。
- 未引入第二套 acceptance model；继续复用 `DecisionEvidenceReplayInternalReport`。
结论：`DUAL_AUTHORITY = NONE`。
