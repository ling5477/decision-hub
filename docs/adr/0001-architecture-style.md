# ADR-0001: 模块化单体（Modular Monolith）

## 决策
v1 采用模块化单体：各模块独立 jar、明确依赖方向，运行时单进程部署。

## 原因
- 先保证业务闭环与交付速度
- 为未来拆分服务预留 Outbox/Event/Ledger 边界

## 影响
- 模块间不通过 gRPC；以“函数调用/领域事件”为主
- future：拆分服务时优先将 dh-providers、dh-connector、dh-scheduler 外置
