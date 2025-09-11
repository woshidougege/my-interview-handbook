package com.noah.superagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.noah.superagent.config.OpenApiDocProperties;
import com.noah.superagent.config.WxPayProperties;

/**
 * Super Agent 用户管理和计费平台 - 启动类
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.noah.superagent")
@EnableConfigurationProperties({OpenApiDocProperties.class, WxPayProperties.class})
public class SuperAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperAgentApplication.class, args);
    }
}
