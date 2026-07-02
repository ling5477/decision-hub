# Decision Hub Workflow

DH 采用与 NQ 一致的阶段化工作流。

## 1. 标准流程

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

## 2. 阶段定义

### PLAN

只做规划、边界、范围、验收标准，不写业务代码。

### WO

输出可执行工单，明确：

```text
目标
范围
不做事项
模块清单
API/DB/测试要求
验收标准
Codex 开工提示词
```

### IMPLEMENT

按 WO 实现代码和测试。

### VERIFY

运行验证命令，记录结果。

最低要求：

```bash
mvn test
```

如涉及前端，再增加：

```bash
npm run build
npm run test:e2e
```

### FREEZE

冻结当前阶段，复制 `docs/current` 到：

```text
docs/gates/<stage-id>/
```

并更新：

```text
README.md
AGENTS.md
docs/README.md
docs/current/STATUS.md
docs/current/WORKLOG.md
```

### NEXT PLAN

只允许进入下一个明确阶段，不允许跨阶段夹带功能。

## 3. 当前事实源规则

任何人、任何 Agent、任何 Codex 任务开始前必须读取：

```text
README.md
AGENTS.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORK_ORDER.md
```

如涉及 DH/NQ 集成，还必须读取：

```text
docs/current/DH_NQ_INTEGRATION.md
```

## 4. 禁止事项

```text
禁止跳过 PLAN 直接写大功能
禁止跳过 VERIFY 标记完成
禁止修改冻结快照冒充历史事实
禁止在 DH 中实现交易执行
禁止绕过 NQ 风控
禁止把 DH 直接迁进 NQ
禁止建设第二套完整业务前端
```

## 5. 验收记录

每次实现结束必须更新：

```text
docs/current/STATUS.md
docs/current/TESTING.md
docs/current/WORKLOG.md
```
