package com.noah.superagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 启用Spring Boot官方的WebSocket注解支持
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注入ServerEndpointExporter，这个Bean会自动注册使用了@ServerEndpoint注解声明的WebSocket端点
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}