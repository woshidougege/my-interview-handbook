package com.noah.superagent.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 第三方组件集成配置
 * 用于扫描和注册第三方JAR包中的Spring组件
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = {
    "com.norinrd.client"  // 扫描第三方SSO客户端组件
    // 未来可以在这里添加其他第三方包的扫描路径
})
public class ThirdPartyIntegrationConfig {
    
    /**
     * 配置说明：
     * - com.norinrd.client: 第三方SSO客户端控制器和服务类
     * 
     * 使用独立配置类的优势：
     * 1. 保持启动类简洁
     * 2. 第三方组件配置集中管理
     * 3. 便于条件化装配和环境控制
     * 4. 符合Spring Boot最佳实践
     */
    
}
