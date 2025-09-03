package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户订阅记录表
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("user_subscription")
public class UserSubscription extends BaseEntity {

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
    private java.time.LocalDateTime startTime;

    /**
     * 订阅结束时间
     */
    private java.time.LocalDateTime endTime;

    /**
     * 支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 获得积分数量
     */
    private BigDecimal creditAmount;

    /**
     * 订阅状态 1-生效中 2-已过期 3-已取消
     */
    private Integer status;

    /**
     * 支付订单号
     */
    private String payOrderNo;

    /**
     * 备注
     */
    private String remark;
}
