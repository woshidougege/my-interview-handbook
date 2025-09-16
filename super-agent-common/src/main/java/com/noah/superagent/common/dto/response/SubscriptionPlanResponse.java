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
     * 套餐价格（兼容字段）
     */
    @Schema(description = "套餐价格（兼容字段，通常与monthlyPrice相同）", example = "39.00")
    private BigDecimal price;

    /**
     * 按月价格
     */
    @Schema(description = "按月订阅价格", example = "39.00")
    private BigDecimal monthlyPrice;

    /**
     * 按年价格
     */
    @Schema(description = "按年订阅价格（通常有优惠）", example = "388.00")
    private BigDecimal yearlyPrice;

    /**
     * 赠送积分数量（兼容字段）
     */
    @Schema(description = "赠送积分数量（兼容字段）", example = "1900.00")
    private BigDecimal creditAmount;

    /**
     * 按月赠送积分数量
     */
    @Schema(description = "按月订阅赠送的积分数量", example = "1900.00")
    private BigDecimal monthlyCreditAmount;

    /**
     * 按年赠送积分数量
     */
    @Schema(description = "按年订阅赠送的积分数量", example = "1900.00")
    private BigDecimal yearlyCreditAmount;

    /**
     * 每日刷新积分数量
     */
    @Schema(description = "每日自动刷新的积分数量（0表示不刷新）", example = "0")
    private Integer dailyRefreshCredit;

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
     * 排序值
     */
    @Schema(description = "排序值（数字越小越靠前）", example = "2")
    private Integer sortOrder;
}
