package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订阅记录表
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_user_subscription")
public class UserSubscriptionEntity extends BaseEntity {

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