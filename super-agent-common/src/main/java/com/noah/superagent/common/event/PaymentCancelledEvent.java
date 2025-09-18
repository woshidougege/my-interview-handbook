package com.noah.superagent.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付取消事件
 * <p>
 * 当用户主动取消订单时发布此事件，用于取消对应的超时检查任务
 * 
 * 事件流程：
 * 用户取消订单 → 发布此事件 → PaymentSchedulingTask监听 → 取消对应的超时检查任务
 * 
 * 与PaymentSuccessEvent的区别：
 * - PaymentSuccessEvent：支付成功后取消监控
 * - PaymentCancelledEvent：用户主动取消后取消监控
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class PaymentCancelledEvent extends ApplicationEvent {

    /**
     * 订单号
     */
    private final String orderNo;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 订单金额
     */
    private final BigDecimal amount;

    /**
     * 取消时间
     */
    private final LocalDateTime cancelledAt;

    /**
     * 取消原因
     */
    private final String cancelReason;

    public PaymentCancelledEvent(Object source, String orderNo, Long userId, 
                               BigDecimal amount, LocalDateTime cancelledAt, String cancelReason) {
        super(source);
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }

    /**
     * 简化构造方法
     */
    public PaymentCancelledEvent(Object source, String orderNo, Long userId, 
                               BigDecimal amount, String cancelReason) {
        this(source, orderNo, userId, amount, LocalDateTime.now(), cancelReason);
    }
}
