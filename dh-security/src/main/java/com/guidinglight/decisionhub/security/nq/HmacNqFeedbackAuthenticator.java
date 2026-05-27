package com.guidinglight.decisionhub.security.nq;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * NQ feedback HMAC 来源认证实现。
 *
 * <p>Why：仅校验 envelope 里的 sourceSystem 无法证明调用方来自 NQ。本实现用共享密钥对
 * source/timestamp/nonce/requestId/payload 等关键字段签名，并用 nonce/requestId 防重放。
 * 失败原因只返回类型，不暴露密钥、签名或 payload 明文。
 */
public final class HmacNqFeedbackAuthenticator implements NqFeedbackAuthenticator {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final Set<String> allowedSources;
  private final String secret;
  private final Duration maxClockSkew;
  private final long maxPayloadBytes;
  private final NonceReplayGuard replayGuard;

  /**
   * 构造 HMAC authenticator。
   *
   * @param allowedSources 允许的 NQ sourceSystem 白名单。
   * @param secret HMAC 共享密钥；为空时拒绝所有请求。
   * @param maxClockSkew timestamp 最大偏移。
   * @param maxPayloadBytes 请求体或 payload 最大字节数。
   * @param replayGuard nonce/requestId 防重放实现。
   */
  public HmacNqFeedbackAuthenticator(
      final Set<String> allowedSources,
      final String secret,
      final Duration maxClockSkew,
      final long maxPayloadBytes,
      final NonceReplayGuard replayGuard) {
    this.allowedSources =
        allowedSources == null
            ? Set.of()
            : allowedSources.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(HmacNqFeedbackAuthenticator::normalizeSource)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    this.secret = secret == null ? "" : secret;
    this.maxClockSkew = maxClockSkew == null ? Duration.ofMinutes(5) : maxClockSkew;
    this.maxPayloadBytes = maxPayloadBytes <= 0 ? 64 * 1024L : maxPayloadBytes;
    this.replayGuard = Objects.requireNonNull(replayGuard, "replayGuard");
  }

  @Override
  public NqFeedbackAuthResult authenticate(final NqFeedbackAuthRequest request) {
    Objects.requireNonNull(request, "request");
    if (request.contentLength() > maxPayloadBytes
        || utf8Size(request.payloadJson()) > maxPayloadBytes) {
      return NqFeedbackAuthResult.rejected(413, "PAYLOAD_TOO_LARGE");
    }
    if (secret.isBlank()) {
      return NqFeedbackAuthResult.rejected(401, "NQ_SECRET_NOT_CONFIGURED");
    }
    final String source = normalizeSource(request.sourceSystem());
    final String sourceHeader = normalizeSource(request.sourceHeader());
    if (source.isBlank() || !source.equals(sourceHeader) || !allowedSources.contains(source)) {
      return NqFeedbackAuthResult.rejected(403, "SOURCE_NOT_ALLOWED");
    }
    final Instant timestamp = parseTimestamp(request.timestampHeader());
    if (timestamp == null || isExpired(timestamp, request.now())) {
      return NqFeedbackAuthResult.rejected(401, "TIMESTAMP_EXPIRED");
    }
    if (isBlank(request.nonce()) || isBlank(request.requestId())) {
      return NqFeedbackAuthResult.rejected(401, "REPLAY_KEY_MISSING");
    }
    if (!verifySignature(request, timestamp)) {
      return NqFeedbackAuthResult.rejected(401, "BAD_SIGNATURE");
    }
    final String replayKey = source + "::" + request.nonce() + "::" + request.requestId();
    final Instant replayExpiresAt =
        (request.now() == null ? Instant.now() : request.now()).plus(maxClockSkew.multipliedBy(2));
    if (!replayGuard.markIfAbsent(replayKey, replayExpiresAt)) {
      return NqFeedbackAuthResult.rejected(409, "REPLAY_DETECTED");
    }
    return NqFeedbackAuthResult.success();
  }

  /**
   * 签名材料的稳定格式。
   *
   * <p>字段顺序固定，避免 JSON 序列化顺序差异影响验签；payloadJson 仍参与签名，防止主体被篡改。
   */
  public static String signatureMaterial(final NqFeedbackAuthRequest request) {
    return String.join(
        "\n",
        normalizeSource(request.sourceSystem()),
        value(request.timestampHeader()),
        value(request.nonce()),
        value(request.eventId()),
        value(request.requestId()),
        value(request.traceId()),
        value(request.payloadJson()));
  }

  /** 计算 HMAC-SHA256 hex；仅用于调用方生成或测试签名。 */
  public static String hmacSha256Hex(final String secret, final String material) {
    try {
      final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      final byte[] bytes = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (final byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC-SHA256 not available", e);
    }
  }

  private boolean verifySignature(final NqFeedbackAuthRequest request, final Instant timestamp) {
    final NqFeedbackAuthRequest canonical =
        new NqFeedbackAuthRequest(
            request.sourceHeader(),
            request.sourceSystem(),
            timestamp.toString(),
            request.nonce(),
            request.signature(),
            request.eventId(),
            request.requestId(),
            request.traceId(),
            request.payloadJson(),
            request.contentLength(),
            request.now());
    final String expected = hmacSha256Hex(secret, signatureMaterial(canonical));
    final String actual = request.signature() == null ? "" : request.signature().trim().toLowerCase();
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private boolean isExpired(final Instant timestamp, final Instant now) {
    final Instant anchor = now == null ? Instant.now() : now;
    return timestamp.isBefore(anchor.minus(maxClockSkew)) || timestamp.isAfter(anchor.plus(maxClockSkew));
  }

  private static Instant parseTimestamp(final String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (final RuntimeException ignored) {
      return null;
    }
  }

  private static long utf8Size(final String value) {
    return value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  private static String value(final String value) {
    return value == null ? "" : value;
  }

  private static String normalizeSource(final String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
