package com.guidinglight.decisionhub.domain.feedback;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：NQ -> DH 正式回流事件信封。
 *
 * <p>不同于 Stage1 的 {@link NqFeedbackEvent}（仅承载经验权重所需的最小字段），本类是 Stage2 起 NQ 与 DH 之间的统一正式契约：
 *
 * <ul>
 *   <li>{@code eventId} 由 NQ 侧生成，用于 DH 侧幂等（同一 eventId 重放视作命中）。
 *   <li>{@code eventType} 强类型化为 {@link NqFeedbackEventType}，禁止使用自由文本。
 *   <li>{@code sourceSystem} 固定常量 {@link #SOURCE_SYSTEM_NEXUS_QUANT}，用于跨系统识别。
 *   <li>{@code sourceJobId} 是 NQ 侧 job / paper run / backtest 的稳定 ID，便于对账。
 *   <li>{@code traceId} 是 DH 发起请求时生成的全链路 trace，关联 ResearchRun。
 *   <li>{@code requestId} 是单次 HTTP 请求级 ID，可由客户端透传或由 DH 入口注入。
 *   <li>{@code correlationId} 是跨服务关联 ID；如客户端未传，由上层用 {@code traceId} 兜底。
 *   <li>{@code payloadJson} 必须保留原始 JSON 字符串，不允许丢失原始数据；具体结构由 {@link NqFeedbackEventType} 决定。
 * </ul>
 *
 * <p>本类故意不引入 Jackson 强依赖，保留 {@code payloadJson} 为字符串，由上层 Validator / Mapper 自行解析为强类型 payload value
 * object。
 */
public final class NqFeedbackEnvelope {

  /** {@code sourceSystem} 唯一合法值（NQ 侧）。 */
  public static final String SOURCE_SYSTEM_NEXUS_QUANT = "nexus-quant";

  /** 默认 schema 版本（与 contracts/json-schema/nq-feedback-envelope.schema.json 对齐）。 */
  public static final String DEFAULT_SCHEMA_VERSION = "1.0.0";

  private final String eventId;
  private final NqFeedbackEventType eventType;
  private final Instant occurredAt;
  private final String sourceSystem;
  private final String sourceJobId;
  private final String traceId;
  private final String requestId;
  private final String correlationId;
  private final String schemaVersion;
  private final String payloadJson;
  private final Instant receivedAt;

  private NqFeedbackEnvelope(
      final String eventId,
      final NqFeedbackEventType eventType,
      final Instant occurredAt,
      final String sourceSystem,
      final String sourceJobId,
      final String traceId,
      final String requestId,
      final String correlationId,
      final String schemaVersion,
      final String payloadJson,
      final Instant receivedAt) {
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.eventType = Objects.requireNonNull(eventType, "eventType");
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    this.sourceSystem = Objects.requireNonNull(sourceSystem, "sourceSystem");
    this.sourceJobId = Objects.requireNonNull(sourceJobId, "sourceJobId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
    this.schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
    this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
    this.receivedAt = receivedAt;
  }

  /**
   * 工厂方法：构造 Stage2 envelope。
   *
   * <p>所有字段必填（除 {@code receivedAt} 由 DH 入口在落库前补写）。
   *
   * @param eventId NQ 侧事件 ID，幂等键
   * @param eventType 事件类型（必须是 {@link NqFeedbackEventType} 枚举之一）
   * @param occurredAt NQ 侧事件发生时间（ISO-8601）
   * @param sourceSystem 必须等于 {@link #SOURCE_SYSTEM_NEXUS_QUANT}
   * @param sourceJobId NQ 侧 job / paper run / backtest 稳定 ID
   * @param traceId DH 侧 trace（全链路）
   * @param requestId 单次 HTTP 请求 ID
   * @param correlationId 跨服务关联 ID
   * @param schemaVersion semver，默认 {@link #DEFAULT_SCHEMA_VERSION}
   * @param payloadJson 原始 payload JSON 字符串（不允许丢失）
   * @param receivedAt DH 收到事件的时间（可空，由入口补写）
   * @return Stage2 envelope 值对象
   */
  public static NqFeedbackEnvelope of(
      final String eventId,
      final NqFeedbackEventType eventType,
      final Instant occurredAt,
      final String sourceSystem,
      final String sourceJobId,
      final String traceId,
      final String requestId,
      final String correlationId,
      final String schemaVersion,
      final String payloadJson,
      final Instant receivedAt) {
    return new NqFeedbackEnvelope(
        eventId,
        eventType,
        occurredAt,
        sourceSystem,
        sourceJobId,
        traceId,
        requestId,
        correlationId,
        schemaVersion,
        payloadJson,
        receivedAt);
  }

  public String getEventId() {
    return eventId;
  }

  public NqFeedbackEventType getEventType() {
    return eventType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getSourceJobId() {
    return sourceJobId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  /** 原始 payload JSON 字符串，必须保留，禁止丢失原始数据。 */
  public String getPayloadJson() {
    return payloadJson;
  }

  /** DH 收到事件的时间；入口未补写前可为 {@code null}。 */
  public Instant getReceivedAt() {
    return receivedAt;
  }
}
