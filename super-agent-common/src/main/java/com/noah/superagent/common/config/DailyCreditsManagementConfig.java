package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 每日积分管理配置
 * <p>
 * 用于配置每日积分管理任务的执行时间和开关
 * 包含积分清理和积分补发的完整流程
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "daily-credits-management")
public class DailyCreditsManagementConfig {

    /**
     * CRON表达式，默认每天凌晨00:00:00执行
     * 先清理过期的每日积分，再为活跃用户补发新积分
     */
    private String cron = "0 0 0 * * ?";

    /**
     * 是否启用每日积分管理任务
     */
    private boolean enabled = true;
    
    /**
     * 任务描述信息
     */
    private String description = "每日积分管理任务：先清理过期的每日积分，再为活跃用户补发新的每日积分";
    
    /**
     * 是否启用积分清理功能
     */
    private boolean cleanupEnabled = true;
    
    /**
     * 是否启用积分补发功能  
     */
    private boolean grantEnabled = true;
}
