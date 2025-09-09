package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.CreditTransactionTypeEnum;
import com.noah.superagent.common.enums.CreditTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分交易记录表
 *
 * @author 任相鹏
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
     * 交易类型：INCOME_SUBSCRIPTION-包月赠送 INCOME_DAILY_FREE-每日免费 EXPENSE_TOKEN_USAGE-Token消费 EXPENSE_EXPIRED_CLEAR-过期清零
     */
    private CreditTransactionTypeEnum transactionType;

    /**
     * 积分类型代码
     */
    private CreditTypeEnum creditType;

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