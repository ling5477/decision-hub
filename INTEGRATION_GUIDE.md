# v1 Drop-in（按实际 package: com.guidinglight.decisionhub.usecase 对齐）集成指南

你当前 dh-usecase 的根包为：
- com.guidinglight.decisionhub.usecase

本压缩包已把 v1 的契约/引擎骨架放到：
- dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/contract/*
- dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/run/DecisionEngineV1.java
- dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/run/support/OutputResolver.java

## 1) 与现有目录的“自然贴合点”

- usecase/gate：你可以把 Gate 的实现（按 constraint+consistency passed）放到这里，
  或直接在你现有 gate 组件里调用 DecisionEngineV1.gateCandidates()

- usecase/run：这里通常就是编排执行链路的位置，DecisionEngineV1 已放到该包下，便于你接线

- usecase/idempotency：你已有 IdempotencyStore，可用于 attempt/idempotency_key；v1 文档里已描述幂等策略

- usecase/usage：你已有 UsageMeter，可把 cost/token/latency 写回 DecisionRecord.usage / 冗余列

## 2) 配置文件读取

- dh-config/src/main/resources/decision-hub/evaluators/evaluators.v1.yaml
- dh-config/src/main/resources/decision-hub/decision-strategy/strategy.v1.yaml

建议在 dh-usecase 的启动/装配处加载为 Map，然后在策略执行时写入 final_decision.strategy.params。

## 3) 落库接线（dh-infra）

SQL：db/db_schema_v1.sql
Store 骨架：dh-infra/.../MysqlDecisionRecordStore.java

你用现有 Mapper 体系实现 DecisionRecordStore 即可。
