package com.guidinglight.decisionhub.security.nq;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Integration-1 limited dry-run endpoint 的 HMAC / timestamp / nonce / tenant-source gate。
 *
 * <p>本实现只处理入站 dry-run 校验，不发起 HTTP、不调用 NQ、不读取 provider 或 credential。它复用既有
 * {@link NonceReplayGuard} 端口承接持久化 replay 防护，并将 replay key 命名空间绑定到
 * tenant/source/endpoint/nonce/requestId，避免与旧 feedback endpoint 或其他 future endpoint 共享 nonce 空间。
 *
 * <p>线程安全：类本身不可变，是否线程安全取决于注入的 {@link NonceReplayGuard} 是否满足原子
 * mark-if-absent 语义。
 */
public final class HmacNqDryRunAuthenticator {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final Set<String> allowedSources;
  private final Set<String> allowedTenantSourcePairs;
  private final Set<String> allowedSourceEnvironmentPairs;
  private final Set<String> allowedTenantEnvironmentPairs;
  private final Set<String> allowedTenantSourceEnvironmentTriples;
  private final String secret;
  private final Duration maxClockSkew;
  private final long maxPayloadBytes;
  private final NonceReplayGuard replayGuard;

  /**
   * 创建 dry-run HMAC authenticator。
   *
   * @param allowedSources 允许的 source 名称；配置值可去除首尾空白，但 request wire value 必须精确匹配。
   * @param allowedTenantSourcePairs 允许的 tenant/source pair，格式支持 tenant:NQ_DRYRUN 或 tenant::NQ_DRYRUN。
   * @param secret HMAC secret；为空时 fail-closed。
   * @param maxClockSkew timestamp 最大偏移窗口。
   * @param maxPayloadBytes raw body 最大字节数。
   * @param replayGuard nonce replay guard；生产应使用持久化实现。
   */
  public HmacNqDryRunAuthenticator(
      final Set<String> allowedSources,
      final Set<String> allowedTenantSourcePairs,
      final Set<String> allowedSourceEnvironmentPairs,
      final Set<String> allowedTenantEnvironmentPairs,
      final Set<String> allowedTenantSourceEnvironmentTriples,
      final String secret,
      final Duration maxClockSkew,
      final long maxPayloadBytes,
      final NonceReplayGuard replayGuard) {
    this.allowedSources =
        allowedSources == null
            ? Set.of()
            : allowedSources.stream()
                .filter(s -> !isBlank(s))
                .map(HmacNqDryRunAuthenticator::normalizeConfiguredSource)
                .collect(Collectors.toUnmodifiableSet());
    this.allowedTenantSourcePairs =
        allowedTenantSourcePairs == null
            ? Set.of()
            : allowedTenantSourcePairs.stream()
                .filter(s -> !isBlank(s))
                .map(HmacNqDryRunAuthenticator::normalizePairConfig)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    this.allowedSourceEnvironmentPairs = normalizeConfiguredBindings(allowedSourceEnvironmentPairs);
    this.allowedTenantEnvironmentPairs = normalizeConfiguredBindings(allowedTenantEnvironmentPairs);
    this.allowedTenantSourceEnvironmentTriples =
        normalizeConfiguredBindings(allowedTenantSourceEnvironmentTriples);
    this.secret = secret == null ? "" : secret;
    this.maxClockSkew = maxClockSkew == null ? Duration.ofMinutes(5) : maxClockSkew;
    this.maxPayloadBytes = maxPayloadBytes <= 0 ? 64 * 1024L : maxPayloadBytes;
    this.replayGuard = Objects.requireNonNull(replayGuard, "replayGuard");
  }

  /** Preserves the former constructor while making every environment authorization fail closed. */
  public HmacNqDryRunAuthenticator(
      final Set<String> allowedSources,
      final Set<String> allowedTenantSourcePairs,
      final String secret,
      final Duration maxClockSkew,
      final long maxPayloadBytes,
      final NonceReplayGuard replayGuard) {
    this(
        allowedSources,
        allowedTenantSourcePairs,
        Set.of(),
        Set.of(),
        Set.of(),
        secret,
        maxClockSkew,
        maxPayloadBytes,
        replayGuard);
  }

