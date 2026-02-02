package com.guidinglight.decisionhub.knowledge;

import java.util.Map;

public record Evidence(String source, String title, String snippet, String uri, Map<String, Object> meta) {}
