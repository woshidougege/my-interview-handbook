package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS跨域配置属性类
 * 简化设计：只支持测试环境和生产环境两种模式
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "super-agent.cors")
public class CorsProperties {

    /**
     * 环境模式
     * test: 测试环境，允许所有来源访问
     * prod: 生产环境，使用可配置的允许源列表
     */
    private String mode = "test";

    /**
     * 允许的访问源列表（仅在mode=prod时生效）
     * 默认只允许本地访问，可通过环境变量自定义
     * 格式：逗号分隔的URL列表，如：http://localhost:*,https://your-domain.com
     */
    private String allowedOrigins = "http://localhost:*,http://127.0.0.1:*";
}
