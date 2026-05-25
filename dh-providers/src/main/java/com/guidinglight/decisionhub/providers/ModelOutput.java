package com.guidinglight.decisionhub.providers;

import java.util.Map;

/** @deprecated Stage1-CLOSE：旧 ModelOutput 结构。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public record ModelOutput(String providerKey, String text, Map<String, Object> meta) {}
