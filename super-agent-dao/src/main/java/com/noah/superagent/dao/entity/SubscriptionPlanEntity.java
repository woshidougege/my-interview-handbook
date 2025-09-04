package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订阅套餐表
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_subscription_plan")
public class SubscriptionPlanEntity extends BaseEntity {

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 套餐价格
     */
    private BigDecimal price;

    /**
     * 赠送积分数量
     */
    private BigDecimal creditAmount;

    /**
     * 套餐有效期（天）
     */
    private Integer validityDays;

    /**
     * 是否启用 1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 排序值
     */
    private Integer sortOrder;
}