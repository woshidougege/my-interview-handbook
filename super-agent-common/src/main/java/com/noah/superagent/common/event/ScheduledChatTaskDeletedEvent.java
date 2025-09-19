package com.noah.superagent.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 定时聊天任务删除事件
 * <p>
 * 当用户删除定时聊天任务时发布此事件，用于取消已安排的调度任务
 *
 * @author System
 * @since 1.0.0
 */
@Getter
public class ScheduledChatTaskDeletedEvent extends ApplicationEvent {

    /**
     * 任务ID
     */
    private final Long taskId;

    public ScheduledChatTaskDeletedEvent(Object source, Long taskId) {
        super(source);
        this.taskId = taskId;
    }
}
