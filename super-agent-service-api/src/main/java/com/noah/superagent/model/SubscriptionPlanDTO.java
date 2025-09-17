package com.noah.superagent.model;

import com.noah.superagent.common.enums.EnabledEnum;
import com.noah.superagent.common.enums.PlanCodeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订阅套餐DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlanDTO extends BaseDTO {

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 套餐代码（英文标识）
     */
    private PlanCodeEnum planCode;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 套餐特性描述列表
     */
    private List<PlanFeatureDTO> features;

    /**
     * 按月原价（未优惠）
     */
    private BigDecimal monthlyOriginalPrice;

    /**
     * 按月价格（最终价格，优惠后）
     */
    private BigDecimal monthlyPrice;

    /**
     * 按年原价（月价*12，未优惠）
     */
    private BigDecimal yearlyOriginalPrice;

    /**
     * 按年价格（最终价格，优惠后）
     */
    private BigDecimal yearlyPrice;

    /**
     * 按月优惠金额
     */
    private BigDecimal monthlySavings;

    /**
     * 套餐有效期（天）
     */
    private Integer validityDays;

    /**
     * 每日刷新积分数量
     */
    private Integer dailyRefreshCredits;

    /**
     * 是否启用 ENABLED-启用 DISABLED-禁用
     */
    private EnabledEnum enabled;

    /**
     * 是否推荐套餐
     */
    private Boolean isRecommended;

    /**
     * 是否为用户当前套餐
     */
    private Boolean isCurrentPlan;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 按年优惠比例（0-1之间的小数，如0.17表示17%优惠）
     */
    private Double yearlyDiscountRate;

    /**
     * 按年订阅优惠金额（一年总共省多少钱）
     */
    private BigDecimal yearlySavings;

    /**
     * 积分数量（积分套餐专用）
     */
    private Long creditsAmount;
}
