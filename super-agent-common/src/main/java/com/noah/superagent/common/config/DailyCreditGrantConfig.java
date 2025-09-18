package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 每日积分补发任务配置
 *
 * @author AI Assistant  
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "daily-credit-grant")
public class DailyCreditGrantConfig {

    /**
     * CRON表达式
     * 默认：每天凌晨00:00:00执行
     */
    private String cron = "0 0 0 * * ?";

    /**
     * 是否启用任务
     */
    private boolean enabled = true;
}
