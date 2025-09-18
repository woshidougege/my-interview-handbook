package com.noah.superagent.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 订阅延期事件
 * <p>
 * 当订阅时间被延长时发布此事件，用于重新安排到期任务
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class SubscriptionExtendedEvent extends ApplicationEvent {

    /**
     * 订阅ID
     */
    private final Long subscriptionId;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 套餐ID
     */
    private final Long planId;

    /**
     * 套餐名称
     */
    private final String planName;

    /**
     * 新的到期时间
     */
    private final LocalDateTime newEndTime;

    public SubscriptionExtendedEvent(Object source, Long subscriptionId, Long userId, 
                                   Long planId, String planName, LocalDateTime newEndTime) {
        super(source);
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planId = planId;
        this.planName = planName;
        this.newEndTime = newEndTime;
    }
}
