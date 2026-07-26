package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Internal-only, tenant/environment-bound historical feedback evidence query. */
public record HistoricalFeedbackEvidenceQuery(
    String tenantId,
    FeedbackEnvironment environment,
    Instant fromObservedAt,
    Instant toObservedAt,
    Integer pageSize,
    Cursor cursor,
    String decisionId,
    String traceId,
    String policyId,
    String policyVersion,
    FeedbackStatus attributionStatus,
    ObservedDecisionOutcome outcomeStatus,
    ReferenceType referenceType) {

  /** Stable keyset ordering version included in each cursor fingerprint. */
  public static final String ORDERING_VERSION = "OBSERVED_AT_DESC_ATTRIBUTION_ID_DESC_V1";
  /** Default bounded page size. */
  public static final int DEFAULT_PAGE_SIZE = 50;
  /** Hard bounded page size. */
  public static final int MAX_PAGE_SIZE = 100;
  private static final Duration MAX_RANGE = Duration.ofDays(90);
  private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");

  /** Rejects unsafe/unbounded filters before the query port can reach JDBC. */
  public HistoricalFeedbackEvidenceQuery {
    tenantId = text(tenantId, "tenantId");
    environment = Objects.requireNonNull(environment, "environment");
    fromObservedAt = Objects.requireNonNull(fromObservedAt, "fromObservedAt");
    toObservedAt = Objects.requireNonNull(toObservedAt, "toObservedAt");
    if (fromObservedAt.isAfter(toObservedAt)) {
      throw invalid(ValidationCode.INVALID_TIME_RANGE);
    }
    if (Duration.between(fromObservedAt, toObservedAt).compareTo(MAX_RANGE) > 0) {
      throw invalid(ValidationCode.TIME_RANGE_EXCEEDED);
    }
    if (pageSize != null && (pageSize < 1 || pageSize > MAX_PAGE_SIZE)) {
      throw invalid(ValidationCode.INVALID_PAGE_SIZE);
    }
    decisionId = optionalText(decisionId, "decisionId");
    traceId = optionalText(traceId, "traceId");
    policyId = optionalText(policyId, "policyId");
    policyVersion = optionalText(policyVersion, "policyVersion");
    if (cursor != null) {
      cursor.verifyAgainst(
          tenantId,
          environment,
          fingerprint(
              tenantId,
              environment,
              fromObservedAt,
              toObservedAt,
              decisionId,
              traceId,
              policyId,
              policyVersion,
              attributionStatus,
              outcomeStatus,
              referenceType));
    }
  }

  /** Returns the null-safe bounded page size. */
  public int effectivePageSize() {
    return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
  }

  /** Computes a deterministic, locale-independent binding for scope and every supported filter. */
  public String filterFingerprint() {
    return fingerprint(
        tenantId,
        environment,
        fromObservedAt,
        toObservedAt,
        decisionId,
        traceId,
        policyId,
        policyVersion,
        attributionStatus,
        outcomeStatus,
        referenceType);
  }

  private static String fingerprint(
      final String tenantId,
      final FeedbackEnvironment environment,
      final Instant fromObservedAt,
      final Instant toObservedAt,
      final String decisionId,
      final String traceId,
      final String policyId,
      final String policyVersion,
      final FeedbackStatus attributionStatus,
      final ObservedDecisionOutcome outcomeStatus,
      final ReferenceType referenceType) {
    final StringBuilder canonical = new StringBuilder(ORDERING_VERSION);
    append(canonical, tenantId);
    append(canonical, environment.name());
    append(canonical, fromObservedAt.toString());
    append(canonical, toObservedAt.toString());
    append(canonical, decisionId);
    append(canonical, traceId);
    append(canonical, policyId);
    append(canonical, policyVersion);
    append(canonical, name(attributionStatus));
    append(canonical, name(outcomeStatus));
    append(canonical, name(referenceType));
    return sha256(canonical.toString());
  }

  /** Typed internal continuation cursor; it is not a public token protocol. */
  public record Cursor(
      int version,
      String tenantId,
      FeedbackEnvironment environment,
      String filterFingerprint,
      Instant lastObservedAt,
      String lastAttributionId) {

    /** Validates cursor shape independently of any concrete query. */
    public Cursor {
      if (version != 1) {
        throw invalid(ValidationCode.CURSOR_INVALID);
      }
      tenantId = text(tenantId, "cursor.tenantId");
      environment = Objects.requireNonNull(environment, "cursor.environment");
      filterFingerprint = hash(filterFingerprint, "cursor.filterFingerprint");
      lastObservedAt = Objects.requireNonNull(lastObservedAt, "cursor.lastObservedAt");
      lastAttributionId = hash(lastAttributionId, "cursor.lastAttributionId");
    }

    private void verifyAgainst(
        final String requestedTenant,
        final FeedbackEnvironment requestedEnvironment,
        final String requestedFingerprint) {
      if (!tenantId.equals(requestedTenant) || environment != requestedEnvironment) {
        throw invalid(ValidationCode.CURSOR_SCOPE_MISMATCH);
      }
      if (!filterFingerprint.equals(requestedFingerprint)) {
        throw invalid(ValidationCode.CURSOR_FILTER_MISMATCH);
      }
    }
  }

  /** Stable validation categories exposed only through the internal use-case contract. */
  public enum ValidationCode {
    INVALID_TIME_RANGE,
    TIME_RANGE_EXCEEDED,
    INVALID_PAGE_SIZE,
    CURSOR_INVALID,
    CURSOR_SCOPE_MISMATCH,
    CURSOR_FILTER_MISMATCH
  }

  /** Carries no raw input value, cursor contents, SQL, or connection details. */
  public static final class ValidationException extends IllegalArgumentException {
    private final ValidationCode code;

    private ValidationException(final ValidationCode code) {
      super(Objects.requireNonNull(code, "code").name());
      this.code = code;
    }

    /** Returns the stable internal validation category. */
    public ValidationCode code() {
      return code;
    }
  }

  private static ValidationException invalid(final ValidationCode code) {
    return new ValidationException(code);
  }

  private static String optionalText(final String value, final String field) {
    return value == null ? null : text(value, field);
  }

  private static String text(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty() || checked.length() > 128) {
      throw invalid(ValidationCode.CURSOR_INVALID);
    }
    return checked;
  }

  private static String hash(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim().toLowerCase(Locale.ROOT);
    if (!HASH.matcher(checked).matches()) {
      throw invalid(ValidationCode.CURSOR_INVALID);
    }
    return checked;
  }

  private static String name(final Enum<?> value) {
    return value == null ? "" : value.name();
  }

  private static void append(final StringBuilder target, final String value) {
    final String checked = value == null ? "" : value;
    target.append('|').append(checked.length()).append(':').append(checked);
  }

  private static String sha256(final String value) {
    try {
      return java.util.HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 must be available", error);
    }
  }
}
