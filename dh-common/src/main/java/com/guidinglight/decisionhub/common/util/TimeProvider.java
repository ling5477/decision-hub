package com.guidinglight.decisionhub.common.util;

import java.time.Clock;
import java.time.Instant;

public final class TimeProvider {
  private static volatile Clock clock = Clock.systemUTC();

  private TimeProvider() {}

  public static Instant now() {
    return Instant.now(clock);
  }

  public static long nowMillis() {
    return now().toEpochMilli();
  }

  public static void useClock(Clock c) {
    clock = c;
  }
}
