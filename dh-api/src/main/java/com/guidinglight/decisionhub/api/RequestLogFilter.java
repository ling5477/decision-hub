package com.guidinglight.decisionhub.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RequestLogFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long cost = System.currentTimeMillis() - start;
      Object traceId = request.getAttribute(TraceIdFilter.TRACE_HEADER);
      log.info("http {} {} status={} costMs={} traceId={}",
          request.getMethod(), request.getRequestURI(), response.getStatus(), cost, traceId);
    }
  }
}
