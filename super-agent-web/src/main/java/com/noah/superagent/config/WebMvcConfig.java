package com.noah.superagent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SSOInterceptor ssoInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        // 添加SSO拦截器，拦截所有请求，排除已配置的不需要认证的路径
        registry.addInterceptor(ssoInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/sso/**",
                        "/favicon.ico",
                        "/**/*.css",
                        "/**/*.html",
                        "/**/*.js",
                        "/swagger-resource",
                        "/v3/api-docs/**",
                        "/doc.html/**",
                        "/webjars/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/index.html",
                        "/chat.html",
                        "/super-agent/chat.html",
                        "/static/**",
                        "/ws/**",
                        "/v2/api-docs"
                );
    }
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/front/static/resources/bundle-main/static/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/ui/**").addResourceLocations("file:ui/");
        // 确保chat.html可以被直接访问
        registry.addResourceHandler("/chat.html")
                .addResourceLocations("classpath:/static/chat.html");
        registry.addResourceHandler("/ws/**")
                .addResourceLocations("classpath:/ws/**");
    }
}
