# Repository Guidelines

## 项目结构与模块组织
- 根目录为多模块 Maven 工程。`dh-app/` 为 Spring Boot 入口模块，其他 `dh-*` 模块按领域/分层拆分。
- 契约与协议：`contracts/`（OpenAPI、JSON Schema、事件协议）。
- 文档与用例：`docs/`（ADR/规范），`golden_cases/`（回归用例骨架）。
- 运维与本地依赖：`ops/`（`docker-compose.yml`）。
- 代码与资源：`dh-*/src/main/java`、`dh-app/src/main/resources`；数据库迁移在 `dh-app/src/main/resources/db/migration/`（例：`V1__init.sql`）。

## 构建、测试与本地运行
- `mvn -DskipTests clean package`：全量构建并跳过测试。
- `mvn -pl dh-app -am spring-boot:run`：启动应用并自动构建依赖模块。
- `mvn test` 或 `mvn -pl dh-app -am test`：运行单元/集成测试。
- `mvn -Pquality validate`：运行 Checkstyle + Spotless 规范检查。
- `docker compose -f ops/docker-compose.yml up -d`：启动本地依赖（以 compose 配置为准）。

## 编码风格与命名约定
- Java 21；Spring Boot 3.5.x。
- 使用 Spotless（Google Java Format，2 空格缩进）与 Checkstyle（配置见 `dh-bom/checkstyle/checkstyle.xml`）。
- 包名以 `com.guidinglight.decisionhub` 为前缀；类名 `PascalCase`。
- 测试类以 `*Test` 结尾；Flyway 迁移命名 `V{版本}__{描述}.sql`。

## 测试指南
- 测试框架：JUnit 5 + Spring Boot Test；集成测试使用 Testcontainers（需要 Docker）。
- 架构约束：ArchUnit；测试放在 `src/test/java`。
- 覆盖率未设定硬指标；新增功能需至少包含单元测试或集成测试。

## 提交与 PR 指南
- Git 历史仅包含 `init dev`/`Initial commit`，尚无固定规范；请使用简洁命令式摘要（例：`Add ledger repository`）。
- PR 需包含：变更说明、关联 issue（如有）、测试命令与结果。涉及接口需同步更新 `contracts/`，涉及数据库需提供迁移脚本与回滚说明。

## 安全与配置
- 禁止提交密钥；配置集中在 `dh-app/src/main/resources/application*.yml`，敏感值通过环境变量或安全配置注入。
- 所有外部输入需校验，避免 SQL/路径/命令注入。

## Codex 执行协议（强制）
- 每次开始任务前必须依序读取：
  1) docs/codex/WORK_ORDER.md（里程碑与范围）
  2) docs/codex/PLAN_QUEUE.json（未完成计划列表）
  3) docs/codex/PLAN_CURRENT_POINTER.json（当前计划指针）
  4) 当前计划目录：docs/codex/plans/<planId>/STATUS.json 与 PLAN.md
- 任何编码任务必须先输出计划，并将计划落盘到当前计划目录的 PLAN.md（禁止只在对话里输出）。
- 只允许从 STATUS.json 的“第一个未完成步骤”继续执行，禁止跳步。
- 每完成一步必须更新 STATUS.json（状态、evidence 文件列表、lastVerify）。
- 每次任务结束必须执行 scripts/verify.ps1，并把结果写入：
  - docs/codex/plans/<planId>/STATUS.json 的 lastVerify
  - docs/codex/CHANGE_NOTES.md

## 计划归档与进度追踪（强制）
- 计划正文永久保留：docs/codex/plans/<planId>/PLAN.md（不得覆盖历史计划）
- 进度状态文件：docs/codex/plans/<planId>/STATUS.json（允许更新，用于记录 DONE/TODO/IN_PROGRESS）
- 当前计划指针：docs/codex/PLAN_CURRENT_POINTER.json（允许覆盖，仅作为最新计划入口）
- 计划队列：docs/codex/PLAN_QUEUE.json
  - active：未完成计划列表（启动只读取 active）
  - done：已完成计划列表（用于复盘与审计）

## 注释与命名规范（本项目强制，强化版）
- 注释要求：所有 public/protected 的类、接口、枚举、字段、方法必须有清晰注释；关键 private 方法若承载业务规则也必须注释。
- 命名要求：变量/方法/类/枚举命名必须贴近业务原意、可读、可搜索，不允许随意简写或无语义命名。
- 重命名/迁移/批量改引用：必须优先使用 IntelliJ IDEA MCP 的语义重构能力，禁止纯文本全局替换导致漏改引用。
