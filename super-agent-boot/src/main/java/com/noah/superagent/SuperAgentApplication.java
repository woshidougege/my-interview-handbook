package com.noah.superagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Super Agent 用户管理和计费平台 - 启动类
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.noah.superagent")
public class SuperAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperAgentApplication.class, args);
        System.out.println("🚀 Super Agent Platform Started Successfully!");
    }
}
