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
     * 全局CORS配置 - 根据环境模式自动配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var mapping = registry.addMapping("/**");
        
        if ("test".equals(corsProperties.getMode())) {
            // 🎯 测试环境：允许所有来源访问
            mapping.allowedMethods("*")
                   .allowedHeaders("*")
                   .allowedOriginPatterns("*")
                   .allowCredentials(true)
                   .maxAge(3600);
        } else if ("prod".equals(corsProperties.getMode())) {
            // 🎯 生产环境：使用配置的允许源列表
            String[] origins = corsProperties.getAllowedOrigins().split(",");
            mapping.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                   .allowedHeaders("*")
                   .allowedOriginPatterns(origins)  // 使用模式匹配支持通配符
                   .allowCredentials(true)
                   .maxAge(3600);
        }
    }

    /**
     * CORS过滤器 - 测试环境双重保险
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        if ("test".equals(corsProperties.getMode())) {
            // 🎯 测试环境：双重保险，确保CORS完全放开
            config.addAllowedOriginPattern("*");
            config.addAllowedMethod("*");
            config.addAllowedHeader("*");
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            
            source.registerCorsConfiguration("/**", config);
        }
        
        return new CorsFilter(source);
    }
}