package com.noah.superagent.model;

import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订阅DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSubscriptionDTO extends BaseDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 订阅开始时间
     */
    private LocalDateTime startTime;

    /**
     * 订阅结束时间
     */
    private LocalDateTime endTime;

    /**
     * 支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 获得积分数量
     */
    private BigDecimal creditAmount;

    /**
     * 订阅状态 ACTIVE-生效中 EXPIRED-已过期 CANCELLED-已取消
     */
    private SubscriptionStatusEnum status;

    /**
     * 支付订单号
     */
    private String payOrderNo;

    /**
     * 备注
     */
    private String remark;
}
