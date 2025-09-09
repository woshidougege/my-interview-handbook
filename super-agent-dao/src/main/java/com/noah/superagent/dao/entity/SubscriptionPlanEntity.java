package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Table;
import com.noah.superagent.common.enums.EnabledEnum;
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
     * 套餐特性描述（JSON格式）
     */
    private String features;

    /**
     * 套餐价格（兼容字段）
     */
    private BigDecimal price;

    /**
     * 按月价格
     */
    private BigDecimal monthlyPrice;

    /**
     * 按年价格
     */
    private BigDecimal yearlyPrice;

    /**
     * 赠送积分数量（兼容字段）
     */
    private BigDecimal creditAmount;

    /**
     * 按月赠送积分数量
     */
    private BigDecimal monthlyCreditAmount;

    /**
     * 按年赠送积分数量
     */
    private BigDecimal yearlyCreditAmount;

    /**
     * 每日刷新积分数量
     */
    private Integer dailyRefreshCredit;

    /**
     * 套餐有效期（天）
     */
    private Integer validityDays;

    /**
     * 是否启用 ENABLED-启用 DISABLED-禁用
     */
    private EnabledEnum enabled;

    /**
     * 是否推荐套餐
     */
    private Boolean isRecommended;

    /**
     * 排序值
     */
    private Integer sortOrder;
}