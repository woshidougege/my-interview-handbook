package com.noah.superagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.noah.superagent.config.ConfigurationConfig;
import com.noah.superagent.config.ThirdPartyIntegrationConfig;

/**
 * Super Agent 用户管理和计费平台 - 启动类
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.noah.superagent")
@Import({ConfigurationConfig.class, ThirdPartyIntegrationConfig.class})
public class SuperAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperAgentApplication.class, args);
    }
}