  /**
   * 校验 dry-run request 的 source、tenant、timestamp、signature 与 nonce。
   *
   * <p>source allowlist 与 tenant/source pair 使用验签后的 wire value 做精确匹配；验签材料本身不得被
   * lowercase、alias 或 fallback source 改写，否则 NQ 与 DH 会重新出现 HMAC material drift。
   *
   * @param request dry-run auth request。
   * @return 通过或 fail-closed 结果。
   */
  public NqDryRunAuthResult authenticate(final NqDryRunAuthRequest request) {
    Objects.requireNonNull(request, "request");
    if (isPayloadTooLarge(request.rawBody(), request.contentLength())) {
      return NqDryRunAuthResult.rejected(
          413, "PAYLOAD_TOO_LARGE", "dry-run request payload exceeds configured cap");
    }
    if (secret.isBlank()) {
      return NqDryRunAuthResult.rejected(
          401, "SIGNATURE_INVALID", "dry-run HMAC secret is not configured");
    }
    if (!sameTenant(request.authenticatedTenantId(), request.tenantId())) {
      return NqDryRunAuthResult.rejected(
          403, "TENANT_MISMATCH", "body tenant does not match authenticated tenant");
    }
    final String source = wireValue(request.sourceSystem());
    final String sourceHeader = wireValue(request.sourceHeader());
    final TimestampCheck timestamp =
        parseTimestamp(request.timestampHeader(), request.now(), maxClockSkew);
    if (timestamp.status() != TimestampStatus.OK) {
      return switch (timestamp.status()) {
        case INVALID ->
            NqDryRunAuthResult.rejected(
                401, "TIMESTAMP_INVALID", "timestamp must be RFC3339 UTC Z");
        case OUT_OF_WINDOW ->
            NqDryRunAuthResult.rejected(
                401, "TIMESTAMP_OUT_OF_WINDOW", "timestamp is outside replay window");
        case OK -> throw new IllegalStateException("unreachable timestamp status");
      };
    }
    if (isBlank(request.nonce()) || isBlank(request.requestId()) || isBlank(request.traceId())) {
      return NqDryRunAuthResult.rejected(
          401, "SIGNATURE_INVALID", "replay or signature binding key is missing");
    }
    final FeedbackEnvironment environment;
    try {
      environment = FeedbackEnvironment.fromWire(request.environment());
    } catch (final IllegalArgumentException error) {
      return NqDryRunAuthResult.rejected(
          403,
          isBlank(request.environment()) ? "ENVIRONMENT_REQUIRED" : "ENVIRONMENT_INVALID",
          "dry-run environment is missing or unsupported");
    }
    if (!verifySignature(request)) {
      return NqDryRunAuthResult.rejected(401, "SIGNATURE_INVALID", "bad dry-run signature");
    }
    if (source.isBlank() || !source.equals(sourceHeader) || !allowedSources.contains(source)) {
      return NqDryRunAuthResult.rejected(403, "SOURCE_DENIED", "dry-run source is denied");
    }
    final String pair = normalizePair(request.tenantId(), source);
    if (!allowedTenantSourcePairs.contains(pair)) {
      return NqDryRunAuthResult.rejected(
          403, "SOURCE_DENIED", "tenant/source pair is not allowlisted");
    }
    final String environmentName = environment.name();
    if (!allowedSourceEnvironmentPairs.contains(normalizePair(source, environmentName))) {
      return NqDryRunAuthResult.rejected(
          403,
          "SOURCE_ENVIRONMENT_NOT_AUTHORIZED",
          "dry-run source/environment pair is not allowlisted");
    }
    if (!allowedTenantEnvironmentPairs.contains(normalizePair(request.tenantId(), environmentName))) {
      return NqDryRunAuthResult.rejected(
          403,
          "TENANT_ENVIRONMENT_MISMATCH",
          "dry-run tenant/environment pair is not allowlisted");
    }
    if (!allowedTenantSourceEnvironmentTriples.contains(
        normalizeTriple(request.tenantId(), source, environmentName))) {
      return NqDryRunAuthResult.rejected(
          403,
          "ENVIRONMENT_NOT_AUTHORIZED",
          "dry-run tenant/source/environment binding is not allowlisted");
    }
    final String replayKey =
        normalizePair(request.tenantId(), source)
            + "::"
            + value(request.path())
            + "::"
            + value(request.nonce())
            + "::"
            + value(request.requestId());
    final Instant replayExpiresAt = now(request.now()).plus(maxClockSkew.multipliedBy(2));
    if (!replayGuard.markIfAbsent(replayKey, replayExpiresAt)) {
      return NqDryRunAuthResult.rejected(409, "NONCE_REPLAY", "dry-run nonce replay detected");
    }
    return NqDryRunAuthResult.success();
  }

