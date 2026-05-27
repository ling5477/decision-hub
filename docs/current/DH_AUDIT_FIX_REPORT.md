# DH AUDIT FIX REPORT

## 1. 结论

- P1-1：已关闭。`/api/ai/research-runs/**` 与 `/api/ai/feedback/nq` 已接入请求认证与租户上下文，未授权请求返回 401，认证租户与受控 header 不一致返回 403。
- P1-2：已关闭。NQ feedback 已增加 HMAC 来源认证、timestamp/nonce 防重放、source allowlist、payload size gate 与结构校验前置。
- P1-3：已关闭。已补齐 ProviderTrustPolicy 安全策略框架，默认拒绝 UNKNOWN / UNTRUSTED_RELAY，拒绝未知 OpenAI-compatible relay 类配置，并提供敏感上下文出站 redaction gate。
- `mvn test`：已恢复，通过；本机 Docker 不可用时 `PostgresContainerSmokeTest` 按 Testcontainers 能力检测跳过。
- 是否允许继续 DH 内部重构：允许，仅限受控内部重构，不允许接真实 provider、不允许接 NQ RealClient、不允许对外暴露。
- 是否允许进入 NQ 集成边界审查：允许进入 `NQ_DH_INTEGRATION_SECURITY_AUDIT`；在该审查通过前，不允许 DH 接入 NQ 或真实 provider。

## 2. 修改清单

