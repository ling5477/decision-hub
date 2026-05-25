package com.guidinglight.decisionhub.api.research;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Stage1：创建 ResearchRun 的请求体。 */
public final class CreateResearchRunRequest {

  @NotBlank(message = "topic must not be blank")
  private String topic;

  private Map<String, Object> payloadJson;

  /** Topic 自然语言或结构化主题 key。 */
  public String getTopic() {
    return topic;
  }

  public void setTopic(final String topic) {
    this.topic = topic;
  }

  /** 入参快照，可携带 hint/context 等结构化字段。 */
  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(final Map<String, Object> payloadJson) {
    this.payloadJson = payloadJson;
  }
}
