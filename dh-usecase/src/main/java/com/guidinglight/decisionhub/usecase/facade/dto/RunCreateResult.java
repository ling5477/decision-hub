package com.guidinglight.decisionhub.usecase.facade.dto;

/**
 * 创建 Run 的输出结果。
 *
 * @deprecated Stage1-CLOSE：旧多模型平台 DTO，新链路使用
 *     {@code dh-api} 的 {@code ResearchRunView}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class RunCreateResult {

    /** Run ID。 */
    private String runId;

    /** 状态。 */
    private String status;

    public RunCreateResult() {}

    public RunCreateResult(String runId, String status) {
        this.runId = runId;
        this.status = status;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
