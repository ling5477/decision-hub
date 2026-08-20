# Spring Platform Profile

本文件描述 Decision Hub 当前 `platform-profile.json` 探测到的 Spring 技术线及新代码规则。不得据此批量迁移历史代码或改变现有 HTTP、配置、事务和 executor 模型。

## Jakarta 与组件边界

- `SPRING-PLATFORM-JAKARTA-001`：已迁移 API 使用 `jakarta.persistence`、`jakarta.validation`、`jakarta.servlet`、`jakarta.annotation`；只禁止明确迁移包，不粗暴禁止所有 `javax.*`。
- `SPRING-PLATFORM-DI-001`：新代码默认 constructor injection；单构造器不添加无意义 `@Autowired`，历史 field injection 只进入 Shadow baseline。
- `SPRING-PLATFORM-STEREOTYPE-001`：`@Component`、`@Service`、`@Repository` 表达实际 architecture role，不用于制造 ceremony。

## 配置与事务

- `SPRING-PLATFORM-CONFIG-001`：优先沿用 typed `@ConfigurationProperties`、validation 和显式安全默认值；不得重写现有 namespace、hard ceiling 或兼容层。
- `SPRING-PLATFORM-TX-001`：`@Transactional` 只在 Spring-managed proxy boundary 生效；检查 self invocation、private method、propagation、readOnly、isolation、rollback、async 与外部副作用。
- `SPRING-TRANSACTION-PRIVATE-METHOD`：禁止在 private helper 上添加 `@Transactional` 并假定代理生效。

## Async、HTTP 与测试

- `SPRING-PLATFORM-ASYNC-001`：异步代码使用项目管理的 executor，明确 queue/backpressure、context、failure、timeout 和 shutdown；virtual threads 不得绕过 observability。
- `SPRING-PLATFORM-HTTP-001`：新 HTTP 代码服从现有 adapter/client 架构，配置 timeout、错误映射、有限重试、幂等、日志和 secret filtering；不得为“现代化”切换 blocking/reactive 模型。
- `SPRING-PLATFORM-TEST-001`：区分 unit、slice、context integration、PostgreSQL/Testcontainers、contract 与 architecture test；禁止一律升级为 `@SpringBootTest`。
- `SPRING-DEPRECATED-TEST-ANNOTATION`：新增测试只有在实际 Spring Framework 支持且需要 Bean override 时才采用当前 Mockito Bean API；历史 deprecated annotation 只进入 Shadow baseline。

## 当前项目事实

- 当前未引入 Spring Security；不得因为通用规范假设 Security API 或新增依赖。
- 当前使用 Spring Data JPA/Redis，并存在 WebClient、RestTemplate 与 provider/connector 边界；本任务不迁移 client 模型。
- 当前未检测到 Mockito Bean override annotation；普通 Mockito/unit test 不应为了形式升级为 Spring context test。
