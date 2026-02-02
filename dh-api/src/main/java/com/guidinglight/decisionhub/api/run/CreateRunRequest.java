package com.guidinglight.decisionhub.api.run;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class CreateRunRequest {

  @NotBlank(message = "question must not be blank")
  private String question;

  private Map<String, Object> configSnapshot;

  public String getQuestion() { return question; }
  public void setQuestion(String question) { this.question = question; }

  public Map<String, Object> getConfigSnapshot() { return configSnapshot; }
  public void setConfigSnapshot(Map<String, Object> configSnapshot) { this.configSnapshot = configSnapshot; }
}
