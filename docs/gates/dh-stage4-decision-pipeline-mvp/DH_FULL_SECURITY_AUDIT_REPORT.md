# DH FULL SECURITY AUDIT REPORT

审查日期：2026-05-26  
审查范围：Decision Hub 当前仓库全量静态审查、基础安全扫描器可用性检查、项目验证命令。  
审查边界：本轮只审查；未修改业务代码，未新增依赖，未删除文件，未执行真实外部调用测试，未输出任何密钥明文。

## 1. 审查结论

结论：有条件通过。

当前仓库未发现 P0 级问题：未确认存在生产密钥明文泄露、已实装未知远程命令执行、隐藏遥测上报、真实 LLM 上下文外发到不可信第三方中转站、或应用运行时可直接执行任意系统命令。

但存在 3 个 P1 风险，主要集中在 API 暴露面、NQ feedback 信任边界、以及未来 provider/中转站接入前缺少强制信任分级与出口闸门。`mvn test` 当前也存在回归失败。因此 DH 可以继续做受控的内部重构与修复，但不建议继续推进 NQ 接入、真实 provider、RealClient 或对外暴露，必须先进入 `DH-AUDIT-FIX` 收敛 P1 与测试失败。

问题统计：

| 级别 | 数量 |
| --- | ---: |
| P0 | 0 |
| P1 | 3 |
| P2 | 7 |
| P3 | 4 |

## 2. 执行环境

工作目录：`E:\Project\decision-hub`  
当前分支：`dev`  
最近提交：`67cc869 Stage3-B3 DH Backtest Request Adapter IMPL completed`  
Git 基线：

- `git status --short`：审查前为空；写入报告前无业务代码变更。
- `git branch --show-current`：`dev`。
- `git log --oneline -20`：最近 20 个提交已检查，当前 HEAD 为 Stage3-B3 完成提交。
- `git diff --stat`：审查前为空。
- `git diff -- . ':!*.lock'`：审查前为空。
- `git ls-files`：共 623 个 tracked 文件。

已读取当前事实源：

- `README.md`
- `docs/current/README.md`
- `docs/current/STATUS.md`
- `docs/current/ROADMAP.md`
- `docs/current/WORKFLOW.md`
- `docs/current/WORK_ORDER.md`
- `docs/current/DH_NQ_INTEGRATION.md`
- `docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md`

已执行命令摘要：

| 命令 | 结果 |
| --- | --- |
| `git status --short` | 成功；审查前工作区干净 |
| `git branch --show-current` | 成功；`dev` |
| `git log --oneline -20` | 成功 |
| `git diff --stat` | 成功；无 diff |
| `git diff -- . ':!*.lock'` | 成功；无 diff |
| `git ls-files` | 成功；623 个文件 |
| `Get-Command gitleaks` | 未找到 |
| `Get-Command semgrep` | 未找到 |
| `Get-Command trivy` | 未找到 |
| `mvn test` | 失败；`DhBacktestRequestIdempotencyTest.afterWindow_newRequestGenerated` 断言失败 |
| `mvn -pl dh-usecase -am -Dtest=DhBacktestRequestIdempotencyTest '-Dsurefire.failIfNoSpecifiedTests=false' test` | 失败；复现同一断言失败 |
| `mvn -Pquality validate` | 失败；Spotless 插件版本缺失触发元数据解析，且本机 Maven 仓库写入被拒绝 |
| `docker compose -f ops/docker-compose.yml config` | 成功；仅解析配置，未启动容器 |
| `pytest` | 失败；未收集到测试 |
| `ruff check . --no-cache` | 失败；`.agents/skills/ui-ux-pro-max/scripts` 存在 14 个 lint 问题 |
| `mypy . --no-incremental` | 失败；根目录未发现可检查的 `.py[i]` 文件 |

## 3. P0 问题

未发现 P0。

未确认以下风险在当前代码中成立：

- 未发现生产 API Key、模型 Key、Token、私钥明文泄露。
- 未发现应用运行时调用 `ProcessBuilder`、`Runtime.getRuntime`、`ScriptEngine`、`setAccessible`、`child_process`、`subprocess`、`os.system` 等任意命令执行入口。
- 未发现真实 HTTP LLM provider、OpenAI-compatible 中转站、OpenRouter、new-api、one-api、siliconflow 等已实装出站调用。
- 未发现隐藏 telemetry、collect、report、sendBeacon、webhook 或 callback 上报链路。
- 未发现 `curl | bash`、`iwr | iex`、`Invoke-Expression` 形式的远程安装脚本。

