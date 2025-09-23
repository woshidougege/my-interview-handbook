package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS跨域配置属性
 * 对应 application.yml 中的 super-agent.cors 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.cors")
public class CorsProperties {

    /**
     * 是否启用CORS跨域支持
     * 测试环境：true（默认）
     * 开发环境：true
     * 生产环境：false（nginx代理，同域访问）
     */
    private Boolean enabled = true;

    /**
     * 允许的源地址列表（精确匹配）
     * 开发环境示例：http://localhost:3000
     * 生产环境：不配置或为空
     * 注意：使用 "*" 时不能同时设置 allowCredentials=true
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 允许的源地址模式列表（模式匹配）
     * 支持通配符匹配，如：http://*.example.com:*
     * 测试环境默认：允许所有IP和端口 http://*:*, https://*:*
     * 可以与 allowCredentials=true 同时使用
     */
    private List<String> allowedOriginPatterns = List.of("http://*:*", "https://*:*");

    /**
     * 允许的HTTP方法
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 是否允许携带认证信息（如Cookie、Authorization头等）
     * 测试环境默认：true（支持完整认证流程）
     */
    private Boolean allowCredentials = true;

    /**
     * 预检请求的缓存时间（秒）
     */
    private Long maxAge = 3600L;
}
