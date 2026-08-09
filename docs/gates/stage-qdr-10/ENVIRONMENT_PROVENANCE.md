# Environment Provenance

## Accepted contract

```text
trusted caller = FeedbackExecutionScope
caller environment = persisted decision origin = feedback environment
```

Persisted decision origin 通过 tenant-bound、parameterized、bounded 的单次查询证明；证明链要求：

- persistent guard 状态为 `COMPLETED`；
- exact V5/V6 decision identifiers；
- exact core request/run identity；
- 唯一且无歧义的 environment。

missing、duplicate、ambiguous、cross-tenant、cross-environment 或 storage error 均返回不可用结果，不允许用 Spring profile、header、唯一命中、latest row 或默认值推断 environment。
