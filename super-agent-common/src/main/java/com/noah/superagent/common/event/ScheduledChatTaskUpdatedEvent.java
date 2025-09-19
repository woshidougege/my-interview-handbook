package com.noah.superagent.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * 定时聊天任务更新事件
 * <p>
 * 当用户更新定时聊天任务时发布此事件，用于重新安排调度任务
 *
 * @author System
 * @since 1.0.0
 */
@Getter
public class ScheduledChatTaskUpdatedEvent extends ApplicationEvent {

    /**
     * 任务ID
     */
    private final Long taskId;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 工作空间ID
     */
    private final Long workspaceId;

    /**
     * 任务名称
     */
    private final String taskName;

    /**
     * 提示词
     */
    private final String prompt;

    /**
     * 任务类型：0-一次性任务 1-可重复任务
     */
    private final Integer taskType;

    /**
     * 下次执行时间
     */
    private final Instant nextExecutionTime;

    /**
     * CRON表达式（用于重复任务）
     */
    private final String cronExpression;

    public ScheduledChatTaskUpdatedEvent(Object source, 
                                       Long taskId, 
                                       Long userId, 
                                       Long workspaceId,
                                       String taskName,
                                       String prompt,
                                       Integer taskType,
                                       Instant nextExecutionTime,
                                       String cronExpression) {
        super(source);
        this.taskId = taskId;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.taskName = taskName;
        this.prompt = prompt;
        this.taskType = taskType;
        this.nextExecutionTime = nextExecutionTime;
        this.cronExpression = cronExpression;
    }
}
