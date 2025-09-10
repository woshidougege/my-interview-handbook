package com.noah.superagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 配置
 * 现代化API文档生成，支持OpenAPI 3.0规范
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:super-agent}")
    private String applicationName;

    @Value("${server.servlet.context-path:/super-agent}")
    private String contextPath;

    @Value("${server.port:8081}")
    private String serverPort;

    /**
     * OpenAPI 3.0 全局配置
     * SpringDoc会自动按照@Tag注解进行分组，无需额外GroupedOpenApi配置
     * 包扫描和路径过滤在application.yml中配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Super Agent API 接口文档")
                        .description("## 🚀 Super Agent 用户管理和计费平台\n\n" +
                                "### 📋 系统功能\n" +
                                "- **用户管理**：用户注册、登录、信息管理\n" +
                                "- **工作空间管理**：多租户工作空间支持\n" +
                                "- **积分管理**：用户积分充值、消费、查询\n" +
                                "- **资源使用**：Token、功能、媒体等资源消费记录\n" +
                                "- **支付系统**：支持微信支付、支付宝支付\n" +
                                "- **任务管理**：智能体任务创建和管理\n\n" +
                                "### 🔗 相关链接\n" +
                                "- **监控页面**：[Druid监控](http://localhost:8081/super-agent/druid)\n" +
                                "- **项目仓库**：[GitHub](https://github.com/your-org/super-agent)\n\n" +
                                "### 📞 联系方式\n" +
                                "如有问题请联系开发团队：dev@noah.com")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@noah.com")
                                .url("https://github.com/your-org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + contextPath)
                                .description("开发环境"),
                        new Server()
                                .url("https://api.example.com" + contextPath)
                                .description("生产环境")
                ));
    }

}
