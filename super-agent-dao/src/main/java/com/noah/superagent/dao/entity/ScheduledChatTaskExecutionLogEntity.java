package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 定时对话任务执行日志实体类
 * <p>
 * 对应数据库表: t_scheduled_chat_task_execution_log
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "t_scheduled_chat_task_execution_log")
public class ScheduledChatTaskExecutionLogEntity extends BaseEntity {

    /**
     * 定时任务ID
     */
    private Long taskId;

    /**
     * 对话任务ID
     */
    private Long chatTaskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 执行开始时间
     */
    private Date startTime;

    /**
     * 执行结束时间
     */
    private Date endTime;

    /**
     * 执行状态: 1成功 0失败
     */
    private Integer executionStatus;

    /**
     * 执行结果
     */
    private String executionResult;

    /**
     * 执行耗时(毫秒)
     */
    private Long duration;

    /**
     * 错误信息
     */
    private String errorMessage;
}