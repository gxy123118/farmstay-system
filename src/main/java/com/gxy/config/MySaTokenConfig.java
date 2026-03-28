package com.gxy.config;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录拦截配置。
 * 对 SSE 场景下的异步和错误分发直接放行，避免二次分发时再次触发线程上下文异常。
 */
@Configuration
public class MySaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                DispatcherType dispatcherType = request.getDispatcherType();
                if (dispatcherType == DispatcherType.ASYNC
                        || dispatcherType == DispatcherType.ERROR
                        || dispatcherType == DispatcherType.FORWARD) {
                    return true;
                }
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }
                if (isPublicPath(request.getRequestURI())) {
                    return true;
                }
                StpUtil.checkLogin();
                return true;
            }
        }).addPathPatterns("/**");
    }

    private boolean isPublicPath(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.startsWith("/api/auth/")
                || uri.startsWith("/api/home/")
                || uri.equals("/api/account/recharges/alipay/notify")
                || uri.equals("/api/farmstays/search")
                || uri.startsWith("/api/farmstays/")
                || uri.equals("/error")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/v3/api-docs")
                || uri.startsWith("/v3/api-docs/");
    }
}
