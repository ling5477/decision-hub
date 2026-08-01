# Milestone Plan

## Frozen objective

隔离 legacy NQ feedback ingress 的隐式 learning mutation，使 inbound boundary 固化为：

```text
INGEST_ONLY
NO_IMPLICIT_LEARNING
NO_MUTABLE_LEARNING_STORE_ACCESS
```

完整原始计划保存在
[source/DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md](source/DH_POST_STAGE_QDR_9_NEXT_STAGE_PLAN.md)，
SHA-256 由 [SOURCE_MANIFEST.md](SOURCE_MANIFEST.md) 冻结。

## Planned route

1. B1：冻结 contract 与完整 call chain。
2. B2：将八个 handlers 和 legacy compatibility use case 改为 append-only。
3. B3：补齐 unit、Spring wiring、WebMvc、architecture 与 full regression guards。
4. B4：独立 exact-diff security review、publication、exact-SHA CI、archive-before-tag 与治理关闭。

## Non-goals

- 不连接 structured QDR attribution 到 legacy ingress。
- 不启用 feedback learning、case promotion 或 memory evolution。
- 不修改 API、migration、Repository、contracts、POM 或 workflow。
- 不执行 formal capacity，不宣称 production ready。
- 不连接 NQ runtime、真实 HTTP/Provider、Agent、LangGraph、Paper 或 LIVE。
