package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 套餐列表响应（简洁版）
 */
@Data
@Schema(description = "套餐列表响应")
public class PlansListResponse {

    /**
     * 全局优惠比例配置
     */
    @Schema(description = "按年优惠比例（如0.17表示17%）", example = "0.17")
    private Double yearlyDiscountRate;

    /**
     * 优惠百分比显示文本（如"17%"）
     */
    @Schema(description = "优惠百分比显示文本", example = "17%")
    private String discountPercentageText;

    /**
     * 套餐列表
     */
    @Schema(
        description = "套餐列表",
        implementation = SubscriptionPlanResponse.class
    ) 
    private List<SubscriptionPlanResponse> plans;
}
