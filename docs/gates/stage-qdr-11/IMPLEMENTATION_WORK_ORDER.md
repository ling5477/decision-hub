# Stage-QDR-11 Implementation Work Order
实施只允许修改 internal report/facade、Spring wiring、相关测试与 current factsources；禁止扩大 API、migration、schema、Repository 或运行时边界。
最终实现：
- `DecisionEvidenceReplayReportService.generate` 仅消费 `DecisionFeedbackEvidenceAggregate`。
- 新增 `DecisionFeedbackInternalAcceptanceService`，一次 aggregate、一次 report evaluation、无重试和 fallback。
- `DecisionEvidenceReplayInternalReport` 携带 execution scope、completeness、bounded policy、overflow、feedback count 和 stable aggregate ref。
- Spring 中各保留一个 evidence/report/facade bean。
- tests 覆盖 completeness、bounds、RUN mismatch、tenant/environment、non-authorization、zero-side-effect 与 PostgreSQL。
原始工单 byte-identical 副本见 `source/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md`。
