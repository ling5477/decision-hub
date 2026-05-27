package com.guidinglight.decisionhub.usecase.agent.feedback.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stage2-PoC-B2：handler 公共基类。
 *
 * <p>每个 8 个 handler 的最小闭环：
 *
 * <ol>
 *   <li>解析 {@code envelope.payloadJson} 为 {@code Map<String,Object>}（失败则降级为空 map）。
 *   <li>构造 Stage1 {@link NqFeedbackEvent} 桥接现有 {@link ExperienceFeedbackService}。
 *   <li>追加 Stage1 事件（{@link NqFeedbackEventRepository#append}）便于 Stage1 查询继续可用。
 *   <li>调用 {@link ExperienceFeedbackService#apply(NqFeedbackEvent)} 触发经验/信息素更新。
 * </ol>
 *
 * <p>禁止在子类里写复杂业务推理；子类只能贡献：positive 判定、FeedbackSource 选择、candidateId 解析。
 *
 * <p>tenantId 必须来自认证后的 ingestion command，禁止 handler 使用硬编码默认 tenant。
 */
public abstract class AbstractNqFeedbackEventHandler implements NqFeedbackEventHandler {

  private final ExperienceFeedbackService experienceFeedbackService;
  private final NqFeedbackEventRepository feedbackEventRepository;
  private final ObjectMapper objectMapper;

  protected AbstractNqFeedbackEventHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper objectMapper) {
    this.experienceFeedbackService = experienceFeedbackService;
    this.feedbackEventRepository = feedbackEventRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public final void handle(final NqFeedbackEnvelope envelope, final String tenantId) {
    final Map<String, Object> payload = parsePayload(envelope.getPayloadJson());
    final NqFeedbackEvent event =
        NqFeedbackEvent.create(
            tenantId,
            envelope.getTraceId(), // Stage1：traceId == runId
            extractCandidateId(payload),
            envelope.getTraceId(),
            feedbackSource(),
            envelope.getEventType().name(),
            isPositive(payload),
            payload,
            envelope.getOccurredAt(),
            envelope.getReceivedAt() == null ? TimeProvider.now() : envelope.getReceivedAt());
    feedbackEventRepository.append(event);
    experienceFeedbackService.apply(event);
  }

  /** 子类决定 Stage1 FeedbackSource 映射（BACKTEST / PAPER / RISK / RELEASE / LIVE / REVIEW）。 */
  protected abstract FeedbackSource feedbackSource();

  /** 子类按 payload 决定 positive 判定。默认 true，不做复杂业务推理。 */
  protected boolean isPositive(final Map<String, Object> payload) {
    return true;
  }

  /** 默认从 payload 取 {@code candidateId}，可被子类覆盖。 */
  protected String extractCandidateId(final Map<String, Object> payload) {
    final Object v = payload.get("candidateId");
    return v == null ? IdGenerator.newId() : String.valueOf(v);
  }

  /**
   * 把原始 payloadJson 解析为可变 {@code Map<String,Object>}，并保留 {@code rawPayloadJson}
   * 字段（供 Stage1 经验链路审计）。
   *
   * <p>解析失败时退化为只含 {@code rawPayloadJson} 的 map，不抛异常（handler 不允许中断 ingestion）。
   */
  private Map<String, Object> parsePayload(final String payloadJson) {
    final Map<String, Object> result = new HashMap<>();
    result.put("rawPayloadJson", payloadJson);
    if (payloadJson == null || payloadJson.isBlank()) {
      return result;
    }
    try {
      final JsonNode node = objectMapper.readTree(payloadJson);
      if (node != null && node.isObject()) {
        final Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
          final Map.Entry<String, JsonNode> e = it.next();
          result.putIfAbsent(e.getKey(), unwrap(e.getValue()));
        }
      }
    } catch (Exception ignored) {
      // 解析失败：保留 rawPayloadJson，让上层基于原文做后续校验。
    }
    return result;
  }

  private static Object unwrap(final JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isInt() || node.isLong()) {
      return node.asLong();
    }
    if (node.isFloatingPointNumber()) {
      return node.asDouble();
    }
    return node.toString();
  }
}
