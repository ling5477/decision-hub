package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** B3 request bounds and cursor binding unit tests. */
class HistoricalFeedbackEvidenceReadServiceTest {

  private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-07-31T00:00:00Z");

  @Test
  void appliesDefaultAndHardPageBoundsAndTimeRangeBound() {
    assertEquals(50, query(null).effectivePageSize());
    assertEquals(1, query(1).effectivePageSize());
    assertEquals(100, query(100).effectivePageSize());
    assertThrows(HistoricalFeedbackEvidenceQuery.ValidationException.class, () -> query(0));
    assertThrows(HistoricalFeedbackEvidenceQuery.ValidationException.class, () -> query(101));
    assertThrows(HistoricalFeedbackEvidenceQuery.ValidationException.class, () -> new HistoricalFeedbackEvidenceQuery("tenant-a", FeedbackEnvironment.DEV, TO, FROM, 1, null, null, null, null, null, null, null, null));
    assertThrows(HistoricalFeedbackEvidenceQuery.ValidationException.class, () -> new HistoricalFeedbackEvidenceQuery("tenant-a", FeedbackEnvironment.DEV, FROM, TO.plusSeconds(90L * 24 * 60 * 60 + 1), 1, null, null, null, null, null, null, null, null));
  }

  @Test
  void rejectsCursorScopeAndFilterMismatchBeforeCallingPort() {
    final HistoricalFeedbackEvidenceQuery source = query(50);
    final var cursor = new HistoricalFeedbackEvidenceQuery.Cursor(1, "tenant-a", FeedbackEnvironment.DEV, source.filterFingerprint(), TO, hash());
    final AtomicBoolean called = new AtomicBoolean();
    final HistoricalFeedbackEvidenceReadService service = new HistoricalFeedbackEvidenceReadService(q -> { called.set(true); return HistoricalFeedbackEvidencePage.terminal(java.util.List.of()); });
    assertThrows(HistoricalFeedbackEvidenceQuery.ValidationException.class, () -> new HistoricalFeedbackEvidenceQuery("tenant-b", FeedbackEnvironment.DEV, FROM, TO, 50, cursor, null, null, null, null, null, null, null));
    assertFalse(called.get());
    assertTrue(service.read(new HistoricalFeedbackEvidenceQuery("tenant-a", FeedbackEnvironment.DEV, FROM, TO, 50, cursor, null, null, null, null, null, null, null)).items().isEmpty());
    assertTrue(called.get());
  }

  private static HistoricalFeedbackEvidenceQuery query(final Integer pageSize) {
    return new HistoricalFeedbackEvidenceQuery("tenant-a", FeedbackEnvironment.DEV, FROM, TO, pageSize, null, null, null, null, null, null, null, null);
  }

  private static String hash() { return "a".repeat(64); }
}