## 4. P1 问题

### P1-1：AI Research 与 NQ Feedback API 缺少已装配的认证与租户隔离

位置：

- `dh-api/src/main/java/com/guidinglight/decisionhub/api/research/ResearchRunController.java:29`
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/research/ResearchRunController.java:32`
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackController.java:32`
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackController.java:35`
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/TokenVerifier.java`

现象：

- `ResearchRunController` 暴露 `/api/ai/research-runs` 的创建、列表、详情、启动、任务、候选、JudgeDecision 查询接口。
- `NqFeedbackController` 暴露 `/api/ai/feedback/nq` 写入 NQ feedback。
- 两者均使用硬编码默认 tenant。
- 仓库中存在 `TokenVerifier` 抽象，但未发现 `SecurityFilterChain`、请求级认证过滤器或 controller 级鉴权装配。

风险：

如果服务被非本机或非可信网络访问，攻击者可创建/启动研究任务、读取运行信息、伪造 NQ feedback 进入 DH 经验/记忆链路，造成上下文污染、反馈污染和数据泄露。

建议：

进入 `DH-AUDIT-FIX` 后补齐请求认证、租户解析、来源校验、权限分级，并在 controller 测试中覆盖未授权请求。

### P1-2：NQ Feedback 信任边界依赖客户端字段，缺少强认证或签名

位置：

- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/DefaultNqFeedbackContractValidator.java`
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackController.java:44`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepository.java:34`

现象：

- Validator 会检查 `sourceSystem`、`schemaVersion`、`traceId`、`eventType`、payload 必填字段。
- `sourceSystem` 和 `traceId` 均来自请求体；未发现 HMAC、mTLS、网关签名、单独 token verifier 或 NQ 来源白名单。
- 原始 `payload_json` 会持久化。

风险：

字段级校验能防止结构错误，但不能证明请求来自可信 NQ。若接口暴露或被横向访问，伪造 feedback 可以污染 DH 对策略、候选、回测结果的历史反馈。

建议：

为 NQ feedback 增加来源认证、签名校验、重放保护、请求体大小限制和审计记录。

### P1-3：未来 LLM Provider / 中转站启用前缺少强制信任分级与出口闸门

位置：

- `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/config/ModelConfig.java:9`
- `dh-app/src/main/resources/db/migration/V1__init.sql:60`
- `dh-config/src/main/resources/strategies/strategy.v1.yaml`
- `dh-app/src/main/java/com/guidinglight/decisionhub/config/NqBacktestClientProperties.java:34`

现象：

- 当前运行时代码未实装真实 LLM provider，`dh-providers` 仍为 deprecated mock。
- 领域模型和 DDL 已预留 `endpoint`，策略配置中存在模型名称权重。
- Stage3 NQ backtest client 当前仍是 Fake/Disabled，未发现 Real HTTP client。
- 未发现 provider trust level、官方 API / 自建网关 / 第三方中转站分类、出站域名 allowlist、prompt redaction gate、或 OpenAI-compatible 中转站拒绝策略。

风险：

当前无已实装外发风险，但一旦接入真实 provider 或 OpenAI-compatible baseURL，缺少统一的信任分级会让第三方中转站获得完整 system/developer/user prompt、工具结果、文件片段与历史记忆，形成不可接受的上下文篡改和泄露风险。

建议：

在接入真实 provider 前建立 `ProviderTrustPolicy`：官方 API、自建网关、受控 relay、禁止 relay 四类；默认拒绝未知 baseURL；记录 provider、baseURL hash、traceId；禁止将敏感上下文发往未信任中转。

## 5. P2 问题

### P2-1：配置中存在默认本地凭据

位置：

- `dh-app/src/main/resources/application.yml:8`
- `dh-app/src/main/resources/application-dev.yml:5`
- `ops/docker-compose.yml:7`

说明：

配置中存在数据库密码默认值或本地固定密码。未在报告中输出明文。当前判断为本地开发默认值，不作为 P0；但若镜像、演示环境或测试环境直接复用，会扩大横向访问风险。

### P2-2：Actuator 暴露范围与 health detail 过宽

位置：

- `dh-app/src/main/resources/application.yml:29`
- `dh-app/src/main/resources/application.yml:32`

说明：

Actuator 暴露 `health,info,metrics,prometheus`，且 health details 为 always。若缺少网关或鉴权，可能泄露服务依赖、指标和环境特征。

### P2-3：prompt、payload、原始外部响应留存缺少统一脱敏与大小限制

位置：

- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/run/RunService.java:22`
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/run/RunService.java:279`
- `dh-app/src/main/resources/db/migration/V2__dh_agent_runtime.sql:10`
- `dh-app/src/main/resources/db/migration/V3__stage2_poc_tools.sql:18`
- `dh-app/src/main/resources/db/migration/V3__stage2_poc_tools.sql:28`
- `dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/JdbcNqFeedbackEventRepository.java:34`

说明：

仓库设计强调审计复盘，保留 prompt snapshot、payload_json、raw_payload_json 是合理的，但当前未发现统一脱敏策略、字段级敏感信息过滤、payload 大小/深度限制和保留周期策略。

### P2-4：审计日志字段未见统一 redaction

位置：

- `dh-observability/src/main/java/com/guidinglight/decisionhub/observability/Slf4jAuditLogger.java`
- `dh-app/src/main/resources/logback-spring.xml`

说明：

请求日志当前不记录 header/body，这是正向设计；但 audit logger 会输出调用方传入的 `fields`。如果未来把 prompt、payload、provider response、token 标记错误地放入 fields，可能进入日志。

### P2-5：`X-Trace-Id` 未限制格式与长度

位置：

- `dh-api/src/main/java/com/guidinglight/decisionhub/api/TraceIdFilter.java:17`

说明：

TraceIdFilter 接收调用方提供的 `X-Trace-Id` 并写入响应与 MDC。未发现格式、长度、字符集校验。攻击者可构造过长或带控制字符的 traceId，造成日志污染或检索污染。

### P2-6：依赖与构建供应链稳定性不足

位置：

- `pom.xml`
- `dep-tree.txt`

说明：

`mvn -Pquality validate` 报告 `com.diffplug.spotless:spotless-maven-plugin` 缺少显式版本，并触发 Maven 元数据解析。`dep-tree.txt` 中记录过从外部 Maven 镜像和 Apache snapshots 解析依赖的历史。当前 POM 未发现显式 `<repositories>` 或 `<pluginRepositories>`，但构建结果依赖本机 Maven settings 与外部仓库策略。

### P2-7：repo-local Agent skill 指令面过宽

位置：

- `.agents/skills/shadcn/SKILL.md:5`
- `.agents/skills/shadcn/SKILL.md:158`
- `.agents/skills/critique/SKILL.md:18`
- `.agents/skills/critique/SKILL.md:66`

说明：

这些是仓库内 Agent 辅助技能，不是 DH 应用运行时代码。`shadcn` skill 允许执行 `npx shadcn@latest` 并获取外部 docs，`critique` skill 鼓励 sub-agent 与浏览器注入式检查。对 UI 任务可能有用，但在安全审查或敏感代码任务中属于较宽的工具/网络权限面。

## 6. P3 问题

### P3-1：`mvn test` 当前失败

位置：

- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/backtest/DhBacktestRequestIdempotencyTest.java`

