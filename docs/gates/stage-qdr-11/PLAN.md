# Stage-QDR-11 Plan
## 目标
把 Stage-QDR-10 的 `DecisionFeedbackEvidenceAggregate` 接入既有 internal acceptance report，使 evidence completeness、bounded policy、tenant/environment/correlation 及 RUN identity 以单一 authority 进入只读 acceptance path。
## 不做
不新增 API、migration、schema、Repository/JDBC、contracts、POM、workflow、NQ、HTTP、Provider、Agent、LangGraph、Paper、LIVE 或 feedback learning；不执行 formal capacity。
## 验收
- 唯一 production evidence authority，无 legacy direct acceptance fallback。
- `COMPLETE_WITHIN_BOUNDS` 只进入完整 policy evaluation。
- `PARTIAL_WITHIN_BOUNDS` 与 `NOT_FOUND` 为 `INCOMPLETE`。
- `INCONSISTENT` 或 overflow 为 `INVALID`。
- bounded policy 在 aggregate 到 report 间无损传播。
- `ACCEPTED` 不产生任何外部授权。
- 全量测试、PostgreSQL/Flyway、quality 与安全审查通过。
原始计划 byte-identical 副本见 `source/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md`。
