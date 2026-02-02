package com.guidinglight.decisionhub.api;

import com.guidinglight.decisionhub.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthPingController {

  @GetMapping("/_ping")
  public ApiResponse<String> ping(HttpServletRequest req) {
    Object traceId = req.getAttribute(TraceIdFilter.TRACE_HEADER);
    return ApiResponse.ok("ok", traceId == null ? null : String.valueOf(traceId));
  }
}
