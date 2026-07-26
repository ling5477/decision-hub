package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery.Cursor;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Typed internal cursor round-trip and malformed-input tests. */
class FeedbackEvidenceCursorCodecTest {

  @Test
  void roundTripsTypedCursorWithoutCreatingPublicTokenSemantics() {
    final Cursor cursor = new Cursor(1, "tenant-a", FeedbackEnvironment.TEST, "a".repeat(64), Instant.parse("2026-07-25T10:00:00Z"), "b".repeat(64));
    final FeedbackEvidenceCursorCodec codec = new FeedbackEvidenceCursorCodec();
    assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
  }

  @Test
  void rejectsMalformedCursor() {
    assertThatThrownBy(() -> new FeedbackEvidenceCursorCodec().decode("not-a-cursor"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("CURSOR_INVALID");
  }
}
