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
- 使用 Spotless（Google Java Format，2 空格缩进）与 Checkstyle（配置见 `config/checkstyle/checkstyle.xml`）。
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
- **唯一权威**：执行与推进规则以 `docs/codex/WORK_ORDER.md` 为准（本文件不重复定义细节）。
- **启动必读**：按 `WORK_ORDER.md` 指定顺序读取计划/指针/状态文件，然后再决定下一步。
- **落盘优先**：涉及实现/修改时，计划与进度必须落盘到 `docs/codex/plans/<activePlanId>/`（PLAN.md + STATUS.json）。
- **验证门禁**：`taskType=delivery` 必须执行 `scripts/verify.ps1` 并回填 `STATUS.json:lastVerify`；变更摘要写入 `docs/codex/CHANGE_NOTES.md`。
- **MCP 使用**：优先使用 IntelliJ MCP 做语义重构；如 MCP 不可用必须降级为最小改动 + 可验证变更（禁止盲目全局替换）。

## 日志规范

- 📘 [日志规范（Final）](docs/logging-spec-final.md)
- 禁止 `System.out / System.err`
- 必须使用 `SLF4J Logger`
- 日志需携带关键上下文（traceId / runId / caseId 等）

## 注释与命名规范（本项目强制，强化版）
- 注释要求：所有 public/protected 的类、接口、枚举、字段、方法必须有清晰注释；关键 private 方法若承载业务规则也必须注释。
- 命名要求：变量/方法/类/枚举命名必须贴近业务原意、可读、可搜索，不允许随意简写或无语义命名。
- 重命名/迁移/批量改引用：优先使用 IntelliJ IDEA MCP 做语义重构；若 MCP 不可用，必须采用保守降级（小步修改 + 搜索/编译/测试验证），禁止盲目全局替换。
