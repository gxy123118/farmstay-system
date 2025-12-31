package com.gxy.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.gxy.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 捕获常见业务异常并统一返回 ApiResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidationException(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) ex).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException) {
            message = ((BindException) ex).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("参数异常: {}", message);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException ex) {
        log.warn("未登录或登录态失效: {}", ex.getMessage());
        return new ApiResponse<>(401, "登录已失效，请重新登录", null);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ApiResponse.fail("系统繁忙，请稍后重试");
    }
}
