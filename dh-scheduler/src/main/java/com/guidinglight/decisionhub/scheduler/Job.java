package com.guidinglight.decisionhub.scheduler;

import java.util.Map;

public interface Job {
  String name();
  JobResult execute(Map<String, Object> param);

  record JobResult(boolean success, String message) {}
}
