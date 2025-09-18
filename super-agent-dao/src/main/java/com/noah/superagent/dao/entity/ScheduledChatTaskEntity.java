package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务实体类
 * <p>
 * 对应数据库表: t_scheduled_chat_task
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "t_scheduled_chat_task")
public class ScheduledChatTaskEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(value = "user_id")
    private Long userId;

    /**
     * 工作空间ID
     */
    @Column(value = "workspace_id")
    private Long workspaceId;

    /**
     * 关联的对话任务ID
     */
    @Column(value = "chat_task_id")
    private Long chatTaskId;

    /**
     * 任务名称
     */
    @Column(value = "task_name")
    private String taskName;

    /**
     * Cron表达式
     */
    @Column(value = "cron_expression")
    @Deprecated
    private String cronExpression;

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
    @Column(value = "last_execution_time")
    private Date lastExecutionTime;

    /**
     * 下次执行时间
     */
    @Column(value = "next_execution_time")
    private Date nextExecutionTime;

    /**
     * 任务类型: 0-一次性任务 1-可重复任务
     */
    @Column(value = "task_type")
    private Integer taskType;

    
    /**
     * 任务调度配置（JSON格式存储）
     */
    @Column(value = "schedule_config")
    private String scheduleConfig;
}