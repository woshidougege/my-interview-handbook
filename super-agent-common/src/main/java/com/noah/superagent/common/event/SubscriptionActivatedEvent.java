package com.noah.superagent.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 订阅激活事件
 * <p>
 * 当用户订阅被激活时发布此事件，用于安排到期任务
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class SubscriptionActivatedEvent extends ApplicationEvent {

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
     * 订阅到期时间
     */
    private final LocalDateTime endTime;

    public SubscriptionActivatedEvent(Object source, Long subscriptionId, Long userId, 
                                    Long planId, String planName, LocalDateTime endTime) {
        super(source);
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planId = planId;
        this.planName = planName;
        this.endTime = endTime;
    }
}
