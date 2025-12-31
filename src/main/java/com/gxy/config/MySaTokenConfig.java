package com.gxy.config;

import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import cn.dev33.satoken.interceptor.SaInterceptor;

@Configuration
public class MySaTokenConfig implements WebMvcConfigurer {
    // 注册sa-token的拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch("/api/auth/**","/api/home/**" )
                    .notMatch("/api/farmstays/search")
                    .notMatchMethod("OPTIONS")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
