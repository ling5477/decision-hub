# Decision-Hub 日志规范（Final）

## 1. 目标与原则
1) **可观测性优先**：日志用于排障、审计、指标归因、链路追踪。  
2) **结构化优先**：优先键值对参数化日志，避免字符串拼接。  
3) **低噪音**：减少无意义 INFO；错误必须给出上下文。  
4) **可关联**：通过 traceId / runId / planId 等串联日志。  
5) **安全合规**：禁止输出敏感信息（token/密码/密钥/隐私）。

## 2. 技术选型与依赖边界
- **应用模块（dh-app）**：决定日志实现（默认 Logback），提供配置（logback-spring.xml）。
- **库模块（dh-common/dh-domain/dh-usecase/dh-eval 等）**：
  - 仅依赖 `org.slf4j:slf4j-api`（如编译期需要）
  - 禁止引入任何日志实现（logback/log4j2）
- **禁止** 使用 `System.out/err`（由 Checkstyle 门禁）。

## 3. Logger 使用方式（统一）
```java
private static final Logger log = LoggerFactory.getLogger(CurrentClass.class);
```

## 4. 日志级别规范
- **ERROR**：失败不可继续，必须包含异常栈（如有）。
- **WARN**：非致命问题/降级/重试。
- **INFO**：关键业务节点（创建、完成、结果摘要）。
- **DEBUG**：诊断细节（默认生产关闭）。
- **TRACE**：极高频细节（临时排障）。

## 5. 参数化与异常
- 禁止字符串拼接：
```java
log.info("runId={} status={}", runId, status);
```
- 异常必须带堆栈：
```java
log.error("createRun failed runId={}", runId, e);
```

## 6. 敏感信息与脱敏
- 禁止输出：密码、密钥、Token、会话、完整隐私。
- 脱敏示例：手机号 `138****1234`，Token `abc****xyz`。

## 7. MDC 规范（推荐）
- 入口写入：traceId/tenantId/userId。
- 异步必须传递 MDC。
- 结束清理：`MDC.clear()`。

## 8. Golden/离线任务（dh-eval）
- 禁止 `System.out/err`。
- INFO：PASS 总结；ERROR：FAIL 明细。
```java
log.info("[GOLDEN] PASS op={} caseId={} file={}", op, caseId, file);
log.error("[GOLDEN] FAIL op={} caseId={} file={}", op, caseId, file);
```

## 9. 性能与容量
- 循环内禁止 INFO/WARN/ERROR（除非限流）。
- 批处理：按 N 条输出进度。
- 大文本需截断并记录长度。

## 10. 门禁与评审
- Checkstyle 禁止 `System.out/err`。
- Code Review 检查：参数化、异常栈、关键 ID、敏感信息。

## 11. 示例
```java
final long start = System.nanoTime();
try {
  final RunCreateResult res = facade.createRun(cmd);
  log.info("[RUN] createRun success runId={} durationMs={}",
      res.getRunId(), (System.nanoTime()-start)/1_000_000);
} catch (final Exception e) {
  log.error("[RUN] createRun failed traceId={} durationMs={}",
      MDC.get("traceId"), (System.nanoTime()-start)/1_000_000, e);
  throw e;
}
```
