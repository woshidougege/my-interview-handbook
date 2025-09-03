package com.noah.superagent.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import com.noah.superagent.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Flex 配置
 *
 * @author System
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    private final SnowflakeIdGenerator idGenerator;

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 开启审计功能
        AuditManager.setAuditEnable(true);
        
        // 设置SQL审计收集器
        AuditManager.setMessageCollector(auditMessage -> {
            System.out.println("SQL: " + auditMessage.getFullSql());
            System.out.println("Time: " + auditMessage.getElapsedTime() + " ms");
        });
    }

    /**
     * 注册雪花算法ID生成器
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator();
    }
}