现象：

`afterWindow_newRequestGenerated` 期望 `FAKE_ACCEPTED`，实际为 `IDEMPOTENT_SHORT_CIRCUIT`。该问题阻断最低验证门槛，虽非直接安全漏洞，但会影响继续重构与安全修复验收。

### P3-2：`NqBacktestClient` 注释与 null 入参行为不完全一致

位置：

- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/backtest/NqBacktestClient.java`

说明：

接口注释强调避免 RuntimeException 中断 caller，但默认方法对 null request 抛出 `IllegalArgumentException`。这是边界一致性问题，不构成当前安全漏洞。

### P3-3：Python Agent 辅助脚本存在 lint 问题

位置：

- `.agents/skills/ui-ux-pro-max/scripts/design_system.py`
- `.agents/skills/ui-ux-pro-max/scripts/search.py`

说明：

`ruff check . --no-cache` 报告 14 个 unused import/variable 与 f-string 问题。该目录为 Agent 辅助资产，不是 DH 后端主链路。

### P3-4：Python 验证配置不完整

说明：

仓库存在 `.agents` 下的 Python 脚本，但无 Python 项目配置；`pytest` 未收集到测试，`mypy . --no-incremental` 未发现根目录可检查文件。建议后续明确该目录是否纳入仓库质量门禁。

## 7. 中转站 / Provider 审查

发现的 provider / model / endpoint 配置面：

| 类型 | 位置 | 结论 |
| --- | --- | --- |
| Deprecated Mock Provider | `dh-providers/src/main/java/.../MockProvider.java` | 仅本地 mock，无网络调用 |
| 模型配置领域对象 | `dh-domain/src/main/java/com/guidinglight/decisionhub/domain/config/ModelConfig.java:9` | 有 `endpoint` 字段，无 key 字段 |
| 数据库模型配置 | `dh-app/src/main/resources/db/migration/V1__init.sql:60` | `dh_model_config.endpoint` 可存 endpoint |
| 策略模型权重 | `dh-config/src/main/resources/strategies/strategy.v1.yaml` | 仅模型名权重，无 baseURL/key |
| NQ Backtest 配置 | `dh-app/src/main/java/com/guidinglight/decisionhub/config/NqBacktestClientProperties.java:34` | `baseUrl` 默认为空；当前 Fake/Disabled |

未发现已实装配置：

- `openrouter`
- `siliconflow`
- `new-api`
- `one-api`
- 第三方 relay/proxy gateway baseURL
- OpenAI-compatible 真实 HTTP client
- `openai`、`anthropic`、`gemini`、`deepseek`、`qwen` 真实 SDK 或出站调用

判断：

当前未发现第三方中转站已接入 DH 运行时，因此没有已证实的中转站上下文篡改风险。但代码和 DDL 已具备未来填入 endpoint 的扩展点，必须在真实接入前建立 provider 信任分级、baseURL allowlist、敏感上下文脱敏、审计和禁用未知中转站策略。

## 8. API Key 管理审查

Key 来源审查：

| 来源 | 位置 | 判断 |
| --- | --- | --- |
| 环境变量 | `dh-app/src/main/resources/application-prod.yml:5` | prod 使用 env placeholder，未输出明文 |
| 本地默认值 | `dh-app/src/main/resources/application.yml:8`、`application-dev.yml:5`、`ops/docker-compose.yml:7` | 存在默认本地密码，P2 |
| 数据库 | `dh-app/src/main/resources/db/migration/V1__init.sql:100` | `dh_api_key.key_hash` 存 hash，不存明文，正向设计 |
| 前端 | 未发现前端应用或 `package.json` | 未发现前端暴露 key |
| 日志 | `RequestLogFilter` 不记录 header/body；`Slf4jAuditLogger` 记录 arbitrary fields | 需要统一 redaction |
| 请求参数 | 未发现 API key 作为 query 参数 | 未发现直接问题 |

敏感信息扫描结论：

- 未发现可确认为生产密钥的明文 API Key、模型 Key、Token 或私钥。
- 发现配置默认密码类风险，但按本地开发默认值处理为 P2。
- 因 `gitleaks` 未安装，本轮未执行专业 secret 扫描器；结论以关键词与红acted 静态审查为准。

## 9. 上下文拼接审查

当前上下文来源：

- 用户输入：`CreateResearchRunRequest.topic` 与 `payloadJson`。
- 历史数据：ResearchRun、Task、Candidate、JudgeDecision、ReflectionEntry、NQ feedback event。
- 旧链路：`RunService` 保留 `prompt_snapshot`、raw output、tool traces，用于回放。
- Provider：当前为 mock/rule-based，无真实 LLM 调用。

风险判断：

- 当前没有真实 LLM 外发，因此 prompt injection 不会直接外发给第三方模型。
- 但 `payloadJson`、NQ raw payload、reflection、history 进入持久化，缺少统一的“不可信上下文”标记、大小限制和 prompt instruction 隔离策略。
- 未来若接入真实 LLM，必须保证用户内容、工具结果、历史记忆、文件内容不能覆盖 system/developer 指令。

建议：

引入 ContextBuilder 分层：`trusted_system`、`trusted_policy`、`user_content`、`tool_result_untrusted`、`memory_untrusted`；所有外部文本进入模型前做显式边界包裹、长度限制和注入检测。

## 10. Agent 工具权限审查

应用运行时能力：

| 能力 | 当前结论 |
| --- | --- |
| 读文件 | 应用代码未发现通用文件读取 API 暴露 |
| 写文件 | 应用主要写数据库；未发现任意路径写文件 API |
| 执行命令 | 未发现运行时命令执行入口 |
| 访问网络 | 当前真实 provider HTTP client 未实装；DB/Redis 为本地依赖 |
| 调用数据库 | JDBC repository 正常持久化业务数据 |
| 调用外部 API | Stage3 NQ backtest 目前 Fake/Disabled，无真实 HTTP |

Agent/开发辅助能力：

- `.agents/skills` 目录包含 UI/设计/组件技能，部分 skill 指令允许浏览器自动化、sub-agent、`npx shadcn@latest` 等操作。
- 这些不是 DH 应用运行时工具，但会影响 Codex/Agent 执行任务时的权限面。

判断：

DH 应用运行时未发现任意命令执行能力；Agent 辅助层需要在安全任务中默认禁用网络型/安装型技能，或由任务白名单显式允许。

## 11. 网络出口审查

发现的网络/出口相关项：

| 类型 | 位置 | 结论 |
| --- | --- | --- |
| JDBC | `dh-app/src/main/resources/application.yml` | 默认 localhost PostgreSQL |
| Redis | `dh-app/src/main/resources/application.yml` | 默认 localhost Redis |
| Docker ports | `ops/docker-compose.yml:8`、`ops/docker-compose.yml:16` | PostgreSQL/Redis 端口发布到宿主机 |
| NQ baseUrl | `NqBacktestClientProperties.java:34` | 默认为空；当前无 Real HTTP client |
| Maven 解析 | `mvn -Pquality validate` | 因插件版本缺失触发外部元数据解析 |
| Docs placeholder URL | docs/contracts 中若干 example host | 文档占位，不是运行时出口 |

未发现：

- `fetch`、`axios` 前端调用链路。
- Java `RestTemplate`、`WebClient`、`OkHttp`、`HttpClient` 真实出站实现。
- WebSocket、EventSource、Socket、ServerSocket、webhook、callback、telemetry、collect、sendBeacon 运行时代码。

## 12. 日志与审计审查

正向发现：

- `RequestLogFilter` 记录 method、URI、status、cost、traceId，不记录 header/body。
- `logback-spring.xml` 输出 traceId，利于审计关联。

风险：

- `Slf4jAuditLogger` 对 `fields` 无统一脱敏，未来可能把 prompt、payload、工具结果或 token-like 字段写入日志。
- `X-Trace-Id` 未限制格式和长度，存在日志污染风险。
- 数据库保留 raw payload，缺少红线字段脱敏策略。

建议：

建立统一 `SensitiveValueRedactor`，覆盖日志、审计、payload 入库、模型请求前处理，并加入敏感字段黑名单与 token-like pattern。

## 13. 依赖审查

项目类型：

- Maven 多模块 Java 21 / Spring Boot 3.5.x。
- 未发现 `package.json`，无前端 npm 项目。
- 未发现 `requirements.txt`、`pyproject.toml`、Go/Rust 项目配置。
- `.agents` 下存在 Python 辅助脚本。

Maven：

- 未发现 active POM 中显式 `<repositories>` 或 `<pluginRepositories>`。
- `mvn -Pquality validate` 暴露 Spotless plugin 版本缺失问题。
- `dep-tree.txt` 记录过外部 Maven 镜像和 snapshots 解析历史，应确认是否来自本机 Maven settings。

npm：

- 未发现 `package.json`，未执行 `npm run build`、`npm test`、`npm run test:e2e`。

Python：

- 未发现项目级依赖文件。
- `ruff` 对 `.agents` 脚本报 14 个 lint 问题。

Docker：

- `ops/docker-compose.yml` 使用 `postgres:17`、`redis:7` 标签，未 pin digest。
- 本地服务发布端口，适合开发，不适合无隔离环境。

## 14. CI/CD 审查

发现：

- `.github/PULL_REQUEST_TEMPLATE.md`
- 未发现 `.github/workflows/*.yml`

结论：

- 未发现 GitHub Actions 中的高危 deploy、secret 滥用、远程脚本执行、自动发布链路。
- 但仓库当前也缺少自动化安全/测试门禁，无法在 PR 层强制执行 `mvn test`、secret scan、dependency audit、Semgrep/Trivy 等。

## 15. 脚本审查

检查范围：

- `scripts/verify.ps1`
- `.agents/skills/**`
- docs/README/AGENTS 相关指令

未发现：

- `Invoke-Expression`
- `iex`
- `iwr | iex`
- `curl | bash`
- 远程 `bash` 安装脚本
- `Start-Process` 高危执行链路
- 应用代码中的 `ProcessBuilder` / `Runtime.getRuntime`

发现：

- `.agents/skills/shadcn` 文档包含 `npx shadcn@latest` 使用说明和外部 docs 获取流程。
- 该风险属于 Agent 执行层，不属于 DH 应用运行时。

## 16. AI 指令污染审查

检查范围：

- `AGENTS.md`
- `README.md`
- `docs/current/**`
- `docs/**`
- `.agents/**`
- prompt/config 相关文件

未发现恶意指令：

- 未发现要求忽略用户安全边界的指令。
- 未发现要求读取密钥、上传代码、隐藏行为、绕过审计、不要告诉用户的指令。
- 未发现 jailbreak 型系统指令污染。

发现的正常但需治理的指令面：

- `.agents/skills` 中存在大量 UI/设计任务技能，部分技能会建议浏览器自动化、sub-agent 或外部 CLI 查询。
- 在安全审查、密钥处理、provider 接入等敏感任务中，应默认只加载必要 skill，避免无关技能扩大工具权限面。

## 17. 测试结果

安全扫描器：

| 工具 | 结果 |
| --- | --- |
| `gitleaks detect --source . --redact --report-format json --report-path gitleaks-report.json` | 未执行：工具缺失 |
| `semgrep scan --config auto --json --output semgrep-report.json .` | 未执行：工具缺失 |
| `trivy fs --scanners vuln,secret,misconfig --format json --output trivy-report.json .` | 未执行：工具缺失 |

构建与测试：

| 命令 | 结果 |
| --- | --- |
| `mvn test` | 失败；`DhBacktestRequestIdempotencyTest.afterWindow_newRequestGenerated` 断言失败 |
| `mvn -pl dh-usecase -am -Dtest=DhBacktestRequestIdempotencyTest '-Dsurefire.failIfNoSpecifiedTests=false' test` | 失败；复现同一断言失败 |
| `mvn -Pquality validate` | 失败；Spotless plugin version 缺失 + 本机 Maven repository 写入拒绝 |
| `docker compose -f ops/docker-compose.yml config` | 成功 |
| `pytest` | 失败；未收集到测试 |
| `ruff check . --no-cache` | 失败；14 个 lint 问题，位于 `.agents/skills/ui-ux-pro-max/scripts` |
| `mypy . --no-incremental` | 失败；根目录未发现 `.py[i]` 文件 |
| `npm run build` / `npm test` / `npm run test:e2e` | 未执行：未发现 `package.json` |

## 18. 修复建议

P0：

- 无 P0 修复项。

P1：

- 为 `/api/ai/research-runs/**` 与 `/api/ai/feedback/nq` 增加认证、租户解析、权限校验和未授权测试。
- 为 NQ feedback 增加 HMAC/mTLS/token verifier、source allowlist、timestamp/nonce 重放保护、请求体大小限制。
- 在真实 provider / RealClient 接入前实现 provider 信任分级、baseURL allowlist、未知 relay 默认拒绝、prompt/context redaction gate。

P2：

- 移除配置中的默认密码 fallback，改为本地 profile 示例或 `.env.example` 占位。
- 收紧 actuator：生产默认只暴露必要 endpoint，health details 按角色或环境控制。
- 为 prompt、payload、raw response、audit fields 增加统一脱敏、大小限制、保留周期。
- 校验 `X-Trace-Id` 字符集、长度和格式。
- 固定 Spotless plugin 版本，确认 Maven settings 仓库策略，避免 snapshots 或未知镜像进入默认构建。
- Docker image pin digest；本地 compose 默认仅绑定 loopback 或增加说明。
- 对 `.agents/skills` 建立敏感任务白名单，安全任务默认禁止网络型 skill。

P3：

- 修复 `DhBacktestRequestIdempotencyTest` 当前回归失败后再推进重构。
- 对齐 `NqBacktestClient` 注释与 null 入参行为。
- 清理 `.agents` Python lint 问题，明确是否纳入质量门禁。
- 为 Python 辅助脚本补充项目配置或从主仓质量门禁中显式排除。

## 19. 最终判断

- 是否允许 DH 继续重构：允许受控继续，仅限不扩大外部暴露面、不接真实 provider、不接 RealClient 的内部重构和修复；当前测试失败必须优先修复。
- 是否允许 DH 接入 NQ：不建议继续接入；需先完成 `DH-AUDIT-FIX`，至少修复 P1-1、P1-2，并恢复 `mvn test` 通过。
- 是否必须先进入 DH-AUDIT-FIX：是。
- 是否存在中转站不可接受风险：当前未发现已接入的第三方中转站；如果后续允许未知 OpenAI-compatible relay/new-api/one-api/openrouter/siliconflow 且未做信任分级与出站闸门，则属于不可接受风险。

本轮交付物：

- `docs/current/DH_FULL_SECURITY_AUDIT_REPORT.md`

本轮未修复业务代码，未新增依赖，未删除文件，未执行真实外部调用测试，未输出任何密钥明文。
