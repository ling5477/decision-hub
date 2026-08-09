# Overview

Decision Hub 是一个 Java 21 / Spring Boot 多模块决策系统，用于组合受控证据、生成只读决策建议、执行回放与评估并保留审计记录。其高价值资产包括租户隔离的数据、decision/replay/audit identity、结构化反馈、策略与风险结果、持久化完整性、调用者身份以及外部 Provider/NexusQuant 边界。系统不得把内部分析或 acceptance 结果提升为交易、Provider、Paper 或 LIVE 授权。

# Threat Model, Trust Boundaries, and Assumptions

- 外部请求与 `dh-api`/应用入口之间是第一信任边界；所有身份、租户、环境、分页、批量和状态输入均视为不可信。
- use-case/domain 与 persistence (`dh-infra`) 之间是数据完整性边界；SQL 必须参数化，事务、幂等、状态机和审计关系必须 fail closed。
- tenant、environment、trace、request、decision、run 和 replay namespace 共同构成隔离边界。缺失、歧义或不一致的 provenance 不得推断或降级处理。
- Provider、HTTP、NexusQuant、消息、调度和任何交易执行面是独立的高权限边界。只读结构化结果不能创建外部调用、资金操作或运行权限。
- feedback ingestion 与 learning stores 之间是副作用边界；入站反馈默认只允许受控持久化，不得隐式触发 Experience/Pheromone/FailureCase 学习或 promotion。
- operator-controlled 配置包括超时、限流、批量、保留和环境选择；developer-controlled 输入包括 migration、workflow 与依赖；这些输入不应被普通请求影响。
- 假设生产凭证不在仓库中，认证/授权由受信入口提供，PostgreSQL 与调用方身份绑定可信。破坏这些假设会提高相应问题的严重度。

# Attack Surface, Mitigations, and Attacker Stories

- API/序列化面：攻击者可能提交越权 tenant/environment、超大分页/批量、恶意 JSON 或非法状态。关键控制是后端校验、稳定 DTO、限界和 fail-closed 错误转换。
- 决策、回放与审计面：攻击者可能重放请求、混淆 correlation、跨环境复用 identity 或让不完整证据看似完整。关键控制是显式 correlation、持久 provenance、canonical encoding、幂等与状态机保护。
- 数据面：注入、N+1/无界扫描、并发 torn read/write、迁移不兼容和事务外副作用会破坏一致性。关键控制是参数化 SQL、单语句快照、真实 PostgreSQL/Testcontainers、Flyway 顺序与小事务边界。
- 外部依赖面：SSRF、无超时重试、原始错误泄漏或在事务中等待外部调用会扩大影响。关键控制是禁止/隔离真实 Provider/NQ，显式超时、有界重试、脱敏日志和保守默认值。
- Spring wiring 面：重复 bean、兼容 fallback 或旁路 facade 可能形成第二授权路径。关键控制是 architecture tests、唯一 bean、模块单向依赖和禁止 Controller/scheduler 自动触发。
- 反馈与 learning 面：恶意或重复反馈可能污染 learning state。关键控制是 validation-first、严格单对象 JSON、精确 correlation、原子写入和 inbound learning containment。
- Secret/logging 面：token、cookie、签名、私钥、完整个人数据或 provider 原始响应不得进入日志、报告、Git 或 deterministic refs。
- 测试、文档与本地工具通常不是直接生产攻击面，但 migration、workflow、fixture 或 governance 事实若错误地授权生产行为，仍可能造成供应链或发布风险。

# Severity Calibration (Critical, High, Medium, Low)

- Critical：可绕过风控直接发起真实交易/支付、跨租户大规模读取或修改敏感数据、泄露生产密钥、远程代码执行或不可恢复地破坏账务/审计数据。
- High：可稳定跨租户/环境访问 decision 或 feedback、绕过 acceptance/authorization 边界触发 Provider/NQ、伪造或重放受信 identity、破坏关键状态机或事务一致性。
- Medium：可造成受控范围拒绝服务、无界查询/队列、敏感内部信息泄漏、错误证据 completeness、重复副作用或需要较强前置条件的数据完整性问题。
- Low：不直接越权的防御纵深缺口、低敏信息暴露、有限可维护性问题或仅开发/测试环境可达且无生产影响的问题。

Repository: target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79
Version: codex-security-snapshot/v1:sha256:3c499b40232176c7a74b40ea0b0c7a0f6ebbe89283eac6fe548aca093c30eb3f

