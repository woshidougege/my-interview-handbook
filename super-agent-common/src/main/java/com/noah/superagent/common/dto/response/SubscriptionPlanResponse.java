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
@Schema(description = "订阅套餐信息")
public class SubscriptionPlanResponse extends BaseResponse {

    /**
     * 套餐名称
     */
    @Schema(description = "套餐名称", example = "免费版")
    private String planName;

    /**
     * 套餐描述
     */
    @Schema(description = "套餐描述", example = "适合轻度使用的个人用户")
    private String description;

    /**
     * 套餐特性描述列表
     */
    @Schema(description = "套餐功能特性列表")
    private List<PlanFeatureResponse> features;

    /**
     * 套餐价格（兼容字段）
     */
    @Schema(description = "套餐价格", example = "0")
    private BigDecimal price;

    /**
     * 按月价格
     */
    @Schema(description = "按月价格", example = "0")
    private BigDecimal monthlyPrice;

    /**
     * 按年价格
     */
    @Schema(description = "按年价格", example = "0")
    private BigDecimal yearlyPrice;

    /**
     * 赠送积分数量（兼容字段）
     */
    @Schema(description = "赠送积分数量", example = "1000")
    private BigDecimal creditAmount;

    /**
     * 按月赠送积分数量
     */
    @Schema(description = "按月赠送积分数量", example = "0")
    private BigDecimal monthlyCreditAmount;

    /**
     * 按年赠送积分数量
     */
    @Schema(description = "按年赠送积分数量", example = "0")
    private BigDecimal yearlyCreditAmount;

    /**
     * 每日刷新积分数量
     */
    @Schema(description = "每日刷新积分数量", example = "0")
    private Integer dailyRefreshCredit;

    /**
     * 套餐有效期（天）
     */
    @Schema(description = "套餐有效期天数", example = "90")
    private Integer validityDays;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    /**
     * 是否推荐套餐
     */
    @Schema(description = "是否推荐套餐", example = "false")
    private Boolean isRecommended;

    /**
     * 排序值
     */
    @Schema(description = "排序值", example = "1")
    private Integer sortOrder;
}
