package com.thesis.session_defense.config;

import com.thesis.session_defense.interceptor.SessionSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SessionSecurityInterceptor sessionSecurityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionSecurityInterceptor)
                // 拦截所有以 /api/ 开头的请求（黑名单检查对所有 API 生效）
                .addPathPatterns("/api/**");
    }
}
