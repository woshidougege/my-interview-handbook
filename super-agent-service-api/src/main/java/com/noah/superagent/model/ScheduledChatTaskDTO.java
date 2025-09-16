package com.noah.superagent.model;

import com.noah.superagent.common.dto.ScheduleConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务DTO
 * 对应数据库表：t_scheduled_chat_task
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduledChatTaskDTO extends BaseDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作空间ID
     */
    private Long workspaceId;

    /**
     * 关联的对话任务ID
     */
    private Long chatTaskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * Cron表达式
     */
    @Deprecated
    private String cronExpression;

    /**
     * 任务调度配置
     */
    private ScheduleConfig scheduleConfig;

    /**
     * 对话提示词
     */
    private String prompt;

    /**
     * 任务状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 上次执行时间
     */
    private Date lastExecutionTime;

    /**
     * 下次执行时间
     */
    private Date nextExecutionTime;

    /**
     * 任务类型: 0-一次性任务 1-可重复任务
     */
    private Integer taskType;
}