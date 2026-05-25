# Decision Hub Testing

## 1. 当前状态

DH-REFIT-1-PLAN 阶段只做文档结构重构，没有业务代码变更。

当前未执行 Maven 测试。

## 2. 标准验证命令

后续实现阶段最低验证：

```bash
mvn test
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动验证：

```bash
mvn -pl dh-app -am spring-boot:run
```

## 3. Stage1 必测闭环

DH-REFIT-1-WO 实现后必须覆盖：

```text
创建 ResearchRun
启动 ResearchRun
生成多个 StrategyCandidate
生成 JudgeDecision
接收 NQ Feedback Event
更新 ExperienceEntry / PheromoneEdge
查询 ResearchRun 详情能看到完整任务轨迹
```

## 4. 边界测试

必须检查：

```text
没有直接下单实现
没有绕过 NQ 风控实现
没有复制 NQ 订单状态机
没有复制 NQ 回测核心
没有自动实盘发布链路
```

## 5. 验收记录格式

每次 VERIFY 后更新本文件：

```text
日期
阶段
命令
结果
失败原因
修复结论
剩余风险
```
