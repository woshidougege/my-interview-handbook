package com.noah.superagent.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功事件
 * <p>
 * 当支付成功时发布此事件，用于取消超时检查任务
 * 
 * 事件流程：
 * 支付成功 → 发布此事件 → PaymentSchedulingTask监听 → 取消对应的超时检查任务
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class PaymentSuccessEvent extends ApplicationEvent {

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
     * 支付时间
     */
    private final LocalDateTime paidAt;

    /**
     * 支付方式
     */
    private final String paymentMethod;

    public PaymentSuccessEvent(Object source, String orderNo, Long userId, 
                             BigDecimal amount, LocalDateTime paidAt, String paymentMethod) {
        super(source);
        this.orderNo = orderNo;
        this.userId = userId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
    }
}
