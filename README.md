# Decision Hub

> 多 AI 模型协作决策系统（Multi-AI Decision System / Decision Hub）  
> 面向复杂决策场景的统一编排、评估、决策与复盘平台。

---

## 一、项目简介

Decision Hub 是一个工程级的多模型协作决策系统，目标是：

- 统一编排多个 AI / 决策模型（LLM、规则引擎、策略模型等）
- 对决策过程进行评估、投票、记录、审计与复盘
- 提供可追溯（traceable）、可解释（explainable）、可复盘（replayable）的决策能力
- 支持自动化与 AI Agent（Codex）协同开发

---

## 二、仓库结构（真实目录对齐）

```
decision-hub/
├─ .codex/              # Codex / Agent 行为约束
├─ .github/             # CI / GitHub Actions
├─ .mvn/                # Maven Wrapper
├─ config/              # Checkstyle / 质量门禁配置
├─ contracts/           # API / 事件 / 协议契约
├─ db/                  # 数据库脚本 / Flyway
├─ docs/                # 工程级文档（规范 / 架构说明）
├─ golden_cases/        # Golden 用例
├─ ops/                 # 运维 / 部署
├─ patches/             # 临时补丁
├─ scripts/             # 开发脚本
├─ tools/               # 辅助工具
│
├─ dh-app/              # 应用启动入口（Spring Boot，日志实现与配置）
├─ dh-api/              # 对外 API / Facade / DTO 定义
├─ dh-bom/              # 依赖与插件版本统一管理（BOM）
├─ dh-common/           # 通用工具与基础抽象
├─ dh-config/           # 配置模型与配置加载
├─ dh-connector/        # 外部系统连接器（HTTP / MQ / Third-party）
├─ dh-domain/           # 领域模型（Domain）
├─ dh-eval/             # 评估 / Golden 回归 / 离线执行
├─ dh-infra/            # DB / Redis / ES 等基础设施实现
├─ dh-knowledge/        # 知识库 / Prompt / Rules
├─ dh-ledger/           # 决策账本 / 审计 / 决策记录
├─ dh-memory/           # 长短期记忆 / 上下文存储
├─ dh-observability/    # 日志 / 指标 / Trace 适配
├─ dh-providers/        # 模型提供方（LLM / AI Provider）
├─ dh-scheduler/        # 调度 / 任务编排
├─ dh-sdk/              # 对外 SDK
├─ dh-security/         # 认证 / 鉴权 / 安全
├─ dh-usecase/          # 用例层（Application Service）
└─ pom.xml              # 根聚合 POM（modules 列表以此为准）
```

---

## 三、工程规范（强制）

### 1）日志规范
- 📘 [日志规范（Final）](docs/logging-spec-final.md)
- 禁止 `System.out / System.err`
- 必须使用 `SLF4J Logger`
- 日志需携带关键上下文（traceId / runId / caseId 等）

### 2）代码风格
- 使用 Checkstyle 作为强制门禁  
- 规则文件：[`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml)

---

## 四、常用命令

### 1）质量门禁（推荐：全仓库）

```bash
mvn -Pquality -DskipTests validate
```

### 2）质量门禁（排障：按模块分组）

**核心业务层**
```bash
mvn -Pquality -DskipTests validate -pl dh-domain,dh-usecase,dh-api -am
```

**支撑能力层**
```bash
mvn -Pquality -DskipTests validate -pl dh-common,dh-config,dh-security,dh-observability -am
```

**决策能力扩展**
```bash
mvn -Pquality -DskipTests validate -pl dh-providers,dh-knowledge,dh-memory,dh-ledger -am
```

**基础设施与调度**
```bash
mvn -Pquality -DskipTests validate -pl dh-infra,dh-connector,dh-scheduler -am
```

**评估与工具**
```bash
mvn -Pquality -DskipTests validate -pl dh-eval,dh-sdk -am
```

### 3）Golden 回归（dh-eval）

```bash
mvn -DskipTests verify -pl dh-eval -am
```

---

## 五、相关文档

- 📘 [日志规范（Final）](docs/logging-spec-final.md)
- 📘 [Checkstyle 规则](config/checkstyle/checkstyle.xml)

---

## 六、License

> 待补充（Apache 2.0 / MIT / 内部使用等）