- `pom.xml`：补齐 `spotless-maven-plugin` 版本属性，恢复 `mvn -Pquality validate` 质量门禁。
- `dh-api/pom.xml`：补充 `dh-security` 依赖，使 API 层可复用现有认证与安全策略。
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/security/AuthenticatedRequest.java`：新增认证上下文与租户解析工具，避免控制器继续使用硬编码默认 tenant。
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java`：新增 API 认证过滤器，保护 research run 与 NQ feedback 入口。
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/research/ResearchRunController.java`：将 research run 创建、查询、启动、任务、候选、裁决接口改为从认证上下文取 tenant，并校验资源租户隔离。
- `dh-api/src/main/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackController.java`：将 NQ feedback 入口改为先认证请求与 NQ 来源，再按认证 tenant 入库。
- `dh-api/src/test/java/com/guidinglight/decisionhub/api/research/ResearchRunControllerSecurityWebMvcTest.java`：新增 research run 认证与租户隔离回归测试。
- `dh-api/src/test/java/com/guidinglight/decisionhub/api/feedback/NqFeedbackControllerWebMvcTest.java`：扩展 NQ feedback 认证、签名、防重放、allowlist、大小限制与成功路径测试。
- `dh-app/pom.xml`：补充 `dh-security` 直接依赖，装配安全 Bean。
- `dh-app/src/main/java/com/guidinglight/decisionhub/config/SecurityWiringConfig.java`：新增最小安全装配；默认 fail-closed，不配置 token hash 或 HMAC secret 时拒绝请求。
- `dh-app/src/test/java/com/guidinglight/decisionhub/PostgresContainerSmokeTest.java`：在本机 Docker 不可用时跳过容器 smoke test，避免环境能力导致 `mvn test` 失败。
- `dh-security/pom.xml`：补充 JUnit 测试依赖。
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/AuthContext.java`：补充安全上下文说明。
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/TokenVerifier.java`：补充认证接口说明。
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/StaticTokenVerifier.java`：新增 SHA-256 bearer token verifier，避免明文 token 入库或日志输出。
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/*`：新增 NQ feedback HMAC 认证、防重放与认证结果模型。
- `dh-security/src/main/java/com/guidinglight/decisionhub/security/provider/*`：新增 provider trust level、trust policy、审计字段与敏感上下文 gate。
- `dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqFeedbackAuthenticatorTest.java`：新增 NQ feedback 来源认证回归测试。
- `dh-security/src/test/java/com/guidinglight/decisionhub/security/provider/ProviderTrustPolicyTest.java`：新增 provider trust policy 与 redaction gate 回归测试。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/backtest/InMemoryDhBacktestRequestRepository.java`：修复幂等窗口测试失败根因，repository 使用请求对象的 `requestedAt` 作为窗口时间基准。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/NqFeedbackEventHandler.java`：反馈处理接口显式接收认证 tenant。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/NqFeedbackEventTypeRouter.java`：反馈路由显式传递认证 tenant。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/DefaultNqFeedbackIngestionService.java`：由 ingestion command 的 tenant 驱动事件路由。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/DefaultNqFeedbackEventTypeRouter.java`：将 tenant 透传到 handler。
- `dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/feedback/AbstractNqFeedbackEventHandler.java`：移除事件落库时的硬编码默认 tenant。
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/feedback/NqFeedbackHandlerDispatchTest.java`：更新 handler dispatch 测试，验证认证 tenant 传递。
- `dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/feedback/NqFeedbackIdempotencyTest.java`：更新幂等测试，验证反馈事件使用认证 tenant。

## 3. P1-1 关闭证据

- Research run API 已由 `DhApiAuthenticationFilter` 保护，覆盖路径为 `/api/ai/research-runs` 与 `/api/ai/research-runs/**`。
- NQ feedback API 已由同一过滤器保护，覆盖路径为 `/api/ai/feedback/nq`。
- 认证结果由 `TokenVerifier` 写入 request attribute，控制器只能通过 `AuthenticatedRequest.requireTenantId` 获取 tenant。
- `X-DH-Tenant-Id` 作为受控租户 header，仅允许与认证 tenant 一致；不一致返回 403。
- Research run 资源读取、启动、任务、候选、裁决均校验资源 tenant 与认证 tenant 一致。
- 覆盖测试：
  - 无认证创建 research run 返回 401。
  - 无认证查询 research run 返回 401。
  - 无认证提交 NQ feedback 返回 401。
  - 有效认证后允许创建、查询、提交。
  - tenant header 与认证 tenant 不匹配时返回 403。

## 4. P1-2 关闭证据

- NQ feedback 入口在入库前调用 `NqFeedbackAuthenticator`，认证失败时不会构造 ingestion command。
- `HmacNqFeedbackAuthenticator` 校验：
  - 必须存在 source、timestamp、nonce、signature。
  - `X-DH-NQ-Source` 必须与请求体 `sourceSystem` 一致，并命中 allowlist。
  - timestamp 必须落在允许偏移窗口内。
  - nonce 与 requestId 组合必须未被使用，重放返回 409。
  - HMAC-SHA256 签名必须匹配 canonical fields。
  - payload bytes 不得超过配置上限。
- `payload_json` 保持原样入库，但只在来源认证与结构校验后进入 ingestion service。
- 覆盖测试：
  - 缺少签名拒绝。
  - 错误签名拒绝。
  - 过期 timestamp 拒绝。
  - 重放 nonce/requestId 拒绝。
  - 非 allowlist source 拒绝。
  - 合法 NQ feedback 接收成功。
  - 超大请求体拒绝。

## 5. P1-3 关闭证据

- 已新增 `ProviderTrustLevel`：
  - `OFFICIAL_API`
  - `SELF_HOSTED_GATEWAY`
  - `CONTROLLED_RELAY`
  - `UNTRUSTED_RELAY`
  - `UNKNOWN`
- `DefaultProviderTrustPolicy` 默认拒绝：
  - 未配置 baseURL。
  - 未命中 allowlist 的 baseURL。
  - `UNTRUSTED_RELAY`。
  - 包含 openrouter、siliconflow、new-api、one-api、relay、proxy 等未知中转特征的 OpenAI-compatible relay。
- allowlist 命中且 trust level 为 `OFFICIAL_API`、`SELF_HOSTED_GATEWAY` 或 `CONTROLLED_RELAY` 时才允许通过基础出站 gate。
- `DefaultPromptContextRedactionGate` 对敏感上下文进一步收紧：敏感上下文只允许发往 `OFFICIAL_API` 或 `SELF_HOSTED_GATEWAY`。
- 审计字段已包含 provider、baseUrlHash、trustLevel、traceId，不记录 raw baseURL。
- 本轮只实现策略框架与测试，不接真实 provider、不发起真实 LLM 调用。

## 6. 测试结果

- `git status --short`：已执行；工作区包含本轮 P1 修复、测试、报告文件，以及上一轮审计报告未跟踪文件。
- `git diff --stat`：已执行；用于核对 tracked 变更范围。
- `git diff`：已执行；用于核对实现差异，无敏感值输出到报告。
- `mvn test`：通过。`PostgresContainerSmokeTest` 在本机 Docker 不可用时跳过，其他测试通过。
- `mvn -pl dh-usecase -am -Dtest=DhBacktestRequestIdempotencyTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过，3 tests，0 failures。
- `docker compose -f ops/docker-compose.yml config`：通过，compose 配置可解析；报告不复述任何敏感默认值。
- `mvn -Pquality validate`：通过。Spotless plugin version 已补齐，质量门禁恢复。

## 7. 剩余 P2/P3

- 本轮未扩大修复 P2/P3。
- 仍保留原 DH FULL SECURITY AUDIT 中登记的 P2 = 7、P3 = 4，后续应进入独立安全加固或质量收口批次处理。
- 当前 P2/P3 不阻断 DH 继续受控内部重构，但仍阻断真实 provider、真实 NQ 集成与对外暴露。

## 8. 最终判断

- 是否允许 DH 继续受控重构：允许。
- 是否允许 DH 接入 NQ：不允许直接接入；必须先进入并通过 `NQ_DH_INTEGRATION_SECURITY_AUDIT`。
- 是否需要 `NQ_DH_INTEGRATION_SECURITY_AUDIT`：需要。
- 是否允许真实 provider 接入：不允许；必须另行完成 provider 出站安全审查、密钥管理审查与真实调用隔离验证。
