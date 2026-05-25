package com.guidinglight.decisionhub.usecase.agent.feedback;

/**
 * Stage2-PoC-B2：NQ feedback ingestion 入口用例。
 *
 * <p>典型流程（详见 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.4）：
 *
 * <pre>
 *   ingest(command)
 *     1. eventType 字符串映射到枚举
 *     2. NqFeedbackContractValidator 全量校验
 *     3. 幂等：findEnvelopeByEventId 命中 -> DUPLICATE
 *     4. saveEnvelope（唯一键冲突视为 DUPLICATE）
 *     5. NqFeedbackEventTypeRouter 派发 handler
 *     6. 返回 ACCEPTED
 * </pre>
 */
public interface NqFeedbackIngestionService {

  /** 处理一条 ingestion 命令；不抛业务异常，全部以 {@link IngestionResult} 返回。 */
  IngestionResult ingest(IngestionCommand command);
}
