package com.guidinglight.decisionhub.connector;

import java.util.Map;

public interface Connector {
  String type();
  ConnectorResult fetch(ConnectorRequest request);

  record ConnectorRequest(String query, Map<String, Object> options) {}
}
