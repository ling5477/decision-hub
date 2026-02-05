# WORK_ORDER（唯一权威）

## v1 必做范围
- 单体模块化（不引入 Spring Cloud）
- Run（执行实例）+ Gate（闸门/裁决）+ Provider（模型/工具抽象）+ Ledger（事件账本）+ Memory/RAG（接口预留与基础实现）
- 全链路可回归：mvn verify = 单测 + 规范检查 + golden 回归（不得漏跑）

## v1 可选但建议列为必做（高收益）
- IntelliJ MCP 语义重构接入（提升大改动正确率）
- 计划/进度落盘（plans/<planId> + STATUS.json）确保续跑不丢上下文
- 变更记录（CHANGE_NOTES）+ ADR（关键决策记录）机制固化

## v1 明确不做（只预留）
- 未来推演业务逻辑（只做接口与数据结构预留）
- 前端 UI
- 全网实时采集引擎（只做 data-source 抽象与接口预留）
