package com.guidinglight.decisionhub.api.run;

import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.usecase.run.RunService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/runs")
public class RunController {

  private final RunService runService;

  public RunController(RunService runService) {
    this.runService = runService;
  }

  @PostMapping
  public ApiResponse<RunView> create(@Valid @RequestBody CreateRunRequest req, HttpServletRequest httpReq) {
    // v1: tenantId 先固定占位，后续由 dh-security 注入
    String tenantId = "t-default";
    Run run = runService.create(tenantId, req.getQuestion(), req.getConfigSnapshot());
    String traceId = (String) httpReq.getAttribute(com.guidinglight.decisionhub.api.TraceIdFilter.TRACE_HEADER);
    return ApiResponse.ok(toView(run), traceId);
  }

  @GetMapping("/{runId}")
  public ApiResponse<RunView> get(@PathVariable String runId, HttpServletRequest httpReq) {
    Run run = runService.get(runId);
    String traceId = (String) httpReq.getAttribute(com.guidinglight.decisionhub.api.TraceIdFilter.TRACE_HEADER);
    return ApiResponse.ok(toView(run), traceId);
  }

  private static RunView toView(Run r) {
    return new RunView(r.getRunId(), r.getTenantId(), r.getStatus(), r.getQuestion(), r.getConfigSnapshot(), r.getCreatedAt(), r.getUpdatedAt());
  }
}
