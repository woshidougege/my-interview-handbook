package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅订单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_subscription_order")
public class SubscriptionOrderEntity extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 计费周期 monthly/yearly
     */
    private String billingCycle;

    /**
     * 订单状态 pending/paid/cancelled/expired
     */
    private String status;

    /**
     * 支付方式 wechat/alipay
     */
    private String paymentMethod;

    /**
     * 第三方支付订单号
     */
    private String thirdPartyOrderNo;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 套餐生效开始时间
     */
    private LocalDateTime effectiveStartTime;

    /**
     * 套餐生效结束时间
     */
    private LocalDateTime effectiveEndTime;

    /**
     * 备注
     */
    private String remark;
}
