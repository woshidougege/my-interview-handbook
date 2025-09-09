package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户积分账户表
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_credit_account")
public class UserCreditAccountEntity extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总积分余额
     */
    private BigDecimal totalBalance;


    /**
     * 累计获得积分
     */
    private BigDecimal totalEarned;

    /**
     * 累计消费积分
     */
    private BigDecimal totalSpent;

    /**
     * 版本号（乐观锁）
     */
    private Integer version;
}