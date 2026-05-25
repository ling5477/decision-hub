package com.guidinglight.decisionhub.usecase.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * 创建 Run 的输入参数。
 *
 * @deprecated Stage1-CLOSE：旧多模型平台 DTO，新链路使用
 *     {@code dh-api} 的 {@code CreateResearchRunRequest}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunCreateCommand {

    /** 租户 ID。 */
    private String tenantId;

    /** 决策主题。 */
    private String topic;

    /** 可选项。 */
    private Map<String, Object> options;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }
}
