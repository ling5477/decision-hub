package com.guidinglight.decisionhub.api.legacy.run;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.usecase.run.RunService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy 多模型平台 Run REST 入口。
 *
 * <p>Stage1-CLOSE：从 {@code /runs} 改路径到 {@code /legacy/runs}，避免与新链路 {@code /api/ai/research-runs}
 * 混淆；同时整体标记 @Deprecated，等 Stage2 接通真实 NQ 事件后再删除。
 *
 * @deprecated Stage1-CLOSE：旧多模型平台入口；新链路使用
 *     {@link com.guidinglight.decisionhub.api.research.ResearchRunController}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
@SuppressWarnings("deprecation")
@RestController
@RequestMapping("/legacy/runs")
public class RunController {

  private final RunService runService;

  public RunController(final RunService runService) {
    this.runService = runService;
  }

  @PostMapping
  public ApiResponse<RunView> create(
      @Valid @RequestBody final CreateRunRequest req, final HttpServletRequest httpReq) {
    final String tenantId = "t-default";
    final Run run = runService.create(tenantId, req.getQuestion(), req.getConfigSnapshot());
    final String traceId = (String) httpReq.getAttribute(TraceIdFilter.TRACE_HEADER);
    return ApiResponse.ok(toView(run), traceId);
  }

  @GetMapping("/{runId}")
  public ApiResponse<RunView> get(
      @PathVariable final String runId, final HttpServletRequest httpReq) {
    final Run run = runService.get(runId);
    final String traceId = (String) httpReq.getAttribute(TraceIdFilter.TRACE_HEADER);
    return ApiResponse.ok(toView(run), traceId);
  }

  private static RunView toView(final Run r) {
    return new RunView(
        r.getRunId(),
        r.getTenantId(),
        r.getStatus(),
        r.getQuestion(),
        r.getConfigSnapshot(),
        r.getCreatedAt(),
        r.getUpdatedAt());
  }
}
