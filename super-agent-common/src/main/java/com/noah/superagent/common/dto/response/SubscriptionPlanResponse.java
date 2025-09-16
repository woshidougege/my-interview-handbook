package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订阅套餐响应DTO
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "SubscriptionPlanResponse", 
    title = "订阅套餐响应", 
    description = "订阅套餐完整信息，包含价格、功能特性、有效期等详细数据"
)
public class SubscriptionPlanResponse extends BaseResponse {

    /**
     * 套餐名称
     */
    @Schema(description = "套餐名称", example = "基础版")
    private String planName;

    /**
     * 套餐代码（英文标识）
     */
    @Schema(description = "套餐代码（英文标识）", example = "basic")
    private String planCode;

    /**
     * 套餐描述
     */
    @Schema(description = "套餐描述", example = "适合中度使用的专业用户")
    private String description;

    /**
     * 套餐特性描述列表
     */
    @Schema(
        description = "套餐功能特性列表，展示该套餐包含的功能点", 
        example = "[{\"text\":\"一次性获得1900永久积分\",\"highlight\":true},{\"text\":\"享受所有免费版权益\",\"highlight\":false},{\"text\":\"图片、视频生成\",\"highlight\":true}]"
    )
    private List<PlanFeatureResponse> features;

    /**
     * 按月原价（未优惠）
     */
    @Schema(description = "按月订阅原价（未优惠）", example = "39.00")
    private BigDecimal monthlyOriginalPrice;

    /**
     * 按月价格（最终价格，优惠后）
     */
    @Schema(description = "按月订阅最终价格（优惠后）", example = "39.00")
    private BigDecimal monthlyPrice;

    /**
     * 按年原价（月价*12，未优惠）
     */
    @Schema(description = "按年订阅原价（月价*12，未优惠）", example = "468.00")
    private BigDecimal yearlyOriginalPrice;

    /**
     * 按年价格（最终价格，优惠后）
     */
    @Schema(description = "按年订阅最终价格（优惠后）", example = "388.00")
    private BigDecimal yearlyPrice;

    /**
     * 按月优惠金额
     */
    @Schema(description = "按月订阅优惠金额（通常为0）", example = "0.00")
    private BigDecimal monthlySavings;

    /**
     * 套餐有效期（天）
     */
    @Schema(description = "套餐有效期天数（0表示永久）", example = "30")
    private Integer validityDays;

    /**
     * 是否启用
     */
    @Schema(description = "套餐是否启用（1启用 0禁用）", example = "true")
    private Boolean enabled;

    /**
     * 是否推荐套餐
     */
    @Schema(description = "是否为推荐套餐（前端可显示推荐标签）", example = "true")
    private Boolean isRecommended;

    /**
     * 是否为用户当前套餐
     */
    @Schema(description = "是否为用户当前套餐（前端可显示当前计划标识）", example = "false")
    private Boolean isCurrentPlan;

    /**
     * 排序值
     */
    @Schema(description = "排序值（数字越小越靠前）", example = "2")
    private Integer sortOrder;

    /**
     * 按年订阅优惠金额（一年总共省多少钱）
     */
    @Schema(description = "按年订阅优惠金额（元）", example = "80.00")
    private BigDecimal yearlySavings;
}
