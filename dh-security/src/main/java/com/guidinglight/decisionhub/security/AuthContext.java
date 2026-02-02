package com.guidinglight.decisionhub.security;

import java.util.Set;

public record AuthContext(String userId, String tenantId, Set<String> roles) {}
