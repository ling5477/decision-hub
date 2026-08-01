# Security Boundary

## Closed boundary

```text
FEEDBACK_INGRESS: INGEST_ONLY
IMPLICIT_LEARNING: REMOVED
MUTABLE_LEARNING_STORE_ACCESS_FROM_INBOUND: NONE
EVENT_APPEND: PRESERVED
VALIDATOR / ROUTER / IDEMPOTENCY: PRESERVED
AUTH / HMAC / TIMESTAMP / NONCE / SOURCE / TENANT / RATE_LIMIT: UNCHANGED
```

八个 handlers 和 `DefaultNqIntegrationUseCase` 只 append feedback event。append failure 继续传播，不形成
fail-open 或 false success。Spring inbound graph 不持有 `ExperienceFeedbackService` 或三个 mutable stores。

## Deferred and forbidden capabilities

- Feedback learning、case promotion、reference-liveness、retention：未授权。
- NQ mutation、NQ DB access、真实 HTTP/Provider：未授权。
- Agent、LangGraph、Paper、LIVE：未授权。
- Formal capacity：未执行；production capacity：未证明。
