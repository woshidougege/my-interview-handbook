package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务表
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_scheduled_chat_task")
public class ScheduledChatTaskEntity extends BaseEntity {

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 关联工作空间ID
     */
    private Long workspaceId;

    /**
     * 关联对话任务ID
     */
    private Long chatTaskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 对话提示词
     */
    private String prompt;

    /**
     * 状态: 1启用 0禁用
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
}