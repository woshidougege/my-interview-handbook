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
     * 套餐列表
     */
    @Schema(description = "套餐列表") 
    private List<SubscriptionPlanResponse> plans;
}
