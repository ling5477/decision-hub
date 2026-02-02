package com.guidinglight.decisionhub.security;

public interface TokenVerifier {
  AuthContext verify(String token);
}
