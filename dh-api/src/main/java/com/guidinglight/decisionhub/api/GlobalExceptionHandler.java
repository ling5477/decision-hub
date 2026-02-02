package com.guidinglight.decisionhub.api;

import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.error.BizException;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BizException.class)
  public ResponseEntity<ApiResponse<Object>> handleBiz(BizException ex, HttpServletRequest req) {
    String traceId = traceId(req, ex.getTraceId());
    ApiResponse<Object> body = ApiResponse.fail(ex.getCode(), safeMsg(ex.getMessage()), traceId, ex.getDetail());
    return ResponseEntity.status(ex.getHttpStatus()).body(body);
  }

  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      MissingServletRequestParameterException.class,
      IllegalArgumentException.class
  })
  public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex, HttpServletRequest req) {
    String traceId = traceId(req, null);
    ApiResponse<Object> body = ApiResponse.fail(
        CommonErrorCodes.BAD_REQUEST.code(),
        safeMsg(ex.getMessage()),
        traceId,
        null
    );
    return ResponseEntity.status(CommonErrorCodes.BAD_REQUEST.httpStatus()).body(body);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ApiResponse<Object>> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
    String traceId = traceId(req, null);
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    String code;
    if (status.value() == 404) code = CommonErrorCodes.NOT_FOUND.code();
    else if (status.value() == 401) code = CommonErrorCodes.UNAUTHORIZED.code();
    else if (status.value() == 403) code = CommonErrorCodes.FORBIDDEN.code();
    else if (status.is4xxClientError()) code = CommonErrorCodes.BAD_REQUEST.code();
    else code = CommonErrorCodes.INTERNAL_ERROR.code();

    ApiResponse<Object> body = ApiResponse.fail(code, safeMsg(ex.getMessage()), traceId, null);
    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex, HttpServletRequest req) {
    String traceId = traceId(req, null);
    ApiResponse<Object> body = ApiResponse.fail(
        CommonErrorCodes.INTERNAL_ERROR.code(),
        "Internal error",
        traceId,
        null
    );
    return ResponseEntity.status(CommonErrorCodes.INTERNAL_ERROR.httpStatus()).body(body);
  }

  private static String traceId(HttpServletRequest req, String prefer) {
    if (prefer != null && !prefer.isBlank()) return prefer;
    Object v = req.getAttribute(TraceIdFilter.TRACE_HEADER);
    return v == null ? null : String.valueOf(v);
  }

  private static String safeMsg(String msg) {
    if (msg == null) return "error";
    String trimmed = msg.trim();
    if (trimmed.length() > 500) return trimmed.substring(0, 500);
    return trimmed;
  }
}
