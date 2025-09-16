package com.noah.superagent.convert;

import com.noah.superagent.common.dto.ScheduleConfig;
import com.noah.superagent.common.util.CronExpressionUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * 定时对话任务映射辅助类
 * 提供各种映射转换的辅助方法
 */
@Component
public class ScheduledChatTaskMappingHelper {

    /**
     * 截取任务名称前10个字符
     *
     * @param taskName 原始任务名称
     * @return 截取后的任务名称
     */
    @Named("trimTaskName")
    public String trimTaskName(String taskName) {
        if (StringUtils.isBlank(taskName)) {
            return taskName;
        }

        if (taskName.length() > 10) {
            return taskName.substring(0, 10);
        }

        return taskName;
    }
    
    /**
     * 将ScheduleConfig转换为cron表达式
     *
     * @param scheduleConfig 调度配置
     * @return cron表达式
     */
    @Named("scheduleConfigToCron")
    public String scheduleConfigToCron(ScheduleConfig scheduleConfig) {
        if (scheduleConfig == null) {
            return null;
        }
        return CronExpressionUtil.generateCronExpression(scheduleConfig);
    }
    
    /**
     * 将cron表达式转换为ScheduleConfig
     *
     * @param cronExpression cron表达式
     * @return 调度配置
     */
    @Named("cronToScheduleConfig")
    public ScheduleConfig cronToScheduleConfig(String cronExpression) {
        if (StringUtils.isBlank(cronExpression)) {
            return null;
        }
        return CronExpressionUtil.parseCronExpression(cronExpression);
    }
}