package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** QDR7 canonical safe request projection与domain-separated SHA-256；不会返回或持久化canonical bytes。 */
public final class DecisionDryRunRequestFingerprint {

  private static final String DOMAIN = IdempotencyAdmissionCommand.HASH_VERSION + "\n";

  /** 计算不含requestId/traceId/timestamp/nonce/signature/transport metadata的fingerprint。 */
  public String hash(final DecisionDryRunCommand command) {
    final DecisionDryRunCommand checked = Objects.requireNonNull(command, "command");
    final String canonical = canonicalJson(checked);
    try {
      final byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((DOMAIN + canonical).getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  private static String canonicalJson(final DecisionDryRunCommand command) {
    final DecisionDryRunContext context = Objects.requireNonNull(command.context(), "context");
    final List<String> capabilities = new ArrayList<>(command.forbiddenCapabilities());
    capabilities.sort(DecisionDryRunRequestFingerprint::compareCodePoints);
    final List<String> evidence = new ArrayList<>(context.evidenceRefs());
    final String verifiedEnvironment =
        command.executionScope() == null ? null : command.executionScope().environment().name();
    return "{\"decisionContext\":{"
        + field("capturedAt", context.capturedAt() == null ? null : context.capturedAt().toString())
        + ","
        + field("contextRef", context.contextRef())
        + ",\"evidenceRefs\":"
        + array(evidence)
        + ","
        + field("market", context.market())
        + ","
        + field("researchRef", context.researchRef())
        + ","
        + field("snapshotId", context.snapshotId())
        + ","
        + field("strategyRef", context.strategyRef())
        + ","
        + field("symbol", context.symbol())
        + ","
        + field("timeframe", context.timeframe())
        + "},\"dryRun\":"
        + command.dryRun()
        + ",\"forbiddenCapabilities\":"
        + array(capabilities)
        + ","
        + field("schemaVersion", command.schemaVersion())
        + ","
        + field("source", command.source())
        + ","
        + field("tenantId", command.tenantId())
        + (verifiedEnvironment == null ? "" : "," + field("environment", verifiedEnvironment))
        + "}";
  }

  private static String field(final String name, final String value) {
    return quote(name) + ":" + (value == null ? "null" : quote(value));
  }

  private static String array(final List<String> values) {
    return values.stream()
        .map(DecisionDryRunRequestFingerprint::quote)
        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
  }

  private static String quote(final String value) {
    final StringBuilder escaped = new StringBuilder("\"");
    for (int index = 0; index < value.length(); index++) {
      final char character = value.charAt(index);
      switch (character) {
        case '\"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (character < 0x20) {
            escaped.append(String.format("\\u%04x", (int) character));
          } else {
            escaped.append(character);
          }
        }
      }
    }
    return escaped.append('\"').toString();
  }

  /** 按Unicode code point逐项比较，避免UTF-16 surrogate顺序偏离冻结的canonical集合顺序。 */
  private static int compareCodePoints(final String left, final String right) {
    int leftIndex = 0;
    int rightIndex = 0;
    while (leftIndex < left.length() && rightIndex < right.length()) {
      final int leftCodePoint = left.codePointAt(leftIndex);
      final int rightCodePoint = right.codePointAt(rightIndex);
      if (leftCodePoint != rightCodePoint) {
        return Integer.compare(leftCodePoint, rightCodePoint);
      }
      leftIndex += Character.charCount(leftCodePoint);
      rightIndex += Character.charCount(rightCodePoint);
    }
    return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
  }
}
