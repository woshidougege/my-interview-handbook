package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分交易记录表
 *
 * @author System
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_credit_transaction")
public class CreditTransactionEntity extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易类型 1-包月赠送 2-每日免费 3-Token消费 4-过期清零
     */
    private Integer transactionType;

    /**
     * 交易金额（正数表示收入，负数表示支出）
     */
    private BigDecimal amount;

    /**
     * 交易前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 交易后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 关联订单ID（如果有）
     */
    private Long relatedOrderId;

    /**
     * 关联订阅ID（如果有）
     */
    private Long relatedSubscriptionId;

    /**
     * 过期时间（包月积分会有过期时间）
     */
    private LocalDateTime expireTime;
}