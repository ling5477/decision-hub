package com.guidinglight.decisionhub.api.legacy.run;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @deprecated Stage1-CLOSE：旧多模型平台请求体；新链路使用
 *     {@link com.guidinglight.decisionhub.api.research.CreateResearchRunRequest}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class CreateRunRequest {

  @NotBlank(message = "question must not be blank")
  private String question;

  private Map<String, Object> configSnapshot;

  public String getQuestion() {
    return question;
  }

  public void setQuestion(final String question) {
    this.question = question;
  }

  public Map<String, Object> getConfigSnapshot() {
    return configSnapshot;
  }

  public void setConfigSnapshot(final Map<String, Object> configSnapshot) {
    this.configSnapshot = configSnapshot;
  }
}