  /**
   * 在 JSON 解析前判断 raw payload 是否超过 dry-run endpoint 上限。
   *
   * <p>payload cap 属于安全 gate，必须早于 DTO 反序列化生效；否则超大或畸形 JSON 会被映射成普通
   * policy error，无法向调用方稳定返回 {@code PAYLOAD_TOO_LARGE}。该方法只读取 raw body 和
   * Content-Length，不解析业务字段，也不会写 nonce，因此不会改变 HMAC / replay 校验语义。
   *
   * @param rawBody 原始 request body；为空时按 0 字节处理。
   * @param contentLength servlet 报告的 Content-Length；未知或负值时只依赖 raw body UTF-8 长度。
   * @return true 表示超过配置上限，调用方应 fail-closed。
   */
  public boolean isPayloadTooLarge(final String rawBody, final long contentLength) {
    return contentLength > maxPayloadBytes || utf8Size(rawBody) > maxPayloadBytes;
  }

  /**
   * 生成 dry-run 签名材料。
   *
   * <p>签名材料固定为 method/path/source/tenant/environment/requestId/traceId/timestamp/nonce/schemaVersion/bodySha256，
   * 其中 source 使用 request 的 wire-level 精确值，body 使用 SHA-256 hash 而非原文，避免签名工具或日志误带 raw
   * body。
   *
   * @param request dry-run auth request。
   * @return 稳定签名材料。
   */
  public static String signatureMaterial(final NqDryRunAuthRequest request) {
    return String.join(
        "\n",
        value(request.method()).toUpperCase(Locale.ROOT),
        value(request.path()),
        wireValue(request.sourceSystem()),
        value(request.tenantId()),
        value(request.environment()),
        value(request.requestId()),
        value(request.traceId()),
        value(request.timestampHeader()),
        value(request.nonce()),
        value(request.schemaVersion()),
        sha256Hex(value(request.rawBody())));
  }

  /**
   * 计算 HMAC-SHA256 hex。
   *
   * @param secret HMAC secret。
   * @param material 签名材料。
   * @return hex 编码签名。
   */
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
    } catch (final NoSuchAlgorithmException | InvalidKeyException error) {
      throw new IllegalStateException("HMAC-SHA256 not available", error);
    }
  }

  private boolean verifySignature(final NqDryRunAuthRequest request) {
    final String expected = hmacSha256Hex(secret, signatureMaterial(request));
    final String actual = value(request.signature()).trim().toLowerCase(Locale.ROOT);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private static TimestampCheck parseTimestamp(
      final String value, final Instant now, final Duration maxClockSkew) {
    if (isBlank(value) || !value.endsWith("Z")) {
      return new TimestampCheck(TimestampStatus.INVALID);
    }
    final Instant parsed;
    try {
      parsed = Instant.parse(value);
    } catch (final RuntimeException ignored) {
      return new TimestampCheck(TimestampStatus.INVALID);
    }
    final Instant anchor = now(now);
    final Duration skew = maxClockSkew == null ? Duration.ofMinutes(5) : maxClockSkew;
    if (parsed.isBefore(anchor.minus(skew)) || parsed.isAfter(anchor.plus(skew))) {
      return new TimestampCheck(TimestampStatus.OUT_OF_WINDOW);
    }
    return new TimestampCheck(TimestampStatus.OK);
  }

  private static String sha256Hex(final String value) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(hash.length * 2);
      for (final byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 not available", error);
    }
  }

  private static boolean sameTenant(final String authenticatedTenant, final String bodyTenant) {
    return !isBlank(authenticatedTenant)
        && !isBlank(bodyTenant)
        && authenticatedTenant.trim().equals(bodyTenant.trim());
  }

  private static String normalizePairConfig(final String configuredPair) {
    final String[] parts = configuredPair.contains("::")
        ? configuredPair.split("::", 2)
        : configuredPair.split(":", 2);
    if (parts.length != 2) {
      return "";
    }
    return value(parts[0]).trim() + "::" + normalizeConfiguredSource(parts[1]);
  }

  private static String normalizePair(final String tenantId, final String source) {
    return value(tenantId).trim() + "::" + wireValue(source);
  }

  private static String normalizeTriple(
      final String tenantId, final String source, final String environment) {
    return value(tenantId).trim() + "::" + wireValue(source) + "::" + wireValue(environment);
  }

  private static Set<String> normalizeConfiguredBindings(final Set<String> configuredBindings) {
    return configuredBindings == null
        ? Set.of()
        : configuredBindings.stream()
            .filter(binding -> !isBlank(binding))
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalizeConfiguredSource(final String value) {
    return value(value).trim();
  }

  private static String wireValue(final String value) {
    return value == null ? "" : value;
  }

  private static long utf8Size(final String value) {
    return value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static Instant now(final Instant value) {
    return value == null ? Instant.now() : value;
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  private static String value(final String value) {
    return value == null ? "" : value;
  }

  private enum TimestampStatus {
    OK,
    INVALID,
    OUT_OF_WINDOW
  }

  private record TimestampCheck(TimestampStatus status) {}
}
