package com.gxy.config;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.UserMapper;
import com.gxy.model.entity.User;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Sa-Token 登录拦截配置。
 * 对 SSE 场景下的异步和错误分发直接放行，避免二次分发时再次触发线程上下文异常。
 */
@Configuration
public class MySaTokenConfig implements WebMvcConfigurer {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final UserMapper userMapper;

    public MySaTokenConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Paths.get("uploads").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

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
                Long loginId = StpUtil.getLoginIdAsLong();
                User user = userMapper.selectById(loginId);
                if (user == null || !STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
                    StpUtil.logout(loginId);
                    throw new BusinessException("Account has been disabled");
                }
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
                || uri.startsWith("/uploads/")
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
