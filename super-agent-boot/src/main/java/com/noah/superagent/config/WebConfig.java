package com.noah.superagent.config;

import com.noah.superagent.common.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 处理CORS跨域问题
 * 支持通过配置文件管理CORS设置，区分开发和生产环境
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    /**
     * 全局CORS配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsProperties.getEnabled()) {
            var mapping = registry.addMapping("/**")
                    .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
                    .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                    .allowCredentials(corsProperties.getAllowCredentials())
                    .maxAge(corsProperties.getMaxAge());

            // 优先使用 allowedOriginPatterns（支持通配符且可与认证同时使用）
            if (!corsProperties.getAllowedOriginPatterns().isEmpty()) {
                mapping.allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(new String[0]));
            } else if (!corsProperties.getAllowedOrigins().isEmpty()) {
                mapping.allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]));
            }
        }
    }

    /**
     * CORS过滤器配置
     * 提供更细粒度的CORS控制，支持通配符模式匹配
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        if (corsProperties.getEnabled()) {
            // 配置HTTP方法和请求头
            corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
            corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
            config.setAllowCredentials(corsProperties.getAllowCredentials());
            config.setMaxAge(corsProperties.getMaxAge());
            
            // 优先使用 allowedOriginPatterns（支持通配符且可与认证同时使用）
            if (!corsProperties.getAllowedOriginPatterns().isEmpty()) {
                corsProperties.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern);
            } else if (!corsProperties.getAllowedOrigins().isEmpty()) {
                corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
            } else {
                // 如果都没有配置，默认允许所有来源（但不能携带认证信息）
                config.addAllowedOrigin("*");
                config.setAllowCredentials(false);
            }
            
            source.registerCorsConfiguration("/**", config);
        }
        
        return new CorsFilter(source);
    }
}