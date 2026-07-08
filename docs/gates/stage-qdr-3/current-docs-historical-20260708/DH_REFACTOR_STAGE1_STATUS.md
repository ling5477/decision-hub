# DH Refactor Stage1 Status

> Updated: 2026-05-25
> Branch: dev
> Driver: ling5477

## Top-line

- Stage1（Boundary Freeze + Agent Runtime Skeleton）核心范围已落地，闭环测试通过。
- 所有 NQ 边界硬约束在代码层得到体现（NqContractVerifier 显式拒绝危险字段；DH 仅消费 NQ 回流事件）。
- 默认绑定内存仓储 / 规则评分器 / Fake NQ 客户端；可逐步在 Stage2 替换。

## 完成项

- [x] 4.1 dh-domain 领域模型 + 状态枚举（ResearchRunStatus / TaskNodeStatus / CandidateStatus /
  JudgeDecisionStatus / FeedbackSource）。
- [x] 4.2 dh-usecase 用例服务（含默认实现 + 内存仓储）。
- [x] 4.3 dh-memory 经验/信息素存储（接口 + 内存实现）。
- [x] 4.4 dh-eval 评分骨架（接口 + 规则默认实现）。
- [x] 4.5 dh-connector NQ adapter（接口 + Fake 实现）。
- [x] 4.6 dh-api 8 个 REST 端点（research-runs + feedback/nq）。
- [x] 4.7 dh-infra 迁移脚本占位：`V2__dh_agent_runtime.sql`，10 张表。
- [x] 5.1 蜂群并行候选搜索框架（CandidateGenerationService）。
- [x] 5.2 蚁群经验分数 + 信息素读写（ExperienceFeedbackService + PheromoneStore）。
- [x] 5.3 狼群任务编排 + Judge 仲裁骨架。
- [x] 8.1 编译验收：`mvn test -Dtest='!PostgresContainerSmokeTest'` BUILD SUCCESS。
- [x] 8.2 API 验收闭环（单测 `ResearchRunStage1ClosedLoopTest` 覆盖：
  create → start → 候选 → 仲裁 → NQ feedback → 经验更新）。
- [x] 8.3 边界验收：仓库内未出现 `下单 / 绕过风控 / 复制 NQ 回测核心 / 复制订单状态机 / 自动实盘发布` 等实现。
- [x] 8.4 审计验收：每个 ResearchRun 都可追踪到任务图 / 角色 / 节点 / 候选 / 评分 / 仲裁 / NQ 事件 /
  经验更新（通过 `ResearchRunQueryService` + 内存仓储链路）。

## 未完成 / 后续

- [ ] dh-infra：将 In-Memory 仓储替换为 Postgres / Flyway 持久化实现（V2 脚本已落位）。
- [ ] 接通真实 LLM provider：当前 Stage1 用规则评分 + Fake mapper，符合工单“fake provider / rule scorer”要求。
- [ ] 安全：将默认 tenant `t-default` 替换为 dh-security 注入。
- [ ] 集成测试：在具备 Docker 的 CI 环境跑 `PostgresContainerSmokeTest`（与本次改动无关，预存在）。

## 验证命令

```bash
mvn -pl dh-domain,dh-memory,dh-eval,dh-connector,dh-usecase -am clean test
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

两条命令均为 BUILD SUCCESS。

## 风险

- 当前所有持久化默认为内存：进程重启会丢失 ResearchRun / Candidate / JudgeDecision / Experience。
  Stage2 必须先接通 dh-infra Repository 实现，再开放任何对外灰度。
- Fake NQ adapter 仅做契约校验与内存记录，未与真实 NQ 端联调；DH→NQ 控制面与 NQ→DH 事实面的真实联调
  留给 Stage2。
- TimeProvider 当前以静态方法暴露，测试中无法注入；后续若需要时间冻结测试，建议在 dh-common 中提供
  非静态封装。
