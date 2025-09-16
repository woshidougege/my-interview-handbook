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

    /**
     * 按年优惠比例（0-1之间的小数，如0.17表示17%优惠）
     */
    private Double yearlyDiscountRate;
}
