package com.noah.superagent.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单创建事件
 * <p>
 * 当支付订单被创建时发布此事件，用于安排超时检查任务
 * 
 * 事件流程：
 * 订单创建 → 发布此事件 → PaymentSchedulingTask监听 → 安排30分钟后检查任务
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class PaymentOrderCreatedEvent extends ApplicationEvent {

    /**
     * 订单号
     */
    private final String orderNo;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 支付金额
     */
    private final BigDecimal amount;

    /**
     * 订单过期时间
     */
    private final LocalDateTime expiredAt;

    /**
     * 套餐ID（可选）
     */
    private final Long planId;

    public PaymentOrderCreatedEvent(Object source, String orderNo, Long userId, 
                                  BigDecimal amount, LocalDateTime expiredAt, Long planId) {
        super(source);
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
        this.expiredAt = expiredAt;
        this.planId = planId;
    }
}
