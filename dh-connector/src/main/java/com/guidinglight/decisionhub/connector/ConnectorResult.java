package com.guidinglight.decisionhub.connector;

import java.util.List;
import java.util.Map;

public record ConnectorResult(List<Map<String, Object>> items, Map<String, Object> meta) {}
