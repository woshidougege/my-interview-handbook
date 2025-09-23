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
            registry.addMapping("/**")
                    .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                    .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
                    .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                    .allowCredentials(corsProperties.getAllowCredentials())
                    .maxAge(corsProperties.getMaxAge());
        }
    }

    /**
     * CORS过滤器配置
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        if (corsProperties.getEnabled() && !corsProperties.getAllowedOrigins().isEmpty()) {
            // 只在启用CORS且配置了允许的源时才设置
            corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
            corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
            corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
            config.setAllowCredentials(corsProperties.getAllowCredentials());
            config.setMaxAge(corsProperties.getMaxAge());
            
            source.registerCorsConfiguration("/**", config);
        }
        
        return new CorsFilter(source);
    }
}