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
     * 免费积分余额（新用户1000积分，90天有效）
     */
    private BigDecimal freeBalance;

    /**
     * 包月积分余额（已废弃，数据迁移后可删除）
     */
    private BigDecimal subscriptionBalance;

    /**
     * 当日积分余额（每日登录300积分，1天有效）
     */
    private BigDecimal dailyBalance;

    /**
     * 活动积分余额（分享奖励500积分等，90天有效）
     */
    private BigDecimal activityBalance;

    /**
     * 永久积分余额（付费积分，无期限）
     */
    private BigDecimal permanentBalance;

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