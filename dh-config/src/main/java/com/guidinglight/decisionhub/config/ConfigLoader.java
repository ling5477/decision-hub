package com.guidinglight.decisionhub.config;

import java.util.Map;

public interface ConfigLoader {
  Map<String, Object> loadAll();
}
