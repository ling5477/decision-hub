package com.guidinglight.decisionhub.providers;

import java.util.Map;

public record ModelOutput(String providerKey, String text, Map<String, Object> meta) {}
