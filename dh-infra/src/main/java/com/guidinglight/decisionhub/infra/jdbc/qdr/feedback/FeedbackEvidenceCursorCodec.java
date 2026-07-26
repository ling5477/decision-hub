package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery.Cursor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/** Internal codec for typed B3 cursors; it is not an HTTP or public-token protocol. */
public final class FeedbackEvidenceCursorCodec {

  /** Encodes fixed-shape cursor fields with Base64URL and no padding. */
  public String encode(final Cursor cursor) {
    final Cursor checked = java.util.Objects.requireNonNull(cursor, "cursor");
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(
            String.join(
                    "\n",
                    Integer.toString(checked.version()),
                    checked.tenantId(),
                    checked.environment().name(),
                    checked.filterFingerprint(),
                    checked.lastObservedAt().toString(),
                    checked.lastAttributionId())
                .getBytes(StandardCharsets.UTF_8));
  }

  /** Decodes a fixed-shape cursor and lets its typed constructor reject malformed fields. */
  public Cursor decode(final String encoded) {
    try {
      final String[] fields =
          new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\n", -1);
      if (fields.length != 6) {
        throw new IllegalArgumentException("cursor field count");
      }
      return new Cursor(
          Integer.parseInt(fields[0]),
          fields[1],
          FeedbackEnvironment.valueOf(fields[2]),
          fields[3],
          Instant.parse(fields[4]),
          fields[5]);
    } catch (final IllegalArgumentException error) {
      throw new IllegalArgumentException("CURSOR_INVALID", error);
    }
  }
}
