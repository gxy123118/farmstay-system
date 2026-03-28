package com.gxy.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxy.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一异常处理。
 * 普通接口返回 ApiResponse，SSE 接口返回 event-stream 错误事件。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: {}", ex.getMessage());
        if (isSseRequest(request)) {
            return sseError(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
        }
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValidationException(Exception ex, HttpServletRequest request) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            message = methodArgumentNotValidException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException bindException) {
            message = bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("参数异常: {}", message);
        if (isSseRequest(request)) {
            return sseError(HttpStatus.BAD_REQUEST, 400, message);
        }
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(NotLoginException.class)
    public Object handleNotLogin(NotLoginException ex, HttpServletRequest request) {
        log.warn("未登录或登录态失效: {}", ex.getMessage());
        if (isSseRequest(request)) {
            return sseError(HttpStatus.UNAUTHORIZED, 401, "登录已失效，请重新登录");
        }
        return new ApiResponse<>(401, "登录已失效，请重新登录", null);
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常", ex);
        if (isSseRequest(request)) {
            return sseError(HttpStatus.INTERNAL_SERVER_ERROR, 500, "系统繁忙，请稍后重试");
        }
        return ApiResponse.fail("系统繁忙，请稍后重试");
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request == null ? null : request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private ResponseEntity<String> sseError(HttpStatus status, int code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "error");
        payload.put("code", code);
        payload.put("message", message);
        String body = "event:error\ndata:" + toJson(payload) + "\n\n";
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"code\":500,\"message\":\"系统繁忙，请稍后重试\"}";
        }
    }
}
