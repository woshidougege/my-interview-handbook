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
     * 截取任务名称前10个字符，如果任务名称为空则从prompt中截取
     *
     * @param taskName 原始任务名称
     * @return 截取后的任务名称
     */
    @Named("trimTaskName")
    public String trimTaskName(String taskName) {
        if (StringUtils.isBlank(taskName)) {
            return "未命名任务";
        }

        if (taskName.length() > 10) {
            return taskName.substring(0, 10);
        }

        return taskName;
    }
    
    /**
     * 从prompt生成任务名称的方法
     * 
     * @param prompt 提示词
     * @return 基于prompt生成的任务名称
     */
    @Named("generateTaskNameFromPrompt")
    public String generateTaskNameFromPrompt(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return "未命名任务";
        }
        
        // 从prompt中截取前10个字符作为任务名称
        if (prompt.length() > 10) {
            return prompt.substring(0, 10);
        }
        
        return prompt;
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